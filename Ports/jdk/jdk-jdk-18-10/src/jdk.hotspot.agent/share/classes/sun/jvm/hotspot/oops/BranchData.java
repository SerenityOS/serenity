/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

// BranchData
//
// A BranchData is used to access profiling data for a two-way branch.
// It consists of taken and notTaken counts as well as a data displacement
// for the taken case.
public class BranchData extends JumpData {

  static final int notTakenOffSet = jumpCellCount;
  static final int branchCellCount = notTakenOffSet + 1;

  public BranchData(DataLayout layout) {
    super(layout);
    //assert(layout.tag() == DataLayout.branchDataTag, "wrong type");
  }

  static int staticCellCount() {
    return branchCellCount;
  }

  public int cellCount() {
    return staticCellCount();
  }

  // Direct accessor
  int notTaken() {
    return uintAt(notTakenOffSet);
  }

  // Code generation support
  static int notTakenOffset() {
    return cellOffset(notTakenOffSet);
  }
  static int branchDataSize() {
    return cellOffset(branchCellCount);
  }

  public void printDataOn(PrintStream st) {
    printShared(st, "BranchData");
    st.println("taken(" + taken() + ") displacement(" + displacement() + ")");
    tab(st);
    st.println("not taken(" + notTaken() + ")");
  }
}
