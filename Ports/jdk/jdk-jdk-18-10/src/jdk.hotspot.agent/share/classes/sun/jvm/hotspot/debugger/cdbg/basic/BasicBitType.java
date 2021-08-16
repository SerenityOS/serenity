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
import sun.jvm.hotspot.utilities.Assert;

public class BasicBitType extends BasicIntType implements BitType {
  // Integer type or lazy type
  private Type underlyingType;
  private int sizeInBits;
  private int offset;

  /** Underlying type of enum must be an integer type (or as yet
      unresolved) */

  public BasicBitType(Type underlyingType, int sizeInBits, int lsbOffset) {
    this(underlyingType, sizeInBits, lsbOffset, 0);
  }

  private BasicBitType(Type underlyingType, int sizeInBits, int lsbOffset, int cvAttributes) {
    super(null, 0, false, cvAttributes);
    this.underlyingType = underlyingType;
    this.sizeInBits = sizeInBits;
    this.offset = lsbOffset;
  }

  public BitType asBit() { return this; }

  public int     getSize() { return underlyingType.getSize(); }
  public boolean isUnsigned() {
    if (underlyingType.isInt()) {
      return ((IntType) underlyingType).isUnsigned();
    }
    return false;
  }

  public int getSizeInBits() {
    return sizeInBits;
  }

  public int getOffset() {
    return offset;
  }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    super.resolveTypes(db, listener);
    underlyingType = db.resolveType(this, underlyingType, listener, "resolving bit type");
    setName(underlyingType.getName());
    if (Assert.ASSERTS_ENABLED) {
      BasicType b = (BasicType) underlyingType;
      Assert.that(b.isLazy() || b.isInt(),
                  "Underlying type of bitfield must be integer type (or unresolved due to error)");
    }
    return this;
  }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {
    long mask = maskFor(sizeInBits);
    long val = ((a.getCIntegerAt(0, getSize(), isUnsigned())) >> getOffset()) & mask;
    if (!isUnsigned()) {
      if ((val & highBit(sizeInBits)) != 0) {
        // Must sign extend
        val = val | (~mask);
      }
    }
    v.doBit(f, val);
  }

  protected Type createCVVariant(int cvAttributes) {
    return new BasicBitType(underlyingType, getSizeInBits(), getOffset(), cvAttributes);
  }

  public void visit(TypeVisitor v) {
    v.doBitType(this);
  }

  private static long maskFor(int sizeInBits) {
    return ((1 << sizeInBits) - 1);
  }

  private static long highBit(int sizeInBits) {
    return (1 << (sizeInBits - 1));
  }
}
