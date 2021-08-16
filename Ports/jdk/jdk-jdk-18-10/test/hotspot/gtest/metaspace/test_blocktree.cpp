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
#include "memory/metaspace/blockTree.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/resourceArea.hpp"
// #define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"

using metaspace::BlockTree;
using metaspace::MemRangeCounter;

// Small helper. Given a 0-terminated array of sizes, a feeder buffer and a tree,
//  add blocks of these sizes to the tree in the order they appear in the array.
static void create_nodes(const size_t sizes[], FeederBuffer& fb, BlockTree& bt) {
  for (int i = 0; sizes[i] > 0; i ++) {
    size_t s = sizes[i];
    MetaWord* p = fb.get(s);
    bt.add_block(p, s);
  }
}

#define CHECK_BT_CONTENT(bt, expected_num, expected_size) { \
  EXPECT_EQ(bt.count(), (unsigned)expected_num); \
  EXPECT_EQ(bt.total_size(), (size_t)expected_size); \
  if (expected_num == 0) { \
    EXPECT_TRUE(bt.is_empty()); \
  } else { \
    EXPECT_FALSE(bt.is_empty()); \
  } \
}

TEST_VM(metaspace, BlockTree_basic) {

  BlockTree bt;
  CHECK_BT_CONTENT(bt, 0, 0);

  size_t real_size = 0;
  MetaWord* p = NULL;
  MetaWord arr[10000];

  ASSERT_LE(BlockTree::MinWordSize, (size_t)6); // Sanity check. Adjust if Node is changed.

  const size_t minws = BlockTree::MinWordSize;

  // remove_block from empty tree should yield nothing
  p = bt.remove_block(minws, &real_size);
  EXPECT_NULL(p);
  EXPECT_0(real_size);
  CHECK_BT_CONTENT(bt, 0, 0);

  // Add some blocks and retrieve them right away.
  size_t sizes[] = {
      minws, // smallest possible
      minws + 10,
      1024,
      4711,
      0
  };

  for (int i = 0; sizes[i] > 0; i++) {
    bt.add_block(arr, sizes[i]);
    CHECK_BT_CONTENT(bt, 1, sizes[i]);

    DEBUG_ONLY(bt.verify();)

    MetaWord* p = bt.remove_block(sizes[i], &real_size);
    EXPECT_EQ(p, arr);
    EXPECT_EQ(real_size, (size_t)sizes[i]);
    CHECK_BT_CONTENT(bt, 0, 0);
  }

}

// Helper for test_find_nearest_fit_with_tree.
// Out of an array of sizes return the closest upper match to a requested size.
// Returns SIZE_MAX if none found.
static size_t helper_find_nearest_fit(const size_t sizes[], size_t request_size) {
  size_t best = SIZE_MAX;
  for (int i = 0; sizes[i] > 0; i++) {
    if (sizes[i] >= request_size && sizes[i] < best) {
      best = sizes[i];
    }
  }
  return best;
}

// Given a sequence of (0-terminated) sizes, add blocks of those sizes to the tree in the order given. Then, ask
// for a request size and check that it is the expected result.
static void test_find_nearest_fit_with_tree(const size_t sizes[], size_t request_size) {

  BlockTree bt;
  FeederBuffer fb(4 * K);

  create_nodes(sizes, fb, bt);

  DEBUG_ONLY(bt.verify();)

  size_t expected_size = helper_find_nearest_fit(sizes, request_size);
  size_t real_size = 0;
  MetaWord* p = bt.remove_block(request_size, &real_size);

  if (expected_size != SIZE_MAX) {
    EXPECT_NOT_NULL(p);
    EXPECT_EQ(real_size, expected_size);
  } else {
    EXPECT_NULL(p);
    EXPECT_0(real_size);
  }

  LOG(SIZE_FORMAT ": " SIZE_FORMAT ".", request_size, real_size);

}

TEST_VM(metaspace, BlockTree_find_nearest_fit) {

  // Test tree for test_find_nearest_fit looks like this
  //                30
  //               /  \
  //              /    \
  //             /      \
  //            17       50
  //           /  \     /  \
  //          /    \   /    \
  //         10    28 32     51
  //                    \
  //                     35

  static const size_t sizes[] = {
    30, 17, 10, 28,
    50, 32, 51, 35,
    0 // stop
  };

  BlockTree bt;
  FeederBuffer fb(4 * K);

  create_nodes(sizes, fb, bt);

  for (int i = BlockTree::MinWordSize; i <= 60; i ++) {
    test_find_nearest_fit_with_tree(sizes, i);
  }

}

