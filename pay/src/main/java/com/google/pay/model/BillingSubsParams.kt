package com.mt.libpay.model

import java.io.Serializable

/**
 * <pre>
 *     类描述  :订阅购买参数
 *
 *
 *     @author : never
 *     @since   : 2023/11/20
 * </pre>
 */

/**
 * @param accountId 业务方标识用户的唯一id
 * @param productId google play后天配置的商品id
 * @param basePlanId 基础方案id
 * @param offerId 基础方案-优惠id
 * @param charge_no 在业务方生成的业务订单,用于绑定google play后台订单
 * @param purchaseToken 当前app有效订阅的凭证token
 * @param priceAmountMicros 价格微分单位 /1000000
 * @param priceCurrencyCode 货币符号 SGD
 * */
class BillingSubsParams private constructor(
    val accountId: String,
    val productId: String,
    val basePlanId: String,
    val offerId: String,
    val chargeNo: String,
    val purchaseToken: String,
    val priceAmountMicros: String,
    val priceCurrencyCode: String
) : Serializable {

    fun toBuilder(): Builder {
        return Builder()
            .setAccountId(accountId)
            .setProductId(productId)
            .setBasePlanId(basePlanId)
            .setOfferId(offerId)
            .setChargeNo(chargeNo)
            .setPurchaseToken(purchaseToken)
            .setPriceAmountMicros(priceAmountMicros)
            .setPriceCurrencyCode(priceCurrencyCode)
    }


    class Builder {
        private var accountId: String = ""
        private var productId: String = ""
        private var basePlanId: String = ""
        private var offerId: String = ""
        private var chargeNo: String = ""
        private var purchaseToken: String = ""
        private var priceAmountMicros: String = ""
        private var priceCurrencyCode: String = ""

        fun setAccountId(accountId: String) = apply { this.accountId = accountId }
        fun setProductId(productId: String) = apply { this.productId = productId }
        fun setBasePlanId(basePlanId: String) = apply { this.basePlanId = basePlanId }
        fun setOfferId(offerId: String) = apply { this.offerId = offerId }
        fun setChargeNo(chargeNo: String) = apply { this.chargeNo = chargeNo }
        fun setPurchaseToken(purchaseToken: String) = apply { this.purchaseToken = purchaseToken }
        fun setPriceAmountMicros(priceAmountMicros: String) =
            apply { this.priceAmountMicros = priceAmountMicros }

        fun setPriceCurrencyCode(priceCurrencyCode: String) =
            apply { this.priceCurrencyCode = priceCurrencyCode }

        fun build(): BillingSubsParams {
            return BillingSubsParams(
                accountId,
                productId,
                basePlanId,
                offerId,
                chargeNo,
                purchaseToken,
                priceAmountMicros,
                priceCurrencyCode
            )
        }
    }
}
