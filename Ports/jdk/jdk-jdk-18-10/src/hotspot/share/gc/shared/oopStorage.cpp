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

#include "precompiled.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/os.hpp"
#include "runtime/safefetch.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/count_trailing_zeros.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"
#include "utilities/population_count.hpp"
#include "utilities/powerOfTwo.hpp"

OopStorage::AllocationListEntry::AllocationListEntry() : _prev(NULL), _next(NULL) {}

OopStorage::AllocationListEntry::~AllocationListEntry() {
  assert(_prev == NULL, "deleting attached block");
  assert(_next == NULL, "deleting attached block");
}

OopStorage::AllocationList::AllocationList() : _head(NULL), _tail(NULL) {}

OopStorage::AllocationList::~AllocationList() {
  // ~OopStorage() empties its lists before destroying them.
  assert(_head == NULL, "deleting non-empty block list");
  assert(_tail == NULL, "deleting non-empty block list");
}

void OopStorage::AllocationList::push_front(const Block& block) {
  const Block* old = _head;
  if (old == NULL) {
    assert(_tail == NULL, "invariant");
    _head = _tail = &block;
  } else {
    block.allocation_list_entry()._next = old;
    old->allocation_list_entry()._prev = &block;
    _head = &block;
  }
}

void OopStorage::AllocationList::push_back(const Block& block) {
  const Block* old = _tail;
  if (old == NULL) {
    assert(_head == NULL, "invariant");
    _head = _tail = &block;
  } else {
    old->allocation_list_entry()._next = &block;
    block.allocation_list_entry()._prev = old;
    _tail = &block;
  }
}

void OopStorage::AllocationList::unlink(const Block& block) {
  const AllocationListEntry& block_entry = block.allocation_list_entry();
  const Block* prev_blk = block_entry._prev;
  const Block* next_blk = block_entry._next;
  block_entry._prev = NULL;
  block_entry._next = NULL;
  if ((prev_blk == NULL) && (next_blk == NULL)) {
    assert(_head == &block, "invariant");
    assert(_tail == &block, "invariant");
    _head = _tail = NULL;
  } else if (prev_blk == NULL) {
    assert(_head == &block, "invariant");
    next_blk->allocation_list_entry()._prev = NULL;
    _head = next_blk;
  } else if (next_blk == NULL) {
    assert(_tail == &block, "invariant");
    prev_blk->allocation_list_entry()._next = NULL;
    _tail = prev_blk;
  } else {
    next_blk->allocation_list_entry()._prev = prev_blk;
    prev_blk->allocation_list_entry()._next = next_blk;
  }
}

bool OopStorage::AllocationList::contains(const Block& block) const {
  return (next(block) != NULL) || (ctail() == &block);
}

OopStorage::ActiveArray::ActiveArray(size_t size) :
  _size(size),
  _block_count(0),
  _refcount(0)
{}

OopStorage::ActiveArray::~ActiveArray() {
  assert(_refcount == 0, "precondition");
}

OopStorage::ActiveArray* OopStorage::ActiveArray::create(size_t size,
                                                         MEMFLAGS memflags,
                                                         AllocFailType alloc_fail) {
  size_t size_in_bytes = blocks_offset() + sizeof(Block*) * size;
  void* mem = NEW_C_HEAP_ARRAY3(char, size_in_bytes, memflags, CURRENT_PC, alloc_fail);
  if (mem == NULL) return NULL;
  return new (mem) ActiveArray(size);
}

void OopStorage::ActiveArray::destroy(ActiveArray* ba) {
  ba->~ActiveArray();
  FREE_C_HEAP_ARRAY(char, ba);
}

size_t OopStorage::ActiveArray::size() const {
  return _size;
}

size_t OopStorage::ActiveArray::block_count() const {
  return _block_count;
}

size_t OopStorage::ActiveArray::block_count_acquire() const {
  return Atomic::load_acquire(&_block_count);
}

void OopStorage::ActiveArray::increment_refcount() const {
  int new_value = Atomic::add(&_refcount, 1);
  assert(new_value >= 1, "negative refcount %d", new_value - 1);
}

bool OopStorage::ActiveArray::decrement_refcount() const {
  int new_value = Atomic::sub(&_refcount, 1);
  assert(new_value >= 0, "negative refcount %d", new_value);
  return new_value == 0;
}

bool OopStorage::ActiveArray::push(Block* block) {
  size_t index = _block_count;
  if (index < _size) {
    block->set_active_index(index);
    *block_ptr(index) = block;
    // Use a release_store to ensure all the setup is complete before
    // making the block visible.
    Atomic::release_store(&_block_count, index + 1);
    return true;
  } else {
    return false;
  }
}

void OopStorage::ActiveArray::remove(Block* block) {
  assert(_block_count > 0, "array is empty");
  size_t index = block->active_index();
  assert(*block_ptr(index) == block, "block not present");
  size_t last_index = _block_count - 1;
  Block* last_block = *block_ptr(last_index);
  last_block->set_active_index(index);
  *block_ptr(index) = last_block;
  _block_count = last_index;
}

void OopStorage::ActiveArray::copy_from(const ActiveArray* from) {
  assert(_block_count == 0, "array must be empty");
  size_t count = from->_block_count;
  assert(count <= _size, "precondition");
  Block* const* from_ptr = from->block_ptr(0);
  Block** to_ptr = block_ptr(0);
  for (size_t i = 0; i < count; ++i) {
    Block* block = *from_ptr++;
    assert(block->active_index() == i, "invariant");
    *to_ptr++ = block;
  }
  _block_count = count;
}

