/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, Red Hat Inc. All rights reserved.
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
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateTable.hpp"
#include "memory/universe.hpp"
#include "oops/methodData.hpp"
#include "oops/method.hpp"
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

// Address computation: local variables

static inline Address iaddress(int n) {
  return Address(rlocals, Interpreter::local_offset_in_bytes(n));
}

static inline Address laddress(int n) {
  return iaddress(n + 1);
}

static inline Address faddress(int n) {
  return iaddress(n);
}

static inline Address daddress(int n) {
  return laddress(n);
}

static inline Address aaddress(int n) {
  return iaddress(n);
}

static inline Address iaddress(Register r) {
  return Address(rlocals, r, Address::lsl(3));
}

static inline Address laddress(Register r, Register scratch,
                               InterpreterMacroAssembler* _masm) {
  __ lea(scratch, Address(rlocals, r, Address::lsl(3)));
  return Address(scratch, Interpreter::local_offset_in_bytes(1));
}

static inline Address faddress(Register r) {
  return iaddress(r);
}

static inline Address daddress(Register r, Register scratch,
                               InterpreterMacroAssembler* _masm) {
  return laddress(r, scratch, _masm);
}

static inline Address aaddress(Register r) {
  return iaddress(r);
}

static inline Address at_rsp() {
  return Address(esp, 0);
}

// At top of Java expression stack which may be different than esp().  It
// isn't for category 1 objects.
static inline Address at_tos   () {
  return Address(esp,  Interpreter::expr_offset_in_bytes(0));
}

static inline Address at_tos_p1() {
  return Address(esp,  Interpreter::expr_offset_in_bytes(1));
}

static inline Address at_tos_p2() {
  return Address(esp,  Interpreter::expr_offset_in_bytes(2));
}

static inline Address at_tos_p3() {
  return Address(esp,  Interpreter::expr_offset_in_bytes(3));
}

static inline Address at_tos_p4() {
  return Address(esp,  Interpreter::expr_offset_in_bytes(4));
}

static inline Address at_tos_p5() {
  return Address(esp,  Interpreter::expr_offset_in_bytes(5));
}

// Condition conversion
static Assembler::Condition j_not(TemplateTable::Condition cc) {
  switch (cc) {
  case TemplateTable::equal        : return Assembler::NE;
  case TemplateTable::not_equal    : return Assembler::EQ;
  case TemplateTable::less         : return Assembler::GE;
  case TemplateTable::less_equal   : return Assembler::GT;
  case TemplateTable::greater      : return Assembler::LE;
  case TemplateTable::greater_equal: return Assembler::LT;
  }
  ShouldNotReachHere();
  return Assembler::EQ;
}


// Miscelaneous helper routines
// Store an oop (or NULL) at the Address described by obj.
// If val == noreg this means store a NULL
static void do_oop_store(InterpreterMacroAssembler* _masm,
                         Address dst,
                         Register val,
                         DecoratorSet decorators) {
  assert(val == noreg || val == r0, "parameter is just for looks");
  __ store_heap_oop(dst, val, r10, r1, decorators);
}

static void do_oop_load(InterpreterMacroAssembler* _masm,
                        Address src,
                        Register dst,
                        DecoratorSet decorators) {
  __ load_heap_oop(dst, src, r10, r1, decorators);
}

Address TemplateTable::at_bcp(int offset) {
  assert(_desc->uses_bcp(), "inconsistent uses_bcp information");
  return Address(rbcp, offset);
}

void TemplateTable::patch_bytecode(Bytecodes::Code bc, Register bc_reg,
                                   Register temp_reg, bool load_bc_into_bc_reg/*=true*/,
                                   int byte_no)
{
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
      __ get_cache_and_index_and_bytecode_at_bcp(temp_reg, bc_reg, temp_reg, byte_no, 1);
      __ movw(bc_reg, bc);
      __ cbzw(temp_reg, L_patch_done);  // don't patch
    }
    break;
  default:
    assert(byte_no == -1, "sanity");
    // the pair bytecodes have already done the load.
    if (load_bc_into_bc_reg) {
      __ movw(bc_reg, bc);
    }
  }

  if (JvmtiExport::can_post_breakpoint()) {
    Label L_fast_patch;
    // if a breakpoint is present we can't rewrite the stream directly
    __ load_unsigned_byte(temp_reg, at_bcp(0));
    __ cmpw(temp_reg, Bytecodes::_breakpoint);
    __ br(Assembler::NE, L_fast_patch);
    // Let breakpoint table handling rewrite to quicker bytecode
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::set_original_bytecode_at), rmethod, rbcp, bc_reg);
    __ b(L_patch_done);
    __ bind(L_fast_patch);
  }

#ifdef ASSERT
  Label L_okay;
  __ load_unsigned_byte(temp_reg, at_bcp(0));
  __ cmpw(temp_reg, (int) Bytecodes::java_code(bc));
  __ br(Assembler::EQ, L_okay);
  __ cmpw(temp_reg, bc_reg);
  __ br(Assembler::EQ, L_okay);
  __ stop("patching the wrong bytecode");
  __ bind(L_okay);
#endif

  // patch bytecode
  __ strb(bc_reg, at_bcp(0));
  __ bind(L_patch_done);
}


// Individual instructions

void TemplateTable::nop() {
  transition(vtos, vtos);
  // nothing to do
}

void TemplateTable::shouldnotreachhere() {
  transition(vtos, vtos);
  __ stop("shouldnotreachhere bytecode");
}

void TemplateTable::aconst_null()
{
  transition(vtos, atos);
  __ mov(r0, 0);
}

void TemplateTable::iconst(int value)
{
  transition(vtos, itos);
  __ mov(r0, value);
}

void TemplateTable::lconst(int value)
{
  __ mov(r0, value);
}

void TemplateTable::fconst(int value)
{
  transition(vtos, ftos);
  switch (value) {
  case 0:
    __ fmovs(v0, 0.0);
    break;
  case 1:
    __ fmovs(v0, 1.0);
    break;
  case 2:
    __ fmovs(v0, 2.0);
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void TemplateTable::dconst(int value)
{
  transition(vtos, dtos);
  switch (value) {
  case 0:
    __ fmovd(v0, 0.0);
    break;
  case 1:
    __ fmovd(v0, 1.0);
    break;
  case 2:
    __ fmovd(v0, 2.0);
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void TemplateTable::bipush()
{
  transition(vtos, itos);
  __ load_signed_byte32(r0, at_bcp(1));
}

void TemplateTable::sipush()
{
  transition(vtos, itos);
  __ load_unsigned_short(r0, at_bcp(1));
  __ revw(r0, r0);
  __ asrw(r0, r0, 16);
}

void TemplateTable::ldc(bool wide)
{
  transition(vtos, vtos);
  Label call_ldc, notFloat, notClass, notInt, Done;

  if (wide) {
    __ get_unsigned_2_byte_index_at_bcp(r1, 1);
  } else {
    __ load_unsigned_byte(r1, at_bcp(1));
  }
  __ get_cpool_and_tags(r2, r0);

  const int base_offset = ConstantPool::header_size() * wordSize;
  const int tags_offset = Array<u1>::base_offset_in_bytes();

  // get type
  __ add(r3, r1, tags_offset);
  __ lea(r3, Address(r0, r3));
  __ ldarb(r3, r3);

  // unresolved class - get the resolved class
  __ cmp(r3, (u1)JVM_CONSTANT_UnresolvedClass);
  __ br(Assembler::EQ, call_ldc);

  // unresolved class in error state - call into runtime to throw the error
  // from the first resolution attempt
  __ cmp(r3, (u1)JVM_CONSTANT_UnresolvedClassInError);
  __ br(Assembler::EQ, call_ldc);

  // resolved class - need to call vm to get java mirror of the class
  __ cmp(r3, (u1)JVM_CONSTANT_Class);
  __ br(Assembler::NE, notClass);

  __ bind(call_ldc);
  __ mov(c_rarg1, wide);
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::ldc), c_rarg1);
  __ push_ptr(r0);
  __ verify_oop(r0);
  __ b(Done);

  __ bind(notClass);
  __ cmp(r3, (u1)JVM_CONSTANT_Float);
  __ br(Assembler::NE, notFloat);
  // ftos
  __ adds(r1, r2, r1, Assembler::LSL, 3);
  __ ldrs(v0, Address(r1, base_offset));
  __ push_f();
  __ b(Done);

  __ bind(notFloat);

  __ cmp(r3, (u1)JVM_CONSTANT_Integer);
  __ br(Assembler::NE, notInt);

  // itos
  __ adds(r1, r2, r1, Assembler::LSL, 3);
  __ ldrw(r0, Address(r1, base_offset));
  __ push_i(r0);
  __ b(Done);

  __ bind(notInt);
  condy_helper(Done);

  __ bind(Done);
}

// Fast path for caching oop constants.
void TemplateTable::fast_aldc(bool wide)
{
  transition(vtos, atos);

  Register result = r0;
  Register tmp = r1;
  Register rarg = r2;

  int index_size = wide ? sizeof(u2) : sizeof(u1);

  Label resolved;

  // We are resolved if the resolved reference cache entry contains a
  // non-null object (String, MethodType, etc.)
  assert_different_registers(result, tmp);
  __ get_cache_index_at_bcp(tmp, 1, index_size);
  __ load_resolved_reference_at_index(result, tmp);
  __ cbnz(result, resolved);

  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc);

  // first time invocation - must resolve first
  __ mov(rarg, (int)bytecode());
  __ call_VM(result, entry, rarg);

  __ bind(resolved);

  { // Check for the null sentinel.
    // If we just called the VM, it already did the mapping for us,
    // but it's harmless to retry.
    Label notNull;

    // Stash null_sentinel address to get its value later
    __ movptr(rarg, (uintptr_t)Universe::the_null_sentinel_addr());
    __ ldr(tmp, Address(rarg));
    __ resolve_oop_handle(tmp);
    __ cmpoop(result, tmp);
    __ br(Assembler::NE, notNull);
    __ mov(result, 0);  // NULL object reference
    __ bind(notNull);
  }

  if (VerifyOops) {
    // Safe to call with 0 result
    __ verify_oop(result);
  }
}

void TemplateTable::ldc2_w()
{
  transition(vtos, vtos);
  Label notDouble, notLong, Done;
  __ get_unsigned_2_byte_index_at_bcp(r0, 1);

  __ get_cpool_and_tags(r1, r2);
  const int base_offset = ConstantPool::header_size() * wordSize;
  const int tags_offset = Array<u1>::base_offset_in_bytes();

  // get type
  __ lea(r2, Address(r2, r0, Address::lsl(0)));
  __ load_unsigned_byte(r2, Address(r2, tags_offset));
  __ cmpw(r2, (int)JVM_CONSTANT_Double);
  __ br(Assembler::NE, notDouble);

  // dtos
  __ lea (r2, Address(r1, r0, Address::lsl(3)));
  __ ldrd(v0, Address(r2, base_offset));
  __ push_d();
  __ b(Done);

  __ bind(notDouble);
  __ cmpw(r2, (int)JVM_CONSTANT_Long);
  __ br(Assembler::NE, notLong);

  // ltos
  __ lea(r0, Address(r1, r0, Address::lsl(3)));
  __ ldr(r0, Address(r0, base_offset));
  __ push_l();
  __ b(Done);

  __ bind(notLong);
  condy_helper(Done);

  __ bind(Done);
}

void TemplateTable::condy_helper(Label& Done)
{
  Register obj = r0;
  Register rarg = r1;
  Register flags = r2;
  Register off = r3;

  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc);

  __ mov(rarg, (int) bytecode());
  __ call_VM(obj, entry, rarg);

  __ get_vm_result_2(flags, rthread);

  // VMr = obj = base address to find primitive value to push
  // VMr2 = flags = (tos, off) using format of CPCE::_flags
  __ mov(off, flags);
  __ andw(off, off, ConstantPoolCacheEntry::field_index_mask);

  const Address field(obj, off);

  // What sort of thing are we loading?
  // x86 uses a shift and mask or wings it with a shift plus assert
  // the mask is not needed. aarch64 just uses bitfield extract
  __ ubfxw(flags, flags, ConstantPoolCacheEntry::tos_state_shift,
           ConstantPoolCacheEntry::tos_state_bits);

  switch (bytecode()) {
    case Bytecodes::_ldc:
    case Bytecodes::_ldc_w:
      {
        // tos in (itos, ftos, stos, btos, ctos, ztos)
        Label notInt, notFloat, notShort, notByte, notChar, notBool;
        __ cmpw(flags, itos);
        __ br(Assembler::NE, notInt);
        // itos
        __ ldrw(r0, field);
        __ push(itos);
        __ b(Done);

        __ bind(notInt);
        __ cmpw(flags, ftos);
        __ br(Assembler::NE, notFloat);
        // ftos
        __ load_float(field);
        __ push(ftos);
        __ b(Done);

        __ bind(notFloat);
        __ cmpw(flags, stos);
        __ br(Assembler::NE, notShort);
        // stos
        __ load_signed_short(r0, field);
        __ push(stos);
        __ b(Done);

        __ bind(notShort);
        __ cmpw(flags, btos);
        __ br(Assembler::NE, notByte);
        // btos
        __ load_signed_byte(r0, field);
        __ push(btos);
        __ b(Done);

        __ bind(notByte);
        __ cmpw(flags, ctos);
        __ br(Assembler::NE, notChar);
        // ctos
        __ load_unsigned_short(r0, field);
        __ push(ctos);
        __ b(Done);

        __ bind(notChar);
        __ cmpw(flags, ztos);
        __ br(Assembler::NE, notBool);
        // ztos
        __ load_signed_byte(r0, field);
        __ push(ztos);
        __ b(Done);

        __ bind(notBool);
        break;
      }

    case Bytecodes::_ldc2_w:
      {
        Label notLong, notDouble;
        __ cmpw(flags, ltos);
        __ br(Assembler::NE, notLong);
        // ltos
        __ ldr(r0, field);
        __ push(ltos);
        __ b(Done);

        __ bind(notLong);
        __ cmpw(flags, dtos);
        __ br(Assembler::NE, notDouble);
        // dtos
        __ load_double(field);
        __ push(dtos);
        __ b(Done);

       __ bind(notDouble);
        break;
      }

    default:
      ShouldNotReachHere();
    }

    __ stop("bad ldc/condy");
}

void TemplateTable::locals_index(Register reg, int offset)
{
  __ ldrb(reg, at_bcp(offset));
  __ neg(reg, reg);
}

void TemplateTable::iload() {
  iload_internal();
}

void TemplateTable::nofast_iload() {
  iload_internal(may_not_rewrite);
}

