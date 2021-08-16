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

// no precompiled headers
#include "jvm.h"
#include "asm/assembler.inline.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.inline.hpp"
#include "nativeInst_arm.hpp"
#include "os_share_linux.hpp"
#include "prims/jniFastGetField.hpp"
#include "prims/jvm_misc.hpp"
#include "runtime/arguments.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/timer.hpp"
#include "signals_posix.hpp"
#include "utilities/debug.hpp"
#include "utilities/events.hpp"
#include "utilities/vmError.hpp"

// put OS-includes here
# include <sys/types.h>
# include <sys/mman.h>
# include <pthread.h>
# include <signal.h>
# include <errno.h>
# include <dlfcn.h>
# include <stdlib.h>
# include <stdio.h>
# include <unistd.h>
# include <sys/resource.h>
# include <pthread.h>
# include <sys/stat.h>
# include <sys/time.h>
# include <sys/utsname.h>
# include <sys/socket.h>
# include <sys/wait.h>
# include <pwd.h>
# include <poll.h>
# include <ucontext.h>
# include <fpu_control.h>
# include <asm/ptrace.h>

#define SPELL_REG_SP  "sp"

// Don't #define SPELL_REG_FP for thumb because it is not safe to use, so this makes sure we never fetch it.
#ifndef __thumb__
#define SPELL_REG_FP "fp"
#endif

address os::current_stack_pointer() {
  register address sp __asm__ (SPELL_REG_SP);
  return sp;
}

char* os::non_memory_address_word() {
  // Must never look like an address returned by reserve_memory
  return (char*) -1;
}


#if NGREG == 16
// These definitions are based on the observation that until
// the certain version of GCC mcontext_t was defined as
// a structure containing gregs[NGREG] array with 16 elements.
// In later GCC versions mcontext_t was redefined as struct sigcontext,
// along with NGREG constant changed to 18.
#define arm_pc gregs[15]
#define arm_sp gregs[13]
#define arm_fp gregs[11]
#define arm_r0 gregs[0]
#endif

#define ARM_REGS_IN_CONTEXT  16


address os::Posix::ucontext_get_pc(const ucontext_t* uc) {
  return (address)uc->uc_mcontext.arm_pc;
}

void os::Posix::ucontext_set_pc(ucontext_t* uc, address pc) {
  uc->uc_mcontext.arm_pc = (uintx)pc;
}

intptr_t* os::Linux::ucontext_get_sp(const ucontext_t* uc) {
  return (intptr_t*)uc->uc_mcontext.arm_sp;
}

intptr_t* os::Linux::ucontext_get_fp(const ucontext_t* uc) {
  return (intptr_t*)uc->uc_mcontext.arm_fp;
}

bool is_safe_for_fp(address pc) {
#ifdef __thumb__
  if (CodeCache::find_blob(pc) != NULL) {
    return true;
  }
  // For thumb C frames, given an fp we have no idea how to access the frame contents.
  return false;
#else
  // Calling os::address_is_in_vm() here leads to a dladdr call. Calling any libc
  // function during os::get_native_stack() can result in a deadlock if JFR is
  // enabled. For now, be more lenient and allow all pc's. There are other
  // frame sanity checks in shared code, and to date they have been sufficient
  // for other platforms.
  //return os::address_is_in_vm(pc);
  return true;
#endif
}

