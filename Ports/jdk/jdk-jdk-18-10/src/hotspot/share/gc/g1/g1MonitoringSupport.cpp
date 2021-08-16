/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1MonitoringSupport.hpp"
#include "gc/g1/g1Policy.hpp"
#include "gc/g1/g1MemoryPool.hpp"
#include "gc/shared/hSpaceCounters.hpp"
#include "memory/metaspaceCounters.hpp"
#include "services/memoryPool.hpp"

class G1GenerationCounters : public GenerationCounters {
protected:
  G1MonitoringSupport* _monitoring_support;

public:
  G1GenerationCounters(G1MonitoringSupport* monitoring_support,
                       const char* name, int ordinal, int spaces,
                       size_t min_capacity, size_t max_capacity,
                       size_t curr_capacity)
  : GenerationCounters(name, ordinal, spaces, min_capacity,
                       max_capacity, curr_capacity),
    _monitoring_support(monitoring_support) { }
};

class G1YoungGenerationCounters : public G1GenerationCounters {
public:
  // We pad the capacity three times given that the young generation
  // contains three spaces (eden and two survivors).
  G1YoungGenerationCounters(G1MonitoringSupport* monitoring_support, const char* name, size_t max_size)
  : G1GenerationCounters(monitoring_support, name, 0 /* ordinal */, 3 /* spaces */,
                         G1MonitoringSupport::pad_capacity(0, 3) /* min_capacity */,
                         G1MonitoringSupport::pad_capacity(max_size, 3),
                         G1MonitoringSupport::pad_capacity(0, 3) /* curr_capacity */) {
    if (UsePerfData) {
      update_all();
    }
  }

  virtual void update_all() {
    size_t committed =
              G1MonitoringSupport::pad_capacity(_monitoring_support->young_gen_committed(), 3);
    _current_size->set_value(committed);
  }
};

class G1OldGenerationCounters : public G1GenerationCounters {
public:
  G1OldGenerationCounters(G1MonitoringSupport* monitoring_support, const char* name, size_t max_size)
  : G1GenerationCounters(monitoring_support, name, 1 /* ordinal */, 1 /* spaces */,
                         G1MonitoringSupport::pad_capacity(0) /* min_capacity */,
                         G1MonitoringSupport::pad_capacity(max_size),
                         G1MonitoringSupport::pad_capacity(0) /* curr_capacity */) {
    if (UsePerfData) {
      update_all();
    }
  }

  virtual void update_all() {
    size_t committed =
              G1MonitoringSupport::pad_capacity(_monitoring_support->old_gen_committed());
    _current_size->set_value(committed);
  }
};

G1MonitoringSupport::G1MonitoringSupport(G1CollectedHeap* g1h) :
  _g1h(g1h),
  _incremental_memory_manager("G1 Young Generation", "end of minor GC"),
  _full_gc_memory_manager("G1 Old Generation", "end of major GC"),
  _eden_space_pool(NULL),
  _survivor_space_pool(NULL),
  _old_gen_pool(NULL),
  _incremental_collection_counters(NULL),
  _full_collection_counters(NULL),
  _conc_collection_counters(NULL),
  _young_gen_counters(NULL),
  _old_gen_counters(NULL),
  _old_space_counters(NULL),
  _eden_space_counters(NULL),
  _from_space_counters(NULL),
  _to_space_counters(NULL),

  _overall_committed(0),
  _overall_used(0),
  _young_gen_committed(0),
  _old_gen_committed(0),

  _eden_space_committed(0),
  _eden_space_used(0),
  _survivor_space_committed(0),
  _survivor_space_used(0),
  _old_gen_used(0) {

  recalculate_sizes();

  // Counters for garbage collections
  //
  //  name "collector.0".  In a generational collector this would be the
  // young generation collection.
  _incremental_collection_counters =
    new CollectorCounters("G1 young collection pauses", 0);
  //   name "collector.1".  In a generational collector this would be the
  // old generation collection.
  _full_collection_counters =
    new CollectorCounters("G1 full collection pauses", 1);
  //   name "collector.2".  In a generational collector this would be the
  // STW phases in concurrent collection.
  _conc_collection_counters =
    new CollectorCounters("G1 concurrent cycle pauses", 2);

  // "Generation" and "Space" counters.
  //
  //  name "generation.1" This is logically the old generation in
  // generational GC terms.  The "1, 1" parameters are for
  // the n-th generation (=1) with 1 space.
  // Counters are created from minCapacity, maxCapacity, and capacity
  _old_gen_counters = new G1OldGenerationCounters(this, "old", _g1h->max_capacity());

  //  name  "generation.1.space.0"
  // Counters are created from maxCapacity, capacity, initCapacity,
  // and used.
  _old_space_counters = new HSpaceCounters(_old_gen_counters->name_space(),
    "space", 0 /* ordinal */,
    pad_capacity(g1h->max_capacity()) /* max_capacity */,
    pad_capacity(_old_gen_committed) /* init_capacity */);

  //   Young collection set
  //  name "generation.0".  This is logically the young generation.
  //  The "0, 3" are parameters for the n-th generation (=0) with 3 spaces.
  // See  _old_collection_counters for additional counters
  _young_gen_counters = new G1YoungGenerationCounters(this, "young", _g1h->max_capacity());

  const char* young_collection_name_space = _young_gen_counters->name_space();

  //  name "generation.0.space.0"
  // See _old_space_counters for additional counters
  _eden_space_counters = new HSpaceCounters(young_collection_name_space,
    "eden", 0 /* ordinal */,
    pad_capacity(g1h->max_capacity()) /* max_capacity */,
    pad_capacity(_eden_space_committed) /* init_capacity */);

  //  name "generation.0.space.1"
  // See _old_space_counters for additional counters
  // Set the arguments to indicate that this survivor space is not used.
  _from_space_counters = new HSpaceCounters(young_collection_name_space,
    "s0", 1 /* ordinal */,
    pad_capacity(0) /* max_capacity */,
    pad_capacity(0) /* init_capacity */);
  // Given that this survivor space is not used, we update it here
  // once to reflect that its used space is 0 so that we don't have to
  // worry about updating it again later.
  if (UsePerfData) {
    _from_space_counters->update_used(0);
  }

  //  name "generation.0.space.2"
  // See _old_space_counters for additional counters
  _to_space_counters = new HSpaceCounters(young_collection_name_space,
    "s1", 2 /* ordinal */,
    pad_capacity(g1h->max_capacity()) /* max_capacity */,
    pad_capacity(_survivor_space_committed) /* init_capacity */);
}

