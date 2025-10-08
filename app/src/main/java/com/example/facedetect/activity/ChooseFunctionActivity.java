package com.example.facedetect.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.example.facedetect.R;
import com.example.facedetect.common.Constants;
import com.example.facedetect.fragment.ChooseDetectDegreeDialog;
import com.example.facedetect.fragment.PermissionDegreeDialog;
import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.RuntimeABI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class ChooseFunctionActivity extends BaseActivity {
    private static final String TAG = "ChooseFunctionActivity";
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    // 在线激活所需的权限
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    boolean libraryExists = true;
    // Demo 所需的动态库文件
//    private static final String[] LIBRARIES = new String[]{
//            // 人脸相关
//            "libarcsoft_face_engine.so",
//            "libarcsoft_face.so",
//            // 图像库相关
//            "libarcsoft_image_util.so",
//    };
    // 修改配置项的对话框
    //ChooseDetectDegreeDialog chooseDetectDegreeDialog;
    //PermissionDegreeDialog permissionDegreeDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_function);
        //activeEngine(null);
        //permissionDegreeDialog();
        //不知道为什么一直提示没有动态链接库，但实际上查看sdk是包含了库的，所以这里不做检查
//        libraryExists = checkSoFile(LIBRARIES);
//        ApplicationInfo applicationInfo = getApplicationInfo();
//        Log.i(TAG, "onCreate: " + applicationInfo.nativeLibraryDir);
//        if (!libraryExists) {
//            showToast(getString(R.string.library_not_found));
//        }else {
//            VersionInfo versionInfo = new VersionInfo();
//            int code = FaceEngine.getVersion(versionInfo);
//            Log.i(TAG, "onCreate: getVersion, code is: " + code + ", versionInfo is: " + versionInfo);
//        }
    }

    /**
     * 打开相机，RGB活体检测，人脸注册，人脸识别
     *
     * @param view
     */
    public void jumpToFaceRecognizeActivity(View view) {
        checkLibraryAndJump(RegisterAndRecognizeActivity.class);
    }

    /**
     * 批量注册和删除功能
     *
     * @param view
     */
    public void jumpToBatchRegisterActivity(View view) {
        checkLibraryAndJump(FaceManageActivity.class);
    }

    /**
     * 激活引擎
     *
     * @param view
     */
    public void activeEngine(final View view) {
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
            return;
        }
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
                int activeCode = FaceEngine.activeOnline(ChooseFunctionActivity.this, Constants.APP_ID, Constants.SDK_KEY);
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
                        int res = FaceEngine.getActiveFileInfo(ChooseFunctionActivity.this, activeFileInfo);
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

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                activeEngine(null);
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    void checkLibraryAndJump(Class activityClass) {
        if (!libraryExists) {
            showToast(getString(R.string.library_not_found));
            return;
        }
        startActivity(new Intent(this, activityClass));
    }


//    public void chooseDetectDegree(View view) {
//        if (chooseDetectDegreeDialog == null) {
//            chooseDetectDegreeDialog = new ChooseDetectDegreeDialog();
//        }
//        if (chooseDetectDegreeDialog.isAdded()) {
//            chooseDetectDegreeDialog.dismiss();
//        }
//        chooseDetectDegreeDialog.show(getSupportFragmentManager(), ChooseDetectDegreeDialog.class.getSimpleName());
//    }

//    public void permissionDegreeDialog() {
//        if (permissionDegreeDialog == null) {
//            permissionDegreeDialog = new PermissionDegreeDialog();
//            permissionDegreeDialog.setCallback(new PermissionDegreeDialog.Callback() {
//                @Override
//                public void onRefuse(boolean refuse) {
//                    if (refuse) {
//                        finish();
//                    }
//                }
//            });
//        }
//        if (permissionDegreeDialog.isAdded()) {
//            permissionDegreeDialog.dismiss();
//        }
//        permissionDegreeDialog.show(getSupportFragmentManager(), PermissionDegreeDialog.class.getSimpleName());
//    }
}
