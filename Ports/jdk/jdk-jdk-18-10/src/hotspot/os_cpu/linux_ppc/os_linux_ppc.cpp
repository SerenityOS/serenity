/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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
#include "assembler_ppc.hpp"
#include "asm/assembler.inline.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/allocation.inline.hpp"
#include "nativeInst_ppc.hpp"
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
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"
#include "runtime/vm_version.hpp"
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


address os::current_stack_pointer() {
  return (address)__builtin_frame_address(0);
}

char* os::non_memory_address_word() {
  // Must never look like an address returned by reserve_memory,
  // even in its subfields (as defined by the CPU immediate fields,
  // if the CPU splits constants across multiple instructions).

  return (char*) -1;
}

// Frame information (pc, sp, fp) retrieved via ucontext
// always looks like a C-frame according to the frame
// conventions in frame_ppc64.hpp.
address os::Posix::ucontext_get_pc(const ucontext_t * uc) {
  // On powerpc64, ucontext_t is not selfcontained but contains
  // a pointer to an optional substructure (mcontext_t.regs) containing the volatile
  // registers - NIP, among others.
  // This substructure may or may not be there depending where uc came from:
  // - if uc was handed over as the argument to a sigaction handler, a pointer to the
  //   substructure was provided by the kernel when calling the signal handler, and
  //   regs->nip can be accessed.
  // - if uc was filled by getcontext(), it is undefined - getcontext() does not fill
  //   it because the volatile registers are not needed to make setcontext() work.
  //   Hopefully it was zero'd out beforehand.
  guarantee(uc->uc_mcontext.regs != NULL, "only use ucontext_get_pc in sigaction context");
  return (address)uc->uc_mcontext.regs->nip;
}

// modify PC in ucontext.
// Note: Only use this for an ucontext handed down to a signal handler. See comment
// in ucontext_get_pc.
void os::Posix::ucontext_set_pc(ucontext_t * uc, address pc) {
  guarantee(uc->uc_mcontext.regs != NULL, "only use ucontext_set_pc in sigaction context");
  uc->uc_mcontext.regs->nip = (unsigned long)pc;
}

static address ucontext_get_lr(const ucontext_t * uc) {
  return (address)uc->uc_mcontext.regs->link;
}

intptr_t* os::Linux::ucontext_get_sp(const ucontext_t * uc) {
  return (intptr_t*)uc->uc_mcontext.regs->gpr[1/*REG_SP*/];
}

intptr_t* os::Linux::ucontext_get_fp(const ucontext_t * uc) {
  return NULL;
}

static unsigned long ucontext_get_trap(const ucontext_t * uc) {
  return uc->uc_mcontext.regs->trap;
}

