/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/castnode.hpp"
#include "opto/convertnode.hpp"
#include "opto/matcher.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "runtime/sharedRuntime.hpp"

//=============================================================================
//------------------------------Identity---------------------------------------
Node* Conv2BNode::Identity(PhaseGVN* phase) {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return in(1);
  if( t == TypeInt::ZERO ) return in(1);
  if( t == TypeInt::ONE ) return in(1);
  if( t == TypeInt::BOOL ) return in(1);
  return this;
}

//------------------------------Value------------------------------------------
const Type* Conv2BNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == TypeInt::ZERO ) return TypeInt::ZERO;
  if( t == TypePtr::NULL_PTR ) return TypeInt::ZERO;
  const TypePtr *tp = t->isa_ptr();
  if( tp != NULL ) {
    if( tp->ptr() == TypePtr::AnyNull ) return Type::TOP;
    if( tp->ptr() == TypePtr::Constant) return TypeInt::ONE;
    if (tp->ptr() == TypePtr::NotNull)  return TypeInt::ONE;
    return TypeInt::BOOL;
  }
  if (t->base() != Type::Int) return TypeInt::BOOL;
  const TypeInt *ti = t->is_int();
  if( ti->_hi < 0 || ti->_lo > 0 ) return TypeInt::ONE;
  return TypeInt::BOOL;
}


// The conversions operations are all Alpha sorted.  Please keep it that way!
//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvD2FNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == Type::DOUBLE ) return Type::FLOAT;
  const TypeD *td = t->is_double_constant();
  return TypeF::make( (float)td->getd() );
}

//------------------------------Ideal------------------------------------------
// If we see pattern ConvF2D SomeDoubleOp ConvD2F, do operation as float.
Node *ConvD2FNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if ( in(1)->Opcode() == Op_SqrtD ) {
    Node* sqrtd = in(1);
    if ( sqrtd->in(1)->Opcode() == Op_ConvF2D ) {
      if ( Matcher::match_rule_supported(Op_SqrtF) ) {
        Node* convf2d = sqrtd->in(1);
        return new SqrtFNode(phase->C, sqrtd->in(0), convf2d->in(1));
      }
    }
  }
  return NULL;
}

//------------------------------Identity---------------------------------------
// Float's can be converted to doubles with no loss of bits.  Hence
// converting a float to a double and back to a float is a NOP.
Node* ConvD2FNode::Identity(PhaseGVN* phase) {
  return (in(1)->Opcode() == Op_ConvF2D) ? in(1)->in(1) : this;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvD2INode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == Type::DOUBLE ) return TypeInt::INT;
  const TypeD *td = t->is_double_constant();
  return TypeInt::make( SharedRuntime::d2i( td->getd() ) );
}

//------------------------------Ideal------------------------------------------
// If converting to an int type, skip any rounding nodes
Node *ConvD2INode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if( in(1)->Opcode() == Op_RoundDouble )
  set_req(1,in(1)->in(1));
  return NULL;
}

//------------------------------Identity---------------------------------------
// Int's can be converted to doubles with no loss of bits.  Hence
// converting an integer to a double and back to an integer is a NOP.
Node* ConvD2INode::Identity(PhaseGVN* phase) {
  return (in(1)->Opcode() == Op_ConvI2D) ? in(1)->in(1) : this;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvD2LNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == Type::DOUBLE ) return TypeLong::LONG;
  const TypeD *td = t->is_double_constant();
  return TypeLong::make( SharedRuntime::d2l( td->getd() ) );
}

//------------------------------Identity---------------------------------------
Node* ConvD2LNode::Identity(PhaseGVN* phase) {
  // Remove ConvD2L->ConvL2D->ConvD2L sequences.
  if( in(1)       ->Opcode() == Op_ConvL2D &&
     in(1)->in(1)->Opcode() == Op_ConvD2L )
  return in(1)->in(1);
  return this;
}

