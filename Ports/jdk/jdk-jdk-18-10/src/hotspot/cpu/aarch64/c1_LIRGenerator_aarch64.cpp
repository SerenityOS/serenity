/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_Compilation.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_LIRGenerator.hpp"
#include "c1/c1_Runtime1.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArray.hpp"
#include "ci/ciObjArrayKlass.hpp"
#include "ci/ciTypeArrayKlass.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_aarch64.inline.hpp"

#ifdef ASSERT
#define __ gen()->lir(__FILE__, __LINE__)->
#else
#define __ gen()->lir()->
#endif

// Item will be loaded into a byte register; Intel only
void LIRItem::load_byte_item() {
  load_item();
}


void LIRItem::load_nonconstant() {
  LIR_Opr r = value()->operand();
  if (r->is_constant()) {
    _result = r;
  } else {
    load_item();
  }
}

//--------------------------------------------------------------
//               LIRGenerator
//--------------------------------------------------------------


LIR_Opr LIRGenerator::exceptionOopOpr() { return FrameMap::r0_oop_opr; }
LIR_Opr LIRGenerator::exceptionPcOpr()  { return FrameMap::r3_opr; }
LIR_Opr LIRGenerator::divInOpr()        { Unimplemented(); return LIR_OprFact::illegalOpr; }
LIR_Opr LIRGenerator::divOutOpr()       { Unimplemented(); return LIR_OprFact::illegalOpr; }
LIR_Opr LIRGenerator::remOutOpr()       { Unimplemented(); return LIR_OprFact::illegalOpr; }
LIR_Opr LIRGenerator::shiftCountOpr()   { Unimplemented(); return LIR_OprFact::illegalOpr; }
LIR_Opr LIRGenerator::syncLockOpr()     { return new_register(T_INT); }
LIR_Opr LIRGenerator::syncTempOpr()     { return FrameMap::r0_opr; }
LIR_Opr LIRGenerator::getThreadTemp()   { return LIR_OprFact::illegalOpr; }


LIR_Opr LIRGenerator::result_register_for(ValueType* type, bool callee) {
  LIR_Opr opr;
  switch (type->tag()) {
    case intTag:     opr = FrameMap::r0_opr;          break;
    case objectTag:  opr = FrameMap::r0_oop_opr;      break;
    case longTag:    opr = FrameMap::long0_opr;        break;
    case floatTag:   opr = FrameMap::fpu0_float_opr;  break;
    case doubleTag:  opr = FrameMap::fpu0_double_opr;  break;

    case addressTag:
    default: ShouldNotReachHere(); return LIR_OprFact::illegalOpr;
  }

  assert(opr->type_field() == as_OprType(as_BasicType(type)), "type mismatch");
  return opr;
}


LIR_Opr LIRGenerator::rlock_byte(BasicType type) {
  LIR_Opr reg = new_register(T_INT);
  set_vreg_flag(reg, LIRGenerator::byte_reg);
  return reg;
}


//--------- loading items into registers --------------------------------


bool LIRGenerator::can_store_as_constant(Value v, BasicType type) const {
  if (v->type()->as_IntConstant() != NULL) {
    return v->type()->as_IntConstant()->value() == 0L;
  } else if (v->type()->as_LongConstant() != NULL) {
    return v->type()->as_LongConstant()->value() == 0L;
  } else if (v->type()->as_ObjectConstant() != NULL) {
    return v->type()->as_ObjectConstant()->value()->is_null_object();
  } else {
    return false;
  }
}

bool LIRGenerator::can_inline_as_constant(Value v) const {
  // FIXME: Just a guess
  if (v->type()->as_IntConstant() != NULL) {
    return Assembler::operand_valid_for_add_sub_immediate(v->type()->as_IntConstant()->value());
  } else if (v->type()->as_LongConstant() != NULL) {
    return v->type()->as_LongConstant()->value() == 0L;
  } else if (v->type()->as_ObjectConstant() != NULL) {
    return v->type()->as_ObjectConstant()->value()->is_null_object();
  } else {
    return false;
  }
}


bool LIRGenerator::can_inline_as_constant(LIR_Const* c) const { return false; }


LIR_Opr LIRGenerator::safepoint_poll_register() {
  return LIR_OprFact::illegalOpr;
}


LIR_Address* LIRGenerator::generate_address(LIR_Opr base, LIR_Opr index,
                                            int shift, int disp, BasicType type) {
  assert(base->is_register(), "must be");
  intx large_disp = disp;

  // accumulate fixed displacements
  if (index->is_constant()) {
    LIR_Const *constant = index->as_constant_ptr();
    if (constant->type() == T_INT) {
      large_disp += index->as_jint() << shift;
    } else {
      assert(constant->type() == T_LONG, "should be");
      jlong c = index->as_jlong() << shift;
      if ((jlong)((jint)c) == c) {
        large_disp += c;
        index = LIR_OprFact::illegalOpr;
      } else {
        LIR_Opr tmp = new_register(T_LONG);
        __ move(index, tmp);
        index = tmp;
        // apply shift and displacement below
      }
    }
  }

  if (index->is_register()) {
    // apply the shift and accumulate the displacement
    if (shift > 0) {
      LIR_Opr tmp = new_pointer_register();
      __ shift_left(index, shift, tmp);
      index = tmp;
    }
    if (large_disp != 0) {
      LIR_Opr tmp = new_pointer_register();
      if (Assembler::operand_valid_for_add_sub_immediate(large_disp)) {
        __ add(index, LIR_OprFact::intptrConst(large_disp), tmp);
        index = tmp;
      } else {
        __ move(LIR_OprFact::intptrConst(large_disp), tmp);
        __ add(tmp, index, tmp);
        index = tmp;
      }
      large_disp = 0;
    }
  } else if (large_disp != 0 && !Address::offset_ok_for_immed(large_disp, shift)) {
    // index is illegal so replace it with the displacement loaded into a register
    index = new_pointer_register();
    __ move(LIR_OprFact::intptrConst(large_disp), index);
    large_disp = 0;
  }

  // at this point we either have base + index or base + displacement
  if (large_disp == 0 && index->is_register()) {
    return new LIR_Address(base, index, type);
  } else {
    assert(Address::offset_ok_for_immed(large_disp, 0), "must be");
    return new LIR_Address(base, large_disp, type);
  }
}

