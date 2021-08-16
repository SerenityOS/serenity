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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTAT_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTAT_HPP

#include "gc/shared/stringdedup/stringDedup.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ticks.hpp"

// Deduplication statistics.
//
// Operation counters are updated when deduplicating a string.
// Phase timing information is collected by the processing thread.
class StringDedup::Stat {
public:
  // Only phases that can be blocked, so excluding "idle".
  enum class Phase {
    process,
    resize_table,
    cleanup_table
  };

private:
  // Counters
  size_t _inspected;
  size_t _known;
  size_t _known_shared;
  size_t _new;
  size_t _new_bytes;
  size_t _deduped;
  size_t _deduped_bytes;
  size_t _replaced;
  size_t _deleted;
  size_t _skipped_dead;
  size_t _skipped_incomplete;
  size_t _skipped_shared;

  // Phase counters for deduplication thread
  size_t _concurrent;
  size_t _idle;
  size_t _process;
  size_t _resize_table;
  size_t _cleanup_table;
  size_t _block;

  // Time spent by the deduplication thread in different phases
  Ticks _concurrent_start;
  Tickspan _concurrent_elapsed;
  Ticks _phase_start;
  Tickspan _idle_elapsed;
  Tickspan _process_elapsed;
  Tickspan _resize_table_elapsed;
  Tickspan _cleanup_table_elapsed;
  Tickspan _block_elapsed;

  void report_phase_start(const char* phase);
  void report_phase_end(const char* phase, Tickspan* elapsed);
  Tickspan* elapsed_for_phase(Phase phase);

  void log_times(const char* prefix) const;

public:
  Stat();

  // Track number of strings looked up.
  void inc_inspected() {
    _inspected++;
  }

  // Track number of requests skipped because string died.
  void inc_skipped_dead() {
    _skipped_dead++;
  }

  // Track number of requests skipped because string was incomplete.
  void inc_skipped_incomplete() {
    _skipped_incomplete++;
  }

  // Track number of shared strings skipped because of a previously
  // installed equivalent entry.
  void inc_skipped_shared() {
    _skipped_shared++;
  }

  // Track number of inspected strings already present.
  void inc_known() {
    _known++;
  }

  // Track number of inspected strings found in the shared StringTable.
  void inc_known_shared() {
    _known_shared++;
  }

  // Track number of inspected strings added and accumulated size.
  void inc_new(size_t bytes) {
    _new++;
    _new_bytes += bytes;
  }

  // Track number of inspected strings dedup'ed and accumulated savings.
  void inc_deduped(size_t bytes) {
    _deduped++;
    _deduped_bytes += bytes;
  }

  // Track number of interned strings replacing existing strings.
  void inc_replaced() {
    _replaced++;
  }

  // Track number of strings removed from table.
  void inc_deleted() {
    _deleted++;
  }

  void report_idle_start();
  void report_idle_end();

  void report_process_start();
  void report_process_pause();
  void report_process_resume();
  void report_process_end();

  void report_resize_table_start(size_t new_bucket_count,
                                 size_t old_bucket_count,
                                 size_t entry_count);
  void report_resize_table_end();

  void report_cleanup_table_start(size_t entry_count, size_t dead_count);
  void report_cleanup_table_end();

  void report_concurrent_start();
  void report_concurrent_end();

  void block_phase(Phase phase);
  void unblock_phase();

  void add(const Stat* const stat);
  void log_statistics(bool total) const;

  static void log_summary(const Stat* last_stat, const Stat* total_stat);
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTAT_HPP
