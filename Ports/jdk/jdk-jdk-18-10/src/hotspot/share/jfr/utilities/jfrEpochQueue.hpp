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

#ifndef SHARE_JFR_UTILITIES_JFREPOCHQUEUE_HPP
#define SHARE_JFR_UTILITIES_JFREPOCHQUEUE_HPP

#include "jfr/recorder/storage/jfrEpochStorage.hpp"

/*
 * An ElmentPolicy template template argument provides the implementation for how elements
 * associated with the queue is encoded and managed by exposing the following members:
 *
 *  ElmentPolicy::Type                             the type of the element to be stored in the queue.
 *  size_t element_size(Type* t);                  per element storage size requirement.
 *  void store_element(Type* t, Buffer* buffer);   encode and store element of Type into storage of type Buffer.
 *  Buffer* thread_local_storage(Thread* thread);  quick access to thread local storage.
 *  void set_thread_lcoal_storage(Buffer* buffer, Thread* thread); store back capability for newly acquired storage.
 *
 * The ElementPolicy is also the callback when iterating elements of the queue.
 * The iteration callback signature to be provided by the policy class:
 *
 *  size_t operator()(const u1* next_element, Callback& callback, bool previous_epoch = false);
 */
template <template <typename> class ElementPolicy>
class JfrEpochQueue : public JfrCHeapObj {
 public:
  typedef JfrEpochStorage::Buffer Buffer;
  typedef JfrEpochStorage::BufferPtr BufferPtr;
  typedef typename ElementPolicy<Buffer>::Type Type;
  typedef const Type* TypePtr;
  JfrEpochQueue();
  ~JfrEpochQueue();
  bool initialize(size_t min_buffer_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count);
  void enqueue(TypePtr t);
  template <typename Callback>
  void iterate(Callback& callback, bool previous_epoch = false);
 private:
  typedef ElementPolicy<Buffer> Policy;
  Policy _policy;
  JfrEpochStorage* _storage;
  BufferPtr storage_for_element(TypePtr t, size_t element_size);

  template <typename Callback>
  class ElementDispatch {
   private:
    Callback& _callback;
    Policy& _policy;
   public:
    typedef Buffer Type;
    ElementDispatch(Callback& callback, Policy& policy);
    size_t operator()(const u1* element, bool previous_epoch);
  };
};

#endif // SHARE_JFR_UTILITIES_JFREPOCHQUEUE_HPP
