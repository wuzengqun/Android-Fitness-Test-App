package com.example.facedetect.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.os.Handler;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Bitmap;
import java.util.Calendar;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.MotionEvent;

import com.example.facedetect.R;

public class PhysicalTestActivity extends Activity implements SurfaceHolder.Callback {
    public static final int REQUEST_CAMERA = 100;

    private Yolov8Ncnn yolov8ncnn = new Yolov8Ncnn();
    private int facing = 1;
    //private int selectmode;
    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private int countdown = 10; // 倒计时从60秒开始

    private boolean start_flag = false;

    private SurfaceView cameraView;
    AudioPlayer audioPlayer = new AudioPlayer(this);

    private Handler handler = new Handler();
    private Runnable runnable;   //定时器回调函数
    private long lastPlayTime = 0; // 记录上次播放的时间
    private int i = 8; //用于记录动作是否标准的
    private int flag = 0;  //用于判断倒计时是否结束
    int elapsedMillis = 0;  // 毫秒计数器

    String selectedName_Class_card; // 传入的班级姓名身份证号
    int selectmode;  //传入的模式选择

    Mysql mysql; //数据库

    SQLiteDatabase db; //定义可以操作数据库的实例

    int idd;

    private boolean pullup_isrecord = false;//用来记录三个运动哪个记过了


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.physicaltest);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraView = (SurfaceView) findViewById(R.id.cameraview);

        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        //阻止使用侧边退出
        LinearLayout yourView = findViewById(R.id.physical_layout);
        // 设置 OnTouchListener
        yourView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 处理触摸事件，阻止返回行为
                return true; // 返回 true 表示事件被处理，不传递给下一个处理者
            }
        });

        //定义屏幕显示的text
        TextView textViewcount = findViewById(R.id.textViewcount);
        TextView textViewCountdown = findViewById(R.id.textView_countdown);

        //数据库创建，如果已经存在就不创建
        mysql = new Mysql(this,"Userinfo",null,1);
        db = mysql.getReadableDatabase();

        // 获取传递的参数:name是名字;PROJECT_TYPE是项目类型
        Intent intent = getIntent();
        selectedName_Class_card  = intent.getStringExtra("name");
        selectmode  = intent.getIntExtra("PROJECT_TYPE", 0);

        //name：将传入的selectedName_Class使用_分割，得到姓名和班级并显示到text
        String[] parts = selectedName_Class_card.split("_");
        String name = parts.length > 0 ? parts[0] : "";  // 获取名字
        String className = parts.length > 1 ? parts[1] : "";  // 获取班级
        String card = parts.length > 2 ? parts[2] : "";  // 获取身份证号
        if (selectedName_Class_card != null) {
            // 在这里操作获取到的名字班级。例如，显示在TextView中
            TextView textView1 = findViewById(R.id.textViewName);
            TextView textView_class = findViewById(R.id.textViewClass);
            textView1.setText("姓名：" + name);
            textView_class.setText("班级：" + className);

        } else {
            // 处理没有获取到参数的情况
            Toast.makeText(this, "No param receive", Toast.LENGTH_SHORT).show();
        }

        //显示日期，使用 Calendar 获取当前日期
        TextView dateTextView = findViewById(R.id.textViewdate);
        Calendar calendar = Calendar.getInstance();
        String currentDate = calendar.get(Calendar.YEAR) + "-" +
                (calendar.get(Calendar.MONTH) + 1) + "-" +
                calendar.get(Calendar.DAY_OF_MONTH);
        dateTextView.setText("日期：" + currentDate);

        // project_type,设置显示当前项目
        TextView textView2 = findViewById(R.id.textViewProject);
        switch (selectmode)
        {
            case 0:
                textView2.setText("当前项目：引体向上");
                break;
            case 1:
                textView2.setText("当前项目：俯卧撑");
                break;
            case 2:
                textView2.setText("当前项目：仰卧起坐");
                break;
            case 3:
                textView2.setText("当前项目：深蹲");
                break;
            default:
                textView2.setText("当前项目：无");
                break;
        }

        //先把名字和日期写进数据库，后续的逻辑是，根据名字和日期查找，然后逐一判断，顺序是引体、俯卧撑、仰卧起坐，哪个为空就写入哪个
        //要先判断这个相同名字和日期的是否已经在数据库中初始化过了。
        Cursor cursor = db.query("scorelibrary",
                null,
                "user_name=? AND data=? AND card=?",
                new String[]{name, currentDate, card},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

        } else {
            ContentValues cv2 = new ContentValues();
            cv2.put("user_name",name);
            cv2.put("data",currentDate);
            cv2.put("card",card);
            db.insert("scorelibrary",null,cv2);
        }



        // 定时器：每隔50ms执行一次。主要逻辑：按下开始start_flag=1，此时开始倒计时并计数，当60s到时flag=1，进入if设置start_flag=0，此时
        // 弹出对话框显示姓名、项目、班级、个数、日期等，若是没有问题点击下一位又开始同一个项目的测试（先人脸识别）；如果对结果有异议可以点击
        // 有异议按钮，此时会...
        // i = 0表示引体下放时手臂未伸直；1表示引体上拉时下巴未过杆。
        runnable = new Runnable() {
            @Override
            public void run() {
                //50ms进一次这个if
                if(start_flag) {
                    elapsedMillis += 50;
                    int count = yolov8ncnn.getcount();
                    //一秒进一次下面这个if
                    if (elapsedMillis >= 1000) {
                        elapsedMillis = 0;  // 重置毫秒计数器
                        countdown--;
                        textViewCountdown.setText("倒计时: " + countdown + " S");
                        if (countdown == 0) {
                            flag = 1; // 设置标志位
                            textViewCountdown.setText("时间到!"); // 显示时间到
                            start_flag = false;  // 停止定时器
                            yolov8ncnn.setmode(4);   //设置4，停止cpp层代码计数
                            //数据库操作，将当前数据存入数据库，count这里需要使用switch分辨当前项目类型

                            Cursor cursor = db.query("scorelibrary",
                                    null,
                                    "user_name=? AND data=? AND card=?",
                                    new String[]{name, currentDate, card},
                                    null, null, null);

                            if (cursor != null && cursor.moveToFirst()) {
                                StringBuilder result = new StringBuilder();
                                do {
                                    int idIndex = cursor.getColumnIndex("_id");
                                    idd = cursor.getInt(idIndex);
                                    pullup_isrecord = true;
                                } while (cursor.moveToNext());
                            } else {
                                //textViewResult.setText("未找到记录");
                            }
                            if(pullup_isrecord == true)
                            {
                                ContentValues cv = new ContentValues();
                                switch (selectmode)
                                {
                                    case 0:
                                        cv.put("pullup", count);  //count就是当前的个数
                                        break;
                                    case 1:
                                        cv.put("pushup", count);  //count就是当前的个数
                                        break;
                                    case 2:
                                        cv.put("crunch", count);  //count就是当前的个数
                                        break;
                                    default:
                                        break;
                                }
                                String whereClause = "_id=?";
                                String[] whereArgs = new String[]{String.valueOf(idd)};

                                db.update("scorelibrary", cv, whereClause, whereArgs);
                                pullup_isrecord = false;
                            }

                            //对话框用于确认结果是否出现问题，并选择下一位
                            String message = getString(R.string.tice_recognize, selectedName_Class_card) + "\n" +
                                    getString(R.string.tice_count, count);
                            AlertDialog dialog = new AlertDialog.Builder(PhysicalTestActivity.this)
                                    .setTitle(R.string.face_recognize_notification)
                                    .setMessage(message)
                                    .setPositiveButton(R.string.face_recognize_right, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss(); // 关闭对话框
                                            onDestroy();
                                            Intent intent = new Intent(PhysicalTestActivity.this, RecognizeActivity.class);
                                            intent.putExtra("EXTRA_TYPE", selectmode);//直接进入人脸识别，将项目类别传入
                                            startActivity(intent);
                                        }
                                    })
                                    .setNegativeButton(R.string.face_recognize_error, new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss(); // 关闭对话框
                                            Intent intent = new Intent(PhysicalTestActivity.this, FuncSelectFaceRecogActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {
                            textViewCountdown.setText("倒计时: " + countdown + " S");
                        }
                    }

                    //下面这些都是50ms执行一次。
                    i = yolov8ncnn.getresult();
                    long currentTime = System.currentTimeMillis();
                    if (flag == 0) {
                        lastPlayTime = currentTime;
                        textViewcount.setText("当前个数：" + count);
                        switch (i) {
                            case 0:
                                audioPlayer.playSound(R.raw.yinti_not_fully_extended_down);
                                flag = 1;
                                break;
                            case 1:
                                audioPlayer.playSound(R.raw.yinti_chin_did_not_clear_the_bar);
                                flag = 1;
                                break;
                            case 2:
                                break;
                            default:
                                break;
                        }
                    } else {
                        if (currentTime - lastPlayTime > 4000) {
                            flag = 0;
                        }
                    }
                }
                // 再次调用postDelayed方法，实现定时器的效果
                handler.postDelayed(this, 50); // 1000毫秒后再次执行
            }
        };
        // 启动定时器
        handler.postDelayed(runnable, 1000); // 1秒后开始执行
        reload();
    }

    //查看信息库按钮回调函数
    public void start_count(View view) {
        start_flag = true;
        switch (selectmode)
        {
            case 0:
                audioPlayer.playSound(R.raw.start_yintixiangshang);
                yolov8ncnn.setmode(selectmode);
                break;
            case 1:
                audioPlayer.playSound(R.raw.start_fuwocheng);
                yolov8ncnn.setmode(selectmode);
                break;
            case 2:
                audioPlayer.playSound(R.raw.start_yangwoqizuo);
                yolov8ncnn.setmode(selectmode);
                break;
            case 3:
                audioPlayer.playSound(R.raw.start_shendun);
                yolov8ncnn.setmode(selectmode);
                break;
            //还有一个逻辑，停止，需要定时器，并且需要报出来离开始计数还有多久。
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消定时器
        handler.removeCallbacks(runnable);
    }

    private void reload()
    {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov8ncnn loadModel failed");
        }
    }

    //下面这三个是摄像头相关的回调函数
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    //1、Activity 重新获得焦点：当用户导航到另一个应用或 Activity 后返回到当前 Activity 时，onResume() 会被调用。
    //2、设备屏幕从休眠状态恢复：当设备从睡眠状态恢复并且当前 Activity 在前台时，onResume() 会被调用。
    //3、从其他 Activity 返回：当你通过 Intent 启动另一个 Activity，然后返回当前 Activity 时，onResume() 会被调用。
    @Override
    public void onResume()
    {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        yolov8ncnn.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        yolov8ncnn.closeCamera();
    }

    //这里是禁用了侧滑返回功能，因为有bug。
    @Override
    public void onBackPressed() {
        // 不执行返回逻辑
    }
}