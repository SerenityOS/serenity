/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2017 SAP SE. All rights reserved.
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
#include "vmreg_s390.inline.hpp"
#include "utilities/powerOfTwo.hpp"

#ifdef ASSERT
#define __ gen()->lir(__FILE__, __LINE__)->
#else
#define __ gen()->lir()->
#endif

void LIRItem::load_byte_item() {
  // Byte loads use same registers as other loads.
  load_item();
}

void LIRItem::load_nonconstant(int bits) {
  LIR_Opr r = value()->operand();
  if (_gen->can_inline_as_constant(value(), bits)) {
    if (!r->is_constant()) {
      r = LIR_OprFact::value_type(value()->type());
    }
    _result = r;
  } else {
    load_item();
  }
}

//--------------------------------------------------------------
//               LIRGenerator
//--------------------------------------------------------------

LIR_Opr LIRGenerator::exceptionOopOpr() { return FrameMap::as_oop_opr(Z_EXC_OOP); }
LIR_Opr LIRGenerator::exceptionPcOpr()  { return FrameMap::as_opr(Z_EXC_PC); }
LIR_Opr LIRGenerator::divInOpr()        { return FrameMap::Z_R11_opr; }
LIR_Opr LIRGenerator::divOutOpr()       { return FrameMap::Z_R11_opr; }
LIR_Opr LIRGenerator::remOutOpr()       { return FrameMap::Z_R10_opr; }
LIR_Opr LIRGenerator::ldivInOpr()       { return FrameMap::Z_R11_long_opr; }
LIR_Opr LIRGenerator::ldivOutOpr()      { return FrameMap::Z_R11_long_opr; }
LIR_Opr LIRGenerator::lremOutOpr()      { return FrameMap::Z_R10_long_opr; }
LIR_Opr LIRGenerator::syncLockOpr()     { return new_register(T_INT); }
LIR_Opr LIRGenerator::syncTempOpr()     { return FrameMap::Z_R13_opr; }
LIR_Opr LIRGenerator::getThreadTemp()   { return LIR_OprFact::illegalOpr; }

LIR_Opr LIRGenerator::result_register_for (ValueType* type, bool callee) {
  LIR_Opr opr;
  switch (type->tag()) {
    case intTag:    opr = FrameMap::Z_R2_opr;        break;
    case objectTag: opr = FrameMap::Z_R2_oop_opr;    break;
    case longTag:   opr = FrameMap::Z_R2_long_opr;   break;
    case floatTag:  opr = FrameMap::Z_F0_opr;        break;
    case doubleTag: opr = FrameMap::Z_F0_double_opr; break;

    case addressTag:
    default: ShouldNotReachHere(); return LIR_OprFact::illegalOpr;
  }

  assert(opr->type_field() == as_OprType(as_BasicType(type)), "type mismatch");
  return opr;
}

LIR_Opr LIRGenerator::rlock_byte(BasicType type) {
  return new_register(T_INT);
}

//--------- Loading items into registers. --------------------------------

// z/Architecture cannot inline all constants.
bool LIRGenerator::can_store_as_constant(Value v, BasicType type) const {
  if (v->type()->as_IntConstant() != NULL) {
    return Immediate::is_simm16(v->type()->as_IntConstant()->value());
  } else if (v->type()->as_LongConstant() != NULL) {
    return Immediate::is_simm16(v->type()->as_LongConstant()->value());
  } else if (v->type()->as_ObjectConstant() != NULL) {
    return v->type()->as_ObjectConstant()->value()->is_null_object();
  } else {
    return false;
  }
}

bool LIRGenerator::can_inline_as_constant(Value i, int bits) const {
  if (i->type()->as_IntConstant() != NULL) {
    return Assembler::is_simm(i->type()->as_IntConstant()->value(), bits);
  } else if (i->type()->as_LongConstant() != NULL) {
    return Assembler::is_simm(i->type()->as_LongConstant()->value(), bits);
  } else {
    return can_store_as_constant(i, as_BasicType(i->type()));
  }
}

bool LIRGenerator::can_inline_as_constant(LIR_Const* c) const {
  if (c->type() == T_INT) {
    return Immediate::is_simm20(c->as_jint());
  } else   if (c->type() == T_LONG) {
    return Immediate::is_simm20(c->as_jlong());
  }
  return false;
}

LIR_Opr LIRGenerator::safepoint_poll_register() {
  return new_register(longType);
}

