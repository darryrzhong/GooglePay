<img src="./logo.png" alt="GPay Logo" width="553"/>

# GooglePay

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![](https://jitpack.io/v/darryrzhong/GooglePay.svg)](https://jitpack.io/#darryrzhong/GooglePay)

GooglePay 是一个基于 Google Play Billing Library 封装的 Android 支付库，旨在简化 Google 支付的接入流程，提供统一的接口管理一次性购买和订阅服务。

[English](./README.md) | 简体中文

## 接入指南

本文档提供了将 Google Pay 库集成到您的 Android 应用程序中的详细指南。

### 1. 添加依赖

在您的项目 `build.gradle` 文件中添加 JitPack 仓库和依赖项。

**根目录 `build.gradle`:**

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

**App 模块 `build.gradle`:**

```gradle
dependencies {
    implementation 'com.github.darryrzhong:GooglePay:8.1.0-1.1' // 请检查最新版本
}
```

### 2. 初始化

在您的 `Application` 类中初始化 `GooglePayClient`。此设置配置了结算客户端、调试模式和订阅设置。

**示例 (`GpApp.kt`):**

```kotlin
class GpApp : Application() {
    override fun onCreate() {
        super.onCreate()

        GooglePayClient.getInstance()
            .setDebug(true) // 启用日志
            .setSubscriptionMode(SubscriptionMode.SingleMode) // 或 MultiModal (多订阅模式)
            .setSubscription(true) // 启用订阅支持
            .setInterval(15) // 自动刷新间隔 (如果适用)
            .registerActivitys(arrayListOf(MainActivity::class.java)) // 注册主要 Activity
            .initBillingClient(this, GooglePayServiceImpl.instance) // 使用上下文和服务实现进行初始化
    }
}
```

### 3. 实现 GooglePayService

您必须实现 `GooglePayService` 接口以提供商品 ID 并处理购买验证逻辑。

**示例 (`GooglePayServiceImpl.kt`):**

```kotlin
class GooglePayServiceImpl private constructor() : GooglePayService {

    companion object {
        val instance by lazy { GooglePayServiceImpl() }
    }

    // 返回在 Google Play Console 中配置的一次性消耗型商品 ID 列表
    override suspend fun getOneTimeConsumableProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            val productList = listOf(
                "com.example.product.1",
                "com.example.product.2"
            )
            continuation.resume(productList)
        }

    // 返回一次性非消耗型商品 ID 列表 (如果有)
    override suspend fun getOneTimeNonConsumableProducts(): List<String> {
        return arrayListOf()
    }

    // 返回订阅商品 ID 列表
    override suspend fun getSubscribeProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            val subsList = listOf(
                "com.example.vip.1month",
                "com.example.vip.1year"
            )
            continuation.resume(subsList)
        }

    // 处理购买流程 (验证和消耗/确认)
    override suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    ) {
        // 1. 与您的后端服务器验证购买交易
        // ... 验证逻辑 ...

        // 2. 验证通过后，消耗购买 (如果是消耗品) 或 确认购买 (如果是订阅)
        when (productType) {
            BillingProductType.INAPP -> {
                // 对于一次性消耗型商品
                GooglePayClient.getInstance().getPayService<OneTimeService>()
                    .consumePurchases(purchases, isPay)
            }

            BillingProductType.SUBS -> {
                // 对于订阅商品
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

### 4. 一次性购买流程

发起一次性购买 (例如：游戏货币、道具)：

1. **构建 `BillingParams`**:
    * `accountId`: 业务方标识用户的唯一 ID。
    * `productId`: Google Play 商品 ID。
    * `chargeNo`: 业务方生成的订单号 (用于追踪)。

2. **启动支付流程**:
    * 获取 `OneTimeService`。
    * 调用 `launchBillingFlow`。

**示例:**

```kotlin
val billingParams = BillingParams.Builder()
    .setAccountId("user_123")
    .setProductId("com.example.product.1")
    .setChargeNo("order_456")
    .build()

val result = GooglePayClient.getInstance().getPayService<OneTimeService>()
    .launchBillingFlow(requireActivity(), billingParams)

if (result.code != AppBillingResponseCode.OK) {
    // 处理错误 (例如：显示 Toast 提示)
    result.message.showTips(requireContext())
}
```

### 5. 订阅流程

发起订阅：

1. **构建 `BillingSubsParams`**:
    * `accountId`: 用户唯一 ID。
    * `productId`: 订阅商品 ID。
    * `basePlanId`: 基础方案 ID (Billing Library 5+ 需要)。
    * `offerId`: 优惠 ID (如果适用)。
    * `chargeNo`: 业务订单号 (可选，建议用于追踪)。

2. **启动支付流程**:
    * 获取 `SubscriptionService`。
    * 调用 `launchBillingFlow`。

**示例:**

```kotlin
val billingSubsParams = BillingSubsParams.Builder()
    .setAccountId("user_123")
    .setProductId("com.example.vip.1month")
    .setBasePlanId("base-plan-id") // 新版结算配置需要
    .setOfferId("offer-id") // 可选
    .setChargeNo("order_789") // 可选，建议用于追踪
    .build()

val result = GooglePayClient.getInstance().getPayService<SubscriptionService>()
    .launchBillingFlow(requireActivity(), billingSubsParams)

if (result.code != AppBillingResponseCode.OK) {
    // 处理错误
    result.message.showTips(requireContext())
}
```

### 6. 事件处理

使用 `observePayEvent` 扩展函数监听支付事件 (成功、失败等)。这可以在 Activity、Fragment 或 Dialog 中完成。

**示例:**

```kotlin
observePayEvent { payEvent ->
    when (payEvent) {
        is BillingPayEvent.PaySuccessful -> {
            // 支付成功
            // 注意：消耗/确认操作在 GooglePayService.handlePurchasesProcess 中处理
            "支付成功".showTips(requireContext())
        }
        is BillingPayEvent.PayFailed -> {
            // 支付失败
            payEvent.message.showTips(requireContext())
        }
        is BillingPayEvent.PayConsumeSuccessful -> {
            // 消耗成功 (内部事件)
        }
        is BillingPayEvent.PayConsumeFailed -> {
            // 消耗失败
        }
    }
}
```

### 7. 查询商品

您可以查询商品详情 (价格、标题等) 以在 UI 中显示。

**一次性商品:**

```kotlin
val products = GooglePayClient.getInstance().getPayService<OneTimeService>()
    .queryProductDetails(listOf("com.example.product.1"))
// 使用结果更新 UI
```

**订阅商品:**

```kotlin
val subsDetails = GooglePayClient.getInstance().getPayService<SubscriptionService>()
    .querySubsOfferDetails(subsOfferParams)
```

### 8. 恢复购买

要恢复购买 (例如：用户重装应用或更换设备时)，调用 `queryPurchases()`。这将触发 `handlePurchasesProcess` 处理任何未消耗/未确认的活跃购买，或者仅仅刷新本地缓存。

```kotlin
GooglePayClient.getInstance().queryPurchases()
```

### 9. 检查 Google Play 可用性

当 Google Play 服务不可用时，库内部会自动使用空实现进行处理，因此您的应用不会崩溃。不过，您仍然可以使用 `isGoogleAvailable` 来根据需要隐藏支付 UI 元素。

```kotlin
if (GooglePayClient.getInstance().isGoogleAvailable(context)) {
    // 显示支付 UI
} else {
    // 隐藏支付 UI 或显示其他内容
}
```

### 10. 生命周期管理与自动刷新

库会自动处理 `BillingClient` 的生命周期管理。

**自动刷新：**
如果您通过 `registerActivitys()` 注册了重要 Activity（如首页或商城页），库将在以下情况自动刷新库存并处理消耗（基于 `setInterval` 配置的间隔）：

1. 当这些 Activity 每次可见时。
2. 每次应用回到前台时。

```kotlin
GooglePayClient.getInstance().endConnection() // 可选：应用终止时手动断开连接
```

### 11. 重要注意事项

* **生命周期:** 库会处理 `BillingClient` 的生命周期管理。
* **验证:** 务必在授予权益之前在您的后端服务器上验证购买，以防止欺诈。
* **消耗/确认:** Google 要求在 3 天内确认 (订阅) 或消耗 (消耗品) 购买，否则将会退款。您实现的 `GooglePayService` 中的 `handlePurchasesProcess` 是在验证后触发此操作的地方。

### 11. Api Docs
[Api Docs](./API_DOCS.zh_CN.md)