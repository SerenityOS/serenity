/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

/**
 * @test
 * @key headful
 * @bug 8144166
 * @requires (os.family == "mac")
 */

public final class DockIconRepaint {

    private static volatile Color color;

    private static JFrame frame;

    private static JInternalFrame jif;

    private static Robot robot;

    private static Point iconLoc;

    private static Rectangle iconBounds;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        EventQueue.invokeAndWait(DockIconRepaint::createUI);
        try {
            robot.waitForIdle();
            color = Color.BLUE;
            test();
            color = Color.RED;
            test();
            color = Color.GREEN;
            test();
        } finally {
            frame.dispose();
        }
    }

    private static void test() throws Exception {
        // maximize the frame to force repaint
        EventQueue.invokeAndWait(() -> {
            try {
                jif.setIcon(false);
                jif.setMaximum(true);
            } catch (PropertyVetoException e) {
                throw new RuntimeException(e);
            }
        });
        robot.waitForIdle();
        Thread.sleep(1000);
        // minimize the frame to dock, the icon should be up2date
        EventQueue.invokeAndWait(() -> {
            try {
                jif.setIcon(true);
            } catch (PropertyVetoException e) {
                throw new RuntimeException(e);
            }
            iconLoc = jif.getDesktopIcon().getLocationOnScreen();
            iconBounds = jif.getDesktopIcon().getBounds();
        });
        robot.waitForIdle();
        Thread.sleep(1000);

        final Color c = robot.getPixelColor(iconLoc.x + iconBounds.width / 2,
                                            iconLoc.y + iconBounds.height / 2);
        if (c.getRGB() != color.getRGB()) {
            System.err.println("Exp: " + Integer.toHexString(color.getRGB()));
            System.err.println("Actual: " + Integer.toHexString(c.getRGB()));
            throw new RuntimeException("Wrong color.");
        }
    }

    private static void createUI() {
        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setSize(300, 300);
        frame.setLocationRelativeTo(null);
        final JDesktopPane pane = new JDesktopPane();
        final JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        jif = new JInternalFrame();
        jif.add(panel);
        jif.setVisible(true);
        jif.setSize(300, 300);
        pane.add(jif);
        frame.add(pane);
        frame.setVisible(true);
    }
}
