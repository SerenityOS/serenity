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

#include "precompiled.hpp"
#include "gc/shared/satbMarkQueue.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/globalCounter.inline.hpp"

SATBMarkQueue::SATBMarkQueue(SATBMarkQueueSet* qset) :
  PtrQueue(qset),
  // SATB queues are only active during marking cycles. We create them
  // with their active field set to false. If a thread is created
  // during a cycle, it's SATB queue needs to be activated before the
  // thread starts running.  This is handled by the collector-specific
  // BarrierSet thread attachment protocol.
  _active(false)
{ }

#ifndef PRODUCT
// Helpful for debugging

static void print_satb_buffer(const char* name,
                              void** buf,
                              size_t index,
                              size_t capacity) {
  tty->print_cr("  SATB BUFFER [%s] buf: " PTR_FORMAT " index: " SIZE_FORMAT
                " capacity: " SIZE_FORMAT,
                name, p2i(buf), index, capacity);
}

void SATBMarkQueue::print(const char* name) {
  print_satb_buffer(name, _buf, index(), capacity());
}

#endif // PRODUCT

SATBMarkQueueSet::SATBMarkQueueSet(BufferNode::Allocator* allocator) :
  PtrQueueSet(allocator),
  _list(),
  _count_and_process_flag(0),
  _process_completed_buffers_threshold(SIZE_MAX),
  _buffer_enqueue_threshold(0),
  _all_active(false)
{}

SATBMarkQueueSet::~SATBMarkQueueSet() {
  abandon_completed_buffers();
}

// _count_and_process_flag has flag in least significant bit, count in
// remaining bits.  _process_completed_buffers_threshold is scaled
// accordingly, with the lsbit set, so a _count_and_process_flag value
// is directly comparable with the recorded threshold value.  The
// process flag is set whenever the count exceeds the threshold, and
// remains set until the count is reduced to zero.

// Increment count.  If count > threshold, set flag, else maintain flag.
static void increment_count(volatile size_t* cfptr, size_t threshold) {
  size_t old;
  size_t value = Atomic::load(cfptr);
  do {
    old = value;
    value += 2;
    assert(value > old, "overflow");
    if (value > threshold) value |= 1;
    value = Atomic::cmpxchg(cfptr, old, value);
  } while (value != old);
}

// Decrement count.  If count == 0, clear flag, else maintain flag.
static void decrement_count(volatile size_t* cfptr) {
  size_t old;
  size_t value = Atomic::load(cfptr);
  do {
    assert((value >> 1) != 0, "underflow");
    old = value;
    value -= 2;
    if (value <= 1) value = 0;
    value = Atomic::cmpxchg(cfptr, old, value);
  } while (value != old);
}

void SATBMarkQueueSet::set_process_completed_buffers_threshold(size_t value) {
  // Scale requested threshold to align with count field.  If scaling
  // overflows, just use max value.  Set process flag field to make
  // comparison in increment_count exact.
  size_t scaled_value = value << 1;
  if ((scaled_value >> 1) != value) {
    scaled_value = SIZE_MAX;
  }
  _process_completed_buffers_threshold = scaled_value | 1;
}

void SATBMarkQueueSet::set_buffer_enqueue_threshold_percentage(uint value) {
  // Minimum threshold of 1 ensures enqueuing of completely full buffers.
  size_t size = buffer_size();
  size_t enqueue_qty = (size * value) / 100;
  _buffer_enqueue_threshold = MAX2(size - enqueue_qty, (size_t)1);
}

#ifdef ASSERT
void SATBMarkQueueSet::dump_active_states(bool expected_active) {
  log_error(gc, verify)("Expected SATB active state: %s", expected_active ? "ACTIVE" : "INACTIVE");
  log_error(gc, verify)("Actual SATB active states:");
  log_error(gc, verify)("  Queue set: %s", is_active() ? "ACTIVE" : "INACTIVE");

  class DumpThreadStateClosure : public ThreadClosure {
    SATBMarkQueueSet* _qset;
  public:
    DumpThreadStateClosure(SATBMarkQueueSet* qset) : _qset(qset) {}
    virtual void do_thread(Thread* t) {
      SATBMarkQueue& queue = _qset->satb_queue_for_thread(t);
      log_error(gc, verify)("  Thread \"%s\" queue: %s",
                            t->name(),
                            queue.is_active() ? "ACTIVE" : "INACTIVE");
    }
  } closure(this);
  Threads::threads_do(&closure);
}

