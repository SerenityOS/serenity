/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

public class BasicFunctionSym extends BasicBlockSym implements FunctionSym {
  private Type         type;
  private boolean      isModuleLocal;

  public BasicFunctionSym(BlockSym parent, long length, Address addr, String name,
                          Type type, boolean isModuleLocal) {
    super(parent, length, addr, name);
    this.type          = type;
    this.isModuleLocal = isModuleLocal;
  }

  public FunctionSym  asFunction()    { return this; }

  public Type         getType()       { return type; }
  public boolean      isModuleLocal() { return isModuleLocal; }

  public void resolve(BasicCDebugInfoDataBase db, ResolveListener listener) {
    super.resolve(db, listener);
    type = db.resolveType(this, type, listener, "resolving type of function symbol");
  }

  public String toString() {
    if (getName() == null) {
      return null;
    }

    StringBuilder res = new StringBuilder();
    res.append(getName());
    res.append("(");
    FunctionType type = (FunctionType) getType();
    if (type != null) {
      int nargs = type.getNumArguments();
      for (int i = 0; i < nargs; i++) {
        res.append(type.getArgumentType(i).toString());
        if (i != nargs - 1) {
          res.append(", ");
        }
      }
    }
    res.append(")");
    return res.toString();
  }
}
