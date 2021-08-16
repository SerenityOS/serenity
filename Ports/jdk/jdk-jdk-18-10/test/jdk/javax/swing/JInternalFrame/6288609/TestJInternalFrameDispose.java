/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6288609
 * @summary JInternalFrame.setDefaultCloseOperation() interferes with "close"
              behavior
 * @library ../../regtesthelpers
 * @build Util
 * @run main TestJInternalFrameDispose
 */

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class TestJInternalFrameDispose {

    private static JDesktopPane desktopPane;
    private static JFrame frame = new JFrame("Test Frame");
    private static int count = 0;
    private static JMenu menu;
    private static JMenuBar menuBar;
    private static JMenuItem menuItem;
    private static Robot robot;
    private static JInternalFrame internalFrame;

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
        robot.delay(1000);
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
                        f.setDefaultCloseOperation(
                                JInternalFrame.DO_NOTHING_ON_CLOSE);
                        f.addInternalFrameListener(new InternalFrameAdapter() {
                            @Override
                            public void internalFrameClosing(
                                    InternalFrameEvent e) {
                                        e.getInternalFrame().dispose();
                                    }
                        });
                        f.setSize(200, 300);
                        f.setLocation(count * 20, count * 20);
                        desktopPane.add(f);
                        f.setVisible(true);
                    }
                });
        menu.add(menuItem);

        frame.setSize(400, 500);
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
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                internalFrame = desktopPane.getSelectedFrame();
                internalFrame.doDefaultCloseAction();
                internalFrame = desktopPane.getSelectedFrame();
            }
        });

        robot.delay(2000);
        if (internalFrame == null) {
            dispose();
            throw new RuntimeException("Test Failed");
        }
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
