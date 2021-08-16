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

#ifndef SHARE_OPTO_PHASE_HPP
#define SHARE_OPTO_PHASE_HPP

#include "runtime/timer.hpp"

class IfNode;
class MergeMemNode;
class Node;
class PhaseGVN;

//------------------------------Phase------------------------------------------
// Most optimizations are done in Phases.  Creating a phase does any long
// running analysis required, and caches the analysis in internal data
// structures.  Later the analysis is queried using transform() calls to
// guide transforming the program.  When the Phase is deleted, so is any
// cached analysis info.  This basic Phase class mostly contains timing and
// memory management code.
class Phase : public StackObj {
public:
  enum PhaseNumber {
    Compiler,                         // Top-level compiler phase
    Parser,                           // Parse bytecodes
    Remove_Useless,                   // Remove useless nodes
    Remove_Useless_And_Renumber_Live, // First, remove useless nodes from the graph. Then, renumber live nodes.
    Optimistic,                       // Optimistic analysis phase
    GVN,                              // Pessimistic global value numbering phase
    Ins_Select,                       // Instruction selection phase
    CFG,                              // Build a CFG
    BlockLayout,                      // Linear ordering of blocks
    Register_Allocation,              // Register allocation, duh
    LIVE,                             // Dragon-book LIVE range problem
    StringOpts,                       // StringBuilder related optimizations
    Interference_Graph,               // Building the IFG
    Coalesce,                         // Coalescing copies
    Ideal_Loop,                       // Find idealized trip-counted loops
    Macro_Expand,                     // Expand macro nodes
    Peephole,                         // Apply peephole optimizations
    Vector,
    Output,
    last_phase
  };

  enum PhaseTraceId {
    _t_parser,
    _t_optimizer,
      _t_escapeAnalysis,
        _t_connectionGraph,
        _t_macroEliminate,
      _t_iterGVN,
      _t_incrInline,
        _t_incrInline_ideal,
        _t_incrInline_igvn,
        _t_incrInline_pru,
        _t_incrInline_inline,
      _t_vector,
        _t_vector_elimination,
          _t_vector_igvn,
          _t_vector_pru,
      _t_renumberLive,
      _t_idealLoop,
      _t_idealLoopVerify,
      _t_ccp,
      _t_iterGVN2,
      _t_macroExpand,
      _t_barrierExpand,
      _t_graphReshaping,
    _t_matcher,
      _t_postselect_cleanup,
    _t_scheduler,
    _t_registerAllocation,
      _t_ctorChaitin,
      _t_buildIFGvirtual,
      _t_buildIFGphysical,
      _t_computeLive,
      _t_regAllocSplit,
      _t_postAllocCopyRemoval,
      _t_mergeMultidefs,
      _t_fixupSpills,
      _t_chaitinCompact,
      _t_chaitinCoalesce1,
      _t_chaitinCoalesce2,
      _t_chaitinCoalesce3,
      _t_chaitinCacheLRG,
      _t_chaitinSimplify,
      _t_chaitinSelect,
    _t_blockOrdering,
    _t_peephole,
    _t_postalloc_expand,
    _t_output,
      _t_instrSched,
      _t_shortenBranches,
      _t_buildOopMaps,
      _t_fillBuffer,
      _t_registerMethod,
    _t_temporaryTimer1,
    _t_temporaryTimer2,
    max_phase_timers
   };

  static elapsedTimer timers[max_phase_timers];

protected:
  enum PhaseNumber _pnum;       // Phase number (for stat gathering)

  static int _total_bytes_compiled;

  // accumulated timers
  static elapsedTimer _t_totalCompilation;
  static elapsedTimer _t_methodCompilation;
  static elapsedTimer _t_stubCompilation;

  // Generate a subtyping check.  Takes as input the subtype and supertype.
  // Returns 2 values: sets the default control() to the true path and
  // returns the false path.  Only reads from constant memory taken from the
  // default memory; does not write anything.  It also doesn't take in an
  // Object; if you wish to check an Object you need to load the Object's
  // class prior to coming here.
  // Used in GraphKit and PhaseMacroExpand
  static Node* gen_subtype_check(Node* subklass, Node* superklass, Node** ctrl, Node* mem, PhaseGVN& gvn);

public:
  Compile * C;
  Phase( PhaseNumber pnum );

  static void print_timers();
};

#endif // SHARE_OPTO_PHASE_HPP
