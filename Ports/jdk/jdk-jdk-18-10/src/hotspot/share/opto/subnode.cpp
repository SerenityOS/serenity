/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compileLog.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "memory/allocation.inline.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/loopnode.hpp"
#include "opto/matcher.hpp"
#include "opto/movenode.hpp"
#include "opto/mulnode.hpp"
#include "opto/opcodes.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "runtime/sharedRuntime.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

#include "math.h"

//=============================================================================
//------------------------------Identity---------------------------------------
// If right input is a constant 0, return the left input.
Node* SubNode::Identity(PhaseGVN* phase) {
  assert(in(1) != this, "Must already have called Value");
  assert(in(2) != this, "Must already have called Value");

  // Remove double negation
  const Type *zero = add_id();
  if( phase->type( in(1) )->higher_equal( zero ) &&
      in(2)->Opcode() == Opcode() &&
      phase->type( in(2)->in(1) )->higher_equal( zero ) ) {
    return in(2)->in(2);
  }

  // Convert "(X+Y) - Y" into X and "(X+Y) - X" into Y
  if (in(1)->Opcode() == Op_AddI) {
    if (in(1)->in(2) == in(2)) {
      return in(1)->in(1);
    }
    if (in(1)->in(1) == in(2)) {
      return in(1)->in(2);
    }

    // Also catch: "(X + Opaque2(Y)) - Y".  In this case, 'Y' is a loop-varying
    // trip counter and X is likely to be loop-invariant (that's how O2 Nodes
    // are originally used, although the optimizer sometimes jiggers things).
    // This folding through an O2 removes a loop-exit use of a loop-varying
    // value and generally lowers register pressure in and around the loop.
    if (in(1)->in(2)->Opcode() == Op_Opaque2 && in(1)->in(2)->in(1) == in(2)) {
      return in(1)->in(1);
    }
  }

  return ( phase->type( in(2) )->higher_equal( zero ) ) ? in(1) : this;
}

//------------------------------Value------------------------------------------
// A subtract node differences it's two inputs.
const Type* SubNode::Value_common(PhaseTransform *phase) const {
  const Node* in1 = in(1);
  const Node* in2 = in(2);
  // Either input is TOP ==> the result is TOP
  const Type* t1 = (in1 == this) ? Type::TOP : phase->type(in1);
  if( t1 == Type::TOP ) return Type::TOP;
  const Type* t2 = (in2 == this) ? Type::TOP : phase->type(in2);
  if( t2 == Type::TOP ) return Type::TOP;

  // Not correct for SubFnode and AddFNode (must check for infinity)
  // Equal?  Subtract is zero
  if (in1->eqv_uncast(in2))  return add_id();

  // Either input is BOTTOM ==> the result is the local BOTTOM
  if( t1 == Type::BOTTOM || t2 == Type::BOTTOM )
    return bottom_type();

  return NULL;
}

const Type* SubNode::Value(PhaseGVN* phase) const {
  const Type* t = Value_common(phase);
  if (t != NULL) {
    return t;
  }
  const Type* t1 = phase->type(in(1));
  const Type* t2 = phase->type(in(2));
  return sub(t1,t2);            // Local flavor of type subtraction

}

SubNode* SubNode::make(Node* in1, Node* in2, BasicType bt) {
  switch (bt) {
    case T_INT:
      return new SubINode(in1, in2);
    case T_LONG:
      return new SubLNode(in1, in2);
    default:
      fatal("Not implemented for %s", type2name(bt));
  }
  return NULL;
}

//=============================================================================
//------------------------------Helper function--------------------------------

static bool is_cloop_increment(Node* inc) {
  precond(inc->Opcode() == Op_AddI || inc->Opcode() == Op_AddL);

  if (!inc->in(1)->is_Phi()) {
    return false;
  }
  const PhiNode* phi = inc->in(1)->as_Phi();

  if (!phi->region()->is_CountedLoop()) {
    return false;
  }

  return inc == phi->region()->as_CountedLoop()->incr();
}

// Given the expression '(x + C) - v', or
//                      'v - (x + C)', we examine nodes '+' and 'v':
//
//  1. Do not convert if '+' is a counted-loop increment, because the '-' is
//     loop invariant and converting extends the live-range of 'x' to overlap
//     with the '+', forcing another register to be used in the loop.
//
//  2. Do not convert if 'v' is a counted-loop induction variable, because
//     'x' might be invariant.
//
static bool ok_to_convert(Node* inc, Node* var) {
  return !(is_cloop_increment(inc) || var->is_cloop_ind_var());
}

//------------------------------Ideal------------------------------------------
Node *SubINode::Ideal(PhaseGVN *phase, bool can_reshape){
  Node *in1 = in(1);
  Node *in2 = in(2);
  uint op1 = in1->Opcode();
  uint op2 = in2->Opcode();

#ifdef ASSERT
  // Check for dead loop
  if ((in1 == this) || (in2 == this) ||
      ((op1 == Op_AddI || op1 == Op_SubI) &&
       ((in1->in(1) == this) || (in1->in(2) == this) ||
        (in1->in(1) == in1)  || (in1->in(2) == in1)))) {
    assert(false, "dead loop in SubINode::Ideal");
  }
#endif

  const Type *t2 = phase->type( in2 );
  if( t2 == Type::TOP ) return NULL;
  // Convert "x-c0" into "x+ -c0".
  if( t2->base() == Type::Int ){        // Might be bottom or top...
    const TypeInt *i = t2->is_int();
    if( i->is_con() )
      return new AddINode(in1, phase->intcon(-i->get_con()));
  }

  // Convert "(x+c0) - y" into (x-y) + c0"
  // Do not collapse (x+c0)-y if "+" is a loop increment or
  // if "y" is a loop induction variable.
  if( op1 == Op_AddI && ok_to_convert(in1, in2) ) {
    const Type *tadd = phase->type( in1->in(2) );
    if( tadd->singleton() && tadd != Type::TOP ) {
      Node *sub2 = phase->transform( new SubINode( in1->in(1), in2 ));
      return new AddINode( sub2, in1->in(2) );
    }
  }


  // Convert "x - (y+c0)" into "(x-y) - c0"
  // Need the same check as in above optimization but reversed.
  if (op2 == Op_AddI && ok_to_convert(in2, in1)) {
    Node* in21 = in2->in(1);
    Node* in22 = in2->in(2);
    const TypeInt* tcon = phase->type(in22)->isa_int();
    if (tcon != NULL && tcon->is_con()) {
      Node* sub2 = phase->transform( new SubINode(in1, in21) );
      Node* neg_c0 = phase->intcon(- tcon->get_con());
      return new AddINode(sub2, neg_c0);
    }
  }

  const Type *t1 = phase->type( in1 );
  if( t1 == Type::TOP ) return NULL;

#ifdef ASSERT
  // Check for dead loop
  if ((op2 == Op_AddI || op2 == Op_SubI) &&
      ((in2->in(1) == this) || (in2->in(2) == this) ||
       (in2->in(1) == in2)  || (in2->in(2) == in2))) {
    assert(false, "dead loop in SubINode::Ideal");
  }
#endif

  // Convert "x - (x+y)" into "-y"
  if (op2 == Op_AddI && in1 == in2->in(1)) {
    return new SubINode(phase->intcon(0), in2->in(2));
  }
  // Convert "(x-y) - x" into "-y"
  if (op1 == Op_SubI && in1->in(1) == in2) {
    return new SubINode(phase->intcon(0), in1->in(2));
  }
  // Convert "x - (y+x)" into "-y"
  if (op2 == Op_AddI && in1 == in2->in(2)) {
    return new SubINode(phase->intcon(0), in2->in(1));
  }

  // Convert "0 - (x-y)" into "y-x", leave the double negation "-(-y)" to SubNode::Identity().
  if (t1 == TypeInt::ZERO && op2 == Op_SubI && phase->type(in2->in(1)) != TypeInt::ZERO) {
    return new SubINode(in2->in(2), in2->in(1));
  }

  // Convert "0 - (x+con)" into "-con-x"
  jint con;
  if( t1 == TypeInt::ZERO && op2 == Op_AddI &&
      (con = in2->in(2)->find_int_con(0)) != 0 )
    return new SubINode( phase->intcon(-con), in2->in(1) );

  // Convert "(X+A) - (X+B)" into "A - B"
  if( op1 == Op_AddI && op2 == Op_AddI && in1->in(1) == in2->in(1) )
    return new SubINode( in1->in(2), in2->in(2) );

  // Convert "(A+X) - (B+X)" into "A - B"
  if( op1 == Op_AddI && op2 == Op_AddI && in1->in(2) == in2->in(2) )
    return new SubINode( in1->in(1), in2->in(1) );

  // Convert "(A+X) - (X+B)" into "A - B"
  if( op1 == Op_AddI && op2 == Op_AddI && in1->in(2) == in2->in(1) )
    return new SubINode( in1->in(1), in2->in(2) );

  // Convert "(X+A) - (B+X)" into "A - B"
  if( op1 == Op_AddI && op2 == Op_AddI && in1->in(1) == in2->in(2) )
    return new SubINode( in1->in(2), in2->in(1) );

  // Convert "A-(B-C)" into (A+C)-B", since add is commutative and generally
  // nicer to optimize than subtract.
  if( op2 == Op_SubI && in2->outcnt() == 1) {
    Node *add1 = phase->transform( new AddINode( in1, in2->in(2) ) );
    return new SubINode( add1, in2->in(1) );
  }

  // Associative
  if (op1 == Op_MulI && op2 == Op_MulI) {
    Node* sub_in1 = NULL;
    Node* sub_in2 = NULL;
    Node* mul_in = NULL;

    if (in1->in(1) == in2->in(1)) {
      // Convert "a*b-a*c into a*(b-c)
      sub_in1 = in1->in(2);
      sub_in2 = in2->in(2);
      mul_in = in1->in(1);
    } else if (in1->in(2) == in2->in(1)) {
      // Convert a*b-b*c into b*(a-c)
      sub_in1 = in1->in(1);
      sub_in2 = in2->in(2);
      mul_in = in1->in(2);
    } else if (in1->in(2) == in2->in(2)) {
      // Convert a*c-b*c into (a-b)*c
      sub_in1 = in1->in(1);
      sub_in2 = in2->in(1);
      mul_in = in1->in(2);
    } else if (in1->in(1) == in2->in(2)) {
      // Convert a*b-c*a into a*(b-c)
      sub_in1 = in1->in(2);
      sub_in2 = in2->in(1);
      mul_in = in1->in(1);
    }

    if (mul_in != NULL) {
      Node* sub = phase->transform(new SubINode(sub_in1, sub_in2));
      return new MulINode(mul_in, sub);
    }
  }

  // Convert "0-(A>>31)" into "(A>>>31)"
  if ( op2 == Op_RShiftI ) {
    Node *in21 = in2->in(1);
    Node *in22 = in2->in(2);
    const TypeInt *zero = phase->type(in1)->isa_int();
    const TypeInt *t21 = phase->type(in21)->isa_int();
    const TypeInt *t22 = phase->type(in22)->isa_int();
    if ( t21 && t22 && zero == TypeInt::ZERO && t22->is_con(31) ) {
      return new URShiftINode(in21, in22);
    }
  }

  return NULL;
}

