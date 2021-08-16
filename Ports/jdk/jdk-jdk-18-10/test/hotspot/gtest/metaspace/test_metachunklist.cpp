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
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/metachunkList.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
//#define LOG_PLEASE
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestContexts.hpp"
#include "metaspaceGtestRangeHelpers.hpp"

using metaspace::FreeChunkList;
using metaspace::FreeChunkListVector;
using metaspace::MemRangeCounter;
using metaspace::MetachunkList;
using metaspace::Settings;

TEST_VM(metaspace, metachunklist) {

  ChunkGtestContext context;

  MetachunkList lst;

  Metachunk* chunks[10];
  size_t total_size = 0;

  for (int i = 0; i < 10; i++) {
    Metachunk* c = NULL;
    context.alloc_chunk_expect_success(&c, ChunkLevelRanges::all_chunks().random_value());
    chunks[i] = c;
    total_size += c->committed_words();

    lst.add(c);
    EXPECT_EQ(lst.first(), c);

    Metachunk* c2 = lst.remove_first();
    EXPECT_EQ(c, c2);

    EXPECT_EQ(lst.count(), i);
    lst.add(c);
    EXPECT_EQ(lst.count(), i + 1);
    EXPECT_EQ(lst.calc_committed_word_size(), total_size);

  }

  for (int i = 0; i < 10; i++) {
    DEBUG_ONLY(EXPECT_TRUE(lst.contains(chunks[i]));)
  }

  for (int i = 0; i < 10; i++) {
    Metachunk* c = lst.remove_first();
    DEBUG_ONLY(EXPECT_FALSE(lst.contains(c));)
    context.return_chunk(c);
  }

  EXPECT_EQ(lst.count(), 0);
  EXPECT_EQ(lst.calc_committed_word_size(), (size_t)0);

}

TEST_VM(metaspace, freechunklist) {

  ChunkGtestContext context;

  FreeChunkListVector lst;

  MemRangeCounter cnt;
  MemRangeCounter committed_cnt;

  // Add random chunks to list and check the counter apis (word_size, commited_word_size, num_chunks)
  // Make every other chunk randomly uncommitted, and later we check that committed chunks are sorted in at the front
  // of the lists.
  for (int i = 0; i < 100; i++) {
    Metachunk* c = NULL;
    context.alloc_chunk_expect_success(&c, ChunkLevelRanges::all_chunks().random_value());
    bool uncommitted_chunk = i % 3;
    if (uncommitted_chunk) {
      context.uncommit_chunk_with_test(c);
      c->set_in_use();
    }

    lst.add(c);

    LOG("->" METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c));

    cnt.add(c->word_size());
    committed_cnt.add(c->committed_words());

    EXPECT_EQ(lst.num_chunks(), (int)cnt.count());
    EXPECT_EQ(lst.word_size(), cnt.total_size());
    EXPECT_EQ(lst.calc_committed_word_size(), committed_cnt.total_size());
  }

  // Drain each list separately, front to back. While draining observe the order
  //  in which the chunks come: since uncommitted chunks are added to the tail of
  //  the list (see FreeChunkList::add_chunk()), no committed chunk should ever
  //  follow an uncommitted chunk.
  for (chunklevel_t lvl = LOWEST_CHUNK_LEVEL; lvl <= HIGHEST_CHUNK_LEVEL; lvl++) {
    Metachunk* c = lst.remove_first(lvl);
    bool found_uncommitted = false;
    while (c != NULL) {

      LOG("<-" METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c));

      if (found_uncommitted) {
        EXPECT_TRUE(c->is_fully_uncommitted());
      } else {
        found_uncommitted = c->is_fully_uncommitted();
      }

      cnt.sub(c->word_size());
      committed_cnt.sub(c->committed_words());

      EXPECT_EQ(lst.num_chunks(), (int)cnt.count());
      EXPECT_EQ(lst.word_size(), cnt.total_size());
      EXPECT_EQ(lst.calc_committed_word_size(), committed_cnt.total_size());

      context.return_chunk(c);

      c = lst.remove_first(lvl);
    }
  }

}

// Test, for a list populated with a mixture of fully/partially/uncommitted chunks,
// the retrieval-by-minimally-committed-words function.
TEST_VM(metaspace, freechunklist_retrieval) {

  if (Settings::new_chunks_are_fully_committed()) {
    return;
  }

  ChunkGtestContext context;
  FreeChunkList fcl;
  Metachunk* c = NULL;

  // For a chunk level which allows us to have partially committed chunks...
  const size_t chunk_word_size = Settings::commit_granule_words() * 4;
  const chunklevel_t lvl = level_fitting_word_size(chunk_word_size);

  // get some chunks:

  // ...a completely uncommitted one ...
  Metachunk* c_0 = NULL;
  context.alloc_chunk_expect_success(&c_0, lvl, lvl, 0);

  // ... a fully committed one ...
  Metachunk* c_full = NULL;
  context.alloc_chunk_expect_success(&c_full, lvl);

  // ... a chunk with one commit granule committed ...
  Metachunk* c_1g = NULL;
  context.alloc_chunk_expect_success(&c_1g, lvl, lvl, Settings::commit_granule_words());

  // ... a chunk with two commit granules committed.
  Metachunk* c_2g = NULL;
  context.alloc_chunk_expect_success(&c_2g, lvl, lvl, Settings::commit_granule_words() * 2);

  LOG("c_0: " METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c_0));
  LOG("c_full: " METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c_full));
  LOG("c_1g: " METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c_1g));
  LOG("c_2g: " METACHUNK_FULL_FORMAT, METACHUNK_FULL_FORMAT_ARGS(c_2g));


  // Simple check 1. Empty list should yield nothing.
  {
    c = fcl.first_minimally_committed(0);
    ASSERT_NULL(c);
  }

  // Simple check 2. Just a single uncommitted chunk.
  {
    fcl.add(c_0);
    c = fcl.first_minimally_committed(0);
    ASSERT_EQ(c_0, c);
    c = fcl.first_minimally_committed(1);
    ASSERT_NULL(c);
    fcl.remove(c_0);
  }

  // Now a check with a fully populated list.
  //  For different insert orders, try to retrieve different chunks by minimal commit level
  //  and check the result.
  for (int insert_order = 0; insert_order < 4; insert_order ++) {

    switch (insert_order) {
    case 0:
      fcl.add(c_0);
      fcl.add(c_full);
      fcl.add(c_1g);
      fcl.add(c_2g);
      break;
    case 1:
      fcl.add(c_1g);
      fcl.add(c_2g);
      fcl.add(c_0);
      fcl.add(c_full);
      break;
    case 2:
      fcl.add(c_2g);
      fcl.add(c_1g);
      fcl.add(c_full);
      fcl.add(c_0);
      break;
    case 3:
      fcl.add(c_full);
      fcl.add(c_2g);
      fcl.add(c_1g);
      fcl.add(c_0);
      break;
    }

    c = fcl.first_minimally_committed(0);
    ASSERT_TRUE(c == c_full || c == c_0 || c == c_1g || c == c_2g);

    c = fcl.first_minimally_committed(1);
    ASSERT_TRUE(c == c_full || c == c_1g || c == c_2g);

    c = fcl.first_minimally_committed(Settings::commit_granule_words());
    ASSERT_TRUE(c == c_full || c == c_1g || c == c_2g);

    c = fcl.first_minimally_committed(Settings::commit_granule_words() + 1);
    ASSERT_TRUE(c == c_full || c == c_2g);

    c = fcl.first_minimally_committed(Settings::commit_granule_words() * 2);
    ASSERT_TRUE(c == c_full || c == c_2g);

    c = fcl.first_minimally_committed((Settings::commit_granule_words() * 2) + 1);
    ASSERT_TRUE(c == c_full);

    c = fcl.first_minimally_committed(chunk_word_size);
    ASSERT_TRUE(c == c_full);

    c = fcl.first_minimally_committed(chunk_word_size + 1);
    ASSERT_NULL(c);

    fcl.remove(c_0);
    fcl.remove(c_full);
    fcl.remove(c_1g);
    fcl.remove(c_2g);

  }

  context.return_chunk(c_0);
  context.return_chunk(c_full);
  context.return_chunk(c_1g);
  context.return_chunk(c_2g);

}

