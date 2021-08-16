/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/addnode.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/movenode.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"

//=============================================================================
/*
 The major change is for CMoveP and StrComp.  They have related but slightly
 different problems.  They both take in TWO oops which are both null-checked
 independently before the using Node.  After CCP removes the CastPP's they need
 to pick up the guarding test edge - in this case TWO control edges.  I tried
 various solutions, all have problems:

 (1) Do nothing.  This leads to a bug where we hoist a Load from a CMoveP or a
 StrComp above a guarding null check.  I've seen both cases in normal -Xcomp
 testing.

 (2) Plug the control edge from 1 of the 2 oops in.  Apparent problem here is
 to figure out which test post-dominates.  The real problem is that it doesn't
 matter which one you pick.  After you pick up, the dominating-test elider in
 IGVN can remove the test and allow you to hoist up to the dominating test on
 the chosen oop bypassing the test on the not-chosen oop.  Seen in testing.
 Oops.

 (3) Leave the CastPP's in.  This makes the graph more accurate in some sense;
 we get to keep around the knowledge that an oop is not-null after some test.
 Alas, the CastPP's interfere with GVN (some values are the regular oop, some
 are the CastPP of the oop, all merge at Phi's which cannot collapse, etc).
 This cost us 10% on SpecJVM, even when I removed some of the more trivial
 cases in the optimizer.  Removing more useless Phi's started allowing Loads to
 illegally float above null checks.  I gave up on this approach.

 (4) Add BOTH control edges to both tests.  Alas, too much code knows that
 control edges are in slot-zero ONLY.  Many quick asserts fail; no way to do
 this one.  Note that I really want to allow the CMoveP to float and add both
 control edges to the dependent Load op - meaning I can select early but I
 cannot Load until I pass both tests.

 (5) Do not hoist CMoveP and StrComp.  To this end I added the v-call
 depends_only_on_test().  No obvious performance loss on Spec, but we are
 clearly conservative on CMoveP (also so on StrComp but that's unlikely to
 matter ever).

 */


//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.
// Move constants to the right.
Node *CMoveNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if( in(0) && remove_dead_region(phase, can_reshape) ) return this;
  // Don't bother trying to transform a dead node
  if( in(0) && in(0)->is_top() )  return NULL;
  assert(in(Condition) != this &&
         in(IfFalse) != this &&
         in(IfTrue) != this, "dead loop in CMoveNode::Ideal" );
  if( phase->type(in(Condition)) == Type::TOP )
  return NULL; // return NULL when Condition is dead

  if( in(IfFalse)->is_Con() && !in(IfTrue)->is_Con() ) {
    if( in(Condition)->is_Bool() ) {
      BoolNode* b  = in(Condition)->as_Bool();
      BoolNode* b2 = b->negate(phase);
      return make(in(Control), phase->transform(b2), in(IfTrue), in(IfFalse), _type);
    }
  }
  return NULL;
}

//------------------------------is_cmove_id------------------------------------
// Helper function to check for CMOVE identity.  Shared with PhiNode::Identity
Node *CMoveNode::is_cmove_id( PhaseTransform *phase, Node *cmp, Node *t, Node *f, BoolNode *b ) {
  // Check for Cmp'ing and CMove'ing same values
  if ((cmp->in(1) == f && cmp->in(2) == t) ||
      // Swapped Cmp is OK
      (cmp->in(2) == f && cmp->in(1) == t)) {
       // Give up this identity check for floating points because it may choose incorrect
       // value around 0.0 and -0.0
       if ( cmp->Opcode()==Op_CmpF || cmp->Opcode()==Op_CmpD )
       return NULL;
       // Check for "(t==f)?t:f;" and replace with "f"
       if( b->_test._test == BoolTest::eq )
       return f;
       // Allow the inverted case as well
       // Check for "(t!=f)?t:f;" and replace with "t"
       if( b->_test._test == BoolTest::ne )
       return t;
     }
  return NULL;
}

