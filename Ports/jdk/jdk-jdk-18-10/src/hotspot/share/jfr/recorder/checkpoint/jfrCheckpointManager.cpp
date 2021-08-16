/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleCheckpoint.hpp"
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointManager.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/checkpoint/types/jfrTypeManager.hpp"
#include "jfr/recorder/checkpoint/types/jfrTypeSet.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/recorder/storage/jfrEpochStorage.inline.hpp"
#include "jfr/recorder/storage/jfrMemorySpace.inline.hpp"
#include "jfr/recorder/storage/jfrStorageUtils.inline.hpp"
#include "jfr/support/jfrKlassUnloading.hpp"
#include "jfr/utilities/jfrBigEndian.hpp"
#include "jfr/utilities/jfrIterator.hpp"
#include "jfr/utilities/jfrLinkedList.inline.hpp"
#include "jfr/utilities/jfrSignal.hpp"
#include "jfr/utilities/jfrThreadIterator.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "jfr/writers/jfrJavaEventWriter.hpp"
#include "logging/log.hpp"
#include "memory/iterator.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutex.hpp"
#include "runtime/safepoint.hpp"

typedef JfrCheckpointManager::BufferPtr BufferPtr;

static JfrSignal _new_checkpoint;
static JfrCheckpointManager* _instance = NULL;

JfrCheckpointManager& JfrCheckpointManager::instance() {
  return *_instance;
}

JfrCheckpointManager* JfrCheckpointManager::create(JfrChunkWriter& cw) {
  assert(_instance == NULL, "invariant");
  _instance = new JfrCheckpointManager(cw);
  return _instance;
}

void JfrCheckpointManager::destroy() {
  assert(_instance != NULL, "invariant");
  delete _instance;
  _instance = NULL;
}

JfrCheckpointManager::JfrCheckpointManager(JfrChunkWriter& cw) :
  _global_mspace(NULL),
  _thread_local_mspace(NULL),
  _chunkwriter(cw) {}

JfrCheckpointManager::~JfrCheckpointManager() {
  JfrTraceIdLoadBarrier::destroy();
  JfrTypeManager::destroy();
  delete _global_mspace;
  delete _thread_local_mspace;
}

static const size_t global_buffer_prealloc_count = 2;
static const size_t global_buffer_size = 512 * K;

static const size_t thread_local_buffer_prealloc_count = 16;
static const size_t thread_local_buffer_size = 128;

bool JfrCheckpointManager::initialize() {
  assert(_global_mspace == NULL, "invariant");
  _global_mspace =  create_mspace<JfrCheckpointMspace, JfrCheckpointManager>(global_buffer_size, 0, 0, false, this); // post-pone preallocation
  if (_global_mspace == NULL) {
    return false;
  }
  // preallocate buffer count to each of the epoch live lists
  for (size_t i = 0; i < global_buffer_prealloc_count * 2; ++i) {
    Buffer* const buffer = mspace_allocate(global_buffer_size, _global_mspace);
    _global_mspace->add_to_live_list(buffer, i % 2 == 0);
  }
  assert(_global_mspace->free_list_is_empty(), "invariant");

  assert(_thread_local_mspace == NULL, "invariant");
  _thread_local_mspace = new JfrThreadLocalCheckpointMspace();
  if (_thread_local_mspace == NULL || !_thread_local_mspace->initialize(thread_local_buffer_size,
                                                                        JFR_MSPACE_UNLIMITED_CACHE_SIZE,
                                                                        thread_local_buffer_prealloc_count)) {
    return false;
  }
  return JfrTypeManager::initialize() && JfrTraceIdLoadBarrier::initialize();
}

#ifdef ASSERT
static void assert_lease(const BufferPtr buffer) {
  assert(buffer != NULL, "invariant");
  assert(buffer->acquired_by_self(), "invariant");
  assert(buffer->lease(), "invariant");
}

static void assert_release(const BufferPtr buffer) {
  assert(buffer != NULL, "invariant");
  assert(buffer->lease(), "invariant");
  assert(buffer->acquired_by_self(), "invariant");
}

static void assert_retired(const BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(buffer->acquired_by(thread), "invariant");
  assert(buffer->retired(), "invariant");
}
#endif // ASSERT

void JfrCheckpointManager::register_full(BufferPtr buffer, Thread* thread) {
  DEBUG_ONLY(assert_retired(buffer, thread);)
  // nothing here at the moment
}

