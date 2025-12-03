# GooglePay API 文档

本文档详细介绍了 GooglePay 库的 API 接口、参数说明以及在不同场景下的使用方法。

## 1. 初始化 (Initialization)

在 `Application` 的 `onCreate` 方法中初始化 `GooglePayClient`。

```kotlin
class App : Application(), GooglePayService {

    override fun onCreate() {
        super.onCreate()
        // 初始化 GooglePayClient
        GooglePayClient.getInstance()
            .initBillingClient(this, this) // 传入 Application Context 和 GooglePayService 实现
            .setDebug(true) // 开启调试模式，输出日志
            .setSubscription(false) // 是否支持订阅，默认为 false
            .setSubscriptionMode(SubscriptionMode.MultiModal) // 设置订阅模式
            .setInterval(15) // 设置自动刷新间隔（秒），默认 15秒
            .registerActivitys(listOf(MainActivity::class.java)) // 注册需要触发自动刷新的 Activity
    }

    // 实现 GooglePayService 接口的方法...
}
```

## 2. 核心类与接口

### 2.1 GooglePayClient

`GooglePayClient` 是库的核心入口，单例模式。

* **getInstance()**: 获取单例实例。
* **initBillingClient(context: Application, service: GooglePayService)**: 初始化客户端。
* **setDebug(isDebug: Boolean)**: 设置是否开启调试日志。
* **setSubscription(isSubscription: Boolean)**: 设置是否支持订阅功能。
* **setSubscriptionMode(mode: SubscriptionMode)**: 设置订阅模式。
* **setInterval(interval: Int)**: 设置在指定 Activity `onResume` 时触发刷新购买状态的最小时间间隔（单位：秒），默认 15 秒。
* **registerActivitys(activitys: List<Class<out Activity>>)**: 注册需要触发自动刷新的 Activity 类。当这些 Activity `onResume` 且距离上次刷新超过 `interval` 时，会自动调用 `queryPurchases()`。
* **getPayService<T>()**: 获取具体的支付服务 (`OneTimeService` 或 `SubscriptionService`)。
* **queryPurchases()**: 查询并刷新当前的购买状态（包括一次性商品和订阅）。
* **isGoogleAvailable(context: Context?)**: 检查 Google Play 服务是否可用。
* **endConnection()**: 断开 BillingClient 连接。

### 2.2 GooglePayService

业务端需要实现的接口，用于提供商品 ID 和处理购买结果。

* **getOneTimeConsumableProducts()**: 返回一次性消耗型商品 ID 列表。
* **getOneTimeNonConsumableProducts()**: 返回一次性非消耗型商品 ID 列表。
* **getSubscribeProducts()**: 返回订阅商品 ID 列表。
* **handlePurchasesProcess(isPay: Boolean, productType: BillingProductType, purchases: Purchase)**: 处理购买交易，验证订单并确认。

## 3. 支付服务 (Payment Services)

通过 `GooglePayClient.getInstance().getPayService<T>()` 获取。

### 3.1 OneTimeService (一次性商品服务)

用于处理消耗型和非消耗型商品。

* **launchBillingFlow(activity: Activity, billingParams: BillingParams)**: 启动支付流程。
* **queryProductDetails(productIds: List<String>)**: 查询商品详情。

### 3.2 SubscriptionService (订阅服务)

用于处理订阅商品。

* **launchBillingFlow(activity: Activity, billingSubsParams: BillingSubsParams)**: 启动订阅支付流程。
* **querySubsOfferDetails(subsOfferParams: SubsOfferParams)**: 查询订阅商品详情（包括 Offer）。
* **queryAckSubscribePurchases(productIds: List<String>?)**: 查询当前有效的订阅。

## 4. 参数说明 (Parameters)

### 4.1 BillingParams (一次性购买参数)

用于 `OneTimeService.launchBillingFlow`。

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `accountId` | String | 业务方标识用户的唯一 ID (obfuscatedAccountId) |
| `productId` | String | Google Play 后台配置的商品 ID |
| `chargeNo` | String | 业务方生成的订单号 (obfuscatedProfileId) |

**构建示例：**

```kotlin
val params = BillingParams.Builder()
    .setAccountId("user_123")
    .setProductId("coin_100")
    .setChargeNo("order_abc_456")
    .build()
```

