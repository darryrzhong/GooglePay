package com.google.pay.utils

import com.android.billingclient.api.Purchase
import com.google.pay.model.BillingProductType
import com.google.pay.model.PurchaseProfileInfo
import org.json.JSONObject

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2025/7/30
 * </pre>
 */
object PurchaseHelper {
    /**
     * 获取对应交易中的用户唯一标识信息
     * 一般是业务订单 和 交易商品类型
     * @param purchase 交易信息
     * @return PurchaseProfileInfo 唯一标识信息
     * */
    fun getProfileIdInfo(purchase: Purchase): PurchaseProfileInfo? {
        return runCatching {
            val originalJsonObject = JSONObject(purchase.originalJson)
            val profileJson =
                originalJsonObject.optString("obfuscatedProfileId").takeIf { it.isNotEmpty() }
                    ?: return null
            val profileJsonObject = JSONObject(profileJson)
            when (profileJsonObject.optString("sku_type")) {
                "subs" -> {
                    val changeNo = profileJsonObject.optString("subscription_no")
                    PurchaseProfileInfo(BillingProductType.SUBS, changeNo)
                }

                "inapp" -> {
                    val changeNo = profileJsonObject.optString("charge_no")
                    PurchaseProfileInfo(BillingProductType.INAPP, changeNo)
                }

                else -> null
            }

        }.getOrNull()
    }


}