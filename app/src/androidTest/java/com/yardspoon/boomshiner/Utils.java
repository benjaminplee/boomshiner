package com.yardspoon.boomshiner;

import android.graphics.Color;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import timber.log.Timber;

public class Utils {
    public static void time(String msg, Runnable action) {
        long before = System.nanoTime();
        action.run();
        long after = System.nanoTime();
        long elapsed = (after - before) / 1000 / 1000; // convert to ms
        Timber.d("Time: [%s] took %s ms", msg, elapsed);
    }

    public static void pause(long targetMs) {
        time("Pause for " + targetMs + "ms", () -> SystemClock.sleep(targetMs));
    }

    @NonNull
    public static String pixelColorInHex(int pixel) {
        String red = Integer.toHexString(Color.red(pixel)).toUpperCase();
        String green = Integer.toHexString(Color.green(pixel)).toUpperCase();
        String blue = Integer.toHexString(Color.blue(pixel)).toUpperCase();

        return "#" + red + "|" + green + "|" + blue;
    }

    // https://en.wikipedia.org/wiki/Color_difference
    // "one of the better low-cost approximations"
    public static double colorDistance(int c1, int c2) {
        int c1_red = Color.red(c1);
        int c1_blue = Color.blue(c1);
        int c1_green = Color.green(c1);
        int c2_red = Color.red(c2);
        int c2_blue = Color.blue(c2);
        int c2_green = Color.green(c2);

        double mean_red = (c1_red + c2_red) / 2;
        double delta_red = c1_red - c2_red;
        double delta_blue = c1_blue - c2_blue;
        double delta_green = c1_green - c2_green;

        double red_hunk = (2 + mean_red / 256) * Math.pow(delta_red, 2);
        double green_hunk = 4 * Math.pow(delta_green, 2);
        double blue_hunk = (2 + (255 - mean_red) / 256) * Math.pow(delta_blue, 2);

        return Math.sqrt(red_hunk + green_hunk + blue_hunk);
    }
}
