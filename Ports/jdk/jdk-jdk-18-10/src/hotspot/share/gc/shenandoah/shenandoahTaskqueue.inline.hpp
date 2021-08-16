/*
 * Copyright (c) 2016, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_INLINE_HPP

#include "gc/shenandoah/shenandoahTaskqueue.hpp"

#include "gc/shared/taskqueue.inline.hpp"
#include "utilities/stack.inline.hpp"

template <class E, MEMFLAGS F, unsigned int N>
bool BufferedOverflowTaskQueue<E, F, N>::pop(E &t) {
  if (!_buf_empty) {
    t = _elem;
    _buf_empty = true;
    return true;
  }

  if (taskqueue_t::pop_local(t)) {
    return true;
  }

  return taskqueue_t::pop_overflow(t);
}

template <class E, MEMFLAGS F, unsigned int N>
inline bool BufferedOverflowTaskQueue<E, F, N>::push(E t) {
  if (_buf_empty) {
    _elem = t;
    _buf_empty = false;
  } else {
    bool pushed = taskqueue_t::push(_elem);
    assert(pushed, "overflow queue should always succeed pushing");
    _elem = t;
  }
  return true;
}

template <class E, MEMFLAGS F, unsigned int N>
void BufferedOverflowTaskQueue<E, F, N>::clear() {
    _buf_empty = true;
    taskqueue_t::set_empty();
    taskqueue_t::overflow_stack()->clear();
}


#endif // SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_INLINE_HPP
