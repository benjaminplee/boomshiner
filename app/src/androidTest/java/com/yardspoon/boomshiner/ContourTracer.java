package com.yardspoon.boomshiner;

import android.graphics.Bitmap;
import android.util.SparseIntArray;

import java.util.function.Predicate;

public class ContourTracer {
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

    public static Box findBoundingBox(Bitmap bitmap, int startX, int startY, Predicate<Integer> isBackground) {

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

            if (candidateX >= 0 && candidateX < bitmap.getWidth() &&
                    candidateY >= 0 && candidateY < bitmap.getHeight() &&
                    !isBackground.test(bitmap.getPixel(candidateX, candidateY))) {
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

        int maxColor = findMaxColor(bitmap, minX, minY, maxX, maxY, isBackground);

        return new Box(minX - offset, minY - offset, maxX + offset, maxY + offset, maxColor);
    }

    public static int findMaxColor(Bitmap bitmap, int minX, int minY, int maxX, int maxY, Predicate<Integer> isBackground) {
        SparseIntArray colors = new SparseIntArray(32);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                int color = bitmap.getPixel(x, y);
                if (!isBackground.test(color)) {
                    colors.put(color, colors.get(color, 0) + 1);
                }
            }
        }
        int maxColorCount = 0;
        int maxColor = 0;
        for (int i = 0; i < colors.size(); i++) {
            int colorCount = colors.valueAt(i);
            if (colorCount > maxColorCount) {
                maxColorCount = colorCount;
                maxColor = colors.keyAt(i);
            }
        }
        return maxColor;
    }
}
