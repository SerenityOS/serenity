/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/z/zNMethodTableEntry.hpp"
#include "gc/z/zNMethodTableIteration.hpp"
#include "memory/iterator.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

ZNMethodTableIteration::ZNMethodTableIteration() :
    _table(NULL),
    _size(0),
    _claimed(0) {}

bool ZNMethodTableIteration::in_progress() const {
  return _table != NULL;
}

void ZNMethodTableIteration::nmethods_do_begin(ZNMethodTableEntry* table, size_t size) {
  assert(!in_progress(), "precondition");

  _table = table;
  _size = size;
  _claimed = 0;
}

void ZNMethodTableIteration::nmethods_do_end() {
  assert(_claimed >= _size, "Failed to claim all table entries");

  // Finish iteration
  _table = NULL;
}

void ZNMethodTableIteration::nmethods_do(NMethodClosure* cl) {
  for (;;) {
    // Claim table partition. Each partition is currently sized to span
    // two cache lines. This number is just a guess, but seems to work well.
    const size_t partition_size = (ZCacheLineSize * 2) / sizeof(ZNMethodTableEntry);
    const size_t partition_start = MIN2(Atomic::fetch_and_add(&_claimed, partition_size), _size);
    const size_t partition_end = MIN2(partition_start + partition_size, _size);
    if (partition_start == partition_end) {
      // End of table
      break;
    }

    // Process table partition
    for (size_t i = partition_start; i < partition_end; i++) {
      const ZNMethodTableEntry entry = _table[i];
      if (entry.registered()) {
        cl->do_nmethod(entry.method());
      }
    }
  }
}