// Test repeated adding and removing of blocks of the same size, which
// should exercise the list-part of the tree.
TEST_VM(metaspace, BlockTree_basic_siblings)
{
  BlockTree bt;
  FeederBuffer fb(4 * K);

  CHECK_BT_CONTENT(bt, 0, 0);

  const size_t test_size = BlockTree::MinWordSize;
  const int num = 10;

  for (int i = 0; i < num; i++) {
    bt.add_block(fb.get(test_size), test_size);
    CHECK_BT_CONTENT(bt, i + 1, (i + 1) * test_size);
  }

  DEBUG_ONLY(bt.verify();)

  for (int i = num; i > 0; i --) {
    size_t real_size = 4711;
    MetaWord* p = bt.remove_block(test_size, &real_size);
    EXPECT_TRUE(fb.is_valid_pointer(p));
    EXPECT_EQ(real_size, (size_t)test_size);
    CHECK_BT_CONTENT(bt, i - 1, (i - 1) * test_size);
  }

}

#ifdef ASSERT
TEST_VM(metaspace, BlockTree_print_test) {

  static const size_t sizes[] = {
    30, 17, 10, 28,
    50, 32, 51, 35,
    0 // stop
  };

  BlockTree bt;
  FeederBuffer fb(4 * K);

  create_nodes(sizes, fb, bt);

  ResourceMark rm;

  stringStream ss;
  bt.print_tree(&ss);

  LOG("%s", ss.as_string());
}

// Test that an overwritten node would result in an assert and a printed tree
TEST_VM_ASSERT_MSG(metaspace, BlockTree_overwriter_test, ".*failed: Invalid node") {
  static const size_t sizes1[] = { 30, 17, 0 };
  static const size_t sizes2[] = { 12, 12, 0 };

  BlockTree bt;
  FeederBuffer fb(4 * K);

  // some nodes...
  create_nodes(sizes1, fb, bt);

  // a node we will break...
  MetaWord* p_broken = fb.get(12);
  bt.add_block(p_broken, 12);

  // some more nodes...
  create_nodes(sizes2, fb, bt);

  // overwrite node memory (only the very first byte), then verify tree.
  // Verification should catch the broken canary, print the tree,
  // then assert.
  LOG("Will break node at " PTR_FORMAT ".", p2i(p_broken));
  tty->print_cr("Death test, please ignore the following \"Invalid node\" printout.");
  *((char*)p_broken) = '\0';
  bt.verify();
}
#endif

class BlockTreeTest {

  FeederBuffer _fb;

  BlockTree _bt[2];
  MemRangeCounter _cnt[2];

  RandSizeGenerator _rgen;

#define CHECK_COUNTERS \
  CHECK_BT_CONTENT(_bt[0], _cnt[0].count(), _cnt[0].total_size()) \
  CHECK_BT_CONTENT(_bt[1], _cnt[1].count(), _cnt[1].total_size())

#define CHECK_COUNTERS_ARE_0 \
  CHECK_BT_CONTENT(_bt[0], 0, 0) \
  CHECK_BT_CONTENT(_bt[1], 0, 0)

#ifdef ASSERT
  void verify_trees() {
    _bt[0].verify();
    _bt[1].verify();
  }
#endif

  enum feeding_pattern_t {
    scatter = 1,
    left_right = 2,
    right_left = 3
  };

  // Feed the whole feeder buffer to the trees, according to feeding_pattern.
  void feed_all(feeding_pattern_t feeding_pattern) {

    MetaWord* p = NULL;
    unsigned added = 0;

    // If we feed in small graining, we cap the number of blocks to limit test duration.
    const unsigned max_blocks = 2000;

    size_t old_feeding_size = feeding_pattern == right_left ? _rgen.max() : _rgen.min();
    do {
      size_t s = 0;
      switch (feeding_pattern) {
      case scatter:
        // fill completely random
        s =_rgen.get();
        break;
      case left_right:
        // fill in ascending order to provoke a misformed tree.
        s = MIN2(_rgen.get(), old_feeding_size);
        old_feeding_size = s;
        break;
      case right_left:
        // same, but descending.
        s = MAX2(_rgen.get(), old_feeding_size);
        old_feeding_size = s;
        break;
      }

      // Get a block from the feeder buffer; feed it alternatingly to either tree.
      p = _fb.get(s);
      if (p != NULL) {
        int which = added % 2;
        added++;
        _bt[which].add_block(p, s);
        _cnt[which].add(s);
        CHECK_COUNTERS
      }
    } while (p != NULL && added < max_blocks);

    DEBUG_ONLY(verify_trees();)

    // Trees should contain the same number of nodes (+-1)
    EXPECT_TRUE(_bt[0].count() == _bt[1].count() ||
                _bt[0].count() == _bt[1].count() + 1);

  }

