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

#ifndef SHARE_GC_Z_ZLIST_HPP
#define SHARE_GC_Z_ZLIST_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

template <typename T> class ZList;

// Element in a doubly linked list
template <typename T>
class ZListNode {
  friend class ZList<T>;

private:
  ZListNode<T>* _next;
  ZListNode<T>* _prev;

  NONCOPYABLE(ZListNode);

  void verify_links() const;
  void verify_links_linked() const;
  void verify_links_unlinked() const;

public:
  ZListNode();
  ~ZListNode();
};

// Doubly linked list
template <typename T>
class ZList {
private:
  ZListNode<T> _head;
  size_t       _size;

  NONCOPYABLE(ZList);

  void verify_head() const;

  void insert(ZListNode<T>* before, ZListNode<T>* node);

  ZListNode<T>* cast_to_inner(T* elem) const;
  T* cast_to_outer(ZListNode<T>* node) const;

public:
  ZList();

  size_t size() const;
  bool is_empty() const;

  T* first() const;
  T* last() const;
  T* next(T* elem) const;
  T* prev(T* elem) const;

  void insert_first(T* elem);
  void insert_last(T* elem);
  void insert_before(T* before, T* elem);
  void insert_after(T* after, T* elem);

  void remove(T* elem);
  T* remove_first();
  T* remove_last();
};

template <typename T, bool Forward>
class ZListIteratorImpl : public StackObj {
private:
  const ZList<T>* const _list;
  T*                    _next;

public:
  ZListIteratorImpl(const ZList<T>* list);

  bool next(T** elem);
};

template <typename T, bool Forward>
class ZListRemoveIteratorImpl : public StackObj {
private:
  ZList<T>* const _list;

public:
  ZListRemoveIteratorImpl(ZList<T>* list);

  bool next(T** elem);
};

template <typename T> using ZListIterator = ZListIteratorImpl<T, true /* Forward */>;
template <typename T> using ZListReverseIterator = ZListIteratorImpl<T, false /* Forward */>;
template <typename T> using ZListRemoveIterator = ZListRemoveIteratorImpl<T, true /* Forward */>;

#endif // SHARE_GC_Z_ZLIST_HPP
