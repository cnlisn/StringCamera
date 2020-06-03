package com.lisn.stringcamera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lisn.stringcamera.Utils.Camera2Provider;
import com.lisn.stringcamera.Utils.StringCameraProvider;
import com.lisn.stringcamera.Utils.Utils;


/**
 * A simple {@link Fragment} subclass.
 */
public class TowFragment extends Fragment {

    private static final String TAG = "TowFragment";
    private TextView text_view;
    private int MAX_PREVIEW_WIDTH = 640;
    private int MAX_PREVIEW_HEIGHT = 480;

    private AutoFitTextureView aftv;
    private Activity mContext;
    private Camera2Provider camera2Provider;
    private StringCameraProvider stringCameraProvider;


    public static TowFragment newInstance() {
        return new TowFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tow, container, false);
    }

    // TODO: 2020/6/3  Camera2ProviderFlag=true camera2相机预览 ，false 使用camera2+ImageReader实现字符相机
    boolean Camera2ProviderFlag = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = this.getActivity();
        text_view = view.findViewById(R.id.text_view);
        aftv = view.findViewById(R.id.AFTV);
        if (Camera2ProviderFlag) {
            camera2Provider = new Camera2Provider(mContext);
        } else {
            stringCameraProvider = new StringCameraProvider(mContext);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Camera2ProviderFlag) {
            if (camera2Provider != null) {
                camera2Provider.initTexture(aftv);
            }
            aftv.setVisibility(View.VISIBLE);
        } else {
            aftv.setVisibility(View.GONE);
            if (stringCameraProvider != null) {
                stringCameraProvider.openCamera(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT, mOnImageAvailableListener);
            }
        }
        Log.e(TAG, "onResume: ");
    }

    @Override
    public void onPause() {
        super.onPause();

        if (Camera2ProviderFlag) {
            if (camera2Provider != null) {
                camera2Provider.closeCamera();
            }
        } else {
            if (stringCameraProvider != null) {
                stringCameraProvider.closeCamera();
            }
        }
        Log.e(TAG, "onPause: ");
    }

    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            final String s = Utils.yuv2string(reader);
            if (s != null && text_view != null) {
                text_view.post(new Runnable() {
                    @Override
                    public void run() {
                        if (text_view != null) {
                            text_view.setText(s);
                        }
                    }
                });
            }
        }
    };


}
