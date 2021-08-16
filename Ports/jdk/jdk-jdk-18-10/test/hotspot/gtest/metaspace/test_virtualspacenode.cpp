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
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metachunkList.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/virtualSpaceNode.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestRangeHelpers.hpp"

using metaspace::chunklevel_t;
using metaspace::CommitLimiter;
using metaspace::FreeChunkListVector;
using metaspace::Metachunk;
using metaspace::MetachunkList;
using metaspace::VirtualSpaceNode;
using metaspace::Settings;
using metaspace::SizeCounter;

class VirtualSpaceNodeTest {

  // These counters are updated by the Node.
  SizeCounter _counter_reserved_words;
  SizeCounter _counter_committed_words;
  CommitLimiter _commit_limiter;
  VirtualSpaceNode* _node;

  // These are my checks and counters.
  const size_t _vs_word_size;
  const size_t _commit_limit;

  MetachunkList _root_chunks;

  void verify() const {

    ASSERT_EQ(_root_chunks.count() * metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
              _node->used_words());

    ASSERT_GE(_commit_limit,                      _counter_committed_words.get());
    ASSERT_EQ(_commit_limiter.committed_words(),  _counter_committed_words.get());

    // Since we know _counter_committed_words serves our single node alone, the counter has to
    // match the number of bits in the node internal commit mask.
    ASSERT_EQ(_counter_committed_words.get(), _node->committed_words());

    ASSERT_EQ(_counter_reserved_words.get(), _vs_word_size);
    ASSERT_EQ(_counter_reserved_words.get(), _node->word_size());

  }

  void lock_and_verify_node() {
#ifdef ASSERT
    MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
    _node->verify_locked();
#endif
  }

  Metachunk* alloc_root_chunk() {

    verify();

    const bool node_is_full = _node->used_words() == _node->word_size();
    Metachunk* c = NULL;
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      c = _node->allocate_root_chunk();
    }

    lock_and_verify_node();

    if (node_is_full) {

      EXPECT_NULL(c);

    } else {

      DEBUG_ONLY(c->verify();)
      EXPECT_NOT_NULL(c);
      EXPECT_TRUE(c->is_root_chunk());
      EXPECT_TRUE(c->is_free());
      EXPECT_EQ(c->word_size(), metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);

      EXPECT_TRUE(c->is_fully_uncommitted());

      EXPECT_TRUE(_node->contains(c->base()));

      _root_chunks.add(c);

    }

    verify();

