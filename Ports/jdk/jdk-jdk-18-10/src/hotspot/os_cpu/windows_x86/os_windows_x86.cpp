/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

// no precompiled headers
#include "jvm.h"
#include "asm/macroAssembler.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "nativeInst_x86.hpp"
#include "os_share_windows.hpp"
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
#include "symbolengine.hpp"
#include "unwind_windows_x86.hpp"
#include "utilities/events.hpp"
#include "utilities/vmError.hpp"
#include "windbghelp.hpp"


#undef REG_SP
#undef REG_FP
#undef REG_PC
#ifdef AMD64
#define REG_SP Rsp
#define REG_FP Rbp
#define REG_PC Rip
#else
#define REG_SP Esp
#define REG_FP Ebp
#define REG_PC Eip
#endif // AMD64

JNIEXPORT
extern LONG WINAPI topLevelExceptionFilter(_EXCEPTION_POINTERS* );

// Install a win32 structured exception handler around thread.
void os::os_exception_wrapper(java_call_t f, JavaValue* value, const methodHandle& method, JavaCallArguments* args, JavaThread* thread) {
  __try {

#ifndef AMD64
    // We store the current thread in this wrapperthread location
    // and determine how far away this address is from the structured
    // execption pointer that FS:[0] points to.  This get_thread
    // code can then get the thread pointer via FS.
    //
    // Warning:  This routine must NEVER be inlined since we'd end up with
    //           multiple offsets.
    //
    volatile Thread* wrapperthread = thread;

    if (os::win32::get_thread_ptr_offset() == 0) {
      int thread_ptr_offset;
      __asm {
        lea eax, dword ptr wrapperthread;
        sub eax, dword ptr FS:[0H];
        mov thread_ptr_offset, eax
      };
      os::win32::set_thread_ptr_offset(thread_ptr_offset);
    }
#ifdef ASSERT
    // Verify that the offset hasn't changed since we initally captured
    // it. This might happen if we accidentally ended up with an
    // inlined version of this routine.
    else {
      int test_thread_ptr_offset;
      __asm {
        lea eax, dword ptr wrapperthread;
        sub eax, dword ptr FS:[0H];
        mov test_thread_ptr_offset, eax
      };
      assert(test_thread_ptr_offset == os::win32::get_thread_ptr_offset(),
             "thread pointer offset from SEH changed");
    }
#endif // ASSERT
#endif // !AMD64

    f(value, method, args, thread);
  } __except(topLevelExceptionFilter((_EXCEPTION_POINTERS*)_exception_info())) {
      // Nothing to do.
  }
}

#ifdef AMD64

// This is the language specific handler for exceptions
// originating from dynamically generated code.
// We call the standard structured exception handler
// We only expect Continued Execution since we cannot unwind
// from generated code.
LONG HandleExceptionFromCodeCache(
  IN PEXCEPTION_RECORD ExceptionRecord,
  IN ULONG64 EstablisherFrame,
  IN OUT PCONTEXT ContextRecord,
  IN OUT PDISPATCHER_CONTEXT DispatcherContext) {
  EXCEPTION_POINTERS ep;
  LONG result;

  ep.ExceptionRecord = ExceptionRecord;
  ep.ContextRecord = ContextRecord;

  result = topLevelExceptionFilter(&ep);

  // We better only get a CONTINUE_EXECUTION from our handler
  // since we don't have unwind information registered.

  guarantee( result == EXCEPTION_CONTINUE_EXECUTION,
             "Unexpected result from topLevelExceptionFilter");

  return(ExceptionContinueExecution);
}


// Structure containing the Windows Data Structures required
// to register our Code Cache exception handler.
// We put these in the CodeCache since the API requires
// all addresses in these structures are relative to the Code
// area registered with RtlAddFunctionTable.
typedef struct {
  char ExceptionHandlerInstr[16];  // jmp HandleExceptionFromCodeCache
  RUNTIME_FUNCTION rt;
  UNWIND_INFO_EH_ONLY unw;
} DynamicCodeData, *pDynamicCodeData;

