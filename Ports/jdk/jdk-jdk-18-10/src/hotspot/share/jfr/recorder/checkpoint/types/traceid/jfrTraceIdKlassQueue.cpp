/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdKlassQueue.hpp"
#include "jfr/support/jfrKlassUnloading.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jfr/utilities/jfrEpochQueue.inline.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/metaspace.hpp"
#include "oops/compressedOops.hpp"
#include "utilities/macros.hpp"

#ifdef VM_LITTLE_ENDIAN
static const u1      UNLOADED_BIT = 1;
static const u1      UNCOMPRESSED_BIT = 1 << 1;
static const u1      METADATA_SHIFT = UNCOMPRESSED_BIT;
static const traceid UNLOADED = UNLOADED_BIT;
static const traceid UNCOMPRESSED = UNCOMPRESSED_BIT;
static const juint   UNLOADED_NARROW = UNLOADED_BIT;
static const juint   UNCOMPRESSED_NARROW = UNCOMPRESSED_BIT;
#else
static const u1      UNLOADED_BIT = 1 << 7;
static const u1      UNCOMPRESSED_BIT = 1 << 6;
static const traceid UNLOADED = (traceid)UNLOADED_BIT << 56;
static const traceid UNCOMPRESSED = (traceid)UNCOMPRESSED_BIT << 56;
static const traceid METADATA_MASK = ~(UNCOMPRESSED | UNLOADED);
static const juint   UNLOADED_NARROW = (juint)UNLOADED_BIT << 24;
static const juint   UNCOMPRESSED_NARROW = (juint)UNCOMPRESSED_BIT << 24;
static const juint   METADATA_MASK_NARROW = ~(UNCOMPRESSED_NARROW | UNLOADED_NARROW);
#endif

struct JfrEpochQueueKlassElement {
  traceid id;
  const Klass* klass;
};

struct JfrEpochQueueNarrowKlassElement {
  u4 id;
  narrowKlass compressed_klass;
};

static const size_t ELEMENT_SIZE = sizeof(JfrEpochQueueKlassElement);
static const size_t NARROW_ELEMENT_SIZE = sizeof(JfrEpochQueueNarrowKlassElement);
static const size_t THRESHOLD_SHIFT = 30;

// If the traceid value is less than this threshold (1 073 741 824),
// compress the element for more effective queue storage.
static const traceid uncompressed_threshold = ((traceid)1) << THRESHOLD_SHIFT;

static size_t element_size(bool compressed) {
  return compressed ? NARROW_ELEMENT_SIZE : ELEMENT_SIZE;
}

static bool can_compress_element(traceid id) {
  return Metaspace::using_class_space() && id < uncompressed_threshold;
}

static size_t element_size(const Klass* klass) {
  assert(klass != NULL, "invariant");
  return element_size(can_compress_element(JfrTraceId::load_raw(klass)));
}

static bool is_unloaded(traceid id, bool previous_epoch) {
  return JfrKlassUnloading::is_unloaded(id, previous_epoch);
}

static narrowKlass encode(const Klass* klass) {
  return CompressedKlassPointers::encode(const_cast<Klass*>(klass));
}

static const Klass* decode(narrowKlass klass) {
  return CompressedKlassPointers::decode(klass);
}

static traceid unmask_id(traceid id, bool compressed) {
#ifdef VM_LITTLE_ENDIAN
  return id >> METADATA_SHIFT;
#else
  return compressed ? id & METADATA_MASK_NARROW : id & METADATA_MASK;
#endif
}

static traceid read_compressed_element(const u1* pos, const Klass** klass) {
  const JfrEpochQueueNarrowKlassElement* element = (const JfrEpochQueueNarrowKlassElement*)pos;
  *klass = decode(element->compressed_klass);
  return unmask_id(element->id, true);
}

static traceid read_uncompressed_element(const u1* pos, const Klass** klass) {
  const JfrEpochQueueKlassElement* element = (const JfrEpochQueueKlassElement*)pos;
  *klass = element->klass;
  return unmask_id(element->id, false);
}

static traceid read_element(const u1* pos, const Klass** klass, bool compressed) {
  assert(pos != NULL, "invariant");
  return compressed ? read_compressed_element(pos, klass) : read_uncompressed_element(pos, klass);
}

template <typename T>
static inline void store_traceid(T* element, traceid id, bool uncompressed) {
#ifdef VM_LITTLE_ENDIAN
  id <<= METADATA_SHIFT;
#endif
  element->id = uncompressed ? id | UNCOMPRESSED : id;
}

