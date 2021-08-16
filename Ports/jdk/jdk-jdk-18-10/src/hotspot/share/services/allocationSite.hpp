/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_ALLOCATIONSITE_HPP
#define SHARE_SERVICES_ALLOCATIONSITE_HPP

#include "memory/allocation.hpp"
#include "utilities/nativeCallStack.hpp"

// Allocation site represents a code path that makes a memory
// allocation
class AllocationSite {
 private:
  const NativeCallStack  _call_stack;
  const MEMFLAGS         _flag;
 public:
  AllocationSite(const NativeCallStack& stack, MEMFLAGS flag) : _call_stack(stack), _flag(flag) { }

  bool equals(const NativeCallStack& stack) const {
    return _call_stack.equals(stack);
  }

  bool equals(const AllocationSite& other) const {
    return other.equals(_call_stack);
  }

  const NativeCallStack* call_stack() const {
    return &_call_stack;
  }

  MEMFLAGS flag() const { return _flag; }
};

#endif // SHARE_SERVICES_ALLOCATIONSITE_HPP
