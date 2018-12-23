package com.android.zone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.online.breakpad.BreakpadInit;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private File externalReportPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            initExternalReportPath();
        }

        findViewById(R.id.bt_crash)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                initBreakPad();
                                nativeCrash();
                                // copy core dump to sdcard
                            }
                        });
    }

    /**
     * 一般来说，crash捕获初始化都会放到Application中，这里主要是为了大家有机会可以把崩溃文件输出到sdcard中
     * 做进一步的分析
     */
    private void initBreakPad() {
        if (externalReportPath == null) {
            externalReportPath = new File(getFilesDir(), "crashDump");
            if (!externalReportPath.exists()) {
                externalReportPath.mkdirs();
            }
        }
        BreakpadInit.initBreakpad(externalReportPath.getAbsolutePath());
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initExternalReportPath();
    }

    private void initExternalReportPath() {
        externalReportPath = new File(Environment.getExternalStorageDirectory(), "crashDump");
        if (!externalReportPath.exists()) {
            externalReportPath.mkdirs();
        }
    }


    public native void nativeCrash();
}