LIR_Address* LIRGenerator::generate_address(LIR_Opr base, LIR_Opr index,
                                            int shift, int disp, BasicType type) {
  assert(base->is_register(), "must be");
  if (index->is_constant()) {
    intx large_disp = disp;
    LIR_Const *constant = index->as_constant_ptr();
    if (constant->type() == T_LONG) {
      large_disp += constant->as_jlong() << shift;
    } else {
      large_disp += (intx)(constant->as_jint()) << shift;
    }
    if (Displacement::is_validDisp(large_disp)) {
      return new LIR_Address(base, large_disp, type);
    }
    // Index is illegal so replace it with the displacement loaded into a register.
    index = new_pointer_register();
    __ move(LIR_OprFact::intptrConst(large_disp), index);
    return new LIR_Address(base, index, type);
  } else {
    if (shift > 0) {
      LIR_Opr tmp = new_pointer_register();
      __ shift_left(index, shift, tmp);
      index = tmp;
    }
    return new LIR_Address(base, index, disp, type);
  }
}

LIR_Address* LIRGenerator::emit_array_address(LIR_Opr array_opr, LIR_Opr index_opr,
                                              BasicType type) {
  int elem_size = type2aelembytes(type);
  int shift = exact_log2(elem_size);
  int offset_in_bytes = arrayOopDesc::base_offset_in_bytes(type);

  LIR_Address* addr;
  if (index_opr->is_constant()) {
    addr = new LIR_Address(array_opr,
                           offset_in_bytes + (intx)(index_opr->as_jint()) * elem_size, type);
  } else {
    if (index_opr->type() == T_INT) {
      LIR_Opr tmp = new_register(T_LONG);
      __ convert(Bytecodes::_i2l, index_opr, tmp);
      index_opr = tmp;
    }
    if (shift > 0) {
      __ shift_left(index_opr, shift, index_opr);
    }
    addr = new LIR_Address(array_opr,
                           index_opr,
                           offset_in_bytes, type);
  }
  return addr;
}

LIR_Opr LIRGenerator::load_immediate(int x, BasicType type) {
  LIR_Opr r = LIR_OprFact::illegalOpr;
  if (type == T_LONG) {
    r = LIR_OprFact::longConst(x);
  } else if (type == T_INT) {
    r = LIR_OprFact::intConst(x);
  } else {
    ShouldNotReachHere();
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
  __ add((LIR_Opr)addr, LIR_OprFact::intConst(step), (LIR_Opr)addr);
}

void LIRGenerator::cmp_mem_int(LIR_Condition condition, LIR_Opr base, int disp, int c, CodeEmitInfo* info) {
  LIR_Opr scratch = FrameMap::Z_R1_opr;
  __ load(new LIR_Address(base, disp, T_INT), scratch, info);
  __ cmp(condition, scratch, c);
}

void LIRGenerator::cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Opr base, int disp, BasicType type, CodeEmitInfo* info) {
  __ cmp_reg_mem(condition, reg, new LIR_Address(base, disp, type), info);
}

bool LIRGenerator::strength_reduce_multiply(LIR_Opr left, jint c, LIR_Opr result, LIR_Opr tmp) {
  if (tmp->is_valid()) {
    if (is_power_of_2(c + 1)) {
      __ move(left, tmp);
      __ shift_left(left, log2i_exact(c + 1), left);
      __ sub(left, tmp, result);
      return true;
    } else if (is_power_of_2(c - 1)) {
      __ move(left, tmp);
      __ shift_left(left, log2i_exact(c - 1), left);
      __ add(left, tmp, result);
      return true;
    }
  }
  return false;
}

void LIRGenerator::store_stack_parameter (LIR_Opr item, ByteSize offset_from_sp) {
  BasicType type = item->type();
  __ store(item, new LIR_Address(FrameMap::Z_SP_opr, in_bytes(offset_from_sp), type));
}

//----------------------------------------------------------------------
//             visitor functions
//----------------------------------------------------------------------

void LIRGenerator::array_store_check(LIR_Opr value, LIR_Opr array, CodeEmitInfo* store_check_info, ciMethod* profiled_method, int profiled_bci) {
  LIR_Opr tmp1 = new_register(objectType);
  LIR_Opr tmp2 = new_register(objectType);
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  __ store_check(value, array, tmp1, tmp2, tmp3, store_check_info, profiled_method, profiled_bci);
}

