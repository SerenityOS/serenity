/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7007967
 * @summary Verifies that DefaultRowSorter updates its cache properly when the
 *           all the row are deleted and then new rows are added to the table
 * @run main DefaultRowSorterCacheTest
 */


import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.SwingUtilities;

class CustomTableModel extends DefaultTableModel
{
    public CustomTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (getRowCount() > 0) {
            return getValueAt(0, columnIndex).getClass();
        }
        return super.getColumnClass(columnIndex);
    }
}

public class DefaultRowSorterCacheTest {

    public void testSort() {
        Object[] values = new Object[]{1, 2, 10};
        Object[][] data = new Object[][]{{values[0]}, {values[1]}, {values[2]}};

        //Create custom table model
        DefaultTableModel model = new CustomTableModel(data, new Object[]{"A"});

        TableRowSorter<DefaultTableModel> rowSorter =
                new TableRowSorter<DefaultTableModel>(model);
        rowSorter.toggleSortOrder(0);

        for (int row = 0; row < model.getRowCount(); row++) {
            // values are in sorted ascending. so the row index and
            // view index from sorter should be same
            if (row != rowSorter.convertRowIndexToView(row)) {
                throw new RuntimeException("Wrong sorting before making any " +
                        "changes in test case");
            }
        }

        // clear model and notify sorter
        model.setRowCount(0);
        rowSorter.rowsDeleted(0, values.length - 1);

        // refill the model and notify sorter
        for (int i = 0; i < values.length; i++) {
            model.addRow(new Object[]{values[i]});
            rowSorter.rowsInserted(i, i);
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            // values are in sorted ascending. so the row index and
            // view index from sorter should be same
            if (row != rowSorter.convertRowIndexToView(row)) {
                throw new RuntimeException("Wrong sorting at end of test case");
            }
        }
    }

    public static void main(String[] args) throws Exception{
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                new DefaultRowSorterCacheTest().testSort();
            }
        });
    }
}
