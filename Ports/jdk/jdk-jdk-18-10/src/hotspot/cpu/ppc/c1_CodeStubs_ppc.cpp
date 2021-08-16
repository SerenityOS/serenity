/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "classfile/javaClasses.hpp"
#include "nativeInst_ppc.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/macros.hpp"
#include "vmreg_ppc.inline.hpp"

#define __ ce->masm()->

void C1SafepointPollStub::emit_code(LIR_Assembler* ce) {
  if (UseSIGTRAP) {
    DEBUG_ONLY( __ should_not_reach_here("C1SafepointPollStub::emit_code"); )
  } else {
    assert(SharedRuntime::polling_page_return_handler_blob() != NULL,
           "polling page return stub not created yet");
    address stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();

    __ bind(_entry);
    // Using pc relative address computation.
    {
      Label next_pc;
      __ bl(next_pc);
      __ bind(next_pc);
    }
    int current_offset = __ offset();
    __ mflr(R12);
    __ add_const_optimized(R12, R12, safepoint_offset() - current_offset);
    __ std(R12, in_bytes(JavaThread::saved_exception_pc_offset()), R16_thread);

    __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
    __ mtctr(R0);
    __ bctr();
  }
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
    address a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
    // May be used by optimizations like LoopInvariantCodeMotion or RangeCheckEliminator.
    DEBUG_ONLY( __ untested("RangeCheckStub: predicate_failed_trap_id"); )
    //__ load_const_optimized(R0, a);
    __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(a));
    __ mtctr(R0);
    __ bctrl();
    ce->add_call_info_here(_info);
    ce->verify_oop_map(_info);
    debug_only(__ illtrap());
    return;
  }

  address stub = _throw_index_out_of_bounds_exception ? Runtime1::entry_for(Runtime1::throw_index_exception_id)
                                                      : Runtime1::entry_for(Runtime1::throw_range_check_failed_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mtctr(R0);

  Register index = R0;
  if (_index->is_register()) {
    __ extsw(index, _index->as_register());
  } else {
    __ load_const_optimized(index, _index->as_jint());
  }
  if (_array) {
    __ std(_array->as_pointer_register(), -8, R1_SP);
  }
  __ std(index, -16, R1_SP);

  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ illtrap());
}


PredicateFailedStub::PredicateFailedStub(CodeEmitInfo* info) {
  _info = new CodeEmitInfo(info);
}

void PredicateFailedStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
  //__ load_const_optimized(R0, a);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(a));
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ illtrap());
}


void CounterOverflowStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);

  // Parameter 1: bci
  __ load_const_optimized(R0, _bci);
  __ std(R0, -16, R1_SP);

  // Parameter 2: Method*
  Metadata *m = _method->as_constant_ptr()->as_metadata();
  AddressLiteral md = __ constant_metadata_address(m); // Notify OOP recorder (don't need the relocation).
  __ load_const_optimized(R0, md.value());
  __ std(R0, -8, R1_SP);

  address a = Runtime1::entry_for(Runtime1::counter_overflow_id);
  //__ load_const_optimized(R0, a);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(a));
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);

  __ b(_continuation);
}


void DivByZeroStub::emit_code(LIR_Assembler* ce) {
  if (_offset != -1) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  address stub = Runtime1::entry_for(Runtime1::throw_div0_exception_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ illtrap());
}


void ImplicitNullCheckStub::emit_code(LIR_Assembler* ce) {
  address a;
  if (_info->deoptimize_on_exception()) {
    // Deoptimize, do not throw the exception, because it is probably wrong to do it here.
    a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
  } else {
    a = Runtime1::entry_for(Runtime1::throw_null_pointer_exception_id);
  }

  if (ImplicitNullChecks || TrapBasedNullChecks) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  //__ load_const_optimized(R0, a);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(a));
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ illtrap());
}


// Implementation of SimpleExceptionStub
void SimpleExceptionStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address stub = Runtime1::entry_for(_stub);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  if (_obj->is_valid()) { __ mr_if_needed(/*tmp1 in do_CheckCast*/ R4_ARG2, _obj->as_register()); }
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  debug_only( __ illtrap(); )
}


// Implementation of NewInstanceStub
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

  address entry = Runtime1::entry_for(_stub_id);
  //__ load_const_optimized(R0, entry);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(entry));
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of NewTypeArrayStub
NewTypeArrayStub::NewTypeArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}

void NewTypeArrayStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);

  address entry = Runtime1::entry_for(Runtime1::new_type_array_id);
  //__ load_const_optimized(R0, entry);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(entry));
  __ mr_if_needed(/*op->tmp1()->as_register()*/ R5_ARG3, _length->as_register()); // already sign-extended
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of NewObjectArrayStub
NewObjectArrayStub::NewObjectArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}

void NewObjectArrayStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);

  address entry = Runtime1::entry_for(Runtime1::new_object_array_id);
  //__ load_const_optimized(R0, entry);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(entry));
  __ mr_if_needed(/*op->tmp1()->as_register()*/ R5_ARG3, _length->as_register()); // already sign-extended
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of MonitorAccessStubs
MonitorEnterStub::MonitorEnterStub(LIR_Opr obj_reg, LIR_Opr lock_reg, CodeEmitInfo* info)
  : MonitorAccessStub(obj_reg, lock_reg) {
  _info = new CodeEmitInfo(info);
}

void MonitorEnterStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address stub = Runtime1::entry_for(ce->compilation()->has_fpu_code() ? Runtime1::monitorenter_id : Runtime1::monitorenter_nofpu_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mr_if_needed(/*scratch_opr()->as_register()*/ R4_ARG2, _obj_reg->as_register());
  assert(_lock_reg->as_register() == R5_ARG3, "");
  __ mtctr(R0);
  __ bctrl();
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}

void MonitorExitStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_compute_lock) {
    ce->monitor_address(_monitor_ix, _lock_reg);
  }
  address stub = Runtime1::entry_for(ce->compilation()->has_fpu_code() ? Runtime1::monitorexit_id : Runtime1::monitorexit_nofpu_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  assert(_lock_reg->as_register() == R4_ARG2, "");
  __ mtctr(R0);
  __ bctrl();
  __ b(_continuation);
}


// Implementation of patching:
// - Copy the code at given offset to an inlined buffer (first the bytes, then the number of bytes).
// - Replace original code with a call to the stub.
// At Runtime:
// - call to stub, jump to runtime
// - in runtime: preserve all registers (especially objects, i.e., source and destination object)
// - in runtime: after initializing class, restore original code, reexecute instruction

int PatchingStub::_patch_info_offset = -(5 * BytesPerInstWord);

void PatchingStub::align_patch_site(MacroAssembler* ) {
  // Patch sites on ppc are always properly aligned.
}

#ifdef ASSERT
inline void compare_with_patch_site(address template_start, address pc_start, int bytes_to_copy) {
  address start = template_start;
  for (int i = 0; i < bytes_to_copy; i++) {
    address ptr = (address)(pc_start + i);
    int a_byte = (*ptr) & 0xFF;
    assert(a_byte == *start++, "should be the same code");
  }
}
#endif

void PatchingStub::emit_code(LIR_Assembler* ce) {
  // copy original code here
  assert(NativeGeneralJump::instruction_size <= _bytes_to_copy && _bytes_to_copy <= 0xFF,
         "not enough room for call, need %d", _bytes_to_copy);
  assert((_bytes_to_copy & 0x3) == 0, "must copy a multiple of four bytes");

  Label call_patch;

  int being_initialized_entry = __ offset();

  if (_id == load_klass_id) {
    // Produce a copy of the load klass instruction for use by the being initialized case.
    AddressLiteral addrlit((address)NULL, metadata_Relocation::spec(_index));
    __ load_const(_obj, addrlit, R0);
    DEBUG_ONLY( compare_with_patch_site(__ code_section()->start() + being_initialized_entry, _pc_start, _bytes_to_copy); )
  } else if (_id == load_mirror_id || _id == load_appendix_id) {
    // Produce a copy of the load mirror instruction for use by the being initialized case.
    AddressLiteral addrlit((address)NULL, oop_Relocation::spec(_index));
    __ load_const(_obj, addrlit, R0);
    DEBUG_ONLY( compare_with_patch_site(__ code_section()->start() + being_initialized_entry, _pc_start, _bytes_to_copy); )
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
    __ block_comment(" being_initialized check");

    // Static field accesses have special semantics while the class
    // initializer is being run so we emit a test which can be used to
    // check that this code is being executed by the initializing
    // thread.
    assert(_obj != noreg, "must be a valid register");
    assert(_index >= 0, "must have oop index");
    __ mr(R0, _obj); // spill
    __ ld(_obj, java_lang_Class::klass_offset(), _obj);
    __ ld(_obj, in_bytes(InstanceKlass::init_thread_offset()), _obj);
    __ cmpd(CCR0, _obj, R16_thread);
    __ mr(_obj, R0); // restore
    __ bne(CCR0, call_patch);

    // Load_klass patches may execute the patched code before it's
    // copied back into place so we need to jump back into the main
    // code of the nmethod to continue execution.
    __ b(_patch_site_continuation);

    // Make sure this extra code gets skipped.
    bytes_to_skip += __ offset() - offset;
  }

  // Now emit the patch record telling the runtime how to find the
  // pieces of the patch.  We only need 3 bytes but it has to be
  // aligned as an instruction so emit 4 bytes.
  int sizeof_patch_record = 4;
  bytes_to_skip += sizeof_patch_record;

  // Emit the offsets needed to find the code to patch.
  int being_initialized_entry_offset = __ offset() - being_initialized_entry + sizeof_patch_record;

  // Emit the patch record.  We need to emit a full word, so emit an extra empty byte.
  __ emit_int8(0);
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
    case access_field_id:  target = Runtime1::entry_for(Runtime1::access_field_patching_id); break;
    case load_klass_id:    target = Runtime1::entry_for(Runtime1::load_klass_patching_id);
                           reloc_type = relocInfo::metadata_type; break;
    case load_mirror_id:   target = Runtime1::entry_for(Runtime1::load_mirror_patching_id);
                           reloc_type = relocInfo::oop_type; break;
    case load_appendix_id: target = Runtime1::entry_for(Runtime1::load_appendix_patching_id);
                           reloc_type = relocInfo::oop_type; break;
    default: ShouldNotReachHere();
  }
  __ bind(call_patch);

  __ block_comment("patch entry point");
  //__ load_const(R0, target); + mtctr + bctrl must have size -_patch_info_offset
  __ load_const32(R0, MacroAssembler::offset_to_global_toc(target));
  __ add(R0, R29_TOC, R0);
  __ mtctr(R0);
  __ bctrl();
  assert(_patch_info_offset == (patch_info_pc - __ pc()), "must not change");
  ce->add_call_info_here(_info);
  __ b(_patch_site_entry);
  if (_id == load_klass_id || _id == load_mirror_id || _id == load_appendix_id) {
    CodeSection* cs = __ code_section();
    address pc = (address)_pc_start;
    RelocIterator iter(cs, pc, pc + 1);
    relocInfo::change_reloc_info_for_address(&iter, (address) pc, reloc_type, relocInfo::none);
  }
}


void DeoptimizeStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address stub = Runtime1::entry_for(Runtime1::deoptimize_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mtctr(R0);

  __ load_const_optimized(R0, _trap_request); // Pass trap request in R0.
  __ bctrl();
  ce->add_call_info_here(_info);
  debug_only(__ illtrap());
}


void ArrayCopyStub::emit_code(LIR_Assembler* ce) {
  //---------------slow case: call to native-----------------
  __ bind(_entry);
  __ mr(R3_ARG1, src()->as_register());
  __ extsw(R4_ARG2, src_pos()->as_register());
  __ mr(R5_ARG3, dst()->as_register());
  __ extsw(R6_ARG4, dst_pos()->as_register());
  __ extsw(R7_ARG5, length()->as_register());

  ce->emit_static_call_stub();

  bool success = ce->emit_trampoline_stub_for_call(SharedRuntime::get_resolve_static_call_stub());
  if (!success) { return; }

  __ relocate(relocInfo::static_call_type);
  // Note: At this point we do not have the address of the trampoline
  // stub, and the entry point might be too far away for bl, so __ pc()
  // serves as dummy and the bl will be patched later.
  __ code()->set_insts_mark();
  __ bl(__ pc());
  ce->add_call_info_here(info());
  ce->verify_oop_map(info());

#ifndef PRODUCT
  if (PrintC1Statistics) {
    const address counter = (address)&Runtime1::_arraycopy_slowcase_cnt;
    const Register tmp = R3, tmp2 = R4;
    int simm16_offs = __ load_const_optimized(tmp, counter, tmp2, true);
    __ lwz(tmp2, simm16_offs, tmp);
    __ addi(tmp2, tmp2, 1);
    __ stw(tmp2, simm16_offs, tmp);
  }
#endif

  __ b(_continuation);
}

#undef __
