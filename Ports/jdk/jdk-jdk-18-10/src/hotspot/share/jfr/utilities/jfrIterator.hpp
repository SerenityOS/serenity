/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRITERATOR_HPP
#define SHARE_JFR_UTILITIES_JFRITERATOR_HPP

#include "memory/allocation.hpp"

template <typename List>
class StopOnNullCondition {
  typedef typename List::Node Node;
 private:
  List& _list;
  mutable Node* _node;
 public:
  StopOnNullCondition(List& list) : _list(list), _node(list.head()) {}
  bool has_next() const {
    return _node != NULL;
  }
  Node* next() const {
    assert(_node != NULL, "invariant");
    Node* temp = _node;
    _node = (Node*)_node->_next;
    return temp;
  }
};

template <typename List>
class StopOnNullConditionRemoval {
  typedef typename List::Node Node;
 private:
  List& _list;
  mutable Node* _node;
 public:
  StopOnNullConditionRemoval(List& list) : _list(list), _node(NULL) {}
  bool has_next() const {
    _node = _list.remove();
    return _node != NULL;
  }
  Node* next() const {
    assert(_node != NULL, "invariant");
    return _node;
  }
};

template <typename List, template <typename> class ContinuationPredicate>
class Navigator {
 public:
  typedef typename List::Node Node;
  Navigator(List& list) : _continuation(list) {}
  bool has_next() const {
    return _continuation.has_next();
  }
  Node* next() const {
    return _continuation.next();
  }
 private:
  ContinuationPredicate<List> _continuation;
  mutable Node* _node;
};

template <typename List>
class NavigatorStopOnNull : public Navigator<List, StopOnNullCondition> {
 public:
  NavigatorStopOnNull(List& list) : Navigator<List, StopOnNullCondition>(list) {}
};

template <typename List>
class NavigatorStopOnNullRemoval : public Navigator<List, StopOnNullConditionRemoval> {
public:
  NavigatorStopOnNullRemoval(List& list) : Navigator<List, StopOnNullConditionRemoval>(list) {}
};

template<typename List, template <typename> class Navigator, typename AP = StackObj>
class IteratorHost : public AP {
 private:
  Navigator<List> _navigator;
 public:
  typedef typename List::NodePtr NodePtr;
  IteratorHost(List& list) : AP(), _navigator(list) {}
  void reset() { _navigator.reset(); }
  bool has_next() const { return _navigator.has_next(); }
  NodePtr next() const { return _navigator.next(); }
};

template<typename List, typename AP = StackObj>
class StopOnNullIterator : public IteratorHost<List, NavigatorStopOnNull, AP> {
 public:
  StopOnNullIterator(List& list) : IteratorHost<List, NavigatorStopOnNull, AP>(list) {}
};

template<typename List, typename AP = StackObj>
class StopOnNullIteratorRemoval : public IteratorHost<List, NavigatorStopOnNullRemoval, AP> {
public:
  StopOnNullIteratorRemoval(List& list) : IteratorHost<List, NavigatorStopOnNullRemoval, AP>(list) {}
};

#endif // SHARE_JFR_UTILITIES_JFRITERATOR_HPP
