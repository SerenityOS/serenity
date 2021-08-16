/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/storage/jfrVirtualMemory.hpp"
#include "memory/virtualspace.hpp"
#include "runtime/globals.hpp"
#include "runtime/os.hpp"
#include "services/memTracker.hpp"
#include "utilities/globalDefinitions.hpp"

/*
 * A memory segment represents a virtual memory reservation.
 * It provides ways to commit and decommit physical storage
 * onto its virtual memory reservation.
 */

class JfrVirtualMemorySegment : public JfrCHeapObj {
  friend class JfrVirtualMemoryManager;
 private:
  JfrVirtualMemorySegment* _next;
  char* _top;
  ReservedSpace _rs;
  VirtualSpace  _virtual_memory;

  // Convenience functions to access the underlying virtual space metadata
  const u1* committed_low()  const { return (const u1*)_virtual_memory.low(); }
  const u1* committed_high() const { return (const u1*)_virtual_memory.high(); }
  const u1* reserved_low() const { return (const u1*)_virtual_memory.low_boundary(); }
  const u1* reserved_high() const { return (const u1*)_virtual_memory.high_boundary(); }
  size_t reserved_words() const  { return _virtual_memory.reserved_size() / BytesPerWord; }
  size_t committed_words() const { return _virtual_memory.actual_committed_size() / BytesPerWord; }
  bool is_pre_committed() const { return _virtual_memory.special(); }
  VirtualSpace& virtual_space() { return _virtual_memory; }

  JfrVirtualMemorySegment();
  ~JfrVirtualMemorySegment();

  JfrVirtualMemorySegment* next() const { return _next; }
  void set_next(JfrVirtualMemorySegment* v) { _next = v; }

  // Returns true if requested size is available in the committed area
  bool is_available(size_t block_size_request_words) {
    return block_size_request_words <= pointer_delta(committed_high(), _top, sizeof(char*));
  }

  // allocation pointer committed memory
  char* top() const { return _top; }
  void inc_top(size_t size_in_words) {
    assert(is_available(size_in_words), "invariant");
    _top += size_in_words * BytesPerWord;
    assert(_top <= _virtual_memory.high(), "invariant");
  }

  // initialization is the virtual memory reservation
  bool initialize(size_t reservation_size_request_bytes);
  void* take_from_committed(size_t block_size_request_words);

  // Returns committed memory
  void* commit(size_t block_size_request_words) {
    return take_from_committed(block_size_request_words);
  }

  // Commit more memory in a reservation
  bool expand_by(size_t block_size_request_words);

  // Decommits all committed memory in this reservation segment.
  void decommit();
};

JfrVirtualMemorySegment::JfrVirtualMemorySegment() :
  _next(NULL),
  _top(NULL),
  _rs(),
  _virtual_memory() {}

JfrVirtualMemorySegment::~JfrVirtualMemorySegment() {
  decommit();
  _rs.release();
}

