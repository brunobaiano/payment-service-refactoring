# Phase 1: Legacy Reflection-Based Payment Engine

## Overview

Phase 1 is a **legacy payment processing system** that uses **Java reflection** to dynamically invoke validation methods and route payments. This is a real-world example of how payment systems were built before modern Java features like sealed records, pattern matching, and method references were available.

### Key Characteristics

- 🔍 **Reflection-based method invocation** - Validators are called dynamically via `Method.invoke()`
- 📋 **Configuration-driven validation** - Rules loaded from JSON, not hard-coded
- 🚦 **Dynamic routing** - Payment type and amount determine processing tier
- ⚠️ **Runtime type discovery** - Types determined via `getClass().getSimpleName()`
- 🛡️ **Fraud detection** - Manual review requirements based on amount/patterns
- 📝 **String-based method lookup** - Validation method names come from configuration

This represents the "before modern Java" approach—verbose, error-prone, but powerful enough to solve real problems.

---

## Architecture

### System Flow

```
Payment Input (CreditCard or Pix)
    ↓
[Type Check via Reflection] ← getClass().getSimpleName()
    ↓
[Load Configuration] ← payment-config.json
    ↓
[Run Validators via Reflection] ← Method.invoke()
    ├─ validateCardFormat
    ├─ checkFraudRisk
    ├─ validateLimits
    └─ validatePixKey
    ↓
[Validate: Pass/Fail?]
    ├─ FAIL → Return false
    └─ PASS ↓
[Route Payment] ← PaymentRouter (reflection-based)
    ├─ Amount? Currency? Type?
    ↓
[Fraud Check Required?]
    ├─ YES → Require manual review
    └─ NO ↓
[Manual Review Required?]
    ├─ YES → Require manual review
    └─ NO ↓
[Ready for Stripe Gateway]
    ↓
Charge via Stripe API
```

### Component Diagram

```
LegacyEngine (Main Orchestrator)
    ├── Uses: ValidationMethods (Reflection-invoked validators)
    ├── Uses: PaymentRouter (Reflection-based routing)
    ├── Uses: StripeGateway (Stripe API integration)
    └── Loads: PaymentConfig (from JSON)

Payment Domain Model
    ├── CreditCard { tokenOrCardNumber, amount, currency }
    ├── Pix { pixKey, amount, currency }
    └── PaymentRequest (legacy, generic wrapper)
```

---

## How Reflection Works in This System

### 1. Runtime Type Detection

```java
public static String routePayment(Object payment) {
    // Reflection: Determine type at runtime
    String paymentType = payment.getClass().getSimpleName();
    // Result: "CreditCard" or "Pix"
    
    if (paymentType.equals("CreditCard")) {
        return routeCreditCard((CreditCard) payment);
    } else if (paymentType.equals("Pix")) {
        return routePix((Pix) payment);
    }
    return "UNKNOWN_GATEWAY";
}
```

**Problem This Solves**: In a legacy system, you may not know payment types at compile time. Types loaded from database → need runtime type checking.

### 2. Configuration-Driven Method Invocation

Payment validation rules are stored in `payment-config.json`:

```json
[
  {
    "paymentType": "CREDIT_CARD",
    "requiredValidations": [
      "validateCardFormat",
      "checkFraudRisk",
      "validateLimits"
    ]
  },
  {
    "paymentType": "PIX",
    "requiredValidations": [
      "validatePixKey",
      "checkFraudRisk",
      "validateLimits"
    ]
  }
]
```

Methods are invoked **dynamically** based on this configuration:

