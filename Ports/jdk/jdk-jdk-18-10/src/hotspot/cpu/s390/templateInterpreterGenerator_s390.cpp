/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2020 SAP SE. All rights reserved.
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
#include "interpreter/abstractInterpreter.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "interpreter/templateTable.hpp"
#include "oops/arrayOop.hpp"
#include "oops/methodData.hpp"
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


// Size of interpreter code.  Increase if too small.  Interpreter will
// fail with a guarantee ("not enough space for interpreter generation");
// if too small.
// Run with +PrintInterpreter to get the VM to print out the size.
// Max size with JVMTI
int TemplateInterpreter::InterpreterCodeSize = 320*K;

#undef  __
#ifdef PRODUCT
  #define __ _masm->
#else
  #define __ _masm->
//  #define __ (Verbose ? (_masm->block_comment(FILE_AND_LINE),_masm):_masm)->
#endif

#define BLOCK_COMMENT(str) __ block_comment(str)
#define BIND(label)        __ bind(label); BLOCK_COMMENT(#label ":")

#define oop_tmp_offset     _z_ijava_state_neg(oop_tmp)

//-----------------------------------------------------------------------------

address TemplateInterpreterGenerator::generate_slow_signature_handler() {
  //
  // New slow_signature handler that respects the z/Architecture
  // C calling conventions.
  //
  // We get called by the native entry code with our output register
  // area == 8. First we call InterpreterRuntime::get_result_handler
  // to copy the pointer to the signature string temporarily to the
  // first C-argument and to return the result_handler in
  // Z_RET. Since native_entry will copy the jni-pointer to the
  // first C-argument slot later on, it's OK to occupy this slot
  // temporarily. Then we copy the argument list on the java
  // expression stack into native varargs format on the native stack
  // and load arguments into argument registers. Integer arguments in
  // the varargs vector will be sign-extended to 8 bytes.
  //
  // On entry:
  //   Z_ARG1  - intptr_t*       Address of java argument list in memory.
  //   Z_state - zeroInterpreter* Address of interpreter state for
  //                              this method
  //   Z_method
  //
  // On exit (just before return instruction):
  //   Z_RET contains the address of the result_handler.
  //   Z_ARG2 is not updated for static methods and contains "this" otherwise.
  //   Z_ARG3-Z_ARG5 contain the first 3 arguments of types other than float and double.
  //   Z_FARG1-Z_FARG4 contain the first 4 arguments of type float or double.

  const int LogSizeOfCase = 3;

  const int max_fp_register_arguments   = Argument::n_float_register_parameters;
  const int max_int_register_arguments  = Argument::n_register_parameters - 2;  // First 2 are reserved.

  const Register arg_java       = Z_tmp_2;
  const Register arg_c          = Z_tmp_3;
  const Register signature      = Z_R1_scratch; // Is a string.
  const Register fpcnt          = Z_R0_scratch;
  const Register argcnt         = Z_tmp_4;
  const Register intSlot        = Z_tmp_1;
  const Register sig_end        = Z_tmp_1; // Assumed end of signature (only used in do_object).
  const Register target_sp      = Z_tmp_1;
  const FloatRegister floatSlot = Z_F1;

  const int d_signature         = _z_abi(gpr6); // Only spill space, register contents not affected.
  const int d_fpcnt             = _z_abi(gpr7); // Only spill space, register contents not affected.

  unsigned int entry_offset = __ offset();

  BLOCK_COMMENT("slow_signature_handler {");

  // We use target_sp for storing arguments in the C frame.
  __ save_return_pc();
  __ push_frame_abi160(4*BytesPerWord);                 // Reserve space to save the tmp_[1..4] registers.
  __ z_stmg(Z_R10, Z_R13, frame::z_abi_160_size, Z_SP); // Save registers only after frame is pushed.

  __ z_lgr(arg_java, Z_ARG1);

  Register   method = Z_ARG2; // Directly load into correct argument register.

  __ get_method(method);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::get_signature), Z_thread, method);

  // Move signature to callee saved register.
  // Don't directly write to stack. Frame is used by VM call.
  __ z_lgr(Z_tmp_1, Z_RET);

  // Reload method. Register may have been altered by VM call.
  __ get_method(method);

  // Get address of result handler.
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::get_result_handler), Z_thread, method);

  // Save signature address to stack.
  __ z_stg(Z_tmp_1, d_signature, Z_SP);

  // Don't overwrite return value (Z_RET, Z_ARG1) in rest of the method !

  {
    Label   isStatic;

    // Test if static.
    // We can test the bit directly.
    // Path is Z_method->_access_flags._flags.
    // We only support flag bits in the least significant byte (assert !).
    // Therefore add 3 to address that byte within "_flags".
    // Reload method. VM call above may have destroyed register contents
    __ get_method(method);
    __ testbit(method2_(method, access_flags), JVM_ACC_STATIC_BIT);
    method = noreg;  // end of life
    __ z_btrue(isStatic);

    // For non-static functions, pass "this" in Z_ARG2 and copy it to 2nd C-arg slot.
    // Need to box the Java object here, so we use arg_java
    // (address of current Java stack slot) as argument and
    // don't dereference it as in case of ints, floats, etc..
    __ z_lgr(Z_ARG2, arg_java);
    __ add2reg(arg_java, -BytesPerWord);
    __ bind(isStatic);
  }

  // argcnt == 0 corresponds to 3rd C argument.
  //   arg #1 (result handler) and
  //   arg #2 (this, for non-statics), unused else
  // are reserved and pre-filled above.
  // arg_java points to the corresponding Java argument here. It
  // has been decremented by one argument (this) in case of non-static.
  __ clear_reg(argcnt, true, false);  // Don't set CC.
  __ z_lg(target_sp, 0, Z_SP);
  __ add2reg(arg_c, _z_abi(remaining_cargs), target_sp);
  // No floating-point args parsed so far.
  __ clear_mem(Address(Z_SP, d_fpcnt), 8);

  NearLabel   move_intSlot_to_ARG, move_floatSlot_to_FARG;
  NearLabel   loop_start, loop_start_restore, loop_end;
  NearLabel   do_int, do_long, do_float, do_double;
  NearLabel   do_dontreachhere, do_object, do_array, do_boxed;

#ifdef ASSERT
  // Signature needs to point to '(' (== 0x28) at entry.
  __ z_lg(signature, d_signature, Z_SP);
  __ z_cli(0, signature, (int) '(');
  __ z_brne(do_dontreachhere);
#endif

  __ bind(loop_start_restore);
  __ z_lg(signature, d_signature, Z_SP);  // Restore signature ptr, destroyed by move_XX_to_ARG.

  BIND(loop_start);
  // Advance to next argument type token from the signature.
  __ add2reg(signature, 1);

  // Use CLI, works well on all CPU versions.
    __ z_cli(0, signature, (int) ')');
    __ z_bre(loop_end);                // end of signature
    __ z_cli(0, signature, (int) 'L');
    __ z_bre(do_object);               // object     #9
    __ z_cli(0, signature, (int) 'F');
    __ z_bre(do_float);                // float      #7
    __ z_cli(0, signature, (int) 'J');
    __ z_bre(do_long);                 // long       #6
    __ z_cli(0, signature, (int) 'B');
    __ z_bre(do_int);                  // byte       #1
    __ z_cli(0, signature, (int) 'Z');
    __ z_bre(do_int);                  // boolean    #2
    __ z_cli(0, signature, (int) 'C');
    __ z_bre(do_int);                  // char       #3
    __ z_cli(0, signature, (int) 'S');
    __ z_bre(do_int);                  // short      #4
    __ z_cli(0, signature, (int) 'I');
    __ z_bre(do_int);                  // int        #5
    __ z_cli(0, signature, (int) 'D');
    __ z_bre(do_double);               // double     #8
    __ z_cli(0, signature, (int) '[');
    __ z_bre(do_array);                // array      #10

  __ bind(do_dontreachhere);

  __ unimplemented("ShouldNotReachHere in slow_signature_handler", 120);

  // Array argument
  BIND(do_array);

  {
    Label   start_skip, end_skip;

    __ bind(start_skip);

    // Advance to next type tag from signature.
    __ add2reg(signature, 1);

    // Use CLI, works well on all CPU versions.
    __ z_cli(0, signature, (int) '[');
    __ z_bre(start_skip);               // Skip further brackets.

    __ z_cli(0, signature, (int) '9');
    __ z_brh(end_skip);                 // no optional size

    __ z_cli(0, signature, (int) '0');
    __ z_brnl(start_skip);              // Skip optional size.

    __ bind(end_skip);

    __ z_cli(0, signature, (int) 'L');
    __ z_brne(do_boxed);                // If not array of objects: go directly to do_boxed.
  }

  //  OOP argument
  BIND(do_object);
  // Pass by an object's type name.
  {
    Label   L;

    __ add2reg(sig_end, 4095, signature);     // Assume object type name is shorter than 4k.
    __ load_const_optimized(Z_R0, (int) ';'); // Type name terminator (must be in Z_R0!).
    __ MacroAssembler::search_string(sig_end, signature);
    __ z_brl(L);
    __ z_illtrap();  // No semicolon found: internal error or object name too long.
    __ bind(L);
    __ z_lgr(signature, sig_end);
    // fallthru to do_boxed
  }

  // Need to box the Java object here, so we use arg_java
  // (address of current Java stack slot) as argument and
  // don't dereference it as in case of ints, floats, etc..

  // UNBOX argument
  // Load reference and check for NULL.
  Label  do_int_Entry4Boxed;
  __ bind(do_boxed);
  {
    __ load_and_test_long(intSlot, Address(arg_java));
    __ z_bre(do_int_Entry4Boxed);
    __ z_lgr(intSlot, arg_java);
    __ z_bru(do_int_Entry4Boxed);
  }

  // INT argument

  // (also for byte, boolean, char, short)
  // Use lgf for load (sign-extend) and stg for store.
  BIND(do_int);
  __ z_lgf(intSlot, 0, arg_java);

  __ bind(do_int_Entry4Boxed);
  __ add2reg(arg_java, -BytesPerWord);
  // If argument fits into argument register, go and handle it, otherwise continue.
  __ compare32_and_branch(argcnt, max_int_register_arguments,
                          Assembler::bcondLow, move_intSlot_to_ARG);
  __ z_stg(intSlot, 0, arg_c);
  __ add2reg(arg_c, BytesPerWord);
  __ z_bru(loop_start);

  // LONG argument

  BIND(do_long);
  __ add2reg(arg_java, -2*BytesPerWord);  // Decrement first to have positive displacement for lg.
  __ z_lg(intSlot, BytesPerWord, arg_java);
  // If argument fits into argument register, go and handle it, otherwise continue.
  __ compare32_and_branch(argcnt, max_int_register_arguments,
                          Assembler::bcondLow, move_intSlot_to_ARG);
  __ z_stg(intSlot, 0, arg_c);
  __ add2reg(arg_c, BytesPerWord);
  __ z_bru(loop_start);

  // FLOAT argumen

  BIND(do_float);
  __ z_le(floatSlot, 0, arg_java);
  __ add2reg(arg_java, -BytesPerWord);
  assert(max_fp_register_arguments <= 255, "always true");  // safety net
  __ z_cli(d_fpcnt+7, Z_SP, max_fp_register_arguments);
  __ z_brl(move_floatSlot_to_FARG);
  __ z_ste(floatSlot, 4, arg_c);
  __ add2reg(arg_c, BytesPerWord);
  __ z_bru(loop_start);

  // DOUBLE argument

  BIND(do_double);
  __ add2reg(arg_java, -2*BytesPerWord);  // Decrement first to have positive displacement for lg.
  __ z_ld(floatSlot, BytesPerWord, arg_java);
  assert(max_fp_register_arguments <= 255, "always true");  // safety net
  __ z_cli(d_fpcnt+7, Z_SP, max_fp_register_arguments);
  __ z_brl(move_floatSlot_to_FARG);
  __ z_std(floatSlot, 0, arg_c);
  __ add2reg(arg_c, BytesPerWord);
  __ z_bru(loop_start);

  // Method exit, all arguments proocessed.
  __ bind(loop_end);
  __ z_lmg(Z_R10, Z_R13, frame::z_abi_160_size, Z_SP); // restore registers before frame is popped.
  __ pop_frame();
  __ restore_return_pc();
  __ z_br(Z_R14);

  // Copy int arguments.

  Label  iarg_caselist;   // Distance between each case has to be a power of 2
                          // (= 1 << LogSizeOfCase).
  __ align(16);
  BIND(iarg_caselist);
  __ z_lgr(Z_ARG3, intSlot);    // 4 bytes
  __ z_bru(loop_start_restore); // 4 bytes

  __ z_lgr(Z_ARG4, intSlot);
  __ z_bru(loop_start_restore);

  __ z_lgr(Z_ARG5, intSlot);
  __ z_bru(loop_start_restore);

  __ align(16);
  __ bind(move_intSlot_to_ARG);
  __ z_stg(signature, d_signature, Z_SP);       // Spill since signature == Z_R1_scratch.
  __ z_larl(Z_R1_scratch, iarg_caselist);
  __ z_sllg(Z_R0_scratch, argcnt, LogSizeOfCase);
  __ add2reg(argcnt, 1);
  __ z_agr(Z_R1_scratch, Z_R0_scratch);
  __ z_bcr(Assembler::bcondAlways, Z_R1_scratch);

  // Copy float arguments.

  Label  farg_caselist;   // Distance between each case has to be a power of 2
                          // (= 1 << logSizeOfCase, padded with nop.
  __ align(16);
  BIND(farg_caselist);
  __ z_ldr(Z_FARG1, floatSlot); // 2 bytes
  __ z_bru(loop_start_restore); // 4 bytes
  __ z_nop();                   // 2 bytes

  __ z_ldr(Z_FARG2, floatSlot);
  __ z_bru(loop_start_restore);
  __ z_nop();

  __ z_ldr(Z_FARG3, floatSlot);
  __ z_bru(loop_start_restore);
  __ z_nop();

  __ z_ldr(Z_FARG4, floatSlot);
  __ z_bru(loop_start_restore);
  __ z_nop();

  __ align(16);
  __ bind(move_floatSlot_to_FARG);
  __ z_stg(signature, d_signature, Z_SP);        // Spill since signature == Z_R1_scratch.
  __ z_lg(Z_R0_scratch, d_fpcnt, Z_SP);          // Need old value for indexing.
  __ add2mem_64(Address(Z_SP, d_fpcnt), 1, Z_R1_scratch); // Increment index.
  __ z_larl(Z_R1_scratch, farg_caselist);
  __ z_sllg(Z_R0_scratch, Z_R0_scratch, LogSizeOfCase);
  __ z_agr(Z_R1_scratch, Z_R0_scratch);
  __ z_bcr(Assembler::bcondAlways, Z_R1_scratch);

  BLOCK_COMMENT("} slow_signature_handler");

  return __ addr_at(entry_offset);
}

