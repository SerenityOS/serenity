/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_HPP
#define SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_HPP

#include "oops/oopsHierarchy.hpp"
#include "utilities/chunkedList.hpp"

class OopClosure;

class G1OopStarChunkedList : public CHeapObj<mtGC> {
  size_t _used_memory;

  ChunkedList<oop*, mtGC>* _roots;
  ChunkedList<narrowOop*, mtGC>* _croots;
  ChunkedList<oop*, mtGC>* _oops;
  ChunkedList<narrowOop*, mtGC>* _coops;

  template <typename T> void delete_list(ChunkedList<T*, mtGC>* c);

  template <typename T>
  size_t chunks_do(ChunkedList<T*, mtGC>* head,
                   OopClosure* cl);

  template <typename T>
  inline void push(ChunkedList<T*, mtGC>** field, T* p);

 public:
  G1OopStarChunkedList() : _used_memory(0), _roots(NULL), _croots(NULL), _oops(NULL), _coops(NULL) {}
  ~G1OopStarChunkedList();

  size_t used_memory() { return _used_memory; }

  size_t oops_do(OopClosure* obj_cl, OopClosure* root_cl);

  inline void push_oop(oop* p);
  inline void push_oop(narrowOop* p);
  inline void push_root(oop* p);
  inline void push_root(narrowOop* p);
};

#endif // SHARE_GC_G1_G1OOPSTARCHUNKEDLIST_HPP