// Blocks start with an array of BitsPerWord oop entries.  That array
// is divided into conceptual BytesPerWord sections of BitsPerByte
// entries.  Blocks are allocated aligned on section boundaries, for
// the convenience of mapping from an entry to the containing block;
// see block_for_ptr().  Aligning on section boundary rather than on
// the full _data wastes a lot less space, but makes for a bit more
// work in block_for_ptr().

const unsigned section_size = BitsPerByte;
const unsigned section_count = BytesPerWord;
const unsigned block_alignment = sizeof(oop) * section_size;

OopStorage::Block::Block(const OopStorage* owner, void* memory) :
  _data(),
  _allocated_bitmask(0),
  _owner_address(reinterpret_cast<intptr_t>(owner)),
  _memory(memory),
  _active_index(0),
  _allocation_list_entry(),
  _deferred_updates_next(NULL),
  _release_refcount(0)
{
  STATIC_ASSERT(_data_pos == 0);
  STATIC_ASSERT(section_size * section_count == ARRAY_SIZE(_data));
  assert(offset_of(Block, _data) == _data_pos, "invariant");
  assert(owner != NULL, "NULL owner");
  assert(is_aligned(this, block_alignment), "misaligned block");
}

OopStorage::Block::~Block() {
  assert(_release_refcount == 0, "deleting block while releasing");
  assert(_deferred_updates_next == NULL, "deleting block with deferred update");
  // Clear fields used by block_for_ptr and entry validation, which
  // might help catch bugs.  Volatile to prevent dead-store elimination.
  const_cast<uintx volatile&>(_allocated_bitmask) = 0;
  const_cast<intptr_t volatile&>(_owner_address) = 0;
}

size_t OopStorage::Block::allocation_size() {
  // _data must be first member, so aligning Block aligns _data.
  STATIC_ASSERT(_data_pos == 0);
  return sizeof(Block) + block_alignment - sizeof(void*);
}

size_t OopStorage::Block::allocation_alignment_shift() {
  return exact_log2(block_alignment);
}

static inline bool is_full_bitmask(uintx bitmask) { return ~bitmask == 0; }
static inline bool is_empty_bitmask(uintx bitmask) { return bitmask == 0; }

bool OopStorage::Block::is_full() const {
  return is_full_bitmask(allocated_bitmask());
}

bool OopStorage::Block::is_empty() const {
  return is_empty_bitmask(allocated_bitmask());
}

uintx OopStorage::Block::bitmask_for_entry(const oop* ptr) const {
  return bitmask_for_index(get_index(ptr));
}

// An empty block is not yet deletable if either:
// (1) There is a release() operation currently operating on it.
// (2) It is in the deferred updates list.
// For interaction with release(), these must follow the empty check,
// and the order of these checks is important.
bool OopStorage::Block::is_safe_to_delete() const {
  assert(is_empty(), "precondition");
  OrderAccess::loadload();
  return (Atomic::load_acquire(&_release_refcount) == 0) &&
         (Atomic::load_acquire(&_deferred_updates_next) == NULL);
}

OopStorage::Block* OopStorage::Block::deferred_updates_next() const {
  return _deferred_updates_next;
}

void OopStorage::Block::set_deferred_updates_next(Block* block) {
  _deferred_updates_next = block;
}

bool OopStorage::Block::contains(const oop* ptr) const {
  const oop* base = get_pointer(0);
  return (base <= ptr) && (ptr < (base + ARRAY_SIZE(_data)));
}

size_t OopStorage::Block::active_index() const {
  return _active_index;
}

void OopStorage::Block::set_active_index(size_t index) {
  _active_index = index;
}

size_t OopStorage::Block::active_index_safe(const Block* block) {
  STATIC_ASSERT(sizeof(intptr_t) == sizeof(block->_active_index));
  assert(CanUseSafeFetchN(), "precondition");
  return SafeFetchN((intptr_t*)&block->_active_index, 0);
}

unsigned OopStorage::Block::get_index(const oop* ptr) const {
  assert(contains(ptr), PTR_FORMAT " not in block " PTR_FORMAT, p2i(ptr), p2i(this));
  return static_cast<unsigned>(ptr - get_pointer(0));
}

// Merge new allocation bits into _allocated_bitmask.  Only one thread at a
// time is ever allocating from a block, but other threads may concurrently
// release entries and clear bits in _allocated_bitmask.
// precondition: _allocated_bitmask & add == 0
void OopStorage::Block::atomic_add_allocated(uintx add) {
  // Since the current allocated bitmask has no set bits in common with add,
  // we can use an atomic add to implement the operation.  The assert post
  // facto verifies the precondition held; if there were any set bits in
  // common, then after the add at least one of them will be zero.
  uintx sum = Atomic::add(&_allocated_bitmask, add);
  assert((sum & add) == add, "some already present: " UINTX_FORMAT ":" UINTX_FORMAT,
         sum, add);
}

oop* OopStorage::Block::allocate() {
  uintx allocated = allocated_bitmask();
  assert(!is_full_bitmask(allocated), "attempt to allocate from full block");
  unsigned index = count_trailing_zeros(~allocated);
  // Use atomic update because release may change bitmask.
  atomic_add_allocated(bitmask_for_index(index));
  return get_pointer(index);
}

uintx OopStorage::Block::allocate_all() {
  uintx new_allocated = ~allocated_bitmask();
  assert(new_allocated != 0, "attempt to allocate from full block");
  // Use atomic update because release may change bitmask.
  atomic_add_allocated(new_allocated);
  return new_allocated;
}

