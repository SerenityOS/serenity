/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_WEAKHANDLE_HPP
#define SHARE_OOPS_WEAKHANDLE_HPP

#include "oops/oop.hpp"
#include "runtime/handles.hpp"

class outputStream;
class OopStorage;

// A WeakHandle is a pointer to an oop that is stored in an OopStorage that is
// processed weakly by GC.  The runtime structures that point to the oop must
// either peek or resolve the oop, the latter will keep the oop alive for
// the GC cycle.  The runtime structures that reference the oop must test
// if the value is NULL.  If it is NULL, it has been cleaned out by GC.
// This is the vm version of jweak but has different GC lifetimes and policies,
// depending on the type.

class WeakHandle {
 public:
 private:
  oop* _obj;

  WeakHandle(oop* w) : _obj(w) {}
 public:
  WeakHandle() : _obj(NULL) {} // needed for init
  WeakHandle(OopStorage* storage, Handle obj);
  WeakHandle(OopStorage* storage, oop obj);

  inline oop resolve() const;
  inline oop peek() const;
  void release(OopStorage* storage) const;
  bool is_null() const { return _obj == NULL; }

  void replace(oop with_obj);

  void print() const;
  void print_on(outputStream* st) const;

  bool is_empty() const { return _obj == NULL; }
  oop* ptr_raw() const { return _obj; }
};

#endif // SHARE_OOPS_WEAKHANDLE_HPP
