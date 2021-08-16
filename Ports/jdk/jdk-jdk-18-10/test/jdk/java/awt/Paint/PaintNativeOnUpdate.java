/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;

/**
 * @test
 * @key headful
 * @bug 7157680
 * @library /lib/client
 * @build ExtendedRobot
 * @author Sergey Bylokhov
 * @run main PaintNativeOnUpdate
 */
public final class PaintNativeOnUpdate extends Label {

    private boolean fullUpdate = true;

    public static void main(final String[] args) throws AWTException {
        ExtendedRobot robot = new ExtendedRobot();
        robot.setAutoDelay(50);
        final Frame frame = new Frame();
        final Component label = new PaintNativeOnUpdate();
        frame.setBackground(Color.RED);
        frame.add(label);
        frame.setSize(300, 300);
        frame.setUndecorated(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        robot.waitForIdle(1000);
        label.repaint();// first paint
        robot.waitForIdle(1000);
        label.repaint();// incremental paint
        robot.waitForIdle(1000);

        Point point = label.getLocationOnScreen();
        Color color = robot.getPixelColor(point.x + label.getWidth() / 2,
                                          point.y + label.getHeight() / 2);
        if (!color.equals(Color.GREEN)) {
            System.err.println("Expected color = " + Color.GREEN);
            System.err.println("Actual color = " + color);
            throw new RuntimeException();
        }
        frame.dispose();
    }

    @Override
    public void update(final Graphics g) {
        if (fullUpdate) {
            //full paint
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, getWidth(), getHeight());
            fullUpdate = false;
        } else {
            // Do nothing
            // incremental paint
        }
    }

    @Override
    public void paint(final Graphics g) {
        // Do nothing
    }
}
