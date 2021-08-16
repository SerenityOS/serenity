/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
#include "classfile/classLoader.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "interpreter/interpreter.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "os_share_bsd.hpp"
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
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"
#include "signals_posix.hpp"
#include "utilities/align.hpp"
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
# include <sys/stat.h>
# include <sys/time.h>
# include <sys/utsname.h>
# include <sys/socket.h>
# include <sys/wait.h>
# include <pwd.h>
# include <poll.h>
#ifndef __OpenBSD__
# include <ucontext.h>
#endif

#if !defined(__APPLE__) && !defined(__NetBSD__)
# include <pthread_np.h>
#endif

#define SPELL_REG_SP "sp"
#define SPELL_REG_FP "fp"

#ifdef __APPLE__
// see darwin-xnu/osfmk/mach/arm/_structs.h

// 10.5 UNIX03 member name prefixes
#define DU3_PREFIX(s, m) __ ## s.__ ## m
#endif

#define context_x    uc_mcontext->DU3_PREFIX(ss,x)
#define context_fp   uc_mcontext->DU3_PREFIX(ss,fp)
#define context_lr   uc_mcontext->DU3_PREFIX(ss,lr)
#define context_sp   uc_mcontext->DU3_PREFIX(ss,sp)
#define context_pc   uc_mcontext->DU3_PREFIX(ss,pc)
#define context_cpsr uc_mcontext->DU3_PREFIX(ss,cpsr)
#define context_esr  uc_mcontext->DU3_PREFIX(es,esr)

address os::current_stack_pointer() {
#if defined(__clang__) || defined(__llvm__)
  void *sp;
  __asm__("mov %0, " SPELL_REG_SP : "=r"(sp));
  return (address) sp;
#else
  register void *sp __asm__ (SPELL_REG_SP);
  return (address) sp;
#endif
}

char* os::non_memory_address_word() {
  // Must never look like an address returned by reserve_memory,
  // even in its subfields (as defined by the CPU immediate fields,
  // if the CPU splits constants across multiple instructions).

  // the return value used in computation of Universe::non_oop_word(), which
  // is loaded by cpu/aarch64 by MacroAssembler::movptr(Register, uintptr_t)
  return (char*) 0xffffffffffff;
}

address os::Posix::ucontext_get_pc(const ucontext_t * uc) {
  return (address)uc->context_pc;
}

void os::Posix::ucontext_set_pc(ucontext_t * uc, address pc) {
  uc->context_pc = (intptr_t)pc ;
}

intptr_t* os::Bsd::ucontext_get_sp(const ucontext_t * uc) {
  return (intptr_t*)uc->context_sp;
}

intptr_t* os::Bsd::ucontext_get_fp(const ucontext_t * uc) {
  return (intptr_t*)uc->context_fp;
}

