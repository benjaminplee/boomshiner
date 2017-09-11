package com.yardspoon.boomshiner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.Until;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.yardspoon.boomshiner.Utils.pixelColorInHex;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String BOOMSHINE_PACKAGE = "com.bantambytes.android.game.boomshine";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int MEDIUM_PAUSE_TIMEOUT_MS = 4000;
    private static final int LONG_PAUSE_TIMEOUT_MS = 8000;
    private static final int SHORT_PAUSE_TIMEOUT_MS = 10;

    private static final int BACKGROUND_GREEN_PIXEL_COLOR = -16764623;
    private static final int WHITE_TEXT_PIXEL_COLOR = -9204084;
    private static final int COLOR_DIFF_FOR_SIMILAR = 50;

    private static final int MIN_COLOR_COUNT_THRESHOLD = 25;
    private static final int BOX_MINMAX_OFFSET = 1;

    private static final SparseArray<SparseIntArray> colorDistances = new SparseArray<>();
    private int displayHeight;
    private int displayWidth;

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
        effectiveDisplayHeight = displayHeight - getSoftButtonsBarHeight();

        Timber.d("Dimensions: %s x %s", displayWidth, displayHeight);
    }

    @Test
    public void test_colors() {
        getIntoTheGame();

        takeScreenShot();
        Bitmap bitmap = Images.getBitmap(screenshots.get(0).file);

        SparseIntArray foundColors = new SparseIntArray();

        int rowSkip = 5;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        for (int y = 0; y < height; y += rowSkip) {
            for (int x = 0; x < width; x += 1) {
                int color = bitmap.getPixel(x, y);

                if (!isBackground(color)) {
                    foundColors.put(color, foundColors.get(color, 0) + 1);
                }
            }
        }

        for (int i = 0; i < foundColors.size(); i++) {
            int pixel = foundColors.keyAt(i);

            if (foundColors.get(pixel) > 50) {
                Timber.d("Found significant color: " + pixelColorInHex(pixel) + " " + foundColors.get(pixel) + " times -- [" + pixel + "]");
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

        if (startFromScratch) {
            getIntoTheGame();
        }

        // 4 for analysis
        Utils.time("Take screenshot", this::takeScreenShot);
//        Utils.time("Take screenshot", this::takeScreenShot);
//        Utils.time("Take screenshot", this::takeScreenShot);
//        Utils.time("Take screenshot", this::takeScreenShot);

        // 5th to check predictions
        Utils.pause(MEDIUM_PAUSE_TIMEOUT_MS);
        Utils.time("Take screenshot", this::takeScreenShot);

        List<Finding> findings = new ArrayList<>();
        for (Screenshot screenshot : screenshots) {
            Utils.time("Review screenshot", () -> findings.add(reviewScreenshot(screenshot)));
        }

//        Utils.time("Analyze findings took", () -> analyze(findings));

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

        Finding lastFinding = findings.get(findings.size() - 1);
        Bitmap last = Images.getBitmap(lastFinding.screenshot.file);

        for (int i = 0; i < findings.size() - 1; i++) {
            Finding firstFinding = findings.get(i);
            Finding nextFinding = findings.get(i + 1);
            List<Box> findingBoxs = firstFinding.likelyTargets;
            List<Box> nextFindingBoxs = nextFinding.likelyTargets;

            Images.draw(last, findingBoxs);

            for (Box first : findingBoxs) {
                for (Box second : nextFindingBoxs) {
                    if (first.colorSignature.equals(second.colorSignature)) {
                        Match match = new Match(first, second);

                        long knownDelta = nextFinding.screenshot.nanoTime - firstFinding.screenshot.nanoTime;
                        long projectionDelta = lastFinding.screenshot.nanoTime - firstFinding.screenshot.nanoTime;
                        double deltaRatio = (double) projectionDelta / (double) knownDelta;

                        Images.draw(last, match.start, match.vector, Color.GRAY);
                        Images.draw(last, match.end, match.vector.multiply(deltaRatio), Color.YELLOW);
                    }
                }
            }
        }

        Timber.d("writing out last image");
        Images.write(last, lastFinding.screenshot.reviewedFile);

//        Timber.d("recycling mutable bitmap");
        last.recycle();
    }

    private Finding reviewScreenshot(Screenshot screenshot) {
        Timber.i("reviewing screen shots");

        Finding finding = new Finding(screenshot);
        Bitmap bitmap = Images.getBitmap(screenshot.file);

        analyzePositions(bitmap, finding);

        Timber.d("writing out png");
        Images.write(bitmap, screenshot.analyzedFile);

//        Timber.d("recycling mutable bitmap");
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
//                    Timber.v("Skipping row %s since it starts with black and is probably the menu buttons.", y);
                    break;
                }

                for (int x = 0; x < displayWidth; x += 1) {
                    int color = bitmap.getPixel(x, y);

//                    Timber.v("PIXEL (%s,%s:%s)", x, y, color);

                    if (!isBackground(color)) {
                        boolean alreadyContained = false;
                        for (Box box : finding.boxes) {
                            if (box.contains(x, y) && areSimilarColors(box.colorSignature.getAColor(), color)) {
                                alreadyContained = true;
                                break;
                            }
                        }

                        if (!alreadyContained) {
                            Box box = findBoundingBoxes(bitmap, x, y, displayWidth, effectiveDisplayHeight);
                            Timber.i("Found box: %s", box);
                            finding.boxes.add(0, box); // Trying to speed up box comparisons
                        }
                    }
                }
            }
        });

        Utils.time("Reviewing finding", finding::review);
        Utils.time("drawing likely target boxes on bitmap", () -> Images.draw(bitmap, finding.likelyTargets));
        Utils.time("drawing other boxes target boxes on bitmap", () -> Images.draw(bitmap, finding.unLikelyTargets, Color.GRAY));
    }

    public static boolean isBackground(int color) {
        return color == BACKGROUND_GREEN_PIXEL_COLOR || color == WHITE_TEXT_PIXEL_COLOR || color == Color.BLACK;
    }

    private void pressPlay() {
        Timber.d("Pressing play");
        device.swipe(displayWidth / 2, 2 * displayHeight / 3, displayWidth / 2 + 5, 2 * displayHeight / 3 + 5, 2);
    }

    private void takeScreenShot() {
        Utils.pause(SHORT_PAUSE_TIMEOUT_MS);
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

    public static Box findBoundingBoxes(Bitmap bitmap, int startX, int startY, int width, int height) {
        // DIRECTIONS
        // 1 2 3
        // 0 * 4
        // 7 6 5

//        Log.v(TAG, "Looking for bounding from " + startX + "," + startY);

        int startColor = bitmap.getPixel(startX, startY);

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

            if (candidateX >= 0 && candidateX < width && candidateY >= 0 && candidateY < height) {
                int candidateColor = bitmap.getPixel(candidateX, candidateY);

                if (!isBackground(candidateColor) && areSimilarColors(startColor, candidateColor)) {
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
            }
        } while (foundX != startX || foundY != startY || direction != 0);

        ColorSignature signatureColor = hasBlackAtItsCore(bitmap, minX, minY, maxX, maxY) ? ColorSignature.NULL : findSignatureColor(parseColors(bitmap, minX, minY, maxX, maxY));

        return new Box(minX - BOX_MINMAX_OFFSET, minY - BOX_MINMAX_OFFSET, maxX + BOX_MINMAX_OFFSET, maxY + BOX_MINMAX_OFFSET, signatureColor, isNearEdge(minX, minY, maxX, maxY, width, height));
    }

    private static boolean areSimilarColors(int colorA, int colorB) {
        return findColorDistance(colorA, colorB) < COLOR_DIFF_FOR_SIMILAR;
    }

    private static boolean isNearEdge(int minX, int minY, int maxX, int maxY, int width, int height) {
        return minX <= 1 || minY <= 1 || maxX > (width - 2) || maxY > (height - 2);
    }

    private static boolean hasBlackAtItsCore(Bitmap bitmap, int minX, int minY, int maxX, int maxY) {
        return bitmap.getPixel((maxX - minX) / 2 + minX, (maxY - minY) / 2 + minY) == Color.BLACK;
    }

    public static int findColorDistance(int a, int b) {
        if (a == b) {
            return 0;
        }

        SparseIntArray a_distances = colorDistances.get(a, new SparseIntArray());
        int lookupDistance = a_distances.get(b);
        if (lookupDistance != 0) {
            return lookupDistance;
        }

        int calculatedDistance = (int) Utils.colorDistance(a, b);

        a_distances.put(b, calculatedDistance);
        SparseIntArray b_distances = colorDistances.get(b, new SparseIntArray());
        b_distances.put(a, calculatedDistance);

        return calculatedDistance;
    }

    public static ColorSignature findSignatureColor(SparseIntArray colorSpread) {
        int size = colorSpread.size();

//        if (size < 5) {
//            for (int i = 0; i < size; i++) {
//                int color = colorSpread.keyAt(i);
//
//                Timber.v("  Color %s compared to neighbors: ", Utils.pixelColorInHex(color));
//
//                for (int j = 0; j < size; j++) {
//                    int other = colorSpread.keyAt(j);
//
//                    if (other != color) {
//                        Timber.v("    > %s : %s", Utils.pixelColorInHex(other), Utils.colorDistance(color, other));
//                    }
//                }
//            }
//        }

        ColorSignature colorSignature = new ColorSignature();

        int maxColorCount = 0;
        for (int i = 0; i < size; i++) {
            int colorCount = colorSpread.valueAt(i);
            if (colorCount > maxColorCount) {
                maxColorCount = colorCount;
            }
        }

        double ninetyPercentCountThreshold = maxColorCount * 0.9;

        for (int i = 0; i < size; i++) {
            int colorCount = colorSpread.valueAt(i);
            if (colorCount > ninetyPercentCountThreshold) {
                colorSignature.add(colorSpread.keyAt(i));
            }
        }

//        Timber.v("Identified color signature: %s", colorSignature);
        return colorSignature;
    }

    @NonNull
    private static SparseIntArray parseColors(Bitmap bitmap, int minX, int minY, int maxX, int maxY) {
        SparseIntArray colors = new SparseIntArray(32);

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                int color = bitmap.getPixel(x, y);
                if (!isBackground(color)) {
                    colors.put(color, colors.get(color, 0) + 1);
                }
            }
        }

        int size = colors.size();
        int[] toRemove = new int[size];
        for (int i = 0; i < size; i++) {
            if (colors.valueAt(i) < MIN_COLOR_COUNT_THRESHOLD) {
                toRemove[i] = colors.keyAt(i);
            } else {
                toRemove[i] = -1;
            }
        }
        for (int aToRemove : toRemove) {
            colors.delete(aToRemove);
        }

        return colors;
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
