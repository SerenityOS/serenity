/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7154048
 * @summary Programmatically resized  window does not receive mouse entered/exited events
 * @author  alexandr.scherbatiy area=awt.event
 * @run main ResizingFrameTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ResizingFrameTest {

    private static volatile int mouseEnteredCount = 0;
    private static volatile int mouseExitedCount = 0;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.mouseMove(100, 100);
        robot.delay(200);

        // create a frame under the mouse cursor
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });


        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 1 || mouseExitedCount != 0) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // iconify frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(Frame.ICONIFIED);
            }
        });

        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 1 || mouseExitedCount != 1) {
            throw new RuntimeException("No Mouse Entered/Exited events! "+mouseEnteredCount+", "+mouseExitedCount);
        }

        // deiconify frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(Frame.NORMAL);
            }
        });

        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 2 || mouseExitedCount != 1) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // move the mouse out of the frame
        robot.mouseMove(500, 500);
        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 2 || mouseExitedCount != 2) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // maximize the frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        });

        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 3 || mouseExitedCount != 2) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }


        // demaximize the frame
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setExtendedState(Frame.NORMAL);
            }
        });

        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 3 || mouseExitedCount != 3) {
            throw new RuntimeException("No Mouse Entered/Exited events!");

        }

        // move the frame under the mouse
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setLocation(400, 400);
            }
        });

        robot.waitForIdle();
        robot.delay(1000);

        if (mouseEnteredCount != 4 || mouseExitedCount != 3) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // move the frame out of the mouse
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setLocation(100, 100);
            }
        });

        robot.waitForIdle();
        robot.delay(400);

        if (mouseEnteredCount != 4 || mouseExitedCount != 4) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // enlarge the frame bounds
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setBounds(100, 100, 800, 800);
            }
        });

        robot.waitForIdle();
        robot.delay(200);

        if (mouseEnteredCount != 5 || mouseExitedCount != 4) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }

        // make the frame bounds smaller
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.setBounds(100, 100, 200, 300);
            }
        });

        robot.waitForIdle();
        robot.delay(400);


        if (mouseEnteredCount != 5 || mouseExitedCount != 5) {
            throw new RuntimeException("No Mouse Entered/Exited events!");
        }
    }

    private static void createAndShowGUI() {

        frame = new JFrame("Main Frame");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                mouseEnteredCount++;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseExitedCount++;
            }
        });

        frame.setVisible(true);
    }
}
