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
#include "gc/shared/ptrQueue.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/globalCounter.inline.hpp"

#include <new>

PtrQueue::PtrQueue(PtrQueueSet* qset) :
  _index(0),
  _capacity_in_bytes(index_to_byte_index(qset->buffer_size())),
  _buf(NULL)
{}

PtrQueue::~PtrQueue() {
  assert(_buf == NULL, "queue must be flushed before delete");
}

BufferNode* BufferNode::allocate(size_t size) {
  size_t byte_size = size * sizeof(void*);
  void* data = NEW_C_HEAP_ARRAY(char, buffer_offset() + byte_size, mtGC);
  return new (data) BufferNode;
}

void BufferNode::deallocate(BufferNode* node) {
  node->~BufferNode();
  FREE_C_HEAP_ARRAY(char, node);
}

BufferNode::Allocator::Allocator(const char* name, size_t buffer_size) :
  _buffer_size(buffer_size),
  _pending_list(),
  _free_list(),
  _pending_count(0),
  _free_count(0),
  _transfer_lock(false)
{
  strncpy(_name, name, sizeof(_name) - 1);
  _name[sizeof(_name) - 1] = '\0';
}

BufferNode::Allocator::~Allocator() {
  delete_list(_free_list.pop_all());
  delete_list(_pending_list.pop_all());
}

void BufferNode::Allocator::delete_list(BufferNode* list) {
  while (list != NULL) {
    BufferNode* next = list->next();
    DEBUG_ONLY(list->set_next(NULL);)
    BufferNode::deallocate(list);
    list = next;
  }
}

size_t BufferNode::Allocator::free_count() const {
  return Atomic::load(&_free_count);
}

BufferNode* BufferNode::Allocator::allocate() {
  BufferNode* node;
  {
    // Protect against ABA; see release().
    GlobalCounter::CriticalSection cs(Thread::current());
    node = _free_list.pop();
  }
  if (node == NULL) {
    node = BufferNode::allocate(_buffer_size);
  } else {
    // Decrement count after getting buffer from free list.  This, along
    // with incrementing count before adding to free list, ensures count
    // never underflows.
    size_t count = Atomic::sub(&_free_count, 1u);
    assert((count + 1) != 0, "_free_count underflow");
  }
  return node;
}

// To solve the ABA problem for lock-free stack pop, allocate does the
// pop inside a critical section, and release synchronizes on the
// critical sections before adding to the _free_list.  But we don't
// want to make every release have to do a synchronize.  Instead, we
// initially place released nodes on the _pending_list, and transfer
// them to the _free_list in batches.  Only one transfer at a time is
// permitted, with a lock bit to control access to that phase.  A
// transfer takes all the nodes from the _pending_list, synchronizes on
// the _free_list pops, and then adds the former pending nodes to the
// _free_list.  While that's happening, other threads might be adding
// other nodes to the _pending_list, to be dealt with by some later
// transfer.
void BufferNode::Allocator::release(BufferNode* node) {
  assert(node != NULL, "precondition");
  assert(node->next() == NULL, "precondition");

  // Desired minimum transfer batch size.  There is relatively little
  // importance to the specific number.  It shouldn't be too big, else
  // we're wasting space when the release rate is low.  If the release
  // rate is high, we might accumulate more than this before being
  // able to start a new transfer, but that's okay.  Also note that
  // the allocation rate and the release rate are going to be fairly
  // similar, due to how the buffers are used.
  const size_t trigger_transfer = 10;

  // Add to pending list. Update count first so no underflow in transfer.
  size_t pending_count = Atomic::add(&_pending_count, 1u);
  _pending_list.push(*node);
  if (pending_count > trigger_transfer) {
    try_transfer_pending();
  }
}

