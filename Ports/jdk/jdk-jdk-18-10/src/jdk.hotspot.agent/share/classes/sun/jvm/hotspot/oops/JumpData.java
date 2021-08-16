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

// JumpData
//
// A JumpData is used to access profiling information for a direct
// branch.  It is a counter, used for counting the number of branches,
// plus a data displacement, used for realigning the data pointer to
// the corresponding target bci.
public class JumpData extends ProfileData {
  static final int   takenOffSet = 0;
  static final int     displacementOffSet = 1;
  static final int     jumpCellCount = 2;

  public JumpData(DataLayout layout) {
    super(layout);
    //assert(layout.tag() == DataLayout.jumpDataTag ||
    //       layout.tag() == DataLayout.branchDataTag, "wrong type");
  }

  static int staticCellCount() {
    return jumpCellCount;
  }

  public int cellCount() {
    return staticCellCount();
  }

  // Direct accessor
  int taken() {
    return uintAt(takenOffSet);
  }

  int displacement() {
    return intAt(displacementOffSet);
  }

  // Code generation support
  static int takenOffset() {
    return cellOffset(takenOffSet);
  }

  static int displacementOffset() {
    return cellOffset(displacementOffSet);
  }

  public void printDataOn(PrintStream st) {
    printShared(st, "JumpData");
    st.println("taken(" + taken() + ") displacement(" + displacement() + ")");
  }
}
