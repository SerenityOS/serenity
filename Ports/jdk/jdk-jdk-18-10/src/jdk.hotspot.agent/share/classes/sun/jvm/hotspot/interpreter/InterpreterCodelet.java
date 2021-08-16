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

package sun.jvm.hotspot.interpreter;

import java.io.*;
import java.util.*;

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** An InterpreterCodelet is a piece of interpreter code. All
    interpreter code is generated into little codelets which contain
    extra information for debugging and printing purposes. */

public class InterpreterCodelet extends Stub {
  private static long          instanceSize;
  private static CIntegerField sizeField;             // the size in bytes
  private static AddressField  descriptionField;      // a description of the codelet, for debugging & printing
  private static CIntegerField bytecodeField;         // associated bytecode if any

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("InterpreterCodelet");

    sizeField            = type.getCIntegerField("_size");
    descriptionField     = type.getAddressField("_description");
    bytecodeField        = type.getCIntegerField("_bytecode");

    instanceSize = type.getSize();
  }

  public InterpreterCodelet(Address addr) {
    super(addr);
  }

  public long getSize() {
    return sizeField.getValue(addr);
  }

  public Address codeBegin() {
    return addr.addOffsetTo(instanceSize);
  }

  public Address codeEnd() {
    return addr.addOffsetTo(getSize());
  }

  public long codeSize() {
    return codeEnd().minus(codeBegin());
  }

  public String getDescription() {
    return CStringUtilities.getString(descriptionField.getValue(addr));
  }

  public void verify() {
  }

  public void printOn(PrintStream tty) {
    String desc = getDescription();
    if (desc != null) {
      tty.print(desc);
    }
    // FIXME: add printing of bytecode
    tty.println(" [" + codeBegin() + ", " + codeEnd() + ")  " +
                codeSize() + " bytes  ");
    // FIXME: add disassembly
  }
}
