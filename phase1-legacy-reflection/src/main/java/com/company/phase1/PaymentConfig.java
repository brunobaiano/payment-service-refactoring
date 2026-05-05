package com.company.phase1;

import java.util.List;

public class PaymentConfig {
    private String paymentType;
    private List<String> requiredValidations;

    // Getters and Setters needed for Jackson
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    
    public List<String> getRequiredValidations() { return requiredValidations; }
    public void setRequiredValidations(List<String> requiredValidations) { this.requiredValidations = requiredValidations; }
}
