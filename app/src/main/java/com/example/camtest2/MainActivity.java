package com.example.camtest2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.hardware.Camera;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private int cameraId = -1;

    SurfaceHolder.Callback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check cam perms

        try {

            // perms
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, 1);
                }

            }

            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Log.e("TAG", "Cannot access the camera.", e);
        }

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
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
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
                    e.printStackTrace(); // TODO: értelmesebben
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                try {
                    surfaceSizeChanged(holder, format, width, height);
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO: értelmesebben
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                // The surface has been destroyed, so we can return.
                camera.stopPreview();
            }

            public void surfaceSizeChanged(SurfaceHolder surfaceHolder, int format, int width, int height) throws IOException {
                // The size of the surface has changed, so we must reconfigure it.
                surfaceHolder.setFormat(format);
                camera.setPreviewDisplay(surfaceHolder);
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
                        ContentValues contentValues = new ContentValues();
                        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(
                                contentResolver,
                                BitmapFactory.decodeByteArray(data, 0, data.length),
                                "",
                                "")
                        );
                        if(uri != null) {
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
    protected void onDestroy() {
        super.onDestroy();
        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(callback);
        }
        if (camera != null) {
            camera.release();
        }
    }
}