/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZPAGEALLOCATOR_INLINE_HPP
#define SHARE_GC_Z_ZPAGEALLOCATOR_INLINE_HPP

#include "gc/z/zPageAllocator.hpp"

inline ZPageAllocatorStats::ZPageAllocatorStats(size_t min_capacity,
                                                size_t max_capacity,
                                                size_t soft_max_capacity,
                                                size_t capacity,
                                                size_t used,
                                                size_t used_high,
                                                size_t used_low,
                                                size_t reclaimed) :
    _min_capacity(min_capacity),
    _max_capacity(max_capacity),
    _soft_max_capacity(soft_max_capacity),
    _capacity(capacity),
    _used(used),
    _used_high(used_high),
    _used_low(used_low),
    _reclaimed(reclaimed) {}

inline size_t ZPageAllocatorStats::min_capacity() const {
  return _min_capacity;
}

inline size_t ZPageAllocatorStats::max_capacity() const {
  return _max_capacity;
}

inline size_t ZPageAllocatorStats::soft_max_capacity() const {
  return _soft_max_capacity;
}

inline size_t ZPageAllocatorStats::capacity() const {
  return _capacity;
}

inline size_t ZPageAllocatorStats::used() const {
  return _used;
}

inline size_t ZPageAllocatorStats::used_high() const {
  return _used_high;
}

inline size_t ZPageAllocatorStats::used_low() const {
  return _used_low;
}

inline size_t ZPageAllocatorStats::reclaimed() const {
  return _reclaimed;
}

#endif // SHARE_GC_Z_ZPAGEALLOCATOR_INLINE_HPP
