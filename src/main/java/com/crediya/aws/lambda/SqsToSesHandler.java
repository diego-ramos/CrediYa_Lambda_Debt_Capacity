package com.crediya.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.crediya.aws.lambda.application.ApplicationEvaluation;
import com.crediya.aws.lambda.dto.Application;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.services.ses.SesClient;

public class SqsToSesHandler implements RequestHandler<SQSEvent, Void> {

    private static final String FROM_ADDRESS = "diegoramosp@gmail.com";
    private static final String SUBJECT_TEMPLATE = "Application #%d status update";

    private static final String HTML_BODY_TEMPLATE =
            "<h1>Application Update</h1>" +
                    "<p>Your application <b>%d</b> is now <b>%s</b>.</p>";
    private static final String TEXT_BODY_TEMPLATE =
            "Your application %d is now %s";

    private final ObjectMapper mapper = new ObjectMapper();
    private final SesClient sesClient = SesClient.builder().build();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        event.getRecords().forEach(sqsMessage -> {
            try {
                mapper.registerModule(new JavaTimeModule());
                mapper.registerModule(new Jdk8Module());

                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                JsonNode root = mapper.readTree(sqsMessage.getBody());
                context.getLogger().log("VERSION 121 ***********************************************");
                context.getLogger().log("JSON FROM SQS: " + root);

                Application app = mapper.treeToValue(root.get("application"), Application.class);
                context.getLogger().log("Application from JSON: " + app);

                ApplicationEvaluation appEvaluation = new ApplicationEvaluation(app, context);
                appEvaluation.evaluateApplication();

            } catch (Exception e) {
                context.getLogger().log("Error parsing or evaluating the request: " + e.getMessage());
            }
        });

        return null;
    }

}

