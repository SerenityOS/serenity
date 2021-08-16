/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "classfile/javaClasses.hpp"
#include "compiler/compiler_globals.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "interpreter/templateTable.hpp"
#include "interpreter/bytecodeTracer.hpp"
#include "memory/resourceArea.hpp"
#include "oops/arrayOop.hpp"
#include "oops/methodData.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "runtime/arguments.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/timer.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/debug.hpp"
#include "utilities/powerOfTwo.hpp"
#include <sys/types.h>

#ifndef PRODUCT
#include "oops/method.hpp"
#endif // !PRODUCT

// Size of interpreter code.  Increase if too small.  Interpreter will
// fail with a guarantee ("not enough space for interpreter generation");
// if too small.
// Run with +PrintInterpreter to get the VM to print out the size.
// Max size with JVMTI
int TemplateInterpreter::InterpreterCodeSize = 200 * 1024;

#define __ _masm->

//-----------------------------------------------------------------------------

extern "C" void entry(CodeBuffer*);

//-----------------------------------------------------------------------------

address TemplateInterpreterGenerator::generate_slow_signature_handler() {
  address entry = __ pc();

  __ andr(esp, esp, -16);
  __ mov(c_rarg3, esp);
  // rmethod
  // rlocals
  // c_rarg3: first stack arg - wordSize

  // adjust sp
  __ sub(sp, c_rarg3, 18 * wordSize);
  __ str(lr, Address(__ pre(sp, -2 * wordSize)));
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::slow_signature_handler),
             rmethod, rlocals, c_rarg3);

  // r0: result handler

  // Stack layout:
  // rsp: return address           <- sp
  //      1 garbage
  //      8 integer args (if static first is unused)
  //      1 float/double identifiers
  //      8 double args
  //        stack args              <- esp
  //        garbage
  //        expression stack bottom
  //        bcp (NULL)
  //        ...

  // Restore LR
  __ ldr(lr, Address(__ post(sp, 2 * wordSize)));

  // Do FP first so we can use c_rarg3 as temp
  __ ldrw(c_rarg3, Address(sp, 9 * wordSize)); // float/double identifiers

  for (int i = 0; i < Argument::n_float_register_parameters_c; i++) {
    const FloatRegister r = as_FloatRegister(i);

    Label d, done;

    __ tbnz(c_rarg3, i, d);
    __ ldrs(r, Address(sp, (10 + i) * wordSize));
    __ b(done);
    __ bind(d);
    __ ldrd(r, Address(sp, (10 + i) * wordSize));
    __ bind(done);
  }

  // c_rarg0 contains the result from the call of
  // InterpreterRuntime::slow_signature_handler so we don't touch it
  // here.  It will be loaded with the JNIEnv* later.
  __ ldr(c_rarg1, Address(sp, 1 * wordSize));
  for (int i = c_rarg2->encoding(); i <= c_rarg7->encoding(); i += 2) {
    Register rm = as_Register(i), rn = as_Register(i+1);
    __ ldp(rm, rn, Address(sp, i * wordSize));
  }

  __ add(sp, sp, 18 * wordSize);
  __ ret(lr);

  return entry;
}


//
// Various method entries
//

address TemplateInterpreterGenerator::generate_math_entry(AbstractInterpreter::MethodKind kind) {
  // rmethod: Method*
  // r13: sender sp
  // esp: args

  if (!InlineIntrinsics) return NULL; // Generate a vanilla entry

  // These don't need a safepoint check because they aren't virtually
  // callable. We won't enter these intrinsics from compiled code.
  // If in the future we added an intrinsic which was virtually callable
  // we'd have to worry about how to safepoint so that this code is used.

  // mathematical functions inlined by compiler
  // (interpreter must provide identical implementation
  // in order to avoid monotonicity bugs when switching
  // from interpreter to compiler in the middle of some
  // computation)
  //
  // stack:
  //        [ arg ] <-- esp
  //        [ arg ]
  // retaddr in lr

  address entry_point = NULL;
  Register continuation = lr;
  switch (kind) {
  case Interpreter::java_lang_math_abs:
    entry_point = __ pc();
    __ ldrd(v0, Address(esp));
    __ fabsd(v0, v0);
    __ mov(sp, r13); // Restore caller's SP
    break;
  case Interpreter::java_lang_math_sqrt:
    entry_point = __ pc();
    __ ldrd(v0, Address(esp));
    __ fsqrtd(v0, v0);
    __ mov(sp, r13);
    break;
  case Interpreter::java_lang_math_sin :
  case Interpreter::java_lang_math_cos :
  case Interpreter::java_lang_math_tan :
  case Interpreter::java_lang_math_log :
  case Interpreter::java_lang_math_log10 :
  case Interpreter::java_lang_math_exp :
    entry_point = __ pc();
    __ ldrd(v0, Address(esp));
    __ mov(sp, r13);
    __ mov(r19, lr);
    continuation = r19;  // The first callee-saved register
    generate_transcendental_entry(kind, 1);
    break;
  case Interpreter::java_lang_math_pow :
    entry_point = __ pc();
    __ mov(r19, lr);
    continuation = r19;
    __ ldrd(v0, Address(esp, 2 * Interpreter::stackElementSize));
    __ ldrd(v1, Address(esp));
    __ mov(sp, r13);
    generate_transcendental_entry(kind, 2);
    break;
  case Interpreter::java_lang_math_fmaD :
    if (UseFMA) {
      entry_point = __ pc();
      __ ldrd(v0, Address(esp, 4 * Interpreter::stackElementSize));
      __ ldrd(v1, Address(esp, 2 * Interpreter::stackElementSize));
      __ ldrd(v2, Address(esp));
      __ fmaddd(v0, v0, v1, v2);
      __ mov(sp, r13); // Restore caller's SP
    }
    break;
  case Interpreter::java_lang_math_fmaF :
    if (UseFMA) {
      entry_point = __ pc();
      __ ldrs(v0, Address(esp, 2 * Interpreter::stackElementSize));
      __ ldrs(v1, Address(esp, Interpreter::stackElementSize));
      __ ldrs(v2, Address(esp));
      __ fmadds(v0, v0, v1, v2);
      __ mov(sp, r13); // Restore caller's SP
    }
    break;
  default:
    ;
  }
  if (entry_point) {
    __ br(continuation);
  }

  return entry_point;
}

  // double trigonometrics and transcendentals
  // static jdouble dsin(jdouble x);
  // static jdouble dcos(jdouble x);
  // static jdouble dtan(jdouble x);
  // static jdouble dlog(jdouble x);
  // static jdouble dlog10(jdouble x);
  // static jdouble dexp(jdouble x);
  // static jdouble dpow(jdouble x, jdouble y);

void TemplateInterpreterGenerator::generate_transcendental_entry(AbstractInterpreter::MethodKind kind, int fpargs) {
  address fn;
  switch (kind) {
  case Interpreter::java_lang_math_sin :
    if (StubRoutines::dsin() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dsin);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dsin());
    }
    break;
  case Interpreter::java_lang_math_cos :
    if (StubRoutines::dcos() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dcos);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dcos());
    }
    break;
  case Interpreter::java_lang_math_tan :
    if (StubRoutines::dtan() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dtan);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dtan());
    }
    break;
  case Interpreter::java_lang_math_log :
    if (StubRoutines::dlog() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dlog);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dlog());
    }
    break;
  case Interpreter::java_lang_math_log10 :
    if (StubRoutines::dlog10() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dlog10);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dlog10());
    }
    break;
  case Interpreter::java_lang_math_exp :
    if (StubRoutines::dexp() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dexp);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dexp());
    }
    break;
  case Interpreter::java_lang_math_pow :
    if (StubRoutines::dpow() == NULL) {
      fn = CAST_FROM_FN_PTR(address, SharedRuntime::dpow);
    } else {
      fn = CAST_FROM_FN_PTR(address, StubRoutines::dpow());
    }
    break;
  default:
    ShouldNotReachHere();
    fn = NULL;  // unreachable
  }
  __ mov(rscratch1, fn);
  __ blr(rscratch1);
}

// Abstract method entry
// Attempt to execute abstract method. Throw exception
address TemplateInterpreterGenerator::generate_abstract_entry(void) {
  // rmethod: Method*
  // r13: sender SP

  address entry_point = __ pc();

  // abstract method entry

  //  pop return address, reset last_sp to NULL
  __ empty_expression_stack();
  __ restore_bcp();      // bcp must be correct for exception handler   (was destroyed)
  __ restore_locals();   // make sure locals pointer is correct as well (was destroyed)

  // throw exception
  __ call_VM(noreg, CAST_FROM_FN_PTR(address,
                                     InterpreterRuntime::throw_AbstractMethodErrorWithMethod),
                                     rmethod);
  // the call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();

  return entry_point;
}

