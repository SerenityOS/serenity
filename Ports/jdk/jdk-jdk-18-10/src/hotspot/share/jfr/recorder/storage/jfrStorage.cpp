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
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointManager.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/recorder/service/jfrOptionSet.hpp"
#include "jfr/recorder/service/jfrPostBox.hpp"
#include "jfr/recorder/storage/jfrFullStorage.inline.hpp"
#include "jfr/recorder/storage/jfrMemorySpace.inline.hpp"
#include "jfr/recorder/storage/jfrStorage.hpp"
#include "jfr/recorder/storage/jfrStorageControl.hpp"
#include "jfr/recorder/storage/jfrStorageUtils.inline.hpp"
#include "jfr/utilities/jfrIterator.hpp"
#include "jfr/utilities/jfrLinkedList.inline.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "jfr/writers/jfrNativeEventWriter.hpp"
#include "logging/log.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"

typedef JfrStorage::BufferPtr BufferPtr;

static JfrStorage* _instance = NULL;
static JfrStorageControl* _control;

JfrStorage& JfrStorage::instance() {
  return *_instance;
}

JfrStorage* JfrStorage::create(JfrChunkWriter& chunkwriter, JfrPostBox& post_box) {
  assert(_instance == NULL, "invariant");
  _instance = new JfrStorage(chunkwriter, post_box);
  return _instance;
}

void JfrStorage::destroy() {
  if (_instance != NULL) {
    delete _instance;
    _instance = NULL;
  }
}

JfrStorage::JfrStorage(JfrChunkWriter& chunkwriter, JfrPostBox& post_box) :
  _control(NULL),
  _global_mspace(NULL),
  _thread_local_mspace(NULL),
  _chunkwriter(chunkwriter),
  _post_box(post_box) {}

JfrStorage::~JfrStorage() {
  if (_control != NULL) {
    delete _control;
  }
  if (_global_mspace != NULL) {
    delete _global_mspace;
  }
  if (_thread_local_mspace != NULL) {
    delete _thread_local_mspace;
  }
  if (_full_list != NULL) {
    delete _full_list;
  }
  _instance = NULL;
}

static const size_t thread_local_cache_count = 8;
// start to discard data when the only this number of free buffers are left
static const size_t in_memory_discard_threshold_delta = 2;

bool JfrStorage::initialize() {
  assert(_control == NULL, "invariant");
  assert(_global_mspace == NULL, "invariant");
  assert(_thread_local_mspace == NULL, "invariant");

  const size_t num_global_buffers = (size_t)JfrOptionSet::num_global_buffers();
  assert(num_global_buffers >= in_memory_discard_threshold_delta, "invariant");
  const size_t global_buffer_size = (size_t)JfrOptionSet::global_buffer_size();
  const size_t thread_buffer_size = (size_t)JfrOptionSet::thread_buffer_size();

  _control = new JfrStorageControl(num_global_buffers, num_global_buffers - in_memory_discard_threshold_delta);
  if (_control == NULL) {
    return false;
  }
  _global_mspace = create_mspace<JfrStorageMspace>(global_buffer_size,
                                                   num_global_buffers, // cache count limit
                                                   num_global_buffers, // cache_preallocate count
                                                   false, // preallocate_to_free_list (== preallocate directly to live list)
                                                   this);
  if (_global_mspace == NULL) {
    return false;
  }
  assert(_global_mspace->live_list_is_nonempty(), "invariant");
  _thread_local_mspace = create_mspace<JfrThreadLocalMspace>(thread_buffer_size,
                                                             thread_local_cache_count, // cache count limit
                                                             thread_local_cache_count, // cache preallocate count
                                                             true,  // preallocate_to_free_list
                                                             this);
  if (_thread_local_mspace == NULL) {
    return false;
  }
  assert(_thread_local_mspace->free_list_is_nonempty(), "invariant");
  // The full list will contain nodes pointing to retired global and transient buffers.
  _full_list = new JfrFullList(*_control);
  return _full_list != NULL && _full_list->initialize(num_global_buffers * 2);
}