void TemplateTable::iload_internal(RewriteControl rc) {
  transition(vtos, itos);
  if (RewriteFrequentPairs && rc == may_rewrite) {
    Label rewrite, done;
    Register bc = r4;

    // get next bytecode
    __ load_unsigned_byte(r1, at_bcp(Bytecodes::length_for(Bytecodes::_iload)));

    // if _iload, wait to rewrite to iload2.  We only want to rewrite the
    // last two iloads in a pair.  Comparing against fast_iload means that
    // the next bytecode is neither an iload or a caload, and therefore
    // an iload pair.
    __ cmpw(r1, Bytecodes::_iload);
    __ br(Assembler::EQ, done);

    // if _fast_iload rewrite to _fast_iload2
    __ cmpw(r1, Bytecodes::_fast_iload);
    __ movw(bc, Bytecodes::_fast_iload2);
    __ br(Assembler::EQ, rewrite);

    // if _caload rewrite to _fast_icaload
    __ cmpw(r1, Bytecodes::_caload);
    __ movw(bc, Bytecodes::_fast_icaload);
    __ br(Assembler::EQ, rewrite);

    // else rewrite to _fast_iload
    __ movw(bc, Bytecodes::_fast_iload);

    // rewrite
    // bc: new bytecode
    __ bind(rewrite);
    patch_bytecode(Bytecodes::_iload, bc, r1, false);
    __ bind(done);

  }

  // do iload, get the local value into tos
  locals_index(r1);
  __ ldr(r0, iaddress(r1));

}

void TemplateTable::fast_iload2()
{
  transition(vtos, itos);
  locals_index(r1);
  __ ldr(r0, iaddress(r1));
  __ push(itos);
  locals_index(r1, 3);
  __ ldr(r0, iaddress(r1));
}

void TemplateTable::fast_iload()
{
  transition(vtos, itos);
  locals_index(r1);
  __ ldr(r0, iaddress(r1));
}

void TemplateTable::lload()
{
  transition(vtos, ltos);
  __ ldrb(r1, at_bcp(1));
  __ sub(r1, rlocals, r1, ext::uxtw, LogBytesPerWord);
  __ ldr(r0, Address(r1, Interpreter::local_offset_in_bytes(1)));
}

void TemplateTable::fload()
{
  transition(vtos, ftos);
  locals_index(r1);
  // n.b. we use ldrd here because this is a 64 bit slot
  // this is comparable to the iload case
  __ ldrd(v0, faddress(r1));
}

void TemplateTable::dload()
{
  transition(vtos, dtos);
  __ ldrb(r1, at_bcp(1));
  __ sub(r1, rlocals, r1, ext::uxtw, LogBytesPerWord);
  __ ldrd(v0, Address(r1, Interpreter::local_offset_in_bytes(1)));
}

void TemplateTable::aload()
{
  transition(vtos, atos);
  locals_index(r1);
  __ ldr(r0, iaddress(r1));
}

void TemplateTable::locals_index_wide(Register reg) {
  __ ldrh(reg, at_bcp(2));
  __ rev16w(reg, reg);
  __ neg(reg, reg);
}

void TemplateTable::wide_iload() {
  transition(vtos, itos);
  locals_index_wide(r1);
  __ ldr(r0, iaddress(r1));
}

void TemplateTable::wide_lload()
{
  transition(vtos, ltos);
  __ ldrh(r1, at_bcp(2));
  __ rev16w(r1, r1);
  __ sub(r1, rlocals, r1, ext::uxtw, LogBytesPerWord);
  __ ldr(r0, Address(r1, Interpreter::local_offset_in_bytes(1)));
}

void TemplateTable::wide_fload()
{
  transition(vtos, ftos);
  locals_index_wide(r1);
  // n.b. we use ldrd here because this is a 64 bit slot
  // this is comparable to the iload case
  __ ldrd(v0, faddress(r1));
}

void TemplateTable::wide_dload()
{
  transition(vtos, dtos);
  __ ldrh(r1, at_bcp(2));
  __ rev16w(r1, r1);
  __ sub(r1, rlocals, r1, ext::uxtw, LogBytesPerWord);
  __ ldrd(v0, Address(r1, Interpreter::local_offset_in_bytes(1)));
}

void TemplateTable::wide_aload()
{
  transition(vtos, atos);
  locals_index_wide(r1);
  __ ldr(r0, aaddress(r1));
}

void TemplateTable::index_check(Register array, Register index)
{
  // destroys r1, rscratch1
  // check array
  __ null_check(array, arrayOopDesc::length_offset_in_bytes());
  // sign extend index for use by indexed load
  // __ movl2ptr(index, index);
  // check index
  Register length = rscratch1;
  __ ldrw(length, Address(array, arrayOopDesc::length_offset_in_bytes()));
  __ cmpw(index, length);
  if (index != r1) {
    // ??? convention: move aberrant index into r1 for exception message
    assert(r1 != array, "different registers");
    __ mov(r1, index);
  }
  Label ok;
  __ br(Assembler::LO, ok);
    // ??? convention: move array into r3 for exception message
  __ mov(r3, array);
  __ mov(rscratch1, Interpreter::_throw_ArrayIndexOutOfBoundsException_entry);
  __ br(rscratch1);
  __ bind(ok);
}

void TemplateTable::iaload()
{
  transition(itos, itos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_INT) >> 2);
  __ access_load_at(T_INT, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(2)), noreg, noreg);
}

void TemplateTable::laload()
{
  transition(itos, ltos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_LONG) >> 3);
  __ access_load_at(T_LONG, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(3)), noreg, noreg);
}

void TemplateTable::faload()
{
  transition(itos, ftos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_FLOAT) >> 2);
  __ access_load_at(T_FLOAT, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(2)), noreg, noreg);
}

void TemplateTable::daload()
{
  transition(itos, dtos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_DOUBLE) >> 3);
  __ access_load_at(T_DOUBLE, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(3)), noreg, noreg);
}

void TemplateTable::aaload()
{
  transition(itos, atos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_OBJECT) >> LogBytesPerHeapOop);
  do_oop_load(_masm,
              Address(r0, r1, Address::uxtw(LogBytesPerHeapOop)),
              r0,
              IS_ARRAY);
}

void TemplateTable::baload()
{
  transition(itos, itos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_BYTE) >> 0);
  __ access_load_at(T_BYTE, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(0)), noreg, noreg);
}

void TemplateTable::caload()
{
  transition(itos, itos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_CHAR) >> 1);
  __ access_load_at(T_CHAR, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(1)), noreg, noreg);
}

// iload followed by caload frequent pair
void TemplateTable::fast_icaload()
{
  transition(vtos, itos);
  // load index out of locals
  locals_index(r2);
  __ ldr(r1, iaddress(r2));

  __ pop_ptr(r0);

  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_CHAR) >> 1);
  __ access_load_at(T_CHAR, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(1)), noreg, noreg);
}

void TemplateTable::saload()
{
  transition(itos, itos);
  __ mov(r1, r0);
  __ pop_ptr(r0);
  // r0: array
  // r1: index
  index_check(r0, r1); // leaves index in r1, kills rscratch1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_SHORT) >> 1);
  __ access_load_at(T_SHORT, IN_HEAP | IS_ARRAY, r0, Address(r0, r1, Address::uxtw(1)), noreg, noreg);
}

void TemplateTable::iload(int n)
{
  transition(vtos, itos);
  __ ldr(r0, iaddress(n));
}

void TemplateTable::lload(int n)
{
  transition(vtos, ltos);
  __ ldr(r0, laddress(n));
}

void TemplateTable::fload(int n)
{
  transition(vtos, ftos);
  __ ldrs(v0, faddress(n));
}

void TemplateTable::dload(int n)
{
  transition(vtos, dtos);
  __ ldrd(v0, daddress(n));
}

void TemplateTable::aload(int n)
{
  transition(vtos, atos);
  __ ldr(r0, iaddress(n));
}

void TemplateTable::aload_0() {
  aload_0_internal();
}

void TemplateTable::nofast_aload_0() {
  aload_0_internal(may_not_rewrite);
}

