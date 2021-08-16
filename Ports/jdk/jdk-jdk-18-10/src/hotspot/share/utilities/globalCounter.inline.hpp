/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_GLOBALCOUNTER_INLINE_HPP
#define SHARE_UTILITIES_GLOBALCOUNTER_INLINE_HPP

#include "utilities/globalCounter.hpp"

#include "runtime/atomic.hpp"
#include "runtime/thread.inline.hpp"

inline GlobalCounter::CSContext
GlobalCounter::critical_section_begin(Thread *thread) {
  assert(thread == Thread::current(), "must be current thread");
  uintx old_cnt = Atomic::load(thread->get_rcu_counter());
  // Retain the old counter value if already active, e.g. nested.
  // Otherwise, set the counter to the current version + active bit.
  uintx new_cnt = old_cnt;
  if ((new_cnt & COUNTER_ACTIVE) == 0) {
    new_cnt = Atomic::load(&_global_counter._counter) | COUNTER_ACTIVE;
  }
  Atomic::release_store_fence(thread->get_rcu_counter(), new_cnt);
  return static_cast<CSContext>(old_cnt);
}

inline void
GlobalCounter::critical_section_end(Thread *thread, CSContext context) {
  assert(thread == Thread::current(), "must be current thread");
  assert((*thread->get_rcu_counter() & COUNTER_ACTIVE) == COUNTER_ACTIVE, "must be in critical section");
  // Restore the counter value from before the associated begin.
  Atomic::release_store(thread->get_rcu_counter(),
                        static_cast<uintx>(context));
}

class GlobalCounter::CriticalSection {
 private:
  Thread* _thread;
  CSContext _context;
 public:
  inline CriticalSection(Thread* thread) :
    _thread(thread),
    _context(GlobalCounter::critical_section_begin(_thread))
  {}

  inline  ~CriticalSection() {
    GlobalCounter::critical_section_end(_thread, _context);
  }
};

#endif // SHARE_UTILITIES_GLOBALCOUNTER_INLINE_HPP