address os::fetch_frame_from_context(const void* ucVoid,
                    intptr_t** ret_sp, intptr_t** ret_fp) {

  address epc;
  const ucontext_t* uc = (const ucontext_t*)ucVoid;

  if (uc != NULL) {
    epc = os::Posix::ucontext_get_pc(uc);
    if (ret_sp) *ret_sp = os::Linux::ucontext_get_sp(uc);
    if (ret_fp) {
      intptr_t* fp = os::Linux::ucontext_get_fp(uc);
#ifndef __thumb__
      if (CodeCache::find_blob(epc) == NULL) {
        // It's a C frame. We need to adjust the fp.
        fp += os::C_frame_offset;
      }
#endif
      // Clear FP when stack walking is dangerous so that
      // the frame created will not be walked.
      // However, ensure FP is set correctly when reliable and
      // potentially necessary.
      if (!is_safe_for_fp(epc)) {
        // FP unreliable
        fp = (intptr_t *)NULL;
      }
      *ret_fp = fp;
    }
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

frame os::get_sender_for_C_frame(frame* fr) {
#ifdef __thumb__
  // We can't reliably get anything from a thumb C frame.
  return frame();
#else
  address pc = fr->sender_pc();
  if (! is_safe_for_fp(pc)) {
    return frame(fr->sender_sp(), (intptr_t *)NULL, pc);
  } else {
    return frame(fr->sender_sp(), fr->link() + os::C_frame_offset, pc);
  }
#endif
}

//
// This actually returns two frames up. It does not return os::current_frame(),
// which is the actual current frame. Nor does it return os::get_native_stack(),
// which is the caller. It returns whoever called os::get_native_stack(). Not
// very intuitive, but consistent with how this API is implemented on other
// platforms.
//
frame os::current_frame() {
#ifdef __thumb__
  // We can't reliably get anything from a thumb C frame.
  return frame();
#else
  register intptr_t* fp __asm__ (SPELL_REG_FP);
  // fp is for os::current_frame. We want the fp for our caller.
  frame myframe((intptr_t*)os::current_stack_pointer(), fp + os::C_frame_offset,
                 CAST_FROM_FN_PTR(address, os::current_frame));
  frame caller_frame = os::get_sender_for_C_frame(&myframe);

  if (os::is_first_C_frame(&caller_frame)) {
    // stack is not walkable
    // Assert below was added because it does not seem like this can ever happen.
    // How can this frame ever be the first C frame since it is called from C code?
    // If it does ever happen, undo the assert and comment here on when/why it happens.
    assert(false, "this should never happen");
    return frame();
  }

  // return frame for our caller's caller
  return os::get_sender_for_C_frame(&caller_frame);
#endif
}

extern "C" address check_vfp_fault_instr;
extern "C" address check_vfp3_32_fault_instr;
extern "C" address check_simd_fault_instr;
extern "C" address check_mp_ext_fault_instr;

address check_vfp_fault_instr = NULL;
address check_vfp3_32_fault_instr = NULL;
address check_simd_fault_instr = NULL;
address check_mp_ext_fault_instr = NULL;


bool PosixSignals::pd_hotspot_signal_handler(int sig, siginfo_t* info,
                                             ucontext_t* uc, JavaThread* thread) {

  if (sig == SIGILL &&
      ((info->si_addr == (caddr_t)check_simd_fault_instr)
       || info->si_addr == (caddr_t)check_vfp_fault_instr
       || info->si_addr == (caddr_t)check_vfp3_32_fault_instr
       || info->si_addr == (caddr_t)check_mp_ext_fault_instr)) {
    // skip faulty instruction + instruction that sets return value to
    // success and set return value to failure.
    os::Posix::ucontext_set_pc(uc, (address)info->si_addr + 8);
    uc->uc_mcontext.arm_r0 = 0;
    return true;
  }

  address stub = NULL;
  address pc = NULL;
  bool unsafe_access = false;

  if (info != NULL && uc != NULL && thread != NULL) {
    pc = (address) os::Posix::ucontext_get_pc(uc);

    // Handle ALL stack overflow variations here
    if (sig == SIGSEGV) {
      address addr = (address) info->si_addr;

      // check if fault address is within thread stack
      if (thread->is_in_full_stack(addr)) {
        // stack overflow
        StackOverflow* overflow_state = thread->stack_overflow_state();
        if (overflow_state->in_stack_yellow_reserved_zone(addr)) {
          overflow_state->disable_stack_yellow_reserved_zone();
          if (thread->thread_state() == _thread_in_Java) {
            // Throw a stack overflow exception.  Guard pages will be reenabled
            // while unwinding the stack.
            stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::STACK_OVERFLOW);
          } else {
            // Thread was in the vm or native code.  Return and try to finish.
            return true;
          }
        } else if (overflow_state->in_stack_red_zone(addr)) {
          // Fatal red zone violation.  Disable the guard pages and fall through
          // to handle_unexpected_exception way down below.
          overflow_state->disable_stack_red_zone();
          tty->print_raw_cr("An irrecoverable stack overflow has occurred.");
        } else {
          // Accessing stack address below sp may cause SEGV if current
          // thread has MAP_GROWSDOWN stack. This should only happen when
          // current thread was created by user code with MAP_GROWSDOWN flag
          // and then attached to VM. See notes in os_linux.cpp.
          if (thread->osthread()->expanding_stack() == 0) {
             thread->osthread()->set_expanding_stack();
             if (os::Linux::manually_expand_stack(thread, addr)) {
               thread->osthread()->clear_expanding_stack();
               return true;
             }
             thread->osthread()->clear_expanding_stack();
          } else {
             fatal("recursive segv. expanding stack.");
          }
        }
      }
    }

    if (thread->thread_state() == _thread_in_Java) {
      // Java thread running in Java code => find exception handler if any
      // a fault inside compiled code, the interpreter, or a stub

      if (sig == SIGSEGV && SafepointMechanism::is_poll_address((address)info->si_addr)) {
        stub = SharedRuntime::get_poll_stub(pc);
      } else if (sig == SIGBUS) {
        // BugId 4454115: A read from a MappedByteBuffer can fault
        // here if the underlying file has been truncated.
        // Do not crash the VM in such a case.
        CodeBlob* cb = CodeCache::find_blob_unsafe(pc);
        CompiledMethod* nm = (cb != NULL) ? cb->as_compiled_method_or_null() : NULL;
        if ((nm != NULL && nm->has_unsafe_access()) || (thread->doing_unsafe_access() && UnsafeCopyMemory::contains_pc(pc))) {
          unsafe_access = true;
        }
      } else if (sig == SIGSEGV &&
                 MacroAssembler::uses_implicit_null_check(info->si_addr)) {
          // Determination of interpreter/vtable stub/compiled code null exception
          CodeBlob* cb = CodeCache::find_blob_unsafe(pc);
          if (cb != NULL) {
            stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::IMPLICIT_NULL);
          }
      } else if (sig == SIGILL && *(int *)pc == NativeInstruction::zombie_illegal_instruction) {
        // Zombie
        stub = SharedRuntime::get_handle_wrong_method_stub();
      }
    } else if ((thread->thread_state() == _thread_in_vm ||
                thread->thread_state() == _thread_in_native) &&
               sig == SIGBUS && thread->doing_unsafe_access()) {
        unsafe_access = true;
    }

    // jni_fast_Get<Primitive>Field can trap at certain pc's if a GC kicks in
    // and the heap gets shrunk before the field access.
    if (sig == SIGSEGV || sig == SIGBUS) {
      address addr = JNI_FastGetField::find_slowcase_pc(pc);
      if (addr != (address)-1) {
        stub = addr;
      }
    }
  }

  if (unsafe_access && stub == NULL) {
    // it can be an unsafe access and we haven't found
    // any other suitable exception reason,
    // so assume it is an unsafe access.
    address next_pc = pc + Assembler::InstructionSize;
    if (UnsafeCopyMemory::contains_pc(pc)) {
      next_pc = UnsafeCopyMemory::page_error_continue_pc(pc);
    }
#ifdef __thumb__
    if (uc->uc_mcontext.arm_cpsr & PSR_T_BIT) {
      next_pc = (address)((intptr_t)next_pc | 0x1);
    }
#endif

    stub = SharedRuntime::handle_unsafe_access(thread, next_pc);
  }

  if (stub != NULL) {
#ifdef __thumb__
    if (uc->uc_mcontext.arm_cpsr & PSR_T_BIT) {
      intptr_t p = (intptr_t)pc | 0x1;
      pc = (address)p;

      // Clear Thumb mode bit if we're redirected into the ARM ISA based code
      if (((intptr_t)stub & 0x1) == 0) {
        uc->uc_mcontext.arm_cpsr &= ~PSR_T_BIT;
      }
    } else {
      // No Thumb2 compiled stubs are triggered from ARM ISA compiled JIT'd code today.
      // The support needs to be added if that changes
      assert((((intptr_t)stub & 0x1) == 0), "can't return to Thumb code");
    }
#endif

    // save all thread context in case we need to restore it
    if (thread != NULL) thread->set_saved_exception_pc(pc);

    os::Posix::ucontext_set_pc(uc, stub);
    return true;
  }

  return false;
}

