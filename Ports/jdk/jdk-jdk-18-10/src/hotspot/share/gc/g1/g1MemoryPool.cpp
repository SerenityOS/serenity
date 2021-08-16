/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/g1MemoryPool.hpp"
#include "gc/g1/heapRegion.hpp"

G1MemoryPoolSuper::G1MemoryPoolSuper(G1CollectedHeap* g1h,
                                     const char* name,
                                     size_t init_size,
                                     size_t max_size,
                                     bool support_usage_threshold) :
  CollectedMemoryPool(name,
                      init_size,
                      max_size,
                      support_usage_threshold),
  _g1mm(g1h->monitoring_support()) {
  assert(UseG1GC, "sanity");
}

G1EdenPool::G1EdenPool(G1CollectedHeap* g1h, size_t initial_size) :
  G1MemoryPoolSuper(g1h,
                    "G1 Eden Space",
                    initial_size,
                    MemoryUsage::undefined_size(),
                    false /* support_usage_threshold */) { }

MemoryUsage G1EdenPool::get_memory_usage() {
  return _g1mm->eden_space_memory_usage(initial_size(), max_size());
}

G1SurvivorPool::G1SurvivorPool(G1CollectedHeap* g1h, size_t initial_size) :
  G1MemoryPoolSuper(g1h,
                    "G1 Survivor Space",
                    initial_size,
                    MemoryUsage::undefined_size(),
                    false /* support_usage_threshold */) { }

MemoryUsage G1SurvivorPool::get_memory_usage() {
  return _g1mm->survivor_space_memory_usage(initial_size(), max_size());
}

G1OldGenPool::G1OldGenPool(G1CollectedHeap* g1h, size_t initial_size, size_t max_size) :
  G1MemoryPoolSuper(g1h,
                    "G1 Old Gen",
                    initial_size,
                    max_size,
                    true /* support_usage_threshold */) { }

MemoryUsage G1OldGenPool::get_memory_usage() {
  return _g1mm->old_gen_memory_usage(initial_size(), max_size());
}
