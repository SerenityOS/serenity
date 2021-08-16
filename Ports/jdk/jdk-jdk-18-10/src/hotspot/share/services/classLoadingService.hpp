/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_CLASSLOADINGSERVICE_HPP
#define SHARE_SERVICES_CLASSLOADINGSERVICE_HPP

#include "logging/log.hpp"
#include "runtime/handles.hpp"
#include "runtime/perfData.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/macros.hpp"

class InstanceKlass;

// VM monitoring and management support for the Class Loading subsystem
class ClassLoadingService : public AllStatic {
private:
  // Counters for classes loaded from class files
  static PerfCounter*  _classes_loaded_count;
  static PerfCounter*  _classes_unloaded_count;
  static PerfCounter*  _classbytes_loaded;
  static PerfCounter*  _classbytes_unloaded;

  // Counters for classes loaded from shared archive
  static PerfCounter*  _shared_classes_loaded_count;
  static PerfCounter*  _shared_classes_unloaded_count;
  static PerfCounter*  _shared_classbytes_loaded;
  static PerfCounter*  _shared_classbytes_unloaded;

  static PerfVariable* _class_methods_size;

  static size_t compute_class_size(InstanceKlass* k);

public:
  static void init();

  static bool get_verbose() { return log_is_enabled(Info, class, load); }
  static bool set_verbose(bool verbose);
  static void reset_trace_class_unloading() NOT_MANAGEMENT_RETURN;

  static jlong loaded_class_count() {
    return _classes_loaded_count->get_value() + _shared_classes_loaded_count->get_value();
  }
  static jlong unloaded_class_count() {
    return _classes_unloaded_count->get_value() + _shared_classes_unloaded_count->get_value();
  }
  static jlong loaded_class_bytes() {
    if (UsePerfData) {
      return _classbytes_loaded->get_value() + _shared_classbytes_loaded->get_value();
    } else {
      return -1;
    }
  }
  static jlong unloaded_class_bytes() {
    if (UsePerfData) {
      return _classbytes_unloaded->get_value() + _shared_classbytes_unloaded->get_value();
    } else {
      return -1;
    }
  }

  static jlong loaded_shared_class_count() {
    return _shared_classes_loaded_count->get_value();
  }
  static jlong unloaded_shared_class_count() {
    return _shared_classes_unloaded_count->get_value();
  }
  static jlong loaded_shared_class_bytes() {
    if (UsePerfData) {
      return _shared_classbytes_loaded->get_value();
    } else {
      return -1;
    }
  }
  static jlong unloaded_shared_class_bytes() {
    if (UsePerfData) {
      return _shared_classbytes_unloaded->get_value();
    } else {
      return -1;
    }
  }
  static jlong class_method_data_size() {
    return (UsePerfData ? _class_methods_size->get_value() : -1);
  }

  static void notify_class_loaded(InstanceKlass* k, bool shared_class)
      NOT_MANAGEMENT_RETURN;
  // All unloaded classes are non-shared
  static void notify_class_unloaded(InstanceKlass* k) NOT_MANAGEMENT_RETURN;
  static void add_class_method_size(int size) {
#if INCLUDE_MANAGEMENT
    if (UsePerfData) {
      _class_methods_size->inc(size);
    }
#endif // INCLUDE_MANAGEMENT
  }
};

#endif // SHARE_SERVICES_CLASSLOADINGSERVICE_HPP
