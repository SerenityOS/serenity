/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6711682
 * @summary  JCheckBox in JTable: checkbox doesn't alaways respond to the first mouse click
 * @author Alexander Potochkin
 * @run main bug6711682
 */

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public class bug6711682 {
    private static JCheckBox editorCb;
    private static JCheckBox rendererCb;
    private static JTable table;
    private static JFrame f;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(50);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
            robot.waitForIdle();
            Point l = table.getLocationOnScreen();
            int h = table.getRowHeight();
            for (int i = 0; i < 3; i++) {
                robot.mouseMove(l.x + 5, l.y + 5 + i * h);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
            // Without pressing F2 the last table's cell
            // reported <code>false</code> value
            // note that I can't press it inside the for loop
            // because it doesn't reproduce the bug
            robot.keyPress(KeyEvent.VK_F2);
            robot.keyRelease(KeyEvent.VK_F2);

            for (int i = 0; i < 3; i++) {
                if (!Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                    throw new RuntimeException("Row #" + i + " checkbox is not selected");
                }
            }
            for (int i = 2; i >= 0; i--) {
                robot.mouseMove(l.x + 5, l.y + 5 + i * h);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
            robot.keyPress(KeyEvent.VK_F2);
            robot.keyRelease(KeyEvent.VK_F2);
            for (int i = 0; i < 3; i++) {
                if (Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                    throw new RuntimeException("Row #" + i + " checkbox is selected");
                }
            }
        } finally {
            if (f != null) SwingUtilities.invokeAndWait(() -> f.dispose());
        }
    }

    private static void createAndShowGUI() {
        editorCb = new JCheckBox();
        rendererCb = new JCheckBox();
        f = new JFrame("Table with CheckBox");
        Container p = f.getContentPane();
        p.setLayout(new BorderLayout());
        table = new JTable(new Object[][]{{false}, {false}, {false}}, new Object[]{"CheckBox"});
        TableCellEditor editor = new TableCellEditor() {
            int editedRow;

            public Component getTableCellEditorComponent(JTable table,
                                                         Object value, boolean isSelected, int row, int column) {
                this.editedRow = row;
                editorCb.setSelected(Boolean.TRUE.equals(value));
                editorCb.setBackground(UIManager.getColor("Table.selectionBackground"));
                return editorCb;
            }

            public void addCellEditorListener(CellEditorListener l) {
            }

            public void cancelCellEditing() {
            }

            public Object getCellEditorValue() {
                return editorCb.isSelected();
            }

            public boolean isCellEditable(EventObject anEvent) {
                return true;
            }

            public void removeCellEditorListener(CellEditorListener l) {
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }

            public boolean stopCellEditing() {
                table.getModel().setValueAt(editorCb.isSelected(), editedRow, 0);
                return true;
            }
        };
        table.getColumnModel().getColumn(0).setCellEditor(editor);

        TableCellRenderer renderer = new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                rendererCb.setSelected(Boolean.TRUE.equals(value));
                return rendererCb;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(renderer);

        p.add(table, BorderLayout.CENTER);

        f.pack();
        f.setVisible(true);
    }
}
