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

/* @test
   @bug 6442918 8005914
   @summary Ensures that empty table headers do not show "..."
   @author Shannon Hickey
   @library ../../regtesthelpers
   @build  Util
   @run main/manual bug6442918a
   @requires os.family == "windows"
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;


public class bug6442918a {

    public static void main(String[] args) throws Throwable, Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf"
                                    + ".windows.WindowsLookAndFeel");
                } catch (Exception e) {
                    // test is for Windows look and feel
                    throw new RuntimeException("Test is only for WLaF."
                                   + e.getMessage());
                }
                runTest();
            }
        });
    }

    private static void runTest() {
        JDialog dialog = Util
                    .createModalDialogWithPassFailButtons("Empty header showing \"...\"");
        String[] columnNames = {"", "", "", "", "Testing"};
        String[][] data = {{"1", "2", "3", "4", "5"}};
        JTable table = new JTable(data, columnNames);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        int tableCellWidth = renderer.getFontMetrics(renderer.getFont())
                .stringWidth("test");
        table.setPreferredScrollableViewportSize(new Dimension(
                5 * tableCellWidth, 50));
        JPanel p = new JPanel();
        p.add(new JScrollPane(table));
        dialog.add(p, BorderLayout.NORTH);
        JTextArea area = new JTextArea();
        String txt  = "\nInstructions:\n\n";
               txt += "Only the last column header should show \"...\".";
        area.setText(txt);
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.pack();
        dialog.setVisible(true);
    }
}
