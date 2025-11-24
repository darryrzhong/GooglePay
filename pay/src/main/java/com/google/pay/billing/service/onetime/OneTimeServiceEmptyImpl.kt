package com.google.pay.billing.service.onetime

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.google.pay.model.AppBillingResult
import com.google.pay.model.AppProductDetails
import com.google.pay.model.BillingParams
import com.google.pay.model.BillingPayEvent
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
class OneTimeServiceEmptyImpl : OneTimeService {


    override fun launchBillingFlow(
        activity: Activity,
        billingParams: BillingParams
    ): AppBillingResult {
        return AppBillingResult()
    }

    override suspend fun queryProductDetails(productIds: List<String>): List<AppProductDetails> {
        return mutableListOf()
    }




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