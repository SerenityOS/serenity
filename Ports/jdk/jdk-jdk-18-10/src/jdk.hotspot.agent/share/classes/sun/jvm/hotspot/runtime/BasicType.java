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

import sun.jvm.hotspot.types.TypeDataBase;


/** Encapsulates the BasicType enum in globalDefinitions.hpp in the
    VM. */

public class BasicType {
  public static final BasicType T_BOOLEAN = new BasicType();
  public static final BasicType T_CHAR = new BasicType();
  public static final BasicType T_FLOAT = new BasicType();
  public static final BasicType T_DOUBLE = new BasicType();
  public static final BasicType T_BYTE = new BasicType();
  public static final BasicType T_SHORT = new BasicType();
  public static final BasicType T_INT = new BasicType();
  public static final BasicType T_LONG = new BasicType();
  public static final BasicType T_OBJECT = new BasicType();
  public static final BasicType T_ARRAY = new BasicType();
  public static final BasicType T_VOID = new BasicType();
  public static final BasicType T_ADDRESS = new BasicType();
  public static final BasicType T_NARROWOOP = new BasicType();
  public static final BasicType T_METADATA = new BasicType();
  public static final BasicType T_NARROWKLASS = new BasicType();
  public static final BasicType T_CONFLICT = new BasicType();
  public static final BasicType T_ILLEGAL = new BasicType();

  static {
    VM.registerVMInitializedObserver(
        (o, d) -> initialize(VM.getVM().getTypeDataBase()));
  }

  private static synchronized void initialize(TypeDataBase db) {
    T_BOOLEAN.setType(db.lookupIntConstant("T_BOOLEAN").intValue());
    T_CHAR.setType(db.lookupIntConstant("T_CHAR").intValue());
    T_FLOAT.setType(db.lookupIntConstant("T_FLOAT").intValue());
    T_DOUBLE.setType(db.lookupIntConstant("T_DOUBLE").intValue());
    T_BYTE.setType(db.lookupIntConstant("T_BYTE").intValue());
    T_SHORT.setType(db.lookupIntConstant("T_SHORT").intValue());
    T_INT.setType(db.lookupIntConstant("T_INT").intValue());
    T_LONG.setType(db.lookupIntConstant("T_LONG").intValue());
    T_OBJECT.setType(db.lookupIntConstant("T_OBJECT").intValue());
    T_ARRAY.setType(db.lookupIntConstant("T_ARRAY").intValue());
    T_VOID.setType(db.lookupIntConstant("T_VOID").intValue());
    T_ADDRESS.setType(db.lookupIntConstant("T_ADDRESS").intValue());
    T_NARROWOOP.setType(db.lookupIntConstant("T_NARROWOOP").intValue());
    T_METADATA.setType(db.lookupIntConstant("T_METADATA").intValue());
    T_NARROWKLASS.setType(db.lookupIntConstant("T_NARROWKLASS").intValue());
    T_CONFLICT.setType(db.lookupIntConstant("T_CONFLICT").intValue());
    T_ILLEGAL.setType(db.lookupIntConstant("T_ILLEGAL").intValue());
  }

  public static int getTBoolean() {
    return T_BOOLEAN.getType();
  }

  public static int getTChar() {
    return T_CHAR.getType();
  }

  public static int getTFloat() {
    return T_FLOAT.getType();
  }

  public static int getTDouble() {
    return T_DOUBLE.getType();
  }

  public static int getTByte() {
    return T_BYTE.getType();
  }

  public static int getTShort() {
    return T_SHORT.getType();
  }

  public static int getTInt() {
    return T_INT.getType();
  }

  public static int getTLong() {
    return T_LONG.getType();
  }

  public static int getTObject() {
    return T_OBJECT.getType();
  }

  public static int getTArray() {
    return T_ARRAY.getType();
  }

  public static int getTVoid() {
    return T_VOID.getType();
  }

  public static int getTAddress() {
    return T_ADDRESS.getType();
  }

  public static int getTNarrowOop() {
    return T_NARROWOOP.getType();
  }

  public static int getTMetadata() {
    return T_METADATA.getType();
  }

  public static int getTNarrowKlass() {
    return T_NARROWKLASS.getType();
  }

  /** For stack value type with conflicting contents */
  public static int getTConflict() {
    return T_CONFLICT.getType();
  }

  public static int getTIllegal() {
    return T_ILLEGAL.getType();
  }

  public static BasicType intToBasicType(int i) {
    if (i == T_BOOLEAN.getType()) {
      return T_BOOLEAN;
    } else if (i == T_CHAR.getType()) {
      return T_CHAR;
    } else if (i == T_FLOAT.getType()) {
      return T_FLOAT;
    } else if (i == T_DOUBLE.getType()) {
      return T_DOUBLE;
    } else if (i == T_BYTE.getType()) {
      return T_BYTE;
    } else if (i == T_SHORT.getType()) {
      return T_SHORT;
    } else if (i == T_INT.getType()) {
      return T_INT;
    } else if (i == T_LONG.getType()) {
      return T_LONG;
    } else if (i == T_OBJECT.getType()) {
      return T_OBJECT;
    } else if (i == T_ARRAY.getType()) {
      return T_ARRAY;
    } else if (i == T_VOID.getType()) {
      return T_VOID;
    } else if (i == T_ADDRESS.getType()) {
      return T_ADDRESS;
    } else if (i == T_NARROWOOP.getType()) {
      return T_NARROWOOP;
    } else if (i == T_METADATA.getType()) {
      return T_METADATA;
    } else if (i == T_NARROWKLASS.getType()) {
      return T_NARROWKLASS;
    } else {
      return T_ILLEGAL;
    }
  }

  public static BasicType charToBasicType(char c) {
    switch( c ) {
    case 'B': return T_BYTE;
    case 'C': return T_CHAR;
    case 'D': return T_DOUBLE;
    case 'F': return T_FLOAT;
    case 'I': return T_INT;
    case 'J': return T_LONG;
    case 'S': return T_SHORT;
    case 'Z': return T_BOOLEAN;
    case 'V': return T_VOID;
    case 'L': return T_OBJECT;
    case '[': return T_ARRAY;
    }
    return T_ILLEGAL;
  }

  public static int charToType(char c) {
    return charToBasicType(c).getType();
  }

  public int getType() {
    return type;
  }

  public String getName() {
    if (type == T_BOOLEAN.getType()) {
      return "boolean";
    } else if (type == T_CHAR.getType()) {
      return "char";
    } else if (type == T_FLOAT.getType()) {
      return "float";
    } else if (type == T_DOUBLE.getType()) {
      return "double";
    } else if (type == T_BYTE.getType()) {
      return "byte";
    } else if (type == T_SHORT.getType()) {
      return "short";
    } else if (type == T_INT.getType()) {
      return "int";
    } else if (type == T_LONG.getType()) {
      return "long";
    } else if (type == T_OBJECT.getType()) {
      return "object";
    } else if (type == T_ARRAY.getType()) {
      return "array";
    } else if (type == T_VOID.getType()) {
      return "void";
    } else if (type == T_ADDRESS.getType()) {
      return "address";
    } else if (type == T_NARROWOOP.getType()) {
      return "narrow oop";
    } else if (type == T_METADATA.getType()) {
      return "metadata";
    } else if (type == T_NARROWKLASS.getType()) {
      return "narrow klass";
    } else if (type == T_CONFLICT.getType()) {
      return "conflict";
    } else {
      return "ILLEGAL TYPE";
    }
  }

  //-- Internals only below this point
  private BasicType() {
  }

  private void setType(int type) {
    this.type = type;
  }

  private int type;
}
