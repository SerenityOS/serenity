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
#include "ci/ciFlags.hpp"

// ciFlags
//
// This class represents klass or method flags

// ------------------------------------------------------------------
// ciFlags::print_klass_flags
void ciFlags::print_klass_flags(outputStream* st) {
  if (is_public()) {
    st->print("public");
  } else {
    st->print("DEFAULT_ACCESS");
  }

  if (is_final()) {
    st->print(",final");
  }
  if (is_super()) {
    st->print(",super");
  }
  if (is_interface()) {
    st->print(",interface");
  }
  if (is_abstract()) {
    st->print(",abstract");
  }
}

// ------------------------------------------------------------------
// ciFlags::print_member_flags
void ciFlags::print_member_flags(outputStream* st) {
  if (is_public()) {
    st->print("public");
  } else if (is_private()) {
    st->print("private");
  } else if (is_protected()) {
    st->print("protected");
  } else {
    st->print("DEFAULT_ACCESS");
  }

  if (is_static()) {
    st->print(",static");
  }
  if (is_final()) {
    st->print(",final");
  }
  if (is_synchronized()) {
    st->print(",synchronized");
  }
  if (is_volatile()) {
    st->print(",volatile");
  }
  if (is_transient()) {
    st->print(",transient");
  }
  if (is_native()) {
    st->print(",native");
  }
  if (is_abstract()) {
    st->print(",abstract");
  }

}

// ------------------------------------------------------------------
// ciFlags::print
void ciFlags::print(outputStream* st) {
  st->print(" flags=%x", _flags);
}