```java
private boolean runValidations(Object payment) {
    // Load config based on payment type
    PaymentConfig config = getConfig(payment);
    
    // For each validation rule in config
    for (String methodName : config.getRequiredValidations()) {
        try {
            // Reflection: Find the method by name
            Class<?> paramType = payment.getClass();
            Method method = ValidationMethods.class.getDeclaredMethod(
                methodName,      // Method name from config!
                paramType        // Parameter type matches payment
            );
            
            // Reflection: Invoke the method
            boolean isValid = (boolean) method.invoke(validationMethods, payment);
            
            if (!isValid) {
                return false;
            }
        } catch (NoSuchMethodException e) {
            // Config references method that doesn't exist → Runtime error!
            System.err.println("Method not found: " + methodName);
            return false;
        } catch (InvocationTargetException | IllegalAccessException e) {
            // Reflection boilerplate exceptions
            e.printStackTrace();
            return false;
        }
    }
    return true;
}
```

**Key Problems**:
- ⚠️ Typo in JSON method name? → `NoSuchMethodException` at runtime
- ⚠️ Wrong parameter type? → Method not found at runtime
- ⚠️ Method renamed? → All config files break, but compiler doesn't warn

### 3. Method Overloading + Reflection

Validators are overloaded for different payment types:

```java
public boolean validateCardFormat(CreditCard creditCard) {
    if (creditCard.getTokenOrCardNumber() == null || 
        creditCard.getTokenOrCardNumber().isEmpty()) {
        return false;
    }
    return true;
}

public boolean validateCardFormat(Pix pix) {
    // For PIX, card format validation doesn't apply
    return false;
}
```

**How It Works**:
1. Configuration says "run validateCardFormat"
2. Reflection determines payment type is `CreditCard`
3. `Method.getDeclaredMethod("validateCardFormat", CreditCard.class)` finds the right overload
4. Method is invoked with the payment object

**Why This Matters**: Without sealed records (Phase 3), you can't use pattern matching to pick the right method. Reflection + method overloading is the classical solution.

---

## Payment Domain Model

### CreditCard

```java
public class CreditCard {
    private String tokenOrCardNumber;  // Stripe token or card number
    private double amount;              // Transaction amount in currency units
    private String currency;            // ISO currency code (USD, EUR, etc.)
}
```

### Pix

```java
public class Pix {
    private String pixKey;              // CPF, email, phone, or random key
    private double amount;              // Transaction amount in BRL
    private String currency;            // Currency code (BRL)
}
```

### Legacy: PaymentRequest

```java
public class PaymentRequest {
    private String paymentType;         // Discriminator: "CREDIT_CARD" or "PIX"
    private String tokenOrCardNumber;   // Generic field for token/key
    private double amount;
    private String currency;
}
```

The original `PaymentRequest` is a **discriminated union** pattern—using a string field to determine type. This works but is error-prone (typo in `paymentType` value not caught until runtime).

---

## Validation System

### ValidationMethods: Reflection-Based Validators

Each validator is a method that can be invoked via reflection:

```java
public boolean validateCardFormat(CreditCard creditCard) {
    System.out.println("Executing validateCardFormat via Reflection...");
    if (creditCard.getTokenOrCardNumber() == null || 
        creditCard.getTokenOrCardNumber().isEmpty()) {
        System.err.println("Validation Failed: Card number is empty.");
        return false;
    }
    System.out.println("✓ Card token valid");
    return true;
}

public boolean checkFraudRisk(CreditCard creditCard) {
    System.out.println("Executing checkFraudRisk via Reflection...");
    if (creditCard.getAmount() > 10000) {
        System.err.println("Validation Failed: Amount flagged for fraud risk.");
        return false;
    }
    System.out.println("✓ Fraud risk check passed");
    return true;
}

public boolean validateLimits(CreditCard creditCard) {
    System.out.println("Executing validateLimits via Reflection...");
    if (creditCard.getAmount() <= 0) {
        System.err.println("Validation Failed: Amount must be > 0.");
        return false;
    }
    System.out.println("✓ Amount valid: " + creditCard.getAmount());
    return true;
}
```

### Validation Rules by Payment Type

**CreditCard Validators**:
- `validateCardFormat` - Token/card number not empty
- `checkFraudRisk` - Amount ≤ $10,000
- `validateLimits` - Amount > 0