G1MonitoringSupport::~G1MonitoringSupport() {
  delete _eden_space_pool;
  delete _survivor_space_pool;
  delete _old_gen_pool;
}

void G1MonitoringSupport::initialize_serviceability() {
  _eden_space_pool = new G1EdenPool(_g1h, _eden_space_committed);
  _survivor_space_pool = new G1SurvivorPool(_g1h, _survivor_space_committed);
  _old_gen_pool = new G1OldGenPool(_g1h, _old_gen_committed, _g1h->max_capacity());

  _full_gc_memory_manager.add_pool(_eden_space_pool);
  _full_gc_memory_manager.add_pool(_survivor_space_pool);
  _full_gc_memory_manager.add_pool(_old_gen_pool);

  _incremental_memory_manager.add_pool(_eden_space_pool);
  _incremental_memory_manager.add_pool(_survivor_space_pool);
  _incremental_memory_manager.add_pool(_old_gen_pool, false /* always_affected_by_gc */);
}

MemoryUsage G1MonitoringSupport::memory_usage() {
  MutexLocker x(MonitoringSupport_lock, Mutex::_no_safepoint_check_flag);
  return MemoryUsage(InitialHeapSize, _overall_used, _overall_committed, _g1h->max_capacity());
}

GrowableArray<GCMemoryManager*> G1MonitoringSupport::memory_managers() {
  GrowableArray<GCMemoryManager*> memory_managers(2);
  memory_managers.append(&_incremental_memory_manager);
  memory_managers.append(&_full_gc_memory_manager);
  return memory_managers;
}

GrowableArray<MemoryPool*> G1MonitoringSupport::memory_pools() {
  GrowableArray<MemoryPool*> memory_pools(3);
  memory_pools.append(_eden_space_pool);
  memory_pools.append(_survivor_space_pool);
  memory_pools.append(_old_gen_pool);
  return memory_pools;
}

