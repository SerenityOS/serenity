/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class Mark extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type  = db.lookupType("oopDesc");
    markField  = type.getCIntegerField("_mark");

    ageBits             = db.lookupLongConstant("markWord::age_bits").longValue();
    lockBits            = db.lookupLongConstant("markWord::lock_bits").longValue();
    maxHashBits         = db.lookupLongConstant("markWord::max_hash_bits").longValue();
    hashBits            = db.lookupLongConstant("markWord::hash_bits").longValue();
    lockShift           = db.lookupLongConstant("markWord::lock_shift").longValue();
    ageShift            = db.lookupLongConstant("markWord::age_shift").longValue();
    hashShift           = db.lookupLongConstant("markWord::hash_shift").longValue();
    lockMask            = db.lookupLongConstant("markWord::lock_mask").longValue();
    lockMaskInPlace     = db.lookupLongConstant("markWord::lock_mask_in_place").longValue();
    ageMask             = db.lookupLongConstant("markWord::age_mask").longValue();
    ageMaskInPlace      = db.lookupLongConstant("markWord::age_mask_in_place").longValue();
    hashMask            = db.lookupLongConstant("markWord::hash_mask").longValue();
    hashMaskInPlace     = db.lookupLongConstant("markWord::hash_mask_in_place").longValue();
    lockedValue         = db.lookupLongConstant("markWord::locked_value").longValue();
    unlockedValue       = db.lookupLongConstant("markWord::unlocked_value").longValue();
    monitorValue        = db.lookupLongConstant("markWord::monitor_value").longValue();
    markedValue         = db.lookupLongConstant("markWord::marked_value").longValue();
    noHash              = db.lookupLongConstant("markWord::no_hash").longValue();
    noHashInPlace       = db.lookupLongConstant("markWord::no_hash_in_place").longValue();
    noLockInPlace       = db.lookupLongConstant("markWord::no_lock_in_place").longValue();
    maxAge              = db.lookupLongConstant("markWord::max_age").longValue();
  }

  // Field accessors
  private static CIntegerField markField;

  // Constants -- read from VM
  private static long ageBits;
  private static long lockBits;
  private static long maxHashBits;
  private static long hashBits;

  private static long lockShift;
  private static long ageShift;
  private static long hashShift;

  private static long lockMask;
  private static long lockMaskInPlace;
  private static long ageMask;
  private static long ageMaskInPlace;
  private static long hashMask;
  private static long hashMaskInPlace;

  private static long lockedValue;
  private static long unlockedValue;
  private static long monitorValue;
  private static long markedValue;

  private static long noHash;

  private static long noHashInPlace;
  private static long noLockInPlace;

  private static long maxAge;

  /* Constants in markWord used by CMS. */
  private static long cmsShift;
  private static long cmsMask;
  private static long sizeShift;

  public Mark(Address addr) {
    super(addr);
  }

  public long value() {
    return markField.getValue(addr);
  }

  public Address valueAsAddress() {
    return addr.getAddressAt(markField.getOffset());
  }

  // lock accessors (note that these assume lock_shift == 0)
  public boolean isLocked() {
    return (Bits.maskBitsLong(value(), lockMaskInPlace) != unlockedValue);
  }
  public boolean isUnlocked() {
    return (Bits.maskBitsLong(value(), lockMaskInPlace) == unlockedValue);
  }
  public boolean isMarked() {
    return (Bits.maskBitsLong(value(), lockMaskInPlace) == markedValue);
  }

  // Special temporary state of the markWord while being inflated.
  // Code that looks at mark outside a lock need to take this into account.
  public boolean isBeingInflated() {
    return (value() == 0);
  }

  // Should this header be preserved during GC?
  public boolean mustBePreserved() {
     return (!isUnlocked() || !hasNoHash());
  }

  // WARNING: The following routines are used EXCLUSIVELY by
  // synchronization functions. They are not really gc safe.
  // They must get updated if markWord layout get changed.

  public boolean hasLocker() {
    return ((value() & lockMaskInPlace) == lockedValue);
  }
  public BasicLock locker() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasLocker(), "check");
    }
    return new BasicLock(valueAsAddress());
  }
  public boolean hasMonitor() {
    return ((value() & monitorValue) != 0);
  }
  public ObjectMonitor monitor() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasMonitor(), "check");
    }
    // Use xor instead of &~ to provide one extra tag-bit check.
    Address monAddr = valueAsAddress().xorWithMask(monitorValue);
    return new ObjectMonitor(monAddr);
  }
  public boolean hasDisplacedMarkHelper() {
    return ((value() & unlockedValue) == 0);
  }
  public Mark displacedMarkHelper() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasDisplacedMarkHelper(), "check");
    }
    Address addr = valueAsAddress().andWithMask(~monitorValue);
    return new Mark(addr.getAddressAt(0));
  }
  public int age() { return (int) Bits.maskBitsLong(value() >> ageShift, ageMask); }

  // hash operations
  public long hash() {
    return Bits.maskBitsLong(value() >> hashShift, hashMask);
  }

  public boolean hasNoHash() {
    return hash() == noHash;
  }

  // Debugging
  public void printOn(PrintStream tty) {
    if (isLocked()) {
      tty.print("locked(0x" +
                Long.toHexString(value()) + ")->");
      displacedMarkHelper().printOn(tty);
    } else {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(isUnlocked(), "just checking");
      }
      tty.print("mark(");
      tty.print("hash " + Long.toHexString(hash()) + ",");
      tty.print("age " + age() + ")");
    }
  }

  public long getSize() { return (long)(value() >> sizeShift); }
}
