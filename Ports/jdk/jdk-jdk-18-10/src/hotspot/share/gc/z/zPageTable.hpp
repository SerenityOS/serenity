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

#ifndef SHARE_GC_Z_ZPAGETABLE_HPP
#define SHARE_GC_Z_ZPAGETABLE_HPP

#include "gc/z/zGranuleMap.hpp"
#include "memory/allocation.hpp"

class ZPage;

class ZPageTable {
  friend class VMStructs;
  friend class ZPageTableIterator;

private:
  ZGranuleMap<ZPage*> _map;

public:
  ZPageTable();

  ZPage* get(uintptr_t addr) const;

  void insert(ZPage* page);
  void remove(ZPage* page);
};

class ZPageTableIterator : public StackObj {
private:
  ZGranuleMapIterator<ZPage*> _iter;
  ZPage*                      _prev;

public:
  ZPageTableIterator(const ZPageTable* page_table);

  bool next(ZPage** page);
};

#endif // SHARE_GC_Z_ZPAGETABLE_HPP
