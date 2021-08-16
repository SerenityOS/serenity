/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CISYMBOLS_HPP
#define SHARE_CI_CISYMBOLS_HPP

#include "ci/ciObjectFactory.hpp"
#include "ci/ciSymbol.hpp"
#include "classfile/vmSymbols.hpp"
#include "oops/symbol.hpp"

class ciSymbols {
 public:
#define CI_SYMBOL_DECLARE(name, ignore_def) \
  static ciSymbol* name() { return ciObjectFactory::vm_symbol_at(VM_SYMBOL_ENUM_NAME(name)); }

  VM_SYMBOLS_DO(CI_SYMBOL_DECLARE, CI_SYMBOL_DECLARE)
#undef CI_SYMBOL_DECLARE

};

#endif // SHARE_CI_CISYMBOLS_HPP