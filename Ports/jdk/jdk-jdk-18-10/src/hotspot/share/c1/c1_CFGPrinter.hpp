/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_CFGPRINTER_HPP
#define SHARE_C1_C1_CFGPRINTER_HPP

#include "c1/c1_Compilation.hpp"
#include "c1/c1_Instruction.hpp"

#ifndef PRODUCT

// This is a utility class used for recording the results of a
// compilation for later analysis.

class CFGPrinterOutput;
class Interval;

typedef GrowableArray<Interval*> IntervalList;

class CFGPrinter : public AllStatic {
public:
  static void print_compilation(Compilation* compilation);
  static void print_cfg(BlockList* blocks, const char* name, bool do_print_HIR, bool do_print_LIR);
  static void print_cfg(IR* blocks, const char* name, bool do_print_HIR, bool do_print_LIR);
  static void print_intervals(IntervalList* intervals, const char* name);
};

class CFGPrinterOutput : public CHeapObj<mtCompiler> {
 private:
  outputStream* _output;

  Compilation*  _compilation;
  bool _do_print_HIR;
  bool _do_print_LIR;

  class PrintBlockClosure: public BlockClosure {
    void block_do(BlockBegin* block) { if (block != NULL) Compilation::current()->cfg_printer_output()->print_block(block); }
  };

  outputStream* output() { assert(_output != NULL, ""); return _output; }

  void inc_indent();
  void dec_indent();
  void print(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void print_begin(const char* tag);
  void print_end(const char* tag);

  char* method_name(ciMethod* method, bool short_name = false);

 public:
  CFGPrinterOutput(Compilation* compilation);

  void set_print_flags(bool do_print_HIR, bool do_print_LIR) { _do_print_HIR = do_print_HIR; _do_print_LIR = do_print_LIR; }

  void print_compilation();
  void print_intervals(IntervalList* intervals, const char* name);

  void print_state(BlockBegin* block);
  void print_operand(Value instr);
  void print_HIR(Value instr);
  void print_HIR(BlockBegin* block);
  void print_LIR(BlockBegin* block);
  void print_block(BlockBegin* block);
  void print_cfg(BlockList* blocks, const char* name);
  void print_cfg(IR* blocks, const char* name);
};

#endif

#endif // SHARE_C1_C1_CFGPRINTER_HPP
