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

#ifndef SHARE_JFR_UTILITIES_JFRNODE_HPP
#define SHARE_JFR_UTILITIES_JFRNODE_HPP

#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"

const uint64_t JFR_NODE_LOGICAL_EXCISION_BIT = 1;
const uint64_t JFR_NODE_LOGICAL_INSERTION_BIT = 2;
const uint64_t JFR_NODE_MASK = ~(JFR_NODE_LOGICAL_INSERTION_BIT | JFR_NODE_LOGICAL_EXCISION_BIT);

template <typename Node>
inline bool cas(Node** address, Node* current, Node* exchange) {
  return Atomic::cmpxchg(address, current, exchange) == current;
}

template <typename Node>
inline bool is_marked_for_removal(const Node* ptr) {
  return ((uint64_t)ptr & JFR_NODE_LOGICAL_EXCISION_BIT) == JFR_NODE_LOGICAL_EXCISION_BIT;
}

template <typename Node>
inline bool is_marked_for_insertion(const Node* ptr) {
  return ((uint64_t)ptr & JFR_NODE_LOGICAL_INSERTION_BIT) == JFR_NODE_LOGICAL_INSERTION_BIT;
}

template <typename Node>
inline Node* set_excision_bit(const Node* ptr) {
  return (Node*)(((uint64_t)ptr) | JFR_NODE_LOGICAL_EXCISION_BIT);
}

template <typename Node>
inline Node* set_insertion_bit(const Node* ptr) {
  return (Node*)(((uint64_t)ptr) | JFR_NODE_LOGICAL_INSERTION_BIT);
}

template <typename Node>
inline Node* unmask(const Node* ptr) {
  return (Node*)(((uint64_t)ptr) & JFR_NODE_MASK);
}

template <typename Derived, typename Version = traceid>
class JfrLinkedNode : public ResourceObj {
 public:
  typedef Version VersionType;
  Derived* _next;
  JfrLinkedNode() : _next(NULL) {}
  JfrLinkedNode(JfrLinkedNode<Derived, VersionType>* next) : _next(next) {}
};

template <typename V>
class JfrKeyIsThisNode : public JfrLinkedNode<JfrKeyIsThisNode<V> > {
 private:
  V _value;
 public:
  typedef V Value;
  typedef const JfrKeyIsThisNode<V>* Key;
  JfrKeyIsThisNode(const Value value = NULL) : JfrLinkedNode<JfrKeyIsThisNode<V> >(), _value(value) {}
  Key key() const { return this; }
  Value value() const { return _value; }
  void set_value(Value value) { _value = value; }
};

template <typename V>
class JfrValueNode : public JfrLinkedNode<JfrValueNode<V> > {
 private:
  V _value;
 public:
  typedef V Value;
  typedef Value Key;
  JfrValueNode(const Value value = NULL) : JfrLinkedNode<JfrValueNode<V> >(), _value(value) {}
  Key key() const { return value(); }
  Value value() const { return _value; }
  void set_value(Value value) { _value = value; }
};

template <typename V>
class JfrKeyIsFreeSizeNode : public JfrLinkedNode<JfrKeyIsFreeSizeNode<V> > {
 private:
  V _value;
 public:
  typedef V Value;
  typedef size_t Key;
  JfrKeyIsFreeSizeNode(const Value value = NULL) : JfrLinkedNode<JfrKeyIsFreeSizeNode<V> >(), _value(value) {}
  Key key() const { return value()->free_size(); }
  Value value() const { return _value; }
  void set_value(Value value) { _value = value; }
};

#endif // SHARE_JFR_UTILITIES_JFRNODE_HPP
