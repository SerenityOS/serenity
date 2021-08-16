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

#ifndef SHARE_MEMORY_METASPACE_FREEBLOCKS_HPP
#define SHARE_MEMORY_METASPACE_FREEBLOCKS_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/binList.hpp"
#include "memory/metaspace/blockTree.hpp"
#include "memory/metaspace/counters.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

// Class FreeBlocks manages deallocated blocks in Metaspace.
//
// In Metaspace, allocated memory blocks may be release prematurely. This is
//  uncommon (otherwise an arena-based allocation scheme would not make sense).
//  It can happen e.g. when class loading fails or when bytecode gets rewritten.
//
// All these released blocks should be reused, so they are collected. Since these
//  blocks are embedded into chunks which are still in use by a live arena,
//  we cannot just give these blocks to anyone; only the owner of this arena can
//  reuse these blocks. Therefore these blocks are kept at arena-level.
//
// The structure to manage these released blocks at arena level is class FreeBlocks.
//
// FreeBlocks is optimized toward the typical size and number of deallocated
//  blocks. The vast majority of them (about 90%) are below 16 words in size,
//  but there is a significant portion of memory blocks much larger than that,
//  leftover space from retired chunks, see MetaspaceArena::retire_current_chunk().
//
// Since the vast majority of blocks are small or very small, FreeBlocks consists
//  internally of two separate structures to keep very small blocks and other blocks.
//  Very small blocks are kept in a bin list (see binlist.hpp) and larger blocks in
//  a BST (see blocktree.hpp).

class FreeBlocks : public CHeapObj<mtMetaspace> {

  // _small_blocks takes care of small to very small blocks.
  BinList32 _small_blocks;

  // A BST for larger blocks, only for blocks which are too large
  // to fit into _smallblocks.
  BlockTree _tree;

  // This verifies that blocks too large to go into the binlist can be
  // kept in the blocktree.
  STATIC_ASSERT(BinList32::MaxWordSize >= BlockTree::MinWordSize);

  // Cutoff point: blocks larger than this size are kept in the
  // tree, blocks smaller than or equal to this size in the bin list.
  const size_t MaxSmallBlocksWordSize = BinList32::MaxWordSize;

public:

  // Smallest blocks we can keep in this structure.
  const static size_t MinWordSize = BinList32::MinWordSize;

  // Add a block to the deallocation management.
  void add_block(MetaWord* p, size_t word_size);

  // Retrieve a block of at least requested_word_size.
  MetaWord* remove_block(size_t requested_word_size);

#ifdef ASSERT
  void verify() const {
    _tree.verify();
    _small_blocks.verify();
  };
#endif

  // Returns number of blocks.
  int count() const {
    return _small_blocks.count() + _tree.count();
  }

  // Returns total size, in words, of all elements.
  size_t total_size() const {
    return _small_blocks.total_size() + _tree.total_size();
  }

  // Returns true if empty.
  bool is_empty() const {
    return _small_blocks.is_empty() && _tree.is_empty();
  }

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_FREEBLOCKS_HPP
