/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1HEAPSIZINGPOLICY_HPP
#define SHARE_GC_G1_G1HEAPSIZINGPOLICY_HPP

#include "memory/allocation.hpp"

class G1Analytics;
class G1CollectedHeap;

class G1HeapSizingPolicy: public CHeapObj<mtGC> {
  // MinOverThresholdForGrowth must be less than the number of recorded
  // pause times in G1Analytics, representing the minimum number of pause
  // time ratios that exceed GCTimeRatio before a heap expansion will be triggered.
  const static uint MinOverThresholdForGrowth = 4;

  const G1CollectedHeap* _g1h;
  const G1Analytics* _analytics;

  const uint _num_prev_pauses_for_heuristics;
  // Ratio check data for determining if heap growth is necessary.
  uint _ratio_over_threshold_count;
  double _ratio_over_threshold_sum;
  uint _pauses_since_start;

  // Scale "full" gc pause time threshold with heap size as we want to resize more
  // eagerly at small heap sizes.
  double scale_with_heap(double pause_time_threshold);

  G1HeapSizingPolicy(const G1CollectedHeap* g1h, const G1Analytics* analytics);
public:

  // If an expansion would be appropriate, because recent GC overhead had
  // exceeded the desired limit, return an amount to expand by.
  size_t young_collection_expansion_amount();

  // Returns the amount of bytes to resize the heap; if expand is set, the heap
  // should by expanded by that amount, shrunk otherwise.
  size_t full_collection_resize_amount(bool& expand);
  // Clear ratio tracking data used by expansion_amount().
  void clear_ratio_check_data();

  static G1HeapSizingPolicy* create(const G1CollectedHeap* g1h, const G1Analytics* analytics);
};

#endif // SHARE_GC_G1_G1HEAPSIZINGPOLICY_HPP