LIR_Address* LIRGenerator::emit_array_address(LIR_Opr array_opr, LIR_Opr index_opr,
                                              BasicType type) {
  int offset_in_bytes = arrayOopDesc::base_offset_in_bytes(type);
  int elem_size = type2aelembytes(type);
  int shift = exact_log2(elem_size);

  LIR_Address* addr;
  if (index_opr->is_constant()) {
    addr = new LIR_Address(array_opr,
                           offset_in_bytes + (intx)(index_opr->as_jint()) * elem_size, type);
  } else {
    if (offset_in_bytes) {
      LIR_Opr tmp = new_pointer_register();
      __ add(array_opr, LIR_OprFact::intConst(offset_in_bytes), tmp);
      array_opr = tmp;
      offset_in_bytes = 0;
    }
    addr =  new LIR_Address(array_opr,
                            index_opr,
                            LIR_Address::scale(type),
                            offset_in_bytes, type);
  }
  return addr;
}

LIR_Opr LIRGenerator::load_immediate(int x, BasicType type) {
  LIR_Opr r;
  if (type == T_LONG) {
    r = LIR_OprFact::longConst(x);
    if (!Assembler::operand_valid_for_logical_immediate(false, x)) {
      LIR_Opr tmp = new_register(type);
      __ move(r, tmp);
      return tmp;
    }
  } else if (type == T_INT) {
    r = LIR_OprFact::intConst(x);
    if (!Assembler::operand_valid_for_logical_immediate(true, x)) {
      // This is all rather nasty.  We don't know whether our constant
      // is required for a logical or an arithmetic operation, wo we
      // don't know what the range of valid values is!!
      LIR_Opr tmp = new_register(type);
      __ move(r, tmp);
      return tmp;
    }
  } else {
    ShouldNotReachHere();
    r = NULL;  // unreachable
  }
  return r;
}



void LIRGenerator::increment_counter(address counter, BasicType type, int step) {
  LIR_Opr pointer = new_pointer_register();
  __ move(LIR_OprFact::intptrConst(counter), pointer);
  LIR_Address* addr = new LIR_Address(pointer, type);
  increment_counter(addr, step);
}


void LIRGenerator::increment_counter(LIR_Address* addr, int step) {
  LIR_Opr imm = NULL;
  switch(addr->type()) {
  case T_INT:
    imm = LIR_OprFact::intConst(step);
    break;
  case T_LONG:
    imm = LIR_OprFact::longConst(step);
    break;
  default:
    ShouldNotReachHere();
  }
  LIR_Opr reg = new_register(addr->type());
  __ load(addr, reg);
  __ add(reg, imm, reg);
  __ store(reg, addr);
}

void LIRGenerator::cmp_mem_int(LIR_Condition condition, LIR_Opr base, int disp, int c, CodeEmitInfo* info) {
  LIR_Opr reg = new_register(T_INT);
  __ load(generate_address(base, disp, T_INT), reg, info);
  __ cmp(condition, reg, LIR_OprFact::intConst(c));
}

void LIRGenerator::cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Opr base, int disp, BasicType type, CodeEmitInfo* info) {
  LIR_Opr reg1 = new_register(T_INT);
  __ load(generate_address(base, disp, type), reg1, info);
  __ cmp(condition, reg, reg1);
}


bool LIRGenerator::strength_reduce_multiply(LIR_Opr left, jint c, LIR_Opr result, LIR_Opr tmp) {

  if (is_power_of_2(c - 1)) {
    __ shift_left(left, exact_log2(c - 1), tmp);
    __ add(tmp, left, result);
    return true;
  } else if (is_power_of_2(c + 1)) {
    __ shift_left(left, exact_log2(c + 1), tmp);
    __ sub(tmp, left, result);
    return true;
  } else {
    return false;
  }
}

void LIRGenerator::store_stack_parameter (LIR_Opr item, ByteSize offset_from_sp) {
  BasicType type = item->type();
  __ store(item, new LIR_Address(FrameMap::sp_opr, in_bytes(offset_from_sp), type));
}

void LIRGenerator::array_store_check(LIR_Opr value, LIR_Opr array, CodeEmitInfo* store_check_info, ciMethod* profiled_method, int profiled_bci) {
    LIR_Opr tmp1 = new_register(objectType);
    LIR_Opr tmp2 = new_register(objectType);
    LIR_Opr tmp3 = new_register(objectType);
    __ store_check(value, array, tmp1, tmp2, tmp3, store_check_info, profiled_method, profiled_bci);
}

//----------------------------------------------------------------------
//             visitor functions
//----------------------------------------------------------------------

void LIRGenerator::do_MonitorEnter(MonitorEnter* x) {
  assert(x->is_pinned(),"");
  LIRItem obj(x->obj(), this);
  obj.load_item();

  set_no_result(x);

  // "lock" stores the address of the monitor stack slot, so this is not an oop
  LIR_Opr lock = new_register(T_INT);

  CodeEmitInfo* info_for_exception = NULL;
  if (x->needs_null_check()) {
    info_for_exception = state_for(x);
  }
  // this CodeEmitInfo must not have the xhandlers because here the
  // object is already locked (xhandlers expect object to be unlocked)
  CodeEmitInfo* info = state_for(x, x->state(), true);
  monitor_enter(obj.result(), lock, syncTempOpr(), LIR_OprFact::illegalOpr,
                        x->monitor_no(), info_for_exception, info);
}


void LIRGenerator::do_MonitorExit(MonitorExit* x) {
  assert(x->is_pinned(),"");

  LIRItem obj(x->obj(), this);
  obj.dont_load_item();

  LIR_Opr lock = new_register(T_INT);
  LIR_Opr obj_temp = new_register(T_INT);
  set_no_result(x);
  monitor_exit(obj_temp, lock, syncTempOpr(), LIR_OprFact::illegalOpr, x->monitor_no());
}


