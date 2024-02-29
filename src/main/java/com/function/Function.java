package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Optional;
import java.util.logging.Logger;
import com.domain.Authorisation;
import com.domain.PublishMessage;
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
        Authorisation auth=new Authorisation();
        if (auth.isAuthorised(request)) {
            try {

                String jsonMessage;

                ObjectMapper objectMapper = new ObjectMapper();
                jsonMessage = objectMapper.writeValueAsString(queueMappingRequest.getMessage());
                if (jsonMessage == null)
                    throw new IllegalStateException("Error converting message to JSON");

                PublishMessage publishMessage=new PublishMessage();
                return publishMessage.publish(auth.getUsername(), auth.getPassword(), queueMappingRequest.getEvent(), auth.getId(), jsonMessage);
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

}
