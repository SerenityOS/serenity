/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8032874
 * @summary Test whether ArrayIndexOutOfBoundsException is thrown or not,
 *          once selected row is removed from JTable with Sorter and Filter
 * @author Dmitry Markov
 * @run main bug8032874
 */

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

public class bug8032874 {
    private static final int ROW_COUNT = 5;
    private static JTable table;
    private static TestTableModel tableModel;

    public static void main(String args[]) throws Exception {
        Robot robot = new Robot();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                createAndShowUI();
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                table.getRowSorter().toggleSortOrder(0);
                table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                table.setRowSelectionInterval(1, 2);
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < ROW_COUNT; i++) {
                    tableModel.remove(0);
                    table.getRowSorter().toggleSortOrder(0);
                }
            }
        });
    }

    public static void createAndShowUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JFrame frame = new JFrame("bug8032874");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        tableModel = new TestTableModel();
        table = new JTable(tableModel);
        table.setSurrendersFocusOnKeystroke(true);

        final TableRowSorter<TestTableModel> rowSorter = new TableRowSorter<TestTableModel>(tableModel);
        rowSorter.setRowFilter(new RowFilter<TestTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TestTableModel, ? extends Integer> entry) {
                return entry.getIdentifier() % 2 == 0;
            }
        });
        table.setRowSorter(rowSorter);

        JScrollPane jScrollPane = new JScrollPane(table);
        panel.add(jScrollPane);

        frame.setContentPane(panel);
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }

    private static class TestTableModel extends AbstractTableModel {
        private final List<Integer> data;

        public TestTableModel() {
            data = new ArrayList<Integer>();

            for (int i = 0; i < ROW_COUNT; i++) {
                data.add(i);
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex);
        }

        public void remove(int row) {
            data.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }
}

