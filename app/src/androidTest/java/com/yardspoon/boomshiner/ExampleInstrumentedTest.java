package com.yardspoon.boomshiner;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.SystemClock;
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
    }

    @Test
    public void boomshiner() throws Exception {
        Log.i(TAG, "Boomshiner started");

        pause(LONG_PAUSE_TIMEOUT_MS);

        takeScreenShot();

        Log.i(TAG, "Boomshiner finished");
    }

    // adb pull /storage/emulated/0/Android/data/com.yardspoon.boomshiner/files/Pictures/screenshot.png
    private void takeScreenShot() {
        if(screenShotPath.exists()) {
            Log.d(TAG, "Removing old screenshot file");
            screenShotPath.delete();
        }

        device.takeScreenshot(screenShotPath);
        Log.d(TAG, "Screenshot file created");
    }

    private static void pause(long ms) {
        SystemClock.sleep(ms);
    }

    private static long time(Runnable action) {
        long before = System.nanoTime();
        action.run();
        long after = System.nanoTime();
        return (after - before) / 1000 / 1000; // convert to ms
    }
}
