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
 * @bug 8214112
 * @summary Tests JSpinner selected Text background color
 * @run main TestSelectedTextBackgroundColor
 */

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;

public class TestSelectedTextBackgroundColor {
    private static JFrame frame;
    private static JSpinner listModelSpinner;
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

    private static JFormattedTextField getTextField(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor)editor).getTextField();
        } else {
            System.err.println("Unexpected editor type: "
                    + spinner.getEditor().getClass()
                    + " isn't a descendant of DefaultEditor");
            return null;
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
                    JPanel panel = new JPanel();
                    String list[] = {"            "};
                    SpinnerListModel model = new SpinnerListModel(list);
                    listModelSpinner = new JSpinner(model);
                    panel.add(listModelSpinner, BorderLayout.CENTER);
                    frame = new JFrame("TestSelectedTextBackgroundColor");
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

            blockTillDisplayed(listModelSpinner);
            SwingUtilities.invokeAndWait(() -> {
                point = listModelSpinner.getLocationOnScreen();
                rect = listModelSpinner.getBounds();
            });
            robot.waitForIdle();
            robot.delay(500);

            Color backgroundColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            JFormattedTextField formattedTextField = getTextField(listModelSpinner);
            formattedTextField.selectAll();

            robot.waitForIdle();
            robot.delay(500);

            Color highlightColor = robot
                    .getPixelColor(point.x+rect.width/2, point.y+rect.height/2);
            robot.waitForIdle();
            robot.delay(500);

            int actualColorDifference = getMaxColorDiff(backgroundColor, highlightColor);
            if (actualColorDifference < minColorDifference) {
                throw new RuntimeException("The expected background color for " +
                        "Selected Text was not found");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }
}
