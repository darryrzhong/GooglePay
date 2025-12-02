package com.gp.pay.ui.inapp

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.gp.pay.R

/**
 * <pre>
 *     类描述  :
 *
 *     @author : never
 *     @since  : 2025/12/2
 * </pre>
 */
class InAppAdapter : BaseQuickAdapter<String, QuickViewHolder>() {
    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): QuickViewHolder {
        return QuickViewHolder(R.layout.layout_inapp_item, parent)
    }

    override fun onBindViewHolder(
        holder: QuickViewHolder,
        position: Int,
        item: String?
    ) {
        holder.getView<TextView>(R.id.tv_product).text = "product_$position"
    }
}