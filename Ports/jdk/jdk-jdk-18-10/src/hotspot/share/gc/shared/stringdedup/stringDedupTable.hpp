/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPTABLE_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPTABLE_HPP

#include "memory/allStatic.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "oops/typeArrayOop.hpp"
#include "oops/weakHandle.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class OopStorage;

// Provides deduplication.  This class keeps track of all the unique byte
// arrays used by deduplicated String objects.
//
// The arrays are in a hashtable, hashed using the bytes in the array.  The
// references to the arrays by the hashtable are weak, allowing arrays that
// become unreachable to be collected and their entries pruned from the
// table.  The hashtable is dynamically resized to accommodate the current
// number of hashtable entries.  There are several command line options
// controlling the growth or shrinkage of the hashtable.
//
// Operations on the table are not thread-safe.  Only the deduplication
// thread calls most of the operations on the table.  The only exception is
// the GC dead object count notification and the management of its state.
//
// The table supports resizing and removal of entries for byte arrays that
// have become unreferenced.  These operations are performed by the
// deduplication thread, in a series of small incremental steps.  This
// prevents these potentially long running operations from long blockage of
// safepoints or concurrent deduplication requests from the StringTable.
//
// As a space optimization, when shared StringTable entries exist the shared
// part of the StringTable is also used as a source for byte arrays.  This
// permits deduplication of strings against those shared entries without
// recording them in this table too.
class StringDedup::Table : AllStatic {
private:
  class Bucket;
  class CleanupState;
  class Resizer;
  class Cleaner;
  enum class DeadState;

  // Values in the table are weak references to jbyte[] Java objects.  The
  // String's coder isn't recorded, even though it affects how String access
  // would interpret that array.  For the purposes of deduplication we don't
  // care about that distinction; two Strings with equivalent arrays but
  // different coders can be deduplicated to share a single array.  We also
  // can't depend on the coder value being correct here, since GC requests
  // can provide the deduplication thread with access to a String that is
  // incompletely constructed; the value could be set before the coder.
  using TableValue = WeakHandle;

  // Weak storage for the string data in the table.
  static OopStorage* _table_storage;
  static Bucket* _buckets;
  static size_t _number_of_buckets;
  static size_t _number_of_entries;
  static size_t _grow_threshold;
  static CleanupState* _cleanup_state;
  static bool _need_bucket_shrinking;
  // These are always written while holding StringDedup_lock, but may be
  // read by the dedup thread without holding the lock lock.
  static volatile size_t _dead_count;
  static volatile DeadState _dead_state;

  static uint compute_hash(typeArrayOop obj);
  static size_t hash_to_index(uint hash_code);
  static void add(TableValue tv, uint hash_code);
  static TableValue find(typeArrayOop obj, uint hash_code);
  static void install(typeArrayOop obj, uint hash_code);
  static bool deduplicate_if_permitted(oop java_string, typeArrayOop value);
  static bool try_deduplicate_shared(oop java_string);
  static bool try_deduplicate_found_shared(oop java_string, oop found);
  static Bucket* make_buckets(size_t number_of_buckets, size_t reserve = 0);
  static void free_buckets(Bucket* buckets, size_t number_of_buckets);

  static bool start_resizer(bool grow_only, size_t number_of_entries);
  static bool start_cleaner(size_t number_of_entries, size_t dead_count);

  static void num_dead_callback(size_t num_dead);
  static bool is_dead_count_good_acquire();
  static void set_dead_state_cleaning();

public:
  static void initialize_storage();
  static void initialize();

  // Deduplicate java_string.  If the table already contains the string's
  // data array, replace the string's data array with the one in the table.
  // Otherwise, add the string's data array to the table.
  static void deduplicate(oop java_string);

  // Returns true if table needs to grow.
  static bool is_grow_needed();

  // Returns true if there are enough dead entries to need cleanup.
  static bool is_dead_entry_removal_needed();

  // If cleanup (resizing or removing dead entries) is needed or force
  // is true, setup cleanup state and return true.  If result is true,
  // the caller must eventually call cleanup_end.
  // precondition: no cleanup is in progress.
  static bool cleanup_start_if_needed(bool grow_only, bool force);

  // Perform some cleanup work.  Returns true if any progress was made,
  // false if there is no further work to do.
  // precondition: a cleanup is in progress.
  static bool cleanup_step();

  // Record the cleanup complete and cleanup state.
  // precondition: a cleanup is in progress.
  static void cleanup_end();

  // Return the phase kind for the cleanup being performed.
  // precondition: a cleanup is in progress.
  static Stat::Phase cleanup_phase();

  static void verify();
  static void log_statistics();
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPTABLE_HPP
