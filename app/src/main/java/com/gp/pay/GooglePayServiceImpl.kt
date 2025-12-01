package com.gp.pay

import android.util.Log
import com.android.billingclient.api.Purchase
import com.google.pay.GooglePayService
import com.google.pay.model.BillingProductType

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

    override suspend fun getOneTimeConsumableProducts(): List<String> {
        return arrayListOf("com.niki.product.1","com.niki.product.2","com.niki.product.3")
    }

    override suspend fun getOneTimeNonConsumableProducts(): List<String> {
        return arrayListOf("")
    }

    override suspend fun getSubscribeProducts(): List<String> {
        return arrayListOf("com.niki.vip.1week","com.niki.vip.1month")
    }

    override suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    ) {
        TODO("Not yet implemented")
    }


    override fun printLog(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}