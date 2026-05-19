# Phase 2: Strategy Pattern Payment Engine

## Overview

Phase 2 is an **intermediate refactoring** of the legacy payment system. It replaces the **runtime reflection** from Phase 1 with the classic **Gang of Four Strategy Pattern**, using polymorphism and a factory to dispatch validators.

This is a real-world example of how teams eliminated `Method.invoke()` **before** modern Java features like sealed records and pattern matching were available.

### Key Characteristics

- 🧩 **Strategy Pattern** - Each validator is a dedicated class implementing a common interface
- 🏭 **Factory-based dispatch** - `ValidatorFactory` maps config string names to strategy objects
- ✅ **No more `Method.invoke()`** - Reflection is removed from the execution path
- 📋 **Still configuration-driven** - Rules still loaded from `payment-config.json`
- ⚠️ **Partial type-safety** - String-to-class mapping in the factory is still manually maintained
- 🔗 **Open/Closed Principle** - New validators can be added without changing the engine

This represents the classical Object-Oriented solution—cleaner than reflection, but still not as safe as Phase 3's compile-time approach.

---

## Architecture

### System Flow

```
Payment Input (PaymentRequest)
    ↓
[StrategyEngine.processPayment()]
    ↓
[Load Configuration] ← payment-config.json
    ↓
[Resolve Strategies] ← ValidatorFactory.getValidator(name)
    ├─ "validateCardFormat" → CardFormatValidator instance
    ├─ "checkFraudRisk"    → FraudRiskValidator instance
    ├─ "validateLimits"    → LimitsValidator instance
    └─ "validatePixKey"    → PixKeyValidator instance
    ↓
[Execute Strategies] ← validator.validate(request)
    ├─ FAIL → Return false
    └─ PASS ↓
[StripeGateway.createCharge()]
    ↓
Stripe API
```

### Component Diagram

```
StrategyEngine (Main Orchestrator)
    ├── Uses: ValidatorFactory (Strategy resolver)
    │       └── Returns: PaymentValidator instances
    ├── Uses: PaymentConfig (from JSON)
    └── Uses: StripeGateway (Stripe API integration)

PaymentValidator (Strategy Interface)
    ├── CardFormatValidator  implements PaymentValidator
    ├── FraudRiskValidator   implements PaymentValidator
    ├── LimitsValidator      implements PaymentValidator
    └── PixKeyValidator      implements PaymentValidator
```

### Phase 1 vs Phase 2: The Core Difference

| Concern | Phase 1 (Reflection) | Phase 2 (Strategy Pattern) |
|---------|----------------------|---------------------------|
| **Validator lookup** | `Method.getDeclaredMethod(name, type)` | `ValidatorFactory.getValidator(name)` |
| **Validator execution** | `method.invoke(validationMethods, payment)` | `validator.validate(request)` |
| **Type safety** | ❌ Strings at runtime | ⚠️ Partial (factory is still stringly-typed) |
| **Error discovery** | `NoSuchMethodException` at runtime | `IllegalArgumentException` at factory lookup |
| **Adding a validator** | Create method + update JSON | Create class + register in factory + update JSON |
| **Exception boilerplate** | `NoSuchMethodException`, `InvocationTargetException`, `IllegalAccessException` | None |

---

## How the Strategy Pattern Works Here

### 1. The Strategy Interface

All validators implement a single, shared interface:

```java
public interface PaymentValidator {
    boolean validate(PaymentRequest request);
}
```

**Why this matters**: Instead of invoking an arbitrary method by name via reflection, the engine now calls a well-known method on a well-known type. The compiler verifies the contract.

### 2. Concrete Strategies

Each validation rule becomes its own class:

```java
public class CardFormatValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing CardFormatValidator via Strategy Pattern...");
        if (request.getTokenOrCardNumber() == null || request.getTokenOrCardNumber().isEmpty()) {
            System.err.println("Validation Failed: Card number is empty.");
            return false;
        }
        return true;
    }
}

public class FraudRiskValidator implements PaymentValidator {
    @Override
    public boolean validate(PaymentRequest request) {
        System.out.println("Executing FraudRiskValidator via Strategy Pattern...");
        if (request.getAmount() > 10000) {
            System.err.println("Validation Failed: Amount flagged for high fraud risk.");
            return false;
        }
        return true;
    }
}
```

**Contrast with Phase 1**: In Phase 1, these were ordinary methods discovered by name at runtime. Here they are first-class objects that the compiler knows about.

### 3. The Factory: The Last String-to-Object Bridge

The `ValidatorFactory` is where the JSON config strings are finally resolved to typed objects:

