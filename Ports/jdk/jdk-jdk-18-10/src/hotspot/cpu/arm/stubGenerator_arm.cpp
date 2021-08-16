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
#include "asm/assembler.inline.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "interpreter/interpreter.hpp"
#include "memory/universe.hpp"
#include "nativeInst_arm.hpp"
#include "oops/instanceOop.hpp"
#include "oops/method.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

// Declaration and definition of StubGenerator (no .hpp file).
// For a more detailed description of the stub routine structure
// see the comment in stubRoutines.hpp

#define __ _masm->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

// -------------------------------------------------------------------------------------------------------------------------
// Stub Code definitions

// Platform dependent parameters for array copy stubs

// Note: we have noticed a huge change in behavior on a microbenchmark
// from platform to platform depending on the configuration.

// Instead of adding a series of command line options (which
// unfortunately have to be done in the shared file and cannot appear
// only in the ARM port), the tested result are hard-coded here in a set
// of options, selected by specifying 'ArmCopyPlatform'

// Currently, this 'platform' is hardcoded to a value that is a good
// enough trade-off.  However, one can easily modify this file to test
// the hard-coded configurations or create new ones. If the gain is
// significant, we could decide to either add command line options or
// add code to automatically choose a configuration.

// see comments below for the various configurations created
#define DEFAULT_ARRAYCOPY_CONFIG 0
#define TEGRA2_ARRAYCOPY_CONFIG 1
#define IMX515_ARRAYCOPY_CONFIG 2

// Hard coded choices (XXX: could be changed to a command line option)
#define ArmCopyPlatform DEFAULT_ARRAYCOPY_CONFIG

#define ArmCopyCacheLineSize 32 // not worth optimizing to 64 according to measured gains

// configuration for each kind of loop
typedef struct {
  int pld_distance;       // prefetch distance (0 => no prefetch, <0: prefetch_before);
  bool split_ldm;         // if true, split each STM in STMs with fewer registers
  bool split_stm;         // if true, split each LTM in LTMs with fewer registers
} arraycopy_loop_config;

// configuration for all loops
typedef struct {
  // const char *description;
  arraycopy_loop_config forward_aligned;
  arraycopy_loop_config backward_aligned;
  arraycopy_loop_config forward_shifted;
  arraycopy_loop_config backward_shifted;
} arraycopy_platform_config;

// configured platforms
static arraycopy_platform_config arraycopy_configurations[] = {
  // configuration parameters for arraycopy loops

  // Configurations were chosen based on manual analysis of benchmark
  // results, minimizing overhead with respect to best results on the
  // different test cases.

  // Prefetch before is always favored since it avoids dirtying the
  // cache uselessly for small copies. Code for prefetch after has
  // been kept in case the difference is significant for some
  // platforms but we might consider dropping it.

  // distance, ldm, stm
  {
    // default: tradeoff tegra2/imx515/nv-tegra2,
    // Notes on benchmarking:
    // - not far from optimal configuration on nv-tegra2
    // - within 5% of optimal configuration except for backward aligned on IMX
    // - up to 40% from optimal configuration for backward shifted and backward align for tegra2
    //   but still on par with the operating system copy
    {-256, true,  true  }, // forward aligned
    {-256, true,  true  }, // backward aligned
    {-256, false, false }, // forward shifted
    {-256, true,  true  } // backward shifted
  },
  {
    // configuration tuned on tegra2-4.
    // Warning: should not be used on nv-tegra2 !
    // Notes:
    // - prefetch after gives 40% gain on backward copies on tegra2-4,
    //   resulting in better number than the operating system
    //   copy. However, this can lead to a 300% loss on nv-tegra and has
    //   more impact on the cache (fetches futher than what is
    //   copied). Use this configuration with care, in case it improves
    //   reference benchmarks.
    {-256, true,  true  }, // forward aligned
    {96,   false, false }, // backward aligned
    {-256, false, false }, // forward shifted
    {96,   false, false } // backward shifted
  },
  {
    // configuration tuned on imx515
    // Notes:
    // - smaller prefetch distance is sufficient to get good result and might be more stable
    // - refined backward aligned options within 5% of optimal configuration except for
    //   tests were the arrays fit in the cache
    {-160, false, false }, // forward aligned
    {-160, false, false }, // backward aligned
    {-160, false, false }, // forward shifted
    {-160, true,  true  } // backward shifted
  }
};

class StubGenerator: public StubCodeGenerator {

#ifdef PRODUCT
#define inc_counter_np(a,b,c) ((void)0)
#else
#define inc_counter_np(counter, t1, t2) \
  BLOCK_COMMENT("inc_counter " #counter); \
  __ inc_counter(&counter, t1, t2);
#endif

 private:

  address generate_call_stub(address& return_address) {
    StubCodeMark mark(this, "StubRoutines", "call_stub");
    address start = __ pc();


    assert(frame::entry_frame_call_wrapper_offset == 0, "adjust this code");

    __ mov(Rtemp, SP);
    __ push(RegisterSet(FP) | RegisterSet(LR));
    __ fpush_hardfp(FloatRegisterSet(D8, 8));
    __ stmdb(SP, RegisterSet(R0, R2) | RegisterSet(R4, R6) | RegisterSet(R8, R10) | altFP_7_11, writeback);
    __ mov(Rmethod, R3);
    __ ldmia(Rtemp, RegisterSet(R1, R3) | Rthread); // stacked arguments

    // XXX: TODO
    // Would be better with respect to native tools if the following
    // setting of FP was changed to conform to the native ABI, with FP
    // pointing to the saved FP slot (and the corresponding modifications
    // for entry_frame_call_wrapper_offset and frame::real_fp).
    __ mov(FP, SP);

    {
      Label no_parameters, pass_parameters;
      __ cmp(R3, 0);
      __ b(no_parameters, eq);

      __ bind(pass_parameters);
      __ ldr(Rtemp, Address(R2, wordSize, post_indexed)); // Rtemp OK, unused and scratchable
      __ subs(R3, R3, 1);
      __ push(Rtemp);
      __ b(pass_parameters, ne);
      __ bind(no_parameters);
    }

    __ mov(Rsender_sp, SP);
    __ blx(R1);
    return_address = __ pc();

    __ add(SP, FP, wordSize); // Skip link to JavaCallWrapper
    __ pop(RegisterSet(R2, R3));
#ifndef __ABI_HARD__
    __ cmp(R3, T_LONG);
    __ cmp(R3, T_DOUBLE, ne);
    __ str(R0, Address(R2));
    __ str(R1, Address(R2, wordSize), eq);
#else
    Label cont, l_float, l_double;

    __ cmp(R3, T_DOUBLE);
    __ b(l_double, eq);

    __ cmp(R3, T_FLOAT);
    __ b(l_float, eq);

    __ cmp(R3, T_LONG);
    __ str(R0, Address(R2));
    __ str(R1, Address(R2, wordSize), eq);
    __ b(cont);


    __ bind(l_double);
    __ fstd(D0, Address(R2));
    __ b(cont);

    __ bind(l_float);
    __ fsts(S0, Address(R2));

    __ bind(cont);
#endif

    __ pop(RegisterSet(R4, R6) | RegisterSet(R8, R10) | altFP_7_11);
    __ fpop_hardfp(FloatRegisterSet(D8, 8));
    __ pop(RegisterSet(FP) | RegisterSet(PC));

    return start;
  }


  // (in) Rexception_obj: exception oop
  address generate_catch_exception() {
    StubCodeMark mark(this, "StubRoutines", "catch_exception");
    address start = __ pc();

    __ str(Rexception_obj, Address(Rthread, Thread::pending_exception_offset()));
    __ b(StubRoutines::_call_stub_return_address);

    return start;
  }


  // (in) Rexception_pc: return address
  address generate_forward_exception() {
    StubCodeMark mark(this, "StubRoutines", "forward exception");
    address start = __ pc();

    __ mov(c_rarg0, Rthread);
    __ mov(c_rarg1, Rexception_pc);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address,
                         SharedRuntime::exception_handler_for_return_address),
                         c_rarg0, c_rarg1);
    __ ldr(Rexception_obj, Address(Rthread, Thread::pending_exception_offset()));
    const Register Rzero = __ zero_register(Rtemp); // Rtemp OK (cleared by above call)
    __ str(Rzero, Address(Rthread, Thread::pending_exception_offset()));

#ifdef ASSERT
    // make sure exception is set
    { Label L;
      __ cbnz(Rexception_obj, L);
      __ stop("StubRoutines::forward exception: no pending exception (2)");
      __ bind(L);
    }
#endif

    // Verify that there is really a valid exception in RAX.
    __ verify_oop(Rexception_obj);

    __ jump(R0); // handler is returned in R0 by runtime function
    return start;
  }



  // Integer division shared routine
  //   Input:
  //     R0  - dividend
  //     R2  - divisor
  //   Output:
  //     R0  - remainder
  //     R1  - quotient
  //   Destroys:
  //     R2
  //     LR
  address generate_idiv_irem() {
    Label positive_arguments, negative_or_zero, call_slow_path;
    Register dividend  = R0;
    Register divisor   = R2;
    Register remainder = R0;
    Register quotient  = R1;
    Register tmp       = LR;
    assert(dividend == remainder, "must be");

    address start = __ pc();

    // Check for special cases: divisor <= 0 or dividend < 0
    __ cmp(divisor, 0);
    __ orrs(quotient, dividend, divisor, ne);
    __ b(negative_or_zero, le);

    __ bind(positive_arguments);
    // Save return address on stack to free one extra register
    __ push(LR);
    // Approximate the mamximum order of the quotient
    __ clz(tmp, dividend);
    __ clz(quotient, divisor);
    __ subs(tmp, quotient, tmp);
    __ mov(quotient, 0);
    // Jump to the appropriate place in the unrolled loop below
    __ ldr(PC, Address(PC, tmp, lsl, 2), pl);
    // If divisor is greater than dividend, return immediately
    __ pop(PC);

    // Offset table
    Label offset_table[32];
    int i;
    for (i = 0; i <= 31; i++) {
      __ emit_address(offset_table[i]);
    }

    // Unrolled loop of 32 division steps
    for (i = 31; i >= 0; i--) {
      __ bind(offset_table[i]);
      __ cmp(remainder, AsmOperand(divisor, lsl, i));
      __ sub(remainder, remainder, AsmOperand(divisor, lsl, i), hs);
      __ add(quotient, quotient, 1 << i, hs);
    }
    __ pop(PC);

    __ bind(negative_or_zero);
    // Find the combination of argument signs and jump to corresponding handler
    __ andr(quotient, dividend, 0x80000000, ne);
    __ orr(quotient, quotient, AsmOperand(divisor, lsr, 31), ne);
    __ add(PC, PC, AsmOperand(quotient, ror, 26), ne);
    __ str(LR, Address(Rthread, JavaThread::saved_exception_pc_offset()));

    // The leaf runtime function can destroy R0-R3 and R12 registers which are still alive
    RegisterSet saved_registers = RegisterSet(R3) | RegisterSet(R12);
#if R9_IS_SCRATCHED
    // Safer to save R9 here since callers may have been written
    // assuming R9 survives. This is suboptimal but may not be worth
    // revisiting for this slow case.

    // save also R10 for alignment
    saved_registers = saved_registers | RegisterSet(R9, R10);
#endif
    {
      // divisor == 0
      FixedSizeCodeBlock zero_divisor(_masm, 8, true);
      __ push(saved_registers);
      __ mov(R0, Rthread);
      __ mov(R1, LR);
      __ mov(R2, SharedRuntime::IMPLICIT_DIVIDE_BY_ZERO);
      __ b(call_slow_path);
    }

    {
      // divisor > 0 && dividend < 0
      FixedSizeCodeBlock positive_divisor_negative_dividend(_masm, 8, true);
      __ push(LR);
      __ rsb(dividend, dividend, 0);
      __ bl(positive_arguments);
      __ rsb(remainder, remainder, 0);
      __ rsb(quotient, quotient, 0);
      __ pop(PC);
    }

    {
      // divisor < 0 && dividend > 0
      FixedSizeCodeBlock negative_divisor_positive_dividend(_masm, 8, true);
      __ push(LR);
      __ rsb(divisor, divisor, 0);
      __ bl(positive_arguments);
      __ rsb(quotient, quotient, 0);
      __ pop(PC);
    }

    {
      // divisor < 0 && dividend < 0
      FixedSizeCodeBlock negative_divisor_negative_dividend(_masm, 8, true);
      __ push(LR);
      __ rsb(dividend, dividend, 0);
      __ rsb(divisor, divisor, 0);
      __ bl(positive_arguments);
      __ rsb(remainder, remainder, 0);
      __ pop(PC);
    }

    __ bind(call_slow_path);
    __ call(CAST_FROM_FN_PTR(address, SharedRuntime::continuation_for_implicit_exception));
    __ pop(saved_registers);
    __ bx(R0);

    return start;
  }


 // As per atomic.hpp the Atomic read-modify-write operations must be logically implemented as:
 //  <fence>; <op>; <membar StoreLoad|StoreStore>
 // But for load-linked/store-conditional based systems a fence here simply means
 // no load/store can be reordered with respect to the initial load-linked, so we have:
 // <membar storeload|loadload> ; load-linked; <op>; store-conditional; <membar storeload|storestore>
 // There are no memory actions in <op> so nothing further is needed.
 //
 // So we define the following for convenience:
#define MEMBAR_ATOMIC_OP_PRE \
    MacroAssembler::Membar_mask_bits(MacroAssembler::StoreLoad|MacroAssembler::LoadLoad)
