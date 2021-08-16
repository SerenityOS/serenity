/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_BLOCKOFFSETTABLE_HPP
#define SHARE_GC_SHARED_BLOCKOFFSETTABLE_HPP

#include "gc/shared/gc_globals.hpp"
#include "gc/shared/memset_with_concurrent_readers.hpp"
#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "memory/virtualspace.hpp"
#include "runtime/globals.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

// The CollectedHeap type requires subtypes to implement a method
// "block_start".  For some subtypes, notably generational
// systems using card-table-based write barriers, the efficiency of this
// operation may be important.  Implementations of the "BlockOffsetArray"
// class may be useful in providing such efficient implementations.
//
// BlockOffsetTable (abstract)
//   - BlockOffsetArray (abstract)
//     - BlockOffsetArrayContigSpace
//

class ContiguousSpace;

class BOTConstants : public AllStatic {
public:
  static const uint LogN = 9;
  static const uint LogN_words = LogN - LogHeapWordSize;
  static const uint N_bytes = 1 << LogN;
  static const uint N_words = 1 << LogN_words;
  // entries "e" of at least N_words mean "go back by Base^(e-N_words)."
  // All entries are less than "N_words + N_powers".
  static const uint LogBase = 4;
  static const uint Base = (1 << LogBase);
  static const uint N_powers = 14;

  static size_t power_to_cards_back(uint i) {
    return (size_t)1 << (LogBase * i);
  }
  static size_t power_to_words_back(uint i) {
    return power_to_cards_back(i) * N_words;
  }
  static size_t entry_to_cards_back(u_char entry) {
    assert(entry >= N_words, "Precondition");
    return power_to_cards_back(entry - N_words);
  }
  static size_t entry_to_words_back(u_char entry) {
    assert(entry >= N_words, "Precondition");
    return power_to_words_back(entry - N_words);
  }
};

//////////////////////////////////////////////////////////////////////////
// The BlockOffsetTable "interface"
//////////////////////////////////////////////////////////////////////////
class BlockOffsetTable {
  friend class VMStructs;
protected:
  // These members describe the region covered by the table.

  // The space this table is covering.
  HeapWord* _bottom;    // == reserved.start
  HeapWord* _end;       // End of currently allocated region.

public:
  // Initialize the table to cover the given space.
  // The contents of the initial table are undefined.
  BlockOffsetTable(HeapWord* bottom, HeapWord* end):
    _bottom(bottom), _end(end) {
    assert(_bottom <= _end, "arguments out of order");
  }

  // Note that the committed size of the covered space may have changed,
  // so the table size might also wish to change.
  virtual void resize(size_t new_word_size) = 0;

  virtual void set_bottom(HeapWord* new_bottom) {
    assert(new_bottom <= _end, "new_bottom > _end");
    _bottom = new_bottom;
    resize(pointer_delta(_end, _bottom));
  }

  // Requires "addr" to be contained by a block, and returns the address of
  // the start of that block.
  virtual HeapWord* block_start_unsafe(const void* addr) const = 0;

  // Returns the address of the start of the block containing "addr", or
  // else "null" if it is covered by no block.
  HeapWord* block_start(const void* addr) const;
};

//////////////////////////////////////////////////////////////////////////
// One implementation of "BlockOffsetTable," the BlockOffsetArray,
// divides the covered region into "N"-word subregions (where
// "N" = 2^"LogN".  An array with an entry for each such subregion
// indicates how far back one must go to find the start of the
// chunk that includes the first word of the subregion.
//
// Each BlockOffsetArray is owned by a Space.  However, the actual array
// may be shared by several BlockOffsetArrays; this is useful
// when a single resizable area (such as a generation) is divided up into
// several spaces in which contiguous allocation takes place.  (Consider,
// for example, the garbage-first generation.)

// Here is the shared array type.
//////////////////////////////////////////////////////////////////////////
// BlockOffsetSharedArray
//////////////////////////////////////////////////////////////////////////
class BlockOffsetSharedArray: public CHeapObj<mtGC> {
  friend class BlockOffsetArray;
  friend class BlockOffsetArrayNonContigSpace;
  friend class BlockOffsetArrayContigSpace;
  friend class VMStructs;

