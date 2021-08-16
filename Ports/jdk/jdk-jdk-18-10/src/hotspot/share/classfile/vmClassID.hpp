/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_VMCLASSID_HPP
#define SHARE_CLASSFILE_VMCLASSID_HPP

#include "classfile/vmClassMacros.hpp"
#include "utilities/enumIterator.hpp"

enum class vmClassID : int {
  #define DECLARE_VM_CLASS(name, symbol) _VM_CLASS_ENUM(name), _VM_CLASS_ENUM(symbol) = _VM_CLASS_ENUM(name),
  VM_CLASSES_DO(DECLARE_VM_CLASS)
  #undef DECLARE_VM_CLASS

  LIMIT,             // exclusive upper limit
  FIRST = 0,         // inclusive upper limit
  LAST = LIMIT - 1   // inclusive upper limit
};

ENUMERATOR_RANGE(vmClassID, vmClassID::FIRST, vmClassID::LAST) // (inclusive start, inclusive end)

#endif // SHARE_CLASSFILE_VMCLASSID_HPP
