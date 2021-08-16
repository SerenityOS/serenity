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

#include "precompiled.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/zero/bytecodeInterpreter.hpp"
#include "runtime/thread.inline.hpp"
#include "stack_zero.hpp"
#include "stack_zero.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "utilities/align.hpp"

// Inlined causes circular inclusion with thread.hpp
ZeroStack::ZeroStack()
    : _base(NULL), _top(NULL), _sp(NULL) {
    _shadow_pages_size = StackOverflow::stack_shadow_zone_size();
  }

int ZeroStack::suggest_size(Thread *thread) const {
  assert(needs_setup(), "already set up");
  int abi_available = abi_stack_available(thread);
  assert(abi_available >= 0, "available abi stack must be >= 0");
  return align_down(abi_available / 2, wordSize);
}

void ZeroStack::handle_overflow(TRAPS) {
  JavaThread *thread = THREAD;

  // Set up the frame anchor if it isn't already
  bool has_last_Java_frame = thread->has_last_Java_frame();
  if (!has_last_Java_frame) {
    intptr_t *sp = thread->zero_stack()->sp();
    ZeroFrame *frame = thread->top_zero_frame();
    while (frame) {
      if (frame->is_interpreter_frame()) {
        interpreterState istate =
          frame->as_interpreter_frame()->interpreter_state();
        if (istate->self_link() == istate)
          break;
      }

      sp = ((intptr_t *) frame) + 1;
      frame = frame->next();
    }

    if (frame == NULL)
      fatal("unrecoverable stack overflow");

    thread->set_last_Java_frame(frame, sp);
  }

  // Throw the exception
  switch (thread->thread_state()) {
  case _thread_in_Java:
    InterpreterRuntime::throw_StackOverflowError(thread);
    break;

  case _thread_in_vm:
    Exceptions::throw_stack_overflow_exception(thread, __FILE__, __LINE__,
                                               methodHandle());
    break;

  default:
    ShouldNotReachHere();
  }

  // Reset the frame anchor if necessary
  if (!has_last_Java_frame)
    thread->reset_last_Java_frame();
}

#ifndef PRODUCT
void ZeroStack::zap(int c) {
  memset(_base, c, available_words() * wordSize);
}
#endif // PRODUCT
