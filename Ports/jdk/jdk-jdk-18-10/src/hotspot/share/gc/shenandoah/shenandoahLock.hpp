/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHLOCK_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHLOCK_HPP

#include "gc/shenandoah/shenandoahPadding.hpp"
#include "memory/allocation.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"

class ShenandoahLock  {
private:
  enum LockState { unlocked = 0, locked = 1 };

  shenandoah_padding(0);
  volatile int _state;
  shenandoah_padding(1);
  volatile Thread* _owner;
  shenandoah_padding(2);

public:
  ShenandoahLock() : _state(unlocked), _owner(NULL) {};

  void lock() {
#ifdef ASSERT
    assert(_owner != Thread::current(), "reentrant locking attempt, would deadlock");
#endif
    Thread::SpinAcquire(&_state, "Shenandoah Heap Lock");
#ifdef ASSERT
    assert(_state == locked, "must be locked");
    assert(_owner == NULL, "must not be owned");
    _owner = Thread::current();
#endif
  }

  void unlock() {
#ifdef ASSERT
    assert (_owner == Thread::current(), "sanity");
    _owner = NULL;
#endif
    Thread::SpinRelease(&_state);
  }

  bool owned_by_self() {
#ifdef ASSERT
    return _state == locked && _owner == Thread::current();
#else
    ShouldNotReachHere();
    return false;
#endif
  }
};

class ShenandoahLocker : public StackObj {
private:
  ShenandoahLock* const _lock;
public:
  ShenandoahLocker(ShenandoahLock* lock) : _lock(lock) {
    if (_lock != NULL) {
      _lock->lock();
    }
  }

  ~ShenandoahLocker() {
    if (_lock != NULL) {
      _lock->unlock();
    }
  }
};

class ShenandoahSimpleLock {
private:
  os::PlatformMonitor   _lock; // native lock
public:
  ShenandoahSimpleLock();

  virtual void lock();
  virtual void unlock();
};

class ShenandoahReentrantLock : public ShenandoahSimpleLock {
private:
  Thread* volatile      _owner;
  uint64_t              _count;

public:
  ShenandoahReentrantLock();
  ~ShenandoahReentrantLock();

  virtual void lock();
  virtual void unlock();

  // If the lock already owned by this thread
  bool owned_by_self() const ;
};

class ShenandoahReentrantLocker : public StackObj {
private:
  ShenandoahReentrantLock* const _lock;

public:
  ShenandoahReentrantLocker(ShenandoahReentrantLock* lock) :
    _lock(lock) {
    if (_lock != NULL) {
      _lock->lock();
    }
  }

  ~ShenandoahReentrantLocker() {
    if (_lock != NULL) {
      assert(_lock->owned_by_self(), "Must be owner");
      _lock->unlock();
    }
  }
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHLOCK_HPP
