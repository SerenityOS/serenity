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

#include "precompiled.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/threadLocalAllocBuffer.inline.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/perfData.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "utilities/copy.hpp"

size_t       ThreadLocalAllocBuffer::_max_size = 0;
int          ThreadLocalAllocBuffer::_reserve_for_allocation_prefetch = 0;
unsigned int ThreadLocalAllocBuffer::_target_refills = 0;

ThreadLocalAllocBuffer::ThreadLocalAllocBuffer() :
  _start(NULL),
  _top(NULL),
  _pf_top(NULL),
  _end(NULL),
  _allocation_end(NULL),
  _desired_size(0),
  _refill_waste_limit(0),
  _allocated_before_last_gc(0),
  _bytes_since_last_sample_point(0),
  _number_of_refills(0),
  _refill_waste(0),
  _gc_waste(0),
  _slow_allocations(0),
  _allocated_size(0),
  _allocation_fraction(TLABAllocationWeight) {

  // do nothing. TLABs must be inited by initialize() calls
}

size_t ThreadLocalAllocBuffer::initial_refill_waste_limit()     { return desired_size() / TLABRefillWasteFraction; }
size_t ThreadLocalAllocBuffer::min_size()                       { return align_object_size(MinTLABSize / HeapWordSize) + alignment_reserve(); }
size_t ThreadLocalAllocBuffer::refill_waste_limit_increment()   { return TLABWasteIncrement; }

size_t ThreadLocalAllocBuffer::remaining() {
  if (end() == NULL) {
    return 0;
  }

  return pointer_delta(hard_end(), top());
}

void ThreadLocalAllocBuffer::accumulate_and_reset_statistics(ThreadLocalAllocStats* stats) {
  Thread* thr     = thread();
  size_t capacity = Universe::heap()->tlab_capacity(thr);
  size_t used     = Universe::heap()->tlab_used(thr);

  _gc_waste += (unsigned)remaining();
  size_t total_allocated = thr->allocated_bytes();
  size_t allocated_since_last_gc = total_allocated - _allocated_before_last_gc;
  _allocated_before_last_gc = total_allocated;

  print_stats("gc");

  if (_number_of_refills > 0) {
    // Update allocation history if a reasonable amount of eden was allocated.
    bool update_allocation_history = used > 0.5 * capacity;

    if (update_allocation_history) {
      // Average the fraction of eden allocated in a tlab by this
      // thread for use in the next resize operation.
      // _gc_waste is not subtracted because it's included in
      // "used".
      // The result can be larger than 1.0 due to direct to old allocations.
      // These allocations should ideally not be counted but since it is not possible
      // to filter them out here we just cap the fraction to be at most 1.0.
      // Keep alloc_frac as float and not double to avoid the double to float conversion
      float alloc_frac = MIN2(1.0f, allocated_since_last_gc / (float) used);
      _allocation_fraction.sample(alloc_frac);
    }

    stats->update_fast_allocations(_number_of_refills,
                                   _allocated_size,
                                   _gc_waste,
                                   _refill_waste);
  } else {
    assert(_number_of_refills == 0 && _refill_waste == 0 && _gc_waste == 0,
           "tlab stats == 0");
  }

  stats->update_slow_allocations(_slow_allocations);

  reset_statistics();
}

void ThreadLocalAllocBuffer::insert_filler() {
  assert(end() != NULL, "Must not be retired");
  if (top() < hard_end()) {
    Universe::heap()->fill_with_dummy_object(top(), hard_end(), true);
  }
}

void ThreadLocalAllocBuffer::make_parsable() {
  if (end() != NULL) {
    invariants();
    if (ZeroTLAB) {
      retire();
    } else {
      insert_filler();
    }
  }
}

void ThreadLocalAllocBuffer::retire(ThreadLocalAllocStats* stats) {
  if (stats != NULL) {
    accumulate_and_reset_statistics(stats);
  }

  if (end() != NULL) {
    invariants();
    thread()->incr_allocated_bytes(used_bytes());
    insert_filler();
    initialize(NULL, NULL, NULL);
  }
}

void ThreadLocalAllocBuffer::retire_before_allocation() {
  _refill_waste += (unsigned int)remaining();
  retire();
}

void ThreadLocalAllocBuffer::resize() {
  // Compute the next tlab size using expected allocation amount
  assert(ResizeTLAB, "Should not call this otherwise");
  size_t alloc = (size_t)(_allocation_fraction.average() *
                          (Universe::heap()->tlab_capacity(thread()) / HeapWordSize));
  size_t new_size = alloc / _target_refills;

  new_size = clamp(new_size, min_size(), max_size());

  size_t aligned_new_size = align_object_size(new_size);

  log_trace(gc, tlab)("TLAB new size: thread: " INTPTR_FORMAT " [id: %2d]"
                      " refills %d  alloc: %8.6f desired_size: " SIZE_FORMAT " -> " SIZE_FORMAT,
                      p2i(thread()), thread()->osthread()->thread_id(),
                      _target_refills, _allocation_fraction.average(), desired_size(), aligned_new_size);

  set_desired_size(aligned_new_size);
  set_refill_waste_limit(initial_refill_waste_limit());
}

