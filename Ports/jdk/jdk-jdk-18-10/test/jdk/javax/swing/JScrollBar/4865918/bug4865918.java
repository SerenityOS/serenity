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
 * @bug 4865918
 * @summary REGRESSION:JCK1.4a-runtime api/javax_swing/interactive/JScrollBarTests.html#JScrollBar
 * @author Andrey Pikalev
 * @run main bug4865918
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class bug4865918 {

    private static TestScrollBar sbar;
    private static JFrame frame;

    public static void main(String[] argv) throws Exception {
        try {
            Robot robot = new Robot();
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    sbar.pressMouse();
                }
            });

            robot.waitForIdle();

            int value = getValue();

            if (value != 9) {
                throw new Error("The scrollbar block increment is incorect");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static int getValue() throws Exception {
        final int[] result = new int[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                result[0] = sbar.getValue();
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        frame = new JFrame("bug4865918");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sbar = new TestScrollBar(JScrollBar.HORIZONTAL, -1, 10, -100, 100);
        sbar.setPreferredSize(new Dimension(200, 20));
        sbar.setBlockIncrement(10);

        frame.getContentPane().add(sbar);
        frame.pack();
        frame.setVisible(true);

    }

    static class TestScrollBar extends JScrollBar {

        public TestScrollBar(int orientation, int value, int extent,
                int min, int max) {
            super(orientation, value, extent, min, max);

        }

        public void pressMouse() {
            MouseEvent me = new MouseEvent(sbar,
                    MouseEvent.MOUSE_PRESSED,
                    (new Date()).getTime(),
                    MouseEvent.BUTTON1_MASK,
                    3 * getWidth() / 4, getHeight() / 2,
                    1, true);
            processMouseEvent(me);
        }
    }
}
