package com.company.phase1;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.stripe.Stripe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LegacyEngineTest - Testing both legacy PaymentRequest and new domain classes.
 * 
 * This test suite demonstrates:
 * 1. Legacy path: PaymentRequest → reflection-based dispatch
 * 2. New path: CreditCard/Pix → reflection-based routing with validation
 * 3. Routing decisions based on amount and currency
 * 4. Fraud checks and manual review detection
 * 
 * Pedagogical value: Students see how the same payment scenarios are handled
 * via reflection in Phase 1 vs. pattern matching in Phase 3.
 */
public class LegacyEngineTest {

    private LegacyEngine engine;
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
            
            // Stripe SDK global override
            Stripe.overrideApiBase("http://localhost:" + wireMockServer.port());
            Stripe.apiKey = "sk_test_mock"; // Fake key just to pass SDK validation

            // Stub for Success
            wireMockServer.stubFor(post(urlEqualTo("/v1/charges"))
                    .withRequestBody(containing("tok_visa"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\": \"ch_mock_123\", \"object\": \"charge\", \"amount\": 5000, \"currency\": \"usd\", \"status\": \"succeeded\"}")));

            // Stub for Decline
            wireMockServer.stubFor(post(urlEqualTo("/v1/charges"))
                    .withRequestBody(containing("tok_chargeDeclined"))
                    .willReturn(aResponse()
                            .withStatus(402) // Payment Required / Card Error
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
        engine = new LegacyEngine();
        stripe = new StripeGateway();
    }

    // ===== LEGACY PaymentRequest TESTS (backwards compatibility) =====

    @Test
    public void testValidCreditCardPaymentScenario() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_visa", 50.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertTrue(isValid, "Validations should pass for a valid request amount and token.");

        boolean isCharged = stripe.createCharge(request);
        assertTrue(isCharged, "Stripe charge should succeed.");
    }

    @Test
    public void testCardDeclinedScenario() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_chargeDeclined", 50.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertTrue(isValid, "Local validations pass because format and amounts are fine.");