address os::fetch_frame_from_context(const void* ucVoid,
                    intptr_t** ret_sp, intptr_t** ret_fp) {

  address epc;
  const ucontext_t* uc = (const ucontext_t*)ucVoid;

  if (uc != NULL) {
    epc = os::Posix::ucontext_get_pc(uc);
    if (ret_sp) *ret_sp = os::Linux::ucontext_get_sp(uc);
    if (ret_fp) *ret_fp = os::Linux::ucontext_get_fp(uc);
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
  return frame(sp, epc);
}

frame os::fetch_compiled_frame_from_context(const void* ucVoid) {
  const ucontext_t* uc = (const ucontext_t*)ucVoid;
  intptr_t* sp = os::Linux::ucontext_get_sp(uc);
  address lr = ucontext_get_lr(uc);
  return frame(sp, lr);
}

frame os::get_sender_for_C_frame(frame* fr) {
  if (*fr->sp() == 0) {
    // fr is the last C frame
    return frame(NULL, NULL);
  }
  return frame(fr->sender_sp(), fr->sender_pc());
}


frame os::current_frame() {
  intptr_t* csp = *(intptr_t**) __builtin_frame_address(0);
  frame topframe(csp, CAST_FROM_FN_PTR(address, os::current_frame));
  return os::get_sender_for_C_frame(&topframe);
}

bool PosixSignals::pd_hotspot_signal_handler(int sig, siginfo_t* info,
                                             ucontext_t* uc, JavaThread* thread) {

  // Make the signal handler transaction-aware by checking the existence of a
  // second (transactional) context with MSR TS bits active. If the signal is
  // caught during a transaction, then just return to the HTM abort handler.
  // Please refer to Linux kernel document powerpc/transactional_memory.txt,
  // section "Signals".
  if (uc && uc->uc_link) {
    ucontext_t* second_uc = uc->uc_link;

    // MSR TS bits are 29 and 30 (Power ISA, v2.07B, Book III-S, pp. 857-858,
    // 3.2.1 "Machine State Register"), however note that ISA notation for bit
    // numbering is MSB 0, so for normal bit numbering (LSB 0) they come to be
    // bits 33 and 34. It's not related to endianness, just a notation matter.
    if (second_uc->uc_mcontext.regs->msr & 0x600000000) {
      if (TraceTraps) {
        tty->print_cr("caught signal in transaction, "
                        "ignoring to jump to abort handler");
      }
      // Return control to the HTM abort handler.
      return true;
    }
  }

  // decide if this trap can be handled by a stub
  address stub = NULL;
  address pc   = NULL;

  if (info != NULL && uc != NULL && thread != NULL) {
    pc = (address) os::Posix::ucontext_get_pc(uc);

    // Handle ALL stack overflow variations here
    if (sig == SIGSEGV) {
      // si_addr may not be valid due to a bug in the linux-ppc64 kernel (see
      // comment below). Use get_stack_bang_address instead of si_addr.
      // If SIGSEGV is caused due to a branch to an invalid address an
      // "Instruction Storage Interrupt" is generated and 'pc' (NIP) already
      // contains the invalid address. Otherwise, the SIGSEGV is caused due to
      // load/store instruction trying to load/store from/to an invalid address
      // and causing a "Data Storage Interrupt", so we inspect the intruction
      // in order to extract the faulty data addresss.
      address addr;
      if ((ucontext_get_trap(uc) & 0x0F00 /* no IRQ reply bits */) == 0x0400) {
        // Instruction Storage Interrupt (ISI)
        addr = pc;
      } else {
        // Data Storage Interrupt (DSI), i.e. 0x0300: extract faulty data address
        addr = ((NativeInstruction*)pc)->get_stack_bang_address(uc);
      }

      // Check if fault address is within thread stack.
      if (thread->is_in_full_stack(addr)) {
        // stack overflow
        if (os::Posix::handle_stack_overflow(thread, addr, pc, uc, &stub)) {
          return true; // continue
        }
      }
    }

    if (thread->thread_state() == _thread_in_Java) {
      // Java thread running in Java code => find exception handler if any
      // a fault inside compiled code, the interpreter, or a stub

      CodeBlob *cb = NULL;
      int stop_type = -1;
      // Handle signal from NativeJump::patch_verified_entry().
      if (sig == SIGILL && nativeInstruction_at(pc)->is_sigill_zombie_not_entrant()) {
        if (TraceTraps) {
          tty->print_cr("trap: zombie_not_entrant");
        }
        stub = SharedRuntime::get_handle_wrong_method_stub();
      }

      else if ((sig == USE_POLL_BIT_ONLY ? SIGTRAP : SIGSEGV) &&
               // A linux-ppc64 kernel before 2.6.6 doesn't set si_addr on some segfaults
               // in 64bit mode (cf. http://www.kernel.org/pub/linux/kernel/v2.6/ChangeLog-2.6.6),
               // especially when we try to read from the safepoint polling page. So the check
               //   (address)info->si_addr == os::get_standard_polling_page()
               // doesn't work for us. We use:
               ((NativeInstruction*)pc)->is_safepoint_poll() &&
               CodeCache::contains((void*) pc) &&
               ((cb = CodeCache::find_blob(pc)) != NULL) &&
               cb->is_compiled()) {
        if (TraceTraps) {
          tty->print_cr("trap: safepoint_poll at " INTPTR_FORMAT " (%s)", p2i(pc),
                        USE_POLL_BIT_ONLY ? "SIGTRAP" : "SIGSEGV");
        }
        stub = SharedRuntime::get_poll_stub(pc);
      }

      else if (UseSIGTRAP && sig == SIGTRAP &&
               ((NativeInstruction*)pc)->is_safepoint_poll_return() &&
               CodeCache::contains((void*) pc) &&
               ((cb = CodeCache::find_blob(pc)) != NULL) &&
               cb->is_compiled()) {
        if (TraceTraps) {
          tty->print_cr("trap: safepoint_poll at return at " INTPTR_FORMAT " (nmethod)", p2i(pc));
        }
        stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();
      }

      // SIGTRAP-based ic miss check in compiled code.
      else if (sig == SIGTRAP && TrapBasedICMissChecks &&
               nativeInstruction_at(pc)->is_sigtrap_ic_miss_check()) {
        if (TraceTraps) {
          tty->print_cr("trap: ic_miss_check at " INTPTR_FORMAT " (SIGTRAP)", p2i(pc));
        }
        stub = SharedRuntime::get_ic_miss_stub();
      }

      // SIGTRAP-based implicit null check in compiled code.
      else if (sig == SIGTRAP && TrapBasedNullChecks &&
               nativeInstruction_at(pc)->is_sigtrap_null_check()) {
        if (TraceTraps) {
          tty->print_cr("trap: null_check at " INTPTR_FORMAT " (SIGTRAP)", p2i(pc));
        }
        stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::IMPLICIT_NULL);
      }

      // SIGSEGV-based implicit null check in compiled code.
      else if (sig == SIGSEGV && ImplicitNullChecks &&
               CodeCache::contains((void*) pc) &&
               MacroAssembler::uses_implicit_null_check(info->si_addr)) {
        if (TraceTraps) {
          tty->print_cr("trap: null_check at " INTPTR_FORMAT " (SIGSEGV)", p2i(pc));
        }
        stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::IMPLICIT_NULL);
      }

#ifdef COMPILER2
      // SIGTRAP-based implicit range check in compiled code.
      else if (sig == SIGTRAP && TrapBasedRangeChecks &&
               nativeInstruction_at(pc)->is_sigtrap_range_check()) {
        if (TraceTraps) {
          tty->print_cr("trap: range_check at " INTPTR_FORMAT " (SIGTRAP)", p2i(pc));
        }
        stub = SharedRuntime::continuation_for_implicit_exception(thread, pc, SharedRuntime::IMPLICIT_NULL);
      }
#endif

      // stop on request
      else if (sig == SIGTRAP && (stop_type = nativeInstruction_at(pc)->get_stop_type()) != -1) {
        bool msg_present = (stop_type & MacroAssembler::stop_msg_present);
        stop_type = (stop_type &~ MacroAssembler::stop_msg_present);

        const char *msg = NULL;
        switch (stop_type) {
          case MacroAssembler::stop_stop              : msg = "stop"; break;
          case MacroAssembler::stop_untested          : msg = "untested"; break;
          case MacroAssembler::stop_unimplemented     : msg = "unimplemented"; break;
          case MacroAssembler::stop_shouldnotreachhere: msg = "shouldnotreachhere"; break;
          default: msg = "unknown"; break;
        }

        const char **detail_msg_ptr = (const char**)(pc + 4);
        const char *detail_msg = msg_present ? *detail_msg_ptr : "no details provided";

        if (TraceTraps) {
          tty->print_cr("trap: %s: %s (SIGTRAP, stop type %d)", msg, detail_msg, stop_type);
        }

        // End life with a fatal error, message and detail message and the context.
        // Note: no need to do any post-processing here (e.g. signal chaining)
        va_list va_dummy;
        VMError::report_and_die(thread, uc, NULL, 0, msg, detail_msg, va_dummy);
        va_end(va_dummy);

        ShouldNotReachHere();

      }

      else if (sig == SIGBUS) {
        // BugId 4454115: A read from a MappedByteBuffer can fault here if the
        // underlying file has been truncated. Do not crash the VM in such a case.
        CodeBlob* cb = CodeCache::find_blob_unsafe(pc);
        CompiledMethod* nm = (cb != NULL) ? cb->as_compiled_method_or_null() : NULL;
        bool is_unsafe_arraycopy = (thread->doing_unsafe_access() && UnsafeCopyMemory::contains_pc(pc));
        if ((nm != NULL && nm->has_unsafe_access()) || is_unsafe_arraycopy) {
          address next_pc = pc + 4;
          if (is_unsafe_arraycopy) {
            next_pc = UnsafeCopyMemory::page_error_continue_pc(pc);
          }
          next_pc = SharedRuntime::handle_unsafe_access(thread, next_pc);
          os::Posix::ucontext_set_pc(uc, next_pc);
          return true;
        }
      }
    }

    else { // thread->thread_state() != _thread_in_Java
      if (sig == SIGILL && VM_Version::is_determine_features_test_running()) {
        // SIGILL must be caused by VM_Version::determine_features().
        *(int *)pc = 0; // patch instruction to 0 to indicate that it causes a SIGILL,
                        // flushing of icache is not necessary.
        stub = pc + 4;  // continue with next instruction.
      }
      else if ((thread->thread_state() == _thread_in_vm ||
                thread->thread_state() == _thread_in_native) &&
               sig == SIGBUS && thread->doing_unsafe_access()) {
        address next_pc = pc + 4;
        if (UnsafeCopyMemory::contains_pc(pc)) {
          next_pc = UnsafeCopyMemory::page_error_continue_pc(pc);
        }
        next_pc = SharedRuntime::handle_unsafe_access(thread, next_pc);
        os::Posix::ucontext_set_pc(uc, next_pc);
        return true;
      }
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
    // Save all thread context in case we need to restore it.
    if (thread != NULL) thread->set_saved_exception_pc(pc);
    os::Posix::ucontext_set_pc(uc, stub);
    return true;
  }

  return false;
}

