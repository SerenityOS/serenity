/*
 * Copyright (c) 2018, 2020, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahEvacOOMHandler.inline.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"

const jint ShenandoahEvacOOMHandler::OOM_MARKER_MASK = 0x80000000;

ShenandoahEvacOOMHandler::ShenandoahEvacOOMHandler() :
  _threads_in_evac(0) {
}

void ShenandoahEvacOOMHandler::wait_for_no_evac_threads() {
  while ((Atomic::load_acquire(&_threads_in_evac) & ~OOM_MARKER_MASK) != 0) {
    os::naked_short_sleep(1);
  }
  // At this point we are sure that no threads can evacuate anything. Raise
  // the thread-local oom_during_evac flag to indicate that any attempt
  // to evacuate should simply return the forwarding pointer instead (which is safe now).
  ShenandoahThreadLocalData::set_oom_during_evac(Thread::current(), true);
}

void ShenandoahEvacOOMHandler::register_thread(Thread* thr) {
  jint threads_in_evac = Atomic::load_acquire(&_threads_in_evac);

  assert(!ShenandoahThreadLocalData::is_oom_during_evac(Thread::current()), "TL oom-during-evac must not be set");
  while (true) {
    // Check for OOM.
    // If offender has OOM_MARKER_MASK, then loop until no more threads in evac
    if ((threads_in_evac & OOM_MARKER_MASK) != 0) {
      wait_for_no_evac_threads();
      return;
    }

    jint other = Atomic::cmpxchg(&_threads_in_evac, threads_in_evac, threads_in_evac + 1);
    if (other == threads_in_evac) {
      // Success: caller may safely enter evacuation
      return;
    } else {
      threads_in_evac = other;
    }
  }
}

void ShenandoahEvacOOMHandler::unregister_thread(Thread* thr) {
  if (!ShenandoahThreadLocalData::is_oom_during_evac(thr)) {
    assert((Atomic::load_acquire(&_threads_in_evac) & ~OOM_MARKER_MASK) > 0, "sanity");
    // NOTE: It's ok to simply decrement, even with mask set, because unmasked value is positive.
    Atomic::dec(&_threads_in_evac);
  } else {
    // If we get here, the current thread has already gone through the
    // OOM-during-evac protocol and has thus either never entered or successfully left
    // the evacuation region. Simply flip its TL oom-during-evac flag back off.
    ShenandoahThreadLocalData::set_oom_during_evac(thr, false);
  }
  assert(!ShenandoahThreadLocalData::is_oom_during_evac(thr), "TL oom-during-evac must be turned off");
}

void ShenandoahEvacOOMHandler::handle_out_of_memory_during_evacuation() {
  assert(ShenandoahThreadLocalData::is_evac_allowed(Thread::current()), "sanity");
  assert(!ShenandoahThreadLocalData::is_oom_during_evac(Thread::current()), "TL oom-during-evac must not be set");

  jint threads_in_evac = Atomic::load_acquire(&_threads_in_evac);
  while (true) {
    jint other = Atomic::cmpxchg(&_threads_in_evac, threads_in_evac, (threads_in_evac - 1) | OOM_MARKER_MASK);
    if (other == threads_in_evac) {
      // Success: wait for other threads to get out of the protocol and return.
      wait_for_no_evac_threads();
      return;
    } else {
      // Failure: try again with updated new value.
      threads_in_evac = other;
    }
  }
}

void ShenandoahEvacOOMHandler::clear() {
  assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "must be at a safepoint");
  assert((Atomic::load_acquire(&_threads_in_evac) & ~OOM_MARKER_MASK) == 0, "sanity");
  Atomic::release_store_fence(&_threads_in_evac, (jint)0);
}
