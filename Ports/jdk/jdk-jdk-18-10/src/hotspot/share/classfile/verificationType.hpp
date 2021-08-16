/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_VERIFICATIONTYPE_HPP
#define SHARE_CLASSFILE_VERIFICATIONTYPE_HPP

#include "oops/instanceKlass.hpp"
#include "oops/oop.hpp"
#include "oops/symbol.hpp"
#include "runtime/handles.hpp"
#include "runtime/signature.hpp"

enum {
  // As specifed in the JVM spec
  ITEM_Top = 0,
  ITEM_Integer = 1,
  ITEM_Float = 2,
  ITEM_Double = 3,
  ITEM_Long = 4,
  ITEM_Null = 5,
  ITEM_UninitializedThis = 6,
  ITEM_Object = 7,
  ITEM_Uninitialized = 8,
  ITEM_Bogus = (uint)-1
};

class ClassVerifier;

class VerificationType {
  private:
    // Least significant bits of _handle are always 0, so we use these as
    // the indicator that the _handle is valid.  Otherwise, the _data field
    // contains encoded data (as specified below).  Should the VM change
    // and the lower bits on oops aren't 0, the assert in the constructor
    // will catch this and we'll have to add a descriminator tag to this
    // structure.
    union {
      Symbol*   _sym;
      uintptr_t _data;
    } _u;

    enum {
      // These rest are not found in classfiles, but used by the verifier
      ITEM_Boolean = 9, ITEM_Byte, ITEM_Short, ITEM_Char,
      ITEM_Long_2nd, ITEM_Double_2nd
    };

    // Enum for the _data field
    enum {
      // Bottom two bits determine if the type is a reference, primitive,
      // uninitialized or a query-type.
      TypeMask           = 0x00000003,

      // Topmost types encoding
      Reference          = 0x0,        // _sym contains the name
      Primitive          = 0x1,        // see below for primitive list
      Uninitialized      = 0x2,        // 0x00ffff00 contains bci
      TypeQuery          = 0x3,        // Meta-types used for category testing

      // Utility flags
      ReferenceFlag      = 0x00,       // For reference query types
      Category1Flag      = 0x01,       // One-word values
      Category2Flag      = 0x02,       // First word of a two-word value
      Category2_2ndFlag  = 0x04,       // Second word of a two-word value

      // special reference values
      Null               = 0x00000000, // A reference with a 0 sym is null

      // Primitives categories (the second byte determines the category)
      Category1          = (Category1Flag     << 1 * BitsPerByte) | Primitive,
      Category2          = (Category2Flag     << 1 * BitsPerByte) | Primitive,
      Category2_2nd      = (Category2_2ndFlag << 1 * BitsPerByte) | Primitive,

      // Primitive values (type descriminator stored in most-signifcant bytes)
      // Bogus needs the " | Primitive".  Else, is_reference(Bogus) returns TRUE.
      Bogus              = (ITEM_Bogus      << 2 * BitsPerByte) | Primitive,
      Boolean            = (ITEM_Boolean    << 2 * BitsPerByte) | Category1,
      Byte               = (ITEM_Byte       << 2 * BitsPerByte) | Category1,
      Short              = (ITEM_Short      << 2 * BitsPerByte) | Category1,
      Char               = (ITEM_Char       << 2 * BitsPerByte) | Category1,
      Integer            = (ITEM_Integer    << 2 * BitsPerByte) | Category1,
      Float              = (ITEM_Float      << 2 * BitsPerByte) | Category1,
      Long               = (ITEM_Long       << 2 * BitsPerByte) | Category2,
      Double             = (ITEM_Double     << 2 * BitsPerByte) | Category2,
      Long_2nd           = (ITEM_Long_2nd   << 2 * BitsPerByte) | Category2_2nd,
      Double_2nd         = (ITEM_Double_2nd << 2 * BitsPerByte) | Category2_2nd,

      // Used by Uninitialized (second and third bytes hold the bci)
      BciMask            = 0xffff << 1 * BitsPerByte,
      BciForThis         = ((u2)-1),   // A bci of -1 is an Unintialized-This

      // Query values
      ReferenceQuery     = (ReferenceFlag     << 1 * BitsPerByte) | TypeQuery,
      Category1Query     = (Category1Flag     << 1 * BitsPerByte) | TypeQuery,
      Category2Query     = (Category2Flag     << 1 * BitsPerByte) | TypeQuery,
      Category2_2ndQuery = (Category2_2ndFlag << 1 * BitsPerByte) | TypeQuery
    };

