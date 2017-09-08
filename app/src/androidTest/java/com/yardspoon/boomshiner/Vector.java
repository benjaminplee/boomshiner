package com.yardspoon.boomshiner;

class Vector {
    public static final Vector NULL = new Vector(0, 0);
    public final int deltaX;
    public final int deltaY;

    Vector(int deltaX, int deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public Vector multiply(double multiplier) {
        return new Vector(Double.valueOf(deltaX * multiplier).intValue(), Double.valueOf(deltaY * multiplier).intValue());
    }

    public double distance() {
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    @Override
    public String toString() {
        return "<" + deltaX + "," + deltaY + ">";
    }
}
