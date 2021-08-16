/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @test
 * @key headful
 * @bug 8176009
 */
public class MultiScreenRobotPosition {

    private static volatile boolean fail = true;

    public static void main(String[] args) throws Exception {
        GraphicsDevice[] sds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                  .getScreenDevices();
        for (final GraphicsDevice gd : sds) {
            fail = true;
            Robot robot = new Robot(gd);
            robot.setAutoDelay(100);
            robot.setAutoWaitForIdle(true);

            Frame frame = new Frame(gd.getDefaultConfiguration());
            frame.setUndecorated(true);
            frame.setSize(400, 400);
            frame.setVisible(true);
            robot.waitForIdle();

            frame.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.println("e = " + e);
                    fail = false;
                }
            });

            Rectangle bounds = frame.getBounds();
            robot.mouseMove(bounds.x + bounds.width / 2,
                            bounds.y + bounds.height / 2);
            robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
            frame.dispose();
            if (fail) {
                System.err.println("Frame bounds = " + bounds);
                throw new RuntimeException("Click in the wrong location");
            }
        }
    }
}
