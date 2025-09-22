package com.crediya.aws.lambda.config;

public class AppConfig {
    public String loginApiUrl;
    public String username;
    public String password;
    public String listAppicationsUrl;
    public String updateAppicationUrl;

    public AppConfig() {
        loginApiUrl = System.getenv().getOrDefault("LOGIN_API_URL", "http://localhost:8080/api/v1/usuarios/login");
        username = System.getenv().getOrDefault("USERNAME", "asesor@test.com");
        password = System.getenv().getOrDefault("PASSWORD", "567");
        listAppicationsUrl = System.getenv().getOrDefault(
                "LIST_APPLICATION_URL",
                "https://shy-keys-lie.loca.lt/api/v1/solicitud?findAll=true&statusIds=%s&userIdNumber=%s"
        );
        updateAppicationUrl = System.getenv().getOrDefault(
                "UPDATE_APPLICATION_URL",
                "https://shy-keys-lie.loca.lt/api/v1/solicitud"
        );
    }

    // helper para obtener instancia singleton
    private static AppConfig instance = new AppConfig();
    public static AppConfig get() { return instance; }
}
