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

#include "precompiled.hpp"
#include "gc/g1/g1OldGenAllocationTracker.hpp"
#include "logging/log.hpp"

G1OldGenAllocationTracker::G1OldGenAllocationTracker() :
  _last_period_old_gen_bytes(0),
  _last_period_old_gen_growth(0),
  _humongous_bytes_after_last_gc(0),
  _allocated_bytes_since_last_gc(0),
  _allocated_humongous_bytes_since_last_gc(0) {
}

void G1OldGenAllocationTracker::reset_after_gc(size_t humongous_bytes_after_gc) {
  // Calculate actual increase in old, taking eager reclaim into consideration.
  size_t last_period_humongous_increase = 0;
  if (humongous_bytes_after_gc > _humongous_bytes_after_last_gc) {
    last_period_humongous_increase = humongous_bytes_after_gc - _humongous_bytes_after_last_gc;
    assert(last_period_humongous_increase <= _allocated_humongous_bytes_since_last_gc,
           "Increase larger than allocated " SIZE_FORMAT " <= " SIZE_FORMAT,
           last_period_humongous_increase, _allocated_humongous_bytes_since_last_gc);
  }
  _last_period_old_gen_growth = _allocated_bytes_since_last_gc + last_period_humongous_increase;

  // Calculate and record needed values.
  _last_period_old_gen_bytes = _allocated_bytes_since_last_gc + _allocated_humongous_bytes_since_last_gc;
  _humongous_bytes_after_last_gc = humongous_bytes_after_gc;

  log_debug(gc, alloc, stats)("Old generation allocation in the last mutator period, "
                              "old gen allocated: " SIZE_FORMAT "B, humongous allocated: " SIZE_FORMAT "B,"
                              "old gen growth: " SIZE_FORMAT "B.",
                              _allocated_bytes_since_last_gc,
                              _allocated_humongous_bytes_since_last_gc,
                              _last_period_old_gen_growth);

  // Reset for next mutator period.
  _allocated_bytes_since_last_gc = 0;
  _allocated_humongous_bytes_since_last_gc = 0;
}
