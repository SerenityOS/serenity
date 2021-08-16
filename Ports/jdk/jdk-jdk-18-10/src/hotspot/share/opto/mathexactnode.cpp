/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/mathexactnode.hpp"
#include "opto/subnode.hpp"

template <typename OverflowOp>
class AddHelper {
public:
  typedef typename OverflowOp::TypeClass TypeClass;
  typedef typename TypeClass::NativeType NativeType;

  static bool will_overflow(NativeType value1, NativeType value2) {
    NativeType result = value1 + value2;
    // Hacker's Delight 2-12 Overflow if both arguments have the opposite sign of the result
    if (((value1 ^ result) & (value2 ^ result)) >= 0) {
      return false;
    }
    return true;
  }

  static bool can_overflow(const Type* type1, const Type* type2) {
    if (type1 == TypeClass::ZERO || type2 == TypeClass::ZERO) {
      return false;
    }
    return true;
  }
};

template <typename OverflowOp>
class SubHelper {
public:
  typedef typename OverflowOp::TypeClass TypeClass;
  typedef typename TypeClass::NativeType NativeType;

  static bool will_overflow(NativeType value1, NativeType value2) {
    NativeType result = value1 - value2;
    // hacker's delight 2-12 overflow iff the arguments have different signs and
    // the sign of the result is different than the sign of arg1
    if (((value1 ^ value2) & (value1 ^ result)) >= 0) {
      return false;
    }
    return true;
  }

  static bool can_overflow(const Type* type1, const Type* type2) {
    if (type2 == TypeClass::ZERO) {
      return false;
    }
    return true;
  }
};

template <typename OverflowOp>
class MulHelper {
public:
  typedef typename OverflowOp::TypeClass TypeClass;

  static bool can_overflow(const Type* type1, const Type* type2) {
    if (type1 == TypeClass::ZERO || type2 == TypeClass::ZERO) {
      return false;
    } else if (type1 == TypeClass::ONE || type2 == TypeClass::ONE) {
      return false;
    }
    return true;
  }
};

bool OverflowAddINode::will_overflow(jint v1, jint v2) const {
  return AddHelper<OverflowAddINode>::will_overflow(v1, v2);
}

bool OverflowSubINode::will_overflow(jint v1, jint v2) const {
  return SubHelper<OverflowSubINode>::will_overflow(v1, v2);
}

bool OverflowMulINode::will_overflow(jint v1, jint v2) const {
    jlong result = (jlong) v1 * (jlong) v2;
    if ((jint) result == result) {
      return false;
    }
    return true;
}

bool OverflowAddLNode::will_overflow(jlong v1, jlong v2) const {
  return AddHelper<OverflowAddLNode>::will_overflow(v1, v2);
}

bool OverflowSubLNode::will_overflow(jlong v1, jlong v2) const {
  return SubHelper<OverflowSubLNode>::will_overflow(v1, v2);
}

bool OverflowMulLNode::is_overflow(jlong val1, jlong val2) {
    // x * { 0, 1 } will never overflow. Even for x = min_jlong
    if (val1 == 0 || val2 == 0 || val1 == 1 || val2 == 1) {
      return false;
    }

    // x * min_jlong for x not in { 0, 1 } overflows
    // even -1 as -1 * min_jlong is an overflow
    if (val1 == min_jlong || val2 == min_jlong) {
      return true;
    }

    // if (x * y) / y == x there is no overflow
    //
    // the multiplication here is done as unsigned to avoid undefined behaviour which
    // can be used by the compiler to assume that the check further down (result / val2 != val1)
    // is always false and breaks the overflow check
    julong v1 = (julong) val1;
    julong v2 = (julong) val2;
    julong tmp = v1 * v2;
    jlong result = (jlong) tmp;

    if (result / val2 != val1) {
      return true;
    }

    return false;
}

bool OverflowAddINode::can_overflow(const Type* t1, const Type* t2) const {
  return AddHelper<OverflowAddINode>::can_overflow(t1, t2);
}

bool OverflowSubINode::can_overflow(const Type* t1, const Type* t2) const {
  if (in(1) == in(2)) {
    return false;
  }
  return SubHelper<OverflowSubINode>::can_overflow(t1, t2);
}

