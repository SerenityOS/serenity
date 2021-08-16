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

#include "precompiled.hpp"
#include "classfile/altHashing.hpp"
#include "gc/shared/stringdedup/stringDedupConfig.hpp"
#include "logging/log.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/globals.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

size_t StringDedup::Config::_initial_table_size;
int StringDedup::Config::_age_threshold;
double StringDedup::Config::_load_factor_for_growth;
double StringDedup::Config::_load_factor_for_shrink;
double StringDedup::Config::_load_factor_target;
size_t StringDedup::Config::_minimum_dead_for_cleanup;
double StringDedup::Config::_dead_factor_for_cleanup;
uint64_t StringDedup::Config::_hash_seed;

size_t StringDedup::Config::initial_table_size() {
  return _initial_table_size;
}

int StringDedup::Config::age_threshold() {
  return _age_threshold;
}

bool StringDedup::Config::should_cleanup_table(size_t entry_count, size_t dead_count) {
  return (dead_count > _minimum_dead_for_cleanup) &&
         (dead_count > (entry_count * _dead_factor_for_cleanup));
}

uint64_t StringDedup::Config::hash_seed() {
  return _hash_seed;
}

static uint64_t initial_hash_seed() {
  if (StringDeduplicationHashSeed != 0) {
    return StringDeduplicationHashSeed;
  } else {
    return AltHashing::compute_seed();
  }
}

// Primes after 500 * 2^N and 500 * (2^N + 2^(N-1)) for integer N.
const size_t StringDedup::Config::good_sizes[] = {
  503, 751, 1009, 1511, 2003, 3001, 4001, 6007, 8009, 12007, 16001, 24001,
  32003, 48017, 64007, 96001, 128021, 192007, 256019, 384001, 512009, 768013,
  1024021, 1536011, 2048003, 3072001, 4096013, 6144001, 8192003, 12288011,
  16384001, 24576001, 32768011, 49152001, 65536043, 98304053,
  131072003, 196608007, 262144009, 393216007, 524288057, 786432001,
  1048576019, 1572864001 };

const size_t StringDedup::Config::min_good_size = good_sizes[0];
const size_t StringDedup::Config::max_good_size = good_sizes[ARRAY_SIZE(good_sizes) - 1];

size_t StringDedup::Config::good_size(size_t n) {
  size_t result = good_sizes[ARRAY_SIZE(good_sizes) - 1];
  for (size_t i = 0; i < ARRAY_SIZE(good_sizes); ++i) {
    if (n <= good_sizes[i]) {
      result = good_sizes[i];
      break;
    }
  }
  return result;
}

size_t StringDedup::Config::grow_threshold(size_t table_size) {
  return (table_size < max_good_size) ?
         static_cast<size_t>(table_size * _load_factor_for_growth) :
         SIZE_MAX;
}

size_t StringDedup::Config::shrink_threshold(size_t table_size) {
    return (table_size > min_good_size) ?
           static_cast<size_t>(table_size * _load_factor_for_shrink) :
           0;
}

bool StringDedup::Config::should_grow_table(size_t table_size, size_t entry_count) {
  return entry_count > grow_threshold(table_size);
}

bool StringDedup::Config::should_shrink_table(size_t table_size, size_t entry_count) {
  return entry_count < shrink_threshold(table_size);
}

size_t StringDedup::Config::desired_table_size(size_t entry_count) {
  return good_size(static_cast<size_t>(entry_count / _load_factor_target));
}

bool StringDedup::Config::ergo_initialize() {
  if (!UseStringDeduplication) {
    return true;
  } else if (!UseG1GC && !UseShenandoahGC) {
    // String deduplication requested but not supported by the selected GC.
    // Warn and force disable, but don't error except in debug build with
    // incorrect default.
    assert(!FLAG_IS_DEFAULT(UseStringDeduplication),
           "Enabled by default for GC that doesn't support it");
    log_warning(stringdedup)("String Deduplication disabled: "
                             "not supported by selected GC");
    FLAG_SET_ERGO(UseStringDeduplication, false);
    return true;
  }

  // UseStringDeduplication is enabled.  Check parameters.  These checks are
  // in addition to any range or constraint checks directly associated with
  // the parameters.
  bool result = true;

  // ShrinkTableLoad <= TargetTableLoad <= GrowTableLoad.
  if (StringDeduplicationShrinkTableLoad > StringDeduplicationTargetTableLoad) {
    JVMFlag::printError(true,
                        "StringDeduplicationShrinkTableLoad (%f) must not exceed "
                        "StringDeduplicationTargetTableLoad (%f)",
                        StringDeduplicationShrinkTableLoad,
                        StringDeduplicationTargetTableLoad);
    result = false;
  }
  if (StringDeduplicationTargetTableLoad > StringDeduplicationGrowTableLoad) {
    JVMFlag::printError(true,
                        "StringDeduplicationTargetTableLoad (%f) must not exceed "
                        "StringDeduplicationGrowTableLoad (%f)",
                        StringDeduplicationTargetTableLoad,
                        StringDeduplicationGrowTableLoad);
    result = false;
  }

  return result;
}

void StringDedup::Config::initialize() {
  _initial_table_size = good_size(StringDeduplicationInitialTableSize);
  _age_threshold = StringDeduplicationAgeThreshold;
  _load_factor_for_growth = StringDeduplicationGrowTableLoad;
  _load_factor_for_shrink = StringDeduplicationShrinkTableLoad;
  _load_factor_target = StringDeduplicationTargetTableLoad;
  _minimum_dead_for_cleanup = StringDeduplicationCleanupDeadMinimum;
  _dead_factor_for_cleanup = percent_of(StringDeduplicationCleanupDeadPercent, 100);
  _hash_seed = initial_hash_seed();
}