```java
public class ValidatorFactory {
    private static final Map<String, PaymentValidator> registry = new HashMap<>();

    static {
        // String names from JSON are mapped to compiled Strategy objects here.
        // This is the ONLY place where strings touch the class system.
        registry.put("validateCardFormat", new CardFormatValidator());
        registry.put("checkFraudRisk",     new FraudRiskValidator());
        registry.put("validateLimits",     new LimitsValidator());
        registry.put("validatePixKey",     new PixKeyValidator());
    }

    public static PaymentValidator getValidator(String name) {
        PaymentValidator validator = registry.get(name);
        if (validator == null) {
            throw new IllegalArgumentException("Unknown validation mechanism configured: " + name);
        }
        return validator;
    }
}
```

**Key insight**: The string-to-class boundary is now confined to one place. If a config typo exists, `IllegalArgumentException` is thrown **at factory lookup time** (startup/first use), not buried inside a `Method.invoke()` call with confusing stack traces.

### 4. The Engine: Clean Execution (No Reflection)

```java
public boolean processPayment(PaymentRequest request) {
    // 1. Load config (same as Phase 1)
    Optional<PaymentConfig> configOpt = configs.stream()
            .filter(c -> c.getPaymentType().equals(request.getPaymentType()))
            .findFirst();

    if (configOpt.isEmpty()) {
        System.err.println("No configuration found for payment type: " + request.getPaymentType());
        return false;
    }

    PaymentConfig config = configOpt.get();
    System.out.println("Processing " + request.getPaymentType() + " payment. Resolving Strategies...");

    // 2. Resolve Strategy objects (replaces reflection lookup)
    List<PaymentValidator> activeValidators = new ArrayList<>();
    for (String validationName : config.getRequiredValidations()) {
        // Factory throws IllegalArgumentException if name not found — no reflection exceptions
        activeValidators.add(ValidatorFactory.getValidator(validationName));
    }

    // 3. Execute via polymorphism (replaces Method.invoke())
    for (PaymentValidator validator : activeValidators) {
        if (!validator.validate(request)) {
            System.err.println("Validation execution failed. Aborting payment.");
            return false;
        }
    }

    System.out.println("All Strategy validations passed successfully!");
    return true;
}
```

**What changed from Phase 1**:
- ✅ No `getDeclaredMethod()` — replaced by `ValidatorFactory.getValidator()`
- ✅ No `method.invoke()` — replaced by `validator.validate()`
- ✅ No `NoSuchMethodException`, `InvocationTargetException`, or `IllegalAccessException`
- ⚠️ Config string typos still cause runtime failures (but with a clearer error message)

---

## Payment Domain Model

Phase 2 uses a **unified `PaymentRequest`** class for all payment types, using a string discriminator:

```java
public class PaymentRequest {
    private String paymentType;         // "CREDIT_CARD" or "PIX"
    private String tokenOrCardNumber;   // Credit card token OR PIX key
    private double amount;
    private String currency;
}
```

> [!NOTE]
> Notice the regression compared to Phase 1's separate `CreditCard` and `Pix` classes.
> Phase 2 simplifies the domain model to focus the article on the validator pattern.
> Phase 3 restores (and improves) this with sealed records.

---

## Validation System

### Validators by Payment Type (from `payment-config.json`)

**CreditCard** (`"paymentType": "CREDIT_CARD"`):

| Validator Class | Config Name | Rule |
|----------------|-------------|------|
| `CardFormatValidator` | `"validateCardFormat"` | `tokenOrCardNumber` must not be null/empty |
| `FraudRiskValidator` | `"checkFraudRisk"` | Amount must be ≤ $10,000 |
| `LimitsValidator` | `"validateLimits"` | Amount must be > 0 |

**PIX** (`"paymentType": "PIX"`):

| Validator Class | Config Name | Rule |
|----------------|-------------|------|
| `PixKeyValidator` | `"validatePixKey"` | PIX key must not be null |
| `FraudRiskValidator` | `"checkFraudRisk"` | Amount must be ≤ $10,000 |

