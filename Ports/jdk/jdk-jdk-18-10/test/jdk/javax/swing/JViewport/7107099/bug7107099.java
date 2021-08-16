/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
   @test
  @key headful
   @bug 7107099
   @summary JScrollBar does not show up even if there are enough lebgth of textstring in textField
   @author Pavel Porvatov
*/

import javax.swing.*;
import java.awt.*;

public class bug7107099 {
    private static JFrame frame;
    private static JTextArea textarea;
    private static JScrollPane scrollPane;

    private static int value;
    private static int min;
    private static int max;
    private static int extent;

    public static void main(String[] args) throws Exception {

        java.awt.Robot robot = new java.awt.Robot();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                textarea = new JTextArea("before###1###\nbefore###2###\nbefore###3###\nbefore###4###\nbefore###5###\n");

                scrollPane = new JScrollPane(textarea);
                scrollPane.setPreferredSize(new Dimension(100, 50));

                frame = new JFrame();
                frame.setLayout(new BorderLayout());
                frame.setSize(200, 200);
                frame.add(scrollPane, BorderLayout.SOUTH);
                frame.setVisible(true);
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();

                value = model.getValue();
                min = model.getMinimum();
                max = model.getMaximum();
                extent = model.getExtent();

                // Do tricky manipulation for testing purpose
                textarea.setText(null);
                scrollPane.setViewportView(textarea);
                textarea.setText("after###1###\nafter###1###\nafter###1###\nafter###1###\nafter###1###\n");
                textarea.setCaretPosition(0);
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();

                if (value != model.getValue() ||
                        min != model.getMinimum() ||
                        max != model.getMaximum() ||
                        extent != model.getExtent()) {
                    throw new RuntimeException("Test bug7107099 failed");
                }

                System.out.println("Test bug7107099 passed");

                frame.dispose();
            }
        });
    }
}