#endif // AMD64
//
// Register our CodeCache area with the OS so it will dispatch exceptions
// to our topLevelExceptionFilter when we take an exception in our
// dynamically generated code.
//
// Arguments:  low and high are the address of the full reserved
// codeCache area
//
bool os::register_code_area(char *low, char *high) {
#ifdef AMD64

  ResourceMark rm;

  pDynamicCodeData pDCD;
  PRUNTIME_FUNCTION prt;
  PUNWIND_INFO_EH_ONLY punwind;

  BufferBlob* blob = BufferBlob::create("CodeCache Exception Handler", sizeof(DynamicCodeData));
  CodeBuffer cb(blob);
  MacroAssembler* masm = new MacroAssembler(&cb);
  pDCD = (pDynamicCodeData) masm->pc();

  masm->jump(ExternalAddress((address)&HandleExceptionFromCodeCache));
  masm->flush();

  // Create an Unwind Structure specifying no unwind info
  // other than an Exception Handler
  punwind = &pDCD->unw;
  punwind->Version = 1;
  punwind->Flags = UNW_FLAG_EHANDLER;
  punwind->SizeOfProlog = 0;
  punwind->CountOfCodes = 0;
  punwind->FrameRegister = 0;
  punwind->FrameOffset = 0;
  punwind->ExceptionHandler = (char *)(&(pDCD->ExceptionHandlerInstr[0])) -
                              (char*)low;
  punwind->ExceptionData[0] = 0;

  // This structure describes the covered dynamic code area.
  // Addresses are relative to the beginning on the code cache area
  prt = &pDCD->rt;
  prt->BeginAddress = 0;
  prt->EndAddress = (ULONG)(high - low);
  prt->UnwindData = ((char *)punwind - low);

  guarantee(RtlAddFunctionTable(prt, 1, (ULONGLONG)low),
            "Failed to register Dynamic Code Exception Handler with RtlAddFunctionTable");

#endif // AMD64
  return true;
}

#ifdef AMD64
/*
 * Windows/x64 does not use stack frames the way expected by Java:
 * [1] in most cases, there is no frame pointer. All locals are addressed via RSP
 * [2] in rare cases, when alloca() is used, a frame pointer is used, but this may
 *     not be RBP.
 * See http://msdn.microsoft.com/en-us/library/ew5tede7.aspx
 *
 * So it's not possible to print the native stack using the
 *     while (...) {...  fr = os::get_sender_for_C_frame(&fr); }
 * loop in vmError.cpp. We need to roll our own loop.
 */
bool os::platform_print_native_stack(outputStream* st, const void* context,
                                     char *buf, int buf_size)
{
  CONTEXT ctx;
  if (context != NULL) {
    memcpy(&ctx, context, sizeof(ctx));
  } else {
    RtlCaptureContext(&ctx);
  }

  st->print_cr("Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)");

  STACKFRAME stk;
  memset(&stk, 0, sizeof(stk));
  stk.AddrStack.Offset    = ctx.Rsp;
  stk.AddrStack.Mode      = AddrModeFlat;
  stk.AddrFrame.Offset    = ctx.Rbp;
  stk.AddrFrame.Mode      = AddrModeFlat;
  stk.AddrPC.Offset       = ctx.Rip;
  stk.AddrPC.Mode         = AddrModeFlat;

  int count = 0;
  address lastpc = 0;
  while (count++ < StackPrintLimit) {
    intptr_t* sp = (intptr_t*)stk.AddrStack.Offset;
    intptr_t* fp = (intptr_t*)stk.AddrFrame.Offset; // NOT necessarily the same as ctx.Rbp!
    address pc = (address)stk.AddrPC.Offset;

    if (pc != NULL) {
      if (count == 2 && lastpc == pc) {
        // Skip it -- StackWalk64() may return the same PC
        // (but different SP) on the first try.
      } else {
        // Don't try to create a frame(sp, fp, pc) -- on WinX64, stk.AddrFrame
        // may not contain what Java expects, and may cause the frame() constructor
        // to crash. Let's just print out the symbolic address.
        frame::print_C_frame(st, buf, buf_size, pc);
        // print source file and line, if available
        char buf[128];
        int line_no;
        if (SymbolEngine::get_source_info(pc, buf, sizeof(buf), &line_no)) {
          st->print("  (%s:%d)", buf, line_no);
        }
        st->cr();
      }
      lastpc = pc;
    }

    PVOID p = WindowsDbgHelp::symFunctionTableAccess64(GetCurrentProcess(), stk.AddrPC.Offset);
    if (!p) {
      // StackWalk64() can't handle this PC. Calling StackWalk64 again may cause crash.
      break;
    }

    BOOL result = WindowsDbgHelp::stackWalk64(
        IMAGE_FILE_MACHINE_AMD64,  // __in      DWORD MachineType,
        GetCurrentProcess(),       // __in      HANDLE hProcess,
        GetCurrentThread(),        // __in      HANDLE hThread,
        &stk,                      // __inout   LP STACKFRAME64 StackFrame,
        &ctx);                     // __inout   PVOID ContextRecord,

    if (!result) {
      break;
    }
  }
  if (count > StackPrintLimit) {
    st->print_cr("...<more frames>...");
  }
  st->cr();

  return true;
}
#endif // AMD64

