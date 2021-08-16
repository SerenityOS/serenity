/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_RANGECHECKELIMINATION_HPP
#define SHARE_C1_C1_RANGECHECKELIMINATION_HPP

#include "c1/c1_Instruction.hpp"

// Base class for range check elimination
class RangeCheckElimination : AllStatic {
public:
  static void eliminate(IR *ir);
};

// Implementation
class RangeCheckEliminator {
private:
  int _number_of_instructions;
  bool _optimistic; // Insert predicates and deoptimize when they fail
  IR *_ir;

  typedef GrowableArray<BlockBegin*> BlockBeginList;
  typedef GrowableArray<int> IntegerStack;

#ifdef ASSERT
  class Verification : public BlockClosure {
  // RangeCheckEliminator::Verification should never get instatiated on the heap.
  private:
    void* operator new(size_t size) throw();
    void* operator new[](size_t size) throw();
    void operator delete(void* p) { ShouldNotReachHere(); }
    void operator delete[](void* p) { ShouldNotReachHere(); }

    bool can_reach(BlockBegin *start, BlockBegin *end, BlockBegin *dont_use = NULL);
    bool dominates(BlockBegin *dominator, BlockBegin *block);
    bool is_backbranch_from_xhandler(BlockBegin* block);

    IR *_ir;
    boolArray _used;
    BlockBeginList _current;
    BlockBeginList _successors;

  public:
    Verification(IR *ir);
    virtual void block_do(BlockBegin *block);
  };
#endif

public:
  // Bounds for an instruction in the form x + c which c integer
  // constant and x another instruction
  class Bound : public CompilationResourceObj {
  private:
    int _upper;
    Value _upper_instr;
    int _lower;
    Value _lower_instr;

  public:
    Bound();
    Bound(Value v);
    Bound(Instruction::Condition cond, Value v, int constant = 0);
    Bound(int lower, Value lower_instr, int upper, Value upper_instr);
    ~Bound();

#ifdef ASSERT
    void add_assertion(Instruction *instruction, Instruction *position, int i, Value instr, Instruction::Condition cond);
#endif
    int upper();
    Value upper_instr();
    int lower();
    Value lower_instr();
    void print();
    bool check_no_overflow(int const_value);
    void or_op(Bound *b);
    void and_op(Bound *b);
    bool has_upper();
    bool has_lower();
    void set_upper(int upper, Value upper_instr);
    void set_lower(int lower, Value lower_instr);
    bool is_smaller(Bound *b);
    void remove_upper();
    void remove_lower();
    void add_constant(int value);
    Bound *copy();
  };


  class Visitor : public InstructionVisitor {
  private:
    Bound *_bound;
    RangeCheckEliminator *_rce;

  public:
    void set_range_check_eliminator(RangeCheckEliminator *rce) { _rce = rce; }
    Bound *bound() const { return _bound; }
    void clear_bound() { _bound = NULL; }

  protected:
    // visitor functions
    void do_Constant       (Constant*        x);
    void do_IfOp           (IfOp*            x);
    void do_LogicOp        (LogicOp*         x);
    void do_ArithmeticOp   (ArithmeticOp*    x);
    void do_Phi            (Phi*             x);

    void do_StoreField     (StoreField*      x) { /* nothing to do */ };
    void do_StoreIndexed   (StoreIndexed*    x) { /* nothing to do */ };
    void do_MonitorEnter   (MonitorEnter*    x) { /* nothing to do */ };
    void do_MonitorExit    (MonitorExit*     x) { /* nothing to do */ };
    void do_Invoke         (Invoke*          x) { /* nothing to do */ };
    void do_Intrinsic      (Intrinsic*       x) { /* nothing to do */ };
    void do_Local          (Local*           x) { /* nothing to do */ };
    void do_LoadField      (LoadField*       x) { /* nothing to do */ };
    void do_ArrayLength    (ArrayLength*     x) { /* nothing to do */ };
    void do_LoadIndexed    (LoadIndexed*     x) { /* nothing to do */ };
    void do_NegateOp       (NegateOp*        x) { /* nothing to do */ };
    void do_ShiftOp        (ShiftOp*         x) { /* nothing to do */ };
    void do_CompareOp      (CompareOp*       x) { /* nothing to do */ };
    void do_Convert        (Convert*         x) { /* nothing to do */ };
    void do_NullCheck      (NullCheck*       x) { /* nothing to do */ };
    void do_TypeCast       (TypeCast*        x) { /* nothing to do */ };
    void do_NewInstance    (NewInstance*     x) { /* nothing to do */ };
    void do_NewTypeArray   (NewTypeArray*    x) { /* nothing to do */ };
    void do_NewObjectArray (NewObjectArray*  x) { /* nothing to do */ };
    void do_NewMultiArray  (NewMultiArray*   x) { /* nothing to do */ };
    void do_CheckCast      (CheckCast*       x) { /* nothing to do */ };
    void do_InstanceOf     (InstanceOf*      x) { /* nothing to do */ };
    void do_BlockBegin     (BlockBegin*      x) { /* nothing to do */ };
    void do_Goto           (Goto*            x) { /* nothing to do */ };
    void do_If             (If*              x) { /* nothing to do */ };
    void do_TableSwitch    (TableSwitch*     x) { /* nothing to do */ };
    void do_LookupSwitch   (LookupSwitch*    x) { /* nothing to do */ };
    void do_Return         (Return*          x) { /* nothing to do */ };
    void do_Throw          (Throw*           x) { /* nothing to do */ };
    void do_Base           (Base*            x) { /* nothing to do */ };
    void do_OsrEntry       (OsrEntry*        x) { /* nothing to do */ };
    void do_ExceptionObject(ExceptionObject* x) { /* nothing to do */ };
    void do_RoundFP        (RoundFP*         x) { /* nothing to do */ };
    void do_UnsafePut      (UnsafePut*       x) { /* nothing to do */ };
    void do_UnsafeGet      (UnsafeGet*       x) { /* nothing to do */ };
    void do_UnsafeGetAndSet(UnsafeGetAndSet* x) { /* nothing to do */ };
    void do_ProfileCall    (ProfileCall*     x) { /* nothing to do */ };
    void do_ProfileReturnType (ProfileReturnType*  x) { /* nothing to do */ };
    void do_ProfileInvoke  (ProfileInvoke*   x) { /* nothing to do */ };
    void do_RuntimeCall    (RuntimeCall*     x) { /* nothing to do */ };
    void do_MemBar         (MemBar*          x) { /* nothing to do */ };
    void do_RangeCheckPredicate(RangeCheckPredicate* x) { /* nothing to do */ };
#ifdef ASSERT
    void do_Assert         (Assert*          x) { /* nothing to do */ };
#endif
  };

#ifdef ASSERT
  void add_assertions(Bound *bound, Instruction *instruction, Instruction *position);
#endif

  typedef GrowableArray<Bound*> BoundStack;
  typedef GrowableArray<BoundStack*> BoundMap;
  typedef GrowableArray<AccessIndexed*> AccessIndexedList;
  typedef GrowableArray<Instruction*> InstructionList;

  class AccessIndexedInfo : public CompilationResourceObj  {
  public:
    AccessIndexedList *_list;
    int _min;
    int _max;
  };

  typedef GrowableArray<AccessIndexedInfo*> AccessIndexedInfoArray;
  BoundMap _bounds; // Mapping from Instruction's id to current bound
  AccessIndexedInfoArray _access_indexed_info; // Mapping from Instruction's id to AccessIndexedInfo for in block motion
  Visitor _visitor;

public:
  RangeCheckEliminator(IR *ir);

  IR *ir() const { return _ir; }

  // Pass over the dominator tree to identify blocks where there's an oppportunity for optimization
  bool set_process_block_flags(BlockBegin *block);
  // The core of the optimization work: pass over the dominator tree
  // to propagate bound information, insert predicate out of loops,
  // eliminate bound checks when possible and perform in block motion
  void calc_bounds(BlockBegin *block, BlockBegin *loop_header);
  // reorder bound checks within a block in order to eliminate some of them
  void in_block_motion(BlockBegin *block, AccessIndexedList &accessIndexed, InstructionList &arrays);

  // update/access current bound
  void update_bound(IntegerStack &pushed, Value v, Instruction::Condition cond, Value value, int constant);
  void update_bound(IntegerStack &pushed, Value v, Bound *bound);
  Bound *get_bound(Value v);

  bool loop_invariant(BlockBegin *loop_header, Instruction *instruction);                                    // check for loop invariance
  void add_access_indexed_info(InstructionList &indices, int i, Value instruction, AccessIndexed *ai); // record indexed access for in block motion
  void remove_range_check(AccessIndexed *ai);                                                                // Mark this instructions as not needing a range check
  void add_if_condition(IntegerStack &pushed, Value x, Value y, Instruction::Condition condition);           // Update bound for an If
  bool in_array_bound(Bound *bound, Value array);                                                            // Check whether bound is known to fall within array

  // helper functions to work with predicates
  Instruction* insert_after(Instruction* insert_position, Instruction* instr, int bci);
  Instruction* predicate(Instruction* left, Instruction::Condition cond, Instruction* right, ValueStack* state, Instruction *insert_position, int bci=-1);
  Instruction* predicate_cmp_with_const(Instruction* instr, Instruction::Condition cond, int constant, ValueStack* state, Instruction *insert_position, int bci=1);
  Instruction* predicate_add(Instruction* left, int left_const, Instruction::Condition cond, Instruction* right, ValueStack* state, Instruction *insert_position, int bci=-1);
  Instruction* predicate_add_cmp_with_const(Instruction* left, int left_const, Instruction::Condition cond, int constant, ValueStack* state, Instruction *insert_position, int bci=-1);

  void insert_deoptimization(ValueStack *state, Instruction *insert_position, Instruction *array_instr,      // Add predicate
                             Instruction *length_instruction, Instruction *lower_instr, int lower,
                             Instruction *upper_instr, int upper, AccessIndexed *ai);
  bool is_ok_for_deoptimization(Instruction *insert_position, Instruction *array_instr,                      // Can we safely add a predicate?
                                Instruction *length_instr, Instruction *lower_instr,
                                int lower, Instruction *upper_instr, int upper);
  void process_if(IntegerStack &pushed, BlockBegin *block, If *cond);                                        // process If Instruction
  void process_access_indexed(BlockBegin *loop_header, BlockBegin *block, AccessIndexed *ai);                // process indexed access

  void dump_condition_stack(BlockBegin *cur_block);
  static void print_statistics();
};

#endif // SHARE_C1_C1_RANGECHECKELIMINATION_HPP
