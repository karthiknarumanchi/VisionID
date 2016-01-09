package com.karthiknr.visionid;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/VisionID/";

    private static final String TAG = "MainActivity";

    protected static TextView _field;
    protected static ImageView _image;

    protected String _path;

    protected static ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final File myDir = new File(PATH);
        if (!myDir.mkdirs()) {
            Log.v(TAG,"Make directory failed");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        _field = (TextView) findViewById(R.id.field);
        _image = (ImageView) findViewById(R.id.imageView);

        progress = new ProgressDialog(this);
        progress.setTitle("Please Wait");
        progress.setMessage("Processing Image and Recognizing");
        progress.setCancelable(false);

        _path = PATH + "/img.jpg";
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
            Log.v(TAG, "Starting Camera");
            if(_image.getVisibility()== View.VISIBLE) {
                _image.setVisibility(View.INVISIBLE);
            }
            _field.setText("");
            startCameraActivity();
        } else if (id == R.id.nav_gallery) {
            Log.v(TAG, "Starting Image Chooser");
            if(_image.getVisibility()== View.VISIBLE) {
                _image.setVisibility(View.INVISIBLE);
            }
            _field.setText("");
            startGalleryActivity();
        } else if (id == R.id.nav_info) {
            Toast.makeText(getApplicationContext(), "Karthik Narumanchi,2016", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                    br.append(recog.getTitle() + ": " + recog.getConfidence() * 100 + "\n");
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
