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

#ifndef SHARE_JFR_UTILITIES_JFREPOCHQUEUE_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFREPOCHQUEUE_INLINE_HPP

#include "jfr/utilities/jfrEpochQueue.hpp"

#include "jfr/recorder/storage/jfrEpochStorage.inline.hpp"
#include "jfr/recorder/storage/jfrStorageUtils.inline.hpp"
#include "runtime/thread.inline.hpp"

template <template <typename> class ElementPolicy>
JfrEpochQueue<ElementPolicy>::JfrEpochQueue() : _policy(), _storage(NULL) {}

template <template <typename> class ElementPolicy>
JfrEpochQueue<ElementPolicy>::~JfrEpochQueue() {
  delete _storage;
}

template <template <typename> class ElementPolicy>
bool JfrEpochQueue<ElementPolicy>::initialize(size_t min_buffer_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count) {
  assert(_storage == NULL, "invariant");
  _storage = new JfrEpochStorage();
  return _storage != NULL && _storage->initialize(min_buffer_size, free_list_cache_count_limit, cache_prealloc_count);
}

template <template <typename> class ElementPolicy>
inline typename JfrEpochQueue<ElementPolicy>::BufferPtr
JfrEpochQueue<ElementPolicy>::storage_for_element(JfrEpochQueue<ElementPolicy>::TypePtr t, size_t element_size) {
  assert(_policy.element_size(t) == element_size, "invariant");
  Thread* const thread = Thread::current();
  BufferPtr buffer = _policy.thread_local_storage(thread);
  if (buffer == NULL) {
    buffer = _storage->acquire(element_size, thread);
    _policy.set_thread_local_storage(buffer, thread);
  } else if (buffer->free_size() < element_size) {
    _storage->release(buffer);
    buffer = _storage->acquire(element_size, thread);
    _policy.set_thread_local_storage(buffer, thread);
  }
  assert(buffer->free_size() >= element_size, "invariant");
  assert(_policy.thread_local_storage(thread) == buffer, "invariant");
  return buffer;
}

template <template <typename> class ElementPolicy>
void JfrEpochQueue<ElementPolicy>::enqueue(JfrEpochQueue<ElementPolicy>::TypePtr t) {
  assert(t != NULL, "invariant");
  size_t element_size = _policy.element_size(t);
  BufferPtr buffer = storage_for_element(t, element_size);
  assert(buffer != NULL, "invariant");
  _policy.store_element(t, buffer);
  buffer->set_pos(element_size);
}

template <template <typename> class ElementPolicy>
template <typename Callback>
JfrEpochQueue<ElementPolicy>::ElementDispatch<Callback>::ElementDispatch(Callback& callback, JfrEpochQueue<ElementPolicy>::Policy& policy) :
  _callback(callback),_policy(policy) {}

template <template <typename> class ElementPolicy>
template <typename Callback>
size_t JfrEpochQueue<ElementPolicy>::ElementDispatch<Callback>::operator()(const u1* element, bool previous_epoch) {
  assert(element != NULL, "invariant");
  return _policy(element, _callback, previous_epoch);
}

template <template <typename> class ElementPolicy>
template <typename Callback>
void JfrEpochQueue<ElementPolicy>::iterate(Callback& callback, bool previous_epoch) {
  typedef ElementDispatch<Callback> ElementDispatcher;
  typedef EpochDispatchOp<ElementDispatcher> QueueDispatcher;
  ElementDispatcher element_dispatcher(callback, _policy);
  QueueDispatcher dispatch(element_dispatcher, previous_epoch);
  _storage->iterate(dispatch, previous_epoch);
  DEBUG_ONLY(_storage->verify_previous_empty();)
}

#endif // SHARE_JFR_UTILITIES_JFREPOCHQUEUE_INLINE_HPP
