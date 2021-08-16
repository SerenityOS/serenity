/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "ci/ciUtilities.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/gc_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/universe.hpp"
#include "nativeInst_x86.hpp"
#include "oops/instanceOop.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/arguments.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif
#if INCLUDE_JVMCI
#include "jvmci/jvmci_globals.hpp"
#endif
#if INCLUDE_ZGC
#include "gc/z/zThreadLocalData.hpp"
#endif

// Declaration and definition of StubGenerator (no .hpp file).
// For a more detailed description of the stub routine structure
// see the comment in stubRoutines.hpp

#define __ _masm->
#define TIMES_OOP (UseCompressedOops ? Address::times_4 : Address::times_8)
#define a__ ((Assembler*)_masm)->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")
const int MXCSR_MASK = 0xFFC0;  // Mask out any pending exceptions

// Stub Code definitions

class StubGenerator: public StubCodeGenerator {
 private:

#ifdef PRODUCT
#define inc_counter_np(counter) ((void)0)
#else
  void inc_counter_np_(int& counter) {
    // This can destroy rscratch1 if counter is far from the code cache
    __ incrementl(ExternalAddress((address)&counter));
  }
#define inc_counter_np(counter) \
  BLOCK_COMMENT("inc_counter " #counter); \
  inc_counter_np_(counter);
#endif

  // Call stubs are used to call Java from C
  //
  // Linux Arguments:
  //    c_rarg0:   call wrapper address                   address
  //    c_rarg1:   result                                 address
  //    c_rarg2:   result type                            BasicType
  //    c_rarg3:   method                                 Method*
  //    c_rarg4:   (interpreter) entry point              address
  //    c_rarg5:   parameters                             intptr_t*
  //    16(rbp): parameter size (in words)              int
  //    24(rbp): thread                                 Thread*
  //
  //     [ return_from_Java     ] <--- rsp
  //     [ argument word n      ]
  //      ...
  // -12 [ argument word 1      ]
  // -11 [ saved r15            ] <--- rsp_after_call
  // -10 [ saved r14            ]
  //  -9 [ saved r13            ]
  //  -8 [ saved r12            ]
  //  -7 [ saved rbx            ]
  //  -6 [ call wrapper         ]
  //  -5 [ result               ]
  //  -4 [ result type          ]
  //  -3 [ method               ]
  //  -2 [ entry point          ]
  //  -1 [ parameters           ]
  //   0 [ saved rbp            ] <--- rbp
  //   1 [ return address       ]
  //   2 [ parameter size       ]
  //   3 [ thread               ]
  //
  // Windows Arguments:
  //    c_rarg0:   call wrapper address                   address
  //    c_rarg1:   result                                 address
  //    c_rarg2:   result type                            BasicType
  //    c_rarg3:   method                                 Method*
  //    48(rbp): (interpreter) entry point              address
  //    56(rbp): parameters                             intptr_t*
  //    64(rbp): parameter size (in words)              int
  //    72(rbp): thread                                 Thread*
  //
  //     [ return_from_Java     ] <--- rsp
  //     [ argument word n      ]
  //      ...
  // -60 [ argument word 1      ]
  // -59 [ saved xmm31          ] <--- rsp after_call
  //     [ saved xmm16-xmm30    ] (EVEX enabled, else the space is blank)
  // -27 [ saved xmm15          ]
  //     [ saved xmm7-xmm14     ]
  //  -9 [ saved xmm6           ] (each xmm register takes 2 slots)
  //  -7 [ saved r15            ]
  //  -6 [ saved r14            ]
  //  -5 [ saved r13            ]
  //  -4 [ saved r12            ]
  //  -3 [ saved rdi            ]
  //  -2 [ saved rsi            ]
  //  -1 [ saved rbx            ]
  //   0 [ saved rbp            ] <--- rbp
  //   1 [ return address       ]
  //   2 [ call wrapper         ]
  //   3 [ result               ]
  //   4 [ result type          ]
  //   5 [ method               ]
  //   6 [ entry point          ]
  //   7 [ parameters           ]
  //   8 [ parameter size       ]
  //   9 [ thread               ]
  //
  //    Windows reserves the callers stack space for arguments 1-4.
  //    We spill c_rarg0-c_rarg3 to this space.

  // Call stub stack layout word offsets from rbp
  enum call_stub_layout {
#ifdef _WIN64
    xmm_save_first     = 6,  // save from xmm6
    xmm_save_last      = 31, // to xmm31
    xmm_save_base      = -9,
    rsp_after_call_off = xmm_save_base - 2 * (xmm_save_last - xmm_save_first), // -27
    r15_off            = -7,
    r14_off            = -6,
    r13_off            = -5,
    r12_off            = -4,
    rdi_off            = -3,
    rsi_off            = -2,
    rbx_off            = -1,
    rbp_off            =  0,
    retaddr_off        =  1,
    call_wrapper_off   =  2,
    result_off         =  3,
    result_type_off    =  4,
    method_off         =  5,
    entry_point_off    =  6,
    parameters_off     =  7,
    parameter_size_off =  8,
    thread_off         =  9
#else
    rsp_after_call_off = -12,
    mxcsr_off          = rsp_after_call_off,
    r15_off            = -11,
    r14_off            = -10,
    r13_off            = -9,
    r12_off            = -8,
    rbx_off            = -7,
    call_wrapper_off   = -6,
    result_off         = -5,
    result_type_off    = -4,
    method_off         = -3,
    entry_point_off    = -2,
    parameters_off     = -1,
    rbp_off            =  0,
    retaddr_off        =  1,
    parameter_size_off =  2,
    thread_off         =  3
#endif
  };

#ifdef _WIN64
  Address xmm_save(int reg) {
    assert(reg >= xmm_save_first && reg <= xmm_save_last, "XMM register number out of range");
    return Address(rbp, (xmm_save_base - (reg - xmm_save_first) * 2) * wordSize);
  }
#endif

  address generate_call_stub(address& return_address) {
    assert((int)frame::entry_frame_after_call_words == -(int)rsp_after_call_off + 1 &&
           (int)frame::entry_frame_call_wrapper_offset == (int)call_wrapper_off,
           "adjust this code");
    StubCodeMark mark(this, "StubRoutines", "call_stub");
    address start = __ pc();

    // same as in generate_catch_exception()!
    const Address rsp_after_call(rbp, rsp_after_call_off * wordSize);

    const Address call_wrapper  (rbp, call_wrapper_off   * wordSize);
    const Address result        (rbp, result_off         * wordSize);
    const Address result_type   (rbp, result_type_off    * wordSize);
    const Address method        (rbp, method_off         * wordSize);
    const Address entry_point   (rbp, entry_point_off    * wordSize);
    const Address parameters    (rbp, parameters_off     * wordSize);
    const Address parameter_size(rbp, parameter_size_off * wordSize);

    // same as in generate_catch_exception()!
    const Address thread        (rbp, thread_off         * wordSize);

    const Address r15_save(rbp, r15_off * wordSize);
    const Address r14_save(rbp, r14_off * wordSize);
    const Address r13_save(rbp, r13_off * wordSize);
    const Address r12_save(rbp, r12_off * wordSize);
    const Address rbx_save(rbp, rbx_off * wordSize);

    // stub code
    __ enter();
    __ subptr(rsp, -rsp_after_call_off * wordSize);

    // save register parameters
#ifndef _WIN64
    __ movptr(parameters,   c_rarg5); // parameters
    __ movptr(entry_point,  c_rarg4); // entry_point
#endif

    __ movptr(method,       c_rarg3); // method
    __ movl(result_type,  c_rarg2);   // result type
    __ movptr(result,       c_rarg1); // result
    __ movptr(call_wrapper, c_rarg0); // call wrapper

    // save regs belonging to calling function
    __ movptr(rbx_save, rbx);
    __ movptr(r12_save, r12);
    __ movptr(r13_save, r13);
    __ movptr(r14_save, r14);
    __ movptr(r15_save, r15);

#ifdef _WIN64
    int last_reg = 15;
    if (UseAVX > 2) {
      last_reg = 31;
    }
    if (VM_Version::supports_evex()) {
      for (int i = xmm_save_first; i <= last_reg; i++) {
        __ vextractf32x4(xmm_save(i), as_XMMRegister(i), 0);
      }
    } else {
      for (int i = xmm_save_first; i <= last_reg; i++) {
        __ movdqu(xmm_save(i), as_XMMRegister(i));
      }
    }

    const Address rdi_save(rbp, rdi_off * wordSize);
    const Address rsi_save(rbp, rsi_off * wordSize);

    __ movptr(rsi_save, rsi);
    __ movptr(rdi_save, rdi);
#else
    const Address mxcsr_save(rbp, mxcsr_off * wordSize);
    {
      Label skip_ldmx;
      __ stmxcsr(mxcsr_save);
      __ movl(rax, mxcsr_save);
      __ andl(rax, MXCSR_MASK);    // Only check control and mask bits
      ExternalAddress mxcsr_std(StubRoutines::x86::addr_mxcsr_std());
      __ cmp32(rax, mxcsr_std);
      __ jcc(Assembler::equal, skip_ldmx);
      __ ldmxcsr(mxcsr_std);
      __ bind(skip_ldmx);
    }
#endif

    // Load up thread register
    __ movptr(r15_thread, thread);
    __ reinit_heapbase();

#ifdef ASSERT
    // make sure we have no pending exceptions
    {
      Label L;
      __ cmpptr(Address(r15_thread, Thread::pending_exception_offset()), (int32_t)NULL_WORD);
      __ jcc(Assembler::equal, L);
      __ stop("StubRoutines::call_stub: entered with pending exception");
      __ bind(L);
    }
#endif

    // pass parameters if any
    BLOCK_COMMENT("pass parameters if any");
    Label parameters_done;
    __ movl(c_rarg3, parameter_size);
    __ testl(c_rarg3, c_rarg3);
    __ jcc(Assembler::zero, parameters_done);

    Label loop;
    __ movptr(c_rarg2, parameters);       // parameter pointer
    __ movl(c_rarg1, c_rarg3);            // parameter counter is in c_rarg1
    __ BIND(loop);
    __ movptr(rax, Address(c_rarg2, 0));// get parameter
    __ addptr(c_rarg2, wordSize);       // advance to next parameter
    __ decrementl(c_rarg1);             // decrement counter
    __ push(rax);                       // pass parameter
    __ jcc(Assembler::notZero, loop);

    // call Java function
    __ BIND(parameters_done);
    __ movptr(rbx, method);             // get Method*
    __ movptr(c_rarg1, entry_point);    // get entry_point
    __ mov(r13, rsp);                   // set sender sp
    BLOCK_COMMENT("call Java function");
    __ call(c_rarg1);

    BLOCK_COMMENT("call_stub_return_address:");
    return_address = __ pc();

    // store result depending on type (everything that is not
    // T_OBJECT, T_LONG, T_FLOAT or T_DOUBLE is treated as T_INT)
    __ movptr(c_rarg0, result);
    Label is_long, is_float, is_double, exit;
    __ movl(c_rarg1, result_type);
    __ cmpl(c_rarg1, T_OBJECT);
    __ jcc(Assembler::equal, is_long);
    __ cmpl(c_rarg1, T_LONG);
    __ jcc(Assembler::equal, is_long);
    __ cmpl(c_rarg1, T_FLOAT);
    __ jcc(Assembler::equal, is_float);
    __ cmpl(c_rarg1, T_DOUBLE);
    __ jcc(Assembler::equal, is_double);

    // handle T_INT case
    __ movl(Address(c_rarg0, 0), rax);

    __ BIND(exit);

    // pop parameters
    __ lea(rsp, rsp_after_call);

#ifdef ASSERT
    // verify that threads correspond
    {
     Label L1, L2, L3;
      __ cmpptr(r15_thread, thread);
      __ jcc(Assembler::equal, L1);
      __ stop("StubRoutines::call_stub: r15_thread is corrupted");
      __ bind(L1);
      __ get_thread(rbx);
      __ cmpptr(r15_thread, thread);
      __ jcc(Assembler::equal, L2);
      __ stop("StubRoutines::call_stub: r15_thread is modified by call");
      __ bind(L2);
      __ cmpptr(r15_thread, rbx);
      __ jcc(Assembler::equal, L3);
      __ stop("StubRoutines::call_stub: threads must correspond");
      __ bind(L3);
    }
#endif

    // restore regs belonging to calling function
#ifdef _WIN64
    // emit the restores for xmm regs
    if (VM_Version::supports_evex()) {
      for (int i = xmm_save_first; i <= last_reg; i++) {
        __ vinsertf32x4(as_XMMRegister(i), as_XMMRegister(i), xmm_save(i), 0);
      }
    } else {
      for (int i = xmm_save_first; i <= last_reg; i++) {
        __ movdqu(as_XMMRegister(i), xmm_save(i));
      }
    }
#endif
    __ movptr(r15, r15_save);
    __ movptr(r14, r14_save);
    __ movptr(r13, r13_save);
    __ movptr(r12, r12_save);
    __ movptr(rbx, rbx_save);

#ifdef _WIN64
    __ movptr(rdi, rdi_save);
    __ movptr(rsi, rsi_save);
#else
    __ ldmxcsr(mxcsr_save);
#endif

    // restore rsp
    __ addptr(rsp, -rsp_after_call_off * wordSize);

    // return
    __ vzeroupper();
    __ pop(rbp);
    __ ret(0);

    // handle return types different from T_INT
    __ BIND(is_long);
    __ movq(Address(c_rarg0, 0), rax);
    __ jmp(exit);

    __ BIND(is_float);
    __ movflt(Address(c_rarg0, 0), xmm0);
    __ jmp(exit);

    __ BIND(is_double);
    __ movdbl(Address(c_rarg0, 0), xmm0);
    __ jmp(exit);

    return start;
  }

  // Return point for a Java call if there's an exception thrown in
  // Java code.  The exception is caught and transformed into a
  // pending exception stored in JavaThread that can be tested from
  // within the VM.
  //
  // Note: Usually the parameters are removed by the callee. In case
  // of an exception crossing an activation frame boundary, that is
  // not the case if the callee is compiled code => need to setup the
  // rsp.
  //
  // rax: exception oop

  address generate_catch_exception() {
    StubCodeMark mark(this, "StubRoutines", "catch_exception");
    address start = __ pc();

    // same as in generate_call_stub():
    const Address rsp_after_call(rbp, rsp_after_call_off * wordSize);
    const Address thread        (rbp, thread_off         * wordSize);

#ifdef ASSERT
    // verify that threads correspond
    {
      Label L1, L2, L3;
      __ cmpptr(r15_thread, thread);
      __ jcc(Assembler::equal, L1);
      __ stop("StubRoutines::catch_exception: r15_thread is corrupted");
      __ bind(L1);
      __ get_thread(rbx);
      __ cmpptr(r15_thread, thread);
      __ jcc(Assembler::equal, L2);
      __ stop("StubRoutines::catch_exception: r15_thread is modified by call");
      __ bind(L2);
      __ cmpptr(r15_thread, rbx);
      __ jcc(Assembler::equal, L3);
      __ stop("StubRoutines::catch_exception: threads must correspond");
      __ bind(L3);
    }
#endif

    // set pending exception
    __ verify_oop(rax);

    __ movptr(Address(r15_thread, Thread::pending_exception_offset()), rax);
    __ lea(rscratch1, ExternalAddress((address)__FILE__));
    __ movptr(Address(r15_thread, Thread::exception_file_offset()), rscratch1);
    __ movl(Address(r15_thread, Thread::exception_line_offset()), (int)  __LINE__);

    // complete return to VM
    assert(StubRoutines::_call_stub_return_address != NULL,
           "_call_stub_return_address must have been generated before");
    __ jump(RuntimeAddress(StubRoutines::_call_stub_return_address));

    return start;
  }

  // Continuation point for runtime calls returning with a pending
  // exception.  The pending exception check happened in the runtime
  // or native call stub.  The pending exception in Thread is
  // converted into a Java-level exception.
  //
  // Contract with Java-level exception handlers:
  // rax: exception
  // rdx: throwing pc
  //
  // NOTE: At entry of this stub, exception-pc must be on stack !!

  address generate_forward_exception() {
    StubCodeMark mark(this, "StubRoutines", "forward exception");
    address start = __ pc();

    // Upon entry, the sp points to the return address returning into
    // Java (interpreted or compiled) code; i.e., the return address
    // becomes the throwing pc.
    //
    // Arguments pushed before the runtime call are still on the stack
    // but the exception handler will reset the stack pointer ->
    // ignore them.  A potential result in registers can be ignored as
    // well.

#ifdef ASSERT
    // make sure this code is only executed if there is a pending exception
    {
      Label L;
      __ cmpptr(Address(r15_thread, Thread::pending_exception_offset()), (int32_t) NULL);
      __ jcc(Assembler::notEqual, L);
      __ stop("StubRoutines::forward exception: no pending exception (1)");
      __ bind(L);
    }
#endif

    // compute exception handler into rbx
    __ movptr(c_rarg0, Address(rsp, 0));
    BLOCK_COMMENT("call exception_handler_for_return_address");
    __ call_VM_leaf(CAST_FROM_FN_PTR(address,
                         SharedRuntime::exception_handler_for_return_address),
                    r15_thread, c_rarg0);
    __ mov(rbx, rax);

    // setup rax & rdx, remove return address & clear pending exception
    __ pop(rdx);
    __ movptr(rax, Address(r15_thread, Thread::pending_exception_offset()));
    __ movptr(Address(r15_thread, Thread::pending_exception_offset()), (int32_t)NULL_WORD);

#ifdef ASSERT
    // make sure exception is set
    {
      Label L;
      __ testptr(rax, rax);
      __ jcc(Assembler::notEqual, L);
      __ stop("StubRoutines::forward exception: no pending exception (2)");
      __ bind(L);
    }
#endif

    // continue at exception handler (return address removed)
    // rax: exception
    // rbx: exception handler
    // rdx: throwing pc
    __ verify_oop(rax);
    __ jmp(rbx);

    return start;
  }

  // Support for intptr_t OrderAccess::fence()
  //
  // Arguments :
  //
  // Result:
  address generate_orderaccess_fence() {
    StubCodeMark mark(this, "StubRoutines", "orderaccess_fence");
    address start = __ pc();
    __ membar(Assembler::StoreLoad);
    __ ret(0);

    return start;
  }


  // Support for intptr_t get_previous_sp()
  //
  // This routine is used to find the previous stack pointer for the
  // caller.
  address generate_get_previous_sp() {
    StubCodeMark mark(this, "StubRoutines", "get_previous_sp");
    address start = __ pc();

    __ movptr(rax, rsp);
    __ addptr(rax, 8); // return address is at the top of the stack.
    __ ret(0);

    return start;
  }

  //----------------------------------------------------------------------------------------------------
  // Support for void verify_mxcsr()
  //
  // This routine is used with -Xcheck:jni to verify that native
  // JNI code does not return to Java code without restoring the
  // MXCSR register to our expected state.

  address generate_verify_mxcsr() {
    StubCodeMark mark(this, "StubRoutines", "verify_mxcsr");
    address start = __ pc();

    const Address mxcsr_save(rsp, 0);

    if (CheckJNICalls) {
      Label ok_ret;
      ExternalAddress mxcsr_std(StubRoutines::x86::addr_mxcsr_std());
      __ push(rax);
      __ subptr(rsp, wordSize);      // allocate a temp location
      __ stmxcsr(mxcsr_save);
      __ movl(rax, mxcsr_save);
      __ andl(rax, MXCSR_MASK);    // Only check control and mask bits
      __ cmp32(rax, mxcsr_std);
      __ jcc(Assembler::equal, ok_ret);

      __ warn("MXCSR changed by native JNI code, use -XX:+RestoreMXCSROnJNICall");

      __ ldmxcsr(mxcsr_std);

      __ bind(ok_ret);
      __ addptr(rsp, wordSize);
      __ pop(rax);
    }

    __ ret(0);

    return start;
  }

  address generate_f2i_fixup() {
    StubCodeMark mark(this, "StubRoutines", "f2i_fixup");
    Address inout(rsp, 5 * wordSize); // return address + 4 saves

    address start = __ pc();

    Label L;

    __ push(rax);
    __ push(c_rarg3);
    __ push(c_rarg2);
    __ push(c_rarg1);

    __ movl(rax, 0x7f800000);
    __ xorl(c_rarg3, c_rarg3);
    __ movl(c_rarg2, inout);
    __ movl(c_rarg1, c_rarg2);
    __ andl(c_rarg1, 0x7fffffff);
    __ cmpl(rax, c_rarg1); // NaN? -> 0
    __ jcc(Assembler::negative, L);
    __ testl(c_rarg2, c_rarg2); // signed ? min_jint : max_jint
    __ movl(c_rarg3, 0x80000000);
    __ movl(rax, 0x7fffffff);
    __ cmovl(Assembler::positive, c_rarg3, rax);

    __ bind(L);
    __ movptr(inout, c_rarg3);

    __ pop(c_rarg1);
    __ pop(c_rarg2);
    __ pop(c_rarg3);
    __ pop(rax);

    __ ret(0);

    return start;
  }

  address generate_f2l_fixup() {
    StubCodeMark mark(this, "StubRoutines", "f2l_fixup");
    Address inout(rsp, 5 * wordSize); // return address + 4 saves
    address start = __ pc();

    Label L;

    __ push(rax);
    __ push(c_rarg3);
    __ push(c_rarg2);
    __ push(c_rarg1);

    __ movl(rax, 0x7f800000);
    __ xorl(c_rarg3, c_rarg3);
    __ movl(c_rarg2, inout);
    __ movl(c_rarg1, c_rarg2);
    __ andl(c_rarg1, 0x7fffffff);
    __ cmpl(rax, c_rarg1); // NaN? -> 0
    __ jcc(Assembler::negative, L);
    __ testl(c_rarg2, c_rarg2); // signed ? min_jlong : max_jlong
    __ mov64(c_rarg3, 0x8000000000000000);
    __ mov64(rax, 0x7fffffffffffffff);
    __ cmov(Assembler::positive, c_rarg3, rax);

    __ bind(L);
    __ movptr(inout, c_rarg3);

    __ pop(c_rarg1);
    __ pop(c_rarg2);
    __ pop(c_rarg3);
    __ pop(rax);

    __ ret(0);

    return start;
  }

  address generate_d2i_fixup() {
    StubCodeMark mark(this, "StubRoutines", "d2i_fixup");
    Address inout(rsp, 6 * wordSize); // return address + 5 saves

    address start = __ pc();

    Label L;

    __ push(rax);
    __ push(c_rarg3);
    __ push(c_rarg2);
    __ push(c_rarg1);
    __ push(c_rarg0);

    __ movl(rax, 0x7ff00000);
    __ movq(c_rarg2, inout);
    __ movl(c_rarg3, c_rarg2);
    __ mov(c_rarg1, c_rarg2);
    __ mov(c_rarg0, c_rarg2);
    __ negl(c_rarg3);
    __ shrptr(c_rarg1, 0x20);
    __ orl(c_rarg3, c_rarg2);
    __ andl(c_rarg1, 0x7fffffff);
    __ xorl(c_rarg2, c_rarg2);
    __ shrl(c_rarg3, 0x1f);
    __ orl(c_rarg1, c_rarg3);
    __ cmpl(rax, c_rarg1);
    __ jcc(Assembler::negative, L); // NaN -> 0
    __ testptr(c_rarg0, c_rarg0); // signed ? min_jint : max_jint
    __ movl(c_rarg2, 0x80000000);
    __ movl(rax, 0x7fffffff);
    __ cmov(Assembler::positive, c_rarg2, rax);

    __ bind(L);
    __ movptr(inout, c_rarg2);

    __ pop(c_rarg0);
    __ pop(c_rarg1);
    __ pop(c_rarg2);
    __ pop(c_rarg3);
    __ pop(rax);

    __ ret(0);

    return start;
  }

  address generate_d2l_fixup() {
    StubCodeMark mark(this, "StubRoutines", "d2l_fixup");
    Address inout(rsp, 6 * wordSize); // return address + 5 saves

    address start = __ pc();

    Label L;

    __ push(rax);
    __ push(c_rarg3);
    __ push(c_rarg2);
    __ push(c_rarg1);
    __ push(c_rarg0);

    __ movl(rax, 0x7ff00000);
    __ movq(c_rarg2, inout);
    __ movl(c_rarg3, c_rarg2);
    __ mov(c_rarg1, c_rarg2);
    __ mov(c_rarg0, c_rarg2);
    __ negl(c_rarg3);
    __ shrptr(c_rarg1, 0x20);
    __ orl(c_rarg3, c_rarg2);
    __ andl(c_rarg1, 0x7fffffff);
    __ xorl(c_rarg2, c_rarg2);
    __ shrl(c_rarg3, 0x1f);
    __ orl(c_rarg1, c_rarg3);
    __ cmpl(rax, c_rarg1);
    __ jcc(Assembler::negative, L); // NaN -> 0
    __ testq(c_rarg0, c_rarg0); // signed ? min_jlong : max_jlong
    __ mov64(c_rarg2, 0x8000000000000000);
    __ mov64(rax, 0x7fffffffffffffff);
    __ cmovq(Assembler::positive, c_rarg2, rax);

    __ bind(L);
    __ movq(inout, c_rarg2);

    __ pop(c_rarg0);
    __ pop(c_rarg1);
    __ pop(c_rarg2);
    __ pop(c_rarg3);
    __ pop(rax);

    __ ret(0);

    return start;
  }

  address generate_iota_indices(const char *stub_name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();
    __ emit_data64(0x0706050403020100, relocInfo::none);
    __ emit_data64(0x0F0E0D0C0B0A0908, relocInfo::none);
    __ emit_data64(0x1716151413121110, relocInfo::none);
    __ emit_data64(0x1F1E1D1C1B1A1918, relocInfo::none);
    __ emit_data64(0x2726252423222120, relocInfo::none);
    __ emit_data64(0x2F2E2D2C2B2A2928, relocInfo::none);
    __ emit_data64(0x3736353433323130, relocInfo::none);
    __ emit_data64(0x3F3E3D3C3B3A3938, relocInfo::none);
    return start;
  }

  address generate_vector_byte_shuffle_mask(const char *stub_name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();
    __ emit_data64(0x7070707070707070, relocInfo::none);
    __ emit_data64(0x7070707070707070, relocInfo::none);
    __ emit_data64(0xF0F0F0F0F0F0F0F0, relocInfo::none);
    __ emit_data64(0xF0F0F0F0F0F0F0F0, relocInfo::none);
    return start;
  }

  address generate_fp_mask(const char *stub_name, int64_t mask) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();

    __ emit_data64( mask, relocInfo::none );
    __ emit_data64( mask, relocInfo::none );

    return start;
  }

