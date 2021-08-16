/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

// BitData
//
// A BitData holds a flag or two in its header.
public class BitData extends ProfileData {

  // nullSeen:
  //  saw a null operand (cast/aastore/instanceof)
  static final int nullSeenFlag              = DataLayout.firstFlag + 0;
  static final int bitCellCount = 0;

  public BitData(DataLayout layout) {
    super(layout);
  }

  static int staticCellCount() {
    return bitCellCount;
  }

  public int cellCount() {
    return staticCellCount();
  }

  // Accessor

  // The nullSeen flag bit is specially known to the interpreter.
  // Consulting it allows the compiler to avoid setting up nullCheck traps.
  boolean nullSeen()     { return flagAt(nullSeenFlag); }

  // Code generation support
  // static int nullSeenByteConstant() {
  //   return flagNumberToByteConstant(nullSeenFlag);
  // }

  static int bitDataSize() {
    return cellOffset(bitCellCount);
  }

  public void printDataOn(PrintStream st) {
    printShared(st, "BitData");
  }
}
