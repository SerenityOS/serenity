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

#ifndef SHARE_OPTO_MULNODE_HPP
#define SHARE_OPTO_MULNODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/type.hpp"

// Portions of code courtesy of Clifford Click

class PhaseTransform;

//------------------------------MulNode----------------------------------------
// Classic MULTIPLY functionality.  This covers all the usual 'multiply'
// behaviors for an algebraic ring.  Multiply-integer, multiply-float,
// multiply-double, and binary-and are all inherited from this class.  The
// various identity values are supplied by virtual functions.
class MulNode : public Node {
  virtual uint hash() const;
public:
  MulNode( Node *in1, Node *in2 ): Node(0,in1,in2) {
    init_class_id(Class_Mul);
  }

  // Handle algebraic identities here.  If we have an identity, return the Node
  // we are equivalent to.  We look for "add of zero" as an identity.
  virtual Node* Identity(PhaseGVN* phase);

  // We also canonicalize the Node, moving constants to the right input,
  // and flatten expressions (so that 1+x+2 becomes x+3).
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);

  // Compute a new Type for this node.  Basically we just do the pre-check,
  // then call the virtual add() to set the type.
  virtual const Type* Value(PhaseGVN* phase) const;

  // Supplied function returns the product of the inputs.
  // This also type-checks the inputs for sanity.  Guaranteed never to
  // be passed a TOP or BOTTOM type, these are filtered out by a pre-check.
  // This call recognizes the multiplicative zero type.
  virtual const Type *mul_ring( const Type *, const Type * ) const = 0;

  // Supplied function to return the multiplicative identity type
  virtual const Type *mul_id() const = 0;

  // Supplied function to return the additive identity type
  virtual const Type *add_id() const = 0;

  // Supplied function to return the additive opcode
  virtual int add_opcode() const = 0;

  // Supplied function to return the multiplicative opcode
  virtual int mul_opcode() const = 0;

  // Supplied function to return the additive opcode
  virtual int max_opcode() const = 0;

  // Supplied function to return the multiplicative opcode
  virtual int min_opcode() const = 0;
};

//------------------------------MulINode---------------------------------------
// Multiply 2 integers
class MulINode : public MulNode {
public:
  MulINode( Node *in1, Node *in2 ) : MulNode(in1,in2) {}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeInt::ONE; }
  const Type *add_id() const { return TypeInt::ZERO; }
  int add_opcode() const { return Op_AddI; }
  int mul_opcode() const { return Op_MulI; }
  int max_opcode() const { return Op_MaxI; }
  int min_opcode() const { return Op_MinI; }
  const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------MulLNode---------------------------------------
// Multiply 2 longs
class MulLNode : public MulNode {
public:
  MulLNode( Node *in1, Node *in2 ) : MulNode(in1,in2) {}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeLong::ONE; }
  const Type *add_id() const { return TypeLong::ZERO; }
  int add_opcode() const { return Op_AddL; }
  int mul_opcode() const { return Op_MulL; }
  int max_opcode() const { return Op_MaxL; }
  int min_opcode() const { return Op_MinL; }
  const Type *bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
};


//------------------------------MulFNode---------------------------------------
// Multiply 2 floats
class MulFNode : public MulNode {
public:
  MulFNode( Node *in1, Node *in2 ) : MulNode(in1,in2) {}
  virtual int Opcode() const;
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeF::ONE; }
  const Type *add_id() const { return TypeF::ZERO; }
  int add_opcode() const { return Op_AddF; }
  int mul_opcode() const { return Op_MulF; }
  int max_opcode() const { return Op_MaxF; }
  int min_opcode() const { return Op_MinF; }
  const Type *bottom_type() const { return Type::FLOAT; }
  virtual uint ideal_reg() const { return Op_RegF; }
};

//------------------------------MulDNode---------------------------------------
// Multiply 2 doubles
class MulDNode : public MulNode {
public:
  MulDNode( Node *in1, Node *in2 ) : MulNode(in1,in2) {}
  virtual int Opcode() const;
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeD::ONE; }
  const Type *add_id() const { return TypeD::ZERO; }
  int add_opcode() const { return Op_AddD; }
  int mul_opcode() const { return Op_MulD; }
  int max_opcode() const { return Op_MaxD; }
  int min_opcode() const { return Op_MinD; }
  const Type *bottom_type() const { return Type::DOUBLE; }
  virtual uint ideal_reg() const { return Op_RegD; }
};

//-------------------------------MulHiLNode------------------------------------
// Upper 64 bits of a 64 bit by 64 bit multiply
class MulHiLNode : public Node {
public:
  MulHiLNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
};

