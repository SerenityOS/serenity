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
 * @bug 6510999
 * @summary Selection in a JList with both scrollbars visible jumps on arrowkey-down
 * @author Alexander Potochkin
 * @run main bug6510999
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class bug6510999 {
    private static JScrollPane s;
    static JFrame frame;

    private static void createGui() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultListModel dlm = new DefaultListModel();
        for (int i = 0; i < 100; i++)
          dlm
            .addElement(i + " listItemlistItemlistItemlistItemItem");
        JList l = new JList();
        l.setModel(dlm);
        s = new JScrollPane(l);
        l.setSelectedIndex(50);
        l.ensureIndexIsVisible(50);

        frame.add(s);
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(10);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    bug6510999.createGui();
                }
            });
            robot.waitForIdle();
            Point viewPosition = s.getViewport().getViewPosition();
            robot.keyPress(KeyEvent.VK_DOWN);
            robot.keyRelease(KeyEvent.VK_DOWN);
            robot.waitForIdle();
            if (!s.getViewport().getViewPosition().equals(viewPosition)) {
                throw new RuntimeException("JScrollPane was unexpectedly scrolled");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
