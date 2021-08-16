/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;

/**
 * @test
 * @bug 8176359 8231564
 * @key headful
 * @requires (os.family == "windows" | os.family == "mac")
 * @summary setMaximizedBounds() should work if set to the screen other than
 *          current screen of the Frame, the size of the frame is intentionally
 *          big
 * @run main/othervm MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=1    MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=1.2  MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=1.25 MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=1.5  MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=1.75 MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=2    MaximizedToOppositeScreenBig
 * @run main/othervm -Dsun.java2d.uiScale=2.25 MaximizedToOppositeScreenBig
 */
public final class MaximizedToOppositeScreenBig {

    public static void main(String[] args) throws Exception {
        //Supported platforms are Windows and OS X.
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("windows") && !os.contains("os x")) {
            return;
        }

        if (!Toolkit.getDefaultToolkit().
                isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            return;
        }

        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        Robot robot = new Robot();
        for (GraphicsDevice gd1 : gds) {
            Rectangle framAt = gd1.getDefaultConfiguration().getBounds();
            framAt.grow(-200, -200);
            for (GraphicsDevice gd2 : gds) {
                Rectangle maxTo = gd2.getDefaultConfiguration().getBounds();
                maxTo.grow(-150, -150);
                Frame frame = new Frame(gd1.getDefaultConfiguration());
                try {
                    frame.setBounds(framAt);
                    frame.setVisible(true);
                    robot.waitForIdle();
                    robot.delay(1000);

                    frame.setMaximizedBounds(maxTo);
                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    robot.waitForIdle();
                    robot.delay(1000);

                    Rectangle actual = frame.getBounds();
                    if (!actual.equals(maxTo)) {
                        System.err.println("Actual: " + actual);
                        System.err.println("Expected: " + maxTo);
                        throw new RuntimeException("Wrong bounds");
                    }
                } finally {
                    frame.dispose();
                }
            }
        }
    }
}

