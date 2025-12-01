package com.google.pay.billing.service.onetime

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.pay.AppBillingResponseCode
import com.google.pay.billing.GooglePayClient
import com.google.pay.handleTryEach
import com.google.pay.model.AppBillingResult
import com.google.pay.model.AppProductDetails
import com.google.pay.model.BillingParams
import com.google.pay.model.BillingPayEvent
import com.google.pay.model.BillingProductType
import com.google.pay.utils.PayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

/**
 * <pre>
 *     类描述  :一次性 & 消耗性商品服务
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
internal class OneTimeServiceImpl : OneTimeService {

    //google play商品详情列表
    private val productDetailsMap = ConcurrentHashMap<String, AppProductDetails>()
    private val googleProductDetailsMap = ConcurrentHashMap<String, ProductDetails>()

    //非消耗型商品,这种商品比较特殊,缓存一下
    private var _noConsumableProductIds = CopyOnWriteArrayList<String>()


    /**
     * 启动支付
     * */
    override fun launchBillingFlow(
        activity: Activity,
        billingParams: BillingParams
    ): AppBillingResult {
        val productId = billingParams.productId
        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.FAIL,
                "launch fail : The corresponding $productId could not be found,productDetails does not exist"
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The corresponding $productId could not be found,productDetails does not exist"
                )
            }
            return launchResult
        }
        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.SERVICE_DISCONNECTED,
                "launch fail : The app is not connected to the Play Store service via the Google Play Billing Library."
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The app is not connected to the Play Store service via the Google Play Billing Library."
                )
            }
            return launchResult
        }
        return launchBilling(activity, billingParams)
    }


    override suspend fun queryProductDetails() {
        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            return
        }
        //1.先从app业务端拉取服务端下发的Google play 后台配置的商品列表
        //消耗商品
        val consumableProductIds = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().appBillingService.getOneTimeConsumableProducts()
        }

        //非消耗商品
        val noConsumableProductIds = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().appBillingService.getOneTimeNonConsumableProducts()
        }
        val productIds = consumableProductIds + noConsumableProductIds
        if (productIds.isEmpty()) {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "queryProductList from app fail,productIds is empty"
                )
            }
            return
        }
        if (noConsumableProductIds.isNotEmpty()) {
            _noConsumableProductIds.clear()
            _noConsumableProductIds.addAll(noConsumableProductIds)
        }
        queryProductDetailsList(productIds)
    }

    override suspend fun queryProductDetails(productIds: List<String>): List<AppProductDetails> {
        val localDetails = mutableListOf<AppProductDetails>()
        if (productIds.isEmpty()) {
            return localDetails
        }
        //1. 查询本地缓存中是否存在
        productIds.forEach {
            productDetailsMap[it]?.let {
                localDetails.add(it)
            }
        }
        if (localDetails.isNotEmpty()) {
            return localDetails
        }

        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            return localDetails
        }
        //2. 去google play 查询
        return queryProductDetailsList(productIds)
    }


    override suspend fun queryPurchases() {
        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            return
        }
        val queryPurchasesParams =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        val purchasesResult = GooglePayClient.getInstance().getBillingClient()
            .queryPurchasesAsync(queryPurchasesParams)

        if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val purchases = purchasesResult.purchasesList
            handlePurchases(purchases, false)
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Get a list of unconsumed (inapp) transactions for Google payments $purchases"
                )
            }
        } else {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "fail code : ${purchasesResult.billingResult.responseCode} | message : ${purchasesResult.billingResult.debugMessage}"
            )
        }
    }


    /**
     * 处理购买结果，只有在状态为 PURCHASED 时，才去服务端校验订单，
     * 校验成功后进行消耗，完成一次完整的购买流程
     * @param purchases 订单
     * @param isPay 是否是购买
     * */
    override suspend fun handlePurchases(purchases: List<Purchase>, isPay: Boolean) {
        //判断一下是否是一次性商品的交易
        val oneTimeProductPurchaseList = purchases.filter { purchase ->
            purchase.products.all { product ->
                product in productDetailsMap.keys
            }
        }
        if (oneTimeProductPurchaseList.isEmpty()) {
            return
        }
        val purchasesList = oneTimeProductPurchaseList.filter {
            //只处理已经付款成功的订单
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (purchasesList.isEmpty()) {
            return
        }
        //处理购买交易
        purchasesList.forEach { purchase ->
            GooglePayClient.getInstance().appBillingService.handlePurchasesProcess(
                isPay,
                BillingProductType.INAPP, purchase
            )
        }
    }


    /**
     *  消耗订单-消耗型商品
     *  @param purchase 交易消息
     * @param isPay 是否是刚刚购买的
     * */
    override suspend fun consumePurchases(purchase: Purchase, isPay: Boolean) {
        //是否是非消耗品
        val isNoConsumePurchase = purchase.products.all { product ->
            product in _noConsumableProductIds
        }
        //非消耗品不能消耗,只能确认acknowledge
        if (isNoConsumePurchase && !purchase.isAcknowledged) {
            acknowledgePurchase(purchase, isPay)
            return
        }
        //去Google play 消耗掉订单
        val consumeParams =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken)
                .build()
        val consumeResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient().consumePurchase(consumeParams)
        }
        //google play 消耗成功
        if (consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (isPay) {
                //用户支付场景,需要去刷新一下金币余额等UI操作
                GooglePayClient.getInstance().appBillingPayEventFlow.emit(
                    BillingPayEvent.PayConsumeSuccessful(
                        purchase
                    )
                )
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Google play payments and consumption success ${purchase.originalJson}"
                )
            }
        } else {
            //google play 消耗失败
            if (isPay) {
                GooglePayClient.getInstance().appBillingPayEventFlow.emit(
                    BillingPayEvent.PayConsumeFailed(
                        purchase,
                        consumeResult.billingResult.responseCode,
                        consumeResult.billingResult.debugMessage
                    )
                )
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Google play payments and consumption fail ${purchase.originalJson}"
                )
            }
        }
        if (GooglePayClient.getInstance().deBug) {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "Google play payments and consumption : code : ${consumeResult.billingResult.responseCode} | message : ${consumeResult.billingResult.debugMessage}"
            )
        }
    }


    override fun observePayEvent(
        scope: CoroutineScope,
        onEvent: (BillingPayEvent) -> Unit
    ): Job {
        return GooglePayClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
            onEvent.invoke(event)
        }, catch = {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "observePayEvent catch: ${it.message}"
                )
            }
        }).launchIn(scope)

    }


    override fun observePayEventJava(callback: Consumer<BillingPayEvent>): Closeable {
        val job =
            GooglePayClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
                callback.accept(event)
            }, catch = {

            }).launchIn(GooglePayClient.getInstance().billingMainScope)

        return Closeable { job.cancel() }
    }


    /**
     * 启动google pay
     * Google Play 结算库版本 5.0+
     * */
    private fun launchBilling(
        activity: Activity, billingParams: BillingParams
    ): AppBillingResult {
        val productDetails = googleProductDetailsMap[billingParams.productId]
        if (productDetails == null) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.FAIL,
                "launch fail : The corresponding ${billingParams.productId} could not be found,googleProductDetails does not exist"
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The corresponding ${billingParams.productId} could not be found,googleProductDetails does not exist"
                )
            }
            return launchResult
        }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val jsonObject = JSONObject()
        jsonObject.put("charge_no", billingParams.chargeNo)
        jsonObject.put("sku_type", BillingClient.ProductType.INAPP)
        val billingFlowParams =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList)
                .setObfuscatedAccountId(billingParams.accountId)
                .setObfuscatedProfileId(jsonObject.toString()).build()
        val billingResult = GooglePayClient.getInstance().getBillingClient()
            .launchBillingFlow(activity, billingFlowParams)
        val launchResult = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            AppBillingResult(AppBillingResponseCode.OK, billingParams.chargeNo)
        } else {
            AppBillingResult(billingResult.responseCode, billingResult.debugMessage)
        }
        if (GooglePayClient.getInstance().deBug) {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "launchBillingFlow  : code : ${billingResult.responseCode} | message : ${billingResult.debugMessage}"
            )
        }
        return launchResult
    }


    /**
     *  确认订单-非消耗型商品
     *  @param purchase 交易消息
     * @param isPay 是否是刚刚购买的
     * */
    private suspend fun acknowledgePurchase(purchase: Purchase, isPay: Boolean) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
        val ackPurchaseResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient()
                .acknowledgePurchase(acknowledgePurchaseParams.build())
        }
        //google play 确认成功
        if (ackPurchaseResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (isPay) {
                //用户支付场景,需要去刷新一下金币余额等UI操作
                GooglePayClient.getInstance().appBillingPayEventFlow.emit(
                    BillingPayEvent.PayConsumeSuccessful(
                        purchase
                    )
                )
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Google play payments and acknowledge success ${purchase.originalJson}"
                )
            }
        } else {
            //google play 确认失败
            if (isPay) {
                GooglePayClient.getInstance().appBillingPayEventFlow.emit(
                    BillingPayEvent.PayConsumeFailed(
                        purchase,
                        ackPurchaseResult.responseCode,
                        ackPurchaseResult.debugMessage
                    )
                )
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Google play payments and acknowledge fail ${purchase.originalJson}"
                )
            }
        }
        if (GooglePayClient.getInstance().deBug) {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "Google play payments and acknowledge : code : ${ackPurchaseResult.responseCode} | message : ${ackPurchaseResult.debugMessage}"
            )
        }
    }


    /**
     * Google Play 结算库版本 5.0+
     * 根据productIds查询对应商品详情
     * @param productIds 商品ids
     * */
    private suspend fun queryProductDetailsList(
        productIds: List<String>
    ): List<AppProductDetails> {
        val productDetailsList = mutableListOf<AppProductDetails>()
        val inAppProductInfo = mutableListOf<QueryProductDetailsParams.Product>()
        for (productId in productIds) {
            val product = QueryProductDetailsParams.Product.newBuilder().setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP).build()
            inAppProductInfo.add(product)
        }
        val productDetailsParams =
            QueryProductDetailsParams.newBuilder().setProductList(inAppProductInfo).build()
        val productDetailsResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient()
                .queryProductDetails(productDetailsParams)
        }
        if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val productDetails = productDetailsResult.productDetailsList
            productDetails?.forEach { productDetail ->
                productDetail.oneTimePurchaseOfferDetails?.let {
                    val appProductDetails = AppProductDetails(
                        productDetail.productId,
                        productDetail.name,
                        it.formattedPrice,
                        it.priceAmountMicros,
                        it.priceCurrencyCode
                    )
                    productDetailsMap[productDetail.productId] = appProductDetails
                    googleProductDetailsMap[productDetail.productId] = productDetail
                    productDetailsList.add(appProductDetails)
                }
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Get a list of product (inapp) details configured by Google Play----> |productDetails : $productDetails"
                )
            }
            return productDetailsList
        } else {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "fail code : ${productDetailsResult.billingResult.responseCode} | message : ${productDetailsResult.billingResult.debugMessage}"
                )
            }
            return productDetailsList
        }
    }

}