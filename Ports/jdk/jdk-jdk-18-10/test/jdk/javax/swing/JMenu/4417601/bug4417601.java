/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4417601
 * @summary JMenus with no items paint a tiny menu.
 * @author Alexander Potochkin
 * @library /lib/client
 * @build ExtendedRobot
 * @run main bug4417601
 */

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class bug4417601 {
    static JMenu menu;
    static volatile boolean flag;
    static JFrame frame;

    public static void main(String[] args) throws Exception {

        ExtendedRobot robot = new ExtendedRobot();
        robot.setAutoDelay(10);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                menu = new JMenu("Menu");
                JMenuBar bar = new JMenuBar();
                bar.add(menu);
                frame.setJMenuBar(bar);

                frame.getLayeredPane().addContainerListener(new ContainerAdapter() {
                    public void componentAdded(ContainerEvent e) {
                        flag = true;
                    }
                });

                frame.pack();
                frame.setSize(200, 200);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
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
        if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        if (flag) {
            throw new RuntimeException("Empty popup was shown");
        }
    }
}
