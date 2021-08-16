/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2021 SAP SE. All rights reserved.
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
#include "gc/shared/barrierSetAssembler.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "interpreter/templateTable.hpp"
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
#include "runtime/vm_version.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

#undef __
#define __ _masm->

// Size of interpreter code.  Increase if too small.  Interpreter will
// fail with a guarantee ("not enough space for interpreter generation");
// if too small.
// Run with +PrintInterpreter to get the VM to print out the size.
// Max size with JVMTI
int TemplateInterpreter::InterpreterCodeSize = 256*K;

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label)        __ bind(label); BLOCK_COMMENT(#label ":")

//-----------------------------------------------------------------------------

address TemplateInterpreterGenerator::generate_slow_signature_handler() {
  // Slow_signature handler that respects the PPC C calling conventions.
  //
  // We get called by the native entry code with our output register
  // area == 8. First we call InterpreterRuntime::get_result_handler
  // to copy the pointer to the signature string temporarily to the
  // first C-argument and to return the result_handler in
  // R3_RET. Since native_entry will copy the jni-pointer to the
  // first C-argument slot later on, it is OK to occupy this slot
  // temporarilly. Then we copy the argument list on the java
  // expression stack into native varargs format on the native stack
  // and load arguments into argument registers. Integer arguments in
  // the varargs vector will be sign-extended to 8 bytes.
  //
  // On entry:
  //   R3_ARG1        - intptr_t*     Address of java argument list in memory.
  //   R15_prev_state - BytecodeInterpreter* Address of interpreter state for
  //     this method
  //   R19_method
  //
  // On exit (just before return instruction):
  //   R3_RET            - contains the address of the result_handler.
  //   R4_ARG2           - is not updated for static methods and contains "this" otherwise.
  //   R5_ARG3-R10_ARG8: - When the (i-2)th Java argument is not of type float or double,
  //                       ARGi contains this argument. Otherwise, ARGi is not updated.
  //   F1_ARG1-F13_ARG13 - contain the first 13 arguments of type float or double.

  const int LogSizeOfTwoInstructions = 3;

  // FIXME: use Argument:: GL: Argument names different numbers!
  const int max_fp_register_arguments  = 13;
  const int max_int_register_arguments = 6;  // first 2 are reserved

  const Register arg_java       = R21_tmp1;
  const Register arg_c          = R22_tmp2;
  const Register signature      = R23_tmp3;  // is string
  const Register sig_byte       = R24_tmp4;
  const Register fpcnt          = R25_tmp5;
  const Register argcnt         = R26_tmp6;
  const Register intSlot        = R27_tmp7;
  const Register target_sp      = R28_tmp8;
  const FloatRegister floatSlot = F0;

  address entry = __ function_entry();

  __ save_LR_CR(R0);
  __ save_nonvolatile_gprs(R1_SP, _spill_nonvolatiles_neg(r14));
  // We use target_sp for storing arguments in the C frame.
  __ mr(target_sp, R1_SP);
  __ push_frame_reg_args_nonvolatiles(0, R11_scratch1);

  __ mr(arg_java, R3_ARG1);

  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::get_signature), R16_thread, R19_method);

  // Signature is in R3_RET. Signature is callee saved.
  __ mr(signature, R3_RET);

  // Get the result handler.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::get_result_handler), R16_thread, R19_method);

  {
    Label L;
    // test if static
    // _access_flags._flags must be at offset 0.
    // TODO PPC port: requires change in shared code.
    //assert(in_bytes(AccessFlags::flags_offset()) == 0,
    //       "MethodDesc._access_flags == MethodDesc._access_flags._flags");
    // _access_flags must be a 32 bit value.
    assert(sizeof(AccessFlags) == 4, "wrong size");
    __ lwa(R11_scratch1/*access_flags*/, method_(access_flags));
    // testbit with condition register.
    __ testbitdi(CCR0, R0, R11_scratch1/*access_flags*/, JVM_ACC_STATIC_BIT);
    __ btrue(CCR0, L);
    // For non-static functions, pass "this" in R4_ARG2 and copy it
    // to 2nd C-arg slot.
    // We need to box the Java object here, so we use arg_java
    // (address of current Java stack slot) as argument and don't
    // dereference it as in case of ints, floats, etc.
    __ mr(R4_ARG2, arg_java);
    __ addi(arg_java, arg_java, -BytesPerWord);
    __ std(R4_ARG2, _abi0(carg_2), target_sp);
    __ bind(L);
  }

  // Will be incremented directly after loop_start. argcnt=0
  // corresponds to 3rd C argument.
  __ li(argcnt, -1);
  // arg_c points to 3rd C argument
  __ addi(arg_c, target_sp, _abi0(carg_3));
  // no floating-point args parsed so far
  __ li(fpcnt, 0);

  Label move_intSlot_to_ARG, move_floatSlot_to_FARG;
  Label loop_start, loop_end;
  Label do_int, do_long, do_float, do_double, do_dontreachhere, do_object, do_array, do_boxed;

  // signature points to '(' at entry
#ifdef ASSERT
  __ lbz(sig_byte, 0, signature);
  __ cmplwi(CCR0, sig_byte, '(');
  __ bne(CCR0, do_dontreachhere);
#endif

  __ bind(loop_start);

  __ addi(argcnt, argcnt, 1);
  __ lbzu(sig_byte, 1, signature);

  __ cmplwi(CCR0, sig_byte, ')'); // end of signature
  __ beq(CCR0, loop_end);

  __ cmplwi(CCR0, sig_byte, 'B'); // byte
  __ beq(CCR0, do_int);

  __ cmplwi(CCR0, sig_byte, 'C'); // char
  __ beq(CCR0, do_int);

  __ cmplwi(CCR0, sig_byte, 'D'); // double
  __ beq(CCR0, do_double);

  __ cmplwi(CCR0, sig_byte, 'F'); // float
  __ beq(CCR0, do_float);

  __ cmplwi(CCR0, sig_byte, 'I'); // int
  __ beq(CCR0, do_int);

  __ cmplwi(CCR0, sig_byte, 'J'); // long
  __ beq(CCR0, do_long);

  __ cmplwi(CCR0, sig_byte, 'S'); // short
  __ beq(CCR0, do_int);

  __ cmplwi(CCR0, sig_byte, 'Z'); // boolean
  __ beq(CCR0, do_int);

  __ cmplwi(CCR0, sig_byte, 'L'); // object
  __ beq(CCR0, do_object);

  __ cmplwi(CCR0, sig_byte, '['); // array
  __ beq(CCR0, do_array);

  //  __ cmplwi(CCR0, sig_byte, 'V'); // void cannot appear since we do not parse the return type
  //  __ beq(CCR0, do_void);

  __ bind(do_dontreachhere);

  __ unimplemented("ShouldNotReachHere in slow_signature_handler");

  __ bind(do_array);

  {
    Label start_skip, end_skip;

    __ bind(start_skip);
    __ lbzu(sig_byte, 1, signature);
    __ cmplwi(CCR0, sig_byte, '[');
    __ beq(CCR0, start_skip); // skip further brackets
    __ cmplwi(CCR0, sig_byte, '9');
    __ bgt(CCR0, end_skip);   // no optional size
    __ cmplwi(CCR0, sig_byte, '0');
    __ bge(CCR0, start_skip); // skip optional size
    __ bind(end_skip);

    __ cmplwi(CCR0, sig_byte, 'L');
    __ beq(CCR0, do_object);  // for arrays of objects, the name of the object must be skipped
    __ b(do_boxed);          // otherwise, go directly to do_boxed
  }

  __ bind(do_object);
  {
    Label L;
    __ bind(L);
    __ lbzu(sig_byte, 1, signature);
    __ cmplwi(CCR0, sig_byte, ';');
    __ bne(CCR0, L);
   }
  // Need to box the Java object here, so we use arg_java (address of
  // current Java stack slot) as argument and don't dereference it as
  // in case of ints, floats, etc.
  Label do_null;
  __ bind(do_boxed);
  __ ld(R0,0, arg_java);
  __ cmpdi(CCR0, R0, 0);
  __ li(intSlot,0);
  __ beq(CCR0, do_null);
  __ mr(intSlot, arg_java);
  __ bind(do_null);
  __ std(intSlot, 0, arg_c);
  __ addi(arg_java, arg_java, -BytesPerWord);
  __ addi(arg_c, arg_c, BytesPerWord);
  __ cmplwi(CCR0, argcnt, max_int_register_arguments);
  __ blt(CCR0, move_intSlot_to_ARG);
  __ b(loop_start);

  __ bind(do_int);
  __ lwa(intSlot, 0, arg_java);
  __ std(intSlot, 0, arg_c);
  __ addi(arg_java, arg_java, -BytesPerWord);
  __ addi(arg_c, arg_c, BytesPerWord);
  __ cmplwi(CCR0, argcnt, max_int_register_arguments);
  __ blt(CCR0, move_intSlot_to_ARG);
  __ b(loop_start);

  __ bind(do_long);
  __ ld(intSlot, -BytesPerWord, arg_java);
  __ std(intSlot, 0, arg_c);
  __ addi(arg_java, arg_java, - 2 * BytesPerWord);
  __ addi(arg_c, arg_c, BytesPerWord);
  __ cmplwi(CCR0, argcnt, max_int_register_arguments);
  __ blt(CCR0, move_intSlot_to_ARG);
  __ b(loop_start);

  __ bind(do_float);
  __ lfs(floatSlot, 0, arg_java);
#if defined(LINUX)
  // Linux uses ELF ABI. Both original ELF and ELFv2 ABIs have float
  // in the least significant word of an argument slot.
#if defined(VM_LITTLE_ENDIAN)
  __ stfs(floatSlot, 0, arg_c);
#else
  __ stfs(floatSlot, 4, arg_c);
#endif
#elif defined(AIX)
  // Although AIX runs on big endian CPU, float is in most significant
  // word of an argument slot.
  __ stfs(floatSlot, 0, arg_c);
