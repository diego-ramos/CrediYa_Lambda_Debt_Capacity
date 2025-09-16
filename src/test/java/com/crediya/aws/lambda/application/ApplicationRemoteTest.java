package com.crediya.aws.lambda.application;

import com.amazonaws.services.lambda.runtime.Context;
import com.crediya.aws.lambda.auth.Authentication;
import com.crediya.aws.lambda.config.AppConfig;
import com.crediya.aws.lambda.config.Config;
import com.crediya.aws.lambda.dto.Application;
import com.crediya.aws.lambda.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationRemoteTest {

    private Context context;
    private ApplicationRemote applicationRemote;
    private HttpClient mockHttpClient;

    @BeforeEach
    void setUp() throws Exception {
        context = mock(Context.class, RETURNS_DEEP_STUBS);
        applicationRemote = new ApplicationRemote(context);

        mockHttpClient = mock(HttpClient.class);

        Field httpClientField = ApplicationRemote.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(applicationRemote, mockHttpClient);
    }

    private Application dummyApplication(Long id, Integer identificationNumber) {
        return new Application(
                id,
                identificationNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Optional.empty()
        );
    }


    @Test
    void getUserApprovedApplications_shouldReturnApplications() throws Exception {
        // Arrange
        Application app = dummyApplication(1L, 123456);

        AuthResponse authResponse = new AuthResponse("token123", "testUser", "testRole");

        AppConfig config = new AppConfig();
        config.listAppicationsUrl = "http://test/api/apps?status=%s&identification=%s";

        String body = """
        {
          "content": [
            { "id": 10, "identificationNumber": 123456 }
          ]
        }
        """;

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body);

        try (
                MockedStatic<Authentication> authMock = mockStatic(Authentication.class);
                MockedStatic<Config> configMock = mockStatic(Config.class)
        ) {
            authMock.when(() -> Authentication.authenticate(context)).thenReturn(authResponse);
            configMock.when(Config::get).thenReturn(config);

            when(mockHttpClient.send(
                    any(HttpRequest.class),
                    ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
            )).thenReturn(mockResponse);

            // Act
            List<Application> apps = applicationRemote.getUserApprovedApplications(app);

            // Assert
            assertNotNull(apps);
            assertEquals(1, apps.size());
            assertEquals(10L, apps.get(0).id());
        }
    }

    @Test
    void getUserApprovedApplications_shouldReturnNullOnError() throws Exception {
        Application app = dummyApplication(1L, 123456);
        try (
                MockedStatic<Authentication> authMock = mockStatic(Authentication.class);
                MockedStatic<Config> configMock = mockStatic(Config.class)
        ) {
            authMock.when(() -> Authentication.authenticate(context)).thenThrow(new RuntimeException("Auth failed"));
            configMock.when(Config::get).thenReturn(new AppConfig());

            // Act
            List<Application> apps = applicationRemote.getUserApprovedApplications(app);

            // Assert
            assertNull(apps);
        }
    }

    @Test
    void updateApplicationStatus_shouldReturnUpdatedApplication() throws Exception {
        long id = 1L;
        int newStatus = 2;

        AuthResponse authResponse = new AuthResponse("token123", "testUser", "testRole");
        AppConfig config = new AppConfig();
        config.updateAppicationUrl = "http://test/api/apps/update";

        String body = """
    { "id": 1, "identificationNumber": 123456 }
    """;

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = (HttpResponse<String>) mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body);

        try (
                MockedStatic<Authentication> authMock = mockStatic(Authentication.class);
                MockedStatic<Config> configMock = mockStatic(Config.class)
        ) {
            authMock.when(() -> Authentication.authenticate(context)).thenReturn(authResponse);
            configMock.when(Config::get).thenReturn(config);

            when(mockHttpClient.send(
                    any(HttpRequest.class),
                    ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
            )).thenReturn(mockResponse);

            // Act
            Application updated = applicationRemote.updateApplicationStatus(id, newStatus);

            // Assert
            assertNotNull(updated);
            assertEquals(1L, updated.id());
        }
    }


    @Test
    void updateApplicationStatus_shouldReturnNullOnException() throws Exception {
        long id = 1L;
        int newStatus = 2;

        AuthResponse authResponse = new AuthResponse("token123", "testUser", "testRole");
        AppConfig config = new AppConfig();
        config.updateAppicationUrl = "http://test/api/apps/update";

        try (
                MockedStatic<Authentication> authMock = mockStatic(Authentication.class);
                MockedStatic<Config> configMock = mockStatic(Config.class)
        ) {
            authMock.when(() -> Authentication.authenticate(context)).thenReturn(authResponse);
            configMock.when(Config::get).thenReturn(config);

            when(mockHttpClient.send(any(), any())).thenThrow(new RuntimeException("HTTP error"));

            // Act
            Application result = applicationRemote.updateApplicationStatus(id, newStatus);

            // Assert
            assertNull(result);
        }
    }
}

