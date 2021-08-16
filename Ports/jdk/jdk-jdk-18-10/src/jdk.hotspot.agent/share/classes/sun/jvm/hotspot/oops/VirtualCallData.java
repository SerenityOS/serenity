/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

// VirtualCallData
//
// A VirtualCallData is used to access profiling information about a
// call.  For now, it has nothing more than a ReceiverTypeData.
public class VirtualCallData<K,M> extends ReceiverTypeData<K,M> {
  public VirtualCallData(MethodDataInterface<K,M> methodData, DataLayout layout) {
    super(methodData, layout);
    //assert(layout.tag() == DataLayout.virtualCallDataTag, "wrong type");
  }

  static int staticCellCount() {
    // At this point we could add more profile state, e.g., for arguments.
    // But for now it's the same size as the base record type.
    int cellCount = ReceiverTypeData.staticCellCount();
    return cellCount;
  }

  public int cellCount() {
    return staticCellCount();
  }

  // Direct accessors
  static int virtualCallDataSize() {
    return cellOffset(staticCellCount());
  }

  public void printDataOn(PrintStream st) {
    printShared(st, "VirtualCallData");
    printReceiverDataOn(st);
  }
}
