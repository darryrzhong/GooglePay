package com.mt.libpay

import com.android.billingclient.api.Purchase
import com.mt.libpay.model.BillingProductType

/**
 * <pre>
 *     类描述  : 业务端服务类
 *     支付组件需要依赖业务端的服务,
 *     例如: 提供google play后台的一些配置数据;
 *     业务端创建业务订单等
 *
 *
 *     @author : never
 *     @since   : 2023/8/15
 * </pre>
 */
interface AppBillingService {

    /**
     * 获取google play后台配置的一次性消耗型商品的productID
     * @return 商品id列表
     * */
    suspend fun getOneTimeConsumableProducts(): List<String>


    /**
     * 获取google play后台配置的一次性非消耗型商品的productID
     *
     * @return 商品id列表
     * */
    suspend fun getOneTimeNonConsumableProducts(): List<String>

    /**
     * 获取google play后台配置的订阅商品的 productID
     *
     * @return 商品id列表
     * */
    suspend fun getSubscribeProducts(): List<String>

    /**
     * 处理用户购买交易
     * 注意::: 必须在三天内完成交易确认,以免google自动退款
     * 用户交易完成后(付款成功),一般需要如下操作:
     * 1.去服务器验证购买交易(可选)
     * 2.向google 确认购买交易(必需) 客户端确认 or 服务端确认
     * @param isPay 这次处理过程是否用户主动发起(比如用户点击购买按钮)
     * @param productType  交易商品类型
     * @param purchases 交易信息
     * */
   suspend fun handlePurchasesProcess(
        isPay: Boolean,
        productType: BillingProductType,
        purchases: Purchase
    )

    /**
     * 获取google play 后台的支付key
     * @return google play key
     * */
    fun getGooglePayPubId(): String


    /**
     * 相关日志输出
     * 内部不输出日志,暴露给业务端自行处理
     * */
    fun printLog(tag: String, msg: String)


}