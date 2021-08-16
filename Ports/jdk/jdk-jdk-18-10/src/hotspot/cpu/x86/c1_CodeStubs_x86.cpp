/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "classfile/javaClasses.hpp"
#include "nativeInst_x86.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"
#include "vmreg_x86.inline.hpp"


#define __ ce->masm()->

#ifndef _LP64
float ConversionStub::float_zero = 0.0;
double ConversionStub::double_zero = 0.0;

void ConversionStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  assert(bytecode() == Bytecodes::_f2i || bytecode() == Bytecodes::_d2i, "other conversions do not require stub");


  if (input()->is_single_xmm()) {
    __ comiss(input()->as_xmm_float_reg(),
              ExternalAddress((address)&float_zero));
  } else if (input()->is_double_xmm()) {
    __ comisd(input()->as_xmm_double_reg(),
              ExternalAddress((address)&double_zero));
  } else {
    __ push(rax);
    __ ftst();
    __ fnstsw_ax();
    __ sahf();
    __ pop(rax);
  }

  Label NaN, do_return;
  __ jccb(Assembler::parity, NaN);
  __ jccb(Assembler::below, do_return);

  // input is > 0 -> return maxInt
  // result register already contains 0x80000000, so subtracting 1 gives 0x7fffffff
  __ decrement(result()->as_register());
  __ jmpb(do_return);

  // input is NaN -> return 0
  __ bind(NaN);
  __ xorptr(result()->as_register(), result()->as_register());

  __ bind(do_return);
  __ jmp(_continuation);
}
#endif // !_LP64

void C1SafepointPollStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  InternalAddress safepoint_pc(ce->masm()->pc() - ce->masm()->offset() + safepoint_offset());
#ifdef _LP64
  __ lea(rscratch1, safepoint_pc);
  __ movptr(Address(r15_thread, JavaThread::saved_exception_pc_offset()), rscratch1);
#else
  const Register tmp1 = rcx;
  const Register tmp2 = rdx;
  __ push(tmp1);
  __ push(tmp2);

  __ lea(tmp1, safepoint_pc);
  __ get_thread(tmp2);
  __ movptr(Address(tmp2, JavaThread::saved_exception_pc_offset()), tmp1);

  __ pop(tmp2);
  __ pop(tmp1);
#endif /* _LP64 */
  assert(SharedRuntime::polling_page_return_handler_blob() != NULL,
         "polling page return stub not created yet");

  address stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();
  __ jump(RuntimeAddress(stub));
}

void CounterOverflowStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  Metadata *m = _method->as_constant_ptr()->as_metadata();
  ce->store_parameter(m, 1);
  ce->store_parameter(_bci, 0);
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::counter_overflow_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ jmp(_continuation);
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
    __ call(RuntimeAddress(a));
    ce->add_call_info_here(_info);
    ce->verify_oop_map(_info);
    debug_only(__ should_not_reach_here());
    return;
  }

  // pass the array index on stack because all registers must be preserved
  if (_index->is_cpu_register()) {
    ce->store_parameter(_index->as_register(), 0);
  } else {
    ce->store_parameter(_index->as_jint(), 0);
  }
  Runtime1::StubID stub_id;
  if (_throw_index_out_of_bounds_exception) {
    stub_id = Runtime1::throw_index_exception_id;
  } else {
    stub_id = Runtime1::throw_range_check_failed_id;
    ce->store_parameter(_array->as_pointer_register(), 1);
  }
  __ call(RuntimeAddress(Runtime1::entry_for(stub_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

PredicateFailedStub::PredicateFailedStub(CodeEmitInfo* info) {
  _info = new CodeEmitInfo(info);
}

void PredicateFailedStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  address a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
  __ call(RuntimeAddress(a));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}

void DivByZeroStub::emit_code(LIR_Assembler* ce) {
  if (_offset != -1) {
    ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  }
  __ bind(_entry);
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::throw_div0_exception_id)));
  ce->add_call_info_here(_info);
  debug_only(__ should_not_reach_here());
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
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  __ movptr(rdx, _klass_reg->as_register());
  __ call(RuntimeAddress(Runtime1::entry_for(_stub_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == rax, "result must in rax,");
  __ jmp(_continuation);
}


// Implementation of NewTypeArrayStub

NewTypeArrayStub::NewTypeArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _length = length;
  _result = result;
  _info = new CodeEmitInfo(info);
}


void NewTypeArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  assert(_length->as_register() == rbx, "length must in rbx,");
  assert(_klass_reg->as_register() == rdx, "klass_reg must in rdx");
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::new_type_array_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == rax, "result must in rax,");
  __ jmp(_continuation);
}


// Implementation of NewObjectArrayStub

NewObjectArrayStub::NewObjectArrayStub(LIR_Opr klass_reg, LIR_Opr length, LIR_Opr result, CodeEmitInfo* info) {
  _klass_reg = klass_reg;
  _result = result;
  _length = length;
  _info = new CodeEmitInfo(info);
}


void NewObjectArrayStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  assert(_length->as_register() == rbx, "length must in rbx,");
  assert(_klass_reg->as_register() == rdx, "klass_reg must in rdx");
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::new_object_array_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  assert(_result->as_register() == rax, "result must in rax,");
  __ jmp(_continuation);
}


// Implementation of MonitorAccessStubs

MonitorEnterStub::MonitorEnterStub(LIR_Opr obj_reg, LIR_Opr lock_reg, CodeEmitInfo* info)
: MonitorAccessStub(obj_reg, lock_reg)
{
  _info = new CodeEmitInfo(info);
}


void MonitorEnterStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");
  __ bind(_entry);
  ce->store_parameter(_obj_reg->as_register(),  1);
  ce->store_parameter(_lock_reg->as_register(), 0);
  Runtime1::StubID enter_id;
  if (ce->compilation()->has_fpu_code()) {
    enter_id = Runtime1::monitorenter_id;
  } else {
    enter_id = Runtime1::monitorenter_nofpu_id;
  }
  __ call(RuntimeAddress(Runtime1::entry_for(enter_id)));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  __ jmp(_continuation);
}


void MonitorExitStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  if (_compute_lock) {
    // lock_reg was destroyed by fast unlocking attempt => recompute it
    ce->monitor_address(_monitor_ix, _lock_reg);
  }
  ce->store_parameter(_lock_reg->as_register(), 0);
  // note: non-blocking leaf routine => no call info needed
  Runtime1::StubID exit_id;
  if (ce->compilation()->has_fpu_code()) {
    exit_id = Runtime1::monitorexit_id;
  } else {
    exit_id = Runtime1::monitorexit_nofpu_id;
  }
  __ call(RuntimeAddress(Runtime1::entry_for(exit_id)));
  __ jmp(_continuation);
}


// Implementation of patching:
// - Copy the code at given offset to an inlined buffer (first the bytes, then the number of bytes)
// - Replace original code with a call to the stub
// At Runtime:
// - call to stub, jump to runtime
// - in runtime: preserve all registers (rspecially objects, i.e., source and destination object)
// - in runtime: after initializing class, restore original code, reexecute instruction

int PatchingStub::_patch_info_offset = -NativeGeneralJump::instruction_size;

void PatchingStub::align_patch_site(MacroAssembler* masm) {
  // We're patching a 5-7 byte instruction on intel and we need to
  // make sure that we don't see a piece of the instruction.  It
  // appears mostly impossible on Intel to simply invalidate other
  // processors caches and since they may do aggressive prefetch it's
  // very hard to make a guess about what code might be in the icache.
  // Force the instruction to be double word aligned so that it
  // doesn't span a cache line.
  masm->align(align_up((int)NativeGeneralJump::instruction_size, wordSize));
}

void PatchingStub::emit_code(LIR_Assembler* ce) {
  assert(NativeCall::instruction_size <= _bytes_to_copy && _bytes_to_copy <= 0xFF, "not enough room for call");

  Label call_patch;

  // static field accesses have special semantics while the class
  // initializer is being run so we emit a test which can be used to
  // check that this code is being executed by the initializing
  // thread.
  address being_initialized_entry = __ pc();
  if (CommentedAssembly) {
    __ block_comment(" patch template");
  }
  if (_id == load_klass_id) {
    // produce a copy of the load klass instruction for use by the being initialized case
#ifdef ASSERT
    address start = __ pc();
#endif
    Metadata* o = NULL;
    __ mov_metadata(_obj, o);
#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      assert(a_byte == *start++, "should be the same code");
    }
#endif
  } else if (_id == load_mirror_id) {
    // produce a copy of the load mirror instruction for use by the being
    // initialized case
#ifdef ASSERT
    address start = __ pc();
#endif
    jobject o = NULL;
    __ movoop(_obj, o);
#ifdef ASSERT
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      assert(a_byte == *start++, "should be the same code");
    }
#endif
  } else {
    // make a copy the code which is going to be patched.
    for (int i = 0; i < _bytes_to_copy; i++) {
      address ptr = (address)(_pc_start + i);
      int a_byte = (*ptr) & 0xFF;
      __ emit_int8(a_byte);
      *ptr = 0x90; // make the site look like a nop
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
    Register tmp = rax;
    Register tmp2 = rbx;
    __ push(tmp);
    __ push(tmp2);
    // Load without verification to keep code size small. We need it because
    // begin_initialized_entry_offset has to fit in a byte. Also, we know it's not null.
    __ movptr(tmp2, Address(_obj, java_lang_Class::klass_offset()));
    __ get_thread(tmp);
    __ cmpptr(tmp, Address(tmp2, InstanceKlass::init_thread_offset()));
    __ pop(tmp2);
    __ pop(tmp);
    __ jcc(Assembler::notEqual, call_patch);

    // access_field patches may execute the patched code before it's
    // copied back into place so we need to jump back into the main
    // code of the nmethod to continue execution.
    __ jmp(_patch_site_continuation);

    // make sure this extra code gets skipped
    bytes_to_skip += __ offset() - offset;
  }
  if (CommentedAssembly) {
    __ block_comment("patch data encoded as movl");
  }
  // Now emit the patch record telling the runtime how to find the
  // pieces of the patch.  We only need 3 bytes but for readability of
  // the disassembly we make the data look like a movl reg, imm32,
  // which requires 5 bytes
  int sizeof_patch_record = 5;
  bytes_to_skip += sizeof_patch_record;

  // emit the offsets needed to find the code to patch
  int being_initialized_entry_offset = __ pc() - being_initialized_entry + sizeof_patch_record;

  __ emit_int8((unsigned char)0xB8);
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
    case load_klass_id:    target = Runtime1::entry_for(Runtime1::load_klass_patching_id); reloc_type = relocInfo::metadata_type; break;
    case load_mirror_id:   target = Runtime1::entry_for(Runtime1::load_mirror_patching_id); reloc_type = relocInfo::oop_type; break;
    case load_appendix_id:      target = Runtime1::entry_for(Runtime1::load_appendix_patching_id); reloc_type = relocInfo::oop_type; break;
    default: ShouldNotReachHere();
  }
  __ bind(call_patch);

  if (CommentedAssembly) {
    __ block_comment("patch entry point");
  }
  __ call(RuntimeAddress(target));
  assert(_patch_info_offset == (patch_info_pc - __ pc()), "must not change");
  ce->add_call_info_here(_info);
  int jmp_off = __ offset();
  __ jmp(_patch_site_entry);
  // Add enough nops so deoptimization can overwrite the jmp above with a call
  // and not destroy the world. We cannot use fat nops here, since the concurrent
  // code rewrite may transiently create the illegal instruction sequence.
  for (int j = __ offset() ; j < jmp_off + 5 ; j++ ) {
    __ nop();
  }
  if (_id == load_klass_id || _id == load_mirror_id || _id == load_appendix_id) {
    CodeSection* cs = __ code_section();
    RelocIterator iter(cs, (address)_pc_start, (address)(_pc_start + 1));
    relocInfo::change_reloc_info_for_address(&iter, (address) _pc_start, reloc_type, relocInfo::none);
  }
}


void DeoptimizeStub::emit_code(LIR_Assembler* ce) {
  __ bind(_entry);
  ce->store_parameter(_trap_request, 0);
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::deoptimize_id)));
  ce->add_call_info_here(_info);
  DEBUG_ONLY(__ should_not_reach_here());
}


void ImplicitNullCheckStub::emit_code(LIR_Assembler* ce) {
  address a;
  if (_info->deoptimize_on_exception()) {
    // Deoptimize, do not throw the exception, because it is probably wrong to do it here.
    a = Runtime1::entry_for(Runtime1::predicate_failed_trap_id);
  } else {
    a = Runtime1::entry_for(Runtime1::throw_null_pointer_exception_id);
  }

  ce->compilation()->implicit_exception_table()->append(_offset, __ offset());
  __ bind(_entry);
  __ call(RuntimeAddress(a));
  ce->add_call_info_here(_info);
  ce->verify_oop_map(_info);
  debug_only(__ should_not_reach_here());
}


void SimpleExceptionStub::emit_code(LIR_Assembler* ce) {
  assert(__ rsp_offset() == 0, "frame size should be fixed");

  __ bind(_entry);
  // pass the object on stack because all registers must be preserved
  if (_obj->is_cpu_register()) {
    ce->store_parameter(_obj->as_register(), 0);
  }
  __ call(RuntimeAddress(Runtime1::entry_for(_stub)));
  ce->add_call_info_here(_info);
  debug_only(__ should_not_reach_here());
}


void ArrayCopyStub::emit_code(LIR_Assembler* ce) {
  //---------------slow case: call to native-----------------
  __ bind(_entry);
  // Figure out where the args should go
  // This should really convert the IntrinsicID to the Method* and signature
  // but I don't know how to do that.
  //
  VMRegPair args[5];
  BasicType signature[5] = { T_OBJECT, T_INT, T_OBJECT, T_INT, T_INT};
  SharedRuntime::java_calling_convention(signature, args, 5);

  // push parameters
  // (src, src_pos, dest, destPos, length)
  Register r[5];
  r[0] = src()->as_register();
  r[1] = src_pos()->as_register();
  r[2] = dst()->as_register();
  r[3] = dst_pos()->as_register();
  r[4] = length()->as_register();

  // next registers will get stored on the stack
  for (int i = 0; i < 5 ; i++ ) {
    VMReg r_1 = args[i].first();
    if (r_1->is_stack()) {
      int st_off = r_1->reg2stack() * wordSize;
      __ movptr (Address(rsp, st_off), r[i]);
    } else {
      assert(r[i] == args[i].first()->as_Register(), "Wrong register for arg ");
    }
  }

  ce->align_call(lir_static_call);

  ce->emit_static_call_stub();
  if (ce->compilation()->bailed_out()) {
    return; // CodeCache is full
  }
  AddressLiteral resolve(SharedRuntime::get_resolve_static_call_stub(),
                         relocInfo::static_call_type);
  __ call(resolve);
  ce->add_call_info_here(info());

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ incrementl(ExternalAddress((address)&Runtime1::_arraycopy_slowcase_cnt));
  }
#endif

  __ jmp(_continuation);
}

#undef __
