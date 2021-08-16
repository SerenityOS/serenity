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

package sun.jvm.hotspot.runtime;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;


public class vmSymbols {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static Address symbolsAddress;
  private static int FIRST_SID;
  private static int SID_LIMIT;

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    // All VM symbols are now stored in: Symbol* Symbol::_vm_symbols[];
    Type type            = db.lookupType("Symbol");
    symbolsAddress       = type.getAddressField("_vm_symbols[0]").getStaticFieldAddress();
    FIRST_SID            = db.lookupIntConstant("vmSymbols::FIRST_SID");
    SID_LIMIT            = db.lookupIntConstant("vmSymbols::SID_LIMIT");
  }

  public static Symbol symbolAt(int id) {
    if (id < FIRST_SID || id >= SID_LIMIT) throw new IndexOutOfBoundsException("bad SID " + id);
    return Symbol.create(symbolsAddress.getAddressAt(id * VM.getVM().getAddressSize()));
  }
}
