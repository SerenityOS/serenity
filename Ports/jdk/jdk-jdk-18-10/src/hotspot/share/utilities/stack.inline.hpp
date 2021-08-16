/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_STACK_INLINE_HPP
#define SHARE_UTILITIES_STACK_INLINE_HPP

#include "utilities/stack.hpp"

#include "memory/allocation.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"

template <MEMFLAGS F> StackBase<F>::StackBase(size_t segment_size, size_t max_cache_size,
                     size_t max_size):
  _seg_size(segment_size),
  _max_size(adjust_max_size(max_size, segment_size)),
  _max_cache_size(max_cache_size)
{
  assert(_max_size % _seg_size == 0, "not a multiple");
}

template <MEMFLAGS F> size_t StackBase<F>::adjust_max_size(size_t max_size, size_t seg_size)
{
  assert(seg_size > 0, "cannot be 0");
  assert(max_size >= seg_size || max_size == 0, "max_size too small");
  const size_t limit = max_uintx - (seg_size - 1);
  if (max_size == 0 || max_size > limit) {
    max_size = limit;
  }
  return (max_size + seg_size - 1) / seg_size * seg_size;
}

template <class E, MEMFLAGS F>
Stack<E, F>::Stack(size_t segment_size, size_t max_cache_size, size_t max_size):
  StackBase<F>(adjust_segment_size(segment_size), max_cache_size, max_size)
{
  reset(true);
}

template <class E, MEMFLAGS F>
void Stack<E, F>::push(E item)
{
  assert(!is_full(), "pushing onto a full stack");
  size_t index = this->_cur_seg_size;
  if (index == this->_seg_size) {
    push_segment();
    index = 0;                  // Instead of fetching known zero _cur_seg_size.
  }
  this->_cur_seg[index] = item;
  this->_cur_seg_size = index + 1;
}

template <class E, MEMFLAGS F>
E Stack<E, F>::pop()
{
  assert(!is_empty(), "popping from an empty stack");
  // _cur_seg_size is never 0 if not empty.  pop that empties a
  // segment also pops the segment.  push that adds a segment always
  // adds an entry to the new segment.
  assert(this->_cur_seg_size != 0, "invariant");
  size_t index = --this->_cur_seg_size;
  E result = _cur_seg[index];
  if (index == 0) pop_segment();
  return result;
}

template <class E, MEMFLAGS F>
void Stack<E, F>::clear(bool clear_cache)
{
  free_segments(_cur_seg);
  if (clear_cache) free_segments(_cache);
  reset(clear_cache);
}

template <class E, MEMFLAGS F>
size_t Stack<E, F>::adjust_segment_size(size_t seg_size)
{
  const size_t elem_sz = sizeof(E);
  const size_t ptr_sz = sizeof(E*);
  assert(elem_sz % ptr_sz == 0 || ptr_sz % elem_sz == 0, "bad element size");
  if (elem_sz < ptr_sz) {
    return align_up(seg_size * elem_sz, ptr_sz) / elem_sz;
  }
  return seg_size;
}

template <class E, MEMFLAGS F>
size_t Stack<E, F>::link_offset() const
{
  return align_up(this->_seg_size * sizeof(E), sizeof(E*));
}

template <class E, MEMFLAGS F>
size_t Stack<E, F>::segment_bytes() const
{
  return link_offset() + sizeof(E*);
}

template <class E, MEMFLAGS F>
E** Stack<E, F>::link_addr(E* seg) const
{
  return (E**) ((char*)seg + link_offset());
}

template <class E, MEMFLAGS F>
E* Stack<E, F>::get_link(E* seg) const
{
  return *link_addr(seg);
}

template <class E, MEMFLAGS F>
E* Stack<E, F>::set_link(E* new_seg, E* old_seg)
{
  *link_addr(new_seg) = old_seg;
  return new_seg;
}

template <class E, MEMFLAGS F>
E* Stack<E, F>::alloc(size_t bytes)
{
  return (E*) NEW_C_HEAP_ARRAY(char, bytes, F);
}

template <class E, MEMFLAGS F>
void Stack<E, F>::free(E* addr, size_t bytes)
{
  FREE_C_HEAP_ARRAY(char, (char*) addr);
}

