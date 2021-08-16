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

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/templateTable.hpp"
#include "memory/universe.hpp"
#include "oops/cpCache.hpp"
#include "oops/klass.inline.hpp"
#include "oops/methodData.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.hpp"
#include "utilities/powerOfTwo.hpp"

#define __ _masm->

//----------------------------------------------------------------------------------------------------
// Address computation

// local variables
static inline Address iaddress(int n)            {
  return Address(Rlocals, Interpreter::local_offset_in_bytes(n));
}

static inline Address laddress(int n)            { return iaddress(n + 1); }
static inline Address haddress(int n)            { return iaddress(n + 0); }

static inline Address faddress(int n)            { return iaddress(n); }
static inline Address daddress(int n)            { return laddress(n); }
static inline Address aaddress(int n)            { return iaddress(n); }


void TemplateTable::get_local_base_addr(Register r, Register index) {
  __ sub(r, Rlocals, AsmOperand(index, lsl, Interpreter::logStackElementSize));
}

Address TemplateTable::load_iaddress(Register index, Register scratch) {
  return Address(Rlocals, index, lsl, Interpreter::logStackElementSize, basic_offset, sub_offset);
}

Address TemplateTable::load_aaddress(Register index, Register scratch) {
  return load_iaddress(index, scratch);
}

Address TemplateTable::load_faddress(Register index, Register scratch) {
#ifdef __SOFTFP__
  return load_iaddress(index, scratch);
#else
  get_local_base_addr(scratch, index);
  return Address(scratch);
#endif // __SOFTFP__
}

Address TemplateTable::load_daddress(Register index, Register scratch) {
  get_local_base_addr(scratch, index);
  return Address(scratch, Interpreter::local_offset_in_bytes(1));
}

// At top of Java expression stack which may be different than SP.
// It isn't for category 1 objects.
static inline Address at_tos() {
  return Address(Rstack_top, Interpreter::expr_offset_in_bytes(0));
}

static inline Address at_tos_p1() {
  return Address(Rstack_top, Interpreter::expr_offset_in_bytes(1));
}

static inline Address at_tos_p2() {
  return Address(Rstack_top, Interpreter::expr_offset_in_bytes(2));
}


// Loads double/long local into R0_tos_lo/R1_tos_hi with two
// separate ldr instructions (supports nonadjacent values).
// Used for longs in all modes, and for doubles in SOFTFP mode.
void TemplateTable::load_category2_local(Register Rlocal_index, Register tmp) {
  const Register Rlocal_base = tmp;
  assert_different_registers(Rlocal_index, tmp);

  get_local_base_addr(Rlocal_base, Rlocal_index);
  __ ldr(R0_tos_lo, Address(Rlocal_base, Interpreter::local_offset_in_bytes(1)));
  __ ldr(R1_tos_hi, Address(Rlocal_base, Interpreter::local_offset_in_bytes(0)));
}


// Stores R0_tos_lo/R1_tos_hi to double/long local with two
// separate str instructions (supports nonadjacent values).
// Used for longs in all modes, and for doubles in SOFTFP mode
void TemplateTable::store_category2_local(Register Rlocal_index, Register tmp) {
  const Register Rlocal_base = tmp;
  assert_different_registers(Rlocal_index, tmp);

  get_local_base_addr(Rlocal_base, Rlocal_index);
  __ str(R0_tos_lo, Address(Rlocal_base, Interpreter::local_offset_in_bytes(1)));
  __ str(R1_tos_hi, Address(Rlocal_base, Interpreter::local_offset_in_bytes(0)));
}

// Returns address of Java array element using temp register as address base.
Address TemplateTable::get_array_elem_addr(BasicType elemType, Register array, Register index, Register temp) {
  int logElemSize = exact_log2(type2aelembytes(elemType));
  __ add_ptr_scaled_int32(temp, array, index, logElemSize);
  return Address(temp, arrayOopDesc::base_offset_in_bytes(elemType));
}

// Returns address of Java array element using temp register as offset from array base
Address TemplateTable::get_array_elem_addr_same_base(BasicType elemType, Register array, Register index, Register temp) {
  int logElemSize = exact_log2(type2aelembytes(elemType));
  if (logElemSize == 0) {
    __ add(temp, index, arrayOopDesc::base_offset_in_bytes(elemType));
  } else {
    __ mov(temp, arrayOopDesc::base_offset_in_bytes(elemType));
    __ add_ptr_scaled_int32(temp, temp, index, logElemSize);
  }
  return Address(array, temp);
}

//----------------------------------------------------------------------------------------------------
// Condition conversion
AsmCondition convNegCond(TemplateTable::Condition cc) {
  switch (cc) {
    case TemplateTable::equal        : return ne;
    case TemplateTable::not_equal    : return eq;
    case TemplateTable::less         : return ge;
    case TemplateTable::less_equal   : return gt;
    case TemplateTable::greater      : return le;
    case TemplateTable::greater_equal: return lt;
  }
  ShouldNotReachHere();
  return nv;
}

//----------------------------------------------------------------------------------------------------
// Miscelaneous helper routines

// Store an oop (or NULL) at the address described by obj.
// Blows all volatile registers R0-R3, Rtemp, LR).
// Also destroys new_val and obj.base().
static void do_oop_store(InterpreterMacroAssembler* _masm,
                         Address obj,
                         Register new_val,
                         Register tmp1,
                         Register tmp2,
                         Register tmp3,
                         bool is_null,
                         DecoratorSet decorators = 0) {

  assert_different_registers(obj.base(), new_val, tmp1, tmp2, tmp3, noreg);
  if (is_null) {
    __ store_heap_oop_null(obj, new_val, tmp1, tmp2, tmp3, decorators);
  } else {
    __ store_heap_oop(obj, new_val, tmp1, tmp2, tmp3, decorators);
  }
}

static void do_oop_load(InterpreterMacroAssembler* _masm,
                        Register dst,
                        Address obj,
                        DecoratorSet decorators = 0) {
  __ load_heap_oop(dst, obj, noreg, noreg, noreg, decorators);
}

Address TemplateTable::at_bcp(int offset) {
  assert(_desc->uses_bcp(), "inconsistent uses_bcp information");
  return Address(Rbcp, offset);
}


// Blows volatile registers R0-R3, Rtemp, LR.
void TemplateTable::patch_bytecode(Bytecodes::Code bc, Register bc_reg,
                                   Register temp_reg, bool load_bc_into_bc_reg/*=true*/,
                                   int byte_no) {
  assert_different_registers(bc_reg, temp_reg);
  if (!RewriteBytecodes)  return;
  Label L_patch_done;

  switch (bc) {
  case Bytecodes::_fast_aputfield:
  case Bytecodes::_fast_bputfield:
  case Bytecodes::_fast_zputfield:
  case Bytecodes::_fast_cputfield:
  case Bytecodes::_fast_dputfield:
  case Bytecodes::_fast_fputfield:
  case Bytecodes::_fast_iputfield:
  case Bytecodes::_fast_lputfield:
  case Bytecodes::_fast_sputfield:
    {
      // We skip bytecode quickening for putfield instructions when
      // the put_code written to the constant pool cache is zero.
      // This is required so that every execution of this instruction
      // calls out to InterpreterRuntime::resolve_get_put to do
      // additional, required work.
      assert(byte_no == f1_byte || byte_no == f2_byte, "byte_no out of range");
      assert(load_bc_into_bc_reg, "we use bc_reg as temp");
      __ get_cache_and_index_and_bytecode_at_bcp(bc_reg, temp_reg, temp_reg, byte_no, 1, sizeof(u2));
      __ mov(bc_reg, bc);
      __ cbz(temp_reg, L_patch_done);  // test if bytecode is zero
    }
    break;
  default:
    assert(byte_no == -1, "sanity");
    // the pair bytecodes have already done the load.
    if (load_bc_into_bc_reg) {
      __ mov(bc_reg, bc);
    }
  }

  if (__ can_post_breakpoint()) {
    Label L_fast_patch;
    // if a breakpoint is present we can't rewrite the stream directly
    __ ldrb(temp_reg, at_bcp(0));
    __ cmp(temp_reg, Bytecodes::_breakpoint);
    __ b(L_fast_patch, ne);
    if (bc_reg != R3) {
      __ mov(R3, bc_reg);
    }
    __ mov(R1, Rmethod);
    __ mov(R2, Rbcp);
    // Let breakpoint table handling rewrite to quicker bytecode
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::set_original_bytecode_at), R1, R2, R3);
    __ b(L_patch_done);
    __ bind(L_fast_patch);
  }

#ifdef ASSERT
  Label L_okay;
  __ ldrb(temp_reg, at_bcp(0));
  __ cmp(temp_reg, (int)Bytecodes::java_code(bc));
  __ b(L_okay, eq);
  __ cmp(temp_reg, bc_reg);
  __ b(L_okay, eq);
  __ stop("patching the wrong bytecode");
  __ bind(L_okay);
#endif

  // patch bytecode
  __ strb(bc_reg, at_bcp(0));
  __ bind(L_patch_done);
}

//----------------------------------------------------------------------------------------------------
// Individual instructions

void TemplateTable::nop() {
  transition(vtos, vtos);
  // nothing to do
}

void TemplateTable::shouldnotreachhere() {
  transition(vtos, vtos);
  __ stop("shouldnotreachhere bytecode");
}



void TemplateTable::aconst_null() {
  transition(vtos, atos);
  __ mov(R0_tos, 0);
}


void TemplateTable::iconst(int value) {
  transition(vtos, itos);
  __ mov_slow(R0_tos, value);
}


void TemplateTable::lconst(int value) {
  transition(vtos, ltos);
  assert((value == 0) || (value == 1), "unexpected long constant");
  __ mov(R0_tos, value);
  __ mov(R1_tos_hi, 0);
}


void TemplateTable::fconst(int value) {
  transition(vtos, ftos);
  const int zero = 0;         // 0.0f
  const int one = 0x3f800000; // 1.0f
  const int two = 0x40000000; // 2.0f

  switch(value) {
  case 0:   __ mov(R0_tos, zero);   break;
  case 1:   __ mov(R0_tos, one);    break;
  case 2:   __ mov(R0_tos, two);    break;
  default:  ShouldNotReachHere();   break;
  }

#ifndef __SOFTFP__
  __ fmsr(S0_tos, R0_tos);
#endif // !__SOFTFP__
}


void TemplateTable::dconst(int value) {
  transition(vtos, dtos);
  const int one_lo = 0;            // low part of 1.0
  const int one_hi = 0x3ff00000;   // high part of 1.0

  if (value == 0) {
#ifdef __SOFTFP__
    __ mov(R0_tos_lo, 0);
    __ mov(R1_tos_hi, 0);
#else
    __ mov(R0_tmp, 0);
    __ fmdrr(D0_tos, R0_tmp, R0_tmp);
#endif // __SOFTFP__
  } else if (value == 1) {
    __ mov(R0_tos_lo, one_lo);
    __ mov_slow(R1_tos_hi, one_hi);
#ifndef __SOFTFP__
    __ fmdrr(D0_tos, R0_tos_lo, R1_tos_hi);
#endif // !__SOFTFP__
  } else {
    ShouldNotReachHere();
  }
}


void TemplateTable::bipush() {
  transition(vtos, itos);
  __ ldrsb(R0_tos, at_bcp(1));
}


void TemplateTable::sipush() {
  transition(vtos, itos);
  __ ldrsb(R0_tmp, at_bcp(1));
  __ ldrb(R1_tmp, at_bcp(2));
  __ orr(R0_tos, R1_tmp, AsmOperand(R0_tmp, lsl, BitsPerByte));
}


void TemplateTable::ldc(bool wide) {
  transition(vtos, vtos);
  Label fastCase, Condy, Done;

  const Register Rindex = R1_tmp;
  const Register Rcpool = R2_tmp;
  const Register Rtags  = R3_tmp;
  const Register RtagType = R3_tmp;

  if (wide) {
    __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);
  } else {
    __ ldrb(Rindex, at_bcp(1));
  }
  __ get_cpool_and_tags(Rcpool, Rtags);

  const int base_offset = ConstantPool::header_size() * wordSize;
  const int tags_offset = Array<u1>::base_offset_in_bytes();

  // get const type
  __ add(Rtemp, Rtags, tags_offset);
  __ ldrb(RtagType, Address(Rtemp, Rindex));
  volatile_barrier(MacroAssembler::LoadLoad, Rtemp);

  // unresolved class - get the resolved class
  __ cmp(RtagType, JVM_CONSTANT_UnresolvedClass);

  // unresolved class in error (resolution failed) - call into runtime
  // so that the same error from first resolution attempt is thrown.
  __ cond_cmp(RtagType, JVM_CONSTANT_UnresolvedClassInError, ne);

  // resolved class - need to call vm to get java mirror of the class
  __ cond_cmp(RtagType, JVM_CONSTANT_Class, ne);

  __ b(fastCase, ne);

  // slow case - call runtime
  __ mov(R1, wide);
  call_VM(R0_tos, CAST_FROM_FN_PTR(address, InterpreterRuntime::ldc), R1);
  __ push(atos);
  __ b(Done);

  // int, float, String
  __ bind(fastCase);

  __ cmp(RtagType, JVM_CONSTANT_Integer);
  __ cond_cmp(RtagType, JVM_CONSTANT_Float, ne);
  __ b(Condy, ne);

  // itos, ftos
  __ add(Rtemp, Rcpool, AsmOperand(Rindex, lsl, LogBytesPerWord));
  __ ldr_u32(R0_tos, Address(Rtemp, base_offset));

  // floats and ints are placed on stack in the same way, so
  // we can use push(itos) to transfer float value without VFP
  __ push(itos);
  __ b(Done);

  __ bind(Condy);
  condy_helper(Done);

  __ bind(Done);
}

// Fast path for caching oop constants.
void TemplateTable::fast_aldc(bool wide) {
  transition(vtos, atos);
  int index_size = wide ? sizeof(u2) : sizeof(u1);
  Label resolved;

  // We are resolved if the resolved reference cache entry contains a
  // non-null object (CallSite, etc.)
  assert_different_registers(R0_tos, R2_tmp);
  __ get_index_at_bcp(R2_tmp, 1, R0_tos, index_size);
  __ load_resolved_reference_at_index(R0_tos, R2_tmp);
  __ cbnz(R0_tos, resolved);

  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc);

  // first time invocation - must resolve first
  __ mov(R1, (int)bytecode());
  __ call_VM(R0_tos, entry, R1);
  __ bind(resolved);

  { // Check for the null sentinel.
    // If we just called the VM, that already did the mapping for us,
    // but it's harmless to retry.
    Label notNull;
    Register result = R0;
    Register tmp = R1;
    Register rarg = R2;

    // Stash null_sentinel address to get its value later
    __ mov_slow(rarg, (uintptr_t)Universe::the_null_sentinel_addr());
    __ ldr(tmp, Address(rarg));
    __ resolve_oop_handle(tmp);
    __ cmp(result, tmp);
    __ b(notNull, ne);
    __ mov(result, 0);  // NULL object reference
    __ bind(notNull);
  }

  if (VerifyOops) {
    __ verify_oop(R0_tos);
  }
}

void TemplateTable::ldc2_w() {
  transition(vtos, vtos);
  const Register Rtags  = R2_tmp;
  const Register Rindex = R3_tmp;
  const Register Rcpool = R4_tmp;
  const Register Rbase  = R5_tmp;

  __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);

  __ get_cpool_and_tags(Rcpool, Rtags);
  const int base_offset = ConstantPool::header_size() * wordSize;
  const int tags_offset = Array<u1>::base_offset_in_bytes();

  __ add(Rbase, Rcpool, AsmOperand(Rindex, lsl, LogBytesPerWord));

  // get type from tags
  __ add(Rtemp, Rtags, tags_offset);
  __ ldrb(Rtemp, Address(Rtemp, Rindex));

  Label Done, NotLong, NotDouble;
  __ cmp(Rtemp, JVM_CONSTANT_Double);
  __ b(NotDouble, ne);
#ifdef __SOFTFP__
  __ ldr(R0_tos_lo, Address(Rbase, base_offset + 0 * wordSize));
  __ ldr(R1_tos_hi, Address(Rbase, base_offset + 1 * wordSize));
#else // !__SOFTFP__
  __ ldr_double(D0_tos, Address(Rbase, base_offset));
#endif // __SOFTFP__
  __ push(dtos);
  __ b(Done);
  __ bind(NotDouble);

  __ cmp(Rtemp, JVM_CONSTANT_Long);
  __ b(NotLong, ne);
  __ ldr(R0_tos_lo, Address(Rbase, base_offset + 0 * wordSize));
  __ ldr(R1_tos_hi, Address(Rbase, base_offset + 1 * wordSize));
  __ push(ltos);
  __ b(Done);
  __ bind(NotLong);

  condy_helper(Done);

  __ bind(Done);
}


void TemplateTable::condy_helper(Label& Done)
{
  Register obj   = R0_tmp;
  Register rtmp  = R1_tmp;
  Register flags = R2_tmp;
  Register off   = R3_tmp;

  __ mov(rtmp, (int) bytecode());
  __ call_VM(obj, CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc), rtmp);
  __ get_vm_result_2(flags, rtmp);

  // VMr = obj = base address to find primitive value to push
  // VMr2 = flags = (tos, off) using format of CPCE::_flags
  __ mov(off, flags);

  __ logical_shift_left( off, off, 32 - ConstantPoolCacheEntry::field_index_bits);
  __ logical_shift_right(off, off, 32 - ConstantPoolCacheEntry::field_index_bits);

  const Address field(obj, off);

  __ logical_shift_right(flags, flags, ConstantPoolCacheEntry::tos_state_shift);
  // Make sure we don't need to mask flags after the above shift
  ConstantPoolCacheEntry::verify_tos_state_shift();

  switch (bytecode()) {
    case Bytecodes::_ldc:
    case Bytecodes::_ldc_w:
      {
        // tos in (itos, ftos, stos, btos, ctos, ztos)
        Label notIntFloat, notShort, notByte, notChar, notBool;
        __ cmp(flags, itos);
        __ cond_cmp(flags, ftos, ne);
        __ b(notIntFloat, ne);
        __ ldr(R0_tos, field);
        __ push(itos);
        __ b(Done);

        __ bind(notIntFloat);
        __ cmp(flags, stos);
        __ b(notShort, ne);
        __ ldrsh(R0_tos, field);
        __ push(stos);
        __ b(Done);

        __ bind(notShort);
        __ cmp(flags, btos);
        __ b(notByte, ne);
        __ ldrsb(R0_tos, field);
        __ push(btos);
        __ b(Done);

        __ bind(notByte);
        __ cmp(flags, ctos);
        __ b(notChar, ne);
        __ ldrh(R0_tos, field);
        __ push(ctos);
        __ b(Done);

        __ bind(notChar);
        __ cmp(flags, ztos);
        __ b(notBool, ne);
        __ ldrsb(R0_tos, field);
        __ push(ztos);
        __ b(Done);

        __ bind(notBool);
        break;
      }

    case Bytecodes::_ldc2_w:
      {
        Label notLongDouble;
        __ cmp(flags, ltos);
        __ cond_cmp(flags, dtos, ne);
        __ b(notLongDouble, ne);

        __ add(rtmp, obj, wordSize);
        __ ldr(R0_tos_lo, Address(obj, off));
        __ ldr(R1_tos_hi, Address(rtmp, off));
        __ push(ltos);
        __ b(Done);

        __ bind(notLongDouble);

        break;
      }

    default:
      ShouldNotReachHere();
    }

    __ stop("bad ldc/condy");
}