#define MEMBAR_ATOMIC_OP_POST \
    MacroAssembler::Membar_mask_bits(MacroAssembler::StoreLoad|MacroAssembler::StoreStore)

  // Note: JDK 9 only supports ARMv7+ so we always have ldrexd available even though the
  // code below allows for it to be otherwise. The else clause indicates an ARMv5 system
  // for which we do not support MP and so membars are not necessary. This ARMv5 code will
  // be removed in the future.

  // Implementation of atomic_add(jint add_value, volatile jint* dest)
  // used by Atomic::add(volatile jint* dest, jint add_value)
  //
  // Arguments :
  //
  //      add_value:      R0
  //      dest:           R1
  //
  // Results:
  //
  //     R0: the new stored in dest
  //
  // Overwrites:
  //
  //     R1, R2, R3
  //
  address generate_atomic_add() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_add");
    Label retry;
    start = __ pc();
    Register addval    = R0;
    Register dest      = R1;
    Register prev      = R2;
    Register ok        = R2;
    Register newval    = R3;

    if (VM_Version::supports_ldrex()) {
      __ membar(MEMBAR_ATOMIC_OP_PRE, prev);
      __ bind(retry);
      __ ldrex(newval, Address(dest));
      __ add(newval, addval, newval);
      __ strex(ok, newval, Address(dest));
      __ cmp(ok, 0);
      __ b(retry, ne);
      __ mov (R0, newval);
      __ membar(MEMBAR_ATOMIC_OP_POST, prev);
    } else {
      __ bind(retry);
      __ ldr (prev, Address(dest));
      __ add(newval, addval, prev);
      __ atomic_cas_bool(prev, newval, dest, 0, noreg/*ignored*/);
      __ b(retry, ne);
      __ mov (R0, newval);
    }
    __ bx(LR);

    return start;
  }

  // Implementation of jint atomic_xchg(jint exchange_value, volatile jint* dest)
  // used by Atomic::add(volatile jint* dest, jint exchange_value)
  //
  // Arguments :
  //
  //      exchange_value: R0
  //      dest:           R1
  //
  // Results:
  //
  //     R0: the value previously stored in dest
  //
  // Overwrites:
  //
  //     R1, R2, R3
  //
  address generate_atomic_xchg() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_xchg");
    start = __ pc();
    Register newval    = R0;
    Register dest      = R1;
    Register prev      = R2;

    Label retry;

    if (VM_Version::supports_ldrex()) {
      Register ok=R3;
      __ membar(MEMBAR_ATOMIC_OP_PRE, prev);
      __ bind(retry);
      __ ldrex(prev, Address(dest));
      __ strex(ok, newval, Address(dest));
      __ cmp(ok, 0);
      __ b(retry, ne);
      __ mov (R0, prev);
      __ membar(MEMBAR_ATOMIC_OP_POST, prev);
    } else {
      __ bind(retry);
      __ ldr (prev, Address(dest));
      __ atomic_cas_bool(prev, newval, dest, 0, noreg/*ignored*/);
      __ b(retry, ne);
      __ mov (R0, prev);
    }
    __ bx(LR);

    return start;
  }

  // Implementation of jint atomic_cmpxchg(jint exchange_value, volatile jint *dest, jint compare_value)
  // used by Atomic::cmpxchg(volatile jint *dest, jint compare_value, jint exchange_value)
  //
  // Arguments :
  //
  //      compare_value:  R0
  //      exchange_value: R1
  //      dest:           R2
  //
  // Results:
  //
  //     R0: the value previously stored in dest
  //
  // Overwrites:
  //
  //     R0, R1, R2, R3, Rtemp
  //
  address generate_atomic_cmpxchg() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_cmpxchg");
    start = __ pc();
    Register cmp       = R0;
    Register newval    = R1;
    Register dest      = R2;
    Register temp1     = R3;
    Register temp2     = Rtemp; // Rtemp free (native ABI)

    __ membar(MEMBAR_ATOMIC_OP_PRE, temp1);

    // atomic_cas returns previous value in R0
    __ atomic_cas(temp1, temp2, cmp, newval, dest, 0);

    __ membar(MEMBAR_ATOMIC_OP_POST, temp1);

    __ bx(LR);

    return start;
  }

  // Support for jlong Atomic::cmpxchg(jlong exchange_value, volatile jlong *dest, jlong compare_value)
  // reordered before by a wrapper to (jlong compare_value, jlong exchange_value, volatile jlong *dest)
  //
  // Arguments :
  //
  //      compare_value:  R1 (High), R0 (Low)
  //      exchange_value: R3 (High), R2 (Low)
  //      dest:           SP+0
  //
  // Results:
  //
  //     R0:R1: the value previously stored in dest
  //
  // Overwrites:
  //
  address generate_atomic_cmpxchg_long() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_cmpxchg_long");
    start = __ pc();
    Register cmp_lo      = R0;
    Register cmp_hi      = R1;
    Register newval_lo   = R2;
    Register newval_hi   = R3;
    Register addr        = Rtemp;  /* After load from stack */
    Register temp_lo     = R4;
    Register temp_hi     = R5;
    Register temp_result = R8;
    assert_different_registers(cmp_lo, newval_lo, temp_lo, addr, temp_result, R7);
    assert_different_registers(cmp_hi, newval_hi, temp_hi, addr, temp_result, R7);

    __ membar(MEMBAR_ATOMIC_OP_PRE, Rtemp); // Rtemp free (native ABI)

    // Stack is unaligned, maintain double word alignment by pushing
    // odd number of regs.
    __ push(RegisterSet(temp_result) | RegisterSet(temp_lo, temp_hi));
    __ ldr(addr, Address(SP, 12));

    // atomic_cas64 returns previous value in temp_lo, temp_hi
    __ atomic_cas64(temp_lo, temp_hi, temp_result, cmp_lo, cmp_hi,
                    newval_lo, newval_hi, addr, 0);
    __ mov(R0, temp_lo);
    __ mov(R1, temp_hi);

    __ pop(RegisterSet(temp_result) | RegisterSet(temp_lo, temp_hi));

    __ membar(MEMBAR_ATOMIC_OP_POST, Rtemp); // Rtemp free (native ABI)
    __ bx(LR);

    return start;
  }

  address generate_atomic_load_long() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_load_long");
    start = __ pc();
    Register result_lo = R0;
    Register result_hi = R1;
    Register src       = R0;

    if (!os::is_MP()) {
      __ ldmia(src, RegisterSet(result_lo, result_hi));
      __ bx(LR);
    } else if (VM_Version::supports_ldrexd()) {
      __ ldrexd(result_lo, Address(src));
      __ clrex(); // FIXME: safe to remove?
      __ bx(LR);
    } else {
      __ stop("Atomic load(jlong) unsupported on this platform");
      __ bx(LR);
    }

    return start;
  }

  address generate_atomic_store_long() {
    address start;

    StubCodeMark mark(this, "StubRoutines", "atomic_store_long");
    start = __ pc();
    Register newval_lo = R0;
    Register newval_hi = R1;
    Register dest      = R2;
    Register scratch_lo    = R2;
    Register scratch_hi    = R3;  /* After load from stack */
    Register result    = R3;

    if (!os::is_MP()) {
      __ stmia(dest, RegisterSet(newval_lo, newval_hi));
      __ bx(LR);
    } else if (VM_Version::supports_ldrexd()) {
      __ mov(Rtemp, dest);  // get dest to Rtemp
      Label retry;
      __ bind(retry);
      __ ldrexd(scratch_lo, Address(Rtemp));
      __ strexd(result, R0, Address(Rtemp));
      __ rsbs(result, result, 1);
      __ b(retry, eq);
      __ bx(LR);
    } else {
      __ stop("Atomic store(jlong) unsupported on this platform");
      __ bx(LR);
    }

    return start;
  }



#ifdef COMPILER2
  // Support for uint StubRoutine::Arm::partial_subtype_check( Klass sub, Klass super );
  // Arguments :
  //
  //      ret  : R0, returned
  //      icc/xcc: set as R0 (depending on wordSize)
  //      sub  : R1, argument, not changed
  //      super: R2, argument, not changed
  //      raddr: LR, blown by call
  address generate_partial_subtype_check() {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", "partial_subtype_check");
    address start = __ pc();

    // based on SPARC check_klass_subtype_[fast|slow]_path (without CompressedOops)

    // R0 used as tmp_reg (in addition to return reg)
    Register sub_klass = R1;
    Register super_klass = R2;
    Register tmp_reg2 = R3;
    Register tmp_reg3 = R4;
#define saved_set tmp_reg2, tmp_reg3

    Label L_loop, L_fail;

    int sc_offset = in_bytes(Klass::secondary_super_cache_offset());

    // fast check should be redundant

    // slow check
    {
      __ raw_push(saved_set);

      // a couple of useful fields in sub_klass:
      int ss_offset = in_bytes(Klass::secondary_supers_offset());

      // Do a linear scan of the secondary super-klass chain.
      // This code is rarely used, so simplicity is a virtue here.

      inc_counter_np(SharedRuntime::_partial_subtype_ctr, tmp_reg2, tmp_reg3);

      Register scan_temp = tmp_reg2;
      Register count_temp = tmp_reg3;

      // We will consult the secondary-super array.
      __ ldr(scan_temp, Address(sub_klass, ss_offset));

      Register search_key = super_klass;

      // Load the array length.
      __ ldr_s32(count_temp, Address(scan_temp, Array<Klass*>::length_offset_in_bytes()));
      __ add(scan_temp, scan_temp, Array<Klass*>::base_offset_in_bytes());

      __ add(count_temp, count_temp, 1);

      // Top of search loop
      __ bind(L_loop);
      // Notes:
      //  scan_temp starts at the array elements
      //  count_temp is 1+size
      __ subs(count_temp, count_temp, 1);
      __ b(L_fail, eq); // not found in the array

      // Load next super to check
      // In the array of super classes elements are pointer sized.
      int element_size = wordSize;
      __ ldr(R0, Address(scan_temp, element_size, post_indexed));

      // Look for Rsuper_klass on Rsub_klass's secondary super-class-overflow list
      __ subs(R0, R0, search_key); // set R0 to 0 on success (and flags to eq)

      // A miss means we are NOT a subtype and need to keep looping
      __ b(L_loop, ne);

      // Falling out the bottom means we found a hit; we ARE a subtype

      // Success.  Cache the super we found and proceed in triumph.
      __ str(super_klass, Address(sub_klass, sc_offset));

      // Return success
      // R0 is already 0 and flags are already set to eq
      __ raw_pop(saved_set);
      __ ret();

      // Return failure
      __ bind(L_fail);
      __ movs(R0, 1); // sets the flags
      __ raw_pop(saved_set);
      __ ret();
    }
    return start;
  }
