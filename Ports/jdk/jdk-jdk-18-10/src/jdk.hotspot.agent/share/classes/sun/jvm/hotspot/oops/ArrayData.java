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

// ArrayData
//
// A ArrayData is a base class for accessing profiling data which does
// not have a statically known size.  It consists of an array length
// and an array start.
abstract class ArrayData extends ProfileData {

  static final int arrayLenOffSet = 0;
  static final int arrayStartOffSet = 1;

  int arrayUintAt(int index) {
    int aindex = index + arrayStartOffSet;
    return uintAt(aindex);
  }
  int arrayIntAt(int index) {
    int aindex = index + arrayStartOffSet;
    return intAt(aindex);
  }

  // Code generation support for subclasses.
  static int arrayElementOffset(int index) {
    return cellOffset(arrayStartOffSet + index);
  }

  ArrayData(DataLayout layout) {
    super(layout);
  }

  static int staticCellCount() {
    return -1;
  }

  int arrayLen() {
    return intAt(arrayLenOffSet);
  }

  public int cellCount() {
    return arrayLen() + 1;
  }

  // Code generation support
  static int arrayLenOffset() {
    return cellOffset(arrayLenOffSet);
  }
  static int arrayStartOffset() {
    return cellOffset(arrayStartOffSet);
  }

}
