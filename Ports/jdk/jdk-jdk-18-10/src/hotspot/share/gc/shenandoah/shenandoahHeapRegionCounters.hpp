/*
 * Copyright (c) 2016, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONCOUNTERS_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONCOUNTERS_HPP

#include "memory/allocation.hpp"

/**
 * This provides the following in JVMStat:
 *
 * constants:
 * - sun.gc.shenandoah.regions.timestamp    the timestamp for this sample
 * - sun.gc.shenandoah.regions.max_regions  maximum number of regions
 * - sun.gc.shenandoah.regions.region_size  size per region, in kilobytes
 *
 * variables:
 * - sun.gc.shenandoah.regions.status       current GC status:
 *     - bit 0 set when marking in progress
 *     - bit 1 set when evacuation in progress
 *     - bit 2 set when update refs in progress
 *
 * two variable counters per region, with $max_regions (see above) counters:
 * - sun.gc.shenandoah.regions.region.$i.data
 * where $ is the region number from 0 <= i < $max_regions
 *
 * .data is in the following format:
 * - bits 0-6    used memory in percent
 * - bits 7-13   live memory in percent
 * - bits 14-20  tlab allocated memory in percent
 * - bits 21-27  gclab allocated memory in percent
 * - bits 28-34  shared allocated memory in percent
 * - bits 35-41  <reserved>
 * - bits 42-50  <reserved>
 * - bits 51-57  <reserved>
 * - bits 58-63  status
 *      - bits describe the state as recorded in ShenandoahHeapRegion
 */
class ShenandoahHeapRegionCounters : public CHeapObj<mtGC>  {
private:
  static const jlong PERCENT_MASK = 0x7f;
  static const jlong STATUS_MASK  = 0x3f;

  static const jlong USED_SHIFT   = 0;
  static const jlong LIVE_SHIFT   = 7;
  static const jlong TLAB_SHIFT   = 14;
  static const jlong GCLAB_SHIFT  = 21;
  static const jlong SHARED_SHIFT = 28;

  static const jlong STATUS_SHIFT = 58;

  char* _name_space;
  PerfLongVariable** _regions_data;
  PerfLongVariable* _timestamp;
  PerfLongVariable* _status;
  volatile jlong _last_sample_millis;

public:
  ShenandoahHeapRegionCounters();
  ~ShenandoahHeapRegionCounters();
  void update();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONCOUNTERS_HPP