#undef saved_set
#endif // COMPILER2


  //----------------------------------------------------------------------------------------------------
  // Non-destructive plausibility checks for oops

  address generate_verify_oop() {
    StubCodeMark mark(this, "StubRoutines", "verify_oop");
    address start = __ pc();

    // Incoming arguments:
    //
    // R0: error message (char* )
    // R1: address of register save area
    // R2: oop to verify
    //
    // All registers are saved before calling this stub. However, condition flags should be saved here.

    const Register oop   = R2;
    const Register klass = R3;
    const Register tmp1  = R6;
    const Register tmp2  = R8;

    const Register flags     = Rtmp_save0; // R4/R19
    const Register ret_addr  = Rtmp_save1; // R5/R20
    assert_different_registers(oop, klass, tmp1, tmp2, flags, ret_addr, R7);

    Label exit, error;
    InlinedAddress verify_oop_count((address) StubRoutines::verify_oop_count_addr());

    __ mrs(Assembler::CPSR, flags);

    __ ldr_literal(tmp1, verify_oop_count);
    __ ldr_s32(tmp2, Address(tmp1));
    __ add(tmp2, tmp2, 1);
    __ str_32(tmp2, Address(tmp1));

    // make sure object is 'reasonable'
    __ cbz(oop, exit);                           // if obj is NULL it is ok

    // Check if the oop is in the right area of memory
    // Note: oop_mask and oop_bits must be updated if the code is saved/reused
    const address oop_mask = (address) Universe::verify_oop_mask();
    const address oop_bits = (address) Universe::verify_oop_bits();
    __ mov_address(tmp1, oop_mask);
    __ andr(tmp2, oop, tmp1);
    __ mov_address(tmp1, oop_bits);
    __ cmp(tmp2, tmp1);
    __ b(error, ne);

    // make sure klass is 'reasonable'
    __ load_klass(klass, oop);                   // get klass
    __ cbz(klass, error);                        // if klass is NULL it is broken

    // return if everything seems ok
    __ bind(exit);

    __ msr(Assembler::CPSR_f, flags);

    __ ret();

    // handle errors
    __ bind(error);

    __ mov(ret_addr, LR);                      // save return address

    // R0: error message
    // R1: register save area
    __ call(CAST_FROM_FN_PTR(address, MacroAssembler::debug));

    __ mov(LR, ret_addr);
    __ b(exit);

    __ bind_literal(verify_oop_count);

    return start;
  }

  //----------------------------------------------------------------------------------------------------
  // Array copy stubs

  //
  //  Generate overlap test for array copy stubs
  //
  //  Input:
  //    R0    -  array1
  //    R1    -  array2
  //    R2    -  element count, 32-bit int
  //
  //  input registers are preserved
  //
  void array_overlap_test(address no_overlap_target, int log2_elem_size, Register tmp1, Register tmp2) {
    assert(no_overlap_target != NULL, "must be generated");
    array_overlap_test(no_overlap_target, NULL, log2_elem_size, tmp1, tmp2);
  }
  void array_overlap_test(Label& L_no_overlap, int log2_elem_size, Register tmp1, Register tmp2) {
    array_overlap_test(NULL, &L_no_overlap, log2_elem_size, tmp1, tmp2);
  }
  void array_overlap_test(address no_overlap_target, Label* NOLp, int log2_elem_size, Register tmp1, Register tmp2) {
    const Register from       = R0;
    const Register to         = R1;
    const Register count      = R2;
    const Register to_from    = tmp1; // to - from
    const Register byte_count = (log2_elem_size == 0) ? count : tmp2; // count << log2_elem_size
    assert_different_registers(from, to, count, tmp1, tmp2);

    // no_overlap version works if 'to' lower (unsigned) than 'from'
    // and or 'to' more than (count*size) from 'from'

    BLOCK_COMMENT("Array Overlap Test:");
    __ subs(to_from, to, from);
    if (log2_elem_size != 0) {
      __ mov(byte_count, AsmOperand(count, lsl, log2_elem_size));
    }
    if (NOLp == NULL)
      __ b(no_overlap_target,lo);
    else
      __ b((*NOLp), lo);
    __ cmp(to_from, byte_count);
    if (NOLp == NULL)
      __ b(no_overlap_target, ge);
    else
      __ b((*NOLp), ge);
  }


  //   probably we should choose between "prefetch-store before or after store", not "before or after load".
  void prefetch(Register from, Register to, int offset, int to_delta = 0) {
    __ prefetch_read(Address(from, offset));
  }

  // Generate the inner loop for forward aligned array copy
  //
  // Arguments
  //      from:      src address, 64 bits  aligned
  //      to:        dst address, wordSize aligned
  //      count:     number of elements (32-bit int)
  //      bytes_per_count: number of bytes for each unit of 'count'
  //
  // Return the minimum initial value for count
  //
  // Notes:
  // - 'from' aligned on 64-bit (recommended for 32-bit ARM in case this speeds up LDMIA)
  // - 'to' aligned on wordSize
  // - 'count' must be greater or equal than the returned value
  //
  // Increases 'from' and 'to' by count*bytes_per_count.
  //
  // Scratches 'count', R3.
  // R4-R10 are preserved (saved/restored).
  //
  int generate_forward_aligned_copy_loop(Register from, Register to, Register count, int bytes_per_count, bool unsafe_copy = false) {
    assert (from == R0 && to == R1 && count == R2, "adjust the implementation below");

    const int bytes_per_loop = 8*wordSize; // 8 registers are read and written on every loop iteration
    arraycopy_loop_config *config=&arraycopy_configurations[ArmCopyPlatform].forward_aligned;
    int pld_offset = config->pld_distance;
    const int count_per_loop = bytes_per_loop / bytes_per_count;

    bool split_read= config->split_ldm;
    bool split_write= config->split_stm;

    // XXX optim: use VLDM/VSTM when available (Neon) with PLD
    //  NEONCopyPLD
    //      PLD [r1, #0xC0]
    //      VLDM r1!,{d0-d7}
    //      VSTM r0!,{d0-d7}
    //      SUBS r2,r2,#0x40
    //      BGE NEONCopyPLD

    __ push(RegisterSet(R4,R10));

    const bool prefetch_before = pld_offset < 0;
    const bool prefetch_after = pld_offset > 0;

    Label L_skip_pld;

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, unsafe_copy, true);
      // predecrease to exit when there is less than count_per_loop
      __ sub_32(count, count, count_per_loop);

      if (pld_offset != 0) {
        pld_offset = (pld_offset < 0) ? -pld_offset : pld_offset;

        prefetch(from, to, 0);

        if (prefetch_before) {
          // If prefetch is done ahead, final PLDs that overflow the
          // copied area can be easily avoided. 'count' is predecreased
          // by the prefetch distance to optimize the inner loop and the
          // outer loop skips the PLD.
          __ subs_32(count, count, (bytes_per_loop+pld_offset)/bytes_per_count);

          // skip prefetch for small copies
          __ b(L_skip_pld, lt);
        }

        int offset = ArmCopyCacheLineSize;
        while (offset <= pld_offset) {
          prefetch(from, to, offset);
          offset += ArmCopyCacheLineSize;
        };
      }

      {
        // 32-bit ARM note: we have tried implementing loop unrolling to skip one
        // PLD with 64 bytes cache line but the gain was not significant.

        Label L_copy_loop;
        __ align(OptoLoopAlignment);
        __ BIND(L_copy_loop);

        if (prefetch_before) {
          prefetch(from, to, bytes_per_loop + pld_offset);
          __ BIND(L_skip_pld);
        }

        if (split_read) {
          // Split the register set in two sets so that there is less
          // latency between LDM and STM (R3-R6 available while R7-R10
          // still loading) and less register locking issue when iterating
          // on the first LDM.
          __ ldmia(from, RegisterSet(R3, R6), writeback);
          __ ldmia(from, RegisterSet(R7, R10), writeback);
        } else {
          __ ldmia(from, RegisterSet(R3, R10), writeback);
        }

        __ subs_32(count, count, count_per_loop);

        if (prefetch_after) {
          prefetch(from, to, pld_offset, bytes_per_loop);
        }

        if (split_write) {
          __ stmia(to, RegisterSet(R3, R6), writeback);
          __ stmia(to, RegisterSet(R7, R10), writeback);
        } else {
          __ stmia(to, RegisterSet(R3, R10), writeback);
        }

        __ b(L_copy_loop, ge);

        if (prefetch_before) {
          // the inner loop may end earlier, allowing to skip PLD for the last iterations
          __ cmn_32(count, (bytes_per_loop + pld_offset)/bytes_per_count);
          __ b(L_skip_pld, ge);
        }
      }
      BLOCK_COMMENT("Remaining bytes:");
      // still 0..bytes_per_loop-1 aligned bytes to copy, count already decreased by (at least) bytes_per_loop bytes

      // __ add(count, count, ...); // addition useless for the bit tests
      assert (pld_offset % bytes_per_loop == 0, "decreasing count by pld_offset before loop must not change tested bits");

      __ tst(count, 16 / bytes_per_count);
      __ ldmia(from, RegisterSet(R3, R6), writeback, ne); // copy 16 bytes
      __ stmia(to, RegisterSet(R3, R6), writeback, ne);

      __ tst(count, 8 / bytes_per_count);
      __ ldmia(from, RegisterSet(R3, R4), writeback, ne); // copy 8 bytes
      __ stmia(to, RegisterSet(R3, R4), writeback, ne);

      if (bytes_per_count <= 4) {
        __ tst(count, 4 / bytes_per_count);
        __ ldr(R3, Address(from, 4, post_indexed), ne); // copy 4 bytes
        __ str(R3, Address(to, 4, post_indexed), ne);
      }

      if (bytes_per_count <= 2) {
        __ tst(count, 2 / bytes_per_count);
        __ ldrh(R3, Address(from, 2, post_indexed), ne); // copy 2 bytes
        __ strh(R3, Address(to, 2, post_indexed), ne);
      }

      if (bytes_per_count == 1) {
        __ tst(count, 1);
        __ ldrb(R3, Address(from, 1, post_indexed), ne);
        __ strb(R3, Address(to, 1, post_indexed), ne);
      }
    }

    __ pop(RegisterSet(R4,R10));

    return count_per_loop;
  }


  // Generate the inner loop for backward aligned array copy
  //
  // Arguments
  //      end_from:      src end address, 64 bits  aligned
  //      end_to:        dst end address, wordSize aligned
  //      count:         number of elements (32-bit int)
  //      bytes_per_count: number of bytes for each unit of 'count'
  //
  // Return the minimum initial value for count
  //
  // Notes:
  // - 'end_from' aligned on 64-bit (recommended for 32-bit ARM in case this speeds up LDMIA)
  // - 'end_to' aligned on wordSize
  // - 'count' must be greater or equal than the returned value
  //
  // Decreases 'end_from' and 'end_to' by count*bytes_per_count.
  //
  // Scratches 'count', R3.
  // ARM R4-R10 are preserved (saved/restored).
  //
  int generate_backward_aligned_copy_loop(Register end_from, Register end_to, Register count, int bytes_per_count, bool unsafe_copy = false) {
    assert (end_from == R0 && end_to == R1 && count == R2, "adjust the implementation below");

    const int bytes_per_loop = 8*wordSize; // 8 registers are read and written on every loop iteration
    const int count_per_loop = bytes_per_loop / bytes_per_count;

    arraycopy_loop_config *config=&arraycopy_configurations[ArmCopyPlatform].backward_aligned;
    int pld_offset = config->pld_distance;

    bool split_read= config->split_ldm;
    bool split_write= config->split_stm;

    // See the forward copy variant for additional comments.

    __ push(RegisterSet(R4,R10));

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, unsafe_copy, true);
      __ sub_32(count, count, count_per_loop);

      const bool prefetch_before = pld_offset < 0;
      const bool prefetch_after = pld_offset > 0;

      Label L_skip_pld;

      if (pld_offset != 0) {
        pld_offset = (pld_offset < 0) ? -pld_offset : pld_offset;

        prefetch(end_from, end_to, -wordSize);

        if (prefetch_before) {
          __ subs_32(count, count, (bytes_per_loop + pld_offset) / bytes_per_count);
          __ b(L_skip_pld, lt);
        }

        int offset = ArmCopyCacheLineSize;
        while (offset <= pld_offset) {
          prefetch(end_from, end_to, -(wordSize + offset));
          offset += ArmCopyCacheLineSize;
        };
      }

      {
        // 32-bit ARM note: we have tried implementing loop unrolling to skip one
        // PLD with 64 bytes cache line but the gain was not significant.

        Label L_copy_loop;
        __ align(OptoLoopAlignment);
        __ BIND(L_copy_loop);

        if (prefetch_before) {
          prefetch(end_from, end_to, -(wordSize + bytes_per_loop + pld_offset));
          __ BIND(L_skip_pld);
        }

        if (split_read) {
          __ ldmdb(end_from, RegisterSet(R7, R10), writeback);
          __ ldmdb(end_from, RegisterSet(R3, R6), writeback);
        } else {
          __ ldmdb(end_from, RegisterSet(R3, R10), writeback);
        }

        __ subs_32(count, count, count_per_loop);

        if (prefetch_after) {
          prefetch(end_from, end_to, -(wordSize + pld_offset), -bytes_per_loop);
        }

        if (split_write) {
          __ stmdb(end_to, RegisterSet(R7, R10), writeback);
          __ stmdb(end_to, RegisterSet(R3, R6), writeback);
        } else {
          __ stmdb(end_to, RegisterSet(R3, R10), writeback);
        }

        __ b(L_copy_loop, ge);

        if (prefetch_before) {
          __ cmn_32(count, (bytes_per_loop + pld_offset)/bytes_per_count);
          __ b(L_skip_pld, ge);
        }
      }
      BLOCK_COMMENT("Remaining bytes:");
      // still 0..bytes_per_loop-1 aligned bytes to copy, count already decreased by (at least) bytes_per_loop bytes

      // __ add(count, count, ...); // addition useless for the bit tests
      assert (pld_offset % bytes_per_loop == 0, "decreasing count by pld_offset before loop must not change tested bits");

      __ tst(count, 16 / bytes_per_count);
      __ ldmdb(end_from, RegisterSet(R3, R6), writeback, ne); // copy 16 bytes
      __ stmdb(end_to, RegisterSet(R3, R6), writeback, ne);

      __ tst(count, 8 / bytes_per_count);
      __ ldmdb(end_from, RegisterSet(R3, R4), writeback, ne); // copy 8 bytes
      __ stmdb(end_to, RegisterSet(R3, R4), writeback, ne);

      if (bytes_per_count <= 4) {
        __ tst(count, 4 / bytes_per_count);
        __ ldr(R3, Address(end_from, -4, pre_indexed), ne); // copy 4 bytes
        __ str(R3, Address(end_to, -4, pre_indexed), ne);
      }

      if (bytes_per_count <= 2) {
        __ tst(count, 2 / bytes_per_count);
        __ ldrh(R3, Address(end_from, -2, pre_indexed), ne); // copy 2 bytes
        __ strh(R3, Address(end_to, -2, pre_indexed), ne);
      }

      if (bytes_per_count == 1) {
        __ tst(count, 1);
        __ ldrb(R3, Address(end_from, -1, pre_indexed), ne);
        __ strb(R3, Address(end_to, -1, pre_indexed), ne);
      }
    }
    __ pop(RegisterSet(R4,R10));

    return count_per_loop;
  }


  // Generate the inner loop for shifted forward array copy (unaligned copy).
  // It can be used when bytes_per_count < wordSize, i.e. byte/short copy
  //
  // Arguments
  //      from:      start src address, 64 bits aligned
  //      to:        start dst address, (now) wordSize aligned
  //      count:     number of elements (32-bit int)
  //      bytes_per_count: number of bytes for each unit of 'count'
  //      lsr_shift: shift applied to 'old' value to skipped already written bytes
  //      lsl_shift: shift applied to 'new' value to set the high bytes of the next write
  //
  // Return the minimum initial value for count
  //
  // Notes:
  // - 'from' aligned on 64-bit (recommended for 32-bit ARM in case this speeds up LDMIA)
  // - 'to' aligned on wordSize
  // - 'count' must be greater or equal than the returned value
  // - 'lsr_shift' + 'lsl_shift' = BitsPerWord
  // - 'bytes_per_count' is 1 or 2
  //
  // Increases 'to' by count*bytes_per_count.
  //
  // Scratches 'from' and 'count', R3-R10, R12
  //
  // On entry:
  // - R12 is preloaded with the first 'BitsPerWord' bits read just before 'from'
  // - (R12 >> lsr_shift) is the part not yet written (just before 'to')
  // --> (*to) = (R12 >> lsr_shift) | (*from) << lsl_shift); ...
  //
  // This implementation may read more bytes than required.
  // Actually, it always reads exactly all data from the copied region with upper bound aligned up by wordSize,
  // so excessive read do not cross a word bound and is thus harmless.
  //
  int generate_forward_shifted_copy_loop(Register from, Register to, Register count, int bytes_per_count, int lsr_shift, int lsl_shift) {
    assert (from == R0 && to == R1 && count == R2, "adjust the implementation below");

    const int bytes_per_loop = 8*wordSize; // 8 registers are read and written on every loop iter
    const int count_per_loop = bytes_per_loop / bytes_per_count;

    arraycopy_loop_config *config=&arraycopy_configurations[ArmCopyPlatform].forward_shifted;
    int pld_offset = config->pld_distance;

    bool split_read= config->split_ldm;
    bool split_write= config->split_stm;

    const bool prefetch_before = pld_offset < 0;
    const bool prefetch_after = pld_offset > 0;
    Label L_skip_pld, L_last_read, L_done;
    if (pld_offset != 0) {

      pld_offset = (pld_offset < 0) ? -pld_offset : pld_offset;

      prefetch(from, to, 0);

      if (prefetch_before) {
        __ cmp_32(count, count_per_loop);
        __ b(L_last_read, lt);
        // skip prefetch for small copies
        // warning: count is predecreased by the prefetch distance to optimize the inner loop
        __ subs_32(count, count, ((bytes_per_loop + pld_offset) / bytes_per_count) + count_per_loop);
        __ b(L_skip_pld, lt);
      }

      int offset = ArmCopyCacheLineSize;
      while (offset <= pld_offset) {
        prefetch(from, to, offset);
        offset += ArmCopyCacheLineSize;
      };
    }

    Label L_shifted_loop;

    __ align(OptoLoopAlignment);
    __ BIND(L_shifted_loop);

    if (prefetch_before) {
      // do it early if there might be register locking issues
      prefetch(from, to, bytes_per_loop + pld_offset);
      __ BIND(L_skip_pld);
    } else {
      __ cmp_32(count, count_per_loop);
      __ b(L_last_read, lt);
    }

    // read 32 bytes
    if (split_read) {
      // if write is not split, use less registers in first set to reduce locking
      RegisterSet set1 = split_write ? RegisterSet(R4, R7) : RegisterSet(R4, R5);
      RegisterSet set2 = (split_write ? RegisterSet(R8, R10) : RegisterSet(R6, R10)) | R12;
      __ ldmia(from, set1, writeback);
      __ mov(R3, AsmOperand(R12, lsr, lsr_shift)); // part of R12 not yet written
      __ ldmia(from, set2, writeback);
      __ subs(count, count, count_per_loop); // XXX: should it be before the 2nd LDM ? (latency vs locking)
    } else {
      __ mov(R3, AsmOperand(R12, lsr, lsr_shift)); // part of R12 not yet written
      __ ldmia(from, RegisterSet(R4, R10) | R12, writeback); // Note: small latency on R4
      __ subs(count, count, count_per_loop);
    }

    if (prefetch_after) {
      // do it after the 1st ldm/ldp anyway  (no locking issues with early STM/STP)
      prefetch(from, to, pld_offset, bytes_per_loop);
    }

    // prepare (shift) the values in R3..R10
    __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift)); // merged below low bytes of next val
    __ logical_shift_right(R4, R4, lsr_shift); // unused part of next val
    __ orr(R4, R4, AsmOperand(R5, lsl, lsl_shift)); // ...
    __ logical_shift_right(R5, R5, lsr_shift);
    __ orr(R5, R5, AsmOperand(R6, lsl, lsl_shift));
    __ logical_shift_right(R6, R6, lsr_shift);
    __ orr(R6, R6, AsmOperand(R7, lsl, lsl_shift));
    if (split_write) {
      // write the first half as soon as possible to reduce stm locking
      __ stmia(to, RegisterSet(R3, R6), writeback, prefetch_before ? gt : ge);
    }
    __ logical_shift_right(R7, R7, lsr_shift);
    __ orr(R7, R7, AsmOperand(R8, lsl, lsl_shift));
    __ logical_shift_right(R8, R8, lsr_shift);
    __ orr(R8, R8, AsmOperand(R9, lsl, lsl_shift));
    __ logical_shift_right(R9, R9, lsr_shift);
    __ orr(R9, R9, AsmOperand(R10, lsl, lsl_shift));
    __ logical_shift_right(R10, R10, lsr_shift);
    __ orr(R10, R10, AsmOperand(R12, lsl, lsl_shift));

    if (split_write) {
      __ stmia(to, RegisterSet(R7, R10), writeback, prefetch_before ? gt : ge);
    } else {
      __ stmia(to, RegisterSet(R3, R10), writeback, prefetch_before ? gt : ge);
    }
    __ b(L_shifted_loop, gt); // no need to loop if 0 (when count need not be precise modulo bytes_per_loop)

    if (prefetch_before) {
      // the first loop may end earlier, allowing to skip pld at the end
      __ cmn_32(count, (bytes_per_loop + pld_offset)/bytes_per_count);
      __ stmia(to, RegisterSet(R3, R10), writeback); // stmia was skipped
      __ b(L_skip_pld, ge);
      __ adds_32(count, count, ((bytes_per_loop + pld_offset) / bytes_per_count) + count_per_loop);
    }

    __ BIND(L_last_read);
    __ b(L_done, eq);

    switch (bytes_per_count) {
    case 2:
      __ mov(R3, AsmOperand(R12, lsr, lsr_shift));
      __ tst(count, 8);
      __ ldmia(from, RegisterSet(R4, R7), writeback, ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne); // merged below low bytes of next val
      __ mov(R4, AsmOperand(R4, lsr, lsr_shift), ne); // unused part of next val
      __ orr(R4, R4, AsmOperand(R5, lsl, lsl_shift), ne); // ...
      __ mov(R5, AsmOperand(R5, lsr, lsr_shift), ne);
      __ orr(R5, R5, AsmOperand(R6, lsl, lsl_shift), ne);
      __ mov(R6, AsmOperand(R6, lsr, lsr_shift), ne);
      __ orr(R6, R6, AsmOperand(R7, lsl, lsl_shift), ne);
      __ stmia(to, RegisterSet(R3, R6), writeback, ne);
      __ mov(R3, AsmOperand(R7, lsr, lsr_shift), ne);

      __ tst(count, 4);
      __ ldmia(from, RegisterSet(R4, R5), writeback, ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne); // merged below low bytes of next val
      __ mov(R4, AsmOperand(R4, lsr, lsr_shift), ne); // unused part of next val
      __ orr(R4, R4, AsmOperand(R5, lsl, lsl_shift), ne); // ...
      __ stmia(to, RegisterSet(R3, R4), writeback, ne);
      __ mov(R3, AsmOperand(R5, lsr, lsr_shift), ne);

      __ tst(count, 2);
      __ ldr(R4, Address(from, 4, post_indexed), ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne);
      __ str(R3, Address(to, 4, post_indexed), ne);
      __ mov(R3, AsmOperand(R4, lsr, lsr_shift), ne);

      __ tst(count, 1);
      __ strh(R3, Address(to, 2, post_indexed), ne); // one last short
      break;

    case 1:
      __ mov(R3, AsmOperand(R12, lsr, lsr_shift));
      __ tst(count, 16);
      __ ldmia(from, RegisterSet(R4, R7), writeback, ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne); // merged below low bytes of next val
      __ mov(R4, AsmOperand(R4, lsr, lsr_shift), ne); // unused part of next val
      __ orr(R4, R4, AsmOperand(R5, lsl, lsl_shift), ne); // ...
      __ mov(R5, AsmOperand(R5, lsr, lsr_shift), ne);
      __ orr(R5, R5, AsmOperand(R6, lsl, lsl_shift), ne);
      __ mov(R6, AsmOperand(R6, lsr, lsr_shift), ne);
      __ orr(R6, R6, AsmOperand(R7, lsl, lsl_shift), ne);
      __ stmia(to, RegisterSet(R3, R6), writeback, ne);
      __ mov(R3, AsmOperand(R7, lsr, lsr_shift), ne);

      __ tst(count, 8);
      __ ldmia(from, RegisterSet(R4, R5), writeback, ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne); // merged below low bytes of next val
      __ mov(R4, AsmOperand(R4, lsr, lsr_shift), ne); // unused part of next val
      __ orr(R4, R4, AsmOperand(R5, lsl, lsl_shift), ne); // ...
      __ stmia(to, RegisterSet(R3, R4), writeback, ne);
      __ mov(R3, AsmOperand(R5, lsr, lsr_shift), ne);

      __ tst(count, 4);
      __ ldr(R4, Address(from, 4, post_indexed), ne);
      __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ne);
      __ str(R3, Address(to, 4, post_indexed), ne);
      __ mov(R3, AsmOperand(R4, lsr, lsr_shift), ne);

      __ andr(count, count, 3);
      __ cmp(count, 2);

      // Note: R3 might contain enough bytes ready to write (3 needed at most),
      // thus load on lsl_shift==24 is not needed (in fact forces reading
      // beyond source buffer end boundary)
      if (lsl_shift == 8) {
        __ ldr(R4, Address(from, 4, post_indexed), ge);
        __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), ge);
      } else if (lsl_shift == 16) {
        __ ldr(R4, Address(from, 4, post_indexed), gt);
        __ orr(R3, R3, AsmOperand(R4, lsl, lsl_shift), gt);
      }

      __ strh(R3, Address(to, 2, post_indexed), ge); // two last bytes
      __ mov(R3, AsmOperand(R3, lsr, 16), gt);

      __ tst(count, 1);
      __ strb(R3, Address(to, 1, post_indexed), ne); // one last byte
      break;
    }

    __ BIND(L_done);
    return 0; // no minimum
  }

  // Generate the inner loop for shifted backward array copy (unaligned copy).
  // It can be used when bytes_per_count < wordSize, i.e. byte/short copy
  //
  // Arguments
  //      end_from:  end src address, 64 bits aligned
  //      end_to:    end dst address, (now) wordSize aligned
  //      count:     number of elements (32-bit int)
  //      bytes_per_count: number of bytes for each unit of 'count'
  //      lsl_shift: shift applied to 'old' value to skipped already written bytes
  //      lsr_shift: shift applied to 'new' value to set the low bytes of the next write
  //
  // Return the minimum initial value for count
  //
  // Notes:
  // - 'end_from' aligned on 64-bit (recommended for 32-bit ARM in case this speeds up LDMIA)
  // - 'end_to' aligned on wordSize
  // - 'count' must be greater or equal than the returned value
  // - 'lsr_shift' + 'lsl_shift' = 'BitsPerWord'
  // - 'bytes_per_count' is 1 or 2 on 32-bit ARM
  //
  // Decreases 'end_to' by count*bytes_per_count.
  //
  // Scratches 'end_from', 'count', R3-R10, R12
  //
  // On entry:
  // - R3 is preloaded with the first 'BitsPerWord' bits read just after 'from'
  // - (R3 << lsl_shift) is the part not yet written
  // --> (*--to) = (R3 << lsl_shift) | (*--from) >> lsr_shift); ...
  //
  // This implementation may read more bytes than required.
  // Actually, it always reads exactly all data from the copied region with beginning aligned down by wordSize,
  // so excessive read do not cross a word bound and is thus harmless.
  //
  int generate_backward_shifted_copy_loop(Register end_from, Register end_to, Register count, int bytes_per_count, int lsr_shift, int lsl_shift) {
    assert (end_from == R0 && end_to == R1 && count == R2, "adjust the implementation below");

    const int bytes_per_loop = 8*wordSize; // 8 registers are read and written on every loop iter
    const int count_per_loop = bytes_per_loop / bytes_per_count;

    arraycopy_loop_config *config=&arraycopy_configurations[ArmCopyPlatform].backward_shifted;
    int pld_offset = config->pld_distance;

    bool split_read= config->split_ldm;
    bool split_write= config->split_stm;


    const bool prefetch_before = pld_offset < 0;
    const bool prefetch_after = pld_offset > 0;

    Label L_skip_pld, L_done, L_last_read;
    if (pld_offset != 0) {

      pld_offset = (pld_offset < 0) ? -pld_offset : pld_offset;

      prefetch(end_from, end_to, -wordSize);

      if (prefetch_before) {
        __ cmp_32(count, count_per_loop);
        __ b(L_last_read, lt);

        // skip prefetch for small copies
        // warning: count is predecreased by the prefetch distance to optimize the inner loop
        __ subs_32(count, count, ((bytes_per_loop + pld_offset)/bytes_per_count) + count_per_loop);
        __ b(L_skip_pld, lt);
      }

      int offset = ArmCopyCacheLineSize;
      while (offset <= pld_offset) {
        prefetch(end_from, end_to, -(wordSize + offset));
        offset += ArmCopyCacheLineSize;
      };
    }

    Label L_shifted_loop;
    __ align(OptoLoopAlignment);
    __ BIND(L_shifted_loop);

    if (prefetch_before) {
      // do the 1st ldm/ldp first anyway (no locking issues with early STM/STP)
      prefetch(end_from, end_to, -(wordSize + bytes_per_loop + pld_offset));
      __ BIND(L_skip_pld);
    } else {
      __ cmp_32(count, count_per_loop);
      __ b(L_last_read, lt);
    }

    if (split_read) {
      __ ldmdb(end_from, RegisterSet(R7, R10), writeback);
      __ mov(R12, AsmOperand(R3, lsl, lsl_shift)); // part of R3 not yet written
      __ ldmdb(end_from, RegisterSet(R3, R6), writeback);
    } else {
      __ mov(R12, AsmOperand(R3, lsl, lsl_shift)); // part of R3 not yet written
      __ ldmdb(end_from, RegisterSet(R3, R10), writeback);
    }

    __ subs_32(count, count, count_per_loop);

    if (prefetch_after) { // do prefetch during ldm/ldp latency
      prefetch(end_from, end_to, -(wordSize + pld_offset), -bytes_per_loop);
    }

    // prepare the values in R4..R10,R12
    __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift)); // merged above high  bytes of prev val
    __ logical_shift_left(R10, R10, lsl_shift); // unused part of prev val
    __ orr(R10, R10, AsmOperand(R9, lsr, lsr_shift)); // ...
    __ logical_shift_left(R9, R9, lsl_shift);
    __ orr(R9, R9, AsmOperand(R8, lsr, lsr_shift));
    __ logical_shift_left(R8, R8, lsl_shift);
    __ orr(R8, R8, AsmOperand(R7, lsr, lsr_shift));
    __ logical_shift_left(R7, R7, lsl_shift);
    __ orr(R7, R7, AsmOperand(R6, lsr, lsr_shift));
    __ logical_shift_left(R6, R6, lsl_shift);
    __ orr(R6, R6, AsmOperand(R5, lsr, lsr_shift));
    if (split_write) {
      // store early to reduce locking issues
      __ stmdb(end_to, RegisterSet(R6, R10) | R12, writeback, prefetch_before ? gt : ge);
    }
    __ logical_shift_left(R5, R5, lsl_shift);
    __ orr(R5, R5, AsmOperand(R4, lsr, lsr_shift));
    __ logical_shift_left(R4, R4, lsl_shift);
    __ orr(R4, R4, AsmOperand(R3, lsr, lsr_shift));

    if (split_write) {
      __ stmdb(end_to, RegisterSet(R4, R5), writeback, prefetch_before ? gt : ge);
    } else {
      __ stmdb(end_to, RegisterSet(R4, R10) | R12, writeback, prefetch_before ? gt : ge);
    }

    __ b(L_shifted_loop, gt); // no need to loop if 0 (when count need not be precise modulo bytes_per_loop)

    if (prefetch_before) {
      // the first loop may end earlier, allowing to skip pld at the end
      __ cmn_32(count, ((bytes_per_loop + pld_offset)/bytes_per_count));
      __ stmdb(end_to, RegisterSet(R4, R10) | R12, writeback); // stmdb was skipped
      __ b(L_skip_pld, ge);
      __ adds_32(count, count, ((bytes_per_loop + pld_offset) / bytes_per_count) + count_per_loop);
    }

    __ BIND(L_last_read);
    __ b(L_done, eq);

      switch(bytes_per_count) {
      case 2:
      __ mov(R12, AsmOperand(R3, lsl, lsl_shift)); // part of R3 not yet written
      __ tst(count, 8);
      __ ldmdb(end_from, RegisterSet(R7,R10), writeback, ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ mov(R10, AsmOperand(R10, lsl, lsl_shift),ne); // unused part of prev val
      __ orr(R10, R10, AsmOperand(R9, lsr, lsr_shift),ne); // ...
      __ mov(R9, AsmOperand(R9, lsl, lsl_shift),ne);
      __ orr(R9, R9, AsmOperand(R8, lsr, lsr_shift),ne);
      __ mov(R8, AsmOperand(R8, lsl, lsl_shift),ne);
      __ orr(R8, R8, AsmOperand(R7, lsr, lsr_shift),ne);
      __ stmdb(end_to, RegisterSet(R8,R10)|R12, writeback, ne);
      __ mov(R12, AsmOperand(R7, lsl, lsl_shift), ne);

      __ tst(count, 4);
      __ ldmdb(end_from, RegisterSet(R9, R10), writeback, ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ mov(R10, AsmOperand(R10, lsl, lsl_shift),ne); // unused part of prev val
      __ orr(R10, R10, AsmOperand(R9, lsr,lsr_shift),ne); // ...
      __ stmdb(end_to, RegisterSet(R10)|R12, writeback, ne);
      __ mov(R12, AsmOperand(R9, lsl, lsl_shift), ne);

      __ tst(count, 2);
      __ ldr(R10, Address(end_from, -4, pre_indexed), ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ str(R12, Address(end_to, -4, pre_indexed), ne);
      __ mov(R12, AsmOperand(R10, lsl, lsl_shift), ne);

      __ tst(count, 1);
      __ mov(R12, AsmOperand(R12, lsr, lsr_shift),ne);
      __ strh(R12, Address(end_to, -2, pre_indexed), ne); // one last short
      break;

      case 1:
      __ mov(R12, AsmOperand(R3, lsl, lsl_shift)); // part of R3 not yet written
      __ tst(count, 16);
      __ ldmdb(end_from, RegisterSet(R7,R10), writeback, ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ mov(R10, AsmOperand(R10, lsl, lsl_shift),ne); // unused part of prev val
      __ orr(R10, R10, AsmOperand(R9, lsr, lsr_shift),ne); // ...
      __ mov(R9, AsmOperand(R9, lsl, lsl_shift),ne);
      __ orr(R9, R9, AsmOperand(R8, lsr, lsr_shift),ne);
      __ mov(R8, AsmOperand(R8, lsl, lsl_shift),ne);
      __ orr(R8, R8, AsmOperand(R7, lsr, lsr_shift),ne);
      __ stmdb(end_to, RegisterSet(R8,R10)|R12, writeback, ne);
      __ mov(R12, AsmOperand(R7, lsl, lsl_shift), ne);

      __ tst(count, 8);
      __ ldmdb(end_from, RegisterSet(R9,R10), writeback, ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ mov(R10, AsmOperand(R10, lsl, lsl_shift),ne); // unused part of prev val
      __ orr(R10, R10, AsmOperand(R9, lsr, lsr_shift),ne); // ...
      __ stmdb(end_to, RegisterSet(R10)|R12, writeback, ne);
      __ mov(R12, AsmOperand(R9, lsl, lsl_shift), ne);

      __ tst(count, 4);
      __ ldr(R10, Address(end_from, -4, pre_indexed), ne);
      __ orr(R12, R12, AsmOperand(R10, lsr, lsr_shift), ne);
      __ str(R12, Address(end_to, -4, pre_indexed), ne);
      __ mov(R12, AsmOperand(R10, lsl, lsl_shift), ne);

      __ tst(count, 2);
      if (lsr_shift != 24) {
        // avoid useless reading R10 when we already have 3 bytes ready in R12
        __ ldr(R10, Address(end_from, -4, pre_indexed), ne);
        __ orr(R12, R12, AsmOperand(R10, lsr,lsr_shift), ne);
      }

      // Note: R12 contains enough bytes ready to write (3 needed at most)
      // write the 2 MSBs
      __ mov(R9, AsmOperand(R12, lsr, 16), ne);
      __ strh(R9, Address(end_to, -2, pre_indexed), ne);
      // promote remaining to MSB
      __ mov(R12, AsmOperand(R12, lsl, 16), ne);

      __ tst(count, 1);
      // write the MSB of R12
      __ mov(R12, AsmOperand(R12, lsr, 24), ne);
      __ strb(R12, Address(end_to, -1, pre_indexed), ne);

      break;
      }

    __ BIND(L_done);
    return 0; // no minimum
  }

  // This method is very useful for merging forward/backward implementations
  Address get_addr_with_indexing(Register base, int delta, bool forward) {
    if (forward) {
      return Address(base, delta, post_indexed);
    } else {
      return Address(base, -delta, pre_indexed);
    }
  }

  void load_one(Register rd, Register from, int size_in_bytes, bool forward, AsmCondition cond = al, Register rd2 = noreg) {
    assert_different_registers(from, rd, rd2);
    if (size_in_bytes < 8) {
      Address addr = get_addr_with_indexing(from, size_in_bytes, forward);
      __ load_sized_value(rd, addr, size_in_bytes, false, cond);
    } else {
      assert (rd2 != noreg, "second value register must be specified");
      assert (rd->encoding() < rd2->encoding(), "wrong value register set");

      if (forward) {
        __ ldmia(from, RegisterSet(rd) | rd2, writeback, cond);
      } else {
        __ ldmdb(from, RegisterSet(rd) | rd2, writeback, cond);
      }
    }
  }

  void store_one(Register rd, Register to, int size_in_bytes, bool forward, AsmCondition cond = al, Register rd2 = noreg) {
    assert_different_registers(to, rd, rd2);
    if (size_in_bytes < 8) {
      Address addr = get_addr_with_indexing(to, size_in_bytes, forward);
      __ store_sized_value(rd, addr, size_in_bytes, cond);
    } else {
      assert (rd2 != noreg, "second value register must be specified");
      assert (rd->encoding() < rd2->encoding(), "wrong value register set");

      if (forward) {
        __ stmia(to, RegisterSet(rd) | rd2, writeback, cond);
      } else {
        __ stmdb(to, RegisterSet(rd) | rd2, writeback, cond);
      }
    }
  }

  // Copies data from 'from' to 'to' in specified direction to align 'from' by 64 bits.
  // (on 32-bit ARM 64-bit alignment is better for LDM).
  //
  // Arguments:
  //     from:              beginning (if forward) or upper bound (if !forward) of the region to be read
  //     to:                beginning (if forward) or upper bound (if !forward) of the region to be written
  //     count:             32-bit int, maximum number of elements which can be copied
  //     bytes_per_count:   size of an element
  //     forward:           specifies copy direction
  //
  // Notes:
  //   'from' and 'to' must be aligned by 'bytes_per_count'
  //   'count' must not be less than the returned value
  //   shifts 'from' and 'to' by the number of copied bytes in corresponding direction
  //   decreases 'count' by the number of elements copied
  //
  // Returns maximum number of bytes which may be copied.
  int align_src(Register from, Register to, Register count, Register tmp, int bytes_per_count, bool forward) {
    assert_different_registers(from, to, count, tmp);
    if (bytes_per_count < 8) {
      Label L_align_src;
      __ BIND(L_align_src);
      __ tst(from, 7);
      // ne => not aligned: copy one element and (if bytes_per_count < 4) loop
      __ sub(count, count, 1, ne);
      load_one(tmp, from, bytes_per_count, forward, ne);
      store_one(tmp, to, bytes_per_count, forward, ne);
      if (bytes_per_count < 4) {
        __ b(L_align_src, ne); // if bytes_per_count == 4, then 0 or 1 loop iterations are enough
      }
    }
    return 7/bytes_per_count;
  }

  // Copies 'count' of 'bytes_per_count'-sized elements in the specified direction.
  //
  // Arguments:
  //     from:              beginning (if forward) or upper bound (if !forward) of the region to be read
  //     to:                beginning (if forward) or upper bound (if !forward) of the region to be written
  //     count:             32-bit int, number of elements to be copied
  //     entry:             copy loop entry point
  //     bytes_per_count:   size of an element
  //     forward:           specifies copy direction
  //
  // Notes:
  //     shifts 'from' and 'to'
  void copy_small_array(Register from, Register to, Register count, Register tmp, Register tmp2, int bytes_per_count, bool forward, Label & entry, bool unsafe_copy = false) {
    assert_different_registers(from, to, count, tmp);

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, unsafe_copy, true);
      __ align(OptoLoopAlignment);
      Label L_small_loop;
      __ BIND(L_small_loop);
      store_one(tmp, to, bytes_per_count, forward, al, tmp2);
      __ BIND(entry); // entry point
      __ subs(count, count, 1);
      load_one(tmp, from, bytes_per_count, forward, ge, tmp2);
      __ b(L_small_loop, ge);
    }
  }

  // Aligns 'to' by reading one word from 'from' and writting its part to 'to'.
  //
  // Arguments:
  //     to:                beginning (if forward) or upper bound (if !forward) of the region to be written
  //     count:             32-bit int, number of elements allowed to be copied
  //     to_remainder:      remainder of dividing 'to' by wordSize
  //     bytes_per_count:   size of an element
  //     forward:           specifies copy direction
  //     Rval:              contains an already read but not yet written word;
  //                        its' LSBs (if forward) or MSBs (if !forward) are to be written to align 'to'.
  //
  // Notes:
  //     'count' must not be less then the returned value
  //     'to' must be aligned by bytes_per_count but must not be aligned by wordSize
  //     shifts 'to' by the number of written bytes (so that it becomes the bound of memory to be written)
  //     decreases 'count' by the the number of elements written
  //     Rval's MSBs or LSBs remain to be written further by generate_{forward,backward}_shifted_copy_loop
  int align_dst(Register to, Register count, Register Rval, Register tmp,
                                        int to_remainder, int bytes_per_count, bool forward) {
    assert_different_registers(to, count, tmp, Rval);

    assert (0 < to_remainder && to_remainder < wordSize, "to_remainder is not valid");
    assert (to_remainder % bytes_per_count == 0, "to must be aligned by bytes_per_count");

    int bytes_to_write = forward ? (wordSize - to_remainder) : to_remainder;

    int offset = 0;

    for (int l = 0; l < LogBytesPerWord; ++l) {
      int s = (1 << l);
      if (bytes_to_write & s) {
        int new_offset = offset + s*BitsPerByte;
        if (forward) {
          if (offset == 0) {
            store_one(Rval, to, s, forward);
          } else {
            __ logical_shift_right(tmp, Rval, offset);
            store_one(tmp, to, s, forward);
          }
        } else {
          __ logical_shift_right(tmp, Rval, BitsPerWord - new_offset);
          store_one(tmp, to, s, forward);
        }

        offset = new_offset;
      }
    }

    assert (offset == bytes_to_write * BitsPerByte, "all bytes must be copied");

    __ sub_32(count, count, bytes_to_write/bytes_per_count);

    return bytes_to_write / bytes_per_count;
  }

  // Copies 'count' of elements using shifted copy loop
  //
  // Arguments:
  //     from:              beginning (if forward) or upper bound (if !forward) of the region to be read
  //     to:                beginning (if forward) or upper bound (if !forward) of the region to be written
  //     count:             32-bit int, number of elements to be copied
  //     to_remainder:      remainder of dividing 'to' by wordSize
  //     bytes_per_count:   size of an element
  //     forward:           specifies copy direction
  //     Rval:              contains an already read but not yet written word
  //
  //
  // Notes:
  //     'count' must not be less then the returned value
  //     'from' must be aligned by wordSize
  //     'to' must be aligned by bytes_per_count but must not be aligned by wordSize
  //     shifts 'to' by the number of copied bytes
  //
  // Scratches R3-R10, R12
  int align_dst_and_generate_shifted_copy_loop(Register from, Register to, Register count, Register Rval,
                                                        int to_remainder, int bytes_per_count, bool forward) {

    assert (0 < to_remainder && to_remainder < wordSize, "to_remainder is invalid");

    const Register tmp  = forward ? R3 : R12;
    assert_different_registers(from, to, count, Rval, tmp);

    int required_to_align = align_dst(to, count, Rval, tmp, to_remainder, bytes_per_count, forward);

    int lsr_shift = (wordSize - to_remainder) * BitsPerByte;
    int lsl_shift = to_remainder * BitsPerByte;

    int min_copy;
    if (forward) {
      min_copy = generate_forward_shifted_copy_loop(from, to, count, bytes_per_count, lsr_shift, lsl_shift);
    } else {
      min_copy = generate_backward_shifted_copy_loop(from, to, count, bytes_per_count, lsr_shift, lsl_shift);
    }

    return min_copy + required_to_align;
  }

  // Copies 'count' of elements using shifted copy loop
  //
  // Arguments:
  //     from:              beginning (if forward) or upper bound (if !forward) of the region to be read
  //     to:                beginning (if forward) or upper bound (if !forward) of the region to be written
  //     count:             32-bit int, number of elements to be copied
  //     bytes_per_count:   size of an element
  //     forward:           specifies copy direction
  //
  // Notes:
  //     'count' must not be less then the returned value
  //     'from' must be aligned by wordSize
  //     'to' must be aligned by bytes_per_count but must not be aligned by wordSize
  //     shifts 'to' by the number of copied bytes
  //
  // Scratches 'from', 'count', R3 and R12.
  // R4-R10 saved for use.
  int align_dst_and_generate_shifted_copy_loop(Register from, Register to, Register count, int bytes_per_count, bool forward, bool unsafe_copy = false) {

    const Register Rval = forward ? R12 : R3; // as generate_{forward,backward}_shifted_copy_loop expect

    int min_copy = 0;

    // Note: if {seq} is a sequence of numbers, L{seq} means that if the execution reaches this point,
    // then the remainder of 'to' divided by wordSize is one of elements of {seq}.

    __ push(RegisterSet(R4,R10));

    {
      // UnsafeCopyMemory page error: continue after ucm
      UnsafeCopyMemoryMark ucmm(this, unsafe_copy, true);
      load_one(Rval, from, wordSize, forward);

      switch (bytes_per_count) {
        case 2:
          min_copy = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 2, bytes_per_count, forward);
          break;
        case 1:
        {
          Label L1, L2, L3;
          int min_copy1, min_copy2, min_copy3;

          Label L_loop_finished;

          if (forward) {
              __ tbz(to, 0, L2);
              __ tbz(to, 1, L1);

              __ BIND(L3);
              min_copy3 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 3, bytes_per_count, forward);
              __ b(L_loop_finished);

              __ BIND(L1);
              min_copy1 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 1, bytes_per_count, forward);
              __ b(L_loop_finished);

              __ BIND(L2);
              min_copy2 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 2, bytes_per_count, forward);
          } else {
              __ tbz(to, 0, L2);
              __ tbnz(to, 1, L3);

              __ BIND(L1);
              min_copy1 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 1, bytes_per_count, forward);
              __ b(L_loop_finished);

               __ BIND(L3);
              min_copy3 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 3, bytes_per_count, forward);
              __ b(L_loop_finished);

             __ BIND(L2);
              min_copy2 = align_dst_and_generate_shifted_copy_loop(from, to, count, Rval, 2, bytes_per_count, forward);
          }

          min_copy = MAX2(MAX2(min_copy1, min_copy2), min_copy3);

          __ BIND(L_loop_finished);

          break;
        }
        default:
          ShouldNotReachHere();
          break;
      }
    }
    __ pop(RegisterSet(R4,R10));

    return min_copy;
  }

