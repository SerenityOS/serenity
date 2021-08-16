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

#ifndef SHARE_GC_G1_G1MEMORYPOOL_HPP
#define SHARE_GC_G1_G1MEMORYPOOL_HPP

#include "gc/g1/g1MonitoringSupport.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryUsage.hpp"

// This file contains the three classes that represent the memory
// pools of the G1 spaces: G1EdenPool, G1SurvivorPool, and
// G1OldGenPool. In G1, unlike our other GCs, we do not have a
// physical space for each of those spaces. Instead, we allocate
// regions for all three spaces out of a single pool of regions (that
// pool basically covers the entire heap). As a result, the eden,
// survivor, and old gen are considered logical spaces in G1, as each
// is a set of non-contiguous regions. This is also reflected in the
// way we map them to memory pools here. The easiest way to have done
// this would have been to map the entire G1 heap to a single memory
// pool. However, it's helpful to show how large the eden and survivor
// get, as this does affect the performance and behavior of G1. Which
// is why we introduce the three memory pools implemented here.
//
// See comments in g1MonitoringSupport.hpp for additional details
// on this model.
//

class G1CollectedHeap;

// This class is shared by the three G1 memory pool classes
// (G1EdenPool, G1SurvivorPool, G1OldGenPool).
class G1MemoryPoolSuper : public CollectedMemoryPool {
protected:
  G1MonitoringSupport* _g1mm;

  // Would only be called from subclasses.
  G1MemoryPoolSuper(G1CollectedHeap* g1h,
                    const char* name,
                    size_t init_size,
                    size_t max_size,
                    bool support_usage_threshold);
};

// Memory pool that represents the G1 eden.
class G1EdenPool : public G1MemoryPoolSuper {
public:
  G1EdenPool(G1CollectedHeap* g1h, size_t initial_size);

  size_t used_in_bytes() { return _g1mm->eden_space_used(); }

  MemoryUsage get_memory_usage();
};

// Memory pool that represents the G1 survivor.
class G1SurvivorPool : public G1MemoryPoolSuper {
public:
  G1SurvivorPool(G1CollectedHeap* g1h, size_t initial_size);

  size_t used_in_bytes() { return _g1mm->survivor_space_used(); }

  MemoryUsage get_memory_usage();
};

// Memory pool that represents the G1 old gen.
class G1OldGenPool : public G1MemoryPoolSuper {
public:
  G1OldGenPool(G1CollectedHeap* g1h, size_t initial_size, size_t max_size);

  size_t used_in_bytes() { return _g1mm->old_gen_used(); }

  MemoryUsage get_memory_usage();
};

#endif // SHARE_GC_G1_G1MEMORYPOOL_HPP
