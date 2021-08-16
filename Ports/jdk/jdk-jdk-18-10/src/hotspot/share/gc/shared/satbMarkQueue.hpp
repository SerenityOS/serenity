/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_SATBMARKQUEUE_HPP
#define SHARE_GC_SHARED_SATBMARKQUEUE_HPP

#include "gc/shared/ptrQueue.hpp"
#include "memory/allocation.hpp"
#include "memory/padded.hpp"
#include "oops/oopsHierarchy.hpp"

class Thread;
class Monitor;
class SATBMarkQueueSet;

// Base class for processing the contents of a SATB buffer.
class SATBBufferClosure : public StackObj {
protected:
  ~SATBBufferClosure() { }

public:
  // Process the SATB entries in the designated buffer range.
  virtual void do_buffer(void** buffer, size_t size) = 0;
};

// A PtrQueue whose elements are (possibly stale) pointers to object heads.
class SATBMarkQueue: public PtrQueue {
  friend class VMStructs;
  friend class SATBMarkQueueSet;

private:
  // Per-queue (so thread-local) cache of the SATBMarkQueueSet's
  // active state, to support inline barriers in compiled code.
  bool _active;

public:
  SATBMarkQueue(SATBMarkQueueSet* qset);

  bool is_active() const { return _active; }
  void set_active(bool value) { _active = value; }

#ifndef PRODUCT
  // Helpful for debugging
  void print(const char* name);
#endif // PRODUCT

  // Compiler support.
  static ByteSize byte_offset_of_index() {
    return PtrQueue::byte_offset_of_index<SATBMarkQueue>();
  }
  using PtrQueue::byte_width_of_index;

  static ByteSize byte_offset_of_buf() {
    return PtrQueue::byte_offset_of_buf<SATBMarkQueue>();
  }
  using PtrQueue::byte_width_of_buf;

  static ByteSize byte_offset_of_active() {
    return byte_offset_of(SATBMarkQueue, _active);
  }

  static ByteSize byte_width_of_active() { return in_ByteSize(sizeof(bool)); }
};

class SATBMarkQueueSet: public PtrQueueSet {

  DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, 0);
  PaddedEnd<BufferNode::Stack> _list;
  volatile size_t _count_and_process_flag;
  // These are rarely (if ever) changed, so same cache line as count.
  size_t _process_completed_buffers_threshold;
  size_t _buffer_enqueue_threshold;
  // SATB is only active during marking.  Enqueuing is only done when active.
  bool _all_active;
  DEFINE_PAD_MINUS_SIZE(2, DEFAULT_CACHE_LINE_SIZE, 4 * sizeof(size_t));

  BufferNode* get_completed_buffer();
  void abandon_completed_buffers();

#ifdef ASSERT
  void dump_active_states(bool expected_active);
  void verify_active_states(bool expected_active);
#endif // ASSERT

protected:
  SATBMarkQueueSet(BufferNode::Allocator* allocator);
  ~SATBMarkQueueSet();

  void handle_zero_index(SATBMarkQueue& queue);

  // Return true if the queue's buffer should be enqueued, even if not full.
  // The default method uses the buffer enqueue threshold.
  bool should_enqueue_buffer(SATBMarkQueue& queue);

  template<typename Filter>
  void apply_filter(Filter filter, SATBMarkQueue& queue);

public:
  virtual SATBMarkQueue& satb_queue_for_thread(Thread* const t) const = 0;

  bool is_active() const { return _all_active; }

  // Apply "set_active(active)" to all SATB queues in the set. It should be
  // called only with the world stopped. The method will assert that the
  // SATB queues of all threads it visits, as well as the SATB queue
  // set itself, has an active value same as expected_active.
  void set_active_all_threads(bool active, bool expected_active);

  void set_process_completed_buffers_threshold(size_t value);

  size_t buffer_enqueue_threshold() const { return _buffer_enqueue_threshold; }
  void set_buffer_enqueue_threshold_percentage(uint value);

  // If there exists some completed buffer, pop and process it, and
  // return true.  Otherwise return false.  Processing a buffer
  // consists of applying the closure to the active range of the
  // buffer; the leading entries may be excluded due to filtering.
  bool apply_closure_to_completed_buffer(SATBBufferClosure* cl);

  void flush_queue(SATBMarkQueue& queue);

  // When active, add obj to queue by calling enqueue_known_active.
  void enqueue(SATBMarkQueue& queue, oop obj) {
    if (queue.is_active()) enqueue_known_active(queue, obj);
  }
  // Add obj to queue.  This qset and the queue must be active.
  void enqueue_known_active(SATBMarkQueue& queue, oop obj);
  virtual void filter(SATBMarkQueue& queue) = 0;
  virtual void enqueue_completed_buffer(BufferNode* node);

  // The number of buffers in the list.  Racy and not updated atomically
  // with the set of completed buffers.
  size_t completed_buffers_num() const {
    return _count_and_process_flag >> 1;
  }

  // Return true if completed buffers should be processed.
  bool process_completed_buffers() const {
    return (_count_and_process_flag & 1) != 0;
  }

#ifndef PRODUCT
  // Helpful for debugging
  void print_all(const char* msg);
#endif // PRODUCT

  // If a marking is being abandoned, reset any unprocessed log buffers.
  void abandon_partial_marking();
};

// Removes entries from queue's buffer that are no longer needed, as
// determined by filter. If e is a void* entry in queue's buffer,
// filter_out(e) must be a valid expression whose value is convertible
// to bool. Entries are removed (filtered out) if the result is true,
// retained if false.
template<typename Filter>
inline void SATBMarkQueueSet::apply_filter(Filter filter_out, SATBMarkQueue& queue) {
  void** buf = queue.buffer();

  if (buf == NULL) {
    // nothing to do
    return;
  }

  // Two-fingered compaction toward the end.
  void** src = &buf[queue.index()];
  void** dst = &buf[buffer_size()];
  assert(src <= dst, "invariant");
  for ( ; src < dst; ++src) {
    // Search low to high for an entry to keep.
    void* entry = *src;
    if (!filter_out(entry)) {
      // Found keeper.  Search high to low for an entry to discard.
      while (src < --dst) {
        if (filter_out(*dst)) {
          *dst = entry;         // Replace discard with keeper.
          break;
        }
      }
      // If discard search failed (src == dst), the outer loop will also end.
    }
  }
  // dst points to the lowest retained entry, or the end of the buffer
  // if all the entries were filtered out.
  queue.set_index(dst - buf);
}

#endif // SHARE_GC_SHARED_SATBMARKQUEUE_HPP
