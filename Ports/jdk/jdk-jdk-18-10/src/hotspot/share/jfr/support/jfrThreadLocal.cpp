/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/jfrEvents.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleCheckpoint.hpp"
#include "jfr/periodic/jfrThreadCPULoadEvent.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointManager.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/recorder/storage/jfrStorage.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/sizes.hpp"

JfrThreadLocal::JfrThreadLocal() :
  _java_event_writer(NULL),
  _java_buffer(NULL),
  _native_buffer(NULL),
  _shelved_buffer(NULL),
  _load_barrier_buffer_epoch_0(NULL),
  _load_barrier_buffer_epoch_1(NULL),
  _stackframes(NULL),
  _trace_id(JfrTraceId::assign_thread_id()),
  _thread(),
  _data_lost(0),
  _stack_trace_id(max_julong),
  _user_time(0),
  _cpu_time(0),
  _wallclock_time(os::javaTimeNanos()),
  _stack_trace_hash(0),
  _stackdepth(0),
  _entering_suspend_flag(0),
  _excluded(false),
  _dead(false) {
  Thread* thread = Thread::current_or_null();
  _parent_trace_id = thread != NULL ? thread->jfr_thread_local()->trace_id() : (traceid)0;
}

u8 JfrThreadLocal::add_data_lost(u8 value) {
  _data_lost += value;
  return _data_lost;
}

bool JfrThreadLocal::has_thread_blob() const {
  return _thread.valid();
}

void JfrThreadLocal::set_thread_blob(const JfrBlobHandle& ref) {
  assert(!_thread.valid(), "invariant");
  _thread = ref;
}

const JfrBlobHandle& JfrThreadLocal::thread_blob() const {
  return _thread;
}

static void send_java_thread_start_event(JavaThread* jt) {
  EventThreadStart event;
  event.set_thread(jt->jfr_thread_local()->thread_id());
  event.set_parentThread(jt->jfr_thread_local()->parent_thread_id());
  event.commit();
}

void JfrThreadLocal::on_start(Thread* t) {
  assert(t != NULL, "invariant");
  assert(Thread::current() == t, "invariant");
  JfrJavaSupport::on_thread_start(t);
  if (JfrRecorder::is_recording()) {
    JfrCheckpointManager::write_thread_checkpoint(t);
    if (!t->jfr_thread_local()->is_excluded()) {
      if (t->is_Java_thread()) {
        send_java_thread_start_event(JavaThread::cast(t));
      }
    }
  }
  if (t->jfr_thread_local()->has_cached_stack_trace()) {
    t->jfr_thread_local()->clear_cached_stack_trace();
  }
}

static void send_java_thread_end_events(traceid id, JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(Thread::current() == jt, "invariant");
  assert(jt->jfr_thread_local()->trace_id() == id, "invariant");
  if (JfrRecorder::is_recording()) {
    EventThreadEnd event;
    event.set_thread(id);
    event.commit();
    JfrThreadCPULoadEvent::send_event_for_thread(jt);
  }
}

void JfrThreadLocal::release(Thread* t) {
  if (has_java_event_writer()) {
    assert(t->is_Java_thread(), "invariant");
    JfrJavaSupport::destroy_global_jni_handle(java_event_writer());
    _java_event_writer = NULL;
  }
  if (has_native_buffer()) {
    JfrStorage::release_thread_local(native_buffer(), t);
    _native_buffer = NULL;
  }
  if (has_java_buffer()) {
    JfrStorage::release_thread_local(java_buffer(), t);
    _java_buffer = NULL;
  }
  if (_stackframes != NULL) {
    FREE_C_HEAP_ARRAY(JfrStackFrame, _stackframes);
    _stackframes = NULL;
  }
  if (_load_barrier_buffer_epoch_0 != NULL) {
    _load_barrier_buffer_epoch_0->set_retired();
    _load_barrier_buffer_epoch_0 = NULL;
  }
  if (_load_barrier_buffer_epoch_1 != NULL) {
    _load_barrier_buffer_epoch_1->set_retired();
    _load_barrier_buffer_epoch_1 = NULL;
  }
}

void JfrThreadLocal::release(JfrThreadLocal* tl, Thread* t) {
  assert(tl != NULL, "invariant");
  assert(t != NULL, "invariant");
  assert(Thread::current() == t, "invariant");
  assert(!tl->is_dead(), "invariant");
  assert(tl->shelved_buffer() == NULL, "invariant");
  tl->_dead = true;
  tl->release(t);
}

void JfrThreadLocal::on_exit(Thread* t) {
  assert(t != NULL, "invariant");
  JfrThreadLocal * const tl = t->jfr_thread_local();
  assert(!tl->is_dead(), "invariant");
  if (JfrRecorder::is_recording()) {
    if (t->is_Java_thread()) {
      JavaThread* const jt = JavaThread::cast(t);
      ObjectSampleCheckpoint::on_thread_exit(jt);
      send_java_thread_end_events(tl->thread_id(), jt);
    }
  }
  release(tl, Thread::current()); // because it could be that Thread::current() != t
}

static JfrBuffer* acquire_buffer(bool excluded) {
  JfrBuffer* const buffer = JfrStorage::acquire_thread_local(Thread::current());
  if (buffer != NULL && excluded) {
    buffer->set_excluded();
  }
  return buffer;
}

JfrBuffer* JfrThreadLocal::install_native_buffer() const {
  assert(!has_native_buffer(), "invariant");
  _native_buffer = acquire_buffer(_excluded);
  return _native_buffer;
}

JfrBuffer* JfrThreadLocal::install_java_buffer() const {
  assert(!has_java_buffer(), "invariant");
  assert(!has_java_event_writer(), "invariant");
  _java_buffer = acquire_buffer(_excluded);
  return _java_buffer;
}

JfrStackFrame* JfrThreadLocal::install_stackframes() const {
  assert(_stackframes == NULL, "invariant");
  _stackframes = NEW_C_HEAP_ARRAY(JfrStackFrame, stackdepth(), mtTracing);
  return _stackframes;
}

ByteSize JfrThreadLocal::trace_id_offset() {
  return in_ByteSize(offset_of(JfrThreadLocal, _trace_id));
}

ByteSize JfrThreadLocal::java_event_writer_offset() {
  return in_ByteSize(offset_of(JfrThreadLocal, _java_event_writer));
}

void JfrThreadLocal::exclude(Thread* t) {
  assert(t != NULL, "invariant");
  t->jfr_thread_local()->_excluded = true;
  t->jfr_thread_local()->release(t);
}

void JfrThreadLocal::include(Thread* t) {
  assert(t != NULL, "invariant");
  t->jfr_thread_local()->_excluded = false;
  t->jfr_thread_local()->release(t);
}

u4 JfrThreadLocal::stackdepth() const {
  return _stackdepth != 0 ? _stackdepth : (u4)JfrOptionSet::stackdepth();
}