//------------------------------sub--------------------------------------------
// A subtract node differences it's two inputs.
const Type *SubINode::sub( const Type *t1, const Type *t2 ) const {
  const TypeInt *r0 = t1->is_int(); // Handy access
  const TypeInt *r1 = t2->is_int();
  int32_t lo = java_subtract(r0->_lo, r1->_hi);
  int32_t hi = java_subtract(r0->_hi, r1->_lo);

  // We next check for 32-bit overflow.
  // If that happens, we just assume all integers are possible.
  if( (((r0->_lo ^ r1->_hi) >= 0) ||    // lo ends have same signs OR
       ((r0->_lo ^      lo) >= 0)) &&   // lo results have same signs AND
      (((r0->_hi ^ r1->_lo) >= 0) ||    // hi ends have same signs OR
       ((r0->_hi ^      hi) >= 0)) )    // hi results have same signs
    return TypeInt::make(lo,hi,MAX2(r0->_widen,r1->_widen));
  else                          // Overflow; assume all integers
    return TypeInt::INT;
}

//=============================================================================
//------------------------------Ideal------------------------------------------
Node *SubLNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node *in1 = in(1);
  Node *in2 = in(2);
  uint op1 = in1->Opcode();
  uint op2 = in2->Opcode();

#ifdef ASSERT
  // Check for dead loop
  if ((in1 == this) || (in2 == this) ||
      ((op1 == Op_AddL || op1 == Op_SubL) &&
       ((in1->in(1) == this) || (in1->in(2) == this) ||
        (in1->in(1) == in1)  || (in1->in(2) == in1)))) {
    assert(false, "dead loop in SubLNode::Ideal");
  }
#endif

  if( phase->type( in2 ) == Type::TOP ) return NULL;
  const TypeLong *i = phase->type( in2 )->isa_long();
  // Convert "x-c0" into "x+ -c0".
  if( i &&                      // Might be bottom or top...
      i->is_con() )
    return new AddLNode(in1, phase->longcon(-i->get_con()));

  // Convert "(x+c0) - y" into (x-y) + c0"
  // Do not collapse (x+c0)-y if "+" is a loop increment or
  // if "y" is a loop induction variable.
  if( op1 == Op_AddL && ok_to_convert(in1, in2) ) {
    Node *in11 = in1->in(1);
    const Type *tadd = phase->type( in1->in(2) );
    if( tadd->singleton() && tadd != Type::TOP ) {
      Node *sub2 = phase->transform( new SubLNode( in11, in2 ));
      return new AddLNode( sub2, in1->in(2) );
    }
  }

  // Convert "x - (y+c0)" into "(x-y) - c0"
  // Need the same check as in above optimization but reversed.
  if (op2 == Op_AddL && ok_to_convert(in2, in1)) {
    Node* in21 = in2->in(1);
    Node* in22 = in2->in(2);
    const TypeLong* tcon = phase->type(in22)->isa_long();
    if (tcon != NULL && tcon->is_con()) {
      Node* sub2 = phase->transform( new SubLNode(in1, in21) );
      Node* neg_c0 = phase->longcon(- tcon->get_con());
      return new AddLNode(sub2, neg_c0);
    }
  }

  const Type *t1 = phase->type( in1 );
  if( t1 == Type::TOP ) return NULL;

#ifdef ASSERT
  // Check for dead loop
  if ((op2 == Op_AddL || op2 == Op_SubL) &&
      ((in2->in(1) == this) || (in2->in(2) == this) ||
       (in2->in(1) == in2)  || (in2->in(2) == in2))) {
    assert(false, "dead loop in SubLNode::Ideal");
  }
#endif

  // Convert "x - (x+y)" into "-y"
  if (op2 == Op_AddL && in1 == in2->in(1)) {
    return new SubLNode(phase->makecon(TypeLong::ZERO), in2->in(2));
  }
  // Convert "x - (y+x)" into "-y"
  if (op2 == Op_AddL && in1 == in2->in(2)) {
    return new SubLNode(phase->makecon(TypeLong::ZERO), in2->in(1));
  }

  // Convert "0 - (x-y)" into "y-x", leave the double negation "-(-y)" to SubNode::Identity.
  if (t1 == TypeLong::ZERO && op2 == Op_SubL && phase->type(in2->in(1)) != TypeLong::ZERO) {
    return new SubLNode(in2->in(2), in2->in(1));
  }

  // Convert "(X+A) - (X+B)" into "A - B"
  if( op1 == Op_AddL && op2 == Op_AddL && in1->in(1) == in2->in(1) )
    return new SubLNode( in1->in(2), in2->in(2) );

  // Convert "(A+X) - (B+X)" into "A - B"
  if( op1 == Op_AddL && op2 == Op_AddL && in1->in(2) == in2->in(2) )
    return new SubLNode( in1->in(1), in2->in(1) );

  // Convert "A-(B-C)" into (A+C)-B"
  if( op2 == Op_SubL && in2->outcnt() == 1) {
    Node *add1 = phase->transform( new AddLNode( in1, in2->in(2) ) );
    return new SubLNode( add1, in2->in(1) );
  }

  // Associative
  if (op1 == Op_MulL && op2 == Op_MulL) {
    Node* sub_in1 = NULL;
    Node* sub_in2 = NULL;
    Node* mul_in = NULL;

    if (in1->in(1) == in2->in(1)) {
      // Convert "a*b-a*c into a*(b+c)
      sub_in1 = in1->in(2);
      sub_in2 = in2->in(2);
      mul_in = in1->in(1);
    } else if (in1->in(2) == in2->in(1)) {
      // Convert a*b-b*c into b*(a-c)
      sub_in1 = in1->in(1);
      sub_in2 = in2->in(2);
      mul_in = in1->in(2);
    } else if (in1->in(2) == in2->in(2)) {
      // Convert a*c-b*c into (a-b)*c
      sub_in1 = in1->in(1);
      sub_in2 = in2->in(1);
      mul_in = in1->in(2);
    } else if (in1->in(1) == in2->in(2)) {
      // Convert a*b-c*a into a*(b-c)
      sub_in1 = in1->in(2);
      sub_in2 = in2->in(1);
      mul_in = in1->in(1);
    }

    if (mul_in != NULL) {
      Node* sub = phase->transform(new SubLNode(sub_in1, sub_in2));
      return new MulLNode(mul_in, sub);
    }
  }

  // Convert "0L-(A>>63)" into "(A>>>63)"
  if ( op2 == Op_RShiftL ) {
    Node *in21 = in2->in(1);
    Node *in22 = in2->in(2);
    const TypeLong *zero = phase->type(in1)->isa_long();
    const TypeLong *t21 = phase->type(in21)->isa_long();
    const TypeInt *t22 = phase->type(in22)->isa_int();
    if ( t21 && t22 && zero == TypeLong::ZERO && t22->is_con(63) ) {
      return new URShiftLNode(in21, in22);
    }
  }

  return NULL;
}

//------------------------------sub--------------------------------------------
// A subtract node differences it's two inputs.
const Type *SubLNode::sub( const Type *t1, const Type *t2 ) const {
  const TypeLong *r0 = t1->is_long(); // Handy access
  const TypeLong *r1 = t2->is_long();
  jlong lo = java_subtract(r0->_lo, r1->_hi);
  jlong hi = java_subtract(r0->_hi, r1->_lo);

  // We next check for 32-bit overflow.
  // If that happens, we just assume all integers are possible.
  if( (((r0->_lo ^ r1->_hi) >= 0) ||    // lo ends have same signs OR
       ((r0->_lo ^      lo) >= 0)) &&   // lo results have same signs AND
      (((r0->_hi ^ r1->_lo) >= 0) ||    // hi ends have same signs OR
       ((r0->_hi ^      hi) >= 0)) )    // hi results have same signs
    return TypeLong::make(lo,hi,MAX2(r0->_widen,r1->_widen));
  else                          // Overflow; assume all integers
    return TypeLong::LONG;
}

