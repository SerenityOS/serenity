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
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metaspaceArena.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"
#include "metaspaceGtestRangeHelpers.hpp"

using metaspace::ArenaGrowthPolicy;
using metaspace::CommitLimiter;
using metaspace::InternalStats;
using metaspace::MemRangeCounter;
using metaspace::MetaspaceArena;
using metaspace::SizeAtomicCounter;
using metaspace::Settings;
using metaspace::ArenaStats;

// See metaspaceArena.cpp : needed for predicting commit sizes.
namespace metaspace {
  extern size_t get_raw_word_size_for_requested_word_size(size_t net_word_size);
}

class MetaspaceArenaTestHelper {

  MetaspaceGtestContext& _context;

  Mutex* _lock;
  const ArenaGrowthPolicy* _growth_policy;
  SizeAtomicCounter _used_words_counter;
  MetaspaceArena* _arena;

  void initialize(const ArenaGrowthPolicy* growth_policy, const char* name = "gtest-MetaspaceArena") {
    _growth_policy = growth_policy;
    _lock = new Mutex(Monitor::native, "gtest-MetaspaceArenaTest-lock", false, Monitor::_safepoint_check_never);
    // Lock during space creation, since this is what happens in the VM too
    //  (see ClassLoaderData::metaspace_non_null(), which we mimick here).
    {
      MutexLocker ml(_lock,  Mutex::_no_safepoint_check_flag);
      _arena = new MetaspaceArena(&_context.cm(), _growth_policy, _lock, &_used_words_counter, name);
    }
    DEBUG_ONLY(_arena->verify());

  }

public:

  // Create a helper; growth policy for arena is determined by the given spacetype|class tupel
  MetaspaceArenaTestHelper(MetaspaceGtestContext& helper,
                            Metaspace::MetaspaceType space_type, bool is_class,
                            const char* name = "gtest-MetaspaceArena") :
    _context(helper)
  {
    initialize(ArenaGrowthPolicy::policy_for_space_type(space_type, is_class), name);
  }

  // Create a helper; growth policy is directly specified
  MetaspaceArenaTestHelper(MetaspaceGtestContext& helper, const ArenaGrowthPolicy* growth_policy,
                           const char* name = "gtest-MetaspaceArena") :
    _context(helper)
  {
    initialize(growth_policy, name);
  }

  ~MetaspaceArenaTestHelper() {
    delete_arena_with_tests();
    delete _lock;
  }

  const CommitLimiter& limiter() const { return _context.commit_limiter(); }
  MetaspaceArena* arena() const { return _arena; }
  SizeAtomicCounter& used_words_counter() { return _used_words_counter; }

  // Note: all test functions return void due to gtests limitation that we cannot use ASSERT
  // in non-void returning tests.

  void delete_arena_with_tests() {
    if (_arena != NULL) {
      size_t used_words_before = _used_words_counter.get();
      size_t committed_words_before = limiter().committed_words();
      DEBUG_ONLY(_arena->verify());
      delete _arena;
      _arena = NULL;
      size_t used_words_after = _used_words_counter.get();
      size_t committed_words_after = limiter().committed_words();
      ASSERT_0(used_words_after);
      if (Settings::uncommit_free_chunks()) {
        ASSERT_LE(committed_words_after, committed_words_before);
      } else {
        ASSERT_EQ(committed_words_after, committed_words_before);
      }
    }
  }

  void usage_numbers_with_test(size_t* p_used, size_t* p_committed, size_t* p_capacity) const {
    _arena->usage_numbers(p_used, p_committed, p_capacity);
    if (p_used != NULL) {
      if (p_committed != NULL) {
        ASSERT_GE(*p_committed, *p_used);
      }
      // Since we own the used words counter, it should reflect our usage number 1:1
      ASSERT_EQ(_used_words_counter.get(), *p_used);
    }
    if (p_committed != NULL && p_capacity != NULL) {
      ASSERT_GE(*p_capacity, *p_committed);
    }
  }

