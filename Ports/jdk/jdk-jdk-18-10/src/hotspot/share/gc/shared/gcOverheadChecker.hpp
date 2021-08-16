/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Google and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCOVERHEADCHECKER_HPP
#define SHARE_GC_SHARED_GCOVERHEADCHECKER_HPP

#include "memory/allocation.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gc_globals.hpp"
#include "runtime/globals.hpp"

class SoftRefPolicy;

class GCOverheadTester: public StackObj {
public:
  virtual bool is_exceeded() = 0;
};

class GCOverheadChecker: public CHeapObj<mtGC> {
  // This is a hint for the heap:  we've detected that GC times
  // are taking longer than GCTimeLimit allows.
  bool _gc_overhead_limit_exceeded;
  // Use for diagnostics only.  If UseGCOverheadLimit is false,
  // this variable is still set.
  bool _print_gc_overhead_limit_would_be_exceeded;
  // Count of consecutive GC that have exceeded the
  // GC time limit criterion
  uint _gc_overhead_limit_count;
  // This flag signals that GCTimeLimit is being exceeded
  // but may not have done so for the required number of consecutive
  // collections

public:
  GCOverheadChecker();

  // This is a hint for the heap:  we've detected that gc times
  // are taking longer than GCTimeLimit allows.
  // Most heaps will choose to throw an OutOfMemoryError when
  // this occurs but it is up to the heap to request this information
  // of the policy
  bool gc_overhead_limit_exceeded() {
    return _gc_overhead_limit_exceeded;
  }
  void set_gc_overhead_limit_exceeded(bool v) {
    _gc_overhead_limit_exceeded = v;
  }

  // Tests conditions indicate the GC overhead limit is being approached.
  bool gc_overhead_limit_near() {
    return _gc_overhead_limit_count >= (GCOverheadLimitThreshold - 1);
  }
  void reset_gc_overhead_limit_count() {
    _gc_overhead_limit_count = 0;
  }

  // Check the conditions for an out-of-memory due to excessive GC time.
  // Set _gc_overhead_limit_exceeded if all the conditions have been met.
  void check_gc_overhead_limit(GCOverheadTester* time_overhead,
                               GCOverheadTester* space_overhead,
                               bool is_full_gc,
                               GCCause::Cause gc_cause,
                               SoftRefPolicy* soft_ref_policy);
};

#endif // SHARE_GC_SHARED_GCOVERHEADCHECKER_HPP
