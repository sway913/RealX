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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yy.realx.objectbox.CEffectItem
import io.objectbox.Box
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

    private val box: Box<CEffectItem> by lazy {
        (activity!! as ContainerActivity).boxFor(CEffectItem::class.java)
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
        val extras = mutableListOf<EffectItem>()
        Log.d(TAG, "prepareEffectView():${box.all.size}")
        box.all.forEach { item ->
            val avatar = AvatarItem(item.path, item.values)
            extras.add(EffectItem("face2danim", item.name, item.path, item.type, avatar))
        }
        val adapter = EffectAdapter(extras.toTypedArray())
        adapter.setOnItemClickListener {
            Log.d(TAG, "effect3d.OnClick()")
            val effect = EffectSettings(it.name, it.thumb, it.type)
            if (null != it.avatar) {
                val path = it.avatar.path
                val type = object : TypeToken<List<Float>>() {}.type
                val values = Gson().fromJson<List<Float>>(it.avatar.values, type)
                effect.avatar = AvatarSettings(path, values, false)
            }
            mModel.effect.value = effect
            dismiss()
        }
        effect_gallery.adapter = adapter
        return true
    }

    data class EffectItem(
        val name: String,
        val alias: String,
        val thumb: String,
        val type: Int = EffectSettings.FEATURE_3D,
        val avatar: AvatarItem? = null
    )

    data class AvatarItem(val path: String, val values: String)

    class EffectViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    class EffectAdapter(extras: Array<EffectItem> = emptyArray()) : RecyclerView.Adapter<EffectViewHolder>() {
        private val list: Array<EffectItem> = arrayOf(
            EffectItem("", "无特效", ""),
            EffectItem("avatar_yy_bear", "YY熊", "thumb.png"),
            EffectItem("avatar_monkey", "YY猴", "thumb.png"),
            EffectItem("avatar_aij", "YY酱", "thumb.png")
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

        private var listener: ((EffectItem) -> Unit)? = null

        /**
         * 设置item点击
         */
        fun setOnItemClickListener(function: (item: EffectItem) -> Unit) {
            this.listener = function
        }
    }
}
