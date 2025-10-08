package com.example.facedetect.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.widget.EditText;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.Toast;
import android.content.ContentValues;

import com.example.facedetect.R;

// extends 表示一个类是另一个类的子类，也就是继承
public class RegisterActivity extends Activity {
    EditText usename,usepwd,usepwd2;
    Button submit;
    Mysql mysql;
    SQLiteDatabase db;
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        usename = this.findViewById(R.id.usename);			    //用户名编辑框
        usepwd =  this.findViewById(R.id.usepwd);				//设置初始密码编辑框
        usepwd2 = this.findViewById(R.id.usepwd2);			    //二次输入密码编辑框
        submit =   this.findViewById(R.id.submit);				//注册按钮
        mysql = new Mysql(this,"Userinfo",null,1);      //建数据库
        db = mysql.getReadableDatabase();
        sp = this.getSharedPreferences("useinfo",this.MODE_PRIVATE);


        submit.setOnClickListener(new View.OnClickListener() {
            boolean flag = true;            //判断用户是否已存在的标志位
            @Override
            public void onClick(View v) {
                String name = usename.getText().toString();				//用户名
                String pwd01 = usepwd.getText().toString();				//密码
                String pwd02 = usepwd2.getText().toString();			//二次输入的密码
                //String sex = "";										//性别
                if(name.equals("")||pwd01 .equals("")||pwd02.equals("")){
                    Toast.makeText(RegisterActivity.this, "用户名或密码不能为空!！", Toast.LENGTH_LONG).show();
                }
                else{
                    Cursor cursor = db.query("logins",new String[]{"usname"},null,null,null,null,null);
                    while (cursor.moveToNext()){
                        if(cursor.getString(0).equals(name)){
                            flag = false;
                            break;
                        }
                    }
                    if(flag==true){                                             //判断用户是否已存在,flag==true表示用户不存在
                        if (pwd01.equals(pwd02)) {								//判断两次输入的密码是否一致，若一致则继续，不一致则提醒密码不一致
                            ContentValues cv = new ContentValues();
                            cv.put("usname",name);
                            cv.put("uspwd",pwd01);
                            db.insert("logins",null,cv);
//                            ContentValues cv2 = new ContentValues();
//                            cv2.put("user_name",name);
//                            cv2.put("card","35xxxx20010818xxxx");
//                            cv2.put("data",pwd01);
//                            cv2.put("pullup",2);
//                            cv2.put("pushup",2);
//                            cv2.put("crunch",3);
//                            db.insert("scorelibrary",null,cv2);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("usname",name);
                            editor.putString("uspwd",pwd01);
                            editor.commit();
                            Intent intent = new Intent();
                            intent.setClass(RegisterActivity.this, LoginActivity.class);      //跳转到登录页面
                            startActivity(intent);
                            Toast.makeText(RegisterActivity.this, "注册成功！", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "密码不一致！", Toast.LENGTH_LONG).show();			//提示密码不一致
                        }
                    }
                    else{
                        Toast.makeText(RegisterActivity.this, "用户已存在！", Toast.LENGTH_LONG).show();			//提示密码不一致
                    }

                }
            }


        });
    }
}