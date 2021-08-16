/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1CollectorState.hpp"
#include "gc/g1/g1ConcurrentMark.inline.hpp"
#include "gc/g1/g1EvacFailure.hpp"
#include "gc/g1/g1HeapVerifier.hpp"
#include "gc/g1/g1OopClosures.inline.hpp"
#include "gc/g1/g1RedirtyCardsQueue.hpp"
#include "gc/g1/heapRegion.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"

class UpdateLogBuffersDeferred : public BasicOopIterateClosure {
private:
  G1CollectedHeap* _g1h;
  G1RedirtyCardsLocalQueueSet* _rdc_local_qset;
  G1CardTable*    _ct;

  // Remember the last enqueued card to avoid enqueuing the same card over and over;
  // since we only ever handle a card once, this is sufficient.
  size_t _last_enqueued_card;

public:
  UpdateLogBuffersDeferred(G1RedirtyCardsLocalQueueSet* rdc_local_qset) :
    _g1h(G1CollectedHeap::heap()),
    _rdc_local_qset(rdc_local_qset),
    _ct(_g1h->card_table()),
    _last_enqueued_card(SIZE_MAX) {}

  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(      oop* p) { do_oop_work(p); }
  template <class T> void do_oop_work(T* p) {
    assert(_g1h->heap_region_containing(p)->is_in_reserved(p), "paranoia");
    assert(!_g1h->heap_region_containing(p)->is_survivor(), "Unexpected evac failure in survivor region");

    T const o = RawAccess<>::oop_load(p);
    if (CompressedOops::is_null(o)) {
      return;
    }

    if (HeapRegion::is_in_same_region(p, CompressedOops::decode(o))) {
      return;
    }
    size_t card_index = _ct->index_for(p);
    if (card_index != _last_enqueued_card) {
      _rdc_local_qset->enqueue(_ct->byte_for_index(card_index));
      _last_enqueued_card = card_index;
    }
  }
};

class RemoveSelfForwardPtrObjClosure: public ObjectClosure {
  G1CollectedHeap* _g1h;
  G1ConcurrentMark* _cm;
  HeapRegion* _hr;
  const bool _is_young;
  size_t _marked_bytes;
  UpdateLogBuffersDeferred* _log_buffer_cl;
  bool _during_concurrent_start;
  uint _worker_id;
  HeapWord* _last_forwarded_object_end;

public:
  RemoveSelfForwardPtrObjClosure(HeapRegion* hr,
                                 UpdateLogBuffersDeferred* log_buffer_cl,
                                 bool during_concurrent_start,
                                 uint worker_id) :
    _g1h(G1CollectedHeap::heap()),
    _cm(_g1h->concurrent_mark()),
    _hr(hr),
    _is_young(hr->is_young()),
    _marked_bytes(0),
    _log_buffer_cl(log_buffer_cl),
    _during_concurrent_start(during_concurrent_start),
    _worker_id(worker_id),
    _last_forwarded_object_end(hr->bottom()) { }

  size_t marked_bytes() { return _marked_bytes; }

  // Iterate over the live objects in the region to find self-forwarded objects
  // that need to be kept live. We need to update the remembered sets of these
  // objects. Further update the BOT and marks.
  // We can coalesce and overwrite the remaining heap contents with dummy objects
  // as they have either been dead or evacuated (which are unreferenced now, i.e.
  // dead too) already.
  void do_object(oop obj) {
    HeapWord* obj_addr = cast_from_oop<HeapWord*>(obj);
    assert(_hr->is_in(obj_addr), "sanity");

    if (obj->is_forwarded() && obj->forwardee() == obj) {
      // The object failed to move.

      zap_dead_objects(_last_forwarded_object_end, obj_addr);
      // We consider all objects that we find self-forwarded to be
      // live. What we'll do is that we'll update the prev marking
      // info so that they are all under PTAMS and explicitly marked.
      if (!_cm->is_marked_in_prev_bitmap(obj)) {
        _cm->mark_in_prev_bitmap(obj);
      }
      if (_during_concurrent_start) {
        // For the next marking info we'll only mark the
        // self-forwarded objects explicitly if we are during
        // concurrent start (since, normally, we only mark objects pointed
        // to by roots if we succeed in copying them). By marking all
        // self-forwarded objects we ensure that we mark any that are
        // still pointed to be roots. During concurrent marking, and
        // after concurrent start, we don't need to mark any objects
        // explicitly and all objects in the CSet are considered
        // (implicitly) live. So, we won't mark them explicitly and
        // we'll leave them over NTAMS.
        _cm->mark_in_next_bitmap(_worker_id, _hr, obj);
      }
      size_t obj_size = obj->size();

      _marked_bytes += (obj_size * HeapWordSize);
      PreservedMarks::init_forwarded_mark(obj);

      // During evacuation failure we do not record inter-region
      // references referencing regions that need a remembered set
      // update originating from young regions (including eden) that
      // failed evacuation. Make up for that omission now by rescanning
      // these failed objects.
      if (_is_young) {
        obj->oop_iterate(_log_buffer_cl);
      }

      HeapWord* obj_end = obj_addr + obj_size;
      _last_forwarded_object_end = obj_end;
      _hr->cross_threshold(obj_addr, obj_end);
    }
  }