address TemplateInterpreterGenerator::generate_result_handler_for (BasicType type) {
  address entry = __ pc();

  assert(Z_tos == Z_RET, "Result handler: must move result!");
  assert(Z_ftos == Z_FRET, "Result handler: must move float result!");

  switch (type) {
    case T_BOOLEAN:
      __ c2bool(Z_tos);
      break;
    case T_CHAR:
      __ and_imm(Z_tos, 0xffff);
      break;
    case T_BYTE:
      __ z_lbr(Z_tos, Z_tos);
      break;
    case T_SHORT:
      __ z_lhr(Z_tos, Z_tos);
      break;
    case T_INT:
    case T_LONG:
    case T_VOID:
    case T_FLOAT:
    case T_DOUBLE:
      break;
    case T_OBJECT:
      // Retrieve result from frame...
      __ mem2reg_opt(Z_tos, Address(Z_fp, oop_tmp_offset));
      // and verify it.
      __ verify_oop(Z_tos);
      break;
    default:
      ShouldNotReachHere();
  }
  __ z_br(Z_R14);      // Return from result handler.
  return entry;
}

// Abstract method entry.
// Attempt to execute abstract method. Throw exception.
address TemplateInterpreterGenerator::generate_abstract_entry(void) {
  unsigned int entry_offset = __ offset();

  // Caller could be the call_stub or a compiled method (x86 version is wrong!).

  BLOCK_COMMENT("abstract_entry {");

  // Implement call of InterpreterRuntime::throw_AbstractMethodError.
  __ set_top_ijava_frame_at_SP_as_last_Java_frame(Z_SP, Z_R1);
  __ save_return_pc();       // Save Z_R14.
  __ push_frame_abi160(0);   // Without new frame the RT call could overwrite the saved Z_R14.

  __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_AbstractMethodErrorWithMethod),
                  Z_thread, Z_method);

  __ pop_frame();
  __ restore_return_pc();    // Restore Z_R14.
  __ reset_last_Java_frame();

  // Restore caller sp for c2i case.
  __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.

  // branch to SharedRuntime::generate_forward_exception() which handles all possible callers,
  // i.e. call stub, compiled method, interpreted method.
  __ load_absolute_address(Z_tmp_1, StubRoutines::forward_exception_entry());
  __ z_br(Z_tmp_1);

  BLOCK_COMMENT("} abstract_entry");

  return __ addr_at(entry_offset);
}

address TemplateInterpreterGenerator::generate_Reference_get_entry(void) {
  // Inputs:
  //  Z_ARG1 - receiver
  //
  // What we do:
  //  - Load the referent field address.
  //  - Load the value in the referent field.
  //  - Pass that value to the pre-barrier.
  //
  // In the case of G1 this will record the value of the
  // referent in an SATB buffer if marking is active.
  // This will cause concurrent marking to mark the referent
  // field as live.

  Register  scratch1 = Z_tmp_2;
  Register  scratch2 = Z_tmp_3;
  Register  pre_val  = Z_RET;   // return value
  // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
  Register  Rargp    = Z_esp;

  Label     slow_path;
  address   entry = __ pc();

  const int referent_offset = java_lang_ref_Reference::referent_offset();

  BLOCK_COMMENT("Reference_get {");

  //  If the receiver is null then it is OK to jump to the slow path.
  __ load_and_test_long(pre_val, Address(Rargp, Interpreter::stackElementSize)); // Get receiver.
  __ z_bre(slow_path);

  //  Load the value of the referent field.
  __ load_heap_oop(pre_val, Address(pre_val, referent_offset), scratch1, scratch2, ON_WEAK_OOP_REF);

  // Restore caller sp for c2i case.
  __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.
  __ z_br(Z_R14);

  // Branch to previously generated regular method entry.
  __ bind(slow_path);

  address meth_entry = Interpreter::entry_for_kind(Interpreter::zerolocals);
  __ jump_to_entry(meth_entry, Z_R1);

  BLOCK_COMMENT("} Reference_get");

  return entry;
}

address TemplateInterpreterGenerator::generate_StackOverflowError_handler() {
  address entry = __ pc();

  DEBUG_ONLY(__ verify_esp(Z_esp, Z_ARG5));

  // Restore bcp under the assumption that the current frame is still
  // interpreted.
  __ restore_bcp();

  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();
  // Throw exception.
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_StackOverflowError));
  return entry;
}

//
// Args:
//   Z_ARG2: oop of array
//   Z_ARG3: aberrant index
//
address TemplateInterpreterGenerator::generate_ArrayIndexOutOfBounds_handler() {
  address entry = __ pc();
  address excp = CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_ArrayIndexOutOfBoundsException);

  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();

  // Setup parameters.
  // Pass register with array to create more detailed exceptions.
  __ call_VM(noreg, excp, Z_ARG2, Z_ARG3);
  return entry;
}

address TemplateInterpreterGenerator::generate_ClassCastException_handler() {
  address entry = __ pc();

  // Object is at TOS.
  __ pop_ptr(Z_ARG2);

  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();

  __ call_VM(Z_ARG1,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_ClassCastException),
             Z_ARG2);

  DEBUG_ONLY(__ should_not_reach_here();)

  return entry;
}

address TemplateInterpreterGenerator::generate_exception_handler_common(const char* name, const char* message, bool pass_oop) {
  assert(!pass_oop || message == NULL, "either oop or message but not both");
  address entry = __ pc();

  BLOCK_COMMENT("exception_handler_common {");

  // Expression stack must be empty before entering the VM if an
  // exception happened.
  __ empty_expression_stack();
  if (name != NULL) {
    __ load_absolute_address(Z_ARG2, (address)name);
  } else {
    __ clear_reg(Z_ARG2, true, false);
  }

  if (pass_oop) {
    __ call_VM(Z_tos,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::create_klass_exception),
               Z_ARG2, Z_tos /*object (see TT::aastore())*/);
  } else {
    if (message != NULL) {
      __ load_absolute_address(Z_ARG3, (address)message);
    } else {
      __ clear_reg(Z_ARG3, true, false);
    }
    __ call_VM(Z_tos,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::create_exception),
               Z_ARG2, Z_ARG3);
  }
  // Throw exception.
  __ load_absolute_address(Z_R1_scratch, Interpreter::throw_exception_entry());
  __ z_br(Z_R1_scratch);

  BLOCK_COMMENT("} exception_handler_common");

  return entry;
}

address TemplateInterpreterGenerator::generate_return_entry_for (TosState state, int step, size_t index_size) {
  address entry = __ pc();

  BLOCK_COMMENT("return_entry {");

  // Pop i2c extension or revert top-2-parent-resize done by interpreted callees.
  Register sp_before_i2c_extension = Z_bcp;
  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // Restore frame pointer.
  __ z_lg(sp_before_i2c_extension, Address(Z_fp, _z_ijava_state_neg(top_frame_sp)));
  __ resize_frame_absolute(sp_before_i2c_extension, Z_locals/*tmp*/, true/*load_fp*/);

  // TODO(ZASM): necessary??
  //  // and NULL it as marker that esp is now tos until next java call
  //  __ movptr(Address(rbp, frame::interpreter_frame_last_sp_offset * wordSize), (int32_t)NULL_WORD);

  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();

  if (state == atos) {
    __ profile_return_type(Z_tmp_1, Z_tos, Z_tmp_2);
  }

  Register cache  = Z_tmp_1;
  Register size   = Z_tmp_1;
  Register offset = Z_tmp_2;
  const int flags_offset = in_bytes(ConstantPoolCache::base_offset() +
                                    ConstantPoolCacheEntry::flags_offset());
  __ get_cache_and_index_at_bcp(cache, offset, 1, index_size);

  // #args is in rightmost byte of the _flags field.
  __ z_llgc(size, Address(cache, offset, flags_offset+(sizeof(size_t)-1)));
  __ z_sllg(size, size, Interpreter::logStackElementSize); // Each argument size in bytes.
  __ z_agr(Z_esp, size);                                   // Pop arguments.

  __ check_and_handle_popframe(Z_thread);
  __ check_and_handle_earlyret(Z_thread);

  __ dispatch_next(state, step);

  BLOCK_COMMENT("} return_entry");

  return entry;
}

