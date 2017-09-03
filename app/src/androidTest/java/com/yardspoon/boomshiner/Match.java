package com.yardspoon.boomshiner;

class Match {
    final Box start;
    final Box end;
    final Vector vector;

    Match(Box start, Box end) {
        this.start = start;
        this.end = end;
        this.vector = start.vectorTo(end);
    }

    @Override
    public String toString() {
        return "[" + start + " - " + end + " - " + vector + "]";
    }
}
