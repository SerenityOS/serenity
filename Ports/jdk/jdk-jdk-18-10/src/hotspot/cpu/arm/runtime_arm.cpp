/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/assembler.inline.hpp"
#include "code/vmreg.hpp"
#include "compiler/oopMap.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_arm.hpp"
#include "opto/runtime.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/globalDefinitions.hpp"
#include "vmreg_arm.inline.hpp"
#endif

#define __ masm->

//------------------------------ generate_exception_blob ---------------------------
// creates exception blob at the end
// Using exception blob, this code is jumped from a compiled method.
// (see emit_exception_handler in sparc.ad file)
//
// Given an exception pc at a call we call into the runtime for the
// handler in this method. This handler might merely restore state
// (i.e. callee save registers) unwind the frame and jump to the
// exception handler for the nmethod if there is no Java level handler
// for the nmethod.
//
// This code is entered with a jmp.
//
// Arguments:
//   Rexception_obj (R4/R19): exception oop
//   Rexception_pc  (R5/R20): exception pc
//
// Results:
//   Rexception_obj (R4/R19): exception oop
//   O1: exception pc in caller or ???
//   destination: exception handler of caller
//
// Note: the exception pc MUST be at a call (precise debug information)
//
void OptoRuntime::generate_exception_blob() {
  // allocate space for code
  ResourceMark rm;
  int pad = VerifyThread ? 256 : 0;// Extra slop space for more verify code

  // setup code generation tools
  // Measured 8/7/03 at 256 in 32bit debug build (no VerifyThread)
  // Measured 8/7/03 at 528 in 32bit debug build (VerifyThread)
  CodeBuffer buffer("exception_blob", 600+pad, 512);
  MacroAssembler* masm     = new MacroAssembler(&buffer);

  int framesize_in_words = 2; // FP + LR
  int framesize_in_bytes = framesize_in_words * wordSize;
  int framesize_in_slots = framesize_in_bytes / sizeof(jint);

  int start = __ offset();

  __ str(Rexception_obj, Address(Rthread, JavaThread::exception_oop_offset()));
  __ str(Rexception_pc, Address(Rthread, JavaThread::exception_pc_offset()));

  // This call does all the hard work. It checks if an exception catch
  // exists in the method.
  // If so, it returns the handler address.
  // If the nmethod has been deoptimized and it had a handler the handler
  // address is the deopt blob unpack_with_exception entry.
  //
  // If no handler exists it prepares for stack-unwinding, restoring the callee-save
  // registers of the frame being removed.
  //
  __ mov(LR, Rexception_pc);
  __ raw_push(FP, LR);
  int pc_offset = __ set_last_Java_frame(SP, FP, false, Rtemp);

  __ mov(R0, Rthread);

  // This call can block at exit and nmethod can be deoptimized at that
  // point. If the nmethod had a catch point we would jump to the
  // now deoptimized catch point and fall thru the vanilla deopt
  // path and lose the exception
  // Sure would be simpler if this call didn't block!
  __ call(CAST_FROM_FN_PTR(address, OptoRuntime::handle_exception_C));
  if (pc_offset == -1) {
    pc_offset = __ offset();
  }

  // Set an oopmap for the call site.  This oopmap will only be used if we
  // are unwinding the stack.  Hence, all locations will be dead.
  // Callee-saved registers will be the same as the frame above (i.e.,
  // handle_exception_stub), since they were restored when we got the
  // exception.

  OopMapSet *oop_maps = new OopMapSet();
  oop_maps->add_gc_map(pc_offset - start, new OopMap(framesize_in_slots, 0));

  __ reset_last_Java_frame(Rtemp);

  __ raw_pop(FP, LR);

  // Restore SP from its saved reg (FP) if the exception PC is a MethodHandle call site.
  __ ldr(Rtemp, Address(Rthread, JavaThread::is_method_handle_return_offset()));
  __ cmp(Rtemp, 0);
  __ mov(SP, Rmh_SP_save, ne);

  // R0 contains handler address
  // Since this may be the deopt blob we must set R5 to look like we returned
  // from the original pc that threw the exception

  __ ldr(Rexception_pc,  Address(Rthread, JavaThread::exception_pc_offset()));  // R5/R20

  __ ldr(Rexception_obj, Address(Rthread, JavaThread::exception_oop_offset())); // R4/R19
  __ mov(Rtemp, 0);
#ifdef ASSERT
  __ str(Rtemp, Address(Rthread, JavaThread::exception_handler_pc_offset()));
  __ str(Rtemp, Address(Rthread, JavaThread::exception_pc_offset()));
#endif
  // Clear the exception oop so GC no longer processes it as a root.
  __ str(Rtemp, Address(Rthread, JavaThread::exception_oop_offset()));
  __ jump(R0);

  // -------------
  // make sure all code is generated
  masm->flush();

  _exception_blob = ExceptionBlob::create(&buffer, oop_maps, framesize_in_words);
}