void LIRGenerator::do_NegateOp(NegateOp* x) {

  LIRItem from(x->x(), this);
  from.load_item();
  LIR_Opr result = rlock_result(x);
  __ negate (from.result(), result);

}

// for  _fadd, _fmul, _fsub, _fdiv, _frem
//      _dadd, _dmul, _dsub, _ddiv, _drem
void LIRGenerator::do_ArithmeticOp_FPU(ArithmeticOp* x) {

  if (x->op() == Bytecodes::_frem || x->op() == Bytecodes::_drem) {
    // float remainder is implemented as a direct call into the runtime
    LIRItem right(x->x(), this);
    LIRItem left(x->y(), this);

    BasicTypeList signature(2);
    if (x->op() == Bytecodes::_frem) {
      signature.append(T_FLOAT);
      signature.append(T_FLOAT);
    } else {
      signature.append(T_DOUBLE);
      signature.append(T_DOUBLE);
    }
    CallingConvention* cc = frame_map()->c_calling_convention(&signature);

    const LIR_Opr result_reg = result_register_for(x->type());
    left.load_item_force(cc->at(1));
    right.load_item();

    __ move(right.result(), cc->at(0));

    address entry;
    if (x->op() == Bytecodes::_frem) {
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::frem);
    } else {
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::drem);
    }

    LIR_Opr result = rlock_result(x);
    __ call_runtime_leaf(entry, getThreadTemp(), result_reg, cc->args());
    __ move(result_reg, result);

    return;
  }

  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);
  LIRItem* left_arg  = &left;
  LIRItem* right_arg = &right;

  // Always load right hand side.
  right.load_item();

  if (!left.is_register())
    left.load_item();

  LIR_Opr reg = rlock(x);

  arithmetic_op_fpu(x->op(), reg, left.result(), right.result());

  set_result(x, round_item(reg));
}

// for  _ladd, _lmul, _lsub, _ldiv, _lrem
void LIRGenerator::do_ArithmeticOp_Long(ArithmeticOp* x) {

  // missing test if instr is commutative and if we should swap
  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);

  if (x->op() == Bytecodes::_ldiv || x->op() == Bytecodes::_lrem) {

    left.load_item();
    bool need_zero_check = true;
    if (right.is_constant()) {
      jlong c = right.get_jlong_constant();
      // no need to do div-by-zero check if the divisor is a non-zero constant
      if (c != 0) need_zero_check = false;
      // do not load right if the divisor is a power-of-2 constant
      if (c > 0 && is_power_of_2(c)) {
        right.dont_load_item();
      } else {
        right.load_item();
      }
    } else {
      right.load_item();
    }
    if (need_zero_check) {
      CodeEmitInfo* info = state_for(x);
      __ cmp(lir_cond_equal, right.result(), LIR_OprFact::longConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(info));
    }

    rlock_result(x);
    switch (x->op()) {
    case Bytecodes::_lrem:
      __ rem (left.result(), right.result(), x->operand());
      break;
    case Bytecodes::_ldiv:
      __ div (left.result(), right.result(), x->operand());
      break;
    default:
      ShouldNotReachHere();
      break;
    }


  } else {
    assert (x->op() == Bytecodes::_lmul || x->op() == Bytecodes::_ladd || x->op() == Bytecodes::_lsub,
            "expect lmul, ladd or lsub");
    // add, sub, mul
    left.load_item();
    if (! right.is_register()) {
      if (x->op() == Bytecodes::_lmul
          || ! right.is_constant()
          || ! Assembler::operand_valid_for_add_sub_immediate(right.get_jlong_constant())) {
        right.load_item();
      } else { // add, sub
        assert (x->op() == Bytecodes::_ladd || x->op() == Bytecodes::_lsub, "expect ladd or lsub");
        // don't load constants to save register
        right.load_nonconstant();
      }
    }
    rlock_result(x);
    arithmetic_op_long(x->op(), x->operand(), left.result(), right.result(), NULL);
  }
}

// for: _iadd, _imul, _isub, _idiv, _irem
void LIRGenerator::do_ArithmeticOp_Int(ArithmeticOp* x) {

  // Test if instr is commutative and if we should swap
  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);
  LIRItem* left_arg = &left;
  LIRItem* right_arg = &right;
  if (x->is_commutative() && left.is_stack() && right.is_register()) {
    // swap them if left is real stack (or cached) and right is real register(not cached)
    left_arg = &right;
    right_arg = &left;
  }

  left_arg->load_item();

  // do not need to load right, as we can handle stack and constants
  if (x->op() == Bytecodes::_idiv || x->op() == Bytecodes::_irem) {

    rlock_result(x);
    bool need_zero_check = true;
    if (right.is_constant()) {
      jint c = right.get_jint_constant();
      // no need to do div-by-zero check if the divisor is a non-zero constant
      if (c != 0) need_zero_check = false;
      // do not load right if the divisor is a power-of-2 constant
      if (c > 0 && is_power_of_2(c)) {
        right_arg->dont_load_item();
      } else {
        right_arg->load_item();
      }
    } else {
      right_arg->load_item();
    }
    if (need_zero_check) {
      CodeEmitInfo* info = state_for(x);
      __ cmp(lir_cond_equal, right_arg->result(), LIR_OprFact::longConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(info));
    }

    LIR_Opr ill = LIR_OprFact::illegalOpr;
    if (x->op() == Bytecodes::_irem) {
      __ irem(left_arg->result(), right_arg->result(), x->operand(), ill, NULL);
    } else if (x->op() == Bytecodes::_idiv) {
      __ idiv(left_arg->result(), right_arg->result(), x->operand(), ill, NULL);
    }

  } else if (x->op() == Bytecodes::_iadd || x->op() == Bytecodes::_isub) {
    if (right.is_constant()
        && Assembler::operand_valid_for_add_sub_immediate(right.get_jint_constant())) {
      right.load_nonconstant();
    } else {
      right.load_item();
    }
    rlock_result(x);
    arithmetic_op_int(x->op(), x->operand(), left_arg->result(), right_arg->result(), LIR_OprFact::illegalOpr);
  } else {
    assert (x->op() == Bytecodes::_imul, "expect imul");
    if (right.is_constant()) {
      jint c = right.get_jint_constant();
      if (c > 0 && c < max_jint && (is_power_of_2(c) || is_power_of_2(c - 1) || is_power_of_2(c + 1))) {
        right_arg->dont_load_item();
      } else {
        // Cannot use constant op.
        right_arg->load_item();
      }
    } else {
      right.load_item();
    }
    rlock_result(x);
    arithmetic_op_int(x->op(), x->operand(), left_arg->result(), right_arg->result(), new_register(T_INT));
  }
}

