/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_HEAPREGIONREMSET_HPP
#define SHARE_GC_G1_HEAPREGIONREMSET_HPP

#include "gc/g1/g1CardSet.hpp"
#include "gc/g1/g1CardSetMemory.hpp"
#include "gc/g1/g1CodeCacheRemSet.hpp"
#include "gc/g1/g1FromCardCache.hpp"
#include "runtime/atomic.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/bitMap.hpp"

class outputStream;

class HeapRegionRemSet : public CHeapObj<mtGC> {
  friend class VMStructs;

  Mutex _m;
  // A set of code blobs (nmethods) whose code contains pointers into
  // the region that owns this RSet.
  G1CodeRootSet _code_roots;

  G1CardSetMemoryManager _card_set_mm;

  // The set of cards in the Java heap
  G1CardSet _card_set;

  HeapRegion* _hr;

  inline void split_card(OopOrNarrowOopStar from, uint& card_region, uint& card_within_region) const;
  void clear_fcc();

public:
  HeapRegionRemSet(HeapRegion* hr, G1CardSetConfiguration* config);

  // Setup sparse and fine-grain tables sizes.
  bool cardset_is_empty() const {
    return _card_set.is_empty();
  }

  bool is_empty() const {
    return (strong_code_roots_list_length() == 0) && cardset_is_empty();
  }

  bool occupancy_less_or_equal_than(size_t occ) const {
    return (strong_code_roots_list_length() == 0) && _card_set.occupancy_less_or_equal_to(occ);
  }

  // Iterate the card based remembered set for merging them into the card table.
  // The passed closure must be a CardOrRangeVisitor; we use a template parameter
  // to pass it in to facilitate inlining as much as possible.
  template <class CardOrRangeVisitor>
  inline void iterate_for_merge(CardOrRangeVisitor& cl);

  size_t occupied() {
    return _card_set.occupied();
  }

  // Coarsening statistics since VM start.
  static G1CardSetCoarsenStats coarsen_stats() { return G1CardSet::coarsen_stats(); }

private:
  enum RemSetState {
    Untracked,
    Updating,
    Complete
  };

  RemSetState _state;

  static const char* _state_strings[];
  static const char* _short_state_strings[];
public:

  const char* get_state_str() const { return _state_strings[_state]; }
  const char* get_short_state_str() const { return _short_state_strings[_state]; }

  bool is_tracked() { return _state != Untracked; }
  bool is_updating() { return _state == Updating; }
  bool is_complete() { return _state == Complete; }

  inline void set_state_empty();
  inline void set_state_updating();
  inline void set_state_complete();

  inline void add_reference(OopOrNarrowOopStar from, uint tid);

  // The region is being reclaimed; clear its remset, and any mention of
  // entries for this region in other remsets.
  void clear(bool only_cardset = false);
  void clear_locked(bool only_cardset = false);

  G1CardSetMemoryStats card_set_memory_stats() const { return _card_set_mm.memory_stats(); }

  // The actual # of bytes this hr_remset takes up. Also includes the strong code
  // root set.
  size_t mem_size() {
    return _card_set.mem_size()
      + (sizeof(HeapRegionRemSet) - sizeof(G1CardSet)) // Avoid double-counting G1CardSet.
      + strong_code_roots_mem_size();
  }

  size_t wasted_mem_size() {
    return _card_set.wasted_mem_size();
  }

  // Returns the memory occupancy of all static data structures associated
  // with remembered sets.
  static size_t static_mem_size() {
    return G1CardSet::static_mem_size() + G1CodeRootSet::static_mem_size() + sizeof(G1CardSetFreePool);
  }

  static void print_static_mem_size(outputStream* out);

  inline bool contains_reference(OopOrNarrowOopStar from);

  inline void print_info(outputStream* st, OopOrNarrowOopStar from);

  // Routines for managing the list of code roots that point into
  // the heap region that owns this RSet.
  void add_strong_code_root(nmethod* nm);
  void add_strong_code_root_locked(nmethod* nm);
  void remove_strong_code_root(nmethod* nm);

  // Applies blk->do_code_blob() to each of the entries in
  // the strong code roots list
  void strong_code_roots_do(CodeBlobClosure* blk) const;

  void clean_strong_code_roots(HeapRegion* hr);

  // Returns the number of elements in the strong code roots list
  size_t strong_code_roots_list_length() const {
    return _code_roots.length();
  }

  // Returns true if the strong code roots contains the given
  // nmethod.
  bool strong_code_roots_list_contains(nmethod* nm) {
    return _code_roots.contains(nm);
  }

  // Returns the amount of memory, in bytes, currently
  // consumed by the strong code roots.
  size_t strong_code_roots_mem_size();

  static void invalidate_from_card_cache(uint start_idx, size_t num_regions) {
    G1FromCardCache::invalidate(start_idx, num_regions);
  }

#ifndef PRODUCT
  static void print_from_card_cache() {
    G1FromCardCache::print();
  }

  static void test();
#endif
};

#endif // SHARE_GC_G1_HEAPREGIONREMSET_HPP
