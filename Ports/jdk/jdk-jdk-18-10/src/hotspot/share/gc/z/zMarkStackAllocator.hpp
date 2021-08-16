/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZMARKSTACKALLOCATOR_HPP
#define SHARE_GC_Z_ZMARKSTACKALLOCATOR_HPP

#include "gc/z/zGlobals.hpp"
#include "gc/z/zLock.hpp"
#include "utilities/globalDefinitions.hpp"

class ZMarkStackSpace {
private:
  ZLock              _expand_lock;
  uintptr_t          _start;
  volatile uintptr_t _top;
  volatile uintptr_t _end;

  size_t used() const;

  size_t expand_space();
  size_t shrink_space();

  uintptr_t alloc_space(size_t size);
  uintptr_t expand_and_alloc_space(size_t size);

public:
  ZMarkStackSpace();

  bool is_initialized() const;

  size_t size() const;

  uintptr_t alloc(size_t size);
  void free();
};

class ZMarkStackAllocator {
private:
  ZCACHE_ALIGNED ZMarkStackMagazineList _freelist;
  ZCACHE_ALIGNED ZMarkStackSpace        _space;

  ZMarkStackMagazine* create_magazine_from_space(uintptr_t addr, size_t size);

public:
  ZMarkStackAllocator();

  bool is_initialized() const;

  size_t size() const;

  ZMarkStackMagazine* alloc_magazine();
  void free_magazine(ZMarkStackMagazine* magazine);

  void free();
};

#endif // SHARE_GC_Z_ZMARKSTACKALLOCATOR_HPP
