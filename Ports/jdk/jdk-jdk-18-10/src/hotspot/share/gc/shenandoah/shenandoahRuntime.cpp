/*
 * Copyright (c) 2018, 2019, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/shenandoahBarrierSet.inline.hpp"
#include "gc/shenandoah/shenandoahBarrierSetClone.inline.hpp"
#include "gc/shenandoah/shenandoahRuntime.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/copy.hpp"

void ShenandoahRuntime::arraycopy_barrier_oop_entry(oop* src, oop* dst, size_t length) {
  ShenandoahBarrierSet *bs = ShenandoahBarrierSet::barrier_set();
  bs->arraycopy_barrier(src, dst, length);
}

void ShenandoahRuntime::arraycopy_barrier_narrow_oop_entry(narrowOop* src, narrowOop* dst, size_t length) {
  ShenandoahBarrierSet *bs = ShenandoahBarrierSet::barrier_set();
  bs->arraycopy_barrier(src, dst, length);
}

// Shenandoah pre write barrier slowpath
JRT_LEAF(void, ShenandoahRuntime::write_ref_field_pre_entry(oopDesc* orig, JavaThread *thread))
  assert(orig != NULL, "should be optimized out");
  shenandoah_assert_correct(NULL, orig);
  // store the original value that was in the field reference
  assert(ShenandoahThreadLocalData::satb_mark_queue(thread).is_active(), "Shouldn't be here otherwise");
  SATBMarkQueue& queue = ShenandoahThreadLocalData::satb_mark_queue(thread);
  ShenandoahBarrierSet::satb_mark_queue_set().enqueue_known_active(queue, orig);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_strong(oopDesc* src, oop* load_addr))
  return ShenandoahBarrierSet::barrier_set()->load_reference_barrier_mutator(src, load_addr);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_strong_narrow(oopDesc* src, narrowOop* load_addr))
  return ShenandoahBarrierSet::barrier_set()->load_reference_barrier_mutator(src, load_addr);
JRT_END

// Shenandoah clone barrier: makes sure that references point to to-space
// in cloned objects.
JRT_LEAF(void, ShenandoahRuntime::shenandoah_clone_barrier(oopDesc* src))
  oop s = oop(src);
  shenandoah_assert_correct(NULL, s);
  ShenandoahBarrierSet::barrier_set()->clone_barrier(s);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_weak(oopDesc * src, oop* load_addr))
  return (oopDesc*) ShenandoahBarrierSet::barrier_set()->load_reference_barrier<oop>(ON_WEAK_OOP_REF, oop(src), load_addr);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_weak_narrow(oopDesc * src, narrowOop* load_addr))
  return (oopDesc*) ShenandoahBarrierSet::barrier_set()->load_reference_barrier<narrowOop>(ON_WEAK_OOP_REF, oop(src), load_addr);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_phantom(oopDesc * src, oop* load_addr))
  return (oopDesc*) ShenandoahBarrierSet::barrier_set()->load_reference_barrier<oop>(ON_PHANTOM_OOP_REF, oop(src), load_addr);
JRT_END

JRT_LEAF(oopDesc*, ShenandoahRuntime::load_reference_barrier_phantom_narrow(oopDesc * src, narrowOop* load_addr))
  return (oopDesc*) ShenandoahBarrierSet::barrier_set()->load_reference_barrier<narrowOop>(ON_PHANTOM_OOP_REF, oop(src), load_addr);
JRT_END