address TemplateInterpreterGenerator::generate_deopt_entry_for(TosState state,
                                                               int step,
                                                               address continuation) {
  address entry = __ pc();

  BLOCK_COMMENT("deopt_entry {");

  // TODO(ZASM): necessary? NULL last_sp until next java call
  // __ movptr(Address(rbp, frame::interpreter_frame_last_sp_offset * wordSize), (int32_t)NULL_WORD);
  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // Restore frame pointer.
  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();

  // Handle exceptions.
  {
    Label L;
    __ load_and_test_long(Z_R0/*pending_exception*/, thread_(pending_exception));
    __ z_bre(L);
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::throw_pending_exception));
    __ should_not_reach_here();
    __ bind(L);
  }
  if (continuation == NULL) {
    __ dispatch_next(state, step);
  } else {
    __ jump_to_entry(continuation, Z_R1_scratch);
  }

  BLOCK_COMMENT("} deopt_entry");

  return entry;
}

address TemplateInterpreterGenerator::generate_safept_entry_for (TosState state,
                                                                address runtime_entry) {
  address entry = __ pc();
  __ push(state);
  __ call_VM(noreg, runtime_entry);
  __ dispatch_via(vtos, Interpreter::_normal_table.table_for (vtos));
  return entry;
}

//
// Helpers for commoning out cases in the various type of method entries.
//

// Increment invocation count & check for overflow.
//
// Note: checking for negative value instead of overflow
// so we have a 'sticky' overflow test.
//
// Z_ARG2: method (see generate_fixed_frame())
//
void TemplateInterpreterGenerator::generate_counter_incr(Label* overflow) {
  Label done;
  Register method = Z_ARG2; // Generate_fixed_frame() copies Z_method into Z_ARG2.
  Register m_counters = Z_ARG4;

  BLOCK_COMMENT("counter_incr {");

  // Note: In tiered we increment either counters in method or in MDO depending
  // if we are profiling or not.
  int increment = InvocationCounter::count_increment;
  if (ProfileInterpreter) {
    NearLabel no_mdo;
    Register mdo = m_counters;
    // Are we profiling?
    __ load_and_test_long(mdo, method2_(method, method_data));
    __ branch_optimized(Assembler::bcondZero, no_mdo);
    // Increment counter in the MDO.
    const Address mdo_invocation_counter(mdo, MethodData::invocation_counter_offset() +
                                         InvocationCounter::counter_offset());
    const Address mask(mdo, MethodData::invoke_mask_offset());
    __ increment_mask_and_jump(mdo_invocation_counter, increment, mask,
                               Z_R1_scratch, false, Assembler::bcondZero,
                               overflow);
    __ z_bru(done);
    __ bind(no_mdo);
  }

  // Increment counter in MethodCounters.
  const Address invocation_counter(m_counters,
                                   MethodCounters::invocation_counter_offset() +
                                   InvocationCounter::counter_offset());
  // Get address of MethodCounters object.
  __ get_method_counters(method, m_counters, done);
  const Address mask(m_counters, MethodCounters::invoke_mask_offset());
  __ increment_mask_and_jump(invocation_counter,
                             increment, mask,
                             Z_R1_scratch, false, Assembler::bcondZero,
                             overflow);

  __ bind(done);

  BLOCK_COMMENT("} counter_incr");
}

void TemplateInterpreterGenerator::generate_counter_overflow(Label& do_continue) {
  // InterpreterRuntime::frequency_counter_overflow takes two
  // arguments, the first (thread) is passed by call_VM, the second
  // indicates if the counter overflow occurs at a backwards branch
  // (NULL bcp). We pass zero for it. The call returns the address
  // of the verified entry point for the method or NULL if the
  // compilation did not complete (either went background or bailed
  // out).
  __ clear_reg(Z_ARG2);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::frequency_counter_overflow),
             Z_ARG2);
  __ z_bru(do_continue);
}

void TemplateInterpreterGenerator::generate_stack_overflow_check(Register frame_size, Register tmp1) {
  Register tmp2 = Z_R1_scratch;
  const int page_size = os::vm_page_size();
  NearLabel after_frame_check;

  BLOCK_COMMENT("stack_overflow_check {");

  assert_different_registers(frame_size, tmp1);

  // Stack banging is sufficient overflow check if frame_size < page_size.
  if (Immediate::is_uimm(page_size, 15)) {
    __ z_chi(frame_size, page_size);
    __ z_brl(after_frame_check);
  } else {
    __ load_const_optimized(tmp1, page_size);
    __ compareU32_and_branch(frame_size, tmp1, Assembler::bcondLow, after_frame_check);
  }

  // Get the stack base, and in debug, verify it is non-zero.
  __ z_lg(tmp1, thread_(stack_base));
#ifdef ASSERT
  address reentry = NULL;
  NearLabel base_not_zero;
  __ compareU64_and_branch(tmp1, (intptr_t)0L, Assembler::bcondNotEqual, base_not_zero);
  reentry = __ stop_chain_static(reentry, "stack base is zero in generate_stack_overflow_check");
  __ bind(base_not_zero);
#endif

  // Get the stack size, and in debug, verify it is non-zero.
  assert(sizeof(size_t) == sizeof(intptr_t), "wrong load size");
  __ z_lg(tmp2, thread_(stack_size));
#ifdef ASSERT
  NearLabel size_not_zero;
  __ compareU64_and_branch(tmp2, (intptr_t)0L, Assembler::bcondNotEqual, size_not_zero);
  reentry = __ stop_chain_static(reentry, "stack size is zero in generate_stack_overflow_check");
  __ bind(size_not_zero);
#endif

  // Compute the beginning of the protected zone minus the requested frame size.
  __ z_sgr(tmp1, tmp2);
  __ add2reg(tmp1, StackOverflow::stack_guard_zone_size());

  // Add in the size of the frame (which is the same as subtracting it from the
  // SP, which would take another register.
  __ z_agr(tmp1, frame_size);

  // The frame is greater than one page in size, so check against
  // the bottom of the stack.
  __ compareU64_and_branch(Z_SP, tmp1, Assembler::bcondHigh, after_frame_check);

  // The stack will overflow, throw an exception.

  // Restore SP to sender's sp. This is necessary if the sender's frame is an
  // extended compiled frame (see gen_c2i_adapter()) and safer anyway in case of
  // JSR292 adaptations.
  __ resize_frame_absolute(Z_R10, tmp1, true/*load_fp*/);

  // Note also that the restored frame is not necessarily interpreted.
  // Use the shared runtime version of the StackOverflowError.
  assert(StubRoutines::throw_StackOverflowError_entry() != NULL, "stub not yet generated");
  AddressLiteral stub(StubRoutines::throw_StackOverflowError_entry());
  __ load_absolute_address(tmp1, StubRoutines::throw_StackOverflowError_entry());
  __ z_br(tmp1);

  // If you get to here, then there is enough stack space.
  __ bind(after_frame_check);

  BLOCK_COMMENT("} stack_overflow_check");
}

// Allocate monitor and lock method (asm interpreter).
//
// Args:
//   Z_locals: locals

void TemplateInterpreterGenerator::lock_method(void) {

  BLOCK_COMMENT("lock_method {");

  // Synchronize method.
  const Register method = Z_tmp_2;
  __ get_method(method);

#ifdef ASSERT
  address reentry = NULL;
  {
    Label L;
    __ testbit(method2_(method, access_flags), JVM_ACC_SYNCHRONIZED_BIT);
    __ z_btrue(L);
    reentry = __ stop_chain_static(reentry, "method doesn't need synchronization");
    __ bind(L);
  }
#endif // ASSERT

  // Get synchronization object.
  const Register object = Z_tmp_2;

  {
    Label     done;
    Label     static_method;

    __ testbit(method2_(method, access_flags), JVM_ACC_STATIC_BIT);
    __ z_btrue(static_method);

    // non-static method: Load receiver obj from stack.
    __ mem2reg_opt(object, Address(Z_locals, Interpreter::local_offset_in_bytes(0)));
    __ z_bru(done);

    __ bind(static_method);

    // Lock the java mirror.
    // Load mirror from interpreter frame.
    __ z_lg(object, _z_ijava_state_neg(mirror), Z_fp);

#ifdef ASSERT
    {
      NearLabel L;
      __ compare64_and_branch(object, (intptr_t) 0, Assembler::bcondNotEqual, L);
      reentry = __ stop_chain_static(reentry, "synchronization object is NULL");
      __ bind(L);
    }
#endif // ASSERT

    __ bind(done);
  }

  __ add_monitor_to_stack(true, Z_ARG3, Z_ARG4, Z_ARG5); // Allocate monitor elem.
  // Store object and lock it.
  __ get_monitors(Z_tmp_1);
  __ reg2mem_opt(object, Address(Z_tmp_1, BasicObjectLock::obj_offset_in_bytes()));
  __ lock_object(Z_tmp_1, object);

  BLOCK_COMMENT("} lock_method");
}