void TemplateTable::locals_index(Register reg, int offset) {
  __ ldrb(reg, at_bcp(offset));
}

void TemplateTable::iload() {
  iload_internal();
}

void TemplateTable::nofast_iload() {
  iload_internal(may_not_rewrite);
}

void TemplateTable::iload_internal(RewriteControl rc) {
  transition(vtos, itos);

  if ((rc == may_rewrite) && __ rewrite_frequent_pairs()) {
    Label rewrite, done;
    const Register next_bytecode = R1_tmp;
    const Register target_bytecode = R2_tmp;

    // get next byte
    __ ldrb(next_bytecode, at_bcp(Bytecodes::length_for(Bytecodes::_iload)));
    // if _iload, wait to rewrite to iload2.  We only want to rewrite the
    // last two iloads in a pair.  Comparing against fast_iload means that
    // the next bytecode is neither an iload or a caload, and therefore
    // an iload pair.
    __ cmp(next_bytecode, Bytecodes::_iload);
    __ b(done, eq);

    __ cmp(next_bytecode, Bytecodes::_fast_iload);
    __ mov(target_bytecode, Bytecodes::_fast_iload2);
    __ b(rewrite, eq);

    // if _caload, rewrite to fast_icaload
    __ cmp(next_bytecode, Bytecodes::_caload);
    __ mov(target_bytecode, Bytecodes::_fast_icaload);
    __ b(rewrite, eq);

    // rewrite so iload doesn't check again.
    __ mov(target_bytecode, Bytecodes::_fast_iload);

    // rewrite
    // R2: fast bytecode
    __ bind(rewrite);
    patch_bytecode(Bytecodes::_iload, target_bytecode, Rtemp, false);
    __ bind(done);
  }

  // Get the local value into tos
  const Register Rlocal_index = R1_tmp;
  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(R0_tos, local);
}


void TemplateTable::fast_iload2() {
  transition(vtos, itos);
  const Register Rlocal_index = R1_tmp;

  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(R0_tos, local);
  __ push(itos);

  locals_index(Rlocal_index, 3);
  local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(R0_tos, local);
}

void TemplateTable::fast_iload() {
  transition(vtos, itos);
  const Register Rlocal_index = R1_tmp;

  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(R0_tos, local);
}


void TemplateTable::lload() {
  transition(vtos, ltos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);
  load_category2_local(Rlocal_index, R3_tmp);
}


void TemplateTable::fload() {
  transition(vtos, ftos);
  const Register Rlocal_index = R2_tmp;

  // Get the local value into tos
  locals_index(Rlocal_index);
  Address local = load_faddress(Rlocal_index, Rtemp);
#ifdef __SOFTFP__
  __ ldr(R0_tos, local);
#else
  __ ldr_float(S0_tos, local);
#endif // __SOFTFP__
}


void TemplateTable::dload() {
  transition(vtos, dtos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);

#ifdef __SOFTFP__
  load_category2_local(Rlocal_index, R3_tmp);
#else
  __ ldr_double(D0_tos, load_daddress(Rlocal_index, Rtemp));
#endif // __SOFTFP__
}


void TemplateTable::aload() {
  transition(vtos, atos);
  const Register Rlocal_index = R1_tmp;

  locals_index(Rlocal_index);
  Address local = load_aaddress(Rlocal_index, Rtemp);
  __ ldr(R0_tos, local);
}


void TemplateTable::locals_index_wide(Register reg) {
  assert_different_registers(reg, Rtemp);
  __ ldrb(Rtemp, at_bcp(2));
  __ ldrb(reg, at_bcp(3));
  __ orr(reg, reg, AsmOperand(Rtemp, lsl, 8));
}


void TemplateTable::wide_iload() {
  transition(vtos, itos);
  const Register Rlocal_index = R2_tmp;

  locals_index_wide(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(R0_tos, local);
}


void TemplateTable::wide_lload() {
  transition(vtos, ltos);
  const Register Rlocal_index = R2_tmp;
  const Register Rlocal_base = R3_tmp;

  locals_index_wide(Rlocal_index);
  load_category2_local(Rlocal_index, R3_tmp);
}


void TemplateTable::wide_fload() {
  transition(vtos, ftos);
  const Register Rlocal_index = R2_tmp;

  locals_index_wide(Rlocal_index);
  Address local = load_faddress(Rlocal_index, Rtemp);
#ifdef __SOFTFP__
  __ ldr(R0_tos, local);
#else
  __ ldr_float(S0_tos, local);
#endif // __SOFTFP__
}


void TemplateTable::wide_dload() {
  transition(vtos, dtos);
  const Register Rlocal_index = R2_tmp;

  locals_index_wide(Rlocal_index);
#ifdef __SOFTFP__
  load_category2_local(Rlocal_index, R3_tmp);
#else
  __ ldr_double(D0_tos, load_daddress(Rlocal_index, Rtemp));
#endif // __SOFTFP__
}


void TemplateTable::wide_aload() {
  transition(vtos, atos);
  const Register Rlocal_index = R2_tmp;

  locals_index_wide(Rlocal_index);
  Address local = load_aaddress(Rlocal_index, Rtemp);
  __ ldr(R0_tos, local);
}

void TemplateTable::index_check(Register array, Register index) {
  // Pop ptr into array
  __ pop_ptr(array);
  index_check_without_pop(array, index);
}

void TemplateTable::index_check_without_pop(Register array, Register index) {
  assert_different_registers(array, index, Rtemp);
  // check array
  __ null_check(array, Rtemp, arrayOopDesc::length_offset_in_bytes());
  // check index
  __ ldr_s32(Rtemp, Address(array, arrayOopDesc::length_offset_in_bytes()));
  __ cmp_32(index, Rtemp);
  if (index != R4_ArrayIndexOutOfBounds_index) {
    // convention with generate_ArrayIndexOutOfBounds_handler()
    __ mov(R4_ArrayIndexOutOfBounds_index, index, hs);
  }
  __ mov(R1, array, hs);
  __ b(Interpreter::_throw_ArrayIndexOutOfBoundsException_entry, hs);
}


void TemplateTable::iaload() {
  transition(itos, itos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_INT, Rarray, Rindex, Rtemp);
  __ access_load_at(T_INT, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg);
}


void TemplateTable::laload() {
  transition(itos, ltos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);

  Address addr = get_array_elem_addr_same_base(T_LONG, Rarray, Rindex, Rtemp);
  __ access_load_at(T_LONG, IN_HEAP | IS_ARRAY, addr, noreg /* ltos */, noreg, noreg, noreg);
}


void TemplateTable::faload() {
  transition(itos, ftos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);

  Address addr = get_array_elem_addr_same_base(T_FLOAT, Rarray, Rindex, Rtemp);
  __ access_load_at(T_FLOAT, IN_HEAP | IS_ARRAY, addr, noreg /* ftos */, noreg, noreg, noreg);
}


void TemplateTable::daload() {
  transition(itos, dtos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);

  Address addr = get_array_elem_addr_same_base(T_DOUBLE, Rarray, Rindex, Rtemp);
  __ access_load_at(T_DOUBLE, IN_HEAP | IS_ARRAY, addr, noreg /* dtos */, noreg, noreg, noreg);
}


void TemplateTable::aaload() {
  transition(itos, atos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);
  do_oop_load(_masm, R0_tos, get_array_elem_addr_same_base(T_OBJECT, Rarray, Rindex, Rtemp), IS_ARRAY);
}


void TemplateTable::baload() {
  transition(itos, itos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_BYTE, Rarray, Rindex, Rtemp);
  __ access_load_at(T_BYTE, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg);
}


void TemplateTable::caload() {
  transition(itos, itos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_CHAR, Rarray, Rindex, Rtemp);
  __ access_load_at(T_CHAR, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg);
}


// iload followed by caload frequent pair
void TemplateTable::fast_icaload() {
  transition(vtos, itos);
  const Register Rlocal_index = R1_tmp;
  const Register Rarray = R1_tmp;
  const Register Rindex = R4_tmp; // index_check prefers index on R4
  assert_different_registers(Rlocal_index, Rindex);
  assert_different_registers(Rarray, Rindex);

  // load index out of locals
  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(Rindex, local);

  // get array element
  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_CHAR, Rarray, Rindex, Rtemp);
  __ access_load_at(T_CHAR, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg);
}


void TemplateTable::saload() {
  transition(itos, itos);
  const Register Rarray = R1_tmp;
  const Register Rindex = R0_tos;

  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_SHORT, Rarray, Rindex, Rtemp);
  __ access_load_at(T_SHORT, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg);
}


void TemplateTable::iload(int n) {
  transition(vtos, itos);
  __ ldr_s32(R0_tos, iaddress(n));
}


void TemplateTable::lload(int n) {
  transition(vtos, ltos);
  __ ldr(R0_tos_lo, laddress(n));
  __ ldr(R1_tos_hi, haddress(n));
}


void TemplateTable::fload(int n) {
  transition(vtos, ftos);
#ifdef __SOFTFP__
  __ ldr(R0_tos, faddress(n));
#else
  __ ldr_float(S0_tos, faddress(n));
#endif // __SOFTFP__
}


void TemplateTable::dload(int n) {
  transition(vtos, dtos);
#ifdef __SOFTFP__
  __ ldr(R0_tos_lo, laddress(n));
  __ ldr(R1_tos_hi, haddress(n));
#else
  __ ldr_double(D0_tos, daddress(n));
#endif // __SOFTFP__
}


void TemplateTable::aload(int n) {
  transition(vtos, atos);
  __ ldr(R0_tos, aaddress(n));
}

void TemplateTable::aload_0() {
  aload_0_internal();
}

void TemplateTable::nofast_aload_0() {
  aload_0_internal(may_not_rewrite);
}