#ifndef PRODUCT
  int * get_arraycopy_counter(int bytes_per_count) {
    switch (bytes_per_count) {
      case 1:
        return &SharedRuntime::_jbyte_array_copy_ctr;
      case 2:
        return &SharedRuntime::_jshort_array_copy_ctr;
      case 4:
        return &SharedRuntime::_jint_array_copy_ctr;
      case 8:
        return &SharedRuntime::_jlong_array_copy_ctr;
      default:
        ShouldNotReachHere();
        return NULL;
    }
  }
#endif // !PRODUCT

  address generate_unsafecopy_common_error_exit() {
    address start_pc = __ pc();
      __ mov(R0, 0);
      __ ret();
    return start_pc;
  }

  //
  //  Generate stub for primitive array copy.  If "aligned" is true, the
  //  "from" and "to" addresses are assumed to be heapword aligned.
  //
  //  If "disjoint" is true, arrays are assumed to be disjoint, otherwise they may overlap and
  //  "nooverlap_target" must be specified as the address to jump if they don't.
  //
  // Arguments for generated stub:
  //      from:  R0
  //      to:    R1
  //      count: R2 treated as signed 32-bit int
  //
  address generate_primitive_copy(bool aligned, const char * name, bool status, int bytes_per_count, bool disjoint, address nooverlap_target = NULL) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    const Register from  = R0;   // source array address
    const Register to    = R1;   // destination array address
    const Register count = R2;   // elements count
    const Register tmp1  = R3;
    const Register tmp2  = R12;

    if (!aligned)  {
      BLOCK_COMMENT("Entry:");
    }

    __ zap_high_non_significant_bits(R2);

    if (!disjoint) {
      assert (nooverlap_target != NULL, "must be specified for conjoint case");
      array_overlap_test(nooverlap_target, exact_log2(bytes_per_count), tmp1, tmp2);
    }

    inc_counter_np(*get_arraycopy_counter(bytes_per_count), tmp1, tmp2);

    // Conjoint case: since execution reaches this point, the arrays overlap, so performing backward copy
    // Disjoint case: perform forward copy
    bool forward = disjoint;


    if (!forward) {
      // Set 'from' and 'to' to upper bounds
      int log_bytes_per_count = exact_log2(bytes_per_count);
      __ add_ptr_scaled_int32(to,   to,   count, log_bytes_per_count);
      __ add_ptr_scaled_int32(from, from, count, log_bytes_per_count);
    }

    // There are two main copy loop implementations:
    //  *) The huge and complex one applicable only for large enough arrays
    //  *) The small and simple one applicable for any array (but not efficient for large arrays).
    // Currently "small" implementation is used if and only if the "large" one could not be used.
    // XXX optim: tune the limit higher ?
    // Large implementation lower applicability bound is actually determined by
    // aligned copy loop which require <=7 bytes for src alignment, and 8 words for aligned copy loop.
    const int small_copy_limit = (8*wordSize + 7) / bytes_per_count;

    Label L_small_array;
    __ cmp_32(count, small_copy_limit);
    __ b(L_small_array, le);

    // Otherwise proceed with large implementation.

    bool from_is_aligned = (bytes_per_count >= 8);
    if (aligned && forward && (HeapWordSize % 8 == 0)) {
        // if 'from' is heapword aligned and HeapWordSize is divisible by 8,
        //  then from is aligned by 8
        from_is_aligned = true;
    }

    int count_required_to_align = 0;
    {
      // UnsafeCopyMemoryMark page error: continue at UnsafeCopyMemory common_error_exit
      UnsafeCopyMemoryMark ucmm(this, !aligned, false);
      count_required_to_align = from_is_aligned ? 0 : align_src(from, to, count, tmp1, bytes_per_count, forward);
      assert (small_copy_limit >= count_required_to_align, "alignment could exhaust count");
    }

    // now 'from' is aligned

    bool to_is_aligned = false;

    if (bytes_per_count >= wordSize) {
      // 'to' is aligned by bytes_per_count, so it is aligned by wordSize
      to_is_aligned = true;
    } else {
      if (aligned && (8 % HeapWordSize == 0) && (HeapWordSize % wordSize == 0)) {
        // Originally 'from' and 'to' were heapword aligned;
        // (from - to) has not been changed, so since now 'from' is 8-byte aligned, then it is also heapword aligned,
        //  so 'to' is also heapword aligned and thus aligned by wordSize.
        to_is_aligned = true;
      }
    }

    Label L_unaligned_dst;

    if (!to_is_aligned) {
      BLOCK_COMMENT("Check dst alignment:");
      __ tst(to, wordSize - 1);
      __ b(L_unaligned_dst, ne); // 'to' is not aligned
    }

    // 'from' and 'to' are properly aligned

    int min_copy;
    if (forward) {
      min_copy = generate_forward_aligned_copy_loop(from, to, count, bytes_per_count, !aligned /*add UnsafeCopyMemory entry*/);
    } else {
      min_copy = generate_backward_aligned_copy_loop(from, to, count, bytes_per_count, !aligned /*add UnsafeCopyMemory entry*/);
    }
    assert(small_copy_limit >= count_required_to_align + min_copy, "first loop might exhaust count");

    if (status) {
      __ mov(R0, 0); // OK
    }

    __ ret();

    {
      copy_small_array(from, to, count, tmp1, tmp2, bytes_per_count, forward, L_small_array /* entry */, !aligned /*add UnsafeCopyMemory entry*/);

      if (status) {
        __ mov(R0, 0); // OK
      }

      __ ret();
    }

    if (! to_is_aligned) {
      __ BIND(L_unaligned_dst);
      int min_copy_shifted = align_dst_and_generate_shifted_copy_loop(from, to, count, bytes_per_count, forward, !aligned /*add UnsafeCopyMemory entry*/);
      assert (small_copy_limit >= count_required_to_align + min_copy_shifted, "first loop might exhaust count");

      if (status) {
        __ mov(R0, 0); // OK
      }

      __ ret();
    }

    return start;
  }


  // Generates pattern of code to be placed after raw data copying in generate_oop_copy
  // Includes return from arraycopy stub.
  //
  // Arguments:
  //     to:       destination pointer after copying.
  //               if 'forward' then 'to' == upper bound, else 'to' == beginning of the modified region
  //     count:    total number of copied elements, 32-bit int
  //
  // Blows all volatile R0-R3, Rtemp, LR) and 'to', 'count', 'tmp' registers.
  void oop_arraycopy_stub_epilogue_helper(Register to, Register count, Register tmp, bool status, bool forward, DecoratorSet decorators) {
    assert_different_registers(to, count, tmp);

    if (forward) {
      // 'to' is upper bound of the modified region
      // restore initial dst:
      __ sub_ptr_scaled_int32(to, to, count, LogBytesPerHeapOop);
    }

    // 'to' is the beginning of the region

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_epilogue(_masm, decorators, true, to, count, tmp);

    if (status) {
      __ mov(R0, 0); // OK
    }

    __ pop(PC);
  }


  //  Generate stub for assign-compatible oop copy.  If "aligned" is true, the
  //  "from" and "to" addresses are assumed to be heapword aligned.
  //
  //  If "disjoint" is true, arrays are assumed to be disjoint, otherwise they may overlap and
  //  "nooverlap_target" must be specified as the address to jump if they don't.
  //
  // Arguments for generated stub:
  //      from:  R0
  //      to:    R1
  //      count: R2 treated as signed 32-bit int
  //
  address generate_oop_copy(bool aligned, const char * name, bool status, bool disjoint, address nooverlap_target = NULL) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    Register from  = R0;
    Register to    = R1;
    Register count = R2;
    Register tmp1  = R3;
    Register tmp2  = R12;


    if (!aligned) {
      BLOCK_COMMENT("Entry:");
    }

    __ zap_high_non_significant_bits(R2);

    if (!disjoint) {
      assert (nooverlap_target != NULL, "must be specified for conjoint case");
      array_overlap_test(nooverlap_target, LogBytesPerHeapOop, tmp1, tmp2);
    }

    inc_counter_np(SharedRuntime::_oop_array_copy_ctr, tmp1, tmp2);

    // Conjoint case: since execution reaches this point, the arrays overlap, so performing backward copy
    // Disjoint case: perform forward copy
    bool forward = disjoint;

    const int bytes_per_count = BytesPerHeapOop;
    const int log_bytes_per_count = LogBytesPerHeapOop;

    const Register saved_count = LR;
    const int callee_saved_regs = 3; // R0-R2

    // LR is used later to save barrier args
    __ push(LR);

    DecoratorSet decorators = IN_HEAP | IS_ARRAY;
    if (disjoint) {
      decorators |= ARRAYCOPY_DISJOINT;
    }
    if (aligned) {
      decorators |= ARRAYCOPY_ALIGNED;
    }

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, true, to, count, callee_saved_regs);

    // save arguments for barrier generation (after the pre barrier)
    __ mov(saved_count, count);

    if (!forward) {
      __ add_ptr_scaled_int32(to,   to,   count, log_bytes_per_count);
      __ add_ptr_scaled_int32(from, from, count, log_bytes_per_count);
    }

    // for short arrays, just do single element copy
    Label L_small_array;
    const int small_copy_limit = (8*wordSize + 7)/bytes_per_count; // XXX optim: tune the limit higher ?
    __ cmp_32(count, small_copy_limit);
    __ b(L_small_array, le);

    bool from_is_aligned = (bytes_per_count >= 8);
    if (aligned && forward && (HeapWordSize % 8 == 0)) {
        // if 'from' is heapword aligned and HeapWordSize is divisible by 8,
        //  then from is aligned by 8
        from_is_aligned = true;
    }

    int count_required_to_align = from_is_aligned ? 0 : align_src(from, to, count, tmp1, bytes_per_count, forward);
    assert (small_copy_limit >= count_required_to_align, "alignment could exhaust count");

    // now 'from' is aligned

    bool to_is_aligned = false;

    if (bytes_per_count >= wordSize) {
      // 'to' is aligned by bytes_per_count, so it is aligned by wordSize
      to_is_aligned = true;
    } else {
      if (aligned && (8 % HeapWordSize == 0) && (HeapWordSize % wordSize == 0)) {
        // Originally 'from' and 'to' were heapword aligned;
        // (from - to) has not been changed, so since now 'from' is 8-byte aligned, then it is also heapword aligned,
        //  so 'to' is also heapword aligned and thus aligned by wordSize.
        to_is_aligned = true;
      }
    }

    Label L_unaligned_dst;

    if (!to_is_aligned) {
      BLOCK_COMMENT("Check dst alignment:");
      __ tst(to, wordSize - 1);
      __ b(L_unaligned_dst, ne); // 'to' is not aligned
    }

    int min_copy;
    if (forward) {
      min_copy = generate_forward_aligned_copy_loop(from, to, count, bytes_per_count);
    } else {
      min_copy = generate_backward_aligned_copy_loop(from, to, count, bytes_per_count);
    }
    assert(small_copy_limit >= count_required_to_align + min_copy, "first loop might exhaust count");

    oop_arraycopy_stub_epilogue_helper(to, saved_count, /* tmp */ tmp1, status, forward, decorators);

    {
      copy_small_array(from, to, count, tmp1, noreg, bytes_per_count, forward, L_small_array);

      oop_arraycopy_stub_epilogue_helper(to, saved_count, /* tmp */ tmp1, status, forward, decorators);
    }

    if (!to_is_aligned) {
      __ BIND(L_unaligned_dst);
      ShouldNotReachHere();
      int min_copy_shifted = align_dst_and_generate_shifted_copy_loop(from, to, count, bytes_per_count, forward);
      assert (small_copy_limit >= count_required_to_align + min_copy_shifted, "first loop might exhaust count");

      oop_arraycopy_stub_epilogue_helper(to, saved_count, /* tmp */ tmp1, status, forward, decorators);
    }

    return start;
  }

  //  Generate 'unsafe' array copy stub
  //  Though just as safe as the other stubs, it takes an unscaled
  //  size_t argument instead of an element count.
  //
  // Arguments for generated stub:
  //      from:  R0
  //      to:    R1
  //      count: R2 byte count, treated as ssize_t, can be zero
  //
  // Examines the alignment of the operands and dispatches
  // to a long, int, short, or byte copy loop.
  //
  address generate_unsafe_copy(const char* name) {

    const Register R0_from   = R0;      // source array address
    const Register R1_to     = R1;      // destination array address
    const Register R2_count  = R2;      // elements count

    const Register R3_bits   = R3;      // test copy of low bits

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();
    const Register tmp = Rtemp;

    // bump this on entry, not on exit:
    inc_counter_np(SharedRuntime::_unsafe_array_copy_ctr, R3, tmp);

    __ orr(R3_bits, R0_from, R1_to);
    __ orr(R3_bits, R2_count, R3_bits);

    __ tst(R3_bits, BytesPerLong-1);
    __ mov(R2_count,AsmOperand(R2_count,asr,LogBytesPerLong), eq);
    __ jump(StubRoutines::_jlong_arraycopy, relocInfo::runtime_call_type, tmp, eq);

    __ tst(R3_bits, BytesPerInt-1);
    __ mov(R2_count,AsmOperand(R2_count,asr,LogBytesPerInt), eq);
    __ jump(StubRoutines::_jint_arraycopy, relocInfo::runtime_call_type, tmp, eq);

    __ tst(R3_bits, BytesPerShort-1);
    __ mov(R2_count,AsmOperand(R2_count,asr,LogBytesPerShort), eq);
    __ jump(StubRoutines::_jshort_arraycopy, relocInfo::runtime_call_type, tmp, eq);

    __ jump(StubRoutines::_jbyte_arraycopy, relocInfo::runtime_call_type, tmp);
    return start;
  }

  // Helper for generating a dynamic type check.
  // Smashes only the given temp registers.
  void generate_type_check(Register sub_klass,
                           Register super_check_offset,
                           Register super_klass,
                           Register tmp1,
                           Register tmp2,
                           Register tmp3,
                           Label& L_success) {
    assert_different_registers(sub_klass, super_check_offset, super_klass, tmp1, tmp2, tmp3);

    BLOCK_COMMENT("type_check:");

    // If the pointers are equal, we are done (e.g., String[] elements).

    __ cmp(super_klass, sub_klass);
    __ b(L_success, eq); // fast success


    Label L_loop, L_fail;

    int sc_offset = in_bytes(Klass::secondary_super_cache_offset());

    // Check the supertype display:
    __ ldr(tmp1, Address(sub_klass, super_check_offset));
    __ cmp(tmp1, super_klass);
    __ b(L_success, eq);

    __ cmp(super_check_offset, sc_offset);
    __ b(L_fail, ne); // failure

    BLOCK_COMMENT("type_check_slow_path:");

    // a couple of useful fields in sub_klass:
    int ss_offset = in_bytes(Klass::secondary_supers_offset());

    // Do a linear scan of the secondary super-klass chain.

#ifndef PRODUCT
    int* pst_counter = &SharedRuntime::_partial_subtype_ctr;
    __ inc_counter((address) pst_counter, tmp1, tmp2);
#endif

    Register scan_temp = tmp1;
    Register count_temp = tmp2;

    // We will consult the secondary-super array.
    __ ldr(scan_temp, Address(sub_klass, ss_offset));

    Register search_key = super_klass;

    // Load the array length.
    __ ldr_s32(count_temp, Address(scan_temp, Array<Klass*>::length_offset_in_bytes()));
    __ add(scan_temp, scan_temp, Array<Klass*>::base_offset_in_bytes());

    __ add(count_temp, count_temp, 1);

    // Top of search loop
    __ bind(L_loop);
    // Notes:
    //  scan_temp starts at the array elements
    //  count_temp is 1+size

    __ subs(count_temp, count_temp, 1);
    __ b(L_fail, eq); // not found

    // Load next super to check
    // In the array of super classes elements are pointer sized.
    int element_size = wordSize;
    __ ldr(tmp3, Address(scan_temp, element_size, post_indexed));

    // Look for Rsuper_klass on Rsub_klass's secondary super-class-overflow list
    __ cmp(tmp3, search_key);

    // A miss means we are NOT a subtype and need to keep looping
    __ b(L_loop, ne);

    // Falling out the bottom means we found a hit; we ARE a subtype

    // Success.  Cache the super we found and proceed in triumph.
    __ str(super_klass, Address(sub_klass, sc_offset));

    // Jump to success
    __ b(L_success);

    // Fall through on failure!
    __ bind(L_fail);
  }

  //  Generate stub for checked oop copy.
  //
  // Arguments for generated stub:
  //      from:  R0
  //      to:    R1
  //      count: R2 treated as signed 32-bit int
  //      ckoff: R3 (super_check_offset)
  //      ckval: R4 (super_klass)
  //      ret:   R0 zero for success; (-1^K) where K is partial transfer count (32-bit)
  //
  address generate_checkcast_copy(const char * name) {
    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    const Register from  = R0;  // source array address
    const Register to    = R1;  // destination array address
    const Register count = R2;  // elements count

    const Register R3_ckoff  = R3;      // super_check_offset
    const Register R4_ckval  = R4;      // super_klass

    const int callee_saved_regs = 4; // LR saved differently

    Label load_element, store_element, do_epilogue, fail;

    BLOCK_COMMENT("Entry:");

    __ zap_high_non_significant_bits(R2);

    int pushed = 0;
    __ push(LR);
    pushed+=1;

    DecoratorSet decorators = IN_HEAP | IS_ARRAY | ARRAYCOPY_CHECKCAST;

    BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
    bs->arraycopy_prologue(_masm, decorators, true, to, count, callee_saved_regs);

    const RegisterSet caller_saved_regs = RegisterSet(R4,R6) | RegisterSet(R8,R9) | altFP_7_11;
    __ push(caller_saved_regs);
    assert(caller_saved_regs.size() == 6, "check the count");
    pushed+=6;

    __ ldr(R4_ckval,Address(SP, wordSize*pushed)); // read the argument that was on the stack

    // Save arguments for barrier generation (after the pre barrier):
    // - must be a caller saved register and not LR
    // - ARM32: avoid R10 in case RThread is needed
    const Register saved_count = altFP_7_11;
    __ movs(saved_count, count); // and test count
    __ b(load_element,ne);

    // nothing to copy
    __ mov(R0, 0);

    __ pop(caller_saved_regs);
    __ pop(PC);

    // ======== begin loop ========
    // (Loop is rotated; its entry is load_element.)
    __ align(OptoLoopAlignment);
    __ BIND(store_element);
    if (UseCompressedOops) {
      __ store_heap_oop(Address(to, BytesPerHeapOop, post_indexed), R5);  // store the oop, changes flags
      __ subs_32(count,count,1);
    } else {
      __ subs_32(count,count,1);
      __ str(R5, Address(to, BytesPerHeapOop, post_indexed));             // store the oop
    }
    __ b(do_epilogue, eq); // count exhausted

    // ======== loop entry is here ========
    __ BIND(load_element);
    __ load_heap_oop(R5, Address(from, BytesPerHeapOop, post_indexed));  // load the oop
    __ cbz(R5, store_element); // NULL

    __ load_klass(R6, R5);

    generate_type_check(R6, R3_ckoff, R4_ckval, /*tmps*/ R12, R8, R9,
                        // branch to this on success:
                        store_element);
    // ======== end loop ========

    // It was a real error; we must depend on the caller to finish the job.
    // Register count has number of *remaining* oops, saved_count number of *total* oops.
    // Emit GC store barriers for the oops we have copied
    // and report their number to the caller (0 or (-1^n))
    __ BIND(fail);

    // Note: fail marked by the fact that count differs from saved_count

    __ BIND(do_epilogue);

    Register copied = R4; // saved
    Label L_not_copied;

    __ subs_32(copied, saved_count, count); // copied count (in saved reg)
    __ b(L_not_copied, eq); // nothing was copied, skip post barrier
    __ sub(to, to, AsmOperand(copied, lsl, LogBytesPerHeapOop)); // initial to value
    __ mov(R12, copied); // count arg scratched by post barrier

    bs->arraycopy_epilogue(_masm, decorators, true, to, R12, R3);

    assert_different_registers(R3,R12,LR,copied,saved_count);
    inc_counter_np(SharedRuntime::_checkcast_array_copy_ctr, R3, R12);

    __ BIND(L_not_copied);
    __ cmp_32(copied, saved_count); // values preserved in saved registers

    __ mov(R0, 0, eq); // 0 if all copied
    __ mvn(R0, copied, ne); // else NOT(copied)
    __ pop(caller_saved_regs);
    __ pop(PC);

    return start;
  }

  // Perform range checks on the proposed arraycopy.
  // Kills the two temps, but nothing else.
  void arraycopy_range_checks(Register src,     // source array oop
                              Register src_pos, // source position (32-bit int)
                              Register dst,     // destination array oop
                              Register dst_pos, // destination position (32-bit int)
                              Register length,  // length of copy (32-bit int)
                              Register temp1, Register temp2,
                              Label& L_failed) {

    BLOCK_COMMENT("arraycopy_range_checks:");

    //  if (src_pos + length > arrayOop(src)->length() ) FAIL;

    const Register array_length = temp1;  // scratch
    const Register end_pos      = temp2;  // scratch

    __ add_32(end_pos, length, src_pos);  // src_pos + length
    __ ldr_s32(array_length, Address(src, arrayOopDesc::length_offset_in_bytes()));
    __ cmp_32(end_pos, array_length);
    __ b(L_failed, hi);

    //  if (dst_pos + length > arrayOop(dst)->length() ) FAIL;
    __ add_32(end_pos, length, dst_pos); // dst_pos + length
    __ ldr_s32(array_length, Address(dst, arrayOopDesc::length_offset_in_bytes()));
    __ cmp_32(end_pos, array_length);
    __ b(L_failed, hi);

    BLOCK_COMMENT("arraycopy_range_checks done");
  }

  //
  //  Generate generic array copy stubs
  //
  //  Input:
  //    R0    -  src oop
  //    R1    -  src_pos (32-bit int)
  //    R2    -  dst oop
  //    R3    -  dst_pos (32-bit int)
  //    SP[0] -  element count (32-bit int)
  //
  //  Output: (32-bit int)
  //    R0 ==  0  -  success
  //    R0 <   0  -  need to call System.arraycopy
  //
  address generate_generic_copy(const char *name) {
    Label L_failed, L_objArray;

    // Input registers
    const Register src      = R0;  // source array oop
    const Register src_pos  = R1;  // source position
    const Register dst      = R2;  // destination array oop
    const Register dst_pos  = R3;  // destination position

    // registers used as temp
    const Register R5_src_klass = R5; // source array klass
    const Register R6_dst_klass = R6; // destination array klass
    const Register R_lh         = altFP_7_11; // layout handler
    const Register R8_temp      = R8;

    __ align(CodeEntryAlignment);
    StubCodeMark mark(this, "StubRoutines", name);
    address start = __ pc();

    __ zap_high_non_significant_bits(R1);
    __ zap_high_non_significant_bits(R3);
    __ zap_high_non_significant_bits(R4);

    int pushed = 0;
    const RegisterSet saved_regs = RegisterSet(R4,R6) | RegisterSet(R8,R9) | altFP_7_11;
    __ push(saved_regs);
    assert(saved_regs.size() == 6, "check the count");
    pushed+=6;

    // bump this on entry, not on exit:
    inc_counter_np(SharedRuntime::_generic_array_copy_ctr, R5, R12);

    const Register length   = R4;  // elements count
    __ ldr(length, Address(SP,4*pushed));


    //-----------------------------------------------------------------------
    // Assembler stubs will be used for this call to arraycopy
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
    BLOCK_COMMENT("arraycopy initial argument checks");

    //  if (src == NULL) return -1;
    __ cbz(src, L_failed);

    //  if (src_pos < 0) return -1;
    __ cmp_32(src_pos, 0);
    __ b(L_failed, lt);

    //  if (dst == NULL) return -1;
    __ cbz(dst, L_failed);

    //  if (dst_pos < 0) return -1;
    __ cmp_32(dst_pos, 0);
    __ b(L_failed, lt);

    //  if (length < 0) return -1;
    __ cmp_32(length, 0);
    __ b(L_failed, lt);

    BLOCK_COMMENT("arraycopy argument klass checks");
    //  get src->klass()
    __ load_klass(R5_src_klass, src);

    // Load layout helper
    //
    //  |array_tag|     | header_size | element_type |     |log2_element_size|
    // 32        30    24            16              8     2                 0
    //
    //   array_tag: typeArray = 0x3, objArray = 0x2, non-array = 0x0
    //

    int lh_offset = in_bytes(Klass::layout_helper_offset());
    __ ldr_u32(R_lh, Address(R5_src_klass, lh_offset));

    __ load_klass(R6_dst_klass, dst);

    // Handle objArrays completely differently...
    juint objArray_lh = Klass::array_layout_helper(T_OBJECT);
    __ mov_slow(R8_temp, objArray_lh);
    __ cmp_32(R_lh, R8_temp);
    __ b(L_objArray,eq);

    //  if (src->klass() != dst->klass()) return -1;
    __ cmp(R5_src_klass, R6_dst_klass);
    __ b(L_failed, ne);

    //  if (!src->is_Array()) return -1;
    __ cmp_32(R_lh, Klass::_lh_neutral_value); // < 0
    __ b(L_failed, ge);

    arraycopy_range_checks(src, src_pos, dst, dst_pos, length,
                           R8_temp, R6_dst_klass, L_failed);

    {
      // TypeArrayKlass
      //
      // src_addr = (src + array_header_in_bytes()) + (src_pos << log2elemsize);
      // dst_addr = (dst + array_header_in_bytes()) + (dst_pos << log2elemsize);
      //

      const Register R6_offset = R6_dst_klass;    // array offset
      const Register R12_elsize = R12;            // log2 element size

      __ logical_shift_right(R6_offset, R_lh, Klass::_lh_header_size_shift);
      __ andr(R6_offset, R6_offset, (unsigned int)Klass::_lh_header_size_mask); // array_offset
      __ add(src, src, R6_offset);       // src array offset
      __ add(dst, dst, R6_offset);       // dst array offset
      __ andr(R12_elsize, R_lh, (unsigned int)Klass::_lh_log2_element_size_mask); // log2 element size

      // next registers should be set before the jump to corresponding stub
      const Register from     = R0;  // source array address
      const Register to       = R1;  // destination array address
      const Register count    = R2;  // elements count

      // 'from', 'to', 'count' registers should be set in this order
      // since they are the same as 'src', 'src_pos', 'dst'.


      BLOCK_COMMENT("scale indexes to element size");
      __ add(from, src, AsmOperand(src_pos, lsl, R12_elsize));       // src_addr
      __ add(to, dst, AsmOperand(dst_pos, lsl, R12_elsize));         // dst_addr

      __ mov(count, length);  // length

      // XXX optim: avoid later push in arraycopy variants ?

      __ pop(saved_regs);

      BLOCK_COMMENT("choose copy loop based on element size");
      __ cmp(R12_elsize, 0);
      __ b(StubRoutines::_jbyte_arraycopy,eq);

      __ cmp(R12_elsize, LogBytesPerShort);
      __ b(StubRoutines::_jshort_arraycopy,eq);

      __ cmp(R12_elsize, LogBytesPerInt);
      __ b(StubRoutines::_jint_arraycopy,eq);

      __ b(StubRoutines::_jlong_arraycopy);

    }

    // ObjArrayKlass
    __ BIND(L_objArray);
    // live at this point:  R5_src_klass, R6_dst_klass, src[_pos], dst[_pos], length

    Label L_plain_copy, L_checkcast_copy;
    //  test array classes for subtyping
    __ cmp(R5_src_klass, R6_dst_klass);         // usual case is exact equality
    __ b(L_checkcast_copy, ne);

    BLOCK_COMMENT("Identically typed arrays");
    {
      // Identically typed arrays can be copied without element-wise checks.
      arraycopy_range_checks(src, src_pos, dst, dst_pos, length,
                             R8_temp, R_lh, L_failed);

      // next registers should be set before the jump to corresponding stub
      const Register from     = R0;  // source array address
      const Register to       = R1;  // destination array address
      const Register count    = R2;  // elements count

      __ add(src, src, arrayOopDesc::base_offset_in_bytes(T_OBJECT)); //src offset
      __ add(dst, dst, arrayOopDesc::base_offset_in_bytes(T_OBJECT)); //dst offset
      __ add_ptr_scaled_int32(from, src, src_pos, LogBytesPerHeapOop);         // src_addr
      __ add_ptr_scaled_int32(to, dst, dst_pos, LogBytesPerHeapOop);           // dst_addr
      __ BIND(L_plain_copy);
      __ mov(count, length);

      __ pop(saved_regs); // XXX optim: avoid later push in oop_arraycopy ?
      __ b(StubRoutines::_oop_arraycopy);
    }

    {
      __ BIND(L_checkcast_copy);
      // live at this point:  R5_src_klass, R6_dst_klass

      // Before looking at dst.length, make sure dst is also an objArray.
      __ ldr_u32(R8_temp, Address(R6_dst_klass, lh_offset));
      __ cmp_32(R_lh, R8_temp);
      __ b(L_failed, ne);

      // It is safe to examine both src.length and dst.length.

      arraycopy_range_checks(src, src_pos, dst, dst_pos, length,
                             R8_temp, R_lh, L_failed);

      // next registers should be set before the jump to corresponding stub
      const Register from     = R0;  // source array address
      const Register to       = R1;  // destination array address
      const Register count    = R2;  // elements count

      // Marshal the base address arguments now, freeing registers.
      __ add(src, src, arrayOopDesc::base_offset_in_bytes(T_OBJECT)); //src offset
      __ add(dst, dst, arrayOopDesc::base_offset_in_bytes(T_OBJECT)); //dst offset
      __ add_ptr_scaled_int32(from, src, src_pos, LogBytesPerHeapOop);         // src_addr
      __ add_ptr_scaled_int32(to, dst, dst_pos, LogBytesPerHeapOop);           // dst_addr

      __ mov(count, length); // length (reloaded)

      Register sco_temp = R3;                   // this register is free now
      assert_different_registers(from, to, count, sco_temp,
                                 R6_dst_klass, R5_src_klass);

      // Generate the type check.
      int sco_offset = in_bytes(Klass::super_check_offset_offset());
      __ ldr_u32(sco_temp, Address(R6_dst_klass, sco_offset));
      generate_type_check(R5_src_klass, sco_temp, R6_dst_klass,
                          R8_temp, R9,
                          R12,
                          L_plain_copy);

      // Fetch destination element klass from the ObjArrayKlass header.
      int ek_offset = in_bytes(ObjArrayKlass::element_klass_offset());

      // the checkcast_copy loop needs two extra arguments:
      const Register Rdst_elem_klass = R3;
      __ ldr(Rdst_elem_klass, Address(R6_dst_klass, ek_offset));   // dest elem klass
      __ pop(saved_regs); // XXX optim: avoid later push in oop_arraycopy ?
      __ str(Rdst_elem_klass, Address(SP,0));    // dest elem klass argument
      __ ldr_u32(R3, Address(Rdst_elem_klass, sco_offset));  // sco of elem klass
      __ b(StubRoutines::_checkcast_arraycopy);
    }

    __ BIND(L_failed);

    __ pop(saved_regs);
    __ mvn(R0, 0); // failure, with 0 copied
    __ ret();

    return start;
  }

  // Safefetch stubs.
  void generate_safefetch(const char* name, int size, address* entry, address* fault_pc, address* continuation_pc) {
    // safefetch signatures:
    //   int      SafeFetch32(int*      adr, int      errValue);
    //   intptr_t SafeFetchN (intptr_t* adr, intptr_t errValue);
    //
    // arguments:
    //   R0 = adr
    //   R1 = errValue
    //
    // result:
    //   R0  = *adr or errValue

    StubCodeMark mark(this, "StubRoutines", name);

    // Entry point, pc or function descriptor.
    *entry = __ pc();

    // Load *adr into c_rarg2, may fault.
    *fault_pc = __ pc();

    switch (size) {
      case 4: // int32_t
        __ ldr_s32(R1, Address(R0));
        break;

      case 8: // int64_t
        Unimplemented();
        break;

      default:
        ShouldNotReachHere();
    }

    // return errValue or *adr
    *continuation_pc = __ pc();
    __ mov(R0, R1);
    __ ret();
  }

  void generate_arraycopy_stubs() {

    // Note:  the disjoint stubs must be generated first, some of
    //        the conjoint stubs use them.

    bool status = false; // non failing C2 stubs need not return a status in R0

#ifdef TEST_C2_GENERIC_ARRAYCOPY /* Internal development flag */
    // With this flag, the C2 stubs are tested by generating calls to
    // generic_arraycopy instead of Runtime1::arraycopy

    // Runtime1::arraycopy return a status in R0 (0 if OK, else ~copied)
    // and the result is tested to see whether the arraycopy stub should
    // be called.

    // When we test arraycopy this way, we must generate extra code in the
    // arraycopy methods callable from C2 generic_arraycopy to set the
    // status to 0 for those who always succeed (calling the slow path stub might
    // lead to errors since the copy has already been performed).

    status = true; // generate a status compatible with C1 calls
#endif

    address ucm_common_error_exit       =  generate_unsafecopy_common_error_exit();
    UnsafeCopyMemory::set_common_exit_stub_pc(ucm_common_error_exit);

    // these need always status in case they are called from generic_arraycopy
    StubRoutines::_jbyte_disjoint_arraycopy  = generate_primitive_copy(false, "jbyte_disjoint_arraycopy",  true, 1, true);
    StubRoutines::_jshort_disjoint_arraycopy = generate_primitive_copy(false, "jshort_disjoint_arraycopy", true, 2, true);
    StubRoutines::_jint_disjoint_arraycopy   = generate_primitive_copy(false, "jint_disjoint_arraycopy",   true, 4, true);
    StubRoutines::_jlong_disjoint_arraycopy  = generate_primitive_copy(false, "jlong_disjoint_arraycopy",  true, 8, true);
    StubRoutines::_oop_disjoint_arraycopy    = generate_oop_copy      (false, "oop_disjoint_arraycopy",    true,    true);

    StubRoutines::_arrayof_jbyte_disjoint_arraycopy  = generate_primitive_copy(true, "arrayof_jbyte_disjoint_arraycopy", status, 1, true);
    StubRoutines::_arrayof_jshort_disjoint_arraycopy = generate_primitive_copy(true, "arrayof_jshort_disjoint_arraycopy",status, 2, true);
    StubRoutines::_arrayof_jint_disjoint_arraycopy   = generate_primitive_copy(true, "arrayof_jint_disjoint_arraycopy",  status, 4, true);
    StubRoutines::_arrayof_jlong_disjoint_arraycopy  = generate_primitive_copy(true, "arrayof_jlong_disjoint_arraycopy", status, 8, true);
    StubRoutines::_arrayof_oop_disjoint_arraycopy    = generate_oop_copy      (true, "arrayof_oop_disjoint_arraycopy",   status,    true);

    // these need always status in case they are called from generic_arraycopy
    StubRoutines::_jbyte_arraycopy  = generate_primitive_copy(false, "jbyte_arraycopy",  true, 1, false, StubRoutines::_jbyte_disjoint_arraycopy);
    StubRoutines::_jshort_arraycopy = generate_primitive_copy(false, "jshort_arraycopy", true, 2, false, StubRoutines::_jshort_disjoint_arraycopy);
    StubRoutines::_jint_arraycopy   = generate_primitive_copy(false, "jint_arraycopy",   true, 4, false, StubRoutines::_jint_disjoint_arraycopy);
    StubRoutines::_jlong_arraycopy  = generate_primitive_copy(false, "jlong_arraycopy",  true, 8, false, StubRoutines::_jlong_disjoint_arraycopy);
    StubRoutines::_oop_arraycopy    = generate_oop_copy      (false, "oop_arraycopy",    true,    false, StubRoutines::_oop_disjoint_arraycopy);

    StubRoutines::_arrayof_jbyte_arraycopy    = generate_primitive_copy(true, "arrayof_jbyte_arraycopy",  status, 1, false, StubRoutines::_arrayof_jbyte_disjoint_arraycopy);
    StubRoutines::_arrayof_jshort_arraycopy   = generate_primitive_copy(true, "arrayof_jshort_arraycopy", status, 2, false, StubRoutines::_arrayof_jshort_disjoint_arraycopy);
#ifdef _LP64
    // since sizeof(jint) < sizeof(HeapWord), there's a different flavor:
    StubRoutines::_arrayof_jint_arraycopy     = generate_primitive_copy(true, "arrayof_jint_arraycopy",   status, 4, false, StubRoutines::_arrayof_jint_disjoint_arraycopy);
#else
    StubRoutines::_arrayof_jint_arraycopy     = StubRoutines::_jint_arraycopy;
#endif
    if (BytesPerHeapOop < HeapWordSize) {
      StubRoutines::_arrayof_oop_arraycopy    = generate_oop_copy      (true, "arrayof_oop_arraycopy",    status,    false, StubRoutines::_arrayof_oop_disjoint_arraycopy);
    } else {
      StubRoutines::_arrayof_oop_arraycopy    = StubRoutines::_oop_arraycopy;
    }
    StubRoutines::_arrayof_jlong_arraycopy    = StubRoutines::_jlong_arraycopy;

    StubRoutines::_checkcast_arraycopy = generate_checkcast_copy("checkcast_arraycopy");
    StubRoutines::_unsafe_arraycopy    = generate_unsafe_copy("unsafe_arraycopy");
    StubRoutines::_generic_arraycopy   = generate_generic_copy("generic_arraycopy");


  }

