/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SERIAL_DEFNEWGENERATION_INLINE_HPP
#define SHARE_GC_SERIAL_DEFNEWGENERATION_INLINE_HPP

#include "gc/serial/defNewGeneration.hpp"

#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/genOopClosures.inline.hpp"
#include "gc/shared/space.inline.hpp"
#include "oops/access.inline.hpp"

// Methods of protected closure types

template <class T>
inline void DefNewGeneration::KeepAliveClosure::do_oop_work(T* p) {
#ifdef ASSERT
  {
    // We never expect to see a null reference being processed
    // as a weak reference.
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
    assert (oopDesc::is_oop(obj), "expected an oop while scanning weak refs");
  }
#endif // ASSERT

  Devirtualizer::do_oop(_cl, p);

  // Card marking is trickier for weak refs.
  // This oop is a 'next' field which was filled in while we
  // were discovering weak references. While we might not need
  // to take a special action to keep this reference alive, we
  // will need to dirty a card as the field was modified.
  //
  // Alternatively, we could create a method which iterates through
  // each generation, allowing them in turn to examine the modified
  // field.
  //
  // We could check that p is also in the old generation, but
  // dirty cards in the young gen are never scanned, so the
  // extra check probably isn't worthwhile.
  if (GenCollectedHeap::heap()->is_in_reserved(p)) {
    _rs->inline_write_ref_field_gc(p);
  }
}

template <class T>
inline void DefNewGeneration::FastKeepAliveClosure::do_oop_work(T* p) {
#ifdef ASSERT
  {
    // We never expect to see a null reference being processed
    // as a weak reference.
    oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
    assert (oopDesc::is_oop(obj), "expected an oop while scanning weak refs");
  }
#endif // ASSERT

  Devirtualizer::do_oop(_cl, p);

  // Optimized for Defnew generation if it's the youngest generation:
  // we set a younger_gen card if we have an older->youngest
  // generation pointer.
  oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
  if ((cast_from_oop<HeapWord*>(obj) < _boundary) && GenCollectedHeap::heap()->is_in_reserved(p)) {
    _rs->inline_write_ref_field_gc(p);
  }
}

template <typename OopClosureType>
void DefNewGeneration::oop_since_save_marks_iterate(OopClosureType* cl) {
  eden()->oop_since_save_marks_iterate(cl);
  to()->oop_since_save_marks_iterate(cl);
  from()->oop_since_save_marks_iterate(cl);

  save_marks();
}

#endif // SHARE_GC_SERIAL_DEFNEWGENERATION_INLINE_HPP
