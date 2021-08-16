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

#include "precompiled.hpp"
#include "classfile/classLoaderData.hpp"
#include "gc/g1/g1FullGCMarker.inline.hpp"
#include "gc/shared/referenceProcessor.hpp"
#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/verifyOption.hpp"
#include "memory/iterator.inline.hpp"

G1FullGCMarker::G1FullGCMarker(G1FullCollector* collector,
                               uint worker_id,
                               PreservedMarks* preserved_stack,
                               G1RegionMarkStats* mark_stats) :
    _collector(collector),
    _worker_id(worker_id),
    _bitmap(collector->mark_bitmap()),
    _oop_stack(),
    _objarray_stack(),
    _preserved_stack(preserved_stack),
    _mark_closure(worker_id, this, G1CollectedHeap::heap()->ref_processor_stw()),
    _verify_closure(VerifyOption_G1UseFullMarking),
    _stack_closure(this),
    _cld_closure(mark_closure(), ClassLoaderData::_claim_strong),
    _mark_stats_cache(mark_stats, G1RegionMarkStatsCache::RegionMarkStatsCacheSize) {
  _mark_stats_cache.reset();
  _oop_stack.initialize();
  _objarray_stack.initialize();
}

G1FullGCMarker::~G1FullGCMarker() {
  assert(is_empty(), "Must be empty at this point");
}

void G1FullGCMarker::complete_marking(OopQueueSet* oop_stacks,
                                      ObjArrayTaskQueueSet* array_stacks,
                                      TaskTerminator* terminator) {
  do {
    drain_stack();
    ObjArrayTask steal_array;
    if (array_stacks->steal(_worker_id, steal_array)) {
      follow_array_chunk(objArrayOop(steal_array.obj()), steal_array.index());
    } else {
      oop steal_oop;
      if (oop_stacks->steal(_worker_id, steal_oop)) {
        follow_object(steal_oop);
      }
    }
  } while (!is_empty() || !terminator->offer_termination());
}

void G1FullGCMarker::flush_mark_stats_cache() {
  _mark_stats_cache.evict_all();
}
