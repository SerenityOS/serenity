/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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

#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahTaskqueue.inline.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/resourceArea.hpp"

void ShenandoahObjToScanQueueSet::clear() {
  uint size = GenericTaskQueueSet<ShenandoahObjToScanQueue, mtGC>::size();
  for (uint index = 0; index < size; index ++) {
    ShenandoahObjToScanQueue* q = queue(index);
    assert(q != NULL, "Sanity");
    q->clear();
  }
}

bool ShenandoahObjToScanQueueSet::is_empty() {
  uint size = GenericTaskQueueSet<ShenandoahObjToScanQueue, mtGC>::size();
  for (uint index = 0; index < size; index ++) {
    ShenandoahObjToScanQueue* q = queue(index);
    assert(q != NULL, "Sanity");
    if (!q->is_empty()) {
      return false;
    }
  }
  return true;
}

#if TASKQUEUE_STATS
void ShenandoahObjToScanQueueSet::print_taskqueue_stats_hdr(outputStream* const st) {
  st->print_raw_cr("GC Task Stats");
  st->print_raw("thr "); TaskQueueStats::print_header(1, st); st->cr();
  st->print_raw("--- "); TaskQueueStats::print_header(2, st); st->cr();
}

void ShenandoahObjToScanQueueSet::print_taskqueue_stats() const {
  if (!log_develop_is_enabled(Trace, gc, task, stats)) {
    return;
  }
  Log(gc, task, stats) log;
  ResourceMark rm;
  LogStream ls(log.trace());
  outputStream* st = &ls;
  print_taskqueue_stats_hdr(st);

  ShenandoahObjToScanQueueSet* queues = const_cast<ShenandoahObjToScanQueueSet*>(this);
  TaskQueueStats totals;
  const uint n = size();
  for (uint i = 0; i < n; ++i) {
    st->print(UINT32_FORMAT_W(3), i);
    queues->queue(i)->stats.print(st);
    st->cr();
    totals += queues->queue(i)->stats;
  }
  st->print("tot "); totals.print(st); st->cr();
  DEBUG_ONLY(totals.verify());

}

void ShenandoahObjToScanQueueSet::reset_taskqueue_stats() {
  const uint n = size();
  for (uint i = 0; i < n; ++i) {
    queue(i)->stats.reset();
  }
}
#endif // TASKQUEUE_STATS

bool ShenandoahTerminatorTerminator::should_exit_termination() {
  return _heap->cancelled_gc();
}
