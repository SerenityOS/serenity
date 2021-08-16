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

#ifndef SHARE_OPTO_COUNTBITSNODE_HPP
#define SHARE_OPTO_COUNTBITSNODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"

class PhaseTransform;

//---------- CountBitsNode -----------------------------------------------------
class CountBitsNode : public Node {
  public:
  CountBitsNode(Node* in1) : Node(0, in1) {}
  const Type* bottom_type() const { return TypeInt::INT; }
  virtual uint ideal_reg() const { return Op_RegI; }
};

//---------- CountLeadingZerosINode --------------------------------------------
// Count leading zeros (0-bit count starting from MSB) of an integer.
class CountLeadingZerosINode : public CountBitsNode {
  public:
  CountLeadingZerosINode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
};

//---------- CountLeadingZerosLNode --------------------------------------------
// Count leading zeros (0-bit count starting from MSB) of a long.
class CountLeadingZerosLNode : public CountBitsNode {
  public:
  CountLeadingZerosLNode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
};

//---------- CountTrailingZerosINode -------------------------------------------
// Count trailing zeros (0-bit count starting from LSB) of an integer.
class CountTrailingZerosINode : public CountBitsNode {
  public:
  CountTrailingZerosINode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
};

//---------- CountTrailingZerosLNode -------------------------------------------
// Count trailing zeros (0-bit count starting from LSB) of a long.
class CountTrailingZerosLNode : public CountBitsNode {
  public:
  CountTrailingZerosLNode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
  virtual const Type* Value(PhaseGVN* phase) const;
};

//---------- PopCountINode -----------------------------------------------------
// Population count (bit count) of an integer.
class PopCountINode : public CountBitsNode {
  public:
  PopCountINode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
};

//---------- PopCountLNode -----------------------------------------------------
// Population count (bit count) of a long.
class PopCountLNode : public CountBitsNode {
  public:
  PopCountLNode(Node* in1) : CountBitsNode(in1) {}
  virtual int Opcode() const;
};


#endif // SHARE_OPTO_COUNTBITSNODE_HPP
