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

public class DataLayout {
  public static final int noTag = 0;
  public static final int bitDataTag = 1;
  public static final int counterDataTag = 2;
  public static final int jumpDataTag= 3;
  public static final int receiverTypeDataTag = 4;
  public static final int virtualCallDataTag = 5;
  public static final int retDataTag = 6;
  public static final int branchDataTag = 7;
  public static final int multiBranchDataTag = 8;
  public static final int argInfoDataTag = 9;
  public static final int callTypeDataTag = 10;
  public static final int virtualCallTypeDataTag = 11;
  public static final int parametersTypeDataTag = 12;
  public static final int speculativeTrapDataTag = 13;

  // The trap state breaks down as [recompile:1 | reason:31].
  // This further breakdown is defined in deoptimization.cpp.
  // See Deoptimization.trapStateReason for an assert that
  // trapBits is big enough to hold reasons < reasonRecordedLimit.
  //
  // The trapState is collected only if ProfileTraps is true.
  public static final int trapBits = 1+31;  // 31: enough to distinguish [0..reasonRecordedLimit].
  public static final int trapMask = Bits.rightNBits(trapBits);
  public static final int firstFlag = 0;

  private Address data;

  private int offset;

  public DataLayout(MethodData d, int o) {
    data = d.getAddress();
    offset = o;
  }

  public DataLayout(Address d, int o) {
    data = d;
    offset = o;
  }

  public int dp() { return offset; }

  private int getU11(int at) {
    return data.getJByteAt(offset + at) & 0xff;
  }

  private int getU22(int at) {
    return data.getJShortAt(offset + at) & 0xffff;
  }

  long cellAt(int index) {
    return data.getCIntegerAt(offset + cellOffset(index), MethodData.cellSize, false);
  }

  public Address addressAt(int index) {
    return data.getAddressAt(offset + cellOffset(index));
  }

  // Every data layout begins with a header.  This header
  // contains a tag, which is used to indicate the size/layout
  // of the data, 8 bits of flags, which can be used in any way,
  // 32 bits of trap history (none/one reason/many reasons),
  // and a bci, which is used to tie this piece of data to a
  // specific bci in the bytecodes.
  // union {
  //   u8 _bits;
  //   struct {
  //     u1 _tag;
  //     u1 _flags;
  //     u2 _bci;
  //     u4 _traps;
  //   } _struct;
  // } _header;

  // Some types of data layouts need a length field.
  static boolean needsArrayLen(int tag) {
    return (tag == multiBranchDataTag);
  }

  public static final int counterIncrement = 1;

  // Size computation
  static int headerSizeInBytes() {
    return MethodData.cellSize * headerSizeInCells();
  }
  static int headerSizeInCells() {
      return VM.getVM().isLP64() ? 1 : 2;
  }

  static public int computeSizeInBytes(int cellCount) {
    return headerSizeInBytes() + cellCount * MethodData.cellSize;
  }

  // Initialization
  // void initialize(int tag, int bci, int cellCount);

  // Accessors
  public int tag() {
    return getU11(0);
  }

  // Return a few bits of trap state.  Range is [0..trapMask].
  // The state tells if traps with zero, one, or many reasons have occurred.
  // It also tells whether zero or many recompilations have occurred.
  // The associated trap histogram in the MDO itself tells whether
  // traps are common or not.  If a BCI shows that a trap X has
  // occurred, and the MDO shows N occurrences of X, we make the
  // simplifying assumption that all N occurrences can be blamed
  // on that BCI.
  int trapState() {
    return data.getJIntAt(offset+4);
  }

  int flags() {
    return getU11(1);
  }

  int bci() {
    return getU22(2);
  }

  boolean flagAt(int flagNumber) {
    // assert(flagNumber < flagLimit, "oob");
    return (flags() & (0x1 << flagNumber)) != 0;
  }

  // Low-level support for code generation.
  static int headerOffset() {
    return 0;
  }
  static int tagOffset() {
    return 0;
  }
  static int flagsOffset() {
    return 1;
  }
  static int bciOffset() {
    return 2;
  }
  public static int cellOffset(int index) {
    return (headerSizeInCells() + index) * MethodData.cellSize;
  }
  // // Return a value which, when or-ed as a byte into _flags, sets the flag.
  // static int flagNumberToByteConstant(int flagNumber) {
  //   assert(0 <= flagNumber && flagNumber < flagLimit, "oob");
  //   DataLayout temp; temp.setHeader(0);
  //   temp.setFlagAt(flagNumber);
  //   return temp._header._struct._flags;
  // }
  // // Return a value which, when or-ed as a word into _header, sets the flag.
  // static intptrT flagMaskToHeaderMask(int byteConstant) {
  //   DataLayout temp; temp.setHeader(0);
  //   temp._header._struct._flags = byteConstant;
  //   return temp._header._bits;
  // }
}