//------------------------------Ideal------------------------------------------
// If converting to an int type, skip any rounding nodes
Node *ConvD2LNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if( in(1)->Opcode() == Op_RoundDouble )
  set_req(1,in(1)->in(1));
  return NULL;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvF2DNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  if( t == Type::FLOAT ) return Type::DOUBLE;
  const TypeF *tf = t->is_float_constant();
  return TypeD::make( (double)tf->getf() );
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvF2INode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP )       return Type::TOP;
  if( t == Type::FLOAT ) return TypeInt::INT;
  const TypeF *tf = t->is_float_constant();
  return TypeInt::make( SharedRuntime::f2i( tf->getf() ) );
}

//------------------------------Identity---------------------------------------
Node* ConvF2INode::Identity(PhaseGVN* phase) {
  // Remove ConvF2I->ConvI2F->ConvF2I sequences.
  if( in(1)       ->Opcode() == Op_ConvI2F &&
     in(1)->in(1)->Opcode() == Op_ConvF2I )
  return in(1)->in(1);
  return this;
}

//------------------------------Ideal------------------------------------------
// If converting to an int type, skip any rounding nodes
Node *ConvF2INode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if( in(1)->Opcode() == Op_RoundFloat )
  set_req(1,in(1)->in(1));
  return NULL;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvF2LNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP )       return Type::TOP;
  if( t == Type::FLOAT ) return TypeLong::LONG;
  const TypeF *tf = t->is_float_constant();
  return TypeLong::make( SharedRuntime::f2l( tf->getf() ) );
}

//------------------------------Identity---------------------------------------
Node* ConvF2LNode::Identity(PhaseGVN* phase) {
  // Remove ConvF2L->ConvL2F->ConvF2L sequences.
  if( in(1)       ->Opcode() == Op_ConvL2F &&
     in(1)->in(1)->Opcode() == Op_ConvF2L )
  return in(1)->in(1);
  return this;
}

//------------------------------Ideal------------------------------------------
// If converting to an int type, skip any rounding nodes
Node *ConvF2LNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  if( in(1)->Opcode() == Op_RoundFloat )
  set_req(1,in(1)->in(1));
  return NULL;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvI2DNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeInt *ti = t->is_int();
  if( ti->is_con() ) return TypeD::make( (double)ti->get_con() );
  return bottom_type();
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvI2FNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeInt *ti = t->is_int();
  if( ti->is_con() ) return TypeF::make( (float)ti->get_con() );
  return bottom_type();
}

//------------------------------Identity---------------------------------------
Node* ConvI2FNode::Identity(PhaseGVN* phase) {
  // Remove ConvI2F->ConvF2I->ConvI2F sequences.
  if( in(1)       ->Opcode() == Op_ConvF2I &&
     in(1)->in(1)->Opcode() == Op_ConvI2F )
  return in(1)->in(1);
  return this;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvI2LNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeInt *ti = t->is_int();
  const Type* tl = TypeLong::make(ti->_lo, ti->_hi, ti->_widen);
  // Join my declared type against my incoming type.
  tl = tl->filter(_type);
  return tl;
}

static inline bool long_ranges_overlap(jlong lo1, jlong hi1,
                                       jlong lo2, jlong hi2) {
  // Two ranges overlap iff one range's low point falls in the other range.
  return (lo2 <= lo1 && lo1 <= hi2) || (lo1 <= lo2 && lo2 <= hi1);
}

#ifdef _LP64
// If there is an existing ConvI2L node with the given parent and type, return
// it. Otherwise, create and return a new one. Both reusing existing ConvI2L
// nodes and postponing the idealization of new ones are needed to avoid an
// explosion of recursive Ideal() calls when compiling long AddI chains.
static Node* find_or_make_convI2L(PhaseIterGVN* igvn, Node* parent,
                                  const TypeLong* type) {
  Node* n = new ConvI2LNode(parent, type);
  Node* existing = igvn->hash_find_insert(n);
  if (existing != NULL) {
    n->destruct(igvn);
    return existing;
  }
  return igvn->register_new_node_with_optimizer(n);
}
#endif

