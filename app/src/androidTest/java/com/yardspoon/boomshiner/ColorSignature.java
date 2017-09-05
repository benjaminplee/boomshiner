package com.yardspoon.boomshiner;

import java.util.Arrays;

public class ColorSignature {
    private static final int max = 10;
    private final int[] colors = new int[max];
    private int count = 0;

    public static final ColorSignature NULL = new ColorSignature();

    public void add(int color) {
        if(count < max) {
            colors[count] = color;
            count++;

            Arrays.sort(colors, 0, count);
        }
    }

    public int getAColor() {
        return colors[0];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorSignature that = (ColorSignature) o;

        return Arrays.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(colors);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ColorSig");
        builder.append("[");

        for (int i = 0; i < count; i++) {
            builder.append(Utils.pixelColorInHex(colors[i]));
        }

        builder.append("]");
        return builder.toString();
    }
}