#else
#error "unknown OS"
#endif
  __ addi(arg_java, arg_java, -BytesPerWord);
  __ addi(arg_c, arg_c, BytesPerWord);
  __ cmplwi(CCR0, fpcnt, max_fp_register_arguments);
  __ blt(CCR0, move_floatSlot_to_FARG);
  __ b(loop_start);

  __ bind(do_double);
  __ lfd(floatSlot, - BytesPerWord, arg_java);
  __ stfd(floatSlot, 0, arg_c);
  __ addi(arg_java, arg_java, - 2 * BytesPerWord);
  __ addi(arg_c, arg_c, BytesPerWord);
  __ cmplwi(CCR0, fpcnt, max_fp_register_arguments);
  __ blt(CCR0, move_floatSlot_to_FARG);
  __ b(loop_start);

  __ bind(loop_end);

  __ pop_frame();
  __ restore_nonvolatile_gprs(R1_SP, _spill_nonvolatiles_neg(r14));
  __ restore_LR_CR(R0);

  __ blr();

  Label move_int_arg, move_float_arg;
  __ bind(move_int_arg); // each case must consist of 2 instructions (otherwise adapt LogSizeOfTwoInstructions)
  __ mr(R5_ARG3, intSlot);  __ b(loop_start);
  __ mr(R6_ARG4, intSlot);  __ b(loop_start);
  __ mr(R7_ARG5, intSlot);  __ b(loop_start);
  __ mr(R8_ARG6, intSlot);  __ b(loop_start);
  __ mr(R9_ARG7, intSlot);  __ b(loop_start);
  __ mr(R10_ARG8, intSlot); __ b(loop_start);

  __ bind(move_float_arg); // each case must consist of 2 instructions (otherwise adapt LogSizeOfTwoInstructions)
  __ fmr(F1_ARG1, floatSlot);   __ b(loop_start);
  __ fmr(F2_ARG2, floatSlot);   __ b(loop_start);
  __ fmr(F3_ARG3, floatSlot);   __ b(loop_start);
  __ fmr(F4_ARG4, floatSlot);   __ b(loop_start);
  __ fmr(F5_ARG5, floatSlot);   __ b(loop_start);
  __ fmr(F6_ARG6, floatSlot);   __ b(loop_start);
  __ fmr(F7_ARG7, floatSlot);   __ b(loop_start);
  __ fmr(F8_ARG8, floatSlot);   __ b(loop_start);
  __ fmr(F9_ARG9, floatSlot);   __ b(loop_start);
  __ fmr(F10_ARG10, floatSlot); __ b(loop_start);
  __ fmr(F11_ARG11, floatSlot); __ b(loop_start);
  __ fmr(F12_ARG12, floatSlot); __ b(loop_start);
  __ fmr(F13_ARG13, floatSlot); __ b(loop_start);

  __ bind(move_intSlot_to_ARG);
  __ sldi(R0, argcnt, LogSizeOfTwoInstructions);
  __ load_const(R11_scratch1, move_int_arg); // Label must be bound here.
  __ add(R11_scratch1, R0, R11_scratch1);
  __ mtctr(R11_scratch1/*branch_target*/);
  __ bctr();
  __ bind(move_floatSlot_to_FARG);
  __ sldi(R0, fpcnt, LogSizeOfTwoInstructions);
  __ addi(fpcnt, fpcnt, 1);
  __ load_const(R11_scratch1, move_float_arg); // Label must be bound here.
  __ add(R11_scratch1, R0, R11_scratch1);
  __ mtctr(R11_scratch1/*branch_target*/);
  __ bctr();

  return entry;
}

address TemplateInterpreterGenerator::generate_result_handler_for(BasicType type) {
  //
  // Registers alive
  //   R3_RET
  //   LR
  //
  // Registers updated
  //   R3_RET
  //

  Label done;
  address entry = __ pc();

  switch (type) {
  case T_BOOLEAN:
    // convert !=0 to 1
    __ neg(R0, R3_RET);
    __ orr(R0, R3_RET, R0);
    __ srwi(R3_RET, R0, 31);
    break;
  case T_BYTE:
     // sign extend 8 bits
     __ extsb(R3_RET, R3_RET);
     break;
  case T_CHAR:
     // zero extend 16 bits
     __ clrldi(R3_RET, R3_RET, 48);
     break;
  case T_SHORT:
     // sign extend 16 bits
     __ extsh(R3_RET, R3_RET);
     break;
  case T_INT:
     // sign extend 32 bits
     __ extsw(R3_RET, R3_RET);
     break;
  case T_LONG:
     break;
  case T_OBJECT:
    // JNIHandles::resolve result.
    __ resolve_jobject(R3_RET, R11_scratch1, R31, MacroAssembler::PRESERVATION_FRAME_LR); // kills R31
    break;
  case T_FLOAT:
     break;
  case T_DOUBLE:
     break;
  case T_VOID:
     break;
  default: ShouldNotReachHere();
  }

  BIND(done);
  __ blr();

  return entry;
}

// Abstract method entry.
//
address TemplateInterpreterGenerator::generate_abstract_entry(void) {
  address entry = __ pc();

  //
  // Registers alive
  //   R16_thread     - JavaThread*
  //   R19_method     - callee's method (method to be invoked)
  //   R1_SP          - SP prepared such that caller's outgoing args are near top
  //   LR             - return address to caller
  //
  // Stack layout at this point:
  //
  //   0       [TOP_IJAVA_FRAME_ABI]         <-- R1_SP
  //           alignment (optional)
  //           [outgoing Java arguments]
  //           ...
  //   PARENT  [PARENT_IJAVA_FRAME_ABI]
  //            ...
  //

  // Can't use call_VM here because we have not set up a new
  // interpreter state. Make the call to the vm and make it look like
  // our caller set up the JavaFrameAnchor.
  __ set_top_ijava_frame_at_SP_as_last_Java_frame(R1_SP, R12_scratch2/*tmp*/);

  // Push a new C frame and save LR.
  __ save_LR_CR(R0);
  __ push_frame_reg_args(0, R11_scratch1);

  // This is not a leaf but we have a JavaFrameAnchor now and we will
  // check (create) exceptions afterward so this is ok.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_AbstractMethodErrorWithMethod),
                  R16_thread, R19_method);

  // Pop the C frame and restore LR.
  __ pop_frame();
  __ restore_LR_CR(R0);

  // Reset JavaFrameAnchor from call_VM_leaf above.
  __ reset_last_Java_frame();

  // We don't know our caller, so jump to the general forward exception stub,
  // which will also pop our full frame off. Satisfy the interface of
  // SharedRuntime::generate_forward_exception()
  __ load_const_optimized(R11_scratch1, StubRoutines::forward_exception_entry(), R0);
  __ mtctr(R11_scratch1);
  __ bctr();

  return entry;
}

// Interpreter intrinsic for WeakReference.get().
// 1. Don't push a full blown frame and go on dispatching, but fetch the value
//    into R8 and return quickly
// 2. If G1 is active we *must* execute this intrinsic for corrrectness:
//    It contains a GC barrier which puts the reference into the satb buffer
//    to indicate that someone holds a strong reference to the object the
//    weak ref points to!
address TemplateInterpreterGenerator::generate_Reference_get_entry(void) {
  // Code: _aload_0, _getfield, _areturn
  // parameter size = 1
  //
  // The code that gets generated by this routine is split into 2 parts:
  //    1. the "intrinsified" code for G1 (or any SATB based GC),
  //    2. the slow path - which is an expansion of the regular method entry.
  //
  // Notes:
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

  address entry = __ pc();

  const int referent_offset = java_lang_ref_Reference::referent_offset();

  Label slow_path;

  // Debugging not possible, so can't use __ skip_if_jvmti_mode(slow_path, GR31_SCRATCH);

  // In the G1 code we don't check if we need to reach a safepoint. We
  // continue and the thread will safepoint at the next bytecode dispatch.

  // If the receiver is null then it is OK to jump to the slow path.
  __ ld(R3_RET, Interpreter::stackElementSize, R15_esp); // get receiver

  // Check if receiver == NULL and go the slow path.
  __ cmpdi(CCR0, R3_RET, 0);
  __ beq(CCR0, slow_path);

  __ load_heap_oop(R3_RET, referent_offset, R3_RET,
                   /* non-volatile temp */ R31, R11_scratch1,
                   MacroAssembler::PRESERVATION_FRAME_LR,
                   ON_WEAK_OOP_REF);

  // Generate the G1 pre-barrier code to log the value of
  // the referent field in an SATB buffer. Note with
  // these parameters the pre-barrier does not generate
  // the load of the previous value.

  // Restore caller sp for c2i case (from compiled) and for resized sender frame (from interpreted).
  __ resize_frame_absolute(R21_sender_SP, R11_scratch1, R0);

  __ blr();

  __ bind(slow_path);
  __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::zerolocals), R11_scratch1);
  return entry;
}

address TemplateInterpreterGenerator::generate_StackOverflowError_handler() {
  address entry = __ pc();

  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();
  // Throw exception.
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::throw_StackOverflowError));
  return entry;
}

address TemplateInterpreterGenerator::generate_ArrayIndexOutOfBounds_handler() {
  address entry = __ pc();
  __ empty_expression_stack();
  // R4_ARG2 already contains the array.
  // Index is in R17_tos.
  __ mr(R5_ARG3, R17_tos);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_ArrayIndexOutOfBoundsException), R4_ARG2, R5_ARG3);
  return entry;
}

address TemplateInterpreterGenerator::generate_ClassCastException_handler() {
  address entry = __ pc();
  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();

  // Load exception object.
  // Thread will be loaded to R3_ARG1.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_ClassCastException), R17_tos);
#ifdef ASSERT
  // Above call must not return here since exception pending.
  __ should_not_reach_here();
#endif
  return entry;
}

address TemplateInterpreterGenerator::generate_exception_handler_common(const char* name, const char* message, bool pass_oop) {
  address entry = __ pc();
  //__ untested("generate_exception_handler_common");
  Register Rexception = R17_tos;

  // Expression stack must be empty before entering the VM if an exception happened.
  __ empty_expression_stack();

  __ load_const_optimized(R4_ARG2, (address) name, R11_scratch1);
  if (pass_oop) {
    __ mr(R5_ARG3, Rexception);
    __ call_VM(Rexception, CAST_FROM_FN_PTR(address, InterpreterRuntime::create_klass_exception));
  } else {
    __ load_const_optimized(R5_ARG3, (address) message, R11_scratch1);
    __ call_VM(Rexception, CAST_FROM_FN_PTR(address, InterpreterRuntime::create_exception));
  }

  // Throw exception.
  __ mr(R3_ARG1, Rexception);
  __ load_const_optimized(R11_scratch1, Interpreter::throw_exception_entry(), R12_scratch2);
  __ mtctr(R11_scratch1);
  __ bctr();

  return entry;
}

// This entry is returned to when a call returns to the interpreter.
// When we arrive here, we expect that the callee stack frame is already popped.
address TemplateInterpreterGenerator::generate_return_entry_for(TosState state, int step, size_t index_size) {
  address entry = __ pc();

  // Move the value out of the return register back to the TOS cache of current frame.
  switch (state) {
    case ltos:
    case btos:
    case ztos:
    case ctos:
    case stos:
    case atos:
    case itos: __ mr(R17_tos, R3_RET); break;   // RET -> TOS cache
    case ftos:
    case dtos: __ fmr(F15_ftos, F1_RET); break; // TOS cache -> GR_FRET
    case vtos: break;                           // Nothing to do, this was a void return.
    default  : ShouldNotReachHere();
  }

  __ restore_interpreter_state(R11_scratch1); // Sets R11_scratch1 = fp.
  __ ld(R12_scratch2, _ijava_state_neg(top_frame_sp), R11_scratch1);
  __ resize_frame_absolute(R12_scratch2, R11_scratch1, R0);

  // Compiled code destroys templateTableBase, reload.
  __ load_const_optimized(R25_templateTableBase, (address)Interpreter::dispatch_table((TosState)0), R12_scratch2);

  if (state == atos) {
    __ profile_return_type(R3_RET, R11_scratch1, R12_scratch2);
  }

  const Register cache = R11_scratch1;
  const Register size  = R12_scratch2;
  __ get_cache_and_index_at_bcp(cache, 1, index_size);

  // Get least significant byte of 64 bit value:
#if defined(VM_LITTLE_ENDIAN)
  __ lbz(size, in_bytes(ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::flags_offset()), cache);
#else
  __ lbz(size, in_bytes(ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::flags_offset()) + 7, cache);
#endif
  __ sldi(size, size, Interpreter::logStackElementSize);
  __ add(R15_esp, R15_esp, size);

 __ check_and_handle_popframe(R11_scratch1);
 __ check_and_handle_earlyret(R11_scratch1);

  __ dispatch_next(state, step);
  return entry;
}

