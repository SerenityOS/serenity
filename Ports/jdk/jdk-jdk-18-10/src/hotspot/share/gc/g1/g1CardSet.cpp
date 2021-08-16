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

#include "precompiled.hpp"
#include "gc/g1/g1CardSet.inline.hpp"
#include "gc/g1/g1CardSetContainers.inline.hpp"
#include "gc/g1/g1CardSetMemory.inline.hpp"
#include "gc/g1/g1FromCardCache.hpp"
#include "gc/g1/heapRegion.inline.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/mutex.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/concurrentHashTable.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/lockFreeStack.hpp"
#include "utilities/spinYield.hpp"

#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/gcTraceTime.inline.hpp"
#include "runtime/java.hpp"

G1CardSet::CardSetPtr G1CardSet::FullCardSet = (G1CardSet::CardSetPtr)-1;

G1CardSetConfiguration::G1CardSetConfiguration() :
  _inline_ptr_bits_per_card(HeapRegion::LogOfHRGrainBytes - CardTable::card_shift) {

  // Array of Cards card set container size calculation
  _num_cards_in_array = G1RemSetArrayOfCardsEntries;

  // Full card set container size calculation
  _max_cards_in_card_set = (uint)HeapRegion::CardsPerRegion;
  assert(is_power_of_2(_max_cards_in_card_set),
        "max_cards_in_card_set must be a power of 2: %u", _max_cards_in_card_set);
  _cards_in_howl_threshold = _max_cards_in_card_set * (double)G1RemSetCoarsenHowlToFullPercent / 100;

  // Howl card set container size calculation.
  _num_buckets_in_howl = G1RemSetHowlNumBuckets;

  // Howl Bitmap card set container size calculation.
  _num_cards_in_howl_bitmap = G1CardSetHowl::bitmap_size(_max_cards_in_card_set, _num_buckets_in_howl);
  _log2_num_cards_in_howl_bitmap = log2i_exact(_num_cards_in_howl_bitmap);
  _cards_in_howl_bitmap_threshold = _num_cards_in_howl_bitmap * (double)G1RemSetCoarsenHowlBitmapToHowlFullPercent / 100;
  _bitmap_hash_mask = ~(~(0) << _log2_num_cards_in_howl_bitmap);

  log_configuration();
}

G1CardSetConfiguration::G1CardSetConfiguration(uint inline_ptr_bits_per_card,
                                               uint num_cards_in_array,
                                               double cards_in_bitmap_threshold,
                                               uint max_buckets_in_howl,
                                               double cards_in_howl_threshold,
                                               uint max_cards_in_cardset) :
  _inline_ptr_bits_per_card(inline_ptr_bits_per_card),
  _num_cards_in_array(num_cards_in_array),
  _max_cards_in_card_set(max_cards_in_cardset),
  _cards_in_howl_threshold(max_cards_in_cardset * cards_in_howl_threshold) {

  assert(is_power_of_2(_max_cards_in_card_set),
        "max_cards_in_card_set must be a power of 2: %u", _max_cards_in_card_set);

  _num_buckets_in_howl = G1CardSetHowl::num_buckets(_max_cards_in_card_set, _num_cards_in_array, max_buckets_in_howl);

  _num_cards_in_howl_bitmap = G1CardSetHowl::bitmap_size(_max_cards_in_card_set, _num_buckets_in_howl);
  _cards_in_howl_bitmap_threshold = _num_cards_in_howl_bitmap * cards_in_bitmap_threshold;
  _log2_num_cards_in_howl_bitmap = log2i_exact(_num_cards_in_howl_bitmap);
  _bitmap_hash_mask = ~(~(0) << _log2_num_cards_in_howl_bitmap);

  log_configuration();
}

void G1CardSetConfiguration::log_configuration() {
  log_debug_p(gc, remset)("Card Set container configuration: "
                          "InlinePtr #elems %u size %zu "
                          "Array Of Cards #elems %u size %zu "
                          "Howl #buckets %u coarsen threshold %u "
                          "Howl Bitmap #elems %u size %zu coarsen threshold %u",
                          num_cards_in_inline_ptr(), sizeof(void*),
                          num_cards_in_array(), G1CardSetArray::size_in_bytes(num_cards_in_array()),
                          num_buckets_in_howl(), cards_in_howl_threshold(),
                          num_cards_in_howl_bitmap(), G1CardSetBitMap::size_in_bytes(num_cards_in_howl_bitmap()), cards_in_howl_bitmap_threshold());
}

