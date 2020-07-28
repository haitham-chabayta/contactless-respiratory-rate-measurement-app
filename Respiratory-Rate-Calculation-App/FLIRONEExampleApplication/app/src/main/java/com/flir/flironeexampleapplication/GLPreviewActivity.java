package com.flir.flironeexampleapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.flir.flironeexampleapplication.util.SystemUiHider;
import com.flir.flironesdk.Device;
import com.flir.flironesdk.FlirUsbDevice;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;
import com.flir.flironesdk.SimulatedDevice;
import com.jjoe64.graphview.series.DataPoint;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * An example activity and delegate for FLIR One image streaming and device interaction.
 * Based on an example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 * @see Device.Delegate
 * @see FrameProcessor.Delegate
 * @see Device.StreamDelegate
 * @see Device.PowerUpdateDelegate
 */
public class GLPreviewActivity extends Activity implements Device.Delegate, FrameProcessor.Delegate, Device.StreamDelegate, Device.PowerUpdateDelegate , CameraBridgeViewBase.CvCameraViewListener2{

    LinearLayout linearLayout;
    CascadeClassifier faceDetector;
    private Mat mrgba , mGrey;
    File cascadeFile;

    int startRecordingCnt = 0;
    GLSurfaceView thermalSurfaceView;
    private volatile boolean startRecording = false;
    double fractionOfSecond = 0 ;
    int seconds = 0;
    private volatile Socket streamSocket = null;
    private boolean chargeCableIsConnected = true;
    ArrayList<DataPoint> avrFrameTemp = new ArrayList<>();

    private int deviceRotation= 0;
    private OrientationEventListener orientationEventListener;


    private volatile Device flirOneDevice;
    private FrameProcessor frameProcessor;


    private Device.TuningState currentTuningState = Device.TuningState.Unknown;
    private boolean accFrame = true;


    double rectX = 200;
    double rectY = 400;
    double rectHight = 1;
    double rectWidth = 1;

    double rectXpxl = 200;
    double rectYpxl = 400;
    double rectHightpxl = 1;
    double rectWidthpxl = 1;
    ArrayList<Frame> oldFrames = new ArrayList<Frame>();
    ArrayList<Double> oldFramesTime = new ArrayList<>();

    boolean addFrame = true;
    boolean calculated = false;

    boolean accessOpenCV = true;
    double cTime = 0;
    int noframe =0;
    int noframeCalc =0;

    boolean accFrame2 = true;
    // Device Delegate methods

    // Called during device discovery, when a device is connected
    // During this callback, you should save a reference to device
    // You should also set the power update delegate for the device if you have one
    // Go ahead and start frame stream as soon as connected, in this use case
    // Finally we create a frame processor for rendering frames