### Configuration File (`payment-config.json`)

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
      "checkFraudRisk"
    ]
  }
]
```

The config format is **identical to Phase 1**. The difference is entirely in how these strings are resolved — factory lookup vs. `Method.getDeclaredMethod()`.

### Adding a New Validator

In Phase 2, adding a new validator requires **3 steps** (vs. 2 in Phase 1):

1. **Create the class** implementing `PaymentValidator`
2. **Register it** in `ValidatorFactory.registry`
3. **Add the name** to `payment-config.json`

In Phase 3, no registration step is needed — method references are used directly.

---

## Remaining Problems (Why We Still Need Phase 3)

Phase 2 eliminates reflection from the execution path, but some pain points remain:

### Problem 1: The Factory is Still Stringly-Typed

```java
registry.put("validateCardFormat", new CardFormatValidator()); // What if you typo "validateCardFormat"?
```

If the config says `"validateCardFormatt"` and the factory only knows `"validateCardFormat"`, you get a runtime `IllegalArgumentException`. The compiler does not help.

**Phase 3 Solution**: Eliminates the string-to-method-reference mapping by using method references directly in a typed `Map<String, Predicate<Payment>>`.

### Problem 2: Unified `PaymentRequest` Loses Type Information

Using one generic class for all payment types means you lose compile-time knowledge of what fields are relevant. You can pass a `CreditCard` request to `PixKeyValidator` with no compile-time complaint.

**Phase 3 Solution**: Sealed records (`CreditCard` and `Pix` as distinct types) + pattern matching ensure validators only receive the right type.

### Problem 3: No Exhaustiveness Check

If you add a new payment type to the config, nothing in the compiler tells you to add a new entry to the factory. You discover it at runtime when a payment is processed.

**Phase 3 Solution**: `switch` on sealed types forces you to handle every case at compile time.

---

## Testing

### Running Tests

```bash
# Compile phase2
mvn -pl phase2-strategy-pattern clean compile

# Run all tests
mvn -pl phase2-strategy-pattern test

# Run a specific test
mvn -pl phase2-strategy-pattern test -Dtest=StrategyEngineTest#testValidCreditCardPaymentScenario
```

### Test Scenarios

| Test | Setup | Expected |
|------|-------|----------|
| `testValidCreditCardPaymentScenario` | `tok_visa`, $50, CREDIT_CARD | Engine passes ✅, Stripe charges ✅ |
| `testCardDeclinedScenario` | `tok_chargeDeclined`, $50, CREDIT_CARD | Engine passes ✅, Stripe declines ❌ |
| `testFraudRiskValidationFailure` | `tok_visa`, $15,000, CREDIT_CARD | Engine rejects ❌ (fraud risk) |

### Offline Mocking (Wiremock)

Like Phase 1, Phase 2 uses **WireMock** for offline testing. If `STRIPE_SECRET_KEY` is missing or set to the placeholder `sk_test_...`, the test setup automatically starts an embedded WireMock server and routes the Stripe SDK to it.

> [!NOTE]
> **Don't panic about the red text!** If running offline, you will see a `[WARNING] STRIPE_SECRET_KEY environment variable is missing...` message. This is expected. Green JUnit checkmarks mean everything is working.

---

## Migration Path

### Phase 1 → Phase 2 (This code)

**What changed**:
- `Method.getDeclaredMethod()` → `ValidatorFactory.getValidator()`
- `method.invoke(validationMethods, payment)` → `validator.validate(request)`
- Removed `NoSuchMethodException` / `InvocationTargetException` / `IllegalAccessException` handling
- Added `PaymentValidator` interface and four concrete strategy classes
- Added `ValidatorFactory` with a static registry

**What stayed the same**:
- `payment-config.json` format (identical)
- `StripeGateway` integration
- WireMock offline testing setup
- `PaymentConfig` and `PaymentRequest` POJO structure

### Phase 2 → Phase 3

The next step brings **modern Java** to complete the evolution:
- `PaymentRequest` → Sealed `Payment` record (`CreditCard`, `Pix`)
- `ValidatorFactory` → `Map<String, Predicate<Payment>>` with method references
- `instanceof` + casting → Pattern matching `switch`
- Configuration-driven runtime strings → Compile-time type checking

---

## Summary

Phase 2 demonstrates the **Gang of Four Strategy Pattern** as the classical OO solution for removing `Method.invoke()`:

| Aspect | Phase 1 (Reflection) | Phase 2 (Strategy) | Phase 3 (Modern Java) |
|--------|---------------------|--------------------|-----------------------|
| **Dispatch** | `Method.invoke()` | `ValidatorFactory` + polymorphism | Method references |
| **Type Safety** | ❌ Runtime only | ⚠️ Partial (factory boundary) | ✅ Compile-time |
| **Exhaustiveness** | ❌ No | ❌ No | ✅ Sealed type switch |
| **Reflection** | ✅ Heavy use | ✅ Eliminated | ✅ Eliminated |
| **Exception boilerplate** | ❌ Many reflection exceptions | ✅ None | ✅ None |
| **OO Principles** | Low | High (Open/Closed) | High (Functional) |

---

*Part of the Java Tips and Tricks — Payment Service Refactoring series.*
*Phase 2 corresponds to the second article: "Eliminating Reflection with the Strategy Pattern".*
