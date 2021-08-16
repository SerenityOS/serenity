/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHEVACOOMHANDLER_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHEVACOOMHANDLER_INLINE_HPP

#include "gc/shenandoah/shenandoahEvacOOMHandler.hpp"

#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahThreadLocalData.hpp"
#include "runtime/atomic.hpp"

void ShenandoahEvacOOMHandler::enter_evacuation(Thread* thr) {
  jint threads_in_evac = Atomic::load_acquire(&_threads_in_evac);

  uint8_t level = ShenandoahThreadLocalData::push_evac_oom_scope(thr);
 if (level == 0) {
   // Entering top level scope, register this thread.
   register_thread(thr);
 } else if (!ShenandoahThreadLocalData::is_oom_during_evac(thr)) {
   jint threads_in_evac = Atomic::load_acquire(&_threads_in_evac);
   // If OOM is in progress, handle it.
   if ((threads_in_evac & OOM_MARKER_MASK) != 0) {
     assert((threads_in_evac & ~OOM_MARKER_MASK) > 0, "sanity");
     Atomic::dec(&_threads_in_evac);
     wait_for_no_evac_threads();
   }
 }
}

void ShenandoahEvacOOMHandler::leave_evacuation(Thread* thr) {
  uint8_t level = ShenandoahThreadLocalData::pop_evac_oom_scope(thr);
  // Not top level, just return
  if (level > 1) {
    return;
  }

  // Leaving top level scope, unregister this thread.
  unregister_thread(thr);
}

ShenandoahEvacOOMScope::ShenandoahEvacOOMScope() :
  _thread(Thread::current()) {
  ShenandoahHeap::heap()->enter_evacuation(_thread);
}

ShenandoahEvacOOMScope::ShenandoahEvacOOMScope(Thread* t) :
  _thread(t) {
  ShenandoahHeap::heap()->enter_evacuation(_thread);
}

ShenandoahEvacOOMScope::~ShenandoahEvacOOMScope() {
  ShenandoahHeap::heap()->leave_evacuation(_thread);
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHEVACOOMHANDLER_INLINE_HPP