void ThreadLocalAllocBuffer::reset_statistics() {
  _number_of_refills = 0;
  _refill_waste      = 0;
  _gc_waste          = 0;
  _slow_allocations  = 0;
  _allocated_size    = 0;
}

void ThreadLocalAllocBuffer::fill(HeapWord* start,
                                  HeapWord* top,
                                  size_t    new_size) {
  _number_of_refills++;
  _allocated_size += new_size;
  print_stats("fill");
  assert(top <= start + new_size - alignment_reserve(), "size too small");

  initialize(start, top, start + new_size - alignment_reserve());

  // Reset amount of internal fragmentation
  set_refill_waste_limit(initial_refill_waste_limit());
}

void ThreadLocalAllocBuffer::initialize(HeapWord* start,
                                        HeapWord* top,
                                        HeapWord* end) {
  set_start(start);
  set_top(top);
  set_pf_top(top);
  set_end(end);
  set_allocation_end(end);
  invariants();
}

void ThreadLocalAllocBuffer::initialize() {
  initialize(NULL,                    // start
             NULL,                    // top
             NULL);                   // end

  set_desired_size(initial_desired_size());

  size_t capacity = Universe::heap()->tlab_capacity(thread()) / HeapWordSize;
  // Keep alloc_frac as float and not double to avoid the double to float conversion
  float alloc_frac = desired_size() * target_refills() / (float) capacity;
  _allocation_fraction.sample(alloc_frac);

  set_refill_waste_limit(initial_refill_waste_limit());

  reset_statistics();
}

void ThreadLocalAllocBuffer::startup_initialization() {
  ThreadLocalAllocStats::initialize();

  // Assuming each thread's active tlab is, on average,
  // 1/2 full at a GC
  _target_refills = 100 / (2 * TLABWasteTargetPercent);
  // We need to set initial target refills to 2 to avoid a GC which causes VM
  // abort during VM initialization.
  _target_refills = MAX2(_target_refills, 2U);

#ifdef COMPILER2
  // If the C2 compiler is present, extra space is needed at the end of
  // TLABs, otherwise prefetching instructions generated by the C2
  // compiler will fault (due to accessing memory outside of heap).
  // The amount of space is the max of the number of lines to
  // prefetch for array and for instance allocations. (Extra space must be
  // reserved to accommodate both types of allocations.)
  //
  // Only SPARC-specific BIS instructions are known to fault. (Those
  // instructions are generated if AllocatePrefetchStyle==3 and
  // AllocatePrefetchInstr==1). To be on the safe side, however,
  // extra space is reserved for all combinations of
  // AllocatePrefetchStyle and AllocatePrefetchInstr.
  //
  // If the C2 compiler is not present, no space is reserved.

  // +1 for rounding up to next cache line, +1 to be safe
  if (CompilerConfig::is_c2_or_jvmci_compiler_enabled()) {
    int lines =  MAX2(AllocatePrefetchLines, AllocateInstancePrefetchLines) + 2;
    _reserve_for_allocation_prefetch = (AllocatePrefetchDistance + AllocatePrefetchStepSize * lines) /
                                       (int)HeapWordSize;
  }
#endif

  // During jvm startup, the main thread is initialized
  // before the heap is initialized.  So reinitialize it now.
  guarantee(Thread::current()->is_Java_thread(), "tlab initialization thread not Java thread");
  Thread::current()->tlab().initialize();

  log_develop_trace(gc, tlab)("TLAB min: " SIZE_FORMAT " initial: " SIZE_FORMAT " max: " SIZE_FORMAT,
                               min_size(), Thread::current()->tlab().initial_desired_size(), max_size());
}

size_t ThreadLocalAllocBuffer::initial_desired_size() {
  size_t init_sz = 0;

  if (TLABSize > 0) {
    init_sz = TLABSize / HeapWordSize;
  } else {
    // Initial size is a function of the average number of allocating threads.
    unsigned int nof_threads = ThreadLocalAllocStats::allocating_threads_avg();

    init_sz  = (Universe::heap()->tlab_capacity(thread()) / HeapWordSize) /
                      (nof_threads * target_refills());
    init_sz = align_object_size(init_sz);
  }
  // We can't use clamp() between min_size() and max_size() here because some
  // options based on them may still be inconsistent and so it may assert;
  // inconsistencies between those will be caught by following AfterMemoryInit
  // constraint checking.
  init_sz = MIN2(MAX2(init_sz, min_size()), max_size());
  return init_sz;
}

