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

package sun.jvm.hotspot.debugger.cdbg;

import sun.jvm.hotspot.debugger.*;

/** Models a C or C++ type. Symbols have an associated Type. */

public interface Type {
  public String       getName();
  /** Size of the type in bytes */
  public int          getSize();

  public BitType      asBit();
  public IntType      asInt();
  public EnumType     asEnum();
  public FloatType    asFloat();
  public DoubleType   asDouble();
  public PointerType  asPointer();
  public ArrayType    asArray();
  public RefType      asRef();
  public CompoundType asCompound();
  public FunctionType asFunction();
  public MemberFunctionType asMemberFunction();
  public VoidType     asVoid();

  public boolean      isBit();
  public boolean      isInt();
  public boolean      isEnum();
  public boolean      isFloat();
  public boolean      isDouble();
  public boolean      isPointer();
  public boolean      isArray();
  public boolean      isRef();
  public boolean      isCompound();
  public boolean      isFunction();
  public boolean      isMemberFunction();
  public boolean      isVoid();

  public boolean      isConst();
  public boolean      isVolatile();

  /** Visit an object of this type at the given address with the
      specified visitor */
  public void iterateObject(Address a, ObjectVisitor v);

  /** Alternate visitor which allows end user to specify the
      FieldIdentifier associated with this type (typically for
      visiting locals in a frame) */
  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f);

  /** Returns getName() unless a subclass can return something more
      appropriate */
  public String toString();

  /*
  // Kinds of types

  // Primitive types
  private static final int BIT;    // Specialized integer type with bit offset and size
  private static final int INT;    // Integer type of any size and signedness
  private static final int FLOAT;  // Single-precision floating-point
  private static final int DOUBLE; // Double-precision floating-point

  // Pointer and related types
  private static final int PTR;    // Any pointer type
  private static final int ARRAY;  // Array type with known size
  private static final int REF;    // C++ references

  // Compound types
  private static final int COMPOUND;

  // Function type
  private static final int FUNC;

  // Template types
  private static final int TEMPLATE_CLASS;
  private static final int TEMPLATE_STRUCT;
  private static final int TEMPLATE_UNION;
  private static final int TEMPLATE_FUNCTION;
  */
}
