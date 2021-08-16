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
 * @test
 * @key headful
 * @bug 8137169
 * @summary verifies TabbedScrollPane minimum height for all Look and Feels
 * @library ../../regtesthelpers
 * @build Util
 * @run main ScrollableTabbedPaneTest
 */

import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class ScrollableTabbedPaneTest {

    private static JFrame frame;
    private static JTabbedPane pane;
    private static Robot robot;
    private static volatile String errorString = "";

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.delay(1000);
        UIManager.LookAndFeelInfo[] lookAndFeelArray
                = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            executeCase(lookAndFeelItem.getClassName(),
                        lookAndFeelItem.getName());
        }
        if (!"".equals(errorString)) {
            throw new RuntimeException("Error Log:\n" + errorString);
        }
    }

    private static void executeCase(String lookAndFeelString, String shortLAF)
            throws Exception {
        if (tryLookAndFeel(lookAndFeelString)) {
            createUI(shortLAF);
            stepsToExecute(shortLAF);

            createLeftUI(shortLAF);
            stepsToExecute(shortLAF);

            createRightUI(shortLAF);
            stepsToExecute(shortLAF);
        }
    }

    private static void stepsToExecute(String shortLAF) throws Exception {
        robot.delay(100);
        runTestCase(shortLAF);
        robot.delay(1000);
        cleanUp();
        robot.delay(1000);
    }

    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);

        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            return false;
        }
        return true;
    }

    private static void cleanUp() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    private static void createUI(final String shortLAF)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame(shortLAF);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                pane = new JTabbedPane();
                pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                frame.add(pane);
                frame.setSize(500, 500);
            }
        });
    }
    private static void createLeftUI(final String shortLAF)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame(shortLAF);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                pane = new JTabbedPane();
                pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                pane.setTabPlacement(SwingConstants.LEFT);
                frame.add(pane);
                frame.setSize(500, 500);
            }
        });
    }

    private static void createRightUI(final String shortLAF)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame(shortLAF);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                pane = new JTabbedPane();
                pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
                pane.setTabPlacement(SwingConstants.RIGHT);
                frame.add(pane);
                frame.setSize(500, 500);
            }
        });
    }

    private static void runTestCase(String shortLAF) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                int value= 0;
                do {
                    String title = "Tab" + (i + 1);
                    pane.addTab(title, new JPanel());
                    int tempValue = pane.getMinimumSize().height;
                    if(value==0) {
                        value = tempValue;
                    }
                    if(value != tempValue) {
                        String error = "[" + shortLAF
                            + "]: [Error]: TabbedScrollPane fails";
                    errorString += error;
                    }

                    ++i;
                } while (i < 10);
            }
        });
    }
}

