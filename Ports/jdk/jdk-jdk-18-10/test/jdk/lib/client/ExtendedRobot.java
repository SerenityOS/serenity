/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.GraphicsDevice;
import java.awt.Toolkit;
import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * ExtendedRobot is a subclass of {@link java.awt.Robot}. It provides some convenience methods that are
 * ought to be moved to {@link java.awt.Robot} class.
 * <p>
 * ExtendedRobot uses delay {@link #getSyncDelay()} to make syncing threads with {@link #waitForIdle()}
 * more stable. This delay can be set once on creating object and could not be changed throughout object
 * lifecycle. Constructor reads vm integer property {@code java.awt.robotdelay} and sets the delay value
 * equal to the property value. If the property was not set 500 milliseconds default value is used.
 * <p>
 * When using jtreg you would include this class via something like:
 * <pre>
 * {@literal @}library ../../../../lib/testlibrary
 * {@literal @}build ExtendedRobot
 * </pre>
 *
 * @author      Dmitriy Ermashov
 * @since       9
 */

public class ExtendedRobot extends Robot {

    private static int DEFAULT_SPEED = 20;       // Speed for mouse glide and click
    private static int DEFAULT_SYNC_DELAY = 500; // Default Additional delay for waitForIdle()
    private static int DEFAULT_STEP_LENGTH = 2;  // Step length (in pixels) for mouse glide

    private final int syncDelay = DEFAULT_SYNC_DELAY;

    //TODO: uncomment three lines below after moving functionality to java.awt.Robot
    //{
    //    syncDelay = AccessController.doPrivileged(new GetIntegerAction("java.awt.robotdelay", DEFAULT_SYNC_DELAY));
    //}

    /**
     * Constructs an ExtendedRobot object in the coordinate system of the primary screen.
     *
     * @throws  AWTException if the platform configuration does not allow low-level input
     *          control. This exception is always thrown when
     *          GraphicsEnvironment.isHeadless() returns true
     * @throws  SecurityException if {@code createRobot} permission is not granted
     *
     * @see     java.awt.GraphicsEnvironment#isHeadless
     * @see     SecurityManager#checkPermission
     * @see     java.awt.AWTPermission
     */
    public ExtendedRobot() throws AWTException {
        super();
    }

    /**
     * Creates an ExtendedRobot for the given screen device. Coordinates passed
     * to ExtendedRobot method calls like mouseMove and createScreenCapture will
     * be interpreted as being in the same coordinate system as the specified screen.
     * Note that depending on the platform configuration, multiple screens may either:
     * <ul>
     * <li>share the same coordinate system to form a combined virtual screen</li>
     * <li>use different coordinate systems to act as independent screens</li>
     * </ul>
     * This constructor is meant for the latter case.
     * <p>
     * If screen devices are reconfigured such that the coordinate system is
     * affected, the behavior of existing ExtendedRobot objects is undefined.
     *
     * @param   screen  A screen GraphicsDevice indicating the coordinate
     *                  system the Robot will operate in.
     * @throws  AWTException if the platform configuration does not allow low-level input
     *          control. This exception is always thrown when
     *          GraphicsEnvironment.isHeadless() returns true.
     * @throws  IllegalArgumentException if {@code screen} is not a screen
     *          GraphicsDevice.
     * @throws  SecurityException if {@code createRobot} permission is not granted
     *
     * @see     java.awt.GraphicsEnvironment#isHeadless
     * @see     GraphicsDevice
     * @see     SecurityManager#checkPermission
     * @see     java.awt.AWTPermission
     */
    public ExtendedRobot(GraphicsDevice screen) throws AWTException {
        super(screen);
    }

    /**
     * Returns delay length for {@link #waitForIdle()} method
     *
     * @return  Current delay value
     *
     * @see     #waitForIdle()
     */
    public int getSyncDelay(){ return this.syncDelay; }

    /**
     * Clicks mouse button(s) by calling {@link java.awt.Robot#mousePress(int)} and
     * {@link java.awt.Robot#mouseRelease(int)} methods
     *
     *
     * @param   buttons The button mask; a combination of one or more mouse button masks.
     * @throws  IllegalArgumentException if the {@code buttons} mask contains the mask for
     *          extra mouse button and support for extended mouse buttons is
     *          {@link Toolkit#areExtraMouseButtonsEnabled() disabled} by Java
     * @throws  IllegalArgumentException if the {@code buttons} mask contains the mask for
     *          extra mouse button that does not exist on the mouse and support for extended
     *          mouse buttons is {@link Toolkit#areExtraMouseButtonsEnabled() enabled}
     *          by Java
     *
     * @see     #mousePress(int)
     * @see     #mouseRelease(int)
     * @see     InputEvent#getMaskForButton(int)
     * @see     Toolkit#areExtraMouseButtonsEnabled()
     * @see     java.awt.event.MouseEvent
     */
    public void click(int buttons) {
        mousePress(buttons);
        waitForIdle(DEFAULT_SPEED);
        mouseRelease(buttons);
        waitForIdle();
    }

    /**
     * Clicks mouse button 1
     *
     * @throws  IllegalArgumentException if the {@code buttons} mask contains the mask for
     *          extra mouse button and support for extended mouse buttons is
     *          {@link Toolkit#areExtraMouseButtonsEnabled() disabled} by Java
     * @throws  IllegalArgumentException if the {@code buttons} mask contains the mask for
     *          extra mouse button that does not exist on the mouse and support for extended
     *          mouse buttons is {@link Toolkit#areExtraMouseButtonsEnabled() enabled}
     *          by Java
     *
     * @see     #click(int)
     */
    public void click() {
        click(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Waits until all events currently on the event queue have been processed with given
     * delay after syncing threads. It uses more advanced method of synchronizing threads
     * unlike {@link java.awt.Robot#waitForIdle()}
     *
     * @param   delayValue  Additional delay length in milliseconds to wait until thread
     *                      sync been completed
     * @throws  sun.awt.SunToolkit.IllegalThreadException if called on the AWT event
     *          dispatching thread
     */
    public synchronized void waitForIdle(int delayValue) {
        super.waitForIdle();
        delay(delayValue);
    }

    /**
     * Waits until all events currently on the event queue have been processed with delay
     * {@link #getSyncDelay()} after syncing threads. It uses more advanced method of
     * synchronizing threads unlike {@link java.awt.Robot#waitForIdle()}
     *
     * @throws  sun.awt.SunToolkit.IllegalThreadException if called on the AWT event
     *          dispatching thread
     *
     * @see     #waitForIdle(int)
     */
    @Override
    public synchronized void waitForIdle() {
        waitForIdle(syncDelay);
    }

    /**
     * Move the mouse in multiple steps from where it is
     * now to the destination coordinates.
     *
     * @param   x   Destination point x coordinate
     * @param   y   Destination point y coordinate
     *
     * @see     #glide(int, int, int, int)
     */
    public void glide(int x, int y) {
        Point p = MouseInfo.getPointerInfo().getLocation();
        glide(p.x, p.y, x, y);
    }

    /**
     * Move the mouse in multiple steps from where it is
     * now to the destination point.
     *
     * @param   dest    Destination point
     *
     * @see     #glide(int, int)
     */
    public void glide(Point dest) {
        glide(dest.x, dest.y);
    }

    /**
     * Move the mouse in multiple steps from source coordinates
     * to the destination coordinates.
     *
     * @param   fromX   Source point x coordinate
     * @param   fromY   Source point y coordinate
     * @param   toX     Destination point x coordinate
     * @param   toY     Destination point y coordinate
     *
     * @see     #glide(int, int, int, int, int, int)
     */
    public void glide(int fromX, int fromY, int toX, int toY) {
        glide(fromX, fromY, toX, toY, DEFAULT_STEP_LENGTH, DEFAULT_SPEED);
    }

    /**
     * Move the mouse in multiple steps from source point to the
     * destination point with default speed and step length.
     *
     * @param   src     Source point
     * @param   dest    Destination point
     *
     * @see     #glide(int, int, int, int, int, int)
     */
    public void glide(Point src, Point dest) {
        glide(src.x, src.y, dest.x, dest.y, DEFAULT_STEP_LENGTH, DEFAULT_SPEED);
    }

    /**
     * Move the mouse in multiple steps from source point to the
     * destination point with given speed and step length.
     *
     * @param   srcX        Source point x cordinate
     * @param   srcY        Source point y cordinate
     * @param   destX       Destination point x cordinate
     * @param   destY       Destination point y cordinate
     * @param   stepLength  Approximate length of one step
     * @param   speed       Delay between steps.
     *
     * @see     #mouseMove(int, int)
     * @see     #delay(int)
     */
     public void glide(int srcX, int srcY, int destX, int destY, int stepLength, int speed) {
        int stepNum;
        double tDx, tDy;
        double dx, dy, ds;
        double x, y;

        dx = (destX - srcX);
        dy = (destY - srcY);
        ds = Math.sqrt(dx*dx + dy*dy);

        tDx = dx / ds * stepLength;
        tDy = dy / ds * stepLength;

        int stepsCount = (int) ds / stepLength;

        // Walk the mouse to the destination one step at a time
        mouseMove(srcX, srcY);

        for (x = srcX, y = srcY, stepNum = 0;
             stepNum < stepsCount;
             stepNum++) {
            x += tDx;
            y += tDy;
            mouseMove((int)x, (int)y);
            delay(speed);
        }

        // Ensure the mouse moves to the right destination.
        // The steps may have led the mouse to a slightly wrong place.
        mouseMove(destX, destY);
    }

    /**
     * Moves mouse pointer to given screen coordinates.
     *
     * @param   position    Target position
     *
     * @see     java.awt.Robot#mouseMove(int, int)
     */
    public synchronized void mouseMove(Point position) {
        mouseMove(position.x, position.y);
    }


    /**
     * Emulate native drag and drop process using {@code InputEvent.BUTTON1_DOWN_MASK}.
     * The method successively moves mouse cursor to point with coordinates
     * ({@code fromX}, {@code fromY}), presses mouse button 1, drag mouse to
     * point with coordinates ({@code toX}, {@code toY}) and releases mouse
     * button 1 at last.
     *
     * @param   fromX   Source point x coordinate
     * @param   fromY   Source point y coordinate
     * @param   toX     Destination point x coordinate
     * @param   toY     Destination point y coordinate
     *
     * @see     #mousePress(int)
     * @see     #glide(int, int, int, int)
     */
    public void dragAndDrop(int fromX, int fromY, int toX, int toY){
        mouseMove(fromX, fromY);
        mousePress(InputEvent.BUTTON1_DOWN_MASK);
        waitForIdle();
        glide(toX, toY);
        mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        waitForIdle();
    }

    /**
     * Emulate native drag and drop process using {@code InputEvent.BUTTON1_DOWN_MASK}.
     * The method successively moves mouse cursor to point {@code from},
     * presses mouse button 1, drag mouse to point {@code to} and releases
     * mouse button 1 at last.
     *
     * @param   from    Source point
     * @param   to      Destination point
     *
     * @see     #mousePress(int)
     * @see     #glide(int, int, int, int)
     * @see     #dragAndDrop(int, int, int, int)
     */
    public void dragAndDrop(Point from, Point to){
        dragAndDrop(from.x, from.y, to.x, to.y);
    }

    /**
     * Successively presses and releases a given key.
     * <p>
     * Key codes that have more than one physical key associated with them
     * (e.g. {@code KeyEvent.VK_SHIFT} could mean either the
     * left or right shift key) will map to the left key.
     *
     * @param   keycode Key to press (e.g. {@code KeyEvent.VK_A})
     * @throws  IllegalArgumentException if {@code keycode} is not
     *          a valid key
     *
     * @see     java.awt.Robot#keyPress(int)
     * @see     java.awt.Robot#keyRelease(int)
     * @see     java.awt.event.KeyEvent
     */
    public void type(int keycode) {
        keyPress(keycode);
        waitForIdle(DEFAULT_SPEED);
        keyRelease(keycode);
        waitForIdle(DEFAULT_SPEED);
    }

    /**
     * Types given character
     *
     * @param   c   Character to be typed (e.g. {@code 'a'})
     *
     * @see     #type(int)
     * @see     java.awt.event.KeyEvent
     */
    public void type(char c) {
        type(KeyEvent.getExtendedKeyCodeForChar(c));
    }

    /**
     * Types given array of characters one by one
     *
     * @param   symbols Array of characters to be typed
     *
     * @see     #type(char)
     */
    public void type(char[] symbols) {
        for (int i = 0; i < symbols.length; i++) {
            type(symbols[i]);
        }
    }

    /**
     * Types given string
     *
     * @param   s   String to be typed
     *
     * @see     #type(char[])
     */
    public void type(String s) {
        type(s.toCharArray());
    }
}
