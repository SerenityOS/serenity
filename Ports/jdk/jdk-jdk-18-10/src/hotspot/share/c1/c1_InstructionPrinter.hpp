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

#ifndef SHARE_C1_C1_INSTRUCTIONPRINTER_HPP
#define SHARE_C1_C1_INSTRUCTIONPRINTER_HPP

#include "c1/c1_IR.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_Runtime1.hpp"

#ifndef PRODUCT
class InstructionPrinter: public InstructionVisitor {
 private:
  outputStream* _output;
  bool _print_phis;

  enum LayoutConstants {
    bci_pos   =  2,
    use_pos   =  7,
    temp_pos  = 12,
    instr_pos = 19,
    end_pos   = 60
  };

  bool is_illegal_phi(Value v);

 public:
  InstructionPrinter(bool print_phis = true, outputStream* output = tty)
    : _output(output)
    , _print_phis(print_phis)
  {}

  outputStream* output() { return _output; }

  // helpers
  static const char* basic_type_name(BasicType type);
  static const char* cond_name(If::Condition cond);
  static const char* op_name(Bytecodes::Code op);
  bool is_phi_of_block(Value v, BlockBegin* b);

  // type-specific print functions
  void print_klass(ciKlass* klass);
  void print_object(Value obj);

  // generic print functions
  void print_temp(Value value);
  void print_field(AccessField* field);
  void print_indexed(AccessIndexed* indexed);
  void print_monitor(AccessMonitor* monitor);
  void print_op2(Op2* instr);
  void print_value(Value value);
  void print_instr(Instruction* instr);
  void print_stack(ValueStack* stack);
  void print_inline_level(BlockBegin* block);
  void print_unsafe_op(UnsafeOp* op, const char* name);
  void print_phi(int i, Value v, BlockBegin* b);
  void print_alias(Value v);

  // line printing of instructions
  void fill_to(int pos, char filler = ' ');
  void print_head();
  void print_line(Instruction* instr);

  // visitor functionality
  virtual void do_Phi            (Phi*             x);
  virtual void do_Local          (Local*           x);
  virtual void do_Constant       (Constant*        x);
  virtual void do_LoadField      (LoadField*       x);
  virtual void do_StoreField     (StoreField*      x);
  virtual void do_ArrayLength    (ArrayLength*     x);
  virtual void do_LoadIndexed    (LoadIndexed*     x);
  virtual void do_StoreIndexed   (StoreIndexed*    x);
  virtual void do_NegateOp       (NegateOp*        x);
  virtual void do_ArithmeticOp   (ArithmeticOp*    x);
  virtual void do_ShiftOp        (ShiftOp*         x);
  virtual void do_LogicOp        (LogicOp*         x);
  virtual void do_CompareOp      (CompareOp*       x);
  virtual void do_IfOp           (IfOp*            x);
  virtual void do_Convert        (Convert*         x);
  virtual void do_NullCheck      (NullCheck*       x);
  virtual void do_TypeCast       (TypeCast*        x);
  virtual void do_Invoke         (Invoke*          x);
  virtual void do_NewInstance    (NewInstance*     x);
  virtual void do_NewTypeArray   (NewTypeArray*    x);
  virtual void do_NewObjectArray (NewObjectArray*  x);
  virtual void do_NewMultiArray  (NewMultiArray*   x);
  virtual void do_CheckCast      (CheckCast*       x);
  virtual void do_InstanceOf     (InstanceOf*      x);
  virtual void do_MonitorEnter   (MonitorEnter*    x);
  virtual void do_MonitorExit    (MonitorExit*     x);
  virtual void do_Intrinsic      (Intrinsic*       x);
  virtual void do_BlockBegin     (BlockBegin*      x);
  virtual void do_Goto           (Goto*            x);
  virtual void do_If             (If*              x);
  virtual void do_TableSwitch    (TableSwitch*     x);
  virtual void do_LookupSwitch   (LookupSwitch*    x);
  virtual void do_Return         (Return*          x);
  virtual void do_Throw          (Throw*           x);
  virtual void do_Base           (Base*            x);
  virtual void do_OsrEntry       (OsrEntry*        x);
  virtual void do_ExceptionObject(ExceptionObject* x);
  virtual void do_RoundFP        (RoundFP*         x);
  virtual void do_UnsafeGet      (UnsafeGet*       x);
  virtual void do_UnsafePut      (UnsafePut*       x);
  virtual void do_UnsafeGetAndSet(UnsafeGetAndSet* x);
  virtual void do_ProfileCall    (ProfileCall*     x);
  virtual void do_ProfileReturnType (ProfileReturnType*  x);
  virtual void do_ProfileInvoke  (ProfileInvoke*   x);
  virtual void do_RuntimeCall    (RuntimeCall*     x);
  virtual void do_MemBar         (MemBar*          x);
  virtual void do_RangeCheckPredicate(RangeCheckPredicate* x);
#ifdef ASSERT
  virtual void do_Assert         (Assert*          x);
#endif
};
#endif // PRODUCT

#endif // SHARE_C1_C1_INSTRUCTIONPRINTER_HPP
