/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/metadata/jfrSerializer.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/recorder/stacktrace/jfrStackTraceRepository.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "runtime/mutexLocker.hpp"

/*
 * There are two separate repository instances.
 * One instance is dedicated to stacktraces taken as part of the leak profiler subsystem.
 * It is kept separate because at the point of insertion, it is unclear if a trace will be serialized,
 * which is a decision postponed and taken during rotation.
 */

static JfrStackTraceRepository* _instance = NULL;
static JfrStackTraceRepository* _leak_profiler_instance = NULL;
static traceid _next_id = 0;

JfrStackTraceRepository& JfrStackTraceRepository::instance() {
  assert(_instance != NULL, "invariant");
  return *_instance;
}

static JfrStackTraceRepository& leak_profiler_instance() {
  assert(_leak_profiler_instance != NULL, "invariant");
  return *_leak_profiler_instance;
}

JfrStackTraceRepository::JfrStackTraceRepository() : _last_entries(0), _entries(0) {
  memset(_table, 0, sizeof(_table));
}

JfrStackTraceRepository* JfrStackTraceRepository::create() {
  assert(_instance == NULL, "invariant");
  assert(_leak_profiler_instance == NULL, "invariant");
  _leak_profiler_instance = new JfrStackTraceRepository();
  if (_leak_profiler_instance == NULL) {
    return NULL;
  }
  _instance = new JfrStackTraceRepository();
  return _instance;
}

class JfrFrameType : public JfrSerializer {
 public:
  void serialize(JfrCheckpointWriter& writer) {
    writer.write_count(JfrStackFrame::NUM_FRAME_TYPES);
    writer.write_key(JfrStackFrame::FRAME_INTERPRETER);
    writer.write("Interpreted");
    writer.write_key(JfrStackFrame::FRAME_JIT);
    writer.write("JIT compiled");
    writer.write_key(JfrStackFrame::FRAME_INLINE);
    writer.write("Inlined");
    writer.write_key(JfrStackFrame::FRAME_NATIVE);
    writer.write("Native");
  }
};

bool JfrStackTraceRepository::initialize() {
  return JfrSerializer::register_serializer(TYPE_FRAMETYPE, true, new JfrFrameType());
}

void JfrStackTraceRepository::destroy() {
  assert(_instance != NULL, "invarinat");
  delete _instance;
  _instance = NULL;
  delete _leak_profiler_instance;
  _leak_profiler_instance = NULL;
}

bool JfrStackTraceRepository::is_modified() const {
  return _last_entries != _entries;
}

size_t JfrStackTraceRepository::write(JfrChunkWriter& sw, bool clear) {
  if (_entries == 0) {
    return 0;
  }
  MutexLocker lock(JfrStacktrace_lock, Mutex::_no_safepoint_check_flag);
  assert(_entries > 0, "invariant");
  int count = 0;
  for (u4 i = 0; i < TABLE_SIZE; ++i) {
    JfrStackTrace* stacktrace = _table[i];
    while (stacktrace != NULL) {
      JfrStackTrace* next = const_cast<JfrStackTrace*>(stacktrace->next());
      if (stacktrace->should_write()) {
        stacktrace->write(sw);
        ++count;
      }
      if (clear) {
        delete stacktrace;
      }
      stacktrace = next;
    }
  }
  if (clear) {
    memset(_table, 0, sizeof(_table));
    _entries = 0;
  }
  _last_entries = _entries;
  return count;
}

size_t JfrStackTraceRepository::clear(JfrStackTraceRepository& repo) {
  MutexLocker lock(JfrStacktrace_lock, Mutex::_no_safepoint_check_flag);
  if (repo._entries == 0) {
    return 0;
  }
  for (u4 i = 0; i < TABLE_SIZE; ++i) {
    JfrStackTrace* stacktrace = repo._table[i];
    while (stacktrace != NULL) {
      JfrStackTrace* next = const_cast<JfrStackTrace*>(stacktrace->next());
      delete stacktrace;
      stacktrace = next;
    }
  }
  memset(repo._table, 0, sizeof(repo._table));
  const size_t processed = repo._entries;
  repo._entries = 0;
  repo._last_entries = 0;
  return processed;
}

