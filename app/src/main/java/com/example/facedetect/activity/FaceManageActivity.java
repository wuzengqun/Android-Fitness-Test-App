package com.example.facedetect.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import androidx.core.app.ActivityCompat;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Scroller;
import android.widget.TextView;

import com.example.facedetect.R;
import com.example.facedetect.widget.ProgressDialog;
import com.example.facedetect.faceserver.FaceServer;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.LinearLayout;

import android.view.MotionEvent;
import android.media.ExifInterface;
import java.io.IOException;
import android.graphics.Matrix;

/**
 * 批量注册页面
 */
public class FaceManageActivity extends BaseActivity {
    //注册图所在的目录
    //定义存储人脸数据的根目录。
    private String rootDir;
    private String REGISTER_DIR;
    private String REGISTER_FAILED_DIR;

    private ExecutorService executorService;//单线程执行器，用于后台处理任务

    private TextView tvNotificationRegisterResult;//一个 TextView，用于显示注册结果

    ProgressDialog progressDialog = null;//一个 ProgressDialog，用于显示进度
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_manage);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        executorService = Executors.newSingleThreadExecutor();
        tvNotificationRegisterResult = findViewById(R.id.notification_register_result);
        progressDialog = new ProgressDialog(this);
        FaceServer.getInstance().init(this);

        // 阻止侧滑事件
        LinearLayout yourView = findViewById(R.id.manage_layout);
        // 设置 OnTouchListener
        yourView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 处理触摸事件，阻止返回行为
                return true; // 返回 true 表示事件被处理，不传递给下一个处理者
            }
        });

          // 获取内部存储路径,这种是只能用此电脑连接手机才能看到那个目录，但是安全性高，懂技术的才可以操作。说实话这个比较方便，手机保存图片时名字不好整
          // 这个就在此ide按这个操作：view/tool windows/device Explorer，文件位于：/data/data/com.example.facedetect/files/register,
          // 将需要注册的图片命名好之后导入此文件夹即可。
        rootDir = getFilesDir().getAbsolutePath() + File.separator + "arcfacedemo";
        REGISTER_DIR = rootDir + File.separator + "register";
        REGISTER_FAILED_DIR = rootDir + File.separator + "failed";
        // 创建目录
        new File(REGISTER_DIR).mkdirs();
        new File(REGISTER_FAILED_DIR).mkdirs();

        // 使用 getExternalFilesDir() 方法，它会返回一个路径，用户可以通过手机文件管理器访问:/Android/data/com.example.facedetect/files/arcfacedemo/register
