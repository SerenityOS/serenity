/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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
#include "interpreter/zero/bytecodeInterpreter.hpp"
#include "interpreter/zero/zeroInterpreter.hpp"
#include "runtime/frame.inline.hpp"
#include "utilities/globalDefinitions.hpp"

int AbstractInterpreter::BasicType_as_index(BasicType type) {
  int i = 0;
  switch (type) {
    case T_BOOLEAN: i = 0; break;
    case T_CHAR   : i = 1; break;
    case T_BYTE   : i = 2; break;
    case T_SHORT  : i = 3; break;
    case T_INT    : i = 4; break;
    case T_LONG   : i = 5; break;
    case T_VOID   : i = 6; break;
    case T_FLOAT  : i = 7; break;
    case T_DOUBLE : i = 8; break;
    case T_OBJECT : i = 9; break;
    case T_ARRAY  : i = 9; break;
    default       : ShouldNotReachHere();
  }
  assert(0 <= i && i < AbstractInterpreter::number_of_result_handlers,
         "index out of bounds");
  return i;
}

// Deoptimization helpers

int AbstractInterpreter::size_activation(int       max_stack,
                                         int       tempcount,
                                         int       extra_args,
                                         int       moncount,
                                         int       callee_param_count,
                                         int       callee_locals,
                                         bool      is_top_frame) {
  int header_words        = InterpreterFrame::header_words;
  int monitor_words       = moncount * frame::interpreter_frame_monitor_size();
  int stack_words         = is_top_frame ? max_stack : tempcount;
  int callee_extra_locals = callee_locals - callee_param_count;

  return header_words + monitor_words + stack_words + callee_extra_locals;
}

void AbstractInterpreter::layout_activation(Method* method,
                                            int       tempcount,
                                            int       popframe_extra_args,
                                            int       moncount,
                                            int       caller_actual_parameters,
                                            int       callee_param_count,
                                            int       callee_locals,
                                            frame*    caller,
                                            frame*    interpreter_frame,
                                            bool      is_top_frame,
                                            bool      is_bottom_frame) {
  assert(popframe_extra_args == 0, "what to do?");
  assert(!is_top_frame || (!callee_locals && !callee_param_count),
         "top frame should have no caller");

  // This code must exactly match what InterpreterFrame::build
  // does (the full InterpreterFrame::build, that is, not the
  // one that creates empty frames for the deoptimizer).
  //
  // interpreter_frame will be filled in.  It's size is determined by
  // a previous call to the size_activation() method,
  //
  // Note that tempcount is the current size of the expression
  // stack.  For top most frames we will allocate a full sized
  // expression stack and not the trimmed version that non-top
  // frames have.

  int monitor_words       = moncount * frame::interpreter_frame_monitor_size();
  intptr_t *locals        = interpreter_frame->fp() + method->max_locals();
  interpreterState istate = interpreter_frame->get_interpreterState();
  intptr_t *monitor_base  = (intptr_t*) istate;
  intptr_t *stack_base    = monitor_base - monitor_words;
  intptr_t *stack         = stack_base - tempcount - 1;

  BytecodeInterpreter::layout_interpreterState(istate,
                                               caller,
                                               NULL,
                                               method,
                                               locals,
                                               stack,
                                               stack_base,
                                               monitor_base,
                                               NULL,
                                               is_top_frame);
}

// Helper for (runtime) stack overflow checks

int AbstractInterpreter::size_top_interpreter_activation(Method* method) {
  return 0;
}
