/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 6567433
 *
 * @summary  JTree.updateUI() invokes updateUI() on its TreeCellrenderer via
 * SwingUtilities.updateRendererOrEditorUI().
 * If the TreeCellrenderer is a parent of this JTree, the method recurses
 * endless.
 * This test tests that the fix is effective in avoiding recursion.
 *
 * @run main/othervm UpdateUIRecursionTest
 */

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;

public class UpdateUIRecursionTest extends JFrame implements TreeCellRenderer {
    JTree tree;
    DefaultTreeCellRenderer renderer;

    public UpdateUIRecursionTest() {
        super("UpdateUIRecursionTest");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);

        String[] listData = {
            "First", "Second", "Third", "Fourth", "Fifth", "Sixth"
        };

        tree = new JTree(listData);
        renderer = new DefaultTreeCellRenderer();
        getContentPane().add(new JScrollPane(tree), BorderLayout.CENTER);
        tree.setCellRenderer(this);

        setVisible(true);
    }

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                UpdateUIRecursionTest obj = new UpdateUIRecursionTest();

                obj.test();

                obj.disposeUI();
            }
        });
    }

    public void test() {
        tree.updateUI();
    }

    public void disposeUI() {
        setVisible(false);
        dispose();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                              boolean selected, boolean expanded, boolean leaf,
                              int row, boolean hasFocus)
    {
        return renderer.getTreeCellRendererComponent(tree, value, leaf,
                                                expanded, leaf, row, hasFocus);
    }
}
