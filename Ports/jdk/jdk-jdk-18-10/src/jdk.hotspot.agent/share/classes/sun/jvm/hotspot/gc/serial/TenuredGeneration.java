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

/** <P> TenuredGeneration models a heap of old objects contained
    in a single contiguous space. </P>

    <P> Garbage collection is performed using mark-compact. </P> */

public class TenuredGeneration extends CardGeneration {
  private static AddressField theSpaceField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("TenuredGeneration");

    theSpaceField = type.getAddressField("_the_space");
  }

  public TenuredGeneration(Address addr) {
    super(addr);
  }

  public ContiguousSpace theSpace() {
    return (ContiguousSpace) VMObjectFactory.newObject(ContiguousSpace.class, theSpaceField.getValue(addr));
  }

  public boolean isIn(Address p) {
    return theSpace().contains(p);
  }

  /** Space queries */
  public long capacity()            { return theSpace().capacity();                                }
  public long used()                { return theSpace().used();                                    }
  public long free()                { return theSpace().free();                                    }
  public long contiguousAvailable() { return theSpace().free() + virtualSpace().uncommittedSize(); }

  public void spaceIterate(SpaceClosure blk, boolean usedOnly) {
    blk.doSpace(theSpace());
  }

  public void liveRegionsIterate(LiveRegionsClosure closure) {
    closure.doLiveRegions(theSpace());
  }

  public void printOn(PrintStream tty) {
    tty.print("  old ");
    theSpace().printOn(tty);
  }

  public Generation.Name kind() {
    return Generation.Name.MARK_SWEEP_COMPACT;
  }

  public String name() {
    return "tenured generation";
  }
}
