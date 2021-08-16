/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef GTEST_METASPACE_METASPACE_GTESTCONTEXTS_HPP
#define GTEST_METASPACE_METASPACE_GTESTCONTEXTS_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/testHelpers.hpp"
#include "metaspaceGtestCommon.hpp"

using metaspace::Metachunk;
using metaspace::chunklevel_t;
using namespace metaspace::chunklevel;

class MetaspaceGtestContext : public metaspace::MetaspaceTestContext {
public:
  MetaspaceGtestContext(size_t commit_limit = 0, size_t reserve_limit = 0) :
    metaspace::MetaspaceTestContext("gtest-metaspace-context", commit_limit, reserve_limit)
  {}
};

class ChunkGtestContext : public MetaspaceGtestContext {

  int _num_chunks_allocated;

  void checked_alloc_chunk_0(Metachunk** p_return_value, chunklevel_t preferred_level,
                             chunklevel_t max_level, size_t min_committed_size);

  // Test pattern established when allocating from the chunk with allocate_from_chunk_with_tests().
  void test_pattern(Metachunk* c, size_t word_size);
  void test_pattern(Metachunk* c) { test_pattern(c, c->used_words()); }

public:

  ChunkGtestContext(size_t commit_limit = 0, size_t reserve_limit = 0) :
    MetaspaceGtestContext(commit_limit, reserve_limit),
    _num_chunks_allocated(0)
  {}

  /////

  // Note: all test functions return void and return values are by pointer ref; this is awkward but otherwise we cannot
  // use gtest ASSERT macros inside those functions.

  // Allocate a chunk (you do not know if it will succeed).
  void alloc_chunk(Metachunk** p_return_value, chunklevel_t preferred_level, chunklevel_t max_level, size_t min_committed_size) {
    checked_alloc_chunk_0(p_return_value, preferred_level, max_level, min_committed_size);
  }

  // Allocate a chunk; do not expect success, but if it succeeds, test the chunk.
  void alloc_chunk(Metachunk** p_return_value, chunklevel_t level) {
    alloc_chunk(p_return_value, level, level, word_size_for_level(level));
  }

  // Allocate a chunk; it must succeed. Test the chunk.
  void alloc_chunk_expect_success(Metachunk** p_return_value, chunklevel_t preferred_level, chunklevel_t max_level, size_t min_committed_size) {
    checked_alloc_chunk_0(p_return_value, preferred_level, max_level, min_committed_size);
    ASSERT_NOT_NULL(*p_return_value);
  }

  // Allocate a chunk; it must succeed. Test the chunk.
  void alloc_chunk_expect_success(Metachunk** p_return_value, chunklevel_t level) {
    alloc_chunk_expect_success(p_return_value, level, level, word_size_for_level(level));
  }

  // Allocate a chunk but expect it to fail.
  void alloc_chunk_expect_failure(chunklevel_t preferred_level, chunklevel_t max_level, size_t min_committed_size) {
    Metachunk* c = NULL;
    checked_alloc_chunk_0(&c, preferred_level, max_level, min_committed_size);
    ASSERT_NULL(c);
  }

  // Allocate a chunk but expect it to fail.
  void alloc_chunk_expect_failure(chunklevel_t level) {
    return alloc_chunk_expect_failure(level, level, word_size_for_level(level));
  }

  /////

  void return_chunk(Metachunk* c);

  /////

  // Allocates from a chunk; also, fills allocated area with test pattern which will be tested with test_pattern().
  void allocate_from_chunk(MetaWord** p_return_value, Metachunk* c, size_t word_size);

  // Convenience function: allocate from chunk for when you don't care for the result pointer
  void allocate_from_chunk(Metachunk* c, size_t word_size) {
    MetaWord* dummy;
    allocate_from_chunk(&dummy, c, word_size);
  }

  void commit_chunk_with_test(Metachunk* c, size_t additional_size);
  void commit_chunk_expect_failure(Metachunk* c, size_t additional_size);

  void uncommit_chunk_with_test(Metachunk* c);

};

#endif // GTEST_METASPACE_METASPACE_GTESTCONTEXTS_HPP

