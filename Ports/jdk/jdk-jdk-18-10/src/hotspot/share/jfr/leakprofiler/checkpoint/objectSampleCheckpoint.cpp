/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/chains/edgeStore.hpp"
#include "jfr/leakprofiler/chains/objectSampleMarker.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleCheckpoint.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleWriter.hpp"
#include "jfr/leakprofiler/leakProfiler.hpp"
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "jfr/leakprofiler/sampling/objectSampler.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/recorder/stacktrace/jfrStackTraceRepository.hpp"
#include "jfr/support/jfrKlassUnloading.hpp"
#include "jfr/support/jfrMethodLookup.hpp"
#include "jfr/utilities/jfrHashtable.hpp"
#include "jfr/utilities/jfrPredicate.hpp"
#include "jfr/utilities/jfrRelation.hpp"
#include "memory/resourceArea.inline.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"

const int initial_array_size = 64;

template <typename T>
static GrowableArray<T>* c_heap_allocate_array(int size = initial_array_size) {
  return new (ResourceObj::C_HEAP, mtTracing) GrowableArray<T>(size, mtTracing);
}

static GrowableArray<traceid>* unloaded_thread_id_set = NULL;

class ThreadIdExclusiveAccess : public StackObj {
 private:
  static Semaphore _mutex_semaphore;
 public:
  ThreadIdExclusiveAccess() { _mutex_semaphore.wait(); }
  ~ThreadIdExclusiveAccess() { _mutex_semaphore.signal(); }
};

Semaphore ThreadIdExclusiveAccess::_mutex_semaphore(1);

static bool has_thread_exited(traceid tid) {
  assert(tid != 0, "invariant");
  if (unloaded_thread_id_set == NULL) {
    return false;
  }
  ThreadIdExclusiveAccess lock;
  return JfrPredicate<traceid, compare_traceid>::test(unloaded_thread_id_set, tid);
}

static void add_to_unloaded_thread_set(traceid tid) {
  ThreadIdExclusiveAccess lock;
  if (unloaded_thread_id_set == NULL) {
    unloaded_thread_id_set = c_heap_allocate_array<traceid>();
  }
  JfrMutablePredicate<traceid, compare_traceid>::test(unloaded_thread_id_set, tid);
}

void ObjectSampleCheckpoint::on_thread_exit(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  if (LeakProfiler::is_running()) {
    add_to_unloaded_thread_set(jt->jfr_thread_local()->thread_id());
  }
}

void ObjectSampleCheckpoint::clear() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  if (unloaded_thread_id_set != NULL) {
    delete unloaded_thread_id_set;
    unloaded_thread_id_set = NULL;
  }
  assert(unloaded_thread_id_set == NULL, "invariant");
}

template <typename Processor>
static void do_samples(ObjectSample* sample, const ObjectSample* end, Processor& processor) {
  assert(sample != NULL, "invariant");
  while (sample != end) {
    processor.sample_do(sample);
    sample = sample->next();
  }
}

template <typename Processor>
static void iterate_samples(Processor& processor, bool all = false) {
  ObjectSampler* const sampler = ObjectSampler::sampler();
  assert(sampler != NULL, "invariant");
  ObjectSample* const last = sampler->last();
  assert(last != NULL, "invariant");
  do_samples(last, all ? NULL : sampler->last_resolved(), processor);
}

class SampleMarker {
 private:
  ObjectSampleMarker& _marker;
  jlong _last_sweep;
  int _count;
 public:
  SampleMarker(ObjectSampleMarker& marker, jlong last_sweep) : _marker(marker), _last_sweep(last_sweep), _count(0) {}
  void sample_do(ObjectSample* sample) {
    if (sample->is_alive_and_older_than(_last_sweep)) {
      _marker.mark(sample->object());
      ++_count;
    }
  }
  int count() const {
    return _count;
  }
};

