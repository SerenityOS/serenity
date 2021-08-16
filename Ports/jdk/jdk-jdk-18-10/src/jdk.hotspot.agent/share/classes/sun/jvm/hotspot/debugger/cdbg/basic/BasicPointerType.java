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

public class BasicPointerType extends BasicType implements PointerType {
  private Type targetType;

  public BasicPointerType(int size, Type targetType) {
    this(null, size, targetType, 0);
  }

  private BasicPointerType(String name, int size, Type targetType, int cvAttributes) {
    super(name, size, cvAttributes);
    this.targetType = targetType;
    if (!((BasicType) targetType).isLazy()) {
      computeName();
    }
  }

  public PointerType asPointer() { return this; }

  public Type getTargetType() { return targetType; }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    super.resolveTypes(db, listener);
    targetType = db.resolveType(this, targetType, listener, "resolving pointer type");
    computeName();
    return this;
  }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {
    v.doPointer(f, a.getAddressAt(0));
  }

  protected Type createCVVariant(int cvAttributes) {
    return new BasicPointerType(getName(), getSize(), getTargetType(), cvAttributes);
  }

  public void visit(TypeVisitor v) {
    v.doPointerType(this);
  }

  private void computeName() {
    setName(targetType.getName() + " *");
  }
}
