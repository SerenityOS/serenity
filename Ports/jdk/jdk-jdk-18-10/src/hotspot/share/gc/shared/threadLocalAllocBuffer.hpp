/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_HPP
#define SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_HPP

#include "gc/shared/gcUtil.hpp"
#include "runtime/perfDataTypes.hpp"
#include "utilities/align.hpp"
#include "utilities/sizes.hpp"

class ThreadLocalAllocStats;

// ThreadLocalAllocBuffer: a descriptor for thread-local storage used by
// the threads for allocation.
//            It is thread-private at any time, but maybe multiplexed over
//            time across multiple threads. The park()/unpark() pair is
//            used to make it available for such multiplexing.
//
//            Heap sampling is performed via the end and allocation_end
//            fields.
//            allocation_end contains the real end of the tlab allocation,
//            whereas end can be set to an arbitrary spot in the tlab to
//            trip the return and sample the allocation.
class ThreadLocalAllocBuffer: public CHeapObj<mtThread> {
  friend class VMStructs;
  friend class JVMCIVMStructs;
private:
  HeapWord* _start;                              // address of TLAB
  HeapWord* _top;                                // address after last allocation
  HeapWord* _pf_top;                             // allocation prefetch watermark
  HeapWord* _end;                                // allocation end (can be the sampling end point or _allocation_end)
  HeapWord* _allocation_end;                     // end for allocations (actual TLAB end, excluding alignment_reserve)

  size_t    _desired_size;                       // desired size   (including alignment_reserve)
  size_t    _refill_waste_limit;                 // hold onto tlab if free() is larger than this
  size_t    _allocated_before_last_gc;           // total bytes allocated up until the last gc
  size_t    _bytes_since_last_sample_point;      // bytes since last sample point.

  static size_t   _max_size;                          // maximum size of any TLAB
  static int      _reserve_for_allocation_prefetch;   // Reserve at the end of the TLAB
  static unsigned _target_refills;                    // expected number of refills between GCs

  unsigned  _number_of_refills;
  unsigned  _refill_waste;
  unsigned  _gc_waste;
  unsigned  _slow_allocations;
  size_t    _allocated_size;

  AdaptiveWeightedAverage _allocation_fraction;  // fraction of eden allocated in tlabs

  void reset_statistics();

  void set_start(HeapWord* start)                { _start = start; }
  void set_end(HeapWord* end)                    { _end = end; }
  void set_allocation_end(HeapWord* ptr)         { _allocation_end = ptr; }
  void set_top(HeapWord* top)                    { _top = top; }
  void set_pf_top(HeapWord* pf_top)              { _pf_top = pf_top; }
  void set_desired_size(size_t desired_size)     { _desired_size = desired_size; }
  void set_refill_waste_limit(size_t waste)      { _refill_waste_limit = waste;  }

  size_t initial_refill_waste_limit();

  static int    target_refills()                 { return _target_refills; }
  size_t initial_desired_size();

  size_t remaining();

  // Make parsable and release it.
  void reset();

  void invariants() const { assert(top() >= start() && top() <= end(), "invalid tlab"); }

  void initialize(HeapWord* start, HeapWord* top, HeapWord* end);

  void insert_filler();

  void accumulate_and_reset_statistics(ThreadLocalAllocStats* stats);

  void print_stats(const char* tag);

  Thread* thread();

  // statistics

  int number_of_refills() const { return _number_of_refills; }
  int gc_waste() const          { return _gc_waste; }
  int slow_allocations() const  { return _slow_allocations; }

public:
  ThreadLocalAllocBuffer();

  static size_t min_size();
  static size_t max_size()                       { assert(_max_size != 0, "max_size not set up"); return _max_size; }
  static size_t max_size_in_bytes()              { return max_size() * BytesPerWord; }
  static void set_max_size(size_t max_size)      { _max_size = max_size; }

