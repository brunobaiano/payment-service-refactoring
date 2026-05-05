package com.company.phase1;

/**
 * CreditCard payment class - domain model for credit card payments.
 * This mirrors Phase 3's sealed record structure but using traditional classes
 * and leverages reflection for property introspection in the legacy system.
 * 
 * Demonstrating: How legacy systems would handle multiple payment types
 * without sealed interfaces and pattern matching.
 */
public class CreditCard {
    private String tokenOrCardNumber;
    private double amount;
    private String currency;

    public CreditCard(String tokenOrCardNumber, double amount, String currency) {
        this.tokenOrCardNumber = tokenOrCardNumber;
        this.amount = amount;
        this.currency = currency;
    }

    public String getTokenOrCardNumber() {
        return tokenOrCardNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public String toString() {
        return "CreditCard{" +
                "tokenOrCardNumber='" + tokenOrCardNumber + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }
}