  void ping_pong_loop(int iterations) {

    // We loop and in each iteration randomly retrieve a block from one tree and add it to another.
    for (int i = 0; i < iterations; i++) {
      int taker = 0;
      int giver = 1;
      if ((os::random() % 10) > 5) {
        giver = 0; taker = 1;
      }
      size_t s =_rgen.get();
      size_t real_size = 0;
      MetaWord* p = _bt[giver].remove_block(s, &real_size);
      if (p != NULL) {
        ASSERT_TRUE(_fb.is_valid_range(p, real_size));
        ASSERT_GE(real_size, s);
        _bt[taker].add_block(p, real_size);
        _cnt[giver].sub(real_size);
        _cnt[taker].add(real_size);
        CHECK_COUNTERS;
      }

#ifdef ASSERT
      if (true) {//i % 1000 == 0) {
        verify_trees();
      }
#endif
    }
  }

  // Drain the trees. While draining, observe the order of the drained items.
  void drain_all() {

    for (int which = 0; which < 2; which++) {
      BlockTree* bt = _bt + which;
      size_t last_size = 0;
      while (!bt->is_empty()) {

        // We only query for the minimal size. Actually returned size should be
        // monotonously growing since remove_block should always return the closest fit.
        size_t real_size = 4711;
        MetaWord* p = bt->remove_block(BlockTree::MinWordSize, &real_size);
        ASSERT_TRUE(_fb.is_valid_range(p, real_size));

        ASSERT_GE(real_size, last_size);
        last_size = real_size;

        _cnt[which].sub(real_size);
        CHECK_COUNTERS;

        DEBUG_ONLY(bt->verify();)

      }
    }

  }

  void test(feeding_pattern_t feeding_pattern) {

    CHECK_COUNTERS_ARE_0

    feed_all(feeding_pattern);

    LOG("Blocks in circulation: bt1=%d:" SIZE_FORMAT ", bt2=%d:" SIZE_FORMAT ".",
        _bt[0].count(), _bt[0].total_size(),
        _bt[1].count(), _bt[1].total_size());

    ping_pong_loop(5000);

    LOG("After Pingpong: bt1=%d:" SIZE_FORMAT ", bt2=%d:" SIZE_FORMAT ".",
        _bt[0].count(), _bt[0].total_size(),
        _bt[1].count(), _bt[1].total_size());

    drain_all();

    CHECK_COUNTERS_ARE_0
  }

public:

  BlockTreeTest(size_t min_word_size, size_t max_word_size) :
    _fb(2 * M),
    _bt(),
    _rgen(min_word_size, max_word_size)
  {
    CHECK_COUNTERS;
    DEBUG_ONLY(verify_trees();)
  }

  void test_scatter()      { test(scatter); }
  void test_right_left()   { test(right_left); }
  void test_left_right()   { test(left_right); }

};

#define DO_TEST(name, feedingpattern, min, max) \
  TEST_VM(metaspace, BlockTree_##name##_##feedingpattern) { \
    BlockTreeTest btt(min, max); \
    btt.test_##feedingpattern(); \
  }

#define DO_TEST_ALL_PATTERNS(name, min, max) \
  DO_TEST(name, scatter, min, max) \
  DO_TEST(name, right_left, min, max) \
  DO_TEST(name, left_right, min, max)

DO_TEST_ALL_PATTERNS(wide, BlockTree::MinWordSize, 128 * K);
DO_TEST_ALL_PATTERNS(narrow, BlockTree::MinWordSize, 16)
DO_TEST_ALL_PATTERNS(129, BlockTree::MinWordSize, 129)
DO_TEST_ALL_PATTERNS(4K, BlockTree::MinWordSize, 4*K)