void LIRGenerator::do_MonitorEnter(MonitorEnter* x) {
  assert(x->is_pinned(),"");
  LIRItem obj(x->obj(), this);
  obj.load_item();

  set_no_result(x);

  // "lock" stores the address of the monitor stack slot, so this is not an oop.
  LIR_Opr lock = new_register(T_INT);

  CodeEmitInfo* info_for_exception = NULL;
  if (x->needs_null_check()) {
    info_for_exception = state_for (x);
  }
  // This CodeEmitInfo must not have the xhandlers because here the
  // object is already locked (xhandlers expect object to be unlocked).
  CodeEmitInfo* info = state_for (x, x->state(), true);
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

// _ineg, _lneg, _fneg, _dneg
void LIRGenerator::do_NegateOp(NegateOp* x) {
  LIRItem value(x->x(), this);
  value.load_item();
  LIR_Opr reg = rlock_result(x);
  __ negate(value.result(), reg);
}

// for _fadd, _fmul, _fsub, _fdiv, _frem
//     _dadd, _dmul, _dsub, _ddiv, _drem
void LIRGenerator::do_ArithmeticOp_FPU(ArithmeticOp* x) {
  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);
  LIRItem* left_arg  = &left;
  LIRItem* right_arg = &right;
  assert(!left.is_stack(), "can't both be memory operands");
  left.load_item();

  if (right.is_register() || right.is_constant()) {
    right.load_item();
  } else {
    right.dont_load_item();
  }

  if ((x->op() == Bytecodes::_frem) || (x->op() == Bytecodes::_drem)) {
    address entry;
    switch (x->op()) {
    case Bytecodes::_frem:
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::frem);
      break;
    case Bytecodes::_drem:
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::drem);
      break;
    default:
      ShouldNotReachHere();
    }
    LIR_Opr result = call_runtime(x->x(), x->y(), entry, x->type(), NULL);
    set_result(x, result);
  } else {
    LIR_Opr reg = rlock(x);
    LIR_Opr tmp = LIR_OprFact::illegalOpr;
    arithmetic_op_fpu(x->op(), reg, left.result(), right.result(), tmp);
    set_result(x, reg);
  }
}

// for _ladd, _lmul, _lsub, _ldiv, _lrem
void LIRGenerator::do_ArithmeticOp_Long(ArithmeticOp* x) {
  if (x->op() == Bytecodes::_ldiv || x->op() == Bytecodes::_lrem) {
    // Use shifts if divisior is a power of 2 otherwise use DSGR instruction.
    // Instruction: DSGR R1, R2
    // input : R1+1: dividend   (R1, R1+1 designate a register pair, R1 must be even)
    //         R2:   divisor
    //
    // output: R1+1: quotient
    //         R1:   remainder
    //
    // Register selection: R1:   Z_R10
    //                     R1+1: Z_R11
    //                     R2:   to be chosen by register allocator (linear scan)

    // R1, and R1+1 will be destroyed.

    LIRItem right(x->y(), this);
    LIRItem left(x->x() , this);   // Visit left second, so that the is_register test is valid.

    // Call state_for before load_item_force because state_for may
    // force the evaluation of other instructions that are needed for
    // correct debug info. Otherwise the live range of the fix
    // register might be too long.
    CodeEmitInfo* info = state_for (x);

    LIR_Opr result = rlock_result(x);
    LIR_Opr result_reg = result;
    LIR_Opr tmp = LIR_OprFact::illegalOpr;
    LIR_Opr divisor_opr = right.result();
    if (divisor_opr->is_constant() && is_power_of_2(divisor_opr->as_jlong())) {
      left.load_item();
      right.dont_load_item();
    } else {
      left.load_item_force(ldivInOpr());
      right.load_item();

      // DSGR instruction needs register pair.
      if (x->op() == Bytecodes::_ldiv) {
        result_reg = ldivOutOpr();
        tmp        = lremOutOpr();
      } else {
        result_reg = lremOutOpr();
        tmp        = ldivOutOpr();
      }
    }

    if (!ImplicitDiv0Checks) {
      __ cmp(lir_cond_equal, right.result(), LIR_OprFact::longConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(info));
      // Idiv/irem cannot trap (passing info would generate an assertion).
      info = NULL;
    }

    if (x->op() == Bytecodes::_lrem) {
      __ irem(left.result(), right.result(), result_reg, tmp, info);
    } else if (x->op() == Bytecodes::_ldiv) {
      __ idiv(left.result(), right.result(), result_reg, tmp, info);
    } else {
      ShouldNotReachHere();
    }

    if (result_reg != result) {
      __ move(result_reg, result);
    }
  } else {
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);

    left.load_item();
    right.load_nonconstant(32);
    rlock_result(x);
    arithmetic_op_long(x->op(), x->operand(), left.result(), right.result(), NULL);
  }
}