static void store_compressed_element(traceid id, const Klass* klass, u1* pos) {
  assert(can_compress_element(id), "invariant");
  JfrEpochQueueNarrowKlassElement* const element = new (pos) JfrEpochQueueNarrowKlassElement();
  store_traceid(element, id, false);
  element->compressed_klass = encode(klass);
}

static void store_uncompressed_element(traceid id, const Klass* klass, u1* pos) {
  JfrEpochQueueKlassElement* const element = new (pos) JfrEpochQueueKlassElement();
  store_traceid(element, id, true);
  element->klass = klass;
}

static void store_element(const Klass* klass, u1* pos) {
  assert(pos != NULL, "invariant");
  assert(klass != NULL, "invariant");
  const traceid id = JfrTraceId::load_raw(klass);
  if (can_compress_element(id)) {
    store_compressed_element(id, klass, pos);
    return;
  }
  store_uncompressed_element(id, klass, pos);
}

static void set_unloaded(const u1* pos) {
  *(const_cast<u1*>(pos)) |= UNLOADED_BIT;
}

static bool is_unloaded(const u1* pos) {
  return (*pos & UNLOADED_BIT) == UNLOADED_BIT;
}

static bool is_compressed(const u1* pos) {
  return (*pos & UNCOMPRESSED_BIT) == 0;
}

// this is an optimization to clear out elements
// by short-curcuiting the callback loop.
static bool _clear = false;

template <typename Buffer>
size_t JfrEpochQueueKlassPolicy<Buffer>::operator()(const u1* pos, KlassFunctor& callback, bool previous_epoch) {
  assert(pos != NULL, "invariant");
  const bool compressed = is_compressed(pos);
  const size_t size = ::element_size(compressed);
  if (_clear || is_unloaded(pos)) {
    return size;
  }
  const Klass* klass;
  const traceid id = read_element(pos, &klass, compressed);
  assert(id > 0, "invariant");
  if (is_unloaded(id, previous_epoch)) {
    set_unloaded(pos);
    return size;
  }
  assert(klass != NULL, "invariant");
  callback(const_cast<Klass*>(klass));
  return size;
}

template <typename Buffer>
void JfrEpochQueueKlassPolicy<Buffer>::store_element(const Klass* klass, Buffer* buffer) {
  assert(klass != NULL, "invariant");
  assert(buffer != NULL, "invariant");
  assert(buffer->free_size() >= ::element_size(klass), "invariant");
  ::store_element(klass, buffer->pos());
}

template <typename Buffer>
inline size_t JfrEpochQueueKlassPolicy<Buffer>::element_size(const Klass* klass) {
  assert(klass != NULL, "invariant");
  return ::element_size(klass);
}

template <typename Buffer>
inline Buffer* JfrEpochQueueKlassPolicy<Buffer>::thread_local_storage(Thread* thread) const {
  assert(thread != NULL, "invariant");
  JfrThreadLocal* tl = thread->jfr_thread_local();
  return JfrTraceIdEpoch::epoch() ? tl->_load_barrier_buffer_epoch_1 : tl->_load_barrier_buffer_epoch_0;
}

template <typename Buffer>
inline void JfrEpochQueueKlassPolicy<Buffer>::set_thread_local_storage(Buffer* buffer, Thread* thread) {
  assert(thread != NULL, "invariant");
  JfrThreadLocal* tl = thread->jfr_thread_local();
  if (JfrTraceIdEpoch::epoch()) {
    tl->_load_barrier_buffer_epoch_1 = buffer;
  } else {
    tl->_load_barrier_buffer_epoch_0 = buffer;
  }
}

JfrTraceIdKlassQueue::JfrTraceIdKlassQueue() : _queue() {}

JfrTraceIdKlassQueue::~JfrTraceIdKlassQueue() {
  delete _queue;
}

bool JfrTraceIdKlassQueue::initialize(size_t min_elem_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count) {
  assert(_queue == NULL, "invariant");
  _queue = new JfrEpochQueue<JfrEpochQueueKlassPolicy>();
  return _queue != NULL && _queue->initialize(min_elem_size, free_list_cache_count_limit, cache_prealloc_count);
}

void JfrTraceIdKlassQueue::clear() {
  if (_queue != NULL) {
    _clear = true;
    KlassFunctor functor(NULL);
    _queue->iterate(functor, true);
    _clear = false;
  }
}

void JfrTraceIdKlassQueue::enqueue(const Klass* klass) {
  assert(klass != NULL, "invariant");
  _queue->enqueue(klass);
}

void JfrTraceIdKlassQueue::iterate(klass_callback callback, bool previous_epoch) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  KlassFunctor functor(callback);
  _queue->iterate(functor, previous_epoch);
}
