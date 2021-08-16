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

// This file mirror as much as possible methodHandles_x86.cpp to ease
// cross platform development for JSR292.
// Last synchronization: changeset f8c9417e3571

#include "precompiled.hpp"
#include "jvm.h"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
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
#define BLOCK_COMMENT(str) /* nothing */
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

void MethodHandles::load_klass_from_Class(MacroAssembler* _masm, Register klass_reg, Register temp1, Register temp2) {
  if (VerifyMethodHandles) {
    verify_klass(_masm, klass_reg, temp1, temp2, VM_CLASS_ID(java_lang_Class),
                 "MH argument is a Class");
  }
  __ ldr(klass_reg, Address(klass_reg, java_lang_Class::klass_offset()));
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
                                 Register obj, Register temp1, Register temp2, vmClassID klass_id,
                                 const char* error_message) {
  InstanceKlass** klass_addr = vmClasses::klass_addr_at(klass_id);
  Klass* klass = vmClasses::klass_at(klass_id);
  Label L_ok, L_bad;
  BLOCK_COMMENT("verify_klass {");
  __ verify_oop(obj);
  __ cbz(obj, L_bad);
  __ load_klass(temp1, obj);
  __ lea(temp2, ExternalAddress((address) klass_addr));
  __ ldr(temp2, temp2); // the cmpptr on x86 dereferences the AddressLiteral (not lea)
  __ cmp(temp1, temp2);
  __ b(L_ok, eq);
  intptr_t super_check_offset = klass->super_check_offset();
  __ ldr(temp1, Address(temp1, super_check_offset));
  __ cmp(temp1, temp2);
  __ b(L_ok, eq);

  __ bind(L_bad);
  __ stop(error_message);
  __ BIND(L_ok);
  BLOCK_COMMENT("} verify_klass");
}

void MethodHandles::verify_ref_kind(MacroAssembler* _masm, int ref_kind, Register member_reg, Register temp) {
  Label L;
  BLOCK_COMMENT("verify_ref_kind {");
  __ ldr_u32(temp, Address(member_reg, NONZERO(java_lang_invoke_MemberName::flags_offset())));
  __ logical_shift_right(temp, temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_SHIFT);
  __ andr(temp, temp, (unsigned)java_lang_invoke_MemberName::MN_REFERENCE_KIND_MASK);
  __ cmp(temp, ref_kind);
  __ b(L, eq);
  { char* buf = NEW_C_HEAP_ARRAY(char, 100, mtInternal);
  jio_snprintf(buf, 100, "verify_ref_kind expected %x", ref_kind);
  if (ref_kind == JVM_REF_invokeVirtual ||
      ref_kind == JVM_REF_invokeSpecial)
    // could do this for all ref_kinds, but would explode assembly code size
    trace_method_handle(_masm, buf);
  __ stop(buf);
  }
  BLOCK_COMMENT("} verify_ref_kind");
  __ bind(L);
}

#endif //ASSERT

void MethodHandles::jump_from_method_handle(MacroAssembler* _masm, bool for_compiler_entry) {
  Label L_no_such_method;
  __ cbz(Rmethod, L_no_such_method);

  // Note: JVMTI overhead seems small enough compared to invocation
  // cost and is not worth the complexity or code size overhead of
  // supporting several variants of each adapter.
  if (!for_compiler_entry && (JvmtiExport::can_post_interpreter_events())) {
    // JVMTI events, such as single-stepping, are implemented partly by avoiding running
    // compiled code in threads for which the event is enabled.  Check here for
    // interp_only_mode if these events CAN be enabled.
    __ ldr_s32(Rtemp, Address(Rthread, JavaThread::interp_only_mode_offset()));
    __ cmp(Rtemp, 0);
    __ ldr(PC, Address(Rmethod, Method::interpreter_entry_offset()), ne);
  }
  const ByteSize entry_offset = for_compiler_entry ? Method::from_compiled_offset() :
                                                     Method::from_interpreted_offset();

  __ indirect_jump(Address(Rmethod, entry_offset), Rtemp);

  __ bind(L_no_such_method);
  // throw exception
  __ jump(StubRoutines::throw_AbstractMethodError_entry(), relocInfo::runtime_call_type, Rtemp);
}

void MethodHandles::jump_to_lambda_form(MacroAssembler* _masm,
                                        Register recv, Register tmp,
                                        bool for_compiler_entry) {
  BLOCK_COMMENT("jump_to_lambda_form {");
  // This is the initial entry point of a lazy method handle.
  // After type checking, it picks up the invoker from the LambdaForm.
  assert_different_registers(recv, tmp, Rmethod);

  // Load the invoker, as MH -> MH.form -> LF.vmentry
  __ load_heap_oop(tmp, Address(recv, NONZERO(java_lang_invoke_MethodHandle::form_offset())));
  __ verify_oop(tmp);

  __ load_heap_oop(tmp, Address(tmp, NONZERO(java_lang_invoke_LambdaForm::vmentry_offset())));
  __ verify_oop(tmp);

  __ load_heap_oop(Rmethod, Address(tmp, NONZERO(java_lang_invoke_MemberName::method_offset())));
  __ verify_oop(Rmethod);
  __ access_load_at(T_ADDRESS, IN_HEAP, Address(Rmethod, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset())), Rmethod, noreg, noreg, noreg);

  if (VerifyMethodHandles && !for_compiler_entry) {
    // make sure recv is already on stack
    __ ldr(tmp, Address(Rmethod, Method::const_offset()));
    __ load_sized_value(tmp,
                        Address(tmp, ConstMethod::size_of_parameters_offset()),
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(Method::_size_of_parameters), "");
    Label L;
    __ ldr(tmp, __ receiver_argument_address(Rparams, tmp, tmp));
    __ cmpoop(tmp, recv);
    __ b(L, eq);
    __ stop("receiver not on stack");
    __ bind(L);
  }

  jump_from_method_handle(_masm, for_compiler_entry);
  BLOCK_COMMENT("} jump_to_lambda_form");
}


// Code generation
address MethodHandles::generate_method_handle_interpreter_entry(MacroAssembler* _masm,
                                                                vmIntrinsics::ID iid) {
  const bool not_for_compiler_entry = false;  // this is the interpreter entry
  assert(is_signature_polymorphic(iid), "expected invoke iid");
  if (iid == vmIntrinsics::_invokeGeneric ||
      iid == vmIntrinsics::_compiledLambdaForm ||
      iid == vmIntrinsics::_linkToNative) {
    // Perhaps surprisingly, the user-visible names, and linkToCallSite, are not directly used.
    // They are linked to Java-generated adapters via MethodHandleNatives.linkMethod.
    // They all require an extra argument.
    __ should_not_reach_here();           // empty stubs make SG sick
    return NULL;
  }

  // Rmethod: Method*
  // Rparams (SP on 32-bit ARM): pointer to parameters
  // Rsender_sp (R4/R19): sender SP (must preserve; see prepare_to_jump_from_interpreted)
  // R5_mh: receiver method handle (must load from sp[MethodTypeForm.vmslots])
  // R1, R2, Rtemp: garbage temp, blown away

  // Use same name as x86 to ease future merges
  Register rdx_temp       = R2_tmp;
  Register rdx_param_size = rdx_temp;  // size of parameters
  Register rax_temp       = R1_tmp;
  Register rcx_mh         = R5_mh;     // MH receiver; dies quickly and is recycled
  Register rbx_method     = Rmethod;   // eventual target of this invocation
  Register rdi_temp       = Rtemp;

  // here's where control starts out:
  __ align(CodeEntryAlignment);
  address entry_point = __ pc();

  if (VerifyMethodHandles) {
    Label L;
    BLOCK_COMMENT("verify_intrinsic_id {");
    __ ldrh(rdi_temp, Address(rbx_method, Method::intrinsic_id_offset_in_bytes()));
    __ sub_slow(rdi_temp, rdi_temp, (int) iid);
    __ cbz(rdi_temp, L);
    if (iid == vmIntrinsics::_linkToVirtual ||
        iid == vmIntrinsics::_linkToSpecial) {
      // could do this for all kinds, but would explode assembly code size
      trace_method_handle(_masm, "bad Method*::intrinsic_id");
    }
    __ stop("bad Method*::intrinsic_id");
    __ bind(L);
    BLOCK_COMMENT("} verify_intrinsic_id");
  }

  // First task:  Find out how big the argument list is.
  Address rdx_first_arg_addr;
  int ref_kind = signature_polymorphic_intrinsic_ref_kind(iid);
  assert(ref_kind != 0 || iid == vmIntrinsics::_invokeBasic, "must be _invokeBasic or a linkTo intrinsic");
  if (ref_kind == 0 || MethodHandles::ref_kind_has_receiver(ref_kind)) {
    __ ldr(rdx_param_size, Address(rbx_method, Method::const_offset()));
    __ load_sized_value(rdx_param_size,
                        Address(rdx_param_size, ConstMethod::size_of_parameters_offset()),
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(Method::_size_of_parameters), "");
    rdx_first_arg_addr = __ receiver_argument_address(Rparams, rdx_param_size, rdi_temp);
  } else {
    DEBUG_ONLY(rdx_param_size = noreg);
  }

  if (!is_signature_polymorphic_static(iid)) {
    __ ldr(rcx_mh, rdx_first_arg_addr);
    DEBUG_ONLY(rdx_param_size = noreg);
  }

  // rdx_first_arg_addr is live!

  trace_method_handle_interpreter_entry(_masm, iid);

  if (iid == vmIntrinsics::_invokeBasic) {
    generate_method_handle_dispatch(_masm, iid, rcx_mh, noreg, not_for_compiler_entry);

  } else {
    // Adjust argument list by popping the trailing MemberName argument.
    Register rcx_recv = noreg;
    if (MethodHandles::ref_kind_has_receiver(ref_kind)) {
      // Load the receiver (not the MH; the actual MemberName's receiver) up from the interpreter stack.
      __ ldr(rcx_recv = rcx_mh, rdx_first_arg_addr);
      DEBUG_ONLY(rdx_param_size = noreg);
    }
    Register rbx_member = rbx_method;  // MemberName ptr; incoming method ptr is dead now
    __ pop(rbx_member);
    generate_method_handle_dispatch(_masm, iid, rcx_recv, rbx_member, not_for_compiler_entry);
  }
  return entry_point;
}

void MethodHandles::generate_method_handle_dispatch(MacroAssembler* _masm,
                                                    vmIntrinsics::ID iid,
                                                    Register receiver_reg,
                                                    Register member_reg,
                                                    bool for_compiler_entry) {
  assert(is_signature_polymorphic(iid), "expected invoke iid");
  // Use same name as x86 to ease future merges
  Register rbx_method = Rmethod;   // eventual target of this invocation
  // temps used in this code are not used in *either* compiled or interpreted calling sequences
  Register temp1 = (for_compiler_entry ? saved_last_sp_register() : R1_tmp);
  Register temp2 = R8;
  Register temp3 = Rtemp; // R12/R16
  Register temp4 = R5;
  if (for_compiler_entry) {
    assert(receiver_reg == (iid == vmIntrinsics::_linkToStatic ? noreg : j_rarg0), "only valid assignment");
    assert_different_registers(temp1, j_rarg0, j_rarg1, j_rarg2, j_rarg3);
    assert_different_registers(temp2, j_rarg0, j_rarg1, j_rarg2, j_rarg3);
    assert_different_registers(temp3, j_rarg0, j_rarg1, j_rarg2, j_rarg3);
    assert_different_registers(temp4, j_rarg0, j_rarg1, j_rarg2, j_rarg3);
  }
  assert_different_registers(temp1, temp2, temp3, receiver_reg);
  assert_different_registers(temp1, temp2, temp3, temp4, member_reg);
  if (!for_compiler_entry)
    assert_different_registers(temp1, temp2, temp3, temp4, saved_last_sp_register());  // don't trash lastSP

  if (iid == vmIntrinsics::_invokeBasic) {
    // indirect through MH.form.exactInvoker.vmtarget
    jump_to_lambda_form(_masm, receiver_reg, temp3, for_compiler_entry);

  } else {
    // The method is a member invoker used by direct method handles.
    if (VerifyMethodHandles) {
      // make sure the trailing argument really is a MemberName (caller responsibility)
      verify_klass(_masm, member_reg, temp2, temp3, VM_CLASS_ID(java_lang_invoke_MemberName),
                   "MemberName required for invokeVirtual etc.");
    }

    Address member_clazz(   member_reg, NONZERO(java_lang_invoke_MemberName::clazz_offset()));
    Address member_vmindex( member_reg, NONZERO(java_lang_invoke_MemberName::vmindex_offset()));
    Address member_vmtarget(member_reg, NONZERO(java_lang_invoke_MemberName::method_offset()));
    Address vmtarget_method(Rmethod, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()));

    Register temp1_recv_klass = temp1;
    if (iid != vmIntrinsics::_linkToStatic) {
      if (iid == vmIntrinsics::_linkToSpecial) {
        // Don't actually load the klass; just null-check the receiver.
        __ null_check(receiver_reg, temp3);
      } else {
        // load receiver klass itself
        __ null_check(receiver_reg, temp3, oopDesc::klass_offset_in_bytes());
        __ load_klass(temp1_recv_klass, receiver_reg);
        __ verify_klass_ptr(temp1_recv_klass);
      }
      BLOCK_COMMENT("check_receiver {");
      // The receiver for the MemberName must be in receiver_reg.
      // Check the receiver against the MemberName.clazz
      if (VerifyMethodHandles && iid == vmIntrinsics::_linkToSpecial) {
        // Did not load it above...
        __ load_klass(temp1_recv_klass, receiver_reg);
        __ verify_klass_ptr(temp1_recv_klass);
      }
      // Check the receiver against the MemberName.clazz
      if (VerifyMethodHandles && iid != vmIntrinsics::_linkToInterface) {
        Label L_ok;
        Register temp2_defc = temp2;
        __ load_heap_oop(temp2_defc, member_clazz);
        load_klass_from_Class(_masm, temp2_defc, temp3, temp4);
        __ verify_klass_ptr(temp2_defc);
        __ check_klass_subtype(temp1_recv_klass, temp2_defc, temp3, temp4, noreg, L_ok);
        // If we get here, the type check failed!
        __ stop("receiver class disagrees with MemberName.clazz");
        __ bind(L_ok);
      }
      BLOCK_COMMENT("} check_receiver");
    }
    if (iid == vmIntrinsics::_linkToSpecial ||
        iid == vmIntrinsics::_linkToStatic) {
      DEBUG_ONLY(temp1_recv_klass = noreg);  // these guys didn't load the recv_klass
    }

    // Live registers at this point:
    //  member_reg - MemberName that was the extra argument
    //  temp1_recv_klass - klass of stacked receiver, if needed

    Label L_incompatible_class_change_error;
    switch (iid) {
    case vmIntrinsics::_linkToSpecial:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeSpecial, member_reg, temp3);
      }
      __ load_heap_oop(Rmethod, member_vmtarget);
      __ access_load_at(T_ADDRESS, IN_HEAP, vmtarget_method, Rmethod, noreg, noreg, noreg);
      break;

    case vmIntrinsics::_linkToStatic:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeStatic, member_reg, temp3);
      }
      __ load_heap_oop(Rmethod, member_vmtarget);
      __ access_load_at(T_ADDRESS, IN_HEAP, vmtarget_method, Rmethod, noreg, noreg, noreg);
      break;
      break;

    case vmIntrinsics::_linkToVirtual:
    {
      // same as TemplateTable::invokevirtual,
      // minus the CP setup and profiling:

      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeVirtual, member_reg, temp3);
      }

      // pick out the vtable index from the MemberName, and then we can discard it:
      Register temp2_index = temp2;
      __ access_load_at(T_ADDRESS, IN_HEAP, member_vmindex, temp2_index, noreg, noreg, noreg);

      if (VerifyMethodHandles) {
        Label L_index_ok;
        __ cmp(temp2_index, 0);
        __ b(L_index_ok, ge);
        __ stop("no virtual index");
        __ bind(L_index_ok);
      }

      // Note:  The verifier invariants allow us to ignore MemberName.clazz and vmtarget
      // at this point.  And VerifyMethodHandles has already checked clazz, if needed.

      // get target Method* & entry point
      __ lookup_virtual_method(temp1_recv_klass, temp2_index, Rmethod);
      break;
    }

    case vmIntrinsics::_linkToInterface:
    {
      // same as TemplateTable::invokeinterface
      // (minus the CP setup and profiling, with different argument motion)
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeInterface, member_reg, temp3);
      }

      Register temp3_intf = temp3;
      __ load_heap_oop(temp3_intf, member_clazz);
      load_klass_from_Class(_masm, temp3_intf, temp2, temp4);
      __ verify_klass_ptr(temp3_intf);

      Register rbx_index = rbx_method;
      __ access_load_at(T_ADDRESS, IN_HEAP, member_vmindex, rbx_index, noreg, noreg, noreg);
      if (VerifyMethodHandles) {
        Label L;
        __ cmp(rbx_index, 0);
        __ b(L, ge);
        __ stop("invalid vtable index for MH.invokeInterface");
        __ bind(L);
      }

      // given intf, index, and recv klass, dispatch to the implementation method
      __ lookup_interface_method(temp1_recv_klass, temp3_intf,
                                 // note: next two args must be the same:
                                 rbx_index, rbx_method,
                                 temp2, temp4,
                                 L_incompatible_class_change_error);
      break;
    }

    default:
      fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
      break;
    }

    // Live at this point:
    //   Rmethod (target method)
    //   Rsender_sp, Rparams (if interpreted)
    //   register arguments (if compiled)

    // After figuring out which concrete method to call, jump into it.
    __ verify_method_ptr(Rmethod);
    jump_from_method_handle(_masm, for_compiler_entry);

    if (iid == vmIntrinsics::_linkToInterface) {
      __ bind(L_incompatible_class_change_error);
      __ jump(StubRoutines::throw_IncompatibleClassChangeError_entry(), relocInfo::runtime_call_type, Rtemp);
    }
  }
}


#ifndef PRODUCT
enum {
  ARG_LIMIT = 255, SLOP = 4,
  // use this parameter for checking for garbage stack movements:
  UNREASONABLE_STACK_MOVE = (ARG_LIMIT + SLOP)
  // the slop defends against false alarms due to fencepost errors
};

const int trace_mh_nregs = 15;
const Register trace_mh_regs[trace_mh_nregs] =
  {R0, R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, LR, PC};

void trace_method_handle_stub(const char* adaptername,
                              intptr_t* saved_regs,
                              intptr_t* saved_bp,
                              oop mh) {
  // called as a leaf from native code: do not block the JVM!
  bool has_mh = (strstr(adaptername, "/static") == NULL &&
                 strstr(adaptername, "linkTo") == NULL);    // static linkers don't have MH
  intptr_t* entry_sp = (intptr_t*) &saved_regs[trace_mh_nregs]; // just after the saved regs
  intptr_t* saved_sp = (intptr_t*)  saved_regs[Rsender_sp->encoding()]; // save of Rsender_sp
  intptr_t* last_sp  = (intptr_t*)  saved_bp[frame::interpreter_frame_last_sp_offset];
  intptr_t* base_sp  = last_sp;

  intptr_t    mh_reg = (intptr_t)saved_regs[R5_mh->encoding()];
  const char* mh_reg_name = "R5_mh";
  if (!has_mh) {
    mh_reg_name = "R5";
  }
  log_info(methodhandles)("MH %s %s=" PTR_FORMAT " sp=(" PTR_FORMAT "+" INTX_FORMAT ") stack_size=" INTX_FORMAT " bp=" PTR_FORMAT,
                          adaptername, mh_reg_name, mh_reg,
                          (intptr_t)entry_sp, (intptr_t)saved_sp - (intptr_t)entry_sp, (intptr_t)(base_sp - last_sp), (intptr_t)saved_bp);

  if (last_sp != saved_sp && last_sp != NULL) {
    log_info(methodhandles)("*** last_sp=" INTPTR_FORMAT, p2i(last_sp));
  }
  LogTarget(Trace, methodhandles) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print(" reg dump: ");
    int i;
    for (i = 0; i < trace_mh_nregs; i++) {
      if (i > 0 && i % 4 == 0)
        ls.print("\n   + dump: ");
      const char* reg_name = trace_mh_regs[i]->name();
      ls.print(" %s: " INTPTR_FORMAT, reg_name, p2i((void*)saved_regs[i]));
    }
    ls.cr();

    {
      // dump last frame (from JavaThread::print_frame_layout)

      // Note: code is robust but the dumped information may not be
      // 100% correct, particularly with respect to the dumped
      // "unextended_sp". Getting it right for all trace_method_handle
      // call paths is not worth the complexity/risk. The correct slot
      // will be identified by *Rsender_sp anyway in the dump.
      JavaThread* p = JavaThread::active();

      // may not be needed by safer and unexpensive here
      PreserveExceptionMark pem(Thread::current());
      FrameValues values;

      intptr_t* dump_fp = (intptr_t *) saved_bp;
      address dump_pc = (address) saved_regs[trace_mh_nregs-2]; // LR (with LR,PC last in saved_regs)
      frame dump_frame((intptr_t *)entry_sp, dump_fp, dump_pc);

      dump_frame.describe(values, 1);
      // mark Rsender_sp if seems valid
      if (has_mh) {
        if ((saved_sp >= entry_sp - UNREASONABLE_STACK_MOVE) && (saved_sp < dump_fp)) {
          values.describe(-1, saved_sp, "*Rsender_sp");
        }
      }

      // Note: the unextended_sp may not be correct
      ls.print_cr("  stack layout:");
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
  if (!log_is_enabled(Info, methodhandles))  return;
  BLOCK_COMMENT("trace_method_handle {");
  // register saving
  //  must correspond to trace_mh_nregs and trace_mh_regs defined above
  int push_size = __ save_all_registers();
  assert(trace_mh_nregs*wordSize == push_size,"saved register count mismatch");

  __ mov_slow(R0, adaptername);
  __ mov(R1, SP); // entry_sp (after pushes)
  __ mov(R2, FP);
  if (R5_mh != R3) {
    assert_different_registers(R0, R1, R2, R5_mh);
    __ mov(R3, R5_mh);
  }

  __ call_VM_leaf(CAST_FROM_FN_PTR(address, trace_method_handle_stub), R0, R1, R2, R3);

  __ restore_all_registers();
  BLOCK_COMMENT("} trace_method_handle");
}
#endif //PRODUCT