void os::Linux::init_thread_fpu_state(void) {
  os::setup_fpu();
}

int os::Linux::get_fpu_control_word(void) {
  return 0;
}

void os::Linux::set_fpu_control_word(int fpu_control) {
  // Nothing to do
}

void os::setup_fpu() {
#if !defined(__SOFTFP__) && defined(__VFP_FP__)
  // Turn on IEEE-754 compliant VFP mode
  __asm__ volatile (
    "mov %%r0, #0;"
    "fmxr fpscr, %%r0"
    : /* no output */ : /* no input */ : "r0"
  );
#endif
}

////////////////////////////////////////////////////////////////////////////////
// thread stack

// Minimum usable stack sizes required to get to user code. Space for
// HotSpot guard pages is added later.
size_t os::Posix::_compiler_thread_min_stack_allowed = (32 DEBUG_ONLY(+ 4)) * K;
size_t os::Posix::_java_thread_min_stack_allowed = (32 DEBUG_ONLY(+ 4)) * K;
size_t os::Posix::_vm_internal_thread_min_stack_allowed = (48 DEBUG_ONLY(+ 4)) * K;

// return default stack size for thr_type
size_t os::Posix::default_stack_size(os::ThreadType thr_type) {
  // default stack size (compiler thread needs larger stack)
  size_t s = (thr_type == os::compiler_thread ? 2 * M : 512 * K);
  return s;
}