bool Compile::push_thru_add(PhaseGVN* phase, Node* z, const TypeInteger* tz, const TypeInteger*& rx, const TypeInteger*& ry,
                            BasicType bt) {
  int op = z->Opcode();
  if (op == Op_AddI || op == Op_SubI) {
    Node* x = z->in(1);
    Node* y = z->in(2);
    assert (x != z && y != z, "dead loop in ConvI2LNode::Ideal");
    if (phase->type(x) == Type::TOP) {
      return false;
    }
    if (phase->type(y) == Type::TOP) {
      return false;
    }
    const TypeInt*  tx = phase->type(x)->is_int();
    const TypeInt*  ty = phase->type(y)->is_int();

    jlong xlo = tx->is_int()->_lo;
    jlong xhi = tx->is_int()->_hi;
    jlong ylo = ty->is_int()->_lo;
    jlong yhi = ty->is_int()->_hi;
    jlong zlo = tz->lo_as_long();
    jlong zhi = tz->hi_as_long();
    jlong vbit = CONST64(1) << BitsPerInt;
    int widen =  MAX2(tx->_widen, ty->_widen);
    if (op == Op_SubI) {
      jlong ylo0 = ylo;
      ylo = -yhi;
      yhi = -ylo0;
    }
    // See if x+y can cause positive overflow into z+2**32
    if (long_ranges_overlap(xlo+ylo, xhi+yhi, zlo+vbit, zhi+vbit)) {
      return false;
    }
    // See if x+y can cause negative overflow into z-2**32
    if (long_ranges_overlap(xlo+ylo, xhi+yhi, zlo-vbit, zhi-vbit)) {
      return false;
    }
    // Now it's always safe to assume x+y does not overflow.
    // This is true even if some pairs x,y might cause overflow, as long
    // as that overflow value cannot fall into [zlo,zhi].

    // Confident that the arithmetic is "as if infinite precision",
    // we can now use z's range to put constraints on those of x and y.
    // The "natural" range of x [xlo,xhi] can perhaps be narrowed to a
    // more "restricted" range by intersecting [xlo,xhi] with the
    // range obtained by subtracting y's range from the asserted range
    // of the I2L conversion.  Here's the interval arithmetic algebra:
    //    x == z-y == [zlo,zhi]-[ylo,yhi] == [zlo,zhi]+[-yhi,-ylo]
    //    => x in [zlo-yhi, zhi-ylo]
    //    => x in [zlo-yhi, zhi-ylo] INTERSECT [xlo,xhi]
    //    => x in [xlo MAX zlo-yhi, xhi MIN zhi-ylo]
    jlong rxlo = MAX2(xlo, zlo - yhi);
    jlong rxhi = MIN2(xhi, zhi - ylo);
    // And similarly, x changing place with y:
    jlong rylo = MAX2(ylo, zlo - xhi);
    jlong ryhi = MIN2(yhi, zhi - xlo);
    if (rxlo > rxhi || rylo > ryhi) {
      return false;  // x or y is dying; don't mess w/ it
    }
    if (op == Op_SubI) {
      jlong rylo0 = rylo;
      rylo = -ryhi;
      ryhi = -rylo0;
    }
    assert(rxlo == (int)rxlo && rxhi == (int)rxhi, "x should not overflow");
    assert(rylo == (int)rylo && ryhi == (int)ryhi, "y should not overflow");
    rx = TypeInteger::make(rxlo, rxhi, widen, bt);
    ry = TypeInteger::make(rylo, ryhi, widen, bt);
    return true;
  }
  return false;
}


