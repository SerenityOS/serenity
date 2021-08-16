/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.VM;

/** Simple wrapper for displaying bad addresses in the Inspector */

public class BadAddressTreeNodeAdapter extends FieldTreeNodeAdapter {
  private String message;

  private static String generateMessage(long addr, String kind) {
    return "** BAD " + kind + " " + Long.toHexString(addr) + " **";
  }

  public BadAddressTreeNodeAdapter(Address addr, MetadataField field, boolean treeTableMode) {
    super(field.getID(), treeTableMode);
    message = generateMessage(addr.minus(null), "METADATA");
  }

  public BadAddressTreeNodeAdapter(Address addr, OopField field, boolean treeTableMode) {
    super(field.getID(), treeTableMode);
    message = generateMessage(addr.minus(null), "OOP");
  }

  public BadAddressTreeNodeAdapter(OopHandle addr, FieldIdentifier id, boolean treeTableMode) {
    super(id, treeTableMode);
    message = generateMessage(addr.minus(null), "OOP");
  }

  /** The address may be null (for address fields of structures which
      are null); the FieldIdentifier may also be null (for the root
      node). */
  public BadAddressTreeNodeAdapter(long addr, FieldIdentifier id, boolean treeTableMode) {
    super(id, treeTableMode);
    message = generateMessage(addr, "ADDRESS");
  }

  public int getChildCount() {
    return 0;
  }

  public SimpleTreeNode getChild(int index) {
    throw new RuntimeException("Should not call this");
  }

  public boolean isLeaf() {
    return true;
  }

  public int getIndexOfChild(SimpleTreeNode child) {
    throw new RuntimeException("Should not call this");
  }

  public String getValue() {
    return message;
      }
    }
