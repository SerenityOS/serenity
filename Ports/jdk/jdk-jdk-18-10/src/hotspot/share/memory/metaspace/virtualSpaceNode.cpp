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
#include "gc/shared/gc_globals.hpp"
#include "logging/log.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspace/chunkHeaderPool.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/rootChunkArea.hpp"
#include "memory/metaspace/runningCounters.hpp"
#include "memory/metaspace/virtualSpaceNode.hpp"
#include "runtime/globals.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

#define LOGFMT         "VsListNode @" PTR_FORMAT " base " PTR_FORMAT " "
#define LOGFMT_ARGS    p2i(this), p2i(_base)

#ifdef ASSERT
void check_pointer_is_aligned_to_commit_granule(const MetaWord* p) {
  assert(is_aligned(p, Settings::commit_granule_bytes()),
         "Pointer not aligned to commit granule size: " PTR_FORMAT ".",
         p2i(p));
}
void check_word_size_is_aligned_to_commit_granule(size_t word_size) {
  assert(is_aligned(word_size, Settings::commit_granule_words()),
         "Not aligned to commit granule size: " SIZE_FORMAT ".", word_size);
}
#endif

// Given an address range, ensure it is committed.
//
// The range has to be aligned to granule size.
//
// Function will:
// - check how many granules in that region are uncommitted; If all are committed, it
//    returns true immediately.
// - check if committing those uncommitted granules would bring us over the commit limit
//    (GC threshold, MaxMetaspaceSize). If true, it returns false.
// - commit the memory.
// - mark the range as committed in the commit mask
//
// Returns true if success, false if it did hit a commit limit.
bool VirtualSpaceNode::commit_range(MetaWord* p, size_t word_size) {
  DEBUG_ONLY(check_pointer_is_aligned_to_commit_granule(p);)
  DEBUG_ONLY(check_word_size_is_aligned_to_commit_granule(word_size);)
  assert_lock_strong(Metaspace_lock);

  // First calculate how large the committed regions in this range are
  const size_t committed_words_in_range = _commit_mask.get_committed_size_in_range(p, word_size);
  DEBUG_ONLY(check_word_size_is_aligned_to_commit_granule(committed_words_in_range);)

  // By how much words we would increase commit charge
  //  were we to commit the given address range completely.
  const size_t commit_increase_words = word_size - committed_words_in_range;

  UL2(debug, "committing range " PTR_FORMAT ".." PTR_FORMAT "(" SIZE_FORMAT " words)",
      p2i(p), p2i(p + word_size), word_size);

  if (commit_increase_words == 0) {
    UL(debug, "... already fully committed.");
    return true; // Already fully committed, nothing to do.
  }

  // Before committing any more memory, check limits.
  if (_commit_limiter->possible_expansion_words() < commit_increase_words) {
    UL(debug, "... cannot commit (limit).");
    return false;
  }

  // Commit...
  if (os::commit_memory((char*)p, word_size * BytesPerWord, false) == false) {
    vm_exit_out_of_memory(word_size * BytesPerWord, OOM_MMAP_ERROR, "Failed to commit metaspace.");
  }

  if (AlwaysPreTouch) {
    os::pretouch_memory(p, p + word_size);
  }

  UL2(debug, "... committed " SIZE_FORMAT " additional words.", commit_increase_words);

  // ... tell commit limiter...
  _commit_limiter->increase_committed(commit_increase_words);

  // ... update counters in containing vslist ...
  _total_committed_words_counter->increment_by(commit_increase_words);

  // ... and update the commit mask.
  _commit_mask.mark_range_as_committed(p, word_size);

#ifdef ASSERT
  // The commit boundary maintained in the CommitLimiter should be equal the sum of committed words
  // in both class and non-class vslist (outside gtests).
  if (_commit_limiter == CommitLimiter::globalLimiter()) {
    assert(_commit_limiter->committed_words() == RunningCounters::committed_words(), "counter mismatch");
  }
#endif

  InternalStats::inc_num_space_committed();
  return true;
}

// Given an address range, ensure it is committed.
//
// The range does not have to be aligned to granule size. However, the function will always commit
// whole granules.
//
// Function will:
// - check how many granules in that region are uncommitted; If all are committed, it
//    returns true immediately.
// - check if committing those uncommitted granules would bring us over the commit limit
//    (GC threshold, MaxMetaspaceSize). If true, it returns false.
// - commit the memory.
// - mark the range as committed in the commit mask
//
// !! Careful:
//    calling ensure_range_is_committed on a range which contains both committed and uncommitted
//    areas will commit the whole area, thus erase the content in the existing committed parts.
//    Make sure you never call this on an address range containing live data. !!
//
// Returns true if success, false if it did hit a commit limit.
bool VirtualSpaceNode::ensure_range_is_committed(MetaWord* p, size_t word_size) {
  assert_lock_strong(Metaspace_lock);
  assert(p != NULL && word_size > 0, "Sanity");
  MetaWord* p_start = align_down(p, Settings::commit_granule_bytes());
  MetaWord* p_end = align_up(p + word_size, Settings::commit_granule_bytes());
  return commit_range(p_start, p_end - p_start);
}

// Given an address range (which has to be aligned to commit granule size):
//  - uncommit it
//  - mark it as uncommitted in the commit mask
void VirtualSpaceNode::uncommit_range(MetaWord* p, size_t word_size) {
  DEBUG_ONLY(check_pointer_is_aligned_to_commit_granule(p);)
  DEBUG_ONLY(check_word_size_is_aligned_to_commit_granule(word_size);)
  assert_lock_strong(Metaspace_lock);

  // First calculate how large the committed regions in this range are
  const size_t committed_words_in_range = _commit_mask.get_committed_size_in_range(p, word_size);
  DEBUG_ONLY(check_word_size_is_aligned_to_commit_granule(committed_words_in_range);)

  UL2(debug, "uncommitting range " PTR_FORMAT ".." PTR_FORMAT "(" SIZE_FORMAT " words)",
      p2i(p), p2i(p + word_size), word_size);

  if (committed_words_in_range == 0) {
    UL(debug, "... already fully uncommitted.");
    return; // Already fully uncommitted, nothing to do.
  }

  // Uncommit...
  if (os::uncommit_memory((char*)p, word_size * BytesPerWord) == false) {
    // Note: this can actually happen, since uncommit may increase the number of mappings.
    fatal("Failed to uncommit metaspace.");
  }

  UL2(debug, "... uncommitted " SIZE_FORMAT " words.", committed_words_in_range);

  // ... tell commit limiter...
  _commit_limiter->decrease_committed(committed_words_in_range);

  // ... and global counters...
  _total_committed_words_counter->decrement_by(committed_words_in_range);

   // ... and update the commit mask.
  _commit_mask.mark_range_as_uncommitted(p, word_size);

#ifdef ASSERT
  // The commit boundary maintained in the CommitLimiter should be equal the sum of committed words
  // in both class and non-class vslist (outside gtests).
  if (_commit_limiter == CommitLimiter::globalLimiter()) { // We are outside a test scenario
    assert(_commit_limiter->committed_words() == RunningCounters::committed_words(), "counter mismatch");
  }
#endif
  InternalStats::inc_num_space_uncommitted();
}

//// creation, destruction ////

VirtualSpaceNode::VirtualSpaceNode(ReservedSpace rs, bool owns_rs, CommitLimiter* limiter,
                                   SizeCounter* reserve_counter, SizeCounter* commit_counter) :
  _next(NULL),
  _rs(rs),
  _owns_rs(owns_rs),
  _base((MetaWord*)rs.base()),
  _word_size(rs.size() / BytesPerWord),
  _used_words(0),
  _commit_mask((MetaWord*)rs.base(), rs.size() / BytesPerWord),
  _root_chunk_area_lut((MetaWord*)rs.base(), rs.size() / BytesPerWord),
  _commit_limiter(limiter),
  _total_reserved_words_counter(reserve_counter),
  _total_committed_words_counter(commit_counter)
{
  UL2(debug, "born (word_size " SIZE_FORMAT ").", _word_size);

  // Update reserved counter in vslist
  _total_reserved_words_counter->increment_by(_word_size);

  assert_is_aligned(_base, chunklevel::MAX_CHUNK_BYTE_SIZE);
  assert_is_aligned(_word_size, chunklevel::MAX_CHUNK_WORD_SIZE);
}

// Create a node of a given size (it will create its own space).
VirtualSpaceNode* VirtualSpaceNode::create_node(size_t word_size,
                                                CommitLimiter* limiter, SizeCounter* reserve_words_counter,
                                                SizeCounter* commit_words_counter)
{
  DEBUG_ONLY(assert_is_aligned(word_size, chunklevel::MAX_CHUNK_WORD_SIZE);)
  ReservedSpace rs(word_size * BytesPerWord,
                   Settings::virtual_space_node_reserve_alignment_words() * BytesPerWord,
                   os::vm_page_size());
  if (!rs.is_reserved()) {
    vm_exit_out_of_memory(word_size * BytesPerWord, OOM_MMAP_ERROR, "Failed to reserve memory for metaspace");
  }
  MemTracker::record_virtual_memory_type(rs.base(), mtMetaspace);
  assert_is_aligned(rs.base(), chunklevel::MAX_CHUNK_BYTE_SIZE);
  InternalStats::inc_num_vsnodes_births();
  return new VirtualSpaceNode(rs, true, limiter, reserve_words_counter, commit_words_counter);
}

// Create a node over an existing space
VirtualSpaceNode* VirtualSpaceNode::create_node(ReservedSpace rs, CommitLimiter* limiter,
                                                SizeCounter* reserve_words_counter, SizeCounter* commit_words_counter)
{
  InternalStats::inc_num_vsnodes_births();
  return new VirtualSpaceNode(rs, false, limiter, reserve_words_counter, commit_words_counter);
}

VirtualSpaceNode::~VirtualSpaceNode() {
  DEBUG_ONLY(verify_locked();)

  UL(debug, ": dies.");
  if (_owns_rs) {
    _rs.release();
  }

  // Update counters in vslist
  size_t committed = committed_words();
  _total_committed_words_counter->decrement_by(committed);
  _total_reserved_words_counter->decrement_by(_word_size);

  // ... and tell commit limiter
  _commit_limiter->decrease_committed(committed);

  InternalStats::inc_num_vsnodes_deaths();
}

//// Chunk allocation, splitting, merging /////

// Allocate a root chunk from this node. Will fail and return NULL if the node is full
//  - if we used up the whole address space of this node's memory region.
//    (in case this node backs compressed class space, this is how we hit
//     CompressedClassSpaceSize).
// Note that this just returns reserved memory; caller must take care of committing this
//  chunk before using it.
Metachunk* VirtualSpaceNode::allocate_root_chunk() {
  assert_lock_strong(Metaspace_lock);
  assert_is_aligned(free_words(), chunklevel::MAX_CHUNK_WORD_SIZE);

  if (free_words() >= chunklevel::MAX_CHUNK_WORD_SIZE) {

    MetaWord* loc = _base + _used_words;
    _used_words += chunklevel::MAX_CHUNK_WORD_SIZE;

    RootChunkArea* rca = _root_chunk_area_lut.get_area_by_address(loc);

    // Create a root chunk header and initialize it;
    Metachunk* c = rca->alloc_root_chunk_header(this);
    assert(c->base() == loc && c->vsnode() == this &&
           c->is_free(), "Sanity");
    DEBUG_ONLY(c->verify();)

    UL2(debug, "new root chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));
    return c;
  }
  return NULL; // Node is full.
}

// Given a chunk c, split it recursively until you get a chunk of the given target_level.
//
// The resulting target chunk resides at the same address as the original chunk.
// The resulting splinters are added to freelists.
void VirtualSpaceNode::split(chunklevel_t target_level, Metachunk* c, FreeChunkListVector* freelists) {
  assert_lock_strong(Metaspace_lock);
  // Get the area associated with this chunk and let it handle the splitting
  RootChunkArea* rca = _root_chunk_area_lut.get_area_by_address(c->base());
  DEBUG_ONLY(rca->verify_area_is_ideally_merged();)
  rca->split(target_level, c, freelists);
}

// Given a chunk, attempt to merge it recursively with its neighboring chunks.
//
// If successful (merged at least once), returns address of
// the merged chunk; NULL otherwise.
//
// The merged chunks are removed from the freelists.
//
// !!! Please note that if this method returns a non-NULL value, the
// original chunk will be invalid and should not be accessed anymore! !!!
Metachunk* VirtualSpaceNode::merge(Metachunk* c, FreeChunkListVector* freelists) {
  assert(c != NULL && c->is_free(), "Sanity");
  assert_lock_strong(Metaspace_lock);

  // Get the rca associated with this chunk and let it handle the merging
  RootChunkArea* rca = _root_chunk_area_lut.get_area_by_address(c->base());
  Metachunk* c2 = rca->merge(c, freelists);
  DEBUG_ONLY(rca->verify_area_is_ideally_merged();)
  return c2;
}

// Given a chunk c, which must be "in use" and must not be a root chunk, attempt to
// enlarge it in place by claiming its trailing buddy.
//
// This will only work if c is the leader of the buddy pair and the trailing buddy is free.
//
// If successful, the follower chunk will be removed from the freelists, the leader chunk c will
// double in size (level decreased by one).
//
// On success, true is returned, false otherwise.
bool VirtualSpaceNode::attempt_enlarge_chunk(Metachunk* c, FreeChunkListVector* freelists) {
  assert(c != NULL && c->is_in_use() && !c->is_root_chunk(), "Sanity");
  assert_lock_strong(Metaspace_lock);

  // Get the rca associated with this chunk and let it handle the merging
  RootChunkArea* rca = _root_chunk_area_lut.get_area_by_address(c->base());

  bool rc = rca->attempt_enlarge_chunk(c, freelists);
  DEBUG_ONLY(rca->verify_area_is_ideally_merged();)
  if (rc) {
    InternalStats::inc_num_chunks_enlarged();
  }

  return rc;
}

// Attempts to purge the node:
//
// If all chunks living in this node are free, they will all be removed from
//  the freelist they currently reside in. Then, the node will be deleted.
//
// Returns true if the node has been deleted, false if not.
// !! If this returns true, do not access the node from this point on. !!
bool VirtualSpaceNode::attempt_purge(FreeChunkListVector* freelists) {
  assert_lock_strong(Metaspace_lock);

  if (!_owns_rs) {
    // We do not allow purging of nodes if we do not own the
    // underlying ReservedSpace (CompressClassSpace case).
    return false;
  }

  // First find out if all areas are empty. Since empty chunks collapse to root chunk
  // size, if all chunks in this node are free root chunks we are good to go.
  if (!_root_chunk_area_lut.is_free()) {
    return false;
  }

  UL(debug, ": purging.");

  // Okay, we can purge. Before we can do this, we need to remove all chunks from the freelist.
  for (int narea = 0; narea < _root_chunk_area_lut.number_of_areas(); narea++) {
    RootChunkArea* ra = _root_chunk_area_lut.get_area_by_index(narea);
    Metachunk* c = ra->first_chunk();
    if (c != NULL) {
      UL2(trace, "removing chunk from to-be-purged node: "
          METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(c));
      assert(c->is_free() && c->is_root_chunk(), "Sanity");
      freelists->remove(c);
    }
  }

  // Now, delete the node, then right away return since this object is invalid.
  delete this;

  return true;
}

void VirtualSpaceNode::print_on(outputStream* st) const {
  size_t scale = K;

  st->print("base " PTR_FORMAT ": ", p2i(base()));
  st->print("reserved=");
  print_scaled_words(st, word_size(), scale);
  st->print(", committed=");
  print_scaled_words_and_percentage(st, committed_words(), word_size(), scale);
  st->print(", used=");
  print_scaled_words_and_percentage(st, used_words(), word_size(), scale);

  st->cr();
  _root_chunk_area_lut.print_on(st);
  _commit_mask.print_on(st);
}

// Returns size, in words, of committed space in this node alone.
// Note: iterates over commit mask and hence may be a tad expensive on large nodes.
size_t VirtualSpaceNode::committed_words() const {
  return _commit_mask.get_committed_size();
}

#ifdef ASSERT
void VirtualSpaceNode::verify() const {
  MutexLocker fcl(Metaspace_lock, Mutex::_no_safepoint_check_flag);
  verify_locked();
}

volatile int test_access = 0;

// Verify counters and basic structure. Slow mode: verify all chunks in depth
void VirtualSpaceNode::verify_locked() const {
  assert_lock_strong(Metaspace_lock);
  assert(base() != NULL, "Invalid base");
  assert(base() == (MetaWord*)_rs.base() &&
         word_size() == _rs.size() / BytesPerWord,
         "Sanity");
  assert_is_aligned(base(), chunklevel::MAX_CHUNK_BYTE_SIZE);
  assert(used_words() <= word_size(), "Sanity");
  // Since we only ever hand out root chunks from a vsnode, top should always be aligned
  // to root chunk size.
  assert_is_aligned(used_words(), chunklevel::MAX_CHUNK_WORD_SIZE);

  _commit_mask.verify();

  // Verify memory against commit mask.
  SOMETIMES(
    for (MetaWord* p = base(); p < base() + used_words(); p += os::vm_page_size()) {
      if (_commit_mask.is_committed_address(p)) {
        test_access += *(int*)p;
      }
    }
  )

  assert(committed_words() <= word_size(), "Sanity");
  assert_is_aligned(committed_words(), Settings::commit_granule_words());
  _root_chunk_area_lut.verify();
}

#endif

} // namespace metaspace