address TemplateInterpreterGenerator::generate_deopt_entry_for(TosState state, int step, address continuation) {
  address entry = __ pc();
  // If state != vtos, we're returning from a native method, which put it's result
  // into the result register. So move the value out of the return register back
  // to the TOS cache of current frame.

  switch (state) {
    case ltos:
    case btos:
    case ztos:
    case ctos:
    case stos:
    case atos:
    case itos: __ mr(R17_tos, R3_RET); break;   // GR_RET -> TOS cache
    case ftos:
    case dtos: __ fmr(F15_ftos, F1_RET); break; // TOS cache -> GR_FRET
    case vtos: break;                           // Nothing to do, this was a void return.
    default  : ShouldNotReachHere();
  }

  // Load LcpoolCache @@@ should be already set!
  __ get_constant_pool_cache(R27_constPoolCache);

  // Handle a pending exception, fall through if none.
  __ check_and_forward_exception(R11_scratch1, R12_scratch2);

  // Start executing bytecodes.
  if (continuation == NULL) {
    __ dispatch_next(state, step);
  } else {
    __ jump_to_entry(continuation, R11_scratch1);
  }

  return entry;
}

address TemplateInterpreterGenerator::generate_safept_entry_for(TosState state, address runtime_entry) {
  address entry = __ pc();

  __ push(state);
  __ call_VM(noreg, runtime_entry);
  __ dispatch_via(vtos, Interpreter::_normal_table.table_for(vtos));

  return entry;
}

// Helpers for commoning out cases in the various type of method entries.

// Increment invocation count & check for overflow.
//
// Note: checking for negative value instead of overflow
//       so we have a 'sticky' overflow test.
//
void TemplateInterpreterGenerator::generate_counter_incr(Label* overflow) {
  // Note: In tiered we increment either counters in method or in MDO depending if we're profiling or not.
  Register Rscratch1   = R11_scratch1;
  Register Rscratch2   = R12_scratch2;
  Register R3_counters = R3_ARG1;
  Label done;

  const int increment = InvocationCounter::count_increment;
  Label no_mdo;
  if (ProfileInterpreter) {
    const Register Rmdo = R3_counters;
    __ ld(Rmdo, in_bytes(Method::method_data_offset()), R19_method);
    __ cmpdi(CCR0, Rmdo, 0);
    __ beq(CCR0, no_mdo);

    // Increment invocation counter in the MDO.
    const int mdo_ic_offs = in_bytes(MethodData::invocation_counter_offset()) + in_bytes(InvocationCounter::counter_offset());
    __ lwz(Rscratch2, mdo_ic_offs, Rmdo);
    __ lwz(Rscratch1, in_bytes(MethodData::invoke_mask_offset()), Rmdo);
    __ addi(Rscratch2, Rscratch2, increment);
    __ stw(Rscratch2, mdo_ic_offs, Rmdo);
    __ and_(Rscratch1, Rscratch2, Rscratch1);
    __ bne(CCR0, done);
    __ b(*overflow);
  }

  // Increment counter in MethodCounters*.
  const int mo_ic_offs = in_bytes(MethodCounters::invocation_counter_offset()) + in_bytes(InvocationCounter::counter_offset());
  __ bind(no_mdo);
  __ get_method_counters(R19_method, R3_counters, done);
  __ lwz(Rscratch2, mo_ic_offs, R3_counters);
  __ lwz(Rscratch1, in_bytes(MethodCounters::invoke_mask_offset()), R3_counters);
  __ addi(Rscratch2, Rscratch2, increment);
  __ stw(Rscratch2, mo_ic_offs, R3_counters);
  __ and_(Rscratch1, Rscratch2, Rscratch1);
  __ beq(CCR0, *overflow);

  __ bind(done);
}

// Generate code to initiate compilation on invocation counter overflow.
void TemplateInterpreterGenerator::generate_counter_overflow(Label& continue_entry) {
  // Generate code to initiate compilation on the counter overflow.

  // InterpreterRuntime::frequency_counter_overflow takes one arguments,
  // which indicates if the counter overflow occurs at a backwards branch (NULL bcp)
  // We pass zero in.
  // The call returns the address of the verified entry point for the method or NULL
  // if the compilation did not complete (either went background or bailed out).
  //
  // Unlike the C++ interpreter above: Check exceptions!
  // Assumption: Caller must set the flag "do_not_unlock_if_sychronized" if the monitor of a sync'ed
  // method has not yet been created. Thus, no unlocking of a non-existing monitor can occur.

  __ li(R4_ARG2, 0);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::frequency_counter_overflow), R4_ARG2, true);

  // Returns verified_entry_point or NULL.
  // We ignore it in any case.
  __ b(continue_entry);
}

// See if we've got enough room on the stack for locals plus overhead below
// JavaThread::stack_overflow_limit(). If not, throw a StackOverflowError
// without going through the signal handler, i.e., reserved and yellow zones
// will not be made usable. The shadow zone must suffice to handle the
// overflow.
//
// Kills Rmem_frame_size, Rscratch1.
void TemplateInterpreterGenerator::generate_stack_overflow_check(Register Rmem_frame_size, Register Rscratch1) {
  Label done;
  assert_different_registers(Rmem_frame_size, Rscratch1);

  BLOCK_COMMENT("stack_overflow_check_with_compare {");
  __ sub(Rmem_frame_size, R1_SP, Rmem_frame_size);
  __ ld(Rscratch1, thread_(stack_overflow_limit));
  __ cmpld(CCR0/*is_stack_overflow*/, Rmem_frame_size, Rscratch1);
  __ bgt(CCR0/*is_stack_overflow*/, done);

  // The stack overflows. Load target address of the runtime stub and call it.
  assert(StubRoutines::throw_StackOverflowError_entry() != NULL, "generated in wrong order");
  __ load_const_optimized(Rscratch1, (StubRoutines::throw_StackOverflowError_entry()), R0);
  __ mtctr(Rscratch1);
  // Restore caller_sp (c2i adapter may exist, but no shrinking of interpreted caller frame).
#ifdef ASSERT
  Label frame_not_shrunk;
  __ cmpld(CCR0, R1_SP, R21_sender_SP);
  __ ble(CCR0, frame_not_shrunk);
  __ stop("frame shrunk");
  __ bind(frame_not_shrunk);
  __ ld(Rscratch1, 0, R1_SP);
  __ ld(R0, 0, R21_sender_SP);
  __ cmpd(CCR0, R0, Rscratch1);
  __ asm_assert_eq("backlink");
#endif // ASSERT
  __ mr(R1_SP, R21_sender_SP);
  __ bctr();

  __ align(32, 12);
  __ bind(done);
  BLOCK_COMMENT("} stack_overflow_check_with_compare");
}

// Lock the current method, interpreter register window must be set up!
void TemplateInterpreterGenerator::lock_method(Register Rflags, Register Rscratch1, Register Rscratch2, bool flags_preloaded) {
  const Register Robj_to_lock = Rscratch2;

  {
    if (!flags_preloaded) {
      __ lwz(Rflags, method_(access_flags));
    }

#ifdef ASSERT
    // Check if methods needs synchronization.
    {
      Label Lok;
      __ testbitdi(CCR0, R0, Rflags, JVM_ACC_SYNCHRONIZED_BIT);
      __ btrue(CCR0,Lok);
      __ stop("method doesn't need synchronization");
      __ bind(Lok);
    }
#endif // ASSERT
  }

  // Get synchronization object to Rscratch2.
  {
    Label Lstatic;
    Label Ldone;

    __ testbitdi(CCR0, R0, Rflags, JVM_ACC_STATIC_BIT);
    __ btrue(CCR0, Lstatic);

    // Non-static case: load receiver obj from stack and we're done.
    __ ld(Robj_to_lock, R18_locals);
    __ b(Ldone);

    __ bind(Lstatic); // Static case: Lock the java mirror
    // Load mirror from interpreter frame.
    __ ld(Robj_to_lock, _abi0(callers_sp), R1_SP);
    __ ld(Robj_to_lock, _ijava_state_neg(mirror), Robj_to_lock);

    __ bind(Ldone);
    __ verify_oop(Robj_to_lock);
  }

  // Got the oop to lock => execute!
  __ add_monitor_to_stack(true, Rscratch1, R0);

  __ std(Robj_to_lock, BasicObjectLock::obj_offset_in_bytes(), R26_monitor);
  __ lock_object(R26_monitor, Robj_to_lock);
}