//------------------------------AndINode---------------------------------------
// Logically AND 2 integers.  Included with the MUL nodes because it inherits
// all the behavior of multiplication on a ring.
class AndINode : public MulINode {
public:
  AndINode( Node *in1, Node *in2 ) : MulINode(in1,in2) {}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeInt::MINUS_1; }
  const Type *add_id() const { return TypeInt::ZERO; }
  int add_opcode() const { return Op_OrI; }
  int mul_opcode() const { return Op_AndI; }
  int max_opcode() const { return Op_MaxI; }
  int min_opcode() const { return Op_MinI; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------AndINode---------------------------------------
// Logically AND 2 longs.  Included with the MUL nodes because it inherits
// all the behavior of multiplication on a ring.
class AndLNode : public MulLNode {
public:
  AndLNode( Node *in1, Node *in2 ) : MulLNode(in1,in2) {}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type *mul_ring( const Type *, const Type * ) const;
  const Type *mul_id() const { return TypeLong::MINUS_1; }
  const Type *add_id() const { return TypeLong::ZERO; }
  int add_opcode() const { return Op_OrL; }
  int mul_opcode() const { return Op_AndL; }
  int max_opcode() const { return Op_MaxL; }
  int min_opcode() const { return Op_MinL; }
  virtual uint ideal_reg() const { return Op_RegL; }
};

//------------------------------LShiftINode------------------------------------
// Logical shift left
class LShiftINode : public Node {
public:
  LShiftINode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------LShiftLNode------------------------------------
// Logical shift left
class LShiftLNode : public Node {
public:
  LShiftLNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
};


//------------------------ RotateLeftNode ----------------------------------
class RotateLeftNode : public TypeNode {
  public:
  RotateLeftNode(Node* in1, Node* in2, const Type* type) : TypeNode(type, 3) {
    init_req(1, in1);
    init_req(2, in2);
  }
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
};

//----------------------- RotateRightNode ----------------------------------
class RotateRightNode : public TypeNode {
  public:
  RotateRightNode(Node* in1, Node* in2, const Type* type) : TypeNode(type, 3) {
    init_req(1, in1);
    init_req(2, in2);
  }
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
};

//------------------------------RShiftINode------------------------------------
// Signed shift right
class RShiftINode : public Node {
public:
  RShiftINode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------RShiftLNode------------------------------------
// Signed shift right
class RShiftLNode : public Node {
public:
  RShiftLNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
};

//------------------------------URShiftBNode-----------------------------------
// Logical shift right
class URShiftBNode : public Node {
public:
  URShiftBNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {
    ShouldNotReachHere(); // only vector variant is used
  }
  virtual int Opcode() const;
};

//------------------------------URShiftSNode-----------------------------------
// Logical shift right
class URShiftSNode : public Node {
public:
  URShiftSNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {
    ShouldNotReachHere(); // only vector variant is used
  }
  virtual int Opcode() const;
};

//------------------------------URShiftINode-----------------------------------
// Logical shift right
class URShiftINode : public Node {
public:
  URShiftINode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------URShiftLNode-----------------------------------
// Logical shift right
class URShiftLNode : public Node {
public:
  URShiftLNode( Node *in1, Node *in2 ) : Node(0,in1,in2) {}
  virtual int Opcode() const;
  virtual Node* Identity(PhaseGVN* phase);
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  const Type *bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
};

//------------------------------FmaDNode--------------------------------------
// fused-multiply-add double
class FmaDNode : public Node {
public:
  FmaDNode(Node *c, Node *in1, Node *in2, Node *in3) : Node(c, in1, in2, in3) {}
  virtual int Opcode() const;
  const Type *bottom_type() const { return Type::DOUBLE; }
  virtual uint ideal_reg() const { return Op_RegD; }
  virtual const Type* Value(PhaseGVN* phase) const;
};

//------------------------------FmaFNode--------------------------------------
// fused-multiply-add float
class FmaFNode : public Node {
public:
  FmaFNode(Node *c, Node *in1, Node *in2, Node *in3) : Node(c, in1, in2, in3) {}
  virtual int Opcode() const;
  const Type *bottom_type() const { return Type::FLOAT; }
  virtual uint ideal_reg() const { return Op_RegF; }
  virtual const Type* Value(PhaseGVN* phase) const;
};

//------------------------------MulAddS2INode----------------------------------
// Multiply shorts into integers and add them.
// Semantics: I_OUT = S1 * S2 + S3 * S4
class MulAddS2INode : public Node {
  virtual uint hash() const;
public:
  MulAddS2INode(Node* in1, Node *in2, Node *in3, Node* in4) : Node(0, in1, in2, in3, in4) {}
  virtual int Opcode() const;
  const Type *bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

#endif // SHARE_OPTO_MULNODE_HPP
