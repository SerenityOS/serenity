/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_INTELJCCERRATUM_X86_HPP
#define CPU_X86_INTELJCCERRATUM_X86_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class Block;
class Compile;
class MachNode;
class MacroAssembler;
class PhaseCFG;
class PhaseRegAlloc;

class IntelJccErratum : public AllStatic {
private:
  // Compute which 32 byte boundary an address corresponds to
  static uintptr_t boundary(uintptr_t addr);
  static int jcc_erratum_taint_node(MachNode* node, PhaseRegAlloc* regalloc);

public:
  static bool is_crossing_or_ending_at_32_byte_boundary(uintptr_t start_pc, uintptr_t end_pc);
  static bool is_jcc_erratum_branch(const MachNode* node);
  // Analyze JCC erratum branches. Affected nodes get tagged with Flag_intel_jcc_erratum.
  // The function returns a conservative estimate of all required nops on all mach nodes.
  static int tag_affected_machnodes(Compile* C, PhaseCFG* cfg, PhaseRegAlloc* regalloc);
  // Computes the exact padding for a mach node
  static int compute_padding(uintptr_t current_offset, const MachNode* mach, Block* block, uint index_in_block, PhaseRegAlloc* regalloc);
  static int largest_jcc_size() { return 20; }
};

class IntelJccErratumAlignment {
private:
  MacroAssembler& _masm;
  uintptr_t       _start_pc;

  uintptr_t pc();

public:
  IntelJccErratumAlignment(MacroAssembler& masm, int jcc_size);
  ~IntelJccErratumAlignment();
};

#endif // CPU_X86_INTELJCCERRATUM_X86_HPP

