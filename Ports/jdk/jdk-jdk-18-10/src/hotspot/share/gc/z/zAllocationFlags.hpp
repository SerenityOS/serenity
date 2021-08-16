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

#ifndef SHARE_GC_Z_ZALLOCATIONFLAGS_HPP
#define SHARE_GC_Z_ZALLOCATIONFLAGS_HPP

#include "gc/z/zBitField.hpp"
#include "memory/allocation.hpp"

//
// Allocation flags layout
// -----------------------
//
//   7     2 1 0
//  +-----+-+-+-+
//  |00000|1|1|1|
//  +-----+-+-+-+
//  |     | | |
//  |     | | * 0-0 Non-Blocking Flag (1-bit)
//  |     | |
//  |     | * 1-1 Worker Relocation Flag (1-bit)
//  |     |
//  |     * 2-2 Low Address Flag (1-bit)
//  |
//  * 7-3 Unused (5-bits)
//

class ZAllocationFlags {
private:
  typedef ZBitField<uint8_t, bool, 0, 1> field_non_blocking;
  typedef ZBitField<uint8_t, bool, 1, 1> field_worker_relocation;
  typedef ZBitField<uint8_t, bool, 2, 1> field_low_address;

  uint8_t _flags;

public:
  ZAllocationFlags() :
      _flags(0) {}

  void set_non_blocking() {
    _flags |= field_non_blocking::encode(true);
  }

  void set_worker_relocation() {
    _flags |= field_worker_relocation::encode(true);
  }

  void set_low_address() {
    _flags |= field_low_address::encode(true);
  }

  bool non_blocking() const {
    return field_non_blocking::decode(_flags);
  }

  bool worker_relocation() const {
    return field_worker_relocation::decode(_flags);
  }

  bool low_address() const {
    return field_low_address::decode(_flags);
  }
};

#endif // SHARE_GC_Z_ZALLOCATIONFLAGS_HPP
