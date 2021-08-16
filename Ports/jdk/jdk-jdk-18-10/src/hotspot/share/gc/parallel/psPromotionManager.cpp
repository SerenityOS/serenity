/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "gc/parallel/mutableSpace.hpp"
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psPromotionManager.inline.hpp"
#include "gc/parallel/psScavenge.inline.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/memRegion.hpp"
#include "memory/padded.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"

PaddedEnd<PSPromotionManager>* PSPromotionManager::_manager_array = NULL;
PSPromotionManager::PSScannerTasksQueueSet* PSPromotionManager::_stack_array_depth = NULL;
PreservedMarksSet*             PSPromotionManager::_preserved_marks_set = NULL;
PSOldGen*                      PSPromotionManager::_old_gen = NULL;
MutableSpace*                  PSPromotionManager::_young_space = NULL;

void PSPromotionManager::initialize() {
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();

  _old_gen = heap->old_gen();
  _young_space = heap->young_gen()->to_space();

  const uint promotion_manager_num = ParallelGCThreads + 1;

  // To prevent false sharing, we pad the PSPromotionManagers
  // and make sure that the first instance starts at a cache line.
  assert(_manager_array == NULL, "Attempt to initialize twice");
  _manager_array = PaddedArray<PSPromotionManager, mtGC>::create_unfreeable(promotion_manager_num);

  _stack_array_depth = new PSScannerTasksQueueSet(ParallelGCThreads);

  // Create and register the PSPromotionManager(s) for the worker threads.
  for(uint i=0; i<ParallelGCThreads; i++) {
    stack_array_depth()->register_queue(i, _manager_array[i].claimed_stack_depth());
  }
  // The VMThread gets its own PSPromotionManager, which is not available
  // for work stealing.

  assert(_preserved_marks_set == NULL, "Attempt to initialize twice");
  _preserved_marks_set = new PreservedMarksSet(true /* in_c_heap */);
  _preserved_marks_set->init(promotion_manager_num);
  for (uint i = 0; i < promotion_manager_num; i += 1) {
    _manager_array[i].register_preserved_marks(_preserved_marks_set->get(i));
  }
}

// Helper functions to get around the circular dependency between
// psScavenge.inline.hpp and psPromotionManager.inline.hpp.
bool PSPromotionManager::should_scavenge(oop* p, bool check_to_space) {
  return PSScavenge::should_scavenge(p, check_to_space);
}
bool PSPromotionManager::should_scavenge(narrowOop* p, bool check_to_space) {
  return PSScavenge::should_scavenge(p, check_to_space);
}

PSPromotionManager* PSPromotionManager::gc_thread_promotion_manager(uint index) {
  assert(index < ParallelGCThreads, "index out of range");
  assert(_manager_array != NULL, "Sanity");
  return &_manager_array[index];
}

PSPromotionManager* PSPromotionManager::vm_thread_promotion_manager() {
  assert(_manager_array != NULL, "Sanity");
  return &_manager_array[ParallelGCThreads];
}

void PSPromotionManager::pre_scavenge() {
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();

  _preserved_marks_set->assert_empty();
  _young_space = heap->young_gen()->to_space();

  for(uint i=0; i<ParallelGCThreads+1; i++) {
    manager_array(i)->reset();
  }
}

bool PSPromotionManager::post_scavenge(YoungGCTracer& gc_tracer) {
  bool promotion_failure_occurred = false;

  TASKQUEUE_STATS_ONLY(print_taskqueue_stats());
  for (uint i = 0; i < ParallelGCThreads + 1; i++) {
    PSPromotionManager* manager = manager_array(i);
    assert(manager->claimed_stack_depth()->is_empty(), "should be empty");
    if (manager->_promotion_failed_info.has_failed()) {
      gc_tracer.report_promotion_failed(manager->_promotion_failed_info);
      promotion_failure_occurred = true;
    }
    manager->flush_labs();
  }
  if (!promotion_failure_occurred) {
    // If there was no promotion failure, the preserved mark stacks
    // should be empty.
    _preserved_marks_set->assert_empty();
  }
  return promotion_failure_occurred;
}

