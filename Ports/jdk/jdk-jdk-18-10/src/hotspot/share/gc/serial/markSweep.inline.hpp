/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SERIAL_MARKSWEEP_INLINE_HPP
#define SHARE_GC_SERIAL_MARKSWEEP_INLINE_HPP

#include "gc/serial/markSweep.hpp"

#include "classfile/classLoaderData.inline.hpp"
#include "memory/universe.hpp"
#include "oops/markWord.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/stack.inline.hpp"

inline void MarkSweep::mark_object(oop obj) {
  // some marks may contain information we need to preserve so we store them away
  // and overwrite the mark.  We'll restore it at the end of markSweep.
  markWord mark = obj->mark();
  obj->set_mark(markWord::prototype().set_marked());

  if (obj->mark_must_be_preserved(mark)) {
    preserve_mark(obj, mark);
  }
}

template <class T> inline void MarkSweep::mark_and_push(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    if (!obj->mark().is_marked()) {
      mark_object(obj);
      _marking_stack.push(obj);
    }
  }
}

inline void MarkSweep::follow_klass(Klass* klass) {
  oop op = klass->class_loader_data()->holder_no_keepalive();
  MarkSweep::mark_and_push(&op);
}

inline void MarkSweep::follow_cld(ClassLoaderData* cld) {
  MarkSweep::follow_cld_closure.do_cld(cld);
}

template <typename T>
inline void MarkAndPushClosure::do_oop_work(T* p)            { MarkSweep::mark_and_push(p); }
inline void MarkAndPushClosure::do_oop(oop* p)               { do_oop_work(p); }
inline void MarkAndPushClosure::do_oop(narrowOop* p)         { do_oop_work(p); }
inline void MarkAndPushClosure::do_klass(Klass* k)           { MarkSweep::follow_klass(k); }
inline void MarkAndPushClosure::do_cld(ClassLoaderData* cld) { MarkSweep::follow_cld(cld); }

template <class T> inline void MarkSweep::adjust_pointer(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    assert(Universe::heap()->is_in(obj), "should be in heap");

    oop new_obj = cast_to_oop(obj->mark().decode_pointer());

    assert(new_obj != NULL ||                      // is forwarding ptr?
           obj->mark() == markWord::prototype(), // not gc marked?
           "should be forwarded");

    if (new_obj != NULL) {
      assert(is_object_aligned(new_obj), "oop must be aligned");
      RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);
    }
  }
}

template <typename T>
void AdjustPointerClosure::do_oop_work(T* p)           { MarkSweep::adjust_pointer(p); }
inline void AdjustPointerClosure::do_oop(oop* p)       { do_oop_work(p); }
inline void AdjustPointerClosure::do_oop(narrowOop* p) { do_oop_work(p); }


inline int MarkSweep::adjust_pointers(oop obj) {
  return obj->oop_iterate_size(&MarkSweep::adjust_pointer_closure);
}

#endif // SHARE_GC_SERIAL_MARKSWEEP_INLINE_HPP