uint G1CardSetConfiguration::num_cards_in_inline_ptr() const {
  return num_cards_in_inline_ptr(_inline_ptr_bits_per_card);
}

uint G1CardSetConfiguration::num_cards_in_inline_ptr(uint bits_per_card) {
  return G1CardSetInlinePtr::max_cards_in_inline_ptr(bits_per_card);
}

G1CardSetAllocOptions* G1CardSetConfiguration::mem_object_alloc_options() {
  G1CardSetAllocOptions* result = NEW_C_HEAP_ARRAY(G1CardSetAllocOptions, num_mem_object_types(), mtGC);

  result[0] = { (uint)CardSetHash::get_node_size() };
  result[1] = { (uint)G1CardSetArray::size_in_bytes(num_cards_in_array()), 2, 256 };
  result[2] = { (uint)G1CardSetBitMap::size_in_bytes(num_cards_in_howl_bitmap()), 2, 256 };
  result[3] = { (uint)G1CardSetHowl::size_in_bytes(num_buckets_in_howl()), 2, 256 };

  return result;
}

const char* G1CardSetConfiguration::mem_object_type_name_str(uint index) {
  const char* names[] = { "Node", "Array", "Bitmap", "Howl" };
  return names[index];
}

void G1CardSetCoarsenStats::reset() {
  STATIC_ASSERT(ARRAY_SIZE(_coarsen_from) == ARRAY_SIZE(_coarsen_collision));
  for (uint i = 0; i < ARRAY_SIZE(_coarsen_from); i++) {
    _coarsen_from[i] = 0;
    _coarsen_collision[i] = 0;
  }
}

void G1CardSetCoarsenStats::subtract_from(G1CardSetCoarsenStats& other) {
  STATIC_ASSERT(ARRAY_SIZE(_coarsen_from) == ARRAY_SIZE(_coarsen_collision));
  for (uint i = 0; i < ARRAY_SIZE(_coarsen_from); i++) {
    _coarsen_from[i] = other._coarsen_from[i] - _coarsen_from[i];
    _coarsen_collision[i] = other._coarsen_collision[i] - _coarsen_collision[i];
  }
}

void G1CardSetCoarsenStats::record_coarsening(uint tag, bool collision) {
  assert(tag < ARRAY_SIZE(_coarsen_from), "tag %u out of bounds", tag);
  Atomic::inc(&_coarsen_from[tag], memory_order_relaxed);
  if (collision) {
    Atomic::inc(&_coarsen_collision[tag], memory_order_relaxed);
  }
}

void G1CardSetCoarsenStats::print_on(outputStream* out) {
  out->print_cr("Inline->AoC %zu (%zu) "
                "AoC->Howl %zu (%zu) "
                "Howl->Full %zu (%zu) "
                "Inline->AoC %zu (%zu) "
                "AoC->BitMap %zu (%zu) "
                "BitMap->Full %zu (%zu) ",
                _coarsen_from[0], _coarsen_collision[0],
                _coarsen_from[1], _coarsen_collision[1],
                // There is no BitMap at the first level so we can't .
                _coarsen_from[3], _coarsen_collision[3],
                _coarsen_from[4], _coarsen_collision[4],
                _coarsen_from[5], _coarsen_collision[5],
                _coarsen_from[6], _coarsen_collision[6]
               );
}

class G1CardSetHashTable : public CHeapObj<mtGCCardSet> {
  using CardSetPtr = G1CardSet::CardSetPtr;

  // Did we insert at least one element in the table?
  bool volatile _inserted_elem;

  G1CardSetMemoryManager* _mm;
  CardSetHash _table;

  class G1CardSetHashTableLookUp : public StackObj {
    uint _region_idx;
  public:
    explicit G1CardSetHashTableLookUp(uint region_idx) : _region_idx(region_idx) { }

    uintx get_hash() const { return _region_idx; }

    bool equals(G1CardSetHashTableValue* value, bool* is_dead) {
      *is_dead = false;
      return value->_region_idx == _region_idx;
    }
  };

