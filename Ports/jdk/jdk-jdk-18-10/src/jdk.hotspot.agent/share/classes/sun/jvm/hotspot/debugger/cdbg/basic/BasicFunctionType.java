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

public class BasicFunctionType extends BasicType implements FunctionType {
  private Type returnType;
  private List<Type> argumentTypes;

  public BasicFunctionType(String name, int size, Type returnType) {
    this(name, size, returnType, 0);
  }

  protected BasicFunctionType(String name, int size, Type returnType, int cvAttributes) {
    super(name, size, cvAttributes);
    this.returnType = returnType;
  }

  public FunctionType asFunction()   { return this; }

  public Type getReturnType()        { return returnType; }

  public int  getNumArguments()      { return ((argumentTypes == null) ? 0 : argumentTypes.size()); }
  public Type getArgumentType(int i) {
    return (Type) argumentTypes.get(i);
  }
  public void addArgumentType(Type t) {
    if (argumentTypes == null) {
      argumentTypes = new ArrayList<>();
    }
    argumentTypes.add(t);
  }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    super.resolveTypes(db, listener);
    returnType = db.resolveType(this, returnType, listener, "resolving function return type");
    if (argumentTypes != null) {
      for (ListIterator<Type> iter = argumentTypes.listIterator(); iter.hasNext(); ) {
        iter.set(db.resolveType(this, iter.next(), listener, "resolving function argument types"));
      }
    }
    return this;
  }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {
    // FIXME: nothing to do here? Are we going to provide iteration
    // mechanisms through member functions, and if so what are the
    // types of those functions going to be?
  }

  protected Type createCVVariant(int cvAttributes) {
    BasicFunctionType t = new BasicFunctionType(getName(), getSize(), getReturnType(), cvAttributes);
    t.argumentTypes = argumentTypes;
    return t;
  }

  public void visit(TypeVisitor v) {
    v.doFunctionType(this);
  }
}
