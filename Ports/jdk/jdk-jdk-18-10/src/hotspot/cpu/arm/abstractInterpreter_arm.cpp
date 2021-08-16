/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.hpp"
#include "interpreter/bytecode.hpp"
#include "interpreter/interpreter.hpp"
#include "oops/constMethod.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/synchronizer.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"

int AbstractInterpreter::BasicType_as_index(BasicType type) {
  int i = 0;
  switch (type) {
    case T_VOID   : i = 0; break;
    case T_BOOLEAN: i = 1; break;
    case T_CHAR   : i = 2; break;
    case T_BYTE   : i = 3; break;
    case T_SHORT  : i = 4; break;
    case T_INT    : i = 5; break;
    case T_OBJECT : // fall through
    case T_ARRAY  : i = 6; break;
    case T_LONG   : i = 7; break;
    case T_FLOAT  : i = 8; break;
    case T_DOUBLE : i = 9; break;
    default       : ShouldNotReachHere();
  }
  assert(0 <= i && i < AbstractInterpreter::number_of_result_handlers, "index out of bounds");
  return i;
}

// How much stack a method activation needs in words.
int AbstractInterpreter::size_top_interpreter_activation(Method* method) {
  const int stub_code = 12;  // see generate_call_stub
  // Save space for one monitor to get into the interpreted method in case
  // the method is synchronized
  int monitor_size    = method->is_synchronized() ?
                                1*frame::interpreter_frame_monitor_size() : 0;

  // total overhead size: monitor_size + (sender SP, thru expr stack bottom).
  // be sure to change this if you add/subtract anything to/from the overhead area
  const int overhead_size = monitor_size +
                            (frame::sender_sp_offset - frame::interpreter_frame_initial_sp_offset);
  const int method_stack = (method->max_locals() + method->max_stack()) *
                           Interpreter::stackElementWords;
  return overhead_size + method_stack + stub_code;
}

// asm based interpreter deoptimization helpers
int AbstractInterpreter::size_activation(int max_stack,
                                         int tempcount,
                                         int extra_args,
                                         int moncount,
                                         int callee_param_count,
                                         int callee_locals,
                                         bool is_top_frame) {
  // Note: This calculation must exactly parallel the frame setup
  // in TemplateInterpreterGenerator::generate_fixed_frame.
  // fixed size of an interpreter frame:
  int overhead = frame::sender_sp_offset - frame::interpreter_frame_initial_sp_offset;

  // Our locals were accounted for by the caller (or last_frame_adjust on the transistion)
  // Since the callee parameters already account for the callee's params we only need to account for
  // the extra locals.

  int size = overhead +
         ((callee_locals - callee_param_count)*Interpreter::stackElementWords) +
         (moncount*frame::interpreter_frame_monitor_size()) +
         tempcount*Interpreter::stackElementWords + extra_args;


  return size;
}

void AbstractInterpreter::layout_activation(Method* method,
                                            int tempcount,
                                            int popframe_extra_args,
                                            int moncount,
                                            int caller_actual_parameters,
                                            int callee_param_count,
                                            int callee_locals,
                                            frame* caller,
                                            frame* interpreter_frame,
                                            bool is_top_frame,
                                            bool is_bottom_frame) {

  // Set up the method, locals, and monitors.
  // The frame interpreter_frame is guaranteed to be the right size,
  // as determined by a previous call to the size_activation() method.
  // It is also guaranteed to be walkable even though it is in a skeletal state
  // NOTE: return size is in words not bytes

  // fixed size of an interpreter frame:
  int max_locals = method->max_locals() * Interpreter::stackElementWords;
  int extra_locals = (method->max_locals() - method->size_of_parameters()) * Interpreter::stackElementWords;

#ifdef ASSERT
  assert(caller->sp() == interpreter_frame->sender_sp(), "Frame not properly walkable");
#endif

  interpreter_frame->interpreter_frame_set_method(method);
  // NOTE the difference in using sender_sp and interpreter_frame_sender_sp
  // interpreter_frame_sender_sp is the original sp of the caller (the unextended_sp)
  // and sender_sp is (fp + sender_sp_offset*wordSize)

  intptr_t* locals = interpreter_frame->sender_sp() + max_locals - 1;

  interpreter_frame->interpreter_frame_set_locals(locals);
  BasicObjectLock* montop = interpreter_frame->interpreter_frame_monitor_begin();
  BasicObjectLock* monbot = montop - moncount;
  interpreter_frame->interpreter_frame_set_monitor_end(monbot);

  // Set last_sp
  intptr_t* stack_top = (intptr_t*) monbot  -
    tempcount*Interpreter::stackElementWords -
    popframe_extra_args;
  interpreter_frame->interpreter_frame_set_last_sp(stack_top);

  // All frames but the initial (oldest) interpreter frame we fill in have a
  // value for sender_sp that allows walking the stack but isn't
  // truly correct. Correct the value here.

  if (extra_locals != 0 &&
      interpreter_frame->sender_sp() == interpreter_frame->interpreter_frame_sender_sp() ) {
    interpreter_frame->set_interpreter_frame_sender_sp(caller->sp() + extra_locals);
  }

  *interpreter_frame->interpreter_frame_cache_addr() =
    method->constants()->cache();
  *interpreter_frame->interpreter_frame_mirror_addr() =
    method->method_holder()->java_mirror();
}
