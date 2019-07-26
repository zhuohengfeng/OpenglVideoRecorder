package com.ryan.openglvideorecorder.gl_egl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ryan.openglvideorecorder.utils.Logger;

/**
 * 创建EGL环境，这里接收的surface就是mediacodce的surface
 * 也就是说，这里创建的EGL环境，后续的绘制内容就是绘制到这个surface上，来实现录像功能
 */
public class MyEGLManager {

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLContext mEglContext = EGL14.EGL_NO_CONTEXT;
    private EGLDisplay mEglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLSurface mEglSurface = EGL14.EGL_NO_SURFACE;

    /**
     * 这里可以传入一个存在的inEglContext
     * inSurface 可以是已经存在的surfaceView, surface, surfaceHolder, surfaceTexture等
     * @param inEglContext
     * @param inSurface
     */
    public MyEGLManager(final EGLContext inEglContext, final Object inSurface) {
        initEglManager(inEglContext, inSurface);
    }

    private void initEglManager(EGLContext inEglContext, Object inSurface) {
        //可以是已经存在的surfaceView, surface, surfaceHolder, surfaceTexture
        if (!(inSurface instanceof SurfaceView)
                && !(inSurface instanceof Surface)
                && !(inSurface instanceof SurfaceHolder)
                && !(inSurface instanceof SurfaceTexture)) {
            throw new IllegalArgumentException("unsupported inSurface");
        }

        //--------------------mEGLDisplay-----------------------
        // 1. 获取EglDisplay
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        // 2. 初始化EglDisplay
        final int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null;
            throw new RuntimeException("eglInitialize failed");
        }

        //--------------------mEglConfig-----------------------
        // Configure EGL for recording and OpenGL ES 2.0.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                // 录制android
                EGL_RECORDABLE_ANDROID, // 这个是必要的？
                1,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(mEglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);
        if (configs[0] == null) {
            throw new RuntimeException("chooseConfig failed");
        }

        //--------------------mEglContext-----------------------
        EGLContext myEglContext = inEglContext;
        if (myEglContext == null) { // 如果传入的EglContext不为空，就说明可以共享context
            myEglContext = EGL14.EGL_NO_CONTEXT;
        }
        //
        final int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        //
        mEglContext = EGL14.eglCreateContext(mEglDisplay, configs[0], myEglContext, attrib_list, 0);
        checkMyEGLError("eglCreateContext");

        //-------------------------mEglSurface-------------------------
        //
        final int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        //
        try {
            mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, configs[0], inSurface, surfaceAttribs, 0);
        } catch (final IllegalArgumentException e) {
            Logger.e("eglCreateWindowSurface : "+e.getMessage());
        }
        //-----------------------------
        makeMyEGLCurrentSurface();
    }


    /**
     * 设置为当前的EGL环境
     *
     * @return
     */
    private boolean makeMyEGLCurrentSurface() {
        //
        if (mEglDisplay == null) {
            Logger.e("mEglDisplay == null");
            return false;
        }
        if (mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE) {
            final int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                Logger.e( "makeMyEGLCurrentSurface:returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }
        // EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            Logger.e("eglMakeCurrent:" + EGL14.eglGetError());
            return false;
        }
        return true;
    }


    /**
     * 交换buffer数据 ----- 最后做显示，要调用swap
     *
     * @return
     */
    public int swapMyEGLBuffers() {
        //
        boolean result = EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
        //
        if (!result) {
            final int err = EGL14.eglGetError();
            return err;
        }
        //
        return EGL14.EGL_SUCCESS;
    }


    /**
     * 释放资源
     */
    public void release() {

        // -------mEglSurface----------
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface);

        // -------mEglContext----------
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        EGL14.eglTerminate(mEglDisplay);
        EGL14.eglReleaseThread();
        //
        mEglSurface = EGL14.EGL_NO_SURFACE;
        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
    }


    private void checkMyEGLError(final String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}
