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

package sun.jvm.hotspot.gc.serial;

import java.io.*;
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** DefNewGeneration is a young generation containing eden, from- and
    to-space. */

public class DefNewGeneration extends Generation {
  protected static AddressField edenSpaceField;
  protected static AddressField fromSpaceField;
  protected static AddressField toSpaceField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("DefNewGeneration");

    edenSpaceField = type.getAddressField("_eden_space");
    fromSpaceField = type.getAddressField("_from_space");
    toSpaceField   = type.getAddressField("_to_space");
  }

  public DefNewGeneration(Address addr) {
    super(addr);
  }

  public Generation.Name kind() {
    return Generation.Name.DEF_NEW;
  }

  // Accessing spaces
  public ContiguousSpace eden() {
    return (ContiguousSpace) VMObjectFactory.newObject(ContiguousSpace.class, edenSpaceField.getValue(addr));
  }

  public ContiguousSpace from() {
    return (ContiguousSpace) VMObjectFactory.newObject(ContiguousSpace.class, fromSpaceField.getValue(addr));
  }

  public ContiguousSpace to() {
    return (ContiguousSpace) VMObjectFactory.newObject(ContiguousSpace.class, toSpaceField.getValue(addr));
  }

  public long capacity()            { return eden().capacity() + from().capacity(); /* to() is only used during scavenge */ }
  public long used()                { return eden().used()     + from().used();     /* to() is only used during scavenge */ }
  public long free()                { return eden().free()     + from().free();     /* to() is only used during scavenge */ }
  public long contiguousAvailable() { return eden().free(); }

  public String name() {
    return "default new generation";
  }

  public void spaceIterate(SpaceClosure blk, boolean usedOnly) {
    blk.doSpace(eden());
    blk.doSpace(from());
    if (!usedOnly) {
      blk.doSpace(to());
    }
  }

  public void liveRegionsIterate(LiveRegionsClosure closure) {
    closure.doLiveRegions(eden());
    closure.doLiveRegions(from());
  }

  public void printOn(PrintStream tty) {
    tty.print("  eden");
    eden().printOn(tty);
    tty.print("\n  from");
    from().printOn(tty);
    tty.print("\n  to  ");
    to().printOn(tty);
  }
}