OopStorage::Block* OopStorage::Block::new_block(const OopStorage* owner) {
  // _data must be first member: aligning block => aligning _data.
  STATIC_ASSERT(_data_pos == 0);
  size_t size_needed = allocation_size();
  void* memory = NEW_C_HEAP_ARRAY_RETURN_NULL(char, size_needed, owner->memflags());
  if (memory == NULL) {
    return NULL;
  }
  void* block_mem = align_up(memory, block_alignment);
  assert(sizeof(Block) + pointer_delta(block_mem, memory, 1) <= size_needed,
         "allocated insufficient space for aligned block");
  return ::new (block_mem) Block(owner, memory);
}

void OopStorage::Block::delete_block(const Block& block) {
  void* memory = block._memory;
  block.Block::~Block();
  FREE_C_HEAP_ARRAY(char, memory);
}

// This can return a false positive if ptr is not contained by some
// block.  For some uses, it is a precondition that ptr is valid,
// e.g. contained in some block in owner's _active_array.  Other uses
// require additional validation of the result.
OopStorage::Block*
OopStorage::Block::block_for_ptr(const OopStorage* owner, const oop* ptr) {
  assert(CanUseSafeFetchN(), "precondition");
  STATIC_ASSERT(_data_pos == 0);
  // Const-ness of ptr is not related to const-ness of containing block.
  // Blocks are allocated section-aligned, so get the containing section.
  oop* section_start = align_down(const_cast<oop*>(ptr), block_alignment);
  // Start with a guess that the containing section is the last section,
  // so the block starts section_count-1 sections earlier.
  oop* section = section_start - (section_size * (section_count - 1));
  // Walk up through the potential block start positions, looking for
  // the owner in the expected location.  If we're below the actual block
  // start position, the value at the owner position will be some oop
  // (possibly NULL), which can never match the owner.
  intptr_t owner_addr = reinterpret_cast<intptr_t>(owner);
  for (unsigned i = 0; i < section_count; ++i, section += section_size) {
    Block* candidate = reinterpret_cast<Block*>(section);
    if (SafeFetchN(&candidate->_owner_address, 0) == owner_addr) {
      return candidate;
    }
  }
  return NULL;
}

//////////////////////////////////////////////////////////////////////////////
// Allocation
//
// Allocation involves the _allocation_list, which contains a subset of the
// blocks owned by a storage object.  This is a doubly-linked list, linked
// through dedicated fields in the blocks.  Full blocks are removed from this
// list, though they are still present in the _active_array.  Empty blocks are
// kept at the end of the _allocation_list, to make it easy for empty block
// deletion to find them.
//
// allocate(), and delete_empty_blocks() lock the
// _allocation_mutex while performing any list and array modifications.
//
// allocate() and release() update a block's _allocated_bitmask using CAS
// loops.  This prevents loss of updates even though release() performs
// its updates without any locking.
//
// allocate() obtains the entry from the first block in the _allocation_list,
// and updates that block's _allocated_bitmask to indicate the entry is in
// use.  If this makes the block full (all entries in use), the block is
// removed from the _allocation_list so it won't be considered by future
// allocations until some entries in it are released.
//
// release() is performed lock-free. (Note: This means it can't notify the
// service thread of pending cleanup work.  It must be lock-free because
// it is called in all kinds of contexts where even quite low ranked locks
// may be held.)  release() first looks up the block for
// the entry, using address alignment to find the enclosing block (thereby
// avoiding iteration over the _active_array).  Once the block has been
// determined, its _allocated_bitmask needs to be updated, and its position in
// the _allocation_list may need to be updated.  There are two cases:
//
// (a) If the block is neither full nor would become empty with the release of
// the entry, only its _allocated_bitmask needs to be updated.  But if the CAS
// update fails, the applicable case may change for the retry.
//
// (b) Otherwise, the _allocation_list also needs to be modified.  This requires
// locking the _allocation_mutex.  To keep the release() operation lock-free,
// rather than updating the _allocation_list itself, it instead performs a
// lock-free push of the block onto the _deferred_updates list.  Entries on
// that list are processed by allocate() and delete_empty_blocks(), while
// they already hold the necessary lock.  That processing makes the block's
// list state consistent with its current _allocated_bitmask.  The block is
// added to the _allocation_list if not already present and the bitmask is not
// full.  The block is moved to the end of the _allocation_list if the bitmask
// is empty, for ease of empty block deletion processing.

oop* OopStorage::allocate() {
  MutexLocker ml(_allocation_mutex, Mutex::_no_safepoint_check_flag);

  Block* block = block_for_allocation();
  if (block == NULL) return NULL; // Block allocation failed.
  assert(!block->is_full(), "invariant");
  if (block->is_empty()) {
    // Transitioning from empty to not empty.
    log_block_transition(block, "not empty");
  }
  oop* result = block->allocate();
  assert(result != NULL, "allocation failed");
  assert(!block->is_empty(), "postcondition");
  Atomic::inc(&_allocation_count); // release updates outside lock.
  if (block->is_full()) {
    // Transitioning from not full to full.
    // Remove full blocks from consideration by future allocates.
    log_block_transition(block, "full");
    _allocation_list.unlink(*block);
  }
  log_trace(oopstorage, ref)("%s: allocated " PTR_FORMAT, name(), p2i(result));
  return result;
}

