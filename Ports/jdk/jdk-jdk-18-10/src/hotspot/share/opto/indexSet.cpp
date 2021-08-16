/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "opto/chaitin.hpp"
#include "opto/compile.hpp"
#include "opto/indexSet.hpp"
#include "opto/regmask.hpp"

// This file defines the IndexSet class, a set of sparse integer indices.
// This data structure is used by the compiler in its liveness analysis and
// during register allocation.  It also defines an iterator for this class.

//-------------------------------- Initializations ------------------------------

IndexSet::BitBlock  IndexSet::_empty_block     = IndexSet::BitBlock();

#ifdef ASSERT
// Initialize statistics counters
julong IndexSet::_alloc_new = 0;
julong IndexSet::_alloc_total = 0;

julong IndexSet::_total_bits = 0;
julong IndexSet::_total_used_blocks = 0;
julong IndexSet::_total_unused_blocks = 0;

// Per set, or all sets operation tracing
int IndexSet::_serial_count = 1;
#endif

//---------------------------- IndexSet::populate_free_list() -----------------------------
// Populate the free BitBlock list with a batch of BitBlocks.  The BitBlocks
// are 32 bit aligned.

void IndexSet::populate_free_list() {
  Compile *compile = Compile::current();
  BitBlock *free = (BitBlock*)compile->indexSet_free_block_list();

  char *mem = (char*)arena()->AmallocWords(sizeof(BitBlock) *
                                        bitblock_alloc_chunk_size + 32);

  // Align the pointer to a 32 bit boundary.
  BitBlock *new_blocks = (BitBlock*)(((uintptr_t)mem + 32) & ~0x001F);

  // Add the new blocks to the free list.
  for (int i = 0; i < bitblock_alloc_chunk_size; i++) {
    new_blocks->set_next(free);
    free = new_blocks;
    new_blocks++;
  }

  compile->set_indexSet_free_block_list(free);

#ifdef ASSERT
  if (CollectIndexSetStatistics) {
    inc_stat_counter(&_alloc_new, bitblock_alloc_chunk_size);
  }
#endif
}


//---------------------------- IndexSet::alloc_block() ------------------------
// Allocate a BitBlock from the free list.  If the free list is empty,
// prime it.

IndexSet::BitBlock *IndexSet::alloc_block() {
#ifdef ASSERT
  if (CollectIndexSetStatistics) {
    inc_stat_counter(&_alloc_total, 1);
  }
#endif
  Compile *compile = Compile::current();
  BitBlock* free_list = (BitBlock*)compile->indexSet_free_block_list();
  if (free_list == NULL) {
    populate_free_list();
    free_list = (BitBlock*)compile->indexSet_free_block_list();
  }
  BitBlock *block = free_list;
  compile->set_indexSet_free_block_list(block->next());

  block->clear();
  return block;
}

//---------------------------- IndexSet::alloc_block_containing() -------------
// Allocate a new BitBlock and put it into the position in the _blocks array
// corresponding to element.

IndexSet::BitBlock *IndexSet::alloc_block_containing(uint element) {
  BitBlock *block = alloc_block();
  uint bi = get_block_index(element);
  if (bi >= _current_block_limit) {
    _current_block_limit = bi + 1;
  }
  _blocks[bi] = block;
  return block;
}

//---------------------------- IndexSet::free_block() -------------------------
// Add a BitBlock to the free list.

void IndexSet::free_block(uint i) {
  debug_only(check_watch("free block", i));
  assert(i < _max_blocks, "block index too large");
  BitBlock *block = _blocks[i];
  assert(block != &_empty_block, "cannot free the empty block");
  block->set_next((IndexSet::BitBlock*)Compile::current()->indexSet_free_block_list());
  Compile::current()->set_indexSet_free_block_list(block);
  set_block(i, &_empty_block);
}

//------------------------------lrg_union--------------------------------------
// Compute the union of all elements of one and two which interfere with
// the RegMask mask.  If the degree of the union becomes exceeds
// fail_degree, the union bails out.  The underlying set is cleared before
// the union is performed.

