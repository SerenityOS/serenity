/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_PSMEMORYPOOL_HPP
#define SHARE_GC_PARALLEL_PSMEMORYPOOL_HPP

#include "gc/parallel/mutableSpace.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psYoungGen.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryUsage.hpp"

class PSGenerationPool : public CollectedMemoryPool {
private:
  PSOldGen* _old_gen;

public:
  PSGenerationPool(PSOldGen* pool, const char* name, bool support_usage_threshold);

  MemoryUsage get_memory_usage();
  size_t used_in_bytes() { return _old_gen->used_in_bytes(); }
  size_t max_size() const { return _old_gen->reserved().byte_size(); }
};

class EdenMutableSpacePool : public CollectedMemoryPool {
private:
  PSYoungGen*   _young_gen;
  MutableSpace* _space;

public:
  EdenMutableSpacePool(PSYoungGen* young_gen,
                       MutableSpace* space,
                       const char* name,
                       bool support_usage_threshold);

  MutableSpace* space()                     { return _space; }
  MemoryUsage get_memory_usage();
  size_t used_in_bytes()                    { return space()->used_in_bytes(); }
  size_t max_size() const {
    // Eden's max_size = max_size of Young Gen - the current committed size of survivor spaces
    return _young_gen->max_gen_size() -
           _young_gen->from_space()->capacity_in_bytes() -
           _young_gen->to_space()->capacity_in_bytes();
  }
};

class SurvivorMutableSpacePool : public CollectedMemoryPool {
private:
  PSYoungGen*   _young_gen;

public:
  SurvivorMutableSpacePool(PSYoungGen* young_gen,
                           const char* name,
                           bool support_usage_threshold);

  MemoryUsage get_memory_usage();

  size_t used_in_bytes() {
    return _young_gen->from_space()->used_in_bytes();
  }
  size_t committed_in_bytes() {
    return _young_gen->from_space()->capacity_in_bytes();
  }
  size_t max_size() const {
    // Return current committed size of the from-space
    return _young_gen->from_space()->capacity_in_bytes();
  }
};

#endif // SHARE_GC_PARALLEL_PSMEMORYPOOL_HPP
