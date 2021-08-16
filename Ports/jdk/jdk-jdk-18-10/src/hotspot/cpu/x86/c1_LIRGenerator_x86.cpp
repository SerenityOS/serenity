/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/c1/barrierSetC1.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_x86.inline.hpp"

#ifdef ASSERT
#define __ gen()->lir(__FILE__, __LINE__)->
#else
#define __ gen()->lir()->
#endif

// Item will be loaded into a byte register; Intel only
void LIRItem::load_byte_item() {
  load_item();
  LIR_Opr res = result();

  if (!res->is_virtual() || !_gen->is_vreg_flag_set(res, LIRGenerator::byte_reg)) {
    // make sure that it is a byte register
    assert(!value()->type()->is_float() && !value()->type()->is_double(),
           "can't load floats in byte register");
    LIR_Opr reg = _gen->rlock_byte(T_BYTE);
    __ move(res, reg);

    _result = reg;
  }
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


LIR_Opr LIRGenerator::exceptionOopOpr() { return FrameMap::rax_oop_opr; }
LIR_Opr LIRGenerator::exceptionPcOpr()  { return FrameMap::rdx_opr; }
LIR_Opr LIRGenerator::divInOpr()        { return FrameMap::rax_opr; }
LIR_Opr LIRGenerator::divOutOpr()       { return FrameMap::rax_opr; }
LIR_Opr LIRGenerator::remOutOpr()       { return FrameMap::rdx_opr; }
LIR_Opr LIRGenerator::shiftCountOpr()   { return FrameMap::rcx_opr; }
LIR_Opr LIRGenerator::syncLockOpr()     { return new_register(T_INT); }
LIR_Opr LIRGenerator::syncTempOpr()     { return FrameMap::rax_opr; }
LIR_Opr LIRGenerator::getThreadTemp()   { return LIR_OprFact::illegalOpr; }


LIR_Opr LIRGenerator::result_register_for(ValueType* type, bool callee) {
  LIR_Opr opr;
  switch (type->tag()) {
    case intTag:     opr = FrameMap::rax_opr;          break;
    case objectTag:  opr = FrameMap::rax_oop_opr;      break;
    case longTag:    opr = FrameMap::long0_opr;        break;
#ifdef _LP64
    case floatTag:   opr = FrameMap::xmm0_float_opr;   break;
    case doubleTag:  opr = FrameMap::xmm0_double_opr;  break;
#else
    case floatTag:   opr = UseSSE >= 1 ? FrameMap::xmm0_float_opr  : FrameMap::fpu0_float_opr;  break;
    case doubleTag:  opr = UseSSE >= 2 ? FrameMap::xmm0_double_opr : FrameMap::fpu0_double_opr;  break;
#endif // _LP64
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


// i486 instructions can inline constants
bool LIRGenerator::can_store_as_constant(Value v, BasicType type) const {
  if (type == T_SHORT || type == T_CHAR) {
    // there is no immediate move of word values in asembler_i486.?pp
    return false;
  }
  Constant* c = v->as_Constant();
  if (c && c->state_before() == NULL) {
    // constants of any type can be stored directly, except for
    // unloaded object constants.
    return true;
  }
  return false;
}


bool LIRGenerator::can_inline_as_constant(Value v) const {
  if (v->type()->tag() == longTag) return false;
  return v->type()->tag() != objectTag ||
    (v->type()->is_constant() && v->type()->as_ObjectType()->constant_value()->is_null_object());
}


bool LIRGenerator::can_inline_as_constant(LIR_Const* c) const {
  if (c->type() == T_LONG) return false;
  return c->type() != T_OBJECT || c->as_jobject() == NULL;
}


LIR_Opr LIRGenerator::safepoint_poll_register() {
  NOT_LP64( return new_register(T_ADDRESS); )
  return LIR_OprFact::illegalOpr;
}


LIR_Address* LIRGenerator::generate_address(LIR_Opr base, LIR_Opr index,
                                            int shift, int disp, BasicType type) {
  assert(base->is_register(), "must be");
  if (index->is_constant()) {
    LIR_Const *constant = index->as_constant_ptr();
#ifdef _LP64
    jlong c;
    if (constant->type() == T_INT) {
      c = (jlong(index->as_jint()) << shift) + disp;
    } else {
      assert(constant->type() == T_LONG, "should be");
      c = (index->as_jlong() << shift) + disp;
    }
    if ((jlong)((jint)c) == c) {
      return new LIR_Address(base, (jint)c, type);
    } else {
      LIR_Opr tmp = new_register(T_LONG);
      __ move(index, tmp);
      return new LIR_Address(base, tmp, type);
    }
#else
    return new LIR_Address(base,
                           ((intx)(constant->as_jint()) << shift) + disp,
                           type);
#endif
  } else {
    return new LIR_Address(base, index, (LIR_Address::Scale)shift, disp, type);
  }
}


LIR_Address* LIRGenerator::emit_array_address(LIR_Opr array_opr, LIR_Opr index_opr,
                                              BasicType type) {
  int offset_in_bytes = arrayOopDesc::base_offset_in_bytes(type);

  LIR_Address* addr;
  if (index_opr->is_constant()) {
    int elem_size = type2aelembytes(type);
    addr = new LIR_Address(array_opr,
                           offset_in_bytes + (intx)(index_opr->as_jint()) * elem_size, type);
  } else {
#ifdef _LP64
    if (index_opr->type() == T_INT) {
      LIR_Opr tmp = new_register(T_LONG);
      __ convert(Bytecodes::_i2l, index_opr, tmp);
      index_opr = tmp;
    }
#endif // _LP64
    addr =  new LIR_Address(array_opr,
                            index_opr,
                            LIR_Address::scale(type),
                            offset_in_bytes, type);
  }
  return addr;
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
  __ cmp_mem_int(condition, base, disp, c, info);
}


void LIRGenerator::cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Opr base, int disp, BasicType type, CodeEmitInfo* info) {
  __ cmp_reg_mem(condition, reg, new LIR_Address(base, disp, type), info);
}


bool LIRGenerator::strength_reduce_multiply(LIR_Opr left, jint c, LIR_Opr result, LIR_Opr tmp) {
  if (tmp->is_valid() && c > 0 && c < max_jint) {
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
  __ store(item, new LIR_Address(FrameMap::rsp_opr, in_bytes(offset_from_sp), type));
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


// _ineg, _lneg, _fneg, _dneg
void LIRGenerator::do_NegateOp(NegateOp* x) {
  LIRItem value(x->x(), this);
  value.set_destroys_register();
  value.load_item();
  LIR_Opr reg = rlock(x);

  LIR_Opr tmp = LIR_OprFact::illegalOpr;
#ifdef _LP64
  if (UseAVX > 2 && !VM_Version::supports_avx512vl()) {
    if (x->type()->tag() == doubleTag) {
      tmp = new_register(T_DOUBLE);
      __ move(LIR_OprFact::doubleConst(-0.0), tmp);
    }
    else if (x->type()->tag() == floatTag) {
      tmp = new_register(T_FLOAT);
      __ move(LIR_OprFact::floatConst(-0.0), tmp);
    }
  }
#endif
  __ negate(value.result(), reg, tmp);

  set_result(x, round_item(reg));
}


// for  _fadd, _fmul, _fsub, _fdiv, _frem
//      _dadd, _dmul, _dsub, _ddiv, _drem
void LIRGenerator::do_ArithmeticOp_FPU(ArithmeticOp* x) {
  LIRItem left(x->x(),  this);
  LIRItem right(x->y(), this);
  LIRItem* left_arg  = &left;
  LIRItem* right_arg = &right;
  assert(!left.is_stack() || !right.is_stack(), "can't both be memory operands");
  bool must_load_both = (x->op() == Bytecodes::_frem || x->op() == Bytecodes::_drem);
  if (left.is_register() || x->x()->type()->is_constant() || must_load_both) {
    left.load_item();
  } else {
    left.dont_load_item();
  }

#ifndef _LP64
  // do not load right operand if it is a constant.  only 0 and 1 are
  // loaded because there are special instructions for loading them
  // without memory access (not needed for SSE2 instructions)
  bool must_load_right = false;
  if (right.is_constant()) {
    LIR_Const* c = right.result()->as_constant_ptr();
    assert(c != NULL, "invalid constant");
    assert(c->type() == T_FLOAT || c->type() == T_DOUBLE, "invalid type");

    if (c->type() == T_FLOAT) {
      must_load_right = UseSSE < 1 && (c->is_one_float() || c->is_zero_float());
    } else {
      must_load_right = UseSSE < 2 && (c->is_one_double() || c->is_zero_double());
    }
  }
#endif // !LP64

  if (must_load_both) {
    // frem and drem destroy also right operand, so move it to a new register
    right.set_destroys_register();
    right.load_item();
  } else if (right.is_register()) {
    right.load_item();
#ifndef _LP64
  } else if (must_load_right) {
    right.load_item();
#endif // !LP64
  } else {
    right.dont_load_item();
  }
  LIR_Opr reg = rlock(x);
  LIR_Opr tmp = LIR_OprFact::illegalOpr;
  if (x->op() == Bytecodes::_dmul || x->op() == Bytecodes::_ddiv) {
    tmp = new_register(T_DOUBLE);
  }

#ifdef _LP64
  if (x->op() == Bytecodes::_frem || x->op() == Bytecodes::_drem) {
    // frem and drem are implemented as a direct call into the runtime.
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);

    BasicType bt = as_BasicType(x->type());
    BasicTypeList signature(2);
    signature.append(bt);
    signature.append(bt);
    CallingConvention* cc = frame_map()->c_calling_convention(&signature);

    const LIR_Opr result_reg = result_register_for(x->type());
    left.load_item_force(cc->at(0));
    right.load_item_force(cc->at(1));

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

    LIR_Opr result = rlock_result(x);
    __ call_runtime_leaf(entry, getThreadTemp(), result_reg, cc->args());
    __ move(result_reg, result);
  } else {
    arithmetic_op_fpu(x->op(), reg, left.result(), right.result(), tmp);
    set_result(x, round_item(reg));
  }
#else
  if ((UseSSE >= 1 && x->op() == Bytecodes::_frem) || (UseSSE >= 2 && x->op() == Bytecodes::_drem)) {
    // special handling for frem and drem: no SSE instruction, so must use FPU with temporary fpu stack slots
    LIR_Opr fpu0, fpu1;
    if (x->op() == Bytecodes::_frem) {
      fpu0 = LIR_OprFact::single_fpu(0);
      fpu1 = LIR_OprFact::single_fpu(1);
    } else {
      fpu0 = LIR_OprFact::double_fpu(0);
      fpu1 = LIR_OprFact::double_fpu(1);
    }
    __ move(right.result(), fpu1); // order of left and right operand is important!
    __ move(left.result(), fpu0);
    __ rem (fpu0, fpu1, fpu0);
    __ move(fpu0, reg);

  } else {
    arithmetic_op_fpu(x->op(), reg, left.result(), right.result(), tmp);
  }
  set_result(x, round_item(reg));
#endif // _LP64
}


// for  _ladd, _lmul, _lsub, _ldiv, _lrem
void LIRGenerator::do_ArithmeticOp_Long(ArithmeticOp* x) {
  if (x->op() == Bytecodes::_ldiv || x->op() == Bytecodes::_lrem ) {
    // long division is implemented as a direct call into the runtime
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);

    // the check for division by zero destroys the right operand
    right.set_destroys_register();

    BasicTypeList signature(2);
    signature.append(T_LONG);
    signature.append(T_LONG);
    CallingConvention* cc = frame_map()->c_calling_convention(&signature);

    // check for division by zero (destroys registers of right operand!)
    CodeEmitInfo* info = state_for(x);

    const LIR_Opr result_reg = result_register_for(x->type());
    left.load_item_force(cc->at(1));
    right.load_item();

    __ move(right.result(), cc->at(0));

    __ cmp(lir_cond_equal, right.result(), LIR_OprFact::longConst(0));
    __ branch(lir_cond_equal, new DivByZeroStub(info));

    address entry = NULL;
    switch (x->op()) {
    case Bytecodes::_lrem:
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::lrem);
      break; // check if dividend is 0 is done elsewhere
    case Bytecodes::_ldiv:
      entry = CAST_FROM_FN_PTR(address, SharedRuntime::ldiv);
      break; // check if dividend is 0 is done elsewhere
    default:
      ShouldNotReachHere();
    }

    LIR_Opr result = rlock_result(x);
    __ call_runtime_leaf(entry, getThreadTemp(), result_reg, cc->args());
    __ move(result_reg, result);
  } else if (x->op() == Bytecodes::_lmul) {
    // missing test if instr is commutative and if we should swap
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);

    // right register is destroyed by the long mul, so it must be
    // copied to a new register.
    right.set_destroys_register();

    left.load_item();
    right.load_item();

    LIR_Opr reg = FrameMap::long0_opr;
    arithmetic_op_long(x->op(), reg, left.result(), right.result(), NULL);
    LIR_Opr result = rlock_result(x);
    __ move(reg, result);
  } else {
    // missing test if instr is commutative and if we should swap
    LIRItem left(x->x(), this);
    LIRItem right(x->y(), this);

    left.load_item();
    // don't load constants to save register
    right.load_nonconstant();
    rlock_result(x);
    arithmetic_op_long(x->op(), x->operand(), left.result(), right.result(), NULL);
  }
}



