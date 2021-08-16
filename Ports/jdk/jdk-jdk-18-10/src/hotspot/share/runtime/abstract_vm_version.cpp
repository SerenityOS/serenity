/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm_io.h"
#include "compiler/compilerDefinitions.hpp"
#include "runtime/arguments.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/globalDefinitions.hpp"

const char* Abstract_VM_Version::_s_vm_release = Abstract_VM_Version::vm_release();
const char* Abstract_VM_Version::_s_internal_vm_info_string = Abstract_VM_Version::internal_vm_info_string();

uint64_t Abstract_VM_Version::_features = 0;
const char* Abstract_VM_Version::_features_string = "";

bool Abstract_VM_Version::_supports_cx8 = false;
bool Abstract_VM_Version::_supports_atomic_getset4 = false;
bool Abstract_VM_Version::_supports_atomic_getset8 = false;
bool Abstract_VM_Version::_supports_atomic_getadd4 = false;
bool Abstract_VM_Version::_supports_atomic_getadd8 = false;
unsigned int Abstract_VM_Version::_logical_processors_per_package = 1U;
unsigned int Abstract_VM_Version::_L1_data_cache_line_size = 0;
unsigned int Abstract_VM_Version::_data_cache_line_flush_size = 0;

VirtualizationType Abstract_VM_Version::_detected_virtualization = NoDetectedVirtualization;

#ifndef HOTSPOT_VERSION_STRING
  #error HOTSPOT_VERSION_STRING must be defined
#endif

#ifndef VERSION_FEATURE
  #error VERSION_FEATURE must be defined
#endif
#ifndef VERSION_INTERIM
  #error VERSION_INTERIM must be defined
#endif
#ifndef VERSION_UPDATE
  #error VERSION_UPDATE must be defined
#endif
#ifndef VERSION_PATCH
  #error VERSION_PATCH must be defined
#endif
#ifndef VERSION_BUILD
  #error VERSION_BUILD must be defined
#endif

#ifndef VERSION_STRING
  #error VERSION_STRING must be defined
#endif

#ifndef DEBUG_LEVEL
  #error DEBUG_LEVEL must be defined
#endif

#define VM_RELEASE HOTSPOT_VERSION_STRING

// HOTSPOT_VERSION_STRING equals the JDK VERSION_STRING (unless overridden
// in a standalone build).
int Abstract_VM_Version::_vm_major_version = VERSION_FEATURE;
int Abstract_VM_Version::_vm_minor_version = VERSION_INTERIM;
int Abstract_VM_Version::_vm_security_version = VERSION_UPDATE;
int Abstract_VM_Version::_vm_patch_version = VERSION_PATCH;
int Abstract_VM_Version::_vm_build_number = VERSION_BUILD;

#if defined(_LP64)
  #define VMLP "64-Bit "
#else
  #define VMLP ""
#endif

#ifndef VMTYPE
  #if COMPILER1_AND_COMPILER2
    #define VMTYPE "Server"
  #else // COMPILER1_AND_COMPILER2
  #ifdef ZERO
    #define VMTYPE "Zero"
  #else // ZERO
     #define VMTYPE COMPILER1_PRESENT("Client")   \
                    COMPILER2_PRESENT("Server")
  #endif // ZERO
  #endif // COMPILER1_AND_COMPILER2
#endif

#ifndef HOTSPOT_VM_DISTRO
  #error HOTSPOT_VM_DISTRO must be defined
#endif
#define VMNAME HOTSPOT_VM_DISTRO " " VMLP VMTYPE " VM"

const char* Abstract_VM_Version::vm_name() {
  return VMNAME;
}


const char* Abstract_VM_Version::vm_vendor() {
#ifdef VENDOR
  return VENDOR;
#else
  return "Oracle Corporation";
#endif
}


const char* Abstract_VM_Version::vm_info_string() {
  switch (Arguments::mode()) {
    case Arguments::_int:
      return UseSharedSpaces ? "interpreted mode, sharing" : "interpreted mode";
    case Arguments::_mixed:
      if (UseSharedSpaces) {
        if (CompilationModeFlag::quick_only()) {
          return "mixed mode, emulated-client, sharing";
        } else {
          return "mixed mode, sharing";
         }
      } else {
        if (CompilationModeFlag::quick_only()) {
          return "mixed mode, emulated-client";
        } else {
          return "mixed mode";
        }
      }
    case Arguments::_comp:
      if (CompilationModeFlag::quick_only()) {
         return UseSharedSpaces ? "compiled mode, emulated-client, sharing" : "compiled mode, emulated-client";
      }
      return UseSharedSpaces ? "compiled mode, sharing" : "compiled mode";
  }
  ShouldNotReachHere();
  return "";
}

// NOTE: do *not* use stringStream. this function is called by
//       fatal error handler. if the crash is in native thread,
//       stringStream cannot get resource allocated and will SEGV.
const char* Abstract_VM_Version::vm_release() {
  return VM_RELEASE;
}

// NOTE: do *not* use stringStream. this function is called by
//       fatal error handlers. if the crash is in native thread,
//       stringStream cannot get resource allocated and will SEGV.
const char* Abstract_VM_Version::jre_release_version() {
  return VERSION_STRING;
}

#define OS       LINUX_ONLY("linux")             \
                 WINDOWS_ONLY("windows")         \
                 AIX_ONLY("aix")                 \
                 BSD_ONLY("bsd")

#ifndef CPU
#ifdef ZERO
#define CPU      ZERO_LIBARCH
#elif defined(PPC64)
#if defined(VM_LITTLE_ENDIAN)
#define CPU      "ppc64le"
#else
#define CPU      "ppc64"
#endif // PPC64
#else
#define CPU      AARCH64_ONLY("aarch64")         \
                 AMD64_ONLY("amd64")             \
                 IA32_ONLY("x86")                \
                 IA64_ONLY("ia64")               \
                 S390_ONLY("s390")
