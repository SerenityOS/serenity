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

#ifndef SHARE_MEMORY_METASPACE_VIRTUALSPACELIST_HPP
#define SHARE_MEMORY_METASPACE_VIRTUALSPACELIST_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/virtualSpaceNode.hpp"
#include "memory/virtualspace.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

class Metachunk;
class FreeChunkListVector;

// VirtualSpaceList manages a single (if its non-expandable) or
//  a series of (if its expandable) virtual memory regions used
//  for metaspace.
//
// Internally it holds a list of nodes (VirtualSpaceNode) each
//  managing a single contiguous memory region. The first node of
//  this list is the current node and used for allocation of new
//  root chunks.
//
// Beyond access to those nodes and the ability to grow new nodes
//  (if expandable) it allows for purging: purging this list means
//  removing and unmapping all memory regions which are unused.

class VirtualSpaceList : public CHeapObj<mtClass> {

  // Name
  const char* const _name;

  // Head of the list.
  VirtualSpaceNode* _first_node;

  // Number of nodes (kept for statistics only).
  IntCounter _nodes_counter;

  // Whether this list can expand by allocating new nodes.
  const bool _can_expand;

  // Used to check limits before committing memory.
  CommitLimiter* const _commit_limiter;

  // Statistics

  // Holds sum of reserved space, in words, over all list nodes.
  SizeCounter _reserved_words_counter;

  // Holds sum of committed space, in words, over all list nodes.
  SizeCounter _committed_words_counter;

  // Create a new node and append it to the list. After
  // this function, _current_node shall point to a new empty node.
  // List must be expandable for this to work.
  void create_new_node();

public:

  // Create a new, empty, expandable list.
  VirtualSpaceList(const char* name, CommitLimiter* commit_limiter);

  // Create a new list. The list will contain one node only, which uses the given ReservedSpace.
  // It will be not expandable beyond that first node.
  VirtualSpaceList(const char* name, ReservedSpace rs, CommitLimiter* commit_limiter);

  virtual ~VirtualSpaceList();

  // Allocate a root chunk from this list.
  // Note: this just returns a chunk whose memory is reserved; no memory is committed yet.
  // Hence, before using this chunk, it must be committed.
  // May return NULL if vslist would need to be expanded to hold the new root node but
  // the list cannot be expanded (in practice this means we reached CompressedClassSpaceSize).
  Metachunk* allocate_root_chunk();

  // Attempts to purge nodes. This will remove and delete nodes which only contain free chunks.
  // The free chunks are removed from the freelists before the nodes are deleted.
  // Return number of purged nodes.
  int purge(FreeChunkListVector* freelists);

  //// Statistics ////

  // Return sum of reserved words in all nodes.
  size_t reserved_words() const     { return _reserved_words_counter.get(); }

  // Return sum of committed words in all nodes.
  size_t committed_words() const    { return _committed_words_counter.get(); }

  // Return number of nodes in this list.
  int num_nodes() const             { return _nodes_counter.get(); }

  //// Debug stuff ////
  DEBUG_ONLY(void verify() const;)
  DEBUG_ONLY(void verify_locked() const;)

  // Print all nodes in this space list.
  void print_on(outputStream* st) const;

  // Returns true if this pointer is contained in one of our nodes.
  bool contains(const MetaWord* p) const;

  // Returns true if the list is not expandable and no more root chunks
  // can be allocated.
  bool is_full() const;

  // Convenience methods to return the global class-space vslist
  //  and non-class vslist, respectively.
  static VirtualSpaceList* vslist_class();
  static VirtualSpaceList* vslist_nonclass();

  // These exist purely to print limits of the compressed class space;
  // if we ever change the ccs to not use a degenerated-list-of-one-node this
  // will go away.
  MetaWord* base_of_first_node() const { return _first_node != NULL ? _first_node->base() : NULL; }
  size_t word_size_of_first_node() const { return _first_node != NULL ? _first_node->word_size() : 0; }

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_VIRTUALSPACELIST_HPP
