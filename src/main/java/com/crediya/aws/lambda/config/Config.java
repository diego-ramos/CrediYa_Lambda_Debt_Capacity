package com.crediya.aws.lambda.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class Config {
    private static final AppConfig INSTANCE;

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("application.yml")) {
            if (in == null) {
                throw new IllegalStateException("application.yml not found in resources");
            }
            Yaml yaml = new Yaml();
            INSTANCE = yaml.loadAs(in, AppConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    private Config() {}

    public static AppConfig get() {
        return INSTANCE;
    }
}
