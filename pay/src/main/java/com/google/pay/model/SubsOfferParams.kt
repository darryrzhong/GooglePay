package com.google.pay.model

import java.io.Serializable

/**
 * <pre>
 *     类描述  : 订阅商品详情查询参数
 *
 *
 *     @author : never
 *     @since   : 2023/11/27
 * </pre>
 */
data class SubsOfferParams(
    var productId: String = "",
    var basePlanId: String = "",
    var offerId: String = ""
) : Serializable