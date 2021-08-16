/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_Canonicalizer.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArray.hpp"
#include "runtime/sharedRuntime.hpp"


class PrintValueVisitor: public ValueVisitor {
  void visit(Value* vp) {
    (*vp)->print_line();
  }
};

void Canonicalizer::set_canonical(Value x) {
  assert(x != NULL, "value must exist");
  // Note: we can not currently substitute root nodes which show up in
  // the instruction stream (because the instruction list is embedded
  // in the instructions).
  if (canonical() != x) {
#ifndef PRODUCT
    if (!x->has_printable_bci()) {
      x->set_printable_bci(bci());
    }
#endif
    if (PrintCanonicalization) {
      PrintValueVisitor do_print_value;
      canonical()->input_values_do(&do_print_value);
      canonical()->print_line();
      tty->print_cr("canonicalized to:");
      x->input_values_do(&do_print_value);
      x->print_line();
      tty->cr();
    }
    assert(_canonical->type()->tag() == x->type()->tag(), "types must match");
    _canonical = x;
  }
}


void Canonicalizer::move_const_to_right(Op2* x) {
  if (x->x()->type()->is_constant() && x->is_commutative()) x->swap_operands();
}


void Canonicalizer::do_Op2(Op2* x) {
  if (x->x() == x->y()) {
    switch (x->op()) {
    case Bytecodes::_isub: set_constant(0); return;
    case Bytecodes::_lsub: set_constant(jlong_cast(0)); return;
    case Bytecodes::_iand: // fall through
    case Bytecodes::_land: // fall through
    case Bytecodes::_ior : // fall through
    case Bytecodes::_lor : set_canonical(x->x()); return;
    case Bytecodes::_ixor: set_constant(0); return;
    case Bytecodes::_lxor: set_constant(jlong_cast(0)); return;
    default              : break;
    }
  }

  if (x->x()->type()->is_constant() && x->y()->type()->is_constant()) {
    // do constant folding for selected operations
    switch (x->type()->tag()) {
      case intTag:
        { jint a = x->x()->type()->as_IntConstant()->value();
          jint b = x->y()->type()->as_IntConstant()->value();
          switch (x->op()) {
            case Bytecodes::_iadd: set_constant(a + b); return;
            case Bytecodes::_isub: set_constant(a - b); return;
            case Bytecodes::_imul: set_constant(a * b); return;
            case Bytecodes::_idiv:
              if (b != 0) {
                if (a == min_jint && b == -1) {
                  set_constant(min_jint);
                } else {
                  set_constant(a / b);
                }
                return;
              }
              break;
            case Bytecodes::_irem:
              if (b != 0) {
                if (a == min_jint && b == -1) {
                  set_constant(0);
                } else {
                  set_constant(a % b);
                }
                return;
              }
              break;
            case Bytecodes::_iand: set_constant(a & b); return;
            case Bytecodes::_ior : set_constant(a | b); return;
            case Bytecodes::_ixor: set_constant(a ^ b); return;
            default              : break;
          }
        }
        break;
      case longTag:
        { jlong a = x->x()->type()->as_LongConstant()->value();
          jlong b = x->y()->type()->as_LongConstant()->value();
          switch (x->op()) {
            case Bytecodes::_ladd: set_constant(a + b); return;
            case Bytecodes::_lsub: set_constant(a - b); return;
            case Bytecodes::_lmul: set_constant(a * b); return;
            case Bytecodes::_ldiv:
              if (b != 0) {
                set_constant(SharedRuntime::ldiv(b, a));
                return;
              }
              break;
            case Bytecodes::_lrem:
              if (b != 0) {
                set_constant(SharedRuntime::lrem(b, a));
                return;
              }
              break;
            case Bytecodes::_land: set_constant(a & b); return;
            case Bytecodes::_lor : set_constant(a | b); return;
            case Bytecodes::_lxor: set_constant(a ^ b); return;
            default              : break;
          }
        }
        break;
      default:
        // other cases not implemented (must be extremely careful with floats & doubles!)
        break;
    }
  }
  // make sure constant is on the right side, if any
  move_const_to_right(x);

  if (x->y()->type()->is_constant()) {
    // do constant folding for selected operations
    switch (x->type()->tag()) {
      case intTag:
        if (x->y()->type()->as_IntConstant()->value() == 0) {
          switch (x->op()) {
            case Bytecodes::_iadd: set_canonical(x->x()); return;
            case Bytecodes::_isub: set_canonical(x->x()); return;
            case Bytecodes::_imul: set_constant(0); return;
              // Note: for div and rem, make sure that C semantics
              //       corresponds to Java semantics!
            case Bytecodes::_iand: set_constant(0); return;
            case Bytecodes::_ior : set_canonical(x->x()); return;
            default              : break;
          }
        }
        break;
      case longTag:
        if (x->y()->type()->as_LongConstant()->value() == (jlong)0) {
          switch (x->op()) {
            case Bytecodes::_ladd: set_canonical(x->x()); return;
            case Bytecodes::_lsub: set_canonical(x->x()); return;
            case Bytecodes::_lmul: set_constant((jlong)0); return;
              // Note: for div and rem, make sure that C semantics
              //       corresponds to Java semantics!
            case Bytecodes::_land: set_constant((jlong)0); return;
            case Bytecodes::_lor : set_canonical(x->x()); return;
            default              : break;
          }
        }
        break;
      default:
        break;
    }
  }
}