  address generate_vector_mask(const char *stub_name, int64_t mask) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();

    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);

    return start;
  }

  address generate_vector_byte_perm_mask(const char *stub_name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();

    __ emit_data64(0x0000000000000001, relocInfo::none);
    __ emit_data64(0x0000000000000003, relocInfo::none);
    __ emit_data64(0x0000000000000005, relocInfo::none);
    __ emit_data64(0x0000000000000007, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000002, relocInfo::none);
    __ emit_data64(0x0000000000000004, relocInfo::none);
    __ emit_data64(0x0000000000000006, relocInfo::none);

    return start;
  }

  address generate_vector_fp_mask(const char *stub_name, int64_t mask) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();

    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);
    __ emit_data64(mask, relocInfo::none);

    return start;
  }

  address generate_vector_custom_i32(const char *stub_name, Assembler::AvxVectorLen len,
                                     int32_t val0, int32_t val1, int32_t val2, int32_t val3,
                                     int32_t val4 = 0, int32_t val5 = 0, int32_t val6 = 0, int32_t val7 = 0,
                                     int32_t val8 = 0, int32_t val9 = 0, int32_t val10 = 0, int32_t val11 = 0,
                                     int32_t val12 = 0, int32_t val13 = 0, int32_t val14 = 0, int32_t val15 = 0) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();

    assert(len != Assembler::AVX_NoVec, "vector len must be specified");
    __ emit_data(val0, relocInfo::none, 0);
    __ emit_data(val1, relocInfo::none, 0);
    __ emit_data(val2, relocInfo::none, 0);
    __ emit_data(val3, relocInfo::none, 0);
    if (len >= Assembler::AVX_256bit) {
      __ emit_data(val4, relocInfo::none, 0);
      __ emit_data(val5, relocInfo::none, 0);
      __ emit_data(val6, relocInfo::none, 0);
      __ emit_data(val7, relocInfo::none, 0);
      if (len >= Assembler::AVX_512bit) {
        __ emit_data(val8, relocInfo::none, 0);
        __ emit_data(val9, relocInfo::none, 0);
        __ emit_data(val10, relocInfo::none, 0);
        __ emit_data(val11, relocInfo::none, 0);
        __ emit_data(val12, relocInfo::none, 0);
        __ emit_data(val13, relocInfo::none, 0);
        __ emit_data(val14, relocInfo::none, 0);
        __ emit_data(val15, relocInfo::none, 0);
      }
    }

    return start;
  }

  // Non-destructive plausibility checks for oops
  //
  // Arguments:
  //    all args on stack!
  //
  // Stack after saving c_rarg3:
  //    [tos + 0]: saved c_rarg3
  //    [tos + 1]: saved c_rarg2
  //    [tos + 2]: saved r12 (several TemplateTable methods use it)
  //    [tos + 3]: saved flags
  //    [tos + 4]: return address
  //  * [tos + 5]: error message (char*)
  //  * [tos + 6]: object to verify (oop)
  //  * [tos + 7]: saved rax - saved by caller and bashed
  //  * [tos + 8]: saved r10 (rscratch1) - saved by caller
  //  * = popped on exit
  address generate_verify_oop() {
    StubCodeMark mark(this, "StubRoutines", "verify_oop");
    address start = __ pc();

    Label exit, error;

    __ pushf();
    __ incrementl(ExternalAddress((address) StubRoutines::verify_oop_count_addr()));

    __ push(r12);

    // save c_rarg2 and c_rarg3
    __ push(c_rarg2);
    __ push(c_rarg3);

    enum {
           // After previous pushes.
           oop_to_verify = 6 * wordSize,
           saved_rax     = 7 * wordSize,
           saved_r10     = 8 * wordSize,

           // Before the call to MacroAssembler::debug(), see below.
           return_addr   = 16 * wordSize,
           error_msg     = 17 * wordSize
    };

    // get object
    __ movptr(rax, Address(rsp, oop_to_verify));

    // make sure object is 'reasonable'
    __ testptr(rax, rax);
    __ jcc(Assembler::zero, exit); // if obj is NULL it is OK

#if INCLUDE_ZGC
    if (UseZGC) {
      // Check if metadata bits indicate a bad oop
      __ testptr(rax, Address(r15_thread, ZThreadLocalData::address_bad_mask_offset()));
      __ jcc(Assembler::notZero, error);
    }
#endif

    // Check if the oop is in the right area of memory
    __ movptr(c_rarg2, rax);
    __ movptr(c_rarg3, (intptr_t) Universe::verify_oop_mask());
    __ andptr(c_rarg2, c_rarg3);
    __ movptr(c_rarg3, (intptr_t) Universe::verify_oop_bits());
    __ cmpptr(c_rarg2, c_rarg3);
    __ jcc(Assembler::notZero, error);

    // make sure klass is 'reasonable', which is not zero.
    __ load_klass(rax, rax, rscratch1);  // get klass
    __ testptr(rax, rax);
    __ jcc(Assembler::zero, error); // if klass is NULL it is broken

    // return if everything seems ok
    __ bind(exit);
    __ movptr(rax, Address(rsp, saved_rax));     // get saved rax back
    __ movptr(rscratch1, Address(rsp, saved_r10)); // get saved r10 back
    __ pop(c_rarg3);                             // restore c_rarg3
    __ pop(c_rarg2);                             // restore c_rarg2
    __ pop(r12);                                 // restore r12
    __ popf();                                   // restore flags
    __ ret(4 * wordSize);                        // pop caller saved stuff

    // handle errors
    __ bind(error);
    __ movptr(rax, Address(rsp, saved_rax));     // get saved rax back
    __ movptr(rscratch1, Address(rsp, saved_r10)); // get saved r10 back
    __ pop(c_rarg3);                             // get saved c_rarg3 back
    __ pop(c_rarg2);                             // get saved c_rarg2 back
    __ pop(r12);                                 // get saved r12 back
    __ popf();                                   // get saved flags off stack --
                                                 // will be ignored

    __ pusha();                                  // push registers
                                                 // (rip is already
                                                 // already pushed)
    // debug(char* msg, int64_t pc, int64_t regs[])
    // We've popped the registers we'd saved (c_rarg3, c_rarg2 and flags), and
    // pushed all the registers, so now the stack looks like:
    //     [tos +  0] 16 saved registers
    //     [tos + 16] return address
    //   * [tos + 17] error message (char*)
    //   * [tos + 18] object to verify (oop)
    //   * [tos + 19] saved rax - saved by caller and bashed
    //   * [tos + 20] saved r10 (rscratch1) - saved by caller
    //   * = popped on exit

    __ movptr(c_rarg0, Address(rsp, error_msg));    // pass address of error message
    __ movptr(c_rarg1, Address(rsp, return_addr));  // pass return address
    __ movq(c_rarg2, rsp);                          // pass address of regs on stack
    __ mov(r12, rsp);                               // remember rsp
    __ subptr(rsp, frame::arg_reg_save_area_bytes); // windows
    __ andptr(rsp, -16);                            // align stack as required by ABI
    BLOCK_COMMENT("call MacroAssembler::debug");
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, MacroAssembler::debug64)));
    __ hlt();
    return start;
  }

  //
  // Verify that a register contains clean 32-bits positive value
  // (high 32-bits are 0) so it could be used in 64-bits shifts.
  //
  //  Input:
  //    Rint  -  32-bits value
  //    Rtmp  -  scratch
  //
  void assert_clean_int(Register Rint, Register Rtmp) {
#ifdef ASSERT
    Label L;
    assert_different_registers(Rtmp, Rint);
    __ movslq(Rtmp, Rint);
    __ cmpq(Rtmp, Rint);
    __ jcc(Assembler::equal, L);
    __ stop("high 32-bits of int value are not 0");
    __ bind(L);
#endif
  }

  //  Generate overlap test for array copy stubs
  //
  //  Input:
  //     c_rarg0 - from
  //     c_rarg1 - to
  //     c_rarg2 - element count
  //
  //  Output:
  //     rax   - &from[element count - 1]
  //
  void array_overlap_test(address no_overlap_target, Address::ScaleFactor sf) {
    assert(no_overlap_target != NULL, "must be generated");
    array_overlap_test(no_overlap_target, NULL, sf);
  }
  void array_overlap_test(Label& L_no_overlap, Address::ScaleFactor sf) {
    array_overlap_test(NULL, &L_no_overlap, sf);
  }
  void array_overlap_test(address no_overlap_target, Label* NOLp, Address::ScaleFactor sf) {
    const Register from     = c_rarg0;
    const Register to       = c_rarg1;
    const Register count    = c_rarg2;
    const Register end_from = rax;

    __ cmpptr(to, from);
    __ lea(end_from, Address(from, count, sf, 0));
    if (NOLp == NULL) {
      ExternalAddress no_overlap(no_overlap_target);
      __ jump_cc(Assembler::belowEqual, no_overlap);
      __ cmpptr(to, end_from);
      __ jump_cc(Assembler::aboveEqual, no_overlap);
    } else {
      __ jcc(Assembler::belowEqual, (*NOLp));
      __ cmpptr(to, end_from);
      __ jcc(Assembler::aboveEqual, (*NOLp));
    }
  }

  // Shuffle first three arg regs on Windows into Linux/Solaris locations.
  //
  // Outputs:
  //    rdi - rcx
  //    rsi - rdx
  //    rdx - r8
  //    rcx - r9
  //
  // Registers r9 and r10 are used to save rdi and rsi on Windows, which latter
  // are non-volatile.  r9 and r10 should not be used by the caller.
  //
  DEBUG_ONLY(bool regs_in_thread;)

  void setup_arg_regs(int nargs = 3) {
    const Register saved_rdi = r9;
    const Register saved_rsi = r10;
    assert(nargs == 3 || nargs == 4, "else fix");
#ifdef _WIN64
    assert(c_rarg0 == rcx && c_rarg1 == rdx && c_rarg2 == r8 && c_rarg3 == r9,
           "unexpected argument registers");
    if (nargs >= 4)
      __ mov(rax, r9);  // r9 is also saved_rdi
    __ movptr(saved_rdi, rdi);
    __ movptr(saved_rsi, rsi);
    __ mov(rdi, rcx); // c_rarg0
    __ mov(rsi, rdx); // c_rarg1
    __ mov(rdx, r8);  // c_rarg2
    if (nargs >= 4)
      __ mov(rcx, rax); // c_rarg3 (via rax)
#else
    assert(c_rarg0 == rdi && c_rarg1 == rsi && c_rarg2 == rdx && c_rarg3 == rcx,
           "unexpected argument registers");
#endif
    DEBUG_ONLY(regs_in_thread = false;)
  }

  void restore_arg_regs() {
    assert(!regs_in_thread, "wrong call to restore_arg_regs");
    const Register saved_rdi = r9;
    const Register saved_rsi = r10;
#ifdef _WIN64
    __ movptr(rdi, saved_rdi);
    __ movptr(rsi, saved_rsi);
#endif
  }

  // This is used in places where r10 is a scratch register, and can
  // be adapted if r9 is needed also.
  void setup_arg_regs_using_thread() {
    const Register saved_r15 = r9;
#ifdef _WIN64
    __ mov(saved_r15, r15);  // r15 is callee saved and needs to be restored
    __ get_thread(r15_thread);
    assert(c_rarg0 == rcx && c_rarg1 == rdx && c_rarg2 == r8 && c_rarg3 == r9,
           "unexpected argument registers");
    __ movptr(Address(r15_thread, in_bytes(JavaThread::windows_saved_rdi_offset())), rdi);
    __ movptr(Address(r15_thread, in_bytes(JavaThread::windows_saved_rsi_offset())), rsi);

    __ mov(rdi, rcx); // c_rarg0
    __ mov(rsi, rdx); // c_rarg1
    __ mov(rdx, r8);  // c_rarg2
#else
    assert(c_rarg0 == rdi && c_rarg1 == rsi && c_rarg2 == rdx && c_rarg3 == rcx,
           "unexpected argument registers");
#endif
    DEBUG_ONLY(regs_in_thread = true;)
  }

  void restore_arg_regs_using_thread() {
    assert(regs_in_thread, "wrong call to restore_arg_regs");
    const Register saved_r15 = r9;
#ifdef _WIN64
    __ get_thread(r15_thread);
    __ movptr(rsi, Address(r15_thread, in_bytes(JavaThread::windows_saved_rsi_offset())));
    __ movptr(rdi, Address(r15_thread, in_bytes(JavaThread::windows_saved_rdi_offset())));
    __ mov(r15, saved_r15);  // r15 is callee saved and needs to be restored
#endif
  }

  // Copy big chunks forward
  //
  // Inputs:
  //   end_from     - source arrays end address
  //   end_to       - destination array end address
  //   qword_count  - 64-bits element count, negative
  //   to           - scratch
  //   L_copy_bytes - entry label
  //   L_copy_8_bytes  - exit  label
  //
  void copy_bytes_forward(Register end_from, Register end_to,
                             Register qword_count, Register to,
                             Label& L_copy_bytes, Label& L_copy_8_bytes) {
    DEBUG_ONLY(__ stop("enter at entry label, not here"));
    Label L_loop;
    __ align(OptoLoopAlignment);
    if (UseUnalignedLoadStores) {
      Label L_end;
      __ BIND(L_loop);
      if (UseAVX >= 2) {
        __ vmovdqu(xmm0, Address(end_from, qword_count, Address::times_8, -56));
        __ vmovdqu(Address(end_to, qword_count, Address::times_8, -56), xmm0);
        __ vmovdqu(xmm1, Address(end_from, qword_count, Address::times_8, -24));
        __ vmovdqu(Address(end_to, qword_count, Address::times_8, -24), xmm1);
      } else {
        __ movdqu(xmm0, Address(end_from, qword_count, Address::times_8, -56));
        __ movdqu(Address(end_to, qword_count, Address::times_8, -56), xmm0);
        __ movdqu(xmm1, Address(end_from, qword_count, Address::times_8, -40));
        __ movdqu(Address(end_to, qword_count, Address::times_8, -40), xmm1);
        __ movdqu(xmm2, Address(end_from, qword_count, Address::times_8, -24));
        __ movdqu(Address(end_to, qword_count, Address::times_8, -24), xmm2);
        __ movdqu(xmm3, Address(end_from, qword_count, Address::times_8, - 8));
        __ movdqu(Address(end_to, qword_count, Address::times_8, - 8), xmm3);
      }

      __ BIND(L_copy_bytes);
      __ addptr(qword_count, 8);
      __ jcc(Assembler::lessEqual, L_loop);
      __ subptr(qword_count, 4);  // sub(8) and add(4)
      __ jccb(Assembler::greater, L_end);
      // Copy trailing 32 bytes
      if (UseAVX >= 2) {
        __ vmovdqu(xmm0, Address(end_from, qword_count, Address::times_8, -24));
        __ vmovdqu(Address(end_to, qword_count, Address::times_8, -24), xmm0);
      } else {
        __ movdqu(xmm0, Address(end_from, qword_count, Address::times_8, -24));
        __ movdqu(Address(end_to, qword_count, Address::times_8, -24), xmm0);
        __ movdqu(xmm1, Address(end_from, qword_count, Address::times_8, - 8));
        __ movdqu(Address(end_to, qword_count, Address::times_8, - 8), xmm1);
      }
      __ addptr(qword_count, 4);
      __ BIND(L_end);
      if (UseAVX >= 2) {
        // clean upper bits of YMM registers
        __ vpxor(xmm0, xmm0);
        __ vpxor(xmm1, xmm1);
      }
    } else {
      // Copy 32-bytes per iteration
      __ BIND(L_loop);
      __ movq(to, Address(end_from, qword_count, Address::times_8, -24));
      __ movq(Address(end_to, qword_count, Address::times_8, -24), to);
      __ movq(to, Address(end_from, qword_count, Address::times_8, -16));
      __ movq(Address(end_to, qword_count, Address::times_8, -16), to);
      __ movq(to, Address(end_from, qword_count, Address::times_8, - 8));
      __ movq(Address(end_to, qword_count, Address::times_8, - 8), to);
      __ movq(to, Address(end_from, qword_count, Address::times_8, - 0));
      __ movq(Address(end_to, qword_count, Address::times_8, - 0), to);

      __ BIND(L_copy_bytes);
      __ addptr(qword_count, 4);
      __ jcc(Assembler::lessEqual, L_loop);
    }
    __ subptr(qword_count, 4);
    __ jcc(Assembler::less, L_copy_8_bytes); // Copy trailing qwords
  }

  // Copy big chunks backward
  //
  // Inputs:
  //   from         - source arrays address
  //   dest         - destination array address
  //   qword_count  - 64-bits element count
  //   to           - scratch
  //   L_copy_bytes - entry label
  //   L_copy_8_bytes  - exit  label
  //
  void copy_bytes_backward(Register from, Register dest,
                              Register qword_count, Register to,
                              Label& L_copy_bytes, Label& L_copy_8_bytes) {
    DEBUG_ONLY(__ stop("enter at entry label, not here"));
    Label L_loop;
    __ align(OptoLoopAlignment);
    if (UseUnalignedLoadStores) {
      Label L_end;
      __ BIND(L_loop);
      if (UseAVX >= 2) {
        __ vmovdqu(xmm0, Address(from, qword_count, Address::times_8, 32));
        __ vmovdqu(Address(dest, qword_count, Address::times_8, 32), xmm0);
        __ vmovdqu(xmm1, Address(from, qword_count, Address::times_8,  0));
        __ vmovdqu(Address(dest, qword_count, Address::times_8,  0), xmm1);
      } else {
        __ movdqu(xmm0, Address(from, qword_count, Address::times_8, 48));
        __ movdqu(Address(dest, qword_count, Address::times_8, 48), xmm0);
        __ movdqu(xmm1, Address(from, qword_count, Address::times_8, 32));
        __ movdqu(Address(dest, qword_count, Address::times_8, 32), xmm1);
        __ movdqu(xmm2, Address(from, qword_count, Address::times_8, 16));
        __ movdqu(Address(dest, qword_count, Address::times_8, 16), xmm2);
        __ movdqu(xmm3, Address(from, qword_count, Address::times_8,  0));
        __ movdqu(Address(dest, qword_count, Address::times_8,  0), xmm3);
      }

      __ BIND(L_copy_bytes);
      __ subptr(qword_count, 8);
      __ jcc(Assembler::greaterEqual, L_loop);

      __ addptr(qword_count, 4);  // add(8) and sub(4)
      __ jccb(Assembler::less, L_end);
      // Copy trailing 32 bytes
      if (UseAVX >= 2) {
        __ vmovdqu(xmm0, Address(from, qword_count, Address::times_8, 0));
        __ vmovdqu(Address(dest, qword_count, Address::times_8, 0), xmm0);
      } else {
        __ movdqu(xmm0, Address(from, qword_count, Address::times_8, 16));
        __ movdqu(Address(dest, qword_count, Address::times_8, 16), xmm0);
        __ movdqu(xmm1, Address(from, qword_count, Address::times_8,  0));
        __ movdqu(Address(dest, qword_count, Address::times_8,  0), xmm1);
      }
      __ subptr(qword_count, 4);
      __ BIND(L_end);
      if (UseAVX >= 2) {
        // clean upper bits of YMM registers
        __ vpxor(xmm0, xmm0);
        __ vpxor(xmm1, xmm1);
      }
    } else {
      // Copy 32-bytes per iteration
      __ BIND(L_loop);
      __ movq(to, Address(from, qword_count, Address::times_8, 24));
      __ movq(Address(dest, qword_count, Address::times_8, 24), to);
      __ movq(to, Address(from, qword_count, Address::times_8, 16));
      __ movq(Address(dest, qword_count, Address::times_8, 16), to);
      __ movq(to, Address(from, qword_count, Address::times_8,  8));
      __ movq(Address(dest, qword_count, Address::times_8,  8), to);
      __ movq(to, Address(from, qword_count, Address::times_8,  0));
      __ movq(Address(dest, qword_count, Address::times_8,  0), to);

      __ BIND(L_copy_bytes);
      __ subptr(qword_count, 4);
      __ jcc(Assembler::greaterEqual, L_loop);
    }
    __ addptr(qword_count, 4);
    __ jcc(Assembler::greater, L_copy_8_bytes); // Copy trailing qwords
  }

#ifndef PRODUCT
    int& get_profile_ctr(int shift) {
      if ( 0 == shift)
        return SharedRuntime::_jbyte_array_copy_ctr;
      else if(1 == shift)
        return SharedRuntime::_jshort_array_copy_ctr;
      else if(2 == shift)
        return SharedRuntime::_jint_array_copy_ctr;
      else
        return SharedRuntime::_jlong_array_copy_ctr;
    }
#endif

  void setup_argument_regs(BasicType type) {
    if (type == T_BYTE || type == T_SHORT) {
      setup_arg_regs(); // from => rdi, to => rsi, count => rdx
                        // r9 and r10 may be used to save non-volatile registers
    } else {
      setup_arg_regs_using_thread(); // from => rdi, to => rsi, count => rdx
                                     // r9 is used to save r15_thread
    }
  }

  void restore_argument_regs(BasicType type) {
    if (type == T_BYTE || type == T_SHORT) {
      restore_arg_regs();
    } else {
      restore_arg_regs_using_thread();
    }
  }

#if COMPILER2_OR_JVMCI
  // Note: Following rules apply to AVX3 optimized arraycopy stubs:-
  // - If target supports AVX3 features (BW+VL+F) then implementation uses 32 byte vectors (YMMs)
  //   for both special cases (various small block sizes) and aligned copy loop. This is the
  //   default configuration.
  // - If copy length is above AVX3Threshold, then implementation use 64 byte vectors (ZMMs)
  //   for main copy loop (and subsequent tail) since bulk of the cycles will be consumed in it.
  // - If user forces MaxVectorSize=32 then above 4096 bytes its seen that REP MOVs shows a
  //   better performance for disjoint copies. For conjoint/backward copy vector based
  //   copy performs better.
  // - If user sets AVX3Threshold=0, then special cases for small blocks sizes operate over
  //   64 byte vector registers (ZMMs).

  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  //
  // Side Effects:
  //   disjoint_copy_avx3_masked is set to the no-overlap entry point
  //   used by generate_conjoint_[byte/int/short/long]_copy().
  //

  address generate_disjoint_copy_avx3_masked(address* entry, const char *name, int shift,
                                             bool aligned, bool is_oop, bool dest_uninitialized) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    bool use64byteVector = MaxVectorSize > 32 && AVX3Threshold == 0;
    Label L_main_loop, L_main_loop_64bytes, L_tail, L_tail64, L_exit, L_entry;
    Label L_repmovs, L_main_pre_loop, L_main_pre_loop_64bytes, L_pre_main_post_64;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register temp1       = r8;
    const Register temp2       = r11;
    const Register temp3       = rax;
    const Register temp4       = rcx;
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
       // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    BasicType type_vec[] = { T_BYTE,  T_SHORT,  T_INT,   T_LONG};
    BasicType type = is_oop ? T_OBJECT : type_vec[shift];

    setup_argument_regs(type);

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_DISJOINT;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, count);

    {
      // Type(shift)           byte(0), short(1), int(2),   long(3)
      int loop_size[]        = { 192,     96,       48,      24};
      int threshold[]        = { 4096,    2048,     1024,    512};

      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // 'from', 'to' and 'count' are now valid

      // temp1 holds remaining count and temp4 holds running count used to compute
      // next address offset for start of to/from addresses (temp4 * scale).
      __ mov64(temp4, 0);
      __ movq(temp1, count);

      // Zero length check.
      __ BIND(L_tail);
      __ cmpq(temp1, 0);
      __ jcc(Assembler::lessEqual, L_exit);

      // Special cases using 32 byte [masked] vector copy operations.
      __ arraycopy_avx3_special_cases(xmm1, k2, from, to, temp1, shift,
                                      temp4, temp3, use64byteVector, L_entry, L_exit);

      // PRE-MAIN-POST loop for aligned copy.
      __ BIND(L_entry);

      if (AVX3Threshold != 0) {
        __ cmpq(count, threshold[shift]);
        if (MaxVectorSize == 64) {
          // Copy using 64 byte vectors.
          __ jcc(Assembler::greaterEqual, L_pre_main_post_64);
        } else {
          assert(MaxVectorSize < 64, "vector size should be < 64 bytes");
          // REP MOVS offer a faster copy path.
          __ jcc(Assembler::greaterEqual, L_repmovs);
        }
      }

      if (MaxVectorSize < 64  || AVX3Threshold != 0) {
        // Partial copy to make dst address 32 byte aligned.
        __ movq(temp2, to);
        __ andq(temp2, 31);
        __ jcc(Assembler::equal, L_main_pre_loop);

        __ negptr(temp2);
        __ addq(temp2, 32);
        if (shift) {
          __ shrq(temp2, shift);
        }
        __ movq(temp3, temp2);
        __ copy32_masked_avx(to, from, xmm1, k2, temp3, temp4, temp1, shift);
        __ movq(temp4, temp2);
        __ movq(temp1, count);
        __ subq(temp1, temp2);

        __ cmpq(temp1, loop_size[shift]);
        __ jcc(Assembler::less, L_tail);

        __ BIND(L_main_pre_loop);
        __ subq(temp1, loop_size[shift]);

        // Main loop with aligned copy block size of 192 bytes at 32 byte granularity.
        __ align(32);
        __ BIND(L_main_loop);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 0);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 64);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 128);
           __ addptr(temp4, loop_size[shift]);
           __ subq(temp1, loop_size[shift]);
           __ jcc(Assembler::greater, L_main_loop);

        __ addq(temp1, loop_size[shift]);

        // Tail loop.
        __ jmp(L_tail);

        __ BIND(L_repmovs);
          __ movq(temp2, temp1);
          // Swap to(RSI) and from(RDI) addresses to comply with REP MOVs semantics.
          __ movq(temp3, to);
          __ movq(to,  from);
          __ movq(from, temp3);
          // Save to/from for restoration post rep_mov.
          __ movq(temp1, to);
          __ movq(temp3, from);
          if(shift < 3) {
            __ shrq(temp2, 3-shift);     // quad word count
          }
          __ movq(temp4 , temp2);        // move quad ward count into temp4(RCX).
          __ rep_mov();
          __ shlq(temp2, 3);             // convert quad words into byte count.
          if(shift) {
            __ shrq(temp2, shift);       // type specific count.
          }
          // Restore original addresses in to/from.
          __ movq(to, temp3);
          __ movq(from, temp1);
          __ movq(temp4, temp2);
          __ movq(temp1, count);
          __ subq(temp1, temp2);         // tailing part (less than a quad ward size).
          __ jmp(L_tail);
      }

      if (MaxVectorSize > 32) {
        __ BIND(L_pre_main_post_64);
        // Partial copy to make dst address 64 byte aligned.
        __ movq(temp2, to);
        __ andq(temp2, 63);
        __ jcc(Assembler::equal, L_main_pre_loop_64bytes);

        __ negptr(temp2);
        __ addq(temp2, 64);
        if (shift) {
          __ shrq(temp2, shift);
        }
        __ movq(temp3, temp2);
        __ copy64_masked_avx(to, from, xmm1, k2, temp3, temp4, temp1, shift, 0 , true);
        __ movq(temp4, temp2);
        __ movq(temp1, count);
        __ subq(temp1, temp2);

        __ cmpq(temp1, loop_size[shift]);
        __ jcc(Assembler::less, L_tail64);

        __ BIND(L_main_pre_loop_64bytes);
        __ subq(temp1, loop_size[shift]);

        // Main loop with aligned copy block size of 192 bytes at
        // 64 byte copy granularity.
        __ align(32);
        __ BIND(L_main_loop_64bytes);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 0 , true);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 64, true);
           __ copy64_avx(to, from, temp4, xmm1, false, shift, 128, true);
           __ addptr(temp4, loop_size[shift]);
           __ subq(temp1, loop_size[shift]);
           __ jcc(Assembler::greater, L_main_loop_64bytes);

        __ addq(temp1, loop_size[shift]);
        // Zero length check.
        __ jcc(Assembler::lessEqual, L_exit);

        __ BIND(L_tail64);

        // Tail handling using 64 byte [masked] vector copy operations.
        use64byteVector = true;
        __ arraycopy_avx3_special_cases(xmm1, k2, from, to, temp1, shift,
                                        temp4, temp3, use64byteVector, L_entry, L_exit);
      }
      __ BIND(L_exit);
    }

    address ucme_exit_pc = __ pc();
    // When called from generic_arraycopy r11 contains specific values
    // used during arraycopy epilogue, re-initializing r11.
    if (is_oop) {
      __ movq(r11, shift == 3 ? count : to);
    }
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, count);
    restore_argument_regs(type);
    inc_counter_np(get_profile_ctr(shift)); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  //
  address generate_conjoint_copy_avx3_masked(address* entry, const char *name, int shift,
                                             address nooverlap_target, bool aligned, bool is_oop,
                                             bool dest_uninitialized) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    bool use64byteVector = MaxVectorSize > 32 && AVX3Threshold == 0;

    Label L_main_pre_loop, L_main_pre_loop_64bytes, L_pre_main_post_64;
    Label L_main_loop, L_main_loop_64bytes, L_tail, L_tail64, L_exit, L_entry;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register temp1       = r8;
    const Register temp2       = rcx;
    const Register temp3       = r11;
    const Register temp4       = rax;
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
       // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    array_overlap_test(nooverlap_target, (Address::ScaleFactor)(shift));

    BasicType type_vec[] = { T_BYTE,  T_SHORT,  T_INT,   T_LONG};
    BasicType type = is_oop ? T_OBJECT : type_vec[shift];

    setup_argument_regs(type);

    DecoratorSet decorators = IN_HEAP | IS_ARRAY;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, count);
    {
      // Type(shift)       byte(0), short(1), int(2),   long(3)
      int loop_size[]   = { 192,     96,       48,      24};
      int threshold[]   = { 4096,    2048,     1024,    512};

      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // 'from', 'to' and 'count' are now valid

      // temp1 holds remaining count.
      __ movq(temp1, count);

      // Zero length check.
      __ BIND(L_tail);
      __ cmpq(temp1, 0);
      __ jcc(Assembler::lessEqual, L_exit);

      __ mov64(temp2, 0);
      __ movq(temp3, temp1);
      // Special cases using 32 byte [masked] vector copy operations.
      __ arraycopy_avx3_special_cases_conjoint(xmm1, k2, from, to, temp2, temp3, temp1, shift,
                                               temp4, use64byteVector, L_entry, L_exit);

      // PRE-MAIN-POST loop for aligned copy.
      __ BIND(L_entry);

      if (MaxVectorSize > 32 && AVX3Threshold != 0) {
        __ cmpq(temp1, threshold[shift]);
        __ jcc(Assembler::greaterEqual, L_pre_main_post_64);
      }

      if (MaxVectorSize < 64  || AVX3Threshold != 0) {
        // Partial copy to make dst address 32 byte aligned.
        __ leaq(temp2, Address(to, temp1, (Address::ScaleFactor)(shift), 0));
        __ andq(temp2, 31);
        __ jcc(Assembler::equal, L_main_pre_loop);

        if (shift) {
          __ shrq(temp2, shift);
        }
        __ subq(temp1, temp2);
        __ copy32_masked_avx(to, from, xmm1, k2, temp2, temp1, temp3, shift);

        __ cmpq(temp1, loop_size[shift]);
        __ jcc(Assembler::less, L_tail);

        __ BIND(L_main_pre_loop);

        // Main loop with aligned copy block size of 192 bytes at 32 byte granularity.
        __ align(32);
        __ BIND(L_main_loop);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -64);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -128);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -192);
           __ subptr(temp1, loop_size[shift]);
           __ cmpq(temp1, loop_size[shift]);
           __ jcc(Assembler::greater, L_main_loop);

        // Tail loop.
        __ jmp(L_tail);
      }

      if (MaxVectorSize > 32) {
        __ BIND(L_pre_main_post_64);
        // Partial copy to make dst address 64 byte aligned.
        __ leaq(temp2, Address(to, temp1, (Address::ScaleFactor)(shift), 0));
        __ andq(temp2, 63);
        __ jcc(Assembler::equal, L_main_pre_loop_64bytes);

        if (shift) {
          __ shrq(temp2, shift);
        }
        __ subq(temp1, temp2);
        __ copy64_masked_avx(to, from, xmm1, k2, temp2, temp1, temp3, shift, 0 , true);

        __ cmpq(temp1, loop_size[shift]);
        __ jcc(Assembler::less, L_tail64);

        __ BIND(L_main_pre_loop_64bytes);

        // Main loop with aligned copy block size of 192 bytes at
        // 64 byte copy granularity.
        __ align(32);
        __ BIND(L_main_loop_64bytes);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -64 , true);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -128, true);
           __ copy64_avx(to, from, temp1, xmm1, true, shift, -192, true);
           __ subq(temp1, loop_size[shift]);
           __ cmpq(temp1, loop_size[shift]);
           __ jcc(Assembler::greater, L_main_loop_64bytes);

        // Zero length check.
        __ cmpq(temp1, 0);
        __ jcc(Assembler::lessEqual, L_exit);

        __ BIND(L_tail64);

        // Tail handling using 64 byte [masked] vector copy operations.
        use64byteVector = true;
        __ mov64(temp2, 0);
        __ movq(temp3, temp1);
        __ arraycopy_avx3_special_cases_conjoint(xmm1, k2, from, to, temp2, temp3, temp1, shift,
                                                 temp4, use64byteVector, L_entry, L_exit);
      }
      __ BIND(L_exit);
    }
    address ucme_exit_pc = __ pc();
    // When called from generic_arraycopy r11 contains specific values
    // used during arraycopy epilogue, re-initializing r11.
    if(is_oop) {
      __ movq(r11, count);
    }
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, count);
    restore_argument_regs(type);
    inc_counter_np(get_profile_ctr(shift)); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }
#endif // COMPILER2_OR_JVMCI


  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4-, 2-, or 1-byte boundaries,
  // we let the hardware handle it.  The one to eight bytes within words,
  // dwords or qwords that span cache line boundaries will still be loaded
  // and stored atomically.
  //
  // Side Effects:
  //   disjoint_byte_copy_entry is set to the no-overlap entry point
  //   used by generate_conjoint_byte_copy().
  //
  address generate_disjoint_byte_copy(bool aligned, address* entry, const char *name) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_disjoint_copy_avx3_masked(entry, "jbyte_disjoint_arraycopy_avx3", 0,
                                                 aligned, false, false);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_copy_4_bytes, L_copy_2_bytes;
    Label L_copy_byte, L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register byte_count  = rcx;
    const Register qword_count = count;
    const Register end_from    = from; // source array end address
    const Register end_to      = to;   // destination array end address
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
       // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    setup_arg_regs(); // from => rdi, to => rsi, count => rdx
                      // r9 and r10 may be used to save non-volatile registers

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(byte_count, count);
      __ shrptr(count, 3); // count => qword_count

      // Copy from low to high addresses.  Use 'to' as scratch.
      __ lea(end_from, Address(from, qword_count, Address::times_8, -8));
      __ lea(end_to,   Address(to,   qword_count, Address::times_8, -8));
      __ negptr(qword_count); // make the count negative
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(end_from, qword_count, Address::times_8, 8));
      __ movq(Address(end_to, qword_count, Address::times_8, 8), rax);
      __ increment(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);

      // Check for and copy trailing dword
    __ BIND(L_copy_4_bytes);
      __ testl(byte_count, 4);
      __ jccb(Assembler::zero, L_copy_2_bytes);
      __ movl(rax, Address(end_from, 8));
      __ movl(Address(end_to, 8), rax);

      __ addptr(end_from, 4);
      __ addptr(end_to, 4);

      // Check for and copy trailing word
    __ BIND(L_copy_2_bytes);
      __ testl(byte_count, 2);
      __ jccb(Assembler::zero, L_copy_byte);
      __ movw(rax, Address(end_from, 8));
      __ movw(Address(end_to, 8), rax);

      __ addptr(end_from, 2);
      __ addptr(end_to, 2);

      // Check for and copy trailing byte
    __ BIND(L_copy_byte);
      __ testl(byte_count, 1);
      __ jccb(Assembler::zero, L_exit);
      __ movb(rax, Address(end_from, 8));
      __ movb(Address(end_to, 8), rax);
    }
  __ BIND(L_exit);
    address ucme_exit_pc = __ pc();
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jbyte_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      UnsafeCopyMemoryMark ucmm(this, !aligned, false, ucme_exit_pc);
      // Copy in multi-bytes chunks
      copy_bytes_forward(end_from, end_to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
      __ jmp(L_copy_4_bytes);
    }
    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4-, 2-, or 1-byte boundaries,
  // we let the hardware handle it.  The one to eight bytes within words,
  // dwords or qwords that span cache line boundaries will still be loaded
  // and stored atomically.
  //
  address generate_conjoint_byte_copy(bool aligned, address nooverlap_target,
                                      address* entry, const char *name) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_conjoint_copy_avx3_masked(entry, "jbyte_conjoint_arraycopy_avx3", 0,
                                                 nooverlap_target, aligned, false, false);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_copy_4_bytes, L_copy_2_bytes;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register byte_count  = rcx;
    const Register qword_count = count;

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    array_overlap_test(nooverlap_target, Address::times_1);
    setup_arg_regs(); // from => rdi, to => rsi, count => rdx
                      // r9 and r10 may be used to save non-volatile registers

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(byte_count, count);
      __ shrptr(count, 3);   // count => qword_count

      // Copy from high to low addresses.

      // Check for and copy trailing byte
      __ testl(byte_count, 1);
      __ jcc(Assembler::zero, L_copy_2_bytes);
      __ movb(rax, Address(from, byte_count, Address::times_1, -1));
      __ movb(Address(to, byte_count, Address::times_1, -1), rax);
      __ decrement(byte_count); // Adjust for possible trailing word

      // Check for and copy trailing word
    __ BIND(L_copy_2_bytes);
      __ testl(byte_count, 2);
      __ jcc(Assembler::zero, L_copy_4_bytes);
      __ movw(rax, Address(from, byte_count, Address::times_1, -2));
      __ movw(Address(to, byte_count, Address::times_1, -2), rax);

      // Check for and copy trailing dword
    __ BIND(L_copy_4_bytes);
      __ testl(byte_count, 4);
      __ jcc(Assembler::zero, L_copy_bytes);
      __ movl(rax, Address(from, qword_count, Address::times_8));
      __ movl(Address(to, qword_count, Address::times_8), rax);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(from, qword_count, Address::times_8, -8));
      __ movq(Address(to, qword_count, Address::times_8, -8), rax);
      __ decrement(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);
    }
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jbyte_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // Copy in multi-bytes chunks
      copy_bytes_backward(from, to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
    }
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jbyte_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4- or 2-byte boundaries, we
  // let the hardware handle it.  The two or four words within dwords
  // or qwords that span cache line boundaries will still be loaded
  // and stored atomically.
  //
  // Side Effects:
  //   disjoint_short_copy_entry is set to the no-overlap entry point
  //   used by generate_conjoint_short_copy().
  //
  address generate_disjoint_short_copy(bool aligned, address *entry, const char *name) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_disjoint_copy_avx3_masked(entry, "jshort_disjoint_arraycopy_avx3", 1,
                                                 aligned, false, false);
    }
#endif

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_copy_4_bytes,L_copy_2_bytes,L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register word_count  = rcx;
    const Register qword_count = count;
    const Register end_from    = from; // source array end address
    const Register end_to      = to;   // destination array end address
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    setup_arg_regs(); // from => rdi, to => rsi, count => rdx
                      // r9 and r10 may be used to save non-volatile registers

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(word_count, count);
      __ shrptr(count, 2); // count => qword_count

      // Copy from low to high addresses.  Use 'to' as scratch.
      __ lea(end_from, Address(from, qword_count, Address::times_8, -8));
      __ lea(end_to,   Address(to,   qword_count, Address::times_8, -8));
      __ negptr(qword_count);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(end_from, qword_count, Address::times_8, 8));
      __ movq(Address(end_to, qword_count, Address::times_8, 8), rax);
      __ increment(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);

      // Original 'dest' is trashed, so we can't use it as a
      // base register for a possible trailing word copy

      // Check for and copy trailing dword
    __ BIND(L_copy_4_bytes);
      __ testl(word_count, 2);
      __ jccb(Assembler::zero, L_copy_2_bytes);
      __ movl(rax, Address(end_from, 8));
      __ movl(Address(end_to, 8), rax);

      __ addptr(end_from, 4);
      __ addptr(end_to, 4);

      // Check for and copy trailing word
    __ BIND(L_copy_2_bytes);
      __ testl(word_count, 1);
      __ jccb(Assembler::zero, L_exit);
      __ movw(rax, Address(end_from, 8));
      __ movw(Address(end_to, 8), rax);
    }
  __ BIND(L_exit);
    address ucme_exit_pc = __ pc();
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jshort_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      UnsafeCopyMemoryMark ucmm(this, !aligned, false, ucme_exit_pc);
      // Copy in multi-bytes chunks
      copy_bytes_forward(end_from, end_to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
      __ jmp(L_copy_4_bytes);
    }

    return start;
  }

  address generate_fill(BasicType t, bool aligned, const char *name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    BLOCK_COMMENT("Entry:");

    const Register to       = c_rarg0;  // source array address
    const Register value    = c_rarg1;  // value
    const Register count    = c_rarg2;  // elements count

    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ generate_fill(t, aligned, to, value, count, rax, xmm0);

    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4- or 2-byte boundaries, we
  // let the hardware handle it.  The two or four words within dwords
  // or qwords that span cache line boundaries will still be loaded
  // and stored atomically.
  //
  address generate_conjoint_short_copy(bool aligned, address nooverlap_target,
                                       address *entry, const char *name) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_conjoint_copy_avx3_masked(entry, "jshort_conjoint_arraycopy_avx3", 1,
                                                 nooverlap_target, aligned, false, false);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_copy_4_bytes;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register word_count  = rcx;
    const Register qword_count = count;

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    array_overlap_test(nooverlap_target, Address::times_2);
    setup_arg_regs(); // from => rdi, to => rsi, count => rdx
                      // r9 and r10 may be used to save non-volatile registers

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(word_count, count);
      __ shrptr(count, 2); // count => qword_count

      // Copy from high to low addresses.  Use 'to' as scratch.

      // Check for and copy trailing word
      __ testl(word_count, 1);
      __ jccb(Assembler::zero, L_copy_4_bytes);
      __ movw(rax, Address(from, word_count, Address::times_2, -2));
      __ movw(Address(to, word_count, Address::times_2, -2), rax);

     // Check for and copy trailing dword
    __ BIND(L_copy_4_bytes);
      __ testl(word_count, 2);
      __ jcc(Assembler::zero, L_copy_bytes);
      __ movl(rax, Address(from, qword_count, Address::times_8));
      __ movl(Address(to, qword_count, Address::times_8), rax);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(from, qword_count, Address::times_8, -8));
      __ movq(Address(to, qword_count, Address::times_8, -8), rax);
      __ decrement(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);
    }
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jshort_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !aligned, true);
      // Copy in multi-bytes chunks
      copy_bytes_backward(from, to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
    }
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_jshort_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   is_oop  - true => oop array, so generate store check code
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4-byte boundaries, we let
  // the hardware handle it.  The two dwords within qwords that span
  // cache line boundaries will still be loaded and stored atomicly.
  //
  // Side Effects:
  //   disjoint_int_copy_entry is set to the no-overlap entry point
  //   used by generate_conjoint_int_oop_copy().
  //
  address generate_disjoint_int_oop_copy(bool aligned, bool is_oop, address* entry,
                                         const char *name, bool dest_uninitialized = false) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_disjoint_copy_avx3_masked(entry, "jint_disjoint_arraycopy_avx3", 2,
                                                 aligned, is_oop, dest_uninitialized);
    }
