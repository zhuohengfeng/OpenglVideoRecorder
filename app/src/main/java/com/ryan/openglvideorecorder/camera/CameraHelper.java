package com.ryan.openglvideorecorder.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.ryan.openglvideorecorder.utils.Logger;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("ALL")
public final class CameraHelper {

    public static CameraHelper mInstance;

    private Camera mCamera;

    private volatile boolean mIsInPreview = false;

    private CameraHelper() {}

    public static CameraHelper getInstance() {
        if (mInstance == null) {
            synchronized (CameraHelper.class) {
                if (mInstance == null) {
                    mInstance = new CameraHelper();
                }
            }
        }
        return mInstance;
    }

    public void openCamera(int cameraId, SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            throw new RuntimeException("纹理surfaceTexture为空");
        }
        // 关闭预览
        stopPreview();

        Logger.d("openCamera");
        //---------------------开启摄像头----------------------
        // 摄像头Info
        Camera.CameraInfo info = new Camera.CameraInfo();
        // 获取摄像头数量
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraId) {
                // 打开摄像头
                mCamera = Camera.open(i);
                // 摄像头Id赋值
                break;
            }
        }
        if (mCamera == null) {
            throw new RuntimeException("无法打开相机, cameraId="+cameraId);
        }

        //-------------------设置对焦---------------------
        //
        final Camera.Parameters params = mCamera.getParameters();
        //
        final List<String> focusModes = params.getSupportedFocusModes();
        // 连续连续对焦
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        // 自动聚焦
        else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        //-------------------------------------------
        // 可以帮助减少启动录制的时间，如果用opengl预览，用egl获取buffer，用mediacodec录制编码视频，这里好像没有用了
        params.setRecordingHint(true);
        //--------------------预览区域大小-----------------------
        // 推荐的预览区域大小
        Camera.Size ppsfv = params.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Logger.d("Camera preferred preview size for video is: " + ppsfv.width + "x" + ppsfv.height);
        }
        // 支持的预览区域大小
        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            Logger.d( "supported: " + size.width + "x" + size.height);
        }
        // 这里采用推荐的预览区域大小
        if (ppsfv != null) {
            params.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        //--------------------设置params-----------------------
        //
        mCamera.setParameters(params);
        //--------------------设置预览的SurfaceTexture-----------------------
        try {
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void startPreview() {
        if (mCamera != null && !mIsInPreview) {
            Logger.d("startPreview");
            mCamera.startPreview();
            mIsInPreview = true;
        }
    }

    public void stopPreview() {
        if (mCamera != null && mIsInPreview) {
            Logger.d("stopPreview");
            mCamera.stopPreview();
            mIsInPreview = false;
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Logger.d("releaseCamera");
            mCamera.stopPreview();
            mIsInPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }


    public int getPreviewWidth() {
        if (mCamera != null) {
            final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            return previewSize.width;
        }
        return 0;
    }

    public int getPreviewHeight() {
        if (mCamera != null) {
            final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            return previewSize.height;
        }
        return 0;
    }
}
