/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gc_globals.hpp"
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/leakprofiler/chains/bfsClosure.hpp"
#include "jfr/leakprofiler/chains/bitset.inline.hpp"
#include "jfr/leakprofiler/chains/dfsClosure.hpp"
#include "jfr/leakprofiler/chains/edge.hpp"
#include "jfr/leakprofiler/chains/edgeQueue.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/objectSampleMarker.hpp"
#include "jfr/leakprofiler/chains/rootSetClosure.hpp"
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/objectSampleMarker.hpp"
#include "jfr/leakprofiler/chains/pathToGcRootsOperation.hpp"
#include "jfr/leakprofiler/checkpoint/eventEmitter.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleCheckpoint.hpp"
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "jfr/leakprofiler/sampling/objectSampler.hpp"
#include "jfr/leakprofiler/utilities/granularTimer.hpp"
#include "logging/log.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/globalDefinitions.hpp"

PathToGcRootsOperation::PathToGcRootsOperation(ObjectSampler* sampler, EdgeStore* edge_store, int64_t cutoff, bool emit_all, bool skip_bfs) :
  _sampler(sampler),_edge_store(edge_store), _cutoff_ticks(cutoff), _emit_all(emit_all), _skip_bfs(skip_bfs) {}

/* The EdgeQueue is backed by directly managed virtual memory.
 * We will attempt to dimension an initial reservation
 * in proportion to the size of the heap (represented by heap_region).
 * Initial memory reservation: 5% of the heap OR at least 32 Mb
 * Commit ratio: 1 : 10 (subject to allocation granularties)
 */
static size_t edge_queue_memory_reservation() {
  const size_t memory_reservation_bytes = MAX2(MaxHeapSize / 20, 32*M);
  assert(memory_reservation_bytes >= (size_t)32*M, "invariant");
  return memory_reservation_bytes;
}

static size_t edge_queue_memory_commit_size(size_t memory_reservation_bytes) {
  const size_t memory_commit_block_size_bytes = memory_reservation_bytes / 10;
  assert(memory_commit_block_size_bytes >= (size_t)3*M, "invariant");
  return memory_commit_block_size_bytes;
}

static void log_edge_queue_summary(const EdgeQueue& edge_queue) {
  log_trace(jfr, system)("EdgeQueue reserved size total: " SIZE_FORMAT " [KB]", edge_queue.reserved_size() / K);
  log_trace(jfr, system)("EdgeQueue edges total: " SIZE_FORMAT, edge_queue.top());
  log_trace(jfr, system)("EdgeQueue liveset total: " SIZE_FORMAT " [KB]", edge_queue.live_set() / K);
  if (edge_queue.reserved_size() > 0) {
    log_trace(jfr, system)("EdgeQueue commit reserve ratio: %f\n",
      ((double)edge_queue.live_set() / (double)edge_queue.reserved_size()));
  }
}

void PathToGcRootsOperation::doit() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  assert(_cutoff_ticks > 0, "invariant");

  // The bitset used for marking is dimensioned as a function of the heap size
  BitSet mark_bits;

  // The edge queue is dimensioned as a fraction of the heap size
  const size_t edge_queue_reservation_size = edge_queue_memory_reservation();
  EdgeQueue edge_queue(edge_queue_reservation_size, edge_queue_memory_commit_size(edge_queue_reservation_size));

  // The initialize() routines will attempt to reserve and allocate backing storage memory.
  // Failure to accommodate will render root chain processing impossible.
  // As a fallback on failure, just write out the existing samples, flat, without chains.
  if (!edge_queue.initialize()) {
    log_warning(jfr)("Unable to allocate memory for root chain processing");
    return;
  }

  // Save the original markWord for the potential leak objects,
  // to be restored on function exit
  ObjectSampleMarker marker;
  if (ObjectSampleCheckpoint::save_mark_words(_sampler, marker, _emit_all) == 0) {
    // no valid samples to process
    return;
  }

  // Necessary condition for attempting a root set iteration
  Universe::heap()->ensure_parsability(false);

  BFSClosure bfs(&edge_queue, _edge_store, &mark_bits);
  RootSetClosure<BFSClosure> roots(&bfs);

  GranularTimer::start(_cutoff_ticks, 1000000);
  roots.process();
  if (edge_queue.is_full() || _skip_bfs) {
    // Pathological case where roots don't fit in queue
    // Do a depth-first search, but mark roots first
    // to avoid walking sideways over roots
    DFSClosure::find_leaks_from_root_set(_edge_store, &mark_bits);
  } else {
    bfs.process();
  }
  GranularTimer::stop();
  log_edge_queue_summary(edge_queue);

  // Emit old objects including their reference chains as events
  EventEmitter emitter(GranularTimer::start_time(), GranularTimer::end_time());
  emitter.write_events(_sampler, _edge_store, _emit_all);
}
