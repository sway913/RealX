package com.yy.realx

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cn.jiguang.share.android.api.JShareInterface
import cn.jiguang.share.wechat.Wechat
import com.ycloud.player.widget.MediaPlayerListener
import com.ycloud.svplayer.SvVideoViewInternal
import kotlinx.android.synthetic.main.fragment_share.*
import java.io.File


class ShareFragment : Fragment() {
    companion object {
        private val TAG = ShareFragment::class.java.simpleName
        private val PACKAGES = arrayOf(
            "com.tencent.mm", "com.tencent.mobileqq", "com.sina.weibo", "com.facebook.katana"
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_share, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prepareShareView()
    }

    private val mModel: RealXViewModel by lazy {
        ViewModelProviders.of(activity!!).get(RealXViewModel::class.java)
    }

    private lateinit var mViewInternal: SvVideoViewInternal

    @SuppressLint("ShowToast")
    private fun prepareShareView() {
        Log.d(TAG, "prepareShareView()")
        if (share_video.videoViewInternal !is SvVideoViewInternal) {
            throw IllegalArgumentException("Only support SvVideoViewInternal, please check.")
        }
        mViewInternal = share_video.videoViewInternal as SvVideoViewInternal
        //设定播放器
        mViewInternal.setLayoutMode(SvVideoViewInternal.LAYOUT_SCALE_FIT)
        mViewInternal.setMediaPlayerListener {
            Log.d(TAG, "setMediaPlayerListener():${it.what}")
            when (it.what) {
                MediaPlayerListener.MSG_PLAY_PREPARED -> {
                    //todo: 准备完成，开始播放
                }
                MediaPlayerListener.MSG_PLAY_COMPLETED -> {
                    mViewInternal.start()
                }
                MediaPlayerListener.MSG_PLAY_SEEK_COMPLETED -> {
                    //todo: seek完成，准备播放
                }
            }
        }
        //设定数据
        val video = mModel.video.value
        checkNotNull(video)
        val path = video.export
        Log.d(TAG, "VideoPath():$path")
        mViewInternal.setVideoPath(path)
        //view事件响应
        share_done.setOnClickListener {
            activity!!.onBackPressed()
        }
        val pkg = context!!.packageName
        val uri = FileProvider.getUriForFile(context!!, "${pkg}.fileprovider", File(path))
        Log.d(TAG, "ShareUri():$uri")
        share_wechat.setOnClickListener {
            if (!JShareInterface.isClientValid(Wechat.Name)) {
                return@setOnClickListener
            }
            if (!shareBy("com.tencent.mm", uri)) {
                Toast.makeText(context, "请先安装腾讯微信", Toast.LENGTH_SHORT).show()
            }
        }
        share_qq.setOnClickListener {
            if (!JShareInterface.isClientValid(Wechat.Name)) {
                return@setOnClickListener
            }
            if (!shareBy("com.tencent.mobileqq", uri)) {
                Toast.makeText(context, "请先安装手机qq", Toast.LENGTH_SHORT).show()
            }
        }
        share_weibo.setOnClickListener {
            if (!JShareInterface.isClientValid(Wechat.Name)) {
                return@setOnClickListener
            }
            if (!shareBy("com.sina.weibo", uri)) {
                Toast.makeText(context, "请先安装新浪微博", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 通过系统分享
     */
    private fun shareBy(pkg: String, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
        intent.setPackage(pkg)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.setDataAndType(uri, "video/*")
        val resolvers = context?.packageManager?.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        if (null == resolvers || resolvers.isEmpty()) {
            return false
        }
        val filters = resolvers.filter {
            Log.d(TAG, "filter():${it.activityInfo.packageName}")
            PACKAGES.contains(it.activityInfo.packageName) && (it.activityInfo.name != null)
        }
        if (filters.isEmpty()) {
            return false
        }
        Log.d(TAG, "${filters[0].activityInfo.name}@${filters[0].activityInfo.packageName}")
        if (filters.size > 1) {
            ShareDialogFragment.shareBy(childFragmentManager, filters, object : IShareSelectListener {
                override fun onShare(info: ResolveInfo) {
                    intent.setClassName(info.activityInfo.packageName, info.activityInfo.name)
                    startActivity(intent)
                }
            })
        } else {
            intent.setClassName(filters[0].activityInfo.packageName, filters[0].activityInfo.name)
            startActivity(intent)
        }
        return true
    }

    /**
     * 开始播放
     */
    private fun start() {
        Log.d(TAG, "start()")
        mViewInternal.start()
    }

    /**
     * 暂停播放
     */
    private fun pause() {
        Log.d(TAG, "pause()")
        mViewInternal.pause()
    }

    /**
     * 释放资源
     */
    private fun release() {
        Log.d(TAG, "release()")
        pause()
        mViewInternal.stopPlayback()
    }

    //----------------------生命周期------------------------//

    override fun onResume() {
        super.onResume()
        start()
    }

    override fun onPause() {
        pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }
}