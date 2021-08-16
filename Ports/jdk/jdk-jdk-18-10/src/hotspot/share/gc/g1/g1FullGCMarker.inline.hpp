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

#ifndef SHARE_GC_G1_G1FULLGCMARKER_INLINE_HPP
#define SHARE_GC_G1_G1FULLGCMARKER_INLINE_HPP

#include "gc/g1/g1FullGCMarker.hpp"

#include "classfile/classLoaderData.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "gc/g1/g1Allocator.inline.hpp"
#include "gc/g1/g1ConcurrentMarkBitMap.inline.hpp"
#include "gc/g1/g1FullCollector.inline.hpp"
#include "gc/g1/g1FullGCOopClosures.inline.hpp"
#include "gc/g1/g1RegionMarkStatsCache.hpp"
#include "gc/g1/g1StringDedup.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/debug.hpp"

inline bool G1FullGCMarker::mark_object(oop obj) {
  if (_collector->is_skip_marking(obj)) {
    return false;
  }

  // Try to mark.
  if (!_bitmap->par_mark(obj)) {
    // Lost mark race.
    return false;
  }

  // Marked by us, preserve if needed.
  markWord mark = obj->mark();
  if (obj->mark_must_be_preserved(mark) &&
      // It is not necessary to preserve marks for objects in regions we do not
      // compact because we do not change their headers (i.e. forward them).
      _collector->is_compacting(obj)) {
    preserved_stack()->push(obj, mark);
  }

  // Check if deduplicatable string.
  if (StringDedup::is_enabled() &&
      java_lang_String::is_instance_inlined(obj) &&
      G1StringDedup::is_candidate_from_mark(obj)) {
    _string_dedup_requests.add(obj);
  }

  // Collect live words.
  _mark_stats_cache.add_live_words(obj);

  return true;
}

template <class T> inline void G1FullGCMarker::mark_and_push(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    if (mark_object(obj)) {
      _oop_stack.push(obj);
      assert(_bitmap->is_marked(obj), "Must be marked now - map self");
    } else {
      assert(_bitmap->is_marked(obj) || _collector->is_skip_marking(obj),
             "Must be marked by other or object in skip marking region");
    }
  }
}

inline bool G1FullGCMarker::is_empty() {
  return _oop_stack.is_empty() && _objarray_stack.is_empty();
}

inline bool G1FullGCMarker::pop_object(oop& oop) {
  return _oop_stack.pop_overflow(oop) || _oop_stack.pop_local(oop);
}

inline void G1FullGCMarker::push_objarray(oop obj, size_t index) {
  ObjArrayTask task(obj, index);
  assert(task.is_valid(), "bad ObjArrayTask");
  _objarray_stack.push(task);
}

inline bool G1FullGCMarker::pop_objarray(ObjArrayTask& arr) {
  return _objarray_stack.pop_overflow(arr) || _objarray_stack.pop_local(arr);
}

inline void G1FullGCMarker::follow_array(objArrayOop array) {
  follow_klass(array->klass());
  // Don't push empty arrays to avoid unnecessary work.
  if (array->length() > 0) {
    push_objarray(array, 0);
  }
}

void G1FullGCMarker::follow_array_chunk(objArrayOop array, int index) {
  const int len = array->length();
  const int beg_index = index;
  assert(beg_index < len || len == 0, "index too large");

  const int stride = MIN2(len - beg_index, (int) ObjArrayMarkingStride);
  const int end_index = beg_index + stride;

  // Push the continuation first to allow more efficient work stealing.
  if (end_index < len) {
    push_objarray(array, end_index);
  }

  array->oop_iterate_range(mark_closure(), beg_index, end_index);

  if (VerifyDuringGC) {
    _verify_closure.set_containing_obj(array);
    array->oop_iterate_range(&_verify_closure, beg_index, end_index);
    if (_verify_closure.failures()) {
      assert(false, "Failed");
    }
  }
}

inline void G1FullGCMarker::follow_object(oop obj) {
  assert(_bitmap->is_marked(obj), "should be marked");
  if (obj->is_objArray()) {
    // Handle object arrays explicitly to allow them to
    // be split into chunks if needed.
    follow_array((objArrayOop)obj);
  } else {
    obj->oop_iterate(mark_closure());
    if (VerifyDuringGC) {
      if (obj->is_instance() && InstanceKlass::cast(obj->klass())->is_reference_instance_klass()) {
        return;
      }
      _verify_closure.set_containing_obj(obj);
      obj->oop_iterate(&_verify_closure);
      if (_verify_closure.failures()) {
        log_warning(gc, verify)("Failed after %d", _verify_closure._cc);
        assert(false, "Failed");
      }
    }
  }
}

void G1FullGCMarker::drain_stack() {
  do {
    oop obj;
    while (pop_object(obj)) {
      assert(_bitmap->is_marked(obj), "must be marked");
      follow_object(obj);
    }
    // Process ObjArrays one at a time to avoid marking stack bloat.
    ObjArrayTask task;
    if (pop_objarray(task)) {
      follow_array_chunk(objArrayOop(task.obj()), task.index());
    }
  } while (!is_empty());
}

inline void G1FullGCMarker::follow_klass(Klass* k) {
  oop op = k->class_loader_data()->holder_no_keepalive();
  mark_and_push(&op);
}

inline void G1FullGCMarker::follow_cld(ClassLoaderData* cld) {
  _cld_closure.do_cld(cld);
}

#endif // SHARE_GC_G1_G1FULLGCMARKER_INLINE_HPP
