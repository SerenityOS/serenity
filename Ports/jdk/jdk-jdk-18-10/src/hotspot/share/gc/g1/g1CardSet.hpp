/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CARDSET_HPP
#define SHARE_GC_G1_G1CARDSET_HPP

#include "memory/allocation.hpp"
#include "memory/padded.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/concurrentHashTable.hpp"
#include "utilities/lockFreeStack.hpp"

class G1CardSetAllocOptions;
class G1CardSetBufferList;
class G1CardSetHashTable;
class G1CardSetHashTableValue;
class G1CardSetMemoryManager;
class Mutex;

// The result of an attempt to add a card to that card set.
enum G1AddCardResult {
  Overflow,  // The card set is more than full. The entry may have been added. Need
             // Coarsen and retry.
  Found,     // The card is already in the set.
  Added      // The card has been added to the set by this attempt.
};

class G1CardSetConfiguration {
  uint _inline_ptr_bits_per_card;
  uint _num_cards_in_array;
  uint _num_cards_in_howl_bitmap;
  uint _num_buckets_in_howl;
  uint _max_cards_in_card_set;
  uint _cards_in_howl_threshold;
  uint _cards_in_howl_bitmap_threshold;
  uint _log2_num_cards_in_howl_bitmap;
  size_t _bitmap_hash_mask;

  void log_configuration();
public:

  // Initialize card set configuration from globals.
  G1CardSetConfiguration();
  // Initialize card set configuration from parameters.
  G1CardSetConfiguration(uint inline_ptr_bits_per_card,
                         uint num_cards_in_array,
                         double cards_in_bitmap_threshold,
                         uint max_buckets_in_howl,
                         double cards_in_howl_threshold,
                         uint max_cards_in_cardset);

  // Inline pointer configuration
  uint inline_ptr_bits_per_card() const { return _inline_ptr_bits_per_card; }
  uint num_cards_in_inline_ptr() const;
  static uint num_cards_in_inline_ptr(uint bits_per_card);

  // Array of Cards configuration
  bool use_cards_in_array() const { return _num_cards_in_array != 0; } // Unused for now
  // Number of cards in "Array of Cards" set; 0 to disable.
  // Always coarsen to next level if full, so no specific threshold.
  uint num_cards_in_array() const { return _num_cards_in_array; }

  // Bitmap within Howl card set container configuration
  bool use_cards_in_howl_bitmap() const { return _num_cards_in_howl_bitmap != 0; } // Unused for now
  uint num_cards_in_howl_bitmap() const { return _num_cards_in_howl_bitmap; }
  // (Approximate) Number of cards in bitmap to coarsen Howl Bitmap to Howl Full.
  uint cards_in_howl_bitmap_threshold() const { return _cards_in_howl_bitmap_threshold; }
  uint log2_num_cards_in_howl_bitmap() const {return _log2_num_cards_in_howl_bitmap;}

  // Howl card set container configuration
  uint num_buckets_in_howl() const { return _num_buckets_in_howl; }
  // Threshold at which to turn howling arrays into Full.
  uint cards_in_howl_threshold() const { return _cards_in_howl_threshold; }
  uint howl_bitmap_offset(uint card_idx) const { return card_idx & _bitmap_hash_mask; }
  // Given a card index, return the bucket in the array of card sets.
  uint howl_bucket_index(uint card_idx) { return card_idx >> _log2_num_cards_in_howl_bitmap; }

  // Full card configuration
  // Maximum number of cards in a non-full card set for a single region. Card sets
  // with more entries per region are coarsened to Full.
  uint max_cards_in_region() const { return _max_cards_in_card_set; }

  // Memory object types configuration
  // Number of distinctly sized memory objects on the card set heap.
  // Currently contains CHT-Nodes, ArrayOfCards, BitMaps, Howl
  static constexpr uint num_mem_object_types() { return 4; }
  // Returns the memory allocation options for the memory objects on the card set heap. The returned
  // array must be freed by the caller.
  G1CardSetAllocOptions* mem_object_alloc_options();

  // For a given memory object, get a descriptive name.
  static const char* mem_object_type_name_str(uint index);
};

// Collects coarsening statistics: how many attempts of each kind and how many
// failed due to a competing thread doing the coarsening first.
class G1CardSetCoarsenStats {
public:
  // Number of entries in the statistics tables: since we index with the source
  // cardset of the coarsening, this is the total number of combinations of
  // card sets - 1.
  static constexpr size_t NumCoarsenCategories = 7;
  // Coarsening statistics for the possible CardSetPtr in the Howl card set
  // start from this offset.
  static constexpr size_t CoarsenHowlOffset = 4;

private:
  // Indices are "from" indices.
  size_t _coarsen_from[NumCoarsenCategories];
  size_t _coarsen_collision[NumCoarsenCategories];

public:
  G1CardSetCoarsenStats() { reset(); }

  void reset();

  void subtract_from(G1CardSetCoarsenStats& other);

