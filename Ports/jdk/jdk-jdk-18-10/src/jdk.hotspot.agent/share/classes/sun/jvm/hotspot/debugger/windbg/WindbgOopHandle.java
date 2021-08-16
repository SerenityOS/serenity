/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.windbg;

import sun.jvm.hotspot.debugger.*;

class WindbgOopHandle extends WindbgAddress implements OopHandle {
  WindbgOopHandle(WindbgDebugger debugger, long addr) {
    super(debugger, addr);
  }

  public boolean equals(Object arg) {
    if (arg == null) {
      return false;
    }

    if (!(arg instanceof WindbgOopHandle)) {
      return false;
    }

    return (addr == ((WindbgAddress) arg).addr);
  }

  public Address    addOffsetTo       (long offset) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("addOffsetTo not applicable to OopHandles (interior object pointers not allowed)");
  }

  public Address    andWithMask(long mask) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("andWithMask not applicable to OopHandles (i.e., anything but C addresses)");
  }

  public Address    orWithMask(long mask) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("orWithMask not applicable to OopHandles (i.e., anything but C addresses)");
  }

  public Address    xorWithMask(long mask) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("xorWithMask not applicable to OopHandles (i.e., anything but C addresses)");
  }
}
