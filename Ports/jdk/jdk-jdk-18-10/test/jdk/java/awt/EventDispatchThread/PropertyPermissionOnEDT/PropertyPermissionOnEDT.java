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

import java.awt.Point;
import java.awt.Robot;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8080405
 * @run main/othervm/policy=java.policy -Djava.security.manager PropertyPermissionOnEDT
 */
public final class PropertyPermissionOnEDT {

    public static void main(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(PropertyPermissionOnEDT::test);

        JFrame frame = new JFrame();
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                test();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                test();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                test();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                test();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                test();
            }
        });
        frame.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                test();
            }

            @Override
            public void focusLost(FocusEvent e) {
                test();
            }
        });
        frame.addMouseWheelListener(e -> test());
        frame.addWindowStateListener(e -> test());

        frame.setSize(100, 100);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(100);
        Point loc = frame.getLocationOnScreen();
        robot.mouseMove(loc.x + frame.getWidth() / 2,
                        loc.y + frame.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.mouseWheel(100);
        frame.dispose();
    }

    private static void test() {
        String property = System.getProperty("os.name");
        System.out.println("property = " + property);
    }
}
