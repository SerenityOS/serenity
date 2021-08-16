/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCPOLICYCOUNTERS_HPP
#define SHARE_GC_SHARED_GCPOLICYCOUNTERS_HPP

#include "runtime/perfData.hpp"

// GCPolicyCounters is a holder class for performance counters
// that track a generation

class GCPolicyCounters: public CHeapObj<mtGC> {
  friend class VMStructs;

  // Constant PerfData types don't need to retain a reference.
  // However, it's a good idea to document them here.
  // PerfStringConstant* _name;
  // PerfStringConstant* _collector_size;
  // PerfStringConstant* _generation_size;

  PerfVariable* _tenuring_threshold;
  PerfVariable* _desired_survivor_size;
  PerfVariable* _gc_overhead_limit_exceeded_counter;

  const char* _name_space;

public:
  enum Name {
    NONE,
    GCPolicyCountersKind,
    GCAdaptivePolicyCountersKind,
    PSGCAdaptivePolicyCountersKind
  };

  GCPolicyCounters(const char* name, int collectors, int generations);

  inline PerfVariable* tenuring_threshold() const  {
    return _tenuring_threshold;
  }

  inline PerfVariable* desired_survivor_size() const  {
    return _desired_survivor_size;
  }

  inline PerfVariable* gc_overhead_limit_exceeded_counter() const {
    return _gc_overhead_limit_exceeded_counter;
  }

  const char* name_space() const { return _name_space; }

  virtual void update_counters() {}

  virtual GCPolicyCounters::Name kind() const {
    return GCPolicyCounters::GCPolicyCountersKind;
  }
};

#endif // SHARE_GC_SHARED_GCPOLICYCOUNTERS_HPP
