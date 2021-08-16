/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/decoder.hpp"
#include "symbolengine.hpp"
#include "windbghelp.hpp"

bool Decoder::decode(address addr, char* buf, int buflen, int* offset, const char* modulepath, bool demangle) {
  return SymbolEngine::decode(addr, buf, buflen, offset, demangle);
}

bool Decoder::decode(address addr, char* buf, int buflen, int* offset, const void* base) {
  return SymbolEngine::decode(addr, buf, buflen, offset, true);
}

bool Decoder::get_source_info(address pc, char* buf, size_t buflen, int* line) {
  return SymbolEngine::get_source_info(pc, buf, buflen, line);
}

bool Decoder::demangle(const char* symbol, char* buf, int buflen) {
  return SymbolEngine::demangle(symbol, buf, buflen);
}

void Decoder::print_state_on(outputStream* st) {
  WindowsDbgHelp::print_state_on(st);
  SymbolEngine::print_state_on(st);
}

