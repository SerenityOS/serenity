/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015926
 * @summary Tests that there are no NPE during painting
 * @author Sergey Malenkov
 */

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class Test8015926 implements TreeModelListener, Runnable, Thread.UncaughtExceptionHandler {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        SwingUtilities.invokeAndWait(new Test8015926());
        Thread.sleep(1000L);
    }

    private JTree tree;

    @Override
    public void treeStructureChanged(TreeModelEvent event) {
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent event) {
    }

    @Override
    public void treeNodesInserted(TreeModelEvent event) {
        this.tree.expandPath(event.getTreePath());
    }

    @Override
    public void treeNodesChanged(TreeModelEvent event) {
    }

    @Override
    public void run() {
        Thread.currentThread().setUncaughtExceptionHandler(this);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("Child");
        DefaultTreeModel model = new DefaultTreeModel(root);

        this.tree = new JTree();
        this.tree.setModel(model);

        JFrame frame = new JFrame(getClass().getSimpleName());
        frame.add(this.tree);

        model.addTreeModelListener(this); // frame is not visible yet
        model.insertNodeInto(child, root, root.getChildCount());
        model.removeNodeFromParent(child);

        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        exception.printStackTrace();
        System.exit(1);
    }
}