// Bulk allocation takes the first block off the _allocation_list, and marks
// all remaining entries in that block as allocated.  It then drops the lock
// and fills buffer with those newly allocated entries.  If more entries
// were obtained than requested, the remaining entries are released back
// (which is a lock-free operation).  Finally, the number actually added to
// the buffer is returned.  It's best to request at least as many entries as
// a single block can provide, to avoid the release case.  That number is
// available as bulk_allocate_limit.
size_t OopStorage::allocate(oop** ptrs, size_t size) {
  assert(size > 0, "precondition");
  Block* block;
  uintx taken;
  {
    MutexLocker ml(_allocation_mutex, Mutex::_no_safepoint_check_flag);
    block = block_for_allocation();
    if (block == NULL) return 0; // Block allocation failed.
    // Taking all remaining entries, so remove from list.
    _allocation_list.unlink(*block);
    // Transitioning from empty to not empty.
    if (block->is_empty()) {
      log_block_transition(block, "not empty");
    }
    taken = block->allocate_all();
    // Safe to drop the lock, since we have claimed our entries.
    assert(!is_empty_bitmask(taken), "invariant");
  } // Drop lock, now that we've taken all available entries from block.
  size_t num_taken = population_count(taken);
  Atomic::add(&_allocation_count, num_taken);
  // Fill ptrs from those taken entries.
  size_t limit = MIN2(num_taken, size);
  for (size_t i = 0; i < limit; ++i) {
    assert(taken != 0, "invariant");
    unsigned index = count_trailing_zeros(taken);
    taken ^= block->bitmask_for_index(index);
    ptrs[i] = block->get_pointer(index);
  }
  // If more entries taken than requested, release remainder.
  if (taken == 0) {
    assert(num_taken == limit, "invariant");
  } else {
    assert(size == limit, "invariant");
    assert(num_taken == (limit + population_count(taken)), "invariant");
    block->release_entries(taken, this);
    Atomic::sub(&_allocation_count, num_taken - limit);
  }
  log_trace(oopstorage, ref)("%s: bulk allocate %zu, returned %zu",
                             name(), limit, num_taken - limit);
  return limit;                 // Return number allocated.
}

void OopStorage::log_block_transition(Block* block, const char* new_state) const {
  log_trace(oopstorage, blocks)("%s: block %s " PTR_FORMAT, name(), new_state, p2i(block));
}

bool OopStorage::try_add_block() {
  assert_lock_strong(_allocation_mutex);
  Block* block;
  {
    MutexUnlocker ul(_allocation_mutex, Mutex::_no_safepoint_check_flag);
    block = Block::new_block(this);
  }
  if (block == NULL) return false;

  // Add new block to the _active_array, growing if needed.
  if (!_active_array->push(block)) {
    if (expand_active_array()) {
      guarantee(_active_array->push(block), "push failed after expansion");
    } else {
      log_debug(oopstorage, blocks)("%s: failed active array expand", name());
      Block::delete_block(*block);
      return false;
    }
  }
  // Add to end of _allocation_list.  The mutex release allowed other
  // threads to add blocks to the _allocation_list.  We prefer to
  // allocate from non-empty blocks, to allow empty blocks to be
  // deleted.  But we don't bother notifying about the empty block
  // because we're (probably) about to allocate an entry from it.
  _allocation_list.push_back(*block);
  log_debug(oopstorage, blocks)("%s: new block " PTR_FORMAT, name(), p2i(block));
  return true;
}

OopStorage::Block* OopStorage::block_for_allocation() {
  assert_lock_strong(_allocation_mutex);
  while (true) {
    // Use the first block in _allocation_list for the allocation.
    Block* block = _allocation_list.head();
    if (block != NULL) {
      return block;
    } else if (reduce_deferred_updates()) {
      // Might have added a block to the _allocation_list, so retry.
    } else if (try_add_block()) {
      // Successfully added a new block to the list, so retry.
      assert(_allocation_list.chead() != NULL, "invariant");
    } else if (_allocation_list.chead() != NULL) {
      // Trying to add a block failed, but some other thread added to the
      // list while we'd dropped the lock over the new block allocation.
    } else if (!reduce_deferred_updates()) { // Once more before failure.
      // Attempt to add a block failed, no other thread added a block,
      // and no deferred updated added a block, then allocation failed.
      log_info(oopstorage, blocks)("%s: failed block allocation", name());
      return NULL;
    }
  }
}

// Create a new, larger, active array with the same content as the
// current array, and then replace, relinquishing the old array.
// Return true if the array was successfully expanded, false to
// indicate allocation failure.
bool OopStorage::expand_active_array() {
  assert_lock_strong(_allocation_mutex);
  ActiveArray* old_array = _active_array;
  size_t new_size = 2 * old_array->size();
  log_debug(oopstorage, blocks)("%s: expand active array " SIZE_FORMAT,
                                name(), new_size);
  ActiveArray* new_array = ActiveArray::create(new_size,
                                               memflags(),
                                               AllocFailStrategy::RETURN_NULL);
  if (new_array == NULL) return false;
  new_array->copy_from(old_array);
  replace_active_array(new_array);
  relinquish_block_array(old_array);
  return true;
}

// Make new_array the _active_array.  Increments new_array's refcount
// to account for the new reference.  The assignment is atomic wrto
// obtain_active_array; once this function returns, it is safe for the
// caller to relinquish the old array.
void OopStorage::replace_active_array(ActiveArray* new_array) {
  // Caller has the old array that is the current value of _active_array.
  // Update new_array refcount to account for the new reference.
  new_array->increment_refcount();
  // Install new_array, ensuring its initialization is complete first.
  Atomic::release_store(&_active_array, new_array);
  // Wait for any readers that could read the old array from _active_array.
  // Can't use GlobalCounter here, because this is called from allocate(),
  // which may be called in the scope of a GlobalCounter critical section
  // when inserting a StringTable entry.
  _protect_active.synchronize();
  // All obtain critical sections that could see the old array have
  // completed, having incremented the refcount of the old array.  The
  // caller can now safely relinquish the old array.
}

// Atomically (wrto replace_active_array) get the active array and
// increment its refcount.  This provides safe access to the array,
// even if an allocate operation expands and replaces the value of
// _active_array.  The caller must relinquish the array when done
// using it.
OopStorage::ActiveArray* OopStorage::obtain_active_array() const {
  SingleWriterSynchronizer::CriticalSection cs(&_protect_active);
  ActiveArray* result = Atomic::load_acquire(&_active_array);
  result->increment_refcount();
  return result;
}

