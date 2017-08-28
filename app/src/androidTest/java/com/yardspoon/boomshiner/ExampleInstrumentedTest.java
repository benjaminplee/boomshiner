package com.yardspoon.boomshiner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "BOOMSHINER";
    private static final String BOOMSHINE_PACKAGE = "com.bantambytes.android.game.boomshine";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int LONG_PAUSE_TIMEOUT_MS = 10000;
    private static final int SHORT_PAUSE_TIMEOUT_MS = 2000;

    private static final int BACKGROUND_GREEN_PIXEL_COLOR = -16764623;
    private static final int WHITE_TEXT_PIXEL_COLOR = -9204084;
    private static final Set<Integer> ignorePixelColors = new HashSet<Integer>() {{
        add(BACKGROUND_GREEN_PIXEL_COLOR);
        add(WHITE_TEXT_PIXEL_COLOR);
    }};

    private UiDevice device;
    private File screenShotPath;
    private int displayHeight;
    private int displayWidth;

    @Before
    public void setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressHome();

        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT_MS);

        Context context = InstrumentationRegistry.getContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(BOOMSHINE_PACKAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        device.wait(Until.hasObject(By.pkg(BOOMSHINE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);

        Context appContext = InstrumentationRegistry.getTargetContext();
        screenShotPath = new File(appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshot.png");
        Log.d(TAG, "Screenshot path: " + screenShotPath.getPath());

        displayHeight = device.getDisplayHeight();
        displayWidth = device.getDisplayWidth();
    }

    @Test
    public void boomshiner() throws Exception {
        Log.i(TAG, "Boomshiner started");

        pause(LONG_PAUSE_TIMEOUT_MS);
        pressPlay(); // Start game

        pressPlay(); // Start level
        pause(SHORT_PAUSE_TIMEOUT_MS);
        time("Take screenshot", this::takeScreenShot);
        time("Analyze screenshot", this::analyzeScreenShot);

        Log.i(TAG, "Boomshiner finished");
    }

    private void analyzeScreenShot() {
        Bitmap bitmap = BitmapFactory.decodeFile(screenShotPath.getAbsolutePath());

//        analyzeColors(bitmap);
        analyzePositions(bitmap);

        bitmap.recycle();
    }

    private void analyzePositions(Bitmap bitmap) {
        processPixels(bitmap, 5, x -> {
//            Log.d(TAG, x.toString());
        });
    }

    private void processPixels(Bitmap bitmap, int skip, Method<Integer> consumer) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        for (int x = 0; x < width; x += skip) {
            for (int y = 0; y < height; y += skip) {
                consumer.call(bitmap.getPixel(x, y));
            }
        }
    }

    private void analyzeColors(Bitmap bitmap) {
        Map<Integer, Integer> foundColors = new HashMap<>();

        processPixels(bitmap, 2, pixel -> {
            if (!ignorePixelColors.contains(pixel)) {
                Integer prior = foundColors.get(pixel);

                if (prior == null) {
                    prior = 0;
                }

                foundColors.put(pixel, prior + 1);
            }
        });

        for (Integer pixel : foundColors.keySet()) {
            Integer count = foundColors.get(pixel);
            if (count > 50) {
                Log.d(TAG, "Found significant color: " + pixelColorInHex(pixel) + " " + count + " times -- [" + pixel + "]");
            }
        }
    }

    @NonNull private String pixelColorInHex(Integer pixel) {
        String red = Integer.toHexString(Color.red(pixel)).toUpperCase();
        String green = Integer.toHexString(Color.green(pixel)).toUpperCase();
        String blue = Integer.toHexString(Color.blue(pixel)).toUpperCase();

        return "[" + red + "|" + green + "|" + blue + "]";
    }

    private void pressPlay() {
        Log.d(TAG, "Pressing play");
        device.swipe(displayWidth / 2, 2 * displayHeight / 3, displayWidth / 2 + 5, 2 * displayHeight / 3 + 5, 2);
    }

    // adb pull /storage/emulated/0/Android/data/com.yardspoon.boomshiner/files/Pictures/screenshot.png
    private void takeScreenShot() {
        if (screenShotPath.exists()) {
            Log.d(TAG, "Removing old screenshot file");
            screenShotPath.delete();
        }

        device.takeScreenshot(screenShotPath);
        Log.d(TAG, "Screenshot file created");
    }

    private static void pause(long targetMs) {
        time("Pause for " + targetMs + "ms", () -> SystemClock.sleep(targetMs));
    }

    private static void time(String msg, Runnable action) {
        long before = System.nanoTime();
        action.run();
        long after = System.nanoTime();
        long elapsed = (after - before) / 1000 / 1000; // convert to ms
        Log.d(TAG, "Time: [" + msg + "] took " + elapsed + "ms");
    }

    private interface Method<T> {
        public void call(T t);
    }
}
