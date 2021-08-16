/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2018 SAP SE. All rights reserved.
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
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "classfile/javaClasses.hpp"
#include "nativeInst_s390.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"
#include "vmreg_s390.inline.hpp"

#define __ ce->masm()->
#undef  CHECK_BAILOUT
#define CHECK_BAILOUT() { if (ce->compilation()->bailed_out()) return; }

void C1SafepointPollStub::emit_code(LIR_Assembler* ce) {
  ShouldNotReachHere();
}

RangeCheckStub::RangeCheckStub(CodeEmitInfo* info, LIR_Opr index, LIR_Opr array)
  : _index(index), _array(array), _throw_index_out_of_bounds_exception(false) {
  assert(info != NULL, "must have info");
  _info = new CodeEmitInfo(info);
}

RangeCheckStub::RangeCheckStub(CodeEmitInfo* info, LIR_Opr index)
  : _index(index), _array(NULL), _throw_index_out_of_bounds_exception(true) {
  assert(info != NULL, "must have info");
  _info = new CodeEmitInfo(info);
}

void RangeCheckStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_info->deoptimize_on_exception()) {
    address a = Runtime1::entry_for (Runtime1::predicate_failed_trap_id);
    ce->emit_call_c(a);
    CHECK_BAILOUT();
    ce->add_call_info_here(_info);
    ce->verify_oop_map(_info);
    debug_only(__ should_not_reach_here());
    return;
  }

  // Pass the array index in Z_R1_scratch which is not managed by linear scan.
  if (_index->is_cpu_register()) {
    __ lgr_if_needed(Z_R1_scratch, _index->as_register());
  } else {
    __ load_const_optimized(Z_R1_scratch, _index->as_jint());
  }

  Runtime1::StubID stub_id;
  if (_throw_index_out_of_bounds_exception) {
    stub_id = Runtime1::throw_index_exception_id;
  } else {
    stub_id = Runtime1::throw_range_check_failed_id;
    __ lgr_if_needed(Z_R0_scratch, _array->as_pointer_register());
  }
  ce->emit_call_c(Runtime1::entry_for (stub_id));
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

PredicateFailedStub::PredicateFailedStub(CodeEmitInfo* info) {
  _info = new CodeEmitInfo(info);
}

void PredicateFailedStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address a = Runtime1::entry_for (Runtime1::predicate_failed_trap_id);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

void CounterOverflowStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  Metadata *m = _method->as_constant_ptr()->as_metadata();
  bool success = __ set_metadata_constant(m, Z_R1_scratch);
  if (!success) {
    ce->compilation()->bailout("const section overflow");
    return;
  }
  ce->store_parameter(/*_method->as_register()*/ Z_R1_scratch, 1);
  ce->store_parameter(_bci, 0);
  ce->emit_call_c(Runtime1::entry_for (Runtime1::counter_overflow_id));
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ branch_optimized(Assembler::bcondAlways, _continuation);
}

void DivByZeroStub::emit_code(LIR_Assembler* ce) {
  if (_offset != -1) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  ce->emit_call_c(Runtime1::entry_for (Runtime1::throw_div0_exception_id));
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  debug_only(__ should_not_reach_here());
}

void ImplicitNullCheckStub::emit_code(LIR_Assembler* ce) {
  address a;
  if (_info->deoptimize_on_exception()) {
    // Deoptimize, do not throw the exception, because it is probably wrong to do it here.
    a = Runtime1::entry_for (Runtime1::predicate_failed_trap_id);
  } else {
    a = Runtime1::entry_for (Runtime1::throw_null_pointer_exception_id);
  }

  ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  __ bind(_entry);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

// Note: pass object in Z_R1_scratch
void SimpleExceptionStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_obj->is_valid()) {
    __ z_lgr(Z_R1_scratch, _obj->as_register()); // _obj contains the optional argument to the stub
  }
  address a = Runtime1::entry_for (_stub);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  debug_only(__ should_not_reach_here());
}

NewInstanceStub::NewInstanceStub(LIR_Opr klass_reg, LIR_Opr result, ciInstanceKlass* klass, CodeEmitInfo* info, Runtime1::StubID stub_id) {
  _result = result;
  _klass = klass;
  _klass_reg = klass_reg;
  _info = new CodeEmitInfo(info);
  assert(stub_id == Runtime1::new_instance_id                 ||
         stub_id == Runtime1::fast_new_instance_id            ||
         stub_id == Runtime1::fast_new_instance_init_check_id,
         "need new_instance id");
  _stub_id = stub_id;
}

void NewInstanceStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  assert(_klass_reg->as_register() == Z_R11, "call target expects klass in Z_R11");
  address a = Runtime1::entry_for (_stub_id);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == Z_R2, "callee returns result in Z_R2,");
  __ z_brul(_continuation);
}

NewTypeArrayStub::NewTypeArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}

void NewTypeArrayStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  assert(_klass_reg->as_register() == Z_R11, "call target expects klass in Z_R11");
  __ lgr_if_needed(Z_R13, _length->as_register());
  address a = Runtime1::entry_for (Runtime1::new_type_array_id);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == Z_R2, "callee returns result in Z_R2,");
  __ z_brul(_continuation);
}

NewObjectArrayStub::NewObjectArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}

void NewObjectArrayStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  assert(_klass_reg->as_register() == Z_R11, "call target expects klass in Z_R11");
  __ lgr_if_needed(Z_R13, _length->as_register());
  address a = Runtime1::entry_for (Runtime1::new_object_array_id);
  ce->emit_call_c(a);
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == Z_R2, "callee returns result in Z_R2,");
  __ z_brul(_continuation);
}

MonitorEnterStub::MonitorEnterStub(LIR_Opr obj_reg, LIR_Opr lock_reg, CodeEmitInfo* info)
  : MonitorAccessStub(obj_reg, lock_reg) {
  _info = new CodeEmitInfo(info);
}

void MonitorEnterStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  Runtime1::StubID enter_id;
  if (ce->compilation()->has_fpu_code()) {
    enter_id = Runtime1::monitorenter_id;
  } else {
    enter_id = Runtime1::monitorenter_nofpu_id;
  }
  __ lgr_if_needed(Z_R1_scratch, _obj_reg->as_register());
  __ lgr_if_needed(Z_R13, _lock_reg->as_register()); // See LIRGenerator::syncTempOpr().
  ce->emit_call_c(Runtime1::entry_for (enter_id));
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ branch_optimized(Assembler::bcondAlways, _continuation);
}

void MonitorExitStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  // Move address of the BasicObjectLock into Z_R1_scratch.
  if (_compute_lock) {
    // Lock_reg was destroyed by fast unlocking attempt => recompute it.
    ce->monitor_address(_monitor_ix, FrameMap::as_opr(Z_R1_scratch));
  } else {
    __ lgr_if_needed(Z_R1_scratch, _lock_reg->as_register());
  }
  // Note: non-blocking leaf routine => no call info needed.
  Runtime1::StubID exit_id;
  if (ce->compilation()->has_fpu_code()) {
    exit_id = Runtime1::monitorexit_id;
  } else {
    exit_id = Runtime1::monitorexit_nofpu_id;
  }
  ce->emit_call_c(Runtime1::entry_for (exit_id));
  CHECK_BAILOUT();
  __ branch_optimized(Assembler::bcondAlways, _continuation);
}

// Implementation of patching:
// - Copy the code at given offset to an inlined buffer (first the bytes, then the number of bytes).
// - Replace original code with a call to the stub.
// At Runtime:
// - call to stub, jump to runtime.
// - in runtime: Preserve all registers (especially objects, i.e., source and destination object).
// - in runtime: After initializing class, restore original code, reexecute instruction.

int PatchingStub::_patch_info_offset = - (12 /* load const */ + 2 /*BASR*/);

void PatchingStub::align_patch_site(MacroAssembler* masm) {
#ifndef PRODUCT
  const char* bc;
  switch (_id) {
  case access_field_id: bc = "patch site (access_field)"; break;
  case load_klass_id: bc = "patch site (load_klass)"; break;
  case load_mirror_id: bc = "patch site (load_mirror)"; break;
  case load_appendix_id: bc = "patch site (load_appendix)"; break;
  default: bc = "patch site (unknown patch id)"; break;
  }
  masm->block_comment(bc);
#endif

  masm->align(align_up((int)NativeGeneralJump::instruction_size, wordSize));
}

void PatchingStub::emit_code(LIR_Assembler* ce) {
  // Copy original code here.
  assert(NativeGeneralJump::instruction_size <= _bytes_to_copy && _bytes_to_copy <= 0xFF,
         "not enough room for call, need %d", _bytes_to_copy);

  NearLabel call_patch;

  int being_initialized_entry = __ offset();

  if (_id == load_klass_id) {
    // Produce a copy of the load klass instruction for use by the case being initialized.
#ifdef ASSERT
    address start = __ pc();
#endif
    AddressLiteral addrlit((intptr_t)0, metadata_Relocation::spec(_index));
    __ load_const(_obj, addrlit);

#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      assert(a_byte == *start++, "should be the same code");
    }
#endif
  } else if (_id == load_mirror_id || _id == load_appendix_id) {
    // Produce a copy of the load mirror instruction for use by the case being initialized.
#ifdef ASSERT
    address start = __ pc();
#endif
    AddressLiteral addrlit((intptr_t)0, oop_Relocation::spec(_index));
    __ load_const(_obj, addrlit);

#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      assert(a_byte == *start++, "should be the same code");
    }
#endif
  } else {
    // Make a copy of the code which is going to be patched.
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      __ emit_int8 (a_byte);
    }
  }

  address end_of_patch = __ pc();
  int bytes_to_skip = 0;
  if (_id == load_mirror_id) {
    int offset = __ offset();
    if (CommentedAssembly) {
      __ block_comment(" being_initialized check");
    }

    // Static field accesses have special semantics while the class
    // initializer is being run, so we emit a test which can be used to
    // check that this code is being executed by the initializing
    // thread.
    assert(_obj != noreg, "must be a valid register");
    assert(_index >= 0, "must have oop index");
    __ z_lg(Z_R1_scratch, java_lang_Class::klass_offset(), _obj);
    __ z_cg(Z_thread, Address(Z_R1_scratch, InstanceKlass::init_thread_offset()));
    __ branch_optimized(Assembler::bcondNotEqual, call_patch);

    // Load_klass patches may execute the patched code before it's
    // copied back into place so we need to jump back into the main
    // code of the nmethod to continue execution.
    __ branch_optimized(Assembler::bcondAlways, _patch_site_continuation);

    // Make sure this extra code gets skipped.
    bytes_to_skip += __ offset() - offset;
  }

  // Now emit the patch record telling the runtime how to find the
  // pieces of the patch. We only need 3 bytes but to help the disassembler
  // we make the data look like a the following add instruction:
  //   A R1, D2(X2, B2)
  // which requires 4 bytes.
  int sizeof_patch_record = 4;
  bytes_to_skip += sizeof_patch_record;

  // Emit the offsets needed to find the code to patch.
  int being_initialized_entry_offset = __ offset() - being_initialized_entry + sizeof_patch_record;

  // Emit the patch record: opcode of the add followed by 3 bytes patch record data.
  __ emit_int8((int8_t)(A_ZOPC>>24));
  __ emit_int8(being_initialized_entry_offset);
  __ emit_int8(bytes_to_skip);
  __ emit_int8(_bytes_to_copy);
  address patch_info_pc = __ pc();
  assert(patch_info_pc - end_of_patch == bytes_to_skip, "incorrect patch info");

  address entry = __ pc();
  NativeGeneralJump::insert_unconditional((address)_pc_start, entry);
  address target = NULL;
  relocInfo::relocType reloc_type = relocInfo::none;
  switch (_id) {
    case access_field_id:  target = Runtime1::entry_for (Runtime1::access_field_patching_id); break;
    case load_klass_id:    target = Runtime1::entry_for (Runtime1::load_klass_patching_id); reloc_type = relocInfo::metadata_type; break;
    case load_mirror_id:   target = Runtime1::entry_for (Runtime1::load_mirror_patching_id); reloc_type = relocInfo::oop_type; break;
    case load_appendix_id: target = Runtime1::entry_for (Runtime1::load_appendix_patching_id); reloc_type = relocInfo::oop_type; break;
    default: ShouldNotReachHere();
  }
  __ bind(call_patch);

  if (CommentedAssembly) {
    __ block_comment("patch entry point");
  }
  // Cannot use call_c_opt() because its size is not constant.
  __ load_const(Z_R1_scratch, target); // Must not optimize in order to keep constant _patch_info_offset constant.
  __ z_basr(Z_R14, Z_R1_scratch);
  assert(_patch_info_offset == (patch_info_pc - __ pc()), "must not change");
  ce->add_call_info_here(_info);
  __ z_brcl(Assembler::bcondAlways, _patch_site_entry);
  if (_id == load_klass_id || _id == load_mirror_id || _id == load_appendix_id) {
    CodeSection* cs = __ code_section();
    address pc = (address)_pc_start;
    RelocIterator iter(cs, pc, pc + 1);
    relocInfo::change_reloc_info_for_address(&iter, (address) pc, reloc_type, relocInfo::none);
  }
}

void DeoptimizeStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  __ load_const_optimized(Z_R1_scratch, _trap_request); // Pass trap request in Z_R1_scratch.
  ce->emit_call_c(Runtime1::entry_for (Runtime1::deoptimize_id));
  CHECK_BAILOUT();
  ce->add_call_info_here(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}

void ArrayCopyStub::emit_code(LIR_Assembler* ce) {
  // Slow case: call to native.
  __ bind(_entry);
  __ lgr_if_needed(Z_ARG1, src()->as_register());
  __ lgr_if_needed(Z_ARG2, src_pos()->as_register());
  __ lgr_if_needed(Z_ARG3, dst()->as_register());
  __ lgr_if_needed(Z_ARG4, dst_pos()->as_register());
  __ lgr_if_needed(Z_ARG5, length()->as_register());

  // Must align calls sites, otherwise they can't be updated atomically on MP hardware.
  ce->align_call(lir_static_call);

  assert((__ offset() + NativeCall::call_far_pcrelative_displacement_offset) % NativeCall::call_far_pcrelative_displacement_alignment == 0,
         "must be aligned");

  ce->emit_static_call_stub();

  // Prepend each BRASL with a nop.
  __ relocate(relocInfo::static_call_type);
  __ z_nop();
  __ z_brasl(Z_R14, SharedRuntime::get_resolve_static_call_stub());
  ce->add_call_info_here(info());
  ce->verify_oop_map(info());

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ load_const_optimized(Z_R1_scratch, (address)&Runtime1::_arraycopy_slowcase_cnt);
    __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
  }
#endif

  __ branch_optimized(Assembler::bcondAlways, _continuation);
}

#undef __
