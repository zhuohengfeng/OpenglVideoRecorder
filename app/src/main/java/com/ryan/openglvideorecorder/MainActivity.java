package com.ryan.openglvideorecorder;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.ryan.openglvideorecorder.camera.CameraHelper;
import com.ryan.openglvideorecorder.widget.CameraProgressButton;

public class MainActivity extends Activity implements CameraProgressButton.Listener {

    private MyGLSurfaceView myGLSurfaceView;
    private CameraProgressButton mBtnRecord;

    private SurfaceView mShowRecordSurfaceView;

    private volatile boolean mIsInRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mShowRecordSurfaceView = findViewById(R.id.sf_showRecord);

        myGLSurfaceView = findViewById(R.id.myGLSurfaceView);
        myGLSurfaceView.setDisplayRecordView(mShowRecordSurfaceView);
        mBtnRecord = findViewById(R.id.btnRecord);
        mBtnRecord.setProgressListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        myGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myGLSurfaceView.onPause();
    }

    //========= 开始处理按钮事件 ================
    @Override
    public void onShortPress() {

    }

    @Override
    public void onStartLongPress() {
        myGLSurfaceView.startRecord();
    }

    @Override
    public void onEndLongPress() {
        myGLSurfaceView.stopRecord();
    }

    @Override
    public void onEndMaxProgress() {

    }
}
