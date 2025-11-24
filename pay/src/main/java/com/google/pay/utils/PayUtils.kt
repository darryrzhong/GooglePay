package com.mt.libpay.utils

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.mt.libpay.model.BillingSubsParams
import com.mt.libpay.model.SubsOfferParams

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2023/8/18
 * </pre>
 */
internal object PayUtils {

    fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(purchase.originalJson, purchase.signature)
    }

    /**
     * 获取订阅商品中的购买token
     *@return  offerToken 购买具体订阅方案的唯一标识token
     * */
    @JvmStatic
    fun getSubsOfferToken(
        productDetails: ProductDetails, subsOfferParams: SubsOfferParams
    ): String {
        var offerToken = ""
        var subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        subscriptionOfferDetails?.takeIf { it.isNotEmpty() }?.let { offerDetails ->
            for (offer in offerDetails) {
                //先找到对应的basePlanId
                if (offer.basePlanId == subsOfferParams.basePlanId) {
                    //如果是购买优惠方案
                    if (subsOfferParams.offerId.isNotEmpty() && subsOfferParams.offerId == offer.offerId) {
                        offerToken = offer.offerToken
                        break
                    }
                    //购买基础方案
                    offerToken = offer.offerToken
                }
            }
        }


        return offerToken

    }

    /**
     * 获取订阅商品中的购买详情
     *@return  offerToken 购买具体订阅方案的商品详情信息
     * */
    @JvmStatic
    fun getSubsOfferDetails(
        productDetails: ProductDetails, subsOfferParams: SubsOfferParams
    ): ProductDetails.SubscriptionOfferDetails? {
        var subsOfferDetails: ProductDetails.SubscriptionOfferDetails? = null
        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        subscriptionOfferDetails?.takeIf { it.isNotEmpty() }?.let { offerDetails ->
            for (offer in offerDetails) {
                //先找到对应的basePlanId
                if (offer.basePlanId == subsOfferParams.basePlanId) {
                    //如果是购买优惠方案
                    if (subsOfferParams.offerId.isNotEmpty() && subsOfferParams.offerId == offer.offerId) {
                        subsOfferDetails = offer
                        break
                    }
                    //购买基础方案
                    subsOfferDetails = offer
                }
            }
        }


        return subsOfferDetails

    }

}