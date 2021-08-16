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

#ifndef SHARE_GC_Z_ZPAGECACHE_HPP
#define SHARE_GC_Z_ZPAGECACHE_HPP

#include "gc/z/zList.hpp"
#include "gc/z/zPage.hpp"
#include "gc/z/zValue.hpp"

class ZPageCacheFlushClosure;

class ZPageCache {
private:
  ZPerNUMA<ZList<ZPage> > _small;
  ZList<ZPage>            _medium;
  ZList<ZPage>            _large;
  uint64_t                _last_commit;

  ZPage* alloc_small_page();
  ZPage* alloc_medium_page();
  ZPage* alloc_large_page(size_t size);

  ZPage* alloc_oversized_medium_page(size_t size);
  ZPage* alloc_oversized_large_page(size_t size);
  ZPage* alloc_oversized_page(size_t size);

  bool flush_list_inner(ZPageCacheFlushClosure* cl, ZList<ZPage>* from, ZList<ZPage>* to);
  void flush_list(ZPageCacheFlushClosure* cl, ZList<ZPage>* from, ZList<ZPage>* to);
  void flush_per_numa_lists(ZPageCacheFlushClosure* cl, ZPerNUMA<ZList<ZPage> >* from, ZList<ZPage>* to);
  void flush(ZPageCacheFlushClosure* cl, ZList<ZPage>* to);

public:
  ZPageCache();

  ZPage* alloc_page(uint8_t type, size_t size);
  void free_page(ZPage* page);

  void flush_for_allocation(size_t requested, ZList<ZPage>* to);
  size_t flush_for_uncommit(size_t requested, ZList<ZPage>* to, uint64_t* timeout);

  void set_last_commit();

  void pages_do(ZPageClosure* cl) const;
};

#endif // SHARE_GC_Z_ZPAGECACHE_HPP
