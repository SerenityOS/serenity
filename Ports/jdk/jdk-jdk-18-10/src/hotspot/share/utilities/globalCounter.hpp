/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_GLOBALCOUNTER_HPP
#define SHARE_UTILITIES_GLOBALCOUNTER_HPP

#include "memory/allocation.hpp"
#include "memory/padded.hpp"

class Thread;

// The GlobalCounter provides a synchronization mechanism between threads for
// safe memory reclamation and other ABA problems. All readers must call
// critical_section_begin before reading the volatile data and
// critical_section_end afterwards. Such read-side critical sections may
// be properly nested. The write side must call write_synchronize
// before reclaming the memory. The read-path only does an uncontended store
// to a thread-local-storage and fence to stop any loads from floating up, thus
// light weight and wait-free. The write-side is more heavy since it must check
// all readers and wait until they have left the generation. (a system memory
// barrier can be used on write-side to remove fence in read-side,
// not implemented).
class GlobalCounter : public AllStatic {
 private:
  // Since do not know what we will end up next to in BSS, we make sure the
  // counter is on a seperate cacheline.
  struct PaddedCounter {
    DEFINE_PAD_MINUS_SIZE(0, DEFAULT_CACHE_LINE_SIZE, 0);
    volatile uintx _counter;
    DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, sizeof(volatile uintx));
  };

  // The global counter
  static PaddedCounter _global_counter;

  // Bit 0 is active bit.
  static const uintx COUNTER_ACTIVE = 1;
  // Thus we increase counter by 2.
  static const uintx COUNTER_INCREMENT = 2;

  // The per thread scanning closure.
  class CounterThreadCheck;

 public:

  // The type of the critical section context passed from
  // critical_section_begin() to critical_section_end().
  enum class CSContext : uintx {};

  // Must be called before accessing the data.  The result must be passed
  // to the associated call to critical_section_end().  Acts as a full
  // memory barrier before the code within the critical section.
  static CSContext critical_section_begin(Thread *thread);

  // Must be called after finished accessing the data.  The context
  // must be the result of the associated initiating critical_section_begin().
  // Acts as a release memory barrier after the code within the critical
  // section.
  static void critical_section_end(Thread *thread, CSContext context);

  // Make the data inaccessible to readers before calling. When this call
  // returns it's safe to reclaim the data.  Acts as a full memory barrier.
  static void write_synchronize();

  // A scoped object for a read-side critical-section.
  class CriticalSection;
};

#endif // SHARE_UTILITIES_GLOBALCOUNTER_HPP