#endif

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_copy_4_bytes, L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register dword_count = rcx;
    const Register qword_count = count;
    const Register end_from    = from; // source array end address
    const Register end_to      = to;   // destination array end address
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    setup_arg_regs_using_thread(); // from => rdi, to => rsi, count => rdx
                                   // r9 is used to save r15_thread

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_DISJOINT;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BasicType type = is_oop ? T_OBJECT : T_INT;
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, count);

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(dword_count, count);
      __ shrptr(count, 1); // count => qword_count

      // Copy from low to high addresses.  Use 'to' as scratch.
      __ lea(end_from, Address(from, qword_count, Address::times_8, -8));
      __ lea(end_to,   Address(to,   qword_count, Address::times_8, -8));
      __ negptr(qword_count);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(end_from, qword_count, Address::times_8, 8));
      __ movq(Address(end_to, qword_count, Address::times_8, 8), rax);
      __ increment(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);

      // Check for and copy trailing dword
    __ BIND(L_copy_4_bytes);
      __ testl(dword_count, 1); // Only byte test since the value is 0 or 1
      __ jccb(Assembler::zero, L_exit);
      __ movl(rax, Address(end_from, 8));
      __ movl(Address(end_to, 8), rax);
    }
  __ BIND(L_exit);
    address ucme_exit_pc = __ pc();
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, dword_count);
    restore_arg_regs_using_thread();
    inc_counter_np(SharedRuntime::_jint_array_copy_ctr); // Update counter after rscratch1 is free
    __ vzeroupper();
    __ xorptr(rax, rax); // return 0
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, false, ucme_exit_pc);
      // Copy in multi-bytes chunks
      copy_bytes_forward(end_from, end_to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
      __ jmp(L_copy_4_bytes);
    }

    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord == 8-byte boundary
  //             ignored
  //   is_oop  - true => oop array, so generate store check code
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  // If 'from' and/or 'to' are aligned on 4-byte boundaries, we let
  // the hardware handle it.  The two dwords within qwords that span
  // cache line boundaries will still be loaded and stored atomicly.
  //
  address generate_conjoint_int_oop_copy(bool aligned, bool is_oop, address nooverlap_target,
                                         address *entry, const char *name,
                                         bool dest_uninitialized = false) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_conjoint_copy_avx3_masked(entry, "jint_conjoint_arraycopy_avx3", 2,
                                                 nooverlap_target, aligned, is_oop, dest_uninitialized);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register count       = rdx;  // elements count
    const Register dword_count = rcx;
    const Register qword_count = count;

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
       // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    array_overlap_test(nooverlap_target, Address::times_4);
    setup_arg_regs_using_thread(); // from => rdi, to => rsi, count => rdx
                                   // r9 is used to save r15_thread

    DecoratorSet decorators = IN_HEAP | IS_ARRAY;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BasicType type = is_oop ? T_OBJECT : T_INT;
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    // no registers are destroyed by this call
    bs->arraycopy_prologue(_masm, decorators, type, from, to, count);

    assert_clean_int(count, rax); // Make sure 'count' is clean int.
    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // 'from', 'to' and 'count' are now valid
      __ movptr(dword_count, count);
      __ shrptr(count, 1); // count => qword_count

      // Copy from high to low addresses.  Use 'to' as scratch.

      // Check for and copy trailing dword
      __ testl(dword_count, 1);
      __ jcc(Assembler::zero, L_copy_bytes);
      __ movl(rax, Address(from, dword_count, Address::times_4, -4));
      __ movl(Address(to, dword_count, Address::times_4, -4), rax);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(from, qword_count, Address::times_8, -8));
      __ movq(Address(to, qword_count, Address::times_8, -8), rax);
      __ decrement(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);
    }
    if (is_oop) {
      __ jmp(L_exit);
    }
    restore_arg_regs_using_thread();
    inc_counter_np(SharedRuntime::_jint_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // Copy in multi-bytes chunks
      copy_bytes_backward(from, to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
    }

  __ BIND(L_exit);
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, dword_count);
    restore_arg_regs_using_thread();
    inc_counter_np(SharedRuntime::_jint_array_copy_ctr); // Update counter after rscratch1 is free
    __ xorptr(rax, rax); // return 0
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   is_oop  - true => oop array, so generate store check code
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
 // Side Effects:
  //   disjoint_oop_copy_entry or disjoint_long_copy_entry is set to the
  //   no-overlap entry point used by generate_conjoint_long_oop_copy().
  //
  address generate_disjoint_long_oop_copy(bool aligned, bool is_oop, address *entry,
                                          const char *name, bool dest_uninitialized = false) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_disjoint_copy_avx3_masked(entry, "jlong_disjoint_arraycopy_avx3", 3,
                                                 aligned, is_oop, dest_uninitialized);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register qword_count = rdx;  // elements count
    const Register end_from    = from; // source array end address
    const Register end_to      = rcx;  // destination array end address
    const Register saved_count = r11;
    // End pointers are inclusive, and if count is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    // Save no-overlap entry point for generate_conjoint_long_oop_copy()
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    setup_arg_regs_using_thread(); // from => rdi, to => rsi, count => rdx
                                     // r9 is used to save r15_thread
    // 'from', 'to' and 'qword_count' are now valid

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_DISJOINT;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BasicType type = is_oop ? T_OBJECT : T_LONG;
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, qword_count);
    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);

      // Copy from low to high addresses.  Use 'to' as scratch.
      __ lea(end_from, Address(from, qword_count, Address::times_8, -8));
      __ lea(end_to,   Address(to,   qword_count, Address::times_8, -8));
      __ negptr(qword_count);
      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(end_from, qword_count, Address::times_8, 8));
      __ movq(Address(end_to, qword_count, Address::times_8, 8), rax);
      __ increment(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);
    }
    if (is_oop) {
      __ jmp(L_exit);
    } else {
      restore_arg_regs_using_thread();
      inc_counter_np(SharedRuntime::_jlong_array_copy_ctr); // Update counter after rscratch1 is free
      __ xorptr(rax, rax); // return 0
      __ vzeroupper();
      __ leave(); // required for proper stackwalking of RuntimeStub frame
      __ ret(0);
    }

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);
      // Copy in multi-bytes chunks
      copy_bytes_forward(end_from, end_to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
    }

    __ BIND(L_exit);
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, qword_count);
    restore_arg_regs_using_thread();
    if (is_oop) {
      inc_counter_np(SharedRuntime::_oop_array_copy_ctr); // Update counter after rscratch1 is free
    } else {
      inc_counter_np(SharedRuntime::_jlong_array_copy_ctr); // Update counter after rscratch1 is free
    }
    __ vzeroupper();
    __ xorptr(rax, rax); // return 0
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   is_oop  - true => oop array, so generate store check code
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as ssize_t, can be zero
  //
  address generate_conjoint_long_oop_copy(bool aligned, bool is_oop,
                                          address nooverlap_target, address *entry,
                                          const char *name, bool dest_uninitialized = false) {
#if COMPILER2_OR_JVMCI
    if (VM_Version::supports_avx512vlbw() && VM_Version::supports_bmi2() && MaxVectorSize  >= 32) {
       return generate_conjoint_copy_avx3_masked(entry, "jlong_conjoint_arraycopy_avx3", 3,
                                                 nooverlap_target, aligned, is_oop, dest_uninitialized);
    }
#endif
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Label L_copy_bytes, L_copy_8_bytes, L_exit;
    const Register from        = rdi;  // source array address
    const Register to          = rsi;  // destination array address
    const Register qword_count = rdx;  // elements count
    const Register saved_count = rcx;

    __ enter(); // required for proper stackwalking of RuntimeStub frame
    assert_clean_int(c_rarg2, rax);    // Make sure 'count' is clean int.

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    array_overlap_test(nooverlap_target, Address::times_8);
    setup_arg_regs_using_thread(); // from => rdi, to => rsi, count => rdx
                                   // r9 is used to save r15_thread
    // 'from', 'to' and 'qword_count' are now valid

    DecoratorSet decorators = IN_HEAP | IS_ARRAY;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BasicType type = is_oop ? T_OBJECT : T_LONG;
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, qword_count);
    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);

      __ jmp(L_copy_bytes);

      // Copy trailing qwords
    __ BIND(L_copy_8_bytes);
      __ movq(rax, Address(from, qword_count, Address::times_8, -8));
      __ movq(Address(to, qword_count, Address::times_8, -8), rax);
      __ decrement(qword_count);
      __ jcc(Assembler::notZero, L_copy_8_bytes);
    }
    if (is_oop) {
      __ jmp(L_exit);
    } else {
      restore_arg_regs_using_thread();
      inc_counter_np(SharedRuntime::_jlong_array_copy_ctr); // Update counter after rscratch1 is free
      __ xorptr(rax, rax); // return 0
      __ vzeroupper();
      __ leave(); // required for proper stackwalking of RuntimeStub frame
      __ ret(0);
    }
    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, !is_oop && !aligned, true);

      // Copy in multi-bytes chunks
      copy_bytes_backward(from, to, qword_count, rax, L_copy_bytes, L_copy_8_bytes);
    }
    __ BIND(L_exit);
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, qword_count);
    restore_arg_regs_using_thread();
    if (is_oop) {
      inc_counter_np(SharedRuntime::_oop_array_copy_ctr); // Update counter after rscratch1 is free
    } else {
      inc_counter_np(SharedRuntime::_jlong_array_copy_ctr); // Update counter after rscratch1 is free
    }
    __ vzeroupper();
    __ xorptr(rax, rax); // return 0
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }


  // Helper for generating a dynamic type check.
  // Smashes no registers.
  void generate_type_check(Register sub_klass,
                           Register super_check_offset,
                           Register super_klass,
                           Label& L_success) {
    assert_different_registers(sub_klass, super_check_offset, super_klass);

    BLOCK_COMMENT("type_check:");

    Label L_miss;

    __ check_klass_subtype_fast_path(sub_klass, super_klass, noreg,        &L_success, &L_miss, NULL,
                                     super_check_offset);
    __ check_klass_subtype_slow_path(sub_klass, super_klass, noreg, noreg, &L_success, NULL);

    // Fall through on failure!
    __ BIND(L_miss);
  }

  //
  //  Generate checkcasting array copy stub
  //
  //  Input:
  //    c_rarg0   - source array address
  //    c_rarg1   - destination array address
  //    c_rarg2   - element count, treated as ssize_t, can be zero
  //    c_rarg3   - size_t ckoff (super_check_offset)
  // not Win64
  //    c_rarg4   - oop ckval (super_klass)
  // Win64
  //    rsp+40    - oop ckval (super_klass)
  //
  //  Output:
  //    rax ==  0  -  success
  //    rax == -1^K - failure, where K is partial transfer count
  //
  address generate_checkcast_copy(const char *name, address *entry,
                                  bool dest_uninitialized = false) {

    Label L_load_element, L_store_element, L_do_card_marks, L_done;

    // Input registers (after setup_arg_regs)
    const Register from        = rdi;   // source array address
    const Register to          = rsi;   // destination array address
    const Register length      = rdx;   // elements count
    const Register ckoff       = rcx;   // super_check_offset
    const Register ckval       = r8;    // super_klass

    // Registers used as temps (r13, r14 are save-on-entry)
    const Register end_from    = from;  // source array end address
    const Register end_to      = r13;   // destination array end address
    const Register count       = rdx;   // -(count_remaining)
    const Register r14_length  = r14;   // saved copy of length
    // End pointers are inclusive, and if length is not zero they point
    // to the last unit copied:  end_to[0] := end_from[0]

    const Register rax_oop    = rax;    // actual oop copied
    const Register r11_klass  = r11;    // oop._klass

    //---------------------------------------------------------------
    // Assembler stub will be used for this call to arraycopy
    // if the two arrays are subtypes of Object[] but the
    // destination array type is not equal to or a supertype
    // of the source type.  Each element must be separately
    // checked.

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef ASSERT
    // caller guarantees that the arrays really are different
    // otherwise, we would have to make conjoint checks
    { Label L;
      array_overlap_test(L, TIMES_OOP);
      __ stop("checkcast_copy within a single array");
      __ bind(L);
    }
#endif //ASSERT

    setup_arg_regs(4); // from => rdi, to => rsi, length => rdx
                       // ckoff => rcx, ckval => r8
                       // r9 and r10 may be used to save non-volatile registers
#ifdef _WIN64
    // last argument (#4) is on stack on Win64
    __ movptr(ckval, Address(rsp, 6 * wordSize));
#endif

    // Caller of this entry point must set up the argument registers.
    if (entry != NULL) {
      *entry = __ pc();
      BLOCK_COMMENT("Entry:");
    }

    // allocate spill slots for r13, r14
    enum {
      saved_r13_offset,
      saved_r14_offset,
      saved_r10_offset,
      saved_rbp_offset
    };
    __ subptr(rsp, saved_rbp_offset * wordSize);
    __ movptr(Address(rsp, saved_r13_offset * wordSize), r13);
    __ movptr(Address(rsp, saved_r14_offset * wordSize), r14);
    __ movptr(Address(rsp, saved_r10_offset * wordSize), r10);

#ifdef ASSERT
      Label L2;
      __ get_thread(r14);
      __ cmpptr(r15_thread, r14);
      __ jcc(Assembler::equal, L2);
      __ stop("StubRoutines::call_stub: r15_thread is modified by call");
      __ bind(L2);
#endif // ASSERT

    // check that int operands are properly extended to size_t
    assert_clean_int(length, rax);
    assert_clean_int(ckoff, rax);

#ifdef ASSERT
    BLOCK_COMMENT("assert consistent ckoff/ckval");
    // The ckoff and ckval must be mutually consistent,
    // even though caller generates both.
    { Label L;
      int sco_offset = in_bytes(Klass::super_check_offset_offset());
      __ cmpl(ckoff, Address(ckval, sco_offset));
      __ jcc(Assembler::equal, L);
      __ stop("super_check_offset inconsistent");
      __ bind(L);
    }
#endif //ASSERT

    // Loop-invariant addresses.  They are exclusive end pointers.
    Address end_from_addr(from, length, TIMES_OOP, 0);
    Address   end_to_addr(to,   length, TIMES_OOP, 0);
    // Loop-variant addresses.  They assume post-incremented count < 0.
    Address from_element_addr(end_from, count, TIMES_OOP, 0);
    Address   to_element_addr(end_to,   count, TIMES_OOP, 0);

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_CHECKCAST | ARRAYCOPY_DISJOINT;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }

    BasicType type = T_OBJECT;
    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, type, from, to, count);

    // Copy from low to high addresses, indexed from the end of each array.
    __ lea(end_from, end_from_addr);
    __ lea(end_to,   end_to_addr);
    __ movptr(r14_length, length);        // save a copy of the length
    assert(length == count, "");          // else fix next line:
    __ negptr(count);                     // negate and test the length
    __ jcc(Assembler::notZero, L_load_element);

    // Empty array:  Nothing to do.
    __ xorptr(rax, rax);                  // return 0 on (trivial) success
    __ jmp(L_done);

    // ======== begin loop ========
    // (Loop is rotated; its entry is L_load_element.)
    // Loop control:
    //   for (count = -count; count != 0; count++)
    // Base pointers src, dst are biased by 8*(count-1),to last element.
    __ align(OptoLoopAlignment);

    __ BIND(L_store_element);
    __ store_heap_oop(to_element_addr, rax_oop, noreg, noreg, AS_RAW);  // store the oop
    __ increment(count);               // increment the count toward zero
    __ jcc(Assembler::zero, L_do_card_marks);

    // ======== loop entry is here ========
    __ BIND(L_load_element);
    __ load_heap_oop(rax_oop, from_element_addr, noreg, noreg, AS_RAW); // load the oop
    __ testptr(rax_oop, rax_oop);
    __ jcc(Assembler::zero, L_store_element);

    __ load_klass(r11_klass, rax_oop, rscratch1);// query the object klass
    generate_type_check(r11_klass, ckoff, ckval, L_store_element);
    // ======== end loop ========

    // It was a real error; we must depend on the caller to finish the job.
    // Register rdx = -1 * number of *remaining* oops, r14 = *total* oops.
    // Emit GC store barriers for the oops we have copied (r14 + rdx),
    // and report their number to the caller.
    assert_different_registers(rax, r14_length, count, to, end_to, rcx, rscratch1);
    Label L_post_barrier;
    __ addptr(r14_length, count);     // K = (original - remaining) oops
    __ movptr(rax, r14_length);       // save the value
    __ notptr(rax);                   // report (-1^K) to caller (does not affect flags)
    __ jccb(Assembler::notZero, L_post_barrier);
    __ jmp(L_done); // K == 0, nothing was copied, skip post barrier

    // Come here on success only.
    __ BIND(L_do_card_marks);
    __ xorptr(rax, rax);              // return 0 on success

    __ BIND(L_post_barrier);
    bs->arraycopy_epilogue(_masm, decorators, type, from, to, r14_length);

    // Common exit point (success or failure).
    __ BIND(L_done);
    __ movptr(r13, Address(rsp, saved_r13_offset * wordSize));
    __ movptr(r14, Address(rsp, saved_r14_offset * wordSize));
    __ movptr(r10, Address(rsp, saved_r10_offset * wordSize));
    restore_arg_regs();
    inc_counter_np(SharedRuntime::_checkcast_array_copy_ctr); // Update counter after rscratch1 is free
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  //
  //  Generate 'unsafe' array copy stub
  //  Though just as safe as the other stubs, it takes an unscaled
  //  size_t argument instead of an element count.
  //
  //  Input:
  //    c_rarg0   - source array address
  //    c_rarg1   - destination array address
  //    c_rarg2   - byte count, treated as ssize_t, can be zero
  //
  // Examines the alignment of the operands and dispatches
  // to a long, int, short, or byte copy loop.
  //
  address generate_unsafe_copy(const char *name,
                               address byte_copy_entry, address short_copy_entry,
                               address int_copy_entry, address long_copy_entry) {

    Label L_long_aligned, L_int_aligned, L_short_aligned;

    // Input registers (before setup_arg_regs)
    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register size        = c_rarg2;  // byte count (size_t)

    // Register used as a temp
    const Register bits        = rax;      // test copy of low bits

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    __ enter(); // required for proper stackwalking of RuntimeStub frame

    // bump this on entry, not on exit:
    inc_counter_np(SharedRuntime::_unsafe_array_copy_ctr);

    __ mov(bits, from);
    __ orptr(bits, to);
    __ orptr(bits, size);

    __ testb(bits, BytesPerLong-1);
    __ jccb(Assembler::zero, L_long_aligned);

    __ testb(bits, BytesPerInt-1);
    __ jccb(Assembler::zero, L_int_aligned);

    __ testb(bits, BytesPerShort-1);
    __ jump_cc(Assembler::notZero, RuntimeAddress(byte_copy_entry));

    __ BIND(L_short_aligned);
    __ shrptr(size, LogBytesPerShort); // size => short_count
    __ jump(RuntimeAddress(short_copy_entry));

    __ BIND(L_int_aligned);
    __ shrptr(size, LogBytesPerInt); // size => int_count
    __ jump(RuntimeAddress(int_copy_entry));

    __ BIND(L_long_aligned);
    __ shrptr(size, LogBytesPerLong); // size => qword_count
    __ jump(RuntimeAddress(long_copy_entry));

    return start;
  }

  // Perform range checks on the proposed arraycopy.
  // Kills temp, but nothing else.
  // Also, clean the sign bits of src_pos and dst_pos.
  void arraycopy_range_checks(Register src,     // source array oop (c_rarg0)
                              Register src_pos, // source position (c_rarg1)
                              Register dst,     // destination array oo (c_rarg2)
                              Register dst_pos, // destination position (c_rarg3)
                              Register length,
                              Register temp,
                              Label& L_failed) {
    BLOCK_COMMENT("arraycopy_range_checks:");

    //  if (src_pos + length > arrayOop(src)->length())  FAIL;
    __ movl(temp, length);
    __ addl(temp, src_pos);             // src_pos + length
    __ cmpl(temp, Address(src, arrayOopDesc::length_offset_in_bytes()));
    __ jcc(Assembler::above, L_failed);

    //  if (dst_pos + length > arrayOop(dst)->length())  FAIL;
    __ movl(temp, length);
    __ addl(temp, dst_pos);             // dst_pos + length
    __ cmpl(temp, Address(dst, arrayOopDesc::length_offset_in_bytes()));
    __ jcc(Assembler::above, L_failed);

    // Have to clean up high 32-bits of 'src_pos' and 'dst_pos'.
    // Move with sign extension can be used since they are positive.
    __ movslq(src_pos, src_pos);
    __ movslq(dst_pos, dst_pos);

    BLOCK_COMMENT("arraycopy_range_checks done");
  }

  //
  //  Generate generic array copy stubs
  //
  //  Input:
  //    c_rarg0    -  src oop
  //    c_rarg1    -  src_pos (32-bits)
  //    c_rarg2    -  dst oop
  //    c_rarg3    -  dst_pos (32-bits)
  // not Win64
  //    c_rarg4    -  element count (32-bits)
  // Win64
  //    rsp+40     -  element count (32-bits)
  //
  //  Output:
  //    rax ==  0  -  success
  //    rax == -1^K - failure, where K is partial transfer count
  //
  address generate_generic_copy(const char *name,
                                address byte_copy_entry, address short_copy_entry,
                                address int_copy_entry, address oop_copy_entry,
                                address long_copy_entry, address checkcast_copy_entry) {

    Label L_failed, L_failed_0, L_objArray;
    Label L_copy_shorts, L_copy_ints, L_copy_longs;

    // Input registers
    const Register src        = c_rarg0;  // source array oop
    const Register src_pos    = c_rarg1;  // source position
    const Register dst        = c_rarg2;  // destination array oop
    const Register dst_pos    = c_rarg3;  // destination position
#ifndef _WIN64
    const Register length     = c_rarg4;
    const Register rklass_tmp = r9;  // load_klass
#else
    const Address  length(rsp, 7 * wordSize);  // elements count is on stack on Win64
    const Register rklass_tmp = rdi;  // load_klass
#endif

    { int modulus = CodeEntryAlignment;
      int target  = modulus - 5; // 5 = sizeof jmp(L_failed)
      int advance = target - (__ offset() % modulus);
      if (advance < 0)  advance += modulus;
      if (advance > 0)  __ nop(advance);
    }
    StubCodeMark mark(this, "StubRoutines", name);

    // Short-hop target to L_failed.  Makes for denser prologue code.
    __ BIND(L_failed_0);
    __ jmp(L_failed);
    assert(__ offset() % CodeEntryAlignment == 0, "no further alignment needed");

    __ align(CodeEntryAlignment);
    address start = __ pc();

    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    __ push(rklass_tmp); // rdi is callee-save on Windows
#endif

    // bump this on entry, not on exit:
    inc_counter_np(SharedRuntime::_generic_array_copy_ctr);

    //-----------------------------------------------------------------------
    // Assembler stub will be used for this call to arraycopy
    // if the following conditions are met:
    //
    // (1) src and dst must not be null.
    // (2) src_pos must not be negative.
    // (3) dst_pos must not be negative.
    // (4) length  must not be negative.
    // (5) src klass and dst klass should be the same and not NULL.
    // (6) src and dst should be arrays.
    // (7) src_pos + length must not exceed length of src.
    // (8) dst_pos + length must not exceed length of dst.
    //

    //  if (src == NULL) return -1;
    __ testptr(src, src);         // src oop
    size_t j1off = __ offset();
    __ jccb(Assembler::zero, L_failed_0);

    //  if (src_pos < 0) return -1;
    __ testl(src_pos, src_pos); // src_pos (32-bits)
    __ jccb(Assembler::negative, L_failed_0);

    //  if (dst == NULL) return -1;
    __ testptr(dst, dst);         // dst oop
    __ jccb(Assembler::zero, L_failed_0);

    //  if (dst_pos < 0) return -1;
    __ testl(dst_pos, dst_pos); // dst_pos (32-bits)
    size_t j4off = __ offset();
    __ jccb(Assembler::negative, L_failed_0);

    // The first four tests are very dense code,
    // but not quite dense enough to put four
    // jumps in a 16-byte instruction fetch buffer.
    // That's good, because some branch predicters
    // do not like jumps so close together.
    // Make sure of this.
    guarantee(((j1off ^ j4off) & ~15) != 0, "I$ line of 1st & 4th jumps");

    // registers used as temp
    const Register r11_length    = r11; // elements count to copy
    const Register r10_src_klass = r10; // array klass

    //  if (length < 0) return -1;
    __ movl(r11_length, length);        // length (elements count, 32-bits value)
    __ testl(r11_length, r11_length);
    __ jccb(Assembler::negative, L_failed_0);

    __ load_klass(r10_src_klass, src, rklass_tmp);
#ifdef ASSERT
    //  assert(src->klass() != NULL);
    {
      BLOCK_COMMENT("assert klasses not null {");
      Label L1, L2;
      __ testptr(r10_src_klass, r10_src_klass);
      __ jcc(Assembler::notZero, L2);   // it is broken if klass is NULL
      __ bind(L1);
      __ stop("broken null klass");
      __ bind(L2);
      __ load_klass(rax, dst, rklass_tmp);
      __ cmpq(rax, 0);
      __ jcc(Assembler::equal, L1);     // this would be broken also
      BLOCK_COMMENT("} assert klasses not null done");
    }
#endif

    // Load layout helper (32-bits)
    //
    //  |array_tag|     | header_size | element_type |     |log2_element_size|
    // 32        30    24            16              8     2                 0
    //
    //   array_tag: typeArray = 0x3, objArray = 0x2, non-array = 0x0
    //

    const int lh_offset = in_bytes(Klass::layout_helper_offset());

    // Handle objArrays completely differently...
    const jint objArray_lh = Klass::array_layout_helper(T_OBJECT);
    __ cmpl(Address(r10_src_klass, lh_offset), objArray_lh);
    __ jcc(Assembler::equal, L_objArray);

    //  if (src->klass() != dst->klass()) return -1;
    __ load_klass(rax, dst, rklass_tmp);
    __ cmpq(r10_src_klass, rax);
    __ jcc(Assembler::notEqual, L_failed);

    const Register rax_lh = rax;  // layout helper
    __ movl(rax_lh, Address(r10_src_klass, lh_offset));

    //  if (!src->is_Array()) return -1;
    __ cmpl(rax_lh, Klass::_lh_neutral_value);
    __ jcc(Assembler::greaterEqual, L_failed);

    // At this point, it is known to be a typeArray (array_tag 0x3).
#ifdef ASSERT
    {
      BLOCK_COMMENT("assert primitive array {");
      Label L;
      __ cmpl(rax_lh, (Klass::_lh_array_tag_type_value << Klass::_lh_array_tag_shift));
      __ jcc(Assembler::greaterEqual, L);
      __ stop("must be a primitive array");
      __ bind(L);
      BLOCK_COMMENT("} assert primitive array done");
    }
#endif

    arraycopy_range_checks(src, src_pos, dst, dst_pos, r11_length,
                           r10, L_failed);

    // TypeArrayKlass
    //
    // src_addr = (src + array_header_in_bytes()) + (src_pos << log2elemsize);
    // dst_addr = (dst + array_header_in_bytes()) + (dst_pos << log2elemsize);
    //

    const Register r10_offset = r10;    // array offset
    const Register rax_elsize = rax_lh; // element size

    __ movl(r10_offset, rax_lh);
    __ shrl(r10_offset, Klass::_lh_header_size_shift);
    __ andptr(r10_offset, Klass::_lh_header_size_mask);   // array_offset
    __ addptr(src, r10_offset);           // src array offset
    __ addptr(dst, r10_offset);           // dst array offset
    BLOCK_COMMENT("choose copy loop based on element size");
    __ andl(rax_lh, Klass::_lh_log2_element_size_mask); // rax_lh -> rax_elsize

#ifdef _WIN64
    __ pop(rklass_tmp); // Restore callee-save rdi
#endif

    // next registers should be set before the jump to corresponding stub
    const Register from     = c_rarg0;  // source array address
    const Register to       = c_rarg1;  // destination array address
    const Register count    = c_rarg2;  // elements count

    // 'from', 'to', 'count' registers should be set in such order
    // since they are the same as 'src', 'src_pos', 'dst'.

    __ cmpl(rax_elsize, 0);
    __ jccb(Assembler::notEqual, L_copy_shorts);
    __ lea(from, Address(src, src_pos, Address::times_1, 0));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::times_1, 0));// dst_addr
    __ movl2ptr(count, r11_length); // length
    __ jump(RuntimeAddress(byte_copy_entry));

  __ BIND(L_copy_shorts);
    __ cmpl(rax_elsize, LogBytesPerShort);
    __ jccb(Assembler::notEqual, L_copy_ints);
    __ lea(from, Address(src, src_pos, Address::times_2, 0));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::times_2, 0));// dst_addr
    __ movl2ptr(count, r11_length); // length
    __ jump(RuntimeAddress(short_copy_entry));

  __ BIND(L_copy_ints);
    __ cmpl(rax_elsize, LogBytesPerInt);
    __ jccb(Assembler::notEqual, L_copy_longs);
    __ lea(from, Address(src, src_pos, Address::times_4, 0));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::times_4, 0));// dst_addr
    __ movl2ptr(count, r11_length); // length
    __ jump(RuntimeAddress(int_copy_entry));

  __ BIND(L_copy_longs);
#ifdef ASSERT
    {
      BLOCK_COMMENT("assert long copy {");
      Label L;
      __ cmpl(rax_elsize, LogBytesPerLong);
      __ jcc(Assembler::equal, L);
      __ stop("must be long copy, but elsize is wrong");
      __ bind(L);
      BLOCK_COMMENT("} assert long copy done");
    }
#endif
    __ lea(from, Address(src, src_pos, Address::times_8, 0));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::times_8, 0));// dst_addr
    __ movl2ptr(count, r11_length); // length
    __ jump(RuntimeAddress(long_copy_entry));

    // ObjArrayKlass
  __ BIND(L_objArray);
    // live at this point:  r10_src_klass, r11_length, src[_pos], dst[_pos]

    Label L_plain_copy, L_checkcast_copy;
    //  test array classes for subtyping
    __ load_klass(rax, dst, rklass_tmp);
    __ cmpq(r10_src_klass, rax); // usual case is exact equality
    __ jcc(Assembler::notEqual, L_checkcast_copy);

    // Identically typed arrays can be copied without element-wise checks.
    arraycopy_range_checks(src, src_pos, dst, dst_pos, r11_length,
                           r10, L_failed);

    __ lea(from, Address(src, src_pos, TIMES_OOP,
                 arrayOopDesc::base_offset_in_bytes(T_OBJECT))); // src_addr
    __ lea(to,   Address(dst, dst_pos, TIMES_OOP,
                 arrayOopDesc::base_offset_in_bytes(T_OBJECT))); // dst_addr
    __ movl2ptr(count, r11_length); // length
  __ BIND(L_plain_copy);
#ifdef _WIN64
    __ pop(rklass_tmp); // Restore callee-save rdi
#endif
    __ jump(RuntimeAddress(oop_copy_entry));

  __ BIND(L_checkcast_copy);
    // live at this point:  r10_src_klass, r11_length, rax (dst_klass)
    {
      // Before looking at dst.length, make sure dst is also an objArray.
      __ cmpl(Address(rax, lh_offset), objArray_lh);
      __ jcc(Assembler::notEqual, L_failed);

      // It is safe to examine both src.length and dst.length.
      arraycopy_range_checks(src, src_pos, dst, dst_pos, r11_length,
                             rax, L_failed);

      const Register r11_dst_klass = r11;
      __ load_klass(r11_dst_klass, dst, rklass_tmp); // reload

      // Marshal the base address arguments now, freeing registers.
      __ lea(from, Address(src, src_pos, TIMES_OOP,
                   arrayOopDesc::base_offset_in_bytes(T_OBJECT)));
      __ lea(to,   Address(dst, dst_pos, TIMES_OOP,
                   arrayOopDesc::base_offset_in_bytes(T_OBJECT)));
      __ movl(count, length);           // length (reloaded)
      Register sco_temp = c_rarg3;      // this register is free now
      assert_different_registers(from, to, count, sco_temp,
                                 r11_dst_klass, r10_src_klass);
      assert_clean_int(count, sco_temp);

      // Generate the type check.
      const int sco_offset = in_bytes(Klass::super_check_offset_offset());
      __ movl(sco_temp, Address(r11_dst_klass, sco_offset));
      assert_clean_int(sco_temp, rax);
      generate_type_check(r10_src_klass, sco_temp, r11_dst_klass, L_plain_copy);

      // Fetch destination element klass from the ObjArrayKlass header.
      int ek_offset = in_bytes(ObjArrayKlass::element_klass_offset());
      __ movptr(r11_dst_klass, Address(r11_dst_klass, ek_offset));
      __ movl(  sco_temp,      Address(r11_dst_klass, sco_offset));
      assert_clean_int(sco_temp, rax);

#ifdef _WIN64
      __ pop(rklass_tmp); // Restore callee-save rdi
#endif

      // the checkcast_copy loop needs two extra arguments:
      assert(c_rarg3 == sco_temp, "#3 already in place");
      // Set up arguments for checkcast_copy_entry.
      setup_arg_regs(4);
      __ movptr(r8, r11_dst_klass);  // dst.klass.element_klass, r8 is c_rarg4 on Linux/Solaris
      __ jump(RuntimeAddress(checkcast_copy_entry));
    }

  __ BIND(L_failed);
#ifdef _WIN64
    __ pop(rklass_tmp); // Restore callee-save rdi
