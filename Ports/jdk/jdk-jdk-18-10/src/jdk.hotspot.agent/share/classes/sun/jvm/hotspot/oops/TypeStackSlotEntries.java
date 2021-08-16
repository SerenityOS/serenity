/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

// Type entries used for arguments passed at a call and parameters on
// method entry. 2 cells per entry: one for the type encoded as in
// TypeEntries and one initialized with the stack slot where the
// profiled object is to be found so that the interpreter can locate
// it quickly.
public class TypeStackSlotEntries<K,M> extends TypeEntries<K,M> {
  static final int stackSlotEntry = 0;
  static final int typeEntry = 1;
  static final int perArgCellCount = 2;

  int stackSlotOffset(int i) {
    return baseOff + stackSlotLocalOffset(i);
  }

  final int numberOfEntries;

  int typeOffsetInCells(int i) {
    return baseOff + typeLocalOffset(i);
  }

  TypeStackSlotEntries(MethodDataInterface<K,M> methodData, ProfileData pd, int baseOff, int nbEntries) {
    super(methodData, pd, baseOff);
    numberOfEntries = nbEntries;
  }

  static int stackSlotLocalOffset(int i) {
    return i * perArgCellCount + stackSlotEntry;
  }

  static int typeLocalOffset(int i) {
    return i * perArgCellCount + typeEntry;
  }

  int stackSlot(int i) {
    return pd.uintAt(stackSlotOffset(i));
  }

  K type(int i) {
    return validKlass(typeOffsetInCells(i));
  }

  static int perArgCount() {
    return perArgCellCount;
  }

  int typeIndex(int i) {
    return typeOffsetInCells(i);
  }

  void printDataOn(PrintStream st) {
    for (int i = 0; i < numberOfEntries; i++) {
      pd.tab(st);
      st.print(i + ": stack(" + stackSlot(i)+ ") ");
      printKlass(st, typeOffsetInCells(i));
      st.println();
    }
  }
}
