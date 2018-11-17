package com.yy.realx

import android.app.Activity
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.ycloud.api.common.FilterGroupType
import com.ycloud.api.common.FilterType
import com.ycloud.api.common.SDKCommonCfg
import com.ycloud.api.process.IMediaListener
import com.ycloud.api.process.MediaProbe
import com.ycloud.api.process.MediaProcess
import com.ycloud.api.process.VideoConcat
import com.ycloud.api.videorecord.IMediaInfoRequireListener
import com.ycloud.api.videorecord.IVideoRecord
import com.ycloud.api.videorecord.IVideoRecordListener
import com.ycloud.camera.utils.CameraUtils
import com.ycloud.gpuimagefilter.utils.FilterIDManager
import com.ycloud.gpuimagefilter.utils.FilterOPType
import com.ycloud.mediarecord.VideoRecordConstants
import com.ycloud.utils.FileUtils
import com.ycloud.ymrmodel.MediaSampleExtraInfo
import com.yy.media.MediaConfig
import com.yy.media.MediaUtils
import com.yy.realx.objectbox.CEffectItem
import io.objectbox.Box
import kotlinx.android.synthetic.main.fragment_record.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.concurrent.schedule

class RecordFragment : Fragment() {
    companion object {
        private var TAG = RecordFragment::class.java.simpleName
        private val TunerMode = arrayOf(
            "VeoNone",
            "VeoLuBan",
            "VeoLorie",
            "VeoUncle",
            "VeoWarCraft",
            "VeoYoungLady",
            "VeoGirl",
            "VeoManWebCelebrity"
        )
        private val TunerName = arrayOf(
            "原声", "鲁班", "萝莉", "大叔", "魔兽", "御姐", "少女", "渣男"
        )
        private val SpeedMode = arrayOf(
            0.2f, 0.5f, 1.0f, 2.0f, 4.0f
        )
        private const val REQUEST_AVATAR = 0x0f02
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        preparePreview()
    }

    private lateinit var mRecordConfig: MediaConfig
    private lateinit var mVideoRecord: IVideoRecord

    private var isRecording = false
    private var mRecordListener = object : IVideoRecordListener {

        override fun onProgress(seconds: Float) {
            activity!!.runOnUiThread {
                record_ms.text = String.format(Locale.getDefault(), "%.2fs", seconds + total)
            }
        }

        private var total = 0f

        override fun onStart(successed: Boolean) {
            total = (mModel.video.value?.duration ?: 0).toFloat() / 1000
            activity!!.runOnUiThread {
                record_ms.text = String.format(Locale.getDefault(), "%.2fs", total)
                toggle_record.setImageResource(R.drawable.btn_stop_record)
            }
        }

        override fun onStop(successed: Boolean) {
            //更新数据
            val video = mModel.video.value ?: return
            val segment = video.segmentLast()
            mTimer.schedule(0) {
                val info = MediaProbe.getMediaInfo(segment.path, false) ?: return@schedule
                Log.d(TAG, "getMediaInfo():${info.duration}")
                segment.duration = (info.duration * 1000).toInt()
                //分段显示
                val list = mutableListOf<Int>()
                video.segments.forEach {
                    list.add(it.duration)
                }
                segment_bar.setSegments(list)
            }
            //刷新界面
            activity!!.runOnUiThread {
                btn_finish.isEnabled = video.segments.isNotEmpty()
                record_ms.text = ""
                toggle_record.setImageResource(R.drawable.btn_start_record)
            }
        }
    }

    private val mModel: RealXViewModel by lazy {
        ViewModelProviders.of(activity!!).get(RealXViewModel::class.java)
    }

    private val isInitialed = AtomicBoolean(false)
    private val tuner = AtomicInteger(0)

