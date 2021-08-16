/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPCONFIG_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPCONFIG_HPP

#include "gc/shared/stringdedup/stringDedup.hpp"
#include "memory/allStatic.hpp"
#include "utilities/globalDefinitions.hpp"

// Provides access to canonicalized configuration parameter values.  This
// class captures the various StringDeduplicationXXX command line option
// values, massages them, and provides error checking support.
class StringDedup::Config : AllStatic {
  static size_t _initial_table_size;
  static int _age_threshold;
  static double _load_factor_for_growth;
  static double _load_factor_for_shrink;
  static double _load_factor_target;
  static size_t _minimum_dead_for_cleanup;
  static double _dead_factor_for_cleanup;
  static uint64_t _hash_seed;

  static const size_t good_sizes[];
  static const size_t min_good_size;
  static const size_t max_good_size;
  static size_t good_size(size_t n);

public:
  // Perform ergonomic adjustments and error checking.
  // Returns true on success, false if some error check failed.
  static bool ergo_initialize();

  static void initialize();

  static size_t initial_table_size();
  static int age_threshold();
  static uint64_t hash_seed();

  static size_t grow_threshold(size_t table_size);
  static size_t shrink_threshold(size_t table_size);
  static bool should_grow_table(size_t table_size, size_t entry_count);
  static bool should_shrink_table(size_t table_size, size_t entry_count);
  static size_t desired_table_size(size_t entry_count);
  static bool should_cleanup_table(size_t entry_count, size_t dead_count);
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPCONFIG_HPP

