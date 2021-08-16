/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_FpuStackSim.hpp"
#include "c1/c1_FrameMap.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/ostream.hpp"

//--------------------------------------------------------
//               FpuStackSim
//--------------------------------------------------------

// This class maps the FPU registers to their stack locations; it computes
// the offsets between individual registers and simulates the FPU stack.

const int EMPTY = -1;

int FpuStackSim::regs_at(int i) const {
  assert(i >= 0 && i < FrameMap::nof_fpu_regs, "out of bounds");
  return _regs[i];
}

void FpuStackSim::set_regs_at(int i, int val) {
  assert(i >= 0 && i < FrameMap::nof_fpu_regs, "out of bounds");
  _regs[i] = val;
}

void FpuStackSim::dec_stack_size() {
  _stack_size--;
  assert(_stack_size >= 0, "FPU stack underflow");
}

void FpuStackSim::inc_stack_size() {
  _stack_size++;
  assert(_stack_size <= FrameMap::nof_fpu_regs, "FPU stack overflow");
}

FpuStackSim::FpuStackSim(Compilation* compilation)
 : _compilation(compilation)
{
  _stack_size = 0;
  for (int i = 0; i < FrameMap::nof_fpu_regs; i++) {
    set_regs_at(i, EMPTY);
  }
}


void FpuStackSim::pop() {
  if (TraceFPUStack) { tty->print("FPU-pop "); print(); tty->cr(); }
  set_regs_at(tos_index(), EMPTY);
  dec_stack_size();
}

void FpuStackSim::pop(int rnr) {
  if (TraceFPUStack) { tty->print("FPU-pop %d", rnr); print(); tty->cr(); }
  assert(regs_at(tos_index()) == rnr, "rnr is not on TOS");
  set_regs_at(tos_index(), EMPTY);
  dec_stack_size();
}


void FpuStackSim::push(int rnr) {
  if (TraceFPUStack) { tty->print("FPU-push %d", rnr); print(); tty->cr(); }
  assert(regs_at(stack_size()) == EMPTY, "should be empty");
  set_regs_at(stack_size(), rnr);
  inc_stack_size();
}


void FpuStackSim::swap(int offset) {
  if (TraceFPUStack) { tty->print("FPU-swap %d", offset); print(); tty->cr(); }
  int t = regs_at(tos_index() - offset);
  set_regs_at(tos_index() - offset, regs_at(tos_index()));
  set_regs_at(tos_index(), t);
}


int FpuStackSim::offset_from_tos(int rnr) const {
  for (int i = tos_index(); i >= 0; i--) {
    if (regs_at(i) == rnr) {
      return tos_index() - i;
    }
  }
  assert(false, "FpuStackSim: register not found");
  BAILOUT_("FpuStackSim: register not found", 0);
}


int FpuStackSim::get_slot(int tos_offset) const {
  return regs_at(tos_index() - tos_offset);
}

void FpuStackSim::set_slot(int tos_offset, int rnr) {
  set_regs_at(tos_index() - tos_offset, rnr);
}

void FpuStackSim::rename(int old_rnr, int new_rnr) {
  if (TraceFPUStack) { tty->print("FPU-rename %d %d", old_rnr, new_rnr); print(); tty->cr(); }
  if (old_rnr == new_rnr)
    return;
  bool found = false;
  for (int i = 0; i < stack_size(); i++) {
    assert(regs_at(i) != new_rnr, "should not see old occurrences of new_rnr on the stack");
    if (regs_at(i) == old_rnr) {
      set_regs_at(i, new_rnr);
      found = true;
    }
  }
  assert(found, "should have found at least one instance of old_rnr");
}


bool FpuStackSim::contains(int rnr) {
  for (int i = 0; i < stack_size(); i++) {
    if (regs_at(i) == rnr) {
      return true;
    }
  }
  return false;
}

bool FpuStackSim::is_empty() {
#ifdef ASSERT
  if (stack_size() == 0) {
    for (int i = 0; i < FrameMap::nof_fpu_regs; i++) {
      assert(regs_at(i) == EMPTY, "must be empty");
    }
  }
#endif
  return stack_size() == 0;
}


bool FpuStackSim::slot_is_empty(int tos_offset) {
  return (regs_at(tos_index() - tos_offset) == EMPTY);
}


void FpuStackSim::clear() {
  if (TraceFPUStack) { tty->print("FPU-clear"); print(); tty->cr(); }
  for (int i = tos_index(); i >= 0; i--) {
    set_regs_at(i, EMPTY);
  }
  _stack_size = 0;
}


intArray* FpuStackSim::write_state() {
  intArray* res = new intArray(1 + FrameMap::nof_fpu_regs);
  res->append(stack_size());
  for (int i = 0; i < FrameMap::nof_fpu_regs; i++) {
    res->append(regs_at(i));
  }
  return res;
}


void FpuStackSim::read_state(intArray* fpu_stack_state) {
  _stack_size = fpu_stack_state->at(0);
  for (int i = 0; i < FrameMap::nof_fpu_regs; i++) {
    set_regs_at(i, fpu_stack_state->at(1 + i));
  }
}


#ifndef PRODUCT
void FpuStackSim::print() {
  tty->print(" N=%d[", stack_size());\
  for (int i = 0; i < stack_size(); i++) {
    int reg = regs_at(i);
    if (reg != EMPTY) {
      tty->print("%d", reg);
    } else {
      tty->print("_");
    }
  };
  tty->print(" ]");
}
#endif
