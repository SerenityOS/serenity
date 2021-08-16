/*
 * Copyright (c) 2020, 2021, Red Hat, Inc. All rights reserved.
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
#include "gc/shenandoah/heuristics/shenandoahAdaptiveHeuristics.hpp"
#include "gc/shenandoah/heuristics/shenandoahAggressiveHeuristics.hpp"
#include "gc/shenandoah/heuristics/shenandoahCompactHeuristics.hpp"
#include "gc/shenandoah/heuristics/shenandoahStaticHeuristics.hpp"
#include "gc/shenandoah/mode/shenandoahIUMode.hpp"
#include "logging/log.hpp"
#include "logging/logTag.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"

void ShenandoahIUMode::initialize_flags() const {
  if (FLAG_IS_CMDLINE(ClassUnloadingWithConcurrentMark) && ClassUnloading) {
    log_warning(gc)("Shenandoah I-U mode sets -XX:-ClassUnloadingWithConcurrentMark; see JDK-8261341 for details");
  }
  FLAG_SET_DEFAULT(ClassUnloadingWithConcurrentMark, false);

  if (ClassUnloading) {
    FLAG_SET_DEFAULT(ShenandoahSuspendibleWorkers, true);
    FLAG_SET_DEFAULT(VerifyBeforeExit, false);
  }

  if (FLAG_IS_DEFAULT(ShenandoahIUBarrier)) {
    FLAG_SET_DEFAULT(ShenandoahIUBarrier, true);
  }
  if (FLAG_IS_DEFAULT(ShenandoahSATBBarrier)) {
    FLAG_SET_DEFAULT(ShenandoahSATBBarrier, false);
  }

  SHENANDOAH_ERGO_ENABLE_FLAG(ExplicitGCInvokesConcurrent);
  SHENANDOAH_ERGO_ENABLE_FLAG(ShenandoahImplicitGCInvokesConcurrent);

  // Final configuration checks
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahLoadRefBarrier);
  SHENANDOAH_CHECK_FLAG_UNSET(ShenandoahSATBBarrier);
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahIUBarrier);
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahCASBarrier);
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahCloneBarrier);
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahNMethodBarrier);
  SHENANDOAH_CHECK_FLAG_SET(ShenandoahStackWatermarkBarrier);
}

ShenandoahHeuristics* ShenandoahIUMode::initialize_heuristics() const {
  if (ShenandoahGCHeuristics != NULL) {
    if (strcmp(ShenandoahGCHeuristics, "aggressive") == 0) {
      return new ShenandoahAggressiveHeuristics();
    } else if (strcmp(ShenandoahGCHeuristics, "static") == 0) {
      return new ShenandoahStaticHeuristics();
    } else if (strcmp(ShenandoahGCHeuristics, "adaptive") == 0) {
      return new ShenandoahAdaptiveHeuristics();
    } else if (strcmp(ShenandoahGCHeuristics, "compact") == 0) {
      return new ShenandoahCompactHeuristics();
    } else {
      vm_exit_during_initialization("Unknown -XX:ShenandoahGCHeuristics option");
    }
  }
  ShouldNotReachHere();
  return NULL;
}
