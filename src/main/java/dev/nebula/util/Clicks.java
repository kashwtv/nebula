package dev.nebula.util;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Clicks {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private Clicks() {}

    public static void click(int button) {
        if (button == 0) LEFT.add(System.currentTimeMillis());
        else if (button == 1) RIGHT.add(System.currentTimeMillis());
    }

    private static int count(Deque<Long> d) {
        long cut = System.currentTimeMillis() - 1000;
        while (!d.isEmpty() && d.peekFirst() < cut) d.pollFirst();
        return d.size();
    }

    public static int left() {
        return count(LEFT);
    }

    public static int right() {
        return count(RIGHT);
    }
}