BufferPtr JfrCheckpointManager::lease(Thread* thread, bool previous_epoch /* false */, size_t size /* 0 */) {
  JfrCheckpointMspace* const mspace = instance()._global_mspace;
  assert(mspace != NULL, "invariant");
  static const size_t max_elem_size = mspace->min_element_size(); // min is max
  BufferPtr buffer;
  if (size <= max_elem_size) {
    buffer = mspace_acquire_live(size, mspace, thread, previous_epoch);
    if (buffer != NULL) {
      buffer->set_lease();
      DEBUG_ONLY(assert_lease(buffer);)
      return buffer;
    }
  }
  buffer = mspace_allocate_transient_lease_to_live_list(size, mspace, thread, previous_epoch);
  DEBUG_ONLY(assert_lease(buffer);)
  return buffer;
}

const u1 thread_local_context = 1;

static bool is_thread_local(JfrBuffer* buffer) {
  assert(buffer != NULL, "invariant");
  return buffer->context() == thread_local_context;
}

static void retire(JfrBuffer* buffer) {
  DEBUG_ONLY(assert_release(buffer);)
  buffer->clear_lease();
  buffer->set_retired();
}

/*
 * The buffer is effectively invalidated for the thread post-return,
 * and the caller should take means to ensure that it is not referenced.
 */
static void release(JfrBuffer* buffer) {
  DEBUG_ONLY(assert_release(buffer);)
  if (is_thread_local(buffer)) {
    retire(buffer);
  } else {
    buffer->clear_lease();
    buffer->release();
  }
}
BufferPtr JfrCheckpointManager::acquire_thread_local(size_t size, Thread* thread) {
  assert(thread != NULL, "invariant");
  JfrBuffer* const buffer = instance()._thread_local_mspace->acquire(size, thread);
  assert(buffer != NULL, "invariant");
  assert(buffer->free_size() >= size, "invariant");
  buffer->set_context(thread_local_context);
  assert(is_thread_local(buffer), "invariant");
  buffer->set_lease();
  return buffer;
}

BufferPtr JfrCheckpointManager::lease_thread_local(Thread* thread, size_t size /* 0 */) {
  JfrBuffer* const buffer = acquire_thread_local(size, thread);
  DEBUG_ONLY(assert_lease(buffer);)
  return buffer;
}

BufferPtr JfrCheckpointManager::lease(BufferPtr old, Thread* thread, size_t size) {
  assert(old != NULL, "invariant");
  return is_thread_local(old) ? acquire_thread_local(size, thread) :
                                lease(thread, instance()._global_mspace->in_previous_epoch_list(old), size);
}

BufferPtr JfrCheckpointManager::flush(BufferPtr old, size_t used, size_t requested, Thread* thread) {
  assert(old != NULL, "invariant");
  assert(old->lease(), "invariant");
  if (0 == requested) {
    // indicates a lease is being returned
    release(old);
    // signal completion of a new checkpoint
    _new_checkpoint.signal();
    return NULL;
  }
  BufferPtr new_buffer = lease(old, thread, used + requested);
  assert(new_buffer != NULL, "invariant");
  migrate_outstanding_writes(old, new_buffer, used, requested);
  retire(old);
  return new_buffer;
}

// offsets into the JfrCheckpointEntry
static const juint starttime_offset = sizeof(jlong);
static const juint duration_offset = starttime_offset + sizeof(jlong);
static const juint checkpoint_type_offset = duration_offset + sizeof(jlong);
static const juint types_offset = checkpoint_type_offset + sizeof(juint);
static const juint payload_offset = types_offset + sizeof(juint);

template <typename Return>
static Return read_data(const u1* data) {
  return JfrBigEndian::read<Return>(data);
}

static jlong total_size(const u1* data) {
  return read_data<jlong>(data);
}

static jlong starttime(const u1* data) {
  return read_data<jlong>(data + starttime_offset);
}

static jlong duration(const u1* data) {
  return read_data<jlong>(data + duration_offset);
}

static u1 checkpoint_type(const u1* data) {
  return read_data<u1>(data + checkpoint_type_offset);
}

static juint number_of_types(const u1* data) {
  return read_data<juint>(data + types_offset);
}

static void write_checkpoint_header(JfrChunkWriter& cw, int64_t delta_to_last_checkpoint, const u1* data) {
  cw.reserve(sizeof(u4));
  cw.write<u8>(EVENT_CHECKPOINT);
  cw.write(starttime(data));
  cw.write(duration(data));
  cw.write(delta_to_last_checkpoint);
  cw.write(checkpoint_type(data));
  cw.write(number_of_types(data));
}