// Generate a fixed interpreter frame. This is identical setup for
// interpreted methods and for native methods hence the shared code.
//
// Registers alive
//   Z_thread   - JavaThread*
//   Z_SP       - old stack pointer
//   Z_method   - callee's method
//   Z_esp      - parameter list (slot 'above' last param)
//   Z_R14      - return pc, to be stored in caller's frame
//   Z_R10      - sender sp, note: Z_tmp_1 is Z_R10!
//
// Registers updated
//   Z_SP       - new stack pointer
//   Z_esp      - callee's operand stack pointer
//                points to the slot above the value on top
//   Z_locals   - used to access locals: locals[i] := *(Z_locals - i*BytesPerWord)
//   Z_bcp      - the bytecode pointer
//   Z_fp       - the frame pointer, thereby killing Z_method
//   Z_ARG2     - copy of Z_method
//
void TemplateInterpreterGenerator::generate_fixed_frame(bool native_call) {

  //  stack layout
  //
  //   F1 [TOP_IJAVA_FRAME_ABI]              <-- Z_SP, Z_R10 (see note below)
  //      [F1's operand stack (unused)]
  //      [F1's outgoing Java arguments]     <-- Z_esp
  //      [F1's operand stack (non args)]
  //      [monitors]      (optional)
  //      [IJAVA_STATE]
  //
  //   F2 [PARENT_IJAVA_FRAME_ABI]
  //      ...
  //
  //  0x000
  //
  // Note: Z_R10, the sender sp, will be below Z_SP if F1 was extended by a c2i adapter.

  //=============================================================================
  // Allocate space for locals other than the parameters, the
  // interpreter state, monitors, and the expression stack.

  const Register local_count  = Z_ARG5;
  const Register fp           = Z_tmp_2;
  const Register const_method = Z_ARG1;

  BLOCK_COMMENT("generate_fixed_frame {");
  {
  // local registers
  const Register top_frame_size  = Z_ARG2;
  const Register sp_after_resize = Z_ARG3;
  const Register max_stack       = Z_ARG4;

  __ z_lg(const_method, Address(Z_method, Method::const_offset()));
  __ z_llgh(max_stack, Address(const_method, ConstMethod::size_of_parameters_offset()));
  __ z_sllg(Z_locals /*parameter_count bytes*/, max_stack /*parameter_count*/, LogBytesPerWord);

  if (native_call) {
    // If we're calling a native method, we replace max_stack (which is
    // zero) with space for the worst-case signature handler varargs
    // vector, which is:
    //   max_stack = max(Argument::n_register_parameters, parameter_count+2);
    //
    // We add two slots to the parameter_count, one for the jni
    // environment and one for a possible native mirror. We allocate
    // space for at least the number of ABI registers, even though
    // InterpreterRuntime::slow_signature_handler won't write more than
    // parameter_count+2 words when it creates the varargs vector at the
    // top of the stack. The generated slow signature handler will just
    // load trash into registers beyond the necessary number. We're
    // still going to cut the stack back by the ABI register parameter
    // count so as to get SP+16 pointing at the ABI outgoing parameter
    // area, so we need to allocate at least that much even though we're
    // going to throw it away.
    //
    __ add2reg(max_stack, 2);

    NearLabel passing_args_on_stack;

    // max_stack in bytes
    __ z_sllg(max_stack, max_stack, LogBytesPerWord);

    int argument_registers_in_bytes = Argument::n_register_parameters << LogBytesPerWord;
    __ compare64_and_branch(max_stack, argument_registers_in_bytes, Assembler::bcondNotLow, passing_args_on_stack);

    __ load_const_optimized(max_stack, argument_registers_in_bytes);

    __ bind(passing_args_on_stack);
  } else {
    // !native_call
    // local_count = method->constMethod->max_locals();
    __ z_llgh(local_count, Address(const_method, ConstMethod::size_of_locals_offset()));

    // Calculate number of non-parameter locals (in slots):
    __ z_sgr(local_count, max_stack);

    // max_stack = method->max_stack();
    __ z_llgh(max_stack, Address(const_method, ConstMethod::max_stack_offset()));
    // max_stack in bytes
    __ z_sllg(max_stack, max_stack, LogBytesPerWord);
  }

  // Resize (i.e. normally shrink) the top frame F1 ...
  //   F1      [TOP_IJAVA_FRAME_ABI]          <-- Z_SP, Z_R10
  //           F1's operand stack (free)
  //           ...
  //           F1's operand stack (free)      <-- Z_esp
  //           F1's outgoing Java arg m
  //           ...
  //           F1's outgoing Java arg 0
  //           ...
  //
  //  ... into a parent frame (Z_R10 holds F1's SP before any modification, see also above)
  //
  //           +......................+
  //           :                      :        <-- Z_R10, saved below as F0's z_ijava_state.sender_sp
  //           :                      :
  //   F1      [PARENT_IJAVA_FRAME_ABI]        <-- Z_SP       \
  //           F0's non arg local                             | = delta
  //           ...                                            |
  //           F0's non arg local              <-- Z_esp      /
  //           F1's outgoing Java arg m
  //           ...
  //           F1's outgoing Java arg 0
  //           ...
  //
  // then push the new top frame F0.
  //
  //   F0      [TOP_IJAVA_FRAME_ABI]    = frame::z_top_ijava_frame_abi_size \
  //           [operand stack]          = max_stack                          | = top_frame_size
  //           [IJAVA_STATE]            = frame::z_ijava_state_size         /

  // sp_after_resize = Z_esp - delta
  //
  // delta = PARENT_IJAVA_FRAME_ABI + (locals_count - params_count)

  __ add2reg(sp_after_resize, (Interpreter::stackElementSize) - (frame::z_parent_ijava_frame_abi_size), Z_esp);
  if (!native_call) {
    __ z_sllg(Z_R0_scratch, local_count, LogBytesPerWord); // Params have already been subtracted from local_count.
    __ z_slgr(sp_after_resize, Z_R0_scratch);
  }

  // top_frame_size = TOP_IJAVA_FRAME_ABI + max_stack + size of interpreter state
  __ add2reg(top_frame_size,
             frame::z_top_ijava_frame_abi_size +
             frame::z_ijava_state_size,
             max_stack);

  if (!native_call) {
    // Stack overflow check.
    // Native calls don't need the stack size check since they have no
    // expression stack and the arguments are already on the stack and
    // we only add a handful of words to the stack.
    Register frame_size = max_stack; // Reuse the register for max_stack.
    __ z_lgr(frame_size, Z_SP);
    __ z_sgr(frame_size, sp_after_resize);
    __ z_agr(frame_size, top_frame_size);
    generate_stack_overflow_check(frame_size, fp/*tmp1*/);
  }

  DEBUG_ONLY(__ z_cg(Z_R14, _z_abi16(return_pc), Z_SP));
  __ asm_assert_eq("killed Z_R14", 0);
  __ resize_frame_absolute(sp_after_resize, fp, true);
  __ save_return_pc(Z_R14);

  // ... and push the new frame F0.
  __ push_frame(top_frame_size, fp, true /*copy_sp*/, false);
  }

  //=============================================================================
  // Initialize the new frame F0: initialize interpreter state.

  {
  // locals
  const Register local_addr = Z_ARG4;

  BLOCK_COMMENT("generate_fixed_frame: initialize interpreter state {");

#ifdef ASSERT
  // Set the magic number (using local_addr as tmp register).
  __ load_const_optimized(local_addr, frame::z_istate_magic_number);
  __ z_stg(local_addr, _z_ijava_state_neg(magic), fp);
#endif

  // Save sender SP from F1 (i.e. before it was potentially modified by an
  // adapter) into F0's interpreter state. We use it as well to revert
  // resizing the frame above.
  __ z_stg(Z_R10, _z_ijava_state_neg(sender_sp), fp);

  // Load cp cache and save it at the end of this block.
  __ z_lg(Z_R1_scratch, Address(const_method, ConstMethod::constants_offset()));
  __ z_lg(Z_R1_scratch, Address(Z_R1_scratch, ConstantPool::cache_offset_in_bytes()));

  // z_ijava_state->method = method;
  __ z_stg(Z_method, _z_ijava_state_neg(method), fp);

  // Point locals at the first argument. Method's locals are the
  // parameters on top of caller's expression stack.
  // Tos points past last Java argument.

  __ z_agr(Z_locals, Z_esp);
  // z_ijava_state->locals - i*BytesPerWord points to i-th Java local (i starts at 0)
  // z_ijava_state->locals = Z_esp + parameter_count bytes
  __ z_stg(Z_locals, _z_ijava_state_neg(locals), fp);

  // z_ijava_state->oop_temp = NULL;
  __ store_const(Address(fp, oop_tmp_offset), 0);

  // Initialize z_ijava_state->mdx.
  Register Rmdp = Z_bcp;
  // native_call: assert that mdo == NULL
  const bool check_for_mdo = !native_call DEBUG_ONLY(|| native_call);
  if (ProfileInterpreter && check_for_mdo) {
    Label get_continue;

    __ load_and_test_long(Rmdp, method_(method_data));
    __ z_brz(get_continue);
    DEBUG_ONLY(if (native_call) __ stop("native methods don't have a mdo"));
    __ add2reg(Rmdp, in_bytes(MethodData::data_offset()));
    __ bind(get_continue);
  }
  __ z_stg(Rmdp, _z_ijava_state_neg(mdx), fp);

  // Initialize z_ijava_state->bcp and Z_bcp.
  if (native_call) {
    __ clear_reg(Z_bcp); // Must initialize. Will get written into frame where GC reads it.
  } else {
    __ add2reg(Z_bcp, in_bytes(ConstMethod::codes_offset()), const_method);
  }
  __ z_stg(Z_bcp, _z_ijava_state_neg(bcp), fp);

  // no monitors and empty operand stack
  // => z_ijava_state->monitors points to the top slot in IJAVA_STATE.
  // => Z_ijava_state->esp points one slot above into the operand stack.
  // z_ijava_state->monitors = fp - frame::z_ijava_state_size - Interpreter::stackElementSize;
  // z_ijava_state->esp = Z_esp = z_ijava_state->monitors;
  __ add2reg(Z_esp, -frame::z_ijava_state_size, fp);
  __ z_stg(Z_esp, _z_ijava_state_neg(monitors), fp);
  __ add2reg(Z_esp, -Interpreter::stackElementSize);
  __ z_stg(Z_esp, _z_ijava_state_neg(esp), fp);

  // z_ijava_state->cpoolCache = Z_R1_scratch (see load above);
  __ z_stg(Z_R1_scratch, _z_ijava_state_neg(cpoolCache), fp);

  // Get mirror and store it in the frame as GC root for this Method*.
  __ load_mirror_from_const_method(Z_R1_scratch, const_method);
  __ z_stg(Z_R1_scratch, _z_ijava_state_neg(mirror), fp);

  BLOCK_COMMENT("} generate_fixed_frame: initialize interpreter state");

  //=============================================================================
  if (!native_call) {
    // Local_count is already num_locals_slots - num_param_slots.
    // Start of locals: local_addr = Z_locals - locals size + 1 slot
    __ z_llgh(Z_R0_scratch, Address(const_method, ConstMethod::size_of_locals_offset()));
    __ add2reg(local_addr, BytesPerWord, Z_locals);
    __ z_sllg(Z_R0_scratch, Z_R0_scratch, LogBytesPerWord);
    __ z_sgr(local_addr, Z_R0_scratch);

    __ Clear_Array(local_count, local_addr, Z_ARG2);
  }

  }
  // Finally set the frame pointer, destroying Z_method.
  assert(Z_fp == Z_method, "maybe set Z_fp earlier if other register than Z_method");
  // Oprofile analysis suggests to keep a copy in a register to be used by
  // generate_counter_incr().
  __ z_lgr(Z_ARG2, Z_method);
  __ z_lgr(Z_fp, fp);

  BLOCK_COMMENT("} generate_fixed_frame");
}

// Various method entries

