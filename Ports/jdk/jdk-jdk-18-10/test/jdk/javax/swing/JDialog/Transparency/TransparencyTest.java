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
 @test
 @key headful
 @bug 8062946 8159906
 @summary Verify Transparency upon iconify/deiconify sequence
 @run main TransparencyTest
 */
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TransparencyTest {

    private static JFrame frame;
    private static JDialog dialog;
    private static JDialog backgroundDialog;
    private static final int WIDTH = 250;
    private static final int HEIGHT = 250;
    private static final float OPACITY = 0.60f;
    private static volatile Point dlgPos;

    public static void createAndShowGUI() {
        frame = new JFrame("JFrame");
        frame.setSize(WIDTH, HEIGHT);
        frame.setLocation(100, 300);

        dialog = new JDialog(frame, false);
        dialog.setSize(250, 250);
        dialog.setUndecorated(true);
        dialog.setLocation(400, 300);
        dlgPos = dialog.getLocation();
        backgroundDialog = new JDialog(frame, false);
        backgroundDialog.setSize(250, 250);
        backgroundDialog.setUndecorated(true);
        backgroundDialog.getContentPane().setBackground(Color.red);
        backgroundDialog.setLocation(dlgPos.x, dlgPos.y);

        frame.setVisible(true);
        backgroundDialog.setVisible(true);
        dialog.setVisible(true);
    }

    public static void main(String[] args) throws Exception {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsDevice.WindowTranslucency mode = GraphicsDevice.WindowTranslucency.TRANSLUCENT;
        boolean translucencyCheck = gd.isWindowTranslucencySupported(mode);
        if(!translucencyCheck) {
            return;
        }

        Robot robot = new Robot();
        // create a GUI
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });
        robot.waitForIdle();
        robot.delay(200);

        int x = dlgPos.x + 100;
        int y = dlgPos.y + 100;
        Color opaque = robot.getPixelColor(x, y);

        // set Dialog Opacity
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                dialog.setOpacity(OPACITY);
            }
        });
        robot.waitForIdle();
        robot.delay(200);

        // iconify frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(JFrame.ICONIFIED);
            }
        });
        robot.waitForIdle();
        robot.delay(500);

        // deiconify frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(JFrame.NORMAL);
            }
        });
        robot.waitForIdle();
        robot.delay(500);

        Color transparent = robot.getPixelColor(x, y);
        if (transparent.equals(opaque)) {
            frame.dispose();
            throw new RuntimeException("JDialog transparency lost "
                    + "upon iconify/deiconify sequence");
        }
        frame.dispose();
    }
}