static void write_checkpoint_content(JfrChunkWriter& cw, const u1* data, size_t size) {
  assert(data != NULL, "invariant");
  cw.write_unbuffered(data + payload_offset, size - sizeof(JfrCheckpointEntry));
}

static size_t write_checkpoint_event(JfrChunkWriter& cw, const u1* data) {
  assert(data != NULL, "invariant");
  const int64_t event_begin = cw.current_offset();
  const int64_t last_checkpoint_event = cw.last_checkpoint_offset();
  const int64_t delta_to_last_checkpoint = last_checkpoint_event == 0 ? 0 : last_checkpoint_event - event_begin;
  const int64_t checkpoint_size = total_size(data);
  write_checkpoint_header(cw, delta_to_last_checkpoint, data);
  write_checkpoint_content(cw, data, checkpoint_size);
  const int64_t event_size = cw.current_offset() - event_begin;
  cw.write_padded_at_offset<u4>(event_size, event_begin);
  cw.set_last_checkpoint_offset(event_begin);
  return (size_t)checkpoint_size;
}

static size_t write_checkpoints(JfrChunkWriter& cw, const u1* data, size_t size) {
  assert(cw.is_valid(), "invariant");
  assert(data != NULL, "invariant");
  assert(size > 0, "invariant");
  const u1* const limit = data + size;
  const u1* next = data;
  size_t processed = 0;
  while (next < limit) {
    const size_t checkpoint_size = write_checkpoint_event(cw, next);
    processed += checkpoint_size;
    next += checkpoint_size;
  }
  assert(next == limit, "invariant");
  return processed;
}

template <typename T>
class CheckpointWriteOp {
 private:
  JfrChunkWriter& _writer;
  size_t _processed;
 public:
  typedef T Type;
  CheckpointWriteOp(JfrChunkWriter& writer) : _writer(writer), _processed(0) {}
  bool write(Type* t, const u1* data, size_t size) {
    _processed += write_checkpoints(_writer, data, size);
    return true;
  }
  size_t processed() const { return _processed; }
};

typedef CheckpointWriteOp<JfrCheckpointManager::Buffer> WriteOperation;
typedef MutexedWriteOp<WriteOperation> MutexedWriteOperation;
typedef ReleaseWithExcisionOp<JfrCheckpointMspace, JfrCheckpointMspace::LiveList> ReleaseOperation;
typedef CompositeOperation<MutexedWriteOperation, ReleaseOperation> WriteReleaseOperation;

void JfrCheckpointManager::begin_epoch_shift() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  JfrTraceIdEpoch::begin_epoch_shift();
}

void JfrCheckpointManager::end_epoch_shift() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  debug_only(const u1 current_epoch = JfrTraceIdEpoch::current();)
  JfrTraceIdEpoch::end_epoch_shift();
  assert(current_epoch != JfrTraceIdEpoch::current(), "invariant");
}

size_t JfrCheckpointManager::write() {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_native(JavaThread::current()));
  WriteOperation wo(_chunkwriter);
  MutexedWriteOperation mwo(wo);
  _thread_local_mspace->iterate(mwo, true); // previous epoch list
  assert(_global_mspace->free_list_is_empty(), "invariant");
  ReleaseOperation ro(_global_mspace, _global_mspace->live_list(true));
  WriteReleaseOperation wro(&mwo, &ro);
  process_live_list(wro, _global_mspace, true); // previous epoch list
  return wo.processed();
}

typedef DiscardOp<DefaultDiscarder<JfrCheckpointManager::Buffer> > DiscardOperation;
typedef CompositeOperation<DiscardOperation, ReleaseOperation> DiscardReleaseOperation;

size_t JfrCheckpointManager::clear() {
  JfrTraceIdLoadBarrier::clear();
  clear_type_set();
  DiscardOperation discard_operation(mutexed); // mutexed discard mode
  _thread_local_mspace->iterate(discard_operation, true); // previous epoch list
  ReleaseOperation ro(_global_mspace, _global_mspace->live_list(true));
  DiscardReleaseOperation discard_op(&discard_operation, &ro);
  assert(_global_mspace->free_list_is_empty(), "invariant");
  process_live_list(discard_op, _global_mspace, true); // previous epoch list
  return discard_operation.elements();
}

