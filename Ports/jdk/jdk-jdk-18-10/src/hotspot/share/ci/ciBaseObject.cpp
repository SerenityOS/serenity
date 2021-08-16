/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciBaseObject.hpp"
#include "ci/ciUtilities.hpp"
#include "gc/shared/collectedHeap.inline.hpp"

// ------------------------------------------------------------------
// ciBaseObject::set_ident
//
// Set the unique identity number of a ciBaseObject.
void ciBaseObject::set_ident(uint id) {
  assert(_ident == 0, "must only initialize once");
  _ident = id;
}

// ------------------------------------------------------------------
// ciBaseObject::ident
//
// Report the unique identity number of a ciBaseObject.
uint ciBaseObject::ident() {
  assert(_ident != 0, "must be initialized");
  return _ident;
}
