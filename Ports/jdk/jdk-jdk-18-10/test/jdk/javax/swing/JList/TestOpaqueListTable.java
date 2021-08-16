/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *  @test
 *  @bug 8253266 8264950
 *  @summary setUIProperty should work when opaque property is not set by
 *  client
 *  @key headful
 *  @run main TestOpaqueListTable
 */

public class TestOpaqueListTable {

    public static void main(String[] args) throws Exception {
        UIManager.LookAndFeelInfo[] installedLookAndFeels;
        installedLookAndFeels = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo LF : installedLookAndFeels) {
            try {
                UIManager.setLookAndFeel(LF.getClassName());
                SwingUtilities.invokeAndWait(() -> {
                    JList list = new JList();
                    JTable table = new JTable();
                    JTree tree = new JTree();
                    JToolTip toolTip = new JToolTip();
                    JViewport viewport = new JViewport();
                    String opaqueValue =  new String(" ");

                    if (!list.isOpaque()) {
                        opaqueValue += "JList, ";
                    }
                    if (!table.isOpaque()) {
                        opaqueValue += "JTable, ";
                    }
                    if (!tree.isOpaque()) {
                        opaqueValue += "JTree, ";
                    }
                    if (!toolTip.isOpaque()) {
                        opaqueValue += "JToolTip, ";

                    }
                    if (!viewport.isOpaque()) {
                        opaqueValue += "JViewport, ";
                    }

                    if(!opaqueValue.equals(" ")) {
                        throw new RuntimeException("Default value of " +
                                "\"opaque\" property for " + opaqueValue
                                + " is changed ");
                    }

                    LookAndFeel.installProperty(list, "opaque", false);
                    LookAndFeel.installProperty(table, "opaque", false);
                    LookAndFeel.installProperty(tree, "opaque", false);
                    LookAndFeel.installProperty(toolTip,"opaque",false);
                    LookAndFeel.installProperty(viewport,"opaque",false);

                    opaqueValue = " ";
                    if (list.isOpaque()) {
                        opaqueValue += "JList, ";
                    }
                    if (table.isOpaque()) {
                        opaqueValue += "JTable, ";
                    }
                    if (tree.isOpaque()) {
                        opaqueValue += "JTree, ";
                    }
                    if (toolTip.isOpaque()) {
                        opaqueValue += "JToolTip, ";
                    }
                    if (viewport.isOpaque()) {
                        opaqueValue += "JViewport, ";
                    }
                    if (!opaqueValue.equals(" ")) {
                        throw new RuntimeException(
                                "setUIProperty failed to clear " +
                                        opaqueValue +" opaque" +
                                        " when opaque is not set by client");
                    }


                    list.setOpaque(true);
                    table.setOpaque(true);
                    tree.setOpaque(true);
                    toolTip.setOpaque(true);
                    viewport.setOpaque(true);

                    LookAndFeel.installProperty(list,"opaque",false);
                    LookAndFeel.installProperty(table, "opaque", false);
                    LookAndFeel.installProperty(tree, "opaque", false);
                    LookAndFeel.installProperty(toolTip, "opaque", false);
                    LookAndFeel.installProperty(viewport, "opaque", false);

                    opaqueValue = " ";

                    if (!list.isOpaque()) {
                        opaqueValue += "JList";
                    }
                    if (!table.isOpaque()) {
                        opaqueValue += "JTable";
                    }
                    if (!tree.isOpaque()) {
                        opaqueValue += "JTree";
                    }
                    if (!toolTip.isOpaque()) {
                        opaqueValue += "JToolTip";
                    }
                    if (!viewport.isOpaque()) {
                        opaqueValue += "JViewport";
                    }

                    if (!opaqueValue.equals(" ")) {
                        throw new RuntimeException("" +
                                "setUIProperty cleared the " +opaqueValue +
                                " Opaque when opaque is set by client");
                    }

                });
            } catch (UnsupportedLookAndFeelException e) {
                System.out.println("Note: LookAndFeel " + LF.getClassName()
                        + " is not supported on this configuration");
            }
        }
    }
}