  // Allocate; caller expects success; return pointer in *p_return_value
  void allocate_from_arena_with_tests_expect_success(MetaWord** p_return_value, size_t word_size) {
    allocate_from_arena_with_tests(p_return_value, word_size);
    ASSERT_NOT_NULL(*p_return_value);
  }

  // Allocate; caller expects success but is not interested in return value
  void allocate_from_arena_with_tests_expect_success(size_t word_size) {
    MetaWord* dummy = NULL;
    allocate_from_arena_with_tests_expect_success(&dummy, word_size);
  }

  // Allocate; caller expects failure
  void allocate_from_arena_with_tests_expect_failure(size_t word_size) {
    MetaWord* dummy = NULL;
    allocate_from_arena_with_tests(&dummy, word_size);
    ASSERT_NULL(dummy);
  }

  // Allocate; it may or may not work; return value in *p_return_value
  void allocate_from_arena_with_tests(MetaWord** p_return_value, size_t word_size) {

    // Note: usage_numbers walks all chunks in use and counts.
    size_t used = 0, committed = 0, capacity = 0;
    usage_numbers_with_test(&used, &committed, &capacity);

    size_t possible_expansion = limiter().possible_expansion_words();

    MetaWord* p = _arena->allocate(word_size);

    SOMETIMES(DEBUG_ONLY(_arena->verify();))

    size_t used2 = 0, committed2 = 0, capacity2 = 0;
    usage_numbers_with_test(&used2, &committed2, &capacity2);

    if (p == NULL) {
      // Allocation failed.
      if (Settings::new_chunks_are_fully_committed()) {
        ASSERT_LT(possible_expansion, MAX_CHUNK_WORD_SIZE);
      } else {
        ASSERT_LT(possible_expansion, word_size);
      }

      ASSERT_EQ(used, used2);
      ASSERT_EQ(committed, committed2);
      ASSERT_EQ(capacity, capacity2);
    } else {
      // Allocation succeeded. Should be correctly aligned.
      ASSERT_TRUE(is_aligned(p, sizeof(MetaWord)));
      // used: may go up or may not (since our request may have been satisfied from the freeblocklist
      //   whose content already counts as used).
      // committed: may go up, may not
      // capacity: ditto
      ASSERT_GE(used2, used);
      ASSERT_GE(committed2, committed);
      ASSERT_GE(capacity2, capacity);
    }

    *p_return_value = p;
  }

  // Allocate; it may or may not work; but caller does not care for the result value
  void allocate_from_arena_with_tests(size_t word_size) {
    MetaWord* dummy = NULL;
    allocate_from_arena_with_tests(&dummy, word_size);
  }

  void deallocate_with_tests(MetaWord* p, size_t word_size) {
    size_t used = 0, committed = 0, capacity = 0;
    usage_numbers_with_test(&used, &committed, &capacity);

    _arena->deallocate(p, word_size);

    SOMETIMES(DEBUG_ONLY(_arena->verify();))

    size_t used2 = 0, committed2 = 0, capacity2 = 0;
    usage_numbers_with_test(&used2, &committed2, &capacity2);

    // Nothing should have changed. Deallocated blocks are added to the free block list
    // which still counts as used.
    ASSERT_EQ(used2, used);
    ASSERT_EQ(committed2, committed);
    ASSERT_EQ(capacity2, capacity);
  }

  ArenaStats get_arena_statistics() const {
    ArenaStats stats;
    _arena->add_to_statistics(&stats);
    return stats;
  }

  // Convenience method to return number of chunks in arena (including current chunk)
  int get_number_of_chunks() const {
    return get_arena_statistics().totals()._num;
  }

};

static void test_basics(size_t commit_limit, bool is_micro) {
  MetaspaceGtestContext context(commit_limit);
  MetaspaceArenaTestHelper helper(context, is_micro ? Metaspace::ReflectionMetaspaceType : Metaspace::StandardMetaspaceType, false);

  helper.allocate_from_arena_with_tests(1);
  helper.allocate_from_arena_with_tests(128);
  helper.allocate_from_arena_with_tests(128 * K);
  helper.allocate_from_arena_with_tests(1);
  helper.allocate_from_arena_with_tests(128);
  helper.allocate_from_arena_with_tests(128 * K);
}

