package com.example.camtest2;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class ImageManipulation {
    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap resizedBitmap = Bitmap.createBitmap(source, 0, 0, width, height, matrix, true);

        return resizedBitmap;
    }
}