JfrStorageControl& JfrStorage::control() {
  return *instance()._control;
}

static void log_allocation_failure(const char* msg, size_t size) {
  log_warning(jfr)("Unable to allocate " SIZE_FORMAT " bytes of %s.", size, msg);
}

BufferPtr JfrStorage::acquire_thread_local(Thread* thread, size_t size /* 0 */) {
  BufferPtr buffer = mspace_acquire_to_live_list(size, instance()._thread_local_mspace, thread);
  if (buffer == NULL) {
    log_allocation_failure("thread local_memory", size);
    return NULL;
  }
  assert(buffer->acquired_by_self(), "invariant");
  return buffer;
}

BufferPtr JfrStorage::acquire_transient(size_t size, Thread* thread) {
  BufferPtr buffer = mspace_allocate_transient_lease(size, instance()._thread_local_mspace, thread);
  if (buffer == NULL) {
    log_allocation_failure("transient memory", size);
    return NULL;
  }
  assert(buffer->acquired_by_self(), "invariant");
  assert(buffer->transient(), "invariant");
  assert(buffer->lease(), "invariant");
  return buffer;
}

static BufferPtr acquire_lease(size_t size, JfrStorageMspace* mspace, JfrStorage& storage_instance, size_t retry_count, Thread* thread) {
  assert(size <= mspace->min_element_size(), "invariant");
  while (true) {
    BufferPtr buffer = mspace_acquire_lease_with_retry(size, mspace, retry_count, thread);
    if (buffer == NULL && storage_instance.control().should_discard()) {
      storage_instance.discard_oldest(thread);
      continue;
    }
    return buffer;
  }
}

static BufferPtr acquire_promotion_buffer(size_t size, JfrStorageMspace* mspace, JfrStorage& storage_instance, size_t retry_count, Thread* thread) {
  assert(size <= mspace->min_element_size(), "invariant");
  while (true) {
    BufferPtr buffer= mspace_acquire_live_with_retry(size, mspace, retry_count, thread);
    if (buffer == NULL && storage_instance.control().should_discard()) {
      storage_instance.discard_oldest(thread);
      continue;
    }
    return buffer;
  }
}

static const size_t lease_retry = 10;

BufferPtr JfrStorage::acquire_large(size_t size, Thread* thread) {
  JfrStorage& storage_instance = instance();
  const size_t max_elem_size = storage_instance._global_mspace->min_element_size(); // min is also max
  // if not too large and capacity is still available, ask for a lease from the global system
  if (size < max_elem_size && storage_instance.control().is_global_lease_allowed()) {
    BufferPtr const buffer = acquire_lease(size, storage_instance._global_mspace, storage_instance, lease_retry, thread);
    if (buffer != NULL) {
      assert(buffer->acquired_by_self(), "invariant");
      assert(!buffer->transient(), "invariant");
      assert(buffer->lease(), "invariant");
      storage_instance.control().increment_leased();
      return buffer;
    }
  }
  return acquire_transient(size, thread);
}

static void write_data_loss_event(JfrBuffer* buffer, u8 unflushed_size, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(buffer->empty(), "invariant");
  const u8 total_data_loss = thread->jfr_thread_local()->add_data_lost(unflushed_size);
  if (EventDataLoss::is_enabled()) {
    JfrNativeEventWriter writer(buffer, thread);
    writer.begin_event_write(false);
    writer.write<u8>(EventDataLoss::eventId);
    writer.write(JfrTicks::now());
    writer.write(unflushed_size);
    writer.write(total_data_loss);
    writer.end_event_write(false);
  }
}

static void write_data_loss(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  const size_t unflushed_size = buffer->unflushed_size();
  buffer->reinitialize();
  if (unflushed_size == 0) {
    return;
  }
  write_data_loss_event(buffer, unflushed_size, thread);
}

static const size_t promotion_retry = 100;

