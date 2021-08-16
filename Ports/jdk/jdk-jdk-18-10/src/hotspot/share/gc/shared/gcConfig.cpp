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
#include "gc/shared/gcConfig.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_EPSILONGC
#include "gc/epsilon/epsilonArguments.hpp"
#endif
#if INCLUDE_G1GC
#include "gc/g1/g1Arguments.hpp"
#endif
#if INCLUDE_PARALLELGC
#include "gc/parallel/parallelArguments.hpp"
#endif
#if INCLUDE_SERIALGC
#include "gc/serial/serialArguments.hpp"
#endif
#if INCLUDE_SHENANDOAHGC
#include "gc/shenandoah/shenandoahArguments.hpp"
#endif
#if INCLUDE_ZGC
#include "gc/z/zArguments.hpp"
#endif

struct IncludedGC {
  bool&               _flag;
  CollectedHeap::Name _name;
  GCArguments&        _arguments;
  const char*         _hs_err_name;

  IncludedGC(bool& flag, CollectedHeap::Name name, GCArguments& arguments, const char* hs_err_name) :
      _flag(flag), _name(name), _arguments(arguments), _hs_err_name(hs_err_name) {}
};

   EPSILONGC_ONLY(static EpsilonArguments    epsilonArguments;)
        G1GC_ONLY(static G1Arguments         g1Arguments;)
  PARALLELGC_ONLY(static ParallelArguments   parallelArguments;)
    SERIALGC_ONLY(static SerialArguments     serialArguments;)
SHENANDOAHGC_ONLY(static ShenandoahArguments shenandoahArguments;)
         ZGC_ONLY(static ZArguments          zArguments;)

// Table of included GCs, for translating between command
// line flag, CollectedHeap::Name and GCArguments instance.
static const IncludedGC IncludedGCs[] = {
   EPSILONGC_ONLY_ARG(IncludedGC(UseEpsilonGC,       CollectedHeap::Epsilon,    epsilonArguments,    "epsilon gc"))
        G1GC_ONLY_ARG(IncludedGC(UseG1GC,            CollectedHeap::G1,         g1Arguments,         "g1 gc"))
  PARALLELGC_ONLY_ARG(IncludedGC(UseParallelGC,      CollectedHeap::Parallel,   parallelArguments,   "parallel gc"))
    SERIALGC_ONLY_ARG(IncludedGC(UseSerialGC,        CollectedHeap::Serial,     serialArguments,     "serial gc"))
SHENANDOAHGC_ONLY_ARG(IncludedGC(UseShenandoahGC,    CollectedHeap::Shenandoah, shenandoahArguments, "shenandoah gc"))
         ZGC_ONLY_ARG(IncludedGC(UseZGC,             CollectedHeap::Z,          zArguments,          "z gc"))
};

#define FOR_EACH_INCLUDED_GC(var)                                            \
  for (const IncludedGC* var = &IncludedGCs[0]; var < &IncludedGCs[ARRAY_SIZE(IncludedGCs)]; var++)

#define FAIL_IF_SELECTED(option)                                            \
  if (option) {                                                             \
    vm_exit_during_initialization("Option -XX:+" #option " not supported"); \
  }

GCArguments* GCConfig::_arguments = NULL;
bool GCConfig::_gc_selected_ergonomically = false;

void GCConfig::fail_if_non_included_gc_is_selected() {
  NOT_EPSILONGC(   FAIL_IF_SELECTED(UseEpsilonGC));
  NOT_G1GC(        FAIL_IF_SELECTED(UseG1GC));
  NOT_PARALLELGC(  FAIL_IF_SELECTED(UseParallelGC));
  NOT_SERIALGC(    FAIL_IF_SELECTED(UseSerialGC));
  NOT_SHENANDOAHGC(FAIL_IF_SELECTED(UseShenandoahGC));
  NOT_ZGC(         FAIL_IF_SELECTED(UseZGC));
}

void GCConfig::select_gc_ergonomically() {
  if (os::is_server_class_machine()) {
#if INCLUDE_G1GC
    FLAG_SET_ERGO_IF_DEFAULT(UseG1GC, true);
#elif INCLUDE_PARALLELGC
    FLAG_SET_ERGO_IF_DEFAULT(UseParallelGC, true);
#elif INCLUDE_SERIALGC
    FLAG_SET_ERGO_IF_DEFAULT(UseSerialGC, true);
#endif
  } else {
#if INCLUDE_SERIALGC
    FLAG_SET_ERGO_IF_DEFAULT(UseSerialGC, true);
#endif
  }
}

bool GCConfig::is_no_gc_selected() {
  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_flag) {
      return false;
    }
  }

  return true;
}

bool GCConfig::is_exactly_one_gc_selected() {
  CollectedHeap::Name selected = CollectedHeap::None;

  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_flag) {
      if (gc->_name == selected || selected == CollectedHeap::None) {
        // Selected
        selected = gc->_name;
      } else {
        // More than one selected
        return false;
      }
    }
  }

  return selected != CollectedHeap::None;
}

GCArguments* GCConfig::select_gc() {
  // Fail immediately if an unsupported GC is selected
  fail_if_non_included_gc_is_selected();

  if (is_no_gc_selected()) {
    // Try select GC ergonomically
    select_gc_ergonomically();

    if (is_no_gc_selected()) {
      // Failed to select GC ergonomically
      vm_exit_during_initialization("Garbage collector not selected "
                                    "(default collector explicitly disabled)", NULL);
    }

    // Succeeded to select GC ergonomically
    _gc_selected_ergonomically = true;
  }

  if (!is_exactly_one_gc_selected()) {
    // More than one GC selected
    vm_exit_during_initialization("Multiple garbage collectors selected", NULL);
  }

  // Exactly one GC selected
  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_flag) {
      return &gc->_arguments;
    }
  }

  fatal("Should have found the selected GC");

  return NULL;
}

void GCConfig::initialize() {
  assert(_arguments == NULL, "Already initialized");
  _arguments = select_gc();
}

bool GCConfig::is_gc_supported(CollectedHeap::Name name) {
  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_name == name && gc->_arguments.is_supported()) {
      // Supported
      return true;
    }
  }

  // Not supported
  return false;
}

bool GCConfig::is_gc_selected(CollectedHeap::Name name) {
  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_name == name && gc->_flag) {
      // Selected
      return true;
    }
  }

  // Not selected
  return false;
}

bool GCConfig::is_gc_selected_ergonomically() {
  return _gc_selected_ergonomically;
}

const char* GCConfig::hs_err_name() {
  if (is_exactly_one_gc_selected()) {
    // Exacly one GC selected
    FOR_EACH_INCLUDED_GC(gc) {
      if (gc->_flag) {
        return gc->_hs_err_name;
      }
    }
  }

  // Zero or more than one GC selected
  return "unknown gc";
}

const char* GCConfig::hs_err_name(CollectedHeap::Name name) {
  FOR_EACH_INCLUDED_GC(gc) {
    if (gc->_name == name) {
      return gc->_hs_err_name;
    }
  }
  return "unknown gc";
}

GCArguments* GCConfig::arguments() {
  assert(_arguments != NULL, "Not initialized");
  return _arguments;
}
