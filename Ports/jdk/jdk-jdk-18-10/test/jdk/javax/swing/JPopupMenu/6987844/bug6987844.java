/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6987844
 * @summary Incorrect width of JComboBox drop down
 * @author Alexander Potochkin
 * @run main bug6987844
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class bug6987844 {
    static JMenu menu1;
    static JMenu menu2;
    static JFrame frame;

    public static void main(String... args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(200);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    JMenuBar bar = new JMenuBar();
                    menu1 = new JMenu("Menu1");
                    menu1.add(new JMenuItem("item"));
                    bar.add(menu1);
                    menu2 = new JMenu("Menu2");
                    menu2.add(new JMenuItem("item"));
                    menu2.add(new JMenuItem("item"));
                    bar.add(menu2);

                    frame.setJMenuBar(bar);
                    frame.pack();

                    frame.setVisible(true);
                }
            });
            robot.waitForIdle();
            Point point1 = menu1.getLocationOnScreen();
            Point point2 = menu2.getLocationOnScreen();

            robot.mouseMove(point1.x + 1, point1.y + 1);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            robot.mouseMove(point2.x + 1, point2.y + 1);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            robot.mouseMove(point1.x + 1, point1.y + 1);
            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Dimension popupSize1 = menu1.getPopupMenu().getSize();
                    Dimension popupSize2 = menu2.getPopupMenu().getSize();
                    if (popupSize1.equals(popupSize2)) {
                        throw new RuntimeException("First popup unexpedetly changed its size");
                    }
                }
            });
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
