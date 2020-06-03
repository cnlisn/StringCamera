package com.lisn.stringcamera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lisn.stringcamera.Utils.Utils;

import java.io.IOException;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class OneFragment extends Fragment {


    private AutoFitTextureView aftv;
    private TextView text_view;
    private Camera mCamera;
    private int cameraId;

    public static OneFragment newInstance() {
        return new OneFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_one, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        aftv = view.findViewById(R.id.AFTV);
        text_view = view.findViewById(R.id.text_view);

    }

    @Override
    public void onResume() {
        super.onResume();
        aftv.setSurfaceTextureListener(mSurfaceTextureListener);
        Utils.startConvert(aftv,text_view);
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
            setCamParameters();
            startPreview(surface);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            releaseCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void openCamera() {
        cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCamera = Camera.open(cameraId);
    }


    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }
    }


    private void setCamParameters() {
        if (mCamera == null)
            return;
        Camera.Parameters params = mCamera.getParameters();
        List<Integer> rates = params.getSupportedPreviewFrameRates();

        int rotateDegree = getPreviewRotateDegree(getActivity());
        mCamera.setDisplayOrientation(rotateDegree);
    }


    private int getPreviewRotateDegree(Activity mContext) {
        int phoneDegree = 0;
        int result = 0;
        //获得手机方向
        int phoneRotate = mContext.getWindowManager().getDefaultDisplay().getOrientation();
        //得到手机的角度
        switch (phoneRotate) {
            case Surface.ROTATION_0:
                phoneDegree = 0;
                break;     //旋转90度
            case Surface.ROTATION_90:
                phoneDegree = 90;
                break;    //旋转0度
            case Surface.ROTATION_180:
                phoneDegree = 180;
                break;//旋转270
            case Surface.ROTATION_270:
                phoneDegree = 270;
                break;//旋转180
        }
        //分别计算前后置摄像头需要旋转的角度
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
            result = (cameraInfo.orientation + phoneDegree) % 360;
            result = (360 - result) % 360;
        } else {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
            result = (cameraInfo.orientation - phoneDegree + 360) % 360;
        }
        return result;
    }

    public void startPreview(SurfaceTexture surface) {
        try {
            //mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            int bufSize = previewSize.width * previewSize.height * 3 / 2;
            mCamera.addCallbackBuffer(new byte[bufSize]);
            mCamera.setPreviewCallbackWithBuffer(previewCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            camera.addCallbackBuffer(data);
        }
    };

}
