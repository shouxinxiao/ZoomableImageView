package com.sean.zoomableimageview;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

/**
 * Created by Sean on 2017/5/20.
 */

public class ShowImageViewActivity extends AppCompatActivity {
    public static final String INTENT_KEY = "imageurl";
    ZoomableImageView mSmoothImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTitle();
        setContentView(R.layout.activity_show_layout);
        String imageurl = getIntent().getStringExtra(INTENT_KEY);
        mSmoothImageView = (ZoomableImageView) findViewById(R.id.smoothimageview);
        /**加载本地图片*/
//        getResources().getDrawable(R.drawable.timg1);
//        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.timg1);
//        mSmoothImageView.setImageBitmap(bitmap);
        /**通过Glide加载网络图片*/
        Glide.with(this).load(imageurl).asBitmap().into(target);
        mSmoothImageView.setOnImageTouchedListener(new OnImageTouchedListener() {
            @Override
            public void onImageTouched() {
                ShowImageViewActivity.this.finish();
            }
        });
    }

    private SimpleTarget target = new SimpleTarget<Bitmap>() {
        @Override
        public void onResourceReady(Bitmap bitmap, GlideAnimation glideAnimation) {
            //图片加载完成
            mSmoothImageView.setImageBitmap(bitmap);
        }
    };

    private void initTitle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // 根据SDK版本动态设置title布局高度
            // 沉浸式状态栏设置 ，透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }
}
