/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2010 Red Hat, Inc.
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

#ifndef CPU_ZERO_STACK_ZERO_INLINE_HPP
#define CPU_ZERO_STACK_ZERO_INLINE_HPP

#include "runtime/thread.hpp"
#include "stack_zero.hpp"

inline void ZeroStack::overflow_check(int required_words, TRAPS) {
  // Check the Zero stack
  if (available_words() < required_words) {
    handle_overflow(THREAD);
    return;
  }

  // Check the ABI stack
  if (abi_stack_available(THREAD) < 0) {
    handle_overflow(THREAD);
    return;
  }
}

// This method returns the amount of ABI stack available for us
// to use under normal circumstances.  Note that the returned
// value can be negative.
inline int ZeroStack::abi_stack_available(Thread *thread) const {
  assert(Thread::current() == thread, "should run in the same thread");
  int stack_used = thread->stack_base() - (address) &stack_used
    + (StackOverflow::stack_guard_zone_size() + StackOverflow::stack_shadow_zone_size());
  int stack_free = thread->stack_size() - stack_used;
  return stack_free;
}

#endif // CPU_ZERO_STACK_ZERO_INLINE_HPP
