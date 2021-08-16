/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_INDEXSET_HPP
#define SHARE_OPTO_INDEXSET_HPP

#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "opto/compile.hpp"
#include "opto/regmask.hpp"
#include "utilities/count_trailing_zeros.hpp"

// This file defines the IndexSet class, a set of sparse integer indices.
// This data structure is used by the compiler in its liveness analysis and
// during register allocation.

//-------------------------------- class IndexSet ----------------------------
// An IndexSet is a piece-wise bitvector.  At the top level, we have an array
// of pointers to bitvector chunks called BitBlocks.  Each BitBlock has a fixed
// size and is allocated from a shared free list.  The bits which are set in
// each BitBlock correspond to the elements of the set.

class IndexSet : public ResourceObj {
 friend class IndexSetIterator;

 public:
  // When we allocate an IndexSet, it starts off with an array of top level block
  // pointers of a set length.  This size is intended to be large enough for the
  // majority of IndexSets.  In the cases when this size is not large enough,
  // a separately allocated array is used.

  // The length of the preallocated top level block array
  enum { preallocated_block_list_size = 16 };

  // Elements of a IndexSet get decomposed into three fields.  The highest order
  // bits are the block index, which tell which high level block holds the element.
  // Within that block, the word index indicates which word holds the element.
  // Finally, the bit index determines which single bit within that word indicates
  // membership of the element in the set.

  // The lengths of the index bitfields
  enum {
         // Each block consists of 256 bits
         block_index_length = 8,
         // Split over 4 or 8 words depending on bitness
         word_index_length  = block_index_length - LogBitsPerWord,
         bit_index_length   = block_index_length - word_index_length,
  };

  // Derived constants used for manipulating the index bitfields
  enum {
         bit_index_offset = 0, // not used
         word_index_offset = bit_index_length,
         block_index_offset = bit_index_length + word_index_length,

         bits_per_word = 1 << bit_index_length,
         words_per_block = 1 << word_index_length,
         bits_per_block = bits_per_word * words_per_block,

         bit_index_mask = right_n_bits(bit_index_length),
         word_index_mask = right_n_bits(word_index_length)
  };

  // These routines are used for extracting the block, word, and bit index
  // from an element.
  static uint get_block_index(uint element) {
    return element >> block_index_offset;
  }
  static uint get_word_index(uint element) {
    return mask_bits(element >> word_index_offset,word_index_mask);
  }
  static uint get_bit_index(uint element) {
    return mask_bits(element, bit_index_mask);
  }

  //------------------------------ class BitBlock ----------------------------
  // The BitBlock class is a segment of a bitvector set.

  class BitBlock : public ResourceObj {
   friend class IndexSetIterator;
   friend class IndexSet;

   private:
    // All of BitBlocks fields and methods are declared private.  We limit
    // access to IndexSet and IndexSetIterator.

    // A BitBlock is composed of some number of 32- or 64-bit words.  When a BitBlock
    // is not in use by any IndexSet, it is stored on a free list.  The next field
    // is used by IndexSet to maintain this free list.

    union {
      uintptr_t _words[words_per_block];
      BitBlock *_next;
    } _data;

    // accessors
    uintptr_t* words() { return _data._words; }
    void set_next(BitBlock *next) { _data._next = next; }
    BitBlock *next() { return _data._next; }

    // Operations.  A BitBlock supports four simple operations,
    // clear(), member(), insert(), and remove().  These methods do
    // not assume that the block index has been masked out.

    void clear() {
      memset(words(), 0, sizeof(uintptr_t) * words_per_block);
    }

    bool member(uint element) {
      uint word_index = IndexSet::get_word_index(element);
      uintptr_t bit_index = IndexSet::get_bit_index(element);

      return ((words()[word_index] & (uintptr_t(1) << bit_index)) != 0);
    }

    bool insert(uint element) {
      uint word_index = IndexSet::get_word_index(element);
      uintptr_t bit_index = IndexSet::get_bit_index(element);

      uintptr_t bit = uintptr_t(1) << bit_index;
      uintptr_t before = words()[word_index];
      words()[word_index] = before | bit;
      return ((before & bit) != 0);
    }