void Canonicalizer::do_Phi            (Phi*             x) {}
void Canonicalizer::do_Constant       (Constant*        x) {}
void Canonicalizer::do_Local          (Local*           x) {}
void Canonicalizer::do_LoadField      (LoadField*       x) {}

// checks if v is in the block that is currently processed by
// GraphBuilder. This is the only block that has not BlockEnd yet.
static bool in_current_block(Value v) {
  int max_distance = 4;
  while (max_distance > 0 && v != NULL && v->as_BlockEnd() == NULL) {
    v = v->next();
    max_distance--;
  }
  return v == NULL;
}

void Canonicalizer::do_StoreField     (StoreField*      x) {
  // If a value is going to be stored into a field or array some of
  // the conversions emitted by javac are unneeded because the fields
  // are packed to their natural size.
  Convert* conv = x->value()->as_Convert();
  if (conv) {
    Value value = NULL;
    BasicType type = x->field()->type()->basic_type();
    switch (conv->op()) {
    case Bytecodes::_i2b: if (type == T_BYTE)  value = conv->value(); break;
    case Bytecodes::_i2s: if (type == T_SHORT || type == T_BYTE) value = conv->value(); break;
    case Bytecodes::_i2c: if (type == T_CHAR  || type == T_BYTE)  value = conv->value(); break;
    default             : break;
    }
    // limit this optimization to current block
    if (value != NULL && in_current_block(conv)) {
      set_canonical(new StoreField(x->obj(), x->offset(), x->field(), value, x->is_static(),
                                   x->state_before(), x->needs_patching()));
      return;
    }
  }

}

void Canonicalizer::do_ArrayLength    (ArrayLength*     x) {
  NewArray*  na;
  Constant*  ct;
  LoadField* lf;

  if ((na = x->array()->as_NewArray()) != NULL) {
    // New arrays might have the known length.
    // Do not use the Constant itself, but create a new Constant
    // with same value Otherwise a Constant is live over multiple
    // blocks without being registered in a state array.
    Constant* length;
    NewMultiArray* nma;
    if (na->length() != NULL &&
        (length = na->length()->as_Constant()) != NULL) {
      assert(length->type()->as_IntConstant() != NULL, "array length must be integer");
      set_constant(length->type()->as_IntConstant()->value());
    } else if ((nma = x->array()->as_NewMultiArray()) != NULL &&
               (length = nma->dims()->at(0)->as_Constant()) != NULL) {
      assert(length->type()->as_IntConstant() != NULL, "array length must be integer");
      set_constant(length->type()->as_IntConstant()->value());
    }

  } else if ((ct = x->array()->as_Constant()) != NULL) {
    // Constant arrays have constant lengths.
    ArrayConstant* cnst = ct->type()->as_ArrayConstant();
    if (cnst != NULL) {
      set_constant(cnst->value()->length());
    }

  } else if ((lf = x->array()->as_LoadField()) != NULL) {
    ciField* field = lf->field();
    if (field->is_static_constant()) {
      // Constant field loads are usually folded during parsing.
      // But it doesn't happen with PatchALot, ScavengeRootsInCode < 2, or when
      // holder class is being initialized during parsing (for static fields).
      ciObject* c = field->constant_value().as_object();
      if (!c->is_null_object()) {
        set_constant(c->as_array()->length());
      }
    }
  }
}

