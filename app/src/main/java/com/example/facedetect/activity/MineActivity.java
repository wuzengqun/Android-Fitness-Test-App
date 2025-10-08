package com.example.facedetect.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.RuntimeABI;
import com.example.facedetect.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.example.facedetect.common.Constants;
import com.example.facedetect.fragment.ChooseDetectDegreeDialog;
import com.example.facedetect.fragment.PermissionDegreeDialog;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

// extends 表示一个类是另一个类的子类，也就是继承
public class MineActivity extends BaseActivity {
    private static final String TAG = "ChooseFunctionActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    // 在线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE};

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(MineActivity.this, FuncSelectFaceRecogActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);//这个设置可以让切换时不要动画，适合底部导航栏切换
                    return true;
                case R.id.navigation_score:
                    intent = new Intent(MineActivity.this, ScoreInquiryAngManageActivity.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);//这个设置可以让切换时不要动画，适合底部导航栏切换
                    return true;
                case R.id.navigation_mine:
                    // 可能你想执行一些特定操作或者回到这个Activity
                    return true;
                // 处理其他菜单项
            }
            return false;
        }
    };

    @Override    // @Override 注解用于明确表示某个方法是覆盖了父类中的方法
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // super.onCreate(savedInstanceState); 调用了父类 Activity 的 onCreate 方法，这是在Android应用开发中的标准做法，以确保正确地初始化 Activity。
        setContentView(R.layout.mine);

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation_mine_xml);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        bottomNavigationView.setSelectedItemId(R.id.navigation_mine);

        //阻止使用侧边退出
        LinearLayout yourView = findViewById(R.id.manage_layout_mine);
        // 设置 OnTouchListener
        yourView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 处理触摸事件，阻止返回行为
                return true; // 返回 true 表示事件被处理，不传递给下一个处理者
            }
        });
    }

    //修改密码
    public void secretchange(View view) {
//        Intent intent = new Intent();
//        intent.setClass(ScoreInquiryAngManageActivity.this,FuncSelectFaceRecogActivity.class);
//
//        startActivity(intent);
//        overridePendingTransition(0, 0);
    }

    //退出登录
    public void exit(View view) {
//        Intent intent = new Intent();
//        intent.setClass(ScoreInquiryAngManageActivity.this,FuncSelectFaceRecogActivity.class);
//
//        startActivity(intent);
//        overridePendingTransition(0, 0);
    }

    //激活引擎
    public void activate_engine(final View view) {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        if (view != null) {
            view.setClickable(false);
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(ObservableEmitter<Integer> emitter) {
                        RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
                        Log.i(TAG, "subscribe: getRuntimeABI() " + runtimeABI);

                        long start = System.currentTimeMillis();
                        int activeCode = FaceEngine.activeOnline(MineActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                        Log.i(TAG, "subscribe cost: " + (System.currentTimeMillis() - start));
                        emitter.onNext(activeCode);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast(getString(R.string.active_success));
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                            showToast(getString(R.string.already_activated));
                        } else {
                            showToast(getString(R.string.active_failed, activeCode));
                        }

                        if (view != null) {
                            view.setClickable(true);
                        }
                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(MineActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast(e.getMessage());
                        if (view != null) {
                            view.setClickable(true);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    //这里是禁用了侧滑返回功能，因为有bug。
    @Override
    public void onBackPressed() {
        // 不执行返回逻辑
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                activate_engine(null);
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    void checkLibraryAndJump(Class activityClass) {
        startActivity(new Intent(this, activityClass));
    }
}