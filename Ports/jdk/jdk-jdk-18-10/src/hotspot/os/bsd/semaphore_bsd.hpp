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
 *
 */

#ifndef OS_BSD_SEMAPHORE_BSD_HPP
#define OS_BSD_SEMAPHORE_BSD_HPP

#include "utilities/globalDefinitions.hpp"

#ifndef __APPLE__
// Use POSIX semaphores.
# include "semaphore_posix.hpp"

#else
// OS X doesn't support unnamed POSIX semaphores, so the implementation in os_posix.cpp can't be used.
# include "memory/allocation.hpp"
# include <mach/semaphore.h>

class OSXSemaphore : public CHeapObj<mtInternal>{
  semaphore_t _semaphore;

  NONCOPYABLE(OSXSemaphore);

 public:
  OSXSemaphore(uint value = 0);
  ~OSXSemaphore();

  void signal(uint count = 1);

  void wait();

  bool trywait();

  // wait until the given relative time elapses
  bool timedwait(int64_t millis);
};

typedef OSXSemaphore SemaphoreImpl;

#endif // __APPLE__

#endif // OS_BSD_SEMAPHORE_BSD_HPP
