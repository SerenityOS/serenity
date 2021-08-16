/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/vmSymbols.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArray.hpp"
#include "ci/ciInstance.hpp"
#include "ci/ciObject.hpp"


#ifndef PRODUCT

const char* InstructionPrinter::basic_type_name(BasicType type) {
  const char* n = type2name(type);
  if (n == NULL || type > T_VOID) {
    return "???";
  }
  return n;
}


const char* InstructionPrinter::cond_name(If::Condition cond) {
  switch (cond) {
    case If::eql: return "==";
    case If::neq: return "!=";
    case If::lss: return "<";
    case If::leq: return "<=";
    case If::gtr: return ">";
    case If::geq: return ">=";
    case If::aeq: return "|>=|";
    case If::beq: return "|<=|";
    default:
      ShouldNotReachHere();
      return NULL;
  }
}


const char* InstructionPrinter::op_name(Bytecodes::Code op) {
  switch (op) {
    // arithmetic ops
    case Bytecodes::_iadd : // fall through
    case Bytecodes::_ladd : // fall through
    case Bytecodes::_fadd : // fall through
    case Bytecodes::_dadd : return "+";
    case Bytecodes::_isub : // fall through
    case Bytecodes::_lsub : // fall through
    case Bytecodes::_fsub : // fall through
    case Bytecodes::_dsub : return "-";
    case Bytecodes::_imul : // fall through
    case Bytecodes::_lmul : // fall through
    case Bytecodes::_fmul : // fall through
    case Bytecodes::_dmul : return "*";
    case Bytecodes::_idiv : // fall through
    case Bytecodes::_ldiv : // fall through
    case Bytecodes::_fdiv : // fall through
    case Bytecodes::_ddiv : return "/";
    case Bytecodes::_irem : // fall through
    case Bytecodes::_lrem : // fall through
    case Bytecodes::_frem : // fall through
    case Bytecodes::_drem : return "%";
    // shift ops
    case Bytecodes::_ishl : // fall through
    case Bytecodes::_lshl : return "<<";
    case Bytecodes::_ishr : // fall through
    case Bytecodes::_lshr : return ">>";
    case Bytecodes::_iushr: // fall through
    case Bytecodes::_lushr: return ">>>";
    // logic ops
    case Bytecodes::_iand : // fall through
    case Bytecodes::_land : return "&";
    case Bytecodes::_ior  : // fall through
    case Bytecodes::_lor  : return "|";
    case Bytecodes::_ixor : // fall through
    case Bytecodes::_lxor : return "^";
    default               : return Bytecodes::name(op);
  }
}


bool InstructionPrinter::is_illegal_phi(Value v) {
  Phi* phi = v ? v->as_Phi() : NULL;
  if (phi && phi->is_illegal()) {
    return true;
  }
  return false;
}


bool InstructionPrinter::is_phi_of_block(Value v, BlockBegin* b) {
  Phi* phi = v ? v->as_Phi() : NULL;
  return phi && phi->block() == b;
}


void InstructionPrinter::print_klass(ciKlass* klass) {
  klass->name()->print_symbol_on(output());
}


void InstructionPrinter::print_object(Value obj) {
  ValueType* type = obj->type();
  if (type->as_ObjectConstant() != NULL) {
    ciObject* value = type->as_ObjectConstant()->value();
    if (value->is_null_object()) {
      output()->print("null");
    } else if (!value->is_loaded()) {
      output()->print("<unloaded object " INTPTR_FORMAT ">", p2i(value));
    } else {
      output()->print("<object " INTPTR_FORMAT " klass=", p2i(value->constant_encoding()));
      print_klass(value->klass());
      output()->print(">");
    }
  } else if (type->as_InstanceConstant() != NULL) {
    ciInstance* value = type->as_InstanceConstant()->value();
    if (value->is_loaded()) {
      output()->print("<instance " INTPTR_FORMAT " klass=", p2i(value->constant_encoding()));
      print_klass(value->klass());
      output()->print(">");
    } else {
      output()->print("<unloaded instance " INTPTR_FORMAT ">", p2i(value));
    }
  } else if (type->as_ArrayConstant() != NULL) {
    output()->print("<array " INTPTR_FORMAT ">", p2i(type->as_ArrayConstant()->value()->constant_encoding()));
  } else if (type->as_ClassConstant() != NULL) {
    ciInstanceKlass* klass = type->as_ClassConstant()->value();
    if (!klass->is_loaded()) {
      output()->print("<unloaded> ");
    }
    output()->print("class ");
    print_klass(klass);
  } else if (type->as_MethodConstant() != NULL) {
    ciMethod* m = type->as_MethodConstant()->value();
    output()->print("<method %s.%s>", m->holder()->name()->as_utf8(), m->name()->as_utf8());
  } else {
    output()->print("???");
  }
}


void InstructionPrinter::print_temp(Value value) {
  output()->print("%c%d", value->type()->tchar(), value->id());
}


void InstructionPrinter::print_field(AccessField* field) {
  print_value(field->obj());
  output()->print("._%d", field->offset());
}


void InstructionPrinter::print_indexed(AccessIndexed* indexed) {
  print_value(indexed->array());
  output()->put('[');
  print_value(indexed->index());
  output()->put(']');
  if (indexed->length() != NULL) {
    output()->put('(');
    print_value(indexed->length());
    output()->put(')');
  }
}


void InstructionPrinter::print_monitor(AccessMonitor* monitor) {
  output()->print("monitor[%d](", monitor->monitor_no());
  print_value(monitor->obj());
  output()->put(')');
}


void InstructionPrinter::print_op2(Op2* instr) {
  print_value(instr->x());
  output()->print(" %s ", op_name(instr->op()));
  print_value(instr->y());
}


void InstructionPrinter::print_value(Value value) {
  if (value == NULL) {
    output()->print("NULL");
  } else {
    print_temp(value);
  }
}


void InstructionPrinter::print_instr(Instruction* instr) {
  instr->visit(this);
}


void InstructionPrinter::print_stack(ValueStack* stack) {
  int start_position = output()->position();
  if (stack->stack_is_empty()) {
    output()->print("empty stack");
  } else {
    output()->print("stack [");
    for (int i = 0; i < stack->stack_size();) {
      if (i > 0) output()->print(", ");
      output()->print("%d:", i);
      Value value = stack->stack_at_inc(i);
      print_value(value);
      Phi* phi = value->as_Phi();
      if (phi != NULL) {
        if (phi->operand()->is_valid()) {
          output()->print(" ");
          phi->operand()->print(output());
        }
      }
    }
    output()->put(']');
  }
  if (!stack->no_active_locks()) {
    // print out the lines on the line below this
    // one at the same indentation level.
    output()->cr();
    fill_to(start_position, ' ');
    output()->print("locks [");
    for (int i = 0; i < stack->locks_size(); i++) {
      Value t = stack->lock_at(i);
      if (i > 0) output()->print(", ");
      output()->print("%d:", i);
      if (t == NULL) {
        // synchronized methods push null on the lock stack
        output()->print("this");
      } else {
        print_value(t);
      }
    }
    output()->print("]");
  }
}


void InstructionPrinter::print_inline_level(BlockBegin* block) {
  output()->print_cr("inlining depth %d", block->scope()->level());
}


void InstructionPrinter::print_unsafe_op(UnsafeOp* op, const char* name) {
  output()->print("%s", name);
  print_value(op->object());
  output()->print(", ");
  print_value(op->offset());
}


void InstructionPrinter::print_phi(int i, Value v, BlockBegin* b) {
  Phi* phi = v->as_Phi();
  output()->print("%2d  ", i);
  print_value(v);
  // print phi operands
  if (phi && phi->block() == b) {
    output()->print(" [");
    for (int j = 0; j < phi->operand_count(); j ++) {
      output()->print(" ");
      Value opd = phi->operand_at(j);
      if (opd) print_value(opd);
      else output()->print("NULL");
    }
    output()->print("] ");
  }
  print_alias(v);
}


void InstructionPrinter::print_alias(Value v) {
  if (v != v->subst()) {
    output()->print("alias "); print_value(v->subst());
  }
}


void InstructionPrinter::fill_to(int pos, char filler) {
  while (output()->position() < pos) output()->put(filler);
}


void InstructionPrinter::print_head() {
  const char filler = '_';
  fill_to(bci_pos  , filler); output()->print("bci"  );
  fill_to(use_pos  , filler); output()->print("use"  );
  fill_to(temp_pos , filler); output()->print("tid"  );
  fill_to(instr_pos, filler); output()->print("instr");
  fill_to(end_pos  , filler);
  output()->cr();
}


void InstructionPrinter::print_line(Instruction* instr) {
  // print instruction data on one line
  if (instr->is_pinned()) output()->put('.');
  if (instr->has_printable_bci()) {
    fill_to(bci_pos  ); output()->print("%d", instr->printable_bci());
  }
  fill_to(use_pos  ); output()->print("%d", instr->use_count());
  fill_to(temp_pos ); print_temp(instr);
  fill_to(instr_pos); print_instr(instr);
  output()->cr();
  // add a line for StateSplit instructions w/ non-empty stacks
  // (make it robust so we can print incomplete instructions)
  StateSplit* split = instr->as_StateSplit();
  if (split != NULL && split->state() != NULL && !split->state()->stack_is_empty()) {
    fill_to(instr_pos); print_stack(split->state());
    output()->cr();
  }
}


void InstructionPrinter::do_Phi(Phi* x) {
  output()->print("phi function");  // make that more detailed later
  if (x->is_illegal())
    output()->print(" (illegal)");
}


void InstructionPrinter::do_Local(Local* x) {
  output()->print("local[index %d]", x->java_index());
}


void InstructionPrinter::do_Constant(Constant* x) {
  ValueType* t = x->type();
  switch (t->tag()) {
    case intTag    : output()->print("%d"  , t->as_IntConstant   ()->value());    break;
    case longTag   : output()->print(JLONG_FORMAT, t->as_LongConstant()->value()); output()->print("L"); break;
    case floatTag  : output()->print("%g"  , t->as_FloatConstant ()->value());    break;
    case doubleTag : output()->print("%gD" , t->as_DoubleConstant()->value());    break;
    case objectTag : print_object(x);                                        break;
    case addressTag: output()->print("bci:%d", t->as_AddressConstant()->value()); break;
    default        : output()->print("???");                                      break;
  }
}


void InstructionPrinter::do_LoadField(LoadField* x) {
  print_field(x);
  output()->print(" (%c)", type2char(x->field()->type()->basic_type()));
  output()->print(" %s", x->field()->name()->as_utf8());
}


void InstructionPrinter::do_StoreField(StoreField* x) {
  print_field(x);
  output()->print(" := ");
  print_value(x->value());
  output()->print(" (%c)", type2char(x->field()->type()->basic_type()));
  output()->print(" %s", x->field()->name()->as_utf8());
}


void InstructionPrinter::do_ArrayLength(ArrayLength* x) {
  print_value(x->array());
  output()->print(".length");
}


void InstructionPrinter::do_LoadIndexed(LoadIndexed* x) {
  print_indexed(x);
  output()->print(" (%c)", type2char(x->elt_type()));
  if (x->check_flag(Instruction::NeedsRangeCheckFlag)) {
    output()->print(" [rc]");
  }
}


void InstructionPrinter::do_StoreIndexed(StoreIndexed* x) {
  print_indexed(x);
  output()->print(" := ");
  print_value(x->value());
  output()->print(" (%c)", type2char(x->elt_type()));
  if (x->check_flag(Instruction::NeedsRangeCheckFlag)) {
    output()->print(" [rc]");
  }
}

void InstructionPrinter::do_NegateOp(NegateOp* x) {
  output()->put('-');
  print_value(x->x());
}


void InstructionPrinter::do_ArithmeticOp(ArithmeticOp* x) {
  print_op2(x);
}


void InstructionPrinter::do_ShiftOp(ShiftOp* x) {
  print_op2(x);
}


void InstructionPrinter::do_LogicOp(LogicOp* x) {
  print_op2(x);
}


void InstructionPrinter::do_CompareOp(CompareOp* x) {
  print_op2(x);
}


void InstructionPrinter::do_IfOp(IfOp* x) {
  print_value(x->x());
  output()->print(" %s ", cond_name(x->cond()));
  print_value(x->y());
  output()->print(" ? ");
  print_value(x->tval());
  output()->print(" : ");
  print_value(x->fval());
}


void InstructionPrinter::do_Convert(Convert* x) {
  output()->print("%s(", Bytecodes::name(x->op()));
  print_value(x->value());
  output()->put(')');
}


void InstructionPrinter::do_NullCheck(NullCheck* x) {
  output()->print("null_check(");
  print_value(x->obj());
  output()->put(')');
  if (!x->can_trap()) {
    output()->print(" (eliminated)");
  }
}


void InstructionPrinter::do_TypeCast(TypeCast* x) {
  output()->print("type_cast(");
  print_value(x->obj());
  output()->print(") ");
  if (x->declared_type()->is_klass())
    print_klass(x->declared_type()->as_klass());
  else
    output()->print("%s", type2name(x->declared_type()->basic_type()));
}


void InstructionPrinter::do_Invoke(Invoke* x) {
  if (x->receiver() != NULL) {
    print_value(x->receiver());
    output()->print(".");
  }

  output()->print("%s(", Bytecodes::name(x->code()));
  for (int i = 0; i < x->number_of_arguments(); i++) {
    if (i > 0) output()->print(", ");
    print_value(x->argument_at(i));
  }
  output()->print_cr(")");
  fill_to(instr_pos);
  output()->print("%s.%s%s",
             x->target()->holder()->name()->as_utf8(),
             x->target()->name()->as_utf8(),
             x->target()->signature()->as_symbol()->as_utf8());
}


void InstructionPrinter::do_NewInstance(NewInstance* x) {
  output()->print("new instance ");
  print_klass(x->klass());
}


void InstructionPrinter::do_NewTypeArray(NewTypeArray* x) {
  output()->print("new %s array [", basic_type_name(x->elt_type()));
  print_value(x->length());
  output()->put(']');
}


void InstructionPrinter::do_NewObjectArray(NewObjectArray* x) {
  output()->print("new object array [");
  print_value(x->length());
  output()->print("] ");
  print_klass(x->klass());
}


void InstructionPrinter::do_NewMultiArray(NewMultiArray* x) {
  output()->print("new multi array [");
  Values* dims = x->dims();
  for (int i = 0; i < dims->length(); i++) {
    if (i > 0) output()->print(", ");
    print_value(dims->at(i));
  }
  output()->print("] ");
  print_klass(x->klass());
}


void InstructionPrinter::do_MonitorEnter(MonitorEnter* x) {
  output()->print("enter ");
  print_monitor(x);
}


void InstructionPrinter::do_MonitorExit(MonitorExit* x) {
  output()->print("exit ");
  print_monitor(x);
}


void InstructionPrinter::do_Intrinsic(Intrinsic* x) {
  const char* name = vmIntrinsics::name_at(x->id());
  if (name[0] == '_')  name++;  // strip leading bug from _hashCode, etc.
  const char* kname = vmSymbols::name_for(vmIntrinsics::class_for(x->id()));
  if (strchr(name, '_') == NULL) {
    kname = NULL;
  } else {
    const char* kptr = strrchr(kname, '/');
    if (kptr != NULL)  kname = kptr + 1;
  }
  if (kname == NULL)
    output()->print("%s(", name);
  else
    output()->print("%s.%s(", kname, name);
  for (int i = 0; i < x->number_of_arguments(); i++) {
    if (i > 0) output()->print(", ");
    print_value(x->argument_at(i));
  }
  output()->put(')');
}


void InstructionPrinter::do_BlockBegin(BlockBegin* x) {
  // print block id
  BlockEnd* end = x->end();
  output()->print("B%d ", x->block_id());

  // print flags
  bool printed_flag = false;
  if (x->is_set(BlockBegin::std_entry_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("S"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::osr_entry_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("O"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::exception_entry_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("E"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::subroutine_entry_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("s"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::parser_loop_header_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("LH"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::backward_branch_target_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("b"); printed_flag = true;
  }
  if (x->is_set(BlockBegin::was_visited_flag)) {
    if (!printed_flag) output()->print("(");
    output()->print("V"); printed_flag = true;
  }
  if (printed_flag) output()->print(") ");

  // print block bci range
  output()->print("[%d, %d]", x->bci(), (end == NULL ? -1 : end->printable_bci()));

  // print block successors
  if (end != NULL && end->number_of_sux() > 0) {
    output()->print(" ->");
    for (int i = 0; i < end->number_of_sux(); i++) {
      output()->print(" B%d", end->sux_at(i)->block_id());
    }
  }
  // print exception handlers
  if (x->number_of_exception_handlers() > 0) {
    output()->print(" (xhandlers ");
    for (int i = 0; i < x->number_of_exception_handlers();  i++) {
      if (i > 0) output()->print(" ");
      output()->print("B%d", x->exception_handler_at(i)->block_id());
    }
    output()->put(')');
  }

  // print dominator block
  if (x->dominator() != NULL) {
    output()->print(" dom B%d", x->dominator()->block_id());
  }

  // print predecessors and successors
  if (x->successors()->length() > 0) {
    output()->print(" sux:");
    for (int i = 0; i < x->successors()->length(); i ++) {
      output()->print(" B%d", x->successors()->at(i)->block_id());
    }
  }

  if (x->number_of_preds() > 0) {
    output()->print(" pred:");
    for (int i = 0; i < x->number_of_preds(); i ++) {
      output()->print(" B%d", x->pred_at(i)->block_id());
    }
  }

  if (!_print_phis) {
    return;
  }

  // print phi functions
  bool has_phis_in_locals = false;
  bool has_phis_on_stack = false;

  if (x->end() && x->end()->state()) {
    ValueStack* state = x->state();

    int i = 0;
    while (!has_phis_on_stack && i < state->stack_size()) {
      Value v = state->stack_at_inc(i);
      has_phis_on_stack = is_phi_of_block(v, x);
    }

    do {
      for (i = 0; !has_phis_in_locals && i < state->locals_size();) {
        Value v = state->local_at(i);
        has_phis_in_locals = is_phi_of_block(v, x);
        // also ignore illegal HiWords
        if (v && !v->type()->is_illegal()) i += v->type()->size(); else i ++;
      }
      state = state->caller_state();
    } while (state != NULL);

  }

  // print values in locals
  if (has_phis_in_locals) {
    output()->cr(); output()->print_cr("Locals:");

    ValueStack* state = x->state();
    do {
      for (int i = 0; i < state->locals_size();) {
        Value v = state->local_at(i);
        if (v) {
          print_phi(i, v, x); output()->cr();
          // also ignore illegal HiWords
          i += (v->type()->is_illegal() ? 1 : v->type()->size());
        } else {
          i ++;
        }
      }
      output()->cr();
      state = state->caller_state();
    } while (state != NULL);
  }

  // print values on stack
  if (has_phis_on_stack) {
    output()->print_cr("Stack:");
    int i = 0;
    while (i < x->state()->stack_size()) {
      int o = i;
      Value v = x->state()->stack_at_inc(i);
      if (v) {
        print_phi(o, v, x); output()->cr();
      }
    }
  }
}


void InstructionPrinter::do_CheckCast(CheckCast* x) {
  output()->print("checkcast(");
  print_value(x->obj());
  output()->print(") ");
  print_klass(x->klass());
}


void InstructionPrinter::do_InstanceOf(InstanceOf* x) {
  output()->print("instanceof(");
  print_value(x->obj());
  output()->print(") ");
  print_klass(x->klass());
}


void InstructionPrinter::do_Goto(Goto* x) {
  output()->print("goto B%d", x->default_sux()->block_id());
  if (x->is_safepoint()) output()->print(" (safepoint)");
}


void InstructionPrinter::do_If(If* x) {
  output()->print("if ");
  print_value(x->x());
  output()->print(" %s ", cond_name(x->cond()));
  print_value(x->y());
  output()->print(" then B%d else B%d", x->sux_at(0)->block_id(), x->sux_at(1)->block_id());
  if (x->is_safepoint()) output()->print(" (safepoint)");
}


void InstructionPrinter::do_TableSwitch(TableSwitch* x) {
  output()->print("tableswitch ");
  if (x->is_safepoint()) output()->print("(safepoint) ");
  print_value(x->tag());
  output()->cr();
  int l = x->length();
  for (int i = 0; i < l; i++) {
    fill_to(instr_pos);
    output()->print_cr("case %5d: B%d", x->lo_key() + i, x->sux_at(i)->block_id());
  }
  fill_to(instr_pos);
  output()->print("default   : B%d", x->default_sux()->block_id());
}


void InstructionPrinter::do_LookupSwitch(LookupSwitch* x) {
  output()->print("lookupswitch ");
  if (x->is_safepoint()) output()->print("(safepoint) ");
  print_value(x->tag());
  output()->cr();
  int l = x->length();
  for (int i = 0; i < l; i++) {
    fill_to(instr_pos);
    output()->print_cr("case %5d: B%d", x->key_at(i), x->sux_at(i)->block_id());
  }
  fill_to(instr_pos);
  output()->print("default   : B%d", x->default_sux()->block_id());
}


void InstructionPrinter::do_Return(Return* x) {
  if (x->result() == NULL) {
    output()->print("return");
  } else {
    output()->print("%creturn ", x->type()->tchar());
    print_value(x->result());
  }
}


void InstructionPrinter::do_Throw(Throw* x) {
  output()->print("throw ");
  print_value(x->exception());
}


void InstructionPrinter::do_Base(Base* x) {
  output()->print("std entry B%d", x->std_entry()->block_id());
  if (x->number_of_sux() > 1) {
    output()->print(" osr entry B%d", x->osr_entry()->block_id());
  }
}


void InstructionPrinter::do_OsrEntry(OsrEntry* x) {
  output()->print("osr entry");
}


void InstructionPrinter::do_ExceptionObject(ExceptionObject* x) {
  output()->print("incoming exception");
}


void InstructionPrinter::do_RoundFP(RoundFP* x) {
  output()->print("round_fp ");
  print_value(x->input());
}

void InstructionPrinter::do_UnsafeGet(UnsafeGet* x) {
  print_unsafe_op(x, x->is_raw() ? "UnsafeGet (raw)" : "UnsafeGet");
  output()->put(')');
}

void InstructionPrinter::do_UnsafePut(UnsafePut* x) {
  print_unsafe_op(x, "UnsafePut");
  output()->print(", value ");
  print_value(x->value());
  output()->put(')');
}

void InstructionPrinter::do_UnsafeGetAndSet(UnsafeGetAndSet* x) {
  print_unsafe_op(x, x->is_add()?"UnsafeGetAndSet (add)":"UnsafeGetAndSet");
  output()->print(", value ");
  print_value(x->value());
  output()->put(')');
}

void InstructionPrinter::do_RangeCheckPredicate(RangeCheckPredicate* x) {

  if (x->x() != NULL && x->y() != NULL) {
    output()->print("if ");
    print_value(x->x());
    output()->print(" %s ", cond_name(x->cond()));
    print_value(x->y());
    output()->print(" then deoptimize!");
  } else {
    output()->print("always deoptimize!");
  }
}

#ifdef ASSERT
void InstructionPrinter::do_Assert(Assert* x) {
  output()->print("assert ");
  print_value(x->x());
  output()->print(" %s ", cond_name(x->cond()));
  print_value(x->y());
}
#endif

void InstructionPrinter::do_ProfileCall(ProfileCall* x) {
  output()->print("profile ");
  print_value(x->recv());
  output()->print(" %s.%s", x->method()->holder()->name()->as_utf8(), x->method()->name()->as_utf8());
  if (x->known_holder() != NULL) {
    output()->print(", ");
    print_klass(x->known_holder());
    output()->print(" ");
  }
  for (int i = 0; i < x->nb_profiled_args(); i++) {
    if (i > 0) output()->print(", ");
    print_value(x->profiled_arg_at(i));
    if (x->arg_needs_null_check(i)) {
      output()->print(" [NC]");
    }
  }
  output()->put(')');
}

void InstructionPrinter::do_ProfileReturnType(ProfileReturnType* x) {
  output()->print("profile ret type ");
  print_value(x->ret());
  output()->print(" %s.%s", x->method()->holder()->name()->as_utf8(), x->method()->name()->as_utf8());
  output()->put(')');
}
void InstructionPrinter::do_ProfileInvoke(ProfileInvoke* x) {
  output()->print("profile_invoke ");
  output()->print(" %s.%s", x->inlinee()->holder()->name()->as_utf8(), x->inlinee()->name()->as_utf8());
  output()->put(')');

}

void InstructionPrinter::do_RuntimeCall(RuntimeCall* x) {
  output()->print("call_rt %s(", x->entry_name());
  for (int i = 0; i < x->number_of_arguments(); i++) {
    if (i > 0) output()->print(", ");
    print_value(x->argument_at(i));
  }
  output()->put(')');
}

void InstructionPrinter::do_MemBar(MemBar* x) {
  LIR_Code code = x->code();
  switch (code) {
  case lir_membar_acquire   : output()->print("membar_acquire"); break;
  case lir_membar_release   : output()->print("membar_release"); break;
  case lir_membar           : output()->print("membar"); break;
  case lir_membar_loadload  : output()->print("membar_loadload"); break;
  case lir_membar_storestore: output()->print("membar_storestore"); break;
  case lir_membar_loadstore : output()->print("membar_loadstore"); break;
  case lir_membar_storeload : output()->print("membar_storeload"); break;
  default                   : ShouldNotReachHere(); break;
  }
}

#endif // PRODUCT
