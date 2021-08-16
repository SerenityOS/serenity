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

#ifndef SHARE_GC_Z_ZPHYSICALMEMORY_HPP
#define SHARE_GC_Z_ZPHYSICALMEMORY_HPP

#include "gc/z/zArray.hpp"
#include "gc/z/zMemory.hpp"
#include "memory/allocation.hpp"
#include OS_HEADER(gc/z/zPhysicalMemoryBacking)

class ZPhysicalMemorySegment : public CHeapObj<mtGC> {
private:
  uintptr_t _start;
  uintptr_t _end;
  bool      _committed;

public:
  ZPhysicalMemorySegment();
  ZPhysicalMemorySegment(uintptr_t start, size_t size, bool committed);

  uintptr_t start() const;
  uintptr_t end() const;
  size_t size() const;

  bool is_committed() const;
  void set_committed(bool committed);
};

class ZPhysicalMemory {
private:
  ZArray<ZPhysicalMemorySegment> _segments;

  void insert_segment(int index, uintptr_t start, size_t size, bool committed);
  void replace_segment(int index, uintptr_t start, size_t size, bool committed);
  void remove_segment(int index);

public:
  ZPhysicalMemory();
  ZPhysicalMemory(const ZPhysicalMemorySegment& segment);
  ZPhysicalMemory(const ZPhysicalMemory& pmem);
  const ZPhysicalMemory& operator=(const ZPhysicalMemory& pmem);

  bool is_null() const;
  size_t size() const;

  int nsegments() const;
  const ZPhysicalMemorySegment& segment(int index) const;

  void add_segments(const ZPhysicalMemory& pmem);
  void remove_segments();

  void add_segment(const ZPhysicalMemorySegment& segment);
  bool commit_segment(int index, size_t size);
  bool uncommit_segment(int index, size_t size);

  ZPhysicalMemory split(size_t size);
  ZPhysicalMemory split_committed();
};

class ZPhysicalMemoryManager {
private:
  ZPhysicalMemoryBacking _backing;
  ZMemoryManager         _manager;

  void nmt_commit(uintptr_t offset, size_t size) const;
  void nmt_uncommit(uintptr_t offset, size_t size) const;

  void pretouch_view(uintptr_t addr, size_t size) const;
  void map_view(uintptr_t addr, const ZPhysicalMemory& pmem) const;
  void unmap_view(uintptr_t addr, size_t size) const;

public:
  ZPhysicalMemoryManager(size_t max_capacity);

  bool is_initialized() const;

  void warn_commit_limits(size_t max_capacity) const;
  void try_enable_uncommit(size_t min_capacity, size_t max_capacity);

  void alloc(ZPhysicalMemory& pmem, size_t size);
  void free(const ZPhysicalMemory& pmem);

  bool commit(ZPhysicalMemory& pmem);
  bool uncommit(ZPhysicalMemory& pmem);

  void pretouch(uintptr_t offset, size_t size) const;

  void map(uintptr_t offset, const ZPhysicalMemory& pmem) const;
  void unmap(uintptr_t offset, size_t size) const;

  void debug_map(uintptr_t offset, const ZPhysicalMemory& pmem) const;
  void debug_unmap(uintptr_t offset, size_t size) const;
};

#endif // SHARE_GC_Z_ZPHYSICALMEMORY_HPP
