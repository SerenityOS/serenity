/*
 * Copyright (c) 2013, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_HPP

#include "memory/allocation.hpp"
#include "gc/shenandoah/shenandoahHeap.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahPadding.hpp"
#include "utilities/globalDefinitions.hpp"

class ShenandoahHeapRegionSet;

class ShenandoahHeapRegionSetIterator : public StackObj {
private:
  const ShenandoahHeapRegionSet* _set;
  ShenandoahHeap* const _heap;
  size_t _current_index;

  // No implicit copying: iterators should be passed by reference to capture the state
  NONCOPYABLE(ShenandoahHeapRegionSetIterator);

public:
  ShenandoahHeapRegionSetIterator(const ShenandoahHeapRegionSet* const set);

  // Single-thread version
  ShenandoahHeapRegion* next();
};

class ShenandoahHeapRegionSet : public CHeapObj<mtGC> {
  friend class ShenandoahHeap;
private:
  ShenandoahHeap* const _heap;
  size_t const          _map_size;
  jbyte* const          _set_map;
  size_t                _region_count;

public:
  ShenandoahHeapRegionSet();
  ~ShenandoahHeapRegionSet();

  void add_region(ShenandoahHeapRegion* r);
  void remove_region(ShenandoahHeapRegion* r);

  size_t count()  const { return _region_count; }
  bool is_empty() const { return _region_count == 0; }

  inline bool is_in(ShenandoahHeapRegion* r) const;
  inline bool is_in(size_t region_idx)       const;

  void print_on(outputStream* out) const;

  void clear();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHHEAPREGIONSET_HPP