void TemplateTable::aload_0_internal(RewriteControl rc) {
  transition(vtos, atos);
  // According to bytecode histograms, the pairs:
  //
  // _aload_0, _fast_igetfield
  // _aload_0, _fast_agetfield
  // _aload_0, _fast_fgetfield
  //
  // occur frequently. If RewriteFrequentPairs is set, the (slow) _aload_0
  // bytecode checks if the next bytecode is either _fast_igetfield,
  // _fast_agetfield or _fast_fgetfield and then rewrites the
  // current bytecode into a pair bytecode; otherwise it rewrites the current
  // bytecode into _fast_aload_0 that doesn't do the pair check anymore.
  //
  // Note: If the next bytecode is _getfield, the rewrite must be delayed,
  //       otherwise we may miss an opportunity for a pair.
  //
  // Also rewrite frequent pairs
  //   aload_0, aload_1
  //   aload_0, iload_1
  // These bytecodes with a small amount of code are most profitable to rewrite
  if ((rc == may_rewrite) && __ rewrite_frequent_pairs()) {
    Label rewrite, done;
    const Register next_bytecode = R1_tmp;
    const Register target_bytecode = R2_tmp;

    // get next byte
    __ ldrb(next_bytecode, at_bcp(Bytecodes::length_for(Bytecodes::_aload_0)));

    // if _getfield then wait with rewrite
    __ cmp(next_bytecode, Bytecodes::_getfield);
    __ b(done, eq);

    // if _igetfield then rewrite to _fast_iaccess_0
    assert(Bytecodes::java_code(Bytecodes::_fast_iaccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ cmp(next_bytecode, Bytecodes::_fast_igetfield);
    __ mov(target_bytecode, Bytecodes::_fast_iaccess_0);
    __ b(rewrite, eq);

    // if _agetfield then rewrite to _fast_aaccess_0
    assert(Bytecodes::java_code(Bytecodes::_fast_aaccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ cmp(next_bytecode, Bytecodes::_fast_agetfield);
    __ mov(target_bytecode, Bytecodes::_fast_aaccess_0);
    __ b(rewrite, eq);

    // if _fgetfield then rewrite to _fast_faccess_0, else rewrite to _fast_aload0
    assert(Bytecodes::java_code(Bytecodes::_fast_faccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    assert(Bytecodes::java_code(Bytecodes::_fast_aload_0) == Bytecodes::_aload_0, "fix bytecode definition");

    __ cmp(next_bytecode, Bytecodes::_fast_fgetfield);
    __ mov(target_bytecode, Bytecodes::_fast_faccess_0, eq);
    __ mov(target_bytecode, Bytecodes::_fast_aload_0, ne);

    // rewrite
    __ bind(rewrite);
    patch_bytecode(Bytecodes::_aload_0, target_bytecode, Rtemp, false);

    __ bind(done);
  }

  aload(0);
}

void TemplateTable::istore() {
  transition(itos, vtos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ str_32(R0_tos, local);
}


void TemplateTable::lstore() {
  transition(ltos, vtos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);
  store_category2_local(Rlocal_index, R3_tmp);
}


void TemplateTable::fstore() {
  transition(ftos, vtos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);
  Address local = load_faddress(Rlocal_index, Rtemp);
#ifdef __SOFTFP__
  __ str(R0_tos, local);
#else
  __ str_float(S0_tos, local);
#endif // __SOFTFP__
}


void TemplateTable::dstore() {
  transition(dtos, vtos);
  const Register Rlocal_index = R2_tmp;

  locals_index(Rlocal_index);

#ifdef __SOFTFP__
  store_category2_local(Rlocal_index, R3_tmp);
#else
  __ str_double(D0_tos, load_daddress(Rlocal_index, Rtemp));
#endif // __SOFTFP__
}


void TemplateTable::astore() {
  transition(vtos, vtos);
  const Register Rlocal_index = R1_tmp;

  __ pop_ptr(R0_tos);
  locals_index(Rlocal_index);
  Address local = load_aaddress(Rlocal_index, Rtemp);
  __ str(R0_tos, local);
}


void TemplateTable::wide_istore() {
  transition(vtos, vtos);
  const Register Rlocal_index = R2_tmp;

  __ pop_i(R0_tos);
  locals_index_wide(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ str_32(R0_tos, local);
}


void TemplateTable::wide_lstore() {
  transition(vtos, vtos);
  const Register Rlocal_index = R2_tmp;
  const Register Rlocal_base = R3_tmp;

  __ pop_l(R0_tos_lo, R1_tos_hi);

  locals_index_wide(Rlocal_index);
  store_category2_local(Rlocal_index, R3_tmp);
}


void TemplateTable::wide_fstore() {
  wide_istore();
}


void TemplateTable::wide_dstore() {
  wide_lstore();
}


void TemplateTable::wide_astore() {
  transition(vtos, vtos);
  const Register Rlocal_index = R2_tmp;

  __ pop_ptr(R0_tos);
  locals_index_wide(Rlocal_index);
  Address local = load_aaddress(Rlocal_index, Rtemp);
  __ str(R0_tos, local);
}


void TemplateTable::iastore() {
  transition(itos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // R0_tos: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_INT, Rarray, Rindex, Rtemp);
  __ access_store_at(T_INT, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg, false);
}


void TemplateTable::lastore() {
  transition(ltos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // R0_tos_lo:R1_tos_hi: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);

  Address addr = get_array_elem_addr_same_base(T_LONG, Rarray, Rindex, Rtemp);
  __ access_store_at(T_LONG, IN_HEAP | IS_ARRAY, addr, noreg /* ltos */, noreg, noreg, noreg, false);
}


void TemplateTable::fastore() {
  transition(ftos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // S0_tos/R0_tos: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_FLOAT, Rarray, Rindex, Rtemp);
  __ access_store_at(T_FLOAT, IN_HEAP | IS_ARRAY, addr, noreg /* ftos */, noreg, noreg, noreg, false);
}


void TemplateTable::dastore() {
  transition(dtos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // D0_tos / R0_tos_lo:R1_to_hi: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);

  Address addr = get_array_elem_addr_same_base(T_DOUBLE, Rarray, Rindex, Rtemp);
  __ access_store_at(T_DOUBLE, IN_HEAP | IS_ARRAY, addr, noreg /* dtos */, noreg, noreg, noreg, false);
}


void TemplateTable::aastore() {
  transition(vtos, vtos);
  Label is_null, throw_array_store, done;

  const Register Raddr_1   = R1_tmp;
  const Register Rvalue_2  = R2_tmp;
  const Register Rarray_3  = R3_tmp;
  const Register Rindex_4  = R4_tmp;   // preferred by index_check_without_pop()
  const Register Rsub_5    = R5_tmp;
  const Register Rsuper_LR = LR_tmp;

  // stack: ..., array, index, value
  __ ldr(Rvalue_2, at_tos());     // Value
  __ ldr_s32(Rindex_4, at_tos_p1());  // Index
  __ ldr(Rarray_3, at_tos_p2());  // Array

  index_check_without_pop(Rarray_3, Rindex_4);

  // Compute the array base
  __ add(Raddr_1, Rarray_3, arrayOopDesc::base_offset_in_bytes(T_OBJECT));

  // do array store check - check for NULL value first
  __ cbz(Rvalue_2, is_null);

  // Load subklass
  __ load_klass(Rsub_5, Rvalue_2);
  // Load superklass
  __ load_klass(Rtemp, Rarray_3);
  __ ldr(Rsuper_LR, Address(Rtemp, ObjArrayKlass::element_klass_offset()));

  __ gen_subtype_check(Rsub_5, Rsuper_LR, throw_array_store, R0_tmp, R3_tmp);
  // Come here on success

  // Store value
  __ add(Raddr_1, Raddr_1, AsmOperand(Rindex_4, lsl, LogBytesPerHeapOop));

  // Now store using the appropriate barrier
  do_oop_store(_masm, Raddr_1, Rvalue_2, Rtemp, R0_tmp, R3_tmp, false, IS_ARRAY);
  __ b(done);

  __ bind(throw_array_store);

  // Come here on failure of subtype check
  __ profile_typecheck_failed(R0_tmp);

  // object is at TOS
  __ b(Interpreter::_throw_ArrayStoreException_entry);

  // Have a NULL in Rvalue_2, store NULL at array[index].
  __ bind(is_null);
  __ profile_null_seen(R0_tmp);

  // Store a NULL
  do_oop_store(_masm, Address::indexed_oop(Raddr_1, Rindex_4), Rvalue_2, Rtemp, R0_tmp, R3_tmp, true, IS_ARRAY);

  // Pop stack arguments
  __ bind(done);
  __ add(Rstack_top, Rstack_top, 3 * Interpreter::stackElementSize);
}


void TemplateTable::bastore() {
  transition(itos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // R0_tos: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);

  // Need to check whether array is boolean or byte
  // since both types share the bastore bytecode.
  __ load_klass(Rtemp, Rarray);
  __ ldr_u32(Rtemp, Address(Rtemp, Klass::layout_helper_offset()));
  Label L_skip;
  __ tst(Rtemp, Klass::layout_helper_boolean_diffbit());
  __ b(L_skip, eq);
  __ and_32(R0_tos, R0_tos, 1); // if it is a T_BOOLEAN array, mask the stored value to 0/1
  __ bind(L_skip);
  Address addr = get_array_elem_addr_same_base(T_BYTE, Rarray, Rindex, Rtemp);
  __ access_store_at(T_BYTE, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg, false);
}


void TemplateTable::castore() {
  transition(itos, vtos);
  const Register Rindex = R4_tmp; // index_check prefers index in R4
  const Register Rarray = R3_tmp;
  // R0_tos: value

  __ pop_i(Rindex);
  index_check(Rarray, Rindex);
  Address addr = get_array_elem_addr_same_base(T_CHAR, Rarray, Rindex, Rtemp);
  __ access_store_at(T_CHAR, IN_HEAP | IS_ARRAY, addr, R0_tos, noreg, noreg, noreg, false);
}


void TemplateTable::sastore() {
  assert(arrayOopDesc::base_offset_in_bytes(T_CHAR) ==
           arrayOopDesc::base_offset_in_bytes(T_SHORT),
         "base offsets for char and short should be equal");
  castore();
}


void TemplateTable::istore(int n) {
  transition(itos, vtos);
  __ str_32(R0_tos, iaddress(n));
}


void TemplateTable::lstore(int n) {
  transition(ltos, vtos);
  __ str(R0_tos_lo, laddress(n));
  __ str(R1_tos_hi, haddress(n));
}


void TemplateTable::fstore(int n) {
  transition(ftos, vtos);
#ifdef __SOFTFP__
  __ str(R0_tos, faddress(n));
#else
  __ str_float(S0_tos, faddress(n));
#endif // __SOFTFP__
}


void TemplateTable::dstore(int n) {
  transition(dtos, vtos);
#ifdef __SOFTFP__
  __ str(R0_tos_lo, laddress(n));
  __ str(R1_tos_hi, haddress(n));
#else
  __ str_double(D0_tos, daddress(n));
#endif // __SOFTFP__
}


void TemplateTable::astore(int n) {
  transition(vtos, vtos);
  __ pop_ptr(R0_tos);
  __ str(R0_tos, aaddress(n));
}


void TemplateTable::pop() {
  transition(vtos, vtos);
  __ add(Rstack_top, Rstack_top, Interpreter::stackElementSize);
}


void TemplateTable::pop2() {
  transition(vtos, vtos);
  __ add(Rstack_top, Rstack_top, 2*Interpreter::stackElementSize);
}


void TemplateTable::dup() {
  transition(vtos, vtos);
  // stack: ..., a
  __ load_ptr(0, R0_tmp);
  __ push_ptr(R0_tmp);
  // stack: ..., a, a
}


void TemplateTable::dup_x1() {
  transition(vtos, vtos);
  // stack: ..., a, b
  __ load_ptr(0, R0_tmp);  // load b
  __ load_ptr(1, R2_tmp);  // load a
  __ store_ptr(1, R0_tmp); // store b
  __ store_ptr(0, R2_tmp); // store a
  __ push_ptr(R0_tmp);     // push b
  // stack: ..., b, a, b
}


void TemplateTable::dup_x2() {
  transition(vtos, vtos);
  // stack: ..., a, b, c
  __ load_ptr(0, R0_tmp);   // load c
  __ load_ptr(1, R2_tmp);   // load b
  __ load_ptr(2, R4_tmp);   // load a

  __ push_ptr(R0_tmp);      // push c

  // stack: ..., a, b, c, c
  __ store_ptr(1, R2_tmp);  // store b
  __ store_ptr(2, R4_tmp);  // store a
  __ store_ptr(3, R0_tmp);  // store c
  // stack: ..., c, a, b, c
}


void TemplateTable::dup2() {
  transition(vtos, vtos);
  // stack: ..., a, b
  __ load_ptr(1, R0_tmp);  // load a
  __ push_ptr(R0_tmp);     // push a
  __ load_ptr(1, R0_tmp);  // load b
  __ push_ptr(R0_tmp);     // push b
  // stack: ..., a, b, a, b
}


void TemplateTable::dup2_x1() {
  transition(vtos, vtos);

  // stack: ..., a, b, c
  __ load_ptr(0, R4_tmp);  // load c
  __ load_ptr(1, R2_tmp);  // load b
  __ load_ptr(2, R0_tmp);  // load a

  __ push_ptr(R2_tmp);     // push b
  __ push_ptr(R4_tmp);     // push c

  // stack: ..., a, b, c, b, c

  __ store_ptr(2, R0_tmp);  // store a
  __ store_ptr(3, R4_tmp);  // store c
  __ store_ptr(4, R2_tmp);  // store b

  // stack: ..., b, c, a, b, c
}


void TemplateTable::dup2_x2() {
  transition(vtos, vtos);
  // stack: ..., a, b, c, d
  __ load_ptr(0, R0_tmp);  // load d
  __ load_ptr(1, R2_tmp);  // load c
  __ push_ptr(R2_tmp);     // push c
  __ push_ptr(R0_tmp);     // push d
  // stack: ..., a, b, c, d, c, d
  __ load_ptr(4, R4_tmp);  // load b
  __ store_ptr(4, R0_tmp); // store d in b
  __ store_ptr(2, R4_tmp); // store b in d
  // stack: ..., a, d, c, b, c, d
  __ load_ptr(5, R4_tmp);  // load a
  __ store_ptr(5, R2_tmp); // store c in a
  __ store_ptr(3, R4_tmp); // store a in c
  // stack: ..., c, d, a, b, c, d
}


void TemplateTable::swap() {
  transition(vtos, vtos);
  // stack: ..., a, b
  __ load_ptr(1, R0_tmp);  // load a
  __ load_ptr(0, R2_tmp);  // load b
  __ store_ptr(0, R0_tmp); // store a in b
  __ store_ptr(1, R2_tmp); // store b in a
  // stack: ..., b, a
}


void TemplateTable::iop2(Operation op) {
  transition(itos, itos);
  const Register arg1 = R1_tmp;
  const Register arg2 = R0_tos;

  __ pop_i(arg1);
  switch (op) {
    case add  : __ add_32 (R0_tos, arg1, arg2); break;
    case sub  : __ sub_32 (R0_tos, arg1, arg2); break;
    case mul  : __ mul_32 (R0_tos, arg1, arg2); break;
    case _and : __ and_32 (R0_tos, arg1, arg2); break;
    case _or  : __ orr_32 (R0_tos, arg1, arg2); break;
    case _xor : __ eor_32 (R0_tos, arg1, arg2); break;
    case shl  : __ andr(arg2, arg2, 0x1f); __ mov (R0_tos, AsmOperand(arg1, lsl, arg2)); break;
    case shr  : __ andr(arg2, arg2, 0x1f); __ mov (R0_tos, AsmOperand(arg1, asr, arg2)); break;
    case ushr : __ andr(arg2, arg2, 0x1f); __ mov (R0_tos, AsmOperand(arg1, lsr, arg2)); break;
    default   : ShouldNotReachHere();
  }
}


void TemplateTable::lop2(Operation op) {
  transition(ltos, ltos);
  const Register arg1_lo = R2_tmp;
  const Register arg1_hi = R3_tmp;
  const Register arg2_lo = R0_tos_lo;
  const Register arg2_hi = R1_tos_hi;

  __ pop_l(arg1_lo, arg1_hi);
  switch (op) {
    case add : __ adds(R0_tos_lo, arg1_lo, arg2_lo); __ adc (R1_tos_hi, arg1_hi, arg2_hi); break;
    case sub : __ subs(R0_tos_lo, arg1_lo, arg2_lo); __ sbc (R1_tos_hi, arg1_hi, arg2_hi); break;
    case _and: __ andr(R0_tos_lo, arg1_lo, arg2_lo); __ andr(R1_tos_hi, arg1_hi, arg2_hi); break;
    case _or : __ orr (R0_tos_lo, arg1_lo, arg2_lo); __ orr (R1_tos_hi, arg1_hi, arg2_hi); break;
    case _xor: __ eor (R0_tos_lo, arg1_lo, arg2_lo); __ eor (R1_tos_hi, arg1_hi, arg2_hi); break;
    default : ShouldNotReachHere();
  }
}


void TemplateTable::idiv() {
  transition(itos, itos);
  __ mov(R2, R0_tos);
  __ pop_i(R0);
  // R0 - dividend
  // R2 - divisor
  __ call(StubRoutines::Arm::idiv_irem_entry(), relocInfo::none);
  // R1 - result
  __ mov(R0_tos, R1);
}


void TemplateTable::irem() {
  transition(itos, itos);
  __ mov(R2, R0_tos);
  __ pop_i(R0);
  // R0 - dividend
  // R2 - divisor
  __ call(StubRoutines::Arm::idiv_irem_entry(), relocInfo::none);
  // R0 - remainder
}


void TemplateTable::lmul() {
  transition(ltos, ltos);
  const Register arg1_lo = R0_tos_lo;
  const Register arg1_hi = R1_tos_hi;
  const Register arg2_lo = R2_tmp;
  const Register arg2_hi = R3_tmp;

  __ pop_l(arg2_lo, arg2_hi);

  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::lmul), arg1_lo, arg1_hi, arg2_lo, arg2_hi);
}


void TemplateTable::ldiv() {
  transition(ltos, ltos);
  const Register x_lo = R2_tmp;
  const Register x_hi = R3_tmp;
  const Register y_lo = R0_tos_lo;
  const Register y_hi = R1_tos_hi;

  __ pop_l(x_lo, x_hi);

  // check if y = 0
  __ orrs(Rtemp, y_lo, y_hi);
  __ call(Interpreter::_throw_ArithmeticException_entry, relocInfo::none, eq);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::ldiv), y_lo, y_hi, x_lo, x_hi);
}


void TemplateTable::lrem() {
  transition(ltos, ltos);
  const Register x_lo = R2_tmp;
  const Register x_hi = R3_tmp;
  const Register y_lo = R0_tos_lo;
  const Register y_hi = R1_tos_hi;

  __ pop_l(x_lo, x_hi);

  // check if y = 0
  __ orrs(Rtemp, y_lo, y_hi);
  __ call(Interpreter::_throw_ArithmeticException_entry, relocInfo::none, eq);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::lrem), y_lo, y_hi, x_lo, x_hi);
}


void TemplateTable::lshl() {
  transition(itos, ltos);
  const Register shift_cnt = R4_tmp;
  const Register val_lo = R2_tmp;
  const Register val_hi = R3_tmp;

  __ pop_l(val_lo, val_hi);
  __ andr(shift_cnt, R0_tos, 63);
  __ long_shift(R0_tos_lo, R1_tos_hi, val_lo, val_hi, lsl, shift_cnt);
}


void TemplateTable::lshr() {
  transition(itos, ltos);
  const Register shift_cnt = R4_tmp;
  const Register val_lo = R2_tmp;
  const Register val_hi = R3_tmp;

  __ pop_l(val_lo, val_hi);
  __ andr(shift_cnt, R0_tos, 63);
  __ long_shift(R0_tos_lo, R1_tos_hi, val_lo, val_hi, asr, shift_cnt);
}


void TemplateTable::lushr() {
  transition(itos, ltos);
  const Register shift_cnt = R4_tmp;
  const Register val_lo = R2_tmp;
  const Register val_hi = R3_tmp;

  __ pop_l(val_lo, val_hi);
  __ andr(shift_cnt, R0_tos, 63);
  __ long_shift(R0_tos_lo, R1_tos_hi, val_lo, val_hi, lsr, shift_cnt);
}


void TemplateTable::fop2(Operation op) {
  transition(ftos, ftos);
#ifdef __SOFTFP__
  __ mov(R1, R0_tos);
  __ pop_i(R0);
  switch (op) {
    case add: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_fadd_glibc), R0, R1); break;
    case sub: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_fsub_glibc), R0, R1); break;
    case mul: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_fmul), R0, R1); break;
    case div: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_fdiv), R0, R1); break;
    case rem: __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::frem), R0, R1); break;
    default : ShouldNotReachHere();
  }
#else
  const FloatRegister arg1 = S1_tmp;
  const FloatRegister arg2 = S0_tos;

  switch (op) {
    case add: __ pop_f(arg1); __ add_float(S0_tos, arg1, arg2); break;
    case sub: __ pop_f(arg1); __ sub_float(S0_tos, arg1, arg2); break;
    case mul: __ pop_f(arg1); __ mul_float(S0_tos, arg1, arg2); break;
    case div: __ pop_f(arg1); __ div_float(S0_tos, arg1, arg2); break;
    case rem:
#ifndef __ABI_HARD__
      __ pop_f(arg1);
      __ fmrs(R0, arg1);
      __ fmrs(R1, arg2);
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::frem), R0, R1);
      __ fmsr(S0_tos, R0);
#else
      __ mov_float(S1_reg, arg2);
      __ pop_f(S0);
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::frem));
#endif // !__ABI_HARD__
      break;
    default : ShouldNotReachHere();
  }
#endif // __SOFTFP__
}


void TemplateTable::dop2(Operation op) {
  transition(dtos, dtos);
#ifdef __SOFTFP__
  __ mov(R2, R0_tos_lo);
  __ mov(R3, R1_tos_hi);
  __ pop_l(R0, R1);
  switch (op) {
    // __aeabi_XXXX_glibc: Imported code from glibc soft-fp bundle for calculation accuracy improvement. See CR 6757269.
    case add: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_dadd_glibc), R0, R1, R2, R3); break;
    case sub: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_dsub_glibc), R0, R1, R2, R3); break;
    case mul: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_dmul), R0, R1, R2, R3); break;
    case div: __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_ddiv), R0, R1, R2, R3); break;
    case rem: __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::drem), R0, R1, R2, R3); break;
    default : ShouldNotReachHere();
  }
#else
  const FloatRegister arg1 = D1_tmp;
  const FloatRegister arg2 = D0_tos;

  switch (op) {
    case add: __ pop_d(arg1); __ add_double(D0_tos, arg1, arg2); break;
    case sub: __ pop_d(arg1); __ sub_double(D0_tos, arg1, arg2); break;
    case mul: __ pop_d(arg1); __ mul_double(D0_tos, arg1, arg2); break;
    case div: __ pop_d(arg1); __ div_double(D0_tos, arg1, arg2); break;
    case rem:
#ifndef __ABI_HARD__
      __ pop_d(arg1);
      __ fmrrd(R0, R1, arg1);
      __ fmrrd(R2, R3, arg2);
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::drem), R0, R1, R2, R3);
      __ fmdrr(D0_tos, R0, R1);
#else
      __ mov_double(D1, arg2);
      __ pop_d(D0);
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::drem));
#endif // !__ABI_HARD__
      break;
    default : ShouldNotReachHere();
  }
#endif // __SOFTFP__
}


void TemplateTable::ineg() {
  transition(itos, itos);
  __ neg_32(R0_tos, R0_tos);
}


void TemplateTable::lneg() {
  transition(ltos, ltos);
  __ rsbs(R0_tos_lo, R0_tos_lo, 0);
  __ rsc (R1_tos_hi, R1_tos_hi, 0);
}


void TemplateTable::fneg() {
  transition(ftos, ftos);
#ifdef __SOFTFP__
  // Invert sign bit
  const int sign_mask = 0x80000000;
  __ eor(R0_tos, R0_tos, sign_mask);
#else
  __ neg_float(S0_tos, S0_tos);
#endif // __SOFTFP__
}


void TemplateTable::dneg() {
  transition(dtos, dtos);
#ifdef __SOFTFP__
  // Invert sign bit in the high part of the double
  const int sign_mask_hi = 0x80000000;
  __ eor(R1_tos_hi, R1_tos_hi, sign_mask_hi);
#else
  __ neg_double(D0_tos, D0_tos);
#endif // __SOFTFP__
}


void TemplateTable::iinc() {
  transition(vtos, vtos);
  const Register Rconst = R2_tmp;
  const Register Rlocal_index = R1_tmp;
  const Register Rval = R0_tmp;

  __ ldrsb(Rconst, at_bcp(2));
  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(Rval, local);
  __ add(Rval, Rval, Rconst);
  __ str_32(Rval, local);
}


