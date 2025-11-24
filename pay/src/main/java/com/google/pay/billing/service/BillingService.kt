package com.mt.libpay.billing.service

import com.android.billingclient.api.Purchase
import com.mt.libpay.model.BillingPayEvent
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
 *     @since   : 2023/11/14
 * </pre>
 */
interface BillingService {
    /**
     * 根据业务端接口获取的商品列表，查询Google play 后台配置的可用的商品列表详情，
     * */
    suspend fun queryProductDetails()

    /**
     * 查询已经购买过但是没有被消耗的商品，可能网络不稳定或者中断导致的未被消耗
     * 如果购买成功没消耗，就去消耗，消耗完成视为完整的流程。
     * 否则三天没有消耗 Google 会进行退款
     */
    suspend fun queryPurchases()


    /**
     * 处理购买结果，只有在状态为 PURCHASED 时才进行消耗，完成一次完整的购买流程
     * @param purchases 订单
     * @param isPay 是否用户主动购买触发
     * */
   suspend fun handlePurchases(purchases: List<Purchase>, isPay: Boolean)


    /**
     *  去google play 消耗订单
     * @param isPay 是否是刚刚购买的
     * */
    suspend fun consumePurchases(purchase: Purchase, isPay: Boolean)

    /**
     * 监听支付事件
     * 注意:::
     * 如果使用lifecycleScope会自动取消监听
     * 否则需要手动取消监听
     * @param scope 协程作用域,推荐使用lifecycleScope
     * @param onEvent 支付事件
     * @return job 取消监听
     * */
    fun observePayEvent(scope: CoroutineScope, onEvent: (BillingPayEvent) -> Unit): Job


    /**
     * 监听支付事件
     * 注意::: 需要手动取消监听
     * @param callback 支付事件
     * @return Closeable 取消监听
     * */
    fun observePayEventJava(
        callback: Consumer<BillingPayEvent>
    ): Closeable

}