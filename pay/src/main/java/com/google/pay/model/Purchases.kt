package com.google.pay.model

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2025/7/30
 * </pre>
 */


/**
 * 获取对应交易中的用户唯一标识信息
 * 一般是业务订单 和 交易商品类型
 * @param productType 商品类型
 * @param changeNo 业务订单号
 * */
data class PurchaseProfileInfo(var productType: BillingProductType, var changeNo: String)
