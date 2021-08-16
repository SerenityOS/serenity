/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OOPSTORAGE_INLINE_HPP
#define SHARE_GC_SHARED_OOPSTORAGE_INLINE_HPP

#include "gc/shared/oopStorage.hpp"

#include "memory/allocation.hpp"
#include "metaprogramming/conditional.hpp"
#include "metaprogramming/isConst.hpp"
#include "oops/oop.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/align.hpp"
#include "utilities/count_trailing_zeros.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

// Array of all active blocks.  Refcounted for lock-free reclaim of
// old array when a new array is allocated for expansion.
class OopStorage::ActiveArray {
  friend class OopStorage::TestAccess;

  size_t _size;
  volatile size_t _block_count;
  mutable volatile int _refcount;
  // Block* _blocks[1];            // Pseudo flexible array member.

  ActiveArray(size_t size);
  ~ActiveArray();

  NONCOPYABLE(ActiveArray);

  static size_t blocks_offset();
  Block* const* base_ptr() const;

  Block* const* block_ptr(size_t index) const;
  Block** block_ptr(size_t index);

public:
  static ActiveArray* create(size_t size,
                             MEMFLAGS memflags = mtGC,
                             AllocFailType alloc_fail = AllocFailStrategy::EXIT_OOM);
  static void destroy(ActiveArray* ba);

  inline Block* at(size_t i) const;

  size_t size() const;
  size_t block_count() const;
  size_t block_count_acquire() const;
  void increment_refcount() const;
  bool decrement_refcount() const; // Return true if zero, otherwise false

  // Support for OopStorage::allocate.
  // Add block to the end of the array.  Updates block count at the
  // end of the operation, with a release_store. Returns true if the
  // block was added, false if there was no room available.
  // precondition: owner's _allocation_mutex is locked, or at safepoint.
  bool push(Block* block);

  // Support OopStorage::delete_empty_blocks_xxx operations.
  // Remove block from the array.
  // precondition: block must be present at its active_index element.
  void remove(Block* block);

  void copy_from(const ActiveArray* from);
};

inline size_t OopStorage::ActiveArray::blocks_offset() {
  return align_up(sizeof(ActiveArray), sizeof(Block*));
}

inline OopStorage::Block* const* OopStorage::ActiveArray::base_ptr() const {
  const void* ptr = reinterpret_cast<const char*>(this) + blocks_offset();
  return reinterpret_cast<Block* const*>(ptr);
}

inline OopStorage::Block* const* OopStorage::ActiveArray::block_ptr(size_t index) const {
  return base_ptr() + index;
}

inline OopStorage::Block** OopStorage::ActiveArray::block_ptr(size_t index) {
  return const_cast<Block**>(base_ptr() + index);
}

inline OopStorage::Block* OopStorage::ActiveArray::at(size_t index) const {
  assert(index < _block_count, "precondition");
  return *block_ptr(index);
}

// A Block has an embedded AllocationListEntry to provide the links between
// Blocks in an AllocationList.
class OopStorage::AllocationListEntry {
  friend class OopStorage::AllocationList;

  // Members are mutable, and we deal exclusively with pointers to
  // const, to make const blocks easier to use; a block being const
  // doesn't prevent modifying its list state.
  mutable const Block* _prev;
  mutable const Block* _next;

  NONCOPYABLE(AllocationListEntry);

public:
  AllocationListEntry();
  ~AllocationListEntry();
};

// Fixed-sized array of oops, plus bookkeeping data.
// All blocks are in the storage's _active_array, at the block's _active_index.
// Non-full blocks are in the storage's _allocation_list, linked through the
// block's _allocation_list_entry.  Empty blocks are at the end of that list.
class OopStorage::Block /* No base class, to avoid messing up alignment. */ {
  // _data must be the first non-static data member, for alignment.
  oop _data[BitsPerWord];
  static const unsigned _data_pos = 0; // Position of _data.

