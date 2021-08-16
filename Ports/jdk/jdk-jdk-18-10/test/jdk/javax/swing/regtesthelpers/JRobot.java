/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * JRobot is a wrapper around java.awt.Robot that provides some convenience
 * methods.
 * <p>When using jtreg you would include this class via something like:
 * <pre>
 * @library ../../../regtesthelpers
 * @build JRobot
 * </pre>
 *
 */
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;

public class JRobot extends java.awt.Robot {
    private static int DEFAULT_DELAY = 550;
    private static int INTERNAL_DELAY = 250;

    private int delay;
    private boolean delaysEnabled;

    protected JRobot(boolean enableDelays) throws AWTException {
        super();
        delaysEnabled = enableDelays;
        setAutoWaitForIdle(enableDelays);
        if (enableDelays) {
            setAutoDelay(INTERNAL_DELAY);
            setDelay(DEFAULT_DELAY);
        }
    }

    /**
     * Return a JRobot. Delays are enabled by default.
     * @return a JRobot
     */
    public static JRobot getRobot() {
        return getRobot(true);
    }

    /**
     * Create a JRobot. The parameter controls whether delays are enabled.
     * @param enableDelays controls whether delays are enabled.
     * @return a JRobot
     */
    public static JRobot getRobot(boolean enableDelays) {
        JRobot robot = null;
        try {
            robot = new JRobot(enableDelays);
        } catch (AWTException e) {
            System.err.println("Coudn't create Robot, details below");
            throw new Error(e);
        }
        return robot;
    }

    /**
     * Press and release a key.
     * @param keycode which key to press. For example, KeyEvent.VK_DOWN
     */
    public void hitKey(int keycode) {
        keyPress(keycode);
        keyRelease(keycode);
        delay();
    }

    /**
     * Press and release a key with modifiers.
     * @param keys keys to press. Keys are pressed in order they are passed as
     * parameters to this method. All keys except the last one are considered
     * modifiers. For example, to press Ctrl+Shift+T, call:
     * hitKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_T);
     */
    public void hitKey(int... keys) {
        for (int i = 0; i < keys.length; i++) {
            keyPress(keys[i]);
        }

        for (int i = keys.length - 1; i >= 0; i--) {
            keyRelease(keys[i]);
        }
        delay();
    }

    /**
     * Move mouse cursor to the center of the Component.
     * @param c Component the mouse is placed over
     */
    public void moveMouseTo(Component c) {
        Point p = c.getLocationOnScreen();
        Dimension size = c.getSize();
        p.x += size.width / 2;
        p.y += size.height / 2;
        mouseMove(p.x, p.y);
        delay();
    }

    /**
     * Move mouse smoothly from (x0, y0) to (x1, y1).
     */
    public void glide(int x0, int y0, int x1, int y1) {
        float dmax = (float)Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        float dx = (x1 - x0) / dmax;
        float dy = (y1 - y0) / dmax;

        mouseMove(x0, y0);
        for (int i=1; i<=dmax; i++) {
            mouseMove((int)(x0 + dx*i), (int)(y0 + dy*i));
        }
        delay();
    }

    /**
     * Perform a mouse click, i.e. press and release mouse button(s).
     * @param buttons mouse button(s).
     *                For example, MouseEvent.BUTTON1_MASK
     */
    public void clickMouse(int buttons) {
        mousePress(buttons);
        mouseRelease(buttons);
        delay();
    }

    /**
     * Perform a click with the first mouse button.
     */
    public void clickMouse() {
        clickMouse(InputEvent.BUTTON1_MASK);
    }

    /**
     * Click in the center of the given Component
     * @param c the Component to click on
     * @param buttons mouse button(s).
     */
    public void clickMouseOn(Component c, int buttons) {
        moveMouseTo(c);
        clickMouse(buttons);
    }

    /**
     * Click the first mouse button in the center of the given Component
     * @param c the Component to click on
     */
    public void clickMouseOn(Component c) {
        clickMouseOn(c, InputEvent.BUTTON1_MASK);
    }

    /**
     * Return whether delays are enabled
     * @return whether delays are enabled
     */
    public boolean getDelaysEnabled() {
        return delaysEnabled;
    }

    /**
     * Delay execution by delay milliseconds
     */
    public void delay() {
        delay(delay);
    }

    /**
     * Return the delay amount, in milliseconds
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Set the delay amount, in milliseconds
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Waits until all events currently on the event queue have been processed.
     * Does nothing if called on EDT
     */
    public synchronized void waitForIdle() {
        if (!EventQueue.isDispatchThread()) {
            super.waitForIdle();
        }
    }

    /**
     * Calculate the center of the Rectangle passed, and return them
     * in a Point object.
     * @param r a non-null Rectangle
     * @return a new Point object containing coordinates of r's center
     */
    public Point centerOf(Rectangle r) {
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /**
     * Calculate the center of the Rectangle passed, and store it in p.
     * @param r a non-null Rectangle
     * @param p a non-null Point that receives coordinates of r's center
     * @return p
     */
    public Point centerOf(Rectangle r, Point p) {
        p.x = r.x + r.width / 2;
        p.y = r.y + r.height / 2;
        return p;
    }

    /**
     * Convert a rectangle from coordinate system of Component c to
     * screen coordinate system.
     * @param r a non-null Rectangle
     * @param c a Component whose coordinate system is used for conversion
     */
    public void convertRectToScreen(Rectangle r, Component c) {
        Point p = new Point(r.x, r.y);
        SwingUtilities.convertPointToScreen(p, c);
        r.x = p.x;
        r.y = p.y;
    }

    /**
     * Compares two rectangles pixel-by-pixel.
     * @param r0 the first area
     * @param r1 the second area
     * return true if all pixels in the two areas are identical
     */
    public boolean compareRects(Rectangle r0, Rectangle r1) {
        int xShift = r1.x - r0.x;
        int yShift = r1.y - r0.y;

        for (int y = r0.y; y < r0.y + r0.height; y++) {
            for (int x = r0.x; x < r0.x + r0.width; x++) {
                if (!comparePixels(x, y, x + xShift, y + yShift)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares colors of two points on the screen.
     * @param p0 the first point
     * @param p1 the second point
     * return true if the two points have the same color
     */
    public boolean comparePixels(Point p0, Point p1) {
        return comparePixels(p0.x, p0.y, p1.x, p1.y);
    }

    /**
     * Compares colors of two points on the screen.
     * @param x0 the x coordinate of the first point
     * @param y0 the y coordinate of the first point
     * @param x1 the x coordinate of the second point
     * @param y1 the y coordinate of the second point
     * return true if the two points have the same color
     */
    public boolean comparePixels(int x0, int y0, int x1, int y1) {
        return (getPixelColor(x0, y0).equals(getPixelColor(x1, y1)));
    }
}
