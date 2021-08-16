/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_INTRINSICNODE_HPP
#define SHARE_OPTO_INTRINSICNODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/connode.hpp"


//----------------------PartialSubtypeCheckNode--------------------------------
// The 2nd slow-half of a subtype check.  Scan the subklass's 2ndary superklass
// array for an instance of the superklass.  Set a hidden internal cache on a
// hit (cache is checked with exposed code in gen_subtype_check()).  Return
// not zero for a miss or zero for a hit.
class PartialSubtypeCheckNode : public Node {
 public:
  PartialSubtypeCheckNode(Node* c, Node* sub, Node* super) : Node(c,sub,super) {}
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeRawPtr::BOTTOM; }
  virtual uint ideal_reg() const { return Op_RegP; }
};

//------------------------------StrIntrinsic-------------------------------
// Base class for Ideal nodes used in String intrinsic code.
class StrIntrinsicNode: public Node {
 public:
  // Possible encodings of the parameters passed to the string intrinsic.
  // 'L' stands for Latin1 and 'U' stands for UTF16. For example, 'LU' means that
  // the first string is Latin1 encoded and the second string is UTF16 encoded.
  // 'L' means that the single string is Latin1 encoded
  typedef enum ArgEncoding { LL, LU, UL, UU, L, U, none } ArgEnc;

 protected:
  // Encoding of strings. Used to select the right version of the intrinsic.
  const ArgEncoding _encoding;
  virtual uint size_of() const;

 public:
  StrIntrinsicNode(Node* control, Node* char_array_mem,
                   Node* s1, Node* c1, Node* s2, Node* c2, ArgEncoding encoding):
  Node(control, char_array_mem, s1, c1, s2, c2), _encoding(encoding) {
  }

  StrIntrinsicNode(Node* control, Node* char_array_mem,
                   Node* s1, Node* s2, Node* c, ArgEncoding encoding):
  Node(control, char_array_mem, s1, s2, c), _encoding(encoding) {
  }

  StrIntrinsicNode(Node* control, Node* char_array_mem,
                   Node* s1, Node* s2, ArgEncoding encoding):
  Node(control, char_array_mem, s1, s2), _encoding(encoding) {
  }

  virtual bool depends_only_on_test() const { return false; }
  virtual const TypePtr* adr_type() const { return TypeAryPtr::BYTES; }
  virtual uint match_edge(uint idx) const;
  virtual uint ideal_reg() const { return Op_RegI; }
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
  ArgEncoding encoding() const { return _encoding; }
};

//------------------------------StrComp-------------------------------------
class StrCompNode: public StrIntrinsicNode {
 public:
  StrCompNode(Node* control, Node* char_array_mem,
              Node* s1, Node* c1, Node* s2, Node* c2, ArgEncoding encoding):
  StrIntrinsicNode(control, char_array_mem, s1, c1, s2, c2, encoding) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::INT; }
};

//------------------------------StrEquals-------------------------------------
class StrEqualsNode: public StrIntrinsicNode {
 public:
  StrEqualsNode(Node* control, Node* char_array_mem,
                Node* s1, Node* s2, Node* c, ArgEncoding encoding):
  StrIntrinsicNode(control, char_array_mem, s1, s2, c, encoding) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::BOOL; }
};

//------------------------------StrIndexOf-------------------------------------
class StrIndexOfNode: public StrIntrinsicNode {
 public:
  StrIndexOfNode(Node* control, Node* char_array_mem,
                 Node* s1, Node* c1, Node* s2, Node* c2, ArgEncoding encoding):
  StrIntrinsicNode(control, char_array_mem, s1, c1, s2, c2, encoding) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::INT; }
};

//------------------------------StrIndexOfChar-------------------------------------
class StrIndexOfCharNode: public StrIntrinsicNode {
 public:
  StrIndexOfCharNode(Node* control, Node* char_array_mem,
                     Node* s1, Node* c1, Node* c, ArgEncoding encoding):
  StrIntrinsicNode(control, char_array_mem, s1, c1, c, encoding) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::INT; }
};

//--------------------------StrCompressedCopy-------------------------------
class StrCompressedCopyNode: public StrIntrinsicNode {
 public:
  StrCompressedCopyNode(Node* control, Node* arymem,
                        Node* s1, Node* s2, Node* c):
  StrIntrinsicNode(control, arymem, s1, s2, c, none) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::INT; }
  virtual const TypePtr* adr_type() const { return TypePtr::BOTTOM; }
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
};