TEST_VM(metaspace, MetaspaceArena_basics_micro_nolimit) {
  test_basics(max_uintx, true);
}

TEST_VM(metaspace, MetaspaceArena_basics_micro_limit) {
  test_basics(256 * K, true);
}

TEST_VM(metaspace, MetaspaceArena_basics_standard_nolimit) {
  test_basics(max_uintx, false);
}

TEST_VM(metaspace, MetaspaceArena_basics_standard_limit) {
  test_basics(256 * K, false);
}

// Test chunk enlargement:
//  A single MetaspaceArena, left undisturbed with place to grow. Slowly fill arena up.
//  We should see at least some occurrences of chunk-in-place enlargement.
static void test_chunk_enlargment_simple(Metaspace::MetaspaceType spacetype, bool is_class) {

  MetaspaceGtestContext context;
  MetaspaceArenaTestHelper helper(context, (Metaspace::MetaspaceType)spacetype, is_class);

  uint64_t n1 = metaspace::InternalStats::num_chunks_enlarged();

  size_t allocated = 0;
  while (allocated <= MAX_CHUNK_WORD_SIZE &&
         metaspace::InternalStats::num_chunks_enlarged() == n1) {
    size_t s = IntRange(32, 128).random_value();
    helper.allocate_from_arena_with_tests_expect_success(s);
    allocated += metaspace::get_raw_word_size_for_requested_word_size(s);
  }

  EXPECT_GT(metaspace::InternalStats::num_chunks_enlarged(), n1);

}

// Do this test for some of the standard types; don't do it for the boot loader type
//  since that one starts out with max chunk size so we would not see any enlargement.

TEST_VM(metaspace, MetaspaceArena_test_enlarge_in_place_standard_c) {
  test_chunk_enlargment_simple(Metaspace::StandardMetaspaceType, true);
}

TEST_VM(metaspace, MetaspaceArena_test_enlarge_in_place_standard_nc) {
  test_chunk_enlargment_simple(Metaspace::StandardMetaspaceType, false);
}

TEST_VM(metaspace, MetaspaceArena_test_enlarge_in_place_micro_c) {
  test_chunk_enlargment_simple(Metaspace::ReflectionMetaspaceType, true);
}

TEST_VM(metaspace, MetaspaceArena_test_enlarge_in_place_micro_nc) {
  test_chunk_enlargment_simple(Metaspace::ReflectionMetaspaceType, false);
}

// Test chunk enlargement:
// A single MetaspaceArena, left undisturbed with place to grow. Slowly fill arena up.
//  We should see occurrences of chunk-in-place enlargement.
//  Here, we give it an ideal policy which should enable the initial chunk to grow unmolested
//  until finish.
TEST_VM(metaspace, MetaspaceArena_test_enlarge_in_place_2) {

  if (Settings::use_allocation_guard()) {
    return;
  }

  // Note: internally, chunk in-place enlargement is disallowed if growing the chunk
  //  would cause the arena to claim more memory than its growth policy allows. This
  //  is done to prevent the arena to grow too fast.
  //
  // In order to test in-place growth here without that restriction I give it an
  //  artificial growth policy which starts out with a tiny chunk size, then balloons
  //  right up to max chunk size. This will cause the initial chunk to be tiny, and
  //  then the arena is able to grow it without violating growth policy.
  chunklevel_t growth[] = { HIGHEST_CHUNK_LEVEL, ROOT_CHUNK_LEVEL };
  ArenaGrowthPolicy growth_policy(growth, 2);

  MetaspaceGtestContext context;
  MetaspaceArenaTestHelper helper(context, &growth_policy);

  uint64_t n1 = metaspace::InternalStats::num_chunks_enlarged();

  size_t allocated = 0;
  while (allocated <= MAX_CHUNK_WORD_SIZE) {
    size_t s = IntRange(32, 128).random_value();
    helper.allocate_from_arena_with_tests_expect_success(s);
    allocated += metaspace::get_raw_word_size_for_requested_word_size(s);
    if (allocated <= MAX_CHUNK_WORD_SIZE) {
      // Chunk should have been enlarged in place
      ASSERT_EQ(1, helper.get_number_of_chunks());
    } else {
      // Next chunk should have started
      ASSERT_EQ(2, helper.get_number_of_chunks());
    }
  }

  int times_chunk_were_enlarged = metaspace::InternalStats::num_chunks_enlarged() - n1;
  LOG("chunk was enlarged %d times.", times_chunk_were_enlarged);

  ASSERT_GT0(times_chunk_were_enlarged);

}

// Regression test: Given a single MetaspaceArena, left undisturbed with place to grow,
//  test that in place enlargement correctly fails if growing the chunk would bring us
//  beyond the max. size of a chunk.
TEST_VM(metaspace, MetaspaceArena_test_failing_to_enlarge_in_place_max_chunk_size) {

  if (Settings::use_allocation_guard()) {
    return;
  }

  MetaspaceGtestContext context;

  for (size_t first_allocation_size = 1; first_allocation_size <= MAX_CHUNK_WORD_SIZE / 2; first_allocation_size *= 2) {

    MetaspaceArenaTestHelper helper(context, Metaspace::StandardMetaspaceType, false);

    // we allocate first a small amount, then the full amount possible.
    // The sum of first and second allocation should bring us above root chunk size.
    // This should work, we should not see any problems, but no chunk enlargement should
    // happen.
    int n1 = metaspace::InternalStats::num_chunks_enlarged();

    helper.allocate_from_arena_with_tests_expect_success(first_allocation_size);
    EXPECT_EQ(helper.get_number_of_chunks(), 1);

    helper.allocate_from_arena_with_tests_expect_success(MAX_CHUNK_WORD_SIZE - first_allocation_size + 1);
    EXPECT_EQ(helper.get_number_of_chunks(), 2);

    int times_chunk_were_enlarged = metaspace::InternalStats::num_chunks_enlarged() - n1;
    LOG("chunk was enlarged %d times.", times_chunk_were_enlarged);

    EXPECT_0(times_chunk_were_enlarged);

  }
}

// Regression test: Given a single MetaspaceArena, left undisturbed with place to grow,
//  test that in place enlargement correctly fails if growing the chunk would cause more
//  than doubling its size
TEST_VM(metaspace, MetaspaceArena_test_failing_to_enlarge_in_place_doubling_chunk_size) {

  if (Settings::use_allocation_guard()) {
    return;
  }

  MetaspaceGtestContext context;
  MetaspaceArenaTestHelper helper(context, Metaspace::StandardMetaspaceType, false);

  int n1 = metaspace::InternalStats::num_chunks_enlarged();

  helper.allocate_from_arena_with_tests_expect_success(1000);
  EXPECT_EQ(helper.get_number_of_chunks(), 1);

  helper.allocate_from_arena_with_tests_expect_success(4000);
  EXPECT_EQ(helper.get_number_of_chunks(), 2);

  int times_chunk_were_enlarged = metaspace::InternalStats::num_chunks_enlarged() - n1;
  LOG("chunk was enlarged %d times.", times_chunk_were_enlarged);

  EXPECT_0(times_chunk_were_enlarged);

}

// Test the MetaspaceArenas' free block list:
// Allocate, deallocate, then allocate the same block again. The second allocate should
// reuse the deallocated block.
TEST_VM(metaspace, MetaspaceArena_deallocate) {
  if (Settings::use_allocation_guard()) {
    return;
  }
  for (size_t s = 2; s <= MAX_CHUNK_WORD_SIZE; s *= 2) {
    MetaspaceGtestContext context;
    MetaspaceArenaTestHelper helper(context, Metaspace::StandardMetaspaceType, false);

    MetaWord* p1 = NULL;
    helper.allocate_from_arena_with_tests_expect_success(&p1, s);

    size_t used1 = 0, capacity1 = 0;
    helper.usage_numbers_with_test(&used1, NULL, &capacity1);
    ASSERT_EQ(used1, s);

    helper.deallocate_with_tests(p1, s);

    size_t used2 = 0, capacity2 = 0;
    helper.usage_numbers_with_test(&used2, NULL, &capacity2);
    ASSERT_EQ(used1, used2);
    ASSERT_EQ(capacity2, capacity2);

    MetaWord* p2 = NULL;
    helper.allocate_from_arena_with_tests_expect_success(&p2, s);

    size_t used3 = 0, capacity3 = 0;
    helper.usage_numbers_with_test(&used3, NULL, &capacity3);
    ASSERT_EQ(used3, used2);
    ASSERT_EQ(capacity3, capacity2);

    // Actually, we should get the very same allocation back
    ASSERT_EQ(p1, p2);
  }
}

static void test_recover_from_commit_limit_hit() {

  if (Settings::new_chunks_are_fully_committed()) {
    return; // This would throw off the commit counting in this test.
  }

  // Test:
  // - Multiple MetaspaceArena allocate (operating under the same commit limiter).
  // - One, while attempting to commit parts of its current chunk on demand,
  //   triggers the limit and cannot commit its chunk further.
  // - We release the other MetaspaceArena - its content is put back to the
  //   freelists.
  // - We re-attempt allocation from the first manager. It should now succeed.
  //
  // This means if the first MetaspaceArena may have to let go of its current chunk and
  // retire it and take a fresh chunk from the freelist.

  const size_t commit_limit = Settings::commit_granule_words() * 10;
  MetaspaceGtestContext context(commit_limit);

  // The first MetaspaceArena mimicks a micro loader. This will fill the free
  //  chunk list with very small chunks. We allocate from them in an interleaved
  //  way to cause fragmentation.
  MetaspaceArenaTestHelper helper1(context, Metaspace::ReflectionMetaspaceType, false);
  MetaspaceArenaTestHelper helper2(context, Metaspace::ReflectionMetaspaceType, false);

  // This MetaspaceArena should hit the limit. We use BootMetaspaceType here since
  // it gets a large initial chunk which is committed
  // on demand and we are likely to hit a commit limit while trying to expand it.
  MetaspaceArenaTestHelper helper3(context, Metaspace::BootMetaspaceType, false);

  // Allocate space until we have below two but above one granule left
  size_t allocated_from_1_and_2 = 0;
  while (context.commit_limiter().possible_expansion_words() >= Settings::commit_granule_words() * 2 &&
      allocated_from_1_and_2 < commit_limit) {
    helper1.allocate_from_arena_with_tests_expect_success(1);
    helper2.allocate_from_arena_with_tests_expect_success(1);
    allocated_from_1_and_2 += 2;
  }

  // Now, allocating from helper3, creep up on the limit
  size_t allocated_from_3 = 0;
  MetaWord* p = NULL;
  while ( (helper3.allocate_from_arena_with_tests(&p, 1), p != NULL) &&
         ++allocated_from_3 < Settings::commit_granule_words() * 2);

  EXPECT_LE(allocated_from_3, Settings::commit_granule_words() * 2);

  // We expect the freelist to be empty of committed space...
  EXPECT_0(context.cm().calc_committed_word_size());

  //msthelper.cm().print_on(tty);

  // Release the first MetaspaceArena.
  helper1.delete_arena_with_tests();

  //msthelper.cm().print_on(tty);

  // Should have populated the freelist with committed space
  // We expect the freelist to be empty of committed space...
  EXPECT_GT(context.cm().calc_committed_word_size(), (size_t)0);

  // Repeat allocation from helper3, should now work.
  helper3.allocate_from_arena_with_tests_expect_success(1);

}

TEST_VM(metaspace, MetaspaceArena_recover_from_limit_hit) {
  test_recover_from_commit_limit_hit();
}

static void test_controlled_growth(Metaspace::MetaspaceType type, bool is_class,
                                   size_t expected_starting_capacity,
                                   bool test_in_place_enlargement)
{

  if (Settings::use_allocation_guard()) {
    return;
  }

  // From a MetaspaceArena in a clean room allocate tiny amounts;
  // watch it grow. Used/committed/capacity should not grow in
  // large jumps. Also, different types of MetaspaceArena should
  // have different initial capacities.

  MetaspaceGtestContext context;
  MetaspaceArenaTestHelper smhelper(context, type, is_class, "Grower");

  MetaspaceArenaTestHelper smhelper_harrasser(context, Metaspace::ReflectionMetaspaceType, true, "Harasser");

  size_t used = 0, committed = 0, capacity = 0;
  const size_t alloc_words = 16;

  smhelper.arena()->usage_numbers(&used, &committed, &capacity);
  ASSERT_0(used);
  ASSERT_0(committed);
  ASSERT_0(capacity);

  ///// First allocation //

  smhelper.allocate_from_arena_with_tests_expect_success(alloc_words);

  smhelper.arena()->usage_numbers(&used, &committed, &capacity);

  ASSERT_EQ(used, alloc_words);
  ASSERT_GE(committed, used);
  ASSERT_GE(capacity, committed);

  ASSERT_EQ(capacity, expected_starting_capacity);

  if (!(Settings::new_chunks_are_fully_committed() && type == Metaspace::BootMetaspaceType)) {
    // Initial commit charge for the whole context should be one granule
    ASSERT_EQ(context.committed_words(), Settings::commit_granule_words());
    // Initial commit number for the arena should be less since - apart from boot loader - no
    //  space type has large initial chunks.
    ASSERT_LE(committed, Settings::commit_granule_words());
  }

  ///// subsequent allocations //

  DEBUG_ONLY(const uintx num_chunk_enlarged = metaspace::InternalStats::num_chunks_enlarged();)

  size_t words_allocated = 0;
  int num_allocated = 0;
  const size_t safety = MAX_CHUNK_WORD_SIZE * 1.2;
  size_t highest_capacity_jump = capacity;
  int num_capacity_jumps = 0;

  while (words_allocated < safety && num_capacity_jumps < 15) {

    // if we want to test growth with in-place chunk enlargement, leave MetaspaceArena
    // undisturbed; it will have all the place to grow. Otherwise allocate from a little
    // side arena to increase fragmentation.
    // (Note that this does not completely prevent in-place chunk enlargement but makes it
    //  rather improbable)
    if (!test_in_place_enlargement) {
      smhelper_harrasser.allocate_from_arena_with_tests_expect_success(alloc_words * 2);
    }

    smhelper.allocate_from_arena_with_tests_expect_success(alloc_words);
    words_allocated += metaspace::get_raw_word_size_for_requested_word_size(alloc_words);
    num_allocated++;

    size_t used2 = 0, committed2 = 0, capacity2 = 0;

    smhelper.arena()->usage_numbers(&used2, &committed2, &capacity2);

    // used should not grow larger than what we allocated, plus possible overhead.
    ASSERT_GE(used2, used);
    ASSERT_LE(used2, used + alloc_words * 2);
    ASSERT_LE(used2, words_allocated + 100);
    used = used2;

    // A jump in committed words should not be larger than commit granule size.
    // It can be smaller, since the current chunk of the MetaspaceArena may be
    // smaller than a commit granule.
    // (Note: unless root chunks are born fully committed)
    ASSERT_GE(committed2, used2);
    ASSERT_GE(committed2, committed);
    const size_t committed_jump = committed2 - committed;
    if (committed_jump > 0 && !Settings::new_chunks_are_fully_committed()) {
      ASSERT_LE(committed_jump, Settings::commit_granule_words());
    }
    committed = committed2;

    // Capacity jumps: Test that arenas capacity does not grow too fast.
    ASSERT_GE(capacity2, committed2);
    ASSERT_GE(capacity2, capacity);
    const size_t capacity_jump = capacity2 - capacity;
    if (capacity_jump > 0) {
      LOG(">" SIZE_FORMAT "->" SIZE_FORMAT "(+" SIZE_FORMAT ")", capacity, capacity2, capacity_jump)
      if (capacity_jump > highest_capacity_jump) {
        /* Disabled for now since this is rather shaky. The way it is tested makes it too dependent
         * on allocation history. Need to rethink this.
        ASSERT_LE(capacity_jump, highest_capacity_jump * 2);
        ASSERT_GE(capacity_jump, MIN_CHUNK_WORD_SIZE);
        ASSERT_LE(capacity_jump, MAX_CHUNK_WORD_SIZE);
        */
        highest_capacity_jump = capacity_jump;
      }
      num_capacity_jumps++;
    }

    capacity = capacity2;

  }

  // After all this work, we should see an increase in number of chunk-in-place-enlargements
  //  (this especially is vulnerable to regression: the decisions of when to do in-place-enlargements are somewhat
  //   complicated, see MetaspaceArena::attempt_enlarge_current_chunk())
#ifdef ASSERT
  if (test_in_place_enlargement) {
    const uintx num_chunk_enlarged_2 = metaspace::InternalStats::num_chunks_enlarged();
    ASSERT_GT(num_chunk_enlarged_2, num_chunk_enlarged);
  }
#endif
}

// these numbers have to be in sync with arena policy numbers (see memory/metaspace/arenaGrowthPolicy.cpp)
TEST_VM(metaspace, MetaspaceArena_growth_refl_c_inplace) {
  test_controlled_growth(Metaspace::ReflectionMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_refl_c_not_inplace) {
  test_controlled_growth(Metaspace::ReflectionMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1K), false);
}

TEST_VM(metaspace, MetaspaceArena_growth_anon_c_inplace) {
  test_controlled_growth(Metaspace::ClassMirrorHolderMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_anon_c_not_inplace) {
  test_controlled_growth(Metaspace::ClassMirrorHolderMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1K), false);
}

TEST_VM(metaspace, MetaspaceArena_growth_standard_c_inplace) {
  test_controlled_growth(Metaspace::StandardMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_2K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_standard_c_not_inplace) {
  test_controlled_growth(Metaspace::StandardMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_2K), false);
}

/* Disabled growth tests for BootMetaspaceType: there, the growth steps are too rare,
 * and too large, to make any reliable guess as toward chunks get enlarged in place.
TEST_VM(metaspace, MetaspaceArena_growth_boot_c_inplace) {
  test_controlled_growth(Metaspace::BootMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1M), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_boot_c_not_inplace) {
  test_controlled_growth(Metaspace::BootMetaspaceType, true,
                         word_size_for_level(CHUNK_LEVEL_1M), false);
}
*/

TEST_VM(metaspace, MetaspaceArena_growth_refl_nc_inplace) {
  test_controlled_growth(Metaspace::ReflectionMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_2K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_refl_nc_not_inplace) {
  test_controlled_growth(Metaspace::ReflectionMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_2K), false);
}

TEST_VM(metaspace, MetaspaceArena_growth_anon_nc_inplace) {
  test_controlled_growth(Metaspace::ClassMirrorHolderMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_1K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_anon_nc_not_inplace) {
  test_controlled_growth(Metaspace::ClassMirrorHolderMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_1K), false);
}

TEST_VM(metaspace, MetaspaceArena_growth_standard_nc_inplace) {
  test_controlled_growth(Metaspace::StandardMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_4K), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_standard_nc_not_inplace) {
  test_controlled_growth(Metaspace::StandardMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_4K), false);
}

/* Disabled growth tests for BootMetaspaceType: there, the growth steps are too rare,
 * and too large, to make any reliable guess as toward chunks get enlarged in place.
TEST_VM(metaspace, MetaspaceArena_growth_boot_nc_inplace) {
  test_controlled_growth(Metaspace::BootMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_4M), true);
}

TEST_VM(metaspace, MetaspaceArena_growth_boot_nc_not_inplace) {
  test_controlled_growth(Metaspace::BootMetaspaceType, false,
                         word_size_for_level(CHUNK_LEVEL_4M), false);
}
*/
