package com.google.pay.billing.service.subscription

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.google.pay.model.AppBillingResult
import com.google.pay.model.AppSubscribeDetails
import com.google.pay.model.BillingPayEvent
import com.google.pay.model.BillingSubsParams
import com.google.pay.model.SubsOfferParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.io.Closeable
import java.util.function.Consumer

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2024/12/10
 * </pre>
 */
class SubscriptionServiceEmptyImpl : SubscriptionService {
    override fun launchBillingFlow(
        activity: Activity,
        billingSubsParams: BillingSubsParams
    ): AppBillingResult {
        return AppBillingResult()
    }


    override fun querySubsOfferDetails(subsOfferParams: SubsOfferParams): AppSubscribeDetails? =
        null

    override fun queryAckSubscribePurchases(productIds: List<String>?): Map<String, Purchase> =
        hashMapOf()

    override suspend fun queryProductDetails() {
    }

    override suspend fun queryPurchases() {
    }

    override suspend fun handlePurchases(
        purchases: List<Purchase>,
        isPay: Boolean
    ) {
    }


    override suspend fun consumePurchases(
        purchase: Purchase,
        isPay: Boolean
    ) {
    }

    override fun observePayEvent(
        scope: CoroutineScope,
        onEvent: (BillingPayEvent) -> Unit
    ): Job {
        return Job()
    }

    override fun observePayEventJava(callback: Consumer<BillingPayEvent>): Closeable {
        return Closeable {}
    }


}