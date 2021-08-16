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

package sun.jvm.hotspot.debugger.win32.coff;

import java.util.NoSuchElementException;

/** <p> Provides iteration-style access to the types in the
    sstGlobalTypes subsection of the VC++ 5.0 debug
    information. Clients should walk down these platform-dependent
    types and transform them into the platform-independent interfaces
    described in the package sun.jvm.hotspot.debugger.csym. </p>

    <p> This iterator is a "two-dimensional" iterator; it iterates not
    only over all of the types in the type table, but also iterates
    over the leaf types in the current type string. This structure was
    chosen to avoid constructing a new type iterator for each type in
    the type table because of the expected large number of types. </p>
*/

public interface DebugVC50TypeIterator {
  //
  // Iteration through type table
  //

  /** Indicates whether the iteration through the type table is
      complete. */
  public boolean done();

  /** Go to the next type in the type table. NOTE that the iterator is
      pointing at the first type initially, so one should use a while
      (!iter.done()) { ...  iter.next(); } construct.

      @throw NoSuchElementException if the iterator is already done
      and next() is called. */
  public void next() throws NoSuchElementException;

  /** Gets the length, in bytes, of the current type record. */
  public short getLength();

  /** Gets the type index of the current type. This number is
      compatible with type references in symbols and type records. */
  public int getTypeIndex();

  /** Debugging support only */
  public int getNumTypes();

  //
  // Iteration through type strings
  //

  /** Indicates whether iteration through the current type string is
      complete. */
  public boolean typeStringDone();

  /** Goes to the next element in the current type string. NOTE that
      the iterator is pointing at the first type initially, so one
      should use a while (!iter.typeStringDone()) { ...
      iter.typeStringNext(); } construct.

      @throw NoSuchElementException if the iterator is already done
      and typeStringNext() is called. */
  public void typeStringNext() throws NoSuchElementException;

  /** Return the leaf index (see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeLeafIndices})
      for the current element of the current type string. */
  public int typeStringLeaf();

  /** For debugging: returns the file offset of the current type
      string leaf. */
  public int typeStringOffset();

  //
  // Leaf Indices Referenced from Symbols
  //

  ///////////////////////////
  // LF_MODIFIER accessors //
  ///////////////////////////

  // This record is used to indicate the const,r volatile and
  // unaligned properties for any particular type.

  /** Type index of the modified type. */
  public int getModifierIndex();

  /** Attributes specified in MODIFIER_ enums in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getModifierAttribute();

  //////////////////////////
  // LF_POINTER accessors //
  //////////////////////////

  /** Type index of object pointed to. */
  public int getPointerType();

  /** Pointer attributes. Consists of seven bit fields whose
      enumerants are in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}:
      PTRTYPE, PTRMODE, ISFLAT32, VOLATILE, CONST, UNALIGNED, and
      RESTRICT. */
  public int getPointerAttributes();

  /** Only valid if the pointer type is BASED_ON_TYPE; retrieves index
      of type. */
  public int getPointerBasedOnTypeIndex();

  /** Only valid if the pointer type is BASED_ON_TYPE; retrieves name
      of type. */
  public String getPointerBasedOnTypeName();

  /** Only valid if the pointer mode is either PTR_TO_DATA_MEMBER or
      PTR_TO_METHOD; retrieves the type index of the containing
      class. */
  public int getPointerToMemberClass();

  /** Only valid if the pointer mode is either PTR_TO_DATA_MEMBER or
      PTR_TO_METHOD; retrieves the data format of the pointer in
      memory. See the PTR_FORMAT enum in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getPointerToMemberFormat();

  ////////////////////////
  // LF_ARRAY accessors //
  ////////////////////////

  /** Type index of each array element. */
  public int getArrayElementType();

  /** Type index of indexing variable. */
  public int getArrayIndexType();

  /** Length of the array in bytes. */
  public int getArrayLength() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of array. */
  public String getArrayName();

  /////////////////////////////////////////
  // LF_CLASS and LF_STRUCTURE accessors //
  /////////////////////////////////////////

