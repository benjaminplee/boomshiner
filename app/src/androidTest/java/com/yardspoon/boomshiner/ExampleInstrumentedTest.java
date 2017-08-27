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
import java.util.HashSet;
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
        takeScreenShot();
        analyzeScreenShot();

        Log.i(TAG, "Boomshiner finished");
    }

    private void analyzeScreenShot() {
        Bitmap bitmap = BitmapFactory.decodeFile(screenShotPath.getAbsolutePath());

        Set<Integer> foundColors = new HashSet<>();

        for (int x = 0; x < displayWidth; x++) {
            for (int y = 0; y < displayHeight; y++) {
                foundColors.add(bitmap.getPixel(x, y));
            }
        }

        for (Integer pixel : foundColors) {
            Log.d(TAG, "Found color: " + pixelColorInHex(pixel));
        }

        bitmap.recycle();
    }

    @NonNull private String pixelColorInHex(Integer pixel) {
        String alpha = Integer.toHexString(Color.alpha(pixel)).toUpperCase();
        String red = Integer.toHexString(Color.red(pixel)).toUpperCase();
        String green = Integer.toHexString(Color.green(pixel)).toUpperCase();
        String blue = Integer.toHexString(Color.blue(pixel)).toUpperCase();

        return "[" + alpha + "|" + red + "|" + green + "|" + blue + "]";
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
        Log.d(TAG, "Pausing for " + targetMs + "ms");

        long realms = time(() -> SystemClock.sleep(targetMs));

        Log.v(TAG, "It really took " + realms + "ms");
    }

    private static long time(Runnable action) {
        long before = System.nanoTime();
        action.run();
        long after = System.nanoTime();
        return (after - before) / 1000 / 1000; // convert to ms
    }
}