void Canonicalizer::do_LoadIndexed    (LoadIndexed*     x) {
  StableArrayConstant* array = x->array()->type()->as_StableArrayConstant();
  IntConstant* index = x->index()->type()->as_IntConstant();

  assert(array == NULL || FoldStableValues, "not enabled");

  // Constant fold loads from stable arrays.
  if (!x->mismatched() && array != NULL && index != NULL) {
    jint idx = index->value();
    if (idx < 0 || idx >= array->value()->length()) {
      // Leave the load as is. The range check will handle it.
      return;
    }

    ciConstant field_val = array->value()->element_value(idx);
    if (!field_val.is_null_or_zero()) {
      jint dimension = array->dimension();
      assert(dimension <= array->value()->array_type()->dimension(), "inconsistent info");
      ValueType* value = NULL;
      if (dimension > 1) {
        // Preserve information about the dimension for the element.
        assert(field_val.as_object()->is_array(), "not an array");
        value = new StableArrayConstant(field_val.as_object()->as_array(), dimension - 1);
      } else {
        assert(dimension == 1, "sanity");
        value = as_ValueType(field_val);
      }
      set_canonical(new Constant(value));
    }
  }
}

void Canonicalizer::do_StoreIndexed   (StoreIndexed*    x) {
  // If a value is going to be stored into a field or array some of
  // the conversions emitted by javac are unneeded because the fields
  // are packed to their natural size.
  Convert* conv = x->value()->as_Convert();
  if (conv) {
    Value value = NULL;
    BasicType type = x->elt_type();
    switch (conv->op()) {
    case Bytecodes::_i2b: if (type == T_BYTE)  value = conv->value(); break;
    case Bytecodes::_i2s: if (type == T_SHORT || type == T_BYTE) value = conv->value(); break;
    case Bytecodes::_i2c: if (type == T_CHAR  || type == T_BYTE) value = conv->value(); break;
    default             : break;
    }
    // limit this optimization to current block
    if (value != NULL && in_current_block(conv)) {
      set_canonical(new StoreIndexed(x->array(), x->index(), x->length(),
                                     x->elt_type(), value, x->state_before(),
                                     x->check_boolean()));
      return;
    }
  }
}


void Canonicalizer::do_NegateOp(NegateOp* x) {
  ValueType* t = x->x()->type();
  if (t->is_constant()) {
    switch (t->tag()) {
      case intTag   : set_constant(-t->as_IntConstant   ()->value()); return;
      case longTag  : set_constant(-t->as_LongConstant  ()->value()); return;
      case floatTag : set_constant(-t->as_FloatConstant ()->value()); return;
      case doubleTag: set_constant(-t->as_DoubleConstant()->value()); return;
      default       : ShouldNotReachHere();
    }
  }
}


void Canonicalizer::do_ArithmeticOp   (ArithmeticOp*    x) { do_Op2(x); }


void Canonicalizer::do_ShiftOp        (ShiftOp*         x) {
  ValueType* t = x->x()->type();
  ValueType* t2 = x->y()->type();
  if (t->is_constant()) {
    switch (t->tag()) {
    case intTag   : if (t->as_IntConstant()->value() == 0)         { set_constant(0); return; } break;
    case longTag  : if (t->as_LongConstant()->value() == (jlong)0) { set_constant(jlong_cast(0)); return; } break;
    default       : ShouldNotReachHere();
    }
    if (t2->is_constant()) {
      if (t->tag() == intTag) {
        jint value = t->as_IntConstant()->value();
        jint shift = t2->as_IntConstant()->value();
        switch (x->op()) {
          case Bytecodes::_ishl:  set_constant(java_shift_left(value, shift)); return;
          case Bytecodes::_ishr:  set_constant(java_shift_right(value, shift)); return;
          case Bytecodes::_iushr: set_constant(java_shift_right_unsigned(value, shift)); return;
          default:                break;
        }
      } else if (t->tag() == longTag) {
        jlong value = t->as_LongConstant()->value();
        jint shift = t2->as_IntConstant()->value();
        switch (x->op()) {
          case Bytecodes::_lshl:  set_constant(java_shift_left(value, shift)); return;
          case Bytecodes::_lshr:  set_constant(java_shift_right(value, shift)); return;
          case Bytecodes::_lushr: set_constant(java_shift_right_unsigned(value, shift)); return;
          default:                break;
        }
      }
    }
  }
  if (t2->is_constant()) {
    switch (t2->tag()) {
      case intTag   : if (t2->as_IntConstant()->value() == 0)  set_canonical(x->x()); return;
      case longTag  : if (t2->as_LongConstant()->value() == (jlong)0)  set_canonical(x->x()); return;
      default       : ShouldNotReachHere(); return;
    }
  }
}


