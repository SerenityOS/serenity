/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_DISASSEMBLER_HPP
#define SHARE_COMPILER_DISASSEMBLER_HPP

#include "utilities/globalDefinitions.hpp"

#include "asm/assembler.hpp"
#include "code/codeBlob.hpp"
#include "code/nmethod.hpp"
#include "compiler/abstractDisassembler.hpp"
#include "runtime/globals.hpp"
#include "utilities/macros.hpp"

class decode_env;

// The disassembler prints out assembly code annotated
// with Java specific information.

// Disassembler inherits from AbstractDisassembler
class Disassembler : public AbstractDisassembler {
  friend class decode_env;
 private:
  // this is the type of the dll entry point:
  typedef void* (*decode_func_virtual)(uintptr_t start_va, uintptr_t end_va,
                               unsigned char* buffer, uintptr_t length,
                               void* (*event_callback)(void*, const char*, void*),
                               void* event_stream,
                               int (*printf_callback)(void*, const char*, ...),
                               void* printf_stream,
                               const char* options,
                               int newline);
  // points to the library.
  static void*    _library;
  // bailout
  static bool     _tried_to_load_library;
  static bool     _library_usable;
  // points to the decode function.
  static decode_func_virtual _decode_instructions_virtual;

  // tries to load library and return whether it succeeded.
  // Allow (diagnostic) output redirection.
  // No output at all if stream is NULL. Can be overridden
  // with -Verbose flag, in which case output goes to tty.
  static bool load_library(outputStream* st = NULL);
  static void* dll_load(char* buf, int buflen, int offset, char* ebuf, int ebuflen, outputStream* st);

  // Check if the two addresses are on the same page.
  static bool is_same_page(address a1, address a2) {
    return (((uintptr_t)a1 ^ (uintptr_t)a2) & (~0x0fffUL)) == 0L;
  }

  // Machine dependent stuff
#include CPU_HEADER(disassembler)

 public:
  // We can always decode code blobs.
  // Either we have a disassembler library available (successfully loaded)
  // or we will resort to the abstract disassembler. This method informs
  // about which decoding format is used.
  // We can also enforce using the abstract disassembler.
  static bool is_abstract() {
    if (!_tried_to_load_library) {
      load_library();
    }
    return ! _library_usable;
  }

  // Check out if we are doing a live disassembly or a post-mortem
  // disassembly where the binary data was loaded from a hs_err file.
  static bool is_decode_error_file() {
// Activate once post-mortem disassembly (from hs-err file) is available.
#if 0
    return DecodeErrorFile && (strlen(DecodeErrorFile) != 0);
#else
    return false;
#endif
  }

  // Directly disassemble code blob.
  static void decode(CodeBlob *cb,               outputStream* st = NULL);
  // Directly disassemble nmethod.
  static void decode(nmethod* nm,                outputStream* st = NULL);
  // Disassemble an arbitrary memory range.
  static void decode(address start, address end, outputStream* st = NULL, const CodeStrings* = NULL);

  static void _hook(const char* file, int line, class MacroAssembler* masm);

  // This functions makes it easy to generate comments in the generated
  // interpreter code, by riding on the customary __ macro in the interpreter generator.
  // See templateTable_x86.cpp for an example.
  template<class T> inline static T* hook(const char* file, int line, T* masm) {
    if (PrintInterpreter) {
      _hook(file, line, masm);
    }
    return masm;
  }
};

#endif // SHARE_COMPILER_DISASSEMBLER_HPP
