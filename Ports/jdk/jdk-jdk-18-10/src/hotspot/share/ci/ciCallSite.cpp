/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "ci/ciCallSite.hpp"
#include "ci/ciUtilities.inline.hpp"

// ciCallSite

bool ciCallSite::is_fully_initialized_constant_call_site() {
  if (klass()->is_subclass_of(CURRENT_ENV->ConstantCallSite_klass())) {
    bool is_fully_initialized = _is_fully_initialized_cache;
    if (!is_fully_initialized) { // changes monotonically: false => true
      VM_ENTRY_MARK;
      is_fully_initialized = (java_lang_invoke_ConstantCallSite::is_frozen(get_oop()) != JNI_FALSE);
      _is_fully_initialized_cache = is_fully_initialized; // cache updated value
    }
    return is_fully_initialized;
  } else {
    return false;
  }
}

// ------------------------------------------------------------------
// ciCallSite::print
//
// Print debugging information about the CallSite.
void ciCallSite::print() {
  Unimplemented();
}
