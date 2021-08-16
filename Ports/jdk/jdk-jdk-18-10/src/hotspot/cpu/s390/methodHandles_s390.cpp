/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2017 SAP SE. All rights reserved.
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
#include "jvm.h"
#include "asm/macroAssembler.inline.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/interpreter.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/preserveException.hpp"

#ifdef PRODUCT
#define __ _masm->
#define BLOCK_COMMENT(str) /* nothing */
#else
#define __ (Verbose ? (_masm->block_comment(FILE_AND_LINE),_masm):_masm)->
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

// Workaround for C++ overloading nastiness on '0' for RegisterOrConstant.
static RegisterOrConstant constant(int value) {
  return RegisterOrConstant(value);
}

void MethodHandles::load_klass_from_Class(MacroAssembler* _masm, Register klass_reg,
                                          Register temp_reg, Register temp2_reg) {
  if (VerifyMethodHandles) {
    verify_klass(_masm, klass_reg, VM_CLASS_ID(java_lang_Class),
                 temp_reg, temp2_reg, "MH argument is a Class");
  }
  __ z_lg(klass_reg, Address(klass_reg, java_lang_Class::klass_offset()));
}


#ifdef ASSERT
static int check_nonzero(const char* xname, int x) {
  assert(x != 0, "%s should be nonzero", xname);
  return x;
}
#define NONZERO(x) check_nonzero(#x, x)
#else
#define NONZERO(x) (x)
#endif

#ifdef ASSERT
void MethodHandles::verify_klass(MacroAssembler* _masm,
                                 Register obj_reg, vmClassID klass_id,
                                 Register temp_reg, Register temp2_reg,
                                 const char* error_message) {

  InstanceKlass** klass_addr = vmClasses::klass_addr_at(klass_id);
  Klass* klass = vmClasses::klass_at(klass_id);

  assert(temp_reg != Z_R0 && // Is used as base register!
         temp_reg != noreg && temp2_reg != noreg, "need valid registers!");

  NearLabel L_ok, L_bad;

  BLOCK_COMMENT("verify_klass {");

  __ verify_oop(obj_reg, FILE_AND_LINE);
  __ compareU64_and_branch(obj_reg, (intptr_t)0L, Assembler::bcondEqual, L_bad);
  __ load_klass(temp_reg, obj_reg);
  // klass_addr is a klass in allstatic SystemDictionaryHandles. Can't get GCed.
  __ load_const_optimized(temp2_reg, (address)klass_addr);
  __ z_lg(temp2_reg, Address(temp2_reg));
  __ compareU64_and_branch(temp_reg, temp2_reg, Assembler::bcondEqual, L_ok);

  intptr_t super_check_offset = klass->super_check_offset();
  __ z_lg(temp_reg, Address(temp_reg, super_check_offset));
  __ compareU64_and_branch(temp_reg, temp2_reg, Assembler::bcondEqual, L_ok);
  __ BIND(L_bad);
  __ stop(error_message);
  __ BIND(L_ok);

  BLOCK_COMMENT("} verify_klass");
}

void MethodHandles::verify_ref_kind(MacroAssembler* _masm, int ref_kind,
                                    Register member_reg, Register temp  ) {
  NearLabel L;
  BLOCK_COMMENT("verify_ref_kind {");

  __ z_llgf(temp,
            Address(member_reg,
                    NONZERO(java_lang_invoke_MemberName::flags_offset())));
  __ z_srl(temp,  java_lang_invoke_MemberName::MN_REFERENCE_KIND_SHIFT);
  __ z_nilf(temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_MASK);
  __ compare32_and_branch(temp, constant(ref_kind), Assembler::bcondEqual, L);

  {
    char *buf = NEW_C_HEAP_ARRAY(char, 100, mtInternal);

    jio_snprintf(buf, 100, "verify_ref_kind expected %x", ref_kind);
    if (ref_kind == JVM_REF_invokeVirtual || ref_kind == JVM_REF_invokeSpecial) {
      // Could do this for all ref_kinds, but would explode assembly code size.
      trace_method_handle(_masm, buf);
    }
    __ stop(buf);
  }

  BLOCK_COMMENT("} verify_ref_kind");

  __ bind(L);
}
#endif // ASSERT

void MethodHandles::jump_from_method_handle(MacroAssembler* _masm, Register method, Register target,
                                            Register temp, bool for_compiler_entry) {
  assert(method == Z_method, "interpreter calling convention");
  __ verify_method_ptr(method);

  assert(target != method, "don 't you kill the method reg!");

  Label L_no_such_method;

  if (!for_compiler_entry && JvmtiExport::can_post_interpreter_events()) {
    // JVMTI events, such as single-stepping, are implemented partly
    // by avoiding running compiled code in threads for which the
    // event is enabled. Check here for interp_only_mode if these
    // events CAN be enabled.
    __ verify_thread();

    Label run_compiled_code;

    __ load_and_test_int(temp, Address(Z_thread, JavaThread::interp_only_mode_offset()));
    __ z_bre(run_compiled_code);

    // Null method test is replicated below in compiled case,
    // it might be able to address across the verify_thread().
    __ z_ltgr(temp, method);
    __ z_bre(L_no_such_method);

    __ z_lg(target, Address(method, Method::interpreter_entry_offset()));
    __ z_br(target);

    __ bind(run_compiled_code);
  }

  // Compiled case, either static or fall-through from runtime conditional.
  __ z_ltgr(temp, method);
  __ z_bre(L_no_such_method);

  ByteSize offset = for_compiler_entry ?
                       Method::from_compiled_offset() : Method::from_interpreted_offset();
  Address method_from(method, offset);

  __ z_lg(target, method_from);
  __ z_br(target);

  __ bind(L_no_such_method);
  assert(StubRoutines::throw_AbstractMethodError_entry() != NULL, "not yet generated!");
  __ load_const_optimized(target, StubRoutines::throw_AbstractMethodError_entry());
  __ z_br(target);
}

void MethodHandles::jump_to_lambda_form(MacroAssembler* _masm,
                                        Register recv, Register method_temp,
                                        Register temp2, Register temp3,
                                        bool for_compiler_entry) {

  // This is the initial entry point of a lazy method handle.
  // After type checking, it picks up the invoker from the LambdaForm.
  assert_different_registers(recv, method_temp, temp2, temp3);
  assert(method_temp == Z_method, "required register for loading method");

  BLOCK_COMMENT("jump_to_lambda_form {");

  // Load the invoker, as MH -> MH.form -> LF.vmentry
  __ verify_oop(recv, FILE_AND_LINE);
  __ load_heap_oop(method_temp,
                   Address(recv,
                           NONZERO(java_lang_invoke_MethodHandle::form_offset())),
                   noreg, noreg, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ load_heap_oop(method_temp,
                   Address(method_temp,
                           NONZERO(java_lang_invoke_LambdaForm::vmentry_offset())),
                   noreg, noreg, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ load_heap_oop(method_temp,
                   Address(method_temp,
                           NONZERO(java_lang_invoke_MemberName::method_offset())),
                   noreg, noreg, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ z_lg(method_temp,
          Address(method_temp,
                  NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset())));

  if (VerifyMethodHandles && !for_compiler_entry) {
    // Make sure recv is already on stack.
    NearLabel L;
    Address paramSize(temp2, ConstMethod::size_of_parameters_offset());

    __ z_lg(temp2, Address(method_temp, Method::const_offset()));
    __ load_sized_value(temp2, paramSize, sizeof(u2), /*is_signed*/ false);
    // if (temp2 != recv) stop
    __ z_lg(temp2, __ argument_address(temp2, temp2, 0));
    __ compare64_and_branch(temp2, recv, Assembler::bcondEqual, L);
    __ stop("receiver not on stack");
    __ BIND(L);
  }

  jump_from_method_handle(_masm, method_temp, temp2, Z_R0, for_compiler_entry);

  BLOCK_COMMENT("} jump_to_lambda_form");
}

// code generation
address MethodHandles::generate_method_handle_interpreter_entry(MacroAssembler* _masm,
                                                                vmIntrinsics::ID iid) {
  const bool not_for_compiler_entry = false;  // This is the interpreter entry.
  assert(is_signature_polymorphic(iid), "expected invoke iid");

  if (iid == vmIntrinsics::_invokeGeneric || iid == vmIntrinsics::_compiledLambdaForm) {
    // Perhaps surprisingly, the symbolic references visible to Java
    // are not directly used. They are linked to Java-generated
    // adapters via MethodHandleNatives.linkMethod. They all allow an
    // appendix argument.
    __ should_not_reach_here();           // Empty stubs make SG sick.
    return NULL;
  }

  // No need in interpreter entry for linkToNative for now.
  // Interpreter calls compiled entry through i2c.
  if (iid == vmIntrinsics::_linkToNative) {
    __ should_not_reach_here();           // Empty stubs make SG sick.
    return NULL;
  }

  // Z_R10: sender SP (must preserve; see prepare_to_jump_from_interprted)
  // Z_method: method
  // Z_ARG1 (Gargs): incoming argument list (must preserve)
  Register Z_R4_param_size = Z_R4;   // size of parameters
  address code_start = __ pc();

  // Here is where control starts out:
  __ align(CodeEntryAlignment);

  address entry_point = __ pc();

  if (VerifyMethodHandles) {
    Label L;
    BLOCK_COMMENT("verify_intrinsic_id {");

    // Supplement to 8139891: _intrinsic_id exceeded 1-byte size limit.
    if (Method::intrinsic_id_size_in_bytes() == 1) {
      __ z_cli(Address(Z_method, Method::intrinsic_id_offset_in_bytes()), (int)iid);
    } else {
      assert(Method::intrinsic_id_size_in_bytes() == 2, "size error: check Method::_intrinsic_id");
      __ z_lh(Z_R0_scratch, Address(Z_method, Method::intrinsic_id_offset_in_bytes()));
      __ z_chi(Z_R0_scratch, (int)iid);
    }
    __ z_bre(L);

    if (iid == vmIntrinsics::_linkToVirtual || iid == vmIntrinsics::_linkToSpecial) {
      // Could do this for all kinds, but would explode assembly code size.
      trace_method_handle(_masm, "bad Method::intrinsic_id");
    }

    __ stop("bad Method::intrinsic_id");
    __ bind(L);

    BLOCK_COMMENT("} verify_intrinsic_id");
  }

  // First task: Find out how big the argument list is.
  Address Z_R4_first_arg_addr;
  int ref_kind = signature_polymorphic_intrinsic_ref_kind(iid);

  assert(ref_kind != 0 || iid == vmIntrinsics::_invokeBasic,
         "must be _invokeBasic or a linkTo intrinsic");

  if (ref_kind == 0 || MethodHandles::ref_kind_has_receiver(ref_kind)) {
     Address paramSize(Z_R1_scratch, ConstMethod::size_of_parameters_offset());

    __ z_lg(Z_R1_scratch, Address(Z_method, Method::const_offset()));
    __ load_sized_value(Z_R4_param_size, paramSize, sizeof(u2), /*is_signed*/ false);
    Z_R4_first_arg_addr = __ argument_address(Z_R4_param_size, Z_R4_param_size, 0);
  } else {
    DEBUG_ONLY(Z_R4_param_size = noreg);
  }

  Register Z_mh = noreg;
  if (!is_signature_polymorphic_static(iid)) {
    Z_mh = Z_ARG4;
    __ z_lg(Z_mh, Z_R4_first_arg_addr);
    DEBUG_ONLY(Z_R4_param_size = noreg);
  }

  // Z_R4_first_arg_addr is live!

  trace_method_handle_interpreter_entry(_masm, iid);

  if (iid == vmIntrinsics::_invokeBasic) {
    __ pc(); // just for the block comment
    generate_method_handle_dispatch(_masm, iid, Z_mh, noreg, not_for_compiler_entry);
  } else {
    // Adjust argument list by popping the trailing MemberName argument.
    Register Z_recv = noreg;

    if (MethodHandles::ref_kind_has_receiver(ref_kind)) {
      // Load the receiver (not the MH; the actual MemberName's receiver)
      // up from the interpreter stack.
      __ z_lg(Z_recv = Z_R5, Z_R4_first_arg_addr);
      DEBUG_ONLY(Z_R4_param_size = noreg);
    }

    Register Z_member = Z_method;  // MemberName ptr; incoming method ptr is dead now

    __ z_lg(Z_member, __ argument_address(constant(1)));
    __ add2reg(Z_esp, Interpreter::stackElementSize);
    generate_method_handle_dispatch(_masm, iid, Z_recv, Z_member, not_for_compiler_entry);
  }

  return entry_point;
}

void MethodHandles::generate_method_handle_dispatch(MacroAssembler* _masm,
                                                    vmIntrinsics::ID iid,
                                                    Register receiver_reg,
                                                    Register member_reg,
                                                    bool for_compiler_entry) {
  assert(is_signature_polymorphic(iid), "expected invoke iid");

  Register temp1 = for_compiler_entry ? Z_R10 : Z_R6;
  Register temp2 = Z_R12;
  Register temp3 = Z_R11;
  Register temp4 = Z_R13;

  if (for_compiler_entry) {
    assert(receiver_reg == (iid == vmIntrinsics::_linkToStatic ? noreg : Z_ARG1),
           "only valid assignment");
  }
  if (receiver_reg != noreg) {
    assert_different_registers(temp1, temp2, temp3, temp4, receiver_reg);
  }
  if (member_reg != noreg) {
    assert_different_registers(temp1, temp2, temp3, temp4, member_reg);
  }
  if (!for_compiler_entry) {  // Don't trash last SP.
    assert_different_registers(temp1, temp2, temp3, temp4, Z_R10);
  }

  if (iid == vmIntrinsics::_invokeBasic || iid == vmIntrinsics::_linkToNative) {
    if (iid == vmIntrinsics::_linkToNative) {
      assert(for_compiler_entry, "only compiler entry is supported");
    }
    __ pc(); // Just for the block comment.
    // Indirect through MH.form.vmentry.vmtarget.
    jump_to_lambda_form(_masm, receiver_reg, Z_method, Z_R1, temp3, for_compiler_entry);
    return;
  }

  // The method is a member invoker used by direct method handles.
  if (VerifyMethodHandles) {
    // Make sure the trailing argument really is a MemberName (caller responsibility).
    verify_klass(_masm, member_reg,
                 VM_CLASS_ID(MemberName_klass),
                 temp1, temp2,
                 "MemberName required for invokeVirtual etc.");
  }

  Address  member_clazz(   member_reg, NONZERO(java_lang_invoke_MemberName::clazz_offset()));
  Address  member_vmindex( member_reg, NONZERO(java_lang_invoke_MemberName::vmindex_offset()));
  Address  member_vmtarget(member_reg, NONZERO(java_lang_invoke_MemberName::method_offset()));
  Address  vmtarget_method(Z_method, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()));
  Register temp1_recv_klass = temp1;

  if (iid != vmIntrinsics::_linkToStatic) {
    __ verify_oop(receiver_reg, FILE_AND_LINE);
    if (iid == vmIntrinsics::_linkToSpecial) {
      // Don't actually load the klass; just null-check the receiver.
      __ null_check(receiver_reg);
    } else {
      // Load receiver klass itself.
      __ null_check(receiver_reg, Z_R0, oopDesc::klass_offset_in_bytes());
      __ load_klass(temp1_recv_klass, receiver_reg);
      __ verify_klass_ptr(temp1_recv_klass);
    }
    BLOCK_COMMENT("check_receiver {");
    // The receiver for the MemberName must be in receiver_reg.
    // Check the receiver against the MemberName.clazz.
    if (VerifyMethodHandles && iid == vmIntrinsics::_linkToSpecial) {
      // Did not load it above...
      __ load_klass(temp1_recv_klass, receiver_reg);
      __ verify_klass_ptr(temp1_recv_klass);
    }

    if (VerifyMethodHandles && iid != vmIntrinsics::_linkToInterface) {
      NearLabel L_ok;
      Register temp2_defc = temp2;

      __ load_heap_oop(temp2_defc, member_clazz,
                       noreg, noreg, IS_NOT_NULL);
      load_klass_from_Class(_masm, temp2_defc, temp3, temp4);
      __ verify_klass_ptr(temp2_defc);
      __ check_klass_subtype(temp1_recv_klass, temp2_defc, temp3, temp4, L_ok);
      // If we get here, the type check failed!
      __ stop("receiver class disagrees with MemberName.clazz");
      __ bind(L_ok);
    }
    BLOCK_COMMENT("} check_receiver");
  }
  if (iid == vmIntrinsics::_linkToSpecial || iid == vmIntrinsics::_linkToStatic) {
    DEBUG_ONLY(temp1_recv_klass = noreg);  // These guys didn't load the recv_klass.
  }

  // Live registers at this point:
  //   member_reg       - MemberName that was the trailing argument.
  //   temp1_recv_klass - Klass of stacked receiver, if needed.
  //   Z_R10            - Interpreter linkage if interpreted.

  bool method_is_live = false;

  switch (iid) {
    case vmIntrinsics::_linkToSpecial:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeSpecial, member_reg, temp3);
      }
      __ load_heap_oop(Z_method, member_vmtarget,
                       noreg, noreg, IS_NOT_NULL);
      __ z_lg(Z_method, vmtarget_method);
      method_is_live = true;
      break;

    case vmIntrinsics::_linkToStatic:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeStatic, member_reg, temp3);
      }
      __ load_heap_oop(Z_method, member_vmtarget,
                       noreg, noreg, IS_NOT_NULL);
      __ z_lg(Z_method, vmtarget_method);
      method_is_live = true;
      break;

    case vmIntrinsics::_linkToVirtual: {
      // Same as TemplateTable::invokevirtual, minus the CP setup and profiling.
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeVirtual, member_reg, temp3);
      }

      // Pick out the vtable index from the MemberName, and then we can discard it.
      Register temp2_index = temp2;
      __ z_lg(temp2_index, member_vmindex);

      if (VerifyMethodHandles) {
        // if (member_vmindex < 0) stop
        NearLabel L_index_ok;
        __ compare32_and_branch(temp2_index, constant(0), Assembler::bcondNotLow, L_index_ok);
        __ stop("no virtual index");
        __ BIND(L_index_ok);
      }

      // Note: The verifier invariants allow us to ignore MemberName.clazz and vmtarget
      // at this point. And VerifyMethodHandles has already checked clazz, if needed.

      // Get target method and entry point.
      __ lookup_virtual_method(temp1_recv_klass, temp2_index, Z_method);
      method_is_live = true;
      break;
    }

    case vmIntrinsics::_linkToInterface: {
      // Same as TemplateTable::invokeinterface, minus the CP setup
      // and profiling, with different argument motion.
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeInterface, member_reg, temp3);
      }

      Register temp3_intf = temp3;

      __ load_heap_oop(temp3_intf, member_clazz,
                       noreg, noreg, IS_NOT_NULL);
      load_klass_from_Class(_masm, temp3_intf, temp2, temp4);

      Register Z_index = Z_method;

      __ z_lg(Z_index, member_vmindex);

      if (VerifyMethodHandles) {
        NearLabel L;
        // if (member_vmindex < 0) stop
        __ compare32_and_branch(Z_index, constant(0), Assembler::bcondNotLow, L);
        __ stop("invalid vtable index for MH.invokeInterface");
        __ bind(L);
      }

      // Given interface, index, and recv klass, dispatch to the implementation method.
      Label L_no_such_interface;
      __ lookup_interface_method(temp1_recv_klass, temp3_intf,
                                 // Note: next two args must be the same:
                                 Z_index, Z_method, temp2,
                                 L_no_such_interface);
      jump_from_method_handle(_masm, Z_method, temp2, Z_R0, for_compiler_entry);

      __ bind(L_no_such_interface);

      // Throw exception.
      __ load_const_optimized(Z_R1, StubRoutines::throw_IncompatibleClassChangeError_entry());
      __ z_br(Z_R1);
      break;
    }

    default:
      fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
      break;
  }

  if (method_is_live) {
    // Live at this point: Z_method, O5_savedSP (if interpreted).

    // After figuring out which concrete method to call, jump into it.
    // Note that this works in the interpreter with no data motion.
    // But the compiled version will require that rcx_recv be shifted out.
    jump_from_method_handle(_masm, Z_method, temp1, Z_R0, for_compiler_entry);
  }
}

#ifndef PRODUCT
void trace_method_handle_stub(const char* adaptername,
                              oopDesc* mh,
                              intptr_t* sender_sp,
                              intptr_t* args,
                              intptr_t* tracing_fp) {
  bool has_mh = (strstr(adaptername, "/static") == NULL &&
                 strstr(adaptername, "linkTo") == NULL);    // Static linkers don't have MH.
  const char* mh_reg_name = has_mh ? "Z_R4_mh" : "Z_R4";
  log_info(methodhandles)("MH %s %s=" INTPTR_FORMAT " sender_sp=" INTPTR_FORMAT " args=" INTPTR_FORMAT,
                          adaptername, mh_reg_name,
                          p2i(mh), p2i(sender_sp), p2i(args));

  LogTarget(Trace, methodhandles) lt;
  if (lt.is_enabled()) {
    // Dumping last frame with frame::describe.
    ResourceMark rm;
    LogStream ls(lt);
    JavaThread* p = JavaThread::active();

    // may not be needed by safer and unexpensive here
    PreserveExceptionMark pem(Thread::current());
    FrameValues values;

    // Note: We want to allow trace_method_handle from any call site.
    // While trace_method_handle creates a frame, it may be entered
    // without a valid return PC in Z_R14 (e.g. not just after a call).
    // Walking that frame could lead to failures due to that invalid PC.
    // => carefully detect that frame when doing the stack walking.

    // Walk up to the right frame using the "tracing_fp" argument.
    frame cur_frame = os::current_frame(); // Current C frame.

    while (cur_frame.fp() != tracing_fp) {
      cur_frame = os::get_sender_for_C_frame(&cur_frame);
    }

    // Safely create a frame and call frame::describe.
    intptr_t *dump_sp = cur_frame.sender_sp();
    intptr_t *dump_fp = cur_frame.link();

    bool walkable = has_mh; // Whether the traced frame shoud be walkable.

    // The sender for cur_frame is the caller of trace_method_handle.
    if (walkable) {
      // The previous definition of walkable may have to be refined
      // if new call sites cause the next frame constructor to start
      // failing. Alternatively, frame constructors could be
      // modified to support the current or future non walkable
      // frames (but this is more intrusive and is not considered as
      // part of this RFE, which will instead use a simpler output).
      frame dump_frame = frame(dump_sp);
      dump_frame.describe(values, 1);
    } else {
      // Robust dump for frames which cannot be constructed from sp/younger_sp
      // Add descriptions without building a Java frame to avoid issues.
      values.describe(-1, dump_fp, "fp for #1 <not parsed, cannot trust pc>");
      values.describe(-1, dump_sp, "sp");
    }

    bool has_args = has_mh; // Whether Z_esp is meaningful.

    // Mark args, if seems valid (may not be valid for some adapters).
    if (has_args) {
      if ((args >= dump_sp) && (args < dump_fp)) {
        values.describe(-1, args, "*Z_esp");
      }
    }

    // Note: the unextended_sp may not be correct.
    ls.print_cr("  stack layout:");
    values.print_on(p, &ls);
    if (has_mh && oopDesc::is_oop(mh)) {
      mh->print_on(&ls);
      if (java_lang_invoke_MethodHandle::is_instance(mh)) {
        java_lang_invoke_MethodHandle::form(mh)->print_on(&ls);
      }
    }
  }
}

void MethodHandles::trace_method_handle(MacroAssembler* _masm, const char* adaptername) {
  if (!log_is_enabled(Info, methodhandles)) { return; }

  // If arg registers are contiguous, we can use STMG/LMG.
  assert((Z_ARG5->encoding() - Z_ARG1->encoding() + 1) == RegisterImpl::number_of_arg_registers, "Oops");

  BLOCK_COMMENT("trace_method_handle {");

  // Save argument registers (they are used in raise exception stub).
  // Argument registers have contiguous register numbers -> we can use stmg/lmg.
  __ z_stmg(Z_ARG1, Z_ARG5, 16, Z_SP);

  // Setup arguments.
  __ z_lgr(Z_ARG2, Z_ARG4); // mh, see generate_method_handle_interpreter_entry()
  __ z_lgr(Z_ARG3, Z_R10);  // sender_sp
  __ z_lgr(Z_ARG4, Z_esp);
  __ load_const_optimized(Z_ARG1, (void *)adaptername);
  __ z_lgr(Z_ARG5, Z_SP);   // tracing_fp
  __ save_return_pc();      // saves Z_R14
  __ push_frame_abi160(0);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, trace_method_handle_stub));
  __ pop_frame();
  __ restore_return_pc();   // restores to Z_R14

  // Restore argument registers
  __ z_lmg(Z_ARG1, Z_ARG5, 16, Z_SP);
  __ zap_from_to(Z_SP, Z_SP, Z_R0, Z_R1, 50, -1);
  __ zap_from_to(Z_SP, Z_SP, Z_R0, Z_R1, -1, 5);

  BLOCK_COMMENT("} trace_method_handle");
}
#endif // !PRODUCT
