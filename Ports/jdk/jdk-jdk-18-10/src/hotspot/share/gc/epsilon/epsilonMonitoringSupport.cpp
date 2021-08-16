/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
#include "gc/epsilon/epsilonMonitoringSupport.hpp"
#include "gc/epsilon/epsilonHeap.hpp"
#include "gc/shared/generationCounters.hpp"
#include "memory/allocation.hpp"
#include "memory/metaspaceCounters.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/perfData.hpp"
#include "services/memoryService.hpp"

class EpsilonSpaceCounters: public CHeapObj<mtGC> {
  friend class VMStructs;

private:
  PerfVariable* _capacity;
  PerfVariable* _used;
  char*         _name_space;

public:
  EpsilonSpaceCounters(const char* name,
                 int ordinal,
                 size_t max_size,
                 size_t initial_capacity,
                 GenerationCounters* gc) {
    if (UsePerfData) {
      EXCEPTION_MARK;
      ResourceMark rm;

      const char* cns = PerfDataManager::name_space(gc->name_space(), "space", ordinal);

      _name_space = NEW_C_HEAP_ARRAY(char, strlen(cns)+1, mtGC);
      strcpy(_name_space, cns);

      const char* cname = PerfDataManager::counter_name(_name_space, "name");
      PerfDataManager::create_string_constant(SUN_GC, cname, name, CHECK);

      cname = PerfDataManager::counter_name(_name_space, "maxCapacity");
      PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes, (jlong)max_size, CHECK);

      cname = PerfDataManager::counter_name(_name_space, "capacity");
      _capacity = PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes, initial_capacity, CHECK);

      cname = PerfDataManager::counter_name(_name_space, "used");
      _used = PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes, (jlong) 0, CHECK);

      cname = PerfDataManager::counter_name(_name_space, "initCapacity");
      PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes, initial_capacity, CHECK);
    }
  }

  ~EpsilonSpaceCounters() {
    FREE_C_HEAP_ARRAY(char, _name_space);
  }

  inline void update_all(size_t capacity, size_t used) {
    _capacity->set_value(capacity);
    _used->set_value(used);
  }
};

class EpsilonGenerationCounters : public GenerationCounters {
private:
  EpsilonHeap* _heap;
public:
  EpsilonGenerationCounters(EpsilonHeap* heap) :
          GenerationCounters("Heap", 1, 1, 0, heap->max_capacity(), heap->capacity()),
          _heap(heap)
  {};

  virtual void update_all() {
    _current_size->set_value(_heap->capacity());
  }
};

EpsilonMonitoringSupport::EpsilonMonitoringSupport(EpsilonHeap* heap) {
  _heap_counters  = new EpsilonGenerationCounters(heap);
  _space_counters = new EpsilonSpaceCounters("Heap", 0, heap->max_capacity(), 0, _heap_counters);
}

void EpsilonMonitoringSupport::update_counters() {
  MemoryService::track_memory_usage();

  if (UsePerfData) {
    EpsilonHeap* heap = EpsilonHeap::heap();
    size_t used = heap->used();
    size_t capacity = heap->capacity();
    _heap_counters->update_all();
    _space_counters->update_all(capacity, used);
    MetaspaceCounters::update_performance_counters();
  }
}

