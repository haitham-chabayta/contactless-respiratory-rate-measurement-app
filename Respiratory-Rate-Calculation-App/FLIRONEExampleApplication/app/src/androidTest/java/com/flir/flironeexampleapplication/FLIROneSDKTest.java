package com.flir.flironeexampleapplication;

import android.content.Context;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.flir.flironesdk.Device;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;
import com.flir.flironesdk.SimulatedDevice;

import org.junit.After;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;

import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FLIROneSDKTest extends InstrumentationTestCase {

    private Device flirOne;
    final Context context = InstrumentationRegistry.getTargetContext();

    public void connectToDevice(boolean physical) {
        //Clean up if needed
        cleanup();

        final Object discoveryLock = new Object();

        synchronized (discoveryLock) {
            Device.Delegate discoveryDelegate = new Device.Delegate() {
                @Override
                public void onTuningStateChanged(Device.TuningState newTuningState) { }
                @Override
                public void onAutomaticTuningChanged(boolean deviceWillTuneAutomatically) { }
                @Override
                public void onDeviceDisconnected(Device device) { }

                @Override
                public void onDeviceConnected(Device device) {
                    synchronized (discoveryLock) {
                        flirOne = device;
                        discoveryLock.notify();
                    }
                }
            };

            if (physical) {
                Device.startDiscovery(context, discoveryDelegate);
            }
            else {
                InputStream frames = context.getResources().openRawResource(R.raw.sampleframes);
                try {
                    flirOne = new SimulatedDevice(discoveryDelegate, context, frames, 10);
                } catch (Exception e) {
                    fail("Couldn't create simulated device");
                }
            }

            synchronized (discoveryLock) {
                try {
                    discoveryLock.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertNotNull(flirOne);
            }
        }
    }

    @Test
    public void testConnectPhysical() {
        connectToDevice(true);
        assertNotNull("Physical device not found", flirOne);
    }

    @Test
    public void testConnectSimulated() {
        connectToDevice(false);
        assertNotNull("Simulated device not found", flirOne);
    }

    public Frame getSingleFrame() {
        //Hack to allow a mutable final variable.
        final Frame[] frame = {null};
        final Object lock = new Object();
        flirOne.startFrameStream(new Device.StreamDelegate() {
            @Override
            public void onFrameReceived(Frame recieved) {
                assertNotNull("Processed frame cannot be null", recieved);
                synchronized (frame) {
                    frame[0] = recieved;
                    frame.notify();
                }
            }
        });

        synchronized (frame) {
            try {
                frame.wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertNotNull("Timed out while waiting for frames", frame[0]);
        }

        flirOne.stopFrameStream();
        return frame[0];
    }

    @Test
    public void testGetFramePhysical() {
        connectToDevice(true);
        Assume.assumeNotNull(flirOne);

        assertNotNull(getSingleFrame());
    }

    @Test
    public void testGetFrameSimulated() {
        connectToDevice(false);
        Assume.assumeNotNull(flirOne);

        assertNotNull(getSingleFrame());
    }

    public RenderedImage processFrame(final Frame frame, final RenderedImage.ImageType imageType) throws InterruptedException {
        //This object is used to notify the main thread that we have a frame. Array hack allows a mutable final variable.
        final RenderedImage[] renderedFrame = {null};
        final int[] framesReceived = {0};

        final FrameProcessor.Delegate delegate = new FrameProcessor.Delegate() {
            @Override
            public void onFrameProcessed(RenderedImage renderedImage) {
                assertNotNull("A processed frame cannot be null", renderedImage);

                assertTrue(renderedImage.width() > 0);
                assertTrue(renderedImage.height() > 0);
                assertTrue(renderedImage.pixelData().length > 0);
                if (imageType == RenderedImage.ImageType.ThermalLinearFlux14BitImage || imageType == RenderedImage.ImageType.ThermalRadiometricKelvinImage){
                    assertEquals(renderedImage.height() * renderedImage.width(), renderedImage.thermalPixelData().length);
                } else if (imageType == RenderedImage.ImageType.VisibleUnalignedYUV888Image) {
                    // 888 = 3 bytes per pixel
                    assertEquals(renderedImage.height() * renderedImage.width() * 3, renderedImage.pixelData().length);
                } else if (imageType == RenderedImage.ImageType.VisualJPEGImage) {
                    assertTrue(0 < renderedImage.pixelData().length);
                } else {
                    // all other images are 4 bytes per pixel
                    assertEquals(renderedImage.height() * renderedImage.width() * 4, renderedImage.pixelData().length);
                }

                assertEquals("Image type doesn't match requested image type", imageType, renderedImage.imageType());

                //Let the main thread know we have a frame.
                synchronized (renderedFrame) {
                    framesReceived[0] ++;
                    renderedFrame[0] = renderedImage;
                    renderedFrame.notify();
                }
            }
        };

        FrameProcessor frameProcessor = new FrameProcessor(context, delegate, EnumSet.of(imageType));

        final int FRAMES_TO_SEND = 5;
        for (int i = 0; i < FRAMES_TO_SEND; i ++) {
            frameProcessor.processFrame(frame);
        }

        final long waitStart = System.currentTimeMillis();

        synchronized (renderedFrame) {
            while (framesReceived[0] < FRAMES_TO_SEND) {
                if ((waitStart + 10000) < System.currentTimeMillis()) {
                    throw new RuntimeException("timeout exceeded");
                }
                renderedFrame.wait(200);
            }
        }

        assertNotNull("Timeout while processing frame", renderedFrame[0]);
        assertEquals("Not all frames processed", FRAMES_TO_SEND, framesReceived[0]);

        assertTrue("Zero width image for type:" + renderedFrame[0].imageType(), 0 < renderedFrame[0].width());
        assertTrue("Zero width image for type:" + renderedFrame[0].imageType(), 0 < renderedFrame[0].width());

        assertEquals("Image type doesn't match requested image type", imageType, renderedFrame[0].imageType());
        return renderedFrame[0];
    }

    public void testProcessFrame(boolean physical) throws InterruptedException {
        connectToDevice(physical);
        Assume.assumeNotNull(flirOne);

        Frame frame = getSingleFrame();
        Assume.assumeNotNull(frame);

        //Test every kind of frame processor.
        for (RenderedImage.ImageType type : EnumSet.allOf(RenderedImage.ImageType.class)) {
            processFrame(frame, type);
        }
    }

    @Test
    public void testProcessFramePhysical() throws InterruptedException {
        testProcessFrame(true);
    }

    @Test
    public void testProcessFrameSimulated() throws InterruptedException {
        testProcessFrame(false);
    }

    public void doThreadedSaveTests(Frame frame, final boolean doAsserts) throws Throwable {
        ArrayList<AsyncTask> tasks = new ArrayList<AsyncTask>();

        final FrameProcessor processor = new FrameProcessor(context, null, EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image));
        processor.setImagePalette(RenderedImage.Palette.Iron);

        for (int i = 0; i < 10; i++) {
            AsyncTask<Frame, Void, Void> task = new AsyncTask<Frame, Void, Void>() {
                @Override
                protected Void doInBackground(Frame... frame) {
                    try {
                        File file = File.createTempFile("FLIROneSDKUnitTest", ".jpg");
                        file.createNewFile();
                        frame[0].save(file, processor);
                    } catch (IOException e) {
                        fail("IO failure");
                    }
                    return null;
                }
            };
            task.execute(frame);
            tasks.add(task);
        }
    }

    @Test
    public void testWriteFrameStream() throws IOException {
        connectToDevice(false);
        Assume.assumeNotNull(flirOne);

        FrameProcessor processor = new FrameProcessor(context, null, null);

        final Frame[] frame = {null};
        flirOne.startFrameStream(new Device.StreamDelegate() {
            @Override
            public void onFrameReceived(Frame recieved) {
                assertNotNull("Processed frame cannot be null", recieved);
                synchronized (frame) {
                    Log.d("flir", "got frame");
                    frame[0] = recieved;
                }
            }
        });

        Frame tmpFrame = null;
        synchronized (frame) {
            try {
                frame.wait(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertNotNull(frame[0]);
            tmpFrame = frame[0];
            frame[0] = null;
        }
        int saveIterations = 0;
        while (saveIterations < 100) {
            for (RenderedImage.Palette palette : EnumSet.allOf(RenderedImage.Palette.class)) {
                synchronized (frame) {
                    if (frame[0] != null) {
                        tmpFrame = frame[0];
                        frame[0] = null;
                    }
                }
                File file = File.createTempFile("FLIROneSDKUnitTest", ".jpg");
                file.createNewFile();
                Log.d("flir", "saving");
                tmpFrame.save(file, processor);
                saveIterations++;
            }
        }
    }

    @Test
    public void testWriteFrameThreaded() throws Throwable {
        connectToDevice(false);
        Assume.assumeNotNull(flirOne);
        Frame frame = getSingleFrame();
        RenderedImage image = processFrame(frame, RenderedImage.ImageType.BlendedMSXRGBA8888Image);
        doThreadedSaveTests(image.getFrame(), true);
    }

    @Test
    public void testWriteFrameSimple() throws IOException {
        //Write a single frame with a single frame processor. The easiest of the frame writing tests.

        connectToDevice(false);
        Assume.assumeNotNull(flirOne);

        File file = File.createTempFile("FLIROneSDKUnitTest", ".jpg");
        file.createNewFile();

        FrameProcessor processor = new FrameProcessor(context, null, null);

        getSingleFrame().save(file, processor);

        assertNotEquals("File size can't be zero", file.length(), 0);
    }

    @After
    public void cleanup() {
        if (flirOne != null) {
            flirOne.stopFrameStream();
            flirOne.close();
            flirOne = null;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }
        }
        Device.stopDiscovery();
    }
}
