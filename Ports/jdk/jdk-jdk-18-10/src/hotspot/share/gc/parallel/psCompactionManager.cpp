/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/parallel/objectStartArray.hpp"
#include "gc/parallel/parMarkBitMap.inline.hpp"
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/psCompactionManager.inline.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psParallelCompact.inline.hpp"
#include "gc/shared/taskqueue.inline.hpp"
#include "logging/log.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/instanceMirrorKlass.inline.hpp"
#include "oops/objArrayKlass.inline.hpp"
#include "oops/oop.inline.hpp"

PSOldGen*               ParCompactionManager::_old_gen = NULL;
ParCompactionManager**  ParCompactionManager::_manager_array = NULL;

ParCompactionManager::OopTaskQueueSet*      ParCompactionManager::_oop_task_queues = NULL;
ParCompactionManager::ObjArrayTaskQueueSet* ParCompactionManager::_objarray_task_queues = NULL;
ParCompactionManager::RegionTaskQueueSet*   ParCompactionManager::_region_task_queues = NULL;

ObjectStartArray*    ParCompactionManager::_start_array = NULL;
ParMarkBitMap*       ParCompactionManager::_mark_bitmap = NULL;
GrowableArray<size_t >* ParCompactionManager::_shadow_region_array = NULL;
Monitor*                ParCompactionManager::_shadow_region_monitor = NULL;

ParCompactionManager::ParCompactionManager() {

  ParallelScavengeHeap* heap = ParallelScavengeHeap::heap();

  _old_gen = heap->old_gen();
  _start_array = old_gen()->start_array();

  marking_stack()->initialize();
  _objarray_stack.initialize();
  _region_stack.initialize();

  reset_bitmap_query_cache();
}

void ParCompactionManager::initialize(ParMarkBitMap* mbm) {
  assert(ParallelScavengeHeap::heap() != NULL,
    "Needed for initialization");

  _mark_bitmap = mbm;

  uint parallel_gc_threads = ParallelScavengeHeap::heap()->workers().total_workers();

  assert(_manager_array == NULL, "Attempt to initialize twice");
  _manager_array = NEW_C_HEAP_ARRAY(ParCompactionManager*, parallel_gc_threads+1, mtGC);

  _oop_task_queues = new OopTaskQueueSet(parallel_gc_threads);
  _objarray_task_queues = new ObjArrayTaskQueueSet(parallel_gc_threads);
  _region_task_queues = new RegionTaskQueueSet(parallel_gc_threads);

  // Create and register the ParCompactionManager(s) for the worker threads.
  for(uint i=0; i<parallel_gc_threads; i++) {
    _manager_array[i] = new ParCompactionManager();
    oop_task_queues()->register_queue(i, _manager_array[i]->marking_stack());
    _objarray_task_queues->register_queue(i, &_manager_array[i]->_objarray_stack);
    region_task_queues()->register_queue(i, _manager_array[i]->region_stack());
  }

  // The VMThread gets its own ParCompactionManager, which is not available
  // for work stealing.
  _manager_array[parallel_gc_threads] = new ParCompactionManager();
  assert(ParallelScavengeHeap::heap()->workers().total_workers() != 0,
    "Not initialized?");

  _shadow_region_array = new (ResourceObj::C_HEAP, mtGC) GrowableArray<size_t >(10, mtGC);

  _shadow_region_monitor = new Monitor(Mutex::barrier, "CompactionManager monitor",
                                       Mutex::_allow_vm_block_flag, Monitor::_safepoint_check_never);
}

void ParCompactionManager::reset_all_bitmap_query_caches() {
  uint parallel_gc_threads = ParallelScavengeHeap::heap()->workers().total_workers();
  for (uint i=0; i<=parallel_gc_threads; i++) {
    _manager_array[i]->reset_bitmap_query_cache();
  }
}


ParCompactionManager*
ParCompactionManager::gc_thread_compaction_manager(uint index) {
  assert(index < ParallelGCThreads, "index out of range");
  assert(_manager_array != NULL, "Sanity");
  return _manager_array[index];
}

void ParCompactionManager::follow_marking_stacks() {
  do {
    // Drain the overflow stack first, to allow stealing from the marking stack.
    oop obj;
    while (marking_stack()->pop_overflow(obj)) {
      follow_contents(obj);
    }
    while (marking_stack()->pop_local(obj)) {
      follow_contents(obj);
    }

    // Process ObjArrays one at a time to avoid marking stack bloat.
    ObjArrayTask task;
    if (_objarray_stack.pop_overflow(task) || _objarray_stack.pop_local(task)) {
      follow_array((objArrayOop)task.obj(), task.index());
    }
  } while (!marking_stacks_empty());

  assert(marking_stacks_empty(), "Sanity");
}

void ParCompactionManager::drain_region_stacks() {
  do {
    // Drain overflow stack first so other threads can steal.
    size_t region_index;
    while (region_stack()->pop_overflow(region_index)) {
      PSParallelCompact::fill_and_update_region(this, region_index);
    }

    while (region_stack()->pop_local(region_index)) {
      PSParallelCompact::fill_and_update_region(this, region_index);
    }
  } while (!region_stack()->is_empty());
}

size_t ParCompactionManager::pop_shadow_region_mt_safe(PSParallelCompact::RegionData* region_ptr) {
  MonitorLocker ml(_shadow_region_monitor, Mutex::_no_safepoint_check_flag);
  while (true) {
    if (!_shadow_region_array->is_empty()) {
      return _shadow_region_array->pop();
    }
    // Check if the corresponding heap region is available now.
    // If so, we don't need to get a shadow region anymore, and
    // we return InvalidShadow to indicate such a case.
    if (region_ptr->claimed()) {
      return InvalidShadow;
    }
    ml.wait(1);
  }
}

void ParCompactionManager::push_shadow_region_mt_safe(size_t shadow_region) {
  MonitorLocker ml(_shadow_region_monitor, Mutex::_no_safepoint_check_flag);
  _shadow_region_array->push(shadow_region);
  ml.notify();
}

void ParCompactionManager::push_shadow_region(size_t shadow_region) {
  _shadow_region_array->push(shadow_region);
}

void ParCompactionManager::remove_all_shadow_regions() {
  _shadow_region_array->clear();
}

#ifdef ASSERT
void ParCompactionManager::verify_all_marking_stack_empty() {
  uint parallel_gc_threads = ParallelGCThreads;
  for (uint i = 0; i <= parallel_gc_threads; i++) {
    assert(_manager_array[i]->marking_stacks_empty(), "Marking stack should be empty");
  }
}

void ParCompactionManager::verify_all_region_stack_empty() {
  uint parallel_gc_threads = ParallelGCThreads;
  for (uint i = 0; i <= parallel_gc_threads; i++) {
    assert(_manager_array[i]->region_stack()->is_empty(), "Region stack should be empty");
  }
}
#endif
