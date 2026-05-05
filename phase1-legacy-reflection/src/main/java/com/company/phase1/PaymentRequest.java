package com.company.phase1;

public class PaymentRequest {
    private String paymentType;
    private String tokenOrCardNumber;
    private double amount;
    private String currency;

    public PaymentRequest(String paymentType, String tokenOrCardNumber, double amount, String currency) {
        this.paymentType = paymentType;
        this.tokenOrCardNumber = tokenOrCardNumber;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPaymentType() { return paymentType; }
    public String getTokenOrCardNumber() { return tokenOrCardNumber; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
