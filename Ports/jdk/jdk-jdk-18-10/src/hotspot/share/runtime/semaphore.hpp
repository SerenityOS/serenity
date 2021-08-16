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
 *
 */

#ifndef SHARE_RUNTIME_SEMAPHORE_HPP
#define SHARE_RUNTIME_SEMAPHORE_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

#if defined(LINUX) || defined(AIX)
# include "semaphore_posix.hpp"
#elif defined(BSD)
# include "semaphore_bsd.hpp"
#elif defined(_WINDOWS)
# include "semaphore_windows.hpp"
#else
# error "No semaphore implementation provided for this OS"
#endif

class JavaThread;

// Implements the limited, platform independent Semaphore API.
class Semaphore : public CHeapObj<mtSynchronizer> {
  SemaphoreImpl _impl;

  NONCOPYABLE(Semaphore);

 public:
  Semaphore(uint value = 0) : _impl(value) {}
  ~Semaphore() {}

  void signal(uint count = 1) { _impl.signal(count); }

  void wait()                 { _impl.wait(); }

  bool trywait()              { return _impl.trywait(); }

  void wait_with_safepoint_check(JavaThread* thread);
};

#endif // SHARE_RUNTIME_SEMAPHORE_HPP
