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

#ifndef SHARE_UTILITIES_TABLE_STATISTICS_HPP
#define SHARE_UTILITIES_TABLE_STATISTICS_HPP

#include "memory/allocation.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/numberSeq.hpp"

class TableRateStatistics : CHeapObj<mtStatistics> {

  friend class TableStatistics;

private:
  volatile size_t _added_items;
  volatile size_t _removed_items;

  jlong _time_stamp;
  double _seconds_stamp;
  size_t _added_items_stamp;
  size_t _added_items_stamp_prev;
  size_t _removed_items_stamp;
  size_t _removed_items_stamp_prev;

public:
  TableRateStatistics();
  ~TableRateStatistics();

  void add();
  void remove();

protected:
  void stamp();
  float get_add_rate();
  float get_remove_rate();
};

class TableStatistics : CHeapObj<mtStatistics> {

public:
  size_t _literal_bytes;

  size_t _number_of_buckets;
  size_t _number_of_entries;

  size_t _maximum_bucket_size;
  float _average_bucket_size;
  float _variance_of_bucket_size;
  float _stddev_of_bucket_size;

  size_t _bucket_bytes;
  size_t _entry_bytes;
  size_t _total_footprint;

  size_t _bucket_size;
  size_t _entry_size;

  float _add_rate;
  float _remove_rate;

  TableStatistics();
  TableStatistics(TableRateStatistics& rate_stats, NumberSeq summary, size_t literal_bytes, size_t bucket_bytes, size_t node_bytes);
  ~TableStatistics();

  void print(outputStream* st, const char *table_name);
};

#endif // SHARE_UTILITIES_TABLE_STATISTICS_HPP