void Canonicalizer::do_LogicOp        (LogicOp*         x) { do_Op2(x); }
void Canonicalizer::do_CompareOp      (CompareOp*       x) {
  if (x->x() == x->y()) {
    switch (x->x()->type()->tag()) {
      case longTag: set_constant(0); break;
      case floatTag: {
        FloatConstant* fc = x->x()->type()->as_FloatConstant();
        if (fc) {
          if (g_isnan(fc->value())) {
            set_constant(x->op() == Bytecodes::_fcmpl ? -1 : 1);
          } else {
            set_constant(0);
          }
        }
        break;
      }
      case doubleTag: {
        DoubleConstant* dc = x->x()->type()->as_DoubleConstant();
        if (dc) {
          if (g_isnan(dc->value())) {
            set_constant(x->op() == Bytecodes::_dcmpl ? -1 : 1);
          } else {
            set_constant(0);
          }
        }
        break;
      }
      default:
        break;
    }
  } else if (x->x()->type()->is_constant() && x->y()->type()->is_constant()) {
    switch (x->x()->type()->tag()) {
      case longTag: {
        jlong vx = x->x()->type()->as_LongConstant()->value();
        jlong vy = x->y()->type()->as_LongConstant()->value();
        if (vx == vy)
          set_constant(0);
        else if (vx < vy)
          set_constant(-1);
        else
          set_constant(1);
        break;
      }

      case floatTag: {
        float vx = x->x()->type()->as_FloatConstant()->value();
        float vy = x->y()->type()->as_FloatConstant()->value();
        if (g_isnan(vx) || g_isnan(vy))
          set_constant(x->op() == Bytecodes::_fcmpl ? -1 : 1);
        else if (vx == vy)
          set_constant(0);
        else if (vx < vy)
          set_constant(-1);
        else
          set_constant(1);
        break;
      }

      case doubleTag: {
        double vx = x->x()->type()->as_DoubleConstant()->value();
        double vy = x->y()->type()->as_DoubleConstant()->value();
        if (g_isnan(vx) || g_isnan(vy))
          set_constant(x->op() == Bytecodes::_dcmpl ? -1 : 1);
        else if (vx == vy)
          set_constant(0);
        else if (vx < vy)
          set_constant(-1);
        else
          set_constant(1);
        break;
      }

      default:
        break;
    }
  }
}


void Canonicalizer::do_IfOp(IfOp* x) {
  // Caution: do not use do_Op2(x) here for now since
  //          we map the condition to the op for now!
  move_const_to_right(x);
}


