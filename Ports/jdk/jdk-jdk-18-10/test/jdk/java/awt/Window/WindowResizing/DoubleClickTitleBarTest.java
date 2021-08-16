/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190192
 * @requires os.family=="mac"
 * @summary Double click on the title bar no longer repositions the window
 * @run main DoubleClickTitleBarTest
 */

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Robot;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.AWTException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

public class DoubleClickTitleBarTest {
    private static Point position = null;
    private static JFrame frame = null;
    private static boolean windowMinimizedState = false, windowMaximizedState = false;
    //offset from top left to some place on title bar
    final private static int X_OFFSET = 100, Y_OFFSET = 7;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    doTest();
                }
            });

            Robot robot = new Robot();
            robot.delay(500);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    position = frame.getLocationOnScreen();
                }
            });

            //offset from the top left
            robot.mouseMove(position.x + X_OFFSET,
                                position.y + Y_OFFSET);

            //do resize
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseMove(frame.getLocationOnScreen().x + 200,
                                frame.getLocationOnScreen().y + 200);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(500);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    position = frame.getLocationOnScreen();
                }
            });

            //after resize, do offset from top left
            robot.mouseMove(position.x + X_OFFSET,
                                position.y + Y_OFFSET);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            //wait till maximizing the window
            robot.delay(1000);

            if(!(windowMinimizedState && windowMaximizedState)) {
                throw new RuntimeException("Test failed:");
            }
        } finally {
            if(frame != null) {
                frame.dispose();
            }
        }
    }

    private static void doTest() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();

        WindowStateListener listener = new WindowAdapter() {
            public void windowStateChanged(WindowEvent evt) {
                int oldState = evt.getOldState();
                int newState = evt.getNewState();

                if((oldState & JFrame.MAXIMIZED_BOTH) != 0 &&
                    (newState & JFrame.MAXIMIZED_BOTH) == 0) {
                    windowMinimizedState = true;
                }
                else if(windowMinimizedState &&
                         (oldState & JFrame.MAXIMIZED_BOTH) == 0 &&
                         (newState & JFrame.MAXIMIZED_BOTH) != 0) {
                    windowMaximizedState = true;
                }
            }
        };

        frame.addWindowStateListener(listener);
        frame.setSize(200, 200);
        frame.setLocation(100, 100);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setResizable(true);
        frame.setVisible(true);
    }
}

