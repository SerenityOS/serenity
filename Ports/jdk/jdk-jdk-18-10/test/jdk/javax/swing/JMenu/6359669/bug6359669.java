/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6359669
 * @summary REGRESSION: Submenu does not work if populated in PopupMenuListener.popupMenuWillBecomeVisible
 * @author Alexander Potochkin
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug6359669
 */

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.InputEvent;

public class bug6359669 {
    static JMenu menu;
    static JFrame f;

    public static void main(String[] args) throws Exception {
        try {
            ExtendedRobot robot = new ExtendedRobot();
            robot.setAutoDelay(10);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    f = new JFrame();
                    JMenuBar menuBar = new JMenuBar();
                    menu = new JMenu("Test");
                    menu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
                        public void popupMenuCanceled(PopupMenuEvent e) {
                        }

                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        }

                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            menu.add(new JMenuItem("An item"));
                        }
                    });

                    menuBar.add(menu);
                    f.setJMenuBar(menuBar);

                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    f.setSize(200, 200);
                    f.setVisible(true);
                }
            });
            robot.waitForIdle();
            Point p = menu.getLocationOnScreen();
            Dimension size = menu.getSize();
            p.x += size.width / 2;
            p.y += size.height / 2;
            robot.mouseMove(p.x, p.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            if (menu.getPopupMenu().getComponentCount() == 0) {
                throw new RuntimeException("Where is a menuItem ?");
            }
        } finally {
            if (f != null) SwingUtilities.invokeAndWait(() -> f.dispose());
        }
    }
}
