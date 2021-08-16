/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/shared/generationCounters.hpp"
#include "gc/shared/hSpaceCounters.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zServiceability.hpp"
#include "memory/metaspaceCounters.hpp"
#include "runtime/perfData.hpp"

class ZGenerationCounters : public GenerationCounters {
public:
  ZGenerationCounters(const char* name, int ordinal, int spaces,
                      size_t min_capacity, size_t max_capacity, size_t curr_capacity) :
      GenerationCounters(name, ordinal, spaces,
                         min_capacity, max_capacity, curr_capacity) {}

  void update_capacity(size_t capacity) {
    _current_size->set_value(capacity);
  }
};

// Class to expose perf counters used by jstat.
class ZServiceabilityCounters : public CHeapObj<mtGC> {
private:
  ZGenerationCounters _generation_counters;
  HSpaceCounters      _space_counters;
  CollectorCounters   _collector_counters;

public:
  ZServiceabilityCounters(size_t min_capacity, size_t max_capacity);

  CollectorCounters* collector_counters();

  void update_sizes();
};

ZServiceabilityCounters::ZServiceabilityCounters(size_t min_capacity, size_t max_capacity) :
    // generation.1
    _generation_counters("old"        /* name */,
                         1            /* ordinal */,
                         1            /* spaces */,
                         min_capacity /* min_capacity */,
                         max_capacity /* max_capacity */,
                         min_capacity /* curr_capacity */),
    // generation.1.space.0
    _space_counters(_generation_counters.name_space(),
                    "space"      /* name */,
                    0            /* ordinal */,
                    max_capacity /* max_capacity */,
                    min_capacity /* init_capacity */),
    // gc.collector.2
    _collector_counters("Z concurrent cycle pauses" /* name */,
                        2                           /* ordinal */) {}

CollectorCounters* ZServiceabilityCounters::collector_counters() {
  return &_collector_counters;
}

void ZServiceabilityCounters::update_sizes() {
  if (UsePerfData) {
    const size_t capacity = ZHeap::heap()->capacity();
    const size_t used = MIN2(ZHeap::heap()->used(), capacity);

    _generation_counters.update_capacity(capacity);
    _space_counters.update_capacity(capacity);
    _space_counters.update_used(used);

    MetaspaceCounters::update_performance_counters();
  }
}

ZServiceabilityMemoryPool::ZServiceabilityMemoryPool(size_t min_capacity, size_t max_capacity) :
    CollectedMemoryPool("ZHeap",
                        min_capacity,
                        max_capacity,
                        true /* support_usage_threshold */) {}

size_t ZServiceabilityMemoryPool::used_in_bytes() {
  return ZHeap::heap()->used();
}

MemoryUsage ZServiceabilityMemoryPool::get_memory_usage() {
  const size_t committed = ZHeap::heap()->capacity();
  const size_t used      = MIN2(ZHeap::heap()->used(), committed);

  return MemoryUsage(initial_size(), used, committed, max_size());
}

ZServiceabilityMemoryManager::ZServiceabilityMemoryManager(const char* name,
                                                           const char* end_message,
                                                           ZServiceabilityMemoryPool* pool) :
    GCMemoryManager(name, end_message) {
  add_pool(pool);
}

ZServiceability::ZServiceability(size_t min_capacity, size_t max_capacity) :
    _min_capacity(min_capacity),
    _max_capacity(max_capacity),
    _memory_pool(_min_capacity, _max_capacity),
    _cycle_memory_manager("ZGC Cycles", "end of GC cycle", &_memory_pool),
    _pause_memory_manager("ZGC Pauses", "end of GC pause", &_memory_pool),
    _counters(NULL) {}

void ZServiceability::initialize() {
  _counters = new ZServiceabilityCounters(_min_capacity, _max_capacity);
}

MemoryPool* ZServiceability::memory_pool() {
  return &_memory_pool;
}

GCMemoryManager* ZServiceability::cycle_memory_manager() {
  return &_cycle_memory_manager;
}

GCMemoryManager* ZServiceability::pause_memory_manager() {
  return &_pause_memory_manager;
}

ZServiceabilityCounters* ZServiceability::counters() {
  return _counters;
}

ZServiceabilityCycleTracer::ZServiceabilityCycleTracer() :
    _memory_manager_stats(ZHeap::heap()->serviceability_cycle_memory_manager(),
                          ZCollectedHeap::heap()->gc_cause(),
                          true  /* allMemoryPoolsAffected */,
                          true  /* recordGCBeginTime */,
                          true  /* recordPreGCUsage */,
                          true  /* recordPeakUsage */,
                          true  /* recordPostGCUsage */,
                          true  /* recordAccumulatedGCTime */,
                          true  /* recordGCEndTime */,
                          true  /* countCollection */) {}

ZServiceabilityPauseTracer::ZServiceabilityPauseTracer() :
    _svc_gc_marker(SvcGCMarker::CONCURRENT),
    _counters_stats(ZHeap::heap()->serviceability_counters()->collector_counters()),
    _memory_manager_stats(ZHeap::heap()->serviceability_pause_memory_manager(),
                          ZCollectedHeap::heap()->gc_cause(),
                          true  /* allMemoryPoolsAffected */,
                          true  /* recordGCBeginTime */,
                          false /* recordPreGCUsage */,
                          false /* recordPeakUsage */,
                          false /* recordPostGCUsage */,
                          true  /* recordAccumulatedGCTime */,
                          true  /* recordGCEndTime */,
                          true  /* countCollection */) {}

ZServiceabilityPauseTracer::~ZServiceabilityPauseTracer()  {
  ZHeap::heap()->serviceability_counters()->update_sizes();
  MemoryService::track_memory_usage();
}
