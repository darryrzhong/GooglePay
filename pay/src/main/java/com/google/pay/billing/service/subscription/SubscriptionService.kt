package com.mt.libpay.billing.service.subscription

import android.app.Activity
import androidx.annotation.UiThread
import com.android.billingclient.api.Purchase
import com.mt.libpay.billing.service.BillingBindService
import com.mt.libpay.billing.service.BillingService
import com.mt.libpay.model.AppBillingResult
import com.mt.libpay.model.AppSubscribeDetails
import com.mt.libpay.model.BillingSubsParams
import com.mt.libpay.model.SubsOfferParams

/**
 * <pre>
 *     类描述  : 订阅商品服务
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
@BillingBindService("SubscriptionService")
interface SubscriptionService : BillingService {

    /**
     * 启动Google play 支付
     * @param activity
     * @param billingSubsParams 由app端生成的购买参数
     * @return result 启动结果
     * */
    @UiThread
    fun launchBillingFlow(
        activity: Activity,
        billingSubsParams: BillingSubsParams
    ) : AppBillingResult


    /**
     * 查询当前订阅商品详情
     * @param subsOfferParams 订阅商品基础信息
     * */
    fun querySubsOfferDetails(
        subsOfferParams: SubsOfferParams
    ): AppSubscribeDetails?


    /**
     * 根据订阅Id查询当前有效订阅信息
     * @param productIds 需要查询的订阅Id ,如果不传id则查询当前所有的有效订阅信息
     * @return 返回对应订阅Id对应的订阅详情
     * */
    fun queryAckSubscribePurchases(productIds: List<String>? = null): Map<String, Purchase>

}