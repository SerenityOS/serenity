/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_INLINE_HPP
#define SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_INLINE_HPP

#include "gc/g1/g1OopStarChunkedList.hpp"

#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"

template <typename T>
inline void G1OopStarChunkedList::push(ChunkedList<T*, mtGC>** field, T* p) {
  ChunkedList<T*, mtGC>* list = *field;
  if (list == NULL) {
    *field = new ChunkedList<T*, mtGC>();
    _used_memory += sizeof(ChunkedList<T*, mtGC>);
  } else if (list->is_full()) {
    ChunkedList<T*, mtGC>* next = new ChunkedList<T*, mtGC>();
    next->set_next_used(list);
    *field = next;
    _used_memory += sizeof(ChunkedList<T*, mtGC>);
  }

  (*field)->push(p);
}

inline void G1OopStarChunkedList::push_root(narrowOop* p) {
  push(&_croots, p);
}

inline void G1OopStarChunkedList::push_root(oop* p) {
  push(&_roots, p);
}

inline void G1OopStarChunkedList::push_oop(narrowOop* p) {
  push(&_coops, p);
}

inline void G1OopStarChunkedList::push_oop(oop* p) {
  push(&_oops, p);
}

template <typename T>
void G1OopStarChunkedList::delete_list(ChunkedList<T*, mtGC>* c) {
  while (c != NULL) {
    ChunkedList<T*, mtGC>* next = c->next_used();
    delete c;
    c = next;
  }
}

template <typename T>
size_t G1OopStarChunkedList::chunks_do(ChunkedList<T*, mtGC>* head, OopClosure* cl) {
  size_t result = 0;
  for (ChunkedList<T*, mtGC>* c = head; c != NULL; c = c->next_used()) {
    result += c->size();
    for (size_t i = 0; i < c->size(); i++) {
      T* p = c->at(i);
      cl->do_oop(p);
    }
  }
  return result;
}

#endif // SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_INLINE_HPP