  class G1CardSetHashTableFound : public StackObj {
    G1CardSetHashTableValue* _value;
  public:
    void operator()(G1CardSetHashTableValue* value) {
      _value = value;
    }

    G1CardSetHashTableValue* value() const { return _value; }
  };

  class G1CardSetHashTableScan : public StackObj {
    G1CardSet::G1CardSetPtrIterator* _scan_f;
  public:
    explicit G1CardSetHashTableScan(G1CardSet::G1CardSetPtrIterator* f) : _scan_f(f) { }

    bool operator()(G1CardSetHashTableValue* value) {
      _scan_f->do_cardsetptr(value->_region_idx, value->_num_occupied, value->_card_set);
      return true;
    }
  };


public:
  static const size_t InitialLogTableSize = 2;

  G1CardSetHashTable(G1CardSetMemoryManager* mm,
                     size_t initial_log_table_size = InitialLogTableSize) :
    _inserted_elem(false),
    _mm(mm),
    _table(mm, initial_log_table_size) {
  }

  ~G1CardSetHashTable() {
    reset();
  }

  G1CardSetHashTableValue* get_or_add(uint region_idx, bool* should_grow) {
    G1CardSetHashTableLookUp lookup(region_idx);
    G1CardSetHashTableFound found;

    if (_table.get(Thread::current(), lookup, found)) {
      return found.value();
    }

    G1CardSetHashTableValue value(region_idx, G1CardSetInlinePtr());
    bool inserted = _table.insert_get(Thread::current(), lookup, value, found, should_grow);

    if (!_inserted_elem && inserted) {
      // It does not matter to us who is setting the flag so a regular atomic store
      // is sufficient.
      Atomic::store(&_inserted_elem, true);
    }

    return found.value();
  }

  CardSetPtr get(uint region_idx) {
    G1CardSetHashTableLookUp lookup(region_idx);
    G1CardSetHashTableFound found;

    if (_table.get(Thread::current(), lookup, found)) {
      return found.value()->_card_set;
    }
    return nullptr;
  }

  void iterate_safepoint(G1CardSet::G1CardSetPtrIterator* cl2) {
    G1CardSetHashTableScan cl(cl2);
    _table.do_safepoint_scan(cl);
  }

  void iterate(G1CardSet::G1CardSetPtrIterator* cl2) {
    G1CardSetHashTableScan cl(cl2);
    _table.do_scan(Thread::current(), cl);
  }

  void reset() {
    if (Atomic::load(&_inserted_elem)) {
       _table.unsafe_reset(InitialLogTableSize);
      Atomic::store(&_inserted_elem, false);
    }
  }

  void print(outputStream* os) {
    os->print("TBL " PTR_FORMAT " size %zu mem %zu ", p2i(&_table), _table.get_size_log2(Thread::current()), _table.get_mem_size(Thread::current()));
  }

  void grow() {
    size_t new_limit = _table.get_size_log2(Thread::current()) + 1;
    _table.grow(Thread::current(), new_limit);
  }

  size_t mem_size() {
    return sizeof(*this) +
      _table.get_mem_size(Thread::current()) - sizeof(_table);
  }

  size_t log_table_size() { return _table.get_size_log2(Thread::current()); }
};

void* G1CardSetHashTableConfig::allocate_node(void* context, size_t size, Value const& value) {
  G1CardSetMemoryManager* mm = (G1CardSetMemoryManager*)context;
  return mm->allocate_node();
}

void G1CardSetHashTableConfig::free_node(void* context, void* memory, Value const& value) {
  G1CardSetMemoryManager* mm = (G1CardSetMemoryManager*)context;
  mm->free_node(memory);
}

G1CardSetCoarsenStats G1CardSet::_coarsen_stats;
G1CardSetCoarsenStats G1CardSet::_last_coarsen_stats;

G1CardSet::G1CardSet(G1CardSetConfiguration* config, G1CardSetMemoryManager* mm) :
  _mm(mm),
  _config(config),
  _table(new G1CardSetHashTable(mm)),
  _num_occupied(0) {
}

G1CardSet::~G1CardSet() {
  delete _table;
  _mm->flush();
}

