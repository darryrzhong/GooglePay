package com.gp.pay.ui.inapp

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.pay.AppBillingResponseCode
import com.google.pay.billing.GooglePayClient
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.model.BillingParams
import com.google.pay.model.BillingPayEvent
import com.google.pay.observePayEvent
import com.gp.pay.databinding.FragmentInappBinding
import com.gp.pay.dp
import com.gp.pay.showTips

class InAppFragment : Fragment() {

    private var _binding: FragmentInappBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val inappViewMode by lazy { ViewModelProvider(this).get(InappViewModel::class.java) }
    private val adapter = InAppAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentInappBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initProducts()
        return root
    }

    private fun initProducts() {

        binding.rvInapp.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvInapp.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.top = 12f.dp
                outRect.left = 12f.dp
                outRect.right = 12f.dp
                outRect.bottom = 12f.dp
            }
        });
        binding.rvInapp.adapter = adapter
        adapter.setOnItemClickListener { adapter, view, position ->
            val productId = adapter.getItem(position)
            //1. 先去业务服务端生产一个业务充值订单,后续用这个订单和gp订单绑定
            val changeNo = "22553355"
            val billingParams =
                BillingParams.Builder()
                    .setAccountId("202511214")
                    .setProductId(productId)
                    .setChargeNo(changeNo)
                    .build()
            val result = GooglePayClient.getInstance().getPayService<OneTimeService>()
                .launchBillingFlow(requireActivity(), billingParams)
            if (result.code != AppBillingResponseCode.OK) {
                result.message.showTips(requireContext())
            }
        }
        inappViewMode.products.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        observePayEvent { payEvent ->
            when (payEvent) {
                is BillingPayEvent.PayConsumeFailed -> {}
                is BillingPayEvent.PayConsumeSuccessful -> {}
                is BillingPayEvent.PayFailed -> {
                    payEvent.message.showTips(requireContext())
                }

                is BillingPayEvent.PaySuccessful -> {
                    "pay success".showTips(requireContext())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}