### 4.2 BillingSubsParams (订阅购买参数)

用于 `SubscriptionService.launchBillingFlow`。

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `accountId` | String | 业务方标识用户的唯一 ID |
| `productId` | String | Google Play 后台配置的订阅商品 ID |
| `chargeNo` | String | 业务方生成的订单号 |
| `offerToken` | String | 订阅商品的 Offer Token (从 `AppSubscribeDetails` 获取) |

### 4.3 AppBillingResponseCode (响应码)

`AppBillingResponseCode` 定义了 Google Play Billing API 返回的所有可能的响应码。

| 代码 | 常量 | 说明 |
| :--- | :--- | :--- |
| `0` | `OK` | 请求成功 |
| `-200` | `FAIL` | 业务错误 |
| `-3` | `SERVICE_TIMEOUT` | 请求超时（已废弃，建议使用 `SERVICE_UNAVAILABLE`） |
| `-2` | `FEATURE_NOT_SUPPORTED` | 当前设备或 Google Play 不支持该功能（如订阅、商品详情等） |
| `-1` | `SERVICE_DISCONNECTED` | 与 Google Play Billing 服务断开连接，可尝试重新连接 |
| `1` | `USER_CANCELED` | 用户在结算流程中主动取消 |
| `2` | `SERVICE_UNAVAILABLE` | 网络问题或 Google Play 服务不可用（如正在更新、离线等） |
| `3` | `BILLING_UNAVAILABLE` | 当前设备上不支持 Google Play 结算 |
| `4` | `ITEM_UNAVAILABLE` | 商品在当前区域或账户不可用（例如未发布或已下架） |
| `5` | `DEVELOPER_ERROR` | 开发者调用错误（如参数错误、API 调用顺序错误） |
| `6` | `ERROR` | 一般错误（系统内部异常、未知问题等） |
| `7` | `ITEM_ALREADY_OWNED` | 商品已拥有（如未消耗的消耗型商品或已购买的非消耗型商品） |
| `8` | `ITEM_NOT_OWNED` | 操作失败，因为商品未拥有（尝试消耗或确认未拥有的商品） |
| `12` | `NETWORK_ERROR` | 网络错误（新引入，用于明确网络失败场景） |

**使用示例：**

```kotlin
when (responseCode) {
    AppBillingResponseCode.OK -> {
        // 处理成功
    }
    AppBillingResponseCode.USER_CANCELED -> {
        // 用户取消支付
    }
    AppBillingResponseCode.ITEM_ALREADY_OWNED -> {
        // 商品已拥有
    }
    AppBillingResponseCode.NETWORK_ERROR -> {
        // 网络错误，可以重试
    }
    else -> {
        // 处理其他错误
    }
}
```

### 4.4 AppProductDetails (商品详情模型)

`AppProductDetails` 表示一次性购买商品（消耗型或非消耗型）的详细信息。

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `productId` | String | 商品 ID（例如 `com.xx.product.1`） |
| `productName` | String | 商品名称 |
| `formattedPrice` | String | 格式化后的价格字符串（例如 `"€7.99"`） |
| `priceAmountMicros` | Long | 以微单位表示的价格（例如价格为 `"€7.99"` 时，该值为 `7990000`） |
| `priceCurrencyCode` | String | 货币代码（例如 `"EUR"`） |

**查询商品详情示例：**

```kotlin
val oneTimeService = GooglePayClient.getInstance().getPayService<OneTimeService>()

lifecycleScope.launch {
    oneTimeService.queryProductDetails(listOf("coin_100", "coin_500")).collect { result ->
        result.onSuccess { productList ->
            productList.forEach { product ->
                Log.d("Product", "ID: ${product.productId}")
                Log.d("Product", "名称: ${product.productName}")
                Log.d("Product", "价格: ${product.formattedPrice}")
                Log.d("Product", "货币: ${product.priceCurrencyCode}")
            }
        }
        result.onFailure { error ->
            Log.e("Product", "查询失败: ${error.message}")
        }
    }
}
```

### 4.5 AppSubscribeDetails (订阅详情模型)

