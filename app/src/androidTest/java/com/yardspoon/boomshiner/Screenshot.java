package com.yardspoon.boomshiner;

import android.support.annotation.NonNull;
import android.support.test.uiautomator.UiDevice;

import java.io.File;

public class Screenshot {
    public final long nanoTime;
    public final File file;
    public final File reviewedFile;
    public final File analyzedFile;

    Screenshot(File base, UiDevice device) {
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
