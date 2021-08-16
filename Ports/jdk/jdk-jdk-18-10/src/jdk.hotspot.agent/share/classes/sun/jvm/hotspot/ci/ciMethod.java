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

package sun.jvm.hotspot.ci;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ciMethod extends ciMetadata {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("ciMethod");
    interpreterThrowoutCountField = new CIntField(type.getCIntegerField("_interpreter_throwout_count"), 0);
    interpreterInvocationCountField = new CIntField(type.getCIntegerField("_interpreter_invocation_count"), 0);
    try {
        // XXX
        instructionsSizeField = new CIntField(type.getCIntegerField("_instructions_size"), 0);
    } catch (Exception e) {
    }
  }

  private static CIntField interpreterThrowoutCountField;
  private static CIntField interpreterInvocationCountField;
  private static CIntField instructionsSizeField;

  public ciMethod(Address addr) {
    super(addr);
  }

  public Method method() {
    return (Method)getMetadata();
  }

  public int interpreterThrowoutCount() {
    return (int) interpreterThrowoutCountField.getValue(getAddress());
  }

  public int interpreterInvocationCount() {
    return (int) interpreterInvocationCountField.getValue(getAddress());
  }

  public int instructionsSize() {
    if (instructionsSizeField == null) {
      // XXX
      Method method = method();
      NMethod nm = method.getNativeMethod();
      if (nm != null) return (int)nm.codeEnd().minus(nm.getVerifiedEntryPoint());
      return 0;
    }
    return (int) instructionsSizeField.getValue(getAddress());
  }

  public void printShortName(PrintStream st) {
    Method method = method();
    st.printf(" %s::%s", method.getMethodHolder().getName().asString().replace('/', '.'),
              method.getName().asString());
  }

  public void dumpReplayData(PrintStream out) {
    Method method = (Method)getMetadata();
    NMethod nm = method.getNativeMethod();
    out.println("ciMethod " +
                nameAsAscii() + " " +
                method.getInvocationCount() + " " +
                method.getBackedgeCount() + " " +
                interpreterInvocationCount() + " " +
                interpreterThrowoutCount() + " " +
                instructionsSize());
  }

  public void printValueOn(PrintStream tty) {
    tty.print("ciMethod " + method().getName().asString() + method().getSignature().asString() + "@" + getAddress());
  }

  public String nameAsAscii() {
    Method method = (Method)getMetadata();
    return method.nameAsAscii();
  }
}
