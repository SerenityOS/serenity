/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class BreakpointInfo extends VMObject {
  private static CIntegerField origBytecodeField;
  private static CIntegerField bciField;
  private static CIntegerField nameIndexField;
  private static CIntegerField signatureIndexField;
  private static AddressField  nextField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    if (!VM.getVM().isJvmtiSupported()) {
      // no BreakpointInfo support without JVMTI
      return;
    }

    Type type                  = db.lookupType("BreakpointInfo");

    origBytecodeField   = type.getCIntegerField("_orig_bytecode");
    bciField            = type.getCIntegerField("_bci");
    nameIndexField      = type.getCIntegerField("_name_index");
    signatureIndexField = type.getCIntegerField("_signature_index");
    nextField           = type.getAddressField ("_next");
  }

  public BreakpointInfo(Address addr) {
    super(addr);
  }

  public int  getOrigBytecode()   { return (int) origBytecodeField.getValue(addr);   }
  public int  getBCI()            { return (int) bciField.getValue(addr);            }
  public long getNameIndex()      { return nameIndexField.getValue(addr);            }
  public long getSignatureIndex() { return signatureIndexField.getValue(addr);       }
  public BreakpointInfo getNext() {
    return (BreakpointInfo) VMObjectFactory.newObject(BreakpointInfo.class, nextField.getValue(addr));
  }

  public boolean match(Method m, int bci) {
    return (bci == getBCI() && match(m));
  }

  public boolean match(Method m) {
    return (getNameIndex() == m.getNameIndex() &&
            getSignatureIndex() == m.getSignatureIndex());
  }
}