void TemplateTable::aload_0_internal(RewriteControl rc) {
  // According to bytecode histograms, the pairs:
  //
  // _aload_0, _fast_igetfield
  // _aload_0, _fast_agetfield
  // _aload_0, _fast_fgetfield
  //
  // occur frequently. If RewriteFrequentPairs is set, the (slow)
  // _aload_0 bytecode checks if the next bytecode is either
  // _fast_igetfield, _fast_agetfield or _fast_fgetfield and then
  // rewrites the current bytecode into a pair bytecode; otherwise it
  // rewrites the current bytecode into _fast_aload_0 that doesn't do
  // the pair check anymore.
  //
  // Note: If the next bytecode is _getfield, the rewrite must be
  //       delayed, otherwise we may miss an opportunity for a pair.
  //
  // Also rewrite frequent pairs
  //   aload_0, aload_1
  //   aload_0, iload_1
  // These bytecodes with a small amount of code are most profitable
  // to rewrite
  if (RewriteFrequentPairs && rc == may_rewrite) {
    Label rewrite, done;
    const Register bc = r4;

    // get next bytecode
    __ load_unsigned_byte(r1, at_bcp(Bytecodes::length_for(Bytecodes::_aload_0)));

    // if _getfield then wait with rewrite
    __ cmpw(r1, Bytecodes::Bytecodes::_getfield);
    __ br(Assembler::EQ, done);

    // if _igetfield then rewrite to _fast_iaccess_0
    assert(Bytecodes::java_code(Bytecodes::_fast_iaccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ cmpw(r1, Bytecodes::_fast_igetfield);
    __ movw(bc, Bytecodes::_fast_iaccess_0);
    __ br(Assembler::EQ, rewrite);

    // if _agetfield then rewrite to _fast_aaccess_0
    assert(Bytecodes::java_code(Bytecodes::_fast_aaccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ cmpw(r1, Bytecodes::_fast_agetfield);
    __ movw(bc, Bytecodes::_fast_aaccess_0);
    __ br(Assembler::EQ, rewrite);

    // if _fgetfield then rewrite to _fast_faccess_0
    assert(Bytecodes::java_code(Bytecodes::_fast_faccess_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ cmpw(r1, Bytecodes::_fast_fgetfield);
    __ movw(bc, Bytecodes::_fast_faccess_0);
    __ br(Assembler::EQ, rewrite);

    // else rewrite to _fast_aload0
    assert(Bytecodes::java_code(Bytecodes::_fast_aload_0) == Bytecodes::_aload_0, "fix bytecode definition");
    __ movw(bc, Bytecodes::Bytecodes::_fast_aload_0);

    // rewrite
    // bc: new bytecode
    __ bind(rewrite);
    patch_bytecode(Bytecodes::_aload_0, bc, r1, false);

    __ bind(done);
  }

  // Do actual aload_0 (must do this after patch_bytecode which might call VM and GC might change oop).
  aload(0);
}

void TemplateTable::istore()
{
  transition(itos, vtos);
  locals_index(r1);
  // FIXME: We're being very pernickerty here storing a jint in a
  // local with strw, which costs an extra instruction over what we'd
  // be able to do with a simple str.  We should just store the whole
  // word.
  __ lea(rscratch1, iaddress(r1));
  __ strw(r0, Address(rscratch1));
}

void TemplateTable::lstore()
{
  transition(ltos, vtos);
  locals_index(r1);
  __ str(r0, laddress(r1, rscratch1, _masm));
}

void TemplateTable::fstore() {
  transition(ftos, vtos);
  locals_index(r1);
  __ lea(rscratch1, iaddress(r1));
  __ strs(v0, Address(rscratch1));
}

void TemplateTable::dstore() {
  transition(dtos, vtos);
  locals_index(r1);
  __ strd(v0, daddress(r1, rscratch1, _masm));
}

void TemplateTable::astore()
{
  transition(vtos, vtos);
  __ pop_ptr(r0);
  locals_index(r1);
  __ str(r0, aaddress(r1));
}

void TemplateTable::wide_istore() {
  transition(vtos, vtos);
  __ pop_i();
  locals_index_wide(r1);
  __ lea(rscratch1, iaddress(r1));
  __ strw(r0, Address(rscratch1));
}

void TemplateTable::wide_lstore() {
  transition(vtos, vtos);
  __ pop_l();
  locals_index_wide(r1);
  __ str(r0, laddress(r1, rscratch1, _masm));
}

void TemplateTable::wide_fstore() {
  transition(vtos, vtos);
  __ pop_f();
  locals_index_wide(r1);
  __ lea(rscratch1, faddress(r1));
  __ strs(v0, rscratch1);
}

void TemplateTable::wide_dstore() {
  transition(vtos, vtos);
  __ pop_d();
  locals_index_wide(r1);
  __ strd(v0, daddress(r1, rscratch1, _masm));
}

void TemplateTable::wide_astore() {
  transition(vtos, vtos);
  __ pop_ptr(r0);
  locals_index_wide(r1);
  __ str(r0, aaddress(r1));
}

void TemplateTable::iastore() {
  transition(itos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // r0: value
  // r1: index
  // r3: array
  index_check(r3, r1); // prefer index in r1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_INT) >> 2);
  __ access_store_at(T_INT, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(2)), r0, noreg, noreg);
}

void TemplateTable::lastore() {
  transition(ltos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // r0: value
  // r1: index
  // r3: array
  index_check(r3, r1); // prefer index in r1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_LONG) >> 3);
  __ access_store_at(T_LONG, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(3)), r0, noreg, noreg);
}

void TemplateTable::fastore() {
  transition(ftos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // v0: value
  // r1:  index
  // r3:  array
  index_check(r3, r1); // prefer index in r1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_FLOAT) >> 2);
  __ access_store_at(T_FLOAT, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(2)), noreg /* ftos */, noreg, noreg);
}

void TemplateTable::dastore() {
  transition(dtos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // v0: value
  // r1:  index
  // r3:  array
  index_check(r3, r1); // prefer index in r1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_DOUBLE) >> 3);
  __ access_store_at(T_DOUBLE, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(3)), noreg /* dtos */, noreg, noreg);
}

void TemplateTable::aastore() {
  Label is_null, ok_is_subtype, done;
  transition(vtos, vtos);
  // stack: ..., array, index, value
  __ ldr(r0, at_tos());    // value
  __ ldr(r2, at_tos_p1()); // index
  __ ldr(r3, at_tos_p2()); // array

  Address element_address(r3, r4, Address::uxtw(LogBytesPerHeapOop));

  index_check(r3, r2);     // kills r1
  __ add(r4, r2, arrayOopDesc::base_offset_in_bytes(T_OBJECT) >> LogBytesPerHeapOop);

  // do array store check - check for NULL value first
  __ cbz(r0, is_null);

  // Move subklass into r1
  __ load_klass(r1, r0);
  // Move superklass into r0
  __ load_klass(r0, r3);
  __ ldr(r0, Address(r0,
                     ObjArrayKlass::element_klass_offset()));
  // Compress array + index*oopSize + 12 into a single register.  Frees r2.

  // Generate subtype check.  Blows r2, r5
  // Superklass in r0.  Subklass in r1.
  __ gen_subtype_check(r1, ok_is_subtype);

  // Come here on failure
  // object is at TOS
  __ b(Interpreter::_throw_ArrayStoreException_entry);

  // Come here on success
  __ bind(ok_is_subtype);

  // Get the value we will store
  __ ldr(r0, at_tos());
  // Now store using the appropriate barrier
  do_oop_store(_masm, element_address, r0, IS_ARRAY);
  __ b(done);

  // Have a NULL in r0, r3=array, r2=index.  Store NULL at ary[idx]
  __ bind(is_null);
  __ profile_null_seen(r2);

  // Store a NULL
  do_oop_store(_masm, element_address, noreg, IS_ARRAY);

  // Pop stack arguments
  __ bind(done);
  __ add(esp, esp, 3 * Interpreter::stackElementSize);
}

void TemplateTable::bastore()
{
  transition(itos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // r0: value
  // r1: index
  // r3: array
  index_check(r3, r1); // prefer index in r1

  // Need to check whether array is boolean or byte
  // since both types share the bastore bytecode.
  __ load_klass(r2, r3);
  __ ldrw(r2, Address(r2, Klass::layout_helper_offset()));
  int diffbit_index = exact_log2(Klass::layout_helper_boolean_diffbit());
  Label L_skip;
  __ tbz(r2, diffbit_index, L_skip);
  __ andw(r0, r0, 1);  // if it is a T_BOOLEAN array, mask the stored value to 0/1
  __ bind(L_skip);

  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_BYTE) >> 0);
  __ access_store_at(T_BYTE, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(0)), r0, noreg, noreg);
}

void TemplateTable::castore()
{
  transition(itos, vtos);
  __ pop_i(r1);
  __ pop_ptr(r3);
  // r0: value
  // r1: index
  // r3: array
  index_check(r3, r1); // prefer index in r1
  __ add(r1, r1, arrayOopDesc::base_offset_in_bytes(T_CHAR) >> 1);
  __ access_store_at(T_CHAR, IN_HEAP | IS_ARRAY, Address(r3, r1, Address::uxtw(1)), r0, noreg, noreg);
}

void TemplateTable::sastore()
{
  castore();
}

void TemplateTable::istore(int n)
{
  transition(itos, vtos);
  __ str(r0, iaddress(n));
}

void TemplateTable::lstore(int n)
{
  transition(ltos, vtos);
  __ str(r0, laddress(n));
}

void TemplateTable::fstore(int n)
{
  transition(ftos, vtos);
  __ strs(v0, faddress(n));
}

void TemplateTable::dstore(int n)
{
  transition(dtos, vtos);
  __ strd(v0, daddress(n));
}

void TemplateTable::astore(int n)
{
  transition(vtos, vtos);
  __ pop_ptr(r0);
  __ str(r0, iaddress(n));
}

void TemplateTable::pop()
{
  transition(vtos, vtos);
  __ add(esp, esp, Interpreter::stackElementSize);
}

void TemplateTable::pop2()
{
  transition(vtos, vtos);
  __ add(esp, esp, 2 * Interpreter::stackElementSize);
}

void TemplateTable::dup()
{
  transition(vtos, vtos);
  __ ldr(r0, Address(esp, 0));
  __ push(r0);
  // stack: ..., a, a
}

void TemplateTable::dup_x1()
{
  transition(vtos, vtos);
  // stack: ..., a, b
  __ ldr(r0, at_tos());  // load b
  __ ldr(r2, at_tos_p1());  // load a
  __ str(r0, at_tos_p1());  // store b
  __ str(r2, at_tos());  // store a
  __ push(r0);                  // push b
  // stack: ..., b, a, b
}

void TemplateTable::dup_x2()
{
  transition(vtos, vtos);
  // stack: ..., a, b, c
  __ ldr(r0, at_tos());  // load c
  __ ldr(r2, at_tos_p2());  // load a
  __ str(r0, at_tos_p2());  // store c in a
  __ push(r0);      // push c
  // stack: ..., c, b, c, c
  __ ldr(r0, at_tos_p2());  // load b
  __ str(r2, at_tos_p2());  // store a in b
  // stack: ..., c, a, c, c
  __ str(r0, at_tos_p1());  // store b in c
  // stack: ..., c, a, b, c
}

void TemplateTable::dup2()
{
  transition(vtos, vtos);
  // stack: ..., a, b
  __ ldr(r0, at_tos_p1());  // load a
  __ push(r0);                  // push a
  __ ldr(r0, at_tos_p1());  // load b
  __ push(r0);                  // push b
  // stack: ..., a, b, a, b
}

void TemplateTable::dup2_x1()
{
  transition(vtos, vtos);
  // stack: ..., a, b, c
  __ ldr(r2, at_tos());  // load c
  __ ldr(r0, at_tos_p1());  // load b
  __ push(r0);                  // push b
  __ push(r2);                  // push c
  // stack: ..., a, b, c, b, c
  __ str(r2, at_tos_p3());  // store c in b
  // stack: ..., a, c, c, b, c
  __ ldr(r2, at_tos_p4());  // load a
  __ str(r2, at_tos_p2());  // store a in 2nd c
  // stack: ..., a, c, a, b, c
  __ str(r0, at_tos_p4());  // store b in a
  // stack: ..., b, c, a, b, c
}

void TemplateTable::dup2_x2()
{
  transition(vtos, vtos);
  // stack: ..., a, b, c, d
  __ ldr(r2, at_tos());  // load d
  __ ldr(r0, at_tos_p1());  // load c
  __ push(r0)            ;      // push c
  __ push(r2);                  // push d
  // stack: ..., a, b, c, d, c, d
  __ ldr(r0, at_tos_p4());  // load b
  __ str(r0, at_tos_p2());  // store b in d
  __ str(r2, at_tos_p4());  // store d in b
  // stack: ..., a, d, c, b, c, d
  __ ldr(r2, at_tos_p5());  // load a
  __ ldr(r0, at_tos_p3());  // load c
  __ str(r2, at_tos_p3());  // store a in c
  __ str(r0, at_tos_p5());  // store c in a
  // stack: ..., c, d, a, b, c, d
}

void TemplateTable::swap()
{
  transition(vtos, vtos);
  // stack: ..., a, b
  __ ldr(r2, at_tos_p1());  // load a
  __ ldr(r0, at_tos());  // load b
  __ str(r2, at_tos());  // store a in b
  __ str(r0, at_tos_p1());  // store b in a
  // stack: ..., b, a
}

void TemplateTable::iop2(Operation op)
{
  transition(itos, itos);
  // r0 <== r1 op r0
  __ pop_i(r1);
  switch (op) {
  case add  : __ addw(r0, r1, r0); break;
  case sub  : __ subw(r0, r1, r0); break;
  case mul  : __ mulw(r0, r1, r0); break;
  case _and : __ andw(r0, r1, r0); break;
  case _or  : __ orrw(r0, r1, r0); break;
  case _xor : __ eorw(r0, r1, r0); break;
  case shl  : __ lslvw(r0, r1, r0); break;
  case shr  : __ asrvw(r0, r1, r0); break;
  case ushr : __ lsrvw(r0, r1, r0);break;
  default   : ShouldNotReachHere();
  }
}

void TemplateTable::lop2(Operation op)
{
  transition(ltos, ltos);
  // r0 <== r1 op r0
  __ pop_l(r1);
  switch (op) {
  case add  : __ add(r0, r1, r0); break;
  case sub  : __ sub(r0, r1, r0); break;
  case mul  : __ mul(r0, r1, r0); break;
  case _and : __ andr(r0, r1, r0); break;
  case _or  : __ orr(r0, r1, r0); break;
  case _xor : __ eor(r0, r1, r0); break;
  default   : ShouldNotReachHere();
  }
}

void TemplateTable::idiv()
{
  transition(itos, itos);
  // explicitly check for div0
  Label no_div0;
  __ cbnzw(r0, no_div0);
  __ mov(rscratch1, Interpreter::_throw_ArithmeticException_entry);
  __ br(rscratch1);
  __ bind(no_div0);
  __ pop_i(r1);
  // r0 <== r1 idiv r0
  __ corrected_idivl(r0, r1, r0, /* want_remainder */ false);
}

void TemplateTable::irem()
{
  transition(itos, itos);
  // explicitly check for div0
  Label no_div0;
  __ cbnzw(r0, no_div0);
  __ mov(rscratch1, Interpreter::_throw_ArithmeticException_entry);
  __ br(rscratch1);
  __ bind(no_div0);
  __ pop_i(r1);
  // r0 <== r1 irem r0
  __ corrected_idivl(r0, r1, r0, /* want_remainder */ true);
}

void TemplateTable::lmul()
{
  transition(ltos, ltos);
  __ pop_l(r1);
  __ mul(r0, r0, r1);
}

void TemplateTable::ldiv()
{
  transition(ltos, ltos);
  // explicitly check for div0
  Label no_div0;
  __ cbnz(r0, no_div0);
  __ mov(rscratch1, Interpreter::_throw_ArithmeticException_entry);
  __ br(rscratch1);
  __ bind(no_div0);
  __ pop_l(r1);
  // r0 <== r1 ldiv r0
  __ corrected_idivq(r0, r1, r0, /* want_remainder */ false);
}

void TemplateTable::lrem()
{
  transition(ltos, ltos);
  // explicitly check for div0
  Label no_div0;
  __ cbnz(r0, no_div0);
  __ mov(rscratch1, Interpreter::_throw_ArithmeticException_entry);
  __ br(rscratch1);
  __ bind(no_div0);
  __ pop_l(r1);
  // r0 <== r1 lrem r0
  __ corrected_idivq(r0, r1, r0, /* want_remainder */ true);
}

void TemplateTable::lshl()
{
  transition(itos, ltos);
  // shift count is in r0
  __ pop_l(r1);
  __ lslv(r0, r1, r0);
}

void TemplateTable::lshr()
{
  transition(itos, ltos);
  // shift count is in r0
  __ pop_l(r1);
  __ asrv(r0, r1, r0);
}

void TemplateTable::lushr()
{
  transition(itos, ltos);
  // shift count is in r0
  __ pop_l(r1);
  __ lsrv(r0, r1, r0);
}

void TemplateTable::fop2(Operation op)
{
  transition(ftos, ftos);
  switch (op) {
  case add:
    // n.b. use ldrd because this is a 64 bit slot
    __ pop_f(v1);
    __ fadds(v0, v1, v0);
    break;
  case sub:
    __ pop_f(v1);
    __ fsubs(v0, v1, v0);
    break;
  case mul:
    __ pop_f(v1);
    __ fmuls(v0, v1, v0);
    break;
  case div:
    __ pop_f(v1);
    __ fdivs(v0, v1, v0);
    break;
  case rem:
    __ fmovs(v1, v0);
    __ pop_f(v0);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::frem));
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void TemplateTable::dop2(Operation op)
{
  transition(dtos, dtos);
  switch (op) {
  case add:
    // n.b. use ldrd because this is a 64 bit slot
    __ pop_d(v1);
    __ faddd(v0, v1, v0);
    break;
  case sub:
    __ pop_d(v1);
    __ fsubd(v0, v1, v0);
    break;
  case mul:
    __ pop_d(v1);
    __ fmuld(v0, v1, v0);
    break;
  case div:
    __ pop_d(v1);
    __ fdivd(v0, v1, v0);
    break;
  case rem:
    __ fmovd(v1, v0);
    __ pop_d(v0);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::drem));
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void TemplateTable::ineg()
{
  transition(itos, itos);
  __ negw(r0, r0);

}

void TemplateTable::lneg()
{
  transition(ltos, ltos);
  __ neg(r0, r0);
}

void TemplateTable::fneg()
{
  transition(ftos, ftos);
  __ fnegs(v0, v0);
}

void TemplateTable::dneg()
{
  transition(dtos, dtos);
  __ fnegd(v0, v0);
}

void TemplateTable::iinc()
{
  transition(vtos, vtos);
  __ load_signed_byte(r1, at_bcp(2)); // get constant
  locals_index(r2);
  __ ldr(r0, iaddress(r2));
  __ addw(r0, r0, r1);
  __ str(r0, iaddress(r2));
}

void TemplateTable::wide_iinc()
{
  transition(vtos, vtos);
  // __ mov(r1, zr);
  __ ldrw(r1, at_bcp(2)); // get constant and index
  __ rev16(r1, r1);
  __ ubfx(r2, r1, 0, 16);
  __ neg(r2, r2);
  __ sbfx(r1, r1, 16, 16);
  __ ldr(r0, iaddress(r2));
  __ addw(r0, r0, r1);
  __ str(r0, iaddress(r2));
}

void TemplateTable::convert()
{
  // Checking
#ifdef ASSERT
  {
    TosState tos_in  = ilgl;
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
  // static const int64_t is_nan = 0x8000000000000000L;

  // Conversion
  switch (bytecode()) {
  case Bytecodes::_i2l:
    __ sxtw(r0, r0);
    break;
  case Bytecodes::_i2f:
    __ scvtfws(v0, r0);
    break;
  case Bytecodes::_i2d:
    __ scvtfwd(v0, r0);
    break;
  case Bytecodes::_i2b:
    __ sxtbw(r0, r0);
    break;
  case Bytecodes::_i2c:
    __ uxthw(r0, r0);
    break;
  case Bytecodes::_i2s:
    __ sxthw(r0, r0);
    break;
  case Bytecodes::_l2i:
    __ uxtw(r0, r0);
    break;
  case Bytecodes::_l2f:
    __ scvtfs(v0, r0);
    break;
  case Bytecodes::_l2d:
    __ scvtfd(v0, r0);
    break;
  case Bytecodes::_f2i:
  {
    Label L_Okay;
    __ clear_fpsr();
    __ fcvtzsw(r0, v0);
    __ get_fpsr(r1);
    __ cbzw(r1, L_Okay);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::f2i));
    __ bind(L_Okay);
  }
    break;
  case Bytecodes::_f2l:
  {
    Label L_Okay;
    __ clear_fpsr();
    __ fcvtzs(r0, v0);
    __ get_fpsr(r1);
    __ cbzw(r1, L_Okay);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::f2l));
    __ bind(L_Okay);
  }
    break;
  case Bytecodes::_f2d:
    __ fcvts(v0, v0);
    break;
  case Bytecodes::_d2i:
  {
    Label L_Okay;
    __ clear_fpsr();
    __ fcvtzdw(r0, v0);
    __ get_fpsr(r1);
    __ cbzw(r1, L_Okay);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::d2i));
    __ bind(L_Okay);
  }
    break;
  case Bytecodes::_d2l:
  {
    Label L_Okay;
    __ clear_fpsr();
    __ fcvtzd(r0, v0);
    __ get_fpsr(r1);
    __ cbzw(r1, L_Okay);
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::d2l));
    __ bind(L_Okay);
  }
    break;
  case Bytecodes::_d2f:
    __ fcvtd(v0, v0);
    break;
  default:
    ShouldNotReachHere();
  }
}

