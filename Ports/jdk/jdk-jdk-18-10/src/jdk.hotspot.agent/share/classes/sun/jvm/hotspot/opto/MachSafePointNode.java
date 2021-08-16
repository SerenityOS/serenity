/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.opto;

import java.util.*;
import java.io.PrintStream;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class MachSafePointNode extends MachReturnNode {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("MachSafePointNode");
    jvmsField = type.getAddressField("_jvms");
    jvmadjField = new CIntField(type.getCIntegerField("_jvmadj"), 0);
  }

  private static AddressField jvmsField;
  private static CIntField jvmadjField;

  public MachSafePointNode(Address addr) {
    super(addr);
  }

  public JVMState jvms() {
    return JVMState.create(jvmsField.getValue(getAddress()));
  }

  public void dumpSpec(PrintStream out) {
    try {
      JVMState jvms = jvms();
      if (jvms != null) out.print(" !");
      if (jvms == null) out.print("empty jvms");
      while (jvms != null) {
        Method m = jvms.method().method();
        int bci = jvms.bci();
        out.print(" " + m.getMethodHolder().getName().asString().replace('/', '.') + "::" + m.getName().asString() + " @ bci:" + bci);
        jvms = jvms.caller();
      }
    } catch (Exception e) {
      out.print(e);
    }
  }
}
