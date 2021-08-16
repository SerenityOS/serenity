/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIUTILITIES_HPP
#define SHARE_CI_CIUTILITIES_HPP

#include "ci/ciEnv.hpp"
#include "gc/shared/cardTable.hpp"
#include "utilities/globalDefinitions.hpp"

// The following routines and definitions are used internally in the
// compiler interface.

#define CURRENT_ENV                         \
  ciEnv::current()

// where current thread is THREAD
#define CURRENT_THREAD_ENV                  \
  ciEnv::current(thread)

#define IS_IN_VM                            \
  ciEnv::is_in_vm()

#define ASSERT_IN_VM                        \
  assert(IS_IN_VM, "must be in vm state");

inline const char* bool_to_str(bool b) {
  return ((b) ? "true" : "false");
}

const char* basictype_to_str(BasicType t);

CardTable::CardValue* ci_card_table_address();
template <typename T> T ci_card_table_address_as() {
  return reinterpret_cast<T>(ci_card_table_address());
}

#endif // SHARE_CI_CIUTILITIES_HPP
