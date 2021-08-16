/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * @test
 * @key headful
 * @bug 8072767
 * @summary DefaultCellEditor for comboBox creates ActionEvent with wrong source
 *          object
 * @run main bug8072767
 */

public class bug8072767 {

    private static final String TEST1 = "Test";
    private static final String TEST2 = TEST1 + 1;

    private static JFrame frame;
    private static JTable table;
    private static volatile Point point;
    private static boolean testPass;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(100);
        SwingUtilities.invokeAndWait(bug8072767::createAndShowGUI);
        robot.waitForIdle();
        robot.delay(1000);
        SwingUtilities.invokeAndWait(() -> {
            point = table.getLocationOnScreen();
            Rectangle rect = table.getCellRect(0, 0, true);
            point.translate(rect.width / 2, rect.height / 2);
        });
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_1);
        robot.keyRelease(KeyEvent.VK_1);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            point = frame.getLocationOnScreen();
            point.translate(frame.getWidth() / 2, frame.getHeight() / 2);
        });

        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            testPass = TEST2.equals(table.getValueAt(0, 0));
            frame.dispose();
        });

        if (!testPass) {
            throw new RuntimeException("Table cell is not edited!");
        }
    }

    private static void createAndShowGUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);

        table = new JTable(
                new String[][]{{TEST1}}, new String[]{"Header"});
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setEditable(true);
        table.getColumnModel().getColumn(0).setCellEditor(
                new DefaultCellEditor(comboBox));
        frame.getContentPane().add(table);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }
}
