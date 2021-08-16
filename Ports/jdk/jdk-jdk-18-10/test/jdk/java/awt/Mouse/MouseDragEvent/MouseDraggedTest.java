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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
/*
 * @test
 * @key headful
 * @bug 8080137
 * @summary Dragged events for extra mouse buttons (4,5,6) are not generated
 *            on JSplitPane
 * @author alexandr.scherbatiy area=awt.event
 * @run main MouseDraggedTest
 */
public class MouseDraggedTest {

    private static JFrame frame;
    private static Rectangle frameBounds;
    private static volatile boolean mouseDragged;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(MouseDraggedTest::createAndShowGUI);
            robot.waitForIdle();

            SwingUtilities.invokeAndWait(() -> frameBounds = frame.getBounds());
            robot.waitForIdle();

            for (int i = 1; i <= MouseInfo.getNumberOfButtons(); i++) {
                testMouseDrag(i, robot);
            }
        } finally {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    private static void testMouseDrag(int button, Robot robot) {

        mouseDragged = false;
        int x1 = frameBounds.x + frameBounds.width / 4;
        int y1 = frameBounds.y + frameBounds.height / 4;
        int x2 = frameBounds.x + frameBounds.width / 2;
        int y2 = frameBounds.y + frameBounds.height / 2;

        robot.mouseMove(x1, y1);
        robot.waitForIdle();

        int buttonMask = InputEvent.getMaskForButton(button);
        robot.mousePress(buttonMask);
        robot.mouseMove(x2, y2);
        robot.mouseRelease(buttonMask);
        robot.waitForIdle();

        if (!mouseDragged) {
            throw new RuntimeException("Mouse button " + button
                    + " is not dragged");
        }
    }

    static void createAndShowGUI() {

        frame = new JFrame();
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseDragged = true;
            }
        });
        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
