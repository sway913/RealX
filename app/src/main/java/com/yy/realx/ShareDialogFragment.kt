package com.yy.realx

import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.fragment_share_item.view.*
import kotlinx.android.synthetic.main.fragment_share_panel.*

class ShareDialogFragment : DialogFragment() {
    companion object {
        private val TAG = ShareDialogFragment::class.java.simpleName
        /**
         * 展示对话框
         */
        fun shareBy(manager: FragmentManager, list: List<ResolveInfo>, listener: IShareSelectListener) {
            val fragment = ShareDialogFragment()
            fragment.setShareListener(listener)
            fragment.setResolveList(list)
            fragment.showNow(manager, TAG)
        }
    }

    private var listener: IShareSelectListener? = null

    /**
     * 设置监听器
     */
    fun setShareListener(listener: IShareSelectListener) {
        this.listener = listener
    }

    private val list = mutableListOf<ResolveInfo>()

    /**
     * 设置分享项目
     */
    fun setResolveList(list: List<ResolveInfo>) {
        this.list.clear()
        this.list.addAll(list)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_share_panel, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated()")
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        super.onActivityCreated(savedInstanceState)
        dialog?.apply {
            setCanceledOnTouchOutside(true)
            setOnKeyListener { dialog, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    this@ShareDialogFragment.dismiss()
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.parseColor("#8c000000")))
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM)
            }
        }
        if (!prepareSharePanelView()) {
            dismiss()
        }
    }

    private fun prepareSharePanelView(): Boolean {
        if (list.isNullOrEmpty()) {
            return false
        }
        share_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val adapter = ShareAdapter(list)
        adapter.setOnItemClickListener {
            this.listener?.onShare(it)
            dismiss()
        }
        share_list.adapter = adapter
        return true
    }

    class ShareViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class ShareAdapter(val list: List<ResolveInfo>) : RecyclerView.Adapter<ShareViewHolder>() {

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ShareViewHolder {
            val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.fragment_share_item, viewGroup, false)
            return ShareViewHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
            val info = list[position]
            val ctx = holder.itemView.context
            holder.itemView.share_thumb.setImageDrawable(info.loadIcon(ctx.packageManager))
            holder.itemView.share_name.text = info.loadLabel(ctx.packageManager)
            holder.itemView.setOnClickListener {
                listener?.invoke(info)
            }
        }

        private var listener: ((ResolveInfo) -> Unit)? = null

        /**
         * 增加item点击监听器
         */
        fun setOnItemClickListener(listener: (ResolveInfo) -> Unit) {
            this.listener = listener
        }
    }
}

interface IShareSelectListener {
    fun onShare(info: ResolveInfo)
}