// Generate a fixed interpreter frame for pure interpreter
// and I2N native transition frames.
//
// Before (stack grows downwards):
//
//         |  ...         |
//         |------------- |
//         |  java arg0   |
//         |  ...         |
//         |  java argn   |
//         |              |   <-   R15_esp
//         |              |
//         |--------------|
//         | abi_112      |
//         |              |   <-   R1_SP
//         |==============|
//
//
// After:
//
//         |  ...         |
//         |  java arg0   |<-   R18_locals
//         |  ...         |
//         |  java argn   |
//         |--------------|
//         |              |
//         |  java locals |
//         |              |
//         |--------------|
//         |  abi_48      |
//         |==============|
//         |              |
//         |   istate     |
//         |              |
//         |--------------|
//         |   monitor    |<-   R26_monitor
//         |--------------|
//         |              |<-   R15_esp
//         | expression   |
//         | stack        |
//         |              |
//         |--------------|
//         |              |
//         | abi_112      |<-   R1_SP
//         |==============|
//
// The top most frame needs an abi space of 112 bytes. This space is needed,
// since we call to c. The c function may spill their arguments to the caller
// frame. When we call to java, we don't need these spill slots. In order to save
// space on the stack, we resize the caller. However, java locals reside in
// the caller frame and the frame has to be increased. The frame_size for the
// current frame was calculated based on max_stack as size for the expression
// stack. At the call, just a part of the expression stack might be used.
// We don't want to waste this space and cut the frame back accordingly.
// The resulting amount for resizing is calculated as follows:
// resize =   (number_of_locals - number_of_arguments) * slot_size
//          + (R1_SP - R15_esp) + 48
//
// The size for the callee frame is calculated:
// framesize = 112 + max_stack + monitor + state_size
//
// maxstack:   Max number of slots on the expression stack, loaded from the method.
// monitor:    We statically reserve room for one monitor object.
// state_size: We save the current state of the interpreter to this area.
//
void TemplateInterpreterGenerator::generate_fixed_frame(bool native_call, Register Rsize_of_parameters, Register Rsize_of_locals) {
  Register Rparent_frame_resize = R6_ARG4, // Frame will grow by this number of bytes.
           Rtop_frame_size      = R7_ARG5,
           Rconst_method        = R8_ARG6,
           Rconst_pool          = R9_ARG7,
           Rmirror              = R10_ARG8;

  assert_different_registers(Rsize_of_parameters, Rsize_of_locals, Rparent_frame_resize, Rtop_frame_size,
                             Rconst_method, Rconst_pool);

  __ ld(Rconst_method, method_(const));
  __ lhz(Rsize_of_parameters /* number of params */,
         in_bytes(ConstMethod::size_of_parameters_offset()), Rconst_method);
  if (native_call) {
    // If we're calling a native method, we reserve space for the worst-case signature
    // handler varargs vector, which is max(Argument::n_register_parameters, parameter_count+2).
    // We add two slots to the parameter_count, one for the jni
    // environment and one for a possible native mirror.
    Label skip_native_calculate_max_stack;
    __ addi(Rtop_frame_size, Rsize_of_parameters, 2);
    __ cmpwi(CCR0, Rtop_frame_size, Argument::n_register_parameters);
    __ bge(CCR0, skip_native_calculate_max_stack);
    __ li(Rtop_frame_size, Argument::n_register_parameters);
    __ bind(skip_native_calculate_max_stack);
    __ sldi(Rsize_of_parameters, Rsize_of_parameters, Interpreter::logStackElementSize);
    __ sldi(Rtop_frame_size, Rtop_frame_size, Interpreter::logStackElementSize);
    __ sub(Rparent_frame_resize, R1_SP, R15_esp); // <0, off by Interpreter::stackElementSize!
    assert(Rsize_of_locals == noreg, "Rsize_of_locals not initialized"); // Only relevant value is Rsize_of_parameters.
  } else {
    __ lhz(Rsize_of_locals /* number of params */, in_bytes(ConstMethod::size_of_locals_offset()), Rconst_method);
    __ sldi(Rsize_of_parameters, Rsize_of_parameters, Interpreter::logStackElementSize);
    __ sldi(Rsize_of_locals, Rsize_of_locals, Interpreter::logStackElementSize);
    __ lhz(Rtop_frame_size, in_bytes(ConstMethod::max_stack_offset()), Rconst_method);
    __ sub(R11_scratch1, Rsize_of_locals, Rsize_of_parameters); // >=0
    __ sub(Rparent_frame_resize, R1_SP, R15_esp); // <0, off by Interpreter::stackElementSize!
    __ sldi(Rtop_frame_size, Rtop_frame_size, Interpreter::logStackElementSize);
    __ add(Rparent_frame_resize, Rparent_frame_resize, R11_scratch1);
  }

  // Compute top frame size.
  __ addi(Rtop_frame_size, Rtop_frame_size, frame::abi_reg_args_size + frame::ijava_state_size);

  // Cut back area between esp and max_stack.
  __ addi(Rparent_frame_resize, Rparent_frame_resize, frame::abi_minframe_size - Interpreter::stackElementSize);

  __ round_to(Rtop_frame_size, frame::alignment_in_bytes);
  __ round_to(Rparent_frame_resize, frame::alignment_in_bytes);
  // Rparent_frame_resize = (locals-parameters) - (ESP-SP-ABI48) Rounded to frame alignment size.
  // Enlarge by locals-parameters (not in case of native_call), shrink by ESP-SP-ABI48.

  if (!native_call) {
    // Stack overflow check.
    // Native calls don't need the stack size check since they have no
    // expression stack and the arguments are already on the stack and
    // we only add a handful of words to the stack.
    __ add(R11_scratch1, Rparent_frame_resize, Rtop_frame_size);
    generate_stack_overflow_check(R11_scratch1, R12_scratch2);
  }

  // Set up interpreter state registers.

  __ add(R18_locals, R15_esp, Rsize_of_parameters);
  __ ld(Rconst_pool, in_bytes(ConstMethod::constants_offset()), Rconst_method);
  __ ld(R27_constPoolCache, ConstantPool::cache_offset_in_bytes(), Rconst_pool);

  // Set method data pointer.
  if (ProfileInterpreter) {
    Label zero_continue;
    __ ld(R28_mdx, method_(method_data));
    __ cmpdi(CCR0, R28_mdx, 0);
    __ beq(CCR0, zero_continue);
    __ addi(R28_mdx, R28_mdx, in_bytes(MethodData::data_offset()));
    __ bind(zero_continue);
  }

  if (native_call) {
    __ li(R14_bcp, 0); // Must initialize.
  } else {
    __ add(R14_bcp, in_bytes(ConstMethod::codes_offset()), Rconst_method);
  }

  // Resize parent frame.
  __ mflr(R12_scratch2);
  __ neg(Rparent_frame_resize, Rparent_frame_resize);
  __ resize_frame(Rparent_frame_resize, R11_scratch1);
  __ std(R12_scratch2, _abi0(lr), R1_SP);

  // Get mirror and store it in the frame as GC root for this Method*.
  __ ld(Rmirror, ConstantPool::pool_holder_offset_in_bytes(), Rconst_pool);
  __ ld(Rmirror, in_bytes(Klass::java_mirror_offset()), Rmirror);
  __ resolve_oop_handle(Rmirror, R11_scratch1, R12_scratch2, MacroAssembler::PRESERVATION_FRAME_LR_GP_REGS);

  __ addi(R26_monitor, R1_SP, -frame::ijava_state_size);
  __ addi(R15_esp, R26_monitor, -Interpreter::stackElementSize);

  // Store values.
  __ std(R19_method, _ijava_state_neg(method), R1_SP);
  __ std(Rmirror, _ijava_state_neg(mirror), R1_SP);
  __ std(R18_locals, _ijava_state_neg(locals), R1_SP);
  __ std(R27_constPoolCache, _ijava_state_neg(cpoolCache), R1_SP);

  // Note: esp, bcp, monitor, mdx live in registers. Hence, the correct version can only
  // be found in the frame after save_interpreter_state is done. This is always true
  // for non-top frames. But when a signal occurs, dumping the top frame can go wrong,
  // because e.g. frame::interpreter_frame_bcp() will not access the correct value
  // (Enhanced Stack Trace).
  // The signal handler does not save the interpreter state into the frame.

  // We have to initialize some of these frame slots for native calls (accessed by GC).
  // Also initialize them for non-native calls for better tool support (even though
  // you may not get the most recent version as described above).
  __ li(R0, 0);
  __ std(R26_monitor, _ijava_state_neg(monitors), R1_SP);
  __ std(R14_bcp, _ijava_state_neg(bcp), R1_SP);
  if (ProfileInterpreter) { __ std(R28_mdx, _ijava_state_neg(mdx), R1_SP); }
  __ std(R15_esp, _ijava_state_neg(esp), R1_SP);
  __ std(R0, _ijava_state_neg(oop_tmp), R1_SP); // only used for native_call

  // Store sender's SP and this frame's top SP.
  __ subf(R12_scratch2, Rtop_frame_size, R1_SP);
  __ std(R21_sender_SP, _ijava_state_neg(sender_sp), R1_SP);
  __ std(R12_scratch2, _ijava_state_neg(top_frame_sp), R1_SP);

  // Push top frame.
  __ push_frame(Rtop_frame_size, R11_scratch1);
}

// End of helpers

address TemplateInterpreterGenerator::generate_math_entry(AbstractInterpreter::MethodKind kind) {

  // Decide what to do: Use same platform specific instructions and runtime calls as compilers.
  bool use_instruction = false;
  address runtime_entry = NULL;
  int num_args = 1;
  bool double_precision = true;

  // PPC64 specific:
  switch (kind) {
    case Interpreter::java_lang_math_sqrt: use_instruction = VM_Version::has_fsqrt(); break;
    case Interpreter::java_lang_math_abs:  use_instruction = true; break;
    case Interpreter::java_lang_math_fmaF:
    case Interpreter::java_lang_math_fmaD: use_instruction = UseFMA; break;
    default: break; // Fall back to runtime call.
  }

  switch (kind) {
    case Interpreter::java_lang_math_sin  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dsin);   break;
    case Interpreter::java_lang_math_cos  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dcos);   break;
    case Interpreter::java_lang_math_tan  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dtan);   break;
    case Interpreter::java_lang_math_abs  : /* run interpreted */ break;
    case Interpreter::java_lang_math_sqrt : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dsqrt);  break;
    case Interpreter::java_lang_math_log  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dlog);   break;
    case Interpreter::java_lang_math_log10: runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dlog10); break;
    case Interpreter::java_lang_math_pow  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dpow); num_args = 2; break;
    case Interpreter::java_lang_math_exp  : runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dexp);   break;
    case Interpreter::java_lang_math_fmaF : /* run interpreted */ num_args = 3; double_precision = false; break;
    case Interpreter::java_lang_math_fmaD : /* run interpreted */ num_args = 3; break;
    default: ShouldNotReachHere();
  }

  // Use normal entry if neither instruction nor runtime call is used.
  if (!use_instruction && runtime_entry == NULL) return NULL;

  address entry = __ pc();

  // Load arguments
  assert(num_args <= 13, "passed in registers");
  if (double_precision) {
    int offset = (2 * num_args - 1) * Interpreter::stackElementSize;
    for (int i = 0; i < num_args; ++i) {
      __ lfd(as_FloatRegister(F1_ARG1->encoding() + i), offset, R15_esp);
      offset -= 2 * Interpreter::stackElementSize;
    }
  } else {
    int offset = num_args * Interpreter::stackElementSize;
    for (int i = 0; i < num_args; ++i) {
      __ lfs(as_FloatRegister(F1_ARG1->encoding() + i), offset, R15_esp);
      offset -= Interpreter::stackElementSize;
    }
  }

  if (use_instruction) {
    switch (kind) {
      case Interpreter::java_lang_math_sqrt: __ fsqrt(F1_RET, F1);          break;
      case Interpreter::java_lang_math_abs:  __ fabs(F1_RET, F1);           break;
      case Interpreter::java_lang_math_fmaF: __ fmadds(F1_RET, F1, F2, F3); break;
      case Interpreter::java_lang_math_fmaD: __ fmadd(F1_RET, F1, F2, F3);  break;
      default: ShouldNotReachHere();
    }
  } else {
    // Comment: Can use tail call if the unextended frame is always C ABI compliant:
    //__ load_const_optimized(R12_scratch2, runtime_entry, R0);
    //__ call_c_and_return_to_caller(R12_scratch2);

    // Push a new C frame and save LR.
    __ save_LR_CR(R0);
    __ push_frame_reg_args(0, R11_scratch1);

    __ call_VM_leaf(runtime_entry);

    // Pop the C frame and restore LR.
    __ pop_frame();
    __ restore_LR_CR(R0);
  }

  // Restore caller sp for c2i case (from compiled) and for resized sender frame (from interpreted).
  __ resize_frame_absolute(R21_sender_SP, R11_scratch1, R0);
  __ blr();

  __ flush();

  return entry;
}

void TemplateInterpreterGenerator::bang_stack_shadow_pages(bool native_call) {
  // Quick & dirty stack overflow checking: bang the stack & handle trap.
  // Note that we do the banging after the frame is setup, since the exception
  // handling code expects to find a valid interpreter frame on the stack.
  // Doing the banging earlier fails if the caller frame is not an interpreter
  // frame.
  // (Also, the exception throwing code expects to unlock any synchronized
  // method receiever, so do the banging after locking the receiver.)

  // Bang each page in the shadow zone. We can't assume it's been done for
  // an interpreter frame with greater than a page of locals, so each page
  // needs to be checked.  Only true for non-native.
  const int page_size = os::vm_page_size();
  const int n_shadow_pages = ((int)StackOverflow::stack_shadow_zone_size()) / page_size;
  const int start_page = native_call ? n_shadow_pages : 1;
  BLOCK_COMMENT("bang_stack_shadow_pages:");
  for (int pages = start_page; pages <= n_shadow_pages; pages++) {
    __ bang_stack_with_offset(pages*page_size);
  }
}

