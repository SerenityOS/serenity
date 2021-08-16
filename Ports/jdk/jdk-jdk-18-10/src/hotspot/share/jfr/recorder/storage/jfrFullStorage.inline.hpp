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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEFULLLIST_INLINE_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEFULLLIST_INLINE_HPP

#include "jfr/recorder/storage/jfrFullStorage.hpp"

#include "jfr/recorder/storage/jfrStorageControl.hpp"
#include "jfr/utilities/jfrConcurrentQueue.inline.hpp"

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
JfrFullStorage<ValueType, NodeType, AllocPolicy>
::JfrFullStorage(JfrStorageControl& control) : _control(control), _free_node_list(NULL), _queue(NULL) {}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
JfrFullStorage<ValueType, NodeType, AllocPolicy>::~JfrFullStorage() {
  NodePtr node;
  while (_free_node_list->is_nonempty()) {
    node = _free_node_list->remove();
    delete node;
  }
  delete _free_node_list;

  while (_queue->is_nonempty()) {
    node = _queue->remove();
    delete node;
  }
  delete _queue;
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
bool JfrFullStorage<ValueType, NodeType, AllocPolicy>::initialize(size_t free_list_prealloc_count) {
  assert(_free_node_list == NULL, "invariant");
  _free_node_list = new JfrConcurrentQueue<Node>();
  if (_free_node_list == NULL || !_free_node_list->initialize()) {
    return false;
  }
  for (size_t i = 0; i < free_list_prealloc_count; ++i) {
    NodePtr node = new (ResourceObj::C_HEAP, mtTracing) Node();
    if (node == NULL) {
      return false;
    }
    _free_node_list->add(node);
  }
  assert(_queue == NULL, "invariant");
  _queue = new JfrConcurrentQueue<Node>();
  return _queue != NULL && _queue->initialize();
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline bool JfrFullStorage<ValueType, NodeType, AllocPolicy>::is_empty() const {
  return _queue->is_empty();
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline bool JfrFullStorage<ValueType, NodeType, AllocPolicy>::is_nonempty() const {
  return !is_empty();
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline typename JfrFullStorage<ValueType, NodeType, AllocPolicy>::NodePtr
JfrFullStorage<ValueType, NodeType, AllocPolicy>::acquire() {
  NodePtr node = _free_node_list->remove();
  return node != NULL ? node : new (ResourceObj::C_HEAP, mtTracing) Node();
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline void JfrFullStorage<ValueType, NodeType, AllocPolicy>
::release(typename JfrFullStorage<ValueType, NodeType, AllocPolicy>::NodePtr node) {
  assert(node != NULL, "invariant");
  _free_node_list->add(node);
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline bool JfrFullStorage<ValueType, NodeType, AllocPolicy>::add(ValueType value) {
  assert(value != NULL, "invariant");
  NodePtr node = acquire();
  assert(node != NULL, "invariant");
  node->set_value(value);
  const bool notify = _control.increment_full();
  _queue->add(node);
  return notify;
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
inline ValueType JfrFullStorage<ValueType, NodeType, AllocPolicy>::remove() {
  Value value = NULL;
  NodePtr node = _queue->remove();
  if (node != NULL) {
    _control.decrement_full();
    value = node->value();
    release(node);
  }
  return value;
}

template <typename ValueType, template <typename> class NodeType, typename AllocPolicy>
template <typename Callback>
void JfrFullStorage<ValueType, NodeType, AllocPolicy>::iterate(Callback& cb) {
  _queue->iterate(cb);
}

#endif // SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEFULLLIST_INLINE_HPP
