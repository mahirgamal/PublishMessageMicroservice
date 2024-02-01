package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */

    private static final Logger logger = Logger.getLogger(Function.class.getName());
    private static final String DB_URL = "jdbc:mysql://leisadb.mysql.database.azure.com:3306/leisa";
    private static final String DB_USER = "lei";
    private static final String DB_PASSWORD = "mahirgamal123#";

    private String username, password;
    private long id;

    static {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FunctionName("publish")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<QueueMappingRequest>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final QueueMappingRequest queueMappingRequest = request.getBody().orElse(null);

        if (queueMappingRequest == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body").build();
        }

        if (isAuthorized(request)) {
            try {

                String jsonMessage;

                ObjectMapper objectMapper = new ObjectMapper();
                jsonMessage = objectMapper.writeValueAsString(queueMappingRequest.getMessage());
                if (jsonMessage == null)
                    throw new IllegalStateException("Error converting message to JSON");

                // Create HttpClient instance
                HttpClient client = HttpClient.newHttpClient();

                // Prepare the request to the external API
                HttpRequest validateRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://messagevalidatormicroservice.azurewebsites.net/api/validate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonMessage))
                        .build();

                // Send the request and get the response
                HttpResponse<String> response = client.send(validateRequest, BodyHandlers.ofString());

                // Check response status code or handle the response as needed
                if (response.statusCode() != 200) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(response.body()).build();
                }

                // Read the JSON file (e.g., 'config.json')
                // You'll need to use a library like Jackson or Gson for JSON parsing in Java
                // Here, we assume that you have a Config class to represent the configuration
                // object

                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rabbitmqconfig.json");
                if (inputStream == null) {
                    throw new IOException("rabbitmqconfig.json file not found in resources");
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String configJSON = reader.lines().collect(Collectors.joining("\n"));
                JSONObject config = new JSONObject(configJSON);

                // Configuration values
                String brokerType = config.getString("brokerType");
                String brokerProtocol = config.getString("brokerProtocol");
                String brokerHost = config.getString("brokerHost");
                int brokerPort = config.getInt("brokerPort");

                logger.info("Broker Type: " + brokerType);
                logger.info("Broker Protocol: " + brokerProtocol);
                logger.info("Broker Host: " + brokerHost);
                logger.info("Broker Port: " + brokerPort);
                logger.info("Username: " + username);
                logger.info("Password: " + password);

                // Create a connection to RabbitMQ
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(brokerHost);
                factory.setPort(brokerPort);
                factory.setUsername(username);
                factory.setPassword(password);

                java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn
                        .prepareStatement("SELECT * FROM queue_mapping WHERE publisher_id=? AND event_type=?",
                                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                stmt.setLong(1, id);
                stmt.setString(2, queueMappingRequest.getEvent());
                ResultSet rs = stmt.executeQuery();

                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                if (!rs.next()) {
                    // No destination queues found
                    return request.createResponseBuilder(HttpStatus.OK).body("No destination to publish").build();
                }

                // Reset the cursor for iteration
                rs.beforeFirst();

                while (rs.next()) {
                    channel.basicPublish("", rs.getString("consumer_queuename"), null, jsonMessage.getBytes());

                }

                return request.createResponseBuilder(HttpStatus.OK).body("Published").build();
            } catch (Exception e) {
                e.printStackTrace();
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage())
                        .build();
            }
        } else {
            logger.warning("Authorization failed or user information exists for user");
            return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).body("Unauthorized").build();
        }
    }

    private boolean isAuthorized(HttpRequestMessage<Optional<QueueMappingRequest>> request) {
        // Parse the Authorization header
        final String authHeader = request.getHeaders().get("authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        // Extract and decode username and password
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded);

        // credentials = username:password
        final String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return false; // Incorrect format of the header
        }

        username = values[0];
        password = values[1];

        logger.info(username);
        logger.info(password);

        String sql = "SELECT * FROM users WHERE username=?";

        try (java.sql.Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                    id = rs.getLong("id");
                    return true;
                } else
                    return false;
            }
        } catch (SQLException e) {
            // Handle exceptions (log it or throw as needed)
            e.printStackTrace();
        }

        return false;

    }
}