void LIRGenerator::do_ArithmeticOp(ArithmeticOp* x) {
  // when an operand with use count 1 is the left operand, then it is
  // likely that no move for 2-operand-LIR-form is necessary
  if (x->is_commutative() && x->y()->as_Constant() == NULL && x->x()->use_count() > x->y()->use_count()) {
    x->swap_operands();
  }

  ValueTag tag = x->type()->tag();
  assert(x->x()->type()->tag() == tag && x->y()->type()->tag() == tag, "wrong parameters");
  switch (tag) {
    case floatTag:
    case doubleTag:  do_ArithmeticOp_FPU(x);  return;
    case longTag:    do_ArithmeticOp_Long(x); return;
    case intTag:     do_ArithmeticOp_Int(x);  return;
    default:         ShouldNotReachHere();    return;
  }
}

// _ishl, _lshl, _ishr, _lshr, _iushr, _lushr
void LIRGenerator::do_ShiftOp(ShiftOp* x) {

  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);

  left.load_item();

  rlock_result(x);
  if (right.is_constant()) {
    right.dont_load_item();

    switch (x->op()) {
    case Bytecodes::_ishl: {
      int c = right.get_jint_constant() & 0x1f;
      __ shift_left(left.result(), c, x->operand());
      break;
    }
    case Bytecodes::_ishr: {
      int c = right.get_jint_constant() & 0x1f;
      __ shift_right(left.result(), c, x->operand());
      break;
    }
    case Bytecodes::_iushr: {
      int c = right.get_jint_constant() & 0x1f;
      __ unsigned_shift_right(left.result(), c, x->operand());
      break;
    }
    case Bytecodes::_lshl: {
      int c = right.get_jint_constant() & 0x3f;
      __ shift_left(left.result(), c, x->operand());
      break;
    }
    case Bytecodes::_lshr: {
      int c = right.get_jint_constant() & 0x3f;
      __ shift_right(left.result(), c, x->operand());
      break;
    }
    case Bytecodes::_lushr: {
      int c = right.get_jint_constant() & 0x3f;
      __ unsigned_shift_right(left.result(), c, x->operand());
      break;
    }
    default:
      ShouldNotReachHere();
    }
  } else {
    right.load_item();
    LIR_Opr tmp = new_register(T_INT);
    switch (x->op()) {
    case Bytecodes::_ishl: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x1f), tmp);
      __ shift_left(left.result(), tmp, x->operand(), tmp);
      break;
    }
    case Bytecodes::_ishr: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x1f), tmp);
      __ shift_right(left.result(), tmp, x->operand(), tmp);
      break;
    }
    case Bytecodes::_iushr: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x1f), tmp);
      __ unsigned_shift_right(left.result(), tmp, x->operand(), tmp);
      break;
    }
    case Bytecodes::_lshl: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x3f), tmp);
      __ shift_left(left.result(), tmp, x->operand(), tmp);
      break;
    }
    case Bytecodes::_lshr: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x3f), tmp);
      __ shift_right(left.result(), tmp, x->operand(), tmp);
      break;
    }
    case Bytecodes::_lushr: {
      __ logical_and(right.result(), LIR_OprFact::intConst(0x3f), tmp);
      __ unsigned_shift_right(left.result(), tmp, x->operand(), tmp);
      break;
    }
    default:
      ShouldNotReachHere();
    }
  }
}

// _iand, _land, _ior, _lor, _ixor, _lxor
void LIRGenerator::do_LogicOp(LogicOp* x) {

  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);

  left.load_item();

  rlock_result(x);
  if (right.is_constant()
      && ((right.type()->tag() == intTag
           && Assembler::operand_valid_for_logical_immediate(true, right.get_jint_constant()))
          || (right.type()->tag() == longTag
              && Assembler::operand_valid_for_logical_immediate(false, right.get_jlong_constant()))))  {
    right.dont_load_item();
  } else {
    right.load_item();
  }
  switch (x->op()) {
  case Bytecodes::_iand:
  case Bytecodes::_land:
    __ logical_and(left.result(), right.result(), x->operand()); break;
  case Bytecodes::_ior:
  case Bytecodes::_lor:
    __ logical_or (left.result(), right.result(), x->operand()); break;
  case Bytecodes::_ixor:
  case Bytecodes::_lxor:
    __ logical_xor(left.result(), right.result(), x->operand()); break;
  default: Unimplemented();
  }
}

// _lcmp, _fcmpl, _fcmpg, _dcmpl, _dcmpg
void LIRGenerator::do_CompareOp(CompareOp* x) {
  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);
  ValueTag tag = x->x()->type()->tag();
  if (tag == longTag) {
    left.set_destroys_register();
  }
  left.load_item();
  right.load_item();
  LIR_Opr reg = rlock_result(x);

  if (x->x()->type()->is_float_kind()) {
    Bytecodes::Code code = x->op();
    __ fcmp2int(left.result(), right.result(), reg, (code == Bytecodes::_fcmpl || code == Bytecodes::_dcmpl));
  } else if (x->x()->type()->tag() == longTag) {
    __ lcmp2int(left.result(), right.result(), reg);
  } else {
    Unimplemented();
  }
}

