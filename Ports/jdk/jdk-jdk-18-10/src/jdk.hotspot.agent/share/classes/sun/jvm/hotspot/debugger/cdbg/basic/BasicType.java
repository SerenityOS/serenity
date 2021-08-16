/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.cdbg.basic;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

public abstract class BasicType implements Type, CVAttributes {
  private String name;
  private int    size;
  private int    cvAttributes;
  // Types keep a list of const/volatile qualified variants of themselves
  private List<Type> cvVariants;

  protected BasicType(String name, int size) {
    this(name, size, 0);
  }

  protected BasicType(String name, int size, int cvAttributes) {
    this.name = name;
    this.size = size;
    this.cvAttributes = cvAttributes;
  }

  public String       getName()    { return name; }

  /** For use during resolution only */
  protected void      setName(String name) { this.name = name; }

  public int          getSize()    { return size; }

  public BitType      asBit()      { return null; }
  public IntType      asInt()      { return null; }
  public EnumType     asEnum()     { return null; }
  public FloatType    asFloat()    { return null; }
  public DoubleType   asDouble()   { return null; }
  public PointerType  asPointer()  { return null; }
  public ArrayType    asArray()    { return null; }
  public RefType      asRef()      { return null; }
  public CompoundType asCompound() { return null; }
  public FunctionType asFunction() { return null; }
  public MemberFunctionType asMemberFunction() { return null; }
  public VoidType     asVoid()     { return null; }

  public boolean      isBit()      { return (asBit()      != null); }
  public boolean      isInt()      { return (asInt()      != null); }
  public boolean      isEnum()     { return (asEnum()     != null); }
  public boolean      isFloat()    { return (asFloat()    != null); }
  public boolean      isDouble()   { return (asDouble()   != null); }
  public boolean      isPointer()  { return (asPointer()  != null); }
  public boolean      isArray()    { return (asArray()    != null); }
  public boolean      isRef()      { return (asRef()      != null); }
  public boolean      isCompound() { return (asCompound() != null); }
  public boolean      isFunction() { return (asFunction() != null); }
  public boolean      isMemberFunction() { return (asMemberFunction() != null); }
  public boolean      isVoid()     { return (asVoid()     != null); }

  public boolean      isConst()    { return ((cvAttributes & CONST) != 0); }
  public boolean      isVolatile() { return ((cvAttributes & VOLATILE) != 0); }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    if (cvVariants != null) {
      for (ListIterator<Type> iter = cvVariants.listIterator(); iter.hasNext(); ) {
        iter.set(db.resolveType(this, iter.next(), listener, "resolving const/var variants"));
      }
    }
    return this;
  }
  public boolean       isLazy() { return false; }
  public void          iterateObject(Address a, ObjectVisitor v) {
    iterateObject(a, v, null);
  }
  public abstract void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f);
  public Type          getCVVariant(int cvAttributes) {
    Type t = findCVVariant(cvAttributes);
    if (t != null) return t;
    t = createCVVariant(cvAttributes);
    addCVVariant(t);
    return t;
  }

  public String toString() {
    return getName();
  }

  private   int           getCVAttributes() { return cvAttributes; }
  protected abstract Type createCVVariant(int cvAttributes);
  protected Type          findCVVariant(int cvAttributes) {
    if (cvVariants != null) {
      for (Iterator<Type> iter = cvVariants.iterator(); iter.hasNext(); ) {
        BasicType t = (BasicType) iter.next();
        if (t.getCVAttributes() == cvAttributes) return t;
      }
    }
    return null;
  }
  protected void addCVVariant(Type t) {
    if (cvVariants == null) {
      cvVariants = new ArrayList<>();
    }
    cvVariants.add(t);
  }

  public abstract void visit(TypeVisitor v);
}
