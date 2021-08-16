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
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/metaspace/allocationGuard.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/freeBlocks.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metaspaceArena.hpp"
#include "memory/metaspace/metaspaceArenaGrowthPolicy.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
#include "runtime/atomic.hpp"
#include "runtime/init.hpp"
#include "runtime/mutexLocker.hpp"
#include "services/memoryService.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

#define LOGFMT         "Arena @" PTR_FORMAT " (%s)"
#define LOGFMT_ARGS    p2i(this), this->_name

// Returns the level of the next chunk to be added, acc to growth policy.
chunklevel_t MetaspaceArena::next_chunk_level() const {
  const int growth_step = _chunks.count();
  return _growth_policy->get_level_at_step(growth_step);
}

// Given a chunk, add its remaining free committed space to the free block list.
void MetaspaceArena::salvage_chunk(Metachunk* c) {
  if (Settings::handle_deallocations() == false) {
    return;
  }

  assert_lock_strong(lock());
  size_t remaining_words = c->free_below_committed_words();
  if (remaining_words > FreeBlocks::MinWordSize) {

    UL2(trace, "salvaging chunk " METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(c));

    MetaWord* ptr = c->allocate(remaining_words);
    assert(ptr != NULL, "Should have worked");
    _total_used_words_counter->increment_by(remaining_words);

    add_allocation_to_fbl(ptr, remaining_words);

    // After this operation: the chunk should have no free committed space left.
    assert(c->free_below_committed_words() == 0,
           "Salvaging chunk failed (chunk " METACHUNK_FULL_FORMAT ").",
           METACHUNK_FULL_FORMAT_ARGS(c));
  }
}

// Allocate a new chunk from the underlying chunk manager able to hold at least
// requested word size.
Metachunk* MetaspaceArena::allocate_new_chunk(size_t requested_word_size) {
  assert_lock_strong(lock());

  // Should this ever happen, we need to increase the maximum possible chunk size.
  guarantee(requested_word_size <= chunklevel::MAX_CHUNK_WORD_SIZE,
            "Requested size too large (" SIZE_FORMAT ") - max allowed size per allocation is " SIZE_FORMAT ".",
            requested_word_size, chunklevel::MAX_CHUNK_WORD_SIZE);

  const chunklevel_t max_level = chunklevel::level_fitting_word_size(requested_word_size);
  const chunklevel_t preferred_level = MIN2(max_level, next_chunk_level());

  Metachunk* c = _chunk_manager->get_chunk(preferred_level, max_level, requested_word_size);
  if (c == NULL) {
    return NULL;
  }

  assert(c->is_in_use(), "Wrong chunk state.");
  assert(c->free_below_committed_words() >= requested_word_size, "Chunk not committed");
  return c;
}

void MetaspaceArena::add_allocation_to_fbl(MetaWord* p, size_t word_size) {
  assert(Settings::handle_deallocations(), "Sanity");
  if (_fbl == NULL) {
    _fbl = new FreeBlocks(); // Create only on demand
  }
  _fbl->add_block(p, word_size);
}

MetaspaceArena::MetaspaceArena(ChunkManager* chunk_manager, const ArenaGrowthPolicy* growth_policy,
                               Mutex* lock, SizeAtomicCounter* total_used_words_counter,
                               const char* name) :
  _lock(lock),
  _chunk_manager(chunk_manager),
  _growth_policy(growth_policy),
  _chunks(),
  _fbl(NULL),
  _total_used_words_counter(total_used_words_counter),
  _name(name)
{
  UL(debug, ": born.");

  // Update statistics
  InternalStats::inc_num_arena_births();
}

MetaspaceArena::~MetaspaceArena() {
#ifdef ASSERT
  verify();
  if (Settings::use_allocation_guard()) {
    verify_allocation_guards();
  }
#endif

  MutexLocker fcl(lock(), Mutex::_no_safepoint_check_flag);
  MemRangeCounter return_counter;

  Metachunk* c = _chunks.first();
  Metachunk* c2 = NULL;

  while (c) {
    c2 = c->next();
    return_counter.add(c->used_words());
    DEBUG_ONLY(c->set_prev(NULL);)
    DEBUG_ONLY(c->set_next(NULL);)
    UL2(debug, "return chunk: " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));
    _chunk_manager->return_chunk(c);
    // c may be invalid after return_chunk(c) was called. Don't access anymore.
    c = c2;
  }

  UL2(info, "returned %d chunks, total capacity " SIZE_FORMAT " words.",
      return_counter.count(), return_counter.total_size());

  _total_used_words_counter->decrement_by(return_counter.total_size());
  DEBUG_ONLY(chunk_manager()->verify();)
  delete _fbl;
  UL(debug, ": dies.");

  // Update statistics
  InternalStats::inc_num_arena_deaths();
}

// Attempt to enlarge the current chunk to make it large enough to hold at least
//  requested_word_size additional words.
//
// On success, true is returned, false otherwise.
bool MetaspaceArena::attempt_enlarge_current_chunk(size_t requested_word_size) {
  assert_lock_strong(lock());

  Metachunk* c = current_chunk();
  assert(c->free_words() < requested_word_size, "Sanity");

  // Not if chunk enlargment is switched off...
  if (Settings::enlarge_chunks_in_place() == false) {
    return false;
  }
  // ... nor if we are already a root chunk ...
  if (c->is_root_chunk()) {
    return false;
  }
  // ... nor if the combined size of chunk content and new content would bring us above the size of a root chunk ...
  if ((c->used_words() + requested_word_size) > metaspace::chunklevel::MAX_CHUNK_WORD_SIZE) {
    return false;
  }

  const chunklevel_t new_level =
      chunklevel::level_fitting_word_size(c->used_words() + requested_word_size);
  assert(new_level < c->level(), "Sanity");

  // Atm we only enlarge by one level (so, doubling the chunk in size). So, if the requested enlargement
  // would require the chunk to more than double in size, we bail. But this covers about 99% of all cases,
  // so this is good enough.
  if (new_level < c->level() - 1) {
    return false;
  }
  // This only works if chunk is the leader of its buddy pair (and also if buddy
  // is free and unsplit, but that we cannot check outside of metaspace lock).
  if (!c->is_leader()) {
    return false;
  }
  // If the size added to the chunk would be larger than allowed for the next growth step
  // dont enlarge.
  if (next_chunk_level() > c->level()) {
    return false;
  }

  bool success = _chunk_manager->attempt_enlarge_chunk(c);
  assert(success == false || c->free_words() >= requested_word_size, "Sanity");
  return success;
}

// Allocate memory from Metaspace.
// 1) Attempt to allocate from the free block list.
// 2) Attempt to allocate from the current chunk.
// 3) Attempt to enlarge the current chunk in place if it is too small.
// 4) Attempt to get a new chunk and allocate from that chunk.
// At any point, if we hit a commit limit, we return NULL.
MetaWord* MetaspaceArena::allocate(size_t requested_word_size) {
  MutexLocker cl(lock(), Mutex::_no_safepoint_check_flag);
  UL2(trace, "requested " SIZE_FORMAT " words.", requested_word_size);

  MetaWord* p = NULL;
  const size_t raw_word_size = get_raw_word_size_for_requested_word_size(requested_word_size);

  // 1) Attempt to allocate from the free blocks list
  //    (Note: to reduce complexity, deallocation handling is disabled if allocation guards
  //     are enabled, see Settings::ergo_initialize())
  if (Settings::handle_deallocations() && _fbl != NULL && !_fbl->is_empty()) {
    p = _fbl->remove_block(raw_word_size);
    if (p != NULL) {
      DEBUG_ONLY(InternalStats::inc_num_allocs_from_deallocated_blocks();)
      UL2(trace, "taken from fbl (now: %d, " SIZE_FORMAT ").",
          _fbl->count(), _fbl->total_size());
      // Note: Space which is kept in the freeblock dictionary still counts as used as far
      //  as statistics go; therefore we skip the epilogue in this function to avoid double
      //  accounting.
      return p;
    }
  }

  bool current_chunk_too_small = false;
  bool commit_failure = false;

  if (current_chunk() != NULL) {

    // 2) Attempt to satisfy the allocation from the current chunk.

    // If the current chunk is too small to hold the requested size, attempt to enlarge it.
    // If that fails, retire the chunk.
    if (current_chunk()->free_words() < raw_word_size) {
      if (!attempt_enlarge_current_chunk(raw_word_size)) {
        current_chunk_too_small = true;
      } else {
        DEBUG_ONLY(InternalStats::inc_num_chunks_enlarged();)
        UL(debug, "enlarged chunk.");
      }
    }

    // Commit the chunk far enough to hold the requested word size. If that fails, we
    // hit a limit (either GC threshold or MaxMetaspaceSize). In that case retire the
    // chunk.
    if (!current_chunk_too_small) {
      if (!current_chunk()->ensure_committed_additional(raw_word_size)) {
        UL2(info, "commit failure (requested size: " SIZE_FORMAT ")", raw_word_size);
        commit_failure = true;
      }
    }

    // Allocate from the current chunk. This should work now.
    if (!current_chunk_too_small && !commit_failure) {
      p = current_chunk()->allocate(raw_word_size);
      assert(p != NULL, "Allocation from chunk failed.");
    }
  }

  if (p == NULL) {
    // If we are here, we either had no current chunk to begin with or it was deemed insufficient.
    assert(current_chunk() == NULL ||
           current_chunk_too_small || commit_failure, "Sanity");

    Metachunk* new_chunk = allocate_new_chunk(raw_word_size);
    if (new_chunk != NULL) {
      UL2(debug, "allocated new chunk " METACHUNK_FORMAT " for requested word size " SIZE_FORMAT ".",
          METACHUNK_FORMAT_ARGS(new_chunk), requested_word_size);

      assert(new_chunk->free_below_committed_words() >= raw_word_size, "Sanity");
      if (Settings::new_chunks_are_fully_committed()) {
        assert(new_chunk->is_fully_committed(), "Chunk should be fully committed.");
      }

      // We have a new chunk. Before making it the current chunk, retire the old one.
      if (current_chunk() != NULL) {
        salvage_chunk(current_chunk());
        DEBUG_ONLY(InternalStats::inc_num_chunks_retired();)
      }

      _chunks.add(new_chunk);

      // Now, allocate from that chunk. That should work.
      p = current_chunk()->allocate(raw_word_size);
      assert(p != NULL, "Allocation from chunk failed.");
    } else {
      UL2(info, "failed to allocate new chunk for requested word size " SIZE_FORMAT ".", requested_word_size);
    }
  }

#ifdef ASSERT
  // When using allocation guards, establish a prefix.
  if (p != NULL && Settings::use_allocation_guard()) {
    p = establish_prefix(p, raw_word_size);
  }
#endif

  if (p == NULL) {
    InternalStats::inc_num_allocs_failed_limit();
  } else {
    DEBUG_ONLY(InternalStats::inc_num_allocs();)
    _total_used_words_counter->increment_by(raw_word_size);
  }

  SOMETIMES(verify_locked();)

  if (p == NULL) {
    UL(info, "allocation failed, returned NULL.");
  } else {
    UL2(trace, "after allocation: %u chunk(s), current:" METACHUNK_FULL_FORMAT,
        _chunks.count(), METACHUNK_FULL_FORMAT_ARGS(current_chunk()));
    UL2(trace, "returning " PTR_FORMAT ".", p2i(p));
  }
  return p;
}

// Prematurely returns a metaspace allocation to the _block_freelists
// because it is not needed anymore (requires CLD lock to be active).
void MetaspaceArena::deallocate_locked(MetaWord* p, size_t word_size) {
  if (Settings::handle_deallocations() == false) {
    return;
  }

  assert_lock_strong(lock());
  // At this point a current chunk must exist since we only deallocate if we did allocate before.
  assert(current_chunk() != NULL, "stray deallocation?");
  assert(is_valid_area(p, word_size),
         "Pointer range not part of this Arena and cannot be deallocated: (" PTR_FORMAT ".." PTR_FORMAT ").",
         p2i(p), p2i(p + word_size));

  UL2(trace, "deallocating " PTR_FORMAT ", word size: " SIZE_FORMAT ".",
      p2i(p), word_size);

  size_t raw_word_size = get_raw_word_size_for_requested_word_size(word_size);
  add_allocation_to_fbl(p, raw_word_size);

  DEBUG_ONLY(verify_locked();)
}

// Prematurely returns a metaspace allocation to the _block_freelists because it is not
// needed anymore.
void MetaspaceArena::deallocate(MetaWord* p, size_t word_size) {
  MutexLocker cl(lock(), Mutex::_no_safepoint_check_flag);
  deallocate_locked(p, word_size);
}

// Update statistics. This walks all in-use chunks.
void MetaspaceArena::add_to_statistics(ArenaStats* out) const {
  MutexLocker cl(lock(), Mutex::_no_safepoint_check_flag);

  for (const Metachunk* c = _chunks.first(); c != NULL; c = c->next()) {
    InUseChunkStats& ucs = out->_stats[c->level()];
    ucs._num++;
    ucs._word_size += c->word_size();
    ucs._committed_words += c->committed_words();
    ucs._used_words += c->used_words();
    // Note: for free and waste, we only count what's committed.
    if (c == current_chunk()) {
      ucs._free_words += c->free_below_committed_words();
    } else {
      ucs._waste_words += c->free_below_committed_words();
    }
  }

  if (_fbl != NULL) {
    out->_free_blocks_num += _fbl->count();
    out->_free_blocks_word_size += _fbl->total_size();
  }

  SOMETIMES(out->verify();)
}

// Convenience method to get the most important usage statistics.
// For deeper analysis use add_to_statistics().
void MetaspaceArena::usage_numbers(size_t* p_used_words, size_t* p_committed_words, size_t* p_capacity_words) const {
  MutexLocker cl(lock(), Mutex::_no_safepoint_check_flag);
  size_t used = 0, comm = 0, cap = 0;
  for (const Metachunk* c = _chunks.first(); c != NULL; c = c->next()) {
    used += c->used_words();
    comm += c->committed_words();
    cap += c->word_size();
  }
  if (p_used_words != NULL) {
    *p_used_words = used;
  }
  if (p_committed_words != NULL) {
    *p_committed_words = comm;
  }
  if (p_capacity_words != NULL) {
    *p_capacity_words = cap;
  }
}

#ifdef ASSERT

void MetaspaceArena::verify_locked() const {
  assert_lock_strong(lock());
  assert(_growth_policy != NULL && _chunk_manager != NULL, "Sanity");
  _chunks.verify();
  if (_fbl != NULL) {
    _fbl->verify();
  }
}

void MetaspaceArena::verify_allocation_guards() const {
  assert(Settings::use_allocation_guard(), "Don't call with guards disabled.");

  // Verify canaries of all allocations.
  // (We can walk all allocations since at the start of a chunk an allocation
  //  must be present, and the allocation header contains its size, so we can
  //  find the next one).
  for (const Metachunk* c = _chunks.first(); c != NULL; c = c->next()) {
    const Prefix* first_broken_block = NULL;
    int num_broken_blocks = 0;
    const MetaWord* p = c->base();
    while (p < c->top()) {
      const Prefix* pp = (const Prefix*)p;
      if (!pp->is_valid()) {
        UL2(error, "Corrupt block at " PTR_FORMAT " (chunk: " METACHUNK_FORMAT ").",
            p2i(pp), METACHUNK_FORMAT_ARGS(c));
        if (first_broken_block == NULL) {
          first_broken_block = pp;
        }
        num_broken_blocks ++;
      }
      p += pp->_word_size;
    }
    // After examining all blocks in a chunk, assert if any of those blocks
    // was found to be corrupted.
    if (first_broken_block != NULL) {
      assert(false, "Corrupt block: found at least %d corrupt metaspace block(s) - "
             "first corrupted block at " PTR_FORMAT ".",
             num_broken_blocks, p2i(first_broken_block));
    }
  }
}

void MetaspaceArena::verify() const {
  MutexLocker cl(lock(), Mutex::_no_safepoint_check_flag);
  verify_locked();
}

// Returns true if the area indicated by pointer and size have actually been allocated
// from this arena.
bool MetaspaceArena::is_valid_area(MetaWord* p, size_t word_size) const {
  assert(p != NULL && word_size > 0, "Sanity");
  bool found = false;
  for (const Metachunk* c = _chunks.first(); c != NULL && !found; c = c->next()) {
    assert(c->is_valid_committed_pointer(p) ==
           c->is_valid_committed_pointer(p + word_size - 1), "range intersects");
    found = c->is_valid_committed_pointer(p);
  }
  return found;
}

#endif // ASSERT

void MetaspaceArena::print_on(outputStream* st) const {
  MutexLocker fcl(_lock, Mutex::_no_safepoint_check_flag);
  print_on_locked(st);
}

void MetaspaceArena::print_on_locked(outputStream* st) const {
  assert_lock_strong(_lock);
  st->print_cr("sm %s: %d chunks, total word size: " SIZE_FORMAT ", committed word size: " SIZE_FORMAT, _name,
               _chunks.count(), _chunks.calc_word_size(), _chunks.calc_committed_word_size());
  _chunks.print_on(st);
  st->cr();
  st->print_cr("growth-policy " PTR_FORMAT ", lock " PTR_FORMAT ", cm " PTR_FORMAT ", fbl " PTR_FORMAT,
                p2i(_growth_policy), p2i(_lock), p2i(_chunk_manager), p2i(_fbl));
}

} // namespace metaspace

