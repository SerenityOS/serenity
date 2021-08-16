/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.proc;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.utilities.*;

class ProcCDebugger implements CDebugger {
  private ProcDebugger dbg;

  ProcCDebugger(ProcDebugger dbg) {
    this.dbg = dbg;
  }

  public List<ThreadProxy> getThreadList() throws DebuggerException {
    return dbg.getThreadList();
  }

  public List<LoadObject> getLoadObjectList() throws DebuggerException {
    return dbg.getLoadObjectList();
  }

  public LoadObject loadObjectContainingPC(Address pc) throws DebuggerException {
    if (pc == null) {
      return null;
    }
    List<LoadObject> objs = getLoadObjectList();
    Object[] arr = objs.toArray();
    // load objects are sorted by base address, do binary search
    int mid  = -1;
    int low  = 0;
    int high = arr.length - 1;

    while (low <= high) {
       mid = (low + high) >> 1;
       LoadObject midVal = (LoadObject) arr[mid];
       long cmp = pc.minus(midVal.getBase());
       if (cmp < 0) {
          high = mid - 1;
       } else if (cmp > 0) {
          long size = midVal.getSize();
          if (cmp >= size) {
             low = mid + 1;
          } else {
             return (LoadObject) arr[mid];
          }
       } else { // match found
          return (LoadObject) arr[mid];
       }
    }
    // no match found.
    return null;
  }

  public CFrame topFrameForThread(ThreadProxy thread) throws DebuggerException {
    return dbg.topFrameForThread(thread);
  }

  public String getNameOfFile(String fileName) {
    return new File(fileName).getName();
  }

  public ProcessControl getProcessControl() throws DebuggerException {
    // FIXME: after stabs parser
    return null;
  }

  // C++ name demangling
  public boolean canDemangle() {
    return true;
  }

  public String demangle(String sym) {
    return dbg.demangle(sym);
  }
}
