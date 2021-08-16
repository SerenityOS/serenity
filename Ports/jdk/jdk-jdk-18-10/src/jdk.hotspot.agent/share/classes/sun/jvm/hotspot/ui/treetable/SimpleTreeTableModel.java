/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui.treetable;

import javax.swing.tree.*;
import sun.jvm.hotspot.ui.tree.*;

/** An extension of SimpleTreeModel which implements the
    TreeTableModel interface. It supports a two-column "Name, Value"
    interface and disabling of editing of the "Value" column. Because
    of a bug in the implementation of JTreeTable, it always returns
    "true" for isCellEditable of cells in the "Name" column;
    otherwise, mouse clicks to open handles are not dispatched
    properly. Users are responsible for calling setTreeEditable(false)
    on the JTreeTable to make the names uneditable. */

public class SimpleTreeTableModel extends SimpleTreeModel implements TreeTableModel {
  private boolean valuesEditable = true;

  public int getColumnCount() {
    return 2;
  }
  public String getColumnName(int column) {
    switch (column) {
    case 0: return "Name";
    case 1: return "Value";
    default: throw new RuntimeException("Index " + column + " out of bounds");
    }
  }
  public Class getColumnClass(int column) {
    switch (column) {
    case 0: return TreeTableModel.class;
    case 1: return String.class;
    default: throw new RuntimeException("Index " + column + " out of bounds");
    }
  }
  public Object getValueAt(Object node, int column) {
    SimpleTreeNode realNode = (SimpleTreeNode) node;
    switch (column) {
    case 0: return realNode.getName();
    case 1: return realNode.getValue();
    default: throw new RuntimeException("Index " + column + " out of bounds");
    }
  }
  public boolean isCellEditable(Object node, int column) {
    switch (column) {
    // This must return true in order for the JTreeTable's handles to
    // work properly
    case 0: return true;
    case 1: return valuesEditable;
    default: throw new RuntimeException("Index " + column + " out of bounds");
    }
  }
  public void setValueAt(Object aValue, Object node, int column) {
    // FIXME: figure out how to handle this
    throw new RuntimeException("FIXME: figure out how to handle editing of SimpleTreeNodes");
  }

  /** Defaults to true */
  public boolean getValuesEditable() {
    return valuesEditable;
  }

  /** Defaults to true */
  public void setValuesEditable(boolean val) {
    valuesEditable = val;
  }
}
