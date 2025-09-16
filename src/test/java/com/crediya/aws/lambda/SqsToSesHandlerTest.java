package com.crediya.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.crediya.aws.lambda.application.ApplicationEvaluation;
import com.crediya.aws.lambda.dto.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SqsToSesHandlerTest {

    private SqsToSesHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        handler = new SqsToSesHandler();

        // Mock Context y LambdaLogger
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);

        // Redirige logs a consola
        doAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            System.out.println(arg);
            return null;
        }).when(mockLogger).log(anyString());

        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    private SQSEvent buildSqsEvent(String body) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody(body);

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message));
        return event;
    }

    @Test
    void shouldParseValidApplicationAndEvaluate() throws Exception {
        // language=json
        String json = """
        {
          "application": {
            "id": 1,
            "identificationNumber": 12345
          }
        }
        """;

        SQSEvent event = buildSqsEvent(json);

        // Mock ApplicationEvaluation para no ejecutar lógica real
        try (MockedConstruction<ApplicationEvaluation> mocked =
                     mockConstruction(ApplicationEvaluation.class, (mock, context) -> {
                         doNothing().when(mock).evaluateApplication();
                     })) {

            handler.handleRequest(event, mockContext);

            // Verifica que evaluateApplication se llamó
            verify(mocked.constructed().get(0)).evaluateApplication();
        }
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        String badJson = "{ invalid json }";
        SQSEvent event = buildSqsEvent(badJson);

        // No debe lanzar excepción
        handler.handleRequest(event, mockContext);
    }

    @Test
    void shouldHandleMissingApplicationField() {
        String json = """
        {
          "somethingElse": {}
        }
        """;

        SQSEvent event = buildSqsEvent(json);

        // No debe lanzar excepción ni crear ApplicationEvaluation
        handler.handleRequest(event, mockContext);
    }
}
