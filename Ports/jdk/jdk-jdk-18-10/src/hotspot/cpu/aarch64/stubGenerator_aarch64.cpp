/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2021, Red Hat Inc. All rights reserved.
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
#include "atomic_aarch64.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/universe.hpp"
#include "nativeInst_aarch64.hpp"
#include "oops/instanceOop.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/atomic.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif
#if INCLUDE_ZGC
#include "gc/z/zThreadLocalData.hpp"
#endif

// Declaration and definition of StubGenerator (no .hpp file).
// For a more detailed description of the stub routine structure
// see the comment in stubRoutines.hpp

#undef __
#define __ _masm->
#define TIMES_OOP Address::sxtw(exact_log2(UseCompressedOops ? 4 : 8))

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

// Stub Code definitions

class StubGenerator: public StubCodeGenerator {
 private:

#ifdef PRODUCT
#define inc_counter_np(counter) ((void)0)
#else
  void inc_counter_np_(int& counter) {
    __ lea(rscratch2, ExternalAddress((address)&counter));
    __ ldrw(rscratch1, Address(rscratch2));
    __ addw(rscratch1, rscratch1, 1);
    __ strw(rscratch1, Address(rscratch2));
  }
#define inc_counter_np(counter) \
  BLOCK_COMMENT("inc_counter " #counter); \
  inc_counter_np_(counter);
#endif

  // Call stubs are used to call Java from C
  //
  // Arguments:
  //    c_rarg0:   call wrapper address                   address
  //    c_rarg1:   result                                 address
  //    c_rarg2:   result type                            BasicType
  //    c_rarg3:   method                                 Method*
  //    c_rarg4:   (interpreter) entry point              address
  //    c_rarg5:   parameters                             intptr_t*
  //    c_rarg6:   parameter size (in words)              int
  //    c_rarg7:   thread                                 Thread*
  //
  // There is no return from the stub itself as any Java result
  // is written to result
  //
  // we save r30 (lr) as the return PC at the base of the frame and
  // link r29 (fp) below it as the frame pointer installing sp (r31)
  // into fp.
  //
  // we save r0-r7, which accounts for all the c arguments.
  //
  // TODO: strictly do we need to save them all? they are treated as
  // volatile by C so could we omit saving the ones we are going to
  // place in global registers (thread? method?) or those we only use
  // during setup of the Java call?
  //
  // we don't need to save r8 which C uses as an indirect result location
  // return register.
  //
  // we don't need to save r9-r15 which both C and Java treat as
  // volatile
  //
  // we don't need to save r16-18 because Java does not use them
  //
  // we save r19-r28 which Java uses as scratch registers and C
  // expects to be callee-save
  //
  // we save the bottom 64 bits of each value stored in v8-v15; it is
  // the responsibility of the caller to preserve larger values.
  //
  // so the stub frame looks like this when we enter Java code
  //
  //     [ return_from_Java     ] <--- sp
  //     [ argument word n      ]
  //      ...
  // -27 [ argument word 1      ]
  // -26 [ saved v15            ] <--- sp_after_call
  // -25 [ saved v14            ]
  // -24 [ saved v13            ]
  // -23 [ saved v12            ]
  // -22 [ saved v11            ]
  // -21 [ saved v10            ]
  // -20 [ saved v9             ]
  // -19 [ saved v8             ]
  // -18 [ saved r28            ]
  // -17 [ saved r27            ]
  // -16 [ saved r26            ]
  // -15 [ saved r25            ]
  // -14 [ saved r24            ]
  // -13 [ saved r23            ]
  // -12 [ saved r22            ]
  // -11 [ saved r21            ]
  // -10 [ saved r20            ]
  //  -9 [ saved r19            ]
  //  -8 [ call wrapper    (r0) ]
  //  -7 [ result          (r1) ]
  //  -6 [ result type     (r2) ]
  //  -5 [ method          (r3) ]
  //  -4 [ entry point     (r4) ]
  //  -3 [ parameters      (r5) ]
  //  -2 [ parameter size  (r6) ]
  //  -1 [ thread (r7)          ]
  //   0 [ saved fp       (r29) ] <--- fp == saved sp (r31)
  //   1 [ saved lr       (r30) ]

  // Call stub stack layout word offsets from fp
  enum call_stub_layout {
    sp_after_call_off = -26,

    d15_off            = -26,
    d13_off            = -24,
    d11_off            = -22,
    d9_off             = -20,

    r28_off            = -18,
    r26_off            = -16,
    r24_off            = -14,
    r22_off            = -12,
    r20_off            = -10,
    call_wrapper_off   =  -8,
    result_off         =  -7,
    result_type_off    =  -6,
    method_off         =  -5,
    entry_point_off    =  -4,
    parameter_size_off =  -2,
    thread_off         =  -1,
    fp_f               =   0,
    retaddr_off        =   1,
  };

  address generate_call_stub(address& return_address) {
    assert((int)frame::entry_frame_after_call_words == -(int)sp_after_call_off + 1 &&
           (int)frame::entry_frame_call_wrapper_offset == (int)call_wrapper_off,
           "adjust this code");

    StubCodeMark mark(this, "StubRoutines", "call_stub");
    address start = __ pc();

    const Address sp_after_call(rfp, sp_after_call_off * wordSize);

    const Address call_wrapper  (rfp, call_wrapper_off   * wordSize);
    const Address result        (rfp, result_off         * wordSize);
    const Address result_type   (rfp, result_type_off    * wordSize);
    const Address method        (rfp, method_off         * wordSize);
    const Address entry_point   (rfp, entry_point_off    * wordSize);
    const Address parameter_size(rfp, parameter_size_off * wordSize);

    const Address thread        (rfp, thread_off         * wordSize);

    const Address d15_save      (rfp, d15_off * wordSize);
    const Address d13_save      (rfp, d13_off * wordSize);
    const Address d11_save      (rfp, d11_off * wordSize);
    const Address d9_save       (rfp, d9_off * wordSize);

    const Address r28_save      (rfp, r28_off * wordSize);
    const Address r26_save      (rfp, r26_off * wordSize);
    const Address r24_save      (rfp, r24_off * wordSize);
    const Address r22_save      (rfp, r22_off * wordSize);
    const Address r20_save      (rfp, r20_off * wordSize);

    // stub code

    address aarch64_entry = __ pc();

    // set up frame and move sp to end of save area
    __ enter();
    __ sub(sp, rfp, -sp_after_call_off * wordSize);

    // save register parameters and Java scratch/global registers
    // n.b. we save thread even though it gets installed in
    // rthread because we want to sanity check rthread later
    __ str(c_rarg7,  thread);
    __ strw(c_rarg6, parameter_size);
    __ stp(c_rarg4, c_rarg5,  entry_point);
    __ stp(c_rarg2, c_rarg3,  result_type);
    __ stp(c_rarg0, c_rarg1,  call_wrapper);

    __ stp(r20, r19,   r20_save);
    __ stp(r22, r21,   r22_save);
    __ stp(r24, r23,   r24_save);
    __ stp(r26, r25,   r26_save);
    __ stp(r28, r27,   r28_save);

    __ stpd(v9,  v8,   d9_save);
    __ stpd(v11, v10,  d11_save);
    __ stpd(v13, v12,  d13_save);
    __ stpd(v15, v14,  d15_save);

    // install Java thread in global register now we have saved
    // whatever value it held
    __ mov(rthread, c_rarg7);
    // And method
    __ mov(rmethod, c_rarg3);

    // set up the heapbase register
    __ reinit_heapbase();

#ifdef ASSERT
    // make sure we have no pending exceptions
    {
      Label L;
      __ ldr(rscratch1, Address(rthread, in_bytes(Thread::pending_exception_offset())));
      __ cmp(rscratch1, (u1)NULL_WORD);
      __ br(Assembler::EQ, L);
      __ stop("StubRoutines::call_stub: entered with pending exception");
      __ BIND(L);
    }
#endif
    // pass parameters if any
    __ mov(esp, sp);
    __ sub(rscratch1, sp, c_rarg6, ext::uxtw, LogBytesPerWord); // Move SP out of the way
    __ andr(sp, rscratch1, -2 * wordSize);

    BLOCK_COMMENT("pass parameters if any");
    Label parameters_done;
    // parameter count is still in c_rarg6
    // and parameter pointer identifying param 1 is in c_rarg5
    __ cbzw(c_rarg6, parameters_done);

    address loop = __ pc();
    __ ldr(rscratch1, Address(__ post(c_rarg5, wordSize)));
    __ subsw(c_rarg6, c_rarg6, 1);
    __ push(rscratch1);
    __ br(Assembler::GT, loop);

    __ BIND(parameters_done);

    // call Java entry -- passing methdoOop, and current sp
    //      rmethod: Method*
    //      r13: sender sp
    BLOCK_COMMENT("call Java function");
    __ mov(r13, sp);
    __ blr(c_rarg4);

    // we do this here because the notify will already have been done
    // if we get to the next instruction via an exception
    //
    // n.b. adding this instruction here affects the calculation of
    // whether or not a routine returns to the call stub (used when
    // doing stack walks) since the normal test is to check the return
    // pc against the address saved below. so we may need to allow for
    // this extra instruction in the check.

    // save current address for use by exception handling code

    return_address = __ pc();

    // store result depending on type (everything that is not
    // T_OBJECT, T_LONG, T_FLOAT or T_DOUBLE is treated as T_INT)
    // n.b. this assumes Java returns an integral result in r0
    // and a floating result in j_farg0
    __ ldr(j_rarg2, result);
    Label is_long, is_float, is_double, exit;
    __ ldr(j_rarg1, result_type);
    __ cmp(j_rarg1, (u1)T_OBJECT);
    __ br(Assembler::EQ, is_long);
    __ cmp(j_rarg1, (u1)T_LONG);
    __ br(Assembler::EQ, is_long);
    __ cmp(j_rarg1, (u1)T_FLOAT);
    __ br(Assembler::EQ, is_float);
    __ cmp(j_rarg1, (u1)T_DOUBLE);
    __ br(Assembler::EQ, is_double);

    // handle T_INT case
    __ strw(r0, Address(j_rarg2));

    __ BIND(exit);

    // pop parameters
    __ sub(esp, rfp, -sp_after_call_off * wordSize);

#ifdef ASSERT
    // verify that threads correspond
    {
      Label L, S;
      __ ldr(rscratch1, thread);
      __ cmp(rthread, rscratch1);
      __ br(Assembler::NE, S);
      __ get_thread(rscratch1);
      __ cmp(rthread, rscratch1);
      __ br(Assembler::EQ, L);
      __ BIND(S);
      __ stop("StubRoutines::call_stub: threads must correspond");
      __ BIND(L);
    }
#endif

    // restore callee-save registers
    __ ldpd(v15, v14,  d15_save);
    __ ldpd(v13, v12,  d13_save);
    __ ldpd(v11, v10,  d11_save);
    __ ldpd(v9,  v8,   d9_save);

    __ ldp(r28, r27,   r28_save);
    __ ldp(r26, r25,   r26_save);
    __ ldp(r24, r23,   r24_save);
    __ ldp(r22, r21,   r22_save);
    __ ldp(r20, r19,   r20_save);

    __ ldp(c_rarg0, c_rarg1,  call_wrapper);
    __ ldrw(c_rarg2, result_type);
    __ ldr(c_rarg3,  method);
    __ ldp(c_rarg4, c_rarg5,  entry_point);
    __ ldp(c_rarg6, c_rarg7,  parameter_size);

    // leave frame and return to caller
    __ leave();
    __ ret(lr);

    // handle return types different from T_INT

    __ BIND(is_long);
    __ str(r0, Address(j_rarg2, 0));
    __ br(Assembler::AL, exit);

    __ BIND(is_float);
    __ strs(j_farg0, Address(j_rarg2, 0));
    __ br(Assembler::AL, exit);

    __ BIND(is_double);
    __ strd(j_farg0, Address(j_rarg2, 0));
    __ br(Assembler::AL, exit);

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
  // r0: exception oop

  address generate_catch_exception() {
    StubCodeMark mark(this, "StubRoutines", "catch_exception");
    address start = __ pc();

    // same as in generate_call_stub():
    const Address sp_after_call(rfp, sp_after_call_off * wordSize);
    const Address thread        (rfp, thread_off         * wordSize);

#ifdef ASSERT
    // verify that threads correspond
    {
      Label L, S;
      __ ldr(rscratch1, thread);
      __ cmp(rthread, rscratch1);
      __ br(Assembler::NE, S);
      __ get_thread(rscratch1);
      __ cmp(rthread, rscratch1);
      __ br(Assembler::EQ, L);
      __ bind(S);
      __ stop("StubRoutines::catch_exception: threads must correspond");
      __ bind(L);
    }
#endif

    // set pending exception
    __ verify_oop(r0);

    __ str(r0, Address(rthread, Thread::pending_exception_offset()));
    __ mov(rscratch1, (address)__FILE__);
    __ str(rscratch1, Address(rthread, Thread::exception_file_offset()));
    __ movw(rscratch1, (int)__LINE__);
    __ strw(rscratch1, Address(rthread, Thread::exception_line_offset()));

    // complete return to VM
    assert(StubRoutines::_call_stub_return_address != NULL,
           "_call_stub_return_address must have been generated before");
    __ b(StubRoutines::_call_stub_return_address);

    return start;
  }

  // Continuation point for runtime calls returning with a pending
  // exception.  The pending exception check happened in the runtime
  // or native call stub.  The pending exception in Thread is
  // converted into a Java-level exception.
  //
  // Contract with Java-level exception handlers:
  // r0: exception
  // r3: throwing pc
  //
  // NOTE: At entry of this stub, exception-pc must be in LR !!

  // NOTE: this is always used as a jump target within generated code
  // so it just needs to be generated code wiht no x86 prolog

  address generate_forward_exception() {
    StubCodeMark mark(this, "StubRoutines", "forward exception");
    address start = __ pc();

    // Upon entry, LR points to the return address returning into
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
      __ ldr(rscratch1, Address(rthread, Thread::pending_exception_offset()));
      __ cbnz(rscratch1, L);
      __ stop("StubRoutines::forward exception: no pending exception (1)");
      __ bind(L);
    }
#endif

    // compute exception handler into r19

    // call the VM to find the handler address associated with the
    // caller address. pass thread in r0 and caller pc (ret address)
    // in r1. n.b. the caller pc is in lr, unlike x86 where it is on
    // the stack.
    __ mov(c_rarg1, lr);
    // lr will be trashed by the VM call so we move it to R19
    // (callee-saved) because we also need to pass it to the handler
    // returned by this call.
    __ mov(r19, lr);
    BLOCK_COMMENT("call exception_handler_for_return_address");
    __ call_VM_leaf(CAST_FROM_FN_PTR(address,
                         SharedRuntime::exception_handler_for_return_address),
                    rthread, c_rarg1);
    // Reinitialize the ptrue predicate register, in case the external runtime
    // call clobbers ptrue reg, as we may return to SVE compiled code.
    __ reinitialize_ptrue();

    // we should not really care that lr is no longer the callee
    // address. we saved the value the handler needs in r19 so we can
    // just copy it to r3. however, the C2 handler will push its own
    // frame and then calls into the VM and the VM code asserts that
    // the PC for the frame above the handler belongs to a compiled
    // Java method. So, we restore lr here to satisfy that assert.
    __ mov(lr, r19);
    // setup r0 & r3 & clear pending exception
    __ mov(r3, r19);
    __ mov(r19, r0);
    __ ldr(r0, Address(rthread, Thread::pending_exception_offset()));
    __ str(zr, Address(rthread, Thread::pending_exception_offset()));

#ifdef ASSERT
    // make sure exception is set
    {
      Label L;
      __ cbnz(r0, L);
      __ stop("StubRoutines::forward exception: no pending exception (2)");
      __ bind(L);
    }
#endif

    // continue at exception handler
    // r0: exception
    // r3: throwing pc
    // r19: exception handler
    __ verify_oop(r0);
    __ br(r19);

    return start;
  }

  // Non-destructive plausibility checks for oops
  //
  // Arguments:
  //    r0: oop to verify
  //    rscratch1: error message
  //
  // Stack after saving c_rarg3:
  //    [tos + 0]: saved c_rarg3
  //    [tos + 1]: saved c_rarg2
  //    [tos + 2]: saved lr
  //    [tos + 3]: saved rscratch2
  //    [tos + 4]: saved r0
  //    [tos + 5]: saved rscratch1
  address generate_verify_oop() {

    StubCodeMark mark(this, "StubRoutines", "verify_oop");
    address start = __ pc();

    Label exit, error;

    // save c_rarg2 and c_rarg3
    __ stp(c_rarg3, c_rarg2, Address(__ pre(sp, -16)));

    // __ incrementl(ExternalAddress((address) StubRoutines::verify_oop_count_addr()));
    __ lea(c_rarg2, ExternalAddress((address) StubRoutines::verify_oop_count_addr()));
    __ ldr(c_rarg3, Address(c_rarg2));
    __ add(c_rarg3, c_rarg3, 1);
    __ str(c_rarg3, Address(c_rarg2));

    // object is in r0
    // make sure object is 'reasonable'
    __ cbz(r0, exit); // if obj is NULL it is OK

#if INCLUDE_ZGC
    if (UseZGC) {
      // Check if mask is good.
      // verifies that ZAddressBadMask & r0 == 0
      __ ldr(c_rarg3, Address(rthread, ZThreadLocalData::address_bad_mask_offset()));
      __ andr(c_rarg2, r0, c_rarg3);
      __ cbnz(c_rarg2, error);
    }
#endif

    // Check if the oop is in the right area of memory
    __ mov(c_rarg3, (intptr_t) Universe::verify_oop_mask());
    __ andr(c_rarg2, r0, c_rarg3);
    __ mov(c_rarg3, (intptr_t) Universe::verify_oop_bits());

    // Compare c_rarg2 and c_rarg3.  We don't use a compare
    // instruction here because the flags register is live.
    __ eor(c_rarg2, c_rarg2, c_rarg3);
    __ cbnz(c_rarg2, error);

    // make sure klass is 'reasonable', which is not zero.
    __ load_klass(r0, r0);  // get klass
    __ cbz(r0, error);      // if klass is NULL it is broken

    // return if everything seems ok
    __ bind(exit);

    __ ldp(c_rarg3, c_rarg2, Address(__ post(sp, 16)));
    __ ret(lr);

    // handle errors
    __ bind(error);
    __ ldp(c_rarg3, c_rarg2, Address(__ post(sp, 16)));

    __ push(RegSet::range(r0, r29), sp);
    // debug(char* msg, int64_t pc, int64_t regs[])
    __ mov(c_rarg0, rscratch1);      // pass address of error message
    __ mov(c_rarg1, lr);             // pass return address
    __ mov(c_rarg2, sp);             // pass address of regs on stack
#ifndef PRODUCT
    assert(frame::arg_reg_save_area_bytes == 0, "not expecting frame reg save area");
#endif
    BLOCK_COMMENT("call MacroAssembler::debug");
    __ mov(rscratch1, CAST_FROM_FN_PTR(address, MacroAssembler::debug64));
    __ blr(rscratch1);
    __ hlt(0);

    return start;
  }

  void array_overlap_test(Label& L_no_overlap, Address::sxtw sf) { __ b(L_no_overlap); }

  // Generate indices for iota vector.
  address generate_iota_indices(const char *stub_name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stub_name);
    address start = __ pc();
    __ emit_data64(0x0706050403020100, relocInfo::none);
    __ emit_data64(0x0F0E0D0C0B0A0908, relocInfo::none);
    return start;
  }

  // The inner part of zero_words().  This is the bulk operation,
  // zeroing words in blocks, possibly using DC ZVA to do it.  The
  // caller is responsible for zeroing the last few words.
  //
  // Inputs:
  // r10: the HeapWord-aligned base address of an array to zero.
  // r11: the count in HeapWords, r11 > 0.
  //
  // Returns r10 and r11, adjusted for the caller to clear.
  // r10: the base address of the tail of words left to clear.
  // r11: the number of words in the tail.
  //      r11 < MacroAssembler::zero_words_block_size.

  address generate_zero_blocks() {
    Label done;
    Label base_aligned;

    Register base = r10, cnt = r11;

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "zero_blocks");
    address start = __ pc();

    if (UseBlockZeroing) {
      int zva_length = VM_Version::zva_length();

      // Ensure ZVA length can be divided by 16. This is required by
      // the subsequent operations.
      assert (zva_length % 16 == 0, "Unexpected ZVA Length");

      __ tbz(base, 3, base_aligned);
      __ str(zr, Address(__ post(base, 8)));
      __ sub(cnt, cnt, 1);
      __ bind(base_aligned);

      // Ensure count >= zva_length * 2 so that it still deserves a zva after
      // alignment.
      Label small;
      int low_limit = MAX2(zva_length * 2, (int)BlockZeroingLowLimit);
      __ subs(rscratch1, cnt, low_limit >> 3);
      __ br(Assembler::LT, small);
      __ zero_dcache_blocks(base, cnt);
      __ bind(small);
    }

    {
      // Number of stp instructions we'll unroll
      const int unroll =
        MacroAssembler::zero_words_block_size / 2;
      // Clear the remaining blocks.
      Label loop;
      __ subs(cnt, cnt, unroll * 2);
      __ br(Assembler::LT, done);
      __ bind(loop);
      for (int i = 0; i < unroll; i++)
        __ stp(zr, zr, __ post(base, 16));
      __ subs(cnt, cnt, unroll * 2);
      __ br(Assembler::GE, loop);
      __ bind(done);
      __ add(cnt, cnt, unroll * 2);
    }

    __ ret(lr);

