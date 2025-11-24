package com.mt.libpay.model

import com.android.billingclient.api.Purchase

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
internal data class PurchaseData(var isPay: Boolean = false, var purchases: List<Purchase> = emptyList())
