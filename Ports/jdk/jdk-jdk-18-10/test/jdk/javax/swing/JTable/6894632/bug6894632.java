/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6894632
 * @summary Removing rows from a DefaultTableModel with a RowSorter deselectes
 * last row
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.List;

public class bug6894632 {
    private static JTable table;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {

                //init table with empty sort order
                test(new ArrayList<RowSorter.SortKey>());

                //init table as unsorted
                List<RowSorter.SortKey> sortKeys = new ArrayList<>();
                sortKeys.add(0, new RowSorter.SortKey(0, SortOrder.UNSORTED));
                test(sortKeys);
            }
        });

        System.out.println("ok");
    }

    static void test(final List<RowSorter.SortKey> sortKeys) {
        final JFrame frame = new JFrame();
        try {
            frame.setUndecorated(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            table = new JTable();
            DefaultTableModel tableModel =
                    new DefaultTableModel(10, 1) {
                        public Object getValueAt(int row, int column) {
                            return row == getRowCount() - 1 ? row + "==last" :
                                    row;
                        }
                    };
            table.setModel(tableModel);
            TableRowSorter<TableModel> sorter =
                    new TableRowSorter<TableModel>(tableModel);
            sorter.setSortKeys(sortKeys);
            table.setRowSorter(sorter);

            frame.setContentPane(table);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            int lastRow = table.getRowCount() - 1;

            //select last row
            table.setRowSelectionInterval(lastRow, lastRow);

            //remove row above the last
            tableModel.removeRow(lastRow - 1);
            lastRow = table.getRowCount() - 1;
            if (lastRow != table.getSelectedRow()) {
                throw new RuntimeException("last row must be still selected");
            }

            //sort table
            sortKeys.clear();
            sortKeys.add(0, new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);
            //remove row above the last
            lastRow = table.getRowCount() - 1;
            tableModel.removeRow(lastRow - 1);

            if (!table.getValueAt(table.getSelectedRow(), 0).toString()
                    .endsWith("==last")) {
                throw new RuntimeException(
                        "row ends with \"==last\" row must be still selected");
            }

            //make table unsorted again
            sortKeys.clear();
            sortKeys.add(0, new RowSorter.SortKey(0, SortOrder.UNSORTED));
            sorter.setSortKeys(sortKeys);
            //remove row above the last
            lastRow = table.getRowCount() - 1;
            tableModel.removeRow(lastRow - 1);

            lastRow = table.getRowCount() - 1;
            if (lastRow != table.getSelectedRow()) {
                throw new RuntimeException(
                        "last row must be still selected");
            }
        } finally {
            frame.dispose();
        }
    }


}