    return start;
  }


  typedef enum {
    copy_forwards = 1,
    copy_backwards = -1
  } copy_direction;

  // Bulk copy of blocks of 8 words.
  //
  // count is a count of words.
  //
  // Precondition: count >= 8
  //
  // Postconditions:
  //
  // The least significant bit of count contains the remaining count
  // of words to copy.  The rest of count is trash.
  //
  // s and d are adjusted to point to the remaining words to copy
  //
  void generate_copy_longs(Label &start, Register s, Register d, Register count,
                           copy_direction direction) {
    int unit = wordSize * direction;
    int bias = (UseSIMDForMemoryOps ? 4:2) * wordSize;

    const Register t0 = r3, t1 = r4, t2 = r5, t3 = r6,
      t4 = r7, t5 = r10, t6 = r11, t7 = r12;
    const Register stride = r13;

    assert_different_registers(rscratch1, t0, t1, t2, t3, t4, t5, t6, t7);
    assert_different_registers(s, d, count, rscratch1);

    Label again, drain;
    const char *stub_name;
    if (direction == copy_forwards)
      stub_name = "forward_copy_longs";
    else
      stub_name = "backward_copy_longs";

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", stub_name);

    __ bind(start);

    Label unaligned_copy_long;
    if (AvoidUnalignedAccesses) {
      __ tbnz(d, 3, unaligned_copy_long);
    }

    if (direction == copy_forwards) {
      __ sub(s, s, bias);
      __ sub(d, d, bias);
    }

#ifdef ASSERT
    // Make sure we are never given < 8 words
    {
      Label L;
      __ cmp(count, (u1)8);
      __ br(Assembler::GE, L);
      __ stop("genrate_copy_longs called with < 8 words");
      __ bind(L);
    }
#endif

    // Fill 8 registers
    if (UseSIMDForMemoryOps) {
      __ ldpq(v0, v1, Address(s, 4 * unit));
      __ ldpq(v2, v3, Address(__ pre(s, 8 * unit)));
    } else {
      __ ldp(t0, t1, Address(s, 2 * unit));
      __ ldp(t2, t3, Address(s, 4 * unit));
      __ ldp(t4, t5, Address(s, 6 * unit));
      __ ldp(t6, t7, Address(__ pre(s, 8 * unit)));
    }

    __ subs(count, count, 16);
    __ br(Assembler::LO, drain);

    int prefetch = PrefetchCopyIntervalInBytes;
    bool use_stride = false;
    if (direction == copy_backwards) {
       use_stride = prefetch > 256;
       prefetch = -prefetch;
       if (use_stride) __ mov(stride, prefetch);
    }

    __ bind(again);

    if (PrefetchCopyIntervalInBytes > 0)
      __ prfm(use_stride ? Address(s, stride) : Address(s, prefetch), PLDL1KEEP);

    if (UseSIMDForMemoryOps) {
      __ stpq(v0, v1, Address(d, 4 * unit));
      __ ldpq(v0, v1, Address(s, 4 * unit));
      __ stpq(v2, v3, Address(__ pre(d, 8 * unit)));
      __ ldpq(v2, v3, Address(__ pre(s, 8 * unit)));
    } else {
      __ stp(t0, t1, Address(d, 2 * unit));
      __ ldp(t0, t1, Address(s, 2 * unit));
      __ stp(t2, t3, Address(d, 4 * unit));
      __ ldp(t2, t3, Address(s, 4 * unit));
      __ stp(t4, t5, Address(d, 6 * unit));
      __ ldp(t4, t5, Address(s, 6 * unit));
      __ stp(t6, t7, Address(__ pre(d, 8 * unit)));
      __ ldp(t6, t7, Address(__ pre(s, 8 * unit)));
    }

    __ subs(count, count, 8);
    __ br(Assembler::HS, again);

    // Drain
    __ bind(drain);
    if (UseSIMDForMemoryOps) {
      __ stpq(v0, v1, Address(d, 4 * unit));
      __ stpq(v2, v3, Address(__ pre(d, 8 * unit)));
    } else {
      __ stp(t0, t1, Address(d, 2 * unit));
      __ stp(t2, t3, Address(d, 4 * unit));
      __ stp(t4, t5, Address(d, 6 * unit));
      __ stp(t6, t7, Address(__ pre(d, 8 * unit)));
    }

    {
      Label L1, L2;
      __ tbz(count, exact_log2(4), L1);
      if (UseSIMDForMemoryOps) {
        __ ldpq(v0, v1, Address(__ pre(s, 4 * unit)));
        __ stpq(v0, v1, Address(__ pre(d, 4 * unit)));
      } else {
        __ ldp(t0, t1, Address(s, 2 * unit));
        __ ldp(t2, t3, Address(__ pre(s, 4 * unit)));
        __ stp(t0, t1, Address(d, 2 * unit));
        __ stp(t2, t3, Address(__ pre(d, 4 * unit)));
      }
      __ bind(L1);

      if (direction == copy_forwards) {
        __ add(s, s, bias);
        __ add(d, d, bias);
      }

      __ tbz(count, 1, L2);
      __ ldp(t0, t1, Address(__ adjust(s, 2 * unit, direction == copy_backwards)));
      __ stp(t0, t1, Address(__ adjust(d, 2 * unit, direction == copy_backwards)));
      __ bind(L2);
    }

    __ ret(lr);

    if (AvoidUnalignedAccesses) {
      Label drain, again;
      // Register order for storing. Order is different for backward copy.

      __ bind(unaligned_copy_long);

      // source address is even aligned, target odd aligned
      //
      // when forward copying word pairs we read long pairs at offsets
      // {0, 2, 4, 6} (in long words). when backwards copying we read
      // long pairs at offsets {-2, -4, -6, -8}. We adjust the source
      // address by -2 in the forwards case so we can compute the
      // source offsets for both as {2, 4, 6, 8} * unit where unit = 1
      // or -1.
      //
      // when forward copying we need to store 1 word, 3 pairs and
      // then 1 word at offsets {0, 1, 3, 5, 7}. Rather thna use a
      // zero offset We adjust the destination by -1 which means we
      // have to use offsets { 1, 2, 4, 6, 8} * unit for the stores.
      //
      // When backwards copyng we need to store 1 word, 3 pairs and
      // then 1 word at offsets {-1, -3, -5, -7, -8} i.e. we use
      // offsets {1, 3, 5, 7, 8} * unit.

      if (direction == copy_forwards) {
        __ sub(s, s, 16);
        __ sub(d, d, 8);
      }

      // Fill 8 registers
      //
      // for forwards copy s was offset by -16 from the original input
      // value of s so the register contents are at these offsets
      // relative to the 64 bit block addressed by that original input
      // and so on for each successive 64 byte block when s is updated
      //
      // t0 at offset 0,  t1 at offset 8
      // t2 at offset 16, t3 at offset 24
      // t4 at offset 32, t5 at offset 40
      // t6 at offset 48, t7 at offset 56

      // for backwards copy s was not offset so the register contents
      // are at these offsets into the preceding 64 byte block
      // relative to that original input and so on for each successive
      // preceding 64 byte block when s is updated. this explains the
      // slightly counter-intuitive looking pattern of register usage
      // in the stp instructions for backwards copy.
      //
      // t0 at offset -16, t1 at offset -8
      // t2 at offset -32, t3 at offset -24
      // t4 at offset -48, t5 at offset -40
      // t6 at offset -64, t7 at offset -56

      __ ldp(t0, t1, Address(s, 2 * unit));
      __ ldp(t2, t3, Address(s, 4 * unit));
      __ ldp(t4, t5, Address(s, 6 * unit));
      __ ldp(t6, t7, Address(__ pre(s, 8 * unit)));

      __ subs(count, count, 16);
      __ br(Assembler::LO, drain);

      int prefetch = PrefetchCopyIntervalInBytes;
      bool use_stride = false;
      if (direction == copy_backwards) {
         use_stride = prefetch > 256;
         prefetch = -prefetch;
         if (use_stride) __ mov(stride, prefetch);
      }

      __ bind(again);

      if (PrefetchCopyIntervalInBytes > 0)
        __ prfm(use_stride ? Address(s, stride) : Address(s, prefetch), PLDL1KEEP);

      if (direction == copy_forwards) {
       // allowing for the offset of -8 the store instructions place
       // registers into the target 64 bit block at the following
       // offsets
       //
       // t0 at offset 0
       // t1 at offset 8,  t2 at offset 16
       // t3 at offset 24, t4 at offset 32
       // t5 at offset 40, t6 at offset 48
       // t7 at offset 56

        __ str(t0, Address(d, 1 * unit));
        __ stp(t1, t2, Address(d, 2 * unit));
        __ ldp(t0, t1, Address(s, 2 * unit));
        __ stp(t3, t4, Address(d, 4 * unit));
        __ ldp(t2, t3, Address(s, 4 * unit));
        __ stp(t5, t6, Address(d, 6 * unit));
        __ ldp(t4, t5, Address(s, 6 * unit));
        __ str(t7, Address(__ pre(d, 8 * unit)));
        __ ldp(t6, t7, Address(__ pre(s, 8 * unit)));
      } else {
       // d was not offset when we started so the registers are
       // written into the 64 bit block preceding d with the following
       // offsets
       //
       // t1 at offset -8
       // t3 at offset -24, t0 at offset -16
       // t5 at offset -48, t2 at offset -32
       // t7 at offset -56, t4 at offset -48
       //                   t6 at offset -64
       //
       // note that this matches the offsets previously noted for the
       // loads

        __ str(t1, Address(d, 1 * unit));
        __ stp(t3, t0, Address(d, 3 * unit));
        __ ldp(t0, t1, Address(s, 2 * unit));
        __ stp(t5, t2, Address(d, 5 * unit));
        __ ldp(t2, t3, Address(s, 4 * unit));
        __ stp(t7, t4, Address(d, 7 * unit));
        __ ldp(t4, t5, Address(s, 6 * unit));
        __ str(t6, Address(__ pre(d, 8 * unit)));
        __ ldp(t6, t7, Address(__ pre(s, 8 * unit)));
      }

      __ subs(count, count, 8);
      __ br(Assembler::HS, again);

      // Drain
      //
      // this uses the same pattern of offsets and register arguments
      // as above
      __ bind(drain);
      if (direction == copy_forwards) {
        __ str(t0, Address(d, 1 * unit));
        __ stp(t1, t2, Address(d, 2 * unit));
        __ stp(t3, t4, Address(d, 4 * unit));
        __ stp(t5, t6, Address(d, 6 * unit));
        __ str(t7, Address(__ pre(d, 8 * unit)));
      } else {
        __ str(t1, Address(d, 1 * unit));
        __ stp(t3, t0, Address(d, 3 * unit));
        __ stp(t5, t2, Address(d, 5 * unit));
        __ stp(t7, t4, Address(d, 7 * unit));
        __ str(t6, Address(__ pre(d, 8 * unit)));
      }
      // now we need to copy any remaining part block which may
      // include a 4 word block subblock and/or a 2 word subblock.
      // bits 2 and 1 in the count are the tell-tale for whetehr we
      // have each such subblock
      {
        Label L1, L2;
        __ tbz(count, exact_log2(4), L1);
       // this is the same as above but copying only 4 longs hence
       // with ony one intervening stp between the str instructions
       // but note that the offsets and registers still follow the
       // same pattern
        __ ldp(t0, t1, Address(s, 2 * unit));
        __ ldp(t2, t3, Address(__ pre(s, 4 * unit)));
        if (direction == copy_forwards) {
          __ str(t0, Address(d, 1 * unit));
          __ stp(t1, t2, Address(d, 2 * unit));
          __ str(t3, Address(__ pre(d, 4 * unit)));
        } else {
          __ str(t1, Address(d, 1 * unit));
          __ stp(t3, t0, Address(d, 3 * unit));
          __ str(t2, Address(__ pre(d, 4 * unit)));
        }
        __ bind(L1);

        __ tbz(count, 1, L2);
       // this is the same as above but copying only 2 longs hence
       // there is no intervening stp between the str instructions
       // but note that the offset and register patterns are still
       // the same
        __ ldp(t0, t1, Address(__ pre(s, 2 * unit)));
        if (direction == copy_forwards) {
          __ str(t0, Address(d, 1 * unit));
          __ str(t1, Address(__ pre(d, 2 * unit)));
        } else {
          __ str(t1, Address(d, 1 * unit));
          __ str(t0, Address(__ pre(d, 2 * unit)));
        }
        __ bind(L2);

       // for forwards copy we need to re-adjust the offsets we
       // applied so that s and d are follow the last words written

       if (direction == copy_forwards) {
         __ add(s, s, 16);
         __ add(d, d, 8);
       }

      }

      __ ret(lr);
      }
  }

  // Small copy: less than 16 bytes.
  //
  // NB: Ignores all of the bits of count which represent more than 15
  // bytes, so a caller doesn't have to mask them.

  void copy_memory_small(Register s, Register d, Register count, Register tmp, int step) {
    bool is_backwards = step < 0;
    size_t granularity = uabs(step);
    int direction = is_backwards ? -1 : 1;
    int unit = wordSize * direction;

    Label Lword, Lint, Lshort, Lbyte;

    assert(granularity
           && granularity <= sizeof (jlong), "Impossible granularity in copy_memory_small");

    const Register t0 = r3, t1 = r4, t2 = r5, t3 = r6;

    // ??? I don't know if this bit-test-and-branch is the right thing
    // to do.  It does a lot of jumping, resulting in several
    // mispredicted branches.  It might make more sense to do this
    // with something like Duff's device with a single computed branch.

    __ tbz(count, 3 - exact_log2(granularity), Lword);
    __ ldr(tmp, Address(__ adjust(s, unit, is_backwards)));
    __ str(tmp, Address(__ adjust(d, unit, is_backwards)));
    __ bind(Lword);

    if (granularity <= sizeof (jint)) {
      __ tbz(count, 2 - exact_log2(granularity), Lint);
      __ ldrw(tmp, Address(__ adjust(s, sizeof (jint) * direction, is_backwards)));
      __ strw(tmp, Address(__ adjust(d, sizeof (jint) * direction, is_backwards)));
      __ bind(Lint);
    }

    if (granularity <= sizeof (jshort)) {
      __ tbz(count, 1 - exact_log2(granularity), Lshort);
      __ ldrh(tmp, Address(__ adjust(s, sizeof (jshort) * direction, is_backwards)));
      __ strh(tmp, Address(__ adjust(d, sizeof (jshort) * direction, is_backwards)));
      __ bind(Lshort);
    }

    if (granularity <= sizeof (jbyte)) {
      __ tbz(count, 0, Lbyte);
      __ ldrb(tmp, Address(__ adjust(s, sizeof (jbyte) * direction, is_backwards)));
      __ strb(tmp, Address(__ adjust(d, sizeof (jbyte) * direction, is_backwards)));
      __ bind(Lbyte);
    }
  }

  Label copy_f, copy_b;

  // All-singing all-dancing memory copy.
  //
  // Copy count units of memory from s to d.  The size of a unit is
  // step, which can be positive or negative depending on the direction
  // of copy.  If is_aligned is false, we align the source address.
  //

  void copy_memory(bool is_aligned, Register s, Register d,
                   Register count, Register tmp, int step) {
    copy_direction direction = step < 0 ? copy_backwards : copy_forwards;
    bool is_backwards = step < 0;
    unsigned int granularity = uabs(step);
    const Register t0 = r3, t1 = r4;

    // <= 80 (or 96 for SIMD) bytes do inline. Direction doesn't matter because we always
    // load all the data before writing anything
    Label copy4, copy8, copy16, copy32, copy80, copy_big, finish;
    const Register t2 = r5, t3 = r6, t4 = r7, t5 = r8;
    const Register t6 = r9, t7 = r10, t8 = r11, t9 = r12;
    const Register send = r17, dend = r16;

    if (PrefetchCopyIntervalInBytes > 0)
      __ prfm(Address(s, 0), PLDL1KEEP);
    __ cmp(count, u1((UseSIMDForMemoryOps ? 96:80)/granularity));
    __ br(Assembler::HI, copy_big);

    __ lea(send, Address(s, count, Address::lsl(exact_log2(granularity))));
    __ lea(dend, Address(d, count, Address::lsl(exact_log2(granularity))));

    __ cmp(count, u1(16/granularity));
    __ br(Assembler::LS, copy16);

    __ cmp(count, u1(64/granularity));
    __ br(Assembler::HI, copy80);

    __ cmp(count, u1(32/granularity));
    __ br(Assembler::LS, copy32);

    // 33..64 bytes
    if (UseSIMDForMemoryOps) {
      __ ldpq(v0, v1, Address(s, 0));
      __ ldpq(v2, v3, Address(send, -32));
      __ stpq(v0, v1, Address(d, 0));
      __ stpq(v2, v3, Address(dend, -32));
    } else {
      __ ldp(t0, t1, Address(s, 0));
      __ ldp(t2, t3, Address(s, 16));
      __ ldp(t4, t5, Address(send, -32));
      __ ldp(t6, t7, Address(send, -16));

      __ stp(t0, t1, Address(d, 0));
      __ stp(t2, t3, Address(d, 16));
      __ stp(t4, t5, Address(dend, -32));
      __ stp(t6, t7, Address(dend, -16));
    }
    __ b(finish);

    // 17..32 bytes
    __ bind(copy32);
    __ ldp(t0, t1, Address(s, 0));
    __ ldp(t2, t3, Address(send, -16));
    __ stp(t0, t1, Address(d, 0));
    __ stp(t2, t3, Address(dend, -16));
    __ b(finish);

    // 65..80/96 bytes
    // (96 bytes if SIMD because we do 32 byes per instruction)
    __ bind(copy80);
    if (UseSIMDForMemoryOps) {
      __ ldpq(v0, v1, Address(s, 0));
      __ ldpq(v2, v3, Address(s, 32));
      // Unaligned pointers can be an issue for copying.
      // The issue has more chances to happen when granularity of data is
      // less than 4(sizeof(jint)). Pointers for arrays of jint are at least
      // 4 byte aligned. Pointers for arrays of jlong are 8 byte aligned.
      // The most performance drop has been seen for the range 65-80 bytes.
      // For such cases using the pair of ldp/stp instead of the third pair of
      // ldpq/stpq fixes the performance issue.
      if (granularity < sizeof (jint)) {
        Label copy96;
        __ cmp(count, u1(80/granularity));
        __ br(Assembler::HI, copy96);
        __ ldp(t0, t1, Address(send, -16));

        __ stpq(v0, v1, Address(d, 0));
        __ stpq(v2, v3, Address(d, 32));
        __ stp(t0, t1, Address(dend, -16));
        __ b(finish);

        __ bind(copy96);
      }
      __ ldpq(v4, v5, Address(send, -32));

      __ stpq(v0, v1, Address(d, 0));
      __ stpq(v2, v3, Address(d, 32));
      __ stpq(v4, v5, Address(dend, -32));
    } else {
      __ ldp(t0, t1, Address(s, 0));
      __ ldp(t2, t3, Address(s, 16));
      __ ldp(t4, t5, Address(s, 32));
      __ ldp(t6, t7, Address(s, 48));
      __ ldp(t8, t9, Address(send, -16));

      __ stp(t0, t1, Address(d, 0));
      __ stp(t2, t3, Address(d, 16));
      __ stp(t4, t5, Address(d, 32));
      __ stp(t6, t7, Address(d, 48));
      __ stp(t8, t9, Address(dend, -16));
    }
    __ b(finish);

    // 0..16 bytes
    __ bind(copy16);
    __ cmp(count, u1(8/granularity));
    __ br(Assembler::LO, copy8);

    // 8..16 bytes
    __ ldr(t0, Address(s, 0));
    __ ldr(t1, Address(send, -8));
    __ str(t0, Address(d, 0));
    __ str(t1, Address(dend, -8));
    __ b(finish);

    if (granularity < 8) {
      // 4..7 bytes
      __ bind(copy8);
      __ tbz(count, 2 - exact_log2(granularity), copy4);
      __ ldrw(t0, Address(s, 0));
      __ ldrw(t1, Address(send, -4));
      __ strw(t0, Address(d, 0));
      __ strw(t1, Address(dend, -4));
      __ b(finish);
      if (granularity < 4) {
        // 0..3 bytes
        __ bind(copy4);
        __ cbz(count, finish); // get rid of 0 case
        if (granularity == 2) {
          __ ldrh(t0, Address(s, 0));
          __ strh(t0, Address(d, 0));
        } else { // granularity == 1
          // Now 1..3 bytes. Handle the 1 and 2 byte case by copying
          // the first and last byte.
          // Handle the 3 byte case by loading and storing base + count/2
          // (count == 1 (s+0)->(d+0), count == 2,3 (s+1) -> (d+1))
          // This does means in the 1 byte case we load/store the same
          // byte 3 times.
          __ lsr(count, count, 1);
          __ ldrb(t0, Address(s, 0));
          __ ldrb(t1, Address(send, -1));
          __ ldrb(t2, Address(s, count));
          __ strb(t0, Address(d, 0));
          __ strb(t1, Address(dend, -1));
          __ strb(t2, Address(d, count));
        }
        __ b(finish);
      }
    }

    __ bind(copy_big);
    if (is_backwards) {
      __ lea(s, Address(s, count, Address::lsl(exact_log2(-step))));
      __ lea(d, Address(d, count, Address::lsl(exact_log2(-step))));
    }

    // Now we've got the small case out of the way we can align the
    // source address on a 2-word boundary.

    Label aligned;

    if (is_aligned) {
      // We may have to adjust by 1 word to get s 2-word-aligned.
      __ tbz(s, exact_log2(wordSize), aligned);
      __ ldr(tmp, Address(__ adjust(s, direction * wordSize, is_backwards)));
      __ str(tmp, Address(__ adjust(d, direction * wordSize, is_backwards)));
      __ sub(count, count, wordSize/granularity);
    } else {
      if (is_backwards) {
        __ andr(rscratch2, s, 2 * wordSize - 1);
      } else {
        __ neg(rscratch2, s);
        __ andr(rscratch2, rscratch2, 2 * wordSize - 1);
      }
      // rscratch2 is the byte adjustment needed to align s.
      __ cbz(rscratch2, aligned);
      int shift = exact_log2(granularity);
      if (shift)  __ lsr(rscratch2, rscratch2, shift);
      __ sub(count, count, rscratch2);

#if 0
      // ?? This code is only correct for a disjoint copy.  It may or
      // may not make sense to use it in that case.

      // Copy the first pair; s and d may not be aligned.
      __ ldp(t0, t1, Address(s, is_backwards ? -2 * wordSize : 0));
      __ stp(t0, t1, Address(d, is_backwards ? -2 * wordSize : 0));

      // Align s and d, adjust count
      if (is_backwards) {
        __ sub(s, s, rscratch2);
        __ sub(d, d, rscratch2);
      } else {
        __ add(s, s, rscratch2);
        __ add(d, d, rscratch2);
      }
#else
      copy_memory_small(s, d, rscratch2, rscratch1, step);
#endif
    }

    __ bind(aligned);

    // s is now 2-word-aligned.

    // We have a count of units and some trailing bytes.  Adjust the
    // count and do a bulk copy of words.
    __ lsr(rscratch2, count, exact_log2(wordSize/granularity));
    if (direction == copy_forwards)
      __ bl(copy_f);
    else
      __ bl(copy_b);

    // And the tail.
    copy_memory_small(s, d, count, tmp, step);

    if (granularity >= 8) __ bind(copy8);
    if (granularity >= 4) __ bind(copy4);
    __ bind(finish);
  }


  void clobber_registers() {
#ifdef ASSERT
    RegSet clobbered
      = MacroAssembler::call_clobbered_registers() - rscratch1;
    __ mov(rscratch1, (uint64_t)0xdeadbeef);
    __ orr(rscratch1, rscratch1, rscratch1, Assembler::LSL, 32);
    for (RegSetIterator<> it = clobbered.begin(); *it != noreg; ++it) {
      __ mov(*it, rscratch1);
    }
#endif

  }

  // Scan over array at a for count oops, verifying each one.
  // Preserves a and count, clobbers rscratch1 and rscratch2.
  void verify_oop_array (int size, Register a, Register count, Register temp) {
    Label loop, end;
    __ mov(rscratch1, a);
    __ mov(rscratch2, zr);
    __ bind(loop);
    __ cmp(rscratch2, count);
    __ br(Assembler::HS, end);
    if (size == wordSize) {
      __ ldr(temp, Address(a, rscratch2, Address::lsl(exact_log2(size))));
      __ verify_oop(temp);
    } else {
      __ ldrw(temp, Address(a, rscratch2, Address::lsl(exact_log2(size))));
      __ decode_heap_oop(temp); // calls verify_oop
    }
    __ add(rscratch2, rscratch2, 1);
    __ b(loop);
    __ bind(end);
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
  // cache line boundaries will still be loaded and stored atomically.
  //
  // Side Effects:
  //   disjoint_int_copy_entry is set to the no-overlap entry point
  //   used by generate_conjoint_int_oop_copy().
  //
  address generate_disjoint_copy(int size, bool aligned, bool is_oop, address *entry,
                                  const char *name, bool dest_uninitialized = false) {
    Register s = c_rarg0, d = c_rarg1, count = c_rarg2;
    RegSet saved_reg = RegSet::of(s, d, count);
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();
    __ enter();

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_DISJOINT;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, is_oop, s, d, count, saved_reg);

    if (is_oop) {
      // save regs before copy_memory
      __ push(RegSet::of(d, count), sp);
    }
    {
      // UnsafeCopyMemory page error: continue after ucm
      bool add_entry = !is_oop && (!aligned || sizeof(jlong) == size);
      UnsafeCopyMemoryMark ucmm(this, add_entry, true);
      copy_memory(aligned, s, d, count, rscratch1, size);
    }

    if (is_oop) {
      __ pop(RegSet::of(d, count), sp);
      if (VerifyOops)
        verify_oop_array(size, d, count, r16);
    }

    bs->arraycopy_epilogue(_masm, decorators, is_oop, d, count, rscratch1, RegSet());

    __ leave();
    __ mov(r0, zr); // return 0
    __ ret(lr);
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
  // cache line boundaries will still be loaded and stored atomically.
  //
  address generate_conjoint_copy(int size, bool aligned, bool is_oop, address nooverlap_target,
                                 address *entry, const char *name,
                                 bool dest_uninitialized = false) {
    Register s = c_rarg0, d = c_rarg1, count = c_rarg2;
    RegSet saved_regs = RegSet::of(s, d, count);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();
    __ enter();

    if (entry != NULL) {
      *entry = __ pc();
      // caller can pass a 64-bit byte count here (from Unsafe.copyMemory)
      BLOCK_COMMENT("Entry:");
    }

    // use fwd copy when (d-s) above_equal (count*size)
    __ sub(rscratch1, d, s);
    __ cmp(rscratch1, count, Assembler::LSL, exact_log2(size));
    __ br(Assembler::HS, nooverlap_target);

    DecoratorSet decorators = IN_HEAP | IS_ARRAY;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, is_oop, s, d, count, saved_regs);

    if (is_oop) {
      // save regs before copy_memory
      __ push(RegSet::of(d, count), sp);
    }
    {
      // UnsafeCopyMemory page error: continue after ucm
      bool add_entry = !is_oop && (!aligned || sizeof(jlong) == size);
      UnsafeCopyMemoryMark ucmm(this, add_entry, true);
      copy_memory(aligned, s, d, count, rscratch1, -size);
    }
    if (is_oop) {
      __ pop(RegSet::of(d, count), sp);
      if (VerifyOops)
        verify_oop_array(size, d, count, r16);
    }
    bs->arraycopy_epilogue(_masm, decorators, is_oop, d, count, rscratch1, RegSet());
    __ leave();
    __ mov(r0, zr); // return 0
    __ ret(lr);
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
  // Side Effects:
  //   disjoint_byte_copy_entry is set to the no-overlap entry point  //
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
    const bool not_oop = false;
    return generate_disjoint_copy(sizeof (jbyte), aligned, not_oop, entry, name);
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
    const bool not_oop = false;
    return generate_conjoint_copy(sizeof (jbyte), aligned, not_oop, nooverlap_target, entry, name);
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
  address generate_disjoint_short_copy(bool aligned,
                                       address* entry, const char *name) {
    const bool not_oop = false;
    return generate_disjoint_copy(sizeof (jshort), aligned, not_oop, entry, name);
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
    const bool not_oop = false;
    return generate_conjoint_copy(sizeof (jshort), aligned, not_oop, nooverlap_target, entry, name);

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
  // If 'from' and/or 'to' are aligned on 4-byte boundaries, we let
  // the hardware handle it.  The two dwords within qwords that span
  // cache line boundaries will still be loaded and stored atomically.
  //
  // Side Effects:
  //   disjoint_int_copy_entry is set to the no-overlap entry point
  //   used by generate_conjoint_int_oop_copy().
  //
  address generate_disjoint_int_copy(bool aligned, address *entry,
                                         const char *name, bool dest_uninitialized = false) {
    const bool not_oop = false;
    return generate_disjoint_copy(sizeof (jint), aligned, not_oop, entry, name);
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
  // If 'from' and/or 'to' are aligned on 4-byte boundaries, we let
  // the hardware handle it.  The two dwords within qwords that span
  // cache line boundaries will still be loaded and stored atomically.
  //
  address generate_conjoint_int_copy(bool aligned, address nooverlap_target,
                                     address *entry, const char *name,
                                     bool dest_uninitialized = false) {
    const bool not_oop = false;
    return generate_conjoint_copy(sizeof (jint), aligned, not_oop, nooverlap_target, entry, name);
  }


  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as size_t, can be zero
  //
  // Side Effects:
  //   disjoint_oop_copy_entry or disjoint_long_copy_entry is set to the
  //   no-overlap entry point used by generate_conjoint_long_oop_copy().
  //
  address generate_disjoint_long_copy(bool aligned, address *entry,
                                          const char *name, bool dest_uninitialized = false) {
    const bool not_oop = false;
    return generate_disjoint_copy(sizeof (jlong), aligned, not_oop, entry, name);
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as size_t, can be zero
  //
  address generate_conjoint_long_copy(bool aligned,
                                      address nooverlap_target, address *entry,
                                      const char *name, bool dest_uninitialized = false) {
    const bool not_oop = false;
    return generate_conjoint_copy(sizeof (jlong), aligned, not_oop, nooverlap_target, entry, name);
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as size_t, can be zero
  //
  // Side Effects:
  //   disjoint_oop_copy_entry or disjoint_long_copy_entry is set to the
  //   no-overlap entry point used by generate_conjoint_long_oop_copy().
  //
  address generate_disjoint_oop_copy(bool aligned, address *entry,
                                     const char *name, bool dest_uninitialized) {
    const bool is_oop = true;
    const int size = UseCompressedOops ? sizeof (jint) : sizeof (jlong);
    return generate_disjoint_copy(size, aligned, is_oop, entry, name, dest_uninitialized);
  }

  // Arguments:
  //   aligned - true => Input and output aligned on a HeapWord boundary == 8 bytes
  //             ignored
  //   name    - stub name string
  //
  // Inputs:
  //   c_rarg0   - source array address
  //   c_rarg1   - destination array address
  //   c_rarg2   - element count, treated as size_t, can be zero
  //
  address generate_conjoint_oop_copy(bool aligned,
                                     address nooverlap_target, address *entry,
                                     const char *name, bool dest_uninitialized) {
    const bool is_oop = true;
    const int size = UseCompressedOops ? sizeof (jint) : sizeof (jlong);
    return generate_conjoint_copy(size, aligned, is_oop, nooverlap_target, entry,
                                  name, dest_uninitialized);
  }


  // Helper for generating a dynamic type check.
  // Smashes rscratch1, rscratch2.
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
  //    c_rarg4   - oop ckval (super_klass)
  //
  //  Output:
  //    r0 ==  0  -  success
  //    r0 == -1^K - failure, where K is partial transfer count
  //
  address generate_checkcast_copy(const char *name, address *entry,
                                  bool dest_uninitialized = false) {

    Label L_load_element, L_store_element, L_do_card_marks, L_done, L_done_pop;

    // Input registers (after setup_arg_regs)
    const Register from        = c_rarg0;   // source array address
    const Register to          = c_rarg1;   // destination array address
    const Register count       = c_rarg2;   // elementscount
    const Register ckoff       = c_rarg3;   // super_check_offset
    const Register ckval       = c_rarg4;   // super_klass

    RegSet wb_pre_saved_regs = RegSet::range(c_rarg0, c_rarg4);
    RegSet wb_post_saved_regs = RegSet::of(count);

    // Registers used as temps (r19, r20, r21, r22 are save-on-entry)
    const Register copied_oop  = r22;       // actual oop copied
    const Register count_save  = r21;       // orig elementscount
    const Register start_to    = r20;       // destination array start address
    const Register r19_klass   = r19;       // oop._klass

    //---------------------------------------------------------------
    // Assembler stub will be used for this call to arraycopy
    // if the two arrays are subtypes of Object[] but the
    // destination array type is not equal to or a supertype
    // of the source type.  Each element must be separately
    // checked.

    assert_different_registers(from, to, count, ckoff, ckval, start_to,
                               copied_oop, r19_klass, count_save);

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

    // Caller of this entry point must set up the argument registers.
    if (entry != NULL) {
      *entry = __ pc();
      BLOCK_COMMENT("Entry:");
    }

     // Empty array:  Nothing to do.
    __ cbz(count, L_done);
    __ push(RegSet::of(r19, r20, r21, r22), sp);

#ifdef ASSERT
    BLOCK_COMMENT("assert consistent ckoff/ckval");
    // The ckoff and ckval must be mutually consistent,
    // even though caller generates both.
    { Label L;
      int sco_offset = in_bytes(Klass::super_check_offset_offset());
      __ ldrw(start_to, Address(ckval, sco_offset));
      __ cmpw(ckoff, start_to);
      __ br(Assembler::EQ, L);
      __ stop("super_check_offset inconsistent");
      __ bind(L);
    }
#endif //ASSERT

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_CHECKCAST | ARRAYCOPY_DISJOINT;
    bool is_oop = true;
    if (dest_uninitialized) {
      decorators |= IS_DEST_UNINITIALIZED;
    }

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, is_oop, from, to, count, wb_pre_saved_regs);

    // save the original count
    __ mov(count_save, count);

    // Copy from low to high addresses
    __ mov(start_to, to);              // Save destination array start address
    __ b(L_load_element);

    // ======== begin loop ========
    // (Loop is rotated; its entry is L_load_element.)
    // Loop control:
    //   for (; count != 0; count--) {
    //     copied_oop = load_heap_oop(from++);
    //     ... generate_type_check ...;
    //     store_heap_oop(to++, copied_oop);
    //   }
    __ align(OptoLoopAlignment);

    __ BIND(L_store_element);
    __ store_heap_oop(__ post(to, UseCompressedOops ? 4 : 8), copied_oop, noreg, noreg, AS_RAW);  // store the oop
    __ sub(count, count, 1);
    __ cbz(count, L_do_card_marks);

    // ======== loop entry is here ========
    __ BIND(L_load_element);
    __ load_heap_oop(copied_oop, __ post(from, UseCompressedOops ? 4 : 8), noreg, noreg, AS_RAW); // load the oop
    __ cbz(copied_oop, L_store_element);

    __ load_klass(r19_klass, copied_oop);// query the object klass
    generate_type_check(r19_klass, ckoff, ckval, L_store_element);
    // ======== end loop ========

    // It was a real error; we must depend on the caller to finish the job.
    // Register count = remaining oops, count_orig = total oops.
    // Emit GC store barriers for the oops we have copied and report
    // their number to the caller.

    __ subs(count, count_save, count);     // K = partially copied oop count
    __ eon(count, count, zr);                   // report (-1^K) to caller
    __ br(Assembler::EQ, L_done_pop);

    __ BIND(L_do_card_marks);
    bs->arraycopy_epilogue(_masm, decorators, is_oop, start_to, count_save, rscratch1, wb_post_saved_regs);

    __ bind(L_done_pop);
    __ pop(RegSet::of(r19, r20, r21, r22), sp);
    inc_counter_np(SharedRuntime::_checkcast_array_copy_ctr);

    __ bind(L_done);
    __ mov(r0, count);
    __ leave();
    __ ret(lr);

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

    assert_different_registers(rscratch1, temp);

    //  if (src_pos + length > arrayOop(src)->length())  FAIL;
    __ ldrw(rscratch1, Address(src, arrayOopDesc::length_offset_in_bytes()));
    __ addw(temp, length, src_pos);
    __ cmpw(temp, rscratch1);
    __ br(Assembler::HI, L_failed);

    //  if (dst_pos + length > arrayOop(dst)->length())  FAIL;
    __ ldrw(rscratch1, Address(dst, arrayOopDesc::length_offset_in_bytes()));
    __ addw(temp, length, dst_pos);
    __ cmpw(temp, rscratch1);
    __ br(Assembler::HI, L_failed);

    // Have to clean up high 32 bits of 'src_pos' and 'dst_pos'.
    __ movw(src_pos, src_pos);
    __ movw(dst_pos, dst_pos);

    BLOCK_COMMENT("arraycopy_range_checks done");
  }

  // These stubs get called from some dumb test routine.
  // I'll write them properly when they're called from
  // something that's actually doing something.
  static void fake_arraycopy_stub(address src, address dst, int count) {
    assert(count == 0, "huh?");
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
                               address byte_copy_entry,
                               address short_copy_entry,
                               address int_copy_entry,
                               address long_copy_entry) {
    Label L_long_aligned, L_int_aligned, L_short_aligned;
    Register s = c_rarg0, d = c_rarg1, count = c_rarg2;

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    // bump this on entry, not on exit:
    inc_counter_np(SharedRuntime::_unsafe_array_copy_ctr);

    __ orr(rscratch1, s, d);
    __ orr(rscratch1, rscratch1, count);

    __ andr(rscratch1, rscratch1, BytesPerLong-1);
    __ cbz(rscratch1, L_long_aligned);
    __ andr(rscratch1, rscratch1, BytesPerInt-1);
    __ cbz(rscratch1, L_int_aligned);
    __ tbz(rscratch1, 0, L_short_aligned);
    __ b(RuntimeAddress(byte_copy_entry));

    __ BIND(L_short_aligned);
    __ lsr(count, count, LogBytesPerShort);  // size => short_count
    __ b(RuntimeAddress(short_copy_entry));
    __ BIND(L_int_aligned);
    __ lsr(count, count, LogBytesPerInt);    // size => int_count
    __ b(RuntimeAddress(int_copy_entry));
    __ BIND(L_long_aligned);
    __ lsr(count, count, LogBytesPerLong);   // size => long_count
    __ b(RuntimeAddress(long_copy_entry));

    return start;
  }

  //
  //  Generate generic array copy stubs
  //
  //  Input:
  //    c_rarg0    -  src oop
  //    c_rarg1    -  src_pos (32-bits)
  //    c_rarg2    -  dst oop
  //    c_rarg3    -  dst_pos (32-bits)
  //    c_rarg4    -  element count (32-bits)
  //
  //  Output:
  //    r0 ==  0  -  success
  //    r0 == -1^K - failure, where K is partial transfer count
  //
  address generate_generic_copy(const char *name,
                                address byte_copy_entry, address short_copy_entry,
                                address int_copy_entry, address oop_copy_entry,
                                address long_copy_entry, address checkcast_copy_entry) {

    Label L_failed, L_objArray;
    Label L_copy_bytes, L_copy_shorts, L_copy_ints, L_copy_longs;

    // Input registers
    const Register src        = c_rarg0;  // source array oop
    const Register src_pos    = c_rarg1;  // source position
    const Register dst        = c_rarg2;  // destination array oop
    const Register dst_pos    = c_rarg3;  // destination position
    const Register length     = c_rarg4;


    // Registers used as temps
    const Register dst_klass  = c_rarg5;

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", name);

    address start = __ pc();

    __ enter(); // required for proper stackwalking of RuntimeStub frame

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
    __ cbz(src, L_failed);

    //  if (src_pos < 0) return -1;
    __ tbnz(src_pos, 31, L_failed);  // i.e. sign bit set

    //  if (dst == NULL) return -1;
    __ cbz(dst, L_failed);

    //  if (dst_pos < 0) return -1;
    __ tbnz(dst_pos, 31, L_failed);  // i.e. sign bit set

    // registers used as temp
    const Register scratch_length    = r16; // elements count to copy
    const Register scratch_src_klass = r17; // array klass
    const Register lh                = r15; // layout helper

    //  if (length < 0) return -1;
    __ movw(scratch_length, length);        // length (elements count, 32-bits value)
    __ tbnz(scratch_length, 31, L_failed);  // i.e. sign bit set

    __ load_klass(scratch_src_klass, src);
#ifdef ASSERT
    //  assert(src->klass() != NULL);
    {
      BLOCK_COMMENT("assert klasses not null {");
      Label L1, L2;
      __ cbnz(scratch_src_klass, L2);   // it is broken if klass is NULL
      __ bind(L1);
      __ stop("broken null klass");
      __ bind(L2);
      __ load_klass(rscratch1, dst);
      __ cbz(rscratch1, L1);     // this would be broken also
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
    __ ldrw(lh, Address(scratch_src_klass, lh_offset));
    __ movw(rscratch1, objArray_lh);
    __ eorw(rscratch2, lh, rscratch1);
    __ cbzw(rscratch2, L_objArray);

    //  if (src->klass() != dst->klass()) return -1;
    __ load_klass(rscratch2, dst);
    __ eor(rscratch2, rscratch2, scratch_src_klass);
    __ cbnz(rscratch2, L_failed);

    //  if (!src->is_Array()) return -1;
    __ tbz(lh, 31, L_failed);  // i.e. (lh >= 0)

    // At this point, it is known to be a typeArray (array_tag 0x3).
#ifdef ASSERT
    {
      BLOCK_COMMENT("assert primitive array {");
      Label L;
      __ movw(rscratch2, Klass::_lh_array_tag_type_value << Klass::_lh_array_tag_shift);
      __ cmpw(lh, rscratch2);
      __ br(Assembler::GE, L);
      __ stop("must be a primitive array");
      __ bind(L);
      BLOCK_COMMENT("} assert primitive array done");
    }
#endif

    arraycopy_range_checks(src, src_pos, dst, dst_pos, scratch_length,
                           rscratch2, L_failed);

    // TypeArrayKlass
    //
    // src_addr = (src + array_header_in_bytes()) + (src_pos << log2elemsize);
    // dst_addr = (dst + array_header_in_bytes()) + (dst_pos << log2elemsize);
    //

    const Register rscratch1_offset = rscratch1;    // array offset
    const Register r15_elsize = lh; // element size

    __ ubfx(rscratch1_offset, lh, Klass::_lh_header_size_shift,
           exact_log2(Klass::_lh_header_size_mask+1));   // array_offset
    __ add(src, src, rscratch1_offset);           // src array offset
    __ add(dst, dst, rscratch1_offset);           // dst array offset
    BLOCK_COMMENT("choose copy loop based on element size");

    // next registers should be set before the jump to corresponding stub
    const Register from     = c_rarg0;  // source array address
    const Register to       = c_rarg1;  // destination array address
    const Register count    = c_rarg2;  // elements count

    // 'from', 'to', 'count' registers should be set in such order
    // since they are the same as 'src', 'src_pos', 'dst'.

    assert(Klass::_lh_log2_element_size_shift == 0, "fix this code");

    // The possible values of elsize are 0-3, i.e. exact_log2(element
    // size in bytes).  We do a simple bitwise binary search.
  __ BIND(L_copy_bytes);
    __ tbnz(r15_elsize, 1, L_copy_ints);
    __ tbnz(r15_elsize, 0, L_copy_shorts);
    __ lea(from, Address(src, src_pos));// src_addr
    __ lea(to,   Address(dst, dst_pos));// dst_addr
    __ movw(count, scratch_length); // length
    __ b(RuntimeAddress(byte_copy_entry));

  __ BIND(L_copy_shorts);
    __ lea(from, Address(src, src_pos, Address::lsl(1)));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::lsl(1)));// dst_addr
    __ movw(count, scratch_length); // length
    __ b(RuntimeAddress(short_copy_entry));

  __ BIND(L_copy_ints);
    __ tbnz(r15_elsize, 0, L_copy_longs);
    __ lea(from, Address(src, src_pos, Address::lsl(2)));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::lsl(2)));// dst_addr
    __ movw(count, scratch_length); // length
    __ b(RuntimeAddress(int_copy_entry));

  __ BIND(L_copy_longs);
#ifdef ASSERT
    {
      BLOCK_COMMENT("assert long copy {");
      Label L;
      __ andw(lh, lh, Klass::_lh_log2_element_size_mask); // lh -> r15_elsize
      __ cmpw(r15_elsize, LogBytesPerLong);
      __ br(Assembler::EQ, L);
      __ stop("must be long copy, but elsize is wrong");
      __ bind(L);
      BLOCK_COMMENT("} assert long copy done");
    }
#endif
    __ lea(from, Address(src, src_pos, Address::lsl(3)));// src_addr
    __ lea(to,   Address(dst, dst_pos, Address::lsl(3)));// dst_addr
    __ movw(count, scratch_length); // length
    __ b(RuntimeAddress(long_copy_entry));

    // ObjArrayKlass
  __ BIND(L_objArray);
    // live at this point:  scratch_src_klass, scratch_length, src[_pos], dst[_pos]

    Label L_plain_copy, L_checkcast_copy;
    //  test array classes for subtyping
    __ load_klass(r15, dst);
    __ cmp(scratch_src_klass, r15); // usual case is exact equality
    __ br(Assembler::NE, L_checkcast_copy);

    // Identically typed arrays can be copied without element-wise checks.
    arraycopy_range_checks(src, src_pos, dst, dst_pos, scratch_length,
                           rscratch2, L_failed);

    __ lea(from, Address(src, src_pos, Address::lsl(LogBytesPerHeapOop)));
    __ add(from, from, arrayOopDesc::base_offset_in_bytes(T_OBJECT));
    __ lea(to, Address(dst, dst_pos, Address::lsl(LogBytesPerHeapOop)));
    __ add(to, to, arrayOopDesc::base_offset_in_bytes(T_OBJECT));
    __ movw(count, scratch_length); // length
  __ BIND(L_plain_copy);
    __ b(RuntimeAddress(oop_copy_entry));

  __ BIND(L_checkcast_copy);
    // live at this point:  scratch_src_klass, scratch_length, r15 (dst_klass)
    {
      // Before looking at dst.length, make sure dst is also an objArray.
      __ ldrw(rscratch1, Address(r15, lh_offset));
      __ movw(rscratch2, objArray_lh);
      __ eorw(rscratch1, rscratch1, rscratch2);
      __ cbnzw(rscratch1, L_failed);

      // It is safe to examine both src.length and dst.length.
      arraycopy_range_checks(src, src_pos, dst, dst_pos, scratch_length,
                             r15, L_failed);

      __ load_klass(dst_klass, dst); // reload

      // Marshal the base address arguments now, freeing registers.
      __ lea(from, Address(src, src_pos, Address::lsl(LogBytesPerHeapOop)));
      __ add(from, from, arrayOopDesc::base_offset_in_bytes(T_OBJECT));
      __ lea(to, Address(dst, dst_pos, Address::lsl(LogBytesPerHeapOop)));
      __ add(to, to, arrayOopDesc::base_offset_in_bytes(T_OBJECT));
      __ movw(count, length);           // length (reloaded)
      Register sco_temp = c_rarg3;      // this register is free now
      assert_different_registers(from, to, count, sco_temp,
                                 dst_klass, scratch_src_klass);
      // assert_clean_int(count, sco_temp);

      // Generate the type check.
      const int sco_offset = in_bytes(Klass::super_check_offset_offset());
      __ ldrw(sco_temp, Address(dst_klass, sco_offset));

      // Smashes rscratch1, rscratch2
      generate_type_check(scratch_src_klass, sco_temp, dst_klass, L_plain_copy);

      // Fetch destination element klass from the ObjArrayKlass header.
      int ek_offset = in_bytes(ObjArrayKlass::element_klass_offset());
      __ ldr(dst_klass, Address(dst_klass, ek_offset));
      __ ldrw(sco_temp, Address(dst_klass, sco_offset));

      // the checkcast_copy loop needs two extra arguments:
      assert(c_rarg3 == sco_temp, "#3 already in place");
      // Set up arguments for checkcast_copy_entry.
      __ mov(c_rarg4, dst_klass);  // dst.klass.element_klass
      __ b(RuntimeAddress(checkcast_copy_entry));
    }

  __ BIND(L_failed);
    __ mov(r0, -1);
    __ leave();   // required for proper stackwalking of RuntimeStub frame
    __ ret(lr);

    return start;
  }

  //
  // Generate stub for array fill. If "aligned" is true, the
  // "to" address is assumed to be heapword aligned.
  //
  // Arguments for generated stub:
  //   to:    c_rarg0
  //   value: c_rarg1
  //   count: c_rarg2 treated as signed
  //
  address generate_fill(BasicType t, bool aligned, const char *name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    BLOCK_COMMENT("Entry:");

    const Register to        = c_rarg0;  // source array address
    const Register value     = c_rarg1;  // value
    const Register count     = c_rarg2;  // elements count

    const Register bz_base = r10;        // base for block_zero routine
    const Register cnt_words = r11;      // temp register

    __ enter();

    Label L_fill_elements, L_exit1;

    int shift = -1;
    switch (t) {
      case T_BYTE:
        shift = 0;
        __ cmpw(count, 8 >> shift); // Short arrays (< 8 bytes) fill by element
        __ bfi(value, value, 8, 8);   // 8 bit -> 16 bit
        __ bfi(value, value, 16, 16); // 16 bit -> 32 bit
        __ br(Assembler::LO, L_fill_elements);
        break;
      case T_SHORT:
        shift = 1;
        __ cmpw(count, 8 >> shift); // Short arrays (< 8 bytes) fill by element
        __ bfi(value, value, 16, 16); // 16 bit -> 32 bit
        __ br(Assembler::LO, L_fill_elements);
        break;
      case T_INT:
        shift = 2;
        __ cmpw(count, 8 >> shift); // Short arrays (< 8 bytes) fill by element
        __ br(Assembler::LO, L_fill_elements);
        break;
      default: ShouldNotReachHere();
    }

    // Align source address at 8 bytes address boundary.
    Label L_skip_align1, L_skip_align2, L_skip_align4;
    if (!aligned) {
      switch (t) {
        case T_BYTE:
          // One byte misalignment happens only for byte arrays.
          __ tbz(to, 0, L_skip_align1);
          __ strb(value, Address(__ post(to, 1)));
          __ subw(count, count, 1);
          __ bind(L_skip_align1);
          // Fallthrough
        case T_SHORT:
          // Two bytes misalignment happens only for byte and short (char) arrays.
          __ tbz(to, 1, L_skip_align2);
          __ strh(value, Address(__ post(to, 2)));
          __ subw(count, count, 2 >> shift);
          __ bind(L_skip_align2);
          // Fallthrough
        case T_INT:
          // Align to 8 bytes, we know we are 4 byte aligned to start.
          __ tbz(to, 2, L_skip_align4);
          __ strw(value, Address(__ post(to, 4)));
          __ subw(count, count, 4 >> shift);
          __ bind(L_skip_align4);
          break;
        default: ShouldNotReachHere();
      }
    }

    //
    //  Fill large chunks
    //
    __ lsrw(cnt_words, count, 3 - shift); // number of words
    __ bfi(value, value, 32, 32);         // 32 bit -> 64 bit
    __ subw(count, count, cnt_words, Assembler::LSL, 3 - shift);
    if (UseBlockZeroing) {
      Label non_block_zeroing, rest;
      // If the fill value is zero we can use the fast zero_words().
      __ cbnz(value, non_block_zeroing);
      __ mov(bz_base, to);
      __ add(to, to, cnt_words, Assembler::LSL, LogBytesPerWord);
      __ zero_words(bz_base, cnt_words);
      __ b(rest);
      __ bind(non_block_zeroing);
      __ fill_words(to, cnt_words, value);
      __ bind(rest);
    } else {
      __ fill_words(to, cnt_words, value);
    }

    // Remaining count is less than 8 bytes. Fill it by a single store.
    // Note that the total length is no less than 8 bytes.
    if (t == T_BYTE || t == T_SHORT) {
      Label L_exit1;
      __ cbzw(count, L_exit1);
      __ add(to, to, count, Assembler::LSL, shift); // points to the end
      __ str(value, Address(to, -8));    // overwrite some elements
      __ bind(L_exit1);
      __ leave();
      __ ret(lr);
    }

    // Handle copies less than 8 bytes.
    Label L_fill_2, L_fill_4, L_exit2;
    __ bind(L_fill_elements);
    switch (t) {
      case T_BYTE:
        __ tbz(count, 0, L_fill_2);
        __ strb(value, Address(__ post(to, 1)));
        __ bind(L_fill_2);
        __ tbz(count, 1, L_fill_4);
        __ strh(value, Address(__ post(to, 2)));
        __ bind(L_fill_4);
        __ tbz(count, 2, L_exit2);
        __ strw(value, Address(to));
        break;
      case T_SHORT:
        __ tbz(count, 0, L_fill_4);
        __ strh(value, Address(__ post(to, 2)));
        __ bind(L_fill_4);
        __ tbz(count, 1, L_exit2);
        __ strw(value, Address(to));
        break;
      case T_INT:
        __ cbzw(count, L_exit2);
        __ strw(value, Address(to));
        break;
      default: ShouldNotReachHere();
    }
    __ bind(L_exit2);
    __ leave();
    __ ret(lr);
    return start;
  }

  address generate_data_cache_writeback() {
    const Register line        = c_rarg0;  // address of line to write back

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "_data_cache_writeback");

    address start = __ pc();
    __ enter();
    __ cache_wb(Address(line, 0));
    __ leave();
    __ ret(lr);

    return start;
  }

  address generate_data_cache_writeback_sync() {
    const Register is_pre     = c_rarg0;  // pre or post sync

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "_data_cache_writeback_sync");

    // pre wbsync is a no-op
    // post wbsync translates to an sfence

    Label skip;
    address start = __ pc();
    __ enter();
    __ cbnz(is_pre, skip);
    __ cache_wbsync(false);
    __ bind(skip);
    __ leave();
    __ ret(lr);

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

    generate_copy_longs(copy_f, r0, r1, rscratch2, copy_forwards);
    generate_copy_longs(copy_b, r0, r1, rscratch2, copy_backwards);

    StubRoutines::aarch64::_zero_blocks = generate_zero_blocks();

    //*** jbyte
    // Always need aligned and unaligned versions
    StubRoutines::_jbyte_disjoint_arraycopy         = generate_disjoint_byte_copy(false, &entry,
                                                                                  "jbyte_disjoint_arraycopy");
    StubRoutines::_jbyte_arraycopy                  = generate_conjoint_byte_copy(false, entry,
                                                                                  &entry_jbyte_arraycopy,
                                                                                  "jbyte_arraycopy");
    StubRoutines::_arrayof_jbyte_disjoint_arraycopy = generate_disjoint_byte_copy(true, &entry,
                                                                                  "arrayof_jbyte_disjoint_arraycopy");
    StubRoutines::_arrayof_jbyte_arraycopy          = generate_conjoint_byte_copy(true, entry, NULL,
                                                                                  "arrayof_jbyte_arraycopy");

    //*** jshort
    // Always need aligned and unaligned versions
    StubRoutines::_jshort_disjoint_arraycopy         = generate_disjoint_short_copy(false, &entry,
                                                                                    "jshort_disjoint_arraycopy");
    StubRoutines::_jshort_arraycopy                  = generate_conjoint_short_copy(false, entry,
                                                                                    &entry_jshort_arraycopy,
                                                                                    "jshort_arraycopy");
    StubRoutines::_arrayof_jshort_disjoint_arraycopy = generate_disjoint_short_copy(true, &entry,
                                                                                    "arrayof_jshort_disjoint_arraycopy");
    StubRoutines::_arrayof_jshort_arraycopy          = generate_conjoint_short_copy(true, entry, NULL,
                                                                                    "arrayof_jshort_arraycopy");

    //*** jint
    // Aligned versions
    StubRoutines::_arrayof_jint_disjoint_arraycopy = generate_disjoint_int_copy(true, &entry,
                                                                                "arrayof_jint_disjoint_arraycopy");
    StubRoutines::_arrayof_jint_arraycopy          = generate_conjoint_int_copy(true, entry, &entry_jint_arraycopy,
                                                                                "arrayof_jint_arraycopy");
    // In 64 bit we need both aligned and unaligned versions of jint arraycopy.
    // entry_jint_arraycopy always points to the unaligned version
    StubRoutines::_jint_disjoint_arraycopy         = generate_disjoint_int_copy(false, &entry,
                                                                                "jint_disjoint_arraycopy");
    StubRoutines::_jint_arraycopy                  = generate_conjoint_int_copy(false, entry,
                                                                                &entry_jint_arraycopy,
                                                                                "jint_arraycopy");

    //*** jlong
    // It is always aligned
    StubRoutines::_arrayof_jlong_disjoint_arraycopy = generate_disjoint_long_copy(true, &entry,
                                                                                  "arrayof_jlong_disjoint_arraycopy");
    StubRoutines::_arrayof_jlong_arraycopy          = generate_conjoint_long_copy(true, entry, &entry_jlong_arraycopy,
                                                                                  "arrayof_jlong_arraycopy");
    StubRoutines::_jlong_disjoint_arraycopy         = StubRoutines::_arrayof_jlong_disjoint_arraycopy;
    StubRoutines::_jlong_arraycopy                  = StubRoutines::_arrayof_jlong_arraycopy;

    //*** oops
    {
      // With compressed oops we need unaligned versions; notice that
      // we overwrite entry_oop_arraycopy.
      bool aligned = !UseCompressedOops;

      StubRoutines::_arrayof_oop_disjoint_arraycopy
        = generate_disjoint_oop_copy(aligned, &entry, "arrayof_oop_disjoint_arraycopy",
                                     /*dest_uninitialized*/false);
      StubRoutines::_arrayof_oop_arraycopy
        = generate_conjoint_oop_copy(aligned, entry, &entry_oop_arraycopy, "arrayof_oop_arraycopy",
                                     /*dest_uninitialized*/false);
      // Aligned versions without pre-barriers
      StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit
        = generate_disjoint_oop_copy(aligned, &entry, "arrayof_oop_disjoint_arraycopy_uninit",
                                     /*dest_uninitialized*/true);
      StubRoutines::_arrayof_oop_arraycopy_uninit
        = generate_conjoint_oop_copy(aligned, entry, NULL, "arrayof_oop_arraycopy_uninit",
                                     /*dest_uninitialized*/true);
    }

    StubRoutines::_oop_disjoint_arraycopy            = StubRoutines::_arrayof_oop_disjoint_arraycopy;
    StubRoutines::_oop_arraycopy                     = StubRoutines::_arrayof_oop_arraycopy;
    StubRoutines::_oop_disjoint_arraycopy_uninit     = StubRoutines::_arrayof_oop_disjoint_arraycopy_uninit;
    StubRoutines::_oop_arraycopy_uninit              = StubRoutines::_arrayof_oop_arraycopy_uninit;

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
  }

  void generate_math_stubs() { Unimplemented(); }

  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - source byte array address
  //   c_rarg1   - destination byte array address
  //   c_rarg2   - K (key) in little endian int array
  //
  address generate_aescrypt_encryptBlock() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "aescrypt_encryptBlock");

    Label L_doLast;

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register keylen      = rscratch1;

    address start = __ pc();
    __ enter();

    __ ldrw(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    __ ld1(v0, __ T16B, from); // get 16 bytes of input

    __ ld1(v1, v2, v3, v4, __ T16B, __ post(key, 64));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);
    __ rev32(v3, __ T16B, v3);
    __ rev32(v4, __ T16B, v4);
    __ aese(v0, v1);
    __ aesmc(v0, v0);
    __ aese(v0, v2);
    __ aesmc(v0, v0);
    __ aese(v0, v3);
    __ aesmc(v0, v0);
    __ aese(v0, v4);
    __ aesmc(v0, v0);

    __ ld1(v1, v2, v3, v4, __ T16B, __ post(key, 64));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);
    __ rev32(v3, __ T16B, v3);
    __ rev32(v4, __ T16B, v4);
    __ aese(v0, v1);
    __ aesmc(v0, v0);
    __ aese(v0, v2);
    __ aesmc(v0, v0);
    __ aese(v0, v3);
    __ aesmc(v0, v0);
    __ aese(v0, v4);
    __ aesmc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ cmpw(keylen, 44);
    __ br(Assembler::EQ, L_doLast);

    __ aese(v0, v1);
    __ aesmc(v0, v0);
    __ aese(v0, v2);
    __ aesmc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ cmpw(keylen, 52);
    __ br(Assembler::EQ, L_doLast);

    __ aese(v0, v1);
    __ aesmc(v0, v0);
    __ aese(v0, v2);
    __ aesmc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ BIND(L_doLast);

    __ aese(v0, v1);
    __ aesmc(v0, v0);
    __ aese(v0, v2);

    __ ld1(v1, __ T16B, key);
    __ rev32(v1, __ T16B, v1);
    __ eor(v0, __ T16B, v0, v1);

    __ st1(v0, __ T16B, to);

    __ mov(r0, 0);

    __ leave();
    __ ret(lr);

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
    assert(UseAES, "need AES cryptographic extension support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "aescrypt_decryptBlock");
    Label L_doLast;

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register keylen      = rscratch1;

    address start = __ pc();
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ ldrw(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

    __ ld1(v0, __ T16B, from); // get 16 bytes of input

    __ ld1(v5, __ T16B, __ post(key, 16));
    __ rev32(v5, __ T16B, v5);

    __ ld1(v1, v2, v3, v4, __ T16B, __ post(key, 64));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);
    __ rev32(v3, __ T16B, v3);
    __ rev32(v4, __ T16B, v4);
    __ aesd(v0, v1);
    __ aesimc(v0, v0);
    __ aesd(v0, v2);
    __ aesimc(v0, v0);
    __ aesd(v0, v3);
    __ aesimc(v0, v0);
    __ aesd(v0, v4);
    __ aesimc(v0, v0);

    __ ld1(v1, v2, v3, v4, __ T16B, __ post(key, 64));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);
    __ rev32(v3, __ T16B, v3);
    __ rev32(v4, __ T16B, v4);
    __ aesd(v0, v1);
    __ aesimc(v0, v0);
    __ aesd(v0, v2);
    __ aesimc(v0, v0);
    __ aesd(v0, v3);
    __ aesimc(v0, v0);
    __ aesd(v0, v4);
    __ aesimc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ cmpw(keylen, 44);
    __ br(Assembler::EQ, L_doLast);

    __ aesd(v0, v1);
    __ aesimc(v0, v0);
    __ aesd(v0, v2);
    __ aesimc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ cmpw(keylen, 52);
    __ br(Assembler::EQ, L_doLast);

    __ aesd(v0, v1);
    __ aesimc(v0, v0);
    __ aesd(v0, v2);
    __ aesimc(v0, v0);

    __ ld1(v1, v2, __ T16B, __ post(key, 32));
    __ rev32(v1, __ T16B, v1);
    __ rev32(v2, __ T16B, v2);

    __ BIND(L_doLast);

    __ aesd(v0, v1);
    __ aesimc(v0, v0);
    __ aesd(v0, v2);

    __ eor(v0, __ T16B, v0, v5);

    __ st1(v0, __ T16B, to);

    __ mov(r0, 0);

    __ leave();
    __ ret(lr);

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
  //   x0        - input length
  //
  address generate_cipherBlockChaining_encryptAESCrypt() {
    assert(UseAES, "need AES cryptographic extension support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_encryptAESCrypt");

    Label L_loadkeys_44, L_loadkeys_52, L_aes_loop, L_rounds_44, L_rounds_52;

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register rvec        = c_rarg3;  // r byte array initialized from initvector array address
                                           // and left with the results of the last encryption block
    const Register len_reg     = c_rarg4;  // src len (must be multiple of blocksize 16)
    const Register keylen      = rscratch1;

    address start = __ pc();

      __ enter();

      __ movw(rscratch2, len_reg);

      __ ldrw(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

      __ ld1(v0, __ T16B, rvec);

      __ cmpw(keylen, 52);
      __ br(Assembler::CC, L_loadkeys_44);
      __ br(Assembler::EQ, L_loadkeys_52);

      __ ld1(v17, v18, __ T16B, __ post(key, 32));
      __ rev32(v17, __ T16B, v17);
      __ rev32(v18, __ T16B, v18);
    __ BIND(L_loadkeys_52);
      __ ld1(v19, v20, __ T16B, __ post(key, 32));
      __ rev32(v19, __ T16B, v19);
      __ rev32(v20, __ T16B, v20);
    __ BIND(L_loadkeys_44);
      __ ld1(v21, v22, v23, v24, __ T16B, __ post(key, 64));
      __ rev32(v21, __ T16B, v21);
      __ rev32(v22, __ T16B, v22);
      __ rev32(v23, __ T16B, v23);
      __ rev32(v24, __ T16B, v24);
      __ ld1(v25, v26, v27, v28, __ T16B, __ post(key, 64));
      __ rev32(v25, __ T16B, v25);
      __ rev32(v26, __ T16B, v26);
      __ rev32(v27, __ T16B, v27);
      __ rev32(v28, __ T16B, v28);
      __ ld1(v29, v30, v31, __ T16B, key);
      __ rev32(v29, __ T16B, v29);
      __ rev32(v30, __ T16B, v30);
      __ rev32(v31, __ T16B, v31);

    __ BIND(L_aes_loop);
      __ ld1(v1, __ T16B, __ post(from, 16));
      __ eor(v0, __ T16B, v0, v1);

      __ br(Assembler::CC, L_rounds_44);
      __ br(Assembler::EQ, L_rounds_52);

      __ aese(v0, v17); __ aesmc(v0, v0);
      __ aese(v0, v18); __ aesmc(v0, v0);
    __ BIND(L_rounds_52);
      __ aese(v0, v19); __ aesmc(v0, v0);
      __ aese(v0, v20); __ aesmc(v0, v0);
    __ BIND(L_rounds_44);
      __ aese(v0, v21); __ aesmc(v0, v0);
      __ aese(v0, v22); __ aesmc(v0, v0);
      __ aese(v0, v23); __ aesmc(v0, v0);
      __ aese(v0, v24); __ aesmc(v0, v0);
      __ aese(v0, v25); __ aesmc(v0, v0);
      __ aese(v0, v26); __ aesmc(v0, v0);
      __ aese(v0, v27); __ aesmc(v0, v0);
      __ aese(v0, v28); __ aesmc(v0, v0);
      __ aese(v0, v29); __ aesmc(v0, v0);
      __ aese(v0, v30);
      __ eor(v0, __ T16B, v0, v31);

      __ st1(v0, __ T16B, __ post(to, 16));

      __ subw(len_reg, len_reg, 16);
      __ cbnzw(len_reg, L_aes_loop);

      __ st1(v0, __ T16B, rvec);

      __ mov(r0, rscratch2);

      __ leave();
      __ ret(lr);

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
  //   r0        - input length
  //
  address generate_cipherBlockChaining_decryptAESCrypt() {
    assert(UseAES, "need AES cryptographic extension support");
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "cipherBlockChaining_decryptAESCrypt");

    Label L_loadkeys_44, L_loadkeys_52, L_aes_loop, L_rounds_44, L_rounds_52;

    const Register from        = c_rarg0;  // source array address
    const Register to          = c_rarg1;  // destination array address
    const Register key         = c_rarg2;  // key array address
    const Register rvec        = c_rarg3;  // r byte array initialized from initvector array address
                                           // and left with the results of the last encryption block
    const Register len_reg     = c_rarg4;  // src len (must be multiple of blocksize 16)
    const Register keylen      = rscratch1;

    address start = __ pc();

      __ enter();

      __ movw(rscratch2, len_reg);

      __ ldrw(keylen, Address(key, arrayOopDesc::length_offset_in_bytes() - arrayOopDesc::base_offset_in_bytes(T_INT)));

      __ ld1(v2, __ T16B, rvec);

      __ ld1(v31, __ T16B, __ post(key, 16));
      __ rev32(v31, __ T16B, v31);

      __ cmpw(keylen, 52);
      __ br(Assembler::CC, L_loadkeys_44);
      __ br(Assembler::EQ, L_loadkeys_52);

      __ ld1(v17, v18, __ T16B, __ post(key, 32));
      __ rev32(v17, __ T16B, v17);
      __ rev32(v18, __ T16B, v18);
    __ BIND(L_loadkeys_52);
      __ ld1(v19, v20, __ T16B, __ post(key, 32));
      __ rev32(v19, __ T16B, v19);
      __ rev32(v20, __ T16B, v20);
    __ BIND(L_loadkeys_44);
      __ ld1(v21, v22, v23, v24, __ T16B, __ post(key, 64));
      __ rev32(v21, __ T16B, v21);
      __ rev32(v22, __ T16B, v22);
      __ rev32(v23, __ T16B, v23);
      __ rev32(v24, __ T16B, v24);
      __ ld1(v25, v26, v27, v28, __ T16B, __ post(key, 64));
      __ rev32(v25, __ T16B, v25);
      __ rev32(v26, __ T16B, v26);
      __ rev32(v27, __ T16B, v27);
      __ rev32(v28, __ T16B, v28);
      __ ld1(v29, v30, __ T16B, key);
      __ rev32(v29, __ T16B, v29);
      __ rev32(v30, __ T16B, v30);

    __ BIND(L_aes_loop);
      __ ld1(v0, __ T16B, __ post(from, 16));
      __ orr(v1, __ T16B, v0, v0);

      __ br(Assembler::CC, L_rounds_44);
      __ br(Assembler::EQ, L_rounds_52);

      __ aesd(v0, v17); __ aesimc(v0, v0);
      __ aesd(v0, v18); __ aesimc(v0, v0);
    __ BIND(L_rounds_52);
      __ aesd(v0, v19); __ aesimc(v0, v0);
      __ aesd(v0, v20); __ aesimc(v0, v0);
    __ BIND(L_rounds_44);
      __ aesd(v0, v21); __ aesimc(v0, v0);
      __ aesd(v0, v22); __ aesimc(v0, v0);
      __ aesd(v0, v23); __ aesimc(v0, v0);
      __ aesd(v0, v24); __ aesimc(v0, v0);
      __ aesd(v0, v25); __ aesimc(v0, v0);
      __ aesd(v0, v26); __ aesimc(v0, v0);
      __ aesd(v0, v27); __ aesimc(v0, v0);
      __ aesd(v0, v28); __ aesimc(v0, v0);
      __ aesd(v0, v29); __ aesimc(v0, v0);
      __ aesd(v0, v30);
      __ eor(v0, __ T16B, v0, v31);
      __ eor(v0, __ T16B, v0, v2);

      __ st1(v0, __ T16B, __ post(to, 16));
      __ orr(v2, __ T16B, v1, v1);

      __ subw(len_reg, len_reg, 16);
      __ cbnzw(len_reg, L_aes_loop);

      __ st1(v2, __ T16B, rvec);

      __ mov(r0, rscratch2);

      __ leave();
      __ ret(lr);

    return start;
  }

  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - byte[]  source+offset
  //   c_rarg1   - int[]   SHA.state
  //   c_rarg2   - int     offset
  //   c_rarg3   - int     limit
  //
  address generate_sha1_implCompress(bool multi_block, const char *name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf   = c_rarg0;
    Register state = c_rarg1;
    Register ofs   = c_rarg2;
    Register limit = c_rarg3;

    Label keys;
    Label sha1_loop;

    // load the keys into v0..v3
    __ adr(rscratch1, keys);
    __ ld4r(v0, v1, v2, v3, __ T4S, Address(rscratch1));
    // load 5 words state into v6, v7
    __ ldrq(v6, Address(state, 0));
    __ ldrs(v7, Address(state, 16));


    __ BIND(sha1_loop);
    // load 64 bytes of data into v16..v19
    __ ld1(v16, v17, v18, v19, __ T4S, multi_block ? __ post(buf, 64) : buf);
    __ rev32(v16, __ T16B, v16);
    __ rev32(v17, __ T16B, v17);
    __ rev32(v18, __ T16B, v18);
    __ rev32(v19, __ T16B, v19);

    // do the sha1
    __ addv(v4, __ T4S, v16, v0);
    __ orr(v20, __ T16B, v6, v6);

    FloatRegister d0 = v16;
    FloatRegister d1 = v17;
    FloatRegister d2 = v18;
    FloatRegister d3 = v19;

    for (int round = 0; round < 20; round++) {
      FloatRegister tmp1 = (round & 1) ? v4 : v5;
      FloatRegister tmp2 = (round & 1) ? v21 : v22;
      FloatRegister tmp3 = round ? ((round & 1) ? v22 : v21) : v7;
      FloatRegister tmp4 = (round & 1) ? v5 : v4;
      FloatRegister key = (round < 4) ? v0 : ((round < 9) ? v1 : ((round < 14) ? v2 : v3));

      if (round < 16) __ sha1su0(d0, __ T4S, d1, d2);
      if (round < 19) __ addv(tmp1, __ T4S, d1, key);
      __ sha1h(tmp2, __ T4S, v20);
      if (round < 5)
        __ sha1c(v20, __ T4S, tmp3, tmp4);
      else if (round < 10 || round >= 15)
        __ sha1p(v20, __ T4S, tmp3, tmp4);
      else
        __ sha1m(v20, __ T4S, tmp3, tmp4);
      if (round < 16) __ sha1su1(d0, __ T4S, d3);

      tmp1 = d0; d0 = d1; d1 = d2; d2 = d3; d3 = tmp1;
    }

    __ addv(v7, __ T2S, v7, v21);
    __ addv(v6, __ T4S, v6, v20);

    if (multi_block) {
      __ add(ofs, ofs, 64);
      __ cmp(ofs, limit);
      __ br(Assembler::LE, sha1_loop);
      __ mov(c_rarg0, ofs); // return ofs
    }

    __ strq(v6, Address(state, 0));
    __ strs(v7, Address(state, 16));

    __ ret(lr);

    __ bind(keys);
    __ emit_int32(0x5a827999);
    __ emit_int32(0x6ed9eba1);
    __ emit_int32(0x8f1bbcdc);
    __ emit_int32(0xca62c1d6);

    return start;
  }


  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - byte[]  source+offset
  //   c_rarg1   - int[]   SHA.state
  //   c_rarg2   - int     offset
  //   c_rarg3   - int     limit
  //
  address generate_sha256_implCompress(bool multi_block, const char *name) {
    static const uint32_t round_consts[64] = {
      0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
      0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
      0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
      0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
      0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
      0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
      0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
      0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
      0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
      0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
      0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
      0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
      0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
      0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
      0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
      0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    };
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf   = c_rarg0;
    Register state = c_rarg1;
    Register ofs   = c_rarg2;
    Register limit = c_rarg3;

    Label sha1_loop;

    __ stpd(v8, v9, __ pre(sp, -32));
    __ stpd(v10, v11, Address(sp, 16));

// dga == v0
// dgb == v1
// dg0 == v2
// dg1 == v3
// dg2 == v4
// t0 == v6
// t1 == v7

    // load 16 keys to v16..v31
    __ lea(rscratch1, ExternalAddress((address)round_consts));
    __ ld1(v16, v17, v18, v19, __ T4S, __ post(rscratch1, 64));
    __ ld1(v20, v21, v22, v23, __ T4S, __ post(rscratch1, 64));
    __ ld1(v24, v25, v26, v27, __ T4S, __ post(rscratch1, 64));
    __ ld1(v28, v29, v30, v31, __ T4S, rscratch1);

    // load 8 words (256 bits) state
    __ ldpq(v0, v1, state);

    __ BIND(sha1_loop);
    // load 64 bytes of data into v8..v11
    __ ld1(v8, v9, v10, v11, __ T4S, multi_block ? __ post(buf, 64) : buf);
    __ rev32(v8, __ T16B, v8);
    __ rev32(v9, __ T16B, v9);
    __ rev32(v10, __ T16B, v10);
    __ rev32(v11, __ T16B, v11);

    __ addv(v6, __ T4S, v8, v16);
    __ orr(v2, __ T16B, v0, v0);
    __ orr(v3, __ T16B, v1, v1);

    FloatRegister d0 = v8;
    FloatRegister d1 = v9;
    FloatRegister d2 = v10;
    FloatRegister d3 = v11;


    for (int round = 0; round < 16; round++) {
      FloatRegister tmp1 = (round & 1) ? v6 : v7;
      FloatRegister tmp2 = (round & 1) ? v7 : v6;
      FloatRegister tmp3 = (round & 1) ? v2 : v4;
      FloatRegister tmp4 = (round & 1) ? v4 : v2;

      if (round < 12) __ sha256su0(d0, __ T4S, d1);
       __ orr(v4, __ T16B, v2, v2);
      if (round < 15)
        __ addv(tmp1, __ T4S, d1, as_FloatRegister(round + 17));
      __ sha256h(v2, __ T4S, v3, tmp2);
      __ sha256h2(v3, __ T4S, v4, tmp2);
      if (round < 12) __ sha256su1(d0, __ T4S, d2, d3);

      tmp1 = d0; d0 = d1; d1 = d2; d2 = d3; d3 = tmp1;
    }

    __ addv(v0, __ T4S, v0, v2);
    __ addv(v1, __ T4S, v1, v3);

    if (multi_block) {
      __ add(ofs, ofs, 64);
      __ cmp(ofs, limit);
      __ br(Assembler::LE, sha1_loop);
      __ mov(c_rarg0, ofs); // return ofs
    }

    __ ldpd(v10, v11, Address(sp, 16));
    __ ldpd(v8, v9, __ post(sp, 32));

    __ stpq(v0, v1, state);

    __ ret(lr);

    return start;
  }

  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - byte[]  source+offset
  //   c_rarg1   - int[]   SHA.state
  //   c_rarg2   - int     offset
  //   c_rarg3   - int     limit
  //
  address generate_sha512_implCompress(bool multi_block, const char *name) {
    static const uint64_t round_consts[80] = {
      0x428A2F98D728AE22L, 0x7137449123EF65CDL, 0xB5C0FBCFEC4D3B2FL,
      0xE9B5DBA58189DBBCL, 0x3956C25BF348B538L, 0x59F111F1B605D019L,
      0x923F82A4AF194F9BL, 0xAB1C5ED5DA6D8118L, 0xD807AA98A3030242L,
      0x12835B0145706FBEL, 0x243185BE4EE4B28CL, 0x550C7DC3D5FFB4E2L,
      0x72BE5D74F27B896FL, 0x80DEB1FE3B1696B1L, 0x9BDC06A725C71235L,
      0xC19BF174CF692694L, 0xE49B69C19EF14AD2L, 0xEFBE4786384F25E3L,
      0x0FC19DC68B8CD5B5L, 0x240CA1CC77AC9C65L, 0x2DE92C6F592B0275L,
      0x4A7484AA6EA6E483L, 0x5CB0A9DCBD41FBD4L, 0x76F988DA831153B5L,
      0x983E5152EE66DFABL, 0xA831C66D2DB43210L, 0xB00327C898FB213FL,
      0xBF597FC7BEEF0EE4L, 0xC6E00BF33DA88FC2L, 0xD5A79147930AA725L,
      0x06CA6351E003826FL, 0x142929670A0E6E70L, 0x27B70A8546D22FFCL,
      0x2E1B21385C26C926L, 0x4D2C6DFC5AC42AEDL, 0x53380D139D95B3DFL,
      0x650A73548BAF63DEL, 0x766A0ABB3C77B2A8L, 0x81C2C92E47EDAEE6L,
      0x92722C851482353BL, 0xA2BFE8A14CF10364L, 0xA81A664BBC423001L,
      0xC24B8B70D0F89791L, 0xC76C51A30654BE30L, 0xD192E819D6EF5218L,
      0xD69906245565A910L, 0xF40E35855771202AL, 0x106AA07032BBD1B8L,
      0x19A4C116B8D2D0C8L, 0x1E376C085141AB53L, 0x2748774CDF8EEB99L,
      0x34B0BCB5E19B48A8L, 0x391C0CB3C5C95A63L, 0x4ED8AA4AE3418ACBL,
      0x5B9CCA4F7763E373L, 0x682E6FF3D6B2B8A3L, 0x748F82EE5DEFB2FCL,
      0x78A5636F43172F60L, 0x84C87814A1F0AB72L, 0x8CC702081A6439ECL,
      0x90BEFFFA23631E28L, 0xA4506CEBDE82BDE9L, 0xBEF9A3F7B2C67915L,
      0xC67178F2E372532BL, 0xCA273ECEEA26619CL, 0xD186B8C721C0C207L,
      0xEADA7DD6CDE0EB1EL, 0xF57D4F7FEE6ED178L, 0x06F067AA72176FBAL,
      0x0A637DC5A2C898A6L, 0x113F9804BEF90DAEL, 0x1B710B35131C471BL,
      0x28DB77F523047D84L, 0x32CAAB7B40C72493L, 0x3C9EBE0A15C9BEBCL,
      0x431D67C49C100D4CL, 0x4CC5D4BECB3E42B6L, 0x597F299CFC657E2AL,
      0x5FCB6FAB3AD6FAECL, 0x6C44198C4A475817L
    };

    // Double rounds for sha512.
    #define sha512_dround(dr, i0, i1, i2, i3, i4, rc0, rc1, in0, in1, in2, in3, in4) \
      if (dr < 36)                                                                   \
        __ ld1(v##rc1, __ T2D, __ post(rscratch2, 16));                              \
      __ addv(v5, __ T2D, v##rc0, v##in0);                                           \
      __ ext(v6, __ T16B, v##i2, v##i3, 8);                                          \
      __ ext(v5, __ T16B, v5, v5, 8);                                                \
      __ ext(v7, __ T16B, v##i1, v##i2, 8);                                          \
      __ addv(v##i3, __ T2D, v##i3, v5);                                             \
      if (dr < 32) {                                                                 \
        __ ext(v5, __ T16B, v##in3, v##in4, 8);                                      \
        __ sha512su0(v##in0, __ T2D, v##in1);                                        \
      }                                                                              \
      __ sha512h(v##i3, __ T2D, v6, v7);                                             \
      if (dr < 32)                                                                   \
        __ sha512su1(v##in0, __ T2D, v##in2, v5);                                    \
      __ addv(v##i4, __ T2D, v##i1, v##i3);                                          \
      __ sha512h2(v##i3, __ T2D, v##i1, v##i0);                                      \

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf   = c_rarg0;
    Register state = c_rarg1;
    Register ofs   = c_rarg2;
    Register limit = c_rarg3;

    __ stpd(v8, v9, __ pre(sp, -64));
    __ stpd(v10, v11, Address(sp, 16));
    __ stpd(v12, v13, Address(sp, 32));
    __ stpd(v14, v15, Address(sp, 48));

    Label sha512_loop;

    // load state
    __ ld1(v8, v9, v10, v11, __ T2D, state);

    // load first 4 round constants
    __ lea(rscratch1, ExternalAddress((address)round_consts));
    __ ld1(v20, v21, v22, v23, __ T2D, __ post(rscratch1, 64));

    __ BIND(sha512_loop);
    // load 128B of data into v12..v19
    __ ld1(v12, v13, v14, v15, __ T2D, __ post(buf, 64));
    __ ld1(v16, v17, v18, v19, __ T2D, __ post(buf, 64));
    __ rev64(v12, __ T16B, v12);
    __ rev64(v13, __ T16B, v13);
    __ rev64(v14, __ T16B, v14);
    __ rev64(v15, __ T16B, v15);
    __ rev64(v16, __ T16B, v16);
    __ rev64(v17, __ T16B, v17);
    __ rev64(v18, __ T16B, v18);
    __ rev64(v19, __ T16B, v19);

    __ mov(rscratch2, rscratch1);

    __ mov(v0, __ T16B, v8);
    __ mov(v1, __ T16B, v9);
    __ mov(v2, __ T16B, v10);
    __ mov(v3, __ T16B, v11);

    sha512_dround( 0, 0, 1, 2, 3, 4, 20, 24, 12, 13, 19, 16, 17);
    sha512_dround( 1, 3, 0, 4, 2, 1, 21, 25, 13, 14, 12, 17, 18);
    sha512_dround( 2, 2, 3, 1, 4, 0, 22, 26, 14, 15, 13, 18, 19);
    sha512_dround( 3, 4, 2, 0, 1, 3, 23, 27, 15, 16, 14, 19, 12);
    sha512_dround( 4, 1, 4, 3, 0, 2, 24, 28, 16, 17, 15, 12, 13);
    sha512_dround( 5, 0, 1, 2, 3, 4, 25, 29, 17, 18, 16, 13, 14);
    sha512_dround( 6, 3, 0, 4, 2, 1, 26, 30, 18, 19, 17, 14, 15);
    sha512_dround( 7, 2, 3, 1, 4, 0, 27, 31, 19, 12, 18, 15, 16);
    sha512_dround( 8, 4, 2, 0, 1, 3, 28, 24, 12, 13, 19, 16, 17);
    sha512_dround( 9, 1, 4, 3, 0, 2, 29, 25, 13, 14, 12, 17, 18);
    sha512_dround(10, 0, 1, 2, 3, 4, 30, 26, 14, 15, 13, 18, 19);
    sha512_dround(11, 3, 0, 4, 2, 1, 31, 27, 15, 16, 14, 19, 12);
    sha512_dround(12, 2, 3, 1, 4, 0, 24, 28, 16, 17, 15, 12, 13);
    sha512_dround(13, 4, 2, 0, 1, 3, 25, 29, 17, 18, 16, 13, 14);
    sha512_dround(14, 1, 4, 3, 0, 2, 26, 30, 18, 19, 17, 14, 15);
    sha512_dround(15, 0, 1, 2, 3, 4, 27, 31, 19, 12, 18, 15, 16);
    sha512_dround(16, 3, 0, 4, 2, 1, 28, 24, 12, 13, 19, 16, 17);
    sha512_dround(17, 2, 3, 1, 4, 0, 29, 25, 13, 14, 12, 17, 18);
    sha512_dround(18, 4, 2, 0, 1, 3, 30, 26, 14, 15, 13, 18, 19);
    sha512_dround(19, 1, 4, 3, 0, 2, 31, 27, 15, 16, 14, 19, 12);
    sha512_dround(20, 0, 1, 2, 3, 4, 24, 28, 16, 17, 15, 12, 13);
    sha512_dround(21, 3, 0, 4, 2, 1, 25, 29, 17, 18, 16, 13, 14);
    sha512_dround(22, 2, 3, 1, 4, 0, 26, 30, 18, 19, 17, 14, 15);
    sha512_dround(23, 4, 2, 0, 1, 3, 27, 31, 19, 12, 18, 15, 16);
    sha512_dround(24, 1, 4, 3, 0, 2, 28, 24, 12, 13, 19, 16, 17);
    sha512_dround(25, 0, 1, 2, 3, 4, 29, 25, 13, 14, 12, 17, 18);
    sha512_dround(26, 3, 0, 4, 2, 1, 30, 26, 14, 15, 13, 18, 19);
    sha512_dround(27, 2, 3, 1, 4, 0, 31, 27, 15, 16, 14, 19, 12);
    sha512_dround(28, 4, 2, 0, 1, 3, 24, 28, 16, 17, 15, 12, 13);
    sha512_dround(29, 1, 4, 3, 0, 2, 25, 29, 17, 18, 16, 13, 14);
    sha512_dround(30, 0, 1, 2, 3, 4, 26, 30, 18, 19, 17, 14, 15);
    sha512_dround(31, 3, 0, 4, 2, 1, 27, 31, 19, 12, 18, 15, 16);
    sha512_dround(32, 2, 3, 1, 4, 0, 28, 24, 12,  0,  0,  0,  0);
    sha512_dround(33, 4, 2, 0, 1, 3, 29, 25, 13,  0,  0,  0,  0);
    sha512_dround(34, 1, 4, 3, 0, 2, 30, 26, 14,  0,  0,  0,  0);
    sha512_dround(35, 0, 1, 2, 3, 4, 31, 27, 15,  0,  0,  0,  0);
    sha512_dround(36, 3, 0, 4, 2, 1, 24,  0, 16,  0,  0,  0,  0);
    sha512_dround(37, 2, 3, 1, 4, 0, 25,  0, 17,  0,  0,  0,  0);
    sha512_dround(38, 4, 2, 0, 1, 3, 26,  0, 18,  0,  0,  0,  0);
    sha512_dround(39, 1, 4, 3, 0, 2, 27,  0, 19,  0,  0,  0,  0);

    __ addv(v8, __ T2D, v8, v0);
    __ addv(v9, __ T2D, v9, v1);
    __ addv(v10, __ T2D, v10, v2);
    __ addv(v11, __ T2D, v11, v3);

    if (multi_block) {
      __ add(ofs, ofs, 128);
      __ cmp(ofs, limit);
      __ br(Assembler::LE, sha512_loop);
      __ mov(c_rarg0, ofs); // return ofs
    }

    __ st1(v8, v9, v10, v11, __ T2D, state);

    __ ldpd(v14, v15, Address(sp, 48));
    __ ldpd(v12, v13, Address(sp, 32));
    __ ldpd(v10, v11, Address(sp, 16));
    __ ldpd(v8, v9, __ post(sp, 64));

    __ ret(lr);

    return start;
  }

  // Arguments:
  //
  // Inputs:
  //   c_rarg0   - byte[]  source+offset
  //   c_rarg1   - byte[]   SHA.state
  //   c_rarg2   - int     digest_length
  //   c_rarg3   - int     offset
  //   c_rarg4   - int     limit
  //
  address generate_sha3_implCompress(bool multi_block, const char *name) {
    static const uint64_t round_consts[24] = {
      0x0000000000000001L, 0x0000000000008082L, 0x800000000000808AL,
      0x8000000080008000L, 0x000000000000808BL, 0x0000000080000001L,
      0x8000000080008081L, 0x8000000000008009L, 0x000000000000008AL,
      0x0000000000000088L, 0x0000000080008009L, 0x000000008000000AL,
      0x000000008000808BL, 0x800000000000008BL, 0x8000000000008089L,
      0x8000000000008003L, 0x8000000000008002L, 0x8000000000000080L,
      0x000000000000800AL, 0x800000008000000AL, 0x8000000080008081L,
      0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L
    };

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register buf           = c_rarg0;
    Register state         = c_rarg1;
    Register digest_length = c_rarg2;
    Register ofs           = c_rarg3;
    Register limit         = c_rarg4;

    Label sha3_loop, rounds24_loop;
    Label sha3_512, sha3_384_or_224, sha3_256;

    __ stpd(v8, v9, __ pre(sp, -64));
    __ stpd(v10, v11, Address(sp, 16));
    __ stpd(v12, v13, Address(sp, 32));
    __ stpd(v14, v15, Address(sp, 48));

    // load state
    __ add(rscratch1, state, 32);
    __ ld1(v0, v1, v2,  v3,  __ T1D, state);
    __ ld1(v4, v5, v6,  v7,  __ T1D, __ post(rscratch1, 32));
    __ ld1(v8, v9, v10, v11, __ T1D, __ post(rscratch1, 32));
    __ ld1(v12, v13, v14, v15, __ T1D, __ post(rscratch1, 32));
    __ ld1(v16, v17, v18, v19, __ T1D, __ post(rscratch1, 32));
    __ ld1(v20, v21, v22, v23, __ T1D, __ post(rscratch1, 32));
    __ ld1(v24, __ T1D, rscratch1);

    __ BIND(sha3_loop);

    // 24 keccak rounds
    __ movw(rscratch2, 24);

    // load round_constants base
    __ lea(rscratch1, ExternalAddress((address) round_consts));

    // load input
    __ ld1(v25, v26, v27, v28, __ T8B, __ post(buf, 32));
    __ ld1(v29, v30, v31, __ T8B, __ post(buf, 24));
    __ eor(v0, __ T8B, v0, v25);
    __ eor(v1, __ T8B, v1, v26);
    __ eor(v2, __ T8B, v2, v27);
    __ eor(v3, __ T8B, v3, v28);
    __ eor(v4, __ T8B, v4, v29);
    __ eor(v5, __ T8B, v5, v30);
    __ eor(v6, __ T8B, v6, v31);

    // digest_length == 64, SHA3-512
    __ tbnz(digest_length, 6, sha3_512);

    __ ld1(v25, v26, v27, v28, __ T8B, __ post(buf, 32));
    __ ld1(v29, v30, __ T8B, __ post(buf, 16));
    __ eor(v7, __ T8B, v7, v25);
    __ eor(v8, __ T8B, v8, v26);
    __ eor(v9, __ T8B, v9, v27);
    __ eor(v10, __ T8B, v10, v28);
    __ eor(v11, __ T8B, v11, v29);
    __ eor(v12, __ T8B, v12, v30);

    // digest_length == 28, SHA3-224;  digest_length == 48, SHA3-384
    __ tbnz(digest_length, 4, sha3_384_or_224);

    // SHA3-256
    __ ld1(v25, v26, v27, v28, __ T8B, __ post(buf, 32));
    __ eor(v13, __ T8B, v13, v25);
    __ eor(v14, __ T8B, v14, v26);
    __ eor(v15, __ T8B, v15, v27);
    __ eor(v16, __ T8B, v16, v28);
    __ b(rounds24_loop);

    __ BIND(sha3_384_or_224);
    __ tbz(digest_length, 2, rounds24_loop); // bit 2 cleared? SHA-384

    // SHA3-224
    __ ld1(v25, v26, v27, v28, __ T8B, __ post(buf, 32));
    __ ld1(v29, __ T8B, __ post(buf, 8));
    __ eor(v13, __ T8B, v13, v25);
    __ eor(v14, __ T8B, v14, v26);
    __ eor(v15, __ T8B, v15, v27);
    __ eor(v16, __ T8B, v16, v28);
    __ eor(v17, __ T8B, v17, v29);
    __ b(rounds24_loop);

    __ BIND(sha3_512);
    __ ld1(v25, v26, __ T8B, __ post(buf, 16));
    __ eor(v7, __ T8B, v7, v25);
    __ eor(v8, __ T8B, v8, v26);

    __ BIND(rounds24_loop);
    __ subw(rscratch2, rscratch2, 1);

    __ eor3(v29, __ T16B, v4, v9, v14);
    __ eor3(v26, __ T16B, v1, v6, v11);
    __ eor3(v28, __ T16B, v3, v8, v13);
    __ eor3(v25, __ T16B, v0, v5, v10);
    __ eor3(v27, __ T16B, v2, v7, v12);
    __ eor3(v29, __ T16B, v29, v19, v24);
    __ eor3(v26, __ T16B, v26, v16, v21);
    __ eor3(v28, __ T16B, v28, v18, v23);
    __ eor3(v25, __ T16B, v25, v15, v20);
    __ eor3(v27, __ T16B, v27, v17, v22);

    __ rax1(v30, __ T2D, v29, v26);
    __ rax1(v26, __ T2D, v26, v28);
    __ rax1(v28, __ T2D, v28, v25);
    __ rax1(v25, __ T2D, v25, v27);
    __ rax1(v27, __ T2D, v27, v29);

    __ eor(v0, __ T16B, v0, v30);
    __ xar(v29, __ T2D, v1,  v25, (64 - 1));
    __ xar(v1,  __ T2D, v6,  v25, (64 - 44));
    __ xar(v6,  __ T2D, v9,  v28, (64 - 20));
    __ xar(v9,  __ T2D, v22, v26, (64 - 61));
    __ xar(v22, __ T2D, v14, v28, (64 - 39));
    __ xar(v14, __ T2D, v20, v30, (64 - 18));
    __ xar(v31, __ T2D, v2,  v26, (64 - 62));
    __ xar(v2,  __ T2D, v12, v26, (64 - 43));
    __ xar(v12, __ T2D, v13, v27, (64 - 25));
    __ xar(v13, __ T2D, v19, v28, (64 - 8));
    __ xar(v19, __ T2D, v23, v27, (64 - 56));
    __ xar(v23, __ T2D, v15, v30, (64 - 41));
    __ xar(v15, __ T2D, v4,  v28, (64 - 27));
    __ xar(v28, __ T2D, v24, v28, (64 - 14));
    __ xar(v24, __ T2D, v21, v25, (64 - 2));
    __ xar(v8,  __ T2D, v8,  v27, (64 - 55));
    __ xar(v4,  __ T2D, v16, v25, (64 - 45));
    __ xar(v16, __ T2D, v5,  v30, (64 - 36));
    __ xar(v5,  __ T2D, v3,  v27, (64 - 28));
    __ xar(v27, __ T2D, v18, v27, (64 - 21));
    __ xar(v3,  __ T2D, v17, v26, (64 - 15));
    __ xar(v25, __ T2D, v11, v25, (64 - 10));
    __ xar(v26, __ T2D, v7,  v26, (64 - 6));
    __ xar(v30, __ T2D, v10, v30, (64 - 3));

    __ bcax(v20, __ T16B, v31, v22, v8);
    __ bcax(v21, __ T16B, v8,  v23, v22);
    __ bcax(v22, __ T16B, v22, v24, v23);
    __ bcax(v23, __ T16B, v23, v31, v24);
    __ bcax(v24, __ T16B, v24, v8,  v31);

    __ ld1r(v31, __ T2D, __ post(rscratch1, 8));

    __ bcax(v17, __ T16B, v25, v19, v3);
    __ bcax(v18, __ T16B, v3,  v15, v19);
    __ bcax(v19, __ T16B, v19, v16, v15);
    __ bcax(v15, __ T16B, v15, v25, v16);
    __ bcax(v16, __ T16B, v16, v3,  v25);

    __ bcax(v10, __ T16B, v29, v12, v26);
    __ bcax(v11, __ T16B, v26, v13, v12);
    __ bcax(v12, __ T16B, v12, v14, v13);
    __ bcax(v13, __ T16B, v13, v29, v14);
    __ bcax(v14, __ T16B, v14, v26, v29);

    __ bcax(v7, __ T16B, v30, v9,  v4);
    __ bcax(v8, __ T16B, v4,  v5,  v9);
    __ bcax(v9, __ T16B, v9,  v6,  v5);
    __ bcax(v5, __ T16B, v5,  v30, v6);
    __ bcax(v6, __ T16B, v6,  v4,  v30);

    __ bcax(v3, __ T16B, v27, v0,  v28);
    __ bcax(v4, __ T16B, v28, v1,  v0);
    __ bcax(v0, __ T16B, v0,  v2,  v1);
    __ bcax(v1, __ T16B, v1,  v27, v2);
    __ bcax(v2, __ T16B, v2,  v28, v27);

    __ eor(v0, __ T16B, v0, v31);

    __ cbnzw(rscratch2, rounds24_loop);

    if (multi_block) {
      // block_size =  200 - 2 * digest_length, ofs += block_size
      __ add(ofs, ofs, 200);
      __ sub(ofs, ofs, digest_length, Assembler::LSL, 1);

      __ cmp(ofs, limit);
      __ br(Assembler::LE, sha3_loop);
      __ mov(c_rarg0, ofs); // return ofs
    }

    __ st1(v0, v1, v2,  v3,  __ T1D, __ post(state, 32));
    __ st1(v4, v5, v6,  v7,  __ T1D, __ post(state, 32));
    __ st1(v8, v9, v10, v11, __ T1D, __ post(state, 32));
    __ st1(v12, v13, v14, v15, __ T1D, __ post(state, 32));
    __ st1(v16, v17, v18, v19, __ T1D, __ post(state, 32));
    __ st1(v20, v21, v22, v23, __ T1D, __ post(state, 32));
    __ st1(v24, __ T1D, state);

    __ ldpd(v14, v15, Address(sp, 48));
    __ ldpd(v12, v13, Address(sp, 32));
    __ ldpd(v10, v11, Address(sp, 16));
    __ ldpd(v8, v9, __ post(sp, 64));

    __ ret(lr);

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
        __ ldrw(c_rarg1, Address(c_rarg0, 0));
        break;
      case 8:
        // int64_t
        __ ldr(c_rarg1, Address(c_rarg0, 0));
        break;
      default:
        ShouldNotReachHere();
    }

    // return errValue or *adr
    *continuation_pc = __ pc();
    __ mov(r0, c_rarg1);
    __ ret(lr);
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
    assert(UseCRC32Intrinsics, "what are we doing here?");

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "updateBytesCRC32");

    address start = __ pc();

    const Register crc   = c_rarg0;  // crc
    const Register buf   = c_rarg1;  // source java byte array address
    const Register len   = c_rarg2;  // length
    const Register table0 = c_rarg3; // crc_table address
    const Register table1 = c_rarg4;
    const Register table2 = c_rarg5;
    const Register table3 = c_rarg6;
    const Register tmp3 = c_rarg7;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ kernel_crc32(crc, buf, len,
              table0, table1, table2, table3, rscratch1, rscratch2, tmp3);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(lr);

    return start;
  }

  /**
   *  Arguments:
   *
   * Inputs:
   *   c_rarg0   - int crc
   *   c_rarg1   - byte* buf
   *   c_rarg2   - int length
   *   c_rarg3   - int* table
   *
   * Ouput:
   *       r0   - int crc result
   */
  address generate_updateBytesCRC32C() {
    assert(UseCRC32CIntrinsics, "what are we doing here?");

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "updateBytesCRC32C");

    address start = __ pc();

    const Register crc   = c_rarg0;  // crc
    const Register buf   = c_rarg1;  // source java byte array address
    const Register len   = c_rarg2;  // length
    const Register table0 = c_rarg3; // crc_table address
    const Register table1 = c_rarg4;
    const Register table2 = c_rarg5;
    const Register table3 = c_rarg6;
    const Register tmp3 = c_rarg7;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame

    __ kernel_crc32c(crc, buf, len,
              table0, table1, table2, table3, rscratch1, rscratch2, tmp3);

    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(lr);

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
   *   c_rarg0   - int adler result
   */
  address generate_updateBytesAdler32() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "updateBytesAdler32");
    address start = __ pc();

    Label L_simple_by1_loop, L_nmax, L_nmax_loop, L_by16, L_by16_loop, L_by1_loop, L_do_mod, L_combine, L_by1;

    // Aliases
    Register adler  = c_rarg0;
    Register s1     = c_rarg0;
    Register s2     = c_rarg3;
    Register buff   = c_rarg1;
    Register len    = c_rarg2;
    Register nmax  = r4;
    Register base  = r5;
    Register count = r6;
    Register temp0 = rscratch1;
    Register temp1 = rscratch2;
    FloatRegister vbytes = v0;
    FloatRegister vs1acc = v1;
    FloatRegister vs2acc = v2;
    FloatRegister vtable = v3;

    // Max number of bytes we can process before having to take the mod
    // 0x15B0 is 5552 in decimal, the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
    uint64_t BASE = 0xfff1;
    uint64_t NMAX = 0x15B0;

    __ mov(base, BASE);
    __ mov(nmax, NMAX);

    // Load accumulation coefficients for the upper 16 bits
    __ lea(temp0, ExternalAddress((address) StubRoutines::aarch64::_adler_table));
    __ ld1(vtable, __ T16B, Address(temp0));

    // s1 is initialized to the lower 16 bits of adler
    // s2 is initialized to the upper 16 bits of adler
    __ ubfx(s2, adler, 16, 16);  // s2 = ((adler >> 16) & 0xffff)
    __ uxth(s1, adler);          // s1 = (adler & 0xffff)

    // The pipelined loop needs at least 16 elements for 1 iteration
    // It does check this, but it is more effective to skip to the cleanup loop
    __ cmp(len, (u1)16);
    __ br(Assembler::HS, L_nmax);
    __ cbz(len, L_combine);

    __ bind(L_simple_by1_loop);
    __ ldrb(temp0, Address(__ post(buff, 1)));
    __ add(s1, s1, temp0);
    __ add(s2, s2, s1);
    __ subs(len, len, 1);
    __ br(Assembler::HI, L_simple_by1_loop);

    // s1 = s1 % BASE
    __ subs(temp0, s1, base);
    __ csel(s1, temp0, s1, Assembler::HS);

    // s2 = s2 % BASE
    __ lsr(temp0, s2, 16);
    __ lsl(temp1, temp0, 4);
    __ sub(temp1, temp1, temp0);
    __ add(s2, temp1, s2, ext::uxth);

    __ subs(temp0, s2, base);
    __ csel(s2, temp0, s2, Assembler::HS);

    __ b(L_combine);

    __ bind(L_nmax);
    __ subs(len, len, nmax);
    __ sub(count, nmax, 16);
    __ br(Assembler::LO, L_by16);

    __ bind(L_nmax_loop);

    generate_updateBytesAdler32_accum(s1, s2, buff, temp0, temp1,
                                      vbytes, vs1acc, vs2acc, vtable);

    __ subs(count, count, 16);
    __ br(Assembler::HS, L_nmax_loop);

    // s1 = s1 % BASE
    __ lsr(temp0, s1, 16);
    __ lsl(temp1, temp0, 4);
    __ sub(temp1, temp1, temp0);
    __ add(temp1, temp1, s1, ext::uxth);

    __ lsr(temp0, temp1, 16);
    __ lsl(s1, temp0, 4);
    __ sub(s1, s1, temp0);
    __ add(s1, s1, temp1, ext:: uxth);

    __ subs(temp0, s1, base);
    __ csel(s1, temp0, s1, Assembler::HS);

    // s2 = s2 % BASE
    __ lsr(temp0, s2, 16);
    __ lsl(temp1, temp0, 4);
    __ sub(temp1, temp1, temp0);
    __ add(temp1, temp1, s2, ext::uxth);

    __ lsr(temp0, temp1, 16);
    __ lsl(s2, temp0, 4);
    __ sub(s2, s2, temp0);
    __ add(s2, s2, temp1, ext:: uxth);

    __ subs(temp0, s2, base);
    __ csel(s2, temp0, s2, Assembler::HS);

    __ subs(len, len, nmax);
    __ sub(count, nmax, 16);
    __ br(Assembler::HS, L_nmax_loop);

    __ bind(L_by16);
    __ adds(len, len, count);
    __ br(Assembler::LO, L_by1);

    __ bind(L_by16_loop);

    generate_updateBytesAdler32_accum(s1, s2, buff, temp0, temp1,
                                      vbytes, vs1acc, vs2acc, vtable);

    __ subs(len, len, 16);
    __ br(Assembler::HS, L_by16_loop);

    __ bind(L_by1);
    __ adds(len, len, 15);
    __ br(Assembler::LO, L_do_mod);

    __ bind(L_by1_loop);
    __ ldrb(temp0, Address(__ post(buff, 1)));
    __ add(s1, temp0, s1);
    __ add(s2, s2, s1);
    __ subs(len, len, 1);
    __ br(Assembler::HS, L_by1_loop);

    __ bind(L_do_mod);
    // s1 = s1 % BASE
    __ lsr(temp0, s1, 16);
    __ lsl(temp1, temp0, 4);
    __ sub(temp1, temp1, temp0);
    __ add(temp1, temp1, s1, ext::uxth);

    __ lsr(temp0, temp1, 16);
    __ lsl(s1, temp0, 4);
    __ sub(s1, s1, temp0);
    __ add(s1, s1, temp1, ext:: uxth);

    __ subs(temp0, s1, base);
    __ csel(s1, temp0, s1, Assembler::HS);

    // s2 = s2 % BASE
    __ lsr(temp0, s2, 16);
    __ lsl(temp1, temp0, 4);
    __ sub(temp1, temp1, temp0);
    __ add(temp1, temp1, s2, ext::uxth);

    __ lsr(temp0, temp1, 16);
    __ lsl(s2, temp0, 4);
    __ sub(s2, s2, temp0);
    __ add(s2, s2, temp1, ext:: uxth);

    __ subs(temp0, s2, base);
    __ csel(s2, temp0, s2, Assembler::HS);

    // Combine lower bits and higher bits
    __ bind(L_combine);
    __ orr(s1, s1, s2, Assembler::LSL, 16); // adler = s1 | (s2 << 16)

    __ ret(lr);

    return start;
  }

  void generate_updateBytesAdler32_accum(Register s1, Register s2, Register buff,
          Register temp0, Register temp1, FloatRegister vbytes,
          FloatRegister vs1acc, FloatRegister vs2acc, FloatRegister vtable) {
    // Below is a vectorized implementation of updating s1 and s2 for 16 bytes.
    // We use b1, b2, ..., b16 to denote the 16 bytes loaded in each iteration.
    // In non-vectorized code, we update s1 and s2 as:
    //   s1 <- s1 + b1
    //   s2 <- s2 + s1
    //   s1 <- s1 + b2
    //   s2 <- s2 + b1
    //   ...
    //   s1 <- s1 + b16
    //   s2 <- s2 + s1
    // Putting above assignments together, we have:
    //   s1_new = s1 + b1 + b2 + ... + b16
    //   s2_new = s2 + (s1 + b1) + (s1 + b1 + b2) + ... + (s1 + b1 + b2 + ... + b16)
    //          = s2 + s1 * 16 + (b1 * 16 + b2 * 15 + ... + b16 * 1)
    //          = s2 + s1 * 16 + (b1, b2, ... b16) dot (16, 15, ... 1)
    __ ld1(vbytes, __ T16B, Address(__ post(buff, 16)));

    // s2 = s2 + s1 * 16
    __ add(s2, s2, s1, Assembler::LSL, 4);

    // vs1acc = b1 + b2 + b3 + ... + b16
    // vs2acc = (b1 * 16) + (b2 * 15) + (b3 * 14) + ... + (b16 * 1)
    __ umullv(vs2acc, __ T8B, vtable, vbytes);
    __ umlalv(vs2acc, __ T16B, vtable, vbytes);
    __ uaddlv(vs1acc, __ T16B, vbytes);
    __ uaddlv(vs2acc, __ T8H, vs2acc);

    // s1 = s1 + vs1acc, s2 = s2 + vs2acc
    __ fmovd(temp0, vs1acc);
    __ fmovd(temp1, vs2acc);
    __ add(s1, s1, temp0);
    __ add(s2, s2, temp1);
  }

  /**
   *  Arguments:
   *
   *  Input:
   *    c_rarg0   - x address
   *    c_rarg1   - x length
   *    c_rarg2   - y address
   *    c_rarg3   - y lenth
   *    c_rarg4   - z address
   *    c_rarg5   - z length
   */
  address generate_multiplyToLen() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "multiplyToLen");

    address start = __ pc();
    const Register x     = r0;
    const Register xlen  = r1;
    const Register y     = r2;
    const Register ylen  = r3;
    const Register z     = r4;
    const Register zlen  = r5;

    const Register tmp1  = r10;
    const Register tmp2  = r11;
    const Register tmp3  = r12;
    const Register tmp4  = r13;
    const Register tmp5  = r14;
    const Register tmp6  = r15;
    const Register tmp7  = r16;

    BLOCK_COMMENT("Entry:");
    __ enter(); // required for proper stackwalking of RuntimeStub frame
    __ multiply_to_len(x, xlen, y, ylen, z, zlen, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7);
    __ leave(); // required for proper stackwalking of RuntimeStub frame
    __ ret(lr);

    return start;
  }

  address generate_squareToLen() {
    // squareToLen algorithm for sizes 1..127 described in java code works
    // faster than multiply_to_len on some CPUs and slower on others, but
    // multiply_to_len shows a bit better overall results
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "squareToLen");
    address start = __ pc();

    const Register x     = r0;
    const Register xlen  = r1;
    const Register z     = r2;
    const Register zlen  = r3;
    const Register y     = r4; // == x
    const Register ylen  = r5; // == xlen

    const Register tmp1  = r10;
    const Register tmp2  = r11;
    const Register tmp3  = r12;
    const Register tmp4  = r13;
    const Register tmp5  = r14;
    const Register tmp6  = r15;
    const Register tmp7  = r16;

    RegSet spilled_regs = RegSet::of(y, ylen);
    BLOCK_COMMENT("Entry:");
    __ enter();
    __ push(spilled_regs, sp);
    __ mov(y, x);
    __ mov(ylen, xlen);
    __ multiply_to_len(x, xlen, y, ylen, z, zlen, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7);
    __ pop(spilled_regs, sp);
    __ leave();
    __ ret(lr);
    return start;
  }

  address generate_mulAdd() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "mulAdd");

    address start = __ pc();

    const Register out     = r0;
    const Register in      = r1;
    const Register offset  = r2;
    const Register len     = r3;
    const Register k       = r4;

    BLOCK_COMMENT("Entry:");
    __ enter();
    __ mul_add(out, in, offset, len, k);
    __ leave();
    __ ret(lr);

    return start;
  }

  // Arguments:
  //
  // Input:
  //   c_rarg0   - newArr address
  //   c_rarg1   - oldArr address
  //   c_rarg2   - newIdx
  //   c_rarg3   - shiftCount
  //   c_rarg4   - numIter
  //
  address generate_bigIntegerRightShift() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this,  "StubRoutines", "bigIntegerRightShiftWorker");
    address start = __ pc();

    Label ShiftSIMDLoop, ShiftTwoLoop, ShiftThree, ShiftTwo, ShiftOne, Exit;

    Register newArr        = c_rarg0;
    Register oldArr        = c_rarg1;
    Register newIdx        = c_rarg2;
    Register shiftCount    = c_rarg3;
    Register numIter       = c_rarg4;
    Register idx           = numIter;

    Register newArrCur     = rscratch1;
    Register shiftRevCount = rscratch2;
    Register oldArrCur     = r13;
    Register oldArrNext    = r14;

    FloatRegister oldElem0        = v0;
    FloatRegister oldElem1        = v1;
    FloatRegister newElem         = v2;
    FloatRegister shiftVCount     = v3;
    FloatRegister shiftVRevCount  = v4;

    __ cbz(idx, Exit);

    __ add(newArr, newArr, newIdx, Assembler::LSL, 2);

    // left shift count
    __ movw(shiftRevCount, 32);
    __ subw(shiftRevCount, shiftRevCount, shiftCount);

    // numIter too small to allow a 4-words SIMD loop, rolling back
    __ cmp(numIter, (u1)4);
    __ br(Assembler::LT, ShiftThree);

    __ dup(shiftVCount,    __ T4S, shiftCount);
    __ dup(shiftVRevCount, __ T4S, shiftRevCount);
    __ negr(shiftVCount,   __ T4S, shiftVCount);

    __ BIND(ShiftSIMDLoop);

    // Calculate the load addresses
    __ sub(idx, idx, 4);
    __ add(oldArrNext, oldArr, idx, Assembler::LSL, 2);
    __ add(newArrCur,  newArr, idx, Assembler::LSL, 2);
    __ add(oldArrCur,  oldArrNext, 4);

    // Load 4 words and process
    __ ld1(oldElem0,  __ T4S,  Address(oldArrCur));
    __ ld1(oldElem1,  __ T4S,  Address(oldArrNext));
    __ ushl(oldElem0, __ T4S,  oldElem0, shiftVCount);
    __ ushl(oldElem1, __ T4S,  oldElem1, shiftVRevCount);
    __ orr(newElem,   __ T16B, oldElem0, oldElem1);
    __ st1(newElem,   __ T4S,  Address(newArrCur));

    __ cmp(idx, (u1)4);
    __ br(Assembler::LT, ShiftTwoLoop);
    __ b(ShiftSIMDLoop);

    __ BIND(ShiftTwoLoop);
    __ cbz(idx, Exit);
    __ cmp(idx, (u1)1);
    __ br(Assembler::EQ, ShiftOne);

    // Calculate the load addresses
    __ sub(idx, idx, 2);
    __ add(oldArrNext, oldArr, idx, Assembler::LSL, 2);
    __ add(newArrCur,  newArr, idx, Assembler::LSL, 2);
    __ add(oldArrCur,  oldArrNext, 4);

    // Load 2 words and process
    __ ld1(oldElem0,  __ T2S, Address(oldArrCur));
    __ ld1(oldElem1,  __ T2S, Address(oldArrNext));
    __ ushl(oldElem0, __ T2S, oldElem0, shiftVCount);
    __ ushl(oldElem1, __ T2S, oldElem1, shiftVRevCount);
    __ orr(newElem,   __ T8B, oldElem0, oldElem1);
    __ st1(newElem,   __ T2S, Address(newArrCur));
    __ b(ShiftTwoLoop);

    __ BIND(ShiftThree);
    __ tbz(idx, 1, ShiftOne);
    __ tbz(idx, 0, ShiftTwo);
    __ ldrw(r10,  Address(oldArr, 12));
    __ ldrw(r11,  Address(oldArr, 8));
    __ lsrvw(r10, r10, shiftCount);
    __ lslvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  Address(newArr, 8));

    __ BIND(ShiftTwo);
    __ ldrw(r10,  Address(oldArr, 8));
    __ ldrw(r11,  Address(oldArr, 4));
    __ lsrvw(r10, r10, shiftCount);
    __ lslvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  Address(newArr, 4));

    __ BIND(ShiftOne);
    __ ldrw(r10,  Address(oldArr, 4));
    __ ldrw(r11,  Address(oldArr));
    __ lsrvw(r10, r10, shiftCount);
    __ lslvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  Address(newArr));

    __ BIND(Exit);
    __ ret(lr);

    return start;
  }

  // Arguments:
  //
  // Input:
  //   c_rarg0   - newArr address
  //   c_rarg1   - oldArr address
  //   c_rarg2   - newIdx
  //   c_rarg3   - shiftCount
  //   c_rarg4   - numIter
  //
  address generate_bigIntegerLeftShift() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this,  "StubRoutines", "bigIntegerLeftShiftWorker");
    address start = __ pc();

    Label ShiftSIMDLoop, ShiftTwoLoop, ShiftThree, ShiftTwo, ShiftOne, Exit;

    Register newArr        = c_rarg0;
    Register oldArr        = c_rarg1;
    Register newIdx        = c_rarg2;
    Register shiftCount    = c_rarg3;
    Register numIter       = c_rarg4;

    Register shiftRevCount = rscratch1;
    Register oldArrNext    = rscratch2;

    FloatRegister oldElem0        = v0;
    FloatRegister oldElem1        = v1;
    FloatRegister newElem         = v2;
    FloatRegister shiftVCount     = v3;
    FloatRegister shiftVRevCount  = v4;

    __ cbz(numIter, Exit);

    __ add(oldArrNext, oldArr, 4);
    __ add(newArr, newArr, newIdx, Assembler::LSL, 2);

    // right shift count
    __ movw(shiftRevCount, 32);
    __ subw(shiftRevCount, shiftRevCount, shiftCount);

    // numIter too small to allow a 4-words SIMD loop, rolling back
    __ cmp(numIter, (u1)4);
    __ br(Assembler::LT, ShiftThree);

    __ dup(shiftVCount,     __ T4S, shiftCount);
    __ dup(shiftVRevCount,  __ T4S, shiftRevCount);
    __ negr(shiftVRevCount, __ T4S, shiftVRevCount);

    __ BIND(ShiftSIMDLoop);

    // load 4 words and process
    __ ld1(oldElem0,  __ T4S,  __ post(oldArr, 16));
    __ ld1(oldElem1,  __ T4S,  __ post(oldArrNext, 16));
    __ ushl(oldElem0, __ T4S,  oldElem0, shiftVCount);
    __ ushl(oldElem1, __ T4S,  oldElem1, shiftVRevCount);
    __ orr(newElem,   __ T16B, oldElem0, oldElem1);
    __ st1(newElem,   __ T4S,  __ post(newArr, 16));
    __ sub(numIter,   numIter, 4);

    __ cmp(numIter, (u1)4);
    __ br(Assembler::LT, ShiftTwoLoop);
    __ b(ShiftSIMDLoop);

    __ BIND(ShiftTwoLoop);
    __ cbz(numIter, Exit);
    __ cmp(numIter, (u1)1);
    __ br(Assembler::EQ, ShiftOne);

    // load 2 words and process
    __ ld1(oldElem0,  __ T2S,  __ post(oldArr, 8));
    __ ld1(oldElem1,  __ T2S,  __ post(oldArrNext, 8));
    __ ushl(oldElem0, __ T2S,  oldElem0, shiftVCount);
    __ ushl(oldElem1, __ T2S,  oldElem1, shiftVRevCount);
    __ orr(newElem,   __ T8B,  oldElem0, oldElem1);
    __ st1(newElem,   __ T2S,  __ post(newArr, 8));
    __ sub(numIter,   numIter, 2);
    __ b(ShiftTwoLoop);

    __ BIND(ShiftThree);
    __ ldrw(r10,  __ post(oldArr, 4));
    __ ldrw(r11,  __ post(oldArrNext, 4));
    __ lslvw(r10, r10, shiftCount);
    __ lsrvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  __ post(newArr, 4));
    __ tbz(numIter, 1, Exit);
    __ tbz(numIter, 0, ShiftOne);

    __ BIND(ShiftTwo);
    __ ldrw(r10,  __ post(oldArr, 4));
    __ ldrw(r11,  __ post(oldArrNext, 4));
    __ lslvw(r10, r10, shiftCount);
    __ lsrvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  __ post(newArr, 4));

    __ BIND(ShiftOne);
    __ ldrw(r10,  Address(oldArr));
    __ ldrw(r11,  Address(oldArrNext));
    __ lslvw(r10, r10, shiftCount);
    __ lsrvw(r11, r11, shiftRevCount);
    __ orrw(r12,  r10, r11);
    __ strw(r12,  Address(newArr));

    __ BIND(Exit);
    __ ret(lr);

    return start;
  }

  void ghash_multiply(FloatRegister result_lo, FloatRegister result_hi,
                      FloatRegister a, FloatRegister b, FloatRegister a1_xor_a0,
                      FloatRegister tmp1, FloatRegister tmp2, FloatRegister tmp3, FloatRegister tmp4) {
    // Karatsuba multiplication performs a 128*128 -> 256-bit
    // multiplication in three 128-bit multiplications and a few
    // additions.
    //
    // (C1:C0) = A1*B1, (D1:D0) = A0*B0, (E1:E0) = (A0+A1)(B0+B1)
    // (A1:A0)(B1:B0) = C1:(C0+C1+D1+E1):(D1+C0+D0+E0):D0
    //
    // Inputs:
    //
    // A0 in a.d[0]     (subkey)
    // A1 in a.d[1]
    // (A1+A0) in a1_xor_a0.d[0]
    //
    // B0 in b.d[0]     (state)
    // B1 in b.d[1]

    __ ext(tmp1, __ T16B, b, b, 0x08);
    __ pmull2(result_hi, __ T1Q, b, a, __ T2D);  // A1*B1
    __ eor(tmp1, __ T16B, tmp1, b);            // (B1+B0)
    __ pmull(result_lo,  __ T1Q, b, a, __ T1D);  // A0*B0
    __ pmull(tmp2, __ T1Q, tmp1, a1_xor_a0, __ T1D); // (A1+A0)(B1+B0)

    __ ext(tmp4, __ T16B, result_lo, result_hi, 0x08);
    __ eor(tmp3, __ T16B, result_hi, result_lo); // A1*B1+A0*B0
    __ eor(tmp2, __ T16B, tmp2, tmp4);
    __ eor(tmp2, __ T16B, tmp2, tmp3);

    // Register pair <result_hi:result_lo> holds the result of carry-less multiplication
    __ ins(result_hi, __ D, tmp2, 0, 1);
    __ ins(result_lo, __ D, tmp2, 1, 0);
  }

  void ghash_reduce(FloatRegister result, FloatRegister lo, FloatRegister hi,
                    FloatRegister p, FloatRegister z, FloatRegister t1) {
    const FloatRegister t0 = result;

    // The GCM field polynomial f is z^128 + p(z), where p =
    // z^7+z^2+z+1.
    //
    //    z^128 === -p(z)  (mod (z^128 + p(z)))
    //
    // so, given that the product we're reducing is
    //    a == lo + hi * z^128
    // substituting,
    //      === lo - hi * p(z)  (mod (z^128 + p(z)))
    //
    // we reduce by multiplying hi by p(z) and subtracting the result
    // from (i.e. XORing it with) lo.  Because p has no nonzero high
    // bits we can do this with two 64-bit multiplications, lo*p and
    // hi*p.

    __ pmull2(t0, __ T1Q, hi, p, __ T2D);
    __ ext(t1, __ T16B, t0, z, 8);
    __ eor(hi, __ T16B, hi, t1);
    __ ext(t1, __ T16B, z, t0, 8);
    __ eor(lo, __ T16B, lo, t1);
    __ pmull(t0, __ T1Q, hi, p, __ T1D);
    __ eor(result, __ T16B, lo, t0);
  }

  address generate_has_negatives(address &has_negatives_long) {
    const u1 large_loop_size = 64;
    const uint64_t UPPER_BIT_MASK=0x8080808080808080;
    int dcache_line = VM_Version::dcache_line_size();

    Register ary1 = r1, len = r2, result = r0;

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "has_negatives");

    address entry = __ pc();

    __ enter();

  Label RET_TRUE, RET_TRUE_NO_POP, RET_FALSE, ALIGNED, LOOP16, CHECK_16, DONE,
        LARGE_LOOP, POST_LOOP16, LEN_OVER_15, LEN_OVER_8, POST_LOOP16_LOAD_TAIL;

  __ cmp(len, (u1)15);
  __ br(Assembler::GT, LEN_OVER_15);
  // The only case when execution falls into this code is when pointer is near
  // the end of memory page and we have to avoid reading next page
  __ add(ary1, ary1, len);
  __ subs(len, len, 8);
  __ br(Assembler::GT, LEN_OVER_8);
  __ ldr(rscratch2, Address(ary1, -8));
  __ sub(rscratch1, zr, len, __ LSL, 3);  // LSL 3 is to get bits from bytes.
  __ lsrv(rscratch2, rscratch2, rscratch1);
  __ tst(rscratch2, UPPER_BIT_MASK);
  __ cset(result, Assembler::NE);
  __ leave();
  __ ret(lr);
  __ bind(LEN_OVER_8);
  __ ldp(rscratch1, rscratch2, Address(ary1, -16));
  __ sub(len, len, 8); // no data dep., then sub can be executed while loading
  __ tst(rscratch2, UPPER_BIT_MASK);
  __ br(Assembler::NE, RET_TRUE_NO_POP);
  __ sub(rscratch2, zr, len, __ LSL, 3); // LSL 3 is to get bits from bytes
  __ lsrv(rscratch1, rscratch1, rscratch2);
  __ tst(rscratch1, UPPER_BIT_MASK);
  __ cset(result, Assembler::NE);
  __ leave();
  __ ret(lr);

  Register tmp1 = r3, tmp2 = r4, tmp3 = r5, tmp4 = r6, tmp5 = r7, tmp6 = r10;
  const RegSet spilled_regs = RegSet::range(tmp1, tmp5) + tmp6;

  has_negatives_long = __ pc(); // 2nd entry point

  __ enter();

  __ bind(LEN_OVER_15);
    __ push(spilled_regs, sp);
    __ andr(rscratch2, ary1, 15); // check pointer for 16-byte alignment
    __ cbz(rscratch2, ALIGNED);
    __ ldp(tmp6, tmp1, Address(ary1));
    __ mov(tmp5, 16);
    __ sub(rscratch1, tmp5, rscratch2); // amount of bytes until aligned address
    __ add(ary1, ary1, rscratch1);
    __ sub(len, len, rscratch1);
    __ orr(tmp6, tmp6, tmp1);
    __ tst(tmp6, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);

  __ bind(ALIGNED);
    __ cmp(len, large_loop_size);
    __ br(Assembler::LT, CHECK_16);
    // Perform 16-byte load as early return in pre-loop to handle situation
    // when initially aligned large array has negative values at starting bytes,
    // so LARGE_LOOP would do 4 reads instead of 1 (in worst case), which is
    // slower. Cases with negative bytes further ahead won't be affected that
    // much. In fact, it'll be faster due to early loads, less instructions and
    // less branches in LARGE_LOOP.
    __ ldp(tmp6, tmp1, Address(__ post(ary1, 16)));
    __ sub(len, len, 16);
    __ orr(tmp6, tmp6, tmp1);
    __ tst(tmp6, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);
    __ cmp(len, large_loop_size);
    __ br(Assembler::LT, CHECK_16);

    if (SoftwarePrefetchHintDistance >= 0
        && SoftwarePrefetchHintDistance >= dcache_line) {
      // initial prefetch
      __ prfm(Address(ary1, SoftwarePrefetchHintDistance - dcache_line));
    }
  __ bind(LARGE_LOOP);
    if (SoftwarePrefetchHintDistance >= 0) {
      __ prfm(Address(ary1, SoftwarePrefetchHintDistance));
    }
    // Issue load instructions first, since it can save few CPU/MEM cycles, also
    // instead of 4 triples of "orr(...), addr(...);cbnz(...);" (for each ldp)
    // better generate 7 * orr(...) + 1 andr(...) + 1 cbnz(...) which saves 3
    // instructions per cycle and have less branches, but this approach disables
    // early return, thus, all 64 bytes are loaded and checked every time.
    __ ldp(tmp2, tmp3, Address(ary1));
    __ ldp(tmp4, tmp5, Address(ary1, 16));
    __ ldp(rscratch1, rscratch2, Address(ary1, 32));
    __ ldp(tmp6, tmp1, Address(ary1, 48));
    __ add(ary1, ary1, large_loop_size);
    __ sub(len, len, large_loop_size);
    __ orr(tmp2, tmp2, tmp3);
    __ orr(tmp4, tmp4, tmp5);
    __ orr(rscratch1, rscratch1, rscratch2);
    __ orr(tmp6, tmp6, tmp1);
    __ orr(tmp2, tmp2, tmp4);
    __ orr(rscratch1, rscratch1, tmp6);
    __ orr(tmp2, tmp2, rscratch1);
    __ tst(tmp2, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);
    __ cmp(len, large_loop_size);
    __ br(Assembler::GE, LARGE_LOOP);

  __ bind(CHECK_16); // small 16-byte load pre-loop
    __ cmp(len, (u1)16);
    __ br(Assembler::LT, POST_LOOP16);

  __ bind(LOOP16); // small 16-byte load loop
    __ ldp(tmp2, tmp3, Address(__ post(ary1, 16)));
    __ sub(len, len, 16);
    __ orr(tmp2, tmp2, tmp3);
    __ tst(tmp2, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);
    __ cmp(len, (u1)16);
    __ br(Assembler::GE, LOOP16); // 16-byte load loop end

  __ bind(POST_LOOP16); // 16-byte aligned, so we can read unconditionally
    __ cmp(len, (u1)8);
    __ br(Assembler::LE, POST_LOOP16_LOAD_TAIL);
    __ ldr(tmp3, Address(__ post(ary1, 8)));
    __ sub(len, len, 8);
    __ tst(tmp3, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);

  __ bind(POST_LOOP16_LOAD_TAIL);
    __ cbz(len, RET_FALSE); // Can't shift left by 64 when len==0
    __ ldr(tmp1, Address(ary1));
    __ mov(tmp2, 64);
    __ sub(tmp4, tmp2, len, __ LSL, 3);
    __ lslv(tmp1, tmp1, tmp4);
    __ tst(tmp1, UPPER_BIT_MASK);
    __ br(Assembler::NE, RET_TRUE);
    // Fallthrough

  __ bind(RET_FALSE);
    __ pop(spilled_regs, sp);
    __ leave();
    __ mov(result, zr);
    __ ret(lr);

  __ bind(RET_TRUE);
    __ pop(spilled_regs, sp);
  __ bind(RET_TRUE_NO_POP);
    __ leave();
    __ mov(result, 1);
    __ ret(lr);

  __ bind(DONE);
    __ pop(spilled_regs, sp);
    __ leave();
    __ ret(lr);
    return entry;
  }

  void generate_large_array_equals_loop_nonsimd(int loopThreshold,
        bool usePrefetch, Label &NOT_EQUAL) {
    Register a1 = r1, a2 = r2, result = r0, cnt1 = r10, tmp1 = rscratch1,
        tmp2 = rscratch2, tmp3 = r3, tmp4 = r4, tmp5 = r5, tmp6 = r11,
        tmp7 = r12, tmp8 = r13;
    Label LOOP;

    __ ldp(tmp1, tmp3, Address(__ post(a1, 2 * wordSize)));
    __ ldp(tmp2, tmp4, Address(__ post(a2, 2 * wordSize)));
    __ bind(LOOP);
    if (usePrefetch) {
      __ prfm(Address(a1, SoftwarePrefetchHintDistance));
      __ prfm(Address(a2, SoftwarePrefetchHintDistance));
    }
    __ ldp(tmp5, tmp7, Address(__ post(a1, 2 * wordSize)));
    __ eor(tmp1, tmp1, tmp2);
    __ eor(tmp3, tmp3, tmp4);
    __ ldp(tmp6, tmp8, Address(__ post(a2, 2 * wordSize)));
    __ orr(tmp1, tmp1, tmp3);
    __ cbnz(tmp1, NOT_EQUAL);
    __ ldp(tmp1, tmp3, Address(__ post(a1, 2 * wordSize)));
    __ eor(tmp5, tmp5, tmp6);
    __ eor(tmp7, tmp7, tmp8);
    __ ldp(tmp2, tmp4, Address(__ post(a2, 2 * wordSize)));
    __ orr(tmp5, tmp5, tmp7);
    __ cbnz(tmp5, NOT_EQUAL);
    __ ldp(tmp5, tmp7, Address(__ post(a1, 2 * wordSize)));
    __ eor(tmp1, tmp1, tmp2);
    __ eor(tmp3, tmp3, tmp4);
    __ ldp(tmp6, tmp8, Address(__ post(a2, 2 * wordSize)));
    __ orr(tmp1, tmp1, tmp3);
    __ cbnz(tmp1, NOT_EQUAL);
    __ ldp(tmp1, tmp3, Address(__ post(a1, 2 * wordSize)));
    __ eor(tmp5, tmp5, tmp6);
    __ sub(cnt1, cnt1, 8 * wordSize);
    __ eor(tmp7, tmp7, tmp8);
    __ ldp(tmp2, tmp4, Address(__ post(a2, 2 * wordSize)));
    // tmp6 is not used. MacroAssembler::subs is used here (rather than
    // cmp) because subs allows an unlimited range of immediate operand.
    __ subs(tmp6, cnt1, loopThreshold);
    __ orr(tmp5, tmp5, tmp7);
    __ cbnz(tmp5, NOT_EQUAL);
    __ br(__ GE, LOOP);
    // post-loop
    __ eor(tmp1, tmp1, tmp2);
    __ eor(tmp3, tmp3, tmp4);
    __ orr(tmp1, tmp1, tmp3);
    __ sub(cnt1, cnt1, 2 * wordSize);
    __ cbnz(tmp1, NOT_EQUAL);
  }

  void generate_large_array_equals_loop_simd(int loopThreshold,
        bool usePrefetch, Label &NOT_EQUAL) {
    Register a1 = r1, a2 = r2, result = r0, cnt1 = r10, tmp1 = rscratch1,
        tmp2 = rscratch2;
    Label LOOP;

    __ bind(LOOP);
    if (usePrefetch) {
      __ prfm(Address(a1, SoftwarePrefetchHintDistance));
      __ prfm(Address(a2, SoftwarePrefetchHintDistance));
    }
    __ ld1(v0, v1, v2, v3, __ T2D, Address(__ post(a1, 4 * 2 * wordSize)));
    __ sub(cnt1, cnt1, 8 * wordSize);
    __ ld1(v4, v5, v6, v7, __ T2D, Address(__ post(a2, 4 * 2 * wordSize)));
    __ subs(tmp1, cnt1, loopThreshold);
    __ eor(v0, __ T16B, v0, v4);
    __ eor(v1, __ T16B, v1, v5);
    __ eor(v2, __ T16B, v2, v6);
    __ eor(v3, __ T16B, v3, v7);
    __ orr(v0, __ T16B, v0, v1);
    __ orr(v1, __ T16B, v2, v3);
    __ orr(v0, __ T16B, v0, v1);
    __ umov(tmp1, v0, __ D, 0);
    __ umov(tmp2, v0, __ D, 1);
    __ orr(tmp1, tmp1, tmp2);
    __ cbnz(tmp1, NOT_EQUAL);
    __ br(__ GE, LOOP);
  }

  // a1 = r1 - array1 address
  // a2 = r2 - array2 address
  // result = r0 - return value. Already contains "false"
  // cnt1 = r10 - amount of elements left to check, reduced by wordSize
  // r3-r5 are reserved temporary registers
  address generate_large_array_equals() {
    Register a1 = r1, a2 = r2, result = r0, cnt1 = r10, tmp1 = rscratch1,
        tmp2 = rscratch2, tmp3 = r3, tmp4 = r4, tmp5 = r5, tmp6 = r11,
        tmp7 = r12, tmp8 = r13;
    Label TAIL, NOT_EQUAL, EQUAL, NOT_EQUAL_NO_POP, NO_PREFETCH_LARGE_LOOP,
        SMALL_LOOP, POST_LOOP;
    const int PRE_LOOP_SIZE = UseSIMDForArrayEquals ? 0 : 16;
    // calculate if at least 32 prefetched bytes are used
    int prefetchLoopThreshold = SoftwarePrefetchHintDistance + 32;
    int nonPrefetchLoopThreshold = (64 + PRE_LOOP_SIZE);
    RegSet spilled_regs = RegSet::range(tmp6, tmp8);
    assert_different_registers(a1, a2, result, cnt1, tmp1, tmp2, tmp3, tmp4,
        tmp5, tmp6, tmp7, tmp8);

    __ align(CodeEntryAlignment);

    StubCodeMark mark(this, "StubRoutines", "large_array_equals");

    address entry = __ pc();
    __ enter();
    __ sub(cnt1, cnt1, wordSize);  // first 8 bytes were loaded outside of stub
    // also advance pointers to use post-increment instead of pre-increment
    __ add(a1, a1, wordSize);
    __ add(a2, a2, wordSize);
    if (AvoidUnalignedAccesses) {
      // both implementations (SIMD/nonSIMD) are using relatively large load
      // instructions (ld1/ldp), which has huge penalty (up to x2 exec time)
      // on some CPUs in case of address is not at least 16-byte aligned.
      // Arrays are 8-byte aligned currently, so, we can make additional 8-byte
      // load if needed at least for 1st address and make if 16-byte aligned.
      Label ALIGNED16;
      __ tbz(a1, 3, ALIGNED16);
      __ ldr(tmp1, Address(__ post(a1, wordSize)));
      __ ldr(tmp2, Address(__ post(a2, wordSize)));
      __ sub(cnt1, cnt1, wordSize);
      __ eor(tmp1, tmp1, tmp2);
      __ cbnz(tmp1, NOT_EQUAL_NO_POP);
      __ bind(ALIGNED16);
    }
    if (UseSIMDForArrayEquals) {
      if (SoftwarePrefetchHintDistance >= 0) {
        __ subs(tmp1, cnt1, prefetchLoopThreshold);
        __ br(__ LE, NO_PREFETCH_LARGE_LOOP);
        generate_large_array_equals_loop_simd(prefetchLoopThreshold,
            /* prfm = */ true, NOT_EQUAL);
        __ subs(zr, cnt1, nonPrefetchLoopThreshold);
        __ br(__ LT, TAIL);
      }
      __ bind(NO_PREFETCH_LARGE_LOOP);
      generate_large_array_equals_loop_simd(nonPrefetchLoopThreshold,
          /* prfm = */ false, NOT_EQUAL);
    } else {
      __ push(spilled_regs, sp);
      if (SoftwarePrefetchHintDistance >= 0) {
        __ subs(tmp1, cnt1, prefetchLoopThreshold);
        __ br(__ LE, NO_PREFETCH_LARGE_LOOP);
        generate_large_array_equals_loop_nonsimd(prefetchLoopThreshold,
            /* prfm = */ true, NOT_EQUAL);
        __ subs(zr, cnt1, nonPrefetchLoopThreshold);
        __ br(__ LT, TAIL);
      }
      __ bind(NO_PREFETCH_LARGE_LOOP);
      generate_large_array_equals_loop_nonsimd(nonPrefetchLoopThreshold,
          /* prfm = */ false, NOT_EQUAL);
    }
    __ bind(TAIL);
      __ cbz(cnt1, EQUAL);
      __ subs(cnt1, cnt1, wordSize);
      __ br(__ LE, POST_LOOP);
    __ bind(SMALL_LOOP);
      __ ldr(tmp1, Address(__ post(a1, wordSize)));
      __ ldr(tmp2, Address(__ post(a2, wordSize)));
      __ subs(cnt1, cnt1, wordSize);
      __ eor(tmp1, tmp1, tmp2);
      __ cbnz(tmp1, NOT_EQUAL);
      __ br(__ GT, SMALL_LOOP);
    __ bind(POST_LOOP);
      __ ldr(tmp1, Address(a1, cnt1));
      __ ldr(tmp2, Address(a2, cnt1));
      __ eor(tmp1, tmp1, tmp2);
      __ cbnz(tmp1, NOT_EQUAL);
    __ bind(EQUAL);
      __ mov(result, true);
    __ bind(NOT_EQUAL);
      if (!UseSIMDForArrayEquals) {
        __ pop(spilled_regs, sp);
      }
    __ bind(NOT_EQUAL_NO_POP);
    __ leave();
    __ ret(lr);
    return entry;
  }

  address generate_dsin_dcos(bool isCos) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", isCos ? "libmDcos" : "libmDsin");
    address start = __ pc();
    __ generate_dsin_dcos(isCos, (address)StubRoutines::aarch64::_npio2_hw,
        (address)StubRoutines::aarch64::_two_over_pi,
        (address)StubRoutines::aarch64::_pio2,
        (address)StubRoutines::aarch64::_dsin_coef,
        (address)StubRoutines::aarch64::_dcos_coef);
    return start;
  }

  address generate_dlog() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "dlog");
    address entry = __ pc();
    FloatRegister vtmp0 = v0, vtmp1 = v1, vtmp2 = v2, vtmp3 = v3, vtmp4 = v4,
        vtmp5 = v5, tmpC1 = v16, tmpC2 = v17, tmpC3 = v18, tmpC4 = v19;
    Register tmp1 = r0, tmp2 = r1, tmp3 = r2, tmp4 = r3, tmp5 = r4;
    __ fast_log(vtmp0, vtmp1, vtmp2, vtmp3, vtmp4, vtmp5, tmpC1, tmpC2, tmpC3,
        tmpC4, tmp1, tmp2, tmp3, tmp4, tmp5);
    return entry;
  }

  // code for comparing 16 bytes of strings with same encoding
  void compare_string_16_bytes_same(Label &DIFF1, Label &DIFF2) {
    Register result = r0, str1 = r1, cnt1 = r2, str2 = r3, tmp1 = r10, tmp2 = r11;
    __ ldr(rscratch1, Address(__ post(str1, 8)));
    __ eor(rscratch2, tmp1, tmp2);
    __ ldr(cnt1, Address(__ post(str2, 8)));
    __ cbnz(rscratch2, DIFF1);
    __ ldr(tmp1, Address(__ post(str1, 8)));
    __ eor(rscratch2, rscratch1, cnt1);
    __ ldr(tmp2, Address(__ post(str2, 8)));
    __ cbnz(rscratch2, DIFF2);
  }

  // code for comparing 16 characters of strings with Latin1 and Utf16 encoding
  void compare_string_16_x_LU(Register tmpL, Register tmpU, Label &DIFF1,
      Label &DIFF2) {
    Register cnt1 = r2, tmp2 = r11, tmp3 = r12;
    FloatRegister vtmp = v1, vtmpZ = v0, vtmp3 = v2;

    __ ldrq(vtmp, Address(__ post(tmp2, 16)));
    __ ldr(tmpU, Address(__ post(cnt1, 8)));
    __ zip1(vtmp3, __ T16B, vtmp, vtmpZ);
    // now we have 32 bytes of characters (converted to U) in vtmp:vtmp3

    __ fmovd(tmpL, vtmp3);
    __ eor(rscratch2, tmp3, tmpL);
    __ cbnz(rscratch2, DIFF2);

    __ ldr(tmp3, Address(__ post(cnt1, 8)));
    __ umov(tmpL, vtmp3, __ D, 1);
    __ eor(rscratch2, tmpU, tmpL);
    __ cbnz(rscratch2, DIFF1);

    __ zip2(vtmp, __ T16B, vtmp, vtmpZ);
    __ ldr(tmpU, Address(__ post(cnt1, 8)));
    __ fmovd(tmpL, vtmp);
    __ eor(rscratch2, tmp3, tmpL);
    __ cbnz(rscratch2, DIFF2);

    __ ldr(tmp3, Address(__ post(cnt1, 8)));
    __ umov(tmpL, vtmp, __ D, 1);
    __ eor(rscratch2, tmpU, tmpL);
    __ cbnz(rscratch2, DIFF1);
  }

  // r0  = result
  // r1  = str1
  // r2  = cnt1
  // r3  = str2
  // r4  = cnt2
  // r10 = tmp1
  // r11 = tmp2
  address generate_compare_long_string_different_encoding(bool isLU) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", isLU
        ? "compare_long_string_different_encoding LU"
        : "compare_long_string_different_encoding UL");
    address entry = __ pc();
    Label SMALL_LOOP, TAIL, TAIL_LOAD_16, LOAD_LAST, DIFF1, DIFF2,
        DONE, CALCULATE_DIFFERENCE, LARGE_LOOP_PREFETCH, NO_PREFETCH,
        LARGE_LOOP_PREFETCH_REPEAT1, LARGE_LOOP_PREFETCH_REPEAT2;
    Register result = r0, str1 = r1, cnt1 = r2, str2 = r3, cnt2 = r4,
        tmp1 = r10, tmp2 = r11, tmp3 = r12, tmp4 = r14;
    FloatRegister vtmpZ = v0, vtmp = v1, vtmp3 = v2;
    RegSet spilled_regs = RegSet::of(tmp3, tmp4);

    int prefetchLoopExitCondition = MAX2(64, SoftwarePrefetchHintDistance/2);

    __ eor(vtmpZ, __ T16B, vtmpZ, vtmpZ);
    // cnt2 == amount of characters left to compare
    // Check already loaded first 4 symbols(vtmp and tmp2(LU)/tmp1(UL))
    __ zip1(vtmp, __ T8B, vtmp, vtmpZ);
    __ add(str1, str1, isLU ? wordSize/2 : wordSize);
    __ add(str2, str2, isLU ? wordSize : wordSize/2);
    __ fmovd(isLU ? tmp1 : tmp2, vtmp);
    __ subw(cnt2, cnt2, 8); // Already loaded 4 symbols. Last 4 is special case.
    __ eor(rscratch2, tmp1, tmp2);
    __ mov(rscratch1, tmp2);
    __ cbnz(rscratch2, CALCULATE_DIFFERENCE);
    Register tmpU = isLU ? rscratch1 : tmp1, // where to keep U for comparison
             tmpL = isLU ? tmp1 : rscratch1; // where to keep L for comparison
    __ push(spilled_regs, sp);
    __ mov(tmp2, isLU ? str1 : str2); // init the pointer to L next load
    __ mov(cnt1, isLU ? str2 : str1); // init the pointer to U next load

    __ ldr(tmp3, Address(__ post(cnt1, 8)));

    if (SoftwarePrefetchHintDistance >= 0) {
      __ subs(rscratch2, cnt2, prefetchLoopExitCondition);
      __ br(__ LT, NO_PREFETCH);
      __ bind(LARGE_LOOP_PREFETCH);
        __ prfm(Address(tmp2, SoftwarePrefetchHintDistance));
        __ mov(tmp4, 2);
        __ prfm(Address(cnt1, SoftwarePrefetchHintDistance));
        __ bind(LARGE_LOOP_PREFETCH_REPEAT1);
          compare_string_16_x_LU(tmpL, tmpU, DIFF1, DIFF2);
          __ subs(tmp4, tmp4, 1);
          __ br(__ GT, LARGE_LOOP_PREFETCH_REPEAT1);
          __ prfm(Address(cnt1, SoftwarePrefetchHintDistance));
          __ mov(tmp4, 2);
        __ bind(LARGE_LOOP_PREFETCH_REPEAT2);
          compare_string_16_x_LU(tmpL, tmpU, DIFF1, DIFF2);
          __ subs(tmp4, tmp4, 1);
          __ br(__ GT, LARGE_LOOP_PREFETCH_REPEAT2);
          __ sub(cnt2, cnt2, 64);
          __ subs(rscratch2, cnt2, prefetchLoopExitCondition);
          __ br(__ GE, LARGE_LOOP_PREFETCH);
    }
    __ cbz(cnt2, LOAD_LAST); // no characters left except last load
    __ bind(NO_PREFETCH);
    __ subs(cnt2, cnt2, 16);
    __ br(__ LT, TAIL);
    __ align(OptoLoopAlignment);
    __ bind(SMALL_LOOP); // smaller loop
      __ subs(cnt2, cnt2, 16);
      compare_string_16_x_LU(tmpL, tmpU, DIFF1, DIFF2);
      __ br(__ GE, SMALL_LOOP);
      __ cmn(cnt2, (u1)16);
      __ br(__ EQ, LOAD_LAST);
    __ bind(TAIL); // 1..15 characters left until last load (last 4 characters)
      __ add(cnt1, cnt1, cnt2, __ LSL, 1); // Address of 32 bytes before last 4 characters in UTF-16 string
      __ add(tmp2, tmp2, cnt2); // Address of 16 bytes before last 4 characters in Latin1 string
      __ ldr(tmp3, Address(cnt1, -8));
      compare_string_16_x_LU(tmpL, tmpU, DIFF1, DIFF2); // last 16 characters before last load
      __ b(LOAD_LAST);
    __ bind(DIFF2);
      __ mov(tmpU, tmp3);
    __ bind(DIFF1);
      __ pop(spilled_regs, sp);
      __ b(CALCULATE_DIFFERENCE);
    __ bind(LOAD_LAST);
      // Last 4 UTF-16 characters are already pre-loaded into tmp3 by compare_string_16_x_LU.
      // No need to load it again
      __ mov(tmpU, tmp3);
      __ pop(spilled_regs, sp);

      // tmp2 points to the address of the last 4 Latin1 characters right now
      __ ldrs(vtmp, Address(tmp2));
      __ zip1(vtmp, __ T8B, vtmp, vtmpZ);
      __ fmovd(tmpL, vtmp);

      __ eor(rscratch2, tmpU, tmpL);
      __ cbz(rscratch2, DONE);

    // Find the first different characters in the longwords and
    // compute their difference.
    __ bind(CALCULATE_DIFFERENCE);
      __ rev(rscratch2, rscratch2);
      __ clz(rscratch2, rscratch2);
      __ andr(rscratch2, rscratch2, -16);
      __ lsrv(tmp1, tmp1, rscratch2);
      __ uxthw(tmp1, tmp1);
      __ lsrv(rscratch1, rscratch1, rscratch2);
      __ uxthw(rscratch1, rscratch1);
      __ subw(result, tmp1, rscratch1);
    __ bind(DONE);
      __ ret(lr);
    return entry;
  }

    address generate_method_entry_barrier() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "nmethod_entry_barrier");

    Label deoptimize_label;

    address start = __ pc();

    __ set_last_Java_frame(sp, rfp, lr, rscratch1);

    __ enter();
    __ add(rscratch2, sp, wordSize);  // rscratch2 points to the saved lr

    __ sub(sp, sp, 4 * wordSize);  // four words for the returned {sp, fp, lr, pc}

    __ push_call_clobbered_registers();

    __ mov(c_rarg0, rscratch2);
    __ call_VM_leaf
         (CAST_FROM_FN_PTR
          (address, BarrierSetNMethod::nmethod_stub_entry_barrier), 1);

    __ reset_last_Java_frame(true);

    __ mov(rscratch1, r0);

    __ pop_call_clobbered_registers();

    __ cbnz(rscratch1, deoptimize_label);

    __ leave();
    __ ret(lr);

    __ BIND(deoptimize_label);

    __ ldp(/* new sp */ rscratch1, rfp, Address(sp, 0 * wordSize));
    __ ldp(lr, /* new pc*/ rscratch2, Address(sp, 2 * wordSize));

    __ mov(sp, rscratch1);
    __ br(rscratch2);

    return start;
  }

  // r0  = result
  // r1  = str1
  // r2  = cnt1
  // r3  = str2
  // r4  = cnt2
  // r10 = tmp1
  // r11 = tmp2
  address generate_compare_long_string_same_encoding(bool isLL) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", isLL
        ? "compare_long_string_same_encoding LL"
        : "compare_long_string_same_encoding UU");
    address entry = __ pc();
    Register result = r0, str1 = r1, cnt1 = r2, str2 = r3, cnt2 = r4,
        tmp1 = r10, tmp2 = r11;
    Label SMALL_LOOP, LARGE_LOOP_PREFETCH, CHECK_LAST, DIFF2, TAIL,
        LENGTH_DIFF, DIFF, LAST_CHECK_AND_LENGTH_DIFF,
        DIFF_LAST_POSITION, DIFF_LAST_POSITION2;
    // exit from large loop when less than 64 bytes left to read or we're about
    // to prefetch memory behind array border
    int largeLoopExitCondition = MAX2(64, SoftwarePrefetchHintDistance)/(isLL ? 1 : 2);
    // cnt1/cnt2 contains amount of characters to compare. cnt1 can be re-used
    // update cnt2 counter with already loaded 8 bytes
    __ sub(cnt2, cnt2, wordSize/(isLL ? 1 : 2));
    // update pointers, because of previous read
    __ add(str1, str1, wordSize);
    __ add(str2, str2, wordSize);
    if (SoftwarePrefetchHintDistance >= 0) {
      __ bind(LARGE_LOOP_PREFETCH);
        __ prfm(Address(str1, SoftwarePrefetchHintDistance));
        __ prfm(Address(str2, SoftwarePrefetchHintDistance));
        compare_string_16_bytes_same(DIFF, DIFF2);
        compare_string_16_bytes_same(DIFF, DIFF2);
        __ sub(cnt2, cnt2, isLL ? 64 : 32);
        compare_string_16_bytes_same(DIFF, DIFF2);
        __ subs(rscratch2, cnt2, largeLoopExitCondition);
        compare_string_16_bytes_same(DIFF, DIFF2);
        __ br(__ GT, LARGE_LOOP_PREFETCH);
        __ cbz(cnt2, LAST_CHECK_AND_LENGTH_DIFF); // no more chars left?
    }
    // less than 16 bytes left?
    __ subs(cnt2, cnt2, isLL ? 16 : 8);
    __ br(__ LT, TAIL);
    __ align(OptoLoopAlignment);
    __ bind(SMALL_LOOP);
      compare_string_16_bytes_same(DIFF, DIFF2);
      __ subs(cnt2, cnt2, isLL ? 16 : 8);
      __ br(__ GE, SMALL_LOOP);
    __ bind(TAIL);
      __ adds(cnt2, cnt2, isLL ? 16 : 8);
      __ br(__ EQ, LAST_CHECK_AND_LENGTH_DIFF);
      __ subs(cnt2, cnt2, isLL ? 8 : 4);
      __ br(__ LE, CHECK_LAST);
      __ eor(rscratch2, tmp1, tmp2);
      __ cbnz(rscratch2, DIFF);
      __ ldr(tmp1, Address(__ post(str1, 8)));
      __ ldr(tmp2, Address(__ post(str2, 8)));
      __ sub(cnt2, cnt2, isLL ? 8 : 4);
    __ bind(CHECK_LAST);
      if (!isLL) {
        __ add(cnt2, cnt2, cnt2); // now in bytes
      }
      __ eor(rscratch2, tmp1, tmp2);
      __ cbnz(rscratch2, DIFF);
      __ ldr(rscratch1, Address(str1, cnt2));
      __ ldr(cnt1, Address(str2, cnt2));
      __ eor(rscratch2, rscratch1, cnt1);
      __ cbz(rscratch2, LENGTH_DIFF);
      // Find the first different characters in the longwords and
      // compute their difference.
    __ bind(DIFF2);
      __ rev(rscratch2, rscratch2);
      __ clz(rscratch2, rscratch2);
      __ andr(rscratch2, rscratch2, isLL ? -8 : -16);
      __ lsrv(rscratch1, rscratch1, rscratch2);
      if (isLL) {
        __ lsrv(cnt1, cnt1, rscratch2);
        __ uxtbw(rscratch1, rscratch1);
        __ uxtbw(cnt1, cnt1);
      } else {
        __ lsrv(cnt1, cnt1, rscratch2);
        __ uxthw(rscratch1, rscratch1);
        __ uxthw(cnt1, cnt1);
      }
      __ subw(result, rscratch1, cnt1);
      __ b(LENGTH_DIFF);
    __ bind(DIFF);
      __ rev(rscratch2, rscratch2);
      __ clz(rscratch2, rscratch2);
      __ andr(rscratch2, rscratch2, isLL ? -8 : -16);
      __ lsrv(tmp1, tmp1, rscratch2);
      if (isLL) {
        __ lsrv(tmp2, tmp2, rscratch2);
        __ uxtbw(tmp1, tmp1);
        __ uxtbw(tmp2, tmp2);
      } else {
        __ lsrv(tmp2, tmp2, rscratch2);
        __ uxthw(tmp1, tmp1);
        __ uxthw(tmp2, tmp2);
      }
      __ subw(result, tmp1, tmp2);
      __ b(LENGTH_DIFF);
    __ bind(LAST_CHECK_AND_LENGTH_DIFF);
      __ eor(rscratch2, tmp1, tmp2);
      __ cbnz(rscratch2, DIFF);
    __ bind(LENGTH_DIFF);
      __ ret(lr);
    return entry;
  }

  void generate_compare_long_strings() {
      StubRoutines::aarch64::_compare_long_string_LL
          = generate_compare_long_string_same_encoding(true);
      StubRoutines::aarch64::_compare_long_string_UU
          = generate_compare_long_string_same_encoding(false);
      StubRoutines::aarch64::_compare_long_string_LU
          = generate_compare_long_string_different_encoding(true);
      StubRoutines::aarch64::_compare_long_string_UL
          = generate_compare_long_string_different_encoding(false);
  }

  // R0 = result
  // R1 = str2
  // R2 = cnt1
  // R3 = str1
  // R4 = cnt2
  // This generic linear code use few additional ideas, which makes it faster:
  // 1) we can safely keep at least 1st register of pattern(since length >= 8)
  // in order to skip initial loading(help in systems with 1 ld pipeline)
  // 2) we can use "fast" algorithm of finding single character to search for
  // first symbol with less branches(1 branch per each loaded register instead
  // of branch for each symbol), so, this is where constants like
  // 0x0101...01, 0x00010001...0001, 0x7f7f...7f, 0x7fff7fff...7fff comes from
  // 3) after loading and analyzing 1st register of source string, it can be
  // used to search for every 1st character entry, saving few loads in
  // comparison with "simplier-but-slower" implementation
  // 4) in order to avoid lots of push/pop operations, code below is heavily
  // re-using/re-initializing/compressing register values, which makes code
  // larger and a bit less readable, however, most of extra operations are
  // issued during loads or branches, so, penalty is minimal
  address generate_string_indexof_linear(bool str1_isL, bool str2_isL) {
    const char* stubName = str1_isL
        ? (str2_isL ? "indexof_linear_ll" : "indexof_linear_ul")
        : "indexof_linear_uu";
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", stubName);
    address entry = __ pc();

    int str1_chr_size = str1_isL ? 1 : 2;
    int str2_chr_size = str2_isL ? 1 : 2;
    int str1_chr_shift = str1_isL ? 0 : 1;
    int str2_chr_shift = str2_isL ? 0 : 1;
    bool isL = str1_isL && str2_isL;
   // parameters
    Register result = r0, str2 = r1, cnt1 = r2, str1 = r3, cnt2 = r4;
    // temporary registers
    Register tmp1 = r20, tmp2 = r21, tmp3 = r22, tmp4 = r23;
    RegSet spilled_regs = RegSet::range(tmp1, tmp4);
    // redefinitions
    Register ch1 = rscratch1, ch2 = rscratch2, first = tmp3;

    __ push(spilled_regs, sp);
    Label L_LOOP, L_LOOP_PROCEED, L_SMALL, L_HAS_ZERO,
        L_HAS_ZERO_LOOP, L_CMP_LOOP, L_CMP_LOOP_NOMATCH, L_SMALL_PROCEED,
        L_SMALL_HAS_ZERO_LOOP, L_SMALL_CMP_LOOP_NOMATCH, L_SMALL_CMP_LOOP,
        L_POST_LOOP, L_CMP_LOOP_LAST_CMP, L_HAS_ZERO_LOOP_NOMATCH,
        L_SMALL_CMP_LOOP_LAST_CMP, L_SMALL_CMP_LOOP_LAST_CMP2,
        L_CMP_LOOP_LAST_CMP2, DONE, NOMATCH;
    // Read whole register from str1. It is safe, because length >=8 here
    __ ldr(ch1, Address(str1));
    // Read whole register from str2. It is safe, because length >=8 here
    __ ldr(ch2, Address(str2));
    __ sub(cnt2, cnt2, cnt1);
    __ andr(first, ch1, str1_isL ? 0xFF : 0xFFFF);
    if (str1_isL != str2_isL) {
      __ eor(v0, __ T16B, v0, v0);
    }
    __ mov(tmp1, str2_isL ? 0x0101010101010101 : 0x0001000100010001);
    __ mul(first, first, tmp1);
    // check if we have less than 1 register to check
    __ subs(cnt2, cnt2, wordSize/str2_chr_size - 1);
    if (str1_isL != str2_isL) {
      __ fmovd(v1, ch1);
    }
    __ br(__ LE, L_SMALL);
    __ eor(ch2, first, ch2);
    if (str1_isL != str2_isL) {
      __ zip1(v1, __ T16B, v1, v0);
    }
    __ sub(tmp2, ch2, tmp1);
    __ orr(ch2, ch2, str2_isL ? 0x7f7f7f7f7f7f7f7f : 0x7fff7fff7fff7fff);
    __ bics(tmp2, tmp2, ch2);
    if (str1_isL != str2_isL) {
      __ fmovd(ch1, v1);
    }
    __ br(__ NE, L_HAS_ZERO);
    __ subs(cnt2, cnt2, wordSize/str2_chr_size);
    __ add(result, result, wordSize/str2_chr_size);
    __ add(str2, str2, wordSize);
    __ br(__ LT, L_POST_LOOP);
    __ BIND(L_LOOP);
      __ ldr(ch2, Address(str2));
      __ eor(ch2, first, ch2);
      __ sub(tmp2, ch2, tmp1);
      __ orr(ch2, ch2, str2_isL ? 0x7f7f7f7f7f7f7f7f : 0x7fff7fff7fff7fff);
      __ bics(tmp2, tmp2, ch2);
      __ br(__ NE, L_HAS_ZERO);
    __ BIND(L_LOOP_PROCEED);
      __ subs(cnt2, cnt2, wordSize/str2_chr_size);
      __ add(str2, str2, wordSize);
      __ add(result, result, wordSize/str2_chr_size);
      __ br(__ GE, L_LOOP);
    __ BIND(L_POST_LOOP);
      __ subs(zr, cnt2, -wordSize/str2_chr_size); // no extra characters to check
      __ br(__ LE, NOMATCH);
      __ ldr(ch2, Address(str2));
      __ sub(cnt2, zr, cnt2, __ LSL, LogBitsPerByte + str2_chr_shift);
      __ eor(ch2, first, ch2);
      __ sub(tmp2, ch2, tmp1);
      __ orr(ch2, ch2, str2_isL ? 0x7f7f7f7f7f7f7f7f : 0x7fff7fff7fff7fff);
      __ mov(tmp4, -1); // all bits set
      __ b(L_SMALL_PROCEED);
    __ align(OptoLoopAlignment);
    __ BIND(L_SMALL);
      __ sub(cnt2, zr, cnt2, __ LSL, LogBitsPerByte + str2_chr_shift);
      __ eor(ch2, first, ch2);
      if (str1_isL != str2_isL) {
        __ zip1(v1, __ T16B, v1, v0);
      }
      __ sub(tmp2, ch2, tmp1);
      __ mov(tmp4, -1); // all bits set
      __ orr(ch2, ch2, str2_isL ? 0x7f7f7f7f7f7f7f7f : 0x7fff7fff7fff7fff);
      if (str1_isL != str2_isL) {
        __ fmovd(ch1, v1); // move converted 4 symbols
      }
    __ BIND(L_SMALL_PROCEED);
      __ lsrv(tmp4, tmp4, cnt2); // mask. zeroes on useless bits.
      __ bic(tmp2, tmp2, ch2);
      __ ands(tmp2, tmp2, tmp4); // clear useless bits and check
      __ rbit(tmp2, tmp2);
      __ br(__ EQ, NOMATCH);
    __ BIND(L_SMALL_HAS_ZERO_LOOP);
      __ clz(tmp4, tmp2); // potentially long. Up to 4 cycles on some cpu's
      __ cmp(cnt1, u1(wordSize/str2_chr_size));
      __ br(__ LE, L_SMALL_CMP_LOOP_LAST_CMP2);
      if (str2_isL) { // LL
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte); // address of "index"
        __ ldr(ch2, Address(str2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4); // shift off leading zeroes from match info
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte);
        __ lsl(tmp2, tmp2, 1); // shift off leading "1" from match info
      } else {
        __ mov(ch2, 0xE); // all bits in byte set except last one
        __ andr(ch2, ch2, tmp4, __ LSR, LogBitsPerByte); // byte shift amount
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ lsl(tmp2, tmp2, 1); // shift off leading "1" from match info
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
      }
      __ cmp(ch1, ch2);
      __ mov(tmp4, wordSize/str2_chr_size);
      __ br(__ NE, L_SMALL_CMP_LOOP_NOMATCH);
    __ BIND(L_SMALL_CMP_LOOP);
      str1_isL ? __ ldrb(first, Address(str1, tmp4, Address::lsl(str1_chr_shift)))
               : __ ldrh(first, Address(str1, tmp4, Address::lsl(str1_chr_shift)));
      str2_isL ? __ ldrb(ch2, Address(str2, tmp4, Address::lsl(str2_chr_shift)))
               : __ ldrh(ch2, Address(str2, tmp4, Address::lsl(str2_chr_shift)));
      __ add(tmp4, tmp4, 1);
      __ cmp(tmp4, cnt1);
      __ br(__ GE, L_SMALL_CMP_LOOP_LAST_CMP);
      __ cmp(first, ch2);
      __ br(__ EQ, L_SMALL_CMP_LOOP);
    __ BIND(L_SMALL_CMP_LOOP_NOMATCH);
      __ cbz(tmp2, NOMATCH); // no more matches. exit
      __ clz(tmp4, tmp2);
      __ add(result, result, 1); // advance index
      __ add(str2, str2, str2_chr_size); // advance pointer
      __ b(L_SMALL_HAS_ZERO_LOOP);
    __ align(OptoLoopAlignment);
    __ BIND(L_SMALL_CMP_LOOP_LAST_CMP);
      __ cmp(first, ch2);
      __ br(__ NE, L_SMALL_CMP_LOOP_NOMATCH);
      __ b(DONE);
    __ align(OptoLoopAlignment);
    __ BIND(L_SMALL_CMP_LOOP_LAST_CMP2);
      if (str2_isL) { // LL
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte); // address of "index"
        __ ldr(ch2, Address(str2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4); // shift off leading zeroes from match info
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte);
        __ lsl(tmp2, tmp2, 1); // shift off leading "1" from match info
      } else {
        __ mov(ch2, 0xE); // all bits in byte set except last one
        __ andr(ch2, ch2, tmp4, __ LSR, LogBitsPerByte); // byte shift amount
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ lsl(tmp2, tmp2, 1); // shift off leading "1" from match info
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
      }
      __ cmp(ch1, ch2);
      __ br(__ NE, L_SMALL_CMP_LOOP_NOMATCH);
      __ b(DONE);
    __ align(OptoLoopAlignment);
    __ BIND(L_HAS_ZERO);
      __ rbit(tmp2, tmp2);
      __ clz(tmp4, tmp2); // potentially long. Up to 4 cycles on some CPU's
      // Now, perform compression of counters(cnt2 and cnt1) into one register.
      // It's fine because both counters are 32bit and are not changed in this
      // loop. Just restore it on exit. So, cnt1 can be re-used in this loop.
      __ orr(cnt2, cnt2, cnt1, __ LSL, BitsPerByte * wordSize / 2);
      __ sub(result, result, 1);
    __ BIND(L_HAS_ZERO_LOOP);
      __ mov(cnt1, wordSize/str2_chr_size);
      __ cmp(cnt1, cnt2, __ LSR, BitsPerByte * wordSize / 2);
      __ br(__ GE, L_CMP_LOOP_LAST_CMP2); // case of 8 bytes only to compare
      if (str2_isL) {
        __ lsr(ch2, tmp4, LogBitsPerByte + str2_chr_shift); // char index
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(tmp4, tmp4, 1);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ lsl(tmp2, tmp2, 1);
        __ mov(tmp4, wordSize/str2_chr_size);
      } else {
        __ mov(ch2, 0xE);
        __ andr(ch2, ch2, tmp4, __ LSR, LogBitsPerByte); // byte shift amount
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(tmp4, tmp4, 1);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte);
        __ lsl(tmp2, tmp2, 1);
        __ mov(tmp4, wordSize/str2_chr_size);
        __ sub(str2, str2, str2_chr_size);
      }
      __ cmp(ch1, ch2);
      __ mov(tmp4, wordSize/str2_chr_size);
      __ br(__ NE, L_CMP_LOOP_NOMATCH);
    __ BIND(L_CMP_LOOP);
      str1_isL ? __ ldrb(cnt1, Address(str1, tmp4, Address::lsl(str1_chr_shift)))
               : __ ldrh(cnt1, Address(str1, tmp4, Address::lsl(str1_chr_shift)));
      str2_isL ? __ ldrb(ch2, Address(str2, tmp4, Address::lsl(str2_chr_shift)))
               : __ ldrh(ch2, Address(str2, tmp4, Address::lsl(str2_chr_shift)));
      __ add(tmp4, tmp4, 1);
      __ cmp(tmp4, cnt2, __ LSR, BitsPerByte * wordSize / 2);
      __ br(__ GE, L_CMP_LOOP_LAST_CMP);
      __ cmp(cnt1, ch2);
      __ br(__ EQ, L_CMP_LOOP);
    __ BIND(L_CMP_LOOP_NOMATCH);
      // here we're not matched
      __ cbz(tmp2, L_HAS_ZERO_LOOP_NOMATCH); // no more matches. Proceed to main loop
      __ clz(tmp4, tmp2);
      __ add(str2, str2, str2_chr_size); // advance pointer
      __ b(L_HAS_ZERO_LOOP);
    __ align(OptoLoopAlignment);
    __ BIND(L_CMP_LOOP_LAST_CMP);
      __ cmp(cnt1, ch2);
      __ br(__ NE, L_CMP_LOOP_NOMATCH);
      __ b(DONE);
    __ align(OptoLoopAlignment);
    __ BIND(L_CMP_LOOP_LAST_CMP2);
      if (str2_isL) {
        __ lsr(ch2, tmp4, LogBitsPerByte + str2_chr_shift); // char index
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(tmp4, tmp4, 1);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ lsl(tmp2, tmp2, 1);
      } else {
        __ mov(ch2, 0xE);
        __ andr(ch2, ch2, tmp4, __ LSR, LogBitsPerByte); // byte shift amount
        __ ldr(ch2, Address(str2, ch2)); // read whole register of str2. Safe.
        __ lslv(tmp2, tmp2, tmp4);
        __ add(tmp4, tmp4, 1);
        __ add(result, result, tmp4, __ LSR, LogBitsPerByte + str2_chr_shift);
        __ add(str2, str2, tmp4, __ LSR, LogBitsPerByte);
        __ lsl(tmp2, tmp2, 1);
        __ sub(str2, str2, str2_chr_size);
      }
      __ cmp(ch1, ch2);
      __ br(__ NE, L_CMP_LOOP_NOMATCH);
      __ b(DONE);
    __ align(OptoLoopAlignment);
    __ BIND(L_HAS_ZERO_LOOP_NOMATCH);
      // 1) Restore "result" index. Index was wordSize/str2_chr_size * N until
      // L_HAS_ZERO block. Byte octet was analyzed in L_HAS_ZERO_LOOP,
      // so, result was increased at max by wordSize/str2_chr_size - 1, so,
      // respective high bit wasn't changed. L_LOOP_PROCEED will increase
      // result by analyzed characters value, so, we can just reset lower bits
      // in result here. Clear 2 lower bits for UU/UL and 3 bits for LL
      // 2) restore cnt1 and cnt2 values from "compressed" cnt2
      // 3) advance str2 value to represent next str2 octet. result & 7/3 is
      // index of last analyzed substring inside current octet. So, str2 in at
      // respective start address. We need to advance it to next octet
      __ andr(tmp2, result, wordSize/str2_chr_size - 1); // symbols analyzed
      __ lsr(cnt1, cnt2, BitsPerByte * wordSize / 2);
      __ bfm(result, zr, 0, 2 - str2_chr_shift);
      __ sub(str2, str2, tmp2, __ LSL, str2_chr_shift); // restore str2
      __ movw(cnt2, cnt2);
      __ b(L_LOOP_PROCEED);
    __ align(OptoLoopAlignment);
    __ BIND(NOMATCH);
      __ mov(result, -1);
    __ BIND(DONE);
      __ pop(spilled_regs, sp);
      __ ret(lr);
    return entry;
  }

  void generate_string_indexof_stubs() {
    StubRoutines::aarch64::_string_indexof_linear_ll = generate_string_indexof_linear(true, true);
    StubRoutines::aarch64::_string_indexof_linear_uu = generate_string_indexof_linear(false, false);
    StubRoutines::aarch64::_string_indexof_linear_ul = generate_string_indexof_linear(true, false);
  }

  void inflate_and_store_2_fp_registers(bool generatePrfm,
      FloatRegister src1, FloatRegister src2) {
    Register dst = r1;
    __ zip1(v1, __ T16B, src1, v0);
    __ zip2(v2, __ T16B, src1, v0);
    if (generatePrfm) {
      __ prfm(Address(dst, SoftwarePrefetchHintDistance), PSTL1STRM);
    }
    __ zip1(v3, __ T16B, src2, v0);
    __ zip2(v4, __ T16B, src2, v0);
    __ st1(v1, v2, v3, v4, __ T16B, Address(__ post(dst, 64)));
  }

  // R0 = src
  // R1 = dst
  // R2 = len
  // R3 = len >> 3
  // V0 = 0
  // v1 = loaded 8 bytes
  address generate_large_byte_array_inflate() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "large_byte_array_inflate");
    address entry = __ pc();
    Label LOOP, LOOP_START, LOOP_PRFM, LOOP_PRFM_START, DONE;
    Register src = r0, dst = r1, len = r2, octetCounter = r3;
    const int large_loop_threshold = MAX2(64, SoftwarePrefetchHintDistance)/8 + 4;

    // do one more 8-byte read to have address 16-byte aligned in most cases
    // also use single store instruction
    __ ldrd(v2, __ post(src, 8));
    __ sub(octetCounter, octetCounter, 2);
    __ zip1(v1, __ T16B, v1, v0);
    __ zip1(v2, __ T16B, v2, v0);
    __ st1(v1, v2, __ T16B, __ post(dst, 32));
    __ ld1(v3, v4, v5, v6, __ T16B, Address(__ post(src, 64)));
    __ subs(rscratch1, octetCounter, large_loop_threshold);
    __ br(__ LE, LOOP_START);
    __ b(LOOP_PRFM_START);
    __ bind(LOOP_PRFM);
      __ ld1(v3, v4, v5, v6, __ T16B, Address(__ post(src, 64)));
    __ bind(LOOP_PRFM_START);
      __ prfm(Address(src, SoftwarePrefetchHintDistance));
      __ sub(octetCounter, octetCounter, 8);
      __ subs(rscratch1, octetCounter, large_loop_threshold);
      inflate_and_store_2_fp_registers(true, v3, v4);
      inflate_and_store_2_fp_registers(true, v5, v6);
      __ br(__ GT, LOOP_PRFM);
      __ cmp(octetCounter, (u1)8);
      __ br(__ LT, DONE);
    __ bind(LOOP);
      __ ld1(v3, v4, v5, v6, __ T16B, Address(__ post(src, 64)));
      __ bind(LOOP_START);
      __ sub(octetCounter, octetCounter, 8);
      __ cmp(octetCounter, (u1)8);
      inflate_and_store_2_fp_registers(false, v3, v4);
      inflate_and_store_2_fp_registers(false, v5, v6);
      __ br(__ GE, LOOP);
    __ bind(DONE);
      __ ret(lr);
    return entry;
  }

  /**
   *  Arguments:
   *
   *  Input:
   *  c_rarg0   - current state address
   *  c_rarg1   - H key address
   *  c_rarg2   - data address
   *  c_rarg3   - number of blocks
   *
   *  Output:
   *  Updated state at c_rarg0
   */
  address generate_ghash_processBlocks() {
    // Bafflingly, GCM uses little-endian for the byte order, but
    // big-endian for the bit order.  For example, the polynomial 1 is
    // represented as the 16-byte string 80 00 00 00 | 12 bytes of 00.
    //
    // So, we must either reverse the bytes in each word and do
    // everything big-endian or reverse the bits in each byte and do
    // it little-endian.  On AArch64 it's more idiomatic to reverse
    // the bits in each byte (we have an instruction, RBIT, to do
    // that) and keep the data in little-endian bit order throught the
    // calculation, bit-reversing the inputs and outputs.

    StubCodeMark mark(this, "StubRoutines", "ghash_processBlocks");
    __ align(wordSize * 2);
    address p = __ pc();
    __ emit_int64(0x87);  // The low-order bits of the field
                          // polynomial (i.e. p = z^7+z^2+z+1)
                          // repeated in the low and high parts of a
                          // 128-bit vector
    __ emit_int64(0x87);

    __ align(CodeEntryAlignment);
    address start = __ pc();

    Register state   = c_rarg0;
    Register subkeyH = c_rarg1;
    Register data    = c_rarg2;
    Register blocks  = c_rarg3;

    FloatRegister vzr = v30;
    __ eor(vzr, __ T16B, vzr, vzr); // zero register

    __ ldrq(v0, Address(state));
    __ ldrq(v1, Address(subkeyH));

    __ rev64(v0, __ T16B, v0);          // Bit-reverse words in state and subkeyH
    __ rbit(v0, __ T16B, v0);
    __ rev64(v1, __ T16B, v1);
    __ rbit(v1, __ T16B, v1);

    __ ldrq(v26, p);

    __ ext(v16, __ T16B, v1, v1, 0x08); // long-swap subkeyH into v1
    __ eor(v16, __ T16B, v16, v1);      // xor subkeyH into subkeyL (Karatsuba: (A1+A0))

    {
      Label L_ghash_loop;
      __ bind(L_ghash_loop);

      __ ldrq(v2, Address(__ post(data, 0x10))); // Load the data, bit
                                                 // reversing each byte
      __ rbit(v2, __ T16B, v2);
      __ eor(v2, __ T16B, v0, v2);   // bit-swapped data ^ bit-swapped state

      // Multiply state in v2 by subkey in v1
      ghash_multiply(/*result_lo*/v5, /*result_hi*/v7,
                     /*a*/v1, /*b*/v2, /*a1_xor_a0*/v16,
                     /*temps*/v6, v20, v18, v21);
      // Reduce v7:v5 by the field polynomial
      ghash_reduce(v0, v5, v7, v26, vzr, v20);

      __ sub(blocks, blocks, 1);
      __ cbnz(blocks, L_ghash_loop);
    }

    // The bit-reversed result is at this point in v0
    __ rev64(v1, __ T16B, v0);
    __ rbit(v1, __ T16B, v1);

    __ st1(v1, __ T16B, state);
    __ ret(lr);

    return start;
  }

  void generate_base64_encode_simdround(Register src, Register dst,
        FloatRegister codec, u8 size) {

    FloatRegister in0  = v4,  in1  = v5,  in2  = v6;
    FloatRegister out0 = v16, out1 = v17, out2 = v18, out3 = v19;
    FloatRegister ind0 = v20, ind1 = v21, ind2 = v22, ind3 = v23;

    Assembler::SIMD_Arrangement arrangement = size == 16 ? __ T16B : __ T8B;

    __ ld3(in0, in1, in2, arrangement, __ post(src, 3 * size));

    __ ushr(ind0, arrangement, in0,  2);

    __ ushr(ind1, arrangement, in1,  2);
    __ shl(in0,   arrangement, in0,  6);
    __ orr(ind1,  arrangement, ind1, in0);
    __ ushr(ind1, arrangement, ind1, 2);

    __ ushr(ind2, arrangement, in2,  4);
    __ shl(in1,   arrangement, in1,  4);
    __ orr(ind2,  arrangement, in1,  ind2);
    __ ushr(ind2, arrangement, ind2, 2);

    __ shl(ind3,  arrangement, in2,  2);
    __ ushr(ind3, arrangement, ind3, 2);

    __ tbl(out0,  arrangement, codec,  4, ind0);
    __ tbl(out1,  arrangement, codec,  4, ind1);
    __ tbl(out2,  arrangement, codec,  4, ind2);
    __ tbl(out3,  arrangement, codec,  4, ind3);

    __ st4(out0,  out1, out2, out3, arrangement, __ post(dst, 4 * size));
  }

   /**
   *  Arguments:
   *
   *  Input:
   *  c_rarg0   - src_start
   *  c_rarg1   - src_offset
   *  c_rarg2   - src_length
   *  c_rarg3   - dest_start
   *  c_rarg4   - dest_offset
   *  c_rarg5   - isURL
   *
   */
  address generate_base64_encodeBlock() {

    static const char toBase64[64] = {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    static const char toBase64URL[64] = {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
      'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "encodeBlock");
    address start = __ pc();

    Register src   = c_rarg0;  // source array
    Register soff  = c_rarg1;  // source start offset
    Register send  = c_rarg2;  // source end offset
    Register dst   = c_rarg3;  // dest array
    Register doff  = c_rarg4;  // position for writing to dest array
    Register isURL = c_rarg5;  // Base64 or URL chracter set

    // c_rarg6 and c_rarg7 are free to use as temps
    Register codec  = c_rarg6;
    Register length = c_rarg7;

    Label ProcessData, Process48B, Process24B, Process3B, SIMDExit, Exit;

    __ add(src, src, soff);
    __ add(dst, dst, doff);
    __ sub(length, send, soff);

    // load the codec base address
    __ lea(codec, ExternalAddress((address) toBase64));
    __ cbz(isURL, ProcessData);
    __ lea(codec, ExternalAddress((address) toBase64URL));

    __ BIND(ProcessData);

    // too short to formup a SIMD loop, roll back
    __ cmp(length, (u1)24);
    __ br(Assembler::LT, Process3B);

    __ ld1(v0, v1, v2, v3, __ T16B, Address(codec));

    __ BIND(Process48B);
    __ cmp(length, (u1)48);
    __ br(Assembler::LT, Process24B);
    generate_base64_encode_simdround(src, dst, v0, 16);
    __ sub(length, length, 48);
    __ b(Process48B);

    __ BIND(Process24B);
    __ cmp(length, (u1)24);
    __ br(Assembler::LT, SIMDExit);
    generate_base64_encode_simdround(src, dst, v0, 8);
    __ sub(length, length, 24);

    __ BIND(SIMDExit);
    __ cbz(length, Exit);

    __ BIND(Process3B);
    //  3 src bytes, 24 bits
    __ ldrb(r10, __ post(src, 1));
    __ ldrb(r11, __ post(src, 1));
    __ ldrb(r12, __ post(src, 1));
    __ orrw(r11, r11, r10, Assembler::LSL, 8);
    __ orrw(r12, r12, r11, Assembler::LSL, 8);
    // codec index
    __ ubfmw(r15, r12, 18, 23);
    __ ubfmw(r14, r12, 12, 17);
    __ ubfmw(r13, r12, 6,  11);
    __ andw(r12,  r12, 63);
    // get the code based on the codec
    __ ldrb(r15, Address(codec, r15, Address::uxtw(0)));
    __ ldrb(r14, Address(codec, r14, Address::uxtw(0)));
    __ ldrb(r13, Address(codec, r13, Address::uxtw(0)));
    __ ldrb(r12, Address(codec, r12, Address::uxtw(0)));
    __ strb(r15, __ post(dst, 1));
    __ strb(r14, __ post(dst, 1));
    __ strb(r13, __ post(dst, 1));
    __ strb(r12, __ post(dst, 1));
    __ sub(length, length, 3);
    __ cbnz(length, Process3B);

    __ BIND(Exit);
    __ ret(lr);

    return start;
  }

  void generate_base64_decode_simdround(Register src, Register dst,
        FloatRegister codecL, FloatRegister codecH, int size, Label& Exit) {

    FloatRegister in0  = v16, in1  = v17,  in2 = v18,  in3 = v19;
    FloatRegister out0 = v20, out1 = v21, out2 = v22;

    FloatRegister decL0 = v23, decL1 = v24, decL2 = v25, decL3 = v26;
    FloatRegister decH0 = v28, decH1 = v29, decH2 = v30, decH3 = v31;

    Label NoIllegalData, ErrorInLowerHalf, StoreLegalData;

    Assembler::SIMD_Arrangement arrangement = size == 16 ? __ T16B : __ T8B;

    __ ld4(in0, in1, in2, in3, arrangement, __ post(src, 4 * size));

    // we need unsigned saturating substract, to make sure all input values
    // in range [0, 63] will have 0U value in the higher half lookup
    __ uqsubv(decH0, __ T16B, in0, v27);
    __ uqsubv(decH1, __ T16B, in1, v27);
    __ uqsubv(decH2, __ T16B, in2, v27);
    __ uqsubv(decH3, __ T16B, in3, v27);

    // lower half lookup
    __ tbl(decL0, arrangement, codecL, 4, in0);
    __ tbl(decL1, arrangement, codecL, 4, in1);
    __ tbl(decL2, arrangement, codecL, 4, in2);
    __ tbl(decL3, arrangement, codecL, 4, in3);

    // higher half lookup
    __ tbx(decH0, arrangement, codecH, 4, decH0);
    __ tbx(decH1, arrangement, codecH, 4, decH1);
    __ tbx(decH2, arrangement, codecH, 4, decH2);
    __ tbx(decH3, arrangement, codecH, 4, decH3);

    // combine lower and higher
    __ orr(decL0, arrangement, decL0, decH0);
    __ orr(decL1, arrangement, decL1, decH1);
    __ orr(decL2, arrangement, decL2, decH2);
    __ orr(decL3, arrangement, decL3, decH3);

    // check illegal inputs, value larger than 63 (maximum of 6 bits)
    __ cmhi(decH0, arrangement, decL0, v27);
    __ cmhi(decH1, arrangement, decL1, v27);
    __ cmhi(decH2, arrangement, decL2, v27);
    __ cmhi(decH3, arrangement, decL3, v27);
    __ orr(in0, arrangement, decH0, decH1);
    __ orr(in1, arrangement, decH2, decH3);
    __ orr(in2, arrangement, in0,   in1);
    __ umaxv(in3, arrangement, in2);
    __ umov(rscratch2, in3, __ B, 0);

    // get the data to output
    __ shl(out0,  arrangement, decL0, 2);
    __ ushr(out1, arrangement, decL1, 4);
    __ orr(out0,  arrangement, out0,  out1);
    __ shl(out1,  arrangement, decL1, 4);
    __ ushr(out2, arrangement, decL2, 2);
    __ orr(out1,  arrangement, out1,  out2);
    __ shl(out2,  arrangement, decL2, 6);
    __ orr(out2,  arrangement, out2,  decL3);

    __ cbz(rscratch2, NoIllegalData);

    // handle illegal input
    __ umov(r10, in2, __ D, 0);
    if (size == 16) {
      __ cbnz(r10, ErrorInLowerHalf);

      // illegal input is in higher half, store the lower half now.
      __ st3(out0, out1, out2, __ T8B, __ post(dst, 24));

      __ umov(r10, in2,  __ D, 1);
      __ umov(r11, out0, __ D, 1);
      __ umov(r12, out1, __ D, 1);
      __ umov(r13, out2, __ D, 1);
      __ b(StoreLegalData);

      __ BIND(ErrorInLowerHalf);
    }
    __ umov(r11, out0, __ D, 0);
    __ umov(r12, out1, __ D, 0);
    __ umov(r13, out2, __ D, 0);

    __ BIND(StoreLegalData);
    __ tbnz(r10, 5, Exit); // 0xff indicates illegal input
    __ strb(r11, __ post(dst, 1));
    __ strb(r12, __ post(dst, 1));
    __ strb(r13, __ post(dst, 1));
    __ lsr(r10, r10, 8);
    __ lsr(r11, r11, 8);
    __ lsr(r12, r12, 8);
    __ lsr(r13, r13, 8);
    __ b(StoreLegalData);

    __ BIND(NoIllegalData);
    __ st3(out0, out1, out2, arrangement, __ post(dst, 3 * size));
  }


   /**
   *  Arguments:
   *
   *  Input:
   *  c_rarg0   - src_start
   *  c_rarg1   - src_offset
   *  c_rarg2   - src_length
   *  c_rarg3   - dest_start
   *  c_rarg4   - dest_offset
   *  c_rarg5   - isURL
   *  c_rarg6   - isMIME
   *
   */
  address generate_base64_decodeBlock() {

    // The SIMD part of this Base64 decode intrinsic is based on the algorithm outlined
    // on http://0x80.pl/articles/base64-simd-neon.html#encoding-quadwords, in section
    // titled "Base64 decoding".

    // Non-SIMD lookup tables are mostly dumped from fromBase64 array used in java.util.Base64,
    // except the trailing character '=' is also treated illegal value in this instrinsic. That
    // is java.util.Base64.fromBase64['='] = -2, while fromBase(URL)64ForNoSIMD['='] = 255 here.
    static const uint8_t fromBase64ForNoSIMD[256] = {
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,  62u, 255u, 255u, 255u,  63u,
       52u,  53u,  54u,  55u,  56u,  57u,  58u,  59u,  60u,  61u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u,   0u,   1u,   2u,   3u,   4u,   5u,   6u,   7u,   8u,   9u,  10u,  11u,  12u,  13u,  14u,
       15u,  16u,  17u,  18u,  19u,  20u,  21u,  22u,  23u,  24u,  25u, 255u, 255u, 255u, 255u, 255u,
      255u,  26u,  27u,  28u,  29u,  30u,  31u,  32u,  33u,  34u,  35u,  36u,  37u,  38u,  39u,  40u,
       41u,  42u,  43u,  44u,  45u,  46u,  47u,  48u,  49u,  50u,  51u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
    };

    static const uint8_t fromBase64URLForNoSIMD[256] = {
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,  62u, 255u, 255u,
       52u,  53u,  54u,  55u,  56u,  57u,  58u,  59u,  60u,  61u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u,   0u,   1u,   2u,   3u,   4u,   5u,   6u,   7u,   8u,   9u,  10u,  11u,  12u,  13u,  14u,
       15u,  16u,  17u,  18u,  19u,  20u,  21u,  22u,  23u,  24u,  25u, 255u, 255u, 255u, 255u,  63u,
      255u,  26u,  27u,  28u,  29u,  30u,  31u,  32u,  33u,  34u,  35u,  36u,  37u,  38u,  39u,  40u,
       41u,  42u,  43u,  44u,  45u,  46u,  47u,  48u,  49u,  50u,  51u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
    };

    // A legal value of base64 code is in range [0, 127].  We need two lookups
    // with tbl/tbx and combine them to get the decode data. The 1st table vector
    // lookup use tbl, out of range indices are set to 0 in destination. The 2nd
    // table vector lookup use tbx, out of range indices are unchanged in
    // destination. Input [64..126] is mapped to index [65, 127] in second lookup.
    // The value of index 64 is set to 0, so that we know that we already get the
    // decoded data with the 1st lookup.
    static const uint8_t fromBase64ForSIMD[128] = {
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,  62u, 255u, 255u, 255u,  63u,
       52u,  53u,  54u,  55u,  56u,  57u,  58u,  59u,  60u,  61u, 255u, 255u, 255u, 255u, 255u, 255u,
        0u, 255u,   0u,   1u,   2u,   3u,   4u,   5u,   6u,   7u,   8u,   9u,  10u,  11u,  12u,  13u,
       14u,  15u,  16u,  17u,  18u,  19u,  20u,  21u,  22u,  23u,  24u,  25u, 255u, 255u, 255u, 255u,
      255u, 255u,  26u,  27u,  28u,  29u,  30u,  31u,  32u,  33u,  34u,  35u,  36u,  37u,  38u,  39u,
       40u,  41u,  42u,  43u,  44u,  45u,  46u,  47u,  48u,  49u,  50u,  51u, 255u, 255u, 255u, 255u,
    };

    static const uint8_t fromBase64URLForSIMD[128] = {
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,
      255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u,  62u, 255u, 255u,
       52u,  53u,  54u,  55u,  56u,  57u,  58u,  59u,  60u,  61u, 255u, 255u, 255u, 255u, 255u, 255u,
        0u, 255u,   0u,   1u,   2u,   3u,   4u,   5u,   6u,   7u,   8u,   9u,  10u,  11u,  12u,  13u,
       14u,  15u,  16u,  17u,  18u,  19u,  20u,  21u,  22u,  23u,  24u,  25u, 255u, 255u, 255u, 255u,
       63u, 255u,  26u,  27u,  28u,  29u,  30u,  31u,  32u,  33u,  34u,  35u,  36u,  37u,  38u,  39u,
       40u,  41u,  42u,  43u,  44u,  45u,  46u,  47u,  48u,  49u,  50u,  51u, 255u, 255u, 255u, 255u,
    };

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "decodeBlock");
    address start = __ pc();

    Register src    = c_rarg0;  // source array
    Register soff   = c_rarg1;  // source start offset
    Register send   = c_rarg2;  // source end offset
    Register dst    = c_rarg3;  // dest array
    Register doff   = c_rarg4;  // position for writing to dest array
    Register isURL  = c_rarg5;  // Base64 or URL character set
    Register isMIME = c_rarg6;  // Decoding MIME block - unused in this implementation

    Register length = send;    // reuse send as length of source data to process

    Register simd_codec   = c_rarg6;
    Register nosimd_codec = c_rarg7;

    Label ProcessData, Process64B, Process32B, Process4B, SIMDEnter, SIMDExit, Exit;

    __ enter();

    __ add(src, src, soff);
    __ add(dst, dst, doff);

    __ mov(doff, dst);

    __ sub(length, send, soff);
    __ bfm(length, zr, 0, 1);

    __ lea(nosimd_codec, ExternalAddress((address) fromBase64ForNoSIMD));
    __ cbz(isURL, ProcessData);
    __ lea(nosimd_codec, ExternalAddress((address) fromBase64URLForNoSIMD));

    __ BIND(ProcessData);
    __ mov(rscratch1, length);
    __ cmp(length, (u1)144); // 144 = 80 + 64
    __ br(Assembler::LT, Process4B);

    // In the MIME case, the line length cannot be more than 76
    // bytes (see RFC 2045). This is too short a block for SIMD
    // to be worthwhile, so we use non-SIMD here.
    __ movw(rscratch1, 79);

    __ BIND(Process4B);
    __ ldrw(r14, __ post(src, 4));
    __ ubfxw(r10, r14, 0,  8);
    __ ubfxw(r11, r14, 8,  8);
    __ ubfxw(r12, r14, 16, 8);
    __ ubfxw(r13, r14, 24, 8);
    // get the de-code
    __ ldrb(r10, Address(nosimd_codec, r10, Address::uxtw(0)));
    __ ldrb(r11, Address(nosimd_codec, r11, Address::uxtw(0)));
    __ ldrb(r12, Address(nosimd_codec, r12, Address::uxtw(0)));
    __ ldrb(r13, Address(nosimd_codec, r13, Address::uxtw(0)));
    // error detection, 255u indicates an illegal input
    __ orrw(r14, r10, r11);
    __ orrw(r15, r12, r13);
    __ orrw(r14, r14, r15);
    __ tbnz(r14, 7, Exit);
    // recover the data
    __ lslw(r14, r10, 10);
    __ bfiw(r14, r11, 4, 6);
    __ bfmw(r14, r12, 2, 5);
    __ rev16w(r14, r14);
    __ bfiw(r13, r12, 6, 2);
    __ strh(r14, __ post(dst, 2));
    __ strb(r13, __ post(dst, 1));
    // non-simd loop
    __ subsw(rscratch1, rscratch1, 4);
    __ br(Assembler::GT, Process4B);

    // if exiting from PreProcess80B, rscratch1 == -1;
    // otherwise, rscratch1 == 0.
    __ cbzw(rscratch1, Exit);
    __ sub(length, length, 80);

    __ lea(simd_codec, ExternalAddress((address) fromBase64ForSIMD));
    __ cbz(isURL, SIMDEnter);
    __ lea(simd_codec, ExternalAddress((address) fromBase64URLForSIMD));

    __ BIND(SIMDEnter);
    __ ld1(v0, v1, v2, v3, __ T16B, __ post(simd_codec, 64));
    __ ld1(v4, v5, v6, v7, __ T16B, Address(simd_codec));
    __ mov(rscratch1, 63);
    __ dup(v27, __ T16B, rscratch1);

    __ BIND(Process64B);
    __ cmp(length, (u1)64);
    __ br(Assembler::LT, Process32B);
    generate_base64_decode_simdround(src, dst, v0, v4, 16, Exit);
    __ sub(length, length, 64);
    __ b(Process64B);

    __ BIND(Process32B);
    __ cmp(length, (u1)32);
    __ br(Assembler::LT, SIMDExit);
    generate_base64_decode_simdround(src, dst, v0, v4, 8, Exit);
    __ sub(length, length, 32);
    __ b(Process32B);

    __ BIND(SIMDExit);
    __ cbz(length, Exit);
    __ movw(rscratch1, length);
    __ b(Process4B);

    __ BIND(Exit);
    __ sub(c_rarg0, dst, doff);

    __ leave();
    __ ret(lr);

    return start;
  }

#ifdef LINUX

  // ARMv8.1 LSE versions of the atomic stubs used by Atomic::PlatformXX.
  //
  // If LSE is in use, generate LSE versions of all the stubs. The
  // non-LSE versions are in atomic_aarch64.S.

  // class AtomicStubMark records the entry point of a stub and the
  // stub pointer which will point to it. The stub pointer is set to
  // the entry point when ~AtomicStubMark() is called, which must be
  // after ICache::invalidate_range. This ensures safe publication of
  // the generated code.
  class AtomicStubMark {
    address _entry_point;
    aarch64_atomic_stub_t *_stub;
    MacroAssembler *_masm;
  public:
    AtomicStubMark(MacroAssembler *masm, aarch64_atomic_stub_t *stub) {
      _masm = masm;
      __ align(32);
      _entry_point = __ pc();
      _stub = stub;
    }
    ~AtomicStubMark() {
      *_stub = (aarch64_atomic_stub_t)_entry_point;
    }
  };

  // NB: For memory_order_conservative we need a trailing membar after
  // LSE atomic operations but not a leading membar.
  //
  // We don't need a leading membar because a clause in the Arm ARM
  // says:
  //
  //   Barrier-ordered-before
  //
  //   Barrier instructions order prior Memory effects before subsequent
  //   Memory effects generated by the same Observer. A read or a write
  //   RW1 is Barrier-ordered-before a read or a write RW 2 from the same
  //   Observer if and only if RW1 appears in program order before RW 2
  //   and [ ... ] at least one of RW 1 and RW 2 is generated by an atomic
  //   instruction with both Acquire and Release semantics.
  //
  // All the atomic instructions {ldaddal, swapal, casal} have Acquire
  // and Release semantics, therefore we don't need a leading
  // barrier. However, there is no corresponding Barrier-ordered-after
  // relationship, therefore we need a trailing membar to prevent a
  // later store or load from being reordered with the store in an
  // atomic instruction.
  //
  // This was checked by using the herd7 consistency model simulator
  // (http://diy.inria.fr/) with this test case:
  //
  // AArch64 LseCas
  // { 0:X1=x; 0:X2=y; 1:X1=x; 1:X2=y; }
  // P0 | P1;
  // LDR W4, [X2] | MOV W3, #0;
  // DMB LD       | MOV W4, #1;
  // LDR W3, [X1] | CASAL W3, W4, [X1];
  //              | DMB ISH;
  //              | STR W4, [X2];
  // exists
  // (0:X3=0 /\ 0:X4=1)
  //
  // If X3 == 0 && X4 == 1, the store to y in P1 has been reordered
  // with the store to x in P1. Without the DMB in P1 this may happen.
  //
  // At the time of writing we don't know of any AArch64 hardware that
  // reorders stores in this way, but the Reference Manual permits it.

  void gen_cas_entry(Assembler::operand_size size,
                     atomic_memory_order order) {
    Register prev = r3, ptr = c_rarg0, compare_val = c_rarg1,
      exchange_val = c_rarg2;
    bool acquire, release;
    switch (order) {
      case memory_order_relaxed:
        acquire = false;
        release = false;
        break;
      case memory_order_release:
        acquire = false;
        release = true;
        break;
      default:
        acquire = true;
        release = true;
        break;
    }
    __ mov(prev, compare_val);
    __ lse_cas(prev, exchange_val, ptr, size, acquire, release, /*not_pair*/true);
    if (order == memory_order_conservative) {
      __ membar(Assembler::StoreStore|Assembler::StoreLoad);
    }
    if (size == Assembler::xword) {
      __ mov(r0, prev);
    } else {
      __ movw(r0, prev);
    }
    __ ret(lr);
  }

  void gen_ldaddal_entry(Assembler::operand_size size) {
    Register prev = r2, addr = c_rarg0, incr = c_rarg1;
    __ ldaddal(size, incr, prev, addr);
    __ membar(Assembler::StoreStore|Assembler::StoreLoad);
    if (size == Assembler::xword) {
      __ mov(r0, prev);
    } else {
      __ movw(r0, prev);
    }
    __ ret(lr);
  }

  void gen_swpal_entry(Assembler::operand_size size) {
    Register prev = r2, addr = c_rarg0, incr = c_rarg1;
    __ swpal(size, incr, prev, addr);
    __ membar(Assembler::StoreStore|Assembler::StoreLoad);
    if (size == Assembler::xword) {
      __ mov(r0, prev);
    } else {
      __ movw(r0, prev);
    }
    __ ret(lr);
  }

  void generate_atomic_entry_points() {
    if (! UseLSE) {
      return;
    }

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "atomic entry points");
    address first_entry = __ pc();

    // All memory_order_conservative
    AtomicStubMark mark_fetch_add_4(_masm, &aarch64_atomic_fetch_add_4_impl);
    gen_ldaddal_entry(Assembler::word);
    AtomicStubMark mark_fetch_add_8(_masm, &aarch64_atomic_fetch_add_8_impl);
    gen_ldaddal_entry(Assembler::xword);

    AtomicStubMark mark_xchg_4(_masm, &aarch64_atomic_xchg_4_impl);
    gen_swpal_entry(Assembler::word);
    AtomicStubMark mark_xchg_8_impl(_masm, &aarch64_atomic_xchg_8_impl);
    gen_swpal_entry(Assembler::xword);

    // CAS, memory_order_conservative
    AtomicStubMark mark_cmpxchg_1(_masm, &aarch64_atomic_cmpxchg_1_impl);
    gen_cas_entry(MacroAssembler::byte, memory_order_conservative);
    AtomicStubMark mark_cmpxchg_4(_masm, &aarch64_atomic_cmpxchg_4_impl);
    gen_cas_entry(MacroAssembler::word, memory_order_conservative);
    AtomicStubMark mark_cmpxchg_8(_masm, &aarch64_atomic_cmpxchg_8_impl);
    gen_cas_entry(MacroAssembler::xword, memory_order_conservative);

    // CAS, memory_order_relaxed
    AtomicStubMark mark_cmpxchg_1_relaxed
      (_masm, &aarch64_atomic_cmpxchg_1_relaxed_impl);
    gen_cas_entry(MacroAssembler::byte, memory_order_relaxed);
    AtomicStubMark mark_cmpxchg_4_relaxed
      (_masm, &aarch64_atomic_cmpxchg_4_relaxed_impl);
    gen_cas_entry(MacroAssembler::word, memory_order_relaxed);
    AtomicStubMark mark_cmpxchg_8_relaxed
      (_masm, &aarch64_atomic_cmpxchg_8_relaxed_impl);
    gen_cas_entry(MacroAssembler::xword, memory_order_relaxed);

    AtomicStubMark mark_cmpxchg_4_release
      (_masm, &aarch64_atomic_cmpxchg_4_release_impl);
    gen_cas_entry(MacroAssembler::word, memory_order_release);
    AtomicStubMark mark_cmpxchg_8_release
      (_masm, &aarch64_atomic_cmpxchg_8_release_impl);
    gen_cas_entry(MacroAssembler::xword, memory_order_release);

    AtomicStubMark mark_cmpxchg_4_seq_cst
      (_masm, &aarch64_atomic_cmpxchg_4_seq_cst_impl);
    gen_cas_entry(MacroAssembler::word, memory_order_seq_cst);
    AtomicStubMark mark_cmpxchg_8_seq_cst
      (_masm, &aarch64_atomic_cmpxchg_8_seq_cst_impl);
    gen_cas_entry(MacroAssembler::xword, memory_order_seq_cst);

    ICache::invalidate_range(first_entry, __ pc() - first_entry);
  }