// Decrement refcount of array and destroy if refcount is zero.
void OopStorage::relinquish_block_array(ActiveArray* array) const {
  if (array->decrement_refcount()) {
    assert(array != _active_array, "invariant");
    ActiveArray::destroy(array);
  }
}

class OopStorage::WithActiveArray : public StackObj {
  const OopStorage* _storage;
  ActiveArray* _active_array;

public:
  WithActiveArray(const OopStorage* storage) :
    _storage(storage),
    _active_array(storage->obtain_active_array())
  {}

  ~WithActiveArray() {
    _storage->relinquish_block_array(_active_array);
  }

  ActiveArray& active_array() const {
    return *_active_array;
  }
};

OopStorage::Block* OopStorage::find_block_or_null(const oop* ptr) const {
  assert(ptr != NULL, "precondition");
  return Block::block_for_ptr(this, ptr);
}

static void log_release_transitions(uintx releasing,
                                    uintx old_allocated,
                                    const OopStorage* owner,
                                    const void* block) {
  LogTarget(Trace, oopstorage, blocks) lt;
  if (lt.is_enabled()) {
    LogStream ls(lt);
    if (is_full_bitmask(old_allocated)) {
      ls.print_cr("%s: block not full " PTR_FORMAT, owner->name(), p2i(block));
    }
    if (releasing == old_allocated) {
      ls.print_cr("%s: block empty " PTR_FORMAT, owner->name(), p2i(block));
    }
  }
}

void OopStorage::Block::release_entries(uintx releasing, OopStorage* owner) {
  assert(releasing != 0, "preconditon");
  // Prevent empty block deletion when transitioning to empty.
  Atomic::inc(&_release_refcount);

  // Atomically update allocated bitmask.
  uintx old_allocated = _allocated_bitmask;
  while (true) {
    assert((releasing & ~old_allocated) == 0, "releasing unallocated entries");
    uintx new_value = old_allocated ^ releasing;
    uintx fetched = Atomic::cmpxchg(&_allocated_bitmask, old_allocated, new_value);
    if (fetched == old_allocated) break; // Successful update.
    old_allocated = fetched;             // Retry with updated bitmask.
  }

  // Now that the bitmask has been updated, if we have a state transition
  // (updated bitmask is empty or old bitmask was full), atomically push
  // this block onto the deferred updates list.  Some future call to
  // reduce_deferred_updates will make any needed changes related to this
  // block and _allocation_list.  This deferral avoids _allocation_list
  // updates and the associated locking here.
  if ((releasing == old_allocated) || is_full_bitmask(old_allocated)) {
    // Log transitions.  Both transitions are possible in a single update.
    log_release_transitions(releasing, old_allocated, owner, this);
    // Attempt to claim responsibility for adding this block to the deferred
    // list, by setting the link to non-NULL by self-looping.  If this fails,
    // then someone else has made such a claim and the deferred update has not
    // yet been processed and will include our change, so we don't need to do
    // anything further.
    if (Atomic::replace_if_null(&_deferred_updates_next, this)) {
      // Successfully claimed.  Push, with self-loop for end-of-list.
      Block* head = owner->_deferred_updates;
      while (true) {
        _deferred_updates_next = (head == NULL) ? this : head;
        Block* fetched = Atomic::cmpxchg(&owner->_deferred_updates, head, this);
        if (fetched == head) break; // Successful update.
        head = fetched;             // Retry with updated head.
      }
      // Only request cleanup for to-empty transitions, not for from-full.
      // There isn't any rush to process from-full transitions.  Allocation
      // will reduce deferrals before allocating new blocks, so may process
      // some.  And the service thread will drain the entire deferred list
      // if there are any pending to-empty transitions.
      if (releasing == old_allocated) {
        owner->record_needs_cleanup();
      }
      log_trace(oopstorage, blocks)("%s: deferred update " PTR_FORMAT,
                                    owner->name(), p2i(this));
    }
  }
  // Release hold on empty block deletion.
  Atomic::dec(&_release_refcount);
}

// Process one available deferred update.  Returns true if one was processed.
bool OopStorage::reduce_deferred_updates() {
  assert_lock_strong(_allocation_mutex);
  // Atomically pop a block off the list, if any available.
  // No ABA issue because this is only called by one thread at a time.
  // The atomicity is wrto pushes by release().
  Block* block = Atomic::load_acquire(&_deferred_updates);
  while (true) {
    if (block == NULL) return false;
    // Try atomic pop of block from list.
    Block* tail = block->deferred_updates_next();
    if (block == tail) tail = NULL; // Handle self-loop end marker.
    Block* fetched = Atomic::cmpxchg(&_deferred_updates, block, tail);
    if (fetched == block) break; // Update successful.
    block = fetched;             // Retry with updated block.
  }
  block->set_deferred_updates_next(NULL); // Clear tail after updating head.
  // Ensure bitmask read after pop is complete, including clearing tail, for
  // ordering with release().  Without this, we may be processing a stale
  // bitmask state here while blocking a release() operation from recording
  // the deferred update needed for its bitmask change.
  OrderAccess::fence();
  // Make list state consistent with bitmask state.
  uintx allocated = block->allocated_bitmask();
  if (is_full_bitmask(allocated)) {
    // If full then it shouldn't be in the list, and should stay that way.
    assert(!_allocation_list.contains(*block), "invariant");
  } else if (_allocation_list.contains(*block)) {
    // Block is in list.  If empty, move to the end for possible deletion.
    if (is_empty_bitmask(allocated)) {
      _allocation_list.unlink(*block);
      _allocation_list.push_back(*block);
    }
  } else if (is_empty_bitmask(allocated)) {
    // Block is empty and not in list. Add to back for possible deletion.
    _allocation_list.push_back(*block);
  } else {
    // Block is neither full nor empty, and not in list.  Add to front.
    _allocation_list.push_front(*block);
  }

  log_trace(oopstorage, blocks)("%s: processed deferred update " PTR_FORMAT,
                                name(), p2i(block));
  return true;              // Processed one pending update.
}

