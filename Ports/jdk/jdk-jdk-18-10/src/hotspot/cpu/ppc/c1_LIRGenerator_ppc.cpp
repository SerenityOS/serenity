/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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
#include "runtime/vm_version.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_ppc.inline.hpp"

#ifdef ASSERT
#define __ gen()->lir(__FILE__, __LINE__)->
#else
#define __ gen()->lir()->
#endif

void LIRItem::load_byte_item() {
  // Byte loads use same registers as other loads.
  load_item();
}


void LIRItem::load_nonconstant() {
  LIR_Opr r = value()->operand();
  if (_gen->can_inline_as_constant(value())) {
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

LIR_Opr LIRGenerator::exceptionOopOpr()              { return FrameMap::R3_oop_opr; }
LIR_Opr LIRGenerator::exceptionPcOpr()               { return FrameMap::R4_opr; }
LIR_Opr LIRGenerator::syncLockOpr()                  { return FrameMap::R5_opr; }     // Need temp effect for MonitorEnterStub.
LIR_Opr LIRGenerator::syncTempOpr()                  { return FrameMap::R4_oop_opr; } // Need temp effect for MonitorEnterStub.
LIR_Opr LIRGenerator::getThreadTemp()                { return LIR_OprFact::illegalOpr; } // not needed

LIR_Opr LIRGenerator::result_register_for(ValueType* type, bool callee) {
  LIR_Opr opr;
  switch (type->tag()) {
  case intTag:     opr = FrameMap::R3_opr;         break;
  case objectTag:  opr = FrameMap::R3_oop_opr;     break;
  case longTag:    opr = FrameMap::R3_long_opr;    break;
  case floatTag:   opr = FrameMap::F1_opr;         break;
  case doubleTag:  opr = FrameMap::F1_double_opr;  break;

  case addressTag:
  default: ShouldNotReachHere(); return LIR_OprFact::illegalOpr;
  }

  assert(opr->type_field() == as_OprType(as_BasicType(type)), "type mismatch");
  return opr;
}

LIR_Opr LIRGenerator::rlock_callee_saved(BasicType type) {
  ShouldNotReachHere();
  return LIR_OprFact::illegalOpr;
}


LIR_Opr LIRGenerator::rlock_byte(BasicType type) {
  return new_register(T_INT);
}


//--------- loading items into registers --------------------------------

// PPC cannot inline all constants.
bool LIRGenerator::can_store_as_constant(Value v, BasicType type) const {
  if (v->type()->as_IntConstant() != NULL) {
    return Assembler::is_simm16(v->type()->as_IntConstant()->value());
  } else if (v->type()->as_LongConstant() != NULL) {
    return Assembler::is_simm16(v->type()->as_LongConstant()->value());
  } else if (v->type()->as_ObjectConstant() != NULL) {
    return v->type()->as_ObjectConstant()->value()->is_null_object();
  } else {
    return false;
  }
}


// Only simm16 constants can be inlined.
bool LIRGenerator::can_inline_as_constant(Value i) const {
  return can_store_as_constant(i, as_BasicType(i->type()));
}


bool LIRGenerator::can_inline_as_constant(LIR_Const* c) const {
  if (c->type() == T_INT) {
    return Assembler::is_simm16(c->as_jint());
  }
  if (c->type() == T_LONG) {
    return Assembler::is_simm16(c->as_jlong());
  }
  if (c->type() == T_OBJECT) {
    return c->as_jobject() == NULL;
  }
  return false;
}


LIR_Opr LIRGenerator::safepoint_poll_register() {
  return new_register(T_INT);
}


LIR_Address* LIRGenerator::generate_address(LIR_Opr base, LIR_Opr index,
                                            int shift, int disp, BasicType type) {
  assert(base->is_register(), "must be");
  intx large_disp = disp;

  // Accumulate fixed displacements.
  if (index->is_constant()) {
    LIR_Const *constant = index->as_constant_ptr();
    if (constant->type() == T_LONG) {
      large_disp += constant->as_jlong() << shift;
    } else {
      large_disp += (intx)(constant->as_jint()) << shift;
    }
    index = LIR_OprFact::illegalOpr;
  }

  if (index->is_register()) {
    // Apply the shift and accumulate the displacement.
    if (shift > 0) {
      LIR_Opr tmp = new_pointer_register();
      __ shift_left(index, shift, tmp);
      index = tmp;
    }
    if (large_disp != 0) {
      LIR_Opr tmp = new_pointer_register();
      if (Assembler::is_simm16(large_disp)) {
        __ add(index, LIR_OprFact::intptrConst(large_disp), tmp);
        index = tmp;
      } else {
        __ move(LIR_OprFact::intptrConst(large_disp), tmp);
        __ add(tmp, index, tmp);
        index = tmp;
      }
      large_disp = 0;
    }
  } else if (!Assembler::is_simm16(large_disp)) {
    // Index is illegal so replace it with the displacement loaded into a register.
    index = new_pointer_register();
    __ move(LIR_OprFact::intptrConst(large_disp), index);
    large_disp = 0;
  }

  // At this point we either have base + index or base + displacement.
  if (large_disp == 0) {
    return new LIR_Address(base, index, type);
  } else {
    assert(Assembler::is_simm16(large_disp), "must be");
    return new LIR_Address(base, large_disp, type);
  }
}


LIR_Address* LIRGenerator::emit_array_address(LIR_Opr array_opr, LIR_Opr index_opr,
                                              BasicType type) {
  int elem_size = type2aelembytes(type);
  int shift = exact_log2(elem_size);

  LIR_Opr base_opr;
  intx offset = arrayOopDesc::base_offset_in_bytes(type);

  if (index_opr->is_constant()) {
    intx i = index_opr->as_constant_ptr()->as_jint();
    intx array_offset = i * elem_size;
    if (Assembler::is_simm16(array_offset + offset)) {
      base_opr = array_opr;
      offset = array_offset + offset;
    } else {
      base_opr = new_pointer_register();
      if (Assembler::is_simm16(array_offset)) {
        __ add(array_opr, LIR_OprFact::intptrConst(array_offset), base_opr);
      } else {
        __ move(LIR_OprFact::intptrConst(array_offset), base_opr);
        __ add(base_opr, array_opr, base_opr);
      }
    }
  } else {
#ifdef _LP64
    if (index_opr->type() == T_INT) {
      LIR_Opr tmp = new_register(T_LONG);
      __ convert(Bytecodes::_i2l, index_opr, tmp);
      index_opr = tmp;
    }
#endif

    base_opr = new_pointer_register();
    assert (index_opr->is_register(), "Must be register");
    if (shift > 0) {
      __ shift_left(index_opr, shift, base_opr);
      __ add(base_opr, array_opr, base_opr);
    } else {
      __ add(index_opr, array_opr, base_opr);
    }
  }
  return new LIR_Address(base_opr, offset, type);
}


LIR_Opr LIRGenerator::load_immediate(int x, BasicType type) {
  LIR_Opr r = NULL;
  if (type == T_LONG) {
    r = LIR_OprFact::longConst(x);
  } else if (type == T_INT) {
    r = LIR_OprFact::intConst(x);
  } else {
    ShouldNotReachHere();
  }
  if (!Assembler::is_simm16(x)) {
    LIR_Opr tmp = new_register(type);
    __ move(r, tmp);
    return tmp;
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
  LIR_Opr temp = new_register(addr->type());
  __ move(addr, temp);
  __ add(temp, load_immediate(step, addr->type()), temp);
  __ move(temp, addr);
}


void LIRGenerator::cmp_mem_int(LIR_Condition condition, LIR_Opr base, int disp, int c, CodeEmitInfo* info) {
  LIR_Opr tmp = FrameMap::R0_opr;
  __ load(new LIR_Address(base, disp, T_INT), tmp, info);
  __ cmp(condition, tmp, c);
}


void LIRGenerator::cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Opr base,
                               int disp, BasicType type, CodeEmitInfo* info) {
  LIR_Opr tmp = FrameMap::R0_opr;
  __ load(new LIR_Address(base, disp, type), tmp, info);
  __ cmp(condition, reg, tmp);
}


bool LIRGenerator::strength_reduce_multiply(LIR_Opr left, jint c, LIR_Opr result, LIR_Opr tmp) {
  assert(left != result, "should be different registers");
  if (is_power_of_2(c + 1)) {
    __ shift_left(left, log2i_exact(c + 1), result);
    __ sub(result, left, result);
    return true;
  } else if (is_power_of_2(c - 1)) {
    __ shift_left(left, log2i_exact(c - 1), result);
    __ add(result, left, result);
    return true;
  }
  return false;
}


void LIRGenerator::store_stack_parameter(LIR_Opr item, ByteSize offset_from_sp) {
  BasicType t = item->type();
  LIR_Opr sp_opr = FrameMap::SP_opr;
  __ move(item, new LIR_Address(sp_opr, in_bytes(offset_from_sp), t));
}


//----------------------------------------------------------------------
//             visitor functions
//----------------------------------------------------------------------

void LIRGenerator::array_store_check(LIR_Opr value, LIR_Opr array, CodeEmitInfo* store_check_info, ciMethod* profiled_method, int profiled_bci) {
  // Following registers are used by slow_subtype_check:
  LIR_Opr tmp1 = FrameMap::R4_opr; // super_klass
  LIR_Opr tmp2 = FrameMap::R5_opr; // sub_klass
  LIR_Opr tmp3 = FrameMap::R6_opr; // temp
  __ store_check(value, array, tmp1, tmp2, tmp3, store_check_info, profiled_method, profiled_bci);
}


void LIRGenerator::do_MonitorEnter(MonitorEnter* x) {
  assert(x->is_pinned(),"");
  LIRItem obj(x->obj(), this);
  obj.load_item();

  set_no_result(x);

  // We use R4+R5 in order to get a temp effect. These regs are used in slow path (MonitorEnterStub).
  LIR_Opr lock    = FrameMap::R5_opr;
  LIR_Opr scratch = FrameMap::R4_opr;
  LIR_Opr hdr     = FrameMap::R6_opr;

  CodeEmitInfo* info_for_exception = NULL;
  if (x->needs_null_check()) {
    info_for_exception = state_for(x);
  }

  // This CodeEmitInfo must not have the xhandlers because here the
  // object is already locked (xhandlers expects object to be unlocked).
  CodeEmitInfo* info = state_for(x, x->state(), true);
  monitor_enter(obj.result(), lock, hdr, scratch, x->monitor_no(), info_for_exception, info);
}


void LIRGenerator::do_MonitorExit(MonitorExit* x) {
  assert(x->is_pinned(),"");
  LIRItem obj(x->obj(), this);
  obj.dont_load_item();

  set_no_result(x);
  LIR_Opr lock     = FrameMap::R5_opr;
  LIR_Opr hdr      = FrameMap::R4_opr; // Used for slow path (MonitorExitStub).
  LIR_Opr obj_temp = FrameMap::R6_opr;
  monitor_exit(obj_temp, lock, hdr, LIR_OprFact::illegalOpr, x->monitor_no());
}


// _ineg, _lneg, _fneg, _dneg
void LIRGenerator::do_NegateOp(NegateOp* x) {
  LIRItem value(x->x(), this);
  value.load_item();
  LIR_Opr reg = rlock_result(x);
  __ negate(value.result(), reg);
}


// for  _fadd, _fmul, _fsub, _fdiv, _frem
//      _dadd, _dmul, _dsub, _ddiv, _drem
void LIRGenerator::do_ArithmeticOp_FPU(ArithmeticOp* x) {
  switch (x->op()) {
  case Bytecodes::_fadd:
  case Bytecodes::_fmul:
  case Bytecodes::_fsub:
  case Bytecodes::_fdiv:
  case Bytecodes::_dadd:
  case Bytecodes::_dmul:
  case Bytecodes::_dsub:
  case Bytecodes::_ddiv: {
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);
    left.load_item();
    right.load_item();
    rlock_result(x);
    arithmetic_op_fpu(x->op(), x->operand(), left.result(), right.result());
  }
  break;

  case Bytecodes::_frem:
  case Bytecodes::_drem: {
    address entry = NULL;
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
  }
  break;

  default: ShouldNotReachHere();
  }
}


// for  _ladd, _lmul, _lsub, _ldiv, _lrem
void LIRGenerator::do_ArithmeticOp_Long(ArithmeticOp* x) {
  bool is_div_rem = x->op() == Bytecodes::_ldiv || x->op() == Bytecodes::_lrem;

  LIRItem right(x->y(), this);
  // Missing test if instr is commutative and if we should swap.
  if (right.value()->type()->as_LongConstant() &&
      (x->op() == Bytecodes::_lsub && right.value()->type()->as_LongConstant()->value() == ((-1)<<15)) ) {
    // Sub is implemented by addi and can't support min_simm16 as constant..
    right.load_item();
  } else {
    right.load_nonconstant();
  }
  assert(right.is_constant() || right.is_register(), "wrong state of right");

  if (is_div_rem) {
    LIR_Opr divisor = right.result();
    if (divisor->is_register()) {
      CodeEmitInfo* null_check_info = state_for(x);
      __ cmp(lir_cond_equal, divisor, LIR_OprFact::longConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(null_check_info));
    } else {
      jlong const_divisor = divisor->as_constant_ptr()->as_jlong();
      if (const_divisor == 0) {
        CodeEmitInfo* null_check_info = state_for(x);
        __ jump(new DivByZeroStub(null_check_info));
        rlock_result(x);
        __ move(LIR_OprFact::longConst(0), x->operand()); // dummy
        return;
      }
      if (x->op() == Bytecodes::_lrem && !is_power_of_2(const_divisor) && const_divisor != -1) {
        // Remainder computation would need additional tmp != R0.
        right.load_item();
      }
    }
  }

  LIRItem left(x->x(), this);
  left.load_item();
  rlock_result(x);
  if (is_div_rem) {
    CodeEmitInfo* info = NULL; // Null check already done above.
    LIR_Opr tmp = FrameMap::R0_opr;
    if (x->op() == Bytecodes::_lrem) {
      __ irem(left.result(), right.result(), x->operand(), tmp, info);
    } else if (x->op() == Bytecodes::_ldiv) {
      __ idiv(left.result(), right.result(), x->operand(), tmp, info);
    }
  } else {
    arithmetic_op_long(x->op(), x->operand(), left.result(), right.result(), NULL);
  }
}


// for: _iadd, _imul, _isub, _idiv, _irem
void LIRGenerator::do_ArithmeticOp_Int(ArithmeticOp* x) {
  bool is_div_rem = x->op() == Bytecodes::_idiv || x->op() == Bytecodes::_irem;

  LIRItem right(x->y(), this);
  // Missing test if instr is commutative and if we should swap.
  if (right.value()->type()->as_IntConstant() &&
      (x->op() == Bytecodes::_isub && right.value()->type()->as_IntConstant()->value() == ((-1)<<15)) ) {
    // Sub is implemented by addi and can't support min_simm16 as constant.
    right.load_item();
  } else {
    right.load_nonconstant();
  }
  assert(right.is_constant() || right.is_register(), "wrong state of right");

  if (is_div_rem) {
    LIR_Opr divisor = right.result();
    if (divisor->is_register()) {
      CodeEmitInfo* null_check_info = state_for(x);
      __ cmp(lir_cond_equal, divisor, LIR_OprFact::intConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(null_check_info));
    } else {
      jint const_divisor = divisor->as_constant_ptr()->as_jint();
      if (const_divisor == 0) {
        CodeEmitInfo* null_check_info = state_for(x);
        __ jump(new DivByZeroStub(null_check_info));
        rlock_result(x);
        __ move(LIR_OprFact::intConst(0), x->operand()); // dummy
        return;
      }
      if (x->op() == Bytecodes::_irem && !is_power_of_2(const_divisor) && const_divisor != -1) {
        // Remainder computation would need additional tmp != R0.
        right.load_item();
      }
    }
  }

  LIRItem left(x->x(), this);
  left.load_item();
  rlock_result(x);
  if (is_div_rem) {
    CodeEmitInfo* info = NULL; // Null check already done above.
    LIR_Opr tmp = FrameMap::R0_opr;
    if (x->op() == Bytecodes::_irem) {
      __ irem(left.result(), right.result(), x->operand(), tmp, info);
    } else if (x->op() == Bytecodes::_idiv) {
      __ idiv(left.result(), right.result(), x->operand(), tmp, info);
    }
  } else {
    arithmetic_op_int(x->op(), x->operand(), left.result(), right.result(), FrameMap::R0_opr);
  }
}


void LIRGenerator::do_ArithmeticOp(ArithmeticOp* x) {
  ValueTag tag = x->type()->tag();
  assert(x->x()->type()->tag() == tag && x->y()->type()->tag() == tag, "wrong parameters");
  switch (tag) {
    case floatTag:
    case doubleTag: do_ArithmeticOp_FPU(x);  return;
    case longTag:   do_ArithmeticOp_Long(x); return;
    case intTag:    do_ArithmeticOp_Int(x);  return;
    default: ShouldNotReachHere();
  }
}


// _ishl, _lshl, _ishr, _lshr, _iushr, _lushr
void LIRGenerator::do_ShiftOp(ShiftOp* x) {
  LIRItem value(x->x(), this);
  LIRItem count(x->y(), this);
  value.load_item();
  LIR_Opr reg = rlock_result(x);
  LIR_Opr mcount;
  if (count.result()->is_register()) {
    mcount = FrameMap::R0_opr;
  } else {
    mcount = LIR_OprFact::illegalOpr;
  }
  shift_op(x->op(), reg, value.result(), count.result(), mcount);
}


inline bool can_handle_logic_op_as_uimm(ValueType *type, Bytecodes::Code bc) {
  jlong int_or_long_const;
  if (type->as_IntConstant()) {
    int_or_long_const = type->as_IntConstant()->value();
  } else if (type->as_LongConstant()) {
    int_or_long_const = type->as_LongConstant()->value();
  } else if (type->as_ObjectConstant()) {
    return type->as_ObjectConstant()->value()->is_null_object();
  } else {
    return false;
  }

  if (Assembler::is_uimm(int_or_long_const, 16)) return true;
  if ((int_or_long_const & 0xFFFF) == 0 &&
      Assembler::is_uimm((jlong)((julong)int_or_long_const >> 16), 16)) return true;

  // see Assembler::andi
  if (bc == Bytecodes::_iand &&
      (is_power_of_2(int_or_long_const+1) ||
       is_power_of_2(int_or_long_const) ||
       is_power_of_2(-int_or_long_const))) return true;
  if (bc == Bytecodes::_land &&
      (is_power_of_2(int_or_long_const+1) ||
       (Assembler::is_uimm(int_or_long_const, 32) && is_power_of_2(int_or_long_const)) ||
       (int_or_long_const != min_jlong && is_power_of_2(-int_or_long_const)))) return true;

  // special case: xor -1
  if ((bc == Bytecodes::_ixor || bc == Bytecodes::_lxor) &&
      int_or_long_const == -1) return true;
  return false;
}


// _iand, _land, _ior, _lor, _ixor, _lxor
void LIRGenerator::do_LogicOp(LogicOp* x) {
  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);

  left.load_item();

  Value rval = right.value();
  LIR_Opr r = rval->operand();
  ValueType *type = rval->type();
  // Logic instructions use unsigned immediate values.
  if (can_handle_logic_op_as_uimm(type, x->op())) {
    if (!r->is_constant()) {
      r = LIR_OprFact::value_type(type);
      rval->set_operand(r);
    }
    right.set_result(r);
  } else {
    right.load_item();
  }

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
    Unimplemented();
  }
}


