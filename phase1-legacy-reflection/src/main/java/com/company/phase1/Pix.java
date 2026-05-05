package com.company.phase1;

/**
 * Pix payment class - domain model for PIX payments (Brazilian instant transfer).
 * This mirrors Phase 3's sealed record structure but using traditional classes
 * and leverages reflection for property introspection in the legacy system.
 * 
 * Demonstrating: How legacy systems would handle multiple payment types
 * without sealed interfaces and pattern matching.
 */
public class Pix {
    private String pixKey;
    private double amount;
    private String currency;

    public Pix(String pixKey, double amount, String currency) {
        this.pixKey = pixKey;
        this.amount = amount;
        this.currency = currency;
    }

    public String getPixKey() {
        return pixKey;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public String toString() {
        return "Pix{" +
                "pixKey='" + pixKey + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                '}';
    }
}

