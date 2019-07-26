package com.ryan.openglvideorecorder.encodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import com.ryan.openglvideorecorder.utils.Logger;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoEncoder {

    private static final int FRAME_RATE = 30; // 帧率
    private static final int IFRAME_INTERVAL = 10; //I帧间隔

    private Surface mInputSurface;
    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;

    private MediaMuxer mMediaMuxer;
    private int mTrackIndex;
    private boolean mMuxerStarted;


    public VideoEncoder(int width, int height, File outputFile) {
        try {
            mBufferInfo = new MediaCodec.BufferInfo();
            // 设置mediacodec format
            int bitRate = height * width * 3 * 8 * FRAME_RATE / 256; // TODO 为什么要除以256?
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,IFRAME_INTERVAL);
            Logger.d( "format: " + format);

            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 得到encodec的input surface
            mInputSurface = mMediaCodec.createInputSurface();
            // 启动encodec
            mMediaCodec.start();

            // 创建封装器
            mMediaMuxer = new MediaMuxer(outputFile.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mTrackIndex = -1;
            mMuxerStarted = false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    public void release() {
        Logger.d("releasing encoder objects");
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public Surface getSurface() {
        if (mInputSurface != null) {
            return mInputSurface;
        }
        return null;
    }

    // 开始录像编码，注意因为我们的数据都直接渲染到surface上，所以这里可以直接通过outputbuffer读取数据，并写入mux
    // 注意，这里每一帧绘制都要调用一下
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (endOfStream) {
            // 停止录像
            Logger.d("停止录像 endOfStream");
            mMediaCodec.signalEndOfInputStream();
        }

        while(true) {
            // 获取encodec状态
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Logger.d("drainEncoder encoderStatus="+encoderStatus);

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Logger.e("dequeueOutputBuffer timed out!");
                // no output available yet
                if (!endOfStream) {
                    break;
                } else {
                    Logger.d("no output available, spinning to await EOS");
                }
            }
            // 编码格式变化, 什么时候调用????????
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Logger.d("drainEncoder New format: " + mMediaCodec.getOutputFormat());
                if (mMuxerStarted) { // 还没开始封装
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                Logger.d("encoder output format changed: " + newFormat);

                mTrackIndex = mMediaMuxer.addTrack(newFormat);
                mMediaMuxer.start();
                mMuxerStarted = true;
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Logger.e("dequeueOutputBuffer INFO_OUTPUT_BUFFERS_CHANGED");
            }
            // 出错了？
            else if (encoderStatus < 0) {
                Logger.e("drainEncoder unexpected result from encoder.dequeueOutputBuffer:"+encoderStatus);
            }
            // 等到解码数据
            else {
                Logger.d("drainEncoder encoderStatus="+encoderStatus);

                ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Logger.d( "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    mMediaMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo); // 写入编码后的数据，TODO 这里不需要设置PTS, DTS？？
                    Logger.d("sent " + mBufferInfo.size + " bytes to muxer, ts=" + mBufferInfo.presentationTimeUs);
                } else {
                    Logger.d( "drainEncoder mBufferInfo: " + mBufferInfo.size);
                }
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Logger.d( "reached end of stream unexpectedly");
                    } else {
                        Logger.d("end of stream reached");
                    }
                    break;
                }
            }
        }
    }

}
