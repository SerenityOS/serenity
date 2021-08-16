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

/** <P> A JavaCallWrapper is constructed before each JavaCall and
    destroyed after the call.  Its purpose is to allocate/deallocate a
    new handle block and to save/restore the last Java fp/sp. A
    pointer to the JavaCallWrapper is stored on the stack. </P>

    <P> NOTE: this port is very minimal and is only designed to get
    frame traversal working. FIXME: we will have to add platform-
    specific stuff later and therefore a factory for these
    objects. </P> */

public class JavaCallWrapper extends VMObject {
  protected static AddressField  anchorField;
  private static AddressField  lastJavaSPField;
  private static AddressField  lastJavaPCField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("JavaCallWrapper");
    Type anchorType = db.lookupType("JavaFrameAnchor");

    anchorField      = type.getAddressField("_anchor");
    lastJavaSPField  = anchorType.getAddressField("_last_Java_sp");
    lastJavaPCField  = anchorType.getAddressField("_last_Java_pc");
  }

  public JavaCallWrapper(Address addr) {
    super(addr);
  }

  public Address getLastJavaSP() {
    return lastJavaSPField.getValue(addr.addOffsetTo(anchorField.getOffset()));
  }

  public Address getLastJavaPC() {
    return lastJavaPCField.getValue(addr.addOffsetTo(anchorField.getOffset()));
  }

}
