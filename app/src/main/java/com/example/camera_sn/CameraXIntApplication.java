package com.example.camera_sn;

import android.app.Application;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraX;

public class CameraXIntApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CameraX.initialize(this, Camera2Config.defaultConfig());

    }
}
