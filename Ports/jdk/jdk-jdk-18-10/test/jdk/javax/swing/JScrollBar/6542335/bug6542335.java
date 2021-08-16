/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6542335
 * @summary different behavior on knob of scroll bar between 1.4.2 and 5.0
 * @author  Alexander Potochkin
 * @run main bug6542335
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.InputEvent;

public class bug6542335 {
    private static JScrollBar sb;
    private static MyScrollBarUI ui;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            final Robot robot = new Robot();
            robot.setAutoDelay(10);

            final Rectangle[] thumbBounds = new Rectangle[1];

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame("bug6542335");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    sb = new JScrollBar(0, 0, 1, 0, 1);

                    ui = new MyScrollBarUI();
                    sb.setUI(ui);

                    sb.setPreferredSize(new Dimension(200, 17));
                    DefaultBoundedRangeModel rangeModel = new DefaultBoundedRangeModel();
                    rangeModel.setMaximum(100);
                    rangeModel.setMinimum(0);
                    rangeModel.setExtent(50);
                    rangeModel.setValue(50);

                    sb.setModel(rangeModel);
                    frame.add(sb, BorderLayout.NORTH);

                    frame.setSize(200, 100);
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    thumbBounds[0] = new Rectangle(ui.getThumbBounds());

                    Point l = sb.getLocationOnScreen();

                    robot.mouseMove(l.x + (int) (0.75 * sb.getWidth()), l.y + sb.getHeight() / 2);
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Rectangle newThumbBounds = ui.getThumbBounds();

                    if (!thumbBounds[0].equals(newThumbBounds)) {
                        throw new RuntimeException("Test failed.\nOld bounds: " + thumbBounds[0] +
                        "\nNew bounds: " + newThumbBounds);
                    }
                }
            });
        } finally {
                if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static class MyScrollBarUI extends BasicScrollBarUI {
        public Rectangle getThumbBounds() {
            return super.getThumbBounds();
        }
    }
}
