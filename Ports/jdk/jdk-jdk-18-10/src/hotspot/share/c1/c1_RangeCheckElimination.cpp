/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_ValueStack.hpp"
#include "c1/c1_RangeCheckElimination.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_Canonicalizer.hpp"
#include "c1/c1_ValueMap.hpp"
#include "ci/ciMethodData.hpp"
#include "runtime/deoptimization.hpp"
#ifdef ASSERT
#include "utilities/bitMap.inline.hpp"
#endif

// Macros for the Trace and the Assertion flag
#ifdef ASSERT
#define TRACE_RANGE_CHECK_ELIMINATION(code) if (TraceRangeCheckElimination) { code; }
#define ASSERT_RANGE_CHECK_ELIMINATION(code) if (AssertRangeCheckElimination) { code; }
#define TRACE_OR_ASSERT_RANGE_CHECK_ELIMINATION(code) if (TraceRangeCheckElimination || AssertRangeCheckElimination) { code; }
#else
#define TRACE_RANGE_CHECK_ELIMINATION(code)
#define ASSERT_RANGE_CHECK_ELIMINATION(code)
#define TRACE_OR_ASSERT_RANGE_CHECK_ELIMINATION(code)
#endif

// Entry point for the optimization
void RangeCheckElimination::eliminate(IR *ir) {
  bool do_elimination = ir->compilation()->has_access_indexed();
  ASSERT_RANGE_CHECK_ELIMINATION(do_elimination = true);
  if (do_elimination) {
    RangeCheckEliminator rce(ir);
  }
}

// Constructor
RangeCheckEliminator::RangeCheckEliminator(IR *ir) :
  _bounds(Instruction::number_of_instructions(), Instruction::number_of_instructions(), NULL),
  _access_indexed_info(Instruction::number_of_instructions(), Instruction::number_of_instructions(), NULL)
{
  _visitor.set_range_check_eliminator(this);
  _ir = ir;
  _number_of_instructions = Instruction::number_of_instructions();
  _optimistic = ir->compilation()->is_optimistic();

  TRACE_RANGE_CHECK_ELIMINATION(
    tty->cr();
    tty->print_cr("Range check elimination");
    ir->method()->print_name(tty);
    tty->cr();
  );

  TRACE_RANGE_CHECK_ELIMINATION(
    tty->print_cr("optimistic=%d", (int)_optimistic);
  );

#ifdef ASSERT
  // Verifies several conditions that must be true on the IR-input. Only used for debugging purposes.
  TRACE_RANGE_CHECK_ELIMINATION(
    tty->print_cr("Verification of IR . . .");
  );
  Verification verification(ir);
#endif

  // Set process block flags
  // Optimization so a blocks is only processed if it contains an access indexed instruction or if
  // one of its children in the dominator tree contains an access indexed instruction.
  set_process_block_flags(ir->start());

  // Pass over instructions in the dominator tree
  TRACE_RANGE_CHECK_ELIMINATION(
    tty->print_cr("Starting pass over dominator tree . . .")
  );
  calc_bounds(ir->start(), NULL);

  TRACE_RANGE_CHECK_ELIMINATION(
    tty->print_cr("Finished!")
  );
}

// Instruction specific work for some instructions
// Constant
void RangeCheckEliminator::Visitor::do_Constant(Constant *c) {
  IntConstant *ic = c->type()->as_IntConstant();
  if (ic != NULL) {
    int value = ic->value();
    _bound = new Bound(value, NULL, value, NULL);
  }
}

// LogicOp
void RangeCheckEliminator::Visitor::do_LogicOp(LogicOp *lo) {
  if (lo->type()->as_IntType() && lo->op() == Bytecodes::_iand && (lo->x()->as_Constant() || lo->y()->as_Constant())) {
    int constant = 0;
    Constant *c = lo->x()->as_Constant();
    if (c != NULL) {
      constant = c->type()->as_IntConstant()->value();
    } else {
      constant = lo->y()->as_Constant()->type()->as_IntConstant()->value();
    }
    if (constant >= 0) {
      _bound = new Bound(0, NULL, constant, NULL);
    }
  }
}

// Phi
void RangeCheckEliminator::Visitor::do_Phi(Phi *phi) {
  if (!phi->type()->as_IntType() && !phi->type()->as_ObjectType()) return;

  BlockBegin *block = phi->block();
  int op_count = phi->operand_count();
  bool has_upper = true;
  bool has_lower = true;
  assert(phi, "Phi must not be null");
  Bound *bound = NULL;

  // TODO: support more difficult phis
  for (int i=0; i<op_count; i++) {
    Value v = phi->operand_at(i);

    if (v == phi) continue;

    // Check if instruction is connected with phi itself
    Op2 *op2 = v->as_Op2();
    if (op2 != NULL) {
      Value x = op2->x();
      Value y = op2->y();
      if ((x == phi || y == phi)) {
        Value other = x;
        if (other == phi) {
          other = y;
        }
        ArithmeticOp *ao = v->as_ArithmeticOp();
        if (ao != NULL && ao->op() == Bytecodes::_iadd) {
          assert(ao->op() == Bytecodes::_iadd, "Has to be add!");
          if (ao->type()->as_IntType()) {
            Constant *c = other->as_Constant();
            if (c != NULL) {
              assert(c->type()->as_IntConstant(), "Constant has to be of type integer");
              int value = c->type()->as_IntConstant()->value();
              if (value == 1) {
                has_upper = false;
              } else if (value > 1) {
                // Overflow not guaranteed
                has_upper = false;
                has_lower = false;
              } else if (value < 0) {
                has_lower = false;
              }
              continue;
            }
          }
        }
      }
    }

    // No connection -> new bound
    Bound *v_bound = _rce->get_bound(v);
    Bound *cur_bound;
    int cur_constant = 0;
    Value cur_value = v;

    if (v->type()->as_IntConstant()) {
      cur_constant = v->type()->as_IntConstant()->value();
      cur_value = NULL;
    }
    if (!v_bound->has_upper() || !v_bound->has_lower()) {
      cur_bound = new Bound(cur_constant, cur_value, cur_constant, cur_value);
    } else {
      cur_bound = v_bound;
    }
    if (cur_bound) {
      if (!bound) {
        bound = cur_bound->copy();
      } else {
        bound->or_op(cur_bound);
      }
    } else {
      // No bound!
      bound = NULL;
      break;
    }
  }

  if (bound) {
    if (!has_upper) {
      bound->remove_upper();
    }
    if (!has_lower) {
      bound->remove_lower();
    }
    _bound = bound;
  } else {
    _bound = new Bound();
  }
}


// ArithmeticOp
void RangeCheckEliminator::Visitor::do_ArithmeticOp(ArithmeticOp *ao) {
  Value x = ao->x();
  Value y = ao->y();

  if (ao->op() == Bytecodes::_irem) {
    Bound* x_bound = _rce->get_bound(x);
    Bound* y_bound = _rce->get_bound(y);
    if (x_bound->lower() >= 0 && x_bound->lower_instr() == NULL && y->as_ArrayLength() != NULL) {
      _bound = new Bound(0, NULL, -1, y);
    } else if (y->type()->as_IntConstant() && y->type()->as_IntConstant()->value() != 0) {
      // The binary % operator is said to yield the remainder of its operands from an implied division; the
      // left-hand operand is the dividend and the right-hand operand is the divisor.
      //
      // % operator follows from this rule that the result of the remainder operation can be negative only
      // if the dividend is negative, and can be positive only if the dividend is positive. Moreover, the
      // magnitude of the result is always less than the magnitude of the divisor(See JLS 15.17.3).
      //
      // So if y is a constant integer and not equal to 0, then we can deduce the bound of remainder operation:
      // x % -y  ==> [0, y - 1] Apply RCE
      // x % y   ==> [0, y - 1] Apply RCE
      // -x % y  ==> [-y + 1, 0]
      // -x % -y ==> [-y + 1, 0]
      if (x_bound->has_lower() && x_bound->lower() >= 0) {
        _bound = new Bound(0, NULL, y->type()->as_IntConstant()->value() - 1, NULL);
      } else {
        _bound = new Bound();
      }
    } else {
      _bound = new Bound();
    }
  } else if (!x->as_Constant() || !y->as_Constant()) {
    assert(!x->as_Constant() || !y->as_Constant(), "One of the operands must be non-constant!");
    if (((x->as_Constant() || y->as_Constant()) && (ao->op() == Bytecodes::_iadd)) || (y->as_Constant() && ao->op() == Bytecodes::_isub)) {
      assert(ao->op() == Bytecodes::_iadd || ao->op() == Bytecodes::_isub, "Operand must be iadd or isub");

      if (y->as_Constant()) {
        Value tmp = x;
        x = y;
        y = tmp;
      }
      assert(x->as_Constant()->type()->as_IntConstant(), "Constant must be int constant!");

      // Constant now in x
      int const_value = x->as_Constant()->type()->as_IntConstant()->value();
      if (ao->op() == Bytecodes::_iadd || const_value != min_jint) {
        if (ao->op() == Bytecodes::_isub) {
          const_value = -const_value;
        }

        Bound * bound = _rce->get_bound(y);
        if (bound->has_upper() && bound->has_lower()) {
          int new_lower = bound->lower() + const_value;
          jlong new_lowerl = ((jlong)bound->lower()) + const_value;
          int new_upper = bound->upper() + const_value;
          jlong new_upperl = ((jlong)bound->upper()) + const_value;

          if (((jlong)new_lower) == new_lowerl && ((jlong)new_upper == new_upperl)) {
            Bound *newBound = new Bound(new_lower, bound->lower_instr(), new_upper, bound->upper_instr());
            _bound = newBound;
          } else {
            // overflow
            _bound = new Bound();
          }
        } else {
          _bound = new Bound();
        }
      } else {
        _bound = new Bound();
      }
    } else {
      Bound *bound = _rce->get_bound(x);
      if (ao->op() == Bytecodes::_isub) {
        if (bound->lower_instr() == y) {
          _bound = new Bound(Instruction::geq, NULL, bound->lower());
        } else {
          _bound = new Bound();
        }
      } else {
        _bound = new Bound();
      }
    }
  }
}

// IfOp
void RangeCheckEliminator::Visitor::do_IfOp(IfOp *ifOp)
{
  if (ifOp->tval()->type()->as_IntConstant() && ifOp->fval()->type()->as_IntConstant()) {
    int min = ifOp->tval()->type()->as_IntConstant()->value();
    int max = ifOp->fval()->type()->as_IntConstant()->value();
    if (min > max) {
      // min ^= max ^= min ^= max;
      int tmp = min;
      min = max;
      max = tmp;
    }
    _bound = new Bound(min, NULL, max, NULL);
  }
}

// Get bound. Returns the current bound on Value v. Normally this is the topmost element on the bound stack.
RangeCheckEliminator::Bound *RangeCheckEliminator::get_bound(Value v) {
  // Wrong type or NULL -> No bound
  if (!v || (!v->type()->as_IntType() && !v->type()->as_ObjectType())) return NULL;

  if (!_bounds.at(v->id())) {
    // First (default) bound is calculated
    // Create BoundStack
    _bounds.at_put(v->id(), new BoundStack());
    _visitor.clear_bound();
    Value visit_value = v;
    visit_value->visit(&_visitor);
    Bound *bound = _visitor.bound();
    if (bound) {
      _bounds.at(v->id())->push(bound);
    }
    if (_bounds.at(v->id())->length() == 0) {
      assert(!(v->as_Constant() && v->type()->as_IntConstant()), "constants not handled here");
      _bounds.at(v->id())->push(new Bound());
    }
  } else if (_bounds.at(v->id())->length() == 0) {
    // To avoid endless loops, bound is currently in calculation -> nothing known about it
    return new Bound();
  }

  // Return bound
  return _bounds.at(v->id())->top();
}

// Update bound
void RangeCheckEliminator::update_bound(IntegerStack &pushed, Value v, Instruction::Condition cond, Value value, int constant) {
  if (cond == Instruction::gtr) {
    cond = Instruction::geq;
    constant++;
  } else if (cond == Instruction::lss) {
    cond = Instruction::leq;
    constant--;
  }
  Bound *bound = new Bound(cond, value, constant);
  update_bound(pushed, v, bound);
}

// Checks for loop invariance. Returns true if the instruction is outside of the loop which is identified by loop_header.
bool RangeCheckEliminator::loop_invariant(BlockBegin *loop_header, Instruction *instruction) {
  assert(loop_header, "Loop header must not be null!");
  if (!instruction) return true;
  return instruction->dominator_depth() < loop_header->dominator_depth();
}

// Update bound. Pushes a new bound onto the stack. Tries to do a conjunction with the current bound.
void RangeCheckEliminator::update_bound(IntegerStack &pushed, Value v, Bound *bound) {
  if (v->as_Constant()) {
    // No bound update for constants
    return;
  }
  if (!_bounds.at(v->id())) {
    get_bound(v);
    assert(_bounds.at(v->id()), "Now Stack must exist");
  }
  Bound *top = NULL;
  if (_bounds.at(v->id())->length() > 0) {
    top = _bounds.at(v->id())->top();
  }
  if (top) {
    bound->and_op(top);
  }
  _bounds.at(v->id())->push(bound);
  pushed.append(v->id());
}

// Add instruction + idx for in block motion
void RangeCheckEliminator::add_access_indexed_info(InstructionList &indices, int idx, Value instruction, AccessIndexed *ai) {
  int id = instruction->id();
  AccessIndexedInfo *aii = _access_indexed_info.at(id);
  if (aii == NULL) {
    aii = new AccessIndexedInfo();
    _access_indexed_info.at_put(id, aii);
    indices.append(instruction);
    aii->_min = idx;
    aii->_max = idx;
    aii->_list = new AccessIndexedList();
  } else if (idx >= aii->_min && idx <= aii->_max) {
    remove_range_check(ai);
    return;
  }
  aii->_min = MIN2(aii->_min, idx);
  aii->_max = MAX2(aii->_max, idx);
  aii->_list->append(ai);
}

// In block motion. Tries to reorder checks in order to reduce some of them.
// Example:
// a[i] = 0;
// a[i+2] = 0;
// a[i+1] = 0;
// In this example the check for a[i+1] would be considered as unnecessary during the first iteration.
// After this i is only checked once for i >= 0 and i+2 < a.length before the first array access. If this
// check fails, deoptimization is called.
void RangeCheckEliminator::in_block_motion(BlockBegin *block, AccessIndexedList &accessIndexed, InstructionList &arrays) {
  InstructionList indices;

  // Now iterate over all arrays
  for (int i=0; i<arrays.length(); i++) {
    int max_constant = -1;
    AccessIndexedList list_constant;
    Value array = arrays.at(i);

    // For all AccessIndexed-instructions in this block concerning the current array.
    for(int j=0; j<accessIndexed.length(); j++) {
      AccessIndexed *ai = accessIndexed.at(j);
      if (ai->array() != array || !ai->check_flag(Instruction::NeedsRangeCheckFlag)) continue;

      Value index = ai->index();
      Constant *c = index->as_Constant();
      if (c != NULL) {
        int constant_value = c->type()->as_IntConstant()->value();
        if (constant_value >= 0) {
          if (constant_value <= max_constant) {
            // No range check needed for this
            remove_range_check(ai);
          } else {
            max_constant = constant_value;
            list_constant.append(ai);
          }
        }
      } else {
        int last_integer = 0;
        Instruction *last_instruction = index;
        int base = 0;
        ArithmeticOp *ao = index->as_ArithmeticOp();

        while (ao != NULL && (ao->x()->as_Constant() || ao->y()->as_Constant()) && (ao->op() == Bytecodes::_iadd || ao->op() == Bytecodes::_isub)) {
          c = ao->y()->as_Constant();
          Instruction *other = ao->x();
          if (!c && ao->op() == Bytecodes::_iadd) {
            c = ao->x()->as_Constant();
            other = ao->y();
          }

          if (c) {
            int value = c->type()->as_IntConstant()->value();
            if (value != min_jint) {
              if (ao->op() == Bytecodes::_isub) {
                value = -value;
              }
              base += value;
              last_integer = base;
              last_instruction = other;
            }
            index = other;
          } else {
            break;
          }
          ao = index->as_ArithmeticOp();
        }
        add_access_indexed_info(indices, last_integer, last_instruction, ai);
      }
    }

    // Iterate over all different indices
    if (_optimistic) {
      for (int i = 0; i < indices.length(); i++) {
        Instruction *index_instruction = indices.at(i);
        AccessIndexedInfo *info = _access_indexed_info.at(index_instruction->id());
        assert(info != NULL, "Info must not be null");

        // if idx < 0, max > 0, max + idx may fall between 0 and
        // length-1 and if min < 0, min + idx may overflow and be >=
        // 0. The predicate wouldn't trigger but some accesses could
        // be with a negative index. This test guarantees that for the
        // min and max value that are kept the predicate can't let
        // some incorrect accesses happen.
        bool range_cond = (info->_max < 0 || info->_max + min_jint <= info->_min);

        // Generate code only if more than 2 range checks can be eliminated because of that.
        // 2 because at least 2 comparisons are done
        if (info->_list->length() > 2 && range_cond) {
          AccessIndexed *first = info->_list->at(0);
          Instruction *insert_position = first->prev();
          assert(insert_position->next() == first, "prev was calculated");
          ValueStack *state = first->state_before();

          // Load min Constant
          Constant *min_constant = NULL;
          if (info->_min != 0) {
            min_constant = new Constant(new IntConstant(info->_min));
            NOT_PRODUCT(min_constant->set_printable_bci(first->printable_bci()));
            insert_position = insert_position->insert_after(min_constant);
          }

          // Load max Constant
          Constant *max_constant = NULL;
          if (info->_max != 0) {
            max_constant = new Constant(new IntConstant(info->_max));
            NOT_PRODUCT(max_constant->set_printable_bci(first->printable_bci()));
            insert_position = insert_position->insert_after(max_constant);
          }

          // Load array length
          Value length_instr = first->length();
          if (!length_instr) {
            ArrayLength *length = new ArrayLength(array, first->state_before()->copy());
            length->set_exception_state(length->state_before());
            length->set_flag(Instruction::DeoptimizeOnException, true);
            insert_position = insert_position->insert_after_same_bci(length);
            length_instr = length;
          }

          // Calculate lower bound
          Instruction *lower_compare = index_instruction;
          if (min_constant) {
            ArithmeticOp *ao = new ArithmeticOp(Bytecodes::_iadd, min_constant, lower_compare, NULL);
            insert_position = insert_position->insert_after_same_bci(ao);
            lower_compare = ao;
          }

          // Calculate upper bound
          Instruction *upper_compare = index_instruction;
          if (max_constant) {
            ArithmeticOp *ao = new ArithmeticOp(Bytecodes::_iadd, max_constant, upper_compare, NULL);
            insert_position = insert_position->insert_after_same_bci(ao);
            upper_compare = ao;
          }

          // Trick with unsigned compare is done
          int bci = NOT_PRODUCT(first->printable_bci()) PRODUCT_ONLY(-1);
          insert_position = predicate(upper_compare, Instruction::aeq, length_instr, state, insert_position, bci);
          insert_position = predicate_cmp_with_const(lower_compare, Instruction::leq, -1, state, insert_position);
          for (int j = 0; j<info->_list->length(); j++) {
            AccessIndexed *ai = info->_list->at(j);
            remove_range_check(ai);
          }
        }
      }

      if (list_constant.length() > 1) {
        AccessIndexed *first = list_constant.at(0);
        Instruction *insert_position = first->prev();
        ValueStack *state = first->state_before();
        // Load max Constant
        Constant *constant = new Constant(new IntConstant(max_constant));
        NOT_PRODUCT(constant->set_printable_bci(first->printable_bci()));
        insert_position = insert_position->insert_after(constant);
        Instruction *compare_instr = constant;
        Value length_instr = first->length();
        if (!length_instr) {
          ArrayLength *length = new ArrayLength(array, state->copy());
          length->set_exception_state(length->state_before());
          length->set_flag(Instruction::DeoptimizeOnException, true);
          insert_position = insert_position->insert_after_same_bci(length);
          length_instr = length;
        }
        // Compare for greater or equal to array length
        insert_position = predicate(compare_instr, Instruction::geq, length_instr, state, insert_position);
        for (int j = 0; j<list_constant.length(); j++) {
          AccessIndexed *ai = list_constant.at(j);
          remove_range_check(ai);
        }
      }
    }

    // Clear data structures for next array
    for (int i = 0; i < indices.length(); i++) {
      Instruction *index_instruction = indices.at(i);
      _access_indexed_info.at_put(index_instruction->id(), NULL);
    }
    indices.clear();
  }
}

bool RangeCheckEliminator::set_process_block_flags(BlockBegin *block) {
  Instruction *cur = block;
  bool process = false;

  while (cur) {
    process |= (cur->as_AccessIndexed() != NULL);
    cur = cur->next();
  }

  BlockList *dominates = block->dominates();
  for (int i=0; i<dominates->length(); i++) {
    BlockBegin *next = dominates->at(i);
    process |= set_process_block_flags(next);
  }

  if (!process) {
    block->set(BlockBegin::donot_eliminate_range_checks);
  }
  return process;
}

bool RangeCheckEliminator::is_ok_for_deoptimization(Instruction *insert_position, Instruction *array_instr, Instruction *length_instr, Instruction *lower_instr, int lower, Instruction *upper_instr, int upper) {
  bool upper_check = true;
  assert(lower_instr || lower >= 0, "If no lower_instr present, lower must be greater 0");
  assert(!lower_instr || lower_instr->dominator_depth() <= insert_position->dominator_depth(), "Dominator depth must be smaller");
  assert(!upper_instr || upper_instr->dominator_depth() <= insert_position->dominator_depth(), "Dominator depth must be smaller");
  assert(array_instr, "Array instruction must exist");
  assert(array_instr->dominator_depth() <= insert_position->dominator_depth(), "Dominator depth must be smaller");
  assert(!length_instr || length_instr->dominator_depth() <= insert_position->dominator_depth(), "Dominator depth must be smaller");

  if (upper_instr && upper_instr->as_ArrayLength() && upper_instr->as_ArrayLength()->array() == array_instr) {
    // static check
    if (upper >= 0) return false; // would always trigger a deopt:
                                  // array_length + x >= array_length, x >= 0 is always true
    upper_check = false;
  }
  if (lower_instr && lower_instr->as_ArrayLength() && lower_instr->as_ArrayLength()->array() == array_instr) {
    if (lower > 0) return false;
  }
  // No upper check required -> skip
  if (upper_check && upper_instr && upper_instr->type()->as_ObjectType() && upper_instr == array_instr) {
    // upper_instr is object means that the upper bound is the length
    // of the upper_instr.
    return false;
  }
  return true;
}

Instruction* RangeCheckEliminator::insert_after(Instruction* insert_position, Instruction* instr, int bci) {
  if (bci != -1) {
    NOT_PRODUCT(instr->set_printable_bci(bci));
    return insert_position->insert_after(instr);
  } else {
    return insert_position->insert_after_same_bci(instr);
  }
}

Instruction* RangeCheckEliminator::predicate(Instruction* left, Instruction::Condition cond, Instruction* right, ValueStack* state, Instruction *insert_position, int bci) {
  RangeCheckPredicate *deoptimize = new RangeCheckPredicate(left, cond, true, right, state->copy());
  return insert_after(insert_position, deoptimize, bci);
}

Instruction* RangeCheckEliminator::predicate_cmp_with_const(Instruction* instr, Instruction::Condition cond, int constant, ValueStack* state, Instruction *insert_position, int bci) {
  Constant *const_instr = new Constant(new IntConstant(constant));
  insert_position = insert_after(insert_position, const_instr, bci);
  return predicate(instr, cond, const_instr, state, insert_position);
}

Instruction* RangeCheckEliminator::predicate_add(Instruction* left, int left_const, Instruction::Condition cond, Instruction* right, ValueStack* state, Instruction *insert_position, int bci) {
  Constant *constant = new Constant(new IntConstant(left_const));
  insert_position = insert_after(insert_position, constant, bci);
  ArithmeticOp *ao = new ArithmeticOp(Bytecodes::_iadd, constant, left, NULL);
  insert_position = insert_position->insert_after_same_bci(ao);
  return predicate(ao, cond, right, state, insert_position);
}

Instruction* RangeCheckEliminator::predicate_add_cmp_with_const(Instruction* left, int left_const, Instruction::Condition cond, int constant, ValueStack* state, Instruction *insert_position, int bci) {
  Constant *const_instr = new Constant(new IntConstant(constant));
  insert_position = insert_after(insert_position, const_instr, bci);
  return predicate_add(left, left_const, cond, const_instr, state, insert_position);
}

// Insert deoptimization
void RangeCheckEliminator::insert_deoptimization(ValueStack *state, Instruction *insert_position, Instruction *array_instr, Instruction *length_instr, Instruction *lower_instr, int lower, Instruction *upper_instr, int upper, AccessIndexed *ai) {
  assert(is_ok_for_deoptimization(insert_position, array_instr, length_instr, lower_instr, lower, upper_instr, upper), "should have been tested before");
  bool upper_check = !(upper_instr && upper_instr->as_ArrayLength() && upper_instr->as_ArrayLength()->array() == array_instr);

  int bci = NOT_PRODUCT(ai->printable_bci()) PRODUCT_ONLY(-1);
  if (lower_instr) {
    assert(!lower_instr->type()->as_ObjectType(), "Must not be object type");
    if (lower == 0) {
      // Compare for less than 0
      insert_position = predicate_cmp_with_const(lower_instr, Instruction::lss, 0, state, insert_position, bci);
    } else if (lower > 0) {
      // Compare for smaller 0
      insert_position = predicate_add_cmp_with_const(lower_instr, lower, Instruction::lss, 0, state, insert_position, bci);
    } else {
      assert(lower < 0, "");
      // Add 1
      lower++;
      lower = -lower;
      // Compare for smaller or equal 0
      insert_position = predicate_cmp_with_const(lower_instr, Instruction::leq, lower, state, insert_position, bci);
    }
  }

  // No upper check required -> skip
  if (!upper_check) return;

  // We need to know length of array
  if (!length_instr) {
    // Load length if necessary
    ArrayLength *length = new ArrayLength(array_instr, state->copy());
    NOT_PRODUCT(length->set_printable_bci(ai->printable_bci()));
    length->set_exception_state(length->state_before());
    length->set_flag(Instruction::DeoptimizeOnException, true);
    insert_position = insert_position->insert_after(length);
    length_instr = length;
  }

  if (!upper_instr) {
    // Compare for geq array.length
    insert_position = predicate_cmp_with_const(length_instr, Instruction::leq, upper, state, insert_position, bci);
  } else {
    if (upper_instr->type()->as_ObjectType()) {
      assert(state, "must not be null");
      assert(upper_instr != array_instr, "should be");
      ArrayLength *length = new ArrayLength(upper_instr, state->copy());
      NOT_PRODUCT(length->set_printable_bci(ai->printable_bci()));
      length->set_flag(Instruction::DeoptimizeOnException, true);
      length->set_exception_state(length->state_before());
      insert_position = insert_position->insert_after(length);
      upper_instr = length;
    }
    assert(upper_instr->type()->as_IntType(), "Must not be object type!");

    if (upper == 0) {
      // Compare for geq array.length
      insert_position = predicate(upper_instr, Instruction::geq, length_instr, state, insert_position, bci);
    } else if (upper < 0) {
      // Compare for geq array.length
      insert_position = predicate_add(upper_instr, upper, Instruction::geq, length_instr, state, insert_position, bci);
    } else {
      assert(upper > 0, "");
      upper = -upper;
      // Compare for geq array.length
      insert_position = predicate_add(length_instr, upper, Instruction::leq, upper_instr, state, insert_position, bci);
    }
  }
}

// Add if condition
void RangeCheckEliminator::add_if_condition(IntegerStack &pushed, Value x, Value y, Instruction::Condition condition) {
  if (y->as_Constant()) return;

  int const_value = 0;
  Value instr_value = x;
  Constant *c = x->as_Constant();
  ArithmeticOp *ao = x->as_ArithmeticOp();

  if (c != NULL) {
    const_value = c->type()->as_IntConstant()->value();
    instr_value = NULL;
  } else if (ao != NULL &&  (!ao->x()->as_Constant() || !ao->y()->as_Constant()) && ((ao->op() == Bytecodes::_isub && ao->y()->as_Constant()) || ao->op() == Bytecodes::_iadd)) {
    assert(!ao->x()->as_Constant() || !ao->y()->as_Constant(), "At least one operator must be non-constant!");
    assert(ao->op() == Bytecodes::_isub || ao->op() == Bytecodes::_iadd, "Operation has to be add or sub!");
    c = ao->x()->as_Constant();
    if (c != NULL) {
      const_value = c->type()->as_IntConstant()->value();
      instr_value = ao->y();
    } else {
      c = ao->y()->as_Constant();
      if (c != NULL) {
        const_value = c->type()->as_IntConstant()->value();
        instr_value = ao->x();
      }
    }
    if (ao->op() == Bytecodes::_isub) {
      assert(ao->y()->as_Constant(), "1 - x not supported, only x - 1 is valid!");
      if (const_value > min_jint) {
        const_value = -const_value;
      } else {
        const_value = 0;
        instr_value = x;
      }
    }
  }

  update_bound(pushed, y, condition, instr_value, const_value);
}

// Process If
void RangeCheckEliminator::process_if(IntegerStack &pushed, BlockBegin *block, If *cond) {
  // Only if we are direct true / false successor and NOT both ! (even this may occur)
  if ((cond->tsux() == block || cond->fsux() == block) && cond->tsux() != cond->fsux()) {
    Instruction::Condition condition = cond->cond();
    if (cond->fsux() == block) {
      condition = Instruction::negate(condition);
    }
    Value x = cond->x();
    Value y = cond->y();
    if (x->type()->as_IntType() && y->type()->as_IntType()) {
      add_if_condition(pushed, y, x, condition);
      add_if_condition(pushed, x, y, Instruction::mirror(condition));
    }
  }
}

// Process access indexed
void RangeCheckEliminator::process_access_indexed(BlockBegin *loop_header, BlockBegin *block, AccessIndexed *ai) {
  TRACE_RANGE_CHECK_ELIMINATION(
    tty->fill_to(block->dominator_depth()*2)
  );
  TRACE_RANGE_CHECK_ELIMINATION(
    tty->print_cr("Access indexed: index=%d length=%d", ai->index()->id(), (ai->length() != NULL ? ai->length()->id() :-1 ))
  );

  if (ai->check_flag(Instruction::NeedsRangeCheckFlag)) {
    Bound *index_bound = get_bound(ai->index());
    if (!index_bound->has_lower() || !index_bound->has_upper()) {
      TRACE_RANGE_CHECK_ELIMINATION(
        tty->fill_to(block->dominator_depth()*2);
        tty->print_cr("Index instruction %d has no lower and/or no upper bound!", ai->index()->id())
      );
      return;
    }

    Bound *array_bound;
    if (ai->length()) {
      array_bound = get_bound(ai->length());
    } else {
      array_bound = get_bound(ai->array());
    }

    TRACE_RANGE_CHECK_ELIMINATION(
      tty->fill_to(block->dominator_depth()*2);
      tty->print("Index bound: ");
      index_bound->print();
      tty->print(", Array bound: ");
      array_bound->print();
      tty->cr();
    );

    if (in_array_bound(index_bound, ai->array()) ||
      (index_bound && array_bound && index_bound->is_smaller(array_bound) && !index_bound->lower_instr() && index_bound->lower() >= 0)) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Bounds check for instruction %d in block B%d can be fully eliminated!", ai->id(), ai->block()->block_id())
        );

        remove_range_check(ai);
    } else if (_optimistic && loop_header) {
      assert(ai->array(), "Array must not be null!");
      assert(ai->index(), "Index must not be null!");

      // Array instruction
      Instruction *array_instr = ai->array();
      if (!loop_invariant(loop_header, array_instr)) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Array %d is not loop invariant to header B%d", ai->array()->id(), loop_header->block_id())
        );
        return;
      }

      // Lower instruction
      Value index_instr = ai->index();
      Value lower_instr = index_bound->lower_instr();
      if (!loop_invariant(loop_header, lower_instr)) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Lower instruction %d not loop invariant!", lower_instr->id())
        );
        return;
      }
      if (!lower_instr && index_bound->lower() < 0) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Lower bound smaller than 0 (%d)!", index_bound->lower())
        );
        return;
      }

      // Upper instruction
      Value upper_instr = index_bound->upper_instr();
      if (!loop_invariant(loop_header, upper_instr)) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Upper instruction %d not loop invariant!", upper_instr->id())
        );
        return;
      }

      // Length instruction
      Value length_instr = ai->length();
      if (!loop_invariant(loop_header, length_instr)) {
        // Generate length instruction yourself!
        length_instr = NULL;
      }

      TRACE_RANGE_CHECK_ELIMINATION(
        tty->fill_to(block->dominator_depth()*2);
        tty->print_cr("LOOP INVARIANT access indexed %d found in block B%d!", ai->id(), ai->block()->block_id())
      );

      BlockBegin *pred_block = loop_header->dominator();
      assert(pred_block != NULL, "Every loop header has a dominator!");
      BlockEnd *pred_block_end = pred_block->end();
      Instruction *insert_position = pred_block_end->prev();
      ValueStack *state = pred_block_end->state_before();
      if (pred_block_end->as_Goto() && state == NULL) state = pred_block_end->state();
      assert(state, "State must not be null");

      // Add deoptimization to dominator of loop header
      TRACE_RANGE_CHECK_ELIMINATION(
        tty->fill_to(block->dominator_depth()*2);
        tty->print_cr("Inserting deopt at bci %d in block B%d!", state->bci(), insert_position->block()->block_id())
      );

      if (!is_ok_for_deoptimization(insert_position, array_instr, length_instr, lower_instr, index_bound->lower(), upper_instr, index_bound->upper())) {
        TRACE_RANGE_CHECK_ELIMINATION(
          tty->fill_to(block->dominator_depth()*2);
          tty->print_cr("Could not eliminate because of static analysis!")
        );
        return;
      }

      insert_deoptimization(state, insert_position, array_instr, length_instr, lower_instr, index_bound->lower(), upper_instr, index_bound->upper(), ai);

      // Finally remove the range check!
      remove_range_check(ai);
    }
  }
}

void RangeCheckEliminator::remove_range_check(AccessIndexed *ai) {
  ai->set_flag(Instruction::NeedsRangeCheckFlag, false);
  // no range check, no need for the length instruction anymore
  ai->clear_length();

  TRACE_RANGE_CHECK_ELIMINATION(
    tty->fill_to(ai->dominator_depth()*2);
    tty->print_cr("Range check for instruction %d eliminated!", ai->id());
  );

  ASSERT_RANGE_CHECK_ELIMINATION(
    Value array_length = ai->length();
    if (!array_length) {
      array_length = ai->array();
      assert(array_length->type()->as_ObjectType(), "Has to be object type!");
    }
    int cur_constant = -1;
    Value cur_value = array_length;
    if (cur_value->type()->as_IntConstant()) {
      cur_constant += cur_value->type()->as_IntConstant()->value();
      cur_value = NULL;
    }
    Bound *new_index_bound = new Bound(0, NULL, cur_constant, cur_value);
    add_assertions(new_index_bound, ai->index(), ai);
  );
}

// Calculate bounds for instruction in this block and children blocks in the dominator tree
void RangeCheckEliminator::calc_bounds(BlockBegin *block, BlockBegin *loop_header) {
  // Ensures a valid loop_header
  assert(!loop_header || loop_header->is_set(BlockBegin::linear_scan_loop_header_flag), "Loop header has to be real !");

  // Tracing output
  TRACE_RANGE_CHECK_ELIMINATION(
    tty->fill_to(block->dominator_depth()*2);
    tty->print_cr("Block B%d", block->block_id());
  );

  // Pushed stack for conditions
  IntegerStack pushed;
  // Process If
  BlockBegin *parent = block->dominator();
  if (parent != NULL) {
    If *cond = parent->end()->as_If();
    if (cond != NULL) {
      process_if(pushed, block, cond);
    }
  }

  // Interate over current block
  InstructionList arrays;
  AccessIndexedList accessIndexed;
  Instruction *cur = block;

  while (cur) {
    // Ensure cur wasn't inserted during the elimination
    if (cur->id() < this->_bounds.length()) {
      // Process only if it is an access indexed instruction
      AccessIndexed *ai = cur->as_AccessIndexed();
      if (ai != NULL) {
        process_access_indexed(loop_header, block, ai);
        accessIndexed.append(ai);
        if (!arrays.contains(ai->array())) {
          arrays.append(ai->array());
        }
        Bound *b = get_bound(ai->index());
        if (!b->lower_instr()) {
          // Lower bound is constant
          update_bound(pushed, ai->index(), Instruction::geq, NULL, 0);
        }
        if (!b->has_upper()) {
          if (ai->length() && ai->length()->type()->as_IntConstant()) {
            int value = ai->length()->type()->as_IntConstant()->value();
            update_bound(pushed, ai->index(), Instruction::lss, NULL, value);
          } else {
            // Has no upper bound
            Instruction *instr = ai->length();
            if (instr == NULL) instr = ai->array();
            update_bound(pushed, ai->index(), Instruction::lss, instr, 0);
          }
        }
      }
    }
    cur = cur->next();
  }

  // Output current condition stack
  TRACE_RANGE_CHECK_ELIMINATION(dump_condition_stack(block));

  // Do in block motion of range checks
  in_block_motion(block, accessIndexed, arrays);

  // Call all dominated blocks
  for (int i=0; i<block->dominates()->length(); i++) {
    BlockBegin *next = block->dominates()->at(i);
    if (!next->is_set(BlockBegin::donot_eliminate_range_checks)) {
      // if current block is a loop header and:
      // - next block belongs to the same loop
      // or
      // - next block belongs to an inner loop
      // then current block is the loop header for next block
      if (block->is_set(BlockBegin::linear_scan_loop_header_flag) && (block->loop_index() == next->loop_index() || next->loop_depth() > block->loop_depth())) {
        calc_bounds(next, block);
      } else {
        calc_bounds(next, loop_header);
      }
    }
  }

  // Reset stack
  for (int i=0; i<pushed.length(); i++) {
    _bounds.at(pushed.at(i))->pop();
  }
}

#ifndef PRODUCT
// Dump condition stack
void RangeCheckEliminator::dump_condition_stack(BlockBegin *block) {
  for (int i=0; i<_ir->linear_scan_order()->length(); i++) {
    BlockBegin *cur_block = _ir->linear_scan_order()->at(i);
    Instruction *instr = cur_block;
    for_each_phi_fun(cur_block, phi,
                     BoundStack *bound_stack = _bounds.at(phi->id());
                     if (bound_stack && bound_stack->length() > 0) {
                       Bound *bound = bound_stack->top();
                       if ((bound->has_lower() || bound->has_upper()) && (bound->lower_instr() != phi || bound->upper_instr() != phi || bound->lower() != 0 || bound->upper() != 0)) {
                           TRACE_RANGE_CHECK_ELIMINATION(tty->fill_to(2*block->dominator_depth());
                                                         tty->print("i%d", phi->id());
                                                         tty->print(": ");
                                                         bound->print();
                                                         tty->cr();
                           );
                         }
                     });

    while (!instr->as_BlockEnd()) {
      if (instr->id() < _bounds.length()) {
        BoundStack *bound_stack = _bounds.at(instr->id());
        if (bound_stack && bound_stack->length() > 0) {
          Bound *bound = bound_stack->top();
          if ((bound->has_lower() || bound->has_upper()) && (bound->lower_instr() != instr || bound->upper_instr() != instr || bound->lower() != 0 || bound->upper() != 0)) {
              TRACE_RANGE_CHECK_ELIMINATION(tty->fill_to(2*block->dominator_depth());
                                            tty->print("i%d", instr->id());
                                            tty->print(": ");
                                            bound->print();
                                            tty->cr();
              );
          }
        }
      }
      instr = instr->next();
    }
  }
}
#endif

#ifdef ASSERT
// Verification or the IR
RangeCheckEliminator::Verification::Verification(IR *ir) : _used(BlockBegin::number_of_blocks(), BlockBegin::number_of_blocks(), false) {
  this->_ir = ir;
  ir->iterate_linear_scan_order(this);
}

// Verify this block
void RangeCheckEliminator::Verification::block_do(BlockBegin *block) {
  If *cond = block->end()->as_If();
  // Watch out: tsux and fsux can be the same!
  if (block->number_of_sux() > 1) {
    for (int i=0; i<block->number_of_sux(); i++) {
      BlockBegin *sux = block->sux_at(i);
      BlockBegin *pred = NULL;
      for (int j=0; j<sux->number_of_preds(); j++) {
        BlockBegin *cur = sux->pred_at(j);
        assert(cur != NULL, "Predecessor must not be null");
        if (!pred) {
          pred = cur;
        }
        assert(cur == pred, "Block must not have more than one predecessor if its predecessor has more than one successor");
      }
      assert(sux->number_of_preds() >= 1, "Block must have at least one predecessor");
      assert(sux->pred_at(0) == block, "Wrong successor");
    }
  }

  BlockBegin *dominator = block->dominator();
  if (dominator) {
    assert(block != _ir->start(), "Start block must not have a dominator!");
    assert(can_reach(dominator, block), "Dominator can't reach his block !");
    assert(can_reach(_ir->start(), dominator), "Dominator is unreachable !");
    assert(!can_reach(_ir->start(), block, dominator), "Wrong dominator ! Block can be reached anyway !");
    BlockList *all_blocks = _ir->linear_scan_order();
    for (int i=0; i<all_blocks->length(); i++) {
      BlockBegin *cur = all_blocks->at(i);
      if (cur != dominator && cur != block) {
        assert(can_reach(dominator, block, cur), "There has to be another dominator!");
      }
    }
  } else {
    assert(block == _ir->start(), "Only start block must not have a dominator");
  }

  if (block->is_set(BlockBegin::linear_scan_loop_header_flag)) {
    int loop_index = block->loop_index();
    BlockList *all_blocks = _ir->linear_scan_order();
    assert(block->number_of_preds() >= 1, "Block must have at least one predecessor");
    assert(!block->is_set(BlockBegin::exception_entry_flag), "Loop header must not be exception handler!");

    bool loop_through_xhandler = false;
    for (int i=0; i<block->number_of_sux(); i++) {
      BlockBegin *sux = block->sux_at(i);
      if (!loop_through_xhandler) {
        if (sux->loop_depth() == block->loop_depth() && sux->loop_index() != block->loop_index()) {
          loop_through_xhandler = is_backbranch_from_xhandler(block);
          assert(loop_through_xhandler, "Loop indices have to be the same if same depths but no backbranch from xhandler");
        }
      }
      assert(sux->loop_depth() == block->loop_depth() || sux->loop_index() != block->loop_index(), "Loop index has to be different");
    }

    for (int i=0; i<all_blocks->length(); i++) {
      BlockBegin *cur = all_blocks->at(i);
      if (cur->loop_index() == loop_index && cur != block) {
        assert(dominates(block->dominator(), cur), "Dominator of loop header must dominate all loop blocks");
      }
    }
  }

  Instruction *cur = block;
  while (cur) {
    assert(cur->block() == block, "Block begin has to be set correctly!");
    cur = cur->next();
  }
}

// Called when a successor of a block has the same loop depth but a different loop index. This can happen if a backbranch comes from
// an exception handler of a loop head block, for example, when a loop is only executed once on the non-exceptional path but is
// repeated in case of an exception. In this case, the edge block->sux is not critical and was not split before.
// Check if there is such a backbranch from an xhandler of 'block'.
bool RangeCheckEliminator::Verification::is_backbranch_from_xhandler(BlockBegin* block) {
  for (int i = 0; i < block->number_of_exception_handlers(); i++) {
    BlockBegin *xhandler = block->exception_handler_at(i);
    for (int j = 0; j < block->number_of_preds(); j++) {
      if (dominates(xhandler, block->pred_at(j)) || xhandler == block->pred_at(j)) {
        return true;
      }
    }
  }

  // In case of nested xhandlers, we need to walk through the loop (and all blocks belonging to exception handlers)
  // to find an xhandler of 'block'.
  if (block->number_of_exception_handlers() > 0) {
    for (int i = 0; i < block->number_of_preds(); i++) {
      BlockBegin* pred = block->pred_at(i);
      if (pred->loop_index() == block->loop_index()) {
        // Only check blocks that belong to the loop
        // Do a BFS to find an xhandler block of 'block' starting from 'pred'
        ResourceMark rm;
        ResourceBitMap visited(BlockBegin::number_of_blocks());
        BlockBeginList list;
        list.push(pred);
        while (!list.is_empty()) {
          BlockBegin* next = list.pop();
          if (!visited.at(next->block_id())) {
            visited.set_bit(next->block_id());
            for (int j = 0; j < block->number_of_exception_handlers(); j++) {
               if (next == block->exception_handler_at(j)) {
                 return true;
               }
            }
            for (int j = 0; j < next->number_of_preds(); j++) {
               if (next->pred_at(j) != block) {
                 list.push(next->pred_at(j));
               }
            }
          }
        }
      }
    }
  }
  return false;
}

// Loop header must dominate all loop blocks
bool RangeCheckEliminator::Verification::dominates(BlockBegin *dominator, BlockBegin *block) {
  BlockBegin *cur = block->dominator();
  while (cur && cur != dominator) {
    cur = cur->dominator();
  }
  return cur == dominator;
}

// Try to reach Block end beginning in Block start and not using Block dont_use
bool RangeCheckEliminator::Verification::can_reach(BlockBegin *start, BlockBegin *end, BlockBegin *dont_use /* = NULL */) {
  if (start == end) return start != dont_use;
  // Simple BSF from start to end
  //  BlockBeginList _current;
  for (int i=0; i < _used.length(); i++) {
    _used.at_put(i, false);
  }
  _current.trunc_to(0);
  _successors.trunc_to(0);
  if (start != dont_use) {
    _current.push(start);
    _used.at_put(start->block_id(), true);
  }

  //  BlockBeginList _successors;
  while (_current.length() > 0) {
    BlockBegin *cur = _current.pop();
    // Add exception handlers to list
    for (int i=0; i<cur->number_of_exception_handlers(); i++) {
      BlockBegin *xhandler = cur->exception_handler_at(i);
      _successors.push(xhandler);
      // Add exception handlers of _successors to list
      for (int j=0; j<xhandler->number_of_exception_handlers(); j++) {
        BlockBegin *sux_xhandler = xhandler->exception_handler_at(j);
        _successors.push(sux_xhandler);
      }
    }
    // Add normal _successors to list
    for (int i=0; i<cur->number_of_sux(); i++) {
      BlockBegin *sux = cur->sux_at(i);
      _successors.push(sux);
      // Add exception handlers of _successors to list
      for (int j=0; j<sux->number_of_exception_handlers(); j++) {
        BlockBegin *xhandler = sux->exception_handler_at(j);
        _successors.push(xhandler);
      }
    }
    for (int i=0; i<_successors.length(); i++) {
      BlockBegin *sux = _successors.at(i);
      assert(sux != NULL, "Successor must not be NULL!");
      if (sux == end) {
        return true;
      }
      if (sux != dont_use && !_used.at(sux->block_id())) {
        _used.at_put(sux->block_id(), true);
        _current.push(sux);
      }
    }
    _successors.trunc_to(0);
  }

  return false;
}
#endif // ASSERT

// Bound
RangeCheckEliminator::Bound::~Bound() {
}

// Bound constructor
RangeCheckEliminator::Bound::Bound() {
  this->_lower = min_jint;
  this->_upper = max_jint;
  this->_lower_instr = NULL;
  this->_upper_instr = NULL;
}

// Bound constructor
RangeCheckEliminator::Bound::Bound(int lower, Value lower_instr, int upper, Value upper_instr) {
  assert(!lower_instr || !lower_instr->as_Constant() || !lower_instr->type()->as_IntConstant(), "Must not be constant!");
  assert(!upper_instr || !upper_instr->as_Constant() || !upper_instr->type()->as_IntConstant(), "Must not be constant!");
  this->_lower = lower;
  this->_upper = upper;
  this->_lower_instr = lower_instr;
  this->_upper_instr = upper_instr;
}

// Bound constructor
RangeCheckEliminator::Bound::Bound(Instruction::Condition cond, Value v, int constant) {
  assert(!v || (v->type() && (v->type()->as_IntType() || v->type()->as_ObjectType())), "Type must be array or integer!");
  assert(!v || !v->as_Constant() || !v->type()->as_IntConstant(), "Must not be constant!");

  if (cond == Instruction::eql) {
    _lower = constant;
    _lower_instr = v;
    _upper = constant;
    _upper_instr = v;
  } else if (cond == Instruction::neq) {
    _lower = min_jint;
    _upper = max_jint;
    _lower_instr = NULL;
    _upper_instr = NULL;
    if (v == NULL) {
      if (constant == min_jint) {
        _lower++;
      }
      if (constant == max_jint) {
        _upper--;
      }
    }
  } else if (cond == Instruction::geq) {
    _lower = constant;
    _lower_instr = v;
    _upper = max_jint;
    _upper_instr = NULL;
  } else if (cond == Instruction::leq) {
    _lower = min_jint;
    _lower_instr = NULL;
    _upper = constant;
    _upper_instr = v;
  } else {
    ShouldNotReachHere();
  }
}

// Set lower
void RangeCheckEliminator::Bound::set_lower(int value, Value v) {
  assert(!v || !v->as_Constant() || !v->type()->as_IntConstant(), "Must not be constant!");
  this->_lower = value;
  this->_lower_instr = v;
}

// Set upper
void RangeCheckEliminator::Bound::set_upper(int value, Value v) {
  assert(!v || !v->as_Constant() || !v->type()->as_IntConstant(), "Must not be constant!");
  this->_upper = value;
  this->_upper_instr = v;
}

// Add constant -> no overflow may occur
void RangeCheckEliminator::Bound::add_constant(int value) {
  this->_lower += value;
  this->_upper += value;
}

// or
void RangeCheckEliminator::Bound::or_op(Bound *b) {
  // Watch out, bound is not guaranteed not to overflow!
  // Update lower bound
  if (_lower_instr != b->_lower_instr || (_lower_instr && _lower != b->_lower)) {
    _lower_instr = NULL;
    _lower = min_jint;
  } else {
    _lower = MIN2(_lower, b->_lower);
  }
  // Update upper bound
  if (_upper_instr != b->_upper_instr || (_upper_instr && _upper != b->_upper)) {
    _upper_instr = NULL;
    _upper = max_jint;
  } else {
    _upper = MAX2(_upper, b->_upper);
  }
}

// and
void RangeCheckEliminator::Bound::and_op(Bound *b) {
  // Update lower bound
  if (_lower_instr == b->_lower_instr) {
    _lower = MAX2(_lower, b->_lower);
  }
  if (b->has_lower()) {
    bool set = true;
    if (_lower_instr != NULL && b->_lower_instr != NULL) {
      set = (_lower_instr->dominator_depth() > b->_lower_instr->dominator_depth());
    }
    if (set) {
      _lower = b->_lower;
      _lower_instr = b->_lower_instr;
    }
  }
  // Update upper bound
  if (_upper_instr == b->_upper_instr) {
    _upper = MIN2(_upper, b->_upper);
  }
  if (b->has_upper()) {
    bool set = true;
    if (_upper_instr != NULL && b->_upper_instr != NULL) {
      set = (_upper_instr->dominator_depth() > b->_upper_instr->dominator_depth());
    }
    if (set) {
      _upper = b->_upper;
      _upper_instr = b->_upper_instr;
    }
  }
}

// has_upper
bool RangeCheckEliminator::Bound::has_upper() {
  return _upper_instr != NULL || _upper < max_jint;
}

// is_smaller
bool RangeCheckEliminator::Bound::is_smaller(Bound *b) {
  if (b->_lower_instr != _upper_instr) {
    return false;
  }
  return _upper < b->_lower;
}

// has_lower
bool RangeCheckEliminator::Bound::has_lower() {
  return _lower_instr != NULL || _lower > min_jint;
}

// in_array_bound
bool RangeCheckEliminator::in_array_bound(Bound *bound, Value array){
  if (!bound) return false;
  assert(array != NULL, "Must not be null!");
  assert(bound != NULL, "Must not be null!");
  if (bound->lower() >=0 && bound->lower_instr() == NULL && bound->upper() < 0 && bound->upper_instr() != NULL) {
    ArrayLength *len = bound->upper_instr()->as_ArrayLength();
    if (bound->upper_instr() == array || (len != NULL && len->array() == array)) {
      return true;
    }
  }
  return false;
}

// remove_lower
void RangeCheckEliminator::Bound::remove_lower() {
  _lower = min_jint;
  _lower_instr = NULL;
}

// remove_upper
void RangeCheckEliminator::Bound::remove_upper() {
  _upper = max_jint;
  _upper_instr = NULL;
}

// upper
int RangeCheckEliminator::Bound::upper() {
  return _upper;
}

// lower
int RangeCheckEliminator::Bound::lower() {
  return _lower;
}

// upper_instr
Value RangeCheckEliminator::Bound::upper_instr() {
  return _upper_instr;
}

// lower_instr
Value RangeCheckEliminator::Bound::lower_instr() {
  return _lower_instr;
}

// print
void RangeCheckEliminator::Bound::print() {
  tty->print("%s", "");
  if (this->_lower_instr || this->_lower != min_jint) {
    if (this->_lower_instr) {
      tty->print("i%d", this->_lower_instr->id());
      if (this->_lower > 0) {
        tty->print("+%d", _lower);
      }
      if (this->_lower < 0) {
        tty->print("%d", _lower);
      }
    } else {
      tty->print("%d", _lower);
    }
    tty->print(" <= ");
  }
  tty->print("x");
  if (this->_upper_instr || this->_upper != max_jint) {
    tty->print(" <= ");
    if (this->_upper_instr) {
      tty->print("i%d", this->_upper_instr->id());
      if (this->_upper > 0) {
        tty->print("+%d", _upper);
      }
      if (this->_upper < 0) {
        tty->print("%d", _upper);
      }
    } else {
      tty->print("%d", _upper);
    }
  }
}

// Copy
RangeCheckEliminator::Bound *RangeCheckEliminator::Bound::copy() {
  Bound *b = new Bound();
  b->_lower = _lower;
  b->_lower_instr = _lower_instr;
  b->_upper = _upper;
  b->_upper_instr = _upper_instr;
  return b;
}

#ifdef ASSERT
// Add assertion
void RangeCheckEliminator::Bound::add_assertion(Instruction *instruction, Instruction *position, int i, Value instr, Instruction::Condition cond) {
  Instruction *result = position;
  Instruction *compare_with = NULL;
  ValueStack *state = position->state_before();
  if (position->as_BlockEnd() && !position->as_Goto()) {
    state = position->as_BlockEnd()->state_before();
  }
  Instruction *instruction_before = position->prev();
  if (position->as_Return() && Compilation::current()->method()->is_synchronized() && instruction_before->as_MonitorExit()) {
    instruction_before = instruction_before->prev();
  }
  result = instruction_before;
  // Load constant only if needed
  Constant *constant = NULL;
  if (i != 0 || !instr) {
    constant = new Constant(new IntConstant(i));
    NOT_PRODUCT(constant->set_printable_bci(position->printable_bci()));
    result = result->insert_after(constant);
    compare_with = constant;
  }

  if (instr) {
    assert(instr->type()->as_ObjectType() || instr->type()->as_IntType(), "Type must be array or integer!");
    compare_with = instr;
    // Load array length if necessary
    Instruction *op = instr;
    if (instr->type()->as_ObjectType()) {
      assert(state, "must not be null");
      ArrayLength *length = new ArrayLength(instr, state->copy());
      NOT_PRODUCT(length->set_printable_bci(position->printable_bci()));
      length->set_exception_state(length->state_before());
      result = result->insert_after(length);
      op = length;
      compare_with = length;
    }
    // Add operation only if necessary
    if (constant) {
      ArithmeticOp *ao = new ArithmeticOp(Bytecodes::_iadd, constant, op, NULL);
      NOT_PRODUCT(ao->set_printable_bci(position->printable_bci()));
      result = result->insert_after(ao);
      compare_with = ao;
      // TODO: Check that add operation does not overflow!
    }
  }
  assert(compare_with != NULL, "You have to compare with something!");
  assert(instruction != NULL, "Instruction must not be null!");

  if (instruction->type()->as_ObjectType()) {
    // Load array length if necessary
    Instruction *op = instruction;
    assert(state, "must not be null");
    ArrayLength *length = new ArrayLength(instruction, state->copy());
    length->set_exception_state(length->state_before());
    NOT_PRODUCT(length->set_printable_bci(position->printable_bci()));
    result = result->insert_after(length);
    instruction = length;
  }

  Assert *assert = new Assert(instruction, cond, false, compare_with);
  NOT_PRODUCT(assert->set_printable_bci(position->printable_bci()));
  result->insert_after(assert);
}

// Add assertions
void RangeCheckEliminator::add_assertions(Bound *bound, Instruction *instruction, Instruction *position) {
  // Add lower bound assertion
  if (bound->has_lower()) {
    bound->add_assertion(instruction, position, bound->lower(), bound->lower_instr(), Instruction::geq);
  }
  // Add upper bound assertion
  if (bound->has_upper()) {
    bound->add_assertion(instruction, position, bound->upper(), bound->upper_instr(), Instruction::leq);
  }
}
#endif