`AppSubscribeDetails` 表示订阅商品的详细信息，包括定价阶梯。

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `productId` | String | 订阅商品 ID（例如 `com.xx.subscription.monthly`） |
| `productName` | String | 订阅商品名称 |
| `pricingPhases` | List&lt;PricingPhase&gt; | 定价阶梯列表（支持试用期、介绍价格等） |

**PricingPhase 字段：**

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| `formattedPrice` | String | 格式化后的价格字符串（例如 `"€7.99"`） |
| `priceAmountMicros` | Long | 以微单位表示的价格（例如价格为 `"€7.99"` 时，该值为 `7990000`） |
| `priceCurrencyCode` | String | 货币代码（例如 `"EUR"`） |

**查询订阅详情示例：**

```kotlin
val subscriptionService = GooglePayClient.getInstance().getPayService<SubscriptionService>()

lifecycleScope.launch {
    val params = SubsOfferParams.Builder()
        .setProductIds(listOf("monthly_vip", "yearly_vip"))
        .build()
    
    subscriptionService.querySubsOfferDetails(params).collect { result ->
        result.onSuccess { subscriptionList ->
            subscriptionList.forEach { subscription ->
                Log.d("Subscription", "ID: ${subscription.productId}")
                Log.d("Subscription", "名称: ${subscription.productName}")
                subscription.pricingPhases.forEach { phase ->
                    Log.d("Subscription", "阶梯价格: ${phase.formattedPrice}")
                }
            }
        }
    }
}
```

## 5. 支付事件监听 (Payment Event Listening)

`GooglePayClient` 使用 Kotlin Coroutines 的 `SharedFlow` 发送支付事件。

### 5.1 BillingPayEvent

支付事件密封类：

* `PaySuccessful(purchase: Purchase)`: 支付成功。
* `PayFailed(code: Int, message: String)`: 支付失败。
* `PayConsumeSuccessful(purchase: Purchase)`:  支付并消耗成功。
* `PayConsumeFailed(purchase: Purchase, code: Int, message: String)`:  支付成功但消耗失败。

### 5.2 在 Activity 中使用

建议在 `onCreate` 或 `onStart` 中开始收集事件，使用 `lifecycleScope` 确保在生命周期结束时自动取消。

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    lifecycleScope.launch {
        // 使用 repeatOnLifecycle 确保只在活跃状态下收集
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            GooglePayClient.getInstance().appBillingPayEventFlow.collect { event ->
                when (event) {
                    is BillingPayEvent.PaySuccessful -> {
                        // 处理支付成功
                        Log.d("Pay", "Success: ${event.purchase}")
                    }
                    is BillingPayEvent.PayFailed -> {
                        // 处理支付失败
                        Log.e("Pay", "Failed: ${event.code} - ${event.message}")
                    }
                    else -> {
                        // 其他状态
                    }
                }
            }
        }
    }
}
```

### 5.3 在 Dialog 中使用 (手动释放)

如果在 `Dialog` 中监听支付结果，需要注意协程作用域的管理。

1. **使用 Dialog 所在的 Activity/Fragment 的 lifecycleScope**: 这是最推荐的方式，与 Activity 生命周期绑定，无需手动释放。
2. **使用自定义 Scope**: 如果必须在 Dialog 内部管理，需要在 `dismiss` 时取消 Job。

```kotlin
class PayDialog(context: Context) : Dialog(context) {
    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun show() {
        super.show()
        dialogScope.launch {
            GooglePayClient.getInstance().appBillingPayEventFlow.collect { event ->
                // 处理事件
                if (event is BillingPayEvent.PaySuccessful) {
                    dismiss()
                }
            }
        }
    }

    override fun dismiss() {
        super.dismiss()
        // 必须手动取消，防止内存泄漏
        dialogScope.cancel()
    }
}
```

## 6. 其他注意事项

1. **生命周期**: `GooglePayClient` 内部处理了 Activity 的生命周期感知，但事件监听需要调用者自行管理 Scope。
2. **消耗**: 对于一次性消耗型商品，库会自动进行消耗操作 (`consumeAsync`)。
3. **确认**: 所有购买（包括订阅）都需要确认 (`acknowledgePurchase`)，这部分逻辑在 `GooglePayService.handlePurchasesProcess` 中由业务层触发或库内部处理（具体取决于 `handlePurchases` 的实现，库默认会自动处理消耗，但确认逻辑通常需要业务端配合验证后调用）。
