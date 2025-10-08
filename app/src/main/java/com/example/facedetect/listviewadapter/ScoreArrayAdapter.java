package com.example.facedetect.listviewadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.CursorAdapter;
import android.database.Cursor;

import com.example.facedetect.R;

import java.util.List;

public class ScoreArrayAdapter extends CursorAdapter {
    public ScoreArrayAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_score, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView userNameTextView = view.findViewById(R.id.text_user_name);
        TextView testDateTextView = view.findViewById(R.id.text_test_date);
        TextView param3TextView = view.findViewById(R.id.text_param3);
        TextView param4TextView = view.findViewById(R.id.text_param4);
        TextView param5TextView = view.findViewById(R.id.text_param5);

        int userNameIndex = cursor.getColumnIndex("user_name");
        int testDateIndex = cursor.getColumnIndex("data");
        int text_param3 = cursor.getColumnIndex("pullup");
        int text_param4 = cursor.getColumnIndex("pushup");
        int text_param5 = cursor.getColumnIndex("crunch");

        if (userNameIndex != -1) {
            userNameTextView.setText(cursor.getString(userNameIndex));
        }
        if (userNameIndex != -1) {
            testDateTextView.setText(cursor.getString(testDateIndex));
        }
        if (text_param3 != -1) {
            param3TextView.setText(cursor.getString(text_param3));
        }
        if (text_param4 != -1) {
            param4TextView.setText(cursor.getString(text_param4));
        }
        if (text_param5 != -1) {
            param5TextView.setText(cursor.getString(text_param5));
        }

        // 绑定其他数据到视图...
    }

    @Override
    public long getItemId(int position) {
        Cursor cursor = getCursor();
        if (cursor != null && cursor.moveToPosition(position)) {
            int idIndex = cursor.getColumnIndex("id");
            if (idIndex != -1) {
                return cursor.getLong(idIndex);
            }
        }
        return 0; // 或者可以返回一个默认值，如 -1
    }
}