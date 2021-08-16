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
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/freeBlocks.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

using metaspace::FreeBlocks;
using metaspace::SizeCounter;

#define CHECK_CONTENT(fb, num_blocks_expected, word_size_expected) \
{ \
  if (word_size_expected > 0) { \
    EXPECT_FALSE(fb.is_empty()); \
  } else { \
    EXPECT_TRUE(fb.is_empty()); \
  } \
  EXPECT_EQ(fb.total_size(), (size_t)word_size_expected); \
  EXPECT_EQ(fb.count(), (int)num_blocks_expected); \
}

class FreeBlocksTest {

  FeederBuffer _fb;
  FreeBlocks _freeblocks;

  // random generator for block feeding
  RandSizeGenerator _rgen_feeding;

  // random generator for allocations (and, hence, deallocations)
  RandSizeGenerator _rgen_allocations;

  SizeCounter _allocated_words;

  struct allocation_t {
    allocation_t* next;
    size_t word_size;
    MetaWord* p;
  };

  // Array of the same size as the pool max capacity; holds the allocated elements.
  allocation_t* _allocations;

  int _num_allocs;
  int _num_deallocs;
  int _num_feeds;

  bool feed_some() {
    size_t word_size = _rgen_feeding.get();
    MetaWord* p = _fb.get(word_size);
    if (p != NULL) {
      _freeblocks.add_block(p, word_size);
      return true;
    }
    return false;
  }

  void deallocate_top() {

    allocation_t* a = _allocations;
    if (a != NULL) {
      _allocations = a->next;
      check_marked_range(a->p, a->word_size);
      _freeblocks.add_block(a->p, a->word_size);
      delete a;
      DEBUG_ONLY(_freeblocks.verify();)
    }
  }

  bool allocate() {

    size_t word_size = MAX2(_rgen_allocations.get(), _freeblocks.MinWordSize);
    MetaWord* p = _freeblocks.remove_block(word_size);
    if (p != NULL) {
      _allocated_words.increment_by(word_size);
      allocation_t* a = new allocation_t;
      a->p = p; a->word_size = word_size;
      a->next = _allocations;
      _allocations = a;
      DEBUG_ONLY(_freeblocks.verify();)
      mark_range(p, word_size);
      return true;
    }
    return false;
  }

  void test_all_marked_ranges() {
    for (allocation_t* a = _allocations; a != NULL; a = a->next) {
      check_marked_range(a->p, a->word_size);
    }
  }

  void test_loop() {
    // We loop and in each iteration execute one of three operations:
    // - allocation from fbl
    // - deallocation to fbl of a previously allocated block
    // - feeding a new larger block into the fbl (mimicks chunk retiring)
    // When we have fed all large blocks into the fbl (feedbuffer empty), we
    //  switch to draining the fbl completely (only allocs)
    bool forcefeed = false;
    bool draining = false;
    bool stop = false;
    int iter = 100000; // safety stop
    while (!stop && iter > 0) {
      iter --;
      int surprise = (int)os::random() % 10;
      if (!draining && (surprise >= 7 || forcefeed)) {
        forcefeed = false;
        if (feed_some()) {
          _num_feeds++;
        } else {
          // We fed all input memory into the fbl. Now lets proceed until the fbl is drained.
          draining = true;
        }
      } else if (!draining && surprise < 1) {
        deallocate_top();
        _num_deallocs++;
      } else {
        if (allocate()) {
          _num_allocs++;
        } else {
          if (draining) {
            stop = _freeblocks.total_size() < 512;
          } else {
            forcefeed = true;
          }
        }
      }
      if ((iter % 1000) == 0) {
        DEBUG_ONLY(_freeblocks.verify();)
        test_all_marked_ranges();
        LOG("a %d (" SIZE_FORMAT "), d %d, f %d", _num_allocs, _allocated_words.get(), _num_deallocs, _num_feeds);
#ifdef LOG_PLEASE
        _freeblocks.print(tty, true);
        tty->cr();
#endif
      }
    }

    // Drain

  }

public:

  FreeBlocksTest(size_t avg_alloc_size) :
    _fb(512 * K), _freeblocks(),
    _rgen_feeding(128, 4096),
    _rgen_allocations(avg_alloc_size / 4, avg_alloc_size * 2, 0.01f, avg_alloc_size / 3, avg_alloc_size * 30),
    _allocations(NULL),
    _num_allocs(0),
    _num_deallocs(0),
    _num_feeds(0)
  {
    CHECK_CONTENT(_freeblocks, 0, 0);
    // some initial feeding
    _freeblocks.add_block(_fb.get(1024), 1024);
    CHECK_CONTENT(_freeblocks, 1, 1024);
  }

  static void test_small_allocations() {
    FreeBlocksTest test(10);
    test.test_loop();
  }

  static void test_medium_allocations() {
    FreeBlocksTest test(30);
    test.test_loop();
  }

  static void test_large_allocations() {
    FreeBlocksTest test(150);
    test.test_loop();
  }

};

TEST_VM(metaspace, freeblocks_basics) {

  FreeBlocks fbl;
  MetaWord tmp[1024];
  CHECK_CONTENT(fbl, 0, 0);

  fbl.add_block(tmp, 1024);
  DEBUG_ONLY(fbl.verify();)
  ASSERT_FALSE(fbl.is_empty());
  CHECK_CONTENT(fbl, 1, 1024);

  MetaWord* p = fbl.remove_block(1024);
  EXPECT_EQ(p, tmp);
  DEBUG_ONLY(fbl.verify();)
  CHECK_CONTENT(fbl, 0, 0);

}

TEST_VM(metaspace, freeblocks_small) {
  FreeBlocksTest::test_small_allocations();
}

TEST_VM(metaspace, freeblocks_medium) {
  FreeBlocksTest::test_medium_allocations();
}

TEST_VM(metaspace, freeblocks_large) {
  FreeBlocksTest::test_large_allocations();
}