**PIX Validators**:
- `validatePixKey` - PIX key not empty
- `checkFraudRisk` - Amount ≤ R$50,000 OR key length suspicious
- `validateLimits` - Amount > 0

### How Validation Flows

```java
// In LegacyEngine.processPayment()

// 1. Load configuration for payment type
PaymentConfig config = configs.stream()
    .filter(c -> c.getPaymentType().equals("CREDIT_CARD"))
    .findFirst()
    .get();

// 2. Get validation list from config
List<String> validators = config.getRequiredValidations();
// Result: ["validateCardFormat", "checkFraudRisk", "validateLimits"]

// 3. For each validator string
for (String methodName : validators) {
    // 4. Use reflection to find and invoke method
    Method method = ValidationMethods.class.getDeclaredMethod(
        methodName,
        CreditCard.class
    );
    boolean result = (boolean) method.invoke(validationMethods, payment);
    
    if (!result) {
        return false;  // Validation failed
    }
}

// 5. All validations passed
return true;
```

**Error Scenarios**:

| Scenario | Error | Caught When |
|----------|-------|------------|
| Typo in JSON: `"validateCardFormatt"` | `NoSuchMethodException` | Test runs / production |
| Method renamed but JSON not updated | `NoSuchMethodException` | Test runs / production |
| Wrong parameter type | Method lookup fails | Test runs / production |
| Method throws exception | `InvocationTargetException` | Test runs / production |

**Contrast with Phase 3**:
- Phase 3 uses `Map<String, Predicate<Payment>>` with method references
- Compiler verifies method exists and has correct type
- Mismatches caught at **compile time**, not runtime

---

## Payment Routing System

### PaymentRouter: Reflection-Based Tier Determination

The router uses reflection-based object introspection to determine payment tier:

```java
public static String routePayment(Object payment) {
    // Reflection: Determine type
    String paymentType = payment.getClass().getSimpleName();
    
    if (paymentType.equals("CreditCard")) {
        CreditCard cc = (CreditCard) payment;
        // Manual field extraction (no pattern matching)
        double amount = cc.getAmount();
        String currency = cc.getCurrency();
        
        // Explicit if-else chains
        if (amount > 50000) {
            if (!currency.equals("USD")) {
                return "STRIPE_PREMIUM_INTERNATIONAL";
            } else {
                return "STRIPE_PREMIUM_USD";
            }
        } else if (amount > 1000) {
            if (!currency.equals("USD")) {
                return "STRIPE_INTERNATIONAL";
            } else {
                return "STRIPE_STANDARD";
            }
        } else {
            return "STRIPE_EXPRESS";
        }
    } else if (paymentType.equals("Pix")) {
        Pix pix = (Pix) payment;
        double amount = pix.getAmount();
        
        if (amount > 100000) {
            return "PIX_PREMIUM_GATEWAY";
        } else if (amount > 5000) {
            return "PIX_STANDARD_GATEWAY";
        } else {
            return "PIX_EXPRESS_GATEWAY";
        }
    }
    
    return "UNKNOWN_GATEWAY";
}
```

### Routing Rules

**CreditCard Tiers**:

| Tier | Amount | Currency | Gateway |
|------|--------|----------|---------|
| Premium International | > $50,000 | Non-USD | `STRIPE_PREMIUM_INTERNATIONAL` |
| Premium USD | > $50,000 | USD | `STRIPE_PREMIUM_USD` |
| Standard International | $1k - $50k | Non-USD | `STRIPE_INTERNATIONAL` |
| Standard USD | $1k - $50k | USD | `STRIPE_STANDARD` |
| Express | < $1,000 | Any | `STRIPE_EXPRESS` |

**PIX Tiers**:

| Tier | Amount (BRL) | Gateway |
|------|--------------|---------|
| Premium | > R$100,000 | `PIX_PREMIUM_GATEWAY` |
| Standard | R$5k - R$100k | `PIX_STANDARD_GATEWAY` |
| Express | < R$5,000 | `PIX_EXPRESS_GATEWAY` |

