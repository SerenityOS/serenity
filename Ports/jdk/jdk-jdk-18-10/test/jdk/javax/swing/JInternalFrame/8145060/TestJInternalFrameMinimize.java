/*
 * Copyright (c) 2015,2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8145060
 * @summary Minimizing a JInternalFrame not shifting focus to frame below it
 * @library ../../regtesthelpers
 * @build Util
 * @run main TestJInternalFrameMinimize
 */

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.beans.PropertyVetoException;
import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class TestJInternalFrameMinimize {

    private static JDesktopPane desktopPane;
    private static JFrame frame = new JFrame("Test Frame");
    private static int count = 0;
    private static JMenu menu;
    private static JMenuBar menuBar;
    private static JMenuItem menuItem;
    private static Robot robot;
    private static ActionListener listener;
    private static Timer timer;
    private static int counter;
    private static boolean testFailed;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                createUI();
            }
        });
        robot.waitForIdle();
        executeTest();
        if (testFailed) {
            throw new RuntimeException("Test Failed");
        }
        dispose();
    }

    private static void createUI() {

        desktopPane = new JDesktopPane();
        frame.getContentPane().add(desktopPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = new JMenuItem("New Child");
        menuItem.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JInternalFrame f = new JInternalFrame("Child "
                                + (++count), true, true, true, true);
                        f.setSize(200, 300);
                        f.setLocation(count * 20, count * 20);
                        desktopPane.add(f);
                        f.setVisible(true);
                    }
                });
        menu.add(menuItem);
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void executeTest() throws Exception {

        Point point = Util.getCenterPoint(menu);
        performMouseOperations(point);
        point = Util.getCenterPoint(menuItem);
        performMouseOperations(point);
        point = Util.getCenterPoint(menu);
        performMouseOperations(point);
        point = Util.getCenterPoint(menuItem);
        performMouseOperations(point);
        point = Util.getCenterPoint(menu);
        performMouseOperations(point);
        point = Util.getCenterPoint(menuItem);
        performMouseOperations(point);
        point = Util.getCenterPoint(menu);
        performMouseOperations(point);
        point = Util.getCenterPoint(menuItem);
        performMouseOperations(point);
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                listener = new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        JInternalFrame internalFrame
                                = desktopPane.getSelectedFrame();
                        if (internalFrame != null) {
                            try {
                                internalFrame.setIcon(true);
                                ++counter;
                            } catch (PropertyVetoException ex) {
                            }
                        }
                        if (counter == 4) {
                            try {
                                timer.stop();
                                JInternalFrame currentSelectedFrame
                                        = desktopPane.getSelectedFrame();
                                if (internalFrame.equals(currentSelectedFrame)) {
                                    frame.dispose();
                                    testFailed = true;
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                };
            }
        });
        timer = new Timer(1000, listener);
        timer.start();
        robot.delay(1000);
    }

    private static void dispose() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static void performMouseOperations(Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(1000);
        robot.waitForIdle();
    }
}
