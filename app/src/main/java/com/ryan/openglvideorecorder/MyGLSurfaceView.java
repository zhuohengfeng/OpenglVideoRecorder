package com.ryan.openglvideorecorder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.ryan.openglvideorecorder.camera.CameraHelper;
import com.ryan.openglvideorecorder.gl_drawer.GLTextureDrawer;
import com.ryan.openglvideorecorder.gl_drawer.WaterMarkDrawer;
import com.ryan.openglvideorecorder.gl_utils.GLMatrixState;
import com.ryan.openglvideorecorder.gl_utils.GLTextureUtil;
import com.ryan.openglvideorecorder.utils.Logger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 用来显示相机预览，并添加水印
 */
public final class MyGLSurfaceView extends GLSurfaceView {

    private MyRenderer myRenderer;

    private int mCameraTextureID = -1;
    private SurfaceTexture mPreviewSurfaceTexture;

    public int mCameraPreviewWidth = 1920;
    public int mCameraPreviewHeight = 1080;

    private GLTextureDrawer mGLTextureDrawer;
    private WaterMarkDrawer mWaterMarkDrawer;

    public MyGLSurfaceView(Context context) {
        this(context, null);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGLView();
    }

    private void initGLView() {
        this.myRenderer = new MyRenderer();
        this.setEGLContextClientVersion(2);
        this.setRenderer(this.myRenderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    @Override
    public void onResume() {
        super.onResume();
        CameraHelper.getInstance().startPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraHelper.getInstance().stopPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        CameraHelper.getInstance().releaseCamera();
        myRenderer.onSurfaceDestroyed();
    }

    class MyRenderer implements Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Logger.d("onSurfaceCreated");
            // 清屏颜色为黑色
            GLES20.glClearColor(0, 0, 0, 0);
            // 初始化矩阵
            GLMatrixState.setInitStack();

            // 创建external surfacetexture
            mCameraTextureID = GLTextureUtil.createOESTextureID();
            mPreviewSurfaceTexture = new SurfaceTexture(mCameraTextureID);
            mPreviewSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    // 接收到预览数据，主动刷新
                    MyGLSurfaceView.this.requestRender();
                }
            });

            // 创建相机
            CameraHelper.getInstance().openCamera(Camera.CameraInfo.CAMERA_FACING_BACK, mPreviewSurfaceTexture);

            mCameraPreviewWidth = CameraHelper.getInstance().getPreviewWidth();
            mCameraPreviewHeight = CameraHelper.getInstance().getPreviewHeight();

            // 专门用于绘制预览
            mGLTextureDrawer = new GLTextureDrawer(mCameraPreviewWidth, mCameraPreviewHeight);
            // 绘制一个水印
            mWaterMarkDrawer = new WaterMarkDrawer();

            // 开始预览
            CameraHelper.getInstance().startPreview();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Logger.d("onSurfaceChanged");
            // 设置视口
            GLES20.glViewport(0, 0, width, height);

            // 计算GLSurfaceView的宽高比
            float ratio = (float) width / height;
            // 设置camera位置
            GLMatrixState.setCamera(
                    //
                    0, // 人眼位置的X
                    0, // 人眼位置的Y
                    1, // 人眼位置的Z
                    //
                    0, // 人眼球看的点X
                    0, // 人眼球看的点Y
                    0, // 人眼球看的点Z
                    // up向量
                    0,
                    1,
                    0);
            // 调用此方法计算产生透视投影矩阵
            GLMatrixState.setProjectFrustum(-ratio, ratio, -1, 1, 1, 20);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            // 每次绘制都刷新纹理
            // 如果camera数据可用，手动取一次
            try {
                // 从摄像机更新数据
                if (mPreviewSurfaceTexture != null) {
                    mPreviewSurfaceTexture.updateTexImage(); // 更新
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 清除深度缓冲与颜色缓冲
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
                    | GLES20.GL_COLOR_BUFFER_BIT);


            // 进行渐变矩形的绘制
            GLMatrixState.pushMatrix();
            GLMatrixState.translate(0, 0, -1);
            // 看是否需要进行旋转
            GLMatrixState.rotate(-90, 0, 0, 1);

            // 最总变化矩阵
            float[] mVpMatrix = GLMatrixState.getFinalMatrix();
            // 绘制纹理矩形, 传入相机external纹理
            mGLTextureDrawer.draw(mCameraTextureID, mVpMatrix);

            // 恢复变换矩阵
            GLMatrixState.popMatrix();

            //---------------------------------------
            // 绘制水印
            mWaterMarkDrawer.draw();
        }

        public void onSurfaceDestroyed() {
            if (mPreviewSurfaceTexture != null) {
                mPreviewSurfaceTexture.release();
                mPreviewSurfaceTexture = null;
            }
            GLTextureUtil.deleteTex(mCameraTextureID);
        }

    }


}
