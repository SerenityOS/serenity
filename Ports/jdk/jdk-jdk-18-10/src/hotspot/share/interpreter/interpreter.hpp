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

#ifndef SHARE_INTERPRETER_INTERPRETER_HPP
#define SHARE_INTERPRETER_INTERPRETER_HPP

#include "code/stubs.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreter.hpp"
#include "interpreter/zero/zeroInterpreter.hpp"
#include "memory/resourceArea.hpp"
#include "utilities/align.hpp"

// This file contains the platform-independent parts
// of the interpreter and the interpreter generator.

class InterpreterMacroAssembler;

//------------------------------------------------------------------------------------------------------------------------
// An InterpreterCodelet is a piece of interpreter code. All
// interpreter code is generated into little codelets which
// contain extra information for debugging and printing purposes.

class InterpreterCodelet: public Stub {
  friend class VMStructs;
  friend class CodeCacheDumper; // possible extension [do not remove]
 private:
  int         _size;                             // the size in bytes
  const char* _description;                      // a description of the codelet, for debugging & printing
  Bytecodes::Code _bytecode;                     // associated bytecode if any
  NOT_PRODUCT(CodeStrings _strings;)              // Comments for annotating assembler output.

 public:
  // Initialization/finalization
  void    initialize(int size,
                     CodeStrings& strings)       { _size = size;
                                                   NOT_PRODUCT(_strings = CodeStrings();)
                                                   NOT_PRODUCT(_strings.copy(strings);) }
  void    finalize()                             { ShouldNotCallThis(); }

  // General info/converters
  int     size() const                           { return _size; }
  static  int code_size_to_size(int code_size)   { return align_up((int)sizeof(InterpreterCodelet), CodeEntryAlignment) + code_size; }

  // Code info
  address code_begin() const                     { return (address)this + align_up(sizeof(InterpreterCodelet), CodeEntryAlignment); }
  address code_end() const                       { return (address)this + size(); }

  // Debugging
  void    verify();
  void    print_on(outputStream* st) const;
  void    print() const;

  // Interpreter-specific initialization
  void    initialize(const char* description, Bytecodes::Code bytecode);

  // Interpreter-specific attributes
  int         code_size() const                  { return code_end() - code_begin(); }
  const char* description() const                { return _description; }
  Bytecodes::Code bytecode() const               { return _bytecode; }
};

// Define a prototype interface
DEF_STUB_INTERFACE(InterpreterCodelet);


//------------------------------------------------------------------------------------------------------------------------
// A CodeletMark serves as an automatic creator/initializer for Codelets
// (As a subclass of ResourceMark it automatically GC's the allocated
// code buffer and assemblers).

class CodeletMark: ResourceMark {
 private:
  InterpreterCodelet*         _clet;
  InterpreterMacroAssembler** _masm;
  CodeBuffer                  _cb;

  int codelet_size() {
    // Request the whole code buffer (minus a little for alignment).
    // The commit call below trims it back for each codelet.
    int codelet_size = AbstractInterpreter::code()->available_space() - 2*K;

    // Guarantee there's a little bit of code space left.
    guarantee(codelet_size > 0 && (size_t)codelet_size > 2*K,
              "not enough space for interpreter generation");

    return codelet_size;
  }

 public:
  CodeletMark(InterpreterMacroAssembler*& masm,
              const char* description,
              Bytecodes::Code bytecode = Bytecodes::_illegal);
  ~CodeletMark();
};

// Wrapper typedef to use the name Interpreter to mean either
// the Zero interpreter or the template interpreter.

typedef ZERO_ONLY(ZeroInterpreter) NOT_ZERO(TemplateInterpreter) Interpreter;

#endif // SHARE_INTERPRETER_INTERPRETER_HPP
