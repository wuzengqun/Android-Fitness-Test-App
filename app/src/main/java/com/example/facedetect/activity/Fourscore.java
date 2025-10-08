package com.example.facedetect.activity;

import android.os.Bundle;
import android.widget.ListView;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facedetect.listviewadapter.ScoreArrayAdapter;

import com.example.facedetect.R;

    public class Fourscore extends AppCompatActivity {
    ListView scoresListView;
    SQLiteDatabase db;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_scores);

        // 初始化数据库
        Mysql mysql = new Mysql(this, "Userinfo", null, 1);
        db = mysql.getReadableDatabase();

        // 查询scorelibrary表
        cursor = db.query("scorelibrary", new String[]{"_id","user_name", "data","card","pullup","pushup","crunch"}, null, null, null, null, null);
        cursor.moveToFirst();

        // 假设您有一个适配器来显示数据
        ScoreArrayAdapter adapter = new ScoreArrayAdapter(this, cursor);
        scoresListView = findViewById(R.id.scoresListView);
        scoresListView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }
}