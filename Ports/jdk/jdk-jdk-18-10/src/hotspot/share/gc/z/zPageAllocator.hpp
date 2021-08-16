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

#ifndef SHARE_GC_Z_ZPAGEALLOCATOR_HPP
#define SHARE_GC_Z_ZPAGEALLOCATOR_HPP

#include "gc/z/zAllocationFlags.hpp"
#include "gc/z/zArray.hpp"
#include "gc/z/zList.hpp"
#include "gc/z/zLock.hpp"
#include "gc/z/zPageCache.hpp"
#include "gc/z/zPhysicalMemory.hpp"
#include "gc/z/zSafeDelete.hpp"
#include "gc/z/zVirtualMemory.hpp"

class ThreadClosure;
class ZPageAllocation;
class ZPageAllocatorStats;
class ZWorkers;
class ZUncommitter;
class ZUnmapper;

class ZPageAllocator {
  friend class VMStructs;
  friend class ZUnmapper;
  friend class ZUncommitter;

private:
  mutable ZLock              _lock;
  ZPageCache                 _cache;
  ZVirtualMemoryManager      _virtual;
  ZPhysicalMemoryManager     _physical;
  const size_t               _min_capacity;
  const size_t               _max_capacity;
  volatile size_t            _current_max_capacity;
  volatile size_t            _capacity;
  volatile size_t            _claimed;
  volatile size_t            _used;
  size_t                     _used_high;
  size_t                     _used_low;
  ssize_t                    _reclaimed;
  ZList<ZPageAllocation>     _stalled;
  volatile uint64_t          _nstalled;
  ZList<ZPageAllocation>     _satisfied;
  ZUnmapper*                 _unmapper;
  ZUncommitter*              _uncommitter;
  mutable ZSafeDelete<ZPage> _safe_delete;
  bool                       _initialized;

  bool prime_cache(ZWorkers* workers, size_t size);

  size_t increase_capacity(size_t size);
  void decrease_capacity(size_t size, bool set_max_capacity);

  void increase_used(size_t size, bool relocation);
  void decrease_used(size_t size, bool reclaimed);

  bool commit_page(ZPage* page);
  void uncommit_page(ZPage* page);

  void map_page(const ZPage* page) const;
  void unmap_page(const ZPage* page) const;

  void destroy_page(ZPage* page);

  bool is_alloc_allowed(size_t size) const;

  bool alloc_page_common_inner(uint8_t type, size_t size, ZList<ZPage>* pages);
  bool alloc_page_common(ZPageAllocation* allocation);
  bool alloc_page_stall(ZPageAllocation* allocation);
  bool alloc_page_or_stall(ZPageAllocation* allocation);
  ZPage* alloc_page_create(ZPageAllocation* allocation);
  ZPage* alloc_page_finalize(ZPageAllocation* allocation);
  void alloc_page_failed(ZPageAllocation* allocation);

  void satisfy_stalled();

  void free_page_inner(ZPage* page, bool reclaimed);

  size_t uncommit(uint64_t* timeout);

public:
  ZPageAllocator(ZWorkers* workers,
                 size_t min_capacity,
                 size_t initial_capacity,
                 size_t max_capacity);

  bool is_initialized() const;

  size_t min_capacity() const;
  size_t max_capacity() const;
  size_t soft_max_capacity() const;
  size_t capacity() const;
  size_t used() const;
  size_t unused() const;

  ZPageAllocatorStats stats() const;

  void reset_statistics();

  ZPage* alloc_page(uint8_t type, size_t size, ZAllocationFlags flags);
  void free_page(ZPage* page, bool reclaimed);
  void free_pages(const ZArray<ZPage*>* pages, bool reclaimed);

  void enable_deferred_delete() const;
  void disable_deferred_delete() const;

  void debug_map_page(const ZPage* page) const;
  void debug_unmap_page(const ZPage* page) const;

  bool has_alloc_stalled() const;
  void check_out_of_memory();

  void pages_do(ZPageClosure* cl) const;

  void threads_do(ThreadClosure* tc) const;
};

class ZPageAllocatorStats {
private:
  size_t _min_capacity;
  size_t _max_capacity;
  size_t _soft_max_capacity;
  size_t _current_max_capacity;
  size_t _capacity;
  size_t _used;
  size_t _used_high;
  size_t _used_low;
  size_t _reclaimed;

public:
  ZPageAllocatorStats(size_t min_capacity,
                      size_t max_capacity,
                      size_t soft_max_capacity,
                      size_t capacity,
                      size_t used,
                      size_t used_high,
                      size_t used_low,
                      size_t reclaimed);

  size_t min_capacity() const;
  size_t max_capacity() const;
  size_t soft_max_capacity() const;
  size_t capacity() const;
  size_t used() const;
  size_t used_high() const;
  size_t used_low() const;
  size_t reclaimed() const;
};

#endif // SHARE_GC_Z_ZPAGEALLOCATOR_HPP