#endif
    __ xorptr(rax, rax);
    __ notptr(rax); // return -1
    __ leave();   // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  address generate_data_cache_writeback() {
    const Register src        = c_rarg0;  // source address

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "_data_cache_writeback");

    address start = __ pc();
    __ enter();
    __ cache_wb(Address(src, 0));
    __ leave();
    __ ret(0);

    return start;
  }

  address generate_data_cache_writeback_sync() {
    const Register is_pre    = c_rarg0;  // pre or post sync

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "_data_cache_writeback_sync");

    // pre wbsync is a no-op
    // post wbsync translates to an sfence

    Label skip;
    address start = __ pc();
    __ enter();
    __ cmpl(is_pre, 0);
    __ jcc(Assembler::notEqual, skip);
    __ cache_wbsync(false);
    __ bind(skip);
    __ leave();
    __ ret(0);

    return start;
  }

  void generate_arraycopy_stubs() {
    address entry;
    address entry_jbyte_arraycopy;
    address entry_jshort_arraycopy;
    address entry_jint_arraycopy;
    address entry_oop_arraycopy;
    address entry_jlong_arraycopy;
    address entry_checkcast_arraycopy;

    StubRoutines::_jbyte_disjoint_arraycopy  = generate_disjoint_byte_copy(false, &entry,
                                                                           "jbyte_disjoint_arraycopy");
    StubRoutines::_jbyte_arraycopy           = generate_conjoint_byte_copy(false, entry, &entry_jbyte_arraycopy,
                                                                           "jbyte_arraycopy");

    StubRoutines::_jshort_disjoint_arraycopy = generate_disjoint_short_copy(false, &entry,
                                                                            "jshort_disjoint_arraycopy");
    StubRoutines::_jshort_arraycopy          = generate_conjoint_short_copy(false, entry, &entry_jshort_arraycopy,
                                                                            "jshort_arraycopy");

    StubRoutines::_jint_disjoint_arraycopy   = generate_disjoint_int_oop_copy(false, false, &entry,
                                                                              "jint_disjoint_arraycopy");
    StubRoutines::_jint_arraycopy            = generate_conjoint_int_oop_copy(false, false, entry,
                                                                              &entry_jint_arraycopy, "jint_arraycopy");

    StubRoutines::_jlong_disjoint_arraycopy  = generate_disjoint_long_oop_copy(false, false, &entry,
                                                                               "jlong_disjoint_arraycopy");
    StubRoutines::_jlong_arraycopy           = generate_conjoint_long_oop_copy(false, false, entry,
                                                                               &entry_jlong_arraycopy, "jlong_arraycopy");


    if (UseCompressedOops) {
      StubRoutines::_oop_disjoint_arraycopy  = generate_disjoint_int_oop_copy(false, true, &entry,
                                                                              "oop_disjoint_arraycopy");
      StubRoutines::_oop_arraycopy           = generate_conjoint_int_oop_copy(false, true, entry,
                                                                              &entry_oop_arraycopy, "oop_arraycopy");
      StubRoutines::_oop_disjoint_arraycopy_uninit  = generate_disjoint_int_oop_copy(false, true, &entry,
                                                                                     "oop_disjoint_arraycopy_uninit",
                                                                                     /*dest_uninitialized*/true);
      StubRoutines::_oop_arraycopy_uninit           = generate_conjoint_int_oop_copy(false, true, entry,
                                                                                     NULL, "oop_arraycopy_uninit",
                                                                                     /*dest_uninitialized*/true);
    } else {
      StubRoutines::_oop_disjoint_arraycopy  = generate_disjoint_long_oop_copy(false, true, &entry,
                                                                               "oop_disjoint_arraycopy");
      StubRoutines::_oop_arraycopy           = generate_conjoint_long_oop_copy(false, true, entry,
                                                                               &entry_oop_arraycopy, "oop_arraycopy");
      StubRoutines::_oop_disjoint_arraycopy_uninit  = generate_disjoint_long_oop_copy(false, true, &entry,
                                                                                      "oop_disjoint_arraycopy_uninit",
                                                                                      /*dest_uninitialized*/true);
      StubRoutines::_oop_arraycopy_uninit           = generate_conjoint_long_oop_copy(false, true, entry,
                                                                                      NULL, "oop_arraycopy_uninit",
                                                                                      /*dest_uninitialized*/true);
    }

    StubRoutines::_checkcast_arraycopy        = generate_checkcast_copy("checkcast_arraycopy", &entry_checkcast_arraycopy);
    StubRoutines::_checkcast_arraycopy_uninit = generate_checkcast_copy("checkcast_arraycopy_uninit", NULL,
                                                                        /*dest_uninitialized*/true);

    StubRoutines::_unsafe_arraycopy    = generate_unsafe_copy("unsafe_arraycopy",
                                                              entry_jbyte_arraycopy,
                                                              entry_jshort_arraycopy,
                                                              entry_jint_arraycopy,
                                                              entry_jlong_arraycopy);
    StubRoutines::_generic_arraycopy   = generate_generic_copy("generic_arraycopy",
                                                               entry_jbyte_arraycopy,
                                                               entry_jshort_arraycopy,
                                                               entry_jint_arraycopy,
                                                               entry_oop_arraycopy,
                                                               entry_jlong_arraycopy,
                                                               entry_checkcast_arraycopy);

    StubRoutines::_jbyte_fill = generate_fill(T_BYTE, false, "jbyte_fill");
    StubRoutines::_jshort_fill = generate_fill(T_SHORT, false, "jshort_fill");
    StubRoutines::_jint_fill = generate_fill(T_INT, false, "jint_fill");
    StubRoutines::_arrayof_jbyte_fill = generate_fill(T_BYTE, true, "arrayof_jbyte_fill");
    StubRoutines::_arrayof_jshort_fill = generate_fill(T_SHORT, true, "arrayof_jshort_fill");
    StubRoutines::_arrayof_jint_fill = generate_fill(T_INT, true, "arrayof_jint_fill");

    // We don't generate specialized code for HeapWord-aligned source
    // arrays, so just use the code we've already generated
    StubRoutines::_arrayof_jbyte_disjoint_arraycopy  = StubRoutines::_jbyte_disjoint_arraycopy;
    StubRoutines::_arrayof_jbyte_arraycopy           = StubRoutines::_jbyte_arraycopy;

    StubRoutines::_arrayof_jshort_disjoint_arraycopy = StubRoutines::_jshort_disjoint_arraycopy;
    StubRoutines::_arrayof_jshort_arraycopy          = StubRoutines::_jshort_arraycopy;

    StubRoutines::_arrayof_jint_disjoint_arraycopy   = StubRoutines::_jint_disjoint_arraycopy;
    StubRoutines::_arrayof_jint_arraycopy            = StubRoutines::_jint_arraycopy;

    StubRoutines::_arrayof_jlong_disjoint_arraycopy  = StubRoutines::_jlong_disjoint_arraycopy;
    StubRoutines::_arrayof_jlong_arraycopy           = StubRoutines::_jlong_arraycopy;

    StubRoutines::_arrayof_oop_disjoint_arraycopy    = StubRoutines::_oop_disjoint_arraycopy;
    StubRoutines::_arrayof_oop_arraycopy             = StubRoutines::_oop_arraycopy;

    StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit    = StubRoutines::_oop_disjoint_arraycopy_uninit;
    StubRoutines::_arrayof_oop_arraycopy_uninit             = StubRoutines::_oop_arraycopy_uninit;
  }

  // AES intrinsic stubs
  enum {AESBlockSize = 16};

  address generate_key_shuffle_mask() {
    __ align(16);
    StubCodeMark mark(this, "StubRoutines", "key_shuffle_mask");
    address start = __ pc();
    __ emit_data64( 0x0405060700010203, relocInfo::none );
    __ emit_data64( 0x0c0d0e0f08090a0b, relocInfo::none );
    return start;
  }

  address generate_counter_shuffle_mask() {
    __ align(16);
    StubCodeMark mark(this, "StubRoutines", "counter_shuffle_mask");
    address start = __ pc();
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
    __ emit_data64(0x0001020304050607, relocInfo::none);
    return start;
  }

  // Utility routine for loading a 128-bit key word in little endian format
  // can optionally specify that the shuffle mask is already in an xmmregister
  void load_key(XMMRegister xmmdst, Register key, int offset, XMMRegister xmm_shuf_mask=NULL) {
    __ movdqu(xmmdst, Address(key, offset));
    if (xmm_shuf_mask != NULL) {
      __ pshufb(xmmdst, xmm_shuf_mask);
    } else {
      __ pshufb(xmmdst, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    }
  }

  // Utility routine for increase 128bit counter (iv in CTR mode)
  void inc_counter(Register reg, XMMRegister xmmdst, int inc_delta, Label& next_block) {
    __ pextrq(reg, xmmdst, 0x0);
    __ addq(reg, inc_delta);
    __ pinsrq(xmmdst, reg, 0x0);
    __ jcc(Assembler::carryClear, next_block); // jump if no carry
    __ pextrq(reg, xmmdst, 0x01); // Carry
    __ addq(reg, 0x01);
    __ pinsrq(xmmdst, reg, 0x01); //Carry end
    __ BIND(next_block);          // next instruction
  }

  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //
  address generate_aescrypt_encryptBlock() {
    assert(UseAES, "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "aescrypt_encryptBlock");
    Label L_doLast;
    address start = __ pc();

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register keylen      = rax;

    const XMMRegister xmm_result = xmm0;
    const XMMRegister xmm_key_shuf_mask = xmm1;
    // On win64 xmm6-xmm15 must be preserved so don't use them.
    const XMMRegister xmm_temp1  = xmm2;
    const XMMRegister xmm_temp2  = xmm3;
    const XMMRegister xmm_temp3  = xmm4;
    const XMMRegister xmm_temp4  = xmm5;

    __ enter(); // required for proper stackwalking of RuntimeStub frame

    // keylen could be only {11, 13, 15} * 4 = {44, 52, 60}
    __ movl(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    __ movdqu(xmm_result, Address(from, 0));  // get 16 bytes of input

    // For encryption, the java expanded key ordering is just what we need
    // we don't know if the key is aligned, hence not using load-execute form

    load_key(xmm_temp1, key, 0x00, xmm_key_shuf_mask);
    __ pxor(xmm_result, xmm_temp1);

    load_key(xmm_temp1, key, 0x10, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0x20, xmm_key_shuf_mask);
    load_key(xmm_temp3, key, 0x30, xmm_key_shuf_mask);
    load_key(xmm_temp4, key, 0x40, xmm_key_shuf_mask);

    __ aesenc(xmm_result, xmm_temp1);
    __ aesenc(xmm_result, xmm_temp2);
    __ aesenc(xmm_result, xmm_temp3);
    __ aesenc(xmm_result, xmm_temp4);

    load_key(xmm_temp1, key, 0x50, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0x60, xmm_key_shuf_mask);
    load_key(xmm_temp3, key, 0x70, xmm_key_shuf_mask);
    load_key(xmm_temp4, key, 0x80, xmm_key_shuf_mask);

    __ aesenc(xmm_result, xmm_temp1);
    __ aesenc(xmm_result, xmm_temp2);
    __ aesenc(xmm_result, xmm_temp3);
    __ aesenc(xmm_result, xmm_temp4);

    load_key(xmm_temp1, key, 0x90, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xa0, xmm_key_shuf_mask);

    __ cmpl(keylen, 44);
    __ jccb(Assembler::equal, L_doLast);

    __ aesenc(xmm_result, xmm_temp1);
    __ aesenc(xmm_result, xmm_temp2);

    load_key(xmm_temp1, key, 0xb0, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xc0, xmm_key_shuf_mask);

    __ cmpl(keylen, 52);
    __ jccb(Assembler::equal, L_doLast);

    __ aesenc(xmm_result, xmm_temp1);
    __ aesenc(xmm_result, xmm_temp2);

    load_key(xmm_temp1, key, 0xd0, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xe0, xmm_key_shuf_mask);

    __ BIND(L_doLast);
    __ aesenc(xmm_result, xmm_temp1);
    __ aesenclast(xmm_result, xmm_temp2);
    __ movdqu(Address(to, 0), xmm_result);        // store the result
    __ xorptr(rax, rax); // return 0
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }


  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //
  address generate_aescrypt_decryptBlock() {
    assert(UseAES, "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "aescrypt_decryptBlock");
    Label L_doLast;
    address start = __ pc();

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register keylen      = rax;

    const XMMRegister xmm_result = xmm0;
    const XMMRegister xmm_key_shuf_mask = xmm1;
    // On win64 xmm6-xmm15 must be preserved so don't use them.
    const XMMRegister xmm_temp1  = xmm2;
    const XMMRegister xmm_temp2  = xmm3;
    const XMMRegister xmm_temp3  = xmm4;
    const XMMRegister xmm_temp4  = xmm5;

    __ enter(); // required for proper stackwalking of RuntimeStub frame

    // keylen could be only {11, 13, 15} * 4 = {44, 52, 60}
    __ movl(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    __ movdqu(xmm_result, Address(from, 0));

    // for decryption java expanded key ordering is rotated one position from what we want
    // so we start from 0x10 here and hit 0x00 last
    // we don't know if the key is aligned, hence not using load-execute form
    load_key(xmm_temp1, key, 0x10, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0x20, xmm_key_shuf_mask);
    load_key(xmm_temp3, key, 0x30, xmm_key_shuf_mask);
    load_key(xmm_temp4, key, 0x40, xmm_key_shuf_mask);

    __ pxor  (xmm_result, xmm_temp1);
    __ aesdec(xmm_result, xmm_temp2);
    __ aesdec(xmm_result, xmm_temp3);
    __ aesdec(xmm_result, xmm_temp4);

    load_key(xmm_temp1, key, 0x50, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0x60, xmm_key_shuf_mask);
    load_key(xmm_temp3, key, 0x70, xmm_key_shuf_mask);
    load_key(xmm_temp4, key, 0x80, xmm_key_shuf_mask);

    __ aesdec(xmm_result, xmm_temp1);
    __ aesdec(xmm_result, xmm_temp2);
    __ aesdec(xmm_result, xmm_temp3);
    __ aesdec(xmm_result, xmm_temp4);

    load_key(xmm_temp1, key, 0x90, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xa0, xmm_key_shuf_mask);
    load_key(xmm_temp3, key, 0x00, xmm_key_shuf_mask);

    __ cmpl(keylen, 44);
    __ jccb(Assembler::equal, L_doLast);

    __ aesdec(xmm_result, xmm_temp1);
    __ aesdec(xmm_result, xmm_temp2);

    load_key(xmm_temp1, key, 0xb0, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xc0, xmm_key_shuf_mask);

    __ cmpl(keylen, 52);
    __ jccb(Assembler::equal, L_doLast);

    __ aesdec(xmm_result, xmm_temp1);
    __ aesdec(xmm_result, xmm_temp2);

    load_key(xmm_temp1, key, 0xd0, xmm_key_shuf_mask);
    load_key(xmm_temp2, key, 0xe0, xmm_key_shuf_mask);

    __ BIND(L_doLast);
    __ aesdec(xmm_result, xmm_temp1);
    __ aesdec(xmm_result, xmm_temp2);

    // for decryption the aesdeclast operation is always on key+0x00
    __ aesdeclast(xmm_result, xmm_temp3);
    __ movdqu(Address(to, 0), xmm_result);  // store the result
    __ xorptr(rax, rax); // return 0
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }


  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //   c_rarg3   - r vector byte array address
  //   c_rarg4   - input length
  //
  // Output:
  //   rax       - input length
  //
  address generate_cipherBlockChaining_encryptAESCrypt() {
    assert(UseAES, "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_encryptAESCrypt");
    address start = __ pc();

    Label L_exit, L_key_192_256, L_key_256, L_loopTop_128, L_loopTop_192, L_loopTop_256;
    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register rvec        = c_rarg3;  // r byte array initialized from initvector array address
                                           // and left with the results of the last encryption block
#ifndef _WIN64
    const Register len_reg     = c_rarg4;  // src len (must be multiple of blocksize 16)
#else
    const Address  len_mem(rbp, 6 * wordSize);  // length is on stack on Win64
    const Register len_reg     = r11;      // pick the volatile windows register
#endif
    const Register pos         = rax;

    // xmm register assignments for the loops below
    const XMMRegister xmm_result = xmm0;
    const XMMRegister xmm_temp   = xmm1;
    // keys 0-10 preloaded into xmm2-xmm12
    const int XMM_REG_NUM_KEY_FIRST = 2;
    const int XMM_REG_NUM_KEY_LAST  = 15;
    const XMMRegister xmm_key0   = as_XMMRegister(XMM_REG_NUM_KEY_FIRST);
    const XMMRegister xmm_key10  = as_XMMRegister(XMM_REG_NUM_KEY_FIRST+10);
    const XMMRegister xmm_key11  = as_XMMRegister(XMM_REG_NUM_KEY_FIRST+11);
    const XMMRegister xmm_key12  = as_XMMRegister(XMM_REG_NUM_KEY_FIRST+12);
    const XMMRegister xmm_key13  = as_XMMRegister(XMM_REG_NUM_KEY_FIRST+13);

    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    // on win64, fill len_reg from stack position
    __ movl(len_reg, len_mem);
#else
    __ push(len_reg); // Save
#endif

    const XMMRegister xmm_key_shuf_mask = xmm_temp;  // used temporarily to swap key bytes up front
    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    // load up xmm regs xmm2 thru xmm12 with key 0x00 - 0xa0
    for (int rnum = XMM_REG_NUM_KEY_FIRST, offset = 0x00; rnum <= XMM_REG_NUM_KEY_FIRST+10; rnum++) {
      load_key(as_XMMRegister(rnum), key, offset, xmm_key_shuf_mask);
      offset += 0x10;
    }
    __ movdqu(xmm_result, Address(rvec, 0x00));   // initialize xmm_result with r vec

    // now split to different paths depending on the keylen (len in ints of AESCrypt.KLE array (52=192, or 60=256))
    __ movl(rax, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));
    __ cmpl(rax, 44);
    __ jcc(Assembler::notEqual, L_key_192_256);

    // 128 bit code follows here
    __ movptr(pos, 0);
    __ align(OptoLoopAlignment);

    __ BIND(L_loopTop_128);
    __ movdqu(xmm_temp, Address(from, pos, Address::times_1, 0));   // get next 16 bytes of input
    __ pxor  (xmm_result, xmm_temp);               // xor with the current r vector
    __ pxor  (xmm_result, xmm_key0);               // do the aes rounds
    for (int rnum = XMM_REG_NUM_KEY_FIRST + 1; rnum <= XMM_REG_NUM_KEY_FIRST + 9; rnum++) {
      __ aesenc(xmm_result, as_XMMRegister(rnum));
    }
    __ aesenclast(xmm_result, xmm_key10);
    __ movdqu(Address(to, pos, Address::times_1, 0), xmm_result);     // store into the next 16 bytes of output
    // no need to store r to memory until we exit
    __ addptr(pos, AESBlockSize);
    __ subptr(len_reg, AESBlockSize);
    __ jcc(Assembler::notEqual, L_loopTop_128);

    __ BIND(L_exit);
    __ movdqu(Address(rvec, 0), xmm_result);     // final value of r stored in rvec of CipherBlockChaining object

#ifdef _WIN64
    __ movl(rax, len_mem);
#else
    __ pop(rax); // return length
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    __ BIND(L_key_192_256);
    // here rax = len in ints of AESCrypt.KLE array (52=192, or 60=256)
    load_key(xmm_key11, key, 0xb0, xmm_key_shuf_mask);
    load_key(xmm_key12, key, 0xc0, xmm_key_shuf_mask);
    __ cmpl(rax, 52);
    __ jcc(Assembler::notEqual, L_key_256);

    // 192-bit code follows here (could be changed to use more xmm registers)
    __ movptr(pos, 0);
    __ align(OptoLoopAlignment);

    __ BIND(L_loopTop_192);
    __ movdqu(xmm_temp, Address(from, pos, Address::times_1, 0));   // get next 16 bytes of input
    __ pxor  (xmm_result, xmm_temp);               // xor with the current r vector
    __ pxor  (xmm_result, xmm_key0);               // do the aes rounds
    for (int rnum = XMM_REG_NUM_KEY_FIRST + 1; rnum  <= XMM_REG_NUM_KEY_FIRST + 11; rnum++) {
      __ aesenc(xmm_result, as_XMMRegister(rnum));
    }
    __ aesenclast(xmm_result, xmm_key12);
    __ movdqu(Address(to, pos, Address::times_1, 0), xmm_result);     // store into the next 16 bytes of output
    // no need to store r to memory until we exit
    __ addptr(pos, AESBlockSize);
    __ subptr(len_reg, AESBlockSize);
    __ jcc(Assembler::notEqual, L_loopTop_192);
    __ jmp(L_exit);

    __ BIND(L_key_256);
    // 256-bit code follows here (could be changed to use more xmm registers)
    load_key(xmm_key13, key, 0xd0, xmm_key_shuf_mask);
    __ movptr(pos, 0);
    __ align(OptoLoopAlignment);

    __ BIND(L_loopTop_256);
    __ movdqu(xmm_temp, Address(from, pos, Address::times_1, 0));   // get next 16 bytes of input
    __ pxor  (xmm_result, xmm_temp);               // xor with the current r vector
    __ pxor  (xmm_result, xmm_key0);               // do the aes rounds
    for (int rnum = XMM_REG_NUM_KEY_FIRST + 1; rnum  <= XMM_REG_NUM_KEY_FIRST + 13; rnum++) {
      __ aesenc(xmm_result, as_XMMRegister(rnum));
    }
    load_key(xmm_temp, key, 0xe0);
    __ aesenclast(xmm_result, xmm_temp);
    __ movdqu(Address(to, pos, Address::times_1, 0), xmm_result);     // store into the next 16 bytes of output
    // no need to store r to memory until we exit
    __ addptr(pos, AESBlockSize);
    __ subptr(len_reg, AESBlockSize);
    __ jcc(Assembler::notEqual, L_loopTop_256);
    __ jmp(L_exit);

    return start;
  }

  // Safefetch stubs.
  void generate_safefetch(const char* name, int size, address* entry,
                          address* fault_pc, address* continuation_pc) {
    // safefetch signatures:
    //   int      SafeFetch32(int*      adr, int      errValue);
    //   intptr_t SafeFetchN (intptr_t* adr, intptr_t errValue);
    //
    // arguments:
    //   c_rarg0 = adr
    //   c_rarg1 = errValue
    //
    // result:
    //   PPC_RET  = *adr or errValue

    StubCodeMark mark(this, "StubRoutines", name);

    // Entry point, pc or function descriptor.
    *entry = __ pc();

    // Load *adr into c_rarg1, may fault.
    *fault_pc = __ pc();
    switch (size) {
      case 4:
        // int32_t
        __ movl(c_rarg1, Address(c_rarg0, 0));
        break;
      case 8:
        // int64_t
        __ movq(c_rarg1, Address(c_rarg0, 0));
        break;
      default:
        ShouldNotReachHere();
    }

    // return errValue or *adr
    *continuation_pc = __ pc();
    __ movq(rax, c_rarg1);
    __ ret(0);
  }

  // This is a version of CBC/AES Decrypt which does 4 blocks in a loop at a time
  // to hide instruction latency
  //
  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //   c_rarg3   - r vector byte array address
  //   c_rarg4   - input length
  //
  // Output:
  //   rax       - input length
  //
  address generate_cipherBlockChaining_decryptAESCrypt_Parallel() {
    assert(UseAES, "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_decryptAESCrypt");
    address start = __ pc();

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register rvec        = c_rarg3;  // r byte array initialized from initvector array address
                                           // and left with the results of the last encryption block
#ifndef _WIN64
    const Register len_reg     = c_rarg4;  // src len (must be multiple of blocksize 16)
#else
    const Address  len_mem(rbp, 6 * wordSize);  // length is on stack on Win64
    const Register len_reg     = r11;      // pick the volatile windows register
#endif
    const Register pos         = rax;

    const int PARALLEL_FACTOR = 4;
    const int ROUNDS[3] = { 10, 12, 14 }; // aes rounds for key128, key192, key256

    Label L_exit;
    Label L_singleBlock_loopTopHead[3]; // 128, 192, 256
    Label L_singleBlock_loopTopHead2[3]; // 128, 192, 256
    Label L_singleBlock_loopTop[3]; // 128, 192, 256
    Label L_multiBlock_loopTopHead[3]; // 128, 192, 256
    Label L_multiBlock_loopTop[3]; // 128, 192, 256

    // keys 0-10 preloaded into xmm5-xmm15
    const int XMM_REG_NUM_KEY_FIRST = 5;
    const int XMM_REG_NUM_KEY_LAST  = 15;
    const XMMRegister xmm_key_first = as_XMMRegister(XMM_REG_NUM_KEY_FIRST);
    const XMMRegister xmm_key_last  = as_XMMRegister(XMM_REG_NUM_KEY_LAST);

    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    // on win64, fill len_reg from stack position
    __ movl(len_reg, len_mem);
#else
    __ push(len_reg); // Save
#endif
    __ push(rbx);
    // the java expanded key ordering is rotated one position from what we want
    // so we start from 0x10 here and hit 0x00 last
    const XMMRegister xmm_key_shuf_mask = xmm1;  // used temporarily to swap key bytes up front
    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    // load up xmm regs 5 thru 15 with key 0x10 - 0xa0 - 0x00
    for (int rnum = XMM_REG_NUM_KEY_FIRST, offset = 0x10; rnum < XMM_REG_NUM_KEY_LAST; rnum++) {
      load_key(as_XMMRegister(rnum), key, offset, xmm_key_shuf_mask);
      offset += 0x10;
    }
    load_key(xmm_key_last, key, 0x00, xmm_key_shuf_mask);

    const XMMRegister xmm_prev_block_cipher = xmm1;  // holds cipher of previous block

    // registers holding the four results in the parallelized loop
    const XMMRegister xmm_result0 = xmm0;
    const XMMRegister xmm_result1 = xmm2;
    const XMMRegister xmm_result2 = xmm3;
    const XMMRegister xmm_result3 = xmm4;

    __ movdqu(xmm_prev_block_cipher, Address(rvec, 0x00));   // initialize with initial rvec

    __ xorptr(pos, pos);

    // now split to different paths depending on the keylen (len in ints of AESCrypt.KLE array (52=192, or 60=256))
    __ movl(rbx, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));
    __ cmpl(rbx, 52);
    __ jcc(Assembler::equal, L_multiBlock_loopTopHead[1]);
    __ cmpl(rbx, 60);
    __ jcc(Assembler::equal, L_multiBlock_loopTopHead[2]);

#define DoFour(opc, src_reg)           \
  __ opc(xmm_result0, src_reg);         \
  __ opc(xmm_result1, src_reg);         \
  __ opc(xmm_result2, src_reg);         \
  __ opc(xmm_result3, src_reg);         \

    for (int k = 0; k < 3; ++k) {
      __ BIND(L_multiBlock_loopTopHead[k]);
      if (k != 0) {
        __ cmpptr(len_reg, PARALLEL_FACTOR * AESBlockSize); // see if at least 4 blocks left
        __ jcc(Assembler::less, L_singleBlock_loopTopHead2[k]);
      }
      if (k == 1) {
        __ subptr(rsp, 6 * wordSize);
        __ movdqu(Address(rsp, 0), xmm15); //save last_key from xmm15
        load_key(xmm15, key, 0xb0); // 0xb0; 192-bit key goes up to 0xc0
        __ movdqu(Address(rsp, 2 * wordSize), xmm15);
        load_key(xmm1, key, 0xc0);  // 0xc0;
        __ movdqu(Address(rsp, 4 * wordSize), xmm1);
      } else if (k == 2) {
        __ subptr(rsp, 10 * wordSize);
        __ movdqu(Address(rsp, 0), xmm15); //save last_key from xmm15
        load_key(xmm15, key, 0xd0); // 0xd0; 256-bit key goes upto 0xe0
        __ movdqu(Address(rsp, 6 * wordSize), xmm15);
        load_key(xmm1, key, 0xe0);  // 0xe0;
        __ movdqu(Address(rsp, 8 * wordSize), xmm1);
        load_key(xmm15, key, 0xb0); // 0xb0;
        __ movdqu(Address(rsp, 2 * wordSize), xmm15);
        load_key(xmm1, key, 0xc0);  // 0xc0;
        __ movdqu(Address(rsp, 4 * wordSize), xmm1);
      }
      __ align(OptoLoopAlignment);
      __ BIND(L_multiBlock_loopTop[k]);
      __ cmpptr(len_reg, PARALLEL_FACTOR * AESBlockSize); // see if at least 4 blocks left
      __ jcc(Assembler::less, L_singleBlock_loopTopHead[k]);

      if  (k != 0) {
        __ movdqu(xmm15, Address(rsp, 2 * wordSize));
        __ movdqu(xmm1, Address(rsp, 4 * wordSize));
      }

      __ movdqu(xmm_result0, Address(from, pos, Address::times_1, 0 * AESBlockSize)); // get next 4 blocks into xmmresult registers
      __ movdqu(xmm_result1, Address(from, pos, Address::times_1, 1 * AESBlockSize));
      __ movdqu(xmm_result2, Address(from, pos, Address::times_1, 2 * AESBlockSize));
      __ movdqu(xmm_result3, Address(from, pos, Address::times_1, 3 * AESBlockSize));

      DoFour(pxor, xmm_key_first);
      if (k == 0) {
        for (int rnum = 1; rnum < ROUNDS[k]; rnum++) {
          DoFour(aesdec, as_XMMRegister(rnum + XMM_REG_NUM_KEY_FIRST));
        }
        DoFour(aesdeclast, xmm_key_last);
      } else if (k == 1) {
        for (int rnum = 1; rnum <= ROUNDS[k]-2; rnum++) {
          DoFour(aesdec, as_XMMRegister(rnum + XMM_REG_NUM_KEY_FIRST));
        }
        __ movdqu(xmm_key_last, Address(rsp, 0)); // xmm15 needs to be loaded again.
        DoFour(aesdec, xmm1);  // key : 0xc0
        __ movdqu(xmm_prev_block_cipher, Address(rvec, 0x00));  // xmm1 needs to be loaded again
        DoFour(aesdeclast, xmm_key_last);
      } else if (k == 2) {
        for (int rnum = 1; rnum <= ROUNDS[k] - 4; rnum++) {
          DoFour(aesdec, as_XMMRegister(rnum + XMM_REG_NUM_KEY_FIRST));
        }
        DoFour(aesdec, xmm1);  // key : 0xc0
        __ movdqu(xmm15, Address(rsp, 6 * wordSize));
        __ movdqu(xmm1, Address(rsp, 8 * wordSize));
        DoFour(aesdec, xmm15);  // key : 0xd0
        __ movdqu(xmm_key_last, Address(rsp, 0)); // xmm15 needs to be loaded again.
        DoFour(aesdec, xmm1);  // key : 0xe0
        __ movdqu(xmm_prev_block_cipher, Address(rvec, 0x00));  // xmm1 needs to be loaded again
        DoFour(aesdeclast, xmm_key_last);
      }

      // for each result, xor with the r vector of previous cipher block
      __ pxor(xmm_result0, xmm_prev_block_cipher);
      __ movdqu(xmm_prev_block_cipher, Address(from, pos, Address::times_1, 0 * AESBlockSize));
      __ pxor(xmm_result1, xmm_prev_block_cipher);
      __ movdqu(xmm_prev_block_cipher, Address(from, pos, Address::times_1, 1 * AESBlockSize));
      __ pxor(xmm_result2, xmm_prev_block_cipher);
      __ movdqu(xmm_prev_block_cipher, Address(from, pos, Address::times_1, 2 * AESBlockSize));
      __ pxor(xmm_result3, xmm_prev_block_cipher);
      __ movdqu(xmm_prev_block_cipher, Address(from, pos, Address::times_1, 3 * AESBlockSize));   // this will carry over to next set of blocks
      if (k != 0) {
        __ movdqu(Address(rvec, 0x00), xmm_prev_block_cipher);
      }

      __ movdqu(Address(to, pos, Address::times_1, 0 * AESBlockSize), xmm_result0);     // store 4 results into the next 64 bytes of output
      __ movdqu(Address(to, pos, Address::times_1, 1 * AESBlockSize), xmm_result1);
      __ movdqu(Address(to, pos, Address::times_1, 2 * AESBlockSize), xmm_result2);
      __ movdqu(Address(to, pos, Address::times_1, 3 * AESBlockSize), xmm_result3);

      __ addptr(pos, PARALLEL_FACTOR * AESBlockSize);
      __ subptr(len_reg, PARALLEL_FACTOR * AESBlockSize);
      __ jmp(L_multiBlock_loopTop[k]);

      // registers used in the non-parallelized loops
      // xmm register assignments for the loops below
      const XMMRegister xmm_result = xmm0;
      const XMMRegister xmm_prev_block_cipher_save = xmm2;
      const XMMRegister xmm_key11 = xmm3;
      const XMMRegister xmm_key12 = xmm4;
      const XMMRegister key_tmp = xmm4;

      __ BIND(L_singleBlock_loopTopHead[k]);
      if (k == 1) {
        __ addptr(rsp, 6 * wordSize);
      } else if (k == 2) {
        __ addptr(rsp, 10 * wordSize);
      }
      __ cmpptr(len_reg, 0); // any blocks left??
      __ jcc(Assembler::equal, L_exit);
      __ BIND(L_singleBlock_loopTopHead2[k]);
      if (k == 1) {
        load_key(xmm_key11, key, 0xb0); // 0xb0; 192-bit key goes upto 0xc0
        load_key(xmm_key12, key, 0xc0); // 0xc0; 192-bit key goes upto 0xc0
      }
      if (k == 2) {
        load_key(xmm_key11, key, 0xb0); // 0xb0; 256-bit key goes upto 0xe0
      }
      __ align(OptoLoopAlignment);
      __ BIND(L_singleBlock_loopTop[k]);
      __ movdqu(xmm_result, Address(from, pos, Address::times_1, 0)); // get next 16 bytes of cipher input
      __ movdqa(xmm_prev_block_cipher_save, xmm_result); // save for next r vector
      __ pxor(xmm_result, xmm_key_first); // do the aes dec rounds
      for (int rnum = 1; rnum <= 9 ; rnum++) {
          __ aesdec(xmm_result, as_XMMRegister(rnum + XMM_REG_NUM_KEY_FIRST));
      }
      if (k == 1) {
        __ aesdec(xmm_result, xmm_key11);
        __ aesdec(xmm_result, xmm_key12);
      }
      if (k == 2) {
        __ aesdec(xmm_result, xmm_key11);
        load_key(key_tmp, key, 0xc0);
        __ aesdec(xmm_result, key_tmp);
        load_key(key_tmp, key, 0xd0);
        __ aesdec(xmm_result, key_tmp);
        load_key(key_tmp, key, 0xe0);
        __ aesdec(xmm_result, key_tmp);
      }

      __ aesdeclast(xmm_result, xmm_key_last); // xmm15 always came from key+0
      __ pxor(xmm_result, xmm_prev_block_cipher); // xor with the current r vector
      __ movdqu(Address(to, pos, Address::times_1, 0), xmm_result); // store into the next 16 bytes of output
      // no need to store r to memory until we exit
      __ movdqa(xmm_prev_block_cipher, xmm_prev_block_cipher_save); // set up next r vector with cipher input from this block
      __ addptr(pos, AESBlockSize);
      __ subptr(len_reg, AESBlockSize);
      __ jcc(Assembler::notEqual, L_singleBlock_loopTop[k]);
      if (k != 2) {
        __ jmp(L_exit);
      }
    } //for 128/192/256

    __ BIND(L_exit);
    __ movdqu(Address(rvec, 0), xmm_prev_block_cipher);     // final value of r stored in rvec of CipherBlockChaining object
    __ pop(rbx);
#ifdef _WIN64
    __ movl(rax, len_mem);
#else
    __ pop(rax); // return length
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
}

  address generate_electronicCodeBook_encryptAESCrypt() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "electronicCodeBook_encryptAESCrypt");
    address start = __ pc();
    const Register from = c_rarg0;  // source array address
    const Register to = c_rarg1;  // destination array address
    const Register key = c_rarg2;  // key array address
    const Register len = c_rarg3;  // src len (must be multiple of blocksize 16)
    __ enter(); // required for proper stackwalking of RuntimeStub frame
    __ aesecb_encrypt(from, to, key, len);
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
 }

  address generate_electronicCodeBook_decryptAESCrypt() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "electronicCodeBook_decryptAESCrypt");
    address start = __ pc();
    const Register from = c_rarg0;  // source array address
    const Register to = c_rarg1;  // destination array address
    const Register key = c_rarg2;  // key array address
    const Register len = c_rarg3;  // src len (must be multiple of blocksize 16)
    __ enter(); // required for proper stackwalking of RuntimeStub frame
    __ aesecb_decrypt(from, to, key, len);
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

  // ofs and limit are use for multi-block byte array.
  // int com.sun.security.provider.MD5.implCompress(byte[] b, int ofs)
  address generate_md5_implCompress(bool multi_block, const char *name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    const Register buf_param = r15;
    const Address state_param(rsp, 0 * wordSize);
    const Address ofs_param  (rsp, 1 * wordSize    );
    const Address limit_param(rsp, 1 * wordSize + 4);

    __ enter();
    __ push(rbx);
    __ push(rdi);
    __ push(rsi);
    __ push(r15);
    __ subptr(rsp, 2 * wordSize);

    __ movptr(buf_param, c_rarg0);
    __ movptr(state_param, c_rarg1);
    if (multi_block) {
      __ movl(ofs_param, c_rarg2);
      __ movl(limit_param, c_rarg3);
    }
    __ fast_md5(buf_param, state_param, ofs_param, limit_param, multi_block);

    __ addptr(rsp, 2 * wordSize);
    __ pop(r15);
    __ pop(rsi);
    __ pop(rdi);
    __ pop(rbx);
    __ leave();
    __ ret(0);
    return start;
  }

  address generate_upper_word_mask() {
    __ align(64);
    StubCodeMark mark(this, "StubRoutines", "upper_word_mask");
    address start = __ pc();
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0xFFFFFFFF00000000, relocInfo::none);
    return start;
  }

  address generate_shuffle_byte_flip_mask() {
    __ align(64);
    StubCodeMark mark(this, "StubRoutines", "shuffle_byte_flip_mask");
    address start = __ pc();
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
    __ emit_data64(0x0001020304050607, relocInfo::none);
    return start;
  }

  // ofs and limit are use for multi-block byte array.
  // int com.sun.security.provider.DigestBase.implCompressMultiBlock(byte[] b, int ofs, int limit)
  address generate_sha1_implCompress(bool multi_block, const char *name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf = c_rarg0;
    Register state = c_rarg1;
    Register ofs = c_rarg2;
    Register limit = c_rarg3;

    const XMMRegister abcd = xmm0;
    const XMMRegister e0 = xmm1;
    const XMMRegister e1 = xmm2;
    const XMMRegister msg0 = xmm3;

    const XMMRegister msg1 = xmm4;
    const XMMRegister msg2 = xmm5;
    const XMMRegister msg3 = xmm6;
    const XMMRegister shuf_mask = xmm7;

    __ enter();

    __ subptr(rsp, 4 * wordSize);

    __ fast_sha1(abcd, e0, e1, msg0, msg1, msg2, msg3, shuf_mask,
      buf, state, ofs, limit, rsp, multi_block);

    __ addptr(rsp, 4 * wordSize);

    __ leave();
    __ ret(0);
    return start;
  }

  address generate_pshuffle_byte_flip_mask() {
    __ align(64);
    StubCodeMark mark(this, "StubRoutines", "pshuffle_byte_flip_mask");
    address start = __ pc();
    __ emit_data64(0x0405060700010203, relocInfo::none);
    __ emit_data64(0x0c0d0e0f08090a0b, relocInfo::none);

    if (VM_Version::supports_avx2()) {
      __ emit_data64(0x0405060700010203, relocInfo::none); // second copy
      __ emit_data64(0x0c0d0e0f08090a0b, relocInfo::none);
      // _SHUF_00BA
      __ emit_data64(0x0b0a090803020100, relocInfo::none);
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
      __ emit_data64(0x0b0a090803020100, relocInfo::none);
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
      // _SHUF_DC00
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
      __ emit_data64(0x0b0a090803020100, relocInfo::none);
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
      __ emit_data64(0x0b0a090803020100, relocInfo::none);
    }

    return start;
  }

  //Mask for byte-swapping a couple of qwords in an XMM register using (v)pshufb.
  address generate_pshuffle_byte_flip_mask_sha512() {
    __ align(32);
    StubCodeMark mark(this, "StubRoutines", "pshuffle_byte_flip_mask_sha512");
    address start = __ pc();
    if (VM_Version::supports_avx2()) {
      __ emit_data64(0x0001020304050607, relocInfo::none); // PSHUFFLE_BYTE_FLIP_MASK
      __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
      __ emit_data64(0x1011121314151617, relocInfo::none);
      __ emit_data64(0x18191a1b1c1d1e1f, relocInfo::none);
      __ emit_data64(0x0000000000000000, relocInfo::none); //MASK_YMM_LO
      __ emit_data64(0x0000000000000000, relocInfo::none);
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
      __ emit_data64(0xFFFFFFFFFFFFFFFF, relocInfo::none);
    }

    return start;
  }