// Interpreter stub for calling a native method. (asm interpreter)
// This sets up a somewhat different looking stack for calling the
// native method than the typical interpreter frame setup.
//
// On entry:
//   R19_method    - method
//   R16_thread    - JavaThread*
//   R15_esp       - intptr_t* sender tos
//
//   abstract stack (grows up)
//     [  IJava (caller of JNI callee)  ]  <-- ASP
//        ...
address TemplateInterpreterGenerator::generate_native_entry(bool synchronized) {

  address entry = __ pc();

  const bool inc_counter = UseCompiler || CountCompiledCalls || LogTouchedMethods;

  // -----------------------------------------------------------------------------
  // Allocate a new frame that represents the native callee (i2n frame).
  // This is not a full-blown interpreter frame, but in particular, the
  // following registers are valid after this:
  // - R19_method
  // - R18_local (points to start of arguments to native function)
  //
  //   abstract stack (grows up)
  //     [  IJava (caller of JNI callee)  ]  <-- ASP
  //        ...

  const Register signature_handler_fd = R11_scratch1;
  const Register pending_exception    = R0;
  const Register result_handler_addr  = R31;
  const Register native_method_fd     = R11_scratch1;
  const Register access_flags         = R22_tmp2;
  const Register active_handles       = R11_scratch1; // R26_monitor saved to state.
  const Register sync_state           = R12_scratch2;
  const Register sync_state_addr      = sync_state;   // Address is dead after use.
  const Register suspend_flags        = R11_scratch1;

  //=============================================================================
  // Allocate new frame and initialize interpreter state.

  Label exception_return;
  Label exception_return_sync_check;
  Label stack_overflow_return;

  // Generate new interpreter state and jump to stack_overflow_return in case of
  // a stack overflow.
  //generate_compute_interpreter_state(stack_overflow_return);

  Register size_of_parameters = R22_tmp2;

  generate_fixed_frame(true, size_of_parameters, noreg /* unused */);

  //=============================================================================
  // Increment invocation counter. On overflow, entry to JNI method
  // will be compiled.
  Label invocation_counter_overflow, continue_after_compile;
  if (inc_counter) {
    if (synchronized) {
      // Since at this point in the method invocation the exception handler
      // would try to exit the monitor of synchronized methods which hasn't
      // been entered yet, we set the thread local variable
      // _do_not_unlock_if_synchronized to true. If any exception was thrown by
      // runtime, exception handling i.e. unlock_if_synchronized_method will
      // check this thread local flag.
      // This flag has two effects, one is to force an unwind in the topmost
      // interpreter frame and not perform an unlock while doing so.
      __ li(R0, 1);
      __ stb(R0, in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()), R16_thread);
    }
    generate_counter_incr(&invocation_counter_overflow);

    BIND(continue_after_compile);
  }

  bang_stack_shadow_pages(true);

  if (inc_counter) {
    // Reset the _do_not_unlock_if_synchronized flag.
    if (synchronized) {
      __ li(R0, 0);
      __ stb(R0, in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()), R16_thread);
    }
  }

  // access_flags = method->access_flags();
  // Load access flags.
  assert(access_flags->is_nonvolatile(),
         "access_flags must be in a non-volatile register");
  // Type check.
  assert(4 == sizeof(AccessFlags), "unexpected field size");
  __ lwz(access_flags, method_(access_flags));

  // We don't want to reload R19_method and access_flags after calls
  // to some helper functions.
  assert(R19_method->is_nonvolatile(),
         "R19_method must be a non-volatile register");

  // Check for synchronized methods. Must happen AFTER invocation counter
  // check, so method is not locked if counter overflows.

  if (synchronized) {
    lock_method(access_flags, R11_scratch1, R12_scratch2, true);

    // Update monitor in state.
    __ ld(R11_scratch1, 0, R1_SP);
    __ std(R26_monitor, _ijava_state_neg(monitors), R11_scratch1);
  }

  // jvmti/jvmpi support
  __ notify_method_entry();

  //=============================================================================
  // Get and call the signature handler.

  __ ld(signature_handler_fd, method_(signature_handler));
  Label call_signature_handler;

  __ cmpdi(CCR0, signature_handler_fd, 0);
  __ bne(CCR0, call_signature_handler);

  // Method has never been called. Either generate a specialized
  // handler or point to the slow one.
  //
  // Pass parameter 'false' to avoid exception check in call_VM.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::prepare_native_call), R19_method, false);

  // Check for an exception while looking up the target method. If we
  // incurred one, bail.
  __ ld(pending_exception, thread_(pending_exception));
  __ cmpdi(CCR0, pending_exception, 0);
  __ bne(CCR0, exception_return_sync_check); // Has pending exception.

  // Reload signature handler, it may have been created/assigned in the meanwhile.
  __ ld(signature_handler_fd, method_(signature_handler));
  __ twi_0(signature_handler_fd); // Order wrt. load of klass mirror and entry point (isync is below).

  BIND(call_signature_handler);

  // Before we call the signature handler we push a new frame to
  // protect the interpreter frame volatile registers when we return
  // from jni but before we can get back to Java.

  // First set the frame anchor while the SP/FP registers are
  // convenient and the slow signature handler can use this same frame
  // anchor.

  // We have a TOP_IJAVA_FRAME here, which belongs to us.
  __ set_top_ijava_frame_at_SP_as_last_Java_frame(R1_SP, R12_scratch2/*tmp*/);

  // Now the interpreter frame (and its call chain) have been
  // invalidated and flushed. We are now protected against eager
  // being enabled in native code. Even if it goes eager the
  // registers will be reloaded as clean and we will invalidate after
  // the call so no spurious flush should be possible.

  // Call signature handler and pass locals address.
  //
  // Our signature handlers copy required arguments to the C stack
  // (outgoing C args), R3_ARG1 to R10_ARG8, and FARG1 to FARG13.
  __ mr(R3_ARG1, R18_locals);
#if !defined(ABI_ELFv2)
  __ ld(signature_handler_fd, 0, signature_handler_fd);
#endif

  __ call_stub(signature_handler_fd);

  // Remove the register parameter varargs slots we allocated in
  // compute_interpreter_state. SP+16 ends up pointing to the ABI
  // outgoing argument area.
  //
  // Not needed on PPC64.
  //__ add(SP, SP, Argument::n_register_parameters*BytesPerWord);

  assert(result_handler_addr->is_nonvolatile(), "result_handler_addr must be in a non-volatile register");
  // Save across call to native method.
  __ mr(result_handler_addr, R3_RET);

  __ isync(); // Acquire signature handler before trying to fetch the native entry point and klass mirror.

  // Set up fixed parameters and call the native method.
  // If the method is static, get mirror into R4_ARG2.
  {
    Label method_is_not_static;
    // Access_flags is non-volatile and still, no need to restore it.

    // Restore access flags.
    __ testbitdi(CCR0, R0, access_flags, JVM_ACC_STATIC_BIT);
    __ bfalse(CCR0, method_is_not_static);

    __ ld(R11_scratch1, _abi0(callers_sp), R1_SP);
    // Load mirror from interpreter frame.
    __ ld(R12_scratch2, _ijava_state_neg(mirror), R11_scratch1);
    // R4_ARG2 = &state->_oop_temp;
    __ addi(R4_ARG2, R11_scratch1, _ijava_state_neg(oop_tmp));
    __ std(R12_scratch2/*mirror*/, _ijava_state_neg(oop_tmp), R11_scratch1);
    BIND(method_is_not_static);
  }

  // At this point, arguments have been copied off the stack into
  // their JNI positions. Oops are boxed in-place on the stack, with
  // handles copied to arguments. The result handler address is in a
  // register.

  // Pass JNIEnv address as first parameter.
  __ addir(R3_ARG1, thread_(jni_environment));

  // Load the native_method entry before we change the thread state.
  __ ld(native_method_fd, method_(native_function));

  //=============================================================================
  // Transition from _thread_in_Java to _thread_in_native. As soon as
  // we make this change the safepoint code needs to be certain that
  // the last Java frame we established is good. The pc in that frame
  // just needs to be near here not an actual return address.

  // We use release_store_fence to update values like the thread state, where
  // we don't want the current thread to continue until all our prior memory
  // accesses (including the new thread state) are visible to other threads.
  __ li(R0, _thread_in_native);
  __ release();

  // TODO PPC port assert(4 == JavaThread::sz_thread_state(), "unexpected field size");
  __ stw(R0, thread_(thread_state));

  //=============================================================================
  // Call the native method. Argument registers must not have been
  // overwritten since "__ call_stub(signature_handler);" (except for
  // ARG1 and ARG2 for static methods).
  __ call_c(native_method_fd);

  __ li(R0, 0);
  __ ld(R11_scratch1, 0, R1_SP);
  __ std(R3_RET, _ijava_state_neg(lresult), R11_scratch1);
  __ stfd(F1_RET, _ijava_state_neg(fresult), R11_scratch1);
  __ std(R0/*mirror*/, _ijava_state_neg(oop_tmp), R11_scratch1); // reset

  // Note: C++ interpreter needs the following here:
  // The frame_manager_lr field, which we use for setting the last
  // java frame, gets overwritten by the signature handler. Restore
  // it now.
  //__ get_PC_trash_LR(R11_scratch1);
  //__ std(R11_scratch1, _top_ijava_frame_abi(frame_manager_lr), R1_SP);

  // Because of GC R19_method may no longer be valid.

  // Block, if necessary, before resuming in _thread_in_Java state.
  // In order for GC to work, don't clear the last_Java_sp until after
  // blocking.

  //=============================================================================
  // Switch thread to "native transition" state before reading the
  // synchronization state. This additional state is necessary
  // because reading and testing the synchronization state is not
  // atomic w.r.t. GC, as this scenario demonstrates: Java thread A,
  // in _thread_in_native state, loads _not_synchronized and is
  // preempted. VM thread changes sync state to synchronizing and
  // suspends threads for GC. Thread A is resumed to finish this
  // native method, but doesn't block here since it didn't see any
  // synchronization in progress, and escapes.

  // We use release_store_fence to update values like the thread state, where
  // we don't want the current thread to continue until all our prior memory
  // accesses (including the new thread state) are visible to other threads.
  __ li(R0/*thread_state*/, _thread_in_native_trans);
  __ release();
  __ stw(R0/*thread_state*/, thread_(thread_state));
  __ fence();

  // Now before we return to java we must look for a current safepoint
  // (a new safepoint can not start since we entered native_trans).
  // We must check here because a current safepoint could be modifying
  // the callers registers right this moment.

  // Acquire isn't strictly necessary here because of the fence, but
  // sync_state is declared to be volatile, so we do it anyway
  // (cmp-br-isync on one path, release (same as acquire on PPC64) on the other path).

  Label do_safepoint, sync_check_done;
  // No synchronization in progress nor yet synchronized.
  __ safepoint_poll(do_safepoint, sync_state, true /* at_return */, false /* in_nmethod */);

  // Not suspended.
  // TODO PPC port assert(4 == Thread::sz_suspend_flags(), "unexpected field size");
  __ lwz(suspend_flags, thread_(suspend_flags));
  __ cmpwi(CCR1, suspend_flags, 0);
  __ beq(CCR1, sync_check_done);

  __ bind(do_safepoint);
  __ isync();
  // Block. We do the call directly and leave the current
  // last_Java_frame setup undisturbed. We must save any possible
  // native result across the call. No oop is present.

  __ mr(R3_ARG1, R16_thread);