//=============================================================================
//------------------------------Value------------------------------------------
// A subtract node differences its two inputs.
const Type* SubFPNode::Value(PhaseGVN* phase) const {
  const Node* in1 = in(1);
  const Node* in2 = in(2);
  // Either input is TOP ==> the result is TOP
  const Type* t1 = (in1 == this) ? Type::TOP : phase->type(in1);
  if( t1 == Type::TOP ) return Type::TOP;
  const Type* t2 = (in2 == this) ? Type::TOP : phase->type(in2);
  if( t2 == Type::TOP ) return Type::TOP;

  // if both operands are infinity of same sign, the result is NaN; do
  // not replace with zero
  if (t1->is_finite() && t2->is_finite() && in1 == in2) {
    return add_id();
  }

  // Either input is BOTTOM ==> the result is the local BOTTOM
  const Type *bot = bottom_type();
  if( (t1 == bot) || (t2 == bot) ||
      (t1 == Type::BOTTOM) || (t2 == Type::BOTTOM) )
    return bot;

  return sub(t1,t2);            // Local flavor of type subtraction
}


//=============================================================================
//------------------------------Ideal------------------------------------------
Node *SubFNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  const Type *t2 = phase->type( in(2) );
  // Convert "x-c0" into "x+ -c0".
  if( t2->base() == Type::FloatCon ) {  // Might be bottom or top...
    // return new (phase->C, 3) AddFNode(in(1), phase->makecon( TypeF::make(-t2->getf()) ) );
  }

  // Cannot replace 0.0-X with -X because a 'fsub' bytecode computes
  // 0.0-0.0 as +0.0, while a 'fneg' bytecode computes -0.0.
  //if( phase->type(in(1)) == TypeF::ZERO )
  //return new (phase->C, 2) NegFNode(in(2));

  return NULL;
}

//------------------------------sub--------------------------------------------
// A subtract node differences its two inputs.
const Type *SubFNode::sub( const Type *t1, const Type *t2 ) const {
  // no folding if one of operands is infinity or NaN, do not do constant folding
  if( g_isfinite(t1->getf()) && g_isfinite(t2->getf()) ) {
    return TypeF::make( t1->getf() - t2->getf() );
  }
  else if( g_isnan(t1->getf()) ) {
    return t1;
  }
  else if( g_isnan(t2->getf()) ) {
    return t2;
  }
  else {
    return Type::FLOAT;
  }
}

//=============================================================================
//------------------------------Ideal------------------------------------------
Node *SubDNode::Ideal(PhaseGVN *phase, bool can_reshape){
  const Type *t2 = phase->type( in(2) );
  // Convert "x-c0" into "x+ -c0".
  if( t2->base() == Type::DoubleCon ) { // Might be bottom or top...
    // return new (phase->C, 3) AddDNode(in(1), phase->makecon( TypeD::make(-t2->getd()) ) );
  }

  // Cannot replace 0.0-X with -X because a 'dsub' bytecode computes
  // 0.0-0.0 as +0.0, while a 'dneg' bytecode computes -0.0.
  //if( phase->type(in(1)) == TypeD::ZERO )
  //return new (phase->C, 2) NegDNode(in(2));

  return NULL;
}

//------------------------------sub--------------------------------------------
// A subtract node differences its two inputs.
const Type *SubDNode::sub( const Type *t1, const Type *t2 ) const {
  // no folding if one of operands is infinity or NaN, do not do constant folding
  if( g_isfinite(t1->getd()) && g_isfinite(t2->getd()) ) {
    return TypeD::make( t1->getd() - t2->getd() );
  }
  else if( g_isnan(t1->getd()) ) {
    return t1;
  }
  else if( g_isnan(t2->getd()) ) {
    return t2;
  }
  else {
    return Type::DOUBLE;
  }
}

//=============================================================================
//------------------------------Idealize---------------------------------------
// Unlike SubNodes, compare must still flatten return value to the
// range -1, 0, 1.
// And optimizations like those for (X + Y) - X fail if overflow happens.
Node* CmpNode::Identity(PhaseGVN* phase) {
  return this;
}

#ifndef PRODUCT
//----------------------------related------------------------------------------
// Related nodes of comparison nodes include all data inputs (until hitting a
// control boundary) as well as all outputs until and including control nodes
// as well as their projections. In compact mode, data inputs till depth 1 and
// all outputs till depth 1 are considered.
void CmpNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
    this->collect_nodes(in_rel, 1, false, true);
    this->collect_nodes(out_rel, -1, false, false);
  } else {
    this->collect_nodes_in_all_data(in_rel, false);
    this->collect_nodes_out_all_ctrl_boundary(out_rel);
    // Now, find all control nodes in out_rel, and include their projections
    // and projection targets (if any) in the result.
    GrowableArray<Node*> proj(Compile::current()->unique());
    for (GrowableArrayIterator<Node*> it = out_rel->begin(); it != out_rel->end(); ++it) {
      Node* n = *it;
      if (n->is_CFG() && !n->is_Proj()) {
        // Assume projections and projection targets are found at levels 1 and 2.
        n->collect_nodes(&proj, -2, false, false);
        for (GrowableArrayIterator<Node*> p = proj.begin(); p != proj.end(); ++p) {
          out_rel->append_if_missing(*p);
        }
        proj.clear();
      }
    }
  }
}

#endif

CmpNode *CmpNode::make(Node *in1, Node *in2, BasicType bt, bool unsigned_comp) {
  switch (bt) {
    case T_INT:
      if (unsigned_comp) {
        return new CmpUNode(in1, in2);
      }
      return new CmpINode(in1, in2);
    case T_LONG:
      if (unsigned_comp) {
        return new CmpULNode(in1, in2);
      }
      return new CmpLNode(in1, in2);
    default:
      fatal("Not implemented for %s", type2name(bt));
  }
  return NULL;
}

//=============================================================================
//------------------------------cmp--------------------------------------------
// Simplify a CmpI (compare 2 integers) node, based on local information.
// If both inputs are constants, compare them.
const Type *CmpINode::sub( const Type *t1, const Type *t2 ) const {
  const TypeInt *r0 = t1->is_int(); // Handy access
  const TypeInt *r1 = t2->is_int();

  if( r0->_hi < r1->_lo )       // Range is always low?
    return TypeInt::CC_LT;
  else if( r0->_lo > r1->_hi )  // Range is always high?
    return TypeInt::CC_GT;

  else if( r0->is_con() && r1->is_con() ) { // comparing constants?
    assert(r0->get_con() == r1->get_con(), "must be equal");
    return TypeInt::CC_EQ;      // Equal results.
  } else if( r0->_hi == r1->_lo ) // Range is never high?
    return TypeInt::CC_LE;
  else if( r0->_lo == r1->_hi ) // Range is never low?
    return TypeInt::CC_GE;
  return TypeInt::CC;           // else use worst case results
}

// Simplify a CmpU (compare 2 integers) node, based on local information.
// If both inputs are constants, compare them.
const Type *CmpUNode::sub( const Type *t1, const Type *t2 ) const {
  assert(!t1->isa_ptr(), "obsolete usage of CmpU");

  // comparing two unsigned ints
  const TypeInt *r0 = t1->is_int();   // Handy access
  const TypeInt *r1 = t2->is_int();

  // Current installed version
  // Compare ranges for non-overlap
  juint lo0 = r0->_lo;
  juint hi0 = r0->_hi;
  juint lo1 = r1->_lo;
  juint hi1 = r1->_hi;

  // If either one has both negative and positive values,
  // it therefore contains both 0 and -1, and since [0..-1] is the
  // full unsigned range, the type must act as an unsigned bottom.
  bool bot0 = ((jint)(lo0 ^ hi0) < 0);
  bool bot1 = ((jint)(lo1 ^ hi1) < 0);

  if (bot0 || bot1) {
    // All unsigned values are LE -1 and GE 0.
    if (lo0 == 0 && hi0 == 0) {
      return TypeInt::CC_LE;            //   0 <= bot
    } else if ((jint)lo0 == -1 && (jint)hi0 == -1) {
      return TypeInt::CC_GE;            // -1 >= bot
    } else if (lo1 == 0 && hi1 == 0) {
      return TypeInt::CC_GE;            // bot >= 0
    } else if ((jint)lo1 == -1 && (jint)hi1 == -1) {
      return TypeInt::CC_LE;            // bot <= -1
    }
  } else {
    // We can use ranges of the form [lo..hi] if signs are the same.
    assert(lo0 <= hi0 && lo1 <= hi1, "unsigned ranges are valid");
    // results are reversed, '-' > '+' for unsigned compare
    if (hi0 < lo1) {
      return TypeInt::CC_LT;            // smaller
    } else if (lo0 > hi1) {
      return TypeInt::CC_GT;            // greater
    } else if (hi0 == lo1 && lo0 == hi1) {
      return TypeInt::CC_EQ;            // Equal results
    } else if (lo0 >= hi1) {
      return TypeInt::CC_GE;
    } else if (hi0 <= lo1) {
      // Check for special case in Hashtable::get.  (See below.)
      if ((jint)lo0 >= 0 && (jint)lo1 >= 0 && is_index_range_check())
        return TypeInt::CC_LT;
      return TypeInt::CC_LE;
    }
  }
  // Check for special case in Hashtable::get - the hash index is
  // mod'ed to the table size so the following range check is useless.
  // Check for: (X Mod Y) CmpU Y, where the mod result and Y both have
  // to be positive.
  // (This is a gross hack, since the sub method never
  // looks at the structure of the node in any other case.)
  if ((jint)lo0 >= 0 && (jint)lo1 >= 0 && is_index_range_check())
    return TypeInt::CC_LT;
  return TypeInt::CC;                   // else use worst case results
}