 private:
  bool _init_to_zero;

  // The reserved region covered by the shared array.
  MemRegion _reserved;

  // End of the current committed region.
  HeapWord* _end;

  // Array for keeping offsets for retrieving object start fast given an
  // address.
  VirtualSpace _vs;
  u_char* _offset_array;          // byte array keeping backwards offsets

  void fill_range(size_t start, size_t num_cards, u_char offset) {
    void* start_ptr = &_offset_array[start];
    // If collector is concurrent, special handling may be needed.
    G1GC_ONLY(assert(!UseG1GC, "Shouldn't be here when using G1");)
    memset(start_ptr, offset, num_cards);
  }

 protected:
  // Bounds checking accessors:
  // For performance these have to devolve to array accesses in product builds.
  u_char offset_array(size_t index) const {
    assert(index < _vs.committed_size(), "index out of range");
    return _offset_array[index];
  }
  // An assertion-checking helper method for the set_offset_array() methods below.
  void check_reducing_assertion(bool reducing);

  void set_offset_array(size_t index, u_char offset, bool reducing = false) {
    check_reducing_assertion(reducing);
    assert(index < _vs.committed_size(), "index out of range");
    assert(!reducing || _offset_array[index] >= offset, "Not reducing");
    _offset_array[index] = offset;
  }

  void set_offset_array(size_t index, HeapWord* high, HeapWord* low, bool reducing = false) {
    check_reducing_assertion(reducing);
    assert(index < _vs.committed_size(), "index out of range");
    assert(high >= low, "addresses out of order");
    assert(pointer_delta(high, low) <= BOTConstants::N_words, "offset too large");
    assert(!reducing || _offset_array[index] >=  (u_char)pointer_delta(high, low),
           "Not reducing");
    _offset_array[index] = (u_char)pointer_delta(high, low);
  }

  void set_offset_array(HeapWord* left, HeapWord* right, u_char offset, bool reducing = false) {
    check_reducing_assertion(reducing);
    assert(index_for(right - 1) < _vs.committed_size(),
           "right address out of range");
    assert(left  < right, "Heap addresses out of order");
    size_t num_cards = pointer_delta(right, left) >> BOTConstants::LogN_words;

    fill_range(index_for(left), num_cards, offset);
  }

  void set_offset_array(size_t left, size_t right, u_char offset, bool reducing = false) {
    check_reducing_assertion(reducing);
    assert(right < _vs.committed_size(), "right address out of range");
    assert(left  <= right, "indexes out of order");
    size_t num_cards = right - left + 1;

    fill_range(left, num_cards, offset);
  }

  void check_offset_array(size_t index, HeapWord* high, HeapWord* low) const {
    assert(index < _vs.committed_size(), "index out of range");
    assert(high >= low, "addresses out of order");
    assert(pointer_delta(high, low) <= BOTConstants::N_words, "offset too large");
    assert(_offset_array[index] == pointer_delta(high, low),
           "Wrong offset");
  }

  bool is_card_boundary(HeapWord* p) const;

  // Return the number of slots needed for an offset array
  // that covers mem_region_words words.
  // We always add an extra slot because if an object
  // ends on a card boundary we put a 0 in the next
  // offset array slot, so we want that slot always
  // to be reserved.

  size_t compute_size(size_t mem_region_words) {
    size_t number_of_slots = (mem_region_words / BOTConstants::N_words) + 1;
    return ReservedSpace::allocation_align_size_up(number_of_slots);
  }

public:
  // Initialize the table to cover from "base" to (at least)
  // "base + init_word_size".  In the future, the table may be expanded
  // (see "resize" below) up to the size of "_reserved" (which must be at
  // least "init_word_size".)  The contents of the initial table are
  // undefined; it is the responsibility of the constituent
  // BlockOffsetTable(s) to initialize cards.
  BlockOffsetSharedArray(MemRegion reserved, size_t init_word_size);