#endif // LINUX

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

#undef __
#define __ masm->

  address generate_throw_exception(const char* name,
                                   address runtime_entry,
                                   Register arg1 = noreg,
                                   Register arg2 = noreg) {
    // Information about frame layout at time of blocking runtime call.
    // Note that we only have to preserve callee-saved registers since
    // the compilers are responsible for supplying a continuation point
    // if they expect all registers to be preserved.
    // n.b. aarch64 asserts that frame::arg_reg_save_area_bytes == 0
    enum layout {
      rfp_off = 0,
      rfp_off2,
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

    __ enter(); // Save FP and LR before call

    assert(is_even(framesize/2), "sp not 16-byte aligned");

    // lr and fp are already in place
    __ sub(sp, rfp, ((unsigned)framesize-4) << LogBytesPerInt); // prolog

    int frame_complete = __ pc() - start;

    // Set up last_Java_sp and last_Java_fp
    address the_pc = __ pc();
    __ set_last_Java_frame(sp, rfp, the_pc, rscratch1);

    // Call runtime
    if (arg1 != noreg) {
      assert(arg2 != c_rarg1, "clobbered");
      __ mov(c_rarg1, arg1);
    }
    if (arg2 != noreg) {
      __ mov(c_rarg2, arg2);
    }
    __ mov(c_rarg0, rthread);
    BLOCK_COMMENT("call runtime_entry");
    __ mov(rscratch1, runtime_entry);
    __ blr(rscratch1);

    // Generate oop map
    OopMap* map = new OopMap(framesize, 0);

    oop_maps->add_gc_map(the_pc - start, map);

    __ reset_last_Java_frame(true);

    // Reinitialize the ptrue predicate register, in case the external runtime
    // call clobbers ptrue reg, as we may return to SVE compiled code.
    __ reinitialize_ptrue();

    __ leave();

    // check for pending exceptions
#ifdef ASSERT
    Label L;
    __ ldr(rscratch1, Address(rthread, Thread::pending_exception_offset()));
    __ cbnz(rscratch1, L);
    __ should_not_reach_here();
    __ bind(L);
#endif // ASSERT
    __ far_jump(RuntimeAddress(StubRoutines::forward_exception_entry()));


    // codeBlob framesize is in words (not VMRegImpl::slot_size)
    RuntimeStub* stub =
      RuntimeStub::new_runtime_stub(name,
                                    &code,
                                    frame_complete,
                                    (framesize >> (LogBytesPerWord - LogBytesPerInt)),
                                    oop_maps, false);
    return stub->entry_point();
  }

  class MontgomeryMultiplyGenerator : public MacroAssembler {

    Register Pa_base, Pb_base, Pn_base, Pm_base, inv, Rlen, Ra, Rb, Rm, Rn,
      Pa, Pb, Pn, Pm, Rhi_ab, Rlo_ab, Rhi_mn, Rlo_mn, t0, t1, t2, Ri, Rj;

    RegSet _toSave;
    bool _squaring;

  public:
    MontgomeryMultiplyGenerator (Assembler *as, bool squaring)
      : MacroAssembler(as->code()), _squaring(squaring) {

      // Register allocation

      RegSetIterator<> regs = (RegSet::range(r0, r26) - r18_tls).begin();
      Pa_base = *regs;       // Argument registers
      if (squaring)
        Pb_base = Pa_base;
      else
        Pb_base = *++regs;
      Pn_base = *++regs;
      Rlen= *++regs;
      inv = *++regs;
      Pm_base = *++regs;

                          // Working registers:
      Ra =  *++regs;        // The current digit of a, b, n, and m.
      Rb =  *++regs;
      Rm =  *++regs;
      Rn =  *++regs;

      Pa =  *++regs;        // Pointers to the current/next digit of a, b, n, and m.
      Pb =  *++regs;
      Pm =  *++regs;
      Pn =  *++regs;

      t0 =  *++regs;        // Three registers which form a
      t1 =  *++regs;        // triple-precision accumuator.
      t2 =  *++regs;

      Ri =  *++regs;        // Inner and outer loop indexes.
      Rj =  *++regs;

      Rhi_ab = *++regs;     // Product registers: low and high parts
      Rlo_ab = *++regs;     // of a*b and m*n.
      Rhi_mn = *++regs;
      Rlo_mn = *++regs;

      // r19 and up are callee-saved.
      _toSave = RegSet::range(r19, *regs) + Pm_base;
    }

  private:
    void save_regs() {
      push(_toSave, sp);
    }

    void restore_regs() {
      pop(_toSave, sp);
    }

    template <typename T>
    void unroll_2(Register count, T block) {
      Label loop, end, odd;
      tbnz(count, 0, odd);
      cbz(count, end);
      align(16);
      bind(loop);
      (this->*block)();
      bind(odd);
      (this->*block)();
      subs(count, count, 2);
      br(Assembler::GT, loop);
      bind(end);
    }

    template <typename T>
    void unroll_2(Register count, T block, Register d, Register s, Register tmp) {
      Label loop, end, odd;
      tbnz(count, 0, odd);
      cbz(count, end);
      align(16);
      bind(loop);
      (this->*block)(d, s, tmp);
      bind(odd);
      (this->*block)(d, s, tmp);
      subs(count, count, 2);
      br(Assembler::GT, loop);
      bind(end);
    }

    void pre1(RegisterOrConstant i) {
      block_comment("pre1");
      // Pa = Pa_base;
      // Pb = Pb_base + i;
      // Pm = Pm_base;
      // Pn = Pn_base + i;
      // Ra = *Pa;
      // Rb = *Pb;
      // Rm = *Pm;
      // Rn = *Pn;
      ldr(Ra, Address(Pa_base));
      ldr(Rb, Address(Pb_base, i, Address::uxtw(LogBytesPerWord)));
      ldr(Rm, Address(Pm_base));
      ldr(Rn, Address(Pn_base, i, Address::uxtw(LogBytesPerWord)));
      lea(Pa, Address(Pa_base));
      lea(Pb, Address(Pb_base, i, Address::uxtw(LogBytesPerWord)));
      lea(Pm, Address(Pm_base));
      lea(Pn, Address(Pn_base, i, Address::uxtw(LogBytesPerWord)));

      // Zero the m*n result.
      mov(Rhi_mn, zr);
      mov(Rlo_mn, zr);
    }

    // The core multiply-accumulate step of a Montgomery
    // multiplication.  The idea is to schedule operations as a
    // pipeline so that instructions with long latencies (loads and
    // multiplies) have time to complete before their results are
    // used.  This most benefits in-order implementations of the
    // architecture but out-of-order ones also benefit.
    void step() {
      block_comment("step");
      // MACC(Ra, Rb, t0, t1, t2);
      // Ra = *++Pa;
      // Rb = *--Pb;
      umulh(Rhi_ab, Ra, Rb);
      mul(Rlo_ab, Ra, Rb);
      ldr(Ra, pre(Pa, wordSize));
      ldr(Rb, pre(Pb, -wordSize));
      acc(Rhi_mn, Rlo_mn, t0, t1, t2); // The pending m*n from the
                                       // previous iteration.
      // MACC(Rm, Rn, t0, t1, t2);
      // Rm = *++Pm;
      // Rn = *--Pn;
      umulh(Rhi_mn, Rm, Rn);
      mul(Rlo_mn, Rm, Rn);
      ldr(Rm, pre(Pm, wordSize));
      ldr(Rn, pre(Pn, -wordSize));
      acc(Rhi_ab, Rlo_ab, t0, t1, t2);
    }

    void post1() {
      block_comment("post1");

      // MACC(Ra, Rb, t0, t1, t2);
      // Ra = *++Pa;
      // Rb = *--Pb;
      umulh(Rhi_ab, Ra, Rb);
      mul(Rlo_ab, Ra, Rb);
      acc(Rhi_mn, Rlo_mn, t0, t1, t2);  // The pending m*n
      acc(Rhi_ab, Rlo_ab, t0, t1, t2);

      // *Pm = Rm = t0 * inv;
      mul(Rm, t0, inv);
      str(Rm, Address(Pm));

      // MACC(Rm, Rn, t0, t1, t2);
      // t0 = t1; t1 = t2; t2 = 0;
      umulh(Rhi_mn, Rm, Rn);

#ifndef PRODUCT
      // assert(m[i] * n[0] + t0 == 0, "broken Montgomery multiply");
      {
        mul(Rlo_mn, Rm, Rn);
        add(Rlo_mn, t0, Rlo_mn);
        Label ok;
        cbz(Rlo_mn, ok); {
          stop("broken Montgomery multiply");
        } bind(ok);
      }
#endif
      // We have very carefully set things up so that
      // m[i]*n[0] + t0 == 0 (mod b), so we don't have to calculate
      // the lower half of Rm * Rn because we know the result already:
      // it must be -t0.  t0 + (-t0) must generate a carry iff
      // t0 != 0.  So, rather than do a mul and an adds we just set
      // the carry flag iff t0 is nonzero.
      //
      // mul(Rlo_mn, Rm, Rn);
      // adds(zr, t0, Rlo_mn);
      subs(zr, t0, 1); // Set carry iff t0 is nonzero
      adcs(t0, t1, Rhi_mn);
      adc(t1, t2, zr);
      mov(t2, zr);
    }

    void pre2(RegisterOrConstant i, RegisterOrConstant len) {
      block_comment("pre2");
      // Pa = Pa_base + i-len;
      // Pb = Pb_base + len;
      // Pm = Pm_base + i-len;
      // Pn = Pn_base + len;

      if (i.is_register()) {
        sub(Rj, i.as_register(), len);
      } else {
        mov(Rj, i.as_constant());
        sub(Rj, Rj, len);
      }
      // Rj == i-len

      lea(Pa, Address(Pa_base, Rj, Address::uxtw(LogBytesPerWord)));
      lea(Pb, Address(Pb_base, len, Address::uxtw(LogBytesPerWord)));
      lea(Pm, Address(Pm_base, Rj, Address::uxtw(LogBytesPerWord)));
      lea(Pn, Address(Pn_base, len, Address::uxtw(LogBytesPerWord)));

      // Ra = *++Pa;
      // Rb = *--Pb;
      // Rm = *++Pm;
      // Rn = *--Pn;
      ldr(Ra, pre(Pa, wordSize));
      ldr(Rb, pre(Pb, -wordSize));
      ldr(Rm, pre(Pm, wordSize));
      ldr(Rn, pre(Pn, -wordSize));

      mov(Rhi_mn, zr);
      mov(Rlo_mn, zr);
    }

    void post2(RegisterOrConstant i, RegisterOrConstant len) {
      block_comment("post2");
      if (i.is_constant()) {
        mov(Rj, i.as_constant()-len.as_constant());
      } else {
        sub(Rj, i.as_register(), len);
      }

      adds(t0, t0, Rlo_mn); // The pending m*n, low part

      // As soon as we know the least significant digit of our result,
      // store it.
      // Pm_base[i-len] = t0;
      str(t0, Address(Pm_base, Rj, Address::uxtw(LogBytesPerWord)));

      // t0 = t1; t1 = t2; t2 = 0;
      adcs(t0, t1, Rhi_mn); // The pending m*n, high part
      adc(t1, t2, zr);
      mov(t2, zr);
    }

    // A carry in t0 after Montgomery multiplication means that we
    // should subtract multiples of n from our result in m.  We'll
    // keep doing that until there is no carry.
    void normalize(RegisterOrConstant len) {
      block_comment("normalize");
      // while (t0)
      //   t0 = sub(Pm_base, Pn_base, t0, len);
      Label loop, post, again;
      Register cnt = t1, i = t2; // Re-use registers; we're done with them now
      cbz(t0, post); {
        bind(again); {
          mov(i, zr);
          mov(cnt, len);
          ldr(Rm, Address(Pm_base, i, Address::uxtw(LogBytesPerWord)));
          ldr(Rn, Address(Pn_base, i, Address::uxtw(LogBytesPerWord)));
          subs(zr, zr, zr); // set carry flag, i.e. no borrow
          align(16);
          bind(loop); {
            sbcs(Rm, Rm, Rn);
            str(Rm, Address(Pm_base, i, Address::uxtw(LogBytesPerWord)));
            add(i, i, 1);
            ldr(Rm, Address(Pm_base, i, Address::uxtw(LogBytesPerWord)));
            ldr(Rn, Address(Pn_base, i, Address::uxtw(LogBytesPerWord)));
            sub(cnt, cnt, 1);
          } cbnz(cnt, loop);
          sbc(t0, t0, zr);
        } cbnz(t0, again);
      } bind(post);
    }

    // Move memory at s to d, reversing words.
    //    Increments d to end of copied memory
    //    Destroys tmp1, tmp2
    //    Preserves len
    //    Leaves s pointing to the address which was in d at start
    void reverse(Register d, Register s, Register len, Register tmp1, Register tmp2) {
      assert(tmp1 < r19 && tmp2 < r19, "register corruption");

      lea(s, Address(s, len, Address::uxtw(LogBytesPerWord)));
      mov(tmp1, len);
      unroll_2(tmp1, &MontgomeryMultiplyGenerator::reverse1, d, s, tmp2);
      sub(s, d, len, ext::uxtw, LogBytesPerWord);
    }
    // where
    void reverse1(Register d, Register s, Register tmp) {
      ldr(tmp, pre(s, -wordSize));
      ror(tmp, tmp, 32);
      str(tmp, post(d, wordSize));
    }

    void step_squaring() {
      // An extra ACC
      step();
      acc(Rhi_ab, Rlo_ab, t0, t1, t2);
    }

    void last_squaring(RegisterOrConstant i) {
      Label dont;
      // if ((i & 1) == 0) {
      tbnz(i.as_register(), 0, dont); {
        // MACC(Ra, Rb, t0, t1, t2);
        // Ra = *++Pa;
        // Rb = *--Pb;
        umulh(Rhi_ab, Ra, Rb);
        mul(Rlo_ab, Ra, Rb);
        acc(Rhi_ab, Rlo_ab, t0, t1, t2);
      } bind(dont);
    }

    void extra_step_squaring() {
      acc(Rhi_mn, Rlo_mn, t0, t1, t2);  // The pending m*n

      // MACC(Rm, Rn, t0, t1, t2);
      // Rm = *++Pm;
      // Rn = *--Pn;
      umulh(Rhi_mn, Rm, Rn);
      mul(Rlo_mn, Rm, Rn);
      ldr(Rm, pre(Pm, wordSize));
      ldr(Rn, pre(Pn, -wordSize));
    }

    void post1_squaring() {
      acc(Rhi_mn, Rlo_mn, t0, t1, t2);  // The pending m*n

      // *Pm = Rm = t0 * inv;
      mul(Rm, t0, inv);
      str(Rm, Address(Pm));

      // MACC(Rm, Rn, t0, t1, t2);
      // t0 = t1; t1 = t2; t2 = 0;
      umulh(Rhi_mn, Rm, Rn);

#ifndef PRODUCT
      // assert(m[i] * n[0] + t0 == 0, "broken Montgomery multiply");
      {
        mul(Rlo_mn, Rm, Rn);
        add(Rlo_mn, t0, Rlo_mn);
        Label ok;
        cbz(Rlo_mn, ok); {
          stop("broken Montgomery multiply");
        } bind(ok);
      }
#endif
      // We have very carefully set things up so that
      // m[i]*n[0] + t0 == 0 (mod b), so we don't have to calculate
      // the lower half of Rm * Rn because we know the result already:
      // it must be -t0.  t0 + (-t0) must generate a carry iff
      // t0 != 0.  So, rather than do a mul and an adds we just set
      // the carry flag iff t0 is nonzero.
      //
      // mul(Rlo_mn, Rm, Rn);
      // adds(zr, t0, Rlo_mn);
      subs(zr, t0, 1); // Set carry iff t0 is nonzero
      adcs(t0, t1, Rhi_mn);
      adc(t1, t2, zr);
      mov(t2, zr);
    }

    void acc(Register Rhi, Register Rlo,
             Register t0, Register t1, Register t2) {
      adds(t0, t0, Rlo);
      adcs(t1, t1, Rhi);
      adc(t2, t2, zr);
    }

  public:
    /**
     * Fast Montgomery multiplication.  The derivation of the
     * algorithm is in A Cryptographic Library for the Motorola
     * DSP56000, Dusse and Kaliski, Proc. EUROCRYPT 90, pp. 230-237.
     *
     * Arguments:
     *
     * Inputs for multiplication:
     *   c_rarg0   - int array elements a
     *   c_rarg1   - int array elements b
     *   c_rarg2   - int array elements n (the modulus)
     *   c_rarg3   - int length
     *   c_rarg4   - int inv
     *   c_rarg5   - int array elements m (the result)
     *
     * Inputs for squaring:
     *   c_rarg0   - int array elements a
     *   c_rarg1   - int array elements n (the modulus)
     *   c_rarg2   - int length
     *   c_rarg3   - int inv
     *   c_rarg4   - int array elements m (the result)
     *
     */
    address generate_multiply() {
      Label argh, nothing;
      bind(argh);
      stop("MontgomeryMultiply total_allocation must be <= 8192");

      align(CodeEntryAlignment);
      address entry = pc();

      cbzw(Rlen, nothing);

      enter();

      // Make room.
      cmpw(Rlen, 512);
      br(Assembler::HI, argh);
      sub(Ra, sp, Rlen, ext::uxtw, exact_log2(4 * sizeof (jint)));
      andr(sp, Ra, -2 * wordSize);

      lsrw(Rlen, Rlen, 1);  // length in longwords = len/2

      {
        // Copy input args, reversing as we go.  We use Ra as a
        // temporary variable.
        reverse(Ra, Pa_base, Rlen, t0, t1);
        if (!_squaring)
          reverse(Ra, Pb_base, Rlen, t0, t1);
        reverse(Ra, Pn_base, Rlen, t0, t1);
      }

      // Push all call-saved registers and also Pm_base which we'll need
      // at the end.
      save_regs();

#ifndef PRODUCT
      // assert(inv * n[0] == -1UL, "broken inverse in Montgomery multiply");
      {
        ldr(Rn, Address(Pn_base, 0));
        mul(Rlo_mn, Rn, inv);
        subs(zr, Rlo_mn, -1);
        Label ok;
        br(EQ, ok); {
          stop("broken inverse in Montgomery multiply");
        } bind(ok);
      }
#endif

      mov(Pm_base, Ra);

      mov(t0, zr);
      mov(t1, zr);
      mov(t2, zr);

      block_comment("for (int i = 0; i < len; i++) {");
      mov(Ri, zr); {
        Label loop, end;
        cmpw(Ri, Rlen);
        br(Assembler::GE, end);

        bind(loop);
        pre1(Ri);

        block_comment("  for (j = i; j; j--) {"); {
          movw(Rj, Ri);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::step);
        } block_comment("  } // j");

        post1();
        addw(Ri, Ri, 1);
        cmpw(Ri, Rlen);
        br(Assembler::LT, loop);
        bind(end);
        block_comment("} // i");
      }

      block_comment("for (int i = len; i < 2*len; i++) {");
      mov(Ri, Rlen); {
        Label loop, end;
        cmpw(Ri, Rlen, Assembler::LSL, 1);
        br(Assembler::GE, end);

        bind(loop);
        pre2(Ri, Rlen);

        block_comment("  for (j = len*2-i-1; j; j--) {"); {
          lslw(Rj, Rlen, 1);
          subw(Rj, Rj, Ri);
          subw(Rj, Rj, 1);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::step);
        } block_comment("  } // j");

        post2(Ri, Rlen);
        addw(Ri, Ri, 1);
        cmpw(Ri, Rlen, Assembler::LSL, 1);
        br(Assembler::LT, loop);
        bind(end);
      }
      block_comment("} // i");

      normalize(Rlen);

      mov(Ra, Pm_base);  // Save Pm_base in Ra
      restore_regs();  // Restore caller's Pm_base

      // Copy our result into caller's Pm_base
      reverse(Pm_base, Ra, Rlen, t0, t1);

      leave();
      bind(nothing);
      ret(lr);

      return entry;
    }
    // In C, approximately:

    // void
    // montgomery_multiply(julong Pa_base[], julong Pb_base[],
    //                     julong Pn_base[], julong Pm_base[],
    //                     julong inv, int len) {
    //   julong t0 = 0, t1 = 0, t2 = 0; // Triple-precision accumulator
    //   julong *Pa, *Pb, *Pn, *Pm;
    //   julong Ra, Rb, Rn, Rm;

    //   int i;

    //   assert(inv * Pn_base[0] == -1UL, "broken inverse in Montgomery multiply");

    //   for (i = 0; i < len; i++) {
    //     int j;

    //     Pa = Pa_base;
    //     Pb = Pb_base + i;
    //     Pm = Pm_base;
    //     Pn = Pn_base + i;

    //     Ra = *Pa;
    //     Rb = *Pb;
    //     Rm = *Pm;
    //     Rn = *Pn;

    //     int iters = i;
    //     for (j = 0; iters--; j++) {
    //       assert(Ra == Pa_base[j] && Rb == Pb_base[i-j], "must be");
    //       MACC(Ra, Rb, t0, t1, t2);
    //       Ra = *++Pa;
    //       Rb = *--Pb;
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }

    //     assert(Ra == Pa_base[i] && Rb == Pb_base[0], "must be");
    //     MACC(Ra, Rb, t0, t1, t2);
    //     *Pm = Rm = t0 * inv;
    //     assert(Rm == Pm_base[i] && Rn == Pn_base[0], "must be");
    //     MACC(Rm, Rn, t0, t1, t2);

    //     assert(t0 == 0, "broken Montgomery multiply");

    //     t0 = t1; t1 = t2; t2 = 0;
    //   }

    //   for (i = len; i < 2*len; i++) {
    //     int j;

    //     Pa = Pa_base + i-len;
    //     Pb = Pb_base + len;
    //     Pm = Pm_base + i-len;
    //     Pn = Pn_base + len;

    //     Ra = *++Pa;
    //     Rb = *--Pb;
    //     Rm = *++Pm;
    //     Rn = *--Pn;

    //     int iters = len*2-i-1;
    //     for (j = i-len+1; iters--; j++) {
    //       assert(Ra == Pa_base[j] && Rb == Pb_base[i-j], "must be");
    //       MACC(Ra, Rb, t0, t1, t2);
    //       Ra = *++Pa;
    //       Rb = *--Pb;
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }

    //     Pm_base[i-len] = t0;
    //     t0 = t1; t1 = t2; t2 = 0;
    //   }

    //   while (t0)
    //     t0 = sub(Pm_base, Pn_base, t0, len);
    // }

    /**
     * Fast Montgomery squaring.  This uses asymptotically 25% fewer
     * multiplies than Montgomery multiplication so it should be up to
     * 25% faster.  However, its loop control is more complex and it
     * may actually run slower on some machines.
     *
     * Arguments:
     *
     * Inputs:
     *   c_rarg0   - int array elements a
     *   c_rarg1   - int array elements n (the modulus)
     *   c_rarg2   - int length
     *   c_rarg3   - int inv
     *   c_rarg4   - int array elements m (the result)
     *
     */
    address generate_square() {
      Label argh;
      bind(argh);
      stop("MontgomeryMultiply total_allocation must be <= 8192");

      align(CodeEntryAlignment);
      address entry = pc();

      enter();

      // Make room.
      cmpw(Rlen, 512);
      br(Assembler::HI, argh);
      sub(Ra, sp, Rlen, ext::uxtw, exact_log2(4 * sizeof (jint)));
      andr(sp, Ra, -2 * wordSize);

      lsrw(Rlen, Rlen, 1);  // length in longwords = len/2

      {
        // Copy input args, reversing as we go.  We use Ra as a
        // temporary variable.
        reverse(Ra, Pa_base, Rlen, t0, t1);
        reverse(Ra, Pn_base, Rlen, t0, t1);
      }

      // Push all call-saved registers and also Pm_base which we'll need
      // at the end.
      save_regs();

      mov(Pm_base, Ra);

      mov(t0, zr);
      mov(t1, zr);
      mov(t2, zr);

      block_comment("for (int i = 0; i < len; i++) {");
      mov(Ri, zr); {
        Label loop, end;
        bind(loop);
        cmp(Ri, Rlen);
        br(Assembler::GE, end);

        pre1(Ri);

        block_comment("for (j = (i+1)/2; j; j--) {"); {
          add(Rj, Ri, 1);
          lsr(Rj, Rj, 1);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::step_squaring);
        } block_comment("  } // j");

        last_squaring(Ri);

        block_comment("  for (j = i/2; j; j--) {"); {
          lsr(Rj, Ri, 1);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::extra_step_squaring);
        } block_comment("  } // j");

        post1_squaring();
        add(Ri, Ri, 1);
        cmp(Ri, Rlen);
        br(Assembler::LT, loop);

        bind(end);
        block_comment("} // i");
      }

      block_comment("for (int i = len; i < 2*len; i++) {");
      mov(Ri, Rlen); {
        Label loop, end;
        bind(loop);
        cmp(Ri, Rlen, Assembler::LSL, 1);
        br(Assembler::GE, end);

        pre2(Ri, Rlen);

        block_comment("  for (j = (2*len-i-1)/2; j; j--) {"); {
          lsl(Rj, Rlen, 1);
          sub(Rj, Rj, Ri);
          sub(Rj, Rj, 1);
          lsr(Rj, Rj, 1);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::step_squaring);
        } block_comment("  } // j");

        last_squaring(Ri);

        block_comment("  for (j = (2*len-i)/2; j; j--) {"); {
          lsl(Rj, Rlen, 1);
          sub(Rj, Rj, Ri);
          lsr(Rj, Rj, 1);
          unroll_2(Rj, &MontgomeryMultiplyGenerator::extra_step_squaring);
        } block_comment("  } // j");

        post2(Ri, Rlen);
        add(Ri, Ri, 1);
        cmp(Ri, Rlen, Assembler::LSL, 1);

        br(Assembler::LT, loop);
        bind(end);
        block_comment("} // i");
      }

      normalize(Rlen);

      mov(Ra, Pm_base);  // Save Pm_base in Ra
      restore_regs();  // Restore caller's Pm_base

      // Copy our result into caller's Pm_base
      reverse(Pm_base, Ra, Rlen, t0, t1);

      leave();
      ret(lr);

      return entry;
    }
    // In C, approximately:

    // void
    // montgomery_square(julong Pa_base[], julong Pn_base[],
    //                   julong Pm_base[], julong inv, int len) {
    //   julong t0 = 0, t1 = 0, t2 = 0; // Triple-precision accumulator
    //   julong *Pa, *Pb, *Pn, *Pm;
    //   julong Ra, Rb, Rn, Rm;

    //   int i;

    //   assert(inv * Pn_base[0] == -1UL, "broken inverse in Montgomery multiply");

    //   for (i = 0; i < len; i++) {
    //     int j;

    //     Pa = Pa_base;
    //     Pb = Pa_base + i;
    //     Pm = Pm_base;
    //     Pn = Pn_base + i;

    //     Ra = *Pa;
    //     Rb = *Pb;
    //     Rm = *Pm;
    //     Rn = *Pn;

    //     int iters = (i+1)/2;
    //     for (j = 0; iters--; j++) {
    //       assert(Ra == Pa_base[j] && Rb == Pa_base[i-j], "must be");
    //       MACC2(Ra, Rb, t0, t1, t2);
    //       Ra = *++Pa;
    //       Rb = *--Pb;
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }
    //     if ((i & 1) == 0) {
    //       assert(Ra == Pa_base[j], "must be");
    //       MACC(Ra, Ra, t0, t1, t2);
    //     }
    //     iters = i/2;
    //     assert(iters == i-j, "must be");
    //     for (; iters--; j++) {
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }

    //     *Pm = Rm = t0 * inv;
    //     assert(Rm == Pm_base[i] && Rn == Pn_base[0], "must be");
    //     MACC(Rm, Rn, t0, t1, t2);

    //     assert(t0 == 0, "broken Montgomery multiply");

    //     t0 = t1; t1 = t2; t2 = 0;
    //   }

    //   for (i = len; i < 2*len; i++) {
    //     int start = i-len+1;
    //     int end = start + (len - start)/2;
    //     int j;

    //     Pa = Pa_base + i-len;
    //     Pb = Pa_base + len;
    //     Pm = Pm_base + i-len;
    //     Pn = Pn_base + len;

    //     Ra = *++Pa;
    //     Rb = *--Pb;
    //     Rm = *++Pm;
    //     Rn = *--Pn;

    //     int iters = (2*len-i-1)/2;
    //     assert(iters == end-start, "must be");
    //     for (j = start; iters--; j++) {
    //       assert(Ra == Pa_base[j] && Rb == Pa_base[i-j], "must be");
    //       MACC2(Ra, Rb, t0, t1, t2);
    //       Ra = *++Pa;
    //       Rb = *--Pb;
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }
    //     if ((i & 1) == 0) {
    //       assert(Ra == Pa_base[j], "must be");
    //       MACC(Ra, Ra, t0, t1, t2);
    //     }
    //     iters =  (2*len-i)/2;
    //     assert(iters == len-j, "must be");
    //     for (; iters--; j++) {
    //       assert(Rm == Pm_base[j] && Rn == Pn_base[i-j], "must be");
    //       MACC(Rm, Rn, t0, t1, t2);
    //       Rm = *++Pm;
    //       Rn = *--Pn;
    //     }
    //     Pm_base[i-len] = t0;
    //     t0 = t1; t1 = t2; t2 = 0;
    //   }

    //   while (t0)
    //     t0 = sub(Pm_base, Pn_base, t0, len);
    // }
  };


  // Initialization
  void generate_initial() {
    // Generate initial stubs and initializes the entry points

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

    // Build this early so it's available for the interpreter.
    StubRoutines::_throw_StackOverflowError_entry =
      generate_throw_exception("StackOverflowError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::throw_StackOverflowError));
    StubRoutines::_throw_delayed_StackOverflowError_entry =
      generate_throw_exception("delayed StackOverflowError throw_exception",
                               CAST_FROM_FN_PTR(address,
                                                SharedRuntime::throw_delayed_StackOverflowError));
    if (UseCRC32Intrinsics) {
      // set table address before stub generation which use it
      StubRoutines::_crc_table_adr = (address)StubRoutines::aarch64::_crc_table;
      StubRoutines::_updateBytesCRC32 = generate_updateBytesCRC32();
    }

    if (UseCRC32CIntrinsics) {
      StubRoutines::_updateBytesCRC32C = generate_updateBytesCRC32C();
    }

    // Disabled until JDK-8210858 is fixed
    // if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dlog)) {
    //   StubRoutines::_dlog = generate_dlog();
    // }

    if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dsin)) {
      StubRoutines::_dsin = generate_dsin_dcos(/* isCos = */ false);
    }

    if (vmIntrinsics::is_intrinsic_available(vmIntrinsics::_dcos)) {
      StubRoutines::_dcos = generate_dsin_dcos(/* isCos = */ true);
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
    // support for verify_oop (must happen after universe_init)
    StubRoutines::_verify_oop_subroutine_entry     = generate_verify_oop();
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

    StubRoutines::aarch64::_vector_iota_indices    = generate_iota_indices("iota_indices");

    // arraycopy stubs used by compilers
    generate_arraycopy_stubs();

    // has negatives stub for large arrays.
    StubRoutines::aarch64::_has_negatives = generate_has_negatives(StubRoutines::aarch64::_has_negatives_long);

    // array equals stub for large arrays.
    if (!UseSimpleArrayEquals) {
      StubRoutines::aarch64::_large_array_equals = generate_large_array_equals();
    }

    generate_compare_long_strings();

    generate_string_indexof_stubs();

    // byte_array_inflate stub for large arrays.
    StubRoutines::aarch64::_large_byte_array_inflate = generate_large_byte_array_inflate();

    BarrierSetNMethod* bs_nm = BarrierSet::barrier_set()->barrier_set_nmethod();
    if (bs_nm != NULL) {
      StubRoutines::aarch64::_method_entry_barrier = generate_method_entry_barrier();
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

    if (UseSIMDForBigIntegerShiftIntrinsics) {
      StubRoutines::_bigIntegerRightShiftWorker = generate_bigIntegerRightShift();
      StubRoutines::_bigIntegerLeftShiftWorker  = generate_bigIntegerLeftShift();
    }

    if (UseMontgomeryMultiplyIntrinsic) {
      StubCodeMark mark(this, "StubRoutines", "montgomeryMultiply");
      MontgomeryMultiplyGenerator g(_masm, /*squaring*/false);
      StubRoutines::_montgomeryMultiply = g.generate_multiply();
    }

    if (UseMontgomerySquareIntrinsic) {
      StubCodeMark mark(this, "StubRoutines", "montgomerySquare");
      MontgomeryMultiplyGenerator g(_masm, /*squaring*/true);
      // We use generate_multiply() rather than generate_square()
      // because it's faster for the sizes of modulus we care about.
      StubRoutines::_montgomerySquare = g.generate_multiply();
    }
#endif // COMPILER2

    // generate GHASH intrinsics code
    if (UseGHASHIntrinsics) {
      StubRoutines::_ghash_processBlocks = generate_ghash_processBlocks();
    }

    if (UseBASE64Intrinsics) {
        StubRoutines::_base64_encodeBlock = generate_base64_encodeBlock();
        StubRoutines::_base64_decodeBlock = generate_base64_decodeBlock();
    }

    // data cache line writeback
    StubRoutines::_data_cache_writeback = generate_data_cache_writeback();
    StubRoutines::_data_cache_writeback_sync = generate_data_cache_writeback_sync();

    if (UseAESIntrinsics) {
      StubRoutines::_aescrypt_encryptBlock = generate_aescrypt_encryptBlock();
      StubRoutines::_aescrypt_decryptBlock = generate_aescrypt_decryptBlock();
      StubRoutines::_cipherBlockChaining_encryptAESCrypt = generate_cipherBlockChaining_encryptAESCrypt();
      StubRoutines::_cipherBlockChaining_decryptAESCrypt = generate_cipherBlockChaining_decryptAESCrypt();
    }

    if (UseSHA1Intrinsics) {
      StubRoutines::_sha1_implCompress     = generate_sha1_implCompress(false,   "sha1_implCompress");
      StubRoutines::_sha1_implCompressMB   = generate_sha1_implCompress(true,    "sha1_implCompressMB");
    }
    if (UseSHA256Intrinsics) {
      StubRoutines::_sha256_implCompress   = generate_sha256_implCompress(false, "sha256_implCompress");
      StubRoutines::_sha256_implCompressMB = generate_sha256_implCompress(true,  "sha256_implCompressMB");
    }
    if (UseSHA512Intrinsics) {
      StubRoutines::_sha512_implCompress   = generate_sha512_implCompress(false, "sha512_implCompress");
      StubRoutines::_sha512_implCompressMB = generate_sha512_implCompress(true,  "sha512_implCompressMB");
    }
    if (UseSHA3Intrinsics) {
      StubRoutines::_sha3_implCompress     = generate_sha3_implCompress(false,   "sha3_implCompress");
      StubRoutines::_sha3_implCompressMB   = generate_sha3_implCompress(true,    "sha3_implCompressMB");
    }

    // generate Adler32 intrinsics code
    if (UseAdler32Intrinsics) {
      StubRoutines::_updateBytesAdler32 = generate_updateBytesAdler32();
    }

#ifdef LINUX

    generate_atomic_entry_points();

#endif // LINUX

    StubRoutines::aarch64::set_completed();
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

#define UCM_TABLE_MAX_ENTRIES 8
void StubGenerator_generate(CodeBuffer* code, bool all) {
  if (UnsafeCopyMemory::_table == NULL) {
    UnsafeCopyMemory::create_table(UCM_TABLE_MAX_ENTRIES);
  }
  StubGenerator g(code, all);
}


#ifdef LINUX

// Define pointers to atomic stubs and initialize them to point to the
// code in atomic_aarch64.S.

#define DEFAULT_ATOMIC_OP(OPNAME, SIZE, RELAXED)                                \
  extern "C" uint64_t aarch64_atomic_ ## OPNAME ## _ ## SIZE ## RELAXED ## _default_impl \
    (volatile void *ptr, uint64_t arg1, uint64_t arg2);                 \
  aarch64_atomic_stub_t aarch64_atomic_ ## OPNAME ## _ ## SIZE ## RELAXED ## _impl \
    = aarch64_atomic_ ## OPNAME ## _ ## SIZE ## RELAXED ## _default_impl;

DEFAULT_ATOMIC_OP(fetch_add, 4, )
DEFAULT_ATOMIC_OP(fetch_add, 8, )
DEFAULT_ATOMIC_OP(xchg, 4, )
DEFAULT_ATOMIC_OP(xchg, 8, )
DEFAULT_ATOMIC_OP(cmpxchg, 1, )
DEFAULT_ATOMIC_OP(cmpxchg, 4, )
DEFAULT_ATOMIC_OP(cmpxchg, 8, )
DEFAULT_ATOMIC_OP(cmpxchg, 1, _relaxed)
DEFAULT_ATOMIC_OP(cmpxchg, 4, _relaxed)
DEFAULT_ATOMIC_OP(cmpxchg, 8, _relaxed)
DEFAULT_ATOMIC_OP(cmpxchg, 4, _release)
DEFAULT_ATOMIC_OP(cmpxchg, 8, _release)
DEFAULT_ATOMIC_OP(cmpxchg, 4, _seq_cst)
DEFAULT_ATOMIC_OP(cmpxchg, 8, _seq_cst)

#undef DEFAULT_ATOMIC_OP

#endif // LINUX
