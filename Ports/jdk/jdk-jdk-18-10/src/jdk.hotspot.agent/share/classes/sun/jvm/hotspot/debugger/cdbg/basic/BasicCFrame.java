/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.cdbg.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

/** Basic implementation of the CFrame interface providing some of the
    functionality in a platform-independent manner. */

public abstract class BasicCFrame implements CFrame {
  private CDebugger dbg;

  protected BasicCFrame(CDebugger dbg) {
    this.dbg = dbg;
  }

  protected CDebugger dbg() {
    return dbg;
  }

  public LoadObject loadObjectForPC() {
    return dbg.loadObjectContainingPC(pc());
  }

  public BlockSym blockForPC() {
    LoadObject lo = loadObjectForPC();
    if (lo == null) {
      return null;
    }
    return lo.debugInfoForPC(pc());
  }

  public ClosestSymbol closestSymbolToPC() {
    LoadObject lo = loadObjectForPC();
    if (lo == null) {
      return null;
    }
    return lo.closestSymbolToPC(pc());
  }

  public void iterateLocals(ObjectVisitor v) {
    BlockSym block = blockForPC();
    while (block != null) {
      for (int i = 0; i < block.getNumLocals(); i++) {
        final LocalSym local = block.getLocal(i);
        Type t = local.getType();
        // t should not be null, but be robust in case of bugs in
        // debug info
        if (t != null) {
          t.iterateObject(localVariableBase().addOffsetTo(local.getFrameOffset()),
                          v,
                          new NamedFieldIdentifier() {
                              public Type getType() {
                                return local.getType();
                              }

                              public String getName() {
                                return local.getName();
                              }

                              public String toString() {
                                return getName();
                              }
                            });
        }
      }

      block = block.getParent();
    }
  }
}
