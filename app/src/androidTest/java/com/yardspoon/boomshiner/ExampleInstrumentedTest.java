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
import android.util.SparseIntArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "BOOMSHINER";
    private static final String BOOMSHINE_PACKAGE = "com.bantambytes.android.game.boomshine";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int MEDIUM_PAUSE_TIMEOUT_MS = 2000;
    private static final int LONG_PAUSE_TIMEOUT_MS = 8000;

    private static final int SHORT_PAUSE_TIMEOUT_MS = 1000;
    private static final int BACKGROUND_GREEN_PIXEL_COLOR = -16764623;
    private static final int WHITE_TEXT_PIXEL_COLOR = -9204084;

    private UiDevice device;
    private List<Screenshot> screenshots;
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
        if (intent == null) {
            throw new RuntimeException("Boomshine not installed");
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        device.wait(Until.hasObject(By.pkg(BOOMSHINE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);

        Context appContext = InstrumentationRegistry.getTargetContext();
        picsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "Pics Dir: " + picsDir.getPath());
        screenshots = new ArrayList<>();

        displayHeight = device.getDisplayHeight();
        displayWidth = device.getDisplayWidth();
    }

    @Test
    public void run_boomshiner() throws Exception {
        Log.i(TAG, "Boomshiner started");

        pause(LONG_PAUSE_TIMEOUT_MS); // wait for game to load and get past initial screens
        pressPlay(); // Start game
        pause(MEDIUM_PAUSE_TIMEOUT_MS); // wait for screen to fully display
        pressPlay(); // Start level
        pause(MEDIUM_PAUSE_TIMEOUT_MS); // wait for screen to fully display
        time("Take screenshot", this::takeScreenShot);
        pause(SHORT_PAUSE_TIMEOUT_MS);
        time("Take screenshot", this::takeScreenShot);
        pause(SHORT_PAUSE_TIMEOUT_MS);
        time("Take screenshot", this::takeScreenShot);

        List<Finding> findings = new ArrayList<>();
        for (Screenshot screenshot : screenshots) {
            time("Review screenshot", () -> findings.add(reviewScreenshot(screenshot)));
        }

        time("Analyze findings took", () -> analyze(findings));

        Log.i(TAG, "Boomshiner finished");
    }

    private void analyze(List<Finding> findings) {
        Log.i(TAG, "Analyzing findings");

        List<Box> firstSamples = findings.get(0).likelyTargets;
        List<Box> secondSamplesCopy = new ArrayList<>(findings.get(1).likelyTargets);

        List<Vector> vectors = new ArrayList<>(firstSamples.size());

        for (Box firstSample : firstSamples) {
            List<Box> matchingColors = new ArrayList<>();

            for (Box secondSample : secondSamplesCopy) {
                if (secondSample.maxColor == firstSample.maxColor) {
                    matchingColors.add(secondSample);
                }
            }

            if (matchingColors.isEmpty()) {
                Log.w(TAG, "Unable to match sample " + firstSample + " to anything in second sample");
            } else {
                Box closestMatch = Collections.min(matchingColors, (box1, box2) -> ((Double) box1.distanceTo(box2)).intValue());
                Log.d(TAG, "Found best match for " + firstSample + " as " + closestMatch + " with distance " + firstSample.distanceTo(closestMatch));
                secondSamplesCopy.remove(closestMatch);
                vectors.add(firstSample.vectorTo(closestMatch));
            }
        }

        
    }

    @Test
    public void test_contour_tracing_algorithm() {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(InstrumentationRegistry.getTargetContext().getResources(), R.drawable.test_png, bitmapOptions);

        analyzePositions(bitmap, new Finding(null));

        write(bitmap, new File(picsDir, "test_analyzed.png"));

        bitmap.recycle();
    }

    private Finding reviewScreenshot(Screenshot screenshot) {
        Log.i(TAG, "reviewing screen shots");

        Finding finding = new Finding(screenshot);
        Bitmap bitmap = getBitmap(screenshot.file);

//        analyzeColors(bitmap);
        analyzePositions(bitmap, finding);

        Log.d(TAG, "writing out png");
        write(bitmap, screenshot.analyzedFile);

        Log.d(TAG, "recycling mutable bitmap");
        bitmap.recycle();

        return finding;
    }

    private Bitmap getBitmap(File file) {
        Log.d(TAG, "decoding image");
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inScaled = false;
        bitmapOptions.inPurgeable = true;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);
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

    private void draw(Bitmap bitmap, List<Box> boxes, int color) {
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setAntiAlias(false);

        for (Box box : boxes) {
            canvas.drawRect(new Rect(box.x1, box.y1, box.x2, box.y2), paint);
        }
    }

    private void analyzePositions(Bitmap bitmap, Finding finding) {
        Log.i(TAG, "analysing positions");

        time("processing pixels", () -> {
            int rowSkip = 10;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            for (int y = 0; y < height; y += rowSkip) {
                for (int x = 0; x < width; x += 1) {
                    int color = bitmap.getPixel(x, y);

                    if (!isBackground(color)) {

                        boolean alreadyContained = false;
                        for (Box box : finding.boxes) {
                            if (box.contains(x, y)) {
                                alreadyContained = true;
                                break;
                            }
                        }

                        if (!alreadyContained) {
                            Box boundingBox = findBoundingBox(bitmap, x, y);
                            Log.i(TAG, "Found box: " + boundingBox);
                            finding.boxes.add(boundingBox);
                        }
                    }
                }
            }
        });

        time("Reviewing finding", finding::review);
        time("drawing likely target boxes on bitmap", () -> draw(bitmap, finding.likelyTargets, Color.WHITE));
        time("drawing other boxes target boxes on bitmap", () -> draw(bitmap, finding.unLikelyTargets, Color.RED));
    }

    private boolean isBackground(int color) {
        return color == BACKGROUND_GREEN_PIXEL_COLOR || color == WHITE_TEXT_PIXEL_COLOR || color == Color.BLACK;
    }

    private Box findBoundingBox(Bitmap bitmap, int startX, int startY) {

        int offset = 1;

        // DIRECTIONS
        // 1 2 3
        // 0 * 4
        // 7 6 5

//        Log.v(TAG, "Looking for bounding from " + startX + "," + startY);

        SparseIntArray colors = new SparseIntArray(32);

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

            int color = bitmap.getPixel(candidateX, candidateY);

            if (foundNextPixel(bitmap, candidateX, candidateY, color)) {
//                Log.v(TAG, "Found next pixel in border!");
                foundX = candidateX;
                foundY = candidateY;

                direction = flipDirection(direction);

                minX = Math.min(minX, foundX);
                minY = Math.min(minY, foundY);
                maxX = Math.max(maxX, foundX);
                maxY = Math.max(maxY, foundY);

                colors.put(color, colors.get(color, 0) + 1);
            }

            direction = nextDirection(direction);
        } while (foundX != startX || foundY != startY || direction != 0);

        int maxColorCount = 0;
        int maxColor = 0;
        for (int i = 0; i < colors.size(); i++) {
            int colorCount = colors.valueAt(i);
            if (colorCount > maxColorCount) {
                maxColorCount = colorCount;
                maxColor = colors.keyAt(i);
            }
        }

        return new Box(minX - offset, minY - offset, maxX + offset, maxY + offset, maxColor);
    }

    private boolean foundNextPixel(Bitmap bitmap, int candidateX, int candidateY, int color) {
        return candidateX >= 0 && candidateX < bitmap.getWidth() &&
                candidateY >= 0 && candidateY < bitmap.getHeight() &&
                !isBackground(color);
    }

    private int flipDirection(int priorDirection) {
        return (priorDirection + 4) % 8;
    }

    private int nextDirection(int priorDirection) {
        return (priorDirection + 1) % 8;
    }

    private int nextY(int priorY, int direction) {
        if (direction == 1 || direction == 2 || direction == 3) {
            return priorY - 1;
        }

        if (direction == 5 || direction == 6 || direction == 7) {
            return priorY + 1;
        }

        return priorY;
    }

    private int nextX(int priorX, int direction) {
        if (direction == 0 || direction == 1 || direction == 7) {
            return priorX - 1;
        }

        if (direction == 3 || direction == 4 || direction == 5) {
            return priorX + 1;
        }

        return priorX;
    }

