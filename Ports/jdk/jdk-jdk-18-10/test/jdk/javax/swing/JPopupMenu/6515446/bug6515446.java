/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6515446
 * @summary JMenuItems in JPopupMenus not receiving ActionEvents - incompat with 1.5
 * @author Alexander Potochkin
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug6515446
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class bug6515446 {
    private static JPanel panel;
    private static volatile boolean flag;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    final JPopupMenu popup = new JPopupMenu("Menu");
                    JMenuItem item = new JMenuItem("MenuItem");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            flag = true;
                        }
                    });
                    popup.add(item);

                    panel = new JPanel();
                    panel.addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            popup.show(panel, e.getX(), e.getY());
                        }

                        public void mouseReleased(MouseEvent e) {
                            popup.setVisible(false);
                        }
                    });
                    frame.add(panel);
                    frame.setSize(200, 200);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });

            ExtendedRobot robot = new ExtendedRobot();
            robot.setAutoDelay(10);
            robot.waitForIdle();

            Point l = panel.getLocationOnScreen();

            int x = l.x + panel.getWidth() / 2;
            int y = l.y + panel.getHeight() / 2;
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.glide(x, y, x + 10, y + 10);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();

            if (!flag) {
                throw new RuntimeException("ActionEvent wasn't fired");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
