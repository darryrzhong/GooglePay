package com.gp.pay

import android.util.Log
import com.android.billingclient.api.Purchase
import com.google.pay.AppBillingService
import com.google.pay.model.BillingProductType

/**
 * <pre>
 *     类描述  :
 *
 *     @author : never
 *     @since  : 2025/11/24
 * </pre>
 */
class AppBillingServiceImpl private constructor() : AppBillingService {

    companion object {
        val instance by lazy { AppBillingServiceImpl() }
    }

    override suspend fun getOneTimeConsumableProducts(): List<String> {
        return arrayListOf("")
    }

    override suspend fun getOneTimeNonConsumableProducts(): List<String> {
        return arrayListOf("")
    }

    override suspend fun getSubscribeProducts(): List<String> {
        return arrayListOf("")
    }

    override suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    ) {
        TODO("Not yet implemented")
    }

    override fun getGooglePayPubId(): String {
        return ""
    }

    override fun printLog(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}