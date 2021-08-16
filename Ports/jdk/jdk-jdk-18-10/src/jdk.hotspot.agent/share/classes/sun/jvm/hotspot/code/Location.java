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

package sun.jvm.hotspot.code;

import java.io.*;
import java.util.*;

import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** <P> A Location describes a concrete machine variable location
    (such as integer or floating point register or a stack-held
    variable). Used when generating debug-information for
    nmethods. </P>

    <P> Encoding: </P>
    <PRE>
    bits:
    Type:   [3..0]
    Where:  [4]
    Offset: [31..5]
    </PRE>
*/

public class Location {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static void initialize(TypeDataBase db) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!VM.getVM().isCore(), "Debug info not used in core build");
    }

    OFFSET_MASK  = db.lookupIntConstant("Location::OFFSET_MASK").intValue();
    OFFSET_SHIFT = db.lookupIntConstant("Location::OFFSET_SHIFT").intValue();
    TYPE_MASK    = db.lookupIntConstant("Location::TYPE_MASK").intValue();
    TYPE_SHIFT   = db.lookupIntConstant("Location::TYPE_SHIFT").intValue();
    WHERE_MASK   = db.lookupIntConstant("Location::WHERE_MASK").intValue();
    WHERE_SHIFT  = db.lookupIntConstant("Location::WHERE_SHIFT").intValue();

    // Location::Type constants
    TYPE_NORMAL = db.lookupIntConstant("Location::normal").intValue();
    TYPE_OOP = db.lookupIntConstant("Location::oop").intValue();
    TYPE_NARROWOOP = db.lookupIntConstant("Location::narrowoop").intValue();
    TYPE_INT_IN_LONG = db.lookupIntConstant("Location::int_in_long").intValue();
    TYPE_LNG = db.lookupIntConstant("Location::lng").intValue();
    TYPE_FLOAT_IN_DBL = db.lookupIntConstant("Location::float_in_dbl").intValue();
    TYPE_DBL = db.lookupIntConstant("Location::dbl").intValue();
    TYPE_ADDR = db.lookupIntConstant("Location::addr").intValue();
    TYPE_INVALID = db.lookupIntConstant("Location::invalid").intValue();

    // Location::Where constants
    WHERE_ON_STACK = db.lookupIntConstant("Location::on_stack").intValue();
    WHERE_IN_REGISTER = db.lookupIntConstant("Location::in_register").intValue();
  }

  private int value;

  // type safe enum for "Where"
  public static class Where {
    public static final Where ON_STACK    = new Where("on_stack");
    public static final Where IN_REGISTER = new Where("in_register");

    private Where(String value) {
      this.value = value;
    }

    public String toString() {
      return value;
    }

    private String value;

    public int getValue() {
      if (this == ON_STACK) {
         return WHERE_ON_STACK;
      } else if (this == IN_REGISTER) {
         return WHERE_IN_REGISTER;
      } else {
         throw new RuntimeException("should not reach here");
      }
    }
  }

  // type safe enum for "Type"
  public static class Type {
    /** Ints, floats, double halves */
    public static final Type NORMAL       = new Type("normal");
    /** Oop (please GC me!) */
    public static final Type OOP          = new Type("oop");
    /** NarrowOop (please GC me!) */
    public static final Type NARROWOOP    = new Type("narrowoop");
    /** Long held in one register */
    public static final Type INT_IN_LONG  = new Type("int_in_long");
    /** Long held in one register */
    public static final Type LNG          = new Type("lng");
    /** Float held in double register */
    public static final Type FLOAT_IN_DBL = new Type("float_in_dbl");
    /** Double held in one register */
    public static final Type DBL          = new Type("dbl");
    /** JSR return address */
    public static final Type ADDR         = new Type("addr");
    /** Invalid location */
    public static final Type INVALID      = new Type("invalid");

    private Type(String value) {
      this.value = value;
    }
    private String value;

    public String toString() {
      return value;
    }

    public int getValue() {
      if (this == NORMAL) {
        return TYPE_NORMAL;
      } else if (this == OOP) {
        return TYPE_OOP;
      } else if (this == NARROWOOP) {
        return TYPE_NARROWOOP;
      } else if (this == INT_IN_LONG) {
        return TYPE_INT_IN_LONG;
      } else if (this == LNG) {
        return TYPE_LNG;
      } else if (this == FLOAT_IN_DBL) {
        return TYPE_FLOAT_IN_DBL;
      } else if (this == DBL) {
        return TYPE_DBL;
      } else if (this == ADDR) {
        return TYPE_ADDR;
      } else if (this == INVALID) {
        return TYPE_INVALID;
      } else {
        throw new RuntimeException("should not reach here");
      }
    }
  }

  private static int OFFSET_MASK;
  private static int OFFSET_SHIFT;
  private static int TYPE_MASK;
  private static int TYPE_SHIFT;
  private static int WHERE_MASK;
  private static int WHERE_SHIFT;

  // constants in Type enum
  private static int TYPE_NORMAL;
  private static int TYPE_OOP;
  private static int TYPE_NARROWOOP;
  private static int TYPE_INT_IN_LONG;
  private static int TYPE_LNG;
  private static int TYPE_FLOAT_IN_DBL;
  private static int TYPE_DBL;
  private static int TYPE_ADDR;
  private static int TYPE_INVALID;

  // constants in Where enum
  private static int WHERE_ON_STACK;
  private static int WHERE_IN_REGISTER;

  /** Create a bit-packed Location */
  Location(Where where, Type type, int offset) {
    setWhere(where);
    setType(type);
    setOffset(offset);
  }

  public Where getWhere() {
    int where = (value & WHERE_MASK) >> WHERE_SHIFT;
    if (where == WHERE_ON_STACK) {
       return Where.ON_STACK;
    } else if (where == WHERE_IN_REGISTER) {
       return Where.IN_REGISTER;
    } else {
       throw new RuntimeException("should not reach here");
    }
  }

  public Type getType() {
    int type = (value & TYPE_MASK) >> TYPE_SHIFT;
    if (type == TYPE_NORMAL) {
       return Type.NORMAL;
    } else if (type == TYPE_OOP) {
       return Type.OOP;
    } else if (type == TYPE_NARROWOOP) {
       return Type.NARROWOOP;
    } else if (type == TYPE_INT_IN_LONG) {
       return Type.INT_IN_LONG;
    } else if (type == TYPE_LNG) {
       return Type.LNG;
    } else if (type == TYPE_FLOAT_IN_DBL) {
       return Type.FLOAT_IN_DBL;
    } else if (type == TYPE_DBL) {
       return Type.DBL;
    } else if (type == TYPE_ADDR) {
       return Type.ADDR;
    } else if (type == TYPE_INVALID) {
       return Type.INVALID;
    } else {
       throw new RuntimeException("should not reach here");
    }
  }

  public short getOffset() {
    return (short) ((value & OFFSET_MASK) >> OFFSET_SHIFT);
  }

  public boolean isRegister() {
    return getWhere() == Where.IN_REGISTER;
  }

  public boolean isStack() {
    return getWhere() == Where.ON_STACK;
  }

  public boolean holdsOop() {
    return getType() == Type.OOP;
  }

  public boolean holdsNarrowOop() {
    return getType() == Type.NARROWOOP;
  }

  public boolean holdsInt() {
    return getType() == Type.INT_IN_LONG;
  }

  public boolean holdsLong() {
    return getType() == Type.LNG;
  }

  public boolean holdsFloat() {
    return getType() == Type.FLOAT_IN_DBL;
  }

  public boolean holdsDouble() {
    return getType() == Type.DBL;
  }

  public boolean holdsAddr() {
    return getType() == Type.ADDR;
  }

  public boolean isIllegal() {
    return getType() == Type.INVALID;
  }

  public int getStackOffset() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getWhere() == Where.ON_STACK, "wrong Where");
    }
    return getOffset() * (int)VM.getVM().getIntSize();
  }

  public int getRegisterNumber() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getWhere() == Where.IN_REGISTER, "wrong Where");
    }
    return getOffset();
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    tty.print("Value " + value + ", ");
    if (isIllegal()) {
      tty.print("Illegal");
    } else {
      Where w = getWhere();
      if (w == Where.ON_STACK) {
        tty.print("stack[" + getStackOffset() + "]");
      } else if (w == Where.IN_REGISTER) {
        tty.print("reg " + getRegisterNumber());
      }

      Type type = getType();
      if (type == Type.NORMAL) {
      } else if (type == Type.OOP) {
        tty.print(",oop");
      } else if (type == Type.NARROWOOP) {
        tty.print(",narrowoop");
      } else if (type == Type.INT_IN_LONG) {
        tty.print(",int");
      } else if (type == Type.LNG) {
        tty.print(",long");
      } else if (type == Type.FLOAT_IN_DBL) {
        tty.print(",float");
      } else if (type == Type.DBL) {
        tty.print(",double");
      } else if (type == Type.ADDR) {
        tty.print(",address");
      } else if (type == Type.INVALID) {
        tty.print(",invalid");
      }
    }
  }

  /** Serialization of debugging information */
  public Location(DebugInfoReadStream stream) {
    value = stream.readInt();
  }

  // FIXME: not yet implementable
  // void write_on(DebugInfoWriteStream* stream);


  //-----------------------------------------------------------------------------
  // Internals only below this point
  //

  private void setWhere(Where where) {
    value |= ((where.getValue() << WHERE_SHIFT) & WHERE_MASK);
  }

  private void setType(Type type) {
    value |= ((type.getValue() << TYPE_SHIFT) & TYPE_MASK);
  }

  private void setOffset(int offset) {
    value |= ((offset << OFFSET_SHIFT) & OFFSET_MASK);
  }
}