int ObjectSampleCheckpoint::save_mark_words(const ObjectSampler* sampler, ObjectSampleMarker& marker, bool emit_all) {
  assert(sampler != NULL, "invariant");
  if (sampler->last() == NULL) {
    return 0;
  }
  SampleMarker sample_marker(marker, emit_all ? max_jlong : ObjectSampler::last_sweep());
  iterate_samples(sample_marker, true);
  return sample_marker.count();
}

class BlobCache {
  typedef HashTableHost<JfrBlobHandle, traceid, JfrHashtableEntry, BlobCache> BlobTable;
  typedef BlobTable::HashEntry BlobEntry;
 private:
  BlobTable _table;
  traceid _lookup_id;
 public:
  BlobCache(size_t size) : _table(this, size), _lookup_id(0) {}
  JfrBlobHandle get(const ObjectSample* sample);
  void put(const ObjectSample* sample, const JfrBlobHandle& blob);
  // Hash table callbacks
  void on_link(const BlobEntry* entry) const;
  bool on_equals(uintptr_t hash, const BlobEntry* entry) const;
  void on_unlink(BlobEntry* entry) const;
};

JfrBlobHandle BlobCache::get(const ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  _lookup_id = sample->stack_trace_id();
  assert(_lookup_id != 0, "invariant");
  BlobEntry* const entry = _table.lookup_only(sample->stack_trace_hash());
  return entry != NULL ? entry->literal() : JfrBlobHandle();
}

void BlobCache::put(const ObjectSample* sample, const JfrBlobHandle& blob) {
  assert(sample != NULL, "invariant");
  assert(_table.lookup_only(sample->stack_trace_hash()) == NULL, "invariant");
  _lookup_id = sample->stack_trace_id();
  assert(_lookup_id != 0, "invariant");
  _table.put(sample->stack_trace_hash(), blob);
}

inline void BlobCache::on_link(const BlobEntry* entry) const {
  assert(entry != NULL, "invariant");
  assert(entry->id() == 0, "invariant");
  entry->set_id(_lookup_id);
}

inline bool BlobCache::on_equals(uintptr_t hash, const BlobEntry* entry) const {
  assert(entry != NULL, "invariant");
  assert(entry->hash() == hash, "invariant");
  return entry->id() == _lookup_id;
}

inline void BlobCache::on_unlink(BlobEntry* entry) const {
  assert(entry != NULL, "invariant");
}

static GrowableArray<traceid>* id_set = NULL;

static void prepare_for_resolution() {
  id_set = new GrowableArray<traceid>(JfrOptionSet::old_object_queue_size());
}

static bool stack_trace_precondition(const ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  return sample->has_stack_trace_id() && !sample->is_dead();
}

class StackTraceBlobInstaller {
 private:
  BlobCache _cache;
  void install(ObjectSample* sample);
  const JfrStackTrace* resolve(const ObjectSample* sample) const;
 public:
  StackTraceBlobInstaller() : _cache(JfrOptionSet::old_object_queue_size()) {
    prepare_for_resolution();
  }
  ~StackTraceBlobInstaller() {
    JfrStackTraceRepository::clear_leak_profiler();
  }
  void sample_do(ObjectSample* sample) {
    if (stack_trace_precondition(sample)) {
      install(sample);
    }
  }
};

#ifdef ASSERT
static void validate_stack_trace(const ObjectSample* sample, const JfrStackTrace* stack_trace) {
  assert(!sample->has_stacktrace(), "invariant");
  assert(stack_trace != NULL, "invariant");
  assert(stack_trace->hash() == sample->stack_trace_hash(), "invariant");
  assert(stack_trace->id() == sample->stack_trace_id(), "invariant");
}
#endif

inline const JfrStackTrace* StackTraceBlobInstaller::resolve(const ObjectSample* sample) const {
  return JfrStackTraceRepository::lookup_for_leak_profiler(sample->stack_trace_hash(), sample->stack_trace_id());
}

