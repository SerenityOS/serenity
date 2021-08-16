/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1GCCOUNTERS_HPP
#define SHARE_GC_G1_G1GCCOUNTERS_HPP

#include "utilities/globalDefinitions.hpp"

class G1CollectedHeap;

// Record collection counters for later use when deciding whether a GC has
// been run since the counter state was recorded.
class G1GCCounters {
  uint _total_collections;
  uint _total_full_collections;
  uint _old_marking_cycles_started;

public:
  G1GCCounters() {}             // Uninitialized

  // Capture the current counters from the heap.  The caller must ensure no
  // collections will occur while this constructor is executing.
  explicit G1GCCounters(G1CollectedHeap* g1h);

  uint total_collections() const { return _total_collections; }
  uint total_full_collections() const { return _total_full_collections; }
  uint old_marking_cycles_started() const { return _old_marking_cycles_started; }
};

#endif // SHARE_GC_G1_G1GCCOUNTERS_HPP