uint G1CardSet::card_set_type_to_mem_object_type(uintptr_t type) const {
  assert(type == G1CardSet::CardSetArrayOfCards ||
         type == G1CardSet::CardSetBitMap ||
         type == G1CardSet::CardSetHowl, "should not allocate card set type %zu", type);

  return (uint)type;
}

uint8_t* G1CardSet::allocate_mem_object(uintptr_t type) {
  return _mm->allocate(card_set_type_to_mem_object_type(type));
}

void G1CardSet::free_mem_object(CardSetPtr card_set) {
  assert(card_set != G1CardSet::FreeCardSet, "should not free Free card set");
  assert(card_set != G1CardSet::FullCardSet, "should not free Full card set");

  uintptr_t type = card_set_type(card_set);
  void* value = strip_card_set_type(card_set);

  assert(type == G1CardSet::CardSetArrayOfCards ||
         type == G1CardSet::CardSetBitMap ||
         type == G1CardSet::CardSetHowl, "should not free card set type %zu", type);

#ifdef ASSERT
  if (type == G1CardSet::CardSetArrayOfCards ||
      type == G1CardSet::CardSetBitMap ||
      type == G1CardSet::CardSetHowl) {
    G1CardSetContainer* card_set = (G1CardSetContainer*)value;
    assert((card_set->refcount() == 1), "must be");
  }
#endif

  _mm->free(card_set_type_to_mem_object_type(type), value);
}

G1CardSet::CardSetPtr G1CardSet::acquire_card_set(CardSetPtr volatile* card_set_addr) {
  // Update reference counts under RCU critical section to avoid a
  // use-after-cleapup bug where we increment a reference count for
  // an object whose memory has already been cleaned up and reused.
  GlobalCounter::CriticalSection cs(Thread::current());
  while (true) {
    // Get cardsetptr and increment refcount atomically wrt to memory reuse.
    CardSetPtr card_set = Atomic::load_acquire(card_set_addr);
    uint cs_type = card_set_type(card_set);
    if (card_set == FullCardSet || cs_type == CardSetInlinePtr) {
      return card_set;
    }

    G1CardSetContainer* card_set_on_heap = (G1CardSetContainer*)strip_card_set_type(card_set);

    if (card_set_on_heap->try_increment_refcount()) {
      assert(card_set_on_heap->refcount() >= 3, "Smallest value is 3");
      return card_set;
    }
  }
}

bool G1CardSet::release_card_set(CardSetPtr card_set) {
  uint cs_type = card_set_type(card_set);
  if (card_set == FullCardSet || cs_type == CardSetInlinePtr) {
    return false;
  }

  G1CardSetContainer* card_set_on_heap = (G1CardSetContainer*)strip_card_set_type(card_set);
  return card_set_on_heap->decrement_refcount() == 1;
}

void G1CardSet::release_and_maybe_free_card_set(CardSetPtr card_set) {
  if (release_card_set(card_set)) {
    free_mem_object(card_set);
  }
}

void G1CardSet::release_and_must_free_card_set(CardSetPtr card_set) {
  bool should_free = release_card_set(card_set);
  assert(should_free, "should have been the only one having a reference");
  free_mem_object(card_set);
}

class G1ReleaseCardsets : public StackObj {
  G1CardSet* _card_set;
  using CardSetPtr = G1CardSet::CardSetPtr;

  void coarsen_to_full(CardSetPtr* card_set_addr) {
    while (true) {
      CardSetPtr cur_card_set = Atomic::load_acquire(card_set_addr);
      uint cs_type = G1CardSet::card_set_type(cur_card_set);
      if (cur_card_set == G1CardSet::FullCardSet) {
        return;
      }

      CardSetPtr old_value = Atomic::cmpxchg(card_set_addr, cur_card_set, G1CardSet::FullCardSet);

      if (old_value == cur_card_set) {
        _card_set->release_and_maybe_free_card_set(cur_card_set);
        return;
      }
    }
  }

public:
  explicit G1ReleaseCardsets(G1CardSet* card_set) : _card_set(card_set) { }

  void operator ()(CardSetPtr* card_set_addr) {
    coarsen_to_full(card_set_addr);
  }
};

G1AddCardResult G1CardSet::add_to_array(CardSetPtr card_set, uint card_in_region) {
  G1CardSetArray* array = card_set_ptr<G1CardSetArray>(card_set);
  return array->add(card_in_region);
}