        boolean isCharged = stripe.createCharge(request);
        assertFalse(isCharged, "Stripe should return false because we used a Declined test token.");
    }

    @Test
    public void testFraudRiskValidationFailure() {
        PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_visa", 15000.00, "usd");
        
        boolean isValid = engine.processPayment(request);
        assertFalse(isValid, "Validation should fail due to internal fraud risk check (amount > 10000).");
    }

    // ===== NEW DOMAIN MODEL TESTS (CreditCard and Pix) =====

    /**
     * Test: CreditCard with valid express-tier amount ($50 USD)
     * Expected: Routes to STRIPE_EXPRESS, validation passes
     */
    @Test
    public void testCreditCardExpressProcessing() {
        CreditCard creditCard = new CreditCard("tok_visa", 50.00, "USD");
        
        boolean isValid = engine.processPayment(creditCard);
        assertTrue(isValid, "Express tier credit card should pass validation.");

        boolean isCharged = stripe.createCharge(creditCard);
        assertTrue(isCharged, "Stripe charge should succeed for express tier.");
    }

    /**
     * Test: CreditCard with standard international amount ($2,000 EUR)
     * Expected: Routes to STRIPE_INTERNATIONAL, validation passes
     */
    @Test
    public void testCreditCardStandardInternational() {
        CreditCard creditCard = new CreditCard("tok_visa", 2000.00, "EUR");
        
        boolean isValid = engine.processPayment(creditCard);
        assertTrue(isValid, "Standard international credit card should pass validation.");
        
        // Verify it routes to international gateway
        String route = PaymentRouter.routePayment(creditCard);
        assertTrue(route.contains("INTERNATIONAL"), "Should route to international gateway for non-USD currency.");
    }

    /**
     * Test: CreditCard with premium domestic amount ($60,000 USD)
     * Expected: Fails validation due to fraud check (amount > $10,000), requires manual review
     * This demonstrates how premium amounts require additional processing steps
     */
    @Test
    public void testCreditCardPremiumDomestic() {
        CreditCard creditCard = new CreditCard("tok_visa", 60000.00, "USD");
        
        // Premium tier payment with high amount fails standard validation (needs manual review path)
        boolean isValid = engine.processPayment(creditCard);
        assertFalse(isValid, "Premium domestic credit card should fail validation (requires manual review path).");
        
        // But we can verify it WOULD route to premium gateway if it passed
        String route = PaymentRouter.routePayment(creditCard);
        assertEquals("STRIPE_PREMIUM_USD", route, "Should route to premium USD gateway.");
        assertTrue(PaymentRouter.requiresFraudCheck(creditCard), "Premium tier should require fraud check.");
    }

    /**
     * Test: CreditCard with premium international amount ($75,000 EUR)
     * Expected: Fails validation due to fraud check (amount > $10,000), requires manual review
     * This demonstrates that large international transactions need additional scrutiny
     */
    @Test
    public void testCreditCardPremiumInternational() {
        CreditCard creditCard = new CreditCard("tok_visa", 75000.00, "EUR");
        
        // Premium tier payment with high amount fails standard validation (needs manual review path)
        boolean isValid = engine.processPayment(creditCard);
        assertFalse(isValid, "Premium international credit card should fail validation (requires manual review path).");
        
        String route = PaymentRouter.routePayment(creditCard);
        assertEquals("STRIPE_PREMIUM_INTERNATIONAL", route, "Should route to premium international gateway.");
        assertTrue(PaymentRouter.requiresFraudCheck(creditCard), "Premium international should require fraud check.");
    }

    /**
     * Test: CreditCard fraud risk detection (amount > $10,000)
     * Expected: Validation fails due to fraud risk
     */
    @Test
    public void testCreditCardFraudRiskFailure() {
        CreditCard creditCard = new CreditCard("tok_visa", 15000.00, "USD");
        
        boolean isValid = engine.processPayment(creditCard);
        assertFalse(isValid, "Credit card with amount > $10,000 should fail fraud check.");
        assertTrue(PaymentRouter.requiresFraudCheck(creditCard), "Should be flagged for fraud check.");
    }

    /**
     * Test: CreditCard with extremely high amount requiring manual review
     * Expected: Validation fails due to manual review requirement
     */
    @Test
    public void testCreditCardManualReviewRequired() {
        CreditCard creditCard = new CreditCard("tok_visa", 150000.00, "USD");
        
        boolean isValid = engine.processPayment(creditCard);
        assertFalse(isValid, "Credit card amount > $100,000 should require manual review.");
        assertTrue(PaymentRouter.requiresManualReview(creditCard), "Should require manual review.");
    }

    /**
     * Test: PIX express payment ($1,000 BRL)
     * Expected: Routes to PIX_EXPRESS_GATEWAY, validation passes
     */
    @Test
    public void testPixExpressProcessing() {
        Pix pix = new Pix("user@email.com", 1000.00, "BRL");
        
        boolean isValid = engine.processPayment(pix);
        assertTrue(isValid, "Express tier PIX should pass validation.");
        
        String route = PaymentRouter.routePayment(pix);
        assertEquals("PIX_EXPRESS_GATEWAY", route, "Should route to PIX express gateway.");
    }

    /**
     * Test: PIX standard payment ($20,000 BRL)
     * Expected: Routes to PIX_STANDARD_GATEWAY, validation passes
     */
    @Test
    public void testPixStandardProcessing() {
        Pix pix = new Pix("user@bank.com", 20000.00, "BRL");
        
        boolean isValid = engine.processPayment(pix);
        assertTrue(isValid, "Standard tier PIX should pass validation.");
        
        String route = PaymentRouter.routePayment(pix);
        assertEquals("PIX_STANDARD_GATEWAY", route, "Should route to PIX standard gateway.");
    }

    /**
     * Test: PIX premium payment ($150,000 BRL) with suspicious key
     * Expected: Fails validation due to fraud check (amount > R$50,000 AND suspicious key length)
     * This demonstrates that premium tier PIX payments with unusual patterns need additional processing
     */
    @Test
    public void testPixPremiumProcessing() {
        // PIX with premium amount and suspicious key (length < 5)
        Pix pix = new Pix("ab", 150000.00, "BRL");
        
        // Premium tier payment with high amount and suspicious key fails validation (needs manual review)
        boolean isValid = engine.processPayment(pix);
        assertFalse(isValid, "Premium tier PIX with suspicious key should fail validation (requires manual review path).");
        
        // But we can verify it WOULD route to premium gateway if it passed
        String route = PaymentRouter.routePayment(pix);
        assertEquals("PIX_PREMIUM_GATEWAY", route, "Should route to PIX premium gateway.");
        assertTrue(PaymentRouter.requiresFraudCheck(pix), "Premium PIX with suspicious key should require fraud check.");
    }

    /**
     * Test: PIX fraud risk detection (amount > R$50,000 with suspicious key)
     * Expected: Flagged for fraud check
     */
    @Test
    public void testPixFraudRiskDetection() {
        // PIX with high amount and unusual key length
        Pix pix = new Pix("ab", 75000.00, "BRL"); // key length = 2 (< 5)
        
        assertTrue(PaymentRouter.requiresFraudCheck(pix), "PIX with high amount and short key should be flagged for fraud check.");
    }

    /**
     * Test: PIX manual review requirement (amount > R$500,000)
     * Expected: Manual review required
     */
    @Test
    public void testPixManualReviewRequired() {
        Pix pix = new Pix("user@email.com", 600000.00, "BRL");
        
        boolean isValid = engine.processPayment(pix);
        assertFalse(isValid, "PIX amount > R$500,000 should require manual review.");
        assertTrue(PaymentRouter.requiresManualReview(pix), "Should require manual review.");
    }

    /**
     * Test: Reflection-based routing dispatch
     * Demonstrates how routing is determined via reflection-based object introspection
     */
    @Test
    public void testReflectionBasedRouting() {
        // Create payments and verify routing via reflection
        CreditCard lowAmountCC = new CreditCard("tok_visa", 500.00, "USD");
        CreditCard highAmountCC = new CreditCard("tok_visa", 60000.00, "USD");
        Pix pixPayment = new Pix("user@bank.com", 3000.00, "BRL");

        // Reflection-based routing should determine correct tier
        String route1 = PaymentRouter.routePayment(lowAmountCC);
        String route2 = PaymentRouter.routePayment(highAmountCC);
        String route3 = PaymentRouter.routePayment(pixPayment);

        assertEquals("STRIPE_EXPRESS", route1, "Low amount should route to express.");
        assertEquals("STRIPE_PREMIUM_USD", route2, "High USD amount should route to premium USD.");
        assertEquals("PIX_EXPRESS_GATEWAY", route3, "Low PIX should route to express.");
    }
}
