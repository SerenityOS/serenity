/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdLoadBarrier.inline.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdKlassQueue.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/utilities/jfrEpochQueue.inline.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/mutexLocker.hpp"

// The queue instance used by the load barrier to enqueue tagged Klass'es.
static JfrTraceIdKlassQueue* _klass_queue = NULL;

static JfrTraceIdKlassQueue& klass_queue() {
  assert(_klass_queue != NULL, "invariant");
  return *_klass_queue;
}

const size_t buffer_size_bytes = 1 * K; // min_elem_size of storage unit
const size_t prealloc_count = 32;

bool JfrTraceIdLoadBarrier::initialize() {
  assert(_klass_queue == NULL, "invariant");
  _klass_queue = new JfrTraceIdKlassQueue();
  return _klass_queue != NULL && _klass_queue->initialize(buffer_size_bytes, JFR_MSPACE_UNLIMITED_CACHE_SIZE, prealloc_count);
}

void JfrTraceIdLoadBarrier::clear() {
  if (_klass_queue != NULL) {
    _klass_queue->clear();
  }
}

void JfrTraceIdLoadBarrier::destroy() {
  delete _klass_queue;
  _klass_queue = NULL;
}

void JfrTraceIdLoadBarrier::enqueue(const Klass* klass) {
  assert(klass != NULL, "invariant");
  assert(USED_THIS_EPOCH(klass), "invariant");
  klass_queue().enqueue(klass);
}

void JfrTraceIdLoadBarrier::do_klasses(klass_callback callback, bool previous_epoch) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  klass_queue().iterate(callback, previous_epoch);
}
