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

#ifndef SHARE_OPTO_OPAQUENODE_HPP
#define SHARE_OPTO_OPAQUENODE_HPP

#include "opto/node.hpp"
#include "opto/opcodes.hpp"

//------------------------------Opaque1Node------------------------------------
// A node to prevent unwanted optimizations.  Allows constant folding.
// Stops value-numbering, Ideal calls or Identity functions.
class Opaque1Node : public Node {
  virtual uint hash() const ;                  // { return NO_HASH; }
  virtual bool cmp( const Node &n ) const;
  public:
  Opaque1Node(Compile* C, Node *n) : Node(NULL, n) {
    // Put it on the Macro nodes list to removed during macro nodes expansion.
    init_flags(Flag_is_macro);
    init_class_id(Class_Opaque1);
    C->add_macro_node(this);
  }
  // Special version for the pre-loop to hold the original loop limit
  // which is consumed by range check elimination.
  Opaque1Node(Compile* C, Node *n, Node* orig_limit) : Node(NULL, n, orig_limit) {
    // Put it on the Macro nodes list to removed during macro nodes expansion.
    init_flags(Flag_is_macro);
    init_class_id(Class_Opaque1);
    C->add_macro_node(this);
  }
  Node* original_loop_limit() { return req()==3 ? in(2) : NULL; }
  virtual int Opcode() const;
  virtual const Type *bottom_type() const { return TypeInt::INT; }
  virtual Node* Identity(PhaseGVN* phase);
};

// Opaque nodes specific to range check elimination handling
class OpaqueLoopInitNode : public Opaque1Node {
  public:
  OpaqueLoopInitNode(Compile* C, Node *n) : Opaque1Node(C, n) {
  }
  virtual int Opcode() const;
};

class OpaqueLoopStrideNode : public Opaque1Node {
  public:
  OpaqueLoopStrideNode(Compile* C, Node *n) : Opaque1Node(C, n) {
  }
  virtual int Opcode() const;
};

//------------------------------Opaque2Node------------------------------------
// A node to prevent unwanted optimizations.  Allows constant folding.  Stops
// value-numbering, most Ideal calls or Identity functions.  This Node is
// specifically designed to prevent the pre-increment value of a loop trip
// counter from being live out of the bottom of the loop (hence causing the
// pre- and post-increment values both being live and thus requiring an extra
// temp register and an extra move).  If we "accidentally" optimize through
// this kind of a Node, we'll get slightly pessimal, but correct, code.  Thus
// it's OK to be slightly sloppy on optimizations here.
class Opaque2Node : public Node {
  virtual uint hash() const ;                  // { return NO_HASH; }
  virtual bool cmp( const Node &n ) const;
  public:
  Opaque2Node( Compile* C, Node *n ) : Node(0,n) {
    // Put it on the Macro nodes list to removed during macro nodes expansion.
    init_flags(Flag_is_macro);
    C->add_macro_node(this);
  }
  virtual int Opcode() const;
  virtual const Type *bottom_type() const { return TypeInt::INT; }
};

//------------------------------Opaque3Node------------------------------------
// A node to prevent unwanted optimizations. Will be optimized only during
// macro nodes expansion.
class Opaque3Node : public Opaque2Node {
  int _opt; // what optimization it was used for
  public:
  enum { RTM_OPT };
  Opaque3Node(Compile* C, Node *n, int opt) : Opaque2Node(C, n), _opt(opt) {}
  virtual int Opcode() const;
  bool rtm_opt() const { return (_opt == RTM_OPT); }
};

// Input 1 is a check that we know implicitly is always true or false
// but the compiler has no way to prove. If during optimizations, that
// check becomes true or false, the Opaque4 node is replaced by that
// constant true or false. Input 2 is the constant value we know the
// test takes. After loop optimizations, we replace input 1 by input 2
// so the control that depends on that test can be removed and there's
// no overhead at runtime. Used for instance by
// GraphKit::must_be_not_null().
class Opaque4Node : public Node {
  public:
  Opaque4Node(Compile* C, Node *tst, Node* final_tst) : Node(NULL, tst, final_tst) {}

  virtual int Opcode() const;
  virtual const Type *bottom_type() const { return TypeInt::BOOL; }
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type* Value(PhaseGVN* phase) const;
};


//------------------------------ProfileBooleanNode-------------------------------
// A node represents value profile for a boolean during parsing.
// Once parsing is over, the node goes away (during IGVN).
// It is used to override branch frequencies from MDO (see has_injected_profile in parse2.cpp).
class ProfileBooleanNode : public Node {
  uint _false_cnt;
  uint _true_cnt;
  bool _consumed;
  bool _delay_removal;
  virtual uint hash() const ;                  // { return NO_HASH; }
  virtual bool cmp( const Node &n ) const;
  public:
  ProfileBooleanNode(Node *n, uint false_cnt, uint true_cnt) : Node(0, n),
          _false_cnt(false_cnt), _true_cnt(true_cnt), _consumed(false), _delay_removal(true) {}

  uint false_count() const { return _false_cnt; }
  uint  true_count() const { return  _true_cnt; }

  void consume() { _consumed = true;  }

  virtual int Opcode() const;
  virtual Node *Ideal(PhaseGVN *phase, bool can_reshape);
  virtual Node* Identity(PhaseGVN* phase);
  virtual const Type *bottom_type() const { return TypeInt::BOOL; }
};

#endif // SHARE_OPTO_OPAQUENODE_HPP