address os::fetch_frame_from_context(const void* ucVoid,
                    intptr_t** ret_sp, intptr_t** ret_fp) {

  address epc;
  const ucontext_t* uc = (const ucontext_t*)ucVoid;

  if (uc != NULL) {
    epc = os::Posix::ucontext_get_pc(uc);
    if (ret_sp) *ret_sp = os::Bsd::ucontext_get_sp(uc);
    if (ret_fp) *ret_fp = os::Bsd::ucontext_get_fp(uc);
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

frame os::fetch_compiled_frame_from_context(const void* ucVoid) {
  const ucontext_t* uc = (const ucontext_t*)ucVoid;
  // In compiled code, the stack banging is performed before LR
  // has been saved in the frame.  LR is live, and SP and FP
  // belong to the caller.
  intptr_t* fp = os::Bsd::ucontext_get_fp(uc);
  intptr_t* sp = os::Bsd::ucontext_get_sp(uc);
  address pc = (address)(uc->context_lr
                         - NativeInstruction::instruction_size);
  return frame(sp, fp, pc);
}

// JVM compiled with -fno-omit-frame-pointer, so RFP is saved on the stack.
frame os::get_sender_for_C_frame(frame* fr) {
  return frame(fr->link(), fr->link(), fr->sender_pc());
}

NOINLINE frame os::current_frame() {
  intptr_t *fp = *(intptr_t **)__builtin_frame_address(0);
  frame myframe((intptr_t*)os::current_stack_pointer(),
                (intptr_t*)fp,
                CAST_FROM_FN_PTR(address, os::current_frame));
  if (os::is_first_C_frame(&myframe)) {
    // stack is not walkable
    return frame();
  } else {
    return os::get_sender_for_C_frame(&myframe);
  }
}

ATTRIBUTE_PRINTF(6, 7)
static void report_and_die(Thread* thread, void* context, const char* filename, int lineno, const char* message,
                             const char* detail_fmt, ...) {
  va_list va;
  va_start(va, detail_fmt);
  VMError::report_and_die(thread, context, filename, lineno, message, detail_fmt, va);
  va_end(va);
}

bool PosixSignals::pd_hotspot_signal_handler(int sig, siginfo_t* info,
                                             ucontext_t* uc, JavaThread* thread) {
  // Enable WXWrite: this function is called by the signal handler at arbitrary
  // point of execution.
  ThreadWXEnable wx(WXWrite, thread);

  // decide if this trap can be handled by a stub
  address stub = NULL;

  address pc          = NULL;

  //%note os_trap_1
  if (info != NULL && uc != NULL && thread != NULL) {
    pc = (address) os::Posix::ucontext_get_pc(uc);

    // Handle ALL stack overflow variations here
    if (sig == SIGSEGV || sig == SIGBUS) {
      address addr = (address) info->si_addr;

      // Make sure the high order byte is sign extended, as it may be masked away by the hardware.
      if ((uintptr_t(addr) & (uintptr_t(1) << 55)) != 0) {
        addr = address(uintptr_t(addr) | (uintptr_t(0xFF) << 56));
      }

      // check if fault address is within thread stack
      if (thread->is_in_full_stack(addr)) {
        // stack overflow
        if (os::Posix::handle_stack_overflow(thread, addr, pc, uc, &stub)) {
          return true; // continue
        }
      }
    }

    // We test if stub is already set (by the stack overflow code
    // above) so it is not overwritten by the code that follows. This
    // check is not required on other platforms, because on other
    // platforms we check for SIGSEGV only or SIGBUS only, where here
    // we have to check for both SIGSEGV and SIGBUS.
    if (thread->thread_state() == _thread_in_Java && stub == NULL) {
      // Java thread running in Java code => find exception handler if any
      // a fault inside compiled code, the interpreter, or a stub

      // Handle signal from NativeJump::patch_verified_entry().
      if ((sig == SIGILL)
          && nativeInstruction_at(pc)->is_sigill_zombie_not_entrant()) {
        if (TraceTraps) {
          tty->print_cr("trap: zombie_not_entrant");
        }
        stub = SharedRuntime::get_handle_wrong_method_stub();
      } else if ((sig == SIGSEGV || sig == SIGBUS) && SafepointMechanism::is_poll_address((address)info->si_addr)) {
        stub = SharedRuntime::get_poll_stub(pc);
#if defined(__APPLE__)
      // 32-bit Darwin reports a SIGBUS for nearly all memory access exceptions.
      // 64-bit Darwin may also use a SIGBUS (seen with compressed oops).
      // Catching SIGBUS here prevents the implicit SIGBUS NULL check below from
      // being called, so only do so if the implicit NULL check is not necessary.
      } else if (sig == SIGBUS && !MacroAssembler::uses_implicit_null_check(info->si_addr)) {
#else
      } else if (sig == SIGBUS /* && info->si_code == BUS_OBJERR */) {
#endif
        // BugId 4454115: A read from a MappedByteBuffer can fault
        // here if the underlying file has been truncated.
        // Do not crash the VM in such a case.
        CodeBlob* cb = CodeCache::find_blob_unsafe(pc);
        CompiledMethod* nm = (cb != NULL) ? cb->as_compiled_method_or_null() : NULL;
        bool is_unsafe_arraycopy = (thread->doing_unsafe_access() && UnsafeCopyMemory::contains_pc(pc));
        if ((nm != NULL && nm->has_unsafe_access()) || is_unsafe_arraycopy) {
          address next_pc = pc + NativeCall::instruction_size;
          if (is_unsafe_arraycopy) {
            next_pc = UnsafeCopyMemory::page_error_continue_pc(pc);
          }
          stub = SharedRuntime::handle_unsafe_access(thread, next_pc);
        }
      } else if (sig == SIGILL && nativeInstruction_at(pc)->is_stop()) {
        // Pull a pointer to the error message out of the instruction
        // stream.
        const uint64_t *detail_msg_ptr
          = (uint64_t*)(pc + NativeInstruction::instruction_size);
        const char *detail_msg = (const char *)*detail_msg_ptr;
        const char *msg = "stop";
        if (TraceTraps) {
          tty->print_cr("trap: %s: (SIGILL)", msg);
        }

        // End life with a fatal error, message and detail message and the context.
        // Note: no need to do any post-processing here (e.g. signal chaining)
        report_and_die(thread, uc, NULL, 0, msg, "%s", detail_msg);
        ShouldNotReachHere();

      } else if (sig == SIGFPE &&
          (info->si_code == FPE_INTDIV || info->si_code == FPE_FLTDIV)) {
        stub =
          SharedRuntime::
          continuation_for_implicit_exception(thread,
                                              pc,
                                              SharedRuntime::
                                              IMPLICIT_DIVIDE_BY_ZERO);
      } else if ((sig == SIGSEGV || sig == SIGBUS) &&
                 MacroAssembler::uses_implicit_null_check(info->si_addr)) {
          // Determination of interpreter/vtable stub/compiled code null exception
          stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::IMPLICIT_NULL);
      }
    } else if ((thread->thread_state() == _thread_in_vm ||
                 thread->thread_state() == _thread_in_native) &&
               sig == SIGBUS && /* info->si_code == BUS_OBJERR && */
               thread->doing_unsafe_access()) {
      address next_pc = pc + NativeCall::instruction_size;
      if (UnsafeCopyMemory::contains_pc(pc)) {
        next_pc = UnsafeCopyMemory::page_error_continue_pc(pc);
      }
      stub = SharedRuntime::handle_unsafe_access(thread, next_pc);
    }

    // jni_fast_Get<Primitive>Field can trap at certain pc's if a GC kicks in
    // and the heap gets shrunk before the field access.
    if ((sig == SIGSEGV) || (sig == SIGBUS)) {
      address addr = JNI_FastGetField::find_slowcase_pc(pc);
      if (addr != (address)-1) {
        stub = addr;
      }
    }
  }

  if (stub != NULL) {
    // save all thread context in case we need to restore it
    if (thread != NULL) thread->set_saved_exception_pc(pc);

    os::Posix::ucontext_set_pc(uc, stub);
    return true;
  }

  return false; // Mute compiler
}

void os::Bsd::init_thread_fpu_state(void) {
}

bool os::is_allocatable(size_t bytes) {
  return true;
}

////////////////////////////////////////////////////////////////////////////////
// thread stack

// Minimum usable stack sizes required to get to user code. Space for
// HotSpot guard pages is added later.
size_t os::Posix::_compiler_thread_min_stack_allowed = 72 * K;
size_t os::Posix::_java_thread_min_stack_allowed = 72 * K;
size_t os::Posix::_vm_internal_thread_min_stack_allowed = 72 * K;

// return default stack size for thr_type
size_t os::Posix::default_stack_size(os::ThreadType thr_type) {
  // default stack size (compiler thread needs larger stack)
  size_t s = (thr_type == os::compiler_thread ? 4 * M : 1 * M);
  return s;
}

static void current_stack_region(address * bottom, size_t * size) {
#ifdef __APPLE__
  pthread_t self = pthread_self();
  void *stacktop = pthread_get_stackaddr_np(self);
  *size = pthread_get_stacksize_np(self);
  *bottom = (address) stacktop - *size;
#elif defined(__OpenBSD__)
  stack_t ss;
  int rslt = pthread_stackseg_np(pthread_self(), &ss);

  if (rslt != 0)
    fatal("pthread_stackseg_np failed with error = %d", rslt);

  *bottom = (address)((char *)ss.ss_sp - ss.ss_size);
  *size   = ss.ss_size;
#else
  pthread_attr_t attr;

  int rslt = pthread_attr_init(&attr);

  // JVM needs to know exact stack location, abort if it fails
  if (rslt != 0)
    fatal("pthread_attr_init failed with error = %d", rslt);

  rslt = pthread_attr_get_np(pthread_self(), &attr);

  if (rslt != 0)
    fatal("pthread_attr_get_np failed with error = %d", rslt);

  if (pthread_attr_getstackaddr(&attr, (void **)bottom) != 0 ||
    pthread_attr_getstacksize(&attr, size) != 0) {
    fatal("Can not locate current stack attributes!");
  }

  pthread_attr_destroy(&attr);
#endif
  assert(os::current_stack_pointer() >= *bottom &&
         os::current_stack_pointer() < *bottom + *size, "just checking");
}

address os::current_stack_base() {
  address bottom;
  size_t size;
  current_stack_region(&bottom, &size);
  return (bottom + size);
}

size_t os::current_stack_size() {
  // stack size includes normal stack and HotSpot guard pages
  address bottom;
  size_t size;
  current_stack_region(&bottom, &size);
  return size;
}

/////////////////////////////////////////////////////////////////////////////
// helper functions for fatal error handler

void os::print_context(outputStream *st, const void *context) {
  if (context == NULL) return;

  const ucontext_t *uc = (const ucontext_t*)context;
  st->print_cr("Registers:");
  st->print( " x0=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 0]);
  st->print("  x1=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 1]);
  st->print("  x2=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 2]);
  st->print("  x3=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 3]);
  st->cr();
  st->print( " x4=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 4]);
  st->print("  x5=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 5]);
  st->print("  x6=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 6]);
  st->print("  x7=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 7]);
  st->cr();
  st->print( " x8=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 8]);
  st->print("  x9=" INTPTR_FORMAT, (intptr_t)uc->context_x[ 9]);
  st->print(" x10=" INTPTR_FORMAT, (intptr_t)uc->context_x[10]);
  st->print(" x11=" INTPTR_FORMAT, (intptr_t)uc->context_x[11]);
  st->cr();
  st->print( "x12=" INTPTR_FORMAT, (intptr_t)uc->context_x[12]);
  st->print(" x13=" INTPTR_FORMAT, (intptr_t)uc->context_x[13]);
  st->print(" x14=" INTPTR_FORMAT, (intptr_t)uc->context_x[14]);
  st->print(" x15=" INTPTR_FORMAT, (intptr_t)uc->context_x[15]);
  st->cr();
  st->print( "x16=" INTPTR_FORMAT, (intptr_t)uc->context_x[16]);
  st->print(" x17=" INTPTR_FORMAT, (intptr_t)uc->context_x[17]);
  st->print(" x18=" INTPTR_FORMAT, (intptr_t)uc->context_x[18]);
  st->print(" x19=" INTPTR_FORMAT, (intptr_t)uc->context_x[19]);
  st->cr();
  st->print( "x20=" INTPTR_FORMAT, (intptr_t)uc->context_x[20]);
  st->print(" x21=" INTPTR_FORMAT, (intptr_t)uc->context_x[21]);
  st->print(" x22=" INTPTR_FORMAT, (intptr_t)uc->context_x[22]);
  st->print(" x23=" INTPTR_FORMAT, (intptr_t)uc->context_x[23]);
  st->cr();
  st->print( "x24=" INTPTR_FORMAT, (intptr_t)uc->context_x[24]);
  st->print(" x25=" INTPTR_FORMAT, (intptr_t)uc->context_x[25]);
  st->print(" x26=" INTPTR_FORMAT, (intptr_t)uc->context_x[26]);
  st->print(" x27=" INTPTR_FORMAT, (intptr_t)uc->context_x[27]);
  st->cr();
  st->print( "x28=" INTPTR_FORMAT, (intptr_t)uc->context_x[28]);
  st->print("  fp=" INTPTR_FORMAT, (intptr_t)uc->context_fp);
  st->print("  lr=" INTPTR_FORMAT, (intptr_t)uc->context_lr);
  st->print("  sp=" INTPTR_FORMAT, (intptr_t)uc->context_sp);
  st->cr();
  st->print(  "pc=" INTPTR_FORMAT,  (intptr_t)uc->context_pc);
  st->print(" cpsr=" INTPTR_FORMAT, (intptr_t)uc->context_cpsr);
  st->cr();

  intptr_t *sp = (intptr_t *)os::Bsd::ucontext_get_sp(uc);
  st->print_cr("Top of Stack: (sp=" INTPTR_FORMAT ")", (intptr_t)sp);
  print_hex_dump(st, (address)sp, (address)(sp + 8*sizeof(intptr_t)), sizeof(intptr_t));
  st->cr();

  // Note: it may be unsafe to inspect memory near pc. For example, pc may
  // point to garbage if entry point in an nmethod is corrupted. Leave
  // this at the end, and hope for the best.
  address pc = os::Posix::ucontext_get_pc(uc);
  print_instructions(st, pc, 4/*native instruction size*/);
  st->cr();
}

void os::print_register_info(outputStream *st, const void *context) {
  if (context == NULL) return;

  const ucontext_t *uc = (const ucontext_t*)context;

  st->print_cr("Register to memory mapping:");
  st->cr();

  // this is horrendously verbose but the layout of the registers in the
  // context does not match how we defined our abstract Register set, so
  // we can't just iterate through the gregs area

  // this is only for the "general purpose" registers

  st->print(" x0="); print_location(st, uc->context_x[ 0]);
  st->print(" x1="); print_location(st, uc->context_x[ 1]);
  st->print(" x2="); print_location(st, uc->context_x[ 2]);
  st->print(" x3="); print_location(st, uc->context_x[ 3]);
  st->print(" x4="); print_location(st, uc->context_x[ 4]);
  st->print(" x5="); print_location(st, uc->context_x[ 5]);
  st->print(" x6="); print_location(st, uc->context_x[ 6]);
  st->print(" x7="); print_location(st, uc->context_x[ 7]);
  st->print(" x8="); print_location(st, uc->context_x[ 8]);
  st->print(" x9="); print_location(st, uc->context_x[ 9]);
  st->print("x10="); print_location(st, uc->context_x[10]);
  st->print("x11="); print_location(st, uc->context_x[11]);
  st->print("x12="); print_location(st, uc->context_x[12]);
  st->print("x13="); print_location(st, uc->context_x[13]);
  st->print("x14="); print_location(st, uc->context_x[14]);
  st->print("x15="); print_location(st, uc->context_x[15]);
  st->print("x16="); print_location(st, uc->context_x[16]);
  st->print("x17="); print_location(st, uc->context_x[17]);
  st->print("x18="); print_location(st, uc->context_x[18]);
  st->print("x19="); print_location(st, uc->context_x[19]);
  st->print("x20="); print_location(st, uc->context_x[20]);
  st->print("x21="); print_location(st, uc->context_x[21]);
  st->print("x22="); print_location(st, uc->context_x[22]);
  st->print("x23="); print_location(st, uc->context_x[23]);
  st->print("x24="); print_location(st, uc->context_x[24]);
  st->print("x25="); print_location(st, uc->context_x[25]);
  st->print("x26="); print_location(st, uc->context_x[26]);
  st->print("x27="); print_location(st, uc->context_x[27]);
  st->print("x28="); print_location(st, uc->context_x[28]);

  st->cr();
}

void os::setup_fpu() {
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

void os::current_thread_enable_wx(WXMode mode) {
  pthread_jit_write_protect_np(mode == WXExec);
}

extern "C" {
  int SpinPause() {
    return 0;
  }

  void _Copy_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
    if (from > to) {
      const jshort *end = from + count;
      while (from < end)
        *(to++) = *(from++);
    }
    else if (from < to) {
      const jshort *end = from;
      from += count - 1;
      to   += count - 1;
      while (from >= end)
        *(to--) = *(from--);
    }
  }
  void _Copy_conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
    if (from > to) {
      const jint *end = from + count;
      while (from < end)
        *(to++) = *(from++);
    }
    else if (from < to) {
      const jint *end = from;
      from += count - 1;
      to   += count - 1;
      while (from >= end)
        *(to--) = *(from--);
    }
  }
  void _Copy_conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {
    if (from > to) {
      const jlong *end = from + count;
      while (from < end)
        os::atomic_copy64(from++, to++);
    }
    else if (from < to) {
      const jlong *end = from;
      from += count - 1;
      to   += count - 1;
      while (from >= end)
        os::atomic_copy64(from--, to--);
    }
  }

  void _Copy_arrayof_conjoint_bytes(const HeapWord* from,
                                    HeapWord* to,
                                    size_t    count) {
    memmove(to, from, count);
  }
  void _Copy_arrayof_conjoint_jshorts(const HeapWord* from,
                                      HeapWord* to,
                                      size_t    count) {
    memmove(to, from, count * 2);
  }
  void _Copy_arrayof_conjoint_jints(const HeapWord* from,
                                    HeapWord* to,
                                    size_t    count) {
    memmove(to, from, count * 4);
  }
  void _Copy_arrayof_conjoint_jlongs(const HeapWord* from,
                                     HeapWord* to,
                                     size_t    count) {
    memmove(to, from, count * 8);
  }
};