  /** Number of elements in the class or structure. This count
      includes direct, virtual, and indirect virtual bases, and
      methods including overloads, data members, static data members,
      friends, and so on. */
  public short getClassCount();

  /** Property bit field; see PROPERTY_ enumeration in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getClassProperty();

  /** Type index of the field list for this class. */
  public int getClassFieldList();

  /** Get new iterator pointing at the field list of this class. */
  public DebugVC50TypeIterator getClassFieldListIterator();

  /** Type index of the derivation list. This is output by the
      compiler as 0x0000 and is filled in by the CVPACK utility to a
      LF_DERIVED record containing the type indices of those classes
      which immediately inherit the current class. A zero index
      indicates that no derivation information is available. A LF_NULL
      index indicates that the class is not inherited by other
      classes. */
  public int getClassDerivationList();

  /** Type index of the virtual function table shape descriptor. */
  public int getClassVShape();

  /** Numeric leaf specifying size in bytes of the structure. */
  public int getClassSize() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of this type. */
  public String getClassName();

  ////////////////////////
  // LF_UNION accessors //
  ////////////////////////

  /** Number of fields in the union. */
  public short getUnionCount();

  /** Property bit field. */
  public short getUnionProperty();

  /** Type index of field list. */
  public int getUnionFieldList();

  /** Get new iterator pointing at the field list of this union. */
  public DebugVC50TypeIterator getUnionFieldListIterator();

  /** Numeric leaf specifying size in bytes of the union. */
  public int getUnionSize() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of union. */
  public String getUnionName();

  ///////////////////////
  // LF_ENUM accessors //
  ///////////////////////

  /** Number of enumerates. */
  public short getEnumCount();

  /** Property bit field. */
  public short getEnumProperty();

  /** Index of underlying type of enum. */
  public int getEnumType();

  /** Type index of field list. */
  public int getEnumFieldList();

  /** Get new iterator pointing at the field list of this enum. */
  public DebugVC50TypeIterator getEnumFieldListIterator();

  /** Length-prefixed name of enum. */
  public String getEnumName();

  ////////////////////////////
  // LF_PROCEDURE accessors //
  ////////////////////////////

  /** Type index of the value returned by the procedure. */
  public int getProcedureReturnType();

  /** Calling convention of the procedure; see CALLCONV_ enumeration
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public byte getProcedureCallingConvention();

  /** Number of parameters. */
  public short getProcedureNumberOfParameters();

  /** Type index of argument list type record. */
  public int getProcedureArgumentList();

  /** Get new iterator pointing at the argument list of this procedure. */
  public DebugVC50TypeIterator getProcedureArgumentListIterator();

  ////////////////////////////
  // LF_MFUNCTION accessors //
  ////////////////////////////

  /** Type index of the value returned by the procedure. */
  public int getMFunctionReturnType();

  /** Type index of the containing class of the function. */
  public int getMFunctionContainingClass();

  /** Type index of the <b>this</b> parameter of the member function.
      A type of void indicates that the member function is static and
      has no <b>this</b> parameter. */
  public int getMFunctionThis();

  /** Calling convention of the procedure; see CALLCONV_ enumeration
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public byte getMFunctionCallingConvention();

  /** Number of parameters. This count does not include the
      <b>this</b> parameter. */
  public short getMFunctionNumberOfParameters();

  /** List of parameter specifiers. This list does not include the
      <b>this</b> parameter. */
  public int getMFunctionArgumentList();

  /** Get new iterator pointing at the argument list of this member function. */
  public DebugVC50TypeIterator getMFunctionArgumentListIterator();

  /** Logical <b>this</b> adjustor for the method. Whenever a class
      element is referenced via the <b>this</b> pointer, thisadjust
      will be added to the resultant offset before referencing the
      element. */
  public int getMFunctionThisAdjust();

  //////////////////////////
  // LF_VTSHAPE accessors //
  //////////////////////////