bool JfrStorage::flush_regular_buffer(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(!buffer->lease(), "invariant");
  assert(!buffer->transient(), "invariant");
  const size_t unflushed_size = buffer->unflushed_size();
  if (unflushed_size == 0) {
    buffer->reinitialize();
    assert(buffer->empty(), "invariant");
    return true;
  }

  if (buffer->excluded()) {
    const bool thread_is_excluded = thread->jfr_thread_local()->is_excluded();
    buffer->reinitialize(thread_is_excluded);
    assert(buffer->empty(), "invariant");
    if (!thread_is_excluded) {
      // state change from exclusion to inclusion requires a thread checkpoint
      JfrCheckpointManager::write_thread_checkpoint(thread);
    }
    return true;
  }

  BufferPtr const promotion_buffer = acquire_promotion_buffer(unflushed_size, _global_mspace, *this, promotion_retry, thread);
  if (promotion_buffer == NULL) {
    write_data_loss(buffer, thread);
    return false;
  }
  assert(promotion_buffer->acquired_by_self(), "invariant");
  assert(promotion_buffer->free_size() >= unflushed_size, "invariant");
  buffer->move(promotion_buffer, unflushed_size);
  assert(buffer->empty(), "invariant");
  return true;
}

/*
* 1. If the buffer was a "lease" from the global system, release back.
* 2. If the buffer is transient (temporal dynamically allocated), retire and register full.
*
* The buffer is effectively invalidated for the thread post-return,
* and the caller should take means to ensure that it is not referenced any longer.
*/
void JfrStorage::release_large(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(buffer->lease(), "invariant");
  assert(buffer->acquired_by_self(), "invariant");
  buffer->clear_lease();
  if (buffer->transient()) {
    buffer->set_retired();
    register_full(buffer, thread);
  } else {
    buffer->release();
    control().decrement_leased();
  }
}

void JfrStorage::register_full(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(buffer->acquired_by(thread), "invariant");
  assert(buffer->retired(), "invariant");
  if (_full_list->add(buffer)) {
    _post_box.post(MSG_FULLBUFFER);
  }
}

// don't use buffer on return, it is gone
void JfrStorage::release(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  assert(!buffer->lease(), "invariant");
  assert(!buffer->transient(), "invariant");
  assert(!buffer->retired(), "invariant");
  if (!buffer->empty()) {
    if (!flush_regular_buffer(buffer, thread)) {
      buffer->reinitialize();
    }
  }
  assert(buffer->empty(), "invariant");
  assert(buffer->identity() != NULL, "invariant");
  buffer->clear_excluded();
  buffer->set_retired();
}

void JfrStorage::release_thread_local(BufferPtr buffer, Thread* thread) {
  assert(buffer != NULL, "invariant");
  JfrStorage& storage_instance = instance();
  storage_instance.release(buffer, thread);
}

static void log_discard(size_t pre_full_count, size_t post_full_count, size_t amount) {
  if (log_is_enabled(Debug, jfr, system)) {
    const size_t number_of_discards = pre_full_count - post_full_count;
    if (number_of_discards > 0) {
      log_debug(jfr, system)("Cleared " SIZE_FORMAT " full buffer(s) of " SIZE_FORMAT" bytes.", number_of_discards, amount);
      log_debug(jfr, system)("Current number of full buffers " SIZE_FORMAT "", number_of_discards);
    }
  }
}

void JfrStorage::discard_oldest(Thread* thread) {
  if (JfrBuffer_lock->try_lock()) {
    if (!control().should_discard()) {
      // another thread handled it
      return;
    }
    const size_t num_full_pre_discard = control().full_count();
    size_t discarded_size = 0;
    while (_full_list->is_nonempty()) {
      BufferPtr oldest = _full_list->remove();
      assert(oldest != NULL, "invariant");
      assert(oldest->identity() != NULL, "invariant");
      discarded_size += oldest->discard();
      assert(oldest->unflushed_size() == 0, "invariant");
      if (oldest->transient()) {
        mspace_release(oldest, _thread_local_mspace);
        continue;
      }
      oldest->reinitialize();
      assert(!oldest->retired(), "invariant");
      oldest->release(); // publish
      break;
    }
    JfrBuffer_lock->unlock();
    log_discard(num_full_pre_discard, control().full_count(), discarded_size);
  }
}

