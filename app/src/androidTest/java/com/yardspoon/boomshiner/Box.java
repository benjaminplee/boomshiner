package com.yardspoon.boomshiner;

public class Box {

    public static final Box NULL = new Box(0, 0, 0, 0, 0, true);
    private final int likelyTargetSize = 46;
    private final int likelyTargetMin = 20; // less than half; in case we are on edge
    private final int likelyTargetMax = 250; // 5 wide; may need to bump up for higher levels

    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;
    public final int maxColor;
    public final int cx;
    public final int cy;
    public final boolean nearEdge;

    public Box(int x1, int y1, int x2, int y2, int maxColor, boolean nearEdge) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.cx = (x2 - x1) / 2 + x1;
        this.cy = (y2 - y1) / 2 + y1;
        this.maxColor = maxColor;
        this.nearEdge = nearEdge;
    }

    public boolean contains(int x, int y) {
        return x > x1 && x < x2 && y > y1 && y < y2;
    }

    @Override
    public String toString() {
        return "Box[" + toPointString(x1, y1) + "," + toPointString(x2, y2) + "," + isLikelyTarget() + "," + maxColor + "]";
    }

    private String toPointString(int x, int y) {
        return "(" + x + "," + y + ")";
    }

    public boolean isLikelyTarget() {
        // Reduce minimal size for likely target only if next to effective edge to reduce extra hits on points words
        int effectiveHorizontalMin = nearEdge ? likelyTargetMin : likelyTargetSize;
        int effectiveVerticalMin = nearEdge ? likelyTargetMin : likelyTargetSize;

        int width = x2 - x1;
        int height = y2 - y1;

        return width > effectiveHorizontalMin && width < likelyTargetMax && height > effectiveVerticalMin && height < likelyTargetMax;
    }

    public double distanceTo(Box other) {
        return Math.sqrt(Math.pow(Math.abs(cx - other.cx), 2) + Math.pow(Math.abs(cy - other.cy), 2));
    }

    public Vector vectorTo(Box other) {
        return new Vector(other.cx - cx, other.cy - cy);
    }
}