void TemplateTable::wide_iinc() {
  transition(vtos, vtos);
  const Register Rconst = R2_tmp;
  const Register Rlocal_index = R1_tmp;
  const Register Rval = R0_tmp;

  // get constant in Rconst
  __ ldrsb(R2_tmp, at_bcp(4));
  __ ldrb(R3_tmp, at_bcp(5));
  __ orr(Rconst, R3_tmp, AsmOperand(R2_tmp, lsl, 8));

  locals_index_wide(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(Rval, local);
  __ add(Rval, Rval, Rconst);
  __ str_32(Rval, local);
}


void TemplateTable::convert() {
  // Checking
#ifdef ASSERT
  { TosState tos_in  = ilgl;
    TosState tos_out = ilgl;
    switch (bytecode()) {
      case Bytecodes::_i2l: // fall through
      case Bytecodes::_i2f: // fall through
      case Bytecodes::_i2d: // fall through
      case Bytecodes::_i2b: // fall through
      case Bytecodes::_i2c: // fall through
      case Bytecodes::_i2s: tos_in = itos; break;
      case Bytecodes::_l2i: // fall through
      case Bytecodes::_l2f: // fall through
      case Bytecodes::_l2d: tos_in = ltos; break;
      case Bytecodes::_f2i: // fall through
      case Bytecodes::_f2l: // fall through
      case Bytecodes::_f2d: tos_in = ftos; break;
      case Bytecodes::_d2i: // fall through
      case Bytecodes::_d2l: // fall through
      case Bytecodes::_d2f: tos_in = dtos; break;
      default             : ShouldNotReachHere();
    }
    switch (bytecode()) {
      case Bytecodes::_l2i: // fall through
      case Bytecodes::_f2i: // fall through
      case Bytecodes::_d2i: // fall through
      case Bytecodes::_i2b: // fall through
      case Bytecodes::_i2c: // fall through
      case Bytecodes::_i2s: tos_out = itos; break;
      case Bytecodes::_i2l: // fall through
      case Bytecodes::_f2l: // fall through
      case Bytecodes::_d2l: tos_out = ltos; break;
      case Bytecodes::_i2f: // fall through
      case Bytecodes::_l2f: // fall through
      case Bytecodes::_d2f: tos_out = ftos; break;
      case Bytecodes::_i2d: // fall through
      case Bytecodes::_l2d: // fall through
      case Bytecodes::_f2d: tos_out = dtos; break;
      default             : ShouldNotReachHere();
    }
    transition(tos_in, tos_out);
  }
#endif // ASSERT

  // Conversion
  switch (bytecode()) {
    case Bytecodes::_i2l:
      __ mov(R1_tos_hi, AsmOperand(R0_tos, asr, BitsPerWord-1));
      break;

    case Bytecodes::_i2f:
#ifdef __SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_i2f), R0_tos);
#else
      __ fmsr(S0_tmp, R0_tos);
      __ fsitos(S0_tos, S0_tmp);
#endif // __SOFTFP__
      break;

    case Bytecodes::_i2d:
#ifdef __SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_i2d), R0_tos);
#else
      __ fmsr(S0_tmp, R0_tos);
      __ fsitod(D0_tos, S0_tmp);
#endif // __SOFTFP__
      break;

    case Bytecodes::_i2b:
      __ sign_extend(R0_tos, R0_tos, 8);
      break;

    case Bytecodes::_i2c:
      __ zero_extend(R0_tos, R0_tos, 16);
      break;

    case Bytecodes::_i2s:
      __ sign_extend(R0_tos, R0_tos, 16);
      break;

    case Bytecodes::_l2i:
      /* nothing to do */
      break;

    case Bytecodes::_l2f:
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::l2f), R0_tos_lo, R1_tos_hi);
#if !defined(__SOFTFP__) && !defined(__ABI_HARD__)
      __ fmsr(S0_tos, R0);
#endif // !__SOFTFP__ && !__ABI_HARD__
      break;

    case Bytecodes::_l2d:
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::l2d), R0_tos_lo, R1_tos_hi);
#if !defined(__SOFTFP__) && !defined(__ABI_HARD__)
      __ fmdrr(D0_tos, R0, R1);
#endif // !__SOFTFP__ && !__ABI_HARD__
      break;

    case Bytecodes::_f2i:
#ifndef __SOFTFP__
      __ ftosizs(S0_tos, S0_tos);
      __ fmrs(R0_tos, S0_tos);
#else
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::f2i), R0_tos);
#endif // !__SOFTFP__
      break;

    case Bytecodes::_f2l:
#ifndef __SOFTFP__
      __ fmrs(R0_tos, S0_tos);
#endif // !__SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::f2l), R0_tos);
      break;

    case Bytecodes::_f2d:
#ifdef __SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_f2d), R0_tos);
#else
      __ convert_f2d(D0_tos, S0_tos);
#endif // __SOFTFP__
      break;

    case Bytecodes::_d2i:
#ifndef __SOFTFP__
      __ ftosizd(Stemp, D0);
      __ fmrs(R0, Stemp);
#else
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::d2i), R0_tos_lo, R1_tos_hi);
#endif // !__SOFTFP__
      break;

    case Bytecodes::_d2l:
#ifndef __SOFTFP__
      __ fmrrd(R0_tos_lo, R1_tos_hi, D0_tos);
#endif // !__SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::d2l), R0_tos_lo, R1_tos_hi);
      break;

    case Bytecodes::_d2f:
#ifdef __SOFTFP__
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, __aeabi_d2f), R0_tos_lo, R1_tos_hi);
#else
      __ convert_d2f(S0_tos, D0_tos);
#endif // __SOFTFP__
      break;

    default:
      ShouldNotReachHere();
  }
}


void TemplateTable::lcmp() {
  transition(ltos, itos);
  const Register arg1_lo = R2_tmp;
  const Register arg1_hi = R3_tmp;
  const Register arg2_lo = R0_tos_lo;
  const Register arg2_hi = R1_tos_hi;
  const Register res = R4_tmp;

  __ pop_l(arg1_lo, arg1_hi);

  // long compare arg1 with arg2
  // result is -1/0/+1 if '<'/'='/'>'
  Label done;

  __ mov (res, 0);
  __ cmp (arg1_hi, arg2_hi);
  __ mvn (res, 0, lt);
  __ mov (res, 1, gt);
  __ b(done, ne);
  __ cmp (arg1_lo, arg2_lo);
  __ mvn (res, 0, lo);
  __ mov (res, 1, hi);
  __ bind(done);
  __ mov (R0_tos, res);
}


void TemplateTable::float_cmp(bool is_float, int unordered_result) {
  assert((unordered_result == 1) || (unordered_result == -1), "invalid unordered result");


#ifdef __SOFTFP__

  if (is_float) {
    transition(ftos, itos);
    const Register Rx = R0;
    const Register Ry = R1;

    __ mov(Ry, R0_tos);
    __ pop_i(Rx);

    if (unordered_result == 1) {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::fcmpg), Rx, Ry);
    } else {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::fcmpl), Rx, Ry);
    }

  } else {

    transition(dtos, itos);
    const Register Rx_lo = R0;
    const Register Rx_hi = R1;
    const Register Ry_lo = R2;
    const Register Ry_hi = R3;

    __ mov(Ry_lo, R0_tos_lo);
    __ mov(Ry_hi, R1_tos_hi);
    __ pop_l(Rx_lo, Rx_hi);

    if (unordered_result == 1) {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dcmpg), Rx_lo, Rx_hi, Ry_lo, Ry_hi);
    } else {
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dcmpl), Rx_lo, Rx_hi, Ry_lo, Ry_hi);
    }
  }

#else

  if (is_float) {
    transition(ftos, itos);
    __ pop_f(S1_tmp);
    __ fcmps(S1_tmp, S0_tos);
  } else {
    transition(dtos, itos);
    __ pop_d(D1_tmp);
    __ fcmpd(D1_tmp, D0_tos);
  }

  __ fmstat();

  // comparison result | flag N | flag Z | flag C | flag V
  // "<"               |   1    |   0    |   0    |   0
  // "=="              |   0    |   1    |   1    |   0
  // ">"               |   0    |   0    |   1    |   0
  // unordered         |   0    |   0    |   1    |   1

  if (unordered_result < 0) {
    __ mov(R0_tos, 1);           // result ==  1 if greater
    __ mvn(R0_tos, 0, lt);       // result == -1 if less or unordered (N!=V)
  } else {
    __ mov(R0_tos, 1);           // result ==  1 if greater or unordered
    __ mvn(R0_tos, 0, mi);       // result == -1 if less (N=1)
  }
  __ mov(R0_tos, 0, eq);         // result ==  0 if equ (Z=1)
#endif // __SOFTFP__
}


void TemplateTable::branch(bool is_jsr, bool is_wide) {

  const Register Rdisp = R0_tmp;
  const Register Rbumped_taken_count = R5_tmp;

  __ profile_taken_branch(R0_tmp, Rbumped_taken_count); // R0 holds updated MDP, Rbumped_taken_count holds bumped taken count

  const ByteSize be_offset = MethodCounters::backedge_counter_offset() +
                             InvocationCounter::counter_offset();
  const ByteSize inv_offset = MethodCounters::invocation_counter_offset() +
                              InvocationCounter::counter_offset();
  const int method_offset = frame::interpreter_frame_method_offset * wordSize;

  // Load up R0 with the branch displacement
  if (is_wide) {
    __ ldrsb(R0_tmp, at_bcp(1));
    __ ldrb(R1_tmp, at_bcp(2));
    __ ldrb(R2_tmp, at_bcp(3));
    __ ldrb(R3_tmp, at_bcp(4));
    __ orr(R0_tmp, R1_tmp, AsmOperand(R0_tmp, lsl, BitsPerByte));
    __ orr(R0_tmp, R2_tmp, AsmOperand(R0_tmp, lsl, BitsPerByte));
    __ orr(Rdisp, R3_tmp, AsmOperand(R0_tmp, lsl, BitsPerByte));
  } else {
    __ ldrsb(R0_tmp, at_bcp(1));
    __ ldrb(R1_tmp, at_bcp(2));
    __ orr(Rdisp, R1_tmp, AsmOperand(R0_tmp, lsl, BitsPerByte));
  }

  // Handle all the JSR stuff here, then exit.
  // It's much shorter and cleaner than intermingling with the
  // non-JSR normal-branch stuff occuring below.
  if (is_jsr) {
    // compute return address as bci in R1
    const Register Rret_addr = R1_tmp;
    assert_different_registers(Rdisp, Rret_addr, Rtemp);

    __ ldr(Rtemp, Address(Rmethod, Method::const_offset()));
    __ sub(Rret_addr, Rbcp, - (is_wide ? 5 : 3) + in_bytes(ConstMethod::codes_offset()));
    __ sub(Rret_addr, Rret_addr, Rtemp);

    // Load the next target bytecode into R3_bytecode and advance Rbcp
    __ ldrb(R3_bytecode, Address(Rbcp, Rdisp, lsl, 0, pre_indexed));

    // Push return address
    __ push_i(Rret_addr);
    // jsr returns vtos
    __ dispatch_only_noverify(vtos);
    return;
  }

  // Normal (non-jsr) branch handling

  // Adjust the bcp by the displacement in Rdisp and load next bytecode.
  __ ldrb(R3_bytecode, Address(Rbcp, Rdisp, lsl, 0, pre_indexed));

  assert(UseLoopCounter || !UseOnStackReplacement, "on-stack-replacement requires loop counters");
  Label backedge_counter_overflow;
  Label dispatch;

  if (UseLoopCounter) {
    // increment backedge counter for backward branches
    // Rdisp (R0): target offset

    const Register Rcnt = R2_tmp;
    const Register Rcounters = R1_tmp;

    // count only if backward branch
    __ tst(Rdisp, Rdisp);
    __ b(dispatch, pl);

    Label no_mdo;
    int increment = InvocationCounter::count_increment;
    if (ProfileInterpreter) {
      // Are we profiling?
      __ ldr(Rtemp, Address(Rmethod, Method::method_data_offset()));
      __ cbz(Rtemp, no_mdo);
      // Increment the MDO backedge counter
      const Address mdo_backedge_counter(Rtemp, in_bytes(MethodData::backedge_counter_offset()) +
                                                in_bytes(InvocationCounter::counter_offset()));
      const Address mask(Rtemp, in_bytes(MethodData::backedge_mask_offset()));
      __ increment_mask_and_jump(mdo_backedge_counter, increment, mask,
                                 Rcnt, R4_tmp, eq, &backedge_counter_overflow);
      __ b(dispatch);
    }
    __ bind(no_mdo);
    // Increment backedge counter in MethodCounters*
    // Note Rbumped_taken_count is a callee saved registers for ARM32
    __ get_method_counters(Rmethod, Rcounters, dispatch, true /*saveRegs*/,
                           Rdisp, R3_bytecode,
                           noreg);
    const Address mask(Rcounters, in_bytes(MethodCounters::backedge_mask_offset()));
    __ increment_mask_and_jump(Address(Rcounters, be_offset), increment, mask,
                               Rcnt, R4_tmp, eq, &backedge_counter_overflow);
    __ bind(dispatch);
  }

  if (!UseOnStackReplacement) {
    __ bind(backedge_counter_overflow);
  }

  // continue with the bytecode @ target
  __ dispatch_only(vtos, true);

  if (UseLoopCounter && UseOnStackReplacement) {
    // invocation counter overflow
    __ bind(backedge_counter_overflow);

    __ sub(R1, Rbcp, Rdisp);                   // branch bcp
    call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::frequency_counter_overflow), R1);

    // R0: osr nmethod (osr ok) or NULL (osr not possible)
    const Register Rnmethod = R0;

    __ ldrb(R3_bytecode, Address(Rbcp));       // reload next bytecode

    __ cbz(Rnmethod, dispatch);                // test result, no osr if null

    // nmethod may have been invalidated (VM may block upon call_VM return)
    __ ldrb(R1_tmp, Address(Rnmethod, nmethod::state_offset()));
    __ cmp(R1_tmp, nmethod::in_use);
    __ b(dispatch, ne);

    // We have the address of an on stack replacement routine in Rnmethod,
    // We need to prepare to execute the OSR method. First we must
    // migrate the locals and monitors off of the stack.

    __ mov(Rtmp_save0, Rnmethod);                      // save the nmethod

    call_VM(noreg, CAST_FROM_FN_PTR(address, SharedRuntime::OSR_migration_begin));

    // R0 is OSR buffer

    __ ldr(R1_tmp, Address(Rtmp_save0, nmethod::osr_entry_point_offset()));
    __ ldr(Rtemp, Address(FP, frame::interpreter_frame_sender_sp_offset * wordSize));

    __ ldmia(FP, RegisterSet(FP) | RegisterSet(LR));
    __ bic(SP, Rtemp, StackAlignmentInBytes - 1);     // Remove frame and align stack

    __ jump(R1_tmp);
  }
}


