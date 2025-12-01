package com.gp.pay

import android.util.Log
import com.android.billingclient.api.Purchase
import com.google.pay.GooglePayService
import com.google.pay.billing.GooglePayClient
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.billing.service.subscription.SubscriptionService
import com.google.pay.model.BillingProductType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * <pre>
 *     类描述  :
 *
 *     @author : never
 *     @since  : 2025/11/24
 * </pre>
 */
class GooglePayServiceImpl private constructor() : GooglePayService {

    companion object {
        val instance by lazy { GooglePayServiceImpl() }
    }

    override suspend fun getOneTimeConsumableProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            //网络请求获取google play后台添加的商品productId
            val productIds =
                arrayListOf("com.niki.product.1", "com.niki.product.2", "com.niki.product.3")
            continuation.resume(productIds)
        }

    override suspend fun getOneTimeNonConsumableProducts(): List<String> {
        return arrayListOf("")
    }

    override suspend fun getSubscribeProducts(): List<String> =
        suspendCancellableCoroutine { continuation ->
            //网络请求获取google play后台添加的商品productId
            val productIds =
                arrayListOf("com.niki.product.1", "com.niki.product.2", "com.niki.product.3")
            continuation.resume(productIds)
        }

    override suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    ) {
        // 1. 根据业务需要,去服务端验证交易
        when (productType) {
            BillingProductType.INAPP -> {
//                consumeOneTimePurchase(isPay, purchases)
                // 2.服务端验证完成后,根据业务决定是否客户端消耗 or 服务端消耗该笔交易
                GooglePayClient.getInstance().getPayService<OneTimeService>()
                    .consumePurchases(purchases, isPay)
            }

            BillingProductType.SUBS -> {
//                consumeSubscribePurchase(isPay, purchases)
                // 2.服务端验证完成后,根据业务决定是否客户端消耗 or 服务端消耗该笔交易
                GooglePayClient.getInstance().getPayService<SubscriptionService>()
                    .consumePurchases(purchases, isPay)
            }
        }

    }


    override fun printLog(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}