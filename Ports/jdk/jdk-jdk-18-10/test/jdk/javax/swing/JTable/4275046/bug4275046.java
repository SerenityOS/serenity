/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4275046
 * @summary Tests editable combo box as a table editor.
 * @run main bug4275046
 */

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class bug4275046 {

    private static final String[] colNames = { "ID", "Color", "Stuff" };
    private static final Object[][] data = { { 1, "red", "abc"},
                                             { 2, "red", "def"},
                                             { 3, "red", "ghijk"} };

    private static final String EXPECTED_VALUE = "rededited";

    private JFrame frame;
    private JTable table;

    private volatile Point tableLoc;
    private volatile Rectangle cellRect;

    private volatile Object editedValue;
    private volatile boolean testResult;

    private final Robot robot;

    public static void main(String[] args) throws Exception {
        final bug4275046 test = new bug4275046();
        test.test();
    }

    public bug4275046() throws AWTException {
        robot = new Robot();
        robot.setAutoDelay(100);
    }

    private void createGUI() {
        frame = new JFrame("bug4275046");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JComboBox<Object> cb = new JComboBox<>(
                new Object[] {"blue", "yellow", "green", "red"});
        cb.setEditable(true);
        DefaultCellEditor comboEditor = new DefaultCellEditor(cb);
        comboEditor.setClickCountToStart(1);

        DefaultTableModel model = new DefaultTableModel(data, colNames);
        table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellEditor(comboEditor);

        frame.add(table);
        frame.pack();
        frame.setSize(550, 400);
        frame.setVisible(true);
    }

    private void test() throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createGUI();
                }
            });

            runTest();
            checkResult();
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (frame != null) {
                        frame.dispose();
                    }
                }
            });
        }
    }

    private void runTest() throws Exception {
        robot.waitForIdle();

        // Click the first cell in the "color" column
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                tableLoc = table.getLocationOnScreen();
                cellRect = table.getCellRect(0, 1, true);
            }
        });

        robot.mouseMove(tableLoc.x + cellRect.x + cellRect.width / 2,
                        tableLoc.y + cellRect.y + cellRect.height / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        // Edit the cell
        robot.keyPress(KeyEvent.VK_E);
        robot.keyRelease(KeyEvent.VK_E);

        robot.keyPress(KeyEvent.VK_D);
        robot.keyRelease(KeyEvent.VK_D);

        robot.keyPress(KeyEvent.VK_I);
        robot.keyRelease(KeyEvent.VK_I);

        robot.keyPress(KeyEvent.VK_T);
        robot.keyRelease(KeyEvent.VK_T);

        robot.keyPress(KeyEvent.VK_E);
        robot.keyRelease(KeyEvent.VK_E);

        robot.keyPress(KeyEvent.VK_D);
        robot.keyRelease(KeyEvent.VK_D);
        robot.delay(100);

        // Click another cell
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                cellRect = table.getCellRect(1, 2, true);
            }
        });

        robot.mouseMove(tableLoc.x + cellRect.x + cellRect.width / 2,
                        tableLoc.y + cellRect.y + cellRect.height / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(100);
    }

    private void checkResult() throws Exception {
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            // Read the edited value of from the cell
            editedValue = table.getModel().getValueAt(0, 1);
            System.out.println("The edited value is = " + editedValue);
            testResult = editedValue.equals(EXPECTED_VALUE);
            if (testResult) {
                System.out.println("Test passed");
            } else {
                System.out.println("Test failed");
            }
        }
        });
        if (!testResult) {
            throw new RuntimeException("Expected value in the cell: '" +
                                       EXPECTED_VALUE + "' but found '" + editedValue + "'.");
        }
    }
}