address TemplateInterpreterGenerator::generate_StackOverflowError_handler() {
  address entry = __ pc();

#ifdef ASSERT
  {
    Label L;
    __ ldr(rscratch1, Address(rfp,
                       frame::interpreter_frame_monitor_block_top_offset *
                       wordSize));
    __ mov(rscratch2, sp);
    __ cmp(rscratch1, rscratch2); // maximal rsp for current rfp (stack
                           // grows negative)
    __ br(Assembler::HS, L); // check if frame is complete
    __ stop ("interpreter frame not set up");
    __ bind(L);
  }
#endif // ASSERT
  // Restore bcp under the assumption that the current frame is still
  // interpreted
  __ restore_bcp();

  // expression stack must be empty before entering the VM if an
  // exception happened
  __ empty_expression_stack();
  // throw exception
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::throw_StackOverflowError));
  return entry;
}

address TemplateInterpreterGenerator::generate_ArrayIndexOutOfBounds_handler() {
  address entry = __ pc();
  // expression stack must be empty before entering the VM if an
  // exception happened
  __ empty_expression_stack();
  // setup parameters

  // ??? convention: expect aberrant index in register r1
  __ movw(c_rarg2, r1);
  // ??? convention: expect array in register r3
  __ mov(c_rarg1, r3);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::
                              throw_ArrayIndexOutOfBoundsException),
             c_rarg1, c_rarg2);
  return entry;
}

address TemplateInterpreterGenerator::generate_ClassCastException_handler() {
  address entry = __ pc();

  // object is at TOS
  __ pop(c_rarg1);

  // expression stack must be empty before entering the VM if an
  // exception happened
  __ empty_expression_stack();

  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::
                              throw_ClassCastException),
             c_rarg1);
  return entry;
}

address TemplateInterpreterGenerator::generate_exception_handler_common(
        const char* name, const char* message, bool pass_oop) {
  assert(!pass_oop || message == NULL, "either oop or message but not both");
  address entry = __ pc();
  if (pass_oop) {
    // object is at TOS
    __ pop(c_rarg2);
  }
  // expression stack must be empty before entering the VM if an
  // exception happened
  __ empty_expression_stack();
  // setup parameters
  __ lea(c_rarg1, Address((address)name));
  if (pass_oop) {
    __ call_VM(r0, CAST_FROM_FN_PTR(address,
                                    InterpreterRuntime::
                                    create_klass_exception),
               c_rarg1, c_rarg2);
  } else {
    // kind of lame ExternalAddress can't take NULL because
    // external_word_Relocation will assert.
    if (message != NULL) {
      __ lea(c_rarg2, Address((address)message));
    } else {
      __ mov(c_rarg2, NULL_WORD);
    }
    __ call_VM(r0,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::create_exception),
               c_rarg1, c_rarg2);
  }
  // throw exception
  __ b(address(Interpreter::throw_exception_entry()));
  return entry;
}

address TemplateInterpreterGenerator::generate_return_entry_for(TosState state, int step, size_t index_size) {
  address entry = __ pc();

  // Restore stack bottom in case i2c adjusted stack
  __ ldr(esp, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));
  // and NULL it as marker that esp is now tos until next java call
  __ str(zr, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));
  __ restore_bcp();
  __ restore_locals();
  __ restore_constant_pool_cache();
  __ get_method(rmethod);

  if (state == atos) {
    Register obj = r0;
    Register mdp = r1;
    Register tmp = r2;
    __ profile_return_type(mdp, obj, tmp);
  }

  // Pop N words from the stack
  __ get_cache_and_index_at_bcp(r1, r2, 1, index_size);
  __ ldr(r1, Address(r1, ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::flags_offset()));
  __ andr(r1, r1, ConstantPoolCacheEntry::parameter_size_mask);

  __ add(esp, esp, r1, Assembler::LSL, 3);

  // Restore machine SP
  __ ldr(rscratch1, Address(rmethod, Method::const_offset()));
  __ ldrh(rscratch1, Address(rscratch1, ConstMethod::max_stack_offset()));
  __ add(rscratch1, rscratch1, frame::interpreter_frame_monitor_size() + 2);
  __ ldr(rscratch2,
         Address(rfp, frame::interpreter_frame_initial_sp_offset * wordSize));
  __ sub(rscratch1, rscratch2, rscratch1, ext::uxtw, 3);
  __ andr(sp, rscratch1, -16);

 __ check_and_handle_popframe(rthread);
 __ check_and_handle_earlyret(rthread);

  __ get_dispatch();
  __ dispatch_next(state, step);

  return entry;
}

address TemplateInterpreterGenerator::generate_deopt_entry_for(TosState state,
                                                               int step,
                                                               address continuation) {
  address entry = __ pc();
  __ restore_bcp();
  __ restore_locals();
  __ restore_constant_pool_cache();
  __ get_method(rmethod);
  __ get_dispatch();

  // Calculate stack limit
  __ ldr(rscratch1, Address(rmethod, Method::const_offset()));
  __ ldrh(rscratch1, Address(rscratch1, ConstMethod::max_stack_offset()));
  __ add(rscratch1, rscratch1, frame::interpreter_frame_monitor_size() + 2);
  __ ldr(rscratch2,
         Address(rfp, frame::interpreter_frame_initial_sp_offset * wordSize));
  __ sub(rscratch1, rscratch2, rscratch1, ext::uxtx, 3);
  __ andr(sp, rscratch1, -16);

  // Restore expression stack pointer
  __ ldr(esp, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));
  // NULL last_sp until next java call
  __ str(zr, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));

#if INCLUDE_JVMCI
  // Check if we need to take lock at entry of synchronized method.  This can
  // only occur on method entry so emit it only for vtos with step 0.
  if (EnableJVMCI && state == vtos && step == 0) {
    Label L;
    __ ldrb(rscratch1, Address(rthread, JavaThread::pending_monitorenter_offset()));
    __ cbz(rscratch1, L);
    // Clear flag.
    __ strb(zr, Address(rthread, JavaThread::pending_monitorenter_offset()));
    // Take lock.
    lock_method();
    __ bind(L);
  } else {
#ifdef ASSERT
    if (EnableJVMCI) {
      Label L;
      __ ldrb(rscratch1, Address(rthread, JavaThread::pending_monitorenter_offset()));
      __ cbz(rscratch1, L);
      __ stop("unexpected pending monitor in deopt entry");
      __ bind(L);
    }
#endif
  }
#endif
  // handle exceptions
  {
    Label L;
    __ ldr(rscratch1, Address(rthread, Thread::pending_exception_offset()));
    __ cbz(rscratch1, L);
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::throw_pending_exception));
    __ should_not_reach_here();
    __ bind(L);
  }

  if (continuation == NULL) {
    __ dispatch_next(state, step);
  } else {
    __ jump_to_entry(continuation);
  }
  return entry;
}

address TemplateInterpreterGenerator::generate_result_handler_for(
        BasicType type) {
    address entry = __ pc();
  switch (type) {
  case T_BOOLEAN: __ c2bool(r0);         break;
  case T_CHAR   : __ uxth(r0, r0);       break;
  case T_BYTE   : __ sxtb(r0, r0);        break;
  case T_SHORT  : __ sxth(r0, r0);        break;
  case T_INT    : __ uxtw(r0, r0);        break;  // FIXME: We almost certainly don't need this
  case T_LONG   : /* nothing to do */        break;
  case T_VOID   : /* nothing to do */        break;
  case T_FLOAT  : /* nothing to do */        break;
  case T_DOUBLE : /* nothing to do */        break;
  case T_OBJECT :
    // retrieve result from frame
    __ ldr(r0, Address(rfp, frame::interpreter_frame_oop_temp_offset*wordSize));
    // and verify it
    __ verify_oop(r0);
    break;
  default       : ShouldNotReachHere();
  }
  __ ret(lr);                                  // return from result handler
  return entry;
}

address TemplateInterpreterGenerator::generate_safept_entry_for(
        TosState state,
        address runtime_entry) {
  address entry = __ pc();
  __ push(state);
  __ call_VM(noreg, runtime_entry);
  __ membar(Assembler::AnyAny);
  __ dispatch_via(vtos, Interpreter::_normal_table.table_for(vtos));
  return entry;
}

