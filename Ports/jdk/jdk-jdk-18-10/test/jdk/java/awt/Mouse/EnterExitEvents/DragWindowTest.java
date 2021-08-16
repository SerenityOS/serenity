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
 *     Mouse Entered/Exited events are wrongly generated during dragging the window
 *     from one component to another
 * @library ../../regtesthelpers
 * @build Util
 * @author alexandr.scherbatiy area=awt.event
 * @run main DragWindowTest
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.concurrent.*;

import test.java.awt.regtesthelpers.Util;

public class DragWindowTest {

    private static volatile int dragWindowMouseEnteredCount = 0;
    private static volatile int dragWindowMouseReleasedCount = 0;
    private static volatile int buttonMouseEnteredCount = 0;
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

        if (dragWindowMouseEnteredCount != 1) {
            throw new RuntimeException("No MouseEntered event on Drag Window!");
        }

        Point pointToDrag = Util.invokeOnEDT(new Callable<Point>() {

            @Override
            public Point call() throws Exception {
                button.addMouseListener(new ButtonMouseListener());
                return getCenterPoint(button);
            }
        });

        robot.mouseMove(pointToDrag.x, pointToDrag.y);
        robot.waitForIdle();

        if (buttonMouseEnteredCount != 0) {
            throw new RuntimeException("Extra MouseEntered event on button!");
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
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        label = new JLabel("Label");

        LabelMouseListener listener = new LabelMouseListener(frame);
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

        static int d = 30;

        public MyDragWindow(Window parent, Point location) {
            super(parent);
            setSize(150, 150);
            setVisible(true);
            JPanel panel = new JPanel();
            add(panel);
            setLocation(location.x - d, location.y - d);
            addMouseListener(new DragWindowMouseListener());
        }

        void dragTo(Point point) {
            setLocation(point.x - d, point.y - d);
        }
    }

    static class LabelMouseListener extends MouseAdapter {

        Point origin;
        Window parent;

        public LabelMouseListener(Window parent) {
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
        public void mouseReleased(MouseEvent e) {
            dragWindowMouseReleasedCount++;
        }
    }

    static class ButtonMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            buttonMouseEnteredCount++;
        }
    }
}
