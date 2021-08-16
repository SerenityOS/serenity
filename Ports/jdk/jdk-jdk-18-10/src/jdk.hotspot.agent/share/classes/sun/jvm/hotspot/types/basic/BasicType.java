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

package sun.jvm.hotspot.types.basic;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

/** <P> This is a basic implementation of the Type interface which
    should be complete enough to be portable across platforms. The
    only issue will be the construction of these objects and their
    components from the platform-specific debugging information; see
    BasicTypeDataBase. </P>

    <P> There are two types of clients of this class. The first is
    that which builds the TypeDatabase. This kind of client uses the
    additional public methods beyond those in the Type interface to
    properly configure the BasicType objects. The second is the
    consumer of these types; this kind of client should deal only with
    the Type interfaces. </P> */

public class BasicType implements Type {
  protected BasicTypeDataBase db;

  private String name;
  private long size;
  private boolean isJavaPrimitiveType;
  private boolean isOopType;
  // These are only the fields defined in this class, not any of this
  // class's superclasses.
  private Map<String, Field> nameToFieldMap = new HashMap<>();
  private List<Field> fieldList = new LinkedList<>();
  // Superclass, or null if none. Primitive types do not have any
  // inheritance relationship.
  private Type superclass;

  /** superclass may be null */
  public BasicType(BasicTypeDataBase db, String name, Type superclass) {
    if (name == null) {
      throw new IllegalArgumentException("name may not be null");
    }
    this.db = db;
    this.name = name;
    this.superclass = superclass;
  }

