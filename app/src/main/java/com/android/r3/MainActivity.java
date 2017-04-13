package com.android.r3;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    static Camera mCamera = null;
    static CameraView mCameraView = null;

    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected ILocationProvider locationProvider;

    protected SensorManager sensorManager;
    protected Sensor sensor;
    protected SensorEventListener sensorListener;


    Location lastKnownLocaton = null;
    protected boolean isLoading = false;
    protected JSONArray poiData;

    JSONArray resultarr;

    String[] record_id;
    String[] insert_date;
    double[] latitude;
    double[] longitude;
    String[] short_info;
    String[] long_info;

    ImageView[] imageview;
    TextView[] textViews;
    LinearLayout[] linearLayouts;

    int height;
    int width;


    double DEGREES_TO_RADIANS = Math.PI/180;
    double WGS84_A = 6378137.0;
    double WGS84_E = 8.1819190842622e-2;

    double[] projectionTransform = new double[16];
    double[] cameraTransform = new double[16];

    double[] vout = new double[4];
    double[] v = new double[4];

    double[] c = new double[16];


    // Current Location;
    double myX = 0.0;
    double myY = 0.0;
    double myZ = 0.0;

    // Place Location;
    double poiX = 0;
    double poiY = 0;
    double poiZ = 0;

    // ECEF Location;
    double e = 0;
    double n = 0;
    double u = 0;

    float rotationMatrix[] = new float[16];

    double[] accelerometer = new double[3];

    FrameLayout frameLayout;

    Timer t;

    static String stream = null;


    // Creates a Projection Matrix;
    public void createProjectionMatrix(double[] matrix, double fovy, double aspect, double zNear, double zFar){
        double f = 1.0 / Math.tan(fovy / 2.0);

        matrix[0] = f / aspect;
        matrix[1] = 0.0;
        matrix[2] = 0.0;
        matrix[3] = 0.0;

        matrix[4] = 0.0;
        matrix[5] = f;
        matrix[6] = 0.0;
        matrix[7] = 0.0;

        matrix[8] = 0.0;
        matrix[9] = 0.0;
        matrix[10] = (zFar + zNear) / (zNear - zFar);
        matrix[11] = -1.0;

        matrix[12] = 0.0;
        matrix[13] = 0.0;
        matrix[14] = 2 * zFar * zNear / (zNear-zFar);
        matrix[15] = 0.0;
    }


    // Matrix - Vector and Matrix - Matricx Multiplication Routines;
    public void multiplyMatrixAndVector(double[] vout, double[] m, double[] v) {
        vout[0] = m[0] * v[0] + m[4] * v[1] + m[8] * v[2] + m[12] * v[3];
        vout[1] = m[1] * v[0] + m[5] * v[1] + m[9] * v[2] + m[13] * v[3];
        vout[2] = m[2] * v[0] + m[6] * v[1] + m[10] * v[2] + m[14] * v[3];
        vout[3] = m[3] * v[0] + m[7] * v[1] + m[11] * v[2] + m[15] * v[3];
    }

    public void multiplyMatrixAndMatrix(double[] c, double[] a, double[] b) {
        c[0] = a[0] * b[0] + a[4] * b[1] + a[8] * b[2] + a[12] * b[3];
        c[1] = a[1] * b[0] + a[5] * b[1] + a[9] * b[2] + a[13] * b[3];
        c[2] = a[2] * b[0] + a[6] * b[1] + a[10] * b[2] + a[14] * b[3];
        c[3] = a[3] * b[0] + a[7] * b[1] + a[11] * b[2] + a[15] * b[3];

        c[4] = a[0] * b[4] + a[4] * b[5] + a[8] * b[6] + a[12] * b[7];
        c[5] = a[1] * b[4] + a[5] * b[5] + a[9] * b[6] + a[13] * b[7];
        c[6] = a[2] * b[4] + a[6] * b[5] + a[10] * b[6] + a[14] * b[7];
        c[7] = a[3] * b[4] + a[7] * b[5] + a[11] * b[6] + a[15] * b[7];

        c[8] = a[0] * b[8] + a[4] * b[9] + a[8] * b[10] + a[12] * b[11];
        c[9] = a[1] * b[8] + a[5] * b[9] + a[9] * b[10] + a[13] * b[11];
        c[10] = a[2] * b[8] + a[6] * b[9] + a[10] * b[10] + a[14] * b[11];
        c[11] = a[3] * b[8] + a[7] * b[9] + a[11] * b[10] + a[15] * b[11];

        c[12] = a[0] * b[12] + a[4] * b[13] + a[8] * b[14] + a[12] * b[15];
        c[13] = a[1] * b[12] + a[5] * b[13] + a[9] * b[14] + a[13] * b[15];
        c[14] = a[2] * b[12] + a[6] * b[13] + a[10] * b[14] + a[14] * b[15];
        c[15] = a[3] * b[12] + a[7] * b[13] + a[11] * b[14] + a[15] * b[15];
    }


    // Initialize mout to be an affine transform corresponding to the same rotation specified by m;
   /* public void transformFromCMRotationMatrix(double[] mout, float[] m) {
        mout[0] = (double)m[0];
        mout[1] = (double)m[1];
        mout[2] = (double)m[2];
        mout[3] = 0.0;

        mout[4] = (double)m[3];
        mout[5] = (double)m[4];
        mout[6] = (double)m[5];
        mout[7] = 0.0;

        mout[8] = (double)m[6];
        mout[9] = (double)m[7];
        mout[10] = (double)m[8];
        mout[11] = 0.0;

        mout[12] = 0.0;
        mout[13] = 0.0;
        mout[14] = 0.0;
        mout[15] = 1.0;
    }*/

    // Converts latitude, longitude to ECEF coordinate system;
    public void latLonToEcef(double lat, double lon, double alt) {
        double clat = Math.cos(lat * DEGREES_TO_RADIANS);
        double slat = Math.sin(lat * DEGREES_TO_RADIANS);
        double clon = Math.cos(lon * DEGREES_TO_RADIANS);
        double slon = Math.sin(lon * DEGREES_TO_RADIANS);

        double N = WGS84_A / Math.sqrt( 1.0 - WGS84_E * WGS84_E * slat * slat );

        myX = (N + alt) * clat * clon;
        myY = (N + alt) * clat * slon;
        myZ = (N * (1.0 - WGS84_E * WGS84_E) + alt) * slat;
    }

    public void latLonToEcef1(double lat, double lon, double alt) {
        double clat = Math.cos(lat * DEGREES_TO_RADIANS);
        double slat = Math.sin(lat * DEGREES_TO_RADIANS);
        double clon = Math.cos(lon * DEGREES_TO_RADIANS);
        double slon = Math.sin(lon * DEGREES_TO_RADIANS);

        double N = WGS84_A / Math.sqrt( 1.0 - WGS84_E * WGS84_E * slat * slat );

        poiX = (N + alt) * clat * clon;
        poiY = (N + alt) * clat * slon;
        poiZ = (N * (1.0 - WGS84_E * WGS84_E) + alt) * slat;
    }


    // Coverts ECEF to ENU coordinates centered at given lat, lon;
    public void ecefToEnu(double lat, double lon, double x, double y, double z, double xr, double yr, double zr)
    {
        double clat = Math.cos(lat * DEGREES_TO_RADIANS);
        double slat = Math.sin(lat * DEGREES_TO_RADIANS);
        double clon = Math.cos(lon * DEGREES_TO_RADIANS);
        double slon = Math.sin(lon * DEGREES_TO_RADIANS);
        double dx = x - xr;
        double dy = y - yr;
        double dz = z - zr;

        e = -slon * dx  + clon * dy;
        n = -slat * clon * dx - slat * slon * dy + clat * dz;
        u = clat * clon * dx + clat * slon * dy + slat * dz;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
//------help----------------
        Button button = (Button) findViewById(R.id.help_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,HelpView.class);
                startActivity(intent);
            }
        });

        //-----------getting width and height of device         2016.9.27
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        height = displaymetrics.heightPixels;
        width = displaymetrics.widthPixels;
        System.out.println(height + "~~~~~~~~~~~~" + width);

        try {
            getJsonArray();//----------getting jsonarray from webserver
        }catch (Exception e){

        }

        createProjectionMatrix(projectionTransform, 60.0 * DEGREES_TO_RADIANS, 0.4, 0.25, 1000.0);