bool JfrVirtualMemorySegment::initialize(size_t reservation_size_request_bytes) {
  assert(is_aligned(reservation_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  _rs = ReservedSpace(reservation_size_request_bytes,
                      os::vm_allocation_granularity(),
                      os::vm_page_size());
  if (!_rs.is_reserved()) {
    return false;
  }
  assert(_rs.base() != NULL, "invariant");
  assert(_rs.size() != 0, "invariant");
  assert(is_aligned(_rs.base(), os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(_rs.size(), os::vm_allocation_granularity()), "invariant");
  os::trace_page_sizes("Jfr", reservation_size_request_bytes,
                              reservation_size_request_bytes,
                              os::vm_page_size(),
                              _rs.base(),
                              _rs.size());
  MemTracker::record_virtual_memory_type((address)_rs.base(), mtTracing);
  assert(is_aligned(_rs.base(), os::vm_page_size()), "invariant");
  assert(is_aligned(_rs.size(), os::vm_page_size()), "invariant");

  // ReservedSpaces marked as special will have the entire memory
  // pre-committed. Setting a committed size will make sure that
  // committed_size and actual_committed_size agrees.
  const size_t pre_committed_size = _rs.special() ? _rs.size() : 0;
  const bool result = virtual_space().initialize_with_granularity(_rs, pre_committed_size, os::vm_page_size());

  if (result) {
    assert(virtual_space().committed_size() == virtual_space().actual_committed_size(),
      "Checking that the pre-committed memory was registered by the VirtualSpace");
    _top = virtual_space().low();
  }
  return result;
}

bool JfrVirtualMemorySegment::expand_by(size_t block_size_request_words) {
  size_t block_size_request_bytes = block_size_request_words * BytesPerWord;
  const size_t uncommitted = virtual_space().reserved_size() - virtual_space().actual_committed_size();
  if (uncommitted < block_size_request_bytes) {
    // commit whatever is left in the reservation
    block_size_request_bytes = uncommitted;
  }
  assert(is_aligned(block_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  // commit block in reserved memory
  bool result = virtual_space().expand_by(block_size_request_bytes, false);
  assert(result, "Failed to commit memory");
  return result;
}

void JfrVirtualMemorySegment::decommit() {
  assert(_virtual_memory.committed_size() == _virtual_memory.actual_committed_size(),
    "The committed memory doesn't match the expanded memory.");

  const size_t committed_size = virtual_space().actual_committed_size();
  if (committed_size > 0) {
    virtual_space().shrink_by(committed_size);
  }

  assert(_virtual_memory.actual_committed_size() == 0, "invariant");
}

// Attempt to get a committed block
void* JfrVirtualMemorySegment::take_from_committed(size_t block_size_request_words) {
  // The virtual spaces are always expanded by the
  // commit granularity to enforce the following condition.
  // Without this the is_available check will not work correctly.
  assert(_virtual_memory.committed_size() == _virtual_memory.actual_committed_size(),
    "The committed memory doesn't match the expanded memory.");
  if (!is_available(block_size_request_words)) {
    return NULL;
  }
  void* const block = top();
  assert(block != NULL, "invariant");
  inc_top(block_size_request_words);
  return block;
}

class JfrVirtualMemoryManager : public JfrCHeapObj {
 typedef JfrVirtualMemorySegment Segment;
 private:
  Segment* _segments;
  Segment* _current_segment;
  size_t _reservation_size_request_words;
  size_t _reservation_size_request_limit_words; // total reservation limit

  // Sum of reserved and committed memory in the segments
  size_t _current_reserved_words;
  size_t _current_committed_words;

  void link(Segment* segment);
  Segment* current();

  void inc_reserved_words(size_t words);
  void inc_committed_words(size_t words);

  bool new_segment(size_t reservation_size_request_words);

  bool expand_segment_by(Segment* segment, size_t block_size_request_words);

  bool expand_by(size_t block_size_request_words, size_t reservation_size_request_words);
  bool can_reserve() const;

 public:
  JfrVirtualMemoryManager();
  ~JfrVirtualMemoryManager();

  bool initialize(size_t reservation_size_request_words, size_t segment_count = 1);
  void* commit(size_t requested_block_size_words);

  bool is_full() const {
    return reserved_high() == committed_high();
  }

  u1* top() const { return reinterpret_cast<u1*>(_current_segment->top()); }
  const u1* committed_low() const { return _current_segment->committed_low(); }
  const u1* committed_high() const { return _current_segment->committed_high(); }
  const u1* reserved_low() const { return _current_segment->reserved_low(); }
  const u1* reserved_high() const { return _current_segment->reserved_high(); }
};

JfrVirtualMemoryManager::JfrVirtualMemoryManager() :
  _segments(NULL),
  _current_segment(NULL),
  _reservation_size_request_words(0),
  _reservation_size_request_limit_words(0),
  _current_reserved_words(0),
  _current_committed_words(0) {}

JfrVirtualMemoryManager::~JfrVirtualMemoryManager() {
  JfrVirtualMemorySegment* segment = _segments;
  while (segment != NULL) {
    JfrVirtualMemorySegment* next_segment = segment->next();
    delete segment;
    segment = next_segment;
  }
}

// for now only allow a singleton segment per virtual memory client
bool JfrVirtualMemoryManager::initialize(size_t reservation_size_request_words, size_t segment_count /* 1 */) {
  assert(is_aligned(reservation_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  _reservation_size_request_words = reservation_size_request_words;
  assert(segment_count > 0, "invariant");
  _reservation_size_request_limit_words = reservation_size_request_words * segment_count;
  assert(is_aligned(_reservation_size_request_limit_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  return new_segment(_reservation_size_request_words);
}

bool JfrVirtualMemoryManager::can_reserve() const  {
  return _reservation_size_request_limit_words == 0 ? true : _current_reserved_words < _reservation_size_request_limit_words;
}

// Allocate another segment and add it to the list.
bool JfrVirtualMemoryManager::new_segment(size_t reservation_size_request_words) {
  assert(reservation_size_request_words > 0, "invariant");
  assert(is_aligned(reservation_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  Segment* segment = new Segment();
  if (NULL == segment) {
    return false;
  }
  if (!segment->initialize(reservation_size_request_words * BytesPerWord)) {
    delete segment;
    return false;
  }
  assert(segment->reserved_words() == reservation_size_request_words,
    "Actual reserved memory size differs from requested reservation memory size");
  link(segment);
  return true;
}

bool JfrVirtualMemoryManager::expand_segment_by(JfrVirtualMemorySegment* segment, size_t block_size_request_words) {
  assert(segment != NULL, "invariant");
  const size_t before = segment->committed_words();
  const bool result = segment->expand_by(block_size_request_words);
  const size_t after = segment->committed_words();
  // after and before can be the same if the memory was pre-committed.
  assert(after >= before, "Inconsistency");
  inc_committed_words(after - before);
  return result;
}

void JfrVirtualMemoryManager::inc_reserved_words(size_t words) {
  _current_reserved_words += words;
}

JfrVirtualMemorySegment* JfrVirtualMemoryManager::current() {
  return _current_segment;
}

void JfrVirtualMemoryManager::inc_committed_words(size_t words) {
  _current_committed_words += words;
}

bool JfrVirtualMemoryManager::expand_by(size_t block_size_request_words, size_t reservation_size_request_words) {
  assert(is_aligned(block_size_request_words * BytesPerWord, os::vm_page_size()), "invariant");
  assert(is_aligned(block_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(reservation_size_request_words * BytesPerWord, os::vm_page_size()), "invariant");
  assert(is_aligned(reservation_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  assert(block_size_request_words <= reservation_size_request_words, "invariant");
  // Attempt to commit more memory from the the current virtual space reservation.
  if (expand_segment_by(current(), block_size_request_words)) {
    return true;
  }

  // reached limit of what is allowed to be reserved?
  if (!can_reserve()) {
    return false;
  }

  // Get another segment.
  if (!new_segment(reservation_size_request_words)) {
    return false;
  }

  if (current()->is_pre_committed()) {
    // The memory was pre-committed, so we are done here.
    assert(block_size_request_words <= current()->committed_words(),
           "The new VirtualSpace was pre-committed, so it"
           "should be large enough to fit the alloc request.");
    return true;
  }
  return expand_segment_by(current(), block_size_request_words);
}

void JfrVirtualMemoryManager::link(JfrVirtualMemorySegment* segment) {
  assert(segment != NULL, "invariant");
  if (_segments == NULL) {
    _segments = segment;
  } else {
    assert(_current_segment != NULL, "invariant");
    assert(_segments == _current_segment, "invariant");
    _current_segment->set_next(segment);
  }
  _current_segment = segment;
  inc_reserved_words(segment->reserved_words());
  inc_committed_words(segment->committed_words());
}

void* JfrVirtualMemoryManager::commit(size_t block_size_request_words) {
  assert(is_aligned(block_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  void* block = current()->commit(block_size_request_words);
  if (block != NULL) {
    return block;
  }
  assert(block == NULL, "invariant");
  if (is_full()) {
    return NULL;
  }
  assert(block_size_request_words <= _reservation_size_request_words, "invariant");
  if (expand_by(block_size_request_words, _reservation_size_request_words)) {
    block = current()->commit(block_size_request_words);
    assert(block != NULL, "The allocation was expected to succeed after the expansion");
  }
  return block;
}

JfrVirtualMemory::JfrVirtualMemory() :
  _vmm(NULL),
  _reserved_low(),
  _reserved_high(),
  _top(NULL),
  _commit_point(NULL),
  _physical_commit_size_request_words(0),
  _aligned_datum_size_bytes(0) {}

JfrVirtualMemory::~JfrVirtualMemory() {
  assert(_vmm != NULL, "invariant");
  delete _vmm;
}

size_t JfrVirtualMemory::aligned_datum_size_bytes() const {
  return _aligned_datum_size_bytes;
}

static void adjust_allocation_ratio(size_t* const reservation_size_bytes, size_t* const commit_size_bytes) {
  assert(reservation_size_bytes != NULL, "invariant");
  assert(*reservation_size_bytes > 0, "invariant");
  assert(commit_size_bytes != NULL, "invariant");
  assert(*commit_size_bytes > 0, "invariant");
  assert(*reservation_size_bytes >= *commit_size_bytes, "invariant");
  assert(is_aligned(*reservation_size_bytes, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(*commit_size_bytes, os::vm_allocation_granularity()), "invariant");

  size_t reservation_size_units = *reservation_size_bytes / os::vm_allocation_granularity();
  size_t commit_size_units = *commit_size_bytes / os::vm_allocation_granularity();
  assert(reservation_size_units > 0, "invariant");
  assert(commit_size_units > 0, "invariant");

  size_t original_ratio_units = reservation_size_units / commit_size_units;
  size_t rem = reservation_size_units % commit_size_units;
  assert(original_ratio_units > 0, "invariant");

  if (rem > 0) {
    reservation_size_units -= rem % original_ratio_units;
    commit_size_units += rem / original_ratio_units;
  }

  assert(commit_size_units > 0, "invariant");
  assert(reservation_size_units % original_ratio_units == 0, "invariant");
  assert(original_ratio_units * commit_size_units == reservation_size_units , "invariant");
  assert(original_ratio_units == reservation_size_units / commit_size_units, "invariant");
  *reservation_size_bytes = reservation_size_units * os::vm_allocation_granularity();
  *commit_size_bytes = commit_size_units * os::vm_allocation_granularity();
  assert((*reservation_size_bytes % *commit_size_bytes) == 0, "invariant");
}


void* JfrVirtualMemory::initialize(size_t reservation_size_request_bytes,
                                   size_t block_size_request_bytes,
                                   size_t datum_size_bytes /* 1 */) {
  assert(_vmm == NULL, "invariant");
  _vmm = new JfrVirtualMemoryManager();

  if (_vmm == NULL) {
    return NULL;
  }

  assert(reservation_size_request_bytes > 0, "invariant");
  _aligned_datum_size_bytes = align_up(datum_size_bytes, BytesPerWord);
  assert(is_aligned(_aligned_datum_size_bytes, BytesPerWord), "invariant");

  reservation_size_request_bytes = ReservedSpace::allocation_align_size_up(reservation_size_request_bytes);
  assert(is_aligned(reservation_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(reservation_size_request_bytes, _aligned_datum_size_bytes), "invariant");
  block_size_request_bytes = MAX2(block_size_request_bytes, (size_t)os::vm_allocation_granularity());
  block_size_request_bytes = ReservedSpace::allocation_align_size_up(block_size_request_bytes);
  assert(is_aligned(block_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(block_size_request_bytes, _aligned_datum_size_bytes), "invariant");
  // adjustment to valid ratio in units of vm_allocation_granularity
  adjust_allocation_ratio(&reservation_size_request_bytes, &block_size_request_bytes);
  assert(is_aligned(reservation_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(reservation_size_request_bytes, _aligned_datum_size_bytes), "invariant");
  assert(is_aligned(block_size_request_bytes, os::vm_allocation_granularity()), "invariant");
  assert(is_aligned(block_size_request_bytes, _aligned_datum_size_bytes), "invariant");
  assert((reservation_size_request_bytes % block_size_request_bytes) == 0, "invariant");
  const size_t reservation_size_request_words = reservation_size_request_bytes / BytesPerWord;
  _physical_commit_size_request_words = block_size_request_bytes / BytesPerWord;
  // virtual memory reservation
  if (!_vmm->initialize(reservation_size_request_words)) {
    // is implicitly "full" if reservation fails
    assert(is_full(), "invariant");
    return NULL;
  }
  _reserved_low = (const u1*)_vmm->reserved_low();
  _reserved_high = (const u1*)_vmm->reserved_high();
  assert(static_cast<size_t>(_reserved_high - _reserved_low) == reservation_size_request_bytes, "invariant");
  // reservation complete
  _top = _vmm->top();
  assert(_reserved_low == _top, "invariant"); // initial empty state
  // initial commit
  commit_memory_block();
  return _top;
}

void* JfrVirtualMemory::commit(size_t block_size_request_words) {
  assert(_vmm != NULL, "invariant");
  assert(is_aligned(block_size_request_words * BytesPerWord, os::vm_allocation_granularity()), "invariant");
  return _vmm->commit(block_size_request_words);
}

bool JfrVirtualMemory::is_full() const {
  return _top == _reserved_high;
}

bool JfrVirtualMemory::is_empty() const {
  return _top == _reserved_low;
}

bool JfrVirtualMemory::commit_memory_block() {
  assert(_vmm != NULL, "invariant");
  assert(!is_full(), "invariant");
  void* const block = _vmm->commit(_physical_commit_size_request_words);
  if (block != NULL) {
    _commit_point = _vmm->committed_high();
    return true;
  }
  // all reserved virtual memory is committed
  assert(block == NULL, "invariant");
  assert(_vmm->reserved_high() == _vmm->committed_high(), "invariant");
  return false;
}

void* JfrVirtualMemory::new_datum() {
  assert(_vmm != NULL, "invariant");
  assert(!is_full(), "invariant");
  if (_top == _commit_point) {
    if (!commit_memory_block()) {
      assert(is_full(), "invariant");
      return NULL;
    }
  }
  assert(_top + _aligned_datum_size_bytes <= _commit_point, "invariant");
  u1* allocation = _top;
  _top += _aligned_datum_size_bytes;
  assert(is_aligned(allocation, _aligned_datum_size_bytes), "invariant");
  return allocation;
}

void* JfrVirtualMemory::index_ptr(size_t index) {
  assert((index * _aligned_datum_size_bytes) + _reserved_low < _commit_point, "invariant");
  return (void*)((index * _aligned_datum_size_bytes) + _reserved_low);
}

void* JfrVirtualMemory::get(size_t index) {
  return index_ptr(index);
}

size_t JfrVirtualMemory::count() const {
  return (_top - _reserved_low) / _aligned_datum_size_bytes;
}

size_t JfrVirtualMemory::live_set() const {
  return _top - _reserved_low;
}

size_t JfrVirtualMemory::reserved_size() const {
  return _reserved_high - _reserved_low;
}

bool JfrVirtualMemory::compact(size_t index) {
  assert(index > 0, "invariant");
  assert(index <= reserved_size(), "invariant");
  const u1* low = static_cast<u1*>(index_ptr(index));
  const size_t block_size = _top - low;
  memcpy(const_cast<u1*>(_reserved_low), low, block_size);
  _top = const_cast<u1*>(_reserved_low) + block_size;
  assert(live_set() == block_size, "invariant");
  return true;
}
