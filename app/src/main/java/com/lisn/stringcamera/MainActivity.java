package com.lisn.stringcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.lisn.rxpermissionlibrary.permissions.RxPermissions;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();

// TODO: 2020/6/3   OneFragment 使用camera1实现 ，TowFragment使用camera2实现
        getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.container, OneFragment.newInstance())
                .replace(R.id.container, TowFragment.newInstance())
                .commit();

    }

    private void getPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        Observable<Boolean> request = rxPermissions.request(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        request.subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (!aBoolean) {
                    Log.e("---", "accept: 请开启相关权限");
                    Toast.makeText(MainActivity.this, "请开启相关权限", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