// ofs and limit are use for multi-block byte array.
// int com.sun.security.provider.DigestBase.implCompressMultiBlock(byte[] b, int ofs, int limit)
  address generate_sha256_implCompress(bool multi_block, const char *name) {
    assert(VM_Version::supports_sha() || VM_Version::supports_avx2(), "");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf = c_rarg0;
    Register state = c_rarg1;
    Register ofs = c_rarg2;
    Register limit = c_rarg3;

    const XMMRegister msg = xmm0;
    const XMMRegister state0 = xmm1;
    const XMMRegister state1 = xmm2;
    const XMMRegister msgtmp0 = xmm3;

    const XMMRegister msgtmp1 = xmm4;
    const XMMRegister msgtmp2 = xmm5;
    const XMMRegister msgtmp3 = xmm6;
    const XMMRegister msgtmp4 = xmm7;

    const XMMRegister shuf_mask = xmm8;

    __ enter();

    __ subptr(rsp, 4 * wordSize);

    if (VM_Version::supports_sha()) {
      __ fast_sha256(msg, state0, state1, msgtmp0, msgtmp1, msgtmp2, msgtmp3, msgtmp4,
        buf, state, ofs, limit, rsp, multi_block, shuf_mask);
    } else if (VM_Version::supports_avx2()) {
      __ sha256_AVX2(msg, state0, state1, msgtmp0, msgtmp1, msgtmp2, msgtmp3, msgtmp4,
        buf, state, ofs, limit, rsp, multi_block, shuf_mask);
    }
    __ addptr(rsp, 4 * wordSize);
    __ vzeroupper();
    __ leave();
    __ ret(0);
    return start;
  }

  address generate_sha512_implCompress(bool multi_block, const char *name) {
    assert(VM_Version::supports_avx2(), "");
    assert(VM_Version::supports_bmi2(), "");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf = c_rarg0;
    Register state = c_rarg1;
    Register ofs = c_rarg2;
    Register limit = c_rarg3;

    const XMMRegister msg = xmm0;
    const XMMRegister state0 = xmm1;
    const XMMRegister state1 = xmm2;
    const XMMRegister msgtmp0 = xmm3;
    const XMMRegister msgtmp1 = xmm4;
    const XMMRegister msgtmp2 = xmm5;
    const XMMRegister msgtmp3 = xmm6;
    const XMMRegister msgtmp4 = xmm7;

    const XMMRegister shuf_mask = xmm8;

    __ enter();

    __ sha512_AVX2(msg, state0, state1, msgtmp0, msgtmp1, msgtmp2, msgtmp3, msgtmp4,
    buf, state, ofs, limit, rsp, multi_block, shuf_mask);

    __ vzeroupper();
    __ leave();
    __ ret(0);
    return start;
  }

  // This mask is used for incrementing counter value(linc0, linc4, etc.)
  address counter_mask_addr() {
    __ align(64);
    StubCodeMark mark(this, "StubRoutines", "counter_mask_addr");
    address start = __ pc();
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);//lbswapmask
    __ emit_data64(0x0001020304050607, relocInfo::none);
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
    __ emit_data64(0x0001020304050607, relocInfo::none);
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
    __ emit_data64(0x0001020304050607, relocInfo::none);
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none);
    __ emit_data64(0x0001020304050607, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);//linc0 = counter_mask_addr+64
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000001, relocInfo::none);//counter_mask_addr() + 80
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000002, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000003, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000004, relocInfo::none);//linc4 = counter_mask_addr() + 128
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000004, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000004, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000004, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000008, relocInfo::none);//linc8 = counter_mask_addr() + 192
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000008, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000008, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000008, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000020, relocInfo::none);//linc32 = counter_mask_addr() + 256
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000020, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000020, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000020, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000010, relocInfo::none);//linc16 = counter_mask_addr() + 320
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000010, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000010, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000010, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    return start;
  }

 // Vector AES Counter implementation
  address generate_counterMode_VectorAESCrypt()  {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "counterMode_AESCrypt");
    address start = __ pc();
    const Register from = c_rarg0; // source array address
    const Register to = c_rarg1; // destination array address
    const Register key = c_rarg2; // key array address r8
    const Register counter = c_rarg3; // counter byte array initialized from counter array address
    // and updated with the incremented counter in the end
#ifndef _WIN64
    const Register len_reg = c_rarg4;
    const Register saved_encCounter_start = c_rarg5;
    const Register used_addr = r10;
    const Address  used_mem(rbp, 2 * wordSize);
    const Register used = r11;
#else
    const Address len_mem(rbp, 6 * wordSize); // length is on stack on Win64
    const Address saved_encCounter_mem(rbp, 7 * wordSize); // saved encrypted counter is on stack on Win64
    const Address used_mem(rbp, 8 * wordSize); // used length is on stack on Win64
    const Register len_reg = r10; // pick the first volatile windows register
    const Register saved_encCounter_start = r11;
    const Register used_addr = r13;
    const Register used = r14;
#endif
    __ enter();
   // Save state before entering routine
    __ push(r12);
    __ push(r13);
    __ push(r14);
    __ push(r15);
#ifdef _WIN64
    // on win64, fill len_reg from stack position
    __ movl(len_reg, len_mem);
    __ movptr(saved_encCounter_start, saved_encCounter_mem);
    __ movptr(used_addr, used_mem);
    __ movl(used, Address(used_addr, 0));
#else
    __ push(len_reg); // Save
    __ movptr(used_addr, used_mem);
    __ movl(used, Address(used_addr, 0));
#endif
    __ push(rbx);
    __ aesctr_encrypt(from, to, key, counter, len_reg, used, used_addr, saved_encCounter_start);
    // Restore state before leaving routine
    __ pop(rbx);
#ifdef _WIN64
    __ movl(rax, len_mem); // return length
#else
    __ pop(rax); // return length
#endif
    __ pop(r15);
    __ pop(r14);
    __ pop(r13);
    __ pop(r12);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

  // This is a version of CTR/AES crypt which does 6 blocks in a loop at a time
  // to hide instruction latency
  //
  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //   c_rarg3   - counter vector byte array address
  //   Linux
  //     c_rarg4   -          input length
  //     c_rarg5   -          saved encryptedCounter start
  //     rbp + 6 * wordSize - saved used length
  //   Windows
  //     rbp + 6 * wordSize - input length
  //     rbp + 7 * wordSize - saved encryptedCounter start
  //     rbp + 8 * wordSize - saved used length
  //
  // Output:
  //   rax       - input length
  //
  address generate_counterMode_AESCrypt_Parallel() {
    assert(UseAES, "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "counterMode_AESCrypt");
    address start = __ pc();
    const Register from = c_rarg0; // source array address
    const Register to = c_rarg1; // destination array address
    const Register key = c_rarg2; // key array address
    const Register counter = c_rarg3; // counter byte array initialized from counter array address
                                      // and updated with the incremented counter in the end
#ifndef _WIN64
    const Register len_reg = c_rarg4;
    const Register saved_encCounter_start = c_rarg5;
    const Register used_addr = r10;
    const Address  used_mem(rbp, 2 * wordSize);
    const Register used = r11;
#else
    const Address len_mem(rbp, 6 * wordSize); // length is on stack on Win64
    const Address saved_encCounter_mem(rbp, 7 * wordSize); // length is on stack on Win64
    const Address used_mem(rbp, 8 * wordSize); // length is on stack on Win64
    const Register len_reg = r10; // pick the first volatile windows register
    const Register saved_encCounter_start = r11;
    const Register used_addr = r13;
    const Register used = r14;
#endif
    const Register pos = rax;

    const int PARALLEL_FACTOR = 6;
    const XMMRegister xmm_counter_shuf_mask = xmm0;
    const XMMRegister xmm_key_shuf_mask = xmm1; // used temporarily to swap key bytes up front
    const XMMRegister xmm_curr_counter = xmm2;

    const XMMRegister xmm_key_tmp0 = xmm3;
    const XMMRegister xmm_key_tmp1 = xmm4;

    // registers holding the four results in the parallelized loop
    const XMMRegister xmm_result0 = xmm5;
    const XMMRegister xmm_result1 = xmm6;
    const XMMRegister xmm_result2 = xmm7;
    const XMMRegister xmm_result3 = xmm8;
    const XMMRegister xmm_result4 = xmm9;
    const XMMRegister xmm_result5 = xmm10;

    const XMMRegister xmm_from0 = xmm11;
    const XMMRegister xmm_from1 = xmm12;
    const XMMRegister xmm_from2 = xmm13;
    const XMMRegister xmm_from3 = xmm14; //the last one is xmm14. we have to preserve it on WIN64.
    const XMMRegister xmm_from4 = xmm3; //reuse xmm3~4. Because xmm_key_tmp0~1 are useless when loading input text
    const XMMRegister xmm_from5 = xmm4;

    //for key_128, key_192, key_256
    const int rounds[3] = {10, 12, 14};
    Label L_exit_preLoop, L_preLoop_start;
    Label L_multiBlock_loopTop[3];
    Label L_singleBlockLoopTop[3];
    Label L__incCounter[3][6]; //for 6 blocks
    Label L__incCounter_single[3]; //for single block, key128, key192, key256
    Label L_processTail_insr[3], L_processTail_4_insr[3], L_processTail_2_insr[3], L_processTail_1_insr[3], L_processTail_exit_insr[3];
    Label L_processTail_4_extr[3], L_processTail_2_extr[3], L_processTail_1_extr[3], L_processTail_exit_extr[3];

    Label L_exit;

    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    // allocate spill slots for r13, r14
    enum {
        saved_r13_offset,
        saved_r14_offset
    };
    __ subptr(rsp, 2 * wordSize);
    __ movptr(Address(rsp, saved_r13_offset * wordSize), r13);
    __ movptr(Address(rsp, saved_r14_offset * wordSize), r14);

    // on win64, fill len_reg from stack position
    __ movl(len_reg, len_mem);
    __ movptr(saved_encCounter_start, saved_encCounter_mem);
    __ movptr(used_addr, used_mem);
    __ movl(used, Address(used_addr, 0));
#else
    __ push(len_reg); // Save
    __ movptr(used_addr, used_mem);
    __ movl(used, Address(used_addr, 0));
#endif

    __ push(rbx); // Save RBX
    __ movdqu(xmm_curr_counter, Address(counter, 0x00)); // initialize counter with initial counter
    __ movdqu(xmm_counter_shuf_mask, ExternalAddress(StubRoutines::x86::counter_shuffle_mask_addr()), pos); // pos as scratch
    __ pshufb(xmm_curr_counter, xmm_counter_shuf_mask); //counter is shuffled
    __ movptr(pos, 0);

    // Use the partially used encrpyted counter from last invocation
    __ BIND(L_preLoop_start);
    __ cmpptr(used, 16);
    __ jcc(Assembler::aboveEqual, L_exit_preLoop);
      __ cmpptr(len_reg, 0);
      __ jcc(Assembler::lessEqual, L_exit_preLoop);
      __ movb(rbx, Address(saved_encCounter_start, used));
      __ xorb(rbx, Address(from, pos));
      __ movb(Address(to, pos), rbx);
      __ addptr(pos, 1);
      __ addptr(used, 1);
      __ subptr(len_reg, 1);

    __ jmp(L_preLoop_start);

    __ BIND(L_exit_preLoop);
    __ movl(Address(used_addr, 0), used);

    // key length could be only {11, 13, 15} * 4 = {44, 52, 60}
    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()), rbx); // rbx as scratch
    __ movl(rbx, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));
    __ cmpl(rbx, 52);
    __ jcc(Assembler::equal, L_multiBlock_loopTop[1]);
    __ cmpl(rbx, 60);
    __ jcc(Assembler::equal, L_multiBlock_loopTop[2]);

#define CTR_DoSix(opc, src_reg)                \
    __ opc(xmm_result0, src_reg);              \
    __ opc(xmm_result1, src_reg);              \
    __ opc(xmm_result2, src_reg);              \
    __ opc(xmm_result3, src_reg);              \
    __ opc(xmm_result4, src_reg);              \
    __ opc(xmm_result5, src_reg);

    // k == 0 :  generate code for key_128
    // k == 1 :  generate code for key_192
    // k == 2 :  generate code for key_256
    for (int k = 0; k < 3; ++k) {
      //multi blocks starts here
      __ align(OptoLoopAlignment);
      __ BIND(L_multiBlock_loopTop[k]);
      __ cmpptr(len_reg, PARALLEL_FACTOR * AESBlockSize); // see if at least PARALLEL_FACTOR blocks left
      __ jcc(Assembler::less, L_singleBlockLoopTop[k]);
      load_key(xmm_key_tmp0, key, 0x00, xmm_key_shuf_mask);

      //load, then increase counters
      CTR_DoSix(movdqa, xmm_curr_counter);
      inc_counter(rbx, xmm_result1, 0x01, L__incCounter[k][0]);
      inc_counter(rbx, xmm_result2, 0x02, L__incCounter[k][1]);
      inc_counter(rbx, xmm_result3, 0x03, L__incCounter[k][2]);
      inc_counter(rbx, xmm_result4, 0x04, L__incCounter[k][3]);
      inc_counter(rbx, xmm_result5,  0x05, L__incCounter[k][4]);
      inc_counter(rbx, xmm_curr_counter, 0x06, L__incCounter[k][5]);
      CTR_DoSix(pshufb, xmm_counter_shuf_mask); // after increased, shuffled counters back for PXOR
      CTR_DoSix(pxor, xmm_key_tmp0);   //PXOR with Round 0 key

      //load two ROUND_KEYs at a time
      for (int i = 1; i < rounds[k]; ) {
        load_key(xmm_key_tmp1, key, (0x10 * i), xmm_key_shuf_mask);
        load_key(xmm_key_tmp0, key, (0x10 * (i+1)), xmm_key_shuf_mask);
        CTR_DoSix(aesenc, xmm_key_tmp1);
        i++;
        if (i != rounds[k]) {
          CTR_DoSix(aesenc, xmm_key_tmp0);
        } else {
          CTR_DoSix(aesenclast, xmm_key_tmp0);
        }
        i++;
      }

      // get next PARALLEL_FACTOR blocks into xmm_result registers
      __ movdqu(xmm_from0, Address(from, pos, Address::times_1, 0 * AESBlockSize));
      __ movdqu(xmm_from1, Address(from, pos, Address::times_1, 1 * AESBlockSize));
      __ movdqu(xmm_from2, Address(from, pos, Address::times_1, 2 * AESBlockSize));
      __ movdqu(xmm_from3, Address(from, pos, Address::times_1, 3 * AESBlockSize));
      __ movdqu(xmm_from4, Address(from, pos, Address::times_1, 4 * AESBlockSize));
      __ movdqu(xmm_from5, Address(from, pos, Address::times_1, 5 * AESBlockSize));

      __ pxor(xmm_result0, xmm_from0);
      __ pxor(xmm_result1, xmm_from1);
      __ pxor(xmm_result2, xmm_from2);
      __ pxor(xmm_result3, xmm_from3);
      __ pxor(xmm_result4, xmm_from4);
      __ pxor(xmm_result5, xmm_from5);

      // store 6 results into the next 64 bytes of output
      __ movdqu(Address(to, pos, Address::times_1, 0 * AESBlockSize), xmm_result0);
      __ movdqu(Address(to, pos, Address::times_1, 1 * AESBlockSize), xmm_result1);
      __ movdqu(Address(to, pos, Address::times_1, 2 * AESBlockSize), xmm_result2);
      __ movdqu(Address(to, pos, Address::times_1, 3 * AESBlockSize), xmm_result3);
      __ movdqu(Address(to, pos, Address::times_1, 4 * AESBlockSize), xmm_result4);
      __ movdqu(Address(to, pos, Address::times_1, 5 * AESBlockSize), xmm_result5);

      __ addptr(pos, PARALLEL_FACTOR * AESBlockSize); // increase the length of crypt text
      __ subptr(len_reg, PARALLEL_FACTOR * AESBlockSize); // decrease the remaining length
      __ jmp(L_multiBlock_loopTop[k]);

      // singleBlock starts here
      __ align(OptoLoopAlignment);
      __ BIND(L_singleBlockLoopTop[k]);
      __ cmpptr(len_reg, 0);
      __ jcc(Assembler::lessEqual, L_exit);
      load_key(xmm_key_tmp0, key, 0x00, xmm_key_shuf_mask);
      __ movdqa(xmm_result0, xmm_curr_counter);
      inc_counter(rbx, xmm_curr_counter, 0x01, L__incCounter_single[k]);
      __ pshufb(xmm_result0, xmm_counter_shuf_mask);
      __ pxor(xmm_result0, xmm_key_tmp0);
      for (int i = 1; i < rounds[k]; i++) {
        load_key(xmm_key_tmp0, key, (0x10 * i), xmm_key_shuf_mask);
        __ aesenc(xmm_result0, xmm_key_tmp0);
      }
      load_key(xmm_key_tmp0, key, (rounds[k] * 0x10), xmm_key_shuf_mask);
      __ aesenclast(xmm_result0, xmm_key_tmp0);
      __ cmpptr(len_reg, AESBlockSize);
      __ jcc(Assembler::less, L_processTail_insr[k]);
        __ movdqu(xmm_from0, Address(from, pos, Address::times_1, 0 * AESBlockSize));
        __ pxor(xmm_result0, xmm_from0);
        __ movdqu(Address(to, pos, Address::times_1, 0 * AESBlockSize), xmm_result0);
        __ addptr(pos, AESBlockSize);
        __ subptr(len_reg, AESBlockSize);
        __ jmp(L_singleBlockLoopTop[k]);
      __ BIND(L_processTail_insr[k]);                               // Process the tail part of the input array
        __ addptr(pos, len_reg);                                    // 1. Insert bytes from src array into xmm_from0 register
        __ testptr(len_reg, 8);
        __ jcc(Assembler::zero, L_processTail_4_insr[k]);
          __ subptr(pos,8);
          __ pinsrq(xmm_from0, Address(from, pos), 0);
        __ BIND(L_processTail_4_insr[k]);
        __ testptr(len_reg, 4);
        __ jcc(Assembler::zero, L_processTail_2_insr[k]);
          __ subptr(pos,4);
          __ pslldq(xmm_from0, 4);
          __ pinsrd(xmm_from0, Address(from, pos), 0);
        __ BIND(L_processTail_2_insr[k]);
        __ testptr(len_reg, 2);
        __ jcc(Assembler::zero, L_processTail_1_insr[k]);
          __ subptr(pos, 2);
          __ pslldq(xmm_from0, 2);
          __ pinsrw(xmm_from0, Address(from, pos), 0);
        __ BIND(L_processTail_1_insr[k]);
        __ testptr(len_reg, 1);
        __ jcc(Assembler::zero, L_processTail_exit_insr[k]);
          __ subptr(pos, 1);
          __ pslldq(xmm_from0, 1);
          __ pinsrb(xmm_from0, Address(from, pos), 0);
        __ BIND(L_processTail_exit_insr[k]);

        __ movdqu(Address(saved_encCounter_start, 0), xmm_result0);  // 2. Perform pxor of the encrypted counter and plaintext Bytes.
        __ pxor(xmm_result0, xmm_from0);                             //    Also the encrypted counter is saved for next invocation.

        __ testptr(len_reg, 8);
        __ jcc(Assembler::zero, L_processTail_4_extr[k]);            // 3. Extract bytes from xmm_result0 into the dest. array
          __ pextrq(Address(to, pos), xmm_result0, 0);
          __ psrldq(xmm_result0, 8);
          __ addptr(pos, 8);
        __ BIND(L_processTail_4_extr[k]);
        __ testptr(len_reg, 4);
        __ jcc(Assembler::zero, L_processTail_2_extr[k]);
          __ pextrd(Address(to, pos), xmm_result0, 0);
          __ psrldq(xmm_result0, 4);
          __ addptr(pos, 4);
        __ BIND(L_processTail_2_extr[k]);
        __ testptr(len_reg, 2);
        __ jcc(Assembler::zero, L_processTail_1_extr[k]);
          __ pextrw(Address(to, pos), xmm_result0, 0);
          __ psrldq(xmm_result0, 2);
          __ addptr(pos, 2);
        __ BIND(L_processTail_1_extr[k]);
        __ testptr(len_reg, 1);
        __ jcc(Assembler::zero, L_processTail_exit_extr[k]);
          __ pextrb(Address(to, pos), xmm_result0, 0);

        __ BIND(L_processTail_exit_extr[k]);
        __ movl(Address(used_addr, 0), len_reg);
        __ jmp(L_exit);

    }

    __ BIND(L_exit);
    __ pshufb(xmm_curr_counter, xmm_counter_shuf_mask); //counter is shuffled back.
    __ movdqu(Address(counter, 0), xmm_curr_counter); //save counter back
    __ pop(rbx); // pop the saved RBX.
#ifdef _WIN64
    __ movl(rax, len_mem);
    __ movptr(r13, Address(rsp, saved_r13_offset * wordSize));
    __ movptr(r14, Address(rsp, saved_r14_offset * wordSize));
    __ addptr(rsp, 2 * wordSize);
#else
    __ pop(rax); // return 'len'
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

void roundDec(XMMRegister xmm_reg) {
  __ vaesdec(xmm1, xmm1, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm2, xmm2, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm3, xmm3, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm4, xmm4, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm5, xmm5, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm6, xmm6, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm7, xmm7, xmm_reg, Assembler::AVX_512bit);
  __ vaesdec(xmm8, xmm8, xmm_reg, Assembler::AVX_512bit);
}

void roundDeclast(XMMRegister xmm_reg) {
  __ vaesdeclast(xmm1, xmm1, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm2, xmm2, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm3, xmm3, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm4, xmm4, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm5, xmm5, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm6, xmm6, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm7, xmm7, xmm_reg, Assembler::AVX_512bit);
  __ vaesdeclast(xmm8, xmm8, xmm_reg, Assembler::AVX_512bit);
}

  void ev_load_key(XMMRegister xmmdst, Register key, int offset, XMMRegister xmm_shuf_mask = NULL) {
    __ movdqu(xmmdst, Address(key, offset));
    if (xmm_shuf_mask != NULL) {
      __ pshufb(xmmdst, xmm_shuf_mask);
    } else {
      __ pshufb(xmmdst, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));
    }
    __ evshufi64x2(xmmdst, xmmdst, xmmdst, 0x0, Assembler::AVX_512bit);

  }

address generate_cipherBlockChaining_decryptVectorAESCrypt() {
    assert(VM_Version::supports_avx512_vaes(), "need AES instructions and misaligned SSE support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_decryptAESCrypt");
    address start = __ pc();

    const Register from = c_rarg0;  // source array address
    const Register to = c_rarg1;  // destination array address
    const Register key = c_rarg2;  // key array address
    const Register rvec = c_rarg3;  // r byte array initialized from initvector array address
    // and left with the results of the last encryption block
#ifndef _WIN64
    const Register len_reg = c_rarg4;  // src len (must be multiple of blocksize 16)
#else
    const Address  len_mem(rbp, 6 * wordSize);  // length is on stack on Win64
    const Register len_reg = r11;      // pick the volatile windows register
#endif

    Label Loop, Loop1, L_128, L_256, L_192, KEY_192, KEY_256, Loop2, Lcbc_dec_rem_loop,
          Lcbc_dec_rem_last, Lcbc_dec_ret, Lcbc_dec_rem, Lcbc_exit;

    __ enter();

#ifdef _WIN64
  // on win64, fill len_reg from stack position
    __ movl(len_reg, len_mem);
#else
    __ push(len_reg); // Save
#endif
    __ push(rbx);
    __ vzeroupper();

    // Temporary variable declaration for swapping key bytes
    const XMMRegister xmm_key_shuf_mask = xmm1;
    __ movdqu(xmm_key_shuf_mask, ExternalAddress(StubRoutines::x86::key_shuffle_mask_addr()));

    // Calculate number of rounds from key size: 44 for 10-rounds, 52 for 12-rounds, 60 for 14-rounds
    const Register rounds = rbx;
    __ movl(rounds, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    const XMMRegister IV = xmm0;
    // Load IV and broadcast value to 512-bits
    __ evbroadcasti64x2(IV, Address(rvec, 0), Assembler::AVX_512bit);

    // Temporary variables for storing round keys
    const XMMRegister RK0 = xmm30;
    const XMMRegister RK1 = xmm9;
    const XMMRegister RK2 = xmm18;
    const XMMRegister RK3 = xmm19;
    const XMMRegister RK4 = xmm20;
    const XMMRegister RK5 = xmm21;
    const XMMRegister RK6 = xmm22;
    const XMMRegister RK7 = xmm23;
    const XMMRegister RK8 = xmm24;
    const XMMRegister RK9 = xmm25;
    const XMMRegister RK10 = xmm26;

     // Load and shuffle key
    // the java expanded key ordering is rotated one position from what we want
    // so we start from 1*16 here and hit 0*16 last
    ev_load_key(RK1, key, 1 * 16, xmm_key_shuf_mask);
    ev_load_key(RK2, key, 2 * 16, xmm_key_shuf_mask);
    ev_load_key(RK3, key, 3 * 16, xmm_key_shuf_mask);
    ev_load_key(RK4, key, 4 * 16, xmm_key_shuf_mask);
    ev_load_key(RK5, key, 5 * 16, xmm_key_shuf_mask);
    ev_load_key(RK6, key, 6 * 16, xmm_key_shuf_mask);
    ev_load_key(RK7, key, 7 * 16, xmm_key_shuf_mask);
    ev_load_key(RK8, key, 8 * 16, xmm_key_shuf_mask);
    ev_load_key(RK9, key, 9 * 16, xmm_key_shuf_mask);
    ev_load_key(RK10, key, 10 * 16, xmm_key_shuf_mask);
    ev_load_key(RK0, key, 0*16, xmm_key_shuf_mask);

    // Variables for storing source cipher text
    const XMMRegister S0 = xmm10;
    const XMMRegister S1 = xmm11;
    const XMMRegister S2 = xmm12;
    const XMMRegister S3 = xmm13;
    const XMMRegister S4 = xmm14;
    const XMMRegister S5 = xmm15;
    const XMMRegister S6 = xmm16;
    const XMMRegister S7 = xmm17;

    // Variables for storing decrypted text
    const XMMRegister B0 = xmm1;
    const XMMRegister B1 = xmm2;
    const XMMRegister B2 = xmm3;
    const XMMRegister B3 = xmm4;
    const XMMRegister B4 = xmm5;
    const XMMRegister B5 = xmm6;
    const XMMRegister B6 = xmm7;
    const XMMRegister B7 = xmm8;

    __ cmpl(rounds, 44);
    __ jcc(Assembler::greater, KEY_192);
    __ jmp(Loop);

    __ BIND(KEY_192);
    const XMMRegister RK11 = xmm27;
    const XMMRegister RK12 = xmm28;
    ev_load_key(RK11, key, 11*16, xmm_key_shuf_mask);
    ev_load_key(RK12, key, 12*16, xmm_key_shuf_mask);

    __ cmpl(rounds, 52);
    __ jcc(Assembler::greater, KEY_256);
    __ jmp(Loop);

    __ BIND(KEY_256);
    const XMMRegister RK13 = xmm29;
    const XMMRegister RK14 = xmm31;
    ev_load_key(RK13, key, 13*16, xmm_key_shuf_mask);
    ev_load_key(RK14, key, 14*16, xmm_key_shuf_mask);

    __ BIND(Loop);
    __ cmpl(len_reg, 512);
    __ jcc(Assembler::below, Lcbc_dec_rem);
    __ BIND(Loop1);
    __ subl(len_reg, 512);
    __ evmovdquq(S0, Address(from, 0 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S1, Address(from, 1 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S2, Address(from, 2 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S3, Address(from, 3 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S4, Address(from, 4 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S5, Address(from, 5 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S6, Address(from, 6 * 64), Assembler::AVX_512bit);
    __ evmovdquq(S7, Address(from, 7 * 64), Assembler::AVX_512bit);
    __ leaq(from, Address(from, 8 * 64));

    __ evpxorq(B0, S0, RK1, Assembler::AVX_512bit);
    __ evpxorq(B1, S1, RK1, Assembler::AVX_512bit);
    __ evpxorq(B2, S2, RK1, Assembler::AVX_512bit);
    __ evpxorq(B3, S3, RK1, Assembler::AVX_512bit);
    __ evpxorq(B4, S4, RK1, Assembler::AVX_512bit);
    __ evpxorq(B5, S5, RK1, Assembler::AVX_512bit);
    __ evpxorq(B6, S6, RK1, Assembler::AVX_512bit);
    __ evpxorq(B7, S7, RK1, Assembler::AVX_512bit);

    __ evalignq(IV, S0, IV, 0x06);
    __ evalignq(S0, S1, S0, 0x06);
    __ evalignq(S1, S2, S1, 0x06);
    __ evalignq(S2, S3, S2, 0x06);
    __ evalignq(S3, S4, S3, 0x06);
    __ evalignq(S4, S5, S4, 0x06);
    __ evalignq(S5, S6, S5, 0x06);
    __ evalignq(S6, S7, S6, 0x06);

    roundDec(RK2);
    roundDec(RK3);
    roundDec(RK4);
    roundDec(RK5);
    roundDec(RK6);
    roundDec(RK7);
    roundDec(RK8);
    roundDec(RK9);
    roundDec(RK10);

    __ cmpl(rounds, 44);
    __ jcc(Assembler::belowEqual, L_128);
    roundDec(RK11);
    roundDec(RK12);

    __ cmpl(rounds, 52);
    __ jcc(Assembler::belowEqual, L_192);
    roundDec(RK13);
    roundDec(RK14);

    __ BIND(L_256);
    roundDeclast(RK0);
    __ jmp(Loop2);

    __ BIND(L_128);
    roundDeclast(RK0);
    __ jmp(Loop2);

    __ BIND(L_192);
    roundDeclast(RK0);

    __ BIND(Loop2);
    __ evpxorq(B0, B0, IV, Assembler::AVX_512bit);
    __ evpxorq(B1, B1, S0, Assembler::AVX_512bit);
    __ evpxorq(B2, B2, S1, Assembler::AVX_512bit);
    __ evpxorq(B3, B3, S2, Assembler::AVX_512bit);
    __ evpxorq(B4, B4, S3, Assembler::AVX_512bit);
    __ evpxorq(B5, B5, S4, Assembler::AVX_512bit);
    __ evpxorq(B6, B6, S5, Assembler::AVX_512bit);
    __ evpxorq(B7, B7, S6, Assembler::AVX_512bit);
    __ evmovdquq(IV, S7, Assembler::AVX_512bit);

    __ evmovdquq(Address(to, 0 * 64), B0, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 1 * 64), B1, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 2 * 64), B2, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 3 * 64), B3, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 4 * 64), B4, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 5 * 64), B5, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 6 * 64), B6, Assembler::AVX_512bit);
    __ evmovdquq(Address(to, 7 * 64), B7, Assembler::AVX_512bit);
    __ leaq(to, Address(to, 8 * 64));
    __ jmp(Loop);

    __ BIND(Lcbc_dec_rem);
    __ evshufi64x2(IV, IV, IV, 0x03, Assembler::AVX_512bit);

    __ BIND(Lcbc_dec_rem_loop);
    __ subl(len_reg, 16);
    __ jcc(Assembler::carrySet, Lcbc_dec_ret);

    __ movdqu(S0, Address(from, 0));
    __ evpxorq(B0, S0, RK1, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK2, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK3, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK4, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK5, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK6, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK7, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK8, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK9, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK10, Assembler::AVX_512bit);
    __ cmpl(rounds, 44);
    __ jcc(Assembler::belowEqual, Lcbc_dec_rem_last);

    __ vaesdec(B0, B0, RK11, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK12, Assembler::AVX_512bit);
    __ cmpl(rounds, 52);
    __ jcc(Assembler::belowEqual, Lcbc_dec_rem_last);

    __ vaesdec(B0, B0, RK13, Assembler::AVX_512bit);
    __ vaesdec(B0, B0, RK14, Assembler::AVX_512bit);

    __ BIND(Lcbc_dec_rem_last);
    __ vaesdeclast(B0, B0, RK0, Assembler::AVX_512bit);

    __ evpxorq(B0, B0, IV, Assembler::AVX_512bit);
    __ evmovdquq(IV, S0, Assembler::AVX_512bit);
    __ movdqu(Address(to, 0), B0);
    __ leaq(from, Address(from, 16));
    __ leaq(to, Address(to, 16));
    __ jmp(Lcbc_dec_rem_loop);

    __ BIND(Lcbc_dec_ret);
    __ movdqu(Address(rvec, 0), IV);

    // Zero out the round keys
    __ evpxorq(RK0, RK0, RK0, Assembler::AVX_512bit);
    __ evpxorq(RK1, RK1, RK1, Assembler::AVX_512bit);
    __ evpxorq(RK2, RK2, RK2, Assembler::AVX_512bit);
    __ evpxorq(RK3, RK3, RK3, Assembler::AVX_512bit);
    __ evpxorq(RK4, RK4, RK4, Assembler::AVX_512bit);
    __ evpxorq(RK5, RK5, RK5, Assembler::AVX_512bit);
    __ evpxorq(RK6, RK6, RK6, Assembler::AVX_512bit);
    __ evpxorq(RK7, RK7, RK7, Assembler::AVX_512bit);
    __ evpxorq(RK8, RK8, RK8, Assembler::AVX_512bit);
    __ evpxorq(RK9, RK9, RK9, Assembler::AVX_512bit);
    __ evpxorq(RK10, RK10, RK10, Assembler::AVX_512bit);
    __ cmpl(rounds, 44);
    __ jcc(Assembler::belowEqual, Lcbc_exit);
    __ evpxorq(RK11, RK11, RK11, Assembler::AVX_512bit);
    __ evpxorq(RK12, RK12, RK12, Assembler::AVX_512bit);
    __ cmpl(rounds, 52);
    __ jcc(Assembler::belowEqual, Lcbc_exit);
    __ evpxorq(RK13, RK13, RK13, Assembler::AVX_512bit);
    __ evpxorq(RK14, RK14, RK14, Assembler::AVX_512bit);

    __ BIND(Lcbc_exit);
    __ pop(rbx);
#ifdef _WIN64
    __ movl(rax, len_mem);
#else
    __ pop(rax); // return length
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
}

// Polynomial x^128+x^127+x^126+x^121+1
address ghash_polynomial_addr() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "_ghash_poly_addr");
    address start = __ pc();
    __ emit_data64(0x0000000000000001, relocInfo::none);
    __ emit_data64(0xc200000000000000, relocInfo::none);
    return start;
}