#if TASKQUEUE_STATS
void
PSPromotionManager::print_local_stats(outputStream* const out, uint i) const {
  #define FMT " " SIZE_FORMAT_W(10)
  out->print_cr("%3u" FMT FMT FMT FMT,
                i, _array_chunk_pushes, _array_chunk_steals,
                _arrays_chunked, _array_chunks_processed);
  #undef FMT
}

static const char* const pm_stats_hdr[] = {
  "    ----partial array----     arrays      array",
  "thr       push      steal    chunked     chunks",
  "--- ---------- ---------- ---------- ----------"
};

void
PSPromotionManager::print_taskqueue_stats() {
  if (!log_is_enabled(Trace, gc, task, stats)) {
    return;
  }
  Log(gc, task, stats) log;
  ResourceMark rm;
  LogStream ls(log.trace());
  outputStream* out = &ls;
  out->print_cr("== GC Tasks Stats, GC %3d",
                ParallelScavengeHeap::heap()->total_collections());

  TaskQueueStats totals;
  out->print("thr "); TaskQueueStats::print_header(1, out); out->cr();
  out->print("--- "); TaskQueueStats::print_header(2, out); out->cr();
  for (uint i = 0; i < ParallelGCThreads + 1; ++i) {
    TaskQueueStats& next = manager_array(i)->_claimed_stack_depth.stats;
    out->print("%3d ", i); next.print(out); out->cr();
    totals += next;
  }
  out->print("tot "); totals.print(out); out->cr();

  const uint hlines = sizeof(pm_stats_hdr) / sizeof(pm_stats_hdr[0]);
  for (uint i = 0; i < hlines; ++i) out->print_cr("%s", pm_stats_hdr[i]);
  for (uint i = 0; i < ParallelGCThreads + 1; ++i) {
    manager_array(i)->print_local_stats(out, i);
  }
}

void
PSPromotionManager::reset_stats() {
  claimed_stack_depth()->stats.reset();
  _array_chunk_pushes = _array_chunk_steals = 0;
  _arrays_chunked = _array_chunks_processed = 0;
}
#endif // TASKQUEUE_STATS

PSPromotionManager::PSPromotionManager() {
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();

  // We set the old lab's start array.
  _old_lab.set_start_array(old_gen()->start_array());

  uint queue_size;
  claimed_stack_depth()->initialize();
  queue_size = claimed_stack_depth()->max_elems();

  _totally_drain = (ParallelGCThreads == 1) || (GCDrainStackTargetSize == 0);
  if (_totally_drain) {
    _target_stack_size = 0;
  } else {
    // don't let the target stack size to be more than 1/4 of the entries
    _target_stack_size = (uint) MIN2((uint) GCDrainStackTargetSize,
                                     (uint) (queue_size / 4));
  }

  _array_chunk_size = ParGCArrayScanChunk;
  // let's choose 1.5x the chunk size
  _min_array_size_for_chunking = 3 * _array_chunk_size / 2;

  _preserved_marks = NULL;

  reset();
}

void PSPromotionManager::reset() {
  assert(stacks_empty(), "reset of non-empty stack");

  // We need to get an assert in here to make sure the labs are always flushed.

  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();

  // Do not prefill the LAB's, save heap wastage!
  HeapWord* lab_base = young_space()->top();
  _young_lab.initialize(MemRegion(lab_base, (size_t)0));
  _young_gen_is_full = false;

  lab_base = old_gen()->object_space()->top();
  _old_lab.initialize(MemRegion(lab_base, (size_t)0));
  _old_gen_is_full = false;

  _promotion_failed_info.reset();

  TASKQUEUE_STATS_ONLY(reset_stats());
}

void PSPromotionManager::register_preserved_marks(PreservedMarks* preserved_marks) {
  assert(_preserved_marks == NULL, "do not set it twice");
  _preserved_marks = preserved_marks;
}

void PSPromotionManager::restore_preserved_marks() {
  _preserved_marks_set->restore(&ParallelScavengeHeap::heap()->workers());
}

