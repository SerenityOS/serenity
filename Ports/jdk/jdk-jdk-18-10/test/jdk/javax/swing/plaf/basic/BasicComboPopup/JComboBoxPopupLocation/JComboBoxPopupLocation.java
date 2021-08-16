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

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * @test
 * @key headful
 * @bug 8176448
 * @run main/timeout=600 JComboBoxPopupLocation
 */
public final class JComboBoxPopupLocation {

    private static final int SIZE = 300;
    public static final String PROPERTY_NAME = "JComboBox.isPopDown";
    private static volatile Robot robot;
    private static volatile JComboBox<String> comboBox;
    private static volatile JFrame frame;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        robot.setAutoWaitForIdle(true);
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] sds = ge.getScreenDevices();
        UIManager.LookAndFeelInfo[] lookAndFeelArray =
                UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo lookAndFeelItem : lookAndFeelArray) {
            System.setProperty(PROPERTY_NAME, "true");
            step(sds, lookAndFeelItem);
            if (lookAndFeelItem.getClassName().contains("Aqua")) {
                System.setProperty(PROPERTY_NAME, "false");
                step(sds, lookAndFeelItem);
            }
        }
    }

    private static void step(GraphicsDevice[] sds,
                             UIManager.LookAndFeelInfo lookAndFeelItem)
            throws Exception {
        UIManager.setLookAndFeel(lookAndFeelItem.getClassName());
        Point left = null;
        for (final GraphicsDevice sd : sds) {
            GraphicsConfiguration gc = sd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            if (left == null || left.x > bounds.x) {
                left = new Point(bounds.x, bounds.y + bounds.height / 2);
            }
            Point point = new Point(bounds.x, bounds.y);
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            while (point.y < bounds.y + bounds.height - insets.bottom - SIZE ) {
                while (point.x < bounds.x + bounds.width - insets.right - SIZE) {
                    try {
                        EventQueue.invokeAndWait(() -> {
                            setup(point);
                        });
                        robot.waitForIdle();
                        test(comboBox);
                        robot.waitForIdle();
                        validate(comboBox);
                        robot.waitForIdle();
                        point.translate(bounds.width / 5, 0);
                    } finally {
                        dispose();
                    }
                }
                point.setLocation(bounds.x, point.y + bounds.height / 5);
            }
        }
        if (left != null) {
            final Point finalLeft = left;
            finalLeft.translate(-50, 0);
            try {
                EventQueue.invokeAndWait(() -> {
                    setup(finalLeft);
                });
                robot.waitForIdle();
                test(comboBox);
                robot.waitForIdle();
                validate(comboBox);
            } finally {
                dispose();
            }
        }
    }

    private static void dispose() throws Exception {
        EventQueue.invokeAndWait(() -> {
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    private static void setup(final Point tmp) {
        comboBox = new JComboBox<>();
        for (int i = 1; i < 7; i++) {
            comboBox.addItem("Long-long-long-long-long text in the item-" + i);
        }
        String property = System.getProperty(PROPERTY_NAME);
        comboBox.putClientProperty(PROPERTY_NAME, Boolean.valueOf(property));
        frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setLayout(new FlowLayout());
        frame.add(comboBox);
        frame.pack();
        frame.setSize(frame.getWidth(), SIZE);
        frame.setVisible(true);
        frame.setLocation(tmp.x, tmp.y);
    }

    private static void test(final JComboBox comboBox) throws Exception {
        Point pt = comboBox.getLocationOnScreen();
        robot.mouseMove(pt.x + comboBox.getWidth() / 2,
                        pt.y + comboBox.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        int x = pt.x + comboBox.getWidth() / 2;
        int y = pt.y + comboBox.getHeight() / 2 + 70;
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void validate(final JComboBox comboBox) throws Exception {
        EventQueue.invokeAndWait(() -> {
            if (comboBox.getSelectedIndex() == 0) {
                throw new RuntimeException();
            }
        });
    }
}
