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
#include "opto/narrowptrnode.hpp"
#include "opto/phaseX.hpp"

Node* DecodeNNode::Identity(PhaseGVN* phase) {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return in(1);

  if (in(1)->is_EncodeP()) {
    // (DecodeN (EncodeP p)) -> p
    return in(1)->in(1);
  }
  return this;
}

const Type* DecodeNNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if (t == Type::TOP) return Type::TOP;
  if (t == TypeNarrowOop::NULL_PTR) return TypePtr::NULL_PTR;

  assert(t->isa_narrowoop(), "only  narrowoop here");
  return t->make_ptr();
}

Node* EncodePNode::Identity(PhaseGVN* phase) {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return in(1);

  if (in(1)->is_DecodeN()) {
    // (EncodeP (DecodeN p)) -> p
    return in(1)->in(1);
  }
  return this;
}

const Type* EncodePNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if (t == Type::TOP) return Type::TOP;
  if (t == TypePtr::NULL_PTR) return TypeNarrowOop::NULL_PTR;

  assert(t->isa_oop_ptr(), "only oopptr here");
  return t->make_narrowoop();
}


Node* DecodeNKlassNode::Identity(PhaseGVN* phase) {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return in(1);

  if (in(1)->is_EncodePKlass()) {
    // (DecodeNKlass (EncodePKlass p)) -> p
    return in(1)->in(1);
  }
  return this;
}

const Type* DecodeNKlassNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if (t == Type::TOP) return Type::TOP;
  assert(t != TypeNarrowKlass::NULL_PTR, "null klass?");

  assert(t->isa_narrowklass(), "only narrow klass ptr here");
  return t->make_ptr();
}

Node* EncodePKlassNode::Identity(PhaseGVN* phase) {
  const Type *t = phase->type( in(1) );
  if( t == Type::TOP ) return in(1);

  if (in(1)->is_DecodeNKlass()) {
    // (EncodePKlass (DecodeNKlass p)) -> p
    return in(1)->in(1);
  }
  return this;
}

const Type* EncodePKlassNode::Value(PhaseGVN* phase) const {
  const Type *t = phase->type( in(1) );
  if (t == Type::TOP) return Type::TOP;
  assert (t != TypePtr::NULL_PTR, "null klass?");

  assert(UseCompressedClassPointers && t->isa_klassptr(), "only klass ptr here");
  return t->make_narrowklass();
}

