/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "opto/regalloc.hpp"

static const int NodeRegsOverflowSize = 200;

void (*PhaseRegAlloc::_alloc_statistics[MAX_REG_ALLOCATORS])();
int PhaseRegAlloc::_num_allocators = 0;
#ifndef PRODUCT
int PhaseRegAlloc::_total_framesize = 0;
int PhaseRegAlloc::_max_framesize = 0;
#endif

PhaseRegAlloc::PhaseRegAlloc( uint unique, PhaseCFG &cfg,
                              Matcher &matcher,
                              void (*pr_stats)() ):
               Phase(Register_Allocation),
               _node_regs(0),
               _node_regs_max_index(0),
               _cfg(cfg),
               _framesize(0xdeadbeef),
               _matcher(matcher)
{
    int i;

    for (i=0; i < _num_allocators; i++) {
        if (_alloc_statistics[i] == pr_stats)
            return;
    }
    assert((_num_allocators + 1) < MAX_REG_ALLOCATORS, "too many register allocators");
    _alloc_statistics[_num_allocators++] = pr_stats;
}


//------------------------------reg2offset-------------------------------------
int PhaseRegAlloc::reg2offset_unchecked( OptoReg::Name reg ) const {
  // Slots below _max_in_arg_stack_reg are offset by the entire frame.
  // Slots above _max_in_arg_stack_reg are frame_slots and are not offset.
  int slot = (reg < _matcher._new_SP)
    ? reg - OptoReg::stack0() + _framesize
    : reg - _matcher._new_SP;
  // Note:  We use the direct formula (reg - SharedInfo::stack0) instead of
  // OptoReg::reg2stack(reg), in order to avoid asserts in the latter
  // function.  This routine must remain unchecked, so that dump_frame()
  // can do its work undisturbed.
  // %%% not really clear why reg2stack would assert here

  return slot*VMRegImpl::stack_slot_size;
}

int PhaseRegAlloc::reg2offset( OptoReg::Name reg ) const {

  // Not allowed in the out-preserve area.
  // In-preserve area is allowed so Intel can fetch the return pc out.
  assert( reg <  _matcher._old_SP ||
          (reg >= OptoReg::add(_matcher._old_SP,C->out_preserve_stack_slots()) &&
           reg <  _matcher._in_arg_limit) ||
          reg >=  OptoReg::add(_matcher._new_SP, C->out_preserve_stack_slots()) ||
          // Allow return_addr in the out-preserve area.
          reg == _matcher.return_addr(),
          "register allocated in a preserve area" );
  return reg2offset_unchecked( reg );
}

//------------------------------offset2reg-------------------------------------
OptoReg::Name PhaseRegAlloc::offset2reg(int stk_offset) const {
  int slot = stk_offset / jintSize;
  int reg = (slot < (int) _framesize)
    ? slot + _matcher._new_SP
    : OptoReg::stack2reg(slot) - _framesize;
  assert(stk_offset == reg2offset((OptoReg::Name) reg),
         "offset2reg does not invert properly");
  return (OptoReg::Name) reg;
}

//------------------------------set_oop----------------------------------------
void PhaseRegAlloc::set_oop( const Node *n, bool is_an_oop ) {
  if( is_an_oop ) {
    _node_oops.set(n->_idx);
  }
}

//------------------------------is_oop-----------------------------------------
bool PhaseRegAlloc::is_oop( const Node *n ) const {
  return _node_oops.test(n->_idx) != 0;
}

// Allocate _node_regs table with at least "size" elements
void PhaseRegAlloc::alloc_node_regs(int size) {
  _node_regs_max_index = size + (size >> 1) + NodeRegsOverflowSize;
  _node_regs = NEW_RESOURCE_ARRAY( OptoRegPair, _node_regs_max_index );
  // We assume our caller will fill in all elements up to size-1, so
  // only the extra space we allocate is initialized here.
  for( uint i = size; i < _node_regs_max_index; ++i )
    _node_regs[i].set_bad();
}

#ifndef PRODUCT
void
PhaseRegAlloc::print_statistics() {
  tty->print_cr("Total frameslots = %d, Max frameslots = %d", _total_framesize, _max_framesize);
  int i;

  for (i=0; i < _num_allocators; i++) {
    _alloc_statistics[i]();
  }
}
#endif
