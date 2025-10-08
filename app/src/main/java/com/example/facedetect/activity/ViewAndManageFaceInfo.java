package com.example.facedetect.activity;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

import com.example.facedetect.R;
import com.example.facedetect.faceserver.FaceServer;
import com.example.facedetect.listviewadapter.FaceNameAdapter;

public class ViewAndManageFaceInfo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewandmanagefaceinfo);

        // 获取注册的人脸名称
        FaceServer faceServer = FaceServer.getInstance();
        List<String> registeredNames = faceServer.getAllRegisteredFaceNames();

        // 找到ListView并设置适配器
        ListView listView = findViewById(R.id.listView);
        FaceNameAdapter adapter = new FaceNameAdapter(this, registeredNames);
        listView.setAdapter(adapter);
    }
}
