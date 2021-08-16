/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160087
 * @summary Change IOOBE to warning in the scenarios when it had not being
 *          thrown before the JDK-8078514
 * @run main/othervm DefaultRowSorterIOOBEtest
 */

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class DefaultRowSorterIOOBEtest extends TableRowSorter<TableModel> {
    static List<String> rows = new ArrayList<>();

    static TableModel tableModel = new AbstractTableModel() {

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return rows.get(rowIndex);
        }
    };

    public static void main(String[] args) {
        DefaultRowSorter<TableModel, Integer> sorter =
            new DefaultRowSorter<>() {
            {
                setModelWrapper(new SorterModelWrapper());
            }
        };

        PrintStream err = System.err;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000) {
            @Override
            public synchronized void write(byte[] b, int off, int len) {
                super.write(b, off, len);
                err.print(new String(b, off, len));
            }
        };
        System.setErr(new PrintStream(bos));

        rows.add("New");

        sorter.convertRowIndexToView(0);
        sorter.convertRowIndexToModel(0);

        String out = new String(bos.toByteArray());
        if(out.indexOf("WARNING:") < 0) {
            throw new RuntimeException("No warnings found");
        }
    }

    static class SorterModelWrapper extends
                            DefaultRowSorter.ModelWrapper<TableModel, Integer> {

        @Override
        public TableModel getModel() {
            return tableModel;
        }

        @Override
        public int getColumnCount() {
            return tableModel.getColumnCount();
        }

        @Override
        public int getRowCount() {
            return tableModel.getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return tableModel.getValueAt(row, column);
        }

        @Override
        public Integer getIdentifier(int row) {
            return row;
        }
    }
}
