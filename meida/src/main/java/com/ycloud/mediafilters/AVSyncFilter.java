package com.ycloud.mediafilters;

import android.media.MediaCodec;
import android.os.Handler;
import com.ycloud.api.common.SampleType;
import com.ycloud.datamanager.AudioDataManager;
import com.ycloud.datamanager.VideoDataManager;
import com.ycloud.utils.YYLog;
import com.ycloud.ymrmodel.YYMediaSample;

/**
 * Created by Administrator on 2018/3/2.
 */

public class AVSyncFilter extends AbstractYYMediaFilter {
    private static final String TAG = "AVSyncFilter";
    private volatile long mVideoTotalLen;
    private volatile long mAudioTotalLen;
    private boolean mAudioStop;
    private boolean mVideoStop;
    private volatile boolean mRequestStop;
    private volatile long mRequestStopPTS;
    private static int kTIME_OUT_MS = 2000; // 2s
    private static int DIFF_TIME_NANO = 30000; // 30ms
    private Handler mStopProcessHandler;
    private Runnable mStopRunnable;

    @Override
    public boolean processMediaSample(YYMediaSample sample, Object upstream) {
        if (sample.mMediaFormat != null && sample.mDataByteBuffer == null) {  //
            deliverToDownStream(sample);
        } else if ((sample.mBufferFlag & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            deliverToDownStream(sample);
        } else if (sample.mDataByteBuffer != null && sample.mBufferSize >= 0) {
            if (sample.mSampleType == SampleType.VIDEO) {
                processVideoSample(sample);
            } else if (sample.mSampleType == SampleType.AUDIO) {
                processAudioSample(sample);
            }
        }

        return true;
    }

    public void startRecord() {
        mRequestStop = false;
        mAudioStop = false;
        mVideoStop = false;
        mVideoTotalLen = 0;
        mAudioTotalLen = 0;
    }

    public void stopRecord(Runnable stopRunnable, long audioLen, boolean audioStop, float speed) {
        mRequestStopPTS = System.currentTimeMillis();
        mAudioTotalLen = audioLen;
        mAudioStop = audioStop;
        mStopRunnable = stopRunnable;
        mStopProcessHandler = new Handler();
        mRequestStop = true;
        // there may some problem in audio encode or video encode, and their did not push sample to this filter
        if (mVideoTotalLen == 0 || mAudioTotalLen == 0) {
            YYLog.info(TAG, " have not receive sample ,so stop. audioLen %d : videoLen %d ", mAudioTotalLen / 1000, mVideoTotalLen / 1000);
            mStopRunnable.run();
            mRequestStop = false;
        }
        //DIFF_TIME_NANO = (int) (DIFF_TIME_NANO * speed);
    }

    private void processVideoSample(YYMediaSample sample) {
        mVideoTotalLen = VideoDataManager.instance().getDuration();
        if (mRequestStop && !mVideoStop) {
            YYLog.info(TAG, "processVideoSample() audioLen %d : videoLen %d ", mAudioTotalLen / 1000, mVideoTotalLen / 1000);
            if (mVideoTotalLen >= mAudioTotalLen) {
                mVideoStop = true;
            } else {
                if (mAudioStop) {
                    if ((mVideoTotalLen - mAudioTotalLen) > 0) {
                        mVideoStop = true;
                    } else {
                        int diff = (int) (mAudioTotalLen - mVideoTotalLen - 2 * DIFF_TIME_NANO);
                        YYLog.info(TAG, "processVideoSample " + (diff) + " >> "
                                + sample.mAndoridPtsNanos + " >> " + mAudioTotalLen + ":" + mVideoTotalLen);
//                        sample.mAndoridPtsNanos += diff * 1000;
//                        deliverToDownStream(sample);
//                        mVideoTotalLen = VideoDataManager.instance().getDuration();
//                        mVideoStop = true;
                    }
                }
            }
        }
        YYLog.info(TAG, "processVideoSample():%b, %d, %b, %d", mAudioStop, mAudioTotalLen, mVideoStop, mVideoTotalLen);
        if (!mVideoStop) {
            deliverToDownStream(sample);
        }
        checkStopRecordOrTimeOut();
    }

    private void processAudioSample(YYMediaSample sample) {
        mAudioTotalLen = AudioDataManager.instance().getDuration();
        if (mRequestStop && !mAudioStop) {
            YYLog.info(TAG, "processAudioSample() audioLen %d : videoLen %d ", mAudioTotalLen / 1000, mVideoTotalLen / 1000);
            if (mAudioTotalLen > mVideoTotalLen) {
                mAudioStop = true;
            } else {
                if (mVideoStop) {
                    if ((mVideoTotalLen - mAudioTotalLen) <= DIFF_TIME_NANO) {
                        mAudioStop = true;
                    } else {
                        YYLog.info(TAG, "processAudioSample " + (mAudioTotalLen - mVideoTotalLen) + " >> "
                                + sample.mAndoridPtsNanos + " >> " + mAudioTotalLen + ":" + mVideoTotalLen);
                    }
                }
            }
        }
        YYLog.info(TAG, "processAudioSample():%b, %d, %b, %d", mAudioStop, mAudioTotalLen, mVideoStop, mVideoTotalLen);
        if (!mAudioStop) {
            deliverToDownStream(sample);
        }
        checkStopRecordOrTimeOut();
    }

    private void checkStopRecordOrTimeOut() {
        synchronized (this) {
            if (mRequestStop) {
                boolean timeOut = (System.currentTimeMillis() - mRequestStopPTS) > kTIME_OUT_MS;
                if ((mVideoStop && mAudioStop) || timeOut) {
                    mStopProcessHandler.post(mStopRunnable);
                    mRequestStop = false;
                    YYLog.info(TAG, " stop audioLen %d : videoLen %d time out %b", mAudioTotalLen / 1000, mVideoTotalLen / 1000, timeOut);
                }
            }
        }
    }
}
