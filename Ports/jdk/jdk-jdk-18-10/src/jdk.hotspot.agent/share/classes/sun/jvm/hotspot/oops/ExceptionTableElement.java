/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ExceptionTableElement {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type            = db.lookupType("ExceptionTableElement");
    offsetOfStartPC      = type.getCIntegerField("start_pc").getOffset();
    offsetOfEndPC        = type.getCIntegerField("end_pc").getOffset();
    offsetOfHandlerPC    = type.getCIntegerField("handler_pc").getOffset();
    offsetOfCatchTypeIndex = type.getCIntegerField("catch_type_index").getOffset();
  }

  private static long offsetOfStartPC;
  private static long offsetOfEndPC;
  private static long offsetOfHandlerPC;
  private static long offsetOfCatchTypeIndex;

  private Address handle;
  private long      offset;

  public ExceptionTableElement(Address handle, long offset) {
    this.handle = handle;
    this.offset = offset;
  }

  public int getStartPC() {
    return (int) handle.getCIntegerAt(offset + offsetOfStartPC, 2, true);
  }

  public int getEndPC() {
    return (int) handle.getCIntegerAt(offset + offsetOfEndPC, 2, true);
  }

  public int getHandlerPC() {
    return (int) handle.getCIntegerAt(offset + offsetOfHandlerPC, 2, true);
  }

  public int getCatchTypeIndex() {
    return (int) handle.getCIntegerAt(offset + offsetOfCatchTypeIndex, 2, true);
  }
}
