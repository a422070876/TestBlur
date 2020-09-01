package com.hyq.hm.testblur;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private Handler handler;
    private HandlerThread thread;

    private EGLUtils mEglUtils;

    private GLRenderer renderer;
    private GLFrameBuffer frameBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEglUtils = new EGLUtils();

        renderer = new GLRenderer();
        frameBuffer = new GLFrameBuffer();


        thread = new HandlerThread("testThread");
        thread.start();
        handler = new Handler(thread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 100:
                        renderer.setSigma(msg.arg1);
                        renderer.gaussianWeights();
                        break;
                    case 200:
                        renderer.setBlurRadius(msg.arg1);
                        renderer.gaussianWeights();
                        break;
                    case 300:
                        frameBuffer.drawFrame((Bitmap) msg.obj,msg.arg1,msg.arg2);
                        renderer.setScaleSize(msg.arg1,msg.arg2);
                        break;
                }
                renderer.drawFrame(frameBuffer.getTexture());
                mEglUtils.swap();
            }
        };

        final Bitmap bitmap = fileBitmap(getResources(),R.drawable.ic_jn);

        SurfaceView surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mEglUtils.initEGL(holder.getSurface());
                        frameBuffer.initShader();
                        renderer.initShader(MainActivity.this);
                        frameBuffer.drawFrame(bitmap,bitmap.getWidth(),bitmap.getHeight());
                        renderer.setScaleSize(bitmap.getWidth(),bitmap.getHeight());
                        renderer.setScreenSize(width,height);
                        renderer.gaussianWeights();
                        renderer.drawFrame(frameBuffer.getTexture());
                        mEglUtils.swap();
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        final TextView sigmaTextView = findViewById(R.id.sigma_text_view);
        final TextView radiusTextView = findViewById(R.id.radius_text_view);
        final TextView sizeTextView = findViewById(R.id.size_text_view);
        sigmaTextView.setText("sigma:"+3);
        radiusTextView.setText("radius:"+2);
        sizeTextView.setText("size:"+bitmap.getWidth()+"x"+bitmap.getHeight());

        SeekBar sigmaSeekBar = findViewById(R.id.sigma_seek_bar);
        sigmaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if(fromUser){
                    sigmaTextView.setText("sigma:"+progress);
                    Message msg = new Message();
                    msg.arg1 = progress;
                    msg.what = 100;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SeekBar radiusSeekBar = findViewById(R.id.radius_seek_bar);
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if(fromUser){
                    radiusTextView.setText("radius:"+progress);
                    Message msg = new Message();
                    msg.arg1 = progress;
                    msg.what = 200;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        SeekBar sizeSeekBar = findViewById(R.id.size_seek_bar);
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if(fromUser){

                    int width = bitmap.getWidth()*(progress+5)/100;
                    int height = bitmap.getHeight()*(progress+5)/100;
                    sizeTextView.setText("size:"+width+"x"+height);
                    Message msg = new Message();
                    msg.arg1 = width;
                    msg.arg2 = height;
                    msg.obj = bitmap;
                    msg.what = 300;
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public Bitmap fileBitmap(Resources res, int id) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res,id,opts);
        opts.inSampleSize = calculateInSampleSize(opts,500,500);
        opts.inJustDecodeBounds = false;

        return BitmapFactory.decodeResource(res,id,opts);
    }

    public int calculateInSampleSize(BitmapFactory.Options op, int reqWidth,
                                     int reqHeight) {
        int originalWidth = op.outWidth;
        int originalHeight = op.outHeight;

        int inSampleSize = 1;
        if(originalWidth > originalHeight){
            int halfWidth = originalWidth;
            while (halfWidth > reqWidth) {
                inSampleSize *= 2;
                halfWidth = halfWidth/2;
            }
        }else{
            int halfHeight = originalHeight;
            while (halfHeight  > reqHeight) {
                inSampleSize *= 2;
                halfHeight = halfHeight/2;
            }
        }
        return inSampleSize;
    }

}
