/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6428694
 * @summary Checks that double click closes JOptionPane's input dialog.
 * @library /lib/client
 * @build ExtendedRobot
 * @author Mikhail Lapshin
 * @run main bug6428694
*/

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.*;
import java.awt.event.InputEvent;

public class bug6428694 {
    private static JFrame frame;
    private static boolean mainIsWaitingForDialogClosing;
    private static ExtendedRobot robot;
    private static volatile boolean testPassed;

    public static void main(String[] args) throws Exception {
        robot = new ExtendedRobot();
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    bug6428694.setupUI();
                }
            });
            robot.waitForIdle();
            test();
        } finally {
            stopEDT();
        }

        if (testPassed) {
            System.out.println("Test passed");
        } else {
            throw new RuntimeException("JOptionPane doesn't close input dialog " +
                    "by double click!");
        }
    }

    private static void setupUI() {
        frame = new JFrame("bug6428694 test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Object[] selectedItems = new Object[40];
        for (int i = 0; i < 39; i++) {
            selectedItems[i] = ("item: " + i);
        }
        JOptionPane.showInputDialog(frame,
                "Double click on selected item then click cancel",
                "Test Option Dialog", JOptionPane.WARNING_MESSAGE, null,
                selectedItems, selectedItems[0]);

        // We are here if double click has closed the dialog
        // or when the EDT is stopping
        testPassed = mainIsWaitingForDialogClosing;
    }

    private static void test() {

        mainIsWaitingForDialogClosing = true;

        // Perform double click on an item
        int frameLeftX = frame.getLocationOnScreen().x;
        int frameUpperY = frame.getLocationOnScreen().y;
        robot.mouseMove(frameLeftX + 150, frameUpperY + 120);
        robot.waitForIdle();
        robot.delay(100);
        robot.setAutoDelay(50);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        // Wait for the input dialog closing
        robot.waitForIdle();
        robot.delay(2000);

        mainIsWaitingForDialogClosing = false;
    }

    private static void stopEDT() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
