package com.karthiknr.visionid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Karthik on 03-Jan-16.
 */
public class ProcessImageActivity {

    private static final String TAG = "ProcessImageActivity";

    public Context context;

    public static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "VisionID";
    protected String _path;

    public ProcessImageActivity(Context context){

        this.context=context;
        _path = PATH + "/img.jpg";
    }

    protected void ProcessImage(Bitmap bitmap, boolean isCaptured)
    {
        if(isCaptured) {
            bitmap=rotateImage(bitmap);
        }
        new com.karthiknr.visionid.RecogActivity(context.getAssets(),context).Recognize(bitmap);
    }

    protected Bitmap rotateImage(Bitmap bitmap)
    {
        Log.v(TAG, "Adjusting Rotation");
        // Rotation based on orientation
        try {
            ExifInterface exif = new ExifInterface(_path);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orientation: " + exifOrientation);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tesseract
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        return bitmap;
    }


}