  // Record a coarsening for the given tag/category. Collision should be true if
  // this coarsening lost the race to do the coarsening of that category.
  void record_coarsening(uint tag, bool collision);

  void print_on(outputStream* out);
};

// Sparse set of card indexes comprising a remembered set on the Java heap. Card
// size is assumed to be card table card size.
//
// Technically it is implemented using a ConcurrentHashTable that stores a card
// set container for every region containing at least one card.
//
// There are in total five different containers, encoded in the ConcurrentHashTable
// node as CardSetPtr. A CardSetPtr may cover the whole region or just a part of
// it.
// See its description below for more information.
class G1CardSet : public CHeapObj<mtGCCardSet> {
  friend class G1CardSetTest;
  friend class G1CardSetMtTestTask;

  template <typename Closure, template <typename> class CardorRanges>
  friend class G1CardSetMergeCardIterator;

  friend class G1TransferCard;

  friend class G1ReleaseCardsets;

  static G1CardSetCoarsenStats _coarsen_stats; // Coarsening statistics since VM start.
  static G1CardSetCoarsenStats _last_coarsen_stats; // Coarsening statistics at last GC.
public:
  // Two lower bits are used to encode the card storage types
  static const uintptr_t CardSetPtrHeaderSize = 2;

  // CardSetPtr represents the card storage type of a given covered area. It encodes
  // a type in the LSBs, in addition to having a few significant values.
  //
  // Possible encodings:
  //
  // 0...00000 free               (Empty, should never happen)
  // 1...11111 full               All card indexes in the whole area this CardSetPtr covers are part of this container.
  // X...XXX00 inline-ptr-cards   A handful of card indexes covered by this CardSetPtr are encoded within the CardSetPtr.
  // X...XXX01 array of cards     The container is a contiguous array of card indexes.
  // X...XXX10 bitmap             The container uses a bitmap to determine whether a given index is part of this set.
  // X...XXX11 howl               This is a card set container containing an array of CardSetPtr, with each CardSetPtr
  //                              limited to a sub-range of the original range. Currently only one level of this
  //                              container is supported.
  typedef void* CardSetPtr;
  // Coarsening happens in the order below:
  // CardSetInlinePtr -> CardSetArrayOfCards -> CardSetHowl -> Full
  // Corsening of containers inside the CardSetHowl happens in the order:
  // CardSetInlinePtr -> CardSetArrayOfCards -> CardSetBitMap -> Full
  static const uintptr_t CardSetInlinePtr      = 0x0;
  static const uintptr_t CardSetArrayOfCards   = 0x1;
  static const uintptr_t CardSetBitMap         = 0x2;
  static const uintptr_t CardSetHowl           = 0x3;

  // The special sentinel values
  static constexpr CardSetPtr FreeCardSet = nullptr;
  // Unfortunately we can't make (G1CardSet::CardSetPtr)-1 constexpr because
  // reinterpret_casts are forbidden in constexprs. Use a regular static instead.
  static CardSetPtr FullCardSet;

  static const uintptr_t CardSetPtrTypeMask    = ((uintptr_t)1 << CardSetPtrHeaderSize) - 1;

  static CardSetPtr strip_card_set_type(CardSetPtr ptr) { return (CardSetPtr)((uintptr_t)ptr & ~CardSetPtrTypeMask); }

  static uint card_set_type(CardSetPtr ptr) { return (uintptr_t)ptr & CardSetPtrTypeMask; }

  template <class T>
  static T* card_set_ptr(CardSetPtr ptr);

private:
  G1CardSetMemoryManager* _mm;
  G1CardSetConfiguration* _config;

  G1CardSetHashTable* _table;

  // Total number of cards in this card set. This is a best-effort value, i.e. there may
  // be (slightly) more cards in the card set than this value in reality.
  size_t _num_occupied;

  CardSetPtr make_card_set_ptr(void* value, uintptr_t type);

  CardSetPtr acquire_card_set(CardSetPtr volatile* card_set_addr);
  // Returns true if the card set should be released
  bool release_card_set(CardSetPtr card_set);
  // Release card set and free if needed.
  void release_and_maybe_free_card_set(CardSetPtr card_set);
  // Release card set and free (and it must be freeable).
  void release_and_must_free_card_set(CardSetPtr card_set);

  // Coarsens the CardSet cur_card_set to the next level; tries to replace the
  // previous CardSet with a new one which includes the given card_in_region.
  // coarsen_card_set does not transfer cards from cur_card_set
  // to the new card_set. Transfer is achieved by transfer_cards.
  // Returns true if this was the thread that coarsened the CardSet (and added the card).
  bool coarsen_card_set(CardSetPtr volatile* card_set_addr,
                        CardSetPtr cur_card_set,
                        uint card_in_region, bool within_howl = false);

  CardSetPtr create_coarsened_array_of_cards(uint card_in_region, bool within_howl);

  // Transfer entries from source_card_set to a recently installed coarser storage type
  // We only need to transfer anything finer than CardSetBitMap. "Full" contains
  // all elements anyway.
  void transfer_cards(G1CardSetHashTableValue* table_entry, CardSetPtr source_card_set, uint card_region);
  void transfer_cards_in_howl(CardSetPtr parent_card_set, CardSetPtr source_card_set, uint card_region);

  G1AddCardResult add_to_card_set(CardSetPtr volatile* card_set_addr, CardSetPtr card_set, uint card_region, uint card, bool increment_total = true);

  G1AddCardResult add_to_inline_ptr(CardSetPtr volatile* card_set_addr, CardSetPtr card_set, uint card_in_region);
  G1AddCardResult add_to_array(CardSetPtr card_set, uint card_in_region);
  G1AddCardResult add_to_bitmap(CardSetPtr card_set, uint card_in_region);
  G1AddCardResult add_to_howl(CardSetPtr parent_card_set, uint card_region, uint card_in_region, bool increment_total = true);

  G1CardSetHashTableValue* get_or_add_card_set(uint card_region, bool* should_grow_table);
  CardSetPtr get_card_set(uint card_region);

  // Iterate over cards of a card set container during transfer of the cards from
  // one container to another. Executes
  //
  //     void operator ()(uint card_idx)
  //
  // on the given class.
  template <class CardVisitor>
  void iterate_cards_during_transfer(CardSetPtr const card_set, CardVisitor& found);

  // Iterate over the container, calling a method on every card or card range contained
  // in the card container.
  // For every container, first calls
  //
  //   void start_iterate(uint tag, uint region_idx);
  //
  // Then for every card or card range it calls
  //
  //   void do_card(uint card_idx);
  //   void do_card_range(uint card_idx, uint length);
  //
  // where card_idx is the card index within that region_idx passed before in
  // start_iterate().
  //
  template <class CardOrRangeVisitor>
  void iterate_cards_or_ranges_in_container(CardSetPtr const card_set, CardOrRangeVisitor& found);

  uint card_set_type_to_mem_object_type(uintptr_t type) const;
  uint8_t* allocate_mem_object(uintptr_t type);
  void free_mem_object(CardSetPtr card_set);

public:
  G1CardSetConfiguration* config() const { return _config; }

  // Create a new remembered set for a particular heap region.
  G1CardSet(G1CardSetConfiguration* config, G1CardSetMemoryManager* mm);
  virtual ~G1CardSet();

  // Adds the given card to this set, returning an appropriate result. If added,
  // updates the total count.
  G1AddCardResult add_card(uint card_region, uint card_in_region, bool increment_total = true);

  bool contains_card(uint card_region, uint card_in_region);

  void print_info(outputStream* st, uint card_region, uint card_in_region);

  // Returns whether this remembered set (and all sub-sets) have an occupancy
  // that is less or equal to the given occupancy.
  bool occupancy_less_or_equal_to(size_t limit) const;

  // Returns whether this remembered set (and all sub-sets) does not contain any entry.
  bool is_empty() const;

  // Returns the number of cards contained in this remembered set.
  size_t occupied() const;

  size_t num_containers();

  static G1CardSetCoarsenStats coarsen_stats();
  static void print_coarsen_stats(outputStream* out);

  // Returns size of the actual remembered set containers in bytes.
  size_t mem_size() const;
  size_t wasted_mem_size() const;
  // Returns the size of static data in bytes.
  static size_t static_mem_size();

  // Clear the entire contents of this remembered set.
  void clear();

  void print(outputStream* os);

  // Various iterators - should be made inlineable somehow.
  class G1CardSetPtrIterator {
  public:
    virtual void do_cardsetptr(uint region_idx, size_t num_occupied, CardSetPtr card_set) = 0;
  };

  void iterate_containers(G1CardSetPtrIterator* iter, bool safepoint = false);

  class G1CardSetCardIterator {
  public:
    virtual void do_card(uint region_idx, uint card_idx) = 0;
  };

  void iterate_cards(G1CardSetCardIterator& iter);

  // Iterate all cards for card set merging. Must be a CardOrRangeVisitor as
  // explained above.
  template <class CardOrRangeVisitor>
  void iterate_for_merge(CardOrRangeVisitor& cl);
};

class G1CardSetHashTableValue {
public:
  using CardSetPtr = G1CardSet::CardSetPtr;

  const uint _region_idx;
  uint volatile _num_occupied;
  CardSetPtr volatile _card_set;

  G1CardSetHashTableValue(uint region_idx, CardSetPtr card_set) : _region_idx(region_idx), _num_occupied(0), _card_set(card_set) { }
};

class G1CardSetHashTableConfig : public StackObj {
public:
  using Value = G1CardSetHashTableValue;

  static uintx get_hash(Value const& value, bool* is_dead) {
    *is_dead = false;
    return value._region_idx;
  }
  static void* allocate_node(void* context, size_t size, Value const& value);
  static void free_node(void* context, void* memory, Value const& value);
};

typedef ConcurrentHashTable<G1CardSetHashTableConfig, mtGCCardSet> CardSetHash;

#endif // SHARE_GC_G1_G1CARDSET_HPP
