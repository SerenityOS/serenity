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

#ifndef SHARE_GC_Z_ZLOCK_INLINE_HPP
#define SHARE_GC_Z_ZLOCK_INLINE_HPP

#include "gc/z/zLock.hpp"

#include "runtime/atomic.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"

inline void ZLock::lock() {
  _lock.lock();
}

inline bool ZLock::try_lock() {
  return _lock.try_lock();
}

inline void ZLock::unlock() {
  _lock.unlock();
}

inline ZReentrantLock::ZReentrantLock() :
    _lock(),
    _owner(NULL),
    _count(0) {}

inline void ZReentrantLock::lock() {
  Thread* const thread = Thread::current();
  Thread* const owner = Atomic::load(&_owner);

  if (owner != thread) {
    _lock.lock();
    Atomic::store(&_owner, thread);
  }

  _count++;
}

inline void ZReentrantLock::unlock() {
  assert(is_owned(), "Invalid owner");
  assert(_count > 0, "Invalid count");

  _count--;

  if (_count == 0) {
    Atomic::store(&_owner, (Thread*)NULL);
    _lock.unlock();
  }
}

inline bool ZReentrantLock::is_owned() const {
  Thread* const thread = Thread::current();
  Thread* const owner = Atomic::load(&_owner);
  return owner == thread;
}

inline void ZConditionLock::lock() {
  _lock.lock();
}

inline bool ZConditionLock::try_lock() {
  return _lock.try_lock();
}

inline void ZConditionLock::unlock() {
  _lock.unlock();
}

inline bool ZConditionLock::wait(uint64_t millis) {
  return _lock.wait(millis) == OS_OK;
}

inline void ZConditionLock::notify() {
  _lock.notify();
}

inline void ZConditionLock::notify_all() {
  _lock.notify_all();
}

template <typename T>
inline ZLocker<T>::ZLocker(T* lock) :
    _lock(lock) {
  if (_lock != NULL) {
    _lock->lock();
  }
}

template <typename T>
inline ZLocker<T>::~ZLocker() {
  if (_lock != NULL) {
    _lock->unlock();
  }
}

#endif // SHARE_GC_Z_ZLOCK_INLINE_HPP