void StackTraceBlobInstaller::install(ObjectSample* sample) {
  JfrBlobHandle blob = _cache.get(sample);
  if (blob.valid()) {
    sample->set_stacktrace(blob);
    return;
  }
  const JfrStackTrace* const stack_trace = resolve(sample);
  DEBUG_ONLY(validate_stack_trace(sample, stack_trace));
  JfrCheckpointWriter writer;
  writer.write_type(TYPE_STACKTRACE);
  writer.write_count(1);
  ObjectSampleCheckpoint::write_stacktrace(stack_trace, writer);
  blob = writer.copy();
  _cache.put(sample, blob);
  sample->set_stacktrace(blob);
}

static void install_stack_traces(const ObjectSampler* sampler) {
  assert(sampler != NULL, "invariant");
  const ObjectSample* const last = sampler->last();
  if (last != sampler->last_resolved()) {
    ResourceMark rm;
    JfrKlassUnloading::sort();
    StackTraceBlobInstaller installer;
    iterate_samples(installer);
  }
}

void ObjectSampleCheckpoint::on_rotation(const ObjectSampler* sampler) {
  assert(sampler != NULL, "invariant");
  assert(LeakProfiler::is_running(), "invariant");
  JavaThread* const thread = JavaThread::current();
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_native(thread);)
  // can safepoint here
  ThreadInVMfromNative transition(thread);
  MutexLocker lock(ClassLoaderDataGraph_lock);
  // the lock is needed to ensure the unload lists do not grow in the middle of inspection.
  install_stack_traces(sampler);
}

static bool is_klass_unloaded(traceid klass_id) {
  assert(ClassLoaderDataGraph_lock->owned_by_self(), "invariant");
  return JfrKlassUnloading::is_unloaded(klass_id);
}

static bool is_processed(traceid method_id) {
  assert(method_id != 0, "invariant");
  assert(id_set != NULL, "invariant");
  return JfrMutablePredicate<traceid, compare_traceid>::test(id_set, method_id);
}

void ObjectSampleCheckpoint::add_to_leakp_set(const InstanceKlass* ik, traceid method_id) {
  assert(ik != NULL, "invariant");
  if (is_processed(method_id) || is_klass_unloaded(JfrMethodLookup::klass_id(method_id))) {
    return;
  }
  const Method* const method = JfrMethodLookup::lookup(ik, method_id);
  assert(method != NULL, "invariant");
  assert(method->method_holder() == ik, "invariant");
  JfrTraceId::load_leakp(ik, method);
}

void ObjectSampleCheckpoint::write_stacktrace(const JfrStackTrace* trace, JfrCheckpointWriter& writer) {
  assert(trace != NULL, "invariant");
  // JfrStackTrace
  writer.write(trace->id());
  writer.write((u1)!trace->_reached_root);
  writer.write(trace->_nr_of_frames);
  // JfrStackFrames
  for (u4 i = 0; i < trace->_nr_of_frames; ++i) {
    const JfrStackFrame& frame = trace->_frames[i];
    frame.write(writer);
    add_to_leakp_set(frame._klass, frame._methodid);
  }
}

static void write_blob(const JfrBlobHandle& blob, JfrCheckpointWriter& writer, bool reset) {
  if (reset) {
    blob->reset_write_state();
    return;
  }
  blob->exclusive_write(writer);
}

static void write_type_set_blob(const ObjectSample* sample, JfrCheckpointWriter& writer, bool reset) {
  if (sample->has_type_set()) {
    write_blob(sample->type_set(), writer, reset);
  }
}

static void write_thread_blob(const ObjectSample* sample, JfrCheckpointWriter& writer, bool reset) {
  assert(sample->has_thread(), "invariant");
  if (has_thread_exited(sample->thread_id())) {
    write_blob(sample->thread(), writer, reset);
  }
}