// Helpers for commoning out cases in the various type of method entries.
//


// increment invocation count & check for overflow
//
// Note: checking for negative value instead of overflow
//       so we have a 'sticky' overflow test
//
// rmethod: method
//
void TemplateInterpreterGenerator::generate_counter_incr(Label* overflow) {
  Label done;
  // Note: In tiered we increment either counters in Method* or in MDO depending if we're profiling or not.
  int increment = InvocationCounter::count_increment;
  Label no_mdo;
  if (ProfileInterpreter) {
    // Are we profiling?
    __ ldr(r0, Address(rmethod, Method::method_data_offset()));
    __ cbz(r0, no_mdo);
    // Increment counter in the MDO
    const Address mdo_invocation_counter(r0, in_bytes(MethodData::invocation_counter_offset()) +
                                              in_bytes(InvocationCounter::counter_offset()));
    const Address mask(r0, in_bytes(MethodData::invoke_mask_offset()));
    __ increment_mask_and_jump(mdo_invocation_counter, increment, mask, rscratch1, rscratch2, false, Assembler::EQ, overflow);
    __ b(done);
  }
  __ bind(no_mdo);
  // Increment counter in MethodCounters
  const Address invocation_counter(rscratch2,
                MethodCounters::invocation_counter_offset() +
                InvocationCounter::counter_offset());
  __ get_method_counters(rmethod, rscratch2, done);
  const Address mask(rscratch2, in_bytes(MethodCounters::invoke_mask_offset()));
  __ increment_mask_and_jump(invocation_counter, increment, mask, rscratch1, r1, false, Assembler::EQ, overflow);
  __ bind(done);
}

void TemplateInterpreterGenerator::generate_counter_overflow(Label& do_continue) {

  // Asm interpreter on entry
  // On return (i.e. jump to entry_point) [ back to invocation of interpreter ]
  // Everything as it was on entry

  // InterpreterRuntime::frequency_counter_overflow takes two
  // arguments, the first (thread) is passed by call_VM, the second
  // indicates if the counter overflow occurs at a backwards branch
  // (NULL bcp).  We pass zero for it.  The call returns the address
  // of the verified entry point for the method or NULL if the
  // compilation did not complete (either went background or bailed
  // out).
  __ mov(c_rarg1, 0);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::frequency_counter_overflow),
             c_rarg1);

  __ b(do_continue);
}

// See if we've got enough room on the stack for locals plus overhead
// below JavaThread::stack_overflow_limit(). If not, throw a StackOverflowError
// without going through the signal handler, i.e., reserved and yellow zones
// will not be made usable. The shadow zone must suffice to handle the
// overflow.
// The expression stack grows down incrementally, so the normal guard
// page mechanism will work for that.
//
// NOTE: Since the additional locals are also always pushed (wasn't
// obvious in generate_method_entry) so the guard should work for them
// too.
//
// Args:
//      r3: number of additional locals this frame needs (what we must check)
//      rmethod: Method*
//
// Kills:
//      r0
void TemplateInterpreterGenerator::generate_stack_overflow_check(void) {

  // monitor entry size: see picture of stack set
  // (generate_method_entry) and frame_amd64.hpp
  const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;

  // total overhead size: entry_size + (saved rbp through expr stack
  // bottom).  be sure to change this if you add/subtract anything
  // to/from the overhead area
  const int overhead_size =
    -(frame::interpreter_frame_initial_sp_offset * wordSize) + entry_size;

  const int page_size = os::vm_page_size();

  Label after_frame_check;

  // see if the frame is greater than one page in size. If so,
  // then we need to verify there is enough stack space remaining
  // for the additional locals.
  //
  // Note that we use SUBS rather than CMP here because the immediate
  // field of this instruction may overflow.  SUBS can cope with this
  // because it is a macro that will expand to some number of MOV
  // instructions and a register operation.
  __ subs(rscratch1, r3, (page_size - overhead_size) / Interpreter::stackElementSize);
  __ br(Assembler::LS, after_frame_check);

  // compute rsp as if this were going to be the last frame on
  // the stack before the red zone

  // locals + overhead, in bytes
  __ mov(r0, overhead_size);
  __ add(r0, r0, r3, Assembler::LSL, Interpreter::logStackElementSize);  // 2 slots per parameter.

  const Address stack_limit(rthread, JavaThread::stack_overflow_limit_offset());
  __ ldr(rscratch1, stack_limit);

#ifdef ASSERT
  Label limit_okay;
  // Verify that thread stack limit is non-zero.
  __ cbnz(rscratch1, limit_okay);
  __ stop("stack overflow limit is zero");
  __ bind(limit_okay);
#endif

  // Add stack limit to locals.
  __ add(r0, r0, rscratch1);

  // Check against the current stack bottom.
  __ cmp(sp, r0);
  __ br(Assembler::HI, after_frame_check);

  // Remove the incoming args, peeling the machine SP back to where it
  // was in the caller.  This is not strictly necessary, but unless we
  // do so the stack frame may have a garbage FP; this ensures a
  // correct call stack that we can always unwind.  The ANDR should be
  // unnecessary because the sender SP in r13 is always aligned, but
  // it doesn't hurt.
  __ andr(sp, r13, -16);

  // Note: the restored frame is not necessarily interpreted.
  // Use the shared runtime version of the StackOverflowError.
  assert(StubRoutines::throw_StackOverflowError_entry() != NULL, "stub not yet generated");
  __ far_jump(RuntimeAddress(StubRoutines::throw_StackOverflowError_entry()));

  // all done with frame size check
  __ bind(after_frame_check);
}

// Allocate monitor and lock method (asm interpreter)
//
// Args:
//      rmethod: Method*
//      rlocals: locals
//
// Kills:
//      r0
//      c_rarg0, c_rarg1, c_rarg2, c_rarg3, ...(param regs)
//      rscratch1, rscratch2 (scratch regs)
void TemplateInterpreterGenerator::lock_method() {
  // synchronize method
  const Address access_flags(rmethod, Method::access_flags_offset());
  const Address monitor_block_top(
        rfp,
        frame::interpreter_frame_monitor_block_top_offset * wordSize);
  const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;

#ifdef ASSERT
  {
    Label L;
    __ ldrw(r0, access_flags);
    __ tst(r0, JVM_ACC_SYNCHRONIZED);
    __ br(Assembler::NE, L);
    __ stop("method doesn't need synchronization");
    __ bind(L);
  }
#endif // ASSERT

  // get synchronization object
  {
    Label done;
    __ ldrw(r0, access_flags);
    __ tst(r0, JVM_ACC_STATIC);
    // get receiver (assume this is frequent case)
    __ ldr(r0, Address(rlocals, Interpreter::local_offset_in_bytes(0)));
    __ br(Assembler::EQ, done);
    __ load_mirror(r0, rmethod);

#ifdef ASSERT
    {
      Label L;
      __ cbnz(r0, L);
      __ stop("synchronization object is NULL");
      __ bind(L);
    }
#endif // ASSERT

    __ bind(done);
  }

  // add space for monitor & lock
  __ sub(sp, sp, entry_size); // add space for a monitor entry
  __ sub(esp, esp, entry_size);
  __ mov(rscratch1, esp);
  __ str(rscratch1, monitor_block_top);  // set new monitor block top
  // store object
  __ str(r0, Address(esp, BasicObjectLock::obj_offset_in_bytes()));
  __ mov(c_rarg1, esp); // object address
  __ lock_object(c_rarg1);
}

