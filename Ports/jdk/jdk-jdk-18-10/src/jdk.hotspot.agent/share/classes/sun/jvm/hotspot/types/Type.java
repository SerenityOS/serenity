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

package sun.jvm.hotspot.types;

import java.util.*;

/** This is the top-level interface which describes C++ classes and
    primitive types as well as enough about Java primitive and object
    types to allow the oop hierarchy to be constructed. */

public interface Type {
  public String getName();

  /* The supertype of this type. May be null for top-level classes and
     primitive types. NOTE that multiple inheritance is currently not
     handled. However, it could (probably) be by making this return a
     list and adding appropriate routines to cast an address of one
     type to another. */
  public Type getSuperclass();

  /** The size in bytes, at the machine level, of this type. This
      should be equivalent to the sizeof operator -- i.e., should
      include any compiler-inserted fields like the vtbl for C++
      types. */
  public long getSize();

  /** Indicates whether this type is a C integer type -- regardless of
      size or signedness. */
  public boolean isCIntegerType();

  /** Indicates whether this type is const char*. */
  public boolean isCStringType();

  /** Indicates whether this type is one of the Java primitive types. */
  public boolean isJavaPrimitiveType();

  /** Indicates whether this type is an oop type in the VM. */
  public boolean isOopType();

  /** Indicates whether this type is a pointer type. */
  public boolean isPointerType();

  /** This is permissive and returns the CField regardless of its type
      as long as it is present. Returns null if the field was not
      found, unless throwExceptionIfNotFound is true, in which case a
      RuntimeException is thrown (for debugging purposes). */
  public Field getField(String fieldName, boolean searchSuperclassFields,
                        boolean throwExceptionIfNotFound);

  /** This is permissive and returns the CField regardless of its type
      as long as it is present -- it is equivalent to
      getField(fieldName, searchSuperclassFields, true). Throws a
      RuntimeException if the field was not found. */
  public Field getField(String fieldName, boolean searchSuperclassFields);

  /** This is permissive and returns the CField regardless of its type
      as long as it is present. This does not search superclasses'
      fields; it is equivalent to getField(fieldName, false). Throws a
      RuntimeException if the field was not found. */
  public Field getField(String fieldName);

  /** If there is a mismatch between the declared type and the type
      which would otherwise be returned from this routine, it throws a
      WrongTypeException. declaredType must not be null. Throws a
      RuntimeException if field was otherwise not found. */
  public Field getField(String fieldName, Type declaredType,
                        boolean searchSuperclassFields) throws WrongTypeException;

  /** If there is a mismatch between the declared type and the type
      which would otherwise be returned from this routine, it throws a
      WrongTypeException. declaredType must not be null. This does not
      search superclasses' fields; it is equivalent to
      getField(fieldName, declaredType, false). Throws a
      RuntimeException if field was otherwise not found. */
  public Field getField(String fieldName, Type declaredType) throws WrongTypeException;

  /** Iterate over all of the fields in this type but <B>not</B> in
      any of its superclasses. The returned Iterator's "remove" method
      must not be called. */
  public Iterator getFields();

  /** <P> These accessors are designed to allow strong type checking
      of certain well-known types of fields, specifically Java
      primitive and oop types. Specialized fields for all primitive
      types, as well as oop fields, are required to have strong type
      checking and a WrongTypeException should be thrown if the given
      field is not precisely of the given type. Address and Oop fields
      are more permissive to reduce the complexity of the initial
      implementation. </P>

      <P> These accessors do not search the superclass's fields. </P>
  */
  public JBooleanField       getJBooleanField      (String fieldName) throws WrongTypeException;
  public JByteField          getJByteField         (String fieldName) throws WrongTypeException;
  public JCharField          getJCharField         (String fieldName) throws WrongTypeException;
  public JDoubleField        getJDoubleField       (String fieldName) throws WrongTypeException;
  public JFloatField         getJFloatField        (String fieldName) throws WrongTypeException;
  public JIntField           getJIntField          (String fieldName) throws WrongTypeException;
  public JLongField          getJLongField         (String fieldName) throws WrongTypeException;
  public JShortField         getJShortField        (String fieldName) throws WrongTypeException;
  public CIntegerField       getCIntegerField      (String fieldName) throws WrongTypeException;
  public OopField            getOopField           (String fieldName) throws WrongTypeException;
  public NarrowOopField      getNarrowOopField     (String fieldName) throws WrongTypeException;
  public AddressField        getAddressField       (String fieldName);
}
