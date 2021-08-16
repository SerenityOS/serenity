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

#ifndef SHARE_CI_CISYMBOL_HPP
#define SHARE_CI_CISYMBOL_HPP

#include "ci/ciBaseObject.hpp"
#include "ci/ciObject.hpp"
#include "oops/symbol.hpp"
#include "utilities/vmEnums.hpp"

// ciSymbol
//
// This class represents a Symbol* in the HotSpot virtual
// machine.
class ciSymbol : public ciBaseObject {
  Symbol* _symbol;

  CI_PACKAGE_ACCESS
  // These friends all make direct use of get_symbol:
  friend class ciEnv;
  friend class ciInstanceKlass;
  friend class ciSignature;
  friend class ciMethod;
  friend class ciField;
  friend class ciObjArrayKlass;

private:
  const vmSymbolID _sid;

  ciSymbol(Symbol* s, vmSymbolID sid);

  DEBUG_ONLY(bool sid_ok();)

  Symbol* get_symbol() const { return _symbol; }

  const char* type_string() { return "ciSymbol"; }

  void print_impl(outputStream* st);

  // This is public in Symbol* but private here, because the base can move:
  const u1* base();

  // Make a ciSymbol from a C string (implementation).
  static ciSymbol* make_impl(const char* s);

public:
  // The enumeration ID from vmSymbols, or vmSymbolID::NO_SID if none.
  vmSymbolID sid() const { return _sid; }

  // The text of the symbol as a null-terminated utf8 string.
  const char* as_utf8();
  int         utf8_length();

  // The text of the symbol as ascii with all non-printable characters quoted as \u####
  const char* as_quoted_ascii();

  // Return the i-th utf byte as a char, where i < utf8_length
  char        char_at(int i);

  // Tests if the symbol starts with the given prefix.
  bool starts_with(const char* prefix, int len) const;

  // Determines where the symbol contains the given substring.
  int index_of_at(int i, const char* str, int len) const;

  void print_symbol_on(outputStream* st);
  void print_symbol() {
    print_symbol_on(tty);
  }
  const char* as_klass_external_name() const;

  // Make a ciSymbol from a C string.
  // Consider adding to vmSymbols.hpp instead of using this constructor.
  // (Your code will be less subject to typographical bugs.)
  static ciSymbol* make(const char* s);

  void print() {
    _symbol->print();
  }

  virtual bool is_symbol() const       { return true; }

  // Are two ciSymbols equal?
  bool equals(ciSymbol* obj) { return this->_symbol == obj->get_symbol(); }

  bool is_signature_polymorphic_name() const;
};

#endif // SHARE_CI_CISYMBOL_HPP
