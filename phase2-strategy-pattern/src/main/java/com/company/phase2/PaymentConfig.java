package com.company.phase2;

import java.util.List;

public class PaymentConfig {
    private String paymentType;
    private List<String> requiredValidations;

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    
    public List<String> getRequiredValidations() { return requiredValidations; }
    public void setRequiredValidations(List<String> requiredValidations) { this.requiredValidations = requiredValidations; }
}