LIR_Opr LIRGenerator::atomic_cmpxchg(BasicType type, LIR_Opr addr, LIRItem& cmp_value, LIRItem& new_value) {
  LIR_Opr result = new_register(T_INT);
  LIR_Opr t1 = LIR_OprFact::illegalOpr;
  LIR_Opr t2 = LIR_OprFact::illegalOpr;
  cmp_value.load_item();
  new_value.load_item();

  // Volatile load may be followed by Unsafe CAS.
  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar();
  } else {
    __ membar_release();
  }

  if (is_reference_type(type)) {
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
    Unimplemented();
  }
  __ cmove(lir_cond_equal, LIR_OprFact::intConst(1), LIR_OprFact::intConst(0),
           result, type);
  return result;
}


LIR_Opr LIRGenerator::atomic_xchg(BasicType type, LIR_Opr addr, LIRItem& value) {
  LIR_Opr result = new_register(type);
  LIR_Opr tmp = FrameMap::R0_opr;

  value.load_item();

  // Volatile load may be followed by Unsafe CAS.
  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar();
  } else {
    __ membar_release();
  }

  __ xchg(addr, value.result(), result, tmp);

  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar_acquire();
  } else {
    __ membar();
  }
  return result;
}


LIR_Opr LIRGenerator::atomic_add(BasicType type, LIR_Opr addr, LIRItem& value) {
  LIR_Opr result = new_register(type);
  LIR_Opr tmp = FrameMap::R0_opr;

  value.load_item();

  // Volatile load may be followed by Unsafe CAS.
  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar(); // To be safe. Unsafe semantics are unclear.
  } else {
    __ membar_release();
  }

  __ xadd(addr, value.result(), result, tmp);

  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ membar_acquire();
  } else {
    __ membar();
  }
  return result;
}


