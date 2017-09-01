package com.yardspoon.boomshiner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        add(Color.BLACK);
    }};

    private UiDevice device;
    private File screenShotPath;
    private int displayHeight;
    private int displayWidth;
    private File picsDir;

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
        picsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        screenShotPath = new File(picsDir, "screenshot.png");
        Log.d(TAG, "Screenshot path: " + screenShotPath.getPath());

        displayHeight = device.getDisplayHeight();
        displayWidth = device.getDisplayWidth();
    }

    @Test
    public void run_boomshiner() throws Exception {
        Log.i(TAG, "Boomshiner started");

        pause(LONG_PAUSE_TIMEOUT_MS);
        pressPlay(); // Start game

        pressPlay(); // Start level
        pause(SHORT_PAUSE_TIMEOUT_MS);
        time("Take screenshot", this::takeScreenShot);
        time("Analyze screenshot", this::analyzeScreenShot);

        Log.i(TAG, "Boomshiner finished");
    }

    @Test
    public void test_contour_tracing_algorithm() {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(InstrumentationRegistry.getTargetContext().getResources(), R.drawable.test_png, bitmapOptions);

        analyzePositions(bitmap);

        write(bitmap, new File(picsDir, "test_analyzed.png"));

        bitmap.recycle();
    }

    private void analyzeScreenShot() {
        Log.i(TAG, "analyzing screen shots");

        Log.d(TAG, "decoding png");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeFile(screenShotPath.getAbsolutePath(), bitmapOptions);

//        analyzeColors(bitmap);
        analyzePositions(bitmap);

        Log.d(TAG, "writing out png");
        write(bitmap, new File(picsDir, "screenshot_analyzed.png"));

        Log.d(TAG, "recycling mutable bitmap");
        bitmap.recycle();
    }

    private void write(Bitmap bitmap, File file) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file.getPath());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't write out bitmap", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Couldn't close file stream for bitmap", e);
            }
        }
    }

    private void draw(Bitmap bitmap, List<Box> boxes) {
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(false);

        for (Box box : boxes) {
            canvas.drawRect(new Rect(box.x1, box.y1, box.x2, box.y2), paint);
        }
    }

    private void analyzePositions(Bitmap bitmap) {
        Log.i(TAG, "analysing positions");

        List<Box> boxes = new ArrayList<>();

        time("processing pixels", () -> {
            processPixels(bitmap, 2, (x, y, color) -> {
                if (!ignorePixelColors.contains(color)) {

                    boolean alreadyContained = false;
                    for (Box box : boxes) {
                        if (box.contains(x, y)) {
                            alreadyContained = true;
                            break;
                        }
                    }

                    if (!alreadyContained) {
                        Box boundingBox = findBoundingBox(bitmap, x, y);
                        Log.i(TAG, "Found box: " + boundingBox);
                        boxes.add(boundingBox);
                    }
                }
            });
        });

        time("drawing boxes on bitmap", () -> draw(bitmap, boxes));
    }

    private Box findBoundingBox(Bitmap bitmap, Integer startX, Integer startY) {

        int offset = 1;

        // DIRECTIONS
        // 1 2 3
        // 0 * 4
        // 7 6 5

//        Log.v(TAG, "Looking for bounding from " + startX + "," + startY);

        int minX = startX;
        int minY = startY;
        int maxX = startX;
        int maxY = startY;

        int foundX = startX;
        int foundY = startY;
        int direction = 1; // Assume coming in for initial find from left (direction 0), so go to "next"

        do {
            int candidateX = nextX(foundX, direction);
            int candidateY = nextY(foundY, direction);

//            Log.v(TAG, "Candidate: " + candidateX + "," + candidateY + " DIR: " + direction);

            if (foundNextPixel(bitmap, candidateX, candidateY)) {
//                Log.v(TAG, "Found next pixel in border!");
                foundX = candidateX;
                foundY = candidateY;

                direction = flipDirection(direction);

                minX = Math.min(minX, foundX);
                minY = Math.min(minY, foundY);
                maxX = Math.max(maxX, foundX);
                maxY = Math.max(maxY, foundY);
            } else {
//                Log.v(TAG, "Not a match, moving on");
            }

            direction = nextDirection(direction);
        } while (foundX != startX || foundY != startY || direction != 0);

        return new Box(minX - offset, minY - offset, maxX + offset, maxY + offset);
    }

    private boolean foundNextPixel(Bitmap bitmap, int candidateX, int candidateY) {
        return candidateX >= 0 && candidateX < bitmap.getWidth() &&
                candidateY >= 0 && candidateY < bitmap.getHeight() &&
                !ignorePixelColors.contains(bitmap.getPixel(candidateX, candidateY));
    }

    private int flipDirection(int priorDirection) {
        return (priorDirection + 4) % 8;
    }

    private int nextDirection(int priorDirection) {
        return (priorDirection + 1) % 8;
    }

    private int nextY(Integer priorY, int direction) {
        if (direction == 1 || direction == 2 || direction == 3) {
            return priorY - 1;
        }

        if (direction == 5 || direction == 6 || direction == 7) {
            return priorY + 1;
        }

        return priorY;
    }

    private int nextX(Integer priorX, int direction) {
        if (direction == 0 || direction == 1 || direction == 7) {
            return priorX - 1;
        }

        if (direction == 3 || direction == 4 || direction == 5) {
            return priorX + 1;
        }

        return priorX;
    }

    private void processPixels(Bitmap bitmap, int rowSkip, Method<Integer, Integer, Integer> consumer) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        for (int y = 0; y < height; y += rowSkip) {
            for (int x = 0; x < width; x += 1) {
                consumer.call(x, y, bitmap.getPixel(x, y));
            }
        }
    }

    private void analyzeColors(Bitmap bitmap) {
        Map<Integer, Integer> foundColors = new HashMap<>();

        processPixels(bitmap, 2, (x, y, color) -> {
            if (!ignorePixelColors.contains(color)) {
                Integer prior = foundColors.get(color);

                if (prior == null) {
                    prior = 0;
                }

                foundColors.put(color, prior + 1);
            }
        });

        for (Integer pixel : foundColors.keySet()) {
            Integer count = foundColors.get(pixel);
            if (count > 50) {
                Log.d(TAG, "Found significant color: " + pixelColorInHex(pixel) + " " + count + " times -- [" + pixel + "]");
            }
        }
    }

    @NonNull
    private String pixelColorInHex(Integer pixel) {
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

    private interface Method<T1, T2, T3> {
        void call(T1 t1, T2 t2, T3 t3);
    }

    private class Box {

        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;

        private Box(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        private boolean contains(int x, int y) {
            return x > x1 && x < x2 && y > y1 && y < y2;
        }

        @Override
        public String toString() {
            return "Box[" + toPointString(x1, y1) + "," + toPointString(x2, y2) + "]";
        }

        private String toPointString(int x, int y) {
            return "(" + x + "," + y + ")";
        }
    }
}
