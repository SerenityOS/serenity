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

#ifndef SHARE_JFR_UTILITIES_JFRRELATION_HPP
#define SHARE_JFR_UTILITIES_JFRRELATION_HPP

#include "jfr/utilities/jfrNode.hpp"
#include "jfr/utilities/jfrTypes.hpp"

inline int compare_traceid(const traceid& lhs, const traceid& rhs) {
  return lhs > rhs ? 1 : (lhs < rhs) ? -1 : 0;
}

inline int sort_traceid(traceid* lhs, traceid* rhs) {
  return compare_traceid(*lhs, *rhs);
}

class Klass;

inline int compare_klasses(const Klass*const& lhs, const Klass*const& rhs) {
  return lhs > rhs ? 1 : (lhs < rhs) ? -1 : 0;
}

inline int sort_klasses(const Klass* lhs, const Klass* rhs) {
  return compare_klasses(lhs, rhs);
}

template <typename Node>
class LessThan {
 private:
  typename Node::Key _key;
 public:
  LessThan(typename Node::Key key) : _key(key) {}
  bool operator()(const Node* node) { return node->key() < _key; }
};

template <typename Node>
class GreaterThan {
 private:
  typename Node::Key _key;
 public:
  GreaterThan(typename Node::Key key) : _key(key) {}
  bool operator()(const Node* node) { return node->key() > _key; }
};

/*
 * Using a contradiction as a search predicate amounts
 * to using the physical order of the list (key is neglected).
 * [LessThan relation -> Ascending order ] -> the minimal element.
 * [GreaterThan relation -> Descending order] -> the maximal element.
 */
template <typename Node>
class HeadNode {
 public:
  HeadNode(const Node* node = NULL) {}
  bool operator()(const Node* current, const Node* next) {
    return is_marked_for_removal(next);
  }
};

/*
 * Using a tautology as a search predicate amounts
 * to using the physical store order of the list (key is neglected).
 * [LessThan relation -> Ascending order ] -> the maximal element.
 * [GreaterThan relation -> Descending order] -> the minimal element.
 */
template <typename Node>
class LastNode {
 public:
  LastNode(const Node* node = NULL) {}
  bool operator()(const Node* current, const Node* next ) {
    return true;
  }
};

template <typename Node>
class Identity {
 private:
  const  Node* _target;
  bool _found;
 public:
  Identity(const Node* node = NULL) : _target(node), _found(false) {}
  bool operator()(const Node* current, const Node* next) {
    assert(current != NULL, "invariant");
    assert(next != NULL, "invariant");
    if (!_found && current == _target) {
      _found = true;
    }
    return is_marked_for_removal(next) || !_found;
  }
};

#endif // SHARE_JFR_UTILITIES_JFRRELATION_HPP
