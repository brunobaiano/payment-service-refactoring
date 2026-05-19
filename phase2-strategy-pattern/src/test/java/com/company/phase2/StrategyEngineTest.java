package com.company.phase2;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stripe.Stripe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StrategyEngineTest {

    private StrategyEngine engine;
    private StripeGateway stripe;
    private static WireMockServer wireMockServer;
    private static boolean useMock;

    @BeforeAll
    public static void setupClass() {
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        useMock = (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk_test_..."));

        if (useMock) {
            System.out.println("No valid Stripe Key found. Starting Wiremock for offline testing...");
            wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
            wireMockServer.start();
            
            Stripe.overrideApiBase("http://localhost:" + wireMockServer.port());
            Stripe.apiKey = "sk_test_mock";

            wireMockServer.stubFor(post(urlEqualTo("/v1/charges"))
                    .withRequestBody(containing("tok_visa"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\": \"ch_mock_123\", \"object\": \"charge\", \"amount\": 5000, \"currency\": \"usd\", \"status\": \"succeeded\"}")));

            wireMockServer.stubFor(post(urlEqualTo("/v1/charges"))
                    .withRequestBody(containing("tok_chargeDeclined"))
                    .willReturn(aResponse()
                            .withStatus(402)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": {\"type\": \"card_error\", \"code\": \"card_declined\", \"message\": \"Your card was declined.\"}}")));
        }
    }

    @AfterAll
    public static void teardownClass() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    public void setup() {
        engine = new StrategyEngine();
        stripe = new StripeGateway();
    }

    @Test
    public void testValidCreditCardPaymentScenario() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_visa", 50.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertTrue(isValid, "Validations should pass for a valid request amount and token.");

        if (isValid) {
            boolean isCharged = stripe.createCharge(request);
            assertTrue(isCharged, "Stripe charge should succeed.");
        }
    }

    @Test
    public void testCardDeclinedScenario() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_chargeDeclined", 50.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertTrue(isValid, "Local validations pass.");

        if (isValid) {
            boolean isCharged = stripe.createCharge(request);
            assertFalse(isCharged, "Stripe should decline.");
        }
    }

    @Test
    public void testFraudRiskValidationFailure() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_visa", 15000.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertFalse(isValid, "Validation should fail due to internal fraud risk check.");
    }
}