// for: _iadd, _imul, _isub, _idiv, _irem
void LIRGenerator::do_ArithmeticOp_Int(ArithmeticOp* x) {
  if (x->op() == Bytecodes::_idiv || x->op() == Bytecodes::_irem) {
    // Use shifts if divisior is a power of 2 otherwise use DSGFR instruction.
    // Instruction: DSGFR R1, R2
    // input : R1+1: dividend   (R1, R1+1 designate a register pair, R1 must be even)
    //         R2:   divisor
    //
    // output: R1+1: quotient
    //         R1:   remainder
    //
    // Register selection: R1:   Z_R10
    //                     R1+1: Z_R11
    //                     R2:   To be chosen by register allocator (linear scan).

    // R1, and R1+1 will be destroyed.

    LIRItem right(x->y(), this);
    LIRItem left(x->x() , this);   // Visit left second, so that the is_register test is valid.

    // Call state_for before load_item_force because state_for may
    // force the evaluation of other instructions that are needed for
    // correct debug info. Otherwise the live range of the fix
    // register might be too long.
    CodeEmitInfo* info = state_for (x);

    LIR_Opr result = rlock_result(x);
    LIR_Opr result_reg = result;
    LIR_Opr tmp = LIR_OprFact::illegalOpr;
    LIR_Opr divisor_opr = right.result();
    if (divisor_opr->is_constant() && is_power_of_2(divisor_opr->as_jint())) {
      left.load_item();
      right.dont_load_item();
    } else {
      left.load_item_force(divInOpr());
      right.load_item();

      // DSGFR instruction needs register pair.
      if (x->op() == Bytecodes::_idiv) {
        result_reg = divOutOpr();
        tmp        = remOutOpr();
      } else {
        result_reg = remOutOpr();
        tmp        = divOutOpr();
      }
    }

    if (!ImplicitDiv0Checks) {
      __ cmp(lir_cond_equal, right.result(), LIR_OprFact::intConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(info));
      // Idiv/irem cannot trap (passing info would generate an assertion).
      info = NULL;
    }

    if (x->op() == Bytecodes::_irem) {
      __ irem(left.result(), right.result(), result_reg, tmp, info);
    } else if (x->op() == Bytecodes::_idiv) {
      __ idiv(left.result(), right.result(), result_reg, tmp, info);
    } else {
      ShouldNotReachHere();
    }

    if (result_reg != result) {
      __ move(result_reg, result);
    }
  } else {
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

    // Do not need to load right, as we can handle stack and constants.
    if (x->op() == Bytecodes::_imul) {
      bool use_tmp = false;
      if (right_arg->is_constant()) {
        int iconst = right_arg->get_jint_constant();
        if (is_power_of_2(iconst - 1) || is_power_of_2(iconst + 1)) {
          use_tmp = true;
        }
      }
      right_arg->dont_load_item();
      LIR_Opr tmp = LIR_OprFact::illegalOpr;
      if (use_tmp) {
        tmp = new_register(T_INT);
      }
      rlock_result(x);

      arithmetic_op_int(x->op(), x->operand(), left_arg->result(), right_arg->result(), tmp);
    } else {
      right_arg->dont_load_item();
      rlock_result(x);
      LIR_Opr tmp = LIR_OprFact::illegalOpr;
      arithmetic_op_int(x->op(), x->operand(), left_arg->result(), right_arg->result(), tmp);
    }
  }
}

void LIRGenerator::do_ArithmeticOp(ArithmeticOp* x) {
  // If an operand with use count 1 is the left operand, then it is
  // likely that no move for 2-operand-LIR-form is necessary.
  if (x->is_commutative() && x->y()->as_Constant() == NULL && x->x()->use_count() > x->y()->use_count()) {
    x->swap_operands();
  }

  ValueTag tag = x->type()->tag();
  assert(x->x()->type()->tag() == tag && x->y()->type()->tag() == tag, "wrong parameters");
  switch (tag) {
    case floatTag:
    case doubleTag: do_ArithmeticOp_FPU(x);  return;
    case longTag:   do_ArithmeticOp_Long(x); return;
    case intTag:    do_ArithmeticOp_Int(x);  return;
    default:
      ShouldNotReachHere();
  }
}

// _ishl, _lshl, _ishr, _lshr, _iushr, _lushr
void LIRGenerator::do_ShiftOp(ShiftOp* x) {
  // count must always be in rcx
  LIRItem value(x->x(), this);
  LIRItem count(x->y(), this);

  ValueTag elemType = x->type()->tag();
  bool must_load_count = !count.is_constant();
  if (must_load_count) {
    count.load_item();
  } else {
    count.dont_load_item();
  }
  value.load_item();
  LIR_Opr reg = rlock_result(x);

  shift_op(x->op(), reg, value.result(), count.result(), LIR_OprFact::illegalOpr);
}

