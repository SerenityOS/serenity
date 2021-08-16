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

#ifndef SHARE_OPTO_LIVE_HPP
#define SHARE_OPTO_LIVE_HPP

#include "libadt/vectset.hpp"
#include "opto/block.hpp"
#include "opto/indexSet.hpp"
#include "opto/phase.hpp"
#include "opto/regmask.hpp"

class Block;
class PhaseCFG;
class VectorSet;
class IndexSet;

//------------------------------LRG_List---------------------------------------
// Map Node indices to Live RanGe indices.
// Array lookup in the optimized case.
typedef GrowableArray<uint> LRG_List;

//------------------------------PhaseLive--------------------------------------
// Compute live-in/live-out
class PhaseLive : public Phase {
  // Array of Sets of values live at the start of a block.
  // Indexed by block pre-order number.
  IndexSet *_live; // live out
  IndexSet *_livein; // live in

  // Array of Sets of values defined locally in the block
  // Indexed by block pre-order number.
  IndexSet *_defs;

  // Array of delta-set pointers, indexed by block pre-order number
  IndexSet **_deltas;
  IndexSet *_free_IndexSet;     // Free list of same

  Block_List *_worklist;        // Worklist for iterative solution

  const PhaseCFG &_cfg;         // Basic blocks
  const LRG_List &_names;       // Mapping from Nodes to live ranges
  uint _maxlrg;                 // Largest live-range number
  Arena *_arena;
  bool _keep_deltas;            // Retain live in information

  IndexSet *getset( Block *p );
  IndexSet *getfreeset( );
  void freeset( Block *p );
  void add_liveout( Block *p, uint r, VectorSet &first_pass );
  void add_liveout( Block *p, IndexSet *lo, VectorSet &first_pass );
  void add_livein( Block *p, IndexSet *lo );

public:
  PhaseLive(const PhaseCFG &cfg, const LRG_List &names, Arena *arena, bool keep_deltas);
  ~PhaseLive() {}
  // Compute liveness info
  void compute(uint maxlrg);
  // Reset arena storage
  void reset() { _live = NULL; }

  // Return the live-out set for this block
  IndexSet *live( const Block * b ) { return &_live[b->_pre_order-1]; }
  IndexSet *livein( const Block * b ) { return &_livein[b->_pre_order - 1]; }

#ifndef PRODUCT
  void dump( const Block *b ) const;
  void stats(uint iters) const;
#endif
};

#endif // SHARE_OPTO_LIVE_HPP