// Math function, frame manager must set up an interpreter state, etc.
address TemplateInterpreterGenerator::generate_math_entry(AbstractInterpreter::MethodKind kind) {

  // Decide what to do: Use same platform specific instructions and runtime calls as compilers.
  bool use_instruction = false;
  address runtime_entry = NULL;
  int num_args = 1;
  bool double_precision = true;

  // s390 specific:
  switch (kind) {
    case Interpreter::java_lang_math_sqrt:
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
    case Interpreter::java_lang_math_sqrt : /* runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dsqrt); not available */ break;
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

  if (use_instruction) {
    switch (kind) {
      case Interpreter::java_lang_math_sqrt:
        // Can use memory operand directly.
        __ z_sqdb(Z_FRET, Interpreter::stackElementSize, Z_esp);
        break;
      case Interpreter::java_lang_math_abs:
        // Load operand from stack.
        __ mem2freg_opt(Z_FRET, Address(Z_esp, Interpreter::stackElementSize));
        __ z_lpdbr(Z_FRET);
        break;
      case Interpreter::java_lang_math_fmaF:
        __ mem2freg_opt(Z_FRET,  Address(Z_esp,     Interpreter::stackElementSize)); // result reg = arg3
        __ mem2freg_opt(Z_FARG2, Address(Z_esp, 3 * Interpreter::stackElementSize)); // arg1
        __ z_maeb(Z_FRET, Z_FARG2, Address(Z_esp, 2 * Interpreter::stackElementSize));
        break;
      case Interpreter::java_lang_math_fmaD:
        __ mem2freg_opt(Z_FRET,  Address(Z_esp,     Interpreter::stackElementSize)); // result reg = arg3
        __ mem2freg_opt(Z_FARG2, Address(Z_esp, 5 * Interpreter::stackElementSize)); // arg1
        __ z_madb(Z_FRET, Z_FARG2, Address(Z_esp, 3 * Interpreter::stackElementSize));
        break;
      default: ShouldNotReachHere();
    }
  } else {
    // Load arguments
    assert(num_args <= 4, "passed in registers");
    if (double_precision) {
      int offset = (2 * num_args - 1) * Interpreter::stackElementSize;
      for (int i = 0; i < num_args; ++i) {
        __ mem2freg_opt(as_FloatRegister(Z_FARG1->encoding() + 2 * i), Address(Z_esp, offset));
        offset -= 2 * Interpreter::stackElementSize;
      }
    } else {
      int offset = num_args * Interpreter::stackElementSize;
      for (int i = 0; i < num_args; ++i) {
        __ mem2freg_opt(as_FloatRegister(Z_FARG1->encoding() + 2 * i), Address(Z_esp, offset));
        offset -= Interpreter::stackElementSize;
      }
    }
    // Call runtime
    __ save_return_pc();       // Save Z_R14.
    __ push_frame_abi160(0);   // Without new frame the RT call could overwrite the saved Z_R14.

    __ call_VM_leaf(runtime_entry);

    __ pop_frame();
    __ restore_return_pc();    // Restore Z_R14.
  }

  // Pop c2i arguments (if any) off when we return.
  __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.

  __ z_br(Z_R14);

  return entry;
}

// Interpreter stub for calling a native method. (asm interpreter).
// This sets up a somewhat different looking stack for calling the
// native method than the typical interpreter frame setup.
address TemplateInterpreterGenerator::generate_native_entry(bool synchronized) {
  // Determine code generation flags.
  bool inc_counter = UseCompiler || CountCompiledCalls || LogTouchedMethods;

  // Interpreter entry for ordinary Java methods.
  //
  // Registers alive
  //   Z_SP          - stack pointer
  //   Z_thread      - JavaThread*
  //   Z_method      - callee's method (method to be invoked)
  //   Z_esp         - operand (or expression) stack pointer of caller. one slot above last arg.
  //   Z_R10         - sender sp (before modifications, e.g. by c2i adapter
  //                   and as well by generate_fixed_frame below)
  //   Z_R14         - return address to caller (call_stub or c2i_adapter)
  //
  // Registers updated
  //   Z_SP          - stack pointer
  //   Z_fp          - callee's framepointer
  //   Z_esp         - callee's operand stack pointer
  //                   points to the slot above the value on top
  //   Z_locals      - used to access locals: locals[i] := *(Z_locals - i*BytesPerWord)
  //   Z_tos         - integer result, if any
  //   z_ftos        - floating point result, if any
  //
  // Stack layout at this point:
  //
  //   F1      [TOP_IJAVA_FRAME_ABI]         <-- Z_SP, Z_R10 (Z_R10 will be below Z_SP if
  //                                                          frame was extended by c2i adapter)
  //           [outgoing Java arguments]     <-- Z_esp
  //           ...
  //   PARENT  [PARENT_IJAVA_FRAME_ABI]
  //           ...
  //

  address entry_point = __ pc();

  // Make sure registers are different!
  assert_different_registers(Z_thread, Z_method, Z_esp);

  BLOCK_COMMENT("native_entry {");

  // Make sure method is native and not abstract.
#ifdef ASSERT
  address reentry = NULL;
  { Label L;
    __ testbit(method_(access_flags), JVM_ACC_NATIVE_BIT);
    __ z_btrue(L);
    reentry = __ stop_chain_static(reentry, "tried to execute non-native method as native");
    __ bind(L);
  }
  { Label L;
    __ testbit(method_(access_flags), JVM_ACC_ABSTRACT_BIT);
    __ z_bfalse(L);
    reentry = __ stop_chain_static(reentry, "tried to execute abstract method as non-abstract");
    __ bind(L);
  }
#endif // ASSERT

#ifdef ASSERT
  // Save the return PC into the callers frame for assertion in generate_fixed_frame.
  __ save_return_pc(Z_R14);
#endif

  // Generate the code to allocate the interpreter stack frame.
  generate_fixed_frame(true);

  const Address do_not_unlock_if_synchronized(Z_thread, JavaThread::do_not_unlock_if_synchronized_offset());
  // Since at this point in the method invocation the exception handler
  // would try to exit the monitor of synchronized methods which hasn't
  // been entered yet, we set the thread local variable
  // _do_not_unlock_if_synchronized to true. If any exception was thrown by
  // runtime, exception handling i.e. unlock_if_synchronized_method will
  // check this thread local flag.
  __ z_mvi(do_not_unlock_if_synchronized, true);

  // Increment invocation count and check for overflow.
  NearLabel invocation_counter_overflow;
  if (inc_counter) {
    generate_counter_incr(&invocation_counter_overflow);
  }

  Label continue_after_compile;
  __ bind(continue_after_compile);

  bang_stack_shadow_pages(true);

  // Reset the _do_not_unlock_if_synchronized flag.
  __ z_mvi(do_not_unlock_if_synchronized, false);

  // Check for synchronized methods.
  // This mst happen AFTER invocation_counter check and stack overflow check,
  // so method is not locked if overflows.
  if (synchronized) {
    lock_method();
  } else {
    // No synchronization necessary.
#ifdef ASSERT
    { Label L;
      __ get_method(Z_R1_scratch);
      __ testbit(method2_(Z_R1_scratch, access_flags), JVM_ACC_SYNCHRONIZED_BIT);
      __ z_bfalse(L);
      reentry = __ stop_chain_static(reentry, "method needs synchronization");
      __ bind(L);
    }
#endif // ASSERT
  }

  // start execution

  // jvmti support
  __ notify_method_entry();

  //=============================================================================
  // Get and call the signature handler.
  const Register Rmethod                 = Z_tmp_2;
  const Register signature_handler_entry = Z_tmp_1;
  const Register Rresult_handler         = Z_tmp_3;
  Label call_signature_handler;

  assert_different_registers(Z_fp, Rmethod, signature_handler_entry, Rresult_handler);
  assert(Rresult_handler->is_nonvolatile(), "Rresult_handler must be in a non-volatile register");

  // Reload method.
  __ get_method(Rmethod);

  // Check for signature handler.
  __ load_and_test_long(signature_handler_entry, method2_(Rmethod, signature_handler));
  __ z_brne(call_signature_handler);

  // Method has never been called. Either generate a specialized
  // handler or point to the slow one.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::prepare_native_call),
             Rmethod);

  // Reload method.
  __ get_method(Rmethod);

  // Reload signature handler, it must have been created/assigned in the meantime.
  __ z_lg(signature_handler_entry, method2_(Rmethod, signature_handler));

  __ bind(call_signature_handler);

  // We have a TOP_IJAVA_FRAME here, which belongs to us.
  __ set_top_ijava_frame_at_SP_as_last_Java_frame(Z_SP, Z_R1/*tmp*/);

  // Call signature handler and pass locals address in Z_ARG1.
  __ z_lgr(Z_ARG1, Z_locals);
  __ call_stub(signature_handler_entry);
  // Save result handler returned by signature handler.
  __ z_lgr(Rresult_handler, Z_RET);

  // Reload method (the slow signature handler may block for GC).
  __ get_method(Rmethod);

  // Pass mirror handle if static call.
  {
    Label method_is_not_static;
    __ testbit(method2_(Rmethod, access_flags), JVM_ACC_STATIC_BIT);
    __ z_bfalse(method_is_not_static);
    // Load mirror from interpreter frame.
    __ z_lg(Z_R1, _z_ijava_state_neg(mirror), Z_fp);
    // z_ijava_state.oop_temp = pool_holder->klass_part()->java_mirror();
    __ z_stg(Z_R1, oop_tmp_offset, Z_fp);
    // Pass handle to mirror as 2nd argument to JNI method.
    __ add2reg(Z_ARG2, oop_tmp_offset, Z_fp);
    __ bind(method_is_not_static);
  }

  // Pass JNIEnv address as first parameter.
  __ add2reg(Z_ARG1, in_bytes(JavaThread::jni_environment_offset()), Z_thread);

  // Note: last java frame has been set above already. The pc from there
  // is precise enough.

  // Get native function entry point before we change the thread state.
  __ z_lg(Z_R1/*native_method_entry*/, method2_(Rmethod, native_function));

  //=============================================================================
  // Transition from _thread_in_Java to _thread_in_native. As soon as
  // we make this change the safepoint code needs to be certain that
  // the last Java frame we established is good. The pc in that frame
  // just need to be near here not an actual return address.
#ifdef ASSERT
  {
    NearLabel L;
    __ mem2reg_opt(Z_R14, Address(Z_thread, JavaThread::thread_state_offset()), false /*32 bits*/);
    __ compareU32_and_branch(Z_R14, _thread_in_Java, Assembler::bcondEqual, L);
    reentry = __ stop_chain_static(reentry, "Wrong thread state in native stub");
    __ bind(L);
  }
#endif

  // Memory ordering: Z does not reorder store/load with subsequent load. That's strong enough.
  __ set_thread_state(_thread_in_native);

  //=============================================================================
  // Call the native method. Argument registers must not have been
  // overwritten since "__ call_stub(signature_handler);" (except for
  // ARG1 and ARG2 for static methods).

  __ call_c(Z_R1/*native_method_entry*/);

  // NOTE: frame::interpreter_frame_result() depends on these stores.
  __ z_stg(Z_RET, _z_ijava_state_neg(lresult), Z_fp);
  __ freg2mem_opt(Z_FRET, Address(Z_fp, _z_ijava_state_neg(fresult)));
  const Register Rlresult = signature_handler_entry;
  assert(Rlresult->is_nonvolatile(), "Rlresult must be in a non-volatile register");
  __ z_lgr(Rlresult, Z_RET);

  // Z_method may no longer be valid, because of GC.

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
  // synchronization is progress, and escapes.

  __ set_thread_state(_thread_in_native_trans);
  __ z_fence();

  // Now before we return to java we must look for a current safepoint
  // (a new safepoint can not start since we entered native_trans).
  // We must check here because a current safepoint could be modifying
  // the callers registers right this moment.

  // Check for safepoint operation in progress and/or pending suspend requests.
  {
    Label Continue, do_safepoint;
    __ safepoint_poll(do_safepoint, Z_R1);
    // Check for suspend.
    __ load_and_test_int(Z_R0/*suspend_flags*/, thread_(suspend_flags));
    __ z_bre(Continue); // 0 -> no flag set -> not suspended
    __ bind(do_safepoint);
    __ z_lgr(Z_ARG1, Z_thread);
    __ call_c(CAST_FROM_FN_PTR(address, JavaThread::check_special_condition_for_native_trans));
    __ bind(Continue);
  }

  //=============================================================================
  // Back in Interpreter Frame.

  // We are in thread_in_native_trans here and back in the normal
  // interpreter frame. We don't have to do anything special about
  // safepoints and we can switch to Java mode anytime we are ready.

  // Note: frame::interpreter_frame_result has a dependency on how the
  // method result is saved across the call to post_method_exit. For
  // native methods it assumes that the non-FPU/non-void result is
  // saved in z_ijava_state.lresult and a FPU result in z_ijava_state.fresult. If
  // this changes then the interpreter_frame_result implementation
  // will need to be updated too.

  //=============================================================================
  // Back in Java.

  // Memory ordering: Z does not reorder store/load with subsequent
  // load. That's strong enough.
  __ set_thread_state(_thread_in_Java);

  __ reset_last_Java_frame();

  // We reset the JNI handle block only after unboxing the result; see below.

  // The method register is junk from after the thread_in_native transition
  // until here. Also can't call_VM until the bcp has been
  // restored. Need bcp for throwing exception below so get it now.
  __ get_method(Rmethod);

  // Restore Z_bcp to have legal interpreter frame,
  // i.e., bci == 0 <=> Z_bcp == code_base().
  __ z_lg(Z_bcp, Address(Rmethod, Method::const_offset())); // get constMethod
  __ add2reg(Z_bcp, in_bytes(ConstMethod::codes_offset())); // get codebase

  if (CheckJNICalls) {
    // clear_pending_jni_exception_check
    __ clear_mem(Address(Z_thread, JavaThread::pending_jni_exception_check_fn_offset()), sizeof(oop));
  }

  // Check if the native method returns an oop, and if so, move it
  // from the jni handle to z_ijava_state.oop_temp. This is
  // necessary, because we reset the jni handle block below.
  // NOTE: frame::interpreter_frame_result() depends on this, too.
  { NearLabel no_oop_result;
  __ load_absolute_address(Z_R1, AbstractInterpreter::result_handler(T_OBJECT));
  __ compareU64_and_branch(Z_R1, Rresult_handler, Assembler::bcondNotEqual, no_oop_result);
  __ resolve_jobject(Rlresult, /* tmp1 */ Rmethod, /* tmp2 */ Z_R1);
  __ z_stg(Rlresult, oop_tmp_offset, Z_fp);
  __ bind(no_oop_result);
  }

  // Reset handle block.
  __ z_lg(Z_R1/*active_handles*/, thread_(active_handles));
  __ clear_mem(Address(Z_R1, JNIHandleBlock::top_offset_in_bytes()), 4);

  // Handle exceptions (exception handling will handle unlocking!).
  {
    Label L;
    __ load_and_test_long(Z_R0/*pending_exception*/, thread_(pending_exception));
    __ z_bre(L);
    __ MacroAssembler::call_VM(noreg,
                               CAST_FROM_FN_PTR(address,
                               InterpreterRuntime::throw_pending_exception));
    __ should_not_reach_here();
    __ bind(L);
  }

  if (synchronized) {
    Register Rfirst_monitor = Z_ARG2;
    __ add2reg(Rfirst_monitor, -(frame::z_ijava_state_size + (int)sizeof(BasicObjectLock)), Z_fp);
#ifdef ASSERT
    NearLabel ok;
    __ z_lg(Z_R1, _z_ijava_state_neg(monitors), Z_fp);
    __ compareU64_and_branch(Rfirst_monitor, Z_R1, Assembler::bcondEqual, ok);
    reentry = __ stop_chain_static(reentry, "native_entry:unlock: inconsistent z_ijava_state.monitors");
    __ bind(ok);
#endif
    __ unlock_object(Rfirst_monitor);
  }

  // JVMTI support. Result has already been saved above to the frame.
  __ notify_method_exit(true/*native_method*/, ilgl, InterpreterMacroAssembler::NotifyJVMTI);

  // Move native method result back into proper registers and return.
  __ mem2freg_opt(Z_FRET, Address(Z_fp, _z_ijava_state_neg(fresult)));
  __ mem2reg_opt(Z_RET, Address(Z_fp, _z_ijava_state_neg(lresult)));
  __ call_stub(Rresult_handler);

  // Pop the native method's interpreter frame.
  __ pop_interpreter_frame(Z_R14 /*return_pc*/, Z_ARG2/*tmp1*/, Z_ARG3/*tmp2*/);

  // Return to caller.
  __ z_br(Z_R14);

  if (inc_counter) {
    // Handle overflow of counter and compile method.
    __ bind(invocation_counter_overflow);
    generate_counter_overflow(continue_after_compile);
  }

  BLOCK_COMMENT("} native_entry");

  return entry_point;
}

