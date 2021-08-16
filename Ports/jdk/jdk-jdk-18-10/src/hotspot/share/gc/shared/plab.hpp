/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_PLAB_HPP
#define SHARE_GC_SHARED_PLAB_HPP

#include "gc/shared/gcUtil.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

// Forward declarations.
class PLABStats;

// A per-thread allocation buffer used during GC.
class PLAB: public CHeapObj<mtGC> {
protected:
  char      head[32];
  size_t    _word_sz;          // In HeapWord units
  HeapWord* _bottom;
  HeapWord* _top;
  HeapWord* _end;           // Last allocatable address + 1
  HeapWord* _hard_end;      // _end + AlignmentReserve
  // In support of ergonomic sizing of PLAB's
  size_t    _allocated;     // in HeapWord units
  size_t    _wasted;        // in HeapWord units
  size_t    _undo_wasted;
  char      tail[32];
  static size_t AlignmentReserve;

  // Force future allocations to fail and queries for contains()
  // to return false. Returns the amount of unused space in this PLAB.
  size_t invalidate() {
    _end    = _hard_end;
    size_t remaining = pointer_delta(_end, _top);  // Calculate remaining space.
    _top    = _end;      // Force future allocations to fail.
    _bottom = _end;      // Force future contains() queries to return false.
    return remaining;
  }

  // Fill in remaining space with a dummy object and invalidate the PLAB. Returns
  // the amount of remaining space.
  size_t retire_internal();

  void add_undo_waste(HeapWord* obj, size_t word_sz);

  // Undo the last allocation in the buffer, which is required to be of the
  // "obj" of the given "word_sz".
  void undo_last_allocation(HeapWord* obj, size_t word_sz);

public:
  // Initializes the buffer to be empty, but with the given "word_sz".
  // Must get initialized with "set_buf" for an allocation to succeed.
  PLAB(size_t word_sz);

  static size_t size_required_for_allocation(size_t word_size) { return word_size + AlignmentReserve; }

  // Minimum PLAB size.
  static size_t min_size();
  // Maximum PLAB size.
  static size_t max_size();

  // If an allocation of the given "word_sz" can be satisfied within the
  // buffer, do the allocation, returning a pointer to the start of the
  // allocated block.  If the allocation request cannot be satisfied,
  // return NULL.
  HeapWord* allocate(size_t word_sz) {
    HeapWord* res = _top;
    if (pointer_delta(_end, _top) >= word_sz) {
      _top = _top + word_sz;
      return res;
    } else {
      return NULL;
    }
  }

  // Allocate the object aligned to "alignment_in_bytes".
  inline HeapWord* allocate_aligned(size_t word_sz, unsigned short alignment_in_bytes);

  // Undo any allocation in the buffer, which is required to be of the
  // "obj" of the given "word_sz".
  void undo_allocation(HeapWord* obj, size_t word_sz);

  // The total (word) size of the buffer, including both allocated and
  // unallocated space.
  size_t word_sz() { return _word_sz; }

  size_t waste() { return _wasted; }
  size_t undo_waste() { return _undo_wasted; }

  // The number of words of unallocated space remaining in the buffer.
  size_t words_remaining() {
    assert(_end >= _top, "Negative buffer");
    return pointer_delta(_end, _top, HeapWordSize);
  }

  bool contains(void* addr) {
    return (void*)_bottom <= addr && addr < (void*)_hard_end;
  }

  // Sets the space of the buffer to be [buf, space+word_sz()).
  void set_buf(HeapWord* buf, size_t new_word_sz) {
    assert(new_word_sz > AlignmentReserve, "Too small");
    _word_sz = new_word_sz;

    _bottom   = buf;
    _top      = _bottom;
    _hard_end = _bottom + word_sz();
    _end      = _hard_end - AlignmentReserve;
    assert(_end >= _top, "Negative buffer");
    // In support of ergonomic sizing
    _allocated += word_sz();
  }

  // Flush allocation statistics into the given PLABStats supporting ergonomic
  // sizing of PLAB's and retire the current buffer. To be called at the end of
  // GC.
  void flush_and_retire_stats(PLABStats* stats);

  // Fills in the unallocated portion of the buffer with a garbage object and updates
  // statistics. To be called during GC.
  void retire();
};

// PLAB book-keeping.
class PLABStats : public CHeapObj<mtGC> {
 protected:
  const char* _description;   // Identifying string.

  size_t _allocated;          // Total allocated
  size_t _wasted;             // of which wasted (internal fragmentation)
  size_t _undo_wasted;        // of which wasted on undo (is not used for calculation of PLAB size)
  size_t _unused;             // Unused in last buffer
  size_t _default_plab_sz;
  size_t _desired_net_plab_sz;// Output of filter (below), suitably trimmed and quantized
  AdaptiveWeightedAverage
         _filter;             // Integrator with decay

  virtual void reset() {
    _allocated   = 0;
    _wasted      = 0;
    _undo_wasted = 0;
    _unused      = 0;
  }

  virtual void log_plab_allocation();
  virtual void log_sizing(size_t calculated, size_t net_desired);

  // helper for adjust_desired_plab_sz().
  virtual size_t compute_desired_plab_sz();

 public:
  PLABStats(const char* description, size_t default_per_thread_plab_size, size_t desired_net_plab_sz, unsigned wt) :
    _description(description),
    _allocated(0),
    _wasted(0),
    _undo_wasted(0),
    _unused(0),
    _default_plab_sz(default_per_thread_plab_size),
    _desired_net_plab_sz(desired_net_plab_sz),
    _filter(wt)
  { }

  virtual ~PLABStats() { }

  size_t allocated() const { return _allocated; }
  size_t wasted() const { return _wasted; }
  size_t unused() const { return _unused; }
  size_t used() const { return allocated() - (wasted() + unused()); }
  size_t undo_wasted() const { return _undo_wasted; }

  static const size_t min_size() {
    return PLAB::min_size();
  }

  static const size_t max_size() {
    return PLAB::max_size();
  }

  // Calculates plab size for current number of gc worker threads.
  size_t desired_plab_sz(uint no_of_gc_workers);

  // Updates the current desired PLAB size. Computes the new desired PLAB size with one gc worker thread,
  // updates _desired_plab_sz and clears sensor accumulators.
  void adjust_desired_plab_sz();

  inline void add_allocated(size_t v);

  inline void add_unused(size_t v);

  inline void add_wasted(size_t v);

  inline void add_undo_wasted(size_t v);
};

#endif // SHARE_GC_SHARED_PLAB_HPP
