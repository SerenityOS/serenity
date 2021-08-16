/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZVIRTUALMEMORY_HPP
#define SHARE_GC_Z_ZVIRTUALMEMORY_HPP

#include "gc/z/zMemory.hpp"

class ZVirtualMemory {
  friend class VMStructs;

private:
  uintptr_t _start;
  uintptr_t _end;

public:
  ZVirtualMemory();
  ZVirtualMemory(uintptr_t start, size_t size);

  bool is_null() const;
  uintptr_t start() const;
  uintptr_t end() const;
  size_t size() const;

  ZVirtualMemory split(size_t size);
};

class ZVirtualMemoryManager {
private:
  ZMemoryManager _manager;
  bool           _initialized;

  // Platform specific implementation
  void pd_initialize_before_reserve();
  void pd_initialize_after_reserve();
  bool pd_reserve(uintptr_t addr, size_t size);
  void pd_unreserve(uintptr_t addr, size_t size);

  bool reserve_contiguous(uintptr_t start, size_t size);
  bool reserve_contiguous(size_t size);
  size_t reserve_discontiguous(uintptr_t start, size_t size, size_t min_range);
  size_t reserve_discontiguous(size_t size);
  bool reserve(size_t max_capacity);

  void nmt_reserve(uintptr_t start, size_t size);

public:
  ZVirtualMemoryManager(size_t max_capacity);

  bool is_initialized() const;

  ZVirtualMemory alloc(size_t size, bool force_low_address);
  void free(const ZVirtualMemory& vmem);
};

#endif // SHARE_GC_Z_ZVIRTUALMEMORY_HPP
