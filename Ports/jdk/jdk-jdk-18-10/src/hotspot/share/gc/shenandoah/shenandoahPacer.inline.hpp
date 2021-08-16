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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHPACER_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHPACER_INLINE_HPP

#include "gc/shenandoah/shenandoahPacer.hpp"

#include "runtime/atomic.hpp"

inline void ShenandoahPacer::report_mark(size_t words) {
  report_internal(words);
  report_progress_internal(words);
}

inline void ShenandoahPacer::report_evac(size_t words) {
  report_internal(words);
}

inline void ShenandoahPacer::report_updaterefs(size_t words) {
  report_internal(words);
}

inline void ShenandoahPacer::report_alloc(size_t words) {
  report_internal(words);
}

inline void ShenandoahPacer::report_internal(size_t words) {
  assert(ShenandoahPacing, "Only be here when pacing is enabled");
  add_budget(words);
}

inline void ShenandoahPacer::report_progress_internal(size_t words) {
  assert(ShenandoahPacing, "Only be here when pacing is enabled");
  STATIC_ASSERT(sizeof(size_t) <= sizeof(intptr_t));
  Atomic::add(&_progress, (intptr_t)words, memory_order_relaxed);
}

inline void ShenandoahPacer::add_budget(size_t words) {
  STATIC_ASSERT(sizeof(size_t) <= sizeof(intptr_t));
  intptr_t inc = (intptr_t) words;
  intptr_t new_budget = Atomic::add(&_budget, inc, memory_order_relaxed);

  // Was the budget replenished beyond zero? Then all pacing claims
  // are satisfied, notify the waiters. Avoid taking any locks here,
  // as it can be called from hot paths and/or while holding other locks.
  if (new_budget >= 0 && (new_budget - inc) < 0) {
    _need_notify_waiters.try_set();
  }
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHPACER_INLINE_HPP
