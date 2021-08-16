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

#ifndef SHARE_OPTO_NARROWPTRNODE_HPP
#define SHARE_OPTO_NARROWPTRNODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"

//------------------------------EncodeNarrowPtr--------------------------------
class EncodeNarrowPtrNode : public TypeNode {
  protected:
  EncodeNarrowPtrNode(Node* value, const Type* type):
  TypeNode(type, 2) {
    init_class_id(Class_EncodeNarrowPtr);
    init_req(0, NULL);
    init_req(1, value);
  }
  public:
  virtual uint  ideal_reg() const { return Op_RegN; }
};

//------------------------------EncodeP--------------------------------
// Encodes an oop pointers into its compressed form
// Takes an extra argument which is the real heap base as a long which
// may be useful for code generation in the backend.
class EncodePNode : public EncodeNarrowPtrNode {
  public:
  EncodePNode(Node* value, const Type* type):
  EncodeNarrowPtrNode(value, type) {
    init_class_id(Class_EncodeP);
  }
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
};

//------------------------------EncodePKlass--------------------------------
// Encodes a klass pointer into its compressed form
// Takes an extra argument which is the real heap base as a long which
// may be useful for code generation in the backend.
class EncodePKlassNode : public EncodeNarrowPtrNode {
  public:
  EncodePKlassNode(Node* value, const Type* type):
  EncodeNarrowPtrNode(value, type) {
    init_class_id(Class_EncodePKlass);
  }
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
};

//------------------------------DecodeNarrowPtr--------------------------------
class DecodeNarrowPtrNode : public TypeNode {
  protected:
  DecodeNarrowPtrNode(Node* value, const Type* type):
  TypeNode(type, 2) {
    init_class_id(Class_DecodeNarrowPtr);
    init_req(0, NULL);
    init_req(1, value);
  }
  public:
  virtual uint  ideal_reg() const { return Op_RegP; }
};

//------------------------------DecodeN--------------------------------
// Converts a narrow oop into a real oop ptr.
// Takes an extra argument which is the real heap base as a long which
// may be useful for code generation in the backend.
class DecodeNNode : public DecodeNarrowPtrNode {
  public:
  DecodeNNode(Node* value, const Type* type):
  DecodeNarrowPtrNode(value, type) {
    init_class_id(Class_DecodeN);
  }
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

//------------------------------DecodeNKlass--------------------------------
// Converts a narrow klass pointer into a real klass ptr.
// Takes an extra argument which is the real heap base as a long which
// may be useful for code generation in the backend.
class DecodeNKlassNode : public DecodeNarrowPtrNode {
  public:
  DecodeNKlassNode(Node* value, const Type* type):
  DecodeNarrowPtrNode(value, type) {
    init_class_id(Class_DecodeNKlass);
  }
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

#endif // SHARE_OPTO_NARROWPTRNODE_HPP
