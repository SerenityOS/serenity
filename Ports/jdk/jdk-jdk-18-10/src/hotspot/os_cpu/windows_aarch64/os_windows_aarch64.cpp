/*
 * Copyright (c) 2020, Microsoft Corporation. All rights reserved.
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
#include "jvm.h"
#include "asm/macroAssembler.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "code/nativeInst.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.inline.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "runtime/arguments.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/osThread.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"
#include "unwind_windows_aarch64.hpp"
#include "utilities/debug.hpp"
#include "utilities/events.hpp"
#include "utilities/vmError.hpp"


// put OS-includes here
# include <sys/types.h>
# include <signal.h>
# include <errno.h>
# include <stdlib.h>
# include <stdio.h>
# include <intrin.h>

void os::os_exception_wrapper(java_call_t f, JavaValue* value, const methodHandle& method, JavaCallArguments* args, JavaThread* thread) {
  f(value, method, args, thread);
}

PRAGMA_DISABLE_MSVC_WARNING(4172)
// Returns an estimate of the current stack pointer. Result must be guaranteed
// to point into the calling threads stack, and be no lower than the current
// stack pointer.
address os::current_stack_pointer() {
  int dummy;
  address sp = (address)&dummy;
  return sp;
}

address os::fetch_frame_from_context(const void* ucVoid,
                    intptr_t** ret_sp, intptr_t** ret_fp) {
  address  epc;
  CONTEXT* uc = (CONTEXT*)ucVoid;

  if (uc != NULL) {
    epc = (address)uc->Pc;
    if (ret_sp) *ret_sp = (intptr_t*)uc->Sp;
    if (ret_fp) *ret_fp = (intptr_t*)uc->Fp;
  } else {
    // construct empty ExtendedPC for return value checking
    epc = NULL;
    if (ret_sp) *ret_sp = (intptr_t *)NULL;
    if (ret_fp) *ret_fp = (intptr_t *)NULL;
  }
  return epc;
}

frame os::fetch_frame_from_context(const void* ucVoid) {
  intptr_t* sp;
  intptr_t* fp;
  address epc = fetch_frame_from_context(ucVoid, &sp, &fp);
  return frame(sp, fp, epc);
}

bool os::win32::get_frame_at_stack_banging_point(JavaThread* thread,
        struct _EXCEPTION_POINTERS* exceptionInfo, address pc, frame* fr) {
  PEXCEPTION_RECORD exceptionRecord = exceptionInfo->ExceptionRecord;
  address addr = (address) exceptionRecord->ExceptionInformation[1];
  if (Interpreter::contains(pc)) {
    // interpreter performs stack banging after the fixed frame header has
    // been generated while the compilers perform it before. To maintain
    // semantic consistency between interpreted and compiled frames, the
    // method returns the Java sender of the current frame.
    *fr = os::fetch_frame_from_context((void*)exceptionInfo->ContextRecord);
    if (!fr->is_first_java_frame()) {
      assert(fr->safe_for_sender(thread), "Safety check");
      *fr = fr->java_sender();
    }
  } else {
    // more complex code with compiled code
    assert(!Interpreter::contains(pc), "Interpreted methods should have been handled above");
    CodeBlob* cb = CodeCache::find_blob(pc);
    if (cb == NULL || !cb->is_nmethod() || cb->is_frame_complete_at(pc)) {
      // Not sure where the pc points to, fallback to default
      // stack overflow handling
      return false;
    } else {
      // In compiled code, the stack banging is performed before LR
      // has been saved in the frame.  LR is live, and SP and FP
      // belong to the caller.
      intptr_t* fp = (intptr_t*)exceptionInfo->ContextRecord->Fp;
      intptr_t* sp = (intptr_t*)exceptionInfo->ContextRecord->Sp;
      address pc = (address)(exceptionInfo->ContextRecord->Lr
                         - NativeInstruction::instruction_size);
      *fr = frame(sp, fp, pc);
      if (!fr->is_java_frame()) {
        assert(fr->safe_for_sender(thread), "Safety check");
        assert(!fr->is_first_frame(), "Safety check");
        *fr = fr->java_sender();
      }
    }
  }
  assert(fr->is_java_frame(), "Safety check");
  return true;
}

frame os::get_sender_for_C_frame(frame* fr) {
  ShouldNotReachHere();
  return frame();
}

frame os::current_frame() {
  return frame();  // cannot walk Windows frames this way.  See os::get_native_stack
                   // and os::platform_print_native_stack
}

////////////////////////////////////////////////////////////////////////////////
// thread stack

// Minimum usable stack sizes required to get to user code. Space for
// HotSpot guard pages is added later.

/////////////////////////////////////////////////////////////////////////////
// helper functions for fatal error handler

void os::print_context(outputStream *st, const void *context) {
  if (context == NULL) return;

  const CONTEXT* uc = (const CONTEXT*)context;

  st->print_cr("Registers:");

  st->print(  "X0 =" INTPTR_FORMAT, uc->X0);
  st->print(", X1 =" INTPTR_FORMAT, uc->X1);
  st->print(", X2 =" INTPTR_FORMAT, uc->X2);
  st->print(", X3 =" INTPTR_FORMAT, uc->X3);
  st->cr();
  st->print(  "X4 =" INTPTR_FORMAT, uc->X4);
  st->print(", X5 =" INTPTR_FORMAT, uc->X5);
  st->print(", X6 =" INTPTR_FORMAT, uc->X6);
  st->print(", X7 =" INTPTR_FORMAT, uc->X7);
  st->cr();
  st->print(  "X8 =" INTPTR_FORMAT, uc->X8);
  st->print(", X9 =" INTPTR_FORMAT, uc->X9);
  st->print(", X10=" INTPTR_FORMAT, uc->X10);
  st->print(", X11=" INTPTR_FORMAT, uc->X11);
  st->cr();
  st->print(  "X12=" INTPTR_FORMAT, uc->X12);
  st->print(", X13=" INTPTR_FORMAT, uc->X13);
  st->print(", X14=" INTPTR_FORMAT, uc->X14);
  st->print(", X15=" INTPTR_FORMAT, uc->X15);
  st->cr();
  st->print(  "X16=" INTPTR_FORMAT, uc->X16);
  st->print(", X17=" INTPTR_FORMAT, uc->X17);
  st->print(", X18=" INTPTR_FORMAT, uc->X18);
  st->print(", X19=" INTPTR_FORMAT, uc->X19);
  st->cr();
  st->print(", X20=" INTPTR_FORMAT, uc->X20);
  st->print(", X21=" INTPTR_FORMAT, uc->X21);
  st->print(", X22=" INTPTR_FORMAT, uc->X22);
  st->print(", X23=" INTPTR_FORMAT, uc->X23);
  st->cr();
  st->print(", X24=" INTPTR_FORMAT, uc->X24);
  st->print(", X25=" INTPTR_FORMAT, uc->X25);
  st->print(", X26=" INTPTR_FORMAT, uc->X26);
  st->print(", X27=" INTPTR_FORMAT, uc->X27);
  st->print(", X28=" INTPTR_FORMAT, uc->X28);
  st->cr();
  st->cr();

  intptr_t *sp = (intptr_t *)uc->Sp;
  st->print_cr("Top of Stack: (sp=" PTR_FORMAT ")", sp);
  print_hex_dump(st, (address)sp, (address)(sp + 32), sizeof(intptr_t));
  st->cr();

  // Note: it may be unsafe to inspect memory near pc. For example, pc may
  // point to garbage if entry point in an nmethod is corrupted. Leave
  // this at the end, and hope for the best.
  address pc = (address)uc->Pc;
  st->print_cr("Instructions: (pc=" PTR_FORMAT ")", pc);
  print_hex_dump(st, pc - 32, pc + 32, sizeof(char));
  st->cr();

}

void os::print_register_info(outputStream *st, const void *context) {
 if (context == NULL) return;

  const CONTEXT* uc = (const CONTEXT*)context;

  st->print_cr("Register to memory mapping:");
  st->cr();
  // this is only for the "general purpose" registers
  st->print(" X0="); print_location(st, uc->X0);
  st->print(" X1="); print_location(st, uc->X1);
  st->print(" X2="); print_location(st, uc->X2);
  st->print(" X3="); print_location(st, uc->X3);
  st->cr();
  st->print(" X4="); print_location(st, uc->X4);
  st->print(" X5="); print_location(st, uc->X5);
  st->print(" X6="); print_location(st, uc->X6);
  st->print(" X7="); print_location(st, uc->X7);
  st->cr();
  st->print(" X8="); print_location(st, uc->X8);
  st->print(" X9="); print_location(st, uc->X9);
  st->print("X10="); print_location(st, uc->X10);
  st->print("X11="); print_location(st, uc->X11);
  st->cr();
  st->print("X12="); print_location(st, uc->X12);
  st->print("X13="); print_location(st, uc->X13);
  st->print("X14="); print_location(st, uc->X14);
  st->print("X15="); print_location(st, uc->X15);
  st->cr();
  st->print("X16="); print_location(st, uc->X16);
  st->print("X17="); print_location(st, uc->X17);
  st->print("X18="); print_location(st, uc->X18);
  st->print("X19="); print_location(st, uc->X19);
  st->cr();
  st->print("X20="); print_location(st, uc->X20);
  st->print("X21="); print_location(st, uc->X21);
  st->print("X22="); print_location(st, uc->X22);
  st->print("X23="); print_location(st, uc->X23);
  st->cr();
  st->print("X24="); print_location(st, uc->X24);
  st->print("X25="); print_location(st, uc->X25);
  st->print("X26="); print_location(st, uc->X26);
  st->print("X27="); print_location(st, uc->X27);
  st->print("X28="); print_location(st, uc->X28);

  st->cr();
}

void os::setup_fpu() {
}

bool os::supports_sse() {
  return true;
}

#ifndef PRODUCT
void os::verify_stack_alignment() {
  assert(((intptr_t)os::current_stack_pointer() & (StackAlignmentInBytes-1)) == 0, "incorrect stack alignment");
}
#endif

int os::extra_bang_size_in_bytes() {
  // AArch64 does not require the additional stack bang.
  return 0;
}

extern "C" {
  int SpinPause() {
    return 0;
  }
};
