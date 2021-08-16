/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_CARDTABLE_HPP
#define SHARE_GC_SHARED_CARDTABLE_HPP

#include "memory/allocation.hpp"
#include "memory/memRegion.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/align.hpp"

class CardTable: public CHeapObj<mtGC> {
  friend class VMStructs;
public:
  typedef uint8_t CardValue;

  // All code generators assume that the size of a card table entry is one byte.
  // They need to be updated to reflect any change to this.
  // This code can typically be found by searching for the byte_map_base() method.
  STATIC_ASSERT(sizeof(CardValue) == 1);

protected:
  // The declaration order of these const fields is important; see the
  // constructor before changing.
  const MemRegion _whole_heap;       // the region covered by the card table
  size_t          _guard_index;      // index of very last element in the card
                                     // table; it is set to a guard value
                                     // (last_card) and should never be modified
  size_t          _last_valid_index; // index of the last valid element
  const size_t    _page_size;        // page size used when mapping _byte_map
  size_t          _byte_map_size;    // in bytes
  CardValue*      _byte_map;         // the card marking array
  CardValue*      _byte_map_base;

  int _cur_covered_regions;

  // The covered regions should be in address order.
  MemRegion* _covered;
  // The committed regions correspond one-to-one to the covered regions.
  // They represent the card-table memory that has been committed to service
  // the corresponding covered region.  It may be that committed region for
  // one covered region corresponds to a larger region because of page-size
  // roundings.  Thus, a committed region for one covered region may
  // actually extend onto the card-table space for the next covered region.
  MemRegion* _committed;

  // The last card is a guard card, and we commit the page for it so
  // we can use the card for verification purposes. We make sure we never
  // uncommit the MemRegion for that page.
  MemRegion _guard_region;

  inline size_t compute_byte_map_size();

  // Finds and return the index of the region, if any, to which the given
  // region would be contiguous.  If none exists, assign a new region and
  // returns its index.  Requires that no more than the maximum number of
  // covered regions defined in the constructor are ever in use.
  int find_covering_region_by_base(HeapWord* base);

  // Same as above, but finds the region containing the given address
  // instead of starting at a given base address.
  int find_covering_region_containing(HeapWord* addr);

  // Returns the leftmost end of a committed region corresponding to a
  // covered region before covered region "ind", or else "NULL" if "ind" is
  // the first covered region.
  HeapWord* largest_prev_committed_end(int ind) const;

  // Returns the part of the region mr that doesn't intersect with
  // any committed region other than self.  Used to prevent uncommitting
  // regions that are also committed by other regions.  Also protects
  // against uncommitting the guard region.
  MemRegion committed_unique_to_self(int self, MemRegion mr) const;

  // Some barrier sets create tables whose elements correspond to parts of
  // the heap; the CardTableBarrierSet is an example.  Such barrier sets will
  // normally reserve space for such tables, and commit parts of the table
  // "covering" parts of the heap that are committed. At most one covered
  // region per generation is needed.
  static const int _max_covered_regions = 2;

  enum CardValues {
    clean_card                  = (CardValue)-1,

    dirty_card                  =  0,
    last_card                   =  1,
    CT_MR_BS_last_reserved      =  2
  };

  // a word's worth (row) of clean card values
  static const intptr_t clean_card_row = (intptr_t)(-1);

public:
  CardTable(MemRegion whole_heap);
  virtual ~CardTable();
  virtual void initialize();

  // The kinds of precision a CardTable may offer.
  enum PrecisionStyle {
    Precise,
    ObjHeadPreciseArray
  };

  // Tells what style of precision this card table offers.
  PrecisionStyle precision() {
    return ObjHeadPreciseArray; // Only one supported for now.
  }

  // *** Barrier set functions.

  // Initialization utilities; covered_words is the size of the covered region
  // in, um, words.
  inline size_t cards_required(size_t covered_words) {
    // Add one for a guard card, used to detect errors.
    const size_t words = align_up(covered_words, card_size_in_words);
    return words / card_size_in_words + 1;
  }

  // Dirty the bytes corresponding to "mr" (not all of which must be
  // covered.)
  void dirty_MemRegion(MemRegion mr);

  // Clear (to clean_card) the bytes entirely contained within "mr" (not
  // all of which must be covered.)
  void clear_MemRegion(MemRegion mr);

  // Return true if "p" is at the start of a card.
  bool is_card_aligned(HeapWord* p) {
    CardValue* pcard = byte_for(p);
    return (addr_for(pcard) == p);
  }

