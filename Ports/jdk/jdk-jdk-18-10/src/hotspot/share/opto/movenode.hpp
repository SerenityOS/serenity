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

#ifndef SHARE_OPTO_MOVENODE_HPP
#define SHARE_OPTO_MOVENODE_HPP

#include "opto/node.hpp"

//------------------------------CMoveNode--------------------------------------
// Conditional move
class CMoveNode : public TypeNode {
  public:
  enum { Control,               // When is it safe to do this cmove?
    Condition,             // Condition controlling the cmove
    IfFalse,               // Value if condition is false
    IfTrue };              // Value if condition is true
  CMoveNode( Node *bol, Node *left, Node *right, const Type *t ) : TypeNode(t,4)
  {
    init_class_id(Class_CMove);
    // all inputs are nullified in Node::Node(int)
    // init_req(Control,NULL);
    init_req(Condition,bol);
    init_req(IfFalse,left);
    init_req(IfTrue,right);
  }
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
  static CMoveNode *make(Node *c, Node *bol, Node *left, Node *right, const Type *t);
  // Helper function to spot cmove graph shapes
  static Node *is_cmove_id( PhaseTransform *phase, Node *cmp, Node *t, Node *f, BoolNode *b );
};

//------------------------------CMoveDNode-------------------------------------
class CMoveDNode : public CMoveNode {
  public:
  CMoveDNode( Node *bol, Node *left, Node *right, const Type* t) : CMoveNode(bol,left,right,t){}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
};

//------------------------------CMoveFNode-------------------------------------
class CMoveFNode : public CMoveNode {
  public:
  CMoveFNode( Node *bol, Node *left, Node *right, const Type* t ) : CMoveNode(bol,left,right,t) {}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
};

//------------------------------CMoveINode-------------------------------------
class CMoveINode : public CMoveNode {
  public:
  CMoveINode( Node *bol, Node *left, Node *right, const TypeInt *ti ) : CMoveNode(bol,left,right,ti){}
  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
};

//------------------------------CMoveLNode-------------------------------------
class CMoveLNode : public CMoveNode {
  public:
  CMoveLNode(Node *bol, Node *left, Node *right, const TypeLong *tl ) : CMoveNode(bol,left,right,tl){}
  virtual int Opcode() const;
};

//------------------------------CMovePNode-------------------------------------
class CMovePNode : public CMoveNode {
  public:
  CMovePNode( Node *c, Node *bol, Node *left, Node *right, const TypePtr* t ) : CMoveNode(bol,left,right,t) { init_req(Control,c); }
  virtual int Opcode() const;
};

//------------------------------CMoveNNode-------------------------------------
class CMoveNNode : public CMoveNode {
  public:
  CMoveNNode( Node *c, Node *bol, Node *left, Node *right, const Type* t ) : CMoveNode(bol,left,right,t) { init_req(Control,c); }
  virtual int Opcode() const;
};

//
class MoveNode : public Node {
  protected:
  MoveNode(Node* value) : Node(NULL, value) {
    init_class_id(Class_Move);
  }

  public:
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase);
};

class MoveI2FNode : public MoveNode {
  public:
  MoveI2FNode(Node* value) : MoveNode(value) {}
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::FLOAT; }
  virtual uint ideal_reg() const { return Op_RegF; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

class MoveL2DNode : public MoveNode {
  public:
  MoveL2DNode(Node* value) : MoveNode(value) {}
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::DOUBLE; }
  virtual uint ideal_reg() const { return Op_RegD; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

class MoveF2INode : public MoveNode {
  public:
  MoveF2INode(Node* value) : MoveNode(value) {}
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

class MoveD2LNode : public MoveNode {
  public:
  MoveD2LNode(Node* value) : MoveNode(value) {}
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeLong::LONG; }
  virtual uint ideal_reg() const { return Op_RegL; }
  virtual const Type* Value(PhaseGVN* phase) const;
  virtual Node* Identity(PhaseGVN* phase);
};

//------------------------------BinaryNode-------------------------------------
// Place holder for the 2 conditional inputs to a CMove.  CMove needs 4
// inputs: the Bool (for the lt/gt/eq/ne bits), the flags (result of some
// compare), and the 2 values to select between.  The Matcher requires a
// binary tree so we break it down like this:
//     (CMove (Binary bol cmp) (Binary src1 src2))
class BinaryNode : public Node {
  public:
  BinaryNode( Node *n1, Node *n2 ) : Node(0,n1,n2) { }
  virtual int Opcode() const;
  virtual uint ideal_reg() const { return 0; }

#ifndef PRODUCT
  virtual void related(GrowableArray<Node*> *in_rel, GrowableArray<Node*> *out_rel, bool compact) const;
#endif
};


#endif // SHARE_OPTO_MOVENODE_HPP
