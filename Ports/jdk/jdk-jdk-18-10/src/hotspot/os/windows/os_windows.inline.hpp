/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OS_WINDOWS_OS_WINDOWS_INLINE_HPP
#define OS_WINDOWS_OS_WINDOWS_INLINE_HPP

// os_windows.hpp included by os.hpp

#include "runtime/os.hpp"
#include "runtime/thread.hpp"

inline bool os::uses_stack_guard_pages() {
  return true;
}

inline bool os::must_commit_stack_guard_pages() {
  return true;
}

// Bang the shadow pages if they need to be touched to be mapped.
inline void os::map_stack_shadow_pages(address sp) {
  // Write to each page of our new frame to force OS mapping.
  // If we decrement stack pointer more than one page
  // the OS may not map an intervening page into our space
  // and may fault on a memory access to interior of our frame.
  const int page_size = os::win32::vm_page_size();
  const size_t n_pages = StackOverflow::stack_shadow_zone_size() / page_size;
  for (size_t pages = 1; pages <= n_pages; pages++) {
    sp -= page_size;
    *sp = 0;
  }
}

inline bool os::numa_has_static_binding()   { return true;   }
inline bool os::numa_has_group_homing()     { return false;  }

// Platform Mutex/Monitor implementation

inline os::PlatformMutex::PlatformMutex() {
  InitializeCriticalSection(&_mutex);
}

inline os::PlatformMutex::~PlatformMutex() {
  DeleteCriticalSection(&_mutex);
}

inline os::PlatformMonitor::PlatformMonitor() {
  InitializeConditionVariable(&_cond);
}

inline os::PlatformMonitor::~PlatformMonitor() {
  // There is no DeleteConditionVariable API
}

inline void os::PlatformMutex::lock() {
  EnterCriticalSection(&_mutex);
}

inline void os::PlatformMutex::unlock() {
  LeaveCriticalSection(&_mutex);
}

inline bool os::PlatformMutex::try_lock() {
  return TryEnterCriticalSection(&_mutex);
}

inline void os::PlatformMonitor::notify() {
  WakeConditionVariable(&_cond);
}

inline void os::PlatformMonitor::notify_all() {
  WakeAllConditionVariable(&_cond);
}

#endif // OS_WINDOWS_OS_WINDOWS_INLINE_HPP
