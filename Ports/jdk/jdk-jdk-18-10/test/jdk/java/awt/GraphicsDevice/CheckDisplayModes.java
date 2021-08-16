/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @key headful
 * @bug 8007146 8213119
 * @summary [macosx] Setting a display mode crashes JDK under VNC
 */
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class CheckDisplayModes {

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice graphicDevice : ge.getScreenDevices()) {
            if (!graphicDevice.isDisplayChangeSupported()) {
                System.err.println("Display mode change is not supported on this host. Test is considered passed.");
                continue;
            }
            DisplayMode defaultDisplayMode = graphicDevice.getDisplayMode();
            checkDisplayMode(defaultDisplayMode);
            graphicDevice.setDisplayMode(defaultDisplayMode);

            DisplayMode[] displayModes = graphicDevice.getDisplayModes();
            boolean isDefaultDisplayModeIncluded = false;
            for (DisplayMode displayMode : displayModes) {
                checkDisplayMode(displayMode);
                graphicDevice.setDisplayMode(displayMode);
                if (defaultDisplayMode.equals(displayMode)) {
                    isDefaultDisplayModeIncluded = true;
                }
            }

            if (!isDefaultDisplayModeIncluded) {
                throw new RuntimeException("Default display mode is not included");
            }
        }
    }

    static void checkDisplayMode(DisplayMode displayMode) {
        if (displayMode == null || displayMode.getWidth() <= 1 || displayMode.getHeight() <= 1) {
            throw new RuntimeException("invalid display mode");
        }
    }
}
