package com.lisn.stringcamera.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Arrays;

/**
 * @author : lishan
 * @e-mail : cnlishan@163.com
 * @date : 2020/6/3 11:04 AM
 * @desc :
 */
public class StringCameraProvider {
    private static final String TAG = "StringCameraProvider";
    private Handler mCameraHandler;
    private Context mContext;
    private Size previewSize;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private HandlerThread mCameraThread;

    public StringCameraProvider(Context context) {
        this.mContext = context;
        startCameraThread();
        startBackgroundThread();
    }

    private void startCameraThread() {
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
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void openCamera(int width, int height, ImageReader.OnImageAvailableListener mOnImageAvailableListener) {

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
                        previewSize = CameraUtil.getOptimalSize(map.getOutputSizes(ImageReader.class), width, height);
                        mCameraId = cameraId;

                        Log.e(TAG, "openCamera: mCameraId=" + mCameraId);
                        Log.e(TAG, "openCamera: previewSize=" + previewSize);

                    }
                }
            }

            mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.YUV_420_888, /*maxImages*/2);

            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

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
            try {

                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //如果需要多个surface可以add多个
                mPreviewBuilder.addTarget(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), mStateCallBack, mCameraHandler);

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
        stopBackgroundThread();
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

}