#ifdef ASSERT
typedef const BufferPtr ConstBufferPtr;

static void assert_flush_precondition(ConstBufferPtr cur, size_t used, bool native, const Thread* t) {
  assert(t != NULL, "invariant");
  assert(cur != NULL, "invariant");
  assert(cur->pos() + used <= cur->end(), "invariant");
  assert(native ? t->jfr_thread_local()->native_buffer() == cur : t->jfr_thread_local()->java_buffer() == cur, "invariant");
}

static void assert_flush_regular_precondition(ConstBufferPtr cur, const u1* const cur_pos, size_t used, size_t req, const Thread* t) {
  assert(t != NULL, "invariant");
  assert(cur != NULL, "invariant");
  assert(!cur->lease(), "invariant");
  assert(cur_pos != NULL, "invariant");
  assert(req >= used, "invariant");
}

static void assert_provision_large_precondition(ConstBufferPtr cur, size_t used, size_t req, const Thread* t) {
  assert(cur != NULL, "invariant");
  assert(t != NULL, "invariant");
  assert(t->jfr_thread_local()->shelved_buffer() != NULL, "invariant");
  assert(req >= used, "invariant");
}

static void assert_flush_large_precondition(ConstBufferPtr cur, const u1* const cur_pos, size_t used, size_t req, bool native, Thread* t) {
  assert(t != NULL, "invariant");
  assert(cur != NULL, "invariant");
  assert(cur->lease(), "invariant");
  assert(!cur->excluded(), "invariant");
  assert(cur_pos != NULL, "invariant");
  assert(native ? t->jfr_thread_local()->native_buffer() == cur : t->jfr_thread_local()->java_buffer() == cur, "invariant");
  assert(t->jfr_thread_local()->shelved_buffer() != NULL, "invariant");
  assert(req >= used, "invariant");
  assert(cur != t->jfr_thread_local()->shelved_buffer(), "invariant");
}
#endif // ASSERT

BufferPtr JfrStorage::flush(BufferPtr cur, size_t used, size_t req, bool native, Thread* t) {
  debug_only(assert_flush_precondition(cur, used, native, t);)
  const u1* const cur_pos = cur->pos();
  req += used;
  // requested size now encompass the outstanding used size
  return cur->lease() ? instance().flush_large(cur, cur_pos, used, req, native, t) :
                          instance().flush_regular(cur, cur_pos, used, req, native, t);
}

BufferPtr JfrStorage::flush_regular(BufferPtr cur, const u1* const cur_pos, size_t used, size_t req, bool native, Thread* t) {
  debug_only(assert_flush_regular_precondition(cur, cur_pos, used, req, t);)
  // A flush is needed before memmove since a non-large buffer is thread stable
  // (thread local). The flush will not modify memory in addresses above pos()
  // which is where the "used / uncommitted" data resides. It is therefore both
  // possible and valid to migrate data after the flush. This is however only
  // the case for stable thread local buffers; it is not the case for large buffers.
  flush_regular_buffer(cur, t);
  if (cur->excluded()) {
    return cur;
  }
  if (cur->free_size() >= req) {
    // simplest case, no switching of buffers
    if (used > 0) {
      // source and destination may overlap so memmove must be used instead of memcpy
      memmove(cur->pos(), (void*)cur_pos, used);
    }
    assert(native ? t->jfr_thread_local()->native_buffer() == cur : t->jfr_thread_local()->java_buffer() == cur, "invariant");
    return cur;
  }
  // Going for a "larger-than-regular" buffer.
  // Shelve the current buffer to make room for a temporary lease.
  assert(t->jfr_thread_local()->shelved_buffer() == NULL, "invariant");
  t->jfr_thread_local()->shelve_buffer(cur);
  return provision_large(cur, cur_pos, used, req, native, t);
}

