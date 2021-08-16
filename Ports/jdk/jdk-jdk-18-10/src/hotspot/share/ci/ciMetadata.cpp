/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciObject.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "gc/shared/collectedHeap.inline.hpp"

// Not inlined to preserve visibility of ciMetaData vtable symbol. Required by SA.
bool ciMetadata::is_classless() const { return false; }

// ------------------------------------------------------------------
// ciMetadata::print
//
// Print debugging output about this ciMetadata.
//
// Implementation note: dispatch to the virtual print_impl behavior
// for this ciObject.
void ciMetadata::print(outputStream* st) {
  st->print("<%s", type_string());
  GUARDED_VM_ENTRY(print_impl(st);)
  st->print(" ident=%d address=" INTPTR_FORMAT ">", ident(), p2i((address)this));
}


// ------------------------------------------------------------------
// ciMetadata::print_oop
//
// Print debugging output about the metadata this ciMetadata represents.
void ciMetadata::print_metadata(outputStream* st) {
  if (!is_loaded()) {
    st->print_cr("UNLOADED");
  } else {
    GUARDED_VM_ENTRY(_metadata->print_on(st);)
  }
}