void Canonicalizer::do_Intrinsic      (Intrinsic*       x) {
  switch (x->id()) {
  case vmIntrinsics::_floatToRawIntBits   : {
    FloatConstant* c = x->argument_at(0)->type()->as_FloatConstant();
    if (c != NULL) {
      JavaValue v;
      v.set_jfloat(c->value());
      set_constant(v.get_jint());
    }
    break;
  }
  case vmIntrinsics::_intBitsToFloat      : {
    IntConstant* c = x->argument_at(0)->type()->as_IntConstant();
    if (c != NULL) {
      JavaValue v;
      v.set_jint(c->value());
      set_constant(v.get_jfloat());
    }
    break;
  }
  case vmIntrinsics::_doubleToRawLongBits : {
    DoubleConstant* c = x->argument_at(0)->type()->as_DoubleConstant();
    if (c != NULL) {
      JavaValue v;
      v.set_jdouble(c->value());
      set_constant(v.get_jlong());
    }
    break;
  }
  case vmIntrinsics::_longBitsToDouble    : {
    LongConstant* c = x->argument_at(0)->type()->as_LongConstant();
    if (c != NULL) {
      JavaValue v;
      v.set_jlong(c->value());
      set_constant(v.get_jdouble());
    }
    break;
  }
  case vmIntrinsics::_isInstance          : {
    assert(x->number_of_arguments() == 2, "wrong type");

    InstanceConstant* c = x->argument_at(0)->type()->as_InstanceConstant();
    if (c != NULL && !c->value()->is_null_object()) {
      // ciInstance::java_mirror_type() returns non-NULL only for Java mirrors
      ciType* t = c->value()->java_mirror_type();
      if (t->is_klass()) {
        // substitute cls.isInstance(obj) of a constant Class into
        // an InstantOf instruction
        InstanceOf* i = new InstanceOf(t->as_klass(), x->argument_at(1), x->state_before());
        set_canonical(i);
        // and try to canonicalize even further
        do_InstanceOf(i);
      } else {
        assert(t->is_primitive_type(), "should be a primitive type");
        // cls.isInstance(obj) always returns false for primitive classes
        set_constant(0);
      }
    }
    break;
  }
  case vmIntrinsics::_isPrimitive        : {
    assert(x->number_of_arguments() == 1, "wrong type");

    // Class.isPrimitive is known on constant classes:
    InstanceConstant* c = x->argument_at(0)->type()->as_InstanceConstant();
    if (c != NULL && !c->value()->is_null_object()) {
      ciType* t = c->value()->java_mirror_type();
      set_constant(t->is_primitive_type());
    }
    break;
  }
  case vmIntrinsics::_getModifiers: {
    assert(x->number_of_arguments() == 1, "wrong type");

    // Optimize for Foo.class.getModifier()
    InstanceConstant* c = x->argument_at(0)->type()->as_InstanceConstant();
    if (c != NULL && !c->value()->is_null_object()) {
      ciType* t = c->value()->java_mirror_type();
      if (t->is_klass()) {
        set_constant(t->as_klass()->modifier_flags());
      } else {
        assert(t->is_primitive_type(), "should be a primitive type");
        set_constant(JVM_ACC_ABSTRACT | JVM_ACC_FINAL | JVM_ACC_PUBLIC);
      }
    }
    break;
  }
  default:
    break;
  }
}