address ghash_shufflemask_addr() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "_ghash_shuffmask_addr");
    address start = __ pc();
    __ emit_data64(0x0f0f0f0f0f0f0f0f, relocInfo::none);
    __ emit_data64(0x0f0f0f0f0f0f0f0f, relocInfo::none);
    return start;
}

// Ghash single and multi block operations using AVX instructions
address generate_avx_ghash_processBlocks() {
    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "ghash_processBlocks");
    address start = __ pc();

    // arguments
    const Register state = c_rarg0;
    const Register htbl = c_rarg1;
    const Register data = c_rarg2;
    const Register blocks = c_rarg3;
    __ enter();
   // Save state before entering routine
    __ avx_ghash(state, htbl, data, blocks);
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
}

  // byte swap x86 long
  address generate_ghash_long_swap_mask() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "ghash_long_swap_mask");
    address start = __ pc();
    __ emit_data64(0x0f0e0d0c0b0a0908, relocInfo::none );
    __ emit_data64(0x0706050403020100, relocInfo::none );
  return start;
  }

  // byte swap x86 byte array
  address generate_ghash_byte_swap_mask() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "ghash_byte_swap_mask");
    address start = __ pc();
    __ emit_data64(0x08090a0b0c0d0e0f, relocInfo::none );
    __ emit_data64(0x0001020304050607, relocInfo::none );
  return start;
  }

  /* Single and multi-block ghash operations */
  address generate_ghash_processBlocks() {
    __ align(CodeEntryAlignment);
    Label L_ghash_loop, L_exit;
    StubCodeMark mark(this, "StubRoutines", "ghash_processBlocks");
    address start = __ pc();

    const Register state        = c_rarg0;
    const Register subkeyH      = c_rarg1;
    const Register data         = c_rarg2;
    const Register blocks       = c_rarg3;

    const XMMRegister xmm_temp0 = xmm0;
    const XMMRegister xmm_temp1 = xmm1;
    const XMMRegister xmm_temp2 = xmm2;
    const XMMRegister xmm_temp3 = xmm3;
    const XMMRegister xmm_temp4 = xmm4;
    const XMMRegister xmm_temp5 = xmm5;
    const XMMRegister xmm_temp6 = xmm6;
    const XMMRegister xmm_temp7 = xmm7;
    const XMMRegister xmm_temp8 = xmm8;
    const XMMRegister xmm_temp9 = xmm9;
    const XMMRegister xmm_temp10 = xmm10;

    __ enter();

    __ movdqu(xmm_temp10, ExternalAddress(StubRoutines::x86::ghash_long_swap_mask_addr()));

    __ movdqu(xmm_temp0, Address(state, 0));
    __ pshufb(xmm_temp0, xmm_temp10);


    __ BIND(L_ghash_loop);
    __ movdqu(xmm_temp2, Address(data, 0));
    __ pshufb(xmm_temp2, ExternalAddress(StubRoutines::x86::ghash_byte_swap_mask_addr()));

    __ movdqu(xmm_temp1, Address(subkeyH, 0));
    __ pshufb(xmm_temp1, xmm_temp10);

    __ pxor(xmm_temp0, xmm_temp2);

    //
    // Multiply with the hash key
    //
    __ movdqu(xmm_temp3, xmm_temp0);
    __ pclmulqdq(xmm_temp3, xmm_temp1, 0);      // xmm3 holds a0*b0
    __ movdqu(xmm_temp4, xmm_temp0);
    __ pclmulqdq(xmm_temp4, xmm_temp1, 16);     // xmm4 holds a0*b1

    __ movdqu(xmm_temp5, xmm_temp0);
    __ pclmulqdq(xmm_temp5, xmm_temp1, 1);      // xmm5 holds a1*b0
    __ movdqu(xmm_temp6, xmm_temp0);
    __ pclmulqdq(xmm_temp6, xmm_temp1, 17);     // xmm6 holds a1*b1

    __ pxor(xmm_temp4, xmm_temp5);      // xmm4 holds a0*b1 + a1*b0

    __ movdqu(xmm_temp5, xmm_temp4);    // move the contents of xmm4 to xmm5
    __ psrldq(xmm_temp4, 8);    // shift by xmm4 64 bits to the right
    __ pslldq(xmm_temp5, 8);    // shift by xmm5 64 bits to the left
    __ pxor(xmm_temp3, xmm_temp5);
    __ pxor(xmm_temp6, xmm_temp4);      // Register pair <xmm6:xmm3> holds the result
                                        // of the carry-less multiplication of
                                        // xmm0 by xmm1.

    // We shift the result of the multiplication by one bit position
    // to the left to cope for the fact that the bits are reversed.
    __ movdqu(xmm_temp7, xmm_temp3);
    __ movdqu(xmm_temp8, xmm_temp6);
    __ pslld(xmm_temp3, 1);
    __ pslld(xmm_temp6, 1);
    __ psrld(xmm_temp7, 31);
    __ psrld(xmm_temp8, 31);
    __ movdqu(xmm_temp9, xmm_temp7);
    __ pslldq(xmm_temp8, 4);
    __ pslldq(xmm_temp7, 4);
    __ psrldq(xmm_temp9, 12);
    __ por(xmm_temp3, xmm_temp7);
    __ por(xmm_temp6, xmm_temp8);
    __ por(xmm_temp6, xmm_temp9);

    //
    // First phase of the reduction
    //
    // Move xmm3 into xmm7, xmm8, xmm9 in order to perform the shifts
    // independently.
    __ movdqu(xmm_temp7, xmm_temp3);
    __ movdqu(xmm_temp8, xmm_temp3);
    __ movdqu(xmm_temp9, xmm_temp3);
    __ pslld(xmm_temp7, 31);    // packed right shift shifting << 31
    __ pslld(xmm_temp8, 30);    // packed right shift shifting << 30
    __ pslld(xmm_temp9, 25);    // packed right shift shifting << 25
    __ pxor(xmm_temp7, xmm_temp8);      // xor the shifted versions
    __ pxor(xmm_temp7, xmm_temp9);
    __ movdqu(xmm_temp8, xmm_temp7);
    __ pslldq(xmm_temp7, 12);
    __ psrldq(xmm_temp8, 4);
    __ pxor(xmm_temp3, xmm_temp7);      // first phase of the reduction complete

    //
    // Second phase of the reduction
    //
    // Make 3 copies of xmm3 in xmm2, xmm4, xmm5 for doing these
    // shift operations.
    __ movdqu(xmm_temp2, xmm_temp3);
    __ movdqu(xmm_temp4, xmm_temp3);
    __ movdqu(xmm_temp5, xmm_temp3);
    __ psrld(xmm_temp2, 1);     // packed left shifting >> 1
    __ psrld(xmm_temp4, 2);     // packed left shifting >> 2
    __ psrld(xmm_temp5, 7);     // packed left shifting >> 7
    __ pxor(xmm_temp2, xmm_temp4);      // xor the shifted versions
    __ pxor(xmm_temp2, xmm_temp5);
    __ pxor(xmm_temp2, xmm_temp8);
    __ pxor(xmm_temp3, xmm_temp2);
    __ pxor(xmm_temp6, xmm_temp3);      // the result is in xmm6

    __ decrement(blocks);
    __ jcc(Assembler::zero, L_exit);
    __ movdqu(xmm_temp0, xmm_temp6);
    __ addptr(data, 16);
    __ jmp(L_ghash_loop);

    __ BIND(L_exit);
    __ pshufb(xmm_temp6, xmm_temp10);          // Byte swap 16-byte result
    __ movdqu(Address(state, 0), xmm_temp6);   // store the result
    __ leave();
    __ ret(0);
    return start;
  }

  address base64_shuffle_addr()
  {
    __ align(64, (unsigned long long)__ pc());
    StubCodeMark mark(this, "StubRoutines", "shuffle_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x0405030401020001, relocInfo::none);
    __ emit_data64(0x0a0b090a07080607, relocInfo::none);
    __ emit_data64(0x10110f100d0e0c0d, relocInfo::none);
    __ emit_data64(0x1617151613141213, relocInfo::none);
    __ emit_data64(0x1c1d1b1c191a1819, relocInfo::none);
    __ emit_data64(0x222321221f201e1f, relocInfo::none);
    __ emit_data64(0x2829272825262425, relocInfo::none);
    __ emit_data64(0x2e2f2d2e2b2c2a2b, relocInfo::none);
    return start;
  }

  address base64_avx2_shuffle_addr()
  {
    __ align(32);
    StubCodeMark mark(this, "StubRoutines", "avx2_shuffle_base64");
    address start = __ pc();
    __ emit_data64(0x0809070805060405, relocInfo::none);
    __ emit_data64(0x0e0f0d0e0b0c0a0b, relocInfo::none);
    __ emit_data64(0x0405030401020001, relocInfo::none);
    __ emit_data64(0x0a0b090a07080607, relocInfo::none);
    return start;
  }

  address base64_avx2_input_mask_addr()
  {
    __ align(32);
    StubCodeMark mark(this, "StubRoutines", "avx2_input_mask_base64");
    address start = __ pc();
    __ emit_data64(0x8000000000000000, relocInfo::none);
    __ emit_data64(0x8000000080000000, relocInfo::none);
    __ emit_data64(0x8000000080000000, relocInfo::none);
    __ emit_data64(0x8000000080000000, relocInfo::none);
    return start;
  }

  address base64_avx2_lut_addr()
  {
    __ align(32);
    StubCodeMark mark(this, "StubRoutines", "avx2_lut_base64");
    address start = __ pc();
    __ emit_data64(0xfcfcfcfcfcfc4741, relocInfo::none);
    __ emit_data64(0x0000f0edfcfcfcfc, relocInfo::none);
    __ emit_data64(0xfcfcfcfcfcfc4741, relocInfo::none);
    __ emit_data64(0x0000f0edfcfcfcfc, relocInfo::none);

    // URL LUT
    __ emit_data64(0xfcfcfcfcfcfc4741, relocInfo::none);
    __ emit_data64(0x000020effcfcfcfc, relocInfo::none);
    __ emit_data64(0xfcfcfcfcfcfc4741, relocInfo::none);
    __ emit_data64(0x000020effcfcfcfc, relocInfo::none);
    return start;
  }

  address base64_encoding_table_addr()
  {
    __ align(64, (unsigned long long)__ pc());
    StubCodeMark mark(this, "StubRoutines", "encoding_table_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0, "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x4847464544434241, relocInfo::none);
    __ emit_data64(0x504f4e4d4c4b4a49, relocInfo::none);
    __ emit_data64(0x5857565554535251, relocInfo::none);
    __ emit_data64(0x6665646362615a59, relocInfo::none);
    __ emit_data64(0x6e6d6c6b6a696867, relocInfo::none);
    __ emit_data64(0x767574737271706f, relocInfo::none);
    __ emit_data64(0x333231307a797877, relocInfo::none);
    __ emit_data64(0x2f2b393837363534, relocInfo::none);

    // URL table
    __ emit_data64(0x4847464544434241, relocInfo::none);
    __ emit_data64(0x504f4e4d4c4b4a49, relocInfo::none);
    __ emit_data64(0x5857565554535251, relocInfo::none);
    __ emit_data64(0x6665646362615a59, relocInfo::none);
    __ emit_data64(0x6e6d6c6b6a696867, relocInfo::none);
    __ emit_data64(0x767574737271706f, relocInfo::none);
    __ emit_data64(0x333231307a797877, relocInfo::none);
    __ emit_data64(0x5f2d393837363534, relocInfo::none);
    return start;
  }

  // Code for generating Base64 encoding.
  // Intrinsic function prototype in Base64.java:
  // private void encodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp,
  // boolean isURL) {
  address generate_base64_encodeBlock()
  {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "implEncode");
    address start = __ pc();
    __ enter();

    // Save callee-saved registers before using them
    __ push(r12);
    __ push(r13);
    __ push(r14);
    __ push(r15);

    // arguments
    const Register source = c_rarg0;       // Source Array
    const Register start_offset = c_rarg1; // start offset
    const Register end_offset = c_rarg2;   // end offset
    const Register dest = c_rarg3;   // destination array

#ifndef _WIN64
    const Register dp = c_rarg4;    // Position for writing to dest array
    const Register isURL = c_rarg5; // Base64 or URL character set
#else
    const Address dp_mem(rbp, 6 * wordSize); // length is on stack on Win64
    const Address isURL_mem(rbp, 7 * wordSize);
    const Register isURL = r10; // pick the volatile windows register
    const Register dp = r12;
    __ movl(dp, dp_mem);
    __ movl(isURL, isURL_mem);
#endif

    const Register length = r14;
    const Register encode_table = r13;
    Label L_process3, L_exit, L_processdata, L_vbmiLoop, L_not512, L_32byteLoop;

    // calculate length from offsets
    __ movl(length, end_offset);
    __ subl(length, start_offset);
    __ cmpl(length, 0);
    __ jcc(Assembler::lessEqual, L_exit);

    // Code for 512-bit VBMI encoding.  Encodes 48 input bytes into 64
    // output bytes. We read 64 input bytes and ignore the last 16, so be
    // sure not to read past the end of the input buffer.
    if (VM_Version::supports_avx512_vbmi()) {
      __ cmpl(length, 64); // Do not overrun input buffer.
      __ jcc(Assembler::below, L_not512);

      __ shll(isURL, 6); // index into decode table based on isURL
      __ lea(encode_table, ExternalAddress(StubRoutines::x86::base64_encoding_table_addr()));
      __ addptr(encode_table, isURL);
      __ shrl(isURL, 6); // restore isURL

      __ mov64(rax, 0x3036242a1016040aull); // Shifts
      __ evmovdquq(xmm3, ExternalAddress(StubRoutines::x86::base64_shuffle_addr()), Assembler::AVX_512bit, r15);
      __ evmovdquq(xmm2, Address(encode_table, 0), Assembler::AVX_512bit);
      __ evpbroadcastq(xmm1, rax, Assembler::AVX_512bit);

      __ align(32);
      __ BIND(L_vbmiLoop);

      __ vpermb(xmm0, xmm3, Address(source, start_offset), Assembler::AVX_512bit);
      __ subl(length, 48);

      // Put the input bytes into the proper lanes for writing, then
      // encode them.
      __ evpmultishiftqb(xmm0, xmm1, xmm0, Assembler::AVX_512bit);
      __ vpermb(xmm0, xmm0, xmm2, Assembler::AVX_512bit);

      // Write to destination
      __ evmovdquq(Address(dest, dp), xmm0, Assembler::AVX_512bit);

      __ addptr(dest, 64);
      __ addptr(source, 48);
      __ cmpl(length, 64);
      __ jcc(Assembler::aboveEqual, L_vbmiLoop);

      __ vzeroupper();
    }

    __ BIND(L_not512);
    if (VM_Version::supports_avx2()
        && VM_Version::supports_avx512vlbw()) {
      /*
      ** This AVX2 encoder is based off the paper at:
      **      https://dl.acm.org/doi/10.1145/3132709
      **
      ** We use AVX2 SIMD instructions to encode 24 bytes into 32
      ** output bytes.
      **
      */
      // Lengths under 32 bytes are done with scalar routine
      __ cmpl(length, 31);
      __ jcc(Assembler::belowEqual, L_process3);

      // Set up supporting constant table data
      __ vmovdqu(xmm9, ExternalAddress(StubRoutines::x86::base64_avx2_shuffle_addr()), rax);
      // 6-bit mask for 2nd and 4th (and multiples) 6-bit values
      __ movl(rax, 0x0fc0fc00);
      __ vmovdqu(xmm1, ExternalAddress(StubRoutines::x86::base64_avx2_input_mask_addr()), rax);
      __ evpbroadcastd(xmm8, rax, Assembler::AVX_256bit);

      // Multiplication constant for "shifting" right by 6 and 10
      // bits
      __ movl(rax, 0x04000040);

      __ subl(length, 24);
      __ evpbroadcastd(xmm7, rax, Assembler::AVX_256bit);

      // For the first load, we mask off reading of the first 4
      // bytes into the register. This is so we can get 4 3-byte
      // chunks into each lane of the register, avoiding having to
      // handle end conditions.  We then shuffle these bytes into a
      // specific order so that manipulation is easier.
      //
      // The initial read loads the XMM register like this:
      //
      // Lower 128-bit lane:
      // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
      // | XX | XX | XX | XX | A0 | A1 | A2 | B0 | B1 | B2 | C0 | C1
      // | C2 | D0 | D1 | D2 |
      // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
      //
      // Upper 128-bit lane:
      // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
      // | E0 | E1 | E2 | F0 | F1 | F2 | G0 | G1 | G2 | H0 | H1 | H2
      // | XX | XX | XX | XX |
      // +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
      //
      // Where A0 is the first input byte, B0 is the fourth, etc.
      // The alphabetical significance denotes the 3 bytes to be
      // consumed and encoded into 4 bytes.
      //
      // We then shuffle the register so each 32-bit word contains
      // the sequence:
      //    A1 A0 A2 A1, B1, B0, B2, B1, etc.
      // Each of these byte sequences are then manipulated into 4
      // 6-bit values ready for encoding.
      //
      // If we focus on one set of 3-byte chunks, changing the
      // nomenclature such that A0 => a, A1 => b, and A2 => c, we
      // shuffle such that each 24-bit chunk contains:
      //
      // b7 b6 b5 b4 b3 b2 b1 b0 | a7 a6 a5 a4 a3 a2 a1 a0 | c7 c6
      // c5 c4 c3 c2 c1 c0 | b7 b6 b5 b4 b3 b2 b1 b0
      // Explain this step.
      // b3 b2 b1 b0 c5 c4 c3 c2 | c1 c0 d5 d4 d3 d2 d1 d0 | a5 a4
      // a3 a2 a1 a0 b5 b4 | b3 b2 b1 b0 c5 c4 c3 c2
      //
      // W first and off all but bits 4-9 and 16-21 (c5..c0 and
      // a5..a0) and shift them using a vector multiplication
      // operation (vpmulhuw) which effectively shifts c right by 6
      // bits and a right by 10 bits.  We similarly mask bits 10-15
      // (d5..d0) and 22-27 (b5..b0) and shift them left by 8 and 4
      // bits respecively.  This is done using vpmullw.  We end up
      // with 4 6-bit values, thus splitting the 3 input bytes,
      // ready for encoding:
      //    0 0 d5..d0 0 0 c5..c0 0 0 b5..b0 0 0 a5..a0
      //
      // For translation, we recognize that there are 5 distinct
      // ranges of legal Base64 characters as below:
      //
      //   +-------------+-------------+------------+
      //   | 6-bit value | ASCII range |   offset   |
      //   +-------------+-------------+------------+
      //   |    0..25    |    A..Z     |     65     |
      //   |   26..51    |    a..z     |     71     |
      //   |   52..61    |    0..9     |     -4     |
      //   |     62      |   + or -    | -19 or -17 |
      //   |     63      |   / or _    | -16 or 32  |
      //   +-------------+-------------+------------+
      //
      // We note that vpshufb does a parallel lookup in a
      // destination register using the lower 4 bits of bytes from a
      // source register.  If we use a saturated subtraction and
      // subtract 51 from each 6-bit value, bytes from [0,51]
      // saturate to 0, and [52,63] map to a range of [1,12].  We
      // distinguish the [0,25] and [26,51] ranges by assigning a
      // value of 13 for all 6-bit values less than 26.  We end up
      // with:
      //
      //   +-------------+-------------+------------+
      //   | 6-bit value |   Reduced   |   offset   |
      //   +-------------+-------------+------------+
      //   |    0..25    |     13      |     65     |
      //   |   26..51    |      0      |     71     |
      //   |   52..61    |    0..9     |     -4     |
      //   |     62      |     11      | -19 or -17 |
      //   |     63      |     12      | -16 or 32  |
      //   +-------------+-------------+------------+
      //
      // We then use a final vpshufb to add the appropriate offset,
      // translating the bytes.
      //
      // Load input bytes - only 28 bytes.  Mask the first load to
      // not load into the full register.
      __ vpmaskmovd(xmm1, xmm1, Address(source, start_offset, Address::times_1, -4), Assembler::AVX_256bit);

      // Move 3-byte chunks of input (12 bytes) into 16 bytes,
      // ordering by:
      //   1, 0, 2, 1; 4, 3, 5, 4; etc.  This groups 6-bit chunks
      //   for easy masking
      __ vpshufb(xmm1, xmm1, xmm9, Assembler::AVX_256bit);

      __ addl(start_offset, 24);

      // Load masking register for first and third (and multiples)
      // 6-bit values.
      __ movl(rax, 0x003f03f0);
      __ evpbroadcastd(xmm6, rax, Assembler::AVX_256bit);
      // Multiplication constant for "shifting" left by 4 and 8 bits
      __ movl(rax, 0x01000010);
      __ evpbroadcastd(xmm5, rax, Assembler::AVX_256bit);

      // Isolate 6-bit chunks of interest
      __ vpand(xmm0, xmm8, xmm1, Assembler::AVX_256bit);

      // Load constants for encoding
      __ movl(rax, 0x19191919);
      __ evpbroadcastd(xmm3, rax, Assembler::AVX_256bit);
      __ movl(rax, 0x33333333);
      __ evpbroadcastd(xmm4, rax, Assembler::AVX_256bit);

      // Shift output bytes 0 and 2 into proper lanes
      __ vpmulhuw(xmm2, xmm0, xmm7, Assembler::AVX_256bit);

      // Mask and shift output bytes 1 and 3 into proper lanes and
      // combine
      __ vpand(xmm0, xmm6, xmm1, Assembler::AVX_256bit);
      __ vpmullw(xmm0, xmm5, xmm0, Assembler::AVX_256bit);
      __ vpor(xmm0, xmm0, xmm2, Assembler::AVX_256bit);

      // Find out which are 0..25.  This indicates which input
      // values fall in the range of 'A'-'Z', which require an
      // additional offset (see comments above)
      __ vpcmpgtb(xmm2, xmm0, xmm3, Assembler::AVX_256bit);
      __ vpsubusb(xmm1, xmm0, xmm4, Assembler::AVX_256bit);
      __ vpsubb(xmm1, xmm1, xmm2, Assembler::AVX_256bit);

      // Load the proper lookup table
      __ lea(r11, ExternalAddress(StubRoutines::x86::base64_avx2_lut_addr()));
      __ movl(r15, isURL);
      __ shll(r15, 5);
      __ vmovdqu(xmm2, Address(r11, r15));

      // Shuffle the offsets based on the range calculation done
      // above. This allows us to add the correct offset to the
      // 6-bit value corresponding to the range documented above.
      __ vpshufb(xmm1, xmm2, xmm1, Assembler::AVX_256bit);
      __ vpaddb(xmm0, xmm1, xmm0, Assembler::AVX_256bit);

      // Store the encoded bytes
      __ vmovdqu(Address(dest, dp), xmm0);
      __ addl(dp, 32);

      __ cmpl(length, 31);
      __ jcc(Assembler::belowEqual, L_process3);

      __ align(32);
      __ BIND(L_32byteLoop);

      // Get next 32 bytes
      __ vmovdqu(xmm1, Address(source, start_offset, Address::times_1, -4));

      __ subl(length, 24);
      __ addl(start_offset, 24);

      // This logic is identical to the above, with only constant
      // register loads removed.  Shuffle the input, mask off 6-bit
      // chunks, shift them into place, then add the offset to
      // encode.
      __ vpshufb(xmm1, xmm1, xmm9, Assembler::AVX_256bit);

      __ vpand(xmm0, xmm8, xmm1, Assembler::AVX_256bit);
      __ vpmulhuw(xmm10, xmm0, xmm7, Assembler::AVX_256bit);
      __ vpand(xmm0, xmm6, xmm1, Assembler::AVX_256bit);
      __ vpmullw(xmm0, xmm5, xmm0, Assembler::AVX_256bit);
      __ vpor(xmm0, xmm0, xmm10, Assembler::AVX_256bit);
      __ vpcmpgtb(xmm10, xmm0, xmm3, Assembler::AVX_256bit);
      __ vpsubusb(xmm1, xmm0, xmm4, Assembler::AVX_256bit);
      __ vpsubb(xmm1, xmm1, xmm10, Assembler::AVX_256bit);
      __ vpshufb(xmm1, xmm2, xmm1, Assembler::AVX_256bit);
      __ vpaddb(xmm0, xmm1, xmm0, Assembler::AVX_256bit);

      // Store the encoded bytes
      __ vmovdqu(Address(dest, dp), xmm0);
      __ addl(dp, 32);

      __ cmpl(length, 31);
      __ jcc(Assembler::above, L_32byteLoop);

      __ BIND(L_process3);
      __ vzeroupper();
    } else {
      __ BIND(L_process3);
    }

    __ cmpl(length, 3);
    __ jcc(Assembler::below, L_exit);

    // Load the encoding table based on isURL
    __ lea(r11, ExternalAddress(StubRoutines::x86::base64_encoding_table_addr()));
    __ movl(r15, isURL);
    __ shll(r15, 6);
    __ addptr(r11, r15);

    __ BIND(L_processdata);

    // Load 3 bytes
    __ load_unsigned_byte(r15, Address(source, start_offset));
    __ load_unsigned_byte(r10, Address(source, start_offset, Address::times_1, 1));
    __ load_unsigned_byte(r13, Address(source, start_offset, Address::times_1, 2));

    // Build a 32-bit word with bytes 1, 2, 0, 1
    __ movl(rax, r10);
    __ shll(r10, 24);
    __ orl(rax, r10);

    __ subl(length, 3);

    __ shll(r15, 8);
    __ shll(r13, 16);
    __ orl(rax, r15);

    __ addl(start_offset, 3);

    __ orl(rax, r13);
    // At this point, rax contains | byte1 | byte2 | byte0 | byte1
    // r13 has byte2 << 16 - need low-order 6 bits to translate.
    // This translated byte is the fourth output byte.
    __ shrl(r13, 16);
    __ andl(r13, 0x3f);

    // The high-order 6 bits of r15 (byte0) is translated.
    // The translated byte is the first output byte.
    __ shrl(r15, 10);

    __ load_unsigned_byte(r13, Address(r11, r13));
    __ load_unsigned_byte(r15, Address(r11, r15));

    __ movb(Address(dest, dp, Address::times_1, 3), r13);

    // Extract high-order 4 bits of byte1 and low-order 2 bits of byte0.
    // This translated byte is the second output byte.
    __ shrl(rax, 4);
    __ movl(r10, rax);
    __ andl(rax, 0x3f);

    __ movb(Address(dest, dp, Address::times_1, 0), r15);

    __ load_unsigned_byte(rax, Address(r11, rax));

    // Extract low-order 2 bits of byte1 and high-order 4 bits of byte2.
    // This translated byte is the third output byte.
    __ shrl(r10, 18);
    __ andl(r10, 0x3f);

    __ load_unsigned_byte(r10, Address(r11, r10));

    __ movb(Address(dest, dp, Address::times_1, 1), rax);
    __ movb(Address(dest, dp, Address::times_1, 2), r10);

    __ addl(dp, 4);
    __ cmpl(length, 3);
    __ jcc(Assembler::aboveEqual, L_processdata);

    __ BIND(L_exit);
    __ pop(r15);
    __ pop(r14);
    __ pop(r13);
    __ pop(r12);
    __ leave();
    __ ret(0);
    return start;
  }

  // base64 AVX512vbmi tables
  address base64_vbmi_lookup_lo_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "lookup_lo_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x3f8080803e808080, relocInfo::none);
    __ emit_data64(0x3b3a393837363534, relocInfo::none);
    __ emit_data64(0x8080808080803d3c, relocInfo::none);
    return start;
  }

  address base64_vbmi_lookup_hi_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "lookup_hi_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x0605040302010080, relocInfo::none);
    __ emit_data64(0x0e0d0c0b0a090807, relocInfo::none);
    __ emit_data64(0x161514131211100f, relocInfo::none);
    __ emit_data64(0x8080808080191817, relocInfo::none);
    __ emit_data64(0x201f1e1d1c1b1a80, relocInfo::none);
    __ emit_data64(0x2827262524232221, relocInfo::none);
    __ emit_data64(0x302f2e2d2c2b2a29, relocInfo::none);
    __ emit_data64(0x8080808080333231, relocInfo::none);
    return start;
  }
  address base64_vbmi_lookup_lo_url_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "lookup_lo_base64url");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x8080808080808080, relocInfo::none);
    __ emit_data64(0x80803e8080808080, relocInfo::none);
    __ emit_data64(0x3b3a393837363534, relocInfo::none);
    __ emit_data64(0x8080808080803d3c, relocInfo::none);
    return start;
  }

  address base64_vbmi_lookup_hi_url_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "lookup_hi_base64url");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x0605040302010080, relocInfo::none);
    __ emit_data64(0x0e0d0c0b0a090807, relocInfo::none);
    __ emit_data64(0x161514131211100f, relocInfo::none);
    __ emit_data64(0x3f80808080191817, relocInfo::none);
    __ emit_data64(0x201f1e1d1c1b1a80, relocInfo::none);
    __ emit_data64(0x2827262524232221, relocInfo::none);
    __ emit_data64(0x302f2e2d2c2b2a29, relocInfo::none);
    __ emit_data64(0x8080808080333231, relocInfo::none);
    return start;
  }

  address base64_vbmi_pack_vec_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "pack_vec_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x090a040506000102, relocInfo::none);
    __ emit_data64(0x161011120c0d0e08, relocInfo::none);
    __ emit_data64(0x1c1d1e18191a1415, relocInfo::none);
    __ emit_data64(0x292a242526202122, relocInfo::none);
    __ emit_data64(0x363031322c2d2e28, relocInfo::none);
    __ emit_data64(0x3c3d3e38393a3435, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    __ emit_data64(0x0000000000000000, relocInfo::none);
    return start;
  }

  address base64_vbmi_join_0_1_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "join_0_1_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x090a040506000102, relocInfo::none);
    __ emit_data64(0x161011120c0d0e08, relocInfo::none);
    __ emit_data64(0x1c1d1e18191a1415, relocInfo::none);
    __ emit_data64(0x292a242526202122, relocInfo::none);
    __ emit_data64(0x363031322c2d2e28, relocInfo::none);
    __ emit_data64(0x3c3d3e38393a3435, relocInfo::none);
    __ emit_data64(0x494a444546404142, relocInfo::none);
    __ emit_data64(0x565051524c4d4e48, relocInfo::none);
    return start;
  }

  address base64_vbmi_join_1_2_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "join_1_2_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x1c1d1e18191a1415, relocInfo::none);
    __ emit_data64(0x292a242526202122, relocInfo::none);
    __ emit_data64(0x363031322c2d2e28, relocInfo::none);
    __ emit_data64(0x3c3d3e38393a3435, relocInfo::none);
    __ emit_data64(0x494a444546404142, relocInfo::none);
    __ emit_data64(0x565051524c4d4e48, relocInfo::none);
    __ emit_data64(0x5c5d5e58595a5455, relocInfo::none);
    __ emit_data64(0x696a646566606162, relocInfo::none);
    return start;
  }

  address base64_vbmi_join_2_3_addr() {
    __ align(64, (unsigned long long) __ pc());
    StubCodeMark mark(this, "StubRoutines", "join_2_3_base64");
    address start = __ pc();
    assert(((unsigned long long)start & 0x3f) == 0,
           "Alignment problem (0x%08llx)", (unsigned long long)start);
    __ emit_data64(0x363031322c2d2e28, relocInfo::none);
    __ emit_data64(0x3c3d3e38393a3435, relocInfo::none);
    __ emit_data64(0x494a444546404142, relocInfo::none);
    __ emit_data64(0x565051524c4d4e48, relocInfo::none);
    __ emit_data64(0x5c5d5e58595a5455, relocInfo::none);
    __ emit_data64(0x696a646566606162, relocInfo::none);
    __ emit_data64(0x767071726c6d6e68, relocInfo::none);
    __ emit_data64(0x7c7d7e78797a7475, relocInfo::none);
    return start;
  }

  address base64_decoding_table_addr() {
    StubCodeMark mark(this, "StubRoutines", "decoding_table_base64");
    address start = __ pc();
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0x3fffffff3effffff, relocInfo::none);
    __ emit_data64(0x3b3a393837363534, relocInfo::none);
    __ emit_data64(0xffffffffffff3d3c, relocInfo::none);
    __ emit_data64(0x06050403020100ff, relocInfo::none);
    __ emit_data64(0x0e0d0c0b0a090807, relocInfo::none);
    __ emit_data64(0x161514131211100f, relocInfo::none);
    __ emit_data64(0xffffffffff191817, relocInfo::none);
    __ emit_data64(0x201f1e1d1c1b1aff, relocInfo::none);
    __ emit_data64(0x2827262524232221, relocInfo::none);
    __ emit_data64(0x302f2e2d2c2b2a29, relocInfo::none);
    __ emit_data64(0xffffffffff333231, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);

    // URL table
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffff3effffffffff, relocInfo::none);
    __ emit_data64(0x3b3a393837363534, relocInfo::none);
    __ emit_data64(0xffffffffffff3d3c, relocInfo::none);
    __ emit_data64(0x06050403020100ff, relocInfo::none);
    __ emit_data64(0x0e0d0c0b0a090807, relocInfo::none);
    __ emit_data64(0x161514131211100f, relocInfo::none);
    __ emit_data64(0x3fffffffff191817, relocInfo::none);
    __ emit_data64(0x201f1e1d1c1b1aff, relocInfo::none);
    __ emit_data64(0x2827262524232221, relocInfo::none);
    __ emit_data64(0x302f2e2d2c2b2a29, relocInfo::none);
    __ emit_data64(0xffffffffff333231, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    __ emit_data64(0xffffffffffffffff, relocInfo::none);
    return start;
  }


// Code for generating Base64 decoding.
//
// Based on the article (and associated code) from https://arxiv.org/abs/1910.05109.
//
// Intrinsic function prototype in Base64.java:
// private void decodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp, boolean isURL, isMIME) {
  address generate_base64_decodeBlock() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "implDecode");
    address start = __ pc();
    __ enter();

    // Save callee-saved registers before using them
    __ push(r12);
    __ push(r13);
    __ push(r14);
    __ push(r15);
    __ push(rbx);

    // arguments
    const Register source = c_rarg0; // Source Array
    const Register start_offset = c_rarg1; // start offset
    const Register end_offset = c_rarg2; // end offset
    const Register dest = c_rarg3; // destination array
    const Register isMIME = rbx;