    private var first: Long = 0
    /**
     * 授权成功后回调
     */
    private fun preparePreview() {
        Log.d(TAG, "preparePreview():${lifecycle.currentState}")
        mRecordConfig = MediaConfig.Builder().attach(video_view).build()
        mVideoRecord = MediaUtils.prepare(context, mRecordConfig) {
            Log.d(TAG, "onPreviewStart()")
            isInitialed.set(true)
        }
        video_view.setOnClickListener {
            Log.d(TAG, "VideoView.OnClick():$first")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                return@setOnClickListener
            }
            val second = System.currentTimeMillis()
            if ((second - first) > 500) {
                first = second
            } else {
                switchCamera()
            }
        }
        //事件绑定
        clear_video.setOnClickListener {
            Log.d(TAG, "ClearVideo.OnClick()")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                return@setOnClickListener
            }
            mModel.video.value = null
            mModel.effect.value = null
            btn_finish.isEnabled = false
            speed_mode_2.performClick()
            btn_voice.text = TunerName[0]
            segment_bar.setSegments(emptyList())
            applyEffect(EffectSettings("", "", EffectSettings.FEATURE_3D))
        }
        var frames = 0
        var amplitude = 0
        mVideoRecord.setEnableAudioRecord(true)
        mVideoRecord.setAudioRecordListener { avgAmplitude, maxAmplitude ->
            //            Log.d(TAG, "onVolume():$avgAmplitude, $maxAmplitude")
            synchronized(mModel) {
                frames++
                amplitude += avgAmplitude
            }
        }
        mVideoRecord.setMediaInfoRequireListener(object : IMediaInfoRequireListener {
            override fun onRequireMediaInfo(info: MediaSampleExtraInfo?, pts: Long) {
                Log.d(TAG, "onRequireMediaInfo():$amplitude, $frames, $pts")
            }

            override fun onRequireMediaInfo(info: MediaSampleExtraInfo?) {
                Log.d(TAG, "onRequireMediaInfo():$amplitude, $frames")
                var loudness = 0f
                synchronized(mModel) {
                    if (frames > 0) {
                        loudness = (amplitude / frames).toFloat()
                        frames = 0
                        amplitude = 0
                    }
                }
                info?.rhythmSmoothRatio = loudness
                info?.rhythmFrequencyData = mModel.effect.value?.avatar?.syncBytes() ?: ByteArray(0)
            }
        })
        //录制一段视频
        SDKCommonCfg.disableMemoryMode()
        toggle_record.setOnClickListener {
            Log.d(TAG, "RecordButton.OnClick()")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                mVideoRecord.stopRecord()
            } else {
                var video = mModel.video.value
                if (null == video) {
                    var path = CameraUtils.getOutputMediaFile(CameraUtils.MEDIA_TYPE_VIDEO).absolutePath
                    video = VideoSettings(path)
                    mModel.video.value = video
                }
                checkNotNull(video)
                Log.d(TAG, "startRecord():name = ${video.path}")
                val segment = video.segmentAt(-1)
                checkNotNull(segment)
                segment.tuner = TunerMode[tuner.get() % TunerMode.size]
                segment.effect = mModel.effect.value
                Log.d(TAG, "startRecord():segment = ${segment.path}")
                mVideoRecord.setOutputPath(segment.path)
                mVideoRecord.setRecordListener(mRecordListener)
                mVideoRecord.startRecord(false)
            }
            isRecording = !isRecording
        }
        //
        btn_finish.isEnabled = mModel.video.value?.segments?.isNotEmpty() ?: false
        btn_finish.setOnClickListener {
            Log.d(TAG, "NextStage.OnClick()")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                return@setOnClickListener
            }
            checkNotNull(mModel.video.value)
            concatVideoSegments()
        }
        //选择图片
        btn_avatar.setOnClickListener {
            Log.d(TAG, "AvatarEffect.OnClick()")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                return@setOnClickListener
            }
            val effect = EffectDialogFragment()
            effect.showNow(childFragmentManager, EffectDialogFragment::class.java.simpleName)
            effect.dialog.setOnDismissListener {
                Log.d(TAG, "Effect.dismiss()")
                val effect = mModel.effect.value ?: return@setOnDismissListener
                if (!effect.isNew) {
                    return@setOnDismissListener
                }
                //设置effect已经应用
                effect.isNew = false
                //执行分支代码
                when (effect.feature) {
                    //先去选择图片
                    EffectSettings.FEATURE_2D -> {
                        if (null == effect.avatar) {
                            apply2dAvatar(effect)
                        } else {
                            applyAvatar(effect, effect.avatar!!.path)
                        }
                    }
                    else -> {
                        applyEffect(effect)
                    }
                }
            }
        }
        //速度选择
        val speedModes = arrayOf(speed_mode_0, speed_mode_1, speed_mode_2, speed_mode_3, speed_mode_4)
        val listener = View.OnClickListener {
            Log.d(TAG, "SpeedModes.OnClick():$it")
            if (!isInitialed.get()) {
                return@OnClickListener
            }
            if (isRecording) {
                return@OnClickListener
            }
            if (!it.isSelected) {
                speedModes.forEach { view ->
                    view.isSelected = (view == it)
                }
                val index = speedModes.indexOf(it)
                mVideoRecord.setRecordSpeed(SpeedMode[index])
            }
        }
        speedModes.forEach {
            Log.d(TAG, "SpeedModes.setOnClickListener():$it")
            it.setOnClickListener(listener)
            it.isSelected = false
            it.setOnTouchListener { v, event ->
                Log.d(TAG, "SpeedModes.OnTouch():${event.action}")
                return@setOnTouchListener false
            }
        }
        speed_mode_2.isSelected = true
        //
        btn_voice.text = TunerName[0]
        btn_voice.setOnClickListener {
            Log.d(TAG, "VoiceTuner.OnClick()")
            if (!isInitialed.get()) {
                return@setOnClickListener
            }
            if (isRecording) {
                return@setOnClickListener
            }
            btn_voice.text = TunerName[tuner.incrementAndGet() % TunerName.size]
        }
        //分段显示
        val video = mModel.video.value
        if (null != video) {
            val list = mutableListOf<Int>()
            video.segments.forEach {
                list.add(it.duration)
            }
            segment_bar.setSegments(list)
        }
        /*
        mTimer.schedule(5000) {
            val seq = SequenceSimulate(activity)
                .addSimulate(ClickViewSimulate.obtain(activity, toggle_camera))
                .addSimulate(ClickViewSimulate.obtain(activity, btn_voice))
                .addSimulate(ClickViewSimulate.obtain(activity, btn_avatar))
            seq.simulate()
        }
        */
    }

    /**
     * 切换摄像头
     */
    private fun switchCamera() {
        Log.d(TAG, "SwitchCamera.OnClick()")
        if (!isInitialed.get()) {
            return
        }
        if (isRecording) {
            return
        }
        mVideoRecord.switchCamera()
        if (mRecordConfig.cameraId == VideoRecordConstants.FRONT_CAMERA) {
            mRecordConfig.cameraId = VideoRecordConstants.BACK_CAMERA
        } else {
            mRecordConfig.cameraId = VideoRecordConstants.FRONT_CAMERA
        }
    }

    private var effect = FilterIDManager.NO_ID

    /**
     * 设置effect
     */
    private fun applyEffect(effect: EffectSettings?, call: (() -> Unit)? = null) {
        if (null == effect) {
            return
        }
        val wrapper = mVideoRecord.recordFilterSessionWrapper ?: return
        if (this.effect != FilterIDManager.NO_ID) {
            wrapper.removeFilter(this.effect)
            this.effect = FilterIDManager.NO_ID
        }
        //纯粹清除特效
        if (effect.name.isBlank()) {
            mModel.effect.value = null
            return
        }
        val name = effect.name
        val dir = File(context!!.filesDir, name)
        val avatar = File(dir, "effect0.ofeffect")
        if (!avatar.exists()) {
            extractFromAssets(name)
            //callback
            call?.invoke()
        }
        this.effect = wrapper.addFilter(FilterType.GPUFILTER_EFFECT, FilterGroupType.DEFAULT_FILTER_GROUP)
        val config = hashMapOf<Int, Any>(
            FilterOPType.OP_SET_EFFECT_PATH to avatar.absolutePath
        )
        wrapper.updateFilterConf(this.effect, config)
    }

    /**
     * 解压特效文件
     */
    private fun extractFromAssets(name: String) {
        val dir = File(context!!.filesDir, name)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        var count: Int
        var buffer = ByteArray(4 * 1024)
        val input = ZipInputStream(context!!.assets.open("$name.zip"))
        var entry: ZipEntry? = input.nextEntry
        while (null != entry) {
            if (entry.isDirectory) {
                File(dir, entry.name).mkdirs()
            } else {
                val file = File(dir, entry.name)
                if (file.exists()) {
                    FileUtils.deleteFileSafely(file)
                }
                val output = FileOutputStream(file)
                while (true) {
                    count = input.read(buffer)
                    if (count <= 0) {
                        break
                    } else {
                        output.write(buffer, 0, count)
                    }
                }
                output.flush()
                output.close()
            }
            entry = input.nextEntry
        }
        input.close()
    }

    /**
     * 请求获取图片资源
     */
    private fun apply2dAvatar(effect: EffectSettings?) {
        Log.d(TAG, "apply2dAvatar():${effect?.name}")
        mTimer.schedule(10) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            startActivityForResult(intent, REQUEST_AVATAR)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult():$resultCode")
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_AVATAR -> {
                val uri = data?.data ?: return
                val path = data?.getStringExtra("name")
                Log.d(TAG, "onActivityResult():$uri, $path")
                mTimer.schedule(0) {
                    prepareAvatar(uri)
                }
            }
            else -> {
                Log.d(TAG, "onActivityResult():$requestCode")
            }
        }
    }

    /**
     * 人脸检测
     */
    private fun prepareAvatar(uri: Uri) {
        val projections = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = context!!.contentResolver.query(uri, projections, null, null, null)
        checkNotNull(cursor)
        var path = ""
        if (cursor.moveToFirst()) {
            path = cursor.getString(cursor.getColumnIndexOrThrow(projections[0]))
        }
        cursor.close()
        Log.d(TAG, "prepareAvatar():$path")
        if (path.isBlank()) {
            return
        }
        //拷贝一份文件替换特效
        activity!!.runOnUiThread {
            val avatar = AvatarDialogFragment.newInstance(path)
            avatar.showNow(childFragmentManager, AvatarDialogFragment::class.java.simpleName)
            avatar.dialog.setOnDismissListener {
                Log.d(TAG, "OnDismissListener.onDismiss():$path")
                btn_avatar.setImageURI(Uri.fromFile(File(path)))
                //创建effect
                val effect = mModel.effect.value ?: return@setOnDismissListener
                applyAvatar(effect, path)
                //持久化数据
                if (null != effect.avatar) {
                    val size = box.all.size
                    val json = Gson().toJson(effect.avatar?.values)
                    Log.d(TAG, "applyAvatar():$json")
                    val entity = CEffectItem(path = path, name = "表情$size", values = json)
                    box.put(entity)
                }
            }
        }
    }

    private val box: Box<CEffectItem> by lazy {
        (activity!! as ContainerActivity).boxFor(CEffectItem::class.java)
    }

    /**
     * 添加特效文件
     */
    private fun applyAvatar(effect: EffectSettings?, path: String) {
        Log.d(TAG, "applyAvatar():$path")
        if (null == effect) {
            return
        }
        val dir = File(context!!.filesDir, effect.name)
        FileUtils.copyFile(path, File(dir, "target.png").absolutePath)
        btn_avatar.setImageURI(Uri.fromFile(File(path)))
        //标识初始化
        effect.isNew = false
        applyEffect(effect) {
            FileUtils.copyFile(path, File(dir, "target.png").absolutePath)
        }
    }

    private val mTimer: Timer by lazy {
        Timer("Record_Timer", false)
    }

    /**
     * 先合并视频分段
     */
    private fun concatVideoSegments() {
        Log.d(TAG, "concatVideoSegments()")
        if (!btn_finish.isEnabled) {
            return
        }
        val video = mModel.video.value
        checkNotNull(video)
        val list = mutableListOf<String>()
        video.segments.forEach {
            list.add(it.path)
        }
        val dialog = ProgressDialog.show(context, "", "合并中...", false)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { dialog, keyCode, event -> true }
        val concat = VideoConcat(context, list as ArrayList<String>, video.path)
        concat.setMediaListener(object : IMediaListener {
            override fun onProgress(progress: Float) {
                Log.d(TAG, "VideoConcat.onProgress():$progress")
                dialog.progress = (50 * progress).toInt()
            }

            override fun onError(errType: Int, errMsg: String?) {
                Log.d(TAG, "VideoConcat.onError():$errType, $errMsg")
                concat.cancel()
                concat.release()
                dialog.dismiss()
            }

            override fun onEnd() {
                Log.d(TAG, "VideoConcat.onEnd()")
                concat.cancel()
                concat.release()
                //抽取音轨
                extractAudioFirst(dialog)
            }
        })
        mTimer.schedule(0) {
            concat.execute()
        }
    }

    /**
     * 提取音轨
     */
    private fun extractAudioFirst(dialog: ProgressDialog) {
        Log.d(TAG, "extractAudioFirst()")
        activity!!.runOnUiThread {
            dialog.setMessage("提取中...")
        }
        val video = mModel.video.value
        checkNotNull(video)
        val path = video.path
        val audio = video.audio.path
        val extractor = MediaProcess()
        extractor.setMediaListener(object : IMediaListener {
            override fun onProgress(progress: Float) {
                Log.d(TAG, "AudioExtract.onProgress():$progress")
                dialog.progress = 50 + (50 * progress).toInt()
            }

            override fun onError(errType: Int, errMsg: String?) {
                Log.d(TAG, "AudioExtract.onError():$errType, $errMsg")
                extractor.cancel()
                extractor.release()
                dialog.dismiss()
            }

            override fun onEnd() {
                Log.d(TAG, "AudioExtract.onEnd()")
                extractor.cancel()
                extractor.release()
                dialog.dismiss()
                //跳转
                activity!!.runOnUiThread {
                    mModel.transitTo(Stage.EDIT)
                }
            }
        })
        mTimer.schedule(0) {
            extractor.extractAudioTrack(path, audio)
        }
    }

    //----------------------------------生命周期---------------------------------//

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        mVideoRecord.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        mVideoRecord.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        mVideoRecord.release()
        mTimer.cancel()
    }
}
