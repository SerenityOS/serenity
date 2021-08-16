/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_MATHEXACTNODE_HPP
#define SHARE_OPTO_MATHEXACTNODE_HPP

#include "opto/multnode.hpp"
#include "opto/node.hpp"
#include "opto/addnode.hpp"
#include "opto/subnode.hpp"
#include "opto/type.hpp"

class PhaseGVN;
class PhaseTransform;

class OverflowNode : public CmpNode {
public:
  OverflowNode(Node* in1, Node* in2) : CmpNode(in1, in2) {}

  virtual uint ideal_reg() const { return Op_RegFlags; }
  virtual const Type* sub(const Type* t1, const Type* t2) const;
};

class OverflowINode : public OverflowNode {
public:
  typedef TypeInt TypeClass;

  OverflowINode(Node* in1, Node* in2) : OverflowNode(in1, in2) {}
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;

  virtual bool will_overflow(jint v1, jint v2) const = 0;
  virtual bool can_overflow(const Type* t1, const Type* t2) const = 0;
};


class OverflowLNode : public OverflowNode {
public:
  typedef TypeLong TypeClass;

  OverflowLNode(Node* in1, Node* in2) : OverflowNode(in1, in2) {}
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;

  virtual bool will_overflow(jlong v1, jlong v2) const = 0;
  virtual bool can_overflow(const Type* t1, const Type* t2) const = 0;
};

class OverflowAddINode : public OverflowINode {
public:
  typedef AddINode MathOp;

  OverflowAddINode(Node* in1, Node* in2) : OverflowINode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jint v1, jint v2) const;
  virtual bool can_overflow(const Type* t1, const Type* t2) const;
};

class OverflowSubINode : public OverflowINode {
public:
  typedef SubINode MathOp;

  OverflowSubINode(Node* in1, Node* in2) : OverflowINode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jint v1, jint v2) const;
  virtual bool can_overflow(const Type* t1, const Type* t2) const;
};

class OverflowMulINode : public OverflowINode {
public:
  typedef MulINode MathOp;

  OverflowMulINode(Node* in1, Node* in2) : OverflowINode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jint v1, jint v2) const;
  virtual bool can_overflow(const Type* t1, const Type* t2) const;
};

class OverflowAddLNode : public OverflowLNode {
public:
  typedef AddLNode MathOp;

  OverflowAddLNode(Node* in1, Node* in2) : OverflowLNode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jlong v1, jlong v2) const;
  virtual bool can_overflow(const Type* t1, const Type* t2) const;
};

class OverflowSubLNode : public OverflowLNode {
public:
  typedef SubLNode MathOp;

  OverflowSubLNode(Node* in1, Node* in2) : OverflowLNode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jlong v1, jlong v2) const;
  virtual bool can_overflow(const Type* t1, const Type* t2) const;
};

class OverflowMulLNode : public OverflowLNode {
public:
  typedef MulLNode MathOp;

  OverflowMulLNode(Node* in1, Node* in2) : OverflowLNode(in1, in2) {}
  virtual int Opcode() const;

  virtual bool will_overflow(jlong v1, jlong v2) const { return is_overflow(v1, v2); }
  virtual bool can_overflow(const Type* t1, const Type* t2) const;

  static bool is_overflow(jlong v1, jlong v2);
};

#endif // SHARE_OPTO_MATHEXACTNODE_HPP