traceid JfrStackTraceRepository::record(Thread* thread, int skip /* 0 */) {
  assert(thread == Thread::current(), "invariant");
  JfrThreadLocal* const tl = thread->jfr_thread_local();
  assert(tl != NULL, "invariant");
  if (tl->has_cached_stack_trace()) {
    return tl->cached_stack_trace_id();
  }
  if (!thread->is_Java_thread() || thread->is_hidden_from_external_view() || tl->is_excluded()) {
    return 0;
  }
  JfrStackFrame* frames = tl->stackframes();
  if (frames == NULL) {
    // pending oom
    return 0;
  }
  assert(frames != NULL, "invariant");
  assert(tl->stackframes() == frames, "invariant");
  return instance().record_for(JavaThread::cast(thread), skip, frames, tl->stackdepth());
}

traceid JfrStackTraceRepository::record_for(JavaThread* thread, int skip, JfrStackFrame *frames, u4 max_frames) {
  JfrStackTrace stacktrace(frames, max_frames);
  return stacktrace.record_safe(thread, skip) ? add(instance(), stacktrace) : 0;
}
traceid JfrStackTraceRepository::add(JfrStackTraceRepository& repo, const JfrStackTrace& stacktrace) {
  traceid tid = repo.add_trace(stacktrace);
  if (tid == 0) {
    stacktrace.resolve_linenos();
    tid = repo.add_trace(stacktrace);
  }
  assert(tid != 0, "invariant");
  return tid;
}

traceid JfrStackTraceRepository::add(const JfrStackTrace& stacktrace) {
  return add(instance(), stacktrace);
}

void JfrStackTraceRepository::record_for_leak_profiler(JavaThread* thread, int skip /* 0 */) {
  assert(thread != NULL, "invariant");
  JfrThreadLocal* const tl = thread->jfr_thread_local();
  assert(tl != NULL, "invariant");
  assert(!tl->has_cached_stack_trace(), "invariant");
  JfrStackTrace stacktrace(tl->stackframes(), tl->stackdepth());
  stacktrace.record_safe(thread, skip);
  const unsigned int hash = stacktrace.hash();
  if (hash != 0) {
    tl->set_cached_stack_trace_id(add(leak_profiler_instance(), stacktrace), hash);
  }
}

traceid JfrStackTraceRepository::add_trace(const JfrStackTrace& stacktrace) {
  MutexLocker lock(JfrStacktrace_lock, Mutex::_no_safepoint_check_flag);
  const size_t index = stacktrace._hash % TABLE_SIZE;
  const JfrStackTrace* table_entry = _table[index];

  while (table_entry != NULL) {
    if (table_entry->equals(stacktrace)) {
      return table_entry->id();
    }
    table_entry = table_entry->next();
  }

  if (!stacktrace.have_lineno()) {
    return 0;
  }

  traceid id = ++_next_id;
  _table[index] = new JfrStackTrace(id, stacktrace, _table[index]);
  ++_entries;
  return id;
}

// invariant is that the entry to be resolved actually exists in the table
const JfrStackTrace* JfrStackTraceRepository::lookup_for_leak_profiler(unsigned int hash, traceid id) {
  const size_t index = (hash % TABLE_SIZE);
  const JfrStackTrace* trace = leak_profiler_instance()._table[index];
  while (trace != NULL && trace->id() != id) {
    trace = trace->next();
  }
  assert(trace != NULL, "invariant");
  assert(trace->hash() == hash, "invariant");
  assert(trace->id() == id, "invariant");
  return trace;
}

void JfrStackTraceRepository::clear_leak_profiler() {
  clear(leak_profiler_instance());
}

size_t JfrStackTraceRepository::clear() {
  clear_leak_profiler();
  return clear(instance());
}
