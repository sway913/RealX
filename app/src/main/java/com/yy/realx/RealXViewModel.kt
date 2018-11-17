package com.yy.realx

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger

class RealXViewModel : ViewModel() {
    /**
     * 视频配置
     */
    var video = MutableLiveData<VideoSettings>()

    /**
     * 特效数据
     */
    var effect = MutableLiveData<EffectSettings>()

    /**
     * 流程配置
     */
    var stage = MutableLiveData<Stage>()

    /**
     * 状态切换
     */
    fun transitTo(value: Stage) {
        this.stage.value = value
    }
}

/**
 * 视频数据
 */
data class VideoSettings(val path: String) {
    companion object {
        const val EXT = ".mp4"
    }

    private val generator = AtomicInteger(0)
    val segments = mutableListOf<VideoSegment>()
    val duration: Long
        get() {
            var total: Long = 0
            segments.forEach {
                total += it.duration
            }
            return total
        }

    //编辑区域数据
    val audio: AudioSettings
        get() {
            return AudioSettings(path.replace(EXT, AudioSettings.EXT), 0)
        }
    val export: String = path.replace(EXT, "_export$EXT")

    /**
     * 获取segment
     * index小于0的时候，返回新的segment
     */
    fun segmentAt(index: Int): VideoSegment {
        if (index < 0) {
            val id = generator.getAndIncrement()
            segments.add(VideoSegment(id, path.replace(EXT, "_P$id$EXT")))
        } else if (index >= segments.size) {
            throw NoSuchElementException("Index is out of bound.($index/${segments.size})")
        }
        return if (index < 0) segments.last() else segments[index]
    }

    /**
     * 返回最后一个segment
     */
    fun segmentLast(): VideoSegment {
        return segments.last()
    }
}

/**
 * 视频分段信息
 */
data class VideoSegment(val index: Int, val path: String) {
    var tuner: String = ""
    var effect: EffectSettings? = null
    var duration = 0
}

/**
 * 音频数据
 */
data class AudioSettings(val path: String, val start: Int = 0) {
    companion object {
        const val EXT = ".wav"
    }

    var tuner: String = path.replace(EXT, "_tuner$EXT")
    var mixer: String = path.replace(EXT, "_mixer$EXT")
    var accompany: String? = ""
}

/**
 * 特效数据
 */
data class EffectSettings(val name: String, val thumb: String, val feature: Int = 0, var isNew: Boolean = true) {
    companion object {
        const val FEATURE_3D = 0
        const val FEATURE_2D = 1
    }

    var avatar: AvatarSettings? = null
}

/**
 * 人脸数据
 */
data class AvatarSettings(val path: String, val values: List<Float> = emptyList(), val auto: Boolean = true) {
    private val bytes = mutableListOf<Byte>()
    /**
     * 换算255
     */
    fun syncBytes(): ByteArray {
        if (bytes.isNotEmpty()) {
            return bytes.toByteArray()
        }
        if (values.isNotEmpty()) {
            values.forEach {
                bytes.add((it * 255).toByte())
            }
        }
        return bytes.toByteArray()
    }
}

/**
 * 流程数据
 */
enum class Stage {
    PERMISSION, RECORD, EDIT, SHARE
}