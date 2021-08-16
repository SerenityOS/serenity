/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.interpreter;

import java.util.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.*;

class OopMapForCacheEntry extends GenerateOopMap {
  private OopMapCacheEntry entry;
  private int              bci;
  private int              stackTop;

  OopMapForCacheEntry(Method method, int bci, OopMapCacheEntry entry) {
    super(method);
    this.entry = entry;
    this.bci = bci;
    this.stackTop = -1;
  }

  public boolean reportResults() { return false; }

  public boolean possibleGCPoint(BytecodeStream bcs) {
    return false; // We are not reporting any result. We call resultForBasicblock directly
  }

  public void fillStackmapProlog(int nof_gc_points) {
    // Do nothing
  }

  public void fillStackmapEpilog() {
    // Do nothing
  }

  public void fillStackmapForOpcodes(BytecodeStream bcs,
                                     CellTypeStateList vars,
                                     CellTypeStateList stack,
                                     int stackTop) {
    // Only interested in one specific bci
    if (bcs.bci() == bci) {
      entry.setMask(vars, stack, stackTop);
      this.stackTop = stackTop;
    }
  }

  public void fillInitVars(List<Integer> initVars) {
    // Do nothing
  }

  public void computeMap() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!method().isNative(), "cannot compute oop map for native methods");
    }
    // First check if it is a method where the stackmap is always empty
    if (method().getCodeSize() == 0 || method().getMaxLocals() + method().getMaxStack() == 0) {
      entry.setEmptyMask();
    } else {
      super.computeMap();
      resultForBasicblock(bci);
    }
  }

  public int size() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(stackTop != -1, "computeMap must be called first");
    }
    return (int) ((method().isStatic() ? 0 : 1) + method().getMaxLocals() + stackTop);
  }
}
