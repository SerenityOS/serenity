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
 * @bug 4220171
 * @author Konstantin Eremin
 * @summary Tests
 * @library ../../regtesthelpers
 * @build Util
 * @run main bug4220171
 */
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class bug4220171 {

    private static JTable table;
    private static JFrame frame;

    public static void main(String args[]) throws Exception {
        try {

            Robot robot = new Robot();
            robot.setAutoDelay(50);

            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();

            clickMouse(robot, 0, 0);
            Util.hitKeys(robot, KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_ENTER);
            robot.waitForIdle();
            checkCell(0, 0);

            clickMouse(robot, 0, 1);
            Util.hitKeys(robot, KeyEvent.VK_D, KeyEvent.VK_E, KeyEvent.VK_ENTER);
            robot.waitForIdle();
            checkCell(0, 1);

            clickMouse(robot, 1, 0);
            Util.hitKeys(robot, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_ENTER);
            robot.waitForIdle();
            checkCell(1, 0);

            clickMouse(robot, 1, 1);
            Util.hitKeys(robot, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_ENTER);
            robot.waitForIdle();
            checkCell(1, 1);
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static void checkCell(final int row, final int column) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                if (table.getValueAt(row, column) != null) {
                    throw new RuntimeException(
                            String.format("Cell (%d, %d) is editable", row, column));
                }
            }
        });
    }

    static void clickMouse(Robot robot, int row, int column) throws Exception {
        Point point = getCellClickPoint(row, column);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static Point getCellClickPoint(final int row, final int column) throws Exception {
        final Point[] result = new Point[1];
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                Rectangle rect = table.getCellRect(row, column, false);
                Point point = new Point(rect.x + rect.width / 2,
                        rect.y + rect.height / 2);
                SwingUtilities.convertPointToScreen(point, table);
                result[0] = point;
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);

        table = new JTable(2, 2);
        table.setEnabled(false);

        frame.getContentPane().add(table);
        frame.setVisible(true);
    }
}