void G1MonitoringSupport::recalculate_sizes() {
  assert_heap_locked_or_at_safepoint(true);

  MutexLocker x(MonitoringSupport_lock, Mutex::_no_safepoint_check_flag);
  // Recalculate all the sizes from scratch.

  // This never includes used bytes of current allocating heap region.
  _overall_used = _g1h->used_unlocked();
  _eden_space_used = _g1h->eden_regions_used_bytes();
  _survivor_space_used = _g1h->survivor_regions_used_bytes();

  // _overall_used and _eden_space_used are obtained concurrently so
  // may be inconsistent with each other. To prevent _old_gen_used going negative,
  // use smaller value to substract.
  _old_gen_used = _overall_used - MIN2(_overall_used, _eden_space_used + _survivor_space_used);

  uint survivor_list_length = _g1h->survivor_regions_count();
  // Max length includes any potential extensions to the young gen
  // we'll do when the GC locker is active.
  uint young_list_max_length = _g1h->policy()->young_list_max_length();
  assert(young_list_max_length >= survivor_list_length, "invariant");
  uint eden_list_max_length = young_list_max_length - survivor_list_length;

  // First calculate the committed sizes that can be calculated independently.
  _survivor_space_committed = survivor_list_length * HeapRegion::GrainBytes;
  _old_gen_committed = HeapRegion::align_up_to_region_byte_size(_old_gen_used);

  // Next, start with the overall committed size.
  _overall_committed = _g1h->capacity();
  size_t committed = _overall_committed;

  // Remove the committed size we have calculated so far (for the
  // survivor and old space).
  assert(committed >= (_survivor_space_committed + _old_gen_committed), "sanity");
  committed -= _survivor_space_committed + _old_gen_committed;

  // Next, calculate and remove the committed size for the eden.
  _eden_space_committed = (size_t) eden_list_max_length * HeapRegion::GrainBytes;
  // Somewhat defensive: be robust in case there are inaccuracies in
  // the calculations
  _eden_space_committed = MIN2(_eden_space_committed, committed);
  committed -= _eden_space_committed;

  // Finally, give the rest to the old space...
  _old_gen_committed += committed;
  // ..and calculate the young gen committed.
  _young_gen_committed = _eden_space_committed + _survivor_space_committed;

  assert(_overall_committed ==
         (_eden_space_committed + _survivor_space_committed + _old_gen_committed),
         "the committed sizes should add up");
  // Somewhat defensive: cap the eden used size to make sure it
  // never exceeds the committed size.
  _eden_space_used = MIN2(_eden_space_used, _eden_space_committed);
  // _survivor_space_used is calculated during a safepoint and _survivor_space_committed
  // is calculated from survivor region count * heap region size.
  assert(_survivor_space_used <= _survivor_space_committed, "Survivor used bytes(" SIZE_FORMAT
         ") should be less than or equal to survivor committed(" SIZE_FORMAT ")",
         _survivor_space_used, _survivor_space_committed);
  // _old_gen_committed is calculated in terms of _old_gen_used value.
  assert(_old_gen_used <= _old_gen_committed, "Old gen used bytes(" SIZE_FORMAT
         ") should be less than or equal to old gen committed(" SIZE_FORMAT ")",
         _old_gen_used, _old_gen_committed);
}

void G1MonitoringSupport::update_sizes() {
  recalculate_sizes();
  if (UsePerfData) {
    _eden_space_counters->update_capacity(pad_capacity(_eden_space_committed));
    _eden_space_counters->update_used(_eden_space_used);
   // only the "to" survivor space is active, so we don't need to
    // update the counters for the "from" survivor space
    _to_space_counters->update_capacity(pad_capacity(_survivor_space_committed));
    _to_space_counters->update_used(_survivor_space_used);
    _old_space_counters->update_capacity(pad_capacity(_old_gen_committed));
    _old_space_counters->update_used(_old_gen_used);

    _young_gen_counters->update_all();
    _old_gen_counters->update_all();

    MetaspaceCounters::update_performance_counters();
  }
}

void G1MonitoringSupport::update_eden_size() {
  // Recalculate everything - this should be fast enough and we are sure that we do not
  // miss anything.
  recalculate_sizes();
  if (UsePerfData) {
    _eden_space_counters->update_used(_eden_space_used);
  }
}

MemoryUsage G1MonitoringSupport::eden_space_memory_usage(size_t initial_size, size_t max_size) {
  MutexLocker x(MonitoringSupport_lock, Mutex::_no_safepoint_check_flag);

  return MemoryUsage(initial_size,
                     _eden_space_used,
                     _eden_space_committed,
                     max_size);
}

MemoryUsage G1MonitoringSupport::survivor_space_memory_usage(size_t initial_size, size_t max_size) {
  MutexLocker x(MonitoringSupport_lock, Mutex::_no_safepoint_check_flag);

  return MemoryUsage(initial_size,
                     _survivor_space_used,
                     _survivor_space_committed,
                     max_size);
}

MemoryUsage G1MonitoringSupport::old_gen_memory_usage(size_t initial_size, size_t max_size) {
  MutexLocker x(MonitoringSupport_lock, Mutex::_no_safepoint_check_flag);

  return MemoryUsage(initial_size,
                     _old_gen_used,
                     _old_gen_committed,
                     max_size);
}

G1MonitoringScope::G1MonitoringScope(G1MonitoringSupport* monitoring_support, bool full_gc, bool all_memory_pools_affected) :
  _monitoring_support(monitoring_support),
  _tcs(full_gc ? monitoring_support->_full_collection_counters : monitoring_support->_incremental_collection_counters),
  _tms(full_gc ? &monitoring_support->_full_gc_memory_manager : &monitoring_support->_incremental_memory_manager,
       G1CollectedHeap::heap()->gc_cause(), all_memory_pools_affected) {
}

G1MonitoringScope::~G1MonitoringScope() {
  _monitoring_support->update_sizes();
  // Needs to be called after updating pool sizes.
  MemoryService::track_memory_usage();
}
