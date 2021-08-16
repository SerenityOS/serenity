/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6837004
 * @summary Checks that non-opaque window can be made a fullscreen window
 * @author Artem Ananiev
 * @run main TranslucentWindow
 */

import java.awt.*;
import java.awt.geom.*;

import static java.awt.GraphicsDevice.WindowTranslucency.*;


public class TranslucentWindow {
    public static void main(String args[]) {
        Robot robot;
        try {
            robot = new Robot();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        Frame f = new Frame("Test frame");
        f.setUndecorated(true);
        f.setBounds(100, 100, 320, 240);

        // First, check it can be made fullscreen window without any effects applied
        gd.setFullScreenWindow(f);
        robot.waitForIdle();

        gd.setFullScreenWindow(null);
        robot.waitForIdle();

        // Second, check if it applying any effects doesn't prevent the window
        // from going into the fullscreen mode
        if (gd.isWindowTranslucencySupported(PERPIXEL_TRANSPARENT)) {
            f.setShape(new Ellipse2D.Float(0, 0, f.getWidth(), f.getHeight()));
        }
        if (gd.isWindowTranslucencySupported(TRANSLUCENT)) {
            f.setOpacity(0.5f);
        }
        if (gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT)) {
            f.setBackground(new Color(0, 0, 0, 128));
        }
        gd.setFullScreenWindow(f);
        robot.waitForIdle();

        // Third, make sure all the effects are unset when entering the fullscreen mode
        if (f.getShape() != null) {
            throw new RuntimeException("Test FAILED: fullscreen window shape is not null");
        }
        if (Math.abs(f.getOpacity() - 1.0f) > 1e-4) {
            throw new RuntimeException("Test FAILED: fullscreen window opacity is not 1.0f");
        }
        Color bgColor = f.getBackground();
        if ((bgColor != null) && (bgColor.getAlpha() != 255)) {
            throw new RuntimeException("Test FAILED: fullscreen window background color is not opaque");
        }

        f.dispose();
        System.out.println("Test PASSED");
    }
}
