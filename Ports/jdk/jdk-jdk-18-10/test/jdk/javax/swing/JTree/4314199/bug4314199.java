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
 * @test
 * @bug 4314199
 * @summary Tests that JTree repaints correctly in a container with a JMenu
 * @author Peter Zhelezniakov
 * @run applet/manual=yesno bug4314199.html
 */

import javax.swing.*;
import javax.swing.tree.*;

public class bug4314199 extends JApplet {

    public void init() {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (final Exception e) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    createAndShowMessage("Test fails because of exception: "
                            + e.getMessage());
                }
            });
        }

    }

    private void createAndShowMessage(String message) {
        getContentPane().add(new JLabel(message));
    }

    private void createAndShowGUI() {
        JMenuBar mb = new JMenuBar();

        // needed to exactly align left edge of menu and angled line of tree
        mb.add(Box.createHorizontalStrut(27));

        JMenu mn = new JMenu("Menu");
        JMenuItem mi = new JMenuItem("MenuItem");
        mn.add(mi);
        mb.add(mn);
        setJMenuBar(mb);

        DefaultMutableTreeNode n1 = new DefaultMutableTreeNode("Root");
        DefaultMutableTreeNode n2 = new DefaultMutableTreeNode("Duke");
        n1.add(n2);
        DefaultMutableTreeNode n3 = new DefaultMutableTreeNode("Bug");
        n2.add(n3);
        n3.add(new DefaultMutableTreeNode("Blah"));
        n3.add(new DefaultMutableTreeNode("Blah"));
        n3.add(new DefaultMutableTreeNode("Blah"));
        DefaultMutableTreeNode n4 = new DefaultMutableTreeNode("Here");
        n2.add(n4);

        JTree tree = new JTree(new DefaultTreeModel(n1));
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.expandPath(new TreePath(new Object[]{n1, n2, n3}));
        setContentPane(tree);
    }
}