#if defined(ABI_ELFv2)
  __ call_c(CAST_FROM_FN_PTR(address, JavaThread::check_special_condition_for_native_trans),
            relocInfo::none);
#else
  __ call_c(CAST_FROM_FN_PTR(FunctionDescriptor*, JavaThread::check_special_condition_for_native_trans),
            relocInfo::none);
#endif

  __ bind(sync_check_done);

  //=============================================================================
  // <<<<<< Back in Interpreter Frame >>>>>

  // We are in thread_in_native_trans here and back in the normal
  // interpreter frame. We don't have to do anything special about
  // safepoints and we can switch to Java mode anytime we are ready.

  // Note: frame::interpreter_frame_result has a dependency on how the
  // method result is saved across the call to post_method_exit. For
  // native methods it assumes that the non-FPU/non-void result is
  // saved in _native_lresult and a FPU result in _native_fresult. If
  // this changes then the interpreter_frame_result implementation
  // will need to be updated too.

  // On PPC64, we have stored the result directly after the native call.

  //=============================================================================
  // Back in Java

  // We use release_store_fence to update values like the thread state, where
  // we don't want the current thread to continue until all our prior memory
  // accesses (including the new thread state) are visible to other threads.
  __ li(R0/*thread_state*/, _thread_in_Java);
  __ lwsync(); // Acquire safepoint and suspend state, release thread state.
  __ stw(R0/*thread_state*/, thread_(thread_state));

  if (CheckJNICalls) {
    // clear_pending_jni_exception_check
    __ load_const_optimized(R0, 0L);
    __ st_ptr(R0, JavaThread::pending_jni_exception_check_fn_offset(), R16_thread);
  }

  __ reset_last_Java_frame();

  // Jvmdi/jvmpi support. Whether we've got an exception pending or
  // not, and whether unlocking throws an exception or not, we notify
  // on native method exit. If we do have an exception, we'll end up
  // in the caller's context to handle it, so if we don't do the
  // notify here, we'll drop it on the floor.
  __ notify_method_exit(true/*native method*/,
                        ilgl /*illegal state (not used for native methods)*/,
                        InterpreterMacroAssembler::NotifyJVMTI,
                        false /*check_exceptions*/);

  //=============================================================================
  // Handle exceptions

  if (synchronized) {
    __ unlock_object(R26_monitor); // Can also unlock methods.
  }

  // Reset active handles after returning from native.
  // thread->active_handles()->clear();
  __ ld(active_handles, thread_(active_handles));
  // TODO PPC port assert(4 == JNIHandleBlock::top_size_in_bytes(), "unexpected field size");
  __ li(R0, 0);
  __ stw(R0, JNIHandleBlock::top_offset_in_bytes(), active_handles);

  Label exception_return_sync_check_already_unlocked;
  __ ld(R0/*pending_exception*/, thread_(pending_exception));
  __ cmpdi(CCR0, R0/*pending_exception*/, 0);
  __ bne(CCR0, exception_return_sync_check_already_unlocked);

  //-----------------------------------------------------------------------------
  // No exception pending.

  // Move native method result back into proper registers and return.
  // Invoke result handler (may unbox/promote).
  __ ld(R11_scratch1, 0, R1_SP);
  __ ld(R3_RET, _ijava_state_neg(lresult), R11_scratch1);
  __ lfd(F1_RET, _ijava_state_neg(fresult), R11_scratch1);
  __ call_stub(result_handler_addr);

  __ merge_frames(/*top_frame_sp*/ R21_sender_SP, /*return_pc*/ R0, R11_scratch1, R12_scratch2);

  // Must use the return pc which was loaded from the caller's frame
  // as the VM uses return-pc-patching for deoptimization.
  __ mtlr(R0);
  __ blr();

  //-----------------------------------------------------------------------------
  // An exception is pending. We call into the runtime only if the
  // caller was not interpreted. If it was interpreted the
  // interpreter will do the correct thing. If it isn't interpreted
  // (call stub/compiled code) we will change our return and continue.

  BIND(exception_return_sync_check);

  if (synchronized) {
    __ unlock_object(R26_monitor); // Can also unlock methods.
  }
  BIND(exception_return_sync_check_already_unlocked);

  const Register return_pc = R31;

  __ ld(return_pc, 0, R1_SP);
  __ ld(return_pc, _abi0(lr), return_pc);

  // Get the address of the exception handler.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::exception_handler_for_return_address),
                  R16_thread,
                  return_pc /* return pc */);
  __ merge_frames(/*top_frame_sp*/ R21_sender_SP, noreg, R11_scratch1, R12_scratch2);

  // Load the PC of the the exception handler into LR.
  __ mtlr(R3_RET);

  // Load exception into R3_ARG1 and clear pending exception in thread.
  __ ld(R3_ARG1/*exception*/, thread_(pending_exception));
  __ li(R4_ARG2, 0);
  __ std(R4_ARG2, thread_(pending_exception));

  // Load the original return pc into R4_ARG2.
  __ mr(R4_ARG2/*issuing_pc*/, return_pc);

  // Return to exception handler.
  __ blr();

  //=============================================================================
  // Counter overflow.

  if (inc_counter) {
    // Handle invocation counter overflow.
    __ bind(invocation_counter_overflow);

    generate_counter_overflow(continue_after_compile);
  }

  return entry;
}

// Generic interpreted method entry to (asm) interpreter.
//
address TemplateInterpreterGenerator::generate_normal_entry(bool synchronized) {
  bool inc_counter = UseCompiler || CountCompiledCalls || LogTouchedMethods;
  address entry = __ pc();
  // Generate the code to allocate the interpreter stack frame.
  Register Rsize_of_parameters = R4_ARG2, // Written by generate_fixed_frame.
           Rsize_of_locals     = R5_ARG3; // Written by generate_fixed_frame.

  // Does also a stack check to assure this frame fits on the stack.
  generate_fixed_frame(false, Rsize_of_parameters, Rsize_of_locals);

  // --------------------------------------------------------------------------
  // Zero out non-parameter locals.
  // Note: *Always* zero out non-parameter locals as Sparc does. It's not
  // worth to ask the flag, just do it.
  Register Rslot_addr = R6_ARG4,
           Rnum       = R7_ARG5;
  Label Lno_locals, Lzero_loop;

  // Set up the zeroing loop.
  __ subf(Rnum, Rsize_of_parameters, Rsize_of_locals);
  __ subf(Rslot_addr, Rsize_of_parameters, R18_locals);
  __ srdi_(Rnum, Rnum, Interpreter::logStackElementSize);
  __ beq(CCR0, Lno_locals);
  __ li(R0, 0);
  __ mtctr(Rnum);

  // The zero locals loop.
  __ bind(Lzero_loop);
  __ std(R0, 0, Rslot_addr);
  __ addi(Rslot_addr, Rslot_addr, -Interpreter::stackElementSize);
  __ bdnz(Lzero_loop);

  __ bind(Lno_locals);

  // --------------------------------------------------------------------------
  // Counter increment and overflow check.
  Label invocation_counter_overflow;
  Label continue_after_compile;
  if (inc_counter || ProfileInterpreter) {

    Register Rdo_not_unlock_if_synchronized_addr = R11_scratch1;
    if (synchronized) {
      // Since at this point in the method invocation the exception handler
      // would try to exit the monitor of synchronized methods which hasn't
      // been entered yet, we set the thread local variable
      // _do_not_unlock_if_synchronized to true. If any exception was thrown by
      // runtime, exception handling i.e. unlock_if_synchronized_method will
      // check this thread local flag.
      // This flag has two effects, one is to force an unwind in the topmost
      // interpreter frame and not perform an unlock while doing so.
      __ li(R0, 1);
      __ stb(R0, in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()), R16_thread);
    }

    // Argument and return type profiling.
    __ profile_parameters_type(R3_ARG1, R4_ARG2, R5_ARG3, R6_ARG4);

    // Increment invocation counter and check for overflow.
    if (inc_counter) {
      generate_counter_incr(&invocation_counter_overflow);
    }

    __ bind(continue_after_compile);
  }

  bang_stack_shadow_pages(false);

  if (inc_counter || ProfileInterpreter) {
    // Reset the _do_not_unlock_if_synchronized flag.
    if (synchronized) {
      __ li(R0, 0);
      __ stb(R0, in_bytes(JavaThread::do_not_unlock_if_synchronized_offset()), R16_thread);
    }
  }

  // --------------------------------------------------------------------------
  // Locking of synchronized methods. Must happen AFTER invocation_counter
  // check and stack overflow check, so method is not locked if overflows.
  if (synchronized) {
    lock_method(R3_ARG1, R4_ARG2, R5_ARG3);
  }
#ifdef ASSERT
  else {
    Label Lok;
    __ lwz(R0, in_bytes(Method::access_flags_offset()), R19_method);
    __ andi_(R0, R0, JVM_ACC_SYNCHRONIZED);
    __ asm_assert_eq("method needs synchronization");
    __ bind(Lok);
  }
#endif // ASSERT

  __ verify_thread();

  // --------------------------------------------------------------------------
  // JVMTI support
  __ notify_method_entry();

  // --------------------------------------------------------------------------
  // Start executing instructions.
  __ dispatch_next(vtos);

  // --------------------------------------------------------------------------
  if (inc_counter) {
    // Handle invocation counter overflow.
    __ bind(invocation_counter_overflow);
    generate_counter_overflow(continue_after_compile);
  }
  return entry;
}

// CRC32 Intrinsics.
//
// Contract on scratch and work registers.
// =======================================
//
// On ppc, the register set {R2..R12} is available in the interpreter as scratch/work registers.
// You should, however, keep in mind that {R3_ARG1..R10_ARG8} is the C-ABI argument register set.
// You can't rely on these registers across calls.
//
// The generators for CRC32_update and for CRC32_updateBytes use the
// scratch/work register set internally, passing the work registers
// as arguments to the MacroAssembler emitters as required.
//
// R3_ARG1..R6_ARG4 are preset to hold the incoming java arguments.
// Their contents is not constant but may change according to the requirements
// of the emitted code.
//
// All other registers from the scratch/work register set are used "internally"
// and contain garbage (i.e. unpredictable values) once blr() is reached.
// Basically, only R3_RET contains a defined value which is the function result.
//
/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.update(int crc, int b)
 */
