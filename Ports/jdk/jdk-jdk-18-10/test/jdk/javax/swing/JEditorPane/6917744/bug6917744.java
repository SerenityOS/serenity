/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6917744 8194767
 * @summary JScrollPane Page Up/Down keys do not handle correctly html tables with different cells contents
 * @author Pavel Porvatov
 * @run main bug6917744
 */

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.*;

public class bug6917744 {
    private static JFrame frame;

    private static JEditorPane editorPane;

    private static JScrollPane scrollPane;

    private static Robot robot;

    private static Point p = null;

    static void blockTillDisplayed(JComponent comp) throws Exception {
        while (p == null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    p = comp.getLocationOnScreen();
                });
            } catch (IllegalStateException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        robot = new Robot();
        robot.setAutoDelay(100);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();

                editorPane = new JEditorPane();

                try {
                    editorPane.setPage(bug6917744.class.getResource("/test.html"));
                } catch (IOException e) {
                    frame.dispose();
                    throw new RuntimeException("HTML resource not found", e);
                }

                scrollPane = new JScrollPane(editorPane);

                frame.getContentPane().add(scrollPane);
                frame.setSize(400, 300);
                frame.setVisible(true);
            }
        });

        blockTillDisplayed(editorPane);
        robot.mouseMove(p.x+50, p.y+50);
        robot.waitForIdle();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        for (int i = 0; i < 50; i++) {
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
        }

        robot.waitForIdle();

        // Check that we at the end of document
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();

                if (model.getValue() + model.getExtent() != model.getMaximum()) {
                    frame.dispose();
                    throw new RuntimeException("Invalid HTML position");
                }
            }
        });

        robot.waitForIdle();

        for (int i = 0; i < 50; i++) {
            robot.keyPress(KeyEvent.VK_PAGE_UP);
            robot.keyRelease(KeyEvent.VK_PAGE_UP);
        }

        robot.waitForIdle();

        // Check that we at the begin of document
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                BoundedRangeModel model = scrollPane.getVerticalScrollBar().getModel();

                if (model.getValue() != model.getMinimum()) {
                    frame.dispose();
                    throw new RuntimeException("Invalid HTML position");
                }

                frame.dispose();
            }
        });
    }
}