void TemplateTable::if_0cmp(Condition cc) {
  transition(itos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  __ cmp_32(R0_tos, 0);
  __ b(not_taken, convNegCond(cc));
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(R0_tmp);
}


void TemplateTable::if_icmp(Condition cc) {
  transition(itos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  __ pop_i(R1_tmp);
  __ cmp_32(R1_tmp, R0_tos);
  __ b(not_taken, convNegCond(cc));
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(R0_tmp);
}


void TemplateTable::if_nullcmp(Condition cc) {
  transition(atos, vtos);
  assert(cc == equal || cc == not_equal, "invalid condition");

  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  if (cc == equal) {
    __ cbnz(R0_tos, not_taken);
  } else {
    __ cbz(R0_tos, not_taken);
  }
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(R0_tmp);
}


void TemplateTable::if_acmp(Condition cc) {
  transition(atos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  __ pop_ptr(R1_tmp);
  __ cmpoop(R1_tmp, R0_tos);
  __ b(not_taken, convNegCond(cc));
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(R0_tmp);
}


void TemplateTable::ret() {
  transition(vtos, vtos);
  const Register Rlocal_index = R1_tmp;
  const Register Rret_bci = Rtmp_save0; // R4/R19

  locals_index(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(Rret_bci, local);          // get return bci, compute return bcp
  __ profile_ret(Rtmp_save1, Rret_bci);
  __ ldr(Rtemp, Address(Rmethod, Method::const_offset()));
  __ add(Rtemp, Rtemp, in_bytes(ConstMethod::codes_offset()));
  __ add(Rbcp, Rtemp, Rret_bci);
  __ dispatch_next(vtos);
}


void TemplateTable::wide_ret() {
  transition(vtos, vtos);
  const Register Rlocal_index = R1_tmp;
  const Register Rret_bci = Rtmp_save0; // R4/R19

  locals_index_wide(Rlocal_index);
  Address local = load_iaddress(Rlocal_index, Rtemp);
  __ ldr_s32(Rret_bci, local);               // get return bci, compute return bcp
  __ profile_ret(Rtmp_save1, Rret_bci);
  __ ldr(Rtemp, Address(Rmethod, Method::const_offset()));
  __ add(Rtemp, Rtemp, in_bytes(ConstMethod::codes_offset()));
  __ add(Rbcp, Rtemp, Rret_bci);
  __ dispatch_next(vtos);
}


void TemplateTable::tableswitch() {
  transition(itos, vtos);

  const Register Rindex  = R0_tos;
  const Register Rtemp2  = R1_tmp;
  const Register Rabcp   = R2_tmp;  // aligned bcp
  const Register Rlow    = R3_tmp;
  const Register Rhigh   = R4_tmp;
  const Register Roffset = R5_tmp;

  // align bcp
  __ add(Rtemp, Rbcp, 1 + (2*BytesPerInt-1));
  __ align_reg(Rabcp, Rtemp, BytesPerInt);

  // load lo & hi
  __ ldmia(Rabcp, RegisterSet(Rlow) | RegisterSet(Rhigh), writeback);
  __ byteswap_u32(Rlow, Rtemp, Rtemp2);
  __ byteswap_u32(Rhigh, Rtemp, Rtemp2);

  // compare index with high bound
  __ cmp_32(Rhigh, Rindex);


  // if Rindex <= Rhigh then calculate index in table (Rindex - Rlow)
  __ subs(Rindex, Rindex, Rlow, ge);

  // if Rindex <= Rhigh and (Rindex - Rlow) >= 0
  // ("ge" status accumulated from cmp and subs instructions) then load
  // offset from table, otherwise load offset for default case

  if(ProfileInterpreter) {
    Label default_case, continue_execution;

    __ b(default_case, lt);
    __ ldr(Roffset, Address(Rabcp, Rindex, lsl, LogBytesPerInt));
    __ profile_switch_case(Rabcp, Rindex, Rtemp2, R0_tmp);
    __ b(continue_execution);

    __ bind(default_case);
    __ profile_switch_default(R0_tmp);
    __ ldr(Roffset, Address(Rabcp, -3 * BytesPerInt));

    __ bind(continue_execution);
  } else {
    __ ldr(Roffset, Address(Rabcp, -3 * BytesPerInt), lt);
    __ ldr(Roffset, Address(Rabcp, Rindex, lsl, LogBytesPerInt), ge);
  }

  __ byteswap_u32(Roffset, Rtemp, Rtemp2);

  // load the next bytecode to R3_bytecode and advance Rbcp
  __ ldrb(R3_bytecode, Address(Rbcp, Roffset, lsl, 0, pre_indexed));
  __ dispatch_only(vtos, true);

}


void TemplateTable::lookupswitch() {
  transition(itos, itos);
  __ stop("lookupswitch bytecode should have been rewritten");
}


void TemplateTable::fast_linearswitch() {
  transition(itos, vtos);
  Label loop, found, default_case, continue_execution;

  const Register Rkey     = R0_tos;
  const Register Rabcp    = R2_tmp;  // aligned bcp
  const Register Rdefault = R3_tmp;
  const Register Rcount   = R4_tmp;
  const Register Roffset  = R5_tmp;

  // bswap Rkey, so we can avoid bswapping the table entries
  __ byteswap_u32(Rkey, R1_tmp, Rtemp);

  // align bcp
  __ add(Rtemp, Rbcp, 1 + (BytesPerInt-1));
  __ align_reg(Rabcp, Rtemp, BytesPerInt);

  // load default & counter
  __ ldmia(Rabcp, RegisterSet(Rdefault) | RegisterSet(Rcount), writeback);
  __ byteswap_u32(Rcount, R1_tmp, Rtemp);

  __ cmp_32(Rcount, 0);
  __ ldr(Rtemp, Address(Rabcp, 2*BytesPerInt, post_indexed), ne);
  __ b(default_case, eq);

  // table search
  __ bind(loop);
  __ cmp_32(Rtemp, Rkey);
  __ b(found, eq);
  __ subs(Rcount, Rcount, 1);
  __ ldr(Rtemp, Address(Rabcp, 2*BytesPerInt, post_indexed), ne);
  __ b(loop, ne);

  // default case
  __ bind(default_case);
  __ profile_switch_default(R0_tmp);
  __ mov(Roffset, Rdefault);
  __ b(continue_execution);

  // entry found -> get offset
  __ bind(found);
  // Rabcp is already incremented and points to the next entry
  __ ldr_s32(Roffset, Address(Rabcp, -BytesPerInt));
  if (ProfileInterpreter) {
    // Calculate index of the selected case.
    assert_different_registers(Roffset, Rcount, Rtemp, R0_tmp, R1_tmp, R2_tmp);

    // align bcp
    __ add(Rtemp, Rbcp, 1 + (BytesPerInt-1));
    __ align_reg(R2_tmp, Rtemp, BytesPerInt);

    // load number of cases
    __ ldr_u32(R2_tmp, Address(R2_tmp, BytesPerInt));
    __ byteswap_u32(R2_tmp, R1_tmp, Rtemp);

    // Selected index = <number of cases> - <current loop count>
    __ sub(R1_tmp, R2_tmp, Rcount);
    __ profile_switch_case(R0_tmp, R1_tmp, Rtemp, R1_tmp);
  }

  // continue execution
  __ bind(continue_execution);
  __ byteswap_u32(Roffset, R1_tmp, Rtemp);

  // load the next bytecode to R3_bytecode and advance Rbcp
  __ ldrb(R3_bytecode, Address(Rbcp, Roffset, lsl, 0, pre_indexed));
  __ dispatch_only(vtos, true);
}


void TemplateTable::fast_binaryswitch() {
  transition(itos, vtos);
  // Implementation using the following core algorithm:
  //
  // int binary_search(int key, LookupswitchPair* array, int n) {
  //   // Binary search according to "Methodik des Programmierens" by
  //   // Edsger W. Dijkstra and W.H.J. Feijen, Addison Wesley Germany 1985.
  //   int i = 0;
  //   int j = n;
  //   while (i+1 < j) {
  //     // invariant P: 0 <= i < j <= n and (a[i] <= key < a[j] or Q)
  //     // with      Q: for all i: 0 <= i < n: key < a[i]
  //     // where a stands for the array and assuming that the (inexisting)
  //     // element a[n] is infinitely big.
  //     int h = (i + j) >> 1;
  //     // i < h < j
  //     if (key < array[h].fast_match()) {
  //       j = h;
  //     } else {
  //       i = h;
  //     }
  //   }
  //   // R: a[i] <= key < a[i+1] or Q
  //   // (i.e., if key is within array, i is the correct index)
  //   return i;
  // }

  // register allocation
  const Register key    = R0_tos;                // already set (tosca)
  const Register array  = R1_tmp;
  const Register i      = R2_tmp;
  const Register j      = R3_tmp;
  const Register h      = R4_tmp;
  const Register val    = R5_tmp;
  const Register temp1  = Rtemp;
  const Register temp2  = LR_tmp;
  const Register offset = R3_tmp;

  // set 'array' = aligned bcp + 2 ints
  __ add(temp1, Rbcp, 1 + (BytesPerInt-1) + 2*BytesPerInt);
  __ align_reg(array, temp1, BytesPerInt);

  // initialize i & j
  __ mov(i, 0);                                  // i = 0;
  __ ldr_s32(j, Address(array, -BytesPerInt));   // j = length(array);
  // Convert j into native byteordering
  __ byteswap_u32(j, temp1, temp2);

  // and start
  Label entry;
  __ b(entry);

  // binary search loop
  { Label loop;
    __ bind(loop);
    // int h = (i + j) >> 1;
    __ add(h, i, j);                             // h = i + j;
    __ logical_shift_right(h, h, 1);             // h = (i + j) >> 1;
    // if (key < array[h].fast_match()) {
    //   j = h;
    // } else {
    //   i = h;
    // }
    __ ldr_s32(val, Address(array, h, lsl, 1+LogBytesPerInt));
    // Convert array[h].match to native byte-ordering before compare
    __ byteswap_u32(val, temp1, temp2);
    __ cmp_32(key, val);
    __ mov(j, h, lt);   // j = h if (key <  array[h].fast_match())
    __ mov(i, h, ge);   // i = h if (key >= array[h].fast_match())
    // while (i+1 < j)
    __ bind(entry);
    __ add(temp1, i, 1);                             // i+1
    __ cmp(temp1, j);                                // i+1 < j
    __ b(loop, lt);
  }

  // end of binary search, result index is i (must check again!)
  Label default_case;
  // Convert array[i].match to native byte-ordering before compare
  __ ldr_s32(val, Address(array, i, lsl, 1+LogBytesPerInt));
  __ byteswap_u32(val, temp1, temp2);
  __ cmp_32(key, val);
  __ b(default_case, ne);

  // entry found
  __ add(temp1, array, AsmOperand(i, lsl, 1+LogBytesPerInt));
  __ ldr_s32(offset, Address(temp1, 1*BytesPerInt));
  __ profile_switch_case(R0, i, R1, i);
  __ byteswap_u32(offset, temp1, temp2);
  __ ldrb(R3_bytecode, Address(Rbcp, offset, lsl, 0, pre_indexed));
  __ dispatch_only(vtos, true);

  // default case
  __ bind(default_case);
  __ profile_switch_default(R0);
  __ ldr_s32(offset, Address(array, -2*BytesPerInt));
  __ byteswap_u32(offset, temp1, temp2);
  __ ldrb(R3_bytecode, Address(Rbcp, offset, lsl, 0, pre_indexed));
  __ dispatch_only(vtos, true);
}


void TemplateTable::_return(TosState state) {
  transition(state, state);
  assert(_desc->calls_vm(), "inconsistent calls_vm information"); // call in remove_activation

  if (_desc->bytecode() == Bytecodes::_return_register_finalizer) {
    Label skip_register_finalizer;
    assert(state == vtos, "only valid state");
    __ ldr(R1, aaddress(0));
    __ load_klass(Rtemp, R1);
    __ ldr_u32(Rtemp, Address(Rtemp, Klass::access_flags_offset()));
    __ tbz(Rtemp, exact_log2(JVM_ACC_HAS_FINALIZER), skip_register_finalizer);

    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::register_finalizer), R1);

    __ bind(skip_register_finalizer);
  }

  // Narrow result if state is itos but result type is smaller.
  // Need to narrow in the return bytecode rather than in generate_return_entry
  // since compiled code callers expect the result to already be narrowed.
  if (state == itos) {
    __ narrow(R0_tos);
  }
  __ remove_activation(state, LR);

  __ interp_verify_oop(R0_tos, state, __FILE__, __LINE__);

  // According to interpreter calling conventions, result is returned in R0/R1,
  // so ftos (S0) and dtos (D0) are moved to R0/R1.
  // This conversion should be done after remove_activation, as it uses
  // push(state) & pop(state) to preserve return value.
  __ convert_tos_to_retval(state);

  __ ret();

  __ nop(); // to avoid filling CPU pipeline with invalid instructions
  __ nop();
}


// ----------------------------------------------------------------------------
// Volatile variables demand their effects be made known to all CPU's in
// order.  Store buffers on most chips allow reads & writes to reorder; the
// JMM's ReadAfterWrite.java test fails in -Xint mode without some kind of
// memory barrier (i.e., it's not sufficient that the interpreter does not
// reorder volatile references, the hardware also must not reorder them).
//
// According to the new Java Memory Model (JMM):
// (1) All volatiles are serialized wrt to each other.
// ALSO reads & writes act as aquire & release, so:
// (2) A read cannot let unrelated NON-volatile memory refs that happen after
// the read float up to before the read.  It's OK for non-volatile memory refs
// that happen before the volatile read to float down below it.
// (3) Similar a volatile write cannot let unrelated NON-volatile memory refs
// that happen BEFORE the write float down to after the write.  It's OK for
// non-volatile memory refs that happen after the volatile write to float up
// before it.
//
// We only put in barriers around volatile refs (they are expensive), not
// _between_ memory refs (that would require us to track the flavor of the
// previous memory refs).  Requirements (2) and (3) require some barriers
// before volatile stores and after volatile loads.  These nearly cover
// requirement (1) but miss the volatile-store-volatile-load case.  This final
// case is placed after volatile-stores although it could just as well go
// before volatile-loads.
void TemplateTable::volatile_barrier(MacroAssembler::Membar_mask_bits order_constraint,
                                     Register tmp,
                                     bool preserve_flags,
                                     Register load_tgt) {
  __ membar(order_constraint, tmp, preserve_flags, load_tgt);
}

// Blows all volatile registers: R0-R3, Rtemp, LR.
void TemplateTable::resolve_cache_and_index(int byte_no,
                                            Register Rcache,
                                            Register Rindex,
                                            size_t index_size) {
  assert_different_registers(Rcache, Rindex, Rtemp);

  Label resolved;
  Bytecodes::Code code = bytecode();
  switch (code) {
  case Bytecodes::_nofast_getfield: code = Bytecodes::_getfield; break;
  case Bytecodes::_nofast_putfield: code = Bytecodes::_putfield; break;
  default: break;
  }

  assert(byte_no == f1_byte || byte_no == f2_byte, "byte_no out of range");
  __ get_cache_and_index_and_bytecode_at_bcp(Rcache, Rindex, Rtemp, byte_no, 1, index_size);
  __ cmp(Rtemp, code);  // have we resolved this bytecode?
  __ b(resolved, eq);

  // resolve first time through
  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_from_cache);
  __ mov(R1, code);
  __ call_VM(noreg, entry, R1);
  // Update registers with resolved info
  __ get_cache_and_index_at_bcp(Rcache, Rindex, 1, index_size);
  __ bind(resolved);
}


// The Rcache and Rindex registers must be set before call
void TemplateTable::load_field_cp_cache_entry(Register Rcache,
                                              Register Rindex,
                                              Register Roffset,
                                              Register Rflags,
                                              Register Robj,
                                              bool is_static = false) {

  assert_different_registers(Rcache, Rindex, Rtemp);
  assert_different_registers(Roffset, Rflags, Robj, Rtemp);

  ByteSize cp_base_offset = ConstantPoolCache::base_offset();

  __ add(Rtemp, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));

  // Field offset
  __ ldr(Roffset, Address(Rtemp,
           cp_base_offset + ConstantPoolCacheEntry::f2_offset()));

  // Flags
  __ ldr_u32(Rflags, Address(Rtemp,
           cp_base_offset + ConstantPoolCacheEntry::flags_offset()));

  if (is_static) {
    __ ldr(Robj, Address(Rtemp,
             cp_base_offset + ConstantPoolCacheEntry::f1_offset()));
    const int mirror_offset = in_bytes(Klass::java_mirror_offset());
    __ ldr(Robj, Address(Robj, mirror_offset));
    __ resolve_oop_handle(Robj);
  }
}


// Blows all volatile registers: R0-R3, Rtemp, LR.
void TemplateTable::load_invoke_cp_cache_entry(int byte_no,
                                               Register method,
                                               Register itable_index,
                                               Register flags,
                                               bool is_invokevirtual,
                                               bool is_invokevfinal/*unused*/,
                                               bool is_invokedynamic) {
  // setup registers
  const Register cache = R2_tmp;
  const Register index = R3_tmp;
  const Register temp_reg = Rtemp;
  assert_different_registers(cache, index, temp_reg);
  assert_different_registers(method, itable_index, temp_reg);

  // determine constant pool cache field offsets
  assert(is_invokevirtual == (byte_no == f2_byte), "is_invokevirtual flag redundant");
  const int method_offset = in_bytes(
    ConstantPoolCache::base_offset() +
      ((byte_no == f2_byte)
       ? ConstantPoolCacheEntry::f2_offset()
       : ConstantPoolCacheEntry::f1_offset()
      )
    );
  const int flags_offset = in_bytes(ConstantPoolCache::base_offset() +
                                    ConstantPoolCacheEntry::flags_offset());
  // access constant pool cache fields
  const int index_offset = in_bytes(ConstantPoolCache::base_offset() +
                                    ConstantPoolCacheEntry::f2_offset());

  size_t index_size = (is_invokedynamic ? sizeof(u4) : sizeof(u2));
  resolve_cache_and_index(byte_no, cache, index, index_size);
    __ add(temp_reg, cache, AsmOperand(index, lsl, LogBytesPerWord));
    __ ldr(method, Address(temp_reg, method_offset));

  if (itable_index != noreg) {
    __ ldr(itable_index, Address(temp_reg, index_offset));
  }
  __ ldr_u32(flags, Address(temp_reg, flags_offset));
}


// The registers cache and index expected to be set before call, and should not be Rtemp.
// Blows volatile registers R0-R3, Rtemp, LR,
// except cache and index registers which are preserved.
void TemplateTable::jvmti_post_field_access(Register Rcache,
                                            Register Rindex,
                                            bool is_static,
                                            bool has_tos) {
  assert_different_registers(Rcache, Rindex, Rtemp);

  if (__ can_post_field_access()) {
    // Check to see if a field access watch has been set before we take
    // the time to call into the VM.

    Label Lcontinue;

    __ ldr_global_s32(Rtemp, (address)JvmtiExport::get_field_access_count_addr());
    __ cbz(Rtemp, Lcontinue);

    // cache entry pointer
    __ add(R2, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));
    __ add(R2, R2, in_bytes(ConstantPoolCache::base_offset()));
    if (is_static) {
      __ mov(R1, 0);        // NULL object reference
    } else {
      __ pop(atos);         // Get the object
      __ mov(R1, R0_tos);
      __ verify_oop(R1);
      __ push(atos);        // Restore stack state
    }
    // R1: object pointer or NULL
    // R2: cache entry pointer
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_access),
               R1, R2);
    __ get_cache_and_index_at_bcp(Rcache, Rindex, 1);

    __ bind(Lcontinue);
  }
}


void TemplateTable::pop_and_check_object(Register r) {
  __ pop_ptr(r);
  __ null_check(r, Rtemp);  // for field access must check obj.
  __ verify_oop(r);
}