const Type* CmpUNode::Value(PhaseGVN* phase) const {
  const Type* t = SubNode::Value_common(phase);
  if (t != NULL) {
    return t;
  }
  const Node* in1 = in(1);
  const Node* in2 = in(2);
  const Type* t1 = phase->type(in1);
  const Type* t2 = phase->type(in2);
  assert(t1->isa_int(), "CmpU has only Int type inputs");
  if (t2 == TypeInt::INT) { // Compare to bottom?
    return bottom_type();
  }
  uint in1_op = in1->Opcode();
  if (in1_op == Op_AddI || in1_op == Op_SubI) {
    // The problem rise when result of AddI(SubI) may overflow
    // signed integer value. Let say the input type is
    // [256, maxint] then +128 will create 2 ranges due to
    // overflow: [minint, minint+127] and [384, maxint].
    // But C2 type system keep only 1 type range and as result
    // it use general [minint, maxint] for this case which we
    // can't optimize.
    //
    // Make 2 separate type ranges based on types of AddI(SubI) inputs
    // and compare results of their compare. If results are the same
    // CmpU node can be optimized.
    const Node* in11 = in1->in(1);
    const Node* in12 = in1->in(2);
    const Type* t11 = (in11 == in1) ? Type::TOP : phase->type(in11);
    const Type* t12 = (in12 == in1) ? Type::TOP : phase->type(in12);
    // Skip cases when input types are top or bottom.
    if ((t11 != Type::TOP) && (t11 != TypeInt::INT) &&
        (t12 != Type::TOP) && (t12 != TypeInt::INT)) {
      const TypeInt *r0 = t11->is_int();
      const TypeInt *r1 = t12->is_int();
      jlong lo_r0 = r0->_lo;
      jlong hi_r0 = r0->_hi;
      jlong lo_r1 = r1->_lo;
      jlong hi_r1 = r1->_hi;
      if (in1_op == Op_SubI) {
        jlong tmp = hi_r1;
        hi_r1 = -lo_r1;
        lo_r1 = -tmp;
        // Note, for substructing [minint,x] type range
        // long arithmetic provides correct overflow answer.
        // The confusion come from the fact that in 32-bit
        // -minint == minint but in 64-bit -minint == maxint+1.
      }
      jlong lo_long = lo_r0 + lo_r1;
      jlong hi_long = hi_r0 + hi_r1;
      int lo_tr1 = min_jint;
      int hi_tr1 = (int)hi_long;
      int lo_tr2 = (int)lo_long;
      int hi_tr2 = max_jint;
      bool underflow = lo_long != (jlong)lo_tr2;
      bool overflow  = hi_long != (jlong)hi_tr1;
      // Use sub(t1, t2) when there is no overflow (one type range)
      // or when both overflow and underflow (too complex).
      if ((underflow != overflow) && (hi_tr1 < lo_tr2)) {
        // Overflow only on one boundary, compare 2 separate type ranges.
        int w = MAX2(r0->_widen, r1->_widen); // _widen does not matter here
        const TypeInt* tr1 = TypeInt::make(lo_tr1, hi_tr1, w);
        const TypeInt* tr2 = TypeInt::make(lo_tr2, hi_tr2, w);
        const Type* cmp1 = sub(tr1, t2);
        const Type* cmp2 = sub(tr2, t2);
        if (cmp1 == cmp2) {
          return cmp1; // Hit!
        }
      }
    }
  }

  return sub(t1, t2);            // Local flavor of type subtraction
}

bool CmpUNode::is_index_range_check() const {
  // Check for the "(X ModI Y) CmpU Y" shape
  return (in(1)->Opcode() == Op_ModI &&
          in(1)->in(2)->eqv_uncast(in(2)));
}

//------------------------------Idealize---------------------------------------
Node *CmpINode::Ideal( PhaseGVN *phase, bool can_reshape ) {
  if (phase->type(in(2))->higher_equal(TypeInt::ZERO)) {
    switch (in(1)->Opcode()) {
    case Op_CmpL3:              // Collapse a CmpL3/CmpI into a CmpL
      return new CmpLNode(in(1)->in(1),in(1)->in(2));
    case Op_CmpF3:              // Collapse a CmpF3/CmpI into a CmpF
      return new CmpFNode(in(1)->in(1),in(1)->in(2));
    case Op_CmpD3:              // Collapse a CmpD3/CmpI into a CmpD
      return new CmpDNode(in(1)->in(1),in(1)->in(2));
    //case Op_SubI:
      // If (x - y) cannot overflow, then ((x - y) <?> 0)
      // can be turned into (x <?> y).
      // This is handled (with more general cases) by Ideal_sub_algebra.
    }
  }
  return NULL;                  // No change
}

Node *CmpLNode::Ideal( PhaseGVN *phase, bool can_reshape ) {
  const TypeLong *t2 = phase->type(in(2))->isa_long();
  if (Opcode() == Op_CmpL && in(1)->Opcode() == Op_ConvI2L && t2 && t2->is_con()) {
    const jlong con = t2->get_con();
    if (con >= min_jint && con <= max_jint) {
      return new CmpINode(in(1)->in(1), phase->intcon((jint)con));
    }
  }
  return NULL;
}

//=============================================================================
// Simplify a CmpL (compare 2 longs ) node, based on local information.
// If both inputs are constants, compare them.
const Type *CmpLNode::sub( const Type *t1, const Type *t2 ) const {
  const TypeLong *r0 = t1->is_long(); // Handy access
  const TypeLong *r1 = t2->is_long();

  if( r0->_hi < r1->_lo )       // Range is always low?
    return TypeInt::CC_LT;
  else if( r0->_lo > r1->_hi )  // Range is always high?
    return TypeInt::CC_GT;

  else if( r0->is_con() && r1->is_con() ) { // comparing constants?
    assert(r0->get_con() == r1->get_con(), "must be equal");
    return TypeInt::CC_EQ;      // Equal results.
  } else if( r0->_hi == r1->_lo ) // Range is never high?
    return TypeInt::CC_LE;
  else if( r0->_lo == r1->_hi ) // Range is never low?
    return TypeInt::CC_GE;
  return TypeInt::CC;           // else use worst case results
}


// Simplify a CmpUL (compare 2 unsigned longs) node, based on local information.
// If both inputs are constants, compare them.
const Type* CmpULNode::sub(const Type* t1, const Type* t2) const {
  assert(!t1->isa_ptr(), "obsolete usage of CmpUL");

  // comparing two unsigned longs
  const TypeLong* r0 = t1->is_long();   // Handy access
  const TypeLong* r1 = t2->is_long();

  // Current installed version
  // Compare ranges for non-overlap
  julong lo0 = r0->_lo;
  julong hi0 = r0->_hi;
  julong lo1 = r1->_lo;
  julong hi1 = r1->_hi;

  // If either one has both negative and positive values,
  // it therefore contains both 0 and -1, and since [0..-1] is the
  // full unsigned range, the type must act as an unsigned bottom.
  bool bot0 = ((jlong)(lo0 ^ hi0) < 0);
  bool bot1 = ((jlong)(lo1 ^ hi1) < 0);

  if (bot0 || bot1) {
    // All unsigned values are LE -1 and GE 0.
    if (lo0 == 0 && hi0 == 0) {
      return TypeInt::CC_LE;            //   0 <= bot
    } else if ((jlong)lo0 == -1 && (jlong)hi0 == -1) {
      return TypeInt::CC_GE;            // -1 >= bot
    } else if (lo1 == 0 && hi1 == 0) {
      return TypeInt::CC_GE;            // bot >= 0
    } else if ((jlong)lo1 == -1 && (jlong)hi1 == -1) {
      return TypeInt::CC_LE;            // bot <= -1
    }
  } else {
    // We can use ranges of the form [lo..hi] if signs are the same.
    assert(lo0 <= hi0 && lo1 <= hi1, "unsigned ranges are valid");
    // results are reversed, '-' > '+' for unsigned compare
    if (hi0 < lo1) {
      return TypeInt::CC_LT;            // smaller
    } else if (lo0 > hi1) {
      return TypeInt::CC_GT;            // greater
    } else if (hi0 == lo1 && lo0 == hi1) {
      return TypeInt::CC_EQ;            // Equal results
    } else if (lo0 >= hi1) {
      return TypeInt::CC_GE;
    } else if (hi0 <= lo1) {
      return TypeInt::CC_LE;
    }
  }

  return TypeInt::CC;                   // else use worst case results
}

