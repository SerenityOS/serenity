/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/oopStorageSet.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

OopStorage* OopStorageSet::_storages[all_count] = {};

OopStorage* OopStorageSet::create_strong(const char* name, MEMFLAGS memflags) {
  static uint registered_strong = 0;
  assert(registered_strong < strong_count, "More registered strong storages than slots");
  OopStorage* storage = new (memflags) OopStorage(name, memflags);
  _storages[strong_start + registered_strong++] = storage;
  return storage;
}

OopStorage* OopStorageSet::create_weak(const char* name, MEMFLAGS memflags) {
  static uint registered_weak = 0;
  assert(registered_weak < weak_count, "More registered strong storages than slots");
  OopStorage* storage = new (memflags) OopStorage(name, memflags);
  _storages[weak_start + registered_weak++] = storage;
  return storage;
}


void OopStorageSet::fill_strong(OopStorage* to[strong_count]) {
  for (uint i = 0; i < OopStorageSet::strong_count; i++) {
    to[i] = get_storage(strong_start + i);
  }
}

void OopStorageSet::fill_weak(OopStorage* to[weak_count]) {
  for (uint i = 0; i < OopStorageSet::weak_count; i++) {
    to[i] = get_storage(weak_start + i);
  }
}

void OopStorageSet::fill_all(OopStorage* to[all_count]) {
  for (uint i = 0; i < OopStorageSet::all_count; i++) {
    to[i] = get_storage(all_start + i);
  }
}

OopStorage* OopStorageSet::get_storage(uint index) {
  verify_initialized(index);
  return _storages[index];
}

template<typename E>
OopStorage* OopStorageSet::get_storage(E id) {
  assert(EnumRange<E>().first() <= id, "invalid id");
  assert(id <= EnumRange<E>().last(), "invalid id");
  return get_storage(static_cast<uint>(id));
}

template OopStorage* OopStorageSet::get_storage(StrongId);
template OopStorage* OopStorageSet::get_storage(WeakId);
template OopStorage* OopStorageSet::get_storage(Id);

#ifdef ASSERT

void OopStorageSet::verify_initialized(uint index) {
  assert(index < ARRAY_SIZE(_storages), "invalid index");
  assert(_storages[index] != NULL, "oopstorage_init not yet called");
}

#endif // ASSERT
