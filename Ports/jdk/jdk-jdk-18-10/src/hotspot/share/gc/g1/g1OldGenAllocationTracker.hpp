/*
 * Copyright (c) 2020, Amazon.com, Inc. or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_GC_G1_G1OLDGENALLOCATIONTRACKER_HPP
#define SHARE_VM_GC_G1_G1OLDGENALLOCATIONTRACKER_HPP

#include "gc/g1/heapRegion.hpp"
#include "memory/allocation.hpp"

class G1AdaptiveIHOPControl;

// Track allocation details in the old generation.
class G1OldGenAllocationTracker : public CHeapObj<mtGC> {
  // Total number of bytes allocated in the old generaton during
  // last mutator period.
  size_t _last_period_old_gen_bytes;
  // Total growth of the old geneneration for last mutator period,
  // taking eager reclaim into consideration.
  size_t _last_period_old_gen_growth;

  // Total size of humongous objects for last gc.
  size_t _humongous_bytes_after_last_gc;

  // Non-humongous old generation allocations during last mutator period.
  size_t _allocated_bytes_since_last_gc;
  // Humongous allocations during last mutator period.
  size_t _allocated_humongous_bytes_since_last_gc;

public:
  G1OldGenAllocationTracker();

  void add_allocated_bytes_since_last_gc(size_t bytes) { _allocated_bytes_since_last_gc += bytes; }
  void add_allocated_humongous_bytes_since_last_gc(size_t bytes) { _allocated_humongous_bytes_since_last_gc += bytes; }

  // Record a humongous allocation in a collection pause. This allocation
  // is accounted to the previous mutator period.
  void record_collection_pause_humongous_allocation(size_t bytes) {
    _humongous_bytes_after_last_gc += bytes;
  }

  size_t last_period_old_gen_bytes() const { return _last_period_old_gen_bytes; }
  size_t last_period_old_gen_growth() const { return _last_period_old_gen_growth; };

  // Calculates and resets stats after a collection.
  void reset_after_gc(size_t humongous_bytes_after_gc);
};

#endif // SHARE_VM_GC_G1_G1OLDGENALLOCATIONTRACKER_HPP
