/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciSymbol.hpp"
#include "ci/ciSymbols.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/oopFactory.hpp"
#include "prims/methodHandles.hpp"

// ------------------------------------------------------------------
// ciSymbol::ciSymbol
ciSymbol::ciSymbol(Symbol* s, vmSymbolID sid)
  : _symbol(s), _sid(sid)
{
  assert(_symbol != NULL, "adding null symbol");
  _symbol->increment_refcount();  // increment ref count
  assert(sid_ok(), "sid must be consistent with vmSymbols");
}

DEBUG_ONLY(bool ciSymbol::sid_ok() { return vmSymbols::find_sid(get_symbol()) == _sid; })

// ciSymbol
//
// This class represents a Symbol* in the HotSpot virtual
// machine.

// ------------------------------------------------------------------
// ciSymbol::as_utf8
//
// The text of the symbol as a null-terminated C string.
const char* ciSymbol::as_utf8() {
  GUARDED_VM_QUICK_ENTRY(return get_symbol()->as_utf8();)
}

// The text of the symbol as a null-terminated C string.
const char* ciSymbol::as_quoted_ascii() {
  GUARDED_VM_QUICK_ENTRY(return get_symbol()->as_quoted_ascii();)
}

// ------------------------------------------------------------------
// ciSymbol::base
const u1* ciSymbol::base() {
  GUARDED_VM_ENTRY(return get_symbol()->base();)
}

// ------------------------------------------------------------------
// ciSymbol::char_at
char ciSymbol::char_at(int i) {
  GUARDED_VM_ENTRY(return get_symbol()->char_at(i);)
}

// ------------------------------------------------------------------
// ciSymbol::starts_with
//
// Tests if the symbol starts with the given prefix.
bool ciSymbol::starts_with(const char* prefix, int len) const {
  GUARDED_VM_ENTRY(return get_symbol()->starts_with(prefix, len);)
}

bool ciSymbol::is_signature_polymorphic_name()  const {
  GUARDED_VM_ENTRY(return MethodHandles::is_signature_polymorphic_name(get_symbol());)
}

// ------------------------------------------------------------------
// ciSymbol::index_of
//
// Determines where the symbol contains the given substring.
int ciSymbol::index_of_at(int i, const char* str, int len) const {
  GUARDED_VM_ENTRY(return get_symbol()->index_of_at(i, str, len);)
}

// ------------------------------------------------------------------
// ciSymbol::utf8_length
int ciSymbol::utf8_length() {
  GUARDED_VM_ENTRY(return get_symbol()->utf8_length();)
}

// ------------------------------------------------------------------
// ciSymbol::print_impl
//
// Implementation of the print method
void ciSymbol::print_impl(outputStream* st) {
  st->print(" value=");
  print_symbol_on(st);
}

// ------------------------------------------------------------------
// ciSymbol::print_symbol_on
//
// Print the value of this symbol on an outputStream
void ciSymbol::print_symbol_on(outputStream *st) {
  GUARDED_VM_ENTRY(get_symbol()->print_symbol_on(st);)
}

const char* ciSymbol::as_klass_external_name() const {
  GUARDED_VM_ENTRY(return get_symbol()->as_klass_external_name(););
}

// ------------------------------------------------------------------
// ciSymbol::make_impl
//
// Make a ciSymbol from a C string (implementation).
ciSymbol* ciSymbol::make_impl(const char* s) {
  EXCEPTION_CONTEXT;
  TempNewSymbol sym = SymbolTable::new_symbol(s);
  return CURRENT_THREAD_ENV->get_symbol(sym);
}

// ------------------------------------------------------------------
// ciSymbol::make
//
// Make a ciSymbol from a C string.
ciSymbol* ciSymbol::make(const char* s) {
  GUARDED_VM_ENTRY(return make_impl(s);)
}
