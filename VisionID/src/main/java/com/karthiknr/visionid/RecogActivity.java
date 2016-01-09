/* Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.karthiknr.visionid;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import junit.framework.Assert;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Karthik on 03-Jan-16.
 */

/**
 * Class that recognizes Bitmap with Tensorflow.
 */
public class RecogActivity {

    private static final String TAG = "RecogActivity";

    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/VisionID/";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
          "file:///android_asset/imagenet_comp_graph_label_strings.txt";
    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final int MINIMUM_PREVIEW_SIZE = 320;


    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size previewSize;

  private final com.karthiknr.visionid.TensorflowClassifier tensorflow = new com.karthiknr.visionid.TensorflowClassifier();

  private int previewWidth = 0;
  private int previewHeight = 0;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;

  public Context context;

  public RecogActivity(final AssetManager assetManager,Context context) {
    this.context=context;
    tensorflow.initializeTensorflow(
        assetManager, MODEL_FILE, LABEL_FILE, NUM_CLASSES, INPUT_SIZE, IMAGE_MEAN);
  }

  protected void Recognize(Bitmap bmp)
  {
    Log.v(TAG, "Starting recognition");
      setUpCameraOutputs(bmp.getWidth(), bmp.getHeight());
      previewWidth=previewSize.getWidth();
      previewHeight=previewSize.getHeight();
      Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, previewWidth, previewWidth, true);
      rgbFrameBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true);
      croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
      drawResizedBitmap(rgbFrameBitmap, croppedBitmap);

      Log.v(TAG, "Saving resized final bitmap");
      FileOutputStream out = null;
      try {
          out = new FileOutputStream(PATH+"/preview.png");
          croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
      } catch (Exception e) {
          e.printStackTrace();
      } finally {
          try {
              if (out != null) {
                  out.close();
              }
          } catch (IOException e) {
              e.printStackTrace();
          }
      }

      final List<Classifier.Recognition> results = tensorflow.recognizeImage(croppedBitmap);

    Log.v(TAG,""+ results.size()+" results");
    for (final Classifier.Recognition result : results) {
      String finalresult=result.getTitle();
      Log.v(TAG,"Result: " + finalresult+"");
    }
      MainActivity.SetText(results);
      MainActivity.SetImage(bmp);
  }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);
/*
        // Rotate around the center if necessary.
        if (screenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(screenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }
*/
        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(
            final Size[] choices, final int width, final int height, final Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        final List<Size> bigEnough = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.getHeight() >= MINIMUM_PREVIEW_SIZE && option.getWidth() >= MINIMUM_PREVIEW_SIZE) {
                Log.v(TAG,"Adding size: " + option.getWidth() + "x" + option.getHeight());
                bigEnough.add(option);
            } else {
                Log.v(TAG,"Not adding size: " + option.getWidth() + "x" + option.getHeight());
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            Log.v(TAG,"Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            Log.v(TAG,"Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(final int width, final int height) {
        final CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                final Size largest =
                        Collections.max(
                                Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                                new CompareSizesByArea());

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                previewSize =
                        chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
                return;
            }
        } catch (final CameraAccessException e) {
            Log.v(TAG, "Exception!");
        } catch (final NullPointerException e) {
            Log.v(TAG, "Exception!");
        }
    }

}
