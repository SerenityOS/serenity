/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7146377
 * @summary closed/javax/swing/DataTransfer/4876520/bug4876520.java failed since b08 in jdk 8
 * @author Pavel Porvatov
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class bug7146377 {
    private static JLabel label;
    private static JFrame frame;

    private static volatile Point point;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();

                label = new JLabel("A label");

                label.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        checkEvent(e);
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        checkEvent(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        checkEvent(e);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        checkEvent(e);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        checkEvent(e);
                    }
                });

                frame.add(label);
                frame.setSize(200, 100);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });

        Robot robot = new Robot();

        robot.waitForIdle();

        // On Linux platforms realSync doesn't guaranties setSize completion
        Thread.sleep(1000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                point = label.getLocationOnScreen();
            }
        });


        robot.setAutoDelay(200);

        // Move mouse
        for (int i = 0; i < 20; i++) {
            robot.mouseMove(point.x + i, point.y + i);
        }

        for (int button : new int[]{InputEvent.BUTTON1_MASK, InputEvent.BUTTON2_MASK, InputEvent.BUTTON3_MASK}) {
            robot.mouseMove(point.x, point.y);

            // Mouse Drag
            robot.mousePress(button);

            for (int i = 0; i < 20; i++) {
                robot.mouseMove(point.x + i, point.y + i);
            }

            robot.mouseRelease(button);
        }

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });

        System.out.println("Test passed");
    }

    private static void checkEvent(MouseEvent e) {
        String eventAsStr = eventToString(e);

        System.out.println("Checking event " + eventAsStr);

        check("isLeftMouseButton", SwingUtilities.isLeftMouseButton(e), oldIsLeftMouseButton(e), eventAsStr);
        check("isRightMouseButton", SwingUtilities.isRightMouseButton(e), oldIsRightMouseButton(e), eventAsStr);
        check("isMiddleMouseButton", SwingUtilities.isMiddleMouseButton(e), oldIsMiddleMouseButton(e), eventAsStr);
    }

    private static void check(String methodName, boolean newValue, boolean oldValue, String eventAsStr) {
        if (newValue != oldValue) {
            throw new RuntimeException("Regression on " + methodName + ", newValue = " + newValue +
                    ", oldValue = " + oldValue + ", e = " + eventAsStr);
        }
    }

    private static String eventToString(MouseEvent e) {
        StringBuilder result = new StringBuilder();

        switch (e.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                result.append("MOUSE_PRESSED");
                break;
            case MouseEvent.MOUSE_RELEASED:
                result.append("MOUSE_RELEASED");
                break;
            case MouseEvent.MOUSE_CLICKED:
                result.append("MOUSE_CLICKED");
                break;
            case MouseEvent.MOUSE_ENTERED:
                result.append("MOUSE_ENTERED");
                break;
            case MouseEvent.MOUSE_EXITED:
                result.append("MOUSE_EXITED");
                break;
            case MouseEvent.MOUSE_MOVED:
                result.append("MOUSE_MOVED");
                break;
            case MouseEvent.MOUSE_DRAGGED:
                result.append("MOUSE_DRAGGED");
                break;
            case MouseEvent.MOUSE_WHEEL:
                result.append("MOUSE_WHEEL");
                break;
            default:
                result.append("unknown type");
        }

        result.append(", modifiers = " + MouseEvent.getMouseModifiersText(e.getModifiers()));
        result.append(", modifiersEx = " + MouseEvent.getMouseModifiersText(e.getModifiersEx()));
        result.append(", button = " + e.getButton());

        return result.toString();
    }

    // Original implementation of SwingUtilities.isLeftMouseButton
    private static boolean oldIsLeftMouseButton(MouseEvent e) {
        return ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0);
    }

    // Original implementation of SwingUtilities.isMiddleMouseButton
    private static boolean oldIsMiddleMouseButton(MouseEvent e) {
        return ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK);
    }

    // Original implementation of SwingUtilities.isRightMouseButton
    private static boolean oldIsRightMouseButton(MouseEvent e) {
        return ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK);
    }
}
