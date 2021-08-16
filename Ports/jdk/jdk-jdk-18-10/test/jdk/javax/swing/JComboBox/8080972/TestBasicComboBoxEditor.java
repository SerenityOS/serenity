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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxEditor;
/*
 * @test
 * @bug 8080972
 * @run main/othervm -Djava.security.manager=allow TestBasicComboBoxEditor
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 */

public class TestBasicComboBoxEditor {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestBasicComboBoxEditor::testBasicComboBoxEditor);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestBasicComboBoxEditor::testBasicComboBoxEditor);
    }

    private static void testBasicComboBoxEditor() {

        BasicComboBoxEditor comboBoxEditor = new BasicComboBoxEditor();
        comboBoxEditor.setItem(new UserComboBoxEditorType("100"));

        JTextField editor = (JTextField) comboBoxEditor.getEditorComponent();
        editor.setText("200");
        UserComboBoxEditorType item = (UserComboBoxEditorType) comboBoxEditor.getItem();

        if (!item.str.equals("200")) {
            throw new RuntimeException("Wrong itme value!");
        }
    }

    public static class UserComboBoxEditorType {

        String str;

        public UserComboBoxEditorType(String str) {
            this.str = str;
        }

        public static UserComboBoxEditorType valueOf(String str) {
            return new UserComboBoxEditorType(str);
        }

        @Override
        public String toString() {
            return "UserComboBoxEditorType: " + str;
        }
    }
}
