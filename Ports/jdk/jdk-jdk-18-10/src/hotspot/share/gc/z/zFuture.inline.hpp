/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZFUTURE_INLINE_HPP
#define SHARE_GC_Z_ZFUTURE_INLINE_HPP

#include "gc/z/zFuture.hpp"

#include "runtime/semaphore.inline.hpp"
#include "runtime/thread.hpp"

template <typename T>
inline ZFuture<T>::ZFuture() :
    _value() {}

template <typename T>
inline void ZFuture<T>::set(T value) {
  // Set value
  _value = value;

  // Notify waiter
  _sema.signal();
}

template <typename T>
inline T ZFuture<T>::get() {
  // Wait for notification
  Thread* const thread = Thread::current();
  if (thread->is_Java_thread()) {
    _sema.wait_with_safepoint_check(JavaThread::cast(thread));
  } else {
    _sema.wait();
  }

  // Return value
  return _value;
}

#endif // SHARE_GC_Z_ZFUTURE_INLINE_HPP
