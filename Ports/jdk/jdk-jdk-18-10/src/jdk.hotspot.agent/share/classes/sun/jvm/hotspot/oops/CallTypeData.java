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

// CallTypeData
//
// A CallTypeData is used to access profiling information about a non
// virtual call for which we collect type information about arguments
// and return value.
public class CallTypeData<K,M> extends CounterData implements CallTypeDataInterface<K> {
  final TypeStackSlotEntries<K,M> args;
  final ReturnTypeEntry<K,M> ret;

  int cellCountGlobalOffset() {
    return CounterData.staticCellCount() + TypeEntriesAtCall.cellCountLocalOffset();
  }

  int cellCountNoHeader() {
    return uintAt(cellCountGlobalOffset());
  }

  public CallTypeData(MethodDataInterface<K,M> methodData, DataLayout layout) {
    super(layout);
    args = new TypeStackSlotEntries<K,M>(methodData, this, CounterData.staticCellCount()+TypeEntriesAtCall.headerCellCount(), numberOfArguments());
    ret = new ReturnTypeEntry<K,M>(methodData, this, cellCount() - ReturnTypeEntry.staticCellCount());
  }

  static int staticCellCount() {
    return -1;
  }

  public int cellCount() {
    return CounterData.staticCellCount() +
      TypeEntriesAtCall.headerCellCount() +
      intAt(cellCountGlobalOffset());
  }

  public int numberOfArguments() {
    return cellCountNoHeader() / TypeStackSlotEntries.perArgCount();
  }

  public boolean hasArguments() {
    return cellCountNoHeader() >= TypeStackSlotEntries.perArgCount();
  }

  public K argumentType(int i) {
    return args.type(i);
  }

  public boolean hasReturn() {
    return (cellCountNoHeader() % TypeStackSlotEntries.perArgCount()) != 0;
  }

  public K returnType() {
    return ret.type();
  }

  public int argumentTypeIndex(int i) {
    return args.typeIndex(i);
  }

  public int returnTypeIndex() {
    return ret.typeIndex();
  }

  public void printDataOn(PrintStream st) {
    super.printDataOn(st);
    if (hasArguments()) {
      tab(st);
      st.print("argument types");
      args.printDataOn(st);
    }
    if (hasReturn()) {
      tab(st);
      st.print("return type");
      ret.printDataOn(st);
    }
  }
}