//------------------------------Identity---------------------------------------
// Conditional-move is an identity if both inputs are the same, or the test
// true or false.
Node* CMoveNode::Identity(PhaseGVN* phase) {
  // C-moving identical inputs?
  if (in(IfFalse) == in(IfTrue)) {
    return in(IfFalse); // Then it doesn't matter
  }
  if (phase->type(in(Condition)) == TypeInt::ZERO) {
    return in(IfFalse); // Always pick left(false) input
  }
  if (phase->type(in(Condition)) == TypeInt::ONE) {
    return in(IfTrue);  // Always pick right(true) input
  }

  // Check for CMove'ing a constant after comparing against the constant.
  // Happens all the time now, since if we compare equality vs a constant in
  // the parser, we "know" the variable is constant on one path and we force
  // it.  Thus code like "if( x==0 ) {/*EMPTY*/}" ends up inserting a
  // conditional move: "x = (x==0)?0:x;".  Yucko.  This fix is slightly more
  // general in that we don't need constants.
  if( in(Condition)->is_Bool() ) {
    BoolNode *b = in(Condition)->as_Bool();
    Node *cmp = b->in(1);
    if( cmp->is_Cmp() ) {
      Node *id = is_cmove_id( phase, cmp, in(IfTrue), in(IfFalse), b );
      if( id ) return id;
    }
  }

  return this;
}

//------------------------------Value------------------------------------------
// Result is the meet of inputs
const Type* CMoveNode::Value(PhaseGVN* phase) const {
  if (phase->type(in(Condition)) == Type::TOP) {
    return Type::TOP;
  }
  if (phase->type(in(IfTrue)) == Type::TOP || phase->type(in(IfFalse)) == Type::TOP) {
    return Type::TOP;
  }
  const Type* t = phase->type(in(IfFalse))->meet_speculative(phase->type(in(IfTrue)));
  return t->filter(_type);
}

//------------------------------make-------------------------------------------
// Make a correctly-flavored CMove.  Since _type is directly determined
// from the inputs we do not need to specify it here.
CMoveNode *CMoveNode::make(Node *c, Node *bol, Node *left, Node *right, const Type *t) {
  switch( t->basic_type() ) {
    case T_INT:     return new CMoveINode( bol, left, right, t->is_int() );
    case T_FLOAT:   return new CMoveFNode( bol, left, right, t );
    case T_DOUBLE:  return new CMoveDNode( bol, left, right, t );
    case T_LONG:    return new CMoveLNode( bol, left, right, t->is_long() );
    case T_OBJECT:  return new CMovePNode( c, bol, left, right, t->is_oopptr() );
    case T_ADDRESS: return new CMovePNode( c, bol, left, right, t->is_ptr() );
    case T_NARROWOOP: return new CMoveNNode( c, bol, left, right, t );
    default:
    ShouldNotReachHere();
    return NULL;
  }
}

//=============================================================================
//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.
// Check for conversions to boolean
Node *CMoveINode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Try generic ideal's first
  Node *x = CMoveNode::Ideal(phase, can_reshape);
  if( x ) return x;

  // If zero is on the left (false-case, no-move-case) it must mean another
  // constant is on the right (otherwise the shared CMove::Ideal code would
  // have moved the constant to the right).  This situation is bad for Intel
  // and a don't-care for Sparc.  It's bad for Intel because the zero has to
  // be manifested in a register with a XOR which kills flags, which are live
  // on input to the CMoveI, leading to a situation which causes excessive
  // spilling on Intel.  For Sparc, if the zero in on the left the Sparc will
  // zero a register via G0 and conditionally-move the other constant.  If the
  // zero is on the right, the Sparc will load the first constant with a
  // 13-bit set-lo and conditionally move G0.  See bug 4677505.
  if( phase->type(in(IfFalse)) == TypeInt::ZERO && !(phase->type(in(IfTrue)) == TypeInt::ZERO) ) {
    if( in(Condition)->is_Bool() ) {
      BoolNode* b  = in(Condition)->as_Bool();
      BoolNode* b2 = b->negate(phase);
      return make(in(Control), phase->transform(b2), in(IfTrue), in(IfFalse), _type);
    }
  }

  // Now check for booleans
  int flip = 0;

  // Check for picking from zero/one
  if( phase->type(in(IfFalse)) == TypeInt::ZERO && phase->type(in(IfTrue)) == TypeInt::ONE ) {
    flip = 1 - flip;
  } else if( phase->type(in(IfFalse)) == TypeInt::ONE && phase->type(in(IfTrue)) == TypeInt::ZERO ) {
  } else return NULL;

  // Check for eq/ne test
  if( !in(1)->is_Bool() ) return NULL;
  BoolNode *bol = in(1)->as_Bool();
  if( bol->_test._test == BoolTest::eq ) {
  } else if( bol->_test._test == BoolTest::ne ) {
    flip = 1-flip;
  } else return NULL;

  // Check for vs 0 or 1
  if( !bol->in(1)->is_Cmp() ) return NULL;
  const CmpNode *cmp = bol->in(1)->as_Cmp();
  if( phase->type(cmp->in(2)) == TypeInt::ZERO ) {
  } else if( phase->type(cmp->in(2)) == TypeInt::ONE ) {
    // Allow cmp-vs-1 if the other input is bounded by 0-1
    if( phase->type(cmp->in(1)) != TypeInt::BOOL )
    return NULL;
    flip = 1 - flip;
  } else return NULL;

  // Convert to a bool (flipped)
  // Build int->bool conversion
  if (PrintOpto) { tty->print_cr("CMOV to I2B"); }
  Node *n = new Conv2BNode( cmp->in(1) );
  if( flip )
  n = new XorINode( phase->transform(n), phase->intcon(1) );

  return n;
}

