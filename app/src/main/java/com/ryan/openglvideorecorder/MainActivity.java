package com.ryan.openglvideorecorder;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.ryan.openglvideorecorder.camera.CameraHelper;

public class MainActivity extends Activity {

    private MyGLSurfaceView myGLSurfaceView;
    private Button mBtnRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myGLSurfaceView = findViewById(R.id.myGLSurfaceView);
        mBtnRecord = findViewById(R.id.btnRecord);
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
