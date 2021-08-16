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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDKLASSQUEUE_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDKLASSQUEUE_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrEpochQueue.hpp"

class Klass;
class Thread;

typedef void(*klass_callback)(Klass*);

class KlassFunctor {
  klass_callback _cb;
 public:
  KlassFunctor(klass_callback cb) : _cb(cb) {}
   void operator()(Klass* klass) const {
     _cb(klass);
  }
};

//
// The policy template class to be used in combination with JfrEpochQueue to specialize a queue.
// It details how to store and process an enqueued Klass representation. See utilities/jfrEpochQueue.hpp.
//
template <typename Buffer>
class JfrEpochQueueKlassPolicy {
 public:
  typedef Buffer* BufferPtr;
  typedef Klass Type;
  // Encode an individual klass and additional metadata
  // and store it into the buffer associated with the queue.
  void store_element(const Klass* klass, BufferPtr buffer);
  // Element size is a function of the traceid value.
  size_t element_size(const Klass* klass);
  // Storage associated with the the queue is distributed and cached in thread locals.
  BufferPtr thread_local_storage(Thread* thread) const;
  void set_thread_local_storage(BufferPtr buffer, Thread* thread);
  // Klasses are validated for liveness before being forwarded to the user provided callback.
  size_t operator()(const u1* pos, KlassFunctor& callback, bool previous_epoch = false);
};

class JfrTraceIdKlassQueue : public JfrCHeapObj {
 private:
  JfrEpochQueue<JfrEpochQueueKlassPolicy>* _queue;
 public:
  JfrTraceIdKlassQueue();
  ~JfrTraceIdKlassQueue();
  bool initialize(size_t min_elem_size, size_t free_list_cache_count_limit, size_t cache_prealloc_count);
  void clear();
  void enqueue(const Klass* klass);
  void iterate(klass_callback callback, bool previous_epoch = false);
};

#endif //SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDKLASSQUEUE_HPP
