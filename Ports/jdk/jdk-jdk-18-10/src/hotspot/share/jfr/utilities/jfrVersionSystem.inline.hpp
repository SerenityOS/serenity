/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_INLINE_HPP
#define SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_INLINE_HPP

#include "jfr/utilities/jfrVersionSystem.hpp"

#include "runtime/atomic.hpp"
#include "runtime/os.hpp"

inline JfrVersionSystem::JfrVersionSystem() : _tip(), _head(NULL) {
  _tip._value = 1;
}

inline JfrVersionSystem::~JfrVersionSystem() {
  reset();
}

inline void JfrVersionSystem::reset() {
  NodePtr node = _head;
  while (node != NULL) {
    NodePtr next = node->_next;
    delete node;
    node = next;
  }
  _head = NULL;
  _tip._value = 1;
}

inline JfrVersionSystem::Type JfrVersionSystem::tip() const {
  return Atomic::load(&_tip._value);
}

inline JfrVersionSystem::Type JfrVersionSystem::inc_tip() {
  traceid cmp;
  traceid xchg;
  do {
    cmp = _tip._value;
    xchg = cmp + 1;
  } while (Atomic::cmpxchg(&_tip._value, cmp, xchg) != cmp);
  return xchg;
}

inline JfrVersionSystem::NodePtr JfrVersionSystem::acquire() {
  NodePtr node = _head;
  // free
  while (node != NULL) {
    if (node->_live || Atomic::cmpxchg(&node->_live, false, true)) {
      node = node->_next;
      continue;
    }
    DEBUG_ONLY(assert_state(node);)
    return node;
  }
  // new
  node = new Node(this);
  NodePtr next;
  do {
    next = _head;
    node->_next = next;
  } while (Atomic::cmpxchg(&_head, next, node) != next);
  DEBUG_ONLY(assert_state(node);)
  return node;
}

inline JfrVersionSystem::Handle JfrVersionSystem::get() {
  return Handle::make(acquire());
}

inline JfrVersionSystem::Node::Node(JfrVersionSystem* system) : _system(system), _next(NULL), _version(0), _live(true) {}

inline traceid JfrVersionSystem::Node::version() const {
  return _version;
}

inline void JfrVersionSystem::Node::set(traceid version) const {
  Atomic::release_store_fence(&_version, version);
}

inline void JfrVersionSystem::Node::add_ref() const {
  _ref_counter.inc();
}

inline void JfrVersionSystem::Node::remove_ref() const {
  if (_ref_counter.dec()) {
    assert(_live, "invariant");
    set(0);
    _live = false;
  }
}

inline void JfrVersionSystem::Node::checkout() {
  set(_system->tip());
  assert(version() != 0, "invariant");
}

inline void JfrVersionSystem::Node::commit() {
  assert(version() != 0, "invariant");
  // A commit consist of an atomic increment of the tip.
  const Type commit_version = _system->inc_tip();
  // Release this checkout.
  set(0);
  // Await release of checkouts for earlier versions.
  _system->await(commit_version);
}

inline JfrVersionSystem::NodePtr
JfrVersionSystem::synchronize_with(JfrVersionSystem::Type version, JfrVersionSystem::NodePtr node) const {
  assert(version <= tip(), "invariant");
  while (node != NULL) {
    const Type checkedout = Atomic::load_acquire(&node->_version);
    if (checkedout > 0 && checkedout < version) {
      return node;
    }
    node = node->_next;
  }
  return NULL;
}

inline void JfrVersionSystem::await(JfrVersionSystem::Type version) {
  assert(version > 0, "invariant");
  static const int backoff_unit_ns = 10;
  int backoff_factor = 1;
  NodePtr last = _head;
  while (true) {
    last = synchronize_with(version, last);
    if (last == NULL) {
      return;
    }
    os::naked_short_nanosleep(backoff_unit_ns * backoff_factor++);
  }
}

#ifdef ASSERT
inline void JfrVersionSystem::assert_state(const JfrVersionSystem::Node* node) const {
  assert(node != NULL, "invariant");
  assert(node->_live, "invariant");
  assert(node->_version == 0, "invariant");
  assert(node->_ref_counter.current() == 0, "invariant");
}
#endif // ASSERT

#endif // SHARE_JFR_UTILITIES_JFRVERSIONSYSTEM_INLINE_HPP
