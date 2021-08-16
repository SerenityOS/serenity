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

import javax.swing.*;
import java.awt.*;
/*
 * @test
 * @key headful
 * @bug 6495408
 * @summary REGRESSION: JTabbedPane throws ArrayIndexOutOfBoundsException
 * @author Alexander Potochkin
 * @run main bug6495408
 */

public class bug6495408 {

    static JTabbedPane tabbedPane;
    static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            final Robot robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    tabbedPane = new JTabbedPane();
                    tabbedPane.setTabPlacement(JTabbedPane.LEFT);
                    tabbedPane.addTab("Hello", null);
                    frame.add(tabbedPane);
                    frame.setSize(400, 400);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });

            robot.waitForIdle();

            final Rectangle d = new Rectangle();
            final Point p = new Point();

            for (int i = 0; i < 7; i++) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        int tab = tabbedPane.getTabCount() - 1;
                        Rectangle bounds = tabbedPane.getBoundsAt(tab);
                        if (bounds != null) {
                            d.setBounds(bounds);
                            p.setLocation(d.x + d.width / 2, d.y + d.height / 2);
                            SwingUtilities.convertPointToScreen(p, tabbedPane);
                            robot.mouseMove(p.x, p.y + d.height);
                            tabbedPane.addTab("Hello", null);
                        }
                    }
                });
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
