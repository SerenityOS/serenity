/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8190230 8196360
 * @summary [macosx] Order of overlapping of modal dialogs is wrong
 * @key headful
 * @run main SiblingChildOrderTest
 */

import java.awt.Color;
import java.awt.Robot;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SiblingChildOrderTest
{
    static Color[] colors = new Color[]{Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
    static int[] x = new int[]{200, 150, 100, 50};
    static int[] y = new int[]{200, 150, 100, 50};
    static JDialog[] dlgs = new JDialog[4];
    private static JFrame frame;

    public static void main(String args[]) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("FRAME");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true);
            frame.setBounds(50,50, 400, 400);
            frame.setVisible(true);
        });

        for (int i = 0; i < colors.length; i++) {
            int finalI = i;
            SwingUtilities.invokeLater(() -> {
                dlgs[finalI] = new JDialog(frame, "DLG " + finalI, true);
                dlgs[finalI].getContentPane().setBackground(colors[finalI]);
                dlgs[finalI].setBounds(x[finalI], y[finalI], 200, 200);
                dlgs[finalI].setUndecorated(true);
                dlgs[finalI].setVisible(true);
            });
        }

        Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(1000);

        for (int i = 0; i < colors.length; i++) {
            Color c = robot.getPixelColor(x[i] + 190, y[i] + 190);
        if (!c.equals(colors[i])) {
                throw new RuntimeException("Expected " + colors[i] + " got " + c);
            }
        }

        for (int i = 0; i < colors.length; i++) {
            SwingUtilities.invokeLater(dlgs[i]::dispose);
        }
        SwingUtilities.invokeLater(frame::dispose);
    }
}
