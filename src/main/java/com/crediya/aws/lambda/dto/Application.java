package com.crediya.aws.lambda.dto;

import com.crediya.aws.lambda.application.ApplicationStatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Optional;

public record Application (
    Long id,
    Integer identificationNumber,
    BigDecimal amount,
    Integer term,
    Float interestRate,
    String email,
    Integer applicationStatusId,
    Integer loanTypeId,
    ApplicationStatus applicationStatus,
    @JsonProperty("applicationStatusName") ApplicationStatusEnum applicationStatusName,
    LoanType loanType,
    Optional<User> user
){}