// _iand, _land, _ior, _lor, _ixor, _lxor
void LIRGenerator::do_LogicOp(LogicOp* x) {
  // IF an operand with use count 1 is the left operand, then it is
  // likely that no move for 2-operand-LIR-form is necessary.
  if (x->is_commutative() && x->y()->as_Constant() == NULL && x->x()->use_count() > x->y()->use_count()) {
    x->swap_operands();
  }

  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);

  left.load_item();
  right.load_nonconstant(32);
  LIR_Opr reg = rlock_result(x);

  logic_op(x->op(), reg, left.result(), right.result());
}

// _lcmp, _fcmpl, _fcmpg, _dcmpl, _dcmpg
void LIRGenerator::do_CompareOp(CompareOp* x) {
  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);
  left.load_item();
  right.load_item();
  LIR_Opr reg = rlock_result(x);
  if (x->x()->type()->is_float_kind()) {
    Bytecodes::Code code = x->op();
    __ fcmp2int(left.result(), right.result(), reg, (code == Bytecodes::_fcmpl || code == Bytecodes::_dcmpl));
  } else if (x->x()->type()->tag() == longTag) {
    __ lcmp2int(left.result(), right.result(), reg);
  } else {
    ShouldNotReachHere();
  }
}

LIR_Opr LIRGenerator::atomic_cmpxchg(BasicType type, LIR_Opr addr, LIRItem& cmp_value, LIRItem& new_value) {
  LIR_Opr t1 = LIR_OprFact::illegalOpr;
  LIR_Opr t2 = LIR_OprFact::illegalOpr;
  cmp_value.load_item();
  new_value.load_item();
  if (type == T_OBJECT) {
    if (UseCompressedOops) {
      t1 = new_register(T_OBJECT);
      t2 = new_register(T_OBJECT);
    }
    __ cas_obj(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), t1, t2);
  } else if (type == T_INT) {
    __ cas_int(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), t1, t2);
  } else if (type == T_LONG) {
    __ cas_long(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), t1, t2);
  } else {
    ShouldNotReachHere();
  }
  // Generate conditional move of boolean result.
  LIR_Opr result = new_register(T_INT);
  __ cmove(lir_cond_equal, LIR_OprFact::intConst(1), LIR_OprFact::intConst(0),
           result, type);
  return result;
}

LIR_Opr LIRGenerator::atomic_xchg(BasicType type, LIR_Opr addr, LIRItem& value) {
  Unimplemented(); // Currently not supported on this platform.
  return LIR_OprFact::illegalOpr;
}

LIR_Opr LIRGenerator::atomic_add(BasicType type, LIR_Opr addr, LIRItem& value) {
  LIR_Opr result = new_register(type);
  value.load_item();
  __ xadd(addr, value.result(), result, LIR_OprFact::illegalOpr);
  return result;
}

void LIRGenerator::do_MathIntrinsic(Intrinsic* x) {
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
    case vmIntrinsics::_dsin:   // fall through
    case vmIntrinsics::_dcos:   // fall through
    case vmIntrinsics::_dtan:   // fall through
    case vmIntrinsics::_dlog:   // fall through
    case vmIntrinsics::_dlog10: // fall through
    case vmIntrinsics::_dexp: {
      assert(x->number_of_arguments() == 1, "wrong type");

      address runtime_entry = NULL;
      switch (x->id()) {
        case vmIntrinsics::_dsin:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dsin);
          break;
        case vmIntrinsics::_dcos:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dcos);
          break;
        case vmIntrinsics::_dtan:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dtan);
          break;
        case vmIntrinsics::_dlog:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dlog);
          break;
        case vmIntrinsics::_dlog10:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dlog10);
          break;
        case vmIntrinsics::_dexp:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dexp);
          break;
        default:
          ShouldNotReachHere();
      }

      LIR_Opr result = call_runtime(x->argument_at(0), runtime_entry, x->type(), NULL);
      set_result(x, result);
      break;
    }
    case vmIntrinsics::_dpow: {
      assert(x->number_of_arguments() == 2, "wrong type");
      address runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dpow);
      LIR_Opr result = call_runtime(x->argument_at(0), x->argument_at(1), runtime_entry, x->type(), NULL);
      set_result(x, result);
      break;
    }
    default:
      break;
  }
}

