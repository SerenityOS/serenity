/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_EPSILON_EPSILONMEMORYPOOL_HPP
#define SHARE_GC_EPSILON_EPSILONMEMORYPOOL_HPP

#include "gc/epsilon/epsilonHeap.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryUsage.hpp"
#include "utilities/macros.hpp"

class EpsilonMemoryPool : public CollectedMemoryPool {
private:
  EpsilonHeap* _heap;

public:
  EpsilonMemoryPool(EpsilonHeap* heap);
  size_t committed_in_bytes() { return _heap->capacity();     }
  size_t used_in_bytes()      { return _heap->used();         }
  size_t max_size()     const { return _heap->max_capacity(); }
  MemoryUsage get_memory_usage();
};

#endif // SHARE_GC_EPSILON_EPSILONMEMORYPOOL_HPP