//=============================================================================
//------------------------------sub--------------------------------------------
// Simplify an CmpP (compare 2 pointers) node, based on local information.
// If both inputs are constants, compare them.
const Type *CmpPNode::sub( const Type *t1, const Type *t2 ) const {
  const TypePtr *r0 = t1->is_ptr(); // Handy access
  const TypePtr *r1 = t2->is_ptr();

  // Undefined inputs makes for an undefined result
  if( TypePtr::above_centerline(r0->_ptr) ||
      TypePtr::above_centerline(r1->_ptr) )
    return Type::TOP;

  if (r0 == r1 && r0->singleton()) {
    // Equal pointer constants (klasses, nulls, etc.)
    return TypeInt::CC_EQ;
  }

  // See if it is 2 unrelated classes.
  const TypeOopPtr* oop_p0 = r0->isa_oopptr();
  const TypeOopPtr* oop_p1 = r1->isa_oopptr();
  bool both_oop_ptr = oop_p0 && oop_p1;

  if (both_oop_ptr) {
    Node* in1 = in(1)->uncast();
    Node* in2 = in(2)->uncast();
    AllocateNode* alloc1 = AllocateNode::Ideal_allocation(in1, NULL);
    AllocateNode* alloc2 = AllocateNode::Ideal_allocation(in2, NULL);
    if (MemNode::detect_ptr_independence(in1, alloc1, in2, alloc2, NULL)) {
      return TypeInt::CC_GT;  // different pointers
    }
  }

  const TypeKlassPtr* klass_p0 = r0->isa_klassptr();
  const TypeKlassPtr* klass_p1 = r1->isa_klassptr();

  if (both_oop_ptr || (klass_p0 && klass_p1)) { // both or neither are klass pointers
    ciKlass* klass0 = NULL;
    bool    xklass0 = false;
    ciKlass* klass1 = NULL;
    bool    xklass1 = false;

    if (oop_p0) {
      klass0 = oop_p0->klass();
      xklass0 = oop_p0->klass_is_exact();
    } else {
      assert(klass_p0, "must be non-null if oop_p0 is null");
      klass0 = klass_p0->klass();
      xklass0 = klass_p0->klass_is_exact();
    }

    if (oop_p1) {
      klass1 = oop_p1->klass();
      xklass1 = oop_p1->klass_is_exact();
    } else {
      assert(klass_p1, "must be non-null if oop_p1 is null");
      klass1 = klass_p1->klass();
      xklass1 = klass_p1->klass_is_exact();
    }

    if (klass0 && klass1 &&
        klass0->is_loaded() && !klass0->is_interface() && // do not trust interfaces
        klass1->is_loaded() && !klass1->is_interface() &&
        (!klass0->is_obj_array_klass() ||
         !klass0->as_obj_array_klass()->base_element_klass()->is_interface()) &&
        (!klass1->is_obj_array_klass() ||
         !klass1->as_obj_array_klass()->base_element_klass()->is_interface())) {
      bool unrelated_classes = false;
      // See if neither subclasses the other, or if the class on top
      // is precise.  In either of these cases, the compare is known
      // to fail if at least one of the pointers is provably not null.
      if (klass0->equals(klass1)) {  // if types are unequal but klasses are equal
        // Do nothing; we know nothing for imprecise types
      } else if (klass0->is_subtype_of(klass1)) {
        // If klass1's type is PRECISE, then classes are unrelated.
        unrelated_classes = xklass1;
      } else if (klass1->is_subtype_of(klass0)) {
        // If klass0's type is PRECISE, then classes are unrelated.
        unrelated_classes = xklass0;
      } else {                  // Neither subtypes the other
        unrelated_classes = true;
      }
      if (unrelated_classes) {
        // The oops classes are known to be unrelated. If the joined PTRs of
        // two oops is not Null and not Bottom, then we are sure that one
        // of the two oops is non-null, and the comparison will always fail.
        TypePtr::PTR jp = r0->join_ptr(r1->_ptr);
        if (jp != TypePtr::Null && jp != TypePtr::BotPTR) {
          return TypeInt::CC_GT;
        }
      }
    }
  }

  // Known constants can be compared exactly
  // Null can be distinguished from any NotNull pointers
  // Unknown inputs makes an unknown result
  if( r0->singleton() ) {
    intptr_t bits0 = r0->get_con();
    if( r1->singleton() )
      return bits0 == r1->get_con() ? TypeInt::CC_EQ : TypeInt::CC_GT;
    return ( r1->_ptr == TypePtr::NotNull && bits0==0 ) ? TypeInt::CC_GT : TypeInt::CC;
  } else if( r1->singleton() ) {
    intptr_t bits1 = r1->get_con();
    return ( r0->_ptr == TypePtr::NotNull && bits1==0 ) ? TypeInt::CC_GT : TypeInt::CC;
  } else
    return TypeInt::CC;
}

static inline Node* isa_java_mirror_load(PhaseGVN* phase, Node* n) {
  // Return the klass node for (indirect load from OopHandle)
  //   LoadBarrier?(LoadP(LoadP(AddP(foo:Klass, #java_mirror))))
  //   or NULL if not matching.
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
    n = bs->step_over_gc_barrier(n);

  if (n->Opcode() != Op_LoadP) return NULL;

  const TypeInstPtr* tp = phase->type(n)->isa_instptr();
  if (!tp || tp->klass() != phase->C->env()->Class_klass()) return NULL;

  Node* adr = n->in(MemNode::Address);
  // First load from OopHandle: ((OopHandle)mirror)->resolve(); may need barrier.
  if (adr->Opcode() != Op_LoadP || !phase->type(adr)->isa_rawptr()) return NULL;
  adr = adr->in(MemNode::Address);

  intptr_t off = 0;
  Node* k = AddPNode::Ideal_base_and_offset(adr, phase, off);
  if (k == NULL)  return NULL;
  const TypeKlassPtr* tkp = phase->type(k)->isa_klassptr();
  if (!tkp || off != in_bytes(Klass::java_mirror_offset())) return NULL;

  // We've found the klass node of a Java mirror load.
  return k;
}

static inline Node* isa_const_java_mirror(PhaseGVN* phase, Node* n) {
  // for ConP(Foo.class) return ConP(Foo.klass)
  // otherwise return NULL
  if (!n->is_Con()) return NULL;

  const TypeInstPtr* tp = phase->type(n)->isa_instptr();
  if (!tp) return NULL;

  ciType* mirror_type = tp->java_mirror_type();
  // TypeInstPtr::java_mirror_type() returns non-NULL for compile-
  // time Class constants only.
  if (!mirror_type) return NULL;

  // x.getClass() == int.class can never be true (for all primitive types)
  // Return a ConP(NULL) node for this case.
  if (mirror_type->is_classless()) {
    return phase->makecon(TypePtr::NULL_PTR);
  }

  // return the ConP(Foo.klass)
  assert(mirror_type->is_klass(), "mirror_type should represent a Klass*");
  return phase->makecon(TypeKlassPtr::make(mirror_type->as_klass()));
}

//------------------------------Ideal------------------------------------------
// Normalize comparisons between Java mirror loads to compare the klass instead.
//
// Also check for the case of comparing an unknown klass loaded from the primary
// super-type array vs a known klass with no subtypes.  This amounts to
// checking to see an unknown klass subtypes a known klass with no subtypes;
// this only happens on an exact match.  We can shorten this test by 1 load.
Node *CmpPNode::Ideal( PhaseGVN *phase, bool can_reshape ) {
  // Normalize comparisons between Java mirrors into comparisons of the low-
  // level klass, where a dependent load could be shortened.
  //
  // The new pattern has a nice effect of matching the same pattern used in the
  // fast path of instanceof/checkcast/Class.isInstance(), which allows
  // redundant exact type check be optimized away by GVN.
  // For example, in
  //   if (x.getClass() == Foo.class) {
  //     Foo foo = (Foo) x;
  //     // ... use a ...
  //   }
  // a CmpPNode could be shared between if_acmpne and checkcast
  {
    Node* k1 = isa_java_mirror_load(phase, in(1));
    Node* k2 = isa_java_mirror_load(phase, in(2));
    Node* conk2 = isa_const_java_mirror(phase, in(2));

    if (k1 && (k2 || conk2)) {
      Node* lhs = k1;
      Node* rhs = (k2 != NULL) ? k2 : conk2;
      set_req_X(1, lhs, phase);
      set_req_X(2, rhs, phase);
      return this;
    }
  }

  // Constant pointer on right?
  const TypeKlassPtr* t2 = phase->type(in(2))->isa_klassptr();
  if (t2 == NULL || !t2->klass_is_exact())
    return NULL;
  // Get the constant klass we are comparing to.
  ciKlass* superklass = t2->klass();

  // Now check for LoadKlass on left.
  Node* ldk1 = in(1);
  if (ldk1->is_DecodeNKlass()) {
    ldk1 = ldk1->in(1);
    if (ldk1->Opcode() != Op_LoadNKlass )
      return NULL;
  } else if (ldk1->Opcode() != Op_LoadKlass )
    return NULL;
  // Take apart the address of the LoadKlass:
  Node* adr1 = ldk1->in(MemNode::Address);
  intptr_t con2 = 0;
  Node* ldk2 = AddPNode::Ideal_base_and_offset(adr1, phase, con2);
  if (ldk2 == NULL)
    return NULL;
  if (con2 == oopDesc::klass_offset_in_bytes()) {
    // We are inspecting an object's concrete class.
    // Short-circuit the check if the query is abstract.
    if (superklass->is_interface() ||
        superklass->is_abstract()) {
      // Make it come out always false:
      this->set_req(2, phase->makecon(TypePtr::NULL_PTR));
      return this;
    }
  }

  // Check for a LoadKlass from primary supertype array.
  // Any nested loadklass from loadklass+con must be from the p.s. array.
  if (ldk2->is_DecodeNKlass()) {
    // Keep ldk2 as DecodeN since it could be used in CmpP below.
    if (ldk2->in(1)->Opcode() != Op_LoadNKlass )
      return NULL;
  } else if (ldk2->Opcode() != Op_LoadKlass)
    return NULL;

  // Verify that we understand the situation
  if (con2 != (intptr_t) superklass->super_check_offset())
    return NULL;                // Might be element-klass loading from array klass

  // If 'superklass' has no subklasses and is not an interface, then we are
  // assured that the only input which will pass the type check is
  // 'superklass' itself.
  //
  // We could be more liberal here, and allow the optimization on interfaces
  // which have a single implementor.  This would require us to increase the
  // expressiveness of the add_dependency() mechanism.
  // %%% Do this after we fix TypeOopPtr:  Deps are expressive enough now.

  // Object arrays must have their base element have no subtypes
  while (superklass->is_obj_array_klass()) {
    ciType* elem = superklass->as_obj_array_klass()->element_type();
    superklass = elem->as_klass();
  }
  if (superklass->is_instance_klass()) {
    ciInstanceKlass* ik = superklass->as_instance_klass();
    if (ik->has_subklass() || ik->is_interface())  return NULL;
    // Add a dependency if there is a chance that a subclass will be added later.
    if (!ik->is_final()) {
      phase->C->dependencies()->assert_leaf_type(ik);
    }
  }

  // Bypass the dependent load, and compare directly
  this->set_req(1,ldk2);

  return this;
}

//=============================================================================
//------------------------------sub--------------------------------------------
// Simplify an CmpN (compare 2 pointers) node, based on local information.
// If both inputs are constants, compare them.
const Type *CmpNNode::sub( const Type *t1, const Type *t2 ) const {
  ShouldNotReachHere();
  return bottom_type();
}

//------------------------------Ideal------------------------------------------
Node *CmpNNode::Ideal( PhaseGVN *phase, bool can_reshape ) {
  return NULL;
}

