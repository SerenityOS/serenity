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

#include "precompiled.hpp"
#include "memory/metaspace/chunkHeaderPool.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/metachunk.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

using metaspace::ChunkHeaderPool;
using metaspace::Metachunk;
using metaspace::SizeCounter;

class ChunkHeaderPoolTest {

  static const size_t max_cap = 0x1000;

  ChunkHeaderPool _pool;

  // Array of the same size as the pool max capacity; holds the allocated elements.
  Metachunk* _elems[max_cap];
  SizeCounter _num_allocated;

  void attempt_free_at(size_t index) {

    LOG("attempt_free_at " SIZE_FORMAT ".", index);

    if (_elems[index] == NULL) {
      return;
    }

    _pool.return_chunk_header(_elems[index]);
    _elems[index] = NULL;

    _num_allocated.decrement();
    DEBUG_ONLY(_num_allocated.check(_pool.used());)

    DEBUG_ONLY(_pool.verify();)

  }

  void attempt_allocate_at(size_t index) {

    LOG("attempt_allocate_at " SIZE_FORMAT ".", index);

    if (_elems[index] != NULL) {
      return;
    }

    Metachunk* c = _pool.allocate_chunk_header();
    EXPECT_NOT_NULL(c);
    _elems[index] = c;
    c->set_free();

    _num_allocated.increment();
    DEBUG_ONLY(_num_allocated.check(_pool.used());)

    DEBUG_ONLY(_pool.verify();)
  }

  void attempt_allocate_or_free_at(size_t index) {
    if (_elems[index] == NULL) {
      attempt_allocate_at(index);
    } else {
      attempt_free_at(index);
    }
  }

  // Randomly allocate from the pool and free. Slight preference for allocation.
  void test_random_alloc_free(int num_iterations) {

    for (int iter = 0; iter < num_iterations; iter++) {
      size_t index = (size_t)os::random() % max_cap;
      attempt_allocate_or_free_at(index);
    }

    DEBUG_ONLY(_pool.verify();)

  }

  static void test_once() {
    ChunkHeaderPoolTest test;
    test.test_random_alloc_free(100);
  }

public:

  ChunkHeaderPoolTest() : _pool() {
    memset(_elems, 0, sizeof(_elems));
  }

  static void run_tests() {
    for (int i = 0; i < 1000; i++) {
      test_once();
    }
  }

};

TEST_VM(metaspace, chunk_header_pool_basics) {

  ChunkHeaderPool pool;
  EXPECT_EQ(pool.used(), (int)0);
  EXPECT_EQ(pool.freelist_size(), (int)0);

  Metachunk* header = pool.allocate_chunk_header();
  EXPECT_NOT_NULL(header);
  EXPECT_EQ(pool.used(), 1);
  EXPECT_EQ(pool.freelist_size(), (int)0);

  header->set_free();
  pool.return_chunk_header(header);
  EXPECT_EQ(pool.used(), (int)0);
  EXPECT_EQ(pool.freelist_size(), 1);

  header = pool.allocate_chunk_header();
  EXPECT_NOT_NULL(header);
  EXPECT_EQ(pool.used(), 1);
  EXPECT_EQ(pool.freelist_size(), (int)0);

  header->set_free();
  pool.return_chunk_header(header);
  EXPECT_EQ(pool.used(), (int)0);
  EXPECT_EQ(pool.freelist_size(), 1);

}

TEST_VM(metaspace, chunk_header_pool) {
  ChunkHeaderPoolTest::run_tests();
}
