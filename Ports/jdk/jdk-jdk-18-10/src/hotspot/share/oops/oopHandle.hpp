/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_OOPHANDLE_HPP
#define SHARE_OOPS_OOPHANDLE_HPP

#include "metaprogramming/primitiveConversions.hpp"
#include "oops/oopsHierarchy.hpp"

class OopStorage;

// Simple classes for wrapping oop and atomically accessed oop pointers
// stored in OopStorage, or stored in the ClassLoaderData handles area.
// These classes help with allocation, release, and NativeAccess loads and
// stores with the appropriate barriers.

class OopHandle {
  friend class VMStructs;
private:
  oop* _obj;

public:
  OopHandle() : _obj(NULL) {}
  explicit OopHandle(oop* w) : _obj(w) {}
  OopHandle(OopStorage* storage, oop obj);

  OopHandle(const OopHandle& copy) : _obj(copy._obj) {}

  OopHandle& operator=(const OopHandle& copy) {
    // Allow "this" to be junk if copy is empty; needed by initialization of
    // raw memory in hashtables.
    assert(is_empty() || copy.is_empty(), "can only copy if empty");
    _obj = copy._obj;
    return *this;
  }

  inline oop resolve() const;
  inline oop peek() const;

  bool is_empty() const { return _obj == NULL; }

  inline void release(OopStorage* storage);

  inline void replace(oop obj);

  inline oop xchg(oop new_value);

  // Used only for removing handle.
  oop* ptr_raw() const { return _obj; }
};

// Convert OopHandle to oop*

template<>
struct PrimitiveConversions::Translate<OopHandle> : public TrueType {
  typedef OopHandle Value;
  typedef oop* Decayed;

  static Decayed decay(Value x) { return x.ptr_raw(); }
  static Value recover(Decayed x) { return OopHandle(x); }
};

#endif // SHARE_OOPS_OOPHANDLE_HPP
