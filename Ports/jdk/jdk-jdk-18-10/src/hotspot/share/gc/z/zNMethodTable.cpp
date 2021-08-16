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
#include "code/relocInfo.hpp"
#include "code/nmethod.hpp"
#include "code/icBuffer.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/z/zGlobals.hpp"
#include "gc/z/zHash.inline.hpp"
#include "gc/z/zLock.inline.hpp"
#include "gc/z/zNMethodData.hpp"
#include "gc/z/zNMethodTable.hpp"
#include "gc/z/zNMethodTableEntry.hpp"
#include "gc/z/zNMethodTableIteration.hpp"
#include "gc/z/zSafeDelete.inline.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zWorkers.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/debug.hpp"
#include "utilities/powerOfTwo.hpp"

ZNMethodTableEntry* ZNMethodTable::_table = NULL;
size_t ZNMethodTable::_size = 0;
size_t ZNMethodTable::_nregistered = 0;
size_t ZNMethodTable::_nunregistered = 0;
ZNMethodTableIteration ZNMethodTable::_iteration;
ZSafeDeleteNoLock<ZNMethodTableEntry[]> ZNMethodTable::_safe_delete;

size_t ZNMethodTable::first_index(const nmethod* nm, size_t size) {
  assert(is_power_of_2(size), "Invalid size");
  const size_t mask = size - 1;
  const size_t hash = ZHash::address_to_uint32((uintptr_t)nm);
  return hash & mask;
}

size_t ZNMethodTable::next_index(size_t prev_index, size_t size) {
  assert(is_power_of_2(size), "Invalid size");
  const size_t mask = size - 1;
  return (prev_index + 1) & mask;
}

bool ZNMethodTable::register_entry(ZNMethodTableEntry* table, size_t size, nmethod* nm) {
  const ZNMethodTableEntry entry(nm);
  size_t index = first_index(nm, size);

  for (;;) {
    const ZNMethodTableEntry table_entry = table[index];

    if (!table_entry.registered() && !table_entry.unregistered()) {
      // Insert new entry
      table[index] = entry;
      return true;
    }

    if (table_entry.registered() && table_entry.method() == nm) {
      // Replace existing entry
      table[index] = entry;
      return false;
    }

    index = next_index(index, size);
  }
}

void ZNMethodTable::unregister_entry(ZNMethodTableEntry* table, size_t size, nmethod* nm) {
  size_t index = first_index(nm, size);

  for (;;) {
    const ZNMethodTableEntry table_entry = table[index];
    assert(table_entry.registered() || table_entry.unregistered(), "Entry not found");

    if (table_entry.registered() && table_entry.method() == nm) {
      // Remove entry
      table[index] = ZNMethodTableEntry(true /* unregistered */);
      return;
    }

    index = next_index(index, size);
  }
}

void ZNMethodTable::rebuild(size_t new_size) {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");

  assert(is_power_of_2(new_size), "Invalid size");

  log_debug(gc, nmethod)("Rebuilding NMethod Table: "
                         SIZE_FORMAT "->" SIZE_FORMAT " entries, "
                         SIZE_FORMAT "(%.0f%%->%.0f%%) registered, "
                         SIZE_FORMAT "(%.0f%%->%.0f%%) unregistered",
                         _size, new_size,
                         _nregistered, percent_of(_nregistered, _size), percent_of(_nregistered, new_size),
                         _nunregistered, percent_of(_nunregistered, _size), 0.0);

  // Allocate new table
  ZNMethodTableEntry* const new_table = new ZNMethodTableEntry[new_size];

  // Transfer all registered entries
  for (size_t i = 0; i < _size; i++) {
    const ZNMethodTableEntry entry = _table[i];
    if (entry.registered()) {
      register_entry(new_table, new_size, entry.method());
    }
  }

  // Free old table
  _safe_delete(_table);

  // Install new table
  _table = new_table;
  _size = new_size;
  _nunregistered = 0;
}

void ZNMethodTable::rebuild_if_needed() {
  // The hash table uses linear probing. To avoid wasting memory while
  // at the same time maintaining good hash collision behavior we want
  // to keep the table occupancy between 30% and 70%. The table always
  // grows/shrinks by doubling/halving its size. Pruning of unregistered
  // entries is done by rebuilding the table with or without resizing it.
  const size_t min_size = 1024;
  const size_t shrink_threshold = _size * 0.30;
  const size_t prune_threshold = _size * 0.65;
  const size_t grow_threshold = _size * 0.70;

  if (_size == 0) {
    // Initialize table
    rebuild(min_size);
  } else if (_nregistered < shrink_threshold && _size > min_size) {
    // Shrink table
    rebuild(_size / 2);
  } else if (_nregistered + _nunregistered > grow_threshold) {
    // Prune or grow table
    if (_nregistered < prune_threshold) {
      // Prune table
      rebuild(_size);
    } else {
      // Grow table
      rebuild(_size * 2);
    }
  }
}

size_t ZNMethodTable::registered_nmethods() {
  return _nregistered;
}

size_t ZNMethodTable::unregistered_nmethods() {
  return _nunregistered;
}

void ZNMethodTable::register_nmethod(nmethod* nm) {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");

  // Grow/Shrink/Prune table if needed
  rebuild_if_needed();

  // Insert new entry
  if (register_entry(_table, _size, nm)) {
    // New entry registered. When register_entry() instead returns
    // false the nmethod was already in the table so we do not want
    // to increase number of registered entries in that case.
    _nregistered++;
  }
}

void ZNMethodTable::wait_until_iteration_done() {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");

  while (_iteration.in_progress()) {
    CodeCache_lock->wait_without_safepoint_check();
  }
}

void ZNMethodTable::unregister_nmethod(nmethod* nm) {
  assert(CodeCache_lock->owned_by_self(), "Lock must be held");

  // Remove entry
  unregister_entry(_table, _size, nm);
  _nunregistered++;
  _nregistered--;
}

void ZNMethodTable::nmethods_do_begin() {
  MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);

  // Do not allow the table to be deleted while iterating
  _safe_delete.enable_deferred_delete();

  // Prepare iteration
  _iteration.nmethods_do_begin(_table, _size);
}

void ZNMethodTable::nmethods_do_end() {
  MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);

  // Finish iteration
  _iteration.nmethods_do_end();

  // Allow the table to be deleted
  _safe_delete.disable_deferred_delete();

  // Notify iteration done
  CodeCache_lock->notify_all();
}

void ZNMethodTable::nmethods_do(NMethodClosure* cl) {
  _iteration.nmethods_do(cl);
}