    return c;

  }

  bool commit_root_chunk(Metachunk* c, size_t request_commit_words) {

    verify();

    const size_t committed_words_before = _counter_committed_words.get();

    bool rc = c->ensure_committed(request_commit_words);

    verify();
    DEBUG_ONLY(c->verify();)

    lock_and_verify_node();

    if (rc == false) {

      // We must have hit the commit limit.
      EXPECT_GE(committed_words_before + request_commit_words, _commit_limit);

    } else {

      // We should not have hit the commit limit.
      EXPECT_LE(_counter_committed_words.get(), _commit_limit);

      // We do not know how much we really committed - maybe nothing if the
      // chunk had been committed before - but we know the numbers should have
      // risen or at least stayed equal.
      EXPECT_GE(_counter_committed_words.get(), committed_words_before);

      // The chunk should be as far committed as was requested
      EXPECT_GE(c->committed_words(), request_commit_words);

      // Zap committed portion.
      DEBUG_ONLY(zap_range(c->base(), c->committed_words());)

    }

    verify();

    return rc;

  } // commit_root_chunk

  void uncommit_chunk(Metachunk* c) {

    verify();

    const size_t committed_words_before = _counter_committed_words.get();
    const size_t available_words_before = _commit_limiter.possible_expansion_words();

    c->uncommit();

    DEBUG_ONLY(c->verify();)

    lock_and_verify_node();

    EXPECT_EQ(c->committed_words(), (size_t)0);

    // Commit counter should have gone down (by exactly the size of the chunk) if chunk
    // is larger than a commit granule.
    // For smaller chunks, we do not know, but at least we know the commit size should not
    // have gone up.
    if (c->word_size() >= Settings::commit_granule_words()) {

      EXPECT_EQ(_counter_committed_words.get(), committed_words_before - c->word_size());

      // also, commit number in commit limiter should have gone down, so we have more space
      EXPECT_EQ(_commit_limiter.possible_expansion_words(),
                available_words_before + c->word_size());

    } else {

      EXPECT_LE(_counter_committed_words.get(), committed_words_before);

    }

    verify();

  } // uncommit_chunk

  Metachunk* split_chunk_with_checks(Metachunk* c, chunklevel_t target_level, FreeChunkListVector* freelist) {

    DEBUG_ONLY(c->verify();)

    const chunklevel_t orig_level = c->level();
    assert(orig_level < target_level, "Sanity");
    DEBUG_ONLY(metaspace::chunklevel::check_valid_level(target_level);)

    const int total_num_chunks_in_freelist_before = freelist->num_chunks();
    const size_t total_word_size_in_freelist_before = freelist->word_size();

   // freelist->print_on(tty);

    // Split...
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      _node->split(target_level, c, freelist);
    }

    // freelist->print_on(tty);

    EXPECT_NOT_NULL(c);
    EXPECT_EQ(c->level(), target_level);
    EXPECT_TRUE(c->is_free());

    // ... check that we get the proper amount of splinters. For every chunk split we expect one
    // buddy chunk to appear of level + 1 (aka, half size).
    size_t expected_wordsize_increase = 0;
    int expected_num_chunks_increase = 0;
    for (chunklevel_t l = orig_level + 1; l <= target_level; l++) {
      expected_wordsize_increase += metaspace::chunklevel::word_size_for_level(l);
      expected_num_chunks_increase++;
    }

    const int total_num_chunks_in_freelist_after = freelist->num_chunks();
    const size_t total_word_size_in_freelist_after = freelist->word_size();

    EXPECT_EQ(total_num_chunks_in_freelist_after, total_num_chunks_in_freelist_before + expected_num_chunks_increase);
    EXPECT_EQ(total_word_size_in_freelist_after, total_word_size_in_freelist_before + expected_wordsize_increase);

    return c;

  } // end: split_chunk_with_checks

  Metachunk* merge_chunk_with_checks(Metachunk* c, chunklevel_t expected_target_level, FreeChunkListVector* freelist) {

    const chunklevel_t orig_level = c->level();
    assert(expected_target_level < orig_level, "Sanity");

    const int total_num_chunks_in_freelist_before = freelist->num_chunks();
    const size_t total_word_size_in_freelist_before = freelist->word_size();

    //freelist->print_on(tty);

    Metachunk* result = NULL;
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      result = _node->merge(c, freelist);
    }
    EXPECT_NOT_NULL(result);
    EXPECT_TRUE(result->level() == expected_target_level);

    //freelist->print_on(tty);

    // ... check that we merged in the proper amount of chunks. For every decreased level
    // of the original chunk (each size doubling) we should see one buddy chunk swallowed up.
    size_t expected_wordsize_decrease = 0;
    int expected_num_chunks_decrease = 0;
    for (chunklevel_t l = orig_level; l > expected_target_level; l --) {
      expected_wordsize_decrease += metaspace::chunklevel::word_size_for_level(l);
      expected_num_chunks_decrease++;
    }

    const int total_num_chunks_in_freelist_after = freelist->num_chunks();
    const size_t total_word_size_in_freelist_after = freelist->word_size();

    EXPECT_EQ(total_num_chunks_in_freelist_after, total_num_chunks_in_freelist_before - expected_num_chunks_decrease);
    EXPECT_EQ(total_word_size_in_freelist_after, total_word_size_in_freelist_before - expected_wordsize_decrease);

    return result;

  } // end: merge_chunk_with_checks