void SATBMarkQueueSet::verify_active_states(bool expected_active) {
  // Verify queue set state
  if (is_active() != expected_active) {
    dump_active_states(expected_active);
    fatal("SATB queue set has an unexpected active state");
  }

  // Verify thread queue states
  class VerifyThreadStatesClosure : public ThreadClosure {
    SATBMarkQueueSet* _qset;
    bool _expected_active;
  public:
    VerifyThreadStatesClosure(SATBMarkQueueSet* qset, bool expected_active) :
      _qset(qset), _expected_active(expected_active) {}
    virtual void do_thread(Thread* t) {
      if (_qset->satb_queue_for_thread(t).is_active() != _expected_active) {
        _qset->dump_active_states(_expected_active);
        fatal("Thread SATB queue has an unexpected active state");
      }
    }
  } closure(this, expected_active);
  Threads::threads_do(&closure);
}
#endif // ASSERT

void SATBMarkQueueSet::set_active_all_threads(bool active, bool expected_active) {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at safepoint.");
#ifdef ASSERT
  verify_active_states(expected_active);
#endif // ASSERT
  // Update the global state, synchronized with threads list management.
  {
    MutexLocker ml(NonJavaThreadsList_lock, Mutex::_no_safepoint_check_flag);
    _all_active = active;
  }

  class SetThreadActiveClosure : public ThreadClosure {
    SATBMarkQueueSet* _qset;
    bool _active;
  public:
    SetThreadActiveClosure(SATBMarkQueueSet* qset, bool active) :
      _qset(qset), _active(active) {}
    virtual void do_thread(Thread* t) {
      SATBMarkQueue& queue = _qset->satb_queue_for_thread(t);
      if (queue.buffer() != nullptr) {
        assert(!_active || queue.index() == _qset->buffer_size(),
               "queues should be empty when activated");
        queue.set_index(_qset->buffer_size());
      }
      queue.set_active(_active);
    }
  } closure(this, active);
  Threads::threads_do(&closure);
}

bool SATBMarkQueueSet::apply_closure_to_completed_buffer(SATBBufferClosure* cl) {
  BufferNode* nd = get_completed_buffer();
  if (nd != NULL) {
    void **buf = BufferNode::make_buffer_from_node(nd);
    size_t index = nd->index();
    size_t size = buffer_size();
    assert(index <= size, "invariant");
    cl->do_buffer(buf + index, size - index);
    deallocate_buffer(nd);
    return true;
  } else {
    return false;
  }
}

void SATBMarkQueueSet::flush_queue(SATBMarkQueue& queue) {
  // Filter now to possibly save work later.  If filtering empties the
  // buffer then flush_queue can deallocate the buffer.
  filter(queue);
  PtrQueueSet::flush_queue(queue);
}

void SATBMarkQueueSet::enqueue_known_active(SATBMarkQueue& queue, oop obj) {
  assert(queue.is_active(), "precondition");
  void* value = cast_from_oop<void*>(obj);
  if (!try_enqueue(queue, value)) {
    handle_zero_index(queue);
    retry_enqueue(queue, value);
  }
}

void SATBMarkQueueSet::handle_zero_index(SATBMarkQueue& queue) {
  assert(queue.index() == 0, "precondition");
  if (queue.buffer() == nullptr) {
    install_new_buffer(queue);
  } else {
    filter(queue);
    if (should_enqueue_buffer(queue)) {
      enqueue_completed_buffer(exchange_buffer_with_new(queue));
    } // Else continue to use the existing buffer.
  }
  assert(queue.buffer() != nullptr, "post condition");
  assert(queue.index() > 0, "post condition");
}

