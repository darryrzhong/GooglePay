package com.google.pay.billing.service.subscription

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import com.google.pay.AppBillingResponseCode
import com.google.pay.billing.GooglePayClient
import com.google.pay.handleTryEach
import com.google.pay.model.AppBillingResult
import com.google.pay.model.AppSubscribeDetails
import com.google.pay.model.BillingPayEvent
import com.google.pay.model.BillingProductType
import com.google.pay.model.BillingSubsParams
import com.google.pay.model.PricingPhase
import com.google.pay.model.SubsOfferParams
import com.google.pay.model.SubscriptionMode
import com.google.pay.utils.PayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

/**
 * <pre>
 *     类描述  : 订阅商品服务
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
internal class SubscriptionServiceImpl : SubscriptionService {

    //google play商品详情列表
    private val subscribeDetailsMap = ConcurrentHashMap<String, AppSubscribeDetails>()
    private val googleSubscribeDetailsMap = ConcurrentHashMap<String, ProductDetails>()
    private val googleSkuSubscribeDetailsMap = ConcurrentHashMap<String, SkuDetails>()


    //当前已经确认有效的订阅列表，购买订阅之前需要根据这个列表来判断订阅的类型
    // 1：直接购买 or 订阅其他方案  2：升级or降级
    private val ackSubscribePurchasesMap = hashMapOf<String, Purchase>()


    /**
     * 启动Google play
     * */
    override fun launchBillingFlow(
        activity: Activity,
        billingSubsParams: BillingSubsParams
    ): AppBillingResult {
        val productId = billingSubsParams.productId
        val productDetails = subscribeDetailsMap[productId]
        if (productDetails == null) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.FAIL,
                "launch fail : The corresponding $productId could not be found,subsProductDetails does not exist"
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The corresponding $productId could not be found,subsProductDetails does not exist"
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

        val isOldVersion = GooglePayClient.getInstance().isOldVersion()
        return if (isOldVersion) {
            launchBillingSku(activity, billingSubsParams)
        } else {
            launchBilling(activity, billingSubsParams)
        }
    }


    override suspend fun queryProductDetails() {
        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            return
        }
        //先从app业务端拉取服务端下发的Google play 后台配置的商品列表
        val productIds = GooglePayClient.getInstance().appBillingService.getSubscribeProducts()
        if (productIds.isEmpty()) {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "querySubscribeProductList from app fail,productIds is empty"
                )

            }
            return
        }
        val isOldVersion = GooglePayClient.getInstance().isOldVersion()
        if (isOldVersion) {
            querySkuSubscribeProductDetails(productIds)
        } else {
            querySubscribeProductDetails(productIds)
        }
    }


    override suspend fun queryPurchases() {
        val state = GooglePayClient.getInstance().checkClientState()
        if (!state) {
            return
        }
        val queryPurchasesParams =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                .build()
        val purchasesResult = GooglePayClient.getInstance().getBillingClient().queryPurchasesAsync(
            queryPurchasesParams
        )
        if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val purchases = purchasesResult.purchasesList
            handlePurchases(purchases, false)
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Get a list of unconsumed (subs) transactions for Google payments $purchases"
                )
            }
        } else {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "fail code : ${purchasesResult.billingResult.responseCode} | message : ${purchasesResult.billingResult.debugMessage}"
            )
        }
    }


    override suspend fun handlePurchases(purchases: List<Purchase>, isPay: Boolean) {
        //判断一下是否是订阅的交易
        val subscriptionPurchaseList = purchases.filter { purchase ->
            purchase.products.all { product ->
                product in subscribeDetailsMap.keys
            }
        }
        if (subscriptionPurchaseList.isEmpty()) {
            return
        }
        //处理已经付款成功 & 已经确认的订阅的订单
        val ackPurchases = subscriptionPurchaseList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && it.isAcknowledged
        }
        //根据订阅商品id存储对应的订阅Purchase
        ackPurchases.forEach {
            it.products.forEach { productId ->
                ackSubscribePurchasesMap[productId] = it
            }
        }

        //处理已经付款成功 && 且没有确认订阅的订单
        val unAckPurchases = subscriptionPurchaseList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
        }
        if (unAckPurchases.isEmpty()) {
            return
        }
        unAckPurchases.forEach { purchase ->
            GooglePayClient.getInstance().appBillingService.handlePurchasesProcess(
                isPay,
                BillingProductType.SUBS,
                purchase
            )
        }
    }


    /**
     *  消耗订单
     * @param isPay 是否是刚刚购买的
     * */
    override suspend fun consumePurchases(purchase: Purchase, isPay: Boolean) {
        if (!PayUtils.isSignatureValid(purchase)) {
            //验证签名失败
            return
        }
        //去Google play 消耗掉订单
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        val ackPurchaseResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient()
                .acknowledgePurchase(acknowledgePurchaseParams)
        }
        //google play 消耗成功
        if (ackPurchaseResult.responseCode == BillingClient.BillingResponseCode.OK) {
            if (isPay) {
                //用户支付场景,需要去刷新一下金币余额等UI操作
                GooglePayClient.getInstance().appBillingPayEventFlow.emit(
                    BillingPayEvent.PayConsumeSuccessful(
                        purchase
                    )
                )
                //根据订阅商品id存储对应的订阅Purchase
                purchase.products.forEach { productId ->
                    ackSubscribePurchasesMap[productId] = purchase
                }
                if (GooglePayClient.getInstance().deBug) {
                    GooglePayClient.getInstance().appBillingService.printLog(
                        GooglePayClient.TAG,
                        "Google play payments and consumption (subs) success ${purchase.originalJson}"
                    )
                }
            }
        } else {
            //google play 消耗失败
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
                    "Google play payments and consumption (subs) fail ${purchase.originalJson}"
                )
            }
        }
        if (GooglePayClient.getInstance().deBug) {
            GooglePayClient.getInstance().appBillingService.printLog(
                GooglePayClient.TAG,
                "Google play payments and consumption (subs) : code : ${ackPurchaseResult.responseCode} | message : ${ackPurchaseResult.debugMessage}"
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
     * */
    private fun launchBilling(
        activity: Activity,
        billingSubsParams: BillingSubsParams
    ): AppBillingResult {
        val productId = billingSubsParams.productId
        val productDetails = googleSubscribeDetailsMap[productId]
        if (productDetails == null) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.FAIL,
                "launch fail : The corresponding ${billingSubsParams.productId} could not be found,googleSubProductDetails does not exist"
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The corresponding ${billingSubsParams.productId} could not be found,googleSubProductDetails does not exist"
                )
            }
            return launchResult
        }
        //构建订阅购买参数
        val billingFlowParams =
            builderBillingFlowParamsBuilder(productId, productDetails!!, billingSubsParams)
                ?: return AppBillingResult(
                    AppBillingResponseCode.FAIL,
                    "billingFlowParams is null"
                )
        val billingResult = GooglePayClient.getInstance().getBillingClient()
            .launchBillingFlow(activity, billingFlowParams)
        val launchResult = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            AppBillingResult(AppBillingResponseCode.OK, billingSubsParams.chargeNo)
        } else {
            AppBillingResult(AppBillingResponseCode.FAIL, billingResult.debugMessage)
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
     * 启动google pay
     * */
    private fun launchBillingSku(
        activity: Activity,
        billingSubsParams: BillingSubsParams
    ): AppBillingResult {
        val productId = billingSubsParams.productId
        val skuDetails = googleSkuSubscribeDetailsMap[productId]
        if (skuDetails == null) {
            val launchResult = AppBillingResult(
                AppBillingResponseCode.FAIL,
                "launch fail : The corresponding ${billingSubsParams.productId} could not be found,googleSubSkuDetails does not exist"
            )
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "launch fail : The corresponding ${billingSubsParams.productId} could not be found,googleSubSkuDetails does not exist"
                )
            }
            return launchResult
        }
        val jsonObject = JSONObject()
        jsonObject.put("subscription_no", billingSubsParams.chargeNo)
        jsonObject.put("sku_type", BillingClient.ProductType.SUBS)
        val billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails!!)
            .setObfuscatedAccountId(billingSubsParams.accountId)
            .setObfuscatedProfileId(jsonObject.toString()).build()

        val billingResult = GooglePayClient.getInstance().getBillingClient()
            .launchBillingFlow(activity, billingFlowParams)
        val launchResult = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            AppBillingResult(AppBillingResponseCode.OK, billingSubsParams.chargeNo)
        } else {
            AppBillingResult(AppBillingResponseCode.FAIL, billingResult.debugMessage)
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
     * Google Play 结算库版本 5.0+
     * 根据productIds查询对应订阅商品详情
     * @param productIds 商品ids
     * */
    private suspend fun querySubscribeProductDetails(
        productIds: List<String>
    ) {
        val inAppProductInfo = mutableListOf<QueryProductDetailsParams.Product>()
        for (productId in productIds) {
            val product = QueryProductDetailsParams.Product.newBuilder().setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS).build()
            inAppProductInfo.add(product)
        }
        val productDetailsParams =
            QueryProductDetailsParams.newBuilder().setProductList(inAppProductInfo).build()
        val productDetailsResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient().queryProductDetails(
                productDetailsParams
            )
        }

        if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val productDetails = productDetailsResult.productDetailsList
            productDetails?.forEach { productDetail ->
                productDetail.subscriptionOfferDetails?.takeIf { it.isNotEmpty() }?.let {
                    val appSubscribeDetails = AppSubscribeDetails(
                        productDetail.productId, productDetail.name
                    )
                    //本地缓存订阅商品id
                    subscribeDetailsMap[productDetail.productId] = appSubscribeDetails
                    googleSubscribeDetailsMap[productDetail.productId] = productDetail
                }
            }
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "Get a list of subscribeProduct (subs) details configured by Google Play----> |productDetails : $productDetails"
                )
            }
        } else {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "fail code : ${productDetailsResult.billingResult.responseCode} | message : ${productDetailsResult.billingResult.debugMessage}"
                )
            }
        }
    }

    /**
     * Google Play 结算库版本 5.0 之前
     * 根据productIds查询对应订阅商品详情
     * @param productIds 商品ids
     * */
    private suspend fun querySkuSubscribeProductDetails(
        productIds: List<String>
    ) {
        val skuDetailsParams = SkuDetailsParams.newBuilder().setSkusList(productIds)
            .setType(BillingClient.ProductType.SUBS).build()
        val skuDetailsResult = withContext(Dispatchers.IO) {
            GooglePayClient.getInstance().getBillingClient()
                .querySkuDetails(skuDetailsParams)
        }
        if (skuDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val skuDetails = skuDetailsResult.skuDetailsList
            skuDetails?.let {
                it.forEach { skuDetail ->
                    subscribeDetailsMap[skuDetail.sku] = AppSubscribeDetails(
                        skuDetail.sku,
                        skuDetail.title
                    )
                    googleSkuSubscribeDetailsMap[skuDetail.sku] = skuDetail
                }
                if (GooglePayClient.getInstance().deBug) {
                    GooglePayClient.getInstance().appBillingService.printLog(
                        GooglePayClient.TAG,
                        "Get a list of skuSubscribeProduct (subs) details configured by Google Play----> |skuDetails : $skuDetails"
                    )
                }
            }
        } else {
            if (GooglePayClient.getInstance().deBug) {
                GooglePayClient.getInstance().appBillingService.printLog(
                    GooglePayClient.TAG,
                    "fail code : ${skuDetailsResult.billingResult.responseCode} | message : ${skuDetailsResult.billingResult.debugMessage}"
                )
            }
        }
    }


    override fun querySubsOfferDetails(subsOfferParams: SubsOfferParams): AppSubscribeDetails? {
        val isOldVersion = GooglePayClient.getInstance().isOldVersion()
        //兼容4.0老版本
        if (isOldVersion) {
            val skuDetails = googleSkuSubscribeDetailsMap[subsOfferParams.productId] ?: return null
            return AppSubscribeDetails(
                skuDetails.sku,
                skuDetails.title,
                mutableListOf(
                    PricingPhase(
                        skuDetails.price,
                        skuDetails.priceAmountMicros,
                        skuDetails.priceCurrencyCode
                    )
                )
            )
        }

        val productDetails = googleSubscribeDetailsMap[subsOfferParams.productId] ?: return null
        val subscriptionOfferDetails =
            PayUtils.getSubsOfferDetails(productDetails, subsOfferParams) ?: return null
        subscriptionOfferDetails.let {
            val pricingPhases = mutableListOf<PricingPhase>()
            for (data in it.pricingPhases.pricingPhaseList) {
                pricingPhases.add(
                    PricingPhase(
                        data.formattedPrice,
                        data.priceAmountMicros,
                        data.priceCurrencyCode
                    )
                )
            }
            return AppSubscribeDetails(productDetails.productId, productDetails.name, pricingPhases)
        }
    }

    /**
     * 根据订阅Id查询当前有效订阅信息
     * @param productIds 需要查询的订阅Id ,如果不传id则查询当前所有的有效订阅信息
     * @return 返回对应订阅Id对应的订阅详情
     * */
    override fun queryAckSubscribePurchases(productIds: List<String>?): Map<String, Purchase> {
        val purchaseMap = hashMapOf<String, Purchase>()
        if (productIds == null) {
            purchaseMap.putAll(ackSubscribePurchasesMap)
            return purchaseMap
        }
        for (productId in productIds) {
            ackSubscribePurchasesMap[productId]?.let {
                purchaseMap[productId] = it
            }
        }
        return purchaseMap
    }


    /**
     * 检查订阅购买的场景
     * 1.同一种订阅的不同方案
     * 2.不同订阅的购买
     * */
    private fun checkProductUpDowngrade(
        productId: String,
        billingSubsParams: BillingSubsParams
    ): String? {
        //本地存在有效的相同订阅类别
        val purchase = ackSubscribePurchasesMap[productId]
        if (purchase != null) {
            return purchase.purchaseToken
        }
        //业务端下发了有效订阅
        if (billingSubsParams.purchaseToken.isNotEmpty()) {
            return billingSubsParams.purchaseToken
        }
        return null
    }

    /**
     * BillingFlowParams 生成器，用于升级和降级。
     * @param billingSubsParams 购买参数
     * @param productDetails 商品详情
     * */
    private fun upDowngradeBillingFlowParamsBuilder(
        purchaseToken: String?, productDetails: ProductDetails, billingSubsParams: BillingSubsParams
    ): BillingFlowParams? {
        if (purchaseToken.isNullOrEmpty()) {
            return null
        }
        //现存的有效订阅的购买token
        val oldToken = purchaseToken
        val offerToken = PayUtils.getSubsOfferToken(
            productDetails, SubsOfferParams(
                basePlanId = billingSubsParams.basePlanId, offerId = billingSubsParams.offerId
            )
        )
        val jsonObject = JSONObject()
        jsonObject.put("subscription_no", billingSubsParams.chargeNo)
        jsonObject.put("sku_type", BillingClient.ProductType.SUBS)
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails).setOfferToken(offerToken).build()
            )
        ).setSubscriptionUpdateParams(
            BillingFlowParams.SubscriptionUpdateParams.newBuilder().setOldPurchaseToken(oldToken)
                //直接收费模式 ReplacementMode.CHARGE_FULL_PRICE
                .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
                .build()

        ).setObfuscatedAccountId(billingSubsParams.accountId)
            .setObfuscatedProfileId(jsonObject.toString()).build()
    }

    /**
     * BillingFlowParams 生成器，用于升级和降级。
     * @param billingSubsParams 购买参数
     * @param productDetails 商品详情
     *
     * */
    private fun billingFlowParamsBuilder(
        productDetails: ProductDetails, billingSubsParams: BillingSubsParams
    ): BillingFlowParams {
        val offerToken = PayUtils.getSubsOfferToken(
            productDetails, SubsOfferParams(
                basePlanId = billingSubsParams.basePlanId, offerId = billingSubsParams.offerId
            )
        )
        val jsonObject = JSONObject()
        jsonObject.put("subscription_no", billingSubsParams.chargeNo)
        jsonObject.put("sku_type", BillingClient.ProductType.SUBS)
        return BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails).setOfferToken(offerToken).build()
            )
        ).setObfuscatedAccountId(billingSubsParams.accountId)
            .setObfuscatedProfileId(jsonObject.toString()).build()
    }

    private fun builderBillingFlowParamsBuilder(
        productId: String,
        productDetails: ProductDetails,
        billingSubsParams: BillingSubsParams,
    ): BillingFlowParams? {
        var cruAckSubscribe: Purchase? = null
        //单订阅模式,直接将当前订阅替换成目标订阅
        if (GooglePayClient.getInstance().subscriptionMode == SubscriptionMode.SingleMode) {
            //当前本地存在Google查询的有效订阅
            if (ackSubscribePurchasesMap.isNotEmpty()) {
                val purchaseToken = ackSubscribePurchasesMap.values.toMutableList()[0].purchaseToken
                return upDowngradeBillingFlowParamsBuilder(
                    purchaseToken,
                    productDetails,
                    billingSubsParams
                )
            }
            //当前本地不存在有效订阅,但业务端下发了有效订阅
            if (billingSubsParams.purchaseToken.isNotEmpty()) {
                return upDowngradeBillingFlowParamsBuilder(
                    billingSubsParams.purchaseToken,
                    productDetails,
                    billingSubsParams
                )
            }
            //当前没有有效订阅,直接购买
            return billingFlowParamsBuilder(productDetails, billingSubsParams)

        }
        //多订阅模式,多个订阅可以同时存在,单个订阅组内可以升级 or 降级
        //是否订阅升降级
        val purchaseToken = checkProductUpDowngrade(productId, billingSubsParams)
        val billingFlowParams = if (!purchaseToken.isNullOrEmpty()) {
            //构造升级 or 降级购买参数
            upDowngradeBillingFlowParamsBuilder(purchaseToken, productDetails, billingSubsParams)
        } else {
            billingFlowParamsBuilder(productDetails, billingSubsParams)
        }

        return billingFlowParams
    }


}