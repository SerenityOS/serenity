/*
 * Copyright (c) 2016, 2020, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.inline.hpp"
#include "gc/shenandoah/shenandoahHeapRegionSet.hpp"
#include "gc/shenandoah/shenandoahHeapRegionCounters.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "runtime/perfData.inline.hpp"

ShenandoahHeapRegionCounters::ShenandoahHeapRegionCounters() :
  _last_sample_millis(0)
{
  if (UsePerfData && ShenandoahRegionSampling) {
    EXCEPTION_MARK;
    ResourceMark rm;
    ShenandoahHeap* heap = ShenandoahHeap::heap();
    size_t num_regions = heap->num_regions();
    const char* cns = PerfDataManager::name_space("shenandoah", "regions");
    _name_space = NEW_C_HEAP_ARRAY(char, strlen(cns)+1, mtGC);
    strcpy(_name_space, cns);

    const char* cname = PerfDataManager::counter_name(_name_space, "timestamp");
    _timestamp = PerfDataManager::create_long_variable(SUN_GC, cname, PerfData::U_None, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "max_regions");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_None, num_regions, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "region_size");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_None, ShenandoahHeapRegion::region_size_bytes() >> 10, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "status");
    _status = PerfDataManager::create_long_variable(SUN_GC, cname,
                                                    PerfData::U_None, CHECK);

    _regions_data = NEW_C_HEAP_ARRAY(PerfVariable*, num_regions, mtGC);
    for (uint i = 0; i < num_regions; i++) {
      const char* reg_name = PerfDataManager::name_space(_name_space, "region", i);
      const char* data_name = PerfDataManager::counter_name(reg_name, "data");
      const char* ns = PerfDataManager::ns_to_string(SUN_GC);
      const char* fullname = PerfDataManager::counter_name(ns, data_name);
      assert(!PerfDataManager::exists(fullname), "must not exist");
      _regions_data[i] = PerfDataManager::create_long_variable(SUN_GC, data_name,
                                                               PerfData::U_None, CHECK);
    }
  }
}

ShenandoahHeapRegionCounters::~ShenandoahHeapRegionCounters() {
  if (_name_space != NULL) FREE_C_HEAP_ARRAY(char, _name_space);
}

void ShenandoahHeapRegionCounters::update() {
  if (ShenandoahRegionSampling) {
    jlong current = nanos_to_millis(os::javaTimeNanos());
    jlong last = _last_sample_millis;
    if (current - last > ShenandoahRegionSamplingRate &&
            Atomic::cmpxchg(&_last_sample_millis, last, current) == last) {

      ShenandoahHeap* heap = ShenandoahHeap::heap();
      jlong status = 0;
      if (heap->is_concurrent_mark_in_progress())      status |= 1 << 0;
      if (heap->is_evacuation_in_progress())           status |= 1 << 1;
      if (heap->is_update_refs_in_progress())          status |= 1 << 2;
      _status->set_value(status);

      _timestamp->set_value(os::elapsed_counter());

      size_t num_regions = heap->num_regions();

      {
        ShenandoahHeapLocker locker(heap->lock());
        size_t rs = ShenandoahHeapRegion::region_size_bytes();
        for (uint i = 0; i < num_regions; i++) {
          ShenandoahHeapRegion* r = heap->get_region(i);
          jlong data = 0;
          data |= ((100 * r->used() / rs)                & PERCENT_MASK) << USED_SHIFT;
          data |= ((100 * r->get_live_data_bytes() / rs) & PERCENT_MASK) << LIVE_SHIFT;
          data |= ((100 * r->get_tlab_allocs() / rs)     & PERCENT_MASK) << TLAB_SHIFT;
          data |= ((100 * r->get_gclab_allocs() / rs)    & PERCENT_MASK) << GCLAB_SHIFT;
          data |= ((100 * r->get_shared_allocs() / rs)   & PERCENT_MASK) << SHARED_SHIFT;
          data |= (r->state_ordinal() & STATUS_MASK) << STATUS_SHIFT;
          _regions_data[i]->set_value(data);
        }
      }

    }
  }
}