    bool remove(uint element) {
      uint word_index = IndexSet::get_word_index(element);
      uintptr_t bit_index = IndexSet::get_bit_index(element);

      uintptr_t bit = uintptr_t(1) << bit_index;
      uintptr_t before = words()[word_index];
      words()[word_index] = before & ~bit;
      return ((before & bit) != 0);
    }
  };

  //-------------------------- BitBlock allocation ---------------------------
 private:

  // All IndexSets share an arena from which they allocate BitBlocks.  Unused
  // BitBlocks are placed on a free list.

  // The number of BitBlocks to allocate at a time
  enum { bitblock_alloc_chunk_size = 50 };

  static Arena *arena() { return Compile::current()->indexSet_arena(); }

  static void populate_free_list();

 public:

  // Invalidate the current free BitBlock list and begin allocation
  // from a new arena.  It is essential that this method is called whenever
  // the Arena being used for BitBlock allocation is reset.
  static void reset_memory(Compile* compile, Arena *arena) {
    compile->set_indexSet_free_block_list(NULL);
    compile->set_indexSet_arena(arena);

   // This should probably be done in a static initializer
   _empty_block.clear();
  }

 private:
  friend class BitBlock;
  // A distinguished BitBlock which always remains empty.  When a new IndexSet is
  // created, all of its top level BitBlock pointers are initialized to point to
  // this.
  static BitBlock _empty_block;

  //-------------------------- Members ------------------------------------------

  // The number of elements in the set
  uint      _count;

  // The current upper limit of blocks that has been allocated and might be in use
  uint      _current_block_limit;

  // Our top level array of bitvector segments
  BitBlock **_blocks;

  BitBlock  *_preallocated_block_list[preallocated_block_list_size];

  // The number of top level array entries in use
  uint       _max_blocks;

  // Our assertions need to know the maximum number allowed in the set
#ifdef ASSERT
  uint       _max_elements;
#endif

  // The next IndexSet on the free list (not used at same time as count)
  IndexSet *_next;

 public:
  //-------------------------- Free list operations ------------------------------
  // Individual IndexSets can be placed on a free list.  This is done in PhaseLive.

  IndexSet *next() {
    return _next;
  }

  void set_next(IndexSet *next) {
    _next = next;
  }

 private:
  //-------------------------- Utility methods -----------------------------------

  // Get the block which holds element
  BitBlock *get_block_containing(uint element) const {
    assert(element < _max_elements, "element out of bounds");
    return _blocks[get_block_index(element)];
  }

  // Set a block in the top level array
  void set_block(uint index, BitBlock *block) {
    _blocks[index] = block;
  }

  // Get a BitBlock from the free list
  BitBlock *alloc_block();

  // Get a BitBlock from the free list and place it in the top level array
  BitBlock *alloc_block_containing(uint element);

  // Free a block from the top level array, placing it on the free BitBlock list
  void free_block(uint i);

 public:
  //-------------------------- Primitive set operations --------------------------

  void clear() {
    _count = 0;
    for (uint i = 0; i < _current_block_limit; i++) {
      BitBlock *block = _blocks[i];
      if (block != &_empty_block) {
        free_block(i);
      }
    }
    _current_block_limit = 0;
  }

  uint count() const { return _count; }

  bool is_empty() const { return _count == 0; }

  bool member(uint element) const {
    return get_block_containing(element)->member(element);
  }

  bool insert(uint element) {
    if (element == 0) {
      return 0;
    }
    BitBlock *block = get_block_containing(element);
    if (block == &_empty_block) {
      block = alloc_block_containing(element);
    }
    bool present = block->insert(element);
    if (!present) {
      _count++;
    }
    return !present;
  }

  bool remove(uint element) {
    BitBlock *block = get_block_containing(element);
    bool present = block->remove(element);
    if (present) {
      _count--;
    }
    return present;
  }

  //-------------------------- Compound set operations ------------------------
  // Compute the union of all elements of one and two which interfere
  // with the RegMask mask.  If the degree of the union becomes
  // exceeds fail_degree, the union bails out.  The underlying set is
  // cleared before the union is performed.
  uint lrg_union(uint lr1, uint lr2,
                 const uint fail_degree,
                 const class PhaseIFG *ifg,
                 const RegMask &mask);


  //------------------------- Construction, initialization -----------------------

  IndexSet() {}

  // This constructor is used for making a deep copy of a IndexSet.
  IndexSet(IndexSet *set);

