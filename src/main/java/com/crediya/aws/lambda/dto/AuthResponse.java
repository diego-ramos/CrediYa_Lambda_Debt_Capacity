package com.crediya.aws.lambda.dto;

public record AuthResponse (
        String token,
        String username,
        String roleName
){}