static BufferPtr store_buffer_to_thread_local(BufferPtr buffer, JfrThreadLocal* jfr_thread_local, bool native) {
  assert(buffer != NULL, "invariant");
  if (native) {
    jfr_thread_local->set_native_buffer(buffer);
  } else {
    jfr_thread_local->set_java_buffer(buffer);
  }
  return buffer;
}

static BufferPtr restore_shelved_buffer(bool native, Thread* t) {
  JfrThreadLocal* const tl = t->jfr_thread_local();
  BufferPtr shelved = tl->shelved_buffer();
  assert(shelved != NULL, "invariant");
  tl->shelve_buffer(NULL);
  // restore shelved buffer back as primary
  return store_buffer_to_thread_local(shelved, tl, native);
}

BufferPtr JfrStorage::flush_large(BufferPtr cur, const u1* const cur_pos, size_t used, size_t req, bool native, Thread* t) {
  debug_only(assert_flush_large_precondition(cur, cur_pos, used, req, native, t);)
  // Can the "regular" buffer (now shelved) accommodate the requested size?
  BufferPtr shelved = t->jfr_thread_local()->shelved_buffer();
  assert(shelved != NULL, "invariant");
  if (shelved->free_size() >= req) {
    if (req > 0) {
      memcpy(shelved->pos(), (void*)cur_pos, (size_t)used);
    }
    // release and invalidate
    release_large(cur, t);
    return restore_shelved_buffer(native, t);
  }
  // regular too small
  return provision_large(cur, cur_pos,  used, req, native, t);
}

static BufferPtr large_fail(BufferPtr cur, bool native, JfrStorage& storage_instance, Thread* t) {
  assert(cur != NULL, "invariant");
  assert(t != NULL, "invariant");
  if (cur->lease()) {
    storage_instance.release_large(cur, t);
  }
  return restore_shelved_buffer(native, t);
}

// Always returns a non-null buffer.
// If accommodating the large request fails, the shelved buffer is returned
// even though it might be smaller than the requested size.
// Caller needs to ensure if the size was successfully accommodated.
BufferPtr JfrStorage::provision_large(BufferPtr cur, const u1* const cur_pos, size_t used, size_t req, bool native, Thread* t) {
  debug_only(assert_provision_large_precondition(cur, used, req, t);)
  assert(t->jfr_thread_local()->shelved_buffer() != NULL, "invariant");
  BufferPtr const buffer = acquire_large(req, t);
  if (buffer == NULL) {
    // unable to allocate and serve the request
    return large_fail(cur, native, *this, t);
  }
  // ok managed to acquire a "large" buffer for the requested size
  assert(buffer->free_size() >= req, "invariant");
  assert(buffer->lease(), "invariant");
  // transfer outstanding data
  memcpy(buffer->pos(), (void*)cur_pos, used);
  if (cur->lease()) {
    release_large(cur, t);
    // don't use current anymore, it is gone
  }
  return store_buffer_to_thread_local(buffer, t->jfr_thread_local(), native);
}

typedef UnBufferedWriteToChunk<JfrBuffer> WriteOperation;
typedef MutexedWriteOp<WriteOperation> MutexedWriteOperation;
typedef ConcurrentWriteOp<WriteOperation> ConcurrentWriteOperation;

typedef Excluded<JfrBuffer, true> NonExcluded;
typedef PredicatedConcurrentWriteOp<WriteOperation, NonExcluded>  ConcurrentNonExcludedWriteOperation;

typedef ScavengingReleaseOp<JfrThreadLocalMspace, JfrThreadLocalMspace::LiveList> ReleaseThreadLocalOperation;
typedef CompositeOperation<ConcurrentNonExcludedWriteOperation, ReleaseThreadLocalOperation> ConcurrentWriteReleaseThreadLocalOperation;

