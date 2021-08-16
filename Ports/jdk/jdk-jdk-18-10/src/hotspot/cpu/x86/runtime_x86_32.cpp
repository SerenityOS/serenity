/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/vmreg.hpp"
#include "compiler/oopMap.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/resourceArea.hpp"
#include "opto/runtime.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/globalDefinitions.hpp"
#include "vmreg_x86.inline.hpp"
#endif


#define __ masm->

//------------------------------generate_exception_blob---------------------------
// creates exception blob at the end
// Using exception blob, this code is jumped from a compiled method.
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
//   rax: exception oop
//   rdx: exception pc
//
// Results:
//   rax: exception oop
//   rdx: exception pc in caller or ???
//   destination: exception handler of caller
//
// Note: the exception pc MUST be at a call (precise debug information)
//       Only register rax, rdx, rcx are not callee saved.
//

void OptoRuntime::generate_exception_blob() {

  // Capture info about frame layout
  enum layout {
    thread_off,                 // last_java_sp
    // The frame sender code expects that rbp will be in the "natural" place and
    // will override any oopMap setting for it. We must therefore force the layout
    // so that it agrees with the frame sender code.
    rbp_off,
    return_off,                 // slot for return address
    framesize
  };

  // allocate space for the code
  ResourceMark rm;
  // setup code generation tools
  CodeBuffer   buffer("exception_blob", 512, 512);
  MacroAssembler* masm = new MacroAssembler(&buffer);

  OopMapSet *oop_maps = new OopMapSet();

  address start = __ pc();

  __ push(rdx);
  __ subptr(rsp, return_off * wordSize);   // Prolog!

  // rbp, location is implicitly known
  __ movptr(Address(rsp,rbp_off  *wordSize), rbp);

  // Store exception in Thread object. We cannot pass any arguments to the
  // handle_exception call, since we do not want to make any assumption
  // about the size of the frame where the exception happened in.
  __ get_thread(rcx);
  __ movptr(Address(rcx, JavaThread::exception_oop_offset()), rax);
  __ movptr(Address(rcx, JavaThread::exception_pc_offset()),  rdx);

  // This call does all the hard work.  It checks if an exception handler
  // exists in the method.
  // If so, it returns the handler address.
  // If not, it prepares for stack-unwinding, restoring the callee-save
  // registers of the frame being removed.
  //
  __ movptr(Address(rsp, thread_off * wordSize), rcx); // Thread is first argument
  __ set_last_Java_frame(rcx, noreg, noreg, NULL);

  __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, OptoRuntime::handle_exception_C)));

  // No registers to map, rbp is known implicitly
  oop_maps->add_gc_map( __ pc() - start,  new OopMap( framesize, 0 ));
  __ get_thread(rcx);
  __ reset_last_Java_frame(rcx, false);

  // Restore callee-saved registers
  __ movptr(rbp, Address(rsp, rbp_off * wordSize));

  __ addptr(rsp, return_off * wordSize);   // Epilog!
  __ pop(rdx); // Exception pc

  // rax: exception handler for given <exception oop/exception pc>

  // We have a handler in rax, (could be deopt blob)
  // rdx - throwing pc, deopt blob will need it.

  __ push(rax);

  // Get the exception
  __ movptr(rax, Address(rcx, JavaThread::exception_oop_offset()));
  // Get the exception pc in case we are deoptimized
  __ movptr(rdx, Address(rcx, JavaThread::exception_pc_offset()));
#ifdef ASSERT
  __ movptr(Address(rcx, JavaThread::exception_handler_pc_offset()), NULL_WORD);
  __ movptr(Address(rcx, JavaThread::exception_pc_offset()), NULL_WORD);
#endif
  // Clear the exception oop so GC no longer processes it as a root.
  __ movptr(Address(rcx, JavaThread::exception_oop_offset()), NULL_WORD);

  __ pop(rcx);

  // rax: exception oop
  // rcx: exception handler
  // rdx: exception pc
  __ jmp (rcx);

  // -------------
  // make sure all code is generated
  masm->flush();

  _exception_blob = ExceptionBlob::create(&buffer, oop_maps, framesize);
}
