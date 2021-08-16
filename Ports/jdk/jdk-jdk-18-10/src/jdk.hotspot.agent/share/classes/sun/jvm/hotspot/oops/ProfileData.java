/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

public abstract class ProfileData {
  // This is a pointer to a section of profiling data.
  private DataLayout _data;

  public DataLayout data() { return _data; }

  // How many cells are in this?
  public abstract int cellCount();


  // Return the size of this data.
  public int sizeInBytes() {
    return DataLayout.computeSizeInBytes(cellCount());
  }

  public int dp() {
    return data().dp();
  }

  // Low-level accessors for underlying data
  long intptrAt(int index) {
    //assert(0 <= index && index < cellCount(), "oob");
    return data().cellAt(index);
  }
  int intAt(int index) {
    return (int)intptrAt(index);
  }
  int uintAt(int index) {
    return (int)intptrAt(index);
  }
  public Address addressAt(int index) {
    return data().addressAt(index);
  }

  boolean flagAt(int flagNumber) {
    return data().flagAt(flagNumber);
  }

  // two convenient imports for use by subclasses:
  public static int cellOffset(int index) {
    return DataLayout.cellOffset(index);
  }

  public ProfileData(DataLayout data) {
    _data = data;
  }

  // Constructor for invalid ProfileData.
  ProfileData() {
    _data = null;
  }

  int bci() {
    return data().bci();
  }

  int trapState() {
    return data().trapState();
  }
  public abstract void printDataOn(PrintStream st);

  void tab(PrintStream st) {
    st.print("\t");
  }

  void printShared(PrintStream st, String name) {
    st.print("bci: " + bci());
    // st.fillTo(tabWidthOne);
    st.print(" " +  name + " ");
    tab(st);
    int trap = trapState();
    if (trap != 0) {
      st.print("trap(" + MethodData.formatTrapState(trap) + ") ");
    }
    int flags = data().flags();
    if (flags != 0)
      st.print("flags(" + flags + ") ");
  }

  public String toString() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    try {
      printDataOn(ps);
    } finally {
      ps.close();
    }
    return baos.toString();
  }
}
