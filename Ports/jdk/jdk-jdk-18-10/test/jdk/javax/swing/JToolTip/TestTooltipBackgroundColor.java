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
 * @bug 8214253
 * @summary Tests JToolTip background color
 * @run main TestTooltipBackgroundColor
 */

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;

public class TestTooltipBackgroundColor {
    private static JFrame frame;
    private static JLabel label;
    private static Point point;
    private static Rectangle rect;
    private static Robot robot;
    private static final String GTK_LAF_CLASS = "GTKLookAndFeel";
    private static int minColorDifference = 100;

    private static final CountDownLatch latch = new CountDownLatch(1);

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

    public static Component findSubComponent(Component parent, String className) {
        String parentClassName = parent.getClass().getName();

        if (parentClassName.contains(className)) {
            return parent;
        }

        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                Component subComponent = findSubComponent(child, className);

                if (subComponent != null) {
                    return subComponent;
                }
            }
        }
        return null;
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
                    JPanel panel = new JPanel();
                    label = new JLabel("Label with tooltip") {
                        public Point getToolTipLocation(MouseEvent event) {
                            return new Point(0, 0);
                        }
                    };
                    label.setToolTipText("<html> This is tooltip <br><br><br><br></html>");
                    panel.add(label, BorderLayout.CENTER);
                    frame = new JFrame("TestTooltipBackgroundColor");
                    frame.add(panel);
                    frame.setSize(200, 200);
                    frame.setAlwaysOnTop(true);
                    frame.setLocationRelativeTo(null);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    JLayeredPane layeredPane = (JLayeredPane) findSubComponent(
                            frame, "JLayeredPane");
                    layeredPane.addContainerListener(new ContainerAdapter() {
                        @Override
                        public void componentAdded(ContainerEvent e) {
                            latch.countDown();
                        }
                    });
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();
            robot.delay(500);

            blockTillDisplayed(label);
            SwingUtilities.invokeAndWait(() -> {
                point = label.getLocationOnScreen();
                rect = label.getBounds();
            });

            robot.waitForIdle();
            robot.delay(500);

            Color backgroundColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height*2);
            robot.waitForIdle();
            robot.delay(500);

            robot.mouseMove(point.x + rect.width / 2,
                    point.y + rect.height / 2);

            latch.await();

            robot.waitForIdle();
            robot.delay(500);

            Color highlightColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height*2);
            robot.waitForIdle();
            robot.delay(500);

            int actualColorDifference = getMaxColorDiff(backgroundColor, highlightColor);
            if (actualColorDifference < minColorDifference) {
                throw new RuntimeException("The expected background color for " +
                        "tooltip not found");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }
}