void Canonicalizer::do_Convert        (Convert*         x) {
  if (x->value()->type()->is_constant()) {
    switch (x->op()) {
    case Bytecodes::_i2b:  set_constant((int)((x->value()->type()->as_IntConstant()->value() << 24) >> 24)); break;
    case Bytecodes::_i2s:  set_constant((int)((x->value()->type()->as_IntConstant()->value() << 16) >> 16)); break;
    case Bytecodes::_i2c:  set_constant((int)(x->value()->type()->as_IntConstant()->value() & ((1<<16)-1))); break;
    case Bytecodes::_i2l:  set_constant((jlong)(x->value()->type()->as_IntConstant()->value()));             break;
    case Bytecodes::_i2f:  set_constant((float)(x->value()->type()->as_IntConstant()->value()));             break;
    case Bytecodes::_i2d:  set_constant((double)(x->value()->type()->as_IntConstant()->value()));            break;
    case Bytecodes::_l2i:  set_constant((int)(x->value()->type()->as_LongConstant()->value()));              break;
    case Bytecodes::_l2f:  set_constant(SharedRuntime::l2f(x->value()->type()->as_LongConstant()->value())); break;
    case Bytecodes::_l2d:  set_constant(SharedRuntime::l2d(x->value()->type()->as_LongConstant()->value())); break;
    case Bytecodes::_f2d:  set_constant((double)(x->value()->type()->as_FloatConstant()->value()));          break;
    case Bytecodes::_f2i:  set_constant(SharedRuntime::f2i(x->value()->type()->as_FloatConstant()->value())); break;
    case Bytecodes::_f2l:  set_constant(SharedRuntime::f2l(x->value()->type()->as_FloatConstant()->value())); break;
    case Bytecodes::_d2f:  set_constant((float)(x->value()->type()->as_DoubleConstant()->value()));          break;
    case Bytecodes::_d2i:  set_constant(SharedRuntime::d2i(x->value()->type()->as_DoubleConstant()->value())); break;
    case Bytecodes::_d2l:  set_constant(SharedRuntime::d2l(x->value()->type()->as_DoubleConstant()->value())); break;
    default:
      ShouldNotReachHere();
    }
  }

  Value value = x->value();
  BasicType type = T_ILLEGAL;
  LoadField* lf = value->as_LoadField();
  if (lf) {
    type = lf->field_type();
  } else {
    LoadIndexed* li = value->as_LoadIndexed();
    if (li) {
      type = li->elt_type();
    } else {
      Convert* conv = value->as_Convert();
      if (conv) {
        switch (conv->op()) {
          case Bytecodes::_i2b: type = T_BYTE;  break;
          case Bytecodes::_i2s: type = T_SHORT; break;
          case Bytecodes::_i2c: type = T_CHAR;  break;
          default             :                 break;
        }
      }
    }
  }
  if (type != T_ILLEGAL) {
    switch (x->op()) {
      case Bytecodes::_i2b: if (type == T_BYTE)                    set_canonical(x->value()); break;
      case Bytecodes::_i2s: if (type == T_SHORT || type == T_BYTE) set_canonical(x->value()); break;
      case Bytecodes::_i2c: if (type == T_CHAR)                    set_canonical(x->value()); break;
      default             :                                                                   break;
    }
  } else {
    Op2* op2 = x->value()->as_Op2();
    if (op2 && op2->op() == Bytecodes::_iand && op2->y()->type()->is_constant()) {
      jint safebits = 0;
      jint mask = op2->y()->type()->as_IntConstant()->value();
      switch (x->op()) {
        case Bytecodes::_i2b: safebits = 0x7f;   break;
        case Bytecodes::_i2s: safebits = 0x7fff; break;
        case Bytecodes::_i2c: safebits = 0xffff; break;
        default             :                    break;
      }
      // When casting a masked integer to a smaller signed type, if
      // the mask doesn't include the sign bit the cast isn't needed.
      if (safebits && (mask & ~safebits) == 0) {
        set_canonical(x->value());
      }
    }
  }

}

void Canonicalizer::do_NullCheck      (NullCheck*       x) {
  if (x->obj()->as_NewArray() != NULL || x->obj()->as_NewInstance() != NULL) {
    set_canonical(x->obj());
  } else {
    Constant* con = x->obj()->as_Constant();
    if (con) {
      ObjectType* c = con->type()->as_ObjectType();
      if (c && c->is_loaded()) {
        ObjectConstant* oc = c->as_ObjectConstant();
        if (!oc || !oc->value()->is_null_object()) {
          set_canonical(con);
        }
      }
    }
  }
}

void Canonicalizer::do_TypeCast       (TypeCast*        x) {}
void Canonicalizer::do_Invoke         (Invoke*          x) {}
void Canonicalizer::do_NewInstance    (NewInstance*     x) {}
void Canonicalizer::do_NewTypeArray   (NewTypeArray*    x) {}
void Canonicalizer::do_NewObjectArray (NewObjectArray*  x) {}
void Canonicalizer::do_NewMultiArray  (NewMultiArray*   x) {}
void Canonicalizer::do_CheckCast      (CheckCast*       x) {
  if (x->klass()->is_loaded()) {
    Value obj = x->obj();
    ciType* klass = obj->exact_type();
    if (klass == NULL) {
      klass = obj->declared_type();
    }
    if (klass != NULL && klass->is_loaded()) {
      bool is_interface = klass->is_instance_klass() &&
                          klass->as_instance_klass()->is_interface();
      // Interface casts can't be statically optimized away since verifier doesn't
      // enforce interface types in bytecode.
      if (!is_interface && klass->is_subtype_of(x->klass())) {
        set_canonical(obj);
        return;
      }
    }
    // checkcast of null returns null
    if (obj->as_Constant() && obj->type()->as_ObjectType()->constant_value()->is_null_object()) {
      set_canonical(obj);
    }
  }
}
void Canonicalizer::do_InstanceOf     (InstanceOf*      x) {
  if (x->klass()->is_loaded()) {
    Value obj = x->obj();
    ciType* exact = obj->exact_type();
    if (exact != NULL && exact->is_loaded() && (obj->as_NewInstance() || obj->as_NewArray())) {
      set_constant(exact->is_subtype_of(x->klass()) ? 1 : 0);
      return;
    }
    // instanceof null returns false
    if (obj->as_Constant() && obj->type()->as_ObjectType()->constant_value()->is_null_object()) {
      set_constant(0);
    }
  }

}
void Canonicalizer::do_MonitorEnter   (MonitorEnter*    x) {}
void Canonicalizer::do_MonitorExit    (MonitorExit*     x) {}
void Canonicalizer::do_BlockBegin     (BlockBegin*      x) {}
void Canonicalizer::do_Goto           (Goto*            x) {}


