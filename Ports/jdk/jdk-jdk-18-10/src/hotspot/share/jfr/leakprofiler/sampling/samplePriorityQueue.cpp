/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "jfr/leakprofiler/sampling/samplePriorityQueue.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"

SamplePriorityQueue::SamplePriorityQueue(size_t size) :
  _allocated_size(size),
  _count(0),
  _total(0) {
  _items =  NEW_C_HEAP_ARRAY(ObjectSample*, size, mtTracing);
  memset(_items, 0, sizeof(ObjectSample*) * size);
}

SamplePriorityQueue::~SamplePriorityQueue() {
  FREE_C_HEAP_ARRAY(ObjectSample*, _items);
  _items = NULL;
}

void SamplePriorityQueue::push(ObjectSample* item) {
  assert(item != NULL, "invariant");
  assert(_items[_count] == NULL, "invariant");

  _items[_count] = item;
  _items[_count]->set_index(_count);
  _count++;
  moveUp(_count - 1);
  _total += item->span();
}

size_t SamplePriorityQueue::total() const {
  return _total;
}

ObjectSample* SamplePriorityQueue::pop() {
  if (_count == 0) {
    return NULL;
  }

  ObjectSample* const s = _items[0];
  assert(s != NULL, "invariant");
  swap(0, _count - 1);
  _count--;
  assert(s == _items[_count], "invariant");
  // clear from heap
  _items[_count] = NULL;
  moveDown(0);
  _total -= s->span();
  return s;
}

void SamplePriorityQueue::swap(int i, int j) {
  ObjectSample* tmp = _items[i];
  _items[i] = _items[j];
  _items[j] = tmp;
  _items[i]->set_index(i);
  _items[j]->set_index(j);
}

static int left(int i) {
  return 2 * i + 1;
}

static int right(int i) {
  return 2 * i + 2;
}

static int parent(int i) {
  return (i - 1) / 2;
}

void SamplePriorityQueue::moveDown(int i) {
  do {
    int j = -1;
    int r = right(i);
    if (r < _count && _items[r]->span() < _items[i]->span()) {
      int l = left(i);
      if (_items[l]->span() < _items[r]->span()) {
        j = l;
      } else {
        j = r;
      }
    } else {
      int l = left(i);
      if (l < _count && _items[l]->span() < _items[i]->span()) {
        j = l;
      }
    }
    if (j >= 0) {
      swap(i, j);
    }
    i = j;
  } while (i >= 0);

}

void SamplePriorityQueue::moveUp(int i) {
  int p = parent(i);
  while (i > 0 && _items[i]->span() < _items[p]->span()) {
    swap(i,p);
    i = p;
    p = parent(i);
  }
}

void SamplePriorityQueue::remove(ObjectSample* s) {
  assert(s != NULL, "invariant");
  const size_t realSpan = s->span();
  s->set_span(0);
  moveUp(s->index());
  s->set_span(realSpan);
  pop();
}

int SamplePriorityQueue::count() const {
  return _count;
}

const ObjectSample* SamplePriorityQueue::peek() const {
  return _count == 0 ? NULL : _items[0];
}

ObjectSample* SamplePriorityQueue::item_at(int index) {
  assert(index >= 0 && index < _count, "out of range");
  return _items[index];
}