  volatile uintx _allocated_bitmask; // One bit per _data element.
  intptr_t _owner_address;
  void* _memory;              // Unaligned storage containing block.
  size_t _active_index;
  AllocationListEntry _allocation_list_entry;
  Block* volatile _deferred_updates_next;
  volatile uintx _release_refcount;

  Block(const OopStorage* owner, void* memory);
  ~Block();

  void check_index(unsigned index) const;
  unsigned get_index(const oop* ptr) const;
  void atomic_add_allocated(uintx add);

  template<typename F, typename BlockPtr>
  static bool iterate_impl(F f, BlockPtr b);

  NONCOPYABLE(Block);

public:
  const AllocationListEntry& allocation_list_entry() const;

  static size_t allocation_size();
  static size_t allocation_alignment_shift();

  oop* get_pointer(unsigned index);
  const oop* get_pointer(unsigned index) const;

  uintx bitmask_for_index(unsigned index) const;
  uintx bitmask_for_entry(const oop* ptr) const;

  // Allocation bitmask accessors are racy.
  bool is_full() const;
  bool is_empty() const;
  uintx allocated_bitmask() const;

  bool is_safe_to_delete() const;

  Block* deferred_updates_next() const;
  void set_deferred_updates_next(Block* new_next);

  bool contains(const oop* ptr) const;

  size_t active_index() const;
  void set_active_index(size_t index);
  static size_t active_index_safe(const Block* block); // Returns 0 if access fails.

  // Returns NULL if ptr is not in a block or not allocated in that block.
  static Block* block_for_ptr(const OopStorage* owner, const oop* ptr);

  oop* allocate();
  uintx allocate_all();
  static Block* new_block(const OopStorage* owner);
  static void delete_block(const Block& block);

  void release_entries(uintx releasing, OopStorage* owner);

  template<typename F> bool iterate(F f);
  template<typename F> bool iterate(F f) const;
}; // class Block

inline OopStorage::Block* OopStorage::AllocationList::head() {
  return const_cast<Block*>(_head);
}

inline OopStorage::Block* OopStorage::AllocationList::tail() {
  return const_cast<Block*>(_tail);
}

inline const OopStorage::Block* OopStorage::AllocationList::chead() const {
  return _head;
}

inline const OopStorage::Block* OopStorage::AllocationList::ctail() const {
  return _tail;
}

inline OopStorage::Block* OopStorage::AllocationList::prev(Block& block) {
  return const_cast<Block*>(block.allocation_list_entry()._prev);
}

inline OopStorage::Block* OopStorage::AllocationList::next(Block& block) {
  return const_cast<Block*>(block.allocation_list_entry()._next);
}

inline const OopStorage::Block* OopStorage::AllocationList::prev(const Block& block) const {
  return block.allocation_list_entry()._prev;
}

inline const OopStorage::Block* OopStorage::AllocationList::next(const Block& block) const {
  return block.allocation_list_entry()._next;
}

template<typename Closure>
class OopStorage::OopFn {
public:
  explicit OopFn(Closure* cl) : _cl(cl) {}

  template<typename OopPtr>     // [const] oop*
  bool operator()(OopPtr ptr) const {
    _cl->do_oop(ptr);
    return true;
  }

private:
  Closure* _cl;
};

template<typename Closure>
inline OopStorage::OopFn<Closure> OopStorage::oop_fn(Closure* cl) {
  return OopFn<Closure>(cl);
}

template<typename IsAlive, typename F>
class OopStorage::IfAliveFn {
public:
  IfAliveFn(IsAlive* is_alive, F f) : _is_alive(is_alive), _f(f) {}

  bool operator()(oop* ptr) const {
    bool result = true;
    oop v = *ptr;
    if (v != NULL) {
      if (_is_alive->do_object_b(v)) {
        result = _f(ptr);
      } else {
        *ptr = NULL;            // Clear dead value.
      }
    }
    return result;
  }

private:
  IsAlive* _is_alive;
  F _f;
};

template<typename IsAlive, typename F>
inline OopStorage::IfAliveFn<IsAlive, F> OopStorage::if_alive_fn(IsAlive* is_alive, F f) {
  return IfAliveFn<IsAlive, F>(is_alive, f);
}