static bool is_true(jlong x, If::Condition cond, jlong y) {
  switch (cond) {
    case If::eql: return x == y;
    case If::neq: return x != y;
    case If::lss: return x <  y;
    case If::leq: return x <= y;
    case If::gtr: return x >  y;
    case If::geq: return x >= y;
    default:
      ShouldNotReachHere();
      return false;
  }
}

static bool is_safepoint(BlockEnd* x, BlockBegin* sux) {
  // An Instruction with multiple successors, x, is replaced by a Goto
  // to a single successor, sux. Is a safepoint check needed = was the
  // instruction being replaced a safepoint and the single remaining
  // successor a back branch?
  return x->is_safepoint() && (sux->bci() < x->state_before()->bci());
}

void Canonicalizer::do_If(If* x) {
  // move const to right
  if (x->x()->type()->is_constant()) x->swap_operands();
  // simplify
  const Value l = x->x(); ValueType* lt = l->type();
  const Value r = x->y(); ValueType* rt = r->type();

  if (l == r && !lt->is_float_kind()) {
    // pattern: If (a cond a) => simplify to Goto
    BlockBegin* sux = NULL;
    switch (x->cond()) {
    case If::eql: sux = x->sux_for(true);  break;
    case If::neq: sux = x->sux_for(false); break;
    case If::lss: sux = x->sux_for(false); break;
    case If::leq: sux = x->sux_for(true);  break;
    case If::gtr: sux = x->sux_for(false); break;
    case If::geq: sux = x->sux_for(true);  break;
    default: ShouldNotReachHere();
    }
    // If is a safepoint then the debug information should come from the state_before of the If.
    set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
    return;
  }

  if (lt->is_constant() && rt->is_constant()) {
    if (x->x()->as_Constant() != NULL) {
      // pattern: If (lc cond rc) => simplify to: Goto
      BlockBegin* sux = x->x()->as_Constant()->compare(x->cond(), x->y(),
                                                       x->sux_for(true),
                                                       x->sux_for(false));
      if (sux != NULL) {
        // If is a safepoint then the debug information should come from the state_before of the If.
        set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
      }
    }
  } else if (rt->as_IntConstant() != NULL) {
    // pattern: If (l cond rc) => investigate further
    const jint rc = rt->as_IntConstant()->value();
    if (l->as_CompareOp() != NULL) {
      // pattern: If ((a cmp b) cond rc) => simplify to: If (x cond y) or: Goto
      CompareOp* cmp = l->as_CompareOp();
      bool unordered_is_less = cmp->op() == Bytecodes::_fcmpl || cmp->op() == Bytecodes::_dcmpl;
      BlockBegin* lss_sux = x->sux_for(is_true(-1, x->cond(), rc)); // successor for a < b
      BlockBegin* eql_sux = x->sux_for(is_true( 0, x->cond(), rc)); // successor for a = b
      BlockBegin* gtr_sux = x->sux_for(is_true(+1, x->cond(), rc)); // successor for a > b
      BlockBegin* nan_sux = unordered_is_less ? lss_sux : gtr_sux ; // successor for unordered
      // Note: At this point all successors (lss_sux, eql_sux, gtr_sux, nan_sux) are
      //       equal to x->tsux() or x->fsux(). Furthermore, nan_sux equals either
      //       lss_sux or gtr_sux.
      if (lss_sux == eql_sux && eql_sux == gtr_sux) {
        // all successors identical => simplify to: Goto
        set_canonical(new Goto(lss_sux, x->state_before(), x->is_safepoint()));
      } else {
        // two successors differ and two successors are the same => simplify to: If (x cmp y)
        // determine new condition & successors
        If::Condition cond = If::eql;
        BlockBegin* tsux = NULL;
        BlockBegin* fsux = NULL;
             if (lss_sux == eql_sux) { cond = If::leq; tsux = lss_sux; fsux = gtr_sux; }
        else if (lss_sux == gtr_sux) { cond = If::neq; tsux = lss_sux; fsux = eql_sux; }
        else if (eql_sux == gtr_sux) { cond = If::geq; tsux = eql_sux; fsux = lss_sux; }
        else                         { ShouldNotReachHere();                           }
        If* canon = new If(cmp->x(), cond, nan_sux == tsux, cmp->y(), tsux, fsux, cmp->state_before(), x->is_safepoint());
        if (cmp->x() == cmp->y()) {
          do_If(canon);
        } else {
          if (compilation()->profile_branches() || compilation()->is_profiling()) {
            // TODO: If profiling, leave floating point comparisons unoptimized.
            // We currently do not support profiling of the unordered case.
            switch(cmp->op()) {
              case Bytecodes::_fcmpl: case Bytecodes::_fcmpg:
              case Bytecodes::_dcmpl: case Bytecodes::_dcmpg:
                set_canonical(x);
                return;
              default:
                break;
            }
          }
          set_bci(cmp->state_before()->bci());
          set_canonical(canon);
        }
      }
    }
  } else if (rt == objectNull &&
           (l->as_NewInstance() || l->as_NewArray() ||
             (l->as_Local() && l->as_Local()->is_receiver()))) {
    if (x->cond() == Instruction::eql) {
      BlockBegin* sux = x->fsux();
      set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
    } else {
      assert(x->cond() == Instruction::neq, "only other valid case");
      BlockBegin* sux = x->tsux();
      set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
    }
  }
}


