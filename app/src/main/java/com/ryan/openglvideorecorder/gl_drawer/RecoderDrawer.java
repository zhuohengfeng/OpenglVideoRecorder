package com.ryan.openglvideorecorder.gl_drawer;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.EGLSurface;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;

import com.ryan.openglvideorecorder.encodec.VideoEncoder;
import com.ryan.openglvideorecorder.gl_egl.MyEGLManager;
import com.ryan.openglvideorecorder.gl_utils.GLShaderUtil;
import com.ryan.openglvideorecorder.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 开始一个自定义的EGL，用来把数据绘制到EGL的surface上
 * 1. 这里先传入一个普通的surfaceview，把要录像的内容先绘制到这个surfaceview上看看
 */
public class RecoderDrawer {

    protected int width;

    protected int height;

    protected int mProgram;

    //顶点坐标 Buffer
    private FloatBuffer mVertexBuffer;
    protected int mVertexBufferId;

    //纹理坐标 Buffer
    private FloatBuffer mFrontTextureBuffer;
    protected int mFrontTextureBufferId;

    //纹理坐标 Buffer
    private FloatBuffer mBackTextureBuffer;
    protected int mBackTextureBufferId;

    private FloatBuffer mDisplayTextureBuffer;
    protected int mDisplayTextureBufferId;
    private FloatBuffer mFrameTextureBuffer;
    protected int mFrameTextureBufferId;

    private int av_Position;
    private int af_Position;
    private int s_Texture;

