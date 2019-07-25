package com.ryan.openglvideorecorder;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ryan.openglvideorecorder.camera.CameraHelper;

public class MainActivity extends Activity {

    private MyGLSurfaceView myGLSurfaceView;
    private Button mBtnRecord;

    private volatile boolean mIsInRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myGLSurfaceView = findViewById(R.id.myGLSurfaceView);
        mBtnRecord = findViewById(R.id.btnRecord);
        mBtnRecord.setText("开始录像");
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsInRecord = !mIsInRecord;
                if (mIsInRecord) {
                    mBtnRecord.setText("停止录像");
                }
                else {
                    mBtnRecord.setText("开始录像");
                }
            }
        });
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
}
