/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_WAITBARRIER_GENERIC_HPP
#define SHARE_UTILITIES_WAITBARRIER_GENERIC_HPP

#include "memory/allocation.hpp"
#include "runtime/semaphore.hpp"
#include "utilities/globalDefinitions.hpp"

// In addition to the barrier tag, it uses two counters to keep the semaphore
// count correct and not leave any late thread waiting.
class GenericWaitBarrier : public CHeapObj<mtInternal> {
  volatile int _barrier_tag;
  // The number of threads waiting on or about to wait on the semaphore.
  volatile int _waiters;
  // The number of threads in the wait path, before or after the tag check.
  // These threads can become waiters.
  volatile int _barrier_threads;
  Semaphore _sem_barrier;

  NONCOPYABLE(GenericWaitBarrier);

  int wake_if_needed();

 public:
  GenericWaitBarrier() : _barrier_tag(0), _waiters(0), _barrier_threads(0), _sem_barrier(0) {}
  ~GenericWaitBarrier() {}

  const char* description() { return "semaphore"; }

  void arm(int barrier_tag);
  void disarm();
  void wait(int barrier_tag);
};

#endif // SHARE_UTILITIES_WAITBARRIER_GENERIC_HPP