    protected float vertexData[] = {
            -1f, -1f,// 左下角
            1f, -1f, // 右下角
            -1f, 1f, // 左上角
            1f, 1f,  // 右上角
    };
    protected float frontTextureData[] = {
            1f, 1f, // 右上角
            1f, 0f, // 右下角
            0f, 1f, // 左上角
            0f, 0f //  左下角
    };
    protected float backTextureData[] = {
            0f, 1f, // 左上角
            0f, 0f, //  左下角
            1f, 1f, // 右上角
            1f, 0f  // 右上角
    };
    protected float displayTextureData[] = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
    };
    protected float frameBufferData[] = {
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
    };

    protected final int CoordsPerVertexCount = 2;
    protected final int VertexCount = vertexData.length / CoordsPerVertexCount;
    protected final int VertexStride = CoordsPerVertexCount * 4;
    protected final int CoordsPerTextureCount = 2;
    protected final int TextureStride = CoordsPerTextureCount * 4;



    // 绘制的纹理 ID
    private int mTextureId;
    private SurfaceView mDisplaySurfaceView;
    private VideoEncoder mVideoEncoder;
    private String mVideoPath;
    private MyEGLManager mEglHelper;
    private boolean isRecording = false;
    private EGLContext mEglContext;

    private HandlerThread mBackgroundThread;
    private Handler mMsgHandler;

    public RecoderDrawer(Context context) {

        // 启动后台线程
        mBackgroundThread = new HandlerThread("RecoderThread");
        mBackgroundThread.start();
        mMsgHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleRecordMessage(msg);
            }
        };
    }


    public static final int MSG_START_RECORD = 1;
    public static final int MSG_STOP_RECORD = 2;
    public static final int MSG_UPDATE_CONTEXT = 3;
    public static final int MSG_UPDATE_SIZE = 4;
    public static final int MSG_FRAME = 5;
    public static final int MSG_QUIT = 6;
    private void handleRecordMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_RECORD:
                prepareVideoEncoder((EGLContext) msg.obj, msg.arg1, msg.arg2);
                break;
            case MSG_STOP_RECORD:
                stopVideoEncoder();
                break;
            case MSG_FRAME:
                drawFrame();
                break;
            default:
                break;
        }
    }




    /**
     * 初始化EGL环境，共享之前的EGL上下文
     * 得到要采样的纹理， 这里传入的是相机的external camera textureid
     * 并得到要绘制到哪里，这里是一个普通的surfaceview
     */
    public void initEGL(int textureId, SurfaceView surfaceView) {
        mTextureId = textureId; // 这里传入的是相机的external camera textureid
        mDisplaySurfaceView = surfaceView;
        mEglContext = EGL14.eglGetCurrentContext();
        mProgram = GLShaderUtil.createProgram(getVertexSource(), getFragmentSource());
        initVertexBufferObjects();
        av_Position = GLES30.glGetAttribLocation(mProgram, "av_Position");
        af_Position = GLES30.glGetAttribLocation(mProgram, "af_Position");
        s_Texture = GLES30.glGetUniformLocation(mProgram, "s_Texture");
    }

    public void surfaceChangedSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void startRecord() {
        Logger.d( "startRecord context : " + mEglContext.toString());
        Message msg = mMsgHandler.obtainMessage(MSG_START_RECORD, width, height, mEglContext);
        mMsgHandler.sendMessage(msg);
        isRecording = true;
    }

    public void stopRecord() {
        Logger.d(  "stopRecord");
        isRecording = false;
        mMsgHandler.sendMessage(mMsgHandler.obtainMessage(MSG_STOP_RECORD));
    }

    /**
     * 初始化EGL上下文
     * @param eglContext
     * @param width
     * @param height
     */
    private void prepareVideoEncoder(EGLContext eglContext, int width, int height) {
        // 初始新的EGL上下文, 这里是复用之前的eglContext，注意这里会调用makeCurrent
        mEglHelper = new MyEGLManager(mEglContext, mDisplaySurfaceView);
    }

    public void draw(float[] transformMatrix){
        if (isRecording) {
            clear();
            useProgram();
            viewPort(0, 0, width, height);
            Logger.d("draw: ");
            Message msg = mMsgHandler.obtainMessage(MSG_FRAME, 0);
            mMsgHandler.sendMessage(msg);
        }
    }

    private void stopVideoEncoder() {
        if (mEglHelper != null) {
            mEglHelper.release();
            mEglHelper = null;
        }
    }


    private void drawFrame() {
        Logger.d( "drawFrame: " );
        onDraw();
        //mEglHelper.setPresentationTime(mEglSurface, timeStamp);
        mEglHelper.swapMyEGLBuffers();// 显示内容
    }


    protected void onDraw() {
        clear();
        useProgram();
        viewPort(0, 0, width, height);

        GLES30.glEnableVertexAttribArray(av_Position);
        GLES30.glEnableVertexAttribArray(af_Position);
//        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, VertexStride, mVertexBuffer);
//        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, TextureStride, mDisplayTextureBuffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glVertexAttribPointer(av_Position, CoordsPerVertexCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glVertexAttribPointer(af_Position, CoordsPerTextureCount, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId); // 这里的mTextureId其实是FBO中的textureid
        GLES30.glUniform1i(s_Texture, 0);
        // 绘制 GLES30.GL_TRIANGLE_STRIP:复用坐标
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VertexCount);
        GLES30.glDisableVertexAttribArray(av_Position);
        GLES30.glDisableVertexAttribArray(af_Position);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }


    protected void clear(){
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    }

    protected void initVertexBufferObjects() {
        int[] vbo = new int[5];
        GLES30.glGenBuffers(5, vbo, 0);

        mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        mVertexBuffer.position(0);
        mVertexBufferId = vbo[0];
        // ARRAY_BUFFER 将使用 Float*Array 而 ELEMENT_ARRAY_BUFFER 必须使用 Uint*Array
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVertexBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.length * 4, mVertexBuffer, GLES30.GL_STATIC_DRAW);


        mBackTextureBuffer = ByteBuffer.allocateDirect(backTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(backTextureData);
        mBackTextureBuffer.position(0);
        mBackTextureBufferId = vbo[1];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mBackTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, backTextureData.length * 4, mBackTextureBuffer, GLES30.GL_STATIC_DRAW);

        mFrontTextureBuffer = ByteBuffer.allocateDirect(frontTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frontTextureData);
        mFrontTextureBuffer.position(0);
        mFrontTextureBufferId = vbo[2];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mFrontTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, frontTextureData.length * 4, mFrontTextureBuffer, GLES30.GL_STATIC_DRAW);

        mDisplayTextureBuffer = ByteBuffer.allocateDirect(displayTextureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(displayTextureData);
        mDisplayTextureBuffer.position(0);
        mDisplayTextureBufferId = vbo[3];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mDisplayTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, displayTextureData.length * 4, mDisplayTextureBuffer, GLES30.GL_STATIC_DRAW);

        mFrameTextureBuffer = ByteBuffer.allocateDirect(frameBufferData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(frameBufferData);
        mFrameTextureBuffer.position(0);
        mFrameTextureBufferId = vbo[4];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mFrameTextureBufferId);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, frameBufferData.length * 4, mFrameTextureBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
    }

    private void useProgram(){
        GLES30.glUseProgram(mProgram);
    }

    private void viewPort(int x, int y, int width, int height) {
        GLES30.glViewport(x, y, width,  height);
    }

    private String getVertexSource() {
        final String source = "attribute vec4 av_Position; " +
                "attribute vec2 af_Position; " +
                "varying vec2 v_texPo; " +
                "void main() { " +
                "    v_texPo = af_Position; " +
                "    gl_Position = av_Position; " +
                "}";
        return source;
    }

    private String getFragmentSource() {
        final String source = "precision mediump float;\n" +
                "varying vec2 v_texPo;\n" +
                "uniform sampler2D s_Texture;\n" +
                "void main() {\n" +
                "   vec4 tc = texture2D(s_Texture, v_texPo);\n" +
                "   gl_FragColor = texture2D(s_Texture, v_texPo);\n" +
                "}";
        return source;
    }


}