  // This record describes the format of a virtual function table.
  // This record is accessed via the vfunctabptr in the member list of
  // the class which introduces the virtual function. The vfunctabptr
  // is defined either by the LF_VFUNCTAB or LF_VFUNCOFF member
  // record. If LF_VFUNCTAB record is used, then vfunctabptr is at the
  // address point of the class. If LF_VFUNCOFF record is used, then
  // vfunctabptr is at the specified offset from the class address
  // point. The underlying type of the pointer is a VTShape type
  // record. This record describes how to interpret the memory at the
  // location pointed to by the virtual function table pointer.

  /** Number of descriptors. */
  public short getVTShapeCount();

  /** Fetch the <i>i</i>th descriptor (0..getVTShapeCount() - 1). Each
      descriptor is a 4-bit (half-byte) value described by the
      VTENTRY_ enumeration in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public int getVTShapeDescriptor(int i);

  //
  // NOTE: LF_COBOL0, LF_COBOL1 accessors elided (FIXME)
  //

  /////////////////////////
  // LF_BARRAY accessors //
  /////////////////////////

  /** Type of each element of the array. */
  public int getBasicArrayType();

  ////////////////////////
  // LF_LABEL accessors //
  ////////////////////////

  /** Addressing mode of the label, described by LABEL_ADDR_MODE_ enum
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getLabelAddressMode();

  //
  // LF_NULL, LF_NOTTRANS have no data
  //

  ///////////////////////////
  // LF_DIMARRAY accessors //
  ///////////////////////////

  /** Underlying type of the array. */
  public int getDimArrayType();

  /** Index of the type record containing the dimension information. */
  public int getDimArrayDimInfo();

  /** Length-prefixed name of the array. */
  public String getDimArrayName();

  //////////////////////////
  // LF_VFTPATH accessors //
  //////////////////////////

  /** Count of number of bases in the path to the virtual function
      table. */
  public int getVFTPathCount();

  /** Type indices of the base classes in the path
      (0..getVFTPathCount() - 1). */
  public int getVFTPathBase(int i);

  //
  // NOTE: LF_PRECOMP and LF_ENDPRECOMP accessors elided because the
  // signature contained within is extremely compiler-specific and is
  // left undefined in the specification, so is not useful. (FIXME)
  //

  //
  // NOTE: LF_OEM accessors elided because we will not need to parse
  // vendor-specific debug information (yet). (FIXME)
  //

  //
  // NOTE: LF_TYPESERVER accessors elided because we will not be using
  // this library in conjunction with a program database. (FIXME)
  //

  //
  // Type Records Referenced from Type Records
  //

  ///////////////////////
  // LF_SKIP accessors //
  ///////////////////////

  /** In processing $$TYPES, the index counter is advanced to index
      count, skipping all intermediate indices. This is the next valid
      index. */
  public int getSkipIndex();

  //////////////////////////
  // LF_ARGLIST accessors //
  //////////////////////////

  /** Count of number of indices in list. */
  public int getArgListCount();

  /** List of type indices (0..getArgListCount() - 1) for describing
      the formal parameters to a function or method. */
  public int getArgListType(int i);

  /////////////////////////
  // LF_DEFARG accessors //
  /////////////////////////

  /** Type index of resulting expression. */
  public int getDefaultArgType();

  /** Length-prefixed string of supplied default expression. */
  public String getDefaultArgExpression();

  //
  // Field list accessors (LF_FIELDLIST)
  //
  // No explicit accessors for the field list. The field list is
  // structured similarly to most type strings; it is a series of
  // leaves. LF_INDEX leaves are used to split the field list if it
  // gets long enough that it will cross a 48K boundary; LF_PAD leaves
  // are used to enforce proper alignment. Both of these leaves, and
  // their lengths, are understood by this iterator, and LF_INDEX
  // leaves have an accessor for reaching the target type record.
  //

  //////////////////////////
  // LF_DERIVED accessors //
  //////////////////////////

  // This type record specifies all of the classes that are directly
  // derived from the class that references this type record.

  /** Number of types in the list. */
  public int getDerivedCount();

