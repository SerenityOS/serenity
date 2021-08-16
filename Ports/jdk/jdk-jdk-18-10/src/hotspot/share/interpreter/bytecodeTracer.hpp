/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_BYTECODETRACER_HPP
#define SHARE_INTERPRETER_BYTECODETRACER_HPP

#include "memory/allocation.hpp"
#include "utilities/ostream.hpp"

// The BytecodeTracer is a helper class used by the interpreter for run-time
// bytecode tracing. If bytecode tracing is turned on, trace() will be called
// for each bytecode.
//
// By specialising the BytecodeClosure, all kinds of bytecode traces can
// be done.

// class BytecodeTracer is used by TraceBytecodes option and PrintMethodData

class methodHandle;

class BytecodeClosure;
class BytecodeTracer: AllStatic {
 private:
  static BytecodeClosure* _closure;

 public:
  static BytecodeClosure* std_closure();                        // a printing closure
  static BytecodeClosure* closure()                                                   { return _closure; }
  static void             set_closure(BytecodeClosure* closure) { _closure = closure; }

  static void             trace(const methodHandle& method, address bcp, uintptr_t tos, uintptr_t tos2, outputStream* st = tty);
  static void             trace(const methodHandle& method, address bcp, outputStream* st = tty);
};


// For each bytecode, a BytecodeClosure's trace() routine will be called.

class BytecodeClosure {
 public:
  virtual void trace(const methodHandle& method, address bcp, uintptr_t tos, uintptr_t tos2, outputStream* st) = 0;
  virtual void trace(const methodHandle& method, address bcp, outputStream* st) = 0;
};

#endif // SHARE_INTERPRETER_BYTECODETRACER_HPP
