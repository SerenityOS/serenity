/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/universe.hpp"
#include "nativeInst_arm.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/macros.hpp"
#include "vmreg_arm.inline.hpp"

#define __ ce->masm()->

void C1SafepointPollStub::emit_code(LIR_Assembler* ce) {
  ShouldNotReachHere();
}

void CounterOverflowStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  ce->store_parameter(_bci, 0);
  ce->store_parameter(_method->as_constant_ptr()->as_metadata(), 1);
  __ call(Runtime1::entry_for(Runtime1::counter_overflow_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);

  __ b(_continuation);
}


// TODO: ARM - is it possible to inline these stubs into the main code stream?


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
    __ call(Runtime1::entry_for(Runtime1::predicate_failed_trap_id), relocInfo::runtime_call_type);
    ce->add_call_info_here(_info);
    ce->verify_oop_map(_info);
    debug_only(__ should_not_reach_here());
    return;
  }
  // Pass the array index on stack because all registers must be preserved
  ce->verify_reserved_argument_area_size(_throw_index_out_of_bounds_exception ? 1 : 2);
  if (_index->is_cpu_register()) {
    __ str_32(_index->as_register(), Address(SP));
  } else {
    __ mov_slow(Rtemp, _index->as_jint()); // Rtemp should be OK in C1
    __ str_32(Rtemp, Address(SP));
  }

  if (_throw_index_out_of_bounds_exception) {
    __ call(Runtime1::entry_for(Runtime1::throw_index_exception_id), relocInfo::runtime_call_type);
  } else {
    __ str(_array->as_pointer_register(), Address(SP, BytesPerWord)); // ??? Correct offset? Correct instruction?
    __ call(Runtime1::entry_for(Runtime1::throw_range_check_failed_id), relocInfo::runtime_call_type);
  }
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  DEBUG_ONLY(STOP("RangeCheck");)
}

PredicateFailedStub::PredicateFailedStub(CodeEmitInfo* info) {
  _info = new CodeEmitInfo(info);
}

void PredicateFailedStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  __ call(Runtime1::entry_for(Runtime1::predicate_failed_trap_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

void DivByZeroStub::emit_code(LIR_Assembler* ce) {
  if (_offset != -1) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  __ call(Runtime1::entry_for(Runtime1::throw_div0_exception_id),
          relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  DEBUG_ONLY(STOP("DivByZero");)
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
  _stub_id   = stub_id;
}


void NewInstanceStub::emit_code(LIR_Assembler* ce) {
  assert(_result->as_register() == R0, "runtime call setup");
  assert(_klass_reg->as_register() == R1, "runtime call setup");
  __ bind(_entry);
  __ call(Runtime1::entry_for(_stub_id), relocInfo::runtime_call_type);
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
  assert(_result->as_register() == R0, "runtime call setup");
  assert(_klass_reg->as_register() == R1, "runtime call setup");
  assert(_length->as_register() == R2, "runtime call setup");
  __ bind(_entry);
  __ call(Runtime1::entry_for(Runtime1::new_type_array_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of NewObjectArrayStub

NewObjectArrayStub::NewObjectArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _result = result;
  _length = length;
  _info = new CodeEmitInfo(info);
}


void NewObjectArrayStub::emit_code(LIR_Assembler* ce) {
  assert(_result->as_register() == R0, "runtime call setup");
  assert(_klass_reg->as_register() == R1, "runtime call setup");
  assert(_length->as_register() == R2, "runtime call setup");
  __ bind(_entry);
  __ call(Runtime1::entry_for(Runtime1::new_object_array_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


// Implementation of MonitorAccessStubs

MonitorEnterStub::MonitorEnterStub(LIR_Opr obj_reg, LIR_Opr lock_reg, CodeEmitInfo* info)
: MonitorAccessStub(obj_reg, lock_reg)
{
  _info = new CodeEmitInfo(info);
}


void MonitorEnterStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  const Register obj_reg = _obj_reg->as_pointer_register();
  const Register lock_reg = _lock_reg->as_pointer_register();

  ce->verify_reserved_argument_area_size(2);
  if (obj_reg < lock_reg) {
    __ stmia(SP, RegisterSet(obj_reg) | RegisterSet(lock_reg));
  } else {
    __ str(obj_reg, Address(SP));
    __ str(lock_reg, Address(SP, BytesPerWord));
  }

  Runtime1::StubID enter_id = ce->compilation()->has_fpu_code() ?
                              Runtime1::monitorenter_id :
                              Runtime1::monitorenter_nofpu_id;
  __ call(Runtime1::entry_for(enter_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ b(_continuation);
}


void MonitorExitStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_compute_lock) {
    ce->monitor_address(_monitor_ix, _lock_reg);
  }
  const Register lock_reg = _lock_reg->as_pointer_register();

  ce->verify_reserved_argument_area_size(1);
  __ str(lock_reg, Address(SP));

  // Non-blocking leaf routine - no call info needed
  Runtime1::StubID exit_id = ce->compilation()->has_fpu_code() ?
                             Runtime1::monitorexit_id :
                             Runtime1::monitorexit_nofpu_id;
  __ call(Runtime1::entry_for(exit_id), relocInfo::runtime_call_type);
  __ b(_continuation);
}


// Call return is directly after patch word
int PatchingStub::_patch_info_offset = 0;

void PatchingStub::align_patch_site(MacroAssembler* masm) {
#if 0
  // TODO: investigate if we required to implement this
    ShouldNotReachHere();
#endif
}

void PatchingStub::emit_code(LIR_Assembler* ce) {
  const int patchable_instruction_offset = 0;

  assert(NativeCall::instruction_size <= _bytes_to_copy && _bytes_to_copy <= 0xFF,
         "not enough room for call");
  assert((_bytes_to_copy & 3) == 0, "must copy a multiple of four bytes");
  Label call_patch;
  bool is_load = (_id == load_klass_id) || (_id == load_mirror_id) || (_id == load_appendix_id);


  if (is_load && !VM_Version::supports_movw()) {
    address start = __ pc();

    // The following sequence duplicates code provided in MacroAssembler::patchable_mov_oop()
    // without creating relocation info entry.

    assert((__ pc() - start) == patchable_instruction_offset, "should be");
    __ ldr(_obj, Address(PC));
    // Extra nop to handle case of large offset of oop placeholder (see NativeMovConstReg::set_data).
    __ nop();

#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      assert(((address)_pc_start)[i] == start[i], "should be the same code");
    }
#endif // ASSERT
  }

  address being_initialized_entry = __ pc();
  if (CommentedAssembly) {
    __ block_comment(" patch template");
  }
  if (is_load) {
    address start = __ pc();
    if (_id == load_mirror_id || _id == load_appendix_id) {
      __ patchable_mov_oop(_obj, (jobject)Universe::non_oop_word(), _index);
    } else {
      __ patchable_mov_metadata(_obj, (Metadata*)Universe::non_oop_word(), _index);
    }
#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      assert(((address)_pc_start)[i] == start[i], "should be the same code");
    }
#endif // ASSERT
  } else {
    int* start = (int*)_pc_start;
    int* end = start + (_bytes_to_copy / BytesPerInt);
    while (start < end) {
      __ emit_int32(*start++);
    }
  }
  address end_of_patch = __ pc();

  int bytes_to_skip = 0;
  if (_id == load_mirror_id) {
    int offset = __ offset();
    if (CommentedAssembly) {
      __ block_comment(" being_initialized check");
    }

    assert(_obj != noreg, "must be a valid register");
    // Rtemp should be OK in C1
    __ ldr(Rtemp, Address(_obj, java_lang_Class::klass_offset()));
    __ ldr(Rtemp, Address(Rtemp, InstanceKlass::init_thread_offset()));
    __ cmp(Rtemp, Rthread);
    __ b(call_patch, ne);
    __ b(_patch_site_continuation);

    bytes_to_skip += __ offset() - offset;
  }

  if (CommentedAssembly) {
    __ block_comment("patch data - 3 high bytes of the word");
  }
  const int sizeof_patch_record = 4;
  bytes_to_skip += sizeof_patch_record;
  int being_initialized_entry_offset = __ pc() - being_initialized_entry + sizeof_patch_record;
  __ emit_int32(0xff | being_initialized_entry_offset << 8 | bytes_to_skip << 16 | _bytes_to_copy << 24);

  address patch_info_pc = __ pc();
  assert(patch_info_pc - end_of_patch == bytes_to_skip, "incorrect patch info");

  // runtime call will return here
  Label call_return;
  __ bind(call_return);
  ce->add_call_info_here(_info);
  assert(_patch_info_offset == (patch_info_pc - __ pc()), "must not change");
  __ b(_patch_site_entry);

  address entry = __ pc();
  NativeGeneralJump::insert_unconditional((address)_pc_start, entry);
  address target = NULL;
  relocInfo::relocType reloc_type = relocInfo::none;
  switch (_id) {
    case access_field_id:  target = Runtime1::entry_for(Runtime1::access_field_patching_id); break;
    case load_klass_id:    target = Runtime1::entry_for(Runtime1::load_klass_patching_id); reloc_type = relocInfo::metadata_type; break;
    case load_mirror_id:   target = Runtime1::entry_for(Runtime1::load_mirror_patching_id); reloc_type = relocInfo::oop_type; break;
    case load_appendix_id: target = Runtime1::entry_for(Runtime1::load_appendix_patching_id); reloc_type = relocInfo::oop_type; break;
    default: ShouldNotReachHere();
  }
  __ bind(call_patch);

  if (CommentedAssembly) {
    __ block_comment("patch entry point");
  }

  // arrange for call to return just after patch word
  __ adr(LR, call_return);
  __ jump(target, relocInfo::runtime_call_type, Rtemp);

  if (is_load) {
    CodeSection* cs = __ code_section();
    address pc = (address)_pc_start;
    RelocIterator iter(cs, pc, pc + 1);
    relocInfo::change_reloc_info_for_address(&iter, pc, reloc_type, relocInfo::none);
  }
}

void DeoptimizeStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  __ mov_slow(Rtemp, _trap_request);
  ce->verify_reserved_argument_area_size(1);
  __ str(Rtemp, Address(SP));
  __ call(Runtime1::entry_for(Runtime1::deoptimize_id), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}


void ImplicitNullCheckStub::emit_code(LIR_Assembler* ce) {
  address a;
  if (_info->deoptimize_on_exception()) {
    // Deoptimize, do not throw the exception, because it is
    // probably wrong to do it here.
    a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
  } else {
    a = Runtime1::entry_for(Runtime1::throw_null_pointer_exception_id);
  }
  ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  __ bind(_entry);
  __ call(a, relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  DEBUG_ONLY(STOP("ImplicitNullCheck");)
}


void SimpleExceptionStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  // Pass the object on stack because all registers must be preserved
  if (_obj->is_cpu_register()) {
    ce->verify_reserved_argument_area_size(1);
    __ str(_obj->as_pointer_register(), Address(SP));
  } else {
    assert(_obj->is_illegal(), "should be");
  }
  __ call(Runtime1::entry_for(_stub), relocInfo::runtime_call_type);
  ce->add_call_info_here(_info);
  DEBUG_ONLY(STOP("SimpleException");)
}


void ArrayCopyStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);

  VMRegPair args[5];
  BasicType signature[5] = { T_OBJECT, T_INT, T_OBJECT, T_INT, T_INT };
  SharedRuntime::java_calling_convention(signature, args, 5);

  Register r[5];
  r[0] = src()->as_pointer_register();
  r[1] = src_pos()->as_register();
  r[2] = dst()->as_pointer_register();
  r[3] = dst_pos()->as_register();
  r[4] = length()->as_register();

  for (int i = 0; i < 5; i++) {
    VMReg arg = args[i].first();
    if (arg->is_stack()) {
      __ str(r[i], Address(SP, arg->reg2stack() * VMRegImpl::stack_slot_size));
    } else {
      assert(r[i] == arg->as_Register(), "Calling conventions must match");
    }
  }

  ce->emit_static_call_stub();
  if (ce->compilation()->bailed_out()) {
    return; // CodeCache is full
  }
  int ret_addr_offset = __ patchable_call(SharedRuntime::get_resolve_static_call_stub(), relocInfo::static_call_type);
  assert(ret_addr_offset == __ offset(), "embedded return address not allowed");
  ce->add_call_info_here(info());
  ce->verify_oop_map(info());
  __ b(_continuation);
}

#undef __
