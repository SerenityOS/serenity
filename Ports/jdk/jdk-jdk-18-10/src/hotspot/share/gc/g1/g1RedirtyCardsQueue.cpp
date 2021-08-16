/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1RedirtyCardsQueue.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

// G1RedirtyCardsLocalQueueSet

G1RedirtyCardsLocalQueueSet::G1RedirtyCardsLocalQueueSet(G1RedirtyCardsQueueSet* shared_qset) :
  PtrQueueSet(shared_qset->allocator()),
  _shared_qset(shared_qset),
  _buffers(),
  _queue(this)
{}

#ifdef ASSERT
G1RedirtyCardsLocalQueueSet::~G1RedirtyCardsLocalQueueSet() {
  assert(_buffers._head == NULL, "unflushed qset");
  assert(_buffers._tail == NULL, "invariant");
  assert(_buffers._entry_count == 0, "invariant");
}
#endif // ASSERT

void G1RedirtyCardsLocalQueueSet::enqueue_completed_buffer(BufferNode* node) {
  _buffers._entry_count += buffer_size() - node->index();
  node->set_next(_buffers._head);
  _buffers._head = node;
  if (_buffers._tail == NULL) {
    _buffers._tail = node;
  }
}

void G1RedirtyCardsLocalQueueSet::enqueue(void* value) {
  if (!try_enqueue(_queue, value)) {
    BufferNode* old_node = exchange_buffer_with_new(_queue);
    if (old_node != nullptr) {
      enqueue_completed_buffer(old_node);
    }
    retry_enqueue(_queue, value);
  }
}

void G1RedirtyCardsLocalQueueSet::flush() {
  flush_queue(_queue);
  _shared_qset->add_bufferlist(_buffers);
  _buffers = G1BufferNodeList();
}

// G1RedirtyCardsLocalQueueSet::Queue

G1RedirtyCardsLocalQueueSet::Queue::Queue(G1RedirtyCardsLocalQueueSet* qset) :
  PtrQueue(qset)
{}

#ifdef ASSERT
G1RedirtyCardsLocalQueueSet::Queue::~Queue() {
  assert(buffer() == nullptr, "unflushed queue");
}
#endif // ASSERT

// G1RedirtyCardsQueueSet

G1RedirtyCardsQueueSet::G1RedirtyCardsQueueSet(BufferNode::Allocator* allocator) :
  PtrQueueSet(allocator),
  _list(),
  _entry_count(0),
  _tail(NULL)
  DEBUG_ONLY(COMMA _collecting(true))
{}

G1RedirtyCardsQueueSet::~G1RedirtyCardsQueueSet() {
  verify_empty();
}

#ifdef ASSERT
void G1RedirtyCardsQueueSet::verify_empty() const {
  assert(_list.empty(), "precondition");
  assert(_tail == NULL, "invariant");
  assert(_entry_count == 0, "invariant");
}
#endif // ASSERT

BufferNode* G1RedirtyCardsQueueSet::all_completed_buffers() const {
  DEBUG_ONLY(_collecting = false;)
  return _list.top();
}

G1BufferNodeList G1RedirtyCardsQueueSet::take_all_completed_buffers() {
  DEBUG_ONLY(_collecting = false;)
  G1BufferNodeList result(_list.pop_all(), _tail, _entry_count);
  _tail = NULL;
  _entry_count = 0;
  DEBUG_ONLY(_collecting = true;)
  return result;
}

void G1RedirtyCardsQueueSet::update_tail(BufferNode* node) {
  // Node is the tail of a (possibly single element) list just prepended to
  // _list.  If, after that prepend, node's follower is NULL, then node is
  // also the tail of _list, so record it as such.
  if (node->next() == NULL) {
    assert(_tail == NULL, "invariant");
    _tail = node;
  }
}

void G1RedirtyCardsQueueSet::enqueue_completed_buffer(BufferNode* node) {
  assert(_collecting, "precondition");
  Atomic::add(&_entry_count, buffer_size() - node->index());
  _list.push(*node);
  update_tail(node);
}

void G1RedirtyCardsQueueSet::add_bufferlist(const G1BufferNodeList& buffers) {
  assert(_collecting, "precondition");
  if (buffers._head != NULL) {
    assert(buffers._tail != NULL, "invariant");
    Atomic::add(&_entry_count, buffers._entry_count);
    _list.prepend(*buffers._head, *buffers._tail);
    update_tail(buffers._tail);
  }
}
