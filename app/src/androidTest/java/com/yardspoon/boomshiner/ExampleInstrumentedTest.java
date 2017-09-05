package com.yardspoon.boomshiner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static com.yardspoon.boomshiner.Utils.pixelColorInHex;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String BOOMSHINE_PACKAGE = "com.bantambytes.android.game.boomshine";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int MEDIUM_PAUSE_TIMEOUT_MS = 2000;
    private static final int LONG_PAUSE_TIMEOUT_MS = 8000;
    private static final int SHORT_PAUSE_TIMEOUT_MS = 10;

    private static final int BACKGROUND_GREEN_PIXEL_COLOR = -16764623;
    private static final int WHITE_TEXT_PIXEL_COLOR = -9204084;

    private int displayHeight;
    private int displayWidth;
    private int softButtonHeight;
    private int effectiveDisplayHeight;

    private List<Screenshot> screenshots;
    private UiDevice device;
    private File picsDir;
    private Context appContext;

    private final boolean startFromScratch = false;

    @Before
    public void setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        if (startFromScratch) {
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
        }

        device.wait(Until.hasObject(By.pkg(BOOMSHINE_PACKAGE).depth(0)), LAUNCH_TIMEOUT_MS);

        appContext = InstrumentationRegistry.getTargetContext();
        picsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Timber.d("Pics Dir: %s", picsDir.getPath());
        screenshots = new ArrayList<>();

        displayHeight = device.getDisplayHeight();
        displayWidth = device.getDisplayWidth();
        softButtonHeight = getSoftButtonsBarHeight();
        effectiveDisplayHeight = displayHeight - softButtonHeight;

        Timber.d("Dimensions: %s x %s", displayWidth, displayHeight);
    }

    @Test
    public void test_colors() {
        getIntoTheGame();

        takeScreenShot();
        Bitmap bitmap = Images.getBitmap(screenshots.get(0).file);

        Map<Integer, Integer> foundColors = new HashMap<>();

        int rowSkip = 5;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        for (int y = 0; y < height; y += rowSkip) {
            for (int x = 0; x < width; x += 1) {
                int color = bitmap.getPixel(x, y);

                if (!isBackground(color)) {
                    Integer prior = foundColors.get(color);

                    if (prior == null) {
                        prior = 0;
                    }

                    foundColors.put(color, prior + 1);
                }
            }
        }

        for (Integer pixel : foundColors.keySet()) {
            Integer count = foundColors.get(pixel);
            if (count > 50) {
                Timber.d("Found significant color: " + pixelColorInHex(pixel) + " " + count + " times -- [" + pixel + "]");
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

        Images.write(bitmap, new File(picsDir, "test_analyzed.png"));

        bitmap.recycle();
    }

    @Test
    public void run_boomshiner() throws Exception {
        Timber.i("Boomshiner started");

        if(startFromScratch) {
            getIntoTheGame();
        }

        Utils.time("Take screenshot", this::takeScreenShot);
        Utils.pause(SHORT_PAUSE_TIMEOUT_MS);
        Utils.time("Take screenshot", this::takeScreenShot);
        Utils.pause(SHORT_PAUSE_TIMEOUT_MS);
        Utils.time("Take screenshot", this::takeScreenShot);

        List<Finding> findings = new ArrayList<>();
        for (Screenshot screenshot : screenshots) {
            Utils.time("Review screenshot", () -> findings.add(reviewScreenshot(screenshot)));
        }

        Utils.time("Analyze findings took", () -> analyze(findings));

        Timber.i("Boomshiner finished");
    }

    private void getIntoTheGame() {
        Utils.pause(LONG_PAUSE_TIMEOUT_MS); // wait for game to load and get past initial screens
        pressPlay(); // Start game
        Utils.pause(MEDIUM_PAUSE_TIMEOUT_MS); // wait for screen to fully display
        pressPlay(); // Start level
        Utils.pause(MEDIUM_PAUSE_TIMEOUT_MS); // wait for screen to fully display
    }

    private void analyze(List<Finding> findings) {
        Timber.i("Analyzing findings");

        List<Box> firstSamples = findings.get(0).likelyTargets;
        List<Box> secondSamples = findings.get(1).likelyTargets;
        List<Box> secondSamplesCopy = new ArrayList<>(secondSamples);

        Finding thirdFinding = findings.get(2);
        Bitmap third = Images.getBitmap(thirdFinding.screenshot.file);

        List<Match> matches = new ArrayList<>(firstSamples.size());

        for (Box firstSample : firstSamples) {
//            for (Box secondSample : secondSamples) {
//                matches.add(new Match(firstSample, secondSample));
//            }

            List<Box> matchingColors = new ArrayList<>();

            for (Box secondSample : secondSamplesCopy) {
                if (secondSample.maxColor == firstSample.maxColor) {
                    matches.add(new Match(firstSample, secondSample));
                    matchingColors.add(secondSample);
                }
            }
//
//            if (matchingColors.isEmpty()) {
//                Log.w(TAG, "Unable to match sample " + firstSample + " to anything in second sample");
//            } else {
//                Log.d(TAG, "Found match ...");
////                Log.d(TAG, Arrays.toString(matchingColors.toArray()));
////                Box closestMatch = Collections.min(matchingColors, (box1, box2) -> (int)(firstSample.distanceTo(box1) - firstSample.distanceTo(box2)));
//
//                for (Box matchingColor : matchingColors) {
//                    Match match = new Match(firstSample, matchingColor);
//                    Log.d(TAG, "  Possible match: " + match);
////                    secondSamplesCopy.remove(closestMatch);
//                    matches.add(match);
//                }

//                Match match = new Match(firstSample, closestMatch);
//                Log.d(TAG, "Found match: " + match);
//                secondSamplesCopy.remove(closestMatch);
//                matches.add(match);
//            }
        }

        Images.draw(third, firstSamples);
        Images.draw(third, secondSamples);

        long firstSecondDelta = findings.get(1).screenshot.nanoTime - findings.get(0).screenshot.nanoTime;
        long secondThirdDelta = findings.get(2).screenshot.nanoTime - findings.get(1).screenshot.nanoTime;
        double multiplier = (double) secondThirdDelta / (double) firstSecondDelta;

        for (Match match : matches) {
            Images.draw(third, match.start, match.vector, Color.GRAY);
            Images.draw(third, match.end, match.vector.multiply(multiplier), Color.YELLOW);
        }

        Timber.d("writing out third image");
        Images.write(third, thirdFinding.screenshot.reviewedFile);

        Timber.d("recycling mutable bitmap");
        third.recycle();
    }

    private Finding reviewScreenshot(Screenshot screenshot) {
        Timber.i("reviewing screen shots");

        Finding finding = new Finding(screenshot);
        Bitmap bitmap = Images.getBitmap(screenshot.file);

        analyzePositions(bitmap, finding);

        Timber.d("writing out png");
        Images.write(bitmap, screenshot.analyzedFile);

        Timber.d("recycling mutable bitmap");
        bitmap.recycle();

        return finding;
    }

    private void analyzePositions(Bitmap bitmap, Finding finding) {
        Timber.i("analysing positions");

        Utils.time("processing pixels", () -> {
            int rowSkip = 10;

            for (int y = 0; y < effectiveDisplayHeight; y += rowSkip) {
//                Timber.v("ANALYZING ROW: %s", y);

                // Skip row if starting with black since it is the buttons area
                if (bitmap.getPixel(0, y) == Color.BLACK) {
                    Timber.v("Skipping row %s since it starts with black and is probably the menu buttons.", y);
                    break;
                }

                for (int x = 0; x < displayWidth; x += 1) {
                    int color = bitmap.getPixel(x, y);

//                    Timber.v("PIXEL (%s,%s:%s)", x, y, color);

                    if (!isBackground(color)) {
                        boolean alreadyContained = false;
                        for (Box box : finding.boxes) {
                            if (box.contains(x, y)) {
                                alreadyContained = true;
                                break;
                            }
                        }

                        if (!alreadyContained) {
                            Box boundingBox = findBoundingBox(bitmap, x, y, displayWidth, effectiveDisplayHeight);
                            Timber.i("Found box: %s", boundingBox);
                            finding.boxes.add(boundingBox);
                        }
                    }
                }
            }
        });

        Utils.time("Reviewing finding", finding::review);
        Utils.time("drawing likely target boxes on bitmap", () -> Images.draw(bitmap, finding.likelyTargets));
        Utils.time("drawing other boxes target boxes on bitmap", () -> Images.draw(bitmap, finding.unLikelyTargets));
    }

    public static boolean isBackground(int color) {
        return color == BACKGROUND_GREEN_PIXEL_COLOR || color == WHITE_TEXT_PIXEL_COLOR || color == Color.BLACK;
    }

    private void pressPlay() {
        Timber.d("Pressing play");
        device.swipe(displayWidth / 2, 2 * displayHeight / 3, displayWidth / 2 + 5, 2 * displayHeight / 3 + 5, 2);
    }

    private void takeScreenShot() {
        screenshots.add(new Screenshot(picsDir, device));
        Timber.d("Screenshot file created");
    }

    public static int nextX(int priorX, int direction) {
        if (direction == 0 || direction == 1 || direction == 7) {
            return priorX - 1;
        }

        if (direction == 3 || direction == 4 || direction == 5) {
            return priorX + 1;
        }

        return priorX;
    }

    public static int nextY(int priorY, int direction) {
        if (direction == 1 || direction == 2 || direction == 3) {
            return priorY - 1;
        }

        if (direction == 5 || direction == 6 || direction == 7) {
            return priorY + 1;
        }

        return priorY;
    }

    public static int nextDirection(int priorDirection) {
        return (priorDirection + 1) % 8;
    }

    public static int flipDirection(int priorDirection) {
        return (priorDirection + 4) % 8;
    }

    public static Box findBoundingBox(Bitmap bitmap, int startX, int startY, int width, int height) {

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

            if (candidateX >= 0 && candidateX < width &&
                    candidateY >= 0 && candidateY < height &&
                    !isBackground(bitmap.getPixel(candidateX, candidateY))) {
//                Log.v(TAG, "Found next pixel in border!");
                foundX = candidateX;
                foundY = candidateY;

                direction = flipDirection(direction);

                minX = Math.min(minX, foundX);
                minY = Math.min(minY, foundY);
                maxX = Math.max(maxX, foundX);
                maxY = Math.max(maxY, foundY);
            }

            direction = nextDirection(direction);
        } while (foundX != startX || foundY != startY || direction != 0);

        if (hasBlackAtItsCore(bitmap, minX, minY, maxX, maxY)) {
            return Box.NULL;
        }

        return new Box(minX - offset, minY - offset, maxX + offset, maxY + offset, findSignatureColor(bitmap, minX, minY, maxX, maxY), isNearEdge(minX, minY, maxX, maxY, width, height));
    }

    private static boolean isNearEdge(int minX, int minY, int maxX, int maxY, int width, int height) {
        return minX <= 1 || minY <= 1 || maxX > (width - 2) || maxY > (height - 2);
    }

    private static boolean hasBlackAtItsCore(Bitmap bitmap, int minX, int minY, int maxX, int maxY) {
        return bitmap.getPixel((maxX - minX) / 2 + minX, (maxY - minY) / 2 + minY) == Color.BLACK;
    }

    public static int findSignatureColor(Bitmap bitmap, int minX, int minY, int maxX, int maxY) {
        SparseIntArray colors = new SparseIntArray(32);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                int color = bitmap.getPixel(x, y);
                if (!isBackground(color)) {
                    colors.put(color, colors.get(color, 0) + 1);
                }
            }
        }

        return findSignatureColor(colors);
    }

    private static int findSignatureColor(SparseIntArray colors) {
        int maxColor = 0;


        int maxColorCount = 0;
        for (int i = 0; i < colors.size(); i++) {
            int colorCount = colors.valueAt(i);
            if (colorCount > maxColorCount) {
                maxColorCount = colorCount;
                maxColor = colors.keyAt(i);
            }
        }

        double ninetyPercentCountThreshold = maxColorCount * 0.9;

        int count = 0;
        for (int i = 0; i < colors.size(); i++) {
            int colorCount = colors.valueAt(i);
            if (colorCount < ninetyPercentCountThreshold) {

                maxColorCount = colorCount;
                maxColor = colors.keyAt(i);
            }
        }







//        Timber.v("Found max color %s with count %s", Utils.pixelColorInHex(maxColor), maxColorCount);
        return maxColor;
    }

    // https://stackoverflow.com/questions/29398929/how-get-height-of-the-status-bar-and-soft-key-buttons-bar
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;

        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        }

        return 0;
    }

}
