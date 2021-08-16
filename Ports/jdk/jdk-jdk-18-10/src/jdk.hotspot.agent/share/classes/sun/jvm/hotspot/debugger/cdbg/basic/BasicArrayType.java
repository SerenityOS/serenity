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

package sun.jvm.hotspot.debugger.cdbg.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

public class BasicArrayType extends BasicType implements ArrayType {
  private Type elementType;
  private int  length;

  public BasicArrayType(String name, Type elementType, int sizeInBytes) {
    this(name, elementType, sizeInBytes, 0, 0);
  }

  private BasicArrayType(String name, Type elementType, int sizeInBytes, int length, int cvAttributes) {
    super(name, sizeInBytes, cvAttributes);
    this.elementType = elementType;
    this.length      = length;
  }

  public ArrayType asArray() { return this; }

  public Type getElementType() { return elementType; }
  public int  getLength()      { return length; }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    super.resolveTypes(db, listener);
    elementType = db.resolveType(this, elementType, listener, "resolving array element type");
    // FIXME: need to figure out whether we have to align object sizes
    // ourselves (see below)
    if (!((BasicType) elementType).isLazy()) {
      length = getSize() / elementType.getSize();
    }
    return this;
  }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {
    // What kind of iteration are we doing? If the end user requested
    // iteration over a given array at a given address, the field
    // identifier will be null, and we should descend and iterate
    // through the array. Otherwise, we are already iterating through
    // an object, and it is up to the end user whether to descend into
    // the embedded object.

    if (f == null) {
      v.enterType(this, a);
      for (int i = 0; i < getLength(); i++) {
        // FIXME: need to figure out whether we need to align object
        // sizes ourselves (i.e., round up to doubleword size) or
        // whether that reported in the debug info is already aligned
        // (in Microsoft's compiler, I think the struct alignment is
        // an option, so it's possible that there is nothing we are
        // allowed to do here)
        ((BasicType) getElementType()).iterateObject(a.addOffsetTo(i * getElementType().getSize()),
                                                     v,
                                                     new BasicIndexableFieldIdentifier(getElementType(), i));
      }
      v.exitType();
    } else {
      v.doArray(f, a);
    }
  }

  protected Type createCVVariant(int cvAttributes) {
    return new BasicArrayType(getName(), getElementType(), getSize(), getLength(), cvAttributes);
  }

  public void visit(TypeVisitor v) {
    v.doArrayType(this);
  }
}
