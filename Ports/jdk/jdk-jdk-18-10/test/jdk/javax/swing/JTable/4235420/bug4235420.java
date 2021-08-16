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

/* @test
   @bug 4235420
   @summary Tests that JTable delays creating Renderers and Editors
   @author Peter Zhelezniakov
*/

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class bug4235420 {

    public static void main(String[] argv) throws Exception {
        for (UIManager.LookAndFeelInfo LF :
                UIManager.getInstalledLookAndFeels()) {
            try {
                UIManager.setLookAndFeel(LF.getClassName());
            } catch (UnsupportedLookAndFeelException ignored) {
                System.out.println("Unsupported L&F: " + LF.getClassName());
            } catch (ClassNotFoundException | InstantiationException
                     | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Testing L&F: " + LF.getClassName());

            if ("Nimbus".equals(UIManager.getLookAndFeel().getName()) ||
                "GTK".equals(UIManager.getLookAndFeel().getName())) {
                System.out.println("The test is skipped for Nimbus and GTK");

                continue;
            }

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    Table table = new Table();

                    table.test();
                }
            });
        }
    }

    private static class Table extends JTable {
        public void test() {
            // Renderers
            Class[] rendererClasses = {Object.class, Number.class, Date.class, ImageIcon.class, Boolean.class};

            Map copy = new HashMap(defaultRenderersByColumnClass);

            for (Class rendererClass : rendererClasses) {
                Object obj = copy.get(rendererClass);

                if (obj instanceof TableCellRenderer) {
                    throw new Error("Failed: TableCellRenderer created for " +
                            rendererClass.getClass().getName());
                }
            }

            // Editors
            Class[] editorClasses = {Object.class, Number.class, Boolean.class};

            copy = new HashMap(defaultEditorsByColumnClass);

            for (Class editorClass : editorClasses) {
                Object obj = copy.get(editorClass);

                if (obj instanceof TableCellEditor) {
                    throw new Error("Failed: TableCellEditor created for " +
                            editorClass.getClass().getName());
                }
            }
        }
    }
}
