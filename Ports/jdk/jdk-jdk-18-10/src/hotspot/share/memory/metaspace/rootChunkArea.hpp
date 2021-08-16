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

#ifndef SHARE_MEMORY_METASPACE_ROOTCHUNKAREA_HPP
#define SHARE_MEMORY_METASPACE_ROOTCHUNKAREA_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

class Metachunk;
class MetachunkClosure;
class FreeChunkListVector;
class VirtualSpaceNode;

// RootChunkArea manages a memory area covering a single root chunk.
//
// Such an area may contain a single root chunk, or a number of chunks the
//  root chunk was split into.
//
// RootChunkArea contains the functionality to merge and split chunks in
//  buddy allocator fashion.
//

class RootChunkArea {

  // The base address of this area.
  // Todo: this may be somewhat superfluous since RootChunkArea only exist in the
  //  context of a series of chunks, so the address is somewhat implicit. Remove?
  const MetaWord* const _base;

  // The first chunk in this area; if this area is maximally
  // folded, this is the root chunk covering the whole area size.
  Metachunk* _first_chunk;

public:

  RootChunkArea(const MetaWord* base);
  ~RootChunkArea();

  // Initialize: allocate a root node and a root chunk header; return the
  // root chunk header. It will be partly initialized.
  // Note: this just allocates a memory-less header; memory itself is allocated inside VirtualSpaceNode.
  Metachunk* alloc_root_chunk_header(VirtualSpaceNode* node);

  // Given a chunk c, split it recursively until you get a chunk of the given target_level.
  //
  // The resulting target chunk resides at the same address as the original chunk.
  // The resulting splinters are added to freelists.
  //
  // Returns pointer to the result chunk; the splitted-off chunks are added as
  //  free chunks to the freelists.
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

  /// range ///

  const MetaWord* base() const  { return _base; }
  size_t word_size() const      { return chunklevel::MAX_CHUNK_WORD_SIZE; }
  const MetaWord* end() const   { return _base + word_size(); }

  // Direct access to the first chunk (use with care)
  Metachunk* first_chunk()              { return _first_chunk; }
  const Metachunk* first_chunk() const  { return _first_chunk; }

  // Returns true if this root chunk area is completely free:
  //  In that case, it should only contain one chunk (maximally merged, so a root chunk)
  //  and it should be free.
  bool is_free() const;

  //// Debug stuff ////

#ifdef ASSERT
  void check_pointer(const MetaWord* p) const {
    assert(p >= _base && p < _base + word_size(),
           "pointer " PTR_FORMAT " oob for this root area [" PTR_FORMAT ".." PTR_FORMAT ")",
           p2i(p), p2i(_base), p2i(_base + word_size()));
  }
  void verify() const;

  // This is a separate operation from verify(). We should be able to call verify()
  // from almost anywhere, regardless of state, but verify_area_is_ideally_merged()
  // can only be called outside split and merge ops.
  void verify_area_is_ideally_merged() const;
#endif // ASSERT

  void print_on(outputStream* st) const;

};

// RootChunkAreaLUT (lookup table) manages a series of contiguous root chunk areas
//  in memory (in the context of a VirtualSpaceNode). It allows finding the containing
//  root chunk for any given memory address. It allows for easy iteration over all
//  root chunks.
// Beyond that it is unexciting.
class RootChunkAreaLUT {

  // Base address of the whole area.
  const MetaWord* const _base;

  // Number of root chunk areas.
  const int _num;

  // Array of RootChunkArea objects.
  RootChunkArea* _arr;

#ifdef ASSERT
  void check_pointer(const MetaWord* p) const {
    assert(p >= base() && p < base() + word_size(), "Invalid pointer");
  }
#endif

  // Given an address into this range, return the index into the area array for the
  // area this address falls into.
  int index_by_address(const MetaWord* p) const {
    DEBUG_ONLY(check_pointer(p);)
    int idx = (int)((p - base()) / chunklevel::MAX_CHUNK_WORD_SIZE);
    assert(idx >= 0 && idx < _num, "Sanity");
    return idx;
  }

public:

  RootChunkAreaLUT(const MetaWord* base, size_t word_size);
  ~RootChunkAreaLUT();

  // Given a memory address into the range this array covers, return the
  // corresponding area object. If none existed at this position, create it
  // on demand.
  RootChunkArea* get_area_by_address(const MetaWord* p) const {
    const int idx = index_by_address(p);
    RootChunkArea* ra = _arr + idx;
    DEBUG_ONLY(ra->check_pointer(p);)
    return _arr + idx;
  }

  // Access area by its index
  int number_of_areas() const                               { return _num; }
  RootChunkArea* get_area_by_index(int index)               { assert(index >= 0 && index < _num, "oob"); return _arr + index; }
  const RootChunkArea* get_area_by_index(int index) const   { assert(index >= 0 && index < _num, "oob"); return _arr + index; }

  /// range ///

  const MetaWord* base() const  { return _base; }
  size_t word_size() const      { return _num * chunklevel::MAX_CHUNK_WORD_SIZE; }
  const MetaWord* end() const   { return _base + word_size(); }

  // Returns true if all areas in this area table are free (only contain free chunks).
  bool is_free() const;

  DEBUG_ONLY(void verify() const;)

  void print_on(outputStream* st) const;

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_ROOTCHUNKAREA_HPP
