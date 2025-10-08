package com.example.facedetect.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.facedetect.R;

// extends 表示一个类是另一个类的子类，也就是继承
public class WelcomeActivity extends Activity {
    @Override    // @Override 注解用于明确表示某个方法是覆盖了父类中的方法
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // super.onCreate(savedInstanceState); 调用了父类 Activity 的 onCreate 方法，这是在Android应用开发中的标准做法，以确保正确地初始化 Activity。
        setContentView(R.layout.welcome);

        // 设置一个定时器，3秒后自动跳转到主界面
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // 关闭当前Activity
            }
        }, 3000);
    }
}