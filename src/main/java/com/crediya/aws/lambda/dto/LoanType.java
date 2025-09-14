package com.crediya.aws.lambda.dto;

import java.math.BigDecimal;

public record LoanType(
        Long id,
        String name,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Float interestRate,
        Boolean autoValidation
){}
