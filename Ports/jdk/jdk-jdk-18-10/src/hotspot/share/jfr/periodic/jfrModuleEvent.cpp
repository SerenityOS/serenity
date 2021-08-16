/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/periodic/jfrModuleEvent.hpp"
#include "jfr/utilities/jfrTime.hpp"
#include "runtime/mutexLocker.hpp"

// we want all periodic module events to have the same timestamp
static JfrTicks invocation_time;

typedef void (*EventFunc)(const void* iterated_address, const ModuleEntry* module);
class ModuleEventCallbackClosure : public ModuleClosure {
 protected:
  const EventFunc _event_func;
  ModuleEventCallbackClosure(EventFunc ef) : _event_func(ef) {}
};

class ModuleDependencyClosure : public ModuleEventCallbackClosure {
 private:
  const ModuleEntry* const _module;
 public:
   ModuleDependencyClosure(const ModuleEntry* module, EventFunc ef) : ModuleEventCallbackClosure(ef), _module(module) {}
   void do_module(ModuleEntry* entry);
};

class ModuleExportClosure : public ModuleEventCallbackClosure {
 private:
  const PackageEntry* const _package;
 public:
  ModuleExportClosure(const PackageEntry* pkg, EventFunc ef) : ModuleEventCallbackClosure(ef), _package(pkg) {}
  void do_module(ModuleEntry* entry);
};

static void write_module_dependency_event(const void* from_module, const ModuleEntry* to_module) {
  EventModuleRequire event(UNTIMED);
  event.set_endtime(invocation_time);
  event.set_source((const ModuleEntry* const)from_module);
  event.set_requiredModule(to_module);
  event.commit();
}

static void write_module_export_event(const void* package, const ModuleEntry* qualified_export) {
  EventModuleExport event(UNTIMED);
  event.set_endtime(invocation_time);
  event.set_exportedPackage((const PackageEntry*)package);
  event.set_targetModule(qualified_export);
  event.commit();
}

void ModuleDependencyClosure::do_module(ModuleEntry* to_module) {
  assert_locked_or_safepoint(Module_lock);
  assert(to_module != NULL, "invariant");
  assert(_module != NULL, "invariant");
  assert(_event_func != NULL, "invariant");
  _event_func(_module, to_module);
}

void ModuleExportClosure::do_module(ModuleEntry* qualified_export) {
  assert_locked_or_safepoint(Module_lock);
  assert(qualified_export != NULL, "invariant");
  assert(_package != NULL, "invariant");
  assert(_event_func != NULL, "invariant");
  _event_func(_package, qualified_export);
}

static void module_dependency_event_callback(ModuleEntry* module) {
  assert_locked_or_safepoint(Module_lock);
  assert(module != NULL, "invariant");
  if (module->has_reads_list()) {
    // create an individual event for each directed edge
    ModuleDependencyClosure directed_edges(module, &write_module_dependency_event);
    module->module_reads_do(&directed_edges);
  }
}

static void module_export_event_callback(PackageEntry* package) {
  assert_locked_or_safepoint(Module_lock);
  assert(package != NULL, "invariant");
  if (package->is_exported()) {
    if (package->has_qual_exports_list()) {
      // package is qualifiedly exported to a set of modules,
      // create an event for each module in the qualified exported list
      ModuleExportClosure qexports(package, &write_module_export_event);
      package->package_exports_do(&qexports);
      return;
    }

    assert(!package->is_qual_exported() || package->is_exported_allUnnamed(), "invariant");
    // no qualified exports
    // only create a single event with NULL
    // for the qualified_exports module
    write_module_export_event(package, NULL);
  }
}

void JfrModuleEvent::generate_module_dependency_events() {
  invocation_time = JfrTicks::now();
  MutexLocker cld_lock(ClassLoaderDataGraph_lock);
  MutexLocker module_lock(Module_lock);
  ClassLoaderDataGraph::modules_do(&module_dependency_event_callback);
}

void JfrModuleEvent::generate_module_export_events() {
  invocation_time = JfrTicks::now();
  MutexLocker cld_lock(ClassLoaderDataGraph_lock);
  MutexLocker module_lock(Module_lock);
  ClassLoaderDataGraph::packages_do(&module_export_event_callback);
}
