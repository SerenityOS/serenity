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

#ifndef SHARE_OPTO_SUBTYPENODE_HPP
#define SHARE_OPTO_SUBTYPENODE_HPP

#include "opto/node.hpp"

class SubTypeCheckNode : public CmpNode {
public:
  enum {
    Control,
    ObjOrSubKlass,
    SuperKlass
  };

  SubTypeCheckNode(Compile* C, Node* obj_or_subklass, Node* superklass)
    : CmpNode(obj_or_subklass, superklass) {
    init_class_id(Class_SubTypeCheck);
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }

  Node* Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* sub(const Type*, const Type*) const;
  Node* Identity(PhaseGVN* phase) { return this; }

  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeInt::CC; }
  bool depends_only_on_test() const { return false; };

#ifdef ASSERT
private:
  bool verify(PhaseGVN* phase);
  bool verify_helper(PhaseGVN* phase, Node* subklass, const Type* cached_t);

  static bool is_oop(PhaseGVN* phase, Node* n);
#endif // ASSERT
};

#endif // SHARE_OPTO_SUBTYPENODE_HPP