static inline void check_release_entry(const oop* entry) {
  assert(entry != NULL, "Releasing NULL");
  assert(*entry == NULL, "Releasing uncleared entry: " PTR_FORMAT, p2i(entry));
}

void OopStorage::release(const oop* ptr) {
  check_release_entry(ptr);
  Block* block = find_block_or_null(ptr);
  assert(block != NULL, "%s: invalid release " PTR_FORMAT, name(), p2i(ptr));
  log_trace(oopstorage, ref)("%s: releasing " PTR_FORMAT, name(), p2i(ptr));
  block->release_entries(block->bitmask_for_entry(ptr), this);
  Atomic::dec(&_allocation_count);
}

void OopStorage::release(const oop* const* ptrs, size_t size) {
  size_t i = 0;
  while (i < size) {
    check_release_entry(ptrs[i]);
    Block* block = find_block_or_null(ptrs[i]);
    assert(block != NULL, "%s: invalid release " PTR_FORMAT, name(), p2i(ptrs[i]));
    size_t count = 0;
    uintx releasing = 0;
    for ( ; i < size; ++i) {
      const oop* entry = ptrs[i];
      check_release_entry(entry);
      // If entry not in block, finish block and resume outer loop with entry.
      if (!block->contains(entry)) break;
      // Add entry to releasing bitmap.
      log_trace(oopstorage, ref)("%s: releasing " PTR_FORMAT, name(), p2i(entry));
      uintx entry_bitmask = block->bitmask_for_entry(entry);
      assert((releasing & entry_bitmask) == 0,
             "Duplicate entry: " PTR_FORMAT, p2i(entry));
      releasing |= entry_bitmask;
      ++count;
    }
    // Release the contiguous entries that are in block.
    block->release_entries(releasing, this);
    Atomic::sub(&_allocation_count, count);
  }
}

const size_t initial_active_array_size = 8;

static Mutex* make_oopstorage_mutex(const char* storage_name,
                                    const char* kind,
                                    int rank) {
  char name[256];
  os::snprintf(name, sizeof(name), "%s %s lock", storage_name, kind);
  return new PaddedMutex(rank, name, true, Mutex::_safepoint_check_never);
}

void* OopStorage::operator new(size_t size, MEMFLAGS memflags) {
  assert(size >= sizeof(OopStorage), "precondition");
  return NEW_C_HEAP_ARRAY(char, size, memflags);
}

void OopStorage::operator delete(void* obj, MEMFLAGS /* memflags */) {
  FREE_C_HEAP_ARRAY(char, obj);
}

OopStorage::OopStorage(const char* name, MEMFLAGS memflags) :
  _name(os::strdup(name)),
  _active_array(ActiveArray::create(initial_active_array_size, memflags)),
  _allocation_list(),
  _deferred_updates(NULL),
  _allocation_mutex(make_oopstorage_mutex(name, "alloc", Mutex::oopstorage)),
  _active_mutex(make_oopstorage_mutex(name, "active", Mutex::oopstorage - 1)),
  _num_dead_callback(NULL),
  _allocation_count(0),
  _concurrent_iteration_count(0),
  _memflags(memflags),
  _needs_cleanup(false)
{
  _active_array->increment_refcount();
  assert(_active_mutex->rank() < _allocation_mutex->rank(),
         "%s: active_mutex must have lower rank than allocation_mutex", _name);
  assert(Service_lock->rank() < _active_mutex->rank(),
         "%s: active_mutex must have higher rank than Service_lock", _name);
  assert(_active_mutex->_safepoint_check_required == Mutex::_safepoint_check_never,
         "%s: active mutex requires never safepoint check", _name);
  assert(_allocation_mutex->_safepoint_check_required == Mutex::_safepoint_check_never,
         "%s: allocation mutex requires never safepoint check", _name);
}

void OopStorage::delete_empty_block(const Block& block) {
  assert(block.is_empty(), "discarding non-empty block");
  log_debug(oopstorage, blocks)("%s: delete empty block " PTR_FORMAT, name(), p2i(&block));
  Block::delete_block(block);
}

OopStorage::~OopStorage() {
  Block* block;
  while ((block = _deferred_updates) != NULL) {
    _deferred_updates = block->deferred_updates_next();
    block->set_deferred_updates_next(NULL);
  }
  while ((block = _allocation_list.head()) != NULL) {
    _allocation_list.unlink(*block);
  }
  bool unreferenced = _active_array->decrement_refcount();
  assert(unreferenced, "deleting storage while _active_array is referenced");
  for (size_t i = _active_array->block_count(); 0 < i; ) {
    block = _active_array->at(--i);
    Block::delete_block(*block);
  }
  ActiveArray::destroy(_active_array);
  os::free(const_cast<char*>(_name));
}

void OopStorage::register_num_dead_callback(NumDeadCallback f) {
  assert(_num_dead_callback == NULL, "Only one callback function supported");
  _num_dead_callback = f;
}

void OopStorage::report_num_dead(size_t num_dead) const {
  if (_num_dead_callback != NULL) {
    _num_dead_callback(num_dead);
  }
}

bool OopStorage::should_report_num_dead() const {
  return _num_dead_callback != NULL;
}

