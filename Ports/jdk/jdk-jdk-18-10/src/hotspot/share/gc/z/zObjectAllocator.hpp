/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZOBJECTALLOCATOR_HPP
#define SHARE_GC_Z_ZOBJECTALLOCATOR_HPP

#include "gc/z/zAllocationFlags.hpp"
#include "gc/z/zValue.hpp"

class ZPage;
class ZPageTable;

class ZObjectAllocator {
private:
  const bool         _use_per_cpu_shared_small_pages;
  ZPerCPU<size_t>    _used;
  ZPerCPU<size_t>    _undone;
  ZPerCPU<size_t>    _alloc_for_relocation;
  ZPerCPU<size_t>    _undo_alloc_for_relocation;
  ZContended<ZPage*> _shared_medium_page;
  ZPerCPU<ZPage*>    _shared_small_page;

  ZPage** shared_small_page_addr();
  ZPage* const* shared_small_page_addr() const;

  void register_alloc_for_relocation(const ZPageTable* page_table, uintptr_t addr, size_t size);
  void register_undo_alloc_for_relocation(const ZPage* page, size_t size);

  ZPage* alloc_page(uint8_t type, size_t size, ZAllocationFlags flags);
  void undo_alloc_page(ZPage* page);

  // Allocate an object in a shared page. Allocate and
  // atomically install a new page if necessary.
  uintptr_t alloc_object_in_shared_page(ZPage** shared_page,
                                        uint8_t page_type,
                                        size_t page_size,
                                        size_t size,
                                        ZAllocationFlags flags);

  uintptr_t alloc_large_object(size_t size, ZAllocationFlags flags);
  uintptr_t alloc_medium_object(size_t size, ZAllocationFlags flags);
  uintptr_t alloc_small_object(size_t size, ZAllocationFlags flags);
  uintptr_t alloc_object(size_t size, ZAllocationFlags flags);

public:
  ZObjectAllocator();

  uintptr_t alloc_object(size_t size);
  uintptr_t alloc_object_for_relocation(const ZPageTable* page_table, size_t size);
  void undo_alloc_object_for_relocation(ZPage* page, uintptr_t addr, size_t size);

  size_t used() const;
  size_t remaining() const;
  size_t relocated() const;

  void retire_pages();
};

#endif // SHARE_GC_Z_ZOBJECTALLOCATOR_HPP