G1AddCardResult G1CardSet::add_to_howl(CardSetPtr parent_card_set,
                                                uint card_region,
                                                uint card_in_region,
                                                bool increment_total) {
  G1CardSetHowl* howl = card_set_ptr<G1CardSetHowl>(parent_card_set);

  G1AddCardResult add_result;
  CardSetPtr to_transfer = nullptr;
  CardSetPtr card_set;

  uint bucket = _config->howl_bucket_index(card_in_region);
  volatile CardSetPtr* bucket_entry = howl->get_card_set_addr(bucket);

  while (true) {
    if (Atomic::load(&howl->_num_entries) >= _config->cards_in_howl_threshold()) {
      return Overflow;
    }

    card_set = acquire_card_set(bucket_entry);
    add_result = add_to_card_set(bucket_entry, card_set, card_region, card_in_region);

    if (add_result != Overflow) {
      break;
    }
    // Card set has overflown. Coarsen or retry.
    bool coarsened = coarsen_card_set(bucket_entry, card_set, card_in_region, true /* within_howl */);
    _coarsen_stats.record_coarsening(card_set_type(card_set) + G1CardSetCoarsenStats::CoarsenHowlOffset, !coarsened);
    if (coarsened) {
      // We have been the one coarsening this card set (and in the process added that card).
      add_result = Added;
      to_transfer = card_set;
      break;
    }
    // Somebody else beat us to coarsening. Retry.
    release_and_maybe_free_card_set(card_set);
  }

  if (increment_total && add_result == Added) {
    Atomic::inc(&howl->_num_entries, memory_order_relaxed);
  }

  if (to_transfer != nullptr) {
    transfer_cards_in_howl(parent_card_set, to_transfer, card_region);
  }

  release_and_maybe_free_card_set(card_set);
  return add_result;
}

G1AddCardResult G1CardSet::add_to_bitmap(CardSetPtr card_set, uint card_in_region) {
  G1CardSetBitMap* bitmap = card_set_ptr<G1CardSetBitMap>(card_set);
  uint card_offset = _config->howl_bitmap_offset(card_in_region);
  return bitmap->add(card_offset, _config->cards_in_howl_bitmap_threshold(), _config->num_cards_in_howl_bitmap());
}

G1AddCardResult G1CardSet::add_to_inline_ptr(CardSetPtr volatile* card_set_addr, CardSetPtr card_set, uint card_in_region) {
  G1CardSetInlinePtr value(card_set_addr, card_set);
  return value.add(card_in_region, _config->inline_ptr_bits_per_card(), _config->num_cards_in_inline_ptr());
}

G1CardSet::CardSetPtr G1CardSet::create_coarsened_array_of_cards(uint card_in_region, bool within_howl) {
  uint8_t* data = nullptr;
  CardSetPtr new_card_set;
  if (within_howl) {
    uint const size_in_bits = _config->num_cards_in_howl_bitmap();
    uint card_offset = _config->howl_bitmap_offset(card_in_region);
    data = allocate_mem_object(CardSetBitMap);
    new (data) G1CardSetBitMap(card_offset, size_in_bits);
    new_card_set = make_card_set_ptr(data, CardSetBitMap);
  } else {
    data = allocate_mem_object(CardSetHowl);
    new (data) G1CardSetHowl(card_in_region, _config);
    new_card_set = make_card_set_ptr(data, CardSetHowl);
  }
  return new_card_set;
}