//=============================================================================
//------------------------------Value------------------------------------------
// Simplify an CmpF (compare 2 floats ) node, based on local information.
// If both inputs are constants, compare them.
const Type* CmpFNode::Value(PhaseGVN* phase) const {
  const Node* in1 = in(1);
  const Node* in2 = in(2);
  // Either input is TOP ==> the result is TOP
  const Type* t1 = (in1 == this) ? Type::TOP : phase->type(in1);
  if( t1 == Type::TOP ) return Type::TOP;
  const Type* t2 = (in2 == this) ? Type::TOP : phase->type(in2);
  if( t2 == Type::TOP ) return Type::TOP;

  // Not constants?  Don't know squat - even if they are the same
  // value!  If they are NaN's they compare to LT instead of EQ.
  const TypeF *tf1 = t1->isa_float_constant();
  const TypeF *tf2 = t2->isa_float_constant();
  if( !tf1 || !tf2 ) return TypeInt::CC;

  // This implements the Java bytecode fcmpl, so unordered returns -1.
  if( tf1->is_nan() || tf2->is_nan() )
    return TypeInt::CC_LT;

  if( tf1->_f < tf2->_f ) return TypeInt::CC_LT;
  if( tf1->_f > tf2->_f ) return TypeInt::CC_GT;
  assert( tf1->_f == tf2->_f, "do not understand FP behavior" );
  return TypeInt::CC_EQ;
}


//=============================================================================
//------------------------------Value------------------------------------------
// Simplify an CmpD (compare 2 doubles ) node, based on local information.
// If both inputs are constants, compare them.
const Type* CmpDNode::Value(PhaseGVN* phase) const {
  const Node* in1 = in(1);
  const Node* in2 = in(2);
  // Either input is TOP ==> the result is TOP
  const Type* t1 = (in1 == this) ? Type::TOP : phase->type(in1);
  if( t1 == Type::TOP ) return Type::TOP;
  const Type* t2 = (in2 == this) ? Type::TOP : phase->type(in2);
  if( t2 == Type::TOP ) return Type::TOP;

  // Not constants?  Don't know squat - even if they are the same
  // value!  If they are NaN's they compare to LT instead of EQ.
  const TypeD *td1 = t1->isa_double_constant();
  const TypeD *td2 = t2->isa_double_constant();
  if( !td1 || !td2 ) return TypeInt::CC;

  // This implements the Java bytecode dcmpl, so unordered returns -1.
  if( td1->is_nan() || td2->is_nan() )
    return TypeInt::CC_LT;

  if( td1->_d < td2->_d ) return TypeInt::CC_LT;
  if( td1->_d > td2->_d ) return TypeInt::CC_GT;
  assert( td1->_d == td2->_d, "do not understand FP behavior" );
  return TypeInt::CC_EQ;
}

//------------------------------Ideal------------------------------------------
Node *CmpDNode::Ideal(PhaseGVN *phase, bool can_reshape){
  // Check if we can change this to a CmpF and remove a ConvD2F operation.
  // Change  (CMPD (F2D (float)) (ConD value))
  // To      (CMPF      (float)  (ConF value))
  // Valid when 'value' does not lose precision as a float.
  // Benefits: eliminates conversion, does not require 24-bit mode

  // NaNs prevent commuting operands.  This transform works regardless of the
  // order of ConD and ConvF2D inputs by preserving the original order.
  int idx_f2d = 1;              // ConvF2D on left side?
  if( in(idx_f2d)->Opcode() != Op_ConvF2D )
    idx_f2d = 2;                // No, swap to check for reversed args
  int idx_con = 3-idx_f2d;      // Check for the constant on other input

  if( ConvertCmpD2CmpF &&
      in(idx_f2d)->Opcode() == Op_ConvF2D &&
      in(idx_con)->Opcode() == Op_ConD ) {
    const TypeD *t2 = in(idx_con)->bottom_type()->is_double_constant();
    double t2_value_as_double = t2->_d;
    float  t2_value_as_float  = (float)t2_value_as_double;
    if( t2_value_as_double == (double)t2_value_as_float ) {
      // Test value can be represented as a float
      // Eliminate the conversion to double and create new comparison
      Node *new_in1 = in(idx_f2d)->in(1);
      Node *new_in2 = phase->makecon( TypeF::make(t2_value_as_float) );
      if( idx_f2d != 1 ) {      // Must flip args to match original order
        Node *tmp = new_in1;
        new_in1 = new_in2;
        new_in2 = tmp;
      }
      CmpFNode *new_cmp = (Opcode() == Op_CmpD3)
        ? new CmpF3Node( new_in1, new_in2 )
        : new CmpFNode ( new_in1, new_in2 ) ;
      return new_cmp;           // Changed to CmpFNode
    }
    // Testing value required the precision of a double
  }
  return NULL;                  // No change
}


//=============================================================================
//------------------------------cc2logical-------------------------------------
// Convert a condition code type to a logical type
const Type *BoolTest::cc2logical( const Type *CC ) const {
  if( CC == Type::TOP ) return Type::TOP;
  if( CC->base() != Type::Int ) return TypeInt::BOOL; // Bottom or worse
  const TypeInt *ti = CC->is_int();
  if( ti->is_con() ) {          // Only 1 kind of condition codes set?
    // Match low order 2 bits
    int tmp = ((ti->get_con()&3) == (_test&3)) ? 1 : 0;
    if( _test & 4 ) tmp = 1-tmp;     // Optionally complement result
    return TypeInt::make(tmp);       // Boolean result
  }

  if( CC == TypeInt::CC_GE ) {
    if( _test == ge ) return TypeInt::ONE;
    if( _test == lt ) return TypeInt::ZERO;
  }
  if( CC == TypeInt::CC_LE ) {
    if( _test == le ) return TypeInt::ONE;
    if( _test == gt ) return TypeInt::ZERO;
  }

  return TypeInt::BOOL;
}

//------------------------------dump_spec-------------------------------------
// Print special per-node info
void BoolTest::dump_on(outputStream *st) const {
  const char *msg[] = {"eq","gt","of","lt","ne","le","nof","ge"};
  st->print("%s", msg[_test]);
}

// Returns the logical AND of two tests (or 'never' if both tests can never be true).
// For example, a test for 'le' followed by a test for 'lt' is equivalent with 'lt'.
BoolTest::mask BoolTest::merge(BoolTest other) const {
  const mask res[illegal+1][illegal+1] = {
    // eq,      gt,      of,      lt,      ne,      le,      nof,     ge,      never,   illegal
      {eq,      never,   illegal, never,   never,   eq,      illegal, eq,      never,   illegal},  // eq
      {never,   gt,      illegal, never,   gt,      never,   illegal, gt,      never,   illegal},  // gt
      {illegal, illegal, illegal, illegal, illegal, illegal, illegal, illegal, never,   illegal},  // of
      {never,   never,   illegal, lt,      lt,      lt,      illegal, never,   never,   illegal},  // lt
      {never,   gt,      illegal, lt,      ne,      lt,      illegal, gt,      never,   illegal},  // ne
      {eq,      never,   illegal, lt,      lt,      le,      illegal, eq,      never,   illegal},  // le
      {illegal, illegal, illegal, illegal, illegal, illegal, illegal, illegal, never,   illegal},  // nof
      {eq,      gt,      illegal, never,   gt,      eq,      illegal, ge,      never,   illegal},  // ge
      {never,   never,   never,   never,   never,   never,   never,   never,   never,   illegal},  // never
      {illegal, illegal, illegal, illegal, illegal, illegal, illegal, illegal, illegal, illegal}}; // illegal
  return res[_test][other._test];
}

//=============================================================================
uint BoolNode::hash() const { return (Node::hash() << 3)|(_test._test+1); }
uint BoolNode::size_of() const { return sizeof(BoolNode); }

//------------------------------operator==-------------------------------------
bool BoolNode::cmp( const Node &n ) const {
  const BoolNode *b = (const BoolNode *)&n; // Cast up
  return (_test._test == b->_test._test);
}

//-------------------------------make_predicate--------------------------------
Node* BoolNode::make_predicate(Node* test_value, PhaseGVN* phase) {
  if (test_value->is_Con())   return test_value;
  if (test_value->is_Bool())  return test_value;
  if (test_value->is_CMove() &&
      test_value->in(CMoveNode::Condition)->is_Bool()) {
    BoolNode*   bol   = test_value->in(CMoveNode::Condition)->as_Bool();
    const Type* ftype = phase->type(test_value->in(CMoveNode::IfFalse));
    const Type* ttype = phase->type(test_value->in(CMoveNode::IfTrue));
    if (ftype == TypeInt::ZERO && !TypeInt::ZERO->higher_equal(ttype)) {
      return bol;
    } else if (ttype == TypeInt::ZERO && !TypeInt::ZERO->higher_equal(ftype)) {
      return phase->transform( bol->negate(phase) );
    }
    // Else fall through.  The CMove gets in the way of the test.
    // It should be the case that make_predicate(bol->as_int_value()) == bol.
  }
  Node* cmp = new CmpINode(test_value, phase->intcon(0));
  cmp = phase->transform(cmp);
  Node* bol = new BoolNode(cmp, BoolTest::ne);
  return phase->transform(bol);
}

//--------------------------------as_int_value---------------------------------
Node* BoolNode::as_int_value(PhaseGVN* phase) {
  // Inverse to make_predicate.  The CMove probably boils down to a Conv2B.
  Node* cmov = CMoveNode::make(NULL, this,
                               phase->intcon(0), phase->intcon(1),
                               TypeInt::BOOL);
  return phase->transform(cmov);
}

//----------------------------------negate-------------------------------------
BoolNode* BoolNode::negate(PhaseGVN* phase) {
  return new BoolNode(in(1), _test.negate());
}

