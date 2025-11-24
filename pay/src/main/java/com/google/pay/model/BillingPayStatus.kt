package com.google.pay.model

import com.android.billingclient.api.Purchase

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2025/7/25
 * </pre>
 */
sealed class BillingPayEvent {
    /**
     * 付款成功
     * @param purchase 购买信息
     * */
    data class PaySuccessful(var purchase: Purchase) : BillingPayEvent()

    /**
     * 付款失败
     *@param code 错误码
     * @param message 错误信息
     * */
    data class PayFailed(var code: Int, var message: String) : BillingPayEvent()

    /**
     * 付款成功 & 消耗成功
     * @param purchase 购买信息
     * */
    data class PayConsumeSuccessful(var purchase: Purchase) : BillingPayEvent()

    /**
     * 付款成功 & 消耗失败
     * @param purchase 购买信息
     * @param code 错误码
     * @param message 错误信息
     * */
    data class PayConsumeFailed(var purchase: Purchase, var code: Int, var message: String) :
        BillingPayEvent()

}