// Generate a fixed interpreter frame. This is identical setup for
// interpreted methods and for native methods hence the shared code.
//
// Args:
//      lr: return address
//      rmethod: Method*
//      rlocals: pointer to locals
//      rcpool: cp cache
//      stack_pointer: previous sp
void TemplateInterpreterGenerator::generate_fixed_frame(bool native_call) {
  // initialize fixed part of activation frame
  if (native_call) {
    __ sub(esp, sp, 14 *  wordSize);
    __ mov(rbcp, zr);
    __ stp(esp, zr, Address(__ pre(sp, -14 * wordSize)));
    // add 2 zero-initialized slots for native calls
    __ stp(zr, zr, Address(sp, 12 * wordSize));
  } else {
    __ sub(esp, sp, 12 *  wordSize);
    __ ldr(rscratch1, Address(rmethod, Method::const_offset()));      // get ConstMethod
    __ add(rbcp, rscratch1, in_bytes(ConstMethod::codes_offset())); // get codebase
    __ stp(esp, rbcp, Address(__ pre(sp, -12 * wordSize)));
  }

  if (ProfileInterpreter) {
    Label method_data_continue;
    __ ldr(rscratch1, Address(rmethod, Method::method_data_offset()));
    __ cbz(rscratch1, method_data_continue);
    __ lea(rscratch1, Address(rscratch1, in_bytes(MethodData::data_offset())));
    __ bind(method_data_continue);
    __ stp(rscratch1, rmethod, Address(sp, 6 * wordSize));  // save Method* and mdp (method data pointer)
  } else {
    __ stp(zr, rmethod, Address(sp, 6 * wordSize));        // save Method* (no mdp)
  }

  // Get mirror and store it in the frame as GC root for this Method*
  __ load_mirror(r10, rmethod);
  __ stp(r10, zr, Address(sp, 4 * wordSize));

  __ ldr(rcpool, Address(rmethod, Method::const_offset()));
  __ ldr(rcpool, Address(rcpool, ConstMethod::constants_offset()));
  __ ldr(rcpool, Address(rcpool, ConstantPool::cache_offset_in_bytes()));
  __ stp(rlocals, rcpool, Address(sp, 2 * wordSize));

  __ stp(rfp, lr, Address(sp, 10 * wordSize));
  __ lea(rfp, Address(sp, 10 * wordSize));

  // set sender sp
  // leave last_sp as null
  __ stp(zr, r13, Address(sp, 8 * wordSize));

  // Move SP out of the way
  if (! native_call) {
    __ ldr(rscratch1, Address(rmethod, Method::const_offset()));
    __ ldrh(rscratch1, Address(rscratch1, ConstMethod::max_stack_offset()));
    __ add(rscratch1, rscratch1, frame::interpreter_frame_monitor_size() + 2);
    __ sub(rscratch1, sp, rscratch1, ext::uxtw, 3);
    __ andr(sp, rscratch1, -16);
  }
}

// End of helpers

// Various method entries
//------------------------------------------------------------------------------------------------------------------------
//
//

// Method entry for java.lang.ref.Reference.get.
address TemplateInterpreterGenerator::generate_Reference_get_entry(void) {
  // Code: _aload_0, _getfield, _areturn
  // parameter size = 1
  //
  // The code that gets generated by this routine is split into 2 parts:
  //    1. The "intrinsified" code for G1 (or any SATB based GC),
  //    2. The slow path - which is an expansion of the regular method entry.
  //
  // Notes:-
  // * In the G1 code we do not check whether we need to block for
  //   a safepoint. If G1 is enabled then we must execute the specialized
  //   code for Reference.get (except when the Reference object is null)
  //   so that we can log the value in the referent field with an SATB
  //   update buffer.
  //   If the code for the getfield template is modified so that the
  //   G1 pre-barrier code is executed when the current method is
  //   Reference.get() then going through the normal method entry
  //   will be fine.
  // * The G1 code can, however, check the receiver object (the instance
  //   of java.lang.Reference) and jump to the slow path if null. If the
  //   Reference object is null then we obviously cannot fetch the referent
  //   and so we don't need to call the G1 pre-barrier. Thus we can use the
  //   regular method entry code to generate the NPE.
  //
  // This code is based on generate_accessor_entry.
  //
  // rmethod: Method*
  // r13: senderSP must preserve for slow path, set SP to it on fast path

  // LR is live.  It must be saved around calls.

  address entry = __ pc();

  const int referent_offset = java_lang_ref_Reference::referent_offset();

  Label slow_path;
  const Register local_0 = c_rarg0;
  // Check if local 0 != NULL
  // If the receiver is null then it is OK to jump to the slow path.
  __ ldr(local_0, Address(esp, 0));
  __ cbz(local_0, slow_path);

  __ mov(r19, r13);   // Move senderSP to a callee-saved register

  // Load the value of the referent field.
  const Address field_address(local_0, referent_offset);
  BarrierSetAssembler *bs = BarrierSet::barrier_set()->barrier_set_assembler();
  bs->load_at(_masm, IN_HEAP | ON_WEAK_OOP_REF, T_OBJECT, local_0, field_address, /*tmp1*/ rscratch2, /*tmp2*/ rscratch1);

  // areturn
  __ andr(sp, r19, -16);  // done with stack
  __ ret(lr);

  // generate a vanilla interpreter entry as the slow path
  __ bind(slow_path);
  __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::zerolocals));
  return entry;

}

/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.update(int crc, int b)
 */
address TemplateInterpreterGenerator::generate_CRC32_update_entry() {
  if (UseCRC32Intrinsics) {
    address entry = __ pc();

    // rmethod: Method*
    // r13: senderSP must preserved for slow path
    // esp: args

    Label slow_path;
    // If we need a safepoint check, generate full interpreter entry.
    __ safepoint_poll(slow_path, false /* at_return */, false /* acquire */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters
    const Register crc = c_rarg0;  // crc
    const Register val = c_rarg1;  // source java byte value
    const Register tbl = c_rarg2;  // scratch

    // Arguments are reversed on java expression stack
    __ ldrw(val, Address(esp, 0));              // byte value
    __ ldrw(crc, Address(esp, wordSize));       // Initial CRC

    uint64_t offset;
    __ adrp(tbl, ExternalAddress(StubRoutines::crc_table_addr()), offset);
    __ add(tbl, tbl, offset);

    __ mvnw(crc, crc); // ~crc
    __ update_byte_crc32(crc, val, tbl);
    __ mvnw(crc, crc); // ~crc

    // result in c_rarg0

    __ andr(sp, r13, -16);
    __ ret(lr);

    // generate a vanilla native entry as the slow path
    __ bind(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native));
    return entry;
  }
  return NULL;
}

/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.updateBytes(int crc, byte[] b, int off, int len)
 *   int java.util.zip.CRC32.updateByteBuffer(int crc, long buf, int off, int len)
 */
address TemplateInterpreterGenerator::generate_CRC32_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32Intrinsics) {
    address entry = __ pc();

    // rmethod,: Method*
    // r13: senderSP must preserved for slow path

    Label slow_path;
    // If we need a safepoint check, generate full interpreter entry.
    __ safepoint_poll(slow_path, false /* at_return */, false /* acquire */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters
    const Register crc = c_rarg0;  // crc
    const Register buf = c_rarg1;  // source java byte array address
    const Register len = c_rarg2;  // length
    const Register off = len;      // offset (never overlaps with 'len')

    // Arguments are reversed on java expression stack
    // Calculate address of start element
    if (kind == Interpreter::java_util_zip_CRC32_updateByteBuffer) {
      __ ldr(buf, Address(esp, 2*wordSize)); // long buf
      __ ldrw(off, Address(esp, wordSize)); // offset
      __ add(buf, buf, off); // + offset
      __ ldrw(crc,   Address(esp, 4*wordSize)); // Initial CRC
    } else {
      __ ldr(buf, Address(esp, 2*wordSize)); // byte[] array
      __ add(buf, buf, arrayOopDesc::base_offset_in_bytes(T_BYTE)); // + header size
      __ ldrw(off, Address(esp, wordSize)); // offset
      __ add(buf, buf, off); // + offset
      __ ldrw(crc,   Address(esp, 3*wordSize)); // Initial CRC
    }
    // Can now load 'len' since we're finished with 'off'
    __ ldrw(len, Address(esp, 0x0)); // Length

    __ andr(sp, r13, -16); // Restore the caller's SP

    // We are frameless so we can just jump to the stub.
    __ b(CAST_FROM_FN_PTR(address, StubRoutines::updateBytesCRC32()));

    // generate a vanilla native entry as the slow path
    __ bind(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native));
    return entry;
  }
  return NULL;
}

/**
 * Method entry for intrinsic-candidate (non-native) methods:
 *   int java.util.zip.CRC32C.updateBytes(int crc, byte[] b, int off, int end)
 *   int java.util.zip.CRC32C.updateDirectByteBuffer(int crc, long buf, int off, int end)
 * Unlike CRC32, CRC32C does not have any methods marked as native
 * CRC32C also uses an "end" variable instead of the length variable CRC32 uses
 */
