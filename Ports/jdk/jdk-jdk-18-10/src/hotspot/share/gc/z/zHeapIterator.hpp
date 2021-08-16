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

#ifndef SHARE_GC_Z_ZHEAPITERATOR_HPP
#define SHARE_GC_Z_ZHEAPITERATOR_HPP

#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/taskqueue.hpp"
#include "gc/z/zGranuleMap.hpp"
#include "gc/z/zLock.hpp"
#include "gc/z/zRootsIterator.hpp"
#include "gc/z/zStat.hpp"

class ZHeapIteratorBitMap;
class ZHeapIteratorContext;

using ZHeapIteratorBitMaps = ZGranuleMap<ZHeapIteratorBitMap*>;
using ZHeapIteratorBitMapsIterator = ZGranuleMapIterator<ZHeapIteratorBitMap*>;
using ZHeapIteratorQueue = OverflowTaskQueue<oop, mtGC>;
using ZHeapIteratorQueues = GenericTaskQueueSet<ZHeapIteratorQueue, mtGC>;
using ZHeapIteratorArrayQueue = OverflowTaskQueue<ObjArrayTask, mtGC>;
using ZHeapIteratorArrayQueues = GenericTaskQueueSet<ZHeapIteratorArrayQueue, mtGC>;

class ZHeapIterator : public ParallelObjectIterator {
  friend class ZHeapIteratorContext;

private:
  const bool               _visit_weaks;
  ZStatTimerDisable        _timer_disable;
  ZHeapIteratorBitMaps     _bitmaps;
  ZLock                    _bitmaps_lock;
  ZHeapIteratorQueues      _queues;
  ZHeapIteratorArrayQueues _array_queues;
  ZRootsIterator           _roots;
  ZWeakRootsIterator       _weak_roots;
  TaskTerminator           _terminator;

  ZHeapIteratorBitMap* object_bitmap(oop obj);

  bool mark_object(oop obj);

  void push_strong_roots(const ZHeapIteratorContext& context);
  void push_weak_roots(const ZHeapIteratorContext& context);

  template <bool VisitWeaks>
  void push_roots(const ZHeapIteratorContext& context);

  template <bool VisitReferents>
  void follow_object(const ZHeapIteratorContext& context, oop obj);

  void follow_array(const ZHeapIteratorContext& context, oop obj);
  void follow_array_chunk(const ZHeapIteratorContext& context, const ObjArrayTask& array);

  template <bool VisitWeaks>
  void visit_and_follow(const ZHeapIteratorContext& context, ObjectClosure* cl, oop obj);

  template <bool VisitWeaks>
  void drain(const ZHeapIteratorContext& context, ObjectClosure* cl);

  template <bool VisitWeaks>
  void steal(const ZHeapIteratorContext& context, ObjectClosure* cl);

  template <bool VisitWeaks>
  void drain_and_steal(const ZHeapIteratorContext& context, ObjectClosure* cl);

  template <bool VisitWeaks>
  void object_iterate_inner(const ZHeapIteratorContext& context, ObjectClosure* cl);

public:
  ZHeapIterator(uint nworkers, bool visit_weaks);
  virtual ~ZHeapIterator();

  virtual void object_iterate(ObjectClosure* cl, uint worker_id);
};

#endif // SHARE_GC_Z_ZHEAPITERATOR_HPP