bool G1CardSet::coarsen_card_set(volatile CardSetPtr* card_set_addr,
                                 CardSetPtr cur_card_set,
                                 uint card_in_region,
                                 bool within_howl) {
  CardSetPtr new_card_set = nullptr;

  switch (card_set_type(cur_card_set)) {
    case CardSetArrayOfCards : {
      new_card_set = create_coarsened_array_of_cards(card_in_region, within_howl);
      break;
    }
    case CardSetBitMap: {
      new_card_set = FullCardSet;
      break;
    }
    case CardSetInlinePtr: {
      uint const size = _config->num_cards_in_array();
      uint8_t* data = allocate_mem_object(CardSetArrayOfCards);
      new (data) G1CardSetArray(card_in_region, size);
      new_card_set = make_card_set_ptr(data, CardSetArrayOfCards);
      break;
    }
    case CardSetHowl: {
      new_card_set = FullCardSet; // anything will do at this point.
      break;
    }
    default:
      ShouldNotReachHere();
  }

  CardSetPtr old_value = Atomic::cmpxchg(card_set_addr, cur_card_set, new_card_set); // Memory order?
  if (old_value == cur_card_set) {
    // Success. Indicate that the cards from the current card set must be transferred
    // by this caller.
    // Release the hash table reference to the card. The caller still holds the
    // reference to this card set, so it can never be released (and we do not need to
    // check its result).
    bool should_free = release_card_set(cur_card_set);
    assert(!should_free, "must have had more than one reference");
    // Free containers if cur_card_set is CardSetHowl
    if (card_set_type(cur_card_set) == CardSetHowl) {
      G1ReleaseCardsets rel(this);
      card_set_ptr<G1CardSetHowl>(cur_card_set)->iterate(rel, _config->num_buckets_in_howl());
    }
    return true;
  } else {
    // Somebody else beat us to coarsening that card set. Exit, but clean up first.
    if (new_card_set != FullCardSet) {
      assert(new_card_set != nullptr, "must not be");
      release_and_must_free_card_set(new_card_set);
    }
    return false;
  }
}

class G1TransferCard : public StackObj {
  G1CardSet* _card_set;
  uint _region_idx;
public:
  G1TransferCard(G1CardSet* card_set, uint region_idx) : _card_set(card_set), _region_idx(region_idx) { }

  void operator ()(uint card_idx) {
    _card_set->add_card(_region_idx, card_idx, false);
  }
};

void G1CardSet::transfer_cards(G1CardSetHashTableValue* table_entry, CardSetPtr source_card_set, uint card_region) {
  assert(source_card_set != FullCardSet, "Should not need to transfer from full");
  // Need to transfer old entries unless there is a Full card set in place now, i.e.
  // the old type has been CardSetBitMap. "Full" contains all elements anyway.
  if (card_set_type(source_card_set) != CardSetHowl) {
    G1TransferCard iter(this, card_region);
    iterate_cards_during_transfer(source_card_set, iter);
  } else {
    assert(card_set_type(source_card_set) == CardSetHowl, "must be");
    // Need to correct for that the Full remembered set occupies more cards than the
    // AoCS before.
    Atomic::add(&_num_occupied, _config->max_cards_in_region() - table_entry->_num_occupied, memory_order_relaxed);
  }
}

void G1CardSet::transfer_cards_in_howl(CardSetPtr parent_card_set,
                                                     CardSetPtr source_card_set,
                                                     uint card_region) {
  assert(card_set_type(parent_card_set) == CardSetHowl, "must be");
  assert(source_card_set != FullCardSet, "Should not need to transfer from full");
  // Need to transfer old entries unless there is a Full card set in place now, i.e.
  // the old type has been CardSetBitMap.
  if (card_set_type(source_card_set) != CardSetBitMap) {
    // We only need to transfer from anything below CardSetBitMap.
    G1TransferCard iter(this, card_region);
    iterate_cards_during_transfer(source_card_set, iter);
  } else {
    uint diff = _config->num_cards_in_howl_bitmap() - card_set_ptr<G1CardSetBitMap>(source_card_set)->num_bits_set();

    // Need to correct for that the Full remembered set occupies more cards than the
    // bitmap before.
    // We add 1 element less because the values will be incremented
    // in G1CardSet::add_card for the current addition or where already incremented in
    // G1CardSet::add_to_howl after coarsening.
    diff -= 1;

    G1CardSetHowl* howling_array = card_set_ptr<G1CardSetHowl>(parent_card_set);
    Atomic::add(&howling_array->_num_entries, diff, memory_order_relaxed);

    bool should_grow_table = false;
    G1CardSetHashTableValue* table_entry = get_or_add_card_set(card_region, &should_grow_table);
    Atomic::add(&table_entry->_num_occupied, diff, memory_order_relaxed);

    Atomic::add(&_num_occupied, diff, memory_order_relaxed);
  }
}