// Stack is used by the GC code and in some hot paths a lot of the Stack
// code gets inlined. This is generally good, but when too much code has
// been inlined, further inlining in the caller might be inhibited. So
// prevent infrequent slow path segment manipulation from being inlined.
template <class E, MEMFLAGS F>
NOINLINE void Stack<E, F>::push_segment()
{
  assert(this->_cur_seg_size == this->_seg_size, "current segment is not full");
  E* next;
  if (this->_cache_size > 0) {
    // Use a cached segment.
    next = _cache;
    _cache = get_link(_cache);
    --this->_cache_size;
  } else {
    next = alloc(segment_bytes());
    DEBUG_ONLY(zap_segment(next, true);)
  }
  const bool at_empty_transition = is_empty();
  this->_cur_seg = set_link(next, _cur_seg);
  this->_cur_seg_size = 0;
  this->_full_seg_size += at_empty_transition ? 0 : this->_seg_size;
  DEBUG_ONLY(verify(at_empty_transition);)
}

template <class E, MEMFLAGS F>
NOINLINE void Stack<E, F>::pop_segment()
{
  assert(this->_cur_seg_size == 0, "current segment is not empty");
  E* const prev = get_link(_cur_seg);
  if (this->_cache_size < this->_max_cache_size) {
    // Add the current segment to the cache.
    DEBUG_ONLY(zap_segment(_cur_seg, false);)
    _cache = set_link(_cur_seg, _cache);
    ++this->_cache_size;
  } else {
    DEBUG_ONLY(zap_segment(_cur_seg, true);)
    free(_cur_seg, segment_bytes());
  }
  const bool at_empty_transition = prev == NULL;
  this->_cur_seg = prev;
  this->_cur_seg_size = this->_seg_size;
  this->_full_seg_size -= at_empty_transition ? 0 : this->_seg_size;
  DEBUG_ONLY(verify(at_empty_transition);)
}

template <class E, MEMFLAGS F>
void Stack<E, F>::free_segments(E* seg)
{
  const size_t bytes = segment_bytes();
  while (seg != NULL) {
    E* const prev = get_link(seg);
    free(seg, bytes);
    seg = prev;
  }
}

template <class E, MEMFLAGS F>
void Stack<E, F>::reset(bool reset_cache)
{
  this->_cur_seg_size = this->_seg_size; // So push() will alloc a new segment.
  this->_full_seg_size = 0;
  _cur_seg = NULL;
  if (reset_cache) {
    this->_cache_size = 0;
    _cache = NULL;
  }
}

#ifdef ASSERT
template <class E, MEMFLAGS F>
void Stack<E, F>::verify(bool at_empty_transition) const
{
  assert(size() <= this->max_size(), "stack exceeded bounds");
  assert(this->cache_size() <= this->max_cache_size(), "cache exceeded bounds");
  assert(this->_cur_seg_size <= this->segment_size(), "segment index exceeded bounds");

  assert(this->_full_seg_size % this->_seg_size == 0, "not a multiple");
  assert(at_empty_transition || is_empty() == (size() == 0), "mismatch");
  assert((_cache == NULL) == (this->cache_size() == 0), "mismatch");

  if (is_empty()) {
    assert(this->_cur_seg_size == this->segment_size(), "sanity");
  }
}

template <class E, MEMFLAGS F>
void Stack<E, F>::zap_segment(E* seg, bool zap_link_field) const
{
  if (!ZapStackSegments) return;
  const size_t zap_bytes = segment_bytes() - (zap_link_field ? 0 : sizeof(E*));
  Copy::fill_to_bytes(seg, zap_bytes, badStackSegVal);
}
#endif

template <class E, MEMFLAGS F>
E* ResourceStack<E, F>::alloc(size_t bytes)
{
  return (E*) resource_allocate_bytes(bytes);
}

template <class E, MEMFLAGS F>
void ResourceStack<E, F>::free(E* addr, size_t bytes)
{
  resource_free_bytes((char*) addr, bytes);
}

template <class E, MEMFLAGS F>
void StackIterator<E, F>::sync()
{
  _full_seg_size = _stack._full_seg_size;
  _cur_seg_size = _stack._cur_seg_size;
  _cur_seg = _stack._cur_seg;
}

template <class E, MEMFLAGS F>
E* StackIterator<E, F>::next_addr()
{
  assert(!is_empty(), "no items left");
  if (_cur_seg_size == 1) {
    E* addr = _cur_seg;
    _cur_seg = _stack.get_link(_cur_seg);
    _cur_seg_size = _stack.segment_size();
    _full_seg_size -= _stack.segment_size();
    return addr;
  }
  return _cur_seg + --_cur_seg_size;
}

#endif // SHARE_UTILITIES_STACK_INLINE_HPP
