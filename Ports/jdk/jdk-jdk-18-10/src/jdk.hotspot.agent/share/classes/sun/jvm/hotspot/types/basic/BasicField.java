/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.types.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;

/** A basic implementation of Field which should be suitable for
    cross-platform use. */

public class BasicField implements Field {
  protected BasicTypeDataBase db;
  protected Type type;

  private Type containingType;
  private String name;
  private long size;
  private boolean isStatic;
  /** Used for nonstatic fields only */
  private long offset;
  /** Used for static fields only */
  private Address staticFieldAddress;

  // Copy constructor to create NarrowOopField from OopField.
  public BasicField(Field fld) {
    BasicField field = (BasicField)fld;

    this.db = field.db;
    this.containingType = field.containingType;
    this.name = field.name;
    this.type = field.type;
    this.size = field.size;
    this.isStatic = field.isStatic;
    this.offset = field.offset;
    this.staticFieldAddress = field.staticFieldAddress;
  }
  /** offsetInBytes is ignored if the field is static;
      staticFieldAddress is used only if the field is static. */
  public BasicField(BasicTypeDataBase db, Type containingType, String name, Type type,
                    boolean isStatic, long offsetInBytes, Address staticFieldAddress) {
    this.db = db;
    this.containingType = containingType;
    this.name = name;
    this.type = type;
    this.size = type.getSize();
    this.isStatic = isStatic;
    this.offset = offsetInBytes;
    this.staticFieldAddress = staticFieldAddress;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public long getSize() {
    return size;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public long getOffset() throws WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException("field \"" + name + "\" in class " +
                                   containingType.getName() + " is static");
    }
    return offset;
  }

  public Address getStaticFieldAddress() throws WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException("field \"" + name + "\" in class " +
                                   containingType.getName() + " is not static");
    }
    return staticFieldAddress;
  }

  //--------------------------------------------------------------------------------
  // Dereferencing operations for non-static fields
  //

  public boolean   getJBoolean (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJBooleanAt(offset);
  }
  public byte      getJByte    (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJByteAt(offset);
  }
  public char      getJChar    (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJCharAt(offset);
  }
  public double    getJDouble  (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJDoubleAt(offset);
  }
  public float     getJFloat   (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJFloatAt(offset);
  }
  public int       getJInt     (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJIntAt(offset);
  }
  public long      getJLong    (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJLongAt(offset);
  }
  public short     getJShort   (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getJShortAt(offset);
  }
  public long      getCInteger (Address addr, CIntegerType type)
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getCIntegerAt(offset, type.getSize(), type.isUnsigned());
  }
  public Address   getAddress  (Address addr) throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getAddressAt(offset);
  }
  public OopHandle getOopHandle(Address addr)
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException, NotInHeapException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getOopHandleAt(offset);
  }
  public OopHandle getNarrowOopHandle(Address addr)
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException, NotInHeapException {
    if (isStatic) {
      throw new WrongTypeException();
    }
    return addr.getCompOopHandleAt(offset);
  }

  //--------------------------------------------------------------------------------
  // Dereferencing operations for static fields
  //

  public boolean   getJBoolean () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJBooleanAt(0);
  }
  public byte      getJByte    () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJByteAt(0);
  }
  public char      getJChar    () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJCharAt(0);
  }
  public double    getJDouble  () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJDoubleAt(0);
  }
  public float     getJFloat   () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJFloatAt(0);
  }
  public int       getJInt     () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJIntAt(0);
  }
  public long      getJLong    () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJLongAt(0);
  }
  public short     getJShort   () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getJShortAt(0);
  }
  public long      getCInteger (CIntegerType type)
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getCIntegerAt(0, type.getSize(), type.isUnsigned());
  }
  public Address   getAddress  () throws UnmappedAddressException, UnalignedAddressException, WrongTypeException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getAddressAt(0);
  }
  public OopHandle getOopHandle()
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException, NotInHeapException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getOopHandleAt(0);
  }
  public OopHandle getNarrowOopHandle()
    throws UnmappedAddressException, UnalignedAddressException, WrongTypeException, NotInHeapException {
    if (!isStatic) {
      throw new WrongTypeException();
    }
    return staticFieldAddress.getCompOopHandleAt(0);
  }
}
