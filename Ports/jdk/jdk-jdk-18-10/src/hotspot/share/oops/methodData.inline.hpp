/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_METHODDATA_INLINE_HPP
#define SHARE_OOPS_METHODDATA_INLINE_HPP

#include "oops/methodData.hpp"

#include "runtime/atomic.hpp"

inline void DataLayout::release_set_cell_at(int index, intptr_t value) {
  Atomic::release_store(&_cells[index], value);
}

inline void ProfileData::release_set_intptr_at(int index, intptr_t value) {
  assert(0 <= index && index < cell_count(), "oob");
  data()->release_set_cell_at(index, value);
}

inline void ProfileData::release_set_uint_at(int index, uint value) {
  release_set_intptr_at(index, (intptr_t) value);
}

inline void ProfileData::release_set_int_at(int index, int value) {
  release_set_intptr_at(index, (intptr_t) value);
}

inline void RetData::release_set_bci(uint row, int bci) {
  assert((uint)row < row_limit(), "oob");
  // 'release' when setting the bci acts as a valid flag for other
  // threads wrt bci_count and bci_displacement.
  release_set_int_at(bci0_offset + row * ret_row_cell_count, bci);
}

#endif // SHARE_OOPS_METHODDATA_INLINE_HPP