void TemplateTable::getfield_or_static(int byte_no, bool is_static, RewriteControl rc) {
  transition(vtos, vtos);

  const Register Roffset  = R2_tmp;
  const Register Robj     = R3_tmp;
  const Register Rcache   = R4_tmp;
  const Register Rflagsav = Rtmp_save0;  // R4/R19
  const Register Rindex   = R5_tmp;
  const Register Rflags   = R5_tmp;

  resolve_cache_and_index(byte_no, Rcache, Rindex, sizeof(u2));
  jvmti_post_field_access(Rcache, Rindex, is_static, false);
  load_field_cp_cache_entry(Rcache, Rindex, Roffset, Rflags, Robj, is_static);

  __ mov(Rflagsav, Rflags);

  if (!is_static) pop_and_check_object(Robj);

  Label Done, Lint, Ltable, shouldNotReachHere;
  Label Lbtos, Lztos, Lctos, Lstos, Litos, Lltos, Lftos, Ldtos, Latos;

  // compute type
  __ logical_shift_right(Rflags, Rflags, ConstantPoolCacheEntry::tos_state_shift);
  // Make sure we don't need to mask flags after the above shift
  ConstantPoolCacheEntry::verify_tos_state_shift();

  // There are actually two versions of implementation of getfield/getstatic:
  //
  // 1) Table switch using add(PC,...) instruction (fast_version)
  // 2) Table switch using ldr(PC,...) instruction
  //
  // First version requires fixed size of code block for each case and
  // can not be used in RewriteBytecodes and VerifyOops
  // modes.

  // Size of fixed size code block for fast_version
  const int log_max_block_size = 3;
  const int max_block_size = 1 << log_max_block_size;

  // Decide if fast version is enabled
  bool fast_version = (is_static || !RewriteBytecodes) && !VerifyOops;

  // On 32-bit ARM atos and itos cases can be merged only for fast version, because
  // atos requires additional processing in slow version.
  bool atos_merged_with_itos = fast_version;

  assert(number_of_states == 10, "number of tos states should be equal to 9");

  __ cmp(Rflags, itos);
  if(atos_merged_with_itos) {
    __ cmp(Rflags, atos, ne);
  }

  // table switch by type
  if(fast_version) {
    __ add(PC, PC, AsmOperand(Rflags, lsl, log_max_block_size + Assembler::LogInstructionSize), ne);
  } else {
    __ ldr(PC, Address(PC, Rflags, lsl, LogBytesPerWord), ne);
  }

  // jump to itos/atos case
  __ b(Lint);

  // table with addresses for slow version
  if (fast_version) {
    // nothing to do
  } else  {
    __ bind(Ltable);
    __ emit_address(Lbtos);
    __ emit_address(Lztos);
    __ emit_address(Lctos);
    __ emit_address(Lstos);
    __ emit_address(Litos);
    __ emit_address(Lltos);
    __ emit_address(Lftos);
    __ emit_address(Ldtos);
    __ emit_address(Latos);
  }

#ifdef ASSERT
  int seq = 0;
#endif
  // btos
  {
    assert(btos == seq++, "btos has unexpected value");
    FixedSizeCodeBlock btos_block(_masm, max_block_size, fast_version);
    __ bind(Lbtos);
    __ access_load_at(T_BYTE, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
    __ push(btos);
    // Rewrite bytecode to be faster
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_bgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // ztos (same as btos for getfield)
  {
    assert(ztos == seq++, "btos has unexpected value");
    FixedSizeCodeBlock ztos_block(_masm, max_block_size, fast_version);
    __ bind(Lztos);
    __ access_load_at(T_BOOLEAN, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
    __ push(ztos);
    // Rewrite bytecode to be faster (use btos fast getfield)
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_bgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // ctos
  {
    assert(ctos == seq++, "ctos has unexpected value");
    FixedSizeCodeBlock ctos_block(_masm, max_block_size, fast_version);
    __ bind(Lctos);
    __ access_load_at(T_CHAR, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
    __ push(ctos);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_cgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // stos
  {
    assert(stos == seq++, "stos has unexpected value");
    FixedSizeCodeBlock stos_block(_masm, max_block_size, fast_version);
    __ bind(Lstos);
    __ access_load_at(T_SHORT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
    __ push(stos);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_sgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // itos
  {
    assert(itos == seq++, "itos has unexpected value");
    FixedSizeCodeBlock itos_block(_masm, max_block_size, fast_version);
    __ bind(Litos);
    __ b(shouldNotReachHere);
  }

  // ltos
  {
    assert(ltos == seq++, "ltos has unexpected value");
    FixedSizeCodeBlock ltos_block(_masm, max_block_size, fast_version);
    __ bind(Lltos);
    __ access_load_at(T_LONG, IN_HEAP, Address(Robj, Roffset), noreg /* ltos */, noreg, noreg, noreg);
    __ push(ltos);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_lgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // ftos
  {
    assert(ftos == seq++, "ftos has unexpected value");
    FixedSizeCodeBlock ftos_block(_masm, max_block_size, fast_version);
    __ bind(Lftos);
    // floats and ints are placed on stack in same way, so
    // we can use push(itos) to transfer value without using VFP
    __ access_load_at(T_INT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
    __ push(itos);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_fgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // dtos
  {
    assert(dtos == seq++, "dtos has unexpected value");
    FixedSizeCodeBlock dtos_block(_masm, max_block_size, fast_version);
    __ bind(Ldtos);
    // doubles and longs are placed on stack in the same way, so
    // we can use push(ltos) to transfer value without using VFP
    __ access_load_at(T_LONG, IN_HEAP, Address(Robj, Roffset), noreg /* ltos */, noreg, noreg, noreg);
    __ push(ltos);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_dgetfield, R0_tmp, Rtemp);
    }
    __ b(Done);
  }

  // atos
  {
    assert(atos == seq++, "atos has unexpected value");

    // atos case for slow version on 32-bit ARM
    if(!atos_merged_with_itos) {
      __ bind(Latos);
      do_oop_load(_masm, R0_tos, Address(Robj, Roffset));
      __ push(atos);
      // Rewrite bytecode to be faster
      if (!is_static && rc == may_rewrite) {
        patch_bytecode(Bytecodes::_fast_agetfield, R0_tmp, Rtemp);
      }
      __ b(Done);
    }
  }

  assert(vtos == seq++, "vtos has unexpected value");

  __ bind(shouldNotReachHere);
  __ should_not_reach_here();

  // itos and atos cases are frequent so it makes sense to move them out of table switch
  // atos case can be merged with itos case (and thus moved out of table switch) on 32-bit ARM, fast version only

  __ bind(Lint);
  __ access_load_at(T_INT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
  __ push(itos);
  // Rewrite bytecode to be faster
  if (!is_static && rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_igetfield, R0_tmp, Rtemp);
  }

  __ bind(Done);

  // Check for volatile field
  Label notVolatile;
  __ tbz(Rflagsav, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);

  volatile_barrier(MacroAssembler::Membar_mask_bits(MacroAssembler::LoadLoad | MacroAssembler::LoadStore), Rtemp);

  __ bind(notVolatile);
}

void TemplateTable::getfield(int byte_no) {
  getfield_or_static(byte_no, false);
}

void TemplateTable::nofast_getfield(int byte_no) {
  getfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::getstatic(int byte_no) {
  getfield_or_static(byte_no, true);
}


// The registers cache and index expected to be set before call, and should not be R1 or Rtemp.
// Blows volatile registers R0-R3, Rtemp, LR,
// except cache and index registers which are preserved.
void TemplateTable::jvmti_post_field_mod(Register Rcache, Register Rindex, bool is_static) {
  ByteSize cp_base_offset = ConstantPoolCache::base_offset();
  assert_different_registers(Rcache, Rindex, R1, Rtemp);

  if (__ can_post_field_modification()) {
    // Check to see if a field modification watch has been set before we take
    // the time to call into the VM.
    Label Lcontinue;

    __ ldr_global_s32(Rtemp, (address)JvmtiExport::get_field_modification_count_addr());
    __ cbz(Rtemp, Lcontinue);

    if (is_static) {
      // Life is simple.  Null out the object pointer.
      __ mov(R1, 0);
    } else {
      // Life is harder. The stack holds the value on top, followed by the object.
      // We don't know the size of the value, though; it could be one or two words
      // depending on its type. As a result, we must find the type to determine where
      // the object is.

      __ add(Rtemp, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));
      __ ldr_u32(Rtemp, Address(Rtemp, cp_base_offset + ConstantPoolCacheEntry::flags_offset()));

      __ logical_shift_right(Rtemp, Rtemp, ConstantPoolCacheEntry::tos_state_shift);
      // Make sure we don't need to mask Rtemp after the above shift
      ConstantPoolCacheEntry::verify_tos_state_shift();

      __ cmp(Rtemp, ltos);
      __ cond_cmp(Rtemp, dtos, ne);
      // two word value (ltos/dtos)
      __ ldr(R1, Address(SP, Interpreter::expr_offset_in_bytes(2)), eq);

      // one word value (not ltos, dtos)
      __ ldr(R1, Address(SP, Interpreter::expr_offset_in_bytes(1)), ne);
    }

    // cache entry pointer
    __ add(R2, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));
    __ add(R2, R2, in_bytes(cp_base_offset));

    // object (tos)
    __ mov(R3, Rstack_top);

    // R1: object pointer set up above (NULL if static)
    // R2: cache entry pointer
    // R3: value object on the stack
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_modification),
               R1, R2, R3);
    __ get_cache_and_index_at_bcp(Rcache, Rindex, 1);

    __ bind(Lcontinue);
  }
}


void TemplateTable::putfield_or_static(int byte_no, bool is_static, RewriteControl rc) {
  transition(vtos, vtos);

  const Register Roffset  = R2_tmp;
  const Register Robj     = R3_tmp;
  const Register Rcache   = R4_tmp;
  const Register Rflagsav = Rtmp_save0;  // R4/R19
  const Register Rindex   = R5_tmp;
  const Register Rflags   = R5_tmp;

  resolve_cache_and_index(byte_no, Rcache, Rindex, sizeof(u2));
  jvmti_post_field_mod(Rcache, Rindex, is_static);
  load_field_cp_cache_entry(Rcache, Rindex, Roffset, Rflags, Robj, is_static);

  // Check for volatile field
  Label notVolatile;
  __ mov(Rflagsav, Rflags);
  __ tbz(Rflagsav, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);

  volatile_barrier(MacroAssembler::Membar_mask_bits(MacroAssembler::StoreStore | MacroAssembler::LoadStore), Rtemp);

  __ bind(notVolatile);

  Label Done, Lint, shouldNotReachHere;
  Label Ltable, Lbtos, Lztos, Lctos, Lstos, Litos, Lltos, Lftos, Ldtos, Latos;

  // compute type
  __ logical_shift_right(Rflags, Rflags, ConstantPoolCacheEntry::tos_state_shift);
  // Make sure we don't need to mask flags after the above shift
  ConstantPoolCacheEntry::verify_tos_state_shift();

  // There are actually two versions of implementation of putfield/putstatic:
  //
  // 32-bit ARM:
  // 1) Table switch using add(PC,...) instruction (fast_version)
  // 2) Table switch using ldr(PC,...) instruction
  //
  // First version requires fixed size of code block for each case and
  // can not be used in RewriteBytecodes and VerifyOops
  // modes.

  // Size of fixed size code block for fast_version (in instructions)
  const int log_max_block_size = 3;
  const int max_block_size = 1 << log_max_block_size;

  // Decide if fast version is enabled
  bool fast_version = (is_static || !RewriteBytecodes) && !VerifyOops;

  assert(number_of_states == 10, "number of tos states should be equal to 9");

  // itos case is frequent and is moved outside table switch
  __ cmp(Rflags, itos);

  // table switch by type
  if (fast_version) {
    __ add(PC, PC, AsmOperand(Rflags, lsl, log_max_block_size + Assembler::LogInstructionSize), ne);
  } else  {
    __ ldr(PC, Address(PC, Rflags, lsl, LogBytesPerWord), ne);
  }

  // jump to itos case
  __ b(Lint);

  // table with addresses for slow version
  if (fast_version) {
    // nothing to do
  } else  {
    __ bind(Ltable);
    __ emit_address(Lbtos);
    __ emit_address(Lztos);
    __ emit_address(Lctos);
    __ emit_address(Lstos);
    __ emit_address(Litos);
    __ emit_address(Lltos);
    __ emit_address(Lftos);
    __ emit_address(Ldtos);
    __ emit_address(Latos);
  }

#ifdef ASSERT
  int seq = 0;
#endif
  // btos
  {
    assert(btos == seq++, "btos has unexpected value");
    FixedSizeCodeBlock btos_block(_masm, max_block_size, fast_version);
    __ bind(Lbtos);
    __ pop(btos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_BYTE, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_bputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // ztos
  {
    assert(ztos == seq++, "ztos has unexpected value");
    FixedSizeCodeBlock ztos_block(_masm, max_block_size, fast_version);
    __ bind(Lztos);
    __ pop(ztos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_BOOLEAN, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_zputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // ctos
  {
    assert(ctos == seq++, "ctos has unexpected value");
    FixedSizeCodeBlock ctos_block(_masm, max_block_size, fast_version);
    __ bind(Lctos);
    __ pop(ctos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_CHAR, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_cputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // stos
  {
    assert(stos == seq++, "stos has unexpected value");
    FixedSizeCodeBlock stos_block(_masm, max_block_size, fast_version);
    __ bind(Lstos);
    __ pop(stos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_SHORT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_sputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // itos
  {
    assert(itos == seq++, "itos has unexpected value");
    FixedSizeCodeBlock itos_block(_masm, max_block_size, fast_version);
    __ bind(Litos);
    __ b(shouldNotReachHere);
  }

  // ltos
  {
    assert(ltos == seq++, "ltos has unexpected value");
    FixedSizeCodeBlock ltos_block(_masm, max_block_size, fast_version);
    __ bind(Lltos);
    __ pop(ltos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_LONG, IN_HEAP, Address(Robj, Roffset), noreg /* ltos */, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_lputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // ftos
  {
    assert(ftos == seq++, "ftos has unexpected value");
    FixedSizeCodeBlock ftos_block(_masm, max_block_size, fast_version);
    __ bind(Lftos);
    // floats and ints are placed on stack in the same way, so
    // we can use pop(itos) to transfer value without using VFP
    __ pop(itos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_INT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_fputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // dtos
  {
    assert(dtos == seq++, "dtos has unexpected value");
    FixedSizeCodeBlock dtos_block(_masm, max_block_size, fast_version);
    __ bind(Ldtos);
    // doubles and longs are placed on stack in the same way, so
    // we can use pop(ltos) to transfer value without using VFP
    __ pop(ltos);
    if (!is_static) pop_and_check_object(Robj);
    __ access_store_at(T_LONG, IN_HEAP, Address(Robj, Roffset), noreg /* ltos */, noreg, noreg, noreg, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_dputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  // atos
  {
    assert(atos == seq++, "dtos has unexpected value");
    __ bind(Latos);
    __ pop(atos);
    if (!is_static) pop_and_check_object(Robj);
    // Store into the field
    do_oop_store(_masm, Address(Robj, Roffset), R0_tos, Rtemp, R1_tmp, R5_tmp, false);
    if (!is_static && rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_aputfield, R0_tmp, Rtemp, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(shouldNotReachHere);
  __ should_not_reach_here();

  // itos case is frequent and is moved outside table switch
  __ bind(Lint);
  __ pop(itos);
  if (!is_static) pop_and_check_object(Robj);
  __ access_store_at(T_INT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg, false);
  if (!is_static && rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_iputfield, R0_tmp, Rtemp, true, byte_no);
  }

  __ bind(Done);

  Label notVolatile2;
  if (is_static) {
    // Just check for volatile. Memory barrier for static final field
    // is handled by class initialization.
    __ tbz(Rflagsav, ConstantPoolCacheEntry::is_volatile_shift, notVolatile2);
    volatile_barrier(MacroAssembler::StoreLoad, Rtemp);
    __ bind(notVolatile2);
  } else {
    // Check for volatile field and final field
    Label skipMembar;

    __ tst(Rflagsav, 1 << ConstantPoolCacheEntry::is_volatile_shift |
           1 << ConstantPoolCacheEntry::is_final_shift);
    __ b(skipMembar, eq);

    __ tbz(Rflagsav, ConstantPoolCacheEntry::is_volatile_shift, notVolatile2);

    // StoreLoad barrier after volatile field write
    volatile_barrier(MacroAssembler::StoreLoad, Rtemp);
    __ b(skipMembar);

    // StoreStore barrier after final field write
    __ bind(notVolatile2);
    volatile_barrier(MacroAssembler::StoreStore, Rtemp);

    __ bind(skipMembar);
  }
}

void TemplateTable::putfield(int byte_no) {
  putfield_or_static(byte_no, false);
}

void TemplateTable::nofast_putfield(int byte_no) {
  putfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::putstatic(int byte_no) {
  putfield_or_static(byte_no, true);
}


void TemplateTable::jvmti_post_fast_field_mod() {
  // This version of jvmti_post_fast_field_mod() is not used on ARM
  Unimplemented();
}

// Blows volatile registers R0-R3, Rtemp, LR,
// but preserves tosca with the given state.
void TemplateTable::jvmti_post_fast_field_mod(TosState state) {
  if (__ can_post_field_modification()) {
    // Check to see if a field modification watch has been set before we take
    // the time to call into the VM.
    Label done;

    __ ldr_global_s32(R2, (address)JvmtiExport::get_field_modification_count_addr());
    __ cbz(R2, done);

    __ pop_ptr(R3);               // copy the object pointer from tos
    __ verify_oop(R3);
    __ push_ptr(R3);              // put the object pointer back on tos

    __ push(state);               // save value on the stack

    // access constant pool cache entry
    __ get_cache_entry_pointer_at_bcp(R2, R1, 1);

    __ mov(R1, R3);
    assert(Interpreter::expr_offset_in_bytes(0) == 0, "adjust this code");
    __ mov(R3, Rstack_top); // put tos addr into R3

    // R1: object pointer copied above
    // R2: cache entry pointer
    // R3: jvalue object on the stack
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_modification), R1, R2, R3);

    __ pop(state);                // restore value

    __ bind(done);
  }
}


void TemplateTable::fast_storefield(TosState state) {
  transition(state, vtos);

  ByteSize base = ConstantPoolCache::base_offset();

  jvmti_post_fast_field_mod(state);

  const Register Rcache  = R2_tmp;
  const Register Rindex  = R3_tmp;
  const Register Roffset = R3_tmp;
  const Register Rflags  = Rtmp_save0; // R4/R19
  const Register Robj    = R5_tmp;

  // access constant pool cache
  __ get_cache_and_index_at_bcp(Rcache, Rindex, 1);

  __ add(Rcache, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));

  // load flags to test volatile
  __ ldr_u32(Rflags, Address(Rcache, base + ConstantPoolCacheEntry::flags_offset()));

  // replace index with field offset from cache entry
  __ ldr(Roffset, Address(Rcache, base + ConstantPoolCacheEntry::f2_offset()));

  // Check for volatile store
  Label notVolatile;
  __ tbz(Rflags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);

  volatile_barrier(MacroAssembler::Membar_mask_bits(MacroAssembler::StoreStore | MacroAssembler::LoadStore), Rtemp);

  __ bind(notVolatile);

  // Get object from stack
  pop_and_check_object(Robj);

  Address addr = Address(Robj, Roffset);
  // access field
  switch (bytecode()) {
    case Bytecodes::_fast_zputfield:
      __ access_store_at(T_BOOLEAN, IN_HEAP, addr, R0_tos, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_bputfield:
      __ access_store_at(T_BYTE, IN_HEAP, addr, R0_tos, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_sputfield:
      __ access_store_at(T_SHORT, IN_HEAP, addr, R0_tos, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_cputfield:
      __ access_store_at(T_CHAR, IN_HEAP, addr, R0_tos, noreg, noreg, noreg,false);
      break;
    case Bytecodes::_fast_iputfield:
      __ access_store_at(T_INT, IN_HEAP, addr, R0_tos, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_lputfield:
      __ access_store_at(T_LONG, IN_HEAP, addr, noreg, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_fputfield:
      __ access_store_at(T_FLOAT, IN_HEAP, addr, noreg, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_dputfield:
      __ access_store_at(T_DOUBLE, IN_HEAP, addr, noreg, noreg, noreg, noreg, false);
      break;
    case Bytecodes::_fast_aputfield:
      do_oop_store(_masm, addr, R0_tos, Rtemp, R1_tmp, R2_tmp, false);
      break;

    default:
      ShouldNotReachHere();
  }

  Label notVolatile2;
  Label skipMembar;
  __ tst(Rflags, 1 << ConstantPoolCacheEntry::is_volatile_shift |
         1 << ConstantPoolCacheEntry::is_final_shift);
  __ b(skipMembar, eq);

  __ tbz(Rflags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile2);

  // StoreLoad barrier after volatile field write
  volatile_barrier(MacroAssembler::StoreLoad, Rtemp);
  __ b(skipMembar);

  // StoreStore barrier after final field write
  __ bind(notVolatile2);
  volatile_barrier(MacroAssembler::StoreStore, Rtemp);

  __ bind(skipMembar);
}

void TemplateTable::fast_accessfield(TosState state) {
  transition(atos, state);

  // do the JVMTI work here to avoid disturbing the register state below
  if (__ can_post_field_access()) {
    // Check to see if a field access watch has been set before we take
    // the time to call into the VM.
    Label done;
    __ ldr_global_s32(R2, (address) JvmtiExport::get_field_access_count_addr());
    __ cbz(R2, done);
    // access constant pool cache entry
    __ get_cache_entry_pointer_at_bcp(R2, R1, 1);
    __ push_ptr(R0_tos);  // save object pointer before call_VM() clobbers it
    __ verify_oop(R0_tos);
    __ mov(R1, R0_tos);
    // R1: object pointer copied above
    // R2: cache entry pointer
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_access), R1, R2);
    __ pop_ptr(R0_tos);   // restore object pointer

    __ bind(done);
  }

  const Register Robj    = R0_tos;
  const Register Rcache  = R2_tmp;
  const Register Rflags  = R2_tmp;
  const Register Rindex  = R3_tmp;
  const Register Roffset = R3_tmp;

  // access constant pool cache
  __ get_cache_and_index_at_bcp(Rcache, Rindex, 1);
  // replace index with field offset from cache entry
  __ add(Rtemp, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));
  __ ldr(Roffset, Address(Rtemp, ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::f2_offset()));

  // load flags to test volatile
  __ ldr_u32(Rflags, Address(Rtemp, ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::flags_offset()));

  __ verify_oop(Robj);
  __ null_check(Robj, Rtemp);

  Address addr = Address(Robj, Roffset);
  // access field
  switch (bytecode()) {
    case Bytecodes::_fast_bgetfield:
      __ access_load_at(T_BYTE, IN_HEAP, addr, R0_tos, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_sgetfield:
      __ access_load_at(T_SHORT, IN_HEAP, addr, R0_tos, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_cgetfield:
      __ access_load_at(T_CHAR, IN_HEAP, addr, R0_tos, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_igetfield:
      __ access_load_at(T_INT, IN_HEAP, addr, R0_tos, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_lgetfield:
      __ access_load_at(T_LONG, IN_HEAP, addr, noreg, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_fgetfield:
      __ access_load_at(T_FLOAT, IN_HEAP, addr, noreg, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_dgetfield:
      __ access_load_at(T_DOUBLE, IN_HEAP, addr, noreg, noreg, noreg, noreg);
      break;
    case Bytecodes::_fast_agetfield:
      do_oop_load(_masm, R0_tos, addr);
      __ verify_oop(R0_tos);
      break;
    default:
      ShouldNotReachHere();
  }

  // Check for volatile load
  Label notVolatile;
  __ tbz(Rflags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);

  volatile_barrier(MacroAssembler::Membar_mask_bits(MacroAssembler::LoadLoad | MacroAssembler::LoadStore), Rtemp);

  __ bind(notVolatile);
}


void TemplateTable::fast_xaccess(TosState state) {
  transition(vtos, state);

  const Register Robj = R1_tmp;
  const Register Rcache = R2_tmp;
  const Register Rindex = R3_tmp;
  const Register Roffset = R3_tmp;
  const Register Rflags = R4_tmp;
  Label done;

  // get receiver
  __ ldr(Robj, aaddress(0));

  // access constant pool cache
  __ get_cache_and_index_at_bcp(Rcache, Rindex, 2);
  __ add(Rtemp, Rcache, AsmOperand(Rindex, lsl, LogBytesPerWord));
  __ ldr(Roffset, Address(Rtemp, ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::f2_offset()));

  // load flags to test volatile
  __ ldr_u32(Rflags, Address(Rtemp, ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::flags_offset()));

  // make sure exception is reported in correct bcp range (getfield is next instruction)
  __ add(Rbcp, Rbcp, 1);
  __ null_check(Robj, Rtemp);
  __ sub(Rbcp, Rbcp, 1);


  if (state == itos) {
    __ access_load_at(T_INT, IN_HEAP, Address(Robj, Roffset), R0_tos, noreg, noreg, noreg);
  } else if (state == atos) {
    do_oop_load(_masm, R0_tos, Address(Robj, Roffset));
    __ verify_oop(R0_tos);
  } else if (state == ftos) {
#ifdef __SOFTFP__
    __ ldr(R0_tos, Address(Robj, Roffset));
#else
    __ access_load_at(T_FLOAT, IN_HEAP, Address(Robj, Roffset), noreg /* ftos */, noreg, noreg, noreg);
#endif // __SOFTFP__
  } else {
    ShouldNotReachHere();
  }

  // Check for volatile load
  Label notVolatile;
  __ tbz(Rflags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);

  volatile_barrier(MacroAssembler::Membar_mask_bits(MacroAssembler::LoadLoad | MacroAssembler::LoadStore), Rtemp);

  __ bind(notVolatile);

  __ bind(done);
}



//----------------------------------------------------------------------------------------------------
// Calls

void TemplateTable::prepare_invoke(int byte_no,
                                   Register method,  // linked method (or i-klass)
                                   Register index,   // itable index, MethodType, etc.
                                   Register recv,    // if caller wants to see it
                                   Register flags    // if caller wants to test it
                                   ) {
  // determine flags
  const Bytecodes::Code code = bytecode();
  const bool is_invokeinterface  = code == Bytecodes::_invokeinterface;
  const bool is_invokedynamic    = code == Bytecodes::_invokedynamic;
  const bool is_invokehandle     = code == Bytecodes::_invokehandle;
  const bool is_invokevirtual    = code == Bytecodes::_invokevirtual;
  const bool is_invokespecial    = code == Bytecodes::_invokespecial;
  const bool load_receiver       = (recv != noreg);
  assert(load_receiver == (code != Bytecodes::_invokestatic && code != Bytecodes::_invokedynamic), "");
  assert(recv  == noreg || recv  == R2, "");
  assert(flags == noreg || flags == R3, "");

  // setup registers & access constant pool cache
  if (recv  == noreg)  recv  = R2;
  if (flags == noreg)  flags = R3;
  const Register temp = Rtemp;
  const Register ret_type = R1_tmp;
  assert_different_registers(method, index, flags, recv, LR, ret_type, temp);

  // save 'interpreter return address'
  __ save_bcp();

  load_invoke_cp_cache_entry(byte_no, method, index, flags, is_invokevirtual, false, is_invokedynamic);

  // maybe push extra argument
  if (is_invokedynamic || is_invokehandle) {
    Label L_no_push;
    __ tbz(flags, ConstantPoolCacheEntry::has_appendix_shift, L_no_push);
    __ mov(temp, index);
    __ load_resolved_reference_at_index(index, temp);
    __ verify_oop(index);
    __ push_ptr(index);  // push appendix (MethodType, CallSite, etc.)
    __ bind(L_no_push);
  }

  // load receiver if needed (after extra argument is pushed so parameter size is correct)
  if (load_receiver) {
    __ andr(temp, flags, (uintx)ConstantPoolCacheEntry::parameter_size_mask);  // get parameter size
    Address recv_addr = __ receiver_argument_address(Rstack_top, temp, recv);
    __ ldr(recv, recv_addr);
    __ verify_oop(recv);
  }

  // compute return type
  __ logical_shift_right(ret_type, flags, ConstantPoolCacheEntry::tos_state_shift);
  // Make sure we don't need to mask flags after the above shift
  ConstantPoolCacheEntry::verify_tos_state_shift();
  // load return address
  { const address table = (address) Interpreter::invoke_return_entry_table_for(code);
    __ mov_slow(temp, table);
    __ ldr(LR, Address::indexed_ptr(temp, ret_type));
  }
}


void TemplateTable::invokevirtual_helper(Register index,
                                         Register recv,
                                         Register flags) {

  const Register recv_klass = R2_tmp;

  assert_different_registers(index, recv, flags, Rtemp);
  assert_different_registers(index, recv_klass, R0_tmp, Rtemp);

  // Test for an invoke of a final method
  Label notFinal;
  __ tbz(flags, ConstantPoolCacheEntry::is_vfinal_shift, notFinal);

  assert(index == Rmethod, "Method* must be Rmethod, for interpreter calling convention");

  // do the call - the index is actually the method to call

  // It's final, need a null check here!
  __ null_check(recv, Rtemp);

  // profile this call
  __ profile_final_call(R0_tmp);

  __ jump_from_interpreted(Rmethod);

  __ bind(notFinal);

  // get receiver klass
  __ null_check(recv, Rtemp, oopDesc::klass_offset_in_bytes());
  __ load_klass(recv_klass, recv);

  // profile this call
  __ profile_virtual_call(R0_tmp, recv_klass);

  // get target Method* & entry point
  const int base = in_bytes(Klass::vtable_start_offset());
  assert(vtableEntry::size() == 1, "adjust the scaling in the code below");
  __ add(Rtemp, recv_klass, AsmOperand(index, lsl, LogHeapWordSize));
  __ ldr(Rmethod, Address(Rtemp, base + vtableEntry::method_offset_in_bytes()));
  __ jump_from_interpreted(Rmethod);
}

void TemplateTable::invokevirtual(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f2_byte, "use this argument");

  const Register Rrecv  = R2_tmp;
  const Register Rflags = R3_tmp;

  prepare_invoke(byte_no, Rmethod, noreg, Rrecv, Rflags);

  // Rmethod: index
  // Rrecv:   receiver
  // Rflags:  flags
  // LR:      return address

  invokevirtual_helper(Rmethod, Rrecv, Rflags);
}


void TemplateTable::invokespecial(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");
  const Register Rrecv  = R2_tmp;
  prepare_invoke(byte_no, Rmethod, noreg, Rrecv);
  __ verify_oop(Rrecv);
  __ null_check(Rrecv, Rtemp);
  // do the call
  __ profile_call(Rrecv);
  __ jump_from_interpreted(Rmethod);
}


void TemplateTable::invokestatic(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");
  prepare_invoke(byte_no, Rmethod);
  // do the call
  __ profile_call(R2_tmp);
  __ jump_from_interpreted(Rmethod);
}


void TemplateTable::fast_invokevfinal(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f2_byte, "use this argument");
  __ stop("fast_invokevfinal is not used on ARM");
}


void TemplateTable::invokeinterface(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  const Register Ritable = R1_tmp;
  const Register Rrecv   = R2_tmp;
  const Register Rinterf = R5_tmp;
  const Register Rindex  = R4_tmp;
  const Register Rflags  = R3_tmp;
  const Register Rklass  = R2_tmp; // Note! Same register with Rrecv

  prepare_invoke(byte_no, Rinterf, Rmethod, Rrecv, Rflags);

  // First check for Object case, then private interface method,
  // then regular interface method.

  // Special case of invokeinterface called for virtual method of
  // java.lang.Object.  See cpCache.cpp for details.
  Label notObjectMethod;
  __ tbz(Rflags, ConstantPoolCacheEntry::is_forced_virtual_shift, notObjectMethod);
  invokevirtual_helper(Rmethod, Rrecv, Rflags);
  __ bind(notObjectMethod);

  // Get receiver klass into Rklass - also a null check
  __ load_klass(Rklass, Rrecv);

  // Check for private method invocation - indicated by vfinal
  Label no_such_interface;

  Label notVFinal;
  __ tbz(Rflags, ConstantPoolCacheEntry::is_vfinal_shift, notVFinal);

  Label subtype;
  __ check_klass_subtype(Rklass, Rinterf, R1_tmp, R3_tmp, noreg, subtype);
  // If we get here the typecheck failed
  __ b(no_such_interface);
  __ bind(subtype);

  // do the call
  __ profile_final_call(R0_tmp);
  __ jump_from_interpreted(Rmethod);

  __ bind(notVFinal);

  // Receiver subtype check against REFC.
  __ lookup_interface_method(// inputs: rec. class, interface
                             Rklass, Rinterf, noreg,
                             // outputs:  scan temp. reg1, scan temp. reg2
                             noreg, Ritable, Rtemp,
                             no_such_interface);

  // profile this call
  __ profile_virtual_call(R0_tmp, Rklass);

  // Get declaring interface class from method
  __ ldr(Rtemp, Address(Rmethod, Method::const_offset()));
  __ ldr(Rtemp, Address(Rtemp, ConstMethod::constants_offset()));
  __ ldr(Rinterf, Address(Rtemp, ConstantPool::pool_holder_offset_in_bytes()));

  // Get itable index from method
  __ ldr_s32(Rtemp, Address(Rmethod, Method::itable_index_offset()));
  __ add(Rtemp, Rtemp, (-Method::itable_index_max)); // small negative constant is too large for an immediate on arm32
  __ neg(Rindex, Rtemp);

  __ lookup_interface_method(// inputs: rec. class, interface
                             Rklass, Rinterf, Rindex,
                             // outputs:  scan temp. reg1, scan temp. reg2
                             Rmethod, Ritable, Rtemp,
                             no_such_interface);

  // Rmethod: Method* to call

  // Check for abstract method error
  // Note: This should be done more efficiently via a throw_abstract_method_error
  //       interpreter entry point and a conditional jump to it in case of a null
  //       method.
  { Label L;
    __ cbnz(Rmethod, L);
    // throw exception
    // note: must restore interpreter registers to canonical
    //       state for exception handling to work correctly!
    __ restore_method();
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_AbstractMethodError));
    // the call_VM checks for exception, so we should never return here.
    __ should_not_reach_here();
    __ bind(L);
  }

  // do the call
  __ jump_from_interpreted(Rmethod);

  // throw exception
  __ bind(no_such_interface);
  __ restore_method();
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_IncompatibleClassChangeError));
  // the call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();
}

void TemplateTable::invokehandle(int byte_no) {
  transition(vtos, vtos);

  const Register Rrecv  = R2_tmp;
  const Register Rmtype = R4_tmp;
  const Register R5_method = R5_tmp;  // can't reuse Rmethod!

  prepare_invoke(byte_no, R5_method, Rmtype, Rrecv);
  __ null_check(Rrecv, Rtemp);

  // Rmtype:  MethodType object (from cpool->resolved_references[f1], if necessary)
  // Rmethod: MH.invokeExact_MT method (from f2)

  // Note:  Rmtype is already pushed (if necessary) by prepare_invoke

  // do the call
  __ profile_final_call(R3_tmp);  // FIXME: profile the LambdaForm also
  __ mov(Rmethod, R5_method);
  __ jump_from_interpreted(Rmethod);
}

void TemplateTable::invokedynamic(int byte_no) {
  transition(vtos, vtos);

  const Register Rcallsite = R4_tmp;
  const Register R5_method = R5_tmp;  // can't reuse Rmethod!

  prepare_invoke(byte_no, R5_method, Rcallsite);

  // Rcallsite: CallSite object (from cpool->resolved_references[f1])
  // Rmethod:   MH.linkToCallSite method (from f2)

  // Note:  Rcallsite is already pushed by prepare_invoke

  if (ProfileInterpreter) {
    __ profile_call(R2_tmp);
  }

  // do the call
  __ mov(Rmethod, R5_method);
  __ jump_from_interpreted(Rmethod);
}

//----------------------------------------------------------------------------------------------------
// Allocation

void TemplateTable::_new() {
  transition(vtos, atos);

  const Register Robj   = R0_tos;
  const Register Rcpool = R1_tmp;
  const Register Rindex = R2_tmp;
  const Register Rtags  = R3_tmp;
  const Register Rsize  = R3_tmp;

  Register Rklass = R4_tmp;
  assert_different_registers(Rcpool, Rindex, Rtags, Rklass, Rtemp);
  assert_different_registers(Rcpool, Rindex, Rklass, Rsize);

  Label slow_case;
  Label done;
  Label initialize_header;
  Label initialize_object;  // including clearing the fields

  const bool allow_shared_alloc =
    Universe::heap()->supports_inline_contig_alloc();

  // Literals
  InlinedAddress Lheap_top_addr(allow_shared_alloc ? (address)Universe::heap()->top_addr() : NULL);

  __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);
  __ get_cpool_and_tags(Rcpool, Rtags);

  // Make sure the class we're about to instantiate has been resolved.
  // This is done before loading InstanceKlass to be consistent with the order
  // how Constant Pool is updated (see ConstantPool::klass_at_put)
  const int tags_offset = Array<u1>::base_offset_in_bytes();
  __ add(Rtemp, Rtags, Rindex);

  __ ldrb(Rtemp, Address(Rtemp, tags_offset));

  // use Rklass as a scratch
  volatile_barrier(MacroAssembler::LoadLoad, Rklass);

  // get InstanceKlass
  __ cmp(Rtemp, JVM_CONSTANT_Class);
  __ b(slow_case, ne);
  __ load_resolved_klass_at_offset(Rcpool, Rindex, Rklass);

  // make sure klass is initialized & doesn't have finalizer
  // make sure klass is fully initialized
  __ ldrb(Rtemp, Address(Rklass, InstanceKlass::init_state_offset()));
  __ cmp(Rtemp, InstanceKlass::fully_initialized);
  __ b(slow_case, ne);

  // get instance_size in InstanceKlass (scaled to a count of bytes)
  __ ldr_u32(Rsize, Address(Rklass, Klass::layout_helper_offset()));

  // test to see if it has a finalizer or is malformed in some way
  // Klass::_lh_instance_slow_path_bit is really a bit mask, not bit number
  __ tbnz(Rsize, exact_log2(Klass::_lh_instance_slow_path_bit), slow_case);

  // Allocate the instance:
  //  If TLAB is enabled:
  //    Try to allocate in the TLAB.
  //    If fails, go to the slow path.
  //  Else If inline contiguous allocations are enabled:
  //    Try to allocate in eden.
  //    If fails due to heap end, go to slow path.
  //
  //  If TLAB is enabled OR inline contiguous is enabled:
  //    Initialize the allocation.
  //    Exit.
  //
  //  Go to slow path.
  if (UseTLAB) {
    const Register Rtlab_top = R1_tmp;
    const Register Rtlab_end = R2_tmp;
    assert_different_registers(Robj, Rsize, Rklass, Rtlab_top, Rtlab_end);

    __ tlab_allocate(Robj, Rtlab_top, Rtlab_end, Rsize, slow_case);
    if (ZeroTLAB) {
      // the fields have been already cleared
      __ b(initialize_header);
    } else {
      // initialize both the header and fields
      __ b(initialize_object);
    }
  } else {
    // Allocation in the shared Eden, if allowed.
    if (allow_shared_alloc) {
      const Register Rheap_top_addr = R2_tmp;
      const Register Rheap_top = R5_tmp;
      const Register Rheap_end = Rtemp;
      assert_different_registers(Robj, Rklass, Rsize, Rheap_top_addr, Rheap_top, Rheap_end, LR);

      __ eden_allocate(Robj, Rheap_top, Rheap_top_addr, Rheap_end, Rsize, slow_case);
    }
  }

  if (UseTLAB || allow_shared_alloc) {
    const Register Rzero0 = R1_tmp;
    const Register Rzero1 = R2_tmp;
    const Register Rzero_end = R5_tmp;
    const Register Rzero_cur = Rtemp;
    assert_different_registers(Robj, Rsize, Rklass, Rzero0, Rzero1, Rzero_cur, Rzero_end);

    // The object is initialized before the header.  If the object size is
    // zero, go directly to the header initialization.
    __ bind(initialize_object);
    __ subs(Rsize, Rsize, sizeof(oopDesc));
    __ add(Rzero_cur, Robj, sizeof(oopDesc));
    __ b(initialize_header, eq);

#ifdef ASSERT
    // make sure Rsize is a multiple of 8
    Label L;
    __ tst(Rsize, 0x07);
    __ b(L, eq);
    __ stop("object size is not multiple of 8 - adjust this code");
    __ bind(L);
#endif

    __ mov(Rzero0, 0);
    __ mov(Rzero1, 0);
    __ add(Rzero_end, Rzero_cur, Rsize);

    // initialize remaining object fields: Rsize was a multiple of 8
    { Label loop;
      // loop is unrolled 2 times
      __ bind(loop);
      // #1
      __ stmia(Rzero_cur, RegisterSet(Rzero0) | RegisterSet(Rzero1), writeback);
      __ cmp(Rzero_cur, Rzero_end);
      // #2
      __ stmia(Rzero_cur, RegisterSet(Rzero0) | RegisterSet(Rzero1), writeback, ne);
      __ cmp(Rzero_cur, Rzero_end, ne);
      __ b(loop, ne);
    }

    // initialize object header only.
    __ bind(initialize_header);
    __ mov_slow(Rtemp, (intptr_t)markWord::prototype().value());
    // mark
    __ str(Rtemp, Address(Robj, oopDesc::mark_offset_in_bytes()));

    // klass
    __ store_klass(Rklass, Robj); // blows Rklass:
    Rklass = noreg;

    // Note: Disable DTrace runtime check for now to eliminate overhead on each allocation
    if (DTraceAllocProbes) {
      // Trigger dtrace event for fastpath
      Label Lcontinue;

      __ ldrb_global(Rtemp, (address)&DTraceAllocProbes);
      __ cbz(Rtemp, Lcontinue);

      __ push(atos);
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_object_alloc), Robj);
      __ pop(atos);

      __ bind(Lcontinue);
    }

    __ b(done);
  } else {
    // jump over literals
    __ b(slow_case);
  }

  if (allow_shared_alloc) {
    __ bind_literal(Lheap_top_addr);
  }

  // slow case
  __ bind(slow_case);
  __ get_constant_pool(Rcpool);
  __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);
  __ call_VM(Robj, CAST_FROM_FN_PTR(address, InterpreterRuntime::_new), Rcpool, Rindex);

  // continue
  __ bind(done);

  // StoreStore barrier required after complete initialization
  // (headers + content zeroing), before the object may escape.
  __ membar(MacroAssembler::StoreStore, R1_tmp);
}


void TemplateTable::newarray() {
  transition(itos, atos);
  __ ldrb(R1, at_bcp(1));
  __ mov(R2, R0_tos);
  call_VM(R0_tos, CAST_FROM_FN_PTR(address, InterpreterRuntime::newarray), R1, R2);
  // MacroAssembler::StoreStore useless (included in the runtime exit path)
}


void TemplateTable::anewarray() {
  transition(itos, atos);
  __ get_unsigned_2_byte_index_at_bcp(R2, 1);
  __ get_constant_pool(R1);
  __ mov(R3, R0_tos);
  call_VM(R0_tos, CAST_FROM_FN_PTR(address, InterpreterRuntime::anewarray), R1, R2, R3);
  // MacroAssembler::StoreStore useless (included in the runtime exit path)
}


void TemplateTable::arraylength() {
  transition(atos, itos);
  __ null_check(R0_tos, Rtemp, arrayOopDesc::length_offset_in_bytes());
  __ ldr_s32(R0_tos, Address(R0_tos, arrayOopDesc::length_offset_in_bytes()));
}


void TemplateTable::checkcast() {
  transition(atos, atos);
  Label done, is_null, quicked, resolved, throw_exception;

  const Register Robj = R0_tos;
  const Register Rcpool = R2_tmp;
  const Register Rtags = R3_tmp;
  const Register Rindex = R4_tmp;
  const Register Rsuper = R3_tmp;
  const Register Rsub   = R4_tmp;
  const Register Rsubtype_check_tmp1 = R1_tmp;
  const Register Rsubtype_check_tmp2 = LR_tmp;

  __ cbz(Robj, is_null);

  // Get cpool & tags index
  __ get_cpool_and_tags(Rcpool, Rtags);
  __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);

  // See if bytecode has already been quicked
  __ add(Rtemp, Rtags, Rindex);
  __ ldrb(Rtemp, Address(Rtemp, Array<u1>::base_offset_in_bytes()));

  __ cmp(Rtemp, JVM_CONSTANT_Class);

  volatile_barrier(MacroAssembler::LoadLoad, Rtemp, true);

  __ b(quicked, eq);

  __ push(atos);
  call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  // vm_result_2 has metadata result
  __ get_vm_result_2(Rsuper, Robj);
  __ pop_ptr(Robj);
  __ b(resolved);

  __ bind(throw_exception);
  // Come here on failure of subtype check
  __ profile_typecheck_failed(R1_tmp);
  __ mov(R2_ClassCastException_obj, Robj);             // convention with generate_ClassCastException_handler()
  __ b(Interpreter::_throw_ClassCastException_entry);

  // Get superklass in Rsuper and subklass in Rsub
  __ bind(quicked);
  __ load_resolved_klass_at_offset(Rcpool, Rindex, Rsuper);

  __ bind(resolved);
  __ load_klass(Rsub, Robj);

  // Generate subtype check. Blows both tmps and Rtemp.
  assert_different_registers(Robj, Rsub, Rsuper, Rsubtype_check_tmp1, Rsubtype_check_tmp2, Rtemp);
  __ gen_subtype_check(Rsub, Rsuper, throw_exception, Rsubtype_check_tmp1, Rsubtype_check_tmp2);

  // Come here on success

  // Collect counts on whether this check-cast sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ b(done);
    __ bind(is_null);
    __ profile_null_seen(R1_tmp);
  } else {
    __ bind(is_null);   // same as 'done'
  }
  __ bind(done);
}


void TemplateTable::instanceof() {
  // result = 0: obj == NULL or  obj is not an instanceof the specified klass
  // result = 1: obj != NULL and obj is     an instanceof the specified klass

  transition(atos, itos);
  Label done, is_null, not_subtype, quicked, resolved;

  const Register Robj = R0_tos;
  const Register Rcpool = R2_tmp;
  const Register Rtags = R3_tmp;
  const Register Rindex = R4_tmp;
  const Register Rsuper = R3_tmp;
  const Register Rsub   = R4_tmp;
  const Register Rsubtype_check_tmp1 = R0_tmp;
  const Register Rsubtype_check_tmp2 = R1_tmp;

  __ cbz(Robj, is_null);

  __ load_klass(Rsub, Robj);

  // Get cpool & tags index
  __ get_cpool_and_tags(Rcpool, Rtags);
  __ get_unsigned_2_byte_index_at_bcp(Rindex, 1);

  // See if bytecode has already been quicked
  __ add(Rtemp, Rtags, Rindex);
  __ ldrb(Rtemp, Address(Rtemp, Array<u1>::base_offset_in_bytes()));
  __ cmp(Rtemp, JVM_CONSTANT_Class);

  volatile_barrier(MacroAssembler::LoadLoad, Rtemp, true);

  __ b(quicked, eq);

  __ push(atos);
  call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  // vm_result_2 has metadata result
  __ get_vm_result_2(Rsuper, Robj);
  __ pop_ptr(Robj);
  __ b(resolved);

  // Get superklass in Rsuper and subklass in Rsub
  __ bind(quicked);
  __ load_resolved_klass_at_offset(Rcpool, Rindex, Rsuper);

  __ bind(resolved);
  __ load_klass(Rsub, Robj);

  // Generate subtype check. Blows both tmps and Rtemp.
  __ gen_subtype_check(Rsub, Rsuper, not_subtype, Rsubtype_check_tmp1, Rsubtype_check_tmp2);

  // Come here on success
  __ mov(R0_tos, 1);
  __ b(done);

  __ bind(not_subtype);
  // Come here on failure
  __ profile_typecheck_failed(R1_tmp);
  __ mov(R0_tos, 0);

  // Collect counts on whether this test sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ b(done);
    __ bind(is_null);
    __ profile_null_seen(R1_tmp);
  } else {
    __ bind(is_null);   // same as 'done'
  }
  __ bind(done);
}


//----------------------------------------------------------------------------------------------------
// Breakpoints
void TemplateTable::_breakpoint() {

  // Note: We get here even if we are single stepping..
  // jbug inists on setting breakpoints at every bytecode
  // even if we are in single step mode.

  transition(vtos, vtos);

  // get the unpatched byte code
  __ mov(R1, Rmethod);
  __ mov(R2, Rbcp);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::get_original_bytecode_at), R1, R2);
  __ mov(Rtmp_save0, R0);

  // post the breakpoint event
  __ mov(R1, Rmethod);
  __ mov(R2, Rbcp);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::_breakpoint), R1, R2);

  // complete the execution of original bytecode
  __ mov(R3_bytecode, Rtmp_save0);
  __ dispatch_only_normal(vtos);
}


//----------------------------------------------------------------------------------------------------
// Exceptions

void TemplateTable::athrow() {
  transition(atos, vtos);
  __ mov(Rexception_obj, R0_tos);
  __ null_check(Rexception_obj, Rtemp);
  __ b(Interpreter::throw_exception_entry());
}


//----------------------------------------------------------------------------------------------------
// Synchronization
//
// Note: monitorenter & exit are symmetric routines; which is reflected
//       in the assembly code structure as well
//
// Stack layout:
//
// [expressions  ] <--- Rstack_top        = expression stack top
// ..
// [expressions  ]
// [monitor entry] <--- monitor block top = expression stack bot
// ..
// [monitor entry]
// [frame data   ] <--- monitor block bot
// ...
// [saved FP     ] <--- FP


void TemplateTable::monitorenter() {
  transition(atos, vtos);

  const Register Robj = R0_tos;
  const Register Rentry = R1_tmp;

  // check for NULL object
  __ null_check(Robj, Rtemp);

  const int entry_size = (frame::interpreter_frame_monitor_size() * wordSize);
  assert (entry_size % StackAlignmentInBytes == 0, "keep stack alignment");
  Label allocate_monitor, allocated;

  // initialize entry pointer
  __ mov(Rentry, 0);                             // points to free slot or NULL

  // find a free slot in the monitor block (result in Rentry)
  { Label loop, exit;
    const Register Rcur = R2_tmp;
    const Register Rcur_obj = Rtemp;
    const Register Rbottom = R3_tmp;
    assert_different_registers(Robj, Rentry, Rcur, Rbottom, Rcur_obj);

    __ ldr(Rcur, Address(FP, frame::interpreter_frame_monitor_block_top_offset * wordSize));
                                 // points to current entry, starting with top-most entry
    __ sub(Rbottom, FP, -frame::interpreter_frame_monitor_block_bottom_offset * wordSize);
                                 // points to word before bottom of monitor block

    __ cmp(Rcur, Rbottom);                       // check if there are no monitors
    __ ldr(Rcur_obj, Address(Rcur, BasicObjectLock::obj_offset_in_bytes()), ne);
                                                 // prefetch monitor's object for the first iteration
    __ b(allocate_monitor, eq);                  // there are no monitors, skip searching

    __ bind(loop);
    __ cmp(Rcur_obj, 0);                         // check if current entry is used
    __ mov(Rentry, Rcur, eq);                    // if not used then remember entry

    __ cmp(Rcur_obj, Robj);                      // check if current entry is for same object
    __ b(exit, eq);                              // if same object then stop searching

    __ add(Rcur, Rcur, entry_size);              // otherwise advance to next entry

    __ cmp(Rcur, Rbottom);                       // check if bottom reached
    __ ldr(Rcur_obj, Address(Rcur, BasicObjectLock::obj_offset_in_bytes()), ne);
                                                 // prefetch monitor's object for the next iteration
    __ b(loop, ne);                              // if not at bottom then check this entry
    __ bind(exit);
  }

  __ cbnz(Rentry, allocated);                    // check if a slot has been found; if found, continue with that one

  __ bind(allocate_monitor);

  // allocate one if there's no free slot
  { Label loop;
    assert_different_registers(Robj, Rentry, R2_tmp, Rtemp);

    // 1. compute new pointers


    __ ldr(Rentry, Address(FP, frame::interpreter_frame_monitor_block_top_offset * wordSize));
                                                 // old monitor block top / expression stack bottom

    __ sub(Rstack_top, Rstack_top, entry_size);  // move expression stack top
    __ check_stack_top_on_expansion();

    __ sub(Rentry, Rentry, entry_size);          // move expression stack bottom

    __ mov(R2_tmp, Rstack_top);                  // set start value for copy loop

    __ str(Rentry, Address(FP, frame::interpreter_frame_monitor_block_top_offset * wordSize));
                                                 // set new monitor block top

    // 2. move expression stack contents

    __ cmp(R2_tmp, Rentry);                                 // check if expression stack is empty
    __ ldr(Rtemp, Address(R2_tmp, entry_size), ne);         // load expression stack word from old location
    __ b(allocated, eq);

    __ bind(loop);
    __ str(Rtemp, Address(R2_tmp, wordSize, post_indexed)); // store expression stack word at new location
                                                            // and advance to next word
    __ cmp(R2_tmp, Rentry);                                 // check if bottom reached
    __ ldr(Rtemp, Address(R2, entry_size), ne);             // load expression stack word from old location
    __ b(loop, ne);                                         // if not at bottom then copy next word
  }

  // call run-time routine

  // Rentry: points to monitor entry
  __ bind(allocated);

  // Increment bcp to point to the next bytecode, so exception handling for async. exceptions work correctly.
  // The object has already been poped from the stack, so the expression stack looks correct.
  __ add(Rbcp, Rbcp, 1);

  __ str(Robj, Address(Rentry, BasicObjectLock::obj_offset_in_bytes()));     // store object
  __ lock_object(Rentry);

  // check to make sure this monitor doesn't cause stack overflow after locking
  __ save_bcp();  // in case of exception
  __ arm_stack_overflow_check(0, Rtemp);

  // The bcp has already been incremented. Just need to dispatch to next instruction.
  __ dispatch_next(vtos);
}


void TemplateTable::monitorexit() {
  transition(atos, vtos);

  const Register Robj = R0_tos;
  const Register Rcur = R1_tmp;
  const Register Rbottom = R2_tmp;
  const Register Rcur_obj = Rtemp;
  const Register Rmonitor = R0;      // fixed in unlock_object()

  // check for NULL object
  __ null_check(Robj, Rtemp);

  const int entry_size = (frame::interpreter_frame_monitor_size() * wordSize);
  Label found, throw_exception;

  // find matching slot
  { Label loop;
    assert_different_registers(Robj, Rcur, Rbottom, Rcur_obj);

    __ ldr(Rcur, Address(FP, frame::interpreter_frame_monitor_block_top_offset * wordSize));
                                 // points to current entry, starting with top-most entry
    __ sub(Rbottom, FP, -frame::interpreter_frame_monitor_block_bottom_offset * wordSize);
                                 // points to word before bottom of monitor block

    __ cmp(Rcur, Rbottom);                       // check if bottom reached
    __ ldr(Rcur_obj, Address(Rcur, BasicObjectLock::obj_offset_in_bytes()), ne);
                                                 // prefetch monitor's object for the first iteration
    __ b(throw_exception, eq);                   // throw exception if there are now monitors

    __ bind(loop);
    // check if current entry is for same object
    __ cmp(Rcur_obj, Robj);
    __ b(found, eq);                             // if same object then stop searching
    __ add(Rcur, Rcur, entry_size);              // otherwise advance to next entry
    __ cmp(Rcur, Rbottom);                       // check if bottom reached
    __ ldr(Rcur_obj, Address(Rcur, BasicObjectLock::obj_offset_in_bytes()), ne);
    __ b (loop, ne);                             // if not at bottom then check this entry
  }

  // error handling. Unlocking was not block-structured
  __ bind(throw_exception);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_illegal_monitor_state_exception));
  __ should_not_reach_here();

  // call run-time routine
  // Rcur: points to monitor entry
  __ bind(found);
  __ push_ptr(Robj);                             // make sure object is on stack (contract with oopMaps)
  __ mov(Rmonitor, Rcur);
  __ unlock_object(Rmonitor);
  __ pop_ptr(Robj);                              // discard object
}


//----------------------------------------------------------------------------------------------------
// Wide instructions

void TemplateTable::wide() {
  transition(vtos, vtos);
  __ ldrb(R3_bytecode, at_bcp(1));

  InlinedAddress Ltable((address)Interpreter::_wentry_point);
  __ ldr_literal(Rtemp, Ltable);
  __ indirect_jump(Address::indexed_ptr(Rtemp, R3_bytecode), Rtemp);

  __ nop(); // to avoid filling CPU pipeline with invalid instructions
  __ nop();
  __ bind_literal(Ltable);
}


//----------------------------------------------------------------------------------------------------
// Multi arrays

void TemplateTable::multianewarray() {
  transition(vtos, atos);
  __ ldrb(Rtmp_save0, at_bcp(3));   // get number of dimensions

  // last dim is on top of stack; we want address of first one:
  // first_addr = last_addr + ndims * stackElementSize - 1*wordsize
  // the latter wordSize to point to the beginning of the array.
  __ add(Rtemp, Rstack_top, AsmOperand(Rtmp_save0, lsl, Interpreter::logStackElementSize));
  __ sub(R1, Rtemp, wordSize);

  call_VM(R0, CAST_FROM_FN_PTR(address, InterpreterRuntime::multianewarray), R1);
  __ add(Rstack_top, Rstack_top, AsmOperand(Rtmp_save0, lsl, Interpreter::logStackElementSize));
  // MacroAssembler::StoreStore useless (included in the runtime exit path)
}
