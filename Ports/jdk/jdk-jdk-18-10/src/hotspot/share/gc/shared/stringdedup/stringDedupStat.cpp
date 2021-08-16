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

#include "precompiled.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "logging/log.hpp"
#include "utilities/globalDefinitions.hpp"

StringDedup::Stat::Stat() :
  _inspected(0),
  _known(0),
  _known_shared(0),
  _new(0),
  _new_bytes(0),
  _deduped(0),
  _deduped_bytes(0),
  _replaced(0),
  _deleted(0),
  _skipped_dead(0),
  _skipped_incomplete(0),
  _skipped_shared(0),
  _concurrent(0),
  _idle(0),
  _process(0),
  _resize_table(0),
  _cleanup_table(0),
  _block(0),
  _concurrent_start(),
  _concurrent_elapsed(),
  _phase_start(),
  _idle_elapsed(),
  _process_elapsed(),
  _resize_table_elapsed(),
  _cleanup_table_elapsed(),
  _block_elapsed() {
}

void StringDedup::Stat::add(const Stat* const stat) {
  _inspected           += stat->_inspected;
  _known               += stat->_known;
  _known_shared        += stat->_known_shared;
  _new                 += stat->_new;
  _new_bytes           += stat->_new_bytes;
  _deduped             += stat->_deduped;
  _deduped_bytes       += stat->_deduped_bytes;
  _replaced            += stat->_replaced;
  _deleted             += stat->_deleted;
  _skipped_dead        += stat->_skipped_dead;
  _skipped_incomplete  += stat->_skipped_incomplete;
  _skipped_shared      += stat->_skipped_shared;
  _concurrent          += stat->_concurrent;
  _idle                += stat->_idle;
  _process             += stat->_process;
  _resize_table        += stat->_resize_table;
  _cleanup_table       += stat->_cleanup_table;
  _block               += stat->_block;
  _concurrent_elapsed  += stat->_concurrent_elapsed;
  _idle_elapsed        += stat->_idle_elapsed;
  _process_elapsed     += stat->_process_elapsed;
  _resize_table_elapsed += stat->_resize_table_elapsed;
  _cleanup_table_elapsed += stat->_cleanup_table_elapsed;
  _block_elapsed       += stat->_block_elapsed;
}

// Support for log output formating
#define STRDEDUP_PERCENT_FORMAT         "%5.1f%%"
#define STRDEDUP_PERCENT_FORMAT_NS      "%.1f%%"
#define STRDEDUP_BYTES_FORMAT           "%8.1f%s"
#define STRDEDUP_BYTES_FORMAT_NS        "%.1f%s"
#define STRDEDUP_BYTES_PARAM(bytes)     byte_size_in_proper_unit((double)(bytes)), proper_unit_for_byte_size((bytes))

#define STRDEDUP_ELAPSED_FORMAT_MS         "%.3fms"
static double strdedup_elapsed_param_ms(Tickspan t) {
  return t.seconds() * MILLIUNITS;
}

void StringDedup::Stat::log_summary(const Stat* last_stat, const Stat* total_stat) {
  double total_deduped_bytes_percent = 0.0;

  if (total_stat->_new_bytes > 0) {
    // Avoid division by zero
    total_deduped_bytes_percent = percent_of(total_stat->_deduped_bytes, total_stat->_new_bytes);
  }

  log_info(stringdedup)(
    "Concurrent String Deduplication "
    "%zu/" STRDEDUP_BYTES_FORMAT_NS " (new), "
    "%zu/" STRDEDUP_BYTES_FORMAT_NS " (deduped), "
    "avg " STRDEDUP_PERCENT_FORMAT_NS ", "
    STRDEDUP_ELAPSED_FORMAT_MS " of " STRDEDUP_ELAPSED_FORMAT_MS,
    last_stat->_new, STRDEDUP_BYTES_PARAM(last_stat->_new_bytes),
    last_stat->_deduped, STRDEDUP_BYTES_PARAM(last_stat->_deduped_bytes),
    total_deduped_bytes_percent,
    strdedup_elapsed_param_ms(last_stat->_process_elapsed),
    strdedup_elapsed_param_ms(last_stat->_concurrent_elapsed));
}

void StringDedup::Stat::report_concurrent_start() {
  log_debug(stringdedup, phases, start)("Concurrent start");
  _concurrent_start = Ticks::now();
  _concurrent++;
}

void StringDedup::Stat::report_concurrent_end() {
  _concurrent_elapsed += (Ticks::now() - _concurrent_start);
  log_debug(stringdedup, phases)("Concurrent end: " STRDEDUP_ELAPSED_FORMAT_MS,
                                 strdedup_elapsed_param_ms(_concurrent_elapsed));
}

void StringDedup::Stat::report_phase_start(const char* phase) {
  log_debug(stringdedup, phases, start)("%s start", phase);
  _phase_start = Ticks::now();
}

void StringDedup::Stat::report_phase_end(const char* phase, Tickspan* elapsed) {
  *elapsed += Ticks::now() - _phase_start;
  log_debug(stringdedup, phases)("%s end: " STRDEDUP_ELAPSED_FORMAT_MS,
                                 phase, strdedup_elapsed_param_ms(*elapsed));
}

void StringDedup::Stat::report_idle_start() {
  report_phase_start("Idle");
  _idle++;
}

void StringDedup::Stat::report_idle_end() {
  report_phase_end("Idle", &_idle_elapsed);
}

void StringDedup::Stat::report_process_start() {
  report_phase_start("Process");
  _process++;
}

void StringDedup::Stat::report_process_pause() {
  _process_elapsed += (Ticks::now() - _phase_start);
  log_debug(stringdedup, phases)("Process paused");
}

void StringDedup::Stat::report_process_resume() {
  log_debug(stringdedup, phases)("Process resume");
  _phase_start = Ticks::now();
}

void StringDedup::Stat::report_process_end() {
  report_phase_end("Process", &_process_elapsed);
}

void StringDedup::Stat::report_resize_table_start(size_t new_bucket_count,
                                                  size_t old_bucket_count,
                                                  size_t entry_count) {
  _phase_start = Ticks::now();
  ++_resize_table;
  log_debug(stringdedup, phases, start)
           ("Resize Table: %zu -> %zu (%zu)",
            old_bucket_count, new_bucket_count, entry_count);
}

void StringDedup::Stat::report_resize_table_end() {
  report_phase_end("Resize Table", &_resize_table_elapsed);
}

void StringDedup::Stat::report_cleanup_table_start(size_t entry_count,
                                                   size_t dead_count) {
  log_debug(stringdedup, phases, start)
           ("Cleanup Table: %zu / %zu -> %zu",
            dead_count, entry_count, (entry_count - dead_count));
  _phase_start = Ticks::now();
  _cleanup_table++;
}

void StringDedup::Stat::report_cleanup_table_end() {
  report_phase_end("Cleanup Table", &_cleanup_table_elapsed);
}

Tickspan* StringDedup::Stat::elapsed_for_phase(Phase phase) {
  switch (phase) {
  case Phase::process: return &_process_elapsed;
  case Phase::resize_table: return &_resize_table_elapsed;
  case Phase::cleanup_table: return &_cleanup_table_elapsed;
  }
  ShouldNotReachHere();
  return nullptr;
}

void StringDedup::Stat::block_phase(Phase phase) {
  Ticks now = Ticks::now();
  *elapsed_for_phase(phase) += now - _phase_start;
  _phase_start = now;
  _block++;
}

void StringDedup::Stat::unblock_phase() {
  Ticks now = Ticks::now();
  _block_elapsed += now - _phase_start;
  _phase_start = now;
}

void StringDedup::Stat::log_times(const char* prefix) const {
  log_debug(stringdedup)(
    "  %s Process: %zu/" STRDEDUP_ELAPSED_FORMAT_MS
    ", Idle: %zu/" STRDEDUP_ELAPSED_FORMAT_MS
    ", Blocked: %zu/" STRDEDUP_ELAPSED_FORMAT_MS,
    prefix,
    _process, strdedup_elapsed_param_ms(_process_elapsed),
    _idle, strdedup_elapsed_param_ms(_idle_elapsed),
    _block, strdedup_elapsed_param_ms(_block_elapsed));
  if (_resize_table > 0) {
    log_debug(stringdedup)(
      "  %s Resize Table: %zu/" STRDEDUP_ELAPSED_FORMAT_MS,
      prefix, _resize_table, strdedup_elapsed_param_ms(_resize_table_elapsed));
  }
  if (_cleanup_table > 0) {
    log_debug(stringdedup)(
      "  %s Cleanup Table: %zu/" STRDEDUP_ELAPSED_FORMAT_MS,
      prefix, _cleanup_table, strdedup_elapsed_param_ms(_cleanup_table_elapsed));
  }
}

void StringDedup::Stat::log_statistics(bool total) const {
  double known_percent               = percent_of(_known, _inspected);
  double known_shared_percent        = percent_of(_known_shared, _inspected);
  double new_percent                 = percent_of(_new, _inspected);
  double deduped_percent             = percent_of(_deduped, _inspected);
  double deduped_bytes_percent       = percent_of(_deduped_bytes, _new_bytes);
  double replaced_percent            = percent_of(_replaced, _new);
  double deleted_percent             = percent_of(_deleted, _new);
  log_times(total ? "Total" : "Last");
  log_debug(stringdedup)("    Inspected:    %12zu", _inspected);
  log_debug(stringdedup)("      Known:      %12zu(%5.1f%%)", _known, known_percent);
  log_debug(stringdedup)("      Shared:     %12zu(%5.1f%%)", _known_shared, known_shared_percent);
  log_debug(stringdedup)("      New:        %12zu(%5.1f%%)" STRDEDUP_BYTES_FORMAT,
                         _new, new_percent, STRDEDUP_BYTES_PARAM(_new_bytes));
  log_debug(stringdedup)("      Replaced:   %12zu(%5.1f%%)", _replaced, replaced_percent);
  log_debug(stringdedup)("      Deleted:    %12zu(%5.1f%%)", _deleted, deleted_percent);
  log_debug(stringdedup)("    Deduplicated: %12zu(%5.1f%%)" STRDEDUP_BYTES_FORMAT "(%5.1f%%)",
                         _deduped, deduped_percent, STRDEDUP_BYTES_PARAM(_deduped_bytes), deduped_bytes_percent);
  log_debug(stringdedup)("    Skipped: %zu (dead), %zu (incomplete), %zu (shared)",
                         _skipped_dead, _skipped_incomplete, _skipped_shared);
}
