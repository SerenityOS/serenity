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

/*
 * @test
 * @key headful
 * @bug 8159062
 * @summary Tests DnD property with HIDPI scale set to non-interger value 2.5
 * @run main/othervm -Dsun.java2d.uiScale=2.5 DragTestWithHIDPI
 */

import javax.swing.JList;
import javax.swing.TransferHandler;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.Robot;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.event.InputEvent;

public class DragTestWithHIDPI extends TransferHandler {

    private static boolean didDrag = false;
    private static int threshold = 10;
    private static int DEFAULT_DELAY = 550;

    private Robot robot = null;
    private static JList list = null;
    private static Point listLocation = null;
    private static Dimension listSize = null;
    private static JFrame f = null;

    private enum Direction {
        RIGHT, LEFT, BOTTOM, TOP
    }

    public static void main(String[] args) throws Exception {
        DragTestWithHIDPI test = new DragTestWithHIDPI();

        // set the mouse move drag threshold
        System.setProperty("awt.dnd.drag.threshold", String.valueOf(threshold));

        test.createGUI();
        test.doTest();
        System.out.println("Test Passed");
        test.disposeGUI();
    }

    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        didDrag = true;
    }

    public DragTestWithHIDPI() {
        super("foreground");
    }

    private void createGUI() throws Exception{
        SwingUtilities.invokeAndWait(() -> {
            String[] listData =
                    new String[]{"Pacific Ocean", "Atlantic Ocean", "Indian Ocean",
                            "Arctic Ocean"};
            list = new JList(listData);
            list.setDragEnabled(true);
            list.setTransferHandler(new DragTestWithHIDPI());

            f = new JFrame("DragTestWithHIDPI");
            f.getContentPane().add(list, BorderLayout.CENTER);
            f.pack();
            f.toFront();
            f.setVisible(true);

            listLocation = list.getLocationOnScreen();
            listSize = list.getSize();
        });
    }

    private void disposeGUI () throws  Exception {
        SwingUtilities.invokeAndWait(() -> {
            f.dispose();
        });
    }

    private void doTest() throws Exception {

        robot = new Robot();
        robot.waitForIdle();

        for (Direction direction : Direction.values()) {
            //Drag should not start only by moving (threshold - 1) pixels
            didDrag = false;
            test(threshold - 1, direction);
            if (didDrag) {
                disposeGUI();
                throw new RuntimeException(
                        "Shouldn't start drag until > " + threshold +
                                " pixels " + " while moving " + direction);
            }

            // Drag should not start only by moving threshold pixels
            didDrag = false;
            test(threshold, direction);
            if (didDrag) {
                disposeGUI();
                throw new RuntimeException(
                        "Shouldn't start drag until > " + threshold +
                                " pixels" + " while moving " + direction);
            }

            // Drag should start after moving threshold + 1 pixel
            didDrag = false;
            test(threshold + 1, direction);
            if (!didDrag) {
                disposeGUI();
                throw new RuntimeException(
                        "Should start drag after > " + threshold + " pixels" +
                                " while moving " + direction);
            }
        }
    }

    private void test(int threshold, Direction direction) {
        clickMouseOnList(InputEvent.BUTTON1_DOWN_MASK);
        Point p = MouseInfo.getPointerInfo().getLocation();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(DEFAULT_DELAY);
        glide(p, direction, threshold);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void glide(Point p, Direction direction, int toAdd) {
        switch (direction) {
            case RIGHT:
                // move towards right
                glide(p.x, p.y, p.x + toAdd, p.y);
                break;
            case LEFT:
                // move towards left
                glide(p.x, p.y, p.x - toAdd, p.y);
                break;
            case BOTTOM:
                // move towards bottom
                glide(p.x, p.y, p.x, p.y + toAdd);
                break;
            case TOP:
                // move towards top
                glide(p.x, p.y, p.x, p.y - toAdd);
                break;

        }
    }

    /*
    Some utilities functions from JRobot class.
     */
    private void moveMouseToList() {
        int x = listLocation.x + listSize.width/2;
        int y = listLocation.y + listSize.height/2;
        robot.mouseMove(x, y);
        robot.delay(DEFAULT_DELAY);
    }

    private void clickMouse(int buttons) {
        robot.mousePress(buttons);
        robot.mouseRelease(buttons);
        robot.delay(DEFAULT_DELAY);
    }

    private void clickMouseOnList(int buttons) {
        moveMouseToList();
        clickMouse(buttons);
    }

    private void glide(int x0, int y0, int x1, int y1) {
        float dmax = (float)Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        float dx = (x1 - x0) / dmax;
        float dy = (y1 - y0) / dmax;

        robot.mouseMove(x0, y0);
        for (int i=1; i<=dmax; i++) {
            robot.mouseMove((int)(x0 + dx*i), (int)(y0 + dy*i));
        }
        robot.delay(DEFAULT_DELAY);
    }
}