//=============================================================================
//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.
// Check for absolute value
Node *CMoveFNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Try generic ideal's first
  Node *x = CMoveNode::Ideal(phase, can_reshape);
  if( x ) return x;

  int  cmp_zero_idx = 0;        // Index of compare input where to look for zero
  int  phi_x_idx = 0;           // Index of phi input where to find naked x

  // Find the Bool
  if( !in(1)->is_Bool() ) return NULL;
  BoolNode *bol = in(1)->as_Bool();
  // Check bool sense
  switch( bol->_test._test ) {
    case BoolTest::lt: cmp_zero_idx = 1; phi_x_idx = IfTrue;  break;
    case BoolTest::le: cmp_zero_idx = 2; phi_x_idx = IfFalse; break;
    case BoolTest::gt: cmp_zero_idx = 2; phi_x_idx = IfTrue;  break;
    case BoolTest::ge: cmp_zero_idx = 1; phi_x_idx = IfFalse; break;
    default:           return NULL;                           break;
  }

  // Find zero input of CmpF; the other input is being abs'd
  Node *cmpf = bol->in(1);
  if( cmpf->Opcode() != Op_CmpF ) return NULL;
  Node *X = NULL;
  bool flip = false;
  if( phase->type(cmpf->in(cmp_zero_idx)) == TypeF::ZERO ) {
    X = cmpf->in(3 - cmp_zero_idx);
  } else if (phase->type(cmpf->in(3 - cmp_zero_idx)) == TypeF::ZERO) {
    // The test is inverted, we should invert the result...
    X = cmpf->in(cmp_zero_idx);
    flip = true;
  } else {
    return NULL;
  }

  // If X is found on the appropriate phi input, find the subtract on the other
  if( X != in(phi_x_idx) ) return NULL;
  int phi_sub_idx = phi_x_idx == IfTrue ? IfFalse : IfTrue;
  Node *sub = in(phi_sub_idx);

  // Allow only SubF(0,X) and fail out for all others; NegF is not OK
  if( sub->Opcode() != Op_SubF ||
     sub->in(2) != X ||
     phase->type(sub->in(1)) != TypeF::ZERO ) return NULL;

  Node *abs = new AbsFNode( X );
  if( flip )
  abs = new SubFNode(sub->in(1), phase->transform(abs));

  return abs;
}

//=============================================================================
//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.
// Check for absolute value
Node *CMoveDNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // Try generic ideal's first
  Node *x = CMoveNode::Ideal(phase, can_reshape);
  if( x ) return x;

  int  cmp_zero_idx = 0;        // Index of compare input where to look for zero
  int  phi_x_idx = 0;           // Index of phi input where to find naked x

  // Find the Bool
  if( !in(1)->is_Bool() ) return NULL;
  BoolNode *bol = in(1)->as_Bool();
  // Check bool sense
  switch( bol->_test._test ) {
    case BoolTest::lt: cmp_zero_idx = 1; phi_x_idx = IfTrue;  break;
    case BoolTest::le: cmp_zero_idx = 2; phi_x_idx = IfFalse; break;
    case BoolTest::gt: cmp_zero_idx = 2; phi_x_idx = IfTrue;  break;
    case BoolTest::ge: cmp_zero_idx = 1; phi_x_idx = IfFalse; break;
    default:           return NULL;                           break;
  }

  // Find zero input of CmpD; the other input is being abs'd
  Node *cmpd = bol->in(1);
  if( cmpd->Opcode() != Op_CmpD ) return NULL;
  Node *X = NULL;
  bool flip = false;
  if( phase->type(cmpd->in(cmp_zero_idx)) == TypeD::ZERO ) {
    X = cmpd->in(3 - cmp_zero_idx);
  } else if (phase->type(cmpd->in(3 - cmp_zero_idx)) == TypeD::ZERO) {
    // The test is inverted, we should invert the result...
    X = cmpd->in(cmp_zero_idx);
    flip = true;
  } else {
    return NULL;
  }

  // If X is found on the appropriate phi input, find the subtract on the other
  if( X != in(phi_x_idx) ) return NULL;
  int phi_sub_idx = phi_x_idx == IfTrue ? IfFalse : IfTrue;
  Node *sub = in(phi_sub_idx);

  // Allow only SubD(0,X) and fail out for all others; NegD is not OK
  if( sub->Opcode() != Op_SubD ||
     sub->in(2) != X ||
     phase->type(sub->in(1)) != TypeD::ZERO ) return NULL;

  Node *abs = new AbsDNode( X );
  if( flip )
  abs = new SubDNode(sub->in(1), phase->transform(abs));

  return abs;
}