// Change "bool eq/ne (cmp (add/sub A B) C)" into false/true if add/sub
// overflows and we can prove that C is not in the two resulting ranges.
// This optimization is similar to the one performed by CmpUNode::Value().
Node* BoolNode::fold_cmpI(PhaseGVN* phase, SubNode* cmp, Node* cmp1, int cmp_op,
                          int cmp1_op, const TypeInt* cmp2_type) {
  // Only optimize eq/ne integer comparison of add/sub
  if((_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
     (cmp_op == Op_CmpI) && (cmp1_op == Op_AddI || cmp1_op == Op_SubI)) {
    // Skip cases were inputs of add/sub are not integers or of bottom type
    const TypeInt* r0 = phase->type(cmp1->in(1))->isa_int();
    const TypeInt* r1 = phase->type(cmp1->in(2))->isa_int();
    if ((r0 != NULL) && (r0 != TypeInt::INT) &&
        (r1 != NULL) && (r1 != TypeInt::INT) &&
        (cmp2_type != TypeInt::INT)) {
      // Compute exact (long) type range of add/sub result
      jlong lo_long = r0->_lo;
      jlong hi_long = r0->_hi;
      if (cmp1_op == Op_AddI) {
        lo_long += r1->_lo;
        hi_long += r1->_hi;
      } else {
        lo_long -= r1->_hi;
        hi_long -= r1->_lo;
      }
      // Check for over-/underflow by casting to integer
      int lo_int = (int)lo_long;
      int hi_int = (int)hi_long;
      bool underflow = lo_long != (jlong)lo_int;
      bool overflow  = hi_long != (jlong)hi_int;
      if ((underflow != overflow) && (hi_int < lo_int)) {
        // Overflow on one boundary, compute resulting type ranges:
        // tr1 [MIN_INT, hi_int] and tr2 [lo_int, MAX_INT]
        int w = MAX2(r0->_widen, r1->_widen); // _widen does not matter here
        const TypeInt* tr1 = TypeInt::make(min_jint, hi_int, w);
        const TypeInt* tr2 = TypeInt::make(lo_int, max_jint, w);
        // Compare second input of cmp to both type ranges
        const Type* sub_tr1 = cmp->sub(tr1, cmp2_type);
        const Type* sub_tr2 = cmp->sub(tr2, cmp2_type);
        if (sub_tr1 == TypeInt::CC_LT && sub_tr2 == TypeInt::CC_GT) {
          // The result of the add/sub will never equal cmp2. Replace BoolNode
          // by false (0) if it tests for equality and by true (1) otherwise.
          return ConINode::make((_test._test == BoolTest::eq) ? 0 : 1);
        }
      }
    }
  }
  return NULL;
}

static bool is_counted_loop_cmp(Node *cmp) {
  Node *n = cmp->in(1)->in(1);
  return n != NULL &&
         n->is_Phi() &&
         n->in(0) != NULL &&
         n->in(0)->is_CountedLoop() &&
         n->in(0)->as_CountedLoop()->phi() == n;
}

//------------------------------Ideal------------------------------------------
Node *BoolNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Change "bool tst (cmp con x)" into "bool ~tst (cmp x con)".
  // This moves the constant to the right.  Helps value-numbering.
  Node *cmp = in(1);
  if( !cmp->is_Sub() ) return NULL;
  int cop = cmp->Opcode();
  if( cop == Op_FastLock || cop == Op_FastUnlock || cmp->is_SubTypeCheck()) return NULL;
  Node *cmp1 = cmp->in(1);
  Node *cmp2 = cmp->in(2);
  if( !cmp1 ) return NULL;

  if (_test._test == BoolTest::overflow || _test._test == BoolTest::no_overflow) {
    return NULL;
  }

  // Constant on left?
  Node *con = cmp1;
  uint op2 = cmp2->Opcode();
  // Move constants to the right of compare's to canonicalize.
  // Do not muck with Opaque1 nodes, as this indicates a loop
  // guard that cannot change shape.
  if( con->is_Con() && !cmp2->is_Con() && op2 != Op_Opaque1 &&
      // Because of NaN's, CmpD and CmpF are not commutative
      cop != Op_CmpD && cop != Op_CmpF &&
      // Protect against swapping inputs to a compare when it is used by a
      // counted loop exit, which requires maintaining the loop-limit as in(2)
      !is_counted_loop_exit_test() ) {
    // Ok, commute the constant to the right of the cmp node.
    // Clone the Node, getting a new Node of the same class
    cmp = cmp->clone();
    // Swap inputs to the clone
    cmp->swap_edges(1, 2);
    cmp = phase->transform( cmp );
    return new BoolNode( cmp, _test.commute() );
  }

  // Change "bool eq/ne (cmp (and X 16) 16)" into "bool ne/eq (cmp (and X 16) 0)".
  if (cop == Op_CmpI &&
      (_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
      cmp1->Opcode() == Op_AndI && cmp2->Opcode() == Op_ConI &&
      cmp1->in(2)->Opcode() == Op_ConI) {
    const TypeInt *t12 = phase->type(cmp2)->isa_int();
    const TypeInt *t112 = phase->type(cmp1->in(2))->isa_int();
    if (t12 && t12->is_con() && t112 && t112->is_con() &&
        t12->get_con() == t112->get_con() && is_power_of_2(t12->get_con())) {
      Node *ncmp = phase->transform(new CmpINode(cmp1, phase->intcon(0)));
      return new BoolNode(ncmp, _test.negate());
    }
  }

  // Same for long type: change "bool eq/ne (cmp (and X 16) 16)" into "bool ne/eq (cmp (and X 16) 0)".
  if (cop == Op_CmpL &&
      (_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
      cmp1->Opcode() == Op_AndL && cmp2->Opcode() == Op_ConL &&
      cmp1->in(2)->Opcode() == Op_ConL) {
    const TypeLong *t12 = phase->type(cmp2)->isa_long();
    const TypeLong *t112 = phase->type(cmp1->in(2))->isa_long();
    if (t12 && t12->is_con() && t112 && t112->is_con() &&
        t12->get_con() == t112->get_con() && is_power_of_2(t12->get_con())) {
      Node *ncmp = phase->transform(new CmpLNode(cmp1, phase->longcon(0)));
      return new BoolNode(ncmp, _test.negate());
    }
  }

  // Change "bool eq/ne (cmp (xor X 1) 0)" into "bool ne/eq (cmp X 0)".
  // The XOR-1 is an idiom used to flip the sense of a bool.  We flip the
  // test instead.
  int cmp1_op = cmp1->Opcode();
  const TypeInt* cmp2_type = phase->type(cmp2)->isa_int();
  if (cmp2_type == NULL)  return NULL;
  Node* j_xor = cmp1;
  if( cmp2_type == TypeInt::ZERO &&
      cmp1_op == Op_XorI &&
      j_xor->in(1) != j_xor &&          // An xor of itself is dead
      phase->type( j_xor->in(1) ) == TypeInt::BOOL &&
      phase->type( j_xor->in(2) ) == TypeInt::ONE &&
      (_test._test == BoolTest::eq ||
       _test._test == BoolTest::ne) ) {
    Node *ncmp = phase->transform(new CmpINode(j_xor->in(1),cmp2));
    return new BoolNode( ncmp, _test.negate() );
  }

  // Change ((x & m) u<= m) or ((m & x) u<= m) to always true
  // Same with ((x & m) u< m+1) and ((m & x) u< m+1)
  if (cop == Op_CmpU &&
      cmp1_op == Op_AndI) {
    Node* bound = NULL;
    if (_test._test == BoolTest::le) {
      bound = cmp2;
    } else if (_test._test == BoolTest::lt &&
               cmp2->Opcode() == Op_AddI &&
               cmp2->in(2)->find_int_con(0) == 1) {
      bound = cmp2->in(1);
    }
    if (cmp1->in(2) == bound || cmp1->in(1) == bound) {
      return ConINode::make(1);
    }
  }

  // Change ((x & (m - 1)) u< m) into (m > 0)
  // This is the off-by-one variant of the above
  if (cop == Op_CmpU &&
      _test._test == BoolTest::lt &&
      cmp1_op == Op_AndI) {
    Node* l = cmp1->in(1);
    Node* r = cmp1->in(2);
    for (int repeat = 0; repeat < 2; repeat++) {
      bool match = r->Opcode() == Op_AddI && r->in(2)->find_int_con(0) == -1 &&
                   r->in(1) == cmp2;
      if (match) {
        // arraylength known to be non-negative, so a (arraylength != 0) is sufficient,
        // but to be compatible with the array range check pattern, use (arraylength u> 0)
        Node* ncmp = cmp2->Opcode() == Op_LoadRange
                     ? phase->transform(new CmpUNode(cmp2, phase->intcon(0)))
                     : phase->transform(new CmpINode(cmp2, phase->intcon(0)));
        return new BoolNode(ncmp, BoolTest::gt);
      } else {
        // commute and try again
        l = cmp1->in(2);
        r = cmp1->in(1);
      }
    }
  }

  // Change x u< 1 or x u<= 0 to x == 0
  if (cop == Op_CmpU &&
      cmp1_op != Op_LoadRange &&
      ((_test._test == BoolTest::lt &&
        cmp2->find_int_con(-1) == 1) ||
       (_test._test == BoolTest::le &&
        cmp2->find_int_con(-1) == 0))) {
    Node* ncmp = phase->transform(new CmpINode(cmp1, phase->intcon(0)));
    return new BoolNode(ncmp, BoolTest::eq);
  }

  // Change (arraylength <= 0) or (arraylength == 0)
  //   into (arraylength u<= 0)
  // Also change (arraylength != 0) into (arraylength u> 0)
  // The latter version matches the code pattern generated for
  // array range checks, which will more likely be optimized later.
  if (cop == Op_CmpI &&
      cmp1_op == Op_LoadRange &&
      cmp2->find_int_con(-1) == 0) {
    if (_test._test == BoolTest::le || _test._test == BoolTest::eq) {
      Node* ncmp = phase->transform(new CmpUNode(cmp1, cmp2));
      return new BoolNode(ncmp, BoolTest::le);
    } else if (_test._test == BoolTest::ne) {
      Node* ncmp = phase->transform(new CmpUNode(cmp1, cmp2));
      return new BoolNode(ncmp, BoolTest::gt);
    }
  }

  // Change "bool eq/ne (cmp (Conv2B X) 0)" into "bool eq/ne (cmp X 0)".
  // This is a standard idiom for branching on a boolean value.
  Node *c2b = cmp1;
  if( cmp2_type == TypeInt::ZERO &&
      cmp1_op == Op_Conv2B &&
      (_test._test == BoolTest::eq ||
       _test._test == BoolTest::ne) ) {
    Node *ncmp = phase->transform(phase->type(c2b->in(1))->isa_int()
       ? (Node*)new CmpINode(c2b->in(1),cmp2)
       : (Node*)new CmpPNode(c2b->in(1),phase->makecon(TypePtr::NULL_PTR))
    );
    return new BoolNode( ncmp, _test._test );
  }

  // Comparing a SubI against a zero is equal to comparing the SubI
  // arguments directly.  This only works for eq and ne comparisons
  // due to possible integer overflow.
  if ((_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
        (cop == Op_CmpI) &&
        (cmp1_op == Op_SubI) &&
        ( cmp2_type == TypeInt::ZERO ) ) {
    Node *ncmp = phase->transform( new CmpINode(cmp1->in(1),cmp1->in(2)));
    return new BoolNode( ncmp, _test._test );
  }

  // Same as above but with and AddI of a constant
  if ((_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
      cop == Op_CmpI &&
      cmp1_op == Op_AddI &&
      cmp1->in(2) != NULL &&
      phase->type(cmp1->in(2))->isa_int() &&
      phase->type(cmp1->in(2))->is_int()->is_con() &&
      cmp2_type == TypeInt::ZERO &&
      !is_counted_loop_cmp(cmp) // modifying the exit test of a counted loop messes the counted loop shape
      ) {
    const TypeInt* cmp1_in2 = phase->type(cmp1->in(2))->is_int();
    Node *ncmp = phase->transform( new CmpINode(cmp1->in(1),phase->intcon(-cmp1_in2->_hi)));
    return new BoolNode( ncmp, _test._test );
  }

  // Change "bool eq/ne (cmp (phi (X -X) 0))" into "bool eq/ne (cmp X 0)"
  // since zero check of conditional negation of an integer is equal to
  // zero check of the integer directly.
  if ((_test._test == BoolTest::eq || _test._test == BoolTest::ne) &&
      (cop == Op_CmpI) &&
      (cmp2_type == TypeInt::ZERO) &&
      (cmp1_op == Op_Phi)) {
    // There should be a diamond phi with true path at index 1 or 2
    PhiNode *phi = cmp1->as_Phi();
    int idx_true = phi->is_diamond_phi();
    if (idx_true != 0) {
      // True input is in(idx_true) while false input is in(3 - idx_true)
      Node *tin = phi->in(idx_true);
      Node *fin = phi->in(3 - idx_true);
      if ((tin->Opcode() == Op_SubI) &&
          (phase->type(tin->in(1)) == TypeInt::ZERO) &&
          (tin->in(2) == fin)) {
        // Found conditional negation at true path, create a new CmpINode without that
        Node *ncmp = phase->transform(new CmpINode(fin, cmp2));
        return new BoolNode(ncmp, _test._test);
      }
      if ((fin->Opcode() == Op_SubI) &&
          (phase->type(fin->in(1)) == TypeInt::ZERO) &&
          (fin->in(2) == tin)) {
        // Found conditional negation at false path, create a new CmpINode without that
        Node *ncmp = phase->transform(new CmpINode(tin, cmp2));
        return new BoolNode(ncmp, _test._test);
      }
    }
  }

  // Change (-A vs 0) into (A vs 0) by commuting the test.  Disallow in the
  // most general case because negating 0x80000000 does nothing.  Needed for
  // the CmpF3/SubI/CmpI idiom.
  if( cop == Op_CmpI &&
      cmp1_op == Op_SubI &&
      cmp2_type == TypeInt::ZERO &&
      phase->type( cmp1->in(1) ) == TypeInt::ZERO &&
      phase->type( cmp1->in(2) )->higher_equal(TypeInt::SYMINT) ) {
    Node *ncmp = phase->transform( new CmpINode(cmp1->in(2),cmp2));
    return new BoolNode( ncmp, _test.commute() );
  }

  // Try to optimize signed integer comparison
  return fold_cmpI(phase, cmp->as_Sub(), cmp1, cop, cmp1_op, cmp2_type);

  //  The transformation below is not valid for either signed or unsigned
  //  comparisons due to wraparound concerns at MAX_VALUE and MIN_VALUE.
  //  This transformation can be resurrected when we are able to
  //  make inferences about the range of values being subtracted from
  //  (or added to) relative to the wraparound point.
  //
  //    // Remove +/-1's if possible.
  //    // "X <= Y-1" becomes "X <  Y"
  //    // "X+1 <= Y" becomes "X <  Y"
  //    // "X <  Y+1" becomes "X <= Y"
  //    // "X-1 <  Y" becomes "X <= Y"
  //    // Do not this to compares off of the counted-loop-end.  These guys are
  //    // checking the trip counter and they want to use the post-incremented
  //    // counter.  If they use the PRE-incremented counter, then the counter has
  //    // to be incremented in a private block on a loop backedge.
  //    if( du && du->cnt(this) && du->out(this)[0]->Opcode() == Op_CountedLoopEnd )
  //      return NULL;
  //  #ifndef PRODUCT
  //    // Do not do this in a wash GVN pass during verification.
  //    // Gets triggered by too many simple optimizations to be bothered with
  //    // re-trying it again and again.
  //    if( !phase->allow_progress() ) return NULL;
  //  #endif
  //    // Not valid for unsigned compare because of corner cases in involving zero.
  //    // For example, replacing "X-1 <u Y" with "X <=u Y" fails to throw an
  //    // exception in case X is 0 (because 0-1 turns into 4billion unsigned but
  //    // "0 <=u Y" is always true).
  //    if( cmp->Opcode() == Op_CmpU ) return NULL;
  //    int cmp2_op = cmp2->Opcode();
  //    if( _test._test == BoolTest::le ) {
  //      if( cmp1_op == Op_AddI &&
  //          phase->type( cmp1->in(2) ) == TypeInt::ONE )
  //        return clone_cmp( cmp, cmp1->in(1), cmp2, phase, BoolTest::lt );
  //      else if( cmp2_op == Op_AddI &&
  //         phase->type( cmp2->in(2) ) == TypeInt::MINUS_1 )
  //        return clone_cmp( cmp, cmp1, cmp2->in(1), phase, BoolTest::lt );
  //    } else if( _test._test == BoolTest::lt ) {
  //      if( cmp1_op == Op_AddI &&
  //          phase->type( cmp1->in(2) ) == TypeInt::MINUS_1 )
  //        return clone_cmp( cmp, cmp1->in(1), cmp2, phase, BoolTest::le );
  //      else if( cmp2_op == Op_AddI &&
  //         phase->type( cmp2->in(2) ) == TypeInt::ONE )
  //        return clone_cmp( cmp, cmp1, cmp2->in(1), phase, BoolTest::le );
  //    }
}

//------------------------------Value------------------------------------------
// Simplify a Bool (convert condition codes to boolean (1 or 0)) node,
// based on local information.   If the input is constant, do it.
const Type* BoolNode::Value(PhaseGVN* phase) const {
  return _test.cc2logical( phase->type( in(1) ) );
}

#ifndef PRODUCT
//------------------------------dump_spec--------------------------------------
// Dump special per-node info
void BoolNode::dump_spec(outputStream *st) const {
  st->print("[");
  _test.dump_on(st);
  st->print("]");
}

//-------------------------------related---------------------------------------
// A BoolNode's related nodes are all of its data inputs, and all of its
// outputs until control nodes are hit, which are included. In compact
// representation, inputs till level 3 and immediate outputs are included.
void BoolNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
    this->collect_nodes(in_rel, 3, false, true);
    this->collect_nodes(out_rel, -1, false, false);
  } else {
    this->collect_nodes_in_all_data(in_rel, false);
    this->collect_nodes_out_all_ctrl_boundary(out_rel);
  }
}
#endif

//----------------------is_counted_loop_exit_test------------------------------
// Returns true if node is used by a counted loop node.
bool BoolNode::is_counted_loop_exit_test() {
  for( DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++ ) {
    Node* use = fast_out(i);
    if (use->is_CountedLoopEnd()) {
      return true;
    }
  }
  return false;
}

//=============================================================================
//------------------------------Value------------------------------------------
// Compute sqrt
const Type* SqrtDNode::Value(PhaseGVN* phase) const {
  const Type *t1 = phase->type( in(1) );
  if( t1 == Type::TOP ) return Type::TOP;
  if( t1->base() != Type::DoubleCon ) return Type::DOUBLE;
  double d = t1->getd();
  if( d < 0.0 ) return Type::DOUBLE;
  return TypeD::make( sqrt( d ) );
}

const Type* SqrtFNode::Value(PhaseGVN* phase) const {
  const Type *t1 = phase->type( in(1) );
  if( t1 == Type::TOP ) return Type::TOP;
  if( t1->base() != Type::FloatCon ) return Type::FLOAT;
  float f = t1->getf();
  if( f < 0.0f ) return Type::FLOAT;
  return TypeF::make( (float)sqrt( (double)f ) );
}
