package com.crediya.aws.lambda.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.crediya.aws.lambda.config.AppConfig;
import com.crediya.aws.lambda.config.Config;
import com.crediya.aws.lambda.dto.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Authentication {

    private Authentication() {}

    public static AuthResponse authenticate(Context context) {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final ObjectMapper mapper = new ObjectMapper();

        var config = AppConfig.get();

        String url = config.loginApiUrl;
        context.getLogger().log("LOGIN API URL: " + url);

        try {
            String payload = String.format("""
            {
              "email": "%s",
              "password": "%s"
            }
            """,
                config.username,
                config.password
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            context.getLogger().log("Status: " + response.statusCode());
            context.getLogger().log("Body: " + response.body());

            AuthResponse authResponse = mapper.readValue(response.body(), AuthResponse.class);

            context.getLogger().log("authResponse: " +authResponse);

            return authResponse;
        } catch (Exception e) {
            context.getLogger().log("Error calling API: " + e.getMessage());
            return null;
        }
    }
}
