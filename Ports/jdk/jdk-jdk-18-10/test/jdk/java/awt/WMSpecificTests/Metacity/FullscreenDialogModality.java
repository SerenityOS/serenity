/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8012586
 * @summary verify that modal dialog will appeared above fullscreen window under Metacity WM.
 * @requires os.family == "linux"
 * @modules java.desktop/sun.awt.X11
 * @library ../../regtesthelpers
 * @build Util
 * @run main FullscreenDialogModality
 * @run main/othervm FullscreenDialogModality
 * @author vkravets
 */

import test.java.awt.regtesthelpers.Util;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public class FullscreenDialogModality extends Frame {

    static Robot robot = null;

    public void enterFS() {
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        final boolean fs = gd.isFullScreenSupported();
        System.out.println("FullscreenSupported: " + (fs ? "yes" : "no"));
        gd.setFullScreenWindow(this);
        try {
            // Give the system time to set the FS window and display it
            // properly
            Thread.sleep(2000);
        } catch (Exception e) {}
    }

    public void exitFS() {
        GraphicsDevice gd = getGraphicsConfiguration().getDevice();
        // reset window
        gd.setFullScreenWindow(null);
        try {
            // Give the system time to set the FS window and display it
            // properly
            Thread.sleep(2000);
        } catch (Exception e) {}
    }

    public void checkDialogModality() throws InvocationTargetException, InterruptedException {
        // Dialog
        final Dialog d = new Dialog(FullscreenDialogModality.this, "Modal dialog", Dialog.ModalityType.APPLICATION_MODAL);
        d.setBounds(500, 500, 160, 160);
        d.setModal(true);
        d.setBackground(Color.red);
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                d.setVisible(true);
            }
        });
        // Wait until the dialog is shown
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Empty
            }
        });

        Util.waitForIdle(robot);
        try {
            //Check color
            Point checkPoint = new Point(d.getX() + d.getWidth() / 2, d.getY() + d.getHeight() / 2);
            Color actual = robot.getPixelColor(checkPoint.x, checkPoint.y);
            System.out.println("Color = " + actual);
            if (actual.getRGB() == Color.GREEN.getRGB()) {
                throw new RuntimeException("Test FAILED: Modal dialog shown below fullscreen window");
            } else if (actual.getRGB() == Color.RED.getRGB()) {
                System.out.println("Test PASSED: Modal dialog shown above fullscreen window");
            } else {
                System.out.println("pixelColor " +
                        Integer.toHexString(actual.getRGB()) +
                        " at coordinates (" + checkPoint.x + ", " + checkPoint.y + ")");
                throw new RuntimeException("Test FAILED: Unexpected behavior");
            }

            robot.delay(2000);
            Util.waitForIdle(robot);
        } finally {
            d.dispose();
        }
    }

    public static void main(String args[]) throws InvocationTargetException, InterruptedException {
        if (Util.getWMID() != Util.METACITY_WM) {
            System.out.println("This test is only useful on Metacity");
            return;
        }
        robot = Util.createRobot();
        Util.waitForIdle(robot);
        final FullscreenDialogModality frame = new FullscreenDialogModality();
        frame.setUndecorated(true);
        frame.setBackground(Color.green);
        frame.setSize(500, 500);
        frame.setVisible(true);
        try {
            robot.delay(100);
            Util.waitForIdle(robot);

            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    frame.enterFS();
                }
            });
            robot.delay(200);
            Util.waitForIdle(robot);

            frame.checkDialogModality();

            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    frame.exitFS();
                }
            });
        } finally {
            frame.dispose();
        }
    }
}