### Fraud Detection (Reflection-Based)

```java
public static boolean requiresFraudCheck(Object payment) {
    String paymentType = payment.getClass().getSimpleName();
    
    if (paymentType.equals("CreditCard")) {
        CreditCard cc = (CreditCard) payment;
        // Manual field extraction
        return cc.getAmount() > 10000;
    } else if (paymentType.equals("Pix")) {
        Pix pix = (Pix) payment;
        // Manual field extraction + complex logic
        return pix.getAmount() > 50000 && 
               (pix.getPixKey().length() < 5 || pix.getPixKey().length() > 100);
    }
    return false;
}
```

**Fraud Thresholds**:
- CreditCard: Amount > $10,000
- PIX: Amount > R$50,000 AND (key too short < 5 chars OR too long > 100 chars)

### Manual Review Decision (Reflection-Based)

```java
public static boolean requiresManualReview(Object payment) {
    String paymentType = payment.getClass().getSimpleName();
    
    if (paymentType.equals("CreditCard")) {
        CreditCard cc = (CreditCard) payment;
        return cc.getAmount() > 100000 ||
               cc.getTokenOrCardNumber().startsWith("test_") ||
               cc.getCurrency().equals("XXX");
    } else if (paymentType.equals("Pix")) {
        Pix pix = (Pix) payment;
        return pix.getAmount() > 500000;
    }
    return false;
}
```

**Manual Review Triggers**:
- CreditCard: Amount > $100,000 OR test token OR exotic currency
- PIX: Amount > R$500,000

---

## LegacyEngine: Main Orchestrator

### Processing Flow

```java
public boolean processPayment(CreditCard creditCard) {
    // 1. Log payment details
    System.out.println("=== Processing CreditCard Payment via Reflection ===");
    System.out.println("Payment: " + creditCard);
    
    // 2. Run reflection-based validations
    if (!runValidations(creditCard)) {
        System.err.println("Validation failed.");
        return false;
    }
    
    // 3. Route using reflection-based routing
    String route = PaymentRouter.routePayment(creditCard);
    System.out.println("Routing: " + route);
    
    // 4. Check fraud requirement
    if (PaymentRouter.requiresFraudCheck(creditCard)) {
        System.out.println("⚠️ Fraud check required");
    }
    
    // 5. Check manual review requirement
    if (PaymentRouter.requiresManualReview(creditCard)) {
        System.out.println("⚠️ Manual review required");
        return false;  // Block for manual review
    }
    
    System.out.println("✓ All checks passed!");
    return true;
}
```

### Type Dispatch

```java
private String getConfigPaymentType(Object payment) {
    // Reflection: Check instance type
    if (payment instanceof CreditCard) {
        return "CREDIT_CARD";
    } else if (payment instanceof Pix) {
        return "PIX";
    }
    return "UNKNOWN";
}
```

---

## StripeGateway: Third-Party Integration

### Charge Creation (Overloaded)

```java
public boolean createCharge(CreditCard creditCard) {
    System.out.println("Initiating Stripe Gateway...");
    try {
        ChargeCreateParams params = ChargeCreateParams.builder()
            .setAmount((long) (creditCard.getAmount() * 100))  // Cents
            .setCurrency(creditCard.getCurrency())
            .setSource(creditCard.getTokenOrCardNumber())
            .setDescription("Credit Card Charge")
            .build();
        
        Charge charge = Charge.create(params);
        System.out.println("Charge succeeded: " + charge.getId());
        return true;
    } catch (StripeException e) {
        System.err.println("Stripe failed: " + e.getUserMessage());
        return false;
    }
}
```

**Supported Stripe Test Tokens**:
- `tok_visa` - Successful charge
- `tok_chargeDeclined` - Card declined
- `tok_chargeDeclinedFraudulent` - Fraud detection

---

## Configuration: payment-config.json

