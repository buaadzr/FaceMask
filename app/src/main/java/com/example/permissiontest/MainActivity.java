package com.example.permissiontest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.permissiontest.Util.temp;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;


import static com.example.permissiontest.Util.FaceDection.activeSdk;
import static com.example.permissiontest.Util.FaceDection.drawFaceRect;
import static com.example.permissiontest.Util.FaceDection.getFaceInfo;
import static com.example.permissiontest.Util.FaceDection.initEngine;
import static com.example.permissiontest.Util.FileUtils.Save_Pic;
import static com.example.permissiontest.Util.FileUtils.loadBitmap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private Button save_a_file;
    private Button show_a_image;
    private Button show_a_video;
    private Button play_a_video;
    private Button pause_a_video;
    private ImageView image;
    private TextView text;
    private VideoView video;
    private String path;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                //更新ui
                text.setText("转换完成，生成文件保存在" + path);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        initOpenCV();
        save_a_file = (Button) findViewById(R.id.save_a_file);
        show_a_image = (Button) findViewById(R.id.show_a_image);
        show_a_video = (Button) findViewById(R.id.show_a_video);
        play_a_video = (Button) findViewById(R.id.play_video);
        pause_a_video = (Button) findViewById(R.id.pause_video);
//        video = (VideoView) findViewById(R.id.video_view);
        image = (ImageView) findViewById(R.id.image);
        text = (TextView) findViewById(R.id.file);
        save_a_file.setOnClickListener(this);
        show_a_image.setOnClickListener(this);
        show_a_video.setOnClickListener(this);
        play_a_video.setOnClickListener(this);
        pause_a_video.setOnClickListener(this);

        activeSdk(this);
        initEngine(this);
        initPermission();

        path = this.getExternalFilesDir("").getPath();
    }
    private void initPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA);
    }

    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
        } else {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_a_file:
                String filename = "myfile";
                String fileContents = "Hello world!";
                try (FileOutputStream fos = this.openFileOutput(filename, Context.MODE_PRIVATE)) {
                    fos.write(fileContents.getBytes());
                    Toast.makeText(this, "Save success!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.show_a_image:
                Intent intent_image = new Intent(Intent.ACTION_GET_CONTENT);
                intent_image.setType("image/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent_image.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent_image, 1);
                break;
            case R.id.show_a_video:
                Intent intent_video = new Intent(Intent.ACTION_GET_CONTENT);
                intent_video.setType("video/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent_video.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent_video, 2);
                break;
            case R.id.play_video:
                if (!video.isPlaying()) {
                    video.start();
                }
                break;
            case R.id.pause_video:
                if (video.isPlaying()) {
                    video.pause();
                }
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (video != null) {
            video.suspend();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        try {
                            Bitmap bitmap = loadBitmap(this, uri, true);
                            getFaceInfo(bitmap);
                            bitmap = drawFaceRect(bitmap);
                            image.setImageBitmap(bitmap);
                            Save_Pic(this, bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        try {
                            Log.i("main activity", "path:" + uri.toString());
                            MyThread mThread = new MyThread(this, uri);
                            new Thread(mThread).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    class MyThread implements Runnable{
        private Context context;
        private Uri uri;

        public MyThread(Context context, Uri uri) {
            super();
            this.context = context;
            this.uri = uri;
        }

        @Override
        public void run() {
            temp.startConvert(context, uri);
            Message message = new Message();
            message.what = 0;
            handler.sendMessage(message);
        }
    }
}
