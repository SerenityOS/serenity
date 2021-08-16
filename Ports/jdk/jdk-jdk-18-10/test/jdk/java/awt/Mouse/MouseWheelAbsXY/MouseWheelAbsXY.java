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

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;

import static java.awt.GraphicsEnvironment.*;

/**
 * @test
 * @key headful
 * @bug 6778087
 */
public final class MouseWheelAbsXY {

    private static boolean done;
    private static int wheelX;
    private static int wheelY;
    private static int mouseX;
    private static int mouseY;

    public static void main(final String[] args) throws AWTException {
        GraphicsEnvironment ge = getLocalGraphicsEnvironment();
        GraphicsDevice[] sds = ge.getScreenDevices();
        for (GraphicsDevice gd : sds) {
            test(gd.getDefaultConfiguration());
        }
    }

    private static void test(GraphicsConfiguration gc) throws AWTException {
        final Window frame = new Frame(gc);
        try {
            frame.addMouseWheelListener(e -> {
                wheelX = e.getXOnScreen();
                wheelY = e.getYOnScreen();
                done = true;
            });
            frame.setSize(300, 300);
            frame.setVisible(true);

            final Robot robot = new Robot();
            robot.setAutoDelay(50);
            robot.setAutoWaitForIdle(true);
            mouseX = frame.getX() + frame.getWidth() / 2;
            mouseY = frame.getY() + frame.getHeight() / 2;

            robot.mouseMove(mouseX, mouseY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseWheel(10);

            validate();
        } finally {
            frame.dispose();
        }
    }

    private static void validate() {
        if (!done || wheelX != mouseX || wheelY != mouseY) {
            System.err.println("Expected X: " + mouseX);
            System.err.println("Expected Y: " + mouseY);
            System.err.println("Actual X: " + wheelX);
            System.err.println("Actual Y: " + wheelY);
            throw new RuntimeException("Test failed");
        }
    }
}