void LIRGenerator::do_ArrayCopy(Intrinsic* x) {
  assert(x->number_of_arguments() == 5, "wrong type");

  // Copy stubs possibly call C code, e.g. G1 barriers, so we need to reserve room
  // for the C ABI (see frame::z_abi_160).
  BasicTypeArray sig; // Empty signature is precise enough.
  frame_map()->c_calling_convention(&sig);

  // Make all state_for calls early since they can emit code.
  CodeEmitInfo* info = state_for (x, x->state());

  LIRItem src(x->argument_at(0), this);
  LIRItem src_pos(x->argument_at(1), this);
  LIRItem dst(x->argument_at(2), this);
  LIRItem dst_pos(x->argument_at(3), this);
  LIRItem length(x->argument_at(4), this);

  // Operands for arraycopy must use fixed registers, otherwise
  // LinearScan will fail allocation (because arraycopy always needs a
  // call).

  src.load_item_force     (FrameMap::as_oop_opr(Z_ARG1));
  src_pos.load_item_force (FrameMap::as_opr(Z_ARG2));
  dst.load_item_force     (FrameMap::as_oop_opr(Z_ARG3));
  dst_pos.load_item_force (FrameMap::as_opr(Z_ARG4));
  length.load_item_force  (FrameMap::as_opr(Z_ARG5));

  LIR_Opr tmp =            FrameMap::as_opr(Z_R7);

  set_no_result(x);

  int flags;
  ciArrayKlass* expected_type;
  arraycopy_helper(x, &flags, &expected_type);

  __ arraycopy(src.result(), src_pos.result(), dst.result(), dst_pos.result(),
               length.result(), tmp, expected_type, flags, info); // does add_safepoint
}

// _i2l, _i2f, _i2d, _l2i, _l2f, _l2d, _f2i, _f2l, _f2d, _d2i, _d2l, _d2f
// _i2b, _i2c, _i2s
void LIRGenerator::do_Convert(Convert* x) {
  LIRItem value(x->value(), this);

  value.load_item();
  LIR_Opr reg = rlock_result(x);
  __ convert(x->op(), value.result(), reg);
}

void LIRGenerator::do_NewInstance(NewInstance* x) {
  print_if_not_loaded(x);

  // This instruction can be deoptimized in the slow path : use
  // Z_R2 as result register.
  const LIR_Opr reg = result_register_for (x->type());

  CodeEmitInfo* info = state_for (x, x->state());
  LIR_Opr tmp1 = FrameMap::Z_R12_oop_opr;
  LIR_Opr tmp2 = FrameMap::Z_R13_oop_opr;
  LIR_Opr tmp3 = reg;
  LIR_Opr tmp4 = LIR_OprFact::illegalOpr;
  LIR_Opr klass_reg = FrameMap::Z_R11_metadata_opr;
  new_instance(reg, x->klass(), x->is_unresolved(), tmp1, tmp2, tmp3, tmp4, klass_reg, info);
  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_NewTypeArray(NewTypeArray* x) {
  CodeEmitInfo* info = state_for (x, x->state());

  LIRItem length(x->length(), this);
  length.load_item();

  LIR_Opr reg = result_register_for (x->type());
  LIR_Opr tmp1 = FrameMap::Z_R12_oop_opr;
  LIR_Opr tmp2 = FrameMap::Z_R13_oop_opr;
  LIR_Opr tmp3 = reg;
  LIR_Opr tmp4 = LIR_OprFact::illegalOpr;
  LIR_Opr klass_reg = FrameMap::Z_R11_metadata_opr;
  LIR_Opr len = length.result();
  BasicType elem_type = x->elt_type();

  __ metadata2reg(ciTypeArrayKlass::make(elem_type)->constant_encoding(), klass_reg);

  CodeStub* slow_path = new NewTypeArrayStub(klass_reg, len, reg, info);
  __ allocate_array(reg, len, tmp1, tmp2, tmp3, tmp4, elem_type, klass_reg, slow_path);

  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_NewObjectArray(NewObjectArray* x) {
  // Evaluate state_for early since it may emit code.
  CodeEmitInfo* info = state_for (x, x->state());
  // In case of patching (i.e., object class is not yet loaded), we need to reexecute the instruction
  // and therefore provide the state before the parameters have been consumed.
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info = state_for (x, x->state_before());
  }

  LIRItem length(x->length(), this);
  length.load_item();

  const LIR_Opr reg = result_register_for (x->type());
  LIR_Opr tmp1 = FrameMap::Z_R12_oop_opr;
  LIR_Opr tmp2 = FrameMap::Z_R13_oop_opr;
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  LIR_Opr tmp4 = LIR_OprFact::illegalOpr;
  LIR_Opr klass_reg = FrameMap::Z_R11_metadata_opr;
  LIR_Opr len = length.result();

  CodeStub* slow_path = new NewObjectArrayStub(klass_reg, len, reg, info);
  ciKlass* obj = ciObjArrayKlass::make(x->klass());
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
    patching_info = state_for (x, x->state_before());

    // Cannot re-use same xhandlers for multiple CodeEmitInfos, so
    // clone all handlers (NOTE: Usually this is handled transparently
    // by the CodeEmitInfo cloning logic in CodeStub constructors but
    // is done explicitly here because a stub isn't being used).
    x->set_exception_handlers(new XHandlers(x->exception_handlers()));
  }
  CodeEmitInfo* info = state_for (x, x->state());

  i = dims->length();
  while (--i >= 0) {
    LIRItem* size = items->at(i);
    size->load_nonconstant(32);
    // FrameMap::_reserved_argument_area_size includes the dimensions varargs, because
    // it's initialized to hir()->max_stack() when the FrameMap is created.
    store_stack_parameter(size->result(), in_ByteSize(i*sizeof(jint) + FrameMap::first_available_sp_in_frame));
  }

  LIR_Opr klass_reg = FrameMap::Z_R3_metadata_opr;
  klass2reg_with_patching(klass_reg, x->klass(), patching_info);

  LIR_Opr rank = FrameMap::Z_R4_opr;
  __ move(LIR_OprFact::intConst(x->rank()), rank);
  LIR_Opr varargs = FrameMap::Z_R5_opr;
  __ leal(LIR_OprFact::address(new LIR_Address(FrameMap::Z_SP_opr, FrameMap::first_available_sp_in_frame, T_INT)),
          varargs);
  LIR_OprList* args = new LIR_OprList(3);
  args->append(klass_reg);
  args->append(rank);
  args->append(varargs);
  LIR_Opr reg = result_register_for (x->type());
  __ call_runtime(Runtime1::entry_for (Runtime1::new_multi_array_id),
                  LIR_OprFact::illegalOpr,
                  reg, args, info);

  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}