void ThreadLocalAllocBuffer::print_stats(const char* tag) {
  Log(gc, tlab) log;
  if (!log.is_trace()) {
    return;
  }

  Thread* thrd = thread();
  size_t waste = _gc_waste + _refill_waste;
  double waste_percent = percent_of(waste, _allocated_size);
  size_t tlab_used  = Universe::heap()->tlab_used(thrd);
  log.trace("TLAB: %s thread: " INTPTR_FORMAT " [id: %2d]"
            " desired_size: " SIZE_FORMAT "KB"
            " slow allocs: %d  refill waste: " SIZE_FORMAT "B"
            " alloc:%8.5f %8.0fKB refills: %d waste %4.1f%% gc: %dB"
            " slow: %dB",
            tag, p2i(thrd), thrd->osthread()->thread_id(),
            _desired_size / (K / HeapWordSize),
            _slow_allocations, _refill_waste_limit * HeapWordSize,
            _allocation_fraction.average(),
            _allocation_fraction.average() * tlab_used / K,
            _number_of_refills, waste_percent,
            _gc_waste * HeapWordSize,
            _refill_waste * HeapWordSize);
}

void ThreadLocalAllocBuffer::set_sample_end(bool reset_byte_accumulation) {
  size_t heap_words_remaining = pointer_delta(_end, _top);
  size_t bytes_until_sample = thread()->heap_sampler().bytes_until_sample();
  size_t words_until_sample = bytes_until_sample / HeapWordSize;

  if (reset_byte_accumulation) {
    _bytes_since_last_sample_point = 0;
  }

  if (heap_words_remaining > words_until_sample) {
    HeapWord* new_end = _top + words_until_sample;
    set_end(new_end);
    _bytes_since_last_sample_point += bytes_until_sample;
  } else {
    _bytes_since_last_sample_point += heap_words_remaining * HeapWordSize;
  }
}

Thread* ThreadLocalAllocBuffer::thread() {
  return (Thread*)(((char*)this) + in_bytes(start_offset()) - in_bytes(Thread::tlab_start_offset()));
}

void ThreadLocalAllocBuffer::set_back_allocation_end() {
  _end = _allocation_end;
}

HeapWord* ThreadLocalAllocBuffer::hard_end() {
  return _allocation_end + alignment_reserve();
}

PerfVariable* ThreadLocalAllocStats::_perf_allocating_threads;
PerfVariable* ThreadLocalAllocStats::_perf_total_refills;
PerfVariable* ThreadLocalAllocStats::_perf_max_refills;
PerfVariable* ThreadLocalAllocStats::_perf_total_allocations;
PerfVariable* ThreadLocalAllocStats::_perf_total_gc_waste;
PerfVariable* ThreadLocalAllocStats::_perf_max_gc_waste;
PerfVariable* ThreadLocalAllocStats::_perf_total_refill_waste;
PerfVariable* ThreadLocalAllocStats::_perf_max_refill_waste;
PerfVariable* ThreadLocalAllocStats::_perf_total_slow_allocations;
PerfVariable* ThreadLocalAllocStats::_perf_max_slow_allocations;
AdaptiveWeightedAverage ThreadLocalAllocStats::_allocating_threads_avg(0);

static PerfVariable* create_perf_variable(const char* name, PerfData::Units unit, TRAPS) {
  ResourceMark rm;
  return PerfDataManager::create_variable(SUN_GC, PerfDataManager::counter_name("tlab", name), unit, THREAD);
}

void ThreadLocalAllocStats::initialize() {
  _allocating_threads_avg = AdaptiveWeightedAverage(TLABAllocationWeight);
  _allocating_threads_avg.sample(1); // One allocating thread at startup

  if (UsePerfData) {
    EXCEPTION_MARK;
    _perf_allocating_threads      = create_perf_variable("allocThreads",   PerfData::U_None,  CHECK);
    _perf_total_refills           = create_perf_variable("fills",          PerfData::U_None,  CHECK);
    _perf_max_refills             = create_perf_variable("maxFills",       PerfData::U_None,  CHECK);
    _perf_total_allocations       = create_perf_variable("alloc",          PerfData::U_Bytes, CHECK);
    _perf_total_gc_waste          = create_perf_variable("gcWaste",        PerfData::U_Bytes, CHECK);
    _perf_max_gc_waste            = create_perf_variable("maxGcWaste",     PerfData::U_Bytes, CHECK);
    _perf_total_refill_waste      = create_perf_variable("refillWaste",    PerfData::U_Bytes, CHECK);
    _perf_max_refill_waste        = create_perf_variable("maxRefillWaste", PerfData::U_Bytes, CHECK);
    _perf_total_slow_allocations  = create_perf_variable("slowAlloc",      PerfData::U_None,  CHECK);
    _perf_max_slow_allocations    = create_perf_variable("maxSlowAlloc",   PerfData::U_None,  CHECK);
  }
}

