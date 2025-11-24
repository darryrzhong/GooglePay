package com.mt.libpay

import android.app.Dialog
import android.view.View
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mt.libpay.billing.AppBillingClient
import com.mt.libpay.model.BillingPayEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

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
 * 方便异常处理和维护flow的订阅正常
 * 只是简化了每次需要手动xx.Launch{}
 *
 * uiStateFlow.handleEach({ uiState ->
 *     updateUi(uiState)
 * }, catch = {
 *     Logger.d(it)
 * }).launchIn(lifecycleScope)
 *
 * */

fun <T> Flow<T>.handleTryEach(
    action: suspend (T) -> Unit,
    catch: suspend (cause: Throwable) -> Unit = { cause -> throw cause }
): Flow<T> = flow {
    collect { value ->
        try {
            action.invoke(value)
        } catch (e: Exception) {
            catch.invoke(e)
        }
    }
}

/**
 * 监听支付事件,自动取消监听
 * @param onEvent 事件
 * */
fun ComponentActivity.observePayEvent(onEvent: (BillingPayEvent) -> Unit) {
    AppBillingClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
        onEvent.invoke(event)
    }, catch = {
        if (AppBillingClient.getInstance().deBug) {
            AppBillingClient.getInstance().appBillingService.printLog(
                AppBillingClient.TAG,
                "observePayEvent catch: ${it.message}"
            )
        }
    }).launchIn(this.lifecycleScope)
}

/**
 * 监听支付事件,自动取消监听
 * @param onEvent 事件
 * */
fun Fragment.observePayEvent(onEvent: (BillingPayEvent) -> Unit) {
    AppBillingClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
        onEvent.invoke(event)
    }, catch = {
        if (AppBillingClient.getInstance().deBug) {
            AppBillingClient.getInstance().appBillingService.printLog(
                AppBillingClient.TAG,
                "observePayEvent catch: ${it.message}"
            )
        }
    }).launchIn(this.lifecycleScope)
}


/**
 * 监听支付事件,自动取消监听
 * @param onEvent 事件
 * */
fun Fragment.observePayViewEvent(onEvent: (BillingPayEvent) -> Unit) {
    AppBillingClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
        onEvent.invoke(event)
    }, catch = {
        if (AppBillingClient.getInstance().deBug) {
            AppBillingClient.getInstance().appBillingService.printLog(
                AppBillingClient.TAG,
                "observePayEvent catch: ${it.message}"
            )
        }
    }).launchIn(this.viewLifecycleOwner.lifecycleScope)
}


/**
 * 监听支付事件,自动取消监听
 * @param onEvent 事件
 * */
fun Dialog.observePayEvent(onEvent: (BillingPayEvent) -> Unit) {
    val job =
        AppBillingClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
            onEvent.invoke(event)
        }, catch = {
            if (AppBillingClient.getInstance().deBug) {
                AppBillingClient.getInstance().appBillingService.printLog(
                    AppBillingClient.TAG,
                    "observePayEvent catch: ${it.message}"
                )
            }
        }).launchIn(AppBillingClient.getInstance().billingMainScope)
    this.setOnDismissListener {
        job.cancel()
    }
}


/**
 * 监听支付事件
 * 注意::: 需要手动取消监听
 * @param onEvent 事件
 * */
fun Any.observePayEvent(onEvent: (BillingPayEvent) -> Unit): Job {
    val job =
        AppBillingClient.getInstance().appBillingPayEventFlow.handleTryEach(action = { event ->
            onEvent.invoke(event)
        }, catch = {
            if (AppBillingClient.getInstance().deBug) {
                AppBillingClient.getInstance().appBillingService.printLog(
                    AppBillingClient.TAG,
                    "observePayEvent catch: ${it.message}"
                )
            }
        }).launchIn(AppBillingClient.getInstance().billingMainScope)
    return job
}
