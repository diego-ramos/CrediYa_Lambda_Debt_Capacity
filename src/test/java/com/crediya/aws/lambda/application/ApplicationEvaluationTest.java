package com.crediya.aws.lambda.application;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.crediya.aws.lambda.dto.Application;
import com.crediya.aws.lambda.dto.LoanType;
import com.crediya.aws.lambda.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApplicationEvaluationTest {

    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        mockLogger = mock(LambdaLogger.class);
        mockContext = mock(Context.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    private Application buildApplication(BigDecimal amount, Double rate, int term, BigDecimal baseSalary) {
        User user = new User(
                1,                       // idType
                123456,                  // identificationNumber
                "John",                  // firstNames
                "Doe",                   // lastNames
                "john.doe@test.com",     // email
                "3001234567",            // phone
                baseSalary,              // baseSalary
                LocalDate.of(1990, 1, 1),// birthDate
                "123 Main St"            // address
        );

        LoanType loanType = new LoanType(
                10L, "PERSONAL",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000000),
                rate != null ? rate.floatValue() : 20.0f,
                true
        );

        return new Application(
                100L,                     // id
                user.identificationNumber(), // identificationNumber
                amount,                   // amount
                term,                     // term
                rate != null ? rate.floatValue() : null, // interestRate
                user.email(),             // email
                null,                     // applicationStatusId
                loanType.id().intValue(), // loanTypeId
                null,                     // applicationStatus
                null,                     // applicationStatusName
                loanType,                 // loanType
                Optional.of(user)         // user
        );
    }


    @Test
    void shouldRejectApplicationWhenCapacityInsufficient() {
        Application app = buildApplication(BigDecimal.valueOf(1_000_0000), 20.0, 12, BigDecimal.valueOf(1_000_000));

        try (MockedConstruction<ApplicationRemote> mocked = mockConstruction(ApplicationRemote.class,
                (mock, context) -> {
                    when(mock.getUserApprovedApplications(app)).thenReturn(
                            List.of(buildApplication(BigDecimal.valueOf(200_000), 20.0, 12, BigDecimal.valueOf(1_000_000)))
                    );
                    when(mock.updateApplicationStatus(eq(app.id()), anyInt())).thenReturn(app);
                })) {

            ApplicationEvaluation evaluation = new ApplicationEvaluation(app, mockContext);
            evaluation.evaluateApplication();

            ApplicationRemote remote = mocked.constructed().get(0);
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(remote).updateApplicationStatus(eq(app.id()), statusCaptor.capture());

            assertThat(statusCaptor.getValue()).isEqualTo(ApplicationStatusEnum.REJECTED.id);
        }
    }

    @Test
    void shouldApproveApplicationWhenCapacitySufficient() {
        Application app = buildApplication(BigDecimal.valueOf(10_000), 20.0, 12, BigDecimal.valueOf(100_000));

        try (MockedConstruction<ApplicationRemote> mocked = mockConstruction(ApplicationRemote.class,
                (mock, context) -> {
                    when(mock.getUserApprovedApplications(app)).thenReturn(List.of());
                    when(mock.updateApplicationStatus(eq(app.id()), anyInt())).thenReturn(app);
                })) {

            ApplicationEvaluation evaluation = new ApplicationEvaluation(app, mockContext);
            evaluation.evaluateApplication();

            ApplicationRemote remote = mocked.constructed().get(0);
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(remote).updateApplicationStatus(eq(app.id()), statusCaptor.capture());

            assertThat(statusCaptor.getValue()).isEqualTo(ApplicationStatusEnum.APPROVED.id);
        }
    }

    @Test
    void shouldSendToManualRevisionWhenAmountExceedsFiveTimesSalary() {
        Application app = buildApplication(BigDecimal.valueOf(600_000), 20.0, 120, BigDecimal.valueOf(100_000));

        try (MockedConstruction<ApplicationRemote> mocked = mockConstruction(ApplicationRemote.class,
                (mock, context) -> {
                    when(mock.getUserApprovedApplications(app)).thenReturn(List.of());
                    when(mock.updateApplicationStatus(eq(app.id()), anyInt())).thenReturn(app);
                })) {

            ApplicationEvaluation evaluation = new ApplicationEvaluation(app, mockContext);
            evaluation.evaluateApplication();

            ApplicationRemote remote = mocked.constructed().get(0);
            ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
            verify(remote).updateApplicationStatus(eq(app.id()), statusCaptor.capture());

            assertThat(statusCaptor.getValue()).isEqualTo(ApplicationStatusEnum.MANUAL_REVISION.id);
        }
    }
}