//
// Generic interpreted method entry to template interpreter.
//
address TemplateInterpreterGenerator::generate_normal_entry(bool synchronized) {
  address entry_point = __ pc();

  bool inc_counter = UseCompiler || CountCompiledCalls || LogTouchedMethods;

  // Interpreter entry for ordinary Java methods.
  //
  // Registers alive
  //   Z_SP       - stack pointer
  //   Z_thread   - JavaThread*
  //   Z_method   - callee's method (method to be invoked)
  //   Z_esp      - operand (or expression) stack pointer of caller. one slot above last arg.
  //   Z_R10      - sender sp (before modifications, e.g. by c2i adapter
  //                           and as well by generate_fixed_frame below)
  //   Z_R14      - return address to caller (call_stub or c2i_adapter)
  //
  // Registers updated
  //   Z_SP       - stack pointer
  //   Z_fp       - callee's framepointer
  //   Z_esp      - callee's operand stack pointer
  //                points to the slot above the value on top
  //   Z_locals   - used to access locals: locals[i] := *(Z_locals - i*BytesPerWord)
  //   Z_tos      - integer result, if any
  //   z_ftos     - floating point result, if any
  //
  //
  // stack layout at this point:
  //
  //   F1      [TOP_IJAVA_FRAME_ABI]         <-- Z_SP, Z_R10 (Z_R10 will be below Z_SP if
  //                                                          frame was extended by c2i adapter)
  //           [outgoing Java arguments]     <-- Z_esp
  //           ...
  //   PARENT  [PARENT_IJAVA_FRAME_ABI]
  //           ...
  //
  // stack layout before dispatching the first bytecode:
  //
  //   F0      [TOP_IJAVA_FRAME_ABI]         <-- Z_SP
  //           [operand stack]               <-- Z_esp
  //           monitor (optional, can grow)
  //           [IJAVA_STATE]
  //   F1      [PARENT_IJAVA_FRAME_ABI]      <-- Z_fp (== *Z_SP)
  //           [F0's locals]                 <-- Z_locals
  //           [F1's operand stack]
  //           [F1's monitors] (optional)
  //           [IJAVA_STATE]

  // Make sure registers are different!
  assert_different_registers(Z_thread, Z_method, Z_esp);

  BLOCK_COMMENT("normal_entry {");

  // Make sure method is not native and not abstract.
  // Rethink these assertions - they can be simplified and shared.
#ifdef ASSERT
  address reentry = NULL;
  { Label L;
    __ testbit(method_(access_flags), JVM_ACC_NATIVE_BIT);
    __ z_bfalse(L);
    reentry = __ stop_chain_static(reentry, "tried to execute native method as non-native");
    __ bind(L);
  }
  { Label L;
    __ testbit(method_(access_flags), JVM_ACC_ABSTRACT_BIT);
    __ z_bfalse(L);
    reentry = __ stop_chain_static(reentry, "tried to execute abstract method as non-abstract");
    __ bind(L);
  }
#endif // ASSERT

#ifdef ASSERT
  // Save the return PC into the callers frame for assertion in generate_fixed_frame.
  __ save_return_pc(Z_R14);
#endif

  // Generate the code to allocate the interpreter stack frame.
  generate_fixed_frame(false);

  const Address do_not_unlock_if_synchronized(Z_thread, JavaThread::do_not_unlock_if_synchronized_offset());
  // Since at this point in the method invocation the exception handler
  // would try to exit the monitor of synchronized methods which hasn't
  // been entered yet, we set the thread local variable
  // _do_not_unlock_if_synchronized to true. If any exception was thrown by
  // runtime, exception handling i.e. unlock_if_synchronized_method will
  // check this thread local flag.
  __ z_mvi(do_not_unlock_if_synchronized, true);

  __ profile_parameters_type(Z_tmp_2, Z_ARG3, Z_ARG4);

  // Increment invocation counter and check for overflow.
  //
  // Note: checking for negative value instead of overflow so we have a 'sticky'
  // overflow test (may be of importance as soon as we have true MT/MP).
  NearLabel invocation_counter_overflow;
  NearLabel Lcontinue;
  if (inc_counter) {
    generate_counter_incr(&invocation_counter_overflow);
  }
  __ bind(Lcontinue);

  bang_stack_shadow_pages(false);

  // Reset the _do_not_unlock_if_synchronized flag.
  __ z_mvi(do_not_unlock_if_synchronized, false);

  // Check for synchronized methods.
  // Must happen AFTER invocation_counter check and stack overflow check,
  // so method is not locked if overflows.
  if (synchronized) {
    // Allocate monitor and lock method.
    lock_method();
  } else {
#ifdef ASSERT
    { Label L;
      __ get_method(Z_R1_scratch);
      __ testbit(method2_(Z_R1_scratch, access_flags), JVM_ACC_SYNCHRONIZED_BIT);
      __ z_bfalse(L);
      reentry = __ stop_chain_static(reentry, "method needs synchronization");
      __ bind(L);
    }
#endif // ASSERT
  }

  // start execution

#ifdef ASSERT
  __ verify_esp(Z_esp, Z_R1_scratch);

  __ verify_thread();
#endif

  // jvmti support
  __ notify_method_entry();

  // Start executing instructions.
  __ dispatch_next(vtos);
  // Dispatch_next does not return.
  DEBUG_ONLY(__ should_not_reach_here());

  // Invocation counter overflow.
  if (inc_counter) {
    // Handle invocation counter overflow.
    __ bind(invocation_counter_overflow);
    generate_counter_overflow(Lcontinue);
  }

  BLOCK_COMMENT("} normal_entry");

  return entry_point;
}


/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.update(int crc, int b)
 */