```json
[
  {
    "paymentType": "CREDIT_CARD",
    "requiredValidations": [
      "validateCardFormat",
      "checkFraudRisk",
      "validateLimits"
    ]
  },
  {
    "paymentType": "PIX",
    "requiredValidations": [
      "validatePixKey",
      "checkFraudRisk",
      "validateLimits"
    ]
  }
]
```

**How It's Loaded**:

```java
private List<PaymentConfig> loadConfig() {
    ObjectMapper mapper = new ObjectMapper();
    InputStream is = getClass().getResourceAsStream("/payment-config.json");
    return mapper.readValue(is, new TypeReference<List<PaymentConfig>>() {});
}
```

**Configuration-Driven Benefits**:
- ✓ Add new validator without code change (just update JSON)
- ✓ Different rules for different payment types
- ✓ Easy A/B testing of validation rules

**Configuration-Driven Problems**:
- ⚠️ JSON errors → Runtime failure
- ⚠️ Missing validators not caught until config is loaded
- ⚠️ No type safety between config and code

---

## Reflection Problems This Demonstrates

### Problem 1: String-Based Method Lookup

```java
Method method = ValidationMethods.class.getDeclaredMethod(
    "validateCardFormat",  // ← String! Typo not caught
    CreditCard.class
);
```

**What can go wrong**:
- Typo: `"validateCardFormatt"` → Runtime error
- Renamed method: Change in code but config unchanged → Runtime error
- Missing method: Added new rule but method doesn't exist → Runtime error

**Phase 3 Solution**: Method references with compile-time checking
```java
"validateCardFormat" -> ValidationRules::validateCardFormat  // Compiler verifies
```

### Problem 2: Runtime Type Discovery

```java
String paymentType = payment.getClass().getSimpleName();
if (paymentType.equals("CreditCard")) { ... }
else if (paymentType.equals("Pix")) { ... }
```

**What can go wrong**:
- Typo in string comparison → Silently uses wrong branch
- New payment type added → Code doesn't handle it (no exhaustiveness check)
- Type mismatch → Discovered at runtime, not compile time

**Phase 3 Solution**: Sealed types with pattern matching
```java
switch (payment) {
    case Payment.CreditCard cc -> ...
    case Payment.Pix pix -> ...
    // Compiler requires all cases!
}
```

### Problem 3: Manual Field Extraction

```java
CreditCard cc = (CreditCard) payment;
double amount = cc.getAmount();
String currency = cc.getCurrency();
```

**What can go wrong**:
- Get method name wrong → Runtime error
- Field renamed → Code breaks but compiler doesn't warn
- Boilerplate → 50+ lines for routing logic

**Phase 3 Solution**: Record patterns with destructuring
```java
case Payment.CreditCard(_, double amount, String currency) -> ...
// Fields auto-extracted, compile-time type checking
```

### Problem 4: Exception Handling Boilerplate

```java
catch (NoSuchMethodException e) { ... }
catch (InvocationTargetException e) { ... }
catch (IllegalAccessException e) { ... }
```

**What can go wrong**:
- Many exception types to handle
- Complex recovery logic required
- Error messages not specific enough

**Phase 3 Solution**: Type-safe function registry
```java
Map<String, Predicate<Payment>> validators = Map.of(
    "validateCardFormat", ValidationRules::validateCardFormat  // Type-safe!
);
// No reflection exceptions possible
```

---

## Testing the Legacy System

### Test Scenarios

**Legacy PaymentRequest Path** (3 tests):
```java
PaymentRequest request = new PaymentRequest("CREDIT_CARD", "tok_visa", 50.00, "usd");
boolean isValid = engine.processPayment(request);
```

**New Domain Model Path** (12 tests):
```java
CreditCard creditCard = new CreditCard("tok_visa", 50.00, "USD");
boolean isValid = engine.processPayment(creditCard);

Pix pix = new Pix("user@bank.com", 1000.00, "BRL");
boolean isValid = engine.processPayment(pix);
```