LIR_Opr LIRGenerator::atomic_cmpxchg(BasicType type, LIR_Opr addr, LIRItem& cmp_value, LIRItem& new_value) {
  LIR_Opr ill = LIR_OprFact::illegalOpr;  // for convenience
  new_value.load_item();
  cmp_value.load_item();
  LIR_Opr result = new_register(T_INT);
  if (is_reference_type(type)) {
    __ cas_obj(addr, cmp_value.result(), new_value.result(), new_register(T_INT), new_register(T_INT), result);
  } else if (type == T_INT) {
    __ cas_int(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), ill, ill);
  } else if (type == T_LONG) {
    __ cas_long(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), ill, ill);
  } else {
    ShouldNotReachHere();
    Unimplemented();
  }
  __ logical_xor(FrameMap::r8_opr, LIR_OprFact::intConst(1), result);
  return result;
}

LIR_Opr LIRGenerator::atomic_xchg(BasicType type, LIR_Opr addr, LIRItem& value) {
  bool is_oop = is_reference_type(type);
  LIR_Opr result = new_register(type);
  value.load_item();
  assert(type == T_INT || is_oop LP64_ONLY( || type == T_LONG ), "unexpected type");
  LIR_Opr tmp = new_register(T_INT);
  __ xchg(addr, value.result(), result, tmp);
  return result;
}

LIR_Opr LIRGenerator::atomic_add(BasicType type, LIR_Opr addr, LIRItem& value) {
  LIR_Opr result = new_register(type);
  value.load_item();
  assert(type == T_INT LP64_ONLY( || type == T_LONG ), "unexpected type");
  LIR_Opr tmp = new_register(T_INT);
  __ xadd(addr, value.result(), result, tmp);
  return result;
}

void LIRGenerator::do_MathIntrinsic(Intrinsic* x) {
  assert(x->number_of_arguments() == 1 || (x->number_of_arguments() == 2 && x->id() == vmIntrinsics::_dpow), "wrong type");
  if (x->id() == vmIntrinsics::_dexp || x->id() == vmIntrinsics::_dlog ||
      x->id() == vmIntrinsics::_dpow || x->id() == vmIntrinsics::_dcos ||
      x->id() == vmIntrinsics::_dsin || x->id() == vmIntrinsics::_dtan ||
      x->id() == vmIntrinsics::_dlog10) {
    do_LibmIntrinsic(x);
    return;
  }
  switch (x->id()) {
    case vmIntrinsics::_dabs:
    case vmIntrinsics::_dsqrt: {
      assert(x->number_of_arguments() == 1, "wrong type");
      LIRItem value(x->argument_at(0), this);
      value.load_item();
      LIR_Opr dst = rlock_result(x);

      switch (x->id()) {
        case vmIntrinsics::_dsqrt: {
          __ sqrt(value.result(), dst, LIR_OprFact::illegalOpr);
          break;
        }
        case vmIntrinsics::_dabs: {
          __ abs(value.result(), dst, LIR_OprFact::illegalOpr);
          break;
        }
        default:
          ShouldNotReachHere();
      }
      break;
    }
    default:
      ShouldNotReachHere();
  }
}