address TemplateInterpreterGenerator::generate_CRC32_update_entry() {
  if (UseCRC32Intrinsics) {
    address start = __ pc();  // Remember stub start address (is rtn value).
    Label slow_path;

    // Safepoint check
    const Register sync_state = R11_scratch1;
    __ safepoint_poll(slow_path, sync_state, false /* at_return */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we not even call stub code (we generate the code inline)
    // and there is no safepoint on this path.

    // Load java parameters.
    // R15_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = R15_esp;
    const Register crc     = R3_ARG1;  // crc value
    const Register data    = R4_ARG2;
    const Register table   = R5_ARG3;  // address of crc32 table

    BLOCK_COMMENT("CRC32_update {");

    // Arguments are reversed on java expression stack
#ifdef VM_LITTLE_ENDIAN
    int data_offs = 0+1*wordSize;      // (stack) address of byte value. Emitter expects address, not value.
                                       // Being passed as an int, the single byte is at offset +0.
#else
    int data_offs = 3+1*wordSize;      // (stack) address of byte value. Emitter expects address, not value.
                                       // Being passed from java as an int, the single byte is at offset +3.
#endif
    __ lwz(crc, 2*wordSize, argP);     // Current crc state, zero extend to 64 bit to have a clean register.
    __ lbz(data, data_offs, argP);     // Byte from buffer, zero-extended.
    __ load_const_optimized(table, StubRoutines::crc_table_addr(), R0);
    __ kernel_crc32_singleByteReg(crc, data, table, true);

    // Restore caller sp for c2i case (from compiled) and for resized sender frame (from interpreted).
    __ resize_frame_absolute(R21_sender_SP, R11_scratch1, R0);
    __ blr();

    // Generate a vanilla native entry as the slow path.
    BLOCK_COMMENT("} CRC32_update");
    BIND(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native), R11_scratch1);
    return start;
  }

  return NULL;
}

/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.updateBytes(     int crc, byte[] b,  int off, int len)
 *   int java.util.zip.CRC32.updateByteBuffer(int crc, long* buf, int off, int len)
 */
address TemplateInterpreterGenerator::generate_CRC32_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32Intrinsics) {
    address start = __ pc();  // Remember stub start address (is rtn value).
    Label slow_path;

    // Safepoint check
    const Register sync_state = R11_scratch1;
    __ safepoint_poll(slow_path, sync_state, false /* at_return */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we not even call stub code (we generate the code inline)
    // and there is no safepoint on this path.

    // Load parameters.
    // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = R15_esp;
    const Register crc     = R3_ARG1;  // crc value
    const Register data    = R4_ARG2;  // address of java byte array
    const Register dataLen = R5_ARG3;  // source data len
    const Register tmp     = R11_scratch1;

    // Arguments are reversed on java expression stack.
    // Calculate address of start element.
    if (kind == Interpreter::java_util_zip_CRC32_updateByteBuffer) { // Used for "updateByteBuffer direct".
      BLOCK_COMMENT("CRC32_updateByteBuffer {");
      // crc     @ (SP + 5W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to long array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off
      __ ld(  data,    3*wordSize, argP);  // start of byte buffer
      __ lwa( tmp,     2*wordSize, argP);  // byte buffer offset
      __ lwa( dataLen, 1*wordSize, argP);  // #bytes to process
      __ lwz( crc,     5*wordSize, argP);  // current crc state
      __ add( data, data, tmp);            // Add byte buffer offset.
    } else {                                                         // Used for "updateBytes update".
      BLOCK_COMMENT("CRC32_updateBytes {");
      // crc     @ (SP + 4W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to byte array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off + base_offset
      __ ld(  data,    3*wordSize, argP);  // start of byte buffer
      __ lwa( tmp,     2*wordSize, argP);  // byte buffer offset
      __ lwa( dataLen, 1*wordSize, argP);  // #bytes to process
      __ add( data, data, tmp);            // add byte buffer offset
      __ lwz( crc,     4*wordSize, argP);  // current crc state
      __ addi(data, data, arrayOopDesc::base_offset_in_bytes(T_BYTE));
    }

    __ crc32(crc, data, dataLen, R2, R6, R7, R8, R9, R10, R11, R12, false);

    // Restore caller sp for c2i case (from compiled) and for resized sender frame (from interpreted).
    __ resize_frame_absolute(R21_sender_SP, R11_scratch1, R0);
    __ blr();

    // Generate a vanilla native entry as the slow path.
    BLOCK_COMMENT("} CRC32_updateBytes(Buffer)");
    BIND(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native), R11_scratch1);
    return start;
  }

  return NULL;
}


/**
 * Method entry for intrinsic-candidate (non-native) methods:
 *   int java.util.zip.CRC32C.updateBytes(           int crc, byte[] b,  int off, int end)
 *   int java.util.zip.CRC32C.updateDirectByteBuffer(int crc, long* buf, int off, int end)
 * Unlike CRC32, CRC32C does not have any methods marked as native
 * CRC32C also uses an "end" variable instead of the length variable CRC32 uses
 **/
address TemplateInterpreterGenerator::generate_CRC32C_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32CIntrinsics) {
    address start = __ pc();  // Remember stub start address (is rtn value).

    // We don't generate local frame and don't align stack because
    // we not even call stub code (we generate the code inline)
    // and there is no safepoint on this path.

    // Load parameters.
    // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = R15_esp;
    const Register crc     = R3_ARG1;  // crc value
    const Register data    = R4_ARG2;  // address of java byte array
    const Register dataLen = R5_ARG3;  // source data len
    const Register tmp     = R11_scratch1;

    // Arguments are reversed on java expression stack.
    // Calculate address of start element.
    if (kind == Interpreter::java_util_zip_CRC32C_updateDirectByteBuffer) { // Used for "updateDirectByteBuffer".
      BLOCK_COMMENT("CRC32C_updateDirectByteBuffer {");
      // crc     @ (SP + 5W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to long array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off
      __ ld(  data,    3*wordSize, argP);  // start of byte buffer
      __ lwa( tmp,     2*wordSize, argP);  // byte buffer offset
      __ lwa( dataLen, 1*wordSize, argP);  // #bytes to process
      __ lwz( crc,     5*wordSize, argP);  // current crc state
      __ add( data, data, tmp);            // Add byte buffer offset.
      __ sub( dataLen, dataLen, tmp);      // (end_index - offset)
    } else {                                                         // Used for "updateBytes update".
      BLOCK_COMMENT("CRC32C_updateBytes {");
      // crc     @ (SP + 4W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to byte array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off + base_offset
      __ ld(  data,    3*wordSize, argP);  // start of byte buffer
      __ lwa( tmp,     2*wordSize, argP);  // byte buffer offset
      __ lwa( dataLen, 1*wordSize, argP);  // #bytes to process
      __ add( data, data, tmp);            // add byte buffer offset
      __ sub( dataLen, dataLen, tmp);      // (end_index - offset)
      __ lwz( crc,     4*wordSize, argP);  // current crc state
      __ addi(data, data, arrayOopDesc::base_offset_in_bytes(T_BYTE));
    }

    __ crc32(crc, data, dataLen, R2, R6, R7, R8, R9, R10, R11, R12, true);

    // Restore caller sp for c2i case (from compiled) and for resized sender frame (from interpreted).
    __ resize_frame_absolute(R21_sender_SP, R11_scratch1, R0);
    __ blr();

    BLOCK_COMMENT("} CRC32C_update{Bytes|DirectByteBuffer}");
    return start;
  }

  return NULL;
}

// =============================================================================
// Exceptions

void TemplateInterpreterGenerator::generate_throw_exception() {
  Register Rexception    = R17_tos,
           Rcontinuation = R3_RET;

  // --------------------------------------------------------------------------
  // Entry point if an method returns with a pending exception (rethrow).
  Interpreter::_rethrow_exception_entry = __ pc();
  {
    __ restore_interpreter_state(R11_scratch1); // Sets R11_scratch1 = fp.
    __ ld(R12_scratch2, _ijava_state_neg(top_frame_sp), R11_scratch1);
    __ resize_frame_absolute(R12_scratch2, R11_scratch1, R0);

    // Compiled code destroys templateTableBase, reload.
    __ load_const_optimized(R25_templateTableBase, (address)Interpreter::dispatch_table((TosState)0), R11_scratch1);
  }

  // Entry point if a interpreted method throws an exception (throw).
  Interpreter::_throw_exception_entry = __ pc();
  {
    __ mr(Rexception, R3_RET);

    __ verify_thread();
    __ verify_oop(Rexception);

    // Expression stack must be empty before entering the VM in case of an exception.
    __ empty_expression_stack();
    // Find exception handler address and preserve exception oop.
    // Call C routine to find handler and jump to it.
    __ call_VM(Rexception, CAST_FROM_FN_PTR(address, InterpreterRuntime::exception_handler_for_exception), Rexception);
    __ mtctr(Rcontinuation);
    // Push exception for exception handler bytecodes.
    __ push_ptr(Rexception);

    // Jump to exception handler (may be remove activation entry!).
    __ bctr();
  }

  // If the exception is not handled in the current frame the frame is
  // removed and the exception is rethrown (i.e. exception
  // continuation is _rethrow_exception).
  //
  // Note: At this point the bci is still the bxi for the instruction
  // which caused the exception and the expression stack is
  // empty. Thus, for any VM calls at this point, GC will find a legal
  // oop map (with empty expression stack).

  // In current activation
  // tos: exception
  // bcp: exception bcp

  // --------------------------------------------------------------------------
  // JVMTI PopFrame support

  Interpreter::_remove_activation_preserving_args_entry = __ pc();
  {
    // Set the popframe_processing bit in popframe_condition indicating that we are
    // currently handling popframe, so that call_VMs that may happen later do not
    // trigger new popframe handling cycles.
    __ lwz(R11_scratch1, in_bytes(JavaThread::popframe_condition_offset()), R16_thread);
    __ ori(R11_scratch1, R11_scratch1, JavaThread::popframe_processing_bit);
    __ stw(R11_scratch1, in_bytes(JavaThread::popframe_condition_offset()), R16_thread);

    // Empty the expression stack, as in normal exception handling.
    __ empty_expression_stack();
    __ unlock_if_synchronized_method(vtos, /* throw_monitor_exception */ false, /* install_monitor_exception */ false);

    // Check to see whether we are returning to a deoptimized frame.
    // (The PopFrame call ensures that the caller of the popped frame is
    // either interpreted or compiled and deoptimizes it if compiled.)
    // Note that we don't compare the return PC against the
    // deoptimization blob's unpack entry because of the presence of
    // adapter frames in C2.
    Label Lcaller_not_deoptimized;
    Register return_pc = R3_ARG1;
    __ ld(return_pc, 0, R1_SP);
    __ ld(return_pc, _abi0(lr), return_pc);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::interpreter_contains), return_pc);
    __ cmpdi(CCR0, R3_RET, 0);
    __ bne(CCR0, Lcaller_not_deoptimized);

    // The deoptimized case.
    // In this case, we can't call dispatch_next() after the frame is
    // popped, but instead must save the incoming arguments and restore
    // them after deoptimization has occurred.
    __ ld(R4_ARG2, in_bytes(Method::const_offset()), R19_method);
    __ lhz(R4_ARG2 /* number of params */, in_bytes(ConstMethod::size_of_parameters_offset()), R4_ARG2);
    __ slwi(R4_ARG2, R4_ARG2, Interpreter::logStackElementSize);
    __ addi(R5_ARG3, R18_locals, Interpreter::stackElementSize);
    __ subf(R5_ARG3, R4_ARG2, R5_ARG3);
    // Save these arguments.
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::popframe_preserve_args), R16_thread, R4_ARG2, R5_ARG3);

    // Inform deoptimization that it is responsible for restoring these arguments.
    __ load_const_optimized(R11_scratch1, JavaThread::popframe_force_deopt_reexecution_bit);
    __ stw(R11_scratch1, in_bytes(JavaThread::popframe_condition_offset()), R16_thread);

    // Return from the current method into the deoptimization blob. Will eventually
    // end up in the deopt interpeter entry, deoptimization prepared everything that
    // we will reexecute the call that called us.
    __ merge_frames(/*top_frame_sp*/ R21_sender_SP, /*reload return_pc*/ return_pc, R11_scratch1, R12_scratch2);
    __ mtlr(return_pc);
    __ blr();

    // The non-deoptimized case.
    __ bind(Lcaller_not_deoptimized);

    // Clear the popframe condition flag.
    __ li(R0, 0);
    __ stw(R0, in_bytes(JavaThread::popframe_condition_offset()), R16_thread);

    // Get out of the current method and re-execute the call that called us.
    __ merge_frames(/*top_frame_sp*/ R21_sender_SP, /*return_pc*/ noreg, R11_scratch1, R12_scratch2);
    __ restore_interpreter_state(R11_scratch1);
    __ ld(R12_scratch2, _ijava_state_neg(top_frame_sp), R11_scratch1);
    __ resize_frame_absolute(R12_scratch2, R11_scratch1, R0);
    if (ProfileInterpreter) {
      __ set_method_data_pointer_for_bcp();
      __ ld(R11_scratch1, 0, R1_SP);
      __ std(R28_mdx, _ijava_state_neg(mdx), R11_scratch1);
    }
