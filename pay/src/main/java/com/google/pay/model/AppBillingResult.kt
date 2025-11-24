package com.google.pay.model

import com.google.pay.AppBillingResponseCode

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2025/7/25
 * </pre>
 */
/**
 * @param code Success 1 or fail -1
 * */
data class AppBillingResult(
    var code: Int = AppBillingResponseCode.BILLING_UNAVAILABLE,
    var message: String = "Google Play billing is not supported on the current device"
)