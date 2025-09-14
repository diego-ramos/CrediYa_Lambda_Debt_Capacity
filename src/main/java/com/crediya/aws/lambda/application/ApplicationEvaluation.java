package com.crediya.aws.lambda.application;

import com.amazonaws.services.lambda.runtime.Context;
import com.crediya.aws.lambda.dto.Application;
import com.crediya.aws.lambda.dto.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

public class ApplicationEvaluation {
    BigDecimal CAPACITY_RATE = BigDecimal.valueOf(0.35);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Application application;
    private final Context context;
    private final Optional<User> user;

    public ApplicationEvaluation(Application application, Context context) {
        this.application = application;
        this.context = context;
        this.user = application.user();
    }

    public void evaluateApplication() {
        context.getLogger().log("Calling API: " + application.id());
        ApplicationRemote applicationRemote = new ApplicationRemote(context);
        List<Application> approvedApplications = applicationRemote.getUserApprovedApplications(application);
       // applicationRemote.getUserApprovedApplications(application, context)
        context.getLogger().log("Approved Applications: " + approvedApplications.size());

        BigDecimal totalCurrentPayments = approvedApplications.stream()
                .map(this::calculateMonthlyPayment) // obtain loan monthly payment
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Sum all monthly payments

        context.getLogger().log("Total Current Payments: " + totalCurrentPayments);
        BigDecimal userCapacity = application.user()
                .map(u -> u.baseSalary()
                        .multiply(CAPACITY_RATE)
                        .subtract(totalCurrentPayments))
                .orElse(BigDecimal.ZERO);

        context.getLogger().log("User Capacity: " +userCapacity);
        context.getLogger().log("calculateMonthlyPayment compared to userCapacity: " +calculateMonthlyPayment(application).compareTo(userCapacity));
        boolean loanAproved =  calculateMonthlyPayment(application).compareTo(userCapacity) < 0;

        context.getLogger().log("Application has been Approved?: " +loanAproved);

        int newAppStatus = loanAproved ? ApplicationStatusEnum.APPROVED.id :  ApplicationStatusEnum.REJECTED.id;

        context.getLogger().log("Application has been Updated: " + applicationRemote.updateApplicationStatus(application.id(), newAppStatus));

        }

    private BigDecimal calculateMonthlyPayment(Application application) {
        BigDecimal P = application.amount(); // Capital
        BigDecimal annualRate = application.interestRate() != null ?
                BigDecimal.valueOf(application.interestRate()) : BigDecimal.valueOf(application.loanType().interestRate()); // % anual
        int n = application.term(); // número de meses

        // i = tasa mensual en decimal
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // pasar % a decimal
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP); // mensual

        BigDecimal onePlusRatePow = (BigDecimal.ONE.add(monthlyRate)).pow(n, MathContext.DECIMAL64);

        // fórmula de amortización: C = P * [ i * (1+i)^n ] / [ (1+i)^n - 1 ]
        BigDecimal numerator = P.multiply(monthlyRate).multiply(onePlusRatePow);
        BigDecimal denominator = onePlusRatePow.subtract(BigDecimal.ONE);

        BigDecimal monthlyPayment = numerator.divide(denominator, 10, RoundingMode.HALF_UP);
        this. context.getLogger().log("Montly payment for " + application.id() + ": " + monthlyPayment);
        return monthlyPayment.setScale(2, RoundingMode.HALF_UP); // redondeo final a 2 decimales
    }
}
