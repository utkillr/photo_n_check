package nosikov_k.photoncheck;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
    //constants for settings saving
    public int Flashlight = 0, GeoPosition = 0, Effect = 0;
    public final String FLASHLIGHT = "fl", GEOPOSITION = "gp", EFFECT = "eff";
    public static final String APP_PREFERENCES = "mySettings";
    private SharedPreferences mySettings;
    int CAMERA_SETTINGS = 0;

    //Camera preview
    SurfaceView surfaceView;
    SurfaceHolder holder;

    //Camera
    Camera camera = null;
    boolean cameraIsActive = true;

    //Buttons
    ImageButton PIC;
    ImageButton NEXT;

    //Location
    private LocationManager locationManager;
    String globalLocation = null;
    String globalPlace = null;
    double globalLongitude;
    double globalLatitude;

    //Views for place
    TextView crdsView, placeView;

    //Images
    ImageView imgPic, imgFL, imgPos, imgEff;

    //Threads
    Thread takePictureThread = null;
    Thread nextPictureThread = null;

    /*Creating app*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        placeView = (TextView) findViewById(R.id.placeView);
        crdsView = (TextView) findViewById(R.id.crdsView);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        PIC = (ImageButton) findViewById(R.id.btnTakePicture);
        NEXT = (ImageButton) findViewById(R.id.btnNext);
        imgPic = (ImageView)findViewById(R.id.imgPic);
        imgFL = (ImageView)findViewById(R.id.imgFL);
        imgPos = (ImageView)findViewById(R.id.imgPos);
        imgEff = (ImageView)findViewById(R.id.imgEff);

    }


    /*Restoring app*/

    @Override
    protected void onStart() {
        super.onStart();

        holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int rotate = getWindowManager().getDefaultDisplay().getRotation();
                switch (rotate) {
                    case Surface.ROTATION_0:
                        camera.setDisplayOrientation(90);
                        break;
                    case Surface.ROTATION_90:
                        camera.setDisplayOrientation(0);
                        break;
                    case Surface.ROTATION_270:
                        camera.setDisplayOrientation(180);
                        break;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int rotate = getWindowManager().getDefaultDisplay().getRotation();
                switch (rotate) {
                    case Surface.ROTATION_0:
                        camera.setDisplayOrientation(90);
                        break;
                    case Surface.ROTATION_90:
                        camera.setDisplayOrientation(0);
                        break;
                    case Surface.ROTATION_270:
                        camera.setDisplayOrientation(180);
                        break;
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.removeCallback(this);
            }
        });

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                showLocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Thread showLocationThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            showLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, "showLocation");
                showLocationThread.run();
            }
            else crdsView.setText("No provider enabled");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mySettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

    }


    /*Resuming app*/

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open();
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Camera.Parameters myParameters = camera.getParameters();

        if (mySettings.contains(FLASHLIGHT)) {
            Flashlight = mySettings.getInt(FLASHLIGHT, 0);
            myParameters.setFlashMode(photoSettingFormation(Flashlight, FLASHLIGHT));
            imgFL.setImageResource(getResourceFormation(Flashlight, FLASHLIGHT));
        }
        if (mySettings.contains(GEOPOSITION)) {
            GeoPosition = mySettings.getInt(GEOPOSITION, 0);
            imgPos.setImageResource(getResourceFormation(GeoPosition, GEOPOSITION));
        }
        if (mySettings.contains(EFFECT)) {
            Effect = mySettings.getInt(EFFECT, 0);
            myParameters.setColorEffect(photoSettingFormation(Effect, EFFECT));
            imgEff.setImageResource(getResourceFormation(Effect, EFFECT));
        }

        camera.setParameters(myParameters);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 10, 10, locationListener);
    }

    /*Pausing app*/

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        SharedPreferences.Editor editor = mySettings.edit();
        editor.putInt(FLASHLIGHT, Flashlight);
        editor.putInt(GEOPOSITION, GeoPosition);
        editor.putInt(EFFECT, Effect);
        editor.apply();

        if (camera != null)
            camera.release();
        camera = null;
    }


    /*Stopping app*/

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        if (camera != null)
            camera.release();
        camera = null;
    }


    /*If BACK was pressed*/

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Really Quit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


    /*Location listening*/

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                showLocation(location);
                globalLocation = formatLocation(location);
                globalLongitude = location.getLongitude();
                globalLatitude = location.getLatitude();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {
            try {
                showLocation(locationManager.getLastKnownLocation(provider));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override   //This function work with enable/disabe net/gps
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };


    /*Showing location on screen, getting globals from locationmanager*/

    private void showLocation(Location location) throws IOException {
        if (location == null) return;
        if (photoSettingFormation(Effect, EFFECT) == Camera.Parameters.EFFECT_WHITEBOARD) {
            crdsView.setTextColor(Color.BLACK);
            placeView.setTextColor(Color.BLACK);
        } else {
            crdsView.setTextColor(Color.WHITE);
            placeView.setTextColor(Color.WHITE);
        }
        if (photoSettingFormation(GeoPosition, GEOPOSITION) == "BOTH" || photoSettingFormation(GeoPosition, GEOPOSITION) == "COORDS")
            crdsView.setText("Coords: " + formatLocation(location));
        else crdsView.setText("");
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listAddress = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (null != listAddress && listAddress.size() > 0) {
                String _Location = listAddress.get(0).getAddressLine(0);
                if (photoSettingFormation(GeoPosition, GEOPOSITION) == "BOTH" || photoSettingFormation(GeoPosition, GEOPOSITION) == "PLACE")
                    placeView.setText("Place: " + _Location);
                else placeView.setText("");
                globalPlace = _Location;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*Formating location string*/

    private String formatLocation(Location location) {
        if (location == null) return "Location problems";
        return String.format(
                "lat=%1$.6f, lo=%2$.6f",
                location.getLatitude(), location.getLongitude()
        );
    }

    /*When PICTURE is pressed (taking a photo)*/

    public void onClickPicture(View view) {
        PIC.setEnabled(false);
        PIC.setClickable(false);
        PIC.setVisibility(View.GONE);
        NEXT.setEnabled(true);
        NEXT.setClickable(true);
        NEXT.setVisibility(View.VISIBLE);
        imgPic.setVisibility(View.VISIBLE);

        cameraIsActive = false;

        camera.takePicture(null, null, new PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] data, Camera camera) {

                takePictureThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Bitmap myBitmap = null, myTempBitmap = null;
                            Paint myPaint = new Paint();

                            myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                            int width = myBitmap.getWidth();
                            int height = myBitmap.getHeight();
                            Matrix matrix = new Matrix();
                            matrix.postScale(1, 1);

                            int rotate = getWindowManager().getDefaultDisplay().getRotation();
                            switch (rotate) {
                                case Surface.ROTATION_0:
                                    matrix.postRotate(90);
                                    break;
                                case Surface.ROTATION_90:
                                    matrix.postRotate(0);
                                    break;
                                case Surface.ROTATION_270:
                                    matrix.postRotate(180);
                                    break;
                            }
                            myTempBitmap = Bitmap.createBitmap(myBitmap, 0, 0, width, height, matrix, false);
                            if (!myTempBitmap.isMutable())
                                myTempBitmap = myTempBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(myTempBitmap);
                            myPaint.setColor(Color.WHITE);
                            myPaint.setTextSize(60);
                            switch (photoSettingFormation(GeoPosition, GEOPOSITION)) {
                                case "BOTH":
                                    canvas.drawText(globalLocation!=null ? globalLocation : "", 20, 70, myPaint);
                                    canvas.drawText(globalPlace!= null ? globalPlace : "", 20, 140, myPaint);
                                    break;
                                case "COORDS":
                                    canvas.drawText(globalLocation!=null ? globalLocation : "", 20, 70, myPaint);
                                    break;
                                case "PLACE":
                                    canvas.drawText(globalPlace!= null ? globalPlace : "", 20, 70, myPaint);
                                    break;
                            }

                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "savedBitmap" + System.currentTimeMillis() + ".jpg");

                    /*Saving photo*/

                            try {
                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(file);
                                    myTempBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                } finally {
                                    if (fos != null) fos.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                    /*Adding info in file*/
                            ExifInterface exifFile = new ExifInterface(file.getAbsolutePath());
                            exifFile.setAttribute(exifFile.TAG_GPS_LONGITUDE, String.valueOf(globalLongitude));
                            exifFile.setAttribute(exifFile.TAG_GPS_LATITUDE, String.valueOf(globalLatitude));
                            if (globalPlace != null) {
                                exifFile.setAttribute("UserComment", "Place is " + globalPlace);
                                exifFile.setAttribute("Place", globalPlace);
                            }
                            exifFile.saveAttributes();

                            MediaScannerConnection.scanFile(MainActivity.this,
                                    new String[] { file.toString() }, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        public void onScanCompleted(String path, Uri uri) {}
                                    });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, "takePhoto");

                if (!takePictureThread.isAlive() && nextPictureThread==null)
                    takePictureThread.run();
                else if (!takePictureThread.isAlive() && nextPictureThread!=null)
                    if (!nextPictureThread.isAlive()) takePictureThread.run();
            }
        });
    }

    /*If GEOSET was pressed (goto activity with geosettings)*/

    public void OnGeoSetClick(View view) {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }


    /*If CONTINUE was pressed*/

    public void onNextPhotoClick(View view) {
        nextPictureThread = new Thread(new Runnable() {
            public void run() {
                NEXT.setEnabled(false);
                NEXT.setClickable(false);
                NEXT.setVisibility(View.GONE);
                PIC.setEnabled(true);
                PIC.setClickable(true);
                PIC.setVisibility(View.VISIBLE);
                imgPic.setVisibility(View.INVISIBLE);

                cameraIsActive = true;
                camera.startPreview();
                Camera.Parameters myParameters = camera.getParameters();
                myParameters.setFlashMode(photoSettingFormation(Flashlight, FLASHLIGHT));
                camera.setParameters(myParameters);
            }
        }, "nextPhoto");

        if (!takePictureThread.isAlive() && !nextPictureThread.isAlive()) {
            nextPictureThread.run();
        }
    }


    /*Auto-focusing*/

    public void onSurfaceClick(View view) {
        if (cameraIsActive) {
            imgPic = (ImageView) findViewById(R.id.imgPic);
            camera.cancelAutoFocus();
            camera.autoFocus(null);
        }
    }


    /*If CAMSET was pressed (goto activity with camerasettings)*/

    public void OnCamSetClick(View view) {
        Intent setIntent = new Intent(MainActivity.this, SetActivity.class);
        startActivityForResult(setIntent, CAMERA_SETTINGS);
    }


    /*Returning from activity*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (mySettings.contains(FLASHLIGHT)) {
                Flashlight = mySettings.getInt(FLASHLIGHT, 0);
            }
            if (mySettings.contains(GEOPOSITION)) {
                GeoPosition = mySettings.getInt(GEOPOSITION, 0);

            }
            if (mySettings.contains(EFFECT)) {
                Effect = mySettings.getInt(EFFECT, 0);
            }
        }
    }

    /*Photo and geo settings format*/

    public String photoSettingFormation(int value, String setting) {
        switch (setting) {
            case FLASHLIGHT:
                switch (value) {
                    case 0:
                        return Camera.Parameters.FLASH_MODE_OFF;
                    case 1:
                        return Camera.Parameters.FLASH_MODE_AUTO;
                    case 2:
                        return Camera.Parameters.FLASH_MODE_ON;
                    case 3:
                        return Camera.Parameters.FLASH_MODE_RED_EYE;
                    case 4:
                        return Camera.Parameters.FLASH_MODE_TORCH;
                    default:
                        return Camera.Parameters.FLASH_MODE_OFF;
                }
            case GEOPOSITION:
                switch (value) {
                    case 0:
                        return "BOTH";
                    case 1:
                        return "PLACE";
                    case 2:
                        return "COORDS";
                    case 3:
                        return "NONE";
                    default:
                        return "BOTH";
                }
            case EFFECT:
                switch (value) {
                    case 0:
                        return Camera.Parameters.EFFECT_NONE;
                    case 1:
                        return Camera.Parameters.EFFECT_AQUA;
                    case 2:
                        return Camera.Parameters.EFFECT_BLACKBOARD;
                    case 3:
                        return Camera.Parameters.EFFECT_MONO;
                    case 4:
                        return Camera.Parameters.EFFECT_NEGATIVE;
                    case 5:
                        return Camera.Parameters.EFFECT_POSTERIZE;
                    case 6:
                        return Camera.Parameters.EFFECT_SEPIA;
                    case 7:
                        return Camera.Parameters.EFFECT_SOLARIZE;
                    case 8:
                        return Camera.Parameters.EFFECT_WHITEBOARD;
                    default:
                        return Camera.Parameters.EFFECT_NONE;
                }
            default:
                return "ERROR";
        }
    }
    private int getResourceFormation(int value, String setting) {
        switch (setting) {
            case FLASHLIGHT:
                switch (value) {
                    case 0:
                        return R.drawable.flashlight_off;
                    case 1:
                        return R.drawable.flashlight_auto;
                    case 2:
                        return R.drawable.flashlight_on;
                    case 3:
                        return R.drawable.flashlight_redeyes;
                    case 4:
                        return R.drawable.flashlight_torch;
                    default:
                        return R.drawable.flashlight_off;
                }
            case GEOPOSITION:
                switch (value) {
                    case 0:
                        return R.drawable.place_both;
                    case 1:
                        return R.drawable.place_place;
                    case 2:
                        return R.drawable.place_coords;
                    case 3:
                        return R.drawable.place_none;
                    default:
                        return R.drawable.place_both;
                }
            case EFFECT:
                switch (value) {
                    case 0:
                        return R.drawable.effect_noeffect;
                    case 1:
                        return R.drawable.effect_aqua;
                    case 2:
                        return R.drawable.effect_blackboard;
                    case 3:
                        return R.drawable.effect_mono;
                    case 4:
                        return R.drawable.effect_negative;
                    case 5:
                        return R.drawable.effect_posterize;
                    case 6:
                        return R.drawable.effect_sepia;
                    case 7:
                        return R.drawable.effect_solarize;
                    case 8:
                        return R.drawable.effect_whiteboard;
                    default:
                        return R.drawable.effect_noeffect;
                }
            default:
                return R.drawable.empty;
        }
    }
}