//--------------------------StrInflatedCopy---------------------------------
class StrInflatedCopyNode: public StrIntrinsicNode {
 public:
  StrInflatedCopyNode(Node* control, Node* arymem,
                      Node* s1, Node* s2, Node* c):
  StrIntrinsicNode(control, arymem, s1, s2, c, none) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::MEMORY; }
  virtual const TypePtr* adr_type() const { return TypePtr::BOTTOM; }
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
};

//------------------------------AryEq---------------------------------------
class AryEqNode: public StrIntrinsicNode {
 public:
  AryEqNode(Node* control, Node* char_array_mem,
            Node* s1, Node* s2, ArgEncoding encoding):
  StrIntrinsicNode(control, char_array_mem, s1, s2, encoding) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::BOOL; }
};

//------------------------------HasNegatives---------------------------------
class HasNegativesNode: public StrIntrinsicNode {
 public:
  HasNegativesNode(Node* control, Node* char_array_mem, Node* s1, Node* c1):
  StrIntrinsicNode(control, char_array_mem, s1, c1, none) {};
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return TypeInt::BOOL; }
};


//------------------------------EncodeISOArray--------------------------------
// encode char[] to byte[] in ISO_8859_1
class EncodeISOArrayNode: public Node {
 public:
  EncodeISOArrayNode(Node* control, Node* arymem, Node* s1, Node* s2, Node* c): Node(control, arymem, s1, s2, c) {};
  virtual int Opcode() const;
  virtual bool depends_only_on_test() const { return false; }
  virtual const Type* bottom_type() const { return TypeInt::INT; }
  virtual const TypePtr* adr_type() const { return TypePtr::BOTTOM; }
  virtual uint match_edge(uint idx) const;
  virtual uint ideal_reg() const { return Op_RegI; }
  virtual Node* Ideal(PhaseGVN* phase, bool can_reshape);
  virtual const Type* Value(PhaseGVN* phase) const;
};

//-------------------------------DigitNode----------------------------------------
class DigitNode : public Node {
public:
  DigitNode(Node* control, Node *in1) : Node(control, in1) {}
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeInt::BOOL; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------LowerCaseNode------------------------------------
class LowerCaseNode : public Node {
public:
  LowerCaseNode(Node* control, Node *in1) : Node(control, in1) {}
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeInt::BOOL; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------UpperCaseNode------------------------------------
class UpperCaseNode : public Node {
public:
  UpperCaseNode(Node* control, Node *in1) : Node(control, in1) {}
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeInt::BOOL; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------WhitespaceCode-----------------------------------
class WhitespaceNode : public Node {
public:
  WhitespaceNode(Node* control, Node *in1) : Node(control, in1) {}
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeInt::BOOL; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//------------------------------CopySign-----------------------------------------
class CopySignDNode : public Node {
 protected:
  CopySignDNode(Node* in1, Node* in2, Node* in3) : Node(0, in1, in2, in3) {}
 public:
  static CopySignDNode* make(PhaseGVN& gvn, Node* in1, Node* in2);
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeLong::DOUBLE; }
  virtual uint ideal_reg() const { return Op_RegD; }
};

class CopySignFNode : public Node {
 public:
  CopySignFNode(Node* in1, Node* in2) : Node(0, in1, in2) {}
  virtual int Opcode() const;
  const Type* bottom_type() const { return TypeLong::FLOAT; }
  virtual uint ideal_reg() const { return Op_RegF; }
};

//------------------------------Signum-------------------------------------------
class SignumDNode : public Node {
 protected:
  SignumDNode(Node* in1, Node* in2, Node* in3) : Node(0, in1, in2, in3) {}
 public:
  static SignumDNode* make(PhaseGVN& gvn, Node* in);
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::DOUBLE; }
  virtual uint ideal_reg() const { return Op_RegD; }
};

class SignumFNode : public Node {
 protected:
  SignumFNode(Node* in1, Node* in2, Node* in3) : Node(0, in1, in2, in3) {}
 public:
  static SignumFNode* make(PhaseGVN& gvn, Node* in);
  virtual int Opcode() const;
  virtual const Type* bottom_type() const { return Type::FLOAT; }
  virtual uint ideal_reg() const { return Op_RegF; }
};

#endif // SHARE_OPTO_INTRINSICNODE_HPP
