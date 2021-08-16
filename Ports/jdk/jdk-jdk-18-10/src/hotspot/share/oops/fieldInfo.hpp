/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_FIELDINFO_HPP
#define SHARE_OOPS_FIELDINFO_HPP

#include "oops/constantPool.hpp"
#include "oops/symbol.hpp"
#include "oops/typeArrayOop.hpp"
#include "utilities/vmEnums.hpp"

// This class represents the field information contained in the fields
// array of an InstanceKlass.  Currently it's laid on top an array of
// Java shorts but in the future it could simply be used as a real
// array type.  FieldInfo generally shouldn't be used directly.
// Fields should be queried either through InstanceKlass or through
// the various FieldStreams.
class FieldInfo {
  friend class fieldDescriptor;
  friend class JavaFieldStream;
  friend class ClassFileParser;

 public:
  // fields
  // Field info extracted from the class file and stored
  // as an array of 6 shorts.

#define FIELDINFO_TAG_SIZE             2
#define FIELDINFO_TAG_OFFSET           1 << 0
#define FIELDINFO_TAG_CONTENDED        1 << 1

  // Packed field has the tag, and can be either of:
  //    hi bits <--------------------------- lo bits
  //   |---------high---------|---------low---------|
  //    ..........................................CO
  //    ..........................................00  - non-contended field
  //    [--contention_group--]....................10  - contended field with contention group
  //    [------------------offset----------------]01  - real field offset

  // Bit O indicates if the packed field contains an offset (O=1) or not (O=0)
  // Bit C indicates if the field is contended (C=1) or not (C=0)
  //       (if it is contended, the high packed field contains the contention group)

  enum FieldOffset {
    access_flags_offset      = 0,
    name_index_offset        = 1,
    signature_index_offset   = 2,
    initval_index_offset     = 3,
    low_packed_offset        = 4,
    high_packed_offset       = 5,
    field_slots              = 6
  };

 private:
  u2 _shorts[field_slots];

  void set_name_index(u2 val)                    { _shorts[name_index_offset] = val;         }
  void set_signature_index(u2 val)               { _shorts[signature_index_offset] = val;    }
  void set_initval_index(u2 val)                 { _shorts[initval_index_offset] = val;      }

  u2 name_index() const                          { return _shorts[name_index_offset];        }
  u2 signature_index() const                     { return _shorts[signature_index_offset];   }
  u2 initval_index() const                       { return _shorts[initval_index_offset];     }

 public:
  static FieldInfo* from_field_array(Array<u2>* fields, int index) {
    return ((FieldInfo*)fields->adr_at(index * field_slots));
  }
  static FieldInfo* from_field_array(u2* fields, int index) {
    return ((FieldInfo*)(fields + index * field_slots));
  }

  void initialize(u2 access_flags,
                  u2 name_index,
                  u2 signature_index,
                  u2 initval_index) {
    _shorts[access_flags_offset] = access_flags;
    _shorts[name_index_offset] = name_index;
    _shorts[signature_index_offset] = signature_index;
    _shorts[initval_index_offset] = initval_index;
    _shorts[low_packed_offset] = 0;
    _shorts[high_packed_offset] = 0;
  }

  u2 access_flags() const                        { return _shorts[access_flags_offset];            }
  u4 offset() const {
    assert((_shorts[low_packed_offset] & FIELDINFO_TAG_OFFSET) != 0, "Offset must have been set");
    return build_int_from_shorts(_shorts[low_packed_offset], _shorts[high_packed_offset]) >> FIELDINFO_TAG_SIZE;
  }

  bool is_contended() const {
    return (_shorts[low_packed_offset] & FIELDINFO_TAG_CONTENDED) != 0;
  }

  u2 contended_group() const {
    assert((_shorts[low_packed_offset] & FIELDINFO_TAG_OFFSET) == 0, "Offset must not have been set");
    assert((_shorts[low_packed_offset] & FIELDINFO_TAG_CONTENDED) != 0, "Field must be contended");
    return _shorts[high_packed_offset];
 }

  bool is_offset_set() const {
    return (_shorts[low_packed_offset] & FIELDINFO_TAG_OFFSET)!= 0;
  }

  Symbol* name(ConstantPool* cp) const {
    int index = name_index();
    if (is_internal()) {
      return lookup_symbol(index);
    }
    return cp->symbol_at(index);
  }

  Symbol* signature(ConstantPool* cp) const {
    int index = signature_index();
    if (is_internal()) {
      return lookup_symbol(index);
    }
    return cp->symbol_at(index);
  }

  void set_access_flags(u2 val)                  { _shorts[access_flags_offset] = val;             }
  void set_offset(u4 val)                        {
    val = val << FIELDINFO_TAG_SIZE; // make room for tag
    _shorts[low_packed_offset] = extract_low_short_from_int(val) | FIELDINFO_TAG_OFFSET;
    _shorts[high_packed_offset] = extract_high_short_from_int(val);
  }

  void set_contended_group(u2 val) {
    assert((_shorts[low_packed_offset] & FIELDINFO_TAG_OFFSET) == 0, "Offset must not have been set");
    assert((_shorts[low_packed_offset] & FIELDINFO_TAG_CONTENDED) == 0, "Overwritting contended group");
    _shorts[low_packed_offset] |= FIELDINFO_TAG_CONTENDED;
    _shorts[high_packed_offset] = val;
  }

  bool is_internal() const {
    return (access_flags() & JVM_ACC_FIELD_INTERNAL) != 0;
  }

  bool is_stable() const {
    return (access_flags() & JVM_ACC_FIELD_STABLE) != 0;
  }
  void set_stable(bool z) {
    if (z) _shorts[access_flags_offset] |=  JVM_ACC_FIELD_STABLE;
    else   _shorts[access_flags_offset] &= ~JVM_ACC_FIELD_STABLE;
  }

  Symbol* lookup_symbol(int symbol_index) const {
    assert(is_internal(), "only internal fields");
    return Symbol::vm_symbol_at(static_cast<vmSymbolID>(symbol_index));
  }
};

#endif // SHARE_OOPS_FIELDINFO_HPP
