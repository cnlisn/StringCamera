package com.lisn.stringcamera.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Arrays;

public class Camera2Provider {
    private static final String TAG = "Camera2Provider";
    private Activity mContext;
    private String mCameraId;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private TextureView mTextureView;
    private CaptureRequest.Builder mPreviewBuilder;
    private Size previewSize;
    private HandlerThread mCameraThread;

    public Camera2Provider(Activity mContext) {
        this.mContext = mContext;
        startCameraThread();
    }

    private void startCameraThread() {
        //创建了一个Thread来供Camera运行使用，
        // 使用HandlerThread而不使用Thread是因为HandlerThread给我们创建了Looper，不用我们自己创建了。
        mCameraThread = new HandlerThread("cameraHandler");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

    }

    private void stopCameraThread() {
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            try {
                mCameraThread.join();
                mCameraThread = null;
                mCameraHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置预览view
     *
     * @param textureView 需要预览的TextureView
     */
    public void initTexture(TextureView textureView) {
        mTextureView = textureView;
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera(width, height);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /**
     * surface ready的时候开启Camera
     *
     * @param width  surface的宽
     * @param height surface的高
     */
    public void openCamera(int width, int height) {
        Log.e(TAG, "openCamera: width=" + width + " height=" + height);
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                //描述相机设备的属性类
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                //获取是前置还是后置摄像头
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                //使用后置摄像头 LENS_FACING_BACK,使用前置摄像头 LENS_FACING_FRONT
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        previewSize = CameraUtil.getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                        mCameraId = cameraId;
                        Log.e(TAG, "openCamera: mCameraId=" + mCameraId);
                        Log.e(TAG, "openCamera: previewSize=" + previewSize);
                    }
                }
            }

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(mContext, "请打开相机权限", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException r) {
        }
    }

    /**
     * 状态回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //如果需要多个surface可以add多个
                mPreviewBuilder.addTarget(previewSurface);
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface), mStateCallBack, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
        }
    };

    private CameraCaptureSession.StateCallback mStateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            CaptureRequest request = mPreviewBuilder.build();
            try {
                //开启获取Image，repeat模式
                session.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    };

    /**
     * 记得关掉Camera
     */
    public void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }

        stopCameraThread();
    }


}