/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIBASEOBJECT_HPP
#define SHARE_CI_CIBASEOBJECT_HPP

#include "ci/ciClassList.hpp"
#include "memory/allocation.hpp"

// ciBaseObject
//
// This class represents an oop in the HotSpot virtual machine.
// Its subclasses are structured in a hierarchy which mirrors
// an aggregate of the VM's oop and klass hierarchies (see
// oopHierarchy.hpp).  Each instance of ciBaseObject holds a handle
// to a corresponding oop on the VM side and provides routines
// for accessing the information in its oop.  By using the ciBaseObject
// hierarchy for accessing oops in the VM, the compiler ensures
// that it is safe with respect to garbage collection; that is,
// GC and compilation can proceed independently without
// interference.
//
// Within the VM, the oop and klass hierarchies are separate.
// The compiler interface does not preserve this separation --
// the distinction between `Klass*' and `Klass' are not
// reflected in the interface and instead the Klass hierarchy
// is directly modeled as the subclasses of ciKlass.
class ciBaseObject : public ResourceObj {
  CI_PACKAGE_ACCESS
  friend class ciEnv;

protected:
  uint     _ident;

protected:
  ciBaseObject(): _ident(0) {}

  virtual const char* type_string() { return "ciBaseObject"; }

  void set_ident(uint id);

public:
  // A number unique to this object.
  uint ident();

  // What kind of ciBaseObject is this?
  virtual bool is_symbol() const       { return false; }
  virtual bool is_object() const       { return false; }
  virtual bool is_metadata() const     { return false; }

  ciSymbol* as_symbol() {
    assert(is_symbol(), "must be");
    return (ciSymbol*)this;
  }
  ciObject* as_object() {
    assert(is_object(), "must be");
    return (ciObject*)this;
  }
  ciMetadata* as_metadata() {
    assert(is_metadata(), "must be");
    return (ciMetadata*)this;
  }
};
#endif // SHARE_CI_CIBASEOBJECT_HPP