  VerificationType(uintptr_t raw_data) {
    _u._data = raw_data;
  }

 public:

  VerificationType() { *this = bogus_type(); }

  // Create verification types
  static VerificationType bogus_type() { return VerificationType(Bogus); }
  static VerificationType top_type() { return bogus_type(); } // alias
  static VerificationType null_type() { return VerificationType(Null); }
  static VerificationType integer_type() { return VerificationType(Integer); }
  static VerificationType float_type() { return VerificationType(Float); }
  static VerificationType long_type() { return VerificationType(Long); }
  static VerificationType long2_type() { return VerificationType(Long_2nd); }
  static VerificationType double_type() { return VerificationType(Double); }
  static VerificationType boolean_type() { return VerificationType(Boolean); }
  static VerificationType byte_type() { return VerificationType(Byte); }
  static VerificationType char_type() { return VerificationType(Char); }
  static VerificationType short_type() { return VerificationType(Short); }
  static VerificationType double2_type()
    { return VerificationType(Double_2nd); }

  // "check" types are used for queries.  A "check" type is not assignable
  // to anything, but the specified types are assignable to a "check".  For
  // example, any category1 primitive is assignable to category1_check and
  // any reference is assignable to reference_check.
  static VerificationType reference_check()
    { return VerificationType(ReferenceQuery); }
  static VerificationType category1_check()
    { return VerificationType(Category1Query); }
  static VerificationType category2_check()
    { return VerificationType(Category2Query); }
  static VerificationType category2_2nd_check()
    { return VerificationType(Category2_2ndQuery); }

  // For reference types, store the actual Symbol
  static VerificationType reference_type(Symbol* sh) {
      assert(((uintptr_t)sh & 0x3) == 0, "Symbols must be aligned");
      // If the above assert fails in the future because oop* isn't aligned,
      // then this type encoding system will have to change to have a tag value
      // to descriminate between oops and primitives.
      return VerificationType((uintptr_t)sh);
  }
  static VerificationType uninitialized_type(u2 bci)
    { return VerificationType(bci << 1 * BitsPerByte | Uninitialized); }
  static VerificationType uninitialized_this_type()
    { return uninitialized_type(BciForThis); }

  // Create based on u1 read from classfile
  static VerificationType from_tag(u1 tag);

  bool is_bogus() const     { return (_u._data == Bogus); }
  bool is_null() const      { return (_u._data == Null); }
  bool is_boolean() const   { return (_u._data == Boolean); }
  bool is_byte() const      { return (_u._data == Byte); }
  bool is_char() const      { return (_u._data == Char); }
  bool is_short() const     { return (_u._data == Short); }
  bool is_integer() const   { return (_u._data == Integer); }
  bool is_long() const      { return (_u._data == Long); }
  bool is_float() const     { return (_u._data == Float); }
  bool is_double() const    { return (_u._data == Double); }
  bool is_long2() const     { return (_u._data == Long_2nd); }
  bool is_double2() const   { return (_u._data == Double_2nd); }
  bool is_reference() const { return ((_u._data & TypeMask) == Reference); }
  bool is_category1() const {
    // This should return true for all one-word types, which are category1
    // primitives, and references (including uninitialized refs).  Though
    // the 'query' types should technically return 'false' here, if we
    // allow this to return true, we can perform the test using only
    // 2 operations rather than 8 (3 masks, 3 compares and 2 logical 'ands').
    // Since noone should call this on a query type anyway, this is ok.
    assert(!is_check(), "Must not be a check type (wrong value returned)");
    return ((_u._data & Category1) != Primitive);
    // should only return false if it's a primitive, and the category1 flag
    // is not set.
  }
  bool is_category2() const { return ((_u._data & Category2) == Category2); }
  bool is_category2_2nd() const {
    return ((_u._data & Category2_2nd) == Category2_2nd);
  }
  bool is_reference_check() const { return _u._data == ReferenceQuery; }
  bool is_category1_check() const { return _u._data == Category1Query; }
  bool is_category2_check() const { return _u._data == Category2Query; }
  bool is_category2_2nd_check() const { return _u._data == Category2_2ndQuery; }
  bool is_check() const { return (_u._data & TypeQuery) == TypeQuery; }

