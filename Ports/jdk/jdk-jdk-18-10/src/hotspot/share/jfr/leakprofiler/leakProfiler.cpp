/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/leakprofiler/startOperation.hpp"
#include "jfr/leakprofiler/stopOperation.hpp"
#include "jfr/leakprofiler/checkpoint/eventEmitter.hpp"
#include "jfr/leakprofiler/sampling/objectSampler.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "logging/log.hpp"
#include "memory/iterator.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"

bool LeakProfiler::is_running() {
  return ObjectSampler::is_created();
}

bool LeakProfiler::start(int sample_count) {
  if (is_running()) {
    return true;
  }

  // Allows user to disable leak profiler on command line by setting queue size to zero.
  if (sample_count == 0) {
    return false;
  }

  assert(!is_running(), "invariant");
  assert(sample_count > 0, "invariant");

  // schedule the safepoint operation for installing the object sampler
  StartOperation op(sample_count);
  VMThread::execute(&op);

  if (!is_running()) {
    log_trace(jfr, system)("Object sampling could not be started because the sampler could not be allocated");
    return false;
  }
  assert(is_running(), "invariant");
  log_trace(jfr, system)("Object sampling started");
  return true;
}

bool LeakProfiler::stop() {
  if (!is_running()) {
    return false;
  }

  // schedule the safepoint operation for uninstalling and destroying the object sampler
  StopOperation op;
  VMThread::execute(&op);

  assert(!is_running(), "invariant");
  log_trace(jfr, system)("Object sampling stopped");
  return true;
}

void LeakProfiler::emit_events(int64_t cutoff_ticks, bool emit_all, bool skip_bfs) {
  if (!is_running()) {
    return;
  }
  // exclusive access to object sampler instance
  ObjectSampler* const sampler = ObjectSampler::acquire();
  assert(sampler != NULL, "invariant");
  EventEmitter::emit(sampler, cutoff_ticks, emit_all, skip_bfs);
  ObjectSampler::release();
}

void LeakProfiler::sample(HeapWord* object, size_t size, JavaThread* thread) {
  assert(is_running(), "invariant");
  assert(thread != NULL, "invariant");
  assert(thread->thread_state() == _thread_in_vm, "invariant");

  // exclude compiler threads and code sweeper thread
  if (thread->is_hidden_from_external_view()) {
    return;
  }

  ObjectSampler::sample(object, size, thread);
}
