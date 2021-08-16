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

#include "precompiled.hpp"
#include "gc/parallel/psMemoryPool.hpp"

PSGenerationPool::PSGenerationPool(PSOldGen* old_gen,
                                   const char* name,
                                   bool support_usage_threshold) :
  CollectedMemoryPool(name, old_gen->capacity_in_bytes(),
                      old_gen->reserved().byte_size(), support_usage_threshold), _old_gen(old_gen) {
}

MemoryUsage PSGenerationPool::get_memory_usage() {
  size_t maxSize   = (available_for_allocation() ? max_size() : 0);
  size_t used      = used_in_bytes();
  size_t committed = _old_gen->capacity_in_bytes();

  return MemoryUsage(initial_size(), used, committed, maxSize);
}

// The max size of EdenMutableSpacePool =
//     max size of the PSYoungGen - capacity of two survivor spaces
//
// Max size of PS eden space is changing due to ergonomic.
// PSYoungGen, PSOldGen, Eden, Survivor spaces are all resizable.
//
EdenMutableSpacePool::EdenMutableSpacePool(PSYoungGen* young_gen,
                                           MutableSpace* space,
                                           const char* name,
                                           bool support_usage_threshold) :
  CollectedMemoryPool(name, space->capacity_in_bytes(),
                      (young_gen->max_gen_size() -
                       young_gen->from_space()->capacity_in_bytes() -
                       young_gen->to_space()->capacity_in_bytes()),
                      support_usage_threshold),
  _young_gen(young_gen),
  _space(space) {
}

MemoryUsage EdenMutableSpacePool::get_memory_usage() {
  size_t maxSize   = (available_for_allocation() ? max_size() : 0);
  size_t used = used_in_bytes();
  size_t committed = _space->capacity_in_bytes();

  return MemoryUsage(initial_size(), used, committed, maxSize);
}

// The max size of SurvivorMutableSpacePool =
//     current capacity of the from-space
//
// PS from and to survivor spaces could have different sizes.
//
SurvivorMutableSpacePool::SurvivorMutableSpacePool(PSYoungGen* young_gen,
                                                   const char* name,
                                                   bool support_usage_threshold) :
  CollectedMemoryPool(name, young_gen->from_space()->capacity_in_bytes(),
                      young_gen->from_space()->capacity_in_bytes(),
                      support_usage_threshold), _young_gen(young_gen) {
}

MemoryUsage SurvivorMutableSpacePool::get_memory_usage() {
  size_t maxSize = (available_for_allocation() ? max_size() : 0);
  size_t used    = used_in_bytes();
  size_t committed = committed_in_bytes();
  return MemoryUsage(initial_size(), used, committed, maxSize);
}
