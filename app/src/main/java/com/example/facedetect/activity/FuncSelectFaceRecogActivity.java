package com.example.facedetect.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.example.facedetect.R;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.annotation.NonNull;
import android.view.MenuItem;
import android.widget.LinearLayout;

import android.widget.Toast;
import android.widget.TextView;

public class FuncSelectFaceRecogActivity extends AppCompatActivity  {
    Button btnpullup, btnpushup, btncrunch, btnsitup, startfaceinfomanage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_score:
                    intent = new Intent(FuncSelectFaceRecogActivity.this, ScoreInquiryAngManageActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.navigation_mine:
                    intent = new Intent(FuncSelectFaceRecogActivity.this, MineActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.funcselect);

        btnpullup = this.findViewById(R.id.button_pullup);         //进入引体向上
        btnpushup = this.findViewById(R.id.button_pushup);         //进入俯卧撑
        btncrunch = this.findViewById(R.id.button_crunch);         //进入仰卧起坐
        btnsitup = this.findViewById(R.id.button_situp);           //进入深蹲

        startfaceinfomanage = this.findViewById(R.id.startfaceinfomanage);     //管理人脸信息库

        //底部导航栏相关
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation_funcselect_xml);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setItemIconTintList(null);

        //阻止使用侧边退出
        LinearLayout yourView = findViewById(R.id.manage_fun);
        // 设置 OnTouchListener
        yourView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 处理触摸事件，阻止返回行为
                return true; // 返回 true 表示事件被处理，不传递给下一个处理者
            }
        });


        // 设置班级下拉框选项
//        classSpinner = findViewById(R.id.classSpinner);
//        String[] classOptions = {"一班", "二班", "三班"};
//        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classOptions);
//        classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        classSpinner.setAdapter(classAdapter);
//        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//        @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // 这里可以将选择的班级保存或者传递
//            }
//        @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                Toast.makeText(FuncSelectFaceRecogActivity.this, "Please select a class", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // 设置年级下拉框选项
//        gradeSpinner = findViewById(R.id.gradeSpinner);
//        String[] gradeOptions = {"大一", "大二", "大三"};
//        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gradeOptions);
//        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        gradeSpinner.setAdapter(gradeAdapter);
//        gradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // 这里可以将选择的班级保存或者传递
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                Toast.makeText(FuncSelectFaceRecogActivity.this, "Please select a grade", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//
//        // 设置负责人下拉框选项
//        ownerSpinner = findViewById(R.id.ownerSpinner);
//        String[] ownerOptions = {"负责人 A", "负责人 B", "负责人 C"};
//        ArrayAdapter<String> ownerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ownerOptions);
//        ownerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        ownerSpinner.setAdapter(ownerAdapter);
//        ownerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // 这里可以将选择的班级保存或者传递
//            }
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                Toast.makeText(FuncSelectFaceRecogActivity.this, "Please select a onwer", Toast.LENGTH_SHORT).show();
//            }
//        });

        //进入引体
        btnpullup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectmode_pullup = 0;

                Intent intent = new Intent();
                intent.setClass(FuncSelectFaceRecogActivity.this,RecognizeActivity.class);          //跳转到人脸识别页面
                intent.putExtra("EXTRA_TYPE", selectmode_pullup);

                startActivity(intent);
            }
        });

        //进入俯卧撑
        btnpushup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectmode_pushup = 1;

                Intent intent = new Intent();
                intent.setClass(FuncSelectFaceRecogActivity.this,RecognizeActivity.class);          //跳转到体测页面
                intent.putExtra("EXTRA_TYPE", selectmode_pushup);

                startActivity(intent);
            }
        });

        //进入仰卧起坐
        btncrunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectmode_crunch = 2;

                Intent intent = new Intent();
                intent.setClass(FuncSelectFaceRecogActivity.this,RecognizeActivity.class);          //跳转到体测页面
                intent.putExtra("EXTRA_TYPE", selectmode_crunch);

                startActivity(intent);
            }
        });

        //进入俯卧撑
        btnsitup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectmode_situp = 3;

                Intent intent = new Intent();
                intent.setClass(FuncSelectFaceRecogActivity.this,RecognizeActivity.class);          //跳转到体测页面
                intent.putExtra("EXTRA_TYPE", selectmode_situp);

                startActivity(intent);
            }
        });

        //进入人脸信息库管理，到FaceManageActivity中，可以选择查看信息库和清空信息库
        startfaceinfomanage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(FuncSelectFaceRecogActivity.this,FaceManageActivity.class);

                startActivity(intent);
            }
        });
    }

    //这里是禁用了侧滑返回功能，因为有bug。
    @Override
    public void onBackPressed() {
        // 不执行返回逻辑
    }


}
