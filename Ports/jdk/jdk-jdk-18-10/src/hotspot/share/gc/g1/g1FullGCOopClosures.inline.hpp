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

#ifndef SHARE_GC_G1_G1FULLGCOOPCLOSURES_INLINE_HPP
#define SHARE_GC_G1_G1FULLGCOOPCLOSURES_INLINE_HPP

#include "gc/g1/g1FullGCOopClosures.hpp"

#include "gc/g1/g1Allocator.inline.hpp"
#include "gc/g1/g1FullCollector.inline.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/g1FullGCMarker.inline.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"

template <typename T>
inline void G1MarkAndPushClosure::do_oop_work(T* p) {
  _marker->mark_and_push(p);
}

inline void G1MarkAndPushClosure::do_oop(oop* p) {
  do_oop_work(p);
}

inline void G1MarkAndPushClosure::do_oop(narrowOop* p) {
  do_oop_work(p);
}

inline bool G1MarkAndPushClosure::do_metadata() {
  return true;
}

inline void G1MarkAndPushClosure::do_klass(Klass* k) {
  _marker->follow_klass(k);
}

inline void G1MarkAndPushClosure::do_cld(ClassLoaderData* cld) {
  _marker->follow_cld(cld);
}

template <class T> inline void G1AdjustClosure::adjust_pointer(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (CompressedOops::is_null(heap_oop)) {
    return;
  }

  oop obj = CompressedOops::decode_not_null(heap_oop);
  assert(Universe::heap()->is_in(obj), "should be in heap");
  if (!_collector->is_compacting(obj)) {
    // We never forward objects in non-compacting regions so there is no need to
    // process them further.
    return;
  }

  oop forwardee = obj->forwardee();
  if (forwardee == NULL) {
    // Not forwarded, return current reference.
    assert(obj->mark() == markWord::prototype() || // Correct mark
           obj->mark_must_be_preserved(), // Will be restored by PreservedMarksSet
           "Must have correct prototype or be preserved, obj: " PTR_FORMAT ", mark: " PTR_FORMAT ", prototype: " PTR_FORMAT,
           p2i(obj), obj->mark().value(), markWord::prototype().value());
    return;
  }

  // Forwarded, just update.
  assert(G1CollectedHeap::heap()->is_in_reserved(forwardee), "should be in object space");
  RawAccess<IS_NOT_NULL>::oop_store(p, forwardee);
}

inline void G1AdjustClosure::do_oop(oop* p)       { do_oop_work(p); }
inline void G1AdjustClosure::do_oop(narrowOop* p) { do_oop_work(p); }

inline bool G1IsAliveClosure::do_object_b(oop p) {
  return _bitmap->is_marked(p) || _collector->is_skip_marking(p);
}

template<typename T>
inline void G1FullKeepAliveClosure::do_oop_work(T* p) {
  _marker->mark_and_push(p);
}

#endif // SHARE_GC_G1_G1FULLGCOOPCLOSURES_INLINE_HPP
