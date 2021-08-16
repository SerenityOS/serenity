/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/generation.hpp"
#include "gc/shared/genMemoryPools.hpp"
#include "gc/shared/space.hpp"
#if INCLUDE_SERIALGC
#include "gc/serial/defNewGeneration.hpp"
#endif

ContiguousSpacePool::ContiguousSpacePool(ContiguousSpace* space,
                                         const char* name,
                                         size_t max_size,
                                         bool support_usage_threshold) :
  CollectedMemoryPool(name, space->capacity(), max_size,
                      support_usage_threshold), _space(space) {
}

size_t ContiguousSpacePool::used_in_bytes() {
  return space()->used();
}

MemoryUsage ContiguousSpacePool::get_memory_usage() {
  size_t maxSize   = (available_for_allocation() ? max_size() : 0);
  size_t used      = used_in_bytes();
  size_t committed = _space->capacity();

  return MemoryUsage(initial_size(), used, committed, maxSize);
}

#if INCLUDE_SERIALGC

SurvivorContiguousSpacePool::SurvivorContiguousSpacePool(DefNewGeneration* young_gen,
                                                         const char* name,
                                                         size_t max_size,
                                                         bool support_usage_threshold) :
  CollectedMemoryPool(name, young_gen->from()->capacity(), max_size,
                      support_usage_threshold), _young_gen(young_gen) {
}

size_t SurvivorContiguousSpacePool::used_in_bytes() {
  return _young_gen->from()->used();
}

size_t SurvivorContiguousSpacePool::committed_in_bytes() {
  return _young_gen->from()->capacity();
}

MemoryUsage SurvivorContiguousSpacePool::get_memory_usage() {
  size_t maxSize = (available_for_allocation() ? max_size() : 0);
  size_t used    = used_in_bytes();
  size_t committed = committed_in_bytes();

  return MemoryUsage(initial_size(), used, committed, maxSize);
}

#endif // INCLUDE_SERIALGC

GenerationPool::GenerationPool(Generation* gen,
                               const char* name,
                               bool support_usage_threshold) :
  CollectedMemoryPool(name, gen->capacity(), gen->max_capacity(),
                      support_usage_threshold), _gen(gen) {
}

size_t GenerationPool::used_in_bytes() {
  return _gen->used();
}

MemoryUsage GenerationPool::get_memory_usage() {
  size_t used      = used_in_bytes();
  size_t committed = _gen->capacity();
  size_t maxSize   = (available_for_allocation() ? max_size() : 0);

  return MemoryUsage(initial_size(), used, committed, maxSize);
}
