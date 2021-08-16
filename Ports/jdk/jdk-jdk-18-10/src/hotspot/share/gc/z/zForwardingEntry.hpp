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

#ifndef SHARE_GC_Z_ZFORWARDINGENTRY_HPP
#define SHARE_GC_Z_ZFORWARDINGENTRY_HPP

#include "gc/z/zBitField.hpp"
#include "memory/allocation.hpp"
#include "metaprogramming/primitiveConversions.hpp"
#include <type_traits>

//
// Forwarding entry layout
// -----------------------
//
//   6                  4 4
//   3                  6 5                                                1 0
//  +--------------------+--------------------------------------------------+-+
//  |11111111 11111111 11|111111 11111111 11111111 11111111 11111111 1111111|1|
//  +--------------------+--------------------------------------------------+-+
//  |                    |                                                  |
//  |                    |                      0-0 Populated Flag (1-bits) *
//  |                    |
//  |                    * 45-1 To Object Offset (45-bits)
//  |
//  * 63-46 From Object Index (18-bits)
//

class ZForwardingEntry {
  friend struct PrimitiveConversions::Translate<ZForwardingEntry>;
  friend class VMStructs;

private:
  typedef ZBitField<uint64_t, bool,   0,   1> field_populated;
  typedef ZBitField<uint64_t, size_t, 1,  45> field_to_offset;
  typedef ZBitField<uint64_t, size_t, 46, 18> field_from_index;

  uint64_t _entry;

public:
  ZForwardingEntry() :
      _entry(0) {}

  ZForwardingEntry(size_t from_index, size_t to_offset) :
      _entry(field_populated::encode(true) |
             field_to_offset::encode(to_offset) |
             field_from_index::encode(from_index)) {}

  bool populated() const {
    return field_populated::decode(_entry);
  }

  size_t to_offset() const {
    return field_to_offset::decode(_entry);
  }

  size_t from_index() const {
    return field_from_index::decode(_entry);
  }
};

// Needed to allow atomic operations on ZForwardingEntry
template <>
struct PrimitiveConversions::Translate<ZForwardingEntry> : public std::true_type {
  typedef ZForwardingEntry Value;
  typedef uint64_t         Decayed;

  static Decayed decay(Value v) {
    return v._entry;
  }

  static Value recover(Decayed d) {
    ZForwardingEntry entry;
    entry._entry = d;
    return entry;
  }
};

#endif // SHARE_GC_Z_ZFORWARDINGENTRY_HPP
