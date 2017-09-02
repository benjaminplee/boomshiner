package com.yardspoon.boomshiner;

import java.util.ArrayList;
import java.util.List;

public class Finding {
    public final List<Box> boxes;
    public final List<Box> likelyTargets;
    public final List<Box> unLikelyTargets;
    public final Screenshot screenshot;

    public Finding(Screenshot screenshot) {
        this.screenshot = screenshot;
        boxes = new ArrayList<>(64);
        likelyTargets = new ArrayList<>(64);
        unLikelyTargets = new ArrayList<>(64);
    }

    public void review() {
        for (Box box : boxes) {
            if (box.isLikelyTarget()) {
                likelyTargets.add(box);
            } else {
                unLikelyTargets.add(box);
            }
        }
    }
}
