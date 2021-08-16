/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2021 SAP SE. All rights reserved.
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
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceContext.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
#include "memory/metaspace/virtualSpaceNode.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

#define LOGFMT         "ChkMgr @" PTR_FORMAT " (%s)"
#define LOGFMT_ARGS    p2i(this), this->_name

// Return a single chunk to the freelist and adjust accounting. No merge is attempted.
void ChunkManager::return_chunk_simple_locked(Metachunk* c) {
  assert_lock_strong(Metaspace_lock);
  DEBUG_ONLY(c->verify());
  _chunks.add(c);
  c->reset_used_words();
  // Tracing
  log_debug(metaspace)("ChunkManager %s: returned chunk " METACHUNK_FORMAT ".",
                       _name, METACHUNK_FORMAT_ARGS(c));
}

// Creates a chunk manager with a given name (which is for debug purposes only)
// and an associated space list which will be used to request new chunks from
// (see get_chunk())
ChunkManager::ChunkManager(const char* name, VirtualSpaceList* space_list) :
  _vslist(space_list),
  _name(name),
  _chunks()
{
}

// Given a chunk, split it into a target chunk of a smaller size (higher target level)
//  and at least one, possible several splinter chunks.
// The original chunk must be outside of the freelist and its state must be free.
// The splinter chunks are added to the freelist.
// The resulting target chunk will be located at the same address as the original
//  chunk, but it will of course be smaller (of a higher level).
// The committed areas within the original chunk carry over to the resulting
//  chunks.
void ChunkManager::split_chunk_and_add_splinters(Metachunk* c, chunklevel_t target_level) {
  assert_lock_strong(Metaspace_lock);
  assert(c->is_free(), "chunk to be split must be free.");
  assert(c->level() < target_level, "Target level must be higher than current level.");
  assert(c->prev() == NULL && c->next() == NULL, "Chunk must be outside of any list.");

  DEBUG_ONLY(chunklevel::check_valid_level(target_level);)
  DEBUG_ONLY(c->verify();)

  UL2(debug, "splitting chunk " METACHUNK_FORMAT " to " CHKLVL_FORMAT ".",
      METACHUNK_FORMAT_ARGS(c), target_level);

  DEBUG_ONLY(size_t committed_words_before = c->committed_words();)

  const chunklevel_t orig_level = c->level();
  c->vsnode()->split(target_level, c, &_chunks);

  // Splitting should never fail.
  assert(c->level() == target_level, "Sanity");

  // The size of the committed portion should not change (subject to the reduced chunk size of course)
#ifdef ASSERT
  if (committed_words_before > c->word_size()) {
    assert(c->is_fully_committed(), "Sanity");
  } else {
    assert(c->committed_words() == committed_words_before, "Sanity");
  }
  c->verify();
  verify_locked();
  SOMETIMES(c->vsnode()->verify_locked();)
#endif
  InternalStats::inc_num_chunk_splits();
}

// On success, returns a chunk of level of <preferred_level>, but at most <max_level>.
//  The first first <min_committed_words> of the chunk are guaranteed to be committed.
// On error, will return NULL.
//
// This function may fail for two reasons:
// - Either we are unable to reserve space for a new chunk (if the underlying VirtualSpaceList
//   is non-expandable but needs expanding - aka out of compressed class space).
// - Or, if the necessary space cannot be committed because we hit a commit limit.
//   This may be either the GC threshold or MaxMetaspaceSize.
Metachunk* ChunkManager::get_chunk(chunklevel_t preferred_level, chunklevel_t max_level, size_t min_committed_words) {
  assert(preferred_level <= max_level, "Sanity");
  assert(chunklevel::level_fitting_word_size(min_committed_words) >= max_level, "Sanity");

  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);

  DEBUG_ONLY(verify_locked();)
  DEBUG_ONLY(chunklevel::check_valid_level(max_level);)
  DEBUG_ONLY(chunklevel::check_valid_level(preferred_level);)

  UL2(debug, "requested chunk: pref_level: " CHKLVL_FORMAT
     ", max_level: " CHKLVL_FORMAT ", min committed size: " SIZE_FORMAT ".",
     preferred_level, max_level, min_committed_words);

  // First, optimistically look for a chunk which is already committed far enough to hold min_word_size.

  // 1) Search best or smaller committed chunks (first attempt):
  //    Start at the preferred chunk size and work your way down (level up).
  //    But for now, only consider chunks larger than a certain threshold -
  //    this is to prevent large loaders (eg boot) from unnecessarily gobbling up
  //    all the tiny splinter chunks lambdas leave around.
  Metachunk* c = NULL;
  c = _chunks.search_chunk_ascending(preferred_level, MIN2((chunklevel_t)(preferred_level + 2), max_level), min_committed_words);

  // 2) Search larger committed chunks:
  //    If that did not yield anything, look at larger chunks, which may be committed. We would have to split
  //    them first, of course.
  if (c == NULL) {
    c = _chunks.search_chunk_descending(preferred_level, min_committed_words);
  }
  // 3) Search best or smaller committed chunks (second attempt):
  //    Repeat (1) but now consider even the tiniest chunks as long as they are large enough to hold the
  //    committed min size.
  if (c == NULL) {
    c = _chunks.search_chunk_ascending(preferred_level, max_level, min_committed_words);
  }
  // if we did not get anything yet, there are no free chunks commmitted enough. Repeat search but look for uncommitted chunks too:
  // 4) Search best or smaller chunks, can be uncommitted:
  if (c == NULL) {
    c = _chunks.search_chunk_ascending(preferred_level, max_level, 0);
  }
  // 5) Search a larger uncommitted chunk:
  if (c == NULL) {
    c = _chunks.search_chunk_descending(preferred_level, 0);
  }

  if (c != NULL) {
    UL(trace, "taken from freelist.");
  }

  // Failing all that, allocate a new root chunk from the connected virtual space.
  // This may fail if the underlying vslist cannot be expanded (e.g. compressed class space)
  if (c == NULL) {
    c = _vslist->allocate_root_chunk();
    if (c == NULL) {
      UL(info, "failed to get new root chunk.");
    } else {
      assert(c->level() == chunklevel::ROOT_CHUNK_LEVEL, "root chunk expected");
      UL(debug, "allocated new root chunk.");
    }
  }
  if (c == NULL) {
    // If we end up here, we found no match in the freelists and were unable to get a new
    // root chunk (so we used up all address space, e.g. out of CompressedClassSpace).
    UL2(info, "failed to get chunk (preferred level: " CHKLVL_FORMAT
       ", max level " CHKLVL_FORMAT ".", preferred_level, max_level);
    c = NULL;
  }
  if (c != NULL) {
    // Now we have a chunk.
    //  It may be larger than what the caller wanted, so we may want to split it. This should
    //  always work.
    if (c->level() < preferred_level) {
      split_chunk_and_add_splinters(c, preferred_level);
      assert(c->level() == preferred_level, "split failed?");
    }
    // Attempt to commit the chunk (depending on settings, we either fully commit it or just
    //  commit enough to get the caller going). That may fail if we hit a commit limit. In
    //  that case put the chunk back to the freelist (re-merging it with its neighbors if we
    //  did split it) and return NULL.
    const size_t to_commit = Settings::new_chunks_are_fully_committed() ? c->word_size() : min_committed_words;
    if (c->committed_words() < to_commit) {
      if (c->ensure_committed_locked(to_commit) == false) {
        UL2(info, "failed to commit " SIZE_FORMAT " words on chunk " METACHUNK_FORMAT ".",
            to_commit,  METACHUNK_FORMAT_ARGS(c));
        return_chunk_locked(c);
        c = NULL;
      }
    }
    if (c != NULL) {
      // Still here? We have now a good chunk, all is well.
      assert(c->committed_words() >= min_committed_words, "Sanity");

      // Any chunk returned from ChunkManager shall be marked as in use.
      c->set_in_use();

      UL2(debug, "handing out chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));

      InternalStats::inc_num_chunks_taken_from_freelist();

      SOMETIMES(c->vsnode()->verify_locked();)
    }
  }

  DEBUG_ONLY(verify_locked();)
  return c;
}

// Return a single chunk to the ChunkManager and adjust accounting. May merge chunk
//  with neighbors.
// As a side effect this removes the chunk from whatever list it has been in previously.
// Happens after a Classloader was unloaded and releases its metaspace chunks.
// !! Note: this may invalidate the chunk. Do not access the chunk after
//    this function returns !!
void ChunkManager::return_chunk(Metachunk* c) {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  return_chunk_locked(c);
}

// See return_chunk().
void ChunkManager::return_chunk_locked(Metachunk* c) {
  assert_lock_strong(Metaspace_lock);
  UL2(debug, ": returning chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));
  DEBUG_ONLY(c->verify();)
  assert(contains_chunk(c) == false, "A chunk to be added to the freelist must not be in the freelist already.");
  assert(c->is_in_use() || c->is_free(), "Unexpected chunk state");
  assert(!c->in_list(), "Remove from list first");

  c->set_free();
  c->reset_used_words();
  const chunklevel_t orig_lvl = c->level();

  Metachunk* merged = NULL;
  if (!c->is_root_chunk()) {
    // Only attempt merging if we are not of the lowest level already.
    merged = c->vsnode()->merge(c, &_chunks);
  }

  if (merged != NULL) {
    InternalStats::inc_num_chunk_merges();
    DEBUG_ONLY(merged->verify());
    // We did merge chunks and now have a bigger chunk.
    assert(merged->level() < orig_lvl, "Sanity");
    UL2(debug, "merged into chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(merged));
    c = merged;
  }

  if (Settings::uncommit_free_chunks() &&
      c->word_size() >= Settings::commit_granule_words()) {
    UL2(debug, "uncommitting free chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));
    c->uncommit_locked();
  }

  return_chunk_simple_locked(c);
  DEBUG_ONLY(verify_locked();)
  SOMETIMES(c->vsnode()->verify_locked();)
  InternalStats::inc_num_chunks_returned_to_freelist();
}

// Given a chunk c, whose state must be "in-use" and must not be a root chunk, attempt to
// enlarge it in place by claiming its trailing buddy.
//
// This will only work if c is the leader of the buddy pair and the trailing buddy is free.
//
// If successful, the follower chunk will be removed from the freelists, the leader chunk c will
// double in size (level decreased by one).
//
// On success, true is returned, false otherwise.
bool ChunkManager::attempt_enlarge_chunk(Metachunk* c) {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  return c->vsnode()->attempt_enlarge_chunk(c, &_chunks);
}

static void print_word_size_delta(outputStream* st, size_t word_size_1, size_t word_size_2) {
  if (word_size_1 == word_size_2) {
    print_scaled_words(st, word_size_1);
    st->print (" (no change)");
  } else {
    print_scaled_words(st, word_size_1);
    st->print("->");
    print_scaled_words(st, word_size_2);
    st->print(" (");
    if (word_size_2 <= word_size_1) {
      st->print("-");
      print_scaled_words(st, word_size_1 - word_size_2);
    } else {
      st->print("+");
      print_scaled_words(st, word_size_2 - word_size_1);
    }
    st->print(")");
  }
}

void ChunkManager::purge() {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  UL(info, ": reclaiming memory...");

  const size_t reserved_before = _vslist->reserved_words();
  const size_t committed_before = _vslist->committed_words();
  int num_nodes_purged = 0;

  // We purge to return unused memory to the Operating System. We do this in
  //  two independent steps.

  // 1) We purge the virtual space list: any memory mappings which are
  //   completely deserted can be potentially unmapped. We iterate over the list
  //   of mappings (VirtualSpaceList::purge) and delete every node whose memory
  //   only contains free chunks. Deleting that node includes unmapping its memory,
  //   so all chunk vanish automatically.
  //   Of course we need to remove the chunk headers of those vanished chunks from
  //   the ChunkManager freelist.
  num_nodes_purged = _vslist->purge(&_chunks);
  InternalStats::inc_num_purges();

  // 2) Since (1) is rather ineffective - it is rare that a whole node only contains
  //   free chunks - we now iterate over all remaining free chunks and
  //   and uncommit those which can be uncommitted (>= commit granule size).
  if (Settings::uncommit_free_chunks()) {
    const chunklevel_t max_level =
        chunklevel::level_fitting_word_size(Settings::commit_granule_words());
    for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL;
         l <= max_level;
         l++) {
      // Since we uncommit all chunks at this level, we do not break the "committed chunks are
      //  at the front of the list" condition.
      for (Metachunk* c = _chunks.first_at_level(l); c != NULL; c = c->next()) {
        c->uncommit_locked();
      }
    }
  }

  const size_t reserved_after = _vslist->reserved_words();
  const size_t committed_after = _vslist->committed_words();

  // Print a nice report.
  if (reserved_after == reserved_before && committed_after == committed_before) {
    UL(info, "nothing reclaimed.");
  } else {
    LogTarget(Info, metaspace) lt;
    if (lt.is_enabled()) {
      LogStream ls(lt);
      ls.print_cr(LOGFMT ": finished reclaiming memory: ", LOGFMT_ARGS);
      ls.print("reserved: ");
      print_word_size_delta(&ls, reserved_before, reserved_after);
      ls.cr();
      ls.print("committed: ");
      print_word_size_delta(&ls, committed_before, committed_after);
      ls.cr();
      ls.print_cr("full nodes purged: %d", num_nodes_purged);
    }
  }
  DEBUG_ONLY(_vslist->verify_locked());
  DEBUG_ONLY(verify_locked());
}

// Convenience methods to return the global class-space chunkmanager
//  and non-class chunkmanager, respectively.
ChunkManager* ChunkManager::chunkmanager_class() {
  return MetaspaceContext::context_class() == NULL ? NULL : MetaspaceContext::context_class()->cm();
}

ChunkManager* ChunkManager::chunkmanager_nonclass() {
  return MetaspaceContext::context_nonclass() == NULL ? NULL : MetaspaceContext::context_nonclass()->cm();
}

// Calculates the total number of committed words over all chunks. Walks chunks.
size_t ChunkManager::calc_committed_word_size() const {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  return calc_committed_word_size_locked();
}

size_t ChunkManager::calc_committed_word_size_locked() const {
  assert_lock_strong(Metaspace_lock);
  return _chunks.calc_committed_word_size();
}

// Update statistics.
void ChunkManager::add_to_statistics(ChunkManagerStats* out) const {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  for (chunklevel_t l = chunklevel::ROOT_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    out->_num_chunks[l] += _chunks.num_chunks_at_level(l);
    out->_committed_word_size[l] += _chunks.calc_committed_word_size_at_level(l);
  }
  DEBUG_ONLY(out->verify();)
}

#ifdef ASSERT

void ChunkManager::verify() const {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  verify_locked();
}

void ChunkManager::verify_locked() const {
  assert_lock_strong(Metaspace_lock);
  assert(_vslist != NULL, "No vslist");
  _chunks.verify();
}

bool ChunkManager::contains_chunk(Metachunk* c) const {
  return _chunks.contains(c);
}

#endif // ASSERT

void ChunkManager::print_on(outputStream* st) const {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  print_on_locked(st);
}

void ChunkManager::print_on_locked(outputStream* st) const {
  assert_lock_strong(Metaspace_lock);
  st->print_cr("cm %s: %d chunks, total word size: " SIZE_FORMAT ".", _name,
               total_num_chunks(), total_word_size());
  _chunks.print_on(st);
}

} // namespace metaspace
