/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
/*
 * @test
 * @bug 8080972
 * @run main/othervm -Djava.security.manager=allow TestJTableCellEditor

 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 */

public class TestJTableCellEditor {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestJTableCellEditor::testJTableCellEditor);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestJTableCellEditor::testJTableCellEditor);
    }

    private static void testJTableCellEditor() {

        final Class cls = UserEditor.class;

        JTable table = new JTable(new AbstractTableModel() {
            public int getRowCount() {
                return 0;
            }

            public int getColumnCount() {
                return 1;
            }

            public Object getValueAt(int r, int c) {
                return "Some Value";
            }

            public Class getColumnClass(int c) {
                return cls;
            }
        });

        TableCellEditor editor = table.getDefaultEditor(Object.class);
        editor.getTableCellEditorComponent(table,
                UserEditor.TEST_VALUE, false, 0, 0);
        editor.stopCellEditing();
        Object obj = editor.getCellEditorValue();

        if (obj == null) {
            throw new RuntimeException("Editor object is null!");
        }

        if (!UserEditor.TEST_VALUE.equals(((UserEditor) obj).value)) {
            throw new RuntimeException("Value is incorrect!");
        }
    }

    public static class UserEditor {

        private static final String TEST_VALUE = "Test Value";

        private final String value;

        public UserEditor(String value) {
            this.value = value;
        }
    }
}
