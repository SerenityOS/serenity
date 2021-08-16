/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_ZEROINTERPRETER_HPP
#define SHARE_INTERPRETER_ZEROINTERPRETER_HPP

#include "interpreter/abstractInterpreter.hpp"
#include "utilities/macros.hpp"

#ifdef ZERO

class InterpreterCodelet;

// This file contains the platform-independent parts
// of the c++ interpreter

class ZeroInterpreter: public AbstractInterpreter {
  friend class VMStructs;
 public:
  // Initialization/debugging
  static void       initialize_stub();
  static void       initialize_code();
  // this only returns whether a pc is within generated code for the interpreter.

  // These are moderately dubious interfaces for the c++ interpreter. Only
  // frame code and debug.cpp should be using it.
  static bool       contains(address pc);
  static InterpreterCodelet* codelet_containing(address pc);

 public:


  // No displatch table to switch so no need for these to do anything special
  static void notice_safepoints() {}
  static void ignore_safepoints() {}

  static address    return_entry  (TosState state, int length, Bytecodes::Code code);
  static address    deopt_entry   (TosState state, int length);

  static address    remove_activation_entry() { return (address)-1; }
  static address    remove_activation_early_entry(TosState state);
  static address    remove_activation_preserving_args_entry();

  static void invoke_method(Method* method, address entry_point, TRAPS);
  static void invoke_osr(Method* method,
                         address   entry_point,
                         address   osr_buf,
                         TRAPS);

  static address throw_NullPointerException_entry() { return NULL; }
  static address throw_ArithmeticException_entry()  { return NULL; }
  static address throw_StackOverflowError_entry()   { return NULL; }

# include "zeroInterpreter_zero.hpp"
};

#endif // ZERO

#endif // SHARE_INTERPRETER_ZEROINTERPRETER_HPP