address TemplateInterpreterGenerator::generate_CRC32_update_entry() {

  if (UseCRC32Intrinsics) {
    uint64_t entry_off = __ offset();
    Label    slow_path;

    // If we need a safepoint check, generate full interpreter entry.
    __ safepoint_poll(slow_path, Z_R1);

    BLOCK_COMMENT("CRC32_update {");

    // We don't generate local frame and don't align stack because
    // we not even call stub code (we generate the code inline)
    // and there is no safepoint on this path.

    // Load java parameters.
    // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = Z_esp;
    const Register crc     = Z_ARG1;  // crc value
    const Register data    = Z_ARG2;  // address of java byte value (kernel_crc32 needs address)
    const Register dataLen = Z_ARG3;  // source data len (1 byte). Not used because calling the single-byte emitter.
    const Register table   = Z_ARG4;  // address of crc32 table

    // Arguments are reversed on java expression stack.
    __ z_la(data, 3+1*wordSize, argP);  // byte value (stack address).
                                        // Being passed as an int, the single byte is at offset +3.
    __ z_llgf(crc, 2 * wordSize, argP); // Current crc state, zero extend to 64 bit to have a clean register.

    StubRoutines::zarch::generate_load_crc_table_addr(_masm, table);
    __ kernel_crc32_singleByte(crc, data, dataLen, table, Z_R1, true);

    // Restore caller sp for c2i case.
    __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.

    __ z_br(Z_R14);

    BLOCK_COMMENT("} CRC32_update");

    // Use a previously generated vanilla native entry as the slow path.
    BIND(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native), Z_R1);
    return __ addr_at(entry_off);
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
    uint64_t entry_off = __ offset();
    Label    slow_path;

    // If we need a safepoint check, generate full interpreter entry.
    __ safepoint_poll(slow_path, Z_R1);

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters.
    // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = Z_esp;
    const Register crc     = Z_ARG1;  // crc value
    const Register data    = Z_ARG2;  // address of java byte array
    const Register dataLen = Z_ARG3;  // source data len
    const Register table   = Z_ARG4;  // address of crc32 table
    const Register t0      = Z_R10;   // work reg for kernel* emitters
    const Register t1      = Z_R11;   // work reg for kernel* emitters
    const Register t2      = Z_R12;   // work reg for kernel* emitters
    const Register t3      = Z_R13;   // work reg for kernel* emitters

    // Arguments are reversed on java expression stack.
    // Calculate address of start element.
    if (kind == Interpreter::java_util_zip_CRC32_updateByteBuffer) { // Used for "updateByteBuffer direct".
      // crc     @ (SP + 5W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to long array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off
      BLOCK_COMMENT("CRC32_updateByteBuffer {");
      __ z_llgf(crc,    5*wordSize, argP);  // current crc state
      __ z_lg(data,     3*wordSize, argP);  // start of byte buffer
      __ z_agf(data,    2*wordSize, argP);  // Add byte buffer offset.
      __ z_lgf(dataLen, 1*wordSize, argP);  // #bytes to process
    } else {                                                         // Used for "updateBytes update".
      // crc     @ (SP + 4W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to byte array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off + base_offset
      BLOCK_COMMENT("CRC32_updateBytes {");
      __ z_llgf(crc,    4*wordSize, argP);  // current crc state
      __ z_lg(data,     3*wordSize, argP);  // start of byte buffer
      __ z_agf(data,    2*wordSize, argP);  // Add byte buffer offset.
      __ z_lgf(dataLen, 1*wordSize, argP);  // #bytes to process
      __ z_aghi(data, arrayOopDesc::base_offset_in_bytes(T_BYTE));
    }

    StubRoutines::zarch::generate_load_crc_table_addr(_masm, table);

    __ resize_frame(-(6*8), Z_R0, true); // Resize frame to provide add'l space to spill 5 registers.
    __ z_stmg(t0, t3, 1*8, Z_SP);        // Spill regs 10..13 to make them available as work registers.
    __ kernel_crc32_1word(crc, data, dataLen, table, t0, t1, t2, t3, true);
    __ z_lmg(t0, t3, 1*8, Z_SP);         // Spill regs 10..13 back from stack.

    // Restore caller sp for c2i case.
    __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.

    __ z_br(Z_R14);

    BLOCK_COMMENT("} CRC32_update{Bytes|ByteBuffer}");

    // Use a previously generated vanilla native entry as the slow path.
    BIND(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native), Z_R1);
    return __ addr_at(entry_off);
  }

  return NULL;
}


/**
 * Method entry for intrinsic-candidate (non-native) methods:
 *   int java.util.zip.CRC32C.updateBytes(           int crc, byte[] b,  int off, int end)
 *   int java.util.zip.CRC32C.updateDirectByteBuffer(int crc, long* buf, int off, int end)
 * Unlike CRC32, CRC32C does not have any methods marked as native
 * CRC32C also uses an "end" variable instead of the length variable CRC32 uses
 */
address TemplateInterpreterGenerator::generate_CRC32C_updateBytes_entry(AbstractInterpreter::MethodKind kind) {

  if (UseCRC32CIntrinsics) {
    uint64_t entry_off = __ offset();

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters.
    // Z_esp is callers operand stack pointer, i.e. it points to the parameters.
    const Register argP    = Z_esp;
    const Register crc     = Z_ARG1;  // crc value
    const Register data    = Z_ARG2;  // address of java byte array
    const Register dataLen = Z_ARG3;  // source data len
    const Register table   = Z_ARG4;  // address of crc32 table
    const Register t0      = Z_R10;   // work reg for kernel* emitters
    const Register t1      = Z_R11;   // work reg for kernel* emitters
    const Register t2      = Z_R12;   // work reg for kernel* emitters
    const Register t3      = Z_R13;   // work reg for kernel* emitters

    // Arguments are reversed on java expression stack.
    // Calculate address of start element.
    if (kind == Interpreter::java_util_zip_CRC32C_updateDirectByteBuffer) { // Used for "updateByteBuffer direct".
      // crc     @ (SP + 5W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to long array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off
      BLOCK_COMMENT("CRC32C_updateDirectByteBuffer {");
      __ z_llgf(crc,    5*wordSize, argP);  // current crc state
      __ z_lg(data,     3*wordSize, argP);  // start of byte buffer
      __ z_agf(data,    2*wordSize, argP);  // Add byte buffer offset.
      __ z_lgf(dataLen, 1*wordSize, argP);  // #bytes to process, calculated as
      __ z_sgf(dataLen, Address(argP, 2*wordSize));  // (end_index - offset)
    } else {                                                                // Used for "updateBytes update".
      // crc     @ (SP + 4W) (32bit)
      // buf     @ (SP + 3W) (64bit ptr to byte array)
      // off     @ (SP + 2W) (32bit)
      // dataLen @ (SP + 1W) (32bit)
      // data = buf + off + base_offset
      BLOCK_COMMENT("CRC32C_updateBytes {");
      __ z_llgf(crc,    4*wordSize, argP);  // current crc state
      __ z_lg(data,     3*wordSize, argP);  // start of byte buffer
      __ z_agf(data,    2*wordSize, argP);  // Add byte buffer offset.
      __ z_lgf(dataLen, 1*wordSize, argP);  // #bytes to process, calculated as
      __ z_sgf(dataLen, Address(argP, 2*wordSize));  // (end_index - offset)
      __ z_aghi(data, arrayOopDesc::base_offset_in_bytes(T_BYTE));
    }

    StubRoutines::zarch::generate_load_crc32c_table_addr(_masm, table);

    __ resize_frame(-(6*8), Z_R0, true); // Resize frame to provide add'l space to spill 5 registers.
    __ z_stmg(t0, t3, 1*8, Z_SP);        // Spill regs 10..13 to make them available as work registers.
    __ kernel_crc32_1word(crc, data, dataLen, table, t0, t1, t2, t3, false);
    __ z_lmg(t0, t3, 1*8, Z_SP);         // Spill regs 10..13 back from stack.

    // Restore caller sp for c2i case.
    __ resize_frame_absolute(Z_R10, Z_R0, true); // Cut the stack back to where the caller started.

    __ z_br(Z_R14);

    BLOCK_COMMENT("} CRC32C_update{Bytes|DirectByteBuffer}");
    return __ addr_at(entry_off);
  }

  return NULL;
}

void TemplateInterpreterGenerator::bang_stack_shadow_pages(bool native_call) {
  // Quick & dirty stack overflow checking: bang the stack & handle trap.
  // Note that we do the banging after the frame is setup, since the exception
  // handling code expects to find a valid interpreter frame on the stack.
  // Doing the banging earlier fails if the caller frame is not an interpreter
  // frame.
  // (Also, the exception throwing code expects to unlock any synchronized
  // method receiver, so do the banging after locking the receiver.)

  // Bang each page in the shadow zone. We can't assume it's been done for
  // an interpreter frame with greater than a page of locals, so each page
  // needs to be checked. Only true for non-native. For native, we only bang the last page.
  const int page_size      = os::vm_page_size();
  const int n_shadow_pages = (int)(StackOverflow::stack_shadow_zone_size()/page_size);
  const int start_page_num = native_call ? n_shadow_pages : 1;
  for (int pages = start_page_num; pages <= n_shadow_pages; pages++) {
    __ bang_stack_with_offset(pages*page_size);
  }
}

//-----------------------------------------------------------------------------
// Exceptions

void TemplateInterpreterGenerator::generate_throw_exception() {

  BLOCK_COMMENT("throw_exception {");

  // Entry point in previous activation (i.e., if the caller was interpreted).
  Interpreter::_rethrow_exception_entry = __ pc();
  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // Frame accessors use Z_fp.
  // Z_ARG1 (==Z_tos): exception
  // Z_ARG2          : Return address/pc that threw exception.
  __ restore_bcp();    // R13 points to call/send.
  __ restore_locals();

  // Fallthrough, no need to restore Z_esp.

  // Entry point for exceptions thrown within interpreter code.
  Interpreter::_throw_exception_entry = __ pc();
  // Expression stack is undefined here.
  // Z_ARG1 (==Z_tos): exception
  // Z_bcp: exception bcp
  __ verify_oop(Z_ARG1);
  __ z_lgr(Z_ARG2, Z_ARG1);

  // Expression stack must be empty before entering the VM in case of
  // an exception.
  __ empty_expression_stack();
  // Find exception handler address and preserve exception oop.
  const Register Rpreserved_exc_oop = Z_tmp_1;
  __ call_VM(Rpreserved_exc_oop,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::exception_handler_for_exception),
             Z_ARG2);
  // Z_RET: exception handler entry point
  // Z_bcp: bcp for exception handler
  __ push_ptr(Rpreserved_exc_oop); // Push exception which is now the only value on the stack.
  __ z_br(Z_RET); // Jump to exception handler (may be _remove_activation_entry!).

  // If the exception is not handled in the current frame the frame is
  // removed and the exception is rethrown (i.e. exception
  // continuation is _rethrow_exception).
  //
  // Note: At this point the bci is still the bci for the instruction
  // which caused the exception and the expression stack is
  // empty. Thus, for any VM calls at this point, GC will find a legal
  // oop map (with empty expression stack).

  //
  // JVMTI PopFrame support
  //

  Interpreter::_remove_activation_preserving_args_entry = __ pc();
  __ z_lg(Z_fp, _z_parent_ijava_frame_abi(callers_sp), Z_SP);
  __ empty_expression_stack();
  // Set the popframe_processing bit in pending_popframe_condition
  // indicating that we are currently handling popframe, so that
  // call_VMs that may happen later do not trigger new popframe
  // handling cycles.
  __ load_sized_value(Z_tmp_1, Address(Z_thread, JavaThread::popframe_condition_offset()), 4, false /*signed*/);
  __ z_oill(Z_tmp_1, JavaThread::popframe_processing_bit);
  __ z_sty(Z_tmp_1, thread_(popframe_condition));

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
    NearLabel caller_not_deoptimized;
    __ z_lg(Z_ARG1, _z_parent_ijava_frame_abi(return_pc), Z_fp);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, InterpreterRuntime::interpreter_contains), Z_ARG1);
    __ compareU64_and_branch(Z_RET, (intptr_t)0, Assembler::bcondNotEqual, caller_not_deoptimized);

    // Compute size of arguments for saving when returning to
    // deoptimized caller.
    __ get_method(Z_ARG2);
    __ z_lg(Z_ARG2, Address(Z_ARG2, Method::const_offset()));
    __ z_llgh(Z_ARG2, Address(Z_ARG2, ConstMethod::size_of_parameters_offset()));
    __ z_sllg(Z_ARG2, Z_ARG2, Interpreter::logStackElementSize); // slots 2 bytes
    __ restore_locals();
    // Compute address of args to be saved.
    __ z_lgr(Z_ARG3, Z_locals);
    __ z_slgr(Z_ARG3, Z_ARG2);
    __ add2reg(Z_ARG3, wordSize);
    // Save these arguments.
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::popframe_preserve_args),
                    Z_thread, Z_ARG2, Z_ARG3);

    __ remove_activation(vtos, Z_R14,
                         /* throw_monitor_exception */ false,
                         /* install_monitor_exception */ false,
                         /* notify_jvmdi */ false);

    // Inform deoptimization that it is responsible for restoring
    // these arguments.
    __ store_const(thread_(popframe_condition),
                   JavaThread::popframe_force_deopt_reexecution_bit,
                   Z_tmp_1, false);

    // Continue in deoptimization handler.
    __ z_br(Z_R14);

    __ bind(caller_not_deoptimized);
  }

  // Clear the popframe condition flag.
  __ clear_mem(thread_(popframe_condition), sizeof(int));

  __ remove_activation(vtos,
                       noreg,  // Retaddr is not used.
                       false,  // throw_monitor_exception
                       false,  // install_monitor_exception
                       false); // notify_jvmdi
  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // Restore frame pointer.
  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();
  // The method data pointer was incremented already during
  // call profiling. We have to restore the mdp for the current bcp.
  if (ProfileInterpreter) {
    __ set_method_data_pointer_for_bcp();
  }
