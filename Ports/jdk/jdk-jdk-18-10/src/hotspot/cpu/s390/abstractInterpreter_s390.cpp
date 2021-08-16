/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

// How much stack a method top interpreter activation needs in words.
int AbstractInterpreter::size_top_interpreter_activation(Method* method) {

  // We have to size the following 2 frames:
  //
  //   [TOP_IJAVA_FRAME_ABI]
  //   [ENTRY_FRAME]
  //
  // This expands to (see frame_s390.hpp):
  //
  //   [TOP_IJAVA_FRAME_ABI]
  //   [operand stack]                 > stack
  //   [monitors]      (optional)      > monitors
  //   [IJAVA_STATE]                   > interpreter_state
  //   [PARENT_IJAVA_FRAME_ABI]
  //   [callee's locals w/o arguments] \ locals
  //   [outgoing arguments]            /
  //   [ENTRY_FRAME_LOCALS]

  int locals = method->max_locals() * BytesPerWord;
  int interpreter_state = frame::z_ijava_state_size;

  int stack = method->max_stack() * BytesPerWord;
  int monitors = method->is_synchronized() ? frame::interpreter_frame_monitor_size_in_bytes() : 0;

  int total_bytes =
    frame::z_top_ijava_frame_abi_size +
    stack +
    monitors +
    interpreter_state +
    frame::z_parent_ijava_frame_abi_size +
    locals +
    frame::z_entry_frame_locals_size;

  return (total_bytes/BytesPerWord);
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
  // in AbstractInterpreterGenerator::generate_method_entry.

  assert((Interpreter::stackElementSize == frame::alignment_in_bytes), "must align frame size");
  const int abi_scratch = is_top_frame ? (frame::z_top_ijava_frame_abi_size    / Interpreter::stackElementSize) :
                                         (frame::z_parent_ijava_frame_abi_size / Interpreter::stackElementSize);

  const int size =
    max_stack                                                 +
    (callee_locals - callee_params)                           + // Already counted in max_stack().
    monitors * frame::interpreter_frame_monitor_size()        +
    abi_scratch                                               +
    frame::z_ijava_state_size / Interpreter::stackElementSize;

  // Fixed size of an interpreter frame.
  return size;
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
  // TOP_IJAVA_FRAME:
  //
  //    0 [TOP_IJAVA_FRAME_ABI]         -+
  //   16 [operand stack]                | size
  //      [monitors]      (optional)     |
  //      [IJAVA_STATE]                 -+
  //      Note: own locals are located in the caller frame.
  //
  // PARENT_IJAVA_FRAME:
  //
  //    0 [PARENT_IJAVA_FRAME_ABI]                    -+
  //      [callee's locals w/o arguments]              |
  //      [outgoing arguments]                         | size
  //      [used part of operand stack w/o arguments]   |
  //      [monitors]      (optional)                   |
  //      [IJAVA_STATE]                               -+
  //

  // Now we know our caller, calc the exact frame layout and size
  // z_ijava_state->locals - i*BytesPerWord points to i-th Java local (i starts at 0).
  intptr_t* locals_base = (caller->is_interpreted_frame())
    ? (caller->interpreter_frame_tos_address() + caller_actual_parameters - 1)
    : (caller->sp()                            + method->max_locals()     - 1 +
       frame::z_parent_ijava_frame_abi_size / Interpreter::stackElementSize);

  intptr_t* monitor_base = (intptr_t*)((address)interpreter_frame->fp() - frame::z_ijava_state_size);
  intptr_t* monitor      = monitor_base - (moncount * frame::interpreter_frame_monitor_size());
  intptr_t* operand_stack_base = monitor;
  intptr_t* tos          = operand_stack_base - tempcount - popframe_extra_args;
  intptr_t* top_frame_sp =
    operand_stack_base - method->max_stack() - frame::z_top_ijava_frame_abi_size / Interpreter::stackElementSize;
  intptr_t* sender_sp;
  if (caller->is_interpreted_frame()) {
    sender_sp = caller->interpreter_frame_top_frame_sp();
  } else if (caller->is_compiled_frame()) {
    sender_sp = caller->fp() - caller->cb()->frame_size();
    // The bottom frame's sender_sp is its caller's unextended_sp.
    // It was already set when its skeleton was pushed (see push_skeleton_frames()).
    // Note: the unextended_sp is required by nmethod::orig_pc_addr().
    assert(is_bottom_frame && (sender_sp == caller->unextended_sp()),
           "must initialize sender_sp of bottom skeleton frame when pushing it");
  } else {
    assert(caller->is_entry_frame(), "is there a new frame type??");
    sender_sp = caller->sp(); // Call_stub only uses it's fp.
  }

  interpreter_frame->interpreter_frame_set_method(method);
  interpreter_frame->interpreter_frame_set_mirror(method->method_holder()->java_mirror());
  interpreter_frame->interpreter_frame_set_locals(locals_base);
  interpreter_frame->interpreter_frame_set_monitor_end((BasicObjectLock *)monitor);
  *interpreter_frame->interpreter_frame_cache_addr() = method->constants()->cache();
  interpreter_frame->interpreter_frame_set_tos_address(tos);
  interpreter_frame->interpreter_frame_set_sender_sp(sender_sp);
  interpreter_frame->interpreter_frame_set_top_frame_sp(top_frame_sp);
}
