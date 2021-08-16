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

#ifndef SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_INLINE_HPP

#include "jfr/utilities/jfrConcurrentQueue.hpp"

#include "jfr/utilities/jfrConcurrentLinkedListHost.inline.hpp"
#include "jfr/utilities/jfrVersionSystem.inline.hpp"

template <typename NodeType, typename AllocPolicy>
JfrConcurrentQueue<NodeType, AllocPolicy>::JfrConcurrentQueue() : _list(NULL), _head(), _last(), _tail(), _version_system() {
  _head._next = const_cast<NodePtr>(&_tail);
  _last._next = const_cast<NodePtr>(&_tail);
}

template <typename NodeType, typename AllocPolicy>
bool JfrConcurrentQueue<NodeType, AllocPolicy>::initialize() {
  assert(_list == NULL, "invariant");
  _list = new JfrConcurrentLinkedListHost<JfrConcurrentQueue<NodeType, AllocPolicy>, HeadNode, AllocPolicy>(this);
  return _list != NULL && _list->initialize();
}

template <typename NodeType, typename AllocPolicy>
inline bool JfrConcurrentQueue<NodeType, AllocPolicy>::is_empty() const {
  return Atomic::load_acquire(&_head._next) == &_tail;
}

template <typename NodeType, typename AllocPolicy>
inline bool JfrConcurrentQueue<NodeType, AllocPolicy>::is_nonempty() const {
  return !is_empty();
}

template <typename NodeType, typename AllocPolicy>
void JfrConcurrentQueue<NodeType, AllocPolicy>::add(typename JfrConcurrentQueue<NodeType, AllocPolicy>::NodePtr node) {
  _list->insert_tail(node, &_head, &_last, &_tail);
}

template <typename NodeType, typename AllocPolicy>
typename JfrConcurrentQueue<NodeType, AllocPolicy>::NodePtr JfrConcurrentQueue<NodeType, AllocPolicy>::remove() {
  return _list->remove(&_head, &_tail, &_last, false);
}

template <typename NodeType, typename AllocPolicy>
template <typename Callback>
void JfrConcurrentQueue<NodeType, AllocPolicy>::iterate(Callback& cb) {
  _list->iterate(&_head, &_tail, cb);
}

template <typename NodeType, typename AllocPolicy>
inline JfrVersionSystem::Handle JfrConcurrentQueue<NodeType, AllocPolicy>::get_version_handle() {
  return _version_system.get();
}

template <typename NodeType, typename AllocPolicy>
bool JfrConcurrentQueue<NodeType, AllocPolicy>::in_list(const NodeType* node) const {
  assert(node != NULL, "invariant");
  return _list->in_list(node, const_cast<NodePtr>(&_head), &_tail);
}

#endif // SHARE_JFR_UTILITIES_JFRCONCURRENTQUEUE_INLINE_HPP
