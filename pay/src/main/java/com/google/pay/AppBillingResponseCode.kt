package com.google.pay

import com.google.pay.AppBillingResponseCode.SERVICE_UNAVAILABLE


/**
 * <pre>
 *     类描述  :  app请求
 *
 *
 *     @author : never
 *     @since   : 2023/8/15
 * </pre>
 */
object AppBillingResponseCode {
    /**
     * 调用成功。
     */
    const val OK = 0

    /**
     * 业务错误
     * */
    const val FAIL = -200

    /**
     * 请求超时。已废弃，不再推荐使用。建议使用 [SERVICE_UNAVAILABLE]。
     */
    @Deprecated("Use SERVICE_UNAVAILABLE instead.")
    const val SERVICE_TIMEOUT = -3

    /**
     * 当前设备或 Google Play 不支持某功能（如订阅、商品详情等）。
     */
    const val FEATURE_NOT_SUPPORTED = -2

    /**
     * 与 Google Play Billing 服务断开连接。可尝试重新连接。
     */
    const val SERVICE_DISCONNECTED = -1


    /**
     * 用户在结算流程中主动取消。
     */
    const val USER_CANCELED = 1

    /**
     * 网络问题或 Google Play 服务不可用（如正在更新、离线等）。
     */
    const val SERVICE_UNAVAILABLE = 2

    /**
     * 当前设备上不支持 Google Play 结算。
     */
    const val BILLING_UNAVAILABLE = 3

    /**
     * 商品在当前区域或账户不可用（例如未发布或已下架）。
     */
    const val ITEM_UNAVAILABLE = 4

    /**
     * 开发者调用错误（如参数错误、API 调用顺序错误）。
     */
    const val DEVELOPER_ERROR = 5

    /**
     * 一般错误（系统内部异常、未知问题等）。
     */
    const val ERROR = 6

    /**
     * 商品已拥有（如未消耗的消耗型商品或已购买的非消耗型商品）。
     */
    const val ITEM_ALREADY_OWNED = 7

    /**
     * 操作失败，因为商品未拥有（尝试消耗或确认未拥有的商品）。
     */
    const val ITEM_NOT_OWNED = 8

    /**
     * 网络错误（新引入，用于明确网络失败场景）。
     */
    const val NETWORK_ERROR = 12


}


