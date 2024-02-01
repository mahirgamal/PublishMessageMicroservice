package com.function;

import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for Function class.
 */
public class FunctionTest {

    @Test
    public void testPublish() throws Exception {
        // Setup
                @SuppressWarnings("unchecked")

        final HttpRequestMessage<Optional<QueueMappingRequest>> req = mock(HttpRequestMessage.class);

        QueueMappingRequest queueMappingRequest = new QueueMappingRequest();
        queueMappingRequest.setEvent("TestEvent");
        queueMappingRequest.setMessage(null); // Assuming Message is a POJO with relevant fields

        doReturn(Optional.of(queueMappingRequest)).when(req).getBody();

        final Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Basic " + Base64.getEncoder().encodeToString("username:password".getBytes()));
        doReturn(headers).when(req).getHeaders();

        HttpResponseMessage.Builder responseBuilder = mock(HttpResponseMessage.Builder.class, RETURNS_SELF);
        doReturn(mock(HttpResponseMessage.class)).when(responseBuilder).build();
        doReturn(responseBuilder).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Instantiate your Function class
        Function function = new Function();

        // Invoke
        final HttpResponseMessage ret = function.run(req, context);

        // Verify
        assertNotNull(ret, "The response should not be null.");
        verify(req).createResponseBuilder(any(HttpStatus.class));
    }
}
