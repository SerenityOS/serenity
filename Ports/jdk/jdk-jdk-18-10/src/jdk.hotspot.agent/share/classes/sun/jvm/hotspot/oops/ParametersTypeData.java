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

// ParametersTypeData
//
// A ParametersTypeData is used to access profiling information about
// types of parameters to a method
public class ParametersTypeData<K,M> extends ArrayData {
  final TypeStackSlotEntries<K,M> parameters;

  static int stackSlotLocalOffset(int i) {
    return arrayStartOffSet + TypeStackSlotEntries.stackSlotLocalOffset(i);
  }

  static int typeLocalOffset(int i) {
    return arrayStartOffSet + TypeStackSlotEntries.typeLocalOffset(i);
  }

  public ParametersTypeData(MethodDataInterface<K,M> methodData, DataLayout layout) {
    super(layout);
    parameters = new TypeStackSlotEntries<K,M>(methodData, this, 1, numberOfParameters());
  }

  public int numberOfParameters() {
    return arrayLen() / TypeStackSlotEntries.perArgCount();
  }

  int stackSlot(int i) {
    return parameters.stackSlot(i);
  }

  public K type(int i) {
    return parameters.type(i);
  }

  static public int typeIndex(int i) {
    return typeLocalOffset(i);
  }

  public void printDataOn(PrintStream st) {
    st.print("parameter types");
    parameters.printDataOn(st);
  }
}
