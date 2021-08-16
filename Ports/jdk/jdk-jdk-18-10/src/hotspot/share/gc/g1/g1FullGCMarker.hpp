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

#ifndef SHARE_GC_G1_G1FULLGCMARKER_HPP
#define SHARE_GC_G1_G1FULLGCMARKER_HPP

#include "gc/g1/g1FullGCOopClosures.hpp"
#include "gc/g1/g1RegionMarkStatsCache.hpp"
#include "gc/shared/preservedMarks.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/taskqueue.hpp"
#include "memory/iterator.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.hpp"
#include "runtime/timer.hpp"
#include "utilities/chunkedList.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/stack.hpp"

typedef OverflowTaskQueue<oop, mtGC>                 OopQueue;
typedef OverflowTaskQueue<ObjArrayTask, mtGC>        ObjArrayTaskQueue;

typedef GenericTaskQueueSet<OopQueue, mtGC>          OopQueueSet;
typedef GenericTaskQueueSet<ObjArrayTaskQueue, mtGC> ObjArrayTaskQueueSet;

class G1CMBitMap;
class G1FullCollector;
class TaskTerminator;

class G1FullGCMarker : public CHeapObj<mtGC> {
  G1FullCollector*   _collector;

  uint               _worker_id;
  // Backing mark bitmap
  G1CMBitMap*        _bitmap;

  // Mark stack
  OopQueue           _oop_stack;
  ObjArrayTaskQueue  _objarray_stack;
  PreservedMarks*    _preserved_stack;

  // Marking closures
  G1MarkAndPushClosure  _mark_closure;
  G1VerifyOopClosure    _verify_closure;
  G1FollowStackClosure  _stack_closure;
  CLDToOopClosure       _cld_closure;
  StringDedup::Requests _string_dedup_requests;


  G1RegionMarkStatsCache _mark_stats_cache;

  inline bool is_empty();
  inline bool pop_object(oop& obj);
  inline bool pop_objarray(ObjArrayTask& array);
  inline void push_objarray(oop obj, size_t index);
  inline bool mark_object(oop obj);

  // Marking helpers
  inline void follow_object(oop obj);
  inline void follow_array(objArrayOop array);
  inline void follow_array_chunk(objArrayOop array, int index);
public:
  G1FullGCMarker(G1FullCollector* collector,
                 uint worker_id,
                 PreservedMarks* preserved_stack,
                 G1RegionMarkStats* mark_stats);
  ~G1FullGCMarker();

  // Stack getters
  OopQueue*          oop_stack()       { return &_oop_stack; }
  ObjArrayTaskQueue* objarray_stack()  { return &_objarray_stack; }
  PreservedMarks*    preserved_stack() { return _preserved_stack; }

  // Marking entry points
  template <class T> inline void mark_and_push(T* p);
  inline void follow_klass(Klass* k);
  inline void follow_cld(ClassLoaderData* cld);

  inline void drain_stack();
  void complete_marking(OopQueueSet* oop_stacks,
                        ObjArrayTaskQueueSet* array_stacks,
                        TaskTerminator* terminator);

  // Closure getters
  CLDToOopClosure*      cld_closure()   { return &_cld_closure; }
  G1MarkAndPushClosure* mark_closure()  { return &_mark_closure; }
  G1FollowStackClosure* stack_closure() { return &_stack_closure; }

  // Flush live bytes to regions
  void flush_mark_stats_cache();
};

#endif // SHARE_GC_G1_G1FULLGCMARKER_HPP
