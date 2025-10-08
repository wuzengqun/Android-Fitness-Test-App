package com.example.facedetect.activity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;

import androidx.annotation.Nullable;

// extends 表示一个类是另一个类的子类，也就是继承
public class Mysql extends SQLiteOpenHelper  {
    private Context mContext;

    public Mysql(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建两个表，一个保存登录密码，一个保存体测成绩
        String sql2 = "create table scorelibrary(_id integer primary key autoincrement,user_name text,data text, card text,pullup int,pushup int,crunch int)";
        db.execSQL(sql2);

        String sql = "create table logins(id integer primary key autoincrement,usname text,uspwd text)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // 清空scorelibrary表
    public void clearScoreLibraryTable(SQLiteDatabase db) {
        db.execSQL("DELETE FROM scorelibrary");
    }

    // 导出scorelibrary表
    public void exportTableAsFile(SQLiteDatabase db, String tableName, String fileName) throws IOException {
        if (mContext == null) {
            throw new IllegalStateException("Context cannot be null when exporting database");
        }

        // 查询表
        String query = "SELECT * FROM " + tableName;
        // 获取游标
        android.database.Cursor cursor = db.rawQuery(query, null);

        // 确保cursor不为空
        if (cursor != null && cursor.moveToFirst()) {
            // 获取列名
            String[] columns = cursor.getColumnNames();

            // 获取文件输出流
            FileOutputStream outputStream = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);

            // 写入列名作为标题
            for (int i = 0; i < columns.length; i++) {
                outputStream.write((columns[i] + "\t").getBytes());
            }
            outputStream.write("\n".getBytes());

            // 循环游标并写入数据
            while (!cursor.isAfterLast()) {
                for (int i = 0; i < columns.length; i++) {
                    outputStream.write((cursor.getString(i) + "\t").getBytes());
                }
                outputStream.write("\n".getBytes());
                cursor.moveToNext();
            }

            // 关闭游标和文件输出流
            cursor.close();
            outputStream.close();
        } else if (cursor != null) {
            // 如果游标为空，关闭游标
            cursor.close();
        }
    }

    //将导出的文件从私有目录复制到外部存储目录
    public void copyFileToExternal(Context context, String fileName, String newFileName) {
        File file = new File(context.getFilesDir(), fileName);
        File newFile = new File(context.getExternalFilesDir(null), newFileName);

        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = new FileOutputStream(newFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}