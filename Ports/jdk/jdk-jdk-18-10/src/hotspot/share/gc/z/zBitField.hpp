/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZBITFIELD_HPP
#define SHARE_GC_Z_ZBITFIELD_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"

//
//  Example
//  -------
//
//  typedef ZBitField<uint64_t, uint8_t,  0,  2, 3> field_word_aligned_size;
//  typedef ZBitField<uint64_t, uint32_t, 2, 30>    field_length;
//
//
//   6                                 3 3
//   3                                 2 1                               2 10
//  +-----------------------------------+---------------------------------+--+
//  |11111111 11111111 11111111 11111111|11111111 11111111 11111111 111111|11|
//  +-----------------------------------+---------------------------------+--+
//  |                                   |                                 |
//  |       31-2 field_length (30-bits) *                                 |
//  |                                                                     |
//  |                                1-0 field_word_aligned_size (2-bits) *
//  |
//  * 63-32 Unused (32-bits)
//
//
//  field_word_aligned_size::encode(16) = 2
//  field_length::encode(2342) = 9368
//
//  field_word_aligned_size::decode(9368 | 2) = 16
//  field_length::decode(9368 | 2) = 2342
//

template <typename ContainerType, typename ValueType, int FieldShift, int FieldBits, int ValueShift = 0>
class ZBitField : public AllStatic {
private:
  static const int ContainerBits = sizeof(ContainerType) * BitsPerByte;

  static_assert(FieldBits < ContainerBits, "Field too large");
  static_assert(FieldShift + FieldBits <= ContainerBits, "Field too large");
  static_assert(ValueShift + FieldBits <= ContainerBits, "Field too large");

  static const ContainerType FieldMask = (((ContainerType)1 << FieldBits) - 1);

public:
  static ValueType decode(ContainerType container) {
    return (ValueType)(((container >> FieldShift) & FieldMask) << ValueShift);
  }

  static ContainerType encode(ValueType value) {
    assert(((ContainerType)value & (FieldMask << ValueShift)) == (ContainerType)value, "Invalid value");
    return ((ContainerType)value >> ValueShift) << FieldShift;
  }
};

#endif // SHARE_GC_Z_ZBITFIELD_HPP
