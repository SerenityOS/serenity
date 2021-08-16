/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1ConcurrentMark.inline.hpp"
#include "gc/g1/g1ConcurrentMarkObjArrayProcessor.inline.hpp"

void G1CMObjArrayProcessor::push_array_slice(HeapWord* what) {
  _task->push(G1TaskQueueEntry::from_slice(what));
}

size_t G1CMObjArrayProcessor::process_array_slice(objArrayOop obj, HeapWord* start_from, size_t remaining) {
  size_t words_to_scan = MIN2(remaining, (size_t)ObjArrayMarkingStride);

  if (remaining > ObjArrayMarkingStride) {
    push_array_slice(start_from + ObjArrayMarkingStride);
  }

  // Then process current area.
  MemRegion mr(start_from, words_to_scan);
  return _task->scan_objArray(obj, mr);
}

size_t G1CMObjArrayProcessor::process_obj(oop obj) {
  assert(should_be_sliced(obj), "Must be an array object %d and large " SIZE_FORMAT, obj->is_objArray(), (size_t)obj->size());

  return process_array_slice(objArrayOop(obj), cast_from_oop<HeapWord*>(obj), (size_t)objArrayOop(obj)->size());
}

size_t G1CMObjArrayProcessor::process_slice(HeapWord* slice) {

  // Find the start address of the objArrayOop.
  // Shortcut the BOT access if the given address is from a humongous object. The BOT
  // slide is fast enough for "smaller" objects in non-humongous regions, but is slower
  // than directly using heap region table.
  G1CollectedHeap* g1h = G1CollectedHeap::heap();
  HeapRegion* r = g1h->heap_region_containing(slice);

  HeapWord* const start_address = r->is_humongous() ?
                                  r->humongous_start_region()->bottom() :
                                  g1h->block_start(slice);

  assert(cast_to_oop(start_address)->is_objArray(), "Address " PTR_FORMAT " does not refer to an object array ", p2i(start_address));
  assert(start_address < slice,
         "Object start address " PTR_FORMAT " must be smaller than decoded address " PTR_FORMAT,
         p2i(start_address),
         p2i(slice));

  objArrayOop objArray = objArrayOop(cast_to_oop(start_address));

  size_t already_scanned = slice - start_address;
  size_t remaining = objArray->size() - already_scanned;

  return process_array_slice(objArray, slice, remaining);
}