//        rootDir = getExternalFilesDir(null).getAbsolutePath() + File.separator + "arcfacedemo";
//        REGISTER_DIR = rootDir + File.separator + "register";
//        REGISTER_FAILED_DIR = rootDir + File.separator + "failed";
//        // 创建目录
//        new File(REGISTER_DIR).mkdirs();
//        new File(REGISTER_FAILED_DIR).mkdirs();
    }

    @Override
    protected void onDestroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    //批量人脸注册按钮回调
    public void batchrigester(View view) {
        if (checkPermissions(NEEDED_PERMISSIONS)) {
            doRegister();
        } else {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        }
    }

    //这里会对图像做处理，并且保证读取整个文件夹的图片，以文件名来注册人脸信息，最终调用faceserver中的registerBgr24。
    private void doRegister() {
        File dir = new File(REGISTER_DIR);
        if (!dir.exists()) {
            showToast(getString(R.string.batch_process_path_is_not_exists, REGISTER_DIR));
            return;
        }
        if (!dir.isDirectory()) {
            showToast(getString(R.string.batch_process_path_is_not_dir, REGISTER_DIR));
            return;
        }

        final File[] jpgFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(FaceServer.IMG_SUFFIX);
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final int totalCount = jpgFiles.length;
                int successCount = 0;

                runOnUiThread(() -> {
                    progressDialog.setMaxProgress(totalCount);
                    progressDialog.show();
                    tvNotificationRegisterResult.setText("");
                    tvNotificationRegisterResult.append(getString(R.string.batch_process_processing_please_wait));
                });

                for (int i = 0; i < totalCount; i++) {
                    final int finalI = i;
                    runOnUiThread(() -> {
                        if (progressDialog != null) {
                            progressDialog.refreshProgress(finalI);
                        }
                    });

                    final File jpgFile = jpgFiles[i];
                    Bitmap bitmap = getCorrectlyOrientedImage(jpgFile.getAbsolutePath());

                    if (bitmap == null) {
                        moveToFailedDirectory(jpgFile);
                        continue;
                    }

                    // 处理图像
                    bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
                    if (bitmap == null) {
                        moveToFailedDirectory(jpgFile);
                        continue;
                    }

                    byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), ArcSoftImageFormat.BGR24);
                    int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
                    if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            tvNotificationRegisterResult.append("");
                        });
                        return;
                    }

                    String name = jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf("."));
                    boolean success = FaceServer.getInstance().registerBgr24(FaceManageActivity.this, bgr24, bitmap.getWidth(), bitmap.getHeight(), name);
                    if (!success) {
                        moveToFailedDirectory(jpgFile);
                    } else {
                        successCount++;
                    }
                }

                final int finalSuccessCount = successCount;
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    tvNotificationRegisterResult.append(getString(R.string.batch_process_finished_info, totalCount, finalSuccessCount, totalCount - finalSuccessCount, REGISTER_FAILED_DIR));
                });

                Log.i(FaceManageActivity.class.getSimpleName(), "run: " + executorService.isShutdown());
            }
        });
    }

    private Bitmap getCorrectlyOrientedImage(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.d("ExifOrientation", "Orientation: " + orientation);

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(bitmap, 270);
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: // 水平翻转
                    return flipImage(bitmap, true);
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: // 垂直翻转
                    return flipImage(bitmap, false);
                case 5: // 水平翻转并旋转90度
                    bitmap = flipImage(bitmap, true);
                    return rotateImage(bitmap, 90);
                default:
                    return bitmap; // No rotation needed
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Handle the error
        }
    }

    private Bitmap flipImage(Bitmap src, boolean horizontal) {
        Matrix matrix = new Matrix();
        if (horizontal) {
            matrix.preScale(-1.0f, 1.0f); // 水平翻转
        } else {
            matrix.preScale(1.0f, -1.0f); // 垂直翻转
        }
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }


    private Bitmap rotateImage(Bitmap src, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    private void moveToFailedDirectory(File jpgFile) {
        File failedFile = new File(REGISTER_FAILED_DIR + File.separator + jpgFile.getName());
        if (!failedFile.getParentFile().exists()) {
            failedFile.getParentFile().mkdirs();
        }
        jpgFile.renameTo(failedFile);
    }


    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                doRegister();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    //清空人脸信息库按钮回调函数
    public void clearFaces(View view) {
        int faceNum = FaceServer.getInstance().getFaceNumber(this);
        if (faceNum == 0) {
            showToast(getString(R.string.batch_process_no_face_need_to_delete));
        } else {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.batch_process_notification)
                    .setMessage(getString(R.string.batch_process_confirm_delete, faceNum))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            int deleteCount = FaceServer.getInstance().clearAllFaces(FaceManageActivity.this);
                            showToast(deleteCount + " faces cleared!");
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            dialog.show();
        }
    }

    //查看人脸信息库按钮回调函数
    public void lookFaces(View view) {
            Intent intent = new Intent();
            intent.setClass(FaceManageActivity.this,ViewAndManageFaceInfo.class);

            startActivity(intent);
    }

    //单人人脸注册
    public void facerigester(View view) {
        startFaceRecord(view);
    }

    public void startFaceRecord(View view) {
        InputDialogFragments dialogFragment = new InputDialogFragments();
        dialogFragment.setOnInputListener(new InputDialogFragments.OnInputListener() {
            @Override
            public void onInput(String input, String input_class, String input_card) {
                Intent intent = new Intent(FaceManageActivity.this, RegisterAndRecognizeActivity.class);
                intent.putExtra("input_data", input);
                intent.putExtra("input_class", input_class);
                intent.putExtra("input_card", input_card);
                startActivity(intent);
            }
        });
        dialogFragment.show(getSupportFragmentManager(), "input_dialog");
    }

    //返回功能选择页面按键，因为返回的bug没有排查出来，最好是所有页面都使用按键返回
    public void returnfunselect(View view) {
        Intent intent = new Intent();
        intent.setClass(FaceManageActivity.this,FuncSelectFaceRecogActivity.class);

        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    //这里是禁用了侧滑返回功能，因为有bug。
    @Override
    public void onBackPressed() {
        // 不执行返回逻辑
    }
}