G1AddCardResult G1CardSet::add_to_card_set(volatile CardSetPtr* card_set_addr, CardSetPtr card_set,  uint card_region, uint card_in_region, bool increment_total) {
  assert(card_set_addr != nullptr, "Cannot add to empty cardset");

  G1AddCardResult add_result;

  switch (card_set_type(card_set)) {
    case CardSetInlinePtr: {
      add_result = add_to_inline_ptr(card_set_addr, card_set, card_in_region);
      break;
    }
    case CardSetArrayOfCards : {
      add_result = add_to_array(card_set, card_in_region);
      break;
    }
    case CardSetBitMap: {
      add_result = add_to_bitmap(card_set, card_in_region);
      break;
    }
    case CardSetHowl: {
      assert(CardSetHowl == card_set_type(FullCardSet), "must be");
      if (card_set == FullCardSet) {
        return Found;
      }
      add_result = add_to_howl(card_set, card_region, card_in_region, increment_total);
      break;
    }
    default:
      ShouldNotReachHere();
  }

  return add_result;
}

G1CardSetHashTableValue* G1CardSet::get_or_add_card_set(uint card_region, bool* should_grow_table) {
  return _table->get_or_add(card_region, should_grow_table);
}

G1CardSet::CardSetPtr G1CardSet::get_card_set(uint card_region) {
  return _table->get(card_region);
}

G1AddCardResult G1CardSet::add_card(uint card_region, uint card_in_region, bool increment_total) {
  G1AddCardResult add_result;
  CardSetPtr to_transfer = nullptr;
  CardSetPtr card_set;

  bool should_grow_table = false;
  G1CardSetHashTableValue* table_entry = get_or_add_card_set(card_region, &should_grow_table);
  while (true) {
    card_set = acquire_card_set(&table_entry->_card_set);
    add_result = add_to_card_set(&table_entry->_card_set, card_set, card_region, card_in_region, increment_total);

    if (add_result != Overflow) {
      break;
    }
    // Card set has overflown. Coarsen or retry.
    bool coarsened = coarsen_card_set(&table_entry->_card_set, card_set, card_in_region);
    _coarsen_stats.record_coarsening(card_set_type(card_set), !coarsened);
    if (coarsened) {
      // We have been the one coarsening this card set (and in the process added that card).
      add_result = Added;
      to_transfer = card_set;
      break;
    }
    // Somebody else beat us to coarsening. Retry.
    release_and_maybe_free_card_set(card_set);
  }

  if (increment_total && add_result == Added) {
    Atomic::inc(&table_entry->_num_occupied, memory_order_relaxed);
    Atomic::inc(&_num_occupied, memory_order_relaxed);
  }
  if (should_grow_table) {
    _table->grow();
  }
  if (to_transfer != nullptr) {
    transfer_cards(table_entry, to_transfer, card_region);
  }

  release_and_maybe_free_card_set(card_set);

  return add_result;
}

bool G1CardSet::contains_card(uint card_region, uint card_in_region) {
  assert(card_in_region < _config->max_cards_in_region(),
         "Card %u is beyond max %u", card_in_region, _config->max_cards_in_region());

  // Protect the card set from reclamation.
  GlobalCounter::CriticalSection cs(Thread::current());
  CardSetPtr card_set = get_card_set(card_region);
  if (card_set == nullptr) {
    return false;
  } else if (card_set == FullCardSet) {
    // contains_card() is not a performance critical method so we do not hide that
    // case in the switch below.
    return true;
  }

  switch (card_set_type(card_set)) {
    case CardSetInlinePtr: {
      G1CardSetInlinePtr ptr(card_set);
      return ptr.contains(card_in_region, _config->inline_ptr_bits_per_card());
    }
    case CardSetArrayOfCards :  return card_set_ptr<G1CardSetArray>(card_set)->contains(card_in_region);
    case CardSetBitMap: return card_set_ptr<G1CardSetBitMap>(card_set)->contains(card_in_region, _config->num_cards_in_howl_bitmap());
    case CardSetHowl: {
      G1CardSetHowl* howling_array = card_set_ptr<G1CardSetHowl>(card_set);

      return howling_array->contains(card_in_region, _config);
    }
  }
  ShouldNotReachHere();
  return false;
}

