/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8145896 8194944
 * @summary JInternalFrame setMaximum before adding to desktop throws null pointer exception
 * @library ../../regtesthelpers
 * @build Util
 * @run main TestJInternalFrameMaximize
 */

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyVetoException;
import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class TestJInternalFrameMaximize {

    private static JDesktopPane desktopPane;
    private static JFrame frame;
    private static int count = 0;
    private static JMenu menu;
    private static JMenuBar menuBar;
    private static JMenuItem menuItem;
    private static Robot robot;
    private static volatile String errorMessage = "";
    private static volatile boolean isFrameShowing;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            try {
                String lookAndFeelString = lookAndFeelItem.getClassName();
                if (tryLookAndFeel(lookAndFeelString)) {
                    createUI();
                    robot.waitForIdle();
                    blockTillDisplayed(frame);
                    executeTest();
                    robot.delay(1000);
                }
            } finally {
                frame.dispose();
                isFrameShowing = false;
                robot.waitForIdle();
            }
        }
        if (!"".equals(errorMessage)) {
            throw new RuntimeException(errorMessage);
        }
    }

    private static boolean tryLookAndFeel(String lookAndFeelString) {
        try {
            UIManager.setLookAndFeel(lookAndFeelString);
            return true;
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException |
                InstantiationException | IllegalAccessException e) {
            errorMessage += e.getMessage() + "\n";
            System.err.println("Caught Exception: " + e.getMessage());
            return false;
        }
    }

    private static void createUI() throws Exception {

        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("Test Frame");
            desktopPane = new JDesktopPane();
            frame.getContentPane().add(desktopPane);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            menuBar = new JMenuBar();
            frame.setJMenuBar(menuBar);

            menu = new JMenu("File");
            menuBar.add(menu);

            menuItem = new JMenuItem("New Child");
            menuItem.addActionListener((ActionEvent e) -> {
                try {
                    JInternalFrame f = new JInternalFrame("Child "
                            + (++count), true, true, true, true);
                    f.setSize(200, 300);
                    f.setLocation(count * 20, count * 20);
                    f.setMaximum(true);
                    desktopPane.add(f);
                    f.setVisible(true);
                } catch (PropertyVetoException ex) {
                } catch (RuntimeException ex) {
                    errorMessage = "Test Failed";
                }
            });
            menu.add(menuItem);
            frame.setSize(500, 500);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void blockTillDisplayed(JFrame frame) throws Exception {
        while (!isFrameShowing) {
            try {
                SwingUtilities.invokeAndWait(()-> isFrameShowing = frame.isShowing());
                if (!isFrameShowing) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void executeTest() throws Exception {
        Point point = Util.getCenterPoint(menu);
        performMouseOperations(point);
        point = Util.getCenterPoint(menuItem);
        performMouseOperations(point);
    }

    private static void performMouseOperations(Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(1000);
        robot.waitForIdle();
    }
}
