/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_OPTOREG_HPP
#define SHARE_OPTO_OPTOREG_HPP

#include "utilities/macros.hpp"

// AdGlobals contains c2 specific register handling code as specified
// in the .ad files.
#include CPU_HEADER(adfiles/adGlobals)

//------------------------------OptoReg----------------------------------------
// We eventually need Registers for the Real World.  Registers are essentially
// non-SSA names.  A Register is represented as a number.  Non-regular values
// (e.g., Control, Memory, I/O) use the Special register.  The actual machine
// registers (as described in the ADL file for a machine) start at zero.
// Stack-slots (spill locations) start at the next Chunk past the last machine
// register.
//
// Note that stack spill-slots are treated as a very large register set.
// They have all the correct properties for a Register: not aliased (unique
// named).  There is some simple mapping from a stack-slot register number
// to the actual location on the stack; this mapping depends on the calling
// conventions and is described in the ADL.
//
// Note that Name is not enum. C++ standard defines that the range of enum
// is the range of smallest bit-field that can represent all enumerators
// declared in the enum. The result of assigning a value to enum is undefined
// if the value is outside the enumeration's valid range. OptoReg::Name is
// typedef'ed as int, because it needs to be able to represent spill-slots.
//
class OptoReg {

 friend class C2Compiler;
 public:
  typedef int Name;
  enum {
    // Chunk 0
    Physical = AdlcVMDeps::Physical, // Start of physical regs
    // A few oddballs at the edge of the world
    Special = -2,               // All special (not allocated) values
    Bad = -1                    // Not a register
  };

 private:

 static const VMReg opto2vm[REG_COUNT];
 static Name vm2opto[ConcreteRegisterImpl::number_of_registers];

 public:

  // Stack pointer register
  static OptoReg::Name c_frame_pointer;



  // Increment a register number.  As in:
  //    "for ( OptoReg::Name i; i=Control; i = add(i,1) ) ..."
  static Name add( Name x, int y ) { return Name(x+y); }

  // (We would like to have an operator+ for RegName, but it is not
  // a class, so this would be illegal in C++.)

  static void dump(int, outputStream *st = tty);

  // Get the stack slot number of an OptoReg::Name
  static unsigned int reg2stack( OptoReg::Name r) {
    assert( r >= stack0(), " must be");
    return r - stack0();
  }

  static void invalidate(Name n) {
    vm2opto[n] = Bad;
  }

  // convert a stack slot number into an OptoReg::Name
  static OptoReg::Name stack2reg( int idx) {
    return Name(stack0() + idx);
  }

  static bool is_stack(Name n) {
    return n >= stack0();
  }

  static bool is_valid(Name n) {
    return (n != Bad);
  }

  static bool is_reg(Name n) {
    return  is_valid(n) && !is_stack(n);
  }

  static VMReg as_VMReg(OptoReg::Name n) {
    if (is_reg(n)) {
      // Must use table, it'd be nice if Bad was indexable...
      return opto2vm[n];
    } else {
      assert(!is_stack(n), "must un warp");
      return VMRegImpl::Bad();
    }
  }

  // Can un-warp a stack slot or convert a register or Bad
  static VMReg as_VMReg(OptoReg::Name n, int frame_size, int arg_count) {
    if (is_reg(n)) {
      // Must use table, it'd be nice if Bad was indexable...
      return opto2vm[n];
    } else if (is_stack(n)) {
      int stack_slot = reg2stack(n);
      if (stack_slot < arg_count) {
        return VMRegImpl::stack2reg(stack_slot + frame_size);
      }
      return VMRegImpl::stack2reg(stack_slot - arg_count);
      // return return VMRegImpl::stack2reg(reg2stack(OptoReg::add(n, -arg_count)));
    } else {
      return VMRegImpl::Bad();
    }
  }

  static OptoReg::Name as_OptoReg(VMReg r) {
    if (r->is_stack()) {
      assert(false, "must warp");
      return stack2reg(r->reg2stack());
    } else if (r->is_valid()) {
      // Must use table, it'd be nice if Bad was indexable...
      return vm2opto[r->value()];
    } else {
      return Bad;
    }
  }

  static OptoReg::Name stack0() {
    return VMRegImpl::stack0->value();
  }

  static const char* regname(OptoReg::Name n) {
    return as_VMReg(n)->name();
  }

};

//---------------------------OptoRegPair-------------------------------------------
// Pairs of 32-bit registers for the allocator.
// This is a very similar class to VMRegPair. C2 only interfaces with VMRegPair
// via the calling convention code which is shared between the compilers.
// Since C2 uses OptoRegs for register allocation it is more efficient to use
// VMRegPair internally for nodes that can contain a pair of OptoRegs rather
// than use VMRegPair and continually be converting back and forth. So normally
// C2 will take in a VMRegPair from the calling convention code and immediately
// convert them to an OptoRegPair and stay in the OptoReg world. The only over
// conversion between OptoRegs and VMRegs is for debug info and oopMaps. This
// is not a high bandwidth spot and so it is not an issue.
// Note that onde other consequence of staying in the OptoReg world with OptoRegPairs
// is that there are "physical" OptoRegs that are not representable in the VMReg
// world, notably flags. [ But by design there is "space" in the VMReg world
// for such registers they just may not be concrete ]. So if we were to use VMRegPair
// then the VMReg world would have to have a representation for these registers
// so that a OptoReg->VMReg->OptoReg would reproduce ther original OptoReg. As it
// stands if you convert a flag (condition code) to a VMReg you will get VMRegImpl::Bad
// and converting that will return OptoReg::Bad losing the identity of the OptoReg.

class OptoRegPair {
  friend class VMStructs;
private:
  short _second;
  short _first;
public:
  void set_bad (                   ) { _second = OptoReg::Bad; _first = OptoReg::Bad; }
  void set1    ( OptoReg::Name n  ) { _second = OptoReg::Bad; _first = n; }
  void set2    ( OptoReg::Name n  ) { _second = n + 1;       _first = n; }
  void set_pair( OptoReg::Name second, OptoReg::Name first    ) { _second= second;    _first= first; }
  void set_ptr ( OptoReg::Name ptr ) {
#ifdef _LP64
    _second = ptr+1;
#else
    _second = OptoReg::Bad;
#endif
    _first = ptr;
  }

  OptoReg::Name second() const { return _second; }
  OptoReg::Name first() const { return _first; }
  OptoRegPair(OptoReg::Name second, OptoReg::Name first) {  _second = second; _first = first; }
  OptoRegPair(OptoReg::Name f) { _second = OptoReg::Bad; _first = f; }
  OptoRegPair() { _second = OptoReg::Bad; _first = OptoReg::Bad; }
};

#endif // SHARE_OPTO_OPTOREG_HPP
