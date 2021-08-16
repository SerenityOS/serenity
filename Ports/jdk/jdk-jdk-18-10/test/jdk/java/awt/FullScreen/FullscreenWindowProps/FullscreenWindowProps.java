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

import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/**
 * @test
 * @bug 8211999
 * @key headful
 * @summary verifies the full-screen window bounds and graphics configuration
 */
public final class FullscreenWindowProps {

    public static void main(String[] args) throws Exception {
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        if (!gd.isFullScreenSupported()) {
            return;
        }

        Frame frame = new Frame() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.setColor(Color.GREEN);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        try {
            frame.setUndecorated(true); // workaround JDK-8256257
            frame.setBackground(Color.MAGENTA);
            frame.setVisible(true);
            gd.setFullScreenWindow(frame);
            Thread.sleep(4000);

            for (DisplayMode dm : gd.getDisplayModes()) {
                if (dm.getWidth() == 1024 && dm.getHeight() == 768) {
                    gd.setDisplayMode(dm);
                    Thread.sleep(4000);
                    break;
                }
            }

            GraphicsConfiguration frameGC = frame.getGraphicsConfiguration();
            Rectangle frameBounds = frame.getBounds();

            GraphicsConfiguration screenGC = gd.getDefaultConfiguration();
            Rectangle screenBounds = screenGC.getBounds();

            if (frameGC != screenGC) {
                System.err.println("Expected: " + screenGC);
                System.err.println("Actual: " + frameGC);
                throw new RuntimeException();
            }

            checkSize(frameBounds.x, screenBounds.x, "x");
            checkSize(frameBounds.y, screenBounds.y, "Y");
            checkSize(frameBounds.width, screenBounds.width, "width");
            checkSize(frameBounds.height, screenBounds.height, "height");
        } finally {
            gd.setFullScreenWindow(null);
            frame.dispose();
            Thread.sleep(10000);
        }
    }

    private static void checkSize(int actual, int expected, String prop) {
        if (Math.abs(actual - expected) > 30) { // let's allow size variation,
                                                // the bug is reproduced anyway
            System.err.println("Expected: " + expected);
            System.err.println("Actual: " + actual);
            throw new RuntimeException(prop + " is wrong");
        }
    }
}
