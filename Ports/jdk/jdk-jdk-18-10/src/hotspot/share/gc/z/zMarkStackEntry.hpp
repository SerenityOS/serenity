/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZMARKSTACKENTRY_HPP
#define SHARE_GC_Z_ZMARKSTACKENTRY_HPP

#include "gc/z/zBitField.hpp"
#include "memory/allocation.hpp"

//
// Mark stack entry layout
// -----------------------
//
//  Object entry
//  ------------
//
//   6
//   3                                                                5 4 3 2 1 0
//  +------------------------------------------------------------------+-+-+-+-+-+
//  |11111111 11111111 11111111 11111111 11111111 11111111 11111111 111|1|1|1|1|1|
//  +------------------------------------------------------------------+-+-+-+-+-+
//  |                                                                  | | | | |
//  |                                            4-4 Mark Flag (1-bit) * | | | |
//  |                                                                    | | | |
//  |                                    3-3 Increment Live Flag (1-bit) * | | |
//  |                                                                      | | |
//  |                                              2-2 Follow Flag (1-bit) * | |
//  |                                                                        | |
//  |                                         1-1 Partial Array Flag (1-bit) * |
//  |                                                                          |
//  |                                                   0-0 Final Flag (1-bit) *
//  |
//  * 63-5 Object Address (59-bits)
//
//
//  Partial array entry
//  -------------------
//
//   6                                 3  3
//   3                                 2  1                               2 1 0
//  +------------------------------------+---------------------------------+-+-+
//  |11111111 11111111 11111111 11111111 |11111111 11111111 11111111 111111|1|1|
//  +------------------------------------+---------------------------------+-+-+
//  |                                    |                                 | |
//  |                                    |  1-1 Partial Array Flag (1-bit) * |
//  |                                    |                                   |
//  |                                    |            0-0 Final Flag (1-bit) *
//  |                                    |
//  |                                    * 31-2 Partial Array Length (30-bits)
//  |
//  * 63-32 Partial Array Address Offset (32-bits)
//

class ZMarkStackEntry  {
private:
  typedef ZBitField<uint64_t, bool,      0,  1>  field_finalizable;
  typedef ZBitField<uint64_t, bool,      1,  1>  field_partial_array;
  typedef ZBitField<uint64_t, bool,      2,  1>  field_follow;
  typedef ZBitField<uint64_t, bool,      3,  1>  field_inc_live;
  typedef ZBitField<uint64_t, bool,      4,  1>  field_mark;
  typedef ZBitField<uint64_t, uintptr_t, 5,  59> field_object_address;
  typedef ZBitField<uint64_t, size_t,    2,  30> field_partial_array_length;
  typedef ZBitField<uint64_t, size_t,    32, 32> field_partial_array_offset;

  uint64_t _entry;

public:
  ZMarkStackEntry() {
    // This constructor is intentionally left empty and does not initialize
    // _entry to allow it to be optimized out when instantiating ZMarkStack,
    // which has a long array of ZMarkStackEntry elements, but doesn't care
    // what _entry is initialized to.
  }

  ZMarkStackEntry(uintptr_t object_address, bool mark, bool inc_live, bool follow, bool finalizable) :
      _entry(field_object_address::encode(object_address) |
             field_mark::encode(mark) |
             field_inc_live::encode(inc_live) |
             field_follow::encode(follow) |
             field_partial_array::encode(false) |
             field_finalizable::encode(finalizable)) {}

  ZMarkStackEntry(size_t partial_array_offset, size_t partial_array_length, bool finalizable) :
      _entry(field_partial_array_offset::encode(partial_array_offset) |
             field_partial_array_length::encode(partial_array_length) |
             field_partial_array::encode(true) |
             field_finalizable::encode(finalizable)) {}

  bool finalizable() const {
    return field_finalizable::decode(_entry);
  }

  bool partial_array() const {
    return field_partial_array::decode(_entry);
  }

  size_t partial_array_offset() const {
    return field_partial_array_offset::decode(_entry);
  }

  size_t partial_array_length() const {
    return field_partial_array_length::decode(_entry);
  }

  bool follow() const {
    return field_follow::decode(_entry);
  }

  bool inc_live() const {
    return field_inc_live::decode(_entry);
  }

  bool mark() const {
    return field_mark::decode(_entry);
  }

  uintptr_t object_address() const {
    return field_object_address::decode(_entry);
  }
};

#endif // SHARE_GC_Z_ZMARKSTACKENTRY_HPP