uint IndexSet::lrg_union(uint lr1, uint lr2,
                         const uint fail_degree,
                         const PhaseIFG *ifg,
                         const RegMask &mask ) {
  IndexSet *one = ifg->neighbors(lr1);
  IndexSet *two = ifg->neighbors(lr2);
  LRG &lrg1 = ifg->lrgs(lr1);
  LRG &lrg2 = ifg->lrgs(lr2);
#ifdef ASSERT
  assert(_max_elements == one->_max_elements, "max element mismatch");
  check_watch("union destination");
  one->check_watch("union source");
  two->check_watch("union source");
#endif

  // Compute the degree of the combined live-range.  The combined
  // live-range has the union of the original live-ranges' neighbors set as
  // well as the neighbors of all intermediate copies, minus those neighbors
  // that can not use the intersected allowed-register-set.

  // Copy the larger set.  Insert the smaller set into the larger.
  if (two->count() > one->count()) {
    IndexSet *temp = one;
    one = two;
    two = temp;
  }

  clear();

  // Used to compute degree of register-only interferences.  Infinite-stack
  // neighbors do not alter colorability, as they can always color to some
  // other color.  (A variant of the Briggs assertion)
  uint reg_degree = 0;

  uint element = 0;
  // Load up the combined interference set with the neighbors of one
  if (!one->is_empty()) {
    IndexSetIterator elements(one);
    while ((element = elements.next()) != 0) {
      LRG &lrg = ifg->lrgs(element);
      if (mask.overlap(lrg.mask())) {
        insert(element);
        if (!lrg.mask().is_AllStack()) {
          reg_degree += lrg1.compute_degree(lrg);
          if (reg_degree >= fail_degree) return reg_degree;
        } else {
          // !!!!! Danger!  No update to reg_degree despite having a neighbor.
          // A variant of the Briggs assertion.
          // Not needed if I simplify during coalesce, ala George/Appel.
          assert(lrg.lo_degree(), "");
        }
      }
    }
  }
  // Add neighbors of two as well

  if (!two->is_empty()) {
    IndexSetIterator elements2(two);
    while ((element = elements2.next()) != 0) {
      LRG &lrg = ifg->lrgs(element);
      if (mask.overlap(lrg.mask())) {
        if (insert(element)) {
          if (!lrg.mask().is_AllStack()) {
            reg_degree += lrg2.compute_degree(lrg);
            if (reg_degree >= fail_degree) return reg_degree;
          } else {
            // !!!!! Danger!  No update to reg_degree despite having a neighbor.
            // A variant of the Briggs assertion.
            // Not needed if I simplify during coalesce, ala George/Appel.
            assert(lrg.lo_degree(), "");
          }
        }
      }
    }
  }

  return reg_degree;
}

//---------------------------- IndexSet() -----------------------------
// A deep copy constructor.  This is used when you need a scratch copy of this set.

IndexSet::IndexSet (IndexSet *set) {
#ifdef ASSERT
  _serial_number = _serial_count++;
  set->check_watch("copied", _serial_number);
  check_watch("initialized by copy", set->_serial_number);
  _max_elements = set->_max_elements;
#endif
  _count = set->_count;
  _current_block_limit = set->_current_block_limit;
  _max_blocks = set->_max_blocks;
  if (_max_blocks <= preallocated_block_list_size) {
    _blocks = _preallocated_block_list;
  } else {
    _blocks =
      (IndexSet::BitBlock**) arena()->AmallocWords(sizeof(IndexSet::BitBlock**) * _max_blocks);
  }
  for (uint i = 0; i < _max_blocks; i++) {
    BitBlock *block = set->_blocks[i];
    if (block == &_empty_block) {
      set_block(i, &_empty_block);
    } else {
      BitBlock *new_block = alloc_block();
      memcpy(new_block->words(), block->words(), sizeof(uintptr_t) * words_per_block);
      set_block(i, new_block);
    }
  }
}

//---------------------------- IndexSet::initialize() -----------------------------
// Prepare an IndexSet for use.

void IndexSet::initialize(uint max_elements) {
#ifdef ASSERT
  _serial_number = _serial_count++;
  check_watch("initialized", max_elements);
  _max_elements = max_elements;
#endif
  _count = 0;
  _current_block_limit = 0;
  _max_blocks = (max_elements + bits_per_block - 1) / bits_per_block;

  if (_max_blocks <= preallocated_block_list_size) {
    _blocks = _preallocated_block_list;
  } else {
    _blocks = (IndexSet::BitBlock**) arena()->AmallocWords(sizeof(IndexSet::BitBlock*) * _max_blocks);
  }
  for (uint i = 0; i < _max_blocks; i++) {
    set_block(i, &_empty_block);
  }
}

//---------------------------- IndexSet::initialize()------------------------------
// Prepare an IndexSet for use.  If it needs to allocate its _blocks array, it does
// so from the Arena passed as a parameter.  BitBlock allocation is still done from
// the static Arena which was set with reset_memory().

void IndexSet::initialize(uint max_elements, Arena *arena) {
#ifdef ASSERT
  _serial_number = _serial_count++;
  check_watch("initialized2", max_elements);
  _max_elements = max_elements;
#endif // ASSERT
  _count = 0;
  _current_block_limit = 0;
  _max_blocks = (max_elements + bits_per_block - 1) / bits_per_block;

  if (_max_blocks <= preallocated_block_list_size) {
    _blocks = _preallocated_block_list;
  } else {
    _blocks = (IndexSet::BitBlock**) arena->AmallocWords(sizeof(IndexSet::BitBlock*) * _max_blocks);
  }
  for (uint i = 0; i < _max_blocks; i++) {
    set_block(i, &_empty_block);
  }
}

//---------------------------- IndexSet::swap() -----------------------------
// Exchange two IndexSets.

