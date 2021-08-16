/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_GCBEHAVIOURS_HPP
#define SHARE_GC_SHARED_GCBEHAVIOURS_HPP

#include "memory/iterator.hpp"
#include "oops/oopsHierarchy.hpp"

// This is the behaviour for checking if a CompiledMethod is unloading
// or has unloaded due to having phantomly dead oops in it after a GC.
class IsUnloadingBehaviour {
  static IsUnloadingBehaviour* _current;

public:
  virtual bool is_unloading(CompiledMethod* cm) const = 0;
  static IsUnloadingBehaviour* current() { return _current; }
  static void set_current(IsUnloadingBehaviour* current) { _current = current; }
};

class ClosureIsUnloadingBehaviour: public IsUnloadingBehaviour {
  BoolObjectClosure *const _cl;

public:
  ClosureIsUnloadingBehaviour(BoolObjectClosure* is_alive)
    : _cl(is_alive)
  { }

  virtual bool is_unloading(CompiledMethod* cm) const;
};

#endif // SHARE_GC_SHARED_GCBEHAVIOURS_HPP