static void write_stacktrace_blob(const ObjectSample* sample, JfrCheckpointWriter& writer, bool reset) {
  if (sample->has_stacktrace()) {
    write_blob(sample->stacktrace(), writer, reset);
  }
}

static void write_blobs(const ObjectSample* sample, JfrCheckpointWriter& writer, bool reset) {
  assert(sample != NULL, "invariant");
  write_stacktrace_blob(sample, writer, reset);
  write_thread_blob(sample, writer, reset);
  write_type_set_blob(sample, writer, reset);
}

class BlobWriter {
 private:
  const ObjectSampler* _sampler;
  JfrCheckpointWriter& _writer;
  const jlong _last_sweep;
  bool _reset;
 public:
  BlobWriter(const ObjectSampler* sampler, JfrCheckpointWriter& writer, jlong last_sweep) :
    _sampler(sampler), _writer(writer), _last_sweep(last_sweep), _reset(false)  {}
  void sample_do(ObjectSample* sample) {
    if (sample->is_alive_and_older_than(_last_sweep)) {
      write_blobs(sample, _writer, _reset);
    }
  }
  void set_reset() {
    _reset = true;
  }
};

static void write_sample_blobs(const ObjectSampler* sampler, bool emit_all, Thread* thread) {
  // sample set is predicated on time of last sweep
  const jlong last_sweep = emit_all ? max_jlong : ObjectSampler::last_sweep();
  JfrCheckpointWriter writer(thread, false);
  BlobWriter cbw(sampler, writer, last_sweep);
  iterate_samples(cbw, true);
  // reset blob write states
  cbw.set_reset();
  iterate_samples(cbw, true);
}

void ObjectSampleCheckpoint::write(const ObjectSampler* sampler, EdgeStore* edge_store, bool emit_all, Thread* thread) {
  assert(sampler != NULL, "invariant");
  assert(edge_store != NULL, "invariant");
  assert(thread != NULL, "invariant");
  write_sample_blobs(sampler, emit_all, thread);
  // write reference chains
  if (!edge_store->is_empty()) {
    JfrCheckpointWriter writer(thread);
    ObjectSampleWriter osw(writer, edge_store);
    edge_store->iterate(osw);
  }
}

// A linked list of saved type set blobs for the epoch.
// The link consist of a reference counted handle.
static JfrBlobHandle saved_type_set_blobs;

static void release_state_for_previous_epoch() {
  // decrements the reference count and the list is reinitialized
  saved_type_set_blobs = JfrBlobHandle();
}

class BlobInstaller {
 public:
  ~BlobInstaller() {
    release_state_for_previous_epoch();
  }
  void sample_do(ObjectSample* sample) {
    if (!sample->is_dead()) {
      sample->set_type_set(saved_type_set_blobs);
    }
  }
};

static void install_type_set_blobs() {
  BlobInstaller installer;
  iterate_samples(installer);
}

static void save_type_set_blob(JfrCheckpointWriter& writer, bool copy = false) {
  assert(writer.has_data(), "invariant");
  const JfrBlobHandle blob = copy ? writer.copy() : writer.move();
  if (saved_type_set_blobs.valid()) {
    saved_type_set_blobs->set_next(blob);
  } else {
    saved_type_set_blobs = blob;
  }
}

void ObjectSampleCheckpoint::on_type_set(JfrCheckpointWriter& writer) {
  assert(LeakProfiler::is_running(), "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(JavaThread::current());)
  const ObjectSample* last = ObjectSampler::sampler()->last();
  if (writer.has_data() && last != NULL) {
    save_type_set_blob(writer);
    install_type_set_blobs();
    ObjectSampler::sampler()->set_last_resolved(last);
  }
}

void ObjectSampleCheckpoint::on_type_set_unload(JfrCheckpointWriter& writer) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  assert(LeakProfiler::is_running(), "invariant");
  if (writer.has_data() && ObjectSampler::sampler()->last() != NULL) {
    save_type_set_blob(writer, true);
  }
}