size_t JfrStorage::write() {
  const size_t full_elements = write_full();
  WriteOperation wo(_chunkwriter);
  NonExcluded ne;
  ConcurrentNonExcludedWriteOperation cnewo(wo, ne);
  ReleaseThreadLocalOperation rtlo(_thread_local_mspace, _thread_local_mspace->live_list());
  ConcurrentWriteReleaseThreadLocalOperation tlop(&cnewo, &rtlo);
  process_live_list(tlop, _thread_local_mspace);
  assert(_global_mspace->free_list_is_empty(), "invariant");
  assert(_global_mspace->live_list_is_nonempty(), "invariant");
  process_live_list(cnewo, _global_mspace);
  return full_elements + wo.elements();
}

size_t JfrStorage::write_at_safepoint() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  const size_t full_elements = write_full();
  WriteOperation wo(_chunkwriter);
  NonExcluded ne;
  ConcurrentNonExcludedWriteOperation cnewo(wo, ne); // concurrent because of gc's
  process_live_list(cnewo, _thread_local_mspace);
  assert(_global_mspace->free_list_is_empty(), "invariant");
  assert(_global_mspace->live_list_is_nonempty(), "invariant");
  process_live_list(cnewo, _global_mspace);
  return full_elements + wo.elements();
}

typedef DiscardOp<DefaultDiscarder<JfrStorage::Buffer> > DiscardOperation;
typedef CompositeOperation<DiscardOperation, ReleaseThreadLocalOperation> DiscardReleaseThreadLocalOperation;

size_t JfrStorage::clear() {
  const size_t full_elements = clear_full();
  DiscardOperation discarder(concurrent); // concurrent discard mode
  ReleaseThreadLocalOperation rtlo(_thread_local_mspace, _thread_local_mspace->live_list());
  DiscardReleaseThreadLocalOperation tldo(&discarder, &rtlo);
  process_live_list(tldo, _thread_local_mspace);
  assert(_global_mspace->free_list_is_empty(), "invariant");
  assert(_global_mspace->live_list_is_nonempty(), "invariant");
  process_live_list(discarder, _global_mspace);
  return full_elements + discarder.elements();
}

template <typename Processor>
static size_t process_full(Processor& processor, JfrFullList* list, JfrStorageControl& control) {
  assert(list != NULL, "invariant");
  assert(list->is_nonempty(), "invariant");
  size_t count = 0;
  do {
    BufferPtr full = list->remove();
    if (full == NULL) break;
    assert(full->retired(), "invariant");
    processor.process(full);
    // at this point, the buffer is already live or destroyed
    ++count;
  } while (list->is_nonempty());
  return count;
}

static void log(size_t count, size_t amount, bool clear = false) {
  if (log_is_enabled(Debug, jfr, system)) {
    if (count > 0) {
      log_debug(jfr, system)("%s " SIZE_FORMAT " full buffer(s) of " SIZE_FORMAT" B of data%s",
        clear ? "Discarded" : "Wrote", count, amount, clear ? "." : " to chunk.");
    }
  }
}

typedef ReleaseOp<JfrThreadLocalMspace> ReleaseFullOperation;
typedef CompositeOperation<MutexedWriteOperation, ReleaseFullOperation> WriteFullOperation;

// full writer
// Assumption is retired only; exclusive access
// MutexedWriter -> ReleaseOp
//
size_t JfrStorage::write_full() {
  assert(_chunkwriter.is_valid(), "invariant");
  if (_full_list->is_empty()) {
    return 0;
  }
  WriteOperation wo(_chunkwriter);
  MutexedWriteOperation writer(wo); // a retired buffer implies mutexed access
  ReleaseFullOperation rfo(_thread_local_mspace);
  WriteFullOperation wfo(&writer, &rfo);
  const size_t count = process_full(wfo, _full_list, control());
  if (count != 0) {
    log(count, writer.size());
  }
  return count;
}

size_t JfrStorage::clear_full() {
  if (_full_list->is_empty()) {
    return 0;
  }
  DiscardOperation discarder(mutexed); // a retired buffer implies mutexed access
  const size_t count = process_full(discarder, _full_list, control());
  if (count != 0) {
    log(count, discarder.size());
  }
  return count;
}