ThreadLocalAllocStats::ThreadLocalAllocStats() :
    _allocating_threads(0),
    _total_refills(0),
    _max_refills(0),
    _total_allocations(0),
    _total_gc_waste(0),
    _max_gc_waste(0),
    _total_refill_waste(0),
    _max_refill_waste(0),
    _total_slow_allocations(0),
    _max_slow_allocations(0) {}

unsigned int ThreadLocalAllocStats::allocating_threads_avg() {
  return MAX2((unsigned int)(_allocating_threads_avg.average() + 0.5), 1U);
}

void ThreadLocalAllocStats::update_fast_allocations(unsigned int refills,
                                       size_t allocations,
                                       size_t gc_waste,
                                       size_t refill_waste) {
  _allocating_threads      += 1;
  _total_refills           += refills;
  _max_refills              = MAX2(_max_refills, refills);
  _total_allocations       += allocations;
  _total_gc_waste          += gc_waste;
  _max_gc_waste             = MAX2(_max_gc_waste, gc_waste);
  _total_refill_waste      += refill_waste;
  _max_refill_waste         = MAX2(_max_refill_waste, refill_waste);
}

void ThreadLocalAllocStats::update_slow_allocations(unsigned int allocations) {
  _total_slow_allocations += allocations;
  _max_slow_allocations    = MAX2(_max_slow_allocations, allocations);
}

void ThreadLocalAllocStats::update(const ThreadLocalAllocStats& other) {
  _allocating_threads      += other._allocating_threads;
  _total_refills           += other._total_refills;
  _max_refills              = MAX2(_max_refills, other._max_refills);
  _total_allocations       += other._total_allocations;
  _total_gc_waste          += other._total_gc_waste;
  _max_gc_waste             = MAX2(_max_gc_waste, other._max_gc_waste);
  _total_refill_waste      += other._total_refill_waste;
  _max_refill_waste         = MAX2(_max_refill_waste, other._max_refill_waste);
  _total_slow_allocations  += other._total_slow_allocations;
  _max_slow_allocations     = MAX2(_max_slow_allocations, other._max_slow_allocations);
}

void ThreadLocalAllocStats::reset() {
  _allocating_threads      = 0;
  _total_refills           = 0;
  _max_refills             = 0;
  _total_allocations       = 0;
  _total_gc_waste          = 0;
  _max_gc_waste            = 0;
  _total_refill_waste      = 0;
  _max_refill_waste        = 0;
  _total_slow_allocations  = 0;
  _max_slow_allocations    = 0;
}

void ThreadLocalAllocStats::publish() {
  if (_total_allocations == 0) {
    return;
  }

  _allocating_threads_avg.sample(_allocating_threads);

  const size_t waste = _total_gc_waste + _total_refill_waste;
  const double waste_percent = percent_of(waste, _total_allocations);
  log_debug(gc, tlab)("TLAB totals: thrds: %d  refills: %d max: %d"
                      " slow allocs: %d max %d waste: %4.1f%%"
                      " gc: " SIZE_FORMAT "B max: " SIZE_FORMAT "B"
                      " slow: " SIZE_FORMAT "B max: " SIZE_FORMAT "B",
                      _allocating_threads, _total_refills, _max_refills,
                      _total_slow_allocations, _max_slow_allocations, waste_percent,
                      _total_gc_waste * HeapWordSize, _max_gc_waste * HeapWordSize,
                      _total_refill_waste * HeapWordSize, _max_refill_waste * HeapWordSize);

  if (UsePerfData) {
    _perf_allocating_threads      ->set_value(_allocating_threads);
    _perf_total_refills           ->set_value(_total_refills);
    _perf_max_refills             ->set_value(_max_refills);
    _perf_total_allocations       ->set_value(_total_allocations);
    _perf_total_gc_waste          ->set_value(_total_gc_waste);
    _perf_max_gc_waste            ->set_value(_max_gc_waste);
    _perf_total_refill_waste      ->set_value(_total_refill_waste);
    _perf_max_refill_waste        ->set_value(_max_refill_waste);
    _perf_total_slow_allocations  ->set_value(_total_slow_allocations);
    _perf_max_slow_allocations    ->set_value(_max_slow_allocations);
  }
}

size_t ThreadLocalAllocBuffer::end_reserve() {
  size_t reserve_size = Universe::heap()->tlab_alloc_reserve();
  return MAX2(reserve_size, (size_t)_reserve_for_allocation_prefetch);
}

const HeapWord* ThreadLocalAllocBuffer::start_relaxed() const {
  return Atomic::load(&_start);
}

const HeapWord* ThreadLocalAllocBuffer::top_relaxed() const {
  return Atomic::load(&_top);
}
