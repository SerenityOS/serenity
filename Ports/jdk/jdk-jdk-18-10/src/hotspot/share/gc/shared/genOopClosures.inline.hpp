/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GENOOPCLOSURES_INLINE_HPP
#define SHARE_GC_SHARED_GENOOPCLOSURES_INLINE_HPP

#include "gc/shared/genOopClosures.hpp"

#include "classfile/classLoaderData.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/generation.hpp"
#include "gc/shared/space.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#if INCLUDE_SERIALGC
#include "gc/serial/defNewGeneration.inline.hpp"
#endif

#if INCLUDE_SERIALGC

template <typename Derived>
inline FastScanClosure<Derived>::FastScanClosure(DefNewGeneration* g) :
    BasicOopIterateClosure(g->ref_processor()),
    _young_gen(g),
    _young_gen_end(g->reserved().end()) {}

template <typename Derived>
template <typename T>
inline void FastScanClosure<Derived>::do_oop_work(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  // Should we copy the obj?
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    if (cast_from_oop<HeapWord*>(obj) < _young_gen_end) {
      assert(!_young_gen->to()->is_in_reserved(obj), "Scanning field twice?");
      oop new_obj = obj->is_forwarded() ? obj->forwardee()
                                        : _young_gen->copy_to_survivor_space(obj);
      RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);

      static_cast<Derived*>(this)->barrier(p);
    }
  }
}

template <typename Derived>
inline void FastScanClosure<Derived>::do_oop(oop* p)       { do_oop_work(p); }
template <typename Derived>
inline void FastScanClosure<Derived>::do_oop(narrowOop* p) { do_oop_work(p); }

inline DefNewYoungerGenClosure::DefNewYoungerGenClosure(DefNewGeneration* young_gen, Generation* old_gen) :
    FastScanClosure<DefNewYoungerGenClosure>(young_gen),
    _old_gen(old_gen),
    _old_gen_start(old_gen->reserved().start()),
    _rs(GenCollectedHeap::heap()->rem_set()) {}

template <typename T>
void DefNewYoungerGenClosure::barrier(T* p) {
  assert(_old_gen->is_in_reserved(p), "expected ref in generation");
  T heap_oop = RawAccess<>::oop_load(p);
  assert(!CompressedOops::is_null(heap_oop), "expected non-null oop");
  oop obj = CompressedOops::decode_not_null(heap_oop);
  // If p points to a younger generation, mark the card.
  if (cast_from_oop<HeapWord*>(obj) < _old_gen_start) {
    _rs->inline_write_ref_field_gc(p);
  }
}

inline DefNewScanClosure::DefNewScanClosure(DefNewGeneration* g) :
    FastScanClosure<DefNewScanClosure>(g), _scanned_cld(NULL) {}

template <class T>
void DefNewScanClosure::barrier(T* p) {
  if (_scanned_cld != NULL && !_scanned_cld->has_modified_oops()) {
    _scanned_cld->record_modified_oops();
  }
}

#endif // INCLUDE_SERIALGC

template <class T> void FilteringClosure::do_oop_work(T* p) {
  T heap_oop = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(heap_oop)) {
    oop obj = CompressedOops::decode_not_null(heap_oop);
    if (cast_from_oop<HeapWord*>(obj) < _boundary) {
      _cl->do_oop(p);
    }
  }
}

inline void FilteringClosure::do_oop(oop* p)       { FilteringClosure::do_oop_work(p); }
inline void FilteringClosure::do_oop(narrowOop* p) { FilteringClosure::do_oop_work(p); }

#if INCLUDE_SERIALGC

// Note similarity to FastScanClosure; the difference is that
// the barrier set is taken care of outside this closure.
template <class T> inline void ScanWeakRefClosure::do_oop_work(T* p) {
  oop obj = RawAccess<IS_NOT_NULL>::oop_load(p);
  // weak references are sometimes scanned twice; must check
  // that to-space doesn't already contain this object
  if (cast_from_oop<HeapWord*>(obj) < _boundary && !_g->to()->is_in_reserved(obj)) {
    oop new_obj = obj->is_forwarded() ? obj->forwardee()
                                      : _g->copy_to_survivor_space(obj);
    RawAccess<IS_NOT_NULL>::oop_store(p, new_obj);
  }
}

inline void ScanWeakRefClosure::do_oop(oop* p)       { ScanWeakRefClosure::do_oop_work(p); }
inline void ScanWeakRefClosure::do_oop(narrowOop* p) { ScanWeakRefClosure::do_oop_work(p); }

#endif // INCLUDE_SERIALGC

#endif // SHARE_GC_SHARED_GENOOPCLOSURES_INLINE_HPP
