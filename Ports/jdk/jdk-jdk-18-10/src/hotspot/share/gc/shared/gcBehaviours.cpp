/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "code/compiledMethod.hpp"
#include "code/nmethod.hpp"
#include "gc/shared/gcBehaviours.hpp"

IsUnloadingBehaviour* IsUnloadingBehaviour::_current = NULL;

class IsCompiledMethodUnloadingOopClosure: public OopClosure {
  BoolObjectClosure *_cl;
  bool _is_unloading;

public:
  IsCompiledMethodUnloadingOopClosure(BoolObjectClosure* cl)
    : _cl(cl),
      _is_unloading(false)
  { }

  virtual void do_oop(oop* p) {
    if (_is_unloading) {
      return;
    }
    oop obj = *p;
    if (obj == NULL) {
      return;
    }
    if (!_cl->do_object_b(obj)) {
      _is_unloading = true;
    }
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  bool is_unloading() const {
    return _is_unloading;
  }
};

bool ClosureIsUnloadingBehaviour::is_unloading(CompiledMethod* cm) const {
  if (cm->is_nmethod()) {
    IsCompiledMethodUnloadingOopClosure cl(_cl);
    static_cast<nmethod*>(cm)->oops_do(&cl, true /* allow_dead */);
    return cl.is_unloading();
  } else {
    return false;
  }
}
