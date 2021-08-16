/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/connode.hpp"
#include "opto/matcher.hpp"
#include "opto/phaseX.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"

//=============================================================================
// If input is already higher or equal to cast type, then this is an identity.
Node* ConstraintCastNode::Identity(PhaseGVN* phase) {
  Node* dom = dominating_cast(phase, phase);
  if (dom != NULL) {
    return dom;
  }
  if (_dependency != RegularDependency) {
    return this;
  }
  return phase->type(in(1))->higher_equal_speculative(_type) ? in(1) : this;
}

//------------------------------Value------------------------------------------
// Take 'join' of input and cast-up type
const Type* ConstraintCastNode::Value(PhaseGVN* phase) const {
  if (in(0) && phase->type(in(0)) == Type::TOP) return Type::TOP;
  const Type* ft = phase->type(in(1))->filter_speculative(_type);

#ifdef ASSERT
  // Previous versions of this function had some special case logic,
  // which is no longer necessary.  Make sure of the required effects.
  switch (Opcode()) {
    case Op_CastII:
    {
      const Type* t1 = phase->type(in(1));
      if( t1 == Type::TOP )  assert(ft == Type::TOP, "special case #1");
      const Type* rt = t1->join_speculative(_type);
      if (rt->empty())       assert(ft == Type::TOP, "special case #2");
      break;
    }
    case Op_CastPP:
    if (phase->type(in(1)) == TypePtr::NULL_PTR &&
        _type->isa_ptr() && _type->is_ptr()->_ptr == TypePtr::NotNull)
    assert(ft == Type::TOP, "special case #3");
    break;
  }
#endif //ASSERT

  return ft;
}

//------------------------------Ideal------------------------------------------
// Return a node which is more "ideal" than the current node.  Strip out
// control copies
Node *ConstraintCastNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  return (in(0) && remove_dead_region(phase, can_reshape)) ? this : NULL;
}

bool ConstraintCastNode::cmp(const Node &n) const {
  return TypeNode::cmp(n) && ((ConstraintCastNode&)n)._dependency == _dependency;
}

uint ConstraintCastNode::size_of() const {
  return sizeof(*this);
}

