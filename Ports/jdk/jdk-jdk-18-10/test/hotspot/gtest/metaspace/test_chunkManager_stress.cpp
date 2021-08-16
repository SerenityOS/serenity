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
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"
#include "metaspaceGtestRangeHelpers.hpp"
#include "metaspaceGtestSparseArray.hpp"

using metaspace::ChunkManager;
using metaspace::Settings;

class ChunkManagerRandomChunkAllocTest {

  static const size_t max_footprint_words = 8 * M;

  ChunkGtestContext _context;

  // All allocated live chunks
  typedef SparseArray<Metachunk*> SparseArrayOfChunks;
  SparseArrayOfChunks _chunks;

  const ChunkLevelRange _chunklevel_range;
  const float _commit_factor;

  // Depending on a probability pattern, come up with a reasonable limit to number of live chunks
  static int max_num_live_chunks(ChunkLevelRange r, float commit_factor) {
    // Assuming we allocate only the largest type of chunk, committed to the fullest commit factor,
    // how many chunks can we accomodate before hitting max_footprint_words?
    const size_t largest_chunk_size = word_size_for_level(r.lowest());
    int max_chunks = (max_footprint_words * commit_factor) / largest_chunk_size;
    // .. but cap at (min) 50 and (max) 1000
    max_chunks = MIN2(1000, max_chunks);
    max_chunks = MAX2(50, max_chunks);
    return max_chunks;
  }

  // Return true if, after an allocation error happened, a reserve error seems likely.
  bool could_be_reserve_error() {
    return _context.vslist().is_full();
  }

  // Return true if, after an allocation error happened, a commit error seems likely.
  bool could_be_commit_error(size_t additional_word_size) {

    // could it be commit limit hit?

    if (Settings::new_chunks_are_fully_committed()) {
      // For all we know we may have just failed to fully-commit a new root chunk.
      additional_word_size = MAX_CHUNK_WORD_SIZE;
    }

    // Note that this is difficult to verify precisely, since there are
    // several layers of truth:
    // a) at the lowest layer (RootChunkArea) we have a bitmap of committed granules;
    // b) at the vslist layer, we keep running counters of committed/reserved words;
    // c) at the chunk layer, we keep a commit watermark (committed_words).
    //
    // (a) should mirror reality.
    // (a) and (b) should be precisely in sync. This is tested by
    // VirtualSpaceList::verify().
    // (c) can be, by design, imprecise (too low).
    //
    // Here, I check (b) and trust it to be correct. We also call vslist::verify().
    DEBUG_ONLY(_context.verify();)

    const size_t commit_add = align_up(additional_word_size, Settings::commit_granule_words());
    if (_context.commit_limit() <= (commit_add + _context.vslist().committed_words())) {
      return true;
    }

    return false;

  }

  // Given a chunk level and a factor, return a random commit size.
  static size_t random_committed_words(chunklevel_t lvl, float commit_factor) {
    const size_t sz = word_size_for_level(lvl) * commit_factor;
    if (sz < 2) {
      return 0;
    }
    return MIN2(SizeRange(sz).random_value(), sz);
  }

  //// Chunk allocation ////

  // Given an slot index, allocate a random chunk and set it into that slot. Slot must be empty.
  // Returns false if allocation fails.
  bool allocate_random_chunk_at(int slot) {

    DEBUG_ONLY(_chunks.check_slot_is_null(slot);)

    const ChunkLevelRange r = _chunklevel_range.random_subrange();
    const chunklevel_t pref_level = r.lowest();
    const chunklevel_t max_level = r.highest();
    const size_t min_committed = random_committed_words(max_level, _commit_factor);

    Metachunk* c = NULL;
    _context.alloc_chunk(&c, r.lowest(), r.highest(), min_committed);
    if (c == NULL) {
      EXPECT_TRUE(could_be_reserve_error() ||
                  could_be_commit_error(min_committed));
      LOG("Alloc chunk at %d failed.", slot);
      return false;
    }

    _chunks.set_at(slot, c);

    LOG("Allocated chunk at %d: " METACHUNK_FORMAT ".", slot, METACHUNK_FORMAT_ARGS(c));

    return true;

  }

  // Allocates a random number of random chunks
  bool allocate_random_chunks() {
    int to_alloc = 1 + IntRange(MAX2(1, _chunks.size() / 8)).random_value();
    bool success = true;
    int slot = _chunks.first_null_slot();
    while (to_alloc > 0 && slot != -1 && success) {
      success = allocate_random_chunk_at(slot);
      slot = _chunks.next_null_slot(slot);
      to_alloc --;
    }
    return success && to_alloc == 0;
  }

  bool fill_all_slots_with_random_chunks() {
    bool success = true;
    for (int slot = _chunks.first_null_slot();
         slot != -1 && success; slot = _chunks.next_null_slot(slot)) {
      success = allocate_random_chunk_at(slot);
    }
    return success;
  }

  //// Chunk return ////

  // Given an slot index, return the chunk in that slot to the chunk manager.
  void return_chunk_at(int slot) {
    Metachunk* c = _chunks.at(slot);
    LOG("Returning chunk at %d: " METACHUNK_FORMAT ".", slot, METACHUNK_FORMAT_ARGS(c));
    _context.return_chunk(c);
    _chunks.set_at(slot, NULL);
  }

  // return a random number of chunks (at most a quarter of the full slot range)
  void return_random_chunks() {
    int to_free = 1 + IntRange(MAX2(1, _chunks.size() / 8)).random_value();
    int index = _chunks.first_non_null_slot();
    while (to_free > 0 && index != -1) {
      return_chunk_at(index);
      index = _chunks.next_non_null_slot(index);
      to_free --;
    }
  }

  void return_all_chunks() {
    for (int slot = _chunks.first_non_null_slot();
         slot != -1; slot = _chunks.next_non_null_slot(slot)) {
      return_chunk_at(slot);
    }
  }

  // adjust test if we change levels
  STATIC_ASSERT(HIGHEST_CHUNK_LEVEL == CHUNK_LEVEL_1K);
  STATIC_ASSERT(LOWEST_CHUNK_LEVEL == CHUNK_LEVEL_4M);

  void one_test() {

    fill_all_slots_with_random_chunks();
    _chunks.shuffle();

    IntRange rand(100);

    for (int j = 0; j < 1000; j++) {

      bool force_alloc = false;
      bool force_free = true;

      bool do_alloc =
          force_alloc ? true :
              (force_free ? false : rand.random_value() >= 50);
      force_alloc = force_free = false;

      if (do_alloc) {
        if (!allocate_random_chunks()) {
          force_free = true;
        }
      } else {
        return_random_chunks();
      }

      _chunks.shuffle();

    }

    return_all_chunks();

  }

public:

  // A test with no limits
  ChunkManagerRandomChunkAllocTest(ChunkLevelRange r, float commit_factor) :
    _context(),
    _chunks(max_num_live_chunks(r, commit_factor)),
    _chunklevel_range(r),
    _commit_factor(commit_factor)
  {}

  // A test with no reserve limit but commit limit
  ChunkManagerRandomChunkAllocTest(size_t commit_limit,
                                   ChunkLevelRange r, float commit_factor) :
    _context(commit_limit),
    _chunks(max_num_live_chunks(r, commit_factor)),
    _chunklevel_range(r),
    _commit_factor(commit_factor)
  {}

  // A test with both reserve and commit limit
  // ChunkManagerRandomChunkAllocTest(size_t commit_limit, size_t reserve_limit,
  //                                  ChunkLevelRange r, float commit_factor)
  // : _helper(commit_limit, reserve_limit),
  // _chunks(max_num_live_chunks(r, commit_factor)),
  // _chunklevel_range(r),
  // _commit_factor(commit_factor)
  // {}

  void do_tests() {
    const int num_runs = 5;
    for (int n = 0; n < num_runs; n++) {
      one_test();
    }
  }

};

#define DEFINE_TEST(name, range, commit_factor) \
TEST_VM(metaspace, chunkmanager_random_alloc_##name) { \
  ChunkManagerRandomChunkAllocTest test(range, commit_factor); \
  test.do_tests(); \
}

DEFINE_TEST(test_nolimit_1, ChunkLevelRanges::small_chunks(), 0.0f)
DEFINE_TEST(test_nolimit_2, ChunkLevelRanges::small_chunks(), 0.5f)
DEFINE_TEST(test_nolimit_3, ChunkLevelRanges::small_chunks(), 1.0f)

DEFINE_TEST(test_nolimit_4, ChunkLevelRanges::all_chunks(), 0.0f)
DEFINE_TEST(test_nolimit_5, ChunkLevelRanges::all_chunks(), 0.5f)
DEFINE_TEST(test_nolimit_6, ChunkLevelRanges::all_chunks(), 1.0f)

#define DEFINE_TEST_2(name, range, commit_factor) \
TEST_VM(metaspace, chunkmanager_random_alloc_##name) { \
  const size_t commit_limit = 256 * K; \
  ChunkManagerRandomChunkAllocTest test(commit_limit, range, commit_factor); \
  test.do_tests(); \
}

DEFINE_TEST_2(test_with_limit_1, ChunkLevelRanges::small_chunks(), 0.0f)
DEFINE_TEST_2(test_with_limit_2, ChunkLevelRanges::small_chunks(), 0.5f)
DEFINE_TEST_2(test_with_limit_3, ChunkLevelRanges::small_chunks(), 1.0f)

DEFINE_TEST_2(test_with_limit_4, ChunkLevelRanges::all_chunks(), 0.0f)
DEFINE_TEST_2(test_with_limit_5, ChunkLevelRanges::all_chunks(), 0.5f)
DEFINE_TEST_2(test_with_limit_6, ChunkLevelRanges::all_chunks(), 1.0f)

