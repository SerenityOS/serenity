/*
 * Copyright (c) 2015, 2019, Red Hat, Inc. All rights reserved.
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
#include "gc/shared/collectorCounters.hpp"
#include "gc/shared/generationCounters.hpp"
#include "gc/shared/hSpaceCounters.hpp"
#include "gc/shenandoah/shenandoahMonitoringSupport.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegionCounters.hpp"
#include "memory/metaspaceCounters.hpp"
#include "services/memoryService.hpp"

class ShenandoahYoungGenerationCounters : public GenerationCounters {
public:
  ShenandoahYoungGenerationCounters() :
          GenerationCounters("Young", 0, 0, 0, (size_t)0, (size_t)0) {};

  virtual void update_all() {
    // no update
  }
};

class ShenandoahGenerationCounters : public GenerationCounters {
private:
  ShenandoahHeap* _heap;
public:
  ShenandoahGenerationCounters(ShenandoahHeap* heap) :
          GenerationCounters("Heap", 1, 1, heap->initial_capacity(), heap->max_capacity(), heap->capacity()),
          _heap(heap)
  {};

  virtual void update_all() {
    _current_size->set_value(_heap->capacity());
  }
};

ShenandoahMonitoringSupport::ShenandoahMonitoringSupport(ShenandoahHeap* heap) :
        _partial_counters(NULL),
        _full_counters(NULL)
{
  // Collection counters do not fit Shenandoah very well.
  // We record partial cycles as "young", and full cycles (including full STW GC) as "old".
  _partial_counters  = new CollectorCounters("Shenandoah partial", 0);
  _full_counters     = new CollectorCounters("Shenandoah full",    1);

  // We report young gen as unused.
  _young_counters = new ShenandoahYoungGenerationCounters();
  _heap_counters  = new ShenandoahGenerationCounters(heap);
  _space_counters = new HSpaceCounters(_heap_counters->name_space(), "Heap", 0, heap->max_capacity(), heap->initial_capacity());

  _heap_region_counters = new ShenandoahHeapRegionCounters();
}

CollectorCounters* ShenandoahMonitoringSupport::stw_collection_counters() {
  return _full_counters;
}

CollectorCounters* ShenandoahMonitoringSupport::full_stw_collection_counters() {
  return _full_counters;
}

CollectorCounters* ShenandoahMonitoringSupport::concurrent_collection_counters() {
  return _full_counters;
}

CollectorCounters* ShenandoahMonitoringSupport::partial_collection_counters() {
  return _partial_counters;
}

void ShenandoahMonitoringSupport::update_counters() {
  MemoryService::track_memory_usage();

  if (UsePerfData) {
    ShenandoahHeap* heap = ShenandoahHeap::heap();
    size_t used = heap->used();
    size_t capacity = heap->max_capacity();
    _heap_counters->update_all();
    _space_counters->update_all(capacity, used);
    _heap_region_counters->update();

    MetaspaceCounters::update_performance_counters();
  }
}