size_t JfrCheckpointManager::write_static_type_set(Thread* thread) {
  assert(thread != NULL, "invariant");
  JfrCheckpointWriter writer(true, thread, STATICS);
  JfrTypeManager::write_static_types(writer);
  return writer.used_size();
}

size_t JfrCheckpointManager::write_threads(JavaThread* thread) {
  assert(thread != NULL, "invariant");
  // can safepoint here
  ThreadInVMfromNative transition(thread);
  ResourceMark rm(thread);
  HandleMark hm(thread);
  JfrCheckpointWriter writer(true, thread, THREADS);
  JfrTypeManager::write_threads(writer);
  return writer.used_size();
}

size_t JfrCheckpointManager::write_static_type_set_and_threads() {
  JavaThread* const thread = JavaThread::current();
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_native(thread));
  write_static_type_set(thread);
  write_threads(thread);
  return write();
}

void JfrCheckpointManager::on_rotation() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  JfrTypeManager::on_rotation();
  notify_threads();
}

void JfrCheckpointManager::clear_type_set() {
  assert(!JfrRecorder::is_recording(), "invariant");
  JavaThread* t = JavaThread::current();
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_native(t));
  // can safepoint here
  ThreadInVMfromNative transition(t);
  MutexLocker cld_lock(ClassLoaderDataGraph_lock);
  MutexLocker module_lock(Module_lock);
  JfrTypeSet::clear();
}

void JfrCheckpointManager::write_type_set() {
  {
    JavaThread* const thread = JavaThread::current();
    DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_native(thread));
    // can safepoint here
    ThreadInVMfromNative transition(thread);
    MutexLocker cld_lock(thread, ClassLoaderDataGraph_lock);
    MutexLocker module_lock(thread, Module_lock);
    if (LeakProfiler::is_running()) {
      JfrCheckpointWriter leakp_writer(true, thread);
      JfrCheckpointWriter writer(true, thread);
      JfrTypeSet::serialize(&writer, &leakp_writer, false, false);
      ObjectSampleCheckpoint::on_type_set(leakp_writer);
    } else {
      JfrCheckpointWriter writer(true, thread);
      JfrTypeSet::serialize(&writer, NULL, false, false);
    }
  }
  write();
}

void JfrCheckpointManager::on_unloading_classes() {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  JfrCheckpointWriter writer(Thread::current());
  JfrTypeSet::on_unloading_classes(&writer);
  if (LeakProfiler::is_running()) {
    ObjectSampleCheckpoint::on_type_set_unload(writer);
  }
}

static size_t flush_type_set(Thread* thread) {
  assert(thread != NULL, "invariant");
  JfrCheckpointWriter writer(thread);
  MutexLocker cld_lock(thread, ClassLoaderDataGraph_lock);
  MutexLocker module_lock(thread, Module_lock);
  return JfrTypeSet::serialize(&writer, NULL, false, true);
}

size_t JfrCheckpointManager::flush_type_set() {
  size_t elements = 0;
  if (JfrTraceIdEpoch::has_changed_tag_state()) {
    Thread* const thread = Thread::current();
    if (thread->is_Java_thread()) {
      // can safepoint here
      ThreadInVMfromNative transition(JavaThread::cast(thread));
      elements = ::flush_type_set(thread);
    } else {
      elements = ::flush_type_set(thread);
    }
  }
  if (_new_checkpoint.is_signaled_with_reset()) {
    WriteOperation wo(_chunkwriter);
    MutexedWriteOperation mwo(wo);
    _thread_local_mspace->iterate(mwo); // current epoch list
    assert(_global_mspace->live_list_is_nonempty(), "invariant");
    process_live_list(mwo, _global_mspace); // current epoch list
  }
  return elements;
}

void JfrCheckpointManager::create_thread_blob(Thread* thread) {
  JfrTypeManager::create_thread_blob(thread);
}

void JfrCheckpointManager::write_thread_checkpoint(Thread* thread) {
  JfrTypeManager::write_thread_checkpoint(thread);
}

class JfrNotifyClosure : public ThreadClosure {
 public:
  void do_thread(Thread* thread) {
    assert(thread != NULL, "invariant");
    assert_locked_or_safepoint(Threads_lock);
    JfrJavaEventWriter::notify(JavaThread::cast(thread));
  }
};

void JfrCheckpointManager::notify_threads() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  JfrNotifyClosure tc;
  JfrJavaThreadIterator iter;
  while (iter.has_next()) {
    tc.do_thread(iter.next());
  }
}