void LIRGenerator::do_LibmIntrinsic(Intrinsic* x) {
  LIRItem value(x->argument_at(0), this);
  value.set_destroys_register();

  LIR_Opr calc_result = rlock_result(x);
  LIR_Opr result_reg = result_register_for(x->type());

  CallingConvention* cc = NULL;

  if (x->id() == vmIntrinsics::_dpow) {
    LIRItem value1(x->argument_at(1), this);

    value1.set_destroys_register();

    BasicTypeList signature(2);
    signature.append(T_DOUBLE);
    signature.append(T_DOUBLE);
    cc = frame_map()->c_calling_convention(&signature);
    value.load_item_force(cc->at(0));
    value1.load_item_force(cc->at(1));
  } else {
    BasicTypeList signature(1);
    signature.append(T_DOUBLE);
    cc = frame_map()->c_calling_convention(&signature);
    value.load_item_force(cc->at(0));
  }

  switch (x->id()) {
    case vmIntrinsics::_dexp:
      if (StubRoutines::dexp() != NULL) {
        __ call_runtime_leaf(StubRoutines::dexp(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dexp), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dlog:
      if (StubRoutines::dlog() != NULL) {
        __ call_runtime_leaf(StubRoutines::dlog(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dlog), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dlog10:
      if (StubRoutines::dlog10() != NULL) {
        __ call_runtime_leaf(StubRoutines::dlog10(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dlog10), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dpow:
      if (StubRoutines::dpow() != NULL) {
        __ call_runtime_leaf(StubRoutines::dpow(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dpow), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dsin:
      if (StubRoutines::dsin() != NULL) {
        __ call_runtime_leaf(StubRoutines::dsin(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dsin), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dcos:
      if (StubRoutines::dcos() != NULL) {
        __ call_runtime_leaf(StubRoutines::dcos(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dcos), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dtan:
      if (StubRoutines::dtan() != NULL) {
        __ call_runtime_leaf(StubRoutines::dtan(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dtan), getThreadTemp(), result_reg, cc->args());
      }
      break;
    default:  ShouldNotReachHere();
  }
  __ move(result_reg, calc_result);
}


void LIRGenerator::do_ArrayCopy(Intrinsic* x) {
  assert(x->number_of_arguments() == 5, "wrong type");

  // Make all state_for calls early since they can emit code
  CodeEmitInfo* info = state_for(x, x->state());

  LIRItem src(x->argument_at(0), this);
  LIRItem src_pos(x->argument_at(1), this);
  LIRItem dst(x->argument_at(2), this);
  LIRItem dst_pos(x->argument_at(3), this);
  LIRItem length(x->argument_at(4), this);

  // operands for arraycopy must use fixed registers, otherwise
  // LinearScan will fail allocation (because arraycopy always needs a
  // call)

  // The java calling convention will give us enough registers
  // so that on the stub side the args will be perfect already.
  // On the other slow/special case side we call C and the arg
  // positions are not similar enough to pick one as the best.
  // Also because the java calling convention is a "shifted" version
  // of the C convention we can process the java args trivially into C
  // args without worry of overwriting during the xfer

  src.load_item_force     (FrameMap::as_oop_opr(j_rarg0));
  src_pos.load_item_force (FrameMap::as_opr(j_rarg1));
  dst.load_item_force     (FrameMap::as_oop_opr(j_rarg2));
  dst_pos.load_item_force (FrameMap::as_opr(j_rarg3));
  length.load_item_force  (FrameMap::as_opr(j_rarg4));

  LIR_Opr tmp =           FrameMap::as_opr(j_rarg5);

  set_no_result(x);

  int flags;
  ciArrayKlass* expected_type;
  arraycopy_helper(x, &flags, &expected_type);

  __ arraycopy(src.result(), src_pos.result(), dst.result(), dst_pos.result(), length.result(), tmp, expected_type, flags, info); // does add_safepoint
}

void LIRGenerator::do_update_CRC32(Intrinsic* x) {
  assert(UseCRC32Intrinsics, "why are we here?");
  // Make all state_for calls early since they can emit code
  LIR_Opr result = rlock_result(x);
  int flags = 0;
  switch (x->id()) {
    case vmIntrinsics::_updateCRC32: {
      LIRItem crc(x->argument_at(0), this);
      LIRItem val(x->argument_at(1), this);
      // val is destroyed by update_crc32
      val.set_destroys_register();
      crc.load_item();
      val.load_item();
      __ update_crc32(crc.result(), val.result(), result);
      break;
    }
    case vmIntrinsics::_updateBytesCRC32:
    case vmIntrinsics::_updateByteBufferCRC32: {
      bool is_updateBytes = (x->id() == vmIntrinsics::_updateBytesCRC32);

      LIRItem crc(x->argument_at(0), this);
      LIRItem buf(x->argument_at(1), this);
      LIRItem off(x->argument_at(2), this);
      LIRItem len(x->argument_at(3), this);
      buf.load_item();
      off.load_nonconstant();

      LIR_Opr index = off.result();
      int offset = is_updateBytes ? arrayOopDesc::base_offset_in_bytes(T_BYTE) : 0;
      if(off.result()->is_constant()) {
        index = LIR_OprFact::illegalOpr;
       offset += off.result()->as_jint();
      }
      LIR_Opr base_op = buf.result();

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
      }

      if (offset) {
        LIR_Opr tmp = new_pointer_register();
        __ add(base_op, LIR_OprFact::intConst(offset), tmp);
        base_op = tmp;
        offset = 0;
      }

      LIR_Address* a = new LIR_Address(base_op,
                                       index,
                                       offset,
                                       T_BYTE);
      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for(x->type());

      LIR_Opr addr = new_pointer_register();
      __ leal(LIR_OprFact::address(a), addr);

      crc.load_item_force(cc->at(0));
      __ move(addr, cc->at(1));
      len.load_item_force(cc->at(2));

      __ call_runtime_leaf(StubRoutines::updateBytesCRC32(), getThreadTemp(), result_reg, cc->args());
      __ move(result_reg, result);

      break;
    }
    default: {
      ShouldNotReachHere();
    }
  }
}

void LIRGenerator::do_update_CRC32C(Intrinsic* x) {
  assert(UseCRC32CIntrinsics, "why are we here?");
  // Make all state_for calls early since they can emit code
  LIR_Opr result = rlock_result(x);
  int flags = 0;
  switch (x->id()) {
    case vmIntrinsics::_updateBytesCRC32C:
    case vmIntrinsics::_updateDirectByteBufferCRC32C: {
      bool is_updateBytes = (x->id() == vmIntrinsics::_updateBytesCRC32C);
      int offset = is_updateBytes ? arrayOopDesc::base_offset_in_bytes(T_BYTE) : 0;

      LIRItem crc(x->argument_at(0), this);
      LIRItem buf(x->argument_at(1), this);
      LIRItem off(x->argument_at(2), this);
      LIRItem end(x->argument_at(3), this);

      buf.load_item();
      off.load_nonconstant();
      end.load_nonconstant();

      // len = end - off
      LIR_Opr len  = end.result();
      LIR_Opr tmpA = new_register(T_INT);
      LIR_Opr tmpB = new_register(T_INT);
      __ move(end.result(), tmpA);
      __ move(off.result(), tmpB);
      __ sub(tmpA, tmpB, tmpA);
      len = tmpA;

      LIR_Opr index = off.result();
      if(off.result()->is_constant()) {
        index = LIR_OprFact::illegalOpr;
        offset += off.result()->as_jint();
      }
      LIR_Opr base_op = buf.result();

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
      }

      if (offset) {
        LIR_Opr tmp = new_pointer_register();
        __ add(base_op, LIR_OprFact::intConst(offset), tmp);
        base_op = tmp;
        offset = 0;
      }

      LIR_Address* a = new LIR_Address(base_op,
                                       index,
                                       offset,
                                       T_BYTE);
      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for(x->type());

      LIR_Opr addr = new_pointer_register();
      __ leal(LIR_OprFact::address(a), addr);

      crc.load_item_force(cc->at(0));
      __ move(addr, cc->at(1));
      __ move(len, cc->at(2));

      __ call_runtime_leaf(StubRoutines::updateBytesCRC32C(), getThreadTemp(), result_reg, cc->args());
      __ move(result_reg, result);

      break;
    }
    default: {
      ShouldNotReachHere();
    }
  }
}

void LIRGenerator::do_FmaIntrinsic(Intrinsic* x) {
  assert(x->number_of_arguments() == 3, "wrong type");
  assert(UseFMA, "Needs FMA instructions support.");
  LIRItem value(x->argument_at(0), this);
  LIRItem value1(x->argument_at(1), this);
  LIRItem value2(x->argument_at(2), this);

  value.load_item();
  value1.load_item();
  value2.load_item();

  LIR_Opr calc_input = value.result();
  LIR_Opr calc_input1 = value1.result();
  LIR_Opr calc_input2 = value2.result();
  LIR_Opr calc_result = rlock_result(x);

  switch (x->id()) {
  case vmIntrinsics::_fmaD:   __ fmad(calc_input, calc_input1, calc_input2, calc_result); break;
  case vmIntrinsics::_fmaF:   __ fmaf(calc_input, calc_input1, calc_input2, calc_result); break;
  default:                    ShouldNotReachHere();
  }
}

void LIRGenerator::do_vectorizedMismatch(Intrinsic* x) {
  fatal("vectorizedMismatch intrinsic is not implemented on this platform");
}

// _i2l, _i2f, _i2d, _l2i, _l2f, _l2d, _f2i, _f2l, _f2d, _d2i, _d2l, _d2f
// _i2b, _i2c, _i2s
void LIRGenerator::do_Convert(Convert* x) {
  LIRItem value(x->value(), this);
  value.load_item();
  LIR_Opr input = value.result();
  LIR_Opr result = rlock(x);

  // arguments of lir_convert
  LIR_Opr conv_input = input;
  LIR_Opr conv_result = result;

  __ convert(x->op(), conv_input, conv_result);

  assert(result->is_virtual(), "result must be virtual register");
  set_result(x, result);
}

void LIRGenerator::do_NewInstance(NewInstance* x) {
#ifndef PRODUCT
  if (PrintNotLoaded && !x->klass()->is_loaded()) {
    tty->print_cr("   ###class not loaded at new bci %d", x->printable_bci());
  }
#endif
  CodeEmitInfo* info = state_for(x, x->state());
  LIR_Opr reg = result_register_for(x->type());
  new_instance(reg, x->klass(), x->is_unresolved(),
                       FrameMap::r10_oop_opr,
                       FrameMap::r11_oop_opr,
                       FrameMap::r4_oop_opr,
                       LIR_OprFact::illegalOpr,
                       FrameMap::r3_metadata_opr, info);
  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_NewTypeArray(NewTypeArray* x) {
  CodeEmitInfo* info = state_for(x, x->state());

  LIRItem length(x->length(), this);
  length.load_item_force(FrameMap::r19_opr);

  LIR_Opr reg = result_register_for(x->type());
  LIR_Opr tmp1 = FrameMap::r10_oop_opr;
  LIR_Opr tmp2 = FrameMap::r11_oop_opr;
  LIR_Opr tmp3 = FrameMap::r5_oop_opr;
  LIR_Opr tmp4 = reg;
  LIR_Opr klass_reg = FrameMap::r3_metadata_opr;
  LIR_Opr len = length.result();
  BasicType elem_type = x->elt_type();

  __ metadata2reg(ciTypeArrayKlass::make(elem_type)->constant_encoding(), klass_reg);

  CodeStub* slow_path = new NewTypeArrayStub(klass_reg, len, reg, info);
  __ allocate_array(reg, len, tmp1, tmp2, tmp3, tmp4, elem_type, klass_reg, slow_path);

  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_NewObjectArray(NewObjectArray* x) {
  LIRItem length(x->length(), this);
  // in case of patching (i.e., object class is not yet loaded), we need to reexecute the instruction
  // and therefore provide the state before the parameters have been consumed
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info =  state_for(x, x->state_before());
  }

  CodeEmitInfo* info = state_for(x, x->state());

  LIR_Opr reg = result_register_for(x->type());
  LIR_Opr tmp1 = FrameMap::r10_oop_opr;
  LIR_Opr tmp2 = FrameMap::r11_oop_opr;
  LIR_Opr tmp3 = FrameMap::r5_oop_opr;
  LIR_Opr tmp4 = reg;
  LIR_Opr klass_reg = FrameMap::r3_metadata_opr;

  length.load_item_force(FrameMap::r19_opr);
  LIR_Opr len = length.result();

  CodeStub* slow_path = new NewObjectArrayStub(klass_reg, len, reg, info);
  ciKlass* obj = (ciKlass*) ciObjArrayKlass::make(x->klass());
  if (obj == ciEnv::unloaded_ciobjarrayklass()) {
    BAILOUT("encountered unloaded_ciobjarrayklass due to out of memory error");
  }
  klass2reg_with_patching(klass_reg, obj, patching_info);
  __ allocate_array(reg, len, tmp1, tmp2, tmp3, tmp4, T_OBJECT, klass_reg, slow_path);

  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}


void LIRGenerator::do_NewMultiArray(NewMultiArray* x) {
  Values* dims = x->dims();
  int i = dims->length();
  LIRItemList* items = new LIRItemList(i, i, NULL);
  while (i-- > 0) {
    LIRItem* size = new LIRItem(dims->at(i), this);
    items->at_put(i, size);
  }

  // Evaluate state_for early since it may emit code.
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info = state_for(x, x->state_before());

    // Cannot re-use same xhandlers for multiple CodeEmitInfos, so
    // clone all handlers (NOTE: Usually this is handled transparently
    // by the CodeEmitInfo cloning logic in CodeStub constructors but
    // is done explicitly here because a stub isn't being used).
    x->set_exception_handlers(new XHandlers(x->exception_handlers()));
  }
  CodeEmitInfo* info = state_for(x, x->state());

  i = dims->length();
  while (i-- > 0) {
    LIRItem* size = items->at(i);
    size->load_item();

    store_stack_parameter(size->result(), in_ByteSize(i*4));
  }

  LIR_Opr klass_reg = FrameMap::r0_metadata_opr;
  klass2reg_with_patching(klass_reg, x->klass(), patching_info);

  LIR_Opr rank = FrameMap::r19_opr;
  __ move(LIR_OprFact::intConst(x->rank()), rank);
  LIR_Opr varargs = FrameMap::r2_opr;
  __ move(FrameMap::sp_opr, varargs);
  LIR_OprList* args = new LIR_OprList(3);
  args->append(klass_reg);
  args->append(rank);
  args->append(varargs);
  LIR_Opr reg = result_register_for(x->type());
  __ call_runtime(Runtime1::entry_for(Runtime1::new_multi_array_id),
                  LIR_OprFact::illegalOpr,
                  reg, args, info);

  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_BlockBegin(BlockBegin* x) {
  // nothing to do for now
}

void LIRGenerator::do_CheckCast(CheckCast* x) {
  LIRItem obj(x->obj(), this);

  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || (PatchALot && !x->is_incompatible_class_change_check() && !x->is_invokespecial_receiver_check())) {
    // must do this before locking the destination register as an oop register,
    // and before the obj is loaded (the latter is for deoptimization)
    patching_info = state_for(x, x->state_before());
  }
  obj.load_item();

  // info for exceptions
  CodeEmitInfo* info_for_exception =
      (x->needs_exception_state() ? state_for(x) :
                                    state_for(x, x->state_before(), true /*ignore_xhandler*/));

  CodeStub* stub;
  if (x->is_incompatible_class_change_check()) {
    assert(patching_info == NULL, "can't patch this");
    stub = new SimpleExceptionStub(Runtime1::throw_incompatible_class_change_error_id, LIR_OprFact::illegalOpr, info_for_exception);
  } else if (x->is_invokespecial_receiver_check()) {
    assert(patching_info == NULL, "can't patch this");
    stub = new DeoptimizeStub(info_for_exception,
                              Deoptimization::Reason_class_check,
                              Deoptimization::Action_none);
  } else {
    stub = new SimpleExceptionStub(Runtime1::throw_class_cast_exception_id, obj.result(), info_for_exception);
  }
  LIR_Opr reg = rlock_result(x);
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  if (!x->klass()->is_loaded() || UseCompressedClassPointers) {
    tmp3 = new_register(objectType);
  }
  __ checkcast(reg, obj.result(), x->klass(),
               new_register(objectType), new_register(objectType), tmp3,
               x->direct_compare(), info_for_exception, patching_info, stub,
               x->profiled_method(), x->profiled_bci());
}

void LIRGenerator::do_InstanceOf(InstanceOf* x) {
  LIRItem obj(x->obj(), this);

  // result and test object may not be in same register
  LIR_Opr reg = rlock_result(x);
  CodeEmitInfo* patching_info = NULL;
  if ((!x->klass()->is_loaded() || PatchALot)) {
    // must do this before locking the destination register as an oop register
    patching_info = state_for(x, x->state_before());
  }
  obj.load_item();
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  if (!x->klass()->is_loaded() || UseCompressedClassPointers) {
    tmp3 = new_register(objectType);
  }
  __ instanceof(reg, obj.result(), x->klass(),
                new_register(objectType), new_register(objectType), tmp3,
                x->direct_compare(), patching_info, x->profiled_method(), x->profiled_bci());
}

void LIRGenerator::do_If(If* x) {
  assert(x->number_of_sux() == 2, "inconsistency");
  ValueTag tag = x->x()->type()->tag();
  bool is_safepoint = x->is_safepoint();

  If::Condition cond = x->cond();

  LIRItem xitem(x->x(), this);
  LIRItem yitem(x->y(), this);
  LIRItem* xin = &xitem;
  LIRItem* yin = &yitem;

  if (tag == longTag) {
    // for longs, only conditions "eql", "neq", "lss", "geq" are valid;
    // mirror for other conditions
    if (cond == If::gtr || cond == If::leq) {
      cond = Instruction::mirror(cond);
      xin = &yitem;
      yin = &xitem;
    }
    xin->set_destroys_register();
  }
  xin->load_item();

  if (tag == longTag) {
    if (yin->is_constant()
        && Assembler::operand_valid_for_add_sub_immediate(yin->get_jlong_constant())) {
      yin->dont_load_item();
    } else {
      yin->load_item();
    }
  } else if (tag == intTag) {
    if (yin->is_constant()
        && Assembler::operand_valid_for_add_sub_immediate(yin->get_jint_constant()))  {
      yin->dont_load_item();
    } else {
      yin->load_item();
    }
  } else {
    yin->load_item();
  }

  set_no_result(x);

  LIR_Opr left = xin->result();
  LIR_Opr right = yin->result();

  // add safepoint before generating condition code so it can be recomputed
  if (x->is_safepoint()) {
    // increment backedge counter if needed
    increment_backedge_counter_conditionally(lir_cond(cond), left, right, state_for(x, x->state_before()),
        x->tsux()->bci(), x->fsux()->bci(), x->profiled_bci());
    __ safepoint(LIR_OprFact::illegalOpr, state_for(x, x->state_before()));
  }

  __ cmp(lir_cond(cond), left, right);
  // Generate branch profiling. Profiling code doesn't kill flags.
  profile_branch(x, cond);
  move_to_phi(x->state());
  if (x->x()->type()->is_float_kind()) {
    __ branch(lir_cond(cond), x->tsux(), x->usux());
  } else {
    __ branch(lir_cond(cond), x->tsux());
  }
  assert(x->default_sux() == x->fsux(), "wrong destination above");
  __ jump(x->default_sux());
}

LIR_Opr LIRGenerator::getThreadPointer() {
   return FrameMap::as_pointer_opr(rthread);
}

void LIRGenerator::trace_block_entry(BlockBegin* block) { Unimplemented(); }

void LIRGenerator::volatile_field_store(LIR_Opr value, LIR_Address* address,
                                        CodeEmitInfo* info) {
  __ volatile_store_mem_reg(value, address, info);
}

void LIRGenerator::volatile_field_load(LIR_Address* address, LIR_Opr result,
                                       CodeEmitInfo* info) {
  // 8179954: We need to make sure that the code generated for
  // volatile accesses forms a sequentially-consistent set of
  // operations when combined with STLR and LDAR.  Without a leading
  // membar it's possible for a simple Dekker test to fail if loads
  // use LD;DMB but stores use STLR.  This can happen if C2 compiles
  // the stores in one method and C1 compiles the loads in another.
  if (!CompilerConfig::is_c1_only_no_jvmci()) {
    __ membar();
  }
  __ volatile_load_mem_reg(address, result, info);
}