// for: _iadd, _imul, _isub, _idiv, _irem
void LIRGenerator::do_ArithmeticOp_Int(ArithmeticOp* x) {
  if (x->op() == Bytecodes::_idiv || x->op() == Bytecodes::_irem) {
    // The requirements for division and modulo
    // input : rax,: dividend                         min_int
    //         reg: divisor   (may not be rax,/rdx)   -1
    //
    // output: rax,: quotient  (= rax, idiv reg)       min_int
    //         rdx: remainder (= rax, irem reg)       0

    // rax, and rdx will be destroyed

    // Note: does this invalidate the spec ???
    LIRItem right(x->y(), this);
    LIRItem left(x->x() , this);   // visit left second, so that the is_register test is valid

    // call state_for before load_item_force because state_for may
    // force the evaluation of other instructions that are needed for
    // correct debug info.  Otherwise the live range of the fix
    // register might be too long.
    CodeEmitInfo* info = state_for(x);

    left.load_item_force(divInOpr());

    right.load_item();

    LIR_Opr result = rlock_result(x);
    LIR_Opr result_reg;
    if (x->op() == Bytecodes::_idiv) {
      result_reg = divOutOpr();
    } else {
      result_reg = remOutOpr();
    }

    if (!ImplicitDiv0Checks) {
      __ cmp(lir_cond_equal, right.result(), LIR_OprFact::intConst(0));
      __ branch(lir_cond_equal, new DivByZeroStub(info));
      // Idiv/irem cannot trap (passing info would generate an assertion).
      info = NULL;
    }
    LIR_Opr tmp = FrameMap::rdx_opr; // idiv and irem use rdx in their implementation
    if (x->op() == Bytecodes::_irem) {
      __ irem(left.result(), right.result(), result_reg, tmp, info);
    } else if (x->op() == Bytecodes::_idiv) {
      __ idiv(left.result(), right.result(), result_reg, tmp, info);
    } else {
      ShouldNotReachHere();
    }

    __ move(result_reg, result);
  } else {
    // missing test if instr is commutative and if we should swap
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
    if (x->op() == Bytecodes::_imul ) {
      // check if we can use shift instead
      bool use_constant = false;
      bool use_tmp = false;
      if (right_arg->is_constant()) {
        jint iconst = right_arg->get_jint_constant();
        if (iconst > 0 && iconst < max_jint) {
          if (is_power_of_2(iconst)) {
            use_constant = true;
          } else if (is_power_of_2(iconst - 1) || is_power_of_2(iconst + 1)) {
            use_constant = true;
            use_tmp = true;
          }
        }
      }
      if (use_constant) {
        right_arg->dont_load_item();
      } else {
        right_arg->load_item();
      }
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
  // count must always be in rcx
  LIRItem value(x->x(), this);
  LIRItem count(x->y(), this);

  ValueTag elemType = x->type()->tag();
  bool must_load_count = !count.is_constant() || elemType == longTag;
  if (must_load_count) {
    // count for long must be in register
    count.load_item_force(shiftCountOpr());
  } else {
    count.dont_load_item();
  }
  value.load_item();
  LIR_Opr reg = rlock_result(x);

  shift_op(x->op(), reg, value.result(), count.result(), LIR_OprFact::illegalOpr);
}


// _iand, _land, _ior, _lor, _ixor, _lxor
void LIRGenerator::do_LogicOp(LogicOp* x) {
  // when an operand with use count 1 is the left operand, then it is
  // likely that no move for 2-operand-LIR-form is necessary
  if (x->is_commutative() && x->y()->as_Constant() == NULL && x->x()->use_count() > x->y()->use_count()) {
    x->swap_operands();
  }

  LIRItem left(x->x(), this);
  LIRItem right(x->y(), this);

  left.load_item();
  right.load_nonconstant();
  LIR_Opr reg = rlock_result(x);

  logic_op(x->op(), reg, left.result(), right.result());
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
  if (is_reference_type(type)) {
    cmp_value.load_item_force(FrameMap::rax_oop_opr);
    new_value.load_item();
    __ cas_obj(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), ill, ill);
  } else if (type == T_INT) {
    cmp_value.load_item_force(FrameMap::rax_opr);
    new_value.load_item();
    __ cas_int(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), ill, ill);
  } else if (type == T_LONG) {
    cmp_value.load_item_force(FrameMap::long0_opr);
    new_value.load_item_force(FrameMap::long1_opr);
    __ cas_long(addr->as_address_ptr()->base(), cmp_value.result(), new_value.result(), ill, ill);
  } else {
    Unimplemented();
  }
  LIR_Opr result = new_register(T_INT);
  __ cmove(lir_cond_equal, LIR_OprFact::intConst(1), LIR_OprFact::intConst(0),
           result, T_INT);
  return result;
}