public:

  VirtualSpaceNodeTest(size_t vs_word_size, size_t commit_limit) :
    _counter_reserved_words(),
    _counter_committed_words(),
    _commit_limiter(commit_limit),
    _node(NULL),
    _vs_word_size(vs_word_size),
    _commit_limit(commit_limit)
  {
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      _node = VirtualSpaceNode::create_node(vs_word_size, &_commit_limiter,
                                            &_counter_reserved_words, &_counter_committed_words);
      EXPECT_EQ(_node->word_size(), vs_word_size);
    }
    EXPECT_TRUE(_commit_limiter.possible_expansion_words() == _commit_limit);
    verify();
  }

  ~VirtualSpaceNodeTest() {
    {
      MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
      delete _node;
    }
    // After the node is deleted, counters should be back to zero
    // (we cannot use ASSERT/EXPECT here in the destructor)
    assert(_counter_reserved_words.get() == 0, "Sanity");
    assert(_counter_committed_words.get() == 0, "Sanity");
    assert(_commit_limiter.committed_words() == 0, "Sanity");
  }

  void test_simple() {
    Metachunk* c = alloc_root_chunk();
    commit_root_chunk(c, Settings::commit_granule_words());
    commit_root_chunk(c, c->word_size());
    uncommit_chunk(c);
  }

  void test_exhaust_node() {
    Metachunk* c = NULL;
    bool rc = true;
    do {
      c = alloc_root_chunk();
      if (c != NULL) {
        rc = commit_root_chunk(c, c->word_size());
      }
    } while (c != NULL && rc);
  }

  void test_arbitrary_commits() {

    assert(_commit_limit >= _vs_word_size, "For this test no commit limit.");

    // Get a root chunk to have a committable region
    Metachunk* c = alloc_root_chunk();
    ASSERT_NOT_NULL(c);

    if (c->committed_words() > 0) {
      c->uncommit();
    }

    ASSERT_EQ(_node->committed_words(), (size_t)0);
    ASSERT_EQ(_counter_committed_words.get(), (size_t)0);

    TestMap testmap(c->word_size());
    assert(testmap.get_num_set() == 0, "Sanity");

    for (int run = 0; run < 1000; run++) {

      const size_t committed_words_before = testmap.get_num_set();
      ASSERT_EQ(_commit_limiter.committed_words(), committed_words_before);
      ASSERT_EQ(_counter_committed_words.get(), committed_words_before);

      // A random range
      SizeRange r = SizeRange(c->word_size()).random_aligned_subrange(Settings::commit_granule_words());

      const size_t committed_words_in_range_before =
                   testmap.get_num_set(r.start(), r.end());

      const bool do_commit = IntRange(100).random_value() >= 50;
      if (do_commit) {

        //LOG("c " SIZE_FORMAT "," SIZE_FORMAT, r.start(), r.end());

        bool rc = false;
        {
          MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
          rc = _node->ensure_range_is_committed(c->base() + r.start(), r.size());
        }

        // Test-zap
        zap_range(c->base() + r.start(), r.size());

        // We should never reach commit limit since it is as large as the whole area.
        ASSERT_TRUE(rc);

        testmap.set_range(r.start(), r.end());

      } else {

        //LOG("u " SIZE_FORMAT "," SIZE_FORMAT, r.start(), r.end());

        {
          MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
          _node->uncommit_range(c->base() + r.start(), r.size());
        }

        testmap.clear_range(r.start(), r.end());

      }

      const size_t committed_words_after = testmap.get_num_set();

      ASSERT_EQ(_commit_limiter.committed_words(), committed_words_after);
      ASSERT_EQ(_counter_committed_words.get(), committed_words_after);

      verify();
    }
  }

  // Helper function for test_splitting_chunks_1
  static void check_chunk_is_committed_at_least_up_to(const Metachunk* c, size_t& word_size) {
    if (word_size >= c->word_size()) {
      EXPECT_TRUE(c->is_fully_committed());
      word_size -= c->word_size();
    } else {
      EXPECT_EQ(c->committed_words(), word_size);
      word_size = 0; // clear remaining size if there is.
    }
  }

  void test_split_and_merge_chunks() {

    assert(_commit_limit >= _vs_word_size, "No commit limit here pls");

    // Allocate a root chunk and commit a random part of it. Then repeatedly split
    // it and merge it back together; observe the committed regions of the split chunks.

    Metachunk* c = alloc_root_chunk();

    if (c->committed_words() > 0) {
      c->uncommit();
    }

    // To capture split-off chunks. Note: it is okay to use this here as a temp object.
    FreeChunkListVector freelist;

    const int granules_per_root_chunk = (int)(c->word_size() / Settings::commit_granule_words());

    for (int granules_to_commit = 0; granules_to_commit < granules_per_root_chunk; granules_to_commit++) {

      const size_t words_to_commit = Settings::commit_granule_words() * granules_to_commit;

      c->ensure_committed(words_to_commit);

      ASSERT_EQ(c->committed_words(), words_to_commit);
      ASSERT_EQ(_counter_committed_words.get(), words_to_commit);
      ASSERT_EQ(_commit_limiter.committed_words(), words_to_commit);

      const size_t committed_words_before = c->committed_words();

      verify();

      for (chunklevel_t target_level = LOWEST_CHUNK_LEVEL + 1;
           target_level <= HIGHEST_CHUNK_LEVEL; target_level++) {

        // Split:
        Metachunk* c2 = split_chunk_with_checks(c, target_level, &freelist);
        c2->set_in_use();

        // Split smallest leftover chunk.
        if (c2->level() < HIGHEST_CHUNK_LEVEL) {

          Metachunk* c3 = freelist.remove_first(c2->level());
          ASSERT_NOT_NULL(c3); // Must exist since c2 must have a splinter buddy now.

          Metachunk* c4 = split_chunk_with_checks(c3, HIGHEST_CHUNK_LEVEL, &freelist);
          c4->set_in_use();

          // Merge it back. We expect this to merge up to c2's level, since c2 is in use.
          c4->set_free();
          Metachunk* c5 = merge_chunk_with_checks(c4, c2->level(), &freelist);
          ASSERT_NOT_NULL(c5);
          freelist.add(c5);

        }

        // Merge c2 back.
        c2->set_free();
        merge_chunk_with_checks(c2, LOWEST_CHUNK_LEVEL, &freelist);

        // After all this splitting and combining committed size should not have changed.
        ASSERT_EQ(c2->committed_words(), committed_words_before);

      }

    }

  } // end: test_splitting_chunks

};