//------------------------------MoveNode------------------------------------------

Node* MoveNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (can_reshape) {
    // Fold reinterpret cast into memory operation:
    //    MoveX2Y (LoadX mem) => LoadY mem
    LoadNode* ld = in(1)->isa_Load();
    if (ld != NULL && (ld->outcnt() == 1)) { // replace only
      const Type* rt = bottom_type();
      if (ld->has_reinterpret_variant(rt)) {
        if (phase->C->post_loop_opts_phase()) {
          return ld->convert_to_reinterpret_load(*phase, rt);
        } else {
          phase->C->record_for_post_loop_opts_igvn(this); // attempt the transformation once loop opts are over
        }
      }
    }
  }
  return NULL;
}

Node* MoveNode::Identity(PhaseGVN* phase) {
  if (in(1)->is_Move()) {
    // Back-to-back moves: MoveX2Y (MoveY2X v) => v
    assert(bottom_type() == in(1)->in(1)->bottom_type(), "sanity");
    return in(1)->in(1);
  }
  return this;
}

//------------------------------Value------------------------------------------
const Type* MoveL2DNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeLong *tl = t->is_long();
  if( !tl->is_con() ) return bottom_type();
  JavaValue v;
  v.set_jlong(tl->get_con());
  return TypeD::make( v.get_jdouble() );
}

//------------------------------Identity----------------------------------------
Node* MoveL2DNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_MoveD2L) {
    return in(1)->in(1);
  }
  return this;
}

//------------------------------Value------------------------------------------
const Type* MoveI2FNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeInt *ti = t->is_int();
  if( !ti->is_con() )   return bottom_type();
  JavaValue v;
  v.set_jint(ti->get_con());
  return TypeF::make( v.get_jfloat() );
}

//------------------------------Identity----------------------------------------
Node* MoveI2FNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_MoveF2I) {
    return in(1)->in(1);
  }
  return this;
}

//------------------------------Value------------------------------------------
const Type* MoveF2INode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP )       return Type::TOP;
  if( t == Type::FLOAT ) return TypeInt::INT;
  const TypeF *tf = t->is_float_constant();
  JavaValue v;
  v.set_jfloat(tf->getf());
  return TypeInt::make( v.get_jint() );
}

//------------------------------Identity----------------------------------------
Node* MoveF2INode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_MoveI2F) {
    return in(1)->in(1);
  }
  return this;
}

//------------------------------Value------------------------------------------
const Type* MoveD2LNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == Type::DOUBLE ) return TypeLong::LONG;
  const TypeD *td = t->is_double_constant();
  JavaValue v;
  v.set_jdouble(td->getd());
  return TypeLong::make( v.get_jlong() );
}

//------------------------------Identity----------------------------------------
Node* MoveD2LNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_MoveL2D) {
    return in(1)->in(1);
  }
  return this;
}

#ifndef PRODUCT
//----------------------------BinaryNode---------------------------------------
// The set of related nodes for a BinaryNode is all data inputs and all outputs
// till level 2 (i.e., one beyond the associated CMoveNode). In compact mode,
// it's the inputs till level 1 and the outputs till level 2.
void BinaryNode::related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const {
  if (compact) {
    this->collect_nodes(in_rel, 1, false, true);
  } else {
    this->collect_nodes_in_all_data(in_rel, false);
  }
  this->collect_nodes(out_rel, -2, false, false);
}
#endif
