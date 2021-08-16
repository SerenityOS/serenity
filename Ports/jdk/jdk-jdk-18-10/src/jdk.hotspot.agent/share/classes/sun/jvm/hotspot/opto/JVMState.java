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

import java.io.PrintStream;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.ci.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class JVMState extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("JVMState");
    mapField = type.getAddressField("_map");
    methodField = type.getAddressField("_method");
    bciField = new CIntField(type.getCIntegerField("_bci"), 0);
    spField = new CIntField(type.getCIntegerField("_sp"), 0);
    endoffField = new CIntField(type.getCIntegerField("_endoff"), 0);
    try {
        scloffField = new CIntField(type.getCIntegerField("_scloff"), 0);
    } catch (Exception e) {
    }
    monoffField = new CIntField(type.getCIntegerField("_monoff"), 0);
    stkoffField = new CIntField(type.getCIntegerField("_stkoff"), 0);
    locoffField = new CIntField(type.getCIntegerField("_locoff"), 0);
    depthField = new CIntField(type.getCIntegerField("_depth"), 0);
    callerField = type.getAddressField("_caller");
  }

  private static AddressField mapField;
  private static AddressField methodField;
  private static CIntField bciField;
  private static CIntField spField;
  private static CIntField endoffField;
  private static CIntField scloffField;
  private static CIntField monoffField;
  private static CIntField stkoffField;
  private static CIntField locoffField;
  private static CIntField depthField;
  private static AddressField callerField;

  public static JVMState create(Address addr) {
    if (addr == null) return null;
    return new JVMState(addr);
  }

  public JVMState(Address addr) {
    super(addr);
  }

  public ciMethod method() {
    return (ciMethod) ciObjectFactory.getMetadata(methodField.getValue(getAddress()));
  }

  public int bci() {
    return (int)bciField.getValue(getAddress());
  }

  public int depth() {
    return (int)depthField.getValue(getAddress());
  }

  public JVMState caller() {
    return create(callerField.getValue(getAddress()));
  }

  public void dumpSpec(PrintStream out) {
    ciMethod m = method();
    if (m != null) {
      Method meth = m.method();
      out.print(" " + meth.getMethodHolder().getName().asString().replace('/', '.') + "::" +
                meth.getName().asString() + " @ bci:" + bci());
    } else {
      out.print(" runtime stub");
    }
    if (caller() != null)  caller().dumpSpec(out);
  }
}
