package com.crediya.aws.lambda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record User(
        Integer idType,
        Integer identificationNumber,
        String firstNames,
        String lastNames,
        String email ,
        String phone,
        BigDecimal baseSalary,
        LocalDate birthDate,
        String address
){}