void Canonicalizer::do_TableSwitch(TableSwitch* x) {
  if (x->tag()->type()->is_constant()) {
    int v = x->tag()->type()->as_IntConstant()->value();
    BlockBegin* sux = x->default_sux();
    if (v >= x->lo_key() && v <= x->hi_key()) {
      sux = x->sux_at(v - x->lo_key());
    }
    set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
  }
}


void Canonicalizer::do_LookupSwitch(LookupSwitch* x) {
  if (x->tag()->type()->is_constant()) {
    int v = x->tag()->type()->as_IntConstant()->value();
    BlockBegin* sux = x->default_sux();
    for (int i = 0; i < x->length(); i++) {
      if (v == x->key_at(i)) {
        sux = x->sux_at(i);
      }
    }
    set_canonical(new Goto(sux, x->state_before(), is_safepoint(x, sux)));
  }
}


void Canonicalizer::do_Return         (Return*          x) {}
void Canonicalizer::do_Throw          (Throw*           x) {}
void Canonicalizer::do_Base           (Base*            x) {}
void Canonicalizer::do_OsrEntry       (OsrEntry*        x) {}
void Canonicalizer::do_ExceptionObject(ExceptionObject* x) {}
void Canonicalizer::do_RoundFP        (RoundFP*         x) {}
void Canonicalizer::do_UnsafeGet      (UnsafeGet*       x) {}
void Canonicalizer::do_UnsafePut      (UnsafePut*       x) {}
void Canonicalizer::do_UnsafeGetAndSet(UnsafeGetAndSet* x) {}
void Canonicalizer::do_ProfileCall    (ProfileCall*     x) {}
void Canonicalizer::do_ProfileReturnType(ProfileReturnType* x) {}
void Canonicalizer::do_ProfileInvoke  (ProfileInvoke*   x) {}
void Canonicalizer::do_RuntimeCall    (RuntimeCall*     x) {}
void Canonicalizer::do_RangeCheckPredicate(RangeCheckPredicate* x) {}
#ifdef ASSERT
void Canonicalizer::do_Assert         (Assert*          x) {}
#endif
void Canonicalizer::do_MemBar         (MemBar*          x) {}
