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
import android.util.SparseIntArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import timber.log.Timber;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static com.yardspoon.boomshiner.Utils.pixelColorInHex;
import static org.hamcrest.core.IsNull.notNullValue;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String BOOMSHINE_PACKAGE = "com.bantambytes.android.game.boomshine";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int MEDIUM_PAUSE_TIMEOUT_MS = 2000;
    private static final int LONG_PAUSE_TIMEOUT_MS = 8000;
    private static final int SHORT_PAUSE_TIMEOUT_MS = 50;

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
        Timber.d("Pics Dir: %s", picsDir.getPath());
        screenshots = new ArrayList<>();

        displayHeight = device.getDisplayHeight();
        displayWidth = device.getDisplayWidth();
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

                if (!isBoomshineBackground(color)) {
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

        getIntoTheGame();

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
            for (Box secondSample : secondSamples) {
                matches.add(new Match(firstSample, secondSample));
            }

//            List<Box> matchingColors = new ArrayList<>();

//            for (Box secondSample : secondSamplesCopy) {
//                if (secondSample.maxColor == firstSample.maxColor) {
//                    matchingColors.add(secondSample);
//                }
//            }
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

        Images.draw(third, firstSamples, Color.GRAY);
        Images.draw(third, secondSamples, Color.YELLOW);

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

//        analyzeColors(bitmap);
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
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            for (int y = 0; y < height; y += rowSkip) {
                for (int x = 0; x < width; x += 1) {
                    int color = bitmap.getPixel(x, y);

                    if (!ExampleInstrumentedTest.isBoomshineBackground(color)) {

                        boolean alreadyContained = false;
                        for (Box box : finding.boxes) {
                            if (box.contains(x, y)) {
                                alreadyContained = true;
                                break;
                            }
                        }

                        if (!alreadyContained) {
                            Box boundingBox = ContourTracer.findBoundingBox(bitmap, x, y, ExampleInstrumentedTest::isBoomshineBackground);
                            Timber.i("Found box: %s", boundingBox);
                            finding.boxes.add(boundingBox);
                        }
                    }
                }
            }
        });

        Utils.time("Reviewing finding", finding::review);
        Utils.time("drawing likely target boxes on bitmap", () -> Images.draw(bitmap, finding.likelyTargets, Color.WHITE));
        Utils.time("drawing other boxes target boxes on bitmap", () -> Images.draw(bitmap, finding.unLikelyTargets, Color.RED));
    }

    public static boolean isBoomshineBackground(int color) {
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

}
