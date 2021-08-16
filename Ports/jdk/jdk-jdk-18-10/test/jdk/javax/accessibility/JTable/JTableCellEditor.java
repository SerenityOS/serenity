/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleTable;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * @test
 * @bug 8226653
 * @key headful
 * @summary The active cell editor should be reported as a child of the table.
 *          Note that the accessibility API ignores the real children of the
 *          table, but reports the "virtual" child per cell in the grid.
 */
public final class JTableCellEditor {

    private static final int COUNT = 3;
    private static JTable table;
    private static JFrame frame;

    public static void main(final String[] args)
            throws InvocationTargetException, InterruptedException {
        EventQueue.invokeAndWait(() -> {
            frame = new JFrame();
            table = new JTable(testSelectionWithFilterTable());
            frame.add(table);
            frame.pack();
        });
        EventQueue.invokeAndWait(() -> table.editCellAt(1, 1));
        EventQueue.invokeAndWait(() -> {
            AccessibleTable aTable = table.getAccessibleContext()
                                          .getAccessibleTable();
            int aColumns = aTable.getAccessibleColumnCount();
            int aRows = aTable.getAccessibleRowCount();
            // We cannot assume which component will be used as an editor of the
            // table cell, but we can expect it will have the "text" role.
            AccessibleRole role = aTable.getAccessibleAt(1, 1)
                                        .getAccessibleContext()
                                        .getAccessibleRole();
            frame.dispose();
            if (!role.toDisplayString(Locale.ENGLISH).equals("text")) {
                throw new RuntimeException("Unexpected role: " + role);
            }
            if (aColumns != COUNT) {
                throw new RuntimeException("Wrong columns: " + aColumns);
            }
            if (aRows != COUNT) {
                throw new RuntimeException("Wrong rows: " + aRows);
            }
        });
    }

    /**
     * Creates a dummy table model.
     */
    private static TableModel testSelectionWithFilterTable() {
        DefaultTableModel model = new DefaultTableModel(0, 3);
        for (int i = 0; i < COUNT; i++) {
            model.addRow(new Object[]{i + "x0", i + "x1", i + "x2"});
        }
        return model;
    }
}
