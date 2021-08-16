/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.terminal;

/**
 * Class holding the cursor position.
 *
 * @see Terminal#getCursorPosition(java.util.function.IntConsumer)
 */
public class Cursor {

    private final int x;
    private final int y;

    public Cursor(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Cursor) {
            Cursor c = (Cursor) o;
            return x == c.x && y == c.y;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public String toString() {
        return "Cursor[" + "x=" + x + ", y=" + y + ']';
    }
}
