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

package sun.jvm.hotspot.compiler;

import java.util.*;

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class OopMapValue {
  private short value;
  private short contentReg;

  /** Read from target VM; located in compiler/oopMap.hpp */
  // How bits are organized
  static int TYPE_BITS;
  static int REGISTER_BITS;
  static int TYPE_SHIFT;
  static int REGISTER_SHIFT;
  static int TYPE_MASK;
  static int TYPE_MASK_IN_PLACE;
  static int REGISTER_MASK;
  static int REGISTER_MASK_IN_PLACE;

  // Types of OopValues
  static int OOP_VALUE;
  static int NARROWOOP_VALUE;
  static int CALLEE_SAVED_VALUE;
  static int DERIVED_OOP_VALUE;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    TYPE_BITS              = db.lookupIntConstant("OopMapValue::type_bits").intValue();
    REGISTER_BITS          = db.lookupIntConstant("OopMapValue::register_bits").intValue();
    TYPE_SHIFT             = db.lookupIntConstant("OopMapValue::type_shift").intValue();
    REGISTER_SHIFT         = db.lookupIntConstant("OopMapValue::register_shift").intValue();
    TYPE_MASK              = db.lookupIntConstant("OopMapValue::type_mask").intValue();
    TYPE_MASK_IN_PLACE     = db.lookupIntConstant("OopMapValue::type_mask_in_place").intValue();
    REGISTER_MASK          = db.lookupIntConstant("OopMapValue::register_mask").intValue();
    REGISTER_MASK_IN_PLACE = db.lookupIntConstant("OopMapValue::register_mask_in_place").intValue();
    OOP_VALUE              = db.lookupIntConstant("OopMapValue::oop_value").intValue();
    NARROWOOP_VALUE        = db.lookupIntConstant("OopMapValue::narrowoop_value").intValue();
    CALLEE_SAVED_VALUE     = db.lookupIntConstant("OopMapValue::callee_saved_value").intValue();
    DERIVED_OOP_VALUE      = db.lookupIntConstant("OopMapValue::derived_oop_value").intValue();
  }

  public static abstract class OopTypes {
    public static final OopTypes OOP_VALUE          = new OopTypes() { int getValue() { return OopMapValue.OOP_VALUE;          }};
    public static final OopTypes NARROWOOP_VALUE    = new OopTypes() { int getValue() { return OopMapValue.NARROWOOP_VALUE;         }};
    public static final OopTypes CALLEE_SAVED_VALUE = new OopTypes() { int getValue() { return OopMapValue.CALLEE_SAVED_VALUE; }};
    public static final OopTypes DERIVED_OOP_VALUE  = new OopTypes() { int getValue() { return OopMapValue.DERIVED_OOP_VALUE;  }};

    abstract int getValue();
    protected OopTypes() {}
  }

  public OopMapValue()                                  { setValue((short) 0); setContentReg(new VMReg(0)); }
  public OopMapValue(VMReg reg, OopTypes t)             { setReg(reg); setType(t);                      }
  public OopMapValue(VMReg reg, OopTypes t, VMReg reg2) { setReg(reg); setType(t); setContentReg(reg2); }
  public OopMapValue(CompressedReadStream stream)       { readFrom(stream);                             }

  public void readFrom(CompressedReadStream stream) {
    setValue((short) stream.readInt());
    if (isCalleeSaved() || isDerivedOop()) {
      setContentReg(new VMReg(stream.readInt()));
    }
  }

  // Querying
  public boolean isOop()         { return (getValue() & TYPE_MASK_IN_PLACE) == OOP_VALUE;          }
  public boolean isNarrowOop()   { return (getValue() & TYPE_MASK_IN_PLACE) == NARROWOOP_VALUE;    }
  public boolean isCalleeSaved() { return (getValue() & TYPE_MASK_IN_PLACE) == CALLEE_SAVED_VALUE; }
  public boolean isDerivedOop()  { return (getValue() & TYPE_MASK_IN_PLACE) == DERIVED_OOP_VALUE;  }

  public VMReg getReg() { return new VMReg((getValue() & REGISTER_MASK_IN_PLACE) >> REGISTER_SHIFT); }
  public void  setReg(VMReg r) { setValue((short) (r.getValue() << REGISTER_SHIFT | (getValue() & TYPE_MASK_IN_PLACE))); }

  public OopTypes getType() {
    int which = (getValue() & TYPE_MASK_IN_PLACE);
         if (which == OOP_VALUE)    return OopTypes.OOP_VALUE;
    else if (which == NARROWOOP_VALUE)   return OopTypes.NARROWOOP_VALUE;
    else if (which == CALLEE_SAVED_VALUE) return OopTypes.CALLEE_SAVED_VALUE;
    else if (which == DERIVED_OOP_VALUE)  return OopTypes.DERIVED_OOP_VALUE;
    else throw new InternalError("unknown which " + which + " (TYPE_MASK_IN_PLACE = " + TYPE_MASK_IN_PLACE + ")");
  }
  public void setType(OopTypes t) { setValue((short) ((getValue() & REGISTER_MASK_IN_PLACE) | t.getValue())); }

  public VMReg getContentReg()        { return new VMReg(contentReg); }
  public void  setContentReg(VMReg r) { contentReg = (short) r.getValue(); }

  /** Physical location queries */
  public boolean isRegisterLoc()      { return (getReg().lessThan(VM.getVM().getVMRegImplInfo().getStack0())); }
  public boolean isStackLoc()         { return (getReg().greaterThanOrEqual(VM.getVM().getVMRegImplInfo().getStack0())); }

  /** Returns offset from sp. */
  public int getStackOffset() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isStackLoc(), "must be stack location");
    }
    return getReg().minus(VM.getVM().getVMRegImplInfo().getStack0());
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private void setValue(short value) {
    this.value = value;
  }

  private int getValue() {
    return value;
  }
}