### Running Tests

```bash
# Compile phase1
mvn -pl phase1-legacy-reflection clean compile

# Run all tests
mvn -pl phase1-legacy-reflection test

# Run specific test
mvn -pl phase1-legacy-reflection test -Dtest=LegacyEngineTest#testCreditCardExpressProcessing
```

### Test Coverage

- Express tier payments ($50 and R$1,000)
- Standard tier payments ($2,000 EUR and R$20,000)
- Premium tier payments (flagged for fraud check)
- Fraud risk detection (amount-based)
- Manual review triggers (very high amounts)
- Reflection-based routing dispatch

---

## Lessons Learned

### Why This Pattern Exists

**Before Java 14-21** (which introduced sealed records and pattern matching):
- Needed dynamic type handling → Use reflection
- Had discriminated unions → Use string discriminators
- Needed to call methods from config → Use Method.invoke()
- Configuration-driven systems were common

### What Problems It Solves

✓ **Flexibility**: Rules loaded from database, not hard-coded  
✓ **Extensibility**: Add new payment type without code recompile  
✓ **Configuration-driven**: Business rules in JSON, not code  
✓ **Runtime dispatch**: Different validation per type at runtime  

### What Problems It Creates

⚠️ **Runtime errors**: Method names as strings, caught only at runtime  
⚠️ **No exhaustiveness**: No compile-time verification all types handled  
⚠️ **Boilerplate**: Lots of reflection exception handling  
⚠️ **Performance**: Reflection slower than direct method calls  
⚠️ **Debugging**: Stack traces harder to follow with reflection  

---

## Migration Path: Phase 1 → Phase 3

Students learning this codebase should:

1. **Phase 1 (This code)**
   - Understand reflection-based method invocation
   - See how configuration drives behavior
   - Recognize the verbosity and error-proneness
   - Notice the repetitive type-checking code

2. **Phase 2 (Optional: Strategy Pattern)**
   - See object-oriented alternative to reflection
   - Use inheritance + polymorphism instead of Method.invoke()
   - Still no compile-time type safety

3. **Phase 3 (Modern Java)**
   - Learn sealed records and pattern matching
   - See single switch expression replace 30+ lines of if-else
   - Get compile-time exhaustiveness checking
   - Type-safe method reference registry

---

## Running the System

### Prerequisites

- Java 25 or later
- Maven 3.8.1+
- Stripe API key (optional, uses Wiremock for testing)

### Build

```bash
cd phase1-legacy-reflection
mvn clean install
```

### Test

```bash
# All tests (uses Wiremock for Stripe)
mvn test

# Specific test
mvn test -Dtest=LegacyEngineTest#testCreditCardExpressProcessing
```

### Integration

```bash
# Set Stripe API key
export STRIPE_SECRET_KEY=sk_test_YOUR_KEY_HERE

# Run tests (will use actual Stripe API if key is valid)
mvn test
```

---

## Summary

Phase 1 demonstrates a **working but verbose** payment system built with Java reflection:

| Aspect | Phase 1 (Reflection) | Phase 3 (Modern) |
|--------|-------------------|-----------------|
| **Type Checking** | `getClass().getSimpleName()` | Pattern matching |
| **Method Dispatch** | `Method.invoke()` | Method references |
| **Field Extraction** | Manual getters | Record destructuring |
| **Routing Logic** | 30+ lines if-else | 12 lines switch/guards |
| **Exhaustiveness** | Runtime only | Compile-time |
| **Configuration** | JSON → runtime checks | Code → compile checks |
| **Error Messages** | Generic reflection errors | Compile-time type errors |
| **Performance** | Slower (reflection) | Faster (direct calls) |

**Key Takeaway**: Reflection is powerful but verbose. Modern Java features exist to eliminate this boilerplate while maintaining flexibility.

---

**This is Phase 1: The Legacy Way**  
See Phase 3 for the Modern Java solution to the same problem.

