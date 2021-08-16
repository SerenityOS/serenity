/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/leakprofiler/utilities/rootType.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"

OopStorage* OldObjectRoot::system_oop_storage(System system) {
  int val = int(system);
  if (val >= _strong_oop_storage_set_first && val <= _strong_oop_storage_set_last) {
    using StrongId = OopStorageSet::StrongId;
    using Underlying = std::underlying_type_t<StrongId>;
    auto first = static_cast<Underlying>(EnumRange<StrongId>().first());
    auto id = static_cast<StrongId>(first + (val - _strong_oop_storage_set_first));
    return OopStorageSet::storage(id);
  }
  return NULL;
}

const char* OldObjectRoot::system_description(System system) {
  OopStorage* oop_storage = system_oop_storage(system);
  if (oop_storage != NULL) {
    return oop_storage->name();
  }
  switch (system) {
    case _system_undetermined:
      return "<unknown>";
    case _universe:
      return "Universe";
    case _threads:
      return "Threads";
    case _class_loader_data:
      return "Class Loader Data";
    case _code_cache:
      return "Code Cache";
#if INCLUDE_JVMCI
    case _jvmci:
      return "JVMCI";
#endif
    default:
      ShouldNotReachHere();
  }
  return NULL;
}

const char* OldObjectRoot::type_description(Type type) {
  switch (type) {
    case _type_undetermined:
      return "<unknown>";
    case _stack_variable:
      return "Stack Variable";
    case _local_jni_handle:
      return "Local JNI Handle";
    case _global_jni_handle:
      return "Global JNI Handle";
    case _global_oop_handle:
      return "Global Object Handle";
    case _handle_area:
      return "Handle Area";
    default:
      ShouldNotReachHere();
  }
  return NULL;
}
