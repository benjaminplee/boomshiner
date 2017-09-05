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
    public static String pixelColorInHex(Integer pixel) {
        String red = Integer.toHexString(Color.red(pixel)).toUpperCase();
        String green = Integer.toHexString(Color.green(pixel)).toUpperCase();
        String blue = Integer.toHexString(Color.blue(pixel)).toUpperCase();

        return "#" + red + "|" + green + "|" + blue;
    }

}