//    private void processPixels(Bitmap bitmap, int rowSkip, Method<Integer, Integer, Integer> consumer) {
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//
//        for (int y = 0; y < height; y += rowSkip) {
//            for (int x = 0; x < width; x += 1) {
//                consumer.call(x, y, bitmap.getPixel(x, y));
//            }
//        }
//    }

//    private void analyzeColors(Bitmap bitmap) {
//        Map<Integer, Integer> foundColors = new HashMap<>();
//
//        processPixels(bitmap, 2, (x, y, color) -> {
//            if (!isBackground(color)) {
//                Integer prior = foundColors.get(color);
//
//                if (prior == null) {
//                    prior = 0;
//                }
//
//                foundColors.put(color, prior + 1);
//            }
//        });
//
//        for (Integer pixel : foundColors.keySet()) {
//            Integer count = foundColors.get(pixel);
//            if (count > 50) {
//                Log.d(TAG, "Found significant color: " + pixelColorInHex(pixel) + " " + count + " times -- [" + pixel + "]");
//            }
//        }
//    }

//    @NonNull
//    private String pixelColorInHex(Integer pixel) {
//        String red = Integer.toHexString(Color.red(pixel)).toUpperCase();
//        String green = Integer.toHexString(Color.green(pixel)).toUpperCase();
//        String blue = Integer.toHexString(Color.blue(pixel)).toUpperCase();
//
//        return "[" + red + "|" + green + "|" + blue + "]";
//    }

    private void pressPlay() {
        Log.d(TAG, "Pressing play");
        device.swipe(displayWidth / 2, 2 * displayHeight / 3, displayWidth / 2 + 5, 2 * displayHeight / 3 + 5, 2);
    }

    // adb pull /storage/emulated/0/Android/data/com.yardspoon.boomshiner/files/Pictures/screenshot.png
    private void takeScreenShot() {
        screenshots.add(new Screenshot(picsDir, device));
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

//    private interface Method<T1, T2, T3> {
//        void call(T1 t1, T2 t2, T3 t3);
//    }

    private class Box {

        private final int likelyTargetSize = 50;
        private final int likelyTargetMin = 20; // less than half; in case we are on edge
        private final int likelyTargetMax = 250; // 5 wide; may need to bump up for higher levels

        final int x1;
        final int y1;
        final int x2;
        final int y2;
        final int maxColor;
        final int cx;
        final int cy;

        private Box(int x1, int y1, int x2, int y2, int maxColor) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.cx = x2 - x1;
            this.cy = y2 - y1;
            this.maxColor = maxColor;
        }

        private boolean contains(int x, int y) {
            return x > x1 && x < x2 && y > y1 && y < y2;
        }

        @Override
        public String toString() {
            return "Box[" + toPointString(x1, y1) + "," + toPointString(x2, y2) + "," + isLikelyTarget() + "," + maxColor + "]";
        }

        private String toPointString(int x, int y) {
            return "(" + x + "," + y + ")";
        }

        boolean isLikelyTarget() {
            int width = x2 - x1;
            int height = y2 - y1;

            return width > likelyTargetMin && width < likelyTargetMax && height > likelyTargetMin && height < likelyTargetMax;
        }

        double distanceTo(Box other) {
            return Math.sqrt(Math.pow(Math.abs(cx - other.cx), 2) + Math.pow(Math.abs(cy - other.cy), 2));
        }

        Vector vectorTo(Box other) {
            return new Vector(other.cx - cx, other.cy - cy);
        }
    }

    private class Vector {
        final int deltaX;
        final int deltaY;

        private Vector(int deltaX, int deltaY) {
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }
    }

    private class Screenshot {
        final long nanoTime;
        final File file;
        final File reviewedFile;
        final File analyzedFile;

        private Screenshot(File base, UiDevice device) {
            this.nanoTime = System.nanoTime();
            this.file = new File(base, getFileName());
            device.takeScreenshot(file);
            this.reviewedFile = new File(base, getReviewedFilename());
            this.analyzedFile = new File(base, getAnalyzedFilename());
        }

        @NonNull
        String getFileName() {
            return "screenshot_" + String.valueOf(nanoTime) + ".bmp";
        }

        @NonNull
        String getReviewedFilename() {
            return "screenshot_" + String.valueOf(nanoTime) + "_reviewed.bmp";
        }

        @NonNull
        String getAnalyzedFilename() {
            return "screenshot_" + String.valueOf(nanoTime) + "_analyzed.bmp";
        }
    }

    private class Finding {
        final List<Box> boxes;
        final List<Box> likelyTargets;
        final List<Box> unLikelyTargets;
        final Screenshot screenshot;

        public Finding(Screenshot screenshot) {
            this.screenshot = screenshot;
            boxes = new ArrayList<>(64);
            likelyTargets = new ArrayList<>(64);
            unLikelyTargets = new ArrayList<>(64);
        }

        void review() {
            for (Box box : boxes) {
                if (box.isLikelyTarget()) {
                    likelyTargets.add(box);
                } else {
                    unLikelyTargets.add(box);
                }
            }
        }
    }
}
