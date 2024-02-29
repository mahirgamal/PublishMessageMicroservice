package com.domain;

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
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.function.QueueMappingRequest;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class PublishMessage {
    private static final Logger logger = Logger.getLogger(PublishMessage.class.getName());
    private static final String DB_URL = "jdbc:mysql://leisadb.mysql.database.azure.com:3306/leisa";
    private static final String DB_USER = "lei";
    private static final String DB_PASSWORD = "mahirgamal123#";

    HttpRequestMessage<Optional<QueueMappingRequest>> request;

    public HttpResponseMessage publish(String username, String password, String event, Long id, String jsonMessage)
            throws Exception {

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
        stmt.setString(2, event);
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

    }

}