void os::Linux::init_thread_fpu_state(void) {
  // Disable FP exceptions.
  __asm__ __volatile__ ("mtfsfi 6,0");
}

int os::Linux::get_fpu_control_word(void) {
  // x86 has problems with FPU precision after pthread_cond_timedwait().
  // nothing to do on ppc64.
  return 0;
}

void os::Linux::set_fpu_control_word(int fpu_control) {
  // x86 has problems with FPU precision after pthread_cond_timedwait().
  // nothing to do on ppc64.
}

////////////////////////////////////////////////////////////////////////////////
// thread stack

// Minimum usable stack sizes required to get to user code. Space for
// HotSpot guard pages is added later.
size_t os::Posix::_compiler_thread_min_stack_allowed = 64 * K;
size_t os::Posix::_java_thread_min_stack_allowed = 64 * K;
size_t os::Posix::_vm_internal_thread_min_stack_allowed = 64 * K;

// Return default stack size for thr_type.
size_t os::Posix::default_stack_size(os::ThreadType thr_type) {
  // Default stack size (compiler thread needs larger stack).
  size_t s = (thr_type == os::compiler_thread ? 4 * M : 1024 * K);
  return s;
}

/////////////////////////////////////////////////////////////////////////////
// helper functions for fatal error handler

void os::print_context(outputStream *st, const void *context) {
  if (context == NULL) return;

  const ucontext_t* uc = (const ucontext_t*)context;

  st->print_cr("Registers:");
  st->print("pc =" INTPTR_FORMAT "  ", uc->uc_mcontext.regs->nip);
  st->print("lr =" INTPTR_FORMAT "  ", uc->uc_mcontext.regs->link);
  st->print("ctr=" INTPTR_FORMAT "  ", uc->uc_mcontext.regs->ctr);
  st->cr();
  for (int i = 0; i < 32; i++) {
    st->print("r%-2d=" INTPTR_FORMAT "  ", i, uc->uc_mcontext.regs->gpr[i]);
    if (i % 3 == 2) st->cr();
  }
  st->cr();
  st->cr();

  intptr_t *sp = (intptr_t *)os::Linux::ucontext_get_sp(uc);
  st->print_cr("Top of Stack: (sp=" PTR_FORMAT ")", p2i(sp));
  print_hex_dump(st, (address)sp, (address)(sp + 128), sizeof(intptr_t));
  st->cr();

  // Note: it may be unsafe to inspect memory near pc. For example, pc may
  // point to garbage if entry point in an nmethod is corrupted. Leave
  // this at the end, and hope for the best.
  address pc = os::Posix::ucontext_get_pc(uc);
  print_instructions(st, pc, /*instrsize=*/4);
  st->cr();
}