  /** Fetch <i>i</i>th derived type (0..getDerivedCount() - 1). */
  public int getDerivedType(int i);

  ///////////////////////////
  // LF_BITFIELD accessors //
  ///////////////////////////

  // Bit fields are represented by an entry in the field list that
  // indexes a bit field type definition.

  /** Type index of the field. */
  public int getBitfieldFieldType();

  /** The length in bits of the object. */
  public byte getBitfieldLength();

  /** Starting position (from bit 0) of the object in the word. */
  public byte getBitfieldPosition();

  ////////////////////////
  // LF_MLIST accessors //
  ////////////////////////

  // This record is typically used to describe overloaded methods,
  // though it can also be used (inefficiently) to describe a single
  // method. It is referenced from the LF_METHOD record. The "count"
  // is not really contained in this record; it is contained within
  // the LF_METHOD record which points to this one. However, it seems
  // it can be inferred from the length of this type string as the
  // only repeated portion of the record is the type of each
  // overloaded variant.
  //
  // Once a method has been found in this list, its symbol is found by
  // qualifying the method name with its class (T::name) and then
  // searching the symbol table for a symbol by that name with the
  // correct type index. Note that the number of repeats is determined
  // by the subleaf of the field list that references this LF_MLIST
  // record.

  /** Attribute of the member function; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums} and {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50MemberAttributes}. */
  public short getMListAttribute();

  /** Number of types corresponding to this overloaded method. FIXME:
      must verify this can be inferred solely from this record's
      length. */
  public int getMListLength();

  /** Type index of the procedure record for the <i>i</i>th occurrence
      of the function (0..getMListLength() - 1). */
  public int getMListType(int i);

  /** Convenience routine indicating whether this member function is
      introducing virtual. */
  public boolean isMListIntroducingVirtual();

  /** Present only when property attribute is introducing virtual
      (optional). Offset in vtable of the class which contains the
      pointer to the function. (FIXME: is this on a per-method or
      per-method list basis? If the latter, will have to provide an
      iterator for this record.) */
  public int getMListVtabOffset();

  //
  // NOTE: LF_DIMCONU, LF_DIMCONLU, LF_DIMVARU, and LF_DIMVARLU
  // accessors elided as these are very likely Fortran-specific
  // (FIXME?)
  //

  /////////////////////////
  // LF_REFSYM accessors //
  /////////////////////////

  // This record is used to describe a symbol that is referenced by a
  // type record. The record is defined because type records cannot
  // reference symbols or locations in the $$SYMBOLS table because
  // global symbol compaction will move symbols.

  /** Create a new SymbolIterator pointing at the copy of the symbol
      this record contains. */
  public DebugVC50SymbolIterator getRefSym();

  //
  // Subfields of complex lists
  //

  /////////////////////////
  // LF_BCLASS accessors //
  /////////////////////////

  // This leaf specifies a real base class. If a class inherits real
  // base classes, the corresponding REAL Base Class records will
  // precede all other member records in the field list of that
  // class. Base class records are emitted in left to right
  // declaration order for real bases.

  /** Member attribute bit field. */
  public short getBClassAttribute();

  /** Index to type record of the class. The class name can be
      obtained from this record. */
  public int getBClassType();

  /** Offset of subobject that represents the base class within the
      structure. */
  public int getBClassOffset() throws DebugVC50WrongNumericTypeException;

  //////////////////////////
  // LF_VBCLASS accessors //
  //////////////////////////

  // This leaf specifies a directly inherited virtual base class. If a
  // class directly inherits virtual base classes, the corresponding
  // Direct Virtual BaseClass records will follow all Real Base Class
  // member records and precede all other member records in the field
  // list of that class. Direct Virtual Base class records are emitted
  // in bottommost left-to-right inheritance order for directly
  // inherited virtual bases.

  /** Member attribute bit field. */
  public short getVBClassAttribute();

  /** Index to type record of the direct or indirect virtual base
      class. The class name can be obtained from this record. */
  public int getVBClassBaseClassType();

