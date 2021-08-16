/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui.tree;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/** A very simple tree model which allows the root to be set, so we
    can reuse the same model for various types of data; the
    specialization is contained within the nodes. This tree model
    operates on SimpleTreeNodes. */

public class SimpleTreeModel implements TreeModel {
  private static final SimpleTreeNode singletonNullRoot = new SimpleTreeNode() {
      public int getChildCount()                        { return 0;      }
      public SimpleTreeNode getChild(int index)         { return null;   }
      public boolean isLeaf()                           { return true;   }
      public int getIndexOfChild(SimpleTreeNode child)  { return 0;      }
      public String toString()                          { return ""; }
      public String getName()                           { return toString(); }
      public String getValue()                          { return toString(); }
    };
  private SimpleTreeNode root = singletonNullRoot;
  private List<TreeModelListener> listeners = new ArrayList<>();

  public void setRoot(SimpleTreeNode node) {
    if (node != null) {
      root = node;
    } else {
      root = singletonNullRoot;
    }
    fireTreeStructureChanged();
  }

  public Object getRoot() {
    return root;
  }

  public Object getChild(Object parent, int index) {
    return ((SimpleTreeNode) parent).getChild(index);
  }

  public int getChildCount(Object parent) {
    return ((SimpleTreeNode) parent).getChildCount();
  }

  public boolean isLeaf(Object node) {
    if (node == null) {
      return true;
    }
    return ((SimpleTreeNode) node).isLeaf();
  }

  /** Unsupported operation */
  public void valueForPathChanged(TreePath path, Object newValue) {
    throw new UnsupportedOperationException();
  }

  public int getIndexOfChild(Object parent, Object child) {
    return ((SimpleTreeNode) parent).getIndexOfChild((SimpleTreeNode) child);
  }

  public void addTreeModelListener(TreeModelListener l) {
    listeners.add(l);
  }

  public void removeTreeModelListener(TreeModelListener l) {
    listeners.remove(l);
  }

  public void fireTreeStructureChanged() {
    TreeModelEvent e = new TreeModelEvent(getRoot(), new Object[] { getRoot() }, null, null);
    for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
      TreeModelListener l = (TreeModelListener) iter.next();
      l.treeStructureChanged(e);
    }
  }
}