TEST_VM(metaspace, virtual_space_node_test_basics) {

  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);

  const size_t word_size = metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 10;

  SizeCounter scomm;
  SizeCounter sres;
  CommitLimiter cl (word_size * 2); // basically, no commit limiter.

  VirtualSpaceNode* node = VirtualSpaceNode::create_node(word_size, &cl, &sres, &scomm);
  ASSERT_NOT_NULL(node);
  ASSERT_EQ(node->committed_words(), (size_t)0);
  ASSERT_EQ(node->committed_words(), scomm.get());
  DEBUG_ONLY(node->verify_locked();)

  bool b = node->ensure_range_is_committed(node->base(), node->word_size());
  ASSERT_TRUE(b);
  ASSERT_EQ(node->committed_words(), word_size);
  ASSERT_EQ(node->committed_words(), scomm.get());
  DEBUG_ONLY(node->verify_locked();)
  zap_range(node->base(), node->word_size());

  node->uncommit_range(node->base(), node->word_size());
  ASSERT_EQ(node->committed_words(), (size_t)0);
  ASSERT_EQ(node->committed_words(), scomm.get());
  DEBUG_ONLY(node->verify_locked();)

  const int num_granules = (int)(word_size / Settings::commit_granule_words());
  for (int i = 1; i < num_granules; i += 4) {
    b = node->ensure_range_is_committed(node->base(), i * Settings::commit_granule_words());
    ASSERT_TRUE(b);
    ASSERT_EQ(node->committed_words(), i * Settings::commit_granule_words());
    ASSERT_EQ(node->committed_words(), scomm.get());
    DEBUG_ONLY(node->verify_locked();)
    zap_range(node->base(), i * Settings::commit_granule_words());
  }

  node->uncommit_range(node->base(), node->word_size());
  ASSERT_EQ(node->committed_words(), (size_t)0);
  ASSERT_EQ(node->committed_words(), scomm.get());
  DEBUG_ONLY(node->verify_locked();)

}

// Note: we unfortunately need TEST_VM even though the system tested
// should be pretty independent since we need things like os::vm_page_size()
// which in turn need OS layer initialization.
TEST_VM(metaspace, virtual_space_node_test_1) {
  VirtualSpaceNodeTest test(metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
      metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);
  test.test_simple();
}

TEST_VM(metaspace, virtual_space_node_test_2) {
  // Should not hit commit limit
  VirtualSpaceNodeTest test(3 * metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
      3 * metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);
  test.test_simple();
  test.test_exhaust_node();
}

TEST_VM(metaspace, virtual_space_node_test_3) {
  double d = os::elapsedTime();
  // Test committing uncommitting arbitrary ranges
  for (int run = 0; run < 100; run++) {
    VirtualSpaceNodeTest test(metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
        metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);
    test.test_split_and_merge_chunks();
  }
  double d2 = os::elapsedTime();
  LOG("%f", (d2-d));
}

TEST_VM(metaspace, virtual_space_node_test_4) {
  // Should hit commit limit
  VirtualSpaceNodeTest test(10 * metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
      3 * metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);
  test.test_exhaust_node();
}

TEST_VM(metaspace, virtual_space_node_test_5) {
  // Test committing uncommitting arbitrary ranges
  VirtualSpaceNodeTest test(metaspace::chunklevel::MAX_CHUNK_WORD_SIZE,
      metaspace::chunklevel::MAX_CHUNK_WORD_SIZE);
  test.test_arbitrary_commits();
}

TEST_VM(metaspace, virtual_space_node_test_7) {
  // Test large allocation and freeing.
  {
    VirtualSpaceNodeTest test(metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 100,
        metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 100);
    test.test_exhaust_node();
  }
  {
    VirtualSpaceNodeTest test(metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 100,
        metaspace::chunklevel::MAX_CHUNK_WORD_SIZE * 100);
    test.test_exhaust_node();
  }

}
