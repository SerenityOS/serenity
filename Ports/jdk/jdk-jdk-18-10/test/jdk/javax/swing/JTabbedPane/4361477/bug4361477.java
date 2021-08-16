/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/*
 * @test
 * @key headful
 * @bug 4361477
 * @summary JTabbedPane throws ArrayOutOfBoundsException
 * @author Oleg Mokhovikov
 * @run main bug4361477
 */
public class bug4361477 {

    static JTabbedPane tabbedPane;
    volatile static boolean bStateChanged = false;
    volatile static Rectangle bounds;
    static JFrame frame;

    public static void main(String args[]) throws Exception {
        try {

            Robot robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    createAndShowUI();
                }
            });

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    bounds = tabbedPane.getUI().getTabBounds(tabbedPane, 0);
                }
            });

            Point location = bounds.getLocation();
            SwingUtilities.convertPointToScreen(location, tabbedPane);
            robot.mouseMove(location.x + 1, location.y + 1);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            if (!bStateChanged) {
                throw new RuntimeException("Tabbed pane state is not changed");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static void createAndShowUI() {

        frame = new JFrame();
        tabbedPane = new JTabbedPane();
        tabbedPane.add("Tab0", new JPanel());
        tabbedPane.add("Tab1", new JPanel());
        tabbedPane.add("Tab2", new JPanel());
        tabbedPane.setSelectedIndex(2);
        tabbedPane.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent pick) {
                bStateChanged = true;
                if (tabbedPane.getTabCount() == 3) {
                    tabbedPane.remove(2);
                }
            }
        });

        frame.getContentPane().add(tabbedPane);
        frame.setSize(300, 200);
        frame.setVisible(true);
    }
}
