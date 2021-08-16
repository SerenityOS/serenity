/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.peer;

import java.awt.Rectangle;
import java.awt.Robot;

/**
 * RobotPeer defines an interface whereby toolkits support automated testing
 * by allowing native input events to be generated from Java code.
 *
 * This interface should not be directly imported by code outside the
 * java.awt.* hierarchy; it is not to be considered public and is subject
 * to change.
 *
 * @author      Robi Khan
 */
public interface RobotPeer
{
    /**
     * Moves the mouse pointer to the specified screen location.
     *
     * @param x the X location on screen
     * @param y the Y location on screen
     *
     * @see Robot#mouseMove(int, int)
     */
    void mouseMove(int x, int y);

    /**
     * Simulates a mouse press with the specified button(s).
     *
     * @param buttons the button mask
     *
     * @see Robot#mousePress(int)
     */
    void mousePress(int buttons);

    /**
     * Simulates a mouse release with the specified button(s).
     *
     * @param buttons the button mask
     *
     * @see Robot#mouseRelease(int)
     */
    void mouseRelease(int buttons);

    /**
     * Simulates mouse wheel action.
     *
     * @param wheelAmt number of notches to move the mouse wheel
     *
     * @see Robot#mouseWheel(int)
     */
    void mouseWheel(int wheelAmt);

    /**
     * Simulates a key press of the specified key.
     *
     * @param keycode the key code to press
     *
     * @see Robot#keyPress(int)
     */
    void keyPress(int keycode);

    /**
     * Simulates a key release of the specified key.
     *
     * @param keycode the key code to release
     *
     * @see Robot#keyRelease(int)
     */
    void keyRelease(int keycode);

    /**
     * Gets the RGB value of the specified pixel on screen.
     *
     * @param x the X screen coordinate
     * @param y the Y screen coordinate
     *
     * @return the RGB value of the specified pixel on screen
     *
     * @see Robot#getPixelColor(int, int)
     */
    int getRGBPixel(int x, int y);

    /**
     * Gets the RGB values of the specified screen area as an array.
     *
     * @param bounds the screen area to capture the RGB values from
     *
     * @return the RGB values of the specified screen area
     *
     * @see Robot#createScreenCapture(Rectangle)
     */
    int[] getRGBPixels(Rectangle bounds);

    /**
     * Determines if absolute coordinates should be used by this peer.
     *
     * @return {@code true} if absolute coordinates should be used,
     *         {@code false} otherwise
     */
    default boolean useAbsoluteCoordinates() {
        return false;
    }
}
