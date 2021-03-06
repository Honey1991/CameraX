package com.im2.xcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TextureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    TextView textView;
    PreviewView mCameraView;
    SurfaceHolder holder;
    SurfaceView surfaceView;
    Canvas canvas;
    Paint paint;
    int cameraHeight, cameraWidth, xOffset, yOffset, boxWidth, boxHeight;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Camera camera;

    /**
     *Responsible for converting the rotation degrees from CameraX into the one compatible with Firebase ML
     */

    private int degreesToFirebaseRotation(int degrees) {
        return 0;
    }


    /**
     * Starting Camera
     */
    void startCamera(){
        mCameraView = findViewById(R.id.previewView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                    adjustCameraFocus1();
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     *
     * Binding to camera
     */
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mCameraView.getSurfaceProvider());

        //Image Analysis Function
        //Set static size according to your device or write a dynamic function for it
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(720, 1488))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();


        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void analyze(@NonNull ImageProxy image) {
                //changing normal degrees into Firebase rotation

            }


        });
        camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis,preview);
    }

    private void adjustCameraFocus1() {
        float x = mCameraView.getWidth()/2;
        float y = (mCameraView.getHeight() - mCameraView.getHeight()/4);
        MeteringPoint meteringPoint1 = new SurfaceOrientedMeteringPointFactory(mCameraView.getWidth(), mCameraView.getHeight()).createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(meteringPoint1, FocusMeteringAction.FLAG_AF).setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();
        ListenableFuture<FocusMeteringResult> focusMeteringResultListenableFuture = camera.getCameraControl().startFocusAndMetering(action);

        focusMeteringResultListenableFuture.addListener(() -> {
            try {
                FocusMeteringResult result = focusMeteringResultListenableFuture.get();
                Log.d("isFocusSuccessful", ""+result.isFocusSuccessful());
                // process the result
            } catch (Exception e) {
            }
        } , ContextCompat.getMainExecutor(this));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture);

        //Start Camera
        startCamera();

        //Create the bounding box
        surfaceView = findViewById(R.id.overlay);
        surfaceView.setZOrderOnTop(true);
        holder = surfaceView.getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);
        holder.addCallback(this);

    }

    /**
     *
     * For drawing the rectangular box
     */
    private void DrawFocusRect(int color) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = mCameraView.getHeight();
        int width = mCameraView.getWidth();

        //cameraHeight = height;
        //cameraWidth = width;

        int left, right, top, bottom, diameter;

        diameter = width;
        if (height < width) {
            diameter = height;
        }

        int offset = (int) (0.05 * diameter);
        diameter -= offset;

        canvas = holder.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(5);

        left = width / 2 - diameter / 3;
        top = height / 2 - diameter / 3;
        right = width / 2 + diameter / 3;
        bottom = height / 2 + diameter / 3;

        xOffset = left;
        yOffset = top;
        boxHeight = bottom - top;
        boxWidth = right - left;
        //Changing the value of x in diameter/x will change the size of the box ; inversely proportionate to x
        canvas.drawRect(left, top, right, bottom, paint);
        holder.unlockCanvasAndPost(canvas);
    }

    /**
     * Callback functions for the surface Holder
     */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Drawing rectangle
        DrawFocusRect(Color.parseColor("#b3dabb"));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}