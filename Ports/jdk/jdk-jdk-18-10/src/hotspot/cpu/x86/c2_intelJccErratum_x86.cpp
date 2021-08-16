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

#include "precompiled.hpp"
#include "asm/macroAssembler.hpp"
#include "c2_intelJccErratum_x86.hpp"
#include "opto/cfgnode.hpp"
#include "opto/compile.hpp"
#include "opto/machnode.hpp"
#include "opto/node.hpp"
#include "opto/output.hpp"
#include "opto/regalloc.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

// Compute which 32 byte boundary an address corresponds to
uintptr_t IntelJccErratum::boundary(uintptr_t addr) {
  return addr >> 5;
}

bool IntelJccErratum::is_crossing_or_ending_at_32_byte_boundary(uintptr_t start_pc, uintptr_t end_pc) {
  int jcc_size = int(end_pc - start_pc);
  assert(jcc_size <= largest_jcc_size(), "invalid jcc size: %d", jcc_size);
  return boundary(start_pc) != boundary(end_pc);
}

bool IntelJccErratum::is_jcc_erratum_branch(const MachNode* node) {
  if (node->is_MachCall() && !node->is_MachCallJava()) {
    return true;
  }
  return node->is_MachBranch();
}

int IntelJccErratum::jcc_erratum_taint_node(MachNode* node, PhaseRegAlloc* regalloc) {
  node->add_flag(Node::PD::Flag_intel_jcc_erratum);
  return node->size(regalloc);
}

int IntelJccErratum::tag_affected_machnodes(Compile* C, PhaseCFG* cfg, PhaseRegAlloc* regalloc) {
  ResourceMark rm;
  int nop_size = 0;
  MachNode* last_m = NULL;

  for (uint i = 0; i < cfg->number_of_blocks(); ++i) {
    const Block* const block = cfg->get_block(i);
    for (uint j = 0; j < block->number_of_nodes(); ++j) {
      const Node* const node = block->get_node(j);
      if (!node->is_Mach()) {
        continue;
      }
      MachNode* m = node->as_Mach();
      if (is_jcc_erratum_branch(m)) {
        // Found a root jcc erratum branch, flag it as problematic
        nop_size += jcc_erratum_taint_node(m, regalloc);

        if (!m->is_MachReturn() && !m->is_MachCall()) {
          // We might fuse a problematic jcc erratum branch with a preceding
          // ALU instruction - we must catch such problematic macro fusions
          // and flag the ALU instruction as problematic too.
          for (uint k = 1; k < m->req(); ++k) {
            const Node* const use = m->in(k);
            if (use == last_m && !m->is_MachReturn()) {
              // Flag fused conditions too
              nop_size += jcc_erratum_taint_node(last_m, regalloc);
            }
          }
        }
        last_m = NULL;
      } else {
        last_m = m;
      }
    }
  }
  return nop_size;
}

int IntelJccErratum::compute_padding(uintptr_t current_offset, const MachNode* mach, Block* block, uint index_in_block, PhaseRegAlloc* regalloc) {
  int jcc_size = mach->size(regalloc);
  if (index_in_block < block->number_of_nodes() - 1) {
    Node* next = block->get_node(index_in_block + 1);
    if (next->is_Mach() && (next->as_Mach()->flags() & Node::PD::Flag_intel_jcc_erratum)) {
      jcc_size += mach->size(regalloc);
    }
  }
  if (jcc_size > largest_jcc_size()) {
    // Let's not try fixing this for nodes that seem unreasonably large
    return false;
  }
  if (is_crossing_or_ending_at_32_byte_boundary(current_offset, current_offset + jcc_size)) {
    return int(align_up(current_offset, 32) - current_offset);
  } else {
    return 0;
  }
}

#define __ _masm.

uintptr_t IntelJccErratumAlignment::pc() {
  return (uintptr_t)__ pc();
}

IntelJccErratumAlignment::IntelJccErratumAlignment(MacroAssembler& masm, int jcc_size) :
    _masm(masm),
    _start_pc(pc()) {
  if (!VM_Version::has_intel_jcc_erratum()) {
    return;
  }

  if (Compile::current()->output()->in_scratch_emit_size()) {
    // When we measure the size of this 32 byte alignment, we apply a conservative guess.
    __ nop(jcc_size);
  } else if (IntelJccErratum::is_crossing_or_ending_at_32_byte_boundary(_start_pc, _start_pc + jcc_size)) {
    // The affected branch might get slowed down by micro code mitigations
    // as it could be susceptible to the erratum. Place nops until the next
    // 32 byte boundary to make sure the branch will be cached.
    const int alignment_nops = (int)(align_up(_start_pc, 32) - _start_pc);
    __ nop(alignment_nops);
    _start_pc = pc();
  }
}

IntelJccErratumAlignment::~IntelJccErratumAlignment() {
  if (!VM_Version::has_intel_jcc_erratum() ||
      Compile::current()->output()->in_scratch_emit_size()) {
    return;
  }

  assert(!IntelJccErratum::is_crossing_or_ending_at_32_byte_boundary(_start_pc, pc()), "Invalid jcc_size estimate");
}
