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

package sun.jvm.hotspot.code;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** PcDescs map a physical PC (given as offset from start of nmethod)
    to the corresponding source scope and byte code index. */

public class PCDesc extends VMObject {
  private static CIntegerField pcOffsetField;
  private static CIntegerField scopeDecodeOffsetField;
  private static CIntegerField objDecodeOffsetField;
  private static CIntegerField pcFlagsField;
  private static int reexecuteMask;
  private static int isMethodHandleInvokeMask;
  private static int returnOopMask;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    Type type = db.lookupType("PcDesc");

    pcOffsetField          = type.getCIntegerField("_pc_offset");
    scopeDecodeOffsetField = type.getCIntegerField("_scope_decode_offset");
    objDecodeOffsetField   = type.getCIntegerField("_obj_decode_offset");
    pcFlagsField           = type.getCIntegerField("_flags");

    reexecuteMask            = db.lookupIntConstant("PcDesc::PCDESC_reexecute");
    isMethodHandleInvokeMask = db.lookupIntConstant("PcDesc::PCDESC_is_method_handle_invoke");
    returnOopMask            = db.lookupIntConstant("PcDesc::PCDESC_return_oop");
  }

  public PCDesc(Address addr) {
    super(addr);
  }

  // FIXME: add additional constructor probably needed for ScopeDesc::sender()

  public int getPCOffset() {
    return (int) pcOffsetField.getValue(addr);
  }

  public int getScopeDecodeOffset() {
    return ((int) scopeDecodeOffsetField.getValue(addr));
  }

  public int getObjDecodeOffset() {
    return ((int) objDecodeOffsetField.getValue(addr));
  }

  public Address getRealPC(NMethod code) {
    return code.codeBegin().addOffsetTo(getPCOffset());
  }


  public boolean getReexecute() {
    int flags = (int)pcFlagsField.getValue(addr);
    return (flags & reexecuteMask) != 0;
  }

  public boolean isMethodHandleInvoke() {
    int flags = (int)pcFlagsField.getValue(addr);
    return (flags & isMethodHandleInvokeMask) != 0;
  }

  public void print(NMethod code) {
    printOn(System.out, code);
  }

  public void printOn(PrintStream tty, NMethod code) {
    tty.println("PCDesc(" + getRealPC(code) + "):");
    for (ScopeDesc sd = code.getScopeDescAt(getRealPC(code));
         sd != null;
         sd = sd.sender()) {
      tty.print(" ");
      sd.getMethod().printValueOn(tty);
      tty.print("  @" + sd.getBCI());
      tty.print("  reexecute=" + sd.getReexecute());
      tty.println();
    }
  }
}