address TemplateInterpreterGenerator::generate_CRC32C_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32CIntrinsics) {
    address entry = __ pc();

    // Prepare jump to stub using parameters from the stack
    const Register crc = c_rarg0; // initial crc
    const Register buf = c_rarg1; // source java byte array address
    const Register len = c_rarg2; // len argument to the kernel

    const Register end = len; // index of last element to process
    const Register off = crc; // offset

    __ ldrw(end, Address(esp)); // int end
    __ ldrw(off, Address(esp, wordSize)); // int offset
    __ sub(len, end, off);
    __ ldr(buf, Address(esp, 2*wordSize)); // byte[] buf | long buf
    __ add(buf, buf, off); // + offset
    if (kind == Interpreter::java_util_zip_CRC32C_updateDirectByteBuffer) {
      __ ldrw(crc, Address(esp, 4*wordSize)); // long crc
    } else {
      __ add(buf, buf, arrayOopDesc::base_offset_in_bytes(T_BYTE)); // + header size
      __ ldrw(crc, Address(esp, 3*wordSize)); // long crc
    }

    __ andr(sp, r13, -16); // Restore the caller's SP

    // Jump to the stub.
    __ b(CAST_FROM_FN_PTR(address, StubRoutines::updateBytesCRC32C()));

    return entry;
  }
  return NULL;
}

void TemplateInterpreterGenerator::bang_stack_shadow_pages(bool native_call) {
  // Bang each page in the shadow zone. We can't assume it's been done for
  // an interpreter frame with greater than a page of locals, so each page
  // needs to be checked.  Only true for non-native.
  const int n_shadow_pages = (int)(StackOverflow::stack_shadow_zone_size() / os::vm_page_size());
  const int start_page = native_call ? n_shadow_pages : 1;
  const int page_size = os::vm_page_size();
  for (int pages = start_page; pages <= n_shadow_pages ; pages++) {
    __ sub(rscratch2, sp, pages*page_size);
    __ str(zr, Address(rscratch2));
  }
}


// Interpreter stub for calling a native method. (asm interpreter)
// This sets up a somewhat different looking stack for calling the
// native method than the typical interpreter frame setup.
address TemplateInterpreterGenerator::generate_native_entry(bool synchronized) {
  // determine code generation flags
  bool inc_counter  = UseCompiler || CountCompiledCalls || LogTouchedMethods;

  // r1: Method*
  // rscratch1: sender sp

  address entry_point = __ pc();

  const Address constMethod       (rmethod, Method::const_offset());
  const Address access_flags      (rmethod, Method::access_flags_offset());
  const Address size_of_parameters(r2, ConstMethod::
                                       size_of_parameters_offset());

  // get parameter size (always needed)
  __ ldr(r2, constMethod);
  __ load_unsigned_short(r2, size_of_parameters);

  // Native calls don't need the stack size check since they have no
  // expression stack and the arguments are already on the stack and
  // we only add a handful of words to the stack.

  // rmethod: Method*
  // r2: size of parameters
  // rscratch1: sender sp

  // for natives the size of locals is zero

  // compute beginning of parameters (rlocals)
  __ add(rlocals, esp, r2, ext::uxtx, 3);
  __ add(rlocals, rlocals, -wordSize);

  // Pull SP back to minimum size: this avoids holes in the stack
  __ andr(sp, esp, -16);

  // initialize fixed part of activation frame
  generate_fixed_frame(true);

  // make sure method is native & not abstract
#ifdef ASSERT
  __ ldrw(r0, access_flags);
  {
    Label L;
    __ tst(r0, JVM_ACC_NATIVE);
    __ br(Assembler::NE, L);
    __ stop("tried to execute non-native method as native");
    __ bind(L);
  }
  {
    Label L;
    __ tst(r0, JVM_ACC_ABSTRACT);
    __ br(Assembler::EQ, L);
    __ stop("tried to execute abstract method in interpreter");
    __ bind(L);
  }
#endif

  // Since at this point in the method invocation the exception
  // handler would try to exit the monitor of synchronized methods
  // which hasn't been entered yet, we set the thread local variable
  // _do_not_unlock_if_synchronized to true. The remove_activation
  // will check this flag.

   const Address do_not_unlock_if_synchronized(rthread,
        in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()));
  __ mov(rscratch2, true);
  __ strb(rscratch2, do_not_unlock_if_synchronized);

  // increment invocation count & check for overflow
  Label invocation_counter_overflow;
  if (inc_counter) {
    generate_counter_incr(&invocation_counter_overflow);
  }

  Label continue_after_compile;
  __ bind(continue_after_compile);

  bang_stack_shadow_pages(true);

  // reset the _do_not_unlock_if_synchronized flag
  __ strb(zr, do_not_unlock_if_synchronized);

  // check for synchronized methods
  // Must happen AFTER invocation_counter check and stack overflow check,
  // so method is not locked if overflows.
  if (synchronized) {
    lock_method();
  } else {
    // no synchronization necessary
#ifdef ASSERT
    {
      Label L;
      __ ldrw(r0, access_flags);
      __ tst(r0, JVM_ACC_SYNCHRONIZED);
      __ br(Assembler::EQ, L);
      __ stop("method needs synchronization");
      __ bind(L);
    }
#endif
  }

  // start execution
#ifdef ASSERT
  {
    Label L;
    const Address monitor_block_top(rfp,
                 frame::interpreter_frame_monitor_block_top_offset * wordSize);
    __ ldr(rscratch1, monitor_block_top);
    __ cmp(esp, rscratch1);
    __ br(Assembler::EQ, L);
    __ stop("broken stack frame setup in interpreter");
    __ bind(L);
  }
#endif

  // jvmti support
  __ notify_method_entry();

  // work registers
  const Register t = r17;
  const Register result_handler = r19;

  // allocate space for parameters
  __ ldr(t, Address(rmethod, Method::const_offset()));
  __ load_unsigned_short(t, Address(t, ConstMethod::size_of_parameters_offset()));

  __ sub(rscratch1, esp, t, ext::uxtx, Interpreter::logStackElementSize);
  __ andr(sp, rscratch1, -16);
  __ mov(esp, rscratch1);

  // get signature handler
  {
    Label L;
    __ ldr(t, Address(rmethod, Method::signature_handler_offset()));
    __ cbnz(t, L);
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::prepare_native_call),
               rmethod);
    __ ldr(t, Address(rmethod, Method::signature_handler_offset()));
    __ bind(L);
  }

  // call signature handler
  assert(InterpreterRuntime::SignatureHandlerGenerator::from() == rlocals,
         "adjust this code");
  assert(InterpreterRuntime::SignatureHandlerGenerator::to() == sp,
         "adjust this code");
  assert(InterpreterRuntime::SignatureHandlerGenerator::temp() == rscratch1,
          "adjust this code");

  // The generated handlers do not touch rmethod (the method).
  // However, large signatures cannot be cached and are generated
  // each time here.  The slow-path generator can do a GC on return,
  // so we must reload it after the call.
  __ blr(t);
  __ get_method(rmethod);        // slow path can do a GC, reload rmethod


  // result handler is in r0
  // set result handler
  __ mov(result_handler, r0);
  // pass mirror handle if static call
  {
    Label L;
    __ ldrw(t, Address(rmethod, Method::access_flags_offset()));
    __ tbz(t, exact_log2(JVM_ACC_STATIC), L);
    // get mirror
    __ load_mirror(t, rmethod);
    // copy mirror into activation frame
    __ str(t, Address(rfp, frame::interpreter_frame_oop_temp_offset * wordSize));
    // pass handle to mirror
    __ add(c_rarg1, rfp, frame::interpreter_frame_oop_temp_offset * wordSize);
    __ bind(L);
  }

  // get native function entry point in r10
  {
    Label L;
    __ ldr(r10, Address(rmethod, Method::native_function_offset()));
    address unsatisfied = (SharedRuntime::native_method_throw_unsatisfied_link_error_entry());
    __ mov(rscratch2, unsatisfied);
    __ ldr(rscratch2, rscratch2);
    __ cmp(r10, rscratch2);
    __ br(Assembler::NE, L);
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::prepare_native_call),
               rmethod);
    __ get_method(rmethod);
    __ ldr(r10, Address(rmethod, Method::native_function_offset()));
    __ bind(L);
  }

  // pass JNIEnv
  __ add(c_rarg0, rthread, in_bytes(JavaThread::jni_environment_offset()));

  // Set the last Java PC in the frame anchor to be the return address from
  // the call to the native method: this will allow the debugger to
  // generate an accurate stack trace.
  Label native_return;
  __ set_last_Java_frame(esp, rfp, native_return, rscratch1);

  // change thread state