  /** Type index of the virtual base pointer for this base. */
  public int getVBClassVirtualBaseClassType();

  /** Numeric leaf specifying the offset of the virtual base pointer
      from the address point of the class for this virtual base. */
  public int getVBClassVBPOff() throws DebugVC50WrongNumericTypeException;

  /** Numeric leaf specifying the index into the virtual base
      displacement table of the entry that contains the displacement
      of the virtual base. The displacement is relative to the address
      point of the class plus vbpoff. */
  public int getVBClassVBOff() throws DebugVC50WrongNumericTypeException;

  ///////////////////////////
  // LF_IVBCLASS accessors //
  ///////////////////////////

  // This leaf specifies indirectly inherited virtual base class. If a
  // class indirectly inherits virtual base classes, the corresponding
  // Indirect Virtual Base Class records will follow all Real Base
  // Class and Direct Virtual Base Class member records and precede
  // all other member records in the field list of that class. Direct
  // Virtual Base class records are emitted in bottommost
  // left-to-right inheritance order for virtual bases.

  /** Member attribute bit field. */
  public short getIVBClassAttribute();

  /** Index to type record of the direct or indirect virtual base
      class. The class name can be obtained from this record. */
  public int getIVBClassBType();

  /** Type index of the virtual base pointer for this base. */
  public int getIVBClassVBPType();

  /** Numeric leaf specifying the offset of the virtual base pointer
      from the address point of the class for this virtual base. */
  public int getIVBClassVBPOff() throws DebugVC50WrongNumericTypeException;

  /** Numeric leaf specifying the index into the virtual base
      displacement table of the entry that contains the displacement
      of the virtual base. The displacement is relative to the address
      point of the class plus vbpoff. */
  public int getIVBClassVBOff() throws DebugVC50WrongNumericTypeException;

  ////////////////////////////
  // LF_ENUMERATE accessors //
  ////////////////////////////

  /** Member attribute bit field. */
  public short getEnumerateAttribute();

  /** Numeric leaf specifying the value of enumerate. */
  public long getEnumerateValue() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of the member field. */
  public String getEnumerateName();

  ////////////////////////////
  // LF_FRIENDFCN accessors //
  ////////////////////////////

  /** Index to type record of the friend function. */
  public int getFriendFcnType();

  /** Length prefixed name of friend function. */
  public String getFriendFcnName();

  ////////////////////////
  // LF_INDEX accessors //
  ////////////////////////

  /** Type index. This field is emitted by the compiler when a complex
      list needs to be split during writing. */
  public int getIndexValue();

  /** Create a new type iterator starting at the above index. */
  public DebugVC50TypeIterator getIndexIterator();

  /////////////////////////
  // LF_MEMBER accessors //
  /////////////////////////

  /** Member attribute bit field. */
  public short getMemberAttribute();

  /** Index to type record for field. */
  public int getMemberType();

  /** Numeric leaf specifying the offset of field in the structure. */
  public int getMemberOffset() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of the member field. */
  public String getMemberName();

  ///////////////////////////
  // LF_STMEMBER accessors //
  ///////////////////////////

  // This leaf specifies a static data member of a class. Once a
  // static data member has been found in this list, its symbol is
  // found by qualifying the name with its class (T::name) and then
  // searching the symbol table for a symbol by that name with the
  // correct type index.

  /** Member attribute bit field. */
  public short getStaticAttribute();

  /** Index to type record for field. */
  public int getStaticType();

  /** Length-prefixed name of the member field. */
  public String getStaticName();

  /////////////////////////
  // LF_METHOD accessors //
  /////////////////////////

  // This leaf specifies the overloaded member functions of a class.
  // This type record can also be used to specify a non-overloaded
  // method but is inefficient. The LF_ONEMETHOD record should be used
  // for non-overloaded methods.

  /** Number of occurrences of function within the class. If the
      function is overloaded then there will be multiple entries in
      the method list. */
  public short getMethodCount();

  /** Type index of method list. */
  public int getMethodList();

  /** Length-prefixed name of method. */
  public String getMethodName();

