package com.example.facedetect.activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.facedetect.R;

public class SearchScoreActivity extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextClass;
    private TextView textViewResult;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_score);

        editTextName = findViewById(R.id.editTextName);
        editTextClass = findViewById(R.id.editTextClass);
        Button buttonSearch = findViewById(R.id.buttonSearch);
        textViewResult = findViewById(R.id.textViewResult);

        // 初始化数据库
        Mysql mysql = new Mysql(this, "Userinfo", null, 1);
        db = mysql.getReadableDatabase();

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchStudent();
            }
        });
    }

    private void searchStudent() {
        String name = editTextName.getText().toString();
        String className = editTextClass.getText().toString();

//        Cursor cursor = db.query("scorelibrary",
//                new String[]{"_id","user_name", "test_date","text_param3","text_param4","text_param5"},
//                null,
//                null,
//                null, null, null);

        Cursor cursor = db.query("scorelibrary",
                null,
                "user_name=? AND data=?",
                new String[]{name, className},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            StringBuilder result = new StringBuilder();
            do {
                int idIndex = cursor.getColumnIndex("_id");
                int userNameIndex = cursor.getColumnIndex("user_name");
                int testDateIndex = cursor.getColumnIndex("data");
                int pullupIndex = cursor.getColumnIndex("pullup");
                String namedata = cursor.getString(userNameIndex);
                String testDate = cursor.getString(testDateIndex);
                int pullup = cursor.getInt(pullupIndex);

                result.append("姓名: ").append(namedata).append("\n")
                        .append("日期: ").append(testDate).append("\n")
                        .append("引体: ").append(pullup).append("\n");
            } while (cursor.moveToNext());

            textViewResult.setText(result.toString());
        } else {
            textViewResult.setText("未找到记录");
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
