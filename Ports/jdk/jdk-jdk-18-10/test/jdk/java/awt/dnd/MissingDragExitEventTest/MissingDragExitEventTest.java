/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027913
 * @library ../../regtesthelpers
 * @build Util
 * @compile MissingDragExitEventTest.java
 * @run main/othervm MissingDragExitEventTest
 * @author Sergey Bylokhov
 */

import java.awt.Color;
import java.awt.Point;
import java.awt.Robot;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import test.java.awt.regtesthelpers.Util;

public class MissingDragExitEventTest {

    private static volatile JFrame frame;
    private static boolean FAILED;
    private static boolean MOUSE_ENTERED_DT;
    private static boolean MOUSE_ENTERED;
    private static boolean MOUSE_EXIT_TD;
    private static boolean MOUSE_EXIT;
    private static int SIZE = 300;

    private static void initAndShowUI() {
        frame = new JFrame("Test frame");

        frame.setSize(SIZE, SIZE);
        frame.setLocationRelativeTo(null);
        final JTextArea jta = new JTextArea();
        jta.setBackground(Color.RED);
        frame.add(jta);
        jta.setText("1234567890");
        jta.setFont(jta.getFont().deriveFont(150f));
        jta.setDragEnabled(true);
        jta.selectAll();
        jta.setDropTarget(new DropTarget(jta, DnDConstants.ACTION_COPY,
                                         new TestdropTargetListener()));
        jta.addMouseListener(new TestMouseAdapter());
        frame.setVisible(true);
    }

    public static void main(final String[] args) throws Exception {
        try {
            final Robot r = new Robot();
            r.setAutoDelay(50);
            r.mouseMove(100, 100);
            Util.waitForIdle(r);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    initAndShowUI();
                }
            });

            final Point inside = new Point(frame.getLocationOnScreen());
            inside.translate(20, SIZE / 2);
            final Point outer = new Point(inside);
            outer.translate(-40, 0);
            r.mouseMove(inside.x, inside.y);
            r.mousePress(InputEvent.BUTTON1_MASK);
            try {
                for (int i = 0; i < 3; ++i) {
                    Util.mouseMove(r, inside, outer);
                    Util.mouseMove(r, outer, inside);
                }
            } finally {
                r.mouseRelease(InputEvent.BUTTON1_MASK);
            }
            sleep(r);

            if (FAILED || !MOUSE_ENTERED || !MOUSE_ENTERED_DT || !MOUSE_EXIT
                    || !MOUSE_EXIT_TD) {
                throw new RuntimeException("Failed");
            }
        } finally {
            if (frame != null) {
                frame.dispose();
            }
        }
    }

    private static void sleep(Robot robot) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
        robot.waitForIdle();
    }

    static class TestdropTargetListener extends DropTargetAdapter {

        private volatile boolean inside;

        @Override
        public void dragEnter(final DropTargetDragEvent dtde) {
            if (inside) {
                FAILED = true;
                Thread.dumpStack();
            }
            inside = true;
            MOUSE_ENTERED_DT = true;
            try {
                Thread.sleep(10000); // we should have time to leave a component
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void dragOver(final DropTargetDragEvent dtde) {
            if (!inside) {
                FAILED = true;
                Thread.dumpStack();
            }
        }

        @Override
        public void dragExit(final DropTargetEvent dte) {
            if (!inside) {
                FAILED = true;
                Thread.dumpStack();
            }
            inside = false;
            MOUSE_EXIT_TD = true;
        }

        @Override
        public void drop(final DropTargetDropEvent dtde) {
            if (!inside) {
                FAILED = true;
                Thread.dumpStack();
            }
            inside = false;
        }
    }

    static class TestMouseAdapter extends MouseAdapter {

        private volatile boolean inside;

        @Override
        public void mouseEntered(final MouseEvent e) {
            if (inside) {
                FAILED = true;
                Thread.dumpStack();
            }
            inside = true;
            MOUSE_ENTERED = true;
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            if (!inside) {
                FAILED = true;
                Thread.dumpStack();
            }
            inside = false;
            MOUSE_EXIT = true;
        }
    }
}