#define COMPILE_CRYPTO
#include "stubRoutinesCrypto_arm.cpp"

 private:

#undef  __
#define __ masm->

  //------------------------------------------------------------------------------------------------------------------------
  // Continuation point for throwing of implicit exceptions that are not handled in
  // the current activation. Fabricates an exception oop and initiates normal
  // exception dispatching in this frame.
  address generate_throw_exception(const char* name, address runtime_entry) {
    int insts_size = 128;
    int locs_size  = 32;
    CodeBuffer code(name, insts_size, locs_size);
    OopMapSet* oop_maps;
    int frame_size;
    int frame_complete;

    oop_maps = new OopMapSet();
    MacroAssembler* masm = new MacroAssembler(&code);

    address start = __ pc();

    frame_size = 2;
    __ mov(Rexception_pc, LR);
    __ raw_push(FP, LR);

    frame_complete = __ pc() - start;

    // Any extra arguments are already supposed to be R1 and R2
    __ mov(R0, Rthread);

    int pc_offset = __ set_last_Java_frame(SP, FP, false, Rtemp);
    assert(((__ pc()) - start) == __ offset(), "warning: start differs from code_begin");
    __ call(runtime_entry);
    if (pc_offset == -1) {
      pc_offset = __ offset();
    }

    // Generate oop map
    OopMap* map =  new OopMap(frame_size*VMRegImpl::slots_per_word, 0);
    oop_maps->add_gc_map(pc_offset, map);
    __ reset_last_Java_frame(Rtemp); // Rtemp free since scratched by far call

    __ raw_pop(FP, LR);
    __ jump(StubRoutines::forward_exception_entry(), relocInfo::runtime_call_type, Rtemp);

    RuntimeStub* stub = RuntimeStub::new_runtime_stub(name, &code, frame_complete,
                                                      frame_size, oop_maps, false);
    return stub->entry_point();
  }

  //---------------------------------------------------------------------------
  // Initialization

  void generate_initial() {
    // Generates all stubs and initializes the entry points

    //------------------------------------------------------------------------------------------------------------------------
    // entry points that exist in all platforms
    // Note: This is code that could be shared among different platforms - however the benefit seems to be smaller than
    //       the disadvantage of having a much more complicated generator structure. See also comment in stubRoutines.hpp.
    StubRoutines::_forward_exception_entry      = generate_forward_exception();

    StubRoutines::_call_stub_entry              =
      generate_call_stub(StubRoutines::_call_stub_return_address);
    // is referenced by megamorphic call
    StubRoutines::_catch_exception_entry        = generate_catch_exception();

    // stub for throwing stack overflow error used both by interpreter and compiler
    StubRoutines::_throw_StackOverflowError_entry  = generate_throw_exception("StackOverflowError throw_exception", CAST_FROM_FN_PTR(address, SharedRuntime::throw_StackOverflowError));

    // integer division used both by interpreter and compiler
    StubRoutines::Arm::_idiv_irem_entry = generate_idiv_irem();

    StubRoutines::_atomic_add_entry = generate_atomic_add();
    StubRoutines::_atomic_xchg_entry = generate_atomic_xchg();
    StubRoutines::_atomic_cmpxchg_entry = generate_atomic_cmpxchg();
    StubRoutines::_atomic_cmpxchg_long_entry = generate_atomic_cmpxchg_long();
    StubRoutines::_atomic_load_long_entry = generate_atomic_load_long();
    StubRoutines::_atomic_store_long_entry = generate_atomic_store_long();

    // Safefetch stubs.
    generate_safefetch("SafeFetch32", sizeof(int), &StubRoutines::_safefetch32_entry,
                                                   &StubRoutines::_safefetch32_fault_pc,
                                                   &StubRoutines::_safefetch32_continuation_pc);
    assert (sizeof(int) == wordSize, "32-bit architecture");
    StubRoutines::_safefetchN_entry           = StubRoutines::_safefetch32_entry;
    StubRoutines::_safefetchN_fault_pc        = StubRoutines::_safefetch32_fault_pc;
    StubRoutines::_safefetchN_continuation_pc = StubRoutines::_safefetch32_continuation_pc;
  }

  void generate_all() {
    // Generates all stubs and initializes the entry points

#ifdef COMPILER2
    // Generate partial_subtype_check first here since its code depends on
    // UseZeroBaseCompressedOops which is defined after heap initialization.
    StubRoutines::Arm::_partial_subtype_check                = generate_partial_subtype_check();
#endif
    // These entry points require SharedInfo::stack0 to be set up in non-core builds
    // and need to be relocatable, so they each fabricate a RuntimeStub internally.
    StubRoutines::_throw_AbstractMethodError_entry         = generate_throw_exception("AbstractMethodError throw_exception",          CAST_FROM_FN_PTR(address, SharedRuntime::throw_AbstractMethodError));
    StubRoutines::_throw_IncompatibleClassChangeError_entry= generate_throw_exception("IncompatibleClassChangeError throw_exception", CAST_FROM_FN_PTR(address, SharedRuntime::throw_IncompatibleClassChangeError));
    StubRoutines::_throw_NullPointerException_at_call_entry= generate_throw_exception("NullPointerException at call throw_exception", CAST_FROM_FN_PTR(address, SharedRuntime::throw_NullPointerException_at_call));

    //------------------------------------------------------------------------------------------------------------------------
    // entry points that are platform specific

    // support for verify_oop (must happen after universe_init)
    StubRoutines::_verify_oop_subroutine_entry     = generate_verify_oop();

    // arraycopy stubs used by compilers
    generate_arraycopy_stubs();

#ifdef COMPILE_CRYPTO
    // generate AES intrinsics code
    if (UseAESIntrinsics) {
      aes_init();
      StubRoutines::_aescrypt_encryptBlock = generate_aescrypt_encryptBlock();
      StubRoutines::_aescrypt_decryptBlock = generate_aescrypt_decryptBlock();
      StubRoutines::_cipherBlockChaining_encryptAESCrypt = generate_cipherBlockChaining_encryptAESCrypt();
      StubRoutines::_cipherBlockChaining_decryptAESCrypt = generate_cipherBlockChaining_decryptAESCrypt();
    }
#endif // COMPILE_CRYPTO
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

#define UCM_TABLE_MAX_ENTRIES 32
void StubGenerator_generate(CodeBuffer* code, bool all) {
  if (UnsafeCopyMemory::_table == NULL) {
    UnsafeCopyMemory::create_table(UCM_TABLE_MAX_ENTRIES);
  }
  StubGenerator g(code, all);
}
