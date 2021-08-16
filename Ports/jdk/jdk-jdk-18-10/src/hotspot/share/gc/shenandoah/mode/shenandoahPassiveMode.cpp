/*
 * Copyright (c) 2019, 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/heuristics/shenandoahPassiveHeuristics.hpp"
#include "gc/shenandoah/mode/shenandoahPassiveMode.hpp"
#include "logging/log.hpp"
#include "logging/logTag.hpp"
#include "runtime/globals_extension.hpp"

void ShenandoahPassiveMode::initialize_flags() const {
  // Do not allow concurrent cycles.
  FLAG_SET_DEFAULT(ExplicitGCInvokesConcurrent, false);
  FLAG_SET_DEFAULT(ShenandoahImplicitGCInvokesConcurrent, false);

  // Passive runs with max speed for allocation, because GC is always STW
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahPacing);

  // No need for evacuation reserve with Full GC, only for Degenerated GC.
  if (!ShenandoahDegeneratedGC) {
    SHENANDOAH_ERGO_OVERRIDE_DEFAULT(ShenandoahEvacReserve, 0);
  }

  // Disable known barriers by default.
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahLoadRefBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahSATBBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahIUBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahCASBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahCloneBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahNMethodBarrier);
  SHENANDOAH_ERGO_DISABLE_FLAG(ShenandoahStackWatermarkBarrier);

  // Final configuration checks
  // No barriers are required to run.
}
ShenandoahHeuristics* ShenandoahPassiveMode::initialize_heuristics() const {
  if (ShenandoahGCHeuristics != NULL) {
    return new ShenandoahPassiveHeuristics();
  }
  ShouldNotReachHere();
  return NULL;
}
