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

#ifndef SHARE_UTILITIES_HASHTABLE_INLINE_HPP
#define SHARE_UTILITIES_HASHTABLE_INLINE_HPP

#include "utilities/hashtable.hpp"

#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "services/memTracker.hpp"

// Inline function definitions for hashtable.hpp.

// --------------------------------------------------------------------------

// Initialize a table.

template <MEMFLAGS F> inline BasicHashtable<F>::BasicHashtable(int table_size, int entry_size) {
  // Called on startup, no locking needed
  initialize(table_size, entry_size, 0);
  _buckets = NEW_C_HEAP_ARRAY2(HashtableBucket<F>, table_size, F, CURRENT_PC);
  for (int index = 0; index < _table_size; index++) {
    _buckets[index].clear();
  }
  _stats_rate = TableRateStatistics();
}


template <MEMFLAGS F> inline BasicHashtable<F>::BasicHashtable(int table_size, int entry_size,
                                      HashtableBucket<F>* buckets,
                                      int number_of_entries) {

  // Called on startup, no locking needed
  initialize(table_size, entry_size, number_of_entries);
  _buckets = buckets;
  _stats_rate = TableRateStatistics();
}

template <MEMFLAGS F> inline BasicHashtable<F>::~BasicHashtable() {
  free_buckets();
}

template <MEMFLAGS F> inline void BasicHashtable<F>::initialize(int table_size, int entry_size,
                                       int number_of_entries) {
  // Called on startup, no locking needed
  _table_size = table_size;
  _entry_size = entry_size;
  _number_of_entries = number_of_entries;
}


// The following method is MT-safe and may be used with caution.
template <MEMFLAGS F> inline BasicHashtableEntry<F>* BasicHashtable<F>::bucket(int i) const {
  return _buckets[i].get_entry();
}


template <MEMFLAGS F> inline void HashtableBucket<F>::set_entry(BasicHashtableEntry<F>* l) {
  // Warning: Preserve store ordering.  The PackageEntryTable, ModuleEntryTable and
  //          SystemDictionary are read without locks.  The new entry must be
  //          complete before other threads can be allowed to see it
  //          via a store to _buckets[index].
  Atomic::release_store(&_entry, l);
}


template <MEMFLAGS F> inline BasicHashtableEntry<F>* HashtableBucket<F>::get_entry() const {
  // Warning: Preserve load ordering.  The PackageEntryTable, ModuleEntryTable and
  //          SystemDictionary are read without locks.  The new entry must be
  //          complete before other threads can be allowed to see it
  //          via a store to _buckets[index].
  return Atomic::load_acquire(&_entry);
}


template <MEMFLAGS F> inline void BasicHashtable<F>::set_entry(int index, BasicHashtableEntry<F>* entry) {
  _buckets[index].set_entry(entry);
  if (entry != NULL) {
    JFR_ONLY(_stats_rate.add();)
  } else {
    JFR_ONLY(_stats_rate.remove();)
  }
}


template <MEMFLAGS F> inline void BasicHashtable<F>::add_entry(int index, BasicHashtableEntry<F>* entry) {
  entry->set_next(bucket(index));
  _buckets[index].set_entry(entry);
  ++_number_of_entries;
  JFR_ONLY(_stats_rate.add();)
}

#endif // SHARE_UTILITIES_HASHTABLE_INLINE_HPP
