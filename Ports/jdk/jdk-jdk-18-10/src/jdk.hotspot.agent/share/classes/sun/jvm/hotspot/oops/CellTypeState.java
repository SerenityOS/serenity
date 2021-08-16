/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.utilities.*;

/** Auxiliary class for GenerateOopMap */
public class CellTypeState {
  private int _state;

  // Masks for separating the BITS and INFO portions of a
  // CellTypeState
  private static final int info_mask = Bits.rightNBits(28);
  private static final int bits_mask = ~info_mask;

  // These constant are used for manipulating the BITS portion of a
  // CellTypeState
  private static final int uninit_bit     = Bits.nthBit(31);
  private static final int ref_bit        = Bits.nthBit(30);
  private static final int val_bit        = Bits.nthBit(29);
  private static final int addr_bit       = Bits.nthBit(28);
  private static final int live_bits_mask = bits_mask & ~uninit_bit;

  // These constants are used for manipulating the INFO portion of a
  // CellTypeState
  private static final int top_info_bit        = Bits.nthBit(27);
  private static final int not_bottom_info_bit = Bits.nthBit(26);
  private static final int info_data_mask      = Bits.rightNBits(26);
  private static final int info_conflict       = info_mask;

  // Within the INFO data, these values are used to distinguish
  // different kinds of references.
  // 0 if this reference is locked as a monitor
  private static final int ref_not_lock_bit    = Bits.nthBit(25);
  // 1 if this reference is a "slot" reference
  private static final int ref_slot_bit        = Bits.nthBit(24);
  // 0 if it is a "line" reference.
  private static final int ref_data_mask       = Bits.rightNBits(24);

  // These values are used to initialize commonly used CellTypeState
  // constants.
  private static final int bottom_value        = 0;
  private static final int uninit_value        = uninit_bit | info_conflict;
  private static final int ref_value           = ref_bit;
  private static final int ref_conflict        = ref_bit | info_conflict;
  private static final int val_value           = val_bit | info_conflict;
  private static final int addr_value          = addr_bit;
  private static final int addr_conflict       = addr_bit | info_conflict;

  private CellTypeState() {}

  private CellTypeState(int state) {
    _state = state;
  }

  public CellTypeState copy() {
    return new CellTypeState(_state);
  }

