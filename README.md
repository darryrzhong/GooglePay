<img src="./logo.png" alt="GPay Logo" width="553"/>

# GooglePay

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

GooglePay is an Android payment library based on the Google Play Billing Library, designed to simplify the Google payment integration process and provide a unified interface for managing one-time purchases and subscriptions.

English | [简体中文](./README.zh_CN.md)

## Integration Guide

This document provides a detailed guide on how to integrate the Google Pay library into your Android application.

### 1. Add Dependency

Add the JitPack repository and the dependency to your project's `build.gradle` file.

**Root `build.gradle`:**

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**App module `build.gradle`:**

```gradle
dependencies {
    implementation 'com.github.darryrzhong:GooglePay:1.0.0' // Check for the latest version
}
```

### 2. Initialization

Initialize the `GooglePayClient` in your `Application` class. This setup configures the billing client, debug mode, and subscription settings.

**Example (`GpApp.kt`):**

```kotlin
class GpApp : Application() {
    override fun onCreate() {
        super.onCreate()

        GooglePayClient.getInstance()
            .setDebug(true) // Enable logs
            .setSubscriptionMode(SubscriptionMode.SingleMode) // Or MultiModal
            .setSubscription(true) // Enable subscription support
            .setInterval(15) // Auto-refresh interval (if applicable)
            .registerActivitys(arrayListOf(MainActivity::class.java)) // Register main activities
            .initBillingClient(this, GooglePayServiceImpl.instance) // Initialize with context and service implementation
    }
}
```

### 3. Implement GooglePayService

You must implement the `GooglePayService` interface to provide product IDs and handle purchase verification logic.

**Example (`GooglePayServiceImpl.kt`):**

```kotlin
class GooglePayServiceImpl private constructor() : GooglePayService {

    companion object {
        val instance by lazy { GooglePayServiceImpl() }
    }

    // Return list of one-time product IDs configured in Google Play Console
    override suspend fun getOneTimeConsumableProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                "com.example.product.1",
                "com.example.product.2"
            )
            continuation.resume(productList)
        }

    // Return list of non-consumable product IDs (if any)
    override suspend fun getOneTimeNonConsumableProducts(): List<String> {
        return arrayListOf()
    }

    // Return list of subscription product IDs
    override suspend fun getSubscribeProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            val subsList = listOf(
                "com.example.vip.1month",
                "com.example.vip.1year"
            )
            continuation.resume(subsList)
        }

    // Handle the purchase process (verification and consumption)
    override suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    ) {
        // 1. Verify the purchase with your backend server
        // ... verification logic ...

        // 2. After verification, consume the purchase (if consumable) or acknowledge it
        when (productType) {
            BillingProductType.INAPP -> {
                // For consumable one-time products
                GooglePayClient.getInstance().getPayService<OneTimeService>()
                    .consumePurchases(purchases, isPay)
            }

            BillingProductType.SUBS -> {
                // For subscriptions
                GooglePayClient.getInstance().getPayService<SubscriptionService>()
                    .consumePurchases(purchases, isPay)
            }
        }
    }

    override fun printLog(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}
```

### 4. One-Time Purchase Flow

To initiate a one-time purchase (e.g., in-game currency, items):

1. **Construct `BillingParams`**:
    * `accountId`: Unique user ID in your system.
    * `productId`: Google Play product ID.
    * `chargeNo`: Your system's order ID (for tracking).

2. **Launch Billing Flow**:
    * Get `OneTimeService`.
    * Call `launchBillingFlow`.

**Example:**

```kotlin
val billingParams = BillingParams.Builder()
    .setAccountId("user_123")
    .setProductId("com.example.product.1")
    .setChargeNo("order_456")
    .build()

val result = GooglePayClient.getInstance().getPayService<OneTimeService>()
    .launchBillingFlow(requireActivity(), billingParams)

if (result.code != AppBillingResponseCode.OK) {
    // Handle error (e.g., show toast)
    result.message.showTips(requireContext())
}
```

### 5. Subscription Flow

