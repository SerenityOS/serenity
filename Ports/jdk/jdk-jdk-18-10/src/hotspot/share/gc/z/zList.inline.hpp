/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZLIST_INLINE_HPP
#define SHARE_GC_Z_ZLIST_INLINE_HPP

#include "gc/z/zList.hpp"

#include "utilities/debug.hpp"

template <typename T>
inline ZListNode<T>::ZListNode() :
    _next(this),
    _prev(this) {}

template <typename T>
inline ZListNode<T>::~ZListNode() {
  verify_links_unlinked();
}

template <typename T>
inline void ZListNode<T>::verify_links() const {
  assert(_next->_prev == this, "Corrupt list node");
  assert(_prev->_next == this, "Corrupt list node");
}

template <typename T>
inline void ZListNode<T>::verify_links_linked() const {
  assert(_next != this, "Should be in a list");
  assert(_prev != this, "Should be in a list");
  verify_links();
}

template <typename T>
inline void ZListNode<T>::verify_links_unlinked() const {
  assert(_next == this, "Should not be in a list");
  assert(_prev == this, "Should not be in a list");
}

template <typename T>
inline void ZList<T>::verify_head() const {
  _head.verify_links();
}

template <typename T>
inline void ZList<T>::insert(ZListNode<T>* before, ZListNode<T>* node) {
  verify_head();

  before->verify_links();
  node->verify_links_unlinked();

  node->_prev = before;
  node->_next = before->_next;
  before->_next = node;
  node->_next->_prev = node;

  before->verify_links_linked();
  node->verify_links_linked();

  _size++;
}

template <typename T>
inline ZListNode<T>* ZList<T>::cast_to_inner(T* elem) const {
  return &elem->_node;
}

template <typename T>
inline T* ZList<T>::cast_to_outer(ZListNode<T>* node) const {
  return (T*)((uintptr_t)node - offset_of(T, _node));
}

template <typename T>
inline ZList<T>::ZList() :
    _head(),
    _size(0) {
  verify_head();
}

template <typename T>
inline size_t ZList<T>::size() const {
  verify_head();
  return _size;
}

template <typename T>
inline bool ZList<T>::is_empty() const {
  return size() == 0;
}

template <typename T>
inline T* ZList<T>::first() const {
  return is_empty() ? NULL : cast_to_outer(_head._next);
}

template <typename T>
inline T* ZList<T>::last() const {
  return is_empty() ? NULL : cast_to_outer(_head._prev);
}

template <typename T>
inline T* ZList<T>::next(T* elem) const {
  verify_head();

  ZListNode<T>* const node = cast_to_inner(elem);
  node->verify_links_linked();

  ZListNode<T>* const next = node->_next;
  next->verify_links_linked();

  return (next == &_head) ? NULL : cast_to_outer(next);
}

template <typename T>
inline T* ZList<T>::prev(T* elem) const {
  verify_head();

  ZListNode<T>* const node = cast_to_inner(elem);
  node->verify_links_linked();

  ZListNode<T>* const prev = node->_prev;
  prev->verify_links_linked();

  return (prev == &_head) ? NULL : cast_to_outer(prev);
}

template <typename T>
inline void ZList<T>::insert_first(T* elem) {
  insert(&_head, cast_to_inner(elem));
}

template <typename T>
inline void ZList<T>::insert_last(T* elem) {
  insert(_head._prev, cast_to_inner(elem));
}

template <typename T>
inline void ZList<T>::insert_before(T* before, T* elem) {
  insert(cast_to_inner(before)->_prev, cast_to_inner(elem));
}

template <typename T>
inline void ZList<T>::insert_after(T* after, T* elem) {
  insert(cast_to_inner(after), cast_to_inner(elem));
}

template <typename T>
inline void ZList<T>::remove(T* elem) {
  verify_head();

  ZListNode<T>* const node = cast_to_inner(elem);
  node->verify_links_linked();

  ZListNode<T>* const next = node->_next;
  ZListNode<T>* const prev = node->_prev;
  next->verify_links_linked();
  prev->verify_links_linked();

  node->_next = prev->_next;
  node->_prev = next->_prev;
  node->verify_links_unlinked();

  next->_prev = prev;
  prev->_next = next;
  next->verify_links();
  prev->verify_links();

  _size--;
}

template <typename T>
inline T* ZList<T>::remove_first() {
  T* elem = first();
  if (elem != NULL) {
    remove(elem);
  }

  return elem;
}

template <typename T>
inline T* ZList<T>::remove_last() {
  T* elem = last();
  if (elem != NULL) {
    remove(elem);
  }

  return elem;
}

template <typename T, bool Forward>
inline ZListIteratorImpl<T, Forward>::ZListIteratorImpl(const ZList<T>* list) :
    _list(list),
    _next(Forward ? list->first() : list->last()) {}

template <typename T, bool Forward>
inline bool ZListIteratorImpl<T, Forward>::next(T** elem) {
  if (_next != NULL) {
    *elem = _next;
    _next = Forward ? _list->next(_next) : _list->prev(_next);
    return true;
  }

  // No more elements
  return false;
}

template <typename T, bool Forward>
inline ZListRemoveIteratorImpl<T, Forward>::ZListRemoveIteratorImpl(ZList<T>* list) :
    _list(list) {}

template <typename T, bool Forward>
inline bool ZListRemoveIteratorImpl<T, Forward>::next(T** elem) {
  *elem = Forward ? _list->remove_first() : _list->remove_last();
  return *elem != NULL;
}

#endif // SHARE_GC_Z_ZLIST_INLINE_HPP
