/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/g1/g1SharedDirtyCardQueue.hpp"
#include "gc/shared/ptrQueue.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"

G1SharedDirtyCardQueue::G1SharedDirtyCardQueue(G1DirtyCardQueueSet* qset) :
  _qset(qset),
  _buffer(NULL),
  _index(0)
{}

G1SharedDirtyCardQueue::~G1SharedDirtyCardQueue() {
  flush();
}

void G1SharedDirtyCardQueue::enqueue(void* card_ptr) {
  MutexLocker ml(Shared_DirtyCardQ_lock, Mutex::_no_safepoint_check_flag);
  if (_index == 0) {
    flush();
    _buffer = _qset->allocate_buffer();
    _index = _qset->buffer_size();
    assert(_index != 0, "invariant");
  }
  _buffer[--_index] = card_ptr;
}

void G1SharedDirtyCardQueue::flush() {
  if (_buffer != NULL) {
    BufferNode* node = BufferNode::make_node_from_buffer(_buffer, _index);
    _buffer = NULL;
    _index = 0;
    if (node->index() == _qset->buffer_size()) {
      _qset->deallocate_buffer(node);
    } else {
      _qset->enqueue_completed_buffer(node);
    }
  }
  assert(_index == 0, "invariant");
}

void G1SharedDirtyCardQueue::reset() {
  if (_buffer == NULL) {
    _index = 0;
  } else {
    _index = _qset->buffer_size();
  }
}
