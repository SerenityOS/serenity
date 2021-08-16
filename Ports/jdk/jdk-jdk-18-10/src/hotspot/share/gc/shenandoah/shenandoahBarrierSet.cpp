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

#include "precompiled.hpp"
#include "gc/shenandoah/shenandoahBarrierSet.hpp"
#include "gc/shenandoah/shenandoahBarrierSetClone.inline.hpp"
#include "gc/shenandoah/shenandoahBarrierSetAssembler.hpp"
#include "gc/shenandoah/shenandoahBarrierSetNMethod.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahStackWatermark.hpp"
#ifdef COMPILER1
#include "gc/shenandoah/c1/shenandoahBarrierSetC1.hpp"
#endif
#ifdef COMPILER2
#include "gc/shenandoah/c2/shenandoahBarrierSetC2.hpp"
#endif

class ShenandoahBarrierSetC1;
class ShenandoahBarrierSetC2;

ShenandoahBarrierSet::ShenandoahBarrierSet(ShenandoahHeap* heap) :
  BarrierSet(make_barrier_set_assembler<ShenandoahBarrierSetAssembler>(),
             make_barrier_set_c1<ShenandoahBarrierSetC1>(),
             make_barrier_set_c2<ShenandoahBarrierSetC2>(),
             ShenandoahNMethodBarrier ? new ShenandoahBarrierSetNMethod(heap) : NULL,
             BarrierSet::FakeRtti(BarrierSet::ShenandoahBarrierSet)),
  _heap(heap),
  _satb_mark_queue_buffer_allocator("SATB Buffer Allocator", ShenandoahSATBBufferSize),
  _satb_mark_queue_set(&_satb_mark_queue_buffer_allocator)
{
}

ShenandoahBarrierSetAssembler* ShenandoahBarrierSet::assembler() {
  BarrierSetAssembler* const bsa = BarrierSet::barrier_set()->barrier_set_assembler();
  return reinterpret_cast<ShenandoahBarrierSetAssembler*>(bsa);
}

void ShenandoahBarrierSet::print_on(outputStream* st) const {
  st->print("ShenandoahBarrierSet");
}

bool ShenandoahBarrierSet::need_load_reference_barrier(DecoratorSet decorators, BasicType type) {
  if (!ShenandoahLoadRefBarrier) return false;
  // Only needed for references
  return is_reference_type(type);
}

bool ShenandoahBarrierSet::need_keep_alive_barrier(DecoratorSet decorators, BasicType type) {
  if (!ShenandoahSATBBarrier) return false;
  // Only needed for references
  if (!is_reference_type(type)) return false;

  bool keep_alive = (decorators & AS_NO_KEEPALIVE) == 0;
  bool unknown = (decorators & ON_UNKNOWN_OOP_REF) != 0;
  bool on_weak_ref = (decorators & (ON_WEAK_OOP_REF | ON_PHANTOM_OOP_REF)) != 0;
  return (on_weak_ref || unknown) && keep_alive;
}

void ShenandoahBarrierSet::on_thread_create(Thread* thread) {
  // Create thread local data
  ShenandoahThreadLocalData::create(thread);
}

void ShenandoahBarrierSet::on_thread_destroy(Thread* thread) {
  // Destroy thread local data
  ShenandoahThreadLocalData::destroy(thread);
}

void ShenandoahBarrierSet::on_thread_attach(Thread *thread) {
  assert(!thread->is_Java_thread() || !SafepointSynchronize::is_at_safepoint(),
         "We should not be at a safepoint");
  SATBMarkQueue& queue = ShenandoahThreadLocalData::satb_mark_queue(thread);
  assert(!queue.is_active(), "SATB queue should not be active");
  assert(queue.buffer() == nullptr, "SATB queue should not have a buffer");
  assert(queue.index() == 0, "SATB queue index should be zero");
  queue.set_active(_satb_mark_queue_set.is_active());
  if (thread->is_Java_thread()) {
    ShenandoahThreadLocalData::set_gc_state(thread, _heap->gc_state());
    ShenandoahThreadLocalData::initialize_gclab(thread);
    ShenandoahThreadLocalData::set_disarmed_value(thread, ShenandoahCodeRoots::disarmed_value());

    if (ShenandoahStackWatermarkBarrier) {
      JavaThread* const jt = JavaThread::cast(thread);
      StackWatermark* const watermark = new ShenandoahStackWatermark(jt);
      StackWatermarkSet::add_watermark(jt, watermark);
    }
  }
}

void ShenandoahBarrierSet::on_thread_detach(Thread *thread) {
  SATBMarkQueue& queue = ShenandoahThreadLocalData::satb_mark_queue(thread);
  _satb_mark_queue_set.flush_queue(queue);
  if (thread->is_Java_thread()) {
    PLAB* gclab = ShenandoahThreadLocalData::gclab(thread);
    if (gclab != NULL) {
      gclab->retire();
    }

    // SATB protocol requires to keep alive reacheable oops from roots at the beginning of GC
    if (ShenandoahStackWatermarkBarrier) {
      if (_heap->is_concurrent_mark_in_progress()) {
        ShenandoahKeepAliveClosure oops;
        StackWatermarkSet::finish_processing(JavaThread::cast(thread), &oops, StackWatermarkKind::gc);
      } else if (_heap->is_concurrent_weak_root_in_progress() && _heap->is_evacuation_in_progress()) {
        ShenandoahContextEvacuateUpdateRootsClosure oops;
        StackWatermarkSet::finish_processing(JavaThread::cast(thread), &oops, StackWatermarkKind::gc);
      }
    }
  }
}

void ShenandoahBarrierSet::clone_barrier_runtime(oop src) {
  if (_heap->has_forwarded_objects() || (ShenandoahIUBarrier && _heap->is_concurrent_mark_in_progress())) {
    clone_barrier(src);
  }
}
