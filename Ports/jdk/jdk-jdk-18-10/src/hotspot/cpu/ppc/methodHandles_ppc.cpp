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

#define __ _masm->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) // nothing
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

// Workaround for C++ overloading nastiness on '0' for RegisterOrConstant.
inline static RegisterOrConstant constant(int value) {
  return RegisterOrConstant(value);
}

void MethodHandles::load_klass_from_Class(MacroAssembler* _masm, Register klass_reg,
                                          Register temp_reg, Register temp2_reg) {
  if (VerifyMethodHandles) {
    verify_klass(_masm, klass_reg, VM_CLASS_ID(java_lang_Class),
                 temp_reg, temp2_reg, "MH argument is a Class");
  }
  __ ld(klass_reg, java_lang_Class::klass_offset(), klass_reg);
}

#ifdef ASSERT
static int check_nonzero(const char* xname, int x) {
  assert(x != 0, "%s should be nonzero", xname);
  return x;
}
#define NONZERO(x) check_nonzero(#x, x)
#else //ASSERT
#define NONZERO(x) (x)
#endif //ASSERT

#ifdef ASSERT
void MethodHandles::verify_klass(MacroAssembler* _masm,
                                 Register obj_reg, vmClassID klass_id,
                                 Register temp_reg, Register temp2_reg,
                                 const char* error_message) {
  InstanceKlass** klass_addr = vmClasses::klass_addr_at(klass_id);
  Klass* klass = vmClasses::klass_at(klass_id);
  Label L_ok, L_bad;
  BLOCK_COMMENT("verify_klass {");
  __ verify_oop(obj_reg, FILE_AND_LINE);
  __ cmpdi(CCR0, obj_reg, 0);
  __ beq(CCR0, L_bad);
  __ load_klass(temp_reg, obj_reg);
  __ load_const_optimized(temp2_reg, (address) klass_addr);
  __ ld(temp2_reg, 0, temp2_reg);
  __ cmpd(CCR0, temp_reg, temp2_reg);
  __ beq(CCR0, L_ok);
  __ ld(temp_reg, klass->super_check_offset(), temp_reg);
  __ cmpd(CCR0, temp_reg, temp2_reg);
  __ beq(CCR0, L_ok);
  __ BIND(L_bad);
  __ stop(error_message);
  __ BIND(L_ok);
  BLOCK_COMMENT("} verify_klass");
}

void MethodHandles::verify_ref_kind(MacroAssembler* _masm, int ref_kind, Register member_reg, Register temp) {
  Label L;
  BLOCK_COMMENT("verify_ref_kind {");
  __ load_sized_value(temp, NONZERO(java_lang_invoke_MemberName::flags_offset()), member_reg,
                      sizeof(u4), /*is_signed*/ false);
  // assert(sizeof(u4) == sizeof(java.lang.invoke.MemberName.flags), "");
  __ srwi( temp, temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_SHIFT);
  __ andi(temp, temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_MASK);
  __ cmpwi(CCR1, temp, ref_kind);
  __ beq(CCR1, L);
  { char* buf = NEW_C_HEAP_ARRAY(char, 100, mtInternal);
    jio_snprintf(buf, 100, "verify_ref_kind expected %x", ref_kind);
    if (ref_kind == JVM_REF_invokeVirtual ||
        ref_kind == JVM_REF_invokeSpecial)
      // could do this for all ref_kinds, but would explode assembly code size
      trace_method_handle(_masm, buf);
    __ stop(buf);
  }
  BLOCK_COMMENT("} verify_ref_kind");
  __ BIND(L);
}

#endif // ASSERT

void MethodHandles::jump_from_method_handle(MacroAssembler* _masm, Register method, Register target, Register temp,
                                            bool for_compiler_entry) {
  Label L_no_such_method;
  assert(method == R19_method, "interpreter calling convention");
  assert_different_registers(method, target, temp);

  if (!for_compiler_entry && JvmtiExport::can_post_interpreter_events()) {
    Label run_compiled_code;
    // JVMTI events, such as single-stepping, are implemented partly by avoiding running
    // compiled code in threads for which the event is enabled.  Check here for
    // interp_only_mode if these events CAN be enabled.
    __ verify_thread();
    __ lwz(temp, in_bytes(JavaThread::interp_only_mode_offset()), R16_thread);
    __ cmplwi(CCR0, temp, 0);
    __ beq(CCR0, run_compiled_code);
    // Null method test is replicated below in compiled case,
    // it might be able to address across the verify_thread()
    __ cmplwi(CCR0, R19_method, 0);
    __ beq(CCR0, L_no_such_method);
    __ ld(target, in_bytes(Method::interpreter_entry_offset()), R19_method);
    __ mtctr(target);
    __ bctr();
    __ BIND(run_compiled_code);
  }

  // Compiled case, either static or fall-through from runtime conditional
  __ cmplwi(CCR0, R19_method, 0);
  __ beq(CCR0, L_no_such_method);

  const ByteSize entry_offset = for_compiler_entry ? Method::from_compiled_offset() :
                                                     Method::from_interpreted_offset();
  __ ld(target, in_bytes(entry_offset), R19_method);
  __ mtctr(target);
  __ bctr();

  __ bind(L_no_such_method);
  assert(StubRoutines::throw_AbstractMethodError_entry() != NULL, "not yet generated!");
  __ load_const_optimized(target, StubRoutines::throw_AbstractMethodError_entry());
  __ mtctr(target);
  __ bctr();
}


void MethodHandles::jump_to_lambda_form(MacroAssembler* _masm,
                                        Register recv, Register method_temp,
                                        Register temp2, Register temp3,
                                        bool for_compiler_entry) {
  BLOCK_COMMENT("jump_to_lambda_form {");
  // This is the initial entry point of a lazy method handle.
  // After type checking, it picks up the invoker from the LambdaForm.
  assert_different_registers(recv, method_temp, temp2, temp3);
  assert(method_temp == R19_method, "required register for loading method");

  // Load the invoker, as MH -> MH.form -> LF.vmentry
  __ verify_oop(recv, FILE_AND_LINE);

  const MacroAssembler::PreservationLevel preservation_level = for_compiler_entry
    ? MacroAssembler::PRESERVATION_FRAME_LR_GP_FP_REGS
    : MacroAssembler::PRESERVATION_FRAME_LR;

  __ load_heap_oop(method_temp, NONZERO(java_lang_invoke_MethodHandle::form_offset()), recv,
                   temp2, temp3, preservation_level, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ load_heap_oop(method_temp, NONZERO(java_lang_invoke_LambdaForm::vmentry_offset()), method_temp,
                   temp2, temp3, preservation_level, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ load_heap_oop(method_temp, NONZERO(java_lang_invoke_MemberName::method_offset()), method_temp,
                   temp2, temp3, preservation_level, IS_NOT_NULL);
  __ verify_oop(method_temp, FILE_AND_LINE);
  __ ld(method_temp, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()), method_temp);

  if (VerifyMethodHandles && !for_compiler_entry) {
    // Make sure recv is already on stack.
    __ ld(temp2, in_bytes(Method::const_offset()), method_temp);
    __ load_sized_value(temp2, in_bytes(ConstMethod::size_of_parameters_offset()), temp2,
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(ConstMethod::_size_of_parameters), "");
    Label L;
    __ ld(temp2, __ argument_offset(temp2, temp2, 0), R15_esp);
    __ cmpd(CCR1, temp2, recv);
    __ beq(CCR1, L);
    __ stop("receiver not on stack");
    __ BIND(L);
  }

  jump_from_method_handle(_masm, method_temp, temp2, temp3, for_compiler_entry);
  BLOCK_COMMENT("} jump_to_lambda_form");
}



// Code generation
address MethodHandles::generate_method_handle_interpreter_entry(MacroAssembler* _masm,
                                                                vmIntrinsics::ID iid) {
  const bool not_for_compiler_entry = false;  // this is the interpreter entry
  assert(is_signature_polymorphic(iid), "expected invoke iid");
  if (iid == vmIntrinsics::_invokeGeneric ||
      iid == vmIntrinsics::_compiledLambdaForm) {
    // Perhaps surprisingly, the symbolic references visible to Java are not directly used.
    // They are linked to Java-generated adapters via MethodHandleNatives.linkMethod.
    // They all allow an appendix argument.
    __ stop("Should not reach here");           // empty stubs make SG sick
    return NULL;
  }

  // No need in interpreter entry for linkToNative for now.
  // Interpreter calls compiled entry through i2c.
  if (iid == vmIntrinsics::_linkToNative) {
    __ stop("Should not reach here");           // empty stubs make SG sick
    return NULL;
  }

  Register R15_argbase   = R15_esp; // parameter (preserved)
  Register R30_tmp1      = R30;
  Register R7_param_size = R7;

  // here's where control starts out:
  __ align(CodeEntryAlignment);
  address entry_point = __ pc();

  if (VerifyMethodHandles) {
    assert(Method::intrinsic_id_size_in_bytes() == 2, "assuming Method::_intrinsic_id is u2");

    Label L;
    BLOCK_COMMENT("verify_intrinsic_id {");
    __ load_sized_value(R30_tmp1, Method::intrinsic_id_offset_in_bytes(), R19_method,
                        sizeof(u2), /*is_signed*/ false);
    __ cmpwi(CCR1, R30_tmp1, (int) iid);
    __ beq(CCR1, L);
    if (iid == vmIntrinsics::_linkToVirtual ||
        iid == vmIntrinsics::_linkToSpecial) {
      // could do this for all kinds, but would explode assembly code size
      trace_method_handle(_masm, "bad Method*:intrinsic_id");
    }
    __ stop("bad Method*::intrinsic_id");
    __ BIND(L);
    BLOCK_COMMENT("} verify_intrinsic_id");
  }

  // First task:  Find out how big the argument list is.
  int ref_kind = signature_polymorphic_intrinsic_ref_kind(iid);
  assert(ref_kind != 0 || iid == vmIntrinsics::_invokeBasic, "must be _invokeBasic or a linkTo intrinsic");
  if (ref_kind == 0 || MethodHandles::ref_kind_has_receiver(ref_kind)) {
    __ ld(R7_param_size, in_bytes(Method::const_offset()), R19_method);
    __ load_sized_value(R7_param_size, in_bytes(ConstMethod::size_of_parameters_offset()), R7_param_size,
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(ConstMethod::_size_of_parameters), "");
  } else {
    DEBUG_ONLY(R7_param_size = noreg);
  }

  Register tmp_mh = noreg;
  if (!is_signature_polymorphic_static(iid)) {
    __ ld(tmp_mh = R30_tmp1, __ argument_offset(R7_param_size, R7_param_size, 0), R15_argbase);
    DEBUG_ONLY(R7_param_size = noreg);
  }

  if (log_is_enabled(Info, methodhandles)) {
    if (tmp_mh != noreg) {
      __ mr(R23_method_handle, tmp_mh);  // make stub happy
    }
    trace_method_handle_interpreter_entry(_masm, iid);
  }

  if (iid == vmIntrinsics::_invokeBasic) {
    generate_method_handle_dispatch(_masm, iid, tmp_mh, noreg, not_for_compiler_entry);

  } else {
    // Adjust argument list by popping the trailing MemberName argument.
    Register tmp_recv = noreg;
    if (MethodHandles::ref_kind_has_receiver(ref_kind)) {
      // Load the receiver (not the MH; the actual MemberName's receiver) up from the interpreter stack.
      __ ld(tmp_recv = R30_tmp1, __ argument_offset(R7_param_size, R7_param_size, 0), R15_argbase);
      DEBUG_ONLY(R7_param_size = noreg);
    }
    Register R19_member = R19_method;  // MemberName ptr; incoming method ptr is dead now
    __ ld(R19_member, RegisterOrConstant((intptr_t)8), R15_argbase);
    __ add(R15_argbase, Interpreter::stackElementSize, R15_argbase);
    generate_method_handle_dispatch(_masm, iid, tmp_recv, R19_member, not_for_compiler_entry);
  }

  return entry_point;
}

void MethodHandles::generate_method_handle_dispatch(MacroAssembler* _masm,
                                                    vmIntrinsics::ID iid,
                                                    Register receiver_reg,
                                                    Register member_reg,
                                                    bool for_compiler_entry) {
  assert(is_signature_polymorphic(iid), "expected invoke iid");
  Register temp1 = (for_compiler_entry ? R25_tmp5 : R31); // must be non-volatile due to runtime calls
  Register temp2 = (for_compiler_entry ? R22_tmp2 : R8);
  Register temp3 = (for_compiler_entry ? R23_tmp3 : R9);
  Register temp4 = (for_compiler_entry ? R24_tmp4 : R10);
  if (receiver_reg != noreg)  assert_different_registers(temp1, temp2, temp3, temp4, receiver_reg);
  if (member_reg   != noreg)  assert_different_registers(temp1, temp2, temp3, temp4, member_reg);

  const MacroAssembler::PreservationLevel preservation_level = for_compiler_entry
    ? MacroAssembler::PRESERVATION_FRAME_LR_GP_FP_REGS
    : MacroAssembler::PRESERVATION_FRAME_LR;

  if (iid == vmIntrinsics::_invokeBasic || iid == vmIntrinsics::_linkToNative) {
    if (iid == vmIntrinsics::_linkToNative) {
      assert(for_compiler_entry, "only compiler entry is supported");
    }
    // indirect through MH.form.vmentry.vmtarget
    jump_to_lambda_form(_masm, receiver_reg, R19_method, temp1, temp2, for_compiler_entry);
  } else {
    // The method is a member invoker used by direct method handles.
    if (VerifyMethodHandles) {
      // make sure the trailing argument really is a MemberName (caller responsibility)
      verify_klass(_masm, member_reg, VM_CLASS_ID(MemberName_klass),
                   temp1, temp2,
                   "MemberName required for invokeVirtual etc.");
    }

    Register temp1_recv_klass = temp1;
    if (iid != vmIntrinsics::_linkToStatic) {
      BLOCK_COMMENT("check_receiver {");
      __ verify_oop(receiver_reg, FILE_AND_LINE);

      const int klass_offset = iid == vmIntrinsics::_linkToSpecial
        ? -1                                  // enforce receiver null check
        : oopDesc::klass_offset_in_bytes();   // regular null-checking behavior

      __ null_check_throw(receiver_reg, klass_offset, temp1, Interpreter::throw_NullPointerException_entry());

      if (iid != vmIntrinsics::_linkToSpecial || VerifyMethodHandles) {
        __ load_klass(temp1_recv_klass, receiver_reg);
        __ verify_klass_ptr(temp1_recv_klass);
      }

      if (VerifyMethodHandles && iid != vmIntrinsics::_linkToInterface) {
        Label L_ok;
        Register temp2_defc = temp2;

        __ load_heap_oop(temp2_defc, NONZERO(java_lang_invoke_MemberName::clazz_offset()), member_reg,
                         temp3, temp4, preservation_level, IS_NOT_NULL);
        load_klass_from_Class(_masm, temp2_defc, temp3, temp4);
        __ verify_klass_ptr(temp2_defc);
        __ check_klass_subtype(temp1_recv_klass, temp2_defc, temp3, temp4, L_ok);
        // If we get here, the type check failed!
        __ stop("receiver class disagrees with MemberName.clazz");
        __ BIND(L_ok);
      }
      BLOCK_COMMENT("} check_receiver");
    }
    if (iid == vmIntrinsics::_linkToSpecial ||
        iid == vmIntrinsics::_linkToStatic) {
      DEBUG_ONLY(temp1_recv_klass = noreg);  // these guys didn't load the recv_klass
    }

    // Live registers at this point:
    //  member_reg - MemberName that was the trailing argument
    //  temp1_recv_klass - klass of stacked receiver, if needed

    Label L_incompatible_class_change_error;
    switch (iid) {
    case vmIntrinsics::_linkToSpecial:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeSpecial, member_reg, temp2);
      }
      __ load_heap_oop(R19_method, NONZERO(java_lang_invoke_MemberName::method_offset()), member_reg,
                       temp3, temp4, preservation_level, IS_NOT_NULL);
      __ ld(R19_method, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()), R19_method);
      break;

    case vmIntrinsics::_linkToStatic:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeStatic, member_reg, temp2);
      }
      __ load_heap_oop(R19_method, NONZERO(java_lang_invoke_MemberName::method_offset()), member_reg,
                       temp3, temp4, preservation_level, IS_NOT_NULL);
      __ ld(R19_method, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()), R19_method);
      break;

    case vmIntrinsics::_linkToVirtual:
    {
      // same as TemplateTable::invokevirtual,
      // minus the CP setup and profiling:

      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeVirtual, member_reg, temp2);
      }

      // pick out the vtable index from the MemberName, and then we can discard it:
      Register temp2_index = temp2;
      __ ld(temp2_index, NONZERO(java_lang_invoke_MemberName::vmindex_offset()), member_reg);

      if (VerifyMethodHandles) {
        Label L_index_ok;
        __ cmpdi(CCR1, temp2_index, 0);
        __ bge(CCR1, L_index_ok);
        __ stop("no virtual index");
        __ BIND(L_index_ok);
      }

      // Note:  The verifier invariants allow us to ignore MemberName.clazz and vmtarget
      // at this point.  And VerifyMethodHandles has already checked clazz, if needed.

      // get target Method* & entry point
      __ lookup_virtual_method(temp1_recv_klass, temp2_index, R19_method);
      break;
    }

    case vmIntrinsics::_linkToInterface:
    {
      // same as TemplateTable::invokeinterface
      // (minus the CP setup and profiling, with different argument motion)
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeInterface, member_reg, temp2);
      }

      Register temp2_intf = temp2;
      __ load_heap_oop(temp2_intf, NONZERO(java_lang_invoke_MemberName::clazz_offset()), member_reg,
                       temp3, temp4, preservation_level, IS_NOT_NULL);
      load_klass_from_Class(_masm, temp2_intf, temp3, temp4);
      __ verify_klass_ptr(temp2_intf);

      Register vtable_index = R19_method;
      __ ld(vtable_index, NONZERO(java_lang_invoke_MemberName::vmindex_offset()), member_reg);
      if (VerifyMethodHandles) {
        Label L_index_ok;
        __ cmpdi(CCR1, vtable_index, 0);
        __ bge(CCR1, L_index_ok);
        __ stop("invalid vtable index for MH.invokeInterface");
        __ BIND(L_index_ok);
      }

      // given intf, index, and recv klass, dispatch to the implementation method
      __ lookup_interface_method(temp1_recv_klass, temp2_intf,
                                 // note: next two args must be the same:
                                 vtable_index, R19_method,
                                 temp3, temp4,
                                 L_incompatible_class_change_error);
      break;
    }

    default:
      fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
      break;
    }

    // Live at this point:
    //   R19_method

    // After figuring out which concrete method to call, jump into it.
    // Note that this works in the interpreter with no data motion.
    // But the compiled version will require that rcx_recv be shifted out.
    __ verify_method_ptr(R19_method);
    jump_from_method_handle(_masm, R19_method, temp1, temp2, for_compiler_entry);

    if (iid == vmIntrinsics::_linkToInterface) {
      __ BIND(L_incompatible_class_change_error);
      __ load_const_optimized(temp1, StubRoutines::throw_IncompatibleClassChangeError_entry());
      __ mtctr(temp1);
      __ bctr();
    }
  }
}

#ifndef PRODUCT
void trace_method_handle_stub(const char* adaptername,
                              oopDesc* mh,
                              intptr_t* entry_sp,
                              intptr_t* saved_regs) {

  bool has_mh = (strstr(adaptername, "/static") == NULL &&
                 strstr(adaptername, "linkTo") == NULL);    // static linkers don't have MH
  const char* mh_reg_name = has_mh ? "R23_method_handle" : "G23";
  log_info(methodhandles)("MH %s %s=" INTPTR_FORMAT " sp=" INTPTR_FORMAT,
                adaptername, mh_reg_name, p2i(mh), p2i(entry_sp));

  LogTarget(Trace, methodhandles) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print_cr("Registers:");
    const int abi_offset = frame::abi_reg_args_size / 8;
    for (int i = R3->encoding(); i <= R12->encoding(); i++) {
      Register r = as_Register(i);
      int count = i - R3->encoding();
      // The registers are stored in reverse order on the stack (by save_volatile_gprs(R1_SP, abi_reg_args_size)).
      ls.print("%3s=" PTR_FORMAT, r->name(), saved_regs[abi_offset + count]);
      if ((count + 1) % 4 == 0) {
        ls.cr();
      } else {
        ls.print(", ");
      }
    }
    ls.cr();

    {
      // dumping last frame with frame::describe

      JavaThread* p = JavaThread::active();

      // may not be needed by safer and unexpensive here
      PreserveExceptionMark pem(Thread::current());
      FrameValues values;

      // Note: We want to allow trace_method_handle from any call site.
      // While trace_method_handle creates a frame, it may be entered
      // without a PC on the stack top (e.g. not just after a call).
      // Walking that frame could lead to failures due to that invalid PC.
      // => carefully detect that frame when doing the stack walking

      // Current C frame
      frame cur_frame = os::current_frame();

      // Robust search of trace_calling_frame (independant of inlining).
      assert(cur_frame.sp() <= saved_regs, "registers not saved on stack ?");
      frame trace_calling_frame = os::get_sender_for_C_frame(&cur_frame);
      while (trace_calling_frame.fp() < saved_regs) {
        trace_calling_frame = os::get_sender_for_C_frame(&trace_calling_frame);
      }

      // Safely create a frame and call frame::describe.
      intptr_t *dump_sp = trace_calling_frame.sender_sp();

      frame dump_frame = frame(dump_sp);
      dump_frame.describe(values, 1);

      values.describe(-1, saved_regs, "raw top of stack");

      ls.print_cr("Stack layout:");
      values.print_on(p, &ls);
    }

    if (has_mh && oopDesc::is_oop(mh)) {
      mh->print_on(&ls);
      if (java_lang_invoke_MethodHandle::is_instance(mh)) {
        java_lang_invoke_MethodHandle::form(mh)->print_on(&ls);
      }
    }
  }
}

void MethodHandles::trace_method_handle(MacroAssembler* _masm, const char* adaptername) {
  if (!log_is_enabled(Info, methodhandles)) return;

  BLOCK_COMMENT("trace_method_handle {");

  const Register tmp = R11; // Will be preserved.
  const int nbytes_save = MacroAssembler::num_volatile_regs * 8;
  __ save_volatile_gprs(R1_SP, -nbytes_save); // except R0
  __ save_LR_CR(tmp); // save in old frame

  __ mr(R5_ARG3, R1_SP);     // saved_sp
  __ push_frame_reg_args(nbytes_save, tmp);

  __ load_const_optimized(R3_ARG1, (address)adaptername, tmp);
  __ mr(R4_ARG2, R23_method_handle);
  __ mr(R6_ARG4, R1_SP);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, trace_method_handle_stub));

  __ pop_frame();
  __ restore_LR_CR(tmp);
  __ restore_volatile_gprs(R1_SP, -nbytes_save); // except R0

  BLOCK_COMMENT("} trace_method_handle");
}
#endif // PRODUCT