void TemplateTable::lcmp()
{
  transition(ltos, itos);
  Label done;
  __ pop_l(r1);
  __ cmp(r1, r0);
  __ mov(r0, (uint64_t)-1L);
  __ br(Assembler::LT, done);
  // __ mov(r0, 1UL);
  // __ csel(r0, r0, zr, Assembler::NE);
  // and here is a faster way
  __ csinc(r0, zr, zr, Assembler::EQ);
  __ bind(done);
}

void TemplateTable::float_cmp(bool is_float, int unordered_result)
{
  Label done;
  if (is_float) {
    // XXX get rid of pop here, use ... reg, mem32
    __ pop_f(v1);
    __ fcmps(v1, v0);
  } else {
    // XXX get rid of pop here, use ... reg, mem64
    __ pop_d(v1);
    __ fcmpd(v1, v0);
  }
  if (unordered_result < 0) {
    // we want -1 for unordered or less than, 0 for equal and 1 for
    // greater than.
    __ mov(r0, (uint64_t)-1L);
    // for FP LT tests less than or unordered
    __ br(Assembler::LT, done);
    // install 0 for EQ otherwise 1
    __ csinc(r0, zr, zr, Assembler::EQ);
  } else {
    // we want -1 for less than, 0 for equal and 1 for unordered or
    // greater than.
    __ mov(r0, 1L);
    // for FP HI tests greater than or unordered
    __ br(Assembler::HI, done);
    // install 0 for EQ otherwise ~0
    __ csinv(r0, zr, zr, Assembler::EQ);

  }
  __ bind(done);
}

void TemplateTable::branch(bool is_jsr, bool is_wide)
{
  // We might be moving to a safepoint.  The thread which calls
  // Interpreter::notice_safepoints() will effectively flush its cache
  // when it makes a system call, but we need to do something to
  // ensure that we see the changed dispatch table.
  __ membar(MacroAssembler::LoadLoad);

  __ profile_taken_branch(r0, r1);
  const ByteSize be_offset = MethodCounters::backedge_counter_offset() +
                             InvocationCounter::counter_offset();
  const ByteSize inv_offset = MethodCounters::invocation_counter_offset() +
                              InvocationCounter::counter_offset();

  // load branch displacement
  if (!is_wide) {
    __ ldrh(r2, at_bcp(1));
    __ rev16(r2, r2);
    // sign extend the 16 bit value in r2
    __ sbfm(r2, r2, 0, 15);
  } else {
    __ ldrw(r2, at_bcp(1));
    __ revw(r2, r2);
    // sign extend the 32 bit value in r2
    __ sbfm(r2, r2, 0, 31);
  }

  // Handle all the JSR stuff here, then exit.
  // It's much shorter and cleaner than intermingling with the non-JSR
  // normal-branch stuff occurring below.

  if (is_jsr) {
    // Pre-load the next target bytecode into rscratch1
    __ load_unsigned_byte(rscratch1, Address(rbcp, r2));
    // compute return address as bci
    __ ldr(rscratch2, Address(rmethod, Method::const_offset()));
    __ add(rscratch2, rscratch2,
           in_bytes(ConstMethod::codes_offset()) - (is_wide ? 5 : 3));
    __ sub(r1, rbcp, rscratch2);
    __ push_i(r1);
    // Adjust the bcp by the 16-bit displacement in r2
    __ add(rbcp, rbcp, r2);
    __ dispatch_only(vtos, /*generate_poll*/true);
    return;
  }

  // Normal (non-jsr) branch handling

  // Adjust the bcp by the displacement in r2
  __ add(rbcp, rbcp, r2);

  assert(UseLoopCounter || !UseOnStackReplacement,
         "on-stack-replacement requires loop counters");
  Label backedge_counter_overflow;
  Label dispatch;
  if (UseLoopCounter) {
    // increment backedge counter for backward branches
    // r0: MDO
    // w1: MDO bumped taken-count
    // r2: target offset
    __ cmp(r2, zr);
    __ br(Assembler::GT, dispatch); // count only if backward branch

    // ECN: FIXME: This code smells
    // check if MethodCounters exists
    Label has_counters;
    __ ldr(rscratch1, Address(rmethod, Method::method_counters_offset()));
    __ cbnz(rscratch1, has_counters);
    __ push(r0);
    __ push(r1);
    __ push(r2);
    __ call_VM(noreg, CAST_FROM_FN_PTR(address,
            InterpreterRuntime::build_method_counters), rmethod);
    __ pop(r2);
    __ pop(r1);
    __ pop(r0);
    __ ldr(rscratch1, Address(rmethod, Method::method_counters_offset()));
    __ cbz(rscratch1, dispatch); // No MethodCounters allocated, OutOfMemory
    __ bind(has_counters);

    Label no_mdo;
    int increment = InvocationCounter::count_increment;
    if (ProfileInterpreter) {
      // Are we profiling?
      __ ldr(r1, Address(rmethod, in_bytes(Method::method_data_offset())));
      __ cbz(r1, no_mdo);
      // Increment the MDO backedge counter
      const Address mdo_backedge_counter(r1, in_bytes(MethodData::backedge_counter_offset()) +
                                         in_bytes(InvocationCounter::counter_offset()));
      const Address mask(r1, in_bytes(MethodData::backedge_mask_offset()));
      __ increment_mask_and_jump(mdo_backedge_counter, increment, mask,
                                 r0, rscratch1, false, Assembler::EQ,
                                 UseOnStackReplacement ? &backedge_counter_overflow : &dispatch);
      __ b(dispatch);
    }
    __ bind(no_mdo);
    // Increment backedge counter in MethodCounters*
    __ ldr(rscratch1, Address(rmethod, Method::method_counters_offset()));
    const Address mask(rscratch1, in_bytes(MethodCounters::backedge_mask_offset()));
    __ increment_mask_and_jump(Address(rscratch1, be_offset), increment, mask,
                               r0, rscratch2, false, Assembler::EQ,
                               UseOnStackReplacement ? &backedge_counter_overflow : &dispatch);
    __ bind(dispatch);
  }

  // Pre-load the next target bytecode into rscratch1
  __ load_unsigned_byte(rscratch1, Address(rbcp, 0));

  // continue with the bytecode @ target
  // rscratch1: target bytecode
  // rbcp: target bcp
  __ dispatch_only(vtos, /*generate_poll*/true);

  if (UseLoopCounter && UseOnStackReplacement) {
    // invocation counter overflow
    __ bind(backedge_counter_overflow);
    __ neg(r2, r2);
    __ add(r2, r2, rbcp);     // branch bcp
    // IcoResult frequency_counter_overflow([JavaThread*], address branch_bcp)
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::frequency_counter_overflow),
               r2);
    __ load_unsigned_byte(r1, Address(rbcp, 0));  // restore target bytecode

    // r0: osr nmethod (osr ok) or NULL (osr not possible)
    // w1: target bytecode
    // r2: scratch
    __ cbz(r0, dispatch);     // test result -- no osr if null
    // nmethod may have been invalidated (VM may block upon call_VM return)
    __ ldrb(r2, Address(r0, nmethod::state_offset()));
    if (nmethod::in_use != 0)
      __ sub(r2, r2, nmethod::in_use);
    __ cbnz(r2, dispatch);

    // We have the address of an on stack replacement routine in r0
    // We need to prepare to execute the OSR method. First we must
    // migrate the locals and monitors off of the stack.

    __ mov(r19, r0);                             // save the nmethod

    call_VM(noreg, CAST_FROM_FN_PTR(address, SharedRuntime::OSR_migration_begin));

    // r0 is OSR buffer, move it to expected parameter location
    __ mov(j_rarg0, r0);

    // remove activation
    // get sender esp
    __ ldr(esp,
        Address(rfp, frame::interpreter_frame_sender_sp_offset * wordSize));
    // remove frame anchor
    __ leave();
    // Ensure compiled code always sees stack at proper alignment
    __ andr(sp, esp, -16);

    // and begin the OSR nmethod
    __ ldr(rscratch1, Address(r19, nmethod::osr_entry_point_offset()));
    __ br(rscratch1);
  }
}


