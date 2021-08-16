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

#ifndef SHARE_GC_Z_ZLOCK_HPP
#define SHARE_GC_Z_ZLOCK_HPP

#include "memory/allocation.hpp"
#include "runtime/os.hpp"

class ZLock {
private:
  os::PlatformMutex _lock;

public:
  void lock();
  bool try_lock();
  void unlock();
};

class ZReentrantLock {
private:
  ZLock            _lock;
  Thread* volatile _owner;
  uint64_t         _count;

public:
  ZReentrantLock();

  void lock();
  void unlock();

  bool is_owned() const;
};

class ZConditionLock {
private:
  os::PlatformMonitor _lock;

public:
  void lock();
  bool try_lock();
  void unlock();

  bool wait(uint64_t millis = 0);
  void notify();
  void notify_all();
};

template <typename T>
class ZLocker : public StackObj {
private:
  T* const _lock;

public:
  ZLocker(T* lock);
  ~ZLocker();
};

#endif // SHARE_GC_Z_ZLOCK_HPP