bool SATBMarkQueueSet::should_enqueue_buffer(SATBMarkQueue& queue) {
  // Keep the current buffer if filtered index >= threshold.
  size_t threshold = buffer_enqueue_threshold();
  // Ensure we'll enqueue completely full buffers.
  assert(threshold > 0, "enqueue threshold = 0");
  // Ensure we won't enqueue empty buffers.
  assert(threshold <= buffer_size(),
         "enqueue threshold %zu exceeds capacity %zu",
         threshold, buffer_size());
  return queue.index() < threshold;
}

// SATB buffer life-cycle - Per-thread queues obtain buffers from the
// qset's buffer allocator, fill them, and push them onto the qset's
// list.  The GC concurrently pops buffers from the qset, processes
// them, and returns them to the buffer allocator for re-use.  Both
// the allocator and the qset use lock-free stacks.  The ABA problem
// is solved by having both allocation pops and GC pops performed
// within GlobalCounter critical sections, while the return of buffers
// to the allocator performs a GlobalCounter synchronize before
// pushing onto the allocator's list.

void SATBMarkQueueSet::enqueue_completed_buffer(BufferNode* node) {
  assert(node != NULL, "precondition");
  // Increment count and update flag appropriately.  Done before
  // pushing buffer so count is always at least the actual number in
  // the list, and decrement never underflows.
  increment_count(&_count_and_process_flag, _process_completed_buffers_threshold);
  _list.push(*node);
}

BufferNode* SATBMarkQueueSet::get_completed_buffer() {
  BufferNode* node;
  {
    GlobalCounter::CriticalSection cs(Thread::current());
    node = _list.pop();
  }
  if (node != NULL) {
    // Got a buffer so decrement count and update flag appropriately.
    decrement_count(&_count_and_process_flag);
  }
  return node;
}

#ifndef PRODUCT
// Helpful for debugging

#define SATB_PRINTER_BUFFER_SIZE 256

void SATBMarkQueueSet::print_all(const char* msg) {
  char buffer[SATB_PRINTER_BUFFER_SIZE];
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at safepoint.");

  tty->cr();
  tty->print_cr("SATB BUFFERS [%s]", msg);

  BufferNode* nd = _list.top();
  int i = 0;
  while (nd != NULL) {
    void** buf = BufferNode::make_buffer_from_node(nd);
    os::snprintf(buffer, SATB_PRINTER_BUFFER_SIZE, "Enqueued: %d", i);
    print_satb_buffer(buffer, buf, nd->index(), buffer_size());
    nd = nd->next();
    i += 1;
  }

  class PrintThreadClosure : public ThreadClosure {
    SATBMarkQueueSet* _qset;
    char* _buffer;

  public:
    PrintThreadClosure(SATBMarkQueueSet* qset, char* buffer) :
      _qset(qset), _buffer(buffer) {}

    virtual void do_thread(Thread* t) {
      os::snprintf(_buffer, SATB_PRINTER_BUFFER_SIZE, "Thread: %s", t->name());
      _qset->satb_queue_for_thread(t).print(_buffer);
    }
  } closure(this, buffer);
  Threads::threads_do(&closure);

  tty->cr();
}
#endif // PRODUCT

void SATBMarkQueueSet::abandon_completed_buffers() {
  Atomic::store(&_count_and_process_flag, size_t(0));
  BufferNode* buffers_to_delete = _list.pop_all();
  while (buffers_to_delete != NULL) {
    BufferNode* bn = buffers_to_delete;
    buffers_to_delete = bn->next();
    bn->set_next(NULL);
    deallocate_buffer(bn);
  }
}

void SATBMarkQueueSet::abandon_partial_marking() {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at safepoint.");
  abandon_completed_buffers();

  class AbandonThreadQueueClosure : public ThreadClosure {
    SATBMarkQueueSet& _qset;
  public:
    AbandonThreadQueueClosure(SATBMarkQueueSet& qset) : _qset(qset) {}
    virtual void do_thread(Thread* t) {
      _qset.reset_queue(_qset.satb_queue_for_thread(t));
    }
  } closure(*this);
  Threads::threads_do(&closure);
}
