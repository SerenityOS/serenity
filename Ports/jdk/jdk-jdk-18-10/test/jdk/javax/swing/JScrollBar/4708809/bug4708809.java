/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4708809
 * @summary JScrollBar functionality slightly different from native scrollbar
 * @author Andrey Pikalev
 * @run main bug4708809
 */
import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;

public class bug4708809 {

    private static volatile boolean do_test = false;
    private static volatile boolean passed = true;
    private static JScrollPane spane;
    private static JScrollBar sbar;
    private static JFrame fr;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(350);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    spane.requestFocus();
                    sbar.setValue(sbar.getMaximum());
                }
            });

            robot.waitForIdle();

            Point point = getClickPoint(0.5, 0.5);
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    final int oldValue = sbar.getValue();
                    sbar.addAdjustmentListener(new AdjustmentListener() {

                        public void adjustmentValueChanged(AdjustmentEvent e) {
                            if (e.getValue() >= oldValue) {
                                passed = false;
                            }
                            do_test = true;
                        }
                    });

                }
            });

            robot.waitForIdle();

            point = getClickPoint(0.5, 0.2);
            robot.mouseMove(point.x, point.y);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            if (!do_test || !passed) {
                throw new Exception("The scrollbar moved with incorrect direction");
            }
        } finally {
            if (fr != null) SwingUtilities.invokeAndWait(() -> fr.dispose());
        }

    }

    private static Point getClickPoint(final double scaleX, final double scaleY) throws Exception {
        final Point[] result = new Point[1];

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                Point p = sbar.getLocationOnScreen();
                Rectangle rect = sbar.getBounds();
                result[0] = new Point((int) (p.x + scaleX * rect.width),
                        (int) (p.y + scaleY * rect.height));
            }
        });

        return result[0];

    }

    private static void createAndShowGUI() {
        fr = new JFrame("Test");

        JLabel label = new JLabel("picture");
        label.setPreferredSize(new Dimension(500, 500));
        spane = new JScrollPane(label);
        fr.getContentPane().add(spane);
        sbar = spane.getVerticalScrollBar();

        fr.setSize(200, 200);
        fr.setVisible(true);
    }
}
