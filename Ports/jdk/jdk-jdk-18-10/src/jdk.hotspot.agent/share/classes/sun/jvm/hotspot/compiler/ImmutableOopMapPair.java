/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.compiler;

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ImmutableOopMapPair {
  private static CIntegerField pcOffsetField;
  private static CIntegerField oopmapOffsetField;
  private static long classSize;

  static {
    VM.registerVMInitializedObserver(new Observer() {
      public void update(Observable o, Object data) {
        initialize(VM.getVM().getTypeDataBase());
      }
    });
  }

  private final Address address;

  public ImmutableOopMapPair(Address address) {
    this.address = address;
  }

  public static long classSize() {
    return classSize;
  }

  public int getPC() {
    return (int) pcOffsetField.getValue(address);
  }

  public int getOffset() {
    return (int) oopmapOffsetField.getValue(address);
  }

  private static void initialize(TypeDataBase db) {
    Type type = db.lookupType("ImmutableOopMapPair");

    pcOffsetField = type.getCIntegerField("_pc_offset");
    oopmapOffsetField = type.getCIntegerField("_oopmap_offset");
    classSize = type.getSize();
  }

  public String toString() {
    return "Pair{pc_offset = " + getPC() + ", data_offset = " + getOffset() + "}";
  }
}