#if INCLUDE_JVMTI
    Label L_done;

    __ lbz(R11_scratch1, 0, R14_bcp);
    __ cmpwi(CCR0, R11_scratch1, Bytecodes::_invokestatic);
    __ bne(CCR0, L_done);

    // The member name argument must be restored if _invokestatic is re-executed after a PopFrame call.
    // Detect such a case in the InterpreterRuntime function and return the member name argument, or NULL.
    __ ld(R4_ARG2, 0, R18_locals);
    __ call_VM(R4_ARG2, CAST_FROM_FN_PTR(address, InterpreterRuntime::member_name_arg_or_null), R4_ARG2, R19_method, R14_bcp);

    __ cmpdi(CCR0, R4_ARG2, 0);
    __ beq(CCR0, L_done);
    __ std(R4_ARG2, wordSize, R15_esp);
    __ bind(L_done);
#endif // INCLUDE_JVMTI
    __ dispatch_next(vtos);
  }
  // end of JVMTI PopFrame support

  // --------------------------------------------------------------------------
  // Remove activation exception entry.
  // This is jumped to if an interpreted method can't handle an exception itself
  // (we come from the throw/rethrow exception entry above). We're going to call
  // into the VM to find the exception handler in the caller, pop the current
  // frame and return the handler we calculated.
  Interpreter::_remove_activation_entry = __ pc();
  {
    __ pop_ptr(Rexception);
    __ verify_thread();
    __ verify_oop(Rexception);
    __ std(Rexception, in_bytes(JavaThread::vm_result_offset()), R16_thread);

    __ unlock_if_synchronized_method(vtos, /* throw_monitor_exception */ false, true);
    __ notify_method_exit(false, vtos, InterpreterMacroAssembler::SkipNotifyJVMTI, false);

    __ get_vm_result(Rexception);

    // We are done with this activation frame; find out where to go next.
    // The continuation point will be an exception handler, which expects
    // the following registers set up:
    //
    // RET:  exception oop
    // ARG2: Issuing PC (see generate_exception_blob()), only used if the caller is compiled.

    Register return_pc = R31; // Needs to survive the runtime call.
    __ ld(return_pc, 0, R1_SP);
    __ ld(return_pc, _abi0(lr), return_pc);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::exception_handler_for_return_address), R16_thread, return_pc);

    // Remove the current activation.
    __ merge_frames(/*top_frame_sp*/ R21_sender_SP, /*return_pc*/ noreg, R11_scratch1, R12_scratch2);

    __ mr(R4_ARG2, return_pc);
    __ mtlr(R3_RET);
    __ mr(R3_RET, Rexception);
    __ blr();
  }
}

// JVMTI ForceEarlyReturn support.
// Returns "in the middle" of a method with a "fake" return value.
address TemplateInterpreterGenerator::generate_earlyret_entry_for(TosState state) {

  Register Rscratch1 = R11_scratch1,
           Rscratch2 = R12_scratch2;

  address entry = __ pc();
  __ empty_expression_stack();

  __ load_earlyret_value(state, Rscratch1);

  __ ld(Rscratch1, in_bytes(JavaThread::jvmti_thread_state_offset()), R16_thread);
  // Clear the earlyret state.
  __ li(R0, 0);
  __ stw(R0, in_bytes(JvmtiThreadState::earlyret_state_offset()), Rscratch1);

  __ remove_activation(state, false, false);
  // Copied from TemplateTable::_return.
  // Restoration of lr done by remove_activation.
  switch (state) {
    // Narrow result if state is itos but result type is smaller.
    case btos:
    case ztos:
    case ctos:
    case stos:
    case itos: __ narrow(R17_tos); /* fall through */
    case ltos:
    case atos: __ mr(R3_RET, R17_tos); break;
    case ftos:
    case dtos: __ fmr(F1_RET, F15_ftos); break;
    case vtos: // This might be a constructor. Final fields (and volatile fields on PPC64) need
               // to get visible before the reference to the object gets stored anywhere.
               __ membar(Assembler::StoreStore); break;
    default  : ShouldNotReachHere();
  }
  __ blr();

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
  __ align(32, 12, 24); // align L
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
  //__ flush_bundle();
  address entry = __ pc();

  const char *bname = NULL;
  uint tsize = 0;
  switch(state) {
  case ftos:
    bname = "trace_code_ftos {";
    tsize = 2;
    break;
  case btos:
    bname = "trace_code_btos {";
    tsize = 2;
    break;
  case ztos:
    bname = "trace_code_ztos {";
    tsize = 2;
    break;
  case ctos:
    bname = "trace_code_ctos {";
    tsize = 2;
    break;
  case stos:
    bname = "trace_code_stos {";
    tsize = 2;
    break;
  case itos:
    bname = "trace_code_itos {";
    tsize = 2;
    break;
  case ltos:
    bname = "trace_code_ltos {";
    tsize = 3;
    break;
  case atos:
    bname = "trace_code_atos {";
    tsize = 2;
    break;
  case vtos:
    // Note: In case of vtos, the topmost of stack value could be a int or doubl
    // In case of a double (2 slots) we won't see the 2nd stack value.
    // Maybe we simply should print the topmost 3 stack slots to cope with the problem.
    bname = "trace_code_vtos {";
    tsize = 2;

    break;
  case dtos:
    bname = "trace_code_dtos {";
    tsize = 3;
    break;
  default:
    ShouldNotReachHere();
  }
  BLOCK_COMMENT(bname);

  // Support short-cut for TraceBytecodesAt.
  // Don't call into the VM if we don't want to trace to speed up things.
  Label Lskip_vm_call;
  if (TraceBytecodesAt > 0 && TraceBytecodesAt < max_intx) {
    int offs1 = __ load_const_optimized(R11_scratch1, (address) &TraceBytecodesAt, R0, true);
    int offs2 = __ load_const_optimized(R12_scratch2, (address) &BytecodeCounter::_counter_value, R0, true);
    __ ld(R11_scratch1, offs1, R11_scratch1);
    __ lwa(R12_scratch2, offs2, R12_scratch2);
    __ cmpd(CCR0, R12_scratch2, R11_scratch1);
    __ blt(CCR0, Lskip_vm_call);
  }

  __ push(state);
  // Load 2 topmost expression stack values.
  __ ld(R6_ARG4, tsize*Interpreter::stackElementSize, R15_esp);
  __ ld(R5_ARG3, Interpreter::stackElementSize, R15_esp);
  __ mflr(R31);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::trace_bytecode), /* unused */ R4_ARG2, R5_ARG3, R6_ARG4, false);
  __ mtlr(R31);
  __ pop(state);

  if (TraceBytecodesAt > 0 && TraceBytecodesAt < max_intx) {
    __ bind(Lskip_vm_call);
  }
  __ blr();
  BLOCK_COMMENT("} trace_code");
  return entry;
}

void TemplateInterpreterGenerator::count_bytecode() {
  int offs = __ load_const_optimized(R11_scratch1, (address) &BytecodeCounter::_counter_value, R12_scratch2, true);
  __ lwz(R12_scratch2, offs, R11_scratch1);
  __ addi(R12_scratch2, R12_scratch2, 1);
  __ stw(R12_scratch2, offs, R11_scratch1);
}

void TemplateInterpreterGenerator::histogram_bytecode(Template* t) {
  int offs = __ load_const_optimized(R11_scratch1, (address) &BytecodeHistogram::_counters[t->bytecode()], R12_scratch2, true);
  __ lwz(R12_scratch2, offs, R11_scratch1);
  __ addi(R12_scratch2, R12_scratch2, 1);
  __ stw(R12_scratch2, offs, R11_scratch1);
}

void TemplateInterpreterGenerator::histogram_bytecode_pair(Template* t) {
  const Register addr = R11_scratch1,
                 tmp  = R12_scratch2;
  // Get index, shift out old bytecode, bring in new bytecode, and store it.
  // _index = (_index >> log2_number_of_codes) |
  //          (bytecode << log2_number_of_codes);
  int offs1 = __ load_const_optimized(addr, (address)&BytecodePairHistogram::_index, tmp, true);
  __ lwz(tmp, offs1, addr);
  __ srwi(tmp, tmp, BytecodePairHistogram::log2_number_of_codes);
  __ ori(tmp, tmp, ((int) t->bytecode()) << BytecodePairHistogram::log2_number_of_codes);
  __ stw(tmp, offs1, addr);

  // Bump bucket contents.
  // _counters[_index] ++;
  int offs2 = __ load_const_optimized(addr, (address)&BytecodePairHistogram::_counters, R0, true);
  __ sldi(tmp, tmp, LogBytesPerInt);
  __ add(addr, tmp, addr);
  __ lwz(tmp, offs2, addr);
  __ addi(tmp, tmp, 1);
  __ stw(tmp, offs2, addr);
}

void TemplateInterpreterGenerator::trace_bytecode(Template* t) {
  // Call a little run-time stub to avoid blow-up for each bytecode.
  // The run-time runtime saves the right registers, depending on
  // the tosca in-state for the given template.

  assert(Interpreter::trace_code(t->tos_in()) != NULL,
         "entry must have been generated");

  // Note: we destroy LR here.
  __ bl(Interpreter::trace_code(t->tos_in()));
}

void TemplateInterpreterGenerator::stop_interpreter_at() {
  Label L;
  int offs1 = __ load_const_optimized(R11_scratch1, (address) &StopInterpreterAt, R0, true);
  int offs2 = __ load_const_optimized(R12_scratch2, (address) &BytecodeCounter::_counter_value, R0, true);
  __ ld(R11_scratch1, offs1, R11_scratch1);
  __ lwa(R12_scratch2, offs2, R12_scratch2);
  __ cmpd(CCR0, R12_scratch2, R11_scratch1);
  __ bne(CCR0, L);
  __ illtrap();
  __ bind(L);
}

#endif // !PRODUCT