/////////////////////////////////////////////////////////////////////////////
// helper functions for fatal error handler

void os::print_context(outputStream *st, const void *context) {
  if (context == NULL) return;
  const ucontext_t *uc = (const ucontext_t*)context;

  st->print_cr("Registers:");
  intx* reg_area = (intx*)&uc->uc_mcontext.arm_r0;
  for (int r = 0; r < ARM_REGS_IN_CONTEXT; r++) {
    st->print_cr("  %-3s = " INTPTR_FORMAT, as_Register(r)->name(), reg_area[r]);
  }
#define U64_FORMAT "0x%016llx"
  // now print flag register
  st->print_cr("  %-4s = 0x%08lx", "cpsr",uc->uc_mcontext.arm_cpsr);
  st->cr();

  intptr_t *sp = (intptr_t *)os::Linux::ucontext_get_sp(uc);
  st->print_cr("Top of Stack: (sp=" INTPTR_FORMAT ")", p2i(sp));
  print_hex_dump(st, (address)sp, (address)(sp + 8*sizeof(intptr_t)), sizeof(intptr_t));
  st->cr();

  // Note: it may be unsafe to inspect memory near pc. For example, pc may
  // point to garbage if entry point in an nmethod is corrupted. Leave
  // this at the end, and hope for the best.
  address pc = os::Posix::ucontext_get_pc(uc);
  print_instructions(st, pc, Assembler::InstructionSize);
  st->cr();
}

void os::print_register_info(outputStream *st, const void *context) {
  if (context == NULL) return;

  const ucontext_t *uc = (const ucontext_t*)context;
  intx* reg_area = (intx*)&uc->uc_mcontext.arm_r0;

  st->print_cr("Register to memory mapping:");
  st->cr();
  for (int r = 0; r < ARM_REGS_IN_CONTEXT; r++) {
    st->print("  %-3s = ", as_Register(r)->name());
    print_location(st, reg_area[r]);
  }
  st->cr();
}



typedef int64_t cmpxchg_long_func_t(int64_t, int64_t, volatile int64_t*);

cmpxchg_long_func_t* os::atomic_cmpxchg_long_func = os::atomic_cmpxchg_long_bootstrap;

