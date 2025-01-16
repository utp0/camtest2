package com.example.camtest2;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private int cameraId = -1;

    SurfaceHolder.Callback callback;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    public int realRotation = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (Math.abs(x) > Math.abs(y)) {  // landscape
                if (x > 0) {  // 90
                    realRotation = 1;
                } else {  // 270
                    realRotation = 3;
                }
            } else if (Math.abs(x) < Math.abs(y)) {  // portrait
                if (y > 0) {  // 0
                    realRotation = 0;
                } else {  // 180
                    realRotation = 2;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private int getAvailableCameraId() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameras = manager.getCameraIdList();
            for (String camera : cameras) {
                if (!camera.equals("front")) {
                    return Integer.parseInt(camera);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // TODO: értelmesebben
        }
        return -1;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);


        // permissions
        try {
            PermissionsHelper.makeSurePerms(this);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Cannot access the camera.", e);
        }

        // sensors
        this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // set the view
        setContentView(R.layout.activity_main);

        // Find the SurfaceView and request a Surface from it.
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        // Open the camera. Requested from the user if necessary.
        if (cameraId == -1) {
            cameraId = getAvailableCameraId();
        }
        camera = Camera.open(cameraId);

        // Set up a SurfaceHolder callback to be notified when the surface is available.
        SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                // The surface has been created, assign it to the surface holder.

                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    throw new RuntimeException(e);  // TODO: értelmesebben
                }
                try {
                    // Now that we have a surface holder, we can start using it.
                    camera.startPreview();
                } catch (Exception e) {

                    Toast.makeText(MainActivity.this, "Error starting preview:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    throw new RuntimeException(e); // TODO: értelmesebben
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                try {
                    // The size of the surface has changed, so we must reconfigure it.

                    surfaceHolder.setFormat(format);
                    camera.setPreviewDisplay(surfaceHolder);

                    // set correct aspect ratio
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                    int screenWidth = displayMetrics.widthPixels;
                    int screenHeight = displayMetrics.heightPixels;
                    Camera.Size previewSize = camera.getParameters().getPreviewSize();
                    int currentRotation = getWindowManager().getDefaultDisplay().getRotation();
                    ViewGroup.LayoutParams layoutParams;
                    switch (currentRotation) {
                        // portrait
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            layoutParams = new ViewGroup.LayoutParams(previewSize.height, previewSize.width);
                            camera.setDisplayOrientation(90 + (currentRotation * 90));
                            break;
                        // landscape
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            layoutParams = new ViewGroup.LayoutParams(previewSize.width, previewSize.height);
                            camera.setDisplayOrientation((currentRotation == 3 ? 180 : 0));
                            break;
                        default:
                            // elvileg soha
                            layoutParams = new ViewGroup.LayoutParams(64, 64);
                            break;
                    }
                    surfaceView.setLayoutParams(layoutParams);
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO: értelmesebben
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                // The surface has been destroyed, so we can return.
                camera.stopPreview();
            }
        };
        surfaceHolder.addCallback(callback);

        FloatingActionButton btnTakePicture = findViewById(R.id.cameraButton);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                };
                Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                };
                Camera.PictureCallback postviewCallback = new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                };
                Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        ContentResolver contentResolver = getContentResolver();

                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        float angle;
                        switch (realRotation) {
                            // portrait
                            case 0:
                                angle = 90.0f;
                                break;
                            case 2:
                                angle = 270.0f;
                                break;
                            // landscape
                            case 1:
                                angle = 0.0f;
                                break;
                            case 3:
                                angle = 180.0f;
                                break;
                            default:
                                // elvileg soha
                                angle = 0f;
                                break;
                        }
                        bitmap = ImageManipulation.rotateBitmap(bitmap, angle);
                        Log.d("ASD", String.valueOf(angle));
                        Uri uri = Uri.parse(
                                MediaStore.Images.Media.insertImage(
                                        contentResolver,
                                        bitmap,
                                        "",
                                        ""
                                )
                        );
                        if (uri != null) {
                            camera.stopPreview();
                            camera.startPreview();
                        }
                        Log.d("IMAGE OUT", String.valueOf(uri));
                    }
                };
                camera.takePicture(shutterCallback, rawCallback, postviewCallback, jpegCallback);
            }
        });
    }

    @Override
    protected void onStart() {
        Log.d(this.getClass().getName(), "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(this.getClass().getName(), "onStop");
        super.onStop();
        camera.stopPreview();
    }

    @Override
    protected void onPostResume() {
        Log.d(this.getClass().getName(), "onPostResume");
        super.onPostResume();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        Log.d(this.getClass().getName(), "onPostCreate");
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        Log.d(this.getClass().getName(), "onPause");
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        Log.d(this.getClass().getName(), "onResume");
        super.onResume();
        camera.startPreview();
        mSensorManager.registerListener(this, mAccelerometer, 200000);
    }

    @Override
    protected void onDestroy() {
        Log.d(this.getClass().getName(), "onDestroy");
        super.onDestroy();
        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(callback);
        }
        if (camera != null) {
            camera.release();
        }
    }


}