address os::fetch_frame_from_context(const void* ucVoid,
                    intptr_t** ret_sp, intptr_t** ret_fp) {

  address  epc;
  CONTEXT* uc = (CONTEXT*)ucVoid;

  if (uc != NULL) {
    epc = (address)uc->REG_PC;
    if (ret_sp) *ret_sp = (intptr_t*)uc->REG_SP;
    if (ret_fp) *ret_fp = (intptr_t*)uc->REG_FP;
  } else {
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

#ifndef AMD64
// Ignore "C4172: returning address of local variable or temporary" on 32bit
PRAGMA_DIAG_PUSH
PRAGMA_DISABLE_MSVC_WARNING(4172)
// Returns an estimate of the current stack pointer. Result must be guaranteed
// to point into the calling threads stack, and be no lower than the current
// stack pointer.
address os::current_stack_pointer() {
  int dummy;
  address sp = (address)&dummy;
  return sp;
}
PRAGMA_DIAG_POP
#else
// Returns the current stack pointer. Accurate value needed for
// os::verify_stack_alignment().
address os::current_stack_pointer() {
  typedef address get_sp_func();
  get_sp_func* func = CAST_TO_FN_PTR(get_sp_func*,
                                     StubRoutines::x86::get_previous_sp_entry());
  return (*func)();
}
#endif

bool os::win32::get_frame_at_stack_banging_point(JavaThread* thread,
        struct _EXCEPTION_POINTERS* exceptionInfo, address pc, frame* fr) {
  PEXCEPTION_RECORD exceptionRecord = exceptionInfo->ExceptionRecord;
  address addr = (address) exceptionRecord->ExceptionInformation[1];
  if (Interpreter::contains(pc)) {
    *fr = os::fetch_frame_from_context((void*)exceptionInfo->ContextRecord);
    if (!fr->is_first_java_frame()) {
      // get_frame_at_stack_banging_point() is only called when we
      // have well defined stacks so java_sender() calls do not need
      // to assert safe_for_sender() first.
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
      // in compiled code, the stack banging is performed just after the return pc
      // has been pushed on the stack
      intptr_t* fp = (intptr_t*)exceptionInfo->ContextRecord->REG_FP;
      intptr_t* sp = (intptr_t*)exceptionInfo->ContextRecord->REG_SP;
      *fr = frame(sp + 1, fp, (address)*sp);
      if (!fr->is_java_frame()) {
        // See java_sender() comment above.
        *fr = fr->java_sender();
      }
    }
  }
  assert(fr->is_java_frame(), "Safety check");
  return true;
}


// VC++ does not save frame pointer on stack in optimized build. It
// can be turned off by /Oy-. If we really want to walk C frames,
// we can use the StackWalk() API.
frame os::get_sender_for_C_frame(frame* fr) {
  ShouldNotReachHere();
  return frame();
}

frame os::current_frame() {
  return frame();  // cannot walk Windows frames this way.  See os::get_native_stack
                   // and os::platform_print_native_stack
}

void os::print_context(outputStream *st, const void *context) {
  if (context == NULL) return;

  const CONTEXT* uc = (const CONTEXT*)context;

  st->print_cr("Registers:");
#ifdef AMD64
  st->print(  "RAX=" INTPTR_FORMAT, uc->Rax);
  st->print(", RBX=" INTPTR_FORMAT, uc->Rbx);
  st->print(", RCX=" INTPTR_FORMAT, uc->Rcx);
  st->print(", RDX=" INTPTR_FORMAT, uc->Rdx);
  st->cr();
  st->print(  "RSP=" INTPTR_FORMAT, uc->Rsp);
  st->print(", RBP=" INTPTR_FORMAT, uc->Rbp);
  st->print(", RSI=" INTPTR_FORMAT, uc->Rsi);
  st->print(", RDI=" INTPTR_FORMAT, uc->Rdi);
  st->cr();
  st->print(  "R8 =" INTPTR_FORMAT, uc->R8);
  st->print(", R9 =" INTPTR_FORMAT, uc->R9);
  st->print(", R10=" INTPTR_FORMAT, uc->R10);
  st->print(", R11=" INTPTR_FORMAT, uc->R11);
  st->cr();
  st->print(  "R12=" INTPTR_FORMAT, uc->R12);
  st->print(", R13=" INTPTR_FORMAT, uc->R13);
  st->print(", R14=" INTPTR_FORMAT, uc->R14);
  st->print(", R15=" INTPTR_FORMAT, uc->R15);
  st->cr();
  st->print(  "RIP=" INTPTR_FORMAT, uc->Rip);
  st->print(", EFLAGS=" INTPTR_FORMAT, uc->EFlags);
#else
  st->print(  "EAX=" INTPTR_FORMAT, uc->Eax);
  st->print(", EBX=" INTPTR_FORMAT, uc->Ebx);
  st->print(", ECX=" INTPTR_FORMAT, uc->Ecx);
  st->print(", EDX=" INTPTR_FORMAT, uc->Edx);
  st->cr();
  st->print(  "ESP=" INTPTR_FORMAT, uc->Esp);
  st->print(", EBP=" INTPTR_FORMAT, uc->Ebp);
  st->print(", ESI=" INTPTR_FORMAT, uc->Esi);
  st->print(", EDI=" INTPTR_FORMAT, uc->Edi);
  st->cr();
  st->print(  "EIP=" INTPTR_FORMAT, uc->Eip);
  st->print(", EFLAGS=" INTPTR_FORMAT, uc->EFlags);
#endif // AMD64
  st->cr();
  st->cr();

  intptr_t *sp = (intptr_t *)uc->REG_SP;
  st->print_cr("Top of Stack: (sp=" PTR_FORMAT ")", sp);
  print_hex_dump(st, (address)sp, (address)(sp + 32), sizeof(intptr_t));
  st->cr();

  // Note: it may be unsafe to inspect memory near pc. For example, pc may
  // point to garbage if entry point in an nmethod is corrupted. Leave
  // this at the end, and hope for the best.
  address pc = (address)uc->REG_PC;
  print_instructions(st, pc, sizeof(char));
  st->cr();
}


void os::print_register_info(outputStream *st, const void *context) {
  if (context == NULL) return;

  const CONTEXT* uc = (const CONTEXT*)context;

  st->print_cr("Register to memory mapping:");
  st->cr();

  // this is only for the "general purpose" registers

#ifdef AMD64
  st->print("RIP="); print_location(st, uc->Rip);
  st->print("RAX="); print_location(st, uc->Rax);
  st->print("RBX="); print_location(st, uc->Rbx);
  st->print("RCX="); print_location(st, uc->Rcx);
  st->print("RDX="); print_location(st, uc->Rdx);
  st->print("RSP="); print_location(st, uc->Rsp);
  st->print("RBP="); print_location(st, uc->Rbp);
  st->print("RSI="); print_location(st, uc->Rsi);
  st->print("RDI="); print_location(st, uc->Rdi);
  st->print("R8 ="); print_location(st, uc->R8);
  st->print("R9 ="); print_location(st, uc->R9);
  st->print("R10="); print_location(st, uc->R10);
  st->print("R11="); print_location(st, uc->R11);
  st->print("R12="); print_location(st, uc->R12);
  st->print("R13="); print_location(st, uc->R13);
  st->print("R14="); print_location(st, uc->R14);
  st->print("R15="); print_location(st, uc->R15);
#else
  st->print("EIP="); print_location(st, uc->Eip);
  st->print("EAX="); print_location(st, uc->Eax);
  st->print("EBX="); print_location(st, uc->Ebx);
  st->print("ECX="); print_location(st, uc->Ecx);
  st->print("EDX="); print_location(st, uc->Edx);
  st->print("ESP="); print_location(st, uc->Esp);
  st->print("EBP="); print_location(st, uc->Ebp);
  st->print("ESI="); print_location(st, uc->Esi);
  st->print("EDI="); print_location(st, uc->Edi);
#endif

  st->cr();
}

extern "C" int SpinPause () {
#ifdef AMD64
   return 0 ;
#else
   // pause == rep:nop
   // On systems that don't support pause a rep:nop
   // is executed as a nop.  The rep: prefix is ignored.
   _asm {
      pause ;
   };
   return 1 ;
#endif // AMD64
}

juint os::cpu_microcode_revision() {
  juint result = 0;
  BYTE data[8] = {0};
  HKEY key;
  DWORD status = RegOpenKey(HKEY_LOCAL_MACHINE,
               "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", &key);
  if (status == ERROR_SUCCESS) {
    DWORD size = sizeof(data);
    status = RegQueryValueEx(key, "Update Revision", NULL, NULL, data, &size);
    if (status == ERROR_SUCCESS) {
      if (size == 4) result = *((juint*)data);
      if (size == 8) result = *((juint*)data + 1); // upper 32-bits
    }
    RegCloseKey(key);
  }
  return result;
}

void os::setup_fpu() {
#ifndef AMD64
  int fpu_cntrl_word = StubRoutines::x86::fpu_cntrl_wrd_std();
  __asm fldcw fpu_cntrl_word;
#endif // !AMD64
}

#ifndef PRODUCT
void os::verify_stack_alignment() {
#ifdef AMD64
  // The current_stack_pointer() calls generated get_previous_sp stub routine.
  // Only enable the assert after the routine becomes available.
  if (StubRoutines::code1() != NULL) {
    assert(((intptr_t)os::current_stack_pointer() & (StackAlignmentInBytes-1)) == 0, "incorrect stack alignment");
  }
#endif
}
#endif

int os::extra_bang_size_in_bytes() {
  // JDK-8050147 requires the full cache line bang for x86.
  return VM_Version::L1_line_size();
}
