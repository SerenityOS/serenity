/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"
#include "opto/subnode.hpp"
#include "opto/subtypenode.hpp"

const Type* SubTypeCheckNode::sub(const Type* sub_t, const Type* super_t) const {
  ciKlass* superk = super_t->is_klassptr()->klass();
  ciKlass* subk   = sub_t->isa_klassptr() ? sub_t->is_klassptr()->klass() : sub_t->is_oopptr()->klass();

  bool xsubk = sub_t->isa_klassptr() ? sub_t->is_klassptr()->klass_is_exact() : sub_t->is_oopptr()->klass_is_exact();


  // Oop can't be a subtype of abstract type that has no subclass.
  if (sub_t->isa_oopptr() && superk->is_instance_klass() &&
      !superk->is_interface() && superk->is_abstract() &&
      !superk->as_instance_klass()->has_subklass()) {
    Compile::current()->dependencies()->assert_leaf_type(superk);
    return TypeInt::CC_GT;
  }

  // Similar to logic in CmpPNode::sub()

  // Interfaces can't be trusted unless the subclass is an exact
  // interface (it can then only be a constant) or the subclass is an
  // exact array of interfaces (a newly allocated array of interfaces
  // for instance)
  if (superk && subk &&
      superk->is_loaded() && !superk->is_interface() &&
      subk->is_loaded() && (!subk->is_interface() || xsubk) &&
      (!superk->is_obj_array_klass() ||
       !superk->as_obj_array_klass()->base_element_klass()->is_interface()) &&
      (!subk->is_obj_array_klass() ||
       !subk->as_obj_array_klass()->base_element_klass()->is_interface() ||
       xsubk)) {
    bool unrelated_classes = false;
    if (superk->equals(subk)) {
      // skip
    } else if (superk->is_subtype_of(subk)) {
      // If the subclass is exact then the superclass is a subtype of
      // the subclass. Given they're no equals, that subtype check can
      // only fail.
      unrelated_classes = xsubk;
    } else if (subk->is_subtype_of(superk)) {
      // skip
    } else {
      // Neither class subtypes the other: they are unrelated and this
      // type check is known to fail.
      unrelated_classes = true;
    }
    if (unrelated_classes) {
      TypePtr::PTR jp = sub_t->is_ptr()->join_ptr(super_t->is_ptr()->_ptr);
      if (jp != TypePtr::Null && jp != TypePtr::BotPTR) {
        return TypeInt::CC_GT;
      }
    }
  }

  if (super_t->singleton()) {
    if (subk != NULL) {
      switch (Compile::current()->static_subtype_check(superk, subk)) {
      case Compile::SSC_always_false:
        return TypeInt::CC_GT;
      case Compile::SSC_always_true:
        return TypeInt::CC_EQ;
      case Compile::SSC_easy_test:
      case Compile::SSC_full_test:
        break;
      default:
        ShouldNotReachHere();
      }
    }
  }

  return bottom_type();
}

Node *SubTypeCheckNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* obj_or_subklass = in(ObjOrSubKlass);
  Node* superklass = in(SuperKlass);

  if (obj_or_subklass == NULL ||
      superklass == NULL) {
    return NULL;
  }

  const Type* sub_t = phase->type(obj_or_subklass);
  const Type* super_t = phase->type(superklass);

  if (!super_t->isa_klassptr() ||
      (!sub_t->isa_klassptr() && !sub_t->isa_oopptr())) {
    return NULL;
  }

  Node* addr = NULL;
  if (obj_or_subklass->is_DecodeNKlass()) {
    if (obj_or_subklass->in(1) != NULL &&
        obj_or_subklass->in(1)->Opcode() == Op_LoadNKlass) {
      addr = obj_or_subklass->in(1)->in(MemNode::Address);
    }
  } else if (obj_or_subklass->Opcode() == Op_LoadKlass) {
    addr = obj_or_subklass->in(MemNode::Address);
  }

  if (addr != NULL) {
    intptr_t con = 0;
    Node* obj = AddPNode::Ideal_base_and_offset(addr, phase, con);
    if (con == oopDesc::klass_offset_in_bytes() && obj != NULL) {
      assert(is_oop(phase, obj), "only for oop input");
      set_req(ObjOrSubKlass, obj);
      return this;
    }
  }

  // AllocateNode might have more accurate klass input
  Node* allocated_klass = AllocateNode::Ideal_klass(obj_or_subklass, phase);
  if (allocated_klass != NULL) {
    assert(is_oop(phase, obj_or_subklass), "only for oop input");
    set_req(ObjOrSubKlass, allocated_klass);
    return this;
  }

  // Verify that optimizing the subtype check to a simple code pattern
  // when possible would not constant fold better
  assert(verify(phase), "missing Value() optimization");

  return NULL;
}

