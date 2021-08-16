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

#ifndef SHARE_OOPS_OOPHANDLE_INLINE_HPP
#define SHARE_OOPS_OOPHANDLE_INLINE_HPP

#include "oops/oopHandle.hpp"

#include "oops/access.inline.hpp"
#include "gc/shared/oopStorage.inline.hpp"

inline oop OopHandle::resolve() const {
  return (_obj == NULL) ? (oop)NULL : NativeAccess<>::oop_load(_obj);
}

inline oop OopHandle::peek() const {
  return (_obj == NULL) ? (oop)NULL : NativeAccess<AS_NO_KEEPALIVE>::oop_load(_obj);
}

inline OopHandle::OopHandle(OopStorage* storage, oop obj) :
    _obj(storage->allocate()) {
  if (_obj == NULL) {
    vm_exit_out_of_memory(sizeof(oop), OOM_MALLOC_ERROR,
                          "Cannot create oop handle");
  }
  NativeAccess<>::oop_store(_obj, obj);
}

inline void OopHandle::release(OopStorage* storage) {
  if (peek() != NULL) {
    // Clear the OopHandle first
    NativeAccess<>::oop_store(_obj, (oop)NULL);
    storage->release(_obj);
  }
}

inline void OopHandle::replace(oop obj) {
  oop* ptr = ptr_raw();
  assert(ptr != NULL, "should not use replace");
  NativeAccess<>::oop_store(ptr, obj);
}

inline oop OopHandle::xchg(oop new_value) {
  return NativeAccess<MO_SEQ_CST>::oop_atomic_xchg(_obj, new_value);
}

#endif // SHARE_OOPS_OOPHANDLE_INLINE_HPP
