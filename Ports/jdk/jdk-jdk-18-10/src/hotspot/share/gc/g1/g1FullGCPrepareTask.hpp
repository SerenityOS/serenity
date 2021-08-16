/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1FULLGCPREPARETASK_HPP
#define SHARE_GC_G1_G1FULLGCPREPARETASK_HPP

#include "gc/g1/g1FullGCCompactionPoint.hpp"
#include "gc/g1/g1FullGCScope.hpp"
#include "gc/g1/g1FullGCTask.hpp"
#include "gc/g1/g1RootProcessor.hpp"
#include "gc/g1/heapRegionManager.hpp"
#include "gc/shared/referenceProcessor.hpp"

class G1CMBitMap;
class G1FullCollector;

class G1FullGCPrepareTask : public G1FullGCTask {
protected:
  volatile bool     _freed_regions;
  HeapRegionClaimer _hrclaimer;

  void set_freed_regions();

public:
  G1FullGCPrepareTask(G1FullCollector* collector);
  void work(uint worker_id);
  void prepare_serial_compaction();
  bool has_freed_regions();

protected:
  class G1CalculatePointersClosure : public HeapRegionClosure {
  private:
    template<bool is_humongous>
    void free_pinned_region(HeapRegion* hr);
  protected:
    G1CollectedHeap* _g1h;
    G1FullCollector* _collector;
    G1CMBitMap* _bitmap;
    G1FullGCCompactionPoint* _cp;
    bool _regions_freed;

    bool should_compact(HeapRegion* hr);
    void prepare_for_compaction(HeapRegion* hr);
    void prepare_for_compaction_work(G1FullGCCompactionPoint* cp, HeapRegion* hr);

    void reset_region_metadata(HeapRegion* hr);

  public:
    G1CalculatePointersClosure(G1FullCollector* collector,
                               G1FullGCCompactionPoint* cp);

    bool do_heap_region(HeapRegion* hr);
    bool freed_regions();
  };

  class G1PrepareCompactLiveClosure : public StackObj {
    G1FullGCCompactionPoint* _cp;

  public:
    G1PrepareCompactLiveClosure(G1FullGCCompactionPoint* cp);
    size_t apply(oop object);
  };

  class G1RePrepareClosure : public StackObj {
    G1FullGCCompactionPoint* _cp;
    HeapRegion* _current;

  public:
    G1RePrepareClosure(G1FullGCCompactionPoint* hrcp,
                       HeapRegion* hr) :
        _cp(hrcp),
        _current(hr) { }

    size_t apply(oop object);
  };
};

#endif // SHARE_GC_G1_G1FULLGCPREPARETASK_HPP
