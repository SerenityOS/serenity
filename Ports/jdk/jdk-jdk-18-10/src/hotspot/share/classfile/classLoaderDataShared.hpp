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

#ifndef SHARE_CLASSFILE_CLASSLOADERDATASHARED_HPP
#define SHARE_CLASSFILE_CLASSLOADERDATASHARED_HPP

#include "memory/allStatic.hpp"
#include "oops/oopsHierarchy.hpp"

class ClassLoaderData;
class MetaspaceClosure;
class SerializeClosure;

class ClassLoaderDataShared : AllStatic {
  static bool _full_module_graph_loaded;
public:
  static void allocate_archived_tables();
  static void iterate_symbols(MetaspaceClosure* closure);
  static void init_archived_tables();
  static void init_archived_oops();
  static void serialize(SerializeClosure* f);
  static void clear_archived_oops();
  static oop  restore_archived_oops_for_null_class_loader_data();
  static void restore_java_platform_loader_from_archive(ClassLoaderData* loader_data);
  static void restore_java_system_loader_from_archive(ClassLoaderData* loader_data);
  static bool is_full_module_graph_loaded() { return _full_module_graph_loaded; }
};

#endif // SHARE_CLASSFILE_CLASSLOADERDATASHARED_HPP
