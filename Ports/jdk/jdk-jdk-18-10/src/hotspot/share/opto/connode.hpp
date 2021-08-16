/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_CONNODE_HPP
#define SHARE_OPTO_CONNODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/type.hpp"

class PhaseTransform;
class MachNode;

//------------------------------ConNode----------------------------------------
// Simple constants
class ConNode : public TypeNode {
public:
  ConNode( const Type *t ) : TypeNode(t->remove_speculative(),1) {
    init_req(0, (Node*)Compile::current()->root());
    init_flags(Flag_is_Con);
  }
  virtual int  Opcode() const;
  virtual uint hash() const;
  virtual const RegMask &out_RegMask() const { return RegMask::Empty; }
  virtual const RegMask &in_RegMask(uint) const { return RegMask::Empty; }

  // Polymorphic factory method:
  static ConNode* make(const Type *t);
};

//------------------------------ConINode---------------------------------------
// Simple integer constants
class ConINode : public ConNode {
public:
  ConINode( const TypeInt *t ) : ConNode(t) {}
  virtual int Opcode() const;

  // Factory method:
  static ConINode* make(int con) {
    return new ConINode( TypeInt::make(con) );
  }

};

//------------------------------ConPNode---------------------------------------
// Simple pointer constants
class ConPNode : public ConNode {
public:
  ConPNode( const TypePtr *t ) : ConNode(t) {}
  virtual int Opcode() const;

  // Factory methods:
  static ConPNode* make(address con) {
    if (con == NULL)
      return new ConPNode( TypePtr::NULL_PTR ) ;
    else
      return new ConPNode( TypeRawPtr::make(con) );
  }
};


//------------------------------ConNNode--------------------------------------
// Simple narrow oop constants
class ConNNode : public ConNode {
public:
  ConNNode( const TypeNarrowOop *t ) : ConNode(t) {}
  virtual int Opcode() const;
};

//------------------------------ConNKlassNode---------------------------------
// Simple narrow klass constants
class ConNKlassNode : public ConNode {
public:
  ConNKlassNode( const TypeNarrowKlass *t ) : ConNode(t) {}
  virtual int Opcode() const;
};


//------------------------------ConLNode---------------------------------------
// Simple long constants
class ConLNode : public ConNode {
public:
  ConLNode( const TypeLong *t ) : ConNode(t) {}
  virtual int Opcode() const;

  // Factory method:
  static ConLNode* make(jlong con) {
    return new ConLNode( TypeLong::make(con) );
  }

};

//------------------------------ConFNode---------------------------------------
// Simple float constants
class ConFNode : public ConNode {
public:
  ConFNode( const TypeF *t ) : ConNode(t) {}
  virtual int Opcode() const;

  // Factory method:
  static ConFNode* make(float con) {
    return new ConFNode( TypeF::make(con) );
  }

};

//------------------------------ConDNode---------------------------------------
// Simple double constants
class ConDNode : public ConNode {
public:
  ConDNode( const TypeD *t ) : ConNode(t) {}
  virtual int Opcode() const;

  // Factory method:
  static ConDNode* make(double con) {
    return new ConDNode( TypeD::make(con) );
  }

};

//------------------------------ThreadLocalNode--------------------------------
// Ideal Node which returns the base of ThreadLocalStorage.
class ThreadLocalNode : public Node {
public:
    ThreadLocalNode( ) : Node((Node*)Compile::current()->root()) {}
    virtual int Opcode() const;
    virtual const Type *bottom_type() const { return TypeRawPtr::BOTTOM;}
    virtual uint ideal_reg() const { return Op_RegP; }
};



#endif // SHARE_OPTO_CONNODE_HPP
