/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
#include "code/nmethod.hpp"
#include "compiler/compileBroker.hpp"
#include "opto/compile.hpp"
#include "opto/matcher.hpp"
#include "opto/node.hpp"
#include "opto/phase.hpp"

int Phase::_total_bytes_compiled = 0;

elapsedTimer Phase::_t_totalCompilation;
elapsedTimer Phase::_t_methodCompilation;
elapsedTimer Phase::_t_stubCompilation;

// The counters to use for LogCompilation
elapsedTimer Phase::timers[max_phase_timers];

//------------------------------Phase------------------------------------------
Phase::Phase( PhaseNumber pnum ) : _pnum(pnum), C( pnum == Compiler ? NULL : Compile::current()) {
  // Poll for requests from shutdown mechanism to quiesce compiler (4448539, 4448544).
  // This is an effective place to poll, since the compiler is full of phases.
  // In particular, every inlining site uses a recursively created Parse phase.
  CompileBroker::maybe_block();
}

void Phase::print_timers() {
  tty->print_cr ("    C2 Compile Time:      %7.3f s", Phase::_t_totalCompilation.seconds());
  tty->print_cr ("       Parse:               %7.3f s", timers[_t_parser].seconds());

  {
    tty->print_cr ("       Optimize:            %7.3f s", timers[_t_optimizer].seconds());
    if (DoEscapeAnalysis) {
      // EA is part of Optimizer.
      tty->print_cr ("         Escape Analysis:     %7.3f s", timers[_t_escapeAnalysis].seconds());
      tty->print_cr ("           Conn Graph:          %7.3f s", timers[_t_connectionGraph].seconds());
      tty->print_cr ("           Macro Eliminate:     %7.3f s", timers[_t_macroEliminate].seconds());
    }
    tty->print_cr ("         GVN 1:               %7.3f s", timers[_t_iterGVN].seconds());

    {
       tty->print_cr ("         Incremental Inline:  %7.3f s", timers[_t_incrInline].seconds());
       tty->print_cr ("           IdealLoop:           %7.3f s", timers[_t_incrInline_ideal].seconds());
       tty->print_cr ("          (IGVN:                %7.3f s)", timers[_t_incrInline_igvn].seconds());
       tty->print_cr ("          (Inline:              %7.3f s)", timers[_t_incrInline_inline].seconds());
       tty->print_cr ("          (Prune Useless:       %7.3f s)", timers[_t_incrInline_pru].seconds());

       double other = timers[_t_incrInline].seconds() -
        (timers[_t_incrInline_ideal].seconds());
       if (other > 0) {
         tty->print_cr("           Other:               %7.3f s", other);
       }
    }

    tty->print_cr ("         Vector:              %7.3f s", timers[_t_vector].seconds());
    tty->print_cr ("           Box elimination:   %7.3f s", timers[_t_vector_elimination].seconds());
    tty->print_cr ("             IGVN:            %7.3f s", timers[_t_vector_igvn].seconds());
    tty->print_cr ("             Prune Useless:   %7.3f s", timers[_t_vector_pru].seconds());
    tty->print_cr ("         Renumber Live:       %7.3f s", timers[_t_renumberLive].seconds());
    tty->print_cr ("         IdealLoop:           %7.3f s", timers[_t_idealLoop].seconds());
    tty->print_cr ("         IdealLoop Verify:    %7.3f s", timers[_t_idealLoopVerify].seconds());
    tty->print_cr ("         Cond Const Prop:     %7.3f s", timers[_t_ccp].seconds());
    tty->print_cr ("         GVN 2:               %7.3f s", timers[_t_iterGVN2].seconds());
    tty->print_cr ("         Macro Expand:        %7.3f s", timers[_t_macroExpand].seconds());
    tty->print_cr ("         Barrier Expand:      %7.3f s", timers[_t_barrierExpand].seconds());
    tty->print_cr ("         Graph Reshape:       %7.3f s", timers[_t_graphReshaping].seconds());

    double other = timers[_t_optimizer].seconds() -
      (timers[_t_escapeAnalysis].seconds() +
       timers[_t_iterGVN].seconds() +
       timers[_t_incrInline].seconds() +
       timers[_t_vector].seconds() +
       timers[_t_renumberLive].seconds() +
       timers[_t_idealLoop].seconds() +
       timers[_t_idealLoopVerify].seconds() +
       timers[_t_ccp].seconds() +
       timers[_t_iterGVN2].seconds() +
       timers[_t_macroExpand].seconds() +
       timers[_t_barrierExpand].seconds() +
       timers[_t_graphReshaping].seconds());
    if (other > 0) {
      tty->print_cr("         Other:               %7.3f s", other);
    }
  }

  tty->print_cr ("       Matcher:                  %7.3f s", timers[_t_matcher].seconds());
  if (Matcher::supports_generic_vector_operands) {
    tty->print_cr ("         Post Selection Cleanup: %7.3f s", timers[_t_postselect_cleanup].seconds());
  }
  tty->print_cr ("       Scheduler:                %7.3f s", timers[_t_scheduler].seconds());

  {
    tty->print_cr ("       Regalloc:            %7.3f s", timers[_t_registerAllocation].seconds());
    tty->print_cr ("         Ctor Chaitin:        %7.3f s", timers[_t_ctorChaitin].seconds());
    tty->print_cr ("         Build IFG (virt):    %7.3f s", timers[_t_buildIFGvirtual].seconds());
    tty->print_cr ("         Build IFG (phys):    %7.3f s", timers[_t_buildIFGphysical].seconds());
    tty->print_cr ("         Compute Liveness:    %7.3f s", timers[_t_computeLive].seconds());
    tty->print_cr ("         Regalloc Split:      %7.3f s", timers[_t_regAllocSplit].seconds());
    tty->print_cr ("         Postalloc Copy Rem:  %7.3f s", timers[_t_postAllocCopyRemoval].seconds());
    tty->print_cr ("         Merge multidefs:     %7.3f s", timers[_t_mergeMultidefs].seconds());
    tty->print_cr ("         Fixup Spills:        %7.3f s", timers[_t_fixupSpills].seconds());
    tty->print_cr ("         Compact:             %7.3f s", timers[_t_chaitinCompact].seconds());
    tty->print_cr ("         Coalesce 1:          %7.3f s", timers[_t_chaitinCoalesce1].seconds());
    tty->print_cr ("         Coalesce 2:          %7.3f s", timers[_t_chaitinCoalesce2].seconds());
    tty->print_cr ("         Coalesce 3:          %7.3f s", timers[_t_chaitinCoalesce3].seconds());
    tty->print_cr ("         Cache LRG:           %7.3f s", timers[_t_chaitinCacheLRG].seconds());
    tty->print_cr ("         Simplify:            %7.3f s", timers[_t_chaitinSimplify].seconds());
    tty->print_cr ("         Select:              %7.3f s", timers[_t_chaitinSelect].seconds());

    double other = timers[_t_registerAllocation].seconds() -
      (timers[_t_ctorChaitin].seconds() +
       timers[_t_buildIFGvirtual].seconds() +
       timers[_t_buildIFGphysical].seconds() +
       timers[_t_computeLive].seconds() +
       timers[_t_regAllocSplit].seconds() +
       timers[_t_postAllocCopyRemoval].seconds() +
       timers[_t_mergeMultidefs].seconds() +
       timers[_t_fixupSpills].seconds() +
       timers[_t_chaitinCompact].seconds() +
       timers[_t_chaitinCoalesce1].seconds() +
       timers[_t_chaitinCoalesce2].seconds() +
       timers[_t_chaitinCoalesce3].seconds() +
       timers[_t_chaitinCacheLRG].seconds() +
       timers[_t_chaitinSimplify].seconds() +
       timers[_t_chaitinSelect].seconds());

    if (other > 0) {
      tty->print_cr("         Other:               %7.3f s", other);
    }
  }
  tty->print_cr ("       Block Ordering:      %7.3f s", timers[_t_blockOrdering].seconds());
  tty->print_cr ("       Peephole:            %7.3f s", timers[_t_peephole].seconds());
  if (Matcher::require_postalloc_expand) {
    tty->print_cr ("       Postalloc Expand:    %7.3f s", timers[_t_postalloc_expand].seconds());
  }
  tty->print_cr ("       Code Emission:         %7.3f s", timers[_t_output].seconds());
  tty->print_cr ("         Insn Scheduling:     %7.3f s", timers[_t_instrSched].seconds());
  tty->print_cr ("         Shorten branches:    %7.3f s", timers[_t_shortenBranches].seconds());
  tty->print_cr ("         Build OOP maps:      %7.3f s", timers[_t_buildOopMaps].seconds());
  tty->print_cr ("         Fill buffer:         %7.3f s", timers[_t_fillBuffer].seconds());
  tty->print_cr ("         Code Installation:   %7.3f s", timers[_t_registerMethod].seconds());

  {
    double other = timers[_t_output].seconds() -
                   (timers[_t_instrSched].seconds() +
                    timers[_t_shortenBranches].seconds() +
                    timers[_t_buildOopMaps].seconds() +
                    timers[_t_fillBuffer].seconds() +
                    timers[_t_registerMethod].seconds());

    if (other > 0) {
      tty->print_cr("         Other:               %7.3f s", other);
    }
  }

  if( timers[_t_temporaryTimer1].seconds() > 0 ) {
    tty->cr();
    tty->print_cr ("       Temp Timer 1:        %7.3f s", timers[_t_temporaryTimer1].seconds());
  }
  if( timers[_t_temporaryTimer2].seconds() > 0 ) {
    tty->cr();
    tty->print_cr ("       Temp Timer 2:        %7.3f s", timers[_t_temporaryTimer2].seconds());
  }

   double other = Phase::_t_totalCompilation.seconds() -
      (timers[_t_parser].seconds() +
       timers[_t_optimizer].seconds() +
       timers[_t_matcher].seconds() +
       timers[_t_scheduler].seconds() +
       timers[_t_registerAllocation].seconds() +
       timers[_t_blockOrdering].seconds() +
       timers[_t_peephole].seconds() +
       timers[_t_postalloc_expand].seconds() +
       timers[_t_output].seconds() +
       timers[_t_registerMethod].seconds() +
       timers[_t_temporaryTimer1].seconds() +
       timers[_t_temporaryTimer2].seconds());
    if (other > 0) {
      tty->print_cr("       Other:               %7.3f s", other);
    }

}