int64_t os::atomic_cmpxchg_long_bootstrap(int64_t compare_value, int64_t exchange_value, volatile int64_t* dest) {
  // try to use the stub:
  cmpxchg_long_func_t* func = CAST_TO_FN_PTR(cmpxchg_long_func_t*, StubRoutines::atomic_cmpxchg_long_entry());

  if (func != NULL) {
    os::atomic_cmpxchg_long_func = func;
    return (*func)(compare_value, exchange_value, dest);
  }
  assert(Threads::number_of_threads() == 0, "for bootstrap only");

  int64_t old_value = *dest;
  if (old_value == compare_value)
    *dest = exchange_value;
  return old_value;
}
typedef int64_t load_long_func_t(const volatile int64_t*);

load_long_func_t* os::atomic_load_long_func = os::atomic_load_long_bootstrap;

int64_t os::atomic_load_long_bootstrap(const volatile int64_t* src) {
  // try to use the stub:
  load_long_func_t* func = CAST_TO_FN_PTR(load_long_func_t*, StubRoutines::atomic_load_long_entry());

  if (func != NULL) {
    os::atomic_load_long_func = func;
    return (*func)(src);
  }
  assert(Threads::number_of_threads() == 0, "for bootstrap only");

  int64_t old_value = *src;
  return old_value;
}

typedef void store_long_func_t(int64_t, volatile int64_t*);

store_long_func_t* os::atomic_store_long_func = os::atomic_store_long_bootstrap;

void os::atomic_store_long_bootstrap(int64_t val, volatile int64_t* dest) {
  // try to use the stub:
  store_long_func_t* func = CAST_TO_FN_PTR(store_long_func_t*, StubRoutines::atomic_store_long_entry());

  if (func != NULL) {
    os::atomic_store_long_func = func;
    return (*func)(val, dest);
  }
  assert(Threads::number_of_threads() == 0, "for bootstrap only");

  *dest = val;
}

typedef int32_t  atomic_add_func_t(int32_t add_value, volatile int32_t *dest);

atomic_add_func_t * os::atomic_add_func = os::atomic_add_bootstrap;

int32_t  os::atomic_add_bootstrap(int32_t add_value, volatile int32_t *dest) {
  atomic_add_func_t * func = CAST_TO_FN_PTR(atomic_add_func_t*,
                                            StubRoutines::atomic_add_entry());
  if (func != NULL) {
    os::atomic_add_func = func;
    return (*func)(add_value, dest);
  }

  int32_t old_value = *dest;
  *dest = old_value + add_value;
  return (old_value + add_value);
}

typedef int32_t  atomic_xchg_func_t(int32_t exchange_value, volatile int32_t *dest);

atomic_xchg_func_t * os::atomic_xchg_func = os::atomic_xchg_bootstrap;

int32_t  os::atomic_xchg_bootstrap(int32_t exchange_value, volatile int32_t *dest) {
  atomic_xchg_func_t * func = CAST_TO_FN_PTR(atomic_xchg_func_t*,
                                            StubRoutines::atomic_xchg_entry());
  if (func != NULL) {
    os::atomic_xchg_func = func;
    return (*func)(exchange_value, dest);
  }

  int32_t old_value = *dest;
  *dest = exchange_value;
  return (old_value);
}

typedef int32_t cmpxchg_func_t(int32_t, int32_t, volatile int32_t*);

cmpxchg_func_t* os::atomic_cmpxchg_func = os::atomic_cmpxchg_bootstrap;

int32_t os::atomic_cmpxchg_bootstrap(int32_t compare_value, int32_t exchange_value, volatile int32_t* dest) {
  // try to use the stub:
  cmpxchg_func_t* func = CAST_TO_FN_PTR(cmpxchg_func_t*, StubRoutines::atomic_cmpxchg_entry());

  if (func != NULL) {
    os::atomic_cmpxchg_func = func;
    return (*func)(compare_value, exchange_value, dest);
  }
  assert(Threads::number_of_threads() == 0, "for bootstrap only");

  int32_t old_value = *dest;
  if (old_value == compare_value)
    *dest = exchange_value;
  return old_value;
}


#ifndef PRODUCT
void os::verify_stack_alignment() {
}
#endif

int os::extra_bang_size_in_bytes() {
  // ARM does not require an additional stack bang.
  return 0;
}
