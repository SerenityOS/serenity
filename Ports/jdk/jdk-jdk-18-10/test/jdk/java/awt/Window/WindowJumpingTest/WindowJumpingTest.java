/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8080729
 * @summary Dialogs on multiscreen jump to parent frame on focus gain
 * @author Dmitry Markov
 * @library ../../regtesthelpers
 * @build Util
 * @run main WindowJumpingTest
 */
import java.awt.*;

import test.java.awt.regtesthelpers.Util;

public class WindowJumpingTest {
    public static void main(String[] args) throws AWTException {
        Robot r = Util.createRobot();

        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
        if (graphicsDevices.length < 2) {
            System.out.println("This is multi-screen test... Skipping!");
            return;
        }

        Frame frame = new Frame("Frame", graphicsDevices[0].getDefaultConfiguration());
        frame.setSize(400, 300);
        frame.setVisible(true);
        Util.waitForIdle(r);

        Dialog dialog = new Dialog(frame, "Dialog", false, graphicsDevices[1].getDefaultConfiguration());
        dialog.setSize(400, 300);
        dialog.setVisible(true);
        Util.waitForIdle(r);

        checkGraphicsDevice(frame, graphicsDevices[0]);
        checkGraphicsDevice(dialog, graphicsDevices[1]);

        Util.clickOnComp(frame, r);
        Util.waitForIdle(r);

        checkGraphicsDevice(frame, graphicsDevices[0]);
        checkGraphicsDevice(dialog, graphicsDevices[1]);

        Util.clickOnComp(dialog, r);
        Util.waitForIdle(r);

        checkGraphicsDevice(frame, graphicsDevices[0]);
        checkGraphicsDevice(dialog, graphicsDevices[1]);

        dialog.dispose();
        frame.dispose();
    }

    private static void checkGraphicsDevice(Window window, GraphicsDevice graphicsDevice) {
        GraphicsDevice actualGraphicsDevice = window.getGraphicsConfiguration().getDevice();

        if (!actualGraphicsDevice.equals(graphicsDevice)) {
            System.err.println("Expected screen: " + graphicsDevice);
            System.err.println("Actual screen: "+ actualGraphicsDevice);
            throw new RuntimeException("Test FAILED: " + window + " is displayed on wrong screen");
        }
    }
}

