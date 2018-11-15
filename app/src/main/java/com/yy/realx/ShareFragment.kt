package com.yy.realx

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ycloud.player.widget.MediaPlayerListener
import com.ycloud.svplayer.SvVideoViewInternal
import kotlinx.android.synthetic.main.fragment_edit.*
import kotlinx.android.synthetic.main.fragment_share.*

class ShareFragment : Fragment() {
    companion object {
        private val TAG = ShareFragment::class.java.simpleName
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