template<typename F>
class OopStorage::SkipNullFn {
public:
  SkipNullFn(F f) : _f(f) {}

  template<typename OopPtr>     // [const] oop*
  bool operator()(OopPtr ptr) const {
    return (*ptr != NULL) ? _f(ptr) : true;
  }

private:
  F _f;
};

template<typename F>
inline OopStorage::SkipNullFn<F> OopStorage::skip_null_fn(F f) {
  return SkipNullFn<F>(f);
}

// Inline Block accesses for use in iteration loops.

inline const OopStorage::AllocationListEntry& OopStorage::Block::allocation_list_entry() const {
  return _allocation_list_entry;
}

inline void OopStorage::Block::check_index(unsigned index) const {
  assert(index < ARRAY_SIZE(_data), "Index out of bounds: %u", index);
}

inline oop* OopStorage::Block::get_pointer(unsigned index) {
  check_index(index);
  return &_data[index];
}

inline const oop* OopStorage::Block::get_pointer(unsigned index) const {
  check_index(index);
  return &_data[index];
}

inline uintx OopStorage::Block::allocated_bitmask() const {
  return _allocated_bitmask;
}

inline uintx OopStorage::Block::bitmask_for_index(unsigned index) const {
  check_index(index);
  return uintx(1) << index;
}

// Provide const or non-const iteration, depending on whether BlockPtr
// is const Block* or Block*, respectively.
template<typename F, typename BlockPtr> // BlockPtr := [const] Block*
inline bool OopStorage::Block::iterate_impl(F f, BlockPtr block) {
  uintx bitmask = block->allocated_bitmask();
  while (bitmask != 0) {
    unsigned index = count_trailing_zeros(bitmask);
    bitmask ^= block->bitmask_for_index(index);
    if (!f(block->get_pointer(index))) {
      return false;
    }
  }
  return true;
}

template<typename F>
inline bool OopStorage::Block::iterate(F f) {
  return iterate_impl(f, this);
}

template<typename F>
inline bool OopStorage::Block::iterate(F f) const {
  return iterate_impl(f, this);
}

//////////////////////////////////////////////////////////////////////////////
// Support for serial iteration, always at a safepoint.

// Provide const or non-const iteration, depending on whether Storage is
// const OopStorage* or OopStorage*, respectively.
template<typename F, typename Storage> // Storage := [const] OopStorage
inline bool OopStorage::iterate_impl(F f, Storage* storage) {
  assert_at_safepoint();
  // Propagate const/non-const iteration to the block layer, by using
  // const or non-const blocks as corresponding to Storage.
  typedef typename Conditional<IsConst<Storage>::value, const Block*, Block*>::type BlockPtr;
  ActiveArray* blocks = storage->_active_array;
  size_t limit = blocks->block_count();
  for (size_t i = 0; i < limit; ++i) {
    BlockPtr block = blocks->at(i);
    if (!block->iterate(f)) {
      return false;
    }
  }
  return true;
}

template<typename F>
inline bool OopStorage::iterate_safepoint(F f) {
  return iterate_impl(f, this);
}

template<typename F>
inline bool OopStorage::iterate_safepoint(F f) const {
  return iterate_impl(f, this);
}

template<typename Closure>
inline void OopStorage::oops_do(Closure* cl) {
  iterate_safepoint(oop_fn(cl));
}

template<typename Closure>
inline void OopStorage::oops_do(Closure* cl) const {
  iterate_safepoint(oop_fn(cl));
}

template<typename Closure>
inline void OopStorage::weak_oops_do(Closure* cl) {
  iterate_safepoint(skip_null_fn(oop_fn(cl)));
}

template<typename IsAliveClosure, typename Closure>
inline void OopStorage::weak_oops_do(IsAliveClosure* is_alive, Closure* cl) {
  iterate_safepoint(if_alive_fn(is_alive, oop_fn(cl)));
}

#endif // SHARE_GC_SHARED_OOPSTORAGE_INLINE_HPP
