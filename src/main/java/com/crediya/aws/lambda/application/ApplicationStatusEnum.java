package com.crediya.aws.lambda.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationStatusEnum {
    REVISION_PENDING(1, "Pendiente de revisión"),
    PROCESSING(2, "En Proceso"),
    APPROVED(3, "Aprobada"),
    REJECTED(4, "Rechazada"),
    MANUAL_REVISION(5, "Revision manual"),;

    int id;
    String name;

    ApplicationStatusEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    @JsonValue  // 👈 tells Jackson to serialize using the name
    public String getName() {
        return name;
    }

    @JsonCreator  // 👈 tells Jackson how to create enum from JSON string
    public static ApplicationStatusEnum fromValue(String value) {
        for (ApplicationStatusEnum status : values()) {
            if (status.name.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
