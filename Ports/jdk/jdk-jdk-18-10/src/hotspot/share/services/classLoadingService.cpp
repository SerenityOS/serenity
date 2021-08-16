/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/method.hpp"
#include "runtime/mutexLocker.hpp"
#include "services/classLoadingService.hpp"
#include "services/memoryService.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/macros.hpp"
#include "utilities/defaultStream.hpp"
#include "logging/log.hpp"
#include "logging/logConfiguration.hpp"

#ifdef DTRACE_ENABLED

// Only bother with this argument setup if dtrace is available

#define HOTSPOT_CLASS_unloaded HOTSPOT_CLASS_UNLOADED
#define HOTSPOT_CLASS_loaded HOTSPOT_CLASS_LOADED
#define DTRACE_CLASSLOAD_PROBE(type, clss, shared)  \
  {                                                 \
    char* data = NULL;                              \
    int len = 0;                                    \
    Symbol* name = (clss)->name();                  \
    if (name != NULL) {                             \
      data = (char*)name->bytes();                  \
      len = name->utf8_length();                    \
    }                                               \
    HOTSPOT_CLASS_##type( /* type = unloaded, loaded */ \
      data, len, (void*)(clss)->class_loader_data(), (shared)); \
  }

#else //  ndef DTRACE_ENABLED

#define DTRACE_CLASSLOAD_PROBE(type, clss, shared)

#endif

#if INCLUDE_MANAGEMENT
// counters for classes loaded from class files
PerfCounter*    ClassLoadingService::_classes_loaded_count = NULL;
PerfCounter*    ClassLoadingService::_classes_unloaded_count = NULL;
PerfCounter*    ClassLoadingService::_classbytes_loaded = NULL;
PerfCounter*    ClassLoadingService::_classbytes_unloaded = NULL;

// counters for classes loaded from shared archive
PerfCounter*    ClassLoadingService::_shared_classes_loaded_count = NULL;
PerfCounter*    ClassLoadingService::_shared_classes_unloaded_count = NULL;
PerfCounter*    ClassLoadingService::_shared_classbytes_loaded = NULL;
PerfCounter*    ClassLoadingService::_shared_classbytes_unloaded = NULL;
PerfVariable*   ClassLoadingService::_class_methods_size = NULL;

void ClassLoadingService::init() {
  EXCEPTION_MARK;

  // These counters are for java.lang.management API support.
  // They are created even if -XX:-UsePerfData is set and in
  // that case, they will be allocated on C heap.
  _classes_loaded_count =
                 PerfDataManager::create_counter(JAVA_CLS, "loadedClasses",
                                                 PerfData::U_Events, CHECK);

  _classes_unloaded_count =
                 PerfDataManager::create_counter(JAVA_CLS, "unloadedClasses",
                                                 PerfData::U_Events, CHECK);

  _shared_classes_loaded_count =
                 PerfDataManager::create_counter(JAVA_CLS, "sharedLoadedClasses",
                                                 PerfData::U_Events, CHECK);

  _shared_classes_unloaded_count =
                 PerfDataManager::create_counter(JAVA_CLS, "sharedUnloadedClasses",
                                                 PerfData::U_Events, CHECK);

  if (UsePerfData) {
    _classbytes_loaded =
                 PerfDataManager::create_counter(SUN_CLS, "loadedBytes",
                                                 PerfData::U_Bytes, CHECK);

    _classbytes_unloaded =
                 PerfDataManager::create_counter(SUN_CLS, "unloadedBytes",
                                                 PerfData::U_Bytes, CHECK);
    _shared_classbytes_loaded =
                 PerfDataManager::create_counter(SUN_CLS, "sharedLoadedBytes",
                                                 PerfData::U_Bytes, CHECK);

    _shared_classbytes_unloaded =
                 PerfDataManager::create_counter(SUN_CLS, "sharedUnloadedBytes",
                                                 PerfData::U_Bytes, CHECK);
    _class_methods_size =
                 PerfDataManager::create_variable(SUN_CLS, "methodBytes",
                                                  PerfData::U_Bytes, CHECK);
  }
}

void ClassLoadingService::notify_class_unloaded(InstanceKlass* k) {
  DTRACE_CLASSLOAD_PROBE(unloaded, k, false);
  // Classes that can be unloaded must be non-shared
  _classes_unloaded_count->inc();

  if (UsePerfData) {
    // add the class size
    size_t size = compute_class_size(k);
    _classbytes_unloaded->inc(size);

    // Compute method size & subtract from running total.
    // We are called during phase 1 of mark sweep, so it's
    // still ok to iterate through Method*s here.
    Array<Method*>* methods = k->methods();
    for (int i = 0; i < methods->length(); i++) {
      _class_methods_size->inc(-methods->at(i)->size());
    }
  }
}

void ClassLoadingService::notify_class_loaded(InstanceKlass* k, bool shared_class) {
  DTRACE_CLASSLOAD_PROBE(loaded, k, shared_class);
  PerfCounter* classes_counter = (shared_class ? _shared_classes_loaded_count
                                               : _classes_loaded_count);
  // increment the count
  classes_counter->inc();

  if (UsePerfData) {
    PerfCounter* classbytes_counter = (shared_class ? _shared_classbytes_loaded
                                                    : _classbytes_loaded);
    // add the class size
    size_t size = compute_class_size(k);
    classbytes_counter->inc(size);
  }
}

size_t ClassLoadingService::compute_class_size(InstanceKlass* k) {
  // lifted from ClassStatistics.do_class(Klass* k)

  size_t class_size = 0;

  class_size += k->size();

  if (k->is_instance_klass()) {
    class_size += k->methods()->size();
    // FIXME: Need to count the contents of methods
    class_size += k->constants()->size();
    class_size += k->local_interfaces()->size();
    if (k->transitive_interfaces() != NULL) {
      class_size += k->transitive_interfaces()->size();
    }
    // We do not have to count implementors, since we only store one!
    // FIXME: How should these be accounted for, now when they have moved.
    //class_size += k->fields()->size();
  }
  return class_size * oopSize;
}

bool ClassLoadingService::set_verbose(bool verbose) {
  MutexLocker m(Management_lock);
  // verbose will be set to the previous value
  LogLevelType level = verbose ? LogLevel::Info : LogLevel::Off;
  LogConfiguration::configure_stdout(level, false, LOG_TAGS(class, load));
  reset_trace_class_unloading();
  return verbose;
}

// Caller to this function must own Management_lock
void ClassLoadingService::reset_trace_class_unloading() {
  assert(Management_lock->owned_by_self(), "Must own the Management_lock");
  bool value = MemoryService::get_verbose() || ClassLoadingService::get_verbose();
  LogLevelType level = value ? LogLevel::Info : LogLevel::Off;
  LogConfiguration::configure_stdout(level, false, LOG_TAGS(class, unload));
}

#endif // INCLUDE_MANAGEMENT