void PSPromotionManager::drain_stacks_depth(bool totally_drain) {
  totally_drain = totally_drain || _totally_drain;

#ifdef ASSERT
  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();
  MutableSpace* to_space = heap->young_gen()->to_space();
  MutableSpace* old_space = heap->old_gen()->object_space();
#endif /* ASSERT */

  PSScannerTasksQueue* const tq = claimed_stack_depth();
  do {
    ScannerTask task;

    // Drain overflow stack first, so other threads can steal from
    // claimed stack while we work.
    while (tq->pop_overflow(task)) {
      process_popped_location_depth(task);
    }

    if (totally_drain) {
      while (tq->pop_local(task)) {
        process_popped_location_depth(task);
      }
    } else {
      while (tq->size() > _target_stack_size && tq->pop_local(task)) {
        process_popped_location_depth(task);
      }
    }
  } while ((totally_drain && !tq->taskqueue_empty()) || !tq->overflow_empty());

  assert(!totally_drain || tq->taskqueue_empty(), "Sanity");
  assert(totally_drain || tq->size() <= _target_stack_size, "Sanity");
  assert(tq->overflow_empty(), "Sanity");
}

void PSPromotionManager::flush_labs() {
  assert(stacks_empty(), "Attempt to flush lab with live stack");

  // If either promotion lab fills up, we can flush the
  // lab but not refill it, so check first.
  assert(!_young_lab.is_flushed() || _young_gen_is_full, "Sanity");
  if (!_young_lab.is_flushed())
    _young_lab.flush();

  assert(!_old_lab.is_flushed() || _old_gen_is_full, "Sanity");
  if (!_old_lab.is_flushed())
    _old_lab.flush();

  // Let PSScavenge know if we overflowed
  if (_young_gen_is_full) {
    PSScavenge::set_survivor_overflow(true);
  }
}

template <class T> void PSPromotionManager::process_array_chunk_work(
                                                 oop obj,
                                                 int start, int end) {
  assert(start <= end, "invariant");
  T* const base      = (T*)objArrayOop(obj)->base();
  T* p               = base + start;
  T* const chunk_end = base + end;
  while (p < chunk_end) {
    if (PSScavenge::should_scavenge(p)) {
      claim_or_forward_depth(p);
    }
    ++p;
  }
}

void PSPromotionManager::process_array_chunk(PartialArrayScanTask task) {
  assert(PSChunkLargeArrays, "invariant");

  oop old = task.to_source_array();
  assert(old->is_objArray(), "invariant");
  assert(old->is_forwarded(), "invariant");

  TASKQUEUE_STATS_ONLY(++_array_chunks_processed);

  oop const obj = old->forwardee();

  int start;
  int const end = arrayOop(old)->length();
  if (end > (int) _min_array_size_for_chunking) {
    // we'll chunk more
    start = end - _array_chunk_size;
    assert(start > 0, "invariant");
    arrayOop(old)->set_length(start);
    push_depth(ScannerTask(PartialArrayScanTask(old)));
    TASKQUEUE_STATS_ONLY(++_array_chunk_pushes);
  } else {
    // this is the final chunk for this array
    start = 0;
    int const actual_length = arrayOop(obj)->length();
    arrayOop(old)->set_length(actual_length);
  }

  if (UseCompressedOops) {
    process_array_chunk_work<narrowOop>(obj, start, end);
  } else {
    process_array_chunk_work<oop>(obj, start, end);
  }
}

oop PSPromotionManager::oop_promotion_failed(oop obj, markWord obj_mark) {
  assert(_old_gen_is_full || PromotionFailureALot, "Sanity");

  // Attempt to CAS in the header.
  // This tests if the header is still the same as when
  // this started.  If it is the same (i.e., no forwarding
  // pointer has been installed), then this thread owns
  // it.
  if (obj->cas_forward_to(obj, obj_mark)) {
    // We won any races, we "own" this object.
    assert(obj == obj->forwardee(), "Sanity");

    _promotion_failed_info.register_copy_failure(obj->size());

    push_contents(obj);

    _preserved_marks->push_if_necessary(obj, obj_mark);
  }  else {
    // We lost, someone else "owns" this object
    guarantee(obj->is_forwarded(), "Object must be forwarded if the cas failed.");

    // No unallocation to worry about.
    obj = obj->forwardee();
  }

  log_develop_trace(gc, scavenge)("{promotion-failure %s " PTR_FORMAT " (%d)}", obj->klass()->internal_name(), p2i(obj), obj->size());

  return obj;
}
