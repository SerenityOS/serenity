/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_UTILITIES_ROOTTYPE_HPP
#define SHARE_JFR_LEAKPROFILER_UTILITIES_ROOTTYPE_HPP

#include "gc/shared/oopStorageSet.hpp"
#include "memory/allocation.hpp"
#include "utilities/enumIterator.hpp"

class OldObjectRoot : public AllStatic {
 public:
  enum System {
    _system_undetermined,
    _universe,
    _threads,
    _strong_oop_storage_set_first,
    _strong_oop_storage_set_last = _strong_oop_storage_set_first + EnumRange<OopStorageSet::StrongId>().size() - 1,
    _class_loader_data,
    _code_cache,
    JVMCI_ONLY(_jvmci COMMA)
    _number_of_systems
  };

  enum Type {
    _type_undetermined,
    _stack_variable,
    _local_jni_handle,
    _global_jni_handle,
    _global_oop_handle,
    _handle_area,
    _number_of_types
  };

  static OopStorage* system_oop_storage(System system);
  static const char* system_description(System system);
  static const char* type_description(Type type);
};

#endif // SHARE_JFR_LEAKPROFILER_UTILITIES_ROOTTYPE_HPP
