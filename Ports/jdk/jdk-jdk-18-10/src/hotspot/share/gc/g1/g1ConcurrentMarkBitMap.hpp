/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CONCURRENTMARKBITMAP_HPP
#define SHARE_GC_G1_G1CONCURRENTMARKBITMAP_HPP

#include "gc/g1/g1RegionToSpaceMapper.hpp"
#include "gc/shared/markBitMap.hpp"
#include "memory/memRegion.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/bitMap.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class G1CMBitMap;
class G1CMTask;
class G1ConcurrentMark;
class HeapRegion;

// Closure for iteration over bitmaps
class G1CMBitMapClosure {
  G1ConcurrentMark* const _cm;
  G1CMTask* const _task;
public:
  G1CMBitMapClosure(G1CMTask *task, G1ConcurrentMark* cm) : _cm(cm), _task(task) { }

  bool do_addr(HeapWord* const addr);
};

class G1CMBitMapMappingChangedListener : public G1MappingChangedListener {
  G1CMBitMap* _bm;
public:
  G1CMBitMapMappingChangedListener() : _bm(NULL) {}

  void set_bitmap(G1CMBitMap* bm) { _bm = bm; }

  virtual void on_commit(uint start_idx, size_t num_regions, bool zero_filled);
};

// A generic mark bitmap for concurrent marking.  This is essentially a wrapper
// around the BitMap class that is based on HeapWords, with one bit per (1 << _shifter) HeapWords.
class G1CMBitMap : public MarkBitMap {

  G1CMBitMapMappingChangedListener _listener;

protected:

  virtual void check_mark(HeapWord* addr) NOT_DEBUG_RETURN;

public:

  G1CMBitMap() : MarkBitMap(), _listener() { _listener.set_bitmap(this); }

  // Initializes the underlying BitMap to cover the given area.
  void initialize(MemRegion heap, G1RegionToSpaceMapper* storage);

  // Apply the closure to the addresses that correspond to marked bits in the bitmap.
  inline bool iterate(G1CMBitMapClosure* cl, MemRegion mr);

  void clear_region(HeapRegion* hr);
};

#endif // SHARE_GC_G1_G1CONCURRENTMARKBITMAP_HPP