  // Perform initialization on a IndexSet
  void initialize(uint max_element);

  // Initialize a IndexSet.  If the top level BitBlock array needs to be
  // allocated, do it from the proffered arena.  BitBlocks are still allocated
  // from the static Arena member.
  void initialize(uint max_element, Arena *arena);

  // Exchange two sets
  void swap(IndexSet *set);

  //-------------------------- Debugging and statistics --------------------------

#ifndef PRODUCT
  // Output a IndexSet for debugging
  void dump() const;
#endif

#ifdef ASSERT
  void tally_iteration_statistics() const;

  // BitBlock allocation statistics
  static julong _alloc_new;
  static julong _alloc_total;

  // Block density statistics
  static julong _total_bits;
  static julong _total_used_blocks;
  static julong _total_unused_blocks;

  // Sanity tests
  void verify() const;

  static int _serial_count;
  int        _serial_number;

  // Check to see if the serial number of the current set is the one we're tracing.
  // If it is, print a message.
  void check_watch(const char *operation, uint operand) const {
    if (IndexSetWatch != 0) {
      if (IndexSetWatch == -1 || _serial_number == IndexSetWatch) {
        tty->print_cr("IndexSet %d : %s ( %d )", _serial_number, operation, operand);
      }
    }
  }
  void check_watch(const char *operation) const {
    if (IndexSetWatch != 0) {
      if (IndexSetWatch == -1 || _serial_number == IndexSetWatch) {
        tty->print_cr("IndexSet %d : %s", _serial_number, operation);
      }
    }
  }

 public:
  static void print_statistics();

#endif
};


//-------------------------------- class IndexSetIterator --------------------
// An iterator for IndexSets.

class IndexSetIterator {
 friend class IndexSet;

 private:
  // The current word we are inspecting
  uintptr_t             _current;

  // What element number are we currently on?
  uint                  _value;

  // The index of the next word we will inspect
  uint                  _next_word;

  // The index of the next block we will inspect
  uint                  _next_block;

  // The number of blocks in the set
  uint                  _max_blocks;

  // A pointer to the contents of the current block
  uintptr_t*            _words;

  // A pointer to the blocks in our set
  IndexSet::BitBlock **_blocks;

  // If the iterator was created from a non-const set, we replace
  // non-canonical empty blocks with the _empty_block pointer.  If
  // _set is NULL, we do no replacement.
  IndexSet            *_set;

  // Advance to the next non-empty word and return the next
  // element in the set.
  uint advance_and_next();

 public:

  // If an iterator is built from a constant set then empty blocks
  // are not canonicalized.
  IndexSetIterator(IndexSet *set) :
    _current(0),
    _value(0),
    _next_word(IndexSet::words_per_block),
    _next_block(0),
    _max_blocks(set->is_empty() ? 0 : set->_current_block_limit),
    _words(NULL),
    _blocks(set->_blocks),
    _set(set) {
  #ifdef ASSERT
    if (CollectIndexSetStatistics) {
      set->tally_iteration_statistics();
    }
    set->check_watch("traversed", set->count());
  #endif
  }

  IndexSetIterator(const IndexSet *set) :
    _current(0),
    _value(0),
    _next_word(IndexSet::words_per_block),
    _next_block(0),
    _max_blocks(set->is_empty() ? 0 : set->_current_block_limit),
    _words(NULL),
    _blocks(set->_blocks),
    _set(NULL)
  {
  #ifdef ASSERT
    if (CollectIndexSetStatistics) {
      set->tally_iteration_statistics();
    }
    // We don't call check_watch from here to avoid bad recursion.
    //   set->check_watch("traversed const", set->count());
  #endif
  }

  // Return the next element of the set.
  uint next_value() {
    uintptr_t current = _current;
    assert(current != 0, "sanity");
    uint advance = count_trailing_zeros(current);
    assert(((current >> advance) & 0x1) == 1, "sanity");
    _current = (current >> advance) - 1;
    _value += advance;
    return _value;
  }

  // Return the next element of the set.  Return 0 when done.
  uint next() {
    if (_current != 0) {
      return next_value();
    } else if (_next_word < IndexSet::words_per_block || _next_block < _max_blocks) {
      return advance_and_next();
    } else {
      return 0;
    }
  }

};

#endif // SHARE_OPTO_INDEXSET_HPP