  /** Equivalent to BasicType(db, name, null) */
  public BasicType(BasicTypeDataBase db, String name) {
    this(db, name, null);
  }

  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof BasicType)) {
      return false;
    }

    BasicType arg = (BasicType) obj;

    if (!name.equals(arg.name)) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    return name.hashCode();
  }

  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }

  /** This should only be called at most once, and only by the builder
      of the type database */
  public void setSuperclass(Type superclass) {
    this.superclass = superclass;
  }

  public Type getSuperclass() {
    return superclass;
  }

  /** This should only be called at most once, and only by the builder
      of the type database */
  public void setSize(long sizeInBytes) {
    this.size = sizeInBytes;
  }

  public long getSize() {
    return size;
  }

  /** Overridden by BasicCIntegerType */
  public boolean isCIntegerType() {
    return false;
  }

  public boolean isCStringType() {
    if (isPointerType()) {
      Type target = ((PointerType)this).getTargetType();
      return target.isCIntegerType() &&
             target.getName().equals("const char");
    } else {
      return false;
    }
  }

  public boolean isJavaPrimitiveType() {
    return isJavaPrimitiveType;
  }

  /** This should only be called at most once, and only by the builder
      of the type database */
  public void setIsJavaPrimitiveType(boolean isJavaPrimitiveType) {
    this.isJavaPrimitiveType = isJavaPrimitiveType;
  }

  public boolean isOopType() {
    return isOopType;
  }

  /** Overridden by BasicPointerType */
  public boolean isPointerType() {
    return false;
  }

  /** This should only be called at most once, and only by the builder
      of the type database */
  public void setIsOopType(boolean isOopType) {
    this.isOopType = isOopType;
  }

  public Field getField(String fieldName, boolean searchSuperclassFields,
                        boolean throwExceptionIfNotFound) {
    Field field = null;
    if (nameToFieldMap != null) {
      field = (Field) nameToFieldMap.get(fieldName);

      if (field != null) {
        return field;
      }
    }

    if (searchSuperclassFields) {
      if (superclass != null) {
        field = superclass.getField(fieldName, searchSuperclassFields, false);
      }
    }

    if (field == null && throwExceptionIfNotFound) {
      throw new RuntimeException("field \"" + fieldName + "\" not found in type " + name);
    }

    return field;
  }

  public Field getField(String fieldName, boolean searchSuperclassFields) {
    return getField(fieldName, searchSuperclassFields, true);
  }

  public Field getField(String fieldName) {
    return getField(fieldName, true);
  }

  public Field getField(String fieldName, Type declaredType,
                        boolean searchSuperclassFields) throws WrongTypeException {
    Field res = getField(fieldName, searchSuperclassFields);
    if (res == null) {
      return null;
    }
    if (!res.getType().equals(declaredType)) {
      throw new WrongTypeException("field \"" + fieldName + "\" in type " + name +
                                    " is not of type " + declaredType +
                                    ", but instead of type " + res.getType());
    }
    return res;
  }

  public Field getField(String fieldName, Type declaredType) throws WrongTypeException {
    return getField(fieldName, declaredType, false);
  }

  /** The returned iterator's "remove" method must not be called */
  public Iterator getFields() {
    return new ConstIterator(fieldList.iterator());
  }

  //--------------------------------------------------------------------------------
  // Specialized field type accessors
  //

  public JBooleanField getJBooleanField(String fieldName) throws WrongTypeException {
    return (JBooleanField) getField(fieldName, db.getJBooleanType());
  }

  public JByteField    getJByteField(String fieldName) throws WrongTypeException {
    return (JByteField) getField(fieldName, db.getJByteType());
  }

  public JCharField    getJCharField(String fieldName) throws WrongTypeException {
    return (JCharField) getField(fieldName, db.getJCharType());
  }

  public JDoubleField  getJDoubleField(String fieldName) throws WrongTypeException {
    return (JDoubleField) getField(fieldName, db.getJDoubleType());
  }

  public JFloatField   getJFloatField(String fieldName) throws WrongTypeException {
    return (JFloatField) getField(fieldName, db.getJFloatType());
  }

  public JIntField     getJIntField(String fieldName) throws WrongTypeException {
    return (JIntField) getField(fieldName, db.getJIntType());
  }

  public JLongField    getJLongField(String fieldName) throws WrongTypeException {
    return (JLongField) getField(fieldName, db.getJLongType());
  }

  public JShortField   getJShortField(String fieldName) throws WrongTypeException {
    return (JShortField) getField(fieldName, db.getJShortType());
  }

  public CIntegerField getCIntegerField(String fieldName) throws WrongTypeException {
    Field field = getField(fieldName);
    if (!(field.getType() instanceof CIntegerType)) {
      throw new WrongTypeException("field \"" + fieldName + "\" in type " + name +
                                   " is not of C integer type, but instead of type " +
                                   field.getType());
    }
    return (CIntegerField) field;
  }

  public OopField getOopField(String fieldName) throws WrongTypeException {
    Field field = getField(fieldName);
    if (!field.getType().isOopType()) {
      throw new WrongTypeException("field \"" + fieldName + "\" in type " + name +
                                   " is not of oop type, but instead of type " +
                                   field.getType());
    }
    return (OopField) field;
  }

  public NarrowOopField getNarrowOopField(String fieldName) throws WrongTypeException {
    return (NarrowOopField) new BasicNarrowOopField(getOopField(fieldName));
  }

  public AddressField getAddressField(String fieldName) {
    // This type can not be inferred (for now), so provide a wrapper
    Field field = getField(fieldName);
    if (field == null) {
      return null;
    }
    return new BasicAddressFieldWrapper(field);
  }

  /** This method should only be used by the builder of the
      TypeDataBase. Throws a RuntimeException if a field with this
      name was already present in this class. */
  public void addField(Field field) {
    if (nameToFieldMap.get(field.getName()) != null) {
      throw new RuntimeException("field of name \"" + field.getName() + "\" already present in type " + this);
    }

    nameToFieldMap.put(field.getName(), field);
    fieldList.add(field);
  }

  /** This method should only be used by the builder of the
      TypeDataBase. Throws a RuntimeException if a field with this
      name was not present in this class. */
  public void removeField(Field field) {
    if (nameToFieldMap.remove(field.getName()) == null) {
      throw new RuntimeException("field of name \"" + field.getName() + "\" was not present");
    }
    fieldList.remove(field);
  }
}
