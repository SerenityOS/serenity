/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/chains/edgeQueue.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "jfr/recorder/storage/jfrVirtualMemory.hpp"

EdgeQueue::EdgeQueue(size_t reservation_size_bytes, size_t commit_block_size_bytes) :
  _vmm(NULL),
  _reservation_size_bytes(reservation_size_bytes),
  _commit_block_size_bytes(commit_block_size_bytes),
  _top_index(0),
  _bottom_index(0) {
}

bool EdgeQueue::initialize() {
  assert(_reservation_size_bytes >= _commit_block_size_bytes, "invariant");
  assert(_vmm == NULL, "invariant");
  _vmm = new JfrVirtualMemory();
  return _vmm != NULL && _vmm->initialize(_reservation_size_bytes, _commit_block_size_bytes, sizeof(Edge));
}

EdgeQueue::~EdgeQueue() {
  delete _vmm;
}

void EdgeQueue::add(const Edge* parent, UnifiedOopRef ref) {
  assert(!ref.is_null(), "Null objects not allowed in EdgeQueue");
  assert(!is_full(), "EdgeQueue is full. Check is_full before adding another Edge");
  assert(!_vmm->is_full(), "invariant");
  void* const allocation = _vmm->new_datum();
  assert(allocation != NULL, "invariant");
  new (allocation)Edge(parent, ref);
  _top_index++;
  assert(_vmm->count() == _top_index, "invariant");
}

size_t EdgeQueue::top() const {
  return _top_index;
}

size_t EdgeQueue::bottom() const {
  return EdgeQueue::_bottom_index;
}

bool EdgeQueue::is_empty() const {
  return _top_index == _bottom_index;
}

bool EdgeQueue::is_full() const {
  return _vmm->is_full();
}

const Edge* EdgeQueue::remove() const {
  assert(!is_empty(), "EdgeQueue is empty. Check if empty before removing Edge");
  assert(!_vmm->is_empty(), "invariant");
  return (const Edge*)_vmm->get(_bottom_index++);
}

const Edge* EdgeQueue::element_at(size_t index) const {
  assert(index >= _bottom_index, "invariant");
  assert(index <_top_index, "invariant");
  return (Edge*)_vmm->get(index);
}

size_t EdgeQueue::reserved_size() const {
  assert(_vmm != NULL, "invariant");
  return _vmm->reserved_size();
}

size_t EdgeQueue::live_set() const {
  assert(_vmm != NULL, "invariant");
  return _vmm->live_set();
}

size_t EdgeQueue::sizeof_edge() const {
  assert(_vmm != NULL, "invariant");
  return _vmm->aligned_datum_size_bytes();
}