  // Notes a change in the committed size of the region covered by the
  // table.  The "new_word_size" may not be larger than the size of the
  // reserved region this table covers.
  void resize(size_t new_word_size);

  void set_bottom(HeapWord* new_bottom);

  // Whether entries should be initialized to zero. Used currently only for
  // error checking.
  void set_init_to_zero(bool val) { _init_to_zero = val; }
  bool init_to_zero() { return _init_to_zero; }

  // Updates all the BlockOffsetArray's sharing this shared array to
  // reflect the current "top"'s of their spaces.
  void update_offset_arrays();   // Not yet implemented!

  // Return the appropriate index into "_offset_array" for "p".
  size_t index_for(const void* p) const;

  // Return the address indicating the start of the region corresponding to
  // "index" in "_offset_array".
  HeapWord* address_for_index(size_t index) const;
};

class Space;

//////////////////////////////////////////////////////////////////////////
// The BlockOffsetArray whose subtypes use the BlockOffsetSharedArray.
//////////////////////////////////////////////////////////////////////////
class BlockOffsetArray: public BlockOffsetTable {
  friend class VMStructs;
 protected:
  // The following enums are used by do_block_internal() below
  enum Action {
    Action_single,      // BOT records a single block (see single_block())
    Action_mark,        // BOT marks the start of a block (see mark_block())
    Action_check        // Check that BOT records block correctly
                        // (see verify_single_block()).
  };

  // The shared array, which is shared with other BlockOffsetArray's
  // corresponding to different spaces within a generation or span of
  // memory.
  BlockOffsetSharedArray* _array;

  // The space that owns this subregion.
  Space* _sp;

  // If true, array entries are initialized to 0; otherwise, they are
  // initialized to point backwards to the beginning of the covered region.
  bool _init_to_zero;

  // An assertion-checking helper method for the set_remainder*() methods below.
  void check_reducing_assertion(bool reducing) { _array->check_reducing_assertion(reducing); }

  // Sets the entries
  // corresponding to the cards starting at "start" and ending at "end"
  // to point back to the card before "start": the interval [start, end)
  // is right-open. The last parameter, reducing, indicates whether the
  // updates to individual entries always reduce the entry from a higher
  // to a lower value. (For example this would hold true during a temporal
  // regime during which only block splits were updating the BOT.
  void set_remainder_to_point_to_start(HeapWord* start, HeapWord* end, bool reducing = false);
  // Same as above, except that the args here are a card _index_ interval
  // that is closed: [start_index, end_index]
  void set_remainder_to_point_to_start_incl(size_t start, size_t end, bool reducing = false);

  // A helper function for BOT adjustment/verification work
  void do_block_internal(HeapWord* blk_start, HeapWord* blk_end, Action action, bool reducing = false);

 public:
  // The space may not have its bottom and top set yet, which is why the
  // region is passed as a parameter.  If "init_to_zero" is true, the
  // elements of the array are initialized to zero.  Otherwise, they are
  // initialized to point backwards to the beginning.
  BlockOffsetArray(BlockOffsetSharedArray* array, MemRegion mr,
                   bool init_to_zero_);

  // Note: this ought to be part of the constructor, but that would require
  // "this" to be passed as a parameter to a member constructor for
  // the containing concrete subtype of Space.
  // This would be legal C++, but MS VC++ doesn't allow it.
  void set_space(Space* sp) { _sp = sp; }

  // Resets the covered region to the given "mr".
  void set_region(MemRegion mr) {
    _bottom = mr.start();
    _end = mr.end();
  }

  // Note that the committed size of the covered space may have changed,
  // so the table size might also wish to change.
  virtual void resize(size_t new_word_size) {
    HeapWord* new_end = _bottom + new_word_size;
    if (_end < new_end && !init_to_zero()) {
      // verify that the old and new boundaries are also card boundaries
      assert(_array->is_card_boundary(_end),
             "_end not a card boundary");
      assert(_array->is_card_boundary(new_end),
             "new _end would not be a card boundary");
      // set all the newly added cards
      _array->set_offset_array(_end, new_end, BOTConstants::N_words);
    }
    _end = new_end;  // update _end
  }