#ifdef ASSERT
  {
    Label L;
    __ ldrw(t, Address(rthread, JavaThread::thread_state_offset()));
    __ cmp(t, (u1)_thread_in_Java);
    __ br(Assembler::EQ, L);
    __ stop("Wrong thread state in native stub");
    __ bind(L);
  }
#endif

  // Change state to native
  __ mov(rscratch1, _thread_in_native);
  __ lea(rscratch2, Address(rthread, JavaThread::thread_state_offset()));
  __ stlrw(rscratch1, rscratch2);

  // Call the native method.
  __ blr(r10);
  __ bind(native_return);
  __ get_method(rmethod);
  // result potentially in r0 or v0

  // make room for the pushes we're about to do
  __ sub(rscratch1, esp, 4 * wordSize);
  __ andr(sp, rscratch1, -16);

  // NOTE: The order of these pushes is known to frame::interpreter_frame_result
  // in order to extract the result of a method call. If the order of these
  // pushes change or anything else is added to the stack then the code in
  // interpreter_frame_result must also change.
  __ push(dtos);
  __ push(ltos);

  __ verify_sve_vector_length();

  // change thread state
  __ mov(rscratch1, _thread_in_native_trans);
  __ lea(rscratch2, Address(rthread, JavaThread::thread_state_offset()));
  __ stlrw(rscratch1, rscratch2);

  // Force this write out before the read below
  __ dmb(Assembler::ISH);

  // check for safepoint operation in progress and/or pending suspend requests
  {
    Label L, Continue;

    // We need an acquire here to ensure that any subsequent load of the
    // global SafepointSynchronize::_state flag is ordered after this load
    // of the thread-local polling word.  We don't want this poll to
    // return false (i.e. not safepointing) and a later poll of the global
    // SafepointSynchronize::_state spuriously to return true.
    //
    // This is to avoid a race when we're in a native->Java transition
    // racing the code which wakes up from a safepoint.
    __ safepoint_poll(L, true /* at_return */, true /* acquire */, false /* in_nmethod */);
    __ ldrw(rscratch2, Address(rthread, JavaThread::suspend_flags_offset()));
    __ cbz(rscratch2, Continue);
    __ bind(L);

    // Don't use call_VM as it will see a possible pending exception
    // and forward it and never return here preventing us from
    // clearing _last_native_pc down below. So we do a runtime call by
    // hand.
    //
    __ mov(c_rarg0, rthread);
    __ mov(rscratch2, CAST_FROM_FN_PTR(address, JavaThread::check_special_condition_for_native_trans));
    __ blr(rscratch2);
    __ get_method(rmethod);
    __ reinit_heapbase();
    __ bind(Continue);
  }

  // change thread state
  __ mov(rscratch1, _thread_in_Java);
  __ lea(rscratch2, Address(rthread, JavaThread::thread_state_offset()));
  __ stlrw(rscratch1, rscratch2);

  // reset_last_Java_frame
  __ reset_last_Java_frame(true);

  if (CheckJNICalls) {
    // clear_pending_jni_exception_check
    __ str(zr, Address(rthread, JavaThread::pending_jni_exception_check_fn_offset()));
  }

  // reset handle block
  __ ldr(t, Address(rthread, JavaThread::active_handles_offset()));
  __ str(zr, Address(t, JNIHandleBlock::top_offset_in_bytes()));

  // If result is an oop unbox and store it in frame where gc will see it
  // and result handler will pick it up

  {
    Label no_oop;
    __ adr(t, ExternalAddress(AbstractInterpreter::result_handler(T_OBJECT)));
    __ cmp(t, result_handler);
    __ br(Assembler::NE, no_oop);
    // Unbox oop result, e.g. JNIHandles::resolve result.
    __ pop(ltos);
    __ resolve_jobject(r0, rthread, t);
    __ str(r0, Address(rfp, frame::interpreter_frame_oop_temp_offset*wordSize));
    // keep stack depth as expected by pushing oop which will eventually be discarded
    __ push(ltos);
    __ bind(no_oop);
  }

  {
    Label no_reguard;
    __ lea(rscratch1, Address(rthread, in_bytes(JavaThread::stack_guard_state_offset())));
    __ ldrw(rscratch1, Address(rscratch1));
    __ cmp(rscratch1, (u1)StackOverflow::stack_guard_yellow_reserved_disabled);
    __ br(Assembler::NE, no_reguard);

    __ pusha(); // XXX only save smashed registers
    __ mov(c_rarg0, rthread);
    __ mov(rscratch2, CAST_FROM_FN_PTR(address, SharedRuntime::reguard_yellow_pages));
    __ blr(rscratch2);
    __ popa(); // XXX only restore smashed registers
    __ bind(no_reguard);
  }

  // The method register is junk from after the thread_in_native transition
  // until here.  Also can't call_VM until the bcp has been
  // restored.  Need bcp for throwing exception below so get it now.
  __ get_method(rmethod);

  // restore bcp to have legal interpreter frame, i.e., bci == 0 <=>
  // rbcp == code_base()
  __ ldr(rbcp, Address(rmethod, Method::const_offset()));   // get ConstMethod*
  __ add(rbcp, rbcp, in_bytes(ConstMethod::codes_offset()));          // get codebase
  // handle exceptions (exception handling will handle unlocking!)
  {
    Label L;
    __ ldr(rscratch1, Address(rthread, Thread::pending_exception_offset()));
    __ cbz(rscratch1, L);
    // Note: At some point we may want to unify this with the code
    // used in call_VM_base(); i.e., we should use the
    // StubRoutines::forward_exception code. For now this doesn't work
    // here because the rsp is not correctly set at this point.
    __ MacroAssembler::call_VM(noreg,
                               CAST_FROM_FN_PTR(address,
                               InterpreterRuntime::throw_pending_exception));
    __ should_not_reach_here();
    __ bind(L);
  }

  // do unlocking if necessary
  {
    Label L;
    __ ldrw(t, Address(rmethod, Method::access_flags_offset()));
    __ tbz(t, exact_log2(JVM_ACC_SYNCHRONIZED), L);
    // the code below should be shared with interpreter macro
    // assembler implementation
    {
      Label unlock;
      // BasicObjectLock will be first in list, since this is a
      // synchronized method. However, need to check that the object
      // has not been unlocked by an explicit monitorexit bytecode.

      // monitor expect in c_rarg1 for slow unlock path
      __ lea (c_rarg1, Address(rfp,   // address of first monitor
                               (intptr_t)(frame::interpreter_frame_initial_sp_offset *
                                          wordSize - sizeof(BasicObjectLock))));

      __ ldr(t, Address(c_rarg1, BasicObjectLock::obj_offset_in_bytes()));
      __ cbnz(t, unlock);

      // Entry already unlocked, need to throw exception
      __ MacroAssembler::call_VM(noreg,
                                 CAST_FROM_FN_PTR(address,
                   InterpreterRuntime::throw_illegal_monitor_state_exception));
      __ should_not_reach_here();

      __ bind(unlock);
      __ unlock_object(c_rarg1);
    }
    __ bind(L);
  }

  // jvmti support
  // Note: This must happen _after_ handling/throwing any exceptions since
  //       the exception handler code notifies the runtime of method exits
  //       too. If this happens before, method entry/exit notifications are
  //       not properly paired (was bug - gri 11/22/99).
  __ notify_method_exit(vtos, InterpreterMacroAssembler::NotifyJVMTI);

  // restore potential result in r0:d0, call result handler to
  // restore potential result in ST0 & handle result

  __ pop(ltos);
  __ pop(dtos);

  __ blr(result_handler);

  // remove activation
  __ ldr(esp, Address(rfp,
                    frame::interpreter_frame_sender_sp_offset *
                    wordSize)); // get sender sp
  // remove frame anchor
  __ leave();

  // resture sender sp
  __ mov(sp, esp);

  __ ret(lr);

  if (inc_counter) {
    // Handle overflow of counter and compile method
    __ bind(invocation_counter_overflow);
    generate_counter_overflow(continue_after_compile);
  }

  return entry_point;
}