To initiate a subscription:

1. **Construct `BillingSubsParams`**:
    * `accountId`: Unique user ID.
    * `productId`: Subscription product ID.
    * `basePlanId`: Base plan ID (for Billing Library 5+).
    * `offerId`: Offer ID (if applicable).
    * `chargeNo`: Your system's order ID (optional but recommended).

2. **Launch Billing Flow**:
    * Get `SubscriptionService`.
    * Call `launchBillingFlow`.

**Example:**

```kotlin
val billingSubsParams = BillingSubsParams.Builder()
    .setAccountId("user_123")
    .setProductId("com.example.vip.1month")
    .setBasePlanId("base-plan-id") // Required for newer billing configurations
    .setOfferId("offer-id") // Optional
    .setChargeNo("order_789") // Optional but recommended for tracking
    .build()

val result = GooglePayClient.getInstance().getPayService<SubscriptionService>()
    .launchBillingFlow(requireActivity(), billingSubsParams)

if (result.code != AppBillingResponseCode.OK) {
    // Handle error
    result.message.showTips(requireContext())
}
```

### 6. Event Handling

Listen for payment events (success, failure, etc.) using the `observePayEvent` extension function. This can be done in an Activity, Fragment, or Dialog.

**Example:**

```kotlin
observePayEvent { payEvent ->
    when (payEvent) {
        is BillingPayEvent.PaySuccessful -> {
            // Payment successful
            // Note: Consumption/Acknowledgement is handled in GooglePayService.handlePurchasesProcess
            "Payment Successful".showTips(requireContext())
        }
        is BillingPayEvent.PayFailed -> {
            // Payment failed
            payEvent.message.showTips(requireContext())
        }
        is BillingPayEvent.PayConsumeSuccessful -> {
            // Consumption successful (internal event)
        }
        is BillingPayEvent.PayConsumeFailed -> {
            // Consumption failed
        }
    }
}
```

### 7. Querying Products

You can query product details (price, title, etc.) to display in your UI.

**One-Time Products:**

```kotlin
val products = GooglePayClient.getInstance().getPayService<OneTimeService>()
    .queryProductDetails(listOf("com.example.product.1"))
// Use the result to update UI
```

**Subscriptions:**

```kotlin
val subsDetails = GooglePayClient.getInstance().getPayService<SubscriptionService>()
    .querySubsOfferDetails(subsOfferParams)
```

### 8. Restoring Purchases

To restore purchases (e.g., when a user reinstalls the app or changes devices), call `queryPurchases()`. This will trigger `handlePurchasesProcess` for any active purchases that haven't been consumed/acknowledged, or simply refresh the local cache.

```kotlin
GooglePayClient.getInstance().queryPurchases()
```

### 9. Checking Google Play Availability

The library automatically handles cases where Google Play Services are unavailable by using empty implementations, so your app won't crash. However, you can still use `isGoogleAvailable` to hide payment UI elements if needed.

```kotlin
if (GooglePayClient.getInstance().isGoogleAvailable(context)) {
    // Show payment UI
} else {
    // Hide payment UI or show alternative
}
```

### 10. Lifecycle Management & Auto-Refresh

The library handles `BillingClient` lifecycle management automatically.

**Auto-Refresh:**
If you register important activities (like your Main Activity or Store Page) via `registerActivitys()`, the library will automatically refresh purchase inventory and process consumption:

1. Whenever these activities become visible (subject to the configured `setInterval` time).
2. Every time the app returns to the foreground.

```kotlin
GooglePayClient.getInstance().endConnection() // Optional: Manually end connection on app termination
```

### 11. Important Notes

* **Lifecycle:** The library handles `BillingClient` lifecycle management.
* **Verification:** Always verify purchases on your backend server before granting entitlement to prevent fraud.
* **Consumption/Acknowledgement:** Google requires purchases to be acknowledged (subscriptions) or consumed (consumables) within 3 days, or they will be refunded. The `handlePurchasesProcess` in your `GooglePayService` implementation is the place to trigger this after verification.
