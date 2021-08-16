/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7154048
 * @summary Window created under a mouse does not receive mouse enter event.
 *     Mouse Entered/Exited events should be generated during dragging the window
 *     out of the frame and to the frame.
 * @library ../../regtesthelpers
 * @build Util
 * @author alexandr.scherbatiy area=awt.event
 * @run main DragWindowOutOfFrameTest
 */
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.concurrent.*;

import test.java.awt.regtesthelpers.Util;

public class DragWindowOutOfFrameTest {

    private static volatile int dragWindowMouseEnteredCount = 0;
    private static volatile int dragWindowMouseExitedCount = 0;
    private static volatile int dragWindowMouseReleasedCount = 0;
    private static volatile int buttonMouseEnteredCount = 0;
    private static volatile int buttonMouseExitedCount = 0;
    private static volatile int labelMouseEnteredCount = 0;
    private static volatile int labelMouseExitedCount = 0;
    private static volatile int labelMouseReleasedCount = 0;
    private static MyDragWindow dragWindow;
    private static JLabel label;
    private static JButton button;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();

        Point pointToClick = Util.invokeOnEDT(new Callable<Point>() {

            @Override
            public Point call() throws Exception {
                return getCenterPoint(label);
            }
        });


        robot.mouseMove(pointToClick.x, pointToClick.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        if (dragWindowMouseEnteredCount != 1 && dragWindowMouseExitedCount != 0) {
            throw new RuntimeException(
                    "Wrong number mouse Entered/Exited events on Drag Window!");
        }

        Point pointToDrag = Util.invokeOnEDT(new Callable<Point>() {

            @Override
            public Point call() throws Exception {
                label.addMouseListener(new LabelMouseListener());
                button.addMouseListener(new ButtonMouseListener());
                return getCenterPoint(button);
            }
        });

        robot.mouseMove(450, pointToClick.y);
        robot.waitForIdle();

        if (labelMouseEnteredCount != 0 && labelMouseExitedCount != 1) {
            throw new RuntimeException(
                    "Wrong number Mouse Entered/Exited events on label!");
        }

        robot.mouseMove(450, pointToDrag.y);
        robot.waitForIdle();

        if (labelMouseEnteredCount != 0 && labelMouseExitedCount != 1) {
            throw new RuntimeException(
                    "Wrong number Mouse Entered/Exited events on label!");
        }

        if (buttonMouseEnteredCount != 0 && buttonMouseExitedCount != 0) {
            throw new RuntimeException(
                    "Wrong number Mouse Entered/Exited events on button!");
        }

        robot.mouseMove(pointToDrag.y, pointToDrag.y);
        robot.waitForIdle();

        if (buttonMouseEnteredCount != 1 && buttonMouseExitedCount != 0) {
            throw new RuntimeException(
                    "Wrong number Mouse Entered/Exited events on button!");
        }

        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.waitForIdle();

        if (labelMouseReleasedCount != 1) {
            throw new RuntimeException("No MouseReleased event on label!");
        }
    }

    private static Point getCenterPoint(Component comp) {
        Point p = comp.getLocationOnScreen();
        Rectangle rect = comp.getBounds();
        return new Point(p.x + rect.width / 2, p.y + rect.height / 2);
    }

    private static void createAndShowGUI() {

        JFrame frame = new JFrame("Main Frame");
        frame.setLocation(100, 100);
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        label = new JLabel("Label");

        DragWindowCreationMouseListener listener = new DragWindowCreationMouseListener(frame);
        label.addMouseListener(listener);
        label.addMouseMotionListener(listener);

        button = new JButton("Button");
        Panel panel = new Panel(new BorderLayout());

        panel.add(label, BorderLayout.NORTH);
        panel.add(button, BorderLayout.CENTER);

        frame.getContentPane().add(panel);
        frame.setVisible(true);

    }

    private static Point getAbsoluteLocation(MouseEvent e) {
        return new Point(e.getXOnScreen(), e.getYOnScreen());
    }

    static class MyDragWindow extends Window {

        public MyDragWindow(Window parent, Point location) {
            super(parent);
            setSize(500, 300);
            setVisible(true);
            JPanel panel = new JPanel();
            add(panel);
            setLocation(location.x - 250, location.y - 150);
            addMouseListener(new DragWindowMouseListener());
        }

        void dragTo(Point point) {
            setLocation(point.x - 250, point.y - 150);
        }
    }

    static class DragWindowCreationMouseListener extends MouseAdapter {

        Point origin;
        Window parent;

        public DragWindowCreationMouseListener(Window parent) {
            this.parent = parent;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (dragWindow == null) {
                dragWindow = new MyDragWindow(parent, getAbsoluteLocation(e));
            } else {
                dragWindow.setVisible(true);
                dragWindow.dragTo(getAbsoluteLocation(e));
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            labelMouseReleasedCount++;
            if (dragWindow != null) {
                dragWindow.setVisible(false);
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (dragWindow != null) {
                dragWindow.dragTo(getAbsoluteLocation(e));
            }
        }
    }

    static class DragWindowMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            dragWindowMouseEnteredCount++;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            dragWindowMouseExitedCount++;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragWindowMouseReleasedCount++;
        }
    }

    static class LabelMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            labelMouseEnteredCount++;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            labelMouseExitedCount++;
        }
    }

    static class ButtonMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            buttonMouseEnteredCount++;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            buttonMouseExitedCount++;
        }
    }
}