  /////////////////////////////
  // LF_NESTEDTYPE accessors //
  /////////////////////////////

  /** Type index of nested type. */
  public int getNestedType();

  /** Length-prefixed name of type. */
  public String getNestedName();

  ///////////////////////////
  // LF_VFUNCTAB accessors //
  ///////////////////////////

  // This leaf specifies virtual table pointers within the class. It
  // is a requirement that this record be emitted in the field list
  // before any virtual functions are emitted to the field list.

  /** Index to the pointer record describing the pointer. The pointer
      will in turn have a LF_VTSHAPE type record as the underlying
      type. Note that the offset of the virtual function table pointer
      from the address point of the class is always zero. */
  public int getVFuncTabType();

  ////////////////////////////
  // LF_FRIENDCLS accessors //
  ////////////////////////////

  /** Index to type record of the friend class. The name of the class
      can be obtained from the referenced record. */
  public int getFriendClsType();

  ////////////////////////////
  // LF_ONEMETHOD accessors //
  ////////////////////////////

  /** Method attribute; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums} and
      {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50MemberAttributes}. */
  public short getOneMethodAttribute();

  /** Type index of method. */
  public int getOneMethodType();

  /** Convenience routine indicating whether this method is
      introducing virtual. */
  public boolean isOneMethodIntroducingVirtual();

  /** Offset in virtual function table if introducing virtual method.
      If the method is not an introducing virtual, then this field is
      not present. */
  public int getOneMethodVBaseOff();

  /** Length prefixed name of method. */
  public String getOneMethodName();

  ///////////////////////////
  // LF_VFUNCOFF accessors //
  ///////////////////////////

  // This record is used to specify a virtual function table pointer
  // at a non-zero offset relative to the address point of a class.

  /** Type index of virtual function table pointer. */
  public int getVFuncOffType();

  /** Offset of virtual function table pointer relative to address
      point of class. */
  public int getVFuncOffOffset();

  ///////////////////////////////
  // LF_NESTEDTYPEEX accessors //
  ///////////////////////////////

  // This leaf specifies nested type definition with classes,
  // structures, unions, or enums and includes the protection
  // attributes that are missing in LF_NESTEDTYPE.

  /** Nested type attribute (protection fields are valid). */
  public short getNestedExAttribute();

  /** Type index of nested type. */
  public int getNestedExType();

  /** Length-prefixed name of type. */
  public String getNestedExName();

  ///////////////////////////////
  // LF_MEMBERMODIFY accessors //
  ///////////////////////////////

  /** New protection attributes. */
  public short getMemberModifyAttribute();

  /** Type index of base class that introduced the member. */
  public int getMemberModifyType();

  /** Length-prefixed name of member. */
  public String getMemberModifyName();

  ////////////////////////////
  // Numeric Leaf accessors //
  ////////////////////////////

  /** Fetch the two-byte type (or data, for short integer numeric
      leaves) of the numeric leaf at the given offset, in bytes, from
      the start of the current leaf. */
  public short getNumericTypeAt(int byteOffset);

  /** The size in bytes of the numeric leaf at the given offset, in
      bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if there is no numeric
      leaf at the specified byte offset. */
  public int getNumericLengthAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the integer numeric leaf at the given offset,
      in bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of integer type. */
  public int getNumericIntAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the long or integer numeric leaf at the given
      offset, in bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of long or integer type. */
  public long getNumericLongAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the single-precision floating-point numeric
      leaf at the given offset, in bytes, from the start of the
      current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of 32-bit float type. */
  public float getNumericFloatAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the double-precision floating-point numeric
      leaf at the given offset, in bytes, from the start of the
      current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of 64-bit float type. */
  public double getNumericDoubleAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the raw bytes, including LF_ prefix (if any), of the
      numeric leaf at the given offset, in bytes, from the start of
      the current leaf.

      @throw DebugVC50WrongNumericTypeException if there is no numeric
      leaf at the specified byte offset. */
  public byte[] getNumericDataAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;
}
