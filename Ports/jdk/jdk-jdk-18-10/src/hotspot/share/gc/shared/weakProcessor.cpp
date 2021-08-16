/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/stringTable.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/weakProcessor.inline.hpp"
#include "gc/shared/oopStorageSetParState.inline.hpp"
#include "gc/shared/weakProcessorTimes.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"
#include "prims/resolvedMethodTable.hpp"
#include "runtime/globals.hpp"
#include "utilities/macros.hpp"

#if INCLUDE_JVMTI
#include "prims/jvmtiTagMap.hpp"
#endif // INCLUDE_JVMTI

void notify_jvmti_tagmaps() {
#if INCLUDE_JVMTI
  // Notify JVMTI tagmaps that a STW weak reference processing might be
  // clearing entries, so the tagmaps need cleaning.  Doing this here allows
  // the tagmap's oopstorage notification handler to not care whether it's
  // invoked by STW or concurrent reference processing.
  JvmtiTagMap::set_needs_cleaning();

  // Notify JVMTI tagmaps that a STW collection may have moved objects, so
  // the tagmaps need rehashing.  This isn't the right place for this, but
  // is convenient because all the STW collectors use WeakProcessor.  One
  // problem is that the end of a G1 concurrent collection also comes here,
  // possibly triggering unnecessary rehashes.
  JvmtiTagMap::set_needs_rehashing();
#endif // INCLUDE_JVMTI
}

void WeakProcessor::weak_oops_do(BoolObjectClosure* is_alive, OopClosure* keep_alive) {

  notify_jvmti_tagmaps();

  for (OopStorage* storage : OopStorageSet::Range<OopStorageSet::WeakId>()) {
    if (storage->should_report_num_dead()) {
      CountingClosure<BoolObjectClosure, OopClosure> cl(is_alive, keep_alive);
      storage->oops_do(&cl);
      storage->report_num_dead(cl.dead());
    } else {
      storage->weak_oops_do(is_alive, keep_alive);
    }
  }
}

void WeakProcessor::oops_do(OopClosure* closure) {
  for (OopStorage* storage : OopStorageSet::Range<OopStorageSet::WeakId>()) {
    storage->weak_oops_do(closure);
  }
}

uint WeakProcessor::ergo_workers(uint max_workers) {
  // Ignore ParallelRefProcEnabled; that's for j.l.r.Reference processing.
  if (ReferencesPerThread == 0) {
    // Configuration says always use all the threads.
    return max_workers;
  }

  // One thread per ReferencesPerThread references (or fraction thereof)
  // in the various OopStorage objects, bounded by max_threads.
  size_t ref_count = 0;
  for (OopStorage* storage : OopStorageSet::Range<OopStorageSet::WeakId>()) {
    ref_count += storage->allocation_count();
  }

  // +1 to (approx) round up the ref per thread division.
  size_t nworkers = 1 + (ref_count / ReferencesPerThread);
  nworkers = MIN2(nworkers, static_cast<size_t>(max_workers));
  return static_cast<uint>(nworkers);
}

void WeakProcessor::Task::initialize() {
  assert(_nworkers != 0, "must be");
  assert(_times == nullptr || _nworkers <= _times->max_threads(),
         "nworkers (%u) exceeds max threads (%u)",
         _nworkers, _times->max_threads());

  if (_times) {
    _times->set_active_workers(_nworkers);
  }
  notify_jvmti_tagmaps();
}

WeakProcessor::Task::Task(uint nworkers) : Task(nullptr, nworkers) {}

WeakProcessor::Task::Task(WeakProcessorTimes* times, uint nworkers) :
  _times(times),
  _nworkers(nworkers),
  _storage_states()
{
  initialize();
}

void WeakProcessor::Task::report_num_dead() {
  _storage_states.report_num_dead();
}

void WeakProcessor::GangTask::work(uint worker_id) {
  _erased_do_work(this, worker_id);
}