//------------------------------Ideal------------------------------------------
Node *ConvI2LNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  PhaseIterGVN *igvn = phase->is_IterGVN();
  const TypeLong* this_type = this->type()->is_long();
  Node* this_changed = NULL;

  if (igvn != NULL) {
    // Do NOT remove this node's type assertion until no more loop ops can happen.
    if (phase->C->post_loop_opts_phase()) {
      const TypeInt* in_type = phase->type(in(1))->isa_int();
      if (in_type != NULL && this_type != NULL &&
          (in_type->_lo != this_type->_lo ||
           in_type->_hi != this_type->_hi)) {
        // Although this WORSENS the type, it increases GVN opportunities,
        // because I2L nodes with the same input will common up, regardless
        // of slightly differing type assertions.  Such slight differences
        // arise routinely as a result of loop unrolling, so this is a
        // post-unrolling graph cleanup.  Choose a type which depends only
        // on my input.  (Exception:  Keep a range assertion of >=0 or <0.)
        jlong lo1 = this_type->_lo;
        jlong hi1 = this_type->_hi;
        int   w1  = this_type->_widen;
        if (lo1 != (jint)lo1 ||
            hi1 != (jint)hi1 ||
            lo1 > hi1) {
          // Overflow leads to wraparound, wraparound leads to range saturation.
          lo1 = min_jint; hi1 = max_jint;
        } else if (lo1 >= 0) {
          // Keep a range assertion of >=0.
          lo1 = 0;        hi1 = max_jint;
        } else if (hi1 < 0) {
          // Keep a range assertion of <0.
          lo1 = min_jint; hi1 = -1;
        } else {
          lo1 = min_jint; hi1 = max_jint;
        }
        const TypeLong* wtype = TypeLong::make(MAX2((jlong)in_type->_lo, lo1),
                                               MIN2((jlong)in_type->_hi, hi1),
                                               MAX2((int)in_type->_widen, w1));
        if (wtype != type()) {
          set_type(wtype);
          // Note: this_type still has old type value, for the logic below.
          this_changed = this;
        }
      }
    } else {
      phase->C->record_for_post_loop_opts_igvn(this);
    }
  }
#ifdef _LP64
  // Convert ConvI2L(AddI(x, y)) to AddL(ConvI2L(x), ConvI2L(y))
  // but only if x and y have subranges that cannot cause 32-bit overflow,
  // under the assumption that x+y is in my own subrange this->type().

  // This assumption is based on a constraint (i.e., type assertion)
  // established in Parse::array_addressing or perhaps elsewhere.
  // This constraint has been adjoined to the "natural" type of
  // the incoming argument in(0).  We know (because of runtime
  // checks) - that the result value I2L(x+y) is in the joined range.
  // Hence we can restrict the incoming terms (x, y) to values such
  // that their sum also lands in that range.

  // This optimization is useful only on 64-bit systems, where we hope
  // the addition will end up subsumed in an addressing mode.
  // It is necessary to do this when optimizing an unrolled array
  // copy loop such as x[i++] = y[i++].

  // On 32-bit systems, it's better to perform as much 32-bit math as
  // possible before the I2L conversion, because 32-bit math is cheaper.
  // There's no common reason to "leak" a constant offset through the I2L.
  // Addressing arithmetic will not absorb it as part of a 64-bit AddL.

  Node* z = in(1);
  const TypeInteger* rx = NULL;
  const TypeInteger* ry = NULL;
  if (Compile::push_thru_add(phase, z, this_type, rx, ry, T_LONG)) {
    if (igvn == NULL) {
      // Postpone this optimization to iterative GVN, where we can handle deep
      // AddI chains without an exponential number of recursive Ideal() calls.
      phase->record_for_igvn(this);
      return this_changed;
    }
    int op = z->Opcode();
    Node* x = z->in(1);
    Node* y = z->in(2);

    Node* cx = find_or_make_convI2L(igvn, x, rx->is_long());
    Node* cy = find_or_make_convI2L(igvn, y, ry->is_long());
    switch (op) {
      case Op_AddI:  return new AddLNode(cx, cy);
      case Op_SubI:  return new SubLNode(cx, cy);
      default:       ShouldNotReachHere();
    }
  }
#endif //_LP64

  return this_changed;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvL2DNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeLong *tl = t->is_long();
  if( tl->is_con() ) return TypeD::make( (double)tl->get_con() );
  return bottom_type();
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* ConvL2FNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeLong *tl = t->is_long();
  if( tl->is_con() ) return TypeF::make( (float)tl->get_con() );
  return bottom_type();
}

//=============================================================================
//----------------------------Identity-----------------------------------------
Node* ConvL2INode::Identity(PhaseGVN* phase) {
  // Convert L2I(I2L(x)) => x
  if (in(1)->Opcode() == Op_ConvI2L)  return in(1)->in(1);
  return this;
}

