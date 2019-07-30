package com.ryan.openglvideorecorder.encodec;

import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.view.Surface;

import com.ryan.openglvideorecorder.camera.CameraHelper;
import com.ryan.openglvideorecorder.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoRecoder {

    private static final int FRAME_RATE = 30; // 帧率
    private static final int IFRAME_INTERVAL = 10; //I帧间隔

    private Surface mInputSurface;

    private boolean mRecordStarted;

    private MediaRecorder mMediaRecorder;

    public VideoRecoder(int width, int height, File outputFile) {
        try {
            // 设置mediacodec format
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(width, height);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setOrientationHint(90);
            mMediaRecorder.prepare();


            mInputSurface = mMediaRecorder.getSurface();
            mRecordStarted = false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startRecord() {
        Logger.d("startRecord");
        // start recording
        mMediaRecorder.start();
    }

    public void stopRecod() {
        Logger.d("stopRecod");
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
            } catch(RuntimeException e) {
                e.printStackTrace();
            } finally {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        }
    }

    public Surface getSurface() {
        if (mInputSurface != null) {
            return mInputSurface;
        }
        return null;
    }


}
