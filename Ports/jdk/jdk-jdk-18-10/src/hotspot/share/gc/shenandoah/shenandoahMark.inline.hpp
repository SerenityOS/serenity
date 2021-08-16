/*
 * Copyright (c) 2015, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHMARK_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHMARK_INLINE_HPP

#include "gc/shenandoah/shenandoahMark.hpp"

#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahBarrierSet.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.inline.hpp"
#include "gc/shenandoah/shenandoahStringDedup.inline.hpp"
#include "gc/shenandoah/shenandoahTaskqueue.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "memory/iterator.inline.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/prefetch.inline.hpp"
#include "utilities/powerOfTwo.hpp"

template <class T>
void ShenandoahMark::do_task(ShenandoahObjToScanQueue* q, T* cl, ShenandoahLiveData* live_data, ShenandoahMarkTask* task) {
  oop obj = task->obj();

  shenandoah_assert_not_forwarded(NULL, obj);
  shenandoah_assert_marked(NULL, obj);
  shenandoah_assert_not_in_cset_except(NULL, obj, ShenandoahHeap::heap()->cancelled_gc());

  // Are we in weak subgraph scan?
  bool weak = task->is_weak();
  cl->set_weak(weak);

  if (task->is_not_chunked()) {
    if (obj->is_instance()) {
      // Case 1: Normal oop, process as usual.
      obj->oop_iterate(cl);
    } else if (obj->is_objArray()) {
      // Case 2: Object array instance and no chunk is set. Must be the first
      // time we visit it, start the chunked processing.
      do_chunked_array_start<T>(q, cl, obj, weak);
    } else {
      // Case 3: Primitive array. Do nothing, no oops there. We use the same
      // performance tweak TypeArrayKlass::oop_oop_iterate_impl is using:
      // We skip iterating over the klass pointer since we know that
      // Universe::TypeArrayKlass never moves.
      assert (obj->is_typeArray(), "should be type array");
    }
    // Count liveness the last: push the outstanding work to the queues first
    // Avoid double-counting objects that are visited twice due to upgrade
    // from final- to strong mark.
    if (task->count_liveness()) {
      count_liveness(live_data, obj);
    }
  } else {
    // Case 4: Array chunk, has sensible chunk id. Process it.
    do_chunked_array<T>(q, cl, obj, task->chunk(), task->pow(), weak);
  }
}

inline void ShenandoahMark::count_liveness(ShenandoahLiveData* live_data, oop obj) {
  ShenandoahHeap* const heap = ShenandoahHeap::heap();
  size_t region_idx = heap->heap_region_index_containing(obj);
  ShenandoahHeapRegion* region = heap->get_region(region_idx);
  size_t size = obj->size();

  if (!region->is_humongous_start()) {
    assert(!region->is_humongous(), "Cannot have continuations here");
    ShenandoahLiveData cur = live_data[region_idx];
    size_t new_val = size + cur;
    if (new_val >= SHENANDOAH_LIVEDATA_MAX) {
      // overflow, flush to region data
      region->increase_live_data_gc_words(new_val);
      live_data[region_idx] = 0;
    } else {
      // still good, remember in locals
      live_data[region_idx] = (ShenandoahLiveData) new_val;
    }
  } else {
    shenandoah_assert_in_correct_region(NULL, obj);
    size_t num_regions = ShenandoahHeapRegion::required_regions(size * HeapWordSize);

    for (size_t i = region_idx; i < region_idx + num_regions; i++) {
      ShenandoahHeapRegion* chain_reg = heap->get_region(i);
      assert(chain_reg->is_humongous(), "Expecting a humongous region");
      chain_reg->increase_live_data_gc_words(chain_reg->used() >> LogHeapWordSize);
    }
  }
}

template <class T>
inline void ShenandoahMark::do_chunked_array_start(ShenandoahObjToScanQueue* q, T* cl, oop obj, bool weak) {
  assert(obj->is_objArray(), "expect object array");
  objArrayOop array = objArrayOop(obj);
  int len = array->length();

  // Mark objArray klass metadata
  if (Devirtualizer::do_metadata(cl)) {
    Devirtualizer::do_klass(cl, array->klass());
  }

  if (len <= (int) ObjArrayMarkingStride*2) {
    // A few slices only, process directly
    array->oop_iterate_range(cl, 0, len);
  } else {
    int bits = log2i_graceful(len);
    // Compensate for non-power-of-two arrays, cover the array in excess:
    if (len != (1 << bits)) bits++;

    // Only allow full chunks on the queue. This frees do_chunked_array() from checking from/to
    // boundaries against array->length(), touching the array header on every chunk.
    //
    // To do this, we cut the prefix in full-sized chunks, and submit them on the queue.
    // If the array is not divided in chunk sizes, then there would be an irregular tail,
    // which we will process separately.

    int last_idx = 0;

    int chunk = 1;
    int pow = bits;

    // Handle overflow
    if (pow >= 31) {
      assert (pow == 31, "sanity");
      pow--;
      chunk = 2;
      last_idx = (1 << pow);
      bool pushed = q->push(ShenandoahMarkTask(array, true, weak, 1, pow));
      assert(pushed, "overflow queue should always succeed pushing");
    }

    // Split out tasks, as suggested in ShenandoahMarkTask docs. Record the last
    // successful right boundary to figure out the irregular tail.
    while ((1 << pow) > (int)ObjArrayMarkingStride &&
           (chunk*2 < ShenandoahMarkTask::chunk_size())) {
      pow--;
      int left_chunk = chunk*2 - 1;
      int right_chunk = chunk*2;
      int left_chunk_end = left_chunk * (1 << pow);
      if (left_chunk_end < len) {
        bool pushed = q->push(ShenandoahMarkTask(array, true, weak, left_chunk, pow));
        assert(pushed, "overflow queue should always succeed pushing");
        chunk = right_chunk;
        last_idx = left_chunk_end;
      } else {
        chunk = left_chunk;
      }
    }

    // Process the irregular tail, if present
    int from = last_idx;
    if (from < len) {
      array->oop_iterate_range(cl, from, len);
    }
  }
}

template <class T>
inline void ShenandoahMark::do_chunked_array(ShenandoahObjToScanQueue* q, T* cl, oop obj, int chunk, int pow, bool weak) {
  assert(obj->is_objArray(), "expect object array");
  objArrayOop array = objArrayOop(obj);

  assert (ObjArrayMarkingStride > 0, "sanity");

  // Split out tasks, as suggested in ShenandoahMarkTask docs. Avoid pushing tasks that
  // are known to start beyond the array.
  while ((1 << pow) > (int)ObjArrayMarkingStride && (chunk*2 < ShenandoahMarkTask::chunk_size())) {
    pow--;
    chunk *= 2;
    bool pushed = q->push(ShenandoahMarkTask(array, true, weak, chunk - 1, pow));
    assert(pushed, "overflow queue should always succeed pushing");
  }

  int chunk_size = 1 << pow;

  int from = (chunk - 1) * chunk_size;
  int to = chunk * chunk_size;

#ifdef ASSERT
  int len = array->length();
  assert (0 <= from && from < len, "from is sane: %d/%d", from, len);
  assert (0 < to && to <= len, "to is sane: %d/%d", to, len);
#endif

  array->oop_iterate_range(cl, from, to);
}

class ShenandoahSATBBufferClosure : public SATBBufferClosure {
private:
  StringDedup::Requests     _stringdedup_requests;
  ShenandoahObjToScanQueue* _queue;
  ShenandoahHeap* _heap;
  ShenandoahMarkingContext* const _mark_context;
public:
  ShenandoahSATBBufferClosure(ShenandoahObjToScanQueue* q) :
    _queue(q),
    _heap(ShenandoahHeap::heap()),
    _mark_context(_heap->marking_context())
  {
  }

  void do_buffer(void **buffer, size_t size) {
    assert(size == 0 || !_heap->has_forwarded_objects(), "Forwarded objects are not expected here");
    if (ShenandoahStringDedup::is_enabled()) {
      do_buffer_impl<ENQUEUE_DEDUP>(buffer, size);
    } else {
      do_buffer_impl<NO_DEDUP>(buffer, size);
    }
  }

  template<StringDedupMode STRING_DEDUP>
  void do_buffer_impl(void **buffer, size_t size) {
    for (size_t i = 0; i < size; ++i) {
      oop *p = (oop *) &buffer[i];
      ShenandoahMark::mark_through_ref<oop, STRING_DEDUP>(p, _queue, _mark_context, &_stringdedup_requests, false);
    }
  }
};

template<class T, StringDedupMode STRING_DEDUP>
inline void ShenandoahMark::mark_through_ref(T* p, ShenandoahObjToScanQueue* q, ShenandoahMarkingContext* const mark_context, StringDedup::Requests* const req, bool weak) {
  T o = RawAccess<>::oop_load(p);
  if (!CompressedOops::is_null(o)) {
    oop obj = CompressedOops::decode_not_null(o);

    shenandoah_assert_not_forwarded(p, obj);
    shenandoah_assert_not_in_cset_except(p, obj, ShenandoahHeap::heap()->cancelled_gc());

    bool skip_live = false;
    bool marked;
    if (weak) {
      marked = mark_context->mark_weak(obj);
    } else {
      marked = mark_context->mark_strong(obj, /* was_upgraded = */ skip_live);
    }
    if (marked) {
      bool pushed = q->push(ShenandoahMarkTask(obj, skip_live, weak));
      assert(pushed, "overflow queue should always succeed pushing");

      if ((STRING_DEDUP == ENQUEUE_DEDUP) && ShenandoahStringDedup::is_candidate(obj)) {
        assert(ShenandoahStringDedup::is_enabled(), "Must be enabled");
        req->add(obj);
      } else if ((STRING_DEDUP == ALWAYS_DEDUP) && ShenandoahStringDedup::is_string_candidate(obj)) {
        assert(ShenandoahStringDedup::is_enabled(), "Must be enabled");
        req->add(obj);
      }
    }

    shenandoah_assert_marked(p, obj);
  }
}

ShenandoahObjToScanQueueSet* ShenandoahMark::task_queues() const {
  return _task_queues;
}

ShenandoahObjToScanQueue* ShenandoahMark::get_queue(uint index) const {
  return _task_queues->queue(index);
}
#endif // SHARE_GC_SHENANDOAH_SHENANDOAHMARK_INLINE_HPP