  public static CellTypeState makeAny(int state) {
    CellTypeState s = new CellTypeState(state);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(s.isValidState(), "check to see if CellTypeState is valid");
    }
    return s;
  }

  public static CellTypeState makeBottom() {
    return makeAny(0);
  }

  public static CellTypeState makeTop() {
    return makeAny(Bits.AllBits);
  }

  public static CellTypeState makeAddr(int bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that((bci >= 0) && (bci < info_data_mask),
                  "check to see if ret addr is valid");
    }
    return makeAny(addr_bit | not_bottom_info_bit | (bci & info_data_mask));
  }

  public static CellTypeState makeSlotRef(int slot_num) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(slot_num >= 0 && slot_num < ref_data_mask, "slot out of range");
    }
    return makeAny(ref_bit | not_bottom_info_bit | ref_not_lock_bit | ref_slot_bit |
                   (slot_num & ref_data_mask));
  }

  public static CellTypeState makeLineRef(int bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bci >= 0 && bci < ref_data_mask, "line out of range");
    }
    return makeAny(ref_bit | not_bottom_info_bit | ref_not_lock_bit |
                   (bci & ref_data_mask));
  }

  public static CellTypeState makeLockRef(int bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(bci >= 0 && bci < ref_data_mask, "line out of range");
    }
    return makeAny(ref_bit | not_bottom_info_bit | (bci & ref_data_mask));
  }

  // Query methods:
  public boolean isBottom()     { return _state == 0; }
  public boolean isLive()       { return ((_state & live_bits_mask) != 0); }
  public boolean isValidState() {
    // Uninitialized and value cells must contain no data in their info field:
    if ((canBeUninit() || canBeValue()) && !isInfoTop()) {
      return false;
    }
    // The top bit is only set when all info bits are set:
    if (isInfoTop() && ((_state & info_mask) != info_mask)) {
      return false;
    }
    // The not_bottom_bit must be set when any other info bit is set:
    if (isInfoBottom() && ((_state & info_mask) != 0)) {
      return false;
    }
    return true;
  }

  public boolean isAddress()      { return ((_state & bits_mask) == addr_bit); }
  public boolean isReference()    { return ((_state & bits_mask) == ref_bit); }
  public boolean isValue()        { return ((_state & bits_mask) == val_bit); }
  public boolean isUninit()       { return ((_state & bits_mask) == uninit_bit); }

  public boolean canBeAddress()   { return ((_state & addr_bit) != 0); }
  public boolean canBeReference() { return ((_state & ref_bit) != 0); }
  public boolean canBeValue()     { return ((_state & val_bit) != 0); }
  public boolean canBeUninit()    { return ((_state & uninit_bit) != 0); }

  public boolean isInfoBottom()   { return ((_state & not_bottom_info_bit) == 0); }
  public boolean isInfoTop()      { return ((_state & top_info_bit) != 0); }
  public int     getInfo() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that((!isInfoTop() && !isInfoBottom()),
                  "check to make sure top/bottom info is not used");
    }
    return (_state & info_data_mask);
  }

  public int     getMonitorSource() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isLockReference(), "must be lock");
    }
    return getInfo();
  }

  public boolean isGoodAddress()   { return isAddress() && !isInfoTop(); }
  public boolean isLockReference() {
    return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == ref_bit);
  }
  public boolean isNonlockReference() {
    return ((_state & (bits_mask | top_info_bit | ref_not_lock_bit)) == (ref_bit | ref_not_lock_bit));
  }

  public boolean equal(CellTypeState a)     { return _state == a._state; }
  public boolean equalKind(CellTypeState a) {
    return (_state & bits_mask) == (a._state & bits_mask);
  }

  public char toChar() {
    if (canBeReference()) {
      if (canBeValue() || canBeAddress())
        return '#';    // Conflict that needs to be rewritten
      else
        return 'r';
    } else if (canBeValue())
      return 'v';
    else if (canBeAddress())
      return 'p';
    else if (canBeUninit())
      return ' ';
    else
      return '@';
  }

  // Set
  public void set(CellTypeState cts) {
    _state = cts._state;
  }

  // Merge
  public CellTypeState merge (CellTypeState cts, int slot) {
    CellTypeState result = new CellTypeState();

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!isBottom() && !cts.isBottom(),
                  "merge of bottom values is handled elsewhere");
    }

    result._state = _state | cts._state;

    // If the top bit is set, we don't need to do any more work.
    if (!result.isInfoTop()) {
      Assert.that((result.canBeAddress() || result.canBeReference()),
                  "only addresses and references have non-top info");

      if (!equal(cts)) {
        // The two values being merged are different.  Raise to top.
        if (result.isReference()) {
          result = CellTypeState.makeSlotRef(slot);
        } else {
          result._state |= info_conflict;
        }
      }
    }
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(result.isValidState(), "checking that CTS merge maintains legal state");
    }

    return result;
  }

  // Debugging output
  public void print(PrintStream tty) {
    if (canBeAddress()) {
      tty.print("(p");
    } else {
      tty.print("( ");
    }
    if (canBeReference()) {
      tty.print("r");
    } else {
      tty.print(" ");
    }
    if (canBeValue()) {
      tty.print("v");
    } else {
      tty.print(" ");
    }
    if (canBeUninit()) {
      tty.print("u|");
    } else {
      tty.print(" |");
    }
    if (isInfoTop()) {
      tty.print("Top)");
    } else if (isInfoBottom()) {
      tty.print("Bot)");
    } else {
      if (isReference()) {
        int info = getInfo();
        int data = info & ~(ref_not_lock_bit | ref_slot_bit);
        if ((info & ref_not_lock_bit) != 0) {
          // Not a monitor lock reference.
          if ((info & ref_slot_bit) != 0) {
            // slot
            tty.print("slot" + data + ")");
          } else {
            // line
            tty.print("line" + data + ")");
          }
        } else {
          // lock
          tty.print("lock" + data + ")");
        }
      } else {
        tty.print("" + getInfo() + ")");
      }
    }
  }

  // Default values of common values
  public static CellTypeState bottom    = CellTypeState.makeBottom();
  public static CellTypeState uninit    = CellTypeState.makeAny(uninit_value);
  public static CellTypeState ref       = CellTypeState.makeAny(ref_conflict);
  public static CellTypeState value     = CellTypeState.makeAny(val_value);
  public static CellTypeState refUninit = CellTypeState.makeAny(ref_conflict | uninit_value);
  public static CellTypeState top       = CellTypeState.makeTop();
  public static CellTypeState addr      = CellTypeState.makeAny(addr_conflict);
}