  // Adjust the BOT to show that it has a single block in the
  // range [blk_start, blk_start + size). All necessary BOT
  // cards are adjusted, but _unallocated_block isn't.
  void single_block(HeapWord* blk_start, HeapWord* blk_end);
  void single_block(HeapWord* blk, size_t size) {
    single_block(blk, blk + size);
  }

  // When the alloc_block() call returns, the block offset table should
  // have enough information such that any subsequent block_start() call
  // with an argument equal to an address that is within the range
  // [blk_start, blk_end) would return the value blk_start, provided
  // there have been no calls in between that reset this information
  // (e.g. see BlockOffsetArrayNonContigSpace::single_block() call
  // for an appropriate range covering the said interval).
  // These methods expect to be called with [blk_start, blk_end)
  // representing a block of memory in the heap.
  virtual void alloc_block(HeapWord* blk_start, HeapWord* blk_end);
  void alloc_block(HeapWord* blk, size_t size) {
    alloc_block(blk, blk + size);
  }

  // If true, initialize array slots with no allocated blocks to zero.
  // Otherwise, make them point back to the front.
  bool init_to_zero() { return _init_to_zero; }
  // Corresponding setter
  void set_init_to_zero(bool val) {
    _init_to_zero = val;
    assert(_array != NULL, "_array should be non-NULL");
    _array->set_init_to_zero(val);
  }

  // Debugging
  // Return the index of the last entry in the "active" region.
  virtual size_t last_active_index() const = 0;
  // Verify the block offset table
  void verify() const;
  void check_all_cards(size_t left_card, size_t right_card) const;
};

////////////////////////////////////////////////////////////////////////////
// A subtype of BlockOffsetArray that takes advantage of the fact
// that its underlying space is a ContiguousSpace, so that its "active"
// region can be more efficiently tracked (than for a non-contiguous space).
////////////////////////////////////////////////////////////////////////////
class BlockOffsetArrayContigSpace: public BlockOffsetArray {
  friend class VMStructs;
 private:
  // allocation boundary at which offset array must be updated
  HeapWord* _next_offset_threshold;
  size_t    _next_offset_index;      // index corresponding to that boundary

  // Work function when allocation start crosses threshold.
  void alloc_block_work(HeapWord* blk_start, HeapWord* blk_end);

 public:
  BlockOffsetArrayContigSpace(BlockOffsetSharedArray* array, MemRegion mr):
    BlockOffsetArray(array, mr, true) {
    _next_offset_threshold = NULL;
    _next_offset_index = 0;
  }

  void set_contig_space(ContiguousSpace* sp) { set_space((Space*)sp); }

  // Initialize the threshold for an empty heap.
  HeapWord* initialize_threshold();
  // Zero out the entry for _bottom (offset will be zero)
  void      zero_bottom_entry();

  // Return the next threshold, the point at which the table should be
  // updated.
  HeapWord* threshold() const { return _next_offset_threshold; }

  // In general, these methods expect to be called with
  // [blk_start, blk_end) representing a block of memory in the heap.
  // In this implementation, however, we are OK even if blk_start and/or
  // blk_end are NULL because NULL is represented as 0, and thus
  // never exceeds the "_next_offset_threshold".
  void alloc_block(HeapWord* blk_start, HeapWord* blk_end) {
    if (blk_end > _next_offset_threshold) {
      alloc_block_work(blk_start, blk_end);
    }
  }
  void alloc_block(HeapWord* blk, size_t size) {
    alloc_block(blk, blk + size);
  }

  HeapWord* block_start_unsafe(const void* addr) const;

  // Debugging support
  virtual size_t last_active_index() const;
};

#endif // SHARE_GC_SHARED_BLOCKOFFSETTABLE_HPP
