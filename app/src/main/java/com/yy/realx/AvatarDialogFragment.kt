package com.yy.realx

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.media.ExifInterface
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.*
import com.ycloud.facedetection.STMobileFaceDetectionWrapper
import com.ycloud.utils.FileUtils
import kotlinx.android.synthetic.main.fragment_avatar.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule

class AvatarDialogFragment : DialogFragment() {
    companion object {
        private val TAG = AvatarDialogFragment::class.java.simpleName
        private const val KEY_PATH = "avatar_path"
        private val IndexCursor = arrayOf(
            1, 5, 10, 16, 22, 27, 31,           //脸轮廓
            52, 55, 58, 61, 72, 73, 75, 76,     //眼睛轮廓
            82, 83,                             //鼻子轮廓，49？
            84, 87, 90, 93                      //嘴巴轮廓
        )

        private val EMPTY_VALUES = arrayOf(
            0.33004844F,
            0.4342399F,
            0.3376519F,
            0.4953576F,
            0.37742564F,
            0.5651504F,
            0.4846746F,
            0.60274976F,
            0.5905083F,
            0.56271243F,
            0.6291168F,
            0.49268258F,
            0.6356264F,
            0.43156135F,
            0.38611716F,
            0.4112973F,
            0.44249836F,
            0.4132291F,
            0.52420384F,
            0.41183403F,
            0.58109665F,
            0.40824988F,
            0.41532722F,
            0.40323994F,
            0.41392505F,
            0.41701722F,
            0.55120397F,
            0.40094772F,
            0.55345684F,
            0.41508043F,
            0.43709454F,
            0.4724888F,
            0.52951866F,
            0.47141045F,
            0.42800882F,
            0.5253006F,
            0.4834269F,
            0.50762373F,
            0.5392555F,
            0.5235423F,
            0.48408976F,
            0.539486F
        )

        /**
         * static函数
         */
        fun newInstance(path: String): AvatarDialogFragment {
            val fragment = AvatarDialogFragment()
            val bundle = Bundle()
            bundle.putString(KEY_PATH, path)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_avatar, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        super.onActivityCreated(savedInstanceState)
        dialog?.apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener { dialog, keyCode, event -> true }
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        }
        //准备检测人脸数据
        if (!prepareAvatarView()) {
            dismiss()
        }
    }

    private val mDetection by lazy {
        STMobileFaceDetectionWrapper.getPictureInstance(context!!).apply {
            setFaceLimit(1)
            isCheckFace = true
        }
    }

    private val mTimer by lazy {
        Timer("Avatar_Timer", false)
    }

    private var isDetecting = AtomicBoolean(false)

    /**
     * 可行么？
     */
    private fun prepareAvatarView(): Boolean {
        Log.d(TAG, "prepareAvatarView()")
        val bundle = arguments ?: return false
        val path = bundle.getString(KEY_PATH) ?: return false
        avatar_image.setImageURI(Uri.fromFile(File(path)))
        avatar_done.setOnClickListener {
            if (isDetecting.get()) {
                return@setOnClickListener
            }
            val item = mModel.effect.value
            if (null != item) {
                val effect = EffectSettings(item.name, item.thumb, item.feature, item.isNew)
                val avatar = mModel.effect.value?.avatar
                if (null != avatar) {
                    val values = avatar_image.getValues()
                    effect.avatar = AvatarSettings(avatar.path, values, avatar.auto)
                }
                mModel.effect.value = effect
            }
            dismiss()
        }
        avatar_waiting.visibility = View.VISIBLE
        avatar_message.text = String.format(Locale.getDefault(), "检测中...")
        if (!isDetecting.get()) {
            isDetecting.set(true)
            mTimer.schedule(0) {
                doDetectionOn(path)
            }
        }
        avatar_done.isEnabled = false
        mModel.effect.observe(this, Observer {
            Log.d(TAG, "avatar.observe():${isDetecting.get()}")
            if (isDetecting.get()) {
                return@Observer
            }
            avatar_waiting.visibility = View.GONE
            avatar_image.setImageURI(Uri.fromFile(File(path)))
            avatar_done.isEnabled = true
        })
        return true
    }

