/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6232687
 * @summary Tests that Window.setLocationRelativeTo() method works correctly
 *          for different multiscreen configurations
 * @author artem.ananiev, area=awt.multiscreen
 * @library ../../regtesthelpers
 * @build Util
 * @run main LocationRelativeToTest
 */

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import test.java.awt.regtesthelpers.Util;

public class LocationRelativeToTest
{
    private static int FRAME_WIDTH = 400;
    private static int FRAME_HEIGHT = 300;

    public static void main(String[] args)
    {
        Robot r = Util.createRobot();

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        System.out.println("Center point: " + ge.getCenterPoint());
        GraphicsDevice[] gds = ge.getScreenDevices();
        GraphicsDevice gdDef = ge.getDefaultScreenDevice();
        GraphicsConfiguration gcDef =
            gdDef.getDefaultConfiguration();

        Frame f = new Frame("F", gcDef);
        f.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        f.setVisible(true);
        Util.waitForIdle(r);

        // first, check setLocationRelativeTo(null)
        f.setLocationRelativeTo(null);
        Util.waitForIdle(r);
        checkLocation(f, ge.getCenterPoint());

        for (GraphicsDevice gd : gds)
        {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle gcBounds = gc.getBounds();
            Frame f2 = new Frame("F2", gc);
            f2.setBounds(gcBounds.x + 100, gcBounds.y + 100,
                         FRAME_WIDTH, FRAME_HEIGHT);

            // second, check setLocationRelativeTo(invisible)
            f.setLocationRelativeTo(f2);
            Util.waitForIdle(r);
            checkLocation(f, new Point(gcBounds.x + gcBounds.width / 2,
                                       gcBounds.y + gcBounds.height / 2));

            // third, check setLocationRelativeTo(visible)
            f2.setVisible(true);
            Util.waitForIdle(r);
            Point f2Loc = f2.getLocationOnScreen();
            f.setLocationRelativeTo(f2);
            Util.waitForIdle(r);
            checkLocation(f, new Point(f2Loc.x + f2.getWidth() / 2,
                                       f2Loc.y + f2.getHeight() / 2));
        }
    }

    /*
     * Here the check is performed. Note this check works correctly both
     * for virtual (Win32, X11/Xinerama) and non-virtual (X11/non-Xinerama)
     * screen configurations.
     */
    private static void checkLocation(Frame f, Point rightLoc)
    {
        Point actualLoc = new Point(f.getBounds().x + f.getWidth() / 2,
                                    f.getBounds().y + f.getHeight() / 2);
        if (!rightLoc.equals(actualLoc))
        {
            System.err.println("Right location for the window center: " + rightLoc);
            System.err.println("Actual location for the window center: " + actualLoc);
            throw new RuntimeException("Test FAILED: setLocationRelativeTo() operates incorrectly");
        }
    }
}