//
// Generic interpreted method entry to (asm) interpreter
//
address TemplateInterpreterGenerator::generate_normal_entry(bool synchronized) {
  // determine code generation flags
  bool inc_counter  = UseCompiler || CountCompiledCalls || LogTouchedMethods;

  // rscratch1: sender sp
  address entry_point = __ pc();

  const Address constMethod(rmethod, Method::const_offset());
  const Address access_flags(rmethod, Method::access_flags_offset());
  const Address size_of_parameters(r3,
                                   ConstMethod::size_of_parameters_offset());
  const Address size_of_locals(r3, ConstMethod::size_of_locals_offset());

  // get parameter size (always needed)
  // need to load the const method first
  __ ldr(r3, constMethod);
  __ load_unsigned_short(r2, size_of_parameters);

  // r2: size of parameters

  __ load_unsigned_short(r3, size_of_locals); // get size of locals in words
  __ sub(r3, r3, r2); // r3 = no. of additional locals

  // see if we've got enough room on the stack for locals plus overhead.
  generate_stack_overflow_check();

  // compute beginning of parameters (rlocals)
  __ add(rlocals, esp, r2, ext::uxtx, 3);
  __ sub(rlocals, rlocals, wordSize);

  __ mov(rscratch1, esp);

  // r3 - # of additional locals
  // allocate space for locals
  // explicitly initialize locals
  // Initializing memory allocated for locals in the same direction as
  // the stack grows to ensure page initialization order according
  // to windows-aarch64 stack page growth requirement (see
  // https://docs.microsoft.com/en-us/cpp/build/arm64-windows-abi-conventions?view=msvc-160#stack)
  {
    Label exit, loop;
    __ ands(zr, r3, r3);
    __ br(Assembler::LE, exit); // do nothing if r3 <= 0
    __ bind(loop);
    __ str(zr, Address(__ pre(rscratch1, -wordSize)));
    __ sub(r3, r3, 1); // until everything initialized
    __ cbnz(r3, loop);
    __ bind(exit);
  }

  // Padding between locals and fixed part of activation frame to ensure
  // SP is always 16-byte aligned.
  __ andr(sp, rscratch1, -16);

  // And the base dispatch table
  __ get_dispatch();

  // initialize fixed part of activation frame
  generate_fixed_frame(false);

  // make sure method is not native & not abstract
#ifdef ASSERT
  __ ldrw(r0, access_flags);
  {
    Label L;
    __ tst(r0, JVM_ACC_NATIVE);
    __ br(Assembler::EQ, L);
    __ stop("tried to execute native method as non-native");
    __ bind(L);
  }
 {
    Label L;
    __ tst(r0, JVM_ACC_ABSTRACT);
    __ br(Assembler::EQ, L);
    __ stop("tried to execute abstract method in interpreter");
    __ bind(L);
  }
#endif

  // Since at this point in the method invocation the exception
  // handler would try to exit the monitor of synchronized methods
  // which hasn't been entered yet, we set the thread local variable
  // _do_not_unlock_if_synchronized to true. The remove_activation
  // will check this flag.

   const Address do_not_unlock_if_synchronized(rthread,
        in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()));
  __ mov(rscratch2, true);
  __ strb(rscratch2, do_not_unlock_if_synchronized);

  Register mdp = r3;
  __ profile_parameters_type(mdp, r1, r2);

  // increment invocation count & check for overflow
  Label invocation_counter_overflow;
  if (inc_counter) {
    generate_counter_incr(&invocation_counter_overflow);
  }

  Label continue_after_compile;
  __ bind(continue_after_compile);

  bang_stack_shadow_pages(false);

  // reset the _do_not_unlock_if_synchronized flag
  __ strb(zr, do_not_unlock_if_synchronized);

  // check for synchronized methods
  // Must happen AFTER invocation_counter check and stack overflow check,
  // so method is not locked if overflows.
  if (synchronized) {
    // Allocate monitor and lock method
    lock_method();
  } else {
    // no synchronization necessary
#ifdef ASSERT
    {
      Label L;
      __ ldrw(r0, access_flags);
      __ tst(r0, JVM_ACC_SYNCHRONIZED);
      __ br(Assembler::EQ, L);
      __ stop("method needs synchronization");
      __ bind(L);
    }
#endif
  }

  // start execution
#ifdef ASSERT
  {
    Label L;
     const Address monitor_block_top (rfp,
                 frame::interpreter_frame_monitor_block_top_offset * wordSize);
    __ ldr(rscratch1, monitor_block_top);
    __ cmp(esp, rscratch1);
    __ br(Assembler::EQ, L);
    __ stop("broken stack frame setup in interpreter");
    __ bind(L);
  }
#endif

  // jvmti support
  __ notify_method_entry();

  __ dispatch_next(vtos);

  // invocation counter overflow
  if (inc_counter) {
    // Handle overflow of counter and compile method
    __ bind(invocation_counter_overflow);
    generate_counter_overflow(continue_after_compile);
  }

  return entry_point;
}

//-----------------------------------------------------------------------------
// Exceptions

void TemplateInterpreterGenerator::generate_throw_exception() {
  // Entry point in previous activation (i.e., if the caller was
  // interpreted)
  Interpreter::_rethrow_exception_entry = __ pc();
  // Restore sp to interpreter_frame_last_sp even though we are going
  // to empty the expression stack for the exception processing.
  __ str(zr, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));
  // r0: exception
  // r3: return address/pc that threw exception
  __ restore_bcp();    // rbcp points to call/send
  __ restore_locals();
  __ restore_constant_pool_cache();
  __ reinit_heapbase();  // restore rheapbase as heapbase.
  __ get_dispatch();

  // Entry point for exceptions thrown within interpreter code
  Interpreter::_throw_exception_entry = __ pc();
  // If we came here via a NullPointerException on the receiver of a
  // method, rmethod may be corrupt.
  __ get_method(rmethod);
  // expression stack is undefined here
  // r0: exception
  // rbcp: exception bcp
  __ verify_oop(r0);
  __ mov(c_rarg1, r0);

  // expression stack must be empty before entering the VM in case of
  // an exception
  __ empty_expression_stack();
  // find exception handler address and preserve exception oop
  __ call_VM(r3,
             CAST_FROM_FN_PTR(address,
                          InterpreterRuntime::exception_handler_for_exception),
             c_rarg1);

  // Calculate stack limit
  __ ldr(rscratch1, Address(rmethod, Method::const_offset()));
  __ ldrh(rscratch1, Address(rscratch1, ConstMethod::max_stack_offset()));
  __ add(rscratch1, rscratch1, frame::interpreter_frame_monitor_size() + 4);
  __ ldr(rscratch2,
         Address(rfp, frame::interpreter_frame_initial_sp_offset * wordSize));
  __ sub(rscratch1, rscratch2, rscratch1, ext::uxtx, 3);
  __ andr(sp, rscratch1, -16);

  // r0: exception handler entry point
  // r3: preserved exception oop
  // rbcp: bcp for exception handler
  __ push_ptr(r3); // push exception which is now the only value on the stack
  __ br(r0); // jump to exception handler (may be _remove_activation_entry!)

  // If the exception is not handled in the current frame the frame is
  // removed and the exception is rethrown (i.e. exception
  // continuation is _rethrow_exception).
  //
  // Note: At this point the bci is still the bxi for the instruction
  // which caused the exception and the expression stack is
  // empty. Thus, for any VM calls at this point, GC will find a legal
  // oop map (with empty expression stack).

  //
  // JVMTI PopFrame support
  //

  Interpreter::_remove_activation_preserving_args_entry = __ pc();
  __ empty_expression_stack();
  // Set the popframe_processing bit in pending_popframe_condition
  // indicating that we are currently handling popframe, so that
  // call_VMs that may happen later do not trigger new popframe
  // handling cycles.
  __ ldrw(r3, Address(rthread, JavaThread::popframe_condition_offset()));
  __ orr(r3, r3, JavaThread::popframe_processing_bit);
  __ strw(r3, Address(rthread, JavaThread::popframe_condition_offset()));

  {
    // Check to see whether we are returning to a deoptimized frame.
    // (The PopFrame call ensures that the caller of the popped frame is
    // either interpreted or compiled and deoptimizes it if compiled.)
    // In this case, we can't call dispatch_next() after the frame is
    // popped, but instead must save the incoming arguments and restore
    // them after deoptimization has occurred.
    //
    // Note that we don't compare the return PC against the
    // deoptimization blob's unpack entry because of the presence of
    // adapter frames in C2.
    Label caller_not_deoptimized;
    __ ldr(c_rarg1, Address(rfp, frame::return_addr_offset * wordSize));
    __ super_call_VM_leaf(CAST_FROM_FN_PTR(address,
                               InterpreterRuntime::interpreter_contains), c_rarg1);
    __ cbnz(r0, caller_not_deoptimized);

    // Compute size of arguments for saving when returning to
    // deoptimized caller
    __ get_method(r0);
    __ ldr(r0, Address(r0, Method::const_offset()));
    __ load_unsigned_short(r0, Address(r0, in_bytes(ConstMethod::
                                                    size_of_parameters_offset())));
    __ lsl(r0, r0, Interpreter::logStackElementSize);
    __ restore_locals(); // XXX do we need this?
    __ sub(rlocals, rlocals, r0);
    __ add(rlocals, rlocals, wordSize);
    // Save these arguments
    __ super_call_VM_leaf(CAST_FROM_FN_PTR(address,
                                           Deoptimization::
                                           popframe_preserve_args),
                          rthread, r0, rlocals);

    __ remove_activation(vtos,
                         /* throw_monitor_exception */ false,
                         /* install_monitor_exception */ false,
                         /* notify_jvmdi */ false);

    // Inform deoptimization that it is responsible for restoring
    // these arguments
    __ mov(rscratch1, JavaThread::popframe_force_deopt_reexecution_bit);
    __ strw(rscratch1, Address(rthread, JavaThread::popframe_condition_offset()));

    // Continue in deoptimization handler
    __ ret(lr);

    __ bind(caller_not_deoptimized);
  }

  __ remove_activation(vtos,
                       /* throw_monitor_exception */ false,
                       /* install_monitor_exception */ false,
                       /* notify_jvmdi */ false);

  // Restore the last_sp and null it out
  __ ldr(esp, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));
  __ str(zr, Address(rfp, frame::interpreter_frame_last_sp_offset * wordSize));

  __ restore_bcp();
  __ restore_locals();
  __ restore_constant_pool_cache();
  __ get_method(rmethod);
  __ get_dispatch();

  // The method data pointer was incremented already during
  // call profiling. We have to restore the mdp for the current bcp.
  if (ProfileInterpreter) {
    __ set_method_data_pointer_for_bcp();
  }

  // Clear the popframe condition flag
  __ strw(zr, Address(rthread, JavaThread::popframe_condition_offset()));
  assert(JavaThread::popframe_inactive == 0, "fix popframe_inactive");

