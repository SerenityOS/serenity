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

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/*
 * @test
 * @bug 8013571
 * @summary Tests null as a root of TreeModelEvent
 * @author Sergey Malenkov
 */

public class Test8013571 extends DefaultTreeModel {
    public static void main(String[] args) {
        DefaultMutableTreeNode root = create("root");
        root.add(create("colors", "blue", "violet", "red", "yellow"));
        root.add(create("sports", "basketball", "soccer", "football", "hockey"));
        root.add(create("food", "hot dogs", "pizza", "ravioli", "bananas"));
        Test8013571 model = new Test8013571(root);
        JTree tree = new JTree(model);
        model.fireTreeChanged(tree);
    }

    private static DefaultMutableTreeNode create(String name, String... values) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        for (String value : values) {
            node.add(create(value));
        }
        return node;
    }

    private Test8013571(DefaultMutableTreeNode root) {
        super(root);
    }

    private void fireTreeChanged(Object source) {
        fireTreeNodesInserted(source, null, null, null);
        fireTreeNodesChanged(source, null, null, null);
        fireTreeNodesRemoved(source, null, null, null);
        fireTreeStructureChanged(source, null, null, null);
    }
}
