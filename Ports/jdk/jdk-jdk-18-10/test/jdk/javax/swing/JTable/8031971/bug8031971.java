/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Date;
import java.util.Hashtable;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 8031971 8039750
 * @author Alexander Scherbatiy
 * @summary Use only public methods in the SwingLazyValue
 * @run main/othervm bug8031971
 */
public class bug8031971 {

    static Object[][] RENDERERS = {
        {Object.class, "javax.swing.table.DefaultTableCellRenderer$UIResource"},
        {Number.class, "javax.swing.JTable$NumberRenderer"},
        {Float.class, "javax.swing.JTable$DoubleRenderer"},
        {Double.class, "javax.swing.JTable$DoubleRenderer"},
        {Date.class, "javax.swing.JTable$DateRenderer"},
        {Icon.class, "javax.swing.JTable$IconRenderer"},
        {ImageIcon.class, "javax.swing.JTable$IconRenderer"},
        {Boolean.class, "javax.swing.JTable$BooleanRenderer"}
    };

    static Object[][] EDITORS = {
        {Object.class, "javax.swing.JTable$GenericEditor"},
        {Number.class, "javax.swing.JTable$NumberEditor"},
        {Boolean.class, "javax.swing.JTable$BooleanEditor"}
    };

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            TestTable table = new TestTable();
            test(table.getDefaultRenderersByColumnClass(), RENDERERS);
            test(table.getDefaultEditorsByColumnClass(), EDITORS);
        });
    }

    static void test(Hashtable table, Object[][] values) {
        for (int i = 0; i < values.length; i++) {
            test(table.get(values[i][0]), (String) values[i][1]);
        }
    }

    static void test(Object obj, String className) {
        if (!obj.getClass().getCanonicalName().equals(className.replace('$', '.'))) {
            throw new RuntimeException("Wrong value!");
        }
    }

    static class TestTable extends JTable {

        Hashtable getDefaultRenderersByColumnClass() {
            return defaultRenderersByColumnClass;
        }

        Hashtable getDefaultEditorsByColumnClass() {
            return defaultEditorsByColumnClass;
        }
    }
}
