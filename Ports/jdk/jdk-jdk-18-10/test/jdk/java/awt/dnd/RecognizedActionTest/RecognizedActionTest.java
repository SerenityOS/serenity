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

/**
 * @test
 * @key headful
 * @bug 4494085 8158366
 * @summary verifies that the recognized action matches modifiers state
 * @compile RecognizedActionTest.java
 * @run main RecognizedActionTest
 */

import java.awt.Frame;
import java.awt.Component;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureEvent;

public class RecognizedActionTest implements AWTEventListener {

    final Frame frame = new Frame();
    boolean dragGestureRecognized = false;
    int currentDragAction = DnDConstants.ACTION_NONE;

    final int[] modifiers = {
            0,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK
    };

    final DragSource dragSource = DragSource.getDefaultDragSource();
    final DragGestureListener dragGestureListener = new DragGestureListener() {

        public void dragGestureRecognized(DragGestureEvent dge) {
            dragGestureRecognized = true;

            if (dge.getDragAction() != currentDragAction) {
                throw new RuntimeException("Expected: " +
                        Integer.toHexString(currentDragAction) +
                        " recognized: " +
                        Integer.toHexString(dge.getDragAction()));
            }
        }
    };

    final Object SYNC_LOCK = new Object();
    final int FRAME_ACTIVATION_TIMEOUT = 2000;
    final int MOUSE_RELEASE_TIMEOUT = 1000;

    Component clickedComponent = null;

    public void init() {
        try {
            frame.setTitle("Test frame");
            frame.setBounds(100, 100, 200, 200);
            dragSource.createDefaultDragGestureRecognizer(frame,
                    DnDConstants.ACTION_COPY |
                            DnDConstants.ACTION_MOVE |
                            DnDConstants.ACTION_LINK,
                    dragGestureListener);

            frame.getToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
            frame.setVisible(true);
            Thread.sleep(100);

            final Robot robot = new Robot();
            robot.waitForIdle();

            Thread.sleep(FRAME_ACTIVATION_TIMEOUT);

            final Point srcPoint = frame.getLocationOnScreen();
            Dimension d = frame.getSize();
            srcPoint.translate(d.width / 2, d.height / 2);

            if (!pointInComponent(robot, srcPoint, frame)) {
                throw new RuntimeException("WARNING: Couldn't locate source frame.");
            }

            final Point dstPoint = new Point(srcPoint);
            dstPoint.translate(d.width / 4, d.height / 4);

            if (!pointInComponent(robot, dstPoint, frame)) {
                throw new RuntimeException("WARNING: Couldn't locate target frame.");
            }

            for (int i = 0; i < modifiers.length; i++) {
                currentDragAction = convertModifiersToDropAction(modifiers[i]);
                dragGestureRecognized = false;
                final Point curPoint = new Point(srcPoint);
                robot.mouseMove(curPoint.x, curPoint.y);

                switch (modifiers[i]) {
                    case InputEvent.SHIFT_DOWN_MASK:
                        robot.keyPress(KeyEvent.VK_SHIFT);
                        robot.waitForIdle();
                        break;

                    case InputEvent.CTRL_DOWN_MASK:
                        robot.keyPress(KeyEvent.VK_CONTROL);
                        robot.waitForIdle();
                        break;

                    case InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK:
                        robot.keyPress(KeyEvent.VK_CONTROL);
                        robot.waitForIdle();
                        robot.keyPress(KeyEvent.VK_SHIFT);
                        robot.waitForIdle();
                        break;

                    default:
                        break;
                }
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                Thread.sleep(100);

                for (; !curPoint.equals(dstPoint) && !dragGestureRecognized;
                     curPoint.translate(sign(dstPoint.x - curPoint.x),
                             sign(dstPoint.y - curPoint.y))) {
                    robot.mouseMove(curPoint.x, curPoint.y);
                    Thread.sleep(50);
                }
                Thread.sleep(100);

                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

                switch (modifiers[i]) {
                    case InputEvent.SHIFT_DOWN_MASK:
                        robot.keyRelease(KeyEvent.VK_SHIFT);
                        robot.waitForIdle();
                        break;

                    case InputEvent.CTRL_DOWN_MASK:
                        robot.keyRelease(KeyEvent.VK_CONTROL);
                        robot.waitForIdle();
                        break;

                    case InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK:
                        robot.keyRelease(KeyEvent.VK_CONTROL);
                        robot.waitForIdle();
                        robot.keyRelease(KeyEvent.VK_SHIFT);
                        robot.waitForIdle();
                        break;

                    default:
                        break;
                }
                Thread.sleep(100);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("The test failed.");
        }
    }

    public int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

    public void reset() {
        clickedComponent = null;
    }

    public void eventDispatched(AWTEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            clickedComponent = (Component) e.getSource();
            synchronized (SYNC_LOCK) {
                SYNC_LOCK.notifyAll();
            }
        }
    }

    public boolean pointInComponent(Robot robot, Point p, Component comp)
            throws InterruptedException {
        reset();
        robot.mouseMove(p.x, p.y);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        synchronized (SYNC_LOCK) {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            SYNC_LOCK.wait(MOUSE_RELEASE_TIMEOUT);
        }

        Component c = clickedComponent;

        while (c != null && c != comp) {
            c = c.getParent();
        }

        return c == comp;
    }

    public void dispose() {
        frame.dispose();
    }

    public int convertModifiersToDropAction(int modifiers) {
        int dropAction = DnDConstants.ACTION_NONE;

        switch (modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) {
            case InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK:
                dropAction = DnDConstants.ACTION_LINK;
                break;

            case InputEvent.CTRL_DOWN_MASK:
                dropAction = DnDConstants.ACTION_COPY;
                break;

            case InputEvent.SHIFT_DOWN_MASK:
                dropAction = DnDConstants.ACTION_MOVE;
                break;

            default:
                dropAction = DnDConstants.ACTION_MOVE;
                break;
        }

        return dropAction;
    }

    public static void main(String args[]) {
        RecognizedActionTest actionTest = new RecognizedActionTest();
        actionTest.init();
        actionTest.dispose();
    }
}