#ifndef _WIN64
    const Register dp = c_rarg4;  // Position for writing to dest array
    const Register isURL = c_rarg5;// Base64 or URL character set
    __ movl(isMIME, Address(rbp, 2 * wordSize));
#else
    const Address  dp_mem(rbp, 6 * wordSize);  // length is on stack on Win64
    const Address isURL_mem(rbp, 7 * wordSize);
    const Register isURL = r10;      // pick the volatile windows register
    const Register dp = r12;
    __ movl(dp, dp_mem);
    __ movl(isURL, isURL_mem);
    __ movl(isMIME, Address(rbp, 8 * wordSize));
#endif

    const XMMRegister lookup_lo = xmm5;
    const XMMRegister lookup_hi = xmm6;
    const XMMRegister errorvec = xmm7;
    const XMMRegister pack16_op = xmm9;
    const XMMRegister pack32_op = xmm8;
    const XMMRegister input0 = xmm3;
    const XMMRegister input1 = xmm20;
    const XMMRegister input2 = xmm21;
    const XMMRegister input3 = xmm19;
    const XMMRegister join01 = xmm12;
    const XMMRegister join12 = xmm11;
    const XMMRegister join23 = xmm10;
    const XMMRegister translated0 = xmm2;
    const XMMRegister translated1 = xmm1;
    const XMMRegister translated2 = xmm0;
    const XMMRegister translated3 = xmm4;

    const XMMRegister merged0 = xmm2;
    const XMMRegister merged1 = xmm1;
    const XMMRegister merged2 = xmm0;
    const XMMRegister merged3 = xmm4;
    const XMMRegister merge_ab_bc0 = xmm2;
    const XMMRegister merge_ab_bc1 = xmm1;
    const XMMRegister merge_ab_bc2 = xmm0;
    const XMMRegister merge_ab_bc3 = xmm4;

    const XMMRegister pack24bits = xmm4;

    const Register length = r14;
    const Register output_size = r13;
    const Register output_mask = r15;
    const KRegister input_mask = k1;

    const XMMRegister input_initial_valid_b64 = xmm0;
    const XMMRegister tmp = xmm10;
    const XMMRegister mask = xmm0;
    const XMMRegister invalid_b64 = xmm1;

    Label L_process256, L_process64, L_process64Loop, L_exit, L_processdata, L_loadURL;
    Label L_continue, L_finalBit, L_padding, L_donePadding, L_bruteForce;
    Label L_forceLoop, L_bottomLoop, L_checkMIME, L_exit_no_vzero;

    // calculate length from offsets
    __ movl(length, end_offset);
    __ subl(length, start_offset);
    __ push(dest);          // Save for return value calc

    // If AVX512 VBMI not supported, just compile non-AVX code
    if(VM_Version::supports_avx512_vbmi() &&
       VM_Version::supports_avx512bw()) {
      __ cmpl(length, 128);     // 128-bytes is break-even for AVX-512
      __ jcc(Assembler::lessEqual, L_bruteForce);

      __ cmpl(isMIME, 0);
      __ jcc(Assembler::notEqual, L_bruteForce);

      // Load lookup tables based on isURL
      __ cmpl(isURL, 0);
      __ jcc(Assembler::notZero, L_loadURL);

      __ evmovdquq(lookup_lo, ExternalAddress(StubRoutines::x86::base64_vbmi_lookup_lo_addr()), Assembler::AVX_512bit, r13);
      __ evmovdquq(lookup_hi, ExternalAddress(StubRoutines::x86::base64_vbmi_lookup_hi_addr()), Assembler::AVX_512bit, r13);

      __ BIND(L_continue);

      __ movl(r15, 0x01400140);
      __ evpbroadcastd(pack16_op, r15, Assembler::AVX_512bit);

      __ movl(r15, 0x00011000);
      __ evpbroadcastd(pack32_op, r15, Assembler::AVX_512bit);

      __ cmpl(length, 0xff);
      __ jcc(Assembler::lessEqual, L_process64);

      // load masks required for decoding data
      __ BIND(L_processdata);
      __ evmovdquq(join01, ExternalAddress(StubRoutines::x86::base64_vbmi_join_0_1_addr()), Assembler::AVX_512bit,r13);
      __ evmovdquq(join12, ExternalAddress(StubRoutines::x86::base64_vbmi_join_1_2_addr()), Assembler::AVX_512bit, r13);
      __ evmovdquq(join23, ExternalAddress(StubRoutines::x86::base64_vbmi_join_2_3_addr()), Assembler::AVX_512bit, r13);

      __ align(32);
      __ BIND(L_process256);
      // Grab input data
      __ evmovdquq(input0, Address(source, start_offset, Address::times_1, 0x00), Assembler::AVX_512bit);
      __ evmovdquq(input1, Address(source, start_offset, Address::times_1, 0x40), Assembler::AVX_512bit);
      __ evmovdquq(input2, Address(source, start_offset, Address::times_1, 0x80), Assembler::AVX_512bit);
      __ evmovdquq(input3, Address(source, start_offset, Address::times_1, 0xc0), Assembler::AVX_512bit);

      // Copy the low part of the lookup table into the destination of the permutation
      __ evmovdquq(translated0, lookup_lo, Assembler::AVX_512bit);
      __ evmovdquq(translated1, lookup_lo, Assembler::AVX_512bit);
      __ evmovdquq(translated2, lookup_lo, Assembler::AVX_512bit);
      __ evmovdquq(translated3, lookup_lo, Assembler::AVX_512bit);

      // Translate the base64 input into "decoded" bytes
      __ evpermt2b(translated0, input0, lookup_hi, Assembler::AVX_512bit);
      __ evpermt2b(translated1, input1, lookup_hi, Assembler::AVX_512bit);
      __ evpermt2b(translated2, input2, lookup_hi, Assembler::AVX_512bit);
      __ evpermt2b(translated3, input3, lookup_hi, Assembler::AVX_512bit);

      // OR all of the translations together to check for errors (high-order bit of byte set)
      __ vpternlogd(input0, 0xfe, input1, input2, Assembler::AVX_512bit);

      __ vpternlogd(input3, 0xfe, translated0, translated1, Assembler::AVX_512bit);
      __ vpternlogd(input0, 0xfe, translated2, translated3, Assembler::AVX_512bit);
      __ vpor(errorvec, input3, input0, Assembler::AVX_512bit);

      // Check if there was an error - if so, try 64-byte chunks
      __ evpmovb2m(k3, errorvec, Assembler::AVX_512bit);
      __ kortestql(k3, k3);
      __ jcc(Assembler::notZero, L_process64);

      // The merging and shuffling happens here
      // We multiply each byte pair [00dddddd | 00cccccc | 00bbbbbb | 00aaaaaa]
      // Multiply [00cccccc] by 2^6 added to [00dddddd] to get [0000cccc | ccdddddd]
      // The pack16_op is a vector of 0x01400140, so multiply D by 1 and C by 0x40
      __ vpmaddubsw(merge_ab_bc0, translated0, pack16_op, Assembler::AVX_512bit);
      __ vpmaddubsw(merge_ab_bc1, translated1, pack16_op, Assembler::AVX_512bit);
      __ vpmaddubsw(merge_ab_bc2, translated2, pack16_op, Assembler::AVX_512bit);
      __ vpmaddubsw(merge_ab_bc3, translated3, pack16_op, Assembler::AVX_512bit);

      // Now do the same with packed 16-bit values.
      // We start with [0000cccc | ccdddddd | 0000aaaa | aabbbbbb]
      // pack32_op is 0x00011000 (2^12, 1), so this multiplies [0000aaaa | aabbbbbb] by 2^12
      // and adds [0000cccc | ccdddddd] to yield [00000000 | aaaaaabb | bbbbcccc | ccdddddd]
      __ vpmaddwd(merged0, merge_ab_bc0, pack32_op, Assembler::AVX_512bit);
      __ vpmaddwd(merged1, merge_ab_bc1, pack32_op, Assembler::AVX_512bit);
      __ vpmaddwd(merged2, merge_ab_bc2, pack32_op, Assembler::AVX_512bit);
      __ vpmaddwd(merged3, merge_ab_bc3, pack32_op, Assembler::AVX_512bit);

      // The join vectors specify which byte from which vector goes into the outputs
      // One of every 4 bytes in the extended vector is zero, so we pack them into their
      // final positions in the register for storing (256 bytes in, 192 bytes out)
      __ evpermt2b(merged0, join01, merged1, Assembler::AVX_512bit);
      __ evpermt2b(merged1, join12, merged2, Assembler::AVX_512bit);
      __ evpermt2b(merged2, join23, merged3, Assembler::AVX_512bit);

      // Store result
      __ evmovdquq(Address(dest, dp, Address::times_1, 0x00), merged0, Assembler::AVX_512bit);
      __ evmovdquq(Address(dest, dp, Address::times_1, 0x40), merged1, Assembler::AVX_512bit);
      __ evmovdquq(Address(dest, dp, Address::times_1, 0x80), merged2, Assembler::AVX_512bit);

      __ addptr(source, 0x100);
      __ addptr(dest, 0xc0);
      __ subl(length, 0x100);
      __ cmpl(length, 64 * 4);
      __ jcc(Assembler::greaterEqual, L_process256);

      // At this point, we've decoded 64 * 4 * n bytes.
      // The remaining length will be <= 64 * 4 - 1.
      // UNLESS there was an error decoding the first 256-byte chunk.  In this
      // case, the length will be arbitrarily long.
      //
      // Note that this will be the path for MIME-encoded strings.

      __ BIND(L_process64);

      __ evmovdquq(pack24bits, ExternalAddress(StubRoutines::x86::base64_vbmi_pack_vec_addr()), Assembler::AVX_512bit, r13);

      __ cmpl(length, 63);
      __ jcc(Assembler::lessEqual, L_finalBit);

      __ align(32);
      __ BIND(L_process64Loop);

      // Handle first 64-byte block

      __ evmovdquq(input0, Address(source, start_offset), Assembler::AVX_512bit);
      __ evmovdquq(translated0, lookup_lo, Assembler::AVX_512bit);
      __ evpermt2b(translated0, input0, lookup_hi, Assembler::AVX_512bit);

      __ vpor(errorvec, translated0, input0, Assembler::AVX_512bit);

      // Check for error and bomb out before updating dest
      __ evpmovb2m(k3, errorvec, Assembler::AVX_512bit);
      __ kortestql(k3, k3);
      __ jcc(Assembler::notZero, L_exit);

      // Pack output register, selecting correct byte ordering
      __ vpmaddubsw(merge_ab_bc0, translated0, pack16_op, Assembler::AVX_512bit);
      __ vpmaddwd(merged0, merge_ab_bc0, pack32_op, Assembler::AVX_512bit);
      __ vpermb(merged0, pack24bits, merged0, Assembler::AVX_512bit);

      __ evmovdquq(Address(dest, dp), merged0, Assembler::AVX_512bit);

      __ subl(length, 64);
      __ addptr(source, 64);
      __ addptr(dest, 48);

      __ cmpl(length, 64);
      __ jcc(Assembler::greaterEqual, L_process64Loop);

      __ cmpl(length, 0);
      __ jcc(Assembler::lessEqual, L_exit);

      __ BIND(L_finalBit);
      // Now have 1 to 63 bytes left to decode

      // I was going to let Java take care of the final fragment
      // however it will repeatedly call this routine for every 4 bytes
      // of input data, so handle the rest here.
      __ movq(rax, -1);
      __ bzhiq(rax, rax, length);    // Input mask in rax

      __ movl(output_size, length);
      __ shrl(output_size, 2);   // Find (len / 4) * 3 (output length)
      __ lea(output_size, Address(output_size, output_size, Address::times_2, 0));
      // output_size in r13

      // Strip pad characters, if any, and adjust length and mask
      __ cmpb(Address(source, length, Address::times_1, -1), '=');
      __ jcc(Assembler::equal, L_padding);

      __ BIND(L_donePadding);

      // Output size is (64 - output_size), output mask is (all 1s >> output_size).
      __ kmovql(input_mask, rax);
      __ movq(output_mask, -1);
      __ bzhiq(output_mask, output_mask, output_size);

      // Load initial input with all valid base64 characters.  Will be used
      // in merging source bytes to avoid masking when determining if an error occurred.
      __ movl(rax, 0x61616161);
      __ evpbroadcastd(input_initial_valid_b64, rax, Assembler::AVX_512bit);

      // A register containing all invalid base64 decoded values
      __ movl(rax, 0x80808080);
      __ evpbroadcastd(invalid_b64, rax, Assembler::AVX_512bit);

      // input_mask is in k1
      // output_size is in r13
      // output_mask is in r15
      // zmm0 - free
      // zmm1 - 0x00011000
      // zmm2 - 0x01400140
      // zmm3 - errorvec
      // zmm4 - pack vector
      // zmm5 - lookup_lo
      // zmm6 - lookup_hi
      // zmm7 - errorvec
      // zmm8 - 0x61616161
      // zmm9 - 0x80808080

      // Load only the bytes from source, merging into our "fully-valid" register
      __ evmovdqub(input_initial_valid_b64, input_mask, Address(source, start_offset, Address::times_1, 0x0), true, Assembler::AVX_512bit);

      // Decode all bytes within our merged input
      __ evmovdquq(tmp, lookup_lo, Assembler::AVX_512bit);
      __ evpermt2b(tmp, input_initial_valid_b64, lookup_hi, Assembler::AVX_512bit);
      __ vporq(mask, tmp, input_initial_valid_b64, Assembler::AVX_512bit);

      // Check for error.  Compare (decoded | initial) to all invalid.
      // If any bytes have their high-order bit set, then we have an error.
      __ evptestmb(k2, mask, invalid_b64, Assembler::AVX_512bit);
      __ kortestql(k2, k2);

      // If we have an error, use the brute force loop to decode what we can (4-byte chunks).
      __ jcc(Assembler::notZero, L_bruteForce);

      // Shuffle output bytes
      __ vpmaddubsw(tmp, tmp, pack16_op, Assembler::AVX_512bit);
      __ vpmaddwd(tmp, tmp, pack32_op, Assembler::AVX_512bit);

      __ vpermb(tmp, pack24bits, tmp, Assembler::AVX_512bit);
      __ kmovql(k1, output_mask);
      __ evmovdqub(Address(dest, dp), k1, tmp, true, Assembler::AVX_512bit);

      __ addptr(dest, output_size);

      __ BIND(L_exit);
      __ vzeroupper();
      __ pop(rax);             // Get original dest value
      __ subptr(dest, rax);      // Number of bytes converted
      __ movptr(rax, dest);
      __ pop(rbx);
      __ pop(r15);
      __ pop(r14);
      __ pop(r13);
      __ pop(r12);
      __ leave();
      __ ret(0);

      __ BIND(L_loadURL);
      __ evmovdquq(lookup_lo, ExternalAddress(StubRoutines::x86::base64_vbmi_lookup_lo_url_addr()), Assembler::AVX_512bit, r13);
      __ evmovdquq(lookup_hi, ExternalAddress(StubRoutines::x86::base64_vbmi_lookup_hi_url_addr()), Assembler::AVX_512bit, r13);
      __ jmp(L_continue);

      __ BIND(L_padding);
      __ decrementq(output_size, 1);
      __ shrq(rax, 1);

      __ cmpb(Address(source, length, Address::times_1, -2), '=');
      __ jcc(Assembler::notEqual, L_donePadding);

      __ decrementq(output_size, 1);
      __ shrq(rax, 1);
      __ jmp(L_donePadding);

      __ align(32);
      __ BIND(L_bruteForce);
    }   // End of if(avx512_vbmi)

    // Use non-AVX code to decode 4-byte chunks into 3 bytes of output

    // Register state (Linux):
    // r12-15 - saved on stack
    // rdi - src
    // rsi - sp
    // rdx - sl
    // rcx - dst
    // r8 - dp
    // r9 - isURL

    // Register state (Windows):
    // r12-15 - saved on stack
    // rcx - src
    // rdx - sp
    // r8 - sl
    // r9 - dst
    // r12 - dp
    // r10 - isURL

    // Registers (common):
    // length (r14) - bytes in src

    const Register decode_table = r11;
    const Register out_byte_count = rbx;
    const Register byte1 = r13;
    const Register byte2 = r15;
    const Register byte3 = WINDOWS_ONLY(r8) NOT_WINDOWS(rdx);
    const Register byte4 = WINDOWS_ONLY(r10) NOT_WINDOWS(r9);

    __ shrl(length, 2);    // Multiple of 4 bytes only - length is # 4-byte chunks
    __ cmpl(length, 0);
    __ jcc(Assembler::lessEqual, L_exit_no_vzero);

    __ shll(isURL, 8);    // index into decode table based on isURL
    __ lea(decode_table, ExternalAddress(StubRoutines::x86::base64_decoding_table_addr()));
    __ addptr(decode_table, isURL);

    __ jmp(L_bottomLoop);

    __ align(32);
    __ BIND(L_forceLoop);
    __ shll(byte1, 18);
    __ shll(byte2, 12);
    __ shll(byte3, 6);
    __ orl(byte1, byte2);
    __ orl(byte1, byte3);
    __ orl(byte1, byte4);

    __ addptr(source, 4);

    __ movb(Address(dest, dp, Address::times_1, 2), byte1);
    __ shrl(byte1, 8);
    __ movb(Address(dest, dp, Address::times_1, 1), byte1);
    __ shrl(byte1, 8);
    __ movb(Address(dest, dp, Address::times_1, 0), byte1);

    __ addptr(dest, 3);
    __ decrementl(length, 1);
    __ jcc(Assembler::zero, L_exit_no_vzero);

    __ BIND(L_bottomLoop);
    __ load_unsigned_byte(byte1, Address(source, start_offset, Address::times_1, 0x00));
    __ load_unsigned_byte(byte2, Address(source, start_offset, Address::times_1, 0x01));
    __ load_signed_byte(byte1, Address(decode_table, byte1));
    __ load_signed_byte(byte2, Address(decode_table, byte2));
    __ load_unsigned_byte(byte3, Address(source, start_offset, Address::times_1, 0x02));
    __ load_unsigned_byte(byte4, Address(source, start_offset, Address::times_1, 0x03));
    __ load_signed_byte(byte3, Address(decode_table, byte3));
    __ load_signed_byte(byte4, Address(decode_table, byte4));

    __ mov(rax, byte1);
    __ orl(rax, byte2);
    __ orl(rax, byte3);
    __ orl(rax, byte4);
    __ jcc(Assembler::positive, L_forceLoop);

    __ BIND(L_exit_no_vzero);
    __ pop(rax);             // Get original dest value
    __ subptr(dest, rax);      // Number of bytes converted
    __ movptr(rax, dest);
    __ pop(rbx);
    __ pop(r15);
    __ pop(r14);
    __ pop(r13);
    __ pop(r12);
    __ leave();
    __ ret(0);

    return start;
  }


  /**
   *  Arguments:
   *
   * Inputs:
   *   c_rarg0   - int crc
   *   c_rarg1   - byte* buf
   *   c_rarg2   - int length
   *
   * Ouput:
   *       rax   - int crc result
   */
  address generate_updateBytesCRC32() {
    assert(UseCRC32Intrinsics, "need AVX and CLMUL instructions");

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "updateBytesCRC32");

    address start = __ pc();
    // Win64: rcx, rdx, r8, r9 (c_rarg0, c_rarg1, ...)
    // Unix:  rdi, rsi, rdx, rcx, r8, r9 (c_rarg0, c_rarg1, ...)
    // rscratch1: r10
    const Register crc   = c_rarg0;  // crc
    const Register buf   = c_rarg1;  // source java byte array address
    const Register len   = c_rarg2;  // length
    const Register table = c_rarg3;  // crc_table address (reuse register)
    const Register tmp1   = r11;
    const Register tmp2   = r10;
    assert_different_registers(crc, buf, len, table, tmp1, tmp2, rax);

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    if (VM_Version::supports_sse4_1() && VM_Version::supports_avx512_vpclmulqdq() &&
        VM_Version::supports_avx512bw() &&
        VM_Version::supports_avx512vl()) {
      __ kernel_crc32_avx512(crc, buf, len, table, tmp1, tmp2);
    } else {
      __ kernel_crc32(crc, buf, len, table, tmp1);
    }

    __ movl(rax, crc);
    __ vzeroupper();
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  /**
  *  Arguments:
  *
  * Inputs:
  *   c_rarg0   - int crc
  *   c_rarg1   - byte* buf
  *   c_rarg2   - long length
  *   c_rarg3   - table_start - optional (present only when doing a library_call,
  *              not used by x86 algorithm)
  *
  * Ouput:
  *       rax   - int crc result
  */
  address generate_updateBytesCRC32C(bool is_pclmulqdq_supported) {
      assert(UseCRC32CIntrinsics, "need SSE4_2");
      __ align(CodeEntryAlignment);
      StubCodeMark mark(this, "StubRoutines", "updateBytesCRC32C");
      address start = __ pc();
      //reg.arg        int#0        int#1        int#2        int#3        int#4        int#5        float regs
      //Windows        RCX          RDX          R8           R9           none         none         XMM0..XMM3
      //Lin / Sol      RDI          RSI          RDX          RCX          R8           R9           XMM0..XMM7
      const Register crc = c_rarg0;  // crc
      const Register buf = c_rarg1;  // source java byte array address
      const Register len = c_rarg2;  // length
      const Register a = rax;
      const Register j = r9;
      const Register k = r10;
      const Register l = r11;
#ifdef _WIN64
      const Register y = rdi;
      const Register z = rsi;
#else
      const Register y = rcx;
      const Register z = r8;
#endif
      assert_different_registers(crc, buf, len, a, j, k, l, y, z);

      BLOCK_COMMENT("Entry:");
      __ enter(); // required for proper stackwalking of RuntimeStub frame
#ifdef _WIN64
      __ push(y);
      __ push(z);
#endif
      __ crc32c_ipl_alg2_alt2(crc, buf, len,
                              a, j, k,
                              l, y, z,
                              c_farg0, c_farg1, c_farg2,
                              is_pclmulqdq_supported);
      __ movl(rax, crc);
#ifdef _WIN64
      __ pop(z);
      __ pop(y);
#endif
      __ vzeroupper();
      __ leave(); // required for proper stackwalking of RuntimeStub frame
      __ ret(0);

      return start;
  }


  /***
   *  Arguments:
   *
   *  Inputs:
   *   c_rarg0   - int   adler
   *   c_rarg1   - byte* buff
   *   c_rarg2   - int   len
   *
   * Output:
   *   rax   - int adler result
   */

  address generate_updateBytesAdler32() {
      assert(UseAdler32Intrinsics, "need AVX2");

      __ align(CodeEntryAlignment);
      StubCodeMark mark(this, "StubRoutines", "updateBytesAdler32");

      address start = __ pc();

      const Register data = r9;
      const Register size = r10;

      const XMMRegister yshuf0 = xmm6;
      const XMMRegister yshuf1 = xmm7;
      assert_different_registers(c_rarg0, c_rarg1, c_rarg2, data, size);

      BLOCK_COMMENT("Entry:");
      __ enter(); // required for proper stackwalking of RuntimeStub frame

      __ vmovdqu(yshuf0, ExternalAddress((address) StubRoutines::x86::_adler32_shuf0_table), r9);
      __ vmovdqu(yshuf1, ExternalAddress((address) StubRoutines::x86::_adler32_shuf1_table), r9);
      __ movptr(data, c_rarg1); //data
      __ movl(size, c_rarg2); //length
      __ updateBytesAdler32(c_rarg0, data, size, yshuf0, yshuf1, ExternalAddress((address) StubRoutines::x86::_adler32_ascale_table));
      __ leave();
      __ ret(0);
      return start;
  }

  /**
   *  Arguments:
   *
   *  Input:
   *    c_rarg0   - x address
   *    c_rarg1   - x length
   *    c_rarg2   - y address
   *    c_rarg3   - y length
   * not Win64
   *    c_rarg4   - z address
   *    c_rarg5   - z length
   * Win64
   *    rsp+40    - z address
   *    rsp+48    - z length
   */
  address generate_multiplyToLen() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "multiplyToLen");

    address start = __ pc();
    // Win64: rcx, rdx, r8, r9 (c_rarg0, c_rarg1, ...)
    // Unix:  rdi, rsi, rdx, rcx, r8, r9 (c_rarg0, c_rarg1, ...)
    const Register x     = rdi;
    const Register xlen  = rax;
    const Register y     = rsi;
    const Register ylen  = rcx;
    const Register z     = r8;
    const Register zlen  = r11;

    // Next registers will be saved on stack in multiply_to_len().
    const Register tmp1  = r12;
    const Register tmp2  = r13;
    const Register tmp3  = r14;
    const Register tmp4  = r15;
    const Register tmp5  = rbx;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifndef _WIN64
    __ movptr(zlen, r9); // Save r9 in r11 - zlen
#endif
    setup_arg_regs(4); // x => rdi, xlen => rsi, y => rdx
                       // ylen => rcx, z => r8, zlen => r11
                       // r9 and r10 may be used to save non-volatile registers
#ifdef _WIN64
    // last 2 arguments (#4, #5) are on stack on Win64
    __ movptr(z, Address(rsp, 6 * wordSize));
    __ movptr(zlen, Address(rsp, 7 * wordSize));
#endif

    __ movptr(xlen, rsi);
    __ movptr(y,    rdx);
    __ multiply_to_len(x, xlen, y, ylen, z, zlen, tmp1, tmp2, tmp3, tmp4, tmp5);

    restore_arg_regs();

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  /**
  *  Arguments:
  *
  *  Input:
  *    c_rarg0   - obja     address
  *    c_rarg1   - objb     address
  *    c_rarg3   - length   length
  *    c_rarg4   - scale    log2_array_indxscale
  *
  *  Output:
  *        rax   - int >= mismatched index, < 0 bitwise complement of tail
  */
  address generate_vectorizedMismatch() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "vectorizedMismatch");
    address start = __ pc();

    BLOCK_COMMENT("Entry:");
    __ enter();

#ifdef _WIN64  // Win64: rcx, rdx, r8, r9 (c_rarg0, c_rarg1, ...)
    const Register scale = c_rarg0;  //rcx, will exchange with r9
    const Register objb = c_rarg1;   //rdx
    const Register length = c_rarg2; //r8
    const Register obja = c_rarg3;   //r9
    __ xchgq(obja, scale);  //now obja and scale contains the correct contents

    const Register tmp1 = r10;
    const Register tmp2 = r11;
#endif
#ifndef _WIN64 // Unix:  rdi, rsi, rdx, rcx, r8, r9 (c_rarg0, c_rarg1, ...)
    const Register obja = c_rarg0;   //U:rdi
    const Register objb = c_rarg1;   //U:rsi
    const Register length = c_rarg2; //U:rdx
    const Register scale = c_rarg3;  //U:rcx
    const Register tmp1 = r8;
    const Register tmp2 = r9;
#endif
    const Register result = rax; //return value
    const XMMRegister vec0 = xmm0;
    const XMMRegister vec1 = xmm1;
    const XMMRegister vec2 = xmm2;

    __ vectorized_mismatch(obja, objb, length, scale, result, tmp1, tmp2, vec0, vec1, vec2);

    __ vzeroupper();
    __ leave();
    __ ret(0);

    return start;
  }

/**
   *  Arguments:
   *
  //  Input:
  //    c_rarg0   - x address
  //    c_rarg1   - x length
  //    c_rarg2   - z address
  //    c_rarg3   - z lenth
   *
   */
  address generate_squareToLen() {

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "squareToLen");

    address start = __ pc();
    // Win64: rcx, rdx, r8, r9 (c_rarg0, c_rarg1, ...)
    // Unix:  rdi, rsi, rdx, rcx (c_rarg0, c_rarg1, ...)
    const Register x      = rdi;
    const Register len    = rsi;
    const Register z      = r8;
    const Register zlen   = rcx;

   const Register tmp1      = r12;
   const Register tmp2      = r13;
   const Register tmp3      = r14;
   const Register tmp4      = r15;
   const Register tmp5      = rbx;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    setup_arg_regs(4); // x => rdi, len => rsi, z => rdx
                       // zlen => rcx
                       // r9 and r10 may be used to save non-volatile registers
    __ movptr(r8, rdx);
    __ square_to_len(x, len, z, zlen, tmp1, tmp2, tmp3, tmp4, tmp5, rdx, rax);

    restore_arg_regs();

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  address generate_method_entry_barrier() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "nmethod_entry_barrier");

    Label deoptimize_label;

    address start = __ pc();

    __ push(-1); // cookie, this is used for writing the new rsp when deoptimizing

    BLOCK_COMMENT("Entry:");
    __ enter(); // save rbp

    // save c_rarg0, because we want to use that value.
    // We could do without it but then we depend on the number of slots used by pusha
    __ push(c_rarg0);

    __ lea(c_rarg0, Address(rsp, wordSize * 3)); // 1 for cookie, 1 for rbp, 1 for c_rarg0 - this should be the return address

    __ pusha();

    // The method may have floats as arguments, and we must spill them before calling
    // the VM runtime.
    assert(Argument::n_float_register_parameters_j == 8, "Assumption");
    const int xmm_size = wordSize * 2;
    const int xmm_spill_size = xmm_size * Argument::n_float_register_parameters_j;
    __ subptr(rsp, xmm_spill_size);
    __ movdqu(Address(rsp, xmm_size * 7), xmm7);
    __ movdqu(Address(rsp, xmm_size * 6), xmm6);
    __ movdqu(Address(rsp, xmm_size * 5), xmm5);
    __ movdqu(Address(rsp, xmm_size * 4), xmm4);
    __ movdqu(Address(rsp, xmm_size * 3), xmm3);
    __ movdqu(Address(rsp, xmm_size * 2), xmm2);
    __ movdqu(Address(rsp, xmm_size * 1), xmm1);
    __ movdqu(Address(rsp, xmm_size * 0), xmm0);

    __ call_VM_leaf(CAST_FROM_FN_PTR(address, static_cast<int (*)(address*)>(BarrierSetNMethod::nmethod_stub_entry_barrier)), 1);

    __ movdqu(xmm0, Address(rsp, xmm_size * 0));
    __ movdqu(xmm1, Address(rsp, xmm_size * 1));
    __ movdqu(xmm2, Address(rsp, xmm_size * 2));
    __ movdqu(xmm3, Address(rsp, xmm_size * 3));
    __ movdqu(xmm4, Address(rsp, xmm_size * 4));
    __ movdqu(xmm5, Address(rsp, xmm_size * 5));
    __ movdqu(xmm6, Address(rsp, xmm_size * 6));
    __ movdqu(xmm7, Address(rsp, xmm_size * 7));
    __ addptr(rsp, xmm_spill_size);

    __ cmpl(rax, 1); // 1 means deoptimize
    __ jcc(Assembler::equal, deoptimize_label);

    __ popa();
    __ pop(c_rarg0);

    __ leave();

    __ addptr(rsp, 1 * wordSize); // cookie
    __ ret(0);


    __ BIND(deoptimize_label);

    __ popa();
    __ pop(c_rarg0);

    __ leave();

    // this can be taken out, but is good for verification purposes. getting a SIGSEGV
    // here while still having a correct stack is valuable
    __ testptr(rsp, Address(rsp, 0));

    __ movptr(rsp, Address(rsp, 0)); // new rsp was written in the barrier
    __ jmp(Address(rsp, -1 * wordSize)); // jmp target should be callers verified_entry_point

    return start;
  }

   /**
   *  Arguments:
   *
   *  Input:
   *    c_rarg0   - out address
   *    c_rarg1   - in address
   *    c_rarg2   - offset
   *    c_rarg3   - len
   * not Win64
   *    c_rarg4   - k
   * Win64
   *    rsp+40    - k
   */
  address generate_mulAdd() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "mulAdd");

    address start = __ pc();
    // Win64: rcx, rdx, r8, r9 (c_rarg0, c_rarg1, ...)
    // Unix:  rdi, rsi, rdx, rcx, r8, r9 (c_rarg0, c_rarg1, ...)
    const Register out     = rdi;
    const Register in      = rsi;
    const Register offset  = r11;
    const Register len     = rcx;
    const Register k       = r8;

    // Next registers will be saved on stack in mul_add().
    const Register tmp1  = r12;
    const Register tmp2  = r13;
    const Register tmp3  = r14;
    const Register tmp4  = r15;
    const Register tmp5  = rbx;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    setup_arg_regs(4); // out => rdi, in => rsi, offset => rdx
                       // len => rcx, k => r8
                       // r9 and r10 may be used to save non-volatile registers
#ifdef _WIN64
    // last argument is on stack on Win64
    __ movl(k, Address(rsp, 6 * wordSize));
#endif
    __ movptr(r11, rdx);  // move offset in rdx to offset(r11)
    __ mul_add(out, in, offset, len, k, tmp1, tmp2, tmp3, tmp4, tmp5, rdx, rax);

    restore_arg_regs();

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;
  }

  address generate_bigIntegerRightShift() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "bigIntegerRightShiftWorker");

    address start = __ pc();
    Label Shift512Loop, ShiftTwo, ShiftTwoLoop, ShiftOne, Exit;
    // For Unix, the arguments are as follows: rdi, rsi, rdx, rcx, r8.
    const Register newArr = rdi;
    const Register oldArr = rsi;
    const Register newIdx = rdx;
    const Register shiftCount = rcx;  // It was intentional to have shiftCount in rcx since it is used implicitly for shift.
    const Register totalNumIter = r8;

    // For windows, we use r9 and r10 as temps to save rdi and rsi. Thus we cannot allocate them for our temps.
    // For everything else, we prefer using r9 and r10 since we do not have to save them before use.
    const Register tmp1 = r11;                    // Caller save.
    const Register tmp2 = rax;                    // Caller save.
    const Register tmp3 = WINDOWS_ONLY(r12) NOT_WINDOWS(r9);   // Windows: Callee save. Linux: Caller save.
    const Register tmp4 = WINDOWS_ONLY(r13) NOT_WINDOWS(r10);  // Windows: Callee save. Linux: Caller save.
    const Register tmp5 = r14;                    // Callee save.
    const Register tmp6 = r15;

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WINDOWS
    setup_arg_regs(4);
    // For windows, since last argument is on stack, we need to move it to the appropriate register.
    __ movl(totalNumIter, Address(rsp, 6 * wordSize));
    // Save callee save registers.
    __ push(tmp3);
    __ push(tmp4);
