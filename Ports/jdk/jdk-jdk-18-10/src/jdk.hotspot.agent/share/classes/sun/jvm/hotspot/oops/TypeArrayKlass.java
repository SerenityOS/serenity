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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// TypeArrayKlass is a proxy for TypeArrayKlass in the JVM

public class TypeArrayKlass extends ArrayKlass {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type t             = db.lookupType("TypeArrayKlass");
    maxLength          = new IntField(t.getJIntField("_max_length"), 0);
  }

  public TypeArrayKlass(Address addr) {
    super(addr);
  }

  private static IntField  maxLength;

  public long getMaxLength()          { return  maxLength.getValue(this); }

  public static final int T_BOOLEAN = 4;
  public static final int T_CHAR    = 5;
  public static final int T_FLOAT   = 6;
  public static final int T_DOUBLE  = 7;
  public static final int T_BYTE    = 8;
  public static final int T_SHORT   = 9;
  public static final int T_INT     = 10;
  public static final int T_LONG    = 11;

  public String getTypeName() {
    switch ((int) getElementType()) {
      case T_BOOLEAN: return "[Z";
      case T_CHAR:    return "[C";
      case T_FLOAT:   return "[F";
      case T_DOUBLE:  return "[D";
      case T_BYTE:    return "[B";
      case T_SHORT:   return "[S";
      case T_INT:     return "[I";
      case T_LONG:    return "[J";
    }
    return "Unknown TypeArray";
  }

  public String getElementTypeName() {
    switch ((int) getElementType()) {
      case T_BOOLEAN: return "boolean";
      case T_CHAR:    return "char";
      case T_FLOAT:   return "float";
      case T_DOUBLE:  return "double";
      case T_BYTE:    return "byte";
      case T_SHORT:   return "short";
      case T_INT:     return "int";
      case T_LONG:    return "long";
    }
    throw new RuntimeException("should not reach here");
  }

  public void printValueOn(PrintStream tty) {
    tty.print("TypeArrayKlass for " + getTypeName());
  }

  public void iterateFields(MetadataVisitor visitor) {
    super.iterateFields(visitor);
      visitor.doInt(maxLength, true);
    }

  public Klass arrayKlassImpl(boolean orNull, int n) {
    int dimension = (int) getDimension();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(dimension <= n, "check order of chain");
    }
    if (dimension == n)
      return this;
    ObjArrayKlass ak = (ObjArrayKlass) getHigherDimension();
    if (ak == null) {
      if (orNull) return null;
      // FIXME: would need to change in reflective system to actually
      // allocate klass
      throw new RuntimeException("Can not allocate array klasses in debugging system");
    }
    if (orNull) {
      return ak.arrayKlassOrNull(n);
    }
    return ak.arrayKlass(n);
  }

  public Klass arrayKlassImpl(boolean orNull) {
    return arrayKlassImpl(orNull, (int) (getDimension() + 1));
  }
}