Node* ConstraintCastNode::make_cast(int opcode, Node* c, Node *n, const Type *t, DependencyType dependency) {
  switch(opcode) {
  case Op_CastII: {
    Node* cast = new CastIINode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CastLL: {
    Node* cast = new CastLLNode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CastPP: {
    Node* cast = new CastPPNode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CastFF: {
    Node* cast = new CastFFNode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CastDD: {
    Node* cast = new CastDDNode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CastVV: {
    Node* cast = new CastVVNode(n, t, dependency);
    cast->set_req(0, c);
    return cast;
  }
  case Op_CheckCastPP: return new CheckCastPPNode(c, n, t, dependency);
  default:
    fatal("Bad opcode %d", opcode);
  }
  return NULL;
}

Node* ConstraintCastNode::make(Node* c, Node *n, const Type *t, BasicType bt) {
  switch(bt) {
  case T_INT: {
    return make_cast(Op_CastII, c, n, t, RegularDependency);
  }
  case T_LONG: {
    return make_cast(Op_CastLL, c, n, t, RegularDependency);
  }
  default:
    fatal("Bad basic type %s", type2name(bt));
  }
  return NULL;
}

TypeNode* ConstraintCastNode::dominating_cast(PhaseGVN* gvn, PhaseTransform* pt) const {
  if (_dependency == UnconditionalDependency) {
    return NULL;
  }
  Node* val = in(1);
  Node* ctl = in(0);
  int opc = Opcode();
  if (ctl == NULL) {
    return NULL;
  }
  // Range check CastIIs may all end up under a single range check and
  // in that case only the narrower CastII would be kept by the code
  // below which would be incorrect.
  if (is_CastII() && as_CastII()->has_range_check()) {
    return NULL;
  }
  if (type()->isa_rawptr() && (gvn->type_or_null(val) == NULL || gvn->type(val)->isa_oopptr())) {
    return NULL;
  }
  for (DUIterator_Fast imax, i = val->fast_outs(imax); i < imax; i++) {
    Node* u = val->fast_out(i);
    if (u != this &&
        u->outcnt() > 0 &&
        u->Opcode() == opc &&
        u->in(0) != NULL &&
        u->bottom_type()->higher_equal(type())) {
      if (pt->is_dominator(u->in(0), ctl)) {
        return u->as_Type();
      }
      if (is_CheckCastPP() && u->in(1)->is_Proj() && u->in(1)->in(0)->is_Allocate() &&
          u->in(0)->is_Proj() && u->in(0)->in(0)->is_Initialize() &&
          u->in(1)->in(0)->as_Allocate()->initialization() == u->in(0)->in(0)) {
        // CheckCastPP following an allocation always dominates all
        // use of the allocation result
        return u->as_Type();
      }
    }
  }
  return NULL;
}

#ifndef PRODUCT
void ConstraintCastNode::dump_spec(outputStream *st) const {
  TypeNode::dump_spec(st);
  if (_dependency != RegularDependency) {
    st->print(" %s dependency", _dependency == StrongDependency ? "strong" : "unconditional");
  }
}
#endif

const Type* CastIINode::Value(PhaseGVN* phase) const {
  const Type *res = ConstraintCastNode::Value(phase);

  // Try to improve the type of the CastII if we recognize a CmpI/If
  // pattern.
  if (_dependency != RegularDependency) {
    if (in(0) != NULL && in(0)->in(0) != NULL && in(0)->in(0)->is_If()) {
      assert(in(0)->is_IfFalse() || in(0)->is_IfTrue(), "should be If proj");
      Node* proj = in(0);
      if (proj->in(0)->in(1)->is_Bool()) {
        Node* b = proj->in(0)->in(1);
        if (b->in(1)->Opcode() == Op_CmpI) {
          Node* cmp = b->in(1);
          if (cmp->in(1) == in(1) && phase->type(cmp->in(2))->isa_int()) {
            const TypeInt* in2_t = phase->type(cmp->in(2))->is_int();
            const Type* t = TypeInt::INT;
            BoolTest test = b->as_Bool()->_test;
            if (proj->is_IfFalse()) {
              test = test.negate();
            }
            BoolTest::mask m = test._test;
            jlong lo_long = min_jint;
            jlong hi_long = max_jint;
            if (m == BoolTest::le || m == BoolTest::lt) {
              hi_long = in2_t->_hi;
              if (m == BoolTest::lt) {
                hi_long -= 1;
              }
            } else if (m == BoolTest::ge || m == BoolTest::gt) {
              lo_long = in2_t->_lo;
              if (m == BoolTest::gt) {
                lo_long += 1;
              }
            } else if (m == BoolTest::eq) {
              lo_long = in2_t->_lo;
              hi_long = in2_t->_hi;
            } else if (m == BoolTest::ne) {
              // can't do any better
            } else {
              stringStream ss;
              test.dump_on(&ss);
              fatal("unexpected comparison %s", ss.as_string());
            }
            int lo_int = (int)lo_long;
            int hi_int = (int)hi_long;

            if (lo_long != (jlong)lo_int) {
              lo_int = min_jint;
            }
            if (hi_long != (jlong)hi_int) {
              hi_int = max_jint;
            }

            t = TypeInt::make(lo_int, hi_int, Type::WidenMax);

            res = res->filter_speculative(t);

            return res;
          }
        }
      }
    }
  }
  return res;
}

static Node* find_or_make_CastII(PhaseIterGVN* igvn, Node* parent, Node* control, const TypeInt* type, ConstraintCastNode::DependencyType dependency) {
  Node* n = new CastIINode(parent, type, dependency);
  n->set_req(0, control);
  Node* existing = igvn->hash_find_insert(n);
  if (existing != NULL) {
    n->destruct(igvn);
    return existing;
  }
  return igvn->register_new_node_with_optimizer(n);
}

Node *CastIINode::Ideal(PhaseGVN *phase, bool can_reshape) {
  Node* progress = ConstraintCastNode::Ideal(phase, can_reshape);
  if (progress != NULL) {
    return progress;
  }

  PhaseIterGVN *igvn = phase->is_IterGVN();
  const TypeInt* this_type = this->type()->is_int();
  Node* z = in(1);
  const TypeInteger* rx = NULL;
  const TypeInteger* ry = NULL;
  // Similar to ConvI2LNode::Ideal() for the same reasons
  if (!_range_check_dependency && Compile::push_thru_add(phase, z, this_type, rx, ry, T_INT)) {
    if (igvn == NULL) {
      // Postpone this optimization to iterative GVN, where we can handle deep
      // AddI chains without an exponential number of recursive Ideal() calls.
      phase->record_for_igvn(this);
      return NULL;
    }
    int op = z->Opcode();
    Node* x = z->in(1);
    Node* y = z->in(2);

    Node* cx = find_or_make_CastII(igvn, x, in(0), rx->is_int(), _dependency);
    Node* cy = find_or_make_CastII(igvn, y, in(0), ry->is_int(), _dependency);
    switch (op) {
      case Op_AddI:  return new AddINode(cx, cy);
      case Op_SubI:  return new SubINode(cx, cy);
      default:       ShouldNotReachHere();
    }
  }

  // Similar to ConvI2LNode::Ideal() for the same reasons
  // Do not narrow the type of range check dependent CastIINodes to
  // avoid corruption of the graph if a CastII is replaced by TOP but
  // the corresponding range check is not removed.
  if (can_reshape && !_range_check_dependency) {
    if (phase->C->post_loop_opts_phase()) {
      const TypeInt* this_type = this->type()->is_int();
      const TypeInt* in_type = phase->type(in(1))->isa_int();
      if (in_type != NULL && this_type != NULL &&
          (in_type->_lo != this_type->_lo ||
           in_type->_hi != this_type->_hi)) {
        jint lo1 = this_type->_lo;
        jint hi1 = this_type->_hi;
        int w1  = this_type->_widen;

        if (lo1 >= 0) {
          // Keep a range assertion of >=0.
          lo1 = 0;        hi1 = max_jint;
        } else if (hi1 < 0) {
          // Keep a range assertion of <0.
          lo1 = min_jint; hi1 = -1;
        } else {
          lo1 = min_jint; hi1 = max_jint;
        }
        const TypeInt* wtype = TypeInt::make(MAX2(in_type->_lo, lo1),
                                             MIN2(in_type->_hi, hi1),
                                             MAX2((int)in_type->_widen, w1));
        if (wtype != type()) {
          set_type(wtype);
          return this;
        }
      }
    } else {
      phase->C->record_for_post_loop_opts_igvn(this);
    }
  }
  return NULL;
}

Node* CastIINode::Identity(PhaseGVN* phase) {
  Node* progress = ConstraintCastNode::Identity(phase);
  if (progress != this) {
    return progress;
  }
  if (_range_check_dependency) {
    if (phase->C->post_loop_opts_phase()) {
      return this->in(1);
    } else {
      phase->C->record_for_post_loop_opts_igvn(this);
    }
  }
  return this;
}

bool CastIINode::cmp(const Node &n) const {
  return ConstraintCastNode::cmp(n) && ((CastIINode&)n)._range_check_dependency == _range_check_dependency;
}

uint CastIINode::size_of() const {
  return sizeof(*this);
}

#ifndef PRODUCT
void CastIINode::dump_spec(outputStream* st) const {
  ConstraintCastNode::dump_spec(st);
  if (_range_check_dependency) {
    st->print(" range check dependency");
  }
}
#endif

//=============================================================================
//------------------------------Identity---------------------------------------
// If input is already higher or equal to cast type, then this is an identity.
Node* CheckCastPPNode::Identity(PhaseGVN* phase) {
  Node* dom = dominating_cast(phase, phase);
  if (dom != NULL) {
    return dom;
  }
  if (_dependency != RegularDependency) {
    return this;
  }
  const Type* t = phase->type(in(1));
  if (EnableVectorReboxing && in(1)->Opcode() == Op_VectorBox) {
    if (t->higher_equal_speculative(phase->type(this))) {
      return in(1);
    }
  } else if (t == phase->type(this)) {
    // Toned down to rescue meeting at a Phi 3 different oops all implementing
    // the same interface.
    return in(1);
  }
  return this;
}

//------------------------------Value------------------------------------------
// Take 'join' of input and cast-up type, unless working with an Interface
const Type* CheckCastPPNode::Value(PhaseGVN* phase) const {
  if( in(0) && phase->type(in(0)) == Type::TOP ) return Type::TOP;

  const Type *inn = phase->type(in(1));
  if( inn == Type::TOP ) return Type::TOP;  // No information yet

  const TypePtr *in_type   = inn->isa_ptr();
  const TypePtr *my_type   = _type->isa_ptr();
  const Type *result = _type;
  if( in_type != NULL && my_type != NULL ) {
    TypePtr::PTR   in_ptr    = in_type->ptr();
    if (in_ptr == TypePtr::Null) {
      result = in_type;
    } else if (in_ptr == TypePtr::Constant) {
      if (my_type->isa_rawptr()) {
        result = my_type;
      } else {
        const TypeOopPtr *jptr = my_type->isa_oopptr();
        assert(jptr, "");
        result = !in_type->higher_equal(_type)
          ? my_type->cast_to_ptr_type(TypePtr::NotNull)
          : in_type;
      }
    } else {
      result =  my_type->cast_to_ptr_type( my_type->join_ptr(in_ptr) );
    }
  }

  // This is the code from TypePtr::xmeet() that prevents us from
  // having 2 ways to represent the same type. We have to replicate it
  // here because we don't go through meet/join.
  if (result->remove_speculative() == result->speculative()) {
    result = result->remove_speculative();
  }

  // Same as above: because we don't go through meet/join, remove the
  // speculative type if we know we won't use it.
  return result->cleanup_speculative();

  // JOIN NOT DONE HERE BECAUSE OF INTERFACE ISSUES.
  // FIX THIS (DO THE JOIN) WHEN UNION TYPES APPEAR!

  //
  // Remove this code after overnight run indicates no performance
  // loss from not performing JOIN at CheckCastPPNode
  //
  // const TypeInstPtr *in_oop = in->isa_instptr();
  // const TypeInstPtr *my_oop = _type->isa_instptr();
  // // If either input is an 'interface', return destination type
  // assert (in_oop == NULL || in_oop->klass() != NULL, "");
  // assert (my_oop == NULL || my_oop->klass() != NULL, "");
  // if( (in_oop && in_oop->klass()->is_interface())
  //   ||(my_oop && my_oop->klass()->is_interface()) ) {
  //   TypePtr::PTR  in_ptr = in->isa_ptr() ? in->is_ptr()->_ptr : TypePtr::BotPTR;
  //   // Preserve cast away nullness for interfaces
  //   if( in_ptr == TypePtr::NotNull && my_oop && my_oop->_ptr == TypePtr::BotPTR ) {
  //     return my_oop->cast_to_ptr_type(TypePtr::NotNull);
  //   }
  //   return _type;
  // }
  //
  // // Neither the input nor the destination type is an interface,
  //
  // // history: JOIN used to cause weird corner case bugs
  // //          return (in == TypeOopPtr::NULL_PTR) ? in : _type;
  // // JOIN picks up NotNull in common instance-of/check-cast idioms, both oops.
  // // JOIN does not preserve NotNull in other cases, e.g. RawPtr vs InstPtr
  // const Type *join = in->join(_type);
  // // Check if join preserved NotNull'ness for pointers
  // if( join->isa_ptr() && _type->isa_ptr() ) {
  //   TypePtr::PTR join_ptr = join->is_ptr()->_ptr;
  //   TypePtr::PTR type_ptr = _type->is_ptr()->_ptr;
  //   // If there isn't any NotNull'ness to preserve
  //   // OR if join preserved NotNull'ness then return it
  //   if( type_ptr == TypePtr::BotPTR  || type_ptr == TypePtr::Null ||
  //       join_ptr == TypePtr::NotNull || join_ptr == TypePtr::Constant ) {
  //     return join;
  //   }
  //   // ELSE return same old type as before
  //   return _type;
  // }
  // // Not joining two pointers
  // return join;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* CastX2PNode::Value(PhaseGVN* phase) const {
  const Type* t = phase->type(in(1));
  if (t == Type::TOP) return Type::TOP;
  if (t->base() == Type_X && t->singleton()) {
    uintptr_t bits = (uintptr_t) t->is_intptr_t()->get_con();
    if (bits == 0)   return TypePtr::NULL_PTR;
    return TypeRawPtr::make((address) bits);
  }
  return CastX2PNode::bottom_type();
}

//------------------------------Idealize---------------------------------------
static inline bool fits_in_int(const Type* t, bool but_not_min_int = false) {
  if (t == Type::TOP)  return false;
  const TypeX* tl = t->is_intptr_t();
  jint lo = min_jint;
  jint hi = max_jint;
  if (but_not_min_int)  ++lo;  // caller wants to negate the value w/o overflow
  return (tl->_lo >= lo) && (tl->_hi <= hi);
}

static inline Node* addP_of_X2P(PhaseGVN *phase,
                                Node* base,
                                Node* dispX,
                                bool negate = false) {
  if (negate) {
    dispX = phase->transform(new SubXNode(phase->MakeConX(0), dispX));
  }
  return new AddPNode(phase->C->top(),
                      phase->transform(new CastX2PNode(base)),
                      dispX);
}

Node *CastX2PNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  // convert CastX2P(AddX(x, y)) to AddP(CastX2P(x), y) if y fits in an int
  int op = in(1)->Opcode();
  Node* x;
  Node* y;
  switch (op) {
    case Op_SubX:
    x = in(1)->in(1);
    // Avoid ideal transformations ping-pong between this and AddP for raw pointers.
    if (phase->find_intptr_t_con(x, -1) == 0)
    break;
    y = in(1)->in(2);
    if (fits_in_int(phase->type(y), true)) {
      return addP_of_X2P(phase, x, y, true);
    }
    break;
    case Op_AddX:
    x = in(1)->in(1);
    y = in(1)->in(2);
    if (fits_in_int(phase->type(y))) {
      return addP_of_X2P(phase, x, y);
    }
    if (fits_in_int(phase->type(x))) {
      return addP_of_X2P(phase, y, x);
    }
    break;
  }
  return NULL;
}

//------------------------------Identity---------------------------------------
Node* CastX2PNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_CastP2X)  return in(1)->in(1);
  return this;
}

//=============================================================================
//------------------------------Value------------------------------------------
const Type* CastP2XNode::Value(PhaseGVN* phase) const {
  const Type* t = phase->type(in(1));
  if (t == Type::TOP) return Type::TOP;
  if (t->base() == Type::RawPtr && t->singleton()) {
    uintptr_t bits = (uintptr_t) t->is_rawptr()->get_con();
    return TypeX::make(bits);
  }
  return CastP2XNode::bottom_type();
}

Node *CastP2XNode::Ideal(PhaseGVN *phase, bool can_reshape) {
  return (in(0) && remove_dead_region(phase, can_reshape)) ? this : NULL;
}

//------------------------------Identity---------------------------------------
Node* CastP2XNode::Identity(PhaseGVN* phase) {
  if (in(1)->Opcode() == Op_CastX2P)  return in(1)->in(1);
  return this;
}

Node* ConstraintCastNode::make_cast_for_type(Node* c, Node* in, const Type* type, DependencyType dependency) {
  Node* cast= NULL;
  if (type->isa_int()) {
    cast = make_cast(Op_CastII, c, in, type, dependency);
  } else if (type->isa_long()) {
    cast = make_cast(Op_CastLL, c, in, type, dependency);
  } else if (type->isa_float()) {
    cast = make_cast(Op_CastFF, c, in, type, dependency);
  } else if (type->isa_double()) {
    cast = make_cast(Op_CastDD, c, in, type, dependency);
  } else if (type->isa_vect()) {
    cast = make_cast(Op_CastVV, c, in, type, dependency);
  } else if (type->isa_ptr()) {
    cast = make_cast(Op_CastPP, c, in, type, dependency);
  }
  return cast;
}
