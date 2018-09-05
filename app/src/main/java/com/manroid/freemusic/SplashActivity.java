package com.manroid.freemusic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class SplashActivity extends AppCompatActivity {

    public static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (checkRuntimePermission()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(MainActivity.class);
                }
            }, 2000);
        }
    }


    public boolean checkRuntimePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,}, PERMISSION_REQUEST_CODE);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean readExternalStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (readExternalStorage) {
                        startActivity(MainActivity.class);
                    } else {
                        Toast.makeText(this, getString(R.string.please_update_permission), Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    private void startActivity(Class clazz) {
        startActivity(new Intent(this, clazz));
    }
}
