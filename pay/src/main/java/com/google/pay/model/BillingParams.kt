package com.google.pay.model

import com.google.pay.model.BillingSubsParams.Builder
import java.io.Serializable

/**
 * <pre>
 *     类描述  : 一次性购买参数
 *
 *
 *     @author : never
 *     @since   : 2023/8/17
 * </pre>
 */
/**
 * @param accountId 业务方标识用户的唯一id
 * @param productId google play后天配置的商品id
 * @param chargeNo 在业务方生成的业务订单,用于绑定google play后台订单
 * */
class BillingParams private constructor(
    val accountId: String,
    val productId: String,
    val chargeNo: String
) : Serializable {

    fun toBuilder(): Builder {
        return Builder()
            .setAccountId(accountId)
            .setProductId(productId)
            .setChargeNo(chargeNo)
    }


    class Builder {
        private var accountId: String = ""
        private var productId: String = ""
        private var chargeNo: String = ""

        fun setAccountId(accountId: String) = apply {
            this.accountId = accountId
        }

        fun setProductId(productId: String) = apply {
            this.productId = productId
        }

        fun setChargeNo(chargeNo: String) = apply {
            this.chargeNo = chargeNo
        }

        fun build(): BillingParams {
            return BillingParams(accountId, productId, chargeNo)
        }
    }
}
