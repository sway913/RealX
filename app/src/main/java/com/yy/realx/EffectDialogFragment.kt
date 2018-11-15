package com.yy.realx

import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.fragment_effect.*
import kotlinx.android.synthetic.main.fragment_effect_item.view.*
import java.io.File

class EffectDialogFragment : DialogFragment() {
    companion object {
        private val TAG = EffectDialogFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")
        return inflater.inflate(R.layout.fragment_effect, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated()")
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        super.onActivityCreated(savedInstanceState)
        dialog?.apply {
            setCanceledOnTouchOutside(false)
            setOnKeyListener { dialog, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    this@EffectDialogFragment.dismiss()
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
        if (!prepareEffectView()) {
            dismiss()
        }
    }

    private val mModel: RealXViewModel by lazy {
        ViewModelProviders.of(activity!!).get(RealXViewModel::class.java)
    }

    /**
     * 初始化数据
     */
    private fun prepareEffectView(): Boolean {
        Log.d(TAG, "prepareEffectView()")
        effect_2d_add.setOnClickListener {
            Log.d(TAG, "effect2d.OnClick()")
            val effect = EffectSettings("face2danim", "target.png", EffectSettings.FEATURE_2D)
            mModel.effect.value = effect
            dismiss()
        }
        effect_gallery.layoutManager = GridLayoutManager(context, 3)
        val adapter = EffectAdapter()
        adapter.setOnItemClickListener {
            Log.d(TAG, "effect3d.OnClick()")
            val effect = EffectSettings(it.name, it.thumb, EffectSettings.FEATURE_3D)
            mModel.effect.value = effect
            dismiss()
        }
        effect_gallery.adapter = adapter
        return true
    }

    override fun dismiss() {
        Log.d(TAG, "dismiss()")
        super.dismiss()
    }

    data class EffectItem(val name: String, val alias: String, val thumb: String)

    class EffectViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    class EffectAdapter : RecyclerView.Adapter<EffectViewHolder>() {
        private val list: Array<EffectItem> = arrayOf(
            EffectItem("", "无特效", ""),
            EffectItem("avatar_yy_bear", "YY熊", "")
        )

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EffectViewHolder {
            Log.d(TAG, "onCreateViewHolder():$viewType")
            val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.fragment_effect_item, viewGroup, false)
            return EffectViewHolder(view)
        }

        override fun getItemCount(): Int {
            Log.d(TAG, "getItemCount():${list.size}")
            return list.size
        }

        override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
            Log.d(TAG, "onBindViewHolder():$position")
            val item = list[position]
            if (item.thumb.isBlank()) {
                holder.view.effect_thumb.setImageResource(R.mipmap.ic_menu_portrait)
            } else {
                holder.view.effect_thumb.setImageURI(Uri.fromFile(File(item.thumb)))
            }
            if (item.name.isBlank()) {
                holder.view.effect_thumb.setImageResource(R.drawable.clear_cancel_background)
            }
            holder.view.effect_name.text = item.alias
            //
            holder.view.setOnClickListener {
                this.listener?.invoke(item)
            }
        }

        var listener: ((EffectItem) -> Unit)? = null

        /**
         * 设置item点击
         */
        fun setOnItemClickListener(function: (item: EffectItem) -> Unit) {
            this.listener = function
        }
    }
}