#endif
    __ push(tmp5);

    // Rename temps used throughout the code.
    const Register idx = tmp1;
    const Register nIdx = tmp2;

    __ xorl(idx, idx);

    // Start right shift from end of the array.
    // For example, if #iteration = 4 and newIdx = 1
    // then dest[4] = src[4] >> shiftCount  | src[3] <<< (shiftCount - 32)
    // if #iteration = 4 and newIdx = 0
    // then dest[3] = src[4] >> shiftCount  | src[3] <<< (shiftCount - 32)
    __ movl(idx, totalNumIter);
    __ movl(nIdx, idx);
    __ addl(nIdx, newIdx);

    // If vectorization is enabled, check if the number of iterations is at least 64
    // If not, then go to ShifTwo processing 2 iterations
    if (VM_Version::supports_avx512_vbmi2()) {
      __ cmpptr(totalNumIter, (AVX3Threshold/64));
      __ jcc(Assembler::less, ShiftTwo);

      if (AVX3Threshold < 16 * 64) {
        __ cmpl(totalNumIter, 16);
        __ jcc(Assembler::less, ShiftTwo);
      }
      __ evpbroadcastd(x0, shiftCount, Assembler::AVX_512bit);
      __ subl(idx, 16);
      __ subl(nIdx, 16);
      __ BIND(Shift512Loop);
      __ evmovdqul(x2, Address(oldArr, idx, Address::times_4, 4), Assembler::AVX_512bit);
      __ evmovdqul(x1, Address(oldArr, idx, Address::times_4), Assembler::AVX_512bit);
      __ vpshrdvd(x2, x1, x0, Assembler::AVX_512bit);
      __ evmovdqul(Address(newArr, nIdx, Address::times_4), x2, Assembler::AVX_512bit);
      __ subl(nIdx, 16);
      __ subl(idx, 16);
      __ jcc(Assembler::greaterEqual, Shift512Loop);
      __ addl(idx, 16);
      __ addl(nIdx, 16);
    }
    __ BIND(ShiftTwo);
    __ cmpl(idx, 2);
    __ jcc(Assembler::less, ShiftOne);
    __ subl(idx, 2);
    __ subl(nIdx, 2);
    __ BIND(ShiftTwoLoop);
    __ movl(tmp5, Address(oldArr, idx, Address::times_4, 8));
    __ movl(tmp4, Address(oldArr, idx, Address::times_4, 4));
    __ movl(tmp3, Address(oldArr, idx, Address::times_4));
    __ shrdl(tmp5, tmp4);
    __ shrdl(tmp4, tmp3);
    __ movl(Address(newArr, nIdx, Address::times_4, 4), tmp5);
    __ movl(Address(newArr, nIdx, Address::times_4), tmp4);
    __ subl(nIdx, 2);
    __ subl(idx, 2);
    __ jcc(Assembler::greaterEqual, ShiftTwoLoop);
    __ addl(idx, 2);
    __ addl(nIdx, 2);

    // Do the last iteration
    __ BIND(ShiftOne);
    __ cmpl(idx, 1);
    __ jcc(Assembler::less, Exit);
    __ subl(idx, 1);
    __ subl(nIdx, 1);
    __ movl(tmp4, Address(oldArr, idx, Address::times_4, 4));
    __ movl(tmp3, Address(oldArr, idx, Address::times_4));
    __ shrdl(tmp4, tmp3);
    __ movl(Address(newArr, nIdx, Address::times_4), tmp4);
    __ BIND(Exit);
    // Restore callee save registers.
    __ pop(tmp5);
#ifdef _WINDOWS
    __ pop(tmp4);
    __ pop(tmp3);
    restore_arg_regs();
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

   /**
   *  Arguments:
   *
   *  Input:
   *    c_rarg0   - newArr address
   *    c_rarg1   - oldArr address
   *    c_rarg2   - newIdx
   *    c_rarg3   - shiftCount
   * not Win64
   *    c_rarg4   - numIter
   * Win64
   *    rsp40    - numIter
   */
  address generate_bigIntegerLeftShift() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this,  "StubRoutines", "bigIntegerLeftShiftWorker");
    address start = __ pc();
    Label Shift512Loop, ShiftTwo, ShiftTwoLoop, ShiftOne, Exit;
    // For Unix, the arguments are as follows: rdi, rsi, rdx, rcx, r8.
    const Register newArr = rdi;
    const Register oldArr = rsi;
    const Register newIdx = rdx;
    const Register shiftCount = rcx;  // It was intentional to have shiftCount in rcx since it is used implicitly for shift.
    const Register totalNumIter = r8;
    // For windows, we use r9 and r10 as temps to save rdi and rsi. Thus we cannot allocate them for our temps.
    // For everything else, we prefer using r9 and r10 since we do not have to save them before use.
    const Register tmp1 = r11;                    // Caller save.
    const Register tmp2 = rax;                    // Caller save.
    const Register tmp3 = WINDOWS_ONLY(r12) NOT_WINDOWS(r9);   // Windows: Callee save. Linux: Caller save.
    const Register tmp4 = WINDOWS_ONLY(r13) NOT_WINDOWS(r10);  // Windows: Callee save. Linux: Caller save.
    const Register tmp5 = r14;                    // Callee save.

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WINDOWS
    setup_arg_regs(4);
    // For windows, since last argument is on stack, we need to move it to the appropriate register.
    __ movl(totalNumIter, Address(rsp, 6 * wordSize));
    // Save callee save registers.
    __ push(tmp3);
    __ push(tmp4);
#endif
    __ push(tmp5);

    // Rename temps used throughout the code
    const Register idx = tmp1;
    const Register numIterTmp = tmp2;

    // Start idx from zero.
    __ xorl(idx, idx);
    // Compute interior pointer for new array. We do this so that we can use same index for both old and new arrays.
    __ lea(newArr, Address(newArr, newIdx, Address::times_4));
    __ movl(numIterTmp, totalNumIter);

    // If vectorization is enabled, check if the number of iterations is at least 64
    // If not, then go to ShiftTwo shifting two numbers at a time
    if (VM_Version::supports_avx512_vbmi2()) {
      __ cmpl(totalNumIter, (AVX3Threshold/64));
      __ jcc(Assembler::less, ShiftTwo);

      if (AVX3Threshold < 16 * 64) {
        __ cmpl(totalNumIter, 16);
        __ jcc(Assembler::less, ShiftTwo);
      }
      __ evpbroadcastd(x0, shiftCount, Assembler::AVX_512bit);
      __ subl(numIterTmp, 16);
      __ BIND(Shift512Loop);
      __ evmovdqul(x1, Address(oldArr, idx, Address::times_4), Assembler::AVX_512bit);
      __ evmovdqul(x2, Address(oldArr, idx, Address::times_4, 0x4), Assembler::AVX_512bit);
      __ vpshldvd(x1, x2, x0, Assembler::AVX_512bit);
      __ evmovdqul(Address(newArr, idx, Address::times_4), x1, Assembler::AVX_512bit);
      __ addl(idx, 16);
      __ subl(numIterTmp, 16);
      __ jcc(Assembler::greaterEqual, Shift512Loop);
      __ addl(numIterTmp, 16);
    }
    __ BIND(ShiftTwo);
    __ cmpl(totalNumIter, 1);
    __ jcc(Assembler::less, Exit);
    __ movl(tmp3, Address(oldArr, idx, Address::times_4));
    __ subl(numIterTmp, 2);
    __ jcc(Assembler::less, ShiftOne);

    __ BIND(ShiftTwoLoop);
    __ movl(tmp4, Address(oldArr, idx, Address::times_4, 0x4));
    __ movl(tmp5, Address(oldArr, idx, Address::times_4, 0x8));
    __ shldl(tmp3, tmp4);
    __ shldl(tmp4, tmp5);
    __ movl(Address(newArr, idx, Address::times_4), tmp3);
    __ movl(Address(newArr, idx, Address::times_4, 0x4), tmp4);
    __ movl(tmp3, tmp5);
    __ addl(idx, 2);
    __ subl(numIterTmp, 2);
    __ jcc(Assembler::greaterEqual, ShiftTwoLoop);

    // Do the last iteration
    __ BIND(ShiftOne);
    __ addl(numIterTmp, 2);
    __ cmpl(numIterTmp, 1);
    __ jcc(Assembler::less, Exit);
    __ movl(tmp4, Address(oldArr, idx, Address::times_4, 0x4));
    __ shldl(tmp3, tmp4);
    __ movl(Address(newArr, idx, Address::times_4), tmp3);

    __ BIND(Exit);
    // Restore callee save registers.
    __ pop(tmp5);
#ifdef _WINDOWS
    __ pop(tmp4);
    __ pop(tmp3);
    restore_arg_regs();
#endif
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);
    return start;
  }

  address generate_libmExp() {
    StubCodeMark mark(this, "StubRoutines", "libmExp");

    address start = __ pc();

    const XMMRegister x0  = xmm0;
    const XMMRegister x1  = xmm1;
    const XMMRegister x2  = xmm2;
    const XMMRegister x3  = xmm3;

    const XMMRegister x4  = xmm4;
    const XMMRegister x5  = xmm5;
    const XMMRegister x6  = xmm6;
    const XMMRegister x7  = xmm7;

    const Register tmp   = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ fast_exp(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmLog() {
    StubCodeMark mark(this, "StubRoutines", "libmLog");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp1 = r11;
    const Register tmp2 = r8;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ fast_log(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp1, tmp2);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmLog10() {
    StubCodeMark mark(this, "StubRoutines", "libmLog10");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ fast_log10(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmPow() {
    StubCodeMark mark(this, "StubRoutines", "libmPow");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp1 = r8;
    const Register tmp2 = r9;
    const Register tmp3 = r10;
    const Register tmp4 = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ fast_pow(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp1, tmp2, tmp3, tmp4);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmSin() {
    StubCodeMark mark(this, "StubRoutines", "libmSin");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp1 = r8;
    const Register tmp2 = r9;
    const Register tmp3 = r10;
    const Register tmp4 = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    __ push(rsi);
    __ push(rdi);
#endif
    __ fast_sin(x0, x1, x2, x3, x4, x5, x6, x7, rax, rbx, rcx, rdx, tmp1, tmp2, tmp3, tmp4);

#ifdef _WIN64
    __ pop(rdi);
    __ pop(rsi);
#endif

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmCos() {
    StubCodeMark mark(this, "StubRoutines", "libmCos");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp1 = r8;
    const Register tmp2 = r9;
    const Register tmp3 = r10;
    const Register tmp4 = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    __ push(rsi);
    __ push(rdi);
#endif
    __ fast_cos(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp1, tmp2, tmp3, tmp4);

#ifdef _WIN64
    __ pop(rdi);
    __ pop(rsi);
#endif

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

  address generate_libmTan() {
    StubCodeMark mark(this, "StubRoutines", "libmTan");

    address start = __ pc();

    const XMMRegister x0 = xmm0;
    const XMMRegister x1 = xmm1;
    const XMMRegister x2 = xmm2;
    const XMMRegister x3 = xmm3;

    const XMMRegister x4 = xmm4;
    const XMMRegister x5 = xmm5;
    const XMMRegister x6 = xmm6;
    const XMMRegister x7 = xmm7;

    const Register tmp1 = r8;
    const Register tmp2 = r9;
    const Register tmp3 = r10;
    const Register tmp4 = r11;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

#ifdef _WIN64
    __ push(rsi);
    __ push(rdi);
#endif
    __ fast_tan(x0, x1, x2, x3, x4, x5, x6, x7, rax, rcx, rdx, tmp1, tmp2, tmp3, tmp4);

#ifdef _WIN64
    __ pop(rdi);
    __ pop(rsi);
#endif

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(0);

    return start;

  }

#undef __
#define __ masm->

  // Continuation point for throwing of implicit exceptions that are
  // not handled in the current activation. Fabricates an exception
  // oop and initiates normal exception dispatching in this
  // frame. Since we need to preserve callee-saved values (currently
  // only for C2, but done for C1 as well) we need a callee-saved oop
  // map and therefore have to make these stubs into RuntimeStubs
  // rather than BufferBlobs.  If the compiler needs all registers to
  // be preserved between the fault point and the exception handler
  // then it must assume responsibility for that in
  // AbstractCompiler::continuation_for_implicit_null_exception or
  // continuation_for_implicit_division_by_zero_exception. All other
  // implicit exceptions (e.g., NullPointerException or
  // AbstractMethodError on entry) are either at call sites or
  // otherwise assume that stack unwinding will be initiated, so
  // caller saved registers were assumed volatile in the compiler.
  address generate_throw_exception(const char* name,
                                   address runtime_entry,
                                   Register arg1 = noreg,
                                   Register arg2 = noreg) {
    // Information about frame layout at time of blocking runtime call.
    // Note that we only have to preserve callee-saved registers since
    // the compilers are responsible for supplying a continuation point
    // if they expect all registers to be preserved.
    enum layout {
      rbp_off = frame::arg_reg_save_area_bytes/BytesPerInt,
      rbp_off2,
      return_off,
      return_off2,
      framesize // inclusive of return address
    };

    int insts_size = 512;
    int locs_size  = 64;

    CodeBuffer code(name, insts_size, locs_size);
    OopMapSet* oop_maps  = new OopMapSet();
    MacroAssembler* masm = new MacroAssembler(&code);

    address start = __ pc();

    // This is an inlined and slightly modified version of call_VM
    // which has the ability to fetch the return PC out of
    // thread-local storage and also sets up last_Java_sp slightly
    // differently than the real call_VM

    __ enter(); // required for proper stackwalking of RuntimeStub frame

    assert(is_even(framesize/2), "sp not 16-byte aligned");

    // return address and rbp are already in place
    __ subptr(rsp, (framesize-4) << LogBytesPerInt); // prolog

    int frame_complete = __ pc() - start;

    // Set up last_Java_sp and last_Java_fp
    address the_pc = __ pc();
    __ set_last_Java_frame(rsp, rbp, the_pc);
    __ andptr(rsp, -(StackAlignmentInBytes));    // Align stack

    // Call runtime
    if (arg1 != noreg) {
      assert(arg2 != c_rarg1, "clobbered");
      __ movptr(c_rarg1, arg1);
    }
    if (arg2 != noreg) {
      __ movptr(c_rarg2, arg2);
    }
    __ movptr(c_rarg0, r15_thread);
    BLOCK_COMMENT("call runtime_entry");
    __ call(RuntimeAddress(runtime_entry));

    // Generate oop map
    OopMap* map = new OopMap(framesize, 0);

    oop_maps->add_gc_map(the_pc - start, map);

    __ reset_last_Java_frame(true);

    __ leave(); // required for proper stackwalking of RuntimeStub frame

    // check for pending exceptions
#ifdef ASSERT
    Label L;
    __ cmpptr(Address(r15_thread, Thread::pending_exception_offset()),
            (int32_t) NULL_WORD);
    __ jcc(Assembler::notEqual, L);
    __ should_not_reach_here();
    __ bind(L);
#endif // ASSERT
    __ jump(RuntimeAddress(StubRoutines::forward_exception_entry()));


    // codeBlob framesize is in words (not VMRegImpl::slot_size)
    RuntimeStub* stub =
      RuntimeStub::new_runtime_stub(name,
                                    &code,
                                    frame_complete,
                                    (framesize >> (LogBytesPerWord - LogBytesPerInt)),
                                    oop_maps, false);
    return stub->entry_point();
  }

  void create_control_words() {
    // Round to nearest, 64-bit mode, exceptions masked
    StubRoutines::x86::_mxcsr_std = 0x1F80;
  }

  // Initialization
  void generate_initial() {
    // Generates all stubs and initializes the entry points

    // This platform-specific settings are needed by generate_call_stub()
    create_control_words();

    // entry points that exist in all platforms Note: This is code
    // that could be shared among different platforms - however the
    // benefit seems to be smaller than the disadvantage of having a
    // much more complicated generator structure. See also comment in
    // stubRoutines.hpp.

    StubRoutines::_forward_exception_entry = generate_forward_exception();

    StubRoutines::_call_stub_entry =
      generate_call_stub(StubRoutines::_call_stub_return_address);

    // is referenced by megamorphic call
    StubRoutines::_catch_exception_entry = generate_catch_exception();

    // atomic calls
    StubRoutines::_fence_entry                = generate_orderaccess_fence();

    // platform dependent
    StubRoutines::x86::_get_previous_sp_entry = generate_get_previous_sp();

    StubRoutines::x86::_verify_mxcsr_entry    = generate_verify_mxcsr();

    StubRoutines::x86::_f2i_fixup             = generate_f2i_fixup();
    StubRoutines::x86::_f2l_fixup             = generate_f2l_fixup();
    StubRoutines::x86::_d2i_fixup             = generate_d2i_fixup();
    StubRoutines::x86::_d2l_fixup             = generate_d2l_fixup();

    StubRoutines::x86::_float_sign_mask       = generate_fp_mask("float_sign_mask",  0x7FFFFFFF7FFFFFFF);
    StubRoutines::x86::_float_sign_flip       = generate_fp_mask("float_sign_flip",  0x8000000080000000);
    StubRoutines::x86::_double_sign_mask      = generate_fp_mask("double_sign_mask", 0x7FFFFFFFFFFFFFFF);
    StubRoutines::x86::_double_sign_flip      = generate_fp_mask("double_sign_flip", 0x8000000000000000);

    // Build this early so it's available for the interpreter.
    StubRoutines::_throw_StackOverflowError_entry =
      generate_throw_exception("StackOverflowError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::
                                                throw_StackOverflowError));
    StubRoutines::_throw_delayed_StackOverflowError_entry =
      generate_throw_exception("delayed StackOverflowError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::
                                                throw_delayed_StackOverflowError));
    if (UseCRC32Intrinsics) {
      // set table address before stub generation which use it
      StubRoutines::_crc_table_adr = (address)StubRoutines::x86::_crc_table;
      StubRoutines::_updateBytesCRC32 = generate_updateBytesCRC32();
    }

    if (UseCRC32CIntrinsics) {
      bool supports_clmul = VM_Version::supports_clmul();
      StubRoutines::x86::generate_CRC32C_table(supports_clmul);
      StubRoutines::_crc32c_table_addr = (address)StubRoutines::x86::_crc32c_table;
      StubRoutines::_updateBytesCRC32C = generate_updateBytesCRC32C(supports_clmul);
    }

    if (UseAdler32Intrinsics) {
       StubRoutines::_updateBytesAdler32 = generate_updateBytesAdler32();
    }

    if (UseLibmIntrinsic && InlineIntrinsics) {
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dsin) ||
          vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dcos) ||
          vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dtan)) {
        StubRoutines::x86::_ONEHALF_adr = (address)StubRoutines::x86::_ONEHALF;
        StubRoutines::x86::_P_2_adr = (address)StubRoutines::x86::_P_2;
        StubRoutines::x86::_SC_4_adr = (address)StubRoutines::x86::_SC_4;
        StubRoutines::x86::_Ctable_adr = (address)StubRoutines::x86::_Ctable;
        StubRoutines::x86::_SC_2_adr = (address)StubRoutines::x86::_SC_2;
        StubRoutines::x86::_SC_3_adr = (address)StubRoutines::x86::_SC_3;
        StubRoutines::x86::_SC_1_adr = (address)StubRoutines::x86::_SC_1;
        StubRoutines::x86::_PI_INV_TABLE_adr = (address)StubRoutines::x86::_PI_INV_TABLE;
        StubRoutines::x86::_PI_4_adr = (address)StubRoutines::x86::_PI_4;
        StubRoutines::x86::_PI32INV_adr = (address)StubRoutines::x86::_PI32INV;
        StubRoutines::x86::_SIGN_MASK_adr = (address)StubRoutines::x86::_SIGN_MASK;
        StubRoutines::x86::_P_1_adr = (address)StubRoutines::x86::_P_1;
        StubRoutines::x86::_P_3_adr = (address)StubRoutines::x86::_P_3;
        StubRoutines::x86::_NEG_ZERO_adr = (address)StubRoutines::x86::_NEG_ZERO;
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dexp)) {
        StubRoutines::_dexp = generate_libmExp();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dlog)) {
        StubRoutines::_dlog = generate_libmLog();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dlog10)) {
        StubRoutines::_dlog10 = generate_libmLog10();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dpow)) {
        StubRoutines::_dpow = generate_libmPow();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dsin)) {
        StubRoutines::_dsin = generate_libmSin();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dcos)) {
        StubRoutines::_dcos = generate_libmCos();
      }
      if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dtan)) {
        StubRoutines::_dtan = generate_libmTan();
      }
    }

    // Safefetch stubs.
    generate_safefetch("SafeFetch32", sizeof(int),     &StubRoutines::_safefetch32_entry,
                                                       &StubRoutines::_safefetch32_fault_pc,
                                                       &StubRoutines::_safefetch32_continuation_pc);
    generate_safefetch("SafeFetchN", sizeof(intptr_t), &StubRoutines::_safefetchN_entry,
                                                       &StubRoutines::_safefetchN_fault_pc,
                                                       &StubRoutines::_safefetchN_continuation_pc);
  }

  void generate_all() {
    // Generates all stubs and initializes the entry points

    // These entry points require SharedInfo::stack0 to be set up in
    // non-core builds and need to be relocatable, so they each
    // fabricate a RuntimeStub internally.
    StubRoutines::_throw_AbstractMethodError_entry =
      generate_throw_exception("AbstractMethodError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::
                                                throw_AbstractMethodError));

    StubRoutines::_throw_IncompatibleClassChangeError_entry =
      generate_throw_exception("IncompatibleClassChangeError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::
                                                throw_IncompatibleClassChangeError));

    StubRoutines::_throw_NullPointerException_at_call_entry =
      generate_throw_exception("NullPointerException at call throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::
                                                throw_NullPointerException_at_call));

    // entry points that are platform specific
    StubRoutines::x86::_vector_float_sign_mask = generate_vector_mask("vector_float_sign_mask", 0x7FFFFFFF7FFFFFFF);
    StubRoutines::x86::_vector_float_sign_flip = generate_vector_mask("vector_float_sign_flip", 0x8000000080000000);
    StubRoutines::x86::_vector_double_sign_mask = generate_vector_mask("vector_double_sign_mask", 0x7FFFFFFFFFFFFFFF);
    StubRoutines::x86::_vector_double_sign_flip = generate_vector_mask("vector_double_sign_flip", 0x8000000000000000);
    StubRoutines::x86::_vector_all_bits_set = generate_vector_mask("vector_all_bits_set", 0xFFFFFFFFFFFFFFFF);
    StubRoutines::x86::_vector_short_to_byte_mask = generate_vector_mask("vector_short_to_byte_mask", 0x00ff00ff00ff00ff);
    StubRoutines::x86::_vector_byte_perm_mask = generate_vector_byte_perm_mask("vector_byte_perm_mask");
    StubRoutines::x86::_vector_int_to_byte_mask = generate_vector_mask("vector_int_to_byte_mask", 0x000000ff000000ff);
    StubRoutines::x86::_vector_int_to_short_mask = generate_vector_mask("vector_int_to_short_mask", 0x0000ffff0000ffff);
    StubRoutines::x86::_vector_32_bit_mask = generate_vector_custom_i32("vector_32_bit_mask", Assembler::AVX_512bit,
                                                                        0xFFFFFFFF, 0, 0, 0);
    StubRoutines::x86::_vector_64_bit_mask = generate_vector_custom_i32("vector_64_bit_mask", Assembler::AVX_512bit,
                                                                        0xFFFFFFFF, 0xFFFFFFFF, 0, 0);
    StubRoutines::x86::_vector_int_shuffle_mask = generate_vector_mask("vector_int_shuffle_mask", 0x0302010003020100);
    StubRoutines::x86::_vector_byte_shuffle_mask = generate_vector_byte_shuffle_mask("vector_byte_shuffle_mask");
    StubRoutines::x86::_vector_short_shuffle_mask = generate_vector_mask("vector_short_shuffle_mask", 0x0100010001000100);
    StubRoutines::x86::_vector_long_shuffle_mask = generate_vector_mask("vector_long_shuffle_mask", 0x0000000100000000);
    StubRoutines::x86::_vector_long_sign_mask = generate_vector_mask("vector_long_sign_mask", 0x8000000000000000);
    StubRoutines::x86::_vector_iota_indices = generate_iota_indices("iota_indices");

    // support for verify_oop (must happen after universe_init)
    if (VerifyOops) {
      StubRoutines::_verify_oop_subroutine_entry = generate_verify_oop();
    }

    // data cache line writeback
    StubRoutines::_data_cache_writeback = generate_data_cache_writeback();
    StubRoutines::_data_cache_writeback_sync = generate_data_cache_writeback_sync();

    // arraycopy stubs used by compilers
    generate_arraycopy_stubs();

    // don't bother generating these AES intrinsic stubs unless global flag is set
    if (UseAESIntrinsics) {
      StubRoutines::x86::_key_shuffle_mask_addr = generate_key_shuffle_mask();  // needed by the others
      StubRoutines::_aescrypt_encryptBlock = generate_aescrypt_encryptBlock();
      StubRoutines::_aescrypt_decryptBlock = generate_aescrypt_decryptBlock();
      StubRoutines::_cipherBlockChaining_encryptAESCrypt = generate_cipherBlockChaining_encryptAESCrypt();
      if (VM_Version::supports_avx512_vaes() &&  VM_Version::supports_avx512vl() && VM_Version::supports_avx512dq() ) {
        StubRoutines::_cipherBlockChaining_decryptAESCrypt = generate_cipherBlockChaining_decryptVectorAESCrypt();
        StubRoutines::_electronicCodeBook_encryptAESCrypt = generate_electronicCodeBook_encryptAESCrypt();
        StubRoutines::_electronicCodeBook_decryptAESCrypt = generate_electronicCodeBook_decryptAESCrypt();
      } else {
        StubRoutines::_cipherBlockChaining_decryptAESCrypt = generate_cipherBlockChaining_decryptAESCrypt_Parallel();
      }
    }
    if (UseAESCTRIntrinsics) {
      if (VM_Version::supports_avx512_vaes() && VM_Version::supports_avx512bw() && VM_Version::supports_avx512vl()) {
        StubRoutines::x86::_counter_mask_addr = counter_mask_addr();
        StubRoutines::_counterMode_AESCrypt = generate_counterMode_VectorAESCrypt();
      } else {
        StubRoutines::x86::_counter_shuffle_mask_addr = generate_counter_shuffle_mask();
        StubRoutines::_counterMode_AESCrypt = generate_counterMode_AESCrypt_Parallel();
      }
    }

    if (UseMD5Intrinsics) {
      StubRoutines::_md5_implCompress = generate_md5_implCompress(false, "md5_implCompress");
      StubRoutines::_md5_implCompressMB = generate_md5_implCompress(true, "md5_implCompressMB");
    }
    if (UseSHA1Intrinsics) {
      StubRoutines::x86::_upper_word_mask_addr = generate_upper_word_mask();
      StubRoutines::x86::_shuffle_byte_flip_mask_addr = generate_shuffle_byte_flip_mask();
      StubRoutines::_sha1_implCompress = generate_sha1_implCompress(false, "sha1_implCompress");
      StubRoutines::_sha1_implCompressMB = generate_sha1_implCompress(true, "sha1_implCompressMB");
    }
    if (UseSHA256Intrinsics) {
      StubRoutines::x86::_k256_adr = (address)StubRoutines::x86::_k256;
      char* dst = (char*)StubRoutines::x86::_k256_W;
      char* src = (char*)StubRoutines::x86::_k256;
      for (int ii = 0; ii < 16; ++ii) {
        memcpy(dst + 32 * ii,      src + 16 * ii, 16);
        memcpy(dst + 32 * ii + 16, src + 16 * ii, 16);
      }
      StubRoutines::x86::_k256_W_adr = (address)StubRoutines::x86::_k256_W;
      StubRoutines::x86::_pshuffle_byte_flip_mask_addr = generate_pshuffle_byte_flip_mask();
      StubRoutines::_sha256_implCompress = generate_sha256_implCompress(false, "sha256_implCompress");
      StubRoutines::_sha256_implCompressMB = generate_sha256_implCompress(true, "sha256_implCompressMB");
    }
    if (UseSHA512Intrinsics) {
      StubRoutines::x86::_k512_W_addr = (address)StubRoutines::x86::_k512_W;
      StubRoutines::x86::_pshuffle_byte_flip_mask_addr_sha512 = generate_pshuffle_byte_flip_mask_sha512();
      StubRoutines::_sha512_implCompress = generate_sha512_implCompress(false, "sha512_implCompress");
      StubRoutines::_sha512_implCompressMB = generate_sha512_implCompress(true, "sha512_implCompressMB");
    }

    // Generate GHASH intrinsics code
    if (UseGHASHIntrinsics) {
    StubRoutines::x86::_ghash_long_swap_mask_addr = generate_ghash_long_swap_mask();
    StubRoutines::x86::_ghash_byte_swap_mask_addr = generate_ghash_byte_swap_mask();
      if (VM_Version::supports_avx()) {
        StubRoutines::x86::_ghash_shuffmask_addr = ghash_shufflemask_addr();
        StubRoutines::x86::_ghash_poly_addr = ghash_polynomial_addr();
        StubRoutines::_ghash_processBlocks = generate_avx_ghash_processBlocks();
      } else {
        StubRoutines::_ghash_processBlocks = generate_ghash_processBlocks();
      }
    }


    if (UseBASE64Intrinsics) {
      if(VM_Version::supports_avx2() &&
         VM_Version::supports_avx512bw() &&
         VM_Version::supports_avx512vl()) {
        StubRoutines::x86::_avx2_shuffle_base64 = base64_avx2_shuffle_addr();
        StubRoutines::x86::_avx2_input_mask_base64 = base64_avx2_input_mask_addr();
        StubRoutines::x86::_avx2_lut_base64 = base64_avx2_lut_addr();
      }
      StubRoutines::x86::_encoding_table_base64 = base64_encoding_table_addr();
      if (VM_Version::supports_avx512_vbmi()) {
        StubRoutines::x86::_shuffle_base64 = base64_shuffle_addr();
        StubRoutines::x86::_lookup_lo_base64 = base64_vbmi_lookup_lo_addr();
        StubRoutines::x86::_lookup_hi_base64 = base64_vbmi_lookup_hi_addr();
        StubRoutines::x86::_lookup_lo_base64url = base64_vbmi_lookup_lo_url_addr();
        StubRoutines::x86::_lookup_hi_base64url = base64_vbmi_lookup_hi_url_addr();
        StubRoutines::x86::_pack_vec_base64 = base64_vbmi_pack_vec_addr();
        StubRoutines::x86::_join_0_1_base64 = base64_vbmi_join_0_1_addr();
        StubRoutines::x86::_join_1_2_base64 = base64_vbmi_join_1_2_addr();
        StubRoutines::x86::_join_2_3_base64 = base64_vbmi_join_2_3_addr();
      }
      StubRoutines::x86::_decoding_table_base64 = base64_decoding_table_addr();
      StubRoutines::_base64_encodeBlock = generate_base64_encodeBlock();
      StubRoutines::_base64_decodeBlock = generate_base64_decodeBlock();
    }

    BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
    if (bs_nm != NULL) {
      StubRoutines::x86::_method_entry_barrier = generate_method_entry_barrier();
    }
#ifdef COMPILER2
    if (UseMultiplyToLenIntrinsic) {
      StubRoutines::_multiplyToLen = generate_multiplyToLen();
    }
    if (UseSquareToLenIntrinsic) {
      StubRoutines::_squareToLen = generate_squareToLen();
    }
    if (UseMulAddIntrinsic) {
      StubRoutines::_mulAdd = generate_mulAdd();
    }
    if (VM_Version::supports_avx512_vbmi2()) {
      StubRoutines::_bigIntegerRightShiftWorker = generate_bigIntegerRightShift();
      StubRoutines::_bigIntegerLeftShiftWorker = generate_bigIntegerLeftShift();
    }
    if (UseMontgomeryMultiplyIntrinsic) {
      StubRoutines::_montgomeryMultiply
        = CAST_FROM_FN_PTR(address, SharedRuntime::montgomery_multiply);
    }
    if (UseMontgomerySquareIntrinsic) {
      StubRoutines::_montgomerySquare
        = CAST_FROM_FN_PTR(address, SharedRuntime::montgomery_square);
    }

    // Get svml stub routine addresses
    void *libsvml = NULL;
    char ebuf[1024];
    char dll_name[JVM_MAXPATHLEN];
    if (os::dll_locate_lib(dll_name, sizeof(dll_name), Arguments::get_dll_dir(), "svml")) {
      libsvml = os::dll_load(dll_name, ebuf, sizeof ebuf);
    }
    if (libsvml != NULL) {
      // SVML method naming convention
      //   All the methods are named as __svml_op<T><N>_ha_<VV>
      //   Where:
      //      ha stands for high accuracy
      //      <T> is optional to indicate float/double
      //              Set to f for vector float operation
      //              Omitted for vector double operation
      //      <N> is the number of elements in the vector
      //              1, 2, 4, 8, 16
      //              e.g. 128 bit float vector has 4 float elements
      //      <VV> indicates the avx/sse level:
      //              z0 is AVX512, l9 is AVX2, e9 is AVX1 and ex is for SSE2
      //      e.g. __svml_expf16_ha_z0 is the method for computing 16 element vector float exp using AVX 512 insns
      //           __svml_exp8_ha_z0 is the method for computing 8 element vector double exp using AVX 512 insns

      log_info(library)("Loaded library %s, handle " INTPTR_FORMAT, JNI_LIB_PREFIX "svml" JNI_LIB_SUFFIX, p2i(libsvml));
      if (UseAVX > 2) {
        for (int op = 0; op < VectorSupport::NUM_SVML_OP; op++) {
          int vop = VectorSupport::VECTOR_OP_SVML_START + op;
          if ((!VM_Version::supports_avx512dq()) &&
              (vop == VectorSupport::VECTOR_OP_LOG || vop == VectorSupport::VECTOR_OP_LOG10 || vop == VectorSupport::VECTOR_OP_POW)) {
            continue;
          }
          snprintf(ebuf, sizeof(ebuf), "__svml_%sf16_ha_z0", VectorSupport::svmlname[op]);
          StubRoutines::_vector_f_math[VectorSupport::VEC_SIZE_512][op] = (address)os::dll_lookup(libsvml, ebuf);

          snprintf(ebuf, sizeof(ebuf), "__svml_%s8_ha_z0", VectorSupport::svmlname[op]);
          StubRoutines::_vector_d_math[VectorSupport::VEC_SIZE_512][op] = (address)os::dll_lookup(libsvml, ebuf);
        }
      }
      const char* avx_sse_str = (UseAVX >= 2) ? "l9" : ((UseAVX == 1) ? "e9" : "ex");
      for (int op = 0; op < VectorSupport::NUM_SVML_OP; op++) {
        int vop = VectorSupport::VECTOR_OP_SVML_START + op;
        if (vop == VectorSupport::VECTOR_OP_POW) {
          continue;
        }
        snprintf(ebuf, sizeof(ebuf), "__svml_%sf4_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_f_math[VectorSupport::VEC_SIZE_64][op] = (address)os::dll_lookup(libsvml, ebuf);

        snprintf(ebuf, sizeof(ebuf), "__svml_%sf4_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_f_math[VectorSupport::VEC_SIZE_128][op] = (address)os::dll_lookup(libsvml, ebuf);

        snprintf(ebuf, sizeof(ebuf), "__svml_%sf8_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_f_math[VectorSupport::VEC_SIZE_256][op] = (address)os::dll_lookup(libsvml, ebuf);

        snprintf(ebuf, sizeof(ebuf), "__svml_%s1_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_d_math[VectorSupport::VEC_SIZE_64][op] = (address)os::dll_lookup(libsvml, ebuf);

        snprintf(ebuf, sizeof(ebuf), "__svml_%s2_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_d_math[VectorSupport::VEC_SIZE_128][op] = (address)os::dll_lookup(libsvml, ebuf);

        snprintf(ebuf, sizeof(ebuf), "__svml_%s4_ha_%s", VectorSupport::svmlname[op], avx_sse_str);
        StubRoutines::_vector_d_math[VectorSupport::VEC_SIZE_256][op] = (address)os::dll_lookup(libsvml, ebuf);
      }
    }
#endif // COMPILER2

    if (UseVectorizedMismatchIntrinsic) {
      StubRoutines::_vectorizedMismatch = generate_vectorizedMismatch();
    }
  }

 public:
  StubGenerator(CodeBuffer* code, bool all) : StubCodeGenerator(code) {
    if (all) {
      generate_all();
    } else {
      generate_initial();
    }
  }
}; // end class declaration

#define UCM_TABLE_MAX_ENTRIES 16
void StubGenerator_generate(CodeBuffer* code, bool all) {
  if (UnsafeCopyMemory::_table == NULL) {
    UnsafeCopyMemory::create_table(UCM_TABLE_MAX_ENTRIES);
  }
  StubGenerator g(code, all);
}
