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
 * @bug 7188612
 * @summary AccessibleTableHeader and AccessibleJTableCell should stick to
 *    AccessibleComponent.getLocationOnScreen api.
 * @author Frank Ding
 */

import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleTable;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class JTableAccessibleGetLocationOnScreen {
    private static JFrame frame;
    private static JTable table;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                constructInEDT();
                try {
                    assertGetLocation();
                } finally {
                    frame.dispose();
                }
            }
        });

    }

    private static void constructInEDT() {
        String[] columnNames = { "col1", "col2", };
        Object[][] data = { { "row1, col1", "row1, col2" },
                { "row2, col1", "row2, col2" }, };

        frame = new JFrame(
                "JTable AccessibleTableHeader and AccessibleJTableCell test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        table = new JTable(data, columnNames);
        frame.add(table);
        frame.pack();
    }

    private static void assertGetLocation() {
        // the frame is now invisible
        // test getLocationOnScreen() of
        // JTable$AccessibleJTable$AccessibleJTableHeaderCell
        // and JTable$AccessibleJTable$AccessibleJTableCell
        AccessibleTable accessibleTable = (AccessibleTable) table
                .getAccessibleContext();
        AccessibleTable header = accessibleTable.getAccessibleColumnHeader();
        AccessibleComponent accessibleComp1 = (AccessibleComponent) header
                .getAccessibleAt(0, 0);
        // getLocation() must be null according to its javadoc and no exception
        // is thrown
        if (null != accessibleComp1.getLocationOnScreen()) {
            throw new RuntimeException(
                    "JTable$AccessibleJTable$AccessibleJTableHeaderCell."
                            + "getLocation() must be null");
        }

        JComponent.AccessibleJComponent accessibleJComponent =
                (JComponent.AccessibleJComponent) table.getAccessibleContext();
        AccessibleComponent accessibleComp2 = (AccessibleComponent)
                accessibleJComponent.getAccessibleChild(3);
        // getLocation() must be null according to its javadoc and no exception
        // is thrown
        if (null != accessibleComp2.getLocationOnScreen()) {
            throw new RuntimeException("JTable$AccessibleJTable$"
                    + "AccessibleJTableCell.getLocation() must be null");
        }

    }
}