void LIRGenerator::do_MathIntrinsic(Intrinsic* x) {
  switch (x->id()) {
    case vmIntrinsics::_dabs: {
      assert(x->number_of_arguments() == 1, "wrong type");
      LIRItem value(x->argument_at(0), this);
      value.load_item();
      LIR_Opr dst = rlock_result(x);
      __ abs(value.result(), dst, LIR_OprFact::illegalOpr);
      break;
    }
    case vmIntrinsics::_dsqrt: {
      if (VM_Version::has_fsqrt()) {
        assert(x->number_of_arguments() == 1, "wrong type");
        LIRItem value(x->argument_at(0), this);
        value.load_item();
        LIR_Opr dst = rlock_result(x);
        __ sqrt(value.result(), dst, LIR_OprFact::illegalOpr);
        break;
      } // else fallthru
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
        case vmIntrinsics::_dsqrt:
          runtime_entry = CAST_FROM_FN_PTR(address, SharedRuntime::dsqrt);
          break;
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

  // Make all state_for calls early since they can emit code.
  CodeEmitInfo* info = state_for(x, x->state());

  LIRItem src     (x->argument_at(0), this);
  LIRItem src_pos (x->argument_at(1), this);
  LIRItem dst     (x->argument_at(2), this);
  LIRItem dst_pos (x->argument_at(3), this);
  LIRItem length  (x->argument_at(4), this);

  // Load all values in callee_save_registers (C calling convention),
  // as this makes the parameter passing to the fast case simpler.
  src.load_item_force     (FrameMap::R14_oop_opr);
  src_pos.load_item_force (FrameMap::R15_opr);
  dst.load_item_force     (FrameMap::R17_oop_opr);
  dst_pos.load_item_force (FrameMap::R18_opr);
  length.load_item_force  (FrameMap::R19_opr);
  LIR_Opr tmp =            FrameMap::R20_opr;

  int flags;
  ciArrayKlass* expected_type;
  arraycopy_helper(x, &flags, &expected_type);

  __ arraycopy(src.result(), src_pos.result(), dst.result(), dst_pos.result(),
               length.result(), tmp,
               expected_type, flags, info);
  set_no_result(x);
}


// _i2l, _i2f, _i2d, _l2i, _l2f, _l2d, _f2i, _f2l, _f2d, _d2i, _d2l, _d2f
// _i2b, _i2c, _i2s
void LIRGenerator::do_Convert(Convert* x) {
  if (!VM_Version::has_mtfprd()) {
    switch (x->op()) {

      // int -> float: force spill
      case Bytecodes::_l2f: {
        if (!VM_Version::has_fcfids()) { // fcfids is >= Power7 only
          // fcfid+frsp needs fixup code to avoid rounding incompatibility.
          address entry = CAST_FROM_FN_PTR(address, SharedRuntime::l2f);
          LIR_Opr result = call_runtime(x->value(), entry, x->type(), NULL);
          set_result(x, result);
          return;
        } // else fallthru
      }
      case Bytecodes::_l2d: {
        LIRItem value(x->value(), this);
        LIR_Opr reg = rlock_result(x);
        value.load_item();
        LIR_Opr tmp = force_to_spill(value.result(), T_DOUBLE);
        __ convert(x->op(), tmp, reg);
        return;
      }
      case Bytecodes::_i2f:
      case Bytecodes::_i2d: {
        LIRItem value(x->value(), this);
        LIR_Opr reg = rlock_result(x);
        value.load_item();
        // Convert i2l first.
        LIR_Opr tmp1 = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, value.result(), tmp1);
        LIR_Opr tmp2 = force_to_spill(tmp1, T_DOUBLE);
        __ convert(x->op(), tmp2, reg);
        return;
      }

      // float -> int: result will be stored
      case Bytecodes::_f2l:
      case Bytecodes::_d2l: {
        LIRItem value(x->value(), this);
        LIR_Opr reg = rlock_result(x);
        value.set_destroys_register(); // USE_KILL
        value.load_item();
        set_vreg_flag(reg, must_start_in_memory);
        __ convert(x->op(), value.result(), reg);
        return;
      }
      case Bytecodes::_f2i:
      case Bytecodes::_d2i: {
        LIRItem value(x->value(), this);
        LIR_Opr reg = rlock_result(x);
        value.set_destroys_register(); // USE_KILL
        value.load_item();
        // Convert l2i afterwards.
        LIR_Opr tmp1 = new_register(T_LONG);
        set_vreg_flag(tmp1, must_start_in_memory);
        __ convert(x->op(), value.result(), tmp1);
        __ convert(Bytecodes::_l2i, tmp1, reg);
        return;
      }

      // Within same category: just register conversions.
      case Bytecodes::_i2b:
      case Bytecodes::_i2c:
      case Bytecodes::_i2s:
      case Bytecodes::_i2l:
      case Bytecodes::_l2i:
      case Bytecodes::_f2d:
      case Bytecodes::_d2f:
        break;

      default: ShouldNotReachHere();
    }
  }

  // Register conversion.
  LIRItem value(x->value(), this);
  LIR_Opr reg = rlock_result(x);
  value.load_item();
  switch (x->op()) {
    case Bytecodes::_f2l:
    case Bytecodes::_d2l:
    case Bytecodes::_f2i:
    case Bytecodes::_d2i: value.set_destroys_register(); break; // USE_KILL
    default: break;
  }
  __ convert(x->op(), value.result(), reg);
}


void LIRGenerator::do_NewInstance(NewInstance* x) {
  // This instruction can be deoptimized in the slow path.
  const LIR_Opr reg = result_register_for(x->type());
#ifndef PRODUCT
  if (PrintNotLoaded && !x->klass()->is_loaded()) {
    tty->print_cr("   ###class not loaded at new bci %d", x->printable_bci());
  }
#endif
  CodeEmitInfo* info = state_for(x, x->state());
  LIR_Opr klass_reg = FrameMap::R4_metadata_opr; // Used by slow path (NewInstanceStub).
  LIR_Opr tmp1 = FrameMap::R5_oop_opr;
  LIR_Opr tmp2 = FrameMap::R6_oop_opr;
  LIR_Opr tmp3 = FrameMap::R7_oop_opr;
  LIR_Opr tmp4 = FrameMap::R8_oop_opr;
  new_instance(reg, x->klass(), x->is_unresolved(), tmp1, tmp2, tmp3, tmp4, klass_reg, info);

  // Must prevent reordering of stores for object initialization
  // with stores that publish the new object.
  __ membar_storestore();
  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}


void LIRGenerator::do_NewTypeArray(NewTypeArray* x) {
  // Evaluate state_for early since it may emit code.
  CodeEmitInfo* info = state_for(x, x->state());

  LIRItem length(x->length(), this);
  length.load_item();

  LIR_Opr reg = result_register_for(x->type());
  LIR_Opr klass_reg = FrameMap::R4_metadata_opr; // Used by slow path (NewTypeArrayStub).
  // We use R5 in order to get a temp effect. This reg is used in slow path (NewTypeArrayStub).
  LIR_Opr tmp1 = FrameMap::R5_oop_opr;
  LIR_Opr tmp2 = FrameMap::R6_oop_opr;
  LIR_Opr tmp3 = FrameMap::R7_oop_opr;
  LIR_Opr tmp4 = FrameMap::R8_oop_opr;
  LIR_Opr len = length.result();
  BasicType elem_type = x->elt_type();

  __ metadata2reg(ciTypeArrayKlass::make(elem_type)->constant_encoding(), klass_reg);

  CodeStub* slow_path = new NewTypeArrayStub(klass_reg, len, reg, info);
  __ allocate_array(reg, len, tmp1, tmp2, tmp3, tmp4, elem_type, klass_reg, slow_path);

  // Must prevent reordering of stores for object initialization
  // with stores that publish the new object.
  __ membar_storestore();
  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}


void LIRGenerator::do_NewObjectArray(NewObjectArray* x) {
  // Evaluate state_for early since it may emit code.
  CodeEmitInfo* info = state_for(x, x->state());
  // In case of patching (i.e., object class is not yet loaded),
  // we need to reexecute the instruction and therefore provide
  // the state before the parameters have been consumed.
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info = state_for(x, x->state_before());
  }

  LIRItem length(x->length(), this);
  length.load_item();

  const LIR_Opr reg = result_register_for(x->type());
  LIR_Opr klass_reg = FrameMap::R4_metadata_opr; // Used by slow path (NewObjectArrayStub).
  // We use R5 in order to get a temp effect. This reg is used in slow path (NewObjectArrayStub).
  LIR_Opr tmp1 = FrameMap::R5_oop_opr;
  LIR_Opr tmp2 = FrameMap::R6_oop_opr;
  LIR_Opr tmp3 = FrameMap::R7_oop_opr;
  LIR_Opr tmp4 = FrameMap::R8_oop_opr;
  LIR_Opr len = length.result();

  CodeStub* slow_path = new NewObjectArrayStub(klass_reg, len, reg, info);
  ciMetadata* obj = ciObjArrayKlass::make(x->klass());
  if (obj == ciEnv::unloaded_ciobjarrayklass()) {
    BAILOUT("encountered unloaded_ciobjarrayklass due to out of memory error");
  }
  klass2reg_with_patching(klass_reg, obj, patching_info);
  __ allocate_array(reg, len, tmp1, tmp2, tmp3, tmp4, T_OBJECT, klass_reg, slow_path);

  // Must prevent reordering of stores for object initialization
  // with stores that publish the new object.
  __ membar_storestore();
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
    size->load_nonconstant();
    // FrameMap::_reserved_argument_area_size includes the dimensions
    // varargs, because it's initialized to hir()->max_stack() when the
    // FrameMap is created.
    store_stack_parameter(size->result(), in_ByteSize(i*sizeof(jint) + FrameMap::first_available_sp_in_frame));
  }

  const LIR_Opr klass_reg = FrameMap::R4_metadata_opr; // Used by slow path.
  klass2reg_with_patching(klass_reg, x->klass(), patching_info);

  LIR_Opr rank = FrameMap::R5_opr; // Used by slow path.
  __ move(LIR_OprFact::intConst(x->rank()), rank);

  LIR_Opr varargs = FrameMap::as_pointer_opr(R6); // Used by slow path.
  __ leal(LIR_OprFact::address(new LIR_Address(FrameMap::SP_opr, FrameMap::first_available_sp_in_frame, T_INT)),
          varargs);

  // Note: This instruction can be deoptimized in the slow path.
  LIR_OprList* args = new LIR_OprList(3);
  args->append(klass_reg);
  args->append(rank);
  args->append(varargs);
  const LIR_Opr reg = result_register_for(x->type());
  __ call_runtime(Runtime1::entry_for(Runtime1::new_multi_array_id),
                  LIR_OprFact::illegalOpr,
                  reg, args, info);

  // Must prevent reordering of stores for object initialization
  // with stores that publish the new object.
  __ membar_storestore();
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
    // Must do this before locking the destination register as
    // an oop register, and before the obj is loaded (so x->obj()->item()
    // is valid for creating a debug info location).
    patching_info = state_for(x, x->state_before());
  }
  obj.load_item();
  LIR_Opr out_reg = rlock_result(x);
  CodeStub* stub;
  CodeEmitInfo* info_for_exception =
      (x->needs_exception_state() ? state_for(x) :
                                    state_for(x, x->state_before(), true /*ignore_xhandler*/));

  if (x->is_incompatible_class_change_check()) {
    assert(patching_info == NULL, "can't patch this");
    stub = new SimpleExceptionStub(Runtime1::throw_incompatible_class_change_error_id,
                                   LIR_OprFact::illegalOpr, info_for_exception);
  } else if (x->is_invokespecial_receiver_check()) {
    assert(patching_info == NULL, "can't patch this");
    stub = new DeoptimizeStub(info_for_exception,
                              Deoptimization::Reason_class_check,
                              Deoptimization::Action_none);
  } else {
    stub = new SimpleExceptionStub(Runtime1::throw_class_cast_exception_id, obj.result(), info_for_exception);
  }
  // Following registers are used by slow_subtype_check:
  LIR_Opr tmp1 = FrameMap::R4_oop_opr; // super_klass
  LIR_Opr tmp2 = FrameMap::R5_oop_opr; // sub_klass
  LIR_Opr tmp3 = FrameMap::R6_oop_opr; // temp
  __ checkcast(out_reg, obj.result(), x->klass(), tmp1, tmp2, tmp3,
               x->direct_compare(), info_for_exception, patching_info, stub,
               x->profiled_method(), x->profiled_bci());
}


