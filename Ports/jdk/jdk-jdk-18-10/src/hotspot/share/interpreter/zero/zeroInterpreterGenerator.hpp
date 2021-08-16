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

#ifndef SHARE_INTERPRETER_CPPINTERPRETERGENERATOR_HPP
#define SHARE_INTERPRETER_CPPINTERPRETERGENERATOR_HPP

// This file contains the platform-independent parts
// of the Zero interpreter generator.

# include "entry_zero.hpp"
// # include "interpreter/interp_masm.hpp"

class ZeroInterpreterGenerator: public AbstractInterpreterGenerator {

 private:
  void generate_all();

  address generate_slow_signature_handler();

  address generate_method_entry(AbstractInterpreter::MethodKind kind);
  address generate_normal_entry(bool synchronized);
  address generate_native_entry(bool synchronized);
  address generate_abstract_entry();
  address generate_math_entry(AbstractInterpreter::MethodKind kind);
  address generate_empty_entry();
  address generate_getter_entry();
  address generate_setter_entry();
  address generate_Reference_get_entry();

 public:
  ZeroInterpreterGenerator(StubQueue* _code);

 protected:
  MacroAssembler* assembler() const {
    return _masm;
  }

 public:
  static address generate_entry_impl(MacroAssembler* masm, address entry_point) {
    ZeroEntry *entry = (ZeroEntry *) masm->pc();
    masm->advance(sizeof(ZeroEntry));
    entry->set_entry_point(entry_point);
    return (address) entry;
  }

 protected:
  address generate_entry(address entry_point) {
    return generate_entry_impl(assembler(), entry_point);
  }
};

#endif // SHARE_INTERPRETER_CPPINTERPRETERGENERATOR_HPP
