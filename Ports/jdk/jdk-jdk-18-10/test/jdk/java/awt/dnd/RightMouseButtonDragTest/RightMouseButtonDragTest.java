/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4494085 8079258 8234802
 * @summary Verifies that the right mouse button drag gesture is recognized
 *          on Win32 and Mac OS X
 * @run main RightMouseButtonDragTest
 */

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class RightMouseButtonDragTest implements AWTEventListener {

    private static Frame frame;
    boolean dragRecognized = false;

    final DragSource dragSource = DragSource.getDefaultDragSource();
    final Transferable transferable = new StringSelection("TEXT");
    final DragGestureListener dragGestureListener = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent dge) {
                dragRecognized = true;
            }
        };

    static final Object SYNC_LOCK = new Object();
    static final int FRAME_ACTIVATION_TIMEOUT = 2000;
    static final int DROP_COMPLETION_TIMEOUT = 5000;
    static final int MOUSE_RELEASE_TIMEOUT = 1000;

    final String os = System.getProperty("os.name");
    final boolean isWin = os.startsWith("Win");
    final boolean isMac = os.startsWith("Mac");

    Component clickedComponent = null;

    public static void main(String[] args) throws Exception {
        try {
            RightMouseButtonDragTest app = new RightMouseButtonDragTest();
            SwingUtilities.invokeAndWait(() -> app.init());
            app.start();
        } finally {
            if(frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    public void init() {
        //Create instructions for the user here, as well as set up
        // the environment -- set the layout manager, add buttons,
        // etc.
        frame = new JFrame();
        frame.setTitle("Test frame");
        frame.setBounds(100, 100, 200, 200);
        dragSource.createDefaultDragGestureRecognizer(frame, DnDConstants.ACTION_COPY_OR_MOVE,
                                                      dragGestureListener);


        frame.getToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
        frame.setVisible(true);

    }

    public static int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

    public void start() {
        try {
            final Robot robot = new Robot();
            robot.waitForIdle();

            Thread.sleep(FRAME_ACTIVATION_TIMEOUT);

            final Point srcPoint = frame.getLocationOnScreen();
            Dimension d = frame.getSize();
            srcPoint.translate(d.width / 2, d.height / 2);

            if (!pointInComponent(robot, srcPoint, frame)) {
                System.err.println("WARNING: Couldn't locate source frame.");
                return;
            }

            final Point dstPoint = new Point(srcPoint);
            dstPoint.translate(d.width / 4, d.height / 4);

            if (!pointInComponent(robot, dstPoint, frame)) {
                System.err.println("WARNING: Couldn't locate target frame.");
                return;
            }

            robot.mouseMove(srcPoint.x, srcPoint.y);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.mousePress(InputEvent.BUTTON3_MASK);
            for (;!srcPoint.equals(dstPoint);
                 srcPoint.translate(sign(dstPoint.x - srcPoint.x),
                                    sign(dstPoint.y - srcPoint.y))) {
                robot.mouseMove(srcPoint.x, srcPoint.y);
                Thread.sleep(50);
            }

            robot.mouseRelease(InputEvent.BUTTON3_MASK);
            robot.keyRelease(KeyEvent.VK_CONTROL);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("The test failed.");
        }

        if ( (isWin || isMac) && !dragRecognized) {
            throw new RuntimeException("Drag is not recognized on: " + os);
        } else if (!isWin && !isMac && dragRecognized) {
            throw new RuntimeException("Drag is recognized on: " + os);
        }
    }

    public void reset() {
        clickedComponent = null;
    }

    public void eventDispatched(AWTEvent e) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            clickedComponent = (Component)e.getSource();
            synchronized (SYNC_LOCK) {
                SYNC_LOCK.notifyAll();
            }
        }
    }

    boolean pointInComponent(Robot robot, Point p, Component comp)
      throws InterruptedException {
        robot.waitForIdle();
        reset();
        robot.mouseMove(p.x, p.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        synchronized (SYNC_LOCK) {
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            SYNC_LOCK.wait(MOUSE_RELEASE_TIMEOUT);
        }

        Component c = clickedComponent;

        while (c != null && c != comp) {
            c = c.getParent();
        }

        return c == comp;
    }
}
