/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHECKPOINT_EVENTEMITTER_HPP
#define SHARE_JFR_LEAKPROFILER_CHECKPOINT_EVENTEMITTER_HPP

#include "memory/allocation.hpp"
#include "jfr/utilities/jfrTime.hpp"

typedef u8 traceid;

class EdgeStore;
class JfrThreadLocal;
class ObjectSample;
class ObjectSampler;
class Thread;

class EventEmitter : public CHeapObj<mtTracing> {
  friend class LeakProfiler;
  friend class PathToGcRootsOperation;
 private:
  const JfrTicks& _start_time;
  const JfrTicks& _end_time;
  Thread* _thread;
  JfrThreadLocal* _jfr_thread_local;
  traceid _thread_id;

  EventEmitter(const JfrTicks& start_time, const JfrTicks& end_time);
  ~EventEmitter();

  void write_event(const ObjectSample* sample, EdgeStore* edge_store);
  size_t write_events(ObjectSampler* sampler, EdgeStore* store, bool emit_all);
  void link_sample_with_edge(const ObjectSample* sample, EdgeStore* edge_store);

  static void emit(ObjectSampler* sampler, int64_t cutoff_ticks, bool emit_all, bool skip_bfs);
};

#endif // SHARE_JFR_LEAKPROFILER_CHECKPOINT_EVENTEMITTER_HPP
