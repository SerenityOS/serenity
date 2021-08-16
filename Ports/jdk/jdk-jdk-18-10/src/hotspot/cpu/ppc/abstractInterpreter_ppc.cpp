/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
#include "interpreter/interpreter.hpp"
#include "oops/constMethod.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.hpp"
#include "runtime/frame.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

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
  assert(0 <= i && i < AbstractInterpreter::number_of_result_handlers, "index out of bounds");
  return i;
}

// How much stack a method activation needs in stack slots.
// We must calc this exactly like in generate_fixed_frame.
// Note: This returns the conservative size assuming maximum alignment.
int AbstractInterpreter::size_top_interpreter_activation(Method* method) {
  const int max_alignment_size = 2;
  const int abi_scratch = frame::abi_reg_args_size;
  return method->max_locals() + method->max_stack() +
         frame::interpreter_frame_monitor_size() + max_alignment_size + abi_scratch;
}

// Returns number of stackElementWords needed for the interpreter frame with the
// given sections.
// This overestimates the stack by one slot in case of alignments.
int AbstractInterpreter::size_activation(int max_stack,
                                         int temps,
                                         int extra_args,
                                         int monitors,
                                         int callee_params,
                                         int callee_locals,
                                         bool is_top_frame) {
  // Note: This calculation must exactly parallel the frame setup
  // in TemplateInterpreterGenerator::generate_fixed_frame.
  assert(Interpreter::stackElementWords == 1, "sanity");
  const int max_alignment_space = StackAlignmentInBytes / Interpreter::stackElementSize;
  const int abi_scratch = is_top_frame ? (frame::abi_reg_args_size / Interpreter::stackElementSize) :
                                         (frame::abi_minframe_size / Interpreter::stackElementSize);
  const int size =
    max_stack                                                +
    (callee_locals - callee_params)                          +
    monitors * frame::interpreter_frame_monitor_size()       +
    max_alignment_space                                      +
    abi_scratch                                              +
    frame::ijava_state_size / Interpreter::stackElementSize;

  // Fixed size of an interpreter frame, align to 16-byte.
  return (size & -2);
}

// Fills a sceletal interpreter frame generated during deoptimizations.
//
// Parameters:
//
// interpreter_frame != NULL:
//   set up the method, locals, and monitors.
//   The frame interpreter_frame, if not NULL, is guaranteed to be the
//   right size, as determined by a previous call to this method.
//   It is also guaranteed to be walkable even though it is in a skeletal state
//
// is_top_frame == true:
//   We're processing the *oldest* interpreter frame!
//
// pop_frame_extra_args:
//   If this is != 0 we are returning to a deoptimized frame by popping
//   off the callee frame. We want to re-execute the call that called the
//   callee interpreted, but since the return to the interpreter would pop
//   the arguments off advance the esp by dummy popframe_extra_args slots.
//   Popping off those will establish the stack layout as it was before the call.
//
void AbstractInterpreter::layout_activation(Method* method,
                                            int tempcount,
                                            int popframe_extra_args,
                                            int moncount,
                                            int caller_actual_parameters,
                                            int callee_param_count,
                                            int callee_locals_count,
                                            frame* caller,
                                            frame* interpreter_frame,
                                            bool is_top_frame,
                                            bool is_bottom_frame) {

  const int abi_scratch = is_top_frame ? (frame::abi_reg_args_size / Interpreter::stackElementSize) :
                                         (frame::abi_minframe_size / Interpreter::stackElementSize);

  intptr_t* locals_base  = (caller->is_interpreted_frame()) ?
    caller->interpreter_frame_esp() + caller_actual_parameters :
    caller->sp() + method->max_locals() - 1 + (frame::abi_minframe_size / Interpreter::stackElementSize);

  intptr_t* monitor_base = caller->sp() - frame::ijava_state_size / Interpreter::stackElementSize;
  intptr_t* monitor      = monitor_base - (moncount * frame::interpreter_frame_monitor_size());
  intptr_t* esp_base     = monitor - 1;
  intptr_t* esp          = esp_base - tempcount - popframe_extra_args;
  intptr_t* sp           = (intptr_t *) (((intptr_t) (esp_base - callee_locals_count + callee_param_count - method->max_stack()- abi_scratch)) & -StackAlignmentInBytes);
  intptr_t* sender_sp    = caller->sp() + (frame::abi_minframe_size - frame::abi_reg_args_size) / Interpreter::stackElementSize;
  intptr_t* top_frame_sp = is_top_frame ? sp : sp + (frame::abi_minframe_size - frame::abi_reg_args_size) / Interpreter::stackElementSize;

  interpreter_frame->interpreter_frame_set_method(method);
  interpreter_frame->interpreter_frame_set_mirror(method->method_holder()->java_mirror());
  interpreter_frame->interpreter_frame_set_locals(locals_base);
  interpreter_frame->interpreter_frame_set_cpcache(method->constants()->cache());
  interpreter_frame->interpreter_frame_set_esp(esp);
  interpreter_frame->interpreter_frame_set_monitor_end((BasicObjectLock *)monitor);
  interpreter_frame->interpreter_frame_set_top_frame_sp(top_frame_sp);
  if (!is_bottom_frame) {
    interpreter_frame->interpreter_frame_set_sender_sp(sender_sp);
  }
}
