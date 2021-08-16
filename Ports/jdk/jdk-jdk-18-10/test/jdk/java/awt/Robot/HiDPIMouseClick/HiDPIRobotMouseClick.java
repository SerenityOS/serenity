/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.UIManager;

/**
 * @test
 * @key headful
 * @bug 8073320
 * @summary  Windows HiDPI support
 * @author Alexander Scherbatiy
 * @requires (os.family == "windows")
 * @run main/othervm -Dsun.java2d.win.uiScale=2 HiDPIRobotMouseClick
 */

public class HiDPIRobotMouseClick {

    private static volatile int mouseX;
    private static volatile int mouseY;

    public static void main(String[] args) throws Exception {

        try {
            UIManager.setLookAndFeel(
                    "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            return;
        }

        Frame frame = new Frame();
        frame.setBounds(30, 20, 400, 300);
        frame.setUndecorated(true);

        frame.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                mouseX = e.getXOnScreen();
                mouseY = e.getYOnScreen();
            }
        });

        frame.setVisible(true);

        Robot robot = new Robot();
        robot.waitForIdle();
        Thread.sleep(200);

        Rectangle rect = frame.getBounds();
        rect.setLocation(frame.getLocationOnScreen());

        int x = (int) rect.getCenterX();
        int y = (int) rect.getCenterY();

        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        if (x != mouseX || y != mouseY) {
            throw new RuntimeException("Wrong mouse click point!");
        }
    }
}