//----------getting location of device--------------------
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastKnownLocaton = location;
            //    injectData();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            RequestPermission();
//            return;
//        }

        this.locationProvider = getLocationProvider(this.locationListener);

//--------------start camera-----------------------------------
        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
            System.out.println(mCamera);
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }


        //btn to close the application
       /* ImageButton imgClose = (ImageButton)findViewById(R.id.imgClose);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });*/


//------getting lotation vector of device-----------------------
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                onDisplayLink();

                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(
                            rotationMatrix , event.values);
                }
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    accelerometer[0] = event.values[0];
                    accelerometer[1] = event.values[1];
                    accelerometer[2] = event.values[2];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_FASTEST);
//--------recalling function----------------------------
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

                                  @Override
                                  public void run() {

                                  }

                              },3000, 30);


    }


    public void onDisplayLink() {
        for(int i = 0; i < rotationMatrix.length;i++){
            cameraTransform[i] = (double) rotationMatrix[i];
        }

        // Update;
        updatePlaceViews();

    }

    public void updatePlaceViews() {
        multiplyMatrixAndMatrix(c, projectionTransform,  cameraTransform);

        if (lastKnownLocaton != null){
            latLonToEcef(lastKnownLocaton.getLatitude(), lastKnownLocaton.getLongitude(), 0.0);
        }

        for (int i = 0; i < resultarr.length(); i++)
        {
            latLonToEcef1(latitude[i], longitude[i], 0.0);

            if (lastKnownLocaton != null) {
                ecefToEnu(lastKnownLocaton.getLatitude(), lastKnownLocaton.getLongitude(), myX, myY, myZ, poiX, poiY, poiZ);
            }

            v[0] = n;
            v[1] = -e;
            v[2] = 0.0;
            v[3] = 1.0;

            multiplyMatrixAndVector(vout, c, v);

            // X, Y;
            double x = (vout[0] / vout[3] + 1.0) * 0.5;
            double y = (vout[1] / vout[3] + 1.0) * 0.5;


            if(vout[2] < 0) {
                x = x * width;
                y = height - y * height;


                linearLayouts[i].setX((float) x);
                linearLayouts[i].setY((float) y);

           //     textViews[i].setX((float) x);
           //     textViews[i].setY((float) y + 150);

                double rotation = Math.atan2(-accelerometer[1], accelerometer[0]) * 180 / 3.14 + 90;
                linearLayouts[i].setRotation((float) rotation);
            }else{
             //   imageview[i].setVisibility(View.INVISIBLE);
             //   textViews[i].setVisibility(View.INVISIBLE);
            }
        }

    }


    public void getJsonArray() throws Exception{

        URL url = new URL("http://r3.rna.webfactional.com/poilist.php");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // Check the connection status
        if(urlConnection.getResponseCode() == 200)
        {
            // if response code = 200 ok
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            // Read the BufferedInputStream
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            stream = sb.toString();
            // End reading...............

            // Disconnect the HttpURLConnection
            urlConnection.disconnect();
        }
        else
        {
            // Do something
        }

//--------------------results get in jsonarray ----------------------------------------------------------------------------------

        String ready = "{\"locations\":" + stream + "}";
        JSONObject jo = new JSONObject(ready);
        resultarr = jo.getJSONArray("locations");

        record_id = new String[resultarr.length()];
        insert_date = new String[resultarr.length()];
        latitude = new double[resultarr.length()];
        longitude = new double[resultarr.length()];
        short_info = new String[resultarr.length()];
        long_info = new String[resultarr.length()];
        imageview = new ImageView[resultarr.length()];
        textViews = new TextView[resultarr.length()];
        linearLayouts = new LinearLayout[resultarr.length()];

        for ( int i = 0; i < resultarr.length(); i++)
        {
            JSONObject jObjresult = resultarr.getJSONObject(i);
            record_id[i] = jObjresult.getString("record_id");
            insert_date[i] = jObjresult.getString("insert_date");
            latitude[i] = jObjresult.getDouble("x");
            longitude[i] = jObjresult.getDouble("y");
            short_info[i] = jObjresult.getString("short_info");
            long_info[i] = jObjresult.getString("long_info");

//----------location image array----------------------------------

            frameLayout = (FrameLayout) findViewById(R.id.framelayout);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(120,152);
            FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(200,300);
            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(200,400);

            linearLayouts[i] = new LinearLayout(this);
            linearLayouts[i].setOrientation(LinearLayout.VERTICAL);

            final String date_str = insert_date[i];
            final String long_str = long_info[i];

            imageview[i] = new ImageView(this);
            imageview[i].setImageResource(R.drawable.img_location);
            imageview[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    t.cancel();
                    mCameraView.removeCallbacks(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                    Intent intent = new Intent(MainActivity.this,DetailView.class);
                    intent.putExtra("insert_date",date_str);
                    intent.putExtra("long_info",long_str);
                    startActivity(intent);
                }
            });
            linearLayouts[i].addView(imageview[i],lp);

            textViews[i] = new TextView(this);
            textViews[i].setText(short_info[i]);
            textViews[i].setTextSize(16);
            textViews[i].setTextColor(Color.parseColor("#0000ff"));
            linearLayouts[i].addView(textViews[i],lp1);
            frameLayout.addView(linearLayouts[i],lp2);
        }
    }

    public ILocationProvider getLocationProvider(final LocationListener locationListener) {
        return new LocationProvider(this, locationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (this.locationProvider != null) {
            this.locationProvider.onResume();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.locationProvider != null) {
            this.locationProvider.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }



    private void RequestPermission() {

        List<String> permissionList = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
            Toast.makeText(this, "You can not start with the camera can not be used.", Toast.LENGTH_LONG).show();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            Toast.makeText(this, "Location information (GPS) can not be started and can not be used.", Toast.LENGTH_LONG).show();
        }
        if (permissionList.size() > 0) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            int REQUEST_CODE_NONE = 0;
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_NONE);

            for (int i = 0; i < 300; i++)
            {
                if (isFinishing()) return;
                try {
                    Thread.sleep(100);
                    Thread.yield();
                } catch (InterruptedException e) {
                    break;
                }

                if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    return;
                }
            }


            Toast.makeText(this, "After the permission settings, please re-start the app again.", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    protected void injectData() {
        if (!isLoading) {
            final Thread t = new Thread(new Runnable() {

                @Override
                public void run() {

                    isLoading = true;

                    final int WAIT_FOR_LOCATION_STEP_MS = 2000;
                    while (lastKnownLocaton == null && !isFinishing()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, R.string.location_fetching, Toast.LENGTH_SHORT).show();
                            }
                        });
                        try {
                            Thread.sleep(WAIT_FOR_LOCATION_STEP_MS);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    if (lastKnownLocaton != null && !isFinishing()) {
                        poiData = getPoiInformation(lastKnownLocaton, resultarr.length());
                        callJavaScript("World.loadPoisFromJsonData", new String[]{poiData.toString()});
                    }

                    isLoading = false;
                }
            });
            t.start();
        }
    }

    public JSONArray getPoiInformation(final Location userLocation, final int numberOfPlaces) {

        if (userLocation == null) {
            return null;
        }

        final JSONArray pois = new JSONArray();

        final String ATTR_ID = "id";
        final String ATTR_NAME = "short_info";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";
        final String ATTR_DATE = "insert_date";
        final String ATTR_DISTANCE = "distance";
        final String ATTR_LONG = "long_info";

        for (int i = 0; i < numberOfPlaces; i++) {

            double[] poiLocationLatLon = {latitude[i],longitude[i]};
            double distance = getDistance(poiLocationLatLon[0], userLocation.getLatitude(), poiLocationLatLon[1], userLocation.getLongitude());
            BigDecimal decimalKm = new BigDecimal(distance / 1000);
            String distanceString = (distance > 999) ? (decimalKm.setScale(2, BigDecimal.ROUND_HALF_UP)  + " km") : (Math.round(distance) + " m");



            JSONObject singlePoiInfo = new JSONObject();
            try {
                singlePoiInfo.accumulate(ATTR_ID, record_id[i]);
                singlePoiInfo.accumulate(ATTR_NAME, short_info[i]);
                singlePoiInfo.accumulate(ATTR_LATITUDE, poiLocationLatLon[0]);
                singlePoiInfo.accumulate(ATTR_LONGITUDE, poiLocationLatLon[1]);
                singlePoiInfo.accumulate(ATTR_DISTANCE, distanceString);
                singlePoiInfo.accumulate(ATTR_DATE, insert_date[i]);
                singlePoiInfo.accumulate(ATTR_LONG, long_info[i]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pois.put(singlePoiInfo);
        }
        return pois;
    }


    private void callJavaScript(final String methodName, final String[] arguments) {
        final StringBuilder argumentsString = new StringBuilder("");
        for (int i = 0; i < arguments.length; i++) {
            argumentsString.append(arguments[i]);
            if (i < arguments.length - 1) {
                argumentsString.append(", ");
            }
        }

    }

    public static final boolean isVideoDrawablesSupported() {
        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        return extensions != null && extensions.contains("GL_OES_EGL_image_external");
    }

    public static double getDistance(double targetLatitude, double centerPointLatitude, double targetLongtitude, double centerPointLongitude) {
        double Δφ = (centerPointLatitude - targetLatitude) * Math.PI / 180;
        double Δλ = (centerPointLongitude - targetLongtitude) * Math.PI / 180;
        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) + Math.cos(targetLatitude * Math.PI / 180) * Math.cos(centerPointLatitude * Math.PI / 180) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371e3 * c;
    }

}