  HeapWord* start() const                        { return _start; }
  HeapWord* end() const                          { return _end; }
  HeapWord* top() const                          { return _top; }
  HeapWord* hard_end();
  HeapWord* pf_top() const                       { return _pf_top; }
  size_t desired_size() const                    { return _desired_size; }
  size_t used() const                            { return pointer_delta(top(), start()); }
  size_t used_bytes() const                      { return pointer_delta(top(), start(), 1); }
  size_t free() const                            { return pointer_delta(end(), top()); }
  // Don't discard tlab if remaining space is larger than this.
  size_t refill_waste_limit() const              { return _refill_waste_limit; }
  size_t bytes_since_last_sample_point() const   { return _bytes_since_last_sample_point; }

  // For external inspection.
  const HeapWord* start_relaxed() const;
  const HeapWord* top_relaxed() const;

  // Allocate size HeapWords. The memory is NOT initialized to zero.
  inline HeapWord* allocate(size_t size);

  // Reserve space at the end of TLAB
  static size_t end_reserve();
  static size_t alignment_reserve()              { return align_object_size(end_reserve()); }
  static size_t alignment_reserve_in_bytes()     { return alignment_reserve() * HeapWordSize; }

  // Return tlab size or remaining space in eden such that the
  // space is large enough to hold obj_size and necessary fill space.
  // Otherwise return 0;
  inline size_t compute_size(size_t obj_size);

  // Compute the minimal needed tlab size for the given object size.
  static inline size_t compute_min_size(size_t obj_size);

  // Record slow allocation
  inline void record_slow_allocation(size_t obj_size);

  // Initialization at startup
  static void startup_initialization();

  // Make an in-use tlab parsable.
  void make_parsable();

  // Retire an in-use tlab and optionally collect statistics.
  void retire(ThreadLocalAllocStats* stats = NULL);

  // Retire in-use tlab before allocation of a new tlab
  void retire_before_allocation();

  // Resize based on amount of allocation, etc.
  void resize();

  void fill(HeapWord* start, HeapWord* top, size_t new_size);
  void initialize();

  void set_back_allocation_end();
  void set_sample_end(bool reset_byte_accumulation);

  static size_t refill_waste_limit_increment();

  template <typename T> void addresses_do(T f) {
    f(&_start);
    f(&_top);
    f(&_pf_top);
    f(&_end);
    f(&_allocation_end);
  }

  // Code generation support
  static ByteSize start_offset()                 { return byte_offset_of(ThreadLocalAllocBuffer, _start); }
  static ByteSize end_offset()                   { return byte_offset_of(ThreadLocalAllocBuffer, _end); }
  static ByteSize top_offset()                   { return byte_offset_of(ThreadLocalAllocBuffer, _top); }
  static ByteSize pf_top_offset()                { return byte_offset_of(ThreadLocalAllocBuffer, _pf_top); }
};

class ThreadLocalAllocStats : public StackObj {
private:
  static PerfVariable* _perf_allocating_threads;
  static PerfVariable* _perf_total_refills;
  static PerfVariable* _perf_max_refills;
  static PerfVariable* _perf_total_allocations;
  static PerfVariable* _perf_total_gc_waste;
  static PerfVariable* _perf_max_gc_waste;
  static PerfVariable* _perf_total_refill_waste;
  static PerfVariable* _perf_max_refill_waste;
  static PerfVariable* _perf_total_slow_allocations;
  static PerfVariable* _perf_max_slow_allocations;

  static AdaptiveWeightedAverage _allocating_threads_avg;

  unsigned int _allocating_threads;
  unsigned int _total_refills;
  unsigned int _max_refills;
  size_t       _total_allocations;
  size_t       _total_gc_waste;
  size_t       _max_gc_waste;
  size_t       _total_refill_waste;
  size_t       _max_refill_waste;
  unsigned int _total_slow_allocations;
  unsigned int _max_slow_allocations;

public:
  static void initialize();
  static unsigned int allocating_threads_avg();

  ThreadLocalAllocStats();

  void update_fast_allocations(unsigned int refills,
                               size_t allocations,
                               size_t gc_waste,
                               size_t refill_waste);
  void update_slow_allocations(unsigned int allocations);
  void update(const ThreadLocalAllocStats& other);

  void reset();
  void publish();
};

#endif // SHARE_GC_SHARED_THREADLOCALALLOCBUFFER_HPP
