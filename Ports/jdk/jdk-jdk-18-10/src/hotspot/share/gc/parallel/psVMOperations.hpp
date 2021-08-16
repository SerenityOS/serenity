/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSVMOPERATIONS_HPP
#define SHARE_GC_PARALLEL_PSVMOPERATIONS_HPP

#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/shared/gcCause.hpp"
#include "gc/shared/gcVMOperations.hpp"

class VM_ParallelGCFailedAllocation : public VM_CollectForAllocation {
 public:
  VM_ParallelGCFailedAllocation(size_t word_size, uint gc_count);

  virtual VMOp_Type type() const {
    return VMOp_ParallelGCFailedAllocation;
  }
  virtual void doit();
};

class VM_ParallelGCSystemGC: public VM_GC_Operation {
 public:
  VM_ParallelGCSystemGC(uint gc_count, uint full_gc_count, GCCause::Cause gc_cause);
  virtual VMOp_Type type() const { return VMOp_ParallelGCSystemGC; }
  virtual void doit();
};

#endif // SHARE_GC_PARALLEL_PSVMOPERATIONS_HPP