  // Fill the memory area from start to end with filler objects, and update the BOT
  // and the mark bitmap accordingly.
  void zap_dead_objects(HeapWord* start, HeapWord* end) {
    if (start == end) {
      return;
    }

    size_t gap_size = pointer_delta(end, start);
    MemRegion mr(start, gap_size);
    if (gap_size >= CollectedHeap::min_fill_size()) {
      CollectedHeap::fill_with_objects(start, gap_size);

      HeapWord* end_first_obj = start + cast_to_oop(start)->size();
      _hr->cross_threshold(start, end_first_obj);
      // Fill_with_objects() may have created multiple (i.e. two)
      // objects, as the max_fill_size() is half a region.
      // After updating the BOT for the first object, also update the
      // BOT for the second object to make the BOT complete.
      if (end_first_obj != end) {
        _hr->cross_threshold(end_first_obj, end);
#ifdef ASSERT
        size_t size_second_obj = cast_to_oop(end_first_obj)->size();
        HeapWord* end_of_second_obj = end_first_obj + size_second_obj;
        assert(end == end_of_second_obj,
               "More than two objects were used to fill the area from " PTR_FORMAT " to " PTR_FORMAT ", "
               "second objects size " SIZE_FORMAT " ends at " PTR_FORMAT,
               p2i(start), p2i(end), size_second_obj, p2i(end_of_second_obj));
#endif
      }
    }
    _cm->clear_range_in_prev_bitmap(mr);
  }

  void zap_remainder() {
    zap_dead_objects(_last_forwarded_object_end, _hr->top());
  }
};

class RemoveSelfForwardPtrHRClosure: public HeapRegionClosure {
  G1CollectedHeap* _g1h;
  uint _worker_id;

  G1RedirtyCardsLocalQueueSet _rdc_local_qset;
  UpdateLogBuffersDeferred _log_buffer_cl;

  uint volatile* _num_failed_regions;

public:
  RemoveSelfForwardPtrHRClosure(G1RedirtyCardsQueueSet* rdcqs, uint worker_id, uint volatile* num_failed_regions) :
    _g1h(G1CollectedHeap::heap()),
    _worker_id(worker_id),
    _rdc_local_qset(rdcqs),
    _log_buffer_cl(&_rdc_local_qset),
    _num_failed_regions(num_failed_regions) {
  }

  ~RemoveSelfForwardPtrHRClosure() {
    _rdc_local_qset.flush();
  }

  size_t remove_self_forward_ptr_by_walking_hr(HeapRegion* hr,
                                               bool during_concurrent_start) {
    RemoveSelfForwardPtrObjClosure rspc(hr,
                                        &_log_buffer_cl,
                                        during_concurrent_start,
                                        _worker_id);
    hr->object_iterate(&rspc);
    // Need to zap the remainder area of the processed region.
    rspc.zap_remainder();

    return rspc.marked_bytes();
  }

  bool do_heap_region(HeapRegion *hr) {
    assert(!hr->is_pinned(), "Unexpected pinned region at index %u", hr->hrm_index());
    assert(hr->in_collection_set(), "bad CS");

    if (_g1h->evacuation_failed(hr->hrm_index())) {
      hr->clear_index_in_opt_cset();

      bool during_concurrent_start = _g1h->collector_state()->in_concurrent_start_gc();
      bool during_concurrent_mark = _g1h->collector_state()->mark_or_rebuild_in_progress();

      hr->note_self_forwarding_removal_start(during_concurrent_start,
                                             during_concurrent_mark);
      _g1h->verifier()->check_bitmaps("Self-Forwarding Ptr Removal", hr);

      hr->reset_bot();

      size_t live_bytes = remove_self_forward_ptr_by_walking_hr(hr, during_concurrent_start);

      hr->rem_set()->clean_strong_code_roots(hr);
      hr->rem_set()->clear_locked(true);

      hr->note_self_forwarding_removal_end(live_bytes);

      Atomic::inc(_num_failed_regions, memory_order_relaxed);
    }
    return false;
  }
};

G1ParRemoveSelfForwardPtrsTask::G1ParRemoveSelfForwardPtrsTask(G1RedirtyCardsQueueSet* rdcqs) :
  AbstractGangTask("G1 Remove Self-forwarding Pointers"),
  _g1h(G1CollectedHeap::heap()),
  _rdcqs(rdcqs),
  _hrclaimer(_g1h->workers()->active_workers()),
  _num_failed_regions(0) { }

void G1ParRemoveSelfForwardPtrsTask::work(uint worker_id) {
  RemoveSelfForwardPtrHRClosure rsfp_cl(_rdcqs, worker_id, &_num_failed_regions);

  // We need to check all collection set regions whether they need self forward
  // removals, not only the last collection set increment. The reason is that
  // reference processing (e.g. finalizers) can make it necessary to resurrect an
  // otherwise unreachable object at the very end of the collection. That object
  // might cause an evacuation failure in any region in the collection set.
  _g1h->collection_set_par_iterate_all(&rsfp_cl, &_hrclaimer, worker_id);
}

uint G1ParRemoveSelfForwardPtrsTask::num_failed_regions() const {
  return Atomic::load(&_num_failed_regions);
}