// Try to transfer nodes from _pending_list to _free_list, with a
// synchronization delay for any in-progress pops from the _free_list,
// to solve ABA there.  Return true if performed a (possibly empty)
// transfer, false if blocked from doing so by some other thread's
// in-progress transfer.
bool BufferNode::Allocator::try_transfer_pending() {
  // Attempt to claim the lock.
  if (Atomic::load(&_transfer_lock) || // Skip CAS if likely to fail.
      Atomic::cmpxchg(&_transfer_lock, false, true)) {
    return false;
  }
  // Have the lock; perform the transfer.

  // Claim all the pending nodes.
  BufferNode* first = _pending_list.pop_all();
  if (first != NULL) {
    // Prepare to add the claimed nodes, and update _pending_count.
    BufferNode* last = first;
    size_t count = 1;
    for (BufferNode* next = first->next(); next != NULL; next = next->next()) {
      last = next;
      ++count;
    }
    Atomic::sub(&_pending_count, count);

    // Wait for any in-progress pops, to avoid ABA for them.
    GlobalCounter::write_synchronize();

    // Add synchronized nodes to _free_list.
    // Update count first so no underflow in allocate().
    Atomic::add(&_free_count, count);
    _free_list.prepend(*first, *last);
    log_trace(gc, ptrqueue, freelist)
             ("Transferred %s pending to free: " SIZE_FORMAT, name(), count);
  }
  Atomic::release_store(&_transfer_lock, false);
  return true;
}

size_t BufferNode::Allocator::reduce_free_list(size_t remove_goal) {
  try_transfer_pending();
  size_t removed = 0;
  for ( ; removed < remove_goal; ++removed) {
    BufferNode* node = _free_list.pop();
    if (node == NULL) break;
    BufferNode::deallocate(node);
  }
  size_t new_count = Atomic::sub(&_free_count, removed);
  log_debug(gc, ptrqueue, freelist)
           ("Reduced %s free list by " SIZE_FORMAT " to " SIZE_FORMAT,
            name(), removed, new_count);
  return removed;
}

PtrQueueSet::PtrQueueSet(BufferNode::Allocator* allocator) :
  _allocator(allocator)
{}

PtrQueueSet::~PtrQueueSet() {}

void PtrQueueSet::reset_queue(PtrQueue& queue) {
  if (queue.buffer() != nullptr) {
    queue.set_index(buffer_size());
  }
}

void PtrQueueSet::flush_queue(PtrQueue& queue) {
  void** buffer = queue.buffer();
  if (buffer != nullptr) {
    size_t index = queue.index();
    queue.set_buffer(nullptr);
    queue.set_index(0);
    BufferNode* node = BufferNode::make_node_from_buffer(buffer, index);
    if (index == buffer_size()) {
      deallocate_buffer(node);
    } else {
      enqueue_completed_buffer(node);
    }
  }
}

bool PtrQueueSet::try_enqueue(PtrQueue& queue, void* value) {
  size_t index = queue.index();
  if (index == 0) return false;
  void** buffer = queue.buffer();
  assert(buffer != nullptr, "no buffer but non-zero index");
  buffer[--index] = value;
  queue.set_index(index);
  return true;
}

void PtrQueueSet::retry_enqueue(PtrQueue& queue, void* value) {
  assert(queue.index() != 0, "precondition");
  assert(queue.buffer() != nullptr, "precondition");
  size_t index = queue.index();
  queue.buffer()[--index] = value;
  queue.set_index(index);
}

BufferNode* PtrQueueSet::exchange_buffer_with_new(PtrQueue& queue) {
  BufferNode* node = nullptr;
  void** buffer = queue.buffer();
  if (buffer != nullptr) {
    node = BufferNode::make_node_from_buffer(buffer, queue.index());
  }
  install_new_buffer(queue);
  return node;
}

void PtrQueueSet::install_new_buffer(PtrQueue& queue) {
  queue.set_buffer(allocate_buffer());
  queue.set_index(buffer_size());
}

void** PtrQueueSet::allocate_buffer() {
  BufferNode* node = _allocator->allocate();
  return BufferNode::make_buffer_from_node(node);
}

void PtrQueueSet::deallocate_buffer(BufferNode* node) {
  _allocator->release(node);
}
