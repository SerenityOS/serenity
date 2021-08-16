/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4631471 6972468
 * @summary Tests DefaultTreeModel encoding
 * @run main/othervm -Djava.security.manager=allow Test4631471
 * @author Sergey Malenkov, Mark Davidson
 */

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

public abstract class Test4631471 extends AbstractTest {
    public static void main(String[] args) throws Exception {
        main();
        System.setSecurityManager(new SecurityManager());
        main();
    }

    private static void main() throws Exception {
        // the DefaultMutableTreeNode will archive correctly
        new Test4631471() {
            protected Object getObject() {
                return getRoot();
            }
        }.test(false);

        // the DefaultTreeModel will also archive correctly
        new Test4631471() {
            protected Object getObject() {
                return getModel();
            }
        }.test(false);

        // create a new model from the root node
        // this simulates the the MetaData ctor:
        // registerConstructor("javax.swing.tree.DefaultTreeModel", new String[]{"root"});
        new Test4631471() {
            protected Object getObject() {
                return new DefaultTreeModel((TreeNode) getModel().getRoot());
            }
        }.test(false);

        // the JTree will archive correctly too
        new Test4631471() {
            protected Object getObject() {
                return getTree();
            }
        }.test(false);
    }

    protected final void validate(Object before, Object after) {
        // do not any validation
    }

    public static TreeNode getRoot() {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode first = new DefaultMutableTreeNode("first");
        DefaultMutableTreeNode second = new DefaultMutableTreeNode("second");
        DefaultMutableTreeNode third = new DefaultMutableTreeNode("third");

        first.add(new DefaultMutableTreeNode("1.1"));
        first.add(new DefaultMutableTreeNode("1.2"));
        first.add(new DefaultMutableTreeNode("1.3"));

        second.add(new DefaultMutableTreeNode("2.1"));
        second.add(new DefaultMutableTreeNode("2.2"));
        second.add(new DefaultMutableTreeNode("2.3"));

        third.add(new DefaultMutableTreeNode("3.1"));
        third.add(new DefaultMutableTreeNode("3.2"));
        third.add(new DefaultMutableTreeNode("3.3"));

        node.add(first);
        node.add(second);
        node.add(third);

        return node;
    }

    public static JTree getTree() {
        return new JTree(getRoot());
    }

    public static TreeModel getModel() {
        return getTree().getModel();
    }
}
