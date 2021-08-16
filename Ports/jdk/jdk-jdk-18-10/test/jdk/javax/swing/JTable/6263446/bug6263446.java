/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6263446
 * @summary Tests that double-clicking to edit a cell doesn't select the content.
 * @author Shannon Hickey
 * @run main bug6263446
 */
import java.awt.Point;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;

public class bug6263446 {

    private static JFrame frame;
    private static JTable table;
    private static final String FIRST = "AAAAA";
    private static final String SECOND = "BB";
    private static final String ALL = FIRST + " " + SECOND;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        try {
            robot = new Robot();
            robot.setAutoDelay(50);

            SwingUtilities.invokeAndWait(() -> createAndShowGUI());
            robot.waitForIdle();
            robot.delay(1000);

            Point point = getClickPoint();
            robot.mouseMove(point.x, point.y);
            robot.waitForIdle();

            click(1);
            robot.waitForIdle();
            assertEditing(false);

            click(2);
            robot.waitForIdle();
            checkSelectedText(null);

            click(3);
            robot.waitForIdle();
            checkSelectedText(FIRST);

            click(4);
            robot.waitForIdle();
            checkSelectedText(ALL);

            setClickCountToStart(1);
            robot.waitForIdle();

            click(1);
            robot.waitForIdle();
            checkSelectedText(null);

            click(2);
            robot.waitForIdle();
            checkSelectedText(FIRST);

            click(3);
            robot.waitForIdle();
            checkSelectedText(ALL);

            setClickCountToStart(3);
            robot.waitForIdle();

            click(1);
            robot.waitForIdle();
            assertEditing(false);

            click(2);
            robot.waitForIdle();
            assertEditing(false);

            click(3);
            robot.waitForIdle();
            checkSelectedText(null);

            click(4);
            robot.waitForIdle();
            checkSelectedText(FIRST);

            click(5);
            robot.waitForIdle();
            checkSelectedText(ALL);

            SwingUtilities.invokeAndWait(() -> table.editCellAt(0, 0));

            robot.waitForIdle();
            assertEditing(true);

            click(2);
            robot.waitForIdle();
            checkSelectedText(FIRST);
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }

    private static void checkSelectedText(String sel) throws Exception {
        assertEditing(true);
        checkSelection(sel);
        robot.waitForIdle();
        cancelCellEditing();
        robot.waitForIdle();
        assertEditing(false);
    }

    private static void setClickCountToStart(final int clicks) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultCellEditor editor =
                    (DefaultCellEditor) table.getDefaultEditor(String.class);
            editor.setClickCountToStart(clicks);
        });
    }

    private static void cancelCellEditing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            table.getCellEditor().cancelCellEditing();
        });
    }

    private static void checkSelection(final String sel) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultCellEditor editor =
                    (DefaultCellEditor) table.getDefaultEditor(String.class);
            JTextField field = (JTextField) editor.getComponent();
            String text = field.getSelectedText();
            if (sel == null) {
                if (text != null && text.length() != 0) {
                    throw new RuntimeException("Nothing should be selected,"
                            + " but \"" + text + "\" is selected.");
                }
            } else if (!sel.equals(text)) {
                throw new RuntimeException("\"" + sel + "\" should be "
                        + "selected, but \"" + text + "\" is selected.");
            }
        });
    }

    private static void assertEditing(final boolean editing) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editing && !table.isEditing()) {
                throw new RuntimeException("Table should be editing");
            }
            if (!editing && table.isEditing()) {
                throw new RuntimeException("Table should not be editing");
            }
        });
    }

    private static Point getClickPoint() throws Exception {
        final Point[] result = new Point[1];
        SwingUtilities.invokeAndWait(() -> {
            Rectangle rect = table.getCellRect(0, 0, false);
            Point point = new Point(rect.x + rect.width / 5,
                                   rect.y + rect.height / 2);
            SwingUtilities.convertPointToScreen(point, table);
            result[0] = point;
        });
        return result[0];
    }

    private static void click(int times) {
        robot.delay(500);
        for (int i = 0; i < times; i++) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
    }

    private static TableModel createTableModel() {
        String[] columnNames = {"Column 0"};
        String[][] data = {{ALL}};

        return new DefaultTableModel(data, columnNames);
    }

    private static void createAndShowGUI() {
        frame = new JFrame("bug6263446");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        table = new JTable(createTableModel());
        frame.add(table);
        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        frame.toFront();
    }
}
