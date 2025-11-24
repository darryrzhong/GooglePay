package com.mt.libpay.model

import java.io.Serializable

/**
 * <pre>
 *     类描述  : Google 订阅商品详情
 *
 *
 *     @author : never
 *     @since   : 2024/12/5
 * </pre>
 */
data class AppSubscribeDetails(
    var productId: String, //商品id com.xx.product.1
    var productName: String, //商品名称
    var pricingPhases: List<PricingPhase> = mutableListOf() //定位价格阶梯表
) : Serializable

data class PricingPhase(
    var formattedPrice: String, //格式化后的价格 "€7.99"
    var priceAmountMicros: Long, // if price is "€7.99", price_amount_micros is "7990000".
    var priceCurrencyCode: String // currency code is "€".
) : Serializable