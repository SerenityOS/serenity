/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_CFGPrinter.hpp"
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_LIRGenerator.hpp"
#include "c1/c1_LinearScan.hpp"
#include "c1/c1_ValueStack.hpp"
#include "code/vmreg.inline.hpp"
#include "runtime/timerTrace.hpp"
#include "utilities/bitMap.inline.hpp"

#ifndef PRODUCT

  static LinearScanStatistic _stat_before_alloc;
  static LinearScanStatistic _stat_after_asign;
  static LinearScanStatistic _stat_final;

  static LinearScanTimers _total_timer;

  // helper macro for short definition of timer
  #define TIME_LINEAR_SCAN(timer_name)  TraceTime _block_timer("", _total_timer.timer(LinearScanTimers::timer_name), TimeLinearScan || TimeEachLinearScan, Verbose);

#else
  #define TIME_LINEAR_SCAN(timer_name)
#endif

#ifdef ASSERT

  // helper macro for short definition of trace-output inside code
  #define TRACE_LINEAR_SCAN(level, code)       \
    if (TraceLinearScanLevel >= level) {       \
      code;                                    \
    }
#else
  #define TRACE_LINEAR_SCAN(level, code)
#endif

// Map BasicType to spill size in 32-bit words, matching VMReg's notion of words
#ifdef _LP64
static int type2spill_size[T_CONFLICT+1]={ -1, 0, 0, 0, 1, 1, 1, 2, 1, 1, 1, 2, 2, 2, 0, 2,  1, 2, 1, -1};
#else
static int type2spill_size[T_CONFLICT+1]={ -1, 0, 0, 0, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 0, 1, -1, 1, 1, -1};
#endif


// Implementation of LinearScan

LinearScan::LinearScan(IR* ir, LIRGenerator* gen, FrameMap* frame_map)
 : _compilation(ir->compilation())
 , _ir(ir)
 , _gen(gen)
 , _frame_map(frame_map)
 , _cached_blocks(*ir->linear_scan_order())
 , _num_virtual_regs(gen->max_virtual_register_number())
 , _has_fpu_registers(false)
 , _num_calls(-1)
 , _max_spills(0)
 , _unused_spill_slot(-1)
 , _intervals(0)   // initialized later with correct length
 , _new_intervals_from_allocation(NULL)
 , _sorted_intervals(NULL)
 , _needs_full_resort(false)
 , _lir_ops(0)     // initialized later with correct length
 , _block_of_op(0) // initialized later with correct length
 , _has_info(0)
 , _has_call(0)
 , _interval_in_loop(0)  // initialized later with correct length
 , _scope_value_cache(0) // initialized later with correct length
#ifdef IA32
 , _fpu_stack_allocator(NULL)
#endif
{
  assert(this->ir() != NULL,          "check if valid");
  assert(this->compilation() != NULL, "check if valid");
  assert(this->gen() != NULL,         "check if valid");
  assert(this->frame_map() != NULL,   "check if valid");
}


// ********** functions for converting LIR-Operands to register numbers
//
// Emulate a flat register file comprising physical integer registers,
// physical floating-point registers and virtual registers, in that order.
// Virtual registers already have appropriate numbers, since V0 is
// the number of physical registers.
// Returns -1 for hi word if opr is a single word operand.
//
// Note: the inverse operation (calculating an operand for register numbers)
//       is done in calc_operand_for_interval()

int LinearScan::reg_num(LIR_Opr opr) {
  assert(opr->is_register(), "should not call this otherwise");

  if (opr->is_virtual_register()) {
    assert(opr->vreg_number() >= nof_regs, "found a virtual register with a fixed-register number");
    return opr->vreg_number();
  } else if (opr->is_single_cpu()) {
    return opr->cpu_regnr();
  } else if (opr->is_double_cpu()) {
    return opr->cpu_regnrLo();
#ifdef X86
  } else if (opr->is_single_xmm()) {
    return opr->fpu_regnr() + pd_first_xmm_reg;
  } else if (opr->is_double_xmm()) {
    return opr->fpu_regnrLo() + pd_first_xmm_reg;
#endif
  } else if (opr->is_single_fpu()) {
    return opr->fpu_regnr() + pd_first_fpu_reg;
  } else if (opr->is_double_fpu()) {
    return opr->fpu_regnrLo() + pd_first_fpu_reg;
  } else {
    ShouldNotReachHere();
    return -1;
  }
}

int LinearScan::reg_numHi(LIR_Opr opr) {
  assert(opr->is_register(), "should not call this otherwise");

  if (opr->is_virtual_register()) {
    return -1;
  } else if (opr->is_single_cpu()) {
    return -1;
  } else if (opr->is_double_cpu()) {
    return opr->cpu_regnrHi();
#ifdef X86
  } else if (opr->is_single_xmm()) {
    return -1;
  } else if (opr->is_double_xmm()) {
    return -1;
#endif
  } else if (opr->is_single_fpu()) {
    return -1;
  } else if (opr->is_double_fpu()) {
    return opr->fpu_regnrHi() + pd_first_fpu_reg;
  } else {
    ShouldNotReachHere();
    return -1;
  }
}


// ********** functions for classification of intervals

bool LinearScan::is_precolored_interval(const Interval* i) {
  return i->reg_num() < LinearScan::nof_regs;
}

bool LinearScan::is_virtual_interval(const Interval* i) {
  return i->reg_num() >= LIR_OprDesc::vreg_base;
}

bool LinearScan::is_precolored_cpu_interval(const Interval* i) {
  return i->reg_num() < LinearScan::nof_cpu_regs;
}

bool LinearScan::is_virtual_cpu_interval(const Interval* i) {
#if defined(__SOFTFP__) || defined(E500V2)
  return i->reg_num() >= LIR_OprDesc::vreg_base;
#else
  return i->reg_num() >= LIR_OprDesc::vreg_base && (i->type() != T_FLOAT && i->type() != T_DOUBLE);
#endif // __SOFTFP__ or E500V2
}

bool LinearScan::is_precolored_fpu_interval(const Interval* i) {
  return i->reg_num() >= LinearScan::nof_cpu_regs && i->reg_num() < LinearScan::nof_regs;
}

bool LinearScan::is_virtual_fpu_interval(const Interval* i) {
#if defined(__SOFTFP__) || defined(E500V2)
  return false;
#else
  return i->reg_num() >= LIR_OprDesc::vreg_base && (i->type() == T_FLOAT || i->type() == T_DOUBLE);
#endif // __SOFTFP__ or E500V2
}

bool LinearScan::is_in_fpu_register(const Interval* i) {
  // fixed intervals not needed for FPU stack allocation
  return i->reg_num() >= nof_regs && pd_first_fpu_reg <= i->assigned_reg() && i->assigned_reg() <= pd_last_fpu_reg;
}

bool LinearScan::is_oop_interval(const Interval* i) {
  // fixed intervals never contain oops
  return i->reg_num() >= nof_regs && i->type() == T_OBJECT;
}


// ********** General helper functions

// compute next unused stack index that can be used for spilling
int LinearScan::allocate_spill_slot(bool double_word) {
  int spill_slot;
  if (double_word) {
    if ((_max_spills & 1) == 1) {
      // alignment of double-word values
      // the hole because of the alignment is filled with the next single-word value
      assert(_unused_spill_slot == -1, "wasting a spill slot");
      _unused_spill_slot = _max_spills;
      _max_spills++;
    }
    spill_slot = _max_spills;
    _max_spills += 2;

  } else if (_unused_spill_slot != -1) {
    // re-use hole that was the result of a previous double-word alignment
    spill_slot = _unused_spill_slot;
    _unused_spill_slot = -1;

  } else {
    spill_slot = _max_spills;
    _max_spills++;
  }

  int result = spill_slot + LinearScan::nof_regs + frame_map()->argcount();

  // if too many slots used, bailout compilation.
  if (result > 2000) {
    bailout("too many stack slots used");
  }

  return result;
}

void LinearScan::assign_spill_slot(Interval* it) {
  // assign the canonical spill slot of the parent (if a part of the interval
  // is already spilled) or allocate a new spill slot
  if (it->canonical_spill_slot() >= 0) {
    it->assign_reg(it->canonical_spill_slot());
  } else {
    int spill = allocate_spill_slot(type2spill_size[it->type()] == 2);
    it->set_canonical_spill_slot(spill);
    it->assign_reg(spill);
  }
}

void LinearScan::propagate_spill_slots() {
  if (!frame_map()->finalize_frame(max_spills())) {
    bailout("frame too large");
  }
}

// create a new interval with a predefined reg_num
// (only used for parent intervals that are created during the building phase)
Interval* LinearScan::create_interval(int reg_num) {
  assert(_intervals.at(reg_num) == NULL, "overwriting exisiting interval");

  Interval* interval = new Interval(reg_num);
  _intervals.at_put(reg_num, interval);

  // assign register number for precolored intervals
  if (reg_num < LIR_OprDesc::vreg_base) {
    interval->assign_reg(reg_num);
  }
  return interval;
}

// assign a new reg_num to the interval and append it to the list of intervals
// (only used for child intervals that are created during register allocation)
void LinearScan::append_interval(Interval* it) {
  it->set_reg_num(_intervals.length());
  _intervals.append(it);
  IntervalList* new_intervals = _new_intervals_from_allocation;
  if (new_intervals == NULL) {
    new_intervals = _new_intervals_from_allocation = new IntervalList();
  }
  new_intervals->append(it);
}

// copy the vreg-flags if an interval is split
void LinearScan::copy_register_flags(Interval* from, Interval* to) {
  if (gen()->is_vreg_flag_set(from->reg_num(), LIRGenerator::byte_reg)) {
    gen()->set_vreg_flag(to->reg_num(), LIRGenerator::byte_reg);
  }
  if (gen()->is_vreg_flag_set(from->reg_num(), LIRGenerator::callee_saved)) {
    gen()->set_vreg_flag(to->reg_num(), LIRGenerator::callee_saved);
  }

  // Note: do not copy the must_start_in_memory flag because it is not necessary for child
  //       intervals (only the very beginning of the interval must be in memory)
}


// ********** spill move optimization
// eliminate moves from register to stack if stack slot is known to be correct

// called during building of intervals
void LinearScan::change_spill_definition_pos(Interval* interval, int def_pos) {
  assert(interval->is_split_parent(), "can only be called for split parents");

  switch (interval->spill_state()) {
    case noDefinitionFound:
      assert(interval->spill_definition_pos() == -1, "must no be set before");
      interval->set_spill_definition_pos(def_pos);
      interval->set_spill_state(oneDefinitionFound);
      break;

    case oneDefinitionFound:
      assert(def_pos <= interval->spill_definition_pos(), "positions are processed in reverse order when intervals are created");
      if (def_pos < interval->spill_definition_pos() - 2) {
        // second definition found, so no spill optimization possible for this interval
        interval->set_spill_state(noOptimization);
      } else {
        // two consecutive definitions (because of two-operand LIR form)
        assert(block_of_op_with_id(def_pos) == block_of_op_with_id(interval->spill_definition_pos()), "block must be equal");
      }
      break;

    case noOptimization:
      // nothing to do
      break;

    default:
      assert(false, "other states not allowed at this time");
  }
}

// called during register allocation
void LinearScan::change_spill_state(Interval* interval, int spill_pos) {
  switch (interval->spill_state()) {
    case oneDefinitionFound: {
      int def_loop_depth = block_of_op_with_id(interval->spill_definition_pos())->loop_depth();
      int spill_loop_depth = block_of_op_with_id(spill_pos)->loop_depth();

      if (def_loop_depth < spill_loop_depth) {
        // the loop depth of the spilling position is higher then the loop depth
        // at the definition of the interval -> move write to memory out of loop
        // by storing at definitin of the interval
        interval->set_spill_state(storeAtDefinition);
      } else {
        // the interval is currently spilled only once, so for now there is no
        // reason to store the interval at the definition
        interval->set_spill_state(oneMoveInserted);
      }
      break;
    }

    case oneMoveInserted: {
      // the interval is spilled more then once, so it is better to store it to
      // memory at the definition
      interval->set_spill_state(storeAtDefinition);
      break;
    }

    case storeAtDefinition:
    case startInMemory:
    case noOptimization:
    case noDefinitionFound:
      // nothing to do
      break;

    default:
      assert(false, "other states not allowed at this time");
  }
}


bool LinearScan::must_store_at_definition(const Interval* i) {
  return i->is_split_parent() && i->spill_state() == storeAtDefinition;
}

// called once before asignment of register numbers
void LinearScan::eliminate_spill_moves() {
  TIME_LINEAR_SCAN(timer_eliminate_spill_moves);
  TRACE_LINEAR_SCAN(3, tty->print_cr("***** Eliminating unnecessary spill moves"));

  // collect all intervals that must be stored after their definion.
  // the list is sorted by Interval::spill_definition_pos
  Interval* interval;
  Interval* temp_list;
  create_unhandled_lists(&interval, &temp_list, must_store_at_definition, NULL);

#ifdef ASSERT
  Interval* prev = NULL;
  Interval* temp = interval;
  while (temp != Interval::end()) {
    assert(temp->spill_definition_pos() > 0, "invalid spill definition pos");
    if (prev != NULL) {
      assert(temp->from() >= prev->from(), "intervals not sorted");
      assert(temp->spill_definition_pos() >= prev->spill_definition_pos(), "when intervals are sorted by from, then they must also be sorted by spill_definition_pos");
    }

    assert(temp->canonical_spill_slot() >= LinearScan::nof_regs, "interval has no spill slot assigned");
    assert(temp->spill_definition_pos() >= temp->from(), "invalid order");
    assert(temp->spill_definition_pos() <= temp->from() + 2, "only intervals defined once at their start-pos can be optimized");

    TRACE_LINEAR_SCAN(4, tty->print_cr("interval %d (from %d to %d) must be stored at %d", temp->reg_num(), temp->from(), temp->to(), temp->spill_definition_pos()));

    temp = temp->next();
  }
#endif

  LIR_InsertionBuffer insertion_buffer;
  int num_blocks = block_count();
  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    LIR_OpList* instructions = block->lir()->instructions_list();
    int         num_inst = instructions->length();
    bool        has_new = false;

    // iterate all instructions of the block. skip the first because it is always a label
    for (int j = 1; j < num_inst; j++) {
      LIR_Op* op = instructions->at(j);
      int op_id = op->id();

      if (op_id == -1) {
        // remove move from register to stack if the stack slot is guaranteed to be correct.
        // only moves that have been inserted by LinearScan can be removed.
        assert(op->code() == lir_move, "only moves can have a op_id of -1");
        assert(op->as_Op1() != NULL, "move must be LIR_Op1");
        assert(op->as_Op1()->result_opr()->is_virtual(), "LinearScan inserts only moves to virtual registers");

        LIR_Op1* op1 = (LIR_Op1*)op;
        Interval* interval = interval_at(op1->result_opr()->vreg_number());

        if (interval->assigned_reg() >= LinearScan::nof_regs && interval->always_in_memory()) {
          // move target is a stack slot that is always correct, so eliminate instruction
          TRACE_LINEAR_SCAN(4, tty->print_cr("eliminating move from interval %d to %d", op1->in_opr()->vreg_number(), op1->result_opr()->vreg_number()));
          instructions->at_put(j, NULL); // NULL-instructions are deleted by assign_reg_num
        }

      } else {
        // insert move from register to stack just after the beginning of the interval
        assert(interval == Interval::end() || interval->spill_definition_pos() >= op_id, "invalid order");
        assert(interval == Interval::end() || (interval->is_split_parent() && interval->spill_state() == storeAtDefinition), "invalid interval");

        while (interval != Interval::end() && interval->spill_definition_pos() == op_id) {
          if (!has_new) {
            // prepare insertion buffer (appended when all instructions of the block are processed)
            insertion_buffer.init(block->lir());
            has_new = true;
          }

          LIR_Opr from_opr = operand_for_interval(interval);
          LIR_Opr to_opr = canonical_spill_opr(interval);
          assert(from_opr->is_fixed_cpu() || from_opr->is_fixed_fpu(), "from operand must be a register");
          assert(to_opr->is_stack(), "to operand must be a stack slot");

          insertion_buffer.move(j, from_opr, to_opr);
          TRACE_LINEAR_SCAN(4, tty->print_cr("inserting move after definition of interval %d to stack slot %d at op_id %d", interval->reg_num(), interval->canonical_spill_slot() - LinearScan::nof_regs, op_id));

          interval = interval->next();
        }
      }
    } // end of instruction iteration

    if (has_new) {
      block->lir()->append(&insertion_buffer);
    }
  } // end of block iteration

  assert(interval == Interval::end(), "missed an interval");
}


// ********** Phase 1: number all instructions in all blocks
// Compute depth-first and linear scan block orders, and number LIR_Op nodes for linear scan.

void LinearScan::number_instructions() {
  {
    // dummy-timer to measure the cost of the timer itself
    // (this time is then subtracted from all other timers to get the real value)
    TIME_LINEAR_SCAN(timer_do_nothing);
  }
  TIME_LINEAR_SCAN(timer_number_instructions);

  // Assign IDs to LIR nodes and build a mapping, lir_ops, from ID to LIR_Op node.
  int num_blocks = block_count();
  int num_instructions = 0;
  int i;
  for (i = 0; i < num_blocks; i++) {
    num_instructions += block_at(i)->lir()->instructions_list()->length();
  }

  // initialize with correct length
  _lir_ops = LIR_OpArray(num_instructions, num_instructions, NULL);
  _block_of_op = BlockBeginArray(num_instructions, num_instructions, NULL);

  int op_id = 0;
  int idx = 0;

  for (i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    block->set_first_lir_instruction_id(op_id);
    LIR_OpList* instructions = block->lir()->instructions_list();

    int num_inst = instructions->length();
    for (int j = 0; j < num_inst; j++) {
      LIR_Op* op = instructions->at(j);
      op->set_id(op_id);

      _lir_ops.at_put(idx, op);
      _block_of_op.at_put(idx, block);
      assert(lir_op_with_id(op_id) == op, "must match");

      idx++;
      op_id += 2; // numbering of lir_ops by two
    }
    block->set_last_lir_instruction_id(op_id - 2);
  }
  assert(idx == num_instructions, "must match");
  assert(idx * 2 == op_id, "must match");

  _has_call.initialize(num_instructions);
  _has_info.initialize(num_instructions);
}


// ********** Phase 2: compute local live sets separately for each block
// (sets live_gen and live_kill for each block)

void LinearScan::set_live_gen_kill(Value value, LIR_Op* op, BitMap& live_gen, BitMap& live_kill) {
  LIR_Opr opr = value->operand();
  Constant* con = value->as_Constant();

  // check some asumptions about debug information
  assert(!value->type()->is_illegal(), "if this local is used by the interpreter it shouldn't be of indeterminate type");
  assert(con == NULL || opr->is_virtual() || opr->is_constant() || opr->is_illegal(), "asumption: Constant instructions have only constant operands");
  assert(con != NULL || opr->is_virtual(), "asumption: non-Constant instructions have only virtual operands");

  if ((con == NULL || con->is_pinned()) && opr->is_register()) {
    assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
    int reg = opr->vreg_number();
    if (!live_kill.at(reg)) {
      live_gen.set_bit(reg);
      TRACE_LINEAR_SCAN(4, tty->print_cr("  Setting live_gen for value %c%d, LIR op_id %d, register number %d", value->type()->tchar(), value->id(), op->id(), reg));
    }
  }
}


void LinearScan::compute_local_live_sets() {
  TIME_LINEAR_SCAN(timer_compute_local_live_sets);

  int  num_blocks = block_count();
  int  live_size = live_set_size();
  bool local_has_fpu_registers = false;
  int  local_num_calls = 0;
  LIR_OpVisitState visitor;

  BitMap2D local_interval_in_loop = BitMap2D(_num_virtual_regs, num_loops());

  // iterate all blocks
  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);

    ResourceBitMap live_gen(live_size);
    ResourceBitMap live_kill(live_size);

    if (block->is_set(BlockBegin::exception_entry_flag)) {
      // Phi functions at the begin of an exception handler are
      // implicitly defined (= killed) at the beginning of the block.
      for_each_phi_fun(block, phi,
        if (!phi->is_illegal()) { live_kill.set_bit(phi->operand()->vreg_number()); }
      );
    }

    LIR_OpList* instructions = block->lir()->instructions_list();
    int num_inst = instructions->length();

    // iterate all instructions of the block. skip the first because it is always a label
    assert(visitor.no_operands(instructions->at(0)), "first operation must always be a label");
    for (int j = 1; j < num_inst; j++) {
      LIR_Op* op = instructions->at(j);

      // visit operation to collect all operands
      visitor.visit(op);

      if (visitor.has_call()) {
        _has_call.set_bit(op->id() >> 1);
        local_num_calls++;
      }
      if (visitor.info_count() > 0) {
        _has_info.set_bit(op->id() >> 1);
      }

      // iterate input operands of instruction
      int k, n, reg;
      n = visitor.opr_count(LIR_OpVisitState::inputMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::inputMode, k);
        assert(opr->is_register(), "visitor should only return register operands");

        if (opr->is_virtual_register()) {
          assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
          reg = opr->vreg_number();
          if (!live_kill.at(reg)) {
            live_gen.set_bit(reg);
            TRACE_LINEAR_SCAN(4, tty->print_cr("  Setting live_gen for register %d at instruction %d", reg, op->id()));
          }
          if (block->loop_index() >= 0) {
            local_interval_in_loop.set_bit(reg, block->loop_index());
          }
          local_has_fpu_registers = local_has_fpu_registers || opr->is_virtual_fpu();
        }

#ifdef ASSERT
        // fixed intervals are never live at block boundaries, so
        // they need not be processed in live sets.
        // this is checked by these assertions to be sure about it.
        // the entry block may have incoming values in registers, which is ok.
        if (!opr->is_virtual_register() && block != ir()->start()) {
          reg = reg_num(opr);
          if (is_processed_reg_num(reg)) {
            assert(live_kill.at(reg), "using fixed register that is not defined in this block");
          }
          reg = reg_numHi(opr);
          if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
            assert(live_kill.at(reg), "using fixed register that is not defined in this block");
          }
        }
#endif
      }

      // Add uses of live locals from interpreter's point of view for proper debug information generation
      n = visitor.info_count();
      for (k = 0; k < n; k++) {
        CodeEmitInfo* info = visitor.info_at(k);
        ValueStack* stack = info->stack();
        for_each_state_value(stack, value,
          set_live_gen_kill(value, op, live_gen, live_kill);
          local_has_fpu_registers = local_has_fpu_registers || value->type()->is_float_kind();
        );
      }

      // iterate temp operands of instruction
      n = visitor.opr_count(LIR_OpVisitState::tempMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::tempMode, k);
        assert(opr->is_register(), "visitor should only return register operands");

        if (opr->is_virtual_register()) {
          assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
          reg = opr->vreg_number();
          live_kill.set_bit(reg);
          if (block->loop_index() >= 0) {
            local_interval_in_loop.set_bit(reg, block->loop_index());
          }
          local_has_fpu_registers = local_has_fpu_registers || opr->is_virtual_fpu();
        }

#ifdef ASSERT
        // fixed intervals are never live at block boundaries, so
        // they need not be processed in live sets
        // process them only in debug mode so that this can be checked
        if (!opr->is_virtual_register()) {
          reg = reg_num(opr);
          if (is_processed_reg_num(reg)) {
            live_kill.set_bit(reg_num(opr));
          }
          reg = reg_numHi(opr);
          if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
            live_kill.set_bit(reg);
          }
        }
#endif
      }

      // iterate output operands of instruction
      n = visitor.opr_count(LIR_OpVisitState::outputMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::outputMode, k);
        assert(opr->is_register(), "visitor should only return register operands");

        if (opr->is_virtual_register()) {
          assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
          reg = opr->vreg_number();
          live_kill.set_bit(reg);
          if (block->loop_index() >= 0) {
            local_interval_in_loop.set_bit(reg, block->loop_index());
          }
          local_has_fpu_registers = local_has_fpu_registers || opr->is_virtual_fpu();
        }

#ifdef ASSERT
        // fixed intervals are never live at block boundaries, so
        // they need not be processed in live sets
        // process them only in debug mode so that this can be checked
        if (!opr->is_virtual_register()) {
          reg = reg_num(opr);
          if (is_processed_reg_num(reg)) {
            live_kill.set_bit(reg_num(opr));
          }
          reg = reg_numHi(opr);
          if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
            live_kill.set_bit(reg);
          }
        }
#endif
      }
    } // end of instruction iteration

    block->set_live_gen (live_gen);
    block->set_live_kill(live_kill);
    block->set_live_in  (ResourceBitMap(live_size));
    block->set_live_out (ResourceBitMap(live_size));

    TRACE_LINEAR_SCAN(4, tty->print("live_gen  B%d ", block->block_id()); print_bitmap(block->live_gen()));
    TRACE_LINEAR_SCAN(4, tty->print("live_kill B%d ", block->block_id()); print_bitmap(block->live_kill()));
  } // end of block iteration

  // propagate local calculated information into LinearScan object
  _has_fpu_registers = local_has_fpu_registers;
  compilation()->set_has_fpu_code(local_has_fpu_registers);

  _num_calls = local_num_calls;
  _interval_in_loop = local_interval_in_loop;
}


// ********** Phase 3: perform a backward dataflow analysis to compute global live sets
// (sets live_in and live_out for each block)

void LinearScan::compute_global_live_sets() {
  TIME_LINEAR_SCAN(timer_compute_global_live_sets);

  int  num_blocks = block_count();
  bool change_occurred;
  bool change_occurred_in_block;
  int  iteration_count = 0;
  ResourceBitMap live_out(live_set_size()); // scratch set for calculations

  // Perform a backward dataflow analysis to compute live_out and live_in for each block.
  // The loop is executed until a fixpoint is reached (no changes in an iteration)
  // Exception handlers must be processed because not all live values are
  // present in the state array, e.g. because of global value numbering
  do {
    change_occurred = false;

    // iterate all blocks in reverse order
    for (int i = num_blocks - 1; i >= 0; i--) {
      BlockBegin* block = block_at(i);

      change_occurred_in_block = false;

      // live_out(block) is the union of live_in(sux), for successors sux of block
      int n = block->number_of_sux();
      int e = block->number_of_exception_handlers();
      if (n + e > 0) {
        // block has successors
        if (n > 0) {
          live_out.set_from(block->sux_at(0)->live_in());
          for (int j = 1; j < n; j++) {
            live_out.set_union(block->sux_at(j)->live_in());
          }
        } else {
          live_out.clear();
        }
        for (int j = 0; j < e; j++) {
          live_out.set_union(block->exception_handler_at(j)->live_in());
        }

        if (!block->live_out().is_same(live_out)) {
          // A change occurred.  Swap the old and new live out sets to avoid copying.
          ResourceBitMap temp = block->live_out();
          block->set_live_out(live_out);
          live_out = temp;

          change_occurred = true;
          change_occurred_in_block = true;
        }
      }

      if (iteration_count == 0 || change_occurred_in_block) {
        // live_in(block) is the union of live_gen(block) with (live_out(block) & !live_kill(block))
        // note: live_in has to be computed only in first iteration or if live_out has changed!
        ResourceBitMap live_in = block->live_in();
        live_in.set_from(block->live_out());
        live_in.set_difference(block->live_kill());
        live_in.set_union(block->live_gen());
      }

#ifdef ASSERT
      if (TraceLinearScanLevel >= 4) {
        char c = ' ';
        if (iteration_count == 0 || change_occurred_in_block) {
          c = '*';
        }
        tty->print("(%d) live_in%c  B%d ", iteration_count, c, block->block_id()); print_bitmap(block->live_in());
        tty->print("(%d) live_out%c B%d ", iteration_count, c, block->block_id()); print_bitmap(block->live_out());
      }
#endif
    }
    iteration_count++;

    if (change_occurred && iteration_count > 50) {
      BAILOUT("too many iterations in compute_global_live_sets");
    }
  } while (change_occurred);


#ifdef ASSERT
  // check that fixed intervals are not live at block boundaries
  // (live set must be empty at fixed intervals)
  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    for (int j = 0; j < LIR_OprDesc::vreg_base; j++) {
      assert(block->live_in().at(j)  == false, "live_in  set of fixed register must be empty");
      assert(block->live_out().at(j) == false, "live_out set of fixed register must be empty");
      assert(block->live_gen().at(j) == false, "live_gen set of fixed register must be empty");
    }
  }
#endif

  // check that the live_in set of the first block is empty
  ResourceBitMap live_in_args(ir()->start()->live_in().size());
  if (!ir()->start()->live_in().is_same(live_in_args)) {
#ifdef ASSERT
    tty->print_cr("Error: live_in set of first block must be empty (when this fails, virtual registers are used before they are defined)");
    tty->print_cr("affected registers:");
    print_bitmap(ir()->start()->live_in());

    // print some additional information to simplify debugging
    for (unsigned int i = 0; i < ir()->start()->live_in().size(); i++) {
      if (ir()->start()->live_in().at(i)) {
        Instruction* instr = gen()->instruction_for_vreg(i);
        tty->print_cr("* vreg %d (HIR instruction %c%d)", i, instr == NULL ? ' ' : instr->type()->tchar(), instr == NULL ? 0 : instr->id());

        for (int j = 0; j < num_blocks; j++) {
          BlockBegin* block = block_at(j);
          if (block->live_gen().at(i)) {
            tty->print_cr("  used in block B%d", block->block_id());
          }
          if (block->live_kill().at(i)) {
            tty->print_cr("  defined in block B%d", block->block_id());
          }
        }
      }
    }

#endif
    // when this fails, virtual registers are used before they are defined.
    assert(false, "live_in set of first block must be empty");
    // bailout of if this occurs in product mode.
    bailout("live_in set of first block not empty");
  }
}


// ********** Phase 4: build intervals
// (fills the list _intervals)

void LinearScan::add_use(Value value, int from, int to, IntervalUseKind use_kind) {
  assert(!value->type()->is_illegal(), "if this value is used by the interpreter it shouldn't be of indeterminate type");
  LIR_Opr opr = value->operand();
  Constant* con = value->as_Constant();

  if ((con == NULL || con->is_pinned()) && opr->is_register()) {
    assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
    add_use(opr, from, to, use_kind);
  }
}


void LinearScan::add_def(LIR_Opr opr, int def_pos, IntervalUseKind use_kind) {
  TRACE_LINEAR_SCAN(2, tty->print(" def "); opr->print(tty); tty->print_cr(" def_pos %d (%d)", def_pos, use_kind));
  assert(opr->is_register(), "should not be called otherwise");

  if (opr->is_virtual_register()) {
    assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
    add_def(opr->vreg_number(), def_pos, use_kind, opr->type_register());

  } else {
    int reg = reg_num(opr);
    if (is_processed_reg_num(reg)) {
      add_def(reg, def_pos, use_kind, opr->type_register());
    }
    reg = reg_numHi(opr);
    if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
      add_def(reg, def_pos, use_kind, opr->type_register());
    }
  }
}

void LinearScan::add_use(LIR_Opr opr, int from, int to, IntervalUseKind use_kind) {
  TRACE_LINEAR_SCAN(2, tty->print(" use "); opr->print(tty); tty->print_cr(" from %d to %d (%d)", from, to, use_kind));
  assert(opr->is_register(), "should not be called otherwise");

  if (opr->is_virtual_register()) {
    assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
    add_use(opr->vreg_number(), from, to, use_kind, opr->type_register());

  } else {
    int reg = reg_num(opr);
    if (is_processed_reg_num(reg)) {
      add_use(reg, from, to, use_kind, opr->type_register());
    }
    reg = reg_numHi(opr);
    if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
      add_use(reg, from, to, use_kind, opr->type_register());
    }
  }
}

void LinearScan::add_temp(LIR_Opr opr, int temp_pos, IntervalUseKind use_kind) {
  TRACE_LINEAR_SCAN(2, tty->print(" temp "); opr->print(tty); tty->print_cr(" temp_pos %d (%d)", temp_pos, use_kind));
  assert(opr->is_register(), "should not be called otherwise");

  if (opr->is_virtual_register()) {
    assert(reg_num(opr) == opr->vreg_number() && !is_valid_reg_num(reg_numHi(opr)), "invalid optimization below");
    add_temp(opr->vreg_number(), temp_pos, use_kind, opr->type_register());

  } else {
    int reg = reg_num(opr);
    if (is_processed_reg_num(reg)) {
      add_temp(reg, temp_pos, use_kind, opr->type_register());
    }
    reg = reg_numHi(opr);
    if (is_valid_reg_num(reg) && is_processed_reg_num(reg)) {
      add_temp(reg, temp_pos, use_kind, opr->type_register());
    }
  }
}


void LinearScan::add_def(int reg_num, int def_pos, IntervalUseKind use_kind, BasicType type) {
  Interval* interval = interval_at(reg_num);
  if (interval != NULL) {
    assert(interval->reg_num() == reg_num, "wrong interval");

    if (type != T_ILLEGAL) {
      interval->set_type(type);
    }

    Range* r = interval->first();
    if (r->from() <= def_pos) {
      // Update the starting point (when a range is first created for a use, its
      // start is the beginning of the current block until a def is encountered.)
      r->set_from(def_pos);
      interval->add_use_pos(def_pos, use_kind);

    } else {
      // Dead value - make vacuous interval
      // also add use_kind for dead intervals
      interval->add_range(def_pos, def_pos + 1);
      interval->add_use_pos(def_pos, use_kind);
      TRACE_LINEAR_SCAN(2, tty->print_cr("Warning: def of reg %d at %d occurs without use", reg_num, def_pos));
    }

  } else {
    // Dead value - make vacuous interval
    // also add use_kind for dead intervals
    interval = create_interval(reg_num);
    if (type != T_ILLEGAL) {
      interval->set_type(type);
    }

    interval->add_range(def_pos, def_pos + 1);
    interval->add_use_pos(def_pos, use_kind);
    TRACE_LINEAR_SCAN(2, tty->print_cr("Warning: dead value %d at %d in live intervals", reg_num, def_pos));
  }

  change_spill_definition_pos(interval, def_pos);
  if (use_kind == noUse && interval->spill_state() <= startInMemory) {
        // detection of method-parameters and roundfp-results
        // TODO: move this directly to position where use-kind is computed
    interval->set_spill_state(startInMemory);
  }
}

void LinearScan::add_use(int reg_num, int from, int to, IntervalUseKind use_kind, BasicType type) {
  Interval* interval = interval_at(reg_num);
  if (interval == NULL) {
    interval = create_interval(reg_num);
  }
  assert(interval->reg_num() == reg_num, "wrong interval");

  if (type != T_ILLEGAL) {
    interval->set_type(type);
  }

  interval->add_range(from, to);
  interval->add_use_pos(to, use_kind);
}

void LinearScan::add_temp(int reg_num, int temp_pos, IntervalUseKind use_kind, BasicType type) {
  Interval* interval = interval_at(reg_num);
  if (interval == NULL) {
    interval = create_interval(reg_num);
  }
  assert(interval->reg_num() == reg_num, "wrong interval");

  if (type != T_ILLEGAL) {
    interval->set_type(type);
  }

  interval->add_range(temp_pos, temp_pos + 1);
  interval->add_use_pos(temp_pos, use_kind);
}


// the results of this functions are used for optimizing spilling and reloading
// if the functions return shouldHaveRegister and the interval is spilled,
// it is not reloaded to a register.
IntervalUseKind LinearScan::use_kind_of_output_operand(LIR_Op* op, LIR_Opr opr) {
  if (op->code() == lir_move) {
    assert(op->as_Op1() != NULL, "lir_move must be LIR_Op1");
    LIR_Op1* move = (LIR_Op1*)op;
    LIR_Opr res = move->result_opr();
    bool result_in_memory = res->is_virtual() && gen()->is_vreg_flag_set(res->vreg_number(), LIRGenerator::must_start_in_memory);

    if (result_in_memory) {
      // Begin of an interval with must_start_in_memory set.
      // This interval will always get a stack slot first, so return noUse.
      return noUse;

    } else if (move->in_opr()->is_stack()) {
      // method argument (condition must be equal to handle_method_arguments)
      return noUse;

    } else if (move->in_opr()->is_register() && move->result_opr()->is_register()) {
      // Move from register to register
      if (block_of_op_with_id(op->id())->is_set(BlockBegin::osr_entry_flag)) {
        // special handling of phi-function moves inside osr-entry blocks
        // input operand must have a register instead of output operand (leads to better register allocation)
        return shouldHaveRegister;
      }
    }
  }

  if (opr->is_virtual() &&
      gen()->is_vreg_flag_set(opr->vreg_number(), LIRGenerator::must_start_in_memory)) {
    // result is a stack-slot, so prevent immediate reloading
    return noUse;
  }

  // all other operands require a register
  return mustHaveRegister;
}

IntervalUseKind LinearScan::use_kind_of_input_operand(LIR_Op* op, LIR_Opr opr) {
  if (op->code() == lir_move) {
    assert(op->as_Op1() != NULL, "lir_move must be LIR_Op1");
    LIR_Op1* move = (LIR_Op1*)op;
    LIR_Opr res = move->result_opr();
    bool result_in_memory = res->is_virtual() && gen()->is_vreg_flag_set(res->vreg_number(), LIRGenerator::must_start_in_memory);

    if (result_in_memory) {
      // Move to an interval with must_start_in_memory set.
      // To avoid moves from stack to stack (not allowed) force the input operand to a register
      return mustHaveRegister;

    } else if (move->in_opr()->is_register() && move->result_opr()->is_register()) {
      // Move from register to register
      if (block_of_op_with_id(op->id())->is_set(BlockBegin::osr_entry_flag)) {
        // special handling of phi-function moves inside osr-entry blocks
        // input operand must have a register instead of output operand (leads to better register allocation)
        return mustHaveRegister;
      }

      // The input operand is not forced to a register (moves from stack to register are allowed),
      // but it is faster if the input operand is in a register
      return shouldHaveRegister;
    }
  }


#if defined(X86) || defined(S390)
  if (op->code() == lir_cmove) {
    // conditional moves can handle stack operands
    assert(op->result_opr()->is_register(), "result must always be in a register");
    return shouldHaveRegister;
  }

  // optimizations for second input operand of arithmehtic operations on Intel
  // this operand is allowed to be on the stack in some cases
  BasicType opr_type = opr->type_register();
  if (opr_type == T_FLOAT || opr_type == T_DOUBLE) {
    if (IA32_ONLY( (UseSSE == 1 && opr_type == T_FLOAT) || UseSSE >= 2 ) NOT_IA32( true )) {
      // SSE float instruction (T_DOUBLE only supported with SSE2)
      switch (op->code()) {
        case lir_cmp:
        case lir_add:
        case lir_sub:
        case lir_mul:
        case lir_div:
        {
          assert(op->as_Op2() != NULL, "must be LIR_Op2");
          LIR_Op2* op2 = (LIR_Op2*)op;
          if (op2->in_opr1() != op2->in_opr2() && op2->in_opr2() == opr) {
            assert((op2->result_opr()->is_register() || op->code() == lir_cmp) && op2->in_opr1()->is_register(), "cannot mark second operand as stack if others are not in register");
            return shouldHaveRegister;
          }
        }
        default:
          break;
      }
    } else {
      // FPU stack float instruction
      switch (op->code()) {
        case lir_add:
        case lir_sub:
        case lir_mul:
        case lir_div:
        {
          assert(op->as_Op2() != NULL, "must be LIR_Op2");
          LIR_Op2* op2 = (LIR_Op2*)op;
          if (op2->in_opr1() != op2->in_opr2() && op2->in_opr2() == opr) {
            assert((op2->result_opr()->is_register() || op->code() == lir_cmp) && op2->in_opr1()->is_register(), "cannot mark second operand as stack if others are not in register");
            return shouldHaveRegister;
          }
        }
        default:
          break;
      }
    }
    // We want to sometimes use logical operations on pointers, in particular in GC barriers.
    // Since 64bit logical operations do not current support operands on stack, we have to make sure
    // T_OBJECT doesn't get spilled along with T_LONG.
  } else if (opr_type != T_LONG LP64_ONLY(&& opr_type != T_OBJECT)) {
    // integer instruction (note: long operands must always be in register)
    switch (op->code()) {
      case lir_cmp:
      case lir_add:
      case lir_sub:
      case lir_logic_and:
      case lir_logic_or:
      case lir_logic_xor:
      {
        assert(op->as_Op2() != NULL, "must be LIR_Op2");
        LIR_Op2* op2 = (LIR_Op2*)op;
        if (op2->in_opr1() != op2->in_opr2() && op2->in_opr2() == opr) {
          assert((op2->result_opr()->is_register() || op->code() == lir_cmp) && op2->in_opr1()->is_register(), "cannot mark second operand as stack if others are not in register");
          return shouldHaveRegister;
        }
      }
      default:
        break;
    }
  }
#endif // X86 || S390

  // all other operands require a register
  return mustHaveRegister;
}


void LinearScan::handle_method_arguments(LIR_Op* op) {
  // special handling for method arguments (moves from stack to virtual register):
  // the interval gets no register assigned, but the stack slot.
  // it is split before the first use by the register allocator.

  if (op->code() == lir_move) {
    assert(op->as_Op1() != NULL, "must be LIR_Op1");
    LIR_Op1* move = (LIR_Op1*)op;

    if (move->in_opr()->is_stack()) {
#ifdef ASSERT
      int arg_size = compilation()->method()->arg_size();
      LIR_Opr o = move->in_opr();
      if (o->is_single_stack()) {
        assert(o->single_stack_ix() >= 0 && o->single_stack_ix() < arg_size, "out of range");
      } else if (o->is_double_stack()) {
        assert(o->double_stack_ix() >= 0 && o->double_stack_ix() < arg_size, "out of range");
      } else {
        ShouldNotReachHere();
      }

      assert(move->id() > 0, "invalid id");
      assert(block_of_op_with_id(move->id())->number_of_preds() == 0, "move from stack must be in first block");
      assert(move->result_opr()->is_virtual(), "result of move must be a virtual register");

      TRACE_LINEAR_SCAN(4, tty->print_cr("found move from stack slot %d to vreg %d", o->is_single_stack() ? o->single_stack_ix() : o->double_stack_ix(), reg_num(move->result_opr())));
#endif

      Interval* interval = interval_at(reg_num(move->result_opr()));

      int stack_slot = LinearScan::nof_regs + (move->in_opr()->is_single_stack() ? move->in_opr()->single_stack_ix() : move->in_opr()->double_stack_ix());
      interval->set_canonical_spill_slot(stack_slot);
      interval->assign_reg(stack_slot);
    }
  }
}

void LinearScan::handle_doubleword_moves(LIR_Op* op) {
  // special handling for doubleword move from memory to register:
  // in this case the registers of the input address and the result
  // registers must not overlap -> add a temp range for the input registers
  if (op->code() == lir_move) {
    assert(op->as_Op1() != NULL, "must be LIR_Op1");
    LIR_Op1* move = (LIR_Op1*)op;

    if (move->result_opr()->is_double_cpu() && move->in_opr()->is_pointer()) {
      LIR_Address* address = move->in_opr()->as_address_ptr();
      if (address != NULL) {
        if (address->base()->is_valid()) {
          add_temp(address->base(), op->id(), noUse);
        }
        if (address->index()->is_valid()) {
          add_temp(address->index(), op->id(), noUse);
        }
      }
    }
  }
}

void LinearScan::add_register_hints(LIR_Op* op) {
  switch (op->code()) {
    case lir_move:      // fall through
    case lir_convert: {
      assert(op->as_Op1() != NULL, "lir_move, lir_convert must be LIR_Op1");
      LIR_Op1* move = (LIR_Op1*)op;

      LIR_Opr move_from = move->in_opr();
      LIR_Opr move_to = move->result_opr();

      if (move_to->is_register() && move_from->is_register()) {
        Interval* from = interval_at(reg_num(move_from));
        Interval* to = interval_at(reg_num(move_to));
        if (from != NULL && to != NULL) {
          to->set_register_hint(from);
          TRACE_LINEAR_SCAN(4, tty->print_cr("operation at op_id %d: added hint from interval %d to %d", move->id(), from->reg_num(), to->reg_num()));
        }
      }
      break;
    }
    case lir_cmove: {
      assert(op->as_Op2() != NULL, "lir_cmove must be LIR_Op2");
      LIR_Op2* cmove = (LIR_Op2*)op;

      LIR_Opr move_from = cmove->in_opr1();
      LIR_Opr move_to = cmove->result_opr();

      if (move_to->is_register() && move_from->is_register()) {
        Interval* from = interval_at(reg_num(move_from));
        Interval* to = interval_at(reg_num(move_to));
        if (from != NULL && to != NULL) {
          to->set_register_hint(from);
          TRACE_LINEAR_SCAN(4, tty->print_cr("operation at op_id %d: added hint from interval %d to %d", cmove->id(), from->reg_num(), to->reg_num()));
        }
      }
      break;
    }
    default:
      break;
  }
}


void LinearScan::build_intervals() {
  TIME_LINEAR_SCAN(timer_build_intervals);

  // initialize interval list with expected number of intervals
  // (32 is added to have some space for split children without having to resize the list)
  _intervals = IntervalList(num_virtual_regs() + 32);
  // initialize all slots that are used by build_intervals
  _intervals.at_put_grow(num_virtual_regs() - 1, NULL, NULL);

  // create a list with all caller-save registers (cpu, fpu, xmm)
  // when an instruction is a call, a temp range is created for all these registers
  int num_caller_save_registers = 0;
  int caller_save_registers[LinearScan::nof_regs];

  int i;
  for (i = 0; i < FrameMap::nof_caller_save_cpu_regs(); i++) {
    LIR_Opr opr = FrameMap::caller_save_cpu_reg_at(i);
    assert(opr->is_valid() && opr->is_register(), "FrameMap should not return invalid operands");
    assert(reg_numHi(opr) == -1, "missing addition of range for hi-register");
    caller_save_registers[num_caller_save_registers++] = reg_num(opr);
  }

  // temp ranges for fpu registers are only created when the method has
  // virtual fpu operands. Otherwise no allocation for fpu registers is
  // performed and so the temp ranges would be useless
  if (has_fpu_registers()) {
#ifdef X86
    if (UseSSE < 2) {
#endif // X86
      for (i = 0; i < FrameMap::nof_caller_save_fpu_regs; i++) {
        LIR_Opr opr = FrameMap::caller_save_fpu_reg_at(i);
        assert(opr->is_valid() && opr->is_register(), "FrameMap should not return invalid operands");
        assert(reg_numHi(opr) == -1, "missing addition of range for hi-register");
        caller_save_registers[num_caller_save_registers++] = reg_num(opr);
      }
#ifdef X86
    }
#endif // X86

#ifdef X86
    if (UseSSE > 0) {
      int num_caller_save_xmm_regs = FrameMap::get_num_caller_save_xmms();
      for (i = 0; i < num_caller_save_xmm_regs; i ++) {
        LIR_Opr opr = FrameMap::caller_save_xmm_reg_at(i);
        assert(opr->is_valid() && opr->is_register(), "FrameMap should not return invalid operands");
        assert(reg_numHi(opr) == -1, "missing addition of range for hi-register");
        caller_save_registers[num_caller_save_registers++] = reg_num(opr);
      }
    }
#endif // X86
  }
  assert(num_caller_save_registers <= LinearScan::nof_regs, "out of bounds");


  LIR_OpVisitState visitor;

  // iterate all blocks in reverse order
  for (i = block_count() - 1; i >= 0; i--) {
    BlockBegin* block = block_at(i);
    LIR_OpList* instructions = block->lir()->instructions_list();
    int         block_from =   block->first_lir_instruction_id();
    int         block_to =     block->last_lir_instruction_id();

    assert(block_from == instructions->at(0)->id(), "must be");
    assert(block_to   == instructions->at(instructions->length() - 1)->id(), "must be");

    // Update intervals for registers live at the end of this block;
    ResourceBitMap live = block->live_out();
    int size = (int)live.size();
    for (int number = (int)live.get_next_one_offset(0, size); number < size; number = (int)live.get_next_one_offset(number + 1, size)) {
      assert(live.at(number), "should not stop here otherwise");
      assert(number >= LIR_OprDesc::vreg_base, "fixed intervals must not be live on block bounds");
      TRACE_LINEAR_SCAN(2, tty->print_cr("live in %d to %d", number, block_to + 2));

      add_use(number, block_from, block_to + 2, noUse, T_ILLEGAL);

      // add special use positions for loop-end blocks when the
      // interval is used anywhere inside this loop.  It's possible
      // that the block was part of a non-natural loop, so it might
      // have an invalid loop index.
      if (block->is_set(BlockBegin::linear_scan_loop_end_flag) &&
          block->loop_index() != -1 &&
          is_interval_in_loop(number, block->loop_index())) {
        interval_at(number)->add_use_pos(block_to + 1, loopEndMarker);
      }
    }

    // iterate all instructions of the block in reverse order.
    // skip the first instruction because it is always a label
    // definitions of intervals are processed before uses
    assert(visitor.no_operands(instructions->at(0)), "first operation must always be a label");
    for (int j = instructions->length() - 1; j >= 1; j--) {
      LIR_Op* op = instructions->at(j);
      int op_id = op->id();

      // visit operation to collect all operands
      visitor.visit(op);

      // add a temp range for each register if operation destroys caller-save registers
      if (visitor.has_call()) {
        for (int k = 0; k < num_caller_save_registers; k++) {
          add_temp(caller_save_registers[k], op_id, noUse, T_ILLEGAL);
        }
        TRACE_LINEAR_SCAN(4, tty->print_cr("operation destroys all caller-save registers"));
      }

      // Add any platform dependent temps
      pd_add_temps(op);

      // visit definitions (output and temp operands)
      int k, n;
      n = visitor.opr_count(LIR_OpVisitState::outputMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::outputMode, k);
        assert(opr->is_register(), "visitor should only return register operands");
        add_def(opr, op_id, use_kind_of_output_operand(op, opr));
      }

      n = visitor.opr_count(LIR_OpVisitState::tempMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::tempMode, k);
        assert(opr->is_register(), "visitor should only return register operands");
        add_temp(opr, op_id, mustHaveRegister);
      }

      // visit uses (input operands)
      n = visitor.opr_count(LIR_OpVisitState::inputMode);
      for (k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::inputMode, k);
        assert(opr->is_register(), "visitor should only return register operands");
        add_use(opr, block_from, op_id, use_kind_of_input_operand(op, opr));
      }

      // Add uses of live locals from interpreter's point of view for proper
      // debug information generation
      // Treat these operands as temp values (if the life range is extended
      // to a call site, the value would be in a register at the call otherwise)
      n = visitor.info_count();
      for (k = 0; k < n; k++) {
        CodeEmitInfo* info = visitor.info_at(k);
        ValueStack* stack = info->stack();
        for_each_state_value(stack, value,
          add_use(value, block_from, op_id + 1, noUse);
        );
      }

      // special steps for some instructions (especially moves)
      handle_method_arguments(op);
      handle_doubleword_moves(op);
      add_register_hints(op);

    } // end of instruction iteration
  } // end of block iteration


  // add the range [0, 1[ to all fixed intervals
  // -> the register allocator need not handle unhandled fixed intervals
  for (int n = 0; n < LinearScan::nof_regs; n++) {
    Interval* interval = interval_at(n);
    if (interval != NULL) {
      interval->add_range(0, 1);
    }
  }
}


// ********** Phase 5: actual register allocation

int LinearScan::interval_cmp(Interval** a, Interval** b) {
  if (*a != NULL) {
    if (*b != NULL) {
      return (*a)->from() - (*b)->from();
    } else {
      return -1;
    }
  } else {
    if (*b != NULL) {
      return 1;
    } else {
      return 0;
    }
  }
}

#ifndef PRODUCT
int interval_cmp(Interval* const& l, Interval* const& r) {
  return l->from() - r->from();
}

bool find_interval(Interval* interval, IntervalArray* intervals) {
  bool found;
  int idx = intervals->find_sorted<Interval*, interval_cmp>(interval, found);

  if (!found) {
    return false;
  }

  int from = interval->from();

  // The index we've found using binary search is pointing to an interval
  // that is defined in the same place as the interval we were looking for.
  // So now we have to look around that index and find exact interval.
  for (int i = idx; i >= 0; i--) {
    if (intervals->at(i) == interval) {
      return true;
    }
    if (intervals->at(i)->from() != from) {
      break;
    }
  }

  for (int i = idx + 1; i < intervals->length(); i++) {
    if (intervals->at(i) == interval) {
      return true;
    }
    if (intervals->at(i)->from() != from) {
      break;
    }
  }

  return false;
}

bool LinearScan::is_sorted(IntervalArray* intervals) {
  int from = -1;
  int null_count = 0;

  for (int i = 0; i < intervals->length(); i++) {
    Interval* it = intervals->at(i);
    if (it != NULL) {
      assert(from <= it->from(), "Intervals are unordered");
      from = it->from();
    } else {
      null_count++;
    }
  }

  assert(null_count == 0, "Sorted intervals should not contain nulls");

  null_count = 0;

  for (int i = 0; i < interval_count(); i++) {
    Interval* interval = interval_at(i);
    if (interval != NULL) {
      assert(find_interval(interval, intervals), "Lists do not contain same intervals");
    } else {
      null_count++;
    }
  }

  assert(interval_count() - null_count == intervals->length(),
      "Sorted list should contain the same amount of non-NULL intervals as unsorted list");

  return true;
}
#endif

void LinearScan::add_to_list(Interval** first, Interval** prev, Interval* interval) {
  if (*prev != NULL) {
    (*prev)->set_next(interval);
  } else {
    *first = interval;
  }
  *prev = interval;
}

void LinearScan::create_unhandled_lists(Interval** list1, Interval** list2, bool (is_list1)(const Interval* i), bool (is_list2)(const Interval* i)) {
  assert(is_sorted(_sorted_intervals), "interval list is not sorted");

  *list1 = *list2 = Interval::end();

  Interval* list1_prev = NULL;
  Interval* list2_prev = NULL;
  Interval* v;

  const int n = _sorted_intervals->length();
  for (int i = 0; i < n; i++) {
    v = _sorted_intervals->at(i);
    if (v == NULL) continue;

    if (is_list1(v)) {
      add_to_list(list1, &list1_prev, v);
    } else if (is_list2 == NULL || is_list2(v)) {
      add_to_list(list2, &list2_prev, v);
    }
  }

  if (list1_prev != NULL) list1_prev->set_next(Interval::end());
  if (list2_prev != NULL) list2_prev->set_next(Interval::end());

  assert(list1_prev == NULL || list1_prev->next() == Interval::end(), "linear list ends not with sentinel");
  assert(list2_prev == NULL || list2_prev->next() == Interval::end(), "linear list ends not with sentinel");
}


void LinearScan::sort_intervals_before_allocation() {
  TIME_LINEAR_SCAN(timer_sort_intervals_before);

  if (_needs_full_resort) {
    // There is no known reason why this should occur but just in case...
    assert(false, "should never occur");
    // Re-sort existing interval list because an Interval::from() has changed
    _sorted_intervals->sort(interval_cmp);
    _needs_full_resort = false;
  }

  IntervalList* unsorted_list = &_intervals;
  int unsorted_len = unsorted_list->length();
  int sorted_len = 0;
  int unsorted_idx;
  int sorted_idx = 0;
  int sorted_from_max = -1;

  // calc number of items for sorted list (sorted list must not contain NULL values)
  for (unsorted_idx = 0; unsorted_idx < unsorted_len; unsorted_idx++) {
    if (unsorted_list->at(unsorted_idx) != NULL) {
      sorted_len++;
    }
  }
  IntervalArray* sorted_list = new IntervalArray(sorted_len, sorted_len, NULL);

  // special sorting algorithm: the original interval-list is almost sorted,
  // only some intervals are swapped. So this is much faster than a complete QuickSort
  for (unsorted_idx = 0; unsorted_idx < unsorted_len; unsorted_idx++) {
    Interval* cur_interval = unsorted_list->at(unsorted_idx);

    if (cur_interval != NULL) {
      int cur_from = cur_interval->from();

      if (sorted_from_max <= cur_from) {
        sorted_list->at_put(sorted_idx++, cur_interval);
        sorted_from_max = cur_interval->from();
      } else {
        // the asumption that the intervals are already sorted failed,
        // so this interval must be sorted in manually
        int j;
        for (j = sorted_idx - 1; j >= 0 && cur_from < sorted_list->at(j)->from(); j--) {
          sorted_list->at_put(j + 1, sorted_list->at(j));
        }
        sorted_list->at_put(j + 1, cur_interval);
        sorted_idx++;
      }
    }
  }
  _sorted_intervals = sorted_list;
  assert(is_sorted(_sorted_intervals), "intervals unsorted");
}

void LinearScan::sort_intervals_after_allocation() {
  TIME_LINEAR_SCAN(timer_sort_intervals_after);

  if (_needs_full_resort) {
    // Re-sort existing interval list because an Interval::from() has changed
    _sorted_intervals->sort(interval_cmp);
    _needs_full_resort = false;
  }

  IntervalArray* old_list = _sorted_intervals;
  IntervalList* new_list = _new_intervals_from_allocation;
  int old_len = old_list->length();
  int new_len = new_list == NULL ? 0 : new_list->length();

  if (new_len == 0) {
    // no intervals have been added during allocation, so sorted list is already up to date
    assert(is_sorted(_sorted_intervals), "intervals unsorted");
    return;
  }

  // conventional sort-algorithm for new intervals
  new_list->sort(interval_cmp);

  // merge old and new list (both already sorted) into one combined list
  int combined_list_len = old_len + new_len;
  IntervalArray* combined_list = new IntervalArray(combined_list_len, combined_list_len, NULL);
  int old_idx = 0;
  int new_idx = 0;

  while (old_idx + new_idx < old_len + new_len) {
    if (new_idx >= new_len || (old_idx < old_len && old_list->at(old_idx)->from() <= new_list->at(new_idx)->from())) {
      combined_list->at_put(old_idx + new_idx, old_list->at(old_idx));
      old_idx++;
    } else {
      combined_list->at_put(old_idx + new_idx, new_list->at(new_idx));
      new_idx++;
    }
  }

  _sorted_intervals = combined_list;
  assert(is_sorted(_sorted_intervals), "intervals unsorted");
}


void LinearScan::allocate_registers() {
  TIME_LINEAR_SCAN(timer_allocate_registers);

  Interval* precolored_cpu_intervals, *not_precolored_cpu_intervals;
  Interval* precolored_fpu_intervals, *not_precolored_fpu_intervals;

  // collect cpu intervals
  create_unhandled_lists(&precolored_cpu_intervals, &not_precolored_cpu_intervals,
                         is_precolored_cpu_interval, is_virtual_cpu_interval);

  // collect fpu intervals
  create_unhandled_lists(&precolored_fpu_intervals, &not_precolored_fpu_intervals,
                         is_precolored_fpu_interval, is_virtual_fpu_interval);
  // this fpu interval collection cannot be moved down below with the allocation section as
  // the cpu_lsw.walk() changes interval positions.

  if (!has_fpu_registers()) {
#ifdef ASSERT
    assert(not_precolored_fpu_intervals == Interval::end(), "missed an uncolored fpu interval");
#else
    if (not_precolored_fpu_intervals != Interval::end()) {
      BAILOUT("missed an uncolored fpu interval");
    }
#endif
  }

  // allocate cpu registers
  LinearScanWalker cpu_lsw(this, precolored_cpu_intervals, not_precolored_cpu_intervals);
  cpu_lsw.walk();
  cpu_lsw.finish_allocation();

  if (has_fpu_registers()) {
    // allocate fpu registers
    LinearScanWalker fpu_lsw(this, precolored_fpu_intervals, not_precolored_fpu_intervals);
    fpu_lsw.walk();
    fpu_lsw.finish_allocation();
  }
}


// ********** Phase 6: resolve data flow
// (insert moves at edges between blocks if intervals have been split)

// wrapper for Interval::split_child_at_op_id that performs a bailout in product mode
// instead of returning NULL
Interval* LinearScan::split_child_at_op_id(Interval* interval, int op_id, LIR_OpVisitState::OprMode mode) {
  Interval* result = interval->split_child_at_op_id(op_id, mode);
  if (result != NULL) {
    return result;
  }

  assert(false, "must find an interval, but do a clean bailout in product mode");
  result = new Interval(LIR_OprDesc::vreg_base);
  result->assign_reg(0);
  result->set_type(T_INT);
  BAILOUT_("LinearScan: interval is NULL", result);
}


Interval* LinearScan::interval_at_block_begin(BlockBegin* block, int reg_num) {
  assert(LinearScan::nof_regs <= reg_num && reg_num < num_virtual_regs(), "register number out of bounds");
  assert(interval_at(reg_num) != NULL, "no interval found");

  return split_child_at_op_id(interval_at(reg_num), block->first_lir_instruction_id(), LIR_OpVisitState::outputMode);
}

Interval* LinearScan::interval_at_block_end(BlockBegin* block, int reg_num) {
  assert(LinearScan::nof_regs <= reg_num && reg_num < num_virtual_regs(), "register number out of bounds");
  assert(interval_at(reg_num) != NULL, "no interval found");

  return split_child_at_op_id(interval_at(reg_num), block->last_lir_instruction_id() + 1, LIR_OpVisitState::outputMode);
}

Interval* LinearScan::interval_at_op_id(int reg_num, int op_id) {
  assert(LinearScan::nof_regs <= reg_num && reg_num < num_virtual_regs(), "register number out of bounds");
  assert(interval_at(reg_num) != NULL, "no interval found");

  return split_child_at_op_id(interval_at(reg_num), op_id, LIR_OpVisitState::inputMode);
}


void LinearScan::resolve_collect_mappings(BlockBegin* from_block, BlockBegin* to_block, MoveResolver &move_resolver) {
  DEBUG_ONLY(move_resolver.check_empty());

  const int size = live_set_size();
  const ResourceBitMap live_at_edge = to_block->live_in();

  // visit all registers where the live_at_edge bit is set
  for (int r = (int)live_at_edge.get_next_one_offset(0, size); r < size; r = (int)live_at_edge.get_next_one_offset(r + 1, size)) {
    assert(r < num_virtual_regs(), "live information set for not exisiting interval");
    assert(from_block->live_out().at(r) && to_block->live_in().at(r), "interval not live at this edge");

    Interval* from_interval = interval_at_block_end(from_block, r);
    Interval* to_interval = interval_at_block_begin(to_block, r);

    if (from_interval != to_interval && (from_interval->assigned_reg() != to_interval->assigned_reg() || from_interval->assigned_regHi() != to_interval->assigned_regHi())) {
      // need to insert move instruction
      move_resolver.add_mapping(from_interval, to_interval);
    }
  }
}


void LinearScan::resolve_find_insert_pos(BlockBegin* from_block, BlockBegin* to_block, MoveResolver &move_resolver) {
  if (from_block->number_of_sux() <= 1) {
    TRACE_LINEAR_SCAN(4, tty->print_cr("inserting moves at end of from_block B%d", from_block->block_id()));

    LIR_OpList* instructions = from_block->lir()->instructions_list();
    LIR_OpBranch* branch = instructions->last()->as_OpBranch();
    if (branch != NULL) {
      // insert moves before branch
      assert(branch->cond() == lir_cond_always, "block does not end with an unconditional jump");
      move_resolver.set_insert_position(from_block->lir(), instructions->length() - 2);
    } else {
      move_resolver.set_insert_position(from_block->lir(), instructions->length() - 1);
    }

  } else {
    TRACE_LINEAR_SCAN(4, tty->print_cr("inserting moves at beginning of to_block B%d", to_block->block_id()));
#ifdef ASSERT
    assert(from_block->lir()->instructions_list()->at(0)->as_OpLabel() != NULL, "block does not start with a label");

    // because the number of predecessor edges matches the number of
    // successor edges, blocks which are reached by switch statements
    // may have be more than one predecessor but it will be guaranteed
    // that all predecessors will be the same.
    for (int i = 0; i < to_block->number_of_preds(); i++) {
      assert(from_block == to_block->pred_at(i), "all critical edges must be broken");
    }
#endif

    move_resolver.set_insert_position(to_block->lir(), 0);
  }
}


// insert necessary moves (spilling or reloading) at edges between blocks if interval has been split
void LinearScan::resolve_data_flow() {
  TIME_LINEAR_SCAN(timer_resolve_data_flow);

  int num_blocks = block_count();
  MoveResolver move_resolver(this);
  ResourceBitMap block_completed(num_blocks);
  ResourceBitMap already_resolved(num_blocks);

  int i;
  for (i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);

    // check if block has only one predecessor and only one successor
    if (block->number_of_preds() == 1 && block->number_of_sux() == 1 && block->number_of_exception_handlers() == 0) {
      LIR_OpList* instructions = block->lir()->instructions_list();
      assert(instructions->at(0)->code() == lir_label, "block must start with label");
      assert(instructions->last()->code() == lir_branch, "block with successors must end with branch");
      assert(instructions->last()->as_OpBranch()->cond() == lir_cond_always, "block with successor must end with unconditional branch");

      // check if block is empty (only label and branch)
      if (instructions->length() == 2) {
        BlockBegin* pred = block->pred_at(0);
        BlockBegin* sux = block->sux_at(0);

        // prevent optimization of two consecutive blocks
        if (!block_completed.at(pred->linear_scan_number()) && !block_completed.at(sux->linear_scan_number())) {
          TRACE_LINEAR_SCAN(3, tty->print_cr("**** optimizing empty block B%d (pred: B%d, sux: B%d)", block->block_id(), pred->block_id(), sux->block_id()));
          block_completed.set_bit(block->linear_scan_number());

          // directly resolve between pred and sux (without looking at the empty block between)
          resolve_collect_mappings(pred, sux, move_resolver);
          if (move_resolver.has_mappings()) {
            move_resolver.set_insert_position(block->lir(), 0);
            move_resolver.resolve_and_append_moves();
          }
        }
      }
    }
  }


  for (i = 0; i < num_blocks; i++) {
    if (!block_completed.at(i)) {
      BlockBegin* from_block = block_at(i);
      already_resolved.set_from(block_completed);

      int num_sux = from_block->number_of_sux();
      for (int s = 0; s < num_sux; s++) {
        BlockBegin* to_block = from_block->sux_at(s);

        // check for duplicate edges between the same blocks (can happen with switch blocks)
        if (!already_resolved.at(to_block->linear_scan_number())) {
          TRACE_LINEAR_SCAN(3, tty->print_cr("**** processing edge between B%d and B%d", from_block->block_id(), to_block->block_id()));
          already_resolved.set_bit(to_block->linear_scan_number());

          // collect all intervals that have been split between from_block and to_block
          resolve_collect_mappings(from_block, to_block, move_resolver);
          if (move_resolver.has_mappings()) {
            resolve_find_insert_pos(from_block, to_block, move_resolver);
            move_resolver.resolve_and_append_moves();
          }
        }
      }
    }
  }
}


void LinearScan::resolve_exception_entry(BlockBegin* block, int reg_num, MoveResolver &move_resolver) {
  if (interval_at(reg_num) == NULL) {
    // if a phi function is never used, no interval is created -> ignore this
    return;
  }

  Interval* interval = interval_at_block_begin(block, reg_num);
  int reg = interval->assigned_reg();
  int regHi = interval->assigned_regHi();

  if ((reg < nof_regs && interval->always_in_memory()) ||
      (use_fpu_stack_allocation() && reg >= pd_first_fpu_reg && reg <= pd_last_fpu_reg)) {
    // the interval is split to get a short range that is located on the stack
    // in the following two cases:
    // * the interval started in memory (e.g. method parameter), but is currently in a register
    //   this is an optimization for exception handling that reduces the number of moves that
    //   are necessary for resolving the states when an exception uses this exception handler
    // * the interval would be on the fpu stack at the begin of the exception handler
    //   this is not allowed because of the complicated fpu stack handling on Intel

    // range that will be spilled to memory
    int from_op_id = block->first_lir_instruction_id();
    int to_op_id = from_op_id + 1;  // short live range of length 1
    assert(interval->from() <= from_op_id && interval->to() >= to_op_id,
           "no split allowed between exception entry and first instruction");

    if (interval->from() != from_op_id) {
      // the part before from_op_id is unchanged
      interval = interval->split(from_op_id);
      interval->assign_reg(reg, regHi);
      append_interval(interval);
    } else {
      _needs_full_resort = true;
    }
    assert(interval->from() == from_op_id, "must be true now");

    Interval* spilled_part = interval;
    if (interval->to() != to_op_id) {
      // the part after to_op_id is unchanged
      spilled_part = interval->split_from_start(to_op_id);
      append_interval(spilled_part);
      move_resolver.add_mapping(spilled_part, interval);
    }
    assign_spill_slot(spilled_part);

    assert(spilled_part->from() == from_op_id && spilled_part->to() == to_op_id, "just checking");
  }
}

void LinearScan::resolve_exception_entry(BlockBegin* block, MoveResolver &move_resolver) {
  assert(block->is_set(BlockBegin::exception_entry_flag), "should not call otherwise");
  DEBUG_ONLY(move_resolver.check_empty());

  // visit all registers where the live_in bit is set
  int size = live_set_size();
  for (int r = (int)block->live_in().get_next_one_offset(0, size); r < size; r = (int)block->live_in().get_next_one_offset(r + 1, size)) {
    resolve_exception_entry(block, r, move_resolver);
  }

  // the live_in bits are not set for phi functions of the xhandler entry, so iterate them separately
  for_each_phi_fun(block, phi,
    if (!phi->is_illegal()) { resolve_exception_entry(block, phi->operand()->vreg_number(), move_resolver); }
  );

  if (move_resolver.has_mappings()) {
    // insert moves after first instruction
    move_resolver.set_insert_position(block->lir(), 0);
    move_resolver.resolve_and_append_moves();
  }
}


void LinearScan::resolve_exception_edge(XHandler* handler, int throwing_op_id, int reg_num, Phi* phi, MoveResolver &move_resolver) {
  if (interval_at(reg_num) == NULL) {
    // if a phi function is never used, no interval is created -> ignore this
    return;
  }

  // the computation of to_interval is equal to resolve_collect_mappings,
  // but from_interval is more complicated because of phi functions
  BlockBegin* to_block = handler->entry_block();
  Interval* to_interval = interval_at_block_begin(to_block, reg_num);

  if (phi != NULL) {
    // phi function of the exception entry block
    // no moves are created for this phi function in the LIR_Generator, so the
    // interval at the throwing instruction must be searched using the operands
    // of the phi function
    Value from_value = phi->operand_at(handler->phi_operand());

    // with phi functions it can happen that the same from_value is used in
    // multiple mappings, so notify move-resolver that this is allowed
    move_resolver.set_multiple_reads_allowed();

    Constant* con = from_value->as_Constant();
    if (con != NULL && (!con->is_pinned() || con->operand()->is_constant())) {
      // Need a mapping from constant to interval if unpinned (may have no register) or if the operand is a constant (no register).
      move_resolver.add_mapping(LIR_OprFact::value_type(con->type()), to_interval);
    } else {
      // search split child at the throwing op_id
      Interval* from_interval = interval_at_op_id(from_value->operand()->vreg_number(), throwing_op_id);
      move_resolver.add_mapping(from_interval, to_interval);
    }
  } else {
    // no phi function, so use reg_num also for from_interval
    // search split child at the throwing op_id
    Interval* from_interval = interval_at_op_id(reg_num, throwing_op_id);
    if (from_interval != to_interval) {
      // optimization to reduce number of moves: when to_interval is on stack and
      // the stack slot is known to be always correct, then no move is necessary
      if (!from_interval->always_in_memory() || from_interval->canonical_spill_slot() != to_interval->assigned_reg()) {
        move_resolver.add_mapping(from_interval, to_interval);
      }
    }
  }
}

void LinearScan::resolve_exception_edge(XHandler* handler, int throwing_op_id, MoveResolver &move_resolver) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("resolving exception handler B%d: throwing_op_id=%d", handler->entry_block()->block_id(), throwing_op_id));

  DEBUG_ONLY(move_resolver.check_empty());
  assert(handler->lir_op_id() == -1, "already processed this xhandler");
  DEBUG_ONLY(handler->set_lir_op_id(throwing_op_id));
  assert(handler->entry_code() == NULL, "code already present");

  // visit all registers where the live_in bit is set
  BlockBegin* block = handler->entry_block();
  int size = live_set_size();
  for (int r = (int)block->live_in().get_next_one_offset(0, size); r < size; r = (int)block->live_in().get_next_one_offset(r + 1, size)) {
    resolve_exception_edge(handler, throwing_op_id, r, NULL, move_resolver);
  }

  // the live_in bits are not set for phi functions of the xhandler entry, so iterate them separately
  for_each_phi_fun(block, phi,
    if (!phi->is_illegal()) { resolve_exception_edge(handler, throwing_op_id, phi->operand()->vreg_number(), phi, move_resolver); }
  );

  if (move_resolver.has_mappings()) {
    LIR_List* entry_code = new LIR_List(compilation());
    move_resolver.set_insert_position(entry_code, 0);
    move_resolver.resolve_and_append_moves();

    entry_code->jump(handler->entry_block());
    handler->set_entry_code(entry_code);
  }
}


void LinearScan::resolve_exception_handlers() {
  MoveResolver move_resolver(this);
  LIR_OpVisitState visitor;
  int num_blocks = block_count();

  int i;
  for (i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    if (block->is_set(BlockBegin::exception_entry_flag)) {
      resolve_exception_entry(block, move_resolver);
    }
  }

  for (i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    LIR_List* ops = block->lir();
    int num_ops = ops->length();

    // iterate all instructions of the block. skip the first because it is always a label
    assert(visitor.no_operands(ops->at(0)), "first operation must always be a label");
    for (int j = 1; j < num_ops; j++) {
      LIR_Op* op = ops->at(j);
      int op_id = op->id();

      if (op_id != -1 && has_info(op_id)) {
        // visit operation to collect all operands
        visitor.visit(op);
        assert(visitor.info_count() > 0, "should not visit otherwise");

        XHandlers* xhandlers = visitor.all_xhandler();
        int n = xhandlers->length();
        for (int k = 0; k < n; k++) {
          resolve_exception_edge(xhandlers->handler_at(k), op_id, move_resolver);
        }

#ifdef ASSERT
      } else {
        visitor.visit(op);
        assert(visitor.all_xhandler()->length() == 0, "missed exception handler");
#endif
      }
    }
  }
}


// ********** Phase 7: assign register numbers back to LIR
// (includes computation of debug information and oop maps)

VMReg LinearScan::vm_reg_for_interval(Interval* interval) {
  VMReg reg = interval->cached_vm_reg();
  if (!reg->is_valid() ) {
    reg = vm_reg_for_operand(operand_for_interval(interval));
    interval->set_cached_vm_reg(reg);
  }
  assert(reg == vm_reg_for_operand(operand_for_interval(interval)), "wrong cached value");
  return reg;
}

VMReg LinearScan::vm_reg_for_operand(LIR_Opr opr) {
  assert(opr->is_oop(), "currently only implemented for oop operands");
  return frame_map()->regname(opr);
}


LIR_Opr LinearScan::operand_for_interval(Interval* interval) {
  LIR_Opr opr = interval->cached_opr();
  if (opr->is_illegal()) {
    opr = calc_operand_for_interval(interval);
    interval->set_cached_opr(opr);
  }

  assert(opr == calc_operand_for_interval(interval), "wrong cached value");
  return opr;
}

LIR_Opr LinearScan::calc_operand_for_interval(const Interval* interval) {
  int assigned_reg = interval->assigned_reg();
  BasicType type = interval->type();

  if (assigned_reg >= nof_regs) {
    // stack slot
    assert(interval->assigned_regHi() == any_reg, "must not have hi register");
    return LIR_OprFact::stack(assigned_reg - nof_regs, type);

  } else {
    // register
    switch (type) {
      case T_OBJECT: {
        assert(assigned_reg >= pd_first_cpu_reg && assigned_reg <= pd_last_cpu_reg, "no cpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register");
        return LIR_OprFact::single_cpu_oop(assigned_reg);
      }

      case T_ADDRESS: {
        assert(assigned_reg >= pd_first_cpu_reg && assigned_reg <= pd_last_cpu_reg, "no cpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register");
        return LIR_OprFact::single_cpu_address(assigned_reg);
      }

      case T_METADATA: {
        assert(assigned_reg >= pd_first_cpu_reg && assigned_reg <= pd_last_cpu_reg, "no cpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register");
        return LIR_OprFact::single_cpu_metadata(assigned_reg);
      }

#ifdef __SOFTFP__
      case T_FLOAT:  // fall through
#endif // __SOFTFP__
      case T_INT: {
        assert(assigned_reg >= pd_first_cpu_reg && assigned_reg <= pd_last_cpu_reg, "no cpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register");
        return LIR_OprFact::single_cpu(assigned_reg);
      }

#ifdef __SOFTFP__
      case T_DOUBLE:  // fall through
#endif // __SOFTFP__
      case T_LONG: {
        int assigned_regHi = interval->assigned_regHi();
        assert(assigned_reg >= pd_first_cpu_reg && assigned_reg <= pd_last_cpu_reg, "no cpu register");
        assert(num_physical_regs(T_LONG) == 1 ||
               (assigned_regHi >= pd_first_cpu_reg && assigned_regHi <= pd_last_cpu_reg), "no cpu register");

        assert(assigned_reg != assigned_regHi, "invalid allocation");
        assert(num_physical_regs(T_LONG) == 1 || assigned_reg < assigned_regHi,
               "register numbers must be sorted (ensure that e.g. a move from eax,ebx to ebx,eax can not occur)");
        assert((assigned_regHi != any_reg) ^ (num_physical_regs(T_LONG) == 1), "must be match");
        if (requires_adjacent_regs(T_LONG)) {
          assert(assigned_reg % 2 == 0 && assigned_reg + 1 == assigned_regHi, "must be sequential and even");
        }

#ifdef _LP64
        return LIR_OprFact::double_cpu(assigned_reg, assigned_reg);
#else
#if defined(PPC32)
        return LIR_OprFact::double_cpu(assigned_regHi, assigned_reg);
#else
        return LIR_OprFact::double_cpu(assigned_reg, assigned_regHi);
#endif // PPC32
#endif // LP64
      }

#ifndef __SOFTFP__
      case T_FLOAT: {
#ifdef X86
        if (UseSSE >= 1) {
          int last_xmm_reg = pd_last_xmm_reg;
#ifdef _LP64
          if (UseAVX < 3) {
            last_xmm_reg = pd_first_xmm_reg + (pd_nof_xmm_regs_frame_map / 2) - 1;
          }
#endif // LP64
          assert(assigned_reg >= pd_first_xmm_reg && assigned_reg <= last_xmm_reg, "no xmm register");
          assert(interval->assigned_regHi() == any_reg, "must not have hi register");
          return LIR_OprFact::single_xmm(assigned_reg - pd_first_xmm_reg);
        }
#endif // X86

        assert(assigned_reg >= pd_first_fpu_reg && assigned_reg <= pd_last_fpu_reg, "no fpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register");
        return LIR_OprFact::single_fpu(assigned_reg - pd_first_fpu_reg);
      }

      case T_DOUBLE: {
#ifdef X86
        if (UseSSE >= 2) {
          int last_xmm_reg = pd_last_xmm_reg;
#ifdef _LP64
          if (UseAVX < 3) {
            last_xmm_reg = pd_first_xmm_reg + (pd_nof_xmm_regs_frame_map / 2) - 1;
          }
#endif // LP64
          assert(assigned_reg >= pd_first_xmm_reg && assigned_reg <= last_xmm_reg, "no xmm register");
          assert(interval->assigned_regHi() == any_reg, "must not have hi register (double xmm values are stored in one register)");
          return LIR_OprFact::double_xmm(assigned_reg - pd_first_xmm_reg);
        }
#endif // X86

#if defined(ARM32)
        assert(assigned_reg >= pd_first_fpu_reg && assigned_reg <= pd_last_fpu_reg, "no fpu register");
        assert(interval->assigned_regHi() >= pd_first_fpu_reg && interval->assigned_regHi() <= pd_last_fpu_reg, "no fpu register");
        assert(assigned_reg % 2 == 0 && assigned_reg + 1 == interval->assigned_regHi(), "must be sequential and even");
        LIR_Opr result = LIR_OprFact::double_fpu(assigned_reg - pd_first_fpu_reg, interval->assigned_regHi() - pd_first_fpu_reg);
#else
        assert(assigned_reg >= pd_first_fpu_reg && assigned_reg <= pd_last_fpu_reg, "no fpu register");
        assert(interval->assigned_regHi() == any_reg, "must not have hi register (double fpu values are stored in one register on Intel)");
        LIR_Opr result = LIR_OprFact::double_fpu(assigned_reg - pd_first_fpu_reg);
#endif
        return result;
      }
#endif // __SOFTFP__

      default: {
        ShouldNotReachHere();
        return LIR_OprFact::illegalOpr;
      }
    }
  }
}

LIR_Opr LinearScan::canonical_spill_opr(Interval* interval) {
  assert(interval->canonical_spill_slot() >= nof_regs, "canonical spill slot not set");
  return LIR_OprFact::stack(interval->canonical_spill_slot() - nof_regs, interval->type());
}

LIR_Opr LinearScan::color_lir_opr(LIR_Opr opr, int op_id, LIR_OpVisitState::OprMode mode) {
  assert(opr->is_virtual(), "should not call this otherwise");

  Interval* interval = interval_at(opr->vreg_number());
  assert(interval != NULL, "interval must exist");

  if (op_id != -1) {
#ifdef ASSERT
    BlockBegin* block = block_of_op_with_id(op_id);
    if (block->number_of_sux() <= 1 && op_id == block->last_lir_instruction_id()) {
      // check if spill moves could have been appended at the end of this block, but
      // before the branch instruction. So the split child information for this branch would
      // be incorrect.
      LIR_OpBranch* branch = block->lir()->instructions_list()->last()->as_OpBranch();
      if (branch != NULL) {
        if (block->live_out().at(opr->vreg_number())) {
          assert(branch->cond() == lir_cond_always, "block does not end with an unconditional jump");
          assert(false, "can't get split child for the last branch of a block because the information would be incorrect (moves are inserted before the branch in resolve_data_flow)");
        }
      }
    }
#endif

    // operands are not changed when an interval is split during allocation,
    // so search the right interval here
    interval = split_child_at_op_id(interval, op_id, mode);
  }

  LIR_Opr res = operand_for_interval(interval);

#ifdef X86
  // new semantic for is_last_use: not only set on definite end of interval,
  // but also before hole
  // This may still miss some cases (e.g. for dead values), but it is not necessary that the
  // last use information is completely correct
  // information is only needed for fpu stack allocation
  if (res->is_fpu_register()) {
    if (opr->is_last_use() || op_id == interval->to() || (op_id != -1 && interval->has_hole_between(op_id, op_id + 1))) {
      assert(op_id == -1 || !is_block_begin(op_id), "holes at begin of block may also result from control flow");
      res = res->make_last_use();
    }
  }
#endif

  assert(!gen()->is_vreg_flag_set(opr->vreg_number(), LIRGenerator::callee_saved) || !FrameMap::is_caller_save_register(res), "bad allocation");

  return res;
}


#ifdef ASSERT
// some methods used to check correctness of debug information

void assert_no_register_values(GrowableArray<ScopeValue*>* values) {
  if (values == NULL) {
    return;
  }

  for (int i = 0; i < values->length(); i++) {
    ScopeValue* value = values->at(i);

    if (value->is_location()) {
      Location location = ((LocationValue*)value)->location();
      assert(location.where() == Location::on_stack, "value is in register");
    }
  }
}

void assert_no_register_values(GrowableArray<MonitorValue*>* values) {
  if (values == NULL) {
    return;
  }

  for (int i = 0; i < values->length(); i++) {
    MonitorValue* value = values->at(i);

    if (value->owner()->is_location()) {
      Location location = ((LocationValue*)value->owner())->location();
      assert(location.where() == Location::on_stack, "owner is in register");
    }
    assert(value->basic_lock().where() == Location::on_stack, "basic_lock is in register");
  }
}

void assert_equal(Location l1, Location l2) {
  assert(l1.where() == l2.where() && l1.type() == l2.type() && l1.offset() == l2.offset(), "");
}

void assert_equal(ScopeValue* v1, ScopeValue* v2) {
  if (v1->is_location()) {
    assert(v2->is_location(), "");
    assert_equal(((LocationValue*)v1)->location(), ((LocationValue*)v2)->location());
  } else if (v1->is_constant_int()) {
    assert(v2->is_constant_int(), "");
    assert(((ConstantIntValue*)v1)->value() == ((ConstantIntValue*)v2)->value(), "");
  } else if (v1->is_constant_double()) {
    assert(v2->is_constant_double(), "");
    assert(((ConstantDoubleValue*)v1)->value() == ((ConstantDoubleValue*)v2)->value(), "");
  } else if (v1->is_constant_long()) {
    assert(v2->is_constant_long(), "");
    assert(((ConstantLongValue*)v1)->value() == ((ConstantLongValue*)v2)->value(), "");
  } else if (v1->is_constant_oop()) {
    assert(v2->is_constant_oop(), "");
    assert(((ConstantOopWriteValue*)v1)->value() == ((ConstantOopWriteValue*)v2)->value(), "");
  } else {
    ShouldNotReachHere();
  }
}

void assert_equal(MonitorValue* m1, MonitorValue* m2) {
  assert_equal(m1->owner(), m2->owner());
  assert_equal(m1->basic_lock(), m2->basic_lock());
}

void assert_equal(IRScopeDebugInfo* d1, IRScopeDebugInfo* d2) {
  assert(d1->scope() == d2->scope(), "not equal");
  assert(d1->bci() == d2->bci(), "not equal");

  if (d1->locals() != NULL) {
    assert(d1->locals() != NULL && d2->locals() != NULL, "not equal");
    assert(d1->locals()->length() == d2->locals()->length(), "not equal");
    for (int i = 0; i < d1->locals()->length(); i++) {
      assert_equal(d1->locals()->at(i), d2->locals()->at(i));
    }
  } else {
    assert(d1->locals() == NULL && d2->locals() == NULL, "not equal");
  }

  if (d1->expressions() != NULL) {
    assert(d1->expressions() != NULL && d2->expressions() != NULL, "not equal");
    assert(d1->expressions()->length() == d2->expressions()->length(), "not equal");
    for (int i = 0; i < d1->expressions()->length(); i++) {
      assert_equal(d1->expressions()->at(i), d2->expressions()->at(i));
    }
  } else {
    assert(d1->expressions() == NULL && d2->expressions() == NULL, "not equal");
  }

  if (d1->monitors() != NULL) {
    assert(d1->monitors() != NULL && d2->monitors() != NULL, "not equal");
    assert(d1->monitors()->length() == d2->monitors()->length(), "not equal");
    for (int i = 0; i < d1->monitors()->length(); i++) {
      assert_equal(d1->monitors()->at(i), d2->monitors()->at(i));
    }
  } else {
    assert(d1->monitors() == NULL && d2->monitors() == NULL, "not equal");
  }

  if (d1->caller() != NULL) {
    assert(d1->caller() != NULL && d2->caller() != NULL, "not equal");
    assert_equal(d1->caller(), d2->caller());
  } else {
    assert(d1->caller() == NULL && d2->caller() == NULL, "not equal");
  }
}

void check_stack_depth(CodeEmitInfo* info, int stack_end) {
  if (info->stack()->bci() != SynchronizationEntryBCI && !info->scope()->method()->is_native()) {
    Bytecodes::Code code = info->scope()->method()->java_code_at_bci(info->stack()->bci());
    switch (code) {
      case Bytecodes::_ifnull    : // fall through
      case Bytecodes::_ifnonnull : // fall through
      case Bytecodes::_ifeq      : // fall through
      case Bytecodes::_ifne      : // fall through
      case Bytecodes::_iflt      : // fall through
      case Bytecodes::_ifge      : // fall through
      case Bytecodes::_ifgt      : // fall through
      case Bytecodes::_ifle      : // fall through
      case Bytecodes::_if_icmpeq : // fall through
      case Bytecodes::_if_icmpne : // fall through
      case Bytecodes::_if_icmplt : // fall through
      case Bytecodes::_if_icmpge : // fall through
      case Bytecodes::_if_icmpgt : // fall through
      case Bytecodes::_if_icmple : // fall through
      case Bytecodes::_if_acmpeq : // fall through
      case Bytecodes::_if_acmpne :
        assert(stack_end >= -Bytecodes::depth(code), "must have non-empty expression stack at if bytecode");
        break;
      default:
        break;
    }
  }
}

#endif // ASSERT


IntervalWalker* LinearScan::init_compute_oop_maps() {
  // setup lists of potential oops for walking
  Interval* oop_intervals;
  Interval* non_oop_intervals;

  create_unhandled_lists(&oop_intervals, &non_oop_intervals, is_oop_interval, NULL);

  // intervals that have no oops inside need not to be processed
  // to ensure a walking until the last instruction id, add a dummy interval
  // with a high operation id
  non_oop_intervals = new Interval(any_reg);
  non_oop_intervals->add_range(max_jint - 2, max_jint - 1);

  return new IntervalWalker(this, oop_intervals, non_oop_intervals);
}


OopMap* LinearScan::compute_oop_map(IntervalWalker* iw, LIR_Op* op, CodeEmitInfo* info, bool is_call_site) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("creating oop map at op_id %d", op->id()));

  // walk before the current operation -> intervals that start at
  // the operation (= output operands of the operation) are not
  // included in the oop map
  iw->walk_before(op->id());

  int frame_size = frame_map()->framesize();
  int arg_count = frame_map()->oop_map_arg_count();
  OopMap* map = new OopMap(frame_size, arg_count);

  // Iterate through active intervals
  for (Interval* interval = iw->active_first(fixedKind); interval != Interval::end(); interval = interval->next()) {
    int assigned_reg = interval->assigned_reg();

    assert(interval->current_from() <= op->id() && op->id() <= interval->current_to(), "interval should not be active otherwise");
    assert(interval->assigned_regHi() == any_reg, "oop must be single word");
    assert(interval->reg_num() >= LIR_OprDesc::vreg_base, "fixed interval found");

    // Check if this range covers the instruction. Intervals that
    // start or end at the current operation are not included in the
    // oop map, except in the case of patching moves.  For patching
    // moves, any intervals which end at this instruction are included
    // in the oop map since we may safepoint while doing the patch
    // before we've consumed the inputs.
    if (op->is_patching() || op->id() < interval->current_to()) {

      // caller-save registers must not be included into oop-maps at calls
      assert(!is_call_site || assigned_reg >= nof_regs || !is_caller_save(assigned_reg), "interval is in a caller-save register at a call -> register will be overwritten");

      VMReg name = vm_reg_for_interval(interval);
      set_oop(map, name);

      // Spill optimization: when the stack value is guaranteed to be always correct,
      // then it must be added to the oop map even if the interval is currently in a register
      if (interval->always_in_memory() &&
          op->id() > interval->spill_definition_pos() &&
          interval->assigned_reg() != interval->canonical_spill_slot()) {
        assert(interval->spill_definition_pos() > 0, "position not set correctly");
        assert(interval->canonical_spill_slot() >= LinearScan::nof_regs, "no spill slot assigned");
        assert(interval->assigned_reg() < LinearScan::nof_regs, "interval is on stack, so stack slot is registered twice");

        set_oop(map, frame_map()->slot_regname(interval->canonical_spill_slot() - LinearScan::nof_regs));
      }
    }
  }

  // add oops from lock stack
  assert(info->stack() != NULL, "CodeEmitInfo must always have a stack");
  int locks_count = info->stack()->total_locks_size();
  for (int i = 0; i < locks_count; i++) {
    set_oop(map, frame_map()->monitor_object_regname(i));
  }

  return map;
}


void LinearScan::compute_oop_map(IntervalWalker* iw, const LIR_OpVisitState &visitor, LIR_Op* op) {
  assert(visitor.info_count() > 0, "no oop map needed");

  // compute oop_map only for first CodeEmitInfo
  // because it is (in most cases) equal for all other infos of the same operation
  CodeEmitInfo* first_info = visitor.info_at(0);
  OopMap* first_oop_map = compute_oop_map(iw, op, first_info, visitor.has_call());

  for (int i = 0; i < visitor.info_count(); i++) {
    CodeEmitInfo* info = visitor.info_at(i);
    OopMap* oop_map = first_oop_map;

    // compute worst case interpreter size in case of a deoptimization
    _compilation->update_interpreter_frame_size(info->interpreter_frame_size());

    if (info->stack()->locks_size() != first_info->stack()->locks_size()) {
      // this info has a different number of locks then the precomputed oop map
      // (possible for lock and unlock instructions) -> compute oop map with
      // correct lock information
      oop_map = compute_oop_map(iw, op, info, visitor.has_call());
    }

    if (info->_oop_map == NULL) {
      info->_oop_map = oop_map;
    } else {
      // a CodeEmitInfo can not be shared between different LIR-instructions
      // because interval splitting can occur anywhere between two instructions
      // and so the oop maps must be different
      // -> check if the already set oop_map is exactly the one calculated for this operation
      assert(info->_oop_map == oop_map, "same CodeEmitInfo used for multiple LIR instructions");
    }
  }
}


// frequently used constants
// Allocate them with new so they are never destroyed (otherwise, a
// forced exit could destroy these objects while they are still in
// use).
ConstantOopWriteValue* LinearScan::_oop_null_scope_value = new (ResourceObj::C_HEAP, mtCompiler) ConstantOopWriteValue(NULL);
ConstantIntValue*      LinearScan::_int_m1_scope_value = new (ResourceObj::C_HEAP, mtCompiler) ConstantIntValue(-1);
ConstantIntValue*      LinearScan::_int_0_scope_value =  new (ResourceObj::C_HEAP, mtCompiler) ConstantIntValue((jint)0);
ConstantIntValue*      LinearScan::_int_1_scope_value =  new (ResourceObj::C_HEAP, mtCompiler) ConstantIntValue(1);
ConstantIntValue*      LinearScan::_int_2_scope_value =  new (ResourceObj::C_HEAP, mtCompiler) ConstantIntValue(2);
LocationValue*         _illegal_value = new (ResourceObj::C_HEAP, mtCompiler) LocationValue(Location());

void LinearScan::init_compute_debug_info() {
  // cache for frequently used scope values
  // (cpu registers and stack slots)
  int cache_size = (LinearScan::nof_cpu_regs + frame_map()->argcount() + max_spills()) * 2;
  _scope_value_cache = ScopeValueArray(cache_size, cache_size, NULL);
}

MonitorValue* LinearScan::location_for_monitor_index(int monitor_index) {
  Location loc;
  if (!frame_map()->location_for_monitor_object(monitor_index, &loc)) {
    bailout("too large frame");
  }
  ScopeValue* object_scope_value = new LocationValue(loc);

  if (!frame_map()->location_for_monitor_lock(monitor_index, &loc)) {
    bailout("too large frame");
  }
  return new MonitorValue(object_scope_value, loc);
}

LocationValue* LinearScan::location_for_name(int name, Location::Type loc_type) {
  Location loc;
  if (!frame_map()->locations_for_slot(name, loc_type, &loc)) {
    bailout("too large frame");
  }
  return new LocationValue(loc);
}


int LinearScan::append_scope_value_for_constant(LIR_Opr opr, GrowableArray<ScopeValue*>* scope_values) {
  assert(opr->is_constant(), "should not be called otherwise");

  LIR_Const* c = opr->as_constant_ptr();
  BasicType t = c->type();
  switch (t) {
    case T_OBJECT: {
      jobject value = c->as_jobject();
      if (value == NULL) {
        scope_values->append(_oop_null_scope_value);
      } else {
        scope_values->append(new ConstantOopWriteValue(c->as_jobject()));
      }
      return 1;
    }

    case T_INT: // fall through
    case T_FLOAT: {
      int value = c->as_jint_bits();
      switch (value) {
        case -1: scope_values->append(_int_m1_scope_value); break;
        case 0:  scope_values->append(_int_0_scope_value); break;
        case 1:  scope_values->append(_int_1_scope_value); break;
        case 2:  scope_values->append(_int_2_scope_value); break;
        default: scope_values->append(new ConstantIntValue(c->as_jint_bits())); break;
      }
      return 1;
    }

    case T_LONG: // fall through
    case T_DOUBLE: {
#ifdef _LP64
      scope_values->append(_int_0_scope_value);
      scope_values->append(new ConstantLongValue(c->as_jlong_bits()));
#else
      if (hi_word_offset_in_bytes > lo_word_offset_in_bytes) {
        scope_values->append(new ConstantIntValue(c->as_jint_hi_bits()));
        scope_values->append(new ConstantIntValue(c->as_jint_lo_bits()));
      } else {
        scope_values->append(new ConstantIntValue(c->as_jint_lo_bits()));
        scope_values->append(new ConstantIntValue(c->as_jint_hi_bits()));
      }
#endif
      return 2;
    }

    case T_ADDRESS: {
#ifdef _LP64
      scope_values->append(new ConstantLongValue(c->as_jint()));
#else
      scope_values->append(new ConstantIntValue(c->as_jint()));
#endif
      return 1;
    }

    default:
      ShouldNotReachHere();
      return -1;
  }
}

int LinearScan::append_scope_value_for_operand(LIR_Opr opr, GrowableArray<ScopeValue*>* scope_values) {
  if (opr->is_single_stack()) {
    int stack_idx = opr->single_stack_ix();
    bool is_oop = opr->is_oop_register();
    int cache_idx = (stack_idx + LinearScan::nof_cpu_regs) * 2 + (is_oop ? 1 : 0);

    ScopeValue* sv = _scope_value_cache.at(cache_idx);
    if (sv == NULL) {
      Location::Type loc_type = is_oop ? Location::oop : Location::normal;
      sv = location_for_name(stack_idx, loc_type);
      _scope_value_cache.at_put(cache_idx, sv);
    }

    // check if cached value is correct
    DEBUG_ONLY(assert_equal(sv, location_for_name(stack_idx, is_oop ? Location::oop : Location::normal)));

    scope_values->append(sv);
    return 1;

  } else if (opr->is_single_cpu()) {
    bool is_oop = opr->is_oop_register();
    int cache_idx = opr->cpu_regnr() * 2 + (is_oop ? 1 : 0);
    Location::Type int_loc_type = NOT_LP64(Location::normal) LP64_ONLY(Location::int_in_long);

    ScopeValue* sv = _scope_value_cache.at(cache_idx);
    if (sv == NULL) {
      Location::Type loc_type = is_oop ? Location::oop : int_loc_type;
      VMReg rname = frame_map()->regname(opr);
      sv = new LocationValue(Location::new_reg_loc(loc_type, rname));
      _scope_value_cache.at_put(cache_idx, sv);
    }

    // check if cached value is correct
    DEBUG_ONLY(assert_equal(sv, new LocationValue(Location::new_reg_loc(is_oop ? Location::oop : int_loc_type, frame_map()->regname(opr)))));

    scope_values->append(sv);
    return 1;

#ifdef X86
  } else if (opr->is_single_xmm()) {
    VMReg rname = opr->as_xmm_float_reg()->as_VMReg();
    LocationValue* sv = new LocationValue(Location::new_reg_loc(Location::normal, rname));

    scope_values->append(sv);
    return 1;
#endif

  } else if (opr->is_single_fpu()) {
#ifdef IA32
    // the exact location of fpu stack values is only known
    // during fpu stack allocation, so the stack allocator object
    // must be present
    assert(use_fpu_stack_allocation(), "should not have float stack values without fpu stack allocation (all floats must be SSE2)");
    assert(_fpu_stack_allocator != NULL, "must be present");
    opr = _fpu_stack_allocator->to_fpu_stack(opr);
#elif defined(AMD64)
    assert(false, "FPU not used on x86-64");
#endif

    Location::Type loc_type = float_saved_as_double ? Location::float_in_dbl : Location::normal;
    VMReg rname = frame_map()->fpu_regname(opr->fpu_regnr());
#ifndef __SOFTFP__
#ifndef VM_LITTLE_ENDIAN
    // On S390 a (single precision) float value occupies only the high
    // word of the full double register. So when the double register is
    // stored to memory (e.g. by the RegisterSaver), then the float value
    // is found at offset 0. I.e. the code below is not needed on S390.
#ifndef S390
    if (! float_saved_as_double) {
      // On big endian system, we may have an issue if float registers use only
      // the low half of the (same) double registers.
      // Both the float and the double could have the same regnr but would correspond
      // to two different addresses once saved.

      // get next safely (no assertion checks)
      VMReg next = VMRegImpl::as_VMReg(1+rname->value());
      if (next->is_reg() &&
          (next->as_FloatRegister() == rname->as_FloatRegister())) {
        // the back-end does use the same numbering for the double and the float
        rname = next; // VMReg for the low bits, e.g. the real VMReg for the float
      }
    }
#endif // !S390
#endif
#endif
    LocationValue* sv = new LocationValue(Location::new_reg_loc(loc_type, rname));

    scope_values->append(sv);
    return 1;

  } else {
    // double-size operands

    ScopeValue* first;
    ScopeValue* second;

    if (opr->is_double_stack()) {
#ifdef _LP64
      Location loc1;
      Location::Type loc_type = opr->type() == T_LONG ? Location::lng : Location::dbl;
      if (!frame_map()->locations_for_slot(opr->double_stack_ix(), loc_type, &loc1, NULL)) {
        bailout("too large frame");
      }

      first =  new LocationValue(loc1);
      second = _int_0_scope_value;
#else
      Location loc1, loc2;
      if (!frame_map()->locations_for_slot(opr->double_stack_ix(), Location::normal, &loc1, &loc2)) {
        bailout("too large frame");
      }
      first =  new LocationValue(loc1);
      second = new LocationValue(loc2);
#endif // _LP64

    } else if (opr->is_double_cpu()) {
#ifdef _LP64
      VMReg rname_first = opr->as_register_lo()->as_VMReg();
      first = new LocationValue(Location::new_reg_loc(Location::lng, rname_first));
      second = _int_0_scope_value;
#else
      VMReg rname_first = opr->as_register_lo()->as_VMReg();
      VMReg rname_second = opr->as_register_hi()->as_VMReg();

      if (hi_word_offset_in_bytes < lo_word_offset_in_bytes) {
        // lo/hi and swapped relative to first and second, so swap them
        VMReg tmp = rname_first;
        rname_first = rname_second;
        rname_second = tmp;
      }

      first = new LocationValue(Location::new_reg_loc(Location::normal, rname_first));
      second = new LocationValue(Location::new_reg_loc(Location::normal, rname_second));
#endif //_LP64


#ifdef X86
    } else if (opr->is_double_xmm()) {
      assert(opr->fpu_regnrLo() == opr->fpu_regnrHi(), "assumed in calculation");
      VMReg rname_first  = opr->as_xmm_double_reg()->as_VMReg();
#  ifdef _LP64
      first = new LocationValue(Location::new_reg_loc(Location::dbl, rname_first));
      second = _int_0_scope_value;
#  else
      first = new LocationValue(Location::new_reg_loc(Location::normal, rname_first));
      // %%% This is probably a waste but we'll keep things as they were for now
      if (true) {
        VMReg rname_second = rname_first->next();
        second = new LocationValue(Location::new_reg_loc(Location::normal, rname_second));
      }
#  endif
#endif

    } else if (opr->is_double_fpu()) {
      // On SPARC, fpu_regnrLo/fpu_regnrHi represents the two halves of
      // the double as float registers in the native ordering. On X86,
      // fpu_regnrLo is a FPU stack slot whose VMReg represents
      // the low-order word of the double and fpu_regnrLo + 1 is the
      // name for the other half.  *first and *second must represent the
      // least and most significant words, respectively.

#ifdef IA32
      // the exact location of fpu stack values is only known
      // during fpu stack allocation, so the stack allocator object
      // must be present
      assert(use_fpu_stack_allocation(), "should not have float stack values without fpu stack allocation (all floats must be SSE2)");
      assert(_fpu_stack_allocator != NULL, "must be present");
      opr = _fpu_stack_allocator->to_fpu_stack(opr);

      assert(opr->fpu_regnrLo() == opr->fpu_regnrHi(), "assumed in calculation (only fpu_regnrLo is used)");
#endif
#ifdef AMD64
      assert(false, "FPU not used on x86-64");
#endif
#ifdef ARM32
      assert(opr->fpu_regnrHi() == opr->fpu_regnrLo() + 1, "assumed in calculation (only fpu_regnrLo is used)");
#endif
#ifdef PPC32
      assert(opr->fpu_regnrLo() == opr->fpu_regnrHi(), "assumed in calculation (only fpu_regnrHi is used)");
#endif

#ifdef VM_LITTLE_ENDIAN
      VMReg rname_first = frame_map()->fpu_regname(opr->fpu_regnrLo());
#else
      VMReg rname_first = frame_map()->fpu_regname(opr->fpu_regnrHi());
#endif

#ifdef _LP64
      first = new LocationValue(Location::new_reg_loc(Location::dbl, rname_first));
      second = _int_0_scope_value;
#else
      first = new LocationValue(Location::new_reg_loc(Location::normal, rname_first));
      // %%% This is probably a waste but we'll keep things as they were for now
      if (true) {
        VMReg rname_second = rname_first->next();
        second = new LocationValue(Location::new_reg_loc(Location::normal, rname_second));
      }
#endif

    } else {
      ShouldNotReachHere();
      first = NULL;
      second = NULL;
    }

    assert(first != NULL && second != NULL, "must be set");
    // The convention the interpreter uses is that the second local
    // holds the first raw word of the native double representation.
    // This is actually reasonable, since locals and stack arrays
    // grow downwards in all implementations.
    // (If, on some machine, the interpreter's Java locals or stack
    // were to grow upwards, the embedded doubles would be word-swapped.)
    scope_values->append(second);
    scope_values->append(first);
    return 2;
  }
}


int LinearScan::append_scope_value(int op_id, Value value, GrowableArray<ScopeValue*>* scope_values) {
  if (value != NULL) {
    LIR_Opr opr = value->operand();
    Constant* con = value->as_Constant();

    assert(con == NULL || opr->is_virtual() || opr->is_constant() || opr->is_illegal(), "asumption: Constant instructions have only constant operands (or illegal if constant is optimized away)");
    assert(con != NULL || opr->is_virtual(), "asumption: non-Constant instructions have only virtual operands");

    if (con != NULL && !con->is_pinned() && !opr->is_constant()) {
      // Unpinned constants may have a virtual operand for a part of the lifetime
      // or may be illegal when it was optimized away,
      // so always use a constant operand
      opr = LIR_OprFact::value_type(con->type());
    }
    assert(opr->is_virtual() || opr->is_constant(), "other cases not allowed here");

    if (opr->is_virtual()) {
      LIR_OpVisitState::OprMode mode = LIR_OpVisitState::inputMode;

      BlockBegin* block = block_of_op_with_id(op_id);
      if (block->number_of_sux() == 1 && op_id == block->last_lir_instruction_id()) {
        // generating debug information for the last instruction of a block.
        // if this instruction is a branch, spill moves are inserted before this branch
        // and so the wrong operand would be returned (spill moves at block boundaries are not
        // considered in the live ranges of intervals)
        // Solution: use the first op_id of the branch target block instead.
        if (block->lir()->instructions_list()->last()->as_OpBranch() != NULL) {
          if (block->live_out().at(opr->vreg_number())) {
            op_id = block->sux_at(0)->first_lir_instruction_id();
            mode = LIR_OpVisitState::outputMode;
          }
        }
      }

      // Get current location of operand
      // The operand must be live because debug information is considered when building the intervals
      // if the interval is not live, color_lir_opr will cause an assertion failure
      opr = color_lir_opr(opr, op_id, mode);
      assert(!has_call(op_id) || opr->is_stack() || !is_caller_save(reg_num(opr)), "can not have caller-save register operands at calls");

      // Append to ScopeValue array
      return append_scope_value_for_operand(opr, scope_values);

    } else {
      assert(value->as_Constant() != NULL, "all other instructions have only virtual operands");
      assert(opr->is_constant(), "operand must be constant");

      return append_scope_value_for_constant(opr, scope_values);
    }
  } else {
    // append a dummy value because real value not needed
    scope_values->append(_illegal_value);
    return 1;
  }
}


IRScopeDebugInfo* LinearScan::compute_debug_info_for_scope(int op_id, IRScope* cur_scope, ValueStack* cur_state, ValueStack* innermost_state) {
  IRScopeDebugInfo* caller_debug_info = NULL;

  ValueStack* caller_state = cur_state->caller_state();
  if (caller_state != NULL) {
    // process recursively to compute outermost scope first
    caller_debug_info = compute_debug_info_for_scope(op_id, cur_scope->caller(), caller_state, innermost_state);
  }

  // initialize these to null.
  // If we don't need deopt info or there are no locals, expressions or monitors,
  // then these get recorded as no information and avoids the allocation of 0 length arrays.
  GrowableArray<ScopeValue*>*   locals      = NULL;
  GrowableArray<ScopeValue*>*   expressions = NULL;
  GrowableArray<MonitorValue*>* monitors    = NULL;

  // describe local variable values
  int nof_locals = cur_state->locals_size();
  if (nof_locals > 0) {
    locals = new GrowableArray<ScopeValue*>(nof_locals);

    int pos = 0;
    while (pos < nof_locals) {
      assert(pos < cur_state->locals_size(), "why not?");

      Value local = cur_state->local_at(pos);
      pos += append_scope_value(op_id, local, locals);

      assert(locals->length() == pos, "must match");
    }
    assert(locals->length() == cur_scope->method()->max_locals(), "wrong number of locals");
    assert(locals->length() == cur_state->locals_size(), "wrong number of locals");
  } else if (cur_scope->method()->max_locals() > 0) {
    assert(cur_state->kind() == ValueStack::EmptyExceptionState, "should be");
    nof_locals = cur_scope->method()->max_locals();
    locals = new GrowableArray<ScopeValue*>(nof_locals);
    for(int i = 0; i < nof_locals; i++) {
      locals->append(_illegal_value);
    }
  }

  // describe expression stack
  int nof_stack = cur_state->stack_size();
  if (nof_stack > 0) {
    expressions = new GrowableArray<ScopeValue*>(nof_stack);

    int pos = 0;
    while (pos < nof_stack) {
      Value expression = cur_state->stack_at_inc(pos);
      append_scope_value(op_id, expression, expressions);

      assert(expressions->length() == pos, "must match");
    }
    assert(expressions->length() == cur_state->stack_size(), "wrong number of stack entries");
  }

  // describe monitors
  int nof_locks = cur_state->locks_size();
  if (nof_locks > 0) {
    int lock_offset = cur_state->caller_state() != NULL ? cur_state->caller_state()->total_locks_size() : 0;
    monitors = new GrowableArray<MonitorValue*>(nof_locks);
    for (int i = 0; i < nof_locks; i++) {
      monitors->append(location_for_monitor_index(lock_offset + i));
    }
  }

  return new IRScopeDebugInfo(cur_scope, cur_state->bci(), locals, expressions, monitors, caller_debug_info);
}


void LinearScan::compute_debug_info(CodeEmitInfo* info, int op_id) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("creating debug information at op_id %d", op_id));

  IRScope* innermost_scope = info->scope();
  ValueStack* innermost_state = info->stack();

  assert(innermost_scope != NULL && innermost_state != NULL, "why is it missing?");

  DEBUG_ONLY(check_stack_depth(info, innermost_state->stack_size()));

  if (info->_scope_debug_info == NULL) {
    // compute debug information
    info->_scope_debug_info = compute_debug_info_for_scope(op_id, innermost_scope, innermost_state, innermost_state);
  } else {
    // debug information already set. Check that it is correct from the current point of view
    DEBUG_ONLY(assert_equal(info->_scope_debug_info, compute_debug_info_for_scope(op_id, innermost_scope, innermost_state, innermost_state)));
  }
}


void LinearScan::assign_reg_num(LIR_OpList* instructions, IntervalWalker* iw) {
  LIR_OpVisitState visitor;
  int num_inst = instructions->length();
  bool has_dead = false;

  for (int j = 0; j < num_inst; j++) {
    LIR_Op* op = instructions->at(j);
    if (op == NULL) {  // this can happen when spill-moves are removed in eliminate_spill_moves
      has_dead = true;
      continue;
    }
    int op_id = op->id();

    // visit instruction to get list of operands
    visitor.visit(op);

    // iterate all modes of the visitor and process all virtual operands
    for_each_visitor_mode(mode) {
      int n = visitor.opr_count(mode);
      for (int k = 0; k < n; k++) {
        LIR_Opr opr = visitor.opr_at(mode, k);
        if (opr->is_virtual_register()) {
          visitor.set_opr_at(mode, k, color_lir_opr(opr, op_id, mode));
        }
      }
    }

    if (visitor.info_count() > 0) {
      // exception handling
      if (compilation()->has_exception_handlers()) {
        XHandlers* xhandlers = visitor.all_xhandler();
        int n = xhandlers->length();
        for (int k = 0; k < n; k++) {
          XHandler* handler = xhandlers->handler_at(k);
          if (handler->entry_code() != NULL) {
            assign_reg_num(handler->entry_code()->instructions_list(), NULL);
          }
        }
      } else {
        assert(visitor.all_xhandler()->length() == 0, "missed exception handler");
      }

      // compute oop map
      assert(iw != NULL, "needed for compute_oop_map");
      compute_oop_map(iw, visitor, op);

      // compute debug information
      if (!use_fpu_stack_allocation()) {
        // compute debug information if fpu stack allocation is not needed.
        // when fpu stack allocation is needed, the debug information can not
        // be computed here because the exact location of fpu operands is not known
        // -> debug information is created inside the fpu stack allocator
        int n = visitor.info_count();
        for (int k = 0; k < n; k++) {
          compute_debug_info(visitor.info_at(k), op_id);
        }
      }
    }

#ifdef ASSERT
    // make sure we haven't made the op invalid.
    op->verify();
#endif

    // remove useless moves
    if (op->code() == lir_move) {
      assert(op->as_Op1() != NULL, "move must be LIR_Op1");
      LIR_Op1* move = (LIR_Op1*)op;
      LIR_Opr src = move->in_opr();
      LIR_Opr dst = move->result_opr();
      if (dst == src ||
          (!dst->is_pointer() && !src->is_pointer() &&
           src->is_same_register(dst))) {
        instructions->at_put(j, NULL);
        has_dead = true;
      }
    }
  }

  if (has_dead) {
    // iterate all instructions of the block and remove all null-values.
    int insert_point = 0;
    for (int j = 0; j < num_inst; j++) {
      LIR_Op* op = instructions->at(j);
      if (op != NULL) {
        if (insert_point != j) {
          instructions->at_put(insert_point, op);
        }
        insert_point++;
      }
    }
    instructions->trunc_to(insert_point);
  }
}

void LinearScan::assign_reg_num() {
  TIME_LINEAR_SCAN(timer_assign_reg_num);

  init_compute_debug_info();
  IntervalWalker* iw = init_compute_oop_maps();

  int num_blocks = block_count();
  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    assign_reg_num(block->lir()->instructions_list(), iw);
  }
}


void LinearScan::do_linear_scan() {
  NOT_PRODUCT(_total_timer.begin_method());

  number_instructions();

  NOT_PRODUCT(print_lir(1, "Before Register Allocation"));

  compute_local_live_sets();
  compute_global_live_sets();
  CHECK_BAILOUT();

  build_intervals();
  CHECK_BAILOUT();
  sort_intervals_before_allocation();

  NOT_PRODUCT(print_intervals("Before Register Allocation"));
  NOT_PRODUCT(LinearScanStatistic::compute(this, _stat_before_alloc));

  allocate_registers();
  CHECK_BAILOUT();

  resolve_data_flow();
  if (compilation()->has_exception_handlers()) {
    resolve_exception_handlers();
  }
  // fill in number of spill slots into frame_map
  propagate_spill_slots();
  CHECK_BAILOUT();

  NOT_PRODUCT(print_intervals("After Register Allocation"));
  NOT_PRODUCT(print_lir(2, "LIR after register allocation:"));

  sort_intervals_after_allocation();

  DEBUG_ONLY(verify());

  eliminate_spill_moves();
  assign_reg_num();
  CHECK_BAILOUT();

  NOT_PRODUCT(print_lir(2, "LIR after assignment of register numbers:"));
  NOT_PRODUCT(LinearScanStatistic::compute(this, _stat_after_asign));

  { TIME_LINEAR_SCAN(timer_allocate_fpu_stack);

    if (use_fpu_stack_allocation()) {
      allocate_fpu_stack(); // Only has effect on Intel
      NOT_PRODUCT(print_lir(2, "LIR after FPU stack allocation:"));
    }
  }

  { TIME_LINEAR_SCAN(timer_optimize_lir);

    EdgeMoveOptimizer::optimize(ir()->code());
    ControlFlowOptimizer::optimize(ir()->code());
    // check that cfg is still correct after optimizations
    ir()->verify();
  }

  NOT_PRODUCT(print_lir(1, "Before Code Generation", false));
  NOT_PRODUCT(LinearScanStatistic::compute(this, _stat_final));
  NOT_PRODUCT(_total_timer.end_method(this));
}


// ********** Printing functions

#ifndef PRODUCT

void LinearScan::print_timers(double total) {
  _total_timer.print(total);
}

void LinearScan::print_statistics() {
  _stat_before_alloc.print("before allocation");
  _stat_after_asign.print("after assignment of register");
  _stat_final.print("after optimization");
}

void LinearScan::print_bitmap(BitMap& b) {
  for (unsigned int i = 0; i < b.size(); i++) {
    if (b.at(i)) tty->print("%d ", i);
  }
  tty->cr();
}

void LinearScan::print_intervals(const char* label) {
  if (TraceLinearScanLevel >= 1) {
    int i;
    tty->cr();
    tty->print_cr("%s", label);

    for (i = 0; i < interval_count(); i++) {
      Interval* interval = interval_at(i);
      if (interval != NULL) {
        interval->print();
      }
    }

    tty->cr();
    tty->print_cr("--- Basic Blocks ---");
    for (i = 0; i < block_count(); i++) {
      BlockBegin* block = block_at(i);
      tty->print("B%d [%d, %d, %d, %d] ", block->block_id(), block->first_lir_instruction_id(), block->last_lir_instruction_id(), block->loop_index(), block->loop_depth());
    }
    tty->cr();
    tty->cr();
  }

  if (PrintCFGToFile) {
    CFGPrinter::print_intervals(&_intervals, label);
  }
}

void LinearScan::print_lir(int level, const char* label, bool hir_valid) {
  if (TraceLinearScanLevel >= level) {
    tty->cr();
    tty->print_cr("%s", label);
    print_LIR(ir()->linear_scan_order());
    tty->cr();
  }

  if (level == 1 && PrintCFGToFile) {
    CFGPrinter::print_cfg(ir()->linear_scan_order(), label, hir_valid, true);
  }
}

void LinearScan::print_reg_num(outputStream* out, int reg_num) {
  if (reg_num == -1) {
    out->print("[ANY]");
    return;
  } else if (reg_num >= LIR_OprDesc::vreg_base) {
    out->print("[VREG %d]", reg_num);
    return;
  }

  LIR_Opr opr = get_operand(reg_num);
  assert(opr->is_valid(), "unknown register");
  opr->print(out);
}

LIR_Opr LinearScan::get_operand(int reg_num) {
  LIR_Opr opr = LIR_OprFact::illegal();

#ifdef X86
  int last_xmm_reg = pd_last_xmm_reg;
#ifdef _LP64
  if (UseAVX < 3) {
    last_xmm_reg = pd_first_xmm_reg + (pd_nof_xmm_regs_frame_map / 2) - 1;
  }
#endif
#endif
  if (reg_num >= pd_first_cpu_reg && reg_num <= pd_last_cpu_reg) {
    opr = LIR_OprFact::single_cpu(reg_num);
  } else if (reg_num >= pd_first_fpu_reg && reg_num <= pd_last_fpu_reg) {
    opr = LIR_OprFact::single_fpu(reg_num - pd_first_fpu_reg);
#ifdef X86
  } else if (reg_num >= pd_first_xmm_reg && reg_num <= last_xmm_reg) {
    opr = LIR_OprFact::single_xmm(reg_num - pd_first_xmm_reg);
#endif
  } else {
    // reg_num == -1 or a virtual register, return the illegal operand
  }
  return opr;
}

Interval* LinearScan::find_interval_at(int reg_num) const {
  if (reg_num < 0 || reg_num >= _intervals.length()) {
    return NULL;
  }
  return interval_at(reg_num);
}

#endif // PRODUCT


// ********** verification functions for allocation
// (check that all intervals have a correct register and that no registers are overwritten)
#ifdef ASSERT

void LinearScan::verify() {
  TRACE_LINEAR_SCAN(2, tty->print_cr("********* verifying intervals ******************************************"));
  verify_intervals();

  TRACE_LINEAR_SCAN(2, tty->print_cr("********* verifying that no oops are in fixed intervals ****************"));
  verify_no_oops_in_fixed_intervals();

  TRACE_LINEAR_SCAN(2, tty->print_cr("********* verifying that unpinned constants are not alive across block boundaries"));
  verify_constants();

  TRACE_LINEAR_SCAN(2, tty->print_cr("********* verifying register allocation ********************************"));
  verify_registers();

  TRACE_LINEAR_SCAN(2, tty->print_cr("********* no errors found **********************************************"));
}

void LinearScan::verify_intervals() {
  int len = interval_count();
  bool has_error = false;

  for (int i = 0; i < len; i++) {
    Interval* i1 = interval_at(i);
    if (i1 == NULL) continue;

    i1->check_split_children();

    if (i1->reg_num() != i) {
      tty->print_cr("Interval %d is on position %d in list", i1->reg_num(), i); i1->print(); tty->cr();
      has_error = true;
    }

    if (i1->reg_num() >= LIR_OprDesc::vreg_base && i1->type() == T_ILLEGAL) {
      tty->print_cr("Interval %d has no type assigned", i1->reg_num()); i1->print(); tty->cr();
      has_error = true;
    }

    if (i1->assigned_reg() == any_reg) {
      tty->print_cr("Interval %d has no register assigned", i1->reg_num()); i1->print(); tty->cr();
      has_error = true;
    }

    if (i1->assigned_reg() == i1->assigned_regHi()) {
      tty->print_cr("Interval %d: low and high register equal", i1->reg_num()); i1->print(); tty->cr();
      has_error = true;
    }

    if (!is_processed_reg_num(i1->assigned_reg())) {
      tty->print_cr("Can not have an Interval for an ignored register"); i1->print(); tty->cr();
      has_error = true;
    }

    // special intervals that are created in MoveResolver
    // -> ignore them because the range information has no meaning there
    if (i1->from() == 1 && i1->to() == 2) continue;

    if (i1->first() == Range::end()) {
      tty->print_cr("Interval %d has no Range", i1->reg_num()); i1->print(); tty->cr();
      has_error = true;
    }

    for (Range* r = i1->first(); r != Range::end(); r = r->next()) {
      if (r->from() >= r->to()) {
        tty->print_cr("Interval %d has zero length range", i1->reg_num()); i1->print(); tty->cr();
        has_error = true;
      }
    }

    for (int j = i + 1; j < len; j++) {
      Interval* i2 = interval_at(j);
      if (i2 == NULL || (i2->from() == 1 && i2->to() == 2)) continue;

      int r1 = i1->assigned_reg();
      int r1Hi = i1->assigned_regHi();
      int r2 = i2->assigned_reg();
      int r2Hi = i2->assigned_regHi();
      if ((r1 == r2 || r1 == r2Hi || (r1Hi != any_reg && (r1Hi == r2 || r1Hi == r2Hi))) && i1->intersects(i2)) {
        tty->print_cr("Intervals %d and %d overlap and have the same register assigned", i1->reg_num(), i2->reg_num());
        i1->print(); tty->cr();
        i2->print(); tty->cr();
        has_error = true;
      }
    }
  }

  assert(has_error == false, "register allocation invalid");
}


void LinearScan::verify_no_oops_in_fixed_intervals() {
  Interval* fixed_intervals;
  Interval* other_intervals;
  create_unhandled_lists(&fixed_intervals, &other_intervals, is_precolored_cpu_interval, NULL);

  // to ensure a walking until the last instruction id, add a dummy interval
  // with a high operation id
  other_intervals = new Interval(any_reg);
  other_intervals->add_range(max_jint - 2, max_jint - 1);
  IntervalWalker* iw = new IntervalWalker(this, fixed_intervals, other_intervals);

  LIR_OpVisitState visitor;
  for (int i = 0; i < block_count(); i++) {
    BlockBegin* block = block_at(i);

    LIR_OpList* instructions = block->lir()->instructions_list();

    for (int j = 0; j < instructions->length(); j++) {
      LIR_Op* op = instructions->at(j);
      int op_id = op->id();

      visitor.visit(op);

      if (visitor.info_count() > 0) {
        iw->walk_before(op->id());
        bool check_live = true;
        if (op->code() == lir_move) {
          LIR_Op1* move = (LIR_Op1*)op;
          check_live = (move->patch_code() == lir_patch_none);
        }
        LIR_OpBranch* branch = op->as_OpBranch();
        if (branch != NULL && branch->stub() != NULL && branch->stub()->is_exception_throw_stub()) {
          // Don't bother checking the stub in this case since the
          // exception stub will never return to normal control flow.
          check_live = false;
        }

        // Make sure none of the fixed registers is live across an
        // oopmap since we can't handle that correctly.
        if (check_live) {
          for (Interval* interval = iw->active_first(fixedKind);
               interval != Interval::end();
               interval = interval->next()) {
            if (interval->current_to() > op->id() + 1) {
              // This interval is live out of this op so make sure
              // that this interval represents some value that's
              // referenced by this op either as an input or output.
              bool ok = false;
              for_each_visitor_mode(mode) {
                int n = visitor.opr_count(mode);
                for (int k = 0; k < n; k++) {
                  LIR_Opr opr = visitor.opr_at(mode, k);
                  if (opr->is_fixed_cpu()) {
                    if (interval_at(reg_num(opr)) == interval) {
                      ok = true;
                      break;
                    }
                    int hi = reg_numHi(opr);
                    if (hi != -1 && interval_at(hi) == interval) {
                      ok = true;
                      break;
                    }
                  }
                }
              }
              assert(ok, "fixed intervals should never be live across an oopmap point");
            }
          }
        }
      }

      // oop-maps at calls do not contain registers, so check is not needed
      if (!visitor.has_call()) {

        for_each_visitor_mode(mode) {
          int n = visitor.opr_count(mode);
          for (int k = 0; k < n; k++) {
            LIR_Opr opr = visitor.opr_at(mode, k);

            if (opr->is_fixed_cpu() && opr->is_oop()) {
              // operand is a non-virtual cpu register and contains an oop
              TRACE_LINEAR_SCAN(4, op->print_on(tty); tty->print("checking operand "); opr->print(); tty->cr());

              Interval* interval = interval_at(reg_num(opr));
              assert(interval != NULL, "no interval");

              if (mode == LIR_OpVisitState::inputMode) {
                if (interval->to() >= op_id + 1) {
                  assert(interval->to() < op_id + 2 ||
                         interval->has_hole_between(op_id, op_id + 2),
                         "oop input operand live after instruction");
                }
              } else if (mode == LIR_OpVisitState::outputMode) {
                if (interval->from() <= op_id - 1) {
                  assert(interval->has_hole_between(op_id - 1, op_id),
                         "oop input operand live after instruction");
                }
              }
            }
          }
        }
      }
    }
  }
}


void LinearScan::verify_constants() {
  int num_regs = num_virtual_regs();
  int size = live_set_size();
  int num_blocks = block_count();

  for (int i = 0; i < num_blocks; i++) {
    BlockBegin* block = block_at(i);
    ResourceBitMap live_at_edge = block->live_in();

    // visit all registers where the live_at_edge bit is set
    for (int r = (int)live_at_edge.get_next_one_offset(0, size); r < size; r = (int)live_at_edge.get_next_one_offset(r + 1, size)) {
      TRACE_LINEAR_SCAN(4, tty->print("checking interval %d of block B%d", r, block->block_id()));

      Value value = gen()->instruction_for_vreg(r);

      assert(value != NULL, "all intervals live across block boundaries must have Value");
      assert(value->operand()->is_register() && value->operand()->is_virtual(), "value must have virtual operand");
      assert(value->operand()->vreg_number() == r, "register number must match");
      // TKR assert(value->as_Constant() == NULL || value->is_pinned(), "only pinned constants can be alive accross block boundaries");
    }
  }
}


class RegisterVerifier: public StackObj {
 private:
  LinearScan*   _allocator;
  BlockList     _work_list;      // all blocks that must be processed
  IntervalsList _saved_states;   // saved information of previous check

  // simplified access to methods of LinearScan
  Compilation*  compilation() const              { return _allocator->compilation(); }
  Interval*     interval_at(int reg_num) const   { return _allocator->interval_at(reg_num); }
  int           reg_num(LIR_Opr opr) const       { return _allocator->reg_num(opr); }

  // currently, only registers are processed
  int           state_size()                     { return LinearScan::nof_regs; }

  // accessors
  IntervalList* state_for_block(BlockBegin* block) { return _saved_states.at(block->block_id()); }
  void          set_state_for_block(BlockBegin* block, IntervalList* saved_state) { _saved_states.at_put(block->block_id(), saved_state); }
  void          add_to_work_list(BlockBegin* block) { if (!_work_list.contains(block)) _work_list.append(block); }

  // helper functions
  IntervalList* copy(IntervalList* input_state);
  void          state_put(IntervalList* input_state, int reg, Interval* interval);
  bool          check_state(IntervalList* input_state, int reg, Interval* interval);

  void process_block(BlockBegin* block);
  void process_xhandler(XHandler* xhandler, IntervalList* input_state);
  void process_successor(BlockBegin* block, IntervalList* input_state);
  void process_operations(LIR_List* ops, IntervalList* input_state);

 public:
  RegisterVerifier(LinearScan* allocator)
    : _allocator(allocator)
    , _work_list(16)
    , _saved_states(BlockBegin::number_of_blocks(), BlockBegin::number_of_blocks(), NULL)
  { }

  void verify(BlockBegin* start);
};


// entry function from LinearScan that starts the verification
void LinearScan::verify_registers() {
  RegisterVerifier verifier(this);
  verifier.verify(block_at(0));
}


void RegisterVerifier::verify(BlockBegin* start) {
  // setup input registers (method arguments) for first block
  int input_state_len = state_size();
  IntervalList* input_state = new IntervalList(input_state_len, input_state_len, NULL);
  CallingConvention* args = compilation()->frame_map()->incoming_arguments();
  for (int n = 0; n < args->length(); n++) {
    LIR_Opr opr = args->at(n);
    if (opr->is_register()) {
      Interval* interval = interval_at(reg_num(opr));

      if (interval->assigned_reg() < state_size()) {
        input_state->at_put(interval->assigned_reg(), interval);
      }
      if (interval->assigned_regHi() != LinearScan::any_reg && interval->assigned_regHi() < state_size()) {
        input_state->at_put(interval->assigned_regHi(), interval);
      }
    }
  }

  set_state_for_block(start, input_state);
  add_to_work_list(start);

  // main loop for verification
  do {
    BlockBegin* block = _work_list.at(0);
    _work_list.remove_at(0);

    process_block(block);
  } while (!_work_list.is_empty());
}

void RegisterVerifier::process_block(BlockBegin* block) {
  TRACE_LINEAR_SCAN(2, tty->cr(); tty->print_cr("process_block B%d", block->block_id()));

  // must copy state because it is modified
  IntervalList* input_state = copy(state_for_block(block));

  if (TraceLinearScanLevel >= 4) {
    tty->print_cr("Input-State of intervals:");
    tty->print("    ");
    for (int i = 0; i < state_size(); i++) {
      if (input_state->at(i) != NULL) {
        tty->print(" %4d", input_state->at(i)->reg_num());
      } else {
        tty->print("   __");
      }
    }
    tty->cr();
    tty->cr();
  }

  // process all operations of the block
  process_operations(block->lir(), input_state);

  // iterate all successors
  for (int i = 0; i < block->number_of_sux(); i++) {
    process_successor(block->sux_at(i), input_state);
  }
}

void RegisterVerifier::process_xhandler(XHandler* xhandler, IntervalList* input_state) {
  TRACE_LINEAR_SCAN(2, tty->print_cr("process_xhandler B%d", xhandler->entry_block()->block_id()));

  // must copy state because it is modified
  input_state = copy(input_state);

  if (xhandler->entry_code() != NULL) {
    process_operations(xhandler->entry_code(), input_state);
  }
  process_successor(xhandler->entry_block(), input_state);
}

void RegisterVerifier::process_successor(BlockBegin* block, IntervalList* input_state) {
  IntervalList* saved_state = state_for_block(block);

  if (saved_state != NULL) {
    // this block was already processed before.
    // check if new input_state is consistent with saved_state

    bool saved_state_correct = true;
    for (int i = 0; i < state_size(); i++) {
      if (input_state->at(i) != saved_state->at(i)) {
        // current input_state and previous saved_state assume a different
        // interval in this register -> assume that this register is invalid
        if (saved_state->at(i) != NULL) {
          // invalidate old calculation only if it assumed that
          // register was valid. when the register was already invalid,
          // then the old calculation was correct.
          saved_state_correct = false;
          saved_state->at_put(i, NULL);

          TRACE_LINEAR_SCAN(4, tty->print_cr("process_successor B%d: invalidating slot %d", block->block_id(), i));
        }
      }
    }

    if (saved_state_correct) {
      // already processed block with correct input_state
      TRACE_LINEAR_SCAN(2, tty->print_cr("process_successor B%d: previous visit already correct", block->block_id()));
    } else {
      // must re-visit this block
      TRACE_LINEAR_SCAN(2, tty->print_cr("process_successor B%d: must re-visit because input state changed", block->block_id()));
      add_to_work_list(block);
    }

  } else {
    // block was not processed before, so set initial input_state
    TRACE_LINEAR_SCAN(2, tty->print_cr("process_successor B%d: initial visit", block->block_id()));

    set_state_for_block(block, copy(input_state));
    add_to_work_list(block);
  }
}


IntervalList* RegisterVerifier::copy(IntervalList* input_state) {
  IntervalList* copy_state = new IntervalList(input_state->length());
  copy_state->appendAll(input_state);
  return copy_state;
}

void RegisterVerifier::state_put(IntervalList* input_state, int reg, Interval* interval) {
  if (reg != LinearScan::any_reg && reg < state_size()) {
    if (interval != NULL) {
      TRACE_LINEAR_SCAN(4, tty->print_cr("        reg[%d] = %d", reg, interval->reg_num()));
    } else if (input_state->at(reg) != NULL) {
      TRACE_LINEAR_SCAN(4, tty->print_cr("        reg[%d] = NULL", reg));
    }

    input_state->at_put(reg, interval);
  }
}

bool RegisterVerifier::check_state(IntervalList* input_state, int reg, Interval* interval) {
  if (reg != LinearScan::any_reg && reg < state_size()) {
    if (input_state->at(reg) != interval) {
      tty->print_cr("!! Error in register allocation: register %d does not contain interval %d", reg, interval->reg_num());
      return true;
    }
  }
  return false;
}

void RegisterVerifier::process_operations(LIR_List* ops, IntervalList* input_state) {
  // visit all instructions of the block
  LIR_OpVisitState visitor;
  bool has_error = false;

  for (int i = 0; i < ops->length(); i++) {
    LIR_Op* op = ops->at(i);
    visitor.visit(op);

    TRACE_LINEAR_SCAN(4, op->print_on(tty));

    // check if input operands are correct
    int j;
    int n = visitor.opr_count(LIR_OpVisitState::inputMode);
    for (j = 0; j < n; j++) {
      LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::inputMode, j);
      if (opr->is_register() && LinearScan::is_processed_reg_num(reg_num(opr))) {
        Interval* interval = interval_at(reg_num(opr));
        if (op->id() != -1) {
          interval = interval->split_child_at_op_id(op->id(), LIR_OpVisitState::inputMode);
        }

        has_error |= check_state(input_state, interval->assigned_reg(),   interval->split_parent());
        has_error |= check_state(input_state, interval->assigned_regHi(), interval->split_parent());

        // When an operand is marked with is_last_use, then the fpu stack allocator
        // removes the register from the fpu stack -> the register contains no value
        if (opr->is_last_use()) {
          state_put(input_state, interval->assigned_reg(),   NULL);
          state_put(input_state, interval->assigned_regHi(), NULL);
        }
      }
    }

    // invalidate all caller save registers at calls
    if (visitor.has_call()) {
      for (j = 0; j < FrameMap::nof_caller_save_cpu_regs(); j++) {
        state_put(input_state, reg_num(FrameMap::caller_save_cpu_reg_at(j)), NULL);
      }
      for (j = 0; j < FrameMap::nof_caller_save_fpu_regs; j++) {
        state_put(input_state, reg_num(FrameMap::caller_save_fpu_reg_at(j)), NULL);
      }

#ifdef X86
      int num_caller_save_xmm_regs = FrameMap::get_num_caller_save_xmms();
      for (j = 0; j < num_caller_save_xmm_regs; j++) {
        state_put(input_state, reg_num(FrameMap::caller_save_xmm_reg_at(j)), NULL);
      }
#endif
    }

    // process xhandler before output and temp operands
    XHandlers* xhandlers = visitor.all_xhandler();
    n = xhandlers->length();
    for (int k = 0; k < n; k++) {
      process_xhandler(xhandlers->handler_at(k), input_state);
    }

    // set temp operands (some operations use temp operands also as output operands, so can't set them NULL)
    n = visitor.opr_count(LIR_OpVisitState::tempMode);
    for (j = 0; j < n; j++) {
      LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::tempMode, j);
      if (opr->is_register() && LinearScan::is_processed_reg_num(reg_num(opr))) {
        Interval* interval = interval_at(reg_num(opr));
        if (op->id() != -1) {
          interval = interval->split_child_at_op_id(op->id(), LIR_OpVisitState::tempMode);
        }

        state_put(input_state, interval->assigned_reg(),   interval->split_parent());
        state_put(input_state, interval->assigned_regHi(), interval->split_parent());
      }
    }

    // set output operands
    n = visitor.opr_count(LIR_OpVisitState::outputMode);
    for (j = 0; j < n; j++) {
      LIR_Opr opr = visitor.opr_at(LIR_OpVisitState::outputMode, j);
      if (opr->is_register() && LinearScan::is_processed_reg_num(reg_num(opr))) {
        Interval* interval = interval_at(reg_num(opr));
        if (op->id() != -1) {
          interval = interval->split_child_at_op_id(op->id(), LIR_OpVisitState::outputMode);
        }

        state_put(input_state, interval->assigned_reg(),   interval->split_parent());
        state_put(input_state, interval->assigned_regHi(), interval->split_parent());
      }
    }
  }
  assert(has_error == false, "Error in register allocation");
}

#endif // ASSERT



// **** Implementation of MoveResolver ******************************

MoveResolver::MoveResolver(LinearScan* allocator) :
  _allocator(allocator),
  _insert_list(NULL),
  _insert_idx(-1),
  _insertion_buffer(),
  _mapping_from(8),
  _mapping_from_opr(8),
  _mapping_to(8),
  _multiple_reads_allowed(false)
{
  for (int i = 0; i < LinearScan::nof_regs; i++) {
    _register_blocked[i] = 0;
  }
  DEBUG_ONLY(check_empty());
}


#ifdef ASSERT

void MoveResolver::check_empty() {
  assert(_mapping_from.length() == 0 && _mapping_from_opr.length() == 0 && _mapping_to.length() == 0, "list must be empty before and after processing");
  for (int i = 0; i < LinearScan::nof_regs; i++) {
    assert(register_blocked(i) == 0, "register map must be empty before and after processing");
  }
  assert(_multiple_reads_allowed == false, "must have default value");
}

void MoveResolver::verify_before_resolve() {
  assert(_mapping_from.length() == _mapping_from_opr.length(), "length must be equal");
  assert(_mapping_from.length() == _mapping_to.length(), "length must be equal");
  assert(_insert_list != NULL && _insert_idx != -1, "insert position not set");

  int i, j;
  if (!_multiple_reads_allowed) {
    for (i = 0; i < _mapping_from.length(); i++) {
      for (j = i + 1; j < _mapping_from.length(); j++) {
        assert(_mapping_from.at(i) == NULL || _mapping_from.at(i) != _mapping_from.at(j), "cannot read from same interval twice");
      }
    }
  }

  for (i = 0; i < _mapping_to.length(); i++) {
    for (j = i + 1; j < _mapping_to.length(); j++) {
      assert(_mapping_to.at(i) != _mapping_to.at(j), "cannot write to same interval twice");
    }
  }


  ResourceBitMap used_regs(LinearScan::nof_regs + allocator()->frame_map()->argcount() + allocator()->max_spills());
  if (!_multiple_reads_allowed) {
    for (i = 0; i < _mapping_from.length(); i++) {
      Interval* it = _mapping_from.at(i);
      if (it != NULL) {
        assert(!used_regs.at(it->assigned_reg()), "cannot read from same register twice");
        used_regs.set_bit(it->assigned_reg());

        if (it->assigned_regHi() != LinearScan::any_reg) {
          assert(!used_regs.at(it->assigned_regHi()), "cannot read from same register twice");
          used_regs.set_bit(it->assigned_regHi());
        }
      }
    }
  }

  used_regs.clear();
  for (i = 0; i < _mapping_to.length(); i++) {
    Interval* it = _mapping_to.at(i);
    assert(!used_regs.at(it->assigned_reg()), "cannot write to same register twice");
    used_regs.set_bit(it->assigned_reg());

    if (it->assigned_regHi() != LinearScan::any_reg) {
      assert(!used_regs.at(it->assigned_regHi()), "cannot write to same register twice");
      used_regs.set_bit(it->assigned_regHi());
    }
  }

  used_regs.clear();
  for (i = 0; i < _mapping_from.length(); i++) {
    Interval* it = _mapping_from.at(i);
    if (it != NULL && it->assigned_reg() >= LinearScan::nof_regs) {
      used_regs.set_bit(it->assigned_reg());
    }
  }
  for (i = 0; i < _mapping_to.length(); i++) {
    Interval* it = _mapping_to.at(i);
    assert(!used_regs.at(it->assigned_reg()) || it->assigned_reg() == _mapping_from.at(i)->assigned_reg(), "stack slots used in _mapping_from must be disjoint to _mapping_to");
  }
}

#endif // ASSERT


// mark assigned_reg and assigned_regHi of the interval as blocked
void MoveResolver::block_registers(Interval* it) {
  int reg = it->assigned_reg();
  if (reg < LinearScan::nof_regs) {
    assert(_multiple_reads_allowed || register_blocked(reg) == 0, "register already marked as used");
    set_register_blocked(reg, 1);
  }
  reg = it->assigned_regHi();
  if (reg != LinearScan::any_reg && reg < LinearScan::nof_regs) {
    assert(_multiple_reads_allowed || register_blocked(reg) == 0, "register already marked as used");
    set_register_blocked(reg, 1);
  }
}

// mark assigned_reg and assigned_regHi of the interval as unblocked
void MoveResolver::unblock_registers(Interval* it) {
  int reg = it->assigned_reg();
  if (reg < LinearScan::nof_regs) {
    assert(register_blocked(reg) > 0, "register already marked as unused");
    set_register_blocked(reg, -1);
  }
  reg = it->assigned_regHi();
  if (reg != LinearScan::any_reg && reg < LinearScan::nof_regs) {
    assert(register_blocked(reg) > 0, "register already marked as unused");
    set_register_blocked(reg, -1);
  }
}

// check if assigned_reg and assigned_regHi of the to-interval are not blocked (or only blocked by from)
bool MoveResolver::save_to_process_move(Interval* from, Interval* to) {
  int from_reg = -1;
  int from_regHi = -1;
  if (from != NULL) {
    from_reg = from->assigned_reg();
    from_regHi = from->assigned_regHi();
  }

  int reg = to->assigned_reg();
  if (reg < LinearScan::nof_regs) {
    if (register_blocked(reg) > 1 || (register_blocked(reg) == 1 && reg != from_reg && reg != from_regHi)) {
      return false;
    }
  }
  reg = to->assigned_regHi();
  if (reg != LinearScan::any_reg && reg < LinearScan::nof_regs) {
    if (register_blocked(reg) > 1 || (register_blocked(reg) == 1 && reg != from_reg && reg != from_regHi)) {
      return false;
    }
  }

  return true;
}


void MoveResolver::create_insertion_buffer(LIR_List* list) {
  assert(!_insertion_buffer.initialized(), "overwriting existing buffer");
  _insertion_buffer.init(list);
}

void MoveResolver::append_insertion_buffer() {
  if (_insertion_buffer.initialized()) {
    _insertion_buffer.lir_list()->append(&_insertion_buffer);
  }
  assert(!_insertion_buffer.initialized(), "must be uninitialized now");

  _insert_list = NULL;
  _insert_idx = -1;
}

void MoveResolver::insert_move(Interval* from_interval, Interval* to_interval) {
  assert(from_interval->reg_num() != to_interval->reg_num(), "from and to interval equal");
  assert(from_interval->type() == to_interval->type(), "move between different types");
  assert(_insert_list != NULL && _insert_idx != -1, "must setup insert position first");
  assert(_insertion_buffer.lir_list() == _insert_list, "wrong insertion buffer");

  LIR_Opr from_opr = get_virtual_register(from_interval);
  LIR_Opr to_opr = get_virtual_register(to_interval);

  if (!_multiple_reads_allowed) {
    // the last_use flag is an optimization for FPU stack allocation. When the same
    // input interval is used in more than one move, then it is too difficult to determine
    // if this move is really the last use.
    from_opr = from_opr->make_last_use();
  }
  _insertion_buffer.move(_insert_idx, from_opr, to_opr);

  TRACE_LINEAR_SCAN(4, tty->print_cr("MoveResolver: inserted move from register %d (%d, %d) to %d (%d, %d)", from_interval->reg_num(), from_interval->assigned_reg(), from_interval->assigned_regHi(), to_interval->reg_num(), to_interval->assigned_reg(), to_interval->assigned_regHi()));
}

void MoveResolver::insert_move(LIR_Opr from_opr, Interval* to_interval) {
  assert(from_opr->type() == to_interval->type(), "move between different types");
  assert(_insert_list != NULL && _insert_idx != -1, "must setup insert position first");
  assert(_insertion_buffer.lir_list() == _insert_list, "wrong insertion buffer");

  LIR_Opr to_opr = get_virtual_register(to_interval);
  _insertion_buffer.move(_insert_idx, from_opr, to_opr);

  TRACE_LINEAR_SCAN(4, tty->print("MoveResolver: inserted move from constant "); from_opr->print(); tty->print_cr("  to %d (%d, %d)", to_interval->reg_num(), to_interval->assigned_reg(), to_interval->assigned_regHi()));
}

LIR_Opr MoveResolver::get_virtual_register(Interval* interval) {
  // Add a little fudge factor for the bailout since the bailout is only checked periodically. This allows us to hand out
  // a few extra registers before we really run out which helps to avoid to trip over assertions.
  int reg_num = interval->reg_num();
  if (reg_num + 20 >= LIR_OprDesc::vreg_max) {
    _allocator->bailout("out of virtual registers in linear scan");
    if (reg_num + 2 >= LIR_OprDesc::vreg_max) {
      // Wrap it around and continue until bailout really happens to avoid hitting assertions.
      reg_num = LIR_OprDesc::vreg_base;
    }
  }
  LIR_Opr vreg = LIR_OprFact::virtual_register(reg_num, interval->type());
  assert(vreg != LIR_OprFact::illegal(), "ran out of virtual registers");
  return vreg;
}

void MoveResolver::resolve_mappings() {
  TRACE_LINEAR_SCAN(4, tty->print_cr("MoveResolver: resolving mappings for Block B%d, index %d", _insert_list->block() != NULL ? _insert_list->block()->block_id() : -1, _insert_idx));
  DEBUG_ONLY(verify_before_resolve());

  // Block all registers that are used as input operands of a move.
  // When a register is blocked, no move to this register is emitted.
  // This is necessary for detecting cycles in moves.
  int i;
  for (i = _mapping_from.length() - 1; i >= 0; i--) {
    Interval* from_interval = _mapping_from.at(i);
    if (from_interval != NULL) {
      block_registers(from_interval);
    }
  }

  int spill_candidate = -1;
  while (_mapping_from.length() > 0) {
    bool processed_interval = false;

    for (i = _mapping_from.length() - 1; i >= 0; i--) {
      Interval* from_interval = _mapping_from.at(i);
      Interval* to_interval = _mapping_to.at(i);

      if (save_to_process_move(from_interval, to_interval)) {
        // this inverval can be processed because target is free
        if (from_interval != NULL) {
          insert_move(from_interval, to_interval);
          unblock_registers(from_interval);
        } else {
          insert_move(_mapping_from_opr.at(i), to_interval);
        }
        _mapping_from.remove_at(i);
        _mapping_from_opr.remove_at(i);
        _mapping_to.remove_at(i);

        processed_interval = true;
      } else if (from_interval != NULL && from_interval->assigned_reg() < LinearScan::nof_regs) {
        // this interval cannot be processed now because target is not free
        // it starts in a register, so it is a possible candidate for spilling
        spill_candidate = i;
      }
    }

    if (!processed_interval) {
      // no move could be processed because there is a cycle in the move list
      // (e.g. r1 -> r2, r2 -> r1), so one interval must be spilled to memory
      guarantee(spill_candidate != -1, "no interval in register for spilling found");

      // create a new spill interval and assign a stack slot to it
      Interval* from_interval = _mapping_from.at(spill_candidate);
      Interval* spill_interval = new Interval(-1);
      spill_interval->set_type(from_interval->type());

      // add a dummy range because real position is difficult to calculate
      // Note: this range is a special case when the integrity of the allocation is checked
      spill_interval->add_range(1, 2);

      //       do not allocate a new spill slot for temporary interval, but
      //       use spill slot assigned to from_interval. Otherwise moves from
      //       one stack slot to another can happen (not allowed by LIR_Assembler
      int spill_slot = from_interval->canonical_spill_slot();
      if (spill_slot < 0) {
        spill_slot = allocator()->allocate_spill_slot(type2spill_size[spill_interval->type()] == 2);
        from_interval->set_canonical_spill_slot(spill_slot);
      }
      spill_interval->assign_reg(spill_slot);
      allocator()->append_interval(spill_interval);

      TRACE_LINEAR_SCAN(4, tty->print_cr("created new Interval %d for spilling", spill_interval->reg_num()));

      // insert a move from register to stack and update the mapping
      insert_move(from_interval, spill_interval);
      _mapping_from.at_put(spill_candidate, spill_interval);
      unblock_registers(from_interval);
    }
  }

  // reset to default value
  _multiple_reads_allowed = false;

  // check that all intervals have been processed
  DEBUG_ONLY(check_empty());
}


void MoveResolver::set_insert_position(LIR_List* insert_list, int insert_idx) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("MoveResolver: setting insert position to Block B%d, index %d", insert_list->block() != NULL ? insert_list->block()->block_id() : -1, insert_idx));
  assert(_insert_list == NULL && _insert_idx == -1, "use move_insert_position instead of set_insert_position when data already set");

  create_insertion_buffer(insert_list);
  _insert_list = insert_list;
  _insert_idx = insert_idx;
}

void MoveResolver::move_insert_position(LIR_List* insert_list, int insert_idx) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("MoveResolver: moving insert position to Block B%d, index %d", insert_list->block() != NULL ? insert_list->block()->block_id() : -1, insert_idx));

  if (_insert_list != NULL && (insert_list != _insert_list || insert_idx != _insert_idx)) {
    // insert position changed -> resolve current mappings
    resolve_mappings();
  }

  if (insert_list != _insert_list) {
    // block changed -> append insertion_buffer because it is
    // bound to a specific block and create a new insertion_buffer
    append_insertion_buffer();
    create_insertion_buffer(insert_list);
  }

  _insert_list = insert_list;
  _insert_idx = insert_idx;
}

void MoveResolver::add_mapping(Interval* from_interval, Interval* to_interval) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("MoveResolver: adding mapping from %d (%d, %d) to %d (%d, %d)", from_interval->reg_num(), from_interval->assigned_reg(), from_interval->assigned_regHi(), to_interval->reg_num(), to_interval->assigned_reg(), to_interval->assigned_regHi()));

  _mapping_from.append(from_interval);
  _mapping_from_opr.append(LIR_OprFact::illegalOpr);
  _mapping_to.append(to_interval);
}


void MoveResolver::add_mapping(LIR_Opr from_opr, Interval* to_interval) {
  TRACE_LINEAR_SCAN(4, tty->print("MoveResolver: adding mapping from "); from_opr->print(); tty->print_cr(" to %d (%d, %d)", to_interval->reg_num(), to_interval->assigned_reg(), to_interval->assigned_regHi()));
  assert(from_opr->is_constant(), "only for constants");

  _mapping_from.append(NULL);
  _mapping_from_opr.append(from_opr);
  _mapping_to.append(to_interval);
}

void MoveResolver::resolve_and_append_moves() {
  if (has_mappings()) {
    resolve_mappings();
  }
  append_insertion_buffer();
}



// **** Implementation of Range *************************************

Range::Range(int from, int to, Range* next) :
  _from(from),
  _to(to),
  _next(next)
{
}

// initialize sentinel
Range* Range::_end = NULL;
void Range::initialize(Arena* arena) {
  _end = new (arena) Range(max_jint, max_jint, NULL);
}

int Range::intersects_at(Range* r2) const {
  const Range* r1 = this;

  assert(r1 != NULL && r2 != NULL, "null ranges not allowed");
  assert(r1 != _end && r2 != _end, "empty ranges not allowed");

  do {
    if (r1->from() < r2->from()) {
      if (r1->to() <= r2->from()) {
        r1 = r1->next(); if (r1 == _end) return -1;
      } else {
        return r2->from();
      }
    } else if (r2->from() < r1->from()) {
      if (r2->to() <= r1->from()) {
        r2 = r2->next(); if (r2 == _end) return -1;
      } else {
        return r1->from();
      }
    } else { // r1->from() == r2->from()
      if (r1->from() == r1->to()) {
        r1 = r1->next(); if (r1 == _end) return -1;
      } else if (r2->from() == r2->to()) {
        r2 = r2->next(); if (r2 == _end) return -1;
      } else {
        return r1->from();
      }
    }
  } while (true);
}

#ifndef PRODUCT
void Range::print(outputStream* out) const {
  out->print("[%d, %d[ ", _from, _to);
}
#endif



// **** Implementation of Interval **********************************

// initialize sentinel
Interval* Interval::_end = NULL;
void Interval::initialize(Arena* arena) {
  Range::initialize(arena);
  _end = new (arena) Interval(-1);
}

Interval::Interval(int reg_num) :
  _reg_num(reg_num),
  _type(T_ILLEGAL),
  _first(Range::end()),
  _use_pos_and_kinds(12),
  _current(Range::end()),
  _next(_end),
  _state(invalidState),
  _assigned_reg(LinearScan::any_reg),
  _assigned_regHi(LinearScan::any_reg),
  _cached_to(-1),
  _cached_opr(LIR_OprFact::illegalOpr),
  _cached_vm_reg(VMRegImpl::Bad()),
  _split_children(NULL),
  _canonical_spill_slot(-1),
  _insert_move_when_activated(false),
  _spill_state(noDefinitionFound),
  _spill_definition_pos(-1),
  _register_hint(NULL)
{
  _split_parent = this;
  _current_split_child = this;
}

int Interval::calc_to() {
  assert(_first != Range::end(), "interval has no range");

  Range* r = _first;
  while (r->next() != Range::end()) {
    r = r->next();
  }
  return r->to();
}


#ifdef ASSERT
// consistency check of split-children
void Interval::check_split_children() {
  if (_split_children != NULL && _split_children->length() > 0) {
    assert(is_split_parent(), "only split parents can have children");

    for (int i = 0; i < _split_children->length(); i++) {
      Interval* i1 = _split_children->at(i);

      assert(i1->split_parent() == this, "not a split child of this interval");
      assert(i1->type() == type(), "must be equal for all split children");
      assert(i1->canonical_spill_slot() == canonical_spill_slot(), "must be equal for all split children");

      for (int j = i + 1; j < _split_children->length(); j++) {
        Interval* i2 = _split_children->at(j);

        assert(i1->reg_num() != i2->reg_num(), "same register number");

        if (i1->from() < i2->from()) {
          assert(i1->to() <= i2->from() && i1->to() < i2->to(), "intervals overlapping");
        } else {
          assert(i2->from() < i1->from(), "intervals start at same op_id");
          assert(i2->to() <= i1->from() && i2->to() < i1->to(), "intervals overlapping");
        }
      }
    }
  }
}
#endif // ASSERT

Interval* Interval::register_hint(bool search_split_child) const {
  if (!search_split_child) {
    return _register_hint;
  }

  if (_register_hint != NULL) {
    assert(_register_hint->is_split_parent(), "ony split parents are valid hint registers");

    if (_register_hint->assigned_reg() >= 0 && _register_hint->assigned_reg() < LinearScan::nof_regs) {
      return _register_hint;

    } else if (_register_hint->_split_children != NULL && _register_hint->_split_children->length() > 0) {
      // search the first split child that has a register assigned
      int len = _register_hint->_split_children->length();
      for (int i = 0; i < len; i++) {
        Interval* cur = _register_hint->_split_children->at(i);

        if (cur->assigned_reg() >= 0 && cur->assigned_reg() < LinearScan::nof_regs) {
          return cur;
        }
      }
    }
  }

  // no hint interval found that has a register assigned
  return NULL;
}


Interval* Interval::split_child_at_op_id(int op_id, LIR_OpVisitState::OprMode mode) {
  assert(is_split_parent(), "can only be called for split parents");
  assert(op_id >= 0, "invalid op_id (method can not be called for spill moves)");

  Interval* result;
  if (_split_children == NULL || _split_children->length() == 0) {
    result = this;
  } else {
    result = NULL;
    int len = _split_children->length();

    // in outputMode, the end of the interval (op_id == cur->to()) is not valid
    int to_offset = (mode == LIR_OpVisitState::outputMode ? 0 : 1);

    int i;
    for (i = 0; i < len; i++) {
      Interval* cur = _split_children->at(i);
      if (cur->from() <= op_id && op_id < cur->to() + to_offset) {
        if (i > 0) {
          // exchange current split child to start of list (faster access for next call)
          _split_children->at_put(i, _split_children->at(0));
          _split_children->at_put(0, cur);
        }

        // interval found
        result = cur;
        break;
      }
    }

#ifdef ASSERT
    for (i = 0; i < len; i++) {
      Interval* tmp = _split_children->at(i);
      if (tmp != result && tmp->from() <= op_id && op_id < tmp->to() + to_offset) {
        tty->print_cr("two valid result intervals found for op_id %d: %d and %d", op_id, result->reg_num(), tmp->reg_num());
        result->print();
        tmp->print();
        assert(false, "two valid result intervals found");
      }
    }
#endif
  }

  assert(result != NULL, "no matching interval found");
  assert(result->covers(op_id, mode), "op_id not covered by interval");

  return result;
}


// returns the last split child that ends before the given op_id
Interval* Interval::split_child_before_op_id(int op_id) {
  assert(op_id >= 0, "invalid op_id");

  Interval* parent = split_parent();
  Interval* result = NULL;

  assert(parent->_split_children != NULL, "no split children available");
  int len = parent->_split_children->length();
  assert(len > 0, "no split children available");

  for (int i = len - 1; i >= 0; i--) {
    Interval* cur = parent->_split_children->at(i);
    if (cur->to() <= op_id && (result == NULL || result->to() < cur->to())) {
      result = cur;
    }
  }

  assert(result != NULL, "no split child found");
  return result;
}


// Note: use positions are sorted descending -> first use has highest index
int Interval::first_usage(IntervalUseKind min_use_kind) const {
  assert(LinearScan::is_virtual_interval(this), "cannot access use positions for fixed intervals");

  for (int i = _use_pos_and_kinds.length() - 2; i >= 0; i -= 2) {
    if (_use_pos_and_kinds.at(i + 1) >= min_use_kind) {
      return _use_pos_and_kinds.at(i);
    }
  }
  return max_jint;
}

int Interval::next_usage(IntervalUseKind min_use_kind, int from) const {
  assert(LinearScan::is_virtual_interval(this), "cannot access use positions for fixed intervals");

  for (int i = _use_pos_and_kinds.length() - 2; i >= 0; i -= 2) {
    if (_use_pos_and_kinds.at(i) >= from && _use_pos_and_kinds.at(i + 1) >= min_use_kind) {
      return _use_pos_and_kinds.at(i);
    }
  }
  return max_jint;
}

int Interval::next_usage_exact(IntervalUseKind exact_use_kind, int from) const {
  assert(LinearScan::is_virtual_interval(this), "cannot access use positions for fixed intervals");

  for (int i = _use_pos_and_kinds.length() - 2; i >= 0; i -= 2) {
    if (_use_pos_and_kinds.at(i) >= from && _use_pos_and_kinds.at(i + 1) == exact_use_kind) {
      return _use_pos_and_kinds.at(i);
    }
  }
  return max_jint;
}

int Interval::previous_usage(IntervalUseKind min_use_kind, int from) const {
  assert(LinearScan::is_virtual_interval(this), "cannot access use positions for fixed intervals");

  int prev = 0;
  for (int i = _use_pos_and_kinds.length() - 2; i >= 0; i -= 2) {
    if (_use_pos_and_kinds.at(i) > from) {
      return prev;
    }
    if (_use_pos_and_kinds.at(i + 1) >= min_use_kind) {
      prev = _use_pos_and_kinds.at(i);
    }
  }
  return prev;
}

void Interval::add_use_pos(int pos, IntervalUseKind use_kind) {
  assert(covers(pos, LIR_OpVisitState::inputMode), "use position not covered by live range");

  // do not add use positions for precolored intervals because
  // they are never used
  if (use_kind != noUse && reg_num() >= LIR_OprDesc::vreg_base) {
#ifdef ASSERT
    assert(_use_pos_and_kinds.length() % 2 == 0, "must be");
    for (int i = 0; i < _use_pos_and_kinds.length(); i += 2) {
      assert(pos <= _use_pos_and_kinds.at(i), "already added a use-position with lower position");
      assert(_use_pos_and_kinds.at(i + 1) >= firstValidKind && _use_pos_and_kinds.at(i + 1) <= lastValidKind, "invalid use kind");
      if (i > 0) {
        assert(_use_pos_and_kinds.at(i) < _use_pos_and_kinds.at(i - 2), "not sorted descending");
      }
    }
#endif

    // Note: add_use is called in descending order, so list gets sorted
    //       automatically by just appending new use positions
    int len = _use_pos_and_kinds.length();
    if (len == 0 || _use_pos_and_kinds.at(len - 2) > pos) {
      _use_pos_and_kinds.append(pos);
      _use_pos_and_kinds.append(use_kind);
    } else if (_use_pos_and_kinds.at(len - 1) < use_kind) {
      assert(_use_pos_and_kinds.at(len - 2) == pos, "list not sorted correctly");
      _use_pos_and_kinds.at_put(len - 1, use_kind);
    }
  }
}

void Interval::add_range(int from, int to) {
  assert(from < to, "invalid range");
  assert(first() == Range::end() || to < first()->next()->from(), "not inserting at begin of interval");
  assert(from <= first()->to(), "not inserting at begin of interval");

  if (first()->from() <= to) {
    // join intersecting ranges
    first()->set_from(MIN2(from, first()->from()));
    first()->set_to  (MAX2(to,   first()->to()));
  } else {
    // insert new range
    _first = new Range(from, to, first());
  }
}

Interval* Interval::new_split_child() {
  // allocate new interval
  Interval* result = new Interval(-1);
  result->set_type(type());

  Interval* parent = split_parent();
  result->_split_parent = parent;
  result->set_register_hint(parent);

  // insert new interval in children-list of parent
  if (parent->_split_children == NULL) {
    assert(is_split_parent(), "list must be initialized at first split");

    parent->_split_children = new IntervalList(4);
    parent->_split_children->append(this);
  }
  parent->_split_children->append(result);

  return result;
}

// split this interval at the specified position and return
// the remainder as a new interval.
//
// when an interval is split, a bi-directional link is established between the original interval
// (the split parent) and the intervals that are split off this interval (the split children)
// When a split child is split again, the new created interval is also a direct child
// of the original parent (there is no tree of split children stored, but a flat list)
// All split children are spilled to the same stack slot (stored in _canonical_spill_slot)
//
// Note: The new interval has no valid reg_num
Interval* Interval::split(int split_pos) {
  assert(LinearScan::is_virtual_interval(this), "cannot split fixed intervals");

  // allocate new interval
  Interval* result = new_split_child();

  // split the ranges
  Range* prev = NULL;
  Range* cur = _first;
  while (cur != Range::end() && cur->to() <= split_pos) {
    prev = cur;
    cur = cur->next();
  }
  assert(cur != Range::end(), "split interval after end of last range");

  if (cur->from() < split_pos) {
    result->_first = new Range(split_pos, cur->to(), cur->next());
    cur->set_to(split_pos);
    cur->set_next(Range::end());

  } else {
    assert(prev != NULL, "split before start of first range");
    result->_first = cur;
    prev->set_next(Range::end());
  }
  result->_current = result->_first;
  _cached_to = -1; // clear cached value

  // split list of use positions
  int total_len = _use_pos_and_kinds.length();
  int start_idx = total_len - 2;
  while (start_idx >= 0 && _use_pos_and_kinds.at(start_idx) < split_pos) {
    start_idx -= 2;
  }

  intStack new_use_pos_and_kinds(total_len - start_idx);
  int i;
  for (i = start_idx + 2; i < total_len; i++) {
    new_use_pos_and_kinds.append(_use_pos_and_kinds.at(i));
  }

  _use_pos_and_kinds.trunc_to(start_idx + 2);
  result->_use_pos_and_kinds = _use_pos_and_kinds;
  _use_pos_and_kinds = new_use_pos_and_kinds;

#ifdef ASSERT
  assert(_use_pos_and_kinds.length() % 2 == 0, "must have use kind for each use pos");
  assert(result->_use_pos_and_kinds.length() % 2 == 0, "must have use kind for each use pos");
  assert(_use_pos_and_kinds.length() + result->_use_pos_and_kinds.length() == total_len, "missed some entries");

  for (i = 0; i < _use_pos_and_kinds.length(); i += 2) {
    assert(_use_pos_and_kinds.at(i) < split_pos, "must be");
    assert(_use_pos_and_kinds.at(i + 1) >= firstValidKind && _use_pos_and_kinds.at(i + 1) <= lastValidKind, "invalid use kind");
  }
  for (i = 0; i < result->_use_pos_and_kinds.length(); i += 2) {
    assert(result->_use_pos_and_kinds.at(i) >= split_pos, "must be");
    assert(result->_use_pos_and_kinds.at(i + 1) >= firstValidKind && result->_use_pos_and_kinds.at(i + 1) <= lastValidKind, "invalid use kind");
  }
#endif

  return result;
}

// split this interval at the specified position and return
// the head as a new interval (the original interval is the tail)
//
// Currently, only the first range can be split, and the new interval
// must not have split positions
Interval* Interval::split_from_start(int split_pos) {
  assert(LinearScan::is_virtual_interval(this), "cannot split fixed intervals");
  assert(split_pos > from() && split_pos < to(), "can only split inside interval");
  assert(split_pos > _first->from() && split_pos <= _first->to(), "can only split inside first range");
  assert(first_usage(noUse) > split_pos, "can not split when use positions are present");

  // allocate new interval
  Interval* result = new_split_child();

  // the new created interval has only one range (checked by assertion above),
  // so the splitting of the ranges is very simple
  result->add_range(_first->from(), split_pos);

  if (split_pos == _first->to()) {
    assert(_first->next() != Range::end(), "must not be at end");
    _first = _first->next();
  } else {
    _first->set_from(split_pos);
  }

  return result;
}


// returns true if the op_id is inside the interval
bool Interval::covers(int op_id, LIR_OpVisitState::OprMode mode) const {
  Range* cur  = _first;

  while (cur != Range::end() && cur->to() < op_id) {
    cur = cur->next();
  }
  if (cur != Range::end()) {
    assert(cur->to() != cur->next()->from(), "ranges not separated");

    if (mode == LIR_OpVisitState::outputMode) {
      return cur->from() <= op_id && op_id < cur->to();
    } else {
      return cur->from() <= op_id && op_id <= cur->to();
    }
  }
  return false;
}

// returns true if the interval has any hole between hole_from and hole_to
// (even if the hole has only the length 1)
bool Interval::has_hole_between(int hole_from, int hole_to) {
  assert(hole_from < hole_to, "check");
  assert(from() <= hole_from && hole_to <= to(), "index out of interval");

  Range* cur  = _first;
  while (cur != Range::end()) {
    assert(cur->to() < cur->next()->from(), "no space between ranges");

    // hole-range starts before this range -> hole
    if (hole_from < cur->from()) {
      return true;

    // hole-range completely inside this range -> no hole
    } else if (hole_to <= cur->to()) {
      return false;

    // overlapping of hole-range with this range -> hole
    } else if (hole_from <= cur->to()) {
      return true;
    }

    cur = cur->next();
  }

  return false;
}

// Check if there is an intersection with any of the split children of 'interval'
bool Interval::intersects_any_children_of(Interval* interval) const {
  if (interval->_split_children != NULL) {
    for (int i = 0; i < interval->_split_children->length(); i++) {
      if (intersects(interval->_split_children->at(i))) {
        return true;
      }
    }
  }
  return false;
}


#ifndef PRODUCT
void Interval::print_on(outputStream* out, bool is_cfg_printer) const {
  const char* SpillState2Name[] = { "no definition", "no spill store", "one spill store", "store at definition", "start in memory", "no optimization" };
  const char* UseKind2Name[] = { "N", "L", "S", "M" };

  const char* type_name;
  if (reg_num() < LIR_OprDesc::vreg_base) {
    type_name = "fixed";
  } else {
    type_name = type2name(type());
  }
  out->print("%d %s ", reg_num(), type_name);

  if (is_cfg_printer) {
    // Special version for compatibility with C1 Visualizer.
    LIR_Opr opr = LinearScan::get_operand(reg_num());
    if (opr->is_valid()) {
      out->print("\"");
      opr->print(out);
      out->print("\" ");
    }
  } else {
    // Improved output for normal debugging.
    if (reg_num() < LIR_OprDesc::vreg_base) {
      LinearScan::print_reg_num(out, assigned_reg());
    } else if (assigned_reg() != -1 && (LinearScan::num_physical_regs(type()) == 1 || assigned_regHi() != -1)) {
      LinearScan::calc_operand_for_interval(this)->print(out);
    } else {
      // Virtual register that has no assigned register yet.
      out->print("[ANY]");
    }
    out->print(" ");
  }
  out->print("%d %d ", split_parent()->reg_num(), (register_hint(false) != NULL ? register_hint(false)->reg_num() : -1));

  // print ranges
  Range* cur = _first;
  while (cur != Range::end()) {
    cur->print(out);
    cur = cur->next();
    assert(cur != NULL, "range list not closed with range sentinel");
  }

  // print use positions
  int prev = 0;
  assert(_use_pos_and_kinds.length() % 2 == 0, "must be");
  for (int i =_use_pos_and_kinds.length() - 2; i >= 0; i -= 2) {
    assert(_use_pos_and_kinds.at(i + 1) >= firstValidKind && _use_pos_and_kinds.at(i + 1) <= lastValidKind, "invalid use kind");
    assert(prev < _use_pos_and_kinds.at(i), "use positions not sorted");

    out->print("%d %s ", _use_pos_and_kinds.at(i), UseKind2Name[_use_pos_and_kinds.at(i + 1)]);
    prev = _use_pos_and_kinds.at(i);
  }

  out->print(" \"%s\"", SpillState2Name[spill_state()]);
  out->cr();
}

void Interval::print_parent() const {
  if (_split_parent != this) {
    _split_parent->print_on(tty);
  } else {
    tty->print_cr("Parent: this");
  }
}

void Interval::print_children() const {
  if (_split_children == NULL) {
    tty->print_cr("Children: []");
  } else {
    tty->print_cr("Children:");
    for (int i = 0; i < _split_children->length(); i++) {
      tty->print("%d: ", i);
      _split_children->at(i)->print_on(tty);
    }
  }
}
#endif // NOT PRODUCT




// **** Implementation of IntervalWalker ****************************

IntervalWalker::IntervalWalker(LinearScan* allocator, Interval* unhandled_fixed_first, Interval* unhandled_any_first)
 : _compilation(allocator->compilation())
 , _allocator(allocator)
{
  _unhandled_first[fixedKind] = unhandled_fixed_first;
  _unhandled_first[anyKind]   = unhandled_any_first;
  _active_first[fixedKind]    = Interval::end();
  _inactive_first[fixedKind]  = Interval::end();
  _active_first[anyKind]      = Interval::end();
  _inactive_first[anyKind]    = Interval::end();
  _current_position = -1;
  _current = NULL;
  next_interval();
}


// append interval in order of current range from()
void IntervalWalker::append_sorted(Interval** list, Interval* interval) {
  Interval* prev = NULL;
  Interval* cur  = *list;
  while (cur->current_from() < interval->current_from()) {
    prev = cur; cur = cur->next();
  }
  if (prev == NULL) {
    *list = interval;
  } else {
    prev->set_next(interval);
  }
  interval->set_next(cur);
}

void IntervalWalker::append_to_unhandled(Interval** list, Interval* interval) {
  assert(interval->from() >= current()->current_from(), "cannot append new interval before current walk position");

  Interval* prev = NULL;
  Interval* cur  = *list;
  while (cur->from() < interval->from() || (cur->from() == interval->from() && cur->first_usage(noUse) < interval->first_usage(noUse))) {
    prev = cur; cur = cur->next();
  }
  if (prev == NULL) {
    *list = interval;
  } else {
    prev->set_next(interval);
  }
  interval->set_next(cur);
}


inline bool IntervalWalker::remove_from_list(Interval** list, Interval* i) {
  while (*list != Interval::end() && *list != i) {
    list = (*list)->next_addr();
  }
  if (*list != Interval::end()) {
    assert(*list == i, "check");
    *list = (*list)->next();
    return true;
  } else {
    return false;
  }
}

void IntervalWalker::remove_from_list(Interval* i) {
  bool deleted;

  if (i->state() == activeState) {
    deleted = remove_from_list(active_first_addr(anyKind), i);
  } else {
    assert(i->state() == inactiveState, "invalid state");
    deleted = remove_from_list(inactive_first_addr(anyKind), i);
  }

  assert(deleted, "interval has not been found in list");
}


void IntervalWalker::walk_to(IntervalState state, int from) {
  assert (state == activeState || state == inactiveState, "wrong state");
  for_each_interval_kind(kind) {
    Interval** prev = state == activeState ? active_first_addr(kind) : inactive_first_addr(kind);
    Interval* next   = *prev;
    while (next->current_from() <= from) {
      Interval* cur = next;
      next = cur->next();

      bool range_has_changed = false;
      while (cur->current_to() <= from) {
        cur->next_range();
        range_has_changed = true;
      }

      // also handle move from inactive list to active list
      range_has_changed = range_has_changed || (state == inactiveState && cur->current_from() <= from);

      if (range_has_changed) {
        // remove cur from list
        *prev = next;
        if (cur->current_at_end()) {
          // move to handled state (not maintained as a list)
          cur->set_state(handledState);
          DEBUG_ONLY(interval_moved(cur, kind, state, handledState);)
        } else if (cur->current_from() <= from){
          // sort into active list
          append_sorted(active_first_addr(kind), cur);
          cur->set_state(activeState);
          if (*prev == cur) {
            assert(state == activeState, "check");
            prev = cur->next_addr();
          }
          DEBUG_ONLY(interval_moved(cur, kind, state, activeState);)
        } else {
          // sort into inactive list
          append_sorted(inactive_first_addr(kind), cur);
          cur->set_state(inactiveState);
          if (*prev == cur) {
            assert(state == inactiveState, "check");
            prev = cur->next_addr();
          }
          DEBUG_ONLY(interval_moved(cur, kind, state, inactiveState);)
        }
      } else {
        prev = cur->next_addr();
        continue;
      }
    }
  }
}


void IntervalWalker::next_interval() {
  IntervalKind kind;
  Interval* any   = _unhandled_first[anyKind];
  Interval* fixed = _unhandled_first[fixedKind];

  if (any != Interval::end()) {
    // intervals may start at same position -> prefer fixed interval
    kind = fixed != Interval::end() && fixed->from() <= any->from() ? fixedKind : anyKind;

    assert (kind == fixedKind && fixed->from() <= any->from() ||
            kind == anyKind   && any->from() <= fixed->from(), "wrong interval!!!");
    assert(any == Interval::end() || fixed == Interval::end() || any->from() != fixed->from() || kind == fixedKind, "if fixed and any-Interval start at same position, fixed must be processed first");

  } else if (fixed != Interval::end()) {
    kind = fixedKind;
  } else {
    _current = NULL; return;
  }
  _current_kind = kind;
  _current = _unhandled_first[kind];
  _unhandled_first[kind] = _current->next();
  _current->set_next(Interval::end());
  _current->rewind_range();
}


void IntervalWalker::walk_to(int lir_op_id) {
  assert(_current_position <= lir_op_id, "can not walk backwards");
  while (current() != NULL) {
    bool is_active = current()->from() <= lir_op_id;
    int id = is_active ? current()->from() : lir_op_id;

    TRACE_LINEAR_SCAN(2, if (_current_position < id) { tty->cr(); tty->print_cr("walk_to(%d) **************************************************************", id); })

    // set _current_position prior to call of walk_to
    _current_position = id;

    // call walk_to even if _current_position == id
    walk_to(activeState, id);
    walk_to(inactiveState, id);

    if (is_active) {
      current()->set_state(activeState);
      if (activate_current()) {
        append_sorted(active_first_addr(current_kind()), current());
        DEBUG_ONLY(interval_moved(current(), current_kind(), unhandledState, activeState);)
      }

      next_interval();
    } else {
      return;
    }
  }
}

#ifdef ASSERT
void IntervalWalker::interval_moved(Interval* interval, IntervalKind kind, IntervalState from, IntervalState to) {
  if (TraceLinearScanLevel >= 4) {
    #define print_state(state) \
    switch(state) {\
      case unhandledState: tty->print("unhandled"); break;\
      case activeState: tty->print("active"); break;\
      case inactiveState: tty->print("inactive"); break;\
      case handledState: tty->print("handled"); break;\
      default: ShouldNotReachHere(); \
    }

    print_state(from); tty->print(" to "); print_state(to);
    tty->fill_to(23);
    interval->print();

    #undef print_state
  }
}
#endif // ASSERT

// **** Implementation of LinearScanWalker **************************

LinearScanWalker::LinearScanWalker(LinearScan* allocator, Interval* unhandled_fixed_first, Interval* unhandled_any_first)
  : IntervalWalker(allocator, unhandled_fixed_first, unhandled_any_first)
  , _move_resolver(allocator)
{
  for (int i = 0; i < LinearScan::nof_regs; i++) {
    _spill_intervals[i] = new IntervalList(2);
  }
}


inline void LinearScanWalker::init_use_lists(bool only_process_use_pos) {
  for (int i = _first_reg; i <= _last_reg; i++) {
    _use_pos[i] = max_jint;

    if (!only_process_use_pos) {
      _block_pos[i] = max_jint;
      _spill_intervals[i]->clear();
    }
  }
}

inline void LinearScanWalker::exclude_from_use(int reg) {
  assert(reg < LinearScan::nof_regs, "interval must have a register assigned (stack slots not allowed)");
  if (reg >= _first_reg && reg <= _last_reg) {
    _use_pos[reg] = 0;
  }
}
inline void LinearScanWalker::exclude_from_use(Interval* i) {
  assert(i->assigned_reg() != any_reg, "interval has no register assigned");

  exclude_from_use(i->assigned_reg());
  exclude_from_use(i->assigned_regHi());
}

inline void LinearScanWalker::set_use_pos(int reg, Interval* i, int use_pos, bool only_process_use_pos) {
  assert(use_pos != 0, "must use exclude_from_use to set use_pos to 0");

  if (reg >= _first_reg && reg <= _last_reg) {
    if (_use_pos[reg] > use_pos) {
      _use_pos[reg] = use_pos;
    }
    if (!only_process_use_pos) {
      _spill_intervals[reg]->append(i);
    }
  }
}
inline void LinearScanWalker::set_use_pos(Interval* i, int use_pos, bool only_process_use_pos) {
  assert(i->assigned_reg() != any_reg, "interval has no register assigned");
  if (use_pos != -1) {
    set_use_pos(i->assigned_reg(), i, use_pos, only_process_use_pos);
    set_use_pos(i->assigned_regHi(), i, use_pos, only_process_use_pos);
  }
}

inline void LinearScanWalker::set_block_pos(int reg, Interval* i, int block_pos) {
  if (reg >= _first_reg && reg <= _last_reg) {
    if (_block_pos[reg] > block_pos) {
      _block_pos[reg] = block_pos;
    }
    if (_use_pos[reg] > block_pos) {
      _use_pos[reg] = block_pos;
    }
  }
}
inline void LinearScanWalker::set_block_pos(Interval* i, int block_pos) {
  assert(i->assigned_reg() != any_reg, "interval has no register assigned");
  if (block_pos != -1) {
    set_block_pos(i->assigned_reg(), i, block_pos);
    set_block_pos(i->assigned_regHi(), i, block_pos);
  }
}


void LinearScanWalker::free_exclude_active_fixed() {
  Interval* list = active_first(fixedKind);
  while (list != Interval::end()) {
    assert(list->assigned_reg() < LinearScan::nof_regs, "active interval must have a register assigned");
    exclude_from_use(list);
    list = list->next();
  }
}

void LinearScanWalker::free_exclude_active_any() {
  Interval* list = active_first(anyKind);
  while (list != Interval::end()) {
    exclude_from_use(list);
    list = list->next();
  }
}

void LinearScanWalker::free_collect_inactive_fixed(Interval* cur) {
  Interval* list = inactive_first(fixedKind);
  while (list != Interval::end()) {
    if (cur->to() <= list->current_from()) {
      assert(list->current_intersects_at(cur) == -1, "must not intersect");
      set_use_pos(list, list->current_from(), true);
    } else {
      set_use_pos(list, list->current_intersects_at(cur), true);
    }
    list = list->next();
  }
}

void LinearScanWalker::free_collect_inactive_any(Interval* cur) {
  Interval* list = inactive_first(anyKind);
  while (list != Interval::end()) {
    set_use_pos(list, list->current_intersects_at(cur), true);
    list = list->next();
  }
}

void LinearScanWalker::spill_exclude_active_fixed() {
  Interval* list = active_first(fixedKind);
  while (list != Interval::end()) {
    exclude_from_use(list);
    list = list->next();
  }
}

void LinearScanWalker::spill_block_inactive_fixed(Interval* cur) {
  Interval* list = inactive_first(fixedKind);
  while (list != Interval::end()) {
    if (cur->to() > list->current_from()) {
      set_block_pos(list, list->current_intersects_at(cur));
    } else {
      assert(list->current_intersects_at(cur) == -1, "invalid optimization: intervals intersect");
    }

    list = list->next();
  }
}

void LinearScanWalker::spill_collect_active_any() {
  Interval* list = active_first(anyKind);
  while (list != Interval::end()) {
    set_use_pos(list, MIN2(list->next_usage(loopEndMarker, _current_position), list->to()), false);
    list = list->next();
  }
}

void LinearScanWalker::spill_collect_inactive_any(Interval* cur) {
  Interval* list = inactive_first(anyKind);
  while (list != Interval::end()) {
    if (list->current_intersects(cur)) {
      set_use_pos(list, MIN2(list->next_usage(loopEndMarker, _current_position), list->to()), false);
    }
    list = list->next();
  }
}


void LinearScanWalker::insert_move(int op_id, Interval* src_it, Interval* dst_it) {
  // output all moves here. When source and target are equal, the move is
  // optimized away later in assign_reg_nums

  op_id = (op_id + 1) & ~1;
  BlockBegin* op_block = allocator()->block_of_op_with_id(op_id);
  assert(op_id > 0 && allocator()->block_of_op_with_id(op_id - 2) == op_block, "cannot insert move at block boundary");

  // calculate index of instruction inside instruction list of current block
  // the minimal index (for a block with no spill moves) can be calculated because the
  // numbering of instructions is known.
  // When the block already contains spill moves, the index must be increased until the
  // correct index is reached.
  LIR_OpList* list = op_block->lir()->instructions_list();
  int index = (op_id - list->at(0)->id()) / 2;
  assert(list->at(index)->id() <= op_id, "error in calculation");

  while (list->at(index)->id() != op_id) {
    index++;
    assert(0 <= index && index < list->length(), "index out of bounds");
  }
  assert(1 <= index && index < list->length(), "index out of bounds");
  assert(list->at(index)->id() == op_id, "error in calculation");

  // insert new instruction before instruction at position index
  _move_resolver.move_insert_position(op_block->lir(), index - 1);
  _move_resolver.add_mapping(src_it, dst_it);
}


int LinearScanWalker::find_optimal_split_pos(BlockBegin* min_block, BlockBegin* max_block, int max_split_pos) {
  int from_block_nr = min_block->linear_scan_number();
  int to_block_nr = max_block->linear_scan_number();

  assert(0 <= from_block_nr && from_block_nr < block_count(), "out of range");
  assert(0 <= to_block_nr && to_block_nr < block_count(), "out of range");
  assert(from_block_nr < to_block_nr, "must cross block boundary");

  // Try to split at end of max_block. If this would be after
  // max_split_pos, then use the begin of max_block
  int optimal_split_pos = max_block->last_lir_instruction_id() + 2;
  if (optimal_split_pos > max_split_pos) {
    optimal_split_pos = max_block->first_lir_instruction_id();
  }

  int min_loop_depth = max_block->loop_depth();
  for (int i = to_block_nr - 1; i >= from_block_nr; i--) {
    BlockBegin* cur = block_at(i);

    if (cur->loop_depth() < min_loop_depth) {
      // block with lower loop-depth found -> split at the end of this block
      min_loop_depth = cur->loop_depth();
      optimal_split_pos = cur->last_lir_instruction_id() + 2;
    }
  }
  assert(optimal_split_pos > allocator()->max_lir_op_id() || allocator()->is_block_begin(optimal_split_pos), "algorithm must move split pos to block boundary");

  return optimal_split_pos;
}


int LinearScanWalker::find_optimal_split_pos(Interval* it, int min_split_pos, int max_split_pos, bool do_loop_optimization) {
  int optimal_split_pos = -1;
  if (min_split_pos == max_split_pos) {
    // trivial case, no optimization of split position possible
    TRACE_LINEAR_SCAN(4, tty->print_cr("      min-pos and max-pos are equal, no optimization possible"));
    optimal_split_pos = min_split_pos;

  } else {
    assert(min_split_pos < max_split_pos, "must be true then");
    assert(min_split_pos > 0, "cannot access min_split_pos - 1 otherwise");

    // reason for using min_split_pos - 1: when the minimal split pos is exactly at the
    // beginning of a block, then min_split_pos is also a possible split position.
    // Use the block before as min_block, because then min_block->last_lir_instruction_id() + 2 == min_split_pos
    BlockBegin* min_block = allocator()->block_of_op_with_id(min_split_pos - 1);

    // reason for using max_split_pos - 1: otherwise there would be an assertion failure
    // when an interval ends at the end of the last block of the method
    // (in this case, max_split_pos == allocator()->max_lir_op_id() + 2, and there is no
    // block at this op_id)
    BlockBegin* max_block = allocator()->block_of_op_with_id(max_split_pos - 1);

    assert(min_block->linear_scan_number() <= max_block->linear_scan_number(), "invalid order");
    if (min_block == max_block) {
      // split position cannot be moved to block boundary, so split as late as possible
      TRACE_LINEAR_SCAN(4, tty->print_cr("      cannot move split pos to block boundary because min_pos and max_pos are in same block"));
      optimal_split_pos = max_split_pos;

    } else if (it->has_hole_between(max_split_pos - 1, max_split_pos) && !allocator()->is_block_begin(max_split_pos)) {
      // Do not move split position if the interval has a hole before max_split_pos.
      // Intervals resulting from Phi-Functions have more than one definition (marked
      // as mustHaveRegister) with a hole before each definition. When the register is needed
      // for the second definition, an earlier reloading is unnecessary.
      TRACE_LINEAR_SCAN(4, tty->print_cr("      interval has hole just before max_split_pos, so splitting at max_split_pos"));
      optimal_split_pos = max_split_pos;

    } else {
      // seach optimal block boundary between min_split_pos and max_split_pos
      TRACE_LINEAR_SCAN(4, tty->print_cr("      moving split pos to optimal block boundary between block B%d and B%d", min_block->block_id(), max_block->block_id()));

      if (do_loop_optimization) {
        // Loop optimization: if a loop-end marker is found between min- and max-position,
        // then split before this loop
        int loop_end_pos = it->next_usage_exact(loopEndMarker, min_block->last_lir_instruction_id() + 2);
        TRACE_LINEAR_SCAN(4, tty->print_cr("      loop optimization: loop end found at pos %d", loop_end_pos));

        assert(loop_end_pos > min_split_pos, "invalid order");
        if (loop_end_pos < max_split_pos) {
          // loop-end marker found between min- and max-position
          // if it is not the end marker for the same loop as the min-position, then move
          // the max-position to this loop block.
          // Desired result: uses tagged as shouldHaveRegister inside a loop cause a reloading
          // of the interval (normally, only mustHaveRegister causes a reloading)
          BlockBegin* loop_block = allocator()->block_of_op_with_id(loop_end_pos);

          TRACE_LINEAR_SCAN(4, tty->print_cr("      interval is used in loop that ends in block B%d, so trying to move max_block back from B%d to B%d", loop_block->block_id(), max_block->block_id(), loop_block->block_id()));
          assert(loop_block != min_block, "loop_block and min_block must be different because block boundary is needed between");

          optimal_split_pos = find_optimal_split_pos(min_block, loop_block, loop_block->last_lir_instruction_id() + 2);
          if (optimal_split_pos == loop_block->last_lir_instruction_id() + 2) {
            optimal_split_pos = -1;
            TRACE_LINEAR_SCAN(4, tty->print_cr("      loop optimization not necessary"));
          } else {
            TRACE_LINEAR_SCAN(4, tty->print_cr("      loop optimization successful"));
          }
        }
      }

      if (optimal_split_pos == -1) {
        // not calculated by loop optimization
        optimal_split_pos = find_optimal_split_pos(min_block, max_block, max_split_pos);
      }
    }
  }
  TRACE_LINEAR_SCAN(4, tty->print_cr("      optimal split position: %d", optimal_split_pos));

  return optimal_split_pos;
}


/*
  split an interval at the optimal position between min_split_pos and
  max_split_pos in two parts:
  1) the left part has already a location assigned
  2) the right part is sorted into to the unhandled-list
*/
void LinearScanWalker::split_before_usage(Interval* it, int min_split_pos, int max_split_pos) {
  TRACE_LINEAR_SCAN(2, tty->print   ("----- splitting interval: "); it->print());
  TRACE_LINEAR_SCAN(2, tty->print_cr("      between %d and %d", min_split_pos, max_split_pos));

  assert(it->from() < min_split_pos,         "cannot split at start of interval");
  assert(current_position() < min_split_pos, "cannot split before current position");
  assert(min_split_pos <= max_split_pos,     "invalid order");
  assert(max_split_pos <= it->to(),          "cannot split after end of interval");

  int optimal_split_pos = find_optimal_split_pos(it, min_split_pos, max_split_pos, true);

  assert(min_split_pos <= optimal_split_pos && optimal_split_pos <= max_split_pos, "out of range");
  assert(optimal_split_pos <= it->to(),  "cannot split after end of interval");
  assert(optimal_split_pos > it->from(), "cannot split at start of interval");

  if (optimal_split_pos == it->to() && it->next_usage(mustHaveRegister, min_split_pos) == max_jint) {
    // the split position would be just before the end of the interval
    // -> no split at all necessary
    TRACE_LINEAR_SCAN(4, tty->print_cr("      no split necessary because optimal split position is at end of interval"));
    return;
  }

  // must calculate this before the actual split is performed and before split position is moved to odd op_id
  bool move_necessary = !allocator()->is_block_begin(optimal_split_pos) && !it->has_hole_between(optimal_split_pos - 1, optimal_split_pos);

  if (!allocator()->is_block_begin(optimal_split_pos)) {
    // move position before actual instruction (odd op_id)
    optimal_split_pos = (optimal_split_pos - 1) | 1;
  }

  TRACE_LINEAR_SCAN(4, tty->print_cr("      splitting at position %d", optimal_split_pos));
  assert(allocator()->is_block_begin(optimal_split_pos) || (optimal_split_pos % 2 == 1), "split pos must be odd when not on block boundary");
  assert(!allocator()->is_block_begin(optimal_split_pos) || (optimal_split_pos % 2 == 0), "split pos must be even on block boundary");

  Interval* split_part = it->split(optimal_split_pos);

  allocator()->append_interval(split_part);
  allocator()->copy_register_flags(it, split_part);
  split_part->set_insert_move_when_activated(move_necessary);
  append_to_unhandled(unhandled_first_addr(anyKind), split_part);

  TRACE_LINEAR_SCAN(2, tty->print_cr("      split interval in two parts (insert_move_when_activated: %d)", move_necessary));
  TRACE_LINEAR_SCAN(2, tty->print   ("      "); it->print());
  TRACE_LINEAR_SCAN(2, tty->print   ("      "); split_part->print());
}

/*
  split an interval at the optimal position between min_split_pos and
  max_split_pos in two parts:
  1) the left part has already a location assigned
  2) the right part is always on the stack and therefore ignored in further processing
*/
void LinearScanWalker::split_for_spilling(Interval* it) {
  // calculate allowed range of splitting position
  int max_split_pos = current_position();
  int min_split_pos = MAX2(it->previous_usage(shouldHaveRegister, max_split_pos) + 1, it->from());

  TRACE_LINEAR_SCAN(2, tty->print   ("----- splitting and spilling interval: "); it->print());
  TRACE_LINEAR_SCAN(2, tty->print_cr("      between %d and %d", min_split_pos, max_split_pos));

  assert(it->state() == activeState,     "why spill interval that is not active?");
  assert(it->from() <= min_split_pos,    "cannot split before start of interval");
  assert(min_split_pos <= max_split_pos, "invalid order");
  assert(max_split_pos < it->to(),       "cannot split at end end of interval");
  assert(current_position() < it->to(),  "interval must not end before current position");

  if (min_split_pos == it->from()) {
    // the whole interval is never used, so spill it entirely to memory
    TRACE_LINEAR_SCAN(2, tty->print_cr("      spilling entire interval because split pos is at beginning of interval"));
    assert(it->first_usage(shouldHaveRegister) > current_position(), "interval must not have use position before current_position");

    allocator()->assign_spill_slot(it);
    allocator()->change_spill_state(it, min_split_pos);

    // Also kick parent intervals out of register to memory when they have no use
    // position. This avoids short interval in register surrounded by intervals in
    // memory -> avoid useless moves from memory to register and back
    Interval* parent = it;
    while (parent != NULL && parent->is_split_child()) {
      parent = parent->split_child_before_op_id(parent->from());

      if (parent->assigned_reg() < LinearScan::nof_regs) {
        if (parent->first_usage(shouldHaveRegister) == max_jint) {
          // parent is never used, so kick it out of its assigned register
          TRACE_LINEAR_SCAN(4, tty->print_cr("      kicking out interval %d out of its register because it is never used", parent->reg_num()));
          allocator()->assign_spill_slot(parent);
        } else {
          // do not go further back because the register is actually used by the interval
          parent = NULL;
        }
      }
    }

  } else {
    // search optimal split pos, split interval and spill only the right hand part
    int optimal_split_pos = find_optimal_split_pos(it, min_split_pos, max_split_pos, false);

    assert(min_split_pos <= optimal_split_pos && optimal_split_pos <= max_split_pos, "out of range");
    assert(optimal_split_pos < it->to(), "cannot split at end of interval");
    assert(optimal_split_pos >= it->from(), "cannot split before start of interval");

    if (!allocator()->is_block_begin(optimal_split_pos)) {
      // move position before actual instruction (odd op_id)
      optimal_split_pos = (optimal_split_pos - 1) | 1;
    }

    TRACE_LINEAR_SCAN(4, tty->print_cr("      splitting at position %d", optimal_split_pos));
    assert(allocator()->is_block_begin(optimal_split_pos)  || (optimal_split_pos % 2 == 1), "split pos must be odd when not on block boundary");
    assert(!allocator()->is_block_begin(optimal_split_pos) || (optimal_split_pos % 2 == 0), "split pos must be even on block boundary");

    Interval* spilled_part = it->split(optimal_split_pos);
    allocator()->append_interval(spilled_part);
    allocator()->assign_spill_slot(spilled_part);
    allocator()->change_spill_state(spilled_part, optimal_split_pos);

    if (!allocator()->is_block_begin(optimal_split_pos)) {
      TRACE_LINEAR_SCAN(4, tty->print_cr("      inserting move from interval %d to %d", it->reg_num(), spilled_part->reg_num()));
      insert_move(optimal_split_pos, it, spilled_part);
    }

    // the current_split_child is needed later when moves are inserted for reloading
    assert(spilled_part->current_split_child() == it, "overwriting wrong current_split_child");
    spilled_part->make_current_split_child();

    TRACE_LINEAR_SCAN(2, tty->print_cr("      split interval in two parts"));
    TRACE_LINEAR_SCAN(2, tty->print   ("      "); it->print());
    TRACE_LINEAR_SCAN(2, tty->print   ("      "); spilled_part->print());
  }
}


void LinearScanWalker::split_stack_interval(Interval* it) {
  int min_split_pos = current_position() + 1;
  int max_split_pos = MIN2(it->first_usage(shouldHaveRegister), it->to());

  split_before_usage(it, min_split_pos, max_split_pos);
}

void LinearScanWalker::split_when_partial_register_available(Interval* it, int register_available_until) {
  int min_split_pos = MAX2(it->previous_usage(shouldHaveRegister, register_available_until), it->from() + 1);
  int max_split_pos = register_available_until;

  split_before_usage(it, min_split_pos, max_split_pos);
}

void LinearScanWalker::split_and_spill_interval(Interval* it) {
  assert(it->state() == activeState || it->state() == inactiveState, "other states not allowed");

  int current_pos = current_position();
  if (it->state() == inactiveState) {
    // the interval is currently inactive, so no spill slot is needed for now.
    // when the split part is activated, the interval has a new chance to get a register,
    // so in the best case no stack slot is necessary
    assert(it->has_hole_between(current_pos - 1, current_pos + 1), "interval can not be inactive otherwise");
    split_before_usage(it, current_pos + 1, current_pos + 1);

  } else {
    // search the position where the interval must have a register and split
    // at the optimal position before.
    // The new created part is added to the unhandled list and will get a register
    // when it is activated
    int min_split_pos = current_pos + 1;
    int max_split_pos = MIN2(it->next_usage(mustHaveRegister, min_split_pos), it->to());

    split_before_usage(it, min_split_pos, max_split_pos);

    assert(it->next_usage(mustHaveRegister, current_pos) == max_jint, "the remaining part is spilled to stack and therefore has no register");
    split_for_spilling(it);
  }
}

int LinearScanWalker::find_free_reg(int reg_needed_until, int interval_to, int hint_reg, int ignore_reg, bool* need_split) {
  int min_full_reg = any_reg;
  int max_partial_reg = any_reg;

  for (int i = _first_reg; i <= _last_reg; i++) {
    if (i == ignore_reg) {
      // this register must be ignored

    } else if (_use_pos[i] >= interval_to) {
      // this register is free for the full interval
      if (min_full_reg == any_reg || i == hint_reg || (_use_pos[i] < _use_pos[min_full_reg] && min_full_reg != hint_reg)) {
        min_full_reg = i;
      }
    } else if (_use_pos[i] > reg_needed_until) {
      // this register is at least free until reg_needed_until
      if (max_partial_reg == any_reg || i == hint_reg || (_use_pos[i] > _use_pos[max_partial_reg] && max_partial_reg != hint_reg)) {
        max_partial_reg = i;
      }
    }
  }

  if (min_full_reg != any_reg) {
    return min_full_reg;
  } else if (max_partial_reg != any_reg) {
    *need_split = true;
    return max_partial_reg;
  } else {
    return any_reg;
  }
}

int LinearScanWalker::find_free_double_reg(int reg_needed_until, int interval_to, int hint_reg, bool* need_split) {
  assert((_last_reg - _first_reg + 1) % 2 == 0, "adjust algorithm");

  int min_full_reg = any_reg;
  int max_partial_reg = any_reg;

  for (int i = _first_reg; i < _last_reg; i+=2) {
    if (_use_pos[i] >= interval_to && _use_pos[i + 1] >= interval_to) {
      // this register is free for the full interval
      if (min_full_reg == any_reg || i == hint_reg || (_use_pos[i] < _use_pos[min_full_reg] && min_full_reg != hint_reg)) {
        min_full_reg = i;
      }
    } else if (_use_pos[i] > reg_needed_until && _use_pos[i + 1] > reg_needed_until) {
      // this register is at least free until reg_needed_until
      if (max_partial_reg == any_reg || i == hint_reg || (_use_pos[i] > _use_pos[max_partial_reg] && max_partial_reg != hint_reg)) {
        max_partial_reg = i;
      }
    }
  }

  if (min_full_reg != any_reg) {
    return min_full_reg;
  } else if (max_partial_reg != any_reg) {
    *need_split = true;
    return max_partial_reg;
  } else {
    return any_reg;
  }
}

bool LinearScanWalker::alloc_free_reg(Interval* cur) {
  TRACE_LINEAR_SCAN(2, tty->print("trying to find free register for "); cur->print());

  init_use_lists(true);
  free_exclude_active_fixed();
  free_exclude_active_any();
  free_collect_inactive_fixed(cur);
  free_collect_inactive_any(cur);
  assert(unhandled_first(fixedKind) == Interval::end(), "must not have unhandled fixed intervals because all fixed intervals have a use at position 0");

  // _use_pos contains the start of the next interval that has this register assigned
  // (either as a fixed register or a normal allocated register in the past)
  // only intervals overlapping with cur are processed, non-overlapping invervals can be ignored safely
#ifdef ASSERT
  if (TraceLinearScanLevel >= 4) {
    tty->print_cr("      state of registers:");
    for (int i = _first_reg; i <= _last_reg; i++) {
      tty->print("      reg %d (", i);
      LinearScan::print_reg_num(i);
      tty->print_cr("): use_pos: %d", _use_pos[i]);
    }
  }
#endif

  int hint_reg, hint_regHi;
  Interval* register_hint = cur->register_hint();
  if (register_hint != NULL) {
    hint_reg = register_hint->assigned_reg();
    hint_regHi = register_hint->assigned_regHi();

    if (_num_phys_regs == 2 && allocator()->is_precolored_cpu_interval(register_hint)) {
      assert(hint_reg != any_reg && hint_regHi == any_reg, "must be for fixed intervals");
      hint_regHi = hint_reg + 1;  // connect e.g. eax-edx
    }
#ifdef ASSERT
    if (TraceLinearScanLevel >= 4) {
      tty->print("      hint registers %d (", hint_reg);
      LinearScan::print_reg_num(hint_reg);
      tty->print("), %d (", hint_regHi);
      LinearScan::print_reg_num(hint_regHi);
      tty->print(") from interval ");
      register_hint->print();
    }
#endif
  } else {
    hint_reg = any_reg;
    hint_regHi = any_reg;
  }
  assert(hint_reg == any_reg || hint_reg != hint_regHi, "hint reg and regHi equal");
  assert(cur->assigned_reg() == any_reg && cur->assigned_regHi() == any_reg, "register already assigned to interval");

  // the register must be free at least until this position
  int reg_needed_until = cur->from() + 1;
  int interval_to = cur->to();

  bool need_split = false;
  int split_pos;
  int reg;
  int regHi = any_reg;

  if (_adjacent_regs) {
    reg = find_free_double_reg(reg_needed_until, interval_to, hint_reg, &need_split);
    regHi = reg + 1;
    if (reg == any_reg) {
      return false;
    }
    split_pos = MIN2(_use_pos[reg], _use_pos[regHi]);

  } else {
    reg = find_free_reg(reg_needed_until, interval_to, hint_reg, any_reg, &need_split);
    if (reg == any_reg) {
      return false;
    }
    split_pos = _use_pos[reg];

    if (_num_phys_regs == 2) {
      regHi = find_free_reg(reg_needed_until, interval_to, hint_regHi, reg, &need_split);

      if (_use_pos[reg] < interval_to && regHi == any_reg) {
        // do not split interval if only one register can be assigned until the split pos
        // (when one register is found for the whole interval, split&spill is only
        // performed for the hi register)
        return false;

      } else if (regHi != any_reg) {
        split_pos = MIN2(split_pos, _use_pos[regHi]);

        // sort register numbers to prevent e.g. a move from eax,ebx to ebx,eax
        if (reg > regHi) {
          int temp = reg;
          reg = regHi;
          regHi = temp;
        }
      }
    }
  }

  cur->assign_reg(reg, regHi);
#ifdef ASSERT
  if (TraceLinearScanLevel >= 2) {
    tty->print("      selected registers %d (", reg);
    LinearScan::print_reg_num(reg);
    tty->print("), %d (", regHi);
    LinearScan::print_reg_num(regHi);
    tty->print_cr(")");
  }
#endif
  assert(split_pos > 0, "invalid split_pos");
  if (need_split) {
    // register not available for full interval, so split it
    split_when_partial_register_available(cur, split_pos);
  }

  // only return true if interval is completely assigned
  return _num_phys_regs == 1 || regHi != any_reg;
}


int LinearScanWalker::find_locked_reg(int reg_needed_until, int interval_to, int ignore_reg, bool* need_split) {
  int max_reg = any_reg;

  for (int i = _first_reg; i <= _last_reg; i++) {
    if (i == ignore_reg) {
      // this register must be ignored

    } else if (_use_pos[i] > reg_needed_until) {
      if (max_reg == any_reg || _use_pos[i] > _use_pos[max_reg]) {
        max_reg = i;
      }
    }
  }

  if (max_reg != any_reg && _block_pos[max_reg] <= interval_to) {
    *need_split = true;
  }

  return max_reg;
}

int LinearScanWalker::find_locked_double_reg(int reg_needed_until, int interval_to, bool* need_split) {
  assert((_last_reg - _first_reg + 1) % 2 == 0, "adjust algorithm");

  int max_reg = any_reg;

  for (int i = _first_reg; i < _last_reg; i+=2) {
    if (_use_pos[i] > reg_needed_until && _use_pos[i + 1] > reg_needed_until) {
      if (max_reg == any_reg || _use_pos[i] > _use_pos[max_reg]) {
        max_reg = i;
      }
    }
  }

  if (max_reg != any_reg &&
      (_block_pos[max_reg] <= interval_to || _block_pos[max_reg + 1] <= interval_to)) {
    *need_split = true;
  }

  return max_reg;
}

void LinearScanWalker::split_and_spill_intersecting_intervals(int reg, int regHi) {
  assert(reg != any_reg, "no register assigned");

  for (int i = 0; i < _spill_intervals[reg]->length(); i++) {
    Interval* it = _spill_intervals[reg]->at(i);
    remove_from_list(it);
    split_and_spill_interval(it);
  }

  if (regHi != any_reg) {
    IntervalList* processed = _spill_intervals[reg];
    for (int i = 0; i < _spill_intervals[regHi]->length(); i++) {
      Interval* it = _spill_intervals[regHi]->at(i);
      if (processed->find(it) == -1) {
        remove_from_list(it);
        split_and_spill_interval(it);
      }
    }
  }
}


// Split an Interval and spill it to memory so that cur can be placed in a register
void LinearScanWalker::alloc_locked_reg(Interval* cur) {
  TRACE_LINEAR_SCAN(2, tty->print("need to split and spill to get register for "); cur->print());

  // collect current usage of registers
  init_use_lists(false);
  spill_exclude_active_fixed();
  assert(unhandled_first(fixedKind) == Interval::end(), "must not have unhandled fixed intervals because all fixed intervals have a use at position 0");
  spill_block_inactive_fixed(cur);
  spill_collect_active_any();
  spill_collect_inactive_any(cur);

#ifdef ASSERT
  if (TraceLinearScanLevel >= 4) {
    tty->print_cr("      state of registers:");
    for (int i = _first_reg; i <= _last_reg; i++) {
      tty->print("      reg %d(", i);
      LinearScan::print_reg_num(i);
      tty->print("): use_pos: %d, block_pos: %d, intervals: ", _use_pos[i], _block_pos[i]);
      for (int j = 0; j < _spill_intervals[i]->length(); j++) {
        tty->print("%d ", _spill_intervals[i]->at(j)->reg_num());
      }
      tty->cr();
    }
  }
#endif

  // the register must be free at least until this position
  int reg_needed_until = MIN2(cur->first_usage(mustHaveRegister), cur->from() + 1);
  int interval_to = cur->to();
  assert (reg_needed_until > 0 && reg_needed_until < max_jint, "interval has no use");

  int split_pos = 0;
  int use_pos = 0;
  bool need_split = false;
  int reg, regHi;

  if (_adjacent_regs) {
    reg = find_locked_double_reg(reg_needed_until, interval_to, &need_split);
    regHi = reg + 1;

    if (reg != any_reg) {
      use_pos = MIN2(_use_pos[reg], _use_pos[regHi]);
      split_pos = MIN2(_block_pos[reg], _block_pos[regHi]);
    }
  } else {
    reg = find_locked_reg(reg_needed_until, interval_to, cur->assigned_reg(), &need_split);
    regHi = any_reg;

    if (reg != any_reg) {
      use_pos = _use_pos[reg];
      split_pos = _block_pos[reg];

      if (_num_phys_regs == 2) {
        if (cur->assigned_reg() != any_reg) {
          regHi = reg;
          reg = cur->assigned_reg();
        } else {
          regHi = find_locked_reg(reg_needed_until, interval_to, reg, &need_split);
          if (regHi != any_reg) {
            use_pos = MIN2(use_pos, _use_pos[regHi]);
            split_pos = MIN2(split_pos, _block_pos[regHi]);
          }
        }

        if (regHi != any_reg && reg > regHi) {
          // sort register numbers to prevent e.g. a move from eax,ebx to ebx,eax
          int temp = reg;
          reg = regHi;
          regHi = temp;
        }
      }
    }
  }

  if (reg == any_reg || (_num_phys_regs == 2 && regHi == any_reg) || use_pos <= cur->first_usage(mustHaveRegister)) {
    // the first use of cur is later than the spilling position -> spill cur
    TRACE_LINEAR_SCAN(4, tty->print_cr("able to spill current interval. first_usage(register): %d, use_pos: %d", cur->first_usage(mustHaveRegister), use_pos));

    if (cur->first_usage(mustHaveRegister) <= cur->from() + 1) {
      assert(false, "cannot spill interval that is used in first instruction (possible reason: no register found)");
      // assign a reasonable register and do a bailout in product mode to avoid errors
      allocator()->assign_spill_slot(cur);
      BAILOUT("LinearScan: no register found");
    }

    split_and_spill_interval(cur);
  } else {
#ifdef ASSERT
    if (TraceLinearScanLevel >= 4) {
      tty->print("decided to use register %d (", reg);
      LinearScan::print_reg_num(reg);
      tty->print("), %d (", regHi);
      LinearScan::print_reg_num(regHi);
      tty->print_cr(")");
    }
#endif
    assert(reg != any_reg && (_num_phys_regs == 1 || regHi != any_reg), "no register found");
    assert(split_pos > 0, "invalid split_pos");
    assert(need_split == false || split_pos > cur->from(), "splitting interval at from");

    cur->assign_reg(reg, regHi);
    if (need_split) {
      // register not available for full interval, so split it
      split_when_partial_register_available(cur, split_pos);
    }

    // perform splitting and spilling for all affected intervalls
    split_and_spill_intersecting_intervals(reg, regHi);
  }
}

bool LinearScanWalker::no_allocation_possible(Interval* cur) {
#ifdef X86
  // fast calculation of intervals that can never get a register because the
  // the next instruction is a call that blocks all registers
  // Note: this does not work if callee-saved registers are available (e.g. on Sparc)

  // check if this interval is the result of a split operation
  // (an interval got a register until this position)
  int pos = cur->from();
  if ((pos & 1) == 1) {
    // the current instruction is a call that blocks all registers
    if (pos < allocator()->max_lir_op_id() && allocator()->has_call(pos + 1)) {
      TRACE_LINEAR_SCAN(4, tty->print_cr("      free register cannot be available because all registers blocked by following call"));

      // safety check that there is really no register available
      assert(alloc_free_reg(cur) == false, "found a register for this interval");
      return true;
    }

  }
#endif
  return false;
}

void LinearScanWalker::init_vars_for_alloc(Interval* cur) {
  BasicType type = cur->type();
  _num_phys_regs = LinearScan::num_physical_regs(type);
  _adjacent_regs = LinearScan::requires_adjacent_regs(type);

  if (pd_init_regs_for_alloc(cur)) {
    // the appropriate register range was selected.
  } else if (type == T_FLOAT || type == T_DOUBLE) {
    _first_reg = pd_first_fpu_reg;
    _last_reg = pd_last_fpu_reg;
  } else {
    _first_reg = pd_first_cpu_reg;
    _last_reg = FrameMap::last_cpu_reg();
  }

  assert(0 <= _first_reg && _first_reg < LinearScan::nof_regs, "out of range");
  assert(0 <= _last_reg && _last_reg < LinearScan::nof_regs, "out of range");
}


bool LinearScanWalker::is_move(LIR_Op* op, Interval* from, Interval* to) {
  if (op->code() != lir_move) {
    return false;
  }
  assert(op->as_Op1() != NULL, "move must be LIR_Op1");

  LIR_Opr in = ((LIR_Op1*)op)->in_opr();
  LIR_Opr res = ((LIR_Op1*)op)->result_opr();
  return in->is_virtual() && res->is_virtual() && in->vreg_number() == from->reg_num() && res->vreg_number() == to->reg_num();
}

// optimization (especially for phi functions of nested loops):
// assign same spill slot to non-intersecting intervals
void LinearScanWalker::combine_spilled_intervals(Interval* cur) {
  if (cur->is_split_child()) {
    // optimization is only suitable for split parents
    return;
  }

  Interval* register_hint = cur->register_hint(false);
  if (register_hint == NULL) {
    // cur is not the target of a move, otherwise register_hint would be set
    return;
  }
  assert(register_hint->is_split_parent(), "register hint must be split parent");

  if (cur->spill_state() != noOptimization || register_hint->spill_state() != noOptimization) {
    // combining the stack slots for intervals where spill move optimization is applied
    // is not benefitial and would cause problems
    return;
  }

  int begin_pos = cur->from();
  int end_pos = cur->to();
  if (end_pos > allocator()->max_lir_op_id() || (begin_pos & 1) != 0 || (end_pos & 1) != 0) {
    // safety check that lir_op_with_id is allowed
    return;
  }

  if (!is_move(allocator()->lir_op_with_id(begin_pos), register_hint, cur) || !is_move(allocator()->lir_op_with_id(end_pos), cur, register_hint)) {
    // cur and register_hint are not connected with two moves
    return;
  }

  Interval* begin_hint = register_hint->split_child_at_op_id(begin_pos, LIR_OpVisitState::inputMode);
  Interval* end_hint = register_hint->split_child_at_op_id(end_pos, LIR_OpVisitState::outputMode);
  if (begin_hint == end_hint || begin_hint->to() != begin_pos || end_hint->from() != end_pos) {
    // register_hint must be split, otherwise the re-writing of use positions does not work
    return;
  }

  assert(begin_hint->assigned_reg() != any_reg, "must have register assigned");
  assert(end_hint->assigned_reg() == any_reg, "must not have register assigned");
  assert(cur->first_usage(mustHaveRegister) == begin_pos, "must have use position at begin of interval because of move");
  assert(end_hint->first_usage(mustHaveRegister) == end_pos, "must have use position at begin of interval because of move");

  if (begin_hint->assigned_reg() < LinearScan::nof_regs) {
    // register_hint is not spilled at begin_pos, so it would not be benefitial to immediately spill cur
    return;
  }
  assert(register_hint->canonical_spill_slot() != -1, "must be set when part of interval was spilled");
  assert(!cur->intersects(register_hint), "cur should not intersect register_hint");

  if (cur->intersects_any_children_of(register_hint)) {
    // Bail out if cur intersects any split children of register_hint, which have the same spill slot as their parent. An overlap of two intervals with
    // the same spill slot could result in a situation where both intervals are spilled at the same time to the same stack location which is not correct.
    return;
  }

  // modify intervals such that cur gets the same stack slot as register_hint
  // delete use positions to prevent the intervals to get a register at beginning
  cur->set_canonical_spill_slot(register_hint->canonical_spill_slot());
  cur->remove_first_use_pos();
  end_hint->remove_first_use_pos();
}


// allocate a physical register or memory location to an interval
bool LinearScanWalker::activate_current() {
  Interval* cur = current();
  bool result = true;

  TRACE_LINEAR_SCAN(2, tty->print   ("+++++ activating interval "); cur->print());
  TRACE_LINEAR_SCAN(4, tty->print_cr("      split_parent: %d, insert_move_when_activated: %d", cur->split_parent()->reg_num(), cur->insert_move_when_activated()));

  if (cur->assigned_reg() >= LinearScan::nof_regs) {
    // activating an interval that has a stack slot assigned -> split it at first use position
    // used for method parameters
    TRACE_LINEAR_SCAN(4, tty->print_cr("      interval has spill slot assigned (method parameter) -> split it before first use"));

    split_stack_interval(cur);
    result = false;

  } else if (allocator()->gen()->is_vreg_flag_set(cur->reg_num(), LIRGenerator::must_start_in_memory)) {
    // activating an interval that must start in a stack slot, but may get a register later
    // used for lir_roundfp: rounding is done by store to stack and reload later
    TRACE_LINEAR_SCAN(4, tty->print_cr("      interval must start in stack slot -> split it before first use"));
    assert(cur->assigned_reg() == any_reg && cur->assigned_regHi() == any_reg, "register already assigned");

    allocator()->assign_spill_slot(cur);
    split_stack_interval(cur);
    result = false;

  } else if (cur->assigned_reg() == any_reg) {
    // interval has not assigned register -> normal allocation
    // (this is the normal case for most intervals)
    TRACE_LINEAR_SCAN(4, tty->print_cr("      normal allocation of register"));

    // assign same spill slot to non-intersecting intervals
    combine_spilled_intervals(cur);

    init_vars_for_alloc(cur);
    if (no_allocation_possible(cur) || !alloc_free_reg(cur)) {
      // no empty register available.
      // split and spill another interval so that this interval gets a register
      alloc_locked_reg(cur);
    }

    // spilled intervals need not be move to active-list
    if (cur->assigned_reg() >= LinearScan::nof_regs) {
      result = false;
    }
  }

  // load spilled values that become active from stack slot to register
  if (cur->insert_move_when_activated()) {
    assert(cur->is_split_child(), "must be");
    assert(cur->current_split_child() != NULL, "must be");
    assert(cur->current_split_child()->reg_num() != cur->reg_num(), "cannot insert move between same interval");
    TRACE_LINEAR_SCAN(4, tty->print_cr("Inserting move from interval %d to %d because insert_move_when_activated is set", cur->current_split_child()->reg_num(), cur->reg_num()));

    insert_move(cur->from(), cur->current_split_child(), cur);
  }
  cur->make_current_split_child();

  return result; // true = interval is moved to active list
}


// Implementation of EdgeMoveOptimizer

EdgeMoveOptimizer::EdgeMoveOptimizer() :
  _edge_instructions(4),
  _edge_instructions_idx(4)
{
}

void EdgeMoveOptimizer::optimize(BlockList* code) {
  EdgeMoveOptimizer optimizer = EdgeMoveOptimizer();

  // ignore the first block in the list (index 0 is not processed)
  for (int i = code->length() - 1; i >= 1; i--) {
    BlockBegin* block = code->at(i);

    if (block->number_of_preds() > 1 && !block->is_set(BlockBegin::exception_entry_flag)) {
      optimizer.optimize_moves_at_block_end(block);
    }
    if (block->number_of_sux() == 2) {
      optimizer.optimize_moves_at_block_begin(block);
    }
  }
}


// clear all internal data structures
void EdgeMoveOptimizer::init_instructions() {
  _edge_instructions.clear();
  _edge_instructions_idx.clear();
}

// append a lir-instruction-list and the index of the current operation in to the list
void EdgeMoveOptimizer::append_instructions(LIR_OpList* instructions, int instructions_idx) {
  _edge_instructions.append(instructions);
  _edge_instructions_idx.append(instructions_idx);
}

// return the current operation of the given edge (predecessor or successor)
LIR_Op* EdgeMoveOptimizer::instruction_at(int edge) {
  LIR_OpList* instructions = _edge_instructions.at(edge);
  int idx = _edge_instructions_idx.at(edge);

  if (idx < instructions->length()) {
    return instructions->at(idx);
  } else {
    return NULL;
  }
}

// removes the current operation of the given edge (predecessor or successor)
void EdgeMoveOptimizer::remove_cur_instruction(int edge, bool decrement_index) {
  LIR_OpList* instructions = _edge_instructions.at(edge);
  int idx = _edge_instructions_idx.at(edge);
  instructions->remove_at(idx);

  if (decrement_index) {
    _edge_instructions_idx.at_put(edge, idx - 1);
  }
}


bool EdgeMoveOptimizer::operations_different(LIR_Op* op1, LIR_Op* op2) {
  if (op1 == NULL || op2 == NULL) {
    // at least one block is already empty -> no optimization possible
    return true;
  }

  if (op1->code() == lir_move && op2->code() == lir_move) {
    assert(op1->as_Op1() != NULL, "move must be LIR_Op1");
    assert(op2->as_Op1() != NULL, "move must be LIR_Op1");
    LIR_Op1* move1 = (LIR_Op1*)op1;
    LIR_Op1* move2 = (LIR_Op1*)op2;
    if (move1->info() == move2->info() && move1->in_opr() == move2->in_opr() && move1->result_opr() == move2->result_opr()) {
      // these moves are exactly equal and can be optimized
      return false;
    }

  } else if (op1->code() == lir_fxch && op2->code() == lir_fxch) {
    assert(op1->as_Op1() != NULL, "fxch must be LIR_Op1");
    assert(op2->as_Op1() != NULL, "fxch must be LIR_Op1");
    LIR_Op1* fxch1 = (LIR_Op1*)op1;
    LIR_Op1* fxch2 = (LIR_Op1*)op2;
    if (fxch1->in_opr()->as_jint() == fxch2->in_opr()->as_jint()) {
      // equal FPU stack operations can be optimized
      return false;
    }

  } else if (op1->code() == lir_fpop_raw && op2->code() == lir_fpop_raw) {
    // equal FPU stack operations can be optimized
    return false;
  }

  // no optimization possible
  return true;
}

void EdgeMoveOptimizer::optimize_moves_at_block_end(BlockBegin* block) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("optimizing moves at end of block B%d", block->block_id()));

  if (block->is_predecessor(block)) {
    // currently we can't handle this correctly.
    return;
  }

  init_instructions();
  int num_preds = block->number_of_preds();
  assert(num_preds > 1, "do not call otherwise");
  assert(!block->is_set(BlockBegin::exception_entry_flag), "exception handlers not allowed");

  // setup a list with the lir-instructions of all predecessors
  int i;
  for (i = 0; i < num_preds; i++) {
    BlockBegin* pred = block->pred_at(i);
    LIR_OpList* pred_instructions = pred->lir()->instructions_list();

    if (pred->number_of_sux() != 1) {
      // this can happen with switch-statements where multiple edges are between
      // the same blocks.
      return;
    }

    assert(pred->number_of_sux() == 1, "can handle only one successor");
    assert(pred->sux_at(0) == block, "invalid control flow");
    assert(pred_instructions->last()->code() == lir_branch, "block with successor must end with branch");
    assert(pred_instructions->last()->as_OpBranch() != NULL, "branch must be LIR_OpBranch");
    assert(pred_instructions->last()->as_OpBranch()->cond() == lir_cond_always, "block must end with unconditional branch");

    if (pred_instructions->last()->info() != NULL) {
      // can not optimize instructions when debug info is needed
      return;
    }

    // ignore the unconditional branch at the end of the block
    append_instructions(pred_instructions, pred_instructions->length() - 2);
  }


  // process lir-instructions while all predecessors end with the same instruction
  while (true) {
    LIR_Op* op = instruction_at(0);
    for (i = 1; i < num_preds; i++) {
      if (operations_different(op, instruction_at(i))) {
        // these instructions are different and cannot be optimized ->
        // no further optimization possible
        return;
      }
    }

    TRACE_LINEAR_SCAN(4, tty->print("found instruction that is equal in all %d predecessors: ", num_preds); op->print());

    // insert the instruction at the beginning of the current block
    block->lir()->insert_before(1, op);

    // delete the instruction at the end of all predecessors
    for (i = 0; i < num_preds; i++) {
      remove_cur_instruction(i, true);
    }
  }
}


void EdgeMoveOptimizer::optimize_moves_at_block_begin(BlockBegin* block) {
  TRACE_LINEAR_SCAN(4, tty->print_cr("optimization moves at begin of block B%d", block->block_id()));

  init_instructions();
  int num_sux = block->number_of_sux();

  LIR_OpList* cur_instructions = block->lir()->instructions_list();

  assert(num_sux == 2, "method should not be called otherwise");
  assert(cur_instructions->last()->code() == lir_branch, "block with successor must end with branch");
  assert(cur_instructions->last()->as_OpBranch() != NULL, "branch must be LIR_OpBranch");
  assert(cur_instructions->last()->as_OpBranch()->cond() == lir_cond_always, "block must end with unconditional branch");

  if (cur_instructions->last()->info() != NULL) {
    // can no optimize instructions when debug info is needed
    return;
  }

  LIR_Op* branch = cur_instructions->at(cur_instructions->length() - 2);
  if (branch->info() != NULL || (branch->code() != lir_branch && branch->code() != lir_cond_float_branch)) {
    // not a valid case for optimization
    // currently, only blocks that end with two branches (conditional branch followed
    // by unconditional branch) are optimized
    return;
  }

  // now it is guaranteed that the block ends with two branch instructions.
  // the instructions are inserted at the end of the block before these two branches
  int insert_idx = cur_instructions->length() - 2;

  int i;
#ifdef ASSERT
  for (i = insert_idx - 1; i >= 0; i--) {
    LIR_Op* op = cur_instructions->at(i);
    if ((op->code() == lir_branch || op->code() == lir_cond_float_branch) && ((LIR_OpBranch*)op)->block() != NULL) {
      assert(false, "block with two successors can have only two branch instructions");
    }
  }
#endif

  // setup a list with the lir-instructions of all successors
  for (i = 0; i < num_sux; i++) {
    BlockBegin* sux = block->sux_at(i);
    LIR_OpList* sux_instructions = sux->lir()->instructions_list();

    assert(sux_instructions->at(0)->code() == lir_label, "block must start with label");

    if (sux->number_of_preds() != 1) {
      // this can happen with switch-statements where multiple edges are between
      // the same blocks.
      return;
    }
    assert(sux->pred_at(0) == block, "invalid control flow");
    assert(!sux->is_set(BlockBegin::exception_entry_flag), "exception handlers not allowed");

    // ignore the label at the beginning of the block
    append_instructions(sux_instructions, 1);
  }

  // process lir-instructions while all successors begin with the same instruction
  while (true) {
    LIR_Op* op = instruction_at(0);
    for (i = 1; i < num_sux; i++) {
      if (operations_different(op, instruction_at(i))) {
        // these instructions are different and cannot be optimized ->
        // no further optimization possible
        return;
      }
    }

    TRACE_LINEAR_SCAN(4, tty->print("----- found instruction that is equal in all %d successors: ", num_sux); op->print());

    // insert instruction at end of current block
    block->lir()->insert_before(insert_idx, op);
    insert_idx++;

    // delete the instructions at the beginning of all successors
    for (i = 0; i < num_sux; i++) {
      remove_cur_instruction(i, false);
    }
  }
}


// Implementation of ControlFlowOptimizer

ControlFlowOptimizer::ControlFlowOptimizer() :
  _original_preds(4)
{
}

void ControlFlowOptimizer::optimize(BlockList* code) {
  ControlFlowOptimizer optimizer = ControlFlowOptimizer();

  // push the OSR entry block to the end so that we're not jumping over it.
  BlockBegin* osr_entry = code->at(0)->end()->as_Base()->osr_entry();
  if (osr_entry) {
    int index = osr_entry->linear_scan_number();
    assert(code->at(index) == osr_entry, "wrong index");
    code->remove_at(index);
    code->append(osr_entry);
  }

  optimizer.reorder_short_loops(code);
  optimizer.delete_empty_blocks(code);
  optimizer.delete_unnecessary_jumps(code);
  optimizer.delete_jumps_to_return(code);
}

void ControlFlowOptimizer::reorder_short_loop(BlockList* code, BlockBegin* header_block, int header_idx) {
  int i = header_idx + 1;
  int max_end = MIN2(header_idx + ShortLoopSize, code->length());
  while (i < max_end && code->at(i)->loop_depth() >= header_block->loop_depth()) {
    i++;
  }

  if (i == code->length() || code->at(i)->loop_depth() < header_block->loop_depth()) {
    int end_idx = i - 1;
    BlockBegin* end_block = code->at(end_idx);

    if (end_block->number_of_sux() == 1 && end_block->sux_at(0) == header_block) {
      // short loop from header_idx to end_idx found -> reorder blocks such that
      // the header_block is the last block instead of the first block of the loop
      TRACE_LINEAR_SCAN(1, tty->print_cr("Reordering short loop: length %d, header B%d, end B%d",
                                         end_idx - header_idx + 1,
                                         header_block->block_id(), end_block->block_id()));

      for (int j = header_idx; j < end_idx; j++) {
        code->at_put(j, code->at(j + 1));
      }
      code->at_put(end_idx, header_block);

      // correct the flags so that any loop alignment occurs in the right place.
      assert(code->at(end_idx)->is_set(BlockBegin::backward_branch_target_flag), "must be backward branch target");
      code->at(end_idx)->clear(BlockBegin::backward_branch_target_flag);
      code->at(header_idx)->set(BlockBegin::backward_branch_target_flag);
    }
  }
}

void ControlFlowOptimizer::reorder_short_loops(BlockList* code) {
  for (int i = code->length() - 1; i >= 0; i--) {
    BlockBegin* block = code->at(i);

    if (block->is_set(BlockBegin::linear_scan_loop_header_flag)) {
      reorder_short_loop(code, block, i);
    }
  }

  DEBUG_ONLY(verify(code));
}

// only blocks with exactly one successor can be deleted. Such blocks
// must always end with an unconditional branch to this successor
bool ControlFlowOptimizer::can_delete_block(BlockBegin* block) {
  if (block->number_of_sux() != 1 || block->number_of_exception_handlers() != 0 || block->is_entry_block()) {
    return false;
  }

  LIR_OpList* instructions = block->lir()->instructions_list();

  assert(instructions->length() >= 2, "block must have label and branch");
  assert(instructions->at(0)->code() == lir_label, "first instruction must always be a label");
  assert(instructions->last()->as_OpBranch() != NULL, "last instrcution must always be a branch");
  assert(instructions->last()->as_OpBranch()->cond() == lir_cond_always, "branch must be unconditional");
  assert(instructions->last()->as_OpBranch()->block() == block->sux_at(0), "branch target must be the successor");

  // block must have exactly one successor

  if (instructions->length() == 2 && instructions->last()->info() == NULL) {
    return true;
  }
  return false;
}

// substitute branch targets in all branch-instructions of this blocks
void ControlFlowOptimizer::substitute_branch_target(BlockBegin* block, BlockBegin* target_from, BlockBegin* target_to) {
  TRACE_LINEAR_SCAN(3, tty->print_cr("Deleting empty block: substituting from B%d to B%d inside B%d", target_from->block_id(), target_to->block_id(), block->block_id()));

  LIR_OpList* instructions = block->lir()->instructions_list();

  assert(instructions->at(0)->code() == lir_label, "first instruction must always be a label");
  for (int i = instructions->length() - 1; i >= 1; i--) {
    LIR_Op* op = instructions->at(i);

    if (op->code() == lir_branch || op->code() == lir_cond_float_branch) {
      assert(op->as_OpBranch() != NULL, "branch must be of type LIR_OpBranch");
      LIR_OpBranch* branch = (LIR_OpBranch*)op;

      if (branch->block() == target_from) {
        branch->change_block(target_to);
      }
      if (branch->ublock() == target_from) {
        branch->change_ublock(target_to);
      }
    }
  }
}

void ControlFlowOptimizer::delete_empty_blocks(BlockList* code) {
  int old_pos = 0;
  int new_pos = 0;
  int num_blocks = code->length();

  while (old_pos < num_blocks) {
    BlockBegin* block = code->at(old_pos);

    if (can_delete_block(block)) {
      BlockBegin* new_target = block->sux_at(0);

      // propagate backward branch target flag for correct code alignment
      if (block->is_set(BlockBegin::backward_branch_target_flag)) {
        new_target->set(BlockBegin::backward_branch_target_flag);
      }

      // collect a list with all predecessors that contains each predecessor only once
      // the predecessors of cur are changed during the substitution, so a copy of the
      // predecessor list is necessary
      int j;
      _original_preds.clear();
      for (j = block->number_of_preds() - 1; j >= 0; j--) {
        BlockBegin* pred = block->pred_at(j);
        if (_original_preds.find(pred) == -1) {
          _original_preds.append(pred);
        }
      }

      for (j = _original_preds.length() - 1; j >= 0; j--) {
        BlockBegin* pred = _original_preds.at(j);
        substitute_branch_target(pred, block, new_target);
        pred->substitute_sux(block, new_target);
      }
    } else {
      // adjust position of this block in the block list if blocks before
      // have been deleted
      if (new_pos != old_pos) {
        code->at_put(new_pos, code->at(old_pos));
      }
      new_pos++;
    }
    old_pos++;
  }
  code->trunc_to(new_pos);

  DEBUG_ONLY(verify(code));
}

void ControlFlowOptimizer::delete_unnecessary_jumps(BlockList* code) {
  // skip the last block because there a branch is always necessary
  for (int i = code->length() - 2; i >= 0; i--) {
    BlockBegin* block = code->at(i);
    LIR_OpList* instructions = block->lir()->instructions_list();

    LIR_Op* last_op = instructions->last();
    if (last_op->code() == lir_branch) {
      assert(last_op->as_OpBranch() != NULL, "branch must be of type LIR_OpBranch");
      LIR_OpBranch* last_branch = (LIR_OpBranch*)last_op;

      assert(last_branch->block() != NULL, "last branch must always have a block as target");
      assert(last_branch->label() == last_branch->block()->label(), "must be equal");

      if (last_branch->info() == NULL) {
        if (last_branch->block() == code->at(i + 1)) {

          TRACE_LINEAR_SCAN(3, tty->print_cr("Deleting unconditional branch at end of block B%d", block->block_id()));

          // delete last branch instruction
          instructions->trunc_to(instructions->length() - 1);

        } else {
          LIR_Op* prev_op = instructions->at(instructions->length() - 2);
          if (prev_op->code() == lir_branch || prev_op->code() == lir_cond_float_branch) {
            assert(prev_op->as_OpBranch() != NULL, "branch must be of type LIR_OpBranch");
            LIR_OpBranch* prev_branch = (LIR_OpBranch*)prev_op;

            if (prev_branch->stub() == NULL) {

              LIR_Op2* prev_cmp = NULL;
              // There might be a cmove inserted for profiling which depends on the same
              // compare. If we change the condition of the respective compare, we have
              // to take care of this cmove as well.
              LIR_Op2* prev_cmove = NULL;

              for(int j = instructions->length() - 3; j >= 0 && prev_cmp == NULL; j--) {
                prev_op = instructions->at(j);
                // check for the cmove
                if (prev_op->code() == lir_cmove) {
                  assert(prev_op->as_Op2() != NULL, "cmove must be of type LIR_Op2");
                  prev_cmove = (LIR_Op2*)prev_op;
                  assert(prev_branch->cond() == prev_cmove->condition(), "should be the same");
                }
                if (prev_op->code() == lir_cmp) {
                  assert(prev_op->as_Op2() != NULL, "branch must be of type LIR_Op2");
                  prev_cmp = (LIR_Op2*)prev_op;
                  assert(prev_branch->cond() == prev_cmp->condition(), "should be the same");
                }
              }
              // Guarantee because it is dereferenced below.
              guarantee(prev_cmp != NULL, "should have found comp instruction for branch");
              if (prev_branch->block() == code->at(i + 1) && prev_branch->info() == NULL) {

                TRACE_LINEAR_SCAN(3, tty->print_cr("Negating conditional branch and deleting unconditional branch at end of block B%d", block->block_id()));

                // eliminate a conditional branch to the immediate successor
                prev_branch->change_block(last_branch->block());
                prev_branch->negate_cond();
                prev_cmp->set_condition(prev_branch->cond());
                instructions->trunc_to(instructions->length() - 1);
                // if we do change the condition, we have to change the cmove as well
                if (prev_cmove != NULL) {
                  prev_cmove->set_condition(prev_branch->cond());
                  LIR_Opr t = prev_cmove->in_opr1();
                  prev_cmove->set_in_opr1(prev_cmove->in_opr2());
                  prev_cmove->set_in_opr2(t);
                }
              }
            }
          }
        }
      }
    }
  }

  DEBUG_ONLY(verify(code));
}

void ControlFlowOptimizer::delete_jumps_to_return(BlockList* code) {
#ifdef ASSERT
  ResourceBitMap return_converted(BlockBegin::number_of_blocks());
#endif

  for (int i = code->length() - 1; i >= 0; i--) {
    BlockBegin* block = code->at(i);
    LIR_OpList* cur_instructions = block->lir()->instructions_list();
    LIR_Op*     cur_last_op = cur_instructions->last();

    assert(cur_instructions->at(0)->code() == lir_label, "first instruction must always be a label");
    if (cur_instructions->length() == 2 && cur_last_op->code() == lir_return) {
      // the block contains only a label and a return
      // if a predecessor ends with an unconditional jump to this block, then the jump
      // can be replaced with a return instruction
      //
      // Note: the original block with only a return statement cannot be deleted completely
      //       because the predecessors might have other (conditional) jumps to this block
      //       -> this may lead to unnecesary return instructions in the final code

      assert(cur_last_op->info() == NULL, "return instructions do not have debug information");
      assert(block->number_of_sux() == 0 ||
             (return_converted.at(block->block_id()) && block->number_of_sux() == 1),
             "blocks that end with return must not have successors");

      assert(cur_last_op->as_Op1() != NULL, "return must be LIR_Op1");
      LIR_Opr return_opr = ((LIR_Op1*)cur_last_op)->in_opr();

      for (int j = block->number_of_preds() - 1; j >= 0; j--) {
        BlockBegin* pred = block->pred_at(j);
        LIR_OpList* pred_instructions = pred->lir()->instructions_list();
        LIR_Op*     pred_last_op = pred_instructions->last();

        if (pred_last_op->code() == lir_branch) {
          assert(pred_last_op->as_OpBranch() != NULL, "branch must be LIR_OpBranch");
          LIR_OpBranch* pred_last_branch = (LIR_OpBranch*)pred_last_op;

          if (pred_last_branch->block() == block && pred_last_branch->cond() == lir_cond_always && pred_last_branch->info() == NULL) {
            // replace the jump to a return with a direct return
            // Note: currently the edge between the blocks is not deleted
            pred_instructions->at_put(pred_instructions->length() - 1, new LIR_OpReturn(return_opr));
#ifdef ASSERT
            return_converted.set_bit(pred->block_id());
#endif
          }
        }
      }
    }
  }
}


#ifdef ASSERT
void ControlFlowOptimizer::verify(BlockList* code) {
  for (int i = 0; i < code->length(); i++) {
    BlockBegin* block = code->at(i);
    LIR_OpList* instructions = block->lir()->instructions_list();

    int j;
    for (j = 0; j < instructions->length(); j++) {
      LIR_OpBranch* op_branch = instructions->at(j)->as_OpBranch();

      if (op_branch != NULL) {
        assert(op_branch->block() == NULL || code->find(op_branch->block()) != -1, "branch target not valid");
        assert(op_branch->ublock() == NULL || code->find(op_branch->ublock()) != -1, "branch target not valid");
      }
    }

    for (j = 0; j < block->number_of_sux() - 1; j++) {
      BlockBegin* sux = block->sux_at(j);
      assert(code->find(sux) != -1, "successor not valid");
    }

    for (j = 0; j < block->number_of_preds() - 1; j++) {
      BlockBegin* pred = block->pred_at(j);
      assert(code->find(pred) != -1, "successor not valid");
    }
  }
}
#endif


#ifndef PRODUCT

// Implementation of LinearStatistic

const char* LinearScanStatistic::counter_name(int counter_idx) {
  switch (counter_idx) {
    case counter_method:          return "compiled methods";
    case counter_fpu_method:      return "methods using fpu";
    case counter_loop_method:     return "methods with loops";
    case counter_exception_method:return "methods with xhandler";

    case counter_loop:            return "loops";
    case counter_block:           return "blocks";
    case counter_loop_block:      return "blocks inside loop";
    case counter_exception_block: return "exception handler entries";
    case counter_interval:        return "intervals";
    case counter_fixed_interval:  return "fixed intervals";
    case counter_range:           return "ranges";
    case counter_fixed_range:     return "fixed ranges";
    case counter_use_pos:         return "use positions";
    case counter_fixed_use_pos:   return "fixed use positions";
    case counter_spill_slots:     return "spill slots";

    // counter for classes of lir instructions
    case counter_instruction:     return "total instructions";
    case counter_label:           return "labels";
    case counter_entry:           return "method entries";
    case counter_return:          return "method returns";
    case counter_call:            return "method calls";
    case counter_move:            return "moves";
    case counter_cmp:             return "compare";
    case counter_cond_branch:     return "conditional branches";
    case counter_uncond_branch:   return "unconditional branches";
    case counter_stub_branch:     return "branches to stub";
    case counter_alu:             return "artithmetic + logic";
    case counter_alloc:           return "allocations";
    case counter_sync:            return "synchronisation";
    case counter_throw:           return "throw";
    case counter_unwind:          return "unwind";
    case counter_typecheck:       return "type+null-checks";
    case counter_fpu_stack:       return "fpu-stack";
    case counter_misc_inst:       return "other instructions";
    case counter_other_inst:      return "misc. instructions";

    // counter for different types of moves
    case counter_move_total:      return "total moves";
    case counter_move_reg_reg:    return "register->register";
    case counter_move_reg_stack:  return "register->stack";
    case counter_move_stack_reg:  return "stack->register";
    case counter_move_stack_stack:return "stack->stack";
    case counter_move_reg_mem:    return "register->memory";
    case counter_move_mem_reg:    return "memory->register";
    case counter_move_const_any:  return "constant->any";

    case blank_line_1:            return "";
    case blank_line_2:            return "";

    default: ShouldNotReachHere(); return "";
  }
}

LinearScanStatistic::Counter LinearScanStatistic::base_counter(int counter_idx) {
  if (counter_idx == counter_fpu_method || counter_idx == counter_loop_method || counter_idx == counter_exception_method) {
    return counter_method;
  } else if (counter_idx == counter_loop_block || counter_idx == counter_exception_block) {
    return counter_block;
  } else if (counter_idx >= counter_instruction && counter_idx <= counter_other_inst) {
    return counter_instruction;
  } else if (counter_idx >= counter_move_total && counter_idx <= counter_move_const_any) {
    return counter_move_total;
  }
  return invalid_counter;
}

LinearScanStatistic::LinearScanStatistic() {
  for (int i = 0; i < number_of_counters; i++) {
    _counters_sum[i] = 0;
    _counters_max[i] = -1;
  }

}

// add the method-local numbers to the total sum
void LinearScanStatistic::sum_up(LinearScanStatistic &method_statistic) {
  for (int i = 0; i < number_of_counters; i++) {
    _counters_sum[i] += method_statistic._counters_sum[i];
    _counters_max[i] = MAX2(_counters_max[i], method_statistic._counters_sum[i]);
  }
}

void LinearScanStatistic::print(const char* title) {
  if (CountLinearScan || TraceLinearScanLevel > 0) {
    tty->cr();
    tty->print_cr("***** LinearScan statistic - %s *****", title);

    for (int i = 0; i < number_of_counters; i++) {
      if (_counters_sum[i] > 0 || _counters_max[i] >= 0) {
        tty->print("%25s: %8d", counter_name(i), _counters_sum[i]);

        LinearScanStatistic::Counter cntr = base_counter(i);
        if (cntr != invalid_counter) {
          tty->print("  (%5.1f%%) ", _counters_sum[i] * 100.0 / _counters_sum[cntr]);
        } else {
          tty->print("           ");
        }

        if (_counters_max[i] >= 0) {
          tty->print("%8d", _counters_max[i]);
        }
      }
      tty->cr();
    }
  }
}

void LinearScanStatistic::collect(LinearScan* allocator) {
  inc_counter(counter_method);
  if (allocator->has_fpu_registers()) {
    inc_counter(counter_fpu_method);
  }
  if (allocator->num_loops() > 0) {
    inc_counter(counter_loop_method);
  }
  inc_counter(counter_loop, allocator->num_loops());
  inc_counter(counter_spill_slots, allocator->max_spills());

  int i;
  for (i = 0; i < allocator->interval_count(); i++) {
    Interval* cur = allocator->interval_at(i);

    if (cur != NULL) {
      inc_counter(counter_interval);
      inc_counter(counter_use_pos, cur->num_use_positions());
      if (LinearScan::is_precolored_interval(cur)) {
        inc_counter(counter_fixed_interval);
        inc_counter(counter_fixed_use_pos, cur->num_use_positions());
      }

      Range* range = cur->first();
      while (range != Range::end()) {
        inc_counter(counter_range);
        if (LinearScan::is_precolored_interval(cur)) {
          inc_counter(counter_fixed_range);
        }
        range = range->next();
      }
    }
  }

  bool has_xhandlers = false;
  // Note: only count blocks that are in code-emit order
  for (i = 0; i < allocator->ir()->code()->length(); i++) {
    BlockBegin* cur = allocator->ir()->code()->at(i);

    inc_counter(counter_block);
    if (cur->loop_depth() > 0) {
      inc_counter(counter_loop_block);
    }
    if (cur->is_set(BlockBegin::exception_entry_flag)) {
      inc_counter(counter_exception_block);
      has_xhandlers = true;
    }

    LIR_OpList* instructions = cur->lir()->instructions_list();
    for (int j = 0; j < instructions->length(); j++) {
      LIR_Op* op = instructions->at(j);

      inc_counter(counter_instruction);

      switch (op->code()) {
        case lir_label:           inc_counter(counter_label); break;
        case lir_std_entry:
        case lir_osr_entry:       inc_counter(counter_entry); break;
        case lir_return:          inc_counter(counter_return); break;

        case lir_rtcall:
        case lir_static_call:
        case lir_optvirtual_call: inc_counter(counter_call); break;

        case lir_move: {
          inc_counter(counter_move);
          inc_counter(counter_move_total);

          LIR_Opr in = op->as_Op1()->in_opr();
          LIR_Opr res = op->as_Op1()->result_opr();
          if (in->is_register()) {
            if (res->is_register()) {
              inc_counter(counter_move_reg_reg);
            } else if (res->is_stack()) {
              inc_counter(counter_move_reg_stack);
            } else if (res->is_address()) {
              inc_counter(counter_move_reg_mem);
            } else {
              ShouldNotReachHere();
            }
          } else if (in->is_stack()) {
            if (res->is_register()) {
              inc_counter(counter_move_stack_reg);
            } else {
              inc_counter(counter_move_stack_stack);
            }
          } else if (in->is_address()) {
            assert(res->is_register(), "must be");
            inc_counter(counter_move_mem_reg);
          } else if (in->is_constant()) {
            inc_counter(counter_move_const_any);
          } else {
            ShouldNotReachHere();
          }
          break;
        }

        case lir_cmp:             inc_counter(counter_cmp); break;

        case lir_branch:
        case lir_cond_float_branch: {
          LIR_OpBranch* branch = op->as_OpBranch();
          if (branch->block() == NULL) {
            inc_counter(counter_stub_branch);
          } else if (branch->cond() == lir_cond_always) {
            inc_counter(counter_uncond_branch);
          } else {
            inc_counter(counter_cond_branch);
          }
          break;
        }

        case lir_neg:
        case lir_add:
        case lir_sub:
        case lir_mul:
        case lir_div:
        case lir_rem:
        case lir_sqrt:
        case lir_abs:
        case lir_log10:
        case lir_logic_and:
        case lir_logic_or:
        case lir_logic_xor:
        case lir_shl:
        case lir_shr:
        case lir_ushr:            inc_counter(counter_alu); break;

        case lir_alloc_object:
        case lir_alloc_array:     inc_counter(counter_alloc); break;

        case lir_monaddr:
        case lir_lock:
        case lir_unlock:          inc_counter(counter_sync); break;

        case lir_throw:           inc_counter(counter_throw); break;

        case lir_unwind:          inc_counter(counter_unwind); break;

        case lir_null_check:
        case lir_leal:
        case lir_instanceof:
        case lir_checkcast:
        case lir_store_check:     inc_counter(counter_typecheck); break;

        case lir_fpop_raw:
        case lir_fxch:
        case lir_fld:             inc_counter(counter_fpu_stack); break;

        case lir_nop:
        case lir_push:
        case lir_pop:
        case lir_convert:
        case lir_roundfp:
        case lir_cmove:           inc_counter(counter_misc_inst); break;

        default:                  inc_counter(counter_other_inst); break;
      }
    }
  }

  if (has_xhandlers) {
    inc_counter(counter_exception_method);
  }
}

void LinearScanStatistic::compute(LinearScan* allocator, LinearScanStatistic &global_statistic) {
  if (CountLinearScan || TraceLinearScanLevel > 0) {

    LinearScanStatistic local_statistic = LinearScanStatistic();

    local_statistic.collect(allocator);
    global_statistic.sum_up(local_statistic);

    if (TraceLinearScanLevel > 2) {
      local_statistic.print("current local statistic");
    }
  }
}


// Implementation of LinearTimers

LinearScanTimers::LinearScanTimers() {
  for (int i = 0; i < number_of_timers; i++) {
    timer(i)->reset();
  }
}

const char* LinearScanTimers::timer_name(int idx) {
  switch (idx) {
    case timer_do_nothing:               return "Nothing (Time Check)";
    case timer_number_instructions:      return "Number Instructions";
    case timer_compute_local_live_sets:  return "Local Live Sets";
    case timer_compute_global_live_sets: return "Global Live Sets";
    case timer_build_intervals:          return "Build Intervals";
    case timer_sort_intervals_before:    return "Sort Intervals Before";
    case timer_allocate_registers:       return "Allocate Registers";
    case timer_resolve_data_flow:        return "Resolve Data Flow";
    case timer_sort_intervals_after:     return "Sort Intervals After";
    case timer_eliminate_spill_moves:    return "Spill optimization";
    case timer_assign_reg_num:           return "Assign Reg Num";
    case timer_allocate_fpu_stack:       return "Allocate FPU Stack";
    case timer_optimize_lir:             return "Optimize LIR";
    default: ShouldNotReachHere();       return "";
  }
}

void LinearScanTimers::begin_method() {
  if (TimeEachLinearScan) {
    // reset all timers to measure only current method
    for (int i = 0; i < number_of_timers; i++) {
      timer(i)->reset();
    }
  }
}

void LinearScanTimers::end_method(LinearScan* allocator) {
  if (TimeEachLinearScan) {

    double c = timer(timer_do_nothing)->seconds();
    double total = 0;
    for (int i = 1; i < number_of_timers; i++) {
      total += timer(i)->seconds() - c;
    }

    if (total >= 0.0005) {
      // print all information in one line for automatic processing
      tty->print("@"); allocator->compilation()->method()->print_name();

      tty->print("@ %d ", allocator->compilation()->method()->code_size());
      tty->print("@ %d ", allocator->block_at(allocator->block_count() - 1)->last_lir_instruction_id() / 2);
      tty->print("@ %d ", allocator->block_count());
      tty->print("@ %d ", allocator->num_virtual_regs());
      tty->print("@ %d ", allocator->interval_count());
      tty->print("@ %d ", allocator->_num_calls);
      tty->print("@ %d ", allocator->num_loops());

      tty->print("@ %6.6f ", total);
      for (int i = 1; i < number_of_timers; i++) {
        tty->print("@ %4.1f ", ((timer(i)->seconds() - c) / total) * 100);
      }
      tty->cr();
    }
  }
}

void LinearScanTimers::print(double total_time) {
  if (TimeLinearScan) {
    // correction value: sum of dummy-timer that only measures the time that
    // is necesary to start and stop itself
    double c = timer(timer_do_nothing)->seconds();

    for (int i = 0; i < number_of_timers; i++) {
      double t = timer(i)->seconds();
      tty->print_cr("    %25s: %6.3f s (%4.1f%%)  corrected: %6.3f s (%4.1f%%)", timer_name(i), t, (t / total_time) * 100.0, t - c, (t - c) / (total_time - 2 * number_of_timers * c) * 100);
    }
  }
}

#endif // #ifndef PRODUCT