    private val mModel: RealXViewModel by lazy {
        ViewModelProviders.of(activity!!).get(RealXViewModel::class.java)
    }

    /**
     * 图片检测人脸
     */
    private fun doDetectionOn(path: String) {
        Log.d(TAG, "doDetectionOn():$path")
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = resizeSampleRatio(options, 720, 960)
        options.inJustDecodeBounds = false
        var bitmap = BitmapFactory.decodeFile(path, options)
        bitmap = restrictPortrait(path, bitmap) {
            Log.d(TAG, "restrictPortrait():$path")
            replaceResourceFile(path, it)
        }
        checkNotNull(bitmap)
        val width = bitmap.width
        val height = bitmap.height
        Log.d(TAG, "doDetectionOn():$width, $height")
        val byteBuffer = ByteBuffer.allocate(width * height * 4)
        checkNotNull(byteBuffer)
        byteBuffer.clear()
        bitmap.copyPixelsToBuffer(byteBuffer)
        bitmap.recycle()
        var tryCount = 3
        var point: STMobileFaceDetectionWrapper.FacePointInfo?
        do {
            Log.d(TAG, "doDetectionOn():$tryCount")
            mDetection.onVideoFrameEx(byteBuffer.array(), width, height, true, true)
            point = mDetection.currentFacePointInfo
            tryCount--
        } while (tryCount > 0 && (point?.mFaceCount == null || point.mFaceCount <= 0))
        byteBuffer.clear()
        //输出数据
        val count = point?.mFaceCount ?: 0
        Log.d(TAG, "doDetectionOn():$tryCount, $count")
        val values = mutableListOf<Float>()
        if (count > 0 && null != point?.mFacePoints && point.mFacePoints.isNotEmpty()) {
            point.mFacePoints[0].mapIndexed { index, value ->
                if (IndexCursor.contains(index / 2)) {
                    Log.d(TAG, "Point@($index):$value")
                    values.add(value)
                }
            }
        }
        mDetection.releaseFacePointInfo(point)
        if (values.isEmpty()) {
            values.addAll(EMPTY_VALUES)
        }
        activity!!.runOnUiThread {
            isDetecting.set(false)
            val effect = EffectSettings("face2danim", "target.png", EffectSettings.FEATURE_2D)
            effect.avatar = AvatarSettings(path, values)
            mModel.effect.value = effect
            //刷新ui
            avatar_image.setValues(values)
            if (Build.BRAND.toLowerCase().contains("xiaomi")) {
                avatar_image.setImageBitmap(BitmapFactory.decodeFile(path))
            } else {
                avatar_image.setImageURI(Uri.fromFile(File(path)))
            }
        }
    }

    /**
     * 计算比例缩小
     */
    private fun resizeSampleRatio(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 如果方向不对，矫正
     */
    private fun restrictPortrait(path: String, bitmap: Bitmap, requireSaveBitmap: (bitmap: Bitmap) -> Unit): Bitmap {
        Log.d(TAG, "restrictPortrait():$bitmap")
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        var degree = 0
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                degree = 90
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                degree = 180
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                degree = 270
            }
        }
        Log.d(TAG, "restrictPortrait():$degree")
        if (degree <= 0) {
            requireSaveBitmap(bitmap)
            return bitmap
        }
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val _bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true) ?: return bitmap
        if (bitmap != _bitmap) {
            bitmap.recycle()
        }
        requireSaveBitmap(_bitmap)
        Log.d(TAG, "restrictPortrait():$_bitmap")
        return _bitmap
    }

    /**
     * 把图片保存起来
     */
    private fun replaceResourceFile(path: String, bitmap: Bitmap) {
        FileUtils.deleteFileSafely(File(path))
        //save bitmap
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(File(path)))
    }
}