// Managing service thread notifications.
//
// We don't want cleanup work to linger indefinitely, but we also don't want
// to run the service thread too often.  We're also very limited in what we
// can do in a release operation, where cleanup work is created.
//
// When a release operation changes a block's state to empty, it records the
// need for cleanup in both the associated storage object and in the global
// request state.  A safepoint cleanup task notifies the service thread when
// there may be cleanup work for any storage object, based on the global
// request state.  But that notification is deferred if the service thread
// has run recently, and we also avoid duplicate notifications.  The service
// thread updates the timestamp and resets the state flags on every iteration.

// Global cleanup request state.
static volatile bool needs_cleanup_requested = false;

// Flag for avoiding duplicate notifications.
static bool needs_cleanup_triggered = false;

// Time after which a notification can be made.
static jlong cleanup_trigger_permit_time = 0;

// Minimum time since last service thread check before notification is
// permitted.  The value of 500ms was an arbitrary choice; frequent, but not
// too frequent.
const jlong cleanup_trigger_defer_period = 500 * NANOSECS_PER_MILLISEC;

void OopStorage::trigger_cleanup_if_needed() {
  MonitorLocker ml(Service_lock, Monitor::_no_safepoint_check_flag);
  if (Atomic::load(&needs_cleanup_requested) &&
      !needs_cleanup_triggered &&
      (os::javaTimeNanos() > cleanup_trigger_permit_time)) {
    needs_cleanup_triggered = true;
    ml.notify_all();
  }
}

bool OopStorage::has_cleanup_work_and_reset() {
  assert_lock_strong(Service_lock);
  cleanup_trigger_permit_time =
    os::javaTimeNanos() + cleanup_trigger_defer_period;
  needs_cleanup_triggered = false;
  // Set the request flag false and return its old value.
  // Needs to be atomic to avoid dropping a concurrent request.
  // Can't use Atomic::xchg, which may not support bool.
  return Atomic::cmpxchg(&needs_cleanup_requested, true, false);
}

// Record that cleanup is needed, without notifying the Service thread.
// Used by release(), where we can't lock even Service_lock.
void OopStorage::record_needs_cleanup() {
  // Set local flag first, else service thread could wake up and miss
  // the request.  This order may instead (rarely) unnecessarily notify.
  Atomic::release_store(&_needs_cleanup, true);
  Atomic::release_store_fence(&needs_cleanup_requested, true);
}

bool OopStorage::delete_empty_blocks() {
  // Service thread might have oopstorage work, but not for this object.
  // Check for deferred updates even though that's not a service thread
  // trigger; since we're here, we might as well process them.
  if (!Atomic::load_acquire(&_needs_cleanup) &&
      (Atomic::load_acquire(&_deferred_updates) == NULL)) {
    return false;
  }

  MutexLocker ml(_allocation_mutex, Mutex::_no_safepoint_check_flag);

  // Clear the request before processing.
  Atomic::release_store_fence(&_needs_cleanup, false);

  // Other threads could be adding to the empty block count or the
  // deferred update list while we're working.  Set an upper bound on
  // how many updates we'll process and blocks we'll try to release,
  // so other threads can't cause an unbounded stay in this function.
  // We add a bit of slop because the reduce_deferred_updates clause
  // can cause blocks to be double counted.  If there are few blocks
  // and many of them are deferred and empty, we might hit the limit
  // and spin the caller without doing very much work.  Otherwise,
  // we don't normally hit the limit anyway, instead running out of
  // work to do.
  size_t limit = block_count() + 10;

  for (size_t i = 0; i < limit; ++i) {
    // Process deferred updates, which might make empty blocks available.
    // Continue checking once deletion starts, since additional updates
    // might become available while we're working.
    if (reduce_deferred_updates()) {
      // Be safepoint-polite while looping.
      MutexUnlocker ul(_allocation_mutex, Mutex::_no_safepoint_check_flag);
      ThreadBlockInVM tbiv(JavaThread::current());
    } else {
      Block* block = _allocation_list.tail();
      if ((block == NULL) || !block->is_empty()) {
        return false;
      } else if (!block->is_safe_to_delete()) {
        // Look for other work while waiting for block to be deletable.
        break;
      }

      // Try to delete the block.  First, try to remove from _active_array.
      {
        MutexLocker aml(_active_mutex, Mutex::_no_safepoint_check_flag);
        // Don't interfere with an active concurrent iteration.
        // Instead, give up immediately.  There is more work to do,
        // but don't re-notify, to avoid useless spinning of the
        // service thread.  Instead, iteration completion notifies.
        if (_concurrent_iteration_count > 0) return true;
        _active_array->remove(block);
      }
      // Remove block from _allocation_list and delete it.
      _allocation_list.unlink(*block);
      // Be safepoint-polite while deleting and looping.
      MutexUnlocker ul(_allocation_mutex, Mutex::_no_safepoint_check_flag);
      delete_empty_block(*block);
      ThreadBlockInVM tbiv(JavaThread::current());
    }
  }
  // Exceeded work limit or can't delete last block.  This will
  // cause the service thread to loop, giving other subtasks an
  // opportunity to run too.  There's no need for a notification,
  // because we are part of the service thread (unless gtesting).
  record_needs_cleanup();
  return true;
}

OopStorage::EntryStatus OopStorage::allocation_status(const oop* ptr) const {
  const Block* block = find_block_or_null(ptr);
  if (block != NULL) {
    // Prevent block deletion and _active_array modification.
    MutexLocker ml(_allocation_mutex, Mutex::_no_safepoint_check_flag);
    // Block could be a false positive, so get index carefully.
    size_t index = Block::active_index_safe(block);
    if ((index < _active_array->block_count()) &&
        (block == _active_array->at(index)) &&
        block->contains(ptr)) {
      if ((block->allocated_bitmask() & block->bitmask_for_entry(ptr)) != 0) {
        return ALLOCATED_ENTRY;
      } else {
        return UNALLOCATED_ENTRY;
      }
    }
  }
  return INVALID_ENTRY;
}

