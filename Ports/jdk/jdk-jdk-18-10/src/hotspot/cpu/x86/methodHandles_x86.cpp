/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "classfile/vmClasses.hpp"
#include "compiler/disassembler.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/preserveException.hpp"

#define __ Disassembler::hook<MacroAssembler>(__FILE__, __LINE__, _masm)->

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#define STOP(error) stop(error)
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#define STOP(error) block_comment(error); __ stop(error)
#endif

#define BIND(label) bind(label); BLOCK_COMMENT(#label ":")

void MethodHandles::load_klass_from_Class(MacroAssembler* _masm, Register klass_reg) {
  if (VerifyMethodHandles)
    verify_klass(_masm, klass_reg, VM_CLASS_ID(java_lang_Class),
                 "MH argument is a Class");
  __ movptr(klass_reg, Address(klass_reg, java_lang_Class::klass_offset()));
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
                                 Register obj, vmClassID klass_id,
                                 const char* error_message) {
  InstanceKlass** klass_addr = vmClasses::klass_addr_at(klass_id);
  Klass* klass = vmClasses::klass_at(klass_id);
  Register temp = rdi;
  Register temp2 = noreg;
  LP64_ONLY(temp2 = rscratch1);  // used by MacroAssembler::cmpptr and load_klass
  Label L_ok, L_bad;
  BLOCK_COMMENT("verify_klass {");
  __ verify_oop(obj);
  __ testptr(obj, obj);
  __ jcc(Assembler::zero, L_bad);
  __ push(temp); if (temp2 != noreg)  __ push(temp2);
#define UNPUSH { if (temp2 != noreg)  __ pop(temp2);  __ pop(temp); }
  __ load_klass(temp, obj, temp2);
  __ cmpptr(temp, ExternalAddress((address) klass_addr));
  __ jcc(Assembler::equal, L_ok);
  intptr_t super_check_offset = klass->super_check_offset();
  __ movptr(temp, Address(temp, super_check_offset));
  __ cmpptr(temp, ExternalAddress((address) klass_addr));
  __ jcc(Assembler::equal, L_ok);
  UNPUSH;
  __ bind(L_bad);
  __ STOP(error_message);
  __ BIND(L_ok);
  UNPUSH;
  BLOCK_COMMENT("} verify_klass");
}

void MethodHandles::verify_ref_kind(MacroAssembler* _masm, int ref_kind, Register member_reg, Register temp) {
  Label L;
  BLOCK_COMMENT("verify_ref_kind {");
  __ movl(temp, Address(member_reg, NONZERO(java_lang_invoke_MemberName::flags_offset())));
  __ shrl(temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_SHIFT);
  __ andl(temp, java_lang_invoke_MemberName::MN_REFERENCE_KIND_MASK);
  __ cmpl(temp, ref_kind);
  __ jcc(Assembler::equal, L);
  { char* buf = NEW_C_HEAP_ARRAY(char, 100, mtInternal);
    jio_snprintf(buf, 100, "verify_ref_kind expected %x", ref_kind);
    if (ref_kind == JVM_REF_invokeVirtual ||
        ref_kind == JVM_REF_invokeSpecial)
      // could do this for all ref_kinds, but would explode assembly code size
      trace_method_handle(_masm, buf);
    __ STOP(buf);
  }
  BLOCK_COMMENT("} verify_ref_kind");
  __ bind(L);
}

#endif //ASSERT

void MethodHandles::jump_from_method_handle(MacroAssembler* _masm, Register method, Register temp,
                                            bool for_compiler_entry) {
  assert(method == rbx, "interpreter calling convention");

   Label L_no_such_method;
   __ testptr(rbx, rbx);
   __ jcc(Assembler::zero, L_no_such_method);

  __ verify_method_ptr(method);

  if (!for_compiler_entry && JvmtiExport::can_post_interpreter_events()) {
    Label run_compiled_code;
    // JVMTI events, such as single-stepping, are implemented partly by avoiding running
    // compiled code in threads for which the event is enabled.  Check here for
    // interp_only_mode if these events CAN be enabled.
#ifdef _LP64
    Register rthread = r15_thread;
#else
    Register rthread = temp;
    __ get_thread(rthread);
#endif
    // interp_only is an int, on little endian it is sufficient to test the byte only
    // Is a cmpl faster?
    __ cmpb(Address(rthread, JavaThread::interp_only_mode_offset()), 0);
    __ jccb(Assembler::zero, run_compiled_code);
    __ jmp(Address(method, Method::interpreter_entry_offset()));
    __ BIND(run_compiled_code);
  }

  const ByteSize entry_offset = for_compiler_entry ? Method::from_compiled_offset() :
                                                     Method::from_interpreted_offset();
  __ jmp(Address(method, entry_offset));

  __ bind(L_no_such_method);
  __ jump(RuntimeAddress(StubRoutines::throw_AbstractMethodError_entry()));
}

void MethodHandles::jump_to_lambda_form(MacroAssembler* _masm,
                                        Register recv, Register method_temp,
                                        Register temp2,
                                        bool for_compiler_entry) {
  BLOCK_COMMENT("jump_to_lambda_form {");
  // This is the initial entry point of a lazy method handle.
  // After type checking, it picks up the invoker from the LambdaForm.
  assert_different_registers(recv, method_temp, temp2);
  assert(recv != noreg, "required register");
  assert(method_temp == rbx, "required register for loading method");

  // Load the invoker, as MH -> MH.form -> LF.vmentry
  __ verify_oop(recv);
  __ load_heap_oop(method_temp, Address(recv, NONZERO(java_lang_invoke_MethodHandle::form_offset())), temp2);
  __ verify_oop(method_temp);
  __ load_heap_oop(method_temp, Address(method_temp, NONZERO(java_lang_invoke_LambdaForm::vmentry_offset())), temp2);
  __ verify_oop(method_temp);
  __ load_heap_oop(method_temp, Address(method_temp, NONZERO(java_lang_invoke_MemberName::method_offset())), temp2);
  __ verify_oop(method_temp);
  __ access_load_at(T_ADDRESS, IN_HEAP, method_temp,
                    Address(method_temp, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset())),
                    noreg, noreg);

  if (VerifyMethodHandles && !for_compiler_entry) {
    // make sure recv is already on stack
    __ movptr(temp2, Address(method_temp, Method::const_offset()));
    __ load_sized_value(temp2,
                        Address(temp2, ConstMethod::size_of_parameters_offset()),
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(Method::_size_of_parameters), "");
    Label L;
    __ cmpoop(recv, __ argument_address(temp2, -1));
    __ jcc(Assembler::equal, L);
    __ movptr(rax, __ argument_address(temp2, -1));
    __ STOP("receiver not on stack");
    __ BIND(L);
  }

  jump_from_method_handle(_masm, method_temp, temp2, for_compiler_entry);
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
    __ hlt();           // empty stubs make SG sick
    return NULL;
  }

  // No need in interpreter entry for linkToNative for now.
  // Interpreter calls compiled entry through i2c.
  if (iid == vmIntrinsics::_linkToNative) {
    __ hlt();
    return NULL;
  }

  // rsi/r13: sender SP (must preserve; see prepare_to_jump_from_interpreted)
  // rbx: Method*
  // rdx: argument locator (parameter slot count, added to rsp)
  // rcx: used as temp to hold mh or receiver
  // rax, rdi: garbage temps, blown away
  Register rdx_argp   = rdx;   // argument list ptr, live on error paths
  Register rax_temp   = rax;
  Register rcx_mh     = rcx;   // MH receiver; dies quickly and is recycled
  Register rbx_method = rbx;   // eventual target of this invocation

  // here's where control starts out:
  __ align(CodeEntryAlignment);
  address entry_point = __ pc();

  if (VerifyMethodHandles) {
    assert(Method::intrinsic_id_size_in_bytes() == 2, "assuming Method::_intrinsic_id is u2");

    Label L;
    BLOCK_COMMENT("verify_intrinsic_id {");
    __ cmpw(Address(rbx_method, Method::intrinsic_id_offset_in_bytes()), (int) iid);
    __ jcc(Assembler::equal, L);
    if (iid == vmIntrinsics::_linkToVirtual ||
        iid == vmIntrinsics::_linkToSpecial) {
      // could do this for all kinds, but would explode assembly code size
      trace_method_handle(_masm, "bad Method*::intrinsic_id");
    }
    __ STOP("bad Method*::intrinsic_id");
    __ bind(L);
    BLOCK_COMMENT("} verify_intrinsic_id");
  }

  // First task:  Find out how big the argument list is.
  Address rdx_first_arg_addr;
  int ref_kind = signature_polymorphic_intrinsic_ref_kind(iid);
  assert(ref_kind != 0 || iid == vmIntrinsics::_invokeBasic, "must be _invokeBasic or a linkTo intrinsic");
  if (ref_kind == 0 || MethodHandles::ref_kind_has_receiver(ref_kind)) {
    __ movptr(rdx_argp, Address(rbx_method, Method::const_offset()));
    __ load_sized_value(rdx_argp,
                        Address(rdx_argp, ConstMethod::size_of_parameters_offset()),
                        sizeof(u2), /*is_signed*/ false);
    // assert(sizeof(u2) == sizeof(Method::_size_of_parameters), "");
    rdx_first_arg_addr = __ argument_address(rdx_argp, -1);
  } else {
    DEBUG_ONLY(rdx_argp = noreg);
  }

  if (!is_signature_polymorphic_static(iid)) {
    __ movptr(rcx_mh, rdx_first_arg_addr);
    DEBUG_ONLY(rdx_argp = noreg);
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
      __ movptr(rcx_recv = rcx, rdx_first_arg_addr);
    }
    DEBUG_ONLY(rdx_argp = noreg);
    Register rbx_member = rbx_method;  // MemberName ptr; incoming method ptr is dead now
    __ pop(rax_temp);           // return address
    __ pop(rbx_member);         // extract last argument
    __ push(rax_temp);          // re-push return address
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
  Register rbx_method = rbx;   // eventual target of this invocation
  // temps used in this code are not used in *either* compiled or interpreted calling sequences
#ifdef _LP64
  Register temp1 = rscratch1;
  Register temp2 = rscratch2;
  Register temp3 = rax;
  if (for_compiler_entry) {
    assert(receiver_reg == (iid == vmIntrinsics::_linkToStatic ? noreg : j_rarg0), "only valid assignment");
    assert_different_registers(temp1,        j_rarg0, j_rarg1, j_rarg2, j_rarg3, j_rarg4, j_rarg5);
    assert_different_registers(temp2,        j_rarg0, j_rarg1, j_rarg2, j_rarg3, j_rarg4, j_rarg5);
    assert_different_registers(temp3,        j_rarg0, j_rarg1, j_rarg2, j_rarg3, j_rarg4, j_rarg5);
  }
#else
  Register temp1 = (for_compiler_entry ? rsi : rdx);
  Register temp2 = rdi;
  Register temp3 = rax;
  if (for_compiler_entry) {
    assert(receiver_reg == (iid == vmIntrinsics::_linkToStatic ? noreg : rcx), "only valid assignment");
    assert_different_registers(temp1,        rcx, rdx);
    assert_different_registers(temp2,        rcx, rdx);
    assert_different_registers(temp3,        rcx, rdx);
  }
#endif
  else {
    assert_different_registers(temp1, temp2, temp3, saved_last_sp_register());  // don't trash lastSP
  }
  assert_different_registers(temp1, temp2, temp3, receiver_reg);
  assert_different_registers(temp1, temp2, temp3, member_reg);

  if (iid == vmIntrinsics::_invokeBasic || iid == vmIntrinsics::_linkToNative) {
    if (iid == vmIntrinsics::_linkToNative) {
      assert(for_compiler_entry, "only compiler entry is supported");
    }
    // indirect through MH.form.vmentry.vmtarget
    jump_to_lambda_form(_masm, receiver_reg, rbx_method, temp1, for_compiler_entry);

  } else {
    // The method is a member invoker used by direct method handles.
    if (VerifyMethodHandles) {
      // make sure the trailing argument really is a MemberName (caller responsibility)
      verify_klass(_masm, member_reg, VM_CLASS_ID(java_lang_invoke_MemberName),
                   "MemberName required for invokeVirtual etc.");
    }

    Address member_clazz(    member_reg, NONZERO(java_lang_invoke_MemberName::clazz_offset()));
    Address member_vmindex(  member_reg, NONZERO(java_lang_invoke_MemberName::vmindex_offset()));
    Address member_vmtarget( member_reg, NONZERO(java_lang_invoke_MemberName::method_offset()));
    Address vmtarget_method( rbx_method, NONZERO(java_lang_invoke_ResolvedMethodName::vmtarget_offset()));

    Register temp1_recv_klass = temp1;
    if (iid != vmIntrinsics::_linkToStatic) {
      __ verify_oop(receiver_reg);
      if (iid == vmIntrinsics::_linkToSpecial) {
        // Don't actually load the klass; just null-check the receiver.
        __ null_check(receiver_reg);
      } else {
        // load receiver klass itself
        __ null_check(receiver_reg, oopDesc::klass_offset_in_bytes());
        __ load_klass(temp1_recv_klass, receiver_reg, temp2);
        __ verify_klass_ptr(temp1_recv_klass);
      }
      BLOCK_COMMENT("check_receiver {");
      // The receiver for the MemberName must be in receiver_reg.
      // Check the receiver against the MemberName.clazz
      if (VerifyMethodHandles && iid == vmIntrinsics::_linkToSpecial) {
        // Did not load it above...
        __ load_klass(temp1_recv_klass, receiver_reg, temp2);
        __ verify_klass_ptr(temp1_recv_klass);
      }
      if (VerifyMethodHandles && iid != vmIntrinsics::_linkToInterface) {
        Label L_ok;
        Register temp2_defc = temp2;
        __ load_heap_oop(temp2_defc, member_clazz, temp3);
        load_klass_from_Class(_masm, temp2_defc);
        __ verify_klass_ptr(temp2_defc);
        __ check_klass_subtype(temp1_recv_klass, temp2_defc, temp3, L_ok);
        // If we get here, the type check failed!
        __ STOP("receiver class disagrees with MemberName.clazz");
        __ bind(L_ok);
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
    //  rsi/r13 - interpreter linkage (if interpreted)
    //  rcx, rdx, rsi, rdi, r8 - compiler arguments (if compiled)

    Label L_incompatible_class_change_error;
    switch (iid) {
    case vmIntrinsics::_linkToSpecial:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeSpecial, member_reg, temp3);
      }
      __ load_heap_oop(rbx_method, member_vmtarget);
      __ access_load_at(T_ADDRESS, IN_HEAP, rbx_method, vmtarget_method, noreg, noreg);
      break;

    case vmIntrinsics::_linkToStatic:
      if (VerifyMethodHandles) {
        verify_ref_kind(_masm, JVM_REF_invokeStatic, member_reg, temp3);
      }
      __ load_heap_oop(rbx_method, member_vmtarget);
      __ access_load_at(T_ADDRESS, IN_HEAP, rbx_method, vmtarget_method, noreg, noreg);
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
      __ access_load_at(T_ADDRESS, IN_HEAP, temp2_index, member_vmindex, noreg, noreg);

      if (VerifyMethodHandles) {
        Label L_index_ok;
        __ cmpl(temp2_index, 0);
        __ jcc(Assembler::greaterEqual, L_index_ok);
        __ STOP("no virtual index");
        __ BIND(L_index_ok);
      }

      // Note:  The verifier invariants allow us to ignore MemberName.clazz and vmtarget
      // at this point.  And VerifyMethodHandles has already checked clazz, if needed.

      // get target Method* & entry point
      __ lookup_virtual_method(temp1_recv_klass, temp2_index, rbx_method);
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
      load_klass_from_Class(_masm, temp3_intf);
      __ verify_klass_ptr(temp3_intf);

      Register rbx_index = rbx_method;
      __ access_load_at(T_ADDRESS, IN_HEAP, rbx_index, member_vmindex, noreg, noreg);
      if (VerifyMethodHandles) {
        Label L;
        __ cmpl(rbx_index, 0);
        __ jcc(Assembler::greaterEqual, L);
        __ STOP("invalid vtable index for MH.invokeInterface");
        __ bind(L);
      }

      // given intf, index, and recv klass, dispatch to the implementation method
      __ lookup_interface_method(temp1_recv_klass, temp3_intf,
                                 // note: next two args must be the same:
                                 rbx_index, rbx_method,
                                 temp2,
                                 L_incompatible_class_change_error);
      break;
    }

    default:
      fatal("unexpected intrinsic %d: %s", vmIntrinsics::as_int(iid), vmIntrinsics::name_at(iid));
      break;
    }

    // Live at this point:
    //   rbx_method
    //   rsi/r13 (if interpreted)

    // After figuring out which concrete method to call, jump into it.
    // Note that this works in the interpreter with no data motion.
    // But the compiled version will require that rcx_recv be shifted out.
    __ verify_method_ptr(rbx_method);
    jump_from_method_handle(_masm, rbx_method, temp1, for_compiler_entry);

    if (iid == vmIntrinsics::_linkToInterface) {
      __ bind(L_incompatible_class_change_error);
      __ jump(RuntimeAddress(StubRoutines::throw_IncompatibleClassChangeError_entry()));
    }
  }
}

#ifndef PRODUCT
void trace_method_handle_stub(const char* adaptername,
                              oopDesc* mh,
                              intptr_t* saved_regs,
                              intptr_t* entry_sp) {
  // called as a leaf from native code: do not block the JVM!
  bool has_mh = (strstr(adaptername, "/static") == NULL &&
                 strstr(adaptername, "linkTo") == NULL);    // static linkers don't have MH
  const char* mh_reg_name = has_mh ? "rcx_mh" : "rcx";
  log_info(methodhandles)("MH %s %s=" PTR_FORMAT " sp=" PTR_FORMAT, adaptername, mh_reg_name, p2i(mh), p2i(entry_sp));

  LogTarget(Trace, methodhandles) lt;
  if (lt.is_enabled()) {
    ResourceMark rm;
    LogStream ls(lt);
    ls.print_cr("Registers:");
    const int saved_regs_count = RegisterImpl::number_of_registers;
    for (int i = 0; i < saved_regs_count; i++) {
      Register r = as_Register(i);
      // The registers are stored in reverse order on the stack (by pusha).
#ifdef AMD64
      assert(RegisterImpl::number_of_registers == 16, "sanity");
      if (r == rsp) {
        // rsp is actually not stored by pusha(), compute the old rsp from saved_regs (rsp after pusha): saved_regs + 16 = old rsp
        ls.print("%3s=" PTR_FORMAT, r->name(), (intptr_t)(&saved_regs[16]));
      } else {
        ls.print("%3s=" PTR_FORMAT, r->name(), saved_regs[((saved_regs_count - 1) - i)]);
      }
#else
      ls.print("%3s=" PTR_FORMAT, r->name(), saved_regs[((saved_regs_count - 1) - i)]);
#endif
      if ((i + 1) % 4 == 0) {
        ls.cr();
      } else {
        ls.print(", ");
      }
    }
    ls.cr();

    // Note: We want to allow trace_method_handle from any call site.
    // While trace_method_handle creates a frame, it may be entered
    // without a PC on the stack top (e.g. not just after a call).
    // Walking that frame could lead to failures due to that invalid PC.
    // => carefully detect that frame when doing the stack walking

    {
      // dumping last frame with frame::describe

      JavaThread* p = JavaThread::active();

      // may not be needed by safer and unexpensive here
      PreserveExceptionMark pem(Thread::current());
      FrameValues values;

      frame cur_frame = os::current_frame();

      if (cur_frame.fp() != 0) {  // not walkable

        // Robust search of trace_calling_frame (independent of inlining).
        // Assumes saved_regs comes from a pusha in the trace_calling_frame.
        //
        // We have to start the search from cur_frame, because trace_calling_frame may be it.
        // It is guaranteed that trace_calling_frame is different from the top frame.
        // But os::current_frame() does NOT return the top frame: it returns the next frame under it (caller's frame).
        // (Due to inlining and tail call optimizations, caller's frame doesn't necessarily correspond to the immediate
        // caller in the source code.)
        assert(cur_frame.sp() < saved_regs, "registers not saved on stack ?");
        frame trace_calling_frame = cur_frame;
        while (trace_calling_frame.fp() < saved_regs) {
          assert(trace_calling_frame.cb() == NULL, "not a C frame");
          trace_calling_frame = os::get_sender_for_C_frame(&trace_calling_frame);
        }
        assert(trace_calling_frame.sp() < saved_regs, "wrong frame");

        // safely create a frame and call frame::describe
        intptr_t *dump_sp = trace_calling_frame.sender_sp();
        intptr_t *dump_fp = trace_calling_frame.link();

        if (has_mh) {
          // The previous definition of walkable may have to be refined
          // if new call sites cause the next frame constructor to start
          // failing. Alternatively, frame constructors could be
          // modified to support the current or future non walkable
          // frames (but this is more intrusive and is not considered as
          // part of this RFE, which will instead use a simpler output).
          frame dump_frame = frame(dump_sp, dump_fp);
          dump_frame.describe(values, 1);
        } else {
          // Stack may not be walkable (invalid PC above FP):
          // Add descriptions without building a Java frame to avoid issues
          values.describe(-1, dump_fp, "fp for #1 <not parsed, cannot trust pc>");
          values.describe(-1, dump_sp, "sp for #1");
        }
      }
      values.describe(-1, entry_sp, "raw top of stack");

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

// The stub wraps the arguments in a struct on the stack to avoid
// dealing with the different calling conventions for passing 6
// arguments.
struct MethodHandleStubArguments {
  const char* adaptername;
  oopDesc* mh;
  intptr_t* saved_regs;
  intptr_t* entry_sp;
};
void trace_method_handle_stub_wrapper(MethodHandleStubArguments* args) {
  trace_method_handle_stub(args->adaptername,
                           args->mh,
                           args->saved_regs,
                           args->entry_sp);
}

void MethodHandles::trace_method_handle(MacroAssembler* _masm, const char* adaptername) {
  if (!log_is_enabled(Info, methodhandles))  return;
  BLOCK_COMMENT(err_msg("trace_method_handle %s {", adaptername));
  __ enter();
  __ andptr(rsp, -16); // align stack if needed for FPU state
  __ pusha();
  __ mov(rbx, rsp); // for retreiving saved_regs
  // Note: saved_regs must be in the entered frame for the
  // robust stack walking implemented in trace_method_handle_stub.

  // save FP result, valid at some call sites (adapter_opt_return_float, ...)
  __ decrement(rsp, 2 * wordSize);
#ifdef _LP64
  __ movdbl(Address(rsp, 0), xmm0);
#else
  if  (UseSSE >= 2) {
    __ movdbl(Address(rsp, 0), xmm0);
  } else if (UseSSE == 1) {
    __ movflt(Address(rsp, 0), xmm0);
  } else {
    __ fst_d(Address(rsp, 0));
  }
#endif // LP64

  // Incoming state:
  // rcx: method handle
  //
  // To avoid calling convention issues, build a record on the stack
  // and pass the pointer to that instead.
  __ push(rbp);               // entry_sp (with extra align space)
  __ push(rbx);               // pusha saved_regs
  __ push(rcx);               // mh
  __ push(rcx);               // slot for adaptername
  __ movptr(Address(rsp, 0), (intptr_t) adaptername);
  __ super_call_VM_leaf(CAST_FROM_FN_PTR(address, trace_method_handle_stub_wrapper), rsp);
  __ increment(rsp, sizeof(MethodHandleStubArguments));

#ifdef _LP64
  __ movdbl(xmm0, Address(rsp, 0));
#else
  if  (UseSSE >= 2) {
    __ movdbl(xmm0, Address(rsp, 0));
  } else if (UseSSE == 1) {
    __ movflt(xmm0, Address(rsp, 0));
  } else {
    __ fld_d(Address(rsp, 0));
  }
#endif // LP64
  __ increment(rsp, 2 * wordSize);

  __ popa();
  __ leave();
  BLOCK_COMMENT("} trace_method_handle");
}
#endif //PRODUCT
