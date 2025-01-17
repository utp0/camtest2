package com.example.camtest2;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;

import androidx.annotation.NonNull;

public class CameraHandler {
    private Context context;
    private Camera camera;
    private int cameraId = -1;

    /**
     * @param context Context to use for services etc.
     */
    public CameraHandler(@NonNull Context context) {
        this.context = context;
    }

    private int getAvailableCameraId() {  // TODO: choosing
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
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

    public void openCamera(int cameraId) {
        this.camera = Camera.open(cameraId);
    }

    public void autoOpenCamera() {
        this.releaseCamera();
        this.openCamera(this.getAvailableCameraId());
    }

    public Camera getCamera() {
        if (camera == null) throw new IllegalStateException("A camera is not open.");
        return this.camera;
    }

    /**
     * Stops preview, releases camera, sets this.camera to null
     */
    public void releaseCamera() {
        if(this.camera != null) {
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }
}
