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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/*
 * @test
 * @key headful
 * @bug 7055065
 * @summary NullPointerException when sorting JTable with empty cell
 * @author Jonathan Lu
 * @library ../../regtesthelpers/
 * @build Util
 * @run main bug7055065
 */

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.concurrent.Callable;

public class bug7055065 {

    private static JTable table;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();

        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                createAndShowUI();
            }
        });

        robot.waitForIdle();
        clickCell(robot, 1, 1);
        Util.hitKeys(robot, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_BACK_SPACE,
                KeyEvent.VK_BACK_SPACE);

        robot.waitForIdle();
        clickColumnHeader(robot, 1);

        robot.waitForIdle();
        clickColumnHeader(robot, 1);
    }

    private static void clickCell(Robot robot, final int row, final int column)
        throws Exception {
        Point point = Util.invokeOnEDT(new Callable<Point>() {
            @Override
            public Point call() throws Exception {
                Rectangle rect = table.getCellRect(row, column, false);
                Point point = new Point(rect.x + rect.width / 2, rect.y
                    + rect.height / 2);
                SwingUtilities.convertPointToScreen(point, table);
                return point;
            }
        });

        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void clickColumnHeader(Robot robot, final int column)
        throws Exception {
        Point point = Util.invokeOnEDT(new Callable<Point>() {
            @Override
            public Point call() throws Exception {
                Rectangle rect = table.getCellRect(0, column, false);
                int headerHeight = table.getTableHeader().getHeight();
                Point point = new Point(rect.x + rect.width / 2, rect.y
                    - headerHeight / 2);
                SwingUtilities.convertPointToScreen(point, table);
                return point;
            }
        });

        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("SimpleTableDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel newContentPane = new JPanel();
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);

        final String[] columnNames = { "String", "Number" };
        final Object[][] data = { { "aaaa", new Integer(1) },
            { "bbbb", new Integer(3) }, { "cccc", new Integer(2) },
            { "dddd", new Integer(4) }, { "eeee", new Integer(5) } };
        table = new JTable(data, columnNames);

        table.setPreferredScrollableViewportSize(new Dimension(500, 400));
        table.setFillsViewportHeight(true);

        TableModel dataModel = new AbstractTableModel() {

            public int getColumnCount() {
                return columnNames.length;
            }

            public int getRowCount() {
                return data.length;
            }

            public Object getValueAt(int row, int col) {
                return data[row][col];
            }

            public String getColumnName(int column) {
                return columnNames[column];
            }

            public Class<?> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            public boolean isCellEditable(int row, int col) {
                return col != 5;
            }

            public void setValueAt(Object aValue, int row, int column) {
                data[row][column] = aValue;
            }
        };
        table.setModel(dataModel);
        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(
                dataModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        newContentPane.add(scrollPane);

        frame.pack();
        frame.setLocation(0, 0);
        frame.setVisible(true);
    }
}
