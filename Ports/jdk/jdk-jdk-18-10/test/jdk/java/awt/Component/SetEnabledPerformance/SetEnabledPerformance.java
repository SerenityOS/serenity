/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Robot;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8071306
 * @author Sergey Bylokhov
 */
public final class SetEnabledPerformance {

    private static Frame frame;

    private static void createAndShowGUI() {
        frame = new Frame();
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 0));
        frame.setSize(600, 600);
        frame.setLocationRelativeTo(null);
        for (int i = 1; i < 10001; ++i) {
            frame.add(new JButton("Button " + i));
        }
        frame.setVisible(true);
    }

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> createAndShowGUI());
        final Robot robot = new Robot();
        robot.waitForIdle();
        robot.mouseMove(frame.getX() + 15, frame.getY() + 300);
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(() -> {
            long m = System.currentTimeMillis();
            for (final Component comp : frame.getComponents()) {
                comp.setEnabled(false);
            }
            m = System.currentTimeMillis() - m;
            System.err.println("Disabled in " + m + " ms");
            frame.dispose();
            // we should be much faster, but leaves 1000 for the slow systems
            if (m > 1000) {
                throw new RuntimeException("Too slow");
            }
        });
    }
}
