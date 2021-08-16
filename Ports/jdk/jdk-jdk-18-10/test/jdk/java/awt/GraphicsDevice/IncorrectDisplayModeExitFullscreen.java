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


import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

/**
 * @test
 * @key headful
 * @bug 8019587
 * @author Sergey Bylokhov
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main IncorrectDisplayModeExitFullscreen
 */
public class IncorrectDisplayModeExitFullscreen {
    static ExtendedRobot robot;

    public static void main(final String[] args) {

        final GraphicsDevice[] devices =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                                   .getScreenDevices();
        if (devices.length < 2 || devices[0].getDisplayModes().length < 2
                || !devices[0].isDisplayChangeSupported()
                || !devices[0].isFullScreenSupported()
                || !devices[1].isFullScreenSupported()) {
            System.err.println("Testcase is not applicable");
            return;
        }
        final DisplayMode defaultDM = devices[0].getDisplayMode();
        final DisplayMode[] dms = devices[0].getDisplayModes();
        DisplayMode nonDefaultDM = null;

        for (final DisplayMode dm : dms) {
            if (!dm.equals(defaultDM)) {
                nonDefaultDM = dm;
                break;
            }
        }
        if (nonDefaultDM == null) {
            System.err.println("Testcase is not applicable");
            return;
        }

        try {
            robot = new ExtendedRobot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        final Frame frame = new Frame();
        frame.setBackground(Color.GREEN);
        frame.setUndecorated(true);
        try {
            devices[0].setFullScreenWindow(frame);
            sleep();
            devices[0].setDisplayMode(nonDefaultDM);
            sleep();
            devices[1].setFullScreenWindow(frame);
            sleep();
            if (!defaultDM.equals(devices[0].getDisplayMode())) {
                throw new RuntimeException("DisplayMode is not restored");
            }
        } finally {
            // cleaning up
            devices[0].setFullScreenWindow(null);
            devices[1].setFullScreenWindow(null);
            frame.dispose();
        }
    }
    private static void sleep() {
        robot.waitForIdle(1500);
    }
}