void LIRGenerator::do_BlockBegin(BlockBegin* x) {
  // Nothing to do.
}

void LIRGenerator::do_CheckCast(CheckCast* x) {
  LIRItem obj(x->obj(), this);

  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || (PatchALot && !x->is_incompatible_class_change_check() && !x->is_invokespecial_receiver_check())) {
    // Must do this before locking the destination register as an oop register,
    // and before the obj is loaded (the latter is for deoptimization).
    patching_info = state_for (x, x->state_before());
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
  LIR_Opr tmp1 = new_register(objectType);
  LIR_Opr tmp2 = new_register(objectType);
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  __ checkcast(reg, obj.result(), x->klass(),
               tmp1, tmp2, tmp3,
               x->direct_compare(), info_for_exception, patching_info, stub,
               x->profiled_method(), x->profiled_bci());
}


void LIRGenerator::do_InstanceOf(InstanceOf* x) {
  LIRItem obj(x->obj(), this);
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info = state_for (x, x->state_before());
  }
  // Ensure the result register is not the input register because the
  // result is initialized before the patching safepoint.
  obj.load_item();
  LIR_Opr out_reg = rlock_result(x);
  LIR_Opr tmp1 = new_register(objectType);
  LIR_Opr tmp2 = new_register(objectType);
  LIR_Opr tmp3 = LIR_OprFact::illegalOpr;
  __ instanceof(out_reg, obj.result(), x->klass(), tmp1, tmp2, tmp3,
                x->direct_compare(), patching_info,
                x->profiled_method(), x->profiled_bci());
}


