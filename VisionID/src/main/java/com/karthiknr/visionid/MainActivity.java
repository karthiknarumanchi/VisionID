/* Copyright 2015 Karthik Narumanchi

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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/VisionID/";

    private static final String TAG = "MainActivity";

    protected static TextView _field;
    protected static ImageView _image;

    protected Button shutter_button;
    protected Button gallery_button;

    protected String _path;
    protected static final String PHOTO_TAKEN = "photo_taken";
    protected boolean _taken;


    protected static ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final File myDir = new File(PATH);
        if (!myDir.mkdirs()) {
            Log.v(TAG,"Make directory failed");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        _field = (TextView) findViewById(R.id.textView);
        _image = (ImageView) findViewById(R.id.imageView);

        shutter_button = (Button) findViewById(R.id.shutter_button);
        gallery_button = (Button) findViewById(R.id.gallery_button);

        shutter_button.setOnClickListener(this);
        gallery_button.setOnClickListener(this);

        progress = new ProgressDialog(this);
        progress.setTitle("Please Wait");
        progress.setMessage("Processing Image and Recognizing");
        progress.setCancelable(false);

        _path = PATH + "/img.jpg";
    }

    /*@Override
    public void onBackPressed() {
        super.onBackPressed();
    }*/

    @Override
    public void onClick(View v) {
        if(v == shutter_button) {
            _field.setText("");
            _image.setVisibility(View.INVISIBLE);
            Log.v(TAG, "Starting Camera app");
            startCameraActivity();
        }
        if(v == gallery_button) {
            _field.setText("");
            _image.setVisibility(View.INVISIBLE);
            Log.v(TAG, "Starting Gallery Activity");
            startGalleryActivity();
        }
        //Toast.makeText(getApplicationContext(), "Karthik Narumanchi,2016", Toast.LENGTH_SHORT).show();
    }

    /*
    Methods follow
     */

    protected void startGalleryActivity() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    protected void startCameraActivity() {
        File file = new File(_path);
        Uri outputFileUri = Uri.fromFile(file);
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "resultCode: " + resultCode);
        if(requestCode == 1)//Gallery Request Code
        {
            if (resultCode == RESULT_OK) {
                onPhotoChosen(data);
            } else {
                Log.v(TAG, "User cancelled");
            }
        }
        else if(requestCode == 0)//Camera Request Code
        {
            if (resultCode == RESULT_OK) {
                onPhotoTaken();
            } else {
                Log.v(TAG, "User cancelled");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(MainActivity.PHOTO_TAKEN, _taken);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onRestoreInstanceState()");
        if (savedInstanceState.getBoolean(MainActivity.PHOTO_TAKEN)) {
            _taken = true;
            onPhotoTaken();
        }
    }

    protected void onPhotoChosen(Intent data) {
        Uri uri = data.getData();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            progress.show();
            new com.karthiknr.visionid.ProcessImageActivity(this).ProcessImage(bitmap,false);//Photo not captured
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onPhotoTaken() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2; //Increase for decrease in quality. 1 is for zero sampling,4 results in image res = 1/16
        Bitmap bitmap = BitmapFactory.decodeFile(_path, options);
        progress.show();
        new com.karthiknr.visionid.ProcessImageActivity(this).ProcessImage(bitmap,true);//Photo captured
    }

    public static void SetText(final List<Classifier.Recognition> results)
    {
        StringBuilder br = new StringBuilder();
        if (results != null) {
            progress.dismiss();
            if(_field.getText().toString().length() ==0)
            {
                for (final Classifier.Recognition recog : results) {
                    br.append(recog.getTitle().toUpperCase() + "\n");
                    break;
                }
                _field.setText(br.toString());
                Log.v(TAG, "Text Set to Field");
            }
        }
        else {
            progress.dismiss();
            Log.v(TAG, "Text NOT Set to Field");
            _field.setText("Unable to recognize");
        }

    }

    public static void SetImage(Bitmap bmp)
    {
        if(_image.getVisibility()== View.INVISIBLE) {
            _image.setImageBitmap(bmp);
            _image.setVisibility(View.VISIBLE);
        }
    }
}
