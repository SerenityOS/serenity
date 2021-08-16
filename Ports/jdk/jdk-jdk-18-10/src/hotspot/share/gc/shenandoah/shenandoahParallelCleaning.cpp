/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahClosures.inline.hpp"
#include "gc/shenandoah/shenandoahCodeRoots.hpp"
#include "gc/shenandoah/shenandoahEvacOOMHandler.hpp"
#include "gc/shenandoah/shenandoahParallelCleaning.hpp"
#include "runtime/safepoint.hpp"

ShenandoahClassUnloadingTask::ShenandoahClassUnloadingTask(ShenandoahPhaseTimings::Phase phase,
                                                           BoolObjectClosure* is_alive,
                                                           uint num_workers,
                                                           bool unloading_occurred) :
  AbstractGangTask("Shenandoah Class Unloading"),
  _phase(phase),
  _unloading_occurred(unloading_occurred),
  _code_cache_task(num_workers, is_alive, unloading_occurred),
  _klass_cleaning_task() {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");
}

void ShenandoahClassUnloadingTask::work(uint worker_id) {
  {
    ShenandoahWorkerTimingsTracker x(_phase, ShenandoahPhaseTimings::CodeCacheUnload, worker_id);
    _code_cache_task.work(worker_id);
  }
  // Clean all klasses that were not unloaded.
  // The weak metadata in klass doesn't need to be
  // processed if there was no unloading.
  if (_unloading_occurred) {
    ShenandoahWorkerTimingsTracker x(_phase, ShenandoahPhaseTimings::CLDUnlink, worker_id);
    _klass_cleaning_task.work();
  }
}