LIR_Opr LIRGenerator::atomic_xchg(BasicType type, LIR_Opr addr, LIRItem& value) {
  bool is_oop = is_reference_type(type);
  LIR_Opr result = new_register(type);
  value.load_item();
  // Because we want a 2-arg form of xchg and xadd
  __ move(value.result(), result);
  assert(type == T_INT || is_oop LP64_ONLY( || type == T_LONG ), "unexpected type");
  __ xchg(addr, result, result, LIR_OprFact::illegalOpr);
  return result;
}

LIR_Opr LIRGenerator::atomic_add(BasicType type, LIR_Opr addr, LIRItem& value) {
  LIR_Opr result = new_register(type);
  value.load_item();
  // Because we want a 2-arg form of xchg and xadd
  __ move(value.result(), result);
  assert(type == T_INT LP64_ONLY( || type == T_LONG ), "unexpected type");
  __ xadd(addr, result, result, LIR_OprFact::illegalOpr);
  return result;
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


void LIRGenerator::do_MathIntrinsic(Intrinsic* x) {
  assert(x->number_of_arguments() == 1 || (x->number_of_arguments() == 2 && x->id() == vmIntrinsics::_dpow), "wrong type");

  if (x->id() == vmIntrinsics::_dexp || x->id() == vmIntrinsics::_dlog ||
      x->id() == vmIntrinsics::_dpow || x->id() == vmIntrinsics::_dcos ||
      x->id() == vmIntrinsics::_dsin || x->id() == vmIntrinsics::_dtan ||
      x->id() == vmIntrinsics::_dlog10) {
    do_LibmIntrinsic(x);
    return;
  }

  LIRItem value(x->argument_at(0), this);

  bool use_fpu = false;
#ifndef _LP64
  if (UseSSE < 2) {
    value.set_destroys_register();
  }
#endif // !LP64
  value.load_item();

  LIR_Opr calc_input = value.result();
  LIR_Opr calc_result = rlock_result(x);

  LIR_Opr tmp = LIR_OprFact::illegalOpr;
#ifdef _LP64
  if (UseAVX > 2 && (!VM_Version::supports_avx512vl()) &&
      (x->id() == vmIntrinsics::_dabs)) {
    tmp = new_register(T_DOUBLE);
    __ move(LIR_OprFact::doubleConst(-0.0), tmp);
  }
#endif

  switch(x->id()) {
    case vmIntrinsics::_dabs:   __ abs  (calc_input, calc_result, tmp); break;
    case vmIntrinsics::_dsqrt:  __ sqrt (calc_input, calc_result, LIR_OprFact::illegalOpr); break;
    default:                    ShouldNotReachHere();
  }

  if (use_fpu) {
    __ move(calc_result, x->operand());
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

#ifndef _LP64
  LIR_Opr tmp = FrameMap::fpu0_double_opr;
  result_reg = tmp;
  switch(x->id()) {
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
      if (VM_Version::supports_sse2() && StubRoutines::dsin() != NULL) {
        __ call_runtime_leaf(StubRoutines::dsin(), getThreadTemp(), result_reg, cc->args());
      } else {
        __ call_runtime_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dsin), getThreadTemp(), result_reg, cc->args());
      }
      break;
    case vmIntrinsics::_dcos:
      if (VM_Version::supports_sse2() && StubRoutines::dcos() != NULL) {
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
#else
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
#endif // _LP64
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

#ifndef _LP64
  src.load_item_force     (FrameMap::rcx_oop_opr);
  src_pos.load_item_force (FrameMap::rdx_opr);
  dst.load_item_force     (FrameMap::rax_oop_opr);
  dst_pos.load_item_force (FrameMap::rbx_opr);
  length.load_item_force  (FrameMap::rdi_opr);
  LIR_Opr tmp =           (FrameMap::rsi_opr);
#else

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
#endif // LP64

  set_no_result(x);

  int flags;
  ciArrayKlass* expected_type;
  arraycopy_helper(x, &flags, &expected_type);

  __ arraycopy(src.result(), src_pos.result(), dst.result(), dst_pos.result(), length.result(), tmp, expected_type, flags, info); // does add_safepoint
}

void LIRGenerator::do_update_CRC32(Intrinsic* x) {
  assert(UseCRC32Intrinsics, "need AVX and LCMUL instructions support");
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

#ifndef _LP64
      if (!is_updateBytes) { // long b raw address
         base_op = new_register(T_INT);
         __ convert(Bytecodes::_l2i, buf.result(), base_op);
      }
#else
      if (index->is_valid()) {
        LIR_Opr tmp = new_register(T_LONG);
        __ convert(Bytecodes::_i2l, index, tmp);
        index = tmp;
      }
#endif

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
  Unimplemented();
}

void LIRGenerator::do_vectorizedMismatch(Intrinsic* x) {
  assert(UseVectorizedMismatchIntrinsic, "need AVX instruction support");

  // Make all state_for calls early since they can emit code
  LIR_Opr result = rlock_result(x);

  LIRItem a(x->argument_at(0), this); // Object
  LIRItem aOffset(x->argument_at(1), this); // long
  LIRItem b(x->argument_at(2), this); // Object
  LIRItem bOffset(x->argument_at(3), this); // long
  LIRItem length(x->argument_at(4), this); // int
  LIRItem log2ArrayIndexScale(x->argument_at(5), this); // int

  a.load_item();
  aOffset.load_nonconstant();
  b.load_item();
  bOffset.load_nonconstant();

  long constant_aOffset = 0;
  LIR_Opr result_aOffset = aOffset.result();
  if (result_aOffset->is_constant()) {
    constant_aOffset = result_aOffset->as_jlong();
    result_aOffset = LIR_OprFact::illegalOpr;
  }
  LIR_Opr result_a = a.result();

  long constant_bOffset = 0;
  LIR_Opr result_bOffset = bOffset.result();
  if (result_bOffset->is_constant()) {
    constant_bOffset = result_bOffset->as_jlong();
    result_bOffset = LIR_OprFact::illegalOpr;
  }
  LIR_Opr result_b = b.result();

#ifndef _LP64
  result_a = new_register(T_INT);
  __ convert(Bytecodes::_l2i, a.result(), result_a);
  result_b = new_register(T_INT);
  __ convert(Bytecodes::_l2i, b.result(), result_b);
#endif


  LIR_Address* addr_a = new LIR_Address(result_a,
                                        result_aOffset,
                                        constant_aOffset,
                                        T_BYTE);

  LIR_Address* addr_b = new LIR_Address(result_b,
                                        result_bOffset,
                                        constant_bOffset,
                                        T_BYTE);

  BasicTypeList signature(4);
  signature.append(T_ADDRESS);
  signature.append(T_ADDRESS);
  signature.append(T_INT);
  signature.append(T_INT);
  CallingConvention* cc = frame_map()->c_calling_convention(&signature);
  const LIR_Opr result_reg = result_register_for(x->type());

  LIR_Opr ptr_addr_a = new_pointer_register();
  __ leal(LIR_OprFact::address(addr_a), ptr_addr_a);

  LIR_Opr ptr_addr_b = new_pointer_register();
  __ leal(LIR_OprFact::address(addr_b), ptr_addr_b);

  __ move(ptr_addr_a, cc->at(0));
  __ move(ptr_addr_b, cc->at(1));
  length.load_item_force(cc->at(2));
  log2ArrayIndexScale.load_item_force(cc->at(3));

  __ call_runtime_leaf(StubRoutines::vectorizedMismatch(), getThreadTemp(), result_reg, cc->args());
  __ move(result_reg, result);
}

// _i2l, _i2f, _i2d, _l2i, _l2f, _l2d, _f2i, _f2l, _f2d, _d2i, _d2l, _d2f
// _i2b, _i2c, _i2s
LIR_Opr fixed_register_for(BasicType type) {
  switch (type) {
    case T_FLOAT:  return FrameMap::fpu0_float_opr;
    case T_DOUBLE: return FrameMap::fpu0_double_opr;
    case T_INT:    return FrameMap::rax_opr;
    case T_LONG:   return FrameMap::long0_opr;
    default:       ShouldNotReachHere(); return LIR_OprFact::illegalOpr;
  }
}

void LIRGenerator::do_Convert(Convert* x) {
#ifdef _LP64
  LIRItem value(x->value(), this);
  value.load_item();
  LIR_Opr input = value.result();
  LIR_Opr result = rlock(x);
  __ convert(x->op(), input, result);
  assert(result->is_virtual(), "result must be virtual register");
  set_result(x, result);
#else
  // flags that vary for the different operations and different SSE-settings
  bool fixed_input = false, fixed_result = false, round_result = false, needs_stub = false;

  switch (x->op()) {
    case Bytecodes::_i2l: // fall through
    case Bytecodes::_l2i: // fall through
    case Bytecodes::_i2b: // fall through
    case Bytecodes::_i2c: // fall through
    case Bytecodes::_i2s: fixed_input = false;       fixed_result = false;       round_result = false;      needs_stub = false; break;

    case Bytecodes::_f2d: fixed_input = UseSSE == 1; fixed_result = false;       round_result = false;      needs_stub = false; break;
    case Bytecodes::_d2f: fixed_input = false;       fixed_result = UseSSE == 1; round_result = UseSSE < 1; needs_stub = false; break;
    case Bytecodes::_i2f: fixed_input = false;       fixed_result = false;       round_result = UseSSE < 1; needs_stub = false; break;
    case Bytecodes::_i2d: fixed_input = false;       fixed_result = false;       round_result = false;      needs_stub = false; break;
    case Bytecodes::_f2i: fixed_input = false;       fixed_result = false;       round_result = false;      needs_stub = true;  break;
    case Bytecodes::_d2i: fixed_input = false;       fixed_result = false;       round_result = false;      needs_stub = true;  break;
    case Bytecodes::_l2f: fixed_input = false;       fixed_result = UseSSE >= 1; round_result = UseSSE < 1; needs_stub = false; break;
    case Bytecodes::_l2d: fixed_input = false;       fixed_result = UseSSE >= 2; round_result = UseSSE < 2; needs_stub = false; break;
    case Bytecodes::_f2l: fixed_input = true;        fixed_result = true;        round_result = false;      needs_stub = false; break;
    case Bytecodes::_d2l: fixed_input = true;        fixed_result = true;        round_result = false;      needs_stub = false; break;
    default: ShouldNotReachHere();
  }

  LIRItem value(x->value(), this);
  value.load_item();
  LIR_Opr input = value.result();
  LIR_Opr result = rlock(x);

  // arguments of lir_convert
  LIR_Opr conv_input = input;
  LIR_Opr conv_result = result;
  ConversionStub* stub = NULL;

  if (fixed_input) {
    conv_input = fixed_register_for(input->type());
    __ move(input, conv_input);
  }

  assert(fixed_result == false || round_result == false, "cannot set both");
  if (fixed_result) {
    conv_result = fixed_register_for(result->type());
  } else if (round_result) {
    result = new_register(result->type());
    set_vreg_flag(result, must_start_in_memory);
  }

  if (needs_stub) {
    stub = new ConversionStub(x->op(), conv_input, conv_result);
  }

  __ convert(x->op(), conv_input, conv_result, stub);

  if (result != conv_result) {
    __ move(conv_result, result);
  }

  assert(result->is_virtual(), "result must be virtual register");
  set_result(x, result);
#endif // _LP64
}


void LIRGenerator::do_NewInstance(NewInstance* x) {
  print_if_not_loaded(x);

  CodeEmitInfo* info = state_for(x, x->state());
  LIR_Opr reg = result_register_for(x->type());
  new_instance(reg, x->klass(), x->is_unresolved(),
                       FrameMap::rcx_oop_opr,
                       FrameMap::rdi_oop_opr,
                       FrameMap::rsi_oop_opr,
                       LIR_OprFact::illegalOpr,
                       FrameMap::rdx_metadata_opr, info);
  LIR_Opr result = rlock_result(x);
  __ move(reg, result);
}


void LIRGenerator::do_NewTypeArray(NewTypeArray* x) {
  CodeEmitInfo* info = state_for(x, x->state());

  LIRItem length(x->length(), this);
  length.load_item_force(FrameMap::rbx_opr);

  LIR_Opr reg = result_register_for(x->type());
  LIR_Opr tmp1 = FrameMap::rcx_oop_opr;
  LIR_Opr tmp2 = FrameMap::rsi_oop_opr;
  LIR_Opr tmp3 = FrameMap::rdi_oop_opr;
  LIR_Opr tmp4 = reg;
  LIR_Opr klass_reg = FrameMap::rdx_metadata_opr;
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

  const LIR_Opr reg = result_register_for(x->type());
  LIR_Opr tmp1 = FrameMap::rcx_oop_opr;
  LIR_Opr tmp2 = FrameMap::rsi_oop_opr;
  LIR_Opr tmp3 = FrameMap::rdi_oop_opr;
  LIR_Opr tmp4 = reg;
  LIR_Opr klass_reg = FrameMap::rdx_metadata_opr;

  length.load_item_force(FrameMap::rbx_opr);
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
    size->load_nonconstant();

    store_stack_parameter(size->result(), in_ByteSize(i*4));
  }

  LIR_Opr klass_reg = FrameMap::rax_metadata_opr;
  klass2reg_with_patching(klass_reg, x->klass(), patching_info);

  LIR_Opr rank = FrameMap::rbx_opr;
  __ move(LIR_OprFact::intConst(x->rank()), rank);
  LIR_Opr varargs = FrameMap::rcx_opr;
  __ move(FrameMap::rsp_opr, varargs);
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
    stub = new DeoptimizeStub(info_for_exception, Deoptimization::Reason_class_check, Deoptimization::Action_none);
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
  if (tag == longTag && yin->is_constant() && yin->get_jlong_constant() == 0 && (cond == If::eql || cond == If::neq)) {
    // inline long zero
    yin->dont_load_item();
  } else if (tag == longTag || tag == floatTag || tag == doubleTag) {
    // longs cannot handle constants at right side
    yin->load_item();
  } else {
    yin->dont_load_item();
  }

  LIR_Opr left = xin->result();
  LIR_Opr right = yin->result();

  set_no_result(x);

  // add safepoint before generating condition code so it can be recomputed
  if (x->is_safepoint()) {
    // increment backedge counter if needed
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
#ifdef _LP64
  return FrameMap::as_pointer_opr(r15_thread);
#else
  LIR_Opr result = new_register(T_INT);
  __ get_thread(result);
  return result;
#endif //
}

void LIRGenerator::trace_block_entry(BlockBegin* block) {
  store_stack_parameter(LIR_OprFact::intConst(block->block_id()), in_ByteSize(0));
  LIR_OprList* args = new LIR_OprList();
  address func = CAST_FROM_FN_PTR(address, Runtime1::trace_block_entry);
  __ call_runtime_leaf(func, LIR_OprFact::illegalOpr, LIR_OprFact::illegalOpr, args);
}


void LIRGenerator::volatile_field_store(LIR_Opr value, LIR_Address* address,
                                        CodeEmitInfo* info) {
  if (address->type() == T_LONG) {
    address = new LIR_Address(address->base(),
                              address->index(), address->scale(),
                              address->disp(), T_DOUBLE);
    // Transfer the value atomically by using FP moves.  This means
    // the value has to be moved between CPU and FPU registers.  It
    // always has to be moved through spill slot since there's no
    // quick way to pack the value into an SSE register.
    LIR_Opr temp_double = new_register(T_DOUBLE);
    LIR_Opr spill = new_register(T_LONG);
    set_vreg_flag(spill, must_start_in_memory);
    __ move(value, spill);
    __ volatile_move(spill, temp_double, T_LONG);
    __ volatile_move(temp_double, LIR_OprFact::address(address), T_LONG, info);
  } else {
    __ store(value, address, info);
  }
}

void LIRGenerator::volatile_field_load(LIR_Address* address, LIR_Opr result,
                                       CodeEmitInfo* info) {
  if (address->type() == T_LONG) {
    address = new LIR_Address(address->base(),
                              address->index(), address->scale(),
                              address->disp(), T_DOUBLE);
    // Transfer the value atomically by using FP moves.  This means
    // the value has to be moved between CPU and FPU registers.  In
    // SSE0 and SSE1 mode it has to be moved through spill slot but in
    // SSE2+ mode it can be moved directly.
    LIR_Opr temp_double = new_register(T_DOUBLE);
    __ volatile_move(LIR_OprFact::address(address), temp_double, T_LONG, info);
    __ volatile_move(temp_double, result, T_LONG);
#ifndef _LP64
    if (UseSSE < 2) {
      // no spill slot needed in SSE2 mode because xmm->cpu register move is possible
      set_vreg_flag(result, must_start_in_memory);
    }
#endif // !LP64
  } else {
    __ load(address, result, info);
  }
}
