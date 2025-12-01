package com.google.pay.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.pay.GooglePayService
import com.google.pay.billing.service.BillingServiceManager
import com.google.pay.billing.service.BillingServiceManager.getService
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.billing.service.subscription.SubscriptionService
import com.google.pay.lifecyc.ActivityLifecycleCallback
import com.google.pay.model.BillingPayEvent
import com.google.pay.model.SubscriptionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * <pre>
 *     类描述  :  app内与Google支付库建立通信的client
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
class GooglePayClient private constructor() {

    private val billingScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal val billingMainScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    //支付状态事件通知
    internal var appBillingPayEventFlow =
        MutableSharedFlow<BillingPayEvent>(0, 100, BufferOverflow.DROP_OLDEST)
//   internal val appBillingPayEventFlow = _appBillingPayEventFlow.asSharedFlow()

    @Volatile
    private var tries = 1  //当前连接重试次数

    @Volatile
    private var isConnectionEstablished = false //是否已经连接

    // 使用原子布尔值作为“锁”，防止在同一时刻发起多个重连请求，避免竞态条件。
    private val isReconnecting = AtomicBoolean(false)

    internal var deBug: Boolean = false

    private var subscription: Boolean = false

    //订阅模式 默认支持多订阅模式
    internal var subscriptionMode = SubscriptionMode.MultiModal

    private lateinit var applicationContext: Context

    private val activityLifecycleCallback = ActivityLifecycleCallback()

    internal lateinit var appBillingService: GooglePayService

    //用单例，以避免对某一个事件进行多次 PurchasesUpdatedListener 回调
    private val billingClient: BillingClient by lazy {
        checkInitialized()
        BillingClient.newBuilder(applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    //购买交易更新监听，需要在初始化BillingClient时设置
    //当前购买都会回调此监听
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                //购买商品成功
                if (purchases != null) {
                    for (purchase in purchases) {
                        billingScope.launch {
                            appBillingPayEventFlow.emit(BillingPayEvent.PaySuccessful(purchase))
                        }
                        if (deBug) {
                            appBillingService.printLog(
                                TAG,
                                "Google play payment successfully | purchases: ${purchase.originalJson}"
                            )
                        }
                    }
                    //去消耗
                    billingScope.launch {
                        handlePurchases(purchases, true)
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                //取消购买不需要给与用户提示
//                billingScope.launch {
//                    appBillingPayEventFlow.emit(
//                        BillingPayEvent.PayFailed(
//                            billingResult.responseCode,
//                            billingResult.debugMessage
//                        )
//                    )
//                }
                if (deBug) {
                    appBillingService.printLog(
                        TAG,
                        "Google play payment user canceled | message : ${billingResult.debugMessage}"
                    )
                }
            }

            else -> {
                //购买失败，具体异常码可以到BillingClient.BillingResponseCode中查看
                billingScope.launch {
                    appBillingPayEventFlow.emit(
                        BillingPayEvent.PayFailed(
                            billingResult.responseCode,
                            billingResult.debugMessage
                        )
                    )
                }
                if (deBug) {
                    appBillingService.printLog(
                        TAG,
                        "Google play payment failed | code : ${billingResult.responseCode} | message : ${billingResult.debugMessage}"
                    )
                }
            }
        }
    }

    /**
     * 获取服务-Java调用
     * @param clazz 服务类型
     * */
    fun <T> getPayService(clazz: Class<T>): T {
        return BillingServiceManager.getService(clazz)
    }

    @PublishedApi
    internal fun <T> internalGetService(clazz: Class<T>): T {
        return BillingServiceManager.getService(clazz)
    }

    public inline fun <reified T> getPayService(): T {
        return internalGetService(T::class.java)
    }


    fun queryPurchases() {
        billingScope.launch {
            //1. 刷新一下真实配置列表
            getService<OneTimeService>().queryProductDetails()
            if (subscription) {
                getService<SubscriptionService>().queryProductDetails()
            }
            // 2. 刷新一下库存消耗
            getService<OneTimeService>().queryPurchases()
            if (subscription) {
                getService<SubscriptionService>().queryPurchases()
            }
        }

    }


    private suspend fun handlePurchases(purchases: List<Purchase>, isPay: Boolean) {
        getService<OneTimeService>().handlePurchases(purchases, isPay)
        if (subscription) {
            getService<SubscriptionService>().handlePurchases(purchases, isPay)
        }
    }

    internal fun getBillingClient(): BillingClient {
        return billingClient
    }


    fun initBillingClient(context: Application, appBillingService: GooglePayService) = apply {
        applicationContext = context
        this.appBillingService = appBillingService
        context.registerActivityLifecycleCallbacks(activityLifecycleCallback)
        ProcessLifecycleOwner.get().lifecycle.addObserver(activityLifecycleCallback)
    }

    fun setDebug(isDebug: Boolean) = apply {
        deBug = isDebug
    }

    fun setInterval(interval: Int) = apply {
        activityLifecycleCallback.refreshInterval = interval
    }

    fun registerActivitys(activitys: List<Class<out Activity>>) = apply {
        activityLifecycleCallback.activityList.addAll(activitys)
    }

    fun setSubscription(isSubscription: Boolean) = apply {
        subscription = isSubscription
    }

    fun setSubscriptionMode(subscriptionMode: SubscriptionMode) = apply {
        this.subscriptionMode = subscriptionMode
    }

    private fun checkInitialized() {
        if (!this::applicationContext.isInitialized || !this::appBillingService.isInitialized) {
            throw RuntimeException("Please call the initBillingClient method in application first")
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }


    fun refreshPurchases() {
        billingScope.launch {
            getService<OneTimeService>().queryPurchases()
            if (subscription) {
                getService<SubscriptionService>().queryPurchases()
            }
        }
    }

    /**
     * 检查客户端连接状态
     * */
    fun checkClientState(): Boolean {
        return billingClient.isReady
    }

    fun isOldVersion(): Boolean {
        val isProductDetailsSupported =
            billingClient.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS).responseCode
        if (deBug) {
            appBillingService.printLog(
                TAG,
                "isProductDetailsSupported == $isProductDetailsSupported"
            )
            Log.d(TAG, "isProductDetailsSupported == $isProductDetailsSupported")
        }
        if (isProductDetailsSupported == BillingClient.BillingResponseCode.OK) {
            return false
        }
        if (isProductDetailsSupported == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED) {
            return true
        }
        return false
        //        val isSubscriptionsUpdateSupported =
//            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE).responseCode == BillingClient.BillingResponseCode.OK
//        val isSubscriptionsSupported =
//            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode == BillingClient.BillingResponseCode.OK
//        Log.d(TAG, "isSubscriptionsUpdateSupported == $isSubscriptionsUpdateSupported")
//        Log.d(TAG, "isSubscriptionsSupported == $isSubscriptionsSupported")
    }

    /**
     * google play 是否可用
     * */
    fun isGoogleAvailable(context: Context? = null): Boolean {
        val resultCode = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context ?: applicationContext)
        if (deBug && resultCode != ConnectionResult.SUCCESS) {
            appBillingService.printLog(
                TAG,
                "resultCode : $resultCode | Google Play Services  unavailable on device"
            )
        }
        return resultCode == ConnectionResult.SUCCESS
//        return false
    }


    companion object {
        const val TAG = "GooglePayClient"
        private const val MAX_RETRY_ATTEMPT = 3

        @Volatile
        private var instance: GooglePayClient? = null

        @JvmStatic
        fun getInstance(): GooglePayClient {
            return instance ?: synchronized(this) {
                instance ?: GooglePayClient().also { instance = it }
            }
        }
    }
}