void LIRGenerator::do_If (If* x) {
  assert(x->number_of_sux() == 2, "inconsistency");
  ValueTag tag = x->x()->type()->tag();
  bool is_safepoint = x->is_safepoint();

  If::Condition cond = x->cond();

  LIRItem xitem(x->x(), this);
  LIRItem yitem(x->y(), this);
  LIRItem* xin = &xitem;
  LIRItem* yin = &yitem;

  if (tag == longTag) {
    // For longs, only conditions "eql", "neq", "lss", "geq" are valid;
    // mirror for other conditions.
    if (cond == If::gtr || cond == If::leq) {
      cond = Instruction::mirror(cond);
      xin = &yitem;
      yin = &xitem;
    }
    xin->set_destroys_register();
  }
  xin->load_item();
  // TODO: don't load long constants != 0L
  if (tag == longTag && yin->is_constant() && yin->get_jlong_constant() == 0 && (cond == If::eql || cond == If::neq)) {
    // inline long zero
    yin->dont_load_item();
  } else if (tag == longTag || tag == floatTag || tag == doubleTag) {
    // Longs cannot handle constants at right side.
    yin->load_item();
  } else {
    yin->dont_load_item();
  }

  LIR_Opr left = xin->result();
  LIR_Opr right = yin->result();

  set_no_result(x);

  // Add safepoint before generating condition code so it can be recomputed.
  if (x->is_safepoint()) {
    // Increment backedge counter if needed.
    increment_backedge_counter_conditionally(lir_cond(cond), left, right, state_for(x, x->state_before()),
        x->tsux()->bci(), x->fsux()->bci(), x->profiled_bci());
    // Use safepoint_poll_register() instead of LIR_OprFact::illegalOpr.
    __ safepoint(safepoint_poll_register(), state_for (x, x->state_before()));
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
  return FrameMap::as_pointer_opr(Z_thread);
}

void LIRGenerator::trace_block_entry(BlockBegin* block) {
  __ move(LIR_OprFact::intConst(block->block_id()), FrameMap::Z_R2_opr);
  LIR_OprList* args = new LIR_OprList(1);
  args->append(FrameMap::Z_R2_opr);
  address func = CAST_FROM_FN_PTR(address, Runtime1::trace_block_entry);
  __ call_runtime_leaf(func, LIR_OprFact::illegalOpr, LIR_OprFact::illegalOpr, args);
}

void LIRGenerator::volatile_field_store(LIR_Opr value, LIR_Address* address,
                                        CodeEmitInfo* info) {
  __ store(value, address, info);
}

void LIRGenerator::volatile_field_load(LIR_Address* address, LIR_Opr result,
                                       CodeEmitInfo* info) {
  __ load(address, result, info);
}

void LIRGenerator::do_update_CRC32(Intrinsic* x) {
  assert(UseCRC32Intrinsics, "or should not be here");
  LIR_Opr result = rlock_result(x);

  switch (x->id()) {
    case vmIntrinsics::_updateCRC32: {
      LIRItem crc(x->argument_at(0), this);
      LIRItem val(x->argument_at(1), this);
      // Registers destroyed by update_crc32.
      crc.set_destroys_register();
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
      if (off.result()->is_constant()) {
        index = LIR_OprFact::illegalOpr;
        offset += off.result()->as_jint();
      }
      LIR_Opr base_op = buf.result();

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
      }

      LIR_Address* a = new LIR_Address(base_op, index, offset, T_BYTE);

      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for (x->type());

      LIR_Opr arg1 = cc->at(0);
      LIR_Opr arg2 = cc->at(1);
      LIR_Opr arg3 = cc->at(2);

      crc.load_item_force(arg1); // We skip int->long conversion here, because CRC32 stub doesn't care about high bits.
      __ leal(LIR_OprFact::address(a), arg2);
      len.load_item_force(arg3); // We skip int->long conversion here, because CRC32 stub expects int.

      __ call_runtime_leaf(StubRoutines::updateBytesCRC32(), LIR_OprFact::illegalOpr, result_reg, cc->args());
      __ move(result_reg, result);
      break;
    }
    default: {
      ShouldNotReachHere();
    }
  }
}

void LIRGenerator::do_update_CRC32C(Intrinsic* x) {
  assert(UseCRC32CIntrinsics, "or should not be here");
  LIR_Opr result = rlock_result(x);

  switch (x->id()) {
    case vmIntrinsics::_updateBytesCRC32C:
    case vmIntrinsics::_updateDirectByteBufferCRC32C: {
      bool is_updateBytes = (x->id() == vmIntrinsics::_updateBytesCRC32C);

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
      int offset = is_updateBytes ? arrayOopDesc::base_offset_in_bytes(T_BYTE) : 0;
      if (off.result()->is_constant()) {
        index = LIR_OprFact::illegalOpr;
        offset += off.result()->as_jint();
      }
      LIR_Opr base_op = buf.result();

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
      }

      LIR_Address* a = new LIR_Address(base_op, index, offset, T_BYTE);

      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for (x->type());

      LIR_Opr arg1 = cc->at(0);
      LIR_Opr arg2 = cc->at(1);
      LIR_Opr arg3 = cc->at(2);

      crc.load_item_force(arg1); // We skip int->long conversion here, because CRC32C stub doesn't care about high bits.
      __ leal(LIR_OprFact::address(a), arg2);
      __ move(len, cc->at(2));   // We skip int->long conversion here, because CRC32C stub expects int.

      __ call_runtime_leaf(StubRoutines::updateBytesCRC32C(), LIR_OprFact::illegalOpr, result_reg, cc->args());
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

  value2.set_destroys_register();

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
