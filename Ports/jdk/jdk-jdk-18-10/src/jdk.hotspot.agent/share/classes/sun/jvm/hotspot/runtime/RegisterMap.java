/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** <P> A companion structure used for stack traversal. The
    RegisterMap contains misc. information needed in order to do
    correct stack traversal of stack frames.  Hence, it must always be
    passed in as an argument to Frame.sender(RegisterMap). </P>

    <P> The use of RegisterMaps is slightly different in the
    Serviceability Agent APIs than in the VM itself. In the VM, a
    RegisterMap is created either for a particular thread or cloned
    from another RegisterMap. In these APIs, a JavaThread is the
    top-level factory for RegisterMaps, and RegisterMaps know how to
    copy themselves (through either the clone() or copy()
    methods). </P> */

public abstract class RegisterMap implements Cloneable {
  /** Location of registers */
  protected Address[]  location;
  // FIXME: don't know about LocationValidType
  protected long[]     locationValid;
  /** Should include argument_oop marked locations for compiler */
  protected boolean    includeArgumentOops;
  /** Reference to current thread */
  protected JavaThread thread;
  /** Tells if the register map needs to be updated when traversing the stack */
  protected boolean    updateMap;
  /** Location of a frame where the pc is not at a call (NULL if no frame exists) */
  protected static int regCount;
  protected static int locationValidTypeSize;
  protected static int locationValidSize;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    regCount = db.lookupIntConstant("ConcreteRegisterImpl::number_of_registers").intValue();
    // FIXME: don't know about LocationValidType. The LocationValidType is typedef'ed as julong
    // so used julong to get the size of LocationValidType.
    locationValidTypeSize = (int)db.lookupType("julong").getSize() * 8;
    locationValidSize = (regCount + locationValidTypeSize - 1) / locationValidTypeSize;
  }

  protected RegisterMap(JavaThread thread, boolean updateMap) {
    this.thread    = thread;
    this.updateMap = updateMap;
    location      = new Address[regCount];
    locationValid = new long[locationValidSize];
    clear();
  }

  /** Makes a copy of map into this */
  protected RegisterMap(RegisterMap map) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map != null, "RegisterMap must be present");
    }
    this.thread              = map.getThread();
    this.updateMap           = map.getUpdateMap();
    this.includeArgumentOops = map.getIncludeArgumentOops();
    location                 = new Address[map.location.length];
    locationValid            = new long[map.locationValid.length];
    initializeFromPD(map);
    if (updateMap) {
      for (int i = 0; i < locationValidSize; i++) {
        long bits = (!getUpdateMap()) ? 0 : map.locationValid[i];
        locationValid[i] = bits;
        // for whichever bits are set, pull in the corresponding map->_location
        int j = i*locationValidTypeSize;
        while (bits != 0) {
          if ((bits & 1) != 0) {
            if (Assert.ASSERTS_ENABLED) {
              Assert.that(0 <= j && j < regCount, "range check");
            }
            location[j] = map.location[j];
          }
          bits >>>= 1;
          j += 1;
        }
      }
    }
  }

  public abstract Object clone();

  public RegisterMap copy() {
    return (RegisterMap) clone();
  }

  public void clear() {
    setIncludeArgumentOops(true);
    if (!VM.getVM().isCore()) {
      if (updateMap) {
        for (int i = 0; i < locationValid.length; i++) {
          locationValid[i] = 0;
        }
        clearPD();
      } else {
        initializePD();
      }
    }
  }

  public Address getLocation(VMReg reg) {
    int i = reg.getValue();
    int index = i / locationValidTypeSize;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= i && i < regCount, "sanity check");
      Assert.that(0 <= index && index < locationValidSize, "sanity check");
    }
    if ((locationValid[index] & (1L << i % locationValidTypeSize)) != 0) {
      return location[i];
    } else {
      return getLocationPD(reg);
    }
  }

  public void setLocation(VMReg reg, Address loc) {
    int i = reg.getValue();
    int index = i / locationValidTypeSize;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= i && i < regCount, "sanity check");
      Assert.that(0 <= index && index < locationValidSize, "sanity check");
      Assert.that(updateMap, "updating map that does not need updating");
    }
    location[i]          = loc;
    locationValid[index] |= (1L << (i % locationValidTypeSize));
  }

  public boolean getIncludeArgumentOops() {
    return includeArgumentOops;
  }

  public void setIncludeArgumentOops(boolean f) {
    includeArgumentOops = f;
  }

  public JavaThread getThread() {
    return thread;
  }

  public boolean getUpdateMap() {
    return updateMap;
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    tty.println("Register map");
    for (int i = 0; i < location.length; i++) {
      Address src = getLocation(new VMReg(i));
      if (src != null) {
        tty.print("  " + VMRegImpl.getRegisterName(i) +
                  " [" + src + "] = ");
        if (src.andWithMask(VM.getVM().getAddressSize() - 1) != null) {
          tty.print("<misaligned>");
        } else {
          tty.print(src.getAddressAt(0));
        }
      }
    }
  }

  /** Platform-dependent clear() functionality */
  protected abstract void clearPD();
  /** Platform-dependent initialize() functionality */
  protected abstract void initializePD();
  /** Platform-dependent initializeFrom() functionality */
  protected abstract void initializeFromPD(RegisterMap map);
  /** Platform-dependent getLocation() functionality */
  protected abstract Address getLocationPD(VMReg reg);
}