#if INCLUDE_JVMTI
  {
    Label L_done;

    __ z_cli(0, Z_bcp, Bytecodes::_invokestatic);
    __ z_brc(Assembler::bcondNotEqual, L_done);

    // The member name argument must be restored if _invokestatic is
    // re-executed after a PopFrame call.  Detect such a case in the
    // InterpreterRuntime function and return the member name
    // argument, or NULL.
    __ z_lg(Z_ARG2, Address(Z_locals));
    __ get_method(Z_ARG3);
    __ call_VM(Z_tmp_1,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::member_name_arg_or_null),
               Z_ARG2, Z_ARG3, Z_bcp);

    __ z_ltgr(Z_tmp_1, Z_tmp_1);
    __ z_brc(Assembler::bcondEqual, L_done);

    __ z_stg(Z_tmp_1, Address(Z_esp, wordSize));
    __ bind(L_done);
  }
#endif // INCLUDE_JVMTI
  __ dispatch_next(vtos);
  // End of PopFrame support.
  Interpreter::_remove_activation_entry = __ pc();

  // In between activations - previous activation type unknown yet
  // compute continuation point - the continuation point expects the
  // following registers set up:
  //
  // Z_ARG1 (==Z_tos): exception
  // Z_ARG2          : return address/pc that threw exception

  Register return_pc = Z_tmp_1;
  Register handler   = Z_tmp_2;
   assert(return_pc->is_nonvolatile(), "use non-volatile reg. to preserve exception pc");
   assert(handler->is_nonvolatile(),   "use non-volatile reg. to handler pc");
  __ asm_assert_ijava_state_magic(return_pc/*tmp*/); // The top frame should be an interpreter frame.
  __ z_lg(return_pc, _z_parent_ijava_frame_abi(return_pc), Z_fp);

  // Moved removing the activation after VM call, because the new top
  // frame does not necessarily have the z_abi_160 required for a VM
  // call (e.g. if it is compiled).

  __ super_call_VM_leaf(CAST_FROM_FN_PTR(address,
                                         SharedRuntime::exception_handler_for_return_address),
                        Z_thread, return_pc);
  __ z_lgr(handler, Z_RET); // Save exception handler.

  // Preserve exception over this code sequence.
  __ pop_ptr(Z_ARG1);
  __ set_vm_result(Z_ARG1);
  // Remove the activation (without doing throws on illegalMonitorExceptions).
  __ remove_activation(vtos, noreg/*ret.pc already loaded*/, false/*throw exc*/, true/*install exc*/, false/*notify jvmti*/);
  __ z_lg(Z_fp, _z_abi(callers_sp), Z_SP); // Restore frame pointer.

  __ get_vm_result(Z_ARG1);     // Restore exception.
  __ verify_oop(Z_ARG1);
  __ z_lgr(Z_ARG2, return_pc);  // Restore return address.

#ifdef ASSERT
  // The return_pc in the new top frame is dead... at least that's my
  // current understanding. To assert this I overwrite it.
  // Note: for compiled frames the handler is the deopt blob
  // which writes Z_ARG2 into the return_pc slot.
  __ load_const_optimized(return_pc, 0xb00b1);
  __ z_stg(return_pc, _z_parent_ijava_frame_abi(return_pc), Z_SP);
#endif

  // Z_ARG1 (==Z_tos): exception
  // Z_ARG2          : return address/pc that threw exception

  // Note that an "issuing PC" is actually the next PC after the call.
  __ z_br(handler);         // Jump to exception handler of caller.

  BLOCK_COMMENT("} throw_exception");
}

//
// JVMTI ForceEarlyReturn support
//
address TemplateInterpreterGenerator::generate_earlyret_entry_for (TosState state) {
  address entry = __ pc();

  BLOCK_COMMENT("earlyret_entry {");

  __ z_lg(Z_fp, _z_parent_ijava_frame_abi(callers_sp), Z_SP);
  __ restore_bcp();
  __ restore_locals();
  __ restore_esp();
  __ empty_expression_stack();
  __ load_earlyret_value(state);

  Register RjvmtiState = Z_tmp_1;
  __ z_lg(RjvmtiState, thread_(jvmti_thread_state));
  __ store_const(Address(RjvmtiState, JvmtiThreadState::earlyret_state_offset()),
                 JvmtiThreadState::earlyret_inactive, 4, 4, Z_R0_scratch);

  if (state == itos) {
    // Narrow result if state is itos but result type is smaller.
    // Need to narrow in the return bytecode rather than in generate_return_entry
    // since compiled code callers expect the result to already be narrowed.
    __ narrow(Z_tos, Z_tmp_1); /* fall through */
  }
  __ remove_activation(state,
                       Z_tmp_1, // retaddr
                       false,   // throw_monitor_exception
                       false,   // install_monitor_exception
                       true);   // notify_jvmdi
  __ z_br(Z_tmp_1);

  BLOCK_COMMENT("} earlyret_entry");

  return entry;
}

//-----------------------------------------------------------------------------
// Helper for vtos entry point generation.

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
  aep = __ pc(); __ push_ptr(); __ z_bru(L);
  fep = __ pc(); __ push_f();   __ z_bru(L);
  dep = __ pc(); __ push_d();   __ z_bru(L);
  lep = __ pc(); __ push_l();   __ z_bru(L);
  bep = cep = sep =
  iep = __ pc(); __ push_i();
  vep = __ pc();
  __ bind(L);
  generate_and_dispatch(t);
}

//-----------------------------------------------------------------------------

#ifndef PRODUCT
address TemplateInterpreterGenerator::generate_trace_code(TosState state) {
  address entry = __ pc();
  NearLabel counter_below_trace_threshold;

  if (TraceBytecodesAt > 0) {
    // Skip runtime call, if the trace threshold is not yet reached.
    __ load_absolute_address(Z_tmp_1, (address)&BytecodeCounter::_counter_value);
    __ load_absolute_address(Z_tmp_2, (address)&TraceBytecodesAt);
    __ load_sized_value(Z_tmp_1, Address(Z_tmp_1), 4, false /*signed*/);
    __ load_sized_value(Z_tmp_2, Address(Z_tmp_2), 8, false /*signed*/);
    __ compareU64_and_branch(Z_tmp_1, Z_tmp_2, Assembler::bcondLow, counter_below_trace_threshold);
  }

  int offset2 = state == ltos || state == dtos ? 2 : 1;

  __ push(state);
  // Preserved return pointer is in Z_R14.
  // InterpreterRuntime::trace_bytecode() preserved and returns the value passed as second argument.
  __ z_lgr(Z_ARG2, Z_R14);
  __ z_lg(Z_ARG3, Address(Z_esp, Interpreter::expr_offset_in_bytes(0)));
  if (WizardMode) {
    __ z_lgr(Z_ARG4, Z_esp); // Trace Z_esp in WizardMode.
  } else {
    __ z_lg(Z_ARG4, Address(Z_esp, Interpreter::expr_offset_in_bytes(offset2)));
  }
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::trace_bytecode), Z_ARG2, Z_ARG3, Z_ARG4);
  __ z_lgr(Z_R14, Z_RET); // Estore return address (see above).
  __ pop(state);

  __ bind(counter_below_trace_threshold);
  __ z_br(Z_R14); // return

  return entry;
}

// Make feasible for old CPUs.
void TemplateInterpreterGenerator::count_bytecode() {
  __ load_absolute_address(Z_R1_scratch, (address) &BytecodeCounter::_counter_value);
  __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
}

void TemplateInterpreterGenerator::histogram_bytecode(Template * t) {
  __ load_absolute_address(Z_R1_scratch, (address)&BytecodeHistogram::_counters[ t->bytecode() ]);
  __ add2mem_32(Address(Z_R1_scratch), 1, Z_tmp_1);
}

void TemplateInterpreterGenerator::histogram_bytecode_pair(Template * t) {
  Address  index_addr(Z_tmp_1, (intptr_t) 0);
  Register index = Z_tmp_2;

  // Load previous index.
  __ load_absolute_address(Z_tmp_1, (address) &BytecodePairHistogram::_index);
  __ mem2reg_opt(index, index_addr, false);

  // Mask with current bytecode and store as new previous index.
  __ z_srl(index, BytecodePairHistogram::log2_number_of_codes);
  __ load_const_optimized(Z_R0_scratch,
                          (int)t->bytecode() << BytecodePairHistogram::log2_number_of_codes);
  __ z_or(index, Z_R0_scratch);
  __ reg2mem_opt(index, index_addr, false);

  // Load counter array's address.
  __ z_lgfr(index, index);   // Sign extend for addressing.
  __ z_sllg(index, index, LogBytesPerInt);  // index2bytes
  __ load_absolute_address(Z_R1_scratch,
                           (address) &BytecodePairHistogram::_counters);
  // Add index and increment counter.
  __ z_agr(Z_R1_scratch, index);
  __ add2mem_32(Address(Z_R1_scratch), 1, Z_tmp_1);
}

void TemplateInterpreterGenerator::trace_bytecode(Template* t) {
  // Call a little run-time stub to avoid blow-up for each bytecode.
  // The run-time runtime saves the right registers, depending on
  // the tosca in-state for the given template.
  address entry = Interpreter::trace_code(t->tos_in());
  guarantee(entry != NULL, "entry must have been generated");
  __ call_stub(entry);
}

void TemplateInterpreterGenerator::stop_interpreter_at() {
  NearLabel L;

  __ load_absolute_address(Z_tmp_1, (address)&BytecodeCounter::_counter_value);
  __ load_absolute_address(Z_tmp_2, (address)&StopInterpreterAt);
  __ load_sized_value(Z_tmp_1, Address(Z_tmp_1), 4, false /*signed*/);
  __ load_sized_value(Z_tmp_2, Address(Z_tmp_2), 8, false /*signed*/);
  __ compareU64_and_branch(Z_tmp_1, Z_tmp_2, Assembler::bcondLow, L);
  assert(Z_tmp_1->is_nonvolatile(), "must be nonvolatile to preserve Z_tos");
  assert(Z_F8->is_nonvolatile(), "must be nonvolatile to preserve Z_ftos");
  __ z_lgr(Z_tmp_1, Z_tos);      // Save tos.
  __ z_lgr(Z_tmp_2, Z_bytecode); // Save Z_bytecode.
  __ z_ldr(Z_F8, Z_ftos);        // Save ftos.
  // Use -XX:StopInterpreterAt=<num> to set the limit
  // and break at breakpoint().
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, breakpoint), false);
  __ z_lgr(Z_tos, Z_tmp_1);      // Restore tos.
  __ z_lgr(Z_bytecode, Z_tmp_2); // Save Z_bytecode.
  __ z_ldr(Z_ftos, Z_F8);        // Restore ftos.
  __ bind(L);
}

#endif // !PRODUCT