void LIRGenerator::do_InstanceOf(InstanceOf* x) {
  LIRItem obj(x->obj(), this);
  CodeEmitInfo* patching_info = NULL;
  if (!x->klass()->is_loaded() || PatchALot) {
    patching_info = state_for(x, x->state_before());
  }
  // Ensure the result register is not the input register because the
  // result is initialized before the patching safepoint.
  obj.load_item();
  LIR_Opr out_reg = rlock_result(x);
  // Following registers are used by slow_subtype_check:
  LIR_Opr tmp1 = FrameMap::R4_oop_opr; // super_klass
  LIR_Opr tmp2 = FrameMap::R5_oop_opr; // sub_klass
  LIR_Opr tmp3 = FrameMap::R6_oop_opr; // temp
  __ instanceof(out_reg, obj.result(), x->klass(), tmp1, tmp2, tmp3,
                x->direct_compare(), patching_info,
                x->profiled_method(), x->profiled_bci());
}


void LIRGenerator::do_If(If* x) {
  assert(x->number_of_sux() == 2, "inconsistency");
  ValueTag tag = x->x()->type()->tag();
  LIRItem xitem(x->x(), this);
  LIRItem yitem(x->y(), this);
  LIRItem* xin = &xitem;
  LIRItem* yin = &yitem;
  If::Condition cond = x->cond();

  LIR_Opr left = LIR_OprFact::illegalOpr;
  LIR_Opr right = LIR_OprFact::illegalOpr;

  xin->load_item();
  left = xin->result();

  if (yin->result()->is_constant() && yin->result()->type() == T_INT &&
      Assembler::is_simm16(yin->result()->as_constant_ptr()->as_jint())) {
    // Inline int constants which are small enough to be immediate operands.
    right = LIR_OprFact::value_type(yin->value()->type());
  } else if (tag == longTag && yin->is_constant() && yin->get_jlong_constant() == 0 &&
             (cond == If::eql || cond == If::neq)) {
    // Inline long zero.
    right = LIR_OprFact::value_type(yin->value()->type());
  } else if (tag == objectTag && yin->is_constant() && (yin->get_jobject_constant()->is_null_object())) {
    right = LIR_OprFact::value_type(yin->value()->type());
  } else {
    yin->load_item();
    right = yin->result();
  }
  set_no_result(x);

  // Add safepoint before generating condition code so it can be recomputed.
  if (x->is_safepoint()) {
    // Increment backedge counter if needed.
    increment_backedge_counter_conditionally(lir_cond(cond), left, right, state_for(x, x->state_before()),
        x->tsux()->bci(), x->fsux()->bci(), x->profiled_bci());
    __ safepoint(safepoint_poll_register(), state_for(x, x->state_before()));
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
  return FrameMap::as_pointer_opr(R16_thread);
}


void LIRGenerator::trace_block_entry(BlockBegin* block) {
  LIR_Opr arg1 = FrameMap::R3_opr; // ARG1
  __ move(LIR_OprFact::intConst(block->block_id()), arg1);
  LIR_OprList* args = new LIR_OprList(1);
  args->append(arg1);
  address func = CAST_FROM_FN_PTR(address, Runtime1::trace_block_entry);
  __ call_runtime_leaf(func, LIR_OprFact::illegalOpr, LIR_OprFact::illegalOpr, args);
}


void LIRGenerator::volatile_field_store(LIR_Opr value, LIR_Address* address,
                                        CodeEmitInfo* info) {
#ifdef _LP64
  __ store(value, address, info);
#else
  Unimplemented();
//  __ volatile_store_mem_reg(value, address, info);
#endif
}

void LIRGenerator::volatile_field_load(LIR_Address* address, LIR_Opr result,
                                       CodeEmitInfo* info) {
#ifdef _LP64
  __ load(address, result, info);
#else
  Unimplemented();
//  __ volatile_load_mem_reg(address, result, info);
#endif
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
      LIR_Address* a = NULL;

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
        __ add(index, LIR_OprFact::intptrConst(offset), index);
        a = new LIR_Address(base_op, index, T_BYTE);
      } else {
        a = new LIR_Address(base_op, offset, T_BYTE);
      }

      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for(x->type());

      LIR_Opr arg1 = cc->at(0),
              arg2 = cc->at(1),
              arg3 = cc->at(2);

      crc.load_item_force(arg1); // We skip int->long conversion here, because CRC32 stub doesn't care about high bits.
      __ leal(LIR_OprFact::address(a), arg2);
      len.load_item_force(arg3); // We skip int->long conversion here, , because CRC32 stub expects int.

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
      LIR_Address* a = NULL;

      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
        __ add(index, LIR_OprFact::intptrConst(offset), index);
        a = new LIR_Address(base_op, index, T_BYTE);
      } else {
        a = new LIR_Address(base_op, offset, T_BYTE);
      }

      BasicTypeList signature(3);
      signature.append(T_INT);
      signature.append(T_ADDRESS);
      signature.append(T_INT);
      CallingConvention* cc = frame_map()->c_calling_convention(&signature);
      const LIR_Opr result_reg = result_register_for(x->type());

      LIR_Opr arg1 = cc->at(0),
              arg2 = cc->at(1),
              arg3 = cc->at(2);

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

  value.load_item();
  value1.load_item();
  value2.load_item();

  LIR_Opr calc_input = value.result();
  LIR_Opr calc_input1 = value1.result();
  LIR_Opr calc_input2 = value2.result();
  LIR_Opr calc_result = rlock_result(x);

  switch (x->id()) {
  case vmIntrinsics::_fmaD: __ fmad(calc_input, calc_input1, calc_input2, calc_result); break;
  case vmIntrinsics::_fmaF: __ fmaf(calc_input, calc_input1, calc_input2, calc_result); break;
  default:                  ShouldNotReachHere();
  }
}

void LIRGenerator::do_vectorizedMismatch(Intrinsic* x) {
  fatal("vectorizedMismatch intrinsic is not implemented on this platform");
}
