/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FULLGCCOMPACTIONPOINT_HPP
#define SHARE_GC_G1_G1FULLGCCOMPACTIONPOINT_HPP

#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/growableArray.hpp"

class HeapRegion;

class G1FullGCCompactionPoint : public CHeapObj<mtGC> {
  HeapRegion* _current_region;
  HeapWord*   _threshold;
  HeapWord*   _compaction_top;
  GrowableArray<HeapRegion*>* _compaction_regions;
  GrowableArrayIterator<HeapRegion*> _compaction_region_iterator;

  bool object_will_fit(size_t size);
  void initialize_values(bool init_threshold);
  void switch_region();
  HeapRegion* next_region();

public:
  G1FullGCCompactionPoint();
  ~G1FullGCCompactionPoint();

  bool has_regions();
  bool is_initialized();
  void initialize(HeapRegion* hr, bool init_threshold);
  void update();
  void forward(oop object, size_t size);
  void add(HeapRegion* hr);

  HeapRegion* remove_last();
  HeapRegion* current_region();

  GrowableArray<HeapRegion*>* regions();
};

#endif // SHARE_GC_G1_G1FULLGCCOMPACTIONPOINT_HPP
