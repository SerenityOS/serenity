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

#ifndef SHARE_MEMORY_METASPACE_CHUNKMANAGER_HPP
#define SHARE_MEMORY_METASPACE_CHUNKMANAGER_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/metachunk.hpp"

namespace metaspace {

class VirtualSpaceList;
struct ChunkManagerStats;

// ChunkManager has a somewhat central role.

// Arenas request chunks from it and, on death, return chunks back to it.
//  It keeps freelists for chunks, one per chunk level, sorted by chunk
//  commit state.
//  To feed the freelists, it allocates root chunks from the associated
//  VirtualSpace below it.
//
// ChunkManager directs splitting chunks, if a chunk request cannot be
//  fulfilled directly. It also takes care of merging when chunks are
//  returned to it, before they are added to the freelist.
//
// The freelists are double linked double headed; fully committed chunks
//  are added to the front, others to the back.
//
// Level
//          +--------------------+   +--------------------+
//  0  +----|  free root chunk   |---|  free root chunk   |---...
//     |    +--------------------+   +--------------------+
//     |
//     |    +----------+   +----------+
//  1  +----|          |---|          |---...
//     |    +----------+   +----------+
//     |
//  .
//  .
//  .
//
//     |    +-+   +-+
//  12 +----| |---| |---...
//          +-+   +-+

class ChunkManager : public CHeapObj<mtMetaspace> {

  // A chunk manager is connected to a virtual space list which is used
  // to allocate new root chunks when no free chunks are found.
  VirtualSpaceList* const _vslist;

  // Name
  const char* const _name;

  // Freelists
  FreeChunkListVector _chunks;

  // Returns true if this manager contains the given chunk. Slow (walks free lists) and
  // only needed for verifications.
  DEBUG_ONLY(bool contains_chunk(Metachunk* c) const;)

  // Given a chunk, split it into a target chunk of a smaller size (target level)
  //  at least one, possible more splinter chunks. Splinter chunks are added to the
  //  freelist.
  // The original chunk must be outside of the freelist and its state must be free.
  // The resulting target chunk will be located at the same address as the original
  //  chunk, but it will of course be smaller (of a higher level).
  // The committed areas within the original chunk carry over to the resulting
  //  chunks.
  void split_chunk_and_add_splinters(Metachunk* c, chunklevel_t target_level);

  // See get_chunk(s,s,s)
  Metachunk* get_chunk_locked(size_t preferred_word_size, size_t min_word_size, size_t min_committed_words);

  // Uncommit all chunks equal or below the given level.
  void uncommit_free_chunks(chunklevel_t max_level);

  // Return a single chunk to the freelist without doing any merging, and adjust accounting.
  void return_chunk_simple_locked(Metachunk* c);

  // See return_chunk().
  void return_chunk_locked(Metachunk* c);

  // Calculates the total number of committed words over all chunks. Walks chunks.
  size_t calc_committed_word_size_locked() const;

public:

  // Creates a chunk manager with a given name (which is for debug purposes only)
  // and an associated space list which will be used to request new chunks from
  // (see get_chunk())
  ChunkManager(const char* name, VirtualSpaceList* space_list);

  // On success, returns a chunk of level of <preferred_level>, but at most <max_level>.
  //  The first <min_committed_words> of the chunk are guaranteed to be committed.
  // On error, will return NULL.
  //
  // This function may fail for two reasons:
  // - Either we are unable to reserve space for a new chunk (if the underlying VirtualSpaceList
  //   is non-expandable but needs expanding - aka out of compressed class space).
  // - Or, if the necessary space cannot be committed because we hit a commit limit.
  //   This may be either the GC threshold or MaxMetaspaceSize.
  Metachunk* get_chunk(chunklevel_t preferred_level, chunklevel_t max_level, size_t min_committed_words);

  // Convenience function - get a chunk of a given level, uncommitted.
  Metachunk* get_chunk(chunklevel_t lvl) { return get_chunk(lvl, lvl, 0); }

  // Return a single chunk to the ChunkManager and adjust accounting. May merge chunk
  //  with neighbors.
  // Happens after a Classloader was unloaded and releases its metaspace chunks.
  // !! Notes:
  //    1) After this method returns, c may not be valid anymore. ** Do not access c after this function returns **.
  //    2) This function will not remove c from its current chunk list. This has to be done by the caller prior to
  //       calling this method.
  void return_chunk(Metachunk* c);

  // Given a chunk c, which must be "in use" and must not be a root chunk, attempt to
  // enlarge it in place by claiming its trailing buddy.
  //
  // This will only work if c is the leader of the buddy pair and the trailing buddy is free.
  //
  // If successful, the follower chunk will be removed from the freelists, the leader chunk c will
  // double in size (level decreased by one).
  //
  // On success, true is returned, false otherwise.
  bool attempt_enlarge_chunk(Metachunk* c);

  // Attempt to reclaim free areas in metaspace wholesale:
  // - first, attempt to purge nodes of the backing virtual space list: nodes which are completely
  //   unused get unmapped and deleted completely.
  // - second, it will uncommit free chunks depending on commit granule size.
  void purge();

  // Run verifications. slow=true: verify chunk-internal integrity too.
  DEBUG_ONLY(void verify() const;)
  DEBUG_ONLY(void verify_locked() const;)

  // Returns the name of this chunk manager.
  const char* name() const                  { return _name; }

  // Returns total number of chunks
  int total_num_chunks() const              { return _chunks.num_chunks(); }

  // Returns number of words in all free chunks (regardless of commit state).
  size_t total_word_size() const            { return _chunks.word_size(); }

  // Calculates the total number of committed words over all chunks. Walks chunks.
  size_t calc_committed_word_size() const;

  // Update statistics.
  void add_to_statistics(ChunkManagerStats* out) const;

  void print_on(outputStream* st) const;
  void print_on_locked(outputStream* st) const;

  // Convenience methods to return the global class-space chunkmanager
  //  and non-class chunkmanager, respectively.
  static ChunkManager* chunkmanager_class();
  static ChunkManager* chunkmanager_nonclass();

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_CHUNKMANAGER_HPP