#ifdef ASSERT
bool SubTypeCheckNode::is_oop(PhaseGVN* phase, Node* n) {
    const Type* t = phase->type(n);
    if (!t->isa_oopptr() && t != Type::TOP) {
      n->dump();
      t->dump(); tty->cr();
      return false;
    }
    return true;
}

static Node* record_for_cleanup(Node* n, PhaseGVN* phase) {
  if (phase->is_IterGVN()) {
    phase->is_IterGVN()->_worklist.push(n); // record for cleanup
  }
  return n;
}
bool SubTypeCheckNode::verify_helper(PhaseGVN* phase, Node* subklass, const Type* cached_t) {
  Node* cmp = phase->transform(new CmpPNode(subklass, in(SuperKlass)));
  record_for_cleanup(cmp, phase);

  const Type* cmp_t = phase->type(cmp);
  const Type* t = Value(phase);

  if (t == cmp_t ||
      t != cached_t || // previous observations don't hold anymore
      (cmp_t != TypeInt::CC_GT && cmp_t != TypeInt::CC_EQ)) {
    return true;
  } else {
    t->dump(); tty->cr();
    this->dump(2); tty->cr();
    cmp_t->dump(); tty->cr();
    subklass->dump(2); tty->cr();
    tty->print_cr("==============================");
    phase->C->root()->dump(9999);
    return false;
  }
}

// Verify that optimizing the subtype check to a simple code pattern when possible would not constant fold better.
bool SubTypeCheckNode::verify(PhaseGVN* phase) {
  Compile* C = phase->C;
  Node* obj_or_subklass = in(ObjOrSubKlass);
  Node* superklass = in(SuperKlass);

  const Type* sub_t = phase->type(obj_or_subklass);
  const Type* super_t = phase->type(superklass);

  ciKlass* subk = sub_t->isa_klassptr() ? sub_t->is_klassptr()->klass() : sub_t->is_oopptr()->klass(); // can be NULL for bottom[]
  ciKlass* superk = super_t->is_klassptr()->klass();

  if (super_t->singleton() && subk != NULL) {
    Node* subklass = NULL;
    if (sub_t->isa_oopptr()) {
      Node* adr = phase->transform(new AddPNode(obj_or_subklass, obj_or_subklass, phase->MakeConX(oopDesc::klass_offset_in_bytes())));
      subklass  = phase->transform(LoadKlassNode::make(*phase, NULL, C->immutable_memory(), adr, TypeInstPtr::KLASS));
      record_for_cleanup(subklass, phase);
    } else {
      subklass = obj_or_subklass;
    }

    const Type* cached_t = Value(phase); // cache the type to validate consistency
    switch (C->static_subtype_check(superk, subk)) {
      case Compile::SSC_easy_test: {
        return verify_helper(phase, subklass, cached_t);
      }
      case Compile::SSC_full_test: {
        Node* p1 = phase->transform(new AddPNode(superklass, superklass, phase->MakeConX(in_bytes(Klass::super_check_offset_offset()))));
        Node* chk_off = phase->transform(new LoadINode(NULL, C->immutable_memory(), p1, phase->type(p1)->is_ptr(), TypeInt::INT, MemNode::unordered));
        record_for_cleanup(chk_off, phase);

        int cacheoff_con = in_bytes(Klass::secondary_super_cache_offset());
        bool might_be_cache = (phase->find_int_con(chk_off, cacheoff_con) == cacheoff_con);
        if (!might_be_cache) {
          Node* chk_off_X = chk_off;
#ifdef _LP64
          chk_off_X = phase->transform(new ConvI2LNode(chk_off_X));
#endif
          Node* p2 = phase->transform(new AddPNode(subklass, subklass, chk_off_X));
          Node* nkls = phase->transform(LoadKlassNode::make(*phase, NULL, C->immutable_memory(), p2, phase->type(p2)->is_ptr(), TypeKlassPtr::OBJECT_OR_NULL));

          return verify_helper(phase, nkls, cached_t);
        }
        break;
      }
      case Compile::SSC_always_false:
      case Compile::SSC_always_true:
      default: {
        break; // nothing to do
      }
    }
  }

  return true;
}
#endif
