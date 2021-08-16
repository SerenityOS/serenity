/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import sun.jvm.hotspot.debugger.Address;

public interface TypeDataBase {
  /** Equivalent to lookupType(cTypeName, true) */
  public Type lookupType(String cTypeName);

  /** For simplicity of the initial implementation, this is not
      guaranteed to work for primitive types. If throwException is
      true, throws an (unspecified) run-time exception if the type is
      not found. */
  public Type lookupType(String cTypeName, boolean throwException);

  /** Equivalent to lookupIntConstant(constantName, true) */
  public Integer lookupIntConstant(String constantName);

  /* For convenience, this interface also encapsulates the fetching of
     integer constants, i.e., enums. If no constant of this name was
     present, either throws an (unspecified) run-time exception or
     returns null. */
  public Integer lookupIntConstant(String constantName, boolean throwException);

  /** Equivalent to lookupLongConstant(constantName, true) */
  public Long lookupLongConstant(String constantName);

  /* For convenience, this interface also encapsulates the fetching of
     long constants (those requiring 64 bits on 64-bit platforms). If
     no constant of this name was present, either throws an
     (unspecified) run-time exception or returns null. */
  public Long lookupLongConstant(String constantName, boolean throwException);

  /** Accessors for types representing the Java primitive types; used
      for both proper type checking and for walking down Java arrays. */
  public Type getJBooleanType();
  public Type getJByteType();
  public Type getJCharType();
  public Type getJDoubleType();
  public Type getJFloatType();
  public Type getJIntType();
  public Type getJLongType();
  public Type getJShortType();

  /** Returns the size of a C address in bytes. This is currently
      needed in order to properly traverse an array of pointers.
      Traversing an array of structs, for example, is possible by
      looking up the type of the struct and multiplying the index by
      its size when indexing off of a base Address. (FIXME: what about
      alignment?) */
  public long getAddressSize();

  /** Returns the size of an oop in bytes. This is currently needed in
      order to properly traverse an array of oops; it is distinguished
      from the address size, above, because object pointers could
      conceivably have a different representation than direct
      pointers. Traversing an array of structs, for example, is
      possible by looking up the type of the struct and multiplying
      the index by its size when indexing off of a base Address. */
  public long getOopSize();

  /** <P> This is an experimental interface emulating C++'s run-time
      type information (RTTI) mechanism: determines whether the given
      address is a pointer to the start of a C++ object of precisely
      the given type -- it does not search superclasses of the type.
      The convention is that this returns false for the null pointer.
      It is needed to allow wrapper Java objects of the appropriate
      type to be constructed for existing C++ objects of polymorphic
      type. This method is only intended to work for C++ types
      (implying that we should rename this package and the classes
      contained within back to "ctypes"). Further, since the vptr
      offset in an object is known at compile time but not necessarily
      at runtime (unless debugging information is available), it is
      reasonable for an implementation of this method to search nearby
      memory for the (known) vtbl value for the given type. For this
      reason, this method is not intended to support scans through
      memory finding C++ objects, but is instead targeted towards
      discovering the true type of a pointer assumed to be
      intact. </P>

      <P> The reason this method was placed in the type database is
      that the latter is the first level at which it could be exposed,
      and placing it here could avoid modifying the Type interface. It
      is unclear what other, if any, vtbl access would be useful (or
      implementable), so we are trying to avoid changing interfaces at
      this point to support this kind of functionality. </P>

      <P> This method necessarily does not support multiple
      inheritance. </P> */
  public boolean addressTypeIsEqualToType(Address addr, Type type);

  /** Helper routine for guessing the most derived type of a
      polymorphic C++ object. Iterates the type database calling
      addressTypeIsEqualToType for all known types. Returns a matching
      Type for the given address if one was found, or null if none was
      found. */
  public Type guessTypeForAddress(Address addr);

  /** Helper routine for guessing the most derived type of a
      polymorphic C++ object. Requires a baseType that must be virtual
      so that lookup can be performed without false positives */
  public Type findDynamicTypeForAddress(Address addr, Type baseType);

  /** Returns an Iterator over the Types in the database. */
  public Iterator getTypes();

  /** Returns an Iterator over the String names of the integer
      constants in the database. */
  public Iterator getIntConstants();

  /** Returns an Iterator over the String names of the long constants
      in the database. */
  public Iterator getLongConstants();
}
