/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6402062 6487891
 * @summary Tests JTree encoding
 * @run main/othervm -Djava.security.manager=allow javax_swing_JTree
 * @author Sergey Malenkov
 */

import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public final class javax_swing_JTree extends AbstractTest<JTree> {
    public static void main(String[] args) {
        new javax_swing_JTree().test(true);
    }

    protected JTree getObject() {
        return new JTree(new MyModel());
    }

    protected JTree getAnotherObject() {
        return new JTree();
    }

    protected void validate(JTree before, JTree after) {
        Class type = after.getModel().getClass();
        if (!type.equals(before.getModel().getClass()))
            throw new Error("Invalid model: " + type);
    }

    public static final class MyModel implements TreeModel {
        public Object getRoot() {
            return null;
        }

        public Object getChild(Object parent, int index) {
            return null;
        }

        public int getChildCount(Object parent) {
            return 0;
        }

        public boolean isLeaf(Object node) {
            return false;
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        public int getIndexOfChild(Object parent, Object child) {
            return 0;
        }

        public void addTreeModelListener(TreeModelListener listener) {
        }

        public void removeTreeModelListener(TreeModelListener listener) {
        }
    }
}
