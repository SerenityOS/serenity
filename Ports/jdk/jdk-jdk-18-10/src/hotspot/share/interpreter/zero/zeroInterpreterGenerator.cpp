/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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
#include "asm/assembler.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/zero/bytecodeInterpreter.hpp"
#include "interpreter/zero/zeroInterpreter.hpp"
#include "oops/method.hpp"
#include "zeroInterpreterGenerator.hpp"

ZeroInterpreterGenerator::ZeroInterpreterGenerator(StubQueue* _code): AbstractInterpreterGenerator(_code) {
  generate_all();
}

void ZeroInterpreterGenerator::generate_all() {
  { CodeletMark cm(_masm, "slow signature handler");
    AbstractInterpreter::_slow_signature_handler = generate_slow_signature_handler();
  }

#define method_entry(kind) Interpreter::_entry_table[Interpreter::kind] = generate_method_entry(Interpreter::kind)

  { CodeletMark cm(_masm, "(kind = frame_manager)");
    // all non-native method kinds
    method_entry(zerolocals);
    method_entry(zerolocals_synchronized);
    method_entry(empty);
    method_entry(getter);
    method_entry(setter);
    method_entry(abstract);
    method_entry(java_lang_math_sin   );
    method_entry(java_lang_math_cos   );
    method_entry(java_lang_math_tan   );
    method_entry(java_lang_math_abs   );
    method_entry(java_lang_math_sqrt  );
    method_entry(java_lang_math_log   );
    method_entry(java_lang_math_log10 );
    method_entry(java_lang_math_pow );
    method_entry(java_lang_math_exp );
    method_entry(java_lang_math_fmaD );
    method_entry(java_lang_math_fmaF );
    method_entry(java_lang_ref_reference_get);

    AbstractInterpreter::initialize_method_handle_entries();

    Interpreter::_native_entry_begin = Interpreter::code()->code_end();
    method_entry(native);
    method_entry(native_synchronized);
    Interpreter::_native_entry_end = Interpreter::code()->code_end();
  }

#undef method_entry
}

// Generate method entries
address ZeroInterpreterGenerator::generate_method_entry(
                                        AbstractInterpreter::MethodKind kind) {
  // determine code generation flags
  bool native = false;
  bool synchronized = false;
  address entry_point = NULL;

  switch (kind) {
  case Interpreter::zerolocals             :                                          break;
  case Interpreter::zerolocals_synchronized:                synchronized = true;      break;
  case Interpreter::native                 : native = true;                           break;
  case Interpreter::native_synchronized    : native = true; synchronized = true;      break;
  case Interpreter::empty                  : entry_point = generate_empty_entry();    break;
  case Interpreter::getter                 : entry_point = generate_getter_entry();   break;
  case Interpreter::setter                 : entry_point = generate_setter_entry();   break;
  case Interpreter::abstract               : entry_point = generate_abstract_entry(); break;

  case Interpreter::java_lang_math_sin     : // fall thru
  case Interpreter::java_lang_math_cos     : // fall thru
  case Interpreter::java_lang_math_tan     : // fall thru
  case Interpreter::java_lang_math_abs     : // fall thru
  case Interpreter::java_lang_math_log     : // fall thru
  case Interpreter::java_lang_math_log10   : // fall thru
  case Interpreter::java_lang_math_sqrt    : // fall thru
  case Interpreter::java_lang_math_pow     : // fall thru
  case Interpreter::java_lang_math_exp     : // fall thru
  case Interpreter::java_lang_math_fmaD    : // fall thru
  case Interpreter::java_lang_math_fmaF    : entry_point = generate_math_entry(kind);      break;
  case Interpreter::java_lang_ref_reference_get
                                           : entry_point = generate_Reference_get_entry(); break;
  default:
    fatal("unexpected method kind: %d", kind);
    break;
  }

  if (entry_point) {
    return entry_point;
  }

  // We expect the normal and native entry points to be generated first so we can reuse them.
  if (native) {
    entry_point = Interpreter::entry_for_kind(synchronized ? Interpreter::native_synchronized : Interpreter::native);
    if (entry_point == NULL) {
      entry_point = generate_native_entry(synchronized);
    }
  } else {
    entry_point = Interpreter::entry_for_kind(synchronized ? Interpreter::zerolocals_synchronized : Interpreter::zerolocals);
    if (entry_point == NULL) {
      entry_point = generate_normal_entry(synchronized);
    }
  }

  return entry_point;
}

address ZeroInterpreterGenerator::generate_slow_signature_handler() {
  _masm->advance(1);
  return (address) InterpreterRuntime::slow_signature_handler;
}

address ZeroInterpreterGenerator::generate_math_entry(
    AbstractInterpreter::MethodKind kind) {
  if (!InlineIntrinsics)
    return NULL;

  Unimplemented();
  return NULL;
}

address ZeroInterpreterGenerator::generate_abstract_entry() {
  return generate_entry((address) ShouldNotCallThisEntry());
}

address ZeroInterpreterGenerator::generate_empty_entry() {
  if (!UseFastEmptyMethods)
    return NULL;

  return generate_entry((address) ZeroInterpreter::empty_entry);
}

address ZeroInterpreterGenerator::generate_getter_entry() {
  if (!UseFastAccessorMethods)
    return NULL;

  return generate_entry((address) ZeroInterpreter::getter_entry);
}

address ZeroInterpreterGenerator::generate_setter_entry() {
  if (!UseFastAccessorMethods)
    return NULL;

  return generate_entry((address) ZeroInterpreter::setter_entry);
}

address ZeroInterpreterGenerator::generate_Reference_get_entry(void) {
  return generate_entry((address) ZeroInterpreter::Reference_get_entry);
}

address ZeroInterpreterGenerator::generate_native_entry(bool synchronized) {
  return generate_entry((address) ZeroInterpreter::native_entry);
}

address ZeroInterpreterGenerator::generate_normal_entry(bool synchronized) {
  return generate_entry((address) ZeroInterpreter::normal_entry);
}
