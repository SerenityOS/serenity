/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PARMARKBITMAP_HPP
#define SHARE_GC_PARALLEL_PARMARKBITMAP_HPP

#include "memory/memRegion.hpp"
#include "oops/oop.hpp"
#include "utilities/bitMap.hpp"

class ParMarkBitMapClosure;
class PSVirtualSpace;
class ParCompactionManager;

class ParMarkBitMap: public CHeapObj<mtGC>
{
public:
  typedef BitMap::idx_t idx_t;

  // Values returned by the iterate() methods.
  enum IterationStatus { incomplete, complete, full, would_overflow };

  inline ParMarkBitMap();
  bool initialize(MemRegion covered_region);

  // Atomically mark an object as live.
  bool mark_obj(HeapWord* addr, size_t size);
  inline bool mark_obj(oop obj, int size);

  // Return whether the specified begin or end bit is set.
  inline bool is_obj_beg(idx_t bit) const;
  inline bool is_obj_end(idx_t bit) const;

  // Traditional interface for testing whether an object is marked or not (these
  // test only the begin bits).
  inline bool is_marked(idx_t bit)      const;
  inline bool is_marked(HeapWord* addr) const;
  inline bool is_marked(oop obj)        const;

  inline bool is_unmarked(idx_t bit)      const;
  inline bool is_unmarked(HeapWord* addr) const;
  inline bool is_unmarked(oop obj)        const;

  // Convert sizes from bits to HeapWords and back.  An object that is n bits
  // long will be bits_to_words(n) words long.  An object that is m words long
  // will take up words_to_bits(m) bits in the bitmap.
  inline static size_t bits_to_words(idx_t bits);
  inline static idx_t  words_to_bits(size_t words);

  // Return the size in words of an object given a begin bit and an end bit, or
  // the equivalent beg_addr and end_addr.
  inline size_t obj_size(idx_t beg_bit, idx_t end_bit) const;
  inline size_t obj_size(HeapWord* beg_addr, HeapWord* end_addr) const;

  // Return the size in words of the object (a search is done for the end bit).
  inline size_t obj_size(idx_t beg_bit)  const;
  inline size_t obj_size(HeapWord* addr) const;

  // Apply live_closure to each live object that lies completely within the
  // range [live_range_beg, live_range_end).  This is used to iterate over the
  // compacted region of the heap.  Return values:
  //
  // incomplete         The iteration is not complete.  The last object that
  //                    begins in the range does not end in the range;
  //                    closure->source() is set to the start of that object.
  //
  // complete           The iteration is complete.  All objects in the range
  //                    were processed and the closure is not full;
  //                    closure->source() is set one past the end of the range.
  //
  // full               The closure is full; closure->source() is set to one
  //                    past the end of the last object processed.
  //
  // would_overflow     The next object in the range would overflow the closure;
  //                    closure->source() is set to the start of that object.
  IterationStatus iterate(ParMarkBitMapClosure* live_closure,
                          idx_t range_beg, idx_t range_end) const;
  inline IterationStatus iterate(ParMarkBitMapClosure* live_closure,
                                 HeapWord* range_beg,
                                 HeapWord* range_end) const;

  // Apply live closure as above and additionally apply dead_closure to all dead
  // space in the range [range_beg, dead_range_end).  Note that dead_range_end
  // must be >= range_end.  This is used to iterate over the dense prefix.
  //
  // This method assumes that if the first bit in the range (range_beg) is not
  // marked, then dead space begins at that point and the dead_closure is
  // applied.  Thus callers must ensure that range_beg is not in the middle of a
  // live object.
  IterationStatus iterate(ParMarkBitMapClosure* live_closure,
                          ParMarkBitMapClosure* dead_closure,
                          idx_t range_beg, idx_t range_end,
                          idx_t dead_range_end) const;
  inline IterationStatus iterate(ParMarkBitMapClosure* live_closure,
                                 ParMarkBitMapClosure* dead_closure,
                                 HeapWord* range_beg,
                                 HeapWord* range_end,
                                 HeapWord* dead_range_end) const;

  // Return the number of live words in the range [beg_addr, end_obj) due to
  // objects that start in the range.  If a live object extends onto the range,
  // the caller must detect and account for any live words due to that object.
  // If a live object extends beyond the end of the range, only the words within
  // the range are included in the result. The end of the range must be a live object,
  // which is the case when updating pointers.  This allows a branch to be removed
  // from inside the loop.
  size_t live_words_in_range(ParCompactionManager* cm, HeapWord* beg_addr, oop end_obj) const;

  inline HeapWord* region_start() const;
  inline HeapWord* region_end() const;
  inline size_t    region_size() const;
  inline size_t    size() const;

  size_t reserved_byte_size() const { return _reserved_byte_size; }

  // Convert a heap address to/from a bit index.
  inline idx_t     addr_to_bit(HeapWord* addr) const;
  inline HeapWord* bit_to_addr(idx_t bit) const;

  // Return word-aligned up range_end, which must not be greater than size().
  inline idx_t align_range_end(idx_t range_end) const;

  // Return the bit index of the first marked object that begins (or ends,
  // respectively) in the range [beg, end).  If no object is found, return end.
  // end must be word-aligned.
  inline idx_t find_obj_beg(idx_t beg, idx_t end) const;
  inline idx_t find_obj_end(idx_t beg, idx_t end) const;

  inline HeapWord* find_obj_beg(HeapWord* beg, HeapWord* end) const;
  inline HeapWord* find_obj_end(HeapWord* beg, HeapWord* end) const;

  // Clear a range of bits or the entire bitmap (both begin and end bits are
  // cleared).
  inline void clear_range(idx_t beg, idx_t end);

  // Return the number of bits required to represent the specified number of
  // HeapWords, or the specified region.
  static inline idx_t bits_required(size_t words);
  static inline idx_t bits_required(MemRegion covered_region);

  void print_on_error(outputStream* st) const {
    st->print_cr("Marking Bits: (ParMarkBitMap*) " PTR_FORMAT, p2i(this));
    _beg_bits.print_on_error(st, " Begin Bits: ");
    _end_bits.print_on_error(st, " End Bits:   ");
  }

#ifdef  ASSERT
  void verify_clear() const;
  inline void verify_bit(idx_t bit) const;
  inline void verify_addr(HeapWord* addr) const;
#endif  // #ifdef ASSERT

private:
  size_t live_words_in_range_helper(HeapWord* beg_addr, oop end_obj) const;

  bool is_live_words_in_range_in_cache(ParCompactionManager* cm, HeapWord* beg_addr) const;
  size_t live_words_in_range_use_cache(ParCompactionManager* cm, HeapWord* beg_addr, oop end_obj) const;
  void update_live_words_in_range_cache(ParCompactionManager* cm, HeapWord* beg_addr, oop end_obj, size_t result) const;

  // Each bit in the bitmap represents one unit of 'object granularity.' Objects
  // are double-word aligned in 32-bit VMs, but not in 64-bit VMs, so the 32-bit
  // granularity is 2, 64-bit is 1.
  static inline size_t obj_granularity() { return size_t(MinObjAlignment); }
  static inline int obj_granularity_shift() { return LogMinObjAlignment; }

  HeapWord*       _region_start;
  size_t          _region_size;
  BitMapView      _beg_bits;
  BitMapView      _end_bits;
  PSVirtualSpace* _virtual_space;
  size_t          _reserved_byte_size;
};

#endif // SHARE_GC_PARALLEL_PARMARKBITMAP_HPP
