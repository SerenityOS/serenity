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

/*
 * @test
 * @key headful
 * @bug 8016551
 * @summary JMenuItem in WindowsLookAndFeel can't paint default icons
 * @author Leonid Romanov
 * @run main/othervm bug8016551
 */

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Robot;

public class bug8016551 {
    private static volatile RuntimeException exception = null;

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            // We intentionally allow the test to run with other l&f
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Icon icon = UIManager.getIcon("InternalFrame.closeIcon");
                    if (icon == null) {
                        return;
                    }

                    JMenuItem item = new TestMenuItem(icon);
                    JFrame f = new JFrame();
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    f.getContentPane().add(item);
                    f.pack();
                    f.setVisible(true);
                } catch (ClassCastException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Robot robot = new Robot();
        robot.waitForIdle();

        if (exception != null) {
            throw exception;
        }
    }

    static class TestMenuItem extends JMenuItem {
        TestMenuItem(Icon icon) {
            super(icon);
        }

        @Override
        public void paint(Graphics g) {
            try {
                super.paint(g);
            } catch (RuntimeException e) {
                exception = e;
            }
        }
    }
}

