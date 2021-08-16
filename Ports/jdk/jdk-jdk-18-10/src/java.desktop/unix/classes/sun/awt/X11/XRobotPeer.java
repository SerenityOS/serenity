/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.peer.RobotPeer;
import java.security.AccessController;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;
import sun.awt.UNIXToolkit;
import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;
import sun.security.action.GetPropertyAction;

@SuppressWarnings("removal")
final class XRobotPeer implements RobotPeer {

    private static final boolean tryGtk;
    static {
        loadNativeLibraries();
        tryGtk = Boolean.parseBoolean(
                            AccessController.doPrivileged(
                                    new GetPropertyAction("awt.robot.gtk", "true")
                            ));
    }
    private static volatile boolean useGtk;
    private final X11GraphicsConfig  xgc;

    XRobotPeer(X11GraphicsDevice gd) {
        xgc = (X11GraphicsConfig) gd.getDefaultConfiguration();
        SunToolkit tk = (SunToolkit)Toolkit.getDefaultToolkit();
        setup(tk.getNumberOfButtons(),
                AWTAccessor.getInputEventAccessor().getButtonDownMasks());

        boolean isGtkSupported = false;
        if (tryGtk) {
            if (tk instanceof UNIXToolkit && ((UNIXToolkit) tk).loadGTK()) {
                isGtkSupported = true;
            }
        }

        useGtk = (tryGtk && isGtkSupported);
    }

    @Override
    public void mouseMove(int x, int y) {
        mouseMoveImpl(xgc, xgc.scaleUp(x), xgc.scaleUp(y));
    }

    @Override
    public void mousePress(int buttons) {
        mousePressImpl(buttons);
    }

    @Override
    public void mouseRelease(int buttons) {
        mouseReleaseImpl(buttons);
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        mouseWheelImpl(wheelAmt);
    }

    @Override
    public void keyPress(int keycode) {
        keyPressImpl(keycode);
    }

    @Override
    public void keyRelease(int keycode) {
        keyReleaseImpl(keycode);
    }

    @Override
    public int getRGBPixel(int x, int y) {
        int[] pixelArray = new int[1];
        getRGBPixelsImpl(xgc, x, y, 1, 1, pixelArray, useGtk);
        return pixelArray[0];
    }

    @Override
    public int [] getRGBPixels(Rectangle bounds) {
        int[] pixelArray = new int[bounds.width*bounds.height];
        getRGBPixelsImpl(xgc, bounds.x, bounds.y, bounds.width, bounds.height,
                            pixelArray, useGtk);
        return pixelArray;
    }

    private static synchronized native void setup(int numberOfButtons, int[] buttonDownMasks);
    private static native void loadNativeLibraries();

    private static synchronized native void mouseMoveImpl(X11GraphicsConfig xgc, int x, int y);
    private static synchronized native void mousePressImpl(int buttons);
    private static synchronized native void mouseReleaseImpl(int buttons);
    private static synchronized native void mouseWheelImpl(int wheelAmt);

    private static synchronized native void keyPressImpl(int keycode);
    private static synchronized native void keyReleaseImpl(int keycode);

    private static synchronized native void getRGBPixelsImpl(X11GraphicsConfig xgc,
            int x, int y, int width, int height, int[] pixelArray, boolean isGtkSupported);
}