size_t OopStorage::allocation_count() const {
  return _allocation_count;
}

size_t OopStorage::block_count() const {
  WithActiveArray wab(this);
  // Count access is racy, but don't care.
  return wab.active_array().block_count();
}

size_t OopStorage::total_memory_usage() const {
  size_t total_size = sizeof(OopStorage);
  total_size += strlen(name()) + 1;
  total_size += sizeof(ActiveArray);
  WithActiveArray wab(this);
  const ActiveArray& blocks = wab.active_array();
  // Count access is racy, but don't care.
  total_size += blocks.block_count() * Block::allocation_size();
  total_size += blocks.size() * sizeof(Block*);
  return total_size;
}

MEMFLAGS OopStorage::memflags() const { return _memflags; }

// Parallel iteration support

uint OopStorage::BasicParState::default_estimated_thread_count(bool concurrent) {
  uint configured = concurrent ? ConcGCThreads : ParallelGCThreads;
  return MAX2(1u, configured);  // Never estimate zero threads.
}

OopStorage::BasicParState::BasicParState(const OopStorage* storage,
                                         uint estimated_thread_count,
                                         bool concurrent) :
  _storage(storage),
  _active_array(_storage->obtain_active_array()),
  _block_count(0),              // initialized properly below
  _next_block(0),
  _estimated_thread_count(estimated_thread_count),
  _concurrent(concurrent),
  _num_dead(0)
{
  assert(estimated_thread_count > 0, "estimated thread count must be positive");
  update_concurrent_iteration_count(1);
  // Get the block count *after* iteration state updated, so concurrent
  // empty block deletion is suppressed and can't reduce the count.  But
  // ensure the count we use was written after the block with that count
  // was fully initialized; see ActiveArray::push.
  _block_count = _active_array->block_count_acquire();
}

OopStorage::BasicParState::~BasicParState() {
  _storage->relinquish_block_array(_active_array);
  update_concurrent_iteration_count(-1);
  if (_concurrent) {
    // We may have deferred some cleanup work.
    const_cast<OopStorage*>(_storage)->record_needs_cleanup();
  }
}

void OopStorage::BasicParState::update_concurrent_iteration_count(int value) {
  if (_concurrent) {
    MutexLocker ml(_storage->_active_mutex, Mutex::_no_safepoint_check_flag);
    _storage->_concurrent_iteration_count += value;
    assert(_storage->_concurrent_iteration_count >= 0, "invariant");
  }
}

bool OopStorage::BasicParState::claim_next_segment(IterationData* data) {
  data->_processed += data->_segment_end - data->_segment_start;
  size_t start = Atomic::load_acquire(&_next_block);
  if (start >= _block_count) {
    return finish_iteration(data); // No more blocks available.
  }
  // Try to claim several at a time, but not *too* many.  We want to
  // avoid deciding there are many available and selecting a large
  // quantity, get delayed, and then end up claiming most or all of
  // the remaining largish amount of work, leaving nothing for other
  // threads to do.  But too small a step can lead to contention
  // over _next_block, esp. when the work per block is small.
  size_t max_step = 10;
  size_t remaining = _block_count - start;
  size_t step = MIN2(max_step, 1 + (remaining / _estimated_thread_count));
  // Atomic::add with possible overshoot.  This can perform better
  // than a CAS loop on some platforms when there is contention.
  // We can cope with the uncertainty by recomputing start/end from
  // the result of the add, and dealing with potential overshoot.
  size_t end = Atomic::add(&_next_block, step);
  // _next_block may have changed, so recompute start from result of add.
  start = end - step;
  // _next_block may have changed so much that end has overshot.
  end = MIN2(end, _block_count);
  // _next_block may have changed so much that even start has overshot.
  if (start < _block_count) {
    // Record claimed segment for iteration.
    data->_segment_start = start;
    data->_segment_end = end;
    return true;                // Success.
  } else {
    // No more blocks to claim.
    return finish_iteration(data);
  }
}

bool OopStorage::BasicParState::finish_iteration(const IterationData* data) const {
  log_info(oopstorage, blocks, stats)
          ("Parallel iteration on %s: blocks = " SIZE_FORMAT
           ", processed = " SIZE_FORMAT " (%2.f%%)",
           _storage->name(), _block_count, data->_processed,
           percent_of(data->_processed, _block_count));
  return false;
}

size_t OopStorage::BasicParState::num_dead() const {
  return Atomic::load(&_num_dead);
}

void OopStorage::BasicParState::increment_num_dead(size_t num_dead) {
  Atomic::add(&_num_dead, num_dead);
}

void OopStorage::BasicParState::report_num_dead() const {
  _storage->report_num_dead(Atomic::load(&_num_dead));
}

const char* OopStorage::name() const { return _name; }

#ifndef PRODUCT

void OopStorage::print_on(outputStream* st) const {
  size_t allocations = _allocation_count;
  size_t blocks = _active_array->block_count();

  double data_size = section_size * section_count;
  double alloc_percentage = percent_of((double)allocations, blocks * data_size);

  st->print("%s: " SIZE_FORMAT " entries in " SIZE_FORMAT " blocks (%.F%%), " SIZE_FORMAT " bytes",
            name(), allocations, blocks, alloc_percentage, total_memory_usage());
  if (_concurrent_iteration_count > 0) {
    st->print(", concurrent iteration active");
  }
}

#endif // !PRODUCT
