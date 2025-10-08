package com.example.facedetect.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.example.facedetect.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.annotation.NonNull;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.io.IOException;

import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;


// extends 表示一个类是另一个类的子类，也就是继承
public class ScoreInquiryAngManageActivity extends Activity {
    Mysql mysql;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(ScoreInquiryAngManageActivity.this, FuncSelectFaceRecogActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);//这个设置可以让切换时不要动画，适合底部导航栏切换
                    return true;
                case R.id.navigation_score:
                    return true;
                case R.id.navigation_mine:
                    intent = new Intent(ScoreInquiryAngManageActivity.this, MineActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);//这个设置可以让切换时不要动画，适合底部导航栏切换
                    return true;
                // 处理其他菜单项
            }
            return false;
        }
    };

    @Override    // @Override 注解用于明确表示某个方法是覆盖了父类中的方法
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // super.onCreate(savedInstanceState); 调用了父类 Activity 的 onCreate 方法，这是在Android应用开发中的标准做法，以确保正确地初始化 Activity。
        setContentView(R.layout.scoreinquiryandmanageactivity);

        mysql = new Mysql(this,"Userinfo",null,1);      //建数据库

        //底部导航栏相关
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation_score_xml);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomNavigationView.setSelectedItemId(R.id.navigation_score);

        //阻止使用侧边退出
        LinearLayout yourView = findViewById(R.id.manage_layout_score);
        // 设置 OnTouchListener
        yourView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 处理触摸事件，阻止返回行为
                return true; // 返回 true 表示事件被处理，不传递给下一个处理者
            }
        });
    }

    //全部成绩显示
    public void allacorelook(View view) {
        Intent intent = new Intent();
        intent.setClass(ScoreInquiryAngManageActivity.this,Fourscore.class);

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    //清空成绩库
    public void allscoreclear(View view) {
        SQLiteDatabase db = mysql.getReadableDatabase();
        AlertDialog dialog = new AlertDialog.Builder(ScoreInquiryAngManageActivity.this)
                .setTitle(R.string.score_recognize_notification)
                .setPositiveButton(R.string.score_recognize_right, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // 关闭对话框
                        // 显示Toast消息
                        mysql.clearScoreLibraryTable(db);
                        db.close();
                        Toast.makeText(ScoreInquiryAngManageActivity.this, "score library clear success!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.score_recognize_error, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // 关闭对话框

                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    //导出全部成绩:存储到手机上的位置：内部存储/Android/data/com.example.facedetect/files
    public void allscoreoutputtest(View view) {
        try {
            SQLiteDatabase db = mysql.getReadableDatabase();
            // 导出scorelibrary表到私有文件目录
            mysql.exportTableAsFile(db, "scorelibrary", "scorelibrary.xls");
            db.close();
            //如果没抛异常就是成功了，成功之后下面显示对话框提示到哪寻找导出的文件：
            AlertDialog dialog = new AlertDialog.Builder(ScoreInquiryAngManageActivity.this)
                    .setTitle(R.string.scoreout_recognize_notification)
                    .setMessage(R.string.scoreout_recognize_message)
                    .setPositiveButton(R.string.scoreout_recognize_right, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss(); // 关闭对话框
                        }
                    })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
            // 处理异常
        }
        //将文件复制到外部目录（一开始导出的文件需要使用此ide查看：view/tool windows/device Explorer，文件位于：/data/data/com.example.facedetect/files）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            mysql.copyFileToExternal(ScoreInquiryAngManageActivity.this, "scorelibrary.xls", "scorelibrary_external.xls");
        }
    }

    //单人成绩查询
    public void singlescorefind(View view) {
        Intent intent = new Intent();
        intent.setClass(ScoreInquiryAngManageActivity.this,SearchScoreActivity.class);

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    //这里是禁用了侧滑返回功能，因为有bug。
    @Override
    public void onBackPressed() {
        // 不执行返回逻辑
    }
}