void TemplateTable::if_0cmp(Condition cc)
{
  transition(itos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  if (cc == equal)
    __ cbnzw(r0, not_taken);
  else if (cc == not_equal)
    __ cbzw(r0, not_taken);
  else {
    __ andsw(zr, r0, r0);
    __ br(j_not(cc), not_taken);
  }

  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(r0);
}

void TemplateTable::if_icmp(Condition cc)
{
  transition(itos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  __ pop_i(r1);
  __ cmpw(r1, r0, Assembler::LSL);
  __ br(j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(r0);
}

void TemplateTable::if_nullcmp(Condition cc)
{
  transition(atos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  if (cc == equal)
    __ cbnz(r0, not_taken);
  else
    __ cbz(r0, not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(r0);
}

void TemplateTable::if_acmp(Condition cc)
{
  transition(atos, vtos);
  // assume branch is more often taken than not (loops use backward branches)
  Label not_taken;
  __ pop_ptr(r1);
  __ cmpoop(r1, r0);
  __ br(j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(r0);
}

void TemplateTable::ret() {
  transition(vtos, vtos);
  // We might be moving to a safepoint.  The thread which calls
  // Interpreter::notice_safepoints() will effectively flush its cache
  // when it makes a system call, but we need to do something to
  // ensure that we see the changed dispatch table.
  __ membar(MacroAssembler::LoadLoad);

  locals_index(r1);
  __ ldr(r1, aaddress(r1)); // get return bci, compute return bcp
  __ profile_ret(r1, r2);
  __ ldr(rbcp, Address(rmethod, Method::const_offset()));
  __ lea(rbcp, Address(rbcp, r1));
  __ add(rbcp, rbcp, in_bytes(ConstMethod::codes_offset()));
  __ dispatch_next(vtos, 0, /*generate_poll*/true);
}

void TemplateTable::wide_ret() {
  transition(vtos, vtos);
  locals_index_wide(r1);
  __ ldr(r1, aaddress(r1)); // get return bci, compute return bcp
  __ profile_ret(r1, r2);
  __ ldr(rbcp, Address(rmethod, Method::const_offset()));
  __ lea(rbcp, Address(rbcp, r1));
  __ add(rbcp, rbcp, in_bytes(ConstMethod::codes_offset()));
  __ dispatch_next(vtos, 0, /*generate_poll*/true);
}


void TemplateTable::tableswitch() {
  Label default_case, continue_execution;
  transition(itos, vtos);
  // align rbcp
  __ lea(r1, at_bcp(BytesPerInt));
  __ andr(r1, r1, -BytesPerInt);
  // load lo & hi
  __ ldrw(r2, Address(r1, BytesPerInt));
  __ ldrw(r3, Address(r1, 2 * BytesPerInt));
  __ rev32(r2, r2);
  __ rev32(r3, r3);
  // check against lo & hi
  __ cmpw(r0, r2);
  __ br(Assembler::LT, default_case);
  __ cmpw(r0, r3);
  __ br(Assembler::GT, default_case);
  // lookup dispatch offset
  __ subw(r0, r0, r2);
  __ lea(r3, Address(r1, r0, Address::uxtw(2)));
  __ ldrw(r3, Address(r3, 3 * BytesPerInt));
  __ profile_switch_case(r0, r1, r2);
  // continue execution
  __ bind(continue_execution);
  __ rev32(r3, r3);
  __ load_unsigned_byte(rscratch1, Address(rbcp, r3, Address::sxtw(0)));
  __ add(rbcp, rbcp, r3, ext::sxtw);
  __ dispatch_only(vtos, /*generate_poll*/true);
  // handle default
  __ bind(default_case);
  __ profile_switch_default(r0);
  __ ldrw(r3, Address(r1, 0));
  __ b(continue_execution);
}

void TemplateTable::lookupswitch() {
  transition(itos, itos);
  __ stop("lookupswitch bytecode should have been rewritten");
}

void TemplateTable::fast_linearswitch() {
  transition(itos, vtos);
  Label loop_entry, loop, found, continue_execution;
  // bswap r0 so we can avoid bswapping the table entries
  __ rev32(r0, r0);
  // align rbcp
  __ lea(r19, at_bcp(BytesPerInt)); // btw: should be able to get rid of
                                    // this instruction (change offsets
                                    // below)
  __ andr(r19, r19, -BytesPerInt);
  // set counter
  __ ldrw(r1, Address(r19, BytesPerInt));
  __ rev32(r1, r1);
  __ b(loop_entry);
  // table search
  __ bind(loop);
  __ lea(rscratch1, Address(r19, r1, Address::lsl(3)));
  __ ldrw(rscratch1, Address(rscratch1, 2 * BytesPerInt));
  __ cmpw(r0, rscratch1);
  __ br(Assembler::EQ, found);
  __ bind(loop_entry);
  __ subs(r1, r1, 1);
  __ br(Assembler::PL, loop);
  // default case
  __ profile_switch_default(r0);
  __ ldrw(r3, Address(r19, 0));
  __ b(continue_execution);
  // entry found -> get offset
  __ bind(found);
  __ lea(rscratch1, Address(r19, r1, Address::lsl(3)));
  __ ldrw(r3, Address(rscratch1, 3 * BytesPerInt));
  __ profile_switch_case(r1, r0, r19);
  // continue execution
  __ bind(continue_execution);
  __ rev32(r3, r3);
  __ add(rbcp, rbcp, r3, ext::sxtw);
  __ ldrb(rscratch1, Address(rbcp, 0));
  __ dispatch_only(vtos, /*generate_poll*/true);
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

  // Register allocation
  const Register key   = r0; // already set (tosca)
  const Register array = r1;
  const Register i     = r2;
  const Register j     = r3;
  const Register h     = rscratch1;
  const Register temp  = rscratch2;

  // Find array start
  __ lea(array, at_bcp(3 * BytesPerInt)); // btw: should be able to
                                          // get rid of this
                                          // instruction (change
                                          // offsets below)
  __ andr(array, array, -BytesPerInt);

  // Initialize i & j
  __ mov(i, 0);                            // i = 0;
  __ ldrw(j, Address(array, -BytesPerInt)); // j = length(array);

  // Convert j into native byteordering
  __ rev32(j, j);

  // And start
  Label entry;
  __ b(entry);

  // binary search loop
  {
    Label loop;
    __ bind(loop);
    // int h = (i + j) >> 1;
    __ addw(h, i, j);                           // h = i + j;
    __ lsrw(h, h, 1);                                   // h = (i + j) >> 1;
    // if (key < array[h].fast_match()) {
    //   j = h;
    // } else {
    //   i = h;
    // }
    // Convert array[h].match to native byte-ordering before compare
    __ ldr(temp, Address(array, h, Address::lsl(3)));
    __ rev32(temp, temp);
    __ cmpw(key, temp);
    // j = h if (key <  array[h].fast_match())
    __ csel(j, h, j, Assembler::LT);
    // i = h if (key >= array[h].fast_match())
    __ csel(i, h, i, Assembler::GE);
    // while (i+1 < j)
    __ bind(entry);
    __ addw(h, i, 1);          // i+1
    __ cmpw(h, j);             // i+1 < j
    __ br(Assembler::LT, loop);
  }

  // end of binary search, result index is i (must check again!)
  Label default_case;
  // Convert array[i].match to native byte-ordering before compare
  __ ldr(temp, Address(array, i, Address::lsl(3)));
  __ rev32(temp, temp);
  __ cmpw(key, temp);
  __ br(Assembler::NE, default_case);

  // entry found -> j = offset
  __ add(j, array, i, ext::uxtx, 3);
  __ ldrw(j, Address(j, BytesPerInt));
  __ profile_switch_case(i, key, array);
  __ rev32(j, j);
  __ load_unsigned_byte(rscratch1, Address(rbcp, j, Address::sxtw(0)));
  __ lea(rbcp, Address(rbcp, j, Address::sxtw(0)));
  __ dispatch_only(vtos, /*generate_poll*/true);

  // default case -> j = default offset
  __ bind(default_case);
  __ profile_switch_default(i);
  __ ldrw(j, Address(array, -2 * BytesPerInt));
  __ rev32(j, j);
  __ load_unsigned_byte(rscratch1, Address(rbcp, j, Address::sxtw(0)));
  __ lea(rbcp, Address(rbcp, j, Address::sxtw(0)));
  __ dispatch_only(vtos, /*generate_poll*/true);
}


void TemplateTable::_return(TosState state)
{
  transition(state, state);
  assert(_desc->calls_vm(),
         "inconsistent calls_vm information"); // call in remove_activation

  if (_desc->bytecode() == Bytecodes::_return_register_finalizer) {
    assert(state == vtos, "only valid state");

    __ ldr(c_rarg1, aaddress(0));
    __ load_klass(r3, c_rarg1);
    __ ldrw(r3, Address(r3, Klass::access_flags_offset()));
    Label skip_register_finalizer;
    __ tbz(r3, exact_log2(JVM_ACC_HAS_FINALIZER), skip_register_finalizer);

    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::register_finalizer), c_rarg1);

    __ bind(skip_register_finalizer);
  }

  // Issue a StoreStore barrier after all stores but before return
  // from any constructor for any class with a final field.  We don't
  // know if this is a finalizer, so we always do so.
  if (_desc->bytecode() == Bytecodes::_return)
    __ membar(MacroAssembler::StoreStore);

  // Narrow result if state is itos but result type is smaller.
  // Need to narrow in the return bytecode rather than in generate_return_entry
  // since compiled code callers expect the result to already be narrowed.
  if (state == itos) {
    __ narrow(r0);
  }

  __ remove_activation(state);
  __ ret(lr);
}

// ----------------------------------------------------------------------------
// Volatile variables demand their effects be made known to all CPU's
// in order.  Store buffers on most chips allow reads & writes to
// reorder; the JMM's ReadAfterWrite.java test fails in -Xint mode
// without some kind of memory barrier (i.e., it's not sufficient that
// the interpreter does not reorder volatile references, the hardware
// also must not reorder them).
//
// According to the new Java Memory Model (JMM):
// (1) All volatiles are serialized wrt to each other.  ALSO reads &
//     writes act as aquire & release, so:
// (2) A read cannot let unrelated NON-volatile memory refs that
//     happen after the read float up to before the read.  It's OK for
//     non-volatile memory refs that happen before the volatile read to
//     float down below it.
// (3) Similar a volatile write cannot let unrelated NON-volatile
//     memory refs that happen BEFORE the write float down to after the
//     write.  It's OK for non-volatile memory refs that happen after the
//     volatile write to float up before it.
//
// We only put in barriers around volatile refs (they are expensive),
// not _between_ memory refs (that would require us to track the
// flavor of the previous memory refs).  Requirements (2) and (3)
// require some barriers before volatile stores and after volatile
// loads.  These nearly cover requirement (1) but miss the
// volatile-store-volatile-load case.  This final case is placed after
// volatile-stores although it could just as well go before
// volatile-loads.

void TemplateTable::resolve_cache_and_index(int byte_no,
                                            Register Rcache,
                                            Register index,
                                            size_t index_size) {
  const Register temp = r19;
  assert_different_registers(Rcache, index, temp);

  Label resolved, clinit_barrier_slow;

  Bytecodes::Code code = bytecode();
  switch (code) {
  case Bytecodes::_nofast_getfield: code = Bytecodes::_getfield; break;
  case Bytecodes::_nofast_putfield: code = Bytecodes::_putfield; break;
  default: break;
  }

  assert(byte_no == f1_byte || byte_no == f2_byte, "byte_no out of range");
  __ get_cache_and_index_and_bytecode_at_bcp(Rcache, index, temp, byte_no, 1, index_size);
  __ subs(zr, temp, (int) code);  // have we resolved this bytecode?
  __ br(Assembler::EQ, resolved);

  // resolve first time through
  // Class initialization barrier slow path lands here as well.
  __ bind(clinit_barrier_slow);
  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_from_cache);
  __ mov(temp, (int) code);
  __ call_VM(noreg, entry, temp);

  // Update registers with resolved info
  __ get_cache_and_index_at_bcp(Rcache, index, 1, index_size);
  // n.b. unlike x86 Rcache is now rcpool plus the indexed offset
  // so all clients ofthis method must be modified accordingly
  __ bind(resolved);

  // Class initialization barrier for static methods
  if (VM_Version::supports_fast_class_init_checks() && bytecode() == Bytecodes::_invokestatic) {
    __ load_resolved_method_at_index(byte_no, temp, Rcache);
    __ load_method_holder(temp, temp);
    __ clinit_barrier(temp, rscratch1, NULL, &clinit_barrier_slow);
  }
}

// The Rcache and index registers must be set before call
// n.b unlike x86 cache already includes the index offset
void TemplateTable::load_field_cp_cache_entry(Register obj,
                                              Register cache,
                                              Register index,
                                              Register off,
                                              Register flags,
                                              bool is_static = false) {
  assert_different_registers(cache, index, flags, off);

  ByteSize cp_base_offset = ConstantPoolCache::base_offset();
  // Field offset
  __ ldr(off, Address(cache, in_bytes(cp_base_offset +
                                          ConstantPoolCacheEntry::f2_offset())));
  // Flags
  __ ldrw(flags, Address(cache, in_bytes(cp_base_offset +
                                           ConstantPoolCacheEntry::flags_offset())));

  // klass overwrite register
  if (is_static) {
    __ ldr(obj, Address(cache, in_bytes(cp_base_offset +
                                        ConstantPoolCacheEntry::f1_offset())));
    const int mirror_offset = in_bytes(Klass::java_mirror_offset());
    __ ldr(obj, Address(obj, mirror_offset));
    __ resolve_oop_handle(obj);
  }
}

void TemplateTable::load_invoke_cp_cache_entry(int byte_no,
                                               Register method,
                                               Register itable_index,
                                               Register flags,
                                               bool is_invokevirtual,
                                               bool is_invokevfinal, /*unused*/
                                               bool is_invokedynamic) {
  // setup registers
  const Register cache = rscratch2;
  const Register index = r4;
  assert_different_registers(method, flags);
  assert_different_registers(method, cache, index);
  assert_different_registers(itable_index, flags);
  assert_different_registers(itable_index, cache, index);
  // determine constant pool cache field offsets
  assert(is_invokevirtual == (byte_no == f2_byte), "is_invokevirtual flag redundant");
  const int method_offset = in_bytes(
    ConstantPoolCache::base_offset() +
      (is_invokevirtual
       ? ConstantPoolCacheEntry::f2_offset()
       : ConstantPoolCacheEntry::f1_offset()));
  const int flags_offset = in_bytes(ConstantPoolCache::base_offset() +
                                    ConstantPoolCacheEntry::flags_offset());
  // access constant pool cache fields
  const int index_offset = in_bytes(ConstantPoolCache::base_offset() +
                                    ConstantPoolCacheEntry::f2_offset());

  size_t index_size = (is_invokedynamic ? sizeof(u4) : sizeof(u2));
  resolve_cache_and_index(byte_no, cache, index, index_size);
  __ ldr(method, Address(cache, method_offset));

  if (itable_index != noreg) {
    __ ldr(itable_index, Address(cache, index_offset));
  }
  __ ldrw(flags, Address(cache, flags_offset));
}


// The registers cache and index expected to be set before call.
// Correct values of the cache and index registers are preserved.
void TemplateTable::jvmti_post_field_access(Register cache, Register index,
                                            bool is_static, bool has_tos) {
  // do the JVMTI work here to avoid disturbing the register state below
  // We use c_rarg registers here because we want to use the register used in
  // the call to the VM
  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the time to call into the VM.
    Label L1;
    assert_different_registers(cache, index, r0);
    __ lea(rscratch1, ExternalAddress((address) JvmtiExport::get_field_access_count_addr()));
    __ ldrw(r0, Address(rscratch1));
    __ cbzw(r0, L1);

    __ get_cache_and_index_at_bcp(c_rarg2, c_rarg3, 1);
    __ lea(c_rarg2, Address(c_rarg2, in_bytes(ConstantPoolCache::base_offset())));

    if (is_static) {
      __ mov(c_rarg1, zr); // NULL object reference
    } else {
      __ ldr(c_rarg1, at_tos()); // get object pointer without popping it
      __ verify_oop(c_rarg1);
    }
    // c_rarg1: object pointer or NULL
    // c_rarg2: cache entry pointer
    // c_rarg3: jvalue object on the stack
    __ call_VM(noreg, CAST_FROM_FN_PTR(address,
                                       InterpreterRuntime::post_field_access),
               c_rarg1, c_rarg2, c_rarg3);
    __ get_cache_and_index_at_bcp(cache, index, 1);
    __ bind(L1);
  }
}

void TemplateTable::pop_and_check_object(Register r)
{
  __ pop_ptr(r);
  __ null_check(r);  // for field access must check obj.
  __ verify_oop(r);
}

void TemplateTable::getfield_or_static(int byte_no, bool is_static, RewriteControl rc)
{
  const Register cache = r2;
  const Register index = r3;
  const Register obj   = r4;
  const Register off   = r19;
  const Register flags = r0;
  const Register raw_flags = r6;
  const Register bc    = r4; // uses same reg as obj, so don't mix them

  resolve_cache_and_index(byte_no, cache, index, sizeof(u2));
  jvmti_post_field_access(cache, index, is_static, false);
  load_field_cp_cache_entry(obj, cache, index, off, raw_flags, is_static);

  if (!is_static) {
    // obj is on the stack
    pop_and_check_object(obj);
  }

  // 8179954: We need to make sure that the code generated for
  // volatile accesses forms a sequentially-consistent set of
  // operations when combined with STLR and LDAR.  Without a leading
  // membar it's possible for a simple Dekker test to fail if loads
  // use LDR;DMB but stores use STLR.  This can happen if C2 compiles
  // the stores in one method and we interpret the loads in another.
  if (!CompilerConfig::is_c1_or_interpreter_only_no_jvmci()){
    Label notVolatile;
    __ tbz(raw_flags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::AnyAny);
    __ bind(notVolatile);
  }

  const Address field(obj, off);

  Label Done, notByte, notBool, notInt, notShort, notChar,
              notLong, notFloat, notObj, notDouble;

  // x86 uses a shift and mask or wings it with a shift plus assert
  // the mask is not needed. aarch64 just uses bitfield extract
  __ ubfxw(flags, raw_flags, ConstantPoolCacheEntry::tos_state_shift,
           ConstantPoolCacheEntry::tos_state_bits);

  assert(btos == 0, "change code, btos != 0");
  __ cbnz(flags, notByte);

  // Don't rewrite getstatic, only getfield
  if (is_static) rc = may_not_rewrite;

  // btos
  __ access_load_at(T_BYTE, IN_HEAP, r0, field, noreg, noreg);
  __ push(btos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_bgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notByte);
  __ cmp(flags, (u1)ztos);
  __ br(Assembler::NE, notBool);

  // ztos (same code as btos)
  __ access_load_at(T_BOOLEAN, IN_HEAP, r0, field, noreg, noreg);
  __ push(ztos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    // use btos rewriting, no truncating to t/f bit is needed for getfield.
    patch_bytecode(Bytecodes::_fast_bgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notBool);
  __ cmp(flags, (u1)atos);
  __ br(Assembler::NE, notObj);
  // atos
  do_oop_load(_masm, field, r0, IN_HEAP);
  __ push(atos);
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_agetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notObj);
  __ cmp(flags, (u1)itos);
  __ br(Assembler::NE, notInt);
  // itos
  __ access_load_at(T_INT, IN_HEAP, r0, field, noreg, noreg);
  __ push(itos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_igetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notInt);
  __ cmp(flags, (u1)ctos);
  __ br(Assembler::NE, notChar);
  // ctos
  __ access_load_at(T_CHAR, IN_HEAP, r0, field, noreg, noreg);
  __ push(ctos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_cgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notChar);
  __ cmp(flags, (u1)stos);
  __ br(Assembler::NE, notShort);
  // stos
  __ access_load_at(T_SHORT, IN_HEAP, r0, field, noreg, noreg);
  __ push(stos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_sgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notShort);
  __ cmp(flags, (u1)ltos);
  __ br(Assembler::NE, notLong);
  // ltos
  __ access_load_at(T_LONG, IN_HEAP, r0, field, noreg, noreg);
  __ push(ltos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_lgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notLong);
  __ cmp(flags, (u1)ftos);
  __ br(Assembler::NE, notFloat);
  // ftos
  __ access_load_at(T_FLOAT, IN_HEAP, noreg /* ftos */, field, noreg, noreg);
  __ push(ftos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_fgetfield, bc, r1);
  }
  __ b(Done);

  __ bind(notFloat);
#ifdef ASSERT
  __ cmp(flags, (u1)dtos);
  __ br(Assembler::NE, notDouble);
#endif
  // dtos
  __ access_load_at(T_DOUBLE, IN_HEAP, noreg /* ftos */, field, noreg, noreg);
  __ push(dtos);
  // Rewrite bytecode to be faster
  if (rc == may_rewrite) {
    patch_bytecode(Bytecodes::_fast_dgetfield, bc, r1);
  }
#ifdef ASSERT
  __ b(Done);

  __ bind(notDouble);
  __ stop("Bad state");
#endif

  __ bind(Done);

  Label notVolatile;
  __ tbz(raw_flags, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
  __ membar(MacroAssembler::LoadLoad | MacroAssembler::LoadStore);
  __ bind(notVolatile);
}


void TemplateTable::getfield(int byte_no)
{
  getfield_or_static(byte_no, false);
}

void TemplateTable::nofast_getfield(int byte_no) {
  getfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::getstatic(int byte_no)
{
  getfield_or_static(byte_no, true);
}

// The registers cache and index expected to be set before call.
// The function may destroy various registers, just not the cache and index registers.
void TemplateTable::jvmti_post_field_mod(Register cache, Register index, bool is_static) {
  transition(vtos, vtos);

  ByteSize cp_base_offset = ConstantPoolCache::base_offset();

  if (JvmtiExport::can_post_field_modification()) {
    // Check to see if a field modification watch has been set before
    // we take the time to call into the VM.
    Label L1;
    assert_different_registers(cache, index, r0);
    __ lea(rscratch1, ExternalAddress((address)JvmtiExport::get_field_modification_count_addr()));
    __ ldrw(r0, Address(rscratch1));
    __ cbz(r0, L1);

    __ get_cache_and_index_at_bcp(c_rarg2, rscratch1, 1);

    if (is_static) {
      // Life is simple.  Null out the object pointer.
      __ mov(c_rarg1, zr);
    } else {
      // Life is harder. The stack holds the value on top, followed by
      // the object.  We don't know the size of the value, though; it
      // could be one or two words depending on its type. As a result,
      // we must find the type to determine where the object is.
      __ ldrw(c_rarg3, Address(c_rarg2,
                               in_bytes(cp_base_offset +
                                        ConstantPoolCacheEntry::flags_offset())));
      __ lsr(c_rarg3, c_rarg3,
             ConstantPoolCacheEntry::tos_state_shift);
      ConstantPoolCacheEntry::verify_tos_state_shift();
      Label nope2, done, ok;
      __ ldr(c_rarg1, at_tos_p1());  // initially assume a one word jvalue
      __ cmpw(c_rarg3, ltos);
      __ br(Assembler::EQ, ok);
      __ cmpw(c_rarg3, dtos);
      __ br(Assembler::NE, nope2);
      __ bind(ok);
      __ ldr(c_rarg1, at_tos_p2()); // ltos (two word jvalue)
      __ bind(nope2);
    }
    // cache entry pointer
    __ add(c_rarg2, c_rarg2, in_bytes(cp_base_offset));
    // object (tos)
    __ mov(c_rarg3, esp);
    // c_rarg1: object pointer set up above (NULL if static)
    // c_rarg2: cache entry pointer
    // c_rarg3: jvalue object on the stack
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::post_field_modification),
               c_rarg1, c_rarg2, c_rarg3);
    __ get_cache_and_index_at_bcp(cache, index, 1);
    __ bind(L1);
  }
}

void TemplateTable::putfield_or_static(int byte_no, bool is_static, RewriteControl rc) {
  transition(vtos, vtos);

  const Register cache = r2;
  const Register index = r3;
  const Register obj   = r2;
  const Register off   = r19;
  const Register flags = r0;
  const Register bc    = r4;

  resolve_cache_and_index(byte_no, cache, index, sizeof(u2));
  jvmti_post_field_mod(cache, index, is_static);
  load_field_cp_cache_entry(obj, cache, index, off, flags, is_static);

  Label Done;
  __ mov(r5, flags);

  {
    Label notVolatile;
    __ tbz(r5, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::StoreStore | MacroAssembler::LoadStore);
    __ bind(notVolatile);
  }

  // field address
  const Address field(obj, off);

  Label notByte, notBool, notInt, notShort, notChar,
        notLong, notFloat, notObj, notDouble;

  // x86 uses a shift and mask or wings it with a shift plus assert
  // the mask is not needed. aarch64 just uses bitfield extract
  __ ubfxw(flags, flags, ConstantPoolCacheEntry::tos_state_shift,  ConstantPoolCacheEntry::tos_state_bits);

  assert(btos == 0, "change code, btos != 0");
  __ cbnz(flags, notByte);

  // Don't rewrite putstatic, only putfield
  if (is_static) rc = may_not_rewrite;

  // btos
  {
    __ pop(btos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_BYTE, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_bputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notByte);
  __ cmp(flags, (u1)ztos);
  __ br(Assembler::NE, notBool);

  // ztos
  {
    __ pop(ztos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_BOOLEAN, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_zputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notBool);
  __ cmp(flags, (u1)atos);
  __ br(Assembler::NE, notObj);

  // atos
  {
    __ pop(atos);
    if (!is_static) pop_and_check_object(obj);
    // Store into the field
    do_oop_store(_masm, field, r0, IN_HEAP);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_aputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notObj);
  __ cmp(flags, (u1)itos);
  __ br(Assembler::NE, notInt);

  // itos
  {
    __ pop(itos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_INT, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_iputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notInt);
  __ cmp(flags, (u1)ctos);
  __ br(Assembler::NE, notChar);

  // ctos
  {
    __ pop(ctos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_CHAR, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_cputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notChar);
  __ cmp(flags, (u1)stos);
  __ br(Assembler::NE, notShort);

  // stos
  {
    __ pop(stos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_SHORT, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_sputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notShort);
  __ cmp(flags, (u1)ltos);
  __ br(Assembler::NE, notLong);

  // ltos
  {
    __ pop(ltos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_LONG, IN_HEAP, field, r0, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_lputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notLong);
  __ cmp(flags, (u1)ftos);
  __ br(Assembler::NE, notFloat);

  // ftos
  {
    __ pop(ftos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_FLOAT, IN_HEAP, field, noreg /* ftos */, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_fputfield, bc, r1, true, byte_no);
    }
    __ b(Done);
  }

  __ bind(notFloat);
#ifdef ASSERT
  __ cmp(flags, (u1)dtos);
  __ br(Assembler::NE, notDouble);
#endif

  // dtos
  {
    __ pop(dtos);
    if (!is_static) pop_and_check_object(obj);
    __ access_store_at(T_DOUBLE, IN_HEAP, field, noreg /* dtos */, noreg, noreg);
    if (rc == may_rewrite) {
      patch_bytecode(Bytecodes::_fast_dputfield, bc, r1, true, byte_no);
    }
  }

#ifdef ASSERT
  __ b(Done);

  __ bind(notDouble);
  __ stop("Bad state");
#endif

  __ bind(Done);

  {
    Label notVolatile;
    __ tbz(r5, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::StoreLoad | MacroAssembler::StoreStore);
    __ bind(notVolatile);
  }
}

void TemplateTable::putfield(int byte_no)
{
  putfield_or_static(byte_no, false);
}

void TemplateTable::nofast_putfield(int byte_no) {
  putfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::putstatic(int byte_no) {
  putfield_or_static(byte_no, true);
}

void TemplateTable::jvmti_post_fast_field_mod()
{
  if (JvmtiExport::can_post_field_modification()) {
    // Check to see if a field modification watch has been set before
    // we take the time to call into the VM.
    Label L2;
    __ lea(rscratch1, ExternalAddress((address)JvmtiExport::get_field_modification_count_addr()));
    __ ldrw(c_rarg3, Address(rscratch1));
    __ cbzw(c_rarg3, L2);
    __ pop_ptr(r19);                  // copy the object pointer from tos
    __ verify_oop(r19);
    __ push_ptr(r19);                 // put the object pointer back on tos
    // Save tos values before call_VM() clobbers them. Since we have
    // to do it for every data type, we use the saved values as the
    // jvalue object.
    switch (bytecode()) {          // load values into the jvalue object
    case Bytecodes::_fast_aputfield: __ push_ptr(r0); break;
    case Bytecodes::_fast_bputfield: // fall through
    case Bytecodes::_fast_zputfield: // fall through
    case Bytecodes::_fast_sputfield: // fall through
    case Bytecodes::_fast_cputfield: // fall through
    case Bytecodes::_fast_iputfield: __ push_i(r0); break;
    case Bytecodes::_fast_dputfield: __ push_d(); break;
    case Bytecodes::_fast_fputfield: __ push_f(); break;
    case Bytecodes::_fast_lputfield: __ push_l(r0); break;

    default:
      ShouldNotReachHere();
    }
    __ mov(c_rarg3, esp);             // points to jvalue on the stack
    // access constant pool cache entry
    __ get_cache_entry_pointer_at_bcp(c_rarg2, r0, 1);
    __ verify_oop(r19);
    // r19: object pointer copied above
    // c_rarg2: cache entry pointer
    // c_rarg3: jvalue object on the stack
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::post_field_modification),
               r19, c_rarg2, c_rarg3);

    switch (bytecode()) {             // restore tos values
    case Bytecodes::_fast_aputfield: __ pop_ptr(r0); break;
    case Bytecodes::_fast_bputfield: // fall through
    case Bytecodes::_fast_zputfield: // fall through
    case Bytecodes::_fast_sputfield: // fall through
    case Bytecodes::_fast_cputfield: // fall through
    case Bytecodes::_fast_iputfield: __ pop_i(r0); break;
    case Bytecodes::_fast_dputfield: __ pop_d(); break;
    case Bytecodes::_fast_fputfield: __ pop_f(); break;
    case Bytecodes::_fast_lputfield: __ pop_l(r0); break;
    default: break;
    }
    __ bind(L2);
  }
}

void TemplateTable::fast_storefield(TosState state)
{
  transition(state, vtos);

  ByteSize base = ConstantPoolCache::base_offset();

  jvmti_post_fast_field_mod();

  // access constant pool cache
  __ get_cache_and_index_at_bcp(r2, r1, 1);

  // Must prevent reordering of the following cp cache loads with bytecode load
  __ membar(MacroAssembler::LoadLoad);

  // test for volatile with r3
  __ ldrw(r3, Address(r2, in_bytes(base +
                                   ConstantPoolCacheEntry::flags_offset())));

  // replace index with field offset from cache entry
  __ ldr(r1, Address(r2, in_bytes(base + ConstantPoolCacheEntry::f2_offset())));

  {
    Label notVolatile;
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::StoreStore | MacroAssembler::LoadStore);
    __ bind(notVolatile);
  }

  Label notVolatile;

  // Get object from stack
  pop_and_check_object(r2);

  // field address
  const Address field(r2, r1);

  // access field
  switch (bytecode()) {
  case Bytecodes::_fast_aputfield:
    do_oop_store(_masm, field, r0, IN_HEAP);
    break;
  case Bytecodes::_fast_lputfield:
    __ access_store_at(T_LONG, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_iputfield:
    __ access_store_at(T_INT, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_zputfield:
    __ access_store_at(T_BOOLEAN, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_bputfield:
    __ access_store_at(T_BYTE, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_sputfield:
    __ access_store_at(T_SHORT, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_cputfield:
    __ access_store_at(T_CHAR, IN_HEAP, field, r0, noreg, noreg);
    break;
  case Bytecodes::_fast_fputfield:
    __ access_store_at(T_FLOAT, IN_HEAP, field, noreg /* ftos */, noreg, noreg);
    break;
  case Bytecodes::_fast_dputfield:
    __ access_store_at(T_DOUBLE, IN_HEAP, field, noreg /* dtos */, noreg, noreg);
    break;
  default:
    ShouldNotReachHere();
  }

  {
    Label notVolatile;
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::StoreLoad | MacroAssembler::StoreStore);
    __ bind(notVolatile);
  }
}


void TemplateTable::fast_accessfield(TosState state)
{
  transition(atos, state);
  // Do the JVMTI work here to avoid disturbing the register state below
  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the time to call into the VM.
    Label L1;
    __ lea(rscratch1, ExternalAddress((address) JvmtiExport::get_field_access_count_addr()));
    __ ldrw(r2, Address(rscratch1));
    __ cbzw(r2, L1);
    // access constant pool cache entry
    __ get_cache_entry_pointer_at_bcp(c_rarg2, rscratch2, 1);
    __ verify_oop(r0);
    __ push_ptr(r0);  // save object pointer before call_VM() clobbers it
    __ mov(c_rarg1, r0);
    // c_rarg1: object pointer copied above
    // c_rarg2: cache entry pointer
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address,
                                InterpreterRuntime::post_field_access),
               c_rarg1, c_rarg2);
    __ pop_ptr(r0); // restore object pointer
    __ bind(L1);
  }

  // access constant pool cache
  __ get_cache_and_index_at_bcp(r2, r1, 1);

  // Must prevent reordering of the following cp cache loads with bytecode load
  __ membar(MacroAssembler::LoadLoad);

  __ ldr(r1, Address(r2, in_bytes(ConstantPoolCache::base_offset() +
                                  ConstantPoolCacheEntry::f2_offset())));
  __ ldrw(r3, Address(r2, in_bytes(ConstantPoolCache::base_offset() +
                                   ConstantPoolCacheEntry::flags_offset())));

  // r0: object
  __ verify_oop(r0);
  __ null_check(r0);
  const Address field(r0, r1);

  // 8179954: We need to make sure that the code generated for
  // volatile accesses forms a sequentially-consistent set of
  // operations when combined with STLR and LDAR.  Without a leading
  // membar it's possible for a simple Dekker test to fail if loads
  // use LDR;DMB but stores use STLR.  This can happen if C2 compiles
  // the stores in one method and we interpret the loads in another.
  if (!CompilerConfig::is_c1_or_interpreter_only_no_jvmci()) {
    Label notVolatile;
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::AnyAny);
    __ bind(notVolatile);
  }

  // access field
  switch (bytecode()) {
  case Bytecodes::_fast_agetfield:
    do_oop_load(_masm, field, r0, IN_HEAP);
    __ verify_oop(r0);
    break;
  case Bytecodes::_fast_lgetfield:
    __ access_load_at(T_LONG, IN_HEAP, r0, field, noreg, noreg);
    break;
  case Bytecodes::_fast_igetfield:
    __ access_load_at(T_INT, IN_HEAP, r0, field, noreg, noreg);
    break;
  case Bytecodes::_fast_bgetfield:
    __ access_load_at(T_BYTE, IN_HEAP, r0, field, noreg, noreg);
    break;
  case Bytecodes::_fast_sgetfield:
    __ access_load_at(T_SHORT, IN_HEAP, r0, field, noreg, noreg);
    break;
  case Bytecodes::_fast_cgetfield:
    __ access_load_at(T_CHAR, IN_HEAP, r0, field, noreg, noreg);
    break;
  case Bytecodes::_fast_fgetfield:
    __ access_load_at(T_FLOAT, IN_HEAP, noreg /* ftos */, field, noreg, noreg);
    break;
  case Bytecodes::_fast_dgetfield:
    __ access_load_at(T_DOUBLE, IN_HEAP, noreg /* dtos */, field, noreg, noreg);
    break;
  default:
    ShouldNotReachHere();
  }
  {
    Label notVolatile;
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::LoadLoad | MacroAssembler::LoadStore);
    __ bind(notVolatile);
  }
}

void TemplateTable::fast_xaccess(TosState state)
{
  transition(vtos, state);

  // get receiver
  __ ldr(r0, aaddress(0));
  // access constant pool cache
  __ get_cache_and_index_at_bcp(r2, r3, 2);
  __ ldr(r1, Address(r2, in_bytes(ConstantPoolCache::base_offset() +
                                  ConstantPoolCacheEntry::f2_offset())));

  // 8179954: We need to make sure that the code generated for
  // volatile accesses forms a sequentially-consistent set of
  // operations when combined with STLR and LDAR.  Without a leading
  // membar it's possible for a simple Dekker test to fail if loads
  // use LDR;DMB but stores use STLR.  This can happen if C2 compiles
  // the stores in one method and we interpret the loads in another.
  if (!CompilerConfig::is_c1_or_interpreter_only_no_jvmci()) {
    Label notVolatile;
    __ ldrw(r3, Address(r2, in_bytes(ConstantPoolCache::base_offset() +
                                     ConstantPoolCacheEntry::flags_offset())));
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::AnyAny);
    __ bind(notVolatile);
  }

  // make sure exception is reported in correct bcp range (getfield is
  // next instruction)
  __ increment(rbcp);
  __ null_check(r0);
  switch (state) {
  case itos:
    __ access_load_at(T_INT, IN_HEAP, r0, Address(r0, r1, Address::lsl(0)), noreg, noreg);
    break;
  case atos:
    do_oop_load(_masm, Address(r0, r1, Address::lsl(0)), r0, IN_HEAP);
    __ verify_oop(r0);
    break;
  case ftos:
    __ access_load_at(T_FLOAT, IN_HEAP, noreg /* ftos */, Address(r0, r1, Address::lsl(0)), noreg, noreg);
    break;
  default:
    ShouldNotReachHere();
  }

  {
    Label notVolatile;
    __ ldrw(r3, Address(r2, in_bytes(ConstantPoolCache::base_offset() +
                                     ConstantPoolCacheEntry::flags_offset())));
    __ tbz(r3, ConstantPoolCacheEntry::is_volatile_shift, notVolatile);
    __ membar(MacroAssembler::LoadLoad | MacroAssembler::LoadStore);
    __ bind(notVolatile);
  }

  __ decrement(rbcp);
}



//-----------------------------------------------------------------------------
// Calls

void TemplateTable::prepare_invoke(int byte_no,
                                   Register method, // linked method (or i-klass)
                                   Register index,  // itable index, MethodType, etc.
                                   Register recv,   // if caller wants to see it
                                   Register flags   // if caller wants to test it
                                   ) {
  // determine flags
  Bytecodes::Code code = bytecode();
  const bool is_invokeinterface  = code == Bytecodes::_invokeinterface;
  const bool is_invokedynamic    = code == Bytecodes::_invokedynamic;
  const bool is_invokehandle     = code == Bytecodes::_invokehandle;
  const bool is_invokevirtual    = code == Bytecodes::_invokevirtual;
  const bool is_invokespecial    = code == Bytecodes::_invokespecial;
  const bool load_receiver       = (recv  != noreg);
  const bool save_flags          = (flags != noreg);
  assert(load_receiver == (code != Bytecodes::_invokestatic && code != Bytecodes::_invokedynamic), "");
  assert(save_flags    == (is_invokeinterface || is_invokevirtual), "need flags for vfinal");
  assert(flags == noreg || flags == r3, "");
  assert(recv  == noreg || recv  == r2, "");

  // setup registers & access constant pool cache
  if (recv  == noreg)  recv  = r2;
  if (flags == noreg)  flags = r3;
  assert_different_registers(method, index, recv, flags);

  // save 'interpreter return address'
  __ save_bcp();

  load_invoke_cp_cache_entry(byte_no, method, index, flags, is_invokevirtual, false, is_invokedynamic);

  // maybe push appendix to arguments (just before return address)
  if (is_invokedynamic || is_invokehandle) {
    Label L_no_push;
    __ tbz(flags, ConstantPoolCacheEntry::has_appendix_shift, L_no_push);
    // Push the appendix as a trailing parameter.
    // This must be done before we get the receiver,
    // since the parameter_size includes it.
    __ push(r19);
    __ mov(r19, index);
    __ load_resolved_reference_at_index(index, r19);
    __ pop(r19);
    __ push(index);  // push appendix (MethodType, CallSite, etc.)
    __ bind(L_no_push);
  }

  // load receiver if needed (note: no return address pushed yet)
  if (load_receiver) {
    __ andw(recv, flags, ConstantPoolCacheEntry::parameter_size_mask);
    // FIXME -- is this actually correct? looks like it should be 2
    // const int no_return_pc_pushed_yet = -1;  // argument slot correction before we push return address
    // const int receiver_is_at_end      = -1;  // back off one slot to get receiver
    // Address recv_addr = __ argument_address(recv, no_return_pc_pushed_yet + receiver_is_at_end);
    // __ movptr(recv, recv_addr);
    __ add(rscratch1, esp, recv, ext::uxtx, 3); // FIXME: uxtb here?
    __ ldr(recv, Address(rscratch1, -Interpreter::expr_offset_in_bytes(1)));
    __ verify_oop(recv);
  }

  // compute return type
  // x86 uses a shift and mask or wings it with a shift plus assert
  // the mask is not needed. aarch64 just uses bitfield extract
  __ ubfxw(rscratch2, flags, ConstantPoolCacheEntry::tos_state_shift,  ConstantPoolCacheEntry::tos_state_bits);
  // load return address
  {
    const address table_addr = (address) Interpreter::invoke_return_entry_table_for(code);
    __ mov(rscratch1, table_addr);
    __ ldr(lr, Address(rscratch1, rscratch2, Address::lsl(3)));
  }
}


void TemplateTable::invokevirtual_helper(Register index,
                                         Register recv,
                                         Register flags)
{
  // Uses temporary registers r0, r3
  assert_different_registers(index, recv, r0, r3);
  // Test for an invoke of a final method
  Label notFinal;
  __ tbz(flags, ConstantPoolCacheEntry::is_vfinal_shift, notFinal);

  const Register method = index;  // method must be rmethod
  assert(method == rmethod,
         "Method must be rmethod for interpreter calling convention");

  // do the call - the index is actually the method to call
  // that is, f2 is a vtable index if !is_vfinal, else f2 is a Method*

  // It's final, need a null check here!
  __ null_check(recv);

  // profile this call
  __ profile_final_call(r0);
  __ profile_arguments_type(r0, method, r4, true);

  __ jump_from_interpreted(method, r0);

  __ bind(notFinal);

  // get receiver klass
  __ null_check(recv, oopDesc::klass_offset_in_bytes());
  __ load_klass(r0, recv);

  // profile this call
  __ profile_virtual_call(r0, rlocals, r3);

  // get target Method & entry point
  __ lookup_virtual_method(r0, index, method);
  __ profile_arguments_type(r3, method, r4, true);
  // FIXME -- this looks completely redundant. is it?
  // __ ldr(r3, Address(method, Method::interpreter_entry_offset()));
  __ jump_from_interpreted(method, r3);
}

void TemplateTable::invokevirtual(int byte_no)
{
  transition(vtos, vtos);
  assert(byte_no == f2_byte, "use this argument");

  prepare_invoke(byte_no, rmethod, noreg, r2, r3);

  // rmethod: index (actually a Method*)
  // r2: receiver
  // r3: flags

  invokevirtual_helper(rmethod, r2, r3);
}

void TemplateTable::invokespecial(int byte_no)
{
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  prepare_invoke(byte_no, rmethod, noreg,  // get f1 Method*
                 r2);  // get receiver also for null check
  __ verify_oop(r2);
  __ null_check(r2);
  // do the call
  __ profile_call(r0);
  __ profile_arguments_type(r0, rmethod, rbcp, false);
  __ jump_from_interpreted(rmethod, r0);
}

void TemplateTable::invokestatic(int byte_no)
{
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  prepare_invoke(byte_no, rmethod);  // get f1 Method*
  // do the call
  __ profile_call(r0);
  __ profile_arguments_type(r0, rmethod, r4, false);
  __ jump_from_interpreted(rmethod, r0);
}

void TemplateTable::fast_invokevfinal(int byte_no)
{
  __ call_Unimplemented();
}

void TemplateTable::invokeinterface(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  prepare_invoke(byte_no, r0, rmethod,  // get f1 Klass*, f2 Method*
                 r2, r3); // recv, flags

  // r0: interface klass (from f1)
  // rmethod: method (from f2)
  // r2: receiver
  // r3: flags

  // First check for Object case, then private interface method,
  // then regular interface method.

  // Special case of invokeinterface called for virtual method of
  // java.lang.Object.  See cpCache.cpp for details.
  Label notObjectMethod;
  __ tbz(r3, ConstantPoolCacheEntry::is_forced_virtual_shift, notObjectMethod);

  invokevirtual_helper(rmethod, r2, r3);
  __ bind(notObjectMethod);

  Label no_such_interface;

  // Check for private method invocation - indicated by vfinal
  Label notVFinal;
  __ tbz(r3, ConstantPoolCacheEntry::is_vfinal_shift, notVFinal);

  // Get receiver klass into r3 - also a null check
  __ null_check(r2, oopDesc::klass_offset_in_bytes());
  __ load_klass(r3, r2);

  Label subtype;
  __ check_klass_subtype(r3, r0, r4, subtype);
  // If we get here the typecheck failed
  __ b(no_such_interface);
  __ bind(subtype);

  __ profile_final_call(r0);
  __ profile_arguments_type(r0, rmethod, r4, true);
  __ jump_from_interpreted(rmethod, r0);

  __ bind(notVFinal);

  // Get receiver klass into r3 - also a null check
  __ restore_locals();
  __ null_check(r2, oopDesc::klass_offset_in_bytes());
  __ load_klass(r3, r2);

  Label no_such_method;

  // Preserve method for throw_AbstractMethodErrorVerbose.
  __ mov(r16, rmethod);
  // Receiver subtype check against REFC.
  // Superklass in r0. Subklass in r3. Blows rscratch2, r13
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             r3, r0, noreg,
                             // outputs: scan temp. reg, scan temp. reg
                             rscratch2, r13,
                             no_such_interface,
                             /*return_method=*/false);

  // profile this call
  __ profile_virtual_call(r3, r13, r19);

  // Get declaring interface class from method, and itable index

  __ load_method_holder(r0, rmethod);
  __ ldrw(rmethod, Address(rmethod, Method::itable_index_offset()));
  __ subw(rmethod, rmethod, Method::itable_index_max);
  __ negw(rmethod, rmethod);

  // Preserve recvKlass for throw_AbstractMethodErrorVerbose.
  __ mov(rlocals, r3);
  __ lookup_interface_method(// inputs: rec. class, interface, itable index
                             rlocals, r0, rmethod,
                             // outputs: method, scan temp. reg
                             rmethod, r13,
                             no_such_interface);

  // rmethod,: Method to call
  // r2: receiver
  // Check for abstract method error
  // Note: This should be done more efficiently via a throw_abstract_method_error
  //       interpreter entry point and a conditional jump to it in case of a null
  //       method.
  __ cbz(rmethod, no_such_method);

  __ profile_arguments_type(r3, rmethod, r13, true);

  // do the call
  // r2: receiver
  // rmethod,: Method
  __ jump_from_interpreted(rmethod, r3);
  __ should_not_reach_here();

  // exception handling code follows...
  // note: must restore interpreter registers to canonical
  //       state for exception handling to work correctly!

  __ bind(no_such_method);
  // throw exception
  __ restore_bcp();      // bcp must be correct for exception handler   (was destroyed)
  __ restore_locals();   // make sure locals pointer is correct as well (was destroyed)
  // Pass arguments for generating a verbose error message.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::throw_AbstractMethodErrorVerbose), r3, r16);
  // the call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();

  __ bind(no_such_interface);
  // throw exception
  __ restore_bcp();      // bcp must be correct for exception handler   (was destroyed)
  __ restore_locals();   // make sure locals pointer is correct as well (was destroyed)
  // Pass arguments for generating a verbose error message.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address,
                   InterpreterRuntime::throw_IncompatibleClassChangeErrorVerbose), r3, r0);
  // the call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();
  return;
}

void TemplateTable::invokehandle(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  prepare_invoke(byte_no, rmethod, r0, r2);
  __ verify_method_ptr(r2);
  __ verify_oop(r2);
  __ null_check(r2);

  // FIXME: profile the LambdaForm also

  // r13 is safe to use here as a scratch reg because it is about to
  // be clobbered by jump_from_interpreted().
  __ profile_final_call(r13);
  __ profile_arguments_type(r13, rmethod, r4, true);

  __ jump_from_interpreted(rmethod, r0);
}

void TemplateTable::invokedynamic(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f1_byte, "use this argument");

  prepare_invoke(byte_no, rmethod, r0);

  // r0: CallSite object (from cpool->resolved_references[])
  // rmethod: MH.linkToCallSite method (from f2)

  // Note:  r0_callsite is already pushed by prepare_invoke

  // %%% should make a type profile for any invokedynamic that takes a ref argument
  // profile this call
  __ profile_call(rbcp);
  __ profile_arguments_type(r3, rmethod, r13, false);

  __ verify_oop(r0);

  __ jump_from_interpreted(rmethod, r0);
}


//-----------------------------------------------------------------------------
// Allocation

void TemplateTable::_new() {
  transition(vtos, atos);

  __ get_unsigned_2_byte_index_at_bcp(r3, 1);
  Label slow_case;
  Label done;
  Label initialize_header;
  Label initialize_object; // including clearing the fields

  __ get_cpool_and_tags(r4, r0);
  // Make sure the class we're about to instantiate has been resolved.
  // This is done before loading InstanceKlass to be consistent with the order
  // how Constant Pool is updated (see ConstantPool::klass_at_put)
  const int tags_offset = Array<u1>::base_offset_in_bytes();
  __ lea(rscratch1, Address(r0, r3, Address::lsl(0)));
  __ lea(rscratch1, Address(rscratch1, tags_offset));
  __ ldarb(rscratch1, rscratch1);
  __ cmp(rscratch1, (u1)JVM_CONSTANT_Class);
  __ br(Assembler::NE, slow_case);

  // get InstanceKlass
  __ load_resolved_klass_at_offset(r4, r3, r4, rscratch1);

  // make sure klass is initialized & doesn't have finalizer
  // make sure klass is fully initialized
  __ ldrb(rscratch1, Address(r4, InstanceKlass::init_state_offset()));
  __ cmp(rscratch1, (u1)InstanceKlass::fully_initialized);
  __ br(Assembler::NE, slow_case);

  // get instance_size in InstanceKlass (scaled to a count of bytes)
  __ ldrw(r3,
          Address(r4,
                  Klass::layout_helper_offset()));
  // test to see if it has a finalizer or is malformed in some way
  __ tbnz(r3, exact_log2(Klass::_lh_instance_slow_path_bit), slow_case);

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
  const bool allow_shared_alloc =
    Universe::heap()->supports_inline_contig_alloc();

  if (UseTLAB) {
    __ tlab_allocate(r0, r3, 0, noreg, r1, slow_case);

    if (ZeroTLAB) {
      // the fields have been already cleared
      __ b(initialize_header);
    } else {
      // initialize both the header and fields
      __ b(initialize_object);
    }
  } else {
    // Allocation in the shared Eden, if allowed.
    //
    // r3: instance size in bytes
    if (allow_shared_alloc) {
      __ eden_allocate(r0, r3, 0, r10, slow_case);
    }
  }

  // If UseTLAB or allow_shared_alloc are true, the object is created above and
  // there is an initialize need. Otherwise, skip and go to the slow path.
  if (UseTLAB || allow_shared_alloc) {
    // The object is initialized before the header.  If the object size is
    // zero, go directly to the header initialization.
    __ bind(initialize_object);
    __ sub(r3, r3, sizeof(oopDesc));
    __ cbz(r3, initialize_header);

    // Initialize object fields
    {
      __ add(r2, r0, sizeof(oopDesc));
      Label loop;
      __ bind(loop);
      __ str(zr, Address(__ post(r2, BytesPerLong)));
      __ sub(r3, r3, BytesPerLong);
      __ cbnz(r3, loop);
    }

    // initialize object header only.
    __ bind(initialize_header);
    __ mov(rscratch1, (intptr_t)markWord::prototype().value());
    __ str(rscratch1, Address(r0, oopDesc::mark_offset_in_bytes()));
    __ store_klass_gap(r0, zr);  // zero klass gap for compressed oops
    __ store_klass(r0, r4);      // store klass last

    {
      SkipIfEqual skip(_masm, &DTraceAllocProbes, false);
      // Trigger dtrace event for fastpath
      __ push(atos); // save the return value
      __ call_VM_leaf(
           CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_object_alloc), r0);
      __ pop(atos); // restore the return value

    }
    __ b(done);
  }

  // slow case
  __ bind(slow_case);
  __ get_constant_pool(c_rarg1);
  __ get_unsigned_2_byte_index_at_bcp(c_rarg2, 1);
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::_new), c_rarg1, c_rarg2);
  __ verify_oop(r0);

  // continue
  __ bind(done);
  // Must prevent reordering of stores for object initialization with stores that publish the new object.
  __ membar(Assembler::StoreStore);
}

void TemplateTable::newarray() {
  transition(itos, atos);
  __ load_unsigned_byte(c_rarg1, at_bcp(1));
  __ mov(c_rarg2, r0);
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::newarray),
          c_rarg1, c_rarg2);
  // Must prevent reordering of stores for object initialization with stores that publish the new object.
  __ membar(Assembler::StoreStore);
}

void TemplateTable::anewarray() {
  transition(itos, atos);
  __ get_unsigned_2_byte_index_at_bcp(c_rarg2, 1);
  __ get_constant_pool(c_rarg1);
  __ mov(c_rarg3, r0);
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::anewarray),
          c_rarg1, c_rarg2, c_rarg3);
  // Must prevent reordering of stores for object initialization with stores that publish the new object.
  __ membar(Assembler::StoreStore);
}

void TemplateTable::arraylength() {
  transition(atos, itos);
  __ null_check(r0, arrayOopDesc::length_offset_in_bytes());
  __ ldrw(r0, Address(r0, arrayOopDesc::length_offset_in_bytes()));
}

void TemplateTable::checkcast()
{
  transition(atos, atos);
  Label done, is_null, ok_is_subtype, quicked, resolved;
  __ cbz(r0, is_null);

  // Get cpool & tags index
  __ get_cpool_and_tags(r2, r3); // r2=cpool, r3=tags array
  __ get_unsigned_2_byte_index_at_bcp(r19, 1); // r19=index
  // See if bytecode has already been quicked
  __ add(rscratch1, r3, Array<u1>::base_offset_in_bytes());
  __ lea(r1, Address(rscratch1, r19));
  __ ldarb(r1, r1);
  __ cmp(r1, (u1)JVM_CONSTANT_Class);
  __ br(Assembler::EQ, quicked);

  __ push(atos); // save receiver for result, and for GC
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  // vm_result_2 has metadata result
  __ get_vm_result_2(r0, rthread);
  __ pop(r3); // restore receiver
  __ b(resolved);

  // Get superklass in r0 and subklass in r3
  __ bind(quicked);
  __ mov(r3, r0); // Save object in r3; r0 needed for subtype check
  __ load_resolved_klass_at_offset(r2, r19, r0, rscratch1); // r0 = klass

  __ bind(resolved);
  __ load_klass(r19, r3);

  // Generate subtype check.  Blows r2, r5.  Object in r3.
  // Superklass in r0.  Subklass in r19.
  __ gen_subtype_check(r19, ok_is_subtype);

  // Come here on failure
  __ push(r3);
  // object is at TOS
  __ b(Interpreter::_throw_ClassCastException_entry);

  // Come here on success
  __ bind(ok_is_subtype);
  __ mov(r0, r3); // Restore object in r3

  // Collect counts on whether this test sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ b(done);
    __ bind(is_null);
    __ profile_null_seen(r2);
  } else {
    __ bind(is_null);   // same as 'done'
  }
  __ bind(done);
}

void TemplateTable::instanceof() {
  transition(atos, itos);
  Label done, is_null, ok_is_subtype, quicked, resolved;
  __ cbz(r0, is_null);

  // Get cpool & tags index
  __ get_cpool_and_tags(r2, r3); // r2=cpool, r3=tags array
  __ get_unsigned_2_byte_index_at_bcp(r19, 1); // r19=index
  // See if bytecode has already been quicked
  __ add(rscratch1, r3, Array<u1>::base_offset_in_bytes());
  __ lea(r1, Address(rscratch1, r19));
  __ ldarb(r1, r1);
  __ cmp(r1, (u1)JVM_CONSTANT_Class);
  __ br(Assembler::EQ, quicked);

  __ push(atos); // save receiver for result, and for GC
  call_VM(r0, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  // vm_result_2 has metadata result
  __ get_vm_result_2(r0, rthread);
  __ pop(r3); // restore receiver
  __ verify_oop(r3);
  __ load_klass(r3, r3);
  __ b(resolved);

  // Get superklass in r0 and subklass in r3
  __ bind(quicked);
  __ load_klass(r3, r0);
  __ load_resolved_klass_at_offset(r2, r19, r0, rscratch1);

  __ bind(resolved);

  // Generate subtype check.  Blows r2, r5
  // Superklass in r0.  Subklass in r3.
  __ gen_subtype_check(r3, ok_is_subtype);

  // Come here on failure
  __ mov(r0, 0);
  __ b(done);
  // Come here on success
  __ bind(ok_is_subtype);
  __ mov(r0, 1);

  // Collect counts on whether this test sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ b(done);
    __ bind(is_null);
    __ profile_null_seen(r2);
  } else {
    __ bind(is_null);   // same as 'done'
  }
  __ bind(done);
  // r0 = 0: obj == NULL or  obj is not an instanceof the specified klass
  // r0 = 1: obj != NULL and obj is     an instanceof the specified klass
}

//-----------------------------------------------------------------------------
// Breakpoints
void TemplateTable::_breakpoint() {
  // Note: We get here even if we are single stepping..
  // jbug inists on setting breakpoints at every bytecode
  // even if we are in single step mode.

  transition(vtos, vtos);

  // get the unpatched byte code
  __ get_method(c_rarg1);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::get_original_bytecode_at),
             c_rarg1, rbcp);
  __ mov(r19, r0);

  // post the breakpoint event
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::_breakpoint),
             rmethod, rbcp);

  // complete the execution of original bytecode
  __ mov(rscratch1, r19);
  __ dispatch_only_normal(vtos);
}

//-----------------------------------------------------------------------------
// Exceptions

void TemplateTable::athrow() {
  transition(atos, vtos);
  __ null_check(r0);
  __ b(Interpreter::throw_exception_entry());
}

//-----------------------------------------------------------------------------
// Synchronization
//
// Note: monitorenter & exit are symmetric routines; which is reflected
//       in the assembly code structure as well
//
// Stack layout:
//
// [expressions  ] <--- esp               = expression stack top
// ..
// [expressions  ]
// [monitor entry] <--- monitor block top = expression stack bot
// ..
// [monitor entry]
// [frame data   ] <--- monitor block bot
// ...
// [saved rbp    ] <--- rbp
void TemplateTable::monitorenter()
{
  transition(atos, vtos);

  // check for NULL object
  __ null_check(r0);

  const Address monitor_block_top(
        rfp, frame::interpreter_frame_monitor_block_top_offset * wordSize);
  const Address monitor_block_bot(
        rfp, frame::interpreter_frame_initial_sp_offset * wordSize);
  const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;

  Label allocated;

  // initialize entry pointer
  __ mov(c_rarg1, zr); // points to free slot or NULL

  // find a free slot in the monitor block (result in c_rarg1)
  {
    Label entry, loop, exit;
    __ ldr(c_rarg3, monitor_block_top); // points to current entry,
                                        // starting with top-most entry
    __ lea(c_rarg2, monitor_block_bot); // points to word before bottom

    __ b(entry);

    __ bind(loop);
    // check if current entry is used
    // if not used then remember entry in c_rarg1
    __ ldr(rscratch1, Address(c_rarg3, BasicObjectLock::obj_offset_in_bytes()));
    __ cmp(zr, rscratch1);
    __ csel(c_rarg1, c_rarg3, c_rarg1, Assembler::EQ);
    // check if current entry is for same object
    __ cmp(r0, rscratch1);
    // if same object then stop searching
    __ br(Assembler::EQ, exit);
    // otherwise advance to next entry
    __ add(c_rarg3, c_rarg3, entry_size);
    __ bind(entry);
    // check if bottom reached
    __ cmp(c_rarg3, c_rarg2);
    // if not at bottom then check this entry
    __ br(Assembler::NE, loop);
    __ bind(exit);
  }

  __ cbnz(c_rarg1, allocated); // check if a slot has been found and
                            // if found, continue with that on

  // allocate one if there's no free slot
  {
    Label entry, loop;
    // 1. compute new pointers            // rsp: old expression stack top
    __ ldr(c_rarg1, monitor_block_bot);   // c_rarg1: old expression stack bottom
    __ sub(esp, esp, entry_size);         // move expression stack top
    __ sub(c_rarg1, c_rarg1, entry_size); // move expression stack bottom
    __ mov(c_rarg3, esp);                 // set start value for copy loop
    __ str(c_rarg1, monitor_block_bot);   // set new monitor block bottom

    __ sub(sp, sp, entry_size);           // make room for the monitor

    __ b(entry);
    // 2. move expression stack contents
    __ bind(loop);
    __ ldr(c_rarg2, Address(c_rarg3, entry_size)); // load expression stack
                                                   // word from old location
    __ str(c_rarg2, Address(c_rarg3, 0));          // and store it at new location
    __ add(c_rarg3, c_rarg3, wordSize);            // advance to next word
    __ bind(entry);
    __ cmp(c_rarg3, c_rarg1);        // check if bottom reached
    __ br(Assembler::NE, loop);      // if not at bottom then
                                     // copy next word
  }

  // call run-time routine
  // c_rarg1: points to monitor entry
  __ bind(allocated);

  // Increment bcp to point to the next bytecode, so exception
  // handling for async. exceptions work correctly.
  // The object has already been poped from the stack, so the
  // expression stack looks correct.
  __ increment(rbcp);

  // store object
  __ str(r0, Address(c_rarg1, BasicObjectLock::obj_offset_in_bytes()));
  __ lock_object(c_rarg1);

  // check to make sure this monitor doesn't cause stack overflow after locking
  __ save_bcp();  // in case of exception
  __ generate_stack_overflow_check(0);

  // The bcp has already been incremented. Just need to dispatch to
  // next instruction.
  __ dispatch_next(vtos);
}


void TemplateTable::monitorexit()
{
  transition(atos, vtos);

  // check for NULL object
  __ null_check(r0);

  const Address monitor_block_top(
        rfp, frame::interpreter_frame_monitor_block_top_offset * wordSize);
  const Address monitor_block_bot(
        rfp, frame::interpreter_frame_initial_sp_offset * wordSize);
  const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;

  Label found;

  // find matching slot
  {
    Label entry, loop;
    __ ldr(c_rarg1, monitor_block_top); // points to current entry,
                                        // starting with top-most entry
    __ lea(c_rarg2, monitor_block_bot); // points to word before bottom
                                        // of monitor block
    __ b(entry);

    __ bind(loop);
    // check if current entry is for same object
    __ ldr(rscratch1, Address(c_rarg1, BasicObjectLock::obj_offset_in_bytes()));
    __ cmp(r0, rscratch1);
    // if same object then stop searching
    __ br(Assembler::EQ, found);
    // otherwise advance to next entry
    __ add(c_rarg1, c_rarg1, entry_size);
    __ bind(entry);
    // check if bottom reached
    __ cmp(c_rarg1, c_rarg2);
    // if not at bottom then check this entry
    __ br(Assembler::NE, loop);
  }

  // error handling. Unlocking was not block-structured
  __ call_VM(noreg, CAST_FROM_FN_PTR(address,
                   InterpreterRuntime::throw_illegal_monitor_state_exception));
  __ should_not_reach_here();

  // call run-time routine
  __ bind(found);
  __ push_ptr(r0); // make sure object is on stack (contract with oopMaps)
  __ unlock_object(c_rarg1);
  __ pop_ptr(r0); // discard object
}


// Wide instructions
void TemplateTable::wide()
{
  __ load_unsigned_byte(r19, at_bcp(1));
  __ mov(rscratch1, (address)Interpreter::_wentry_point);
  __ ldr(rscratch1, Address(rscratch1, r19, Address::uxtw(3)));
  __ br(rscratch1);
}


// Multi arrays
void TemplateTable::multianewarray() {
  transition(vtos, atos);
  __ load_unsigned_byte(r0, at_bcp(3)); // get number of dimensions
  // last dim is on top of stack; we want address of first one:
  // first_addr = last_addr + (ndims - 1) * wordSize
  __ lea(c_rarg1, Address(esp, r0, Address::uxtw(3)));
  __ sub(c_rarg1, c_rarg1, wordSize);
  call_VM(r0,
          CAST_FROM_FN_PTR(address, InterpreterRuntime::multianewarray),
          c_rarg1);
  __ load_unsigned_byte(r1, at_bcp(3));
  __ lea(esp, Address(esp, r1, Address::uxtw(3)));
}
