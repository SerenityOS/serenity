/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_METHODLIVENESS_HPP
#define SHARE_COMPILER_METHODLIVENESS_HPP

#include "utilities/bitMap.hpp"
#include "utilities/growableArray.hpp"

class ciMethod;

class MethodLivenessResult : public ResourceBitMap {
 private:
  bool _is_valid;

 public:
  MethodLivenessResult()
    : ResourceBitMap()
    , _is_valid(false)
  {}

  MethodLivenessResult(idx_t size_in_bits)
    : ResourceBitMap(size_in_bits)
    , _is_valid(false)
  {}

  void set_is_valid() { _is_valid = true; }
  bool is_valid() { return _is_valid; }
};

class MethodLiveness : public ResourceObj {
 public:
  // The BasicBlock class is used to represent a basic block in the
  // liveness analysis.
  class BasicBlock : public ResourceObj {
   private:
    // This class is only used by the MethodLiveness class.
    friend class MethodLiveness;

    // The analyzer which created this basic block.
    MethodLiveness* _analyzer;

    // The range of this basic block is [start_bci,limit_bci)
    int _start_bci;
    int _limit_bci;

    // The liveness at the start of the block;
    ArenaBitMap _entry;

    // The summarized liveness effects of our direct successors reached
    // by normal control flow
    ArenaBitMap _normal_exit;

    // The summarized liveness effects of our direct successors reached
    // by exceptional control flow
    ArenaBitMap _exception_exit;

    // These members hold the results of the last call to
    // compute_gen_kill_range().  _gen is the set of locals
    // used before they are defined in the range.  _kill is the
    // set of locals defined before they are used.
    ArenaBitMap _gen;
    ArenaBitMap _kill;
    int         _last_bci;

    // A list of all blocks which could come directly before this one
    // in normal (non-exceptional) control flow.  We propagate liveness
    // information to these blocks.
    GrowableArray<BasicBlock*>* _normal_predecessors;

    // A list of all blocks which could come directly before this one
    // in exceptional control flow.
    GrowableArray<BasicBlock*>* _exception_predecessors;

    // The following fields are used to manage a work list used in the
    // dataflow.
    BasicBlock *_next;
    bool _on_work_list;

    // Our successors call this method to merge liveness information into
    // our _normal_exit member.
    bool merge_normal(const BitMap& other);

    // Our successors call this method to merge liveness information into
    // our _exception_exit member.
    bool merge_exception(const BitMap& other);

    // This helper routine is used to help compute the gen/kill pair for
    // the block.  It is also used to answer queries.
    void compute_gen_kill_range(ciBytecodeStream *bytes);

    // Compute the gen/kill effect of a single instruction.
    void compute_gen_kill_single(ciBytecodeStream *instruction);

    // Helpers for compute_gen_kill_single.
    void load_one(int local);
    void load_two(int local);
    void store_one(int local);
    void store_two(int local);

    BasicBlock(MethodLiveness *analyzer, int start, int limit);

    // -- Accessors

    int start_bci() const { return _start_bci; }

    int limit_bci() const { return _limit_bci; }
    void set_limit_bci(int limit) { _limit_bci = limit; }

    BasicBlock *next() const { return _next; }
    void set_next(BasicBlock *next) { _next = next; }

    bool on_work_list() const { return _on_work_list; }
    void set_on_work_list(bool val) { _on_work_list = val; }

    // -- Flow graph construction.

    // Add a basic block to our list of normal predecessors.
    void add_normal_predecessor(BasicBlock *pred) {
      _normal_predecessors->append_if_missing(pred);
    }

    // Add a basic block to our list of exceptional predecessors
    void add_exception_predecessor(BasicBlock *pred) {
      _exception_predecessors->append_if_missing(pred);
    }

    // Split the basic block at splitBci.  This basic block
    // becomes the second half.  The first half is newly created.
    BasicBlock *split(int splitBci);

    // -- Dataflow.

    void compute_gen_kill(ciMethod* method);

    // Propagate changes from this basic block
    void propagate(MethodLiveness *ml);

    // -- Query.

    MethodLivenessResult get_liveness_at(ciMethod* method, int bci);

    // -- Debugging.

    void print_on(outputStream *os) const PRODUCT_RETURN;

  }; // End of MethodLiveness::BasicBlock

 private:
  // The method we are analyzing.
  ciMethod* _method;
  ciMethod* method() const { return _method; }

  // The arena for storing structures...
  Arena*       _arena;
  Arena*       arena() const { return _arena; }

  // We cache the length of the method.
  int _code_size;

  // The size of a BitMap.
  int _bit_map_size_bits;

  // A list of all BasicBlocks.
  BasicBlock **_block_list;

  // number of blocks
  int  _block_count;

  // Keeps track of bci->block mapping.  One entry for each bci.  Only block starts are
  // recorded.
  GrowableArray<BasicBlock*>* _block_map;

  // Our work list.
  BasicBlock *_work_list;

#ifdef COMPILER1
  // bcis where blocks start are marked
  ArenaBitMap _bci_block_start;
#endif // COMPILER1

  // -- Graph construction & Analysis

  // Compute ranges and predecessors for basic blocks.
  void init_basic_blocks();

  // Compute gen/kill information for all basic blocks.
  void init_gen_kill();

  // Perform the dataflow.
  void propagate_liveness();

 // The class MethodLiveness::BasicBlock needs special access to some
 // of our members.
 friend class MethodLiveness::BasicBlock;

  // And accessors.
  int bit_map_size_bits() const { return _bit_map_size_bits; }

  // Work list manipulation routines.  Called internally by BasicBlock.
  BasicBlock *work_list_get();
  void work_list_add(BasicBlock *block);

 public:
  // Create a liveness analyzer for a method
  MethodLiveness(Arena* arena, ciMethod* method);

  // Compute liveness information for the method
  void compute_liveness();

  // Find out which locals are live at a specific bci.
  MethodLivenessResult get_liveness_at(int bci);

#ifdef COMPILER1
  const BitMap& get_bci_block_start() const { return _bci_block_start; }
#endif // COMPILER1

};

#endif // SHARE_COMPILER_METHODLIVENESS_HPP