#endif // !ZERO
#endif // !CPU

const char *Abstract_VM_Version::vm_platform_string() {
  return OS "-" CPU;
}

const char* Abstract_VM_Version::internal_vm_info_string() {
  #ifndef HOTSPOT_BUILD_USER
    #define HOTSPOT_BUILD_USER unknown
  #endif

  #ifndef HOTSPOT_BUILD_COMPILER
    #ifdef _MSC_VER
      #if _MSC_VER == 1600
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 10.0 (VS2010)"
      #elif _MSC_VER == 1700
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 11.0 (VS2012)"
      #elif _MSC_VER == 1800
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 12.0 (VS2013)"
      #elif _MSC_VER == 1900
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 14.0 (VS2015)"
      #elif _MSC_VER == 1911
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.3 (VS2017)"
      #elif _MSC_VER == 1912
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.5 (VS2017)"
      #elif _MSC_VER == 1913
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.6 (VS2017)"
      #elif _MSC_VER == 1914
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.7 (VS2017)"
      #elif _MSC_VER == 1915
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.8 (VS2017)"
      #elif _MSC_VER == 1916
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 15.9 (VS2017)"
      #elif _MSC_VER == 1920
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.0 (VS2019)"
      #elif _MSC_VER == 1921
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.1 (VS2019)"
      #elif _MSC_VER == 1922
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.2 (VS2019)"
      #elif _MSC_VER == 1923
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.3 (VS2019)"
      #elif _MSC_VER == 1924
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.4 (VS2019)"
      #elif _MSC_VER == 1925
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.5 (VS2019)"
      #elif _MSC_VER == 1926
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.6 (VS2019)"
      #elif _MSC_VER == 1927
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.7 (VS2019)"
      #elif _MSC_VER == 1928
        #define HOTSPOT_BUILD_COMPILER "MS VC++ 16.8 (VS2019)"
      #else
        #define HOTSPOT_BUILD_COMPILER "unknown MS VC++:" XSTR(_MSC_VER)
      #endif
    #elif defined(__clang_version__)
        #define HOTSPOT_BUILD_COMPILER "clang " __VERSION__
    #elif defined(__GNUC__)
        #define HOTSPOT_BUILD_COMPILER "gcc " __VERSION__
    #else
      #define HOTSPOT_BUILD_COMPILER "unknown compiler"
    #endif
  #endif

  #ifndef FLOAT_ARCH
    #if defined(__SOFTFP__)
      #define FLOAT_ARCH_STR "-sflt"
    #else
      #define FLOAT_ARCH_STR ""
    #endif
  #else
    #define FLOAT_ARCH_STR XSTR(FLOAT_ARCH)
  #endif

  #ifdef MUSL_LIBC
    #define LIBC_STR "-" XSTR(LIBC)
  #else
    #define LIBC_STR ""
  #endif

  #ifndef HOTSPOT_BUILD_TIME
    #define HOTSPOT_BUILD_TIME __DATE__ " " __TIME__
  #endif

  #define INTERNAL_VERSION_SUFFIX VM_RELEASE ")" \
         " for " OS "-" CPU FLOAT_ARCH_STR LIBC_STR \
         " JRE (" VERSION_STRING "), built on " HOTSPOT_BUILD_TIME \
         " by " XSTR(HOTSPOT_BUILD_USER) " with " HOTSPOT_BUILD_COMPILER

  return strcmp(DEBUG_LEVEL, "release") == 0
      ? VMNAME " (" INTERNAL_VERSION_SUFFIX
      : VMNAME " (" DEBUG_LEVEL " " INTERNAL_VERSION_SUFFIX;
}

const char *Abstract_VM_Version::vm_build_user() {
  return HOTSPOT_BUILD_USER;
}

const char *Abstract_VM_Version::jdk_debug_level() {
  return DEBUG_LEVEL;
}

const char *Abstract_VM_Version::printable_jdk_debug_level() {
  // Debug level is not printed for "release" builds
  return strcmp(DEBUG_LEVEL, "release") == 0 ? "" : DEBUG_LEVEL " ";
}

unsigned int Abstract_VM_Version::jvm_version() {
  return ((Abstract_VM_Version::vm_major_version() & 0xFF) << 24) |
         ((Abstract_VM_Version::vm_minor_version() & 0xFF) << 16) |
         ((Abstract_VM_Version::vm_security_version() & 0xFF) << 8) |
         (Abstract_VM_Version::vm_build_number() & 0xFF);
}

void Abstract_VM_Version::insert_features_names(char* buf, size_t buflen, const char* features_names[]) {
  uint64_t features = _features;
  uint features_names_index = 0;

  while (features != 0) {
    if (features & 1) {
      int res = jio_snprintf(buf, buflen, ", %s", features_names[features_names_index]);
      assert(res > 0, "not enough temporary space allocated");
      buf += res;
      buflen -= res;
    }
    features >>= 1;
    ++features_names_index;
  }
}

bool Abstract_VM_Version::print_matching_lines_from_file(const char* filename, outputStream* st, const char* keywords_to_match[]) {
  char line[500];
  FILE* fp = fopen(filename, "r");
  if (fp == NULL) {
    return false;
  }

  st->print_cr("Virtualization information:");
  while (fgets(line, sizeof(line), fp) != NULL) {
    int i = 0;
    while (keywords_to_match[i] != NULL) {
      if (strncmp(line, keywords_to_match[i], strlen(keywords_to_match[i])) == 0) {
        st->print("%s", line);
        break;
      }
      i++;
    }
  }
  fclose(fp);
  return true;
}