void G1CardSet::print_info(outputStream* st, uint card_region, uint card_in_region) {
  CardSetPtr card_set = get_card_set(card_region);
  if (card_set == nullptr) {
    st->print("NULL card set");
    return;
  } else if (card_set == FullCardSet) {
    st->print("FULL card set)");
    return;
  }
  switch (card_set_type(card_set)) {
    case CardSetInlinePtr: {
      st->print("InlinePtr not containing %u", card_in_region);
      break;
    }
    case CardSetArrayOfCards :  {
      st->print("AoC not containing %u", card_in_region);
      break;
    }
    case CardSetBitMap: {
      st->print("BitMap not containing %u", card_in_region);
      break;
    }
    case CardSetHowl: {
      st->print("CardSetHowl not containing %u", card_in_region);
      break;
    }
    default: st->print("Unknown card set type %u", card_set_type(card_set)); ShouldNotReachHere(); break;
  }
}

template <class CardVisitor>
void G1CardSet::iterate_cards_during_transfer(CardSetPtr const card_set, CardVisitor& found) {
  uint type = card_set_type(card_set);
  assert(type == CardSetInlinePtr || type == CardSetArrayOfCards,
         "invalid card set type %d to transfer from",
         card_set_type(card_set));

  switch (type) {
    case CardSetInlinePtr: {
      G1CardSetInlinePtr ptr(card_set);
      ptr.iterate(found, _config->inline_ptr_bits_per_card());
      return;
    }
    case CardSetArrayOfCards : {
      card_set_ptr<G1CardSetArray>(card_set)->iterate(found);
      return;
    }
    default:
      ShouldNotReachHere();
  }
}

void G1CardSet::iterate_containers(G1CardSetPtrIterator* found, bool at_safepoint) {
  if (at_safepoint) {
    _table->iterate_safepoint(found);
  } else {
    _table->iterate(found);
  }
}

template <typename Closure>
class G1ContainerCards {
  Closure& _iter;
  uint _region_idx;

public:
  G1ContainerCards(Closure& iter, uint region_idx) : _iter(iter), _region_idx(region_idx) { }

  bool start_iterate(uint tag) { return true; }

  void operator()(uint card_idx) {
    _iter.do_card(_region_idx, card_idx);
  }

  void operator()(uint card_idx, uint length) {
    for (uint i = 0; i < length; i++) {
      _iter.do_card(_region_idx, card_idx);
    }
  }
};

void G1CardSet::iterate_cards(G1CardSetCardIterator& iter) {
  G1CardSetMergeCardIterator<G1CardSetCardIterator, G1ContainerCards> cl(this, iter);
  iterate_containers(&cl);
}

bool G1CardSet::occupancy_less_or_equal_to(size_t limit) const {
  return occupied() <= limit;
}

bool G1CardSet::is_empty() const {
  return _num_occupied == 0;
}

size_t G1CardSet::occupied() const {
  return _num_occupied;
}

size_t G1CardSet::num_containers() {
  class GetNumberOfContainers : public G1CardSetPtrIterator {
  public:
    size_t _count;

    GetNumberOfContainers() : G1CardSetPtrIterator(), _count(0) { }

    void do_cardsetptr(uint region_idx, size_t num_occupied, CardSetPtr card_set) override {
      _count++;
    }
  } cl;

  iterate_containers(&cl);
  return cl._count;
}

G1CardSetCoarsenStats G1CardSet::coarsen_stats() {
  return _coarsen_stats;
}

void G1CardSet::print_coarsen_stats(outputStream* out) {
  _last_coarsen_stats.subtract_from(_coarsen_stats);
  out->print("Coarsening (recent): ");
  _last_coarsen_stats.print_on(out);
  out->print("Coarsening (all): ");
  _coarsen_stats.print_on(out);
}

size_t G1CardSet::mem_size() const {
  return sizeof(*this) +
         _table->mem_size() +
         _mm->mem_size();
}

size_t G1CardSet::wasted_mem_size() const {
  return _mm->wasted_mem_size();
}

size_t G1CardSet::static_mem_size() {
  return sizeof(FullCardSet) + sizeof(_coarsen_stats);
}

void G1CardSet::clear() {
  _table->reset();
  _num_occupied = 0;
  _mm->flush();
}

void G1CardSet::print(outputStream* os) {
  _table->print(os);
  _mm->print(os);
}