bool OverflowMulINode::can_overflow(const Type* t1, const Type* t2) const {
  return MulHelper<OverflowMulINode>::can_overflow(t1, t2);
}

bool OverflowAddLNode::can_overflow(const Type* t1, const Type* t2) const {
  return AddHelper<OverflowAddLNode>::can_overflow(t1, t2);
}

bool OverflowSubLNode::can_overflow(const Type* t1, const Type* t2) const {
  if (in(1) == in(2)) {
    return false;
  }
  return SubHelper<OverflowSubLNode>::can_overflow(t1, t2);
}

bool OverflowMulLNode::can_overflow(const Type* t1, const Type* t2) const {
  return MulHelper<OverflowMulLNode>::can_overflow(t1, t2);
}

const Type* OverflowNode::sub(const Type* t1, const Type* t2) const {
  fatal("sub() should not be called for '%s'", NodeClassNames[this->Opcode()]);
  return TypeInt::CC;
}

template <typename OverflowOp>
struct IdealHelper {
  typedef typename OverflowOp::TypeClass TypeClass; // TypeInt, TypeLong
  typedef typename TypeClass::NativeType NativeType;

  static Node* Ideal(const OverflowOp* node, PhaseGVN* phase, bool can_reshape) {
    Node* arg1 = node->in(1);
    Node* arg2 = node->in(2);
    const Type* type1 = phase->type(arg1);
    const Type* type2 = phase->type(arg2);

    if (type1 == NULL || type2 == NULL) {
      return NULL;
    }

    if (type1 != Type::TOP && type1->singleton() &&
        type2 != Type::TOP && type2->singleton()) {
      NativeType val1 = TypeClass::as_self(type1)->get_con();
      NativeType val2 = TypeClass::as_self(type2)->get_con();
      if (node->will_overflow(val1, val2) == false) {
        Node* con_result = ConINode::make(0);
        return con_result;
      }
      return NULL;
    }
    return NULL;
  }

  static const Type* Value(const OverflowOp* node, PhaseTransform* phase) {
    const Type *t1 = phase->type( node->in(1) );
    const Type *t2 = phase->type( node->in(2) );
    if( t1 == Type::TOP ) return Type::TOP;
    if( t2 == Type::TOP ) return Type::TOP;

    const TypeClass* i1 = TypeClass::as_self(t1);
    const TypeClass* i2 = TypeClass::as_self(t2);

    if (i1 == NULL || i2 == NULL) {
      return TypeInt::CC;
    }

    if (t1->singleton() && t2->singleton()) {
      NativeType val1 = i1->get_con();
      NativeType val2 = i2->get_con();
      if (node->will_overflow(val1, val2)) {
        return TypeInt::CC;
      }
      return TypeInt::ZERO;
    } else if (i1 != TypeClass::TYPE_DOMAIN && i2 != TypeClass::TYPE_DOMAIN) {
      if (node->will_overflow(i1->_lo, i2->_lo)) {
        return TypeInt::CC;
      } else if (node->will_overflow(i1->_lo, i2->_hi)) {
        return TypeInt::CC;
      } else if (node->will_overflow(i1->_hi, i2->_lo)) {
        return TypeInt::CC;
      } else if (node->will_overflow(i1->_hi, i2->_hi)) {
        return TypeInt::CC;
      }
      return TypeInt::ZERO;
    }

    if (!node->can_overflow(t1, t2)) {
      return TypeInt::ZERO;
    }
    return TypeInt::CC;
  }
};

Node* OverflowINode::Ideal(PhaseGVN* phase, bool can_reshape) {
  return IdealHelper<OverflowINode>::Ideal(this, phase, can_reshape);
}

Node* OverflowLNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  return IdealHelper<OverflowLNode>::Ideal(this, phase, can_reshape);
}

const Type* OverflowINode::Value(PhaseGVN* phase) const {
  return IdealHelper<OverflowINode>::Value(this, phase);
}

const Type* OverflowLNode::Value(PhaseGVN* phase) const {
  return IdealHelper<OverflowLNode>::Value(this, phase);
}