//------------------------------Value------------------------------------------
const Type* ConvL2INode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return Type::TOP;
  const TypeLong *tl = t->is_long();
  const TypeInt* ti = TypeInt::INT;
  if (tl->is_con()) {
    // Easy case.
    ti = TypeInt::make((jint)tl->get_con());
  } else if (tl->_lo >= min_jint && tl->_hi <= max_jint) {
    ti = TypeInt::make((jint)tl->_lo, (jint)tl->_hi, tl->_widen);
  }
  return ti->filter(_type);
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.
// Blow off prior masking to int
Node *ConvL2INode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node *andl = in(1);
  uint andl_op = andl->Opcode();
  if( andl_op == Op_AndL ) {
    // Blow off prior masking to int
    if( phase->type(andl->in(2)) == TypeLong::make( 0xFFFFFFFF ) ) {
      set_req_X(1,andl->in(1), phase);
      return this;
    }
  }

  // Swap with a prior add: convL2I(addL(x,y)) ==> addI(convL2I(x),convL2I(y))
  // This replaces an 'AddL' with an 'AddI'.
  if( andl_op == Op_AddL ) {
    // Don't do this for nodes which have more than one user since
    // we'll end up computing the long add anyway.
    if (andl->outcnt() > 1) return NULL;

    Node* x = andl->in(1);
    Node* y = andl->in(2);
    assert( x != andl && y != andl, "dead loop in ConvL2INode::Ideal" );
    if (phase->type(x) == Type::TOP)  return NULL;
    if (phase->type(y) == Type::TOP)  return NULL;
    Node *add1 = phase->transform(new ConvL2INode(x));
    Node *add2 = phase->transform(new ConvL2INode(y));
    return new AddINode(add1,add2);
  }

  // Disable optimization: LoadL->ConvL2I ==> LoadI.
  // It causes problems (sizes of Load and Store nodes do not match)
  // in objects initialization code and Escape Analysis.
  return NULL;
}



//=============================================================================
//------------------------------Identity---------------------------------------
// Remove redundant roundings
Node* RoundFloatNode::Identity(PhaseGVN* phase) {
  assert(Matcher::strict_fp_requires_explicit_rounding, "should only generate for Intel");
  // Do not round constants
  if (phase->type(in(1))->base() == Type::FloatCon)  return in(1);
  int op = in(1)->Opcode();
  // Redundant rounding
  if( op == Op_RoundFloat ) return in(1);
  // Already rounded
  if( op == Op_Parm ) return in(1);
  if( op == Op_LoadF ) return in(1);
  return this;
}

//------------------------------Value------------------------------------------
const Type* RoundFloatNode::Value(PhaseGVN* phase) const {
  return phase->type( in(1) );
}

//=============================================================================
//------------------------------Identity---------------------------------------
// Remove redundant roundings.  Incoming arguments are already rounded.
Node* RoundDoubleNode::Identity(PhaseGVN* phase) {
  assert(Matcher::strict_fp_requires_explicit_rounding, "should only generate for Intel");
  // Do not round constants
  if (phase->type(in(1))->base() == Type::DoubleCon)  return in(1);
  int op = in(1)->Opcode();
  // Redundant rounding
  if( op == Op_RoundDouble ) return in(1);
  // Already rounded
  if( op == Op_Parm ) return in(1);
  if( op == Op_LoadD ) return in(1);
  if( op == Op_ConvF2D ) return in(1);
  if( op == Op_ConvI2D ) return in(1);
  return this;
}

//------------------------------Value------------------------------------------
const Type* RoundDoubleNode::Value(PhaseGVN* phase) const {
  return phase->type( in(1) );
}

//=============================================================================
RoundDoubleModeNode* RoundDoubleModeNode::make(PhaseGVN& gvn, Node* arg, RoundDoubleModeNode::RoundingMode rmode) {
  ConINode* rm = gvn.intcon(rmode);
  return new RoundDoubleModeNode(arg, (Node *)rm);
}

//------------------------------Identity---------------------------------------
// Remove redundant roundings.
Node* RoundDoubleModeNode::Identity(PhaseGVN* phase) {
  int op = in(1)->Opcode();
  // Redundant rounding e.g. floor(ceil(n)) -> ceil(n)
  if(op == Op_RoundDoubleMode) return in(1);
  return this;
}
const Type* RoundDoubleModeNode::Value(PhaseGVN* phase) const {
  return Type::DOUBLE;
}
//=============================================================================
