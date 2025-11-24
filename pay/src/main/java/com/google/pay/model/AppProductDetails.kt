package com.mt.libpay.model

import java.io.Serializable

/**
 * <pre>
 *     类描述  : Google 产品详情
 *
 *
 *     @author : never
 *     @since   : 2023/12/4
 * </pre>
 */
data class AppProductDetails(
    var productId :String, //商品id com.xx.product.1
    var productName: String, //商品名称
    var formattedPrice: String, //格式化后的价格 "€7.99"
    var priceAmountMicros: Long, // if price is "€7.99", price_amount_micros is "7990000".
    var priceCurrencyCode: String // currency code is "€".
) : Serializable