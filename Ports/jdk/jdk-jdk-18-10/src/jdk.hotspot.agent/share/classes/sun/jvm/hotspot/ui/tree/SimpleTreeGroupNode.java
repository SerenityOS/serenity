/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

/** Node to which others can be added; suitable for use as root node
    of graph */

public class SimpleTreeGroupNode implements SimpleTreeNode {
  private List<SimpleTreeNode> children;

  public SimpleTreeGroupNode() {
    children = new ArrayList<>();
  }

  public int getChildCount() { return children.size(); }
  public SimpleTreeNode getChild(int index) {
    return (SimpleTreeNode) children.get(index);
  }
  public void addChild(SimpleTreeNode child) {
    children.add(child);
  }
  public SimpleTreeNode removeChild(int index) {
    return (SimpleTreeNode) children.remove(index);
  }
  public void removeAllChildren() {
    children.clear();
  }
  public boolean isLeaf() {
    return false;
  }
  public int getIndexOfChild(SimpleTreeNode child) {
    return children.indexOf(child);
  }

  public String getName()  { return null; }
  public String getValue() { return null; }
}