  // Mapping from address to card marking array entry
  CardValue* byte_for(const void* p) const {
    assert(_whole_heap.contains(p),
           "Attempt to access p = " PTR_FORMAT " out of bounds of "
           " card marking array's _whole_heap = [" PTR_FORMAT "," PTR_FORMAT ")",
           p2i(p), p2i(_whole_heap.start()), p2i(_whole_heap.end()));
    CardValue* result = &_byte_map_base[uintptr_t(p) >> card_shift];
    assert(result >= _byte_map && result < _byte_map + _byte_map_size,
           "out of bounds accessor for card marking array");
    return result;
  }

  // The card table byte one after the card marking array
  // entry for argument address. Typically used for higher bounds
  // for loops iterating through the card table.
  CardValue* byte_after(const void* p) const {
    return byte_for(p) + 1;
  }

  virtual void invalidate(MemRegion mr);
  void clear(MemRegion mr);
  void dirty(MemRegion mr);

  // Provide read-only access to the card table array.
  const CardValue* byte_for_const(const void* p) const {
    return byte_for(p);
  }
  const CardValue* byte_after_const(const void* p) const {
    return byte_after(p);
  }

  // Mapping from card marking array entry to address of first word
  HeapWord* addr_for(const CardValue* p) const {
    assert(p >= _byte_map && p < _byte_map + _byte_map_size,
           "out of bounds access to card marking array. p: " PTR_FORMAT
           " _byte_map: " PTR_FORMAT " _byte_map + _byte_map_size: " PTR_FORMAT,
           p2i(p), p2i(_byte_map), p2i(_byte_map + _byte_map_size));
    size_t delta = pointer_delta(p, _byte_map_base, sizeof(CardValue));
    HeapWord* result = (HeapWord*) (delta << card_shift);
    assert(_whole_heap.contains(result),
           "Returning result = " PTR_FORMAT " out of bounds of "
           " card marking array's _whole_heap = [" PTR_FORMAT "," PTR_FORMAT ")",
           p2i(result), p2i(_whole_heap.start()), p2i(_whole_heap.end()));
    return result;
  }

  // Mapping from address to card marking array index.
  size_t index_for(void* p) {
    assert(_whole_heap.contains(p),
           "Attempt to access p = " PTR_FORMAT " out of bounds of "
           " card marking array's _whole_heap = [" PTR_FORMAT "," PTR_FORMAT ")",
           p2i(p), p2i(_whole_heap.start()), p2i(_whole_heap.end()));
    return byte_for(p) - _byte_map;
  }

  CardValue* byte_for_index(const size_t card_index) const {
    return _byte_map + card_index;
  }

  // Resize one of the regions covered by the remembered set.
  virtual void resize_covered_region(MemRegion new_region);

  // *** Card-table-RemSet-specific things.

  static uintx ct_max_alignment_constraint();

  // Apply closure "cl" to the dirty cards containing some part of
  // MemRegion "mr".
  void dirty_card_iterate(MemRegion mr, MemRegionClosure* cl);

  // Return the MemRegion corresponding to the first maximal run
  // of dirty cards lying completely within MemRegion mr.
  // If reset is "true", then sets those card table entries to the given
  // value.
  MemRegion dirty_card_range_after_reset(MemRegion mr, bool reset,
                                         int reset_val);

  // Constants
  enum SomePublicConstants {
    card_shift                  = 9,
    card_size                   = 1 << card_shift,
    card_size_in_words          = card_size / sizeof(HeapWord)
  };

  static constexpr CardValue clean_card_val()          { return clean_card; }
  static constexpr CardValue dirty_card_val()          { return dirty_card; }
  static intptr_t clean_card_row_val()   { return clean_card_row; }

  // Card marking array base (adjusted for heap low boundary)
  // This would be the 0th element of _byte_map, if the heap started at 0x0.
  // But since the heap starts at some higher address, this points to somewhere
  // before the beginning of the actual _byte_map.
  CardValue* byte_map_base() const { return _byte_map_base; }

  virtual bool is_in_young(oop obj) const = 0;

  // Print a description of the memory for the card table
  virtual void print_on(outputStream* st) const;

  void verify();
  void verify_guard();

  // val_equals -> it will check that all cards covered by mr equal val
  // !val_equals -> it will check that all cards covered by mr do not equal val
  void verify_region(MemRegion mr, CardValue val, bool val_equals) PRODUCT_RETURN;
  void verify_not_dirty_region(MemRegion mr) PRODUCT_RETURN;
  void verify_dirty_region(MemRegion mr) PRODUCT_RETURN;
};

#endif // SHARE_GC_SHARED_CARDTABLE_HPP
