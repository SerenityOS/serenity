/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires (os.family == "linux")
 * @key headful
 * @bug 8248637
 * @summary Tests selected JMenu and JMenuitem is properly highlighted in GTKL&F
 *  with gtk3 version
 * @run main/othervm -Djdk.gtk.version=3 JMenuSelectedColorTest
 */

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;

public class JMenuSelectedColorTest {
    private static JFrame frame;
    private static JMenu menu;
    private static JMenuItem menuitem;
    private static Point point;
    private static Rectangle rect;
    private static Robot robot;
    private static final String GTK_LAF_CLASS = "GTKLookAndFeel";
    private static int minColorDifference = 100;

    private static void blockTillDisplayed(Component comp) {
        Point p = null;
        while (p == null) {
            try {
                p = comp.getLocationOnScreen();
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    private static int getMaxColorDiff(Color c1, Color c2) {
        return Math.max(Math.abs(c1.getRed() - c2.getRed()),
                Math.max(Math.abs(c1.getGreen() - c2.getGreen()),
                        Math.abs(c1.getBlue() - c2.getBlue())));
    }

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name").startsWith("Linux")) {
            System.out.println("This test is meant for Linux platform only");
            return;
        }

        for (UIManager.LookAndFeelInfo lookAndFeelInfo :
                UIManager.getInstalledLookAndFeels()) {
            if (lookAndFeelInfo.getClassName().contains(GTK_LAF_CLASS)) {
                try {
                    UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                } catch (final UnsupportedLookAndFeelException ignored) {
                    System.out.println("GTK L&F could not be set, so this " +
                            "test can not be run in this scenario ");
                    return;
                }
            }
        }

        robot = new Robot();
        robot.setAutoDelay(100);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    menu = new JMenu("         ") ;
                    menuitem = new JMenuItem("        ");
                    menu.add(menuitem);

                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());

                    JMenuBar menuBar = new JMenuBar();
                    JPanel menuPanel = new JPanel();

                    menuPanel.setLayout(new FlowLayout());

                    menuBar.add(menu);
                    menuPanel.add(menuBar);
                    panel.add(menuPanel, BorderLayout.CENTER);
                    frame = new JFrame("JMenuSelectedColor");
                    frame.add(panel);
                    frame.setSize(200, 200);
                    frame.setAlwaysOnTop(true);
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();
            robot.delay(500);

            blockTillDisplayed(menu);
            SwingUtilities.invokeAndWait(() -> {
                point = menu.getLocationOnScreen();
                rect = menu.getBounds();
            });
            robot.waitForIdle();
            robot.delay(500);

            Color backgroundColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            menu.setSelected(true);
            robot.waitForIdle();
            robot.delay(500);

            Color highlightColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            int actualColorDifference = getMaxColorDiff(backgroundColor, highlightColor);
            if (actualColorDifference < minColorDifference) {
                throw new RuntimeException("The expected highlight color for " +
                        "Menu was not found");
            }

            robot.mouseMove(point.x + rect.width / 2,
                    point.y + rect.height / 2);
            robot.waitForIdle();
            robot.delay(500);

            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            robot.delay(500);

            blockTillDisplayed(menuitem);
            SwingUtilities.invokeAndWait(() -> {
                point = menuitem.getLocationOnScreen();
                rect = menuitem.getBounds();
            });
            robot.waitForIdle();
            robot.delay(500);

            backgroundColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            robot.mouseMove(point.x + rect.width / 2,
                    point.y + rect.height / 2);
            robot.waitForIdle();
            robot.delay(500);

            highlightColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            actualColorDifference = getMaxColorDiff(backgroundColor, highlightColor);
            if (actualColorDifference < minColorDifference) {
                throw new RuntimeException("The expected highlight color for " +
                        "Menuitem was not found");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }
}
