package com.heshicaihao.zxing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.heshicaihao.zxing.camera.CameraManager;
import com.heshicaihao.zxing.decode.CaptureActivityHandler;
import com.heshicaihao.zxing.decode.DecodeManager;
import com.heshicaihao.zxing.decode.InactivityTimer;
import com.heshicaihao.zxing.qr.R;
import com.heshicaihao.zxing.utils.ScreenUtils;
import com.heshicaihao.zxing.view.QrCodeFinderView;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 二维码扫描类。
 */
public class QrCodeActivity extends AppCompatActivity implements Callback, OnClickListener {

    public static final String INTENT_OUT_STRING_SCAN_RESULT = "scan_result";
    private static final String INTENT_IN_INT_SUPPORT_TYPE = "support_type";
    private static final int REQUEST_PERMISSIONS = 1;
    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
    private InactivityTimer mInactivityTimer;
    private QrCodeFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private View mLlFlashLight;
    private boolean mNeedFlashLightOpen = true;
    private ImageView mIvFlashLight;
    private TextView mTvFlashLightText;
    private ViewStub mSurfaceViewStub;
    private DecodeManager mDecodeManager = new DecodeManager();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(getResources().getColor(R.color.qr_code_white));
        }
        ScreenUtils.init(this);
        //适配安卓8.0版本增加了一个限制：如果是透明的Activity，则不能固定它的方向
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && isTranslucentOrFloating()) {
            boolean result = fixOrientation();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        initView();
        initData();
    }

    private void initView() {
        mIvFlashLight = (ImageView) findViewById(R.id.qr_code_iv_flash_light);
        mTvFlashLightText = (TextView) findViewById(R.id.qr_code_tv_flash_light);
        mQrCodeFinderView = (QrCodeFinderView) findViewById(R.id.qr_code_view_finder);
        mLlFlashLight = findViewById(R.id.qr_code_ll_flash_light);
        mSurfaceViewStub = (ViewStub) findViewById(R.id.qr_code_view_stub);
        findViewById(R.id.head_back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mHasSurface = false;
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                mIvFlashLight.setOnClickListener(QrCodeActivity.this);
            }
        }, 1000);
    }

    private void initData() {
        CameraManager.init();
        mInactivityTimer = new InactivityTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        }
    }

    private void initCamera() {
        if (null == mSurfaceView) {
            mSurfaceViewStub.setLayoutResource(R.layout.layout_surface_view);
            mSurfaceView = (SurfaceView) mSurfaceViewStub.inflate();
        }
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            try {
                mCaptureActivityHandler.quitSynchronously();
                mCaptureActivityHandler = null;
                mHasSurface = false;
                if (null != mSurfaceView) {
                    mSurfaceView.getHolder().removeCallback(this);
                }
                CameraManager.get().closeDriver();
            } catch (Exception e) {
                // 关闭摄像头失败的情况下,最好退出该Activity,否则下次初始化的时候会显示摄像头已占用.
                finish();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        findViewById(R.id.qr_code_view_background).setVisibility(View.VISIBLE);
        mQrCodeFinderView.setVisibility(View.GONE);
        mDecodeManager.showPermissionDeniedDialog(this);
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     */
    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            String resultString = result.getText();
            handleResult(resultString);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (!CameraManager.get().openDriver(surfaceHolder)) {
                showPermissionDeniedDialog();
                return;
            }
        } catch (IOException e) {
            // 基本不会出现相机不存在的情况
            Toast.makeText(this, getString(R.string.qr_code_camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            showPermissionDeniedDialog();
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        mLlFlashLight.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        turnFlashLightOff();
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this);
        }
    }

    private void restartPreview() {
        if (null != mCaptureActivityHandler) {
            try {
                mCaptureActivityHandler.restartPreviewAndDecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.qr_code_iv_flash_light) {
            if (mNeedFlashLightOpen) {
                turnFlashlightOn();
            } else {
                turnFlashLightOff();
            }
        }
    }

    private void turnFlashlightOn() {
        try {
            CameraManager.get().setFlashLight(true);
            mNeedFlashLightOpen = false;
            mTvFlashLightText.setText(getString(R.string.qr_code_close_flash_light));
            mIvFlashLight.setBackgroundResource(R.mipmap.flashlight_turn_off);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void turnFlashLightOff() {
        try {
            CameraManager.get().setFlashLight(false);
            mNeedFlashLightOpen = true;
            mTvFlashLightText.setText(getString(R.string.qr_code_open_flash_light));
            mIvFlashLight.setBackgroundResource(R.mipmap.flashlight_turn_on);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0) {
            int cameraPermission = grantResults[0];
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        REQUEST_PERMISSIONS);
            }
        }
    }

    private void handleResult(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            Intent it = new Intent();
            it.putExtra("data", resultString);
            setResult(RESULT_OK, it);
            finish();
//            mDecodeManager.showResultDialog(this, resultString, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                    restartPreview();
//                }
//            });
        }
    }


    private boolean isTranslucentOrFloating() {
        boolean isTranslucentOrFloating = false;
        try {
            int[] styleableRes = (int[]) Class.forName("com.android.internal.R$styleable")
                    .getField("Window")
                    .get(null);
            final TypedArray ta = obtainStyledAttributes(styleableRes);
            Method m = ActivityInfo.class.getMethod("isTranslucentOrFloating", TypedArray.class);
            m.setAccessible(true);
            isTranslucentOrFloating = (boolean) m.invoke(null, ta);
            m.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isTranslucentOrFloating;
    }

    private boolean fixOrientation() {
        try {
            Field field = Activity.class.getDeclaredField("mActivityInfo");
            field.setAccessible(true);
            ActivityInfo o = (ActivityInfo) field.get(this);
            o.screenOrientation = -1;
            field.setAccessible(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}