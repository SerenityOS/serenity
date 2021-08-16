/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021 SAP SE. All rights reserved.
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
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/virtualSpaceNode.hpp"
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"
#include "runtime/mutexLocker.hpp"

using metaspace::ChunkManager;
using metaspace::FreeChunkListVector;
using metaspace::Metachunk;
using metaspace::Settings;
using metaspace::VirtualSpaceNode;
using namespace metaspace::chunklevel;

// Test ChunkManager::get_chunk
TEST_VM(metaspace, get_chunk) {

  ChunkGtestContext context(8 * M);
  Metachunk* c = NULL;

  for (chunklevel_t pref_lvl = LOWEST_CHUNK_LEVEL; pref_lvl <= HIGHEST_CHUNK_LEVEL; pref_lvl++) {

    for (chunklevel_t max_lvl = pref_lvl; max_lvl <= HIGHEST_CHUNK_LEVEL; max_lvl++) {

      for (size_t min_committed_words = Settings::commit_granule_words();
           min_committed_words <= word_size_for_level(max_lvl); min_committed_words *= 2) {
        context.alloc_chunk_expect_success(&c, pref_lvl, max_lvl, min_committed_words);
        context.return_chunk(c);
      }
    }
  }
}

// Test ChunkManager::get_chunk, but with a commit limit.
TEST_VM(metaspace, get_chunk_with_commit_limit) {

  const size_t commit_limit_words = 1 * M;
  ChunkGtestContext context(commit_limit_words);
  Metachunk* c = NULL;

  for (chunklevel_t pref_lvl = LOWEST_CHUNK_LEVEL; pref_lvl <= HIGHEST_CHUNK_LEVEL; pref_lvl++) {

    for (chunklevel_t max_lvl = pref_lvl; max_lvl <= HIGHEST_CHUNK_LEVEL; max_lvl++) {

      for (size_t min_committed_words = Settings::commit_granule_words();
           min_committed_words <= word_size_for_level(max_lvl); min_committed_words *= 2) {

        if (min_committed_words <= commit_limit_words) {
          context.alloc_chunk_expect_success(&c, pref_lvl, max_lvl, min_committed_words);
          context.return_chunk(c);
        } else {
          context.alloc_chunk_expect_failure(pref_lvl, max_lvl, min_committed_words);
        }

      }
    }
  }
}

// Test that recommitting the used portion of a chunk will preserve the original content.
TEST_VM(metaspace, get_chunk_recommit) {

  ChunkGtestContext context;
  Metachunk* c = NULL;
  context.alloc_chunk_expect_success(&c, ROOT_CHUNK_LEVEL, ROOT_CHUNK_LEVEL, 0);
  context.uncommit_chunk_with_test(c);

  context.commit_chunk_with_test(c, Settings::commit_granule_words());
  context.allocate_from_chunk(c, Settings::commit_granule_words());

  c->ensure_committed(Settings::commit_granule_words());
  check_range_for_pattern(c->base(), c->used_words(), (uintx)c);

  c->ensure_committed(Settings::commit_granule_words() * 2);
  check_range_for_pattern(c->base(), c->used_words(), (uintx)c);

  context.return_chunk(c);

}

// Test ChunkManager::get_chunk, but with a reserve limit.
// (meaning, the underlying VirtualSpaceList cannot expand, like compressed class space).
TEST_VM(metaspace, get_chunk_with_reserve_limit) {

  const size_t reserve_limit_words = word_size_for_level(ROOT_CHUNK_LEVEL);
  const size_t commit_limit_words = 1024 * M; // just very high
  ChunkGtestContext context(commit_limit_words, reserve_limit_words);

  // Reserve limit works at root chunk size granularity: if the chunk manager cannot satisfy
  //  a request for a chunk from its freelists, it will acquire a new root chunk from the
  //  underlying virtual space list. If that list is full and cannot be expanded (think ccs)
  //  we should get an error.
  // Testing this is simply testing a chunk allocation which should cause allocation of a new
  //  root chunk.

  // Cause allocation of the firstone root chunk, should still work:
  Metachunk* c = NULL;
  context.alloc_chunk_expect_success(&c, HIGHEST_CHUNK_LEVEL);

  // and this should need a new root chunk and hence fail:
  context.alloc_chunk_expect_failure(ROOT_CHUNK_LEVEL);

  context.return_chunk(c);

}

// Test MetaChunk::allocate
TEST_VM(metaspace, chunk_allocate_full) {

  ChunkGtestContext context;

  for (chunklevel_t lvl = LOWEST_CHUNK_LEVEL; lvl <= HIGHEST_CHUNK_LEVEL; lvl++) {
    Metachunk* c = NULL;
    context.alloc_chunk_expect_success(&c, lvl);
    context.allocate_from_chunk(c, c->word_size());
    context.return_chunk(c);
  }

}

// Test MetaChunk::allocate
TEST_VM(metaspace, chunk_allocate_random) {

  ChunkGtestContext context;

  for (chunklevel_t lvl = LOWEST_CHUNK_LEVEL; lvl <= HIGHEST_CHUNK_LEVEL; lvl++) {

    Metachunk* c = NULL;
    context.alloc_chunk_expect_success(&c, lvl);
    context.uncommit_chunk_with_test(c); // start out fully uncommitted

    RandSizeGenerator rgen(1, c->word_size() / 30);
    bool stop = false;

    while (!stop) {
      const size_t s = rgen.get();
      if (s <= c->free_words()) {
        context.commit_chunk_with_test(c, s);
        context.allocate_from_chunk(c, s);
      } else {
        stop = true;
      }

    }
    context.return_chunk(c);

  }

}

TEST_VM(metaspace, chunk_buddy_stuff) {

  for (chunklevel_t l = ROOT_CHUNK_LEVEL + 1; l <= HIGHEST_CHUNK_LEVEL; l++) {

    ChunkGtestContext context;

    // Allocate two chunks; since we know the first chunk is the first in its area,
    // it has to be a leader, and the next one of the same size its buddy.

    // (Note: strictly speaking the ChunkManager does not promise any placement but
    //  we know how the placement works so these tests make sense).

    Metachunk* c1 = NULL;
    context.alloc_chunk(&c1, CHUNK_LEVEL_1K);
    EXPECT_TRUE(c1->is_leader());

    Metachunk* c2 = NULL;
    context.alloc_chunk(&c2, CHUNK_LEVEL_1K);
    EXPECT_FALSE(c2->is_leader());

    // buddies are adjacent in memory
    // (next/prev_in_vs needs lock)
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      EXPECT_EQ(c1->next_in_vs(), c2);
      EXPECT_EQ(c1->end(), c2->base());
      EXPECT_NULL(c1->prev_in_vs()); // since we know this is the first in the area
      EXPECT_EQ(c2->prev_in_vs(), c1);
    }

    context.return_chunk(c1);
    context.return_chunk(c2);

  }

}

TEST_VM(metaspace, chunk_allocate_with_commit_limit) {

  // This test does not make sense if commit-on-demand is off
  if (Settings::new_chunks_are_fully_committed()) {
    return;
  }

  const size_t granule_sz = Settings::commit_granule_words();
  const size_t commit_limit = granule_sz * 3;
  ChunkGtestContext context(commit_limit);

  // A big chunk, but uncommitted.
  Metachunk* c = NULL;
  context.alloc_chunk_expect_success(&c, ROOT_CHUNK_LEVEL, ROOT_CHUNK_LEVEL, 0);
  context.uncommit_chunk_with_test(c); // ... just to make sure.

  // first granule...
  context.commit_chunk_with_test(c, granule_sz);
  context.allocate_from_chunk(c, granule_sz);

  // second granule...
  context.commit_chunk_with_test(c, granule_sz);
  context.allocate_from_chunk(c, granule_sz);

  // third granule...
  context.commit_chunk_with_test(c, granule_sz);
  context.allocate_from_chunk(c, granule_sz);

  // This should fail now.
  context.commit_chunk_expect_failure(c, granule_sz);

  context.return_chunk(c);

}

// Test splitting a chunk
TEST_VM(metaspace, chunk_split_and_merge) {

  // Split works like this:
  //
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  // |                                  A                                            |
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  //
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  // | A' | b  |    c    |         d         |                   e                   |
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  //
  // A original chunk (A) is split to form a target chunk (A') and as a result splinter
  // chunks form (b..e). A' is the leader of the (A',b) pair, which is the leader of the
  // ((A',b), c) pair and so on. In other words, A' will be a leader chunk, all splinter
  // chunks are follower chunks.
  //
  // Merging reverses this operation:
  //
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  // | A  | b  |    c    |         d         |                   e                   |
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  //
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  // |                                  A'                                           |
  //  ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ----
  //
  // (A) will be merged with its buddy b, (A+b) with its buddy c and so on. The result
  // chunk is A'.
  // Note that merging also works, of course, if we were to start the merge at (b) (so,
  // with a follower chunk, not a leader). Also, at any point in the merge
  // process we may arrive at a follower chunk. So, the fact that in this test
  // we only expect a leader merge is a feature of the test, and of the fact that we
  // start each split test with a fresh ChunkTestsContext.

  // Note: Splitting and merging chunks is usually done from within the ChunkManager and
  //  subject to a lot of assumptions and hence asserts. Here, we have to explicitly use
  //  VirtualSpaceNode::split/::merge and therefore have to observe rules:
  // - both split and merge expect free chunks, so state has to be "free"
  // - but that would trigger the "ideally merged" assertion in the RootChunkArea, so the
  //   original chunk has to be a root chunk, we cannot just split any chunk manually.
  // - Also, after the split we have to completely re-merge to avoid triggering asserts
  //   in ~RootChunkArea()
  // - finally we have to lock manually

  ChunkGtestContext context;

  const chunklevel_t orig_lvl = ROOT_CHUNK_LEVEL;
  for (chunklevel_t target_lvl = orig_lvl + 1; target_lvl <= HIGHEST_CHUNK_LEVEL; target_lvl++) {

    // Split a fully committed chunk. The resulting chunk should be fully
    //  committed as well, and have its content preserved.
    Metachunk* c = NULL;
    context.alloc_chunk_expect_success(&c, orig_lvl);

    // We allocate from this chunk to be able to completely paint the payload.
    context.allocate_from_chunk(c, c->word_size());

    const uintx canary = os::random();
    fill_range_with_pattern(c->base(), c->word_size(), canary);

    FreeChunkListVector splinters;

    {
      // Splitting/Merging chunks is usually done by the chunkmanager, and no explicit
      // outside API exists. So we split/merge chunks via the underlying vs node, directly.
      // This means that we have to go through some extra hoops to not trigger any asserts.
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      c->reset_used_words();
      c->set_free();
      c->vsnode()->split(target_lvl, c, &splinters);
    }

    DEBUG_ONLY(context.verify();)

    EXPECT_EQ(c->level(), target_lvl);
    EXPECT_TRUE(c->is_fully_committed());
    EXPECT_FALSE(c->is_root_chunk());
    EXPECT_TRUE(c->is_leader());

    check_range_for_pattern(c->base(), c->word_size(), canary);

    // I expect splinter chunks (one for each splinter level:
    //  e.g. splitting a 1M chunk to get a 64K chunk should yield splinters: [512K, 256K, 128K, 64K]
    for (chunklevel_t l = LOWEST_CHUNK_LEVEL; l < HIGHEST_CHUNK_LEVEL; l++) {
      const Metachunk* c2 = splinters.first_at_level(l);
      if (l > orig_lvl && l <= target_lvl) {
        EXPECT_NOT_NULL(c2);
        EXPECT_EQ(c2->level(), l);
        EXPECT_TRUE(c2->is_free());
        EXPECT_TRUE(!c2->is_leader());
        DEBUG_ONLY(c2->verify());
        check_range_for_pattern(c2->base(), c2->word_size(), canary);
      } else {
        EXPECT_NULL(c2);
      }
    }

    // Revert the split by using merge. This should result in all splinters coalescing
    // to one chunk.
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      Metachunk* merged = c->vsnode()->merge(c, &splinters);

      // the merged chunk should occupy the same address as the splinter
      // since it should have been the leader in the split.
      EXPECT_EQ(merged, c);
      EXPECT_TRUE(merged->is_root_chunk() || merged->is_leader());

      // Splitting should have arrived at the original chunk since none of the splinters are in use.
      EXPECT_EQ(c->level(), orig_lvl);

      // All splinters should have been removed from the list
      EXPECT_EQ(splinters.num_chunks(), 0);
    }

    context.return_chunk(c);

  }

}

TEST_VM(metaspace, chunk_enlarge_in_place) {

  ChunkGtestContext context;

  // Starting with the smallest chunk size, attempt to enlarge the chunk in place until we arrive
  // at root chunk size. Since the state is clean, this should work.

  Metachunk* c = NULL;
  context.alloc_chunk_expect_success(&c, HIGHEST_CHUNK_LEVEL);

  chunklevel_t l = c->level();

  while (l != ROOT_CHUNK_LEVEL) {

    // commit and allocate from chunk to pattern it...
    const size_t original_chunk_size = c->word_size();
    context.commit_chunk_with_test(c, c->free_words());
    context.allocate_from_chunk(c, c->free_words());

    size_t used_before = c->used_words();
    size_t free_before = c->free_words();
    size_t free_below_committed_before = c->free_below_committed_words();
    const MetaWord* top_before = c->top();

    EXPECT_TRUE(context.cm().attempt_enlarge_chunk(c));
    EXPECT_EQ(l - 1, c->level());
    EXPECT_EQ(c->word_size(), original_chunk_size * 2);

    // Used words should not have changed
    EXPECT_EQ(c->used_words(), used_before);
    EXPECT_EQ(c->top(), top_before);

    // free words should be expanded by the old size (since old chunk is doubled in size)
    EXPECT_EQ(c->free_words(), free_before + original_chunk_size);

    // free below committed can be larger but never smaller
    EXPECT_GE(c->free_below_committed_words(), free_below_committed_before);

    // Old content should be preserved
    check_range_for_pattern(c->base(), original_chunk_size, (uintx)c);

    l = c->level();
  }

  context.return_chunk(c);

}

