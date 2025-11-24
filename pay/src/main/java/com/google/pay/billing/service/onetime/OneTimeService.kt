package com.google.pay.billing.service.onetime

import android.app.Activity
import androidx.annotation.UiThread
import com.google.pay.billing.service.BillingBindService
import com.google.pay.billing.service.BillingService
import com.google.pay.model.AppBillingResult
import com.google.pay.model.AppProductDetails
import com.google.pay.model.BillingParams

/**
 * <pre>
 *     类描述  : 一次性& 消耗性商品服务
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
@BillingBindService("OneTimeService")
interface OneTimeService : BillingService {

    /**
     * 启动Google play 支付
     * @param activity
     * @param billingParams 由app端生成的购买参数
     * @return result 启动结果
     * */
    @UiThread
    fun launchBillingFlow(
        activity: Activity, billingParams: BillingParams
    ): AppBillingResult

    /**
     * 根据商品id查询商品详情
     * @param productIds 商品ids
     * @return list 查询结果列表
     * */
   suspend fun queryProductDetails(
        productIds: List<String>
    ) : List<AppProductDetails>


}