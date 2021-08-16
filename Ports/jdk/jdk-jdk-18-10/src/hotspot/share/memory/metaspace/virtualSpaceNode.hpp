/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_VIRTUALSPACENODE_HPP
#define SHARE_MEMORY_METASPACE_VIRTUALSPACENODE_HPP

#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "memory/metaspace/commitMask.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/rootChunkArea.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

class CommitLimiter;
class FreeChunkListVector;

// VirtualSpaceNode manages a single contiguous address range of metaspace. Logically that memory
//  region is split up into a sequence of "root chunk areas", each one containing one root chunk
//  or splinters of a root chunk.
//
// The underlying memory is also logically divided into a number of "commit granules", units of memory
//  which may be committed or uncommitted independently from each other.
//
// (Both root chunk areas and commit granules have not much to do with each other - one is a way to
//   reserve memory for the upper regions, see ChunkManager. One is a way to manage commited memory.)
//
// VirtualSpaceNode:
//  - exposes a function to allocate a new root chunk (see VirtualSpaceNode::allocate_root_chunk()).
//
//  - knows about the commit state of the memory region - which commit granule are committed, which
//    are not. It exposes functions to commit and uncommit regions (without actively committing
//    itself)
//
//  - It has a reference to a "CommitLimiter", an interface to query whether committing is
//    possible. That interface hides the various ways committing may be limited (GC threshold,
//    MaxMetaspaceSize, ...)
//
//  - It uses ReservedSpace to reserve its memory. It either owns the ReservedSpace or that
//    space got handed in from outside (ccs).
//
//
//
//
// | root chunk area               | root chunk area               | root chunk area               | <-- root chunk areas
//
// +-----------------------------------------------------------------------------------------------+
// |                                                                                               |
// |                                   `VirtualSpaceNode` memory                                   |
// |                                                                                               |
// +-----------------------------------------------------------------------------------------------+
//
// |x| |x|x|x| | | | |x|x|x| | | |x|x| | | |x|x|x|x| | | | | | | | |x| | | |x|x|x|x| | | |x| | | |x| <-- commit granules
//
// (x = committed)
//

class VirtualSpaceNode : public CHeapObj<mtClass> {

  // Link to next VirtualSpaceNode
  VirtualSpaceNode* _next;

  // The underlying space. This has been either created by this node
  //  and is owned by it, or has been handed in from outside (e.g. in
  //  case of CompressedClassSpace).
  ReservedSpace _rs;

  // True if the node owns the reserved space, false if not.
  const bool _owns_rs;

  // Start pointer of the area.
  MetaWord* const _base;

  // Size, in words, of the whole node
  const size_t _word_size;

  // Size, in words, of the range of this node which has been handed out in
  // the form of root chunks.
  size_t _used_words;

  // The bitmap describing the commit state of the region:
  // Each bit covers a region of 64K (see constants::commit_granule_size).
  CommitMask _commit_mask;

  // An array/lookup table of RootChunkArea objects. Each one describes a root chunk area.
  RootChunkAreaLUT _root_chunk_area_lut;

  // Limiter object to ask before expanding the committed size of this node.
  CommitLimiter* const _commit_limiter;

  // Points to outside size counters which we are to increase/decrease when we commit/uncommit
  // space from this node.
  SizeCounter* const _total_reserved_words_counter;
  SizeCounter* const _total_committed_words_counter;

  /// committing, uncommitting ///

  // Given a pointer into this node, calculate the start of the commit granule
  // the pointer points into.
  MetaWord* calc_start_of_granule(MetaWord* p) const {
    DEBUG_ONLY(check_pointer(p));
    return align_down(p, Settings::commit_granule_bytes());
  }

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
  bool commit_range(MetaWord* p, size_t word_size);

  //// creation ////

  // Create a new empty node spanning the given given reserved space.
  VirtualSpaceNode(ReservedSpace rs, bool owns_rs, CommitLimiter* limiter,
                   SizeCounter* reserve_counter, SizeCounter* commit_counter);

public:

  // Create a node of a given size (it will create its own space).
  static VirtualSpaceNode* create_node(size_t word_size, CommitLimiter* limiter, SizeCounter* reserve_words_counter,
                                       SizeCounter* commit_words_counter);

  // Create a node over an existing space
  static VirtualSpaceNode* create_node(ReservedSpace rs, CommitLimiter* limiter, SizeCounter* reserve_words_counter,
                                       SizeCounter* commit_words_counter);

  ~VirtualSpaceNode();

  // Note: public for gtests only, could be private.
  MetaWord* base() const        { return _base; }

  // Reserved size of the whole node.
  size_t word_size() const      { return _word_size; }

  //// Chunk allocation, splitting, merging /////

  // Allocate a root chunk from this node. Will fail and return NULL if the node is full
  //  - if we used up the whole address space of this node's memory region.
  //    (in case this node backs compressed class space, this is how we hit
  //     CompressedClassSpaceSize).
  // Note that this just returns reserved memory; caller must take care of committing this
  //  chunk before using it.
  Metachunk* allocate_root_chunk();

  // Given a chunk c, split it recursively until you get a chunk of the given target_level.
  //
  // The resulting target chunk resides at the same address as the original chunk.
  // The resulting splinters are added to freelists.
  void split(chunklevel_t target_level, Metachunk* c, FreeChunkListVector* freelists);

  // Given a chunk, attempt to merge it recursively with its neighboring chunks.
  //
  // If successful (merged at least once), returns address of
  // the merged chunk; NULL otherwise.
  //
  // The merged chunks are removed from the freelists.
  //
  // !!! Please note that if this method returns a non-NULL value, the
  // original chunk will be invalid and should not be accessed anymore! !!!
  Metachunk* merge(Metachunk* c, FreeChunkListVector* freelists);

  // Given a chunk c, which must be "in use" and must not be a root chunk, attempt to
  // enlarge it in place by claiming its trailing buddy.
  //
  // This will only work if c is the leader of the buddy pair and the trailing buddy is free.
  //
  // If successful, the follower chunk will be removed from the freelists, the leader chunk c will
  // double in size (level decreased by one).
  //
  // On success, true is returned, false otherwise.
  bool attempt_enlarge_chunk(Metachunk* c, FreeChunkListVector* freelists);

  // Attempts to purge the node:
  //
  // If all chunks living in this node are free, they will all be removed from
  //  the freelist they currently reside in. Then, the node will be deleted.
  //
  // Returns true if the node has been deleted, false if not.
  // !! If this returns true, do not access the node from this point on. !!
  bool attempt_purge(FreeChunkListVector* freelists);

  // Attempts to uncommit free areas according to the rules set in settings.
  // Returns number of words uncommitted.
  size_t uncommit_free_areas();

  /// misc /////

  // Returns size, in words, of the used space in this node alone.
  // (Notes:
  //  - This is the space handed out to the ChunkManager, so it is "used" from the viewpoint of this node,
  //    but not necessarily used for Metadata.
  //  - This may or may not be committed memory.
  size_t used_words() const             { return _used_words; }

  // Returns size, in words, of how much space is left in this node alone.
  size_t free_words() const             { return _word_size - _used_words; }

  // Returns size, in words, of committed space in this node alone.
  // Note: iterates over commit mask and hence may be a tad expensive on large nodes.
  size_t committed_words() const;

  //// Committing/uncommitting memory /////

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
  // Returns true if success, false if it did hit a commit limit.
  bool ensure_range_is_committed(MetaWord* p, size_t word_size);

  // Given an address range (which has to be aligned to commit granule size):
  //  - uncommit it
  //  - mark it as uncommitted in the commit mask
  void uncommit_range(MetaWord* p, size_t word_size);

  //// List stuff ////
  VirtualSpaceNode* next() const        { return _next; }
  void set_next(VirtualSpaceNode* vsn)  { _next = vsn; }

  /// Debug stuff ////

  // Print a description about this node.
  void print_on(outputStream* st) const;

  // Verify counters and basic structure. Slow mode: verify all chunks in depth
  bool contains(const MetaWord* p) const {
    return p >= _base && p < _base + _used_words;
  }

#ifdef ASSERT
  void check_pointer(const MetaWord* p) const {
    assert(contains(p), "invalid pointer");
  }
  void verify() const;
  void verify_locked() const;
#endif

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_VIRTUALSPACENODE_HPP