void os::print_register_info(outputStream *st, const void *context) {
  if (context == NULL) return;

  const ucontext_t *uc = (const ucontext_t*)context;

  st->print_cr("Register to memory mapping:");
  st->cr();

  st->print("pc ="); print_location(st, (intptr_t)uc->uc_mcontext.regs->nip);
  st->print("lr ="); print_location(st, (intptr_t)uc->uc_mcontext.regs->link);
  st->print("ctr ="); print_location(st, (intptr_t)uc->uc_mcontext.regs->ctr);
  for (int i = 0; i < 32; i++) {
    st->print("r%-2d=", i);
    print_location(st, uc->uc_mcontext.regs->gpr[i]);
  }
  st->cr();
}

extern "C" {
  int SpinPause() {
    return 0;
  }
}

#ifndef PRODUCT
void os::verify_stack_alignment() {
  assert(((intptr_t)os::current_stack_pointer() & (StackAlignmentInBytes-1)) == 0, "incorrect stack alignment");
}
#endif

int os::extra_bang_size_in_bytes() {
  // PPC does not require the additional stack bang.
  return 0;
}

#ifdef HAVE_FUNCTION_DESCRIPTORS
void* os::resolve_function_descriptor(void* p) {
  return ((const FunctionDescriptor*)p)->entry();
}
#endif
