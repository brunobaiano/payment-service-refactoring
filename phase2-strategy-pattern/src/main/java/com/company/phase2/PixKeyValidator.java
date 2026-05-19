package com.company.phase2;

public class PixKeyValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing PixKeyValidator via Strategy Pattern...");
        if (request.getTokenOrCardNumber() == null) {
            System.err.println("Validation Failed: Pix key missing.");
            return false;
        }
        return true;
    }
}
