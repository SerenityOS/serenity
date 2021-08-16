/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZVIRTUALMEMORY_INLINE_HPP
#define SHARE_GC_Z_ZVIRTUALMEMORY_INLINE_HPP

#include "gc/z/zVirtualMemory.hpp"

#include "gc/z/zMemory.inline.hpp"

inline ZVirtualMemory::ZVirtualMemory() :
    _start(UINTPTR_MAX),
    _end(UINTPTR_MAX) {}

inline ZVirtualMemory::ZVirtualMemory(uintptr_t start, size_t size) :
    _start(start),
    _end(start + size) {}

inline bool ZVirtualMemory::is_null() const {
  return _start == UINTPTR_MAX;
}

inline uintptr_t ZVirtualMemory::start() const {
  return _start;
}

inline uintptr_t ZVirtualMemory::end() const {
  return _end;
}

inline size_t ZVirtualMemory::size() const {
  return _end - _start;
}

inline ZVirtualMemory ZVirtualMemory::split(size_t size) {
  _start += size;
  return ZVirtualMemory(_start - size, size);
}

#endif // SHARE_GC_Z_ZVIRTUALMEMORY_INLINE_HPP
