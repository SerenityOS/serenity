/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#ifdef COMPILER2
#include "asm/macroAssembler.inline.hpp"
#include "code/vmreg.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_s390.hpp"
#include "opto/runtime.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/globalDefinitions.hpp"
#include "vmreg_s390.inline.hpp"
#endif

#define __ masm->


//------------------------------generate_exception_blob---------------------------
// creates exception blob at the end
// Using exception blob, this code is jumped from a compiled method.
// (see emit_exception_handler in s390.ad file)
//
// Given an exception pc at a call we call into the runtime for the
// handler in this method. This handler might merely restore state
// (i.e. callee save registers), unwind the frame, and jump to the
// exception handler for the nmethod if there is no Java level handler
// for the nmethod.
//
// This code is entered with a branch.
//
// Arguments:
//   Z_R2(=Z_ARG1): exception oop
//   Z_R3(=Z_ARG2): exception pc
//
// Results:
//   Z_R2: exception oop
//   Z_R3: exception pc in caller
//   destination: exception handler of caller
//
// Note: the exception pc MUST be at a call (precise debug information)

void OptoRuntime::generate_exception_blob() {

  // Allocate space for the code
  ResourceMark rm;
  // Setup code generation tools
  CodeBuffer buffer("exception_blob", 2048, 1024);
  MacroAssembler* masm = new MacroAssembler(&buffer);

  Register handle_exception = Z_ARG5;

  __ verify_thread();
  __ z_stg(Z_ARG1/*exception oop*/, Address(Z_thread, JavaThread::exception_oop_offset()));
  __ z_stg(Z_ARG2/*issuing pc*/,    Address(Z_thread, JavaThread::exception_pc_offset()));

  // Store issuing pc as return pc into
  // caller's frame. stack-walking needs it. R14 is not valid here,
  // because this code gets entered with a jump.
  __ z_stg(Z_ARG2/*issuing pc*/, _z_abi(return_pc), Z_SP);

  // The following call to function OptoRuntime::handle_exception_C
  // does all the hard work. It checks if an
  // exception catch exists in the method. If so, it returns the
  // handler address. If the nmethod has been deoptimized and it had
  // a handler the handler address is the deopt blob's
  // unpack_with_exception entry.

  // push a C frame for the exception blob. it is needed for the
  // C call later on.

  Register saved_sp = Z_R11;

  __ z_lgr(saved_sp, Z_SP);

  // push frame for blob.
  int frame_size = __ push_frame_abi160(0);

  __ get_PC(Z_R1/*scratch*/);
  __ set_last_Java_frame(/*sp=*/Z_SP, /*pc=*/Z_R1);

  // This call can lead to deoptimization of the nmethod holding the handler.
  __ z_lgr(Z_ARG1, Z_thread);   // argument of C function
  __ call_c(CAST_FROM_FN_PTR(address, OptoRuntime::handle_exception_C));

  __ z_lgr(handle_exception, Z_RET);
  __ reset_last_Java_frame();

  // Pop the exception blob's C frame that has been pushed before.
  __ z_lgr(Z_SP, saved_sp);

  // [Z_RET]!=NULL was possible in hotspot5 but not in sapjvm6.
  // C2I adapter extensions are now removed by a resize in the frame manager
  // (unwind_initial_activation_pending_exception).
#ifdef ASSERT
  __ z_ltgr(handle_exception, handle_exception);
  __ asm_assert_ne("handler must not be NULL", 0x852);
#endif

  // Handle_exception contains the handler address. If the associated frame
  // has been deoptimized then the handler has been patched to jump to
  // the deoptimization blob.

  // If the exception handler jumps to the deoptimization blob, the
  // exception pc will be read from there.
  __ z_lg(Z_ARG2, Address(Z_thread, JavaThread::exception_pc_offset()));

  __ z_lg(Z_ARG1, Address(Z_thread, JavaThread::exception_oop_offset()));

  // Clear the exception oop so GC no longer processes it as a root.
  __ clear_mem(Address(Z_thread, JavaThread::exception_oop_offset()),sizeof(intptr_t));
#ifdef ASSERT
  __ clear_mem(Address(Z_thread, JavaThread::exception_handler_pc_offset()), sizeof(intptr_t));
  __ clear_mem(Address(Z_thread, JavaThread::exception_pc_offset()), sizeof(intptr_t));
#endif

  __ z_br(handle_exception);

  // Make sure all code is generated.
  masm->flush();

  // Set exception blob.
  OopMapSet *oop_maps = NULL;
  _exception_blob =  ExceptionBlob::create(&buffer, oop_maps, frame_size/wordSize);
}
