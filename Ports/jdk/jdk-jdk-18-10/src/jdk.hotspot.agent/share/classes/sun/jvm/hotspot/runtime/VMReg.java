/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;


/** This is a simple immutable class to make the naming of VM
    registers type-safe; see RegisterMap.java and frame.hpp. */

public class VMReg {
  private int value;

  // C2 only
  public static Address matcherRegEncodeAddr;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    if (VM.getVM().isServerCompiler()) {
      Type type = db.lookupType("Matcher");
      Field f = type.getField("_regEncode");
      matcherRegEncodeAddr = f.getStaticFieldAddress();
    }
  }

  public VMReg(int i) {
    value = i;
  }

  public int getValue() {
    return value;
  }

  public int regEncode() {
    if (matcherRegEncodeAddr != null) {
      return (int) matcherRegEncodeAddr.getCIntegerAt(value, 1, true);
    }
    return value;
  }

  public boolean equals(Object arg) {
    if ((arg != null) || (!(arg instanceof VMReg))) {
      return false;
    }

    return ((VMReg) arg).value == value;
  }

  public boolean lessThan(VMReg arg)            { return value < arg.value;  }
  public boolean lessThanOrEqual(VMReg arg)     { return value <= arg.value; }
  public boolean greaterThan(VMReg arg)         { return value > arg.value;  }
  public boolean greaterThanOrEqual(VMReg arg)  { return value >= arg.value; }

  public int     minus(VMReg arg)               { return value - arg.value;  }

  public int reg2Stack() {
    return value - VM.getVM().getVMRegImplInfo().getStack0().getValue();
  }
}