  bool is_x_array(char sig) const {
    return is_null() || (is_array() && (name()->char_at(1) == sig));
  }
  bool is_int_array() const { return is_x_array(JVM_SIGNATURE_INT); }
  bool is_byte_array() const { return is_x_array(JVM_SIGNATURE_BYTE); }
  bool is_bool_array() const { return is_x_array(JVM_SIGNATURE_BOOLEAN); }
  bool is_char_array() const { return is_x_array(JVM_SIGNATURE_CHAR); }
  bool is_short_array() const { return is_x_array(JVM_SIGNATURE_SHORT); }
  bool is_long_array() const { return is_x_array(JVM_SIGNATURE_LONG); }
  bool is_float_array() const { return is_x_array(JVM_SIGNATURE_FLOAT); }
  bool is_double_array() const { return is_x_array(JVM_SIGNATURE_DOUBLE); }
  bool is_object_array() const { return is_x_array(JVM_SIGNATURE_CLASS); }
  bool is_array_array() const { return is_x_array(JVM_SIGNATURE_ARRAY); }
  bool is_reference_array() const
    { return is_object_array() || is_array_array(); }
  bool is_object() const
    { return (is_reference() && !is_null() && name()->utf8_length() >= 1 &&
              name()->char_at(0) != JVM_SIGNATURE_ARRAY); }
  bool is_array() const
    { return (is_reference() && !is_null() && name()->utf8_length() >= 2 &&
              name()->char_at(0) == JVM_SIGNATURE_ARRAY); }
  bool is_uninitialized() const
    { return ((_u._data & Uninitialized) == Uninitialized); }
  bool is_uninitialized_this() const
    { return is_uninitialized() && bci() == BciForThis; }

  VerificationType to_category2_2nd() const {
    assert(is_category2(), "Must be a double word");
    return VerificationType(is_long() ? Long_2nd : Double_2nd);
  }

  u2 bci() const {
    assert(is_uninitialized(), "Must be uninitialized type");
    return ((_u._data & BciMask) >> 1 * BitsPerByte);
  }

  Symbol* name() const {
    assert(is_reference() && !is_null(), "Must be a non-null reference");
    return _u._sym;
  }

  bool equals(const VerificationType& t) const {
    return (_u._data == t._u._data ||
      (is_reference() && t.is_reference() && !is_null() && !t.is_null() &&
       name() == t.name()));
  }

  bool operator ==(const VerificationType& t) const {
    return equals(t);
  }

  bool operator !=(const VerificationType& t) const {
    return !equals(t);
  }

  // The whole point of this type system - check to see if one type
  // is assignable to another.  Returns true if one can assign 'from' to
  // this.
  bool is_assignable_from(
      const VerificationType& from, ClassVerifier* context,
      bool from_field_is_protected, TRAPS) const {
    if (equals(from) || is_bogus()) {
      return true;
    } else {
      switch(_u._data) {
        case Category1Query:
          return from.is_category1();
        case Category2Query:
          return from.is_category2();
        case Category2_2ndQuery:
          return from.is_category2_2nd();
        case ReferenceQuery:
          return from.is_reference() || from.is_uninitialized();
        case Boolean:
        case Byte:
        case Char:
        case Short:
          // An int can be assigned to boolean, byte, char or short values.
          return from.is_integer();
        default:
          if (is_reference() && from.is_reference()) {
            return is_reference_assignable_from(from, context,
                                                from_field_is_protected,
                                                THREAD);
          } else {
            return false;
          }
      }
    }
  }

  // Check to see if one array component type is assignable to another.
  // Same as is_assignable_from() except int primitives must be identical.
  bool is_component_assignable_from(
      const VerificationType& from, ClassVerifier* context,
      bool from_field_is_protected, TRAPS) const {
    if (equals(from) || is_bogus()) {
      return true;
    } else {
      switch(_u._data) {
        case Boolean:
        case Byte:
        case Char:
        case Short:
          return false;
        default:
          return is_assignable_from(from, context, from_field_is_protected, THREAD);
      }
    }
  }

  VerificationType get_component(ClassVerifier* context) const;

  int dimensions() const {
    assert(is_array(), "Must be an array");
    int index = 0;
    while (name()->char_at(index) == JVM_SIGNATURE_ARRAY) index++;
    return index;
  }

  void print_on(outputStream* st) const;

 private:

  bool is_reference_assignable_from(
    const VerificationType&, ClassVerifier*, bool from_field_is_protected,
    TRAPS) const;

 public:
  static bool resolve_and_check_assignability(InstanceKlass* klass, Symbol* name,
                                              Symbol* from_name, bool from_field_is_protected,
                                              bool from_is_array, bool from_is_object,
                                              TRAPS);
};

#endif // SHARE_CLASSFILE_VERIFICATIONTYPE_HPP
