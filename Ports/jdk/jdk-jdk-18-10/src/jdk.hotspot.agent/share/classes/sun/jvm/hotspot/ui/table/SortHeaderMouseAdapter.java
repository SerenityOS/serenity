/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import com.sun.java.swing.ui.CommonUI;

/**
 * A mouse adapater which is attached to the header of a JTable. It listens
 * for mouse clicks on a column and sorts that column.
 */
public class SortHeaderMouseAdapter extends MouseAdapter {

    private SortableTableModel model;
    private JTable table;

    public SortHeaderMouseAdapter(JTable table, SortableTableModel model) {
        this.model = model;
        this.table = table;
    }

    public void mouseClicked(MouseEvent evt) {
        // XXX Benchmark sort performance
        //long start = System.currentTimeMillis();
        CommonUI.setWaitCursor(SwingUtilities.getRoot(table));

        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(evt.getX());
        int column = table.convertColumnIndexToModel(viewColumn);
        if (evt.getClickCount() == 1 && column != -1) {
            // Reverse the sorting direction.
            model.sortByColumn(column, !model.isAscending());
        }

        // XXX Benchmark performance
        //      System.out.println("Sort time: " +
        //         (System.currentTimeMillis() - start));
        CommonUI.setDefaultCursor(SwingUtilities.getRoot(table));
    }
}