#if INCLUDE_JVMTI
  {
    Label L_done;

    __ ldrb(rscratch1, Address(rbcp, 0));
    __ cmpw(rscratch1, Bytecodes::_invokestatic);
    __ br(Assembler::NE, L_done);

    // The member name argument must be restored if _invokestatic is re-executed after a PopFrame call.
    // Detect such a case in the InterpreterRuntime function and return the member name argument, or NULL.

    __ ldr(c_rarg0, Address(rlocals, 0));
    __ call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::member_name_arg_or_null), c_rarg0, rmethod, rbcp);

    __ cbz(r0, L_done);

    __ str(r0, Address(esp, 0));
    __ bind(L_done);
  }
#endif // INCLUDE_JVMTI

  // Restore machine SP
  __ ldr(rscratch1, Address(rmethod, Method::const_offset()));
  __ ldrh(rscratch1, Address(rscratch1, ConstMethod::max_stack_offset()));
  __ add(rscratch1, rscratch1, frame::interpreter_frame_monitor_size() + 4);
  __ ldr(rscratch2,
         Address(rfp, frame::interpreter_frame_initial_sp_offset * wordSize));
  __ sub(rscratch1, rscratch2, rscratch1, ext::uxtw, 3);
  __ andr(sp, rscratch1, -16);

  __ dispatch_next(vtos);
  // end of PopFrame support

  Interpreter::_remove_activation_entry = __ pc();

  // preserve exception over this code sequence
  __ pop_ptr(r0);
  __ str(r0, Address(rthread, JavaThread::vm_result_offset()));
  // remove the activation (without doing throws on illegalMonitorExceptions)
  __ remove_activation(vtos, false, true, false);
  // restore exception
  __ get_vm_result(r0, rthread);

  // In between activations - previous activation type unknown yet
  // compute continuation point - the continuation point expects the
  // following registers set up:
  //
  // r0: exception
  // lr: return address/pc that threw exception
  // esp: expression stack of caller
  // rfp: fp of caller
  __ stp(r0, lr, Address(__ pre(sp, -2 * wordSize)));  // save exception & return address
  __ super_call_VM_leaf(CAST_FROM_FN_PTR(address,
                          SharedRuntime::exception_handler_for_return_address),
                        rthread, lr);
  __ mov(r1, r0);                               // save exception handler
  __ ldp(r0, lr, Address(__ post(sp, 2 * wordSize)));  // restore exception & return address
  // We might be returning to a deopt handler that expects r3 to
  // contain the exception pc
  __ mov(r3, lr);
  // Note that an "issuing PC" is actually the next PC after the call
  __ br(r1);                                    // jump to exception
                                                // handler of caller
}


//
// JVMTI ForceEarlyReturn support
//
address TemplateInterpreterGenerator::generate_earlyret_entry_for(TosState state) {
  address entry = __ pc();

  __ restore_bcp();
  __ restore_locals();
  __ empty_expression_stack();
  __ load_earlyret_value(state);

  __ ldr(rscratch1, Address(rthread, JavaThread::jvmti_thread_state_offset()));
  Address cond_addr(rscratch1, JvmtiThreadState::earlyret_state_offset());

  // Clear the earlyret state
  assert(JvmtiThreadState::earlyret_inactive == 0, "should be");
  __ str(zr, cond_addr);

  __ remove_activation(state,
                       false, /* throw_monitor_exception */
                       false, /* install_monitor_exception */
                       true); /* notify_jvmdi */
  __ ret(lr);

  return entry;
} // end of ForceEarlyReturn support



//-----------------------------------------------------------------------------
// Helper for vtos entry point generation

void TemplateInterpreterGenerator::set_vtos_entry_points(Template* t,
                                                         address& bep,
                                                         address& cep,
                                                         address& sep,
                                                         address& aep,
                                                         address& iep,
                                                         address& lep,
                                                         address& fep,
                                                         address& dep,
                                                         address& vep) {
  assert(t->is_valid() && t->tos_in() == vtos, "illegal template");
  Label L;
  aep = __ pc();  __ push_ptr();  __ b(L);
  fep = __ pc();  __ push_f();    __ b(L);
  dep = __ pc();  __ push_d();    __ b(L);
  lep = __ pc();  __ push_l();    __ b(L);
  bep = cep = sep =
  iep = __ pc();  __ push_i();
  vep = __ pc();
  __ bind(L);
  generate_and_dispatch(t);
}

//-----------------------------------------------------------------------------

// Non-product code
#ifndef PRODUCT
address TemplateInterpreterGenerator::generate_trace_code(TosState state) {
  address entry = __ pc();

  __ push(lr);
  __ push(state);
  __ push(RegSet::range(r0, r15), sp);
  __ mov(c_rarg2, r0);  // Pass itos
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::trace_bytecode),
             c_rarg1, c_rarg2, c_rarg3);
  __ pop(RegSet::range(r0, r15), sp);
  __ pop(state);
  __ pop(lr);
  __ ret(lr);                                   // return from result handler

  return entry;
}

void TemplateInterpreterGenerator::count_bytecode() {
  Register rscratch3 = r0;
  __ push(rscratch1);
  __ push(rscratch2);
  __ push(rscratch3);
  __ mov(rscratch3, (address) &BytecodeCounter::_counter_value);
  __ atomic_add(noreg, 1, rscratch3);
  __ pop(rscratch3);
  __ pop(rscratch2);
  __ pop(rscratch1);
}

void TemplateInterpreterGenerator::histogram_bytecode(Template* t) { ; }

void TemplateInterpreterGenerator::histogram_bytecode_pair(Template* t) { ; }


void TemplateInterpreterGenerator::trace_bytecode(Template* t) {
  // Call a little run-time stub to avoid blow-up for each bytecode.
  // The run-time runtime saves the right registers, depending on
  // the tosca in-state for the given template.

  assert(Interpreter::trace_code(t->tos_in()) != NULL,
         "entry must have been generated");
  __ bl(Interpreter::trace_code(t->tos_in()));
  __ reinit_heapbase();
}


void TemplateInterpreterGenerator::stop_interpreter_at() {
  Label L;
  __ push(rscratch1);
  __ mov(rscratch1, (address) &BytecodeCounter::_counter_value);
  __ ldr(rscratch1, Address(rscratch1));
  __ mov(rscratch2, StopInterpreterAt);
  __ cmpw(rscratch1, rscratch2);
  __ br(Assembler::NE, L);
  __ brk(0);
  __ bind(L);
  __ pop(rscratch1);
}

#endif // !PRODUCT
