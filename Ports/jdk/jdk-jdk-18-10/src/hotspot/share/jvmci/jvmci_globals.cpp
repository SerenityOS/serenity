/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compilerDefinitions.hpp"
#include "gc/shared/gcConfig.hpp"
#include "jvm.h"
#include "jvmci/jvmci_globals.hpp"
#include "logging/log.hpp"
#include "runtime/arguments.hpp"
#include "runtime/flags/jvmFlagAccess.hpp"
#include "runtime/globals_extension.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/ostream.hpp"

fileStream* JVMCIGlobals::_jni_config_file = NULL;

// Return true if jvmci flags are consistent.
bool JVMCIGlobals::check_jvmci_flags_are_consistent() {

#ifndef PRODUCT
#define APPLY_JVMCI_FLAGS(params3, params4) \
  JVMCI_FLAGS(params4, params3, params4, params3, params4, IGNORE_RANGE, IGNORE_CONSTRAINT)
#define JVMCI_DECLARE_CHECK4(type, name, value, ...) bool name##checked = false;
#define JVMCI_DECLARE_CHECK3(type, name, ...)        bool name##checked = false;
#define JVMCI_FLAG_CHECKED(name)                          name##checked = true;
  APPLY_JVMCI_FLAGS(JVMCI_DECLARE_CHECK3, JVMCI_DECLARE_CHECK4)
#else
#define JVMCI_FLAG_CHECKED(name)
#endif

  // Checks that a given flag is not set if a given guard flag is false.
#define CHECK_NOT_SET(FLAG, GUARD)                     \
  JVMCI_FLAG_CHECKED(FLAG)                             \
  if (!GUARD && !FLAG_IS_DEFAULT(FLAG)) {              \
    jio_fprintf(defaultStream::error_stream(),         \
        "Improperly specified VM option '%s': '%s' must be enabled\n", #FLAG, #GUARD); \
    return false;                                      \
  }

  if (EnableJVMCIProduct) {
    if (FLAG_IS_DEFAULT(EnableJVMCI)) {
      FLAG_SET_DEFAULT(EnableJVMCI, true);
    }
    if (EnableJVMCI && FLAG_IS_DEFAULT(UseJVMCICompiler)) {
      FLAG_SET_DEFAULT(UseJVMCICompiler, true);
    }
  }

  JVMCI_FLAG_CHECKED(UseJVMCICompiler)
  JVMCI_FLAG_CHECKED(EnableJVMCI)
  JVMCI_FLAG_CHECKED(EnableJVMCIProduct)

  CHECK_NOT_SET(BootstrapJVMCI,   UseJVMCICompiler)
  CHECK_NOT_SET(PrintBootstrap,   UseJVMCICompiler)
  CHECK_NOT_SET(JVMCIThreads,     UseJVMCICompiler)
  CHECK_NOT_SET(JVMCIHostThreads, UseJVMCICompiler)

  if (UseJVMCICompiler) {
    if (FLAG_IS_DEFAULT(UseJVMCINativeLibrary) && !UseJVMCINativeLibrary) {
      char path[JVM_MAXPATHLEN];
      if (os::dll_locate_lib(path, sizeof(path), Arguments::get_dll_dir(), JVMCI_SHARED_LIBRARY_NAME)) {
        // If a JVMCI native library is present,
        // we enable UseJVMCINativeLibrary by default.
        FLAG_SET_DEFAULT(UseJVMCINativeLibrary, true);
      }
    }
    if (!FLAG_IS_DEFAULT(EnableJVMCI) && !EnableJVMCI) {
      jio_fprintf(defaultStream::error_stream(),
          "Improperly specified VM option UseJVMCICompiler: EnableJVMCI cannot be disabled\n");
      return false;
    }
    FLAG_SET_DEFAULT(EnableJVMCI, true);
    if (BootstrapJVMCI && UseJVMCINativeLibrary) {
      jio_fprintf(defaultStream::error_stream(), "-XX:+BootstrapJVMCI is not compatible with -XX:+UseJVMCINativeLibrary\n");
      return false;
    }
    if (BootstrapJVMCI && (TieredStopAtLevel < CompLevel_full_optimization)) {
      jio_fprintf(defaultStream::error_stream(),
          "-XX:+BootstrapJVMCI is not compatible with -XX:TieredStopAtLevel=%d\n", TieredStopAtLevel);
      return false;
    }
  }

  if (!EnableJVMCI) {
    // Switch off eager JVMCI initialization if JVMCI is disabled.
    // Don't throw error if EagerJVMCI is set to allow testing.
    if (EagerJVMCI) {
      FLAG_SET_DEFAULT(EagerJVMCI, false);
    }
  }
  JVMCI_FLAG_CHECKED(EagerJVMCI)

  CHECK_NOT_SET(JVMCIEventLogLevel,           EnableJVMCI)
  CHECK_NOT_SET(JVMCITraceLevel,              EnableJVMCI)
  CHECK_NOT_SET(JVMCICounterSize,             EnableJVMCI)
  CHECK_NOT_SET(JVMCICountersExcludeCompiler, EnableJVMCI)
  CHECK_NOT_SET(JVMCIUseFastLocking,          EnableJVMCI)
  CHECK_NOT_SET(JVMCINMethodSizeLimit,        EnableJVMCI)
  CHECK_NOT_SET(JVMCIPrintProperties,         EnableJVMCI)
  CHECK_NOT_SET(UseJVMCINativeLibrary,        EnableJVMCI)
  CHECK_NOT_SET(JVMCILibPath,                 EnableJVMCI)
  CHECK_NOT_SET(JVMCINativeLibraryErrorFile,  EnableJVMCI)
  CHECK_NOT_SET(JVMCILibDumpJNIConfig,        EnableJVMCI)

#ifndef COMPILER2
  JVMCI_FLAG_CHECKED(MaxVectorSize)
  JVMCI_FLAG_CHECKED(ReduceInitialCardMarks)
  JVMCI_FLAG_CHECKED(UseMultiplyToLenIntrinsic)
  JVMCI_FLAG_CHECKED(UseSquareToLenIntrinsic)
  JVMCI_FLAG_CHECKED(UseMulAddIntrinsic)
  JVMCI_FLAG_CHECKED(UseMontgomeryMultiplyIntrinsic)
  JVMCI_FLAG_CHECKED(UseMontgomerySquareIntrinsic)
#endif // !COMPILER2

#ifndef PRODUCT
#define JVMCI_CHECK4(type, name, value, ...) assert(name##checked, #name " flag not checked");
#define JVMCI_CHECK3(type, name, ...)        assert(name##checked, #name " flag not checked");
  // Ensures that all JVMCI flags are checked by this method.
  APPLY_JVMCI_FLAGS(JVMCI_CHECK3, JVMCI_CHECK4)
#undef APPLY_JVMCI_FLAGS
#undef JVMCI_DECLARE_CHECK3
#undef JVMCI_DECLARE_CHECK4
#undef JVMCI_CHECK3
#undef JVMCI_CHECK4
#undef JVMCI_FLAG_CHECKED
#endif // PRODUCT
#undef CHECK_NOT_SET

  if (JVMCILibDumpJNIConfig != NULL) {
    _jni_config_file = new(ResourceObj::C_HEAP, mtJVMCI) fileStream(JVMCILibDumpJNIConfig);
    if (_jni_config_file == NULL || !_jni_config_file->is_open()) {
      jio_fprintf(defaultStream::error_stream(),
          "Could not open file for dumping JVMCI shared library JNI config: %s\n", JVMCILibDumpJNIConfig);
      return false;
    }
  }

  return true;
}

// Convert JVMCI flags from experimental to product
bool JVMCIGlobals::enable_jvmci_product_mode(JVMFlagOrigin origin) {
  const char *JVMCIFlags[] = {
    "EnableJVMCI",
    "EnableJVMCIProduct",
    "UseJVMCICompiler",
    "JVMCIPrintProperties",
    "EagerJVMCI",
    "JVMCIThreads",
    "JVMCICounterSize",
    "JVMCICountersExcludeCompiler",
    "JVMCINMethodSizeLimit",
    "JVMCIEventLogLevel",
    "JVMCITraceLevel",
    "JVMCILibPath",
    "JVMCILibDumpJNIConfig",
    "UseJVMCINativeLibrary",
    "JVMCINativeLibraryErrorFile",
    NULL
  };

  for (int i = 0; JVMCIFlags[i] != NULL; i++) {
    JVMFlag *jvmciFlag = (JVMFlag *)JVMFlag::find_declared_flag(JVMCIFlags[i]);
    if (jvmciFlag == NULL) {
      return false;
    }
    jvmciFlag->clear_experimental();
    jvmciFlag->set_product();
  }

  bool value = true;
  JVMFlag *jvmciEnableFlag = JVMFlag::find_flag("EnableJVMCIProduct");
  if (JVMFlagAccess::set_bool(jvmciEnableFlag, &value, origin) != JVMFlag::SUCCESS) {
    return false;
  }

  // Effect of EnableJVMCIProduct on changing defaults of EnableJVMCI
  // and UseJVMCICompiler is deferred to check_jvmci_flags_are_consistent
  // so that setting these flags explicitly (e.g. on the command line)
  // takes precedence.

  return true;
}

bool JVMCIGlobals::gc_supports_jvmci() {
  return UseSerialGC || UseParallelGC || UseG1GC;
}

void JVMCIGlobals::check_jvmci_supported_gc() {
  if (EnableJVMCI) {
    // Check if selected GC is supported by JVMCI and Java compiler
    if (!gc_supports_jvmci()) {
      log_warning(gc, jvmci)("Setting EnableJVMCI to false as selected GC does not support JVMCI: %s", GCConfig::hs_err_name());
      FLAG_SET_DEFAULT(EnableJVMCI, false);
      FLAG_SET_DEFAULT(UseJVMCICompiler, false);
    }
  }
}
