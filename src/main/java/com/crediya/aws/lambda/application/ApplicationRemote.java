package com.crediya.aws.lambda.application;

import com.amazonaws.services.lambda.runtime.Context;
import com.crediya.aws.lambda.auth.Authentication;
import com.crediya.aws.lambda.config.AppConfig;
import com.crediya.aws.lambda.config.Config;
import com.crediya.aws.lambda.dto.Application;
import com.crediya.aws.lambda.dto.AuthResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApplicationRemote {
    final HttpClient httpClient = HttpClient.newHttpClient();
    final ObjectMapper mapper = new ObjectMapper();
    final Context context;

    public ApplicationRemote(Context context){
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.context = context;
    }

    public List<Application> getUserApprovedApplications(Application application) {
        context.getLogger().log(mapper.getRegisteredModuleIds().toString());
        try {
            var config = AppConfig.get();
            AuthResponse authResponse = Authentication.authenticate(context);

            String statusIdsParam = String.valueOf(ApplicationStatusEnum.APPROVED.id);

            context.getLogger().log("AUTH: " + authResponse);
            assert authResponse != null;

            String url = String.format(
                    config.listAppicationsUrl,
                    statusIdsParam,
                    application.identificationNumber()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + authResponse.token())
                    .header("Accept", "application/json")
                    .GET()
                    .build();


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed request getUserApprovedApplications: " + response.statusCode() + " - " + response.body());
            }

            context.getLogger().log("Body: " + response.body());

            JsonNode root = mapper.readTree(response.body());
            JsonNode contentNode = root.path("content");

            return mapper.readValue(
                    contentNode.toString(),
                    new TypeReference<>() {
                    }
            );

        } catch (Exception e) {
            context.getLogger().log("Error calling API to get applications: " + e.getMessage());
            return null;
        }
    }

    public Application updateApplicationStatus(long id, int newApplicationStatusId) {
        var config = AppConfig.get();

        AuthResponse authResponse = Authentication.authenticate(context);
        context.getLogger().log("Calling auth login: " + authResponse);

        assert authResponse != null;

        String url = config.updateAppicationUrl;

        try {
            String payload = String.format("""
            {
              "id": "%s",
              "applicationNewStatusId": "%s"
            }
            """,
                id,
                newApplicationStatusId
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + authResponse.token())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            context.getLogger().log("Status: " + response.statusCode());
            context.getLogger().log("Body: " + response.body());

            Application appResponse = mapper.readValue(response.body(), Application.class);

            context.getLogger().log("appResponse: " + appResponse);

            return appResponse;
        } catch (Exception e) {
            context.getLogger().log("Error calling API: " + e.getMessage());
            return null;
        }
    }
}