void IndexSet::swap(IndexSet *set) {
#ifdef ASSERT
  assert(_max_elements == set->_max_elements, "must have same universe size to swap");
  check_watch("swap", set->_serial_number);
  set->check_watch("swap", _serial_number);
#endif

  uint max = MAX2(_current_block_limit, set->_current_block_limit);
  for (uint i = 0; i < max; i++) {
    BitBlock *temp = _blocks[i];
    set_block(i, set->_blocks[i]);
    set->set_block(i, temp);
  }
  uint temp = _count;
  _count = set->_count;
  set->_count = temp;

  temp = _current_block_limit;
  _current_block_limit = set->_current_block_limit;
  set->_current_block_limit = temp;

}

//---------------------------- IndexSet::dump() -----------------------------
// Print this set.  Used for debugging.

#ifndef PRODUCT
void IndexSet::dump() const {
  IndexSetIterator elements(this);

  tty->print("{");
  uint i;
  while ((i = elements.next()) != 0) {
    tty->print("L%d ", i);
  }
  tty->print_cr("}");
}
#endif

#ifdef ASSERT
//---------------------------- IndexSet::tally_iteration_statistics() -----------------------------
// Update block/bit counts to reflect that this set has been iterated over.

void IndexSet::tally_iteration_statistics() const {
  inc_stat_counter(&_total_bits, count());

  for (uint i = 0; i < _max_blocks; i++) {
    if (_blocks[i] != &_empty_block) {
      inc_stat_counter(&_total_used_blocks, 1);
    } else {
      inc_stat_counter(&_total_unused_blocks, 1);
    }
  }
}

//---------------------------- IndexSet::print_statistics() -----------------------------
// Print statistics about IndexSet usage.

void IndexSet::print_statistics() {
  julong total_blocks = _total_used_blocks + _total_unused_blocks;
  tty->print_cr ("Accumulated IndexSet usage statistics:");
  tty->print_cr ("--------------------------------------");
  tty->print_cr ("  Iteration:");
  tty->print_cr ("    blocks visited: " UINT64_FORMAT, total_blocks);
  tty->print_cr ("    blocks empty: %4.2f%%", 100.0*(double)_total_unused_blocks/total_blocks);
  tty->print_cr ("    bit density (bits/used blocks): %4.2f", (double)_total_bits/_total_used_blocks);
  tty->print_cr ("    bit density (bits/all blocks): %4.2f", (double)_total_bits/total_blocks);
  tty->print_cr ("  Allocation:");
  tty->print_cr ("    blocks allocated: " UINT64_FORMAT, _alloc_new);
  tty->print_cr ("    blocks used/reused: " UINT64_FORMAT, _alloc_total);
}

//---------------------------- IndexSet::verify() -----------------------------
// Expensive test of IndexSet sanity.  Ensure that the count agrees with the
// number of bits in the blocks.  Make sure the iterator is seeing all elements
// of the set.  Meant for use during development.

void IndexSet::verify() const {
  assert(!member(0), "zero cannot be a member");
  uint count = 0;
  uint i;
  for (i = 1; i < _max_elements; i++) {
    if (member(i)) {
      count++;
      assert(count <= _count, "_count is messed up");
    }
  }

  IndexSetIterator elements(this);
  count = 0;
  while ((i = elements.next()) != 0) {
    count++;
    assert(member(i), "returned a non member");
    assert(count <= _count, "iterator returned wrong number of elements");
  }
}
#endif

//---------------------------- IndexSetIterator() -----------------------------
// Create an iterator for a set.  If empty blocks are detected when iterating
// over the set, these blocks are replaced.

//---------------------------- List16Iterator::advance_and_next() -----------------------------
// Advance to the next non-empty word in the set being iterated over.  Return the next element
// if there is one.  If we are done, return 0.  This method is called from the next() method
// when it gets done with a word.

uint IndexSetIterator::advance_and_next() {
  // See if there is another non-empty word in the current block.
  for (uint wi = _next_word; wi < (unsigned)IndexSet::words_per_block; wi++) {
    if (_words[wi] != 0) {
      // Found a non-empty word.
      _value = ((_next_block - 1) * IndexSet::bits_per_block) + (wi * IndexSet::bits_per_word);
      _current = _words[wi];
      _next_word = wi + 1;
      return next_value();
    }
  }

  // We ran out of words in the current block.  Advance to next non-empty block.
  for (uint bi = _next_block; bi < _max_blocks; bi++) {
    if (_blocks[bi] != &IndexSet::_empty_block) {
      // Found a non-empty block.

      _words = _blocks[bi]->words();
      for (uint wi = 0; wi < (unsigned)IndexSet::words_per_block; wi++) {
        if (_words[wi] != 0) {
          // Found a non-empty word.
          _value = (bi * IndexSet::bits_per_block) + (wi * IndexSet::bits_per_word);
          _current = _words[wi];

          _next_block = bi+1;
          _next_word = wi+1;
          return next_value();
        }
      }

      // All of the words in the block were empty.  Replace
      // the block with the empty block.
      if (_set) {
        _set->free_block(bi);
      }
    }
  }

  // No more words.
  return 0;
}