    public void onDeviceConnected(Device device){


        flirOneDevice = device;
        flirOneDevice.setPowerUpdateDelegate(this);
        flirOneDevice.startFrameStream(this);
        //flirOneDevice.setAutomaticTuning(false);
        final ToggleButton chargeCableButton = (ToggleButton)findViewById(R.id.chargeCableToggle);
        if(flirOneDevice instanceof SimulatedDevice){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chargeCableButton.setChecked(chargeCableIsConnected);
                    chargeCableButton.setVisibility(View.VISIBLE);
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    chargeCableButton.setChecked(chargeCableIsConnected);
                    chargeCableButton.setVisibility(View.INVISIBLE);
                    findViewById(R.id.connect_sim_button).setEnabled(false);

                }
            });
        }

        orientationEventListener.enable();
    }

    /**
     * Indicate to the user that the device has disconnected
     */
    public void onDeviceDisconnected(Device device){
        Log.i("ExampleApp", "Device disconnected!");

        final ToggleButton chargeCableButton = (ToggleButton)findViewById(R.id.chargeCableToggle);
        final TextView levelTextView = (TextView)findViewById(R.id.batteryLevelTextView);
        final ImageView chargingIndicator = (ImageView)findViewById(R.id.batteryChargeIndicator);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.pleaseConnect).setVisibility(View.GONE);
                levelTextView.setText("--");
                chargeCableButton.setChecked(chargeCableIsConnected);
                chargeCableButton.setVisibility(View.INVISIBLE);
                chargingIndicator.setVisibility(View.GONE);
                findViewById(R.id.tuningProgressBar).setVisibility(View.GONE);
                findViewById(R.id.tuningTextView).setVisibility(View.GONE);
                findViewById(R.id.connect_sim_button).setEnabled(true);
            }
        });
        flirOneDevice = null;
        orientationEventListener.disable();
    }

    /**
     * If using RenderedImage.ImageType.ThermalRadiometricKelvinImage, you should not rely on
     * the accuracy if tuningState is not Device.TuningState.Tuned
     * @param tuningState
     */
    public void onTuningStateChanged(Device.TuningState tuningState){

        currentTuningState = tuningState;
        if (tuningState == Device.TuningState.InProgress){
            runOnUiThread(new Thread(){
                @Override
                public void run() {
                    super.run();
                    findViewById(R.id.tuningProgressBar).setVisibility(View.VISIBLE);
                    findViewById(R.id.tuningTextView).setVisibility(View.VISIBLE);
                }
            });
        }else {
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    super.run();
                    findViewById(R.id.tuningProgressBar).setVisibility(View.GONE);
                    findViewById(R.id.tuningTextView).setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onAutomaticTuningChanged(boolean deviceWillTuneAutomatically) {

    }
    private ColorFilter originalChargingIndicatorColor = null;
    @Override
    public void onBatteryChargingStateReceived(final Device.BatteryChargingState batteryChargingState) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView chargingIndicator = (ImageView)findViewById(R.id.batteryChargeIndicator);
                if (originalChargingIndicatorColor == null){
                    originalChargingIndicatorColor = chargingIndicator.getColorFilter();
                }
                switch (batteryChargingState) {
                    case FAULT:
                    case FAULT_HEAT:
                        chargingIndicator.setColorFilter(Color.RED);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case FAULT_BAD_CHARGER:
                        chargingIndicator.setColorFilter(Color.DKGRAY);
                        chargingIndicator.setVisibility(View.VISIBLE);
                    case MANAGED_CHARGING:
                        chargingIndicator.setColorFilter(originalChargingIndicatorColor);
                        chargingIndicator.setVisibility(View.VISIBLE);
                        break;
                    case NO_CHARGING:
                    default:
                        chargingIndicator.setVisibility(View.GONE);
                        break;
                }
            }
        });
    }
    @Override
    public void onBatteryPercentageReceived(final byte percentage){
        Log.i("ExampleApp", "Battery percentage received!");

        final TextView levelTextView = (TextView)findViewById(R.id.batteryLevelTextView);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                levelTextView.setText(String.valueOf((int) percentage) + "%");
            }
        });


    }

    // StreamDelegate method
    public void onFrameReceived(Frame frame) {
            if(startRecording)
                noframe++;

            if(startRecording){
                oldFrames.add(frame);
                double second = fractionOfSecond/10.0;
                oldFramesTime.add(second);
            }

        if (currentTuningState != Device.TuningState.InProgress){
            if (accFrame && !calculated ) {
                frameProcessor.processFrame(frame, FrameProcessor.QueuingOption.CLEAR_QUEUED);
                thermalSurfaceView.requestRender();
            }
        }
    }

    private Bitmap thermalBitmap = null;

    // Frame Processor Delegate method, will be called each time a rendered frame is produced
    public void onFrameProcessed(final RenderedImage renderedImage){
       accFrame2 = false;
        try {

            if(renderedImage.imageType() == RenderedImage.ImageType.VisibleAlignedRGBA8888Image && accessOpenCV ){

                accFrame = false;
                mrgba = new Mat(renderedImage.width() , renderedImage.height(), CvType.CV_8UC3);
                //mrgba =  Imgcodecs.imdecode(new MatOfByte(renderedImage.pixelData()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
                mrgba.put(0,0, renderedImage.pixelData());
                final MatOfRect faceDetection = new MatOfRect();
                faceDetector.detectMultiScale(mrgba , faceDetection);

                double width = renderedImage.width();
                double height = renderedImage.height();
                double screenX = Resources.getSystem().getDisplayMetrics().widthPixels;
                double screenY = Resources.getSystem().getDisplayMetrics().heightPixels;

                double opencvWidth = mrgba.width();
                double opencvHeight = mrgba.height();


                for (Rect rect:faceDetection.toArray()){
                    rectX = (rect.x - (rect.x* 0.05))  * (screenX/width) ;
                    rectXpxl = (rect.x - (rect.x* 0.05));
                    rectY = (rect.y +(rect.height*0.75)) * (screenY/height) ;
                    rectYpxl =(rect.y +(rect.height*0.75));
                    rectHight = (rect.height * 0.30) * (screenY/height);
                    rectHightpxl = (rect.height * 0.30);
                    rectWidth = (rect.width *0.60) *(screenX/width);
                    rectWidthpxl = (rect.width *0.60) ;
                }


                /*
                TextView T1 = ((TextView)findViewById(R.id.spotMeterValue));
                String bla = opencvWidth + " " + opencvHeight ;
                T1.setText(bla);*/

                AbsoluteLayout rect = (AbsoluteLayout)findViewById(R.id.recDetect);
                rect.setX((int)rectX);
                rect.setY((int)rectY);
                rect.getLayoutParams().height = (int)rectHight;
                rect.getLayoutParams().width = (int)rectWidth;
               /*
                if(faceDetection.toArray().length == 0){
                    rect.setX(0);
                    rect.setY(0);
                    rect.getLayoutParams().height = 0;
                    rect.getLayoutParams().width = 0;

                     rectXpxl = 0 ;
                     rectYpxl = 0;
                     rectHightpxl = 0;
                     rectWidthpxl = 0;
                }*/


                accFrame = true;
            }
        }catch (final Exception ex ){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView)findViewById(R.id.spotMeterValue)).setText("error "+ex.getMessage());
                }
            });
        }


        if (renderedImage.imageType() == RenderedImage.ImageType.ThermalRadiometricKelvinImage && calculated){
            // Note: this code is not optimized

            int[] thermalPixels = renderedImage.thermalPixelValues();
            // average the center 9 pixels for the spot meter

            int theWidth = renderedImage.width();

            int startPixelIndex = (int)(((rectYpxl-1) * (theWidth)) + rectXpxl);
            int indexexNo = (int)(rectWidthpxl*rectHightpxl);
            int[] centerPixelIndexes = new int[indexexNo];
            int newWidth = (int) rectWidthpxl;

            for(int x = 0 ; x < centerPixelIndexes.length ; x++){
                centerPixelIndexes[x] = startPixelIndex +x;

                if (x %newWidth == 0 && x !=0){
                    startPixelIndex += (int)(theWidth - rectWidthpxl);
                }

            }

            double averageTemp = 0;

            //int counter = 0;
            for (int i = 0; i < centerPixelIndexes.length; i++){
                // Remember: all primitives are signed, we want the unsigned value,
                // we've used renderedImage.thermalPixelValues() to get unsigned values

                int pixelValue = (thermalPixels[centerPixelIndexes[i]]);
                /*
                if (pixelValue<30415 || pixelValue>31015)
                    continue;
                */

                averageTemp += (((double)pixelValue) - averageTemp) / ((double) i + 1);
                //counter ++;
            }
            double averageC = (averageTemp / 100) - 273.15;
            //double second = fractionOfSecond/10.0;

            DataPoint tempPoint = new DataPoint(cTime , averageC);
            avrFrameTemp.add(tempPoint);

            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            numberFormat.setMinimumFractionDigits(2);

            String spotMeterValue = numberFormat.format(averageC) + "ºC";
            TextView textView = ((TextView)findViewById(R.id.spotMeterValue));
            textView.setText(spotMeterValue);
            accFrame2 = true;
        }





    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    public void onTuneClicked(View v){
        if (flirOneDevice != null){
            flirOneDevice.performTuning();
        }

    }

    public void onCaptureImageClicked(View v){
        accessOpenCV = false;
        // startRecordingCnt++;
        if(flirOneDevice != null) {

            this.startRecording = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((ImageButton)findViewById(R.id.imageButton)).setEnabled(false);
                }
            });

            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    fractionOfSecond++;
                    if(fractionOfSecond %10 ==0){
                        seconds++;
                        /*
                        if(seconds %5 == 0)
                            accessOpenCV = true;
                        else
                            accessOpenCV = false;
                        */
                        if(seconds<10) {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.SecondsPreview)).setText("0:0" + seconds);
                                }
                            });
                        }
                        else {
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.SecondsPreview)).setText("0:" + seconds);
                                }
                            });
                        }

                    }

                    if(seconds >= 30){
                        //addFrame = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((ImageButton)findViewById(R.id.imageButton)).setEnabled(true);
                            }
                        });


                        //seconds = 0 ;
                        //fractionOfSecond =0;
                        startRecording = false;
                        calculated = true;




                        try {
                            if(accFrame2 && fractionOfSecond % 2 == 0){
                                Frame nframe = oldFrames.remove(0);
                                cTime = oldFramesTime.remove(0);
                                frameProcessor.processFrame(nframe, FrameProcessor.QueuingOption.CLEAR_QUEUED);
                                thermalSurfaceView.requestRender();
                            }


                            if(oldFrames.size() == 0){
                                calculateBreathRate();
                                this.cancel();
                            }



                        }catch (final Exception ex){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView)findViewById(R.id.spotMeterValue)).setText("error " + ex.getMessage());
                                }
                            });
                        }

                    }
                }
            },0,100);


        }
    }


    public void onConnectSimClicked(View v){
        if(flirOneDevice == null){
            try {
                flirOneDevice = new SimulatedDevice(this, this, getResources().openRawResource(R.raw.sampleframes), 10);
                flirOneDevice.setPowerUpdateDelegate(this);
                chargeCableIsConnected = true;
            } catch(Exception ex) {
                flirOneDevice = null;
                Log.w("FLIROneExampleApp", "IO EXCEPTION");
                ex.printStackTrace();
            }
        }else if(flirOneDevice instanceof SimulatedDevice) {
            flirOneDevice.close();
            flirOneDevice = null;
        }
    }

    public void onSimulatedChargeCableToggleClicked(View v){
        if(flirOneDevice instanceof SimulatedDevice){
            chargeCableIsConnected = !chargeCableIsConnected;
            ((SimulatedDevice)flirOneDevice).setChargeCableState(chargeCableIsConnected);
        }
    }

    public void onRotateClicked(View v){
        ToggleButton theSwitch = (ToggleButton)v;
        if (theSwitch.isChecked()){
            frameProcessor.setImageRotation(180.0f);
        }else{
            frameProcessor.setImageRotation(0.0f);
        }
    }

    public void onChangeViewClicked(View v){
        if (frameProcessor == null){
            ((ToggleButton)v).setChecked(false);
            return;
        }
        ListView paletteListView = (ListView)findViewById(R.id.paletteListView);
        ListView imageTypeListView = (ListView)findViewById(R.id.imageTypeListView);
        if (((ToggleButton)v).isChecked()){
            // only show palette list if selected image type is colorized
            paletteListView.setVisibility(View.INVISIBLE);
            for (RenderedImage.ImageType imageType : frameProcessor.getImageTypes()){
                if (imageType.isColorized()) {
                    paletteListView.setVisibility(View.VISIBLE);
                    break;
                }
            }
            imageTypeListView.setVisibility(View.VISIBLE);
            findViewById(R.id.imageTypeListContainer).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.imageTypeListContainer).setVisibility(View.GONE);
        }


    }

    public void onImageTypeListViewClicked(View v){
        int index = ((ListView) v).getSelectedItemPosition();
        RenderedImage.ImageType imageType = RenderedImage.ImageType.values()[index];
        frameProcessor.setGLOutputMode(imageType);
        int paletteVisibility = (imageType.isColorized()) ? View.VISIBLE : View.GONE;
        findViewById(R.id.paletteListView).setVisibility(paletteVisibility);
    }

    public void onPaletteListViewClicked(View v){
        RenderedImage.Palette pal = (RenderedImage.Palette )(((ListView)v).getSelectedItem());
        frameProcessor.setImagePalette(pal);
    }

    /**
     * Example method of starting/stopping a frame stream to a host
     * @param v The toggle button pushed
     */
    public void onVividClicked(View v){
        final ToggleButton button = (ToggleButton)v;
        frameProcessor.setVividIrEnabled(button.isChecked());
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGrey = new Mat();
        mrgba = new Mat();
        if (Device.getSupportedDeviceClasses(this).contains(FlirUsbDevice.class)){
            findViewById(R.id.pleaseConnect).setVisibility(View.VISIBLE);
        }
        try {
            Device.startDiscovery(this, this);
        }catch(IllegalStateException e){
            // it's okay if we've already started discovery
        }catch (SecurityException e){
            // On some platforms, we need the user to select the app to give us permisison to the USB device.
            Toast.makeText(this, "Please insert FLIR One and select "+getString(R.string.app_name), Toast.LENGTH_LONG).show();
            // There is likely a cleaner way to recover, but for now, exit the activity and
            // wait for user to follow the instructions;
            finish();
        }
    }

    ScaleGestureDetector mScaleDetector;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gl_preview);


        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View controlsViewTop = findViewById(R.id.fullscreen_content_controls_top);
        final View contentView = findViewById(R.id.fullscreen_content);
        linearLayout = findViewById(R.id.fullscreen_content_controls_top);


        RenderedImage.ImageType defaultImageType = RenderedImage.ImageType.BlendedMSXRGBA8888Image;
        frameProcessor = new FrameProcessor(this, this, EnumSet.of(RenderedImage.ImageType.ThermalRadiometricKelvinImage , RenderedImage.ImageType.VisibleAlignedRGBA8888Image), true);
        frameProcessor.setGLOutputMode(defaultImageType);


        thermalSurfaceView = (GLSurfaceView) findViewById(R.id.imageView);
        thermalSurfaceView.setPreserveEGLContextOnPause(true);
        thermalSurfaceView.setEGLContextClientVersion(2);
        thermalSurfaceView.setRenderer(frameProcessor);
        thermalSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        thermalSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);


        final String[] imageTypeNames = new String[]{ "Visible", "Thermal", "MSX" };
        final RenderedImage.ImageType[] imageTypeValues = new RenderedImage.ImageType[]{
                RenderedImage.ImageType.VisibleAlignedRGBA8888Image,
                RenderedImage.ImageType.ThermalRGBA8888Image,
                RenderedImage.ImageType.BlendedMSXRGBA8888Image,
        };

        ListView imageTypeListView = ((ListView)findViewById(R.id.imageTypeListView));
        imageTypeListView.setAdapter(new ArrayAdapter<>(this,R.layout.emptytextview,imageTypeNames));
        imageTypeListView.setSelection(defaultImageType.ordinal());
        imageTypeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (frameProcessor != null) {
                    RenderedImage.ImageType imageType = imageTypeValues[position];
                    frameProcessor.setGLOutputMode(imageType);
                    if (imageType.isColorized()){
                        findViewById(R.id.paletteListView).setVisibility(View.VISIBLE);
                    }else{
                        findViewById(R.id.paletteListView).setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        imageTypeListView.setDivider(null);

        // Palette List View Setup
        ListView paletteListView = ((ListView)findViewById(R.id.paletteListView));
        paletteListView.setDivider(null);
        paletteListView.setAdapter(new ArrayAdapter<>(this, R.layout.emptytextview, RenderedImage.Palette.values()));
        paletteListView.setSelection(frameProcessor.getImagePalette().ordinal());
        paletteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (frameProcessor != null){
                    frameProcessor.setImagePalette(RenderedImage.Palette.values()[position]);
                }
            }
        });
        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.

        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                            controlsViewTop.animate().translationY(visible ? 0 : -1 * mControlsHeight).setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                            controlsViewTop.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && !((ToggleButton)findViewById(R.id.change_view_button)).isChecked() && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.change_view_button).setOnTouchListener(mDelayHideTouchListener);


        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                deviceRotation = orientation;
            }
        };
        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Log.d("ZOOM", "zoom ongoing, scale: " + detector.getScaleFactor());
                frameProcessor.setMSXDistance(detector.getScaleFactor());
                return false;
            }
        });

        findViewById(R.id.fullscreen_content).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                return true;
            }
        });

        // ask for permission?
        String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        boolean permissionGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionGranted = (ContextCompat.checkSelfPermission(this, writePermission) == PackageManager.PERMISSION_GRANTED);
        }
        else {
            permissionGranted = (PermissionChecker.checkSelfPermission(this, writePermission) == PermissionChecker.PERMISSION_GRANTED);
        }

        if(!permissionGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, writePermission)) {
                Toast.makeText(this, "App requires write permission to save photos", Toast.LENGTH_LONG).show();
            }
            else {
                ActivityCompat.requestPermissions(this, new String[]{writePermission}, 0);
            }
        }

        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this,baseCallback);

        }else{
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

            } catch (IOException e) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.spotMeterValue)).setText("manager dose not connected ");
                    }
                });

            }

        }


    }

    @Override
    public void onPause(){
        super.onPause();

        thermalSurfaceView.onPause();
        if (flirOneDevice != null){
            flirOneDevice.stopFrameStream();
        }
    }
    @Override
    public void onResume(){
        super.onResume();

        thermalSurfaceView.onResume();

        if (flirOneDevice != null){
            flirOneDevice.startFrameStream(this);
        }
    }
    @Override
    public void onStop() {
        // We must unregister our usb receiver, otherwise we will steal events from other apps
        Log.e("PreviewActivity", "onStop, stopping discovery!");
        mrgba.release();
        mGrey.release();
        Device.stopDiscovery();
        flirOneDevice = null;
        super.onStop();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    public void calculateBreathRate(){

        Intent intent = new Intent(GLPreviewActivity.this , resultsPreview.class);
        intent.putExtra("resultsList" , avrFrameTemp);
        startActivity(intent);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        /**
         mGrey = new Mat();
         mrgba = new Mat();**/
    }

    @Override
    public void onCameraViewStopped() {
        /**
         mrgba.release();
         mGrey.release();**/
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        return mrgba;
    }


    private BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {

            switch (status){
                case LoaderCallbackInterface.SUCCESS:{


                    InputStream is = getResources().openRawResource(R.raw.haarcascade_mcs_nose);
                    File cascadeDir = getDir("cascade" , Context.MODE_PRIVATE);
                    cascadeFile = new File(cascadeDir , "haarcascade_mcs_nose.xml");

                    FileOutputStream fos = new FileOutputStream(cascadeFile);
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = is.read(buffer)) != -1){
                        fos.write(buffer , 0 , bytesRead);
                    }
                    is.close();
                    fos.close();
                    //is.close();
                    faceDetector = new CascadeClassifier((cascadeFile.getAbsolutePath()));
                    if(faceDetector.empty()){
                        faceDetector=null;
                    }else
                        cascadeDir.delete();
                    // javaCameraView.enableView();
                    //setContentView(thermalSurfaceView);
                    //mrgba  = new Mat();


                }break;

                default:
                {
                    super.onManagerConnected(status);
                }


            }

        }
    };


}
/*
class ImageDetectionFilter {

    // The reference image (this detector's target).
    private final Mat mReferenceImage;
    // Features of the reference image.
    private final MatOfKeyPoint mReferenceKeypoints = new MatOfKeyPoint();
    // Descriptors of the reference image's features.
    private final Mat mReferenceDescriptors = new Mat();
    // The corner coordinates of the reference image, in pixels.
    // CvType defines the color depth, number of channels, and
    // channel layout in the image. Here, each point is represented
    // by two 32-bit floats.
    private final Mat mReferenceCorners = new Mat(4, 1, CvType.CV_32FC2);

    // Features of the scene (the current frame).
    private final MatOfKeyPoint mSceneKeypoints = new MatOfKeyPoint();
    // Descriptors of the scene's features.
    private final Mat mSceneDescriptors = new Mat();

    // pixels.
    private final Mat mCandidateSceneCorners =
            new Mat(4, 1, CvType.CV_32FC2);
    // Good corner coordinates detected in the scene, in pixels.
    private final Mat mSceneCorners = new Mat(4, 1,
            CvType.CV_32FC2);
    // The good detected corner coordinates, in pixels, as integers.
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();
    // A grayscale version of the scene.
    private final Mat mGraySrc = new Mat();
    // Tentative matches of scene features and reference features.
    private final MatOfDMatch mMatches = new MatOfDMatch();
    // A feature detector, which finds features in images.
    private final FeatureDetector mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    // A descriptor extractor, which creates descriptors of
    // features.
    private final DescriptorExtractor mDescriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    // A descriptor matcher, which matches features based on their
    // descriptors.
    private final DescriptorMatcher mDescriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
    // The color of the outline drawn around the detected image.
    private final Scalar mLineColor = new Scalar(0, 255, 0);




    public ImageDetectionFilter(final Context context, final int referenceImageResourceID) throws IOException {
        // Load the reference image from the app's resources.
        // It is loaded in BGR (blue, green, red) format.

      mReferenceImage = Utils.loadResource(context,referenceImageResourceID, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        // Create grayscale and RGBA versions of the reference image.
        final Mat referenceImageGray = new Mat();
        Imgproc.cvtColor(mReferenceImage, referenceImageGray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.cvtColor(mReferenceImage, mReferenceImage, Imgproc.COLOR_BGR2RGBA);
        // Store the reference image's corner coordinates, in pixels.
        mReferenceCorners.put(0, 0, new double[] {0.0, 0.0});

        mReferenceCorners.put(1, 0, new double[] {referenceImageGray.cols(), 0.0});
        mReferenceCorners.put(2, 0, new double[] {referenceImageGray.cols(), referenceImageGray.rows()});
        mReferenceCorners.put(3, 0, new double[] {0.0, referenceImageGray.rows()});
        // Detect the reference features and compute their
        // descriptors.
        mFeatureDetector.detect(referenceImageGray, mReferenceKeypoints);
        mDescriptorExtractor.compute(referenceImageGray, mReferenceKeypoints, mReferenceDescriptors);
    }



    public void apply(final Mat src, final Mat dst) {
        // Convert the scene to grayscale.
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGBA2GRAY);
        // Detect the scene features, compute their descriptors,
        // and match the scene descriptors to reference descriptors.
        mFeatureDetector.detect(mGraySrc, mSceneKeypoints);
        mDescriptorExtractor.compute(mGraySrc, mSceneKeypoints,
                mSceneDescriptors);
        mDescriptorMatcher.match(mSceneDescriptors,
                mReferenceDescriptors, mMatches);
        // Attempt to find the target image's corners in the scene.
        findSceneCorners();
        // If the corners have been found, draw an outline around the
        // target image.
        // Else, draw a thumbnail of the target image. draw(src, dst);
    }

    private void findSceneCorners() {
        List<DMatch> matchesList = mMatches.toList();
        if (matchesList.size() < 4) {
            // There are too few matches to find the homography.
            return;
        }

        List<KeyPoint> referenceKeypointsList =
                mReferenceKeypoints.toList();
        List<KeyPoint> sceneKeypointsList =
                mSceneKeypoints.toList();
        // Calculate the max and min distances between keypoints.
        double maxDist = 0.0;
        double minDist = Double.MAX_VALUE;
        for(DMatch match : matchesList) {
            double dist = match.distance;
            if (dist < minDist) {
                minDist = dist;
            }
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        // The thresholds for minDist are chosen subjectively
        // based on testing. The unit is not related to pixel
        // distances; it is related to the number of failed tests
        // for similarity between the matched descriptors.
        if (minDist > 50.0) {
            // The target is completely lost.
            // Discard any previously found corners.
            mSceneCorners.create(0, 0, mSceneCorners.type());
            return;
        } else if (minDist > 25.0) {
            // The target is lost but maybe it is still close.
            // Keep any previously found corners.
            return;
        }
        // Identify "good" keypoints based on match distance.
        ArrayList<Point> goodReferencePointsList =
                new ArrayList<Point>();
        ArrayList<Point> goodScenePointsList =
                new ArrayList<Point>();
        double maxGoodMatchDist = 1.75 * minDist;
        for(DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodReferencePointsList.add(
                        referenceKeypointsList.get(match.trainIdx).pt);
                goodScenePointsList.add(

                        sceneKeypointsList.get(match.queryIdx).pt);
            }
        }
        if (goodReferencePointsList.size() < 4 ||
                goodScenePointsList.size() < 4) {
            // There are too few good points to find the homography.
            return;
        }
        // There are enough good points to find the homography.
        // (Otherwise, the method would have already returned.)
        // Convert the matched points to MatOfPoint2f format, as
        // required by the Calib3d.findHomography function.
        MatOfPoint2f goodReferencePoints = new MatOfPoint2f();
        goodReferencePoints.fromList(goodReferencePointsList);
        MatOfPoint2f goodScenePoints = new MatOfPoint2f();
        goodScenePoints.fromList(goodScenePointsList);
        // Find the homography.
        Mat homography = Calib3d.findHomography(
                goodReferencePoints, goodScenePoints);
        // Use the homography to project the reference corner
        // coordinates into scene coordinates.
        Core.perspectiveTransform(mReferenceCorners,
                mCandidateSceneCorners, homography);
        // Convert the scene corners to integer format, as required
        // by the Imgproc.isContourConvex function.
        mCandidateSceneCorners.convertTo(mIntSceneCorners,
                CvType.CV_32S);
        // Check whether the corners form a convex polygon. If not,
        // (that is, if the corners form a concave polygon), the
        // detection result is invalid because no real perspective can
        // make the corners of a rectangular image look like a concave
        // polygon!
        if (Imgproc.isContourConvex(mIntSceneCorners)) {
            // The corners form a convex polygon, so record them as
            // valid scene corners.
            mCandidateSceneCorners.copyTo(mSceneCorners);
        }


    }

    protected void draw(Mat src, Mat dst) {
        if (dst != src) {
            src.copyTo(dst);
        }

        if (mSceneCorners.height() < 4) {
            // The target has not been found.
            // Draw a thumbnail of the target in the upper-left
            // corner so that the user knows what it is.
            // Compute the thumbnail's larger dimension as half the
            // video frame's smaller dimension.
            int height = mReferenceImage.height();
            int width = mReferenceImage.width();
            int maxDimension = Math.min(dst.width(),
                    dst.height()) / 2;
            double aspectRatio = width / (double) height;
            if (height > width) {
                height = maxDimension;
                width = (int)(height * aspectRatio);
            } else {
                width = maxDimension;
                height = (int)(width / aspectRatio);
            }
            // Select the region of interest (ROI) where the thumbnail
            // will be drawn.
            Mat dstROI = dst.submat(0, height, 0, width);
            // Copy a resized reference image into the ROI.
            Imgproc.resize(mReferenceImage, dstROI, dstROI.size(),
                    0.0, 0.0, Imgproc.INTER_AREA);
            return;
        }

        // Outline the found target in green.
        Imgproc.line(dst, new Point(mSceneCorners.get(0, 0)),
                new Point(mSceneCorners.get(1, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(1, 0)),
                new Point(mSceneCorners.get(2, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(2, 0)),
                new Point(mSceneCorners.get(3, 0)), mLineColor, 4);
        Imgproc.line(dst, new Point(mSceneCorners.get(3,0)),
                new Point(mSceneCorners.get(0, 0)), mLineColor, 4);

    }

}
*/