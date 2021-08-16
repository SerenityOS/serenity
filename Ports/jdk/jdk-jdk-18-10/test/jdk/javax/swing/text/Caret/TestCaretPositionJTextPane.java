/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8232243
 * @key headful
 * @summary  Verifies caret position in multiline JTextPane
 *           in hidpi mode should be in sync with mouse press position.
 * @run main/othervm -Dsun.java2d.uiScale=2 TestCaretPositionJTextPane
 */

import javax.swing.JComboBox;
import javax.swing.JTextPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

public class TestCaretPositionJTextPane {
    private static JTextPane textPane;
    private static JFrame f;

    private static void createUI() {
        f = new JFrame("Test Cursor/Caret with Java 9");

        textPane = new JTextPane();
        textPane.setFont(new java.awt.Font("Dialog", Font.PLAIN, 12));

        fillTextPane(textPane);

        textPane.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    Caret caret = textPane.getCaret();
                    Rectangle2D rect = textPane.modelToView2D(caret.getDot());

                    if (Math.abs(e.getPoint().x - rect.getX()) > 5) {
                        System.out.println("mouse point " + e.getPoint());
                        System.out.println("caret position " + rect);
                        throw new RuntimeException(" Wrong caret position");
                    }
                } catch (BadLocationException ex) {}
            }

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });
        f.add(new JScrollPane(textPane), BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);
    }

    public static void main(String args[]) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(200);

            SwingUtilities.invokeAndWait(() -> createUI());

            robot.waitForIdle();
            Point p = textPane.getLocationOnScreen();
            robot.mouseMove(p.x+ 380, p.y+6);
            robot.waitForIdle();
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
        } finally {
             SwingUtilities.invokeAndWait(() -> f.dispose());
        }
    }

    private static void fillTextPane(JTextPane textPane) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < 30; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < 50; j++) {
                row.append(j);
                if (j % 5 == 0) {
                    row.append(" ");
                }
            }
            buf.append(row).append(System.lineSeparator());
        }
        textPane.setText(buf.toString());
        textPane.setCaretPosition(0);
    }
}
