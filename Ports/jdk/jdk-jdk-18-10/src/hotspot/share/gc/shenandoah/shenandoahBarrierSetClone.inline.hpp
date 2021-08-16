/*
 * Copyright (c) 2013, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSETCLONE_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSETCLONE_INLINE_HPP

// No shenandoahBarrierSetClone.hpp

#include "gc/shenandoah/shenandoahBarrierSet.inline.hpp"
#include "gc/shenandoah/shenandoahCollectionSet.inline.hpp"
#include "gc/shenandoah/shenandoahEvacOOMHandler.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/access.hpp"
#include "oops/compressedOops.hpp"

template <bool HAS_FWD, bool EVAC, bool ENQUEUE>
class ShenandoahUpdateRefsForOopClosure: public BasicOopIterateClosure {
private:
  ShenandoahHeap* const _heap;
  ShenandoahBarrierSet* const _bs;
  const ShenandoahCollectionSet* const _cset;
  Thread* const _thread;

  template <class T>
  inline void do_oop_work(T* p) {
    T o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o)) {
      oop obj = CompressedOops::decode_not_null(o);
      if (HAS_FWD && _cset->is_in(obj)) {
        oop fwd = _bs->resolve_forwarded_not_null(obj);
        if (EVAC && obj == fwd) {
          fwd = _heap->evacuate_object(obj, _thread);
        }
        assert(obj != fwd || _heap->cancelled_gc(), "must be forwarded");
        ShenandoahHeap::atomic_update_oop(fwd, p, o);
        obj = fwd;
      }
      if (ENQUEUE) {
        _bs->enqueue(obj);
      }
    }
  }
public:
  ShenandoahUpdateRefsForOopClosure() :
          _heap(ShenandoahHeap::heap()),
          _bs(ShenandoahBarrierSet::barrier_set()),
          _cset(_heap->collection_set()),
          _thread(Thread::current()) {
  }

  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
};

void ShenandoahBarrierSet::clone_marking(oop obj) {
  assert(_heap->is_concurrent_mark_in_progress(), "only during marking");
  assert(ShenandoahIUBarrier, "only with incremental-update");
  if (!_heap->marking_context()->allocated_after_mark_start(obj)) {
    ShenandoahUpdateRefsForOopClosure</* has_fwd = */ false, /* evac = */ false, /* enqueue */ true> cl;
    obj->oop_iterate(&cl);
  }
}

void ShenandoahBarrierSet::clone_evacuation(oop obj) {
  assert(_heap->is_evacuation_in_progress(), "only during evacuation");
  if (need_bulk_update(cast_from_oop<HeapWord*>(obj))) {
    ShenandoahEvacOOMScope oom_evac_scope;
    ShenandoahUpdateRefsForOopClosure</* has_fwd = */ true, /* evac = */ true, /* enqueue */ false> cl;
    obj->oop_iterate(&cl);
  }
}

void ShenandoahBarrierSet::clone_update(oop obj) {
  assert(_heap->is_update_refs_in_progress(), "only during update-refs");
  if (need_bulk_update(cast_from_oop<HeapWord*>(obj))) {
    ShenandoahUpdateRefsForOopClosure</* has_fwd = */ true, /* evac = */ false, /* enqueue */ false> cl;
    obj->oop_iterate(&cl);
  }
}

void ShenandoahBarrierSet::clone_barrier(oop obj) {
  assert(ShenandoahCloneBarrier, "only get here with clone barriers enabled");
  shenandoah_assert_correct(NULL, obj);

  int gc_state = _heap->gc_state();
  if ((gc_state & ShenandoahHeap::MARKING) != 0) {
    clone_marking(obj);
  } else if ((gc_state & ShenandoahHeap::EVACUATION) != 0) {
    clone_evacuation(obj);
  } else {
    clone_update(obj);
  }
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHBARRIERSETCLONE_INLINE_HPP
