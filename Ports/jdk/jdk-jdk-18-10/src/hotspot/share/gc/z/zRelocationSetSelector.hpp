/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZRELOCATIONSETSELECTOR_HPP
#define SHARE_GC_Z_ZRELOCATIONSETSELECTOR_HPP

#include "gc/z/zArray.hpp"
#include "memory/allocation.hpp"

class ZPage;

class ZRelocationSetSelectorGroupStats {
  friend class ZRelocationSetSelectorGroup;

private:
  size_t _npages;
  size_t _total;
  size_t _live;
  size_t _empty;
  size_t _relocate;

public:
  ZRelocationSetSelectorGroupStats();

  size_t npages() const;
  size_t total() const;
  size_t live() const;
  size_t empty() const;
  size_t relocate() const;
};

class ZRelocationSetSelectorStats {
  friend class ZRelocationSetSelector;

private:
  ZRelocationSetSelectorGroupStats _small;
  ZRelocationSetSelectorGroupStats _medium;
  ZRelocationSetSelectorGroupStats _large;

public:
  const ZRelocationSetSelectorGroupStats& small() const;
  const ZRelocationSetSelectorGroupStats& medium() const;
  const ZRelocationSetSelectorGroupStats& large() const;
};

class ZRelocationSetSelectorGroup {
private:
  const char* const                _name;
  const uint8_t                    _page_type;
  const size_t                     _page_size;
  const size_t                     _object_size_limit;
  const size_t                     _fragmentation_limit;
  ZArray<ZPage*>                   _live_pages;
  size_t                           _forwarding_entries;
  ZRelocationSetSelectorGroupStats _stats;

  bool is_disabled();
  bool is_selectable();
  void semi_sort();
  void select_inner();

public:
  ZRelocationSetSelectorGroup(const char* name,
                              uint8_t page_type,
                              size_t page_size,
                              size_t object_size_limit);

  void register_live_page(ZPage* page);
  void register_empty_page(ZPage* page);
  void select();

  const ZArray<ZPage*>* selected() const;
  size_t forwarding_entries() const;

  const ZRelocationSetSelectorGroupStats& stats() const;
};

class ZRelocationSetSelector : public StackObj {
private:
  ZRelocationSetSelectorGroup _small;
  ZRelocationSetSelectorGroup _medium;
  ZRelocationSetSelectorGroup _large;
  ZArray<ZPage*>              _empty_pages;

  size_t total() const;
  size_t empty() const;
  size_t relocate() const;

public:
  ZRelocationSetSelector();

  void register_live_page(ZPage* page);
  void register_empty_page(ZPage* page);

  bool should_free_empty_pages(int bulk) const;
  const ZArray<ZPage*>* empty_pages() const;
  void clear_empty_pages();

  void select();

  const ZArray<ZPage*>* small() const;
  const ZArray<ZPage*>* medium() const;
  size_t forwarding_entries() const;

  ZRelocationSetSelectorStats stats() const;
};

#endif // SHARE_GC_Z_ZRELOCATIONSETSELECTOR_HPP
