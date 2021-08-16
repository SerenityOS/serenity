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

#include "precompiled.hpp"
#include "gc/shared/oopStorage.hpp"
#include "oops/access.inline.hpp"
#include "oops/oop.hpp"
#include "oops/weakHandle.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"

WeakHandle::WeakHandle(OopStorage* storage, Handle obj) :
    WeakHandle(storage, obj()) {}

WeakHandle::WeakHandle(OopStorage* storage, oop obj) :
    _obj(storage->allocate()) {
  assert(obj != NULL, "no need to create weak null oop");

  if (_obj == NULL) {
    vm_exit_out_of_memory(sizeof(oop*), OOM_MALLOC_ERROR,
                          "Unable to create new weak oop handle in OopStorage %s",
                          storage->name());
  }

  NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(_obj, obj);
}

void WeakHandle::release(OopStorage* storage) const {
  // Only release if the pointer to the object has been created.
  if (_obj != NULL) {
    // Clear the WeakHandle.  For race in creating ClassLoaderData, we can release this
    // WeakHandle before it is cleared by GC.
    NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(_obj, (oop)NULL);
    storage->release(_obj);
  }
}

void WeakHandle::print() const { print_on(tty); }

void WeakHandle::print_on(outputStream* st) const {
  st->print("WeakHandle: " PTR_FORMAT, p2i(peek()));
}
