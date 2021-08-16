/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/atomic.hpp"
#include "runtime/os.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"
#include "utilities/tableStatistics.hpp"
#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

TableRateStatistics::TableRateStatistics() :
  _added_items(0), _removed_items(0),
  _time_stamp(0), _seconds_stamp(0),
  _added_items_stamp(0), _added_items_stamp_prev(0),
  _removed_items_stamp(0), _removed_items_stamp_prev(0) {}

TableRateStatistics::~TableRateStatistics() { };

void TableRateStatistics::add() {
#if INCLUDE_JFR
  if (Jfr::is_recording()) {
    Atomic::inc(&_added_items);
  }
#endif
}

void TableRateStatistics::remove() {
#if INCLUDE_JFR
  if (Jfr::is_recording()) {
    Atomic::inc(&_removed_items);
  }
#endif
}

void TableRateStatistics::stamp() {
  jlong now = os::javaTimeNanos();

  _added_items_stamp_prev = _added_items_stamp;
  _removed_items_stamp_prev = _removed_items_stamp;

  _added_items_stamp = _added_items;
  _removed_items_stamp = _removed_items;

  if (_time_stamp == 0) {
    _time_stamp = now - 1000000000;
  }
  jlong diff = (now - _time_stamp);
  _seconds_stamp = (float)diff / 1000000000.0;
  _time_stamp = now;
}

float TableRateStatistics::get_add_rate() {
  return (float)((_added_items_stamp - _added_items_stamp_prev) / _seconds_stamp);
}

float TableRateStatistics::get_remove_rate() {
  return (float)((_removed_items_stamp - _removed_items_stamp_prev) / _seconds_stamp);
}

TableStatistics::TableStatistics() :
  _literal_bytes(0),
  _number_of_buckets(0), _number_of_entries(0),
  _maximum_bucket_size(0), _average_bucket_size(0),
  _variance_of_bucket_size(0), _stddev_of_bucket_size(0),
  _bucket_bytes(0), _entry_bytes(0), _total_footprint(0),
  _bucket_size(0), _entry_size(0),
  _add_rate(0), _remove_rate(0) {
}

TableStatistics::TableStatistics(TableRateStatistics& rate_stats, NumberSeq summary, size_t literal_bytes, size_t bucket_bytes, size_t node_bytes) :
  _literal_bytes(literal_bytes),
  _number_of_buckets(0), _number_of_entries(0),
  _maximum_bucket_size(0), _average_bucket_size(0),
  _variance_of_bucket_size(0), _stddev_of_bucket_size(0),
  _bucket_bytes(0), _entry_bytes(0), _total_footprint(0),
  _bucket_size(0), _entry_size(0),
  _add_rate(0), _remove_rate(0) {

  _number_of_buckets = summary.num();
  _number_of_entries = summary.sum();

  _maximum_bucket_size = summary.maximum();
  _average_bucket_size = summary.avg();
  _variance_of_bucket_size = summary.variance();
  _stddev_of_bucket_size = summary.sd();

  _bucket_bytes = _number_of_buckets * bucket_bytes;
  _entry_bytes = _number_of_entries * node_bytes;
  _total_footprint = _literal_bytes + _bucket_bytes + _entry_bytes;

  _bucket_size = (_number_of_buckets <= 0) ? 0 : (_bucket_bytes / _number_of_buckets);
  _entry_size = (_number_of_entries <= 0) ? 0 : (_entry_bytes / _number_of_entries);

#if INCLUDE_JFR
  if (Jfr::is_recording()) {
    rate_stats.stamp();
    _add_rate = rate_stats.get_add_rate();
    _remove_rate = rate_stats.get_remove_rate();
  }
#endif
}

TableStatistics::~TableStatistics() { }

void TableStatistics::print(outputStream* st, const char *table_name) {
  st->print_cr("%s statistics:", table_name);
  st->print_cr("Number of buckets       : %9" PRIuPTR " = %9" PRIuPTR
               " bytes, each " SIZE_FORMAT,
              _number_of_buckets, _bucket_bytes, _bucket_size);
  st->print_cr("Number of entries       : %9" PRIuPTR " = %9" PRIuPTR
               " bytes, each " SIZE_FORMAT,
               _number_of_entries, _entry_bytes, _entry_size);
  if (_literal_bytes != 0) {
    float literal_avg = (_number_of_entries <= 0) ? 0 : (_literal_bytes / _number_of_entries);
    st->print_cr("Number of literals      : %9" PRIuPTR " = %9" PRIuPTR
                 " bytes, avg %7.3f",
                 _number_of_entries, _literal_bytes, literal_avg);
  }
  st->print_cr("Total footprint         : %9s = %9" PRIuPTR " bytes", "", _total_footprint);
  st->print_cr("Average bucket size     : %9.3f", _average_bucket_size);
  st->print_cr("Variance of bucket size : %9.3f", _variance_of_bucket_size);
  st->print_cr("Std. dev. of bucket size: %9.3f", _stddev_of_bucket_size);
  st->print_cr("Maximum bucket size     : %9" PRIuPTR, _maximum_bucket_size);
}

