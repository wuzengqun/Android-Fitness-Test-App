package com.example.facedetect.listviewadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class FaceNameAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> names;

    public FaceNameAdapter(Context context, List<String> names) {
        super(context, android.R.layout.simple_list_item_1, names);
        this.context = context;
        this.names = names;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(names.get(position));

        return convertView;
    }
}
