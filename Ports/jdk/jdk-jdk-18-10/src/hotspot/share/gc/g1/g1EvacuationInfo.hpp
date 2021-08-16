/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#ifndef SHARE_GC_G1_G1EVACUATIONINFO_HPP
#define SHARE_GC_G1_G1EVACUATIONINFO_HPP

#include "memory/allocation.hpp"

class G1EvacuationInfo : public StackObj {
  uint _collectionset_regions;
  uint _allocation_regions;
  size_t _collectionset_used_before;
  size_t _collectionset_used_after;
  size_t _alloc_regions_used_before;
  size_t _bytes_used;
  uint   _regions_freed;

public:
  G1EvacuationInfo() :
    _collectionset_regions(0), _allocation_regions(0), _collectionset_used_before(0),
    _collectionset_used_after(0), _alloc_regions_used_before(0),
    _bytes_used(0), _regions_freed(0) { }

  void set_collectionset_regions(uint collectionset_regions) {
    _collectionset_regions = collectionset_regions;
  }

  void set_allocation_regions(uint allocation_regions) {
    _allocation_regions = allocation_regions;
  }

  void set_collectionset_used_before(size_t used) {
    _collectionset_used_before = used;
  }

  void increment_collectionset_used_after(size_t used) {
    _collectionset_used_after += used;
  }

  void set_alloc_regions_used_before(size_t used) {
    _alloc_regions_used_before = used;
  }

  void set_bytes_used(size_t used) {
    _bytes_used = used;
  }

  void set_regions_freed(uint freed) {
    _regions_freed += freed;
  }

  uint   collectionset_regions()     { return _collectionset_regions; }
  uint   allocation_regions()        { return _allocation_regions; }
  size_t collectionset_used_before() { return _collectionset_used_before; }
  size_t collectionset_used_after()  { return _collectionset_used_after; }
  size_t alloc_regions_used_before() { return _alloc_regions_used_before; }
  size_t bytes_used()                { return _bytes_used; }
  uint   regions_freed()             { return _regions_freed; }
};

#endif // SHARE_GC_G1_G1EVACUATIONINFO_HPP
