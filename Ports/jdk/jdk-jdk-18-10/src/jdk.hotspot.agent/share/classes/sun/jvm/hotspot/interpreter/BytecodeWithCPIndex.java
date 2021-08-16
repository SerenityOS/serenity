/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

// any bytecode with constant pool index

public abstract class BytecodeWithCPIndex extends Bytecode {
  BytecodeWithCPIndex(Method method, int bci) {
    super(method, bci);
  }

  // the constant pool index for this bytecode
  public int index() {
    if (code() == Bytecodes._invokedynamic) {
      int index = getIndexU4();
      if (ConstantPool.isInvokedynamicIndex(index)) {
        return ConstantPool.decodeInvokedynamicIndex(index);
      } else {
        return index;
      }
    } else {
      return getIndexU2(code(), false);
    }
  }

  protected int indexForFieldOrMethod() {
     ConstantPoolCache cpCache = method().getConstants().getCache();
     // get ConstantPool index from ConstantPoolCacheIndex at given bci
     int cpCacheIndex = index();
     if (cpCache == null) {
        return cpCacheIndex;
     } else {
      return cpCache.getEntryAt((int) (0xFFFF & cpCacheIndex)).getConstantPoolIndex();
